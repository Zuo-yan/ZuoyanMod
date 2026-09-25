# 物品获取途径设计（Acquisition Design）

状态：**审计完成 · 3 项待确认** · 2026-09-22
环境：NeoForge 26.3.0.3-beta / Minecraft 1.26.3 · modid `zuoyanmod`

---

## 1. 本文档要解决什么

把全 mod **31 件已注册物品**逐个过一遍，回答两个问题：

1. 每一件**现在**从哪来？（有没有漏掉的"孤儿物品"）
2. 每一件**应该**从哪来？——并说明为什么是合成 / 宝箱 / 掉落 / 交易。

## 2. 五条原则（先定标准，再定个例）

| # | 原则 | 理由 |
|---|---|---|
| **R1** | **功能与工具 → 合成** | 玩家会反复依赖它、围绕它做计划（名刀司命、空间锚点、绝对零度）。锁在 RNG 后面等于"有它就活、没它就死"——把运气塞进了操作里。 |
| **R2** | **基础设施与机器 → 合成** | 机器是下游整条链的**唯一入口**，锁住它会连锁堵死所有依赖它的内容。这类东西一旦拿不到，玩家不是"变弱"而是"玩不到"。 |
| **R3** | **力量上限 → 终局合成，且用同一条链自己的产物当货币** | 强度应该用**肝**换，不该用**运气**换。让顶级道具收自己链条的产出（暗物质），既是成本也是主题闭环。 |
| **R4** | **升级部件 / 钥匙 → 结构宝箱** | 它设计上就是"远征的纪念品"。RNG 在这里不是障碍而是动机——宝箱把"变强"变成"值得跑一趟的地方"。 |
| **R5** | **消耗品 → 宝箱 + 低概率** | 可有可无、抽到是惊喜，不抽到也不影响任何构筑。 |

**关于"怪物掉落"和"交易"的结论（本项目的特殊性）：**

- **怪物掉落：本项目不推荐作为唯一来源。** 本 mod **没有自定义怪物**（现有实现里 `dropCustomDeathLoot` / `addDrop` 一次都没出现），
  所以掉落只能挂到原版怪身上。但原版怪里没有"适合掉终局武器"的载体——凋灵/末影龙是 Boss，
  挂上去等于把武器变成刷 Boss 的副产品；普通怪又撑不住这种强度。
  唯一合理的用法是**作为补充来源**（例如"首次击杀末影龙必掉 1 个"），而不是唯一来源。
- **交易：本项目不推荐**（**2026-09-24 修订：保留一条例外，见下**）。村民交易需要一个"可持续产出的中立来源"。而本 mod 全部道具都是**终局强度**
  （紫金锭要挖矿冶炼、圣遗物核心要下界之星 + 龙息），放进交易等于用绿宝石买终局力量，
  直接**绕开它自己的合成链**。要放也只适合消耗品（巧乐兹 / 雪碧）。

  > **修订：唯一合理的交易场景出现了。** 上面这条否定的前提是"拿垃圾货币买终局力量"。
  > 但当交易**收的是别的产业链的产物**、**卖的是消耗品**时，这条否定不成立 ——
  > 反物质子弹（因果律手枪的专用弹药，每开一枪扣一发）就是这种情况，于是挂到了瑞克身上：
  > 收 **2 反物质微粒 + 1 下界合金锭**，换 **10 反物质子弹**，次数无限。
  > 详见 `docs/rick_trade.md`。
  >
  > 判据细化成三条：
  > 1. **卖消耗品 → 可以。** 给循环里的产出一个出货口，不锁死任何东西；
  > 2. **卖"没别处可得"的装备 → 可以。** 这时交易**本身就是它的获取链**，是在补缺口而不是绕开链条。
  >    因果律手枪就是这种：改动前它的配方和战利品表里都搜不到，只能靠交易补上；
  > 3. **卖"本来有合成配方"的装备 → 不行。** 那才是用绿宝石买终局强度、绕开自己的链条。
  >
  > 瑞克目前三笔买卖：**10 反物质子弹** / **1 因果律手枪** / **1 经验修补附魔书**，
  > 全部属于第 1、2 类。详见 `docs/rick_trade.md`。

## 3. 现状矩阵（31 件全量）

| 物品 | 当前来源 | 途径类型 | 状态 |
|---|---|---|---|
| `violet_gold_ore` / `deepslate_violet_gold_ore` | 世界生成（矿脉） | 世界生成 | ✅ |
| `raw_violet_gold` | 矿石掉落 | 世界生成 | ✅ |
| `violet_gold_ingot` | 熔炼 / 分解紫金块 | 合成 | ✅ |
| `violet_gold_block` | 9 紫金锭 | 合成 | ✅ |
| `relic_core` **圣遗物核心** | 4 紫金锭 + 4 钻石块 + 下界之星 + 龙息 | 合成（终局材料） | ✅ |
| `hallowed_upgrade_smithing_template` **圣辉锻造模板** | **末地城宝箱 50%**（本轮改动） | 结构宝箱 | ✅ 已改 |
| `shadow_helmet` / `shengtian_chestplate` / `wind_leggings` / `walker_boots` | 锻造台 + 模板 + 圣遗物核心 | 锻造 | ✅ |
| `absolute_zero` **绝对零度** | 4 蓝冰 + 4 冰 + 1 雪块（本轮新增） | 合成 | ✅ 已补 |
| `void_resonance_pump` **虚空共振泵** | 4 紫金锭 + 2 末影之眼 + 2 紫珀块 + 1 下界之星 | 合成 | ✅ 已补 |
| `dark_matter_particle` | 虚空共振泵运行时产出 | 机器产出 | ✅ |
| `dark_matter` **暗物质** | 9 粒子（3×3） | 合成 | ✅ |
| `dark_matter_bucket` **超流体暗物质** | 绝对零度液化 → 用桶舀 | 游戏内行为 | ✅ |
| `space_anchor` **空间锚点** | 4 紫金锭 + 2 暗物质 + 2 末影之眼 + 1 圣遗物核心 | 合成 | ✅ 已补 |
| `vacuum_decay` **真空衰变** | 4 暗物质 + 4 超流体暗物质 + 1 圣遗物核心 | 合成 | ✅ 已补 |
| `domain_expansion` **领域展开** | 4 暗物质 + 4 末影之眼 + 回响碎片（本轮改材料） | 合成 | ✅ 已改 |
| `beiming_blade` 北冥狂刃 | 紫金锭 + 圣遗物核心 + 下界合金锭 | 合成 | ✅ |
| `hercules_bow` 海格力斯之弓 | 2 紫金锭 + 2 线 + 圣遗物核心 | 合成 | ✅ |
| `jack_the_ripper_scalpel` 开膛手的手术刀 | 合成 | 合成 | ✅ |
| `ming_dao_si_ming` 名刀司命 | 紫金锭 + 圣遗物核心 + 下界合金锭 | 合成 | ✅ |
| `ring_of_kills` / `wan_hui_ring` / `counter_belt` / `voodoo_necklace` / `yemengade_venom_fang` | 各自配方 | 合成 | ✅ |
| `death_note` 死亡笔记 | 合成 | 合成 | ✅ |
| `chocolate_crisp` 巧乐兹 | 合成 **+** 各类地牢宝箱 1% | 合成 + 宝箱 | ✅ |
| `sprite_drink` 雪碧 | 合成 | 合成 | ✅ |
| `antimatter_particle` 反物质微粒 | 微型强子对撞机（1 潮涌核心 + 1 烈焰棒，8 秒/粒） | 机器产出 | ✅ 已补记 |
| `antimatter_bullet` 反物质子弹 | **瑞克交易**：2 微粒 + 1 下界合金锭 → **10 发**，次数无限（2026-09-24 新增） | 交易 | ✅ 已补 |
| `primordial_black_hole` 原始黑洞 | 微型强子对撞机：**1 下界合金块 + 1 暗物质**，400 tick（2026-09-24 新增；1.20.1 无 `heavy_core`，见 `docs/primordial_black_hole.md` 末尾的差异说明） | 机器产出 | ✅ 已补 |
| `causality_pistol` 因果律手枪 | **瑞克交易**：3 原始黑洞 + 42 反物质微粒 → 1 把（2026-09-24 新增，此前无任何来源） | 交易 | ✅ 已补 |

> ⚠️ 本节表格是"加反物质链之前"的 31 件版本，`antimatter_particle` / `antimatter_bullet` /
> 微型强子对撞机 / 奇点核心 / 克莱因瓶 / 原始黑洞等后续内容已补记在上表末尾，未逐条重排。

## 4. 三个缺口物品的推荐方案

> **状态：已全部被采纳并实装**（用户在原建议上做了调整：真空衰变把四条边的紫金锭换成了**超流体暗物质**；
> 虚空共振泵与空间锚点保持原样）。落地后的实际配方见 §5，下面保留当初的推导过程与理由。

### 4.1 虚空共振泵 —— 推荐：**工作台合成**（R2）

```
紫金锭    末影之眼   紫金锭
紫珀块    下界之星   紫珀块
紫金锭    末影之眼   紫金锭
```

= 4 紫金锭 + 2 末影之眼 + 2 紫珀块 + 1 下界之星

**理由（这是三个里最不能犹豫的一个）**
- 它是**暗物质粒子的唯一来源**，而暗物质是绝对零度、真空衰变、领域展开的共同前置。
  把它锁进 RNG，后果不是"玩家弱一点"，而是**运气差的玩家永远摸不到这个 mod 的终局内容**。
- 用**下界之星**（击杀凋灵）当门槛：机器只在末地生效，却是"打完凋灵才造得出来"的终局装置，强度定位成立。
- 用**紫珀块**（末地城材料）：设定上解释得通——你得先去过末地，才能造出只在末地工作的机器。
- 用**末影之眼**：既是末地语义，也让它比一般机器贵一档。

### 4.2 空间锚点 —— 推荐：**工作台合成**（R1）

```
紫金锭    末影珍珠   紫金锭
末影之眼  圣遗物核心  末影之眼
紫金锭    末影珍珠   紫金锭
```

= 4 紫金锭 + 2 末影珍珠 + 2 末影之眼 + 1 圣遗物核心

**理由**
- 它是**生存功能道具**（致命伤害 → 传回锚点，120 秒冷却），玩家会把它当成"构筑的一部分"来规划。
  功能道具一旦靠 RNG 获得，玩家就无法围绕它做计划——这是最伤的一类门槛。
- 价格对齐现有终局道具的惯例（名刀司命 = 紫金锭 + 圣遗物核心 + 下界合金锭）。
  空间锚点严格强于名刀司命（保命 **+ 位移 + 跨维度**），所以贵一档：4 紫金锭 + 圣遗物核心。
- **末影珍珠 + 末影之眼**承担"传送"的语义，比塞一堆金锭更好读。

### 4.3 真空衰变 —— 推荐：**终局合成**（R3）

```
暗物质    紫金锭    暗物质
紫金锭    圣遗物核心   紫金锭
暗物质    紫金锭    暗物质
```

= 4 暗物质 + 4 紫金锭 + 1 圣遗物核心

**理由**
- 它是**全局力量上限**（137 伤害 + 80% 减伤 + 聚怪黑洞）。
  这种强度的获取方式只有两个选项：**很贵**，或者**靠运气**。
  对一把比你所有装备加起来都强的武器来说，"靠运气"是更坏的设计——它会让"没抽到的人"直接没有游戏体验。
- 用**暗物质**当主要货币，好处是成本直接压在**虚空共振泵的产出**上：
  4 暗物质 = 36 个暗物质粒子，代表这台机器实打实的运行时间。**主题闭环**也成立——
  这把锤子的「分子离解」本来就复用暗物质侵蚀的效果，用暗物质造它是自洽的。
- 沿用项目既有的"紫金锭 + 圣遗物核心"终局惯例（名刀司命 / 北冥狂刃 / 海格力斯之弓都是这个骨架）。

## 5. 本轮已落地的改动

| 改动 | 文件 |
|---|---|
| 领域展开：4× 紫金锭 → 4× 暗物质 | `data/zuoyanmod/recipe/domain_expansion.json` |
| 圣辉锻造模板：改为仅末地城宝箱 50% | `data/zuoyanmod/loot_modifiers/hallowed_template_end_city.json`（新）<br>`data/zuoyanmod/loot_table/inject/hallowed_template.json`（新） |
| 移除圣辉锻造模板的合成配方 | `docs/removed_recipes/hallowed_upgrade_smithing_template.json`（移出 datapack，保留备份） |
| **删除废弃的 GLM 索引** `data/neoforge/loot_modifiers/global_loot_modifiers.json` | `docs/removed_recipes/global_loot_modifiers.json`（26.3 无此文件，留着必报错，见 §7） |
| **空间锚点：补上缺失的配方**（含暗物质） | `data/zuoyanmod/recipe/space_anchor.json`（新） |
| **真空衰变：补上缺失的配方**（四角暗物质 + 四边超流体暗物质） | `data/zuoyanmod/recipe/vacuum_decay.json`（新） |
| **虚空共振泵：补上缺失的配方**（4 紫金锭 + 2 末影之眼 + 2 紫珀块 + 1 下界之星） | `data/zuoyanmod/recipe/void_resonance_pump.json`（新） |
| 死亡笔记：4× 紫金锭 → 4× 暗物质 | `data/zuoyanmod/recipe/death_note.json` |
| 超流体暗物质桶加 `craftRemainder(BUCKET)` | `item/ItemRegistry.java`（它是合成材料了，必须像奶桶一样还空桶） |

**关于"移除合成配方"这件事，请确认。** 原配方是 7 钻石块 + 纸 + 1 紫金锭 **产出 2 个**
（极其便宜），如果保留它，"末地城宝箱"这条获取途径就是纯粹多余的。
我按"把获取方式设定为末地城"的字面意思把旧配方移出了 datapack，
但**没有删除**——原文在 §6，要恢复只需把它复制回 `data/zuoyanmod/recipe/`。

**为什么概率是 50%**：全套圣辉装备有 4 件，每件锻造都**消耗 1 个模板**，所以集齐需要 4 个模板。
末地城的主塔宝箱和末地船宝箱用的是**同一张** `chests/end_city_treasure`，即平均每座城约 1~2 个箱子。
50% 下平均**约 4~8 座末地城**集齐全套——这是刻意的终局远征量。嫌久可以在
`data/zuoyanmod/loot_table/inject/hallowed_template.json` 里把 `chance` 调到 0.7，或者给 `set_count` 让它一次掉 2 个。

## 6. 附录：被移出的配方原文

```json
{
  "type": "minecraft:crafting_shaped",
  "key": {
    "#": "minecraft:diamond_block",
    "P": "minecraft:paper",
    "V": "zuoyanmod:violet_gold_ingot"
  },
  "pattern": [
    "#P#",
    "#V#",
    "###"
  ],
  "result": {
    "id": "zuoyanmod:hallowed_upgrade_smithing_template",
    "count": 2
  }
}
```

## 7. 技术备忘：战利品注入的正确姿势（26.3 / NeoForge）

> ⚠️ **本节原先写错了，已按源码与实机日志更正。** 26.3 的 NeoForge **没有** `global_loot_modifiers.json`
> 索引文件了——把老版 Forge 的索引文件放进 `data/neoforge/loot_modifiers/` 会导致游戏启动时报
> `Couldn't parse data file 'neoforge:global_loot_modifiers' … No key type in MapLike[…]`。

**依据**：`LootModifierManager` 现在直接继承 `SimpleJsonResourceReloadListener<IGlobalLootModifier>`，
用 `FileToIdConverter.registry(ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("loot_modifiers")))`
**自动扫描所有命名空间的 `data/<ns>/loot_modifiers/*.json`**——每个文件自己就是一个修改器，靠文件里的 `"type"` 分派。

所以只需要两个文件：

```
data/zuoyanmod/loot_modifiers/<名字>.json      ← "塞进哪张表"（neoforge:add_table + loot_table_id 条件）
data/zuoyanmod/loot_table/inject/<名字>.json    ← "往箱子里塞什么"（带 random_chance）
```

**不需要**任何索引/注册表文件。

| 要点 | 说明 |
|---|---|
| **不要**建 `data/neoforge/loot_modifiers/global_loot_modifiers.json` | 26.3 已无此索引；它会被当成一个名叫 `neoforge:global_loot_modifiers` 的修改器去解析，必然失败并刷 ERROR |
| 每个修改器必须有 `"type"` | 反序列化是按 `type` 分派的（`IGlobalLootModifier.DIRECT_CODEC`），缺了就是上面那条 `No key type in MapLike` |
| 用 `neoforge:add_table`，**不要覆盖原版表** | 覆盖 `data/minecraft/loot_table/chests/end_city_treasure.json` 会整表替换，与其它做同样事情的 mod 直接冲突 |
| 条件用 `neoforge:loot_table_id` | 单个条件可以裸写；多个才需要 `minecraft:any_of` 包一层 |
| 概率写在**注入表**里（`random_chance`） | 修改器只管"注入哪张表"，概率属于表自己的 `entries.condition` |
| 末地城与末地船共用 `chests/end_city_treasure` | 一条规则同时覆盖主塔宝箱与飞船宝箱 |
| 这些文件**不参与构建校验** | 配方/战利品 json 是运行时加载的，构建通过 ≠ 格式正确。**必须看游戏日志**（`run/logs/latest.log` 搜 ERROR/WARN）来验证，而不是只看 build 有没有过 |
