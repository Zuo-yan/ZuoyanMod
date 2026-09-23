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

/**
 * 微型强子对撞机容器：粒子束 A + 粒子束 B → 对撞进度 → 产物槽。
 * 两个构造是原版惯例：服务端走 MenuProvider 的 4 参版本，客户端走 MenuType 的 2 参版本再同步数据。
 * 槽位坐标与 GUI 贴图布局常量一一对应（见 tools/gen_collider_textures.py），改一处必改另一处。
 */
public class MicroHadronColliderMenu extends AbstractContainerMenu {

    public static final int SLOT_BEAM_A = 0;
    public static final int SLOT_BEAM_B = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int INVENTORY_START = 3;
    public static final int INVENTORY_END = 39;

    private final Container container;
    private final ContainerData data;

    public MicroHadronColliderMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(3), new SimpleContainerData(3));
    }

    public MicroHadronColliderMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(MenuRegistry.MICRO_HADRON_COLLIDER_MENU.get(), containerId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, 3);
        container.startOpen(playerInventory.player);

        // 两束粒子相邻（对撞区居中），产物在右
        addSlot(new Slot(container, SLOT_BEAM_A, 44, 24));
        addSlot(new Slot(container, SLOT_BEAM_B, 62, 24));
        addSlot(new Slot(container, SLOT_OUTPUT, 116, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // 产物槽只取不放
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

    /** 对撞进度 0..duration（ticks） */
    public int getProgress() {
        return data.get(0);
    }

    /** 本配方对撞时长（ticks），最低 100 防止除 0 */
    public int getDuration() {
        return Math.max(1, data.get(1));
    }

    /** 是否接收到红石信号 */
    public boolean isPowered() {
        return data.get(2) != 0;
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
            if (!moveItemStackTo(inSlot, INVENTORY_START, INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index <= SLOT_BEAM_B) {
            // A/B 地位对称：两格之间也允许互换
            if (!moveItemStackTo(inSlot, SLOT_BEAM_A, SLOT_BEAM_B + 1, false)
                    && !moveItemStackTo(inSlot, INVENTORY_START, INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(inSlot, SLOT_BEAM_A, SLOT_BEAM_B + 1, false)
                && !moveItemStackTo(inSlot, INVENTORY_START, INVENTORY_END, false)) {
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
