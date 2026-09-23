package org.gwfx.zuoyanmod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.gwfx.zuoyanmod.client.DeathNoteScreen;

public class DeathNoteItem extends Item {

    public DeathNoteItem(Properties properties) {
        super(properties);
    }

    // 1.20.1 的 Item#use 返回 InteractionResultHolder<ItemStack>
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            openScreen(stack);
        }
        return InteractionResultHolder.success(stack);
    }

    private static void openScreen(ItemStack stack) {
        // 1.20.1 直接在 Minecraft 上 setScreen（26.x 的 .gui.setScreen 是 26.x 的新分层）
        net.minecraft.client.Minecraft.getInstance().setScreen(new DeathNoteScreen(stack));
    }
}
