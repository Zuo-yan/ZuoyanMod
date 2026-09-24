package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class YemengadeVenomFangItem extends net.minecraft.world.item.Item {

    public YemengadeVenomFangItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§5耶梦加得的毒牙"));
        tooltip.add(Component.literal("§7体力未满时受到攻击，有60%概率使攻击者中毒4秒"));
        tooltip.add(Component.literal("§7若攻击者已带有中毒效果，则触发§c尘世巨蟒§7："));
        tooltip.add(Component.literal("§7反伤等同于自身护甲值的伤害"));
        org.gwfx.zuoyanmod.util.AccessoryChecks.appendEquipHint(tooltip);
    }
}
