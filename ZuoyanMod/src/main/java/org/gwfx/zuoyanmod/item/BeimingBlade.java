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
        tooltip.add(Component.literal("§1§l北冥狂刃"));
        tooltip.add(Component.literal("§7§o战之狂热：攻击敌方时进入狂热状态"));
        tooltip.add(Component.literal("§7§o每次攻击提升最大生命值并扣除自身10%生命值"));
        tooltip.add(Component.literal("§7§o对目标造成自身已损生命值60%的额外伤害"));
        tooltip.add(Component.literal("§7§o停止攻击10秒后退出狂热状态"));
    }
}
