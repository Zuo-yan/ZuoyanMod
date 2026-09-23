package org.gwfx.zuoyanmod.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import org.gwfx.zuoyanmod.Zuoyanmod;

public final class ZuoyanDamageTypes {

    public static final ResourceKey<DamageType> HEART_PARALYSIS = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(Zuoyanmod.MODID, "heart_paralysis")
    );

    /** 液态暗物质相位侵蚀：纯真伤，无视护甲与抗性（见 data/minecraft/tags/damage_type） */
    public static final ResourceKey<DamageType> DARK_MATTER = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(Zuoyanmod.MODID, "dark_matter")
    );

    /** 「对称破缺」黑洞坍缩爆炸：普通伤害，正常吃护甲与抗性 */
    public static final ResourceKey<DamageType> VACUUM_DECAY = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(Zuoyanmod.MODID, "vacuum_decay")
    );

    private ZuoyanDamageTypes() {}
}
