package org.gwfx.zuoyanmod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.gwfx.zuoyanmod.Zuoyanmod;

import java.util.EnumSet;
import java.util.List;

/**
 * 湮灭君主：湮灭王座遗迹的守关 Boss。
 *
 * <p>战斗与动画设计：
 * <ul>
 *   <li><b>王座沉眠</b>——随遗迹生成端坐王座：免伤、回血；玩家靠近 6 格触发 3 秒霸体觉醒演出；</li>
 *   <li><b>近战挥斩</b>——手持湮灭君王之刃：普通阶段单手大范围横扫，二阶段追加狂暴双手跃起重劈；</li>
 *   <li><b>狂暴蜕变</b>——血量低于 30% 激活二阶段：展开背部虚空光轮双翼并怒吼，强化攻速移速；</li>
 *   <li><b>死亡跪地</b>——生命归零后进入 3 秒单膝下跪忏悔虚弱演出，大剑脱手插入地面并化作虚空粒子消散。</li>
 * </ul>
 */
public class VoidMonarchEntity extends Monster {

    private static final EntityDataAccessor<Boolean> DATA_AWAKENED =
            SynchedEntityData.defineId(VoidMonarchEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_AWAKENING =
            SynchedEntityData.defineId(VoidMonarchEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ENRAGED =
            SynchedEntityData.defineId(VoidMonarchEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_ATTACK_STATE =
            SynchedEntityData.defineId(VoidMonarchEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_IS_DYING =
            SynchedEntityData.defineId(VoidMonarchEntity.class, EntityDataSerializers.BOOLEAN);

    // 客户端关键帧动画控制器
    public final AnimationState sitAnimationState = new AnimationState();
    public final AnimationState awakenAnimationState = new AnimationState();
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState walkAnimationState = new AnimationState();
    public final AnimationState attackHorizontalAnimationState = new AnimationState();
    public final AnimationState attackOverheadAnimationState = new AnimationState();
    public final AnimationState phase2AnimationState = new AnimationState();
    public final AnimationState deathAnimationState = new AnimationState();
    public final AnimationState attackBarrageAnimationState = new AnimationState();

    /** 触发觉醒的玩家接近半径（格） */
    private static final double AWAKEN_RADIUS = 6.0D;
    /** 横扫半径与伤害 */
    private static final double SWEEP_RADIUS = 4.0D;
    private static final float SWEEP_DAMAGE = 50.0F;
    /** 沉眠时每 tick 回复的血量（20 tick/s -> 5 血/s） */
    private static final float DORMANT_HEAL_PER_TICK = 0.25F;
    /** 弹幕 Goal 的基础冷却（tick），狂暴减半 */
    private static final int BARRAGE_COOLDOWN = 120;
    private static final int BARRAGE_COUNT = 4;
    private static final double BARRAGE_RANGE = 24.0D;

    /** 狂暴加成的属性修饰符 ID */
    private static final Identifier ENRAGE_DAMAGE_ID =
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "void_monarch_enrage_damage");
    private static final Identifier ENRAGE_SPEED_ID =
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "void_monarch_enrage_speed");

    /** Boss 血条：紫红色、10 段刻度。沉眠时隐藏，觉醒后浮现。 */
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Mth.createInsecureUUID(this.random), this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    /** 是否已召唤过侍卫 */
    private boolean summonedGuards;
    /** 起身霸体倒计时（tick） */
    private int awakeningTicks = 0;
    /** 狂暴咆哮动画计时 */
    private int phase2RoarTicks = 0;
    /** 弹幕施法动画计时（0.7s 处释放 volley，1.3s 收势） */
    private int barrageCastTicks = 0;
    /** 弹幕 Goal 冷却 */
    private int cooldownUntil;

    public VoidMonarchEntity(EntityType<? extends VoidMonarchEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 500;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_AWAKENED, false);
        builder.define(DATA_AWAKENING, false);
        builder.define(DATA_ENRAGED, false);
        builder.define(DATA_ATTACK_STATE, (byte) 0);
        builder.define(DATA_IS_DYING, false);
    }

    public boolean isAwakened() {
        return this.entityData.get(DATA_AWAKENED);
    }

    public boolean isAwakening() {
        return this.entityData.get(DATA_AWAKENING);
    }

    public boolean isEnraged() {
        return this.entityData.get(DATA_ENRAGED);
    }

    public byte getAttackState() {
        return this.entityData.get(DATA_ATTACK_STATE);
    }

    public boolean isDying() {
        return this.entityData.get(DATA_IS_DYING);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 2048.0D)
                .add(Attributes.ATTACK_DAMAGE, 100.0D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MonarchBladeAttackGoal());
        this.goalSelector.addGoal(4, new BarrageGoal());
        this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected boolean isImmobile() {
        return !isAwakened() || isAwakening() || isDying() || super.isImmobile();
    }

    @Override
    public boolean isPushable() {
        return isAwakened() && !isAwakening() && !isDying() && super.isPushable();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if ((!isAwakened() || isAwakening() || isDying()) && !source.is(DamageTypes.GENERIC_KILL)) {
            return false;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            updateClientAnimations();
            return;
        }

        if (!isAwakened()) {
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(DORMANT_HEAL_PER_TICK);
            }
            if (this.tickCount % 10 == 0) {
                Player nearby = this.level().getNearestPlayer(this, AWAKEN_RADIUS);
                if (nearby != null) {
                    awaken();
                }
            }
            return;
        }

        if (this.awakeningTicks > 0) {
            this.awakeningTicks--;
            if (this.awakeningTicks == 0) {
                this.entityData.set(DATA_AWAKENING, false);
            }
        }

        if (this.phase2RoarTicks > 0) {
            this.phase2RoarTicks--;
            if (this.phase2RoarTicks == 0 && this.getAttackState() == 3) {
                this.entityData.set(DATA_ATTACK_STATE, (byte) 0);
            }
        }

        if (this.barrageCastTicks > 0) {
            this.barrageCastTicks--;
            if (this.barrageCastTicks == 14 && this.getTarget() != null) {
                fireBarrage();
            }
            if (this.barrageCastTicks == 0 && this.getAttackState() == 4) {
                this.entityData.set(DATA_ATTACK_STATE, (byte) 0);
            }
        }
    }

    private void updateClientAnimations() {
        if (isDying()) {
            this.deathAnimationState.startIfStopped(this.tickCount);
            this.sitAnimationState.stop();
            this.awakenAnimationState.stop();
            this.idleAnimationState.stop();
            this.walkAnimationState.stop();
            this.attackHorizontalAnimationState.stop();
            this.attackOverheadAnimationState.stop();
            this.phase2AnimationState.stop();
            this.attackBarrageAnimationState.stop();
            return;
        }

        if (!isAwakened()) {
            this.sitAnimationState.startIfStopped(this.tickCount);
            this.awakenAnimationState.stop();
            this.idleAnimationState.stop();
            this.walkAnimationState.stop();
            return;
        }

        if (isAwakening()) {
            this.awakenAnimationState.startIfStopped(this.tickCount);
            this.sitAnimationState.stop();
            this.idleAnimationState.stop();
            return;
        }

        this.sitAnimationState.stop();
        this.awakenAnimationState.stop();
        this.idleAnimationState.startIfStopped(this.tickCount);

        if (this.getDeltaMovement().horizontalDistanceSqr() > 0.0008D) {
            this.walkAnimationState.startIfStopped(this.tickCount);
        } else {
            this.walkAnimationState.stop();
        }

        byte atk = getAttackState();
        if (atk == 1) {
            this.attackHorizontalAnimationState.startIfStopped(this.tickCount);
        } else {
            this.attackHorizontalAnimationState.stop();
        }

        if (atk == 2) {
            this.attackOverheadAnimationState.startIfStopped(this.tickCount);
        } else {
            this.attackOverheadAnimationState.stop();
        }

        if (atk == 3) {
            this.phase2AnimationState.startIfStopped(this.tickCount);
        } else {
            this.phase2AnimationState.stop();
        }

        if (atk == 4) {
            this.attackBarrageAnimationState.startIfStopped(this.tickCount);
        } else {
            this.attackBarrageAnimationState.stop();
        }
    }

    private void awaken() {
        this.entityData.set(DATA_AWAKENED, true);
        this.entityData.set(DATA_AWAKENING, true);
        this.awakeningTicks = 60;
        this.cooldownUntil = this.tickCount + 80;
        this.bossEvent.setVisible(true);

        this.playSound(SoundEvents.WITHER_SPAWN, 3.0F, 0.7F);
        if (this.level() instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                player.connection.send(new ClientboundSetTitleTextPacket(
                        Component.translatable("entity.zuoyanmod.void_monarch.awaken_title")));
            }
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                    150, 1.5D, 1.8D, 1.5D, 0.8D);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || !isAwakened() || isAwakening() || isDying()) {
            return;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        float healthFraction = this.getHealth() / this.getMaxHealth();
        if (!this.summonedGuards && healthFraction < 0.6F) {
            this.summonedGuards = true;
            summonGuards();
        }
        if (!isEnraged() && healthFraction < 0.3F) {
            applyEnrage();
        }
    }

    private void summonGuards() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 2.0F, 0.6F);
        for (int i = 0; i < 3; i++) {
            double angle = (Math.PI * 2.0D / 3.0D) * i + this.random.nextDouble();
            double x = this.getX() + Math.cos(angle) * 4.0D;
            double z = this.getZ() + Math.sin(angle) * 4.0D;
            VoidGuardEntity guard = EntityRegistry.VOID_GUARD.get()
                    .create(this.level(), EntitySpawnReason.MOB_SUMMONED);
            if (guard == null) {
                continue;
            }
            guard.setPos(x, this.getY(), z);
            guard.setYRot(this.getYRot());
            guard.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(guard.blockPosition()),
                    EntitySpawnReason.MOB_SUMMONED, null);
            guard.setTarget(this.getTarget());
            serverLevel.addFreshEntity(guard);
            serverLevel.sendParticles(ParticleTypes.PORTAL, x, this.getY() + 1.0D, z,
                    40, 0.5D, 1.0D, 0.5D, 0.3D);
        }
    }

    private void applyEnrage() {
        this.entityData.set(DATA_ENRAGED, true);
        this.entityData.set(DATA_ATTACK_STATE, (byte) 3);
        this.phase2RoarTicks = 40;

        this.playSound(SoundEvents.RAVAGER_ROAR, 3.5F, 0.9F);
        this.playSound(SoundEvents.WITHER_SPAWN, 2.5F, 1.2F);

        this.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(
                new AttributeModifier(ENRAGE_DAMAGE_ID, 20.0D, AttributeModifier.Operation.ADD_VALUE));
        this.getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(
                new AttributeModifier(ENRAGE_SPEED_ID, 0.20D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    this.getX(), this.getY() + 1.5D, this.getZ(),
                    120, 1.2D, 1.8D, 1.2D, 0.8D);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY() + 1.5D, this.getZ(),
                    80, 1.0D, 1.4D, 1.0D, 0.1D);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            AABB sweepBox = target.getBoundingBox().inflate(SWEEP_RADIUS);
            List<LivingEntity> bystanders = level.getEntitiesOfClass(LivingEntity.class, sweepBox,
                    e -> e != this && e != target && e.isAlive() && !this.isAlliedTo(e));
            for (LivingEntity bystander : bystanders) {
                bystander.hurtServer(level, this.damageSources().mobAttack(this), SWEEP_DAMAGE);
            }
        }
        return hit;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void die(DamageSource source) {
        this.entityData.set(DATA_IS_DYING, true);
        this.entityData.set(DATA_ATTACK_STATE, (byte) 0);
        this.bossEvent.setProgress(0.0F);
        this.setTarget(null);
        super.die(source);
    }

    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.deathTime % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        this.getX() + (this.random.nextDouble() - 0.5D) * 1.5D,
                        this.getY() + this.random.nextDouble() * 2.5D,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * 1.5D,
                        15, 0.4D, 0.6D, 0.4D, 0.1D);
            }
            if (this.deathTime == 20) {
                this.playSound(SoundEvents.WITHER_HURT, 3.0F, 0.5F);
            }
            if (this.deathTime >= 60 && !this.isRemoved()) {
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        this.getX(), this.getY() + 1.2D, this.getZ(),
                        200, 1.5D, 1.8D, 1.5D, 0.3D);
                this.level().broadcastEntityEvent(this, (byte) 60);
                this.remove(RemovalReason.KILLED);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Awakened", isAwakened());
        tag.putBoolean("SummonedGuards", this.summonedGuards);
        tag.putBoolean("Enraged", isEnraged());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        boolean aw = tag.getBooleanOr("Awakened", false);
        this.entityData.set(DATA_AWAKENED, aw);
        this.summonedGuards = tag.getBooleanOr("SummonedGuards", false);
        boolean en = tag.getBooleanOr("Enraged", false);
        this.entityData.set(DATA_ENRAGED, en);
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
        this.bossEvent.setVisible(aw);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isAwakened() && !isDying() ? SoundEvents.WITHER_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    private class MonarchBladeAttackGoal extends Goal {
        private int attackTicks = 0;
        private int cooldown = 0;
        private byte chosenType = 1;

        public MonarchBladeAttackGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!VoidMonarchEntity.this.isAwakened() || VoidMonarchEntity.this.isAwakening() || VoidMonarchEntity.this.isDying()) {
                return false;
            }
            // 二阶段纯施法者：君主之刃近战只在常态使用
            if (VoidMonarchEntity.this.isEnraged()) {
                return false;
            }
            if (VoidMonarchEntity.this.phase2RoarTicks > 0 || this.cooldown > 0) {
                if (this.cooldown > 0) {
                    this.cooldown--;
                }
                return false;
            }
            LivingEntity target = VoidMonarchEntity.this.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            return VoidMonarchEntity.this.distanceToSqr(target) <= 25.0D;
        }

        @Override
        public void start() {
            this.attackTicks = 0;
            if (VoidMonarchEntity.this.isEnraged() && VoidMonarchEntity.this.random.nextBoolean()) {
                this.chosenType = 2;
            } else {
                this.chosenType = 1;
            }
            VoidMonarchEntity.this.entityData.set(DATA_ATTACK_STATE, this.chosenType);
        }

        @Override
        public boolean canContinueToUse() {
            return this.attackTicks < (this.chosenType == 2 ? 24 : 20);
        }

        @Override
        public void tick() {
            LivingEntity target = VoidMonarchEntity.this.getTarget();
            if (target != null) {
                VoidMonarchEntity.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }

            this.attackTicks++;

            int hitFrame = (this.chosenType == 2) ? 11 : 8;
            if (this.attackTicks == hitFrame && target != null) {
                if (VoidMonarchEntity.this.distanceToSqr(target) <= 36.0D && VoidMonarchEntity.this.level() instanceof ServerLevel serverLevel) {
                    if (this.chosenType == 2) {
                        VoidMonarchEntity.this.doHurtTarget(serverLevel, target);
                        target.hurtServer(serverLevel, VoidMonarchEntity.this.damageSources().mobAttack(VoidMonarchEntity.this), 40.0F);
                        VoidMonarchEntity.this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.5F, 1.2F);
                        serverLevel.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.2D, target.getZ(), 5, 0.5D, 0.2D, 0.5D, 0.05D);
                    } else {
                        VoidMonarchEntity.this.doHurtTarget(serverLevel, target);
                        VoidMonarchEntity.this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.8F);
                    }
                }
            }
        }

        @Override
        public void stop() {
            VoidMonarchEntity.this.entityData.set(DATA_ATTACK_STATE, (byte) 0);
            this.cooldown = VoidMonarchEntity.this.isEnraged() ? 8 : 16;
        }
    }

    private class BarrageGoal extends Goal {
        BarrageGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!VoidMonarchEntity.this.isAwakened() || VoidMonarchEntity.this.isAwakening() || VoidMonarchEntity.this.isDying()) {
                return false;
            }
            if (VoidMonarchEntity.this.barrageCastTicks > 0
                    || VoidMonarchEntity.this.cooldownUntil > VoidMonarchEntity.this.tickCount || VoidMonarchEntity.this.phase2RoarTicks > 0) {
                return false;
            }
            LivingEntity target = VoidMonarchEntity.this.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            double distSq = VoidMonarchEntity.this.distanceToSqr(target);
            return distSq < BARRAGE_RANGE * BARRAGE_RANGE && distSq > 16.0D;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            // 起手施法动画（attackState 4），volley 由 tick 计时在 0.7s 处释放
            VoidMonarchEntity.this.entityData.set(DATA_ATTACK_STATE, (byte) 4);
            VoidMonarchEntity.this.barrageCastTicks = 26;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

    private void fireBarrage() {
        LivingEntity target = this.getTarget();
        if (target == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean enraged = isEnraged();
        int count = enraged ? BARRAGE_COUNT * 2 : BARRAGE_COUNT;
        for (int i = 0; i < count; i++) {
            VoidBoltEntity bolt = new VoidBoltEntity(this.level(), this);
            double spread = enraged ? 2.5D : 1.5D;
            double tx = target.getX() + (this.random.nextDouble() - 0.5D) * spread * 2.0D;
            double ty = target.getY() + target.getBbHeight() * 0.5D + (this.random.nextDouble() - 0.5D) * spread;
            double tz = target.getZ() + (this.random.nextDouble() - 0.5D) * spread * 2.0D;
            double dx = tx - this.getX();
            double dy = ty - (this.getEyeY() - 0.3D);
            double dz = tz - this.getZ();
            bolt.shoot(dx, dy, dz, 0.9F, 0.0F);
            serverLevel.addFreshEntity(bolt);
        }
        this.playSound(SoundEvents.BLAZE_SHOOT, 2.0F, 0.5F);
        this.cooldownUntil = this.tickCount
                + (enraged ? BARRAGE_COOLDOWN / 2 : BARRAGE_COOLDOWN) + this.random.nextInt(40);
    }
}
