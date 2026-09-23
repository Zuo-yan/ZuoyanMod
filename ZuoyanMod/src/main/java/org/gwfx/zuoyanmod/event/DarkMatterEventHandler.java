package org.gwfx.zuoyanmod.event;

import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.effect.EffectRegistry;
import org.gwfx.zuoyanmod.fluid.FluidRegistry;
import org.gwfx.zuoyanmod.item.HallowedSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 液态暗物质惩罚逻辑（物理层配置见 DarkMatterFluidType）：
 * 1. 极端引力抓取：强制下沉、跳跃失效、鞘翅强制收起、水平速度衰减（泥浆拖拽）
 * 2. 相位侵蚀：伤害主体为【分子离解】效果（见 MolecularDissolutionEffect，每秒 4 点真伤）
 * 3. 分子离解：施加自定义 debuff + DARKNESS/BLINDNESS 视觉扭曲；剥夺所有增益效果
 * 4. 极端熵增：每半秒粉碎盔甲耐久；每 5 秒随机剥离一件装备的一级附魔
 * 5. 反制：穿戴全套圣辉套装（紫金神装）免疫全部惩罚，可正常游动
 * <p>
 * 1.20.1 适配：EntityTickEvent.Post→{@code LivingEvent.LivingTickEvent}；
 * DataComponents.ENCHANTMENTS→{@link EnchantmentHelper}；hurtAndBreak 的槽位参数
 * 换成 Consumer（1.20.1 没有 EquipmentSlot 重载）。
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID)
public class DarkMatterEventHandler {

    /** 每半秒耐久粉碎量 */
    private static final int DURABILITY_SHRED = 2;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        Level level = living.level();
        if (level.isClientSide) return;

        FluidState fluidState = level.getFluidState(living.blockPosition());
        if (!fluidState.getType().isSame(FluidRegistry.DARK_MATTER.get())) return;

        // 反制：全套圣辉套装 → 完全免疫，可正常游动
        if (hasFullHallowedSet(living)) return;

        long gameTime = level.getGameTime();
        var motion = living.getDeltaMovement();

        // 1. 极端引力抓取：压制跳跃/上浮，水平拖拽。
        //    （1.20.1 没有 stopFallFlying，鞘翅只靠下面的恒定下沉压制）
        living.setDeltaMovement(
                motion.x * 0.3D,
                Math.min(motion.y, -0.055D), // 恒定向下：跳跃与游泳上浮被完全覆盖
                motion.z * 0.3D
        );

        // 3. 分子离解：每秒施加/续期（80 tick ≈ 4 秒侵蚀伤害跳 4 次）
        //    伤害主体在 MolecularDissolutionEffect.applyEffectTick 中：每跳 4 点相位侵蚀真伤。
        //    离开液体后残留的离解效果仍会继续侵蚀数秒。
        //    视觉扭曲（黑暗 + 边缘侵蚀）随效果一并施加。
        if (gameTime % 20L == 0L) {
            living.addEffect(new MobEffectInstance(EffectRegistry.MOLECULAR_DISSOLUTION.get(), 80, 0, false, false));
            living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, false));
        }

        // 增益剥夺：每半秒剥离所有有益效果（有序信息被暗物质消除）
        if (gameTime % 10L == 0L) {
            List<MobEffectInstance> beneficial = new ArrayList<>();
            for (MobEffectInstance instance : living.getActiveEffects()) {
                if (instance.getEffect().isBeneficial()) {
                    beneficial.add(instance);
                }
            }
            for (MobEffectInstance instance : beneficial) {
                living.removeEffect(instance.getEffect());
            }
        }

        // 窒息压迫：氧气急速耗尽，归零后溺水伤害
        int air = living.getAirSupply();
        living.setAirSupply(Math.max(-20, air - 8));
        if (living.getAirSupply() <= -20 && gameTime % 20L == 0L) {
            living.hurt(level.damageSources().drown(), 2.0F);
        }

        // 4. 极端熵增：每半秒潮汐剪切粉碎盔甲耐久
        if (gameTime % 10L == 0L) {
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack armor = living.getItemBySlot(slot);
                if (!armor.isEmpty() && armor.isDamageableItem()) {
                    armor.hurtAndBreak(DURABILITY_SHRED, living, e -> e.broadcastBreakEvent(slot));
                }
            }
        }

        // 极端熵增：每 5 秒随机剥离一件装备（主手 + 盔甲）的一级附魔
        if (gameTime % 100L == 0L) {
            List<ItemStack> enchanted = new ArrayList<>();
            ItemStack mainHand = living.getMainHandItem();
            if (hasEnchantments(mainHand)) enchanted.add(mainHand);
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack armor = living.getItemBySlot(slot);
                if (hasEnchantments(armor)) enchanted.add(armor);
            }
            if (!enchanted.isEmpty()) {
                ItemStack target = enchanted.get(living.getRandom().nextInt(enchanted.size()));
                stripOneEnchantmentLevel(target);
            }
        }
    }

    /** 全套圣辉套装判定（紫金神装四件）——与「分子离解」效果共用同一份判定 */
    private static boolean hasFullHallowedSet(LivingEntity living) {
        return HallowedSet.isWearingFull(living);
    }

    private static boolean hasEnchantments(ItemStack stack) {
        return !EnchantmentHelper.getEnchantments(stack).isEmpty();
    }

    /** 随机剥离一级附魔（等级归零则整体移除）。1.20.1 的附魔读写走 EnchantmentHelper 的 Map 视图 */
    private static void stripOneEnchantmentLevel(ItemStack stack) {
        Map<net.minecraft.world.item.enchantment.Enchantment, Integer> enchantments =
                EnchantmentHelper.getEnchantments(stack);
        if (enchantments.isEmpty()) return;

        List<Map.Entry<net.minecraft.world.item.enchantment.Enchantment, Integer>> entries = new ArrayList<>(enchantments.entrySet());
        Map.Entry<net.minecraft.world.item.enchantment.Enchantment, Integer> entry =
                entries.get(RandomSource.create().nextInt(entries.size()));

        Map<net.minecraft.world.item.enchantment.Enchantment, Integer> updated = new HashMap<>(enchantments);
        int newLevel = entry.getValue() - 1;
        if (newLevel <= 0) {
            updated.remove(entry.getKey());
        } else {
            updated.put(entry.getKey(), newLevel);
        }
        EnchantmentHelper.setEnchantments(updated, stack);
    }

}
