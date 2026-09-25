package org.gwfx.zuoyanmod.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.gwfx.zuoyanmod.Zuoyanmod;

/**
 * 世界生成相关的注册表。
 *
 * <p>自定义 {@link StructureType}：{@code zuoyanmod:land_checked_jigsaw}，即
 * {@link LandCheckedJigsawStructure}。它的 JSON 字段和 {@code minecraft:jigsaw} 一模一样，
 * 只在生成前多加一道"地表不能是水"的检查。用法：把结构定义里的 {@code "type"} 从
 * {@code "minecraft:jigsaw"} 改成 {@code "zuoyanmod:land_checked_jigsaw"} 即可。
 *
 * <p>自定义 {@link ChunkGenerator} 类型：{@code zuoyanmod:realm_flat}，即
 * {@link RealmFlatChunkGenerator}。它是原版 {@code minecraft:flat} 的超集，JSON 的
 * {@code settings} 字段格式完全一致，额外在装饰阶段铺一层分层矿物带。
 */
public final class WorldgenRegistry {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Zuoyanmod.MODID);

    /**
     * 区块生成器类型的注册表是 {@code Registry<MapCodec<? extends ChunkGenerator>>}，
     * 所以泛型参数是这个 MapCodec 而不是 ChunkGenerator 本身。
     */
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Zuoyanmod.MODID);

    /** 「只在陆地生成」的拼图结构类型。 */
    public static final DeferredHolder<StructureType<?>, StructureType<LandCheckedJigsawStructure>> LAND_CHECKED_JIGSAW =
            STRUCTURE_TYPES.register("land_checked_jigsaw", WorldgenRegistry::structureType);

    /** 超平坦世界用的区块生成器：原版 flat + 分层矿物带。 */
    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<RealmFlatChunkGenerator>> REALM_FLAT =
            CHUNK_GENERATORS.register("realm_flat", () -> RealmFlatChunkGenerator.CODEC);

    private WorldgenRegistry() {
    }

    /** {@link StructureType} 是单方法接口，直接返回该类自己的 CODEC。 */
    private static StructureType<LandCheckedJigsawStructure> structureType() {
        return () -> LandCheckedJigsawStructure.CODEC;
    }

    public static void register(IEventBus modBus) {
        STRUCTURE_TYPES.register(modBus);
        CHUNK_GENERATORS.register(modBus);
    }
}
