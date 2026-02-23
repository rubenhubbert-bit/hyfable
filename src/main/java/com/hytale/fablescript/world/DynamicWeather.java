package com.hytale.fablescript.world;

import com.hytale.fablescript.core.MoralityAlignment;
import com.hytale.fablescript.utils.FableLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages dynamic weather states per world zone based on the aggregate moral
 * score of players present.
 *
 * <p>Weather is not applied directly to the Hytale world API here — instead,
 * this class maintains desired weather states and provides them to
 * {@link EnvironmentalConsequence} and event listeners which make the actual
 * API calls. This design keeps the weather logic testable and API-agnostic.</p>
 *
 * <p>Weather priority (highest wins when multiple players are in the same zone):
 * <ol>
 *   <li>STORM — PURE_EVIL player present</li>
 *   <li>OVERCAST — EVIL player present (no PURE_EVIL)</li>
 *   <li>CLEAR — PURE_GOOD player present (no evil present)</li>
 *   <li>PARTLY_CLOUDY — GOOD player, no evil or pure good</li>
 *   <li>DEFAULT — neutral or no players</li>
 * </ol>
 * </p>
 */
public class DynamicWeather {

    /** Possible weather states managed by the system. */
    public enum WeatherState {
        CLEAR, PARTLY_CLOUDY, DEFAULT, OVERCAST, STORM
    }

    private final FableLogger logger;

    /** Current weather state per zone key. */
    private final Map<String, WeatherState> zoneWeather = new ConcurrentHashMap<>();

    /** Maps zoneKey → set of player UUIDs currently in that zone. */
    private final Map<String, Set<UUID>> playersInZone = new ConcurrentHashMap<>();

    /** Maps playerUuid → their current alignment tier (updated by event listeners). */
    private final Map<UUID, MoralityAlignment> playerTiers = new ConcurrentHashMap<>();

    /**
     * @param logger plugin logger
     */
    public DynamicWeather(FableLogger logger) {
        this.logger = logger;
    }

    /**
     * Updates the tracked zone for a player and recalculates weather.
     * Called when a player moves between zones.
     *
     * @param playerUuid  player UUID
     * @param oldZone     previous zone key, or null on first entry
     * @param newZone     new zone key
     * @param playerTier  player's current alignment tier
     */
    public void onPlayerZoneChange(UUID playerUuid, String oldZone,
                                    String newZone, MoralityAlignment playerTier) {
        // Remove from old zone
        if (oldZone != null) {
            Set<UUID> oldPlayers = playersInZone.get(oldZone);
            if (oldPlayers != null) {
                oldPlayers.remove(playerUuid);
                recalculate(oldZone);
            }
        }
        // Add to new zone
        playerTiers.put(playerUuid, playerTier);
        playersInZone.computeIfAbsent(newZone, k -> ConcurrentHashMap.newKeySet())
                     .add(playerUuid);
        recalculate(newZone);
    }

    /**
     * Updates a player's alignment tier and recalculates weather for their zone.
     *
     * @param playerUuid player UUID
     * @param newTier    newly computed alignment tier
     * @param zoneKey    zone where the player currently is
     */
    public void onAlignmentChange(UUID playerUuid, MoralityAlignment newTier, String zoneKey) {
        playerTiers.put(playerUuid, newTier);
        recalculate(zoneKey);
    }

    /**
     * Removes a player from all zone tracking on logout.
     *
     * @param playerUuid player UUID
     */
    public void onPlayerLogout(UUID playerUuid) {
        playerTiers.remove(playerUuid);
        playersInZone.forEach((zone, players) -> {
            if (players.remove(playerUuid)) {
                recalculate(zone);
            }
        });
    }

    /**
     * Returns the current desired weather state for a zone.
     *
     * @param zoneKey zone identifier
     * @return weather state for this zone (DEFAULT if not set)
     */
    public WeatherState getWeather(String zoneKey) {
        return zoneWeather.getOrDefault(zoneKey, WeatherState.DEFAULT);
    }

    /** @return an unmodifiable snapshot of all zone weather states */
    public Map<String, WeatherState> getAllZoneWeather() {
        return Collections.unmodifiableMap(zoneWeather);
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void recalculate(String zoneKey) {
        Set<UUID> players = playersInZone.getOrDefault(zoneKey, Set.of());
        WeatherState state = computeWeather(players);
        WeatherState previous = zoneWeather.put(zoneKey, state);
        if (previous != state) {
            logger.debug("Weather in zone '" + zoneKey + "' changed: " + previous + " → " + state);
            // TODO: Hytale API — apply weather state to game world zone
        }
    }

    private WeatherState computeWeather(Set<UUID> players) {
        if (players.isEmpty()) return WeatherState.DEFAULT;

        boolean hasPureEvil = false;
        boolean hasEvil = false;
        boolean hasPureGood = false;
        boolean hasGood = false;

        for (UUID uuid : players) {
            MoralityAlignment tier = playerTiers.getOrDefault(uuid, MoralityAlignment.NEUTRAL);
            if (tier == MoralityAlignment.PURE_EVIL) hasPureEvil = true;
            else if (tier.isEvil()) hasEvil = true;
            else if (tier == MoralityAlignment.PURE_GOOD) hasPureGood = true;
            else if (tier.isGood()) hasGood = true;
        }

        // Priority order
        if (hasPureEvil)                  return WeatherState.STORM;
        if (hasEvil)                      return WeatherState.OVERCAST;
        if (hasPureGood && !hasEvil)      return WeatherState.CLEAR;
        if (hasGood)                      return WeatherState.PARTLY_CLOUDY;
        return WeatherState.DEFAULT;
    }
}
