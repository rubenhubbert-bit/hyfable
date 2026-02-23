package com.hytale.fablescript.abilities;

import java.util.UUID;

/**
 * Defines the neutral ability set, available to all players regardless of alignment.
 *
 * <p>Neutral abilities are the baseline toolkit every player has access to.
 * They do not grant alignment bonuses or penalties when used.</p>
 */
public enum NeutralAbility implements FableAbility {

    FIREBALL(
            "fireball",
            "Fireball",
            "Launches a fireball that deals moderate damage on impact.",
            0, 8_000L, 30
    ),
    TELEPORT(
            "teleport",
            "Teleport",
            "Instantly moves you up to 20 blocks in the direction you are facing.",
            0, 12_000L, 25
    ),
    DETECT(
            "detect",
            "Detect",
            "Reveals hidden entities and players within a 15-block radius for 10 seconds.",
            0, 20_000L, 20
    ),
    BARRIER(
            "barrier",
            "Barrier",
            "Creates a personal shield absorbing up to 50 damage before breaking.",
            0, 25_000L, 40
    ),
    AMPLIFY(
            "amplify",
            "Amplify",
            "Boosts the damage of your next spell by 50%.",
            0, 15_000L, 35
    );

    private final String id;
    private final String displayName;
    private final String description;
    private final int requiredAlignment;
    private final long cooldownMillis;
    private final int manaCost;

    NeutralAbility(String id, String displayName, String description,
                    int requiredAlignment, long cooldownMillis, int manaCost) {
        this.id                = id;
        this.displayName       = displayName;
        this.description       = description;
        this.requiredAlignment = requiredAlignment;
        this.cooldownMillis    = cooldownMillis;
        this.manaCost          = manaCost;
    }

    @Override public String getId()              { return id; }
    @Override public String getDisplayName()     { return displayName; }
    @Override public String getDescription()     { return description; }
    @Override public int getRequiredAlignment()  { return 0; /* always 0 for neutral */ }
    @Override public long getCooldownMillis()    { return cooldownMillis; }
    @Override public int getManaCost()           { return manaCost; }
    @Override public AbilityCategory getCategory() { return AbilityCategory.NEUTRAL; }

    @Override
    public void apply(UUID playerUuid, UUID targetUuid) {
        switch (this) {
            case FIREBALL   -> { /* TODO: Hytale API — fire projectile toward targetUuid */ }
            case TELEPORT   -> { /* TODO: Hytale API — teleport playerUuid 20 blocks forward */ }
            case DETECT     -> { /* TODO: Hytale API — highlight hidden entities near playerUuid */ }
            case BARRIER    -> { /* TODO: Hytale API — apply 50-damage absorption shield */ }
            case AMPLIFY    -> { /* TODO: Hytale API — set next-spell damage multiplier 1.5x */ }
        }
    }
}
