package com.hytale.fablescript.npc;

import com.hytale.fablescript.config.NPCDefinitions;
import com.hytale.fablescript.core.MoralityAlignment;

/**
 * Determines an NPC's behavioural state toward a specific player,
 * given that player's alignment and the NPC's relationship value.
 *
 * <p>This is a pure-function utility class — it holds no mutable state
 * and is safe to call from multiple threads simultaneously.</p>
 */
public class NPCBehavior {

    /**
     * Describes how an NPC currently behaves toward a player.
     *
     * @param greeting           opening line the NPC says when approached
     * @param offersService      whether the NPC will transact with the player
     * @param offersQuests       whether the NPC will offer quests to the player
     * @param fleeOnSight        whether the NPC runs away when the player is nearby
     * @param attackOnSight      whether the NPC initiates combat on sight
     * @param priceMultiplier    shop price multiplier (e.g., 1.5 = 50% surcharge)
     */
    public record BehaviorState(String greeting, boolean offersService, boolean offersQuests,
                                 boolean fleeOnSight, boolean attackOnSight,
                                 double priceMultiplier) {}

    /**
     * Computes the current NPC behaviour state toward a player.
     *
     * @param entry              NPC definition from npcs.yml
     * @param relationship       current relationship with this player
     * @param playerAlignment    player's current alignment value
     * @return computed BehaviorState for use by interaction handlers
     */
    public BehaviorState compute(NPCDefinitions.NPCEntry entry,
                                  NPCRelationship relationship,
                                  int playerAlignment) {
        MoralityAlignment tier = MoralityAlignment.fromValue(playerAlignment);
        NPCRelationship.Tier relTier = relationship.getTier();

        boolean attackOnSight = entry.hostileToEvil() && tier == MoralityAlignment.PURE_EVIL
                             && relTier == NPCRelationship.Tier.HOSTILE;

        boolean fleeOnSight = tier.isEvil()
                           && relTier == NPCRelationship.Tier.HOSTILE
                           && !entry.hostileToEvil();

        boolean offersService = !relationship.isServiceDenied(entry.personality())
                             && !attackOnSight;

        boolean offersQuests = offersService
                            && relTier != NPCRelationship.Tier.UNFRIENDLY
                            && relTier != NPCRelationship.Tier.HOSTILE;

        String greeting = buildGreeting(entry, tier, relTier);

        return new BehaviorState(greeting, offersService, offersQuests,
                fleeOnSight, attackOnSight, relationship.getPriceMultiplier());
    }

    /**
     * Calculates how much the NPC's relationship should shift after an alignment-tier change.
     * Called by MoralityEventListener when a player's tier changes.
     *
     * @param entry             NPC definition
     * @param oldTier           the player's previous alignment tier
     * @param newTier           the player's new alignment tier
     * @return relationship delta to apply (-10 to +10)
     */
    public int alignmentShiftDelta(NPCDefinitions.NPCEntry entry,
                                    MoralityAlignment oldTier, MoralityAlignment newTier) {
        if (!entry.reactToAlignment()) return 0;
        int delta = 0;
        // Noble/Idealistic NPCs reward good shifts, penalise evil ones
        if ("Noble".equals(entry.personality()) || "Idealistic".equals(entry.personality())) {
            if (newTier.isGood() && !oldTier.isGood())  delta = +5;
            if (newTier.isEvil() && !oldTier.isEvil())  delta = -5;
            if (newTier == MoralityAlignment.PURE_GOOD)  delta = +10;
            if (newTier == MoralityAlignment.PURE_EVIL)  delta = -10;
        }
        return delta;
    }

    private String buildGreeting(NPCDefinitions.NPCEntry entry, MoralityAlignment tier,
                                  NPCRelationship.Tier relTier) {
        String name = entry.displayName();
        return switch (relTier) {
            case DEVOTED    -> "Ah, my dearest friend! It warms my heart to see you, "
                               + (tier.isGood() ? "guardian of the light!" : "traveller.") ;
            case FRIENDLY   -> "Welcome back! Always a pleasure, traveller.";
            case NEUTRAL    -> tier.isEvil()
                    ? "...What do you want." : "Greetings. How can I help?";
            case UNFRIENDLY -> "You again. Make it quick.";
            case HOSTILE    -> tier == MoralityAlignment.PURE_EVIL
                    ? name + " recoils in fear, hand on weapon..."
                    : "I have nothing to say to you. Leave.";
        };
    }
}
