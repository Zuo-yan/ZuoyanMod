package org.gwfx.zuoyanmod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.List;

/**
 * 「万能工具」——镐 · 斧 · 铲 · 锄 · 剑 五合一的前期工具，分木质 / 石质 / 铜 / 金 / 铁 / 钻石 / 下界合金七档。
 * <p>
 * <b>数值设计</b>（全部取自对应材质的 {@link Tier}）：
 * <ul>
 *   <li>挖掘速度、耐久、附魔能力、修复材料 —— 与同材质原版工具完全一致（耐久 / 附魔能力 / 修复
 *       由 {@link TieredItem} 基类直接从 Tier 读取）；</li>
 *   <li>攻击力与攻速 —— 取五件中战斗数值最高的「斧」；</li>
 *   <li>采集门槛 —— 保留材质等级：木万能工具照样挖不了钻石矿（{@code deniesDrops} 规则）。</li>
 * </ul>
 * <b>挖掘规则</b>：一个 TOOL 组件合并了镐 / 斧 / 铲 / 锄四张 mineable 标签（各按材质速度生效），
 * 再叠加剑的规则（蛛网 15 倍速、{@code #minecraft:sword_efficient} 1.5 倍速——
 * 与原版 {@code SwordItem#createToolProperties} 同一套规则）。
 * <p>
 * <b>右键行为</b>：1.21.1 里斧剥皮 / 锄耕地 / 铲铲路都收口在原版工具类的静态方法 +
 * NeoForge 的 {@code getToolModifiedState} 钩子上（会走 {@code BlockToolModificationEvent}、
 * 数据映射与原版方块表，自动跟随原版内容更新）。
 * 这里覆写 {@link #useOn}，按 斧（剥皮 → 刮铜 → 除蜡）→ 锄（耕地 / 除根）→ 铲（铲路 → 灭营火）
 * 的原版顺序依次尝试，谁先命中就执行谁——相当于三合一。
 * <p>
 * <b>附魔</b>：物品**不进** {@code #axes / #pickaxes / #shovels / #hoes / #swords} 这五张"工具身份"标签 ——
 * 1.21.1 里它们只剩 {@code ItemAbilities.DEFAULT_*_ACTIONS} 钩子的默认实现在读（见 {@link #canPerformAction}），
 * 进不进对挖掘与附魔都没有影响。附魔可上性由资源包的 {@code #minecraft:enchantable/} 标签驱动，
 * 本工具注册进了其中的 mining / mining_loot / weapon / melee_weapon / sharp_weapon / sweeping /
 * fire_aspect / durability 八张，于是效率 / 时运 / 精准采集 / 锋利 / 抢夺 / 火焰附加 / 横扫之刃 /
 * 击退 / 耐久 / 经验修补等通用附魔全部可上，附魔能力用材质本身的值（木 15 / 石 5 / 铜 13 / 金 22 / 铁 14 / 钻 10 / 合金 15）。
 */
public class UniversalToolItem extends TieredItem {

    public UniversalToolItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    /**
     * 构造万能工具的完整 Properties：四合一挖掘规则 + 剑规则 + 斧的战斗属性。
     * 耐久 / 附魔能力 / 修复材料由 {@link TieredItem} 从 Tier 读取，这里不重复设。
     *
     * @param tier                材质（决定耐久/速度/附魔能力/修复材料/采集门槛）
     * @param axeDamageBaseline   该材质斧的伤害基线（原版数值：木/金 6，铁 6，石 7，钻/合金 5）
     * @param axeSpeedBaseline    该材质斧的攻速基线（原版数值：木/石 -3.2，铁 -3.1，金/钻/合金 -3.0）
     */
    public static Properties properties(Tier tier, float axeDamageBaseline, float axeSpeedBaseline) {
        return new Properties()
                // 武器行为：每次攻击 1 点耐久是 1.21.1 的默认（ItemStack 在命中时调 hurtAndBreak），
                // "命中停盾"在 1.21.1 只认 {@code #minecraft:axes} 标签，本工具不进那张标签，故无停盾。
                .attributes(UniversalToolItem.toolAttributes(tier, axeDamageBaseline, axeSpeedBaseline))
                .component(net.minecraft.core.component.DataComponents.TOOL, toolRules(tier));
    }

    /** 镐/斧/铲/锄四张 mineable 标签按材质速度 + 剑的蛛网/高效规则（同原版剑的 TOOL 组件） */
    private static Tool toolRules(Tier tier) {
        List<Tool.Rule> rules = new java.util.ArrayList<>();
        // 1) 材质等级门槛：木/石工具采不到高级矿的掉落物（与原版一致）
        rules.add(Tool.Rule.deniesDrops(tier.getIncorrectBlocksForDrops()));
        // 2) 四张 mineable 标签：按材质速度挖镐/斧/铲/锄能挖的一切方块并正常掉落
        for (var tag : List.of(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_AXE,
                BlockTags.MINEABLE_WITH_SHOVEL, BlockTags.MINEABLE_WITH_HOE)) {
            rules.add(Tool.Rule.minesAndDrops(tag, tier.getSpeed()));
        }
        // 3) 剑规则：蛛网 15 倍速、sword_efficient 方块 1.5 倍速（与原版 SwordItem 一致）
        rules.add(Tool.Rule.minesAndDrops(List.of(Blocks.COBWEB), 15.0F));
        rules.add(Tool.Rule.overrideSpeed(BlockTags.SWORD_EFFICIENT, 1.5F));

        return new Tool(rules, 1.0F, 1);
    }

    private static ItemAttributeModifiers toolAttributes(Tier tier, float damageBaseline, float speedBaseline) {
        return ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                        new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                Item.BASE_ATTACK_DAMAGE_ID, damageBaseline + tier.getAttackDamageBonus(),
                                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                        net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                        new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                Item.BASE_ATTACK_SPEED_ID, speedBaseline,
                                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                        net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build();
    }

    // ===== 右键：斧剥皮 / 锄耕地 / 铲铲路 三合一 =====

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState state = level.getBlockState(pos);
        if (context.getClickedFace() == net.minecraft.core.Direction.DOWN) {
            return super.useOn(context);
        }

        // 斧：剥皮 → 刮铜 → 除蜡（顺序同原版 AxeItem）
        BlockState modified = state.getToolModifiedState(context, ItemAbilities.AXE_STRIP, false);
        if (modified != null) {
            level.playSound(player, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            modified = state.getToolModifiedState(context, ItemAbilities.AXE_SCRAPE, false);
            if (modified != null) {
                level.playSound(player, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.levelEvent(player, 3005, pos, 0);
            } else {
                modified = state.getToolModifiedState(context, ItemAbilities.AXE_WAX_OFF, false);
                if (modified != null) {
                    level.playSound(player, pos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.levelEvent(player, 3004, pos, 0);
                }
            }
        }
        if (modified != null) {
            return finishTransformation(context, modified);
        }

        // 锄：耕地 / 除根（getToolModifiedState 里含"上方必须是空气"的原版判定；除根掉垂根）
        modified = state.getToolModifiedState(context, ItemAbilities.HOE_TILL, false);
        if (modified != null) {
            level.playSound(player, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            return finishTransformation(context, modified);
        }

        // 铲：铲路（上方必须是空气）→ 扑灭营火（getToolModifiedState 里会先 douse 出烟）
        BlockState flattened = state.getToolModifiedState(context, ItemAbilities.SHOVEL_FLATTEN, false);
        if (flattened != null && level.getBlockState(pos.above()).isAir()) {
            level.playSound(player, pos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0F, 1.0F);
            return finishTransformation(context, flattened);
        }
        modified = state.getToolModifiedState(context, ItemAbilities.SHOVEL_DOUSE, false);
        if (modified != null) {
            if (!level.isClientSide()) {
                level.levelEvent(null, 1009, pos, 0);
            }
            return finishTransformation(context, modified);
        }

        return super.useOn(context);
    }

    /** 落方块 + 记进度 + 扣 1 点耐久（原版三个工具类 useOn 的收尾都是这一套） */
    private static InteractionResult finishTransformation(UseOnContext context, BlockState newState) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (!level.isClientSide) {
            level.setBlock(pos, newState, Block.UPDATE_ALL_IMMEDIATE);
            level.gameEvent(net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE, pos,
                    net.minecraft.world.level.gameevent.GameEvent.Context.of(player, newState));
            ItemStack stack = context.getItemInHand();
            if (player != null) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ===== 能力钩子：补全「剑」的横扫与「铲」的扑灭营火 =====

    /**
     * 1.21.1 里"这是什么工具"由 {@link ItemAbility} 钩子表达：判定能否横扫**不看物品类型**，
     * 而是问 {@code ItemAbilities.SWORD_SWEEP}（见 {@code Player#isSweepAttack}）。
     * {@code Item} 的默认实现一律返回 false，原版五类工具各自声明自己的 {@code DEFAULT_*_ACTIONS}。
     * <p>
     * 本工具是五合一，不该对外宣称自己是剑（{@code #swords} 是给所有模组读的公开语义），
     * 所以选择覆写钩子、把五类工具的能力集取并集全部放行——这本来也正是 NeoForge 把原版
     * 写死的判断抽成钩子的用意。不覆写的话，物品能附上横扫之刃（已挂
     * {@code #minecraft:enchantable/sweeping}）却永远不触发横扫，附魔白附；
     * 右键的剥皮 / 耕地 / 铲路也会因为 {@code getToolModifiedState} 第一道
     * {@code canPerformAction} 门检查直接短路。
     */
    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(itemAbility)
                || ItemAbilities.DEFAULT_AXE_ACTIONS.contains(itemAbility)
                || ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(itemAbility)
                || ItemAbilities.DEFAULT_HOE_ACTIONS.contains(itemAbility)
                || ItemAbilities.DEFAULT_SWORD_ACTIONS.contains(itemAbility);
    }

    // ===== 描述 =====

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.zuoyanmod.universal_tool.desc1"));
        tooltip.add(Component.translatable("item.zuoyanmod.universal_tool.desc2"));
    }
}
