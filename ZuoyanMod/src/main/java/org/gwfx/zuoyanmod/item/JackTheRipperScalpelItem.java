package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class JackTheRipperScalpelItem extends Item {

    /** 基础攻击力（1 点玩家基础 + 4 点物品附加） */
    public static final float BASE_DAMAGE = 5.0f;
    /** 基础攻击速度（4 点玩家基础 + 1.5 点物品附加） */
    public static final float BASE_ATTACK_SPEED = 5.5f;

    public JackTheRipperScalpelItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.zuoyanmod.jack_the_ripper_scalpel.desc1"));
        tooltip.accept(Component.translatable("item.zuoyanmod.jack_the_ripper_scalpel.desc2"));
        tooltip.accept(Component.translatable("item.zuoyanmod.jack_the_ripper_scalpel.desc3"));
        tooltip.accept(Component.translatable("item.zuoyanmod.jack_the_ripper_scalpel.desc4"));
        tooltip.accept(Component.translatable("item.zuoyanmod.jack_the_ripper_scalpel.desc5"));
        tooltip.accept(Component.translatable("item.zuoyanmod.jack_the_ripper_scalpel.desc6"));
        tooltip.accept(Component.translatable("item.zuoyanmod.jack_the_ripper_scalpel.desc7"));
        tooltip.accept(Component.translatable("item.zuoyanmod.jack_the_ripper_scalpel.desc8"));
        tooltip.accept(Component.translatable("item.zuoyanmod.jack_the_ripper_scalpel.desc9"));
        tooltip.accept(Component.translatable("item.zuoyanmod.jack_the_ripper_scalpel.desc10"));
    }
}