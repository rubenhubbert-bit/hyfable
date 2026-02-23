package com.hytale.fablescript.utils;

import com.hytale.fablescript.npc.DialogueTree;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Parses YAML dialogue tree files into {@link DialogueTree} objects.
 *
 * <p>Expected YAML structure:
 * <pre>
 * dialogue:
 *   root:
 *     text: "Hello traveller, what brings you here?"
 *     conditions:
 *       min_alignment: -100
 *       max_alignment: 100
 *     choices:
 *       - id: choice_help
 *         text: "I'm here to help."
 *         alignment_delta: 2
 *         next_node: "node_help"
 *         consequence: "quest_unlock:quest_helper"
 *       - id: choice_rob
 *         text: "Hand over your gold."
 *         alignment_delta: -8
 *         next_node: "node_hostile"
 *   node_help:
 *     text: "Wonderful! I could use assistance..."
 *     choices: []
 * </pre>
 * </p>
 */
public class DialogueParser {

    private final FableLogger logger;

    /**
     * @param logger plugin logger for warnings on malformed dialogue files
     */
    public DialogueParser(FableLogger logger) {
        this.logger = logger;
    }

    /**
     * Parses a dialogue YAML file from disk into a DialogueTree.
     *
     * @param file path to the .yml dialogue file
     * @return parsed DialogueTree, or an empty tree on parse failure
     */
    public DialogueTree parseFile(File file) {
        try (InputStream is = new FileInputStream(file)) {
            return parseStream(is, file.getName());
        } catch (IOException e) {
            logger.warn("Failed to read dialogue file: " + file.getPath(), e);
            return DialogueTree.empty();
        }
    }

    /**
     * Parses a dialogue YAML stream (e.g., from a JAR resource).
     *
     * @param stream  input stream of the YAML file
     * @param sourceName display name for error messages
     * @return parsed DialogueTree
     */
    @SuppressWarnings("unchecked")
    public DialogueTree parseStream(InputStream stream, String sourceName) {
        Yaml yaml = new Yaml();
        try {
            Map<String, Object> root = yaml.load(stream);
            if (root == null || !root.containsKey("dialogue")) {
                logger.warn("Dialogue file '" + sourceName + "' has no 'dialogue' key.");
                return DialogueTree.empty();
            }
            Map<String, Object> nodes = (Map<String, Object>) root.get("dialogue");
            return buildTree(nodes, sourceName);
        } catch (Exception e) {
            logger.warn("Error parsing dialogue file '" + sourceName + "'", e);
            return DialogueTree.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private DialogueTree buildTree(Map<String, Object> nodesMap, String sourceName) {
        DialogueTree tree = new DialogueTree();
        for (Map.Entry<String, Object> entry : nodesMap.entrySet()) {
            String nodeId = entry.getKey();
            Map<String, Object> nodeData = (Map<String, Object>) entry.getValue();
            DialogueTree.DialogueNode node = parseNode(nodeId, nodeData, sourceName);
            tree.addNode(nodeId, node);
        }
        return tree;
    }

    @SuppressWarnings("unchecked")
    private DialogueTree.DialogueNode parseNode(String id, Map<String, Object> data,
                                                 String sourceName) {
        String text = (String) data.getOrDefault("text", "...");
        int minAlignment = -100;
        int maxAlignment = 100;

        if (data.containsKey("conditions")) {
            Map<String, Object> cond = (Map<String, Object>) data.get("conditions");
            minAlignment = (int) cond.getOrDefault("min_alignment", -100);
            maxAlignment = (int) cond.getOrDefault("max_alignment", 100);
        }

        List<DialogueTree.DialogueChoice> choices = new ArrayList<>();
        List<Map<String, Object>> rawChoices =
                (List<Map<String, Object>>) data.getOrDefault("choices", List.of());

        for (Map<String, Object> c : rawChoices) {
            choices.add(new DialogueTree.DialogueChoice(
                    (String) c.getOrDefault("id", "unknown"),
                    (String) c.getOrDefault("text", "..."),
                    (int) c.getOrDefault("alignment_delta", 0),
                    (String) c.get("next_node"),
                    (String) c.get("consequence")
            ));
        }

        return new DialogueTree.DialogueNode(id, text, minAlignment, maxAlignment, choices);
    }
}
