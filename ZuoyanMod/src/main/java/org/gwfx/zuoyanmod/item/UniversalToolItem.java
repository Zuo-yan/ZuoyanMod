package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * 「万能工具」——镐 · 斧 · 铲 · 锄 · 剑 五合一的前期工具，分木 / 石 / 金 / 铁 / 钻石 / 下界合金六档。
 *
 * <p><b>数值设计</b>（全部取自对应材质的 {@link Tier}）：
 * <ul>
 *   <li>挖掘速度、耐久、附魔能力、修复材料 —— 与同材质原版工具完全一致（由 {@link TieredItem} 兜底）；</li>
 *   <li>攻击力与攻速 —— 取五件中战斗数值最高的「斧」；</li>
 *   <li>采集门槛 —— 保留材质等级：木万能工具照样挖不了钻石矿。</li>
 * </ul>
 *
 * <h2>1.20.1 适配（与 26.3 的实现是两套东西，别照着抄）</h2>
 * 26.3 里挖掘/横扫/右键变形全部是<b>数据驱动</b>：一个 {@code DataComponents.TOOL} 组件塞四张
 * mineable 标签 + 剑规则，一个 {@code BLOCK_TRANSFORMER} 组件管右键。1.20.1 没有这套东西，
 * 于是按 1.20.1 的语义重写：
 * <ul>
 *   <li><b>挖掘</b>：继承 {@link DiggerItem}（它已经把 Tier 的耐久 / 附魔能力 / 修复材料 /
 *       攻击属性全部搞好），覆写 {@link #getDestroySpeed} 与 {@link #isCorrectToolForDrops}
 *       让它同时认 pickaxe / axe / shovel / hoe 四张 mineable 标签。</li>
 *   <li><b>右键变形</b>：26.3 用 {@code BlockTransformer} 注册表逐条尝试三件套；1.20.1 的做法是
 *       <b>委托给原版工具类实例</b>——依次把 {@code UseOnContext} 交给镐子里「能变形」的那三家
 *       （斧 / 锄 / 铲），谁先 {@code consumesAction} 就算谁。好处是自动跟随原版后续更新，
 *       也不用复制 {@code getToolModifiedState} 那一串分支。</li>
 *   <li><b>横扫之刃</b>：26.3 的横扫判定问 {@code DataComponents.WEAPON}；1.20.1 里问的是
 *       {@code EnchantmentHelper.getSweepingDamageRatio(player)}，只认玩家身上的横扫之刃等级，
 *       <b>不认物品类型</b>，所以「能上横扫之刃」就等于「能触发横扫」，这里不用补任何钩子。</li>
 *   <li><b>破盾</b>：26.3 是 {@code Weapon(AXE_DISABLES_BLOCKING_FOR_SECONDS)}；1.20.1 要覆写
 *       Forge 的 {@code canDisableShield}，见 {@link #canDisableShield}。</li>
 *   <li><b>附魔</b>：26.3 由 {@code #minecraft:enchantable/*} 八张标签决定。1.20.1 没有这套标签，
 *       附魔可上性由 {@link EnchantmentCategory} 说了算，而它们是<b>硬编码 instanceof</b>：
 *       {@code WEAPON} 要 {@code instanceof SwordItem}、{@code DIGGER} 要 {@code instanceof DiggerItem}。
 *       鱼与熊掌不可兼得 —— 好在 Forge 把「能不能上」做成了 {@link Item#canApplyAtEnchantingTable}
 *       钩子，这里覆写它对两类同时放行，效果等价于主线那八张标签。
 *       注意 {@code data/minecraft/tags/item/enchantable/*} 那些 JSON 在 1.20.1 也不需要。</li>
 * </ul>
 */
public class UniversalToolItem extends DiggerItem {

    /** 四张 mineable 标签：镐 / 斧 / 铲 / 锄能挖的一切都按材质速度挖。 */
    private static final List<TagKey<Block>> MINEABLE_TAGS = List.of(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.MINEABLE_WITH_AXE,
            BlockTags.MINEABLE_WITH_SHOVEL,
            BlockTags.MINEABLE_WITH_HOE
    );

    /**
     * 右键变形的委托对象：斧 → 锄 → 铲。
     *
     * <p>委托给真实的 vanilla 工具类实例，而不是复制它们的 useOn 逻辑：
     * 原版 {@code AxeItem} / {@code HoeItem} / {@code ShovelItem} 的 {@code useOn} 内部走的是
     * Forge 的 {@code ToolAction}（{@code getToolModifiedState}），行为会随 Forge 与原版更新而变化，
     * 复制过来就是一份很快会腐烂的拷贝。耐久照常扣在我们这个物品上（它们拿的是 context 里的
     * ItemStack），所以外观上也看不出是委托。
     */
    private static final List<Item> RIGHT_CLICK_DELEGATES = List.of(
            Items.NETHERITE_AXE,
            Items.NETHERITE_HOE,
            Items.NETHERITE_SHOVEL
    );

    /** 对外宣称支持的工具动作（供其它模组查询，与我们自己的挖掘逻辑无关）。 */
    private static final Set<ToolAction> SUPPORTED_ACTIONS = Set.of(
            ToolActions.AXE_STRIP,
            ToolActions.AXE_SCRAPE,
            ToolActions.AXE_WAX_OFF,
            ToolActions.HOE_TILL,
            ToolActions.SHOVEL_FLATTEN,
            ToolActions.SWORD_DIG,
            ToolActions.AXE_DIG,
            ToolActions.PICKAXE_DIG,
            ToolActions.SHOVEL_DIG,
            ToolActions.HOE_DIG
    );

    public UniversalToolItem(Tier tier, float axeDamageBaseline, float axeSpeedBaseline, Properties properties) {
        // DiggerItem 只需要 Tags 里的任意一张当作「默认主线」，真正的判定在下面两个覆写里
        super(axeDamageBaseline, axeSpeedBaseline, tier, BlockTags.MINEABLE_WITH_PICKAXE, properties);
    }

    /**
     * 构造万能工具用的 Properties。
     *
     * <p>耐久、修复材料、附魔能力全部由 {@link Tier} 经 {@link TieredItem} 自动生效，
     * 这里本来什么都不用做；保留这个工厂方法是为了与主线同名，
     * 把「下界合金要防火」之类的附加属性收在一处。攻击力与攻速由 {@link DiggerItem} 的构造器消费，
     * 两个基线值在注册那里传，不进 Properties。
     */
    public static Properties properties(Tier material) {
        return new Properties();
    }

    // ===== 挖掘：四张标签合一 =====

    /**
     * 挖掘速度。
     *
     * <p>26.3 版把「蛛网 15 倍速」写成一个 {@code Tool.Rule.minesAndDrops(cobweb, 15.0)}，
     * 这里同理手动补齐两档：{@code #mineable/*} 走材质速度、蛛网 15 倍速、{#sword_efficient} 1.5 倍速。
     * 注意 1.20.1 <b>没有</b> {@code #sword_instantly_mines} 这个标签，所以少了「瞬破」那一档 ——
     * 它是 1.20.4 之后才加的，替换不了就别装作有。
     */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        for (TagKey<Block> tag : MINEABLE_TAGS) {
            if (state.is(tag)) {
                return this.speed;
            }
        }
        if (state.is(Blocks.COBWEB)) {
            return 15.0F;
        }
        if (state.is(BlockTags.SWORD_EFFICIENT)) {
            return this.speed * 1.5F;
        }
        return 1.0F;
    }

    /**
     * 能否采到掉落物：材质门槛按原版规则，工具身份取四张标签的并集。
     *
     * <p>{@link DiggerItem} 的原版实现只认自己那一张 tag；这里改成四张任意一张命中即可，
     * 于是镐能挖的、斧能挖的、铲能挖的、锄能挖的，它都能「正确地挖」。
     */
    @Override
    public boolean isCorrectToolForDrops(BlockState state) {
        int level = this.getTier().getLevel();
        if (level < 3 && state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return false;
        }
        if (level < 2 && state.is(BlockTags.NEEDS_IRON_TOOL)) {
            return false;
        }
        if (level < 1 && state.is(BlockTags.NEEDS_STONE_TOOL)) {
            return false;
        }
        for (TagKey<Block> tag : MINEABLE_TAGS) {
            if (state.is(tag)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return this.isCorrectToolForDrops(state);
    }

    /** 攻击只扣 1 点耐久（同 26.3 的 {@code new Weapon(1, ...)}），而不是 DiggerItem 默认的 2 点。 */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, entity -> entity.broadcastBreakEvent(
                net.minecraft.world.entity.EquipmentSlot.MAINHAND));
        return true;
    }

    // ===== 右键：斧剥皮 / 锄耕地 / 铲铲路 三合一 =====

    @Override
    public InteractionResult useOn(UseOnContext context) {
        for (Item delegate : RIGHT_CLICK_DELEGATES) {
            InteractionResult result = delegate.useOn(context);
            if (result.consumesAction()) {
                return result;
            }
        }
        return super.useOn(context);
    }

    /** 对外宣称自己是斧 / 锄 / 铲 / 镐 / 剑五种工具（供其它模组查询）。 */
    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return SUPPORTED_ACTIONS.contains(toolAction) || super.canPerformAction(stack, toolAction);
    }

    /** 斧的破盾：26.3 是 Weapon 组件的属性，1.20.1 走 Forge 的 IForgeItem 钩子。 */
    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return true;
    }

    /**
     * 附魔表放行：锋利系（{@code WEAPON}）+ 挖掘系（{@code DIGGER}）+ 耐久/经验修补（{@code BREAKABLE}）。
     *
     * <p>见类注释：1.20.1 的 {@link EnchantmentCategory} 是硬编码 instanceof，一把工具不可能同时
     * 是 {@code SwordItem} 和 {@code DiggerItem}，只能在 Forge 的这个钩子上放行。
     */
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment.category == EnchantmentCategory.WEAPON
                || enchantment.category == EnchantmentCategory.DIGGER
                || enchantment.category == EnchantmentCategory.BREAKABLE
                || super.canApplyAtEnchantingTable(stack, enchantment);
    }

    // ===== 描述 =====

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.zuoyanmod.universal_tool.desc1"));
        tooltip.add(Component.translatable("item.zuoyanmod.universal_tool.desc2"));
    }
}
