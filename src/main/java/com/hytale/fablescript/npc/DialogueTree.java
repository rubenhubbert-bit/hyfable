package com.hytale.fablescript.npc;

import java.util.*;

/**
 * Represents a branching dialogue tree for a single NPC, parsed from YAML
 * by {@link com.hytale.fablescript.utils.DialogueParser}.
 *
 * <p>Dialogue trees are node graphs where each node holds NPC text and a list
 * of player response choices. Nodes have optional alignment conditions so
 * players only see choices appropriate to their moral tier.</p>
 */
public class DialogueTree {

    /** Sentinel node ID for the conversation entry point. */
    public static final String ROOT_NODE = "root";

    private final Map<String, DialogueNode> nodes = new LinkedHashMap<>();

    /** Constructs an empty tree (used as a safe fallback). */
    public DialogueTree() {}

    /**
     * Returns an empty DialogueTree used when a parse error occurs.
     *
     * @return empty, non-null DialogueTree
     */
    public static DialogueTree empty() {
        return new DialogueTree();
    }

    /**
     * Adds a node to this tree.
     *
     * @param id   unique node identifier within this tree
     * @param node the node to store
     */
    public void addNode(String id, DialogueNode node) {
        nodes.put(id, node);
    }

    /**
     * Returns the root node for the start of a conversation.
     *
     * @return Optional containing the root node, or empty if tree is empty
     */
    public Optional<DialogueNode> getRootNode() {
        return Optional.ofNullable(nodes.get(ROOT_NODE));
    }

    /**
     * Returns the node with the given ID.
     *
     * @param nodeId node identifier
     * @return Optional containing the node, or empty if not found
     */
    public Optional<DialogueNode> getNode(String nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    /**
     * Filters choices of a node to only those the player qualifies for
     * based on their current alignment value.
     *
     * @param node            dialogue node to filter
     * @param playerAlignment player's current alignment value
     * @return filtered list of available choices
     */
    public List<DialogueChoice> getAvailableChoices(DialogueNode node, int playerAlignment) {
        return node.choices().stream()
                .filter(c -> playerAlignment >= node.minAlignment()
                          && playerAlignment <= node.maxAlignment())
                .toList();
    }

    /** @return true if this tree has at least a root node */
    public boolean isValid() { return nodes.containsKey(ROOT_NODE); }

    /** @return number of nodes in this tree */
    public int nodeCount() { return nodes.size(); }

    // ── Nested types ─────────────────────────────────────────────────────────

    /**
     * A single node in the dialogue graph.
     *
     * @param id           unique node identifier
     * @param text         NPC speech text displayed to the player
     * @param minAlignment minimum player alignment required to reach this node
     * @param maxAlignment maximum player alignment allowed to reach this node
     * @param choices      list of player response options
     */
    public record DialogueNode(String id, String text,
                                int minAlignment, int maxAlignment,
                                List<DialogueChoice> choices) {

        /** @return true if this node ends the conversation (no choices). */
        public boolean isLeaf() { return choices == null || choices.isEmpty(); }
    }

    /**
     * A single player response option within a dialogue node.
     *
     * @param id             unique choice identifier within the node
     * @param text           player-visible response text
     * @param alignmentDelta alignment change applied when player selects this choice
     * @param nextNodeId     ID of the node to navigate to, or null to end conversation
     * @param consequence    consequence trigger string (e.g., "quest_unlock:quest_id"),
     *                       or null if no consequence
     */
    public record DialogueChoice(String id, String text, int alignmentDelta,
                                  String nextNodeId, String consequence) {

        /** @return true if selecting this choice triggers a consequence */
        public boolean hasConsequence() { return consequence != null && !consequence.isBlank(); }

        /** @return true if selecting this choice navigates to another node */
        public boolean continuesDialogue() { return nextNodeId != null && !nextNodeId.isBlank(); }
    }
}
