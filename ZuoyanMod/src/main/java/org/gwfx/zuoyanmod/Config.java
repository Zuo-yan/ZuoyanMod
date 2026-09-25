package org.gwfx.zuoyanmod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.gwfx.zuoyanmod.platform.RegistryLookup;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    // ===== 超平坦世界（zuoyanmod:realm）的矿物带 =====
    // 这三项只影响 realm 维度的矿带生成，见 worldgen/RealmOreBandGenerator。

    private static final ForgeConfigSpec.BooleanValue REALM_ORE_BANDS_ENABLED = BUILDER
            .comment("超平坦世界的分层矿物带是否启用")
            .define("realmOreBands.enabled", true);

    private static final ForgeConfigSpec.DoubleValue REALM_ORE_DENSITY_MULTIPLIER = BUILDER
            .comment("矿物带密度倍率。1.0 = 与原版单位体积密度相当，2.0 = 双倍富矿")
            .defineInRange("realmOreBands.densityMultiplier", 1.0D, 0.0D, 10.0D);

    private static final ForgeConfigSpec.IntValue REALM_ORE_MAX_ATTEMPTS_PER_ORE = BUILDER
            .comment("单个矿物每区块的尝试次数上限，防止个别矿物在窄高度区间下折算爆炸")
            .defineInRange("realmOreBands.maxAttemptsPerOre", 32, 1, 256);

    private static final ForgeConfigSpec.IntValue REALM_ORE_MAX_ATTEMPTS_PER_CHUNK = BUILDER
            .comment("每个区块所有矿物加起来的尝试次数上限")
            .defineInRange("realmOreBands.maxAttemptsPerChunk", 120, 1, 2048);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> REALM_ORE_EXCLUDED = BUILDER
            .comment("""
                    不参与矿物带的矿物特征 id 列表（对应 data/<ns>/worldgen/feature/ 下的条目）。
                    默认排除的几项都不是真正意义上的矿物：
                      minecraft:ore_infested  虫蚀石（会刷蠹虫）
                      minecraft:ore_dirt      泥土矿脉
                      minecraft:ore_gravel    沙砾矿脉
                      minecraft:ore_clay      黏土矿脉
                      minecraft:ore_emerald   绿宝石只在山地生成且每区块尝试 100 次，压进矿带会泛滥
                    """)
            .defineListAllowEmpty("realmOreBands.excluded", List.of(
                    "minecraft:ore_infested",
                    "minecraft:ore_dirt",
                    "minecraft:ore_gravel",
                    "minecraft:ore_clay",
                    "minecraft:ore_emerald"
            ), Config::validateResourceLocation);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;

    /** 这里给默认值，保证配置加载事件之前被读到也不会是 0 / false。 */
    public static boolean realmOreBandsEnabled = true;
    public static double realmOreDensityMultiplier = 1.0D;
    public static int realmOreMaxAttemptsPerOre = 32;
    public static int realmOreMaxAttemptsPerChunk = 120;
    public static Set<String> realmOreExcluded = Set.of(
            "minecraft:ore_infested", "minecraft:ore_dirt", "minecraft:ore_gravel",
            "minecraft:ore_clay", "minecraft:ore_emerald");

    private static boolean validateResourceLocation(final Object obj) {
        return obj instanceof String name && ResourceLocation.tryParse(name) != null;
    }

    private static boolean validateItemName(final Object obj) {
        if (obj instanceof String itemName) {
            return RegistryLookup.hasItem(ResourceLocation.tryParse(itemName));
        }
        return false;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        realmOreBandsEnabled = REALM_ORE_BANDS_ENABLED.get();
        realmOreDensityMultiplier = REALM_ORE_DENSITY_MULTIPLIER.get();
        realmOreMaxAttemptsPerOre = REALM_ORE_MAX_ATTEMPTS_PER_ORE.get();
        realmOreMaxAttemptsPerChunk = REALM_ORE_MAX_ATTEMPTS_PER_CHUNK.get();
        realmOreExcluded = REALM_ORE_EXCLUDED.get().stream()
                .map(String::valueOf)
                .collect(Collectors.toUnmodifiableSet());

        // 注册表按 ID 查询的返回类型随版本变化（26.3 是 Optional<Holder.Reference>），
        // 解包细节统一封装在 platform 适配层的 RegistryLookup 里
        items = ITEM_STRINGS.get().stream()
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .flatMap(id -> RegistryLookup.item(id).stream())
                .collect(Collectors.toSet());
    }

    /** 由主类在构造时调用：Forge 1.20.1 没有构造注入，要显式注册配置 */
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }
}
