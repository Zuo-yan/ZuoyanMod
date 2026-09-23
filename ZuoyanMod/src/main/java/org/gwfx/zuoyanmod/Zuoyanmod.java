package org.gwfx.zuoyanmod;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.gwfx.zuoyanmod.block.BlockRegistry;
import org.gwfx.zuoyanmod.effect.EffectRegistry;
import org.gwfx.zuoyanmod.fluid.FluidRegistry;
import org.gwfx.zuoyanmod.item.CreativeTabRegistry;
import org.gwfx.zuoyanmod.item.FourDimensionalSpaceCapability;
import org.gwfx.zuoyanmod.item.ItemRegistry;
import org.gwfx.zuoyanmod.menu.MenuRegistry;
import org.gwfx.zuoyanmod.loot.LootModifiers;
import org.gwfx.zuoyanmod.network.PacketHandler;
import org.gwfx.zuoyanmod.sound.SoundRegistry;
import org.slf4j.Logger;

/**
 * ZuoyanMod 主入口（1.20.1 Forge 分支）。
 *
 * <p>与 26.3 NeoForge 主线的差别：Forge 没有 Mod 构造注入（IEventBus/ModContainer），
 * 通过 {@link FMLJavaModLoadingContext#get()} 取 mod 事件总线；
 * 菜单 Screen 的注册搬到客户端 client setup；网络见 {@link PacketHandler}。
 */
@Mod(Zuoyanmod.MODID)
public class Zuoyanmod {
    public static final String MODID = "zuoyanmod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Zuoyanmod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        // 挂载音效和药水效果注册表
        SoundRegistry.SOUND_EVENTS.register(modEventBus);
        EffectRegistry.EFFECTS.register(modEventBus);

        // 注册方块、物品系统与创造栏
        FluidRegistry.register(modEventBus);
        BlockRegistry.register(modEventBus);
        ItemRegistry.register(modEventBus);
        MenuRegistry.register(modEventBus);
        CreativeTabRegistry.register(modEventBus);
        // 全局战利品修改器（add_table 等）
        LootModifiers.LOOT_MODIFIERS.register(modEventBus);

        // Forge 的游戏事件总线：事件处理器都走 @Mod.EventBusSubscriber 自动挂载
        MinecraftForge.EVENT_BUS.register(this);

        Config.register();

        // SimpleChannel 需要在 mod 构造期注册（内部走 channel lock）
        PacketHandler.register();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("ZuoyanMod initialized successfully!");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        // 玩家离开时清掉内存态（FatalProtection 等静态缓存）
        event.getServer().getPlayerList().getPlayers().forEach(
                player -> org.gwfx.zuoyanmod.event.FatalProtection.clear(player.getUUID()));
    }

    /**
     * 客户端专属初始化：菜单 Screen 与按键注册。
     * Forge 没有按事件分发 Screen 的专用事件，走 FMLClientSetupEvent。
     */
    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                net.minecraft.client.gui.screens.MenuScreens.register(
                        MenuRegistry.VOID_RESONANCE_PUMP_MENU.get(),
                        org.gwfx.zuoyanmod.client.VoidResonancePumpScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(
                        MenuRegistry.KLEIN_BOTTLE_MENU.get(),
                        org.gwfx.zuoyanmod.client.KleinBottleScreen::new);
                LOGGER.info("ZuoyanMod client setup complete. User: {}", Minecraft.getInstance().getUser().getName());
            });
        }

        @SubscribeEvent
        public static void onRegisterKeys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
            event.register(org.gwfx.zuoyanmod.event.RealmKeybindHandler.TOGGLE_REALM);
            event.register(org.gwfx.zuoyanmod.event.KleinBottleKeyHandler.OPEN_KLEIN_BOTTLE);
        }
    }
}
