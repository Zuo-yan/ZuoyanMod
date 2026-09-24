package org.gwfx.zuoyanmod.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

import java.util.Optional;

/**
 * 「只在陆地上生成」的拼图结构。
 *
 * <p>字段与 {@code minecraft:jigsaw} <b>完全一致</b>，JSON 可以直接互换，只是多了一道
 * 地表检查：如果目标区块的地表方块是液体（海洋／湖泊／河流），就放弃在这一格生成。</p>
 *
 * <p>为什么需要它：结构定义里的 {@code project_start_to_heightmap: WORLD_SURFACE_WG}
 * 会把建筑吸附到"地表高度"，而水面也算地表——陆上群系里夹着的湖泊照样会让房子浮在水上。
 * 群系标签只能排除海洋／河流这类<b>整个群系都是水</b>的情况，管不了森林里的一潭湖水。
 * 这也是 When Dungeons Arise 用自定义结构类做的事。</p>
 *
 * <p>检查点在区块正中央那一列。</p>
 *
 * <p>1.20.1 适配：{@code StructureType} 的 codec 是 {@code Codec<S>}（26.x 是 MapCodec）；
 * 1.20.1 的 jigsaw 没有 {@code pool_aliases} / {@code dimension_padding} /
 * {@code liquid_settings}，且 {@code max_distance_from_center} 是 1..128 的整数
 * （26.x 是带 horizontal/vertical 的对象）。字段因此做了相应精简。</p>
 */
public class LandCheckedJigsawStructure extends Structure {

    /** 与 {@link JigsawStructure} 同款字段（1.20.1 版本的子集），保持 JSON 格式兼容。 */
    public static final Codec<LandCheckedJigsawStructure> CODEC = ExtraCodecs.validate(
            RecordCodecBuilder.<LandCheckedJigsawStructure>mapCodec(
                    i -> i.group(
                            settingsCodec(i),
                            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
                            ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
                            Codec.intRange(0, 20).fieldOf("size").forGetter(s -> s.maxDepth),
                            HeightProvider.CODEC.fieldOf("start_height").forGetter(s -> s.startHeight),
                            Codec.BOOL.fieldOf("use_expansion_hack").forGetter(s -> s.useExpansionHack),
                            Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(s -> s.projectStartToHeightmap),
                            Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistanceFromCenter)
                    ).apply(i, LandCheckedJigsawStructure::new)
            ), LandCheckedJigsawStructure::verifyRange).codec();

    private final Holder<StructureTemplatePool> startPool;
    private final Optional<ResourceLocation> startJigsawName;
    private final int maxDepth;
    private final HeightProvider startHeight;
    private final boolean useExpansionHack;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;

    public LandCheckedJigsawStructure(
            Structure.StructureSettings settings,
            Holder<StructureTemplatePool> startPool,
            Optional<ResourceLocation> startJigsawName,
            int maxDepth,
            HeightProvider startHeight,
            boolean useExpansionHack,
            Optional<Heightmap.Types> projectStartToHeightmap,
            int maxDistanceFromCenter
    ) {
        super(settings);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.maxDepth = maxDepth;
        this.startHeight = startHeight;
        this.useExpansionHack = useExpansionHack;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
    }

    /**
     * 与原版 {@code JigsawStructure.verifyRange} 同一套校验：
     * 中心最大距离加上地形贴合所需的边缘（12 格）不得超过 128。
     */
    private static DataResult<LandCheckedJigsawStructure> verifyRange(LandCheckedJigsawStructure structure) {
        int edgeNeeded = switch (structure.terrainAdaptation()) {
            case NONE -> 0;
            case BURY, BEARD_THIN, BEARD_BOX -> 12;
        };
        return structure.maxDistanceFromCenter + edgeNeeded > JigsawStructure.MAX_TOTAL_STRUCTURE_RANGE
                ? DataResult.error(() -> "Structure size including terrain adaptation must not exceed 128")
                : DataResult.success(structure);
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        // 地表是水就整格放弃 —— 必须放在 addPieces 之前，
        // 否则建筑会被吸附到水面高度、变成"浮在水上的房子"。
        if (isSurfaceLiquid(context)) {
            return Optional.empty();
        }

        ChunkPos chunkPos = context.chunkPos();
        int height = this.startHeight.sample(
                context.random(),
                new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor())
        );
        BlockPos startPos = new BlockPos(chunkPos.getMinBlockX(), height, chunkPos.getMinBlockZ());
        return JigsawPlacement.addPieces(
                context,
                this.startPool,
                this.startJigsawName,
                this.maxDepth,
                startPos,
                this.useExpansionHack,
                this.projectStartToHeightmap,
                this.maxDistanceFromCenter
        );
    }

    /**
     * 取区块正中央那一列，看地表方块是不是液体。
     *
     * <p>用 {@code WORLD_SURFACE_WG}（含流体）取高度，所以湖泊会返回水面高度、
     * 水面上那块就是水；海草／海带这类含水流体的方块同样会被判为"在水中"，正是想要的结果。</p>
     */
    private static boolean isSurfaceLiquid(Structure.GenerationContext context) {
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
        return WorldgenRegistry.LAND_CHECKED_JIGSAW.get();
    }
}
