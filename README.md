# CloudWorks API

**Minecraft 模组开发统一接口** — 为 NeoForge 1.21.1 模组提供耐久方块系统、配方解析、控制台日志捕获等标准化能力。

> 版本：1.1.0-1.21.1 | 平台：NeoForge 1.21.1 | Java 21

---

## 目录

1. [快速开始](#快速开始)
2. [DurableBlock — 耐久方块系统](#durableblock--耐久方块系统)
3. [RecipeParser — 配方解析](#recipeparser--配方解析)
4. [ConsoleSeeker — 控制台日志捕获](#consoleseeker--控制台日志捕获)
5. [调试命令](#调试命令)
6. [模块架构](#模块架构)
7. [许可证](#许可证)

---

## 快速开始

### 添加依赖

在你的 `build.gradle` 中添加：

```gradle
repositories {
    maven { url = 'https://your-maven-repo' }
}

dependencies {
    implementation 'com.cloudworks:CloudWorksAPI:1.1.0-1.21.1'
}
```

### 可选依赖：Jade 模组适配

如果需要耐久方块的 Jade 耐久显示功能，额外添加 Jade API 依赖：

```gradle
repositories {
    maven { url = "https://api.modrinth.com/maven" }
}

dependencies {
    compileOnly "maven.modrinth:jade:15.10.5+neoforge"
}
```

### 启用模块

CloudWorks API 通过注解按需激活模块。在你的主模组类上添加对应注解：

```java
@Mod("your_mod_id")
@CloudworksRecipeParser       // 启用 RecipeParser 模块
@CloudWorksConsoleSeeker      // 启用 ConsoleSeeker 本体 + API
public class YourMod {
    public YourMod(IEventBus modEventBus) {
        // ...
    }
}
```

- **注解存在时**：强制启用对应模块，配置文件中的开关被忽略
- **注解不存在时**：由配置文件决定是否启用
- 各模块可独立启用/禁用，互不依赖

---

## DurableBlock — 耐久方块系统

### 核心概念

DurableBlock 提供一套完整的**耐久方块系统**，允许方块拥有耐久值、伤害抗性和自动恢复能力。

区别于传统方案，该系统**不依赖 BlockEntity**，而是使用 `LivingEntity` 子类（`DurableBlockEntity`）来存储耐久数据和接收伤害。这一设计兼具 BlockEntity 的数据持久化能力和 LivingEntity 的可被攻击能力。

### 架构

```
DurableBlock (Block)
    │ 放置时生成实体
    │ 方块属性：maxDurability, baseResistance, 各类型抗性
    ▼
DurableBlockEntity (LivingEntity)
    │ 服务端仅存实体（clientTrackingRange=0）
    │ 绑定方块坐标（boundPos），每 tick 校验方块存在性
    │ 耐久计算、伤害处理、NBT 持久化
    │
    ├── 正常流程：耐久归零 → 破坏方块 → 自毁
    └── PersistentDurableBlockEntity（子类）
            └── 代码博弈保护：拦截即死攻击 → 20% 耐久扣减
```

### 核心类

| 类 | 职责 |
|---|---|
| `DurableBlock` | 耐久方块基类，定义耐久属性和抗性参数，负责实体生成 |
| `DurableBlockEntity` | 方块实体替代（LivingEntity），处理伤害、耐久计算、数据持久化 |
| `PersistentDurableBlock` | 持久耐久方块，继承 DurableBlock，使用 PersistentDurableBlockEntity |
| `PersistentDurableBlockEntity` | 持久实体，拦截即死攻击，转换为耐久扣减 |
| `DurableBlockDamageType` | 伤害类型枚举（EXPLOSION / PHYSICAL / MAGIC） |

### 使用方式

#### 基础耐久方块

```java
public class MyConcreteBlock extends DurableBlock {
    public MyConcreteBlock() {
        super(Properties.of()
                .strength(3.0f)
                .requiresCorrectToolForDrops(),
            200,    // maxBlockDurability: 最大耐久
            2,      // baseResistance: 基础抗性（等量减免）
            0.3f,   // explosionResistance: 爆炸伤害减免（0.0 ~ 1.0）
            0.5f,   // physicalResistance: 物理伤害减免（0.0 ~ 1.0）
            0.1f,   // magicResistance: 魔法伤害减免（0.0 ~ 1.0）
            0       // recoveryRate: 每秒自动恢复耐久（0 = 不恢复）
        );
    }
}
```

#### 带代码博弈保护的持久耐久方块

```java
public class MyPersistentBlock extends PersistentDurableBlock {
    public MyPersistentBlock() {
        super(Properties.of().strength(3.0f),
            200, 2, 0.3f, 0.5f, 0.1f, 0);
    }
}
```

### 伤害计算机制

```
实际伤害 = max(0, 原始伤害 - 基础抗性) × (1 - 类型减免)
```

1. **基础抗性**：从原始伤害中等量扣除（最低减到 0）
2. **类型减免**：根据伤害类型应用对应的减免比例（爆炸/物理/魔法）
3. **伤害类型分类**：
   - **爆炸伤害**：TNT、苦力怕、末影水晶、床/重生锚爆炸等
   - **物理伤害**：生物近战攻击、箭矢、三叉戟、弹射物等
   - **魔法伤害**：药水、龙息、凋零效果、虚空伤害、火焰/闪电等

### 服务端仅存实体方案

耐久方块实体采用 `clientTrackingRange(0)` 注册，实体仅在服务端运行，客户端完全不知道实体存在。这带来以下好处：

- **玩家无法近战攻击实体**：客户端无实体目标，只能通过挖掘破坏方块
- **不阻碍方块交互**：不阻碍玩家在方块上表面放置方块或长按左键挖掘
- **生物和远程攻击正常**：凋灵、僵尸等生物及箭矢等远程攻击可在服务端正常攻击实体
- **无需同步**：无需处理客户端/服务端实体数据同步问题

### 代码博弈保护（Persistent）

`PersistentDurableBlockEntity` 通过多层拦截机制抵抗即死类攻击：

1. **`remove()` 拦截**：检测 `internalRemoval` 标记，外部调用时转为 20% 耐久扣减
2. **`setHealth()` 拦截**：阻止外部直接设置血量归零
3. **`setPose()` 拦截**：阻止 `Pose.DYING` 状态
4. **`hurt()` 检测**：检查 `instant_kill_weapon` 标签，取消即死武器伤害事件
5. **`dead` 状态校正**：每 tick 检查并修正被即死攻击直接修改的 `dead` 字段
6. **无敌帧保护**：每次受击后 3 tick 无敌，限制每秒最多受击 5 次

### 即死武器标签

`cloudworks_api:instant_kill_weapon` 物品标签标记即死类武器，持有该标签物品的实体攻击时，`PersistentDurableBlockEntity` 将直接取消伤害事件。

默认包含：

```json
{
  "values": [
    "avaritia:infinity_sword"
  ]
}
```

模组包作者可通过 datapack 向此标签添加更多武器。

### 生命周期管理

- **方块放置**：`DurableBlock.setPlacedBy()` 在方块位置生成实体
- **方块破坏**：实体不再由方块破坏事件清除，而是由实体自身管理
- **Tick 校验**：实体每 tick 检查是否仍绑定有效方块，否则自毁
- **耐久归零**：`onDurabilityZero()` 破坏方块并移除实体
- **NBT 持久化**：耐久数据通过 `addAdditionalSaveData` / `readAdditionalSaveData` 存入区块

### 绑定坐标机制

每个实体在 `boundPos` 中存储其绑定的方块位置。实体在任意可能导致自身消失的流程中，会破坏该坐标的方块。同时每 tick 检查该坐标的方块是否仍存在且类型匹配，若不匹配则自动清除自身。这一设计避免了因实体碰撞箱延伸到相邻方块区域而误删其他实体的 Bug。

### Jade 模组适配

DurableBlock 内置 [Jade](https://modrinth.com/mod/jade) 模组适配插件（`DurableBlockJadePlugin`），在 Jade 工具提示中显示方块耐久信息：

- 在方块名下一行显示 `耐久：当前值 / 最大值`
- 当前耐久保留 2 位小数
- 由于实体采用服务端仅存方案，通过 `IServerDataProvider` 在服务端收集耐久数据，通过 Jade 数据包同步到客户端显示

### 碰撞箱设计

实体碰撞箱为 1.02×1.02（略大于 1×1 方块），中心位于方块中心（`y+0.5`），六面均露出 0.01 格。这使得生物可以从任意面攻击到实体，同时实体不会阻挡方块放置或移动。

---

## RecipeParser — 配方解析

### 核心概念

配方解析流程：

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

### API 参考

全部 API 通过 `RecipeParserAPI` 静态方法调用。

#### 基础查询

| 方法 | 说明 |
|---|---|
| `getRecipeData(ResourceLocation, RecipeManager)` | 解析单个配方 |
| `getRecipeDataBatch(Collection, RecipeManager)` | 批量解析配方 |
| `isRecipeParsable(ResourceLocation, RecipeManager)` | 检查配方是否可解析 |
| `getParsableRecipes(String modId, String recipeType, RecipeManager)` | 获取指定类型下所有可解析配方 |

#### 高级查询

```java
// 查询产出目标的配方
List<RecipeParseResult> parseProduceRecipe(
    ResourceLocation targetId,  // 目标物品/流体 ID
    QueryMode mode,             // ITEM 或 FLUID
    RecipeManager recipeManager
)

// 查询使用目标作为原料的配方
List<RecipeParseResult> parseUsageRecipe(
    ResourceLocation targetId,
    QueryMode mode,
    RecipeManager recipeManager
)
```

| mode | 匹配逻辑（produce） | 匹配逻辑（usage） |
|---|---|---|
| `ITEM` | 匹配直接产出 + 流体→物品转化 | 匹配 `unit=item` 的原料 |
| `FLUID` | 匹配流体产物 + 转化反向匹配 | 匹配 `unit=fluid` 的原料 |

#### 异步 API

所有查询支持异步版本，繁重工作运行在专用线程池（`AsyncRecipeParser`，2 个守护线程），结果通过 `server.execute()` 回调到服务器线程：

```java
RecipeParserAPI.parseProduceRecipeAsync(
    ResourceLocation.parse("minecraft:oak_planks"),
    QueryMode.ITEM,
    recipeManager,
    results -> {
        // 此回调在服务器线程执行，可安全操作 Minecraft 对象
        for (RecipeParseResult r : results) {
            sendSuccess("找到配方: " + r.getRecipeId());
        }
    },
    errorMsg -> sendFailure("查询失败: " + errorMsg),
    server
);
```

异步方法签名：

| 方法 | 说明 |
|---|---|
| `getRecipeDataAsync(id, mgr, cb, err, server)` | 异步解析单个配方 |
| `getRecipeDataBatchAsync(ids, mgr, cb, err, server)` | 异步批量解析 |
| `parseProduceRecipeAsync(id, mode, mgr, cb, err, server)` | 异步查找产出配方 |
| `parseUsageRecipeAsync(id, mode, mgr, cb, err, server)` | 异步查找使用配方 |

#### 数据模型

**Ingredient**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `String` | 物品/流体/标签 ID |
| `count` | `double` | 数量 |
| `unit` | `String` | `"item"` / `"fluid"` |
| `type` | `String` | `"solid"` / `"tag"` / `"fluid"` |

**Product**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `String` | 产物 ID |
| `count` | `double` | 数量 |
| `unit` | `String` | `"item"` / `"fluid"` |
| `type` | `String` | `"solid"` / `"tag"` / `"fluid"` |
| `rate` | `double` | 产出概率（0.0 ~ 1.0，默认 1.0） |

### DSL 配方模板

配方模板（`.rpml` 文件）位于 `cloudworks/recipe_parser/templates/`，命名格式为 `{modid}_{recipetype}.rpml`。

#### 标记类型

| 标记 | 语法 | 说明 |
|---|---|---|
| `input` | `<input,id=foo,count=1,unit=item,type=solid>` | 声明输入原料 |
| `output` | `<output,id=bar,count=1,unit=item,type=solid>` | 声明产物 |
| `object` | `<object,id=myObj>` | 声明 JSON 对象节点 |
| `key` | `<key,id=myKey>` | 动态 JSON key 遍历 |
| `io_attribute` | `<count,output_id=bar>` | 向标记注入属性（数量/概率等） |
| `symbol` | `<symbol,id=sym,input_id=keyIng>` | 声明有序合成符号 |
| `patternline` | `<patternline,id=line>` | 声明有序合成图案行 |
| `duplicate` | `<duplicate,id=dupX,structure=X>` | 声明可重复的 JSON 对象结构 |
| `script` | `<script,set_global_fluid_transfer=true>` | 全局设置脚本 |
| `optional` | `<optional,id=opt>` | 可选字段 |
| `variable` | `<variable,id=var>` | 变量引用 |

#### 模板示例

**原版工作台（有序合成）**

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

**原版工作台（无序合成）**

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

### 流体→物品转换

当配方产出流体时，可将其转换为等价的物品，使得通过物品 ID 也能查询到该配方。

**转换逻辑**：`流体总量 (mB) ÷ rate → 取整（按 round 策略）→ × 每单位产物数量`

**全局设置**（在模板中通过 `<script>` 配置）：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `global_fluid_transfer` | `boolean` | `false` | 是否启用转换 |
| `global_default_transfer_rate` | `double` | `100` | 多少 mB 转换为 1 个物品 |
| `global_default_transfer_result` | `string` | `null` | 转换结果物品 ID |
| `global_default_transfer_extra_input` | `map` | `{}` | 额外原料 |
| `global_default_transfer_float_round` | `enum` | `default` | `round_up` / `round_down` / `default` |
| `global_enable_template_config` | `boolean` | `true` | 是否启用外部模板配置文件 |

**配方级配置**（`cloudworks/recipe_parser/templates_config/{modid}_{recipetype}.json`）：

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

### 模板更新机制

`cloudworks/recipe_parser/config.json`：

```json
{
  "version": "1.1.0-1.21.1",
  "enable_update": true,
  "force_update": false,
  "update_ignore": ["minecraft_crafting.rpml"]
}
```

- `force_update=true` → 跳过检查，全部释放
- `enable_update=false` → 跳过更新
- `version` 匹配 → 跳过更新
- 否则 → 更新版本号，按 `update_ignore` 排除后释放

---

## ConsoleSeeker — 控制台日志捕获

ConsoleSeeker 将控制台日志实时输出到游戏聊天栏，并提供完整的事件管线供下游模组过滤和处理日志。

### 启用方式

```java
@CloudWorksConsoleSeeker  // 强制启用 ConsoleSeeker 本体 + API
```

或在 `cloudworks/console_seeker/config.json` 中配置：

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

| 字段 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `enable_module` | `boolean` | `true` | 是否启用 ConsoleSeeker 本体 |
| `enable_api` | `boolean` | `false` | 是否启用事件 API（注解可越过此项） |
| `enable_command_for_any_operator` | `boolean` | `true` | 是否对所有 OP 开放命令 |
| `list_type` | `string` | `"whitelist"` | `"whitelist"` / `"blacklist"` |
| `player_list` | `string[]` | `[]` | 玩家名称列表 |
| `max_log_length` | `int` | `150` | 日志最大长度（0 = 不截断） |
| `enable_timestamp` | `boolean` | `false` | 是否显示时间戳 |

### 架构

```
Log4j2 Logger
    │
    ▼
ChatAppender (Log4j2 Appender)
    │ 过滤 [CHAT] 子字符串
    ▼
LogToChatManager
    │ 按日志级别和玩家订阅过滤
    ▼
ConsoleSeekerEventManager
    │ 内部过滤单元 → 收集 tagSet
    ▼
NeoForge EVENT_BUS
    │ 发布 ConsoleSeekerInfoEvent / WarnEvent / ErrorEvent
    ▼
ExternalLogFilter (下游模组订阅)
    │ 匹配 tagSet → parse() → onReceive()
    ▼
下游模组自定义逻辑
```

### 事件管线

三个独立的事件管线，分别对应不同日志级别：

| 事件类 | 日志级别 |
|---|---|
| `ConsoleSeekerInfoEvent` | INFO |
| `ConsoleSeekerWarnEvent` | WARN |
| `ConsoleSeekerErrorEvent` | ERROR |

所有事件继承自 `ConsoleSeekerLogEvent`，包含以下字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `loggerName` | `String` | Logger 名称 |
| `level` | `Level` | 日志级别 |
| `message` | `String` | 格式化消息（已去除 ANSI 颜色码） |
| `timestamp` | `long` | Unix 时间戳（毫秒） |
| `threadName` | `String` | 线程名称 |
| `thrownString` | `String` | 异常堆栈（无异常时为 null） |
| `tagSet` | `Set<DirectedDeliveryTag>` | 定向投送标记集合 |

### 内部过滤器（主动过滤）

通过 `ConsoleSeekerEventManager` 注册内部过滤单元，让 ConsoleSeeker 主动过滤日志：

```java
// 实现 InternalFilterUnit 接口
InternalFilterUnit myUnit = new InternalFilterUnit() {
    public DirectedDeliveryTag getTag() {
        return new DirectedDeliveryTag("mymod", "cpu_alert");
    }
    public boolean test(LogEvent event) {
        return event.getLoggerName().startsWith("com.example");
    }
};

// 注册到内部过滤单元列表
ConsoleSeekerEventManager.addInternalFilterUnit(myUnit);
```

**过滤规则**：
- 列表为空 → 所有日志事件直接发布，tagSet 为空集
- 列表非空 → 至少一个单元通过才发布，收集通过单元的标记到 tagSet
- 所有单元未通过 → 丢弃事件

### 定向投送标记

采用 `"modid:tagName"` 格式，确保不同模组间不会串线：

```java
// 创建标记
DirectedDeliveryTag tag = new DirectedDeliveryTag("mymod", "cpu_alert");

// 匹配规则：空标记匹配所有，非空标记精确匹配
tag.matches(otherTag);
```

### 外部过滤器（被动过滤）

下游模组继承 `ExternalLogFilter<T>` 实现自定义日志解析：

```java
public class MyCpuFilter extends ExternalLogFilter<Integer> {
    public MyCpuFilter() {
        super(new DirectedDeliveryTag("mymod", "cpu_alert"));
    }

    @Override
    protected Integer parse(ConsoleSeekerLogEvent event) {
        // 自定义解析逻辑
        return Integer.parseInt(event.getMessage());
    }

    @Override
    protected void onReceive(Integer value) {
        // 处理解析结果
        System.out.println("CPU usage: " + value);
    }
}

// 注册以开始接收事件
MyCpuFilter filter = new MyCpuFilter();
filter.register();
```

**工作流程**：
1. 匹配事件的 `tagSet` 与本过滤器关心的标记（有交集即匹配，空标记集匹配所有）
2. `parse()` 将原始事件解析为自定义类型（默认返回原始日志字符串）
3. `onReceive()` 处理解析结果

---

## 调试命令

命令前缀：`/cloudworks`。需要 OP 权限等级 4（部分命令）。

### 配方模块

```
/cloudworks recipe parse produce item [id]     — 查询产出指定物品的配方
/cloudworks recipe parse produce liquid [id]   — 查询产出指定流体的配方
/cloudworks recipe parse usage item [id]       — 查询使用指定物品的配方
/cloudworks recipe parse usage liquid [id]     — 查询使用指定流体的配方
/cloudworks recipe parsebatch <modid> <type>   — 批量解析
/cloudworks recipe listtemplates               — 列出已加载模板
```

### 控制台模块

```
/cloudworks console <info|warn|error> <on|off>             — 控制日志输出到聊天栏
/cloudworks config console player_list add <player_name>   — 添加玩家到列表
/cloudworks config console player_list remove <player_name> — 从列表移除玩家
/cloudworks config console player_list query               — 查看玩家列表
```

### 状态查询

```
/cloudworks status   — 查看所有模块状态
```

输出包括：RecipeParser 模板数、ConsoleSeeker 启用级别/API 状态/过滤单元数。

---

## 模块架构

```
src/main/java/com/cloudworks/api/
├── CloudWorksAPI.java                    # NeoForge 模组入口
├── annotation/
│   ├── CloudworksRecipeParser.java       # 启用 RecipeParser
│   └── CloudWorksConsoleSeeker.java      # 启用 ConsoleSeeker
├── command/
│   └── DebugCommand.java                 # 统一调试命令入口
│
├── durableblock/                         # DurableBlock 模块
│   ├── DurableBlock.java                 # 耐久方块基类
│   ├── DurableBlockEntity.java           # 方块实体替代（LivingEntity）
│   ├── PersistentDurableBlock.java       # 持久耐久方块
│   ├── PersistentDurableBlockEntity.java # 代码博弈实体
│   ├── DurableBlockDamageType.java       # 伤害类型枚举
│   └── jade/
│       └── DurableBlockJadePlugin.java   # Jade 模组适配插件
│
├── recipeparser/                         # RecipeParser 模块
│   ├── RecipeParser.java                 # 核心单例
│   ├── RecipeParserAPI.java              # 静态 API 外观
│   ├── AsyncRecipeParser.java            # 异步线程池
│   ├── ApiSelfTest.java                  # 自动测试
│   ├── DebugOutputWriter.java            # 调试 JSON 导出
│   ├── dsl/                              # DSL 模板引擎
│   │   ├── Template.java, TemplateNode.java
│   │   ├── TemplateParser.java, TemplateTokenizer.java
│   │   ├── TemplateValidator.java, RecipeExtractor.java
│   │   ├── GlobalSettings.java, TemplateConfig.java
│   │   └── ...
│   └── model/                            # 数据模型
│       ├── RecipeData.java, Ingredient.java, Product.java
│       └── QueryMode.java, RecipeParseResult.java
│
└── consoleseeker/                        # ConsoleSeeker 模块
    ├── ChatAppender.java                 # Log4j2 Appender
    ├── LogToChatManager.java             # 日志→聊天栏管理
    ├── ConsoleSeekerConfig.java          # 配置文件管理
    ├── ConsoleSeekerEventManager.java    # 事件管线核心
    ├── ConsoleSeekerCommand.java         # 命令处理
    ├── InternalFilterUnit.java           # 内部过滤单元接口
    ├── ExternalLogFilter.java            # 外部过滤器抽象类
    ├── DirectedDeliveryTag.java          # 定向投送标记
    ├── LogFilter.java                    # 日志工具（截断/去色）
    └── event/                            # 事件类
        ├── ConsoleSeekerLogEvent.java     # 事件基类
        ├── ConsoleSeekerInfoEvent.java
        ├── ConsoleSeekerWarnEvent.java
        └── ConsoleSeekerErrorEvent.java
```

---

## 许可证

本项目采用 GNU General Public License v3.0 开源协议。详见 [LICENSE](LICENSE) 文件。

CloudWorks API — Minecraft 模组开发统一接口
Copyright (C) 2026 CloudWorks Team

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.