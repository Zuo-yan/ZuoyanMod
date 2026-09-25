# 原始黑洞（Primordial Black Hole）

状态：**已实装（含右键释放效果）** · 2026-09-24
环境：NeoForge 26.3.0.3-beta / Minecraft 1.26.3 · modid `zuoyanmod`

---

## 1. 结论先说

| 项 | 值 |
|---|---|
| 物品 id | `zuoyanmod:primordial_black_hole` |
| 中文 / 英文名 | 原始黑洞 / Primordial Black Hole |
| 稀有度 | `EPIC`（金色名字），堆叠 64 |
| 来源 | **微型强子对撞机**：`1 沉重核心 + 1 暗物质` |
| 对撞时长 | **400 tick（20 秒）** |
| 产物数量 | 1 |
| 定位 | **可释放的奇点装置** —— 右键消耗 1 个，在准心落点展开一个持续 20 秒的黑洞 |
| 效果 | 牵引半径**随生命曲线从 1.5 格张开到 40 格、再收回来**；坍缩时对 12 格内生物爆发 40 点伤害 |
| 使用限制 | 冷却 60 秒；同一玩家同时只能维持一个黑洞 |

## 2. 三条设计理由

### 2.1 为什么放在对撞机，而不是工作台

对撞机是这个 mod 里唯一的"**把两样不相干的东西强行合成一样新东西**"的机器，
语义上就是干这个的。已经在它身上跑的两条配方也是同一逻辑：

| 配方 | 输入 | 读作 |
|---|---|---|
| 反物质微粒 | 潮涌核心 + 烈焰棒 | 两种能量态对撞出反物质 |
| 奇点核心 | 回响碎片 + 下界合金锭 | 深暗回响 + 顶级合金压成奇点 |
| **原始黑洞** | **沉重核心 + 暗物质** | **密度极值 + 质量极值压成黑洞** |

"沉重核心"（`minecraft:heavy_core`，试炼密室的战利品、原版合成重锤的材料）本身就是
原版里"最重的东西"的具象；"暗物质"则已经是本 mod 暗物质链的终点材料。
两者都是各自体系里的**密度上限**，让它们对撞坍缩成黑洞，是这套比喻的直接延伸 ——
比"用一张工作台配方拼出来"要有说服力得多。

顺带，这条配方也给**沉重核心**加了一个原版之外的用途：原版它只用于合成重锤，
多余的核心在此之前是纯垃圾。

### 2.2 为什么时长定 400 tick

现有两档是 160 tick（反物质微粒）和 200 tick（奇点核心）。400 tick 是刻意"越过一档"：

- **它是这个 mod 里第一件不参与任何配方、也不提供任何数值的物品**，
  稀缺性只能靠获取成本表达。若和奇点核心同级，玩家会默认"它应该和奇点核心一样重要"，
  但实际上它现在什么也做不了 —— 成本过低会让人产生"我撞出个没用的东西"的挫败。
- 20 秒一次、一次只出一个，换算下来是**明确的长期目标**：想要它就得让机器真的转起来。
- 将来若给它接上功能（见 §5），这个时长正好是"值得为之投入"的量级。

### 2.3 为什么只做"物"、不做"材料" —— 以及效果是怎么定的

它不参与任何配方。有三条路，选了第三条：

1. **做成中间材料**（某件终局装备的合成件）—— 最省事，但等于把它降级成"过路的"，
   而它已经是这条链上最贵的一格（400 tick + 沉重核心），做中间材料浪费了这个位置。
2. **做成纯收藏品**（撞出来摆着看）—— 玩家花 20 秒撞出来的东西**没有任何反馈**，
   这是最坏的一种终局产物。
3. **做成功能道具（本方案）**：右键释放一个 20 秒黑洞。给了"撞出来的东西"一个出口，
   而且这个效果**不需要另起一套新设计** —— 牵引与爆发都复用项目里真空衰变黑洞
   已经调好的手感公式，只是把"5 秒后爆炸"改成"20 秒持续牵引（范围逐渐张开再收回）+ 结束时爆发"。

**为什么是"牵引 + 结束爆发一次"，而不是持续伤害**：持续伤害会把它的定位从
"改变战场形状的工具"变成"一个伤害技能"，而**牵引才是它的独特价值**
（把散落的掉落物拢成一堆、把怪聚到一起、把逃跑的怪拽回来）。
爆发放在最后一刻，是为了给这 10 秒一个**结算点** —— 否则玩家只会站在旁边看它自己结束。

> 这也符合 `docs/item_acquisition.md` 里那套判据：**获取途径先立住，功能可以后接**。


## 3. 关键实现细节

### 3.1 对撞机输入槽只扣 1 个（硬约束）

`MicroHadronColliderBlockEntity#finishCollision` 里是 `shrink(1)`，
**配方 JSON 里写的数量不参与消耗计算** —— 也就是说
"1 沉重核心 + 1 暗物质"是固定的，想改成"消耗 2 个暗物质"要动 BlockEntity，不是改 JSON。

这条约束对本次没有影响（两样各 1 个正好），但**以后设计对撞配方时要先想到它**：
配方的 `count` 只影响产物，不影响成本。

### 3.2 用 `DescribedItem` 而不是裸 `Item`

物品眼下没有功能，"它是什么"只能靠描述行交代，所以用 `DescribedItem` 挂一行说明。
文案遵循该类的既有约定（见 `DescribedItem` 的类注释）：**只说"是什么、能做什么"，
不写警告句、不堆裸数值**。

等以后真给它接上效果，把注册用的类换成对应的功能类、把 `desc` 改成"能做什么"即可。

### 3.3 贴图是脚本生成的

贴图 `assets/zuoyanmod/textures/item/primordial_black_hole.png`（16×16）由
`tools/gen_primordial_black_hole_texture.py` 生成 —— 与项目里其它贴图同一惯例：
**生成脚本放 `tools/`（`.gitignore` 已登记，不进仓库），只有产物 PNG 进仓库。**

画法：横向细吸积盘（橙→赤→暗红）+ 正圆光子环（暖白）+ 纯黑事件视界（边缘带紫）。
三层的纵向尺度刻意错开 —— 盘的纵向比环窄，于是光子环的上下两端会从盘里"冒"出来，
黑洞的剪影就自然成立了，不需要额外画引力透镜弧。

几个踩过的坑，写进脚本注释了：

| 尝试 | 结果 |
|---|---|
| 画竖椭圆做引力透镜弧 | 45° 方向会露馅成"原子模型"轮廓，已放弃 |
| 按预览图（192px）调参数 | 误导性极强 —— 16×16 下光环会糊成一片白 |
| **必须按真实 16×16 像素放大预览来调** | 最终几何（核心 6px、光环 4px）是按像素尺度反推的 |
| 用 Pillow | 本仓库生成脚本要"随便一台装了 Python 的机器都能跑"，故改为纯 `zlib`+`struct` 手写 PNG 编码器，零依赖 |

## 4. 右键效果的设计与实现

### 4.1 数值

| 项 | 值 |
|---|---|
| 存续 | 400 tick（20 秒） |
| 牵引半径 | **动态**：1.5 格 → 40 格 → 1.5 格（下限/上限在实体类顶部） |
| 生命曲线 | 0~55% 张开（0~11 秒）→ 55%~80% 维持满值（11~16 秒）→ 80%~100% 收束（16~20 秒）→ 坍缩爆炸 |
| 牵引加速度 | `0.09 + 0.20 × (1 - d/R)`，越近越强；进死区后改为强阻尼（防止绕中心公转） |
| 速度上限 | 1.6 格/tick（**必须有**：真空衰变黑洞只活 100 tick，这里活 400 tick，同一套每 tick 叠加的公式会变弹弓） |
| Boss 抵抗 | 吸力 ×0.5（用项目自建标签 `zuoyanmod:bosses`） |
| 爆发半径 / 伤害 | 12 格 / 40 点（普通伤害，吃护甲） |
| 冷却 | 60 秒；同一玩家同时只能维持一个 |

数值全部集中在 `entity/PrimordialBlackHoleEntity` 顶部，改一个常量就能调平衡。

### 4.2 为什么牵引半径是动态的，以及为什么必须和视觉共用一条曲线

**第一版是个"假动画"**：视觉上前 6 秒确实在长大，但牵引半径从生成那一刻起就恒定 10 格 ——
两条曲线脱钩，玩家一眼就能看出"它变大了但吸力没变"。

修法是引入一个**唯一的曲线函数** {@code PrimordialBlackHoleEntity#scaleFactor(ageTicks)}，
**服务端和客户端共用**：

- 服务端：`currentPullRadius() = MIN + (MAX - MIN) × scaleFactor(LIFETIME_TICKS - life)`；
- 客户端渲染器：`视觉半径 = VISUAL_RADIUS × scaleFactor(ageInTicks)`。

两侧各自本地推算（服务端有倒计时 `life`，客户端只有正计时 `tickCount`，都换算成"已经活了多久"），
所以**不需要为它多加任何同步字段**。

> 注意视觉半径（最大 3.6 格）和牵引半径（最大 40 格）**本来就不可能 1:1** ——
> 真按 40 格画会糊满整个屏幕。玩家感知"范围"靠的是另外两条线索：
> ① 周围生物和掉落物开始从多远被拽过来；② 中心粒子云的铺开范围
> （`spawnAmbientParticles` 里也乘了同一个 `scaleFactor`，所以粒子云会跟着一起张开、收拢）。

### 4.3 为什么做成实体，而不是像真空衰变那样做成 Manager

`event/VacuumDecayBlackHoleManager` 那套（静态列表 + `ServerTickEvent.Post`）本次**故意没沿用**，
唯一理由是表现力：

- Manager 方案**没有任何世界空间的自绘能力**。本项目所有特效都是"服务端广播原版粒子"，
  也没用过 `RenderLevelStageEvent` 之类的世界渲染事件 —— 想画"永远正对相机的黑盘 + 自转涡流"，
  唯一现成的挂点就是 `EntityRenderer`（项目里 `CausalityBulletRenderer` 已经铺好路）。
- 顺带白拿位置同步、追踪范围、`getBoundingBoxForCulling` 一整套现成机制。

代价是多注册一个实体 + 一个渲染器；换来的是"看得见的黑洞"而不是"一团粒子"。

### 4.4 三个必须踩对的坑

1. **`energySwirl` 是 QUADS 拓扑，不能发三角形。**
   参考实现用的是"随机旋转的三角链"，但本版本的 `RenderPipelines.ENERGY_SWIRL_SNIPPET` 明确是
   `withPrimitiveTopology(PrimitiveTopology.QUADS)` —— 顶点会被**按 4 个一组**解释成四边形。
   照抄三角链会把画面糊成随机四边形。本方案改成"一整圈四边形带"（48 段），
   细节交给纹理里的螺旋纹路。
2. **速度变更必须补 `syncVelocity = true`。**
   `setDeltaMovement` / `push` 只置 `needsSync`（位置同步），不补这个标记服务端就不会发
   `ClientboundSetEntityMotionPacket` —— 玩家（客户端权威）表现为"被吸但不动"。
   这条真空衰变黑洞那边踩过一次，本次直接沿用。
3. **冷却期间 `Item.use` 根本不会被调用。**
   `ServerPlayerGameMode#useItem` 开头就 `if (isOnCooldown) return PASS;`，
   所以**不要试图在 `use()` 里发"还在冷却"的提示**，那段代码永远不会执行。
   原版给的反馈是物品图标上的灰色冷却扇形（`ItemCooldowns` 自动同步），够用了。

### 4.5 与参考实现（Iron's Spells 的黑洞）的差异，以及为什么

| 参考实现 | 本方案 | 理由 |
|---|---|---|
| 30 秒、半径随法术等级 6~16 | 20 秒、半径随生命曲线 1.5→40→1.5 | 本项目没有法术等级体系；用"张开-维持-收束"代替等级缩放 |
| 每 0.5 秒持续伤害 | 只在坍缩时爆发一次 | 定位是"控场工具"而不是"伤害技能"，见 §2.3 |
| 吃地形（把方块转成下落方块吸进来） | **不做** | 破坏地形不可逆，且与"掉落物聚在中心"的既有行为冲突 |
| 用实体包围盒当效果体积（`sized(R*2,R*2)`） | 本体 2×2，扫描 AABB 每次现造 | 大 AABB 会撑爆实体分区索引、把剔除距离拖到上千格、与活塞/碰撞查询打架 |
| 自定义粒子 `UNSTABLE_ENDER` | 原版 `REVERSE_PORTAL` / `SCULK_SOUL` 等 | 26.3 原版没有该粒子；为一次演出自注册一个粒子类型不划算 |
| 自定义音效（`black_hole_cast.ogg` 等） | 原版音效（`PORTAL_TRIGGER` / `WARDEN_SONIC_BOOM` / `GENERIC_EXPLODE`） | 不要求用户额外提供音频素材 |


## 5. 改动清单

| 文件 | 改动 |
|---|---|
| `item/ItemRegistry.java` | 注册 `PRIMORDIAL_BLACK_HOLE`（`DescribedItem`，EPIC） |
| `item/CreativeTabRegistry.java` | 创造栏"过往浮现"增加该物品（跟在对撞机产出组后面） |
| `data/zuoyanmod/recipe/micro_collision_primordial_black_hole.json` | **新增**配方 |
| `assets/zuoyanmod/items/primordial_black_hole.json` | **新增**物品定义 |
| `assets/zuoyanmod/models/item/primordial_black_hole.json` | **新增**模型 |
| `assets/zuoyanmod/textures/item/primordial_black_hole.png` | **新增**贴图（16×16） |
| `lang/zh_cn.json` / `lang/en_us.json` | 名称 + 描述行 |
| `tools/gen_primordial_black_hole_texture.py` | **新增**贴图生成脚本（本机留存，不进仓库） |
| `docs/item_acquisition.md` | 补记获取途径 |
| `item/PrimordialBlackHoleItem.java` | **新增** 右键使用规则：冷却 / 一人一个 / 全局上限 / 落点 / 扣料 |
| `entity/PrimordialBlackHoleEntity.java` | **新增** 黑洞实体：10 秒存续、牵引、坍缩爆发、演出 |
| `client/PrimordialBlackHoleRenderer.java` | **新增** billboard 黑盘 + energySwirl 涡流 |
| `damage/PrimordialBlackHoleDamageSource.java` | **新增** 伤害来源工厂 |
| `data/zuoyanmod/damage_type/primordial_black_hole.json` | **新增** 伤害类型 |
| `assets/zuoyanmod/textures/entity/primordial_black_hole_core.png` | **新增** 事件视界贴图（64×64） |
| `assets/zuoyanmod/textures/entity/primordial_black_hole_swirl.png` | **新增** 涡流贴图（64×64，参数空间） |
| `tools/gen_primordial_black_hole_entity_textures.py` | **新增** 上面两张贴图的生成脚本（本机留存） |
| `entity/EntityRegistry.java` | 注册 `primordial_black_hole` 实体（`sized(2,2)` / `noSave()`） |
| `damage/ZuoyanDamageTypes.java` | 新增 `PRIMORDIAL_BLACK_HOLE` 伤害类型键 |
| `Zuoyanmod.java` | `onRegisterRenderers` 里登记渲染器 |

**不需要改动**：`RecipeRegistry`、`MicroCollisionRecipe`、JEI 类别 ——
新配方是纯数据，JEI 类别是按 `RecipeType` 派生的，加 JSON 就自动收录。

## 5. 待确认 / 可调项

- **对撞时长**：现在 400 tick。改 `data/zuoyanmod/recipe/micro_collision_primordial_black_hole.json` 的 `duration` 即可。
- **配方原料**：若要换成别的，改同一个 JSON 的 `ingredient_a` / `ingredient_b`（注意 §3.1 的数量约束）。
- **牵引半径上限 / 爆发半径 / 爆发伤害**：现在 40 格 / 12 格 / 40 点，三个常量都在
  `PrimordialBlackHoleEntity` 顶部。半径再往上加意义不大（实体只存在于已加载区块里，
  默认模拟距离约 160 格），而且扫描开销按半径的立方增长。
- **生命曲线的四个阶段比例**：`GROW_END = 0.55`、`SHRINK_START = 0.80`，改这两个数就能调
  "长得多快、维持多久、收得多急"。
- **掉落物要不要自动进背包**：目前只聚到中心。想做成「清场后一键收物」的话，
  在 `collapse()` 里加一段把 `ItemEntity` 塞进施法者背包的逻辑即可。
- **要不要加地形破坏**：参考实现会把方块转成下落方块吸进来，本方案刻意没做（见 §4.4）。
- **音效**：全部用原版音效，不需要额外素材；想换成自建音效见 `sound/SoundRegistry` 的既有做法。

---

## 1.20.1 分支差异

主线用的 `minecraft:heavy_core`（沉重核心）是 **1.21 才加入的物品**（试炼密室战利品、重锤的材料），
1.20.1 里不存在 —— 原样搬过来会让配方加载直接报
`JsonSyntaxException: Unknown item 'minecraft:heavy_core'`，整条配方失效。

所以 1.20.1 分支把这一格换成 **`minecraft:netherite_block`（下界合金块）**：

| | 26.3 主线 | 1.20.1 分支 |
|---|---|---|
| 对撞配方 | 1 沉重核心 + 1 暗物质 | **1 下界合金块** + 1 暗物质 |
| 文案（`desc1`） | 「沉重核心与暗物质在对撞中坍缩而成的原始奇点」 | 「下界合金块与暗物质在对撞中坍缩而成的原始奇点」 |

选它的理由：原版的"沉重核心"是**不可合成的终局材料**，而下界合金块在 1.20.1 里
同样是玩家能拿出的最重的、带"分量感"的终局材料，单格价格也大致相当。
代价是它**可以合成**（9 锭），所以 1.20.1 这边这一格的获取方式比主线平缓一些 ——
如果你想要更接近"只能靠探索拿到"的手感，可以把 `ingredient_a` 换成
`minecraft:nether_star`（凋灵掉落）或 `minecraft:echo_shard`（远古城市战利品），
改 `data/zuoyanmod/recipes/micro_collision_primordial_black_hole.json` 一处即可。
