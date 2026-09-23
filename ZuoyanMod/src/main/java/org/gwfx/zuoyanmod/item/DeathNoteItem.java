package org.gwfx.zuoyanmod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.gwfx.zuoyanmod.client.DeathNoteScreen;

public class DeathNoteItem extends Item {

    public DeathNoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            openScreen(player.getItemInHand(hand));
        }
        return InteractionResult.SUCCESS;
    }

    private static void openScreen(ItemStack stack) {
        net.minecraft.client.Minecraft.getInstance().gui.setScreen(new DeathNoteScreen(stack));
    }
}