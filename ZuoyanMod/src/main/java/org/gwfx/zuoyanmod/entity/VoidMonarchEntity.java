package org.gwfx.zuoyanmod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
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

import java.util.List;

/**
 * 湮灭君主：湮灭王座遗迹的守关 Boss。
 *
 * <p>战斗设计（数值刻意拉高，参照系：凋灵 300 血 / Rick 攻击 42）：
 * <ul>
 *   <li><b>沉眠</b>——随遗迹生成时坐在王座上：不动、不受伤害、每秒回 5 血；
 *       玩家靠近王座 6 格才觉醒（标题演出 + 音效 + Boss 血条浮现），
 *       这是与暮色森林（Boss 直接站着等）和 WDA（没有 Boss）的差异化机制；</li>
 *   <li><b>挥砍</b>——近战 100 点，命中时对目标周围 4 格内的其他生物横扫 50 点；</li>
 *   <li><b>召唤</b>——血量低于 60% 召唤 3 名湮灭侍卫（只触发一次）；</li>
 *   <li><b>弹幕</b>——周期性向目标发射暗物质螺栓（每发 40 点），狂暴后加密；</li>
 *   <li><b>狂暴</b>——血量低于 30% 时攻击 +20（共 120）、移速 +20%。</li>
 * </ul>
 */
public class VoidMonarchEntity extends Monster {

    /** 触发觉醒的玩家接近半径（格） */
    private static final double AWAKEN_RADIUS = 6.0D;
    /** 横扫半径与伤害 */
    private static final double SWEEP_RADIUS = 4.0D;
    private static final float SWEEP_DAMAGE = 50.0F;
    /** 沉眠时每 tick 回复的血量（20 tick/s → 5 血/s） */
    private static final float DORMANT_HEAL_PER_TICK = 0.25F;
    /** 弹幕 Goal 的基础冷却（tick），狂暴减半 */
    private static final int BARRAGE_COOLDOWN = 120;
    private static final int BARRAGE_COUNT = 4;
    private static final double BARRAGE_RANGE = 24.0D;

    /** 狂暴加成的属性修饰符 ID（永久修饰符随实体 NBT 存档，不会重复叠加） */
    private static final Identifier ENRAGE_DAMAGE_ID =
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "void_monarch_enrage_damage");
    private static final Identifier ENRAGE_SPEED_ID =
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "void_monarch_enrage_speed");

    /** Boss 血条：紫红色、10 段刻度。沉眠时隐藏，觉醒后浮现。 */
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Mth.createInsecureUUID(this.random), this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    /** 是否已从沉眠中觉醒 */
    private boolean awakened;
    /** 是否已召唤过侍卫（每场战斗只召唤一轮） */
    private boolean summonedGuards;
    /** 是否已进入狂暴 */
    private boolean enraged;

    public boolean isEnraged() {
        return this.enraged;
    }

    public VoidMonarchEntity(EntityType<? extends VoidMonarchEntity> type, Level level) {
        super(type, level);
        // 结构生成的 Boss 不能随距离消失，否则玩家走远再回来 Boss 就没了
        this.setPersistenceRequired();
        this.xpReward = 500;
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
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(4, new BarrageGoal());
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ------------------------------------------------------------------
    // 沉眠 / 觉醒
    // ------------------------------------------------------------------

    public boolean isAwakened() {
        return this.awakened;
    }

    /** 沉眠时完全不挪动（同原版睡觉生物的 isImmobile 用法）。 */
    @Override
    protected boolean isImmobile() {
        return !this.awakened || super.isImmobile();
    }

    @Override
    public boolean isPushable() {
        return this.awakened && super.isPushable();
    }

    /** 沉眠时无敌——否则玩家隔墙射箭能把 Boss 磨死，觉醒演出就没了意义。 */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (!this.awakened && !source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)) {
            return false;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (!this.awakened) {
            // 沉眠回血 + 每 10 tick 扫一次附近玩家
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(DORMANT_HEAL_PER_TICK);
            }
            if (this.tickCount % 10 == 0) {
                Player nearby = this.level().getNearestPlayer(this, AWAKEN_RADIUS);
                if (nearby != null) {
                    awaken();
                }
            }
        }
    }

    private void awaken() {
        this.awakened = true;
        this.bossEvent.setVisible(true);
        // 觉醒演出后给玩家 3 秒喘息，弹幕不会立刻砸脸
        this.cooldownUntil = this.tickCount + 60;
        this.playSound(SoundEvents.WITHER_SPAWN, 3.0F, 0.7F);
        if (this.level() instanceof ServerLevel serverLevel) {
            // 全维度广播标题（Boss 遗迹值得一点仪式感）
            for (ServerPlayer player : serverLevel.players()) {
                player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                player.connection.send(new ClientboundSetTitleTextPacket(
                        Component.translatable("entity.zuoyanmod.void_monarch.awaken_title")));
            }
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                    120, 1.2D, 1.6D, 1.2D, 0.6D);
        }
    }

    // ------------------------------------------------------------------
    // 阶段机制
    // ------------------------------------------------------------------

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || !this.awakened) {
            return;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        float healthFraction = this.getHealth() / this.getMaxHealth();
        if (!this.summonedGuards && healthFraction < 0.6F) {
            this.summonedGuards = true;
            summonGuards();
        }
        if (!this.enraged && healthFraction < 0.3F) {
            this.enraged = true;
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
        this.playSound(SoundEvents.RAVAGER_ROAR, 3.0F, 1.2F);
        // 永久修饰符会随 NBT 存档，enraged 标记保证只在进入狂暴时加一次
        this.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(
                new AttributeModifier(ENRAGE_DAMAGE_ID, 20.0D, AttributeModifier.Operation.ADD_VALUE));
        this.getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(
                new AttributeModifier(ENRAGE_SPEED_ID, 0.20D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                    80, 1.0D, 1.4D, 1.0D, 0.1D);
        }
    }

    /** 近战横扫：主目标吃满 100，站在目标身边的生物被剑气波及。 */
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

    // ------------------------------------------------------------------
    // Boss 血条生命周期
    // ------------------------------------------------------------------

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
        super.die(source);
        this.bossEvent.setProgress(0.0F);
    }

    // ------------------------------------------------------------------
    // 存档
    // ------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Awakened", this.awakened);
        tag.putBoolean("SummonedGuards", this.summonedGuards);
        tag.putBoolean("Enraged", this.enraged);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        this.awakened = tag.getBooleanOr("Awakened", false);
        this.summonedGuards = tag.getBooleanOr("SummonedGuards", false);
        this.enraged = tag.getBooleanOr("Enraged", false);
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
        // 觉醒状态随存档恢复：读档后血条立即对在场玩家可见
        this.bossEvent.setVisible(this.awakened);
    }

    // ------------------------------------------------------------------
    // 音效
    // ------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return this.awakened ? SoundEvents.WITHER_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    // ------------------------------------------------------------------
    // 弹幕 Goal
    // ------------------------------------------------------------------

    /**
     * 暗物质弹幕：对着目标扇形发射暗物质螺栓。
     * 沉眠时永不触发；狂暴后冷却减半、弹数翻倍。
     */
    private class BarrageGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private int cooldown;

        BarrageGoal() {
            this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!VoidMonarchEntity.this.awakened || VoidMonarchEntity.this.cooldownUntil > VoidMonarchEntity.this.tickCount) {
                return false;
            }
            LivingEntity target = VoidMonarchEntity.this.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            double distSq = VoidMonarchEntity.this.distanceToSqr(target);
            return distSq < BARRAGE_RANGE * BARRAGE_RANGE && distSq > 9.0D;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            fireBarrage();
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

    /** Goal 结束后由 tick 推进冷却；字段挂在实体上方便存档期不需要持久化（数值级细节）。 */
    private int cooldownUntil;

    private void fireBarrage() {
        LivingEntity target = this.getTarget();
        if (target == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean enraged = this.enraged;
        int count = enraged ? BARRAGE_COUNT * 2 : BARRAGE_COUNT;
        for (int i = 0; i < count; i++) {
            VoidBoltEntity bolt = new VoidBoltEntity(this.level(), this);
            // 目标位置加随机散布，形成弹幕而非激光
            double spread = enraged ? 2.5D : 1.5D;
            double tx = target.getX() + (this.random.nextDouble() - 0.5D) * spread * 2.0D;
            double ty = target.getY() + target.getBbHeight() * 0.5D + (this.random.nextDouble() - 0.5D) * spread;
            double tz = target.getZ() + (this.random.nextDouble() - 0.5D) * spread * 2.0D;
            double dx = tx - this.getX();
            double dy = ty - (this.getEyeY() - 0.3D);
            double dz = tz - this.getZ();
            // shoot 内部对方向归一化后乘 velocity，这里只需给固定初速
            bolt.shoot(dx, dy, dz, 0.9F, 0.0F);
            serverLevel.addFreshEntity(bolt);
        }
        this.playSound(SoundEvents.BLAZE_SHOOT, 2.0F, 0.5F);
        this.cooldownUntil = this.tickCount
                + (enraged ? BARRAGE_COOLDOWN / 2 : BARRAGE_COOLDOWN) + this.random.nextInt(40);
    }
}
