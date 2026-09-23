#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把克莱因瓶接进注册表/网络/界面/语言/模型。幂等：重复运行会因为找不到锚点而报错，便于发现重复应用。"""
import json
import os

ROOT = r"A:/Code/Minecraft/ZuoyanMod/ZuoyanMod/src/main"
JAVA = os.path.join(ROOT, "java", "org", "gwfx", "zuoyanmod")
RES = os.path.join(ROOT, "resources", "assets", "zuoyanmod")


def patch(path, old, new, label):
    with open(path, encoding="utf-8-sig") as f:
        s = f.read()
    if new in s and old not in s:
        print("SKIP (已应用)", label)
        return
    assert old in s, "锚点未找到: " + label
    s = s.replace(old, new, 1)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(s)
    print("PATCH", label)


# 1) MenuRegistry
p = os.path.join(JAVA, "menu", "MenuRegistry.java")
patch(p,
      "import org.gwfx.zuoyanmod.Zuoyanmod;",
      "import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;\nimport org.gwfx.zuoyanmod.Zuoyanmod;",
      "MenuRegistry import")
patch(p,
      "    private MenuRegistry() {}",
      '''    /** 克莱因瓶存储：需要额外同步"打开的是哪一格"，所以走扩展工厂 */
    public static final DeferredHolder<MenuType<?>, MenuType<KleinBottleMenu>> KLEIN_BOTTLE_MENU =
            MENUS.register("klein_bottle",
                    () -> IMenuTypeExtension.create(KleinBottleMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<KleinBottleFurnaceMenu>> KLEIN_BOTTLE_FURNACE_MENU =
            MENUS.register("klein_bottle_furnace",
                    () -> new MenuType<>(KleinBottleFurnaceMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private MenuRegistry() {}''',
      "MenuRegistry registrations")

# 2) PacketHandler
p = os.path.join(JAVA, "network", "PacketHandler.java")
patch(p,
      "        LOGGER.info(\"[Network] Successfully registered payload handlers\");",
      '''        registrar.playToServer(
                OpenKleinBottlePacket.TYPE,
                OpenKleinBottlePacket.STREAM_CODEC,
                OpenKleinBottlePacket::handle
        );
        LOGGER.info("[Network] Successfully registered payload handlers");''',
      "PacketHandler register")
patch(p,
      "    private static void sendToServer(CustomPacketPayload payload) {",
      '''    public static void sendOpenKleinBottle() {
        sendToServer(new OpenKleinBottlePacket());
    }

    private static void sendToServer(CustomPacketPayload payload) {''',
      "PacketHandler sender")

# 3) Zuoyanmod：注册两个界面
p = os.path.join(JAVA, "Zuoyanmod.java")
patch(p,
      "            event.register(MenuRegistry.VOID_RESONANCE_PUMP_MENU.get(), VoidResonancePumpScreen::new);",
      '''            event.register(MenuRegistry.VOID_RESONANCE_PUMP_MENU.get(), VoidResonancePumpScreen::new);
            event.register(MenuRegistry.KLEIN_BOTTLE_MENU.get(),
                    org.gwfx.zuoyanmod.client.KleinBottleScreen::new);
            event.register(MenuRegistry.KLEIN_BOTTLE_FURNACE_MENU.get(),
                    org.gwfx.zuoyanmod.client.KleinBottleFurnaceScreen::new);''',
      "Zuoyanmod screens")

# 4) ItemRegistry
p = os.path.join(JAVA, "item", "ItemRegistry.java")
patch(p,
      "    // ===== 领域展开 =====",
      '''    // ===== 克莱因瓶（随身存储终端 + 内置工作台 / 无燃料熔炉） =====
    public static final DeferredItem<KleinBottleItem> KLEIN_BOTTLE = ITEMS.registerItem(
            "klein_bottle",
            KleinBottleItem::new,
            props -> props.stacksTo(1).rarity(Rarity.EPIC)
    );

    // ===== 领域展开 =====''',
      "ItemRegistry register")

# 5) 创造栏
p = os.path.join(JAVA, "item", "CreativeTabRegistry.java")
patch(p,
      "                        // 真空衰变（万能挖掘锤：负熵灌注 / 分子离解 / 对称破缺）\n                        output.accept(ItemRegistry.VACUUM_DECAY.get());",
      '''                        // 真空衰变（万能挖掘锤：负熵灌注 / 分子离解 / 对称破缺）
                        output.accept(ItemRegistry.VACUUM_DECAY.get());

                        // 克莱因瓶（随身存储终端）
                        output.accept(ItemRegistry.KLEIN_BOTTLE.get());''',
      "CreativeTab")

# 6) 熔炉菜单：补 2 参构造（客户端 MenuType 需要） + 空瓶守卫
p = os.path.join(JAVA, "menu", "KleinBottleFurnaceMenu.java")
patch(p,
      "    /** 服务端每 tick 刷一次进度百分比，交给原版同步机制发给客户端 */",
      '''    /** 客户端 2 参构造：内容全部靠槽位同步包填，瓶子本身不需要（服务端才知道是哪一只） */
    public KleinBottleFurnaceMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ItemStack.EMPTY);
    }

    /** 服务端每 tick 刷一次进度百分比，交给原版同步机制发给客户端 */''',
      "Furnace 2-arg ctor")
patch(p,
      "        @Override\n        public void setChanged() {\n            super.setChanged();\n            CompoundTag tag = root();",
      "        @Override\n        public void setChanged() {\n            super.setChanged();\n            if (bottle.isEmpty()) {\n                return; // 客户端的空壳容器：只接同步，不回写\n            }\n            CompoundTag tag = root();",
      "Furnace empty guard")

# 7) 语言
for lang, item_name, key_name in (
        ("zh_cn.json", "克莱因瓶", "打开克莱因瓶"),
        ("en_us.json", "Klein Bottle", "Open Klein Bottle")):
    p = os.path.join(RES, "lang", lang)
    with open(p, encoding="utf-8-sig") as f:
        d = json.load(f)
    d["item.zuoyanmod.klein_bottle"] = item_name
    d["key.zuoyanmod.open_klein_bottle"] = key_name
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(d, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("LANG", lang)

# 8) 物品定义与模型
os.makedirs(os.path.join(RES, "items"), exist_ok=True)
os.makedirs(os.path.join(RES, "models", "item"), exist_ok=True)
with open(os.path.join(RES, "items", "klein_bottle.json"), "w", encoding="utf-8", newline="\n") as f:
    f.write('{"model": {"type": "minecraft:model", "model": "zuoyanmod:item/klein_bottle"}}\n')
with open(os.path.join(RES, "models", "item", "klein_bottle.json"), "w", encoding="utf-8", newline="\n") as f:
    json.dump({"parent": "minecraft:item/generated",
               "textures": {"layer0": "zuoyanmod:item/klein_bottle"}}, f, indent=2)
    f.write("\n")
print("MODELS klein_bottle")
