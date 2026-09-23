package org.gwfx.zuoyanmod.fluid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import org.gwfx.zuoyanmod.Zuoyanmod;

/**
 * 液态暗物质：极高密度与引力场的奇异流体。
 * 物理层：无流动推力、极粘滞、不可形成无限源、船无法行驶、寻路视为危险。
 * 惩罚层（见 DarkMatterEventHandler）：强制下沉、真实伤害、剥夺增益、耐久粉碎、附魔降级。
 * 穿戴全套圣辉套装（紫金神装）可在其中正常游动。
 * <p>
 * 1.20.1 的流体贴图注册走 {@link IClientFluidTypeExtensions}（通过
 * {@code getRenderProperties()} 挂到 FluidType 上）；26.3 改成了 FluidModel 注册事件
 * （见主线 client/ClientFluidModels），这里对应回退。
 */
public class DarkMatterFluidType extends FluidType {

    public DarkMatterFluidType() {
        super(Properties.create()
                .descriptionId("fluid.zuoyanmod.dark_matter")
                .motionScale(0.0D)          // 无流动推力（不推实体）
                .canPushEntity(false)        // 粘滞场，不推动实体
                .canSwim(true)               // 圣辉套装穿戴者可游动（其余在事件层强制下沉）
                .canDrown(false)             // 窒息由事件手动控制
                .fallDistanceModifier(0.0F)  // 坠入不减免摔落伤害
                .canConvertToSource(false)   // 不可形成无限源
                .supportsBoating(false)      // 船无法行驶
                .pathType(BlockPathTypes.LAVA) // 寻路视为危险液体（1.20.1 类名是 BlockPathTypes）
                .lightLevel(0)
                .density(4000)               // 极高密度：强下沉
                .viscosity(6000)             // 极粘滞：流动缓慢
                .temperature(300)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
        );
    }

    /** 1.20.1：流体渲染属性通过 initializeClient 挂 IClientFluidTypeExtensions（26.3 的 FluidModel 事件在 1.20.1 不存在） */
    @Override
    public void initializeClient(java.util.function.Consumer<IClientFluidTypeExtensions> extensions) {
        extensions.accept(new IClientFluidTypeExtensions() {
            private static final String STILL = "block/dark_matter_still";
            private static final String FLOW = "block/dark_matter_flow";

            @Override
            public ResourceLocation getStillTexture() {
                return new ResourceLocation(Zuoyanmod.MODID, STILL);
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return new ResourceLocation(Zuoyanmod.MODID, FLOW);
            }
        });
    }
}
