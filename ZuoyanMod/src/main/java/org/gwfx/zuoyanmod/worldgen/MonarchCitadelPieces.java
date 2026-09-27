package org.gwfx.zuoyanmod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.block.state.BlockState;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.block.BlockRegistry;
import org.gwfx.zuoyanmod.entity.EntityRegistry;
import org.gwfx.zuoyanmod.entity.VoidGuardEntity;
import org.gwfx.zuoyanmod.entity.VoidMonarchEntity;

/**
 * 湮灭王座遗迹的拼装件与拼装器（纯代码生成，无结构模板资产）。
 *
 * <p>布局（俯视，x 向东 / z 向南；总体 56 x 41）：
 * <pre>
 *   z 0..15   前庭：断墙 + 大门 + 碎石 + 驻军 + 宝箱（CourtyardPiece）
 *   z 4..12   双塔骑在前庭两侧线上（TowerPiece，高 12~20 随机）
 *   z 15..40  大殿：沉降式王座殿，君主沉眠于王座（HallPiece）
 * </pre>
 *
 * <p><b>坐标约定</b>：piece 一律 {@code setOrientation(Direction.SOUTH)}——
 * 这个朝向下 mirror/rotation 均为 NONE、{@code getWorldX/Z = min + 局部}、
 * {@code getWorldY = minY + 局部}。因此 postProcess 里<xem>全部使用局部坐标</xem>
 * （0 从 boundingBox.min 起算），由 StructurePiece 的变换负责平移。
 * 千万不要传绝对坐标（会被再平移一次），也不要留 null 朝向
 * （null 时 getWorldX 返回裸局部坐标，整座建筑会平移丢失原点——已踩坑）。
 *
 * <p>随机残破度（墙洞 / 塌顶 / 碎石）在 postProcess 里按固定顺序消费 random——
 * 每个 piece 的 postProcess 会因跨区块被执行多次，<b>循环必须全量跑完</b>
 * （placeBlock 自带 chunkBB 裁剪），保证各区块看到的随机序列一致、不出接缝。
 */
public final class MonarchCitadelPieces {

    /** 前庭/大殿共用的宝箱战利品表：data/zuoyanmod/loot_table/chests/ruined_monarch_citadel.json */
    static final ResourceKey<net.minecraft.world.level.storage.loot.LootTable> CITADEL_CHEST_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath(Zuoyanmod.MODID, "chests/ruined_monarch_citadel"));

    // ---- 常用方块（局部缓存，避免每格调用 get()）----
    private static final BlockState STONE_BRICKS = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState CRACKED_BRICKS = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
    private static final BlockState DEEPSLATE_BRICKS = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState CRACKED_DEEPSLATE = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState COBBLE = Blocks.COBBLESTONE.defaultBlockState();
    private static final BlockState MOSSY_COBBLE = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
    private static final BlockState COBWEB = Blocks.COBWEB.defaultBlockState();
    private static final BlockState VIOLET_GOLD = BlockRegistry.VIOLET_GOLD_BLOCK.get().defaultBlockState();
    private static final BlockState SOUL_LANTERN = Blocks.SOUL_LANTERN.defaultBlockState();

    private MonarchCitadelPieces() {}

    // ==================================================================
    // 拼装器
    // ==================================================================

    /** 由 {@link MonarchCitadelStructure#findGenerationPoint} 调用：按种子拼出整座遗迹。 */
    public static void assemblePieces(Structure.GenerationContext context, StructurePiecesBuilder builder, BlockPos start) {
        RandomSource random = context.random();
        int x0 = start.getX();
        int groundY = start.getY();
        int z0 = start.getZ();

        // 大殿（含王座与沉眠的君主）：26 x 26，位于后场
        builder.addPiece(new HallPiece(random, x0 + 15, groundY, z0 + 15));

        // 双塔骑在前庭两侧线上，高度随机，两塔错峰（视觉不对称的残破感）
        int westHeight = 12 + random.nextInt(9);
        int eastHeight = 12 + random.nextInt(9);
        builder.addPiece(new TowerPiece(random, x0 + 2, groundY, z0 + 4, westHeight));
        builder.addPiece(new TowerPiece(random, x0 + 45, groundY, z0 + 4, eastHeight));

        // 前庭：断墙 + 大门 + 碎石
        builder.addPiece(new CourtyardPiece(random, x0, groundY, z0));
    }

    // ==================================================================
    // 共用小工具
    // ==================================================================

    /** 石砖 / 龟裂石砖 随机混合的残旧墙面。 */
    private static BlockState weatheredBrick(RandomSource random) {
        return random.nextFloat() < 0.25F ? CRACKED_BRICKS : STONE_BRICKS;
    }

    /** 深板岩砖 / 龟裂深板岩砖 随机混合。 */
    private static BlockState weatheredDeepslate(RandomSource random) {
        return random.nextFloat() < 0.25F ? CRACKED_DEEPSLATE : DEEPSLATE_BRICKS;
    }

    private static VoidGuardEntity spawnGuard(WorldGenLevel level, EntityType<VoidGuardEntity> type,
                                              double x, double y, double z, float yRot) {
        VoidGuardEntity guard = type.create(level.getLevel());
        if (guard == null) {
            return null;
        }
        guard.setPos(x, y, z);
        guard.setYRot(yRot);
        guard.setYHeadRot(yRot);
        guard.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, y, z)),
                MobSpawnType.STRUCTURE, null);
        level.addFreshEntityWithPassengers(guard);
        return guard;
    }

    // ==================================================================
    // 大殿：沉降式王座殿（26 x 26 x 20，含君主）
    // 局部 y：0..3 = 地基底座，3 = 殿内地板（顶面 4），4 = 天然草层，
    // 5 = 前庭地表站立层；殿内比前庭沉降 1 格
    // ==================================================================

    public static class HallPiece extends StructurePiece {
        private boolean spawnedMonarch;
        private boolean spawnedGuards;

        public HallPiece(RandomSource random, int x, int groundY, int z) {
            super(WorldgenRegistry.CITADEL_HALL.get(), 0,
                    // 世界 y 范围：地基(groundY-5) .. 檐口(groundY+14)
                    new BoundingBox(x, groundY - 5, z, x + 25, groundY + 14, z + 25));
            // SOUTH：mirror/rotation = NONE，getWorldX/Z = min + 局部坐标
            this.setOrientation(Direction.SOUTH);
        }

        public HallPiece(CompoundTag tag) {
            super(WorldgenRegistry.CITADEL_HALL.get(), tag);
            this.spawnedMonarch = tag.getBoolean("SpawnedMonarch");
            this.spawnedGuards = tag.getBoolean("SpawnedGuards");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putBoolean("SpawnedMonarch", this.spawnedMonarch);
            tag.putBoolean("SpawnedGuards", this.spawnedGuards);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                                RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            // ---- 地基：局部 y0..3 的深板岩底座（顶面即地板标高）----
            this.generateBox(level, chunkBB, 0, 0, 0, 25, 3, 25, DEEPSLATE_BRICKS, CAVE_AIR, false);

            // ---- 清出殿内空间（局部 y4..12），顺带挖掉天然草层 ----
            this.generateAirBox(level, chunkBB, 1, 4, 1, 24, 12, 24);

            // ---- 地板（殿内 24x24，沉降 1 格：局部 y3）----
            for (int x = 1; x <= 24; x++) {
                for (int z = 1; z <= 24; z++) {
                    this.placeBlock(level, weatheredBrick(random), x, 3, z, chunkBB);
                }
            }

            // ---- 门前踏步：跨过 1 格沉降差 ----
            for (int x = 11; x <= 14; x++) {
                this.placeBlock(level, weatheredBrick(random), x, 4, 2, chunkBB);
                this.placeBlock(level, weatheredBrick(random), x, 4, 3, chunkBB);
            }

            // ---- 墙体（局部 y4..13，四条边）----
            for (int y = 4; y <= 13; y++) {
                for (int x = 0; x <= 25; x++) {
                    this.placeBlock(level, weatheredDeepslate(random), x, y, 0, chunkBB);
                    this.placeBlock(level, weatheredDeepslate(random), x, y, 25, chunkBB);
                }
                for (int z = 1; z <= 24; z++) {
                    this.placeBlock(level, weatheredDeepslate(random), 0, y, z, chunkBB);
                    this.placeBlock(level, weatheredDeepslate(random), 25, y, z, chunkBB);
                }
            }

            // ---- 正门：前墙 (z=0) 开 4 宽 4 高门洞，紫金门柱 ----
            for (int y = 3; y <= 6; y++) {
                for (int x = 11; x <= 14; x++) {
                    this.placeBlock(level, CAVE_AIR, x, y, 0, chunkBB);
                }
            }
            for (int y = 4; y <= 13; y++) {
                this.placeBlock(level, VIOLET_GOLD, 10, y, 0, chunkBB);
                this.placeBlock(level, VIOLET_GOLD, 15, y, 0, chunkBB);
            }

            // ---- 殿内立柱：四角 2x2 深板岩柱 + 王座两侧紫金柱，柱顶魂灯笼 ----
            int[][] cornerPillars = {{1, 1}, {23, 1}, {1, 23}, {23, 23}};
            for (int[] p : cornerPillars) {
                for (int y = 4; y <= 13; y++) {
                    this.placeBlock(level, weatheredDeepslate(random), p[0], y, p[1], chunkBB);
                    this.placeBlock(level, weatheredDeepslate(random), p[0] + 1, y, p[1], chunkBB);
                    this.placeBlock(level, weatheredDeepslate(random), p[0], y, p[1] + 1, chunkBB);
                    this.placeBlock(level, weatheredDeepslate(random), p[0] + 1, y, p[1] + 1, chunkBB);
                }
            }
            for (int y = 4; y <= 13; y++) {
                this.placeBlock(level, VIOLET_GOLD, 7, y, 12, chunkBB);
                this.placeBlock(level, VIOLET_GOLD, 18, y, 12, chunkBB);
            }
            this.placeBlock(level, SOUL_LANTERN, 7, 13, 13, chunkBB);
            this.placeBlock(level, SOUL_LANTERN, 18, 13, 13, chunkBB);

            // ---- 塌顶：局部 y13 一层深板岩天花板，15% 随机塌洞 + 蛛网 ----
            for (int x = 1; x <= 24; x++) {
                for (int z = 1; z <= 24; z++) {
                    if (random.nextFloat() < 0.15F) {
                        this.placeBlock(level, CAVE_AIR, x, 13, z, chunkBB);
                        if (random.nextFloat() < 0.5F) {
                            this.placeBlock(level, COBWEB, x, 12, z, chunkBB);
                        }
                    } else {
                        this.placeBlock(level, weatheredDeepslate(random), x, 13, z, chunkBB);
                    }
                }
            }

            // ---- 檐口雉堞：局部 y14..16 沿墙顶交错残缺 ----
            for (int y = 14; y <= 16; y++) {
                boolean merlon = (y - 14) % 2 == 0;
                for (int x = 0; x <= 25; x++) {
                    if (merlon ? (x % 2 == 0 || random.nextFloat() < 0.3F) : random.nextFloat() < 0.25F) {
                        this.placeBlock(level, weatheredDeepslate(random), x, y, 0, chunkBB);
                        this.placeBlock(level, weatheredDeepslate(random), x, y, 25, chunkBB);
                    }
                }
                for (int z = 1; z <= 24; z++) {
                    if (merlon ? (z % 2 == 0 || random.nextFloat() < 0.3F) : random.nextFloat() < 0.25F) {
                        this.placeBlock(level, weatheredDeepslate(random), 0, y, z, chunkBB);
                        this.placeBlock(level, weatheredDeepslate(random), 25, y, z, chunkBB);
                    }
                }
            }

            // ---- 王座平台：后场抬起 1 格的深板岩台（x10..15, z19..23）----
            for (int x = 10; x <= 15; x++) {
                for (int z = 19; z <= 23; z++) {
                    this.placeBlock(level, weatheredDeepslate(random), x, 4, z, chunkBB);
                }
            }
            // 台前踏步
            this.placeBlock(level, weatheredDeepslate(random), 12, 3, 18, chunkBB);
            this.placeBlock(level, weatheredDeepslate(random), 13, 3, 18, chunkBB);
            // 王座：紫金椅面 + 深板岩扶手 + 高椅背（x12..13, 靠后墙 z23）
            this.placeBlock(level, VIOLET_GOLD, 12, 5, 22, chunkBB);
            this.placeBlock(level, VIOLET_GOLD, 13, 5, 22, chunkBB);
            this.placeBlock(level, DEEPSLATE_BRICKS, 11, 5, 22, chunkBB);
            this.placeBlock(level, DEEPSLATE_BRICKS, 14, 5, 22, chunkBB);
            for (int y = 5; y <= 6; y++) {
                this.placeBlock(level, DEEPSLATE_BRICKS, 12, y, 23, chunkBB);
                this.placeBlock(level, DEEPSLATE_BRICKS, 13, y, 23, chunkBB);
            }
            this.placeBlock(level, VIOLET_GOLD, 12, 7, 23, chunkBB);
            this.placeBlock(level, VIOLET_GOLD, 13, 7, 23, chunkBB);
            // 王座两侧宝箱
            this.createChest(level, chunkBB, random, 9, 5, 21, CITADEL_CHEST_LOOT);
            this.createChest(level, chunkBB, random, 16, 5, 21, CITADEL_CHEST_LOOT);

            // ---- 大殿驻军：王座两侧各一名侍卫（站在殿内地板，局部 y4）----
            if (!this.spawnedGuards && chunkBB.isInside(new BlockPos(
                    this.getWorldX(8, 16), this.getWorldY(4), this.getWorldZ(8, 16)))) {
                this.spawnedGuards = true;
                spawnGuard(level, EntityRegistry.VOID_GUARD.get(),
                        this.getWorldX(8, 16) + 0.5D, this.getWorldY(4), this.getWorldZ(8, 16) + 0.5D, 90.0F);
                spawnGuard(level, EntityRegistry.VOID_GUARD.get(),
                        this.getWorldX(17, 16) + 0.5D, this.getWorldY(4), this.getWorldZ(17, 16) + 0.5D, -90.0F);
            }

            // ---- 沉眠的湮灭君主（核心演出：坐镇王座前，等玩家走近）----
            if (!this.spawnedMonarch && chunkBB.isInside(new BlockPos(
                    this.getWorldX(12, 20), this.getWorldY(5), this.getWorldZ(12, 20)))) {
                this.spawnedMonarch = true;
                VoidMonarchEntity monarch = EntityRegistry.VOID_MONARCH.get()
                        .create(level.getLevel());
                if (monarch != null) {
                    double mx = this.getWorldX(12, 20) + 0.5D;
                    double mz = this.getWorldZ(12, 20) + 0.5D;
                    monarch.setPos(mx, this.getWorldY(5), mz);
                    monarch.setYRot(180.0F);
                    monarch.setYHeadRot(180.0F);
                    monarch.finalizeSpawn(level, level.getCurrentDifficultyAt(monarch.blockPosition()),
                            MobSpawnType.STRUCTURE, null);
                    level.addFreshEntityWithPassengers(monarch);
                }
            }
        }
    }

    // ==================================================================
    // 塔楼：9 x 9 实心砖塔（高 12~20 随机），梯子通顶，塔顶雉堞
    // 局部 y：0 = 地表草层，1 = 地表站立层
    // ==================================================================

    public static class TowerPiece extends StructurePiece {
        private final int height;
        private boolean spawnedGuard;
        private boolean placedChest;

        public TowerPiece(RandomSource random, int x, int groundY, int z, int height) {
            super(WorldgenRegistry.CITADEL_TOWER.get(), 0,
                    new BoundingBox(x, groundY - 1, z, x + 8, groundY + height, z + 8));
            this.height = height;
            this.setOrientation(Direction.SOUTH);
        }

        public TowerPiece(CompoundTag tag) {
            super(WorldgenRegistry.CITADEL_TOWER.get(), tag);
            this.height = tag.contains("Height") ? tag.getInt("Height") : 16;
            this.spawnedGuard = tag.getBoolean("SpawnedGuard");
            this.placedChest = tag.getBoolean("PlacedChest");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putInt("Height", this.height);
            tag.putBoolean("SpawnedGuard", this.spawnedGuard);
            tag.putBoolean("PlacedChest", this.placedChest);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                                RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            // ---- 四面塔墙：从地表下 1 格（嵌进草地防浮空）垒到塔顶 ----
            for (int y = 0; y <= this.height; y++) {
                for (int x = 0; x <= 8; x++) {
                    this.placeBlock(level, weatheredBrick(random), x, y, 0, chunkBB);
                    this.placeBlock(level, weatheredBrick(random), x, y, 8, chunkBB);
                }
                for (int z = 1; z <= 7; z++) {
                    this.placeBlock(level, weatheredBrick(random), 0, y, z, chunkBB);
                    this.placeBlock(level, weatheredBrick(random), 8, y, z, chunkBB);
                }
            }

            // ---- 塔门：前墙 (z=0) 中央 1 宽 2 高 ----
            for (int y = 1; y <= 2; y++) {
                this.placeBlock(level, CAVE_AIR, 4, y, 0, chunkBB);
            }

            // ---- 紫金饰带：塔身 2/3 高处绕一圈 ----
            int band = (this.height * 2) / 3;
            for (int x = 0; x <= 8; x++) {
                this.placeBlock(level, VIOLET_GOLD, x, band, 0, chunkBB);
                this.placeBlock(level, VIOLET_GOLD, x, band, 8, chunkBB);
            }
            for (int z = 1; z <= 7; z++) {
                this.placeBlock(level, VIOLET_GOLD, 0, band, z, chunkBB);
                this.placeBlock(level, VIOLET_GOLD, 8, band, z, chunkBB);
            }

            // ---- 内部：贴西墙的梯子通顶 ----
            for (int y = 1; y <= this.height - 1; y++) {
                BlockState ladder = Blocks.LADDER.defaultBlockState()
                        .setValue(LadderBlock.FACING, Direction.EAST);
                this.placeBlock(level, ladder, 1, y, 1, chunkBB);
            }

            // ---- 塔底驻军与储物箱 ----
            if (!this.placedChest && chunkBB.isInside(new BlockPos(
                    this.getWorldX(2, 6), this.getWorldY(1), this.getWorldZ(2, 6)))) {
                this.placedChest = true;
                this.createChest(level, chunkBB, random, 2, 1, 6, CITADEL_CHEST_LOOT);
            }
            if (!this.spawnedGuard && chunkBB.isInside(new BlockPos(
                    this.getWorldX(5, 5), this.getWorldY(1), this.getWorldZ(5, 5)))) {
                this.spawnedGuard = random.nextFloat() < 0.6F;   // 塔里不一定有人
                if (this.spawnedGuard) {
                    spawnGuard(level, EntityRegistry.VOID_GUARD.get(),
                            this.getWorldX(5, 5) + 0.5D, this.getWorldY(1), this.getWorldZ(5, 5) + 0.5D, 180.0F);
                }
            }

            // ---- 塔顶雉堞：交错残缺的垛口 ----
            for (int y = this.height - 1; y <= this.height; y++) {
                boolean merlon = (this.height - y) % 2 == 0;
                for (int x = 0; x <= 8; x++) {
                    if (merlon ? x % 2 == 0 : random.nextFloat() < 0.35F) {
                        this.placeBlock(level, weatheredBrick(random), x, y, 0, chunkBB);
                        this.placeBlock(level, weatheredBrick(random), x, y, 8, chunkBB);
                    }
                }
                for (int z = 1; z <= 7; z++) {
                    if (merlon ? z % 2 == 0 : random.nextFloat() < 0.35F) {
                        this.placeBlock(level, weatheredBrick(random), 0, y, z, chunkBB);
                        this.placeBlock(level, weatheredBrick(random), 8, y, z, chunkBB);
                    }
                }
            }
            // 塔内照明：嵌进北墙的魂灯笼（不要悬浮）
            this.placeBlock(level, SOUL_LANTERN, 0, 3, 4, chunkBB);
        }
    }

    // ==================================================================
    // 前庭：56 x 16 断墙广场（大门 + 碎石 + 驻军 + 宝箱）
    // 局部 y：0 = 地表草层，1 = 地表站立层
    // ==================================================================

    public static class CourtyardPiece extends StructurePiece {
        private int guardsSpawned;
        private int chestsPlaced;

        public CourtyardPiece(RandomSource random, int x, int groundY, int z) {
            super(WorldgenRegistry.CITADEL_COURTYARD.get(), 0,
                    new BoundingBox(x, groundY - 1, z, x + 55, groundY + 7, z + 15));
            this.setOrientation(Direction.SOUTH);
        }

        public CourtyardPiece(CompoundTag tag) {
            super(WorldgenRegistry.CITADEL_COURTYARD.get(), tag);
            this.guardsSpawned = tag.contains("GuardsSpawned") ? tag.getInt("GuardsSpawned") : 0;
            this.chestsPlaced = tag.contains("ChestsPlaced") ? tag.getInt("ChestsPlaced") : 0;
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putInt("GuardsSpawned", this.guardsSpawned);
            tag.putInt("ChestsPlaced", this.chestsPlaced);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                                RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            // ---- 中央步道：把门到大殿门口的草皮换成残旧石砖 ----
            for (int x = 25; x <= 30; x++) {
                for (int z = 0; z <= 15; z++) {
                    this.placeBlock(level, random.nextFloat() < 0.2F ? CRACKED_BRICKS : STONE_BRICKS,
                            x, 0, z, chunkBB);
                }
            }

            // ---- 前墙（z=0）：残缺雉堞墙 + 正门 ----
            for (int x = 0; x <= 55; x++) {
                boolean inGate = x >= 25 && x <= 30;
                for (int y = 0; y <= 5; y++) {
                    // 大门门洞 6 宽 4 高；墙体带 10% 缺口
                    if (inGate && y >= 1 && y <= 4) {
                        continue;
                    }
                    if (random.nextFloat() < 0.10F) {
                        continue;
                    }
                    this.placeBlock(level, weatheredBrick(random), x, y, 0, chunkBB);
                }
                // 雉堞
                if (!inGate && x % 2 == 0 && random.nextFloat() < 0.8F) {
                    this.placeBlock(level, weatheredBrick(random), x, 6, 0, chunkBB);
                }
            }
            // 门柱与门楣（紫金）
            for (int y = 1; y <= 6; y++) {
                this.placeBlock(level, VIOLET_GOLD, 24, y, 0, chunkBB);
                this.placeBlock(level, VIOLET_GOLD, 31, y, 0, chunkBB);
            }
            for (int x = 25; x <= 30; x++) {
                this.placeBlock(level, VIOLET_GOLD, x, 5, 0, chunkBB);
            }

            // ---- 两侧院墙（x=0 / x=55），带随机大缺口 ----
            for (int wx : new int[]{0, 55}) {
                int gapStart = 3 + random.nextInt(4);
                for (int z = 1; z <= 14; z++) {
                    boolean inGap = z >= gapStart && z <= gapStart + 2 && random.nextFloat() < 0.85F;
                    if (inGap) {
                        continue;
                    }
                    for (int y = 0; y <= 4; y++) {
                        this.placeBlock(level, weatheredBrick(random), wx, y, z, chunkBB);
                    }
                    if (z % 2 == 0 && random.nextFloat() < 0.8F) {
                        this.placeBlock(level, weatheredBrick(random), wx, 5, z, chunkBB);
                    }
                }
            }

            // ---- 碎石堆：4~7 处，1~2 高的乱石 ----
            int piles = 4 + random.nextInt(4);
            for (int i = 0; i < piles; i++) {
                int px = 3 + random.nextInt(50);
                int pz = 3 + random.nextInt(11);
                int size = 1 + random.nextInt(2);
                for (int dx = -size; dx <= size; dx++) {
                    for (int dz = -size; dz <= size; dz++) {
                        int bx = px + dx;
                        int bz = pz + dz;
                        if (bx < 1 || bx > 54 || bz < 1 || bz > 14) {
                            continue;
                        }
                        if (bx >= 24 && bx <= 31) {
                            continue;   // 别堵步道
                        }
                        this.placeBlock(level, rubble(random), bx, 1, bz, chunkBB);
                        if (random.nextFloat() < 0.35F) {
                            this.placeBlock(level, rubble(random), bx, 2, bz, chunkBB);
                        }
                    }
                }
            }

            // ---- 蛛网点缀 ----
            for (int i = 0; i < 10; i++) {
                int wx = 1 + random.nextInt(54);
                int wz = 1 + random.nextInt(14);
                this.placeBlock(level, COBWEB, wx, 1 + random.nextInt(3), wz, chunkBB);
            }

            // ---- 宝箱 2 只（靠后侧两角）----
            if (this.chestsPlaced < 2 && chunkBB.isInside(new BlockPos(
                    this.getWorldX(4, 13), this.getWorldY(1), this.getWorldZ(4, 13)))) {
                this.chestsPlaced = 2;
                this.createChest(level, chunkBB, random, 4, 1, 13, CITADEL_CHEST_LOOT);
                this.createChest(level, chunkBB, random, 51, 1, 13, CITADEL_CHEST_LOOT);
            }

            // ---- 驻军 2 名（前庭游荡）----
            if (this.guardsSpawned < 2 && chunkBB.isInside(new BlockPos(
                    this.getWorldX(12, 8), this.getWorldY(1), this.getWorldZ(12, 8)))) {
                this.guardsSpawned = 2;
                spawnGuard(level, EntityRegistry.VOID_GUARD.get(),
                        this.getWorldX(12, 8) + 0.5D, this.getWorldY(1), this.getWorldZ(12, 8) + 0.5D, 45.0F);
                spawnGuard(level, EntityRegistry.VOID_GUARD.get(),
                        this.getWorldX(40, 10) + 0.5D, this.getWorldY(1), this.getWorldZ(40, 10) + 0.5D, -45.0F);
            }
        }

        private static BlockState rubble(RandomSource random) {
            float f = random.nextFloat();
            if (f < 0.35F) {
                return COBBLE;
            } else if (f < 0.6F) {
                return MOSSY_COBBLE;
            } else if (f < 0.8F) {
                return CRACKED_BRICKS;
            }
            return STONE_BRICKS;
        }
    }
}
