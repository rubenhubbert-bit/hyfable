package com.hytale.fablescript.abilities;

import java.util.UUID;

/**
 * Common interface implemented by all FableScript ability enums
 * ({@link GoodAbility}, {@link NeutralAbility}, {@link EvilAbility}).
 *
 * <p>Enables {@link AbilityManager} to handle all ability types through
 * a single polymorphic API without casting.</p>
 */
public interface FableAbility {

    /**
     * Moral category of this ability, used to determine alignment gating direction.
     */
    enum AbilityCategory { GOOD, NEUTRAL, EVIL }

    /** @return unique ability identifier (snake_case, e.g., "holy_shield") */
    String getId();

    /** @return display name shown to players in {@code /abilities} */
    String getDisplayName();

    /** @return description shown in the ability list */
    String getDescription();

    /**
     * Returns the alignment gate for this ability.
     * <ul>
     *   <li>Positive → player alignment must be {@literal >=} this value (good abilities)</li>
     *   <li>Negative → player alignment must be {@literal <=} this value (evil abilities)</li>
     *   <li>Zero    → no alignment requirement (neutral abilities)</li>
     * </ul>
     *
     * @return required alignment threshold
     */
    int getRequiredAlignment();

    /** @return milliseconds before this ability can be used again */
    long getCooldownMillis();

    /** @return mana (or equivalent resource) consumed on use */
    int getManaCost();

    /** @return the moral category this ability belongs to */
    AbilityCategory getCategory();

    /**
     * Applies this ability's game effect.
     * Implementations should call Hytale world API methods.
     *
     * @param playerUuid UUID of the player casting this ability
     * @param targetUuid UUID of the target entity (may equal playerUuid for self-abilities)
     */
    void apply(UUID playerUuid, UUID targetUuid);

    /**
     * Returns a human-readable cooldown duration string.
     *
     * @return e.g., "45s" or "5m 00s"
     */
    default String getCooldownDisplay() {
        long secs = getCooldownMillis() / 1000L;
        if (secs >= 60) {
            return String.format("%dm %02ds", secs / 60, secs % 60);
        }
        return secs + "s";
    }
}
