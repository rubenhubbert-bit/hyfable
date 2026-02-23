package com.hytale.fablescript.config;

/**
 * Strongly-typed value object containing all FableScript configuration parameters.
 *
 * <p>Populated by {@link ConfigManager} from {@code config.yml} on startup
 * and on {@code /fable reload}. All fields are immutable after construction.</p>
 */
public final class FableConfig {

    // ── Alignment ────────────────────────────────────────────────────────────

    /** Daily decay amount toward neutral for inactive players (e.g., 0.1). */
    private final double decayPerDay;

    /** Maximum alignment change from a single player action (default 25). */
    private final int maxChangePerAction;

    /** Minimum alignment required to access GOOD abilities (default +20). */
    private final int goodAbilityThreshold;

    /** Maximum alignment allowed to access EVIL abilities (default -20). */
    private final int evilAbilityThreshold;

    // ── Consequences ─────────────────────────────────────────────────────────

    /** Whether environmental consequences (corruption/blessing) are active. */
    private final boolean environmentalEnabled;

    /** Radius in blocks around the player that corruption/blessing can spread. */
    private final int corruptionSpreadRadius;

    /** Probability (0–1) of corruption spreading per check interval. */
    private final double corruptionSpreadChance;

    /** Hours that environmental corruption persists after the player leaves. */
    private final int corruptionPersistenceHours;

    // ── NPCs ─────────────────────────────────────────────────────────────────

    /** Whether NPCs use alignment-reactive dialogue variants. */
    private final boolean dialogueVariationEnabled;

    /** Whether NPCs remember past player choices. */
    private final boolean relationshipMemoryEnabled;

    /** Whether NPCs dynamically change patrol/flee behaviours based on alignment. */
    private final boolean dynamicBehaviourEnabled;

    /** Radius in blocks within which NPCs react to a player's alignment. */
    private final int npcReactionRadius;

    // ── Quests ───────────────────────────────────────────────────────────────

    /** Whether quests offer multiple moral-path solutions. */
    private final boolean multipleSolutionsEnabled;

    /** Whether quest availability is gated by player alignment. */
    private final boolean alignmentBasedAvailability;

    // ── Storage ──────────────────────────────────────────────────────────────

    /** Storage backend: "sqlite" or "json". */
    private final String storageType;

    /** How often player data is auto-saved in minutes (default 30). */
    private final int backupIntervalMinutes;

    // ── World Reactivity ─────────────────────────────────────────────────────

    /** Whether the world reactivity system is enabled. */
    private final boolean worldReactivityEnabled;

    // ── Debug ────────────────────────────────────────────────────────────────

    /** Whether verbose debug logging is active. */
    private final boolean debugEnabled;

    /**
     * Constructs a FableConfig with all parameters. Callers should use
     * {@link Builder} rather than this constructor directly.
     */
    private FableConfig(Builder b) {
        this.decayPerDay              = b.decayPerDay;
        this.maxChangePerAction       = b.maxChangePerAction;
        this.goodAbilityThreshold     = b.goodAbilityThreshold;
        this.evilAbilityThreshold     = b.evilAbilityThreshold;
        this.environmentalEnabled     = b.environmentalEnabled;
        this.corruptionSpreadRadius   = b.corruptionSpreadRadius;
        this.corruptionSpreadChance   = b.corruptionSpreadChance;
        this.corruptionPersistenceHours = b.corruptionPersistenceHours;
        this.dialogueVariationEnabled = b.dialogueVariationEnabled;
        this.relationshipMemoryEnabled= b.relationshipMemoryEnabled;
        this.dynamicBehaviourEnabled  = b.dynamicBehaviourEnabled;
        this.npcReactionRadius        = b.npcReactionRadius;
        this.multipleSolutionsEnabled = b.multipleSolutionsEnabled;
        this.alignmentBasedAvailability = b.alignmentBasedAvailability;
        this.storageType              = b.storageType;
        this.backupIntervalMinutes    = b.backupIntervalMinutes;
        this.worldReactivityEnabled   = b.worldReactivityEnabled;
        this.debugEnabled             = b.debugEnabled;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    /** @return daily alignment decay rate toward neutral */
    public double getDecayPerDay() { return decayPerDay; }

    /** @return maximum alignment delta permitted per single action */
    public int getMaxChangePerAction() { return maxChangePerAction; }

    /** @return minimum alignment to access good abilities */
    public int getGoodAbilityThreshold() { return goodAbilityThreshold; }

    /** @return maximum (most-negative) alignment to access evil abilities */
    public int getEvilAbilityThreshold() { return evilAbilityThreshold; }

    /** @return true if environmental corruption/blessing are enabled */
    public boolean isEnvironmentalEnabled() { return environmentalEnabled; }

    /** @return block radius for corruption/blessing spread */
    public int getCorruptionSpreadRadius() { return corruptionSpreadRadius; }

    /** @return per-tick probability of corruption spreading */
    public double getCorruptionSpreadChance() { return corruptionSpreadChance; }

    /** @return hours environmental corruption persists after player departs */
    public int getCorruptionPersistenceHours() { return corruptionPersistenceHours; }

    /** @return true if NPCs use alignment-specific dialogue */
    public boolean isDialogueVariationEnabled() { return dialogueVariationEnabled; }

    /** @return true if NPCs remember past player interactions */
    public boolean isRelationshipMemoryEnabled() { return relationshipMemoryEnabled; }

    /** @return true if NPCs dynamically change behaviour based on alignment */
    public boolean isDynamicBehaviourEnabled() { return dynamicBehaviourEnabled; }

    /** @return block radius within which NPCs react to a player's alignment */
    public int getNpcReactionRadius() { return npcReactionRadius; }

    /** @return true if quests can be completed via multiple moral paths */
    public boolean isMultipleSolutionsEnabled() { return multipleSolutionsEnabled; }

    /** @return true if quest availability is filtered by player alignment */
    public boolean isAlignmentBasedAvailability() { return alignmentBasedAvailability; }

    /** @return storage backend identifier ("sqlite" or "json") */
    public String getStorageType() { return storageType; }

    /** @return minutes between automatic player-data saves */
    public int getBackupIntervalMinutes() { return backupIntervalMinutes; }

    /** @return true if world reactivity system is active */
    public boolean isWorldReactivityEnabled() { return worldReactivityEnabled; }

    /** @return true if verbose debug logging is on */
    public boolean isDebugEnabled() { return debugEnabled; }

    // ── Builder ──────────────────────────────────────────────────────────────

    /** Creates a Builder pre-loaded with sensible defaults. */
    public static Builder defaults() { return new Builder(); }

    /** Fluent builder for FableConfig. */
    public static final class Builder {
        private double decayPerDay              = 0.1;
        private int    maxChangePerAction       = 25;
        private int    goodAbilityThreshold     = 20;
        private int    evilAbilityThreshold     = -20;
        private boolean environmentalEnabled    = true;
        private int    corruptionSpreadRadius   = 50;
        private double corruptionSpreadChance   = 0.3;
        private int    corruptionPersistenceHours = 24;
        private boolean dialogueVariationEnabled= true;
        private boolean relationshipMemoryEnabled = true;
        private boolean dynamicBehaviourEnabled = true;
        private int    npcReactionRadius        = 100;
        private boolean multipleSolutionsEnabled= true;
        private boolean alignmentBasedAvailability = true;
        private String storageType              = "sqlite";
        private int    backupIntervalMinutes    = 30;
        private boolean worldReactivityEnabled  = true;
        private boolean debugEnabled            = false;

        public Builder decayPerDay(double v)              { decayPerDay = v; return this; }
        public Builder maxChangePerAction(int v)          { maxChangePerAction = v; return this; }
        public Builder goodAbilityThreshold(int v)        { goodAbilityThreshold = v; return this; }
        public Builder evilAbilityThreshold(int v)        { evilAbilityThreshold = v; return this; }
        public Builder environmentalEnabled(boolean v)    { environmentalEnabled = v; return this; }
        public Builder corruptionSpreadRadius(int v)      { corruptionSpreadRadius = v; return this; }
        public Builder corruptionSpreadChance(double v)   { corruptionSpreadChance = v; return this; }
        public Builder corruptionPersistenceHours(int v)  { corruptionPersistenceHours = v; return this; }
        public Builder dialogueVariationEnabled(boolean v){ dialogueVariationEnabled = v; return this; }
        public Builder relationshipMemoryEnabled(boolean v){ relationshipMemoryEnabled = v; return this; }
        public Builder dynamicBehaviourEnabled(boolean v) { dynamicBehaviourEnabled = v; return this; }
        public Builder npcReactionRadius(int v)           { npcReactionRadius = v; return this; }
        public Builder multipleSolutionsEnabled(boolean v){ multipleSolutionsEnabled = v; return this; }
        public Builder alignmentBasedAvailability(boolean v){ alignmentBasedAvailability = v; return this; }
        public Builder storageType(String v)              { storageType = v; return this; }
        public Builder backupIntervalMinutes(int v)       { backupIntervalMinutes = v; return this; }
        public Builder worldReactivityEnabled(boolean v)  { worldReactivityEnabled = v; return this; }
        public Builder debugEnabled(boolean v)            { debugEnabled = v; return this; }

        /** @return a new immutable FableConfig */
        public FableConfig build() { return new FableConfig(this); }
    }
}
