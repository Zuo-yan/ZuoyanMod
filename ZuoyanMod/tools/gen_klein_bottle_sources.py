#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""一次性生成克莱因瓶剩余的几个类（熔炉菜单 / 两个 Screen / 网络包 / 按键触发）。
生成后由 build 校验；这个脚本只是省去逐个手写样板，不属于运行时资源。"""
import os

JAVA = r"A:/Code/Minecraft/ZuoyanMod/ZuoyanMod/src/main/java/org/gwfx/zuoyanmod"

FILES = {}

# ---------------------------------------------------------------- 熔炉菜单
FILES[r"menu/KleinBottleFurnaceMenu.java"] = '''package org.gwfx.zuoyanmod.menu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.gwfx.zuoyanmod.item.KleinBottleItem;

/**
 * 克莱因瓶的内置熔炉：**没有燃料槽，也不需要燃料**——放进去就烧。
 *
 * <p>烧制本身不在这里推进，而由 {@link KleinBottleItem#tickFurnace} 挂在物品的 inventoryTick 上：
 * 那样玩家关掉界面、甚至只是把瓶子揣在包里，烧制都会继续。这个 Menu 只负责显示与取放。
 */
public class KleinBottleFurnaceMenu extends AbstractContainerMenu {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    private final FurnaceSlots slots;
    private final ContainerData data;

    public KleinBottleFurnaceMenu(int containerId, Inventory playerInventory, ItemStack bottle) {
        super(MenuRegistry.KLEIN_BOTTLE_FURNACE_MENU.get(), containerId);
        this.slots = new FurnaceSlots(bottle);
        this.data = new SimpleContainerData(2);

        addSlot(new Slot(slots, SLOT_INPUT, 56, 17));
        addSlot(new Slot(slots, SLOT_OUTPUT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // 产出槽只取不放
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        // data[0] 由外部每 tick 写入（见 KleinBottleItem#tickFurnace 的调用方），这里只留出同步位
        addDataSlots(data);
    }

    /** 烧制进度百分比，供 Screen 画箭头 */
    public int getProgressPercent() {
        return data.get(0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots().get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack inSlot = slot.getItem();
        ItemStack result = inSlot.copy();

        if (index == SLOT_OUTPUT) {
            if (!moveItemStackTo(inSlot, 2, 38, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index == SLOT_INPUT) {
            if (!moveItemStackTo(inSlot, 2, 38, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(inSlot, SLOT_INPUT, SLOT_INPUT + 1, false)) {
            return ItemStack.EMPTY;
        }

        if (inSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, inSlot);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** 输入/产出直接读写瓶子 NBT 的两格容器；数量与 ItemStack.count 分离（同存储区的理由） */
    private static final class FurnaceSlots extends SimpleContainer {

        private final ItemStack bottle;

        FurnaceSlots(ItemStack bottle) {
            super(2);
            this.bottle = bottle;
            CompoundTag tag = root();
            setItem(0, KleinBottleItem.decodeStack(tag, KleinBottleItem.TAG_FURNACE_IN));
            setItem(1, KleinBottleItem.decodeStack(tag, KleinBottleItem.TAG_FURNACE_OUT));
        }

        private CompoundTag root() {
            return bottle.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        }

        @Override
        public void setChanged() {
            super.setChanged();
            CompoundTag tag = root();
            ItemStack in = getItem(0);
            ItemStack out = getItem(1);
            KleinBottleItem.encodeStack(tag, KleinBottleItem.TAG_FURNACE_IN, in);
            tag.putInt(KleinBottleItem.TAG_FURNACE_IN_COUNT, in.isEmpty() ? 0 : in.getCount());
            KleinBottleItem.encodeStack(tag, KleinBottleItem.TAG_FURNACE_OUT, out);
            tag.putInt(KleinBottleItem.TAG_FURNACE_OUT_COUNT, out.isEmpty() ? 0 : out.getCount());
            bottle.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }
}
'''

# ---------------------------------------------------------------- 存储界面
FILES[r"client/KleinBottleScreen.java"] = '''package org.gwfx.zuoyanmod.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.gwfx.zuoyanmod.menu.KleinBottleMenu;

/**
 * 克莱因瓶存储界面。使用原版 {@code generic_54} 贴图（176×222），布局与之完全对齐。
 *
 * <p>本界面唯一的自绘内容是**每格右下角的真实总数**——原版槽位只能显示到堆叠上限，
 * 而这里的格子可能装着上百万个，所以数字必须另外画。
 */
public class KleinBottleScreen extends AbstractContainerScreen<KleinBottleMenu> {

    private static final Identifier TEXTURE =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int STORAGE_COLS = 9;
    private static final int STORAGE_ROWS = 6;
    private static final int SLOT_X = 8;
    private static final int SLOT_Y = 18;

    public KleinBottleScreen(KleinBottleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        this.inventoryLabelY = 222 - 94;
    }

    @Override
    protected void init() {
        super.init();
        // 两个功能按钮贴着左下角，避开 54 格区域
        addRenderableWidget(Button.builder(Component.literal("工作台"), b -> sendButton(KleinBottleMenu.BUTTON_CRAFTING))
                .bounds(leftPos + 8, topPos + 222 - 24, 58, 18).build());
        addRenderableWidget(Button.builder(Component.literal("熔炉"), b -> sendButton(KleinBottleMenu.BUTTON_FURNACE))
                .bounds(leftPos + 68, topPos + 222 - 24, 58, 18).build());
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE,
                leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 不调 super：默认标签色在深色底上看不清（见项目备忘）
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFFEDE7F6, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF9C8FBE, false);
        drawAmounts(graphics);
    }

    /** 每格右下角叠字显示真实总数：1.2k / 3.4M 这种紧凑写法，和 AE 一样 */
    private void drawAmounts(GuiGraphicsExtractor graphics) {
        for (int i = 0; i < STORAGE_COLS * STORAGE_ROWS; i++) {
            if (menu.getSlots().get(i).getItem().isEmpty()) {
                continue;
            }
            int amount = menu.getAmount(i);
            if (amount <= 1) {
                continue;
            }
            String text = compact(amount);
            int x = leftPos + SLOT_X + (i % STORAGE_COLS) * 18 + 17 - font.width(text);
            int y = topPos + SLOT_Y + (i / STORAGE_COLS) * 18 + 9;
            graphics.text(font, text, x, y, 0xFFFFE082, true);
        }
    }

    /** 紧凑数量：1000 → 1.0k，1_000_000 → 1.0M */
    public static String compact(int value) {
        if (value < 1000) {
            return Integer.toString(value);
        }
        if (value < 1_000_000) {
            return String.format("%.1fk", value / 1000.0);
        }
        return String.format("%.1fM", value / 1_000_000.0);
    }
}
'''

# ---------------------------------------------------------------- 熔炉界面
FILES[r"client/KleinBottleFurnaceScreen.java"] = '''package org.gwfx.zuoyanmod.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.gwfx.zuoyanmod.menu.KleinBottleFurnaceMenu;

/** 内置熔炉界面：复用原版熔炉底图，燃料槽区域留空——这台熔炉本来就不烧燃料。 */
public class KleinBottleFurnaceScreen extends AbstractContainerScreen<KleinBottleFurnaceMenu> {

    private static final Identifier TEXTURE =
            Identifier.withDefaultNamespace("textures/gui/container/furnace.png");

    public KleinBottleFurnaceScreen(KleinBottleFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE,
                leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFFEDE7F6, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF9C8FBE, false);
    }
}
'''

# ---------------------------------------------------------------- 网络包
FILES[r"network/OpenKleinBottlePacket.java"] = '''package org.gwfx.zuoyanmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.item.KleinBottleItem;

/** 客户端按 K → 请服务端打开背包里的克莱因瓶 */
public record OpenKleinBottlePacket() implements CustomPacketPayload {

    public static final Type<OpenKleinBottlePacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "open_klein_bottle"));

    public static final StreamCodec<ByteBuf, OpenKleinBottlePacket> STREAM_CODEC =
            StreamCodec.unit(new OpenKleinBottlePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenKleinBottlePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            // 主手 → 副手 → 背包，取第一只有权限的瓶子
            ItemStack main = player.getMainHandItem();
            if (main.getItem() instanceof KleinBottleItem && KleinBottleItem.openFor(player, main)) {
                return;
            }
            ItemStack off = player.getOffhandItem();
            if (off.getItem() instanceof KleinBottleItem && KleinBottleItem.openFor(player, off)) {
                return;
            }
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack candidate = inventory.getItem(i);
                if (candidate.getItem() instanceof KleinBottleItem && KleinBottleItem.openFor(player, candidate)) {
                    return;
                }
            }
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7背包里没有克莱因瓶"));
        });
    }
}
'''

# ---------------------------------------------------------------- 按键触发
FILES[r"event/KleinBottleKeyHandler.java"] = '''package org.gwfx.zuoyanmod.event;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.network.PacketHandler;

/** 克莱因瓶的 K 键：把背包里的随身终端召出来。 */
@EventBusSubscriber(modid = Zuoyanmod.MODID, value = Dist.CLIENT)
public final class KleinBottleKeyHandler {

    public static final KeyMapping OPEN_KLEIN_BOTTLE =
            new KeyMapping("key.zuoyanmod.open_klein_bottle", InputConstants.KEY_K, RealmKeybindHandler.CATEGORY);

    private KleinBottleKeyHandler() {}

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_KLEIN_BOTTLE);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null) {
            return;
        }
        if (OPEN_KLEIN_BOTTLE.consumeClick()) {
            PacketHandler.sendOpenKleinBottle();
        }
    }
}
'''

for rel, content in FILES.items():
    path = os.path.join(JAVA, rel.replace("/", os.sep))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    print("wrote", rel)
