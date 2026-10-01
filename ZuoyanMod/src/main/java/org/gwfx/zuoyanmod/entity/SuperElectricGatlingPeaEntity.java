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
import net.minecraft.world.entity.Pose;
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
 *   <li>基础射击：每次进行高速 5 连发射击；</li>
 *   <li>35% 概率开大（大招）：爆发倾泻 24 发暴雨般的电浆风暴；</li>
 *   <li>定制音效：带有电击高能电离与电流滋滋声；</li>
 *   <li>右键抱起/按住右键连发：玩家抱在胸前可按住右键持续射击，同样具有 35% 开大陆续爆发机制；</li>
 *   <li>Shift + 右键放下：安全放置回地面继续驻守。</li>
 * </ul>
 */
public class SuperElectricGatlingPeaEntity extends PathfinderMob {

    private static final EntityDataAccessor<Boolean> DATA_SHOOTING =
            SynchedEntityData.defineId(SuperElectricGatlingPeaEntity.class, EntityDataSerializers.BOOLEAN);

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState shootAnimationState = new AnimationState();

    public int shootCooldown = 0;
    private int burstShotsRemaining = 0;
    private int burstDelay = 0;
    private boolean isUltBurst = false;
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
    }

    public boolean isShooting() {
        return this.entityData.get(DATA_SHOOTING);
    }

    public void setShooting(boolean shooting) {
        this.entityData.set(DATA_SHOOTING, shooting);
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

    @Override
    public void rideTick() {
        super.rideTick();
        // 被玩家抱着时，视角完全跟随玩家准心
        if (this.getVehicle() instanceof LivingEntity vehicle) {
            this.setYRot(vehicle.getYRot());
            this.yRotO = vehicle.getYRot();
            this.setXRot(vehicle.getXRot());
            this.xRotO = vehicle.getXRot();
            this.yBodyRot = vehicle.yBodyRot;
            this.yHeadRot = vehicle.getYHeadRot();
            this.yHeadRotO = vehicle.yHeadRotO;
        }
    }

    @Override
    public Vec3 getVehicleAttachmentPoint(Entity vehicle) {
        // 当载具是玩家时，将自身定位于玩家胸前正前方（抱在手上）
        if (vehicle instanceof Player player) {
            float yRot = player.getYRot();
            Vec3 forward = Vec3.directionFromRotation(0, yRot);
            double targetY = player.getY() + 0.85D;
            double offsetY = (player.getY() + player.getDimensions(Pose.STANDING).height()) - targetY;
            return new Vec3(-forward.x * 0.45D, offsetY, -forward.z * 0.45D);
        }
        return super.getVehicleAttachmentPoint(vehicle);
    }

    /**
     * 玩家右键互动：抱起或放下
     */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide()) {
            if (player.isShiftKeyDown()) {
                // Shift + 右键：放下
                if (this.isPassenger()) {
                    putDown(player);
                    return InteractionResult.SUCCESS;
                }
            } else {
                // 普通右键：抱在手上
                if (!this.isPassenger() && player.getPassengers().isEmpty()) {
                    this.startRiding(player, true, true);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return super.mobInteract(player, hand);
    }

    /**
     * 安全将植物放置在玩家面前的地面上
     */
    public void putDown(Player player) {
        this.stopRiding();
        Vec3 look = player.getLookAngle();
        Vec3 targetPos = player.position().add(look.x * 1.2D, 0.0D, look.z * 1.2D);
        this.setPos(targetPos.x, targetPos.y, targetPos.z);
        this.setYRot(player.getYRot());
        this.yRotO = player.getYRot();
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GRASS_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /**
     * 玩家手持时右键连续发射高速电能豌豆（支持 35% 概率开大）
     */
    public void playerShoot(Player player) {
        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.shootCooldown <= 0) {
                // 判断是否开大招（35% 概率）
                boolean triggerUlt = this.random.nextFloat() < 0.35F;
                if (triggerUlt) {
                    this.isUltBurst = true;
                    this.burstShotsRemaining = 24; // 大招倾泻 24 发
                    this.burstDelay = 0;
                    this.shootCooldown = 18; // 大招连射期间极速间隔
                    playUltSound(player.getX(), player.getY(), player.getZ(), SoundSource.PLAYERS);
                } else {
                    this.isUltBurst = false;
                    this.burstShotsRemaining = 5; // 基础一次发射 5 颗
                    this.burstDelay = 0;
                    this.shootCooldown = 14;
                }
                this.currentBurstTarget = null;
                this.setShooting(true);
            }
        }
    }

    /**
     * 自动索敌启动连发脉冲（向指定目标发射 5 颗子弹，35% 概率开大招发射 24 颗子弹）
     */
    public void startBurstAt(LivingEntity target) {
        this.currentBurstTarget = target;
        boolean triggerUlt = this.random.nextFloat() < 0.35F;

        if (triggerUlt) {
            this.isUltBurst = true;
            this.burstShotsRemaining = 24; // 开大招：倾泻 24 发雷霆暴风！
            this.burstDelay = 0;
            this.shootCooldown = 32;       // 大招爆发循环
            playUltSound(this.getX(), this.getY(), this.getZ(), SoundSource.NEUTRAL);
        } else {
            this.isUltBurst = false;
            this.burstShotsRemaining = 5;  // 基础：一次 5 颗
            this.burstDelay = 0;
            this.shootCooldown = 24;       // 普攻点射循环
        }
        this.setShooting(true);
    }

    private void handleBurstTick() {
        if (this.shootCooldown > 0) {
            this.shootCooldown--;
        }

        if (this.burstShotsRemaining > 0) {
            if (this.burstDelay <= 0) {
                if (this.isPassenger() && this.getVehicle() instanceof Player player) {
                    fireBulletFromPlayer(player);
                } else {
                    fireSingleBulletAtTarget(this.currentBurstTarget);
                }
                this.burstShotsRemaining--;
                // 大招每发间隔 2 tick，普攻每发间隔 3 tick
                this.burstDelay = this.isUltBurst ? 2 : 3;
            } else {
                this.burstDelay--;
            }
        } else if (this.shootCooldown <= 6 && this.isShooting()) {
            this.setShooting(false);
            this.isUltBurst = false;
        }
    }

    private void fireBulletFromPlayer(Player player) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition().add(look.scale(0.55D));

        ElectroPeaBulletEntity bullet = new ElectroPeaBulletEntity(serverLevel, player);
        bullet.setPos(eyePos.x, eyePos.y - 0.1D, eyePos.z);
        float speed = this.isUltBurst ? 2.6F : 2.2F;
        float spread = this.isUltBurst ? 2.5F : 1.0F; // 大招带有些微扇形暴雨散射
        bullet.shoot(look.x, look.y, look.z, speed, spread);
        serverLevel.addFreshEntity(bullet);

        playShootEffects(eyePos, SoundSource.PLAYERS);
    }

    private void fireSingleBulletAtTarget(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        if (target != null && !target.isAlive()) return;

        Vec3 snoutPos = this.position().add(0, 0.95D, 0).add(this.getLookAngle().scale(0.65D));
        ElectroPeaBulletEntity bullet = new ElectroPeaBulletEntity(serverLevel, this);
        bullet.setPos(snoutPos.x, snoutPos.y, snoutPos.z);

        float speed = this.isUltBurst ? 2.5F : 2.1F;
        float spread = this.isUltBurst ? 2.2F : 1.2F;

        if (target != null) {
            double dx = target.getX() - snoutPos.x;
            double dy = target.getY(0.5D) - snoutPos.y;
            double dz = target.getZ() - snoutPos.z;
            bullet.shoot(dx, dy, dz, speed, spread);
        } else {
            Vec3 look = this.getLookAngle();
            bullet.shoot(look.x, look.y, look.z, speed, spread);
        }

        serverLevel.addFreshEntity(bullet);

        playShootEffects(snoutPos, SoundSource.NEUTRAL);
    }

    private void playShootEffects(Vec3 pos, SoundSource source) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        // 强电离火花与闪电微光
        int count = this.isUltBurst ? 10 : 5;
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                pos.x, pos.y, pos.z, count, 0.12D, 0.12D, 0.12D, 0.08D);

        // 电流滋滋声与能量脉冲音效（结合定制音效与高音调高频电弧声）
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
            if (this.pea.isPassenger()) return false;
            LivingEntity target = this.pea.getTarget();
            return target != null && target.isAlive() && this.pea.distanceToSqr(target) <= 32.0D * 32.0D;
        }

        @Override
        public void tick() {
            LivingEntity target = this.pea.getTarget();
            if (target == null) return;

            // 头部精准追踪瞄准目标
            this.pea.getLookControl().setLookAt(target, 35.0F, 35.0F);

            if (this.pea.shootCooldown <= 0 && this.pea.burstShotsRemaining <= 0) {
                this.pea.startBurstAt(target);
            }
        }
    }
}

