package org.gwfx.zuoyanmod.util;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.gwfx.zuoyanmod.item.ItemRegistry;

/**
 * 圣辉套装（紫金神装四件）判定。
 * <p>
 * 抽成工具类的原因：「全套圣辉套装免疫暗物质侵蚀」是本 mod 的公开承诺，
 * 而暗物质侵蚀有**两条入口**——踩进液态暗物质（{@code DarkMatterEventHandler}），
 * 以及被「真空衰变」直接附加【分子离解】效果（{@code MolecularDissolutionEffect}）。
 * 两条入口必须用同一份判定，否则新武器会悄悄绕过这条承诺。
 */
public final class HallowedSet {

    private HallowedSet() {}

    public static boolean isWearingFull(LivingEntity living) {
        return living.getItemBySlot(EquipmentSlot.HEAD).is(ItemRegistry.SHADOW_HELMET.get())
                && living.getItemBySlot(EquipmentSlot.CHEST).is(ItemRegistry.SHENG_TIAN_CHESTPLATE.get())
                && living.getItemBySlot(EquipmentSlot.LEGS).is(ItemRegistry.WIND_LEGGINGS.get())
                && living.getItemBySlot(EquipmentSlot.FEET).is(ItemRegistry.WALKER_BOOTS.get());
    }
}
