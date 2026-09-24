package org.gwfx.zuoyanmod.compat;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import org.gwfx.zuoyanmod.util.AccessoryChecks;

/**
 * Curios API 适配层。
 *
 * <p><b>类加载约束</b>：本类直接引用 Curios 的类，因此只能在
 * {@code ModList.get().isLoaded("curios")} 为真时才允许被首次加载，
 * 否则会抛 {@link NoClassDefFoundError}。所有调用方必须先做该判断
 * （入口见 {@link AccessoryChecks}）。</p>
 *
 * <p>1.20.1 适配：Curios 的库存能力是 Forge 的 {@code LazyOptional}，
 * 先 {@code resolve()} 再取用。</p>
 */
public final class CuriosCompat {

    private CuriosCompat() {
    }

    /**
     * 在玩家的饰品栏中查找指定饰品。
     *
     * @return 佩戴中的饰品堆；未佩戴返回 {@link ItemStack#EMPTY}；
     *         玩家身上没有饰品栏能力时返回 {@code null}，由调用方决定回退策略
     */
    @Nullable
    public static ItemStack findEquipped(Player player, Item item) {
        Optional<ICuriosItemHandler> handler = CuriosApi.getCuriosInventory(player).resolve();
        if (handler.isEmpty()) {
            return null;
        }
        return handler.get().findFirstCurio(item).map(SlotResult::stack).orElse(ItemStack.EMPTY);
    }

    /**
     * 把本模组的六件饰品注册为可佩戴的 Curio（空实现，仅走默认行为）。
     * 须在物品注册完成后（common setup）调用；Curios 在能力首次查询时才读取注册表，
     * 因此在 common setup 注册是安全的。
     */
    public static void registerCurioItems() {
        ICurioItem marker = new MarkerCurio();
        for (Item item : AccessoryChecks.accessoryItems()) {
            CuriosApi.registerCurio(item, marker);
        }
    }

    /**
     * 空的 Curio 行为实现：Curios 由此识别"这是一件 Curio"，
     * 从而获得右键佩戴、佩戴音效等默认行为。具体效果仍由本模组的事件处理器驱动。
     */
    private static final class MarkerCurio implements ICurioItem {
    }
}
