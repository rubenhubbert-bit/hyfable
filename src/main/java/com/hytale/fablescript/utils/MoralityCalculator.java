package com.hytale.fablescript.utils;

import com.hytale.fablescript.core.MoralityAlignment;

/**
 * Stateless utility class providing morality arithmetic used throughout the plugin.
 *
 * <p>All methods are pure functions; no state is held here.
 * This class is not meant to be instantiated.</p>
 */
public final class MoralityCalculator {

    /** Absolute minimum alignment value. */
    public static final int MIN_ALIGNMENT = -100;

    /** Absolute maximum alignment value. */
    public static final int MAX_ALIGNMENT = 100;

    private MoralityCalculator() { /* utility class */ }

    /**
     * Applies a delta to the current alignment value, clamping to [-100, 100].
     *
     * @param current current alignment (-100 to 100)
     * @param delta   points to add (positive = good, negative = evil)
     * @return new alignment value, clamped
     */
    public static int applyDelta(int current, int delta) {
        return Math.max(MIN_ALIGNMENT, Math.min(MAX_ALIGNMENT, current + delta));
    }

    /**
     * Calculates daily alignment decay toward neutral.
     * Inactive players drift toward 0 at the configured rate.
     *
     * @param current       current alignment value
     * @param decayPerDay   amount to move toward 0 each day (positive, from config)
     * @param daysElapsed   number of in-game days since last activity
     * @return new alignment after decay, same sign preserved until reaching 0
     */
    public static int applyDecay(int current, double decayPerDay, int daysElapsed) {
        if (current == 0) return 0;
        double totalDecay = decayPerDay * daysElapsed;
        if (current > 0) {
            return (int) Math.max(0, current - totalDecay);
        } else {
            return (int) Math.min(0, current + totalDecay);
        }
    }

    /**
     * Clamps an alignment delta to the maximum change permitted per single action.
     *
     * @param delta      raw delta from a player action
     * @param maxChange  configured maximum per-action change (e.g., 25)
     * @return clamped delta in [-maxChange, maxChange]
     */
    public static int clampDelta(int delta, int maxChange) {
        return Math.max(-maxChange, Math.min(maxChange, delta));
    }

    /**
     * Returns a formatted progress-bar string for display in chat.
     * Example: {@code [##########----------] 50/100 NEUTRAL}
     *
     * @param value      current alignment value (-100 to 100)
     * @param barWidth   total character width of the bar (recommended: 20)
     * @return displayable alignment bar string
     */
    public static String buildProgressBar(int value, int barWidth) {
        MoralityAlignment tier = MoralityAlignment.fromValue(value);
        // Normalise -100..+100 to 0..barWidth
        int filled = (int) Math.round(((value + 100.0) / 200.0) * barWidth);
        filled = Math.max(0, Math.min(barWidth, filled));

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barWidth; i++) {
            bar.append(i < filled ? "#" : "-");
        }
        bar.append("] ");
        bar.append(value > 0 ? "+" : "").append(value);
        bar.append(" ").append(tier.getDisplayName());
        return bar.toString();
    }

    /**
     * Checks whether a player passes the alignment gate for a given ability tier.
     *
     * @param currentValue       player's current alignment
     * @param requiredAlignment  positive threshold for GOOD abilities,
     *                           negative for EVIL abilities, 0 for NEUTRAL (always allowed)
     * @return true if the player meets the requirement
     */
    public static boolean meetsAbilityRequirement(int currentValue, int requiredAlignment) {
        if (requiredAlignment == 0) return true;
        if (requiredAlignment > 0) return currentValue >= requiredAlignment;
        return currentValue <= requiredAlignment;
    }

    /**
     * Calculates a scaled reward multiplier based on alignment (used for quest gold).
     * Pure Good / Pure Evil players receive a 20% bonus; neutral gets the base rate.
     *
     * @param alignment current alignment value
     * @return multiplier in range [0.8, 1.2]
     */
    public static double rewardMultiplier(int alignment) {
        // Scale: 0 → 1.0, ±100 → 1.2
        return 1.0 + (Math.abs(alignment) / 100.0) * 0.2;
    }
}
