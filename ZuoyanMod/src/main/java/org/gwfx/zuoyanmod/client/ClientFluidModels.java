package org.gwfx.zuoyanmod.client;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.fluid.FluidRegistry;

/**
 * 26.3 流体渲染改为 FluidModel 注册（原 IClientFluidTypeExtensions 贴图方法已移除）。
 * 液态暗物质：深紫黑无染色流体（tint=null，贴图原色渲染）。
 */
@EventBusSubscriber(modid = Zuoyanmod.MODID, value = Dist.CLIENT)
public final class ClientFluidModels {

    private ClientFluidModels() {}

    @SubscribeEvent
    public static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        FluidModel.Unbaked model = new FluidModel.Unbaked(
                new Material(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "block/dark_matter_still")),
                new Material(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "block/dark_matter_flow")),
                null, // 无 overlay
                null  // 无染色
        );
        event.register(model, FluidRegistry.DARK_MATTER, FluidRegistry.FLOWING_DARK_MATTER);
    }
}
