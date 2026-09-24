package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.gwfx.zuoyanmod.sound.SoundRegistry;

import java.util.function.Consumer;

public class SpriteDrinkItem extends Item {

    public SpriteDrinkItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (entity instanceof Player player) {
            player.getPersistentData().putBoolean("zuoyan_sprite_active", true);
            player.getPersistentData().putFloat("zuoyan_sprite_bonus", 0.0F);
            player.getPersistentData().putDouble("zuoyan_sprite_last_x", player.getX());
            player.getPersistentData().putDouble("zuoyan_sprite_last_y", player.getY());
            player.getPersistentData().putDouble("zuoyan_sprite_last_z", player.getZ());
            player.getPersistentData().putFloat("zuoyan_sprite_walk_acc", 0.0F);
            player.getPersistentData().putLong("zuoyan_sprite_end_tick", player.level().getGameTime() + 20L * 15L);
            player.sendSystemMessage(Component.translatable("message.zuoyanmod.sprite_drink.triggered"));
            if (!level.isClientSide()) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundRegistry.CHILLED_DRINK.get(), SoundSource.PLAYERS, 0.9F, 1.15F);
            }
        }

        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.zuoyanmod.sprite_drink.desc1"));
        tooltip.accept(Component.translatable("item.zuoyanmod.sprite_drink.desc2"));
        tooltip.accept(Component.translatable("item.zuoyanmod.sprite_drink.desc3"));
        tooltip.accept(Component.translatable("item.zuoyanmod.sprite_drink.desc4"));
        tooltip.accept(Component.translatable("item.zuoyanmod.sprite_drink.desc5"));
        tooltip.accept(Component.translatable("item.zuoyanmod.sprite_drink.desc6"));
    }
}