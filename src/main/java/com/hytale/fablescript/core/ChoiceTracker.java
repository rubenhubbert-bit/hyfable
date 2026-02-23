package com.hytale.fablescript.core;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks every moral choice made by every online player.
 *
 * <p>Stores choices in-memory with timestamps and provides anti-spam cooldown
 * enforcement to prevent players from farming alignment through rapid choices.
 * Choices are also written to the database via
 * {@link com.hytale.fablescript.storage.PlayerDataManager}.</p>
 */
public class ChoiceTracker {

    /** Minimum milliseconds between alignment-changing choices for the same context key. */
    private static final long CHOICE_COOLDOWN_MS = 10_000L; // 10 seconds

    /** Maximum in-memory choices stored per player before oldest are pruned. */
    private static final int MAX_CHOICES_PER_PLAYER = 200;

    /** Map of playerUuid → list of recorded choices. */
    private final Map<UUID, List<ChoiceRecord>> choicesByPlayer = new ConcurrentHashMap<>();

    /** Tracks last choice timestamp per player+contextKey to enforce cooldowns. */
    private final Map<String, Long> cooldownMap = new ConcurrentHashMap<>();

    /**
     * Records a player choice and checks it against the cooldown system.
     *
     * @param playerUuid       UUID of the acting player
     * @param contextKey       quest/dialogue node identifier
     * @param choiceId         the specific choice option selected
     * @param choiceText       human-readable description of the choice
     * @param alignmentDelta   alignment impact of this choice
     * @return true if the choice was accepted (not rate-limited)
     */
    public boolean recordChoice(UUID playerUuid, String contextKey, String choiceId,
                                 String choiceText, int alignmentDelta) {
        String cooldownKey = playerUuid + ":" + contextKey;
        long now = System.currentTimeMillis();

        if (isOnCooldown(cooldownKey, now)) {
            return false;
        }

        cooldownMap.put(cooldownKey, now);
        List<ChoiceRecord> records = choicesByPlayer.computeIfAbsent(
                playerUuid, k -> new ArrayList<>());

        records.add(new ChoiceRecord(contextKey, choiceId, choiceText,
                alignmentDelta, Instant.now().getEpochSecond()));

        if (records.size() > MAX_CHOICES_PER_PLAYER) {
            records.remove(0);
        }
        return true;
    }

    /**
     * Returns the full choice history for a player (most recent last).
     *
     * @param playerUuid UUID of the player
     * @return unmodifiable list of choice records, empty if none
     */
    public List<ChoiceRecord> getChoices(UUID playerUuid) {
        List<ChoiceRecord> records = choicesByPlayer.get(playerUuid);
        return records == null ? Collections.emptyList() : Collections.unmodifiableList(records);
    }

    /**
     * Returns the last {@code count} choices for a player, useful for history display.
     *
     * @param playerUuid UUID of the player
     * @param count      max number of recent choices to return
     * @return list of most recent choices, up to count
     */
    public List<ChoiceRecord> getRecentChoices(UUID playerUuid, int count) {
        List<ChoiceRecord> all = getChoices(playerUuid);
        int start = Math.max(0, all.size() - count);
        return all.subList(start, all.size());
    }

    /**
     * Counts how many times a player has chosen evil options (delta {@literal <} 0).
     *
     * @param playerUuid UUID to analyse
     * @return count of evil choices
     */
    public long countEvilChoices(UUID playerUuid) {
        return getChoices(playerUuid).stream()
                .filter(r -> r.alignmentDelta() < 0)
                .count();
    }

    /**
     * Counts how many times a player has chosen good options (delta {@literal >} 0).
     *
     * @param playerUuid UUID to analyse
     * @return count of good choices
     */
    public long countGoodChoices(UUID playerUuid) {
        return getChoices(playerUuid).stream()
                .filter(r -> r.alignmentDelta() > 0)
                .count();
    }

    /**
     * Clears all in-memory choices for a player (called on data reset or logout).
     *
     * @param playerUuid UUID of the player to clear
     */
    public void clearPlayer(UUID playerUuid) {
        choicesByPlayer.remove(playerUuid);
        cooldownMap.entrySet().removeIf(e -> e.getKey().startsWith(playerUuid.toString()));
    }

    private boolean isOnCooldown(String cooldownKey, long now) {
        Long last = cooldownMap.get(cooldownKey);
        return last != null && (now - last) < CHOICE_COOLDOWN_MS;
    }

    /**
     * Immutable record of a single player choice.
     *
     * @param contextKey      quest or dialogue node where this choice was made
     * @param choiceId        identifier of the selected option
     * @param choiceText      human-readable text of what the player chose
     * @param alignmentDelta  alignment points applied
     * @param epochSecond     wall-clock time of this choice
     */
    public record ChoiceRecord(String contextKey, String choiceId, String choiceText,
                                int alignmentDelta, long epochSecond) {}
}
