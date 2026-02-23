package com.hytale.fablescript.commands;

import com.hytale.fablescript.quests.Quest;
import com.hytale.fablescript.quests.QuestChoice;
import com.hytale.fablescript.quests.QuestManager;
import com.hytale.fablescript.storage.PlayerDataManager;
import com.hytale.fablescript.utils.FableLogger;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import org.jetbrains.annotations.Nonnull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handles the {@code /quest} command for players.
 *
 * <p>Sub-commands:
 * <ul>
 *   <li>{@code /quest status}             – list in-progress and available quests</li>
 *   <li>{@code /quest info <questId>}     – show choices and description for a quest</li>
 *   <li>{@code /quest choose <questId> <choiceId>} – make a quest choice</li>
 * </ul>
 * </p>
 */
public class QuestCommandExecutor extends AbstractCommand {

    private final QuestManager questManager;
    private final PlayerDataManager playerData;
    private final boolean alignmentGatingEnabled;
    private final FableLogger logger;

    public QuestCommandExecutor(QuestManager questManager, PlayerDataManager playerData,
                                 boolean alignmentGatingEnabled, FableLogger logger) {
        super("quest", "View and interact with Fable quests.");
        this.questManager           = questManager;
        this.playerData             = playerData;
        this.alignmentGatingEnabled = alignmentGatingEnabled;
        this.logger                 = logger;
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        UUID playerUuid = resolveSenderUuid(context);
        if (playerUuid == null) {
            context.sendMessage(Message.raw("§cThis command must be run by a player."));
            return CompletableFuture.completedFuture(null);
        }

        String[] args = resolveArgs(context);
        if (args.length == 0) {
            showQuestList(context, playerUuid);
            return CompletableFuture.completedFuture(null);
        }

        switch (args[0].toLowerCase()) {
            case "status" -> showQuestList(context, playerUuid);
            case "info"   -> {
                if (args.length < 2) {
                    context.sendMessage(Message.raw("§cUsage: /quest info <questId>"));
                } else {
                    showQuestInfo(context, playerUuid, args[1]);
                }
            }
            case "choose" -> {
                if (args.length < 3) {
                    context.sendMessage(Message.raw("§cUsage: /quest choose <questId> <choiceId>"));
                } else {
                    makeQuestChoice(context, playerUuid, args[1], args[2]);
                }
            }
            default -> context.sendMessage(Message.raw(
                    "§cUnknown sub-command. Try: status, info <id>, choose <id> <choiceId>"));
        }

        return CompletableFuture.completedFuture(null);
    }

    private void showQuestList(CommandContext context, UUID playerUuid) {
        List<Quest> available = questManager.getAvailableQuests(playerUuid, alignmentGatingEnabled);
        context.sendMessage(Message.raw("§6§l[FableScript] Quests (" + available.size() + " available)"));
        if (available.isEmpty()) {
            context.sendMessage(Message.raw("§7No quests available. Your alignment may restrict options."));
            return;
        }
        available.forEach(q -> {
            Quest.Status status = questManager.getStatus(playerUuid, q.getId());
            String statusTag = status == Quest.Status.IN_PROGRESS ? " §e[In Progress]" : "";
            context.sendMessage(Message.raw("§e" + q.getId() + statusTag + " §8— §f" + q.getTitle()));
        });
        context.sendMessage(Message.raw("§7Use §f/quest info <id> §7for details."));
    }

    private void showQuestInfo(CommandContext context, UUID playerUuid, String questId) {
        Optional<Quest> questOpt = questManager.getQuest(questId);
        if (questOpt.isEmpty()) {
            context.sendMessage(Message.raw("§cUnknown quest: " + questId));
            return;
        }
        Quest quest = questOpt.get();
        context.sendMessage(Message.raw("§6§l" + quest.getTitle()));
        context.sendMessage(Message.raw("§7" + quest.getDescription()));
        context.sendMessage(Message.raw("§8───────────────────────────"));
        context.sendMessage(Message.raw("§7Available paths:"));

        int playerAlignment = playerData.getProfile(playerUuid)
                .map(p -> p.getAlignment()).orElse(0);
        List<QuestChoice> choices = quest.getAvailableChoices(playerAlignment);

        if (choices.isEmpty()) {
            context.sendMessage(Message.raw("§cNo choices available at your current alignment."));
        } else {
            choices.forEach(c -> {
                String moralTag = switch (c.getMoralPath()) {
                    case GOOD    -> "§a[Good]";
                    case EVIL    -> "§c[Evil]";
                    case NEUTRAL -> "§7[Neutral]";
                };
                String alignSign = c.getAlignmentDelta() >= 0 ? "§a+" : "§c";
                context.sendMessage(Message.raw(
                        moralTag + " §f" + c.getId() + " §8— §7" + c.getDescription()
                        + " §8(" + alignSign + c.getAlignmentDelta() + " alignment§8)"));
            });
            context.sendMessage(Message.raw("§7Use §f/quest choose " + questId
                    + " <choiceId> §7to proceed."));
        }
    }

    private void makeQuestChoice(CommandContext context, UUID playerUuid,
                                  String questId, String choiceId) {
        QuestManager.CompletionResult result =
                questManager.completeQuestWithChoice(playerUuid, questId, choiceId, "default_zone");
        if (result == null) {
            context.sendMessage(Message.raw(
                    "§cCould not complete quest. Check your alignment or try again shortly."));
            return;
        }
        String alignSign = result.alignmentDelta() >= 0 ? "§a+" : "§c";
        context.sendMessage(Message.raw("§6✦ Quest complete: §f" + result.quest().getTitle()));
        context.sendMessage(Message.raw("§7Path: §f" + result.choice().getMoralPath().name()
                + "  " + alignSign + result.alignmentDelta() + " §7alignment"));
        result.rewards().forEach(r ->
                context.sendMessage(Message.raw("§7Reward: §f" + r.getType().name()
                        + " — " + r.getValue())));
    }

    private UUID resolveSenderUuid(CommandContext context) {
        // TODO: Hytale API — context.getPlayer().getUniqueId()
        return null;
    }

    private String[] resolveArgs(CommandContext context) {
        // TODO: Hytale API — context.getArgs()
        return new String[0];
    }
}
