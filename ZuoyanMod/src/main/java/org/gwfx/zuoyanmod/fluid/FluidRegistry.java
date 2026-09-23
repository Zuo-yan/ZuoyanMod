package org.gwfx.zuoyanmod.fluid;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.block.BlockRegistry;
import org.gwfx.zuoyanmod.item.ItemRegistry;

public final class FluidRegistry {

    private FluidRegistry() {}

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Zuoyanmod.MODID);

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, Zuoyanmod.MODID);

    public static final DeferredHolder<FluidType, DarkMatterFluidType> DARK_MATTER_FLUID_TYPE =
            FLUID_TYPES.register("dark_matter", DarkMatterFluidType::new);

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> DARK_MATTER =
            FLUIDS.register("dark_matter", FluidRegistry::createSource);

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_DARK_MATTER =
            FLUIDS.register("flowing_dark_matter", FluidRegistry::createFlowing);

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(DARK_MATTER_FLUID_TYPE, DARK_MATTER, FLOWING_DARK_MATTER)
                .bucket(() -> ItemRegistry.DARK_MATTER_BUCKET.get())
                .block(() -> BlockRegistry.DARK_MATTER_BLOCK.get())
                .slopeFindDistance(2)   // 侧向扩散极短
                .levelDecreasePerBlock(2)
                .tickRate(20)           // 每秒才流动一次（粘稠）
                .explosionResistance(100.0F);
    }

    private static BaseFlowingFluid.Source createSource() {
        return new BaseFlowingFluid.Source(properties());
    }

    private static BaseFlowingFluid.Flowing createFlowing() {
        return new BaseFlowingFluid.Flowing(properties());
    }

    public static void register(IEventBus modBus) {
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
    }
}
