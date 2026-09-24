# 跨 Minecraft 版本迁移指南

本文档是「把 ZuoyanMod 迁移到下一个 Minecraft / NeoForge 版本」的操作清单。
重构后的代码按下面四层组织，**自上而下稳定性递减、迁移工作量递增**：

```
org.gwfx.zuoyanmod
├── core/       纯逻辑层：零 Minecraft / NeoForge 依赖，迁移时完全不用动
├── platform/   版本适配层：签名随版本变化的 MC API 集中在这里，迁移时优先改这里
├── content 层   item / block / effect / fluid / damage / sound / menu / world：
│               游戏内容本体，迁移时按编译报错逐个核对
└── 绑定层       client / network / event + Config / Zuoyanmod：
                与 MC 客户端、NeoForge 事件总线和注册表绑定最深，迁移时改动最多
```

## 1. 构建侧（改配置，不改代码）

全部版本号集中在 `gradle.properties`：

| 属性 | 说明 |
|---|---|
| `minecraft_version` / `minecraft_version_range` | 目标 MC 版本及 mods.toml 声明范围 |
| `neo_version` / `neo_version_range` | NeoForge 版本（必须与 MC 版本匹配） |
| `loader_version_range` | FML 版本范围 |
| `parchment_minecraft_version` / `parchment_mappings_version` | Parchment mappings |

另外 `build.gradle` 里有两个**随 MC 版本变化的 CurseMaven file ID**：
JEI（`curse.maven:jei-238222:...`）和 Curios（`curse.maven:curios-309927:...`），
迁移时要换成目标版本对应的 file ID。

## 2. platform/ 适配层（迁移时优先改这里）

| 类 | 封装内容 | 26.3 现状 |
|---|---|---|
| `Teleports` | 玩家跨维度传送 | `teleportTo(ServerLevel, double, double, double, Set<Relative>, float, float, boolean)`，相比 1.21 多了 `Set<Relative>` |
| `RegistryLookup` | 内置注册表按 ID 查询 | 返回 `Optional<Holder.Reference>`，要两次解包 |

版本迁移流程：先改这两个类 + 全量编译，大部分「签名漂移」会被编译器直接暴露。

## 3. core/ 纯逻辑层（迁移时不用动）

| 类 | 说明 |
|---|---|
| `AmountFormat` | 数量格式化（1.2k / 千分位） |
| `KleinTerminalLayout` | 终端 GUI 布局常量；**9×6 网格形状的单一事实来源**（`FourDimensionalSpace.COLUMNS/DEFAULT_ROWS` 反向引用这里） |

## 4. 已知版本敏感点（26.3 的行为差异，迁出/迁入时逐条核对）

以下位置是 26.3 相对旧版的**API 行为差异**，迁移时最可能出问题：

### 内容层
- `item/FourDimensionalSpace`：26.x 的 `ItemStack` 会校验 count 不超过物品堆叠上限，
  所以「总量存 long、模板 count 恒 1」的模型是刻意的，别为了迁版本改回塞 count。
- `item/KleinFurnace`：燃料判定 = `DataComponents.COOKING_FUEL`（`burnTime` 是
  `ResolvableInt`，注册表引用要走 `LootContext` 解析）；容器残留走
  `DataComponents.USE_REMAINDER`（26.3 已没有 `hasCraftingRemainingItem`）。
- `item/VacuumDecayItem`：挖掘行为由 `DataComponents.TOOL` 组件驱动；
  「不设 durability = 无限耐久」是 26.x 语义。
- `item/ItemRegistry`：紫金盔甲材质用 26.x 的 Record 形式定义。
- `item/KleinBottleItem`：26.3 的 `Item#inventoryTick` 只在服务端调用。
- `effect/*`：效果 tick 判定方法在 26.3 是新签名（接收 `ServerLevel` 返回 boolean，
  替代旧 `isDurationEffectTick`）；`ADD_MULTIPLIED_TOTAL` 等修饰符常量也在 26.x 改过名。
- `world/*`：实体重建/存档走 `TagValueOutput` + `EntitySpawnReason`（26.x 新 API），
  玩家传送已收口到 `platform/Teleports`。
- `event/VacuumDecayBlackHoleManager`：26.3 的 `Entity#push` 只同步位置，
  修改速度后要额外设 `syncVelocity` 标记客户端才会收到。

### 绑定层
- `menu/KleinBottleMenu`：26.3 的 `Player#drop` 多了第三个参数 `Prediction`；
  配料摆放用 `PlacementInfo`；槽位同步依赖「两侧槽位数量一致」。
- `client/*`：26.3 GUI 是 `GuiGraphicsExtractor` + `RenderPipelines` 架构；
  `ContainerEventHandler#mouseDragged` 只转发右键拖动；
  `KeyEvent#key()` 返回 SDL scancode（不是 GLFW keycode）；
  流体渲染走 `FluidModel` 注册（原 `IClientFluidTypeExtensions` 已移除）。
- `network/PacketHandler`：NeoForge `PayloadRegistrar` 注册协议（26.x API）。
- `client/jei/ZuoyanJeiPlugin`：JEI 配方传输接口，随 JEI 版本变化。
- 各 `*Registry`：`DeferredRegister` + `EventBusSubscriber` 模式，跨小版本较稳定，
  但注册项签名（如 `Item` 构造 `Properties`、材质 Record）会变。
- `Config`：注册表查询已收口到 `platform/RegistryLookup`。

### 资源（JSON 数据驱动，格式随版本变化）
`src/main/resources` 下配方（`recipe/`）、战利品表（`loot_table/`）、标签（`tags/`）、
世界生成（`worldgen/`）、伤害类型、维度 JSON 均为数据驱动；目录名与字段格式
（如 26.x 的 `loot_table` 单数、`recipe` 单数）在不同版本间变过，迁移时要对照
目标版本的 vanilla 数据包格式核对。

## 5. 迁移步骤建议

1. 改 `gradle.properties`（+ build.gradle 的 JEI/Curios file ID），刷新依赖。
2. 全量编译，优先修复 `platform/` 两个类的报错。
3. 按编译报错处理内容层（item → effect → world → menu）。
4. 处理绑定层（client 渲染差异大多不报编译错，要进游戏逐个界面验证）。
5. 核对资源 JSON 格式（跑 datagen 或对照 vanilla 数据包）。
6. 游戏内回归：四维空间终端、随身熔炉、真空衰变、领域展开、随身维度切换。

## 6. `1.20.1-forge` 分支专有差异（从 26.x 主线往回迁时必看）

分支的代码结构与主线一致（同样用 mojmap，类名/方法名风格不用切换），
但下面这些点是**逐条踩过的**，凭记忆改必错：

### 构建侧
- **必须用 JDK 17 跑 Gradle**：PATH 上的 `java` 可能是 25，ForgeGradle 6 会直接报
  `Unsupported class file major version 69`。`export JAVA_HOME="C:\Program Files\Java\jdk-17"`。
- ForgeGradle 6 + Gradle 8.8 + Java 17 toolchain；依赖走 CurseMaven（JEI / Curios 的 file ID
  随版本变，见 `build.gradle` 注释）。
- **Curios 只 `compileOnly`，不要加 `runtimeOnly`**：1.20.1 那版 Curios 的 mixin 带的是 SRG
  refmap，`fg.deobf` 不会把它重映射到 official 名，dev 环境一启动就
  `Mixin apply failed curios.mixins.json:AccessorEntity` 直接崩溃（与我们的模组无关）。
  只 compileOnly → 编译期能拿到 API，dev 不加载 Curios（饰品走背包判定），
  正式环境玩家装 Curios 即启用饰品栏。

### 资源目录/格式（全是"目录名或键名对不上 → 静默失效"）
| 26.x | 1.20.1 |
|---|---|
| `data/<ns>/recipe/` | `data/<ns>/recipes/` |
| `data/<ns>/loot_table/` | `data/<ns>/loot_tables/` |
| `data/<ns>/loot_table/inject/` | `data/<ns>/loot_tables/inject/` |
| `data/<ns>/structure/*.nbt`（结构模板） | `data/<ns>/structures/*.nbt`（**复数**） |
| `data/minecraft/tags/block/`、`tags/item/` | `data/minecraft/tags/blocks/`、`tags/items/` |
| `data/curios/tags/item/` | `data/curios/tags/items/` |
| `data/neoforge/biome_modifier/` | `data/<ns>/forge/biome_modifier/` |
| `assets/<ns>/items/<name>.json`（物品定义） | 无此机制，靠 `models/item/` + 注册代码里的属性 |
| `src/main/templates/META-INF/neoforge.mods.toml` | `src/main/resources/META-INF/mods.toml` |
| 配方 `result: { id, count }`（1.21+） | `result: { item, count }`；ingredient 必须写成 `{ "item": ... }` 或 `{ "tag": ... }`，**裸字符串非法** |
| 战利品表 pool 里 `"condition": {...}` | pool 里 `"conditions": [ {...} ]`（数组） |

### 结构 NBT（最容易静默翻车的一条）
- 1.20.1 的 `NbtUtils.readBlockState` 只认调色板里的 **`Name` / `Properties`**（首字母大写）；
  26.x 是 **`id` / `properties`**（全小写）。
  **键名不对 → 每个方块都读成空气 → `/place template` 放下一片空地，且没有任何报错。**
- 顺便把 `DataVersion` 改成目标版本（1.20.1 = **3465**），否则结构数据修复器会拿新版本号去跑旧修复链。
- 转换工具：`tools/rick_structure_nbt.py retag <nbt> [--data-version 3465]`（会先备份 `.orig`，
  **备份务必放在 `tools/` 里**，放 `src/main/resources` 旁边会被 `processResources` 打进 jar）。

### 代码层（编译期就会报，或只在服务端暴露）
- `DeferredRegister.create(Registries.X, MODID)` + `RegistryObject`；没有 `DeferredRegister.Blocks/Items` 便捷包装。
- `MenuType` 构造要传 `FeatureFlags.DEFAULT_FLAGS`。
- 方块实体存档是 `saveAdditional(CompoundTag)` / `load(CompoundTag)`；`SimpleContainer` 没有 `getItems()`。
- 方块交互是 `use(...)`（26.x 的 `useWithoutItem`）；破坏钩子是 `onRemove(...)`（26.x 的 `affectNeighborsAfterRemoval`）。
- `level.isClientSide` 是**字段**不是方法。
- `Recipe<C extends Container>`：`matches/assemble/getResultItem/canCraftInDimensions` + 显式 `getId()`；
  `RecipeSerializer` 是接口（要自己实现 `fromJson` / `fromNetwork` / `toNetwork`）；
  `RecipeType.simple(ResourceLocation)` 造匿名实例。
- 配方查询 `level.getRecipeManager().getRecipeFor(...)` 返回**裸配方**（1.20.1 没有 RecipeHolder 包装）。
- 属性修改器用 **UUID** 标识（26.x 是 ResourceLocation）；`LivingDamageEvent` 直接有 getAmount/setAmount。
- 实体：`EntitySpawnReason`→`MobSpawnType`；`finalizeSpawn` 多一个 `@Nullable CompoundTag` 参数；
  `Arrow` 在 `world.entity.projectile` 包、构造只有 `(Level, LivingEntity)`、落地判定读 `inGround` 字段。
- **`Mob` 的领地（restriction）与 `NeutralMob` 的仇恨在 1.20.1 都不落盘**，
  自己的实体必须在 `addAdditionalSaveData` / `readAdditionalSaveData` 里手工存读。
- `NeutralMob` 在 1.20.1 是 `int` 剩余仇恨 tick + `UUID` 目标（26.x 是 `EntityReference` + 绝对结束时刻）。
- 渲染器没有 render state 分层：`HumanoidMobRenderer<T, M>` + `getTextureLocation(T)`；
  `EntityType.Builder` 没有 `eyeHeight(...)`。
- **common 包（非 `client` 包）里绝不能让 JVM 校验被迫加载客户端类**，否则专用服务端直接
  `NoClassDefFoundError: Screen`。客户端入口要挪进 `client` 包且**方法签名只用通用类型**，
  例：`client/DeathNoteClient.openScreen(ItemStack)`、`client/ClientPacketSender.sendToServer(...)`。

### 冒烟测试
`bash tools/verify_1201_sync.sh` —— 起 `runServer`（用独立 `level-name`，不动 `run/world`），
等 `Done (` 后走 RCON 跑 `/place template` + 查实体，最后把错误/数据包告警汇总到
`tools/verify_1201_result.txt`。**"common 引客户端类"这类校验期崩溃只有起服务端才看得见。**
