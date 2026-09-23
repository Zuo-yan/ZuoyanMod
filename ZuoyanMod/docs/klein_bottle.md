# 克莱因瓶 · 四维空间 · 设计文档（GDD）

状态：**已实现（第八版 · 浮空按钮 / 修好铁砧 / 修好 @模组 / 滚动条三条路兜底）** · 2026-09-23
环境：NeoForge 26.3.0.3-beta / Minecraft 1.26.3 · modid `zuoyanmod`

> ⚠️ 下面「第七版」以前的都是历史记录。以**第八版**为准。

---

## 第八版：实机反馈修复（按钮去框 / 铁砧能用 / @模组能用 / 拖动兜底）

### 8.1 铁砧"不能用"——真正的 bug

`anvilInput` 原来是**普通 `SimpleContainer`**，而它的 `setChanged()` **不会**通知菜单——
原版里只有 `TransientCraftingContainer`、`ItemCombinerMenu` 内部那个容器这类
"知道自己属于哪个菜单"的容器才会调 `menu.slotsChanged(container)`。

于是 `updateAnvilResult()` **一次都没被调过**，产物槽永远是空的。
修法：匿名子类覆写 `setChanged()` → `KleinBottleMenu.this.slotsChanged(this)`，
和合成网格（`TransientCraftingContainer`）一个套路。

> 这条和 v3 的「`Slot#setChanged()` 才是槽位内容变化的唯一可靠钩子」是同一族坑：
> **`SimpleContainer#setChanged()` 不会通知菜单**。要"内容变了就算一下"，容器必须自己调。

### 8.2 `@模组` 搜不出来

提示写的是「@模组」，玩家顺手就把 `@模组` 三个字原样敲进去 → 按命名空间**精确**匹配
当然是零匹配。两头来修：提示改成「@模组id」；匹配放宽为**命名空间前缀** 或
**模组显示名包含**（`@zuo` 也能中 `zuoyanmod`，中文名也行），
显示名取自 `ModList.get().getModContainerById(ns)`，取不到就只按命名空间，不会炸。

### 8.3 滚动条拖动——第三条兜底路

v7 的 `mouseDragged` + `mouseMoved` 两条路实测仍不通。**再加第三条**：
`containerTick()`（每 tick 必到）里直接读鼠标当前坐标
（`MouseHandler#getScaledYPos(Window)`）推给滚动条。20Hz 对拖滚动条足够，
而且**不依赖任何事件能不能送达**。命中区域也往外扩了 1px（把凹槽边框算进来）。

### 8.4 左侧四个按钮去掉边框

照 RS2 的浮空控件：平时只有图标本身；hover 时一层极淡的底 + 底部一条青色下划线。
原来的 `buttonFrame`（填充 + 描边 + 高光角标）不再用。

---

## 第七版：滚动条能拖了 + 铁砧缝进终端 + 三张功能卡：滚动条能拖了 + 铁砧缝进终端 + 三张功能卡

### 7.1 滚动条「只能滚轮、按住拖不动」

**根子在 26.3 的事件模型变了。** `ContainerEventHandler#mouseClicked` 现在只在
**右键**（`event.button() == 1`）时才 `setDragging(true)`，`mouseDragged` 也只转发右键拖动——
框架层面的"拖动"已经变成了右键拖拽的概念，左键拖动不再有框架级支持。

我们的滚动条是纯辅助类、由 Screen 转发事件，所以能用左键拖；但**必须正好点中那 14px 的
手柄**才能开始拖——点在轨道上只会"翻一屏"，按住拖没有任何反应。这就是"只能滚轮"的体感。

修法照 RS2 `ScrollbarWidget`：
- **按在轨道任意位置 = 直接跳到那里并进入拖动**（不再区分手柄/轨道）；
- 拖动同时挂在 `mouseDragged` 和 `mouseMoved` 两条路上——鼠标一动 `mouseMoved` 必到，
  哪条通走哪条（`KleinBottleScreen#mouseMoved` 转发给滚动条）；
- 代价：失去"点手柄上/下方翻一屏"，翻屏留给 Ctrl+滚轮和 PageUp/PageDown。

### 7.2 铁砧缝进终端（熔炉下方那块装饰的位置）

涡环装饰删了，位置给**铁砧**：目标格 + 材料格 + 产物格 + **改名输入框** + 「需要 X 级经验」。

**实现方式是"隐形铁砧"，不是重写一遍铁砧算术**：服务端持有一个不发给客户端的
`AnvilMenu` 实例（`new AnvilMenu(0, inventory)`，它自己就用 `ContainerLevelAccess.NULL` 构造）。
修理、合并、附魔书、改名、经验代价、代价上限、NeoForge 的 AnvilCraft 事件——
**全套行为和真铁砧一致，因为那几百行算术就是它自己算的**：

- 我们的输入格一变 → 镜像进 ghost 的槽位 → `SimpleContainer#setChanged` →
  `ItemCombinerMenu#slotsChanged` → `createResult()`（不用手动调）；
- 产物格的 `mayPickup` 问 ghost 的 `mayPickup`（经验够不够、代价是不是 0），
  `onTake` 调 ghost 的 `onTake`（扣经验、消耗材料、触发事件）；
  这两个方法是 protected，所以包了一层 `GhostAnvil extends AnvilMenu` 暴露出来；
- 关界面时输入格剩下的东西送回四维空间（和合成网格同款），不落地面。

改名输入框是真 `EditBox`，内容走新包 `KleinAnvilNamePacket`（C2S，最长 50 字符 = 原版上限）。
代价数值走 `ContainerData` 同步。

**为什么用 `ContainerLevelAccess.NULL` 安全**：`createResult` 不碰 access；
`onTake` 里唯一用到 access 的是"消耗铁砧方块耐久"，NULL 的 `execute` 是空操作——
我们没有方块可掉耐久，正合适。

### 7.3 三张功能卡（层次感）

右栏从"一整块面板 + 分隔线"改成**三张独立卡片**，各带一套色调：

| 卡片 | 色调 | accent |
|---|---|---|
| 合成 | 暖紫 | `(150,110,128)` |
| 熔炉 | 炉火橙 | `(188,110,62)` |
| 铁砧 | 钢灰蓝 | `(104,122,150)` |

层次感 = 底色分层 + 边框带色调 + **顶部一条亮边**（读作"标题条"），
标题（图标 + 文字）压在卡片外面上头。槽位凹槽、改名框凹槽烤在卡片里。

**删掉的东西**：右栏底部的"维度涡环"装饰（`portalRings`）——位置让给了铁砧，
连同方法一起删了。别加回来。

### 7.4 槽位区间（v7）

| 区间 | 作用 |
|---|---|
| 0..53 | 存储窗口 9×6 |
| 54..62 | 合成网格 3×3 |
| 63 | 合成产物 |
| 64 / 65 / 66 | 熔炉 输入 / 燃料 / 产物 |
| 67 / 68 / 69 | 铁砧 目标 / 材料 / 产物 |
| 70..105 | 玩家背包 27 + 快捷栏 9 |

界面 **318 × 244**。

---

## 第六版：功能按钮改真控件 + 竖排最左列

### 6.1 修了什么

第五版实机反馈：**「搜索栏下面的四个功能按钮全部未实现」，并且竖着排在左侧更好。**

- **根子出在实现方式上**：那四个按钮是 `Screen` 自己用 `fill` 画出来的方块，
  命中判定、hover、发包全是手写的，和控件框架并行。两端链路逐段核对都是通的，
  但"手画 + 手写命中"这种结构只要有一个环节（坐标、事件顺序、焦点）对不上，
  表现就是"点了没反应"，而且离机很难定位。
- **修法不是再补一笔，而是把机制换掉**：新增 `client/klein/KleinSideButton`
  （`AbstractButton` 子类），命中 / hover / 键盘触发 / 按下音效全部交给控件框架，
  `Screen` 只负责摆位置和画图标。这正是 RS2 `AbstractSideButtonWidget` 的结构
  （继承 `Button`，屏幕里 `addSideButton` 逐个往下排）。
- **竖排最左列**：v4/v5 四个按钮横排在搜索框下面，独占一行（约 18px 高）。
  竖排到存储网格左边那一列之后，**那一行整个还给了网格**，整张图矮了 18px
  （262 → 244）；代价是加宽 20px（298 → 318）。
- 顺带删掉了横排时代那条"排序：录入顺序 ↑ 6/7"状态文字——排序状态现在
  就在按钮的悬浮提示里（RS2 也是把状态放进按钮 subtext），少一处要同步的东西。
- 顺带修了一个相关的健壮性问题：客户端有个"回声抑制"（自己发滚动请求后 20 tick
  内不信任服务端回传的行号），**按按钮触发的服务端改滚动位置会被它吞掉**，
  表现是「回到顶部」最多要等 1 秒才生效。现在按按钮时先清掉等待窗口。

### 6.2 为什么不在那一列放"轨道状"的东西

物品栏右侧那一竖条是 ∞（`KleinTheme#infinityFlow`）。那里早先是"维度刻度"装饰，
因为紧贴物品栏、看起来像第二根滚动条被砍掉了。所以**侧边按钮列放在最左**，
不和 ∞ 同列，也不做成轨道状——避免再造一次"这是不是第二根滚动条"的误会。

### 6.3 版面（v6）

界面 **318 × 244**，槽位区间不变：
0..53 存储 / 54..62 合成 / 63 合成产物 / 64-66 熔炉 / 67..102 背包。

| 位置 | 内容 |
|---|---|
| x=8（最左列） | 四个功能按钮竖排：回到顶部 / 排序方式 / 排序方向 / 清空搜索 |
| x=30，y=4 | 标题（左）+ 种类/总数统计（右） |
| x=30，y=17 | 搜索框 |
| x=30，y=34 | 9×6 存储网格 + 右侧滚动条（x=196） |
| x=196，y=146 | 无限符号 ∞ |
| x=208..310 | 右栏：合成（上）+ 熔炉（下）+ 涡环装饰 |

---

## 第五版：取出手感、紧凑标题、随身熔炉、无限符号

### 5.1 四条反馈

第四版实机截图之后的反馈：

1. **"右键拿一个，再右键一下再拿一个，以此类推，就像原版箱子的逻辑。"**
   第四版的右键是"取一个"，但光标上有了 1 个之后再右键就变成"放回一个"
   （原版语义），于是永远攒不起来。**这条是纯 bug。**
2. **"顶部显示种类和四维空间的标题和动效占比太大。"**
   标题左边那枚会转的克莱因环徽记 + 独占一行的标题 + 右对齐的统计，
   把顶部撑成了三行高。
3. **"工作台下面不需要操作说明，可以改成熔炉界面，拥有和熔炉一样的功能。"**
   （第四版之前那版做过"放进去就自动烧"的开关，但那不是熔炉。）
4. **"原本玩家物品栏右侧被删除的滚轮条可以改成无限符号的动向，以此让 UI 显得更加协调。"**

### 5.2 改了什么

| 反馈 | 落地 |
|---|---|
| 右键攒不起来 | `storagePickup` 拆出 `storagePickupOne`：**只要光标还没满，右键就继续拿一个**；满了才退化回"放回一个" |
| 标题太占地方 | 删掉徽记（`EMBLEM_*`、`drawHeaderEmblem`），标题与统计并到**同一行** y=4；搜索行 23→17、按钮行 39→33、网格 56→52，整图 266→**262** |
| 工作台下面是空话 | 加**真熔炉**：输入/燃料/产物三格 + 炉火 + 进度条；操作说明整段删除 |
| 物品栏右侧空着 | 放一个**竖放的 ∞**（`KleinTheme#infinityFlow`），一个光点顺着双纽线往下淌 |

### 5.3 随身熔炉（`item/KleinFurnace.java`）

**它是真熔炉，不是"一键烧"。** 要放燃料、火会烧完、输入一格一格变产物、产物满了就停。

- 状态 = 3 个 `ItemStack` + `burnTime/burnDuration/cookTime/cookDuration` + `pendingExperience`，
  继承 `SimpleContainer` 并**跟着 `FourDimensionalSpace` 一起存档**（`output.child("Furnace")`）。
  放进菜单里就"关掉界面不烧了"，那不叫熔炉。
- 推进挂在 `KleinBottleItem#inventoryTick`。26.3 的 `Item#inventoryTick(ItemStack, ServerLevel, Entity, EquipmentSlot)`
  **只在服务端调**（`ItemStack#inventoryTick` 里有 `level instanceof ServerLevel`），所以不用再判 `isClientSide`。
  熔炉内部有"完全空闲就早退"，没东西在烧时开销可忽略。
- **燃料 API 和老版本完全不同**：
  - 是不是燃料 = `stack.has(DataComponents.COOKING_FUEL)`（= 原版 `AbstractFurnaceMenu#isFuel`）；
  - 烧多久 = 那个组件里的 `ResolvableInt burnTime`：常量直接读值，
    注册表引用要用 `LootContext` 解析（`new LootParams.Builder(level).create(ContextKeySet.EMPTY)`
    → `new LootContext.Builder(params).create(Optional.empty())`）。
- **容器残留物**：26.3 没有 `hasCraftingRemainingItem`，统一走 `DataComponents.USE_REMAINDER`
  → `remainder.convertInto().create()`。熔岩桶烧完退回空桶靠的就是它。
- 进度同步走**原版 ContainerData 通道**：`SimpleContainerData(4)` + `addDataSlots(...)`，
  `broadcastChanges()` 里把四个值写进去，`AbstractContainerMenu` 自己会发
  `ClientboundContainerSetDataPacket`。界面照 `FurnaceScreen` 的语义读
  （0=剩余燃料，1=燃料总量，2=熔炼进度，3=熔炼总时长）。
- 经验：烧好时累加进 `pendingExperience`，**玩家把产物拿走时才结算**
  （`FurnaceOutputSlot#onTake`）——和原版熔炉一致。
- 26.3 还顺手踩到两个：`ItemStack#onCraftedBy(Player, int)` 只剩 2 参（老的带 Level 的没了）。

### 5.4 无限符号

那一竖条（x=172..186，y=164..254）在第四版是空的——第三版那里是"维度刻度"装饰，
因为紧贴物品栏、看起来像第二根滚动条，被整条砍掉了。

现在放 ∞：Gerono 型双纽线竖放 `x = a·sin t·cos t, y = -b·cos t`，
x 的幅度取 `w-2`（因为 `sin·cos` 峰值只有 0.5，取 `w/2` 会瘦成一根线），
再让一个光点带 7 帧拖尾沿曲线往下淌。它对圆那根真滚动条没有语义冲突，
而且说的正是"这个仓库没有底"。

### 5.5 布局常量一致性

布局数字散在 Java（建槽 + 绘制）和 `tools/gen_klein_terminal_gui.py`（烤底图）两处，
错一个只有进游戏才看得出来。新增 **`tools/check_layout_constants.py`**：
以 Java 为基准解析全部 `public static final int`（支持常量互相引用），
和 Python 模块属性逐个比对，47 个常量一次跑完。

```bash
C:/Users/lkuvi/.workbuddy/binaries/python/versions/3.13.12/python.exe tools/check_layout_constants.py
```

### 5.6 槽位区间（第五版）

| 区间 | 作用 |
|---|---|
| 0..53 | 存储窗口 9×6 |
| 54..62 | 合成网格 3×3 |
| 63 | 合成产物 |
| 64 / 65 / 66 | 熔炉 输入 / 燃料 / 产物 |
| 67..102 | 玩家背包 27 + 快捷栏 9 |

界面 **298×262**。

---

## 第四版：一格一种物品 + 内嵌工作台

### 4.1 三个改动的起因

第三版实机截图暴露了三件事：

1. **"总量叠字有了，但同一种东西还是铺满好几页。"**
   第三版是"每个条目 = 一个普通 ItemStack（≤64）"，所以 3300 个石头摊成 52 格，
   界面上满屏同一种方块，右下角都写着 3.3k。数量看得见了，格子数量的问题没解决。
2. **"下方玩家物品栏都显示全了，右边那个滚轮是干嘛的？"**
   网格右边一根真滚动条、物品栏右边还有一条青色"维度刻度"装饰，
   两条竖条并排，看起来像"物品栏也有滚动条"。
3. **"工作台和熔炉的独立 UI 呢？合成终端的合成区不该要点按钮才出来。"**

### 4.2 改法

| 问题 | 做法 |
|---|---|
| 一格一种物品 | 存储模型从"条目 = ItemStack"改成 **`(模板 ItemStack, 总量 long)`**。同一种东西**永远只有一条**，3300 个石头就是一格。 |
| 两条竖条 | **删掉"维度刻度"**装饰。右栏改成合成区，界面右侧只剩网格旁那一根真滚动条。 |
| 工作台 | **3×3 + 产物槽直接嵌进终端右栏**，不再有独立界面、不再需要点按钮切换。JEI 的 + 直接往这个网格里填料。 |
| 熔炉 | **整个功能删掉**（自动熔炼、熔炼按钮、`processAutoSmelt`、lang 文案全部移除）。 |

### 4.3 为什么总量必须和 ItemStack 分家

26.x 的 `ItemStack` 会校验"堆叠数不能超过该物品上限"——项目日志里有过实例：

```
[WARN] Can't create item stack with properties ItemStackTemplate[...], error:
       Item stack with stack size of N was larger than maximum: 1
```

所以"一格 3300 个"**不能**把 3300 塞进 `ItemStack.count`。做法是：

- 条目里存 `template`（count 恒为 1）和 `total`（long，权威总数）；
- 给原版/界面看的 `display(i)` 是把总量夹到堆叠上限的**展示栈**；
- **取放必须自己接管**。原版 `Slot.remove(n)` 只认那个夹到 64 的展示栈，
  玩家取走一组它就会认为"这格空了"，剩下的 3236 个全没。所以窗口槽位
  `StorageSlot` 的 `mayPlace`/`mayPickup` 都关掉，取放全部在 `KleinBottleMenu#clicked` 里
  按"从 `total` 里扣 / 往 `total` 里加"实现。

### 4.4 操作（第四版）

| 操作 | 行为 |
|---|---|
| 左键条目 | 取一组（该物品的堆叠上限）到光标 |
| 右键条目 | 取一个 |
| 光标拿着同类东西点条目 | 直接并进那一格（**不限量**） |
| 光标拿着别的东西点条目 | 收进四维空间（同种自动合并） |
| Shift + 左键 | 反复搬整组进背包，直到背包塞不下或这一格取空 |
| 数字键 | 快捷栏那一格 ⇄ 这一格（快捷栏里的先收进空间） |
| Q / Ctrl+Q | 丢一个 / 丢一组 |
| 滚轮 / 拖滚动条 | 视图滚动（一行；Ctrl 一屏） |
| PageUp / PageDown / Home / End | 翻屏 / 到顶 / 到底 |
| F | 聚焦搜索框（Esc 退出搜索框，再按一次关界面） |
| JEI 配方上的 + | 材料从四维空间直接填进右栏的 3×3 |

工具按钮从 6 个减到 **4 个**：回到顶部 / 排序方式 / 排序方向 / 清空搜索。
原来的「整理」退休了——一格一种之后同种合并由模型天然保证，那个按钮会变成点一下什么也不发生的废键。

### 4.5 版面

```
┌──────────────────────────────────────────────┬──────────────────┐
│ (克莱因环) 四维空间        57 种 · 3,587 个   │  ⊞ 合成          │
│ ┌ ⌕ 搜索物品 · @模组 · -排除 ┐                │ ┌──┬──┬──┐       │
│ └───────────────────────────┘                │ │  │  │  │      │
│ [⤒][序][↓][×]    录入顺序 ↑ 6/7               │ ├──┼──┼──┤  → ┌┐│
│ ┌──────────────────────────────┐ ┌▮┐          │ │  │  │  │    └┘│
│ │      9 × 6 存储视图窗口       │ │ │          │ ├──┼──┼──┤       │
│ └──────────────────────────────┘ └▯┘          │ │  │  │  │       │
│ ──────────────────────────────────────────    │ ─────────        │
│ 物品栏                                        │ 操作 / 说明       │
│ [ 背包 3 行 + 快捷栏 ]                        │ (维度涡环)        │
└──────────────────────────────────────────────┴──────────────────┘
```

槽位区间（**两侧数量必须完全一致**，原版靠序号同步）：

| 序号 | 区间 | 作用 |
|---|---|---|
| 0..53 | 存储窗口 | 9×6 视图窗口 |
| 54..62 | 合成网格 | 3×3，内嵌 |
| 63 | 产物槽 | 原版 `ResultSlot` |
| 64..99 | 玩家背包 | 27 格 + 9 格快捷栏 |

界面尺寸 **298×266**（宽是因为右栏多了 102px 的合成区；高度刻意不涨——
高 GUI 缩放档下高度才是稀缺资源）。

### 4.6 存档迁移

存储的序列化格式换成了 `Entries`（`(item, count)` 列表）。
读档时如果发现旧的 `Items`（每格一个 ItemStack 的列表），会**按种类合并进新格式**，
并打一条日志，免得升级模组之后一仓库东西全没了。

### 4.7 已知取舍

1. **一格的后备总量是 long，但同步给客户端的叠字截到 2^31**。
   真实场景到不了，只是防止溢出把数字画成负数。
2. **搜索/排序与点击之间存在极小概率的竞态**：两者分属不同包，
   若在同一个 tick 内"先到视图变更包、后到点击包"，这一次点击可能落在变更后的那一格上。
   后果是"拿到的东西不是刚看到的那件"，可恢复（放回去即可），不丢东西。
3. **拖拽涂色不能把光标上的东西刷进已有条目**：`canItemQuickReplace` 判定"槽位已满"就跳过。
   用左键点一下即可（不限量）。
4. 退休的两个文件（独立工作台菜单/界面）留在 `docs/removed_code/`，没有删。

### 4.8 26.3 API 备忘（本轮新增）

- ⚠️ **`Player#drop` 多了一个参数**：`drop(ItemStack, boolean thrownFromHand, Prediction)`，
  `Prediction.SERVER_ONLY` 用于服务端丢弃。写两个参数会直接编译失败。
- ⚠️ **写回虚拟容器必须覆写 `Slot#setChanged()`**，不能挂容器 `setItem()`：
  原版 `AbstractContainerMenu#moveItemStackTo` 合并同类物品时是**就地改**
  `slot.getItem()` 的 count、然后只调 `slot.setChanged()`，容器那条路不会被回调。
- **`Slot.mayPlace/mayPickup` 返回 false 是很有用的"防误伤开关"**：
  原版拖拽涂色（QUICK_CRAFT）收集目标槽时要过 `mayPlace`，
  关掉它就等于"任何原版路径都碰不到这些槽"，实现完全自定义的取放时可以放心。
- `ContainerInput` 的取值：`PICKUP / QUICK_MOVE / SWAP / CLONE / THROW / QUICK_CRAFT / PICKUP_ALL`；
  覆写 `AbstractContainerMenu#clicked(int, int, ContainerInput, Player)`（public）即可拦截全部点击语义。
- `ValueOutput` **只有 `putIntArray`，没有 long 数组**；要存 long 用
  `RecordCodecBuilder` 拼一个 record 的 `Codec` 再 `output.store(name, CODEC.listOf(), list)`。

---

## 第三版：终端 UI（历史）

### 0.1 为什么重做

第二版把存储搬到玩家附件上、界面改成自绘底图之后，剩下最大的问题是**交互太原始**：
翻页只能点 ◀▶ 或滚轮"整页跳"，一仓库东西没有搜索、没有排序、看不到某个物品的**总量**
（同种东西散在多个条目里时，槽位只显示那一个条目的 ≤64）。这些都是主流存储终端早已解决的事，
所以这一版直接对着 **Applied Energistics 2** 与 **Refined Storage 2** 的终端架构重写。

参考到的东西（逐条对应到本项目的实现）：

| 参考 | 出处 | 本项目的对应 |
|---|---|---|
| 客户端持有一份存储视图（`Repo` / `IClientRepo`） | AE2 `client/gui/me/common/Repo.java` | `client/ClientKleinBottleView` |
| 总量单独同步（`GridInventoryEntry` / `ResourceAmount`） | AE2 / RS2 | `KleinBottleSyncPacket.windowStats` |
| 视图 = 过滤 + 排序后的仓储下标数组（`ResourceRepository.setFilterAndSort`） | RS2 `AbstractGridContainerMenu` | `FourDimensionalSpace#view` |
| 排序方式 / 方向 / 过滤（`GridSortingTypes` + `SortingDirection`） | RS2 | `SortMode` + `descending` |
| 以"行"为单位的滚动条（`Scrollbar#setRange(min,max,pageSize)`） | AE2 `client/gui/widgets/Scrollbar.java` | `client/klein/KleinScrollbar` |
| 点手柄上下方翻屏、拖手柄连续滚、滚轮滚一行 | AE2 / RS2 | 同上 |
| 槽位右下角缩放叠字显示总量（`StackSizeRenderer`） | AE2 | `client/klein/KleinAmountRenderer` |
| 终端槽位自己画、**不调 super** 以免和原版堆叠数字打架（`RepoSlot` + `extractSlot` 提前 return） | AE2 `MEStorageScreen#extractSlot` | `KleinBottleScreen#drawStorageSlot` |
| 搜索框带占位提示、右键清空、可失焦 | RS2 `SearchFieldWidget` | `client/klein/KleinSearchBox` |
| 侧边竖排图标按钮 + hover 提示 | RS2 `AbstractSideButtonWidget` | `KleinTerminalLayout` + `KleinTheme.buttonFrame` |

### 0.2 界面与操作

```
┌──────────────────────────────────────────────────────┐
│ (克莱因环)  四维空间              1,234 项 · 5,678 个 │  标题 + 视图统计
│ ┌ ⌕ 搜索物品 · @模组 · -排除 ────────────┐            │
│ └────────────────────────────────────────┘            │  搜索（200ms 去抖）
│ [台][火][并][序][↓][×]    排序：数量 ↓ · 行 1/3        │  6 个工具按钮 + 状态
│ ┌──────────────────────────────────┐ ┌▮┐              │
│ │        9 × 6 视图窗口            │ │ │              │  可滚动的存储窗口
│ └──────────────────────────────────┘ └▯┘              │
│ ────────────────────────────────────────────          │
│ 背包 / 快捷栏                                          │
│                                            ╷╷╷         │  右侧"维度刻度"动画
└──────────────────────────────────────────────────────┘
```

| 操作 | 行为 |
|---|---|
| 左键条目 | 拿起该条目整组（≤ 堆叠上限） |
| 右键条目 | 拿起一半 |
| Shift + 左键 | 存储 → 背包；背包 → 存储走 `insert`（**不经过原版搬运**，见 0.4） |
| 滚轮 | 视图滚一行；**Ctrl + 滚轮** 滚一屏 |
| 拖滚动条 | 连续滚动；点手柄上/下方 翻一屏 |
| PageUp / PageDown / Home / End | 翻屏 / 到顶 / 到底 |
| F | 聚焦搜索框（Esc 退出搜索框，再按一次关界面） |
| 工具按钮 | 工作台 / 自动熔炼开关 / 整理 / 排序方式 / 排序方向 / 清空搜索 |
| 搜索语法 | 空格分隔为 AND；`@minecraft` 按模组；`-石头` 排除 |

### 0.3 视觉：怎么表达"无限"和"四维"

| 元素 | 位置 | 手法 |
|---|---|---|
| 克莱因环徽记 | 标题左侧 | 半径按 `1 + 0.22·sin(2θ + t)` 调制 + 一条横穿环内的金色瓶颈弧 → 2D 圆环被读成"自己穿过自己"的曲面 |
| 维度刻度 | 网格右侧竖条 | 刻度明暗错相呼吸 + 三个光点循环下落，暗示"这 6 行只是四维空间的一个剖面" |
| 超立方角标 | 每个存储槽 | 外环四角点青色 L，**背包槽不点** → 一眼分出"四维空间"与"背包" |
| 数量辉光 | 槽位右下 | 总量破千转青、破百万纯青，并加一层呼吸外发光（`amountColor` / `drawGlow`） |
| 嵌套方框 | 面板 / 控制区 / 槽位 | 同一"方框套方框"在三个尺度上重复 = 超立方切面 |
| 斜向扫光 | 网格带上 | 7.5 秒一轮、alpha ≤ 0.075 的青色光带，让整片槽位"活"起来但不压过物品 |

物品图标（16×16）也重画了：**瓶颈从右上下来、瓶口在瓶身内部张开**（这就是克莱因瓶的自交），
内部是一条贯通的暗腔，自交处用 2–3 个青色像素描边。明度分 5 阶、光从左上，不做自动描边。

### 0.4 三个必须守住的坑（都是真踩过的）

1. **写回要挂在 `Slot#setChanged()`，不能挂在容器 `setItem()` 上。**
   原版 `AbstractContainerMenu#moveItemStackTo` 合并同类物品时是**就地改** `slot.getItem()` 的
   `count`、然后只调 `slot.setChanged()`——容器那条路根本不会被回调。所以窗口槽位必须是自研的
   `KleinBottleMenu.WindowSlot`（`KleinBottleCraftingMenu.SpaceSlot` 同理）。
   第二版把 `setChanged()` 置空是为了躲递归，副作用就是"shift-click 合并静默失效"。

2. **空档不能留、但也不能马上挪。**
   一次 shift-click 会在同一 tick 里连续写好几个槽。若写入时直接"删掉空条目"，`items` 会左移、
   视图下标立刻全部错位，后续写入就打错条目（真丢东西）。
   所以 `setStable()` 只**置空不挪列表**，等这一 tick 交互全部结束后由
   `broadcastChanges()` 里的 `settle()` 一次性收紧并重建视图。空档最多存在一个 tick。
   `take()` 与 `processAutoSmelt()` 也遵守这条：只置 `dirty`，不自己重建视图。

3. **客户端绝不能装填窗口。**
   客户端没有 `space`（数据在服务端附件上），装填会把原版刚同步下来的内容清空。
   所有 `refreshWindow()` 都带 `space != null` 判断。

### 0.5 视图为什么放在服务端

AE2 把整份物品清单同步给客户端、过滤排序都在客户端做，滚动是零延迟的。
本项目**没走这条路**：存储挂在玩家附件上、条目数不设上限，全量同步不划算。
所以过滤 / 排序 / 滚动都是"客户端提交意图 → 服务端算好 → 槽位同步 + `KleinBottleSyncPacket` 回传"。
代价是一次往返的延迟（局域网 1 帧、线上 50–100ms），换来的是**零失同步风险**
（客户端和服务端的窗口内容永远由同一份数据决定）。滚动条做了乐观更新 + 回声校验
（`pendingScrollRow` / `scrollEchoTicks`），拖起来不会"弹回去"。

### 0.6 第三版文件清单

**新增**

| 文件 | 作用 |
|---|---|
| `util/KleinTerminalLayout.java` | 布局常量（服务端建槽位、客户端画界面、Python 出底图**三处共用**） |
| `client/ClientKleinBottleView.java` | 客户端视图快照（对应 AE2 的 `Repo`） |
| `client/klein/KleinTheme.java` | 配色 + 全部运行时绘制的装饰与图标 |
| `client/klein/KleinScrollbar.java` | 以行为单位的滚动条（纯辅助类，由 Screen 转发鼠标事件） |
| `client/klein/KleinSearchBox.java` | 搜索框（去边框、放大点击热区、右键清空） |
| `client/klein/KleinAmountRenderer.java` | 槽位右下角缩放叠字 + 辉光（AE2 `StackSizeRenderer`） |
| `client/klein/AmountFormat.java` | `1.2k` / `3.4M` 与带千分位的完整数字 |
| `network/KleinBottleViewPacket.java` | C2S：搜索词 + 滚动位置 |
| `network/KleinBottleSyncPacket.java` | S2C：视图统计 + 每格 [总量, 条目数] |
| `tools/gen_klein_terminal_gui.py` | 底图生成（190×266） |
| `tools/gen_klein_bottle_texture.py` | 物品图标生成（16×16，重写） |

**改动**：`item/FourDimensionalSpace.java`（视图模型 + 总量聚合 + `setStable`/`settle`）、
`menu/KleinBottleMenu.java`（重写）、`menu/KleinBottleCraftingMenu.java`（改走视图下标 + `SpaceSlot`）、
`client/KleinBottleScreen.java`（重写）、`network/PacketHandler.java`、两套 lang、两份贴图。

**已废弃但保留**：`textures/gui/four_dimensional_space.png`（第二版的底图，第三版改用
`klein_terminal.png`）。`four_dimensional_crafting.png` 仍在使用。

### 0.7 26.3 界面 API 备忘（写这一版时确认过的）

- `extractBackground` 里是**屏幕绝对坐标**（加 `leftPos/topPos`）；
  `extractLabels` / `extractSlot` / 原版高亮都在 `translate(leftPos, topPos)` **之后**调用，
  用界面局部坐标。所以 `extractContents` 里 `super` 返回后矩阵已弹回，之后画的滚动条要按绝对坐标来。
- `extractLabels` 默认色 `0xFF404040`，深色底上必须覆写且**不要调 super**。
- `KeyEvent#key()` 是 **`InputConstants` 那套 SDL scancode**（`KEY_A = 4`），不是 GLFW key。
  比较请用 `InputConstants.KEY_*`；Esc / 回车用语义化的 `event.isEscape()` / `event.isConfirmation()`。
- `MouseButtonEvent(event.x(), event.y(), buttonInfo)`；`mouseScrolled(x, y, scrollX, scrollY)`。
- 原版拖拽只转发给**当前获得焦点的子控件**（`ContainerEventHandler#mouseDragged`），
  所以滚动条做成纯辅助类由 Screen 转发，免得焦点被搜索框抢走时拖拽断掉。
- `imageWidth` / `imageHeight` 是 **final**，界面高度不能在 `init()` 里改 →
  行数固定 6（AE2 的 ME 终端同样是固定 9×6）。

---

## 0. 第二版改了什么（历史）

| | 第一版 | **第二版（当前）** |
|---|---|---|
| 存储挂钩对象 | **物品 NBT**（跟着瓶子走） | **玩家附件**（跟着人走） |
| 名字 | 克莱因瓶（容器） | 存储空间统一叫 **「四维空间」**，瓶子只是钥匙 |
| 丢了瓶子 | 一仓库东西陪葬 | **东西都还在**，换一只瓶子照样开 |
| 死亡 | 随物品掉落 | `copyOnDeath()` → **不掉落** |
| 界面 | 原版 `generic_54` 底图 | **自绘终端底图**（标题条 + 容量读数 + 按钮行 + 网格），布局参考主流终端存储模组 |
| 功能按钮 | 工作台 / 熔炉 | 工作台 / 熔炉 / **整理**（一键同种合并去空档） |
| 打开方式 | 需要把"是哪只瓶子"同步给客户端 | 不再需要——数据源是玩家自己 |

**为什么改**：第一版的存储跟着**物品**走，于是"随身终端"变成了"随身箱子"——瓶子一丢全没了，
想换一只还得手动搬。改成挂在玩家身上之后，克莱因瓶退化成纯钥匙，语义才对得上
"四维空间是随身维度，不是背包里的一个格子"。

**技术要点**：NeoForge **attachment**（`NeoForgeRegistries.Keys.ATTACHMENT_TYPES`）+
`AttachmentType.builder(...).serialize(IAttachmentSerializer).copyOnDeath().build()`。
序列化用 `ContainerHelper.saveAllItems(ValueOutput, NonNullList)` 加上一个 int 数组（每格真实总数）。
副作用是**客户端不再需要任何附加数据同步机制**，`IMenuTypeExtension` 那套可以整个去掉。

---

## 1. 定位（第一版原文，仍然成立）

> **一个内外不分的容器：54 格存储、单格 100 万、同种物品自动并成一格，还自带工作台与无燃料熔炉。**

克莱因瓶在拓扑上是个"没有内外之分"的曲面，所以它作为容器天然成立——**它装的是它自己**。
这为"近乎无限的随身仓库"提供了设定依据，而不是简单粗暴地给个无限背包。

## 2. 三条支柱

| # | 支柱 | 过审用法 |
|---|---|---|
| P1 | 同种合并，不是格子多 | 与"大箱子"区分开：新物品自动并进已有的同种格，槽位右下角叠字显示真实总数（AE 观感） |
| P2 | 随身终端，不是背包 | 手持右键或背包里按 K 都能召出；**只有绑定者本人能存取** |
| P3 | 数据随物品走 | 全部状态存在物品 NBT 里，没有 BlockEntity——丢了、扔岩浆里就是真没了，符合克莱因瓶的"自我包含"设定 |

## 3. 规格

| 项 | 值 |
|---|---|
| 物品 id | `zuoyanmod:klein_bottle` |
| 稀有度 / 堆叠 | EPIC / 1 |
| 存储格数 | **54**（9×6） |
| 单格上限 | **1,000,000** |
| 打开方式 | ①手持右键 ②**背包里按 K** |
| 归属 | 首次打开绑定 UUID，之后只有本人能开（他人打开提示"这不是你的克莱因瓶"） |
| 内置工作台 | 终端里点「工作台」→ 复用原版 3×3 合成 |
| 内置熔炉 | 点「熔炉」→ **无燃料槽、不需要燃料**，放进去就烧 |
| 熔炉速度 | 5 秒一件（原版 10 秒的一半） |
| 熔炉特性 | **装在背包里也会继续烧**（挂在 `inventoryTick` 上，不要求开着界面） |

## 4. 关键实现决策（这部分比功能本身更重要）

### 4.1 为什么数量不能存在 `ItemStack.count` 里

26.x 的 `ItemStack` 会校验数量不超过该物品的最大堆叠数。项目日志里就有现成的例子：

```
[WARN] Can't create item stack with properties ItemStackTemplate[... hallowed_upgrade_smithing_template,
count=2, components={}], error: Item stack with stack size of 2 was larger than maximum: 1
```

所以"一格放 100 万块石头"**不能**靠把它塞进一个 `ItemStack`。本实现的做法是：

- `KleinBottleContainer.amounts[]` 是**权威数量**，单独存进 NBT（NBT 键 `KleinAmounts`，一个 int 数组）；
- `getItem(slot)` 只返回一个"展示用"的 `ItemStack`，数量夹到该物品的最大堆叠数——
  这样**原版 `Slot` 的取放逻辑一行都不用改写**就能工作（一次最多拿一组）；
- 真实总量走 `ContainerData` 单独同步给客户端，由界面在槽位右下角叠字显示（`1.2k` / `3.4M`）。

**代价（已知且接受）**：从格子里取东西一次最多一组（64），想整格取出得点几次。
这恰好也是存储类模组的通行手感，不是缺陷。

### 4.2 为什么客户端和服务端用的容器不一样

存储的权威数据在**物品自己的 NBT** 里，而物品 NBT 不会被容器协议自动同步。所以：

| 侧 | 容器 | 数据来源 |
|---|---|---|
| 服务端 | `KleinBottleContainer`（NBT 权威） | 物品 NBT |
| 客户端 | 平铺的 `SimpleContainer(54)` | 原版槽位同步包 |

"每格真实总数"两侧都读 `ContainerData`，所以客户端也能正确叠字。

### 4.3 熔炉为什么能"关着界面也烧"

烧制逻辑不在 Menu 里，而挂在 `Item.inventoryTick` 上——物品在背包里每 tick 都会被调用一次。
Menu 只负责取放与显示，进度靠覆写 `AbstractContainerMenu.broadcastChanges()`（服务端每 tick 一次）
从瓶子 NBT 刷进 `ContainerData` 再走原版同步。

### 4.4 打开菜单怎么告诉客户端"是哪一只瓶子"

用 NeoForge 的 `IPlayerExtension.openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)`
把**背包槽位号**写进附加数据，客户端用 `IMenuTypeExtension` 的扩展工厂读回来，
再从自己背包里取同一个 `ItemStack` 实例。槽位号用引用相等（`getItem(i) == bottle`）定位，
不依赖任何可能被重命名的 `Inventory` 内部字段。

## 5. 文件清单

**新增**

| 文件 | 作用 |
|---|---|
| `item/KleinBottleItem.java` | 道具：打开逻辑、所有权绑定、熔炉 tick、tooltip |
| `item/KleinBottleContainer.java` | AE 式 NBT 权威存储（核心） |
| `menu/KleinBottleMenu.java` | 存储终端菜单（54 + 36 + 两个按钮） |
| `menu/KleinBottleFurnaceMenu.java` | 无燃料熔炉菜单 |
| `client/KleinBottleScreen.java` | 终端界面（原版 generic_54 底图 + 数量叠字 + 按钮） |
| `client/KleinBottleFurnaceScreen.java` | 熔炉界面（原版 furnace 底图） |
| `network/OpenKleinBottlePacket.java` | K 键 → 服务端开界面 |
| `event/KleinBottleKeyHandler.java` | K 键注册与触发 |
| `tools/gen_klein_bottle_texture.py` | 物品图标（16×16） |
| `tools/gen_klein_bottle_sources.py` / `gen_klein_bottle_patches.py` | 生成/接线脚本（一次性，保留备查） |

**改动**：`item/ItemRegistry.java`、`item/CreativeTabRegistry.java`、`menu/MenuRegistry.java`、
`network/PacketHandler.java`、`Zuoyanmod.java`（界面注册）、两套 lang、物品定义与模型。

## 6. 待决策 / 已知取舍

1. **界面用的是原版底图**（`generic_54` / `furnace`），没有自绘。
   好处是零贴图工作、观感原生；如果要做成和虚空共振泵一样的定制皮肤，说一声，照那套脚本再来一次即可。
2. **熔炉界面没画进度箭头**。进度值已经同步到 `ContainerData`（`getProgressPercent()`），
   只是第一版没画——补几行 `fill` 就行。
3. **单格 100 万是否合适**：`KleinBottleContainer.MAX_PER_SLOT` 一个常量。
4. **工作台/熔炉的产物不会自动进存储区**：它们走的是原版界面，取回要手动。
   如果想让产物直接落进瓶子，需要另行处理（工作量不小）。
5. **所有权不可转移**：绑定后无法解绑。若以后要做"转赠"，需要一个解除绑定的手段（例如潜行右键）。
6. **多只瓶子**：K 键按"主手 → 副手 → 背包顺序"取第一只；持有多只时不会弹选择菜单。

## 7. Playtest 清单

1. 创造栏取「克莱因瓶」→ tooltip 显示随身终端 / 54 格 / 单格 100 万 / 归属
2. 手持右键 → 打开终端界面，两个按钮在位
3. 丢 1000 块石头进去 → **只占一格**，右下角显示 `1.0k`
4. 再丢 500 块石头 → 同一格显示 `1.5k`（**没有占用新格**）
5. 从那一格往外拖 → 一次拿走 64，数字随之减少；反复拖直到清空
6. 放进背包（不手持）按 **K** → 同样打开终端
7. 第二只瓶子 / 另一个玩家打开 → 被拒绝，提示归属
8. 点「工作台」→ 出原版 3×3 合成，能正常合成并取回
9. 点「熔炉」→ 放入生铁 → **不放任何燃料**，5 秒后产出铁锭
10. 熔炉里放好东西后**关掉界面**，在背包里等一会儿 → 再打开，确认已经烧好（关着界面也在烧）
11. 把瓶子丢进岩浆 → 里面的东西一起没了（设计内）
12. 退出重进 → 存储内容与归属都还在（NBT 持久化回归）
