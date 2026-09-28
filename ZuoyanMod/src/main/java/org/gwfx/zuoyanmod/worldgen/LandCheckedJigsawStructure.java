package org.gwfx.zuoyanmod.worldgen;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

/**
 * 「只在陆地上生成」的拼图结构。
 *
 * <p>字段与 {@code minecraft:jigsaw} <b>完全一致</b>，JSON 可以直接互换，只是多了一道
 * 地表检查：如果目标区块的地表方块是液体（海洋／湖泊／河流），就放弃在这一格生成。
 *
 * <p>为什么需要它：结构定义里的 {@code project_start_to_heightmap: WORLD_SURFACE_WG}
 * 会把建筑吸附到"地表高度"，而水面也算地表——陆上群系里夹着的湖泊照样会让房子浮在水上。
 * 群系标签只能排除海洋／河流这类<b>整个群系都是水</b>的情况，管不了森林里的一潭湖水。
 * 这也是 When Dungeons Arise 用自定义结构类做的事（见其 {@code isFeatureChunk}）。
 *
 * <p>检查点在区块正中央那一列，和原版 WDA 的做法一致。
 */
public class LandCheckedJigsawStructure extends Structure {

    /** 与 {@link JigsawStructure} 同款字段，保持 JSON 格式兼容。 */
    public static final MapCodec<LandCheckedJigsawStructure> CODEC = RecordCodecBuilder.<LandCheckedJigsawStructure>mapCodec(
            i -> i.group(
                    settingsCodec(i),
                    StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
                    Identifier.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
                    Codec.intRange(0, 20).fieldOf("size").forGetter(s -> s.maxDepth),
                    HeightProvider.CODEC.fieldOf("start_height").forGetter(s -> s.startHeight),
                    Codec.BOOL.fieldOf("use_expansion_hack").forGetter(s -> s.useExpansionHack),
                    Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(s -> s.projectStartToHeightmap),
                    JigsawStructure.MaxDistance.CODEC.fieldOf("max_distance_from_center").forGetter(s -> s.maxDistanceFromCenter),
                    Codec.list(PoolAliasBinding.CODEC).optionalFieldOf("pool_aliases", List.of()).forGetter(s -> s.poolAliases),
                    DimensionPadding.CODEC.optionalFieldOf("dimension_padding", DimensionPadding.ZERO).forGetter(s -> s.dimensionPadding),
                    LiquidSettings.CODEC.optionalFieldOf("liquid_settings", LiquidSettings.APPLY_WATERLOGGING).forGetter(s -> s.liquidSettings)
            ).apply(i, LandCheckedJigsawStructure::new)
    ).validate(LandCheckedJigsawStructure::verifyRange);

    private final net.minecraft.core.Holder<StructureTemplatePool> startPool;
    private final Optional<Identifier> startJigsawName;
    private final int maxDepth;
    private final HeightProvider startHeight;
    private final boolean useExpansionHack;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final JigsawStructure.MaxDistance maxDistanceFromCenter;
    private final List<PoolAliasBinding> poolAliases;
    private final DimensionPadding dimensionPadding;
    private final LiquidSettings liquidSettings;

    public LandCheckedJigsawStructure(
            Structure.StructureSettings settings,
            net.minecraft.core.Holder<StructureTemplatePool> startPool,
            Optional<Identifier> startJigsawName,
            int maxDepth,
            HeightProvider startHeight,
            boolean useExpansionHack,
            Optional<Heightmap.Types> projectStartToHeightmap,
            JigsawStructure.MaxDistance maxDistanceFromCenter,
            List<PoolAliasBinding> poolAliases,
            DimensionPadding dimensionPadding,
            LiquidSettings liquidSettings
    ) {
        super(settings);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.maxDepth = maxDepth;
        this.startHeight = startHeight;
        this.useExpansionHack = useExpansionHack;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.poolAliases = poolAliases;
        this.dimensionPadding = dimensionPadding;
        this.liquidSettings = liquidSettings;
    }

    /**
     * 与原版 {@code JigsawStructure.verifyRange} 同一套校验：
     * 中心最大距离加上地形贴合所需的边缘（12 格）不得超过 128。
     */
    private static DataResult<LandCheckedJigsawStructure> verifyRange(LandCheckedJigsawStructure structure) {
        int edgeNeeded = switch (structure.terrainAdaptation()) {
            case NONE -> 0;
            case BURY, BEARD_THIN, BEARD_BOX, ENCAPSULATE -> 12;
        };
        return structure.maxDistanceFromCenter.horizontal() + edgeNeeded > JigsawStructure.MAX_TOTAL_STRUCTURE_RANGE
                ? DataResult.error(() -> "Horizontal structure size including terrain adaptation must not exceed 128")
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
                this.maxDistanceFromCenter,
                PoolAliasLookup.create(this.poolAliases, startPos, context.seed()),
                this.dimensionPadding,
                this.liquidSettings
        );
    }

    /**
     * 取区块正中央那一列，看地表方块是不是液体。
     *
     * <p>用 {@code WORLD_SURFACE_WG}（含流体）取高度，所以湖泊会返回水面高度、
     * 水面上那块就是水；海草／海带这类含水流体的方块同样会被判为"在水中"，正是想要的结果。
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
