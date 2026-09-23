#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""克莱因瓶第三版（下）：分页滚动终端 + 去掉熔炉独立界面 + 简化描述。"""
import os

JAVA = r"A:/Code/Minecraft/ZuoyanMod/ZuoyanMod/src/main/java/org/gwfx/zuoyanmod"
F = {}

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
 * 「四维空间」终端：**分页滚动**的无限存储。
 *
 * <p>存储本体是 {@link FourDimensionalSpace} 里一条不设上限的条目列表；这个 Menu 只是把
 * 其中 **54 条**投到界面上（一页），翻页时重新装填。翻页用 ◀ ▶ 按钮，也可以直接**滚轮**
 * （客户端把滚轮转成按钮点击发上来，见 {@code KleinBottleScreen#mouseScrolled}）。
 *
 * <p>两个易错点都记下了：
 * <ul>
 *   <li>窗口容器的 {@code setChanged()} 故意置空——它在 {@code setItem} 里被回调，若在这里回写
 *       四维空间就会重演 v2 的 {@code StackOverflowError}；回写放在 {@code setItem}/{@code removeItem} 里做。</li>
 *   <li>刷新窗口内容只能用 {@code getItems().set(...)}，绝不能走 {@code setItem}。</li>
 * </ul>
 */
public class KleinBottleMenu extends AbstractContainerMenu {

    public static final int BUTTON_CRAFTING = 0;
    public static final int BUTTON_FURNACE = 1;
    public static final int BUTTON_PAGE_PREV = 2;
    public static final int BUTTON_PAGE_NEXT = 3;

    public static final int PAGE_SLOTS = FourDimensionalSpace.PAGE_SLOTS;
    public static final int COL_X = 8;
    public static final int GRID_Y = 44;
    public static final int PLAYER_Y = 168;

    private final Container window;
    private final ContainerData data;
    private final FourDimensionalSpace space;
    private int page;
    private int lastSize = -1;
    private int lastPage = -1;

    public KleinBottleMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, playerInventory.player);
    }

    public KleinBottleMenu(int containerId, Inventory playerInventory, Player player) {
        super(MenuRegistry.KLEIN_BOTTLE_MENU.get(), containerId);
        boolean client = player.level().isClientSide();
        this.space = client ? null : FourDimensionalSpace.of(player);
        this.window = new WindowContainer();
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                if (space == null) {
                    return 0;
                }
                return switch (index) {
                    case 0 -> page;
                    case 1 -> space.pageCount();
                    case 2 -> space.isAutoSmelt() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 3;
            }
        };

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(window, col + row * 9, COL_X + col * 18, GRID_Y + row * 18));
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
        refreshWindow();
    }

    public int getPage() {
        return data.get(0);
    }

    public int getPageCount() {
        return Math.max(1, data.get(1));
    }

    public boolean isAutoSmelt() {
        return data.get(2) == 1;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer) || space == null) {
            return false;
        }
        switch (id) {
            case BUTTON_CRAFTING -> FourDimensionalSpace.openCrafting(serverPlayer);
            case BUTTON_FURNACE -> {
                space.setAutoSmelt(!space.isAutoSmelt());
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§d四维空间 §7自动熔炼：" + (space.isAutoSmelt() ? "§a开" : "§c关")));
            }
            case BUTTON_PAGE_PREV -> page = Math.max(0, page - 1);
            case BUTTON_PAGE_NEXT -> page = Math.min(space.pageCount() - 1, page + 1);
            default -> {
                return false;
            }
        }
        refreshWindow();
        return true;
    }

    /** 把当前页的条目装进窗口容器（**只能**用 getItems().set，绝不能走 setItem） */
    private void refreshWindow() {
        for (int i = 0; i < PAGE_SLOTS; i++) {
            int abs = page * PAGE_SLOTS + i;
            window.getItems().set(i, space != null ? space.get(abs) : ItemStack.EMPTY);
        }
    }

    @Override
    public void broadcastChanges() {
        if (space != null && (lastPage != page || lastSize != space.size())) {
            page = Math.min(page, Math.max(0, space.pageCount() - 1));
            refreshWindow();
            lastPage = page;
            lastSize = space.size();
        }
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack inSlot = slot.getItem();
        ItemStack result = inSlot.copy();

        if (index < PAGE_SLOTS) {
            if (!moveItemStackTo(inSlot, PAGE_SLOTS, PAGE_SLOTS + 36, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(inSlot, 0, PAGE_SLOTS, false)) {
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
        // 关界面时顺手整理一次：跨页的同种物品合并、空档收紧
        if (space != null) {
            space.compact();
        }
    }

    /**
     * 54 格的"视口"容器：读写都翻译到四维空间的绝对条目上。
     * {@code setChanged()} 置空是刻意的（见类注释），回写在 {@code setItem}/{@code removeItem} 里做。
     */
    private final class WindowContainer extends SimpleContainer {

        WindowContainer() {
            super(PAGE_SLOTS);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            getItems().set(slot, stack);
            writeBack(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            ItemStack out = super.removeItem(slot, count);
            writeBack(slot);
            return out;
        }

        private void writeBack(int viewSlot) {
            if (space == null) {
                return;
            }
            space.set(page * PAGE_SLOTS + viewSlot, getItem(viewSlot));
        }

        @Override
        public void setChanged() {
            // 空实现：这里的变更不需要通知任何人，页内容由 refreshWindow() 全量装填
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }
}
'''

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

/** 克莱因瓶：**一把钥匙**。右键或背包里按 K，打开属于你自己的「四维空间」。 */
public class KleinBottleItem extends Item {

    public KleinBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            KleinBottleItem.openFor(serverPlayer);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    public static boolean openFor(ServerPlayer player) {
        FourDimensionalSpace.of(player).openTerminal(player);
        return true;
    }

    /** 四维空间的自动熔炼挂在这里：只要瓶子在背包里就生效，不需要开着界面 */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (entity instanceof Player player) {
            FourDimensionalSpace.of(player).processAutoSmelt(level);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("§7右键或按 §fK §7打开 §d四维空间"));
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

/** 「四维空间」终端界面：自绘底图 + 翻页按钮 + 滚轮翻页。 */
public class KleinBottleScreen extends AbstractContainerScreen<KleinBottleMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "textures/gui/four_dimensional_space.png");

    public static final int IMAGE_W = 176;
    public static final int IMAGE_H = 250;
    public static final int GRID_Y = 44;
    public static final int PLAYER_Y = 168;

    public KleinBottleScreen(KleinBottleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_W, IMAGE_H);
        this.inventoryLabelY = PLAYER_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("工作台"), b -> click(KleinBottleMenu.BUTTON_CRAFTING))
                .bounds(leftPos + 8, topPos + 24, 52, 16).build());
        addRenderableWidget(Button.builder(Component.literal("熔炉"), b -> click(KleinBottleMenu.BUTTON_FURNACE))
                .bounds(leftPos + 62, topPos + 24, 52, 16).build());
        addRenderableWidget(Button.builder(Component.literal("<"), b -> click(KleinBottleMenu.BUTTON_PAGE_PREV))
                .bounds(leftPos + 117, topPos + 24, 24, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> click(KleinBottleMenu.BUTTON_PAGE_NEXT))
                .bounds(leftPos + 143, topPos + 24, 24, 16).build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    /** 滚轮 = 翻页 */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && minecraft != null && minecraft.player != null) {
            click(scrollY > 0 ? KleinBottleMenu.BUTTON_PAGE_NEXT : KleinBottleMenu.BUTTON_PAGE_PREV);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
        String page = "页 " + (menu.getPage() + 1) + " / " + menu.getPageCount();
        String smelt = menu.isAutoSmelt() ? "自动熔炼 开" : "自动熔炼 关";
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFFEDE7F6, false);
        graphics.text(font, net.minecraft.network.chat.Component.literal(smelt),
                IMAGE_W - 8 - font.width(smelt), 8, menu.isAutoSmelt() ? 0xFF9FE1CB : 0xFF9C8FBE, false);
        graphics.text(font, net.minecraft.network.chat.Component.literal(page),
                IMAGE_W - 8 - font.width(page), 29, 0xFFFFE082, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF9C8FBE, false);
    }
}
'''

for rel, content in F.items():
    path = os.path.join(JAVA, rel.replace("/", os.sep))
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    print("wrote", rel)

# 熔炉不再有独立界面
for rel in (r"menu/KleinBottleFurnaceMenu.java", r"client/KleinBottleFurnaceScreen.java"):
    p = os.path.join(JAVA, rel.replace("/", os.sep))
    if os.path.exists(p):
        os.remove(p)
        print("removed", rel)

# MenuRegistry：去掉熔炉菜单；Zuoyanmod：去掉熔炉界面注册
p = os.path.join(JAVA, "menu", "MenuRegistry.java")
s = open(p, encoding="utf-8").read()
lines = [l for l in s.split("\n") if "KLEIN_BOTTLE_FURNACE_MENU" not in l
         and "KleinBottleFurnaceMenu" not in l]
open(p, "w", encoding="utf-8", newline="\n").write("\n".join(lines))
print("patched MenuRegistry")

p = os.path.join(JAVA, "Zuoyanmod.java")
s = open(p, encoding="utf-8").read()
lines = [l for l in s.split("\n") if "KleinBottleFurnaceScreen" not in l]
open(p, "w", encoding="utf-8", newline="\n").write("\n".join(lines))
print("patched Zuoyanmod")
