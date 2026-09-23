package org.gwfx.zuoyanmod.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class HerculesBowItem extends BowItem {

    public static final String BLESSING_TAG = "HerculesBlessing";

    public enum BlessingType {
        ARTEMIS("猎神·阿尔忒弥斯"),
        HELIOS("太阳神·赫利俄斯"),
        CERBERUS("地狱三头犬·刻耳柏洛斯"),
        HIPPOLYTA("亚马逊女王·希波吕忒");

        private final String displayName;

        BlessingType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static final int BASE_DRAW_TIME = 20;
    public static final float FULL_CHARGE_DAMAGE = 25.0f;
    private static final float VANILLA_FULL_CHARGE_DAMAGE = 11.5f;
    public static final float DAMAGE_MULTIPLIER = FULL_CHARGE_DAMAGE / VANILLA_FULL_CHARGE_DAMAGE;

    public HerculesBowItem(Properties properties) {
        super(properties);
    }

    /** 1.20.1：耐久修复直接覆写 isValidRepairItem（26.x 的 Properties#repairable 在 1.20.1 不存在） */
    @Override
    public boolean isValidRepairItem(ItemStack bow, ItemStack material) {
        return material.is(ItemRegistry.VIOLET_GOLD_INGOT.get());
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    // 1.20.1 的 releaseUsing 返回 void（26.x 是 boolean）
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            int drawDuration = getUseDuration(stack) - timeLeft;
            float charge = calculatePowerForTime(drawDuration, stack);

            if (charge >= 0.1f) {
                // 1.20.1 的 Arrow 构造是 (Level, LivingEntity)，pickup 栈由射手当前物品决定（26.x 才加了 ItemStack 参数）
                Arrow arrow = new Arrow(level, player);
                arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, charge * 3.0f, 1.0f);

                if (charge >= 1.0f) {
                    arrow.setCritArrow(true);
                }

                arrow.setBaseDamage(3.6D * DAMAGE_MULTIPLIER);

                if (!level.isClientSide) {
                    level.addFreshEntity(arrow);
                }

                // 1.20.1 的 hurtAndBreak 第三参是"破坏时回调"（26.x 直接传 EquipmentSlot）
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
            }
        }
    }

    private static float calculatePowerForTime(int time, ItemStack stack) {
        int effectiveDrawTime = BASE_DRAW_TIME;
        BlessingType blessing = getBlessingType(stack);
        if (blessing == BlessingType.ARTEMIS) {
            effectiveDrawTime = BASE_DRAW_TIME / 2;
        }

        float f = (float) time / (float) effectiveDrawTime;
        f = (f * f + f * 2.0f) / 3.0f;
        if (f > 1.0f) {
            f = 1.0f;
        }
        return f;
    }

    public static void assignBlessingIfMissing(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(BLESSING_TAG)) {
            BlessingType randomBlessing = BlessingType.values()[ThreadLocalRandom.current().nextInt(BlessingType.values().length)];
            tag.putString(BLESSING_TAG, randomBlessing.name());
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    public static BlessingType getBlessingType(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(BLESSING_TAG)) {
            try {
                return BlessingType.valueOf(tag.getString(BLESSING_TAG));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§6赫拉克勒斯之弓"));
        tooltip.add(Component.literal("§7赫拉克勒斯十二试炼的神圣遗物"));

        BlessingType blessing = getBlessingType(stack);
        if (blessing != null) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("§e赐福效果:"));
            switch (blessing) {
                case ARTEMIS:
                    tooltip.add(Component.literal("§a猎神·阿尔忒弥斯赐福"));
                    tooltip.add(Component.literal("§7远程攻击必定暴击"));
                    tooltip.add(Component.literal("§7暴击时额外提升伤害"));
                    tooltip.add(Component.literal("§7拉弓蓄力速度翻倍"));
                    break;
                case HELIOS:
                    tooltip.add(Component.literal("§c太阳神·赫利俄斯赐福"));
                    tooltip.add(Component.literal("§7攻击完全忽略敌方护甲"));
                    tooltip.add(Component.literal("§7命中目标后将其点燃，并持续燃烧"));
                    break;
                case CERBERUS:
                    tooltip.add(Component.literal("§5地狱三头犬·刻耳柏洛斯赐福"));
                    tooltip.add(Component.literal("§7命中时召唤三只猎犬撕咬目标"));
                    tooltip.add(Component.literal("§7持续20秒"));
                    break;
                case HIPPOLYTA:
                    tooltip.add(Component.literal("§d亚马逊女王·希波吕忒赐福"));
                    tooltip.add(Component.literal("§7额外造成目标已损生命值25%的伤害"));
                    break;
            }
        } else {
            tooltip.add(Component.literal("§7赐福效果待激活..."));
        }

        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§7首次获取时随机赋予一项永久性赐福"));
    }
}
