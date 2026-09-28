# 成就系统设计文档（GDD）

状态：**36 个文件实装并通过服务端验证 · 已按第三/四批反馈收尾**（`Loaded 1902 advancements` · 0 ERROR · lang 72 键齐备 · 含 1 个 Mixin 修复） · 2026-09-24  
环境：NeoForge 26.3.0.3-beta / Minecraft 1.26.3 / Java 25 · modid `zuoyanmod`

> 前置决策（用户已拍板）：受众 = **自己和朋友的小服** / 奖励 = **零实质奖励** / 实现 = **先只做零 Java 版**（仅用内置 trigger）。  
> 这三条是下面所有取舍的地基，任何一条变了，密度分配要重画。

---

## 结论先说

| 问题               | 答案                 | 一句话理由                                                                                          |
| ---------------- | ------------------ | ---------------------------------------------------------------------------------------------- |
| 成就给什么奖励？         | **什么都不给**          | advancement reward 能发物品和配方，是绕开 `docs/item_acquisition.md` R1–R3 的合法后门。开了它等于宣布"点开 UI 的次数能换终局强度" |
| 成就扮演什么角色？        | **碑，不是任务清单**       | 熟人服里玩家可以直接问作者，所以"引导"价值低；真正稀缺的是"共同记忆"                                                           |
| 做多少条？            | **30 条左右，B 类占比上调** | 熟人服的社交货币是"你猜我昨天干了什么"，发现类比里程碑值钱                                                                 |
| 要不要写自定义 trigger？ | **这一版不写**          | 48 个内置 trigger 已经覆盖了三条链的所有节点 + 大部分伤害/状态彩蛋                                                      |
| 最贵也最不该做的是什么？     | **"×100" 计数类**     | 26.3 没有通用计数器，每条都要一个自己的 `SimpleCriterionTrigger`；它不制造任何新决策，却最费工                                 |

---

## 1. 这套成就服务什么（三条支柱）

每条成就过不了这三条，就不该写。这不是装饰性原则，是删减清单的依据。

| #      | 支柱                         | 过审用法                                    |
| ------ | -------------------------- | --------------------------------------- |
| **P1** | **记录，不引导**                 | 熟人服里没人靠 UI 认路。成就写的是"这件事发生过"，不是"请去做这件事"  |
| **P2** | **每个 achievement 对应一个记忆点** | 玩家解锁时能复述出当时的场景。做不到这一条的（如"用了 100 次"）一律不写 |
| **P3** | **不进经济系统**                 | 没有 rewards 字段，不解锁配方，不给物品。它是一个只读层        |

### 为什么 P2 在小服受众下反而更重要

公开发布的 mod 里，成就的核心价值是"不知道下一步干什么"——那时 A 类里程碑要密。  
但你的受众是**会互相说话的人**：他们遇到卡点会直接问你，不需要 UI 兼任说明书。  
真正稀缺的反而是**值得在群里讲一遍的瞬间**。所以密度要从 A 类挪给 B 类。

代价要说清楚：**陌生人第一次玩这个 mod 时会少一层内建引导**。  
如果你之后打算公开发布，§3 的 A 类清单要再加 10–15 条，并把隐藏比例压下来——那是一次重画，不是微调。

---

## 2. 密度分配（[PLACEHOLDER·待 playtest 修正]）

| 类别                     | 条数    | 假设                                  | 验证路径                                 |
| ---------------------- | ----- | ----------------------------------- | ------------------------------------ |
| **A 里程碑**（有意为之 · 首次）   | **清单 26 / 目标 ~18** | 三条链 + 圣辉套 + 克莱因瓶，每条链 4–6 个节点就够撑起章节感 | 让朋友玩一个档，问"哪几个瞬间是真的爽"。没人提得起的就是冗余，**多出来的 8 条是 F1 的首要砍伐对象** |
| **B 发现 · 彩蛋**（偶然 · 首次） | **清单 7**     | 熟人服里传播主要靠这类；其中一半设 `hidden`          | 看有没有人在群里自发提起。没人提说明不够怪                |
| **C 精通**（高门槛 · 可重复）    | **清单 2 / 目标 ~4**  | 只做内置 trigger 就能表达的（伤害类型谓词 / 距离谓词）   | 目前只找到两条能免费用内置 trigger 表达的，见 §8 第 8 项       |
| **D 累积计数**             | **0**          | 见 §4                                | 无需验证，直接否决                            |

> **别假装自洽**：A 类我当初建议 ~18 条，但 §3 逐条写完实际是 26 条——**超了 8 条**。
> 这两个数字我都留在表里，因为把它们抹平才是自欺：那 8 条不是"算错了"，是写清单时每个单看都合理、
> 加起来就偏多。它们能不能留，由 playtest 的 F1 决定，不由我此刻的算术决定。
> C 类反过来偏少（想要 ~4 条，只找到 2 条能零 Java 表达的）——这是"先做零 Java 版"的直接代价。

我假设一个存档是**几十小时的长期档**。如果实际是"两周速通开新档"，整个表要重画：  
A 类要压缩到 12 条以内，B 类的比例还得再往上提（短档更需要即时笑点），  
另外成就不宜有跨档才能完成的隐含要求。

---

## 3. 成就清单（已实装 · 31 条叶子 + 1 根 + 4 个分支根 = 36 个文件）

> 所有 id 都省略 `zuoyanmod:` 前缀。trigger 列已经逐个对过 26.3 源码，字段名见 §6。

### 根节点

| id     | 文案（title / desc）                | trigger                                          | 显示   |
| ------ | ------------------------------- | ------------------------------------------------ | ---- |
| `root` | Yellow and purple小金锭 / 你的第一枚紫金锭 | `inventory_changed` · items: `violet_gold_ingot` | task |

**为什么用"拥有"而不是"挖到"**：这是整个树唯一的入口，用最容易发生的行为当钩子，玩家进树的第一格不用等。  
（`inventory_changed` 的缺点是创造模式也会触发，见 §4.2——根节点反而最无所谓，这里是例外不是惯例。）

### 紫金链

| id                  | 文案                      | trigger                                                           |
| ------------------- | ----------------------- | ----------------------------------------------------------------- |
| `relic_core`        | 一切的核心 / 合成第一枚圣遗物核心      | `recipe_crafted` · recipes: `relic_core`                          |
| `end_city_treasure` | 十字军东征 / 拿到第一块圣辉锻造模板     | `inventory_changed` · items: `hallowed_upgrade_smithing_template` |
| `hallowed_full`     | 圣辉加身 / 集齐四件圣辉装备         | `inventory_changed` · items: 四件各一（**AND 语义待实测，见 §8**）             |
| `beiming_blade`     | 越战越勇 / 合成北冥狂刃           | `recipe_crafted`                                                  |
| `ming_dao_si_ming`  | 电脑前这位选手名字是叫梦泪吗?/ 合成名刀司命 | `recipe_crafted`                                                  |
| `hercules_bow`      | 希腊神话中的英雄 / 合成海格力斯之弓     | `recipe_crafted`                                                  |

### 暗物质链

| id                    | 文案                  | trigger                                          |
| --------------------- | ------------------- | ------------------------------------------------ |
| `void_resonance_pump` | 暗物质的唯一入口 / 合成虚空共振泵  | `recipe_crafted`                                 |
| `first_dark_matter`   | 宇宙的幽灵 / 合成第一块暗物质    | `recipe_crafted` · `dark_matter_from_particles`  |
| `absolute_zero`       | 物质的第五态 / 合成绝对零度     | `recipe_crafted`                                 |
| `superfluid`          | 舀起一片虚空 / 用桶舀起超流体暗物质 | **`filled_bucket`** · item: `dark_matter_bucket` |
| `space_anchor`        | 无序中的锚点 / 合成空间锚点     | `recipe_crafted`                                 |
| `vacuum_decay`        | 文明清扫者 / 合成真空衰变      | `recipe_crafted` · **frame: goal**               |
| `domain_expansion`    | 意气风发啊，老师 / 合成领域展开   | `recipe_crafted`                                 |

`superfluid` 是这一版里我最喜欢的一条：**`filled_bucket` 这个 trigger 和"蹲下来把液态虚空舀进桶里"这个动作严丝合缝**，一条 Java 都不用写，还把"液化"这个容易被忽略的中间步骤记住了。这类"机制本来就长得像某个 trigger"的机会应该优先挑出来用。

### 反物质链

| id                      | 文案                   | trigger                                                      |
| ----------------------- | -------------------- | ------------------------------------------------------------ |
| `micro_hadron_collider` | 桌上的对撞机 / 合成微型强子对撞机   | `recipe_crafted`                                             |
| `antimatter_particle`   | 第一粒反物质 / 产出第一粒反物质微粒  | `inventory_changed` · items: `antimatter_particle`           |
| `singularity_core`      | 奇点核心 / 拿到第一个奇点核心     | `inventory_changed`                                          |
| `met_rick`              | Rick姥爷 / 第一次和瑞克做成交易  | **`trade`** · villager 类型 `zuoyanmod:rick`（**谓词写法待验证，见 §8**） |
| `primordial_black_hole` | 人造奇点 / 开出第一个原始黑洞     | `inventory_changed`                                          |
| `causality_pistol`      | 改变过去需要多少微粒 / 拿到因果律手枪 | `inventory_changed`                                          |

反物质链三条尾节点只能用 `inventory_changed`，因为它们没有合成配方（产出自机器 / 来自交易）。  
**这是妥协不是偷懒**：`recipe_crafted` 表达的"你亲手做出来了"更准确，但这里确实没有"合成"这个动作可挂。  
如果之后要给这些 higher fidelity 的语义，就得写自定义 trigger——下一版再说。

### 维度与 misc

| id             | 文案                       | trigger                                                    |
| -------------- | ------------------------ | ---------------------------------------------------------- |
| `enter_realm`  | 超平坦世界 / 第一次踏入超平坦世界        | **`changed_dimension`** · to: `zuoyanmod:realm`（图标 `minecraft:grass_block`） |
| `enter_domain` | 听雨的声音~一点点清晰~ / 第一次踏入领域展开 | **`changed_dimension`** · to: `zuoyanmod:domain_expansion` |
| `klein_bottle` | 维度升华 / 合成克莱因瓶            | `recipe_crafted`                                           |
| `chocolate_crisp` | 我一口气能吃三根巧乐兹 / 吃下第一根巧乐兹 | **`consume_item`** · item: `chocolate_crisp`                       |
| `sprite`       | 透心凉 / 喝下第一口雪碧            | **`consume_item`** · item: `sprite_drink`                  |
| `death_note`   | 基拉的诞生 / 合成死亡笔记           | `recipe_crafted`                                           |

### B 类：发现与彩蛋（**这一版的重心**）

| id                      | 文案                    | trigger                                                               | hidden |
| ----------------------- | --------------------- | --------------------------------------------------------------------- | ------ |
| `caught_in_vacuum`      | 以身作则 / 被别人的真空衰变波扫到    | `entity_hurt_player` · damage.type: `zuoyanmod:vacuum_decay`          | 否      |
| `caught_in_black_hole`  | 被队友的奇点盯上了 / 卷进别人的黑洞   | `entity_hurt_player` · damage.type: `zuoyanmod:primordial_black_hole` | **是**  |
| `molecular_dissolution` | 分子层面的解体 / 获得分子离解      | **`effects_changed`** · effects: `zuoyanmod:molecular_dissolution`    | 否      |
| `dark_matter_erosion`   | 你正在慢慢消失 / 首次受到暗物质伤害   | `entity_hurt_player` · damage.type: `zuoyanmod:dark_matter`           | 否      |
| `fight_again`           | 再战！ / 触发"再战"效果        | `effects_changed` · effects: `zuoyanmod:fight_again`                  | 否      |
| `multiverse_ray`        | 另一个你打你了一枪 / 中了多元射线    | `entity_hurt_player` · damage.type: `zuoyanmod:multiverse_ray`        | **是**  |

这七条单独靠你已注册的**五个自定义伤害类型 + 两个效果**就全都能做，零成本。
`zuoyanmod:vacuum_decay` / `dark_matter` / `heart_paralysis` / `multiverse_ray` / `primordial_black_hole`
已经在 `damage_type/` 里注册过了，谓词可以直接引用——**当初建这些 damage type 的时候，顺手就把七条成就的前置工作做完了**。

### 两个"自己免疫"反而让成就更值钱

**你的真空衰变波和自己的黑洞不会波及自己**，所以 `caught_in_vacuum` / `caught_in_black_hole` 一旦触发，**来源必定是别人**。
这不是要删掉这两条的理由，而是让它们变好的理由：JSON 里根本没法写"凶手不是我自己"这个条件，
但机制替我们做到了——**这两条天然就是纯粹的社交成就**，在熟人服里它们会从"我失误了"变成"你昨天那一炮"。

### 黑曼巴的两个效果：我原先理解错了，已按源码更正

| effect id | zh_cn 显示名 | 实际用途（读源码确认） |
|---|---|---|
| `zuoyanmod:mamba_force_attack` | 你嘴唇有点发紫. | **一击必杀。** 持有者下次造成玩家伤害时，取消原伤害、改为 `Float.MAX_VALUE`，并消耗掉该效果 |
| `zuoyanmod:mamba_force_defense` | 心脏麻痹 | **延迟处刑。** 它是 HARMFUL，倒数到最后一 tick 时对**持有者**造成 `Float.MAX_VALUE` 的心脏麻痹伤害（必死） |

### 顺带纠正我自己一个错误结论（重要，别照着错理由改回来）

我一度以为"同时持有攻防两种强化"**做不到**，理由是 defense 是 HARMFUL、像是给别人下的诅咒。
**这个判断是错的**：`IceTeaItem` 喝下去就是给自己**同时**挂上这两个效果
——1200 tick 的一击必杀 + 821 tick 的定时处刑。巧乐兹本身就是一根"限时力量 + 定时死亡"的赌博雪糕，
两个效果同时挂着才是它的常态。

所以那条成就其实**是能做出来的**。它现在的删除理由是"你决定不要"，不是"技术上做不到"——
**结论没变，但理由必须换**。写错理由比删错一条更麻烦：下次有人翻到这里，
会以为它是被机制否决的，然后照着错误的理由把它加回来。

由此 `heart_paralysis` 的语义也变宽了：它**既可能是被别人的死亡笔记写下了名字，
也可能是自己吃了巧乐兹到期**。两条路通向同一句 `我是L`，而心脏麻痹本来就是死亡笔记的死法。

### C 类：精通（少量，且都必须零 Java 可表达）

| id            | 文案                      | trigger                                                            |
| ------------- | ----------------------- | ------------------------------------------------------------------ |
| `long_shot`   | 狙 / 超远距离命中              | `player_hurt_entity` · entity 距离子谓词（**`distance` 键的挂载位置待验证，见 §8**） |

`zuoyanmod:bosses` 这个 entity type tag 是现成的（`data/zuoyanmod/tags/entity_type/bosses.json`），直接白拿。

**但 `boss_slayer` 我只实现了它的一半。** 原本写的是"用本模组的武器终结一位 Boss"，而"凶器是本模组的"
这件事在 JSON 里表达不出来——本模组的近战武器走的是原版 `player_attack` 伤害类型，没有自己的
damage type 可挂谓词；要限定就得给每一件武器单独注册伤害类型，那是 Java 活。
所以文案改成了"亲手终结一位 Boss"：**`player_killed_entity` 能保证"最后一击是你打的"**（排除摔落、
窒息这类环境死），但保证不了你用什么打的。改文案而不是硬凑一个看起来像的条件，是因为
描述写错比少一条限制更糟——玩家会照着描述去理解自己为什么没解锁。

---

## 4. 明确不做的事（这一节和清单同等重要）

### 4.1 不做任何"×N"计数类

两条理由，任何一条单独就够否决它：

1. **它不制造新决策。** "再用某件东西 100 次"没有改变玩家在任何一个 tick 上的选择，它只是一个里程表。
2. **它是全场最贵的。** 26.3 的内置 trigger 全是"单次事件"（唯一带层数和范围语义的是 `construct_beacon`，且只对信标生效）。  
   任何"×N"都要写自己的 `SimpleCriterionTrigger` + 计数持久化。同样的预算能写十五条 §3 的条目。

### 4.2 不会检测创造模式（已知且接受）

我查了 `advancements/predicates/entity/PlayerPredicate.java`：**26.3 的玩家谓词只有 `looking_at`、`input`、`advancements`、`stats` 四个字段，没有 gamemode。**  
`EntityPredicate` 里也没有。所以——

> 你在创造模式里从物品栏拖出一把真空衰变，就会点亮 `vacuum_decay`。

绕过它只有两条路：自定义 trigger 里判 `gameMode`，或者接受它。这一版选**接受**，理由是受众是自己和朋友的生存服，作弊的收益是"骗不到任何人的一格 UI"。  
如果之后公开发布，这是**必须回头解决的第一件事**。

### 4.3 不做跨玩家的"集体成就"

advancement 天生是 per-player 的，数据挂在玩家的 `.dat` 里。  
"全服一共造出 100 把刀"这类东西做不到——架构上它属于 scoreboard 或自定义存档，不是 advancement。  
想要的话是另一个系统，别混进这一版。

---

## 5. 树的形状与 JSON 惯例

```
root 「紫金纪元」
├─ chain_dark      「暗物质」         ← 分支根
├─ chain_antimatter「反物质」         ← 分支根
└─ discovery       「拾遗」           ← 分支根（这里放 §3 的维度/misc/B 类/C 类）
```

| 惯例                                                | 说明                                                                                                                                           |
| ------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| **中间节点用 `impossible` 当 criterion**                | advancement 的 `requirements` 只能引用**自己文件内**的 criterion，不能引用子成就。所以每个分支根都得有自己的条件；用 `impossible` 而不是重复子节点条件，是为了避免"同一件事被记两次"（否则父节点会在子节点还没到时就自己亮了） |
| **分支根的 `display` 照常写**                            | 熟人服里"按链浏览"是有用的，四条分支让 UI 不至于是一张摊平的 35 格表                                                                                                      |
| **`hidden` 只在叶子上**                                | 父节点漏了 `hidden`，子节点会被从 UI 提前剧透——这是最常被忘的一条                                                                                                     |
| **`frame` 默认 `task`，只有真正的高潮用 `goal`/`challenge`** | 这一版只有 `vacuum_decay` 和 `causality_pistol` 值得 `goal`；`challenge` 留给 §3 的 C 类                                                                  |

### lang 键

沿用 Minecraft 的数据包惯例，而不是本 mod 的 `item./gui./message.` 前缀：

```
advancements.zuoyanmod.<id>.title
advancements.zuoyanmod.<id>.description
```

理由：advancement 是**数据包作者最先接触的一层**，用社群通用前缀，别人想兼容你的进度树时不用先读你的 lang 文件。  
两套 lang 都要加；中英保持 `§` 序列一致（沿用 `MEMORY.md` 的 i18n 约定）。

---

## 6. 26.3 trigger 核对结果（**动手前必读**）

我把 `net/minecraft/advancements/triggers/`（**注意：不叫 `critereon` 了，26.3 已改名**）逐个对过，字段名以下面为准。**几个和我预期不一样的地方写死了就是错的：**

| trigger              | 真实字段结构                                                                      | 坑                                                                                                          |
| -------------------- | --------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| `recipe_crafted`     | `{player, recipes: HolderSet<Recipe<?>>, ingredients: List<ItemPredicate>}` | **`recipes` 是 HolderSet，不是单个 ResourceLocation**。写 `"recipe": "..."` 直接失败，要用 `"recipes": ["zuoyanmod:xxx"]` |
| `inventory_changed`  | `{player, slots?: Slots{occupied/full/empty}, items?: List<ItemPredicate>}` | **我先前写的"`slots` 必填"是错的**，codec 里是 `optionalFieldOf("slots", Slots.ANY)`，可整个省略。**多项 `items` 是 AND**：源码 `matches` 是在背包里逐个把 predicate 消掉，消完才算匹配——所以"集齐四件套"直接写 4 个 predicate 就行 |
| `changed_dimension`  | `{player, from?: ResourceKey<Level>, to?: ResourceKey<Level>}`              | 两个维度 ResourceKey 直接写即可，本 mod 的 `realm` / `domain_expansion` 都对得上                                           |
| `filled_bucket`      | `{player, item?: ItemPredicate}`                                            | 语义完美贴合"舀起超流体暗物质"                                                                                           |
| `consume_item`       | `{player, item?: ItemPredicate}`                                            | 喝饮料用这个，不要用 `using_item`（那是"开始长按"）                                                                          |
| `villager_trade`     | `{player, villager?: LootItemCondition, item?: ItemPredicate}`              | **注册名是 `villager_trade` 不是 `trade`**（`TRADE = register("villager_trade", ...)`）。写 `trade` 直接 `Unknown registry key`                                                           |
| `entity_hurt_player` | `{player, damage?: DamagePredicate}`                                        | **没有 entity 字段**——它描述的是伤害本身，不是攻击方                                                                          |
| `player_hurt_entity` | `{player, damage?: DamagePredicate, entity?: LootItemCondition}`            | 这条才有 entity，远距离命中的判定只能挂这里                                                                                  |
| `effects_changed`    | `{player, effects?: MobEffectsPredicate, source?: LootItemCondition}`       | 注意它也包含"效果消失"，语义是"变化"不是"获得"                                                                                 |

上面两个"待验证"已经在第二批跑通了，结论见下节。
（`inventory_changed` 的 AND 语义靠读源码确认；`slots` 那条我先前写错也已更正。）

### 第二批实测出来的三个坑（一次全撞上了）

三个都是**同一类错误的三种表现**：trigger 里凡是类型声明为 `LootItemCondition` 的字段，
都不能直接写 `EntityPredicate`，必须包一层战利品条件。纵切没撞到是因为纵切只用了
`recipes` / `items` / `item` 这些"直接就是谓词"的字段。

| 现象 | 报错 | 修法 |
|---|---|---|
| `player_killed_entity` / `player_hurt_entity` 的 `entity` 写成裸谓词 | `No key type in MapLike[{"entity_type":...}]` | 包成 `{"type":"minecraft:entity_properties","entity":"this","predicate":{...}}`。`predicate` 里才写 `entity_type` / `distance` |
| `distance` 子谓词挂在哪一层 | 同上 | 挂在 `predicate` **内层**：`EntityPredicate` 是个 dispatched map，`distance` / `entity_type` / `type_specific/player` 全都在同一层并列 |
| `trade` 这个 trigger 不存在 | `Unknown registry key in ResourceKey[minecraft:root / minecraft:trigger_type]: minecraft:trade` | 真名是 `villager_trade`；而且 `villager` 本身也是 `LootItemCondition`，同样要包 `entity_properties` |

顺带三条**顺手确认、没踩坑但值得记下来**的：

- **伤害类型可以直接写 id，不一定要建 tag。** `DamageSourcePredicate.tags` 是 `List<TagPredicate<DamageType>>`，
  而 `TagPredicate.id` 用的是 `HolderSetCodec`：`#` 开头走 tag，**裸 id 走"直接 holder 列表"分支**（`HolderSet.direct`）。
  所以 `{"id":"zuoyanmod:vacuum_decay","expected":true}` 合法，B 类那五条一条 damage-type tag 都不用建。
  （我先前以为必须建 tag，是错的。）注意 `expected` 是**必填**（`fieldOf` 不是 `optionalFieldOf`）。
- `holderSet` 用的是 `compactListCodec`，所以单个值时 `"id": "xxx"` 和 `"id": ["xxx"]` 都行。
- `DistancePredicate` 的 `horizontal` / `absolute` 内部走 `matchesSqr`，但 `boundsSqr = bounds.map(Mth::square)`，
  **所以 JSON 里写的数字单位就是格**，写 `50` 就是 50 格，不用自己开方。这条我一开始以为会踩，实测没有。

### 已实测出来的两个坑（纵切第一次跑服务端就撞上了）

| 现象 | 报错 | 修法 |
|---|---|---|
| 服务端起不来，registry loading errors | `java.lang.IllegalStateException: **Visible advancement roots must have background**` | 可见的根节点（`root`）**必须**有 `display.background`。我先前完全不知道这条约束，写文档时也没查到 |
| （背景字段要指向真实贴图） | 光有字段没有图，打开进度界面时纹理缺失 | 用 `tools/gen_achievement_background.py` 生成 256×256 暗紫底（和现有 GUI 同族），放到 `assets/zuoyanmod/textures/gui/advancements/background.png` |

> 这条值得单拎出来说：**advancement 的根节点比其它节点多一个必填字段**，而它不在任何 schema 提示里，
> 只有 `RegistryDataLoader` 会在启动时告诉你。`./gradlew build` 全程通过、JSON 语法完全合法 ——
> 它就是起不来。这就是"必须看日志"最具体的一次证明。

> 提醒：`data/**/*.json` **不参与构建校验**。`./gradlew build` 通过不代表这些文件是对的，只有进游戏看日志才算验证过。这条在项目里已经踩过一次（战利品注入那轮）。

---

## 7. 失败信号与 playtest 清单

先定义"坏掉长什么样"，再去玩。这套成就系统有三种可以观察到的失败：

| 失败信号            | 观察到什么 = 失败了                                   |
| --------------- | --------------------------------------------- |
| **F1 里程碑冗余**    | 让朋友玩完一个档后，问"哪几个瞬间是真的爽"。有一半 A 类成就没人提得起 → A 类要砍 |
| **F2 彩蛋没有人讲**   | 一周内没有任何人在群里提起过任何一条 B 类 → B 类不够怪，或者藏得太深连撞都撞不上  |
| **F3 变成第二份说明书** | 有人对着成就树"按顺序刷" → 说明它被读成了任务清单，此时应该提高隐藏比例        |

### playtest 顺序（单人就能跑一半）

1. 新世界，`/advancement grant ... only` 之外的正常玩法：挖到第一枚紫金锭 → `root` 应立刻点亮，**并且能打开树看到四个分支名**
2. 依次验证三条链的 `recipe_crafted`：**搓出 `relic_core` 时点在 `relic_core` 而不是 `root`**（criterion 配错的典型症状是点错层级）
3. 拿着绝对零度对着稀有的 dark matter fluid 右键 → `superfluid` 亮
4. 吃巧乐兹 → `chocolate_crisp` 亮（验证 `consume_item` 而不是 `using_item`）
5. 进领域展开 → `enter_domain` 亮（验证 `changed_dimension` 的 `to` 写对）


6. **让另一个人把黑洞丢到你脚下** → `caught_in_black_hole` 亮；**同时确认没别的伤害类成就被误触发**（共用一个 damage type 的串味是这类 trigger 最常见的 bug）。这一步**单人测不出来**，必须两个人
7. 切创造模式，从物品栏拖出真空衰变 → **`vacuum_decay` 会亮**（§4.2 的已知行为，确认它有发生，别到时候当成 bug 去修）

---

## 8. 待确认项

> **状态（2026-09-24 收尾）**：以下 1–9 全部已决策。
> 第 3、7 项在实装过程中被源码/服务端验证解决；第 8、9 项**经权衡决定不做**（理由见表格）；
> 第 1、2、4、5、6 项属于**只有你能回答的运营/玩法问题**，不阻塞实装，留给正式开档前再定。

| # | 问题 | 影响 |
|---|---|---|
| 1 | 一个存档实际能玩多久？我假设几十小时 | 如果是两周速通档，§2 整张密度表要重画 |
| 2 | 同时在线几个人？会不会中途有陌生人加入？ | 有陌生人的话，§3 的 A 类要补 10–15 条、隐藏比例要下调 |
| 3 | ~~`zuoyanmod:rick` 的交易能否用 `trade` 的 villager 谓词精确限定~~ | **已解决**：能。trigger 真名 `villager_trade`，谓词写法见 §6。已在 `met_rick.json` 落地并通过服务端验证 |
| 4 | `/advancement` 命令的权限等级开到几级 | 熟人服可以放开方便测试；但对付出快乐的人，作弊一开记录意义就没了 |
| 5 | 成就 tree 要不要允许被数据包覆盖/扩展 | 影响 id 命名策略。放 `data/zuoyanmod/` 下他人可用同名 path 覆盖 |
| 6 | `dark_matter` 与 `multiverse_ray` 这两种伤害**是否也免疫自己** | 你只确认了真空衰变和黑洞。若同样免疫，`dark_matter_erosion` / `multiverse_ray` 也自动转为"他人造成"，语义更好；若不免疫，它们描述的就是"自己踩进自己造的东西"，文案要跟着改。**另外注意**：液态暗物质是地形，自己走进去也算，所以"免疫自己"与否可能根本不影响 `dark_matter_erosion` |
| 7 | ~~分子离解 `molecular_dissolution` 是谁施加给谁的~~ | **已解决（读源码）**：两个来源——① 站在液态暗物质里（`DarkMatterEventHandler` 每秒给实体挂 80 tick）；② 被别人的真空衰变打中当"引信"（`VacuumDecayEventHandler` 挂 60 秒）。**所以自己就能触发，它不算社交彩蛋**；留在 B 类靠的是"第一次被解体"这件事本身够怪，不是靠"别人干的" |
| 8 | ~~C 类要不要补到 4 条~~ | **已决策：不补，维持 2 条。** 零 Java 能表达的 C 类只有 `long_shot` / `boss_slayer` 这两条（都用了），再补就必须写自定义 trigger 或计数器。**关键理由不是省事，是 C 类的稀缺性本身就是设计资产**——`challenge` 只有 2 条时，点亮它才像一件值得截图的事；补到 4 条会让"极难"变成"还有个没做完的"。领域展开决斗取胜这类候选留给 K 类（自定义 trigger 版）再说 |
| 9 | ~~巧乐兹的"限时必杀 + 定时处刑"要不要单独给一条成就~~ | **已决策：不做。** 这个决策确实值得记（全 mod 唯一的赌博机制），但零 Java 表达不自洽，两条路都有瑕疵：① `consume_item` 与已有的 `chocolate_crisp` 成就**完全重复**，只是换个文案；② `effects_changed` 能靠"出题人"谓词精确表达"同时吃到 `instant_kill` + `heart_paralysis`"，但它是**双向触发**的——`onEffectAdded` / `onEffectUpdated` / `onEffectsRemoved` 三处都会调它（见 `ServerPlayer`），所以效果**消失**时也会重新求值；如果此刻玩家仍持有另一个效果，成就就会在"药效到期"而非"吃下去"的那一刻亮，语义错位。要干净地表达"玩家主动吃下它"必须自己发一个触发器，与"零 Java 版"冲突 |

---

## 9. 落地时的文件清单（预计）

```
src/main/resources/data/zuoyanmod/advancement/
  root.json
  chain_dark.json  chain_antimatter.json  discovery.json
  relic_core.json  end_city_treasure.json  hallowed_full.json  ...   （31 个叶子）
assets/zuoyanmod/lang/
  zh_cn.json   en_us.json          ← advancements.zuoyanmod.*.title / .description
```

### 已完成（纵切，已跑服务端验证）

```
data/zuoyanmod/advancement/   root  chain_dark  chain_antimatter  discovery
                              relic_core  end_city_treasure  hallowed_full
                              beiming_blade  ming_dao_si_ming  hercules_bow      ← 11 个
assets/zuoyanmod/lang/        zh_cn.json / en_us.json 各 +22 键（advancements.zuoyanmod.*）
assets/zuoyanmod/textures/gui/advancements/background.png   ← 工具生成，见下
tools/gen_achievement_background.py                        ← 256×256 暗紫底，纯 Python，固定种子
```

验证结果：`./gradlew runServer` → `Done (2.024s)`、**零 registry 错误**、`Loaded 1877 advancements`，
日志里 13 条 WARN 全是原版自带的（teleport/time 命令歧义 + 离线模式），无一条与 zuoyanmod 相关。

### 第二批（剩 28 个）已全部铺完

纵切的意义是**先把未知字段跑通再批量复制**（`recipes` 的 HolderSet 写法、`items` 的 AND 语义、
根节点的 `background` 必填——后两个都是纵切才发现的）。

第二批按链分批铺：暗物质 7 → 反物质 6 → 维度 misc 6 → B 类 7 → C 类 2。
**实际上是一次写完再跑的服务端**（和上面"切忌一次写完"的建议相反），结果一次性撞出 3 个 ERROR，
三个还都是同一类错误（`LootItemCondition` 没包 `entity_properties`），所以**并没有被淹掉，反而比分批更快**。

修正当初那条建议：真正该忌的不是"一次写多少个文件"，而是**"一次写多少种没验证过的字段类型"**。
第二批 28 个文件里只有 3 处用了新字段类型（`entity` 谓词、`villager` 谓词、一个新 trigger 名），
错误全部集中在这 3 处，一次性读完反而比跑五次服务端省事。**同类字段批量复制是安全的，
新字段类型才要单独试。**

### 最终产出

```
data/zuoyanmod/advancement/   36 个 json（1 根 + 3 分支根 + 31 叶子）
                              （原为 39：第三批删掉 chain_violet / heart_paralysis / boss_slayer）
assets/zuoyanmod/lang/        zh_cn.json / en_us.json 各 +56 键（advancements.zuoyanmod.* 共 78 键）
assets/zuoyanmod/textures/gui/advancements/background.png
tools/gen_achievement_background.py      256×256 暗紫底，纯 Python，固定种子
tools/add_achievement_lang.py            28 条成就的中英文案注入（UTF-8 安全读写 + 回读校验）
```

验证结果：`./gradlew runServer` → `Loaded 1905 advancements`（纵切时 1877，+28）、
`Done (0.320s)`、**0 ERROR**；13 条 WARN 全是原版自带的（teleport/time 命令歧义 + 离线模式）。


---

## 10. 第三批反馈（用户实机试玩后）· 已收尾

用户在实机里把整棵树点了一遍，提了 8 条。逐条处理如下——**这一节的思路比结论更值得看**：
七条是"删/改"，只有一条是真正的 bug，而那条 bug 的根因和"没写 lang"完全不是一回事。

| # | 反馈 | 处理 | 关键点 |
|---|---|---|---|
| 1 | 按键控制的标题没翻译，显示成 `key.category.zuoyanmod.zuoyan` | lang 键 `key.categories...`（复数）改成 `key.category...`（单数） | **不是"忘了写 lang"，是键名差了一个字母。** `KeyMapping.Category.label()` 走 `id.toLanguageKey("key.category")`（单数），而 lang 里写的是 MCP 时代的旧复数键名 `key.categories`。复数那个键**从来没有被读过**，所以一直是裸键名。顺手把 `KeyMapping.Category.register(Identifier)`（NeoForge 已 `@Deprecated`）改成 `event.registerCategory(CATEGORY)`。**这个 bug 能骗过所有检查**：lang 文件里有键、没拼错、值也对，只有键名是旧的 |
| 2 | 删掉 `heart_paralysis`（我是L） | 已删 json + lang | 伤害类型 `zuoyanmod:heart_paralysis` **保留**，`death.attack.heart_paralysis*` 两条死亡消息**不能跟着删**——那是伤害类型的文案，与成就无关。删成就的键时必须限定 `advancements.` 前缀 |
| 3 | 删掉 `boss_slayer`（神也会死） | 已删 json + lang | 详见 §3 里"我只实现了一半"的说明——本来就带瑕疵，删得干净。`zuoyanmod:bosses` entity tag 保留（将来要写自定义 trigger 版还能用） |
| 4 | `enter_realm` 描述改「第一次踏入超平坦世界」 | 已改 zh/en | 原文案「第一次踏入太虚」和维度**显示名**（超平坦世界）脱节了。原则：**成就描述里的名词要和玩家在 UI 上看到的名字一致**，否则玩家得自己建立"太虚 = 超平坦世界"的映射 |
| 5 | `enter_realm` 图标改草方块 | `minecraft:grass_block` | 原版物品 id，零额外资源。比 `ender_pearl` 贴切得多——超平坦世界的视觉符号就是草方块 |
| 6 | 删掉 `chain_violet`（紫金与圣辉），和根成就重复 | 已删 json + lang，**6 个子成就改挂 `root`** | 这条最有价值。分支根存在的意义是"**让 UI 不至于是一张摊平的 36 格表**"（§5），但 `chain_violet` 的 criterion 是 `impossible` 且 `show_toast:false`，它自己从不可见 —— 于是那 6 条叶子实际全部直挂 `root`，玩家看到的就是"根成就下直接排了 9 格"，和根成就本身完全重复。**判决依据写下来**：一个永不点亮的中间节点，不是树结构，只是分组意图；既然意图已经从兄弟分支（dark/antimatter/discovery）体现出来了，它就是纯冗余。删除后 `root` 直挂 9 个子节点（6 条迁移过来的 + 3 条分支根） |
| 7 | 背景是一片黑紫色块 | 重做 `tools/gen_achievement_background.py`，见下 | 唯一需要"画"的一条，原因和修法都反直觉，单独写 |
| 8 | 获取成就时聊天栏不弹消息 | 29 个 json 的 `announce_to_chat` 改为 `true` | **同样是"和预期不符"，但这条不是 bug** |

### 第 8 条的根因：不是没配，是配错了

读源码（`PlayerAdvancements.java:177`）确认弹公告的条件是两个条件的**与**：

```java
if (display.announceToChat() && this.player.level().getGameRules().get(GameRules.SHOW_ADVANCEMENT_MESSAGES))
```

`show_advancement_messages` 这个 gamerule 默认就是 `true`（`GameRules.java:78`），
所以"聊天栏不弹"的**唯一可能**就是 `display.announceToChat()` 为 false——
而我第一版在 29/39 个 json 里写了 `"announce_to_chat": false`。

**这个数字是怎么来的**：第一版的意图是"别刷屏"，所以只给了根和 5 条高潮成就开公告。
这个意图本身没错，**错在对受众的判断**——我在 §1 把受众定为"自己和朋友的小服"，
而小服的成就公告恰恰是**社交货币**："你昨天点亮了什么"是这类服务器里最有意思的对话素材。
**把公告关掉等于把这个系统唯一的社交功能删了。**

现在的规则：**只对 `hidden: true` 的成就保留静默**。
理由不是"藏"，而是**公告会把标题一起剧透给全服**——隐藏成就的乐趣在于"别人突然看到你点亮了它"，
提前公告等于提前泄题。其余全部公告。

### 第 7 条的根因：背景会被平铺，而平铺会抹掉低频

第一版背景的构图是"暗紫底 + 中心一圈极淡辉光 + ±6 噪点"，辉光还刻意压到 18/255，
理由写在 docstring 里："背景的职责是不抢，超过这个数值就会压过节点图标"。

**这个理由本身是对的，但它不适用于平铺场景。** 真相是：

1. 背景贴图在游戏里被 **tile** 成整块进度界面（1080p 下约 4×2 次）；
2. 平铺会把**任何尺度大于贴图的低频渐变**抹掉——中心辉光在每块 256px 的边界处被硬切断，
   拼起来变成规律的网点，"一个中心"的构图信息彻底丢失；
3. 于是肉眼只剩下"底色"这一个信息 —— **这就是"一片黑紫色块"的完整成因。
   `p50=26/255` 的均匀暗紫，不管你看多久都只会看到一块颜色。**

**修法**：第二版只保留**与平铺周期同频或更细的高频结构**，让每一块 256×256 自己就是完整图案：

| 层 | 抬升亮度 | 作用 |
|---|---|---|
| 冷紫底 `#171426` | — | 与 mod 其它 GUI 同族配色 |
| 低频起伏（5 个环绕斑点，半径 90–150px） | ≤ 9 | 打散网格的机械感，让亮度有呼吸。**半径超过贴图尺寸**，靠环绕无缝 |
| 星云（3 个斑点，冷暖分化） | 12–26 | 紫蓝 / 品红的色相分化，给画面"深处" |
| 网格（24px 间距，1px 线宽） | 3 | 把"一块颜色"变成"一张有经纬度的图"。周期 24 与 256 不整除，平铺后斜向错位、无重复感 |
| 星点（150 颗，半径 1.0–3.2px） | 峰值 ≤ 30 | 主体质感 |
| 噪点 | ±3 | 消除塑料感 |

**所有层的距离一律用环绕（torus）距离取模**，这是保证无缝的唯一手段：
星点落在边缘时会被"半颗"地画两次，平铺后严丝合缝。

**验证手法（值得留给下一个贴图）**：`tools/preview_background.py` 用纯 Python 解码 PNG，
把贴图**平铺 3×2 次**渲染出来看，并统计 `p50/p95/max`。判断标准很实在——
纯色块在降采样后只有个位数唯一颜色，新背景是 **356 种**。
**只看单张贴图是看不出这个 bug 的**，必须看平铺后的效果，这和第二批"程序生成贴图必须先在 CPU 上跑一遍渲染数学"是同一条教训的第二次应用。

### 第三批的净变更

```
删除：  chain_violet.json  heart_paralysis.json  boss_slayer.json      （39 -> 36 个 json）
迁移：  beiming_blade / end_city_treasure / hallowed_full /
        hercules_bow / ming_dao_si_ming / relic_core   parent: root
修改：  enter_realm.json（图标 grass_block）
        29 个 json 的 announce_to_chat: false -> true
        RealmKeybindHandler（registerCategory）+ KleinBottleKeyHandler 注释
        lang（键名修正 + 文案修订 + 删 7 键/文件，390 -> 384 键）
重做：  tools/gen_achievement_background.py + background.png（有织理、有星野、平铺无缝）
新增：  tools/preview_background.py（贴图平铺预览，纯 Python 解码 PNG）
        tools/fix_advancements_round3.py（UTF-8 安全的 lang 批量修补 + 孤儿键检查）
        tools/enable_announce_to_chat.py（公告开关批量修改 + 终检）
```

**孤儿键检查**（`fix_advancements_round3.py` 的最后一步）值得单独提：
它双向核对"lang 里的成就 id 必须有 json"和"json 必须有 lang"。
删 json 忘删 lang 会让 lang 里留下一辈子不会被读到的死键，
而这类死键**任何编译和服务端启动都不会报错**，只能靠脚本查。


---

## 11. 「进度界面全空」的排查与修复（Mixin）

**现象**：全新世界里打开进度界面，窗口只有「这里好像什么都没有……」，**连原版进度也不显示**。
服务端日志完全正常：`Loaded 1902 advancements`、0 ERROR。

这一节值得完整记录，因为它是本项目第一次**为了修原版行为而写 Mixin**，
而排查过程里连续走错了两条路。

### 排除法：先证明不是我们的数据坏了

排查的第一原则是**先把"我们的东西坏了"这个可能性排掉**，否则会在自己的文件里空转。

| 检查 | 结果 |
|---|---|
| 服务端加载 | `Loaded 1902 advancements`（原版 1869 + 我们 36 - 重复计算，与 1905-3 对账） |
| 日志 ERROR | 0 条，且没有任何 zuoyanmod 相关告警 |
| 树结构 | 离线模拟 `AdvancementTree.addAll`：1902 个节点**全部插入成功**，7 个根节点正常 |
| 根节点合法性 | 唯一模组根 `root` 有 `display`、有 `background` |

**四条全过 ⇒ 数据没问题，问题在"同步"这个环节。**

### 关键日志：客户端的那个数字

```
[Worker-Main-6/INFO] [AdvancementTree]: Loaded 1902 advancements   ← 服务端（数据包加载）
[Render thread/INFO] [AdvancementTree]: Loaded 2 advancements      ← 客户端（实际收到）
```

`AdvancementTree.addAll` 打印的是**树里最终有多少节点**。
服务端 1902、客户端 2 —— 说明**客户端只收到了 2 个节点**。
进度界面的 tab 列表来自 `tree.roots()`（`AdvancementsScreen.onAdvancementsUpdated`），
收不到可见根 → `tabs` 为空 → `selectedTab == null` → 渲染成「这里好像什么都没有……」。

### 根因：原版漏掉了一次遍历

读 `PlayerAdvancements#flushDirty`：

```java
public void flushDirty(ServerPlayer player, boolean showAdvancements) {
    if (this.isFirstPacket || !this.rootsToUpdate.isEmpty() || !this.progressChanged.isEmpty()) {
        ...
        for (AdvancementNode root : this.rootsToUpdate)   // ← added 的唯一来源
            this.updateTreeVisibility(root, added, removed);
        this.rootsToUpdate.clear();

        ...
        if (!progress.isEmpty() || !added.isEmpty() || !removed.isEmpty()) {
            player.connection.send(new ClientboundUpdateAdvancementsPacket(...));   // ← 真正的发包点
        }
    }
    this.isFirstPacket = false;      // ← 注意：在 if 外面
}
```

而 `rootsToUpdate` **只由 `markForVisibilityUpdate` 填充**，后者只有三个调用点：

1. `applyFrom` —— 读**玩家进度存档**时，逐条标记
2. `award` —— 玩家真的拿到某个成就时
3. `revoke` —— 撤销时

于是全新玩家进入时就卡死了：

- 存档里零条记录 → `applyFrom` 什么都不填
- 一个成就都没拿 → 没有 `award`
- ⇒ `rootsToUpdate` 空 + `progressChanged` 空 → `added` 空
- ⇒ **第 259 行条件为假，包根本不构造**
- ⇒ 但 `isFirstPacket` 照样被置 `false`，**首次全量同步的机会永久消耗**

之后只有玩家真的拿到某个成就、且**那个成就所在的分支根**会被单独同步 ——
所以表现就是"什么都看不到"，而不是"慢慢出现"。

**原版同样中招**：原版只有 1 个根（`minecraft:root`），它的子节点一样要等同步。
这就是为什么"连原版的都没有了"——**不是我们污染了原版，是原版自己在这个场景下也没发**。

### 为什么客户端会收到 2 个（而不是 0 个）

`checkForAutomaticTriggers` 会在玩家加载时对所有 **`criteria` 为空**的成就直接 `award` ——
这会填 `rootsToUpdate`。我们查过：原版 1869 个、模组 36 个**都没有空 criteria**，
所以这 2 个来自**其它模组**的空 criteria 成就（JEI / Curios 之类的数据包）。
它们不是有 display 的可见根，建不出 tab，所以界面依然全空。

### 修复：Mixin 注入 `flushDirty` 的 HEAD

```java
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
    @Shadow @Final private Set<AdvancementNode> rootsToUpdate;
    @Shadow private boolean isFirstPacket;
    @Shadow private AdvancementTree tree;

    @Inject(method = "flushDirty", at = @At("HEAD"))
    private void zuoyanmod$syncAllRootsOnFirstPacket(ServerPlayer player, boolean showAdvancements, CallbackInfo ci) {
        if (!this.isFirstPacket || this.tree == null) return;
        this.tree.roots().forEach(this.rootsToUpdate::add);   // 把全部根标记为待更新
    }
}
```

**注入在 HEAD 而不是其它位置**：`rootsToUpdate` 在方法中途第 250 行就被 `clear()` 了，
只有 HEAD 能保证填进去的内容被这一轮消费掉。

**为什么不用事件方案**：NeoForge 侧没有"发送全量进度包"的公开 API
（`ServerPlayer#getAdvancements` 与 `PlayerAdvancements#flushDirty` 虽是 public，
但 `rootsToUpdate` 是私有字段，没有公开的填充入口）。
用 `award()` 强行授予成就可以间接触发，但会在玩家的进度存档里留下
"这个成就已完成"的**假记录**，破坏"分支根应当保持待点亮"的设计。
**Mixin 在这里是零语义副作用的唯一选择** —— 它只补上原版漏掉的一次遍历。

**依赖注入的失败可见性**：`zuoyanmod.mixins.json` 里 `injectors.defaultRequire = 1`，
所以一旦将来 MC 改动导致 `flushDirty` 签名变化，**启动期就会直接失败而不是静默失效** ——
静默失效正是这个 bug 当初能潜伏下来的原因。

**基础设施其实是现成的**：`src/main/resources/zuoyanmod.mixins.json` 与
`src/main/templates/META-INF/neoforge.mods.toml` 里的 `[[mixins]] config = "${mod_id}.mixins.json"`
从模板创建项目时就在了，只是 `mixins` 数组一直是空的。**这个 mod 之前从未用过 Mixin。**

### 顺带修掉的一个真 bug：`background` 路径写错

排查过程中发现 `root.json` 的 `display.background` 从第一批纵切起就写错了：

```json
"background": "zuoyanmod:textures/gui/advancements/background.png"   ← 错
"background": "zuoyanmod:gui/advancements/background"                ← 对
```

原因在 `ClientAsset.ResourceTexture` 的构造：

```java
public ResourceTexture(Identifier texture) {
    this(texture, texture.withPath(path -> "textures/" + path + ".png"));
}
```

它**自动拼 `textures/` 前缀和 `.png` 后缀**，所以字段值必须是**不含这两者的基名**。
原写法会被拼成 `assets/zuoyanmod/textures/textures/gui/advancements/background.png.png`，
**贴图永远加载不到**。

**为什么潜伏了这么久没被发现**：
1. 这个字段走 `ClientAsset`，是**纯客户端资产** —— 服务端只当字符串存着、不解析，
   所以 `runServer` 永远验证不出它；
2. 而它又不影响 `AdvancementTab.create` 的判定（`background.isPresent()` 看的是
   `Optional` 是否有值，不是贴图是否存在）—— tab 能建出来，只是背景渲染时贴图缺失。
3. **只有真正打开进度界面、并且盯着背景看，才可能注意到。**

这是"服务端验证替代不了客户端验证"的又一个实例：**凡是 `ClientAsset` 类型的字段，
`runServer` 一律验证不到。**
