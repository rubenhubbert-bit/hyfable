# FableScript — Hytale Fable Morality & Consequence Plugin

A comprehensive Hytale server plugin that brings core Fable game mechanics to your server:
meaningful player choices, moral consequences, NPC relationship dynamics, and dynamic world reactivity.

---

## Features

| System | Description |
|---|---|
| **5-Tier Morality** | PURE_GOOD → GOOD → NEUTRAL → EVIL → PURE_EVIL with visual feedback |
| **Choice Tracking** | Every dialogue/quest decision logged with timestamp and cause |
| **NPC Relationships** | Per-player per-NPC relationship values (-100 to +100) |
| **Multi-Path Quests** | 5 built-in quests with GOOD/NEUTRAL/EVIL completion paths |
| **Ability System** | 15 abilities across 3 moral tiers with cooldown enforcement |
| **World Reactivity** | Zone corruption/blessing, dynamic weather, environmental effects |
| **SQLite Persistence** | Async player data with 30-minute auto-save and JSON export |
| **Admin Commands** | Full admin suite: reload, set alignment, reset player, export |

---

## Requirements

| Dependency | Version |
|---|---|
| Java | 21+ (25 recommended once available) |
| Hytale Server | Early Access (January 2026+) |
| Maven | 3.8+ (for building) |

---

## Installation

### Build from source

```bash
# Clone the repository
git clone https://github.com/your-org/hytale-fablescript.git
cd hytale-fablescript

# Build the shaded JAR (bundles SQLite, SnakeYAML, Gson)
mvn clean package -DskipTests

# Output: target/fablescript-1.0.0.jar
```

### Deploy to server

```bash
cp target/fablescript-1.0.0.jar /path/to/hytale-server/plugins/
# Restart or reload the Hytale server
```

On first startup, FableScript creates:
```
plugins/FableScript/
├── config.yml          ← main configuration
├── npcs.yml            ← NPC definitions
├── fablescript.db      ← SQLite database (auto-created)
├── dialogues/          ← place dialogue YAML files here
├── logs/               ← rotating log files (10 MB max)
└── exports/            ← admin data exports
```

---

## Configuration Guide

### config.yml

```yaml
fable:
  alignment:
    decay-per-day: 0.1          # How fast alignment drifts to 0 while inactive
    max-change-per-action: 25   # Anti-spam cap per choice
    good-ability-threshold: 20  # Min alignment for GOOD abilities
    evil-ability-threshold: -20 # Max alignment for EVIL abilities

  consequences:
    environmental-enabled: true
    corruption-spread-radius: 50
    corruption-spread-chance: 0.3
    corruption-persistence-hours: 24

  npcs:
    dialogue-variation-enabled: true
    relationship-memory-enabled: true
    dynamic-behavior-enabled: true
    reaction-radius: 100

  quests:
    multiple-solutions-enabled: true
    alignment-based-quest-availability: true

  storage:
    type: sqlite         # "sqlite" or "json"
    backup-interval-minutes: 30
```

Use `/fable reload` to apply config changes without restarting.

---

## NPC Definition Template

Add NPCs to `npcs.yml`. Each NPC needs a dialogue tree YAML file in `dialogues/`.

```yaml
npcs:
  my_npc_id:
    display-name: "NPC Display Name"
    personality: Noble       # Noble | Greedy | Vengeful | Idealistic | Pragmatic
    starting-relationship: 0 # -100 (hostile) to +100 (devoted)
    dialogue-tree: my_npc.yml
    quests:
      - quest_id_one
    location: "zone_key_or_description"
    ai-behavior:
      type: wanderer          # static | wanderer | patrol
      wander-radius: 20
      react-to-alignment: true
      hostile-to-evil: false
```

**Personality effects on relationship drift:**
- `Noble` / `Idealistic` — drifts toward good players, away from evil
- `Greedy` — neutral on alignment, prioritises transactions
- `Vengeful` — strongly hostile to PURE_EVIL players
- `Pragmatic` — no alignment-based drift

---

## Dialogue Tree Creation

Create `plugins/FableScript/dialogues/my_npc.yml`:

```yaml
dialogue:
  root:
    text: "Greetings, traveller. What brings you here?"
    choices:
      - id: choice_friendly
        text: "I come in peace."
        alignment_delta: 1
        next_node: node_friendly

      - id: choice_hostile
        text: "Your gold, now."
        alignment_delta: -8
        next_node: node_hostile
        consequence: "BOUNTY_PLACED:reason=extortion"

  node_friendly:
    text: "Wonderful! I have a job for someone of your... character."
    choices:
      - id: accept
        text: "I'm listening."
        alignment_delta: 0
        consequence: "QUEST_STATE_CHANGE:quest_id=my_quest;state=UNLOCK"

  node_hostile:
    text: "Guards! HELP!"
    choices: []   # Leaf node — ends the conversation
```

**Consequence format:** `TYPE:key=value;key=value`

Valid types: `ENVIRONMENTAL_CORRUPTION`, `ENVIRONMENTAL_BLESSING`, `NPC_HOSTILITY_INCREASE`,
`NPC_FRIENDLINESS_INCREASE`, `QUEST_STATE_CHANGE`, `BOUNTY_PLACED`, `BOUNTY_REMOVED`

---

## Quest Creation Guide

Quests are registered in `FablePlugin.registerDefaultQuests()`. To add your own:

```java
questManager.registerQuest(Quest.builder("my_quest_id")
    .title("My Quest Title")
    .description("A detailed description shown in /quest info.")
    .giverNpc("my_npc_id")          // null if world-triggered
    .minAlignment(-50)               // alignment gate
    .prerequisites(List.of())        // required completed quest IDs
    .resettable(true)
    .choices(List.of(
        QuestChoice.builder("good_path")
            .description("The heroic approach.")
            .moralPath(QuestChoice.MoralPath.GOOD)
            .alignmentDelta(10)
            .rewards(List.of(
                QuestReward.scaledGold(100),
                QuestReward.experience(150),
                QuestReward.abilityUnlock("heal")
            ))
            .build(),
        QuestChoice.builder("evil_path")
            .description("The selfish approach.")
            .moralPath(QuestChoice.MoralPath.EVIL)
            .alignmentDelta(-15)
            .maxAlignment(-1)        // only available to evil players
            .rewards(List.of(QuestReward.gold(200)))
            .consequenceKey("BOUNTY_PLACED:reason=my_quest_evil")
            .build()
    ))
    .build());
```

---

## Player Commands

| Command | Description |
|---|---|
| `/morality` | Show current alignment progress bar |
| `/morality history` | Last 10 alignment changes with causes |
| `/morality effects` | Current NPC reaction + environmental summary |
| `/quest status` | List available quests |
| `/quest info <id>` | View quest description and available choices |
| `/quest choose <id> <choiceId>` | Make a quest choice |

---

## Admin Commands

| Command | Description |
|---|---|
| `/fable reload` | Reload config and NPC definition files |
| `/fable set-alignment <uuid> <value>` | Force a player's alignment value |
| `/fable reset-player <uuid>` | Clear all FableScript data for a player |
| `/fable world-state` | Show zone corruption/blessing scores |
| `/fable export <uuid>` | Export player data to JSON file |
| `/fable consequence-log <uuid>` | View pending delayed consequences |
| `/fable debug` | Toggle verbose debug logging |

---

## Alignment Tiers

| Tier | Range | Abilities | NPC Reaction | Environment |
|---|---|---|---|---|
| PURE_GOOD | +41 to +100 | Holy Shield, Heal, **Resurrection**, Divine Intervention, Blessing | Revered — discounts, special quests | Flowers bloom, clear skies |
| GOOD | +1 to +40 | Holy Shield, Heal, Divine Intervention, Blessing | Friendly — gifts, normal pricing | Improved crops, friendly wildlife |
| NEUTRAL | 0 | All neutral abilities | Professional — standard pricing | No change |
| EVIL | -1 to -40 | Life Drain, Curse, Summon Undead | Fearful — inflated prices | Crops wither, weather darkens |
| PURE_EVIL | -41 to -100 | All evil abilities + **Mind Control**, **Corruption Wave** | Hostile — bounty system active | Corruption spreads, darkness |

---

## Database Schema

```sql
-- Core player record
players (uuid PK, name, alignment, last_seen, first_seen, playtime_seconds)

-- Per-player per-NPC relationship values
npc_relationships (player_uuid, npc_id PK, relationship_value)

-- Ability unlock tracking
unlocked_abilities (player_uuid, ability_id PK)

-- Completed quest tracking
completed_quests (player_uuid, quest_id PK)

-- Per-quest-node choice log
quest_choices (player_uuid, choice_key PK, choice_id)
```

Switch from SQLite to JSON in `config.yml`:
```yaml
storage:
  type: json
```

---

## Hytale API Integration Notes

FableScript is built against the **Hytale Early Access API** (January 2026).
Areas marked `// TODO: Hytale API` require wiring once the relevant API methods are confirmed:

- **Player UUID** — `player.getUniqueId()` (assumed standard)
- **Command arguments** — `context.getArgs()` / `context.getPlayer()`
- **Visual effects** — particle, aura, glow API (not yet public)
- **Inventory grants** — item/gold API
- **NPC AI hooks** — flee, attack, patrol API
- **Zone detection** — resolving which zone a player is in

Follow the [HytaleModding documentation](https://hytalemodding.dev) and update the
TODO stubs as the API is published.

---

## Performance

| Operation | Target | Mechanism |
|---|---|---|
| Quest choice processing | < 5 ms | Synchronous, no I/O |
| Alignment change + effects | < 10 ms | Sync event dispatch |
| Profile load (login) | Async | Background I/O thread pool |
| Profile save (auto) | Async | Background save scheduler |
| Database saves | Non-blocking | Dirty-flag + 30-min schedule |
| Max CPU impact | < 1% at peak | Consequence scanner runs every 30s |

---

## Troubleshooting

**Plugin doesn't load:**
- Confirm `manifest.json` is present in the JAR root
- Check Java version is 21+
- Check Hytale server logs for missing dependency errors

**Gradle sync fails (if adapting to Gradle):**
- Ensure `maven.hytale.com/release` is accessible
- Run with `--refresh-dependencies`

**SQLite "table not found":**
- Delete `fablescript.db` and restart — schema recreates automatically

**Config changes not applying:**
- Run `/fable reload` (no restart needed)

**Debug mode:**
- Run `/fable debug` or set `debug: true` in `config.yml`
- Logs written to `plugins/FableScript/logs/fablescript-0.log`

---

## Project Structure

```
src/main/java/com/hytale/fablescript/
├── FablePlugin.java          # Main entry point, wires all subsystems
├── config/                   # Config loading and NPC definition parsing
├── core/                     # Morality alignment, choice tracking, consequences
├── npc/                      # NPC manager, dialogue trees, relationship system
├── world/                    # Zone state, environmental effects, dynamic weather
├── quests/                   # Quest definitions, multi-path completion, rewards
├── abilities/                # Good/Evil/Neutral ability sets and cooldown manager
├── storage/                  # SQLite/JSON persistence, player profiles
├── listeners/                # Hytale and custom event handlers
├── commands/                 # Player and admin command executors
├── events/                   # Custom FableScript event types and event bus
└── utils/                    # Logger, morality calculator, dialogue parser, math utils
```

---

## License

MIT — see LICENSE file. Attribution appreciated but not required.

## Contributing

Pull requests welcome. Please add tests for new morality calculation logic in `MoralityCalculator`.
