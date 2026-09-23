#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""克莱因瓶第二版：存储迁到玩家附件（四维空间），界面重构为终端风格。一次性生成源码。"""
import os

JAVA = r"A:/Code/Minecraft/ZuoyanMod/ZuoyanMod/src/main/java/org/gwfx/zuoyanmod"
F = {}

F[r"item/KleinBottleItem.java"] = '''package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * 克莱因瓶：**一把钥匙**。右键或背包里按 K，打开属于你自己的「四维空间」。
 *
 * <p>第二版的重要变化：它**不再保存任何数据**。第一版把 54 格塞在物品 NBT 里，
 * 于是存储跟着瓶子走——瓶子丢了一仓库东西陪葬。现在数据挂在**玩家**身上
 * （见 {@link FourDimensionalSpace}），瓶子只是打开那扇门的凭证，换多少只都一样。
 */
public class KleinBottleItem extends Item {

    /** 熔炉单次烧制耗时：5 秒（原版的一半） */
    public static final int COOK_TICKS = 100;

    public KleinBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            FourDimensionalSpace.of(serverPlayer).openTerminal(serverPlayer);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /** 打开任意一个玩家的四维空间（K 键与右键共用） */
    public static boolean openFor(ServerPlayer player) {
        FourDimensionalSpace.of(player).openTerminal(player);
        return true;
    }

    /**
     * 熔炉挂在物品的 inventoryTick 上：只要瓶子在背包里，四维空间的熔炉就在烧，
     * 不需要开着界面、也不需要 BlockEntity。
     */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (entity instanceof Player player) {
            FourDimensionalSpace.of(player).tickFurnace(level);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("§d§l克莱因瓶"));
        tooltip.accept(Component.literal("§7内外不分，所以它通向一个不属于这里的空间"));

        tooltip.accept(Component.literal("§8【四维空间】"));
        tooltip.accept(Component.literal("§7手持右键，或放在背包里按 §fK §7打开"));
        tooltip.accept(Component.literal("§8  · " + FourDimensionalSpace.SLOTS + " 格，单格上限 §f"
                + (FourDimensionalSpace.MAX_PER_SLOT / 10000) + " 万"));
        tooltip.accept(Component.literal("§8  · 同种物品自动并进一格，槽位右下角显示真实总数"));
        tooltip.accept(Component.literal("§8  · §f属于你自己§8：换一只瓶子、甚至瓶子丢了，东西都还在"));
        tooltip.accept(Component.literal("§8  · 死亡不掉落"));
        tooltip.accept(Component.literal("§8【终端内置】"));
        tooltip.accept(Component.literal("§7工作台 · 无燃料熔炉 · 一键整理"));
    }
}
'''

F[r"menu/KleinBottleMenu.java"] = '''package org.gwfx.zuoyanmod.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.gwfx.zuoyanmod.item.FourDimensionalSpace;

/**
 * 「四维空间」终端：9×6 存储 + 玩家背包 + 三个功能按钮（工作台 / 熔炉 / 整理）。
 *
 * <p>数据源是玩家的 attachment，**不是物品 NBT**，所以不再需要"告诉客户端是哪一只瓶子"。
 * 客户端依旧用平铺容器接槽位同步包，真实总数走 ContainerData（可能上百万，ItemStack 装不下）。
 */
public class KleinBottleMenu extends AbstractContainerMenu {

    public static final int BUTTON_CRAFTING = 0;
    public static final int BUTTON_FURNACE = 1;
    public static final int BUTTON_SORT = 2;

    private static final int SLOTS = FourDimensionalSpace.SLOTS;
    private static final int COL_X = 8;
    public static final int GRID_Y = 44;
    public static final int PLAYER_Y = 168;

    private final Container container;
    private final ContainerData data;

    public KleinBottleMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, playerInventory.player);
    }

    public KleinBottleMenu(int containerId, Inventory playerInventory, Player player) {
        super(MenuRegistry.KLEIN_BOTTLE_MENU.get(), containerId);
        boolean client = player.level().isClientSide();
        FourDimensionalSpace space = client ? null : FourDimensionalSpace.of(player);
        this.container = client ? new SimpleContainer(SLOTS) : new SpaceContainer(space);
        this.data = client ? new SimpleContainerData(SLOTS) : amountData(space);
        checkContainerSize(container, SLOTS);
        container.startOpen(playerInventory.player);

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(container, col + row * 9, COL_X + col * 18, GRID_Y + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, COL_X + col * 18, PLAYER_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, COL_X + col * 18, PLAYER_Y + 58));
        }
        addDataSlots(data);
    }

    private static ContainerData amountData(FourDimensionalSpace space) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return space.amountOf(index);
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return SLOTS;
            }
        };
    }

    /** 某一格真实总数（两侧一致：都读同步数据） */
    public int getAmount(int slot) {
        return slot >= 0 && slot < SLOTS ? data.get(slot) : 0;
    }

    /** 已占格数 */
    public int getUsedSlots() {
        int n = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (!getSlot(i).getItem().isEmpty()) {
                n++;
            }
        }
        return n;
    }

    /** 总件数（超 int 时按 int 截断显示，够用） */
    public long getTotalItems() {
        long n = 0;
        for (int i = 0; i < SLOTS; i++) {
            n += getAmount(i);
        }
        return n;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        FourDimensionalSpace space = FourDimensionalSpace.of(serverPlayer);
        switch (id) {
            case BUTTON_CRAFTING -> {
                FourDimensionalSpace.openCrafting(serverPlayer);
                return true;
            }
            case BUTTON_FURNACE -> {
                space.openFurnace(serverPlayer);
                return true;
            }
            case BUTTON_SORT -> {
                space.compact();
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("§d四维空间 §7已整理"));
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack inSlot = slot.getItem();
        ItemStack result = inSlot.copy();

        if (index < SLOTS) {
            if (!moveItemStackTo(inSlot, SLOTS, SLOTS + 36, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(inSlot, 0, SLOTS, false)) {
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

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    /** 把容器接口翻译到 {@link FourDimensionalSpace}：真实数量与 ItemStack.count 分离 */
    private static final class SpaceContainer extends SimpleContainer {

        private final FourDimensionalSpace space;

        SpaceContainer(FourDimensionalSpace space) {
            super(SLOTS);
            this.space = space;
            refreshAll();
        }

        /**
         * 直接写内部列表，**绝不走 setItem**。
         * 原因：SimpleContainer.setItem 内部会回调 setChanged()，而 setChanged() 又要刷新展示栈——
         * 用 super.setItem 刷新就会 setChanged → setItem → setChanged 无限递归（v2 首次实机就是死在这里）。
         */
        private void put(int slot, ItemStack stack) {
            getItems().set(slot, stack);
        }

        private void refreshAll() {
            for (int i = 0; i < SLOTS; i++) {
                put(i, space.displayOf(i));
            }
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (stack.isEmpty()) {
                space.clearSlot(slot);
            } else {
                ItemStack type = space.typeOf(slot);
                if (type.isEmpty() || !ItemStack.isSameItemSameComponents(type, stack)) {
                    space.clearSlot(slot);
                    space.setType(slot, stack);
                }
                space.insert(slot, stack.getCount());
            }
            put(slot, space.displayOf(slot));
            // 现在安全了：setChanged 内部只用 put()，不会再回到这里
            super.setChanged();
        }

        @Override
        public void setChanged() {
            super.setChanged();
            refreshAll();
        }
    }
}
'''

F[r"menu/KleinBottleFurnaceMenu.java"] = '''package org.gwfx.zuoyanmod.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.gwfx.zuoyanmod.item.FourDimensionalSpace;
import org.gwfx.zuoyanmod.item.KleinBottleItem;

/**
 * 四维空间的内置熔炉：**没有燃料槽，也不需要燃料**——放进去就烧。
 * 烧制推进在 {@link FourDimensionalSpace#tickFurnace}（挂在克莱因瓶的 inventoryTick 上），
 * 这里只负责取放与进度显示。
 */
public class KleinBottleFurnaceMenu extends AbstractContainerMenu {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    private final SimpleContainerData data = new SimpleContainerData(1);
    private final FourDimensionalSpace space;
    private final Container container;

    public KleinBottleFurnaceMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, playerInventory.player);
    }

    public KleinBottleFurnaceMenu(int containerId, Inventory playerInventory, Player player) {
        super(MenuRegistry.KLEIN_BOTTLE_FURNACE_MENU.get(), containerId);
        boolean client = player.level().isClientSide();
        this.space = client ? null : FourDimensionalSpace.of(player);
        this.container = client ? new SimpleContainer(2) : new FurnaceContainer(space);

        addSlot(new Slot(container, SLOT_INPUT, 56, 17));
        addSlot(new Slot(container, SLOT_OUTPUT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
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
        addDataSlots(data);
    }

    @Override
    public void broadcastChanges() {
        if (space != null) {
            data.set(0, Math.min(100, space.cookProgress() * 100 / KleinBottleItem.COOK_TICKS));
        }
        super.broadcastChanges();
    }

    public int getProgressPercent() {
        return data.get(0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
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

    /** 两格容器，直接读写四维空间的熔炉字段 */
    private static final class FurnaceContainer extends SimpleContainer {

        private final FourDimensionalSpace space;

        FurnaceContainer(FourDimensionalSpace space) {
            super(2);
            this.space = space;
            if (space.furnaceInputCount() > 0 && !space.furnaceInput().isEmpty()) {
                getItems().set(0, space.furnaceInput().copyWithCount(space.furnaceInputCount()));
            }
            if (space.furnaceOutputCount() > 0 && !space.furnaceOutput().isEmpty()) {
                getItems().set(1, space.furnaceOutput().copyWithCount(space.furnaceOutputCount()));
            }
        }

        @Override
        public void setChanged() {
            super.setChanged();
            ItemStack in = getItem(0);
            ItemStack out = getItem(1);
            space.setFurnaceInput(in, in.isEmpty() ? 0 : in.getCount());
            space.setFurnaceOutput(out, out.isEmpty() ? 0 : out.getCount());
        }
    }
}
'''

F[r"client/KleinBottleScreen.java"] = '''package org.gwfx.zuoyanmod.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.menu.KleinBottleMenu;

/**
 * 「四维空间」终端界面。自绘底图（见 tools/gen_klein_bottle_textures.py），
 * 布局参考主流终端存储模组：顶部标题 + 容量读数，下面一排功能按钮，再往下是存储网格与背包。
 *
 * <p>唯一的动态绘制是**每格右下角的真实总数**（可能上百万），原版槽位只能显示到堆叠上限。
 */
public class KleinBottleScreen extends AbstractContainerScreen<KleinBottleMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "textures/gui/four_dimensional_space.png");

    public static final int IMAGE_W = 176;
    public static final int IMAGE_H = 250;
    public static final int GRID_Y = 44;
    public static final int PLAYER_Y = 168;
    public static final int SLOT_X = 8;
    public static final int BUTTON_Y = 24;
    public static final int BUTTON_H = 16;

    private static final int[] BUTTON_X = {8, 62, 116};
    private static final int BUTTON_W = 52;
    private static final String[] BUTTON_LABEL = {"工作台", "熔炉", "整理"};

    public KleinBottleScreen(KleinBottleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_W, IMAGE_H);
        this.inventoryLabelY = PLAYER_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < 3; i++) {
            final int id = i;
            addRenderableWidget(Button.builder(Component.literal(BUTTON_LABEL[i]), b -> click(id))
                    .bounds(leftPos + BUTTON_X[i], topPos + BUTTON_Y, BUTTON_W, BUTTON_H).build());
        }
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE,
                leftPos, topPos, 0.0F, 0.0F, IMAGE_W, IMAGE_H, IMAGE_W, IMAGE_H);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 不调 super：默认标签色在深色底上看不清
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFFEDE7F6, false);

        String stats = menu.getUsedSlots() + " / " + org.gwfx.zuoyanmod.item.FourDimensionalSpace.SLOTS
                + " 格   " + compact(menu.getTotalItems()) + " 件";
        graphics.text(font, net.minecraft.network.chat.Component.literal(stats),
                IMAGE_W - 8 - font.width(stats), titleLabelY, 0xFF9C8FBE, false);

        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF9C8FBE, false);
        drawAmounts(graphics);
    }

    /** 每格右下角叠字显示真实总数：1.2k / 3.4M，和 AE 一样 */
    private void drawAmounts(GuiGraphicsExtractor graphics) {
        for (int i = 0; i < 54; i++) {
            if (menu.slots.get(i).getItem().isEmpty()) {
                continue;
            }
            int amount = menu.getAmount(i);
            if (amount <= 1) {
                continue;
            }
            String text = compact(amount);
            int x = leftPos + SLOT_X + (i % 9) * 18 + 17 - font.width(text);
            int y = topPos + GRID_Y + (i / 9) * 18 + 9;
            graphics.text(font, text, x, y, 0xFFFFE082, true);
        }
    }

    /** 紧凑数量：1000 → 1.0k，1_000_000 → 1.0M */
    public static String compact(long value) {
        if (value < 1000) {
            return Long.toString(value);
        }
        if (value < 1_000_000L) {
            return String.format("%.1fk", value / 1000.0);
        }
        if (value < 1_000_000_000L) {
            return String.format("%.1fM", value / 1_000_000.0);
        }
        return String.format("%.1fG", value / 1_000_000_000.0);
    }
}
'''

for rel, content in F.items():
    path = os.path.join(JAVA, rel.replace("/", os.sep))
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    print("wrote", rel)

# 第一版的物品 NBT 容器已废弃
dead = os.path.join(JAVA, "item", "KleinBottleContainer.java")
if os.path.exists(dead):
    os.remove(dead)
    print("removed item/KleinBottleContainer.java (第一版方案，已废弃)")
