package org.gwfx.zuoyanmod.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.entity.EntityRegistry;
import org.gwfx.zuoyanmod.sound.SoundRegistry;
import org.gwfx.zuoyanmod.fluid.FluidRegistry;
import org.gwfx.zuoyanmod.block.BlockRegistry;

import java.util.List;
import java.util.Map;

public final class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Zuoyanmod.MODID);

    // ===== 紫金盔甲材质（1.21.1 的 ArmorMaterial 是注册表 record，必须先注册拿 Holder） =====
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.ARMOR_MATERIAL, Zuoyanmod.MODID);

    // ===== 紫金材料链（须在紫金装备之前注册，装备 repairable 引用紫金锭） =====
    public static final DeferredItem<Item> RAW_VIOLET_GOLD = ITEMS.registerItem(
            "raw_violet_gold",
            Item::new
    );

    public static final DeferredItem<VioletGoldIngotItem> VIOLET_GOLD_INGOT = ITEMS.registerItem(
            "violet_gold_ingot",
            VioletGoldIngotItem::new
    );

    // 圣遗物核心：终局合成材料
    public static final DeferredItem<Item> RELIC_CORE = ITEMS.registerItem(
            "relic_core",
            props -> new Item(props.rarity(Rarity.EPIC))
    );

    // 圣辉锻造模板：锻造台升级下界合金盔甲为紫金神装
    // 1.21.1 的 SmithingTemplateItem 自己 new Properties，外部 Properties 传不进去
    //（物品名/堆叠等走默认，模板本就无需 stacksTo(1) 之外的特殊属性）。
    public static final DeferredItem<SmithingTemplateItem> HALLOWED_UPGRADE_SMITHING_TEMPLATE = ITEMS.registerItem(
            "hallowed_upgrade_smithing_template",
            props -> new SmithingTemplateItem(
                    Component.translatable("item.zuoyanmod.hallowed_upgrade_smithing_template.applies_to"),
                    Component.translatable("item.zuoyanmod.hallowed_upgrade_smithing_template.ingredients"),
                    Component.translatable("item.zuoyanmod.hallowed_upgrade_smithing_template.upgrade_description"),
                    Component.translatable("item.zuoyanmod.hallowed_upgrade_smithing_template.base_slot_description"),
                    Component.translatable("item.zuoyanmod.hallowed_upgrade_smithing_template.additions_slot_description"),
                    List.of(
                            ResourceLocation.withDefaultNamespace("container/slot/helmet"),
                            ResourceLocation.withDefaultNamespace("container/slot/chestplate"),
                            ResourceLocation.withDefaultNamespace("container/slot/leggings"),
                            ResourceLocation.withDefaultNamespace("container/slot/boots")
                    ),
                    List.of(ResourceLocation.withDefaultNamespace("container/slot/diamond"))
            )
    );

    // ===== 消耗与功能道具 =====
    public static final DeferredItem<ChocolateCrispItem> CHOCOLATE_CRISP = ITEMS.registerItem(
            "chocolate_crisp",
            props -> new ChocolateCrispItem(props.stacksTo(16).food(
                    new FoodProperties.Builder()
                            .nutrition(3)
                            .saturationModifier(0.5f)
                            .alwaysEdible()
                            .build()))
    );

    public static final DeferredItem<SpriteDrinkItem> SPRITE_DRINK = ITEMS.registerItem(
            "sprite_drink",
            props -> new SpriteDrinkItem(props.stacksTo(16).food(
                    new FoodProperties.Builder()
                            .nutrition(3)
                            .saturationModifier(0.5f)
                            .alwaysEdible()
                            .build()))
    );

    public static final DeferredItem<DeathNoteItem> DEATH_NOTE = ITEMS.registerItem(
            "death_note",
            props -> new DeathNoteItem(props.stacksTo(1))
    );

    public static final DeferredItem<MingDaoSiMingItem> MING_DAO_SI_MING = ITEMS.registerItem(
            "ming_dao_si_ming",
            props -> new MingDaoSiMingItem(props.stacksTo(1))
    );

    public static final DeferredItem<SpaceAnchorItem> SPACE_ANCHOR = ITEMS.registerItem(
            "space_anchor",
            props -> new SpaceAnchorItem(props.stacksTo(1))
    );

    public static final DeferredItem<BeimingBlade> BEIMING_BLADE = ITEMS.registerItem(
            "beiming_blade",
            // 剑类武器：攻击力 33（1 基础 + 32）、攻速 3（4 基础 - 1）、攻击距离正常
            props -> new BeimingBlade(swordComponents(props.stacksTo(1).attributes(swordAttributes(32.0F, -1.0F))))
    );

    /**
     * 自定义铜质 Tier（1.21.1 原版 Tiers 没有铜档）：耐久/速度/伤害加成取石与铁的中间值，
     * 附魔能力 13、修复材料铜锭，采集门槛沿用 {@code #incorrect_for_stone_tool}。
     * 声明在万能工具之前，避免静态初始化的非法前向引用。
     */
    public static final Tier COPPER_TIER = new Tier() {
        @Override
        public int getUses() {
            return 190;
        }

        @Override
        public float getSpeed() {
            return 5.0F;
        }

        @Override
        public float getAttackDamageBonus() {
            return 1.5F;
        }

        @Override
        public net.minecraft.tags.TagKey<Block> getIncorrectBlocksForDrops() {
            return net.minecraft.tags.BlockTags.INCORRECT_FOR_STONE_TOOL;
        }

        @Override
        public int getEnchantmentValue() {
            return 13;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(Items.COPPER_INGOT);
        }
    };

    // ===== 万能工具（镐·斧·铲·锄·剑 五合一，数值取自对应 Tier，攻击取同材质斧） =====
    public static final DeferredItem<UniversalToolItem> WOODEN_UNIVERSAL_TOOL = ITEMS.registerItem(
            "wooden_universal_tool",
            props -> new UniversalToolItem(Tiers.WOOD, UniversalToolItem.properties(Tiers.WOOD, 6.0F, -3.2F))
    );

    public static final DeferredItem<UniversalToolItem> STONE_UNIVERSAL_TOOL = ITEMS.registerItem(
            "stone_universal_tool",
            props -> new UniversalToolItem(Tiers.STONE, UniversalToolItem.properties(Tiers.STONE, 7.0F, -3.2F))
    );

    public static final DeferredItem<UniversalToolItem> COPPER_UNIVERSAL_TOOL = ITEMS.registerItem(
            "copper_universal_tool",
            // 原版铜斧基线：伤害 7.0、攻速 -3.2（1.21.1 没有铜质工具档，这里用自定义铜 Tier，
            // 数值介于石与铁之间，采集门槛取石头一档）
            props -> new UniversalToolItem(COPPER_TIER, UniversalToolItem.properties(COPPER_TIER, 7.0F, -3.2F))
    );    public static final DeferredItem<UniversalToolItem> GOLDEN_UNIVERSAL_TOOL = ITEMS.registerItem(
            "golden_universal_tool",
            props -> new UniversalToolItem(Tiers.GOLD, UniversalToolItem.properties(Tiers.GOLD, 6.0F, -3.0F))
    );

    public static final DeferredItem<UniversalToolItem> IRON_UNIVERSAL_TOOL = ITEMS.registerItem(
            "iron_universal_tool",
            props -> new UniversalToolItem(Tiers.IRON, UniversalToolItem.properties(Tiers.IRON, 6.0F, -3.1F))
    );

    public static final DeferredItem<UniversalToolItem> DIAMOND_UNIVERSAL_TOOL = ITEMS.registerItem(
            "diamond_universal_tool",
            props -> new UniversalToolItem(Tiers.DIAMOND, UniversalToolItem.properties(Tiers.DIAMOND, 5.0F, -3.0F))
    );

    public static final DeferredItem<UniversalToolItem> NETHERITE_UNIVERSAL_TOOL = ITEMS.registerItem(
            "netherite_universal_tool",
            // 下界合金同原版：防火不掉落（岩浆里烧不坏）
            props -> new UniversalToolItem(Tiers.NETHERITE,
                    UniversalToolItem.properties(Tiers.NETHERITE, 5.0F, -3.0F).fireResistant())
    );

    // ===== 阶段三新武器 =====
    public static final DeferredItem<HerculesBowItem> HERCULES_BOW = ITEMS.registerItem(
            "hercules_bow",
            // 修复材料在 HerculesBowItem#isValidRepairItem 里判（1.21.1 的 Properties 没有 repairable）
            props -> new HerculesBowItem(props.stacksTo(1).durability(1000))
    );

    public static final DeferredItem<JackTheRipperScalpelItem> JACK_THE_RIPPER_SCALPEL = ITEMS.registerItem(
            "jack_the_ripper_scalpel",
            // 匕首类：攻击力 5（1 基础 + 4）、攻速 5.5（4 基础 + 1.5）、攻击距离比剑略短（3.0 - 0.5 = 2.5 格）
            props -> new JackTheRipperScalpelItem(swordComponents(props.stacksTo(1).attributes(ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE,
                            new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 4.0F, AttributeModifier.Operation.ADD_VALUE),
                            EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ATTACK_SPEED,
                            new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, 1.5F, AttributeModifier.Operation.ADD_VALUE),
                            EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ENTITY_INTERACTION_RANGE,
                            new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Zuoyanmod.MODID, "scalpel_reach"), -0.5F, AttributeModifier.Operation.ADD_VALUE),
                            EquipmentSlotGroup.MAINHAND)
                    .build())))
    );

    // ===== 紫金材质定义（1.21.1：注册进 ARMOR_MATERIAL 注册表拿 Holder） =====
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> VIOLETGOLD_MATERIAL =
            ARMOR_MATERIALS.register("violetgold", () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.BOOTS, 25,
                            ArmorItem.Type.LEGGINGS, 35,
                            ArmorItem.Type.CHESTPLATE, 50,
                            ArmorItem.Type.HELMET, 25
                    ),
                    20, // 附魔能力
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(VIOLET_GOLD_INGOT.get()), // 修复材料：紫金锭
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(Zuoyanmod.MODID, "violetgold"))),
                    15.0F, // 盔甲韧性
                    3.0F // 击退抗性
            ));

    /** 紫金盔甲耐久倍率（近乎无限耐久），乘在原版各槽位的基础耐久上 */
    public static final int VIOLETGOLD_DURABILITY_FACTOR = 999999;

    // ===== 紫金神装四件套注册 =====
    public static final DeferredItem<ShadowArmorItem> SHADOW_HELMET = ITEMS.registerItem(
            "shadow_helmet",
            props -> new ShadowArmorItem(VIOLETGOLD_MATERIAL, props
                    .durability(ArmorItem.Type.HELMET.getDurability(VIOLETGOLD_DURABILITY_FACTOR))
                    .stacksTo(1))
    );

    public static final DeferredItem<ShengTianChestplate> SHENG_TIAN_CHESTPLATE = ITEMS.registerItem(
            "shengtian_chestplate",
            props -> new ShengTianChestplate(VIOLETGOLD_MATERIAL, props
                    .durability(ArmorItem.Type.CHESTPLATE.getDurability(VIOLETGOLD_DURABILITY_FACTOR))
                    .stacksTo(1))
    );

    public static final DeferredItem<WindLeggings> WIND_LEGGINGS = ITEMS.registerItem(
            "wind_leggings",
            props -> new WindLeggings(VIOLETGOLD_MATERIAL, props
                    .durability(ArmorItem.Type.LEGGINGS.getDurability(VIOLETGOLD_DURABILITY_FACTOR))
                    .stacksTo(1))
    );

    public static final DeferredItem<WalkerBoots> WALKER_BOOTS = ITEMS.registerItem(
            "walker_boots",
            props -> new WalkerBoots(VIOLETGOLD_MATERIAL, props
                    .durability(ArmorItem.Type.BOOTS.getDurability(VIOLETGOLD_DURABILITY_FACTOR))
                    .stacksTo(1))
    );

    // ===== 饰品类物品（原 Curios 槽位，现在放入背包即生效） =====
    public static final DeferredItem<RingItem> RING_OF_KILLS = ITEMS.registerItem(
            "ring_of_kills",
            props -> new RingItem(props.stacksTo(1))
    );

    public static final DeferredItem<WanHuiRingItem> WAN_HUI_RING = ITEMS.registerItem(
            "wan_hui_ring",
            props -> new WanHuiRingItem(props.stacksTo(1))
    );

    public static final DeferredItem<VoodooNecklaceItem> VOODOO_NECKLACE = ITEMS.registerItem(
            "voodoo_necklace",
            props -> new VoodooNecklaceItem(props.stacksTo(1))
    );

    public static final DeferredItem<YemengadeVenomFangItem> YEMENGADE_VENOM_FANG = ITEMS.registerItem(
            "yemengade_venom_fang",
            props -> new YemengadeVenomFangItem(props.stacksTo(1))
    );

    public static final DeferredItem<CounterBeltItem> COUNTER_BELT = ITEMS.registerItem(
            "counter_belt",
            props -> new CounterBeltItem(props.stacksTo(1))
    );

    // ===== 暗物质（终局合成材料，由虚空共振泵产出） =====
    public static final DeferredItem<Item> DARK_MATTER = ITEMS.registerItem(
            "dark_matter",
            props -> new Item(props.rarity(Rarity.EPIC))
    );

    // ===== 暗物质粒子（虚空共振泵的直接产物） =====
    public static final DeferredItem<DarkMatterParticleItem> DARK_MATTER_PARTICLE = ITEMS.registerItem(
            "dark_matter_particle",
            props -> new DarkMatterParticleItem(props.rarity(Rarity.UNCOMMON))
    );

    // ===== 反物质微粒（微型强子对撞机产物：潮涌核心 + 烈焰棒） =====
    public static final DeferredItem<Item> ANTIMATTER_PARTICLE = ITEMS.registerItem(
            "antimatter_particle",
            props -> new Item(props.rarity(Rarity.RARE))
    );

    // ===== 反物质子弹（因果律手枪的专用弹药：生存模式每发消耗 1 枚） =====
    public static final DeferredItem<Item> ANTIMATTER_BULLET = ITEMS.registerItem(
            "antimatter_bullet",
            props -> new Item(props.rarity(Rarity.UNCOMMON))
    );

    // ===== 虚空共振泵方块物品（使用条件说明） =====
    public static final DeferredItem<BlockItem> VOID_RESONANCE_PUMP_ITEM = ITEMS.registerItem(
            "void_resonance_pump",
            props -> new DescriptionBlockItem(BlockRegistry.VOID_RESONANCE_PUMP.get(), props,
                    "block.zuoyanmod.void_resonance_pump.desc2")
    );

    // ===== 微型强子对撞机（红石充能，双粒子束对撞）方块物品 =====
    public static final DeferredItem<BlockItem> MICRO_HADRON_COLLIDER_ITEM = ITEMS.registerItem(
            "micro_hadron_collider",
            props -> new DescriptionBlockItem(BlockRegistry.MICRO_HADRON_COLLIDER.get(), props,
                    "block.zuoyanmod.micro_hadron_collider.desc1")
    );

    // ===== 奇点核心（对撞产物：克莱因瓶的唯一入口材料） =====
    public static final DeferredItem<Item> SINGULARITY_CORE = ITEMS.registerItem(
            "singularity_core",
            props -> new Item(props.rarity(Rarity.RARE))
    );

    // ===== 原始黑洞（对撞产物：沉重核心 + 暗物质；右键释放一个 10 秒的黑洞）=====
    // 继承 DescribedItem 是为了保留描述行机制；使用规则与具体数值见 PrimordialBlackHoleItem。
    public static final DeferredItem<DescribedItem> PRIMORDIAL_BLACK_HOLE = ITEMS.registerItem(
            "primordial_black_hole",
            props -> new PrimordialBlackHoleItem(props.rarity(Rarity.EPIC),
                    "item.zuoyanmod.primordial_black_hole.desc1",
                    "item.zuoyanmod.primordial_black_hole.desc2",
                    "item.zuoyanmod.primordial_black_hole.desc3")
    );

    // ===== 天赋重置卷轴（右键清空已选终极天赋，可重新选择；基础加点不受影响）=====
    public static final DeferredItem<TalentResetItem> TALENT_RESET_SCROLL = ITEMS.registerItem(
            "talent_reset_scroll",
            props -> new TalentResetItem(props.stacksTo(16).rarity(Rarity.RARE),
                    "item.zuoyanmod.talent_reset_scroll.desc1",
                    "item.zuoyanmod.talent_reset_scroll.desc2")
    );

    // ===== 超流体暗物质（原「暗物质桶」，仅显示名变更，注册 id 保持 dark_matter_bucket） =====
    // craftRemainder(BUCKET)：它要当合成材料（真空衰变的配方要 4 个），必须像原版奶桶那样把空桶还回来，
    // 否则每合成一次就白吞 4 个铁桶。
    public static final DeferredItem<BucketItem> DARK_MATTER_BUCKET = ITEMS.registerItem(
            "dark_matter_bucket",
            props -> new BucketItem(FluidRegistry.DARK_MATTER.get(),
                    props.stacksTo(1).craftRemainder(Items.BUCKET))
    );

    // ===== 绝对零度（玻色-爱因斯坦凝聚：时停 + 暗物质液化成超流体） =====
    public static final DeferredItem<AbsoluteZeroItem> ABSOLUTE_ZERO = ITEMS.registerItem(
            "absolute_zero",
            props -> new AbsoluteZeroItem(props.durability(AbsoluteZeroItem.MAX_USES).rarity(Rarity.EPIC))
    );

    // ===== 真空衰变（万能挖掘锤：普朗克解构 / 负熵灌注 / 分子离解 / 对称破缺） =====
    public static final DeferredItem<VacuumDecayItem> VACUUM_DECAY = ITEMS.registerItem(
            "vacuum_decay",
            // 锤类：攻击力 137（1 基础 + 136）、攻速 4（4 基础 + 0，无速度惩罚）、
            // 攻击距离 +2.73（3.0 基础 → 5.73 格）。
            // 不设 durability → 天生不可损坏（1.21.1 里"没有 max_damage 组件"= 无限耐久），
            // 因此也不需要修复材料。
            props -> new VacuumDecayItem(props.stacksTo(1)
                    .rarity(Rarity.EPIC)
                    .attributes(ItemAttributeModifiers.builder()
                            .add(Attributes.ATTACK_DAMAGE,
                                    new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 136.0F, AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .add(Attributes.ATTACK_SPEED,
                                    new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, 0.0F, AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .add(Attributes.ENTITY_INTERACTION_RANGE,
                                    new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Zuoyanmod.MODID, "vacuum_decay_reach"), 2.73F, AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .build()))
    );

    // ===== 因果律手枪（规则级武器：平行宇宙同位体；反物质子弹供弹） =====
    public static final DeferredItem<CausalityPistolItem> CAUSALITY_PISTOL = ITEMS.registerItem(
            "causality_pistol",
            props -> new CausalityPistolItem(props.stacksTo(1).rarity(Rarity.EPIC))
    );

    // ===== 克莱因瓶（随身存储终端 + 内置工作台 / 无燃料熔炉） =====
    public static final DeferredItem<KleinBottleItem> KLEIN_BOTTLE = ITEMS.registerItem(
            "klein_bottle",
            props -> new KleinBottleItem(props.stacksTo(1).rarity(Rarity.EPIC))
    );

    // ===== 领域展开 =====
    public static final DeferredItem<DomainExpansionItem> DOMAIN_EXPANSION = ITEMS.registerItem(
            "domain_expansion",
            props -> new DomainExpansionItem(props.stacksTo(1))
    );

    // ===== 紫金方块物品 =====
    public static final DeferredItem<BlockItem> VIOLET_GOLD_ORE_ITEM = ITEMS.registerSimpleBlockItem(
            "violet_gold_ore", BlockRegistry.VIOLET_GOLD_ORE
    );

    public static final DeferredItem<BlockItem> DEEPSLATE_VIOLET_GOLD_ORE_ITEM = ITEMS.registerSimpleBlockItem(
            "deepslate_violet_gold_ore", BlockRegistry.DEEPSLATE_VIOLET_GOLD_ORE
    );

    public static final DeferredItem<BlockItem> VIOLET_GOLD_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(
            "violet_gold_block", BlockRegistry.VIOLET_GOLD_BLOCK
    );

    // ===== 草原传送门（门框 + 传送门本体）方块物品 =====
    // 草原门框：描述里写明"打火石点燃"的开启方式（点燃入口见 GrassPortalEventHandler）。
    // 1.21.1 的 BlockItem#getDescriptionId 本来就返回方块的描述键（block. 前缀），
    // 无需任何额外配置即可复用 block.zuoyanmod.grass_portal_frame 的翻译。
    public static final DeferredItem<BlockItem> GRASS_PORTAL_FRAME_ITEM = ITEMS.registerItem(
            "grass_portal_frame",
            props -> new DescriptionBlockItem(BlockRegistry.GRASS_PORTAL_FRAME.get(), props,
                    "block.zuoyanmod.grass_portal_frame.desc1")
    );

    public static final DeferredItem<BlockItem> GRASS_PORTAL_ITEM = ITEMS.registerSimpleBlockItem(
            "grass_portal", BlockRegistry.GRASS_PORTAL
    );

    // ===== 生物刷怪蛋 =====
    // 1.21.1 原版 SpawnEggItem 直接收 EntityType，NeoForge 提供 DeferredSpawnEggItem
    // 收 Supplier——注册期不触碰实体注册表，注册顺序天然安全。
    public static final DeferredItem<DeferredSpawnEggItem> RICK_SPAWN_EGG = ITEMS.registerItem(
            "rick_spawn_egg",
            props -> new DeferredSpawnEggItem(EntityRegistry.RICK, 0x6B4F2E, 0xD2B48C, props)
    );

    public static final DeferredItem<DeferredSpawnEggItem> VOID_MONARCH_SPAWN_EGG = ITEMS.registerItem(
            "void_monarch_spawn_egg",
            props -> new DeferredSpawnEggItem(EntityRegistry.VOID_MONARCH, 0x1A0B2E, 0x8A2BE2, props)
    );

    public static final DeferredItem<DeferredSpawnEggItem> VOID_GUARD_SPAWN_EGG = ITEMS.registerItem(
            "void_guard_spawn_egg",
            props -> new DeferredSpawnEggItem(EntityRegistry.VOID_GUARD, 0x2B1B3D, 0x4F86F7, props)
    );

    // ===== 湮灭君王之刃（湮灭君主必掉武器：攻击 35、攻速 2.4、对 Boss 标签目标追加魔法伤害） =====
    public static final DeferredItem<MonarchBladeItem> MONARCH_BLADE = ITEMS.registerItem(
            "monarch_blade",
            // 攻击力 35（1 基础 + 34）、攻速 2.4（4 基础 - 1.6）：重剑手感
            props -> new MonarchBladeItem(swordComponents(props.stacksTo(1).rarity(Rarity.EPIC)
                    .attributes(swordAttributes(34.0F, -1.6F))))
    );


    // ===== 音乐唱片（三首外部曲子，放进唱片机即可播放）=====
    // 1.21.1 的唱片 = 普通 Item 挂 JUKEBOX_PLAYABLE 组件，曲目元数据（时长 / 比较器输出 / 描述）
    // 在 data/zuoyanmod/jukebox_song/*.json。这里用 ResourceKey 延迟引用点歌注册表，
    // 注册期不触碰点歌数据；但如果对应 json 缺失，放唱片时会直接失败——两个文件必须成对存在。
    // stacksTo(1)：唱片机一次只收 1 张，原版唱片同样是 1。
    public static final DeferredItem<Item> MUSIC_DISC_SHOTS = ITEMS.registerItem(
            "music_disc_shots",
            props -> new Item(props.stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(SoundRegistry.SONG_SHOTS))
    );

    public static final DeferredItem<Item> MUSIC_DISC_NIGHT_DANCER = ITEMS.registerItem(
            "music_disc_night_dancer",
            props -> new Item(props.stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(SoundRegistry.SONG_NIGHT_DANCER))
    );

    public static final DeferredItem<Item> MUSIC_DISC_CASTLE = ITEMS.registerItem(
            "music_disc_castle",
            props -> new Item(props.stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(SoundRegistry.SONG_CASTLE))
    );

    private ItemRegistry() {}

    /** 剑类攻击属性：最终攻击力 = 1 + damageBonus，最终攻速 = 4 + speedBonus */
    private static ItemAttributeModifiers swordAttributes(float damageBonus, float speedBonus) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damageBonus, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speedBonus, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    /** 附加剑类公共组件：快速破坏蜘蛛网（1.21.1 无 WEAPON 组件，命中耗耐久是默认行为）、可附魔 */
    private static Item.Properties swordComponents(Item.Properties props) {
        return props
                .component(DataComponents.TOOL, new Tool(
                        List.of(Tool.Rule.minesAndDrops(List.of(Blocks.COBWEB), 15.0F)),
                        1.0F, 2));
    }

    public static void register(IEventBus modBus) {
        ARMOR_MATERIALS.register(modBus);
        ITEMS.register(modBus);
    }
}
