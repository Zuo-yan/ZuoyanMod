package org.gwfx.zuoyanmod.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import org.gwfx.zuoyanmod.Zuoyanmod;

/**
 * 世界生成相关的注册表。
 *
 * <p>目前只有一个自定义 {@link StructureType}：{@code zuoyanmod:land_checked_jigsaw}，
 * 即 {@link LandCheckedJigsawStructure}。它的 JSON 字段和 {@code minecraft:jigsaw}
 * 一模一样（按 1.20.1 的字段集），只在生成前多加一道"地表不能是水"的检查。</p>
 *
 * <p>用法：把结构定义里的 {@code "type"} 从 {@code "minecraft:jigsaw"} 改成
 * {@code "zuoyanmod:land_checked_jigsaw"} 即可，其余字段不用动。</p>
 */
public final class WorldgenRegistry {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Zuoyanmod.MODID);

    /** 「只在陆地生成」的拼图结构类型。 */
    public static final RegistryObject<StructureType<LandCheckedJigsawStructure>> LAND_CHECKED_JIGSAW =
            STRUCTURE_TYPES.register("land_checked_jigsaw", WorldgenRegistry::structureType);

    private WorldgenRegistry() {
    }

    /** 1.20.1 的 {@link StructureType} 是单方法接口，直接返回该类自己的 Codec。 */
    private static StructureType<LandCheckedJigsawStructure> structureType() {
        return () -> LandCheckedJigsawStructure.CODEC;
    }

    public static void register(IEventBus modBus) {
        STRUCTURE_TYPES.register(modBus);
    }
}
