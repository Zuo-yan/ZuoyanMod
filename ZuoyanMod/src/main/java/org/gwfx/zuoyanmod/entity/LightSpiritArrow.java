package org.gwfx.zuoyanmod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;

/**
 * 赫拉克勒斯之弓射出的光灵箭：
 * - 由神圣光灵凝聚而成，不消耗任何真实箭矢；
 * - 无法被任何玩家拾取（拾取返还的也不是弓）；
 * - 飞行时拖曳光灵尾迹，落地后化作光尘消散。
 *
 * <p>复用原版 {@code EntityTypes.ARROW}（客户端自动按原版箭渲染），因此无需注册实体类型。</p>
 *
 * <p>1.20.1 适配：{@code Arrow} 在 1.20.1 位于 {@code world.entity.projectile}（26.x 挪到了
 * {@code ...projectile.arrow}），构造只有 {@code (Level, LivingEntity)}；
 * "是否落地"读的是父类的 {@code inGround} 字段（1.20.1 没有 isInGround()）。</p>
 */
public class LightSpiritArrow extends Arrow {

    public LightSpiritArrow(Level level, LivingEntity owner) {
        super(level, owner);
        // 构造时 setOwner 会把 DISALLOWED 提升为 ALLOWED（可拾取），
        // 改用 CREATIVE_ONLY：不会被 setOwner 流程改回，也不会在命中时掉落物品；
        // 配合 tryPickup 双重保险，光灵箭永远无法变成玩家背包里的东西。
        this.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
    }

    /** 光灵箭是纯粹的能量体，任何人都无法拾取 */
    @Override
    protected boolean tryPickup(Player player) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.inGround) {
                // 落地：化作光尘消散，不留任何残骸
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        this.getX(), this.getY(0.5), this.getZ(),
                        10, 0.2, 0.2, 0.2, 0.05);
                this.discard();
            } else if (this.tickCount % 2 == 0) {
                // 飞行：光灵尾迹
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        this.getX(), this.getY(), this.getZ(),
                        1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }
}
