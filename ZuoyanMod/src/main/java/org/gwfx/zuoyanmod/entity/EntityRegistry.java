package org.gwfx.zuoyanmod.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import org.gwfx.zuoyanmod.Zuoyanmod;

/**
 * 生物（实体类型）注册表。
 *
 * <p>注册一个生物需要三处配合，缺一不可：
 * <ol>
 *   <li>这里注册 {@link EntityType}（决定尺寸、追踪范围、生成分类）；</li>
 *   <li>{@link #onEntityAttributeCreation} 里绑定属性（生命/攻击/移速…），
 *       不注册的话实体一生成就崩（找不到 AttributeSupplier）；</li>
 *   <li>客户端 {@code RickRenderer} 注册对应渲染器，否则游戏会警告
 *       "No renderer registered for zuoyanmod:rick" 并且实体不可见。</li>
 * </ol>
 *
 * <p>1.20.1 适配：`DeferredRegister<EntityType<?>>` + {@code EntityType.Builder}
 * （26.x 有 DeferredRegister.Entities 便捷包装）；属性绑定事件在 Forge 里走
 * **mod 事件总线**（{@code Bus.MOD}）。</p>
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EntityRegistry {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Zuoyanmod.MODID);

    /**
     * 瑞克：与玩家同尺寸的人形中立生物。
     * <p>
     * {@code clientTrackingRange(10)} 与原版僵尸一致（10 个区块），
     * 保证玩家在较远处也能看到它的模型。
     */
    public static final RegistryObject<EntityType<RickEntity>> RICK =
            ENTITY_TYPES.register("rick",
                    () -> EntityType.Builder.<RickEntity>of(RickEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build(new ResourceLocation(Zuoyanmod.MODID, "rick").toString()));

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
