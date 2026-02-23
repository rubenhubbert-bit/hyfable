package com.hytale.fablescript.events;

import com.hytale.fablescript.core.MoralityAlignment;

import java.util.UUID;

/**
 * Fired when a player's moral alignment value changes, including tier transitions.
 *
 * <p>Listeners use this event to apply visual effects, update NPC behaviors,
 * and enable or disable alignment-gated abilities.</p>
 */
public class AlignmentChangeEvent {

    private final UUID playerUuid;
    private final int previousValue;
    private final int newValue;
    private final MoralityAlignment previousTier;
    private final MoralityAlignment newTier;
    private final String cause;

    /**
     * Constructs an AlignmentChangeEvent with full tier information.
     *
     * @param playerUuid    UUID of the affected player
     * @param previousValue alignment value before this change
     * @param newValue      alignment value after this change
     * @param previousTier  alignment tier before this change
     * @param newTier       alignment tier after this change
     * @param cause         short description of what caused the change (for logging)
     */
    public AlignmentChangeEvent(UUID playerUuid, int previousValue, int newValue,
                                 MoralityAlignment previousTier, MoralityAlignment newTier,
                                 String cause) {
        this.playerUuid = playerUuid;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.previousTier = previousTier;
        this.newTier = newTier;
        this.cause = cause;
    }

    /** @return UUID of the player whose alignment changed */
    public UUID getPlayerUuid() { return playerUuid; }

    /** @return alignment value before this event */
    public int getPreviousValue() { return previousValue; }

    /** @return alignment value after this event */
    public int getNewValue() { return newValue; }

    /** @return alignment tier before this event */
    public MoralityAlignment getPreviousTier() { return previousTier; }

    /** @return alignment tier after this event */
    public MoralityAlignment getNewTier() { return newTier; }

    /** @return plain-text cause of this alignment change */
    public String getCause() { return cause; }

    /**
     * Returns true when this change causes the player to cross an alignment tier boundary.
     * Useful for triggering tier-specific effects (particles, NPC updates, ability unlocks).
     *
     * @return true if the player's tier changed
     */
    public boolean isTierChange() {
        return previousTier != newTier;
    }

    /**
     * Returns the net delta of this alignment change.
     *
     * @return newValue minus previousValue (positive = shift toward good)
     */
    public int getDelta() {
        return newValue - previousValue;
    }

    @Override
    public String toString() {
        return String.format("AlignmentChangeEvent{player=%s, %d->%d (%s->%s), cause='%s'}",
                playerUuid, previousValue, newValue, previousTier, newTier, cause);
    }
}
