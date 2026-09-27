package org.gwfx.zuoyanmod.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.gwfx.zuoyanmod.Zuoyanmod;

/**
 * 世界生成相关的注册表。
 *
 * <p>自定义 {@link StructureType}：
 * <ul>
 *   <li>{@code zuoyanmod:land_checked_jigsaw}（{@link LandCheckedJigsawStructure}）——
 *       字段和 {@code minecraft:jigsaw} 一模一样，只在生成前多加一道"地表不能是水"的检查；</li>
 *   <li>{@code zuoyanmod:monarch_citadel}（{@link MonarchCitadelStructure}）——
 *       湮灭王座遗迹：地表检查后用代码程序化拼装大殿/塔楼/前庭（不走 jigsaw，
 *       每次生成按种子随机变化），仿 When Dungeons Arise 的做法。</li>
 * </ul>
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

    /** 湮灭王座遗迹的三类拼装件（大殿 / 塔楼 / 前庭），各自负责自己的存档与生成。 */
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, Zuoyanmod.MODID);

    /** 「只在陆地生成」的拼图结构类型。 */
    public static final DeferredHolder<StructureType<?>, StructureType<LandCheckedJigsawStructure>> LAND_CHECKED_JIGSAW =
            STRUCTURE_TYPES.register("land_checked_jigsaw", WorldgenRegistry::structureType);

    /** 超平坦世界用的区块生成器：原版 flat + 分层矿物带。 */
    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<RealmFlatChunkGenerator>> REALM_FLAT =
            CHUNK_GENERATORS.register("realm_flat", () -> RealmFlatChunkGenerator.CODEC);

    /** 湮灭王座遗迹结构类型（代码程序化拼装）。 */
    public static final DeferredHolder<StructureType<?>, StructureType<MonarchCitadelStructure>> MONARCH_CITADEL =
            STRUCTURE_TYPES.register("monarch_citadel", WorldgenRegistry::monarchCitadelType);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> CITADEL_HALL =
            STRUCTURE_PIECE_TYPES.register("citadel_hall",
                    () -> (StructurePieceType.ContextlessType) MonarchCitadelPieces.HallPiece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> CITADEL_TOWER =
            STRUCTURE_PIECE_TYPES.register("citadel_tower",
                    () -> (StructurePieceType.ContextlessType) MonarchCitadelPieces.TowerPiece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> CITADEL_COURTYARD =
            STRUCTURE_PIECE_TYPES.register("citadel_courtyard",
                    () -> (StructurePieceType.ContextlessType) MonarchCitadelPieces.CourtyardPiece::new);

    private WorldgenRegistry() {
    }

    /** {@link StructureType} 是单方法接口，直接返回该类自己的 CODEC。 */
    private static StructureType<LandCheckedJigsawStructure> structureType() {
        return () -> LandCheckedJigsawStructure.CODEC;
    }

    private static StructureType<MonarchCitadelStructure> monarchCitadelType() {
        return () -> MonarchCitadelStructure.CODEC;
    }

    public static void register(IEventBus modBus) {
        STRUCTURE_TYPES.register(modBus);
        CHUNK_GENERATORS.register(modBus);
        STRUCTURE_PIECE_TYPES.register(modBus);
    }
}
