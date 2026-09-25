package org.gwfx.zuoyanmod.worldgen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import org.gwfx.zuoyanmod.Config;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 超平坦世界（{@code zuoyanmod:realm}）的「分层矿物带」。
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
 * 判据是「该 {@link ConfiguredFeature} 的 Feature 是 {@link OreFeature}」，因此<b>任何</b>模组
 * 后来往这些群系添加的矿物（无论是改群系 JSON 还是用 {@code forge:add_features}）
 * 都会自动进入矿带，无需手写任何配置。
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
 *
 * <h2>1.20.1 适配</h2>
 * <ul>
 *   <li><b>Feature ↔ ConfiguredFeature</b>：26.3 把配置内联进 Feature，
 *       {@code PlacedFeature#feature()} 直接是 {@code Holder<Feature>}；1.20.1 里它是
 *       {@code Holder<ConfiguredFeature<?,?>>}，真正有 {@code place} 的是后者。
 *       所以 {@code OreEntry} 存的是 ConfiguredFeature 的 Holder，
 *       「去重 / 排除名单」用的 id 也随之从 Feature id 变成 <b>ConfiguredFeature id</b>
 *       —— 好在默认排除的那几项（{@code minecraft:ore_dirt}、{@code minecraft:ore_infested}…）
 *       在两个版本里都是 configured_feature 的名字，配置文案不用改。</li>
 *   <li><b>{@code AbstractOreFeature} 不存在</b>：1.20.1 只有 {@link OreFeature}，
 *       {@code ScatteredOreFeature} 继承自它，所以 {@code instanceof OreFeature} 一样全覆盖
 *       （1.20.1 的远古残骸就是 {@code ScatteredOreFeature}）。</li>
 *   <li><b>高度基准值</b>：1.20.1 主世界同样是 384 格、地狱与末地 256 格，数值沿用主线。</li>
 *   <li><b>读取放置次数要靠 codec 回解</b>：见 {@link #countOf} 的注释。</li>
 * </ul>
 */
public final class RealmOreBandGenerator {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 解 {@code count} 字段用的 codec；范围与原版 {@code CountPlacement} 一致（0..256）。 */
    private static final Codec<IntProvider> COUNT_PROVIDER = IntProvider.codec(0, 256);

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

    /** 1.20.1：存的是 ConfiguredFeature 的 Holder，{@link ConfiguredFeature#place} 才是投放入口。 */
    private record OreEntry(Holder<ConfiguredFeature<?, ?>> feature, float attempts) {
    }

    private record Snapshot(List<Band> bands, Map<BandKind, List<OreEntry>> pools) {
    }

    private static final Snapshot EMPTY = new Snapshot(List.of(), Map.of());

    /** 缓存：区块生成是多线程的，用 volatile 快照 + 加锁重建。 */
    private volatile Snapshot snapshot;

    /** 投放阶段出错只记一次日志，避免刷屏。 */
    private volatile boolean placeFailed;

    /**
     * 让缓存失效。
     *
     * <p>26.3 里这是 {@code ChunkGenerator#refreshFeaturesPerStep} 的覆写回调，
     * 但 1.20.1 的 {@code ChunkGenerator} 没有这个方法 —— 好在 1.20.1 每次 {@code /reload}
     * 或重新加载世界都会由 codec 重新构造 ChunkGenerator，而本对象是生成器实例的成员，
     * 跟着一起重建，缓存自然失效（见 {@link RealmFlatChunkGenerator} 的类注释）。
     */
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
                    // 缓存空快照，避免每个区块重复抛异常；重载数据生成器时会重建本对象从而重试
                    LOGGER.error("[Realm] 构建矿物带失败，本次不再生成矿物带", t);
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

        List<Band> bands = detectBands(flat.settings().getLayers(), level.getMinBuildHeight());
        if (bands.isEmpty()) {
            LOGGER.warn("[Realm] 层配置里没有识别到任何矿带（需要 stone/deepslate、netherrack、end_stone 连续层）");
            return new Snapshot(List.of(), Map.of());
        }

        RegistryAccess access = level.registryAccess();
        Registry<Biome> biomes = access.registryOrThrow(Registries.BIOME);

        float multiplier = (float) Config.realmOreDensityMultiplier;
        int maxPerOre = Config.realmOreMaxAttemptsPerOre;
        Set<String> excluded = Config.realmOreExcluded;

        Map<BandKind, List<OreEntry>> pools = new EnumMap<>(BandKind.class);
        for (Band band : bands) {
            // 1.20.1 的 Registry 只有 getTag(TagKey) → Optional<HolderSet.Named>，
            // 没有 getOrThrow(TagKey)（26.3 才把它换成会抛异常的版本）。
            // 标签缺失时跳过该矿带而不是抛异常 —— 别让一个标签打错字把区块生成搞崩。
            HolderSet.Named<Biome> biomeHolders = biomes.getTag(band.kind().biomeTag).orElse(null);
            if (biomeHolders == null) {
                LOGGER.warn("[Realm] 找不到群系标签 {}，跳过该矿带", band.kind().biomeTag.location());
                pools.put(band.kind(), List.of());
                continue;
            }

            // 先按 placed_feature 去重（同一个 placed_feature 会出现在几十个群系里，会被重复扫到），
            // 再把同一 ConfiguredFeature 的多个 placed_feature 的次数累加（例如煤的 upper + lower）。
            Set<ResourceLocation> seenPlacedFeatures = new HashSet<>();
            Map<ResourceLocation, Holder<ConfiguredFeature<?, ?>>> holders = new HashMap<>();
            Map<ResourceLocation, Float> counts = new HashMap<>();

            for (Holder<Biome> biome : biomeHolders) {
                for (HolderSet<PlacedFeature> step : biome.value().getGenerationSettings().features()) {
                    for (Holder<PlacedFeature> placedHolder : step) {
                        PlacedFeature placed = placedHolder.value();
                        Holder<ConfiguredFeature<?, ?>> configuredHolder = placed.feature();
                        if (!(configuredHolder.value().feature() instanceof OreFeature)) {
                            continue;
                        }
                        ResourceLocation configuredId = configuredHolder.unwrapKey()
                                .map(ResourceKey::location).orElse(null);
                        if (configuredId == null || excluded.contains(configuredId.toString())) {
                            continue;
                        }
                        ResourceLocation placedId = placedHolder.unwrapKey()
                                .map(ResourceKey::location).orElse(null);
                        if (placedId == null || !seenPlacedFeatures.add(placedId)) {
                            continue;
                        }
                        float count = countOf(placed);
                        if (count <= 0.0f) {
                            continue;
                        }
                        holders.putIfAbsent(configuredId, configuredHolder);
                        counts.merge(configuredId, count, Float::sum);
                    }
                }
            }

            List<OreEntry> entries = new ArrayList<>(counts.size());
            for (Map.Entry<ResourceLocation, Float> entry : counts.entrySet()) {
                float attempts = entry.getValue() * band.height() / (float) band.kind().referenceHeight * multiplier;
                attempts = Math.min(attempts, maxPerOre);
                if (attempts < 0.01f) {
                    continue;
                }
                entries.add(new OreEntry(holders.get(entry.getKey()), attempts));
            }
            // 稳定排序：随机种子按索引派生，顺序必须只与注册 id 有关，不能依赖集合迭代顺序
            entries.sort(Comparator.comparing(e -> e.feature().unwrapKey()
                    .map(key -> key.location().toString()).orElse("")));
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

    /**
     * 摊平成一个 placed_feature 的「每区块平均尝试次数」。
     *
     * <p><b>1.20.1 适配（这里与主线差别最大）</b>：26.3 有 {@code CountPlacement#count()} 与
     * {@code RarityFilter#chance()} 这类公开访问器，可以直接读意图；1.20.1 里这两个字段是
     * <b>private 且没有 getter</b>（{@code CountPlacement} 只暴露受保护的
     * {@code count(RandomSource, BlockPos)}）。按字段名反射在正式服（searge 混淆）会直接失效，
     * 所以这里改用 {@link PlacementModifier#CODEC} 把修饰符<b>编回 JSON 再解</b>：
     * {@code {"type":"minecraft:count","count":20}} / {@code {"type":"minecraft:rarity_filter","chance":8}}。
     * 这是纯公开 API、跟着原版格式走，而且是<b>每个 included feature 只跑一次</b>（结果缓存在快照里）。
     *
     * <p>{@code count} 可能是 {@code 20}、{@code {"min":3,"max":6}} 或
     * {@code {"base":4,"spread":2}}，统一交给 {@link IntProvider} 的 codec 解，取中值；
     * 解不出来就退回 {@code 1.0F}（极端容错：宁可稀也不崩）。
     */
    private static float countOf(PlacedFeature placed) {
        for (PlacementModifier modifier : placed.placement()) {
            Optional<JsonElement> encoded = PlacementModifier.CODEC
                    .encodeStart(JsonOps.INSTANCE, modifier)
                    .result();
            if (encoded.isEmpty() || !encoded.get().isJsonObject()) {
                continue;
            }
            JsonObject json = encoded.get().getAsJsonObject();

            JsonElement chance = json.get("chance");
            if (chance != null && chance.isJsonPrimitive()) {
                int value = chance.getAsInt();
                return value <= 0 ? 1.0f : 1.0f / value;
            }

            JsonElement count = json.get("count");
            if (count != null) {
                Optional<IntProvider> provider = COUNT_PROVIDER.parse(JsonOps.INSTANCE, count).result();
                if (provider.isPresent()) {
                    return (provider.get().getMinValue() + provider.get().getMaxValue()) / 2.0f;
                }
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
                    // 绕开 PlacedFeature 自带的 count / rarity 等放置修饰符 —— 它们会把单次调用
                    // 摊成整份次数已经算进 attempts 里了（见 countOf），这里必须只放一次。
                    entry.feature().value().place(level, generator, random, new BlockPos(x, y, z));
                }
            }
        }
    }
}
