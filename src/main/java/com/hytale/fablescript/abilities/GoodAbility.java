package com.hytale.fablescript.abilities;

import java.util.List;

/**
 * Defines the built-in Good-alignment ability set.
 *
 * <p>Each constant represents a usable ability with its metadata.
 * Actual application of game effects uses the Hytale world API and is
 * marked with {@code // TODO: Hytale API} where specific calls are needed.</p>
 *
 * <p>All good abilities require a minimum alignment of +20 (configurable).</p>
 */
public enum GoodAbility implements FableAbility {

    HOLY_SHIELD(
            "holy_shield",
            "Holy Shield",
            "Creates a protective barrier that reflects 30% of incoming damage.",
            20, 45_000L, 60
    ),
    HEAL(
            "heal",
            "Heal",
            "Restores 40% health to self and up to 3 nearby allies within 8 blocks.",
            20, 20_000L, 40
    ),
    RESURRECTION(
            "resurrection",
            "Resurrection",
            "Revives a fallen nearby ally with 50% health. Long cooldown.",
            41, 300_000L, 80
    ),
    DIVINE_INTERVENTION(
            "divine_intervention",
            "Divine Intervention",
            "Negates the next critical hit against you. One use per life.",
            20, 180_000L, 50
    ),
    BLESSING(
            "blessing",
            "Blessing",
            "Grants nearby allies +20% speed and +15% damage for 30 seconds.",
            30, 90_000L, 70
    );

    private final String id;
    private final String displayName;
    private final String description;
    private final int requiredAlignment;
    private final long cooldownMillis;
    private final int manaCost;

    GoodAbility(String id, String displayName, String description,
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
    @Override public int getRequiredAlignment()  { return requiredAlignment; }
    @Override public long getCooldownMillis()    { return cooldownMillis; }
    @Override public int getManaCost()           { return manaCost; }
    @Override public AbilityCategory getCategory() { return AbilityCategory.GOOD; }

    /**
     * Applies this ability's effect for the given player.
     * Hytale world API calls are stubbed with TODO markers.
     *
     * @param playerUuid  UUID of the casting player
     * @param targetUuid  UUID of the target player (may be same as caster for self-abilities)
     */
    @Override
    public void apply(java.util.UUID playerUuid, java.util.UUID targetUuid) {
        switch (this) {
            case HOLY_SHIELD -> {
                // TODO: Hytale API — apply 30% damage reflection shield to playerUuid
            }
            case HEAL -> {
                // TODO: Hytale API — restore 40% health to playerUuid and nearby allies
            }
            case RESURRECTION -> {
                // TODO: Hytale API — check targetUuid is dead; revive with 50% health
            }
            case DIVINE_INTERVENTION -> {
                // TODO: Hytale API — register one-time critical-hit negation for playerUuid
            }
            case BLESSING -> {
                // TODO: Hytale API — apply speed/damage buff to playerUuid's nearby allies
            }
        }
    }

    /** @return all good abilities that require exactly the base threshold (no PURE_GOOD gate). */
    public static List<GoodAbility> baseGoodAbilities() {
        return List.of(HOLY_SHIELD, HEAL, DIVINE_INTERVENTION, BLESSING);
    }

    /** @return abilities exclusive to PURE_GOOD tier (requiredAlignment {@literal >=} 41). */
    public static List<GoodAbility> pureGoodAbilities() {
        return List.of(RESURRECTION);
    }
}
