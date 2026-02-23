package com.hytale.fablescript.world;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the persistent moral state of world zones.
 *
 * <p>World zones are identified by string keys (e.g., "marketplace", "forest_north").
 * Each zone accumulates a corruption or blessing score based on player activity.
 * These scores decay over time and influence visual/gameplay effects via
 * {@link EnvironmentalConsequence}.</p>
 */
public class WorldState {

    /** Corruption/blessing score bounds per zone. */
    private static final int MIN_ZONE_SCORE = -100;
    private static final int MAX_ZONE_SCORE =  100;

    /** Number of in-game ticks before a zone's score decays by 1 point. */
    private static final long ZONE_DECAY_TICKS = 20 * 60 * 30L; // ~30 min

    /**
     * Current moral score per zone.
     * Positive = blessed, negative = corrupted.
     */
    private final Map<String, Integer> zoneScores = new ConcurrentHashMap<>();

    /** Epoch-second timestamps of last player activity per zone. */
    private final Map<String, Long> zoneLastActivity = new ConcurrentHashMap<>();

    /** Set of zones currently marked as active corruption zones. */
    private final Set<String> activeCorruptionZones = ConcurrentHashMap.newKeySet();

    /** Set of zones currently marked as active blessing zones. */
    private final Set<String> activeBlessingZones = ConcurrentHashMap.newKeySet();

    /**
     * Returns the current moral score for a zone (default 0 for unknown zones).
     *
     * @param zoneKey zone identifier
     * @return score in [-100, 100]; negative = corrupted, positive = blessed
     */
    public int getZoneScore(String zoneKey) {
        return zoneScores.getOrDefault(zoneKey, 0);
    }

    /**
     * Adjusts the moral score of a zone, clamped to [-100, 100].
     *
     * @param zoneKey zone identifier
     * @param delta   amount to adjust (positive = more blessing, negative = more corruption)
     */
    public void adjustZoneScore(String zoneKey, int delta) {
        int current = zoneScores.getOrDefault(zoneKey, 0);
        int updated = Math.max(MIN_ZONE_SCORE, Math.min(MAX_ZONE_SCORE, current + delta));
        zoneScores.put(zoneKey, updated);
        zoneLastActivity.put(zoneKey, System.currentTimeMillis() / 1000L);
        updateZoneClassification(zoneKey, updated);
    }

    /**
     * Applies time-based decay to all zone scores, nudging them back toward 0.
     * Should be called by a periodic background task (e.g., every 30 minutes).
     *
     * @param persistenceHours hours before a zone fully resets to neutral;
     *                         used to compute per-tick decay magnitude
     */
    public void decayAllZones(int persistenceHours) {
        long now = System.currentTimeMillis() / 1000L;
        long decayWindowSeconds = (long) persistenceHours * 3600L;

        zoneScores.entrySet().removeIf(e -> {
            String zone = e.getKey();
            int score = e.getValue();
            if (score == 0) return true; // clean up neutral zones

            long lastActivity = zoneLastActivity.getOrDefault(zone, now);
            long elapsed = now - lastActivity;

            // Decay proportional to elapsed time
            double decayFraction = Math.min(1.0, (double) elapsed / decayWindowSeconds);
            int decayed = (int) (score * (1.0 - decayFraction));

            if (decayed == 0) {
                activeCorruptionZones.remove(zone);
                activeBlessingZones.remove(zone);
                return true; // remove entry
            }
            e.setValue(decayed);
            updateZoneClassification(zone, decayed);
            return false;
        });
    }

    /**
     * Records that an NPC was killed (permanently affects zone).
     *
     * @param npcId    NPC identifier that was killed
     * @param zoneKey  zone where the NPC resided
     */
    public void recordNpcDeath(String npcId, String zoneKey) {
        adjustZoneScore(zoneKey, -15); // NPC death is a significant corruption event
    }

    /**
     * Returns an unmodifiable snapshot of all zone scores.
     *
     * @return map of zoneKey → score
     */
    public Map<String, Integer> getAllZoneScores() {
        return Collections.unmodifiableMap(zoneScores);
    }

    /**
     * Returns all zones currently classified as corrupted (score {@literal <} -20).
     *
     * @return unmodifiable set of corrupted zone keys
     */
    public Set<String> getActiveCorruptionZones() {
        return Collections.unmodifiableSet(activeCorruptionZones);
    }

    /**
     * Returns all zones currently classified as blessed (score {@literal >} 20).
     *
     * @return unmodifiable set of blessed zone keys
     */
    public Set<String> getActiveBlessingZones() {
        return Collections.unmodifiableSet(activeBlessingZones);
    }

    /**
     * Checks whether a zone is currently suffering corruption.
     *
     * @param zoneKey zone identifier
     * @return true if score is below -20
     */
    public boolean isCorrupted(String zoneKey) {
        return getZoneScore(zoneKey) < -20;
    }

    /**
     * Checks whether a zone is currently blessed.
     *
     * @param zoneKey zone identifier
     * @return true if score is above 20
     */
    public boolean isBlessed(String zoneKey) {
        return getZoneScore(zoneKey) > 20;
    }

    private void updateZoneClassification(String zoneKey, int score) {
        if (score < -20) {
            activeCorruptionZones.add(zoneKey);
            activeBlessingZones.remove(zoneKey);
        } else if (score > 20) {
            activeBlessingZones.add(zoneKey);
            activeCorruptionZones.remove(zoneKey);
        } else {
            activeCorruptionZones.remove(zoneKey);
            activeBlessingZones.remove(zoneKey);
        }
    }
}
