package org.gwfx.zuoyanmod.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 虚空共振泵容器：输入槽（燃料）+ 输出槽（暗物质粒子）+ 共振/进度同步。
 * 两个构造是原版惯例：服务端走 MenuProvider 的 4 参版本，客户端走 MenuType 的 2 参版本再同步数据。
 * 槽位坐标与 GUI 贴图布局常量一一对应（见 tools/gen_void_pump_textures.py）。
 */
public class VoidResonancePumpMenu extends AbstractContainerMenu {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    private final Container container;
    private final ContainerData data;
    private final Level level;

    public VoidResonancePumpMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(2), new SimpleContainerData(3));
    }

    public VoidResonancePumpMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(MenuRegistry.VOID_RESONANCE_PUMP_MENU.get(), containerId);
        this.container = container;
        this.data = data;
        this.level = playerInventory.player.level();
        checkContainerSize(container, 2);
        container.startOpen(playerInventory.player);

        // 两个槽并排居中，中间是产出进度条
        addSlot(new Slot(container, SLOT_INPUT, 57, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return org.gwfx.zuoyanmod.block.VoidResonancePumpBlockEntity.fuelValue(stack) > 0;
            }
        });
        addSlot(new Slot(container, SLOT_OUTPUT, 104, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // 输出槽只取不放
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 70 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 128));
        }

        addDataSlots(data);
    }

    public Level getLevel() {
        return level;
    }

    /** 共振等级 0–5（0 = 休眠） */
    public int getResonanceLevel() {
        return data.get(0);
    }

    /** 产出进度 0–100 */
    public int getProgress() {
        return data.get(1);
    }

    /** 当前共振值 0–1200 */
    public int getResonance() {
        return data.get(2);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack inSlot = slot.getItem();
        result = inSlot.copy();

        if (index == SLOT_OUTPUT) {
            if (!moveItemStackTo(inSlot, 2, 38, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index == SLOT_INPUT) {
            if (!moveItemStackTo(inSlot, 2, 38, false)) {
                return ItemStack.EMPTY;
            }
        } else if (org.gwfx.zuoyanmod.block.VoidResonancePumpBlockEntity.fuelValue(inSlot) > 0) {
            if (!moveItemStackTo(inSlot, SLOT_INPUT, SLOT_INPUT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(inSlot, SLOT_INPUT, SLOT_INPUT + 1, false)
                && !moveItemStackTo(inSlot, 2, 38, false)) {
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
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}

