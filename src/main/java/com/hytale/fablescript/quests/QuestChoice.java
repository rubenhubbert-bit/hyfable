package com.hytale.fablescript.quests;

import java.util.List;

/**
 * Represents a single branching choice point within a quest.
 *
 * <p>Each QuestChoice has an alignment impact, a set of requirements that
 * must be met for it to appear, and a list of rewards granted on completion
 * via this path.</p>
 */
public class QuestChoice {

    /** Moral path identifier — controls which narrative arc this choice belongs to. */
    public enum MoralPath { GOOD, NEUTRAL, EVIL }

    private final String id;
    private final String description;
    private final MoralPath moralPath;
    private final int alignmentDelta;
    private final int minAlignmentRequired;
    private final int maxAlignmentRequired;
    private final List<QuestReward> rewards;

    /** Optional consequence key fired when this choice is taken (may be null). */
    private final String consequenceKey;

    /** ID of the next quest in a chain triggered by this choice (may be null). */
    private final String nextQuestId;

    /**
     * Full constructor; use {@link Builder} for readable construction.
     */
    private QuestChoice(Builder b) {
        this.id                    = b.id;
        this.description           = b.description;
        this.moralPath             = b.moralPath;
        this.alignmentDelta        = b.alignmentDelta;
        this.minAlignmentRequired  = b.minAlignmentRequired;
        this.maxAlignmentRequired  = b.maxAlignmentRequired;
        this.rewards               = List.copyOf(b.rewards);
        this.consequenceKey        = b.consequenceKey;
        this.nextQuestId           = b.nextQuestId;
    }

    /**
     * Returns true if a player with the given alignment is eligible to take this choice.
     *
     * @param playerAlignment current alignment value
     * @return true if within the configured range
     */
    public boolean isAvailableTo(int playerAlignment) {
        return playerAlignment >= minAlignmentRequired
            && playerAlignment <= maxAlignmentRequired;
    }

    /** @return unique choice identifier within its parent quest */
    public String getId() { return id; }

    /** @return player-visible description of this choice */
    public String getDescription() { return description; }

    /** @return moral narrative path this choice belongs to */
    public MoralPath getMoralPath() { return moralPath; }

    /** @return alignment delta applied when this choice is taken */
    public int getAlignmentDelta() { return alignmentDelta; }

    /** @return minimum alignment required to select this option */
    public int getMinAlignmentRequired() { return minAlignmentRequired; }

    /** @return maximum alignment allowed to select this option */
    public int getMaxAlignmentRequired() { return maxAlignmentRequired; }

    /** @return list of rewards granted for completing the quest via this choice */
    public List<QuestReward> getRewards() { return rewards; }

    /** @return consequence trigger key, or null if none */
    public String getConsequenceKey() { return consequenceKey; }

    /** @return next quest ID in a quest chain, or null if standalone */
    public String getNextQuestId() { return nextQuestId; }

    /** @return true if this choice triggers a world consequence */
    public boolean hasConsequence() { return consequenceKey != null && !consequenceKey.isBlank(); }

    /** @return true if this choice unlocks a follow-up quest */
    public boolean chainsToQuest() { return nextQuestId != null && !nextQuestId.isBlank(); }

    // ── Builder ──────────────────────────────────────────────────────────────

    /** @return a new Builder for a QuestChoice */
    public static Builder builder(String id) { return new Builder(id); }

    /** Fluent builder for QuestChoice. */
    public static final class Builder {
        private final String id;
        private String description = "";
        private MoralPath moralPath = MoralPath.NEUTRAL;
        private int alignmentDelta = 0;
        private int minAlignmentRequired = -100;
        private int maxAlignmentRequired = 100;
        private List<QuestReward> rewards = List.of();
        private String consequenceKey;
        private String nextQuestId;

        private Builder(String id) { this.id = id; }

        public Builder description(String v)            { description = v; return this; }
        public Builder moralPath(MoralPath v)           { moralPath = v; return this; }
        public Builder alignmentDelta(int v)            { alignmentDelta = v; return this; }
        public Builder minAlignment(int v)              { minAlignmentRequired = v; return this; }
        public Builder maxAlignment(int v)              { maxAlignmentRequired = v; return this; }
        public Builder rewards(List<QuestReward> v)     { rewards = v; return this; }
        public Builder consequenceKey(String v)         { consequenceKey = v; return this; }
        public Builder nextQuestId(String v)            { nextQuestId = v; return this; }

        /** @return built QuestChoice */
        public QuestChoice build() { return new QuestChoice(this); }
    }
}
