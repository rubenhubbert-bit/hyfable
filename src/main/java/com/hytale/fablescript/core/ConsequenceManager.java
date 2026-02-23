package com.hytale.fablescript.core;

import com.hytale.fablescript.events.ConsequenceTriggeredEvent;
import com.hytale.fablescript.events.FableEventBus;
import com.hytale.fablescript.utils.FableLogger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Schedules and resolves delayed and cascading consequences of player moral choices.
 *
 * <p>Consequences fall into three categories:
 * <ul>
 *   <li><b>Immediate</b> – resolved in the same tick as the triggering action</li>
 *   <li><b>Delayed</b>   – scheduled to resolve after N in-game minutes</li>
 *   <li><b>Cascading</b> – resolved immediately but register further delayed events</li>
 * </ul>
 *
 * The manager uses a single-threaded {@link ScheduledExecutorService} to
 * fire delayed consequences without blocking the server thread.</p>
 */
public class ConsequenceManager {

    /** How often to scan for pending consequences that are due (seconds). */
    private static final long SCAN_INTERVAL_SECONDS = 30L;

    private final FableLogger logger;
    private final FableEventBus eventBus;
    private final ScheduledExecutorService scheduler;

    /** Pending delayed consequences keyed by UUID; multiple may exist per player. */
    private final Map<UUID, List<PendingConsequence>> pendingByPlayer = new ConcurrentHashMap<>();

    /**
     * @param logger   plugin logger
     * @param eventBus event bus used to publish ConsequenceTriggeredEvents
     */
    public ConsequenceManager(FableLogger logger, FableEventBus eventBus) {
        this.logger = logger;
        this.eventBus = eventBus;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "FableScript-Consequence-Scheduler");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::resolveMaturedConsequences,
                SCAN_INTERVAL_SECONDS, SCAN_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Registers an immediate consequence and fires it at once.
     *
     * @param playerUuid UUID of the responsible player
     * @param type       type of consequence to apply
     * @param payload    semicolon-delimited key=value data
     * @param zoneKey    world zone affected, or null for global
     */
    public void triggerImmediate(UUID playerUuid,
                                  ConsequenceTriggeredEvent.ConsequenceType type,
                                  String payload, String zoneKey) {
        ConsequenceTriggeredEvent event =
                new ConsequenceTriggeredEvent(playerUuid, type, payload, zoneKey);
        eventBus.publish(event);
        logger.debug("Immediate consequence fired: " + event);
    }

    /**
     * Schedules a consequence to resolve after a delay of {@code delayMinutes} minutes.
     *
     * @param playerUuid    UUID of the responsible player
     * @param type          type of consequence to apply at resolution time
     * @param payload       semicolon-delimited key=value data
     * @param zoneKey       world zone affected, or null for global
     * @param delayMinutes  minutes to wait before this consequence fires
     */
    public void scheduleDelayed(UUID playerUuid,
                                 ConsequenceTriggeredEvent.ConsequenceType type,
                                 String payload, String zoneKey, long delayMinutes) {
        long triggerEpoch = Instant.now().getEpochSecond() + (delayMinutes * 60L);
        PendingConsequence pending = new PendingConsequence(playerUuid, type,
                payload, zoneKey, triggerEpoch);
        pendingByPlayer.computeIfAbsent(playerUuid, k -> new CopyOnWriteArrayList<>())
                       .add(pending);
        logger.debug("Scheduled consequence in " + delayMinutes + "m for player " + playerUuid);
    }

    /**
     * Schedules a cascade of consequences from a single triggering action.
     * The first fires immediately, subsequent ones are staggered by {@code intervalMinutes}.
     *
     * @param playerUuid       UUID of the responsible player
     * @param types            ordered list of consequence types to fire
     * @param payloads         parallel list of payloads (must match types.size())
     * @param zoneKey          world zone affected
     * @param intervalMinutes  minutes between each cascading consequence
     */
    public void triggerCascade(UUID playerUuid,
                                List<ConsequenceTriggeredEvent.ConsequenceType> types,
                                List<String> payloads, String zoneKey, long intervalMinutes) {
        for (int i = 0; i < types.size(); i++) {
            if (i == 0) {
                triggerImmediate(playerUuid, types.get(i), payloads.get(i), zoneKey);
            } else {
                scheduleDelayed(playerUuid, types.get(i), payloads.get(i),
                        zoneKey, (long) i * intervalMinutes);
            }
        }
    }

    /**
     * Returns all pending (not yet triggered) consequences for a player.
     *
     * @param playerUuid UUID to query
     * @return unmodifiable list of pending consequences
     */
    public List<PendingConsequence> getPending(UUID playerUuid) {
        List<PendingConsequence> list = pendingByPlayer.get(playerUuid);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    /**
     * Cancels all pending consequences for a player (used on admin reset).
     *
     * @param playerUuid UUID to clear
     */
    public void clearPending(UUID playerUuid) {
        pendingByPlayer.remove(playerUuid);
        logger.info("Cleared all pending consequences for " + playerUuid);
    }

    /** Shuts down the background scheduler cleanly on plugin disable. */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /** Scans all pending consequences and fires any that have matured. */
    private void resolveMaturedConsequences() {
        long now = Instant.now().getEpochSecond();
        pendingByPlayer.forEach((uuid, list) -> {
            List<PendingConsequence> matured = list.stream()
                    .filter(p -> p.triggerEpochSecond() <= now)
                    .toList();
            matured.forEach(p -> {
                ConsequenceTriggeredEvent event = new ConsequenceTriggeredEvent(
                        p.playerUuid(), p.type(), p.payload(), p.zoneKey());
                eventBus.publish(event);
                logger.debug("Resolved delayed consequence: " + event);
            });
            list.removeAll(matured);
        });
    }

    /**
     * Immutable descriptor for a consequence that has not yet resolved.
     *
     * @param playerUuid        UUID of the responsible player
     * @param type              consequence type to apply on resolution
     * @param payload           key=value data string
     * @param zoneKey           zone affected, or null for global
     * @param triggerEpochSecond wall-clock epoch-second when this should fire
     */
    public record PendingConsequence(UUID playerUuid,
                                      ConsequenceTriggeredEvent.ConsequenceType type,
                                      String payload, String zoneKey,
                                      long triggerEpochSecond) {}
}
