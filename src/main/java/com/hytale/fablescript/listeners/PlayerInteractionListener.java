package com.hytale.fablescript.listeners;

import com.hytale.fablescript.core.ChoiceTracker;
import com.hytale.fablescript.core.ConsequenceManager;
import com.hytale.fablescript.core.PlayerMorality;
import com.hytale.fablescript.events.AlignmentChangeEvent;
import com.hytale.fablescript.events.FableEventBus;
import com.hytale.fablescript.npc.DialogueTree;
import com.hytale.fablescript.npc.NPCBehavior;
import com.hytale.fablescript.npc.NPCManager;
import com.hytale.fablescript.storage.PlayerDataManager;
import com.hytale.fablescript.utils.FableLogger;
import com.hytale.fablescript.utils.MoralityCalculator;
import com.hytale.fablescript.world.EnvironmentalConsequence;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.Message;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles player join, movement, and NPC interaction events from the Hytale API.
 *
 * <p>Wires Hytale's event system into the FableScript consequence and morality pipeline.
 * Hytale events are registered in {@link com.hytale.fablescript.FablePlugin#setup()}
 * using {@code getEventRegistry().registerGlobal(EventClass.class, handler)}.</p>
 */
public class PlayerInteractionListener {

    private final PlayerDataManager playerData;
    private final NPCManager npcManager;
    private final EnvironmentalConsequence environmentalConsequence;
    private final ChoiceTracker choiceTracker;
    private final FableEventBus eventBus;
    private final FableLogger logger;
    private final int maxAlignmentChange;
    private final int decayIntervalHours;

    /** Tracks session start time per player for playtime calculation on logout. */
    private final Map<UUID, Long> sessionStartTimes = new ConcurrentHashMap<>();

    /** In-memory morality state per online player (loaded from profile on join). */
    private final Map<UUID, PlayerMorality> moralityMap = new ConcurrentHashMap<>();

    /**
     * Full constructor; called by FablePlugin.
     */
    public PlayerInteractionListener(PlayerDataManager playerData, NPCManager npcManager,
                                      EnvironmentalConsequence environmentalConsequence,
                                      ChoiceTracker choiceTracker, FableEventBus eventBus,
                                      int maxAlignmentChange, int decayIntervalHours,
                                      FableLogger logger) {
        this.playerData            = playerData;
        this.npcManager            = npcManager;
        this.environmentalConsequence = environmentalConsequence;
        this.choiceTracker         = choiceTracker;
        this.eventBus              = eventBus;
        this.maxAlignmentChange    = maxAlignmentChange;
        this.decayIntervalHours    = decayIntervalHours;
        this.logger                = logger;
    }

    // ── Hytale API event handlers ─────────────────────────────────────────────

    /**
     * Fired by Hytale when a player finishes loading and is ready.
     * Loads/creates profile and sets up morality state.
     *
     * @param event Hytale PlayerReadyEvent
     */
    public void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getDisplayName();

        sessionStartTimes.put(uuid, System.currentTimeMillis());

        playerData.onPlayerLogin(uuid, name).thenAccept(profile -> {
            PlayerMorality morality = new PlayerMorality(uuid, profile.getAlignment());
            moralityMap.put(uuid, morality);

            player.sendMessage(Message.raw(
                    "§6[FableScript] Welcome back, §f" + name + "§6! "
                    + "Alignment: §f" + profile.getAlignmentTier().getDisplayName()
                    + " §7(" + (profile.getAlignment() >= 0 ? "+" : "") + profile.getAlignment() + ")"
            ));
            logger.info("Player joined: " + name + " [alignment=" + profile.getAlignment() + "]");
        });
    }

    /**
     * Called when a player leaves the server.
     * Saves playtime and flushes profile to disk.
     *
     * @param playerUuid UUID of the departing player
     * @param playerName display name for logging
     */
    public void onPlayerLeave(UUID playerUuid, String playerName) {
        Long start = sessionStartTimes.remove(playerUuid);
        long sessionSeconds = start != null ? (System.currentTimeMillis() - start) / 1000L : 0L;

        // Sync morality value back to profile before save
        PlayerMorality morality = moralityMap.remove(playerUuid);
        if (morality != null) {
            playerData.updateAlignment(playerUuid, morality.getAlignmentValue());
        }

        choiceTracker.clearPlayer(playerUuid);
        playerData.onPlayerLogout(playerUuid, sessionSeconds);
        logger.info("Player left: " + playerName + " [session=" + sessionSeconds + "s]");
    }

    /**
     * Applies a moral alignment change to a player from any source (dialogue, quest, combat).
     * Publishes {@link AlignmentChangeEvent} if the change is non-zero.
     *
     * @param playerUuid  UUID of the player
     * @param delta       alignment change (positive = good, negative = evil)
     * @param cause       short description of the cause (for history logging)
     */
    public void applyAlignmentChange(UUID playerUuid, int delta, String cause) {
        PlayerMorality morality = moralityMap.get(playerUuid);
        if (morality == null) return;

        var previousTier  = morality.getCurrentTier();
        int previousValue = morality.getAlignmentValue();

        morality.applyDelta(delta, cause, maxAlignmentChange);

        int newValue = morality.getAlignmentValue();
        if (newValue == previousValue) return; // no actual change

        playerData.updateAlignment(playerUuid, newValue);

        eventBus.publish(new AlignmentChangeEvent(
                playerUuid, previousValue, newValue,
                previousTier, morality.getCurrentTier(), cause));
    }

    /**
     * Returns the in-memory morality state for an online player.
     *
     * @param playerUuid UUID to query
     * @return Optional containing morality state, or empty if player is not online
     */
    public Optional<PlayerMorality> getMorality(UUID playerUuid) {
        return Optional.ofNullable(moralityMap.get(playerUuid));
    }

    /**
     * Handles a player interacting with an NPC — resolves behaviour and presents dialogue.
     * This method is called from a game interaction hook.
     *
     * @param playerUuid UUID of the interacting player
     * @param npcId      NPC the player interacted with
     */
    public void onNpcInteract(UUID playerUuid, String npcId) {
        Optional<NPCBehavior.BehaviorState> stateOpt = npcManager.getBehavior(npcId, playerUuid);
        if (stateOpt.isEmpty()) return;

        NPCBehavior.BehaviorState state = stateOpt.get();

        if (state.attackOnSight()) {
            // TODO: Hytale API — trigger NPC combat against player
            logger.debug("NPC " + npcId + " attacks " + playerUuid + " on sight");
            return;
        }
        if (state.fleeOnSight()) {
            // TODO: Hytale API — trigger NPC flee behaviour
            logger.debug("NPC " + npcId + " flees from " + playerUuid);
            return;
        }

        // Present dialogue greeting via chat
        // TODO: Hytale API — open dialogue UI with state.greeting()
        logger.debug("NPC " + npcId + " greets " + playerUuid + ": " + state.greeting());
    }
}
