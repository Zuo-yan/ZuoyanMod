package org.gwfx.zuoyanmod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 电能豌豆子弹：超级电能机枪豌豆发射的高速电浆飞行物。
 * 命中目标后造成高额电击伤害，并引发连锁闪电跳跃攻击附近的敌对怪物。
 */
public class ElectroPeaBulletEntity extends ThrowableProjectile {

    public static final float BASE_DAMAGE = 14.0F;
    public static final float CHAIN_DAMAGE = 9.0F;
    public static final double CHAIN_RANGE = 7.0D;
    public static final int MAX_CHAIN_TARGETS = 4;

    public ElectroPeaBulletEntity(EntityType<? extends ElectroPeaBulletEntity> type, Level level) {
        super(type, level);
    }

    public ElectroPeaBulletEntity(Level level, LivingEntity owner) {
        super(EntityRegistry.ELECTRO_PEA_BULLET.get(),
                owner.getX(), owner.getEyeY() - 0.2D, owner.getZ(), level);
        setOwner(owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected float getAirDrag() {
        return 0.99F;
    }

    @Override
    protected double getDefaultGravity() {
        // 近距离准直线，远距离微下坠
        return 0.015D;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            // 飞行拖尾电火花与微光粒子
            Vec3 pos = this.position();
            Vec3 vel = this.getDeltaMovement();
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.x - vel.x * 0.5D, pos.y - vel.y * 0.5D, pos.z - vel.z * 0.5D,
                    2, 0.05D, 0.05D, 0.05D, 0.02D);

            if (this.tickCount % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.GLOW,
                        pos.x, pos.y, pos.z, 1, 0.02D, 0.02D, 0.02D, 0.01D);
            }

            // 超过 120 tick (6秒) 自动销毁
            if (this.tickCount > 120) {
                this.discard();
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (result.getEntity() instanceof LivingEntity target) {
            LivingEntity owner = getOwnerLiving();
            // 不伤自己和同类植物
            if (target == owner || target instanceof SuperElectricGatlingPeaEntity) {
                return;
            }

            DamageSource damageSource = owner != null
                    ? (owner instanceof Player player ? this.damageSources().playerAttack(player) : this.damageSources().mobProjectile(this, owner))
                    : this.damageSources().magic();

           // 重置受击无敌帧，确保 6 发直线子弹与 210 发弹幕扫射能打满连击伤害
           target.setInvulnerableTime(0);

            // 结算主体伤害
            target.hurtServer(serverLevel, damageSource, BASE_DAMAGE);

           // 音效节流：20% 概率播放震耳雷声，避免大招 210 发密集命中导致爆音卡顿
           if (this.random.nextFloat() < 0.2F) {
               serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                       SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.NEUTRAL, 0.7F, 1.6F);
               serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                       SoundEvents.TRIDENT_THUNDER.value(), SoundSource.NEUTRAL, 0.5F, 1.8F);
           }

            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                   8, 0.25D, 0.25D, 0.25D, 0.1D);

            // 连锁闪电（Chain Lightning）
            triggerChainLightning(serverLevel, target, owner, damageSource);
        }

        this.discard();
    }

    /**
     * 触发连锁闪电：以首发目标为中心，搜寻周围的敌对生物并弹射电弧。
     */
    private void triggerChainLightning(ServerLevel level, LivingEntity primaryTarget,
                                       LivingEntity owner, DamageSource damageSource) {
        AABB searchBox = primaryTarget.getBoundingBox().inflate(CHAIN_RANGE);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox, entity ->
                entity != primaryTarget
                        && entity != owner
                        && !(entity instanceof SuperElectricGatlingPeaEntity)
                        && entity.isAlive()
                        && (entity instanceof Enemy || entity.isAttackable())
                        && !(entity instanceof Player)
        );

        int chained = 0;
        Vec3 startPos = primaryTarget.position().add(0, primaryTarget.getBbHeight() * 0.5D, 0);

        for (LivingEntity nextTarget : candidates) {
            if (chained >= MAX_CHAIN_TARGETS) break;

            // 造成连锁电击伤害
            nextTarget.hurtServer(level, damageSource, CHAIN_DAMAGE);

            Vec3 endPos = nextTarget.position().add(0, nextTarget.getBbHeight() * 0.5D, 0);
            drawLightningArc(level, startPos, endPos);

            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    endPos.x, endPos.y, endPos.z, 12, 0.2D, 0.2D, 0.2D, 0.1D);

            chained++;
        }

        if (chained > 0) {
            level.playSound(null, primaryTarget.getX(), primaryTarget.getY(), primaryTarget.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.NEUTRAL, 0.4F, 2.0F);
        }
    }

    /**
     * 沿着起点与终点绘制电弧离子束
     */
    private void drawLightningArc(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 diff = to.subtract(from);
        double dist = diff.length();
        int steps = Math.max(3, (int) (dist * 4));
        Vec3 stepVec = diff.scale(1.0D / steps);

        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(stepVec.scale(i));
            // 微微随机扰动电弧折线感
            double jx = (level.getRandom().nextDouble() - 0.5D) * 0.12D;
            double jy = (level.getRandom().nextDouble() - 0.5D) * 0.12D;
            double jz = (level.getRandom().nextDouble() - 0.5D) * 0.12D;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    p.x + jx, p.y + jy, p.z + jz, 1, 0, 0, 0, 0);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    this.getX(), this.getY(), this.getZ(), 10, 0.15D, 0.15D, 0.15D, 0.08D);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.NEUTRAL, 0.4F, 1.8F);
        }
        this.discard();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }

    private LivingEntity getOwnerLiving() {
        return getOwner() instanceof LivingEntity living ? living : null;
    }
}

