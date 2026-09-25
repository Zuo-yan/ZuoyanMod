package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class BeimingBlade extends SwordItem {

    public BeimingBlade(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.zuoyanmod.beiming_blade.desc1"));
        tooltip.add(Component.translatable("item.zuoyanmod.beiming_blade.desc2"));
        tooltip.add(Component.translatable("item.zuoyanmod.beiming_blade.desc3"));
        tooltip.add(Component.translatable("item.zuoyanmod.beiming_blade.desc4"));
        tooltip.add(Component.translatable("item.zuoyanmod.beiming_blade.desc5"));
    }
}
