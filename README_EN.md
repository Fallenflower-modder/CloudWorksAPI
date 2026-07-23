# CloudWorks API

**Unified Recipe Parsing Interface** -- Provides standardized recipe data query and parsing capabilities for NeoForge 1.21.1 mods.

> Version: 1.0.0-1.21.1 | Platform: NeoForge 1.21.1 | Java 21

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Core Concepts](#core-concepts)
3. [API Reference](#api-reference)
4. [DSL Recipe Templates](#dsl-recipe-templates)
5. [Global Settings & Script](#global-settings--script)
6. [Fluid-to-Item Transfer](#fluid-to-item-transfer)
7. [Template Config Files](#template-config-files)
8. [Template Update Mechanism](#template-update-mechanism)
9. [Debug Commands](#debug-commands)
10. [Module Architecture](#module-architecture)

---

## Quick Start

### Adding the Dependency

Add the following to your `build.gradle`:

```gradle
repositories {
    maven { url = 'https://your-maven-repo' }
}

dependencies {
    implementation 'com.cloudworks:CloudWorksAPI:1.0.0-1.21.1'
}
```

### Enabling the Module

Add the `@CloudworksRecipeParser` annotation to your main mod class:

```java
@Mod("your_mod_id")
@CloudworksRecipeParser  // Enables the RecipeParser module
public class YourMod {
    public YourMod(IEventBus modEventBus) {
        // ...
    }
}
```

### First Call

```java
// Query recipes that produce oak planks
List<RecipeParseResult> results = RecipeParserAPI.parseProduceRecipe(
    ResourceLocation.parse("minecraft:oak_planks"),
    QueryMode.ITEM,
    recipeManager
);

for (RecipeParseResult r : results) {
    System.out.println(r.getRecipeId());           // Recipe ID
    System.out.println(r.getData().getInputs());   // Input list
    System.out.println(r.getData().getOutputs());  // Output list
}
```

---

## Core Concepts

### Recipe Parsing Flow

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
| `Template` | DSL template model, contains AST root node and `GlobalSettings` |

---

## API Reference

All APIs are called via `RecipeParserAPI` static methods.

### Basic Queries

| Method | Description |
|---|---|
| `getRecipeData(ResourceLocation, RecipeManager)` | Parse a single recipe |
| `getRecipeDataBatch(Collection, RecipeManager)` | Batch parse recipes |
| `isRecipeParsable(ResourceLocation, RecipeManager)` | Check if a recipe is parsable |
| `getParsableRecipes(String modId, String recipeType, RecipeManager)` | Get all parsable recipes of a given type |

### Advanced Queries

#### `parseProduceRecipe`

```java
List<RecipeParseResult> parseProduceRecipe(
    ResourceLocation targetId,  // Target item/fluid ID
    QueryMode mode,             // ITEM or FLUID
    RecipeManager recipeManager
)
```

| mode | Match Logic |
|---|---|
| `ITEM` | Matches `getResultItem()` direct output + fluid-to-item transfer matches |
| `FLUID` | Matches fluid outputs + reverse transfer matches (same-ID items) |

#### `parseUsageRecipe`

```java
List<RecipeParseResult> parseUsageRecipe(
    ResourceLocation targetId,  // Target item/fluid ID
    QueryMode mode,             // ITEM or FLUID
    RecipeManager recipeManager
)
```

| mode | Match Logic |
|---|---|
| `ITEM` | Matches ingredients where `unit=item` and ID matches |
| `FLUID` | Matches ingredients where `unit=fluid` and ID matches |

### Data Models

#### Ingredient

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Item/fluid/tag ID |
| `count` | `double` | Quantity |
| `unit` | `String` | Unit: `"item"` / `"fluid"` |
| `type` | `String` | Type: `"solid"` / `"tag"` / `"fluid"` |

#### Product

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Product ID |
| `count` | `double` | Quantity |
| `unit` | `String` | Unit: `"item"` / `"fluid"` |
| `type` | `String` | Type: `"solid"` / `"tag"` / `"fluid"` |
| `rate` | `double` | Output probability (0.0 ~ 1.0, default 1.0) |

---

## DSL Recipe Templates

Recipe templates (`.rpml` files) are located in `cloudworks/recipe_parser/templates/`, named in the format `{modid}_{recipetype}.rpml`.

### Marker Types

| Marker | Syntax | Description |
|---|---|---|
| `input` | `<input,id=foo,count=1,unit=item,type=solid>` | Declares an input ingredient |
| `output` | `<output,id=bar,count=1,unit=item,type=solid>` | Declares an output product |
| `object` | `<object,id=myObj>` | Declares a JSON object node |
| `array` | `<array,id=myArr>` | Declares a JSON array node |
| `key` | `<key,id=myKey>` | Dynamic JSON key traversal |
| `io_attribute` | `<count,output_id=bar>, <amount,input_id=foo>` | Injects attributes into markers |
| `symbol` | `<symbol,id=sym,input_id=keyIng>` | Declares a shaped crafting symbol |
| `patternline` | `<patternline,id=line>` | Declares a shaped crafting pattern row |
| `script` | `<script,set_global_fluid_transfer=true>` | Global settings script |

### Template Examples

#### Vanilla Crafting Table (Shaped)

```json
{
  "type": "minecraft:crafting_shaped",
  <script,set_global_fluid_transfer=true,set_global_default_transfer_rate=250>
  "pattern": <array,id=pattern>
    <patternline,id=line>
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

#### Vanilla Crafting Table (Shapeless)

```json
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": <array,id=ingredients>
    <object,id=ingStruct>
      "item": <input,id=ingItem,count=1,unit=item,type=solid>
      "tag": <input,id=ingItem,count=1,unit=item,type=tag>
    <count,id=ingCount,input_id=ingItem>
    <duplicate,id=ingStruct>
  "result": <object,id=res>
    "count": <count,output_id=res>
    "id": <type,output_id=res>
    "item": <output,id=res,count=1,unit=item,type=solid>
```

---

## Global Settings & Script

Use the `<script>` marker in templates to configure global behavior:

```json
<script,
  set_global_fluid_transfer=true,
  set_global_default_transfer_rate=250,
  set_global_default_transfer_float_round=default,
  set_global_enable_template_config=true
>
```

### Configurable Items

| Parameter | Type | Default | Description |
|---|---|---|---|
| `global_fluid_transfer` | `boolean` | `false` | Whether to enable fluid-to-item transfer |
| `global_default_transfer_rate` | `double` | `100` | How many mB convert to 1 item |
| `global_default_transfer_result` | `string` | `null` | Default transfer result item ID (null = same as fluid ID) |
| `global_default_transfer_extra_input` | `map` | `{}` | Extra ingredients (`key1=1.0,key2=2.0`) |
| `global_default_transfer_float_round` | `enum` | `default` | `round_up` / `round_down` / `default` (half-up) |
| `global_enable_template_config` | `boolean` | `true` | Whether to enable external template config files |

---

## Fluid-to-Item Transfer

When a recipe outputs fluid, it can be converted to an equivalent item, allowing the recipe to be found via item ID as well.

### Transfer Logic

```
Total fluid (mB) / rate -> rounded (per round strategy) * per-unit product quantity
```

### Example

Recipe outputs `minecraft:water * 1000 mB`, with config `rate=250`, `round=default`:

```
1000 / 250 = 4.0 -> half-up -> 4 items
```

### Effect on Queries

- `parseProduceRecipe(ITEM, "minecraft:water_bucket")` -- matches recipes that output `water` fluid
- `parseProduceRecipe(FLUID, "minecraft:water")` -- directly matches fluid outputs

---

## Template Config Files

Template config files are located in `cloudworks/recipe_parser/templates_config/`, named in the format `{modid}_{recipetype}.json`.

### Format

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

### Field Descriptions

| Field | Type | Default | Description |
|---|---|---|---|
| `enable_transfer` | `boolean` | `true` | Whether transfer is enabled for this recipe |
| `transfer_blacklist` | `string[]` | `[]` | Fluid product IDs excluded from transfer |
| `methods` | `object[]` | **Required** | List of transfer methods (missing = error and skip) |
| `methods.rate` | `double` | `100` | Transfer ratio |
| `methods.round` | `string` | `"default"` | Rounding strategy |
| `methods.extra_input` | `object` | `{}` | Extra ingredients (`"itemID": quantity`) |
| `methods.result` | `object` | **Required** | Transfer products (`"itemID": quantity`, missing = error and skip) |

---

## Template Update Mechanism

At mod startup, `cloudworks/recipe_parser/config.json` is automatically checked:

```json
{
  "version": "1.0.0-1.21.1",
  "enable_update": true,
  "force_update": false,
  "update_ignore": ["minecraft_crafting.rpml"]
}
```

### Decision Flow

```
config.json does not exist?
  -> Create default config, release all files

force_update == true?
  -> Skip all checks, release all files

enable_update == false?
  -> Skip update

version matches current version?
  -> Skip update

Otherwise -> Update version, release files excluding those in update_ignore
```

### update_ignore Logic

Files marked as ignored are only skipped if they **already exist**. If the file does not exist (e.g., first-time installation), it will still be extracted normally.

---

## Debug Commands

Requires permission level 4 (OP).

### Command Syntax

```
/cloudworks recipe parse                  -> Item mode, held item
/cloudworks recipe parse <target>         -> Item mode, specified ID
/cloudworks recipe parse item             -> Item mode, held item
/cloudworks recipe parse item <target>    -> Item mode, specified ID
/cloudworks recipe parse liquid           -> Fluid mode, held item ID
/cloudworks recipe parse liquid <target>  -> Fluid mode, specified ID
```

### Output Example

```
Found 5 recipes producing create:andesite_alloy:
  Recipe: create:crafting/materials/andesite_alloy
  Inputs:
    - c:nuggets/iron *2 item [tag]
    - minecraft:andesite *2 item [solid]
  Outputs:
    - create:andesite_alloy *1 item [solid]
```

Fluid outputs are displayed as `*250 mB`, and probabilistic outputs are displayed as `(75%)`.

---

## Module Architecture

```
src/main/java/com/cloudworks/api/
├── CloudWorksAPI.java              # NeoForge mod entry point
├── annotation/
│   └── CloudworksRecipeParser.java # Enable annotation
├── command/
│   └── DebugCommand.java           # Debug commands
└── recipeparser/
    ├── RecipeParser.java            # Core singleton (template loading, parsing, transfer)
    ├── RecipeParserAPI.java         # Static API facade
    ├── ApiSelfTest.java             # Auto-test (disabled)
    ├── DebugOutputWriter.java       # Debug JSON export
    ├── dsl/                         # DSL template engine
    │   ├── Template.java            # Template model
    │   ├── TemplateNode.java        # AST node
    │   ├── TemplateParser.java      # Syntax parser
    │   ├── TemplateTokenizer.java   # Tokenizer
    │   ├── TemplateValidator.java   # Template validator
    │   ├── RecipeExtractor.java     # Data extractor
    │   ├── Token.java / TokenType.java
    │   ├── MarkerDef.java
    │   ├── ParameterOp.java
    │   ├── GlobalSettings.java      # Global settings
    │   ├── TemplateConfig.java      # Recipe-level config
    │   └── TemplateConfigManager.java
    ├── exception/
    │   ├── RecipeNotFoundException.java
    │   └── RecipeParseException.java
    └── model/
        ├── RecipeData.java
        ├── Ingredient.java
        ├── Product.java
        ├── QueryMode.java
        └── RecipeParseResult.java
```

---

## License

This project is open-sourced under the GNU General Public License v3.0. See [LICENSE](LICENSE) for the full text.

CloudWorks API — Unified Recipe Parsing Interface
Copyright (C) 2026 CloudWorks Team

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.