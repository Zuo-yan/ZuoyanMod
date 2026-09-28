# 万能工具（Universal Tool）

前期工具线：把原版五件套（镐 · 斧 · 铲 · 锄 · 剑）融合成一件，按材质分六档。

## 物品列表

| 物品 ID | 中文名 | 材质耐久 | 挖掘速度 | 攻击力 | 攻速 | 附魔能力 |
|---|---|---|---|---|---|---|
| `wooden_universal_tool` | 木质万能工具 | 59 | 2.0 | 7 | 0.8 | 15 |
| `stone_universal_tool` | 石质万能工具 | 131 | 4.0 | 9 | 0.8 | 5 |
| `golden_universal_tool` | 金质万能工具 | 32 | 12.0 | 7 | 1.0 | 22 |
| `iron_universal_tool` | 铁质万能工具 | 250 | 6.0 | 9 | 0.9 | 14 |
| `diamond_universal_tool` | 钻石万能工具 | 1561 | 8.0 | 9 | 1.0 | 10 |
| `netherite_universal_tool` | 下界合金万能工具 | 2031 | 9.0 | 10 | 1.0 | 15 |

- 攻击力/攻速取同材质**斧**的原版数值（五件中最高），下界合金档带 `fire_resistant`（岩浆不烧毁）。
- 挖掘速度、耐久、附魔能力、修复材料全部沿用对应 `ToolMaterial`（26.x `net.minecraft.world.item.ToolMaterial`）。
- 修复材料即对应材质工具的修复材料（木板/圆石/金锭/铁锭/钻石/下界合金锭，走 `*_tool_materials` 标签）。

## 行为设计（实现要点）

- **挖掘**：`DataComponents.TOOL` 一个组件合并四张 mineable 标签
  （`mineable/pickaxe`、`/axe`、`/shovel`、`/hoe`），各按材质速度生效；再叠加剑的规则
  （蛛网 15 倍速、`sword_instantly_mines` 瞬破、`sword_efficient` 1.5 倍速）。
  标签在注册期经 `BuiltInRegistries.acquireBootstrapRegistrationLookup` 解析（与原版 `ToolMaterial.applyToolProperties` 同法）。
- **采集门槛**：保留 `deniesDrops(incorrectBlocksForDrops)` —— 木质万能工具照样挖不了钻石矿掉落物，材质等级不吃亏也不越级。
- **右键三合一**：26.x 的剥树皮/锄耕地/铲铲路由 `DataComponents.BLOCK_TRANSFORMER`（单一 `Holder<BlockTransformer>`）驱动，
  一个组件挂不下三个 transformer，因此 `UniversalToolItem#useOn` 覆写为按 **斧 → 锄 → 铲** 顺序
  从 `level.registryAccess()` 取原版三个 transformer 依次尝试，谁命中执行谁，自动跟随原版更新。
- **战斗**：`Weapon(1, 5.0F)` —— 每次攻击 1 点耐久（剑标准）+ 命中停盾 5 秒（斧特性）。

## 附魔

物品加入原版 `enchantable/*` 物品标签（`data/minecraft/tags/item/enchantable/*.json`）：
`mining`（效率/精准采集）、`mining_loot`（时运）、`sharp_weapon`（锋利）、
`weapon`（抢夺/节肢杀手等）、`melee_weapon`（击退/火焰附加）、`sweeping`（横扫之刃）、
`durability`（耐久/经验修补）。附魔能力用材质本身的值（金 22 最好附魔，石 5 最差）。

## 配方

工作台 3x3，把该材质的五件原版工具摆成十字：

```
 . 镐 .
 斧 锄 剑
 . 铲 .
```

配方文件：`data/zuoyanmod/recipe/<tier>_universal_tool.json`。

## 贴图

`assets/zuoyanmod/textures/item/<tier>_universal_tool.png`：由原版铁镐 + 铁斧纹理
alpha 叠加（共用同一斜柄）成"镐弧+斧刃"剪影，头部像素按亮度三档重映射到各材质配色。
生成脚本思路见 git 历史（Pillow，16x16）。

## 与「真空衰变」的边界

真空衰变是终局万能挖掘锤（全方块正确采集 + 无限耐久）；万能工具是**前期**的材质级
融合工具，数值和等级严格跟随原版材质，不越级、不万能采。
