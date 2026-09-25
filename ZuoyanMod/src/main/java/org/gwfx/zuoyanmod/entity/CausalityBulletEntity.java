package org.gwfx.zuoyanmod.entity;

import net.minecraft.core.particles.ParticleTypes;
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
 *
 * <p><b>1.20.1 适配</b>：
 * <ul>
 *   <li>{@code defineSynchedData()}：26.3 的签名收 {@code SynchedEntityData.Builder}
 *       且整条继承链上无实现可 super；1.20.1 是<b>无参</b>的
 *       {@code protected abstract void defineSynchedData()}。
 *       本实体无自定义同步数据，覆写成空方法体即可——这一点比 26.3 更安全：
 *       1.20.1 的 {@code Entity} 构造器里已经把基础 Accessor（shared flags / air /
 *       custom name / pose…）逐条 {@code entityData.define()} 掉了，
 *       空实现<b>不会</b>漏掉任何基类数据（26.3 那边同理，注释保留以说明差异）。</li>
 *   <li>重力：26.3 的 {@code getDefaultGravity()} 在 1.20.1 是
 *       {@code protected float getGravity()}（返回 0.0，射线不受重力牵引）。</li>
 *   <li>空气阻力：26.3 覆写 {@code getAirDrag()} 返回 0.99；1.20.1 <b>没有这个钩子</b>，
 *       {@code ThrowableProjectile#tick} 内部本来就写死 {@code f = 0.99F}（水中 0.8F），
 *       数值与主线一致，所以直接删掉该覆写即可，行为完全等价。</li>
 *   <li>其余（尾迹粒子种类/数量、命中判定、消散流程）与主线逐行一致。</li>
 * </ul>
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
     * 本实体无额外需要客户端同步的数据，空实现即可。
     *
     * <p>与 26.3 的差异见类注释：1.20.1 这个方法无参，且基类数据在 Entity 构造器里已经定义好，
     * 因此这里不（也不能）调 super——{@code Entity#defineSynchedData} 是抽象方法。
     */
    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected float getGravity() {
        // 射线不受重力牵引（因果已定，轨迹不改）
        return 0.0F;
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
