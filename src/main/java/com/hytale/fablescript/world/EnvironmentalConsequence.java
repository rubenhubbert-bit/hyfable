package com.hytale.fablescript.world;

import com.hytale.fablescript.core.MoralityAlignment;
import com.hytale.fablescript.events.ConsequenceTriggeredEvent;
import com.hytale.fablescript.events.FableEventBus;
import com.hytale.fablescript.utils.FableLogger;
import com.hytale.fablescript.utils.MathUtils;

import java.util.UUID;

/**
 * Applies environmental effects to the world based on player alignment and zone scores.
 *
 * <p>Environmental consequences are visual and gameplay modifications communicated
 * to players via chat messages and Hytale visual effect API calls.
 * All Hytale-specific world API calls are marked with {@code // TODO: Hytale API}
 * so they can be wired up once the API stabilises.</p>
 *
 * <p>Effect categories:
 * <ul>
 *   <li>PURE_GOOD players: flowers in footsteps, bright particles, clear weather</li>
 *   <li>GOOD players:      subtle positive particles, improved crop growth</li>
 *   <li>EVIL players:      dark aura, crop withering, wildlife fleeing</li>
 *   <li>PURE_EVIL players: corruption spread, darkness, NPC hostility escalation</li>
 * </ul>
 * </p>
 */
public class EnvironmentalConsequence {

    private final WorldState worldState;
    private final FableEventBus eventBus;
    private final FableLogger logger;
    private final int spreadRadius;
    private final double spreadChance;

    /**
     * @param worldState   mutable world zone state tracker
     * @param eventBus     event bus for publishing ConsequenceTriggeredEvents
     * @param spreadRadius config: block radius for corruption/blessing spread
     * @param spreadChance config: probability [0,1] of spread per check
     * @param logger       plugin logger
     */
    public EnvironmentalConsequence(WorldState worldState, FableEventBus eventBus,
                                     int spreadRadius, double spreadChance,
                                     FableLogger logger) {
        this.worldState   = worldState;
        this.eventBus     = eventBus;
        this.spreadRadius = spreadRadius;
        this.spreadChance = spreadChance;
        this.logger       = logger;
    }

    /**
     * Applies alignment-based environmental effects when a player moves through a zone.
     *
     * @param playerUuid      UUID of the player
     * @param playerAlignment current alignment value
     * @param zoneKey         zone the player is in
     */
    public void onPlayerPresence(UUID playerUuid, int playerAlignment, String zoneKey) {
        MoralityAlignment tier = MoralityAlignment.fromValue(playerAlignment);
        int zoneImpact = computeZoneImpact(tier);

        if (zoneImpact != 0) {
            worldState.adjustZoneScore(zoneKey, zoneImpact);
        }

        if (tier.isPure() && MathUtils.chance(spreadChance)) {
            applySpreadEffect(playerUuid, tier, zoneKey);
        }

        applyPlayerVisualEffect(playerUuid, tier);
        logger.debug("Environmental presence: player=" + playerUuid
                + " tier=" + tier + " zone=" + zoneKey + " impact=" + zoneImpact);
    }

    /**
     * Applies weather effects based on the player's alignment. Called periodically.
     *
     * @param playerUuid      player whose alignment drives weather
     * @param playerAlignment current alignment value
     * @param zoneKey         zone to modify weather in
     */
    public void applyWeatherEffect(UUID playerUuid, int playerAlignment, String zoneKey) {
        MoralityAlignment tier = MoralityAlignment.fromValue(playerAlignment);
        switch (tier) {
            case PURE_GOOD -> {
                // TODO: Hytale API — set weather to CLEAR in zone
                logger.debug("Weather set to CLEAR for PURE_GOOD player in zone " + zoneKey);
            }
            case EVIL -> {
                // TODO: Hytale API — set weather to OVERCAST in zone
                logger.debug("Weather set to OVERCAST for EVIL player in zone " + zoneKey);
            }
            case PURE_EVIL -> {
                // TODO: Hytale API — trigger STORM in zone
                logger.debug("Weather set to STORM for PURE_EVIL player in zone " + zoneKey);
            }
            default -> { /* GOOD and NEUTRAL: no weather intervention */ }
        }
    }

    /**
     * Called when a player's alignment tier changes; recalculates zone influence.
     *
     * @param playerUuid UUID of the player
     * @param newTier    the newly assigned alignment tier
     * @param zoneKey    current zone of the player
     */
    public void onTierChange(UUID playerUuid, MoralityAlignment newTier, String zoneKey) {
        ConsequenceTriggeredEvent.ConsequenceType type = newTier.isGood()
                ? ConsequenceTriggeredEvent.ConsequenceType.ENVIRONMENTAL_BLESSING
                : newTier.isEvil()
                ? ConsequenceTriggeredEvent.ConsequenceType.ENVIRONMENTAL_CORRUPTION
                : null;

        if (type != null) {
            String payload = "tier=" + newTier + ";zone=" + zoneKey;
            eventBus.publish(new ConsequenceTriggeredEvent(playerUuid, type, payload, zoneKey));
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private int computeZoneImpact(MoralityAlignment tier) {
        return switch (tier) {
            case PURE_GOOD -> +3;
            case GOOD      -> +1;
            case NEUTRAL   ->  0;
            case EVIL      -> -1;
            case PURE_EVIL -> -3;
        };
    }

    private void applySpreadEffect(UUID playerUuid, MoralityAlignment tier, String zoneKey) {
        // Derive adjacent zone keys using naming convention (zone_N, zone_S, etc.)
        String[] adjacent = { zoneKey + "_N", zoneKey + "_S", zoneKey + "_E", zoneKey + "_W" };
        int spreadImpact = tier == MoralityAlignment.PURE_GOOD ? +1 : -1;
        for (String adjZone : adjacent) {
            if (MathUtils.chance(spreadChance / 2)) {
                worldState.adjustZoneScore(adjZone, spreadImpact);
                logger.debug("Spread effect " + spreadImpact + " to adjacent zone " + adjZone);
            }
        }
    }

    private void applyPlayerVisualEffect(UUID playerUuid, MoralityAlignment tier) {
        // TODO: Hytale API — apply particle/aura effects per tier
        // PURE_GOOD  → golden radiant particles + glow
        // GOOD       → subtle white sparkles
        // NEUTRAL    → no effect
        // EVIL       → dark smoke particles + dim aura
        // PURE_EVIL  → corruption spreading particles + darkness effect
        logger.debug("Visual effect applied: " + tier + " for player " + playerUuid);
    }
}
