package com.hytale.fablescript.commands;

import com.hytale.fablescript.config.ConfigManager;
import com.hytale.fablescript.core.ConsequenceManager;
import com.hytale.fablescript.listeners.PlayerInteractionListener;
import com.hytale.fablescript.storage.DataPersistence;
import com.hytale.fablescript.storage.PlayerDataManager;
import com.hytale.fablescript.storage.PlayerProfile;
import com.hytale.fablescript.utils.FableLogger;
import com.hytale.fablescript.utils.MathUtils;
import com.hytale.fablescript.utils.MoralityCalculator;
import com.hytale.fablescript.world.WorldState;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import org.jetbrains.annotations.Nonnull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handles all admin-facing {@code /fable} commands.
 *
 * <p>Sub-commands:
 * <ul>
 *   <li>{@code /fable reload}                    – hot-reload all config files</li>
 *   <li>{@code /fable set-alignment <uuid> <v>}  – force a player's alignment value</li>
 *   <li>{@code /fable reset-player <uuid>}        – clear all FableScript data for a player</li>
 *   <li>{@code /fable world-state}               – print zone corruption/blessing scores</li>
 *   <li>{@code /fable export <uuid>}             – export player data to JSON</li>
 *   <li>{@code /fable consequence-log}           – list pending delayed consequences</li>
 *   <li>{@code /fable debug}                     – toggle debug logging</li>
 * </ul>
 * </p>
 */
public class AdminCommandExecutor extends AbstractCommand {

    private final ConfigManager configManager;
    private final PlayerDataManager playerData;
    private final PlayerInteractionListener interactionListener;
    private final ConsequenceManager consequenceManager;
    private final WorldState worldState;
    private final DataPersistence persistence;
    private final FableLogger logger;
    private final File dataFolder;

    public AdminCommandExecutor(ConfigManager configManager,
                                 PlayerDataManager playerData,
                                 PlayerInteractionListener interactionListener,
                                 ConsequenceManager consequenceManager,
                                 WorldState worldState,
                                 DataPersistence persistence,
                                 File dataFolder,
                                 FableLogger logger) {
        super("fable", "FableScript admin commands.");
        this.configManager        = configManager;
        this.playerData           = playerData;
        this.interactionListener  = interactionListener;
        this.consequenceManager   = consequenceManager;
        this.worldState           = worldState;
        this.persistence          = persistence;
        this.dataFolder           = dataFolder;
        this.logger               = logger;
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        String[] args = resolveArgs(context);
        if (args.length == 0) {
            showHelp(context);
            return CompletableFuture.completedFuture(null);
        }

        switch (args[0].toLowerCase()) {
            case "reload"           -> handleReload(context);
            case "set-alignment"    -> handleSetAlignment(context, args);
            case "reset-player"     -> handleResetPlayer(context, args);
            case "world-state"      -> handleWorldState(context);
            case "export"           -> handleExport(context, args);
            case "consequence-log"  -> handleConsequenceLog(context, args);
            case "debug"            -> handleDebugToggle(context);
            default                 -> {
                context.sendMessage(Message.raw("§cUnknown sub-command. Use /fable for help."));
                showHelp(context);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    private void handleReload(CommandContext context) {
        configManager.reload();
        context.sendMessage(Message.raw("§a[FableScript] Configuration reloaded."));
        logger.info("Config reloaded by admin.");
    }

    private void handleSetAlignment(CommandContext context, String[] args) {
        if (args.length < 3) {
            context.sendMessage(Message.raw("§cUsage: /fable set-alignment <uuid> <value>"));
            return;
        }
        try {
            UUID target = UUID.fromString(args[1]);
            int value = MoralityCalculator.clamp(Integer.parseInt(args[2]),
                    MoralityCalculator.MIN_ALIGNMENT, MoralityCalculator.MAX_ALIGNMENT);
            playerData.updateAlignment(target, value);
            context.sendMessage(Message.raw("§a[FableScript] Alignment for " + target
                    + " set to " + value + "."));
            logger.info("Admin set alignment for " + target + " to " + value);
        } catch (IllegalArgumentException e) {
            context.sendMessage(Message.raw("§cInvalid UUID or alignment value."));
        }
    }

    private void handleResetPlayer(CommandContext context, String[] args) {
        if (args.length < 2) {
            context.sendMessage(Message.raw("§cUsage: /fable reset-player <uuid>"));
            return;
        }
        try {
            UUID target = UUID.fromString(args[1]);
            playerData.resetPlayer(target, "unknown");
            consequenceManager.clearPending(target);
            context.sendMessage(Message.raw("§a[FableScript] Player " + target + " has been reset."));
        } catch (IllegalArgumentException e) {
            context.sendMessage(Message.raw("§cInvalid UUID."));
        }
    }

    private void handleWorldState(CommandContext context) {
        context.sendMessage(Message.raw("§6§l[FableScript] World Zone Scores"));
        worldState.getAllZoneScores().forEach((zone, score) -> {
            String color = score > 20 ? "§a" : score < -20 ? "§c" : "§7";
            context.sendMessage(Message.raw("§f" + zone + " §8— " + color + score));
        });
        context.sendMessage(Message.raw("§7Corrupted zones: §c"
                + worldState.getActiveCorruptionZones().size()
                + "  §7Blessed: §a" + worldState.getActiveBlessingZones().size()));
    }

    private void handleExport(CommandContext context, String[] args) {
        if (args.length < 2) {
            context.sendMessage(Message.raw("§cUsage: /fable export <uuid>"));
            return;
        }
        try {
            UUID target = UUID.fromString(args[1]);
            File exportDir = new File(dataFolder, "exports");

            // Try online profile first, fall back to database
            Optional<PlayerProfile> profile = playerData.getProfile(target);
            if (profile.isPresent()) {
                doExport(context, profile.get(), exportDir);
            } else {
                context.sendMessage(Message.raw("§7Loading offline profile..."));
                playerData.loadOfflineProfile(target).thenAccept(opt -> {
                    opt.ifPresentOrElse(
                            p -> doExport(context, p, exportDir),
                            () -> context.sendMessage(Message.raw("§cPlayer not found in database."))
                    );
                });
            }
        } catch (IllegalArgumentException e) {
            context.sendMessage(Message.raw("§cInvalid UUID."));
        }
    }

    private void doExport(CommandContext context, PlayerProfile profile, File exportDir) {
        try {
            Path out = persistence.exportToJson(profile, exportDir);
            context.sendMessage(Message.raw("§a[FableScript] Exported to: §f" + out.getFileName()));
        } catch (Exception e) {
            context.sendMessage(Message.raw("§cExport failed: " + e.getMessage()));
            logger.warn("Export failed", e);
        }
    }

    private void handleConsequenceLog(CommandContext context, String[] args) {
        if (args.length < 2) {
            context.sendMessage(Message.raw("§cUsage: /fable consequence-log <uuid>"));
            return;
        }
        try {
            UUID target = UUID.fromString(args[1]);
            var pending = consequenceManager.getPending(target);
            context.sendMessage(Message.raw("§6§l[FableScript] Pending Consequences ("
                    + pending.size() + ")"));
            pending.forEach(p -> context.sendMessage(Message.raw(
                    "§f" + p.type() + " §8— §7zone=" + p.zoneKey()
                    + " fires in " + MathUtils.formatDuration(
                            Math.max(0, p.triggerEpochSecond()
                                     - System.currentTimeMillis() / 1000L))
            )));
        } catch (IllegalArgumentException e) {
            context.sendMessage(Message.raw("§cInvalid UUID."));
        }
    }

    private void handleDebugToggle(CommandContext context) {
        boolean current = logger.isDebugEnabled();
        logger.setDebugEnabled(!current);
        context.sendMessage(Message.raw("§a[FableScript] Debug logging: "
                + (!current ? "§aENABLED" : "§cDISABLED")));
    }

    private void showHelp(CommandContext context) {
        context.sendMessage(Message.raw("§6§l[FableScript] Admin Commands"));
        context.sendMessage(Message.raw("§f/fable reload §8— §7Reload config files"));
        context.sendMessage(Message.raw("§f/fable set-alignment <uuid> <v> §8— §7Force alignment"));
        context.sendMessage(Message.raw("§f/fable reset-player <uuid> §8— §7Clear player data"));
        context.sendMessage(Message.raw("§f/fable world-state §8— §7Show zone scores"));
        context.sendMessage(Message.raw("§f/fable export <uuid> §8— §7Export player data to JSON"));
        context.sendMessage(Message.raw("§f/fable consequence-log <uuid> §8— §7Pending consequences"));
        context.sendMessage(Message.raw("§f/fable debug §8— §7Toggle debug logging"));
    }

    private String[] resolveArgs(CommandContext context) {
        // TODO: Hytale API — context.getArgs()
        return new String[0];
    }
}
