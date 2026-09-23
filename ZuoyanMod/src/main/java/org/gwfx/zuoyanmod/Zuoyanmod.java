package org.gwfx.zuoyanmod;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.gwfx.zuoyanmod.block.BlockRegistry;
import org.gwfx.zuoyanmod.client.VoidResonancePumpScreen;
import org.gwfx.zuoyanmod.menu.MenuRegistry;
import org.gwfx.zuoyanmod.effect.EffectRegistry;
import org.gwfx.zuoyanmod.fluid.FluidRegistry;
import org.gwfx.zuoyanmod.item.CreativeTabRegistry;
import org.gwfx.zuoyanmod.item.ItemRegistry;
import org.gwfx.zuoyanmod.sound.SoundRegistry;
import org.slf4j.Logger;

@Mod(Zuoyanmod.MODID)
public class Zuoyanmod {
    public static final String MODID = "zuoyanmod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Zuoyanmod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        // 挂载音效和药水效果注册表
        SoundRegistry.SOUND_EVENTS.register(modEventBus);
        EffectRegistry.EFFECTS.register(modEventBus);

        // 注册方块、物品系统与创造栏
        FluidRegistry.register(modEventBus);
        BlockRegistry.register(modEventBus);
        ItemRegistry.register(modEventBus);
        MenuRegistry.register(modEventBus);
        org.gwfx.zuoyanmod.recipe.RecipeRegistry.register(modEventBus);
        org.gwfx.zuoyanmod.item.FourDimensionalSpace.ATTACHMENTS.register(modEventBus);
        CreativeTabRegistry.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 装有 Curios API 时，把六件饰品注册为可佩戴的 Curio（佩戴提示、右键佩戴等默认行为）。
        // 无 Curios 时跳过：CuriosCompat 引用 Curios 类，绝不能在未判定前类加载。
        // 此时饰品走"放背包生效"的逻辑，判定见 util.AccessoryChecks。
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
            org.gwfx.zuoyanmod.compat.CuriosCompat.registerCurioItems();
        }
        LOGGER.info("ZuoyanMod initialized successfully!");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("ZuoyanMod server starting...");
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("ZuoyanMod client setup complete. User: {}", Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        public static void onRegisterMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
            event.register(MenuRegistry.VOID_RESONANCE_PUMP_MENU.get(), VoidResonancePumpScreen::new);
            event.register(MenuRegistry.KLEIN_BOTTLE_MENU.get(),
                    org.gwfx.zuoyanmod.client.KleinBottleScreen::new);
            event.register(MenuRegistry.MICRO_HADRON_COLLIDER_MENU.get(),
                    org.gwfx.zuoyanmod.client.MicroHadronColliderScreen::new);
        }
    }
}