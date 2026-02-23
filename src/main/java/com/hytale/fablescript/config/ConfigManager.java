package com.hytale.fablescript.config;

import com.hytale.fablescript.utils.FableLogger;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.*;
import java.util.Map;

/**
 * Loads and manages all YAML configuration files for FableScript.
 *
 * <p>On first startup, default config files are copied from the plugin JAR
 * to the plugin data folder so server administrators can customise them.
 * Call {@link #reload()} to hot-reload config files without restarting.</p>
 */
public class ConfigManager {

    private static final String CONFIG_FILE   = "config.yml";
    private static final String NPC_FILE      = "npcs.yml";

    private final File dataFolder;
    private final FableLogger logger;

    private FableConfig fableConfig;
    private NPCDefinitions npcDefinitions;

    /**
     * @param dataFolder plugin data directory (created automatically on startup)
     * @param logger     plugin logger
     */
    public ConfigManager(File dataFolder, FableLogger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    /**
     * Initialises the config system: creates default files if absent and loads all configs.
     * Must be called once during {@code FablePlugin.setup()}.
     */
    public void initialise() {
        dataFolder.mkdirs();
        saveDefaultIfAbsent(CONFIG_FILE);
        saveDefaultIfAbsent(NPC_FILE);
        loadAll();
    }

    /**
     * Reloads all configuration files from disk.
     * Safe to call at runtime via {@code /fable reload}.
     */
    public void reload() {
        logger.info("Reloading FableScript configuration...");
        loadAll();
        logger.info("Configuration reloaded successfully.");
    }

    /** @return the current FableConfig (never null after initialise()) */
    public FableConfig getFableConfig() { return fableConfig; }

    /** @return the current NPCDefinitions (never null after initialise()) */
    public NPCDefinitions getNpcDefinitions() { return npcDefinitions; }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void loadAll() {
        fableConfig = loadFableConfig();
        npcDefinitions = loadNpcDefinitions();
    }

    @SuppressWarnings("unchecked")
    private FableConfig loadFableConfig() {
        Map<String, Object> root = loadYaml(CONFIG_FILE);
        if (root == null) {
            logger.warn("config.yml not found or empty; using defaults.");
            return FableConfig.defaults().build();
        }

        Map<String, Object> f = cast(root.get("fable"));
        if (f == null) return FableConfig.defaults().build();

        FableConfig.Builder b = FableConfig.defaults();

        Map<String, Object> align = cast(f.get("alignment"));
        if (align != null) {
            applyDouble(align, "decay-per-day",          v -> b.decayPerDay(v));
            applyInt   (align, "max-change-per-action",  v -> b.maxChangePerAction(v));
            applyInt   (align, "good-ability-threshold", v -> b.goodAbilityThreshold(v));
            applyInt   (align, "evil-ability-threshold", v -> b.evilAbilityThreshold(v));
        }

        Map<String, Object> cons = cast(f.get("consequences"));
        if (cons != null) {
            applyBool  (cons, "environmental-enabled",        v -> b.environmentalEnabled(v));
            applyInt   (cons, "corruption-spread-radius",     v -> b.corruptionSpreadRadius(v));
            applyDouble(cons, "corruption-spread-chance",     v -> b.corruptionSpreadChance(v));
            applyInt   (cons, "corruption-persistence-hours", v -> b.corruptionPersistenceHours(v));
        }

        Map<String, Object> npcs = cast(f.get("npcs"));
        if (npcs != null) {
            applyBool(npcs, "dialogue-variation-enabled",  v -> b.dialogueVariationEnabled(v));
            applyBool(npcs, "relationship-memory-enabled", v -> b.relationshipMemoryEnabled(v));
            applyBool(npcs, "dynamic-behavior-enabled",    v -> b.dynamicBehaviourEnabled(v));
            applyInt (npcs, "reaction-radius",             v -> b.npcReactionRadius(v));
        }

        Map<String, Object> quests = cast(f.get("quests"));
        if (quests != null) {
            applyBool(quests, "multiple-solutions-enabled",           v -> b.multipleSolutionsEnabled(v));
            applyBool(quests, "alignment-based-quest-availability",   v -> b.alignmentBasedAvailability(v));
        }

        Map<String, Object> storage = cast(f.get("storage"));
        if (storage != null) {
            if (storage.containsKey("type")) b.storageType((String) storage.get("type"));
            applyInt(storage, "backup-interval-minutes", v -> b.backupIntervalMinutes(v));
        }

        Map<String, Object> world = cast(f.get("world-reactivity"));
        if (world != null) {
            applyBool(world, "enabled", v -> b.worldReactivityEnabled(v));
        }

        applyBool(f, "debug", v -> b.debugEnabled(v));
        return b.build();
    }

    private NPCDefinitions loadNpcDefinitions() {
        Map<String, Object> root = loadYaml(NPC_FILE);
        return new NPCDefinitions(root, logger);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(String filename) {
        File file = new File(dataFolder, filename);
        if (!file.exists()) return null;
        try (InputStream is = new FileInputStream(file)) {
            return new Yaml().load(is);
        } catch (IOException e) {
            logger.warn("Failed to read " + filename, e);
            return null;
        }
    }

    private void saveDefaultIfAbsent(String filename) {
        File target = new File(dataFolder, filename);
        if (target.exists()) return;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (in == null) {
                logger.warn("Default resource '" + filename + "' not found in JAR.");
                return;
            }
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Created default " + filename);
        } catch (IOException e) {
            logger.warn("Could not save default " + filename, e);
        }
    }

    // ── Tiny lambda-based setters to avoid boilerplate ────────────────────────

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) { return o == null ? null : (T) o; }

    private void applyInt(Map<String, Object> map, String key, java.util.function.IntConsumer c) {
        if (map.containsKey(key)) c.accept(((Number) map.get(key)).intValue());
    }

    private void applyDouble(Map<String, Object> map, String key,
                              java.util.function.Consumer<Double> c) {
        if (map.containsKey(key)) c.accept(((Number) map.get(key)).doubleValue());
    }

    private void applyBool(Map<String, Object> map, String key,
                            java.util.function.Consumer<Boolean> c) {
        if (map.containsKey(key)) c.accept((Boolean) map.get(key));
    }
}
