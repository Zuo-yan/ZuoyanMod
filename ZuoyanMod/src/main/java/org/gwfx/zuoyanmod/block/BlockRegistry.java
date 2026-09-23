package org.gwfx.zuoyanmod.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import java.util.Set;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.fluid.DarkMatterLiquidBlock;
import org.gwfx.zuoyanmod.fluid.FluidRegistry;

public final class BlockRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Zuoyanmod.MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Zuoyanmod.MODID);

    // ===== 紫金矿石（主世界深层稀有，掉落经验） =====
    public static final DeferredBlock<Block> VIOLET_GOLD_ORE = BLOCKS.registerBlock(
            "violet_gold_ore",
            props -> new DropExperienceBlock(UniformInt.of(3, 7), props),
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 3.0F)
    );

    public static final DeferredBlock<Block> DEEPSLATE_VIOLET_GOLD_ORE = BLOCKS.registerBlock(
            "deepslate_violet_gold_ore",
            props -> new DropExperienceBlock(UniformInt.of(3, 7), props),
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .requiresCorrectToolForDrops()
                    .strength(4.5F, 3.0F)
                    .sound(SoundType.DEEPSLATE)
    );

    // ===== 紫金块（存储方块） =====
    public static final DeferredBlock<Block> VIOLET_GOLD_BLOCK = BLOCKS.registerBlock(
            "violet_gold_block",
            Block::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
    );

    // ===== 液态暗物质（高密度奇异流体，见 DarkMatterEventHandler 惩罚逻辑） =====
    public static final DeferredBlock<DarkMatterLiquidBlock> DARK_MATTER_BLOCK = BLOCKS.registerBlock(
            "dark_matter",
            props -> new DarkMatterLiquidBlock(FluidRegistry.DARK_MATTER.get(), props),
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .replaceable()
                    .noCollision()
                    .strength(100.0F)
                    .pushReaction(PushReaction.POPPED)
                    .noLootTable()
                    .liquid()
                    .sound(SoundType.EMPTY)
    );

    // ===== 虚空共振泵（末地悬空处把过剩物转化为暗物质粒子） =====
    public static final DeferredBlock<VoidResonancePumpBlock> VOID_RESONANCE_PUMP = BLOCKS.registerBlock(
            "void_resonance_pump",
            VoidResonancePumpBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .requiresCorrectToolForDrops()
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL)
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VoidResonancePumpBlockEntity>> VOID_RESONANCE_PUMP_BE =
            BLOCK_ENTITY_TYPES.register("void_resonance_pump",
                    () -> new BlockEntityType<>(VoidResonancePumpBlockEntity::new, Set.of(VOID_RESONANCE_PUMP.get())));

    private BlockRegistry() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
    }
}
