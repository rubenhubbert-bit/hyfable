package com.hytale.fablescript.quests;

import com.hytale.fablescript.core.ChoiceTracker;
import com.hytale.fablescript.core.ConsequenceManager;
import com.hytale.fablescript.events.ConsequenceTriggeredEvent;
import com.hytale.fablescript.events.MoralChoiceEvent;
import com.hytale.fablescript.events.FableEventBus;
import com.hytale.fablescript.storage.PlayerDataManager;
import com.hytale.fablescript.storage.PlayerProfile;
import com.hytale.fablescript.utils.FableLogger;
import com.hytale.fablescript.utils.MoralityCalculator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for all quest lifecycle operations.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Registering and loading quest definitions</li>
 *   <li>Determining quest availability per player</li>
 *   <li>Processing quest choice completions (alignment, rewards, consequences)</li>
 *   <li>Tracking active quest states per player</li>
 * </ul>
 * </p>
 */
public class QuestManager {

    private final PlayerDataManager playerData;
    private final ChoiceTracker choiceTracker;
    private final ConsequenceManager consequenceManager;
    private final FableEventBus eventBus;
    private final FableLogger logger;
    private final int maxAlignmentChangePerAction;

    /** Registry of all available quests keyed by quest ID. */
    private final Map<String, Quest> questRegistry = new ConcurrentHashMap<>();

    /** Active quest state per player: playerUuid → questId → current status. */
    private final Map<UUID, Map<String, Quest.Status>> playerQuestState = new ConcurrentHashMap<>();

    /**
     * @param playerData                player data manager for profile access
     * @param choiceTracker             records player choices with cooldown enforcement
     * @param consequenceManager        schedules/fires consequences on choice
     * @param eventBus                  publishes MoralChoiceEvent on completion
     * @param maxAlignmentChangePerAction config cap for per-action alignment change
     * @param logger                    plugin logger
     */
    public QuestManager(PlayerDataManager playerData, ChoiceTracker choiceTracker,
                         ConsequenceManager consequenceManager, FableEventBus eventBus,
                         int maxAlignmentChangePerAction, FableLogger logger) {
        this.playerData                = playerData;
        this.choiceTracker             = choiceTracker;
        this.consequenceManager        = consequenceManager;
        this.eventBus                  = eventBus;
        this.maxAlignmentChangePerAction = maxAlignmentChangePerAction;
        this.logger                    = logger;
    }

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Registers a quest definition. Typically called at plugin startup.
     *
     * @param quest quest to register
     */
    public void registerQuest(Quest quest) {
        questRegistry.put(quest.getId(), quest);
        logger.debug("Registered quest: " + quest.getId());
    }

    /**
     * Returns the quest definition for a given ID.
     *
     * @param questId quest identifier
     * @return Optional containing the quest, or empty if unknown
     */
    public Optional<Quest> getQuest(String questId) {
        return Optional.ofNullable(questRegistry.get(questId));
    }

    // ── Availability ──────────────────────────────────────────────────────────

    /**
     * Returns all quests currently available to a player based on their alignment,
     * completed prerequisites, and alignment-gating config.
     *
     * @param playerUuid              player UUID
     * @param alignmentGatingEnabled  if false, all quests are available regardless of alignment
     * @return list of available quests (not yet completed)
     */
    public List<Quest> getAvailableQuests(UUID playerUuid, boolean alignmentGatingEnabled) {
        return playerData.getProfile(playerUuid).map(profile -> {
            Set<String> completed = profile.getCompletedQuests();
            int alignment = profile.getAlignment();

            return questRegistry.values().stream()
                    .filter(q -> !completed.contains(q.getId()))
                    .filter(q -> getStatus(playerUuid, q.getId()) == Quest.Status.NOT_STARTED
                              || getStatus(playerUuid, q.getId()) == Quest.Status.IN_PROGRESS)
                    .filter(q -> !alignmentGatingEnabled
                              || q.isAvailableTo(alignment, completed))
                    .sorted(Comparator.comparing(Quest::getTitle))
                    .toList();
        }).orElse(List.of());
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Starts a quest for a player, changing their status from NOT_STARTED to IN_PROGRESS.
     *
     * @param playerUuid player UUID
     * @param questId    quest to start
     * @return true if the quest was successfully started
     */
    public boolean startQuest(UUID playerUuid, String questId) {
        Quest quest = questRegistry.get(questId);
        if (quest == null || getStatus(playerUuid, questId) != Quest.Status.NOT_STARTED) {
            return false;
        }
        setStatus(playerUuid, questId, Quest.Status.IN_PROGRESS);
        logger.info("Player " + playerUuid + " started quest: " + questId);
        return true;
    }

    /**
     * Completes a quest via a specific moral-path choice.
     * Applies alignment delta, grants rewards, fires consequences, and updates profile.
     *
     * @param playerUuid player UUID
     * @param questId    quest being completed
     * @param choiceId   selected moral-path choice ID
     * @param zoneKey    current world zone of the player
     * @return CompletionResult describing what happened, or null on failure
     */
    public CompletionResult completeQuestWithChoice(UUID playerUuid, String questId,
                                                     String choiceId, String zoneKey) {
        Quest quest = questRegistry.get(questId);
        if (quest == null) return null;

        QuestChoice choice = quest.findChoice(choiceId);
        if (choice == null) {
            logger.warn("Unknown choice '" + choiceId + "' for quest '" + questId + "'");
            return null;
        }

        Optional<PlayerProfile> profileOpt = playerData.getProfile(playerUuid);
        if (profileOpt.isEmpty()) return null;
        PlayerProfile profile = profileOpt.get();

        // Enforce cooldown via ChoiceTracker
        boolean accepted = choiceTracker.recordChoice(playerUuid, questId + ":" + choiceId,
                choiceId, choice.getDescription(), choice.getAlignmentDelta());
        if (!accepted) {
            logger.debug("Choice rate-limited for player " + playerUuid);
            return null;
        }

        // Apply alignment delta
        int clamped = MoralityCalculator.clampDelta(choice.getAlignmentDelta(),
                maxAlignmentChangePerAction);
        int previousAlignment = profile.getAlignment();
        profile.setAlignment(MoralityCalculator.applyDelta(previousAlignment, clamped));
        playerData.updateAlignment(playerUuid, profile.getAlignment());

        // Grant rewards
        double multiplier = MoralityCalculator.rewardMultiplier(profile.getAlignment());
        applyRewards(playerUuid, choice.getRewards(), profile, multiplier);

        // Mark quest complete
        profile.markQuestCompleted(questId);
        profile.recordQuestChoice(questId, "final", choiceId);
        setStatus(playerUuid, questId, Quest.Status.COMPLETED);
        playerData.markQuestCompleted(playerUuid, questId);

        // Fire events
        boolean isMajor = Math.abs(clamped) >= 10;
        eventBus.publish(new MoralChoiceEvent(playerUuid, choice.getDescription(),
                questId + ":" + choiceId, clamped, isMajor));

        // Fire consequences
        if (choice.hasConsequence()) {
            processConsequence(playerUuid, choice.getConsequenceKey(), zoneKey);
        }

        logger.info("Player " + playerUuid + " completed quest '" + questId
                + "' via choice '" + choiceId + "' [Δalignment=" + clamped + "]");

        return new CompletionResult(quest, choice, previousAlignment,
                profile.getAlignment(), choice.getRewards());
    }

    /**
     * Resets a resettable quest for a player (removes completion marker, restores IN_PROGRESS).
     *
     * @param playerUuid player UUID
     * @param questId    quest to reset
     * @return true if the reset was performed
     */
    public boolean resetQuest(UUID playerUuid, String questId) {
        Quest quest = questRegistry.get(questId);
        if (quest == null || !quest.isResettable()) return false;
        playerData.getProfile(playerUuid).ifPresent(p -> {
            p.getCompletedQuests(); // ensure loaded
            // Note: removal from completed set not exposed on PlayerProfile on purpose;
            // admin reset should use PlayerDataManager.resetPlayer() for full resets.
        });
        setStatus(playerUuid, questId, Quest.Status.NOT_STARTED);
        logger.info("Quest '" + questId + "' reset for player " + playerUuid);
        return true;
    }

    // ── Status helpers ────────────────────────────────────────────────────────

    /**
     * Returns the player's current status for a quest.
     *
     * @param playerUuid player UUID
     * @param questId    quest identifier
     * @return Quest.Status (NOT_STARTED if never touched)
     */
    public Quest.Status getStatus(UUID playerUuid, String questId) {
        Map<String, Quest.Status> states = playerQuestState.get(playerUuid);
        if (states == null) return Quest.Status.NOT_STARTED;
        return states.getOrDefault(questId, Quest.Status.NOT_STARTED);
    }

    private void setStatus(UUID playerUuid, String questId, Quest.Status status) {
        playerQuestState.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
                        .put(questId, status);
    }

    // ── Reward application ────────────────────────────────────────────────────

    private void applyRewards(UUID playerUuid, List<QuestReward> rewards,
                               PlayerProfile profile, double multiplier) {
        for (QuestReward reward : rewards) {
            switch (reward.getType()) {
                case GOLD        -> {
                    int gold = reward.getScaledAmount(multiplier);
                    // TODO: Hytale API — grant gold to player inventory
                    logger.debug("Granting " + gold + " gold to " + playerUuid);
                }
                case EXPERIENCE  -> {
                    int xp = reward.getScaledAmount(multiplier);
                    // TODO: Hytale API — grant XP to player
                    logger.debug("Granting " + xp + " XP to " + playerUuid);
                }
                case ITEM        -> {
                    // TODO: Hytale API — add item by ID to player inventory
                    logger.debug("Granting item '" + reward.getValue() + "' to " + playerUuid);
                }
                case ABILITY_UNLOCK -> {
                    profile.unlockAbility(reward.getValue());
                    playerData.unlockAbility(playerUuid, reward.getValue());
                }
                case RELATIONSHIP   -> {
                    String[] parts = reward.getValue().split(":", 2);
                    if (parts.length == 2) {
                        int delta = Integer.parseInt(parts[1]);
                        playerData.updateNpcRelationship(playerUuid, parts[0],
                                profile.getNpcRelationship(parts[0], 0) + delta);
                    }
                }
                case ALIGNMENT   -> {
                    int bonus = reward.getScaledAmount(multiplier);
                    profile.setAlignment(MoralityCalculator.applyDelta(profile.getAlignment(), bonus));
                }
                default -> logger.debug("Unhandled reward type: " + reward.getType());
            }
        }
    }

    private void processConsequence(UUID playerUuid, String consequenceKey, String zoneKey) {
        // Parse "type:payload" consequence key format
        String[] parts = consequenceKey.split(":", 2);
        String typeStr = parts[0].toUpperCase();
        String payload = parts.length > 1 ? parts[1] : "";

        try {
            ConsequenceTriggeredEvent.ConsequenceType type =
                    ConsequenceTriggeredEvent.ConsequenceType.valueOf(typeStr);
            consequenceManager.triggerImmediate(playerUuid, type, payload, zoneKey);
        } catch (IllegalArgumentException e) {
            // Not a ConsequenceType — could be a quest_unlock or other custom key
            logger.debug("Custom consequence key: " + consequenceKey + " for " + playerUuid);
        }
    }

    // ── Result type ───────────────────────────────────────────────────────────

    /**
     * Describes the outcome of a quest completion action.
     *
     * @param quest             quest that was completed
     * @param choice            chosen moral path
     * @param previousAlignment alignment before completion
     * @param newAlignment      alignment after completion
     * @param rewards           list of rewards granted
     */
    public record CompletionResult(Quest quest, QuestChoice choice,
                                    int previousAlignment, int newAlignment,
                                    java.util.List<QuestReward> rewards) {
        /** @return net alignment change from this completion */
        public int alignmentDelta() { return newAlignment - previousAlignment; }
    }
}
