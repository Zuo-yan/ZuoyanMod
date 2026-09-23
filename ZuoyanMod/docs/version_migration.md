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
