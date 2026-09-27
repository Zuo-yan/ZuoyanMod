package org.gwfx.zuoyanmod.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.fluid.FluidRegistry;

/**
 * 液态暗物质的客户端外观。
 *
 * <p>1.21.1 NeoForge 的流体贴图走 {@link IClientFluidTypeExtensions}
 * （{@code stillTexture / flowingTexture / overlayTexture / tint}），
 * 经 {@link RegisterClientExtensionsEvent} 挂到流体类型上，不需要改动 fluid 包。
 *
 * <p>液态暗物质：深紫黑无染色流体（不覆盖 {@code getTintColor}，默认 0xFFFFFFFF 即原色渲染）。
 */
@EventBusSubscriber(modid = Zuoyanmod.MODID, value = Dist.CLIENT)
public final class ClientFluidModels {

    private static final ResourceLocation STILL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Zuoyanmod.MODID, "block/dark_matter_still");
    private static final ResourceLocation FLOWING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Zuoyanmod.MODID, "block/dark_matter_flow");

    private ClientFluidModels() {}

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING_TEXTURE;
            }

            // overlay 与染色都用默认实现：getOverlayTexture() 返回 null（无 overlay），
            // getTintColor() 返回 0xFFFFFFFF（无染色，贴图原色渲染）
        }, FluidRegistry.DARK_MATTER_FLUID_TYPE);
    }
}
