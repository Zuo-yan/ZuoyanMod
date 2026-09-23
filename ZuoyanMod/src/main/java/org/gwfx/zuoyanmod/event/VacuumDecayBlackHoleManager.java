package org.gwfx.zuoyanmod.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.damage.VacuumDecayDamageSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * 「对称破缺」产生的真空衰变泡（玩家看到的就是一个黑洞）。服务端权威，两段式：
 * <ol>
 *   <li><b>牵引</b>（{@link #LIFETIME_TICKS} tick / 5 秒）：每 tick 把 {@link #PULL_RADIUS}
 *       内的所有实体往中心拽。**施术者自己不被牵引**（"除自身以外"）。</li>
 *   <li><b>坍缩爆炸</b>：时间到 → 对 {@link #BLAST_RADIUS} 内的活体各造成
 *       {@link #BLAST_DAMAGE} 点伤害，**同样不打施术者**，然后场消失。</li>
 * </ol>
 * 牵引为什么要写 {@code syncVelocity}：{@code Entity.push} 在 26.3 只置 {@code needsSync}
 * （位置同步），速度变更必须额外置 {@code entity.syncVelocity = true}，服务端才会补发
 * {@code ClientboundSetEntityMotionPacket}（见 {@code ServerEntity.sendChanges}）。
 * 玩家是客户端权威的，漏了这一步就只有生物会被吸过来、玩家纹丝不动。
 */
@EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class VacuumDecayBlackHoleManager {

    /** 聚怪半径：以黑洞为中心 16 格内的实体都会被拽过来 */
    public static final double PULL_RADIUS = 16.0D;
    /** 黑洞存续时长：5 秒 */
    public static final int LIFETIME_TICKS = 5 * 20;
    /** 爆炸伤害半径 */
    public static final double BLAST_RADIUS = 8.0D;
    /** 爆炸伤害：20 点（普通伤害，吃护甲减免） */
    public static final float BLAST_DAMAGE = 20.0F;

    /** 牵引基础加速度（格/tick），在半径边缘处生效 */
    private static final double PULL_ACCEL_BASE = 0.09D;
    /** 越靠近中心越强的附加加速度 */
    private static final double PULL_ACCEL_BONUS = 0.20D;
    /** 死区：进入这个距离就不再加速，避免实体在中心疯狂抖动 */
    private static final double PULL_DEAD_ZONE = 0.9D;

    /**
     * 同时存在的黑洞上限。纯安全阀：一个黑洞每 tick 要遍历半径 16 内的全部实体，
     * 十几个叠加就是可观的卡顿源。
     */
    private static final int MAX_ACTIVE_HOLES = 8;

    private record Hole(ResourceKey<Level> dimension, Vec3 center, UUID caster, long expiresAt) {}

    private static final List<Hole> HOLES = new ArrayList<>();

    private VacuumDecayBlackHoleManager() {}

    // ===== 对外：开一个黑洞 =====

    public static void spawn(ServerLevel level, Vec3 center, Player caster) {
        if (HOLES.size() >= MAX_ACTIVE_HOLES) {
            HOLES.remove(0); // 顶掉最老的一个，保证不会无限堆积
        }
        HOLES.add(new Hole(level.dimension(), center, caster.getUUID(),
                level.getGameTime() + LIFETIME_TICKS));

        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 1.4F, 0.42F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 0.8F, 0.6F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                center.x, center.y, center.z, 80, 3.0D, 3.0D, 3.0D, 0.6D);
        caster.sendSystemMessage(Component.literal("§d对称破缺 §7- 真空衰变泡展开 §8(5s)"));
    }

    // ===== 每 tick 维持 =====

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (HOLES.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();

        Iterator<Hole> iterator = HOLES.iterator();
        while (iterator.hasNext()) {
            Hole hole = iterator.next();
            ServerLevel level = server.getLevel(hole.dimension());
            if (level == null) {
                iterator.remove();
                continue;
            }
            if (hole.expiresAt() <= now) {
                collapse(server, level, hole);
                iterator.remove();
                continue;
            }
            pull(level, hole, now);
        }
    }

    private static void pull(ServerLevel level, Hole hole, long now) {
        Vec3 center = hole.center();
        AABB zone = box(center, PULL_RADIUS);
        boolean visualTick = now % 2L == 0L;

        for (Entity entity : level.getEntitiesOfClass(Entity.class, zone)) {
            // 施术者是参照点，不被牵引（"将除自身以外的所有实体聚集到一起"）
            if (entity.getUUID().equals(hole.caster()) || entity.isSpectator()) {
                continue;
            }
            Vec3 delta = center.subtract(entity.position());
            double distance = delta.length();
            if (distance < PULL_DEAD_ZONE) {
                continue;
            }
            // 越近拉得越狠，但方向始终指向中心 → 形成"卷进去"的手感
            double accel = PULL_ACCEL_BASE + PULL_ACCEL_BONUS * (1.0D - Math.min(1.0D, distance / PULL_RADIUS));
            Vec3 pull = delta.scale(accel / distance);
            entity.push(pull.x, pull.y, pull.z);
            // 26.3：push 只同步位置，速度要额外打这个标记才会发 SetEntityMotion 包
            entity.syncVelocity = true;

            if (visualTick) {
                level.sendParticles(ParticleTypes.PORTAL,
                        entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                        2, 0.25D, 0.3D, 0.25D, 0.1D);
            }
        }

        if (visualTick) {
            level.sendParticles(ParticleTypes.PORTAL,
                    center.x, center.y, center.z, 10, 1.6D, 1.6D, 1.6D, 0.4D);
            level.sendParticles(ParticleTypes.SQUID_INK,
                    center.x, center.y, center.z, 3, 1.2D, 1.2D, 1.2D, 0.02D);
        }
    }

    private static void collapse(MinecraftServer server, ServerLevel level, Hole hole) {
        Vec3 center = hole.center();
        Player caster = server.getPlayerList().getPlayer(hole.caster());
        DamageSource source = VacuumDecayDamageSource.create(level, caster);

        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box(center, BLAST_RADIUS))) {
            // 施术者不吃自己的爆炸
            if (living.getUUID().equals(hole.caster())) {
                continue;
            }
            living.hurtServer(level, source, BLAST_DAMAGE);
        }

        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.8F, 0.8F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 60, 2.0D, 2.0D, 2.0D, 0.9D);

        if (caster != null) {
            caster.sendSystemMessage(Component.literal("§d对称破缺 §7- 真空衰变泡坍缩"));
        }
    }

    private static AABB box(Vec3 center, double radius) {
        return new AABB(center.subtract(radius, radius, radius), center.add(radius, radius, radius));
    }
}
