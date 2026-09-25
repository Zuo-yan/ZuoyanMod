package org.gwfx.zuoyanmod.item;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.List;
import java.util.function.Consumer;

/**
 * 「万能工具」——镐 · 斧 · 铲 · 锄 · 剑 五合一的前期工具，分木质 / 石质 / 金 / 铁 / 钻石 / 下界合金六档。
 * <p>
 * <b>数值设计</b>（全部取自对应材质的 {@link ToolMaterial}）：
 * <ul>
 *   <li>挖掘速度、耐久、附魔能力、修复材料 —— 与同材质原版工具完全一致；</li>
 *   <li>攻击力与攻速 —— 取五件中战斗数值最高的「斧」；</li>
 *   <li>采集门槛 —— 保留材质等级：木万能工具照样挖不了钻石矿（{@code deniesDrops} 规则）。</li>
 * </ul>
 * <b>挖掘规则</b>：一个 TOOL 组件合并了镐 / 斧 / 铲 / 锄四张 mineable 标签（各按材质速度生效），
 * 再叠加剑的规则（蛛网 15 倍速、{@code SWORD_INSTANTLY_MINES} 瞬破、{@code SWORD_EFFICIENT} 1.5 倍速）。
 * 与 {@link VacuumDecayItem} 不同，本工具是"讲道理"的材质级工具，只加速该快挖的方块，
 * 挂组件而非覆写方法（标签通过 {@code acquireBootstrapRegistrationLookup} 在注册期即可解析）。
 * <p>
 * <b>右键行为</b>：26.x 里斧剥树皮 / 锄耕地 / 铲铲路都由 {@code DataComponents.BLOCK_TRANSFORMER}
 * （单一 {@code Holder<BlockTransformer>}）驱动，一个组件只能挂一个 transformer。
 * 这里覆写 {@link #useOn}，按 斧 → 锄 → 铲 的顺序依次尝试原版三个 transformer，
 * 谁先命中就执行谁——相当于三合一，且自动跟随原版后续更新。
 * <p>
 * <b>附魔</b>：物品**不进** {@code #axes / #pickaxes / #shovels / #hoes / #swords} 这五张"工具身份"标签 ——
 * 26.3 里它们只剩能力钩子的默认实现在读（{@code #swords} 只影响横扫判定，见 {@link #canPerformAction}），
 * 进不进对挖掘与附魔都没有影响。真正决定附魔可上性的是 {@code #minecraft:enchantable/} 下的标签，
 * 本工具注册进了其中的 mining / mining_loot / weapon / melee_weapon / sharp_weapon / sweeping /
 * fire_aspect / durability 八张，于是效率 / 时运 / 精准采集 / 锋利 / 抢夺 / 火焰附加 / 横扫之刃 /
 * 击退 / 耐久 / 经验修补等通用附魔全部可上，附魔能力用材质本身的值（木 15 / 石 5 / 金 22 / 铁 14 / 钻 10 / 合金 15）。
 */
public class UniversalToolItem extends Item {

    /** useOn 时依次尝试的原版方块变换：剥树皮 → 锄耕地/除根 → 铲铲路 */
    private static final List<ResourceKey<BlockTransformer>> RIGHT_CLICK_TRANSFORMERS = List.of(
            BlockTransformers.AXE,
            BlockTransformers.HOE,
            BlockTransformers.SHOVEL
    );

    public UniversalToolItem(Properties properties) {
        super(properties);
    }

    /**
     * 构造万能工具的完整 Properties：材质数值 + 四合一挖掘规则 + 剑规则 + 斧的战斗属性。
     *
     * @param material            材质（决定耐久/速度/附魔能力/修复材料/采集门槛）
     * @param axeDamageBaseline   该材质斧的伤害基线（原版数值：木/金 6，铁 6，石 7，钻/合金 5）
     * @param axeSpeedBaseline    该材质斧的攻速基线（原版数值：木/石 -3.2，铁 -3.1，金/钻/合金 -3.0）
     */
    public static Properties properties(ToolMaterial material, float axeDamageBaseline, float axeSpeedBaseline) {
        Properties props = new Properties()
                .durability(material.durability())
                .repairable(material.repairItems())
                .enchantable(material.enchantmentValue())
                // 剑的武器行为：每次攻击 1 点耐久 + 命中停盾 5 秒（斧特性）
                .component(DataComponents.WEAPON, new Weapon(1, Weapon.AXE_DISABLES_BLOCKING_FOR_SECONDS))
                .attributes(UniversalToolItem.toolAttributes(material, axeDamageBaseline, axeSpeedBaseline));

        HolderGetter<Block> lookup = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
        List<Tool.Rule> rules = new java.util.ArrayList<>();
        // 1) 材质等级门槛：木/石工具采不到高级矿的掉落物（与原版一致）
        rules.add(Tool.Rule.deniesDrops(lookup.getOrThrow(material.incorrectBlocksForDrops())));
        // 2) 四张 mineable 标签：按材质速度挖镐/斧/铲/锄能挖的一切方块并正常掉落
        for (var tag : List.of(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_AXE,
                BlockTags.MINEABLE_WITH_SHOVEL, BlockTags.MINEABLE_WITH_HOE)) {
            rules.add(Tool.Rule.minesAndDrops(lookup.getOrThrow(tag), material.speed()));
        }
        // 3) 剑规则：蛛网 15 倍速、SWORD_INSTANTLY_MINES 瞬破、SWORD_EFFICIENT 1.5 倍速
        rules.add(Tool.Rule.minesAndDrops(HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), 15.0F));
        rules.add(Tool.Rule.overrideSpeed(lookup.getOrThrow(BlockTags.SWORD_INSTANTLY_MINES), Float.MAX_VALUE));
        rules.add(Tool.Rule.overrideSpeed(lookup.getOrThrow(BlockTags.SWORD_EFFICIENT), 1.5F));

        return props.component(DataComponents.TOOL, new Tool(rules, 1.0F, 1, true));
    }

    private static ItemAttributeModifiers toolAttributes(
            ToolMaterial material, float damageBaseline, float speedBaseline) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damageBaseline + material.attackDamageBonus(),
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speedBaseline,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    // ===== 右键：斧剥皮 / 锄耕地 / 铲铲路 三合一 =====

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Registry<BlockTransformer> registry = level.registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER);
        for (ResourceKey<BlockTransformer> key : RIGHT_CLICK_TRANSFORMERS) {
            BlockTransformer transformer = registry.getValue(key);
            if (transformer != null) {
                InteractionResult result = transformer.transformBlock(context);
                if (result.consumesAction()) {
                    return result;
                }
            }
        }
        return super.useOn(context);
    }

    // ===== 能力钩子：补全「剑」的横扫与「铲」的扑灭营火 =====

    /**
     * 26.3 把"这是什么工具"彻底数据化了：判定能否横扫**不再看物品类型**，而是问
     * {@code ItemAbilities.SWORD_SWEEP}（见 {@code Player#isSweepAttack}）。
     * 这个钩子的最上层默认实现在 NeoForge 的 {@code IItemExtension} 里，内容就是
     * {@code stack.is(ItemTags.SWORDS)} —— 也就是说，剑类物品已被删除之后，
     * "是不是剑"等价于"在不在 {@code #minecraft:swords} 标签里"。
     * <p>
     * 本工具是五合一，不该对外宣称自己是剑（{@code #swords} 是给所有模组读的公开语义），
     * 所以选择覆写钩子、只对横扫放行。这本来也正是 NeoForge 把原版写死的
     * {@code ItemTags.SWORDS} 判断抽成钩子的用意。不覆写的话，物品能附上横扫之刃
     * （已挂 {@code #minecraft:enchantable/sweeping}）却永远不触发横扫，
     * 附魔给的那个 {@code sweeping_damage_ratio} 属性加成没有任何东西可以加成 —— 等于白附。
     * <p>
     * 顺便放行 {@code SHOVEL_DOUSE}：它同样只认 {@code #minecraft:douses_campfires} 标签，
     * 不挂就点不熄营火。三合一的"铲"那一路只有补上它才算真的完整。
     * 其余能力仍交回默认实现，以免以后原版加东西时这里被写死。
     */
    @Override
    public boolean canPerformAction(ItemInstance stack, ItemAbility itemAbility) {
        if (itemAbility == ItemAbilities.SWORD_SWEEP || itemAbility == ItemAbilities.SHOVEL_DOUSE) {
            return true;
        }
        return super.canPerformAction(stack, itemAbility);
    }

    // ===== 描述 =====

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.zuoyanmod.universal_tool.desc1"));
        tooltip.accept(Component.translatable("item.zuoyanmod.universal_tool.desc2"));
    }
}
