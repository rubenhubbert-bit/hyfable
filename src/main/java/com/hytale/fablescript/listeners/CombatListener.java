package com.hytale.fablescript.listeners;

import com.hytale.fablescript.core.MoralityAlignment;
import com.hytale.fablescript.events.ConsequenceTriggeredEvent;
import com.hytale.fablescript.events.FableEventBus;
import com.hytale.fablescript.storage.PlayerDataManager;
import com.hytale.fablescript.utils.FableLogger;

import java.util.UUID;

/**
 * Handles combat-related alignment changes and consequence triggers.
 *
 * <p>Key moral combat scenarios:
 * <ul>
 *   <li>Killing a non-hostile NPC: significant evil alignment shift</li>
 *   <li>Protecting a villager NPC from attack: good alignment bonus</li>
 *   <li>Killing a player with PURE_EVIL alignment: small good bonus (vigilante)</li>
 *   <li>Killing a player with GOOD alignment: evil alignment shift</li>
 * </ul>
 *
 * <p>All Hytale combat API hook registration is done in
 * {@link com.hytale.fablescript.FablePlugin#setup()} via
 * {@code getEventRegistry().registerGlobal(CombatEvent.class, combatListener::onKill)}.</p>
 */
public class CombatListener {

    private static final int NEUTRAL_NPC_KILL_DELTA  = -12;
    private static final int HOSTILE_NPC_KILL_DELTA  = +3;
    private static final int GOOD_PLAYER_KILL_DELTA  = -10;
    private static final int EVIL_PLAYER_KILL_DELTA  = +5;
    private static final int PROTECTOR_BONUS_DELTA   = +4;

    private final PlayerInteractionListener interactionListener;
    private final PlayerDataManager playerData;
    private final FableEventBus eventBus;
    private final FableLogger logger;

    /**
     * @param interactionListener used to apply alignment changes to killers
     * @param playerData          used to read victim alignment for contextual deltas
     * @param eventBus            for publishing consequence events
     * @param logger              plugin logger
     */
    public CombatListener(PlayerInteractionListener interactionListener,
                           PlayerDataManager playerData,
                           FableEventBus eventBus, FableLogger logger) {
        this.interactionListener = interactionListener;
        this.playerData          = playerData;
        this.eventBus            = eventBus;
        this.logger              = logger;
    }

    /**
     * Called when a player kills a non-hostile NPC.
     * Results in a significant evil alignment shift and a bounty consequence.
     *
     * @param killerUuid UUID of the player who performed the kill
     * @param npcId      NPC identifier of the victim
     * @param zoneKey    zone where the kill occurred
     */
    public void onNeutralNpcKill(UUID killerUuid, String npcId, String zoneKey) {
        interactionListener.applyAlignmentChange(killerUuid,
                NEUTRAL_NPC_KILL_DELTA, "killed_neutral_npc:" + npcId);

        eventBus.publish(new ConsequenceTriggeredEvent(
                killerUuid,
                ConsequenceTriggeredEvent.ConsequenceType.NPC_HOSTILITY_INCREASE,
                "npc_id=" + npcId + ";zone=" + zoneKey,
                zoneKey));

        eventBus.publish(new ConsequenceTriggeredEvent(
                killerUuid,
                ConsequenceTriggeredEvent.ConsequenceType.BOUNTY_PLACED,
                "reason=npc_murder;npc=" + npcId,
                null));

        logger.info("Player " + killerUuid + " killed neutral NPC '" + npcId
                + "' in zone '" + zoneKey + "'");
    }

    /**
     * Called when a player kills a hostile NPC (bandit, monster, etc.).
     * Grants a small good alignment bonus.
     *
     * @param killerUuid UUID of the player
     * @param npcId      NPC identifier
     * @param zoneKey    zone where the kill occurred
     */
    public void onHostileNpcKill(UUID killerUuid, String npcId, String zoneKey) {
        interactionListener.applyAlignmentChange(killerUuid,
                HOSTILE_NPC_KILL_DELTA, "killed_hostile_npc:" + npcId);
        logger.debug("Player " + killerUuid + " killed hostile NPC " + npcId);
    }

    /**
     * Called when a player kills another player.
     * Alignment impact depends on the victim's current alignment.
     *
     * @param killerUuid UUID of the killer
     * @param victimUuid UUID of the victim
     * @param zoneKey    zone where the kill occurred
     */
    public void onPlayerKill(UUID killerUuid, UUID victimUuid, String zoneKey) {
        MoralityAlignment victimTier = playerData.getProfile(victimUuid)
                .map(p -> MoralityAlignment.fromValue(p.getAlignment()))
                .orElse(MoralityAlignment.NEUTRAL);

        int delta;
        String cause;

        if (victimTier == MoralityAlignment.PURE_EVIL) {
            delta = EVIL_PLAYER_KILL_DELTA;
            cause = "killed_pure_evil_player";
        } else if (victimTier.isEvil()) {
            delta = EVIL_PLAYER_KILL_DELTA / 2;
            cause = "killed_evil_player";
        } else if (victimTier.isGood()) {
            delta = GOOD_PLAYER_KILL_DELTA;
            cause = "killed_good_player";
            // Also place a bounty on the killer
            eventBus.publish(new ConsequenceTriggeredEvent(
                    killerUuid,
                    ConsequenceTriggeredEvent.ConsequenceType.BOUNTY_PLACED,
                    "reason=good_player_murder;victim=" + victimUuid,
                    null));
        } else {
            delta = -3; // killing a neutral player — small evil shift
            cause = "killed_neutral_player";
        }

        interactionListener.applyAlignmentChange(killerUuid, delta, cause);
        logger.info("Player kill: killer=" + killerUuid + " victim=" + victimUuid
                + " delta=" + delta + " zone=" + zoneKey);
    }

    /**
     * Called when a player successfully defends or protects a friendly NPC from attack.
     *
     * @param protectorUuid UUID of the protecting player
     * @param npcId         NPC that was protected
     */
    public void onNpcProtected(UUID protectorUuid, String npcId) {
        interactionListener.applyAlignmentChange(protectorUuid,
                PROTECTOR_BONUS_DELTA, "protected_npc:" + npcId);
        logger.debug("Player " + protectorUuid + " protected NPC " + npcId);
    }
}
