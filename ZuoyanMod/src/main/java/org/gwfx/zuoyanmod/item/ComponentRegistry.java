package org.gwfx.zuoyanmod.item;

import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.gwfx.zuoyanmod.Zuoyanmod;

/**
 * 自定义物品 {@code DataComponentType} 注册表（26.3 API）。
 *
 * <p>26.3 里注册一个自定义组件需要两处配合：
 * <ol>
 *   <li>这里通过 {@code DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, modid)}
 *       建 {@code DeferredRegister$DataComponents}（组件类型用 {@code registerComponentType} 注册）
 *       ——注意 26.3 的工厂签名首参是 {@code ResourceKey}，不是旧版的 modid 字符串；</li>
 *   <li>{@code Zuoyanmod} 构造器里在 {@code ItemRegistry.register} 之前调用
 *       {@link #register(IEventBus)}——物品的默认组件要在注册期引用组件 holder，必须先注册组件。</li>
 * </ol>
 *
 * <p>因果律手枪的等级组件（CAUSALITY_LEVEL）已随升级系统一并移除——
 * 手枪无等级、无成长，规则恒定。
 */
public final class ComponentRegistry {

    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Zuoyanmod.MODID);

    private ComponentRegistry() {}

    public static void register(IEventBus modBus) {
        DATA_COMPONENT_TYPES.register(modBus);
    }
}
