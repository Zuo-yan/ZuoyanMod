package org.gwfx.zuoyanmod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.jetbrains.annotations.Nullable;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.damage.MultiverseRayDamageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「平行宇宙同位体」的生成与生命周期服务。
 *
 * <p>核心规则（因果律手枪）：
 * <ul>
 *   <li>命中普通 Mob：以 NBT 快照克隆目标本体（继承血量比例与装备），注入
 *       {@link #CLONE_TAG} 标记，仇恨互设（本体与同位体互相锁定为对方目标），
 *       互殴 {@link #CLONE_LIFETIME_TICKS} 后同位体化作粒子消散；</li>
 *   <li>防套娃：目标已带 {@link #CLONE_TAG} 的（本身是同位体），再次命中无效，仅粒子反馈；</li>
 *   <li>防刷物：同位体的掉落与经验由事件层（MultiverseCloneEventHandler）统一拦截；</li>
 *   <li>湮灭射线：命中 {@code zuoyanmod:bosses} 标签内的实体（末影龙/凋灵/远古守卫者等）
 *       或玩家（无仇恨系统、无法召唤同位体的目标）时，改判一道无视护甲的巨额
 *       湮灭射线（{@value #RAY_DAMAGE}，一击湮灭任何原版 Boss），伴随高能音效与粒子。</li>
 * </ul>
 *
 * <p>仇恨锁定的三道保障（26.3 实战验证的坑）：
 * <ol>
 *   <li>生成时移除快照中的 {@link LivingEntity#TAG_BRAIN} 字段——否则同位体会继承本体
 *       对玩家的仇恨记忆（Brain 序列化了 ATTACK_TARGET），出生即转头打射手；</li>
 *   <li>每 tick 由 {@link #tickClones} 强制维持互设目标——vanilla 的
 *       {@code NearestAttackableTargetGoal} 等索敌 AI 会周期性重选目标（通常是玩家），
 *       一次性 {@code setTarget} 会被覆盖，必须周期性刷回；</li>
 *   <li>目标写入走 {@link #lockTarget} 双轨（字段 + 脑记忆）——26.3 生物 AI 分
 *       Goal 系（僵尸/卫道士，读 {@code Mob.target} 字段）与 Brain 系（猪灵/疣猪兽，
 *       读 {@code ATTACK_TARGET} 脑记忆）两套体系，只写一边必有一半生物不互殴。</li>
 * </ol>
 *
 * <p>26.3 实现要点：实体克隆走 NBT 快照法——{@code Entity.copy()} 在 26.3 已不存在，
 * 用 {@link TagValueOutput}/{@link TagValueInput} 序列化管道（替代旧版 CompoundTag 直存）
 * 完整复制目标状态（装备、药水效果、鞍、血量等一网打尽）；
 * 快照中移除 {@link Entity#TAG_UUID} 字段避免克隆体与本体 UUID 冲突（"移除旧 UUID"）。
 *
 * <p>粒子语言：统一"平行宇宙撕裂"主题——裂隙电光（ELECTRIC_SPARK）、
 * 能量流光（TOTEM_OF_UNDYING）、亮白芯（END_ROD）、冲击波（SONIC_BOOM）。
 */
public final class MultiverseCloneService {

    /** 克隆体标记：注入 entityTags（26.3 中 getTags 改名为 entityTags） */
    public static final String CLONE_TAG = "zuoyanmod:multiverse_clone";

    /** 同位体存活时长：300 ticks = 15 秒 */
    public static final int CLONE_LIFETIME_TICKS = 300;

    /**
     * 湮灭射线伤害：命中玩家/Boss 的统一结算，无视护甲（damage_type 带 bypasses_armor）。
     * 数值刻意拉满——一击湮灭任何原版 Boss（凋灵 300 血也扛不住）。
     */
    public static final float RAY_DAMAGE = 500.0F;

    /** Boss 判定标签：zuoyanmod:bosses（data/zuoyanmod/tags/entity_type/bosses.json） */
    private static final TagKey<EntityType<?>> BOSSES_TAG = TagKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "bosses")
    );

    /**
     * 活跃克隆登记：克隆体 UUID → 登记项（本体 UUID + 到期时刻）。
     * 用 UUID 而非实体引用，避免长期持有实体导致的内存泄漏与卸载维度泄漏；
     * 每条记录同时承担「互殴维持」与「到期消散」两个职责，由 {@link #tickClones} 驱动。
     */
    private record ActiveClone(UUID originId, long expireAtTick) {}

    private static final Map<UUID, ActiveClone> ACTIVE_CLONES = new ConcurrentHashMap<>();

    /** 临时调试日志（定位克隆仇恨失效用，问题解决后删除） */
    private static final Logger DEBUG_LOG = LoggerFactory.getLogger("MultiverseCloneDebug");

    private MultiverseCloneService() {}

    /**
     * 子弹命中实体的结算总入口。
     *
     * @param level   服务端世界
     * @param target  命中的目标
     * @param shooter 射手（可空：发射器等非生物射手降级为无归因）
     */
    public static void tryResolve(ServerLevel level, LivingEntity target,
                                  @Nullable LivingEntity shooter) {
        // 防套娃：目标本身已是同位体 → 无效，仅电光裂隙反馈（示意"因果已用尽"）
        if (target.entityTags().contains(CLONE_TAG)) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getY(0.5), target.getZ(),
                    16, 0.4, 0.5, 0.4, 0.15);
            level.sendParticles(ParticleTypes.SONIC_BOOM,
                    target.getX(), target.getY(0.5), target.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
            return;
        }

        // 湮灭射线分支：Boss 与玩家（无仇恨系统的目标）无法召唤同位体，
        // 改判一道无视护甲的巨额湮灭射线
        if (isBoss(target) || !(target instanceof Mob mob)) {
            fireAnnihilationRay(level, target, shooter);
            return;
        }

        spawnClone(level, mob);
    }

    /** Boss 检测：实体类型是否在 zuoyanmod:bosses 标签内 */
    private static boolean isBoss(LivingEntity target) {
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(target.getType()).is(BOSSES_TAG);
    }

    /**
     * 仇恨锁定（双轨写入，兼容两套 AI 体系）：
     *
     * <p>26.3 的生物 AI 分两套体系，只写任何一边都会有一半生物"不听话"：
     * <ul>
     *   <li><b>Goal 系</b>（僵尸、卫道士等经典生物）：目标存 {@code Mob.target} 字段，
     *       由 {@code setTarget} 写入，索敌 Goal 周期性重选覆盖；</li>
     *   <li><b>Brain 系</b>（猪灵、疣猪兽等新体系生物）：{@code Hoglin#getTarget} 等
     *       直接读 Brain 的 {@code ATTACK_TARGET} 记忆，{@code setTarget} 写的字段它们
     *       根本不读——这是"猪灵/疣猪兽同位体不互殴"的根因。</li>
     * </ul>
     * 因此每次锁定同时写两处：字段 + 脑记忆；顺手清掉 {@code AVOID_TARGET}
     * （疣猪兽被打怂后会进入撤退流程，撤退行为会反过来擦除攻击目标）。
     * 每天刷新由 {@link #tickClones} 每 tick 调用本方法压制两套 AI 的重选。
     */
    private static void lockTarget(Mob mob, LivingEntity target) {
        // Goal 系：写目标字段（Brain 系生物读不到这里，但写入无害）
        mob.setTarget(target);
        // Brain 系：直接写 ATTACK_TARGET 脑记忆（Goal 系生物脑里没注册该模块，空操作）
        mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        // 撤退压制：清掉逃跑记忆，避免疣猪兽一边被锁定一边逃
        mob.getBrain().eraseMemory(MemoryModuleType.AVOID_TARGET);
    }

    /**
     * 湮灭射线结算：一次无视护甲的巨额伤害（{@value RAY_DAMAGE}，足以一击湮灭任何原版 Boss）。
     * 射手在场时伤害归因到射手（击杀显示射手）；否则无归因（宁可无归因也不丢结算）。
     * 特效：音爆冲击波 + 电光迸射 + 亮芯环绕。
     */
    private static void fireAnnihilationRay(ServerLevel level, LivingEntity victim,
                                            @Nullable LivingEntity shooter) {
        DamageSource source = shooter != null
                ? MultiverseRayDamageSource.create(level, shooter)
                : MultiverseRayDamageSource.create(level);
        // 高能反馈：湮灭冲击波 + 大范围电光
        level.sendParticles(ParticleTypes.SONIC_BOOM,
                victim.getX(), victim.getY(0.5), victim.getZ(),
                1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                victim.getX(), victim.getY(0.5), victim.getZ(),
                32, 0.8, 0.8, 0.8, 0.2);
        level.sendParticles(ParticleTypes.END_ROD,
                victim.getX(), victim.getY(0.5), victim.getZ(),
                16, 0.6, 0.6, 0.6, 0.08);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2F, 0.8F);
        victim.hurtServer(level, source, RAY_DAMAGE);
    }

    /**
     * 克隆生成：NBT 快照法完整复制目标（装备、药水、血量比例随快照继承），
     * 清除快照中的仇恨记忆与旧 UUID、注入克隆标记、放到目标侧翼、仇恨互设、登记生命周期。
     */
    private static void spawnClone(ServerLevel level, Mob target) {
        // 1. 序列化快照（saveWithoutId 不写入实体 id，天然适合克隆）
        TagValueOutput out = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        target.saveWithoutId(out);
        CompoundTag snapshot = out.buildResult();

        // 2. 移除旧 UUID：否则 load 会把克隆体的 UUID 覆盖成本体的，两者同 UUID 冲突
        snapshot.remove(Entity.TAG_UUID);
        // 3. 移除 Brain 仇恨记忆：否则克隆体继承本体对玩家（射手）的 ATTACK_TARGET，
        //    出生即转头与本体一起围殴射手——这是"克隆体不打本体反打玩家"的第一根源
        snapshot.remove(LivingEntity.TAG_BRAIN);

        // 4. 反序列化为新实体（CONVERSION：由既有实体转化而来）
        ValueInput in = TagValueInput.create(
                ProblemReporter.DISCARDING, level.registryAccess(), snapshot);
        Optional<Entity> revived = EntityType.create(
                (EntityType<?>) target.getType(), in, level, EntitySpawnReason.CONVERSION);
        if (revived.isEmpty() || !(revived.get() instanceof Mob clone)) {
            return; // 反序列化失败（极端情况），宁可静默不结算也不崩服
        }

        // 5. 双保险：显式换新 UUID（构造期已随机，load 未覆盖，此处语义上再确认一次）
        clone.setUUID(UUID.randomUUID());

        // 6. 落位：目标侧翼 1.5 格，面向本体（yRot + 180）
        float yawRad = (float) Math.toRadians(target.getYRot());
        double dx = -Math.cos(yawRad) * 1.5;
        double dz = Math.sin(yawRad) * 1.5;
        clone.setPos(target.getX() + dx, target.getY(), target.getZ() + dz);
        clone.forceSetRotation(target.getYRot() + 180.0F, true, target.getXRot(), true);

        // 7. 注入克隆标记（先于 addFreshEntity，确保出生即受防掉落保护）
        clone.addTag(CLONE_TAG);

        // 8. 仇恨互设（双轨锁定）：本体锁定同位体，同位体锁定本体——平行宇宙的自己互相对冲
        lockTarget(clone, target);
        lockTarget(target, clone);

        // 9. 出生特效（绿金流光自撕裂处涌出）+ 登记生命周期
        level.addFreshEntity(clone);
        long expireAt = level.getServer().getTickCount() + CLONE_LIFETIME_TICKS;
        ACTIVE_CLONES.put(clone.getUUID(), new ActiveClone(target.getUUID(), expireAt));
        DEBUG_LOG.info("[MCDBG] clone spawned: clone={} origin={} pos={} serverTick={}",
                clone.getUUID(), target.getUUID(), clone.blockPosition(), level.getServer().getTickCount());

        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                clone.getX(), clone.getY(0.5), clone.getZ(),
                24, 0.4, 0.6, 0.4, 0.2);
        level.sendParticles(ParticleTypes.END_ROD,
                clone.getX(), clone.getY(0.5), clone.getZ(),
                8, 0.3, 0.5, 0.3, 0.05);
        level.playSound(null, clone.getX(), clone.getY(), clone.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F, 0.7F);
    }

    /**
     * 生命周期驱动：由 {@code MultiverseCloneEventHandler} 在每 ServerTickEvent.Post 调用。
     *
     * <p>对每条活跃记录做两件事：
     * <ol>
     *   <li>未到期：维持互殴——vanilla 索敌 AI（NearestAttackableTargetGoal 等）会周期性
     *       重选目标（通常重选回玩家），单次 setTarget 会被刷掉，必须每 tick 强制互设回去；
     *       本体或克隆体任一方死亡则只维持存活一方继续存在到到期；</li>
     *   <li>到期：discard 克隆体并爆散流光粒子；中途已被杀死/找不到的只清理登记。</li>
     * </ol>
     */
    public static void tickClones(MinecraftServer server) {
        if (ACTIVE_CLONES.isEmpty()) {
            return;
        }
        long now = server.getTickCount();
        Iterator<Map.Entry<UUID, ActiveClone>> iterator = ACTIVE_CLONES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveClone> entry = iterator.next();
            ActiveClone record = entry.getValue();

            // —— 到期消散 ——
            if (record.expireAtTick() <= now) {
                iterator.remove();
                for (ServerLevel level : server.getAllLevels()) {
                    Entity entity = level.getEntityInAnyDimension(entry.getKey());
                    if (entity == null) {
                        continue;
                    }
                    entity.discard();
                    level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                            entity.getX(), entity.getY(0.5), entity.getZ(),
                            48, 0.5, 0.8, 0.5, 0.25);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            entity.getX(), entity.getY(0.5), entity.getZ(),
                            16, 0.5, 0.6, 0.5, 0.15);
                    level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.6F, 1.4F);
                    break;
                }
                continue;
            }

            // —— 互殴维持（每 tick 刷回，压制 vanilla 索敌 AI） ——
            for (ServerLevel level : server.getAllLevels()) {
                Entity cloneEntity = level.getEntityInAnyDimension(entry.getKey());
                if (cloneEntity == null) {
                    continue;
                }
                Entity originEntity = level.getEntityInAnyDimension(record.originId());
                boolean applied = false;
                if (originEntity instanceof Mob origin && cloneEntity instanceof Mob clone
                        && origin.isAlive() && clone.isAlive()) {
                    lockTarget(clone, origin);
                    lockTarget(origin, clone);
                    applied = true;
                }
                // 临时调试：每 2 秒汇报一次维持状态（问题解决后删除）
                if (now % 40L == 0L) {
                    String cloneTarget = cloneEntity instanceof Mob m ? String.valueOf(m.getTarget()) : "-";
                    String originTarget = originEntity instanceof Mob m2 ? String.valueOf(m2.getTarget()) : "-";
                    DEBUG_LOG.info("[MCDBG] maintain: cloneFound={} originFound={} applied={} cloneTarget={} originTarget={}",
                            cloneEntity != null, originEntity != null, applied, cloneTarget, originTarget);
                }
                break;
            }
        }
    }
}
