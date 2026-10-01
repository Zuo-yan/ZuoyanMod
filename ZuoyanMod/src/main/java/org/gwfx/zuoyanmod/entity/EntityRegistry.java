package org.gwfx.zuoyanmod.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.gwfx.zuoyanmod.Zuoyanmod;

/**
 * 生物（实体类型）注册表。
 *
 * <p>26.3 里注册一个生物需要三处配合，缺一不可：
 * <ol>
 *   <li>这里注册 {@link EntityType}（决定尺寸、追踪范围、生成分类）；</li>
 *   <li>{@link #onEntityAttributeCreation} 里绑定属性（生命/攻击/移速…），
 *       不注册的话实体一生成就崩（找不到 AttributeSupplier）；</li>
 *   <li>客户端 {@code RickRenderer} 注册对应渲染器，否则游戏会警告
 *       "No renderer registered for zuoyanmod:rick" 并且实体不可见。</li>
 * </ol>
 */
@EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class EntityRegistry {

    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities(Zuoyanmod.MODID);

    /**
     * 瑞克：与玩家同尺寸的人形中立生物。
     * <p>
     * {@code clientTrackingRange(10)} 与原版僵尸一致（10 个区块），
     * 保证玩家在较远处也能看到它的模型。
     */
    public static final DeferredHolder<EntityType<?>, EntityType<RickEntity>> RICK =
            ENTITY_TYPES.registerEntityType(
                    "rick",
                    RickEntity::new,
                    MobCategory.CREATURE,
                    builder -> builder
                            .sized(0.6F, 1.8F)
                            .eyeHeight(1.62F)
                            .clientTrackingRange(10)
            );

    /**
     * 原始黑洞的子弹：纯能量体（MobCategory.MISC，与原版箭/雪球同类，
     * 不占刷怪上限）。客户端无模型，靠服务端绿色粒子表现（NoopRenderer）。
     * <p>{@code updateInterval(1)}：高速射线需要每 tick 同步位置，防跳变。
     */
    public static final DeferredHolder<EntityType<?>, EntityType<CausalityBulletEntity>> CAUSALITY_BULLET =
            ENTITY_TYPES.registerEntityType(
                    "causality_bullet",
                    CausalityBulletEntity::new,
                    MobCategory.MISC,
                    builder -> builder
                            .sized(0.3F, 0.3F)
                            .eyeHeight(0.15F)
                            .clientTrackingRange(10)
                            .updateInterval(1)
            );

    /**
     * 原始黑洞：右键道具释放的奇点场。
     * <p>
     * 三个参数都不是随手填的：
     * <ul>
     *   <li>{@code sized(2, 2)}：**刻意只给 2×2 的物理体积**，不用效果体积当碰撞箱。
     *       详见 {@link PrimordialBlackHoleEntity} 的类注释（大 AABB 会撑爆实体分区索引、
     *       把剔除距离拖到上千格、还和活塞/碰撞查询打架）；
     *   <li>{@code clientTrackingRange(8)}：128 格内同步给客户端，和
     *       {@code PrimordialBlackHoleEntity#RENDER_DISTANCE}（也是 128）配套 ——
     *       同步范围小于渲染距离会出现"看得见但没数据"的空壳；
     *   <li>{@code updateInterval(3)}：位置几乎不动（只有出生时 setPos 一次），
     *       没必要每 tick 同步坐标，降到 3 省带宽；
     *   <li>{@code noSave()}：不写进存档。宁可区块卸载时提前消失，
     *       也不要读档后场上飘着一个永远不坍缩、又找不到主人的黑洞。
     * </ul>
     */
    public static final DeferredHolder<EntityType<?>, EntityType<PrimordialBlackHoleEntity>> PRIMORDIAL_BLACK_HOLE =
            ENTITY_TYPES.registerEntityType(
                    "primordial_black_hole",
                    PrimordialBlackHoleEntity::new,
                    MobCategory.MISC,
                    builder -> builder
                            .sized(2.0F, 2.0F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .noSave()
            );

    /**
     * 湮灭君主：湮灭王座遗迹的守关 Boss（MobCategory.MONSTER）。
     * <p>
     * {@code sized(1.6, 3.6)}：比玩家高大一圈的君主体格；渲染端模型再放大 1.8 倍，
     * 视觉高度 ≈ 3.4 格。{@code fireImmune()}：虚空之主不惧火焰，也免得遗迹里
     * 的火把把 Boss 烧得乱跳。属性绑定见 {@link #onEntityAttributeCreation}。
     */
    public static final DeferredHolder<EntityType<?>, EntityType<VoidMonarchEntity>> VOID_MONARCH =
            ENTITY_TYPES.registerEntityType(
                    "void_monarch",
                    VoidMonarchEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder
                            .sized(1.6F, 3.6F)
                            .eyeHeight(3.2F)
                            .clientTrackingRange(10)
                            .fireImmune()
            );

    /** 湮灭侍卫：君主麾下的人形精英小怪，遗迹驻军 + 召唤物。 */
    public static final DeferredHolder<EntityType<?>, EntityType<VoidGuardEntity>> VOID_GUARD =
            ENTITY_TYPES.registerEntityType(
                    "void_guard",
                    VoidGuardEntity::new,
                    MobCategory.MONSTER,
                    builder -> builder
                            .sized(0.7F, 2.0F)
                            .eyeHeight(1.75F)
                            .clientTrackingRange(10)
            );

    /** 暗物质螺栓：君主的弹幕投射物（MISC，与原版箭/雪球同类，不占刷怪上限）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<VoidBoltEntity>> VOID_BOLT =
            ENTITY_TYPES.registerEntityType(
                    "void_bolt",
                    VoidBoltEntity::new,
                    MobCategory.MISC,
                    builder -> builder
                            .sized(0.4F, 0.4F)
                            .eyeHeight(0.2F)
                            .clientTrackingRange(8)
                            .updateInterval(2)
            );

    /** 超级电能机枪豌豆：高速射击并连锁电击僵尸/怪物的植物战力。 */
    public static final DeferredHolder<EntityType<?>, EntityType<SuperElectricGatlingPeaEntity>> SUPER_ELECTRIC_GATLING_PEA =
            ENTITY_TYPES.registerEntityType(
                    "super_electric_gatling_pea",
                    SuperElectricGatlingPeaEntity::new,
                    MobCategory.CREATURE,
                    builder -> builder
                            .sized(0.7F, 1.3F)
                            .eyeHeight(1.0F)
                            .clientTrackingRange(10)
            );

    /** 电能豌豆子弹：极速电浆子弹，带连锁闪电。 */
    public static final DeferredHolder<EntityType<?>, EntityType<ElectroPeaBulletEntity>> ELECTRO_PEA_BULLET =
            ENTITY_TYPES.registerEntityType(
                    "electro_pea_bullet",
                    ElectroPeaBulletEntity::new,
                    MobCategory.MISC,
                    builder -> builder
                            .sized(0.4F, 0.4F)
                            .eyeHeight(0.2F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
            );

    private EntityRegistry() {}

    /**
     * 把实体属性表挂到实体类型上。
     * <p>
     * 属性基值定义在各实体类的 {@code createAttributes()}，这里只做绑定——
     * 不绑定的话实体一生成就会因为找不到 AttributeSupplier 而崩。
     */
    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(RICK.get(), RickEntity.createAttributes().build());
        event.put(VOID_MONARCH.get(), VoidMonarchEntity.createAttributes().build());
        event.put(VOID_GUARD.get(), VoidGuardEntity.createAttributes().build());
        event.put(SUPER_ELECTRIC_GATLING_PEA.get(), SuperElectricGatlingPeaEntity.createAttributes().build());
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}
