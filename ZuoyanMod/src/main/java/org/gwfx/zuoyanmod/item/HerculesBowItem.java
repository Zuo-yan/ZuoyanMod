package org.gwfx.zuoyanmod.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.gwfx.zuoyanmod.entity.LightSpiritArrow;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class HerculesBowItem extends BowItem {

    public static final String BLESSING_TAG = "HerculesBlessing";

    /**
     * 四项赐福。这里存的是 <b>lang 键</b>而不是中文名 —— 原因是 displayName 只用于显示，
     * 而存档里记的是 {@link #name()}（枚举名 ARTEMIS 等），两者本就解耦，
     * 所以把显示文案抽到 lang 既不影响旧存档，也能让英文语言下正常显示英文名。
     */
    public enum BlessingType {
        ARTEMIS("item.zuoyanmod.hercules_bow.blessing.artemis"),
        HELIOS("item.zuoyanmod.hercules_bow.blessing.helios"),
        CERBERUS("item.zuoyanmod.hercules_bow.blessing.cerberus"),
        HIPPOLYTA("item.zuoyanmod.hercules_bow.blessing.hippolyta");

        private final String translationKey;

        BlessingType(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return translationKey;
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

    /** 光灵箭由神圣光灵凝聚而成，无需背包中备有箭矢即可拉弓。 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionResultHolder<ItemStack> hook =
                net.minecraftforge.event.ForgeEventFactory.onArrowNock(stack, level, player, hand, true);
        if (hook != null) {
            return hook;
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    // 1.20.1 的 releaseUsing 返回 void（26.x 是 boolean）
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            int drawDuration = getUseDuration(stack) - timeLeft;
            drawDuration = net.minecraftforge.event.ForgeEventFactory.onArrowLoose(stack, level, player, drawDuration, true);
            if (drawDuration < 0) {
                return;
            }

            float charge = calculatePowerForTime(drawDuration, stack);

            if (charge >= 0.1f) {
                if (!level.isClientSide) {
                    // 发射光灵箭：不消耗真实箭矢，箭体无法被拾取，落地后化作光尘消散。
                    // 1.20.1 的 Arrow 构造是 (Level, LivingEntity)。
                    LightSpiritArrow arrow = new LightSpiritArrow(level, player);
                    arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, charge * 3.0f, 1.0f);

                    if (charge >= 1.0f) {
                        arrow.setCritArrow(true);
                    }

                    arrow.setBaseDamage(3.6D * DAMAGE_MULTIPLIER);
                    level.addFreshEntity(arrow);
                }

                level.playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.ARROW_SHOOT,
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + charge * 0.5F);
                player.awardStat(Stats.ITEM_USED.get(this));

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
        tooltip.add(Component.translatable("item.zuoyanmod.hercules_bow.desc1"));
        tooltip.add(Component.translatable("item.zuoyanmod.hercules_bow.desc2"));
        tooltip.add(Component.translatable("item.zuoyanmod.hercules_bow.desc3"));

        BlessingType blessing = getBlessingType(stack);
        if (blessing != null) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.translatable("item.zuoyanmod.hercules_bow.blessing_header"));
            switch (blessing) {
                case ARTEMIS:
                    tooltip.add(Component.translatable("item.zuoyanmod.hercules_bow.artemis.title"));
                    tooltip.add(Component.translatable("item.zuoyanmod.hercules_bow.artemis.desc1"));
                    tooltip.add(Component.translatable("item.zuoyanmod.hercules_bow.artemis.desc2"));
                    tooltip.add(Component.translatable("item.zuoyanmod.hercules_bow.artemis.desc3"));
                    break;
                case HELIOS:
                    tooltip.add(Component.translatable("item.zuoyanmod.hercules_bow.helios.title"));
                    tooltip.add(Component.translatable("item.zuoyanmod.hercules_bow.helios.desc1"));
                    tooltip.add(Component.translatable("item.zuoyanmod.hercules_bow.helios.desc2"));
                    break;
                case CERBERUS:
                    tooltip.add(Component.translatable("item.zuoyanmod.hercules_bow.cerberus.title"));
                    tooltip.add(Component.translatable("item.zuoyanmod.hercules_bow.cerberus.desc1"));
                    tooltip.add(Component.translatable("item.zuoyanmod.hercules_bow.cerberus.desc2"));
                    break;
                case HIPPOLYTA:
                    tooltip.add(Component.translatable("item.zuoyanmod.hercules_bow.hippolyta.title"));
                    tooltip.add(Component.translatable("item.zuoyanmod.hercules_bow.hippolyta.desc1"));
                    break;
            }
        } else {
            tooltip.add(Component.translatable("item.zuoyanmod.hercules_bow.blessing_pending"));
        }

        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("item.zuoyanmod.hercules_bow.desc4"));
    }
}
