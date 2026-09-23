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
        tooltip.accept(Component.literal("§6开膛手杰克的手术刀"));
        tooltip.accept(Component.literal("§7匕首类武器：攻击距离略短于剑"));
        tooltip.accept(Component.literal("§7首次击杀触发隐身；隐身持续60秒，结束后进入60秒冷却"));
        tooltip.accept(Component.literal("§7隐身期间每次击杀叠加1层，不会刷新隐身持续时间"));
        tooltip.accept(Component.literal("§e叠层效果："));
        tooltip.accept(Component.literal("§7每层使攻击伤害与攻击速度变为 2^层数 倍"));
        tooltip.accept(Component.literal("§7（1层×2 → 10伤害/11.0攻速，2层×4 → 20伤害/22.0攻速，"));
        tooltip.accept(Component.literal("§7 3层×8 → 40伤害/44.0攻速，以此类推）"));
        tooltip.accept(Component.literal("§7只在手持手术刀时生效；切换武器后加成立即失效"));
        tooltip.accept(Component.literal("§7冷却期间无法再次获得隐身；隐身结束或被驱散后，加成清空"));
    }
}