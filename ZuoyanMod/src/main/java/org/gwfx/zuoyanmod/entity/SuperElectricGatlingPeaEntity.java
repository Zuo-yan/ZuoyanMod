package org.gwfx.zuoyanmod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.gwfx.zuoyanmod.sound.SoundRegistry;

import java.util.EnumSet;

/**
 * 超级电能机枪豌豆实体。
 * <p>
 * 特性：
 * <ul>
 *   <li>基础射击：每 0.7 秒（14 ticks）一瞬间射出 6 颗直线纵深弹链子弹（无散射）；</li>
 *   <li>25% 概率开大（大招）：在 3.5 秒内极速倾泻将近 210 发扇形散射雷霆弹幕；</li>
 *   <li>定制音效：带有电击高能电离与电流滋滋声；</li>
 *   <li>右键抱起/按住右键连发：空手右键抱在胸前，按住右键持续射击，享受直线弹链与 210 发散射大招；</li>
 *   <li>Shift + 右键放下：安全放置回地面继续驻守。</li>
 * </ul>
 */
public class SuperElectricGatlingPeaEntity extends PathfinderMob {

    private static final EntityDataAccessor<Boolean> DATA_SHOOTING =
            SynchedEntityData.defineId(SuperElectricGatlingPeaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_CARRIER_ID =
            SynchedEntityData.defineId(SuperElectricGatlingPeaEntity.class, EntityDataSerializers.INT);

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState shootAnimationState = new AnimationState();

    public int shootCooldown = 0;
    public int ultTicksRemaining = 0;
    private LivingEntity currentBurstTarget = null;

    public SuperElectricGatlingPeaEntity(EntityType<? extends SuperElectricGatlingPeaEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 160.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SHOOTING, false);
        builder.define(DATA_CARRIER_ID, -1);
    }

    public boolean isShooting() {
        return this.entityData.get(DATA_SHOOTING);
    }

    public void setShooting(boolean shooting) {
        this.entityData.set(DATA_SHOOTING, shooting);
    }

    public boolean isCarried() {
        return this.entityData.get(DATA_CARRIER_ID) >= 0;
    }

    public int getCarrierId() {
        return this.entityData.get(DATA_CARRIER_ID);
    }

    public Player getCarrier() {
        int id = getCarrierId();
        if (id < 0) return null;
        Entity entity = this.level().getEntity(id);
        return entity instanceof Player player ? player : null;
    }

    public void startCarrying(Player player) {
        this.entityData.set(DATA_CARRIER_ID, player.getId());
        this.setDeltaMovement(Vec3.ZERO);
    }

    public void stopCarrying() {
        this.entityData.set(DATA_CARRIER_ID, -1);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new GatlingAttackGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // 优先还击攻击者（不反击主人/玩家）
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return super.canUse() && !(this.mob.getLastHurtByMob() instanceof Player);
            }
        });

        // 主动搜寻并消灭附近的敌对生物
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                (target, lvl) -> target instanceof Enemy && !(target instanceof SuperElectricGatlingPeaEntity)));
    }

    @Override
    public void tick() {
        super.tick();

        // 处理抱持跟随状态（客户端与服务端双向对齐）
        if (this.isCarried()) {
            Player carrier = getCarrier();
            if (carrier == null || !carrier.isAlive() || carrier.isRemoved()) {
                if (!this.level().isClientSide()) {
                    stopCarrying();
                }
            } else {
                this.setYRot(carrier.getYRot());
                this.yRotO = carrier.getYRot();
                this.setXRot(carrier.getXRot());
                this.xRotO = carrier.getXRot();
                this.yBodyRot = carrier.yBodyRot;
                this.setYHeadRot(carrier.getYHeadRot());
                this.yHeadRotO = carrier.yHeadRotO;

                float yRot = carrier.getYRot();
                Vec3 forward = Vec3.directionFromRotation(0, yRot);
                double posX = carrier.getX() + forward.x * 0.45D;
                double posY = carrier.getY() + 0.65D;
                double posZ = carrier.getZ() + forward.z * 0.45D;
                this.setPos(posX, posY, posZ);
                this.setDeltaMovement(Vec3.ZERO);
                this.fallDistance = 0.0F;
            }
        }

        // 客户端动画状态同步
        if (this.level().isClientSide()) {
            this.idleAnimationState.startIfStopped(this.tickCount);
            if (this.isShooting()) {
                this.shootAnimationState.startIfStopped(this.tickCount);
            } else {
                this.shootAnimationState.stop();
            }
        } else {
            // 服务端点射与大招连射轮询
            handleBurstTick();
        }
    }

    /**
     * 玩家右键互动：抱起或放下
     */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (this.isCarried()) {
            if (player.getId() == this.getCarrierId() && player.isShiftKeyDown()) {
                if (!this.level().isClientSide()) {
                    putDown(player);
                }
               return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // 主手空手右键抱在手上
        if (player.getItemInHand(hand).isEmpty()) {
            if (!player.isShiftKeyDown()) {
                if (!this.level().isClientSide()) {
                    startCarrying(player);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
                }
               return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    /**
     * 安全将植物放置在玩家面前的地面上
     */
    public void putDown(Player player) {
        stopCarrying();
        Vec3 look = player.getLookAngle();
        Vec3 targetPos = player.position().add(look.x * 1.2D, 0.0D, look.z * 1.2D);
        this.setPos(targetPos.x, targetPos.y, targetPos.z);
        this.setYRot(player.getYRot());
        this.yRotO = player.getYRot();
        this.setXRot(0.0F);
        this.xRotO = 0.0F;
        this.yBodyRot = player.getYRot();
        this.setYHeadRot(player.getYRot());
        this.setDeltaMovement(Vec3.ZERO);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GRASS_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /**
     * 玩家手持时右键发射：每 0.7 秒（14 ticks）瞬间射出 6 颗直线纵深弹链子弹，25% 概率开大扫射 210 发
     */
    public void playerShoot(Player player) {
        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.shootCooldown <= 0 && this.ultTicksRemaining <= 0) {
                boolean triggerUlt = this.random.nextFloat() < 0.25F;
                if (triggerUlt) {
                    this.ultTicksRemaining = 70; // 3.5 秒狂暴扫射（70 ticks * 3 发/tick = 210 发）
                    this.shootCooldown = 70 + 14;
                    playUltSound(player.getX(), player.getY(), player.getZ(), SoundSource.PLAYERS);
                } else {
                    this.shootCooldown = 14; // 0.7 秒攻击间隔
                    fireStraightBurstFromPlayer(player);
                }
                this.currentBurstTarget = null;
                this.setShooting(true);
            }
        }
    }

    /**
     * 自动索敌启动连发脉冲（向指定目标发射直线 6 颗子弹，25% 概率开大招发射 210 颗子弹）
     */
    public void startBurstAt(LivingEntity target) {
        this.currentBurstTarget = target;
        boolean triggerUlt = this.random.nextFloat() < 0.25F;

        if (triggerUlt) {
            this.ultTicksRemaining = 70;
            this.shootCooldown = 70 + 14;
            playUltSound(this.getX(), this.getY(), this.getZ(), SoundSource.NEUTRAL);
        } else {
            this.shootCooldown = 14;
            fireStraightBurstAtTarget(target);
        }
        this.setShooting(true);
    }

    private void handleBurstTick() {
        if (this.shootCooldown > 0) {
            this.shootCooldown--;
        }

        if (this.ultTicksRemaining > 0) {
            if (this.isCarried()) {
                Player carrier = getCarrier();
                if (carrier != null) {
                    fireScatterBulletsFromPlayer(carrier, 3);
                }
            } else {
                fireScatterBulletsAtTarget(this.currentBurstTarget, 3);
            }
            this.ultTicksRemaining--;
            this.setShooting(true);
        } else if (this.shootCooldown <= 10 && this.isShooting()) {
            this.setShooting(false);
        }
    }

    private void fireStraightBurstFromPlayer(Player player) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition().add(look.scale(0.55D));

        // 一瞬间射出 6 颗直线纵深弹链子弹（无散射，梯度速度拉开纵深）
        float[] speeds = {2.0F, 2.15F, 2.3F, 2.45F, 2.6F, 2.75F};
        for (float spd : speeds) {
            ElectroPeaBulletEntity bullet = new ElectroPeaBulletEntity(serverLevel, player);
            bullet.setPos(eyePos.x, eyePos.y - 0.1D, eyePos.z);
            bullet.shoot(look.x, look.y, look.z, spd, 0.0F);
            serverLevel.addFreshEntity(bullet);
        }

        playShootEffects(eyePos, SoundSource.PLAYERS);
    }

    private void fireStraightBurstAtTarget(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        if (target != null && !target.isAlive()) return;

        Vec3 snoutPos = this.position().add(0, 0.95D, 0).add(this.getLookAngle().scale(0.65D));
        Vec3 shootDir;
        if (target != null) {
            shootDir = new Vec3(target.getX() - snoutPos.x, target.getY(0.5D) - snoutPos.y, target.getZ() - snoutPos.z).normalize();
        } else {
            shootDir = this.getLookAngle();
        }

        float[] speeds = {2.0F, 2.15F, 2.3F, 2.45F, 2.6F, 2.75F};
        for (float spd : speeds) {
            ElectroPeaBulletEntity bullet = new ElectroPeaBulletEntity(serverLevel, this);
            bullet.setPos(snoutPos.x, snoutPos.y, snoutPos.z);
            bullet.shoot(shootDir.x, shootDir.y, shootDir.z, spd, 0.0F);
            serverLevel.addFreshEntity(bullet);
        }

        playShootEffects(snoutPos, SoundSource.NEUTRAL);
    }

    private void fireScatterBulletsFromPlayer(Player player, int count) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition().add(look.scale(0.55D));

        for (int i = 0; i < count; i++) {
            ElectroPeaBulletEntity bullet = new ElectroPeaBulletEntity(serverLevel, player);
            bullet.setPos(eyePos.x, eyePos.y - 0.1D, eyePos.z);
            bullet.shoot(look.x, look.y, look.z, 2.6F, 10.0F); // 扇形散射
            serverLevel.addFreshEntity(bullet);
        }

        if (this.ultTicksRemaining % 3 == 0) {
            playShootEffects(eyePos, SoundSource.PLAYERS);
        }
    }

    private void fireScatterBulletsAtTarget(LivingEntity target, int count) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        Vec3 snoutPos = this.position().add(0, 0.95D, 0).add(this.getLookAngle().scale(0.65D));
        Vec3 shootDir;
        if (target != null && target.isAlive()) {
            shootDir = new Vec3(target.getX() - snoutPos.x, target.getY(0.5D) - snoutPos.y, target.getZ() - snoutPos.z).normalize();
        } else {
            shootDir = this.getLookAngle();
        }

        for (int i = 0; i < count; i++) {
            ElectroPeaBulletEntity bullet = new ElectroPeaBulletEntity(serverLevel, this);
            bullet.setPos(snoutPos.x, snoutPos.y, snoutPos.z);
            bullet.shoot(shootDir.x, shootDir.y, shootDir.z, 2.5F, 10.0F);
            serverLevel.addFreshEntity(bullet);
        }

        if (this.ultTicksRemaining % 3 == 0) {
            playShootEffects(snoutPos, SoundSource.NEUTRAL);
        }
    }

    private void playShootEffects(Vec3 pos, SoundSource source) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        int count = this.ultTicksRemaining > 0 ? 10 : 5;
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                pos.x, pos.y, pos.z, count, 0.12D, 0.12D, 0.12D, 0.08D);

        float pitch = 1.3F + (this.random.nextFloat() * 0.4F);
        serverLevel.playSound(null, pos.x, pos.y, pos.z,
                SoundRegistry.SUPER_GATLING_PEA_SHOOT.get(), source, 0.85F, pitch);
    }

    private void playUltSound(double x, double y, double z, SoundSource source) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, x, y, z,
                    SoundRegistry.SUPER_GATLING_PEA_ULT.get(), source, 1.2F, 1.0F);
        }
    }

    /**
     * 自动索敌加特林射击 AI
     */
    static class GatlingAttackGoal extends Goal {
        private final SuperElectricGatlingPeaEntity pea;

        public GatlingAttackGoal(SuperElectricGatlingPeaEntity pea) {
            this.pea = pea;
            this.setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // 当被玩家抱在手上时不自主开火，由玩家手动操控
            if (this.pea.isCarried()) return false;
            LivingEntity target = this.pea.getTarget();
            return target != null && target.isAlive() && this.pea.distanceToSqr(target) <= 32.0D * 32.0D;
        }

        @Override
        public void tick() {
            LivingEntity target = this.pea.getTarget();
            if (target == null) return;

            // 头部精准追踪瞄准目标
            this.pea.getLookControl().setLookAt(target, 35.0F, 35.0F);

            if (this.pea.shootCooldown <= 0 && this.pea.ultTicksRemaining <= 0) {
                this.pea.startBurstAt(target);
            }
        }
    }
}
