package org.gwfx.zuoyanmod.entity;

import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.gwfx.zuoyanmod.item.ItemRegistry;

/**
 * 瑞克的交易表 —— 全 mod 唯一一间"商店"。
 *
 * <p>目前三笔买卖（按界面上的顺序）：
 * <ol>
 *   <li><b>2 反物质微粒 + 1 下界合金锭 → 10 反物质子弹</b>（弹药，可以反复买）；</li>
 *   <li><b>3 原始黑洞 + 42 反物质微粒 → 1 因果律手枪</b>（武器本体）；</li>
 *   <li><b>1 超流体暗物质 + 8 金苹果 → 1 经验修补附魔书</b>。</li>
 * </ol>
 *
 * <h2>为什么有些东西走交易，而不是工作台配方</h2>
 * 判据在 {@code docs/item_acquisition.md}：<b>交易可以卖"量"，不能卖"档"</b>。
 * 落到这三笔上的实际情况是：
 * <ul>
 *   <li><b>反物质子弹</b>：命中注定要被大量消耗（每开一枪扣一发），做成工作台配方就是印钞机；
 *       交易给了它一个"可调的汇率"，而汇率集中在本文件里改。</li>
 *   <li><b>因果律手枪</b>：在这次改动之前它<b>和子弹一样没有任何获取途径</b>
 *       （配方目录与战利品表里都搜不到），所以这不是"用绿宝石买终局力量"，
 *       而是<b>补上它唯一的来源</b>。价格定得极贵（3 个原始黑洞＝3 次 400 tick 的对撞 + 1 个沉重核心/次），
 *       就是为了让"买"和"肝"等价，而不是绕开链条。</li>
 *   <li><b>经验修补附魔书</b>：原版来源是村民交易 / 钓鱼 / 战利品，属于典型的长周期 RNG。
 *       这里卖的是"确定性"，收的是本 mod 暗物质链的成品（超流体暗物质），
 *       相当于用自家产业链换掉原版那套磨人的随机。</li>
 * </ul>
 * 换言之：<b>它们全都是"补缺口"或"换掉 RNG"，没有一笔是让玩家跳过一个本来存在的合成链。</b>
 *
 * <h2>关于"无限次交易"的写法</h2>
 * 原版村民用 {@code MerchantOffer.uses} 累计到 {@code maxUses} 来判定"售罄"
 * （{@link MerchantOffer#isOutOfStock()}），一旦售罄
 * {@code MerchantContainer#updateSellItem()} 就不会再往产物槽放东西。这里要的是无限次，
 * 所以做了两件事，缺一不可：
 * <ol>
 *   <li>{@code maxUses} 顶到 {@code int} 上限（这里）；</li>
 *   <li>瑞克的 {@code notifyTrade} <b>不</b>调 {@code offer.increaseUses()}（见 {@link RickEntity}）。</li>
 * </ol>
 * 只做第 2 件事也能工作，但客户端那份 {@code ClientSideMerchant} 会走原版的
 * {@code notifyTrade → increaseUses()}，所以 maxUses 必须一起顶满，否则客户端会在
 * N 次交易后自己把交易项灰掉，与服务端的判断不一致。
 *
 * <p><b>价格槽只有两个</b>：{@link MerchantOffer} 的结构是"价格 A + 价格 B + 产物"，
 * 所以每笔交易最多收两种东西。想收三种就得拆成两笔交易或者自制容器，
 * 前者会让玩家多点一次，后者不值当。
 *
 * <p>价格是常量而非配置项：这是设计数值（和 mod 里其它终局物品的做法一致），
 * 改价格请直接改这里，改一处即可，界面会自动跟着变（价格槽读的就是这份报价）。
 *
 * <h2>1.20.1 适配</h2>
 * <ul>
 *   <li><b>价格槽是 {@link ItemStack} 不是 {@code ItemCost}</b>：{@code ItemCost} 是 1.20.5
 *       才引入的"带数量的价格"类型，1.20.1 的 {@code MerchantOffer} 直接用
 *       {@code ItemStack} 的 count 表示数量。</li>
 *   <li><b>附魔书不需要注册表</b>：26.3 的附魔是数据驱动注册表条目，编译期只有
 *       {@code ResourceKey}，所以 {@code createOffers} 必须收一个 {@code RegistryAccess}；
 *       1.20.1 的 {@code Enchantments.MENDING} 本身就是 {@link net.minecraft.world.item.enchantment.Enchantment}
 *       实例，{@link EnchantedBookItem#createForEnchantment} 直接就能造。
 *       为了少一处调用约定，这里的签名仍然收 {@code Level}（调用方是实体，手头就有），
 *       但已经不从中取任何东西 —— 留参数是为了和主线保持同一个形状，方便对照。</li>
 *   <li><b>没有 {@code Merchant#stillValid}</b>：那是 1.20.2+ 才进接口的，
 *       1.20.1 的 {@code MerchantMenu#stillValid} 只查 {@code getTradingPlayer() == player}。</li>
 * </ul>
 */
public final class RickTrades {

    // ===================== 交易 1：反物质子弹 =====================

    /** 每次兑换消耗的反物质微粒数量。 */
    public static final int BULLET_PARTICLE_COST = 2;

    /** 每次兑换消耗的下界合金锭数量。 */
    public static final int BULLET_NETHERITE_COST = 1;

    /** 每次兑换产出的反物质子弹数量。 */
    public static final int BULLET_YIELD = 10;

    // ===================== 交易 2：因果律手枪 =====================

    /** 购买因果律手枪需要的原始黑洞数量。 */
    public static final int PISTOL_BLACK_HOLE_COST = 3;

    /** 购买因果律手枪需要的反物质微粒数量。 */
    public static final int PISTOL_PARTICLE_COST = 42;

    // ===================== 交易 3：经验修补附魔书 =====================

    /** 购买经验修补附魔书需要的超流体暗物质（桶）数量。 */
    public static final int MENDING_DARK_MATTER_COST = 1;

    /** 购买经验修补附魔书需要的金苹果数量。 */
    public static final int MENDING_APPLE_COST = 8;

    /**
     * "无限次"：{@code uses} 永远追不上这个上限。
     * <p>不用 {@code Integer.MAX_VALUE} 之外的取巧写法（比如设成 0）——原版的
     * {@code isOutOfStock()} 是 {@code uses >= maxUses}，maxUses=0 会让它一出生就是售罄。
     */
    private static final int UNLIMITED_USES = Integer.MAX_VALUE;

    private RickTrades() {}

    /**
     * 构造瑞克的报价表。
     *
     * <p>不缓存、每次调用返回新实例：报价表只在服务端懒建一次（见 {@code RickEntity#getOffers}），
     * 而 {@code MerchantOffer} 内部有 {@code uses} 这类可变状态，共享单例容易在
     * "服务端菜单 / 客户端预测"两条路径之间串味。
     *
     * <p>{@code xp} 与 {@code priceMultiplier} 都给 0：瑞克不升级、不显示等级条，
     * 价格也不随供需浮动（{@code demand} 为 0 时改价公式恒等于标价）。
     */
    public static MerchantOffers createOffers() {
        MerchantOffers offers = new MerchantOffers();
        offers.add(bulletOffer());
        offers.add(pistolOffer());
        offers.add(mendingBookOffer());
        return offers;
    }

    /** 2 反物质微粒 + 1 下界合金锭 → 10 反物质子弹。 */
    private static MerchantOffer bulletOffer() {
        return new MerchantOffer(
                // A 价格：反物质微粒（对撞机产出）
                new ItemStack(ItemRegistry.ANTIMATTER_PARTICLE.get(), BULLET_PARTICLE_COST),
                // B 价格：下界合金锭（第二个价格槽；存在时玩家必须两个都放）
                new ItemStack(Items.NETHERITE_INGOT, BULLET_NETHERITE_COST),
                new ItemStack(ItemRegistry.ANTIMATTER_BULLET.get(), BULLET_YIELD),
                UNLIMITED_USES,
                0,
                0.0F
        );
    }

    /** 3 原始黑洞 + 42 反物质微粒 → 1 因果律手枪。 */
    private static MerchantOffer pistolOffer() {
        return new MerchantOffer(
                new ItemStack(ItemRegistry.PRIMORDIAL_BLACK_HOLE.get(), PISTOL_BLACK_HOLE_COST),
                new ItemStack(ItemRegistry.ANTIMATTER_PARTICLE.get(), PISTOL_PARTICLE_COST),
                new ItemStack(ItemRegistry.CAUSALITY_PISTOL.get(), 1),
                UNLIMITED_USES,
                0,
                0.0F
        );
    }

    /**
     * 1 超流体暗物质 + 8 金苹果 → 1 经验修补附魔书。
     *
     * <p>⚠️ 超流体暗物质是**桶装**物品（带 {@code craftRemainder(BUCKET)}），
     * 但交易消耗不走合成台的"返还容器"逻辑 —— 玩家会连铁桶一起交出去。
     * 这是刻意的取舍：为了"退还空桶"要自己接管成交结算，代价远大于一个铁桶。
     */
    private static MerchantOffer mendingBookOffer() {
        ItemStack book = EnchantedBookItem.createForEnchantment(
                new EnchantmentInstance(Enchantments.MENDING, 1));
        return new MerchantOffer(
                new ItemStack(ItemRegistry.DARK_MATTER_BUCKET.get(), MENDING_DARK_MATTER_COST),
                new ItemStack(Items.GOLDEN_APPLE, MENDING_APPLE_COST),
                book,
                UNLIMITED_USES,
                0,
                0.0F
        );
    }
}
