package org.gwfx.zuoyanmod.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

/**
 * 湮灭王座：代码程序化生成的 Boss 遗迹（不走 jigsaw）。
 *
 * <p>做法参考 When Dungeons Arise 的自定义结构类——{@code findGenerationPoint}
 * 里先做"地表不能是水"检查（复用 {@link LandCheckedJigsawStructure} 的思路），
 * 通过后把一个 {@code Consumer<StructurePiecesBuilder>} 交给原版管线，
 * 在 {@link MonarchCitadelPieces} 里按种子随机拼装大殿、双塔与前庭。
 *
 * <p>与两个参考模组的差异：不做多层迷宫（WDA），也不是完整城堡（暮色森林），
 * 而是一座<b>沉降式王座大殿 + 断墙前庭</b>的遗迹——地表只露出门楼与塔尖，
 * 玩家从坍塌的大门走进前庭，就能看见王座上沉眠的湮灭君主。
 *
 * <p>JSON 里只需 {@code biomes/step/spawn_overrides/terrain_adaptation} 四个字段
 * （{@code settingsCodec} 自带），不需要 start_pool / size 等 jigsaw 字段。
 * 超平坦世界地形恒平，落点高度取自 WORLD_SURFACE_WG 高度图。
 */
public class MonarchCitadelStructure extends Structure {

    public static final MapCodec<MonarchCitadelStructure> CODEC =
            RecordCodecBuilder.<MonarchCitadelStructure>mapCodec(
                    i -> i.group(settingsCodec(i)).apply(i, MonarchCitadelStructure::new)
            );

    public MonarchCitadelStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        // 地表是水就整格放弃（虽然超平坦 realm 不会有水，留着与陆地检查保持一致）
        if (isSurfaceLiquid(context)) {
            return Optional.empty();
        }

        ChunkPos chunkPos = context.chunkPos();
        // 落点 = 地表第一格空气（超平坦：草地上一格）。取区块中心列的高度。
        int surfaceY = context.chunkGenerator().getFirstOccupiedHeight(
                chunkPos.getMiddleBlockX(), chunkPos.getMiddleBlockZ(),
                Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        BlockPos startPos = new BlockPos(chunkPos.getMinBlockX(), surfaceY + 1, chunkPos.getMinBlockZ());

        return Optional.of(new GenerationStub(startPos,
                builder -> MonarchCitadelPieces.assemblePieces(context, builder, startPos)));
    }

    /** 与 {@link LandCheckedJigsawStructure} 相同的地表液体检查（区块正中央一列）。 */
    private static boolean isSurfaceLiquid(GenerationContext context) {
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor heightAccessor = context.heightAccessor();
        RandomState randomState = context.randomState();
        BlockPos probe = context.chunkPos().getMiddleBlockPosition(0);

        int surfaceY = generator.getFirstOccupiedHeight(
                probe.getX(), probe.getZ(), Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState
        );
        NoiseColumn column = generator.getBaseColumn(probe.getX(), probe.getZ(), heightAccessor, randomState);
        BlockState topState = column.getBlock(surfaceY);
        if (topState == null) {
            return false;
        }
        return !topState.getFluidState().isEmpty();
    }

    @Override
    public StructureType<?> type() {
        return WorldgenRegistry.MONARCH_CITADEL.get();
    }
}
