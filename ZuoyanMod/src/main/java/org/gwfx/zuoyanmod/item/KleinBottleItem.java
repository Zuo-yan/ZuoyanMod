package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** 克莱因瓶：**一把钥匙**。随身携带时按打开键（默认 K，可在按键设置中修改），打开属于你自己的「四维空间」。 */
public class KleinBottleItem extends Item {

    /** 打开键的 keybind ID，与服务端 {@code KleinBottleKeyHandler} 中注册的 KeyMapping 保持一致。 */
    private static final String KEY_ID = "key.zuoyanmod.open_klein_bottle";

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
     * 1.21.1 的 {@code Item#inventoryTick} 客户端和服务端都会调（旧行为"只在服务端调"
     * 已经不存在），所以必须自己判一次 {@code level instanceof ServerLevel}。
     * 熔炉内部有空闲早退，没东西在烧时这点开销可以忽略。
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level instanceof ServerLevel serverLevel && entity instanceof Player player) {
            FourDimensionalSpace.of(player).furnace().tick(serverLevel);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.zuoyanmod.klein_bottle.desc1"));
        tooltip.add(Component.translatable("item.zuoyanmod.klein_bottle.desc2"));
        // desc3 传入 keybind 组件，客户端渲染时自动解析为当前实际绑定的按键（改键后描述同步变化）
        tooltip.add(Component.translatable("item.zuoyanmod.klein_bottle.desc3",
                Component.keybind(KEY_ID)));
    }
}
