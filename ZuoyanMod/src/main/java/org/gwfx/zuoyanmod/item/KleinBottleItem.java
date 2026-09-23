package org.gwfx.zuoyanmod.item;

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
import org.jspecify.annotations.Nullable;

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

    /**
     * 推进随身熔炉。
     *
     * <p>挂在这里而不是菜单里，是为了<b>关着界面也在烧</b>——菜单一关就没了。
     * 26.3 的 {@code Item#inventoryTick} 只在服务端调（{@code ItemStack#inventoryTick}
     * 里有 {@code level instanceof ServerLevel} 判断），所以不用再判一次 {@code isClientSide}。
     * 熔炉内部有空闲早退，没东西在烧时这点开销可以忽略。
     */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (entity instanceof Player player) {
            FourDimensionalSpace.of(player).furnace().tick(level);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.zuoyanmod.klein_bottle.desc1"));
        tooltip.accept(Component.translatable("item.zuoyanmod.klein_bottle.desc2"));
        tooltip.accept(Component.translatable("item.zuoyanmod.klein_bottle.desc3"));
    }
}
