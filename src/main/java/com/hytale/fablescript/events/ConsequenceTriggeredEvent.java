package com.hytale.fablescript.events;

import java.util.UUID;

/**
 * Fired when a delayed or cascading consequence resolves and affects the world.
 *
 * <p>Consequences are scheduled by {@link com.hytale.fablescript.core.ConsequenceManager}
 * and may be immediate, delayed by game-days, or triggered by player proximity.</p>
 */
public class ConsequenceTriggeredEvent {

    /** Identifies the category of consequence (e.g., "CORRUPTION", "BLESSING", "QUEST_UPDATE"). */
    public enum ConsequenceType {
        ENVIRONMENTAL_CORRUPTION,
        ENVIRONMENTAL_BLESSING,
        NPC_HOSTILITY_INCREASE,
        NPC_FRIENDLINESS_INCREASE,
        QUEST_STATE_CHANGE,
        ABILITY_UNLOCK,
        ABILITY_REVOKE,
        WORLD_MARKER_SET,
        BOUNTY_PLACED,
        BOUNTY_REMOVED
    }

    private final UUID playerUuid;
    private final ConsequenceType type;

    /** Arbitrary key-value payload for the consequence (serialized as "key=value;..." string). */
    private final String payload;

    /** Zone or location tag where this consequence applies (may be null for global). */
    private final String zoneKey;

    private final long triggeredAtMillis;

    /**
     * Constructs a ConsequenceTriggeredEvent.
     *
     * @param playerUuid       UUID of the player whose past action caused this consequence
     * @param type             category of the consequence
     * @param payload          semicolon-delimited key=value data string
     * @param zoneKey          world zone affected, or null for server-wide
     */
    public ConsequenceTriggeredEvent(UUID playerUuid, ConsequenceType type,
                                      String payload, String zoneKey) {
        this.playerUuid = playerUuid;
        this.type = type;
        this.payload = payload;
        this.zoneKey = zoneKey;
        this.triggeredAtMillis = System.currentTimeMillis();
    }

    /** @return UUID of the player whose action triggered this consequence */
    public UUID getPlayerUuid() { return playerUuid; }

    /** @return the category of consequence */
    public ConsequenceType getType() { return type; }

    /** @return semicolon-delimited key=value payload string */
    public String getPayload() { return payload; }

    /** @return zone key affected by this consequence, or null if server-wide */
    public String getZoneKey() { return zoneKey; }

    /** @return wall-clock time (epoch millis) when this consequence fired */
    public long getTriggeredAtMillis() { return triggeredAtMillis; }

    /**
     * Convenience: reads a single value from the payload by key.
     *
     * @param key the key to look up in the "key=value;..." payload
     * @return value string, or null if the key is absent
     */
    public String getPayloadValue(String key) {
        if (payload == null || payload.isBlank()) return null;
        for (String entry : payload.split(";")) {
            String[] parts = entry.split("=", 2);
            if (parts.length == 2 && parts[0].trim().equals(key)) {
                return parts[1].trim();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("ConsequenceTriggeredEvent{player=%s, type=%s, zone='%s'}",
                playerUuid, type, zoneKey);
    }
}
