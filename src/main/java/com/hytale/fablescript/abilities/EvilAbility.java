package com.hytale.fablescript.abilities;

import java.util.List;
import java.util.UUID;

/**
 * Defines the built-in Evil-alignment ability set.
 *
 * <p>All evil abilities require an alignment of -20 or below (configurable).
 * PURE_EVIL abilities additionally require alignment -41 or below.</p>
 */
public enum EvilAbility implements FableAbility {

    LIFE_DRAIN(
            "life_drain",
            "Life Drain",
            "Drains 25% of the target's current health, healing yourself for the same amount.",
            -20, 15_000L, 35
    ),
    CURSE(
            "curse",
            "Curse",
            "Reduces the target's speed by 30% and damage by 20% for 20 seconds.",
            -20, 30_000L, 45
    ),
    SUMMON_UNDEAD(
            "summon_undead",
            "Summon Undead",
            "Summons an undead minion that fights for you for 60 seconds.",
            -30, 60_000L, 65
    ),
    MIND_CONTROL(
            "mind_control",
            "Mind Control",
            "Temporarily controls a non-player NPC for 15 seconds (weak targets only).",
            -41, 120_000L, 80
    ),
    CORRUPTION_WAVE(
            "corruption_wave",
            "Corruption Wave",
            "Unleashes a wave of corruption in a 10-block radius, applying Curse to all enemies.",
            -41, 90_000L, 90
    );

    private final String id;
    private final String displayName;
    private final String description;
    private final int requiredAlignment;
    private final long cooldownMillis;
    private final int manaCost;

    EvilAbility(String id, String displayName, String description,
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
    @Override public AbilityCategory getCategory() { return AbilityCategory.EVIL; }

    @Override
    public void apply(UUID playerUuid, UUID targetUuid) {
        switch (this) {
            case LIFE_DRAIN -> {
                // TODO: Hytale API — deal damage to targetUuid equal to 25% of their HP;
                //                    heal playerUuid for the same amount
            }
            case CURSE -> {
                // TODO: Hytale API — apply speed/damage debuff to targetUuid for 20s
            }
            case SUMMON_UNDEAD -> {
                // TODO: Hytale API — spawn undead entity at playerUuid's location,
                //                    allied to playerUuid, 60s lifespan
            }
            case MIND_CONTROL -> {
                // TODO: Hytale API — take control of a nearby NPC entity for 15s
            }
            case CORRUPTION_WAVE -> {
                // TODO: Hytale API — query entities within 10 blocks of playerUuid,
                //                    apply CURSE to all hostile/enemy entities
            }
        }
    }

    /** @return evil abilities available at base threshold (-20). */
    public static List<EvilAbility> baseEvilAbilities() {
        return List.of(LIFE_DRAIN, CURSE, SUMMON_UNDEAD);
    }

    /** @return abilities exclusive to PURE_EVIL tier (requiredAlignment {@literal <=} -41). */
    public static List<EvilAbility> pureEvilAbilities() {
        return List.of(MIND_CONTROL, CORRUPTION_WAVE);
    }
}
