package com.hytale.fablescript.core;

import com.hytale.fablescript.utils.MoralityCalculator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Holds and manages the moral alignment state for a single player.
 *
 * <p>Tracks the current numeric value, tier, history of changes, and timestamps.
 * This is a mutable value object; all persistence is delegated to
 * {@link com.hytale.fablescript.storage.PlayerDataManager}.</p>
 */
public class PlayerMorality {

    /** Maximum number of alignment history entries kept in memory. */
    private static final int MAX_HISTORY_SIZE = 50;

    private final UUID playerUuid;

    /** Current alignment value in [-100, 100]. */
    private int alignmentValue;

    /** Derived tier; always consistent with alignmentValue. */
    private MoralityAlignment currentTier;

    /** Ordered history of alignment changes, most recent last. */
    private final List<AlignmentEntry> history;

    /** Epoch-second of last player activity (used for decay calculation). */
    private long lastActivityEpoch;

    /**
     * Constructs a PlayerMorality for a new or freshly-loaded player.
     *
     * @param playerUuid       UUID of the player
     * @param initialAlignment starting alignment value (typically 0 for new players)
     */
    public PlayerMorality(UUID playerUuid, int initialAlignment) {
        this.playerUuid = playerUuid;
        this.alignmentValue = MoralityCalculator.clamp(initialAlignment,
                MoralityCalculator.MIN_ALIGNMENT, MoralityCalculator.MAX_ALIGNMENT);
        this.currentTier = MoralityAlignment.fromValue(alignmentValue);
        this.history = new ArrayList<>();
        this.lastActivityEpoch = Instant.now().getEpochSecond();
    }

    /**
     * Applies a morality delta and records the change in history.
     * Automatically updates the current tier and fires an
     * {@link com.hytale.fablescript.events.AlignmentChangeEvent} via the event bus.
     *
     * @param delta  alignment points to add (positive = good, negative = evil)
     * @param cause  short description of why this change occurred
     * @param maxChange config-capped maximum absolute change per action
     */
    public void applyDelta(int delta, String cause, int maxChange) {
        int clamped = MoralityCalculator.clampDelta(delta, maxChange);
        int previous = alignmentValue;
        alignmentValue = MoralityCalculator.applyDelta(alignmentValue, clamped);
        currentTier = MoralityAlignment.fromValue(alignmentValue);
        lastActivityEpoch = Instant.now().getEpochSecond();
        recordHistory(previous, alignmentValue, cause);
    }

    /**
     * Applies daily decay toward neutral for inactive players.
     *
     * @param decayPerDay amount of decay per in-game day (from config, e.g. 0.1)
     * @param daysElapsed number of in-game days since last activity
     */
    public void applyDecay(double decayPerDay, int daysElapsed) {
        if (daysElapsed <= 0 || alignmentValue == 0) return;
        int previous = alignmentValue;
        alignmentValue = MoralityCalculator.applyDecay(alignmentValue, decayPerDay, daysElapsed);
        currentTier = MoralityAlignment.fromValue(alignmentValue);
        if (previous != alignmentValue) {
            recordHistory(previous, alignmentValue, "daily_decay");
        }
    }

    /**
     * Forcibly sets the alignment value, used by admin commands.
     *
     * @param value new alignment value, will be clamped to [-100, 100]
     * @param cause reason string (e.g., "admin_set")
     */
    public void forceSet(int value, String cause) {
        int previous = alignmentValue;
        alignmentValue = MoralityCalculator.clamp(value,
                MoralityCalculator.MIN_ALIGNMENT, MoralityCalculator.MAX_ALIGNMENT);
        currentTier = MoralityAlignment.fromValue(alignmentValue);
        recordHistory(previous, alignmentValue, cause);
    }

    /** @return player UUID */
    public UUID getPlayerUuid() { return playerUuid; }

    /** @return current alignment value in [-100, 100] */
    public int getAlignmentValue() { return alignmentValue; }

    /** @return current alignment tier derived from alignmentValue */
    public MoralityAlignment getCurrentTier() { return currentTier; }

    /** @return epoch-second of last recorded player activity */
    public long getLastActivityEpoch() { return lastActivityEpoch; }

    /**
     * Returns an unmodifiable view of the alignment change history.
     * Most recent changes are at the end of the list.
     *
     * @return immutable list of {@link AlignmentEntry} records
     */
    public List<AlignmentEntry> getHistory() {
        return Collections.unmodifiableList(history);
    }

    private void recordHistory(int from, int to, String cause) {
        history.add(new AlignmentEntry(from, to, cause, Instant.now().getEpochSecond()));
        if (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }
    }

    /**
     * Immutable record of a single alignment change event.
     */
    public record AlignmentEntry(int from, int to, String cause, long epochSecond) {

        /** @return net delta of this change */
        public int delta() { return to - from; }
    }
}
