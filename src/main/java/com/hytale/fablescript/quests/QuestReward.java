package com.hytale.fablescript.quests;

/**
 * Describes a single reward granted to a player upon completing a quest choice path.
 *
 * <p>Rewards are stackable — a choice can grant multiple reward objects.
 * Reward values can scale with the player's alignment via the multiplier mechanism
 * in {@link com.hytale.fablescript.utils.MoralityCalculator#rewardMultiplier}.</p>
 */
public class QuestReward {

    /** Category of reward determines how it is applied by QuestManager. */
    public enum RewardType {
        GOLD,           // in-game currency
        EXPERIENCE,     // player XP
        ITEM,           // specific item grant (item ID in value field)
        ALIGNMENT,      // bonus alignment change (in addition to choice delta)
        ABILITY_UNLOCK, // unlocks a specific ability (ability ID in value field)
        RELATIONSHIP,   // boosts NPC relationship (format "npc_id:delta" in value)
        TITLE           // cosmetic title grant (title string in value field)
    }

    private final RewardType type;

    /**
     * Numeric magnitude for GOLD, EXPERIENCE, ALIGNMENT, or RELATIONSHIP,
     * or the string key for ITEM, ABILITY_UNLOCK, and TITLE.
     */
    private final String value;

    /** Whether this reward amount scales with the player's alignment multiplier. */
    private final boolean alignmentScaled;

    /**
     * Constructs a fixed (non-scaled) reward.
     *
     * @param type  category of reward
     * @param value magnitude or key string
     */
    public QuestReward(RewardType type, String value) {
        this(type, value, false);
    }

    /**
     * Constructs a reward with optional alignment scaling.
     *
     * @param type            category of reward
     * @param value           magnitude or key string
     * @param alignmentScaled true to scale numeric value by alignment multiplier
     */
    public QuestReward(RewardType type, String value, boolean alignmentScaled) {
        this.type            = type;
        this.value           = value;
        this.alignmentScaled = alignmentScaled;
    }

    /** @return reward category */
    public RewardType getType() { return type; }

    /**
     * Returns the raw value string.
     * For numeric types (GOLD, EXPERIENCE), parse with {@link Integer#parseInt(String)}.
     *
     * @return raw value string
     */
    public String getValue() { return value; }

    /**
     * Returns the numeric value of this reward, scaled by the provided multiplier.
     * If not alignment-scaled, the multiplier is ignored.
     *
     * @param alignmentMultiplier multiplier from MoralityCalculator.rewardMultiplier()
     * @return scaled integer value (rounded)
     */
    public int getScaledAmount(double alignmentMultiplier) {
        try {
            int base = Integer.parseInt(value);
            return alignmentScaled ? (int) Math.round(base * alignmentMultiplier) : base;
        } catch (NumberFormatException e) {
            return 0; // non-numeric value type
        }
    }

    /** @return true if this reward's numeric amount scales with alignment */
    public boolean isAlignmentScaled() { return alignmentScaled; }

    @Override
    public String toString() {
        return "QuestReward{type=" + type + ", value='" + value
                + "', scaled=" + alignmentScaled + "}";
    }

    // ── Factory helpers ───────────────────────────────────────────────────────

    /** Creates a fixed gold reward. */
    public static QuestReward gold(int amount) {
        return new QuestReward(RewardType.GOLD, String.valueOf(amount));
    }

    /** Creates a gold reward that scales with player alignment. */
    public static QuestReward scaledGold(int baseAmount) {
        return new QuestReward(RewardType.GOLD, String.valueOf(baseAmount), true);
    }

    /** Creates an experience reward. */
    public static QuestReward experience(int amount) {
        return new QuestReward(RewardType.EXPERIENCE, String.valueOf(amount));
    }

    /** Creates an item reward by item ID. */
    public static QuestReward item(String itemId) {
        return new QuestReward(RewardType.ITEM, itemId);
    }

    /** Creates an ability unlock reward. */
    public static QuestReward abilityUnlock(String abilityId) {
        return new QuestReward(RewardType.ABILITY_UNLOCK, abilityId);
    }

    /** Creates a relationship boost reward (e.g., {@code "merchant_thomas:10"}). */
    public static QuestReward relationship(String npcId, int delta) {
        return new QuestReward(RewardType.RELATIONSHIP, npcId + ":" + delta);
    }
}
