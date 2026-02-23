package com.hytale.fablescript.npc;

import com.hytale.fablescript.core.MoralityAlignment;

/**
 * Calculates and describes the relationship state between a player and a specific NPC.
 *
 * <p>Relationship values range from -100 (hostile) to +100 (devoted).
 * The relationship tier name changes the dialogue and service options offered by the NPC.</p>
 */
public class NPCRelationship {

    /** Relationship thresholds and tier names. */
    public enum Tier {
        DEVOTED   ("Devoted",    60, 100),
        FRIENDLY  ("Friendly",   20,  59),
        NEUTRAL   ("Neutral",    -19, 19),
        UNFRIENDLY("Unfriendly", -59, -20),
        HOSTILE   ("Hostile",   -100, -60);

        private final String label;
        private final int min;
        private final int max;

        Tier(String label, int min, int max) {
            this.label = label; this.min = min; this.max = max;
        }

        /** @return display label for this relationship tier */
        public String getLabel() { return label; }

        /**
         * Returns the tier for a given relationship value.
         *
         * @param value relationship value [-100, 100]
         * @return matching Tier
         */
        public static Tier fromValue(int value) {
            if (value >= 60)  return DEVOTED;
            if (value >= 20)  return FRIENDLY;
            if (value >= -19) return NEUTRAL;
            if (value >= -59) return UNFRIENDLY;
            return HOSTILE;
        }
    }

    private final String npcId;
    private int value;

    /**
     * Creates a relationship tracker for a single NPC.
     *
     * @param npcId   NPC identifier
     * @param initial starting value from NPCDefinitions.startingRelationship
     */
    public NPCRelationship(String npcId, int initial) {
        this.npcId = npcId;
        this.value = Math.max(-100, Math.min(100, initial));
    }

    /**
     * Adjusts the relationship value by a delta, clamping to [-100, 100].
     *
     * @param delta positive = more friendly, negative = more hostile
     */
    public void adjust(int delta) {
        value = Math.max(-100, Math.min(100, value + delta));
    }

    /**
     * Calculates the alignment-based drift of this relationship.
     * Good NPCs drift away from evil players and toward good ones (+/- up to 2 per check).
     *
     * @param npcPersonality   personality string from NPCDefinitions
     * @param playerAlignment  player's current alignment value
     * @return delta to apply to this relationship based on alignment affinity
     */
    public int calculateAlignmentDrift(String npcPersonality, int playerAlignment) {
        MoralityAlignment tier = MoralityAlignment.fromValue(playerAlignment);
        return switch (npcPersonality) {
            case "Noble", "Idealistic" -> tier.isGood() ? 1 : tier.isEvil() ? -1 : 0;
            case "Greedy"              -> Math.abs(playerAlignment) > 40 ? -1 : 0; // hates extremes
            case "Vengeful"            -> tier == MoralityAlignment.PURE_EVIL ? -2 : 0;
            default                    -> 0; // Pragmatic and others — no drift
        };
    }

    /** @return NPC identifier */
    public String getNpcId() { return npcId; }

    /** @return current relationship value [-100, 100] */
    public int getValue() { return value; }

    /** @return human-readable relationship tier */
    public Tier getTier() { return Tier.fromValue(value); }

    /**
     * Returns a price multiplier for this NPC's shop services.
     * Hostile NPCs charge up to 50% more; devoted ones offer 20% discount.
     *
     * @return multiplier (e.g., 1.5 for hostile, 0.8 for devoted)
     */
    public double getPriceMultiplier() {
        return switch (getTier()) {
            case DEVOTED    -> 0.8;
            case FRIENDLY   -> 0.9;
            case NEUTRAL    -> 1.0;
            case UNFRIENDLY -> 1.25;
            case HOSTILE    -> 1.5;
        };
    }

    /**
     * Returns true if this NPC will refuse service to the player.
     * NPCs refuse when relationship is HOSTILE and their personality is not Greedy.
     *
     * @param npcPersonality personality string from NPCDefinitions
     * @return true if service should be denied
     */
    public boolean isServiceDenied(String npcPersonality) {
        return getTier() == Tier.HOSTILE && !"Greedy".equals(npcPersonality);
    }
}
