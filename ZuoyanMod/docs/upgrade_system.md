# 经验升级系统（基础能力 + 终极天赋）

> 26.3 新增玩法系统。用经验等级购买基础能力，全部满级后选定一个按 Y 释放的终极天赋。

## 玩法循环

1. **攒经验** → 按 **U** 打开升级界面（`UpgradeScreen`）
2. **左键**点击 5 条基础能力行升级，每条 10 级，第 n+1 级花 `base + step×n` 级经验（默认 1,2,3…10，每条 55 级、全满 275 级）；**右键**退还最后一级、返还当年花费（随时可退，无手续费）
3. **50/50 全满**后，右列终极天赋卡解锁，点两下（选中 → 确认）选定一个（想换用「天赋重置卷轴」，见下）
4. 按 **Y** 释放终极天赋，各天赋独立冷却（基础 22/6/45/60 秒 × 配置倍率，已整体砍半、击飞单独定为 6 秒）；冷却结束瞬间右上角弹模组 toast「已就绪」，平时不占 UI，按 Y 可随时查剩余冷却

## 基础能力（`upgrade/UpgradeType`）

| 条目 | 每级 | 满级 | 实现 |
|---|---|---|---|
| 生命上限 | +2 | +20 | `MAX_HEALTH` ADD_VALUE transient modifier |

> 右键退还：`RefundStatPacket` → `UpgradeManager.refundStat`，返还 `xpCost(level-1)`。
| 攻击力 | +0.5 | +5 | `ATTACK_DAMAGE` ADD_VALUE |
| 移动速度 | +4% | +40% | `MOVEMENT_SPEED` ADD_MULTIPLIED_TOTAL |
| 护甲值 | +2 | +20 | `ARMOR` ADD_VALUE |
| 伤害减免 | -2% | -20% | `LivingDamageEvent.Pre` 乘法减免（护甲结算后） |

modifier 重算入口：登录、重生、每次升级（`UpgradeManager.applyAttributes`）。
数据本体在 Attachment（`copyOnDeath`，死亡不掉），transient modifier 不入库、靠重算恢复。

## 终极天赋（`upgrade/UltimateTalent`）

| 天赋 | 效果 | 冷却 | 实现 |
|---|---|---|---|
| 绝对零度·抓取 | 准星目标（≤24 格）拽至身边 + 冻结 5 秒 | 45s | `ProjectileUtil.getHitResultOnViewVector` 射线 + 服务端 tick 拽拉表（改写速度 + 对玩家直发 motion 包）+ 冻结表（速度清零、缓慢 X、`setTicksFrozen`、雪花粒子） |
| 天罚·击飞 | 半径 10 格目标升空（初速 2.4，高度 ×1.5），摔落 + 虚弱 III 2 分钟 | 6s | AABB 查询 + `applyVelocity`，摔伤交给原版坠落结算 |
| 分子离解·灌能 | 30 秒内下一次近战命中使目标离解 3 分钟 | 90s | 档案里记到期 gameTime；伤害事件里消费，施加现成 `molecular_dissolution`（每秒 4 点真伤，3 分钟 ≈ 720 点） |
| 原始黑洞 | 准星落点展开黑洞 | 120s | **直接调 `PrimordialBlackHoleEntity.spawn`，与「原始黑洞」物品完全同款**（同实体、同 24 格射线落点 + 0.6 表格外推、同"每人一个 + 全局上限"检查与提示文案） |

拽拉/冰冻是 `UpgradeManager` 里的服务端登记表（`VacuumDecayBlackHoleManager` 同款套路），
不值得起真实体；失败（没目标/黑洞上限）不进冷却。

## 网络与同步

| 包 | 方向 | 作用 |
|---|---|---|
| `UpgradeStatPacket` | C2S | 升级某条（服务端校验下标/满级/经验） |
| `ChooseTalentPacket` | C2S | 选定天赋（校验全满 + 未选过） |
| `TriggerUltimatePacket` | C2S | Y 键触发 |
| `RequestUpgradeSyncPacket` | C2S | 开界面时请求快照 |
| `UpgradeSyncPacket` | S2C | 档案快照：等级 + 天赋 + 冷却截止 gameTime + 灌能到期 |

客户端 `ClientUpgradeData` 只读快照；冷却剩余用客户端世界 gameTime 对减
（原版每 tick 同步世界时间，两端对齐），无需轮询。

## 持久化（`UpgradeData`）

`AttachmentType("upgrade_data")`，`copyOnDeath` + `IAttachmentSerializer`。
冷却序列化成**剩余秒数**（int 数组），读档按当前 gameTime 换算——26.3 的
`ValueOutput` 没有 long 数组，存剩余值顺便消灭了"绝对 gameTime 跨存档漂移"问题。

## 终极天赋重置卷轴（`item/TalentResetItem`，id `talent_reset_scroll`）

右键清空已选终极天赋（冷却/灌能一并归零），可重新选择；基础加点不受影响。
成功才扣卷轴（`consume` 自带创造豁免）；没选天赋时提示且不扣。`UpgradeManager.resetTalent` 是服务端权威。
贴图由 `tools/gen_talent_reset_texture.py` 生成（零依赖手写 PNG 编码，羊皮卷 + 金色循环箭头）。

## 冷却就绪 toast

`UpgradeServerEvents.onServerTick` 每 tick 轮询在线玩家 → `UpgradeData.pollReadyAnnouncement`
（"刚刚结束"只报一次，瞬态 `readyAnnounced` 标记；离线期间过期的冷却不补报）→
`ModToastPacket` 右上角 toast + 经验音效。常驻 HUD 已移除（`UpgradeHudOverlay` 删除）。

## 配置（`Config.upgrade.*`）

`enabled` / `xpCostBase(1)` / `xpCostStep(1)` / `ultimateCooldownMultiplier(1.0)` /
`grappleRange(24)` / `launchRadius(10)`

## 按键与 UI

- 按键：`key.zuoyanmod.open_upgrade`（U）、`key.zuoyanmod.ultimate`（Y），
  挂在 `RealmKeybindHandler.CATEGORY` 分类下，`client/UpgradeKeyHandler` 注册与消费
- 界面：`client/UpgradeScreen` 纯 Screen 无 Menu，全 `GuiGraphicsExtractor` 代码绘制，
  配色复用 klein 主题；数值细节在 hover tooltip；天赋卡两段确认防手滑
- 界面右键属性行 = 退还最后一级（`UpgradeRowButton` 覆写 `mouseClicked` 拦右键，
  26.3 的 `AbstractWidget.isValidClickButton` 默认恰是右键，super 路径不会误触发）
- ~~HUD~~：常驻冷却显示已按需求移除，就绪走 toast、查询走 Y 键

## 26.3 API 备注（本系统趟过的坑）

- `AttributeModifier.Operation.MULTIPLY_TOTAL` → `ADD_MULTIPLIED_TOTAL`
- `Attributes.X` 是 `Holder<Attribute>`，`getAttribute(holder)` 按这个传
- `Entity.hurtMarked` 公共字段没了（protected `markHurt()`）；对玩家强制位移要
  直发 `ClientboundSetEntityMotionPacket`（原版爆炸的击退就是这么做的）
- `ServerPlayer#displayClientMessage(msg, true)`（actionbar）→ `sendOverlayMessage(msg)`
- `ServerLifecycleHooks` 在 `net.neoforged.neoforge.server`（不是 `fml.server`）
- `Options.hideGui` 没了 → `Minecraft.getInstance().gui.hud.isHidden()`
- `LivingEntity.hurt` 废弃 → `hurtServer(level, source, damage)`（本系统没直接用，但改这里时注意）
- 纯 Screen 自绘：覆写 `extractRenderState(GuiGraphicsExtractor, ...)`；
  tooltip 用 `g.setTooltipForNextFrame(font, List<Component>, Optional.empty(), x, y)`
