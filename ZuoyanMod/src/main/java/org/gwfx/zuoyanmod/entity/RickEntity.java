package org.gwfx.zuoyanmod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 瑞克 —— 中立人形生物。
 *
 * <p><b>行为</b>：平时不主动攻击任何生物，被谁打了就追着谁打；目标死亡或跑远后自动恢复中立。
 * 这条"被打才还手"全靠原版两个目标选择器，没有一行自定义索敌逻辑：
 * <ul>
 *   <li>{@link HurtByTargetGoal} —— 谁打我，我打谁；</li>
 *   <li>{@link NearestAttackableTargetGoal} —— 只在 {@link NeutralMob#isAngryAt} 为真时才认玩家作目标，
 *       所以不生气的时候它不会主动找玩家麻烦。</li>
 * </ul>
 *
 * <p><b>攻击力 42</b>：写在 {@code ATTACK_DAMAGE} 属性里（见 {@link EntityRegistry}），
 * 由 {@link MeleeAttackGoal} 触发的普通近战结算，不需要手写任何伤害注入。
 *
 * <p><b>无限复活</b>：死亡时在倒下的位置生成一个满血的自己。为避免"每个副本都再复制一份"
 * 造成指数增殖，每个瑞克身上带一个 {@code canRebirth} 标记，<b>且该标记不写入存档</b>：
 * <ul>
 *   <li>刷怪蛋/指令生成 → {@link #finalizeSpawn} 标记为 true（第一代，会复活）；</li>
 *   <li>复活产生的下一代 → 只复制位置和仇恨，<b>不</b>给标记（第二代，死透了）；</li>
 *   <li>从磁盘读回来 → 拿不到标记（一律死透，复活链长度恒为 1）。</li>
 * </ul>
 * 这样"无限复活"对玩家而言是永动机（第一代永在），但对实体总数是受控的。
 *
 * <p><b>交易</b>：右键任意瑞克可以跟它做买卖 —— 2 反物质微粒 + 1 下界合金锭 → 10 反物质子弹。
 * 实现方式是让它直接 {@link Merchant 实现原版商人接口}，于是白嫖到原版村民交易界面
 * （价格槽、材料不足自动灰化、成交音效全部现成），没有一行自定义 GUI 或网络包代码。
 * 报价表在 {@link RickTrades}，交易次数无限。见 {@link #mobInteract}。
 */
public class RickEntity extends PathfinderMob implements NeutralMob, Merchant {

    /** 复活后的瑞克继承的仇恨时间：够它跑到凶手面前再打一架。 */
    private static final int REBIRTH_ANGER_TICKS = 200;

    /** 中立仇恨默认持续时长（狼是 400~1200 随机，这里取固定值，行为可预期）。 */
    private static final int PERSISTENT_ANGER_TICKS = 400;

    /** 复活时的粒子数量，纯装饰。 */
    private static final int REBIRTH_PARTICLE_COUNT = 30;

    /**
     * 小屋守卫的领地半径（格）。
     * <p>按结构主厅的尺度取的：从中心到四面内墙大约 4~9 格，半径 7 能让它在厅里正常走动，
     * 又不会把墙角、门廊圈进领地。房屋本身是有墙的，所以这个圆只是"活动意愿边界"，
     * 无需与墙严格对齐——真正挡住它的是墙，这个半径负责的是别让它"想"出去。
     */
    private static final int STRUCTURE_HOME_RADIUS = 7;

    /**
     * 交易时允许的最大距离（格）。
     * <p>与村民一致取 4：走远了界面自动关掉，避免"人跑了交易还挂着"。
     */
    private static final double TRADE_DISTANCE = 4.0D;

    /**
     * 守卫标签：打上它的瑞克会把出生点认作领地。
     * <p>结构 NBT 里给小屋那只预置了这个标签（见 {@code tools/rick_structure_nbt.py}）。
     * <p>为什么不用 {@code EntitySpawnReason.STRUCTURE} 判断：只有走区块生成的
     * {@code SinglePoolElement} 会设 {@code finalizeEntities=true}；{@code /place structure}
     * 这条路径走的是裸的 {@code StructureTemplate.placeInWorld}，<b>不会</b>调 finalizeSpawn。
     * 用标签的话，"结构生成 / {@code /place} / {@code /summon} 带 NBT"三种途径行为一致，
     * 调试起来也方便（{@code /summon} 一只带标签的就能复现守卫行为）。
     */
    public static final String GUARD_TAG = "zuoyanmod.rick_guard";

    /**
     * 是否还保有"复活资格"。
     * <p>
     * 刻意用普通字段而非 SynchedEntityData：这个标记只在服务端被读，
     * 而 {@code dropAllDeathLoot} 里读同步数据是不可靠的（实体进入死亡状态后
     * 同步数据的可读性受死状态影响），普通字段没有这个隐患。
     */
    private boolean canRebirth;

    private long persistentAngerEndTime = NO_ANGER_END_TIME;
    private @Nullable EntityReference<LivingEntity> persistentAngerTarget;

    /**
     * 当前正在跟它做买卖的玩家，null 表示没人。
     * <p>刻意<b>不</b>存盘：交易是纯会话状态，读档后重新右键即可；
     * 存了反而会在"玩家退游戏时界面没关干净"的情况下留下一个永远打不开的幽灵交易锁。
     */
    private @Nullable Player tradingPlayer;

    /**
     * 报价表，第一次有人开界面时才构造（见 {@link #getOffers}）。
     * <p>同样不存盘：价格固定、交易无限次，实体重新加载后重建一份结果完全一样，
     * 没有任何需要跨存档保留的状态（这也是我们不需要覆写 addAdditionalSaveData 的原因）。
     */
    private @Nullable MerchantOffers offers;

    public RickEntity(EntityType<? extends RickEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    /**
     * 属性基值。集中放在实体类里、注册表那边委托过来，数值只在一处出现。
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                // 生命 20：与玩家同血量，强度靠无限复活而非血厚来体现
                .add(Attributes.MAX_HEALTH, 20.0D)
                // 单次平A 42 点：招牌数值，走属性而不是硬编码伤害
                .add(Attributes.ATTACK_DAMAGE, 42.0D)
                // 略快于僵尸（0.23）：追得上普通玩家，但跑不过疾跑
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 25.0D)
                // 不给护甲/韧性/击退抗性：20 点血就该是实打实的 20 点
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        // 6：被挤出领地（击退、被活塞推、卡在墙里）时主动走回出生点。
        //    没有领地的瑞克（刷怪蛋放出来的）这一条永远不触发，行为与之前完全一致。
        this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 1.0D));
        // 7：漫步只在领地内选点，见 HomeBoundedStrollGoal
        this.goalSelector.addGoal(7, new HomeBoundedStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        // 1：谁打我，我就打谁 —— "被打才还手"的主入口
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // 2：把被激怒的玩家重新认回来（复活后、或仇恨目标一度丢失时用）。
        //    mustSee=true  → 看不见就不锁定，避免隔墙索敌
        //    mustReach=false → 允许隔着一段距离先记住目标
        //    末尾的过滤器要求"生气 且 在领地内"：小屋守卫不会把屋外的玩家列进名单，
        //    从源头上避免它为了追人而贴到墙上磨蹭。
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, Player.class, 10, true, false,
                (target, serverLevel) -> this.isAngryAt(target, serverLevel) && this.canReachTarget(target)));
    }

    /** 有领地时只承认领地内的目标；没有领地（刷怪蛋生成）则一律照旧。 */
    private boolean canReachTarget(LivingEntity entity) {
        return !this.hasHome() || this.isWithinHome(entity.blockPosition());
    }

    // ===================== 中立生物（NeutralMob）=====================

    @Override
    public long getPersistentAngerEndTime() {
        return this.persistentAngerEndTime;
    }

    @Override
    public void setPersistentAngerEndTime(long endTime) {
        this.persistentAngerEndTime = endTime;
    }

    @Override
    public @Nullable EntityReference<LivingEntity> getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> target) {
        this.persistentAngerTarget = target;
    }

    @Override
    public void startPersistentAngerTimer() {
        this.setTimeToRemainAngry(PERSISTENT_ANGER_TICKS);
    }

    /** 仇恨计时结束、目标死亡、或目标切创造/旁观时调用。 */
    @Override
    public void stopBeingAngry() {
        NeutralMob.super.stopBeingAngry();
        this.persistentAngerEndTime = NO_ANGER_END_TIME;
        this.persistentAngerTarget = null;
    }

    // ===================== 复活 =====================

    public boolean canRebirth() {
        return this.canRebirth;
    }

    public void setCanRebirth(boolean canRebirth) {
        this.canRebirth = canRebirth;
    }

    /**
     * 死亡即复活。
     *
     * <p>拦在 {@code dropAllDeathLoot} 而不是 {@code die}：这里正好是
     * "清点掉落 + 掉落经验"的唯一入口，直接覆写成空实现（不调 super），
     * 就不用再去跟 {@code shouldDropLoot} / {@code xpReward} 的默认值较劲了。
     * 同时出生点也确认过了——死亡时它会朝凶手的方向站着，复活体继承一致的朝向。
     */
    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        if (this.canRebirth) {
            this.rebirth(level, source);
        }
        // 有意不调用 super：瑞克不掉落任何物品，也不给经验。
    }

    private void rebirth(ServerLevel level, DamageSource source) {
        EntityType<RickEntity> type = EntityRegistry.RICK.get();
        // create(Level, reason) 只构造实体，不会调 finalizeSpawn —— 正好，下面手动安排。
        RickEntity next = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (next == null) {
            return;
        }

        // 死亡位置原样继承，朝向也一致：视觉上就是"原地站起来了一个新的"
        next.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        // 有意不给 next 设 canRebirth：第二代死透，复活链长度恒为 1。
        next.finalizeSpawn(level, level.getCurrentDifficultyAt(this.blockPosition()),
                EntitySpawnReason.MOB_SUMMONED, null);
        // finalizeSpawn 会重掷血量相关随机，这里明确拉满 → 满血复活
        next.setHealth(next.getMaxHealth());

        // 领地跟着一起继承：小屋里那只被打倒后，新站起来的这只仍然守在同一块地方。
        // 必须显式拷贝——next 是全新构造的实体，既没走存档读回、生成原因也不是 STRUCTURE，
        // 上面 finalizeSpawn 里设家那一段不会替它触发。
        if (this.hasHome()) {
            next.setHomeTo(this.getHomePosition(), this.getHomeRadius());
        }

        // 记住凶手：谁打死上一个，新瑞克接着跟他算账。
        // 顺序很重要——先 setLastHurtByMob，因为下面的持久仇恨目标就是取它。
        LivingEntity attacker = this.resolveAttacker(source);
        if (attacker != null) {
            next.setLastHurtByMob(attacker);
            next.setPersistentAngerTarget(EntityReference.of(attacker));
            next.setPersistentAngerEndTime(level.getGameTime() + REBIRTH_ANGER_TICKS);
            next.setTarget(attacker);
        }

        level.addFreshEntity(next);
        level.gameEvent(next, GameEvent.ENTITY_PLACE, next.blockPosition());
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                this.getX(), this.getY() + 1.0D, this.getZ(),
                REBIRTH_PARTICLE_COUNT, 0.35D, 0.6D, 0.35D, 0.02D);
    }

    /** 找出"算在谁头上"：优先伤害直接来源，其次是凶手，最后是最后打我的人。 */
    private @Nullable LivingEntity resolveAttacker(DamageSource source) {
        if (source.getEntity() instanceof LivingEntity direct) {
            return direct;
        }
        LivingEntity killer = this.getKillCredit();
        if (killer != null) {
            return killer;
        }
        return this.getLastHurtByMob();
    }

    /**
     * 一切正常生成路径（刷怪蛋、{@code /summon}、自然生成）都会走到这里，
     * 在这里发"复活资格"，正好覆盖"第一代"的语义。
     * 复活产生的下一代是手工 {@code create + snapTo} 的，不会经过 finalizeSpawn，因此拿不到资格。
     */
    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            net.minecraft.world.entity.@Nullable SpawnGroupData groupData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        this.canRebirth = true;
        // 无论哪条生成路径，瑞克出场都该是完整的 20 点血
        this.setHealth(this.getMaxHealth());
        return result;
    }

    // ===================== 领地（小屋守卫）=====================

    /**
     * 补上 {@link HurtByTargetGoal} 那条路径的漏洞。
     * <p>{@code HurtByTargetGoal} 不挑地方，谁打它就记谁；而
     * {@link MeleeAttackGoal#canContinueToUse()} 一看到目标在领地外就放弃追击。
     * 两者叠在一起的结果是：玩家站在屋外打它，它会每 20 tick 朝门外冲一下再停住，来回抽搐。
     * 这里在目标跑出领地时直接放手——仇恨计时不受影响，玩家一进屋它照样立刻扑上来。
     * <p>{@link NearestAttackableTargetGoal} 那条路径不用管，过滤器里已经挡掉了。
     */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            return;
        }
        this.claimGuardHome();
        if (!this.hasHome()) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target != null && !this.isWithinHome(target.blockPosition())) {
            this.setTarget(null);
        }
    }

    /**
     * 带着 {@link #GUARD_TAG} 出生的瑞克，在第一次服务端 tick 认领领地。
     * <p>为什么拖到 tick 而不是在读 NBT 时设：结构 NBT 里只能写相对坐标，实体被真正
     * 放到目标格上要等 {@code StructureTemplate} 完成 {@code snapTo}。等到第一 tick，
     * 位置已经定下来了，这时取 {@code blockPosition()} 才是我们想要的那一格。
     * <p>认领后 {@code home_pos}/{@code home_radius} 由 {@code Mob} 自己写进存档，
     * 所以只需要认一次；不再带标签的、或者没有标签的瑞克完全不受影响。
     */
    private void claimGuardHome() {
        // 注意 26.3 的 getter 叫 entityTags()，不是 getTags()；存盘键名仍是 "Tags"
        if (this.hasHome() || !this.entityTags().contains(GUARD_TAG)) {
            return;
        }
        this.setHomeTo(this.blockPosition(), STRUCTURE_HOME_RADIUS);
    }

    /**
     * 只在领地范围内挑落脚点的随机漫步。
     * <p>原版漫步完全无视 home —— {@code LandRandomPos.getPos(mob, 10, 7)} 是纯随机方向，
     * 会把屋里的瑞克一路遛到圈外。这里反复挑几次，只接受落在领地内的候选点。
     * 没有领地的瑞克直接沿用原版行为。
     */
    private static final class HomeBoundedStrollGoal extends WaterAvoidingRandomStrollGoal {

        /** 连挑这么多次都没落在圈内，就当这一轮不散步了。 */
        private static final int MAX_ATTEMPTS = 8;

        HomeBoundedStrollGoal(PathfinderMob mob, double speedModifier) {
            super(mob, speedModifier);
        }

        @Override
        protected @Nullable Vec3 getPosition() {
            if (!this.mob.hasHome()) {
                return super.getPosition();
            }
            for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                Vec3 candidate = super.getPosition();
                if (candidate == null) {
                    return null;
                }
                if (this.mob.isWithinHome(candidate)) {
                    return candidate;
                }
            }
            return null;
        }
    }

    // ===================== 交易（Merchant）=====================

    /**
     * 右键开商店。
     *
     * <p>三种情况不交易：
     * <ul>
     *   <li>实体已经死了 / 别人正在跟它交易 —— 交给 super，什么都不做；</li>
     *   <li>玩家按着潜行键右键 —— 沿用原版村民的约定，把"潜行交互"留给以后可能加的功能；</li>
     *   <li><b>它正被激怒</b>（{@link NeutralMob#isAngry()}）—— 正在气头上不做买卖，
     *       不然会出现"玩家一边被它追着打、一边悠闲地翻它的货架"这种荒唐画面。</li>
     * </ul>
     *
     * <p>真正的开门动作是 {@link #startTrading}：它也是接口默认方法
     * {@link Merchant#openTradingScreen} 的唯一调用点，内部自己开 {@code MerchantMenu}
     * 并把报价表推给客户端，所以我们一行网络包都不用写。
     * 界面标题直接用它自己的名字，玩家给瑞克改了名牌的话标题会跟着变。
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isAlive() || this.isTrading() || player.isSecondaryUseActive()) {
            return super.mobInteract(player, hand);
        }

        if (this.isAngry()) {
            if (!this.level().isClientSide()) {
                player.sendSystemMessage(Component.translatable("message.zuoyanmod.rick.not_in_mood"));
            }
            return InteractionResult.SUCCESS;
        }

        if (!this.level().isClientSide()) {
            this.startTrading(player);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * 真正开界面：<b>先登记交易对象，再开界面</b>。顺序不能反。
     *
     * <p>原版 {@link Merchant#openTradingScreen} 只管开菜单，<b>不会</b>帮你调
     * {@code setTradingPlayer}。而 {@link #stillValid} 与
     * {@code MerchantContainer#stillValid} 都要求
     * {@code getTradingPlayer() == player}，容器每 tick 都会查一次 ——
     * 少了这一句的话，界面会在开出后的第一个 tick 就被判为失效并自动关掉，
     * 表现就是"界面闪一下没了"。村民那边的对应实现在
     * {@code Villager#startTrading}，同样是先 {@code setTradingPlayer} 再开。
     */
    private void startTrading(Player player) {
        this.setTradingPlayer(player);
        // level 传 1：不显示村民那条等级进度条（showProgressBar() 也是 false），
        // 只是为了让原版界面有一个合法的"商人等级"可读，避免出现等级 0 的空标题。
        this.openTradingScreen(player, this.getName(), 1);
    }

    /** 是否已有人在跟它交易（原版村民用它避免两个人同时开同一个界面）。 */
    public boolean isTrading() {
        return this.tradingPlayer != null;
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    @Override
    public @Nullable Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    /**
     * 报价表，懒建一次然后复用。
     *
     * <p>把 {@code registryAccess()} 递下去是必须的：报价里有一笔是**经验修补附魔书**，
     * 而附魔是数据驱动的注册表条目，编译期只有 {@code ResourceKey}，
     * 得到运行时才能解析成 {@code Holder<Enchantment>}（见 {@link RickTrades}）。
     *
     * <p>与 {@code AbstractVillager#getOffers} 的差异：村民的报价要用服务端注册表里的
     * TradeSet 现算，所以它在客户端调用会直接抛异常；我们的报价是写死的数据 + 一次附魔查表，
     * 两端构造都安全，因此不做端侧限制。实际上客户端也走不到这条路——
     * 那边的 {@code MerchantMenu} 持有的是原版 {@code ClientSideMerchant}，不是这只瑞克。
     */
    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            this.offers = RickTrades.createOffers(this.registryAccess());
        }
        return this.offers;
    }

    /** 客户端同步报价时会用到；对我们这只实体实际上不会被调用（见 {@link #getOffers}）。 */
    @Override
    public void overrideOffers(MerchantOffers offers) {
        this.offers = offers;
    }

    /**
     * 成交回调。
     *
     * <p><b>故意不调 {@code offer.increaseUses()}</b>：原版村民靠 uses 累加到 maxUses 来"售罄"，
     * 我们这里是无限次交易，所以既不累加、也不改供需（demand 恒为 0，价格恒定）。
     *
     * <p>唯一做的事是重置环境音计时。<b>注意这不是在放音效</b> —— 瑞克全身上下已经被静音了
     * （见「音效 / 杂项」一节），重置计时器的意义是让 {@link #notifyTradeUpdated} 那套
     * 限流逻辑保持和原版一致的节奏，将来若要给它加回声音，这里不用再改。
     *
     * <p>注意客户端那份 {@code ClientSideMerchant} 会老老实实按原版调 increaseUses，
     * 靠 {@link RickTrades} 里把 maxUses 顶到 int 上限来保证客户端也永远不会灰化交易项。
     */
    @Override
    public void notifyTrade(MerchantOffer offer) {
        this.ambientSoundTime = -this.getAmbientSoundInterval();
    }

    /**
     * 材料槽内容变化时的即时反馈。
     *
     * <p>原版村民在这里按"放够了 / 放不够"分别播 yes / no 两条音效，<b>这里是空实现</b> ——
     * 应要求把瑞克发出的声音全部去掉了。保留这个方法（而不是删掉）是因为
     * {@link Merchant} 把 {@code notifyTradeUpdated} 定为抽象方法，必须实现；
     * 而且 {@code MerchantContainer#updateSellItem} 每次改动材料槽都会调它，
     * 将来若要加回音效，逻辑（含 20 tick 限流）照抄 {@code AbstractVillager} 即可。
     */
    @Override
    public void notifyTradeUpdated(ItemStack stack) {
        // 有意为空：瑞克不发出任何声音。
    }

    /** 不给交易经验：瑞克不升级，也没有等级体系。 */
    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
        // 有意为空：原版 MerchantResultSlot 每次成交都会回调这里，我们全部忽略。
    }

    /** 不显示村民界面上那条等级进度条 —— 瑞克没有等级。 */
    @Override
    public boolean showProgressBar() {
        return false;
    }

    /**
     * 交易成交音。
     *
     * <p>接口规定必须返回一个 {@link SoundEvent}，不接受 null
     * （{@code MerchantMenu#playTradeSound} 里直接把它喂给
     * {@code Level#playLocalSound}，那个方法参数非空，返回 null 会 NPE）。
     * 所以返回原版的 {@link SoundEvents#EMPTY}：它是注册表里一个
     * <b>故意不带任何音频文件的占位音效</b>（id 为 {@code minecraft:intentionally_empty}），
     * 播放它等于什么都不播 —— 这正是"瑞克不出声"想要的效果。
     */
    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.EMPTY;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide();
    }

    /**
     * 界面是否还该开着：必须是同一个玩家、瑞克还活着、且人没走远。
     * <p>三个条件都会被容器每 tick 检查，任一不成立原版就自动关界面并把价格槽里的
     * 材料退回背包（见 {@code MerchantMenu#removed}）。
     */
    @Override
    public boolean stillValid(Player player) {
        return this.getTradingPlayer() == player
                && this.isAlive()
                && player.isWithinEntityInteractionRange(this, TRADE_DISTANCE);
    }

    // ===================== 音效 / 杂项 =====================
    //
    // 瑞克被设定为「不发声」的生物：三个语音音效（环境音/受伤/死亡）全部返回 null。
    // 为什么返回 null 而不是 getSoundVolume() = 0：
    //   ① 原版这三处调用点都做了 null 判断（见 LivingEntity#makeSound、
    //      #handleDamageEvent、#handleEntityEvent），null 是官方支持的"这个生物没这条音效"的写法，
    //      像盔甲架这类哑巴实体就是这么干的；
    //   ② getSoundVolume() 会连带压低脚步声、游泳声、装备穿脱声等所有经由
    //      Entity#playSound 的声音，语义太脏，而且"0 音量"仍然会发包，不如直接不播。
    //
    // ⚠️ 仍然发声的部分：脚步（playStepSound）和落水（getSwimSound）走的是方块/通用音效，
    //    对所有生物一视同仁，所以没动。要连这些一起消掉，再覆写
    //    playStepSound / getSwimSound / getSwimSplashSound 返回空即可。

    /** 不播环境音（原先借用玩家的呼吸声）。 */
    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    /** 不播受伤声（原先借用玩家的受伤声）。 */
    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    /** 不播死亡声（原先借用玩家的死亡声）。 */
    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        // 玩家跑远也别清掉：第一代没了，无限复活就断了。
        return false;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        // 不打同类：无限复活的场面里自己打自己很难看
        return !(target instanceof RickEntity) && super.canAttack(target);
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    protected boolean shouldDropLoot(ServerLevel level) {
        return false;
    }
}
