package org.gwfx.zuoyanmod.fluid;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * 液态暗物质：极高密度与引力场的奇异流体。
 * 物理层：无流动推力、极粘滞、不可形成无限源、船无法行驶、寻路视为危险。
 * 惩罚层（见 DarkMatterEventHandler）：强制下沉、真实伤害、剥夺增益、耐久粉碎、附魔降级。
 * 穿戴全套圣辉套装（紫金神装）可在其中正常游动。
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
                .pathType(PathType.LAVA)     // 寻路视为危险液体
                .lightLevel(0)
                .density(4000)               // 极高密度：强下沉
                .viscosity(6000)             // 极粘滞：流动缓慢
                .temperature(300)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
        );
    }
}
