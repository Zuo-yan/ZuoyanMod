package org.gwfx.zuoyanmod;

import com.mojang.logging.LogUtils;
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
        // 生物实体类型先于物品注册：刷怪蛋的 ENTITY_DATA 组件要引用 EntityType
        org.gwfx.zuoyanmod.entity.EntityRegistry.register(modEventBus);
        // 自定义 DataComponent 先于物品注册：物品默认组件要引用组件类型（当前无自定义组件，保留注册管线）
        org.gwfx.zuoyanmod.item.ComponentRegistry.register(modEventBus);
        ItemRegistry.register(modEventBus);
        MenuRegistry.register(modEventBus);
        org.gwfx.zuoyanmod.recipe.RecipeRegistry.register(modEventBus);
        org.gwfx.zuoyanmod.worldgen.WorldgenRegistry.register(modEventBus);
        // "多此一举"成就的自定义触发器（草原传送门点燃时调用，见 GrassPortalEventHandler）
        org.gwfx.zuoyanmod.advancement.GrassPortalTrigger.TRIGGERS.register(modEventBus);
        org.gwfx.zuoyanmod.item.FourDimensionalSpace.ATTACHMENTS.register(modEventBus);
        // 经验升级档案（基础能力等级 / 终极天赋 / 冷却）
        org.gwfx.zuoyanmod.upgrade.UpgradeData.ATTACHMENTS.register(modEventBus);
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
            LOGGER.info("ZuoyanMod client setup complete. User: {}", net.minecraft.client.Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        public static void onRegisterMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
            event.register(MenuRegistry.VOID_RESONANCE_PUMP_MENU.get(),
                    org.gwfx.zuoyanmod.client.VoidResonancePumpScreen::new);
            event.register(MenuRegistry.KLEIN_BOTTLE_MENU.get(),
                    org.gwfx.zuoyanmod.client.KleinBottleScreen::new);
            event.register(MenuRegistry.MICRO_HADRON_COLLIDER_MENU.get(),
                    org.gwfx.zuoyanmod.client.MicroHadronColliderScreen::new);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(org.gwfx.zuoyanmod.entity.EntityRegistry.RICK.get(),
                    org.gwfx.zuoyanmod.client.RickRenderer::new);
            // 因果律子弹：自定义 billboard 渲染器，绿色能量球贴图，始终正对摄像机
            event.registerEntityRenderer(org.gwfx.zuoyanmod.entity.EntityRegistry.CAUSALITY_BULLET.get(),
                    org.gwfx.zuoyanmod.client.CausalityBulletRenderer::new);
            // 原始黑洞：billboard 黑盘 + energySwirl 涡流，无模型，纯几何自绘
            event.registerEntityRenderer(org.gwfx.zuoyanmod.entity.EntityRegistry.PRIMORDIAL_BLACK_HOLE.get(),
                    org.gwfx.zuoyanmod.client.PrimordialBlackHoleRenderer::new);
            // 湮灭君主：人形放大 1.8 倍 + 金冠模型层
            event.registerEntityRenderer(org.gwfx.zuoyanmod.entity.EntityRegistry.VOID_MONARCH.get(),
                    org.gwfx.zuoyanmod.client.VoidMonarchRenderer::new);
            // 湮灭侍卫：普通人形，暗甲皮肤
            event.registerEntityRenderer(org.gwfx.zuoyanmod.entity.EntityRegistry.VOID_GUARD.get(),
                    org.gwfx.zuoyanmod.client.VoidGuardRenderer::new);
            // 暗物质螺栓：暗紫能量球 billboard
            event.registerEntityRenderer(org.gwfx.zuoyanmod.entity.EntityRegistry.VOID_BOLT.get(),
                    org.gwfx.zuoyanmod.client.VoidBoltRenderer::new);
            // 超级电能机枪豌豆与电能豌豆子弹
            event.registerEntityRenderer(org.gwfx.zuoyanmod.entity.EntityRegistry.SUPER_ELECTRIC_GATLING_PEA.get(),
                    org.gwfx.zuoyanmod.client.SuperElectricGatlingPeaRenderer::new);
            event.registerEntityRenderer(org.gwfx.zuoyanmod.entity.EntityRegistry.ELECTRO_PEA_BULLET.get(),
                    org.gwfx.zuoyanmod.client.ElectroPeaBulletRenderer::new);
        }

        @SubscribeEvent
        public static void onRegisterLayerDefinitions(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(org.gwfx.zuoyanmod.client.RickModelLayers.RICK_BODY,
                    org.gwfx.zuoyanmod.client.RickModelLayers::createBodyLayer);
            event.registerLayerDefinition(org.gwfx.zuoyanmod.client.BossModelLayers.VOID_MONARCH_BODY,
                    org.gwfx.zuoyanmod.client.BossModelLayers::createMonarchBodyLayer);
            event.registerLayerDefinition(org.gwfx.zuoyanmod.client.BossModelLayers.VOID_GUARD_BODY,
                    org.gwfx.zuoyanmod.client.BossModelLayers::createGuardBodyLayer);
            event.registerLayerDefinition(org.gwfx.zuoyanmod.client.PeaModelLayers.SUPER_ELECTRIC_GATLING_PEA,
                    org.gwfx.zuoyanmod.client.SuperElectricGatlingPeaModel::createBodyLayer);
            event.registerLayerDefinition(org.gwfx.zuoyanmod.client.PeaModelLayers.ELECTRO_PEA_BULLET,
                    org.gwfx.zuoyanmod.client.ElectroPeaBulletModel::createBodyLayer);
        }
    }
}
