package com.hytale.fablescript.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hytale.fablescript.utils.FableLogger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * Low-level data persistence layer supporting both SQLite and JSON backends.
 *
 * <p>All database I/O is intended to be called from an async thread via
 * {@link PlayerDataManager}. Methods in this class are blocking and must
 * <em>not</em> be called on the main server thread.</p>
 *
 * <h3>SQLite schema</h3>
 * <pre>
 *   players             — uuid, name, alignment, last_seen, first_seen, playtime_seconds
 *   npc_relationships   — player_uuid, npc_id, relationship_value
 *   unlocked_abilities  — player_uuid, ability_id
 *   completed_quests    — player_uuid, quest_id
 *   quest_choices       — player_uuid, choice_key, choice_id
 * </pre>
 */
public class DataPersistence {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>(){}.getType();

    private final File dataFolder;
    private final FableLogger logger;
    private final String storageType;

    /** SQLite connection — null when using JSON backend. */
    private Connection sqliteConnection;

    /**
     * @param dataFolder  plugin data folder
     * @param storageType "sqlite" or "json"
     * @param logger      plugin logger
     */
    public DataPersistence(File dataFolder, String storageType, FableLogger logger) {
        this.dataFolder  = dataFolder;
        this.storageType = storageType;
        this.logger      = logger;
    }

    /**
     * Opens the database connection and creates schema tables if absent.
     * Must be called once before any other method.
     */
    public void initialise() {
        if ("sqlite".equalsIgnoreCase(storageType)) {
            initialiseSqlite();
        } else {
            new File(dataFolder, "playerdata").mkdirs();
            logger.info("Using JSON backend — player data stored in playerdata/");
        }
    }

    /** Cleanly closes the database connection. */
    public void close() {
        if (sqliteConnection != null) {
            try { sqliteConnection.close(); }
            catch (SQLException e) { logger.warn("Error closing SQLite connection", e); }
        }
    }

    // ── Load / Save ──────────────────────────────────────────────────────────

    /**
     * Loads a PlayerProfile from the configured backend.
     *
     * @param uuid player UUID
     * @return Optional containing the profile if found, or empty for new players
     */
    public Optional<PlayerProfile> loadProfile(UUID uuid) {
        try {
            return "sqlite".equalsIgnoreCase(storageType)
                    ? loadFromSqlite(uuid)
                    : loadFromJson(uuid);
        } catch (Exception e) {
            logger.warn("Failed to load profile for " + uuid, e);
            return Optional.empty();
        }
    }

    /**
     * Saves a PlayerProfile to the configured backend.
     *
     * @param profile the profile to persist
     */
    public void saveProfile(PlayerProfile profile) {
        try {
            if ("sqlite".equalsIgnoreCase(storageType)) {
                saveToSqlite(profile);
            } else {
                saveToJson(profile);
            }
        } catch (Exception e) {
            logger.warn("Failed to save profile for " + profile.getUuid(), e);
        }
    }

    /**
     * Exports a player profile to a human-readable JSON file for admin review.
     *
     * @param profile      profile to export
     * @param outputFolder destination directory
     * @return path of the written file
     */
    public Path exportToJson(PlayerProfile profile, File outputFolder) throws IOException {
        outputFolder.mkdirs();
        Path out = outputFolder.toPath().resolve(profile.getUuid() + "_export.json");
        String json = GSON.toJson(profileToMap(profile));
        Files.writeString(out, json, StandardCharsets.UTF_8);
        return out;
    }

    // ── SQLite implementation ─────────────────────────────────────────────────

    private void initialiseSqlite() {
        try {
            Class.forName("org.sqlite.JDBC");
            File dbFile = new File(dataFolder, "fablescript.db");
            sqliteConnection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createSchema();
            logger.info("SQLite database initialised at " + dbFile.getPath());
        } catch (Exception e) {
            logger.severe("Failed to initialise SQLite; falling back to JSON.", e);
            sqliteConnection = null;
        }
    }

    private void createSchema() throws SQLException {
        try (Statement stmt = sqliteConnection.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    alignment INTEGER DEFAULT 0,
                    last_seen INTEGER DEFAULT 0,
                    first_seen INTEGER DEFAULT 0,
                    playtime_seconds INTEGER DEFAULT 0
                )""");
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS npc_relationships (
                    player_uuid TEXT NOT NULL,
                    npc_id TEXT NOT NULL,
                    relationship_value INTEGER DEFAULT 0,
                    PRIMARY KEY (player_uuid, npc_id)
                )""");
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS unlocked_abilities (
                    player_uuid TEXT NOT NULL,
                    ability_id TEXT NOT NULL,
                    PRIMARY KEY (player_uuid, ability_id)
                )""");
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS completed_quests (
                    player_uuid TEXT NOT NULL,
                    quest_id TEXT NOT NULL,
                    PRIMARY KEY (player_uuid, quest_id)
                )""");
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS quest_choices (
                    player_uuid TEXT NOT NULL,
                    choice_key TEXT NOT NULL,
                    choice_id TEXT NOT NULL,
                    PRIMARY KEY (player_uuid, choice_key)
                )""");
        }
    }

    private Optional<PlayerProfile> loadFromSqlite(UUID uuid) throws SQLException {
        String uuidStr = uuid.toString();
        try (PreparedStatement ps = sqliteConnection.prepareStatement(
                "SELECT * FROM players WHERE uuid = ?")) {
            ps.setString(1, uuidStr);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return Optional.empty();

            String name = rs.getString("name");
            int alignment = rs.getInt("alignment");
            long lastSeen = rs.getLong("last_seen");
            long firstSeen = rs.getLong("first_seen");
            long playtime = rs.getLong("playtime_seconds");

            Map<String, Integer> rels = loadNpcRelationships(uuidStr);
            Set<String> abilities = loadAbilities(uuidStr);
            Set<String> quests = loadCompletedQuests(uuidStr);
            Map<String, String> choices = loadQuestChoices(uuidStr);

            return Optional.of(new PlayerProfile(uuid, name, alignment, lastSeen, firstSeen,
                    playtime, rels, abilities, quests, choices));
        }
    }

    private void saveToSqlite(PlayerProfile p) throws SQLException {
        String uuidStr = p.getUuid().toString();
        try (PreparedStatement ps = sqliteConnection.prepareStatement("""
                INSERT INTO players (uuid,name,alignment,last_seen,first_seen,playtime_seconds)
                VALUES (?,?,?,?,?,?)
                ON CONFLICT(uuid) DO UPDATE SET
                  name=excluded.name, alignment=excluded.alignment,
                  last_seen=excluded.last_seen, playtime_seconds=excluded.playtime_seconds
                """)) {
            ps.setString(1, uuidStr);
            ps.setString(2, p.getPlayerName());
            ps.setInt(3, p.getAlignment());
            ps.setLong(4, p.getLastSeenEpoch());
            ps.setLong(5, p.getFirstSeenEpoch());
            ps.setLong(6, p.getTotalPlaytimeSeconds());
            ps.executeUpdate();
        }
        saveNpcRelationships(uuidStr, p.getAllNpcRelationships());
        saveAbilities(uuidStr, p.getUnlockedAbilities());
        saveCompletedQuests(uuidStr, p.getCompletedQuests());
        saveQuestChoices(uuidStr, p.getAllQuestChoices());
    }

    private Map<String, Integer> loadNpcRelationships(String uuid) throws SQLException {
        Map<String, Integer> result = new HashMap<>();
        try (PreparedStatement ps = sqliteConnection.prepareStatement(
                "SELECT npc_id, relationship_value FROM npc_relationships WHERE player_uuid = ?")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.put(rs.getString("npc_id"), rs.getInt("relationship_value"));
        }
        return result;
    }

    private Set<String> loadAbilities(String uuid) throws SQLException {
        Set<String> result = new HashSet<>();
        try (PreparedStatement ps = sqliteConnection.prepareStatement(
                "SELECT ability_id FROM unlocked_abilities WHERE player_uuid = ?")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.add(rs.getString("ability_id"));
        }
        return result;
    }

    private Set<String> loadCompletedQuests(String uuid) throws SQLException {
        Set<String> result = new HashSet<>();
        try (PreparedStatement ps = sqliteConnection.prepareStatement(
                "SELECT quest_id FROM completed_quests WHERE player_uuid = ?")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.add(rs.getString("quest_id"));
        }
        return result;
    }

    private Map<String, String> loadQuestChoices(String uuid) throws SQLException {
        Map<String, String> result = new HashMap<>();
        try (PreparedStatement ps = sqliteConnection.prepareStatement(
                "SELECT choice_key, choice_id FROM quest_choices WHERE player_uuid = ?")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.put(rs.getString("choice_key"), rs.getString("choice_id"));
        }
        return result;
    }

    private void saveNpcRelationships(String uuid, Map<String, Integer> rels) throws SQLException {
        try (PreparedStatement ps = sqliteConnection.prepareStatement("""
                INSERT INTO npc_relationships(player_uuid,npc_id,relationship_value) VALUES(?,?,?)
                ON CONFLICT(player_uuid,npc_id) DO UPDATE SET relationship_value=excluded.relationship_value
                """)) {
            for (Map.Entry<String, Integer> e : rels.entrySet()) {
                ps.setString(1, uuid); ps.setString(2, e.getKey()); ps.setInt(3, e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void saveAbilities(String uuid, Set<String> abilities) throws SQLException {
        try (PreparedStatement ps = sqliteConnection.prepareStatement(
                "INSERT OR IGNORE INTO unlocked_abilities(player_uuid,ability_id) VALUES(?,?)")) {
            for (String a : abilities) {
                ps.setString(1, uuid); ps.setString(2, a); ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void saveCompletedQuests(String uuid, Set<String> quests) throws SQLException {
        try (PreparedStatement ps = sqliteConnection.prepareStatement(
                "INSERT OR IGNORE INTO completed_quests(player_uuid,quest_id) VALUES(?,?)")) {
            for (String q : quests) {
                ps.setString(1, uuid); ps.setString(2, q); ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void saveQuestChoices(String uuid, Map<String, String> choices) throws SQLException {
        try (PreparedStatement ps = sqliteConnection.prepareStatement("""
                INSERT INTO quest_choices(player_uuid,choice_key,choice_id) VALUES(?,?,?)
                ON CONFLICT(player_uuid,choice_key) DO UPDATE SET choice_id=excluded.choice_id
                """)) {
            for (Map.Entry<String, String> e : choices.entrySet()) {
                ps.setString(1, uuid); ps.setString(2, e.getKey()); ps.setString(3, e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ── JSON implementation ───────────────────────────────────────────────────

    private Optional<PlayerProfile> loadFromJson(UUID uuid) throws IOException {
        File file = jsonFile(uuid);
        if (!file.exists()) return Optional.empty();
        String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        Map<String, Object> map = GSON.fromJson(json, MAP_TYPE);
        return Optional.of(mapToProfile(uuid, map));
    }

    private void saveToJson(PlayerProfile p) throws IOException {
        String json = GSON.toJson(profileToMap(p));
        Files.writeString(jsonFile(p.getUuid()).toPath(), json, StandardCharsets.UTF_8);
    }

    private File jsonFile(UUID uuid) {
        return new File(new File(dataFolder, "playerdata"), uuid + ".json");
    }

    @SuppressWarnings("unchecked")
    private PlayerProfile mapToProfile(UUID uuid, Map<String, Object> m) {
        String name = (String) m.getOrDefault("name", "Unknown");
        int alignment = ((Number) m.getOrDefault("alignment", 0)).intValue();
        long lastSeen = ((Number) m.getOrDefault("last_seen", 0L)).longValue();
        long firstSeen = ((Number) m.getOrDefault("first_seen", 0L)).longValue();
        long playtime = ((Number) m.getOrDefault("playtime_seconds", 0L)).longValue();
        Map<String, Integer> rels = (Map<String, Integer>) m.getOrDefault("npc_relationships", Map.of());
        List<String> abilList = (List<String>) m.getOrDefault("unlocked_abilities", List.of());
        List<String> questList = (List<String>) m.getOrDefault("completed_quests", List.of());
        Map<String, String> choices = (Map<String, String>) m.getOrDefault("quest_choices", Map.of());
        return new PlayerProfile(uuid, name, alignment, lastSeen, firstSeen, playtime,
                rels, new HashSet<>(abilList), new HashSet<>(questList), choices);
    }

    private Map<String, Object> profileToMap(PlayerProfile p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("uuid",                 p.getUuid().toString());
        m.put("name",                 p.getPlayerName());
        m.put("alignment",            p.getAlignment());
        m.put("last_seen",            p.getLastSeenEpoch());
        m.put("first_seen",           p.getFirstSeenEpoch());
        m.put("playtime_seconds",     p.getTotalPlaytimeSeconds());
        m.put("npc_relationships",    p.getAllNpcRelationships());
        m.put("unlocked_abilities",   new ArrayList<>(p.getUnlockedAbilities()));
        m.put("completed_quests",     new ArrayList<>(p.getCompletedQuests()));
        m.put("quest_choices",        p.getAllQuestChoices());
        return m;
    }
}
