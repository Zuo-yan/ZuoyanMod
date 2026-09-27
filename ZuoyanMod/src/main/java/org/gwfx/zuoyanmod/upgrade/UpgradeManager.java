package org.gwfx.zuoyanmod.upgrade;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gwfx.zuoyanmod.Config;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.effect.EffectRegistry;
import org.gwfx.zuoyanmod.entity.PrimordialBlackHoleEntity;
import org.gwfx.zuoyanmod.network.ModToastPacket;
import org.gwfx.zuoyanmod.network.UpgradeSyncPacket;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.List;
import java.util.UUID;

/**
 * 经验升级系统的服务端权威逻辑：升级扣经验、选定天赋、四个终极天赋的效果本体、
 * 属性重算与客户端同步。客户端只发意图（三个 C2S 包），一切校验在这里。
 *
 * <h2>抓取的"两段式"实现</h2>
 * 拽拉与冰冻都做成服务端 tick 推进的登记表（{@link #GRAPPLES} / {@link #FREEZES}），
 * 和 {@code VacuumDecayBlackHoleManager} 同款套路——短生命周期、随缘清理，
 * 值不起一套真实体：拽拉期间每 tick 改写目标速度（{@code hurtMarked = true} 强制把
 * 速度同步给客户端，否则玩家型目标只会原地飘），到达身边后转入冰冻段。
 */
public final class UpgradeManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 拽拉阶段把目标"吸"向施术者的速度上限（格/tick）。 */
    private static final double GRAPPLE_MAX_SPEED = 1.6D;
    /** 距离足够近（≤ 此值）即视为"已拉到身边"，转入冰冻。 */
    private static final double GRAPPLE_ARRIVE_DISTANCE = 2.2D;
    /** 拽拉最长持续：40 tick 后无论拉没拉到都就地冻结（防卡墙死循环）。 */
    private static final int GRAPPLE_MAX_TICKS = 40;
    /** 绝对零度冰冻时长：5 秒。 */
    private static final int FREEZE_TICKS = 5 * 20;
    /** 击飞初速：垂直分量（1.6 × 1.5）。原版摔落伤害按实际下落高度结算，不用我们补刀。 */
    private static final double LAUNCH_UPWARD = 2.4D;
    /** 分子离解·灌能的待命窗口：30 秒。 */
    public static final int DISSOCIATION_ARM_TICKS = 30 * 20;
    /** 分子离解效果的施加时长：3 分钟（效果本体每秒 4 点真伤）。 */
    public static final int DISSOCIATION_DURATION_TICKS = 3 * 60 * 20;

    /** 一段进行中的拽拉 */
    private record Grapple(ResourceKey<Level> dimension, UUID caster, UUID target, int ticksLeft) {}
    /** 一段进行中的绝对零度冰冻 */
    private record Freeze(ResourceKey<Level> dimension, UUID target, int ticksLeft) {}

    private static final List<Grapple> GRAPPLES = new ArrayList<>();
    private static final List<Freeze> FREEZES = new ArrayList<>();

    private UpgradeManager() {}

    /**
     * 服务端改写实体速度。旧版靠 {@code hurtMarked = true} 触发同步，26.3 里那个字段
     * 收归 protected {@code markHurt()}——对生物，速度由服务端模拟 + ServerEntity 跟踪同步；
     * 对玩家型目标必须再直发一次 motion 包（原版爆炸就是这么做击退的），否则客户端不认。
     */
    private static void applyVelocity(Entity target, Vec3 velocity) {
        target.setDeltaMovement(velocity);
        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
        }
    }

    // ===== 升级 =====

    public static void upgradeStat(ServerPlayer player, int statIndex) {
        LOGGER.info("[Upgrade] {} 请求升级 statIndex={} (经验 {} 级)", player.getName().getString(),
                statIndex, player.experienceLevel);
        if (!Config.upgradeEnabled || statIndex < 0 || statIndex >= UpgradeType.VALUES.length) {
            LOGGER.info("[Upgrade] 拒绝:系统关闭或下标非法");
            return;
        }
        UpgradeData data = UpgradeData.of(player);
        UpgradeType type = UpgradeType.VALUES[statIndex];
        int level = data.level(statIndex);
        if (level >= UpgradeType.MAX_LEVEL) {
            LOGGER.info("[Upgrade] 拒绝:{}已满级", type);
            feedback(player, Component.translatable("message.zuoyanmod.upgrade.maxed",
                    Component.translatable(type.nameKey())));
            return;
        }
        int cost = UpgradeType.xpCost(level);
        if (player.experienceLevel < cost) {
            LOGGER.info("[Upgrade] 拒绝:经验不足,需要 {} 级", cost);
            feedback(player, Component.translatable("message.zuoyanmod.upgrade.no_xp", cost));
            return;
        }
        player.giveExperienceLevels(-cost);
        data.setLevel(statIndex, level + 1);
        applyAttributes(player);
        sync(player);
        LOGGER.info("[Upgrade] 成功:{} → Lv.{} (花费 {} 级经验)", type, level + 1, cost);
        feedback(player, Component.translatable("message.zuoyanmod.upgrade.level_up",
                Component.translatable(type.nameKey()), level + 1));
    }

    // ===== 选定终极天赋 =====

    public static void chooseTalent(ServerPlayer player, int talentIndex) {
        if (talentIndex < 0 || talentIndex >= UltimateTalent.VALUES.length) {
            return;
        }
        UpgradeData data = UpgradeData.of(player);
        if (data.talent() != null) {
            feedback(player, Component.translatable("message.zuoyanmod.upgrade.talent_taken"));
            return;
        }
        if (!data.allMaxed()) {
            feedback(player, Component.translatable("message.zuoyanmod.upgrade.not_maxed"));
            return;
        }
        UltimateTalent talent = UltimateTalent.VALUES[talentIndex];
        data.setTalent(talent);
        sync(player);
        LOGGER.info("[Upgrade] {} 选定终极天赋 {}", player.getName().getString(), talent);
        player.sendSystemMessage(Component.translatable("message.zuoyanmod.upgrade.talent_chosen",
                Component.translatable(talent.nameKey())));
    }

    // ===== 触发终极天赋（Y 键）=====

    public static void triggerUltimate(ServerPlayer player) {
        if (!Config.upgradeEnabled) {
            return;
        }
        UpgradeData data = UpgradeData.of(player);
        UltimateTalent talent = data.talent();
        if (talent == null) {
            feedback(player, Component.translatable("message.zuoyanmod.upgrade.no_talent"));
            return;
        }
        long gameTime = player.level().getGameTime();
        if (data.isOnCooldown(talent, gameTime)) {
            long seconds = (data.cooldownRemainingTicks(talent, gameTime) + 19) / 20;
            feedback(player, Component.translatable("message.zuoyanmod.upgrade.cooldown", seconds));
            return;
        }
        boolean executed = switch (talent) {
            case GRAPPLE_ABSOLUTE_ZERO -> executeGrapple(player);
            case SKY_LAUNCH -> executeLaunch(player);
            case MOLECULAR_DISSOCIATION -> executeDissociationArm(player, data, gameTime);
            case PRIMORDIAL_BLACK_HOLE -> executeBlackHole(player);
        };
        // 失败（没抓到目标 / 黑洞上限）不进冷却，玩家可以立刻重试
        if (executed) {
            data.startCooldown(talent, gameTime);
            sync(player);
            LOGGER.info("[Upgrade] {} 触发终极天赋 {} 成功", player.getName().getString(), talent);
        } else {
            LOGGER.info("[Upgrade] {} 触发终极天赋 {} 失败(不进冷却)", player.getName().getString(), talent);
        }
    }

    // ===== 天赋一：绝对零度·抓取 =====

    private static boolean executeGrapple(ServerPlayer player) {
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(player,
                e -> e instanceof LivingEntity living && living.isAlive() && !living.isSpectator(),
                Config.upgradeGrappleRange);
        if (!(hit instanceof EntityHitResult entityHit)
                || !(entityHit.getEntity() instanceof LivingEntity target)) {
            feedback(player, Component.translatable("message.zuoyanmod.upgrade.grapple_no_target"));
            return false;
        }
        // 目标可能自己在动，拽拉交给每 tick 的登记表推进；距离远时第一 tick 就能冲出去大半程
        GRAPPLES.add(new Grapple(player.level().dimension(), player.getUUID(), target.getUUID(),
                GRAPPLE_MAX_TICKS));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.6F, 1.6F);
        return true;
    }

    private static void tickGrapples(MinecraftServer server, ListIterator<Grapple> it) {
        Grapple grapple = it.next();
        ServerLevel level = server.getLevel(grapple.dimension());
        Player caster = grapple.caster() != null ? server.getPlayerList().getPlayer(grapple.caster()) : null;
        Entity target = level != null ? level.getEntity(grapple.target()) : null;
        boolean done = grapple.ticksLeft() <= 1
                || caster == null
                || target == null || !target.isAlive();
        if (!done) {
            // 每tick把目标速度改写成"冲向施术者眼睛"，hurtMarked 强制同步给客户端
            Vec3 toCaster = caster.getEyePosition().subtract(target.position());
            double distance = toCaster.length();
            if (distance <= GRAPPLE_ARRIVE_DISTANCE) {
                done = true;
            } else {
                double speed = Math.min(GRAPPLE_MAX_SPEED, Math.max(0.7D, distance * 0.35D));
                applyVelocity(target, toCaster.normalize().scale(speed));
                if (level != null && grapple.ticksLeft() % 4 == 0) {
                    level.sendParticles(ParticleTypes.SNOWFLAKE,
                            target.getX(), target.getY() + target.getBbHeight() / 2.0D, target.getZ(),
                            4, 0.25D, 0.4D, 0.25D, 0.01D);
                }
            }
        }
        if (done) {
            it.remove();
            if (target != null && target.isAlive() && level != null) {
                startFreeze(level, target);
            }
        } else {
            it.set(new Grapple(grapple.dimension(), grapple.caster(), grapple.target(),
                    grapple.ticksLeft() - 1));
        }
    }

    /** 到达身边（或超时）：就地绝对零度冰冻 5 秒。 */
    private static void startFreeze(ServerLevel level, Entity target) {
        FREEZES.add(new Freeze(level.dimension(), target.getUUID(), FREEZE_TICKS));
        if (target instanceof LivingEntity living) {
            // 缓慢 X：横向位移归零；跳跃由 tick 段的运动清零兜住
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, FREEZE_TICKS, 9, false, true));
        }
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 0.8F, 1.4F);
    }

    private static void tickFreezes(MinecraftServer server, ListIterator<Freeze> it) {
        Freeze freeze = it.next();
        ServerLevel level = server.getLevel(freeze.dimension());
        Entity target = level != null ? level.getEntity(freeze.target()) : null;
        if (freeze.ticksLeft() <= 1 || target == null || !target.isAlive()) {
            it.remove();
            return;
        }
        // 每 tick 顶住速度：解冻前目标动不了（也被动免疫击退）
        applyVelocity(target, Vec3.ZERO);
        if (target instanceof LivingEntity living) {
            living.setTicksFrozen(living.getTicksRequiredToFreeze());
        }
        if (freeze.ticksLeft() % 5 == 0 && level != null) {
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    target.getX(), target.getY() + target.getBbHeight() / 2.0D, target.getZ(),
                    6, 0.35D, 0.6D, 0.35D, 0.02D);
        }
        it.set(new Freeze(freeze.dimension(), freeze.target(), freeze.ticksLeft() - 1));
    }

    // ===== 天赋二：天罚·击飞 =====

    private static boolean executeLaunch(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        double radius = Config.upgradeLaunchRadius;
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive() && !e.isSpectator());
        if (targets.isEmpty()) {
            feedback(player, Component.translatable("message.zuoyanmod.upgrade.launch_no_target"));
            return false;
        }
        for (LivingEntity target : targets) {
            Vec3 away = target.position().subtract(player.position());
            Vec3 horizontal = new Vec3(away.x, 0.0D, away.z);
            if (horizontal.lengthSqr() < 1.0E-4D) {
                horizontal = new Vec3((player.getRandom().nextDouble() - 0.5D) * 2.0D, 0.0D,
                        (player.getRandom().nextDouble() - 0.5D) * 2.0D);
            }
            Vec3 scatter = horizontal.normalize().scale(0.35D);
            applyVelocity(target, new Vec3(scatter.x, LAUNCH_UPWARD, scatter.z));
            target.fallDistance = 0.0D; // 摔落伤害从最高点重新累计
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 2 * 60 * 20, 2, false, true));
        }
        level.sendParticles(ParticleTypes.EXPLOSION,
                player.getX(), player.getY() + 1.0D, player.getZ(), 2, 0.5D, 0.5D, 0.5D, 0.0D);
        level.sendParticles(ParticleTypes.CLOUD,
                player.getX(), player.getY() + 0.5D, player.getZ(), 30, radius * 0.4D, 0.5D, radius * 0.4D, 0.08D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9F, 1.2F);
        return true;
    }

    // ===== 天赋三：分子离解·灌能 =====

    private static boolean executeDissociationArm(ServerPlayer player, UpgradeData data, long gameTime) {
        data.armDissociation(gameTime, DISSOCIATION_ARM_TICKS);
        player.sendSystemMessage(Component.translatable("message.zuoyanmod.upgrade.dissociation_armed"));
        return true;
    }

    /**
     * 伤害事件里调（{@code UpgradeServerEvents}）：灌能中的玩家近战命中 → 消耗灌能，
     * 给目标挂 3 分钟分子离解。返回 false 表示没有灌能或没命中条件，事件处理器不用管。
     */
    public static boolean consumeDissociation(ServerPlayer attacker, LivingEntity target) {
        UpgradeData data = UpgradeData.of(attacker);
        long gameTime = attacker.level().getGameTime();
        if (!data.dissociationArmed(gameTime)) {
            return false;
        }
        data.consumeDissociation();
        target.addEffect(new MobEffectInstance(EffectRegistry.MOLECULAR_DISSOLUTION,
                DISSOCIATION_DURATION_TICKS, 0, false, true));
        sync(attacker);
        attacker.sendSystemMessage(Component.translatable("message.zuoyanmod.upgrade.dissociation_hit"));
        return true;
    }

    // ===== 天赋四：原始黑洞 =====

    private static boolean executeBlackHole(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        MinecraftServer server = level.getServer();
        // 规则与「原始黑洞」物品完全同款：一人一个 + 全局上限，提示也复用它的 lang 键
        if (PrimordialBlackHoleEntity.hasActiveBlackHole(server, player.getUUID())) {
            player.sendSystemMessage(Component.translatable(
                    "message.zuoyanmod.primordial_black_hole.already_active"));
            return false;
        }
        if (PrimordialBlackHoleEntity.countActive(server) >= PrimordialBlackHoleEntity.MAX_ACTIVE_HOLES) {
            player.sendSystemMessage(Component.translatable(
                    "message.zuoyanmod.primordial_black_hole.limit_reached"));
            return false;
        }
        PrimordialBlackHoleEntity.spawn(level, player, resolveImpact(player));
        return true;
    }

    /**
     * 落点计算，逐行复刻 {@code PrimordialBlackHoleItem#resolveImpact}（24 格射线 +
     * 0.6 格表面外推），保证天赋召唤的黑洞和物品丢出去的落点手感一致。
     */
    private static Vec3 resolveImpact(Player player) {
        Vec3 eye = player.getEyePosition();
        HitResult hit = player.pick(24.0D, 0.0F, false);
        Vec3 point = hit.getLocation();
        if (hit.getType() == HitResult.Type.MISS) {
            return point;
        }
        Vec3 direction = point.subtract(eye);
        if (direction.lengthSqr() < 1.0E-6D) {
            return point;
        }
        return point.add(direction.normalize().scale(0.6D));
    }

    // ===== 属性重算 =====

    /**
     * 按 {@link UpgradeData} 的等级把 4 条属性 modifier 重算一遍（减伤不是属性，
     * 在伤害事件里算）。登录、重生、每次升级都会调用；transient modifier 不入库，
     * 数据本体在 Attachment 里，重登/重生后靠这里恢复。
     */
    public static void applyAttributes(Player player) {
        UpgradeData data = UpgradeData.of(player);
        for (UpgradeType type : UpgradeType.VALUES) {
            if (type.attribute == null || type.operation == null) {
                continue;
            }
            AttributeInstance instance = player.getAttribute(type.attribute);
            if (instance == null) {
                continue;
            }
            instance.removeModifier(modifierId(type));
            int level = data.level(type.ordinal());
            if (level > 0) {
                instance.addTransientModifier(new AttributeModifier(modifierId(type),
                        type.totalBonus(level), type.operation));
            }
        }
        // 配置被调小（或 modifier 意外丢失）时把血量夹回上限
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static Identifier modifierId(UpgradeType type) {
        return Identifier.fromNamespaceAndPath(Zuoyanmod.MODID,
                "upgrade_" + type.name().toLowerCase(java.util.Locale.ROOT));
    }

    // ===== 退还加点 =====

    /**
     * 右键退还某条能力的最后一级：退回当年买它花的经验，属性随之重算。
     * 随时随地可退，没有手续费——加点方案可以随便试。
     */
    public static void refundStat(ServerPlayer player, int statIndex) {
        if (!Config.upgradeEnabled || statIndex < 0 || statIndex >= UpgradeType.VALUES.length) {
            return;
        }
        UpgradeData data = UpgradeData.of(player);
        UpgradeType type = UpgradeType.VALUES[statIndex];
        int level = data.level(statIndex);
        if (level <= 0) {
            LOGGER.info("[Upgrade] {} 退还未果:{}没有已加的点", player.getName().getString(), type);
            feedback(player, Component.translatable("message.zuoyanmod.upgrade.refund_empty",
                    Component.translatable(type.nameKey())));
            return;
        }
        int refund = UpgradeType.xpCost(level - 1);
        player.giveExperienceLevels(refund);
        data.setLevel(statIndex, level - 1);
        applyAttributes(player);
        sync(player);
        LOGGER.info("[Upgrade] {} 退还 {} Lv.{} → Lv.{} (返还 {} 级经验)",
                player.getName().getString(), type, level, level - 1, refund);
        feedback(player, Component.translatable("message.zuoyanmod.upgrade.refund",
                refund, Component.translatable(type.nameKey()), level - 1));
    }

    // ===== 终极天赋重置（道具）=====

    /**
     * 「天赋重置卷轴」的右键效果：清空已选终极天赋（冷却/灌能一并归零），
     * 玩家可以重新选一个。基础能力的加点不受影响。
     *
     * @return true = 重置成功（调用方据此扣道具）；false = 本来就没选天赋（不扣）
     */
    public static boolean resetTalent(ServerPlayer player) {
        UpgradeData data = UpgradeData.of(player);
        UltimateTalent current = data.talent();
        if (current == null) {
            feedback(player, Component.translatable("message.zuoyanmod.talent_reset.no_talent"));
            return false;
        }
        data.clearTalent();
        sync(player);
        LOGGER.info("[Upgrade] {} 使用重置卷轴,清空终极天赋 {}(可重新选择)",
                player.getName().getString(), current);
        player.sendSystemMessage(Component.translatable("message.zuoyanmod.talent_reset.done"));
        return true;
    }

    // ===== 冷却完毕 toast =====

    /**
     * 每 tick 轮询在线玩家的天赋冷却，结束瞬间用模组专属 toast 提示「冷却完毕」
     * （HUD 不再常驻冷却显示；玩家按 Y 也能随时主动查冷却）。
     */
    public static void pollReadyToasts(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UltimateTalent ready = UpgradeData.of(player).pollReadyAnnouncement(
                    player.level().getGameTime());
            if (ready != null) {
                ModToastPacket.send(player, Component.translatable("toast.zuoyanmod.upgrade.ready",
                        Component.translatable(ready.nameKey())));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 1.6F);
            }
        }
    }

    // ===== tick 与同步 =====

    /** {@code UpgradeServerEvents} 的 ServerTickEvent.Post 里调。 */
    public static void tick(MinecraftServer server) {
        if (!GRAPPLES.isEmpty()) {
            tickGrapples(server, GRAPPLES.listIterator());
        }
        if (!FREEZES.isEmpty()) {
            tickFreezes(server, FREEZES.listIterator());
        }
    }

    /** 把档案快照发给客户端（升级/选天赋/触发/登录后都发）。 */
    public static void sync(ServerPlayer player) {
        UpgradeSyncPacket.send(player, UpgradeData.of(player).toSyncPacket());
    }

    /** actionbar 反馈：不打断正在做的事，也不刷聊天栏。 */
    private static void feedback(ServerPlayer player, Component message) {
        player.sendOverlayMessage(message);
    }
}
