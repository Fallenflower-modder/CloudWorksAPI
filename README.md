# CloudWorks API

**统一配方解析接口** — 为 NeoForge 1.21.1 模组提供标准化的配方数据查询与解析能力。

> 版本：1.0.0-1.21.1 | 平台：NeoForge 1.21.1 | Java 21

---

## 目录

1. [快速开始](#快速开始)
2. [核心概念](#核心概念)
3. [API 参考](#api-参考)
4. [DSL 配方模板](#dsl-配方模板)
5. [全局设置与脚本](#全局设置与脚本)
6. [流体→物品转换](#流体物品转换)
7. [模板配置文件](#模板配置文件)
8. [模板更新机制](#模板更新机制)
9. [调试命令](#调试命令)
10. [模块架构](#模块架构)

---

## 快速开始

### 添加依赖

在你的 `build.gradle` 中添加：

```gradle
repositories {
    maven { url = 'https://your-maven-repo' }
}

dependencies {
    implementation 'com.cloudworks:CloudWorksAPI:1.0.0-1.21.1'
}
```

### 启用模块

在你的主模组类上添加 `@CloudworksRecipeParser` 注解：

```java
@Mod("your_mod_id")
@CloudworksRecipeParser  // 启用 RecipeParser 模块
public class YourMod {
    public YourMod(IEventBus modEventBus) {
        // ...
    }
}
```

### 首次调用

```java
// 查询产出橡木木板的配方
List<RecipeParseResult> results = RecipeParserAPI.parseProduceRecipe(
    ResourceLocation.parse("minecraft:oak_planks"),
    QueryMode.ITEM,
    recipeManager
);

for (RecipeParseResult r : results) {
    System.out.println(r.getRecipeId());           // 配方 ID
    System.out.println(r.getData().getInputs());   // 原料列表
    System.out.println(r.getData().getOutputs());  // 产物列表
}
```

---

## 核心概念

### 配方解析流程

```
配方 JSON  ──→  serializeRecipe()  ──→  JsonElement
                                              │
                                              ▼
DSL 模板 (.rpml)  ──→  TemplateParser  ──→  TemplateNode (AST)
                                              │
                                              ▼
                              RecipeExtractor.extract()
                                              │
                                              ▼
                                       RecipeData
                                   (Ingredient[], Product[])
```

1. **序列化**：通过 Minecraft Codec 将配方序列化为 JSON
2. **模板解析**：将 `.rpml` DSL 模板解析为语法树（AST）
3. **数据提取**：根据 AST 从 JSON 中提取结构化数据

### 核心类

| 类 | 职责 |
|---|---|
| `RecipeParser` | 单例核心，管理模板加载、配方解析、流体转换 |
| `RecipeParserAPI` | 静态外观类，对外暴露全部 API |
| `RecipeData` | 解析结果，包含 `inputs` 和 `outputs` |
| `Ingredient` | 原料：id、count、unit、type |
| `Product` | 产物：id、count、unit、type、rate（概率） |
| `Template` | DSL 模板模型，包含 AST 根节点和 `GlobalSettings` |

---

## API 参考

全部 API 通过 `RecipeParserAPI` 静态方法调用。

### 基础查询

| 方法 | 说明 |
|---|---|
| `getRecipeData(ResourceLocation, RecipeManager)` | 解析单个配方 |
| `getRecipeDataBatch(Collection, RecipeManager)` | 批量解析配方 |
| `isRecipeParsable(ResourceLocation, RecipeManager)` | 检查配方是否可解析 |
| `getParsableRecipes(String modId, String recipeType, RecipeManager)` | 获取指定类型下所有可解析配方 |

### 高级查询

#### `parseProduceRecipe`

```java
List<RecipeParseResult> parseProduceRecipe(
    ResourceLocation targetId,  // 目标物品/流体 ID
    QueryMode mode,             // ITEM 或 FLUID
    RecipeManager recipeManager
)
```

| mode | 匹配逻辑 |
|---|---|
| `ITEM` | 匹配 `getResultItem()` 直接产出 + 流体→物品转化匹配 |
| `FLUID` | 匹配流体产物 + 转化反向匹配（同 ID 物品） |

#### `parseUsageRecipe`

```java
List<RecipeParseResult> parseUsageRecipe(
    ResourceLocation targetId,  // 目标物品/流体 ID
    QueryMode mode,             // ITEM 或 FLUID
    RecipeManager recipeManager
)
```

| mode | 匹配逻辑 |
|---|---|
| `ITEM` | 匹配原料中 `unit=item` 且 ID 相等 |
| `FLUID` | 匹配原料中 `unit=fluid` 且 ID 相等 |

### 数据模型

#### Ingredient

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `String` | 物品/流体/标签 ID |
| `count` | `double` | 数量 |
| `unit` | `String` | 单位：`"item"` / `"fluid"` |
| `type` | `String` | 类型：`"solid"` / `"tag"` / `"fluid"` |

#### Product

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `String` | 产物 ID |
| `count` | `double` | 数量 |
| `unit` | `String` | 单位：`"item"` / `"fluid"` |
| `type` | `String` | 类型：`"solid"` / `"tag"` / `"fluid"` |
| `rate` | `double` | 产出概率（0.0 ~ 1.0，默认 1.0） |

---

## DSL 配方模板

配方模板（`.rpml` 文件）位于 `cloudworks/recipe_parser/templates/`，命名格式为 `{modid}_{recipetype}.rpml`。

### 标记类型

| 标记 | 语法 | 说明 |
|---|---|---|
| `input` | `<input,id=foo,count=1,unit=item,type=solid>` | 声明输入原料 |
| `output` | `<output,id=bar,count=1,unit=item,type=solid>` | 声明产物 |
| `object` | `<object,id=myObj>` | 声明 JSON 对象节点 |
| `array` | `<array,id=myArr>` | 声明 JSON 数组节点 |
| `key` | `<key,id=myKey>` | 动态 JSON key 遍历 |
| `io_attribute` | `<count,output_id=bar>, <amount,input_id=foo>` | 向标记注入属性 |
| `symbol` | `<symbol,id=sym,input_id=keyIng>` | 声明有序合成符号 |
| `patternline` | `<patternline,id=line>` | 声明有序合成图案行 |
| `script` | `<script,set_global_fluid_transfer=true>` | 全局设置脚本 |

### 模板示例

#### 原版工作台（有序合成）

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

#### 原版工作台（无序合成）

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

## 全局设置与脚本

在模板中使用 `<script>` 标记配置全局行为：

```json
<script,
  set_global_fluid_transfer=true,
  set_global_default_transfer_rate=250,
  set_global_default_transfer_float_round=default,
  set_global_enable_template_config=true
>
```

### 可配置项

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `global_fluid_transfer` | `boolean` | `false` | 是否启用流体→物品转换 |
| `global_default_transfer_rate` | `double` | `100` | 默认多少 mB 转换为 1 个物品 |
| `global_default_transfer_result` | `string` | `null` | 默认转换结果物品 ID（null = 同流体 ID） |
| `global_default_transfer_extra_input` | `map` | `{}` | 额外原料（`key1=1.0,key2=2.0`） |
| `global_default_transfer_float_round` | `enum` | `default` | `round_up` / `round_down` / `default`（四舍五入） |
| `global_enable_template_config` | `boolean` | `true` | 是否启用外部模板配置文件 |

---

## 流体→物品转换

当配方产出流体时，可以将其转换为等价的物品，使得通过物品 ID 也能查询到该配方。

### 转换逻辑

```
流体总量 (mB) ÷ rate → 取整（按 round 策略）→ × 每单位产物数量
```

### 示例

配方产出 `minecraft:water × 1000 mB`，配置 `rate=250`，`round=default`：

```
1000 ÷ 250 = 4.0 → 四舍五入 → 4 个物品
```

### 在 Google 查询中生效

- `parseProduceRecipe(ITEM, "minecraft:water_bucket")` — 会匹配产出 `water` 流体的配方
- `parseProduceRecipe(FLUID, "minecraft:water")` — 直接匹配流体产物

---

## 模板配置文件

模板配置文件位于 `cloudworks/recipe_parser/templates_config/`，命名格式为 `{modid}_{recipetype}.json`。

### 格式

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

### 字段说明

| 字段 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `enable_transfer` | `boolean` | `true` | 该配方是否启用转化 |
| `transfer_blacklist` | `string[]` | `[]` | 不参与转化的流体产物 ID |
| `methods` | `object[]` | **必填** | 转化方式列表（缺失则报错并跳过） |
| `methods.rate` | `double` | `100` | 转化比例 |
| `methods.round` | `string` | `"default"` | 取整策略 |
| `methods.extra_input` | `object` | `{}` | 额外原料（`"物品ID": 数量`） |
| `methods.result` | `object` | **必填** | 转化产物（`"物品ID": 数量`，缺失则报错并忽略该方式） |

---

## 模板更新机制

模组启动时自动检查 `cloudworks/recipe_parser/config.json`：

```json
{
  "version": "1.0.0-1.21.1",
  "enable_update": true,
  "force_update": false,
  "update_ignore": ["minecraft_crafting.rpml"]
}
```

### 决策流程

```
config.json 不存在？
  → 创建默认配置，全部释放

force_update == true？
  → 跳过所有检查，全部释放

enable_update == false？
  → 跳过更新

version 匹配当前版本？
  → 跳过更新

否则 → 更新版本号，按 update_ignore 排除后释放
```

### update_ignore 逻辑

被标记忽略的文件仅在其**已存在**时跳过。若文件不存在（如首次安装），仍会正常释放。

---

## 调试命令

需要权限等级 4（OP）。

### 命令语法

```
/cloudworks recipe parse                  → 物品模式，手持物品
/cloudworks recipe parse <target>         → 物品模式，指定 ID
/cloudworks recipe parse item             → 物品模式，手持物品
/cloudworks recipe parse item <target>    → 物品模式，指定 ID
/cloudworks recipe parse liquid           → 流体模式，手持物品 ID
/cloudworks recipe parse liquid <target>  → 流体模式，指定 ID
```

### 输出示例

```
找到 5 个产出 create:andesite_alloy 的配方：
  配方：create:crafting/materials/andesite_alloy
  原料：
    - c:nuggets/iron ×2 item [tag]
    - minecraft:andesite ×2 item [solid]
  产物：
    - create:andesite_alloy ×1 item [solid]
```

流体产物显示为 `×250 mB`，概率性产物显示为 `(75%)`。

---

## 模块架构

```
src/main/java/com/cloudworks/api/
├── CloudWorksAPI.java              # NeoForge 模组入口
├── annotation/
│   └── CloudworksRecipeParser.java # 启用注解
├── command/
│   └── DebugCommand.java           # 调试命令
└── recipeparser/
    ├── RecipeParser.java            # 核心单例（模板加载、解析、转换）
    ├── RecipeParserAPI.java         # 静态 API 外观
    ├── ApiSelfTest.java             # 自动测试（已禁用）
    ├── DebugOutputWriter.java       # 调试 JSON 导出
    ├── dsl/                         # DSL 模板引擎
    │   ├── Template.java            # 模板模型
    │   ├── TemplateNode.java        # AST 节点
    │   ├── TemplateParser.java      # 语法解析器
    │   ├── TemplateTokenizer.java   # 词法分析器
    │   ├── TemplateValidator.java   # 模板验证器
    │   ├── RecipeExtractor.java     # 数据提取器
    │   ├── Token.java / TokenType.java
    │   ├── MarkerDef.java
    │   ├── ParameterOp.java
    │   ├── GlobalSettings.java      # 全局设置
    │   ├── TemplateConfig.java      # 配方级配置
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

## 许可证

本项目采用 GNU General Public License v3.0 开源协议。详见 [LICENSE](LICENSE) 文件。

CloudWorks API — 统一配方解析接口
Copyright (C) 2026 CloudWorks Team

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.