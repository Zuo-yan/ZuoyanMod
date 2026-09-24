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

    private EntityRegistry() {}

    /**
     * 把瑞克的属性表挂到实体类型上。
     * <p>
     * 属性基值定义在 {@link RickEntity#createAttributes()}，这里只做绑定——
     * 不绑定的话实体一生成就会因为找不到 AttributeSupplier 而崩。
     */
    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(RICK.get(), RickEntity.createAttributes().build());
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}
