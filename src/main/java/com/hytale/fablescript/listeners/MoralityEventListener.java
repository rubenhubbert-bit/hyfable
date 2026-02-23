package com.hytale.fablescript.listeners;

import com.hytale.fablescript.abilities.AbilityManager;
import com.hytale.fablescript.core.MoralityAlignment;
import com.hytale.fablescript.events.AlignmentChangeEvent;
import com.hytale.fablescript.events.FableEventBus;
import com.hytale.fablescript.npc.NPCManager;
import com.hytale.fablescript.storage.PlayerDataManager;
import com.hytale.fablescript.utils.FableLogger;
import com.hytale.fablescript.utils.MoralityCalculator;
import com.hytale.fablescript.world.DynamicWeather;
import com.hytale.fablescript.world.EnvironmentalConsequence;

import java.util.UUID;

/**
 * Responds to {@link AlignmentChangeEvent} by applying all downstream effects.
 *
 * <p>Effect chain on alignment change:
 * <ol>
 *   <li>Notify the player with a chat message and visual cue</li>
 *   <li>Recalculate and update ability unlocks</li>
 *   <li>Update all NPC relationships if a tier boundary was crossed</li>
 *   <li>Apply environmental consequence (corruption/blessing) in current zone</li>
 *   <li>Update dynamic weather in current zone</li>
 * </ol>
 * </p>
 */
public class MoralityEventListener {

    private final PlayerDataManager playerData;
    private final AbilityManager abilityManager;
    private final NPCManager npcManager;
    private final EnvironmentalConsequence environmentalConsequence;
    private final DynamicWeather dynamicWeather;
    private final FableLogger logger;

    /**
     * Constructs the listener and registers itself with the event bus.
     *
     * @param eventBus                bus to subscribe to AlignmentChangeEvent
     * @param playerData              player data manager
     * @param abilityManager          ability unlock/revoke coordinator
     * @param npcManager              NPC relationship coordinator
     * @param environmentalConsequence world environmental effect handler
     * @param dynamicWeather          weather state coordinator
     * @param logger                  plugin logger
     */
    public MoralityEventListener(FableEventBus eventBus, PlayerDataManager playerData,
                                   AbilityManager abilityManager, NPCManager npcManager,
                                   EnvironmentalConsequence environmentalConsequence,
                                   DynamicWeather dynamicWeather, FableLogger logger) {
        this.playerData              = playerData;
        this.abilityManager          = abilityManager;
        this.npcManager              = npcManager;
        this.environmentalConsequence = environmentalConsequence;
        this.dynamicWeather          = dynamicWeather;
        this.logger                  = logger;
        eventBus.subscribe(AlignmentChangeEvent.class, this::onAlignmentChange);
    }

    /**
     * Main handler — fires on every alignment change.
     *
     * @param event the alignment change event
     */
    public void onAlignmentChange(AlignmentChangeEvent event) {
        UUID playerUuid = event.getPlayerUuid();
        int newValue    = event.getNewValue();
        logger.debug("AlignmentChangeEvent received: " + event);

        // 1. Notify player
        notifyPlayer(playerUuid, event);

        // 2. Recalculate ability grants/revokes
        abilityManager.recalculateAbilities(playerUuid, newValue);

        // 3. Update NPC relationships on tier cross
        if (event.isTierChange()) {
            npcManager.onAlignmentTierChange(playerUuid, event.getPreviousTier(), event.getNewTier());
            notifyTierChange(playerUuid, event.getPreviousTier(), event.getNewTier());
        }

        // 4. Apply environmental consequence in current zone
        String zoneKey = resolvePlayerZone(playerUuid);
        environmentalConsequence.onTierChange(playerUuid, event.getNewTier(), zoneKey);

        // 5. Update dynamic weather
        dynamicWeather.onAlignmentChange(playerUuid, event.getNewTier(), zoneKey);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void notifyPlayer(UUID playerUuid, AlignmentChangeEvent event) {
        String direction = event.getDelta() > 0 ? "+" : "";
        String tierColor = event.getNewTier().getColorCode();
        String msg = tierColor + "[Fable] Alignment: "
                + direction + event.getDelta()
                + " → " + event.getNewTier().getDisplayName()
                + " (" + (event.getNewValue() > 0 ? "+" : "") + event.getNewValue() + ")";

        // TODO: Hytale API — player.sendMessage(Message.raw(msg))
        logger.debug("Notify player " + playerUuid + ": " + msg);
    }

    private void notifyTierChange(UUID playerUuid, MoralityAlignment from, MoralityAlignment to) {
        String msg = to.getColorCode() + "[Fable] ✦ Your alignment has shifted: "
                + from.getDisplayName() + " → " + to.getDisplayName() + "!";
        // TODO: Hytale API — player.sendMessage(Message.raw(msg))
        // TODO: Hytale API — play tier-change sound effect
        logger.info("Tier change for " + playerUuid + ": " + from + " → " + to);
    }

    private String resolvePlayerZone(UUID playerUuid) {
        // TODO: Hytale API — resolve zone from player location
        // For now, return a default zone key
        return "default_zone";
    }
}
