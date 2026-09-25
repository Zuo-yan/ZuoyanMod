package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.gwfx.zuoyanmod.item.FourDimensionalSpaceCapability;

import javax.annotation.Nullable;
import java.util.List;

/** 克莱因瓶：**一把钥匙**。右键或背包里按 K，打开属于你自己的「四维空间」。 */
public class KleinBottleItem extends Item {

    /** 打开键的 keybind ID，与客户端 {@code KleinBottleKeyHandler} 注册的 KeyMapping 保持一致。 */
    private static final String KEY_ID = "key.zuoyanmod.open_klein_bottle";

    public KleinBottleItem(Properties properties) {
        super(properties);
    }

    // 1.20.1 的 Item#use 返回 InteractionResultHolder<ItemStack>
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            KleinBottleItem.openFor(serverPlayer);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    public static boolean openFor(net.minecraft.server.level.ServerPlayer player) {
        FourDimensionalSpace.of(player).openTerminal(player);
        return true;
    }

    /**
     * 推进随身熔炉。
     *
     * <p>挂在这里而不是菜单里，是为了<b>关着界面也在烧</b>——菜单一关就没了。
     * 1.20.1 的 {@code Item#inventoryTick} **双端都会调**（26.3 只在服务端调），
     * 签名也不同（多 slot/selected 两个参数），所以这里要显式判服务端。
     * 熔炉内部有空闲早退，没东西在烧时这点开销可以忽略。
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)) {
            return;
        }
        FourDimensionalSpace.of(player).furnace().tick(serverLevel);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.zuoyanmod.klein_bottle.desc1"));
        tooltip.add(Component.translatable("item.zuoyanmod.klein_bottle.desc2"));
        // desc3 传入 keybind 组件，客户端渲染时自动解析为当前实际绑定的按键（改键后描述同步变化）
        tooltip.add(Component.translatable("item.zuoyanmod.klein_bottle.desc3",
                Component.keybind(KEY_ID)));
    }
}
