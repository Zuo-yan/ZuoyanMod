package org.gwfx.zuoyanmod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 湮灭君主的暗物质螺栓：弹幕投射物。
 *
 * <p>刻意不复用 {@link CausalityBulletEntity}：那颗弹命中会走
 * {@code MultiverseCloneService} 的平行宇宙结算，是因果律手枪的专属玩法。
 * 这颗螺栓就是纯粹的暗物质伤害弹：命中实体结算 40 点伤害、命中方块湮灭消散。
 *
 * <p>视觉：紫色传送门粒子拖尾 + 命中端影火粒子，客户端用
 * {@code VoidBoltRenderer} 的 billboard 暗色能量球呈现。
 */
public class VoidBoltEntity extends ThrowableProjectile {

    /** 每发螺栓的伤害（从君主视角 mobProjectile 结算） */
    public static final float DAMAGE = 40.0F;

    public VoidBoltEntity(EntityType<? extends VoidBoltEntity> type, Level level) {
        super(type, level);
    }

    public VoidBoltEntity(Level level, LivingEntity owner) {
        super(EntityRegistry.VOID_BOLT.get(),
                owner.getX(), owner.getEyeY() - 0.3D, owner.getZ(), level);
        setOwner(owner);
    }

    /** 本实体无额外客户端同步数据（同 CausalityBulletEntity 的约定）。 */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        // 带一点下坠，远距离弹幕有弧线感
        return 0.03D;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    this.getX(), this.getY(), this.getZ(), 2, 0.1D, 0.1D, 0.1D, 0.02D);
            if (this.tickCount > 200) {
                this.discard();
            }
        }
        // 1.21.1 没有可覆写的空气阻力钩子（ThrowableProjectile#tick 硬编码 0.99）：
        // 每 tick 把被乘掉的 0.99 补回来，等效 26.x 的 getAirDrag() = 1.0（无空气阻力）。
        this.setDeltaMovement(this.getDeltaMovement().scale(1.0D / 0.99D));
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level() instanceof ServerLevel serverLevel
                && result.getEntity() instanceof LivingEntity target
                && !(result.getEntity() instanceof VoidMonarchEntity)
                && !(result.getEntity() instanceof VoidGuardEntity)) {
            target.hurt(this.damageSources().mobProjectile(this, getOwnerLiving()), DAMAGE);
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(), this.getY(), this.getZ(), 12, 0.2D, 0.2D, 0.2D, 0.02D);
            this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE,
                    SoundSource.HOSTILE, 0.8F, 0.6F);
        }
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY(), this.getZ(), 8, 0.2D, 0.2D, 0.2D, 0.02D);
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
