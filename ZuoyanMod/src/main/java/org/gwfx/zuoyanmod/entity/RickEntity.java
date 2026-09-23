package org.gwfx.zuoyanmod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
 */
public class RickEntity extends PathfinderMob implements NeutralMob {

    /** 复活后的瑞克继承的仇恨时间：够它跑到凶手面前再打一架。 */
    private static final int REBIRTH_ANGER_TICKS = 200;

    /** 中立仇恨默认持续时长（狼是 400~1200 随机，这里取固定值，行为可预期）。 */
    private static final int PERSISTENT_ANGER_TICKS = 400;

    /** 复活时的粒子数量，纯装饰。 */
    private static final int REBIRTH_PARTICLE_COUNT = 30;

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
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        // 1：谁打我，我就打谁 —— "被打才还手"的主入口
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // 2：把被激怒的玩家重新认回来（复活后、或仇恨目标一度丢失时用）。
        //    mustSee=true  → 看不见就不锁定，避免隔墙索敌
        //    mustReach=false → 允许隔着一段距离先记住目标
        //    末尾 this::isAngryAt 是过滤器：不生气时整个选择器不工作。
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, Player.class, 10, true, false, this::isAngryAt));
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
        return result;
    }

    // ===================== 音效 / 杂项 =====================

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        // 用玩家的呼吸声，比僵尸的喉音更贴合人形中立生物
        return SoundEvents.PLAYER_BREATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
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
