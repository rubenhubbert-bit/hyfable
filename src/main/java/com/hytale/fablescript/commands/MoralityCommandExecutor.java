package com.hytale.fablescript.commands;

import com.hytale.fablescript.abilities.AbilityManager;
import com.hytale.fablescript.core.PlayerMorality;
import com.hytale.fablescript.listeners.PlayerInteractionListener;
import com.hytale.fablescript.npc.NPCManager;
import com.hytale.fablescript.quests.Quest;
import com.hytale.fablescript.quests.QuestManager;
import com.hytale.fablescript.storage.PlayerDataManager;
import com.hytale.fablescript.utils.FableLogger;
import com.hytale.fablescript.utils.MoralityCalculator;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import org.jetbrains.annotations.Nonnull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handles all player-facing {@code /morality} and related commands.
 *
 * <p>Available sub-commands:
 * <ul>
 *   <li>{@code /morality}           – show current alignment progress bar</li>
 *   <li>{@code /morality history}   – show last 10 alignment changes</li>
 *   <li>{@code /alignment effects}  – show current ability/NPC info by tier</li>
 *   <li>{@code /relationships}      – show all NPC opinion values</li>
 *   <li>{@code /abilities}          – list unlocked/locked abilities</li>
 *   <li>{@code /quest status}       – list available quests</li>
 * </ul>
 * </p>
 */
public class MoralityCommandExecutor extends AbstractCommand {

    private final PlayerInteractionListener interactionListener;
    private final PlayerDataManager playerData;
    private final AbilityManager abilityManager;
    private final NPCManager npcManager;
    private final QuestManager questManager;
    private final boolean alignmentGatingEnabled;
    private final FableLogger logger;

    public MoralityCommandExecutor(PlayerInteractionListener interactionListener,
                                    PlayerDataManager playerData,
                                    AbilityManager abilityManager,
                                    NPCManager npcManager,
                                    QuestManager questManager,
                                    boolean alignmentGatingEnabled,
                                    FableLogger logger) {
        super("morality", "View and manage your Fable alignment.");
        this.interactionListener    = interactionListener;
        this.playerData             = playerData;
        this.abilityManager         = abilityManager;
        this.npcManager             = npcManager;
        this.questManager           = questManager;
        this.alignmentGatingEnabled = alignmentGatingEnabled;
        this.logger                 = logger;
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        // TODO: Hytale API — resolve sender UUID from context
        UUID playerUuid = resolveSenderUuid(context);
        if (playerUuid == null) {
            context.sendMessage(Message.raw("§cThis command must be run by a player."));
            return CompletableFuture.completedFuture(null);
        }

        String[] args = resolveArgs(context);
        String sub = args.length > 0 ? args[0].toLowerCase() : "";

        switch (sub) {
            case "history" -> showHistory(context, playerUuid);
            case "effects" -> showEffects(context, playerUuid);
            default        -> showAlignment(context, playerUuid);
        }
        return CompletableFuture.completedFuture(null);
    }

    private void showAlignment(CommandContext context, UUID playerUuid) {
        Optional<PlayerMorality> moralityOpt = interactionListener.getMorality(playerUuid);
        if (moralityOpt.isEmpty()) {
            context.sendMessage(Message.raw("§cYour profile is still loading. Try again in a moment."));
            return;
        }
        PlayerMorality m = moralityOpt.get();
        String bar = MoralityCalculator.buildProgressBar(m.getAlignmentValue(), 20);
        context.sendMessage(Message.raw("§6§l[FableScript] Alignment"));
        context.sendMessage(Message.raw("§f" + bar));
        context.sendMessage(Message.raw("§7Tier: §f" + m.getCurrentTier().getDisplayName()
                + "  §7Use §f/morality history §7for change log."));
    }

    private void showHistory(CommandContext context, UUID playerUuid) {
        Optional<PlayerMorality> moralityOpt = interactionListener.getMorality(playerUuid);
        if (moralityOpt.isEmpty()) {
            context.sendMessage(Message.raw("§cProfile not loaded."));
            return;
        }
        context.sendMessage(Message.raw("§6§l[FableScript] Alignment History (last 10)"));
        var history = moralityOpt.get().getHistory();
        int start = Math.max(0, history.size() - 10);
        for (int i = start; i < history.size(); i++) {
            var entry = history.get(i);
            String sign = entry.delta() >= 0 ? "§a+" : "§c";
            context.sendMessage(Message.raw(
                    "§7• " + sign + entry.delta()
                    + " §f(" + entry.from() + " → " + entry.to() + ")"
                    + " §8— §7" + entry.cause()
            ));
        }
    }

    private void showEffects(CommandContext context, UUID playerUuid) {
        playerData.getProfile(playerUuid).ifPresentOrElse(profile -> {
            var tier = profile.getAlignmentTier();
            context.sendMessage(Message.raw("§6§l[FableScript] " + tier.getDisplayName() + " Effects"));
            context.sendMessage(Message.raw("§7NPC Reaction: §f" + npcReactionDesc(tier)));
            context.sendMessage(Message.raw("§7Environment:  §f" + envDesc(tier)));
            context.sendMessage(Message.raw("§7Use §f/abilities §7to see unlocked spells."));
        }, () -> context.sendMessage(Message.raw("§cProfile not loaded.")));
    }

    /** Shows the /relationships sub-command (registered as a separate command). */
    public void showRelationships(CommandContext context, UUID playerUuid) {
        Map<String, String> summary = npcManager.getRelationshipSummary(playerUuid);
        context.sendMessage(Message.raw("§6§l[FableScript] NPC Relationships"));
        if (summary.isEmpty()) {
            context.sendMessage(Message.raw("§7You have not interacted with any NPCs yet."));
            return;
        }
        summary.forEach((npcName, rel) ->
                context.sendMessage(Message.raw("§f" + npcName + " §8— §7" + rel)));
    }

    /** Shows the /abilities sub-command (registered as a separate command). */
    public void showAbilities(CommandContext context, UUID playerUuid) {
        playerData.getProfile(playerUuid).ifPresent(profile -> {
            List<AbilityManager.AbilityInfo> abilities =
                    abilityManager.listAbilities(playerUuid, profile.getAlignment());
            context.sendMessage(Message.raw("§6§l[FableScript] Abilities"));
            for (AbilityManager.AbilityInfo info : abilities) {
                String status = info.unlocked()
                        ? (info.cooldownRemaining() > 0 ? "§e(CD: " + info.cooldownRemaining() + "s)" : "§aReady")
                        : "§cLocked";
                context.sendMessage(Message.raw(
                        "§f" + info.ability().getDisplayName() + " §8— " + status
                        + " §8| §7" + info.ability().getDescription()
                ));
            }
        });
    }

    /** Shows the /quest status sub-command. */
    public void showQuestStatus(CommandContext context, UUID playerUuid) {
        List<Quest> available = questManager.getAvailableQuests(playerUuid, alignmentGatingEnabled);
        context.sendMessage(Message.raw("§6§l[FableScript] Available Quests (" + available.size() + ")"));
        if (available.isEmpty()) {
            context.sendMessage(Message.raw("§7No quests available for your current alignment."));
            return;
        }
        available.forEach(q -> context.sendMessage(Message.raw(
                "§e" + q.getTitle() + " §8— §7" + q.getDescription().substring(
                        0, Math.min(60, q.getDescription().length())) + "..."
        )));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String npcReactionDesc(com.hytale.fablescript.core.MoralityAlignment tier) {
        return switch (tier) {
            case PURE_GOOD -> "Revered as a savior. Discounts and special quests.";
            case GOOD      -> "Friendly. Normal pricing, occasional gifts.";
            case NEUTRAL   -> "Professional. Standard pricing and all quests.";
            case EVIL      -> "Fearful. Inflated prices, restricted quests.";
            case PURE_EVIL -> "Hostile. Some attack on sight. Bounty active.";
        };
    }

    private String envDesc(com.hytale.fablescript.core.MoralityAlignment tier) {
        return switch (tier) {
            case PURE_GOOD -> "Flowers bloom at your feet. Skies clear.";
            case GOOD      -> "Subtle positive aura. Crops grow faster.";
            case NEUTRAL   -> "No environmental effect.";
            case EVIL      -> "Dark aura. Crops wither. Wildlife flees.";
            case PURE_EVIL -> "Corruption spreads around you. Darkness falls.";
        };
    }

    /** Resolves the UUID of the command sender. Stub until Hytale API is confirmed. */
    private UUID resolveSenderUuid(CommandContext context) {
        // TODO: Hytale API — context.getPlayer().getUniqueId()
        return null; // placeholder — replace with actual API call
    }

    /** Parses command arguments from context. Stub until Hytale API is confirmed. */
    private String[] resolveArgs(CommandContext context) {
        // TODO: Hytale API — context.getArgs() or similar
        return new String[0];
    }
}
