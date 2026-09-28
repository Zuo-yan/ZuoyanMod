# 瑞克交易系统（Rick Trading）

状态：**已实装** · 2026-09-24
环境：NeoForge 26.3.0.3-beta / Minecraft 1.26.3 · modid `zuoyanmod`

---

## 1. 结论先说

| 项 | 值 |
|---|---|
| 谁卖 | 任意瑞克（`zuoyanmod:rick`），右键打开 |
| 界面 | 原版村民交易界面（`MerchantMenu` / `MerchantScreen`） |
| 次数 | 无限次，不设库存、不补货、不涨价 |

**目前三笔买卖**（按界面顺序）：

| # | 收 | 换到 | 定位 |
|---|---|---|---|
| 1 | 2 反物质微粒 + 1 下界合金锭 | **10 反物质子弹** | 弹药（消耗品，卖`量`） |
| 2 | 3 原始黑洞 + 42 反物质微粒 | **1 因果律手枪** | 武器本体（补上它唯一的来源） |
| 3 | 1 超流体暗物质 + 8 金苹果 | **1 经验修补附魔书** | 用自家产业链换掉原版的 RNG 磨人 |

> 价格槽只有两个（`MerchantOffer` 的结构是"价格 A + 价格 B + 产物"），所以每笔最多收两种东西。

## 2. 为什么是"瑞克卖子弹"

这次改动实际解决的问题不是"给瑞克加功能"，而是**补一个孤儿物品**：

> `zuoyanmod:antimatter_bullet`（反物质子弹）在本次改动之前**没有任何获取途径**。
> 全项目检索下来，它只出现在创造模式物品栏和因果律手枪的找弹逻辑里。
> 也就是说生存模式下这把枪等于没有弹药 —— `docs/item_acquisition.md` 的 31 件清单
> 是加反物质链之前的版本，这次一并补记。

那么为什么是交易而不是配方？三条理由：

1. **它是消耗品，不是装备。** 每开一枪扣一发，做成工作台配方就是"无限量印钞机"，
   因果律手枪会失去它的稀缺感。交易能给它配一个**可调节的汇率**，而且汇率是集中在一处改的。
2. **瑞克是这个 mod 唯一一个"常驻、可再生、非敌对"的 NPC。** 它无限复活、玩家跑远也不消失
   （`removeWhenFarAway → false`）。挂别的载体都不合适 —— 原版怪没有撑得起终局弹药的，
   宝箱又是纯 RNG。
3. **收的这两样东西本身就是"两条链的产出"**：
   - 反物质微粒 = 微型强子对撞机的产出（1 潮涌核心 + 1 烈焰棒 → 1 粒 / 8 秒），
     成本直接压在机器运行时间上，和"暗物质粒子当货币"是同一套设计语言；
   - 下界合金锭 = 一道下界门槛，防止子弹在游戏前中期泛滥。

   也就是说：**用两条既有产业链的产物，换第三条链（因果律手枪）的弹药。** 主题闭环成立。

### 2.1 后两笔的同一套判据

判据来自 `docs/item_acquisition.md`：**交易可以卖"量"，不能卖"档"。**
三笔买卖逐条对照，**没有一笔是让玩家跳过一个本来存在的合成链**：

| 交易 | 属于哪种情况 |
|---|---|
| 反物质子弹 | **补缺口** —— 改动前它没有任何来源；做成配方又是印钞机，所以走"可调汇率"的交易 |
| 因果律手枪 | **补缺口** —— 改动前它同样没有任何来源（配方目录与战利品表里都搜不到），这笔交易就是它**唯一**的获取途径。价格定得极贵（3 个原始黑洞 = 3 次 400 tick 对撞 + 3 个沉重核心），正是为了让"买"和"肝"等价 |
| 经验修补附魔书 | **换掉 RNG** —— 原版靠村民 / 钓鱼 / 宝箱，典型的长周期随机；这里卖的是"确定性"，收的是本 mod 暗物质链的成品 |

反过来说：**本来有工作台配方的东西，就不该出现在这张表里** —— 那才是"用绿宝石买终局力量"。


## 3. 实现方案：为什么直接复用原版交易界面

实现上让 `RickEntity` 直接 `implements net.minecraft.world.item.trading.Merchant`，
于是白嫖了原版村民交易的全部交互：

| 现成能力 | 来自哪里 |
|---|---|
| 开界面 + 把报价推给客户端 | `Merchant#openTradingScreen`（接口默认方法，内部自己开 `MerchantMenu` + `sendMerchantOffers`） |
| 界面上显示"要 2 微粒 + 1 合金锭 → 10 子弹" | `MerchantMenu` 的两个价格槽 + `MerchantResultSlot` |
| 材料不够时产物槽自动空着、交易项灰化 | `MerchantContainer#updateSellItem` |
| 点交易项从背包自动抓材料 | `MerchantMenu#tryMoveItems` |
| 关界面时把价格槽里剩的材料退回背包 | `MerchantMenu#removed` |
| 交易音效 | `Merchant#notifyTrade` / `notifyTradeUpdated` |

**代码量：`RickEntity` 里约 110 行 + `RickTrades` 约 60 行，0 个网络包、0 张 GUI 贴图、0 个新 MenuType。**
对比另一条路（自绘 GUI，见下）要新增 1 个 Menu + 1 个 Screen + 1 张贴图 + 1 个 MenuType 注册。

**代价**：界面是村民风格（左侧交易列表、边框是村庄木牌），和本 mod 暗色科幻系的
虚空共振泵 / 对撞机 GUI 不同源。考虑到只有一条交易，且买/卖关系一眼可懂，这个代价是值得的。
如果以后瑞克要卖十几样东西、需要分类标签，那时候再自绘界面也不迟 —— `Merchant` 接口不用改，
只换 Screen 即可。

## 4. 关键实现细节（踩坑记录）

### 4.1 "无限次交易"必须做两件事

原版 `MerchantOffer` 用 `uses` 累加到 `maxUses` 判定售罄：

```java
public boolean isOutOfStock() { return this.uses >= this.maxUses; }
```

而售罄会让 `MerchantContainer#updateSellItem` 不再往产物槽放东西 —— 交易直接卡死。
所以要无限次，**两处都要处理**：

| 位置 | 做法 | 为什么不能只做一处 |
|---|---|---|
| `RickTrades#UNLIMITED_USES` | `maxUses = Integer.MAX_VALUE` | 客户端拿到的报价表是服务端发的，但**成交后的 uses 累加是客户端自己算的**（`ClientSideMerchant#notifyTrade → offer.increaseUses()`）。只把服务端设成不累加的话，客户端在 N 次之后会自己灰掉交易项，两边不一致。 |
| `RickEntity#notifyTrade` | **不**调 `offer.increaseUses()` | 服务端才是权威。不累加 = 服务端永远认为可交易。 |

> `maxUses` 不能取 0 走捷径：`uses >= maxUses` 会让它一出生就是售罄。

### 4.2 激怒状态拒绝交易

`RickEntity implements NeutralMob`，`isAngry()` 是接口默认方法（读 `persistentAngerEndTime`）。
加这一道判断是为了避免"玩家一边被它追着打 42 点伤害、一边悠闲地翻它货架"的画面。
被拒绝时给一条快捷栏提示（瑞克现在是静音生物，没有音效反馈，见 §4.6）。

注意仇恨时长：被打一次记 400 tick（20 秒），复活体继承 200 tick。所以"打完架要等一会儿才能买东西"。

### 4.3 潜行右键不触发（沿用原版约定）

`player.isSecondaryUseActive()` 为真时直接走 `super.mobInteract`。
原版村民也是这个行为，好处是把"潜行交互"这个位置空出来 —— 以后要给瑞克加别的右键功能
（比如"潜行右键切换跟随/待命"）时不会和交易抢。

### 4.4 标题用实体自己的名字

`openTradingScreen(player, this.getName(), 1)` —— 不额外加 lang key，
而且玩家给瑞克改了名牌，交易界面标题会跟着变。

`level` 传 1 只是为了给原版界面一个合法的"商人等级"，配合 `showProgressBar() = false`
（瑞克不显示村民那条等级进度条）。交易不给经验：`getVillagerXp() = 0`、`overrideXp()` 空实现。

### 4.5 什么都不存盘

`tradingPlayer` 和 `offers` 都**不进存档**，所以本次改动**没有动 `addAdditionalSaveData` /
`readAdditionalSaveData`**（这一点和村民不同，村民要存 `Offers` 和补货计时）。

理由：价格固定、交易无限次 ⇒ 重载后懒建一份报价表，结果完全一致，没有跨存档状态。

### 4.6 界面"闪一下就消失"——先登记交易对象，再开界面

**这是首版实现的一个真 bug，已修。** 症状：右键瑞克，交易界面出现约 1 tick 就自己关掉。

**原因**：`Merchant#openTradingScreen` 只管开菜单，**不会**替你调 `setTradingPlayer`。
而 `MerchantMenu#stillValid → trader.stillValid(player)` 和 `MerchantContainer#stillValid`
都要求 `getTradingPlayer() == player`，容器每 tick 都会查一次 —— 没登记就是第一 tick 立即失效。

**修法**：把开门动作收进一个 `startTrading(player)`，顺序固定为
**`setTradingPlayer(player)` → `openTradingScreen(...)`**。
村民的对应实现在 `Villager#startTrading`（第 306 行），同样是这个顺序 —— 照着原版抄就不会错。

> 教训：实现原版接口时，**要注意哪些步骤是"接口默认方法不负责"的**。
> `openTradingScreen` 的名字很容易让人以为它把"开始交易"这件事全包了。

### 4.7 瑞克是静音生物

按要求移除了它发出的**全部**音效：

| 音效 | 原实现 | 现在 |
|---|---|---|
| 环境音 | `PLAYER_BREATH`（玩家的呼吸声） | `getAmbientSound() → null` |
| 受伤 | `PLAYER_HURT` | `getHurtSound() → null` |
| 死亡 | `PLAYER_DEATH` | `getDeathSound() → null` |
| 交易成交 | `VILLAGER_YES` | `getNotifyTradeSound() → SoundEvents.EMPTY` |
| 材料增减反馈 | `VILLAGER_YES / VILLAGER_NO` | `notifyTradeUpdated` 空实现 |

两个技术点：

1. **为什么返回 null 而不是 `getSoundVolume() = 0`**：原版三处调用点都做了 null 判断
   （`LivingEntity#makeSound` / `#handleDamageEvent` / `#handleEntityEvent`），
   null 是官方支持的"这个生物没这条音效"的写法（哑巴实体如盔甲架就是这么干的）。
   而 `getSoundVolume()` 会连带压低脚步声、游泳声、装备声等所有走 `Entity#playSound` 的声音，
   语义太脏，而且 0 音量仍然会发包。
2. **`getNotifyTradeSound()` 不能返回 null**：接口声明非空，且
   `MerchantMenu#playTradeSound` 会把它直接喂给 `Level#playLocalSound`（参数非空），
   返回 null 会 NPE。所以返回 `SoundEvents.EMPTY` —— 注册 id 是
   `minecraft:intentionally_empty` 的占位音效，客户端 `SoundManager` /
   `AbstractSoundInstance` 对它做了专门分支，播放它等于什么都不播、也不会刷日志。

**仍然会出声的部分**：脚步（`playStepSound`）和落水（`getSwimSound`）走的是方块与通用音效表，
对所有生物一视同仁，本次没有动。要连这些一起消掉，再覆写
`playStepSound` / `getSwimSound` / `getSwimSplashSound` 三个方法返回空即可。

## 5. 改动清单

| 文件 | 改动 |
|---|---|
| `entity/RickTrades.java` | **新增**。报价表定义 + 价格常量（`PARTICLE_COST` / `NETHERITE_COST` / `BULLET_YIELD`） |
| `entity/RickEntity.java` | `implements Merchant`；新增 `mobInteract` / `startTrading` / `isTrading`，以及全部 `Merchant` 接口实现；**三个语音音效 + 交易音效全部静音** |
| `docs/item_acquisition.md` | 补记反物质子弹的来源（原先标注为"无途径"） |

## 6. 待确认 / 可调项

- **音效**：瑞克目前是**完全静音**的（见 §4.7），包括交易成交与材料增减的反馈。
  如果你希望成交时至少有一下提示音，`notifyTradeUpdated` 里照抄 `AbstractVillager`
  的 20 tick 限流逻辑加回声音即可；`getNotifyTradeSound` 把 `SoundEvents.EMPTY`
  换成想要的事件即可。
- **价格**：现在是 2 微粒 + 1 合金锭 → 10 发。改 `RickTrades` 顶部的三个常量即可，
  界面会自动跟着变（价格槽读的就是这份报价）。
- **反物质子弹要不要同时给一条工作台配方**：目前没有，唯一来源就是瑞克。如果希望
  玩家在前期也能少量自制，可以再补一条贵一点的配方（需要你确认）。
