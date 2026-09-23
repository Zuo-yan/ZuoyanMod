package org.gwfx.zuoyanmod.fluid;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.block.BlockRegistry;
import org.gwfx.zuoyanmod.item.ItemRegistry;

public final class FluidRegistry {

    private FluidRegistry() {}

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Zuoyanmod.MODID);

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, Zuoyanmod.MODID);

    public static final RegistryObject<DarkMatterFluidType> DARK_MATTER_FLUID_TYPE =
            FLUID_TYPES.register("dark_matter", DarkMatterFluidType::new);

    public static final RegistryObject<ForgeFlowingFluid.Source> DARK_MATTER =
            FLUIDS.register("dark_matter", FluidRegistry::createSource);

    public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_DARK_MATTER =
            FLUIDS.register("flowing_dark_matter", FluidRegistry::createFlowing);

    private static ForgeFlowingFluid.Properties properties() {
        return new ForgeFlowingFluid.Properties(DARK_MATTER_FLUID_TYPE, DARK_MATTER, FLOWING_DARK_MATTER)
                .bucket(() -> ItemRegistry.DARK_MATTER_BUCKET.get())
                .block(() -> BlockRegistry.DARK_MATTER_BLOCK.get())
                .slopeFindDistance(2)   // 侧向扩散极短
                .levelDecreasePerBlock(2)
                .tickRate(20)           // 每秒才流动一次（粘稠）
                .explosionResistance(100.0F);
    }

    private static ForgeFlowingFluid.Source createSource() {
        return new ForgeFlowingFluid.Source(properties());
    }

    private static ForgeFlowingFluid.Flowing createFlowing() {
        return new ForgeFlowingFluid.Flowing(properties());
    }

    public static void register(IEventBus modBus) {
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
    }
}
