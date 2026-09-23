package org.gwfx.zuoyanmod.item;

import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.entity.EntityRegistry;
import org.gwfx.zuoyanmod.fluid.FluidRegistry;
import org.gwfx.zuoyanmod.block.BlockRegistry;

import java.util.List;
import java.util.Map;

public final class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Zuoyanmod.MODID);

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
            Item::new,
            props -> props.rarity(Rarity.EPIC)
    );

    // 圣辉锻造模板：锻造台升级下界合金盔甲为紫金神装
    public static final DeferredItem<SmithingTemplateItem> HALLOWED_UPGRADE_SMITHING_TEMPLATE = ITEMS.registerItem(
            "hallowed_upgrade_smithing_template",
            props -> new SmithingTemplateItem(
                    Component.translatable("item.zuoyanmod.hallowed_upgrade_smithing_template.applies_to"),
                    Component.translatable("item.zuoyanmod.hallowed_upgrade_smithing_template.ingredients"),
                    Component.translatable("item.zuoyanmod.hallowed_upgrade_smithing_template.base_slot_description"),
                    Component.translatable("item.zuoyanmod.hallowed_upgrade_smithing_template.additions_slot_description"),
                    List.of(
                            Identifier.withDefaultNamespace("container/slot/helmet"),
                            Identifier.withDefaultNamespace("container/slot/chestplate"),
                            Identifier.withDefaultNamespace("container/slot/leggings"),
                            Identifier.withDefaultNamespace("container/slot/boots")
                    ),
                    List.of(Identifier.withDefaultNamespace("container/slot/diamond")),
                    props
            ),
            props -> props.stacksTo(1)
    );

    // ===== 消耗与功能道具 =====
    public static final DeferredItem<IceTeaItem> ICE_TEA = ITEMS.registerItem(
            "ice_tea",
            IceTeaItem::new,
            props -> props.stacksTo(16).food(
                    new FoodProperties.Builder()
                            .nutrition(3)
                            .saturationModifier(0.5f)
                            .alwaysEdible()
                            .build()
            )
    );

    public static final DeferredItem<SpriteDrinkItem> SPRITE_DRINK = ITEMS.registerItem(
            "sprite_drink",
            SpriteDrinkItem::new,
            props -> props.stacksTo(16).food(
                    new FoodProperties.Builder()
                            .nutrition(3)
                            .saturationModifier(0.5f)
                            .alwaysEdible()
                            .build()
            )
    );

    public static final DeferredItem<DeathNoteItem> DEATH_NOTE = ITEMS.registerItem(
            "death_note",
            DeathNoteItem::new,
            props -> props.stacksTo(1)
    );

    public static final DeferredItem<MingDaoSiMingItem> MING_DAO_SI_MING = ITEMS.registerItem(
            "ming_dao_si_ming",
            MingDaoSiMingItem::new,
            props -> props.stacksTo(1)
    );

    public static final DeferredItem<SpaceAnchorItem> SPACE_ANCHOR = ITEMS.registerItem(
            "space_anchor",
            SpaceAnchorItem::new,
            props -> props.stacksTo(1)
    );

    public static final DeferredItem<BeimingBlade> BEIMING_BLADE = ITEMS.registerItem(
            "beiming_blade",
            BeimingBlade::new,
            // 剑类武器：攻击力 33（1 基础 + 32）、攻速 3（4 基础 - 1）、攻击距离正常
            props -> swordComponents(props.stacksTo(1).attributes(swordAttributes(32.0F, -1.0F)))
    );

    // ===== 阶段三新武器 =====
    public static final DeferredItem<HerculesBowItem> HERCULES_BOW = ITEMS.registerItem(
            "hercules_bow",
            HerculesBowItem::new,
            props -> props.stacksTo(1).durability(1000).repairable(VIOLET_GOLD_INGOT.get())
    );

    public static final DeferredItem<JackTheRipperScalpelItem> JACK_THE_RIPPER_SCALPEL = ITEMS.registerItem(
            "jack_the_ripper_scalpel",
            JackTheRipperScalpelItem::new,
            // 匕首类：攻击力 5（1 基础 + 4）、攻速 5.5（4 基础 + 1.5）、攻击距离比剑略短（3.0 - 0.5 = 2.5 格）
            props -> swordComponents(props.stacksTo(1).attributes(ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE,
                            new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 4.0F, AttributeModifier.Operation.ADD_VALUE),
                            EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ATTACK_SPEED,
                            new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, 1.5F, AttributeModifier.Operation.ADD_VALUE),
                            EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ENTITY_INTERACTION_RANGE,
                            new AttributeModifier(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "scalpel_reach"), -0.5F, AttributeModifier.Operation.ADD_VALUE),
                            EquipmentSlotGroup.MAINHAND)
                    .build()))
    );

    // ===== 紫金材质定义 (26.x Record) =====
    public static final ResourceKey<EquipmentAsset> VIOLETGOLD_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "violetgold")
    );

    public static final ArmorMaterial VIOLETGOLD_MATERIAL = new ArmorMaterial(
            999999, // 耐久倍率（近乎无限耐久）
            Map.of(
                    ArmorType.BOOTS, 25,
                    ArmorType.LEGGINGS, 35,
                    ArmorType.CHESTPLATE, 50,
                    ArmorType.HELMET, 25
            ),
            20, // 附魔能力
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            15.0F, // 盔甲韧性
            3.0F, // 击退抗性
            ItemTags.REPAIRS_NETHERITE_ARMOR,
            VIOLETGOLD_ASSET
    );

    // ===== 紫金神装四件套注册 =====
    public static final DeferredItem<ShadowArmorItem> SHADOW_HELMET = ITEMS.registerItem(
            "shadow_helmet",
            ShadowArmorItem::new,
            props -> props.humanoidArmor(VIOLETGOLD_MATERIAL, ArmorType.HELMET).stacksTo(1).repairable(VIOLET_GOLD_INGOT.get())
    );

    public static final DeferredItem<ShengTianChestplate> SHENG_TIAN_CHESTPLATE = ITEMS.registerItem(
            "shengtian_chestplate",
            ShengTianChestplate::new,
            props -> props.humanoidArmor(VIOLETGOLD_MATERIAL, ArmorType.CHESTPLATE).stacksTo(1).repairable(VIOLET_GOLD_INGOT.get())
    );

    public static final DeferredItem<WindLeggings> WIND_LEGGINGS = ITEMS.registerItem(
            "wind_leggings",
            WindLeggings::new,
            props -> props.humanoidArmor(VIOLETGOLD_MATERIAL, ArmorType.LEGGINGS).stacksTo(1).repairable(VIOLET_GOLD_INGOT.get())
    );

    public static final DeferredItem<WalkerBoots> WALKER_BOOTS = ITEMS.registerItem(
            "walker_boots",
            WalkerBoots::new,
            props -> props.humanoidArmor(VIOLETGOLD_MATERIAL, ArmorType.BOOTS).stacksTo(1).repairable(VIOLET_GOLD_INGOT.get())
    );

    // ===== 饰品类物品（原 Curios 槽位，现在放入背包即生效） =====
    public static final DeferredItem<RingItem> RING_OF_KILLS = ITEMS.registerItem(
            "ring_of_kills",
            RingItem::new,
            props -> props.stacksTo(1)
    );

    public static final DeferredItem<WanHuiRingItem> WAN_HUI_RING = ITEMS.registerItem(
            "wan_hui_ring",
            WanHuiRingItem::new,
            props -> props.stacksTo(1)
    );

    public static final DeferredItem<VoodooNecklaceItem> VOODOO_NECKLACE = ITEMS.registerItem(
            "voodoo_necklace",
            VoodooNecklaceItem::new,
            props -> props.stacksTo(1)
    );

    public static final DeferredItem<YemengadeVenomFangItem> YEMENGADE_VENOM_FANG = ITEMS.registerItem(
            "yemengade_venom_fang",
            YemengadeVenomFangItem::new,
            props -> props.stacksTo(1)
    );

    public static final DeferredItem<CounterBeltItem> COUNTER_BELT = ITEMS.registerItem(
            "counter_belt",
            CounterBeltItem::new,
            props -> props.stacksTo(1)
    );

    // ===== 暗物质（终局合成材料，由虚空共振泵产出） =====
    public static final DeferredItem<Item> DARK_MATTER = ITEMS.registerItem(
            "dark_matter",
            Item::new,
            props -> props.rarity(Rarity.EPIC)
    );

    // ===== 暗物质粒子（虚空共振泵的直接产物） =====
    public static final DeferredItem<DarkMatterParticleItem> DARK_MATTER_PARTICLE = ITEMS.registerItem(
            "dark_matter_particle",
            DarkMatterParticleItem::new,
            props -> props.rarity(Rarity.UNCOMMON)
    );

    // ===== 反物质微粒（微型强子对撞机产物：潮涌核心 + 烈焰棒） =====
    public static final DeferredItem<Item> ANTIMATTER_PARTICLE = ITEMS.registerItem(
            "antimatter_particle",
            Item::new,
            props -> props.rarity(Rarity.RARE)
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
            Item::new,
            props -> props.rarity(Rarity.RARE)
    );

    // ===== 超流体暗物质（原「暗物质桶」，仅显示名变更，注册 id 保持 dark_matter_bucket） =====
    // craftRemainder(BUCKET)：它要当合成材料（真空衰变的配方要 4 个），必须像原版奶桶那样把空桶还回来，
    // 否则每合成一次就白吞 4 个铁桶。
    public static final DeferredItem<BucketItem> DARK_MATTER_BUCKET = ITEMS.registerItem(
            "dark_matter_bucket",
            props -> new BucketItem(FluidRegistry.DARK_MATTER.get(), props),
            props -> props.stacksTo(1).craftRemainder(Items.BUCKET)
    );

    // ===== 绝对零度（玻色-爱因斯坦凝聚：时停 + 暗物质液化成超流体） =====
    public static final DeferredItem<AbsoluteZeroItem> ABSOLUTE_ZERO = ITEMS.registerItem(
            "absolute_zero",
            AbsoluteZeroItem::new,
            props -> props.durability(AbsoluteZeroItem.MAX_USES).rarity(Rarity.EPIC)
    );

    // ===== 真空衰变（万能挖掘锤：普朗克解构 / 负熵灌注 / 分子离解 / 对称破缺） =====
    public static final DeferredItem<VacuumDecayItem> VACUUM_DECAY = ITEMS.registerItem(
            "vacuum_decay",
            VacuumDecayItem::new,
            // 锤类：攻击力 137（1 基础 + 136）、攻速 4（4 基础 + 0，无速度惩罚）、
            // 攻击距离 +2.73（3.0 基础 → 5.73 格）。
            // 不设 durability → 天生不可损坏（26.x 里"没有 max_damage 组件"= 无限耐久），
            // 因此也不需要 repairable。
            props -> universalToolComponents(props.stacksTo(1)
                    .rarity(Rarity.EPIC)
                    .attributes(ItemAttributeModifiers.builder()
                            .add(Attributes.ATTACK_DAMAGE,
                                    new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 136.0F, AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .add(Attributes.ATTACK_SPEED,
                                    new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, 0.0F, AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .add(Attributes.ENTITY_INTERACTION_RANGE,
                                    new AttributeModifier(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "vacuum_decay_reach"), 2.73F, AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .build()))
    );

    // ===== 克莱因瓶（随身存储终端 + 内置工作台 / 无燃料熔炉） =====
    public static final DeferredItem<KleinBottleItem> KLEIN_BOTTLE = ITEMS.registerItem(
            "klein_bottle",
            KleinBottleItem::new,
            props -> props.stacksTo(1).rarity(Rarity.EPIC)
    );

    // ===== 领域展开 =====
    public static final DeferredItem<DomainExpansionItem> DOMAIN_EXPANSION = ITEMS.registerItem(
            "domain_expansion",
            DomainExpansionItem::new,
            props -> props.stacksTo(1)
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

    // ===== 生物刷怪蛋 =====
    // 26.3 里 SpawnEggItem 不再自带"我对应哪个实体"的字段，
    // 信息全部落在 ENTITY_DATA 组件上，所以必须用 Properties#spawnEgg 来构造，
    // 否则物品放下去不知道要生成什么（getType 返回 null，右键直接 FAIL）。
    public static final DeferredItem<SpawnEggItem> RICK_SPAWN_EGG = ITEMS.registerItem(
            "rick_spawn_egg",
            SpawnEggItem::new,
            props -> props.spawnEgg(EntityRegistry.RICK.get())
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

    /** 附加剑类公共组件：武器行为（每次攻击损耗）、快速破坏蜘蛛网、可附魔 */
    private static Item.Properties swordComponents(Item.Properties props) {
        return props
                .component(DataComponents.WEAPON, new Weapon(1))
                .component(DataComponents.TOOL, new Tool(
                        List.of(Tool.Rule.minesAndDrops(HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), 15.0F)),
                        1.0F, 2, false))
                .enchantable(15);
    }

    /**
     * 万能挖掘工具的公共组件：武器行为 + 可附魔。
     * <p>
     * **故意不挂 {@code DataComponents.TOOL}**：TOOL 组件只能表达"一组 HolderSet 规则"，
     * 而"所有方块都能采"在注册期凑不出这样一个 HolderSet（拿不到 registry lookup 去展开 mineable 标签）。
     * 挖掘行为改由 {@code VacuumDecayItem} 覆写 {@code getDestroySpeed / isCorrectToolForDrops / mineBlock}
     * 三个入口实现——这三个方法本来就是"去读 TOOL 组件"的，覆写后和挂组件等价，且覆盖全部方块。
     * <p>
     * {@code Weapon(0)}：每次攻击消耗 0 点耐久——配上"不设 durability"，本工具完全不可损坏。
     */
    private static Item.Properties universalToolComponents(Item.Properties props) {
        return props
                .component(DataComponents.WEAPON, new Weapon(0))
                .enchantable(15);
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}