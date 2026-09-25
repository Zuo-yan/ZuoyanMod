package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.block.BlockRegistry;
import org.gwfx.zuoyanmod.fluid.FluidRegistry;
import org.gwfx.zuoyanmod.sound.SoundRegistry;

import java.util.List;
import java.util.Map;

public final class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Zuoyanmod.MODID);

    // ===== 紫金材料链（须在紫金装备之前注册，装备 repair 引用紫金锭） =====
    public static final RegistryObject<Item> RAW_VIOLET_GOLD =
            ITEMS.register("raw_violet_gold", () -> new Item(new Item.Properties()));

    public static final RegistryObject<VioletGoldIngotItem> VIOLET_GOLD_INGOT =
            ITEMS.register("violet_gold_ingot", () -> new VioletGoldIngotItem(new Item.Properties()));

    // 圣遗物核心：终局合成材料
    public static final RegistryObject<Item> RELIC_CORE =
            ITEMS.register("relic_core", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));

    // 圣辉锻造模板：锻造台升级下界合金盔甲为紫金神装
    public static final RegistryObject<SmithingTemplateItem> HALLOWED_UPGRADE_SMITHING_TEMPLATE =
            ITEMS.register("hallowed_upgrade_smithing_template", () -> new SmithingTemplateItem(
                    // 1.20.1 的构造参数是 5 个 Component（第一个是标题）+ 2 个槽位贴图列表
                    Component.translatable("item.zuoyanmod.hallowed_upgrade_smithing_template"),
                    Component.translatable("item.zuoyanmod.hallowed_upgrade_smithing_template.applies_to"),
                    Component.translatable("item.zuoyanmod.hallowed_upgrade_smithing_template.ingredients"),
                    Component.translatable("item.zuoyanmod.hallowed_upgrade_smithing_template.base_slot_description"),
                    Component.translatable("item.zuoyanmod.hallowed_upgrade_smithing_template.additions_slot_description"),
                    List.of(
                            new ResourceLocation("container/slot/helmet"),
                            new ResourceLocation("container/slot/chestplate"),
                            new ResourceLocation("container/slot/leggings"),
                            new ResourceLocation("container/slot/boots")
                    ),
                    List.of(new ResourceLocation("container/slot/diamond"))
            ));

    // ===== 消耗与功能道具 =====
    // 注册 id 由主线与 26.3 同步改名为 chocolate_crisp（显示名一直是「巧乐兹」，id 才对不上）。
    // 旧存档里的 zuoyanmod:ice_tea 由 Zuoyanmod 的 MissingMappings 钩子改名续命，见那里的注释。
    public static final RegistryObject<ChocolateCrispItem> CHOCOLATE_CRISP =
            ITEMS.register("chocolate_crisp", () -> new ChocolateCrispItem(new Item.Properties().stacksTo(16).food(
                    new FoodProperties.Builder()
                            .nutrition(3)
                            .saturationMod(0.5f)
                            .alwaysEat()
                            .build()
            )));

    public static final RegistryObject<SpriteDrinkItem> SPRITE_DRINK =
            ITEMS.register("sprite_drink", () -> new SpriteDrinkItem(new Item.Properties().stacksTo(16).food(
                    new FoodProperties.Builder()
                            .nutrition(3)
                            .saturationMod(0.5f)
                            .alwaysEat()
                            .build()
            )));

    public static final RegistryObject<DeathNoteItem> DEATH_NOTE =
            ITEMS.register("death_note", () -> new DeathNoteItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<MingDaoSiMingItem> MING_DAO_SI_MING =
            ITEMS.register("ming_dao_si_ming", () -> new MingDaoSiMingItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<SpaceAnchorItem> SPACE_ANCHOR =
            ITEMS.register("space_anchor", () -> new SpaceAnchorItem(new Item.Properties().stacksTo(1)));

    // ===== 紫金武器层级（1.20.1：Tier + SwordItem；26.x 是组件/Tier record） =====

    /** 紫金武器通用 Tier：附魔能力 15、不可损坏（uses=0）、挖掘等级按钻石 */
    public static final Tier VIOLET_GOLD_TIER = new Tier() {
        @Override
        public int getUses() {
            return 0; // uses=0 → maxDamage=0 → 不可损坏（26.x 是"不设 durability 组件"，等价）
        }

        @Override
        public float getSpeed() {
            return 6.0F;
        }

        @Override
        public float getAttackDamageBonus() {
            return 0.0F; // 全部伤害走 SwordItem 的 attackDamage 参数
        }

        // 1.20.1 的 Tier 接口：getLevel() 表示挖掘等级（钻石=3）；
        // 26.x 的 incorrect_for 标签在 1.20.1 还不存在（Forge 的 getTag() 默认返回 null，与原版 Tiers 一致）

        @Override
        public int getLevel() {
            return 3;
        }

        @Override
        public int getEnchantmentValue() {
            return 15; // 26.x 的 enchantable(15)
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(VIOLET_GOLD_INGOT.get());
        }
    };

    // 剑类武器：攻击力 33（1 基础 + 32）、攻速 3（4 基础 - 1）。
    // 26.x 有 ENTITY_INTERACTION_RANGE（攻击距离）属性，1.20.1 没有该属性，攻击距离加成暂缺。
    public static final RegistryObject<BeimingBlade> BEIMING_BLADE =
            ITEMS.register("beiming_blade", () -> new BeimingBlade(VIOLET_GOLD_TIER, 32, -1.0F,
                    new Item.Properties().stacksTo(1)));

    // ===== 万能工具（镐·斧·铲·锄·剑 五合一，数值取自对应 ToolMaterial）=====
    // 1.20.1 适配：26.x 用 ToolMaterial + DataComponents.TOOL；1.20.1 的等价物是 Tiers + DiggerItem，
    // 攻击力/攻速由 DiggerItem 构造器按「同材质斧」的基线给（见 UniversalToolItem 的类注释）。
    public static final RegistryObject<UniversalToolItem> WOODEN_UNIVERSAL_TOOL =
            ITEMS.register("wooden_universal_tool", () -> new UniversalToolItem(
                    Tiers.WOOD, 6.0F, -3.2F, UniversalToolItem.properties(Tiers.WOOD)));

    public static final RegistryObject<UniversalToolItem> STONE_UNIVERSAL_TOOL =
            ITEMS.register("stone_universal_tool", () -> new UniversalToolItem(
                    Tiers.STONE, 7.0F, -3.2F, UniversalToolItem.properties(Tiers.STONE)));

    public static final RegistryObject<UniversalToolItem> GOLDEN_UNIVERSAL_TOOL =
            ITEMS.register("golden_universal_tool", () -> new UniversalToolItem(
                    Tiers.GOLD, 6.0F, -3.0F, UniversalToolItem.properties(Tiers.GOLD)));

    public static final RegistryObject<UniversalToolItem> IRON_UNIVERSAL_TOOL =
            ITEMS.register("iron_universal_tool", () -> new UniversalToolItem(
                    Tiers.IRON, 6.0F, -3.1F, UniversalToolItem.properties(Tiers.IRON)));

    public static final RegistryObject<UniversalToolItem> DIAMOND_UNIVERSAL_TOOL =
            ITEMS.register("diamond_universal_tool", () -> new UniversalToolItem(
                    Tiers.DIAMOND, 5.0F, -3.0F, UniversalToolItem.properties(Tiers.DIAMOND)));

    // 下界合金同原版：防火不掉落（岩浆里烧不坏）
    public static final RegistryObject<UniversalToolItem> NETHERITE_UNIVERSAL_TOOL =
            ITEMS.register("netherite_universal_tool", () -> new UniversalToolItem(
                    Tiers.NETHERITE, 5.0F, -3.0F, UniversalToolItem.properties(Tiers.NETHERITE).fireResistant()));

    // ===== 阶段三新武器 =====
    public static final RegistryObject<HerculesBowItem> HERCULES_BOW =
            ITEMS.register("hercules_bow", () -> new HerculesBowItem(new Item.Properties().stacksTo(1).durability(1000)));

    // 匕首类：攻击力 5（1 基础 + 4）、攻速 5.5（4 基础 + 1.5）；攻击距离加成同上暂缺
    public static final RegistryObject<JackTheRipperScalpelItem> JACK_THE_RIPPER_SCALPEL =
            ITEMS.register("jack_the_ripper_scalpel", () -> new JackTheRipperScalpelItem(
                    VIOLET_GOLD_TIER, 4, 1.5F, new Item.Properties().stacksTo(1)));

    // ===== 紫金材质定义（1.20.1：ArmorMaterial 接口实现；26.x 是 record + EquipmentAsset） =====

    public static final ArmorMaterial VIOLETGOLD_MATERIAL = new ArmorMaterial() {
        /** 每件防具的基准耐久（原版同款基数），乘上近乎无限的倍率 */
        private static final Map<ArmorItem.Type, Integer> BASE_DURABILITY = Map.of(
                ArmorItem.Type.BOOTS, 13,
                ArmorItem.Type.LEGGINGS, 15,
                ArmorItem.Type.CHESTPLATE, 16,
                ArmorItem.Type.HELMET, 11
        );
        private static final Map<ArmorItem.Type, Integer> DEFENSE = Map.of(
                ArmorItem.Type.BOOTS, 25,
                ArmorItem.Type.LEGGINGS, 35,
                ArmorItem.Type.CHESTPLATE, 50,
                ArmorItem.Type.HELMET, 25
        );

        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return BASE_DURABILITY.getOrDefault(type, 13) * 999999; // 耐久倍率（近乎无限耐久）
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return DEFENSE.getOrDefault(type, 0);
        }

        @Override
        public int getEnchantmentValue() {
            return 20;
        }

        @Override
        public net.minecraft.sounds.SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_NETHERITE;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(VIOLET_GOLD_INGOT.get());
        }

        @Override
        public String getName() {
            return "violetgold"; // 对应 textures/models/armor/violetgold_layer_1/2.png
        }

        @Override
        public float getToughness() {
            return 15.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 3.0F;
        }
    };

    // ===== 紫金神装四件套注册 =====
    public static final RegistryObject<ShadowArmorItem> SHADOW_HELMET =
            ITEMS.register("shadow_helmet", () -> new ShadowArmorItem(
                    VIOLETGOLD_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<ShengTianChestplate> SHENG_TIAN_CHESTPLATE =
            ITEMS.register("shengtian_chestplate", () -> new ShengTianChestplate(
                    VIOLETGOLD_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<WindLeggings> WIND_LEGGINGS =
            ITEMS.register("wind_leggings", () -> new WindLeggings(
                    VIOLETGOLD_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<WalkerBoots> WALKER_BOOTS =
            ITEMS.register("walker_boots", () -> new WalkerBoots(
                    VIOLETGOLD_MATERIAL, ArmorItem.Type.BOOTS, new Item.Properties().stacksTo(1)));

    // ===== 饰品类物品（放入背包即生效） =====
    public static final RegistryObject<RingItem> RING_OF_KILLS =
            ITEMS.register("ring_of_kills", () -> new RingItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<WanHuiRingItem> WAN_HUI_RING =
            ITEMS.register("wan_hui_ring", () -> new WanHuiRingItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<VoodooNecklaceItem> VOODOO_NECKLACE =
            ITEMS.register("voodoo_necklace", () -> new VoodooNecklaceItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<YemengadeVenomFangItem> YEMENGADE_VENOM_FANG =
            ITEMS.register("yemengade_venom_fang", () -> new YemengadeVenomFangItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CounterBeltItem> COUNTER_BELT =
            ITEMS.register("counter_belt", () -> new CounterBeltItem(new Item.Properties().stacksTo(1)));

    // ===== 暗物质（终局合成材料，由虚空共振泵产出） =====
    public static final RegistryObject<Item> DARK_MATTER =
            ITEMS.register("dark_matter", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));

    // ===== 暗物质粒子（虚空共振泵的直接产物） =====
    public static final RegistryObject<DarkMatterParticleItem> DARK_MATTER_PARTICLE =
            ITEMS.register("dark_matter_particle", () -> new DarkMatterParticleItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // ===== 反物质微粒（微型强子对撞机产物：潮涌核心 + 烈焰棒） =====
    public static final RegistryObject<Item> ANTIMATTER_PARTICLE =
            ITEMS.register("antimatter_particle", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    // ===== 反物质子弹（因果律手枪的专用弹药：生存模式每发消耗 1 枚） =====
    public static final RegistryObject<Item> ANTIMATTER_BULLET =
            ITEMS.register("antimatter_bullet", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // ===== 虚空共振泵方块物品（使用条件说明） =====
    public static final RegistryObject<BlockItem> VOID_RESONANCE_PUMP_ITEM =
            ITEMS.register("void_resonance_pump",
                    () -> new DescriptionBlockItem(BlockRegistry.VOID_RESONANCE_PUMP.get(), new Item.Properties(),
                            "block.zuoyanmod.void_resonance_pump.desc2"));

    // ===== 微型强子对撞机（红石充能，双粒子束对撞）方块物品 =====
    public static final RegistryObject<BlockItem> MICRO_HADRON_COLLIDER_ITEM =
            ITEMS.register("micro_hadron_collider",
                    () -> new DescriptionBlockItem(BlockRegistry.MICRO_HADRON_COLLIDER.get(), new Item.Properties(),
                            "block.zuoyanmod.micro_hadron_collider.desc1"));

    // ===== 奇点核心（对撞产物：克莱因瓶的唯一入口材料） =====
    public static final RegistryObject<Item> SINGULARITY_CORE =
            ITEMS.register("singularity_core", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    // ===== 原始黑洞（对撞产物：沉重核心 + 暗物质；右键释放一个 20 秒的黑洞）=====
    // 继承 DescribedItem 是为了保留描述行机制；使用规则与具体数值见 PrimordialBlackHoleItem。
    public static final RegistryObject<PrimordialBlackHoleItem> PRIMORDIAL_BLACK_HOLE =
            ITEMS.register("primordial_black_hole", () -> new PrimordialBlackHoleItem(
                    new Item.Properties().rarity(Rarity.EPIC),
                    "item.zuoyanmod.primordial_black_hole.desc1",
                    "item.zuoyanmod.primordial_black_hole.desc2",
                    "item.zuoyanmod.primordial_black_hole.desc3"));

    // ===== 超流体暗物质（原「暗物质桶」，仅显示名变更，注册 id 保持 dark_matter_bucket） =====
    // craftRemainder(BUCKET)：它要当合成材料（真空衰变的配方要 4 个），必须像原版奶桶那样把空桶还回来，
    // 否则每合成一次就白吞 4 个铁桶。
    public static final RegistryObject<BucketItem> DARK_MATTER_BUCKET =
            ITEMS.register("dark_matter_bucket",
                    () -> new BucketItem(FluidRegistry.DARK_MATTER,
                            new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    // ===== 绝对零度（玻色-爱因斯坦凝聚：时停 + 暗物质液化成超流体） =====
    public static final RegistryObject<AbsoluteZeroItem> ABSOLUTE_ZERO =
            ITEMS.register("absolute_zero", () -> new AbsoluteZeroItem(
                    new Item.Properties().durability(AbsoluteZeroItem.MAX_USES).rarity(Rarity.EPIC)));

    // ===== 真空衰变（万能挖掘锤：普朗克解构 / 负熵灌注 / 分子离解 / 对称破缺） =====
    // 锤类：攻击力 137（1 基础 + 136）、攻速 4（4 基础 + 0，无速度惩罚）。
    // Tier uses=0 → 不可损坏（26.x 的"不设 durability"在 1.20.1 的等价写法）。
    public static final RegistryObject<VacuumDecayItem> VACUUM_DECAY =
            ITEMS.register("vacuum_decay", () -> new VacuumDecayItem(
                    VIOLET_GOLD_TIER, 136, 0.0F,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // ===== 克莱因瓶（随身存储终端 + 内置工作台 / 无燃料熔炉） =====
    public static final RegistryObject<KleinBottleItem> KLEIN_BOTTLE =
            ITEMS.register("klein_bottle", () -> new KleinBottleItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // ===== 领域展开 =====
    public static final RegistryObject<DomainExpansionItem> DOMAIN_EXPANSION =
            ITEMS.register("domain_expansion", () -> new DomainExpansionItem(new Item.Properties().stacksTo(1)));

    // ===== 紫金方块物品 =====
    public static final RegistryObject<BlockItem> VIOLET_GOLD_ORE_ITEM =
            ITEMS.register("violet_gold_ore",
                    () -> new BlockItem(BlockRegistry.VIOLET_GOLD_ORE.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> DEEPSLATE_VIOLET_GOLD_ORE_ITEM =
            ITEMS.register("deepslate_violet_gold_ore",
                    () -> new BlockItem(BlockRegistry.DEEPSLATE_VIOLET_GOLD_ORE.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> VIOLET_GOLD_BLOCK_ITEM =
            ITEMS.register("violet_gold_block",
                    () -> new BlockItem(BlockRegistry.VIOLET_GOLD_BLOCK.get(), new Item.Properties()));

    // ===== 因果律手枪（规则级武器：平行宇宙同位体；反物质子弹供弹） =====
    public static final RegistryObject<CausalityPistolItem> CAUSALITY_PISTOL =
            ITEMS.register("causality_pistol", () -> new CausalityPistolItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // ===== 音乐唱片（三首外部曲子，放进唱片机即可播放）=====
    // 1.20.1 适配：26.x 里唱片 = 普通 Item 挂 JUKEBOX_PLAYABLE 组件 + data/zuoyanmod/jukebox_song/*.json
    // 描述曲目元数据；1.20.1 没有这套机制，唱片就是原版 {@code RecordItem}。
    // 前三参（比较器输出、音效、长度）取自 26.3 的 jukebox_song json 里的
    // comparator_output / length_in_seconds —— 注意该构造器的长度单位是 **tick**（不是秒），
    // 原版 3 参重载内部才 *20，这里直接给秒会变成「放 3 秒就停」。
    public static final RegistryObject<RecordItem> MUSIC_DISC_SHOTS =
            ITEMS.register("music_disc_shots", () -> new RecordItem(
                    1, SoundRegistry.MUSIC_DISC_SHOTS, new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                    SECONDS_TO_TICKS(191.6)));

    public static final RegistryObject<RecordItem> MUSIC_DISC_NIGHT_DANCER =
            ITEMS.register("music_disc_night_dancer", () -> new RecordItem(
                    2, SoundRegistry.MUSIC_DISC_NIGHT_DANCER, new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                    SECONDS_TO_TICKS(211.0)));

    public static final RegistryObject<RecordItem> MUSIC_DISC_CASTLE =
            ITEMS.register("music_disc_castle", () -> new RecordItem(
                    3, SoundRegistry.MUSIC_DISC_CASTLE, new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                    SECONDS_TO_TICKS(187.0)));

    /** 26.3 的曲目时长按「秒」写在 jukebox_song json 里，1.20.1 要 tick —— 换算收在这一处。 */
    private static int SECONDS_TO_TICKS(double seconds) {
        return (int) Math.ceil(seconds * 20.0D);
    }

    // ===== 生物刷怪蛋 =====
    // 1.20.1 用 Forge 的 ForgeSpawnEggItem：它接的是 EntityType 的 Supplier，
    // 因此物品构造时不必要求实体类型已经注册完（26.x 走 ENTITY_DATA 组件）。
    public static final RegistryObject<SpawnEggItem> RICK_SPAWN_EGG =
            ITEMS.register("rick_spawn_egg",
                    () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                            org.gwfx.zuoyanmod.entity.EntityRegistry.RICK,
                            0xDDE6EE, 0x8FD8F0,
                            new Item.Properties()));

    private ItemRegistry() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
