package com.hytale.fablescript.storage;

import com.hytale.fablescript.utils.FableLogger;

import java.util.*;
import java.util.concurrent.*;

/**
 * High-level player data manager providing an async, cached facade over {@link DataPersistence}.
 *
 * <p>Keeps loaded profiles in a concurrent in-memory cache (populated on login,
 * evicted on logout). All database I/O runs on a dedicated background thread pool,
 * ensuring the main server thread is never blocked.</p>
 *
 * <p>A periodic auto-save task flushes dirty profiles every N minutes
 * as configured in {@code config.yml}.</p>
 */
public class PlayerDataManager {

    private final DataPersistence persistence;
    private final FableLogger logger;
    private final ExecutorService ioExecutor;
    private final ScheduledExecutorService saveScheduler;

    /** In-memory cache: UUID → profile. Populated on login, removed on logout. */
    private final Map<UUID, PlayerProfile> cache = new ConcurrentHashMap<>();

    /** UUIDs whose profiles have been modified since last save. */
    private final Set<UUID> dirtyProfiles = ConcurrentHashMap.newKeySet();

    /**
     * Constructs a PlayerDataManager.
     *
     * @param persistence           initialised DataPersistence backend
     * @param backupIntervalMinutes how often to auto-save dirty profiles (from config)
     * @param logger                plugin logger
     */
    public PlayerDataManager(DataPersistence persistence, int backupIntervalMinutes,
                              FableLogger logger) {
        this.persistence   = persistence;
        this.logger        = logger;
        this.ioExecutor    = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "FableScript-IO");
            t.setDaemon(true);
            return t;
        });
        this.saveScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "FableScript-AutoSave");
            t.setDaemon(true);
            return t;
        });
        saveScheduler.scheduleAtFixedRate(
                this::saveAllDirty,
                backupIntervalMinutes, backupIntervalMinutes, TimeUnit.MINUTES);
    }

    // ── Player lifecycle ──────────────────────────────────────────────────────

    /**
     * Loads (or creates) a player profile asynchronously on login.
     * The returned future resolves to the player's profile once loaded.
     *
     * @param uuid       player UUID
     * @param playerName player display name
     * @return CompletableFuture resolving to the loaded or newly-created profile
     */
    public CompletableFuture<PlayerProfile> onPlayerLogin(UUID uuid, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<PlayerProfile> stored = persistence.loadProfile(uuid);
            PlayerProfile profile = stored.orElseGet(() -> {
                logger.info("Creating new profile for " + playerName + " (" + uuid + ")");
                return new PlayerProfile(uuid, playerName);
            });
            profile.setPlayerName(playerName); // catch name changes
            profile.touchLastSeen();
            cache.put(uuid, profile);
            return profile;
        }, ioExecutor);
    }

    /**
     * Saves and evicts a player profile from cache on logout.
     *
     * @param uuid          player UUID
     * @param sessionSeconds seconds played in this session
     */
    public void onPlayerLogout(UUID uuid, long sessionSeconds) {
        PlayerProfile profile = cache.remove(uuid);
        if (profile == null) return;
        profile.addPlaytime(sessionSeconds);
        profile.touchLastSeen();
        ioExecutor.submit(() -> persistence.saveProfile(profile));
        dirtyProfiles.remove(uuid);
    }

    // ── Cache access ──────────────────────────────────────────────────────────

    /**
     * Returns the cached profile for an online player.
     *
     * @param uuid player UUID
     * @return Optional containing the profile if player is online (cached)
     */
    public Optional<PlayerProfile> getProfile(UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    /**
     * Returns the cached profile, throwing if absent (use for systems that
     * guarantee the player is online).
     *
     * @param uuid player UUID
     * @return profile, never null
     * @throws IllegalStateException if player is not in cache
     */
    public PlayerProfile requireProfile(UUID uuid) {
        PlayerProfile p = cache.get(uuid);
        if (p == null) throw new IllegalStateException("No profile cached for " + uuid);
        return p;
    }

    /**
     * Loads a profile for an offline player directly from the database (blocking).
     * Used by admin commands that need to inspect or modify offline player data.
     *
     * @param uuid player UUID
     * @return CompletableFuture resolving to an Optional containing the profile if found
     */
    public CompletableFuture<Optional<PlayerProfile>> loadOfflineProfile(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> persistence.loadProfile(uuid), ioExecutor);
    }

    // ── Mutation helpers ─────────────────────────────────────────────────────

    /**
     * Updates the alignment on a cached profile and marks it dirty for auto-save.
     *
     * @param uuid      player UUID (must be online)
     * @param alignment new alignment value, pre-clamped
     */
    public void updateAlignment(UUID uuid, int alignment) {
        getProfile(uuid).ifPresent(p -> {
            p.setAlignment(alignment);
            markDirty(uuid);
        });
    }

    /**
     * Updates an NPC relationship value on the cached profile and marks dirty.
     *
     * @param uuid  player UUID
     * @param npcId NPC identifier
     * @param value new relationship value (clamped internally by PlayerProfile)
     */
    public void updateNpcRelationship(UUID uuid, String npcId, int value) {
        getProfile(uuid).ifPresent(p -> {
            p.setNpcRelationship(npcId, value);
            markDirty(uuid);
        });
    }

    /**
     * Unlocks an ability on the player's profile.
     *
     * @param uuid      player UUID
     * @param abilityId ability to unlock
     */
    public void unlockAbility(UUID uuid, String abilityId) {
        getProfile(uuid).ifPresent(p -> {
            p.unlockAbility(abilityId);
            markDirty(uuid);
        });
    }

    /**
     * Marks a quest as completed.
     *
     * @param uuid    player UUID
     * @param questId quest to mark
     */
    public void markQuestCompleted(UUID uuid, String questId) {
        getProfile(uuid).ifPresent(p -> {
            p.markQuestCompleted(questId);
            markDirty(uuid);
        });
    }

    // ── Admin operations ──────────────────────────────────────────────────────

    /**
     * Resets all FableScript data for a player and persists the clean state.
     *
     * @param uuid       player UUID
     * @param playerName player display name
     * @return new clean profile
     */
    public PlayerProfile resetPlayer(UUID uuid, String playerName) {
        PlayerProfile fresh = new PlayerProfile(uuid, playerName);
        cache.put(uuid, fresh);
        ioExecutor.submit(() -> persistence.saveProfile(fresh));
        dirtyProfiles.remove(uuid);
        logger.info("Reset FableScript data for " + playerName + " (" + uuid + ")");
        return fresh;
    }

    /** Immediately saves all dirty cached profiles (used on server shutdown). */
    public void saveAll() {
        saveAllDirty();
        cache.forEach((uuid, profile) -> {
            if (!dirtyProfiles.contains(uuid)) return;
            persistence.saveProfile(profile);
        });
    }

    /** Shuts down background threads cleanly. */
    public void shutdown() {
        saveAll();
        saveScheduler.shutdown();
        ioExecutor.shutdown();
        persistence.close();
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void markDirty(UUID uuid) {
        dirtyProfiles.add(uuid);
    }

    private void saveAllDirty() {
        Set<UUID> toSave = new HashSet<>(dirtyProfiles);
        for (UUID uuid : toSave) {
            PlayerProfile profile = cache.get(uuid);
            if (profile != null) {
                persistence.saveProfile(profile);
                dirtyProfiles.remove(uuid);
            }
        }
        if (!toSave.isEmpty()) {
            logger.debug("Auto-saved " + toSave.size() + " dirty profiles.");
        }
    }
}
