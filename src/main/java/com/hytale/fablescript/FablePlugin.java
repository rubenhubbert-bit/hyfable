package com.hytale.fablescript;

import com.hytale.fablescript.abilities.AbilityManager;
import com.hytale.fablescript.commands.AdminCommandExecutor;
import com.hytale.fablescript.commands.MoralityCommandExecutor;
import com.hytale.fablescript.commands.QuestCommandExecutor;
import com.hytale.fablescript.config.ConfigManager;
import com.hytale.fablescript.config.FableConfig;
import com.hytale.fablescript.core.ChoiceTracker;
import com.hytale.fablescript.core.ConsequenceManager;
import com.hytale.fablescript.events.FableEventBus;
import com.hytale.fablescript.listeners.CombatListener;
import com.hytale.fablescript.listeners.MoralityEventListener;
import com.hytale.fablescript.listeners.PlayerInteractionListener;
import com.hytale.fablescript.listeners.QuestEventListener;
import com.hytale.fablescript.npc.NPCManager;
import com.hytale.fablescript.quests.*;
import com.hytale.fablescript.storage.DataPersistence;
import com.hytale.fablescript.storage.PlayerDataManager;
import com.hytale.fablescript.utils.DialogueParser;
import com.hytale.fablescript.utils.FableLogger;
import com.hytale.fablescript.world.DynamicWeather;
import com.hytale.fablescript.world.EnvironmentalConsequence;
import com.hytale.fablescript.world.WorldState;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;

/**
 * FableScript — Hytale Fable Morality and Consequence Plugin.
 *
 * <p>This is the main entry point of the plugin. It initialises all subsystems
 * in dependency order and registers Hytale event listeners and commands.</p>
 *
 * <p>Startup order:
 * <ol>
 *   <li>Logger (depends only on data folder)</li>
 *   <li>ConfigManager → FableConfig (depends on logger)</li>
 *   <li>FableEventBus (no deps)</li>
 *   <li>DataPersistence → PlayerDataManager (depends on config)</li>
 *   <li>ChoiceTracker (no deps)</li>
 *   <li>ConsequenceManager (depends on event bus)</li>
 *   <li>WorldState, EnvironmentalConsequence, DynamicWeather (world layer)</li>
 *   <li>NPCManager (depends on config, player data, dialogue parser)</li>
 *   <li>AbilityManager (depends on player data, config)</li>
 *   <li>QuestManager (depends on player data, choice tracker, consequence manager)</li>
 *   <li>Event Listeners (wire everything together, subscribe to event bus)</li>
 *   <li>Commands (register with Hytale command registry)</li>
 *   <li>Default quest registration</li>
 * </ol>
 * </p>
 */
public class FablePlugin extends JavaPlugin {

    // ── Subsystem references ──────────────────────────────────────────────────

    private FableLogger fableLogger;
    private ConfigManager configManager;
    private FableConfig config;
    private FableEventBus eventBus;
    private DataPersistence persistence;
    private PlayerDataManager playerData;
    private ChoiceTracker choiceTracker;
    private ConsequenceManager consequenceManager;
    private WorldState worldState;
    private EnvironmentalConsequence environmentalConsequence;
    private DynamicWeather dynamicWeather;
    private NPCManager npcManager;
    private AbilityManager abilityManager;
    private QuestManager questManager;
    private PlayerInteractionListener interactionListener;
    private CombatListener combatListener;

    /**
     * Required Hytale plugin constructor.
     *
     * @param init plugin initialisation context provided by the Hytale server
     */
    public FablePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    /**
     * Called by the Hytale server once the plugin is ready to initialise.
     * All subsystems are constructed and wired here.
     */
    @Override
    protected void setup() {
        File dataFolder = getDataFolder();

        // ── 1. Logger ─────────────────────────────────────────────────────────
        fableLogger = new FableLogger(dataFolder, false);
        fableLogger.info("FableScript starting up...");

        // ── 2. Config ─────────────────────────────────────────────────────────
        configManager = new ConfigManager(dataFolder, fableLogger);
        configManager.initialise();
        config = configManager.getFableConfig();
        fableLogger.setDebugEnabled(config.isDebugEnabled());

        // ── 3. Event Bus ──────────────────────────────────────────────────────
        eventBus = new FableEventBus();

        // ── 4. Storage ────────────────────────────────────────────────────────
        persistence = new DataPersistence(dataFolder, config.getStorageType(), fableLogger);
        persistence.initialise();
        playerData = new PlayerDataManager(persistence,
                config.getBackupIntervalMinutes(), fableLogger);

        // ── 5. Core systems ───────────────────────────────────────────────────
        choiceTracker = new ChoiceTracker();
        consequenceManager = new ConsequenceManager(fableLogger, eventBus);

        // ── 6. World layer ────────────────────────────────────────────────────
        worldState = new WorldState();
        environmentalConsequence = new EnvironmentalConsequence(
                worldState, eventBus,
                config.getCorruptionSpreadRadius(),
                config.getCorruptionSpreadChance(),
                fableLogger);
        dynamicWeather = new DynamicWeather(fableLogger);

        // ── 7. NPC system ─────────────────────────────────────────────────────
        DialogueParser dialogueParser = new DialogueParser(fableLogger);
        npcManager = new NPCManager(configManager.getNpcDefinitions(),
                playerData, dialogueParser, dataFolder, fableLogger);
        npcManager.initialise();

        // ── 8. Abilities ──────────────────────────────────────────────────────
        abilityManager = new AbilityManager(playerData,
                config.getGoodAbilityThreshold(),
                config.getEvilAbilityThreshold(),
                fableLogger);

        // ── 9. Quests ─────────────────────────────────────────────────────────
        questManager = new QuestManager(playerData, choiceTracker, consequenceManager,
                eventBus, config.getMaxChangePerAction(), fableLogger);
        registerDefaultQuests();

        // ── 10. Listeners ─────────────────────────────────────────────────────
        interactionListener = new PlayerInteractionListener(
                playerData, npcManager, environmentalConsequence, choiceTracker,
                eventBus, config.getMaxChangePerAction(),
                (int) (config.getDecayPerDay() * 24), fableLogger);

        combatListener = new CombatListener(interactionListener, playerData, eventBus, fableLogger);

        // MoralityEventListener self-registers with the event bus
        new MoralityEventListener(eventBus, playerData, abilityManager, npcManager,
                environmentalConsequence, dynamicWeather, fableLogger);

        // QuestEventListener self-registers with the event bus
        new QuestEventListener(eventBus, questManager, playerData, fableLogger);

        // ── 11. Hytale event registrations ────────────────────────────────────
        getEventRegistry().registerGlobal(PlayerReadyEvent.class,
                interactionListener::onPlayerReady);
        // TODO: register additional Hytale events as the API matures:
        //   getEventRegistry().registerGlobal(PlayerLeaveEvent.class, ...)
        //   getEventRegistry().registerGlobal(PlayerKillEvent.class, ...)
        //   getEventRegistry().registerGlobal(PlayerInteractNpcEvent.class, ...)

        // ── 12. Commands ──────────────────────────────────────────────────────
        registerCommands();

        fableLogger.info("FableScript initialised successfully. "
                + questManager.getAvailableQuests(null, false).size()
                + " quests registered, "
                + configManager.getNpcDefinitions().count() + " NPCs loaded.");
    }

    /** Retrieves the plugin's data folder from Hytale's plugin system. */
    private File getDataFolder() {
        // TODO: Hytale API — confirm the correct way to get the data folder path
        // This is standard for most server plugin frameworks.
        return new File("plugins/FableScript");
    }

    // ── Command registration ──────────────────────────────────────────────────

    private void registerCommands() {
        MoralityCommandExecutor moralityCmd = new MoralityCommandExecutor(
                interactionListener, playerData, abilityManager, npcManager, questManager,
                config.isAlignmentBasedAvailability(), fableLogger);
        getCommandRegistry().registerCommand(moralityCmd);

        QuestCommandExecutor questCmd = new QuestCommandExecutor(
                questManager, playerData, config.isAlignmentBasedAvailability(), fableLogger);
        getCommandRegistry().registerCommand(questCmd);

        AdminCommandExecutor adminCmd = new AdminCommandExecutor(
                configManager, playerData, interactionListener, consequenceManager,
                worldState, persistence, getDataFolder(), fableLogger);
        getCommandRegistry().registerCommand(adminCmd);

        fableLogger.info("Registered 3 commands: /morality, /quest, /fable");
    }

    // ── Default quest registration ────────────────────────────────────────────

    /**
     * Registers all built-in quests with the quest manager.
     * These represent the five core Fable questlines described in the specification.
     */
    private void registerDefaultQuests() {
        // ── Quest 1: The Starving Family ──────────────────────────────────────
        questManager.registerQuest(Quest.builder("quest_starving_family")
                .title("The Starving Family")
                .description("A desperate family begs for food and coin. "
                        + "How you help them will define your character.")
                .giverNpc("merchant_thomas")
                .resettable(true)
                .choices(List.of(
                        QuestChoice.builder("choice_steal")
                                .description("Steal from the merchant and give the family the goods.")
                                .moralPath(QuestChoice.MoralPath.EVIL)
                                .alignmentDelta(-5)
                                .rewards(List.of(QuestReward.gold(50)))
                                .consequenceKey("BOUNTY_PLACED:reason=theft")
                                .build(),
                        QuestChoice.builder("choice_work")
                                .description("Complete an honest day's work to earn coin for the family.")
                                .moralPath(QuestChoice.MoralPath.NEUTRAL)
                                .alignmentDelta(2)
                                .rewards(List.of(QuestReward.scaledGold(40),
                                                 QuestReward.experience(50)))
                                .build(),
                        QuestChoice.builder("choice_crime_lord")
                                .description("Convince the family to work for the local crime lord.")
                                .moralPath(QuestChoice.MoralPath.EVIL)
                                .alignmentDelta(-15)
                                .rewards(List.of(QuestReward.gold(150)))
                                .consequenceKey("NPC_HOSTILITY_INCREASE:zone=marketplace")
                                .build()
                ))
                .build());

        // ── Quest 2: The Bandit Hunt ──────────────────────────────────────────
        questManager.registerQuest(Quest.builder("quest_bandit_hunt")
                .title("Bandits on the Road")
                .description("Bandits have been terrorising traders. "
                        + "Captain Elena needs them stopped, but how is up to you.")
                .giverNpc("guard_captain")
                .minAlignment(-30) // available to most alignments
                .choices(List.of(
                        QuestChoice.builder("choice_apprehend")
                                .description("Apprehend the bandits and bring them to justice.")
                                .moralPath(QuestChoice.MoralPath.GOOD)
                                .alignmentDelta(10)
                                .rewards(List.of(QuestReward.scaledGold(80),
                                                 QuestReward.relationship("guard_captain", 15)))
                                .build(),
                        QuestChoice.builder("choice_kill")
                                .description("Kill the bandits outright. Faster but brutal.")
                                .moralPath(QuestChoice.MoralPath.NEUTRAL)
                                .alignmentDelta(-2)
                                .rewards(List.of(QuestReward.gold(60)))
                                .build(),
                        QuestChoice.builder("choice_recruit")
                                .description("Recruit the bandits to your cause instead.")
                                .moralPath(QuestChoice.MoralPath.EVIL)
                                .alignmentDelta(-8)
                                .maxAlignment(-1)
                                .rewards(List.of(QuestReward.gold(30),
                                                 QuestReward.abilityUnlock("life_drain")))
                                .build()
                ))
                .build());

        // ── Quest 3: The Plague Village ───────────────────────────────────────
        questManager.registerQuest(Quest.builder("quest_plague_village")
                .title("The Plague Village")
                .description("A remote village suffers a mysterious illness. "
                        + "Supplies are scarce. What will you sacrifice?")
                .minAlignment(0) // neutral+ only
                .choices(List.of(
                        QuestChoice.builder("choice_cure")
                                .description("Share your own supplies and help find a cure.")
                                .moralPath(QuestChoice.MoralPath.GOOD)
                                .alignmentDelta(20)
                                .rewards(List.of(QuestReward.experience(200),
                                                 QuestReward.abilityUnlock("heal"),
                                                 QuestReward.relationship("guard_captain", 10)))
                                .nextQuestId("quest_ancient_remedy")
                                .build(),
                        QuestChoice.builder("choice_quarantine")
                                .description("Seal the village to prevent spread. Pragmatic but cold.")
                                .moralPath(QuestChoice.MoralPath.NEUTRAL)
                                .alignmentDelta(0)
                                .rewards(List.of(QuestReward.gold(100)))
                                .build()
                ))
                .build());

        // ── Quest 4: Ancient Remedy (chain from Plague Village - GOOD path) ──
        questManager.registerQuest(Quest.builder("quest_ancient_remedy")
                .title("The Ancient Remedy")
                .description("Rumours speak of an ancient cure deep in the corrupted forest. "
                        + "Will you brave its dangers?")
                .minAlignment(20) // requires GOOD alignment
                .prerequisites(List.of("quest_plague_village"))
                .choices(List.of(
                        QuestChoice.builder("choice_brave_forest")
                                .description("Enter the corrupted forest and retrieve the remedy.")
                                .moralPath(QuestChoice.MoralPath.GOOD)
                                .alignmentDelta(15)
                                .rewards(List.of(QuestReward.abilityUnlock("resurrection"),
                                                 QuestReward.scaledGold(120),
                                                 QuestReward.experience(300)))
                                .build()
                ))
                .build());

        // ── Quest 5: The Corrupt Official ─────────────────────────────────────
        questManager.registerQuest(Quest.builder("quest_corrupt_official")
                .title("The Corrupt Official")
                .description("You discover the town official is embezzling funds. "
                        + "Evidence in hand — now what?")
                .choices(List.of(
                        QuestChoice.builder("choice_expose")
                                .description("Expose him publicly. Justice for the town.")
                                .moralPath(QuestChoice.MoralPath.GOOD)
                                .alignmentDelta(12)
                                .rewards(List.of(QuestReward.relationship("guard_captain", 20),
                                                 QuestReward.scaledGold(50)))
                                .build(),
                        QuestChoice.builder("choice_blackmail")
                                .description("Use the evidence to blackmail him for personal gain.")
                                .moralPath(QuestChoice.MoralPath.EVIL)
                                .alignmentDelta(-18)
                                .rewards(List.of(QuestReward.gold(250)))
                                .consequenceKey("NPC_HOSTILITY_INCREASE:zone=city_hall")
                                .build(),
                        QuestChoice.builder("choice_mediator")
                                .description("Broker a deal — he reforms, you stay quiet.")
                                .moralPath(QuestChoice.MoralPath.NEUTRAL)
                                .alignmentDelta(3)
                                .rewards(List.of(QuestReward.gold(100),
                                                 QuestReward.experience(80)))
                                .build()
                ))
                .build());

        fableLogger.info("Registered 5 default quests.");
    }

    // ── Shutdown ──────────────────────────────────────────────────────────────

    /**
     * Called by the Hytale server on plugin shutdown.
     * Gracefully shuts down all background threads and flushes player data.
     *
     * <p>TODO: Hytale API — verify the correct shutdown lifecycle hook name.</p>
     */
    public void onShutdown() {
        fableLogger.info("FableScript shutting down...");
        consequenceManager.shutdown();
        playerData.shutdown();
        eventBus.clearAll();
        fableLogger.info("FableScript shutdown complete.");
    }
}
