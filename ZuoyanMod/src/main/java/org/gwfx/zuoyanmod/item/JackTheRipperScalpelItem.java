package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class JackTheRipperScalpelItem extends SwordItem {

    /** 基础攻击力（1 点玩家基础 + 4 点物品附加） */
    public static final float BASE_DAMAGE = 5.0f;
    /** 基础攻击速度（4 点玩家基础 + 1.5 点物品附加） */
    public static final float BASE_ATTACK_SPEED = 5.5f;

    public JackTheRipperScalpelItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.zuoyanmod.jack_the_ripper_scalpel.desc1"));
        tooltip.add(Component.translatable("item.zuoyanmod.jack_the_ripper_scalpel.desc2"));
        tooltip.add(Component.translatable("item.zuoyanmod.jack_the_ripper_scalpel.desc3"));
        tooltip.add(Component.translatable("item.zuoyanmod.jack_the_ripper_scalpel.desc4"));
        tooltip.add(Component.translatable("item.zuoyanmod.jack_the_ripper_scalpel.desc5"));
        tooltip.add(Component.translatable("item.zuoyanmod.jack_the_ripper_scalpel.desc6"));
        tooltip.add(Component.translatable("item.zuoyanmod.jack_the_ripper_scalpel.desc7"));
        tooltip.add(Component.translatable("item.zuoyanmod.jack_the_ripper_scalpel.desc8"));
        tooltip.add(Component.translatable("item.zuoyanmod.jack_the_ripper_scalpel.desc9"));
        tooltip.add(Component.translatable("item.zuoyanmod.jack_the_ripper_scalpel.desc10"));
    }
}
