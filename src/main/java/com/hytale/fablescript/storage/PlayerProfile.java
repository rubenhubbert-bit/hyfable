package com.hytale.fablescript.storage;

import com.hytale.fablescript.core.MoralityAlignment;

import java.util.*;

/**
 * Complete persistent profile for a single player.
 *
 * <p>Loaded from the database on first login and cached in memory until logout.
 * Serialised to JSON by {@link DataPersistence} for the fallback JSON backend.</p>
 */
public class PlayerProfile {

    private final UUID uuid;
    private String playerName;
    private int alignment;
    private long lastSeenEpoch;
    private long firstSeenEpoch;
    private long totalPlaytimeSeconds;

    /** NPC relationship map: npcId → relationship value (-100 to +100). */
    private final Map<String, Integer> npcRelationships;

    /** Set of unlocked ability IDs. */
    private final Set<String> unlockedAbilities;

    /** Set of completed quest IDs. */
    private final Set<String> completedQuests;

    /** Map of quest choices: questId_nodeId → choiceId taken. */
    private final Map<String, String> questChoices;

    /**
     * Creates a brand-new profile with neutral alignment for a first-time player.
     *
     * @param uuid       player UUID
     * @param playerName player display name
     */
    public PlayerProfile(UUID uuid, String playerName) {
        this.uuid                = uuid;
        this.playerName          = playerName;
        this.alignment           = 0;
        this.lastSeenEpoch       = System.currentTimeMillis() / 1000L;
        this.firstSeenEpoch      = this.lastSeenEpoch;
        this.totalPlaytimeSeconds = 0L;
        this.npcRelationships    = new HashMap<>();
        this.unlockedAbilities   = new HashSet<>();
        this.completedQuests     = new HashSet<>();
        this.questChoices        = new HashMap<>();
    }

    /**
     * Full constructor used when re-hydrating a profile from the database.
     *
     * @param uuid                  player UUID
     * @param playerName            player name
     * @param alignment             stored alignment value
     * @param lastSeenEpoch         epoch-second of last session
     * @param firstSeenEpoch        epoch-second of first join
     * @param totalPlaytimeSeconds  cumulative playtime in seconds
     * @param npcRelationships      persisted NPC relationship map
     * @param unlockedAbilities     persisted unlocked ability IDs
     * @param completedQuests       persisted completed quest IDs
     * @param questChoices          persisted quest-node choice map
     */
    public PlayerProfile(UUID uuid, String playerName, int alignment,
                          long lastSeenEpoch, long firstSeenEpoch,
                          long totalPlaytimeSeconds,
                          Map<String, Integer> npcRelationships,
                          Set<String> unlockedAbilities,
                          Set<String> completedQuests,
                          Map<String, String> questChoices) {
        this.uuid                 = uuid;
        this.playerName           = playerName;
        this.alignment            = alignment;
        this.lastSeenEpoch        = lastSeenEpoch;
        this.firstSeenEpoch       = firstSeenEpoch;
        this.totalPlaytimeSeconds = totalPlaytimeSeconds;
        this.npcRelationships     = new HashMap<>(npcRelationships);
        this.unlockedAbilities    = new HashSet<>(unlockedAbilities);
        this.completedQuests      = new HashSet<>(completedQuests);
        this.questChoices         = new HashMap<>(questChoices);
    }

    // ── Alignment ─────────────────────────────────────────────────────────────

    /** @return current alignment value [-100, 100] */
    public int getAlignment() { return alignment; }

    /**
     * Updates the stored alignment value.
     *
     * @param alignment new value, must be pre-clamped by caller
     */
    public void setAlignment(int alignment) { this.alignment = alignment; }

    /** @return derived alignment tier */
    public MoralityAlignment getAlignmentTier() { return MoralityAlignment.fromValue(alignment); }

    // ── NPC Relationships ────────────────────────────────────────────────────

    /**
     * Returns the relationship value with a specific NPC, defaulting to the NPC's
     * configured starting relationship if not yet set.
     *
     * @param npcId              NPC identifier
     * @param defaultRelationship NPC's starting value (from NPCDefinitions)
     * @return relationship value [-100, 100]
     */
    public int getNpcRelationship(String npcId, int defaultRelationship) {
        return npcRelationships.getOrDefault(npcId, defaultRelationship);
    }

    /**
     * Sets the relationship value with an NPC, clamped to [-100, 100].
     *
     * @param npcId NPC identifier
     * @param value relationship value
     */
    public void setNpcRelationship(String npcId, int value) {
        npcRelationships.put(npcId, Math.max(-100, Math.min(100, value)));
    }

    /** @return unmodifiable view of all NPC relationship values */
    public Map<String, Integer> getAllNpcRelationships() {
        return Collections.unmodifiableMap(npcRelationships);
    }

    // ── Abilities ────────────────────────────────────────────────────────────

    /**
     * Unlocks an ability for this player.
     *
     * @param abilityId unique ability identifier
     */
    public void unlockAbility(String abilityId) { unlockedAbilities.add(abilityId); }

    /**
     * Revokes an ability (e.g., alignment shift locked the player out).
     *
     * @param abilityId unique ability identifier
     */
    public void revokeAbility(String abilityId) { unlockedAbilities.remove(abilityId); }

    /** @return true if the ability is currently unlocked */
    public boolean hasAbility(String abilityId) { return unlockedAbilities.contains(abilityId); }

    /** @return unmodifiable set of all unlocked ability IDs */
    public Set<String> getUnlockedAbilities() { return Collections.unmodifiableSet(unlockedAbilities); }

    // ── Quests ───────────────────────────────────────────────────────────────

    /** @return true if the specified quest has been completed */
    public boolean hasCompletedQuest(String questId) { return completedQuests.contains(questId); }

    /** Marks a quest as completed. */
    public void markQuestCompleted(String questId) { completedQuests.add(questId); }

    /** @return unmodifiable set of all completed quest IDs */
    public Set<String> getCompletedQuests() { return Collections.unmodifiableSet(completedQuests); }

    /**
     * Records the player's choice at a specific quest branch node.
     *
     * @param questId  quest identifier
     * @param nodeId   dialogue/quest node ID
     * @param choiceId the option the player selected
     */
    public void recordQuestChoice(String questId, String nodeId, String choiceId) {
        questChoices.put(questId + ":" + nodeId, choiceId);
    }

    /**
     * Returns the choice the player made at a specific quest node, if any.
     *
     * @param questId quest identifier
     * @param nodeId  node identifier
     * @return Optional containing the choiceId, or empty if not yet decided
     */
    public Optional<String> getQuestChoice(String questId, String nodeId) {
        return Optional.ofNullable(questChoices.get(questId + ":" + nodeId));
    }

    /** @return unmodifiable view of all quest choice entries */
    public Map<String, String> getAllQuestChoices() { return Collections.unmodifiableMap(questChoices); }

    // ── Identity / timing ────────────────────────────────────────────────────

    /** @return player UUID */
    public UUID getUuid() { return uuid; }

    /** @return last known player name */
    public String getPlayerName() { return playerName; }

    /** Updates the stored player name (handles name changes). */
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    /** @return epoch-second of last login/activity */
    public long getLastSeenEpoch() { return lastSeenEpoch; }

    /** Updates the last-seen timestamp to now. */
    public void touchLastSeen() { this.lastSeenEpoch = System.currentTimeMillis() / 1000L; }

    /** @return epoch-second of first join */
    public long getFirstSeenEpoch() { return firstSeenEpoch; }

    /** @return total playtime accumulated in seconds */
    public long getTotalPlaytimeSeconds() { return totalPlaytimeSeconds; }

    /** Adds seconds to the cumulative playtime counter. */
    public void addPlaytime(long seconds) { this.totalPlaytimeSeconds += seconds; }
}
