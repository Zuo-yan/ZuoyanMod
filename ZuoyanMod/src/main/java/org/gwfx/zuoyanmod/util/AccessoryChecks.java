package org.gwfx.zuoyanmod.util;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import org.gwfx.zuoyanmod.compat.CuriosCompat;

/**
 * 饰品佩戴判定工具。
 *
 * <p>规则：游戏内装有 Curios API 时，饰品必须佩戴进饰品栏才生效；
 * 未安装 Curios 时，饰品放在背包（含主手 / 副手）中即生效。</p>
 *
 * <p>所有饰品事件处理器应通过本类做"饰品是否生效"判定，
 * 不要再各自遍历玩家背包。</p>
 *
 * <p>1.20.1 适配：Forge 没有 NeoForge 的 {@code ModList.get()}，改用
 * {@code net.minecraftforge.fml.ModList}；Curios 的库存能力是
 * {@code LazyOptional}，取用时要先 {@code resolve()}。</p>
 */
public final class AccessoryChecks {

    public static final String CURIOS_MODID = "curios";

    /** 本模组的六件饰品（供 Curios 注册与文案提示复用）。 */
    public static List<Item> accessoryItems() {
        return List.of(
                org.gwfx.zuoyanmod.item.ItemRegistry.MING_DAO_SI_MING.get(),
                org.gwfx.zuoyanmod.item.ItemRegistry.RING_OF_KILLS.get(),
                org.gwfx.zuoyanmod.item.ItemRegistry.WAN_HUI_RING.get(),
                org.gwfx.zuoyanmod.item.ItemRegistry.VOODOO_NECKLACE.get(),
                org.gwfx.zuoyanmod.item.ItemRegistry.YEMENGADE_VENOM_FANG.get(),
                org.gwfx.zuoyanmod.item.ItemRegistry.COUNTER_BELT.get());
    }

    private AccessoryChecks() {
    }

    /** 游戏内是否装有 Curios API。 */
    public static boolean isCuriosLoaded() {
        return ModList.get().isLoaded(CURIOS_MODID);
    }

    /** 饰品当前是否处于"生效中"（有 Curios：佩戴在饰品栏；无 Curios：在背包里）。 */
    public static boolean isEquipped(Player player, Item item) {
        return !findEquippedStack(player, item).isEmpty();
    }

    /**
     * 查找当前生效中的饰品堆。
     * 有 Curios 时只查饰品栏；无 Curios 时查背包。
     *
     * @return 生效中的物品堆，未找到返回 {@link ItemStack#EMPTY}
     */
    public static ItemStack findEquippedStack(Player player, Item item) {
        if (isCuriosLoaded()) {
            ItemStack stack = CuriosCompat.findEquipped(player, item);
            if (stack != null) {
                // 有饰品栏能力：只认饰品栏里的，背包里不算生效（哪怕饰品栏里是空的）
                return stack;
            }
            // 没有饰品栏能力（极少见的异常情况）：回退背包判定，避免饰品凭空失效
        }
        return findInInventory(player, item);
    }

    /**
     * 背包判定：主手 → 副手 → 背包格子。
     * 与事件处理器原来的私有实现逐行等价。
     */
    private static ItemStack findInInventory(Player player, Item item) {
        if (player.getMainHandItem().is(item)) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().is(item)) {
            return player.getOffhandItem();
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 往 tooltip 追加"生效方式"提示行（随是否装有 Curios 动态变化）：
     * 有饰品栏时提示"需佩戴在饰品栏生效"，否则提示"放入背包即生效"。
     *
     * <p>1.20.1 的 tooltip 是 {@code List<Component>}，用 {@code list::add} 适配成 Consumer。</p>
     */
    public static void appendEquipHint(List<Component> tooltip) {
        Consumer<Component> sink = tooltip::add;
        if (isCuriosLoaded()) {
            sink.accept(Component.translatable("tooltip.zuoyanmod.accessory.need_equipped"));
        } else {
            sink.accept(Component.translatable("tooltip.zuoyanmod.accessory.inventory_ok"));
        }
    }
}
