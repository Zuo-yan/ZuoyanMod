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
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 因果律手枪射出的「平行宇宙射线」子弹。
 *
 * <p>子弹本体是纯粹的能量体，<b>不直接造成伤害</b>：
 * 命中实体时移交 {@link MultiverseCloneService#tryResolve} 结算——
 * 普通 Mob 被拉出平行宇宙同位体互殴（无掉落防刷、时限消散），
 * 玩家与 Boss 改判湮灭射线，同位体自身不可再被命中（防套娃）。
 *
 * <p>视觉表现：飞行时拖曳绿金小星 + 白色亮芯尾迹（服务端粒子），
 * 命中任何东西时音爆冲击波 + 电光迸射并消散。
 */
public class CausalityBulletEntity extends ThrowableProjectile {

    /** 尾迹粒子：绿金小星（WAX_ON）+ 白色亮芯（END_ROD），替代廉价的红石尘埃 */

    /** EntityFactory 用构造器（反序列化/注册表路径） */
    public CausalityBulletEntity(EntityType<? extends CausalityBulletEntity> type, Level level) {
        super(type, level);
    }

    /** 发射用构造器：从射手眼部生成 */
    public CausalityBulletEntity(Level level, LivingEntity owner) {
        super(EntityRegistry.CAUSALITY_BULLET.get(),
                owner.getX(), owner.getEyeY() - 0.15, owner.getZ(), level);
        setOwner(owner);
    }

    /**
     * 26.3 Entity 约定：基类数据（名称/隐形/重力标记等）已由 Entity 构造器填充进 builder，
     * 本实体无额外需要客户端同步的数据，空实现即可（同官方 ThrowableItemProjectile 模式，
     * 不调 super——整条继承链上没有可调的实现）。
     */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected float getAirDrag() {
        // 射线是规则级能量体：几乎无空气阻力，保持手枪的平直弹道
        return 0.99F;
    }

    @Override
    protected double getDefaultGravity() {
        // 射线不受重力牵引（因果已定，轨迹不改）
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        // 飞行尾迹：绿金小星铺底 + END_ROD 白色亮芯，清晰而不廉价
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.WAX_ON,
                    getX(), getY(), getZ(),
                    3, 0.02, 0.02, 0.02, 0.0);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    getX(), getY(), getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /**
     * 命中实体的结算分支（先于 {@link #onHit} 由 Projectile 分派调用）。
     *
     * <p>「平行宇宙同位体」规则入口：移交 {@link MultiverseCloneService#tryResolve}——
     * 普通 Mob 被拉出同位体互殴（无掉落防刷、时限消散），
     * 玩家与 Boss 改判湮灭射线，同位体自身不可再被命中（防套娃）。
     */
    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level() instanceof ServerLevel serverLevel
                && result.getEntity() instanceof LivingEntity target) {
            LivingEntity shooter = getOwner() instanceof LivingEntity owner ? owner : null;
            MultiverseCloneService.tryResolve(serverLevel, target, shooter);
        }
    }

    /**
     * 命中任何目标（实体/方块）的统一视觉入口。
     *
     * <p>结算逻辑已在 {@link #onHitEntity} 完成；这里只负责命中特效：
     * 绿色粒子爆发 + 音爆声，随后子弹本体消散。
     */
    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel serverLevel) {
            // 命中：湮灭冲击波 + 电光迸射 + 亮芯环绕（替代廉价尘埃爆发）
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                    getX(), getY(0.5), getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    getX(), getY(0.5), getZ(),
                    24, 0.3, 0.3, 0.3, 0.15);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    getX(), getY(0.5), getZ(),
                    12, 0.2, 0.2, 0.2, 0.05);
            serverLevel.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5F, 1.8F);
        }
        this.discard();
    }
}
