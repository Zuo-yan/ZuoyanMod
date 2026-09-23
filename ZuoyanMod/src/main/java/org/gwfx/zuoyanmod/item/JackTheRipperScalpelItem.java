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
        tooltip.add(Component.literal("§6开膛手杰克的手术刀"));
        tooltip.add(Component.literal("§7匕首类武器：攻击距离略短于剑"));
        tooltip.add(Component.literal("§7首次击杀触发隐身；隐身持续60秒，结束后进入60秒冷却"));
        tooltip.add(Component.literal("§7隐身期间每次击杀叠加1层，不会刷新隐身持续时间"));
        tooltip.add(Component.literal("§e叠层效果："));
        tooltip.add(Component.literal("§7每层使攻击伤害与攻击速度变为 2^层数 倍"));
        tooltip.add(Component.literal("§7（1层×2 → 10伤害/11.0攻速，2层×4 → 20伤害/22.0攻速，"));
        tooltip.add(Component.literal("§7 3层×8 → 40伤害/44.0攻速，以此类推）"));
        tooltip.add(Component.literal("§7只在手持手术刀时生效；切换武器后加成立即失效"));
        tooltip.add(Component.literal("§7冷却期间无法再次获得隐身；隐身结束或被驱散后，加成清空"));
    }
}
