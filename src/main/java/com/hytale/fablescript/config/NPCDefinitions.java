package com.hytale.fablescript.config;

import com.hytale.fablescript.utils.FableLogger;

import java.util.*;

/**
 * Parsed representation of {@code npcs.yml}, providing typed NPC definition records.
 *
 * <p>Each entry in the YAML file becomes an {@link NPCEntry} keyed by the NPC's
 * unique string ID (e.g., {@code "merchant_thomas"}).</p>
 */
public class NPCDefinitions {

    private final Map<String, NPCEntry> entries;

    /**
     * Parses raw YAML data into NPC definition records.
     *
     * @param rawYaml root YAML map from {@code npcs.yml} (may be null)
     * @param logger  plugin logger for parse warnings
     */
    @SuppressWarnings("unchecked")
    public NPCDefinitions(Map<String, Object> rawYaml, FableLogger logger) {
        this.entries = new LinkedHashMap<>();
        if (rawYaml == null) return;

        Map<String, Object> npcsMap = (Map<String, Object>) rawYaml.get("npcs");
        if (npcsMap == null) {
            logger.warn("npcs.yml has no 'npcs' section.");
            return;
        }

        for (Map.Entry<String, Object> e : npcsMap.entrySet()) {
            String id = e.getKey();
            try {
                NPCEntry entry = parseEntry(id, (Map<String, Object>) e.getValue());
                entries.put(id, entry);
            } catch (Exception ex) {
                logger.warn("Failed to parse NPC definition for '" + id + "'", ex);
            }
        }
        logger.info("Loaded " + entries.size() + " NPC definitions.");
    }

    /**
     * Returns the NPC definition for the given ID.
     *
     * @param npcId unique NPC identifier from npcs.yml
     * @return Optional containing the entry, or empty if not found
     */
    public Optional<NPCEntry> getEntry(String npcId) {
        return Optional.ofNullable(entries.get(npcId));
    }

    /**
     * Returns an unmodifiable view of all NPC definitions.
     *
     * @return map of npcId → NPCEntry
     */
    public Map<String, NPCEntry> getAllEntries() {
        return Collections.unmodifiableMap(entries);
    }

    /** @return number of loaded NPC definitions */
    public int count() { return entries.size(); }

    @SuppressWarnings("unchecked")
    private NPCEntry parseEntry(String id, Map<String, Object> data) {
        String displayName = (String) data.getOrDefault("display-name", id);
        String personality = (String) data.getOrDefault("personality", "Pragmatic");
        int startingRelationship = (int) data.getOrDefault("starting-relationship", 0);
        String dialogueTree = (String) data.getOrDefault("dialogue-tree", "default_dialogue.yml");
        String location = (String) data.getOrDefault("location", "unknown");
        List<String> quests = (List<String>) data.getOrDefault("quests", List.of());

        boolean reactToAlignment = false;
        boolean hostileToEvil = false;
        String aiType = "static";
        int wanderRadius = 10;

        Map<String, Object> ai = (Map<String, Object>) data.get("ai-behavior");
        if (ai != null) {
            aiType = (String) ai.getOrDefault("type", "static");
            reactToAlignment = (boolean) ai.getOrDefault("react-to-alignment", false);
            hostileToEvil = (boolean) ai.getOrDefault("hostile-to-evil", false);
            wanderRadius = (int) ai.getOrDefault("wander-radius", 10);
        }

        return new NPCEntry(id, displayName, personality, startingRelationship,
                dialogueTree, location, List.copyOf(quests),
                aiType, reactToAlignment, hostileToEvil, wanderRadius);
    }

    /**
     * Immutable record representing a single NPC's definition from npcs.yml.
     *
     * @param id                   unique NPC identifier
     * @param displayName          in-game name shown to players
     * @param personality          personality archetype (Noble, Greedy, Vengeful, Idealistic, Pragmatic)
     * @param startingRelationship base relationship value with all players (-100 to +100)
     * @param dialogueTreeFile     filename of this NPC's dialogue YAML
     * @param location             world location tag or coordinate descriptor
     * @param questIds             list of quest IDs this NPC offers
     * @param aiType               AI behaviour type (static, wanderer, patrol)
     * @param reactToAlignment     true if this NPC changes behaviour based on player alignment
     * @param hostileToEvil        true if this NPC attacks PURE_EVIL players on sight
     * @param wanderRadius         block radius for wandering AI type
     */
    public record NPCEntry(
            String id, String displayName, String personality, int startingRelationship,
            String dialogueTreeFile, String location, List<String> questIds,
            String aiType, boolean reactToAlignment, boolean hostileToEvil, int wanderRadius
    ) {}
}
