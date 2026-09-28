package org.gwfx.zuoyanmod.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier; // 改为 Identifier
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import org.gwfx.zuoyanmod.Zuoyanmod;

public final class ZuoyanDamageTypes {

    public static final ResourceKey<DamageType> HEART_PARALYSIS = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "heart_paralysis")
    );

    /** 液态暗物质相位侵蚀：纯真伤，无视护甲与抗性（见 data/minecraft/tags/damage_type） */
    public static final ResourceKey<DamageType> DARK_MATTER = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "dark_matter")
    );

    /** 「对称破缺」黑洞坍缩爆炸：普通伤害，正常吃护甲与抗性 */
    public static final ResourceKey<DamageType> VACUUM_DECAY = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "vacuum_decay")
    );

    /** 「平行宇宙射线」湮灭：Boss 降级结算用的穿透伤害，无视护甲（bypasses_armor，见 damage_type JSON） */
    public static final ResourceKey<DamageType> MULTIVERSE_RAY = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "multiverse_ray")
    );

    /**
     * 「原始黑洞」坍缩的爆发伤害：普通伤害，正常吃护甲与抗性。
     * <p>与 {@link #VACUUM_DECAY} 性质相同、只是归因不同 —— 单开一个类型是为了让死亡消息
     * 能准确说成"被原始黑洞吞噬"，而不是笼统地显示成真空衰变。
     */
    public static final ResourceKey<DamageType> PRIMORDIAL_BLACK_HOLE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "primordial_black_hole")
    );

    private ZuoyanDamageTypes() {}
}