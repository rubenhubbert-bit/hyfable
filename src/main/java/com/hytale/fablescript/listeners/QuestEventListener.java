package com.hytale.fablescript.listeners;

import com.hytale.fablescript.events.ConsequenceTriggeredEvent;
import com.hytale.fablescript.events.FableEventBus;
import com.hytale.fablescript.events.MoralChoiceEvent;
import com.hytale.fablescript.quests.QuestManager;
import com.hytale.fablescript.storage.PlayerDataManager;
import com.hytale.fablescript.utils.FableLogger;

import java.util.UUID;

/**
 * Handles downstream effects of quest completion and moral choice events.
 *
 * <p>Subscribes to {@link MoralChoiceEvent} and {@link ConsequenceTriggeredEvent}
 * to apply quest-chain progressions, unlock follow-up quests, and notify
 * players of pending consequences.</p>
 */
public class QuestEventListener {

    private final QuestManager questManager;
    private final PlayerDataManager playerData;
    private final FableLogger logger;

    /**
     * Constructs the listener and subscribes to the event bus.
     *
     * @param eventBus     FableScript event bus
     * @param questManager quest state coordinator
     * @param playerData   player data manager
     * @param logger       plugin logger
     */
    public QuestEventListener(FableEventBus eventBus, QuestManager questManager,
                               PlayerDataManager playerData, FableLogger logger) {
        this.questManager = questManager;
        this.playerData   = playerData;
        this.logger       = logger;

        eventBus.subscribe(MoralChoiceEvent.class, this::onMoralChoice);
        eventBus.subscribe(ConsequenceTriggeredEvent.class, this::onConsequenceTriggered);
    }

    /**
     * Handles a {@link MoralChoiceEvent}:
     * checks if the choice unlocks a quest chain follow-up and notifies the player.
     *
     * @param event the moral choice event
     */
    public void onMoralChoice(MoralChoiceEvent event) {
        UUID playerUuid = event.getPlayerUuid();
        logger.debug("MoralChoiceEvent: " + event);

        // Check if the context key references a quest chain (format "questId:choiceId")
        String[] parts = event.getContextKey().split(":", 2);
        if (parts.length == 2) {
            String questId  = parts[0];
            String choiceId = parts[1];
            // Notify player about their alignment change
            String direction = event.getAlignmentDelta() > 0 ? "positive" : "negative";
            String msg = "§7[Fable] §fYour choice carries " + direction + " moral weight.";
            // TODO: Hytale API — send message to player with UUID playerUuid
            logger.debug("Quest choice noted: quest=" + questId + " choice=" + choiceId);
        }

        // Major choices trigger a special announcement
        if (event.isMajorChoice()) {
            String symbol = event.getAlignmentDelta() > 0 ? "§6✦" : "§4☠";
            String announcement = symbol + " §7A significant moral choice has been made...";
            // TODO: Hytale API — broadcast or send to nearby players
            logger.info("Major moral choice by " + playerUuid + ": "
                    + event.getChoiceDescription());
        }
    }

    /**
     * Handles a {@link ConsequenceTriggeredEvent}:
     * resolves quest-related consequences (e.g., quest_unlock, quest_fail).
     *
     * @param event the consequence event
     */
    public void onConsequenceTriggered(ConsequenceTriggeredEvent event) {
        UUID playerUuid = event.getPlayerUuid();
        String type = event.getType().name();

        if (type.equals("QUEST_STATE_CHANGE")) {
            String questId = event.getPayloadValue("quest_id");
            String state   = event.getPayloadValue("state");
            if (questId == null || state == null) return;

            switch (state.toUpperCase()) {
                case "UNLOCK" -> {
                    questManager.startQuest(playerUuid, questId);
                    String msg = "§6[Fable] §fA new quest is available: §e" + questId;
                    // TODO: Hytale API — send message to playerUuid
                    logger.info("Quest unlocked for " + playerUuid + ": " + questId);
                }
                case "FAIL" -> {
                    String msg = "§c[Fable] §fQuest failed: §e" + questId;
                    // TODO: Hytale API — send message to playerUuid
                    logger.info("Quest failed for " + playerUuid + ": " + questId);
                }
                default -> logger.warn("Unknown quest state change: " + state);
            }
        }

        if (type.equals("BOUNTY_PLACED")) {
            String reason = event.getPayloadValue("reason");
            String msg = "§c[Fable] ☠ A bounty has been placed on you! Reason: " + reason;
            // TODO: Hytale API — send message to playerUuid
            logger.info("Bounty placed on " + playerUuid + " for: " + reason);
        }

        if (type.equals("BOUNTY_REMOVED")) {
            String msg = "§a[Fable] ✔ Your bounty has been cleared.";
            // TODO: Hytale API — send message to playerUuid
        }
    }
}
