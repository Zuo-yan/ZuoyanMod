package org.gwfx.zuoyanmod.menu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.gwfx.zuoyanmod.Zuoyanmod;

public final class MenuRegistry {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Zuoyanmod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<VoidResonancePumpMenu>> VOID_RESONANCE_PUMP_MENU =
            MENUS.register("void_resonance_pump",
                    () -> new MenuType<>(VoidResonancePumpMenu::new, FeatureFlags.DEFAULT_FLAGS));

    /**
     * 克莱因瓶：打开属于该玩家的「四维空间」终端。
     * 存储窗口 + **内嵌的 3×3 工作台**都在同一个界面里，没有独立的"工作台界面"。
     */
    public static final DeferredHolder<MenuType<?>, MenuType<KleinBottleMenu>> KLEIN_BOTTLE_MENU =
            MENUS.register("klein_bottle",
                    () -> new MenuType<>(KleinBottleMenu::new, FeatureFlags.DEFAULT_FLAGS));

    /** 微型强子对撞机：双粒子束对撞，产出奇点核心等高能产物。 */
    public static final DeferredHolder<MenuType<?>, MenuType<MicroHadronColliderMenu>> MICRO_HADRON_COLLIDER_MENU =
            MENUS.register("micro_hadron_collider",
                    () -> new MenuType<>(MicroHadronColliderMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private MenuRegistry() {}

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
