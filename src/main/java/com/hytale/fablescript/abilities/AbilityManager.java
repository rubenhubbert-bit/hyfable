package com.hytale.fablescript.abilities;

import com.hytale.fablescript.storage.PlayerDataManager;
import com.hytale.fablescript.utils.FableLogger;
import com.hytale.fablescript.utils.MoralityCalculator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ability unlocking, cooldown enforcement, and usage processing for all players.
 *
 * <p>All three ability sets ({@link GoodAbility}, {@link NeutralAbility}, {@link EvilAbility})
 * are tracked here. On alignment change, this manager automatically unlocks/revokes
 * alignment-gated abilities and notifies the player.</p>
 */
public class AbilityManager {

    private final PlayerDataManager playerData;
    private final FableLogger logger;

    /** Config-derived alignment thresholds for ability gating. */
    private final int goodThreshold;
    private final int evilThreshold;

    /** Per-player cooldown tracking: playerUuid → abilityId → expiryMillis. */
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    /**
     * @param playerData      player data manager for profile access and persistence
     * @param goodThreshold   minimum alignment required for good abilities (e.g., +20)
     * @param evilThreshold   maximum alignment allowed for evil abilities (e.g., -20)
     * @param logger          plugin logger
     */
    public AbilityManager(PlayerDataManager playerData, int goodThreshold,
                           int evilThreshold, FableLogger logger) {
        this.playerData    = playerData;
        this.goodThreshold = goodThreshold;
        this.evilThreshold = evilThreshold;
        this.logger        = logger;
    }

    // ── Alignment-driven unlock/revoke ─────────────────────────────────────────

    /**
     * Recalculates which abilities a player should have unlocked based on their alignment.
     * Called when alignment changes tier. Unlocks newly eligible abilities and revokes
     * abilities the player can no longer use.
     *
     * @param playerUuid      player UUID
     * @param newAlignment    new alignment value after the change
     */
    public void recalculateAbilities(UUID playerUuid, int newAlignment) {
        playerData.getProfile(playerUuid).ifPresent(profile -> {
            // Neutral abilities: always available, unlock once
            Arrays.stream(NeutralAbility.values())
                  .forEach(a -> {
                      if (!profile.hasAbility(a.getId())) {
                          profile.unlockAbility(a.getId());
                          playerData.unlockAbility(playerUuid, a.getId());
                      }
                  });

            // Good abilities: unlock at >= goodThreshold, revoke below
            Arrays.stream(GoodAbility.values()).forEach(a -> {
                boolean eligible = MoralityCalculator.meetsAbilityRequirement(
                        newAlignment, a.getRequiredAlignment());
                if (eligible && !profile.hasAbility(a.getId())) {
                    profile.unlockAbility(a.getId());
                    playerData.unlockAbility(playerUuid, a.getId());
                    logger.debug("Unlocked good ability '" + a.getId() + "' for " + playerUuid);
                } else if (!eligible && profile.hasAbility(a.getId())) {
                    profile.revokeAbility(a.getId());
                    logger.debug("Revoked good ability '" + a.getId() + "' from " + playerUuid);
                }
            });

            // Evil abilities: unlock at <= evilThreshold, revoke above
            Arrays.stream(EvilAbility.values()).forEach(a -> {
                boolean eligible = MoralityCalculator.meetsAbilityRequirement(
                        newAlignment, a.getRequiredAlignment());
                if (eligible && !profile.hasAbility(a.getId())) {
                    profile.unlockAbility(a.getId());
                    playerData.unlockAbility(playerUuid, a.getId());
                    logger.debug("Unlocked evil ability '" + a.getId() + "' for " + playerUuid);
                } else if (!eligible && profile.hasAbility(a.getId())) {
                    profile.revokeAbility(a.getId());
                    logger.debug("Revoked evil ability '" + a.getId() + "' from " + playerUuid);
                }
            });
        });
    }

    // ── Ability usage ─────────────────────────────────────────────────────────

    /**
     * Attempts to use an ability for a player, enforcing unlock status and cooldowns.
     *
     * @param playerUuid  UUID of the casting player
     * @param targetUuid  UUID of the target (may equal playerUuid for self-abilities)
     * @param abilityId   ID of the ability to use
     * @return UseResult describing success or reason for failure
     */
    public UseResult useAbility(UUID playerUuid, UUID targetUuid, String abilityId) {
        Optional<FableAbility> abilityOpt = resolveAbility(abilityId);
        if (abilityOpt.isEmpty()) {
            return UseResult.failure("Unknown ability: " + abilityId);
        }
        FableAbility ability = abilityOpt.get();

        // Check unlock status
        boolean unlocked = playerData.getProfile(playerUuid)
                .map(p -> p.hasAbility(abilityId)).orElse(false);
        if (!unlocked) {
            String req = alignmentRequirementText(ability);
            return UseResult.failure("Ability locked. Requires " + req + ".");
        }

        // Check cooldown
        long now = System.currentTimeMillis();
        Long expiry = cooldowns.getOrDefault(playerUuid, Map.of()).get(abilityId);
        if (expiry != null && now < expiry) {
            long remaining = (expiry - now) / 1000L;
            return UseResult.failure("Cooldown: " + remaining + "s remaining.");
        }

        // Apply effect
        ability.apply(playerUuid, targetUuid);
        setCooldown(playerUuid, abilityId, now + ability.getCooldownMillis());
        logger.debug("Player " + playerUuid + " used ability '" + abilityId + "'");
        return UseResult.success(ability);
    }

    /**
     * Returns all abilities (unlocked and locked) with their availability state for a player.
     *
     * @param playerUuid      player UUID
     * @param playerAlignment current alignment (for filtering locked reason)
     * @return list of AbilityInfo records
     */
    public List<AbilityInfo> listAbilities(UUID playerUuid, int playerAlignment) {
        List<AbilityInfo> result = new ArrayList<>();
        boolean hasProfile = playerData.getProfile(playerUuid).isPresent();

        for (FableAbility a : allAbilities()) {
            boolean unlocked = hasProfile && playerData.getProfile(playerUuid)
                    .map(p -> p.hasAbility(a.getId())).orElse(false);
            long cooldownRemaining = getCooldownRemaining(playerUuid, a.getId());
            result.add(new AbilityInfo(a, unlocked, cooldownRemaining));
        }
        return result;
    }

    /** Clears all cooldowns for a player (admin reset). */
    public void clearCooldowns(UUID playerUuid) {
        cooldowns.remove(playerUuid);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Optional<FableAbility> resolveAbility(String abilityId) {
        for (FableAbility a : allAbilities()) {
            if (a.getId().equals(abilityId)) return Optional.of(a);
        }
        return Optional.empty();
    }

    private List<FableAbility> allAbilities() {
        List<FableAbility> all = new ArrayList<>();
        all.addAll(Arrays.asList(NeutralAbility.values()));
        all.addAll(Arrays.asList(GoodAbility.values()));
        all.addAll(Arrays.asList(EvilAbility.values()));
        return all;
    }

    private void setCooldown(UUID playerUuid, String abilityId, long expiryMillis) {
        cooldowns.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
                 .put(abilityId, expiryMillis);
    }

    private long getCooldownRemaining(UUID playerUuid, String abilityId) {
        Long expiry = cooldowns.getOrDefault(playerUuid, Map.of()).get(abilityId);
        if (expiry == null) return 0L;
        long remaining = expiry - System.currentTimeMillis();
        return Math.max(0L, remaining / 1000L);
    }

    private String alignmentRequirementText(FableAbility ability) {
        int req = ability.getRequiredAlignment();
        if (req == 0) return "any alignment";
        if (req > 0)  return "alignment +" + req + " or above";
        return "alignment " + req + " or below";
    }

    // ── Result and info types ─────────────────────────────────────────────────

    /**
     * Result of an ability use attempt.
     *
     * @param success       true if the ability was successfully applied
     * @param ability       the ability that was used, or null on failure
     * @param failureReason human-readable reason for failure, or null on success
     */
    public record UseResult(boolean success, FableAbility ability, String failureReason) {
        static UseResult success(FableAbility a) { return new UseResult(true, a, null); }
        static UseResult failure(String reason)  { return new UseResult(false, null, reason); }
    }

    /**
     * Describes an ability's state for a specific player.
     *
     * @param ability           the ability definition
     * @param unlocked          true if the player currently has access
     * @param cooldownRemaining seconds remaining on cooldown (0 = ready)
     */
    public record AbilityInfo(FableAbility ability, boolean unlocked, long cooldownRemaining) {
        /** @return true if this ability is ready to use (unlocked and not on cooldown) */
        public boolean isReady() { return unlocked && cooldownRemaining == 0; }
    }
}
