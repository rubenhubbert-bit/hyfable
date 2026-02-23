package com.hytale.fablescript.quests;

import java.util.List;

/**
 * Describes a single quest with one or more moral-path choices.
 *
 * <p>A quest may be completed via different {@link QuestChoice} paths (good/neutral/evil),
 * each with different alignment impacts and rewards. Quest availability can be gated
 * by the player's alignment tier and prerequisite quest completions.</p>
 */
public class Quest {

    /** Tracks a player's progress through this specific quest. */
    public enum Status { NOT_STARTED, IN_PROGRESS, COMPLETED, FAILED }

    private final String id;
    private final String title;
    private final String description;
    private final String giverNpcId;

    /** Minimum alignment required to accept this quest (-100 to +100). */
    private final int minAlignment;

    /** Maximum alignment allowed to accept this quest (-100 to +100). */
    private final int maxAlignment;

    /** Quest IDs that must be completed before this one becomes available. */
    private final List<String> prerequisites;

    /** All available moral-path choices for completing this quest. */
    private final List<QuestChoice> choices;

    /** Whether this quest can be reset (admin or redemption mechanic). */
    private final boolean resettable;

    private Quest(Builder b) {
        this.id            = b.id;
        this.title         = b.title;
        this.description   = b.description;
        this.giverNpcId    = b.giverNpcId;
        this.minAlignment  = b.minAlignment;
        this.maxAlignment  = b.maxAlignment;
        this.prerequisites = List.copyOf(b.prerequisites);
        this.choices       = List.copyOf(b.choices);
        this.resettable    = b.resettable;
    }

    /**
     * Returns true if a player with the given alignment and completed quests can accept this quest.
     *
     * @param playerAlignment  current player alignment value
     * @param completedQuests  set of quest IDs the player has already completed
     * @return true if all conditions are met
     */
    public boolean isAvailableTo(int playerAlignment, java.util.Set<String> completedQuests) {
        if (playerAlignment < minAlignment || playerAlignment > maxAlignment) return false;
        return completedQuests.containsAll(prerequisites);
    }

    /**
     * Returns choices available to a player at their current alignment.
     *
     * @param playerAlignment player's current alignment value
     * @return filtered list of eligible QuestChoices
     */
    public List<QuestChoice> getAvailableChoices(int playerAlignment) {
        return choices.stream()
                .filter(c -> c.isAvailableTo(playerAlignment))
                .toList();
    }

    /**
     * Finds a choice by its ID.
     *
     * @param choiceId the choice identifier
     * @return matching QuestChoice, or null if not found
     */
    public QuestChoice findChoice(String choiceId) {
        return choices.stream()
                .filter(c -> c.getId().equals(choiceId))
                .findFirst()
                .orElse(null);
    }

    /** @return unique quest identifier */
    public String getId() { return id; }

    /** @return display title shown to players */
    public String getTitle() { return title; }

    /** @return long-form quest description */
    public String getDescription() { return description; }

    /** @return NPC ID of the quest giver, or null if world-triggered */
    public String getGiverNpcId() { return giverNpcId; }

    /** @return minimum alignment to accept this quest */
    public int getMinAlignment() { return minAlignment; }

    /** @return maximum alignment to accept this quest */
    public int getMaxAlignment() { return maxAlignment; }

    /** @return prerequisite quest IDs required before this quest is offered */
    public List<String> getPrerequisites() { return prerequisites; }

    /** @return all moral-path choices for this quest */
    public List<QuestChoice> getChoices() { return choices; }

    /** @return true if this quest can be reset via admin command or redemption */
    public boolean isResettable() { return resettable; }

    // ── Builder ──────────────────────────────────────────────────────────────

    /** @return a new Builder for the given quest ID */
    public static Builder builder(String id) { return new Builder(id); }

    /** Fluent builder for Quest. */
    public static final class Builder {
        private final String id;
        private String title = "Untitled Quest";
        private String description = "";
        private String giverNpcId;
        private int minAlignment = -100;
        private int maxAlignment = 100;
        private List<String> prerequisites = List.of();
        private List<QuestChoice> choices = List.of();
        private boolean resettable = false;

        private Builder(String id) { this.id = id; }

        public Builder title(String v)                { title = v; return this; }
        public Builder description(String v)          { description = v; return this; }
        public Builder giverNpc(String v)             { giverNpcId = v; return this; }
        public Builder minAlignment(int v)            { minAlignment = v; return this; }
        public Builder maxAlignment(int v)            { maxAlignment = v; return this; }
        public Builder prerequisites(List<String> v)  { prerequisites = v; return this; }
        public Builder choices(List<QuestChoice> v)   { choices = v; return this; }
        public Builder resettable(boolean v)          { resettable = v; return this; }

        /** @return built Quest */
        public Quest build() { return new Quest(this); }
    }
}
