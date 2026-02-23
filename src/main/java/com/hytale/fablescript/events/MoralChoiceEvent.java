package com.hytale.fablescript.events;

import java.util.UUID;

/**
 * Fired when a player makes a choice that carries moral weight.
 * Published through {@link FableEventBus} and consumed by MoralityEventListener.
 *
 * <p>Example: player chooses to spare a bandit (+3 alignment, GOOD) or
 * execute the bandit for gold (-5 alignment, EVIL).</p>
 */
public class MoralChoiceEvent {

    /** The unique id of the player making the choice. */
    private final UUID playerUuid;

    /** Human-readable description of the choice made. */
    private final String choiceDescription;

    /** Context key linking this choice to a quest or dialogue node. */
    private final String contextKey;

    /** Alignment delta applied as a result of this choice (positive = good, negative = evil). */
    private final int alignmentDelta;

    /** Whether this choice was a major moral decision (±10 or more). */
    private final boolean majorChoice;

    /**
     * Constructs a new MoralChoiceEvent.
     *
     * @param playerUuid         UUID of the acting player
     * @param choiceDescription  short description of the decision
     * @param contextKey         quest/dialogue identifier for traceability
     * @param alignmentDelta     alignment points awarded (negative = evil)
     * @param majorChoice        true if |alignmentDelta| {@literal >=} 10
     */
    public MoralChoiceEvent(UUID playerUuid, String choiceDescription,
                             String contextKey, int alignmentDelta, boolean majorChoice) {
        this.playerUuid = playerUuid;
        this.choiceDescription = choiceDescription;
        this.contextKey = contextKey;
        this.alignmentDelta = alignmentDelta;
        this.majorChoice = majorChoice;
    }

    /** @return UUID of the player who made this choice */
    public UUID getPlayerUuid() { return playerUuid; }

    /** @return human-readable description shown in history logs */
    public String getChoiceDescription() { return choiceDescription; }

    /** @return quest/dialogue key for consequence lookup */
    public String getContextKey() { return contextKey; }

    /** @return alignment points applied (positive = good, negative = evil) */
    public int getAlignmentDelta() { return alignmentDelta; }

    /** @return true if this is a major moral event (|delta| {@literal >=} 10) */
    public boolean isMajorChoice() { return majorChoice; }

    @Override
    public String toString() {
        return String.format("MoralChoiceEvent{player=%s, delta=%+d, context='%s', major=%b}",
                playerUuid, alignmentDelta, contextKey, majorChoice);
    }
}
