package com.hytale.fablescript.npc;

import com.hytale.fablescript.config.NPCDefinitions;
import com.hytale.fablescript.storage.PlayerDataManager;
import com.hytale.fablescript.storage.PlayerProfile;
import com.hytale.fablescript.utils.DialogueParser;
import com.hytale.fablescript.utils.FableLogger;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for all NPC interactions, dialogue loading, and relationship tracking.
 *
 * <p>On startup, pre-loads all dialogue trees defined in {@code npcs.yml} from the
 * {@code dialogues/} subfolder of the plugin data directory.
 * Provides the primary API for player↔NPC interaction resolution at runtime.</p>
 */
public class NPCManager {

    /** Subfolder inside data folder where dialogue YAML files are stored. */
    private static final String DIALOGUE_DIR = "dialogues";

    private final NPCDefinitions definitions;
    private final PlayerDataManager playerData;
    private final DialogueParser parser;
    private final NPCBehavior behavior;
    private final FableLogger logger;
    private final File dataFolder;

    /** Cached dialogue trees keyed by NPC dialogue file name. */
    private final Map<String, DialogueTree> dialogueCache = new ConcurrentHashMap<>();

    /**
     * @param definitions NPC definition registry from config
     * @param playerData  player data manager for relationship persistence
     * @param parser      dialogue YAML parser
     * @param dataFolder  plugin data folder
     * @param logger      plugin logger
     */
    public NPCManager(NPCDefinitions definitions, PlayerDataManager playerData,
                       DialogueParser parser, File dataFolder, FableLogger logger) {
        this.definitions = definitions;
        this.playerData  = playerData;
        this.parser      = parser;
        this.behavior    = new NPCBehavior();
        this.dataFolder  = dataFolder;
        this.logger      = logger;
    }

    /** Pre-loads all dialogue trees defined in npcs.yml into memory. */
    public void initialise() {
        File dialogueDir = new File(dataFolder, DIALOGUE_DIR);
        dialogueDir.mkdirs();
        definitions.getAllEntries().values().forEach(entry -> loadDialogue(entry.dialogueTreeFile()));
        logger.info("Loaded " + dialogueCache.size() + " dialogue trees.");
    }

    // ── Interaction API ───────────────────────────────────────────────────────

    /**
     * Resolves the full behaviour state for an NPC toward a specific player.
     * Used by PlayerInteractionListener to decide what the NPC says and offers.
     *
     * @param npcId      NPC identifier from npcs.yml
     * @param playerUuid player UUID
     * @return Optional BehaviorState, empty if the NPC ID is unknown
     */
    public Optional<NPCBehavior.BehaviorState> getBehavior(String npcId, UUID playerUuid) {
        return definitions.getEntry(npcId).flatMap(entry ->
            playerData.getProfile(playerUuid).map(profile -> {
                NPCRelationship rel = buildRelationship(entry, profile);
                return behavior.compute(entry, rel, profile.getAlignment());
            })
        );
    }

    /**
     * Returns the dialogue tree for the given NPC, filtered to choices
     * visible at the player's current alignment.
     *
     * @param npcId      NPC identifier
     * @param playerUuid player UUID
     * @return Optional containing the dialogue tree, or empty if NPC unknown
     */
    public Optional<DialogueTree> getDialogue(String npcId, UUID playerUuid) {
        return definitions.getEntry(npcId)
                .map(entry -> dialogueCache.computeIfAbsent(
                        entry.dialogueTreeFile(), this::loadDialogue));
    }

    /**
     * Processes a player's dialogue choice: adjusts alignment, updates relationship,
     * and returns any consequence trigger string.
     *
     * @param npcId      NPC identifier
     * @param playerUuid player UUID
     * @param choice     the chosen dialogue option
     * @return consequence trigger string (e.g., "quest_unlock:quest_id"), or null
     */
    public String processDialogueChoice(String npcId, UUID playerUuid,
                                         DialogueTree.DialogueChoice choice) {
        definitions.getEntry(npcId).ifPresent(entry -> {
            // Adjust relationship based on choice alignment delta
            playerData.getProfile(playerUuid).ifPresent(profile -> {
                int relDelta = choice.alignmentDelta() / 3; // choices affect relationship more mildly
                profile.setNpcRelationship(npcId,
                        profile.getNpcRelationship(npcId, entry.startingRelationship()) + relDelta);
                logger.debug("NPC " + npcId + " relationship updated by " + relDelta
                        + " for " + playerUuid);
            });
        });
        return choice.hasConsequence() ? choice.consequence() : null;
    }

    /**
     * Adjusts a player's relationship with all NPCs following an alignment tier change.
     * Called from MoralityEventListener when the player's tier transitions.
     *
     * @param playerUuid  player UUID
     * @param oldTier     previous alignment tier
     * @param newTier     new alignment tier
     */
    public void onAlignmentTierChange(UUID playerUuid,
                                       com.hytale.fablescript.core.MoralityAlignment oldTier,
                                       com.hytale.fablescript.core.MoralityAlignment newTier) {
        playerData.getProfile(playerUuid).ifPresent(profile ->
            definitions.getAllEntries().forEach((npcId, entry) -> {
                int delta = behavior.alignmentShiftDelta(entry, oldTier, newTier);
                if (delta != 0) {
                    int current = profile.getNpcRelationship(npcId, entry.startingRelationship());
                    profile.setNpcRelationship(npcId, current + delta);
                }
            })
        );
    }

    /**
     * Returns the NPC relationship info for display in {@code /relationships}.
     *
     * @param playerUuid player UUID
     * @return map of npcDisplayName → relationship tier label
     */
    public Map<String, String> getRelationshipSummary(UUID playerUuid) {
        Map<String, String> summary = new LinkedHashMap<>();
        playerData.getProfile(playerUuid).ifPresent(profile ->
            definitions.getAllEntries().forEach((npcId, entry) -> {
                int value = profile.getNpcRelationship(npcId, entry.startingRelationship());
                NPCRelationship rel = new NPCRelationship(npcId, value);
                summary.put(entry.displayName(), rel.getTier().getLabel() + " (" + value + ")");
            })
        );
        return summary;
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private DialogueTree loadDialogue(String filename) {
        File file = new File(new File(dataFolder, DIALOGUE_DIR), filename);
        if (!file.exists()) {
            logger.warn("Dialogue file not found: " + file.getPath());
            return DialogueTree.empty();
        }
        return parser.parseFile(file);
    }

    private NPCRelationship buildRelationship(NPCDefinitions.NPCEntry entry, PlayerProfile profile) {
        int value = profile.getNpcRelationship(entry.id(), entry.startingRelationship());
        return new NPCRelationship(entry.id(), value);
    }
}
