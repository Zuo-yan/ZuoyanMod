package org.gwfx.zuoyanmod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.damage.PrimordialBlackHoleDamageSource;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 原始黑洞 —— 右键「原始黑洞」道具后，在准心落点展开的**奇点场**。
 *
 * <p><b>它是一个"场"，不是一个"物"。</b>存续 20 秒，期间把周围实体与掉落物往中心拽 ——
 * 而且**牵引半径不是恒定的**：它跟着生命曲线从 {@value #PULL_RADIUS_MIN} 格一路长到
 * {@value #PULL_RADIUS_MAX} 格，最后 4 秒再收回去，然后才结算爆发伤害。
 * 全程**没有持续伤害** —— 它的作用是"把东西聚到一起"，伤害只在谢幕那一下。
 *
 * <h2>为什么做成真实体，而不是像本项目的真空衰变黑洞那样做成 Manager</h2>
 * 真空衰变的黑洞（{@code event/VacuumDecayBlackHoleManager}）是一份"静态列表 + 每 tick 推进"的
 * 管理器，完全够用 —— 但它有一个先天限制：**没有任何世界空间的自绘能力**。
 * 本项目所有特效都是"服务端广播原版粒子"，也没用过 {@code RenderLevelStageEvent} 之类的事件，
 * 所以要做"永远正对相机的黑盘 + 自转的能量涡流"这种表现，必须有客户端渲染挂点，
 * 而现成的挂点就是 {@code EntityRenderer}（本项目已有 {@code CausalityBulletRenderer} 铺好路）。
 * 于是宁可多注册一个实体，也要拿到这个挂点。
 *
 * <h2>为什么继承 {@link Entity} 而不是 {@code Projectile}</h2>
 * {@code Projectile} 带的是"会飞的东西"那一整套语义（owner / shoot / 命中判定 / 已发射标记），
 * 而本实体**从不移动、不命中任何东西**。继承 {@code Entity} 只需要补 4 个抽象方法，负担最小。
 *
 * <h2>为什么本体碰撞箱只有 2×2，而不是像参考模组那样直接用效果体积当碰撞箱</h2>
 * 参考实现（Iron's Spells 的黑洞）把实体碰撞箱设成 {@code sized(R*2, R*2)}，拿它当效果体积用。
 * 那是**有代价**的：一个 16×16×16 的 AABB 会被登记进它所覆盖的每一个 {@code EntitySection}
 * （每次包围盒变动都要重索引）、会把 {@code shouldRenderAtSqrDistance} 的默认值
 * 撑到 {@code size*64} = 1024 格、还会与活塞和实体碰撞查询相互干扰。
 * 这里把**物理体积与效果体积解耦**：本体只有 2×2×2（只为同步与剔除服务），
 * 吸力扫描用的 AABB 每次在 {@link #pull} 里现造。代价是每 2 tick 多造一个 AABB 对象，
 * 换掉上面三个坑，很划算。
 *
 * <h2>为什么不存盘（{@code EntityType.Builder#noSave}）</h2>
 * "读档后场上还飘着一个永远不会坍缩的黑洞"是比"能量在区块卸载时提前消失"更坏的结果：
 * 前者会让玩家莫名其妙持续被吸，且**没有任何办法处理它**（它已经没有归属了）。
 * 所以宁可让它随区块一起消失。注意 {@code noSave} 只是不写进存档，
 * {@link #addAdditionalSaveData} / {@link #readAdditionalSaveData} 仍然要实现完整 ——
 * 它们是抽象方法，而且写对了将来改主意也只要删掉一个 {@code noSave()}。
 *
 * <h2>1.20.1 适配</h2>
 * <ul>
 *   <li>{@code Identifier} → {@code ResourceLocation}，
 *       {@code Identifier.fromNamespaceAndPath(ns, path)} → {@code new ResourceLocation(ns, path)}。</li>
 *   <li>{@code defineSynchedData(SynchedEntityData.Builder)} → 1.20.1 是
 *       {@code protected void defineSynchedData()}，内部自己调
 *       {@code this.entityData.define(ACCESSOR, 默认值)}；
 *       {@code SynchedEntityData.defineId(Class, Serializer)} 与 26.3 同名。</li>
 *   <li>存盘：26.3 的 {@code ValueOutput} / {@code ValueInput} → 1.20.1 是
 *       {@code addAdditionalSaveData(CompoundTag)} / {@code readAdditionalSaveData(CompoundTag)}。
 *       26.3 的 {@code getIntOr(key, 默认值)} 在 1.20.1 没有，改成
 *       {@code tag.contains(key) ? tag.getInt(key) : 默认值}。
 *       Owner 仍然按**字符串**存（而不是 {@code putUUID}）—— 保留主线那条
 *       "UUID 坏了就当没有主"的容错解析，不让坏存档把整个实体加载失败。</li>
 *   <li>免疫伤害：26.3 覆写 {@code hurtServer(ServerLevel, DamageSource, float)}，
 *       1.20.1 没有这个方法，改成覆写 {@code hurt(DamageSource, float)}。
 *       同样不影响 {@code /kill}（它走 {@code remove(RemovalReason.KILLED)}，不经过 hurt）。</li>
 *   <li>结算伤害：{@code living.hurtServer(level, source, amount)} → {@code living.hurt(source, amount)}。</li>
 *   <li>速度同步：26.3 的 {@code entity.syncVelocity = true} 在 1.20.1 不存在，改成
 *       {@code entity.hurtMarked = true} —— 与本项目 {@code VacuumDecayBlackHoleManager}
 *       里已有的处理完全一致（服务端才会补发 {@code ClientboundSetEntityMotionPacket}）。</li>
 *   <li>音效：26.3 的 {@code SoundEvents} 常量全是 holder，要 {@code .value()}；
 *       1.20.1 大部分是裸 {@code SoundEvent}（如 {@code GENERIC_EXPLODE}），
 *       但 {@code RESPAWN_ANCHOR_DEPLETE} 在 1.20.1 是
 *       {@code Holder.Reference<SoundEvent>}，仍然要 {@code .value()}。</li>
 *   <li>剔除箱：26.3 是渲染器覆写 {@code EntityRenderer#getBoundingBoxForCulling(实体, partialTicks)}；
 *       1.20.1 的 {@code EntityRenderer} 没有这个钩子，剔除箱来自
 *       {@code Entity#getBoundingBoxForCulling()}，所以这里改在实体侧覆写。</li>
 *   <li>{@code level.isClientSide} 在 1.20.1 是字段不是方法。</li>
 *   <li>{@code @Nullable} 用 {@code javax.annotation.Nullable}（本项目 1.20.1 分支的约定）。</li>
 * </ul>
 */
public class PrimordialBlackHoleEntity extends Entity {

    // ===================== 设计数值（要调平衡改这里就够了）=====================

    /** 存续时长：20 秒。留足"长大 → 维持 → 缩小 → 坍缩"四个阶段的表演时间。 */
    public static final int LIFETIME_TICKS = 400;

    /**
     * 牵引半径上限（格）：以黑洞为中心的立方体搜索半边长，最大 80 格直径。
     *
     * <p><b>实际半径是随生命曲线缩放的</b>，不是恒定的 —— 见 {@link #scaleFactor}。
     * 刚生成时接近 {@link #PULL_RADIUS_MIN}，成年时到满值，坍缩前又收回去。
     *
     * <p>为什么定 40：实体只存在于**已加载区块**里，默认模拟距离约 160 格，
     * 所以 40 格直径 80 已经覆盖了"整个战场"；再大的话多出来的部分只是在扫空区块
     * （扫描开销按半径的立方增长），而远处实体被拽过来时没有寻路、会卡在山坡树木上，
     * 观感上反而更差。
     */
    public static final double PULL_RADIUS_MAX = 40.0D;

    /** 牵引半径下限（格）：刚生成和即将坍缩时保留的最小范围。 */
    public static final double PULL_RADIUS_MIN = 1.5D;

    /** 爆发伤害半径（格）。坍缩前黑洞已经收束，东西都堆在中心，12 格足够覆盖。 */
    public static final double BLAST_RADIUS = 12.0D;

    /** 爆发伤害：20 心，普通伤害（吃护甲/抗性/保护附魔）。 */
    public static final float BLAST_DAMAGE = 40.0F;

    /** 视觉半径上限（格）：客户端渲染器看到的"整体大小"，黑盘与涡流都按它按比例缩放。 */
    public static final double VISUAL_RADIUS = 3.6D;

    /**
     * 生命曲线的三个节点（占寿命的比例）：0 → {@value #GROW_END} 长大，{@value #GROW_END} → {@value #SHRINK_START} 维持，
     * {@value #SHRINK_START} → 1 收束。
     * <p>以 20 秒寿命换算：前 11 秒长大、中段 5 秒维持满值、最后 4 秒缩回。
     */
    public static final float GROW_END = 0.55F;
    public static final float SHRINK_START = 0.80F;

    /** 全局同时存在的黑洞数量上限。每个黑洞每 2 tick 要扫一遍最大 40 格内的实体，必须封顶。 */
    public static final int MAX_ACTIVE_HOLES = 16;

    /** 客户端的剔除距离（格）。覆写 {@link #shouldRenderAtSqrDistance} 用，见那里的注释。 */
    public static final double RENDER_DISTANCE = 128.0D;

    /** 牵引基础加速度（格/tick）：在半径边缘处生效。数值沿用真空衰变黑洞的手感公式。 */
    private static final double PULL_ACCEL_BASE = 0.09D;

    /** 越靠近中心越强的附加加速度（格/tick）。 */
    private static final double PULL_ACCEL_BONUS = 0.20D;

    /**
     * 死区半径（格）：进到这里就不再加速。
     * <p>不加速之后改**强阻尼**（见 {@link #DEAD_ZONE_DAMPING}）而不是放任不管 ——
     * 否则被拽进来的实体会带着残余速度绕着中心公转，看起来像一群卫星而不是"被吸进去"。
     */
    private static final double PULL_DEAD_ZONE = 0.9D;

    /** 死区内的每 tick 速度保留比例。 */
    private static final double DEAD_ZONE_DAMPING = 0.5D;

    /**
     * 速度上限（格/tick）。
     * <p><b>这个上限是必须的</b>：真空衰变的黑洞只活 100 tick，而这里活 400 tick，
     * 同一套"每 tick 叠加加速度"的公式累积下来会变成弹弓 —— 实体被甩到几百格外。
     */
    private static final double PULL_MAX_SPEED = 1.6D;

    /** 吸力扫描降频：每 N tick 扫一次。 */
    private static final int UPDATE_INTERVAL = 2;

    /**
     * 拖尾粒子的作用距离（格）。
     * <p>半径最大能到 40 格，但粒子的实际可见距离远小于此；给视野外每一只被吸的生物都发粒子
     * 纯属浪费带宽（刷怪塔里可能有几百只）。只画这一圈之内的，视觉上完全一样。
     */
    private static final double PARTICLE_TRAIL_RANGE = 24.0D;

    /** 循环音效间隔（tick）。20 秒内响 10 次，够营造"持续存在感"又不吵。 */
    private static final int LOOP_SOUND_INTERVAL = 40;

    /** 进入最后 N tick 开始播"心跳"，预告即将内爆。 */
    private static final int WARNING_TICKS = 60;

    /** 心跳间隔（tick）。 */
    private static final int WARNING_SOUND_INTERVAL = 20;

    /**
     * Boss 判定标签：{@code zuoyanmod:bosses}（data/zuoyanmod/tags/entity_type/bosses.json）。
     * <p><b>1.20.1 注意</b>：本分支的资源目录按 1.20.1 的命名规则应为复数
     * {@code data/zuoyanmod/tags/entity_types/bosses.json}（见 {@code docs/version_migration.md} 第 6 节）。
     * 标签文件缺失时 {@code isBoss} 一律返回 false，只是"不再对 Boss 减半吸力"，
     * 不会报错、也不会影响其它行为。
     */
    private static final TagKey<EntityType<?>> BOSSES_TAG = TagKey.create(
            Registries.ENTITY_TYPE,
            new ResourceLocation(Zuoyanmod.MODID, "bosses")
    );

    /**
     * 唯一需要同步给客户端的量：视觉半径。
     * <p><b>为什么不能直接让渲染器读普通字段</b>：客户端的这个实体是服务端发
     * {@code ClientboundAddEntityPacket} 新建出来的，普通字段一律是初始化值，
     * 渲染器读到的永远是默认数。反过来说，凡是渲染需要的量都必须走 SynchedEntityData；
     * 能由客户端本地推出来的量（比如动画进度，取自 {@code tickCount}）就不该浪费同步带宽。
     */
    private static final EntityDataAccessor<Float> DATA_VISUAL_RADIUS =
            SynchedEntityData.defineId(PrimordialBlackHoleEntity.class, EntityDataSerializers.FLOAT);

    // ===================== 状态 =====================

    /** 剩余寿命（tick）。减到 0 就坍缩。 */
    private int life = LIFETIME_TICKS;

    /**
     * 施法者 UUID，null 表示不是玩家放出来的（{@code /summon} 调试生成）。
     * <p>存 UUID 而不是实体引用：黑洞要比玩家在线时间活得久，持有实体引用会阻止它被回收。
     */
    private @Nullable UUID ownerUuid;

    // ===================== 构造 =====================

    /** 注册表 / 反序列化路径。 */
    public PrimordialBlackHoleEntity(EntityType<? extends PrimordialBlackHoleEntity> type, Level level) {
        super(type, level);
        // 位置永久固定的"场"：不参与方块碰撞、不被推动、不会被挤出方块
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** 施法路径。 */
    public PrimordialBlackHoleEntity(ServerLevel level, @Nullable Player caster, Vec3 center) {
        this(EntityRegistry.PRIMORDIAL_BLACK_HOLE.get(), level);
        if (caster != null) {
            this.ownerUuid = caster.getUUID();
        }
        this.setPos(center.x, center.y, center.z);
        this.setVisualRadius((float) VISUAL_RADIUS);
    }

    /**
     * 在指定位置展开一个黑洞（只负责"造 + 放 + 演出"，**不含**任何上限/冷却检查 ——
     * 那些属于使用它的物品，见 {@code item/PrimordialBlackHoleItem}）。
     */
    public static void spawn(ServerLevel level, @Nullable Player caster, Vec3 center) {
        PrimordialBlackHoleEntity hole = new PrimordialBlackHoleEntity(level, caster, center);
        level.addFreshEntity(hole);
        hole.playOpenEffects(level);
    }

    // ===================== 同步数据 =====================

    public float getVisualRadius() {
        return this.entityData.get(DATA_VISUAL_RADIUS);
    }

    public void setVisualRadius(float radius) {
        this.entityData.set(DATA_VISUAL_RADIUS, radius);
    }

    public @Nullable UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    @Override
    protected void defineSynchedData() {
        // 基类数据（名字、隐身、重力标记等）由 Entity 构造器一并定义，这里只管自己的
        this.entityData.define(DATA_VISUAL_RADIUS, (float) VISUAL_RADIUS);
    }

    // ===================== 行为开关 =====================

    /** 准心选不中它：它是"空间本身"，不是一个可以交互的物体。 */
    @Override
    public boolean isPickable() {
        return false;
    }

    /**
     * 免疫一切伤害。
     * <p>注意这不影响管理员清场：{@code /kill} 走的是 {@code Entity#kill(ServerLevel)} →
     * {@code remove(RemovalReason.KILLED)}，**根本不经过 hurt**。
     * 所以返回 false 是安全的 —— 玩家打不烂它，管理员仍然收得掉它。
     */
    @Override
    public boolean hurt(DamageSource source, float damage) {
        return false;
    }

    /**
     * 剔除距离用固定值。
     * <p>默认实现是 {@code distance < getBoundingBox().getSize() * 64}，而本体只有 2×2×2 的盒子，
     * 算出来只有 128 格 —— 数值上够用，但把"渲染距离"和"碰撞箱大小"绑在一起是脆的：
     * 哪天有人把 box 改小，黑洞就会在远处突然消失。这里写死，让两者解耦。
     */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < RENDER_DISTANCE * RENDER_DISTANCE;
    }

    /**
     * 剔除箱必须手动撑开。
     * <p>本体碰撞箱只有 2×2×2（见 {@code EntityRegistry} 里的注释），而视觉半径到
     * 3.6 格、涡流还要再往外一圈。不撑开的话，玩家贴近时黑洞本体在视锥外，
     * 整团特效会被一起剔掉 —— 表现为"走到跟前黑洞就消失"。
     * <p>1.20.1 的 {@code EntityRenderer#shouldRender} 读的是
     * {@code Entity#getBoundingBoxForCulling()}，所以这个钩子挂在实体上，
     * 而不是像 26.3 那样挂在渲染器上（那边有
     * {@code getBoundingBoxForCulling(实体, partialTicks)} 覆写点）。
     */
    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(VISUAL_RADIUS + 1.5D);
    }

    // ===================== 存盘 =====================
    // 实体类型注册时带了 noSave()，所以正常情况下这两个方法不会被调用；
    // 但它们把状态写全，既满足抽象方法的要求，也让"哪天想让它存盘"只需要删掉 noSave()。

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", this.life);
        if (this.ownerUuid != null) {
            tag.putString("Owner", this.ownerUuid.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.life = tag.contains("Life") ? tag.getInt("Life") : LIFETIME_TICKS;
        String owner = tag.getString("Owner");
        try {
            this.ownerUuid = owner.isEmpty() ? null : UUID.fromString(owner);
        } catch (IllegalArgumentException ignored) {
            // 存档里的 UUID 坏了就当没有主 —— 宁可不做归因，也不要因此让整个实体加载失败
            this.ownerUuid = null;
        }
    }

    // ===================== 生命曲线（服务端与客户端共用，唯一的真值来源）=====================

    /**
     * 当前"张开程度"，0 = 刚生成/已收束，1 = 成年。
     *
     * <p><b>为什么把它做成静态方法、两端共用</b>：视觉的生长动画和牵引半径必须走同一条曲线，
     * 否则又会出现"看着在变大、吸力却恒定"的脱钩（这正是上一版的毛病）。
     * 服务端有 {@link #life}（倒计时），客户端只有 {@code tickCount}（正计时），
     * 所以这里统一按"已经活了多久"来算：服务端传 {@code LIFETIME_TICKS - life}，
     * 渲染器传 {@code ageInTicks}。两边各自本地推算，不需要为它多传一个同步字段。
     *
     * @param ageTicks 已经存活的 tick 数
     */
    public static float scaleFactor(float ageTicks) {
        float life = LIFETIME_TICKS;
        if (ageTicks <= 0.0F) {
            return 0.0F;
        }
        if (ageTicks < life * GROW_END) {
            return Mth.clamp(ageTicks / (life * GROW_END), 0.0F, 1.0F);
        }
        if (ageTicks > life * SHRINK_START) {
            // 收束段：越接近寿命末端越小，到 0 时正好触发坍缩
            return Mth.clamp((life - ageTicks) / (life * (1.0F - SHRINK_START)), 0.0F, 1.0F);
        }
        return 1.0F;
    }

    /** 本黑洞已经活了多少 tick。 */
    private float age() {
        return LIFETIME_TICKS - this.life;
    }

    /**
     * 当前实际牵引半径：在下限与上限之间按生命曲线插值。
     * <p>留一个下限而不是从 0 开始，是为了让"刚生成的那一瞬"和"收束的最后一刻"也能抓到身边的东西，
     * 不然会有两段时间明明看得见黑洞却什么都吸不动。
     */
    public double currentPullRadius() {
        return PULL_RADIUS_MIN + (PULL_RADIUS_MAX - PULL_RADIUS_MIN) * scaleFactor(this.age());
    }

    // ===================== 每 tick =====================

    @Override
    public void tick() {
        // 必须调 super：tickCount 在这里自增，客户端的动画进度就是拿 tickCount 算的，
        // 不调的话客户端拿不到时间轴，黑洞会僵在初始大小。
        super.tick();

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.life <= 0) {
            this.collapse(serverLevel);
            return;
        }

        if (this.tickCount % LOOP_SOUND_INTERVAL == 0) {
            this.playLoopSound(serverLevel);
        }
        // 最后 3 秒加心跳：给玩家一个"快炸了"的预告，也是唯一能提示"该跑远点"的手段
        if (this.life <= WARNING_TICKS && this.life % WARNING_SOUND_INTERVAL == 0) {
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.7F, 0.6F);
        }

        if (this.tickCount % UPDATE_INTERVAL == 0) {
            this.pull(serverLevel);
            this.spawnAmbientParticles(serverLevel);
        }

        this.life--;
    }

    // ===================== 牵引 =====================

    /**
     * 把范围内的实体往中心拽。
     *
     * <p>吸力公式是"越近越强"的**线性**衰减：{@code accel = 0.09 + 0.20 × (1 - d/R)}，
     * 沿用真空衰变黑洞已经调好的手感。方向始终指向中心，所以观感上是"卷进去"。
     *
     * <p><b>半径 R 每次都现算</b>（见 {@link #currentPullRadius()}）：它跟着生命曲线长大再缩回，
     * 所以同一只生物在黑洞刚生成时够不到、成年时就被拽进来了 —— 这正是"范围也在长大"的实现。
     *
     * <p>几个刻意为之的地方：
     * <ul>
     *   <li><b>加速度乘 {@link #UPDATE_INTERVAL}</b>：本方法每 2 tick 才跑一次，
     *       不补这一下的话平均受力只有逐 tick 版本的一半，手感会"软"。</li>
     *   <li><b>速度封顶</b>：见 {@link #PULL_MAX_SPEED} 的注释。</li>
     *   <li><b>Boss 吸力减半</b>：不这么做的话末影龙、凋灵会被拽着满地跑，
     *       既难看又等于白送一个"控 Boss"的手段。</li>
     *   <li><b>把施法者自己排除</b>：他要站在原地看，而且被自己的黑洞吸走体验极差。</li>
     * </ul>
     */
    private void pull(ServerLevel level) {
        Vec3 center = this.position();
        // 半径每次现算：它跟着生命曲线长大再缩回，所以不能提到外面当缓存
        double radius = this.currentPullRadius();
        AABB zone = box(center, radius);

        for (Entity entity : level.getEntitiesOfClass(Entity.class, zone)) {
            if (entity == this || entity instanceof PrimordialBlackHoleEntity) {
                continue; // 不吸自己，也不吸别的黑洞（互相吸会把它们挤成一坨）
            }
            if (entity.isSpectator()) {
                continue;
            }
            if (this.ownerUuid != null && this.ownerUuid.equals(entity.getUUID())) {
                continue; // 施法者是参照系
            }

            Vec3 delta = center.subtract(entity.getBoundingBox().getCenter());
            double distance = delta.length();

            if (distance < PULL_DEAD_ZONE) {
                // 死区：不再加速，改为强阻尼 —— 让它们"堆"在中心，而不是绕中心公转
                entity.setDeltaMovement(entity.getDeltaMovement().scale(DEAD_ZONE_DAMPING));
                // 1.20.1：26.3 的 entity.syncVelocity = true 在这里不存在，改打 hurtMarked
                entity.hurtMarked = true;
                continue;
            }

            double accel = PULL_ACCEL_BASE + PULL_ACCEL_BONUS * (1.0D - Math.min(1.0D, distance / radius));
            if (isBoss(entity)) {
                accel *= 0.5D;
            }
            accel *= UPDATE_INTERVAL;

            Vec3 velocity = entity.getDeltaMovement().add(delta.scale(accel / distance));
            if (velocity.lengthSqr() > PULL_MAX_SPEED * PULL_MAX_SPEED) {
                velocity = velocity.normalize().scale(PULL_MAX_SPEED);
            }
            entity.setDeltaMovement(velocity);

            // ⚠️ 1.20.1 的坑（真空衰变黑洞那边已经踩过一次）：setDeltaMovement / push 只会置
            //    needsSync，也就是**只同步位置**。速度变更必须额外打 hurtMarked，否则服务端不会补发
            //    ClientboundSetEntityMotionPacket —— 玩家（客户端权威）会纹丝不动。
            //    （26.3 对应的写法是 entity.syncVelocity = true）
            entity.hurtMarked = true;
            // 被吸着飞不算坠落：不清零的话，等它松手落地会按"从高处掉下来"结算摔落伤害
            entity.resetFallDistance();

            // 拖尾粒子只画近处的：半径能到 40 格，而粒子的实际可见距离远小于此，
            // 给视野外每一只被吸的生物都发一遍粒子纯属浪费带宽（团子农场里能有几百只）
            if (distance <= PARTICLE_TRAIL_RANGE) {
                level.sendParticles(ParticleTypes.PORTAL,
                        entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                        2, 0.2D, 0.3D, 0.2D, 0.05D);
            }
        }
    }

    /** Boss 检测：实体类型是否在 {@code zuoyanmod:bosses} 标签内。 */
    private static boolean isBoss(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType()).is(BOSSES_TAG);
    }

    /** 以 center 为中心、半径 radius 的立方体搜索区。 */
    private static AABB box(Vec3 center, double radius) {
        return new AABB(center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
    }

    // ===================== 收尾（内爆）=====================

    /**
     * 坍缩：结算一次爆发伤害，放完最后一次演出就消散。
     *
     * <p>施法者可能已经离线、死了退出、或者压根没有（{@code /summon} 出来的）——
     * 这时 {@code getPlayer(uuid)} 返回 null，{@link DamageSource} 的 causingEntity 就为空。
     * 这是**故意允许**的：宁可这一下没有归因（死亡消息退化成"被原始黑洞吞噬"），
     * 也不要因为找不到人就跳过整段伤害结算。
     */
    private void collapse(ServerLevel level) {
        Vec3 center = this.position();

        Player caster = this.ownerUuid == null
                ? null
                : level.getServer().getPlayerList().getPlayer(this.ownerUuid);
        DamageSource source = PrimordialBlackHoleDamageSource.create(level, caster);

        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box(center, BLAST_RADIUS))) {
            if (this.ownerUuid != null && this.ownerUuid.equals(living.getUUID())) {
                continue; // 施法者不吃自己的爆发
            }
            // 1.20.1：没有 hurtServer(...)，回到 hurt(source, amount)
            living.hurt(source, BLAST_DAMAGE);
        }

        // 内爆演出：三层叠 —— 爆炸亮闪 + 音波冲击 + 反向传送门粒子内收 + 幽匿灵魂上升
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 0.6F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0F, 1.4F);
        level.playSound(null, center.x, center.y, center.z,
                // 1.20.1 里 RESPAWN_ANCHOR_DEPLETE 是 Holder.Reference<SoundEvent>，仍要 .value()
                SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 1.2F, 0.5F);

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 120, 1.5D, 1.5D, 1.5D, 0.9D);
        level.sendParticles(ParticleTypes.SCULK_SOUL, center.x, center.y, center.z, 40, 1.0D, 1.0D, 1.0D, 0.05D);

        this.discard();
    }

    // ===================== 演出（全部是原版粒子/音效，服务端广播）=====================

    /** 开场：一下"撕开空间"的音爆 + 反向传送门粒子外扩。 */
    public void playOpenEffects(ServerLevel level) {
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 1.4F, 0.42F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 0.8F, 0.6F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY(), this.getZ(),
                80, 3.0D, 3.0D, 3.0D, 0.6D);
        level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY(), this.getZ(),
                12, 1.5D, 1.5D, 1.5D, 0.02D);
    }

    /** 循环音：低频的传送门环境音，每 {@link #LOOP_SOUND_INTERVAL} tick 一次。 */
    private void playLoopSound(ServerLevel level) {
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 0.6F, 0.32F);
    }

    /**
     * 环境粒子：中心一小撮黑雾 + 传送门粒子。
     * <p>和牵引一样每 {@link #UPDATE_INTERVAL} tick 才发一次 —— 每 tick 都发的话，
     * 16 个黑洞同时在场的极端情况下就是粒子风暴，而观感并不会更好。
     */
    private void spawnAmbientParticles(ServerLevel level) {
        // 最后 2 秒换成内收的反向粒子 + 上升的末影光点，把"要炸了"写在画面上
        boolean collapsing = this.life <= WARNING_TICKS / 3;
        // 粒子云的铺开范围跟着生命曲线走：小的时候是一小撮，成年时是一大片，收束时又收回来。
        // 这是玩家唯一能"看见范围"的线索 —— 黑盘本体最大也才 3.6 格，而吸力到 40 格。
        float open = scaleFactor(this.age());
        double spread = 2.0D + 9.0D * open;
        double speed = 0.35D + 0.55D * open;

        level.sendParticles(collapsing ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.PORTAL,
                this.getX(), this.getY(), this.getZ(),
                collapsing ? 20 : 10, spread, spread, spread, speed);
        level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY(), this.getZ(),
                3, spread * 0.75D, spread * 0.75D, spread * 0.75D, 0.02D);
        if (collapsing) {
            level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(),
                    6, spread * 0.6D, spread * 0.6D, spread * 0.6D, 0.1D);
        }
    }

    // ===================== 查询工具（给物品与外部用）=====================

    /**
     * 某玩家是否已经有一个未坍缩的黑洞（**跨维度**查）。
     *
     * <p>用 {@code ServerLevel#getAllEntities()} 全量枚举而不是开一个大 AABB 去查：
     * AABB 受限于单维度、还可能漏掉刚好在边界上的实体，而这里只在右键时调用一次，
     * O(全世界实体数) 完全可以接受。
     */
    public static boolean hasActiveBlackHole(MinecraftServer server, UUID ownerUuid) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof PrimordialBlackHoleEntity hole
                        && ownerUuid.equals(hole.getOwnerUuid())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 当前全世界（含各维度）活跃黑洞数量，用于全局安全阀。 */
    public static int countActive(MinecraftServer server) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof PrimordialBlackHoleEntity) {
                    count++;
                }
            }
        }
        return count;
    }
}
