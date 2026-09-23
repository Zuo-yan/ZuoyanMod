package org.gwfx.zuoyanmod.menu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import org.gwfx.zuoyanmod.Zuoyanmod;

public final class MenuRegistry {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Zuoyanmod.MODID);

    // 1.20.1 的 MenuType 构造要传 FeatureFlagSet（26.x 才移除）
    public static final RegistryObject<MenuType<VoidResonancePumpMenu>> VOID_RESONANCE_PUMP_MENU =
            MENUS.register("void_resonance_pump",
                    () -> new MenuType<>(VoidResonancePumpMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    /**
     * 克莱因瓶：打开属于该玩家的「四维空间」终端。
     * 存储窗口 + **内嵌的 3×3 工作台**都在同一个界面里，没有独立的"工作台界面"。
     */
    public static final RegistryObject<MenuType<KleinBottleMenu>> KLEIN_BOTTLE_MENU =
            MENUS.register("klein_bottle",
                    () -> new MenuType<>(KleinBottleMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    private MenuRegistry() {}

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
