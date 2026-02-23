package com.hytale.fablescript.utils;

/**
 * General-purpose math and probability utilities for FableScript.
 */
public final class MathUtils {

    private MathUtils() { /* utility class */ }

    /**
     * Linearly interpolates between {@code a} and {@code b} by factor {@code t}.
     *
     * @param a start value
     * @param b end value
     * @param t interpolation factor [0, 1]
     * @return interpolated value
     */
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * Math.max(0, Math.min(1, t));
    }

    /**
     * Clamps an integer to an inclusive range.
     *
     * @param value value to clamp
     * @param min   lower bound
     * @param max   upper bound
     * @return clamped value
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Rolls a probability check using {@link Math#random()}.
     *
     * @param chance probability in [0, 1]; 0.3 means 30% chance
     * @return true if the roll succeeded
     */
    public static boolean chance(double chance) {
        return Math.random() < chance;
    }

    /**
     * Returns a random integer in [min, max] inclusive.
     *
     * @param min lower bound (inclusive)
     * @param max upper bound (inclusive)
     * @return random integer
     */
    public static int randomInt(int min, int max) {
        if (min >= max) return min;
        return min + (int) (Math.random() * (max - min + 1));
    }

    /**
     * Converts a percentage (0-100) to a 0.0-1.0 fraction, clamping to [0, 1].
     *
     * @param percent percentage value
     * @return fraction
     */
    public static double percentToFraction(double percent) {
        return Math.max(0.0, Math.min(1.0, percent / 100.0));
    }

    /**
     * Formats seconds into a human-readable duration string (e.g., "2h 15m 03s").
     *
     * @param totalSeconds total duration in seconds
     * @return formatted duration string
     */
    public static String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%dh %dm %02ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %02ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
