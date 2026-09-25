package org.gwfx.zuoyanmod.worldgen;

import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.AbstractOreFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import org.gwfx.zuoyanmod.Config;
import org.slf4j.Logger;

/**
 * 超平坦世界的「分层矿物带」。
 *
 * <h2>分层</h2>
 * 层结构由维度 JSON 的 {@code generator.settings.layers} 决定，这里<b>不写死坐标</b>，
 * 而是运行时按方块材质自动识别三个连续的矿带：
 * <ul>
 *   <li>{@code stone}/{@code deepslate} 等石类 → 主世界带</li>
 *   <li>{@code netherrack} → 地狱带</li>
 *   <li>{@code end_stone} → 末地带</li>
 * </ul>
 * 所以想加厚、想调整上下顺序，只改 JSON 的 layers 就行。
 *
 * <h2>矿物来源（自动）</h2>
 * 每个带从对应维度标签的群系里收集矿物特征：
 * {@code #minecraft:is_overworld} / {@code #minecraft:is_nether} / {@code #minecraft:is_end}。
 * 判据是 {@code feature instanceof AbstractOreFeature}，因此<b>任何</b>模组后来往这些群系
 * 添加的矿物（无论是改群系 JSON 还是用 {@code neoforge:add_features}）都会自动进入矿带，
 * 数据包重载后自动刷新，无需手写任何配置。
 *
 * <h2>为什么岩石匹配不用管</h2>
 * 矿物特征自带 {@code tag_match} 目标谓词（{@code stone_ore_replaceables} /
 * {@code deepslate_ore_replaceables} / {@code netherrack} / {@code base_stone_nether}…），
 * 只替换自己认的岩石。主世界矿落到地狱岩带上什么也不会发生，反之亦然；
 * 末地原版没有矿物，所以那一带自然就是纯末地石。
 *
 * <h2>密度</h2>
 * {@code 尝试次数 = 该矿物在原版每区块的总次数 × (矿带高度 ÷ 原版维度高度) × 配置倍率}。
 * 即「把整个主世界/地狱/末地的矿物预算压进这一层」，单位体积密度与原版相当。
 */
public final class RealmOreBandGenerator {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 每个矿带锚定一个原版维度：用它的群系标签取矿物，用它的高度做密度折算基准。 */
    private enum BandKind {
        OVERWORLD(BiomeTags.IS_OVERWORLD, 384),
        NETHER(BiomeTags.IS_NETHER, 256),
        END(BiomeTags.IS_END, 256);

        private final TagKey<Biome> biomeTag;
        private final int referenceHeight;

        BandKind(TagKey<Biome> biomeTag, int referenceHeight) {
            this.biomeTag = biomeTag;
            this.referenceHeight = referenceHeight;
        }
    }

    private record Band(BandKind kind, int minY, int height) {
    }

    private record OreEntry(Holder<Feature> feature, float attempts) {
    }

    private record Snapshot(List<Band> bands, Map<BandKind, List<OreEntry>> pools) {
    }

    private static final Snapshot EMPTY = new Snapshot(List.of(), Map.of());

    /** 缓存：区块生成是多线程的，用 volatile 快照 + 加锁重建。 */
    private volatile Snapshot snapshot;

    /** 投放阶段出错只记一次日志，避免刷屏。 */
    private volatile boolean placeFailed;

    /** 数据包重载 / 层配置变化时调用。 */
    public void invalidate() {
        this.snapshot = null;
        this.placeFailed = false;
    }

    public void decorate(ChunkGenerator generator, WorldGenLevel level, ChunkAccess chunk) {
        if (!Config.realmOreBandsEnabled) {
            return;
        }
        Snapshot snap = snapshot(generator, level);
        if (snap.bands().isEmpty()) {
            return;
        }
        try {
            place(generator, level, chunk, snap);
        } catch (Throwable t) {
            // 绝不因为矿带出错而让区块生成崩掉
            if (!placeFailed) {
                placeFailed = true;
                LOGGER.error("[Realm] 矿物带投放失败", t);
            }
        }
    }

    private Snapshot snapshot(ChunkGenerator generator, WorldGenLevel level) {
        Snapshot current = this.snapshot;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (this.snapshot == null) {
                try {
                    this.snapshot = build(generator, level);
                } catch (Throwable t) {
                    // 缓存空快照，避免每个区块重复抛异常；/reload 会通过 invalidate() 重试
                    LOGGER.error("[Realm] 构建矿物带失败，本世界不再生成矿物带", t);
                    this.snapshot = EMPTY;
                }
            }
            return this.snapshot;
        }
    }

    // ------------------------------------------------------------------ 构建

    private static Snapshot build(ChunkGenerator generator, WorldGenLevel level) {
        if (!(generator instanceof FlatLevelSource flat)) {
            return new Snapshot(List.of(), Map.of());
        }

        List<Band> bands = detectBands(flat.settings().getLayers(), level.getMinY());
        if (bands.isEmpty()) {
            LOGGER.warn("[Realm] 层配置里没有识别到任何矿带（需要 stone/deepslate、netherrack、end_stone 连续层）");
            return new Snapshot(List.of(), Map.of());
        }

        RegistryAccess access = level.registryAccess();
        Registry<Biome> biomes = access.lookupOrThrow(Registries.BIOME);

        float multiplier = (float) Config.realmOreDensityMultiplier;
        int maxPerOre = Config.realmOreMaxAttemptsPerOre;
        Set<String> excluded = Config.realmOreExcluded;

        Map<BandKind, List<OreEntry>> pools = new EnumMap<>(BandKind.class);
        for (Band band : bands) {
            // 先按 placed_feature 去重（同一个 placed_feature 会出现在几十个群系里，会被重复扫到），
            // 再把同一 Feature 的多个 placed_feature 的次数累加（例如煤的 upper + lower）。
            Set<Identifier> seenPlacedFeatures = new HashSet<>();
            Map<Identifier, Holder<Feature>> holders = new HashMap<>();
            Map<Identifier, Float> counts = new HashMap<>();

            for (Holder<Biome> biome : biomes.getOrThrow(band.kind().biomeTag)) {
                for (HolderSet<PlacedFeature> step : biome.value().getGenerationSettings().features()) {
                    for (Holder<PlacedFeature> placedHolder : step) {
                        PlacedFeature placed = placedHolder.value();
                        Holder<Feature> featureHolder = placed.feature();
                        if (!(featureHolder.value() instanceof AbstractOreFeature)) {
                            continue;
                        }
                        Identifier featureId = featureHolder.unwrapKey().map(ResourceKey::identifier).orElse(null);
                        if (featureId == null || excluded.contains(featureId.toString())) {
                            continue;
                        }
                        Identifier placedId = placedHolder.unwrapKey().map(ResourceKey::identifier).orElse(null);
                        if (placedId == null || !seenPlacedFeatures.add(placedId)) {
                            continue;
                        }
                        float count = countOf(placed);
                        if (count <= 0.0f) {
                            continue;
                        }
                        holders.putIfAbsent(featureId, featureHolder);
                        counts.merge(featureId, count, Float::sum);
                    }
                }
            }

            List<OreEntry> entries = new ArrayList<>(counts.size());
            for (Map.Entry<Identifier, Float> entry : counts.entrySet()) {
                float attempts = entry.getValue() * band.height() / (float) band.kind().referenceHeight * multiplier;
                attempts = Math.min(attempts, maxPerOre);
                if (attempts < 0.01f) {
                    continue;
                }
                entries.add(new OreEntry(holders.get(entry.getKey()), attempts));
            }
            // 稳定排序：随机种子按索引派生，顺序必须只与注册 id 有关，不能依赖集合迭代顺序
            entries.sort(Comparator.comparing(e -> e.feature().unwrapKey()
                    .map(key -> key.identifier().toString()).orElse("")));
            pools.put(band.kind(), List.copyOf(entries));

            LOGGER.info("[Realm] {} 矿带: y={}..{} ({} 格), 矿物 {} 种",
                    band.kind(), band.minY(), band.minY() + band.height() - 1, band.height(), entries.size());
        }
        return new Snapshot(List.copyOf(bands), Map.copyOf(pools));
    }

    /** 从层列表里按材质找出连续矿带（层索引 0 = 世界最低点）。 */
    private static List<Band> detectBands(List<BlockState> layers, int minY) {
        Map<BandKind, int[]> ranges = new EnumMap<>(BandKind.class);
        for (int i = 0; i < layers.size(); i++) {
            BandKind kind = classify(layers.get(i));
            if (kind == null) {
                continue;
            }
            int[] range = ranges.get(kind);
            if (range == null) {
                ranges.put(kind, new int[]{i, i});
            } else {
                range[1] = i;
            }
        }
        List<Band> bands = new ArrayList<>(ranges.size());
        for (Map.Entry<BandKind, int[]> entry : ranges.entrySet()) {
            int[] range = entry.getValue();
            bands.add(new Band(entry.getKey(), minY + range[0], range[1] - range[0] + 1));
        }
        bands.sort(Comparator.comparingInt(Band::minY).reversed());
        return List.copyOf(bands);
    }

    private static BandKind classify(BlockState state) {
        if (state == null) {
            return null;
        }
        Block block = state.getBlock();
        if (block == Blocks.END_STONE) {
            return BandKind.END;
        }
        if (block == Blocks.NETHERRACK) {
            return BandKind.NETHER;
        }
        if (block == Blocks.STONE || block == Blocks.DEEPSLATE || block == Blocks.GRANITE
                || block == Blocks.DIORITE || block == Blocks.ANDESITE || block == Blocks.TUFF) {
            return BandKind.OVERWORLD;
        }
        return null;
    }

    /** 摊平成一个 placed_feature 的「每区块平均尝试次数」。 */
    private static float countOf(PlacedFeature placed) {
        for (PlacementModifier modifier : placed.placement()) {
            if (modifier instanceof CountPlacement count) {
                IntProvider provider = count.count();
                try {
                    return (provider.minInclusive() + provider.maxInclusive()) / 2.0f;
                } catch (Throwable ignored) {
                    return 1.0f;
                }
            }
            if (modifier instanceof RarityFilter rarity) {
                return 1.0f / Math.max(1, rarity.chance());
            }
        }
        // 既没有 count 也没有 rarity_filter = 原版语义「每区块 1 次」（远古残骸等）
        return 1.0f;
    }

    // ------------------------------------------------------------------ 投放

    private static void place(ChunkGenerator generator, WorldGenLevel level, ChunkAccess chunk, Snapshot snap) {
        ChunkPos chunkPos = chunk.getPos();
        WorldgenRandom random = new WorldgenRandom(new XoroshiroRandomSource(level.getSeed()));
        long decorationSeed = random.setDecorationSeed(level.getSeed(), chunkPos.getMinBlockX(), chunkPos.getMinBlockZ());

        int budget = Config.realmOreMaxAttemptsPerChunk;

        for (Band band : snap.bands()) {
            List<OreEntry> pool = snap.pools().get(band.kind());
            if (pool == null || pool.isEmpty()) {
                continue;
            }
            for (int i = 0; i < pool.size(); i++) {
                OreEntry entry = pool.get(i);
                random.setFeatureSeed(decorationSeed, i, band.kind().ordinal());

                int whole = (int) entry.attempts();
                float fraction = entry.attempts() - whole;
                // 小数部分按概率取整：0.3 次 = 30% 的区块放一次，
                // 否则像远古残骸这种极稀有的矿会被取整成「每区块必有」
                if (fraction > 0.0f && random.nextFloat() < fraction) {
                    whole++;
                }
                for (int k = 0; k < whole; k++) {
                    if (budget-- <= 0) {
                        return;
                    }
                    int x = chunkPos.getMinBlockX() + random.nextInt(16);
                    int z = chunkPos.getMinBlockZ() + random.nextInt(16);
                    int y = band.minY() + random.nextInt(band.height());
                    // 26.3 没有 ConfiguredFeature：配置直接内联在 Feature 实例上
                    entry.feature().value().place(level, generator, random, new BlockPos(x, y, z));
                }
            }
        }
    }
}
