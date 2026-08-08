# CloudWorks API

**Unified Mod Development Interface** — Provides standardized capabilities for NeoForge 1.21.1 mods, including a durable block system, recipe parsing, and console log capture.

> Version: 1.1.0-1.21.1 | Platform: NeoForge 1.21.1 | Java 21

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [DurableBlock — Durable Block System](#durableblock--durable-block-system)
3. [RecipeParser — Recipe Parsing](#recipeparser--recipe-parsing)
4. [ConsoleSeeker — Console Log Capture](#consoleseeker--console-log-capture)
5. [Debug Commands](#debug-commands)
6. [Module Architecture](#module-architecture)
7. [License](#license)

---

## Quick Start

### Adding the Dependency

Add the following to your `build.gradle`:

```gradle
repositories {
    maven { url = 'https://your-maven-repo' }
}

dependencies {
    implementation 'com.cloudworks:CloudWorksAPI:1.1.0-1.21.1'
}
```

### Optional Dependency: Jade Mod Integration

If you need Jade durability display for durable blocks, add the Jade API dependency:

```gradle
repositories {
    maven { url = "https://api.modrinth.com/maven" }
}

dependencies {
    compileOnly "maven.modrinth:jade:15.10.5+neoforge"
}
```

### Enabling Modules

CloudWorks API uses annotations to activate modules on demand. Add the corresponding annotation to your main mod class:

```java
@Mod("your_mod_id")
@CloudworksRecipeParser       // Enables the RecipeParser module
@CloudWorksConsoleSeeker      // Enables ConsoleSeeker + API
public class YourMod {
    public YourMod(IEventBus modEventBus) {
        // ...
    }
}
```

- **With annotation**: Forces the module to be enabled, ignoring the config file switch
- **Without annotation**: Controlled by the config file
- Each module can be independently enabled/disabled with no dependencies on each other

---

## DurableBlock — Durable Block System

### Core Concept

DurableBlock provides a complete **durable block system** that allows blocks to have durability values, damage resistance, and automatic recovery.

Unlike traditional solutions, this system **does not rely on BlockEntity**. Instead, it uses a `LivingEntity` subclass (`DurableBlockEntity`) to store durability data and receive damage. This design combines the data persistence of BlockEntity with the attackability of LivingEntity.

### Architecture

```
DurableBlock (Block)
    │ Spawns entity on placement
    │ Properties: maxDurability, baseResistance, type-specific resistances
    ▼
DurableBlockEntity (LivingEntity)
    │ Server-side only entity (clientTrackingRange=0)
    │ Bound to block coordinates (boundPos), validates block existence every tick
    │ Durability calculation, damage handling, NBT persistence
    │
    ├── Normal flow: durability reaches zero → breaks block → self-destructs
    └── PersistentDurableBlockEntity (subclass)
            └── Code game protection: intercepts instant-kill attacks → 20% durability loss
```

### Core Classes

| Class | Responsibility |
|---|---|
| `DurableBlock` | Durable block base class, defines durability attributes and resistance parameters, manages entity spawning |
| `DurableBlockEntity` | Block entity replacement (LivingEntity), handles damage, durability calculation, data persistence |
| `PersistentDurableBlock` | Persistent durable block, extends DurableBlock, uses PersistentDurableBlockEntity |
| `PersistentDurableBlockEntity` | Persistent entity, intercepts instant-kill attacks, converts to durability loss |
| `DurableBlockDamageType` | Damage type enum (EXPLOSION / PHYSICAL / MAGIC) |

### Usage

#### Basic Durable Block

```java
public class MyConcreteBlock extends DurableBlock {
    public MyConcreteBlock() {
        super(Properties.of()
                .strength(3.0f)
                .requiresCorrectToolForDrops(),
            200,    // maxBlockDurability: Maximum durability
            2,      // baseResistance: Flat damage reduction
            0.3f,   // explosionResistance: Explosion damage reduction (0.0 ~ 1.0)
            0.5f,   // physicalResistance: Physical damage reduction (0.0 ~ 1.0)
            0.1f,   // magicResistance: Magic damage reduction (0.0 ~ 1.0)
            0       // recoveryRate: Auto-recovery per second (0 = none)
        );
    }
}
```

#### Persistent Durable Block (with Code Game Protection)

```java
public class MyPersistentBlock extends PersistentDurableBlock {
    public MyPersistentBlock() {
        super(Properties.of().strength(3.0f),
            200, 2, 0.3f, 0.5f, 0.1f, 0);
    }
}
```

### Damage Calculation

```
actualDamage = max(0, rawDamage - baseResistance) × (1 - typeResistance)
```

1. **Base Resistance**: Flat reduction from raw damage (minimum 0)
2. **Type Resistance**: Applies the corresponding reduction ratio based on damage type (explosion/physical/magic)
3. **Damage Type Classification**:
   - **Explosion**: TNT, creepers, end crystals, bed/respawn anchor explosions
   - **Physical**: Melee attacks, arrows, tridents, projectiles
   - **Magic**: Potions, dragon breath, wither effect, void damage, fire/lightning

### Server-Side Only Entity

Durable block entities are registered with `clientTrackingRange(0)`, meaning the entity exists only on the server. The client has no knowledge of the entity's existence. This provides the following benefits:

- **Players cannot melee-attack the entity**: No target on the client side; the block can only be mined
- **No block interaction obstruction**: Does not prevent placing blocks on top or left-click mining
- **Mob and ranged attacks work normally**: Withers, zombies, and ranged attacks can still damage the entity on the server
- **No synchronization needed**: No client/server entity data synchronization issues

### Code Game Protection (Persistent)

`PersistentDurableBlockEntity` uses a multi-layer interception mechanism to resist instant-kill attacks:

1. **`remove()` Interception**: Checks `internalRemoval` flag; external calls trigger 20% durability loss instead
2. **`setHealth()` Interception**: Prevents external health zeroing
3. **`setPose()` Interception**: Prevents `Pose.DYING` state
4. **`hurt()` Detection**: Checks `instant_kill_weapon` tag, cancels instant-kill weapon damage events
5. **`dead` State Correction**: Checks and corrects the `dead` field every tick
6. **Invulnerability Frames**: 3 ticks of invulnerability after each hit, limiting hits to 5 per second

### Instant-Kill Weapon Tag

The `cloudworks_api:instant_kill_weapon` item tag marks instant-kill weapons. When an entity holding a tagged item attacks, `PersistentDurableBlockEntity` will cancel the damage event directly.

Default contents:

```json
{
  "values": [
    "avaritia:infinity_sword"
  ]
}
```

Modpack authors can add more weapons to this tag via datapack.

### Lifecycle Management

- **Block Placement**: `DurableBlock.setPlacedBy()` spawns the entity at the block position
- **Block Breaking**: The entity is no longer removed by block break events; it manages its own lifecycle
- **Tick Validation**: The entity checks every tick whether its bound block still exists; if not, self-destructs
- **Durability Depletion**: `onDurabilityZero()` breaks the block and removes the entity
- **NBT Persistence**: Durability data is saved to chunks via `addAdditionalSaveData` / `readAdditionalSaveData`

### Bound Position Mechanism

Each entity stores its bound block position in `boundPos`. When the entity is about to disappear for any reason, it destroys the block at that position. Simultaneously, it checks every tick whether the block at that position still exists and matches the expected type; if not, it removes itself. This design prevents the bug where an entity's collision box extending into adjacent block areas would incorrectly delete other entities.

### Jade Mod Integration

DurableBlock includes built-in [Jade](https://modrinth.com/mod/jade) integration (`DurableBlockJadePlugin`) that displays block durability information in Jade tooltips:

- Displays `Durability: current / max` on the line below the block name
- Current durability shows 2 decimal places
- Since the entity uses the server-side only approach, durability data is collected server-side via `IServerDataProvider` and synced to the client via Jade's data packets

### Collision Box Design

The entity collision box is 1.02×1.02 (slightly larger than a 1×1 block), centered at the block center (`y+0.5`), extending 0.01 blocks on all six sides. This allows mobs to attack the entity from any side while the entity does not obstruct block placement or movement.

---

## RecipeParser — Recipe Parsing

### Core Concepts

Recipe parsing flow:

```
Recipe JSON  -->  serializeRecipe()  -->  JsonElement
                                              |
                                              v
DSL Template (.rpml)  -->  TemplateParser  -->  TemplateNode (AST)
                                              |
                                              v
                              RecipeExtractor.extract()
                                              |
                                              v
                                       RecipeData
                                   (Ingredient[], Product[])
```

1. **Serialize**: Serialize the recipe to JSON via the Minecraft Codec
2. **Template Parse**: Parse the `.rpml` DSL template into an AST
3. **Data Extract**: Extract structured data from JSON according to the AST

### Core Classes

| Class | Responsibility |
|---|---|
| `RecipeParser` | Singleton core, manages template loading, recipe parsing, and fluid transfer |
| `RecipeParserAPI` | Static facade class, exposes all APIs externally |
| `RecipeData` | Parse result, contains `inputs` and `outputs` |
| `Ingredient` | Input material: id, count, unit, type |
| `Product` | Output product: id, count, unit, type, rate (probability) |

### API Reference

All APIs are called via `RecipeParserAPI` static methods.

#### Basic Queries

| Method | Description |
|---|---|
| `getRecipeData(ResourceLocation, RecipeManager)` | Parse a single recipe |
| `getRecipeDataBatch(Collection, RecipeManager)` | Batch parse recipes |
| `isRecipeParsable(ResourceLocation, RecipeManager)` | Check if a recipe is parsable |
| `getParsableRecipes(String modId, String recipeType, RecipeManager)` | Get all parsable recipes of a given type |

#### Advanced Queries

```java
// Find recipes that produce the target
List<RecipeParseResult> parseProduceRecipe(
    ResourceLocation targetId,  // Target item/fluid ID
    QueryMode mode,             // ITEM or FLUID
    RecipeManager recipeManager
)

// Find recipes that use the target as input
List<RecipeParseResult> parseUsageRecipe(
    ResourceLocation targetId,
    QueryMode mode,
    RecipeManager recipeManager
)
```

| mode | Match Logic (produce) | Match Logic (usage) |
|---|---|---|
| `ITEM` | Matches direct output + fluid-to-item transfer | Matches ingredients where `unit=item` |
| `FLUID` | Matches fluid outputs + reverse transfer matches | Matches ingredients where `unit=fluid` |

#### Async API Methods

All async APIs are called via `RecipeParserAPI` static methods. Heavy recipe scanning and parsing work runs on dedicated worker threads (`AsyncRecipeParser` thread pool, 2 daemon threads), and results are delivered back to the server thread via `MinecraftServer` callback.

```java
RecipeParserAPI.parseProduceRecipeAsync(
    ResourceLocation.parse("minecraft:oak_planks"),
    QueryMode.ITEM,
    recipeManager,
    results -> {
        // This callback runs on the server thread -- safe to manipulate Minecraft objects
        for (RecipeParseResult r : results) {
            sendSuccess("Found recipe: " + r.getRecipeId());
        }
    },
    errorMsg -> sendFailure("Query failed: " + errorMsg),
    server
);
```

Async method signatures:

| Method | Description |
|---|---|
| `getRecipeDataAsync(id, mgr, cb, err, server)` | Asynchronously parse a single recipe |
| `getRecipeDataBatchAsync(ids, mgr, cb, err, server)` | Asynchronously batch parse recipes |
| `parseProduceRecipeAsync(id, mode, mgr, cb, err, server)` | Asynchronously find producing recipes |
| `parseUsageRecipeAsync(id, mode, mgr, cb, err, server)` | Asynchronously find usage recipes |

#### Data Models

**Ingredient**

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Item/fluid/tag ID |
| `count` | `double` | Quantity |
| `unit` | `String` | `"item"` / `"fluid"` |
| `type` | `String` | `"solid"` / `"tag"` / `"fluid"` |

**Product**

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Product ID |
| `count` | `double` | Quantity |
| `unit` | `String` | `"item"` / `"fluid"` |
| `type` | `String` | `"solid"` / `"tag"` / `"fluid"` |
| `rate` | `double` | Output probability (0.0 ~ 1.0, default 1.0) |

### DSL Recipe Templates

Recipe templates (`.rpml` files) are located in `cloudworks/recipe_parser/templates/`, named in the format `{modid}_{recipetype}.rpml`.

#### Marker Types

| Marker | Syntax | Description |
|---|---|---|
| `input` | `<input,id=foo,count=1,unit=item,type=solid>` | Declares an input ingredient |
| `output` | `<output,id=bar,count=1,unit=item,type=solid>` | Declares an output product |
| `object` | `<object,id=myObj>` | Declares a JSON object node |
| `key` | `<key,id=myKey>` | Dynamic JSON key traversal |
| `io_attribute` | `<count,output_id=bar>` | Injects attributes into markers |
| `symbol` | `<symbol,id=sym,input_id=keyIng>` | Declares a shaped crafting symbol |
| `patternline` | `<patternline,id=line>` | Declares a shaped crafting pattern row |
| `duplicate` | `<duplicate,id=dupX,structure=X>` | Declares a repeatable JSON object structure |
| `script` | `<script,set_global_fluid_transfer=true>` | Global settings script |
| `optional` | `<optional,id=opt>` | Optional field |
| `variable` | `<variable,id=var>` | Variable reference |

#### Template Examples

**Vanilla Crafting Table (Shaped)**

```json
{
  "type": "minecraft:crafting_shaped",
  <script,set_global_fluid_transfer=true,set_global_default_transfer_rate=250>
  "pattern": [
    <patternline,id=line>
  ],
  "key": <object,id=keyObj>
    <key,id=key>
      <object,id=keyIng>
        "item": <input,id=keyIng,count=1,unit=item,type=solid>
        "tag": <input,id=keyIng,count=1,unit=item,type=tag>
      <symbol,id=sym,input_id=keyIng>
    <count,id=keyIng_count,input_id=keyIng>
    <patternline,id=line,input_id=sym>
  "result": <object,id=res>
    "count": <count,output_id=res>
    "id": <type,output_id=res>
    "item": <output,id=res,count=1,unit=item,type=solid>
```

**Vanilla Crafting Table (Shapeless)**

```json
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    <object,id=ingStruct>
      "item": <input,id=ingItem,count=1,unit=item,type=solid>
      "tag": <input,id=ingItem,count=1,unit=item,type=tag>
    <count,id=ingCount,input_id=ingItem>
    <duplicate,id=ingStruct>
  ],
  "result": <object,id=res>
    "count": <count,output_id=res>
    "id": <type,output_id=res>
    "item": <output,id=res,count=1,unit=item,type=solid>
```

### Fluid-to-Item Transfer

When a recipe outputs fluid, it can be converted to an equivalent item, allowing the recipe to be found via item ID as well.

**Transfer Logic**: `Total fluid (mB) ÷ rate → rounded (per round strategy) × per-unit product quantity`

**Global Settings** (configured via `<script>` in templates):

| Parameter | Type | Default | Description |
|---|---|---|---|
| `global_fluid_transfer` | `boolean` | `false` | Whether to enable fluid-to-item transfer |
| `global_default_transfer_rate` | `double` | `100` | How many mB convert to 1 item |
| `global_default_transfer_result` | `string` | `null` | Default transfer result item ID |
| `global_default_transfer_extra_input` | `map` | `{}` | Extra ingredients |
| `global_default_transfer_float_round` | `enum` | `default` | `round_up` / `round_down` / `default` |
| `global_enable_template_config` | `boolean` | `true` | Whether to enable external template config files |

**Recipe-Level Config** (`cloudworks/recipe_parser/templates_config/{modid}_{recipetype}.json`):

```json
{
  "recipe:id": {
    "enable_transfer": true,
    "transfer_blacklist": ["fluid:to_skip"],
    "methods": [{
      "rate": 100,
      "round": "default",
      "extra_input": {"minecraft:item": 1},
      "result": {"minecraft:output_item": 2}
    }]
  }
}
```

### Template Update Mechanism

`cloudworks/recipe_parser/config.json`:

```json
{
  "version": "1.1.0-1.21.1",
  "enable_update": true,
  "force_update": false,
  "update_ignore": ["minecraft_crafting.rpml"]
}
```

- `force_update=true` → Skip all checks, release all files
- `enable_update=false` → Skip update
- `version` matches → Skip update
- Otherwise → Update version, release files excluding those in `update_ignore`

---

## ConsoleSeeker — Console Log Capture

ConsoleSeeker outputs console logs to the in-game chat in real-time and provides a complete event pipeline for downstream mods to filter and process logs.

### Enabling

```java
@CloudWorksConsoleSeeker  // Forces ConsoleSeeker + API to be enabled
```

Or configure in `cloudworks/console_seeker/config.json`:

```json
{
  "enable_module": true,
  "enable_api": false,
  "enable_command_for_any_operator": true,
  "list_type": "whitelist",
  "player_list": [],
  "max_log_length": 150,
  "enable_timestamp": false
}
```

| Field | Type | Default | Description |
|---|---|---|---|
| `enable_module` | `boolean` | `true` | Whether to enable the ConsoleSeeker module |
| `enable_api` | `boolean` | `false` | Whether to enable the event API (annotations override this) |
| `enable_command_for_any_operator` | `boolean` | `true` | Whether to allow all OPs to use commands |
| `list_type` | `string` | `"whitelist"` | `"whitelist"` / `"blacklist"` |
| `player_list` | `string[]` | `[]` | Player name list |
| `max_log_length` | `int` | `150` | Maximum log length (0 = no truncation) |
| `enable_timestamp` | `boolean` | `false` | Whether to show timestamps |

### Architecture

```
Log4j2 Logger
    │
    ▼
ChatAppender (Log4j2 Appender)
    │ Filters [CHAT] substring
    ▼
LogToChatManager
    │ Filters by log level and player subscription
    ▼
ConsoleSeekerEventManager
    │ Internal filter units → collects tagSet
    ▼
NeoForge EVENT_BUS
    │ Dispatches ConsoleSeekerInfoEvent / WarnEvent / ErrorEvent
    ▼
ExternalLogFilter (subscribed by downstream mods)
    │ Matches tagSet → parse() → onReceive()
    ▼
Downstream mod custom logic
```

### Event Pipeline

Three independent event pipelines for different log levels:

| Event Class | Log Level |
|---|---|
| `ConsoleSeekerInfoEvent` | INFO |
| `ConsoleSeekerWarnEvent` | WARN |
| `ConsoleSeekerErrorEvent` | ERROR |

All events inherit from `ConsoleSeekerLogEvent` with the following fields:

| Field | Type | Description |
|---|---|---|
| `loggerName` | `String` | Logger name |
| `level` | `Level` | Log level |
| `message` | `String` | Formatted message (ANSI color codes removed) |
| `timestamp` | `long` | Unix timestamp (milliseconds) |
| `threadName` | `String` | Thread name |
| `thrownString` | `String` | Exception stack trace (null if no exception) |
| `tagSet` | `Set<DirectedDeliveryTag>` | Directed delivery tag set |

### Internal Filters (Active Filtering)

Register internal filter units via `ConsoleSeekerEventManager` to have ConsoleSeeker actively filter logs:

```java
// Implement the InternalFilterUnit interface
InternalFilterUnit myUnit = new InternalFilterUnit() {
    public DirectedDeliveryTag getTag() {
        return new DirectedDeliveryTag("mymod", "cpu_alert");
    }
    public boolean test(LogEvent event) {
        return event.getLoggerName().startsWith("com.example");
    }
};

// Register to the internal filter unit list
ConsoleSeekerEventManager.addInternalFilterUnit(myUnit);
```

**Filter Rules**:
- Empty list → All log events are published directly, tagSet is empty
- Non-empty list → At least one unit must pass to publish; collects tags from passing units into tagSet
- All units fail → Event is discarded

### Directed Delivery Tags

Uses the `"modid:tagName"` format to prevent cross-mod conflicts:

```java
// Create a tag
DirectedDeliveryTag tag = new DirectedDeliveryTag("mymod", "cpu_alert");

// Matching rules: empty tag matches everything, non-empty tag requires exact match
tag.matches(otherTag);
```

### External Filters (Passive Filtering)

Downstream mods extend `ExternalLogFilter<T>` for custom log parsing:

```java
public class MyCpuFilter extends ExternalLogFilter<Integer> {
    public MyCpuFilter() {
        super(new DirectedDeliveryTag("mymod", "cpu_alert"));
    }

    @Override
    protected Integer parse(ConsoleSeekerLogEvent event) {
        // Custom parsing logic
        return Integer.parseInt(event.getMessage());
    }

    @Override
    protected void onReceive(Integer value) {
        // Handle the parsed result
        System.out.println("CPU usage: " + value);
    }
}

// Register to start receiving events
MyCpuFilter filter = new MyCpuFilter();
filter.register();
```

**Workflow**:
1. Match the event's `tagSet` against the filter's tag (intersection matches, empty tag set matches everything)
2. `parse()` converts the raw event to a custom type (returns the raw log string by default)
3. `onReceive()` processes the parsed result

---

## Debug Commands

Command prefix: `/cloudworks`. Requires OP level 4 (some commands).

### Recipe Module

```
/cloudworks recipe parse produce item [id]     — Query recipes producing the specified item
/cloudworks recipe parse produce liquid [id]   — Query recipes producing the specified fluid
/cloudworks recipe parse usage item [id]       — Query recipes using the specified item
/cloudworks recipe parse usage liquid [id]     — Query recipes using the specified fluid
/cloudworks recipe parsebatch <modid> <type>   — Batch parse
/cloudworks recipe listtemplates               — List loaded templates
```

### Console Module

```
/cloudworks console <info|warn|error> <on|off>             — Toggle log output to chat
/cloudworks config console player_list add <player_name>   — Add player to list
/cloudworks config console player_list remove <player_name> — Remove player from list
/cloudworks config console player_list query               — View player list
```

### Status Query

```
/cloudworks status   — View all module status
```

Output includes: RecipeParser template count, ConsoleSeeker enabled levels/API status/filter unit count.

---

## Module Architecture

```
src/main/java/com/cloudworks/api/
├── CloudWorksAPI.java                    # NeoForge mod entry point
├── annotation/
│   ├── CloudworksRecipeParser.java       # Enables RecipeParser
│   └── CloudWorksConsoleSeeker.java      # Enables ConsoleSeeker
├── command/
│   └── DebugCommand.java                 # Unified debug command entry
│
├── durableblock/                         # DurableBlock module
│   ├── DurableBlock.java                 # Durable block base class
│   ├── DurableBlockEntity.java           # Block entity replacement (LivingEntity)
│   ├── PersistentDurableBlock.java       # Persistent durable block
│   ├── PersistentDurableBlockEntity.java # Code game protection entity
│   ├── DurableBlockDamageType.java       # Damage type enum
│   └── jade/
│       └── DurableBlockJadePlugin.java   # Jade mod integration plugin
│
├── recipeparser/                         # RecipeParser module
│   ├── RecipeParser.java                 # Core singleton
│   ├── RecipeParserAPI.java              # Static API facade
│   ├── AsyncRecipeParser.java            # Async thread pool
│   ├── ApiSelfTest.java                  # Auto-test
│   ├── DebugOutputWriter.java            # Debug JSON export
│   ├── dsl/                              # DSL template engine
│   │   ├── Template.java, TemplateNode.java
│   │   ├── TemplateParser.java, TemplateTokenizer.java
│   │   ├── TemplateValidator.java, RecipeExtractor.java
│   │   ├── GlobalSettings.java, TemplateConfig.java
│   │   └── ...
│   └── model/                            # Data models
│       ├── RecipeData.java, Ingredient.java, Product.java
│       └── QueryMode.java, RecipeParseResult.java
│
└── consoleseeker/                        # ConsoleSeeker module
    ├── ChatAppender.java                 # Log4j2 Appender
    ├── LogToChatManager.java             # Log-to-chat manager
    ├── ConsoleSeekerConfig.java          # Config file management
    ├── ConsoleSeekerEventManager.java    # Event pipeline core
    ├── ConsoleSeekerCommand.java         # Command handler
    ├── InternalFilterUnit.java           # Internal filter unit interface
    ├── ExternalLogFilter.java            # External filter abstract class
    ├── DirectedDeliveryTag.java          # Directed delivery tag
    ├── LogFilter.java                    # Log utility (truncation/color removal)
    └── event/                            # Event classes
        ├── ConsoleSeekerLogEvent.java     # Base event class
        ├── ConsoleSeekerInfoEvent.java
        ├── ConsoleSeekerWarnEvent.java
        └── ConsoleSeekerErrorEvent.java
```

---

## License

This project is open-sourced under the GNU General Public License v3.0. See [LICENSE](LICENSE) for the full text.

CloudWorks API — Unified Mod Development Interface
Copyright (C) 2026 CloudWorks Team

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.