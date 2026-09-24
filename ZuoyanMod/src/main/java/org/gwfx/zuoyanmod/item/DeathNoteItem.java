package org.gwfx.zuoyanmod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DeathNoteItem extends Item {

    public DeathNoteItem(Properties properties) {
        super(properties);
    }

    // 1.20.1 的 Item#use 返回 InteractionResultHolder<ItemStack>
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            // 屏幕是纯客户端概念，实现放在 client 包（见 DeathNoteClient 的注释）。
            // 这里绝不能出现 net.minecraft.client.* 的类型，否则服务端加载模组时
            // 会因为校验 openScreen 而被迫加载 Screen，直接崩在专用服务端上。
            org.gwfx.zuoyanmod.client.DeathNoteClient.openScreen(stack);
        }
        return InteractionResultHolder.success(stack);
    }
}
