package com.hytale.fablescript.core;

/**
 * Defines the five moral alignment tiers available to players.
 *
 * <p>Each tier has a numeric range, display name, chat color code,
 * and helper flags used by abilities, NPC reactions, and environmental systems.</p>
 *
 * <pre>
 *  +100 ─────── PURE_GOOD (+41 to +100): noble, radiant, revered
 *   +40 ─────── GOOD      (+1  to +40 ): heroic, respected
 *     0 ─────── NEUTRAL   (0)           : balanced, pragmatic
 *   -40 ─────── EVIL      (-1  to -40 ): selfish, feared
 *  -100 ─────── PURE_EVIL (-41 to -100): corrupt, despised
 * </pre>
 */
public enum MoralityAlignment {

    PURE_GOOD("Pure Good",  41,  100, "\u00a76", "§6✦"),
    GOOD     ("Good",        1,   40, "\u00a7a", "§a✚"),
    NEUTRAL  ("Neutral",     0,    0, "\u00a77", "§7◆"),
    EVIL     ("Evil",      -40,   -1, "\u00a7c", "§c✖"),
    PURE_EVIL("Pure Evil", -100, -41, "\u00a74", "§4☠");

    private final String displayName;
    private final int minValue;   // lowest value in this tier
    private final int maxValue;   // highest value in this tier
    private final String colorCode;
    private final String symbol;

    MoralityAlignment(String displayName, int minValue, int maxValue,
                       String colorCode, String symbol) {
        this.displayName = displayName;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.colorCode = colorCode;
        this.symbol = symbol;
    }

    /**
     * Returns the alignment tier that corresponds to the given numeric value.
     *
     * @param value alignment integer in [-100, 100]
     * @return matching MoralityAlignment tier
     */
    public static MoralityAlignment fromValue(int value) {
        if (value >= 41)  return PURE_GOOD;
        if (value >= 1)   return GOOD;
        if (value <= -41) return PURE_EVIL;
        if (value <= -1)  return EVIL;
        return NEUTRAL;
    }

    /** @return human-readable tier name (e.g., "Pure Good") */
    public String getDisplayName() { return displayName; }

    /** @return lowest numeric value in this tier */
    public int getMinValue() { return minValue; }

    /** @return highest numeric value in this tier */
    public int getMaxValue() { return maxValue; }

    /** @return Minecraft-style chat color code for this tier */
    public String getColorCode() { return colorCode; }

    /** @return decorative symbol prefix for chat formatting */
    public String getSymbol() { return symbol; }

    /**
     * Returns true if this tier is considered aligned with good (GOOD or PURE_GOOD).
     * Good-aligned players can access good abilities and receive NPC bonuses.
     *
     * @return true for GOOD and PURE_GOOD
     */
    public boolean isGood() { return this == GOOD || this == PURE_GOOD; }

    /**
     * Returns true if this tier is considered aligned with evil (EVIL or PURE_EVIL).
     * Evil-aligned players can access evil abilities and face NPC hostility.
     *
     * @return true for EVIL and PURE_EVIL
     */
    public boolean isEvil() { return this == EVIL || this == PURE_EVIL; }

    /**
     * Returns true if the player is at the extreme end of their alignment (|value| >= 41).
     * Pure tiers unlock special particle effects and unique quest lines.
     *
     * @return true for PURE_GOOD and PURE_EVIL
     */
    public boolean isPure() { return this == PURE_GOOD || this == PURE_EVIL; }
}
