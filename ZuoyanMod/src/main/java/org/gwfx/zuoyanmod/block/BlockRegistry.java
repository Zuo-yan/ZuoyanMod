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

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.fluid.DarkMatterLiquidBlock;
import org.gwfx.zuoyanmod.fluid.FluidRegistry;

public final class BlockRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, Zuoyanmod.MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Zuoyanmod.MODID);

    // ===== 紫金矿石（主世界深层稀有，掉落经验） =====
    public static final RegistryObject<Block> VIOLET_GOLD_ORE = BLOCKS.register(
            "violet_gold_ore",
            // 1.20.1 的参数顺序是 (Properties, IntProvider)（26.x 反过来）
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 3.0F), UniformInt.of(3, 7))
    );

    public static final RegistryObject<Block> DEEPSLATE_VIOLET_GOLD_ORE = BLOCKS.register(
            "deepslate_violet_gold_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .requiresCorrectToolForDrops()
                    .strength(4.5F, 3.0F)
                    .sound(SoundType.DEEPSLATE), UniformInt.of(3, 7))
    );

    // ===== 紫金块（存储方块） =====
    public static final RegistryObject<Block> VIOLET_GOLD_BLOCK = BLOCKS.register(
            "violet_gold_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL))
    );

    // ===== 液态暗物质（高密度奇异流体，见 DarkMatterEventHandler 惩罚逻辑） =====
    public static final RegistryObject<DarkMatterLiquidBlock> DARK_MATTER_BLOCK = BLOCKS.register(
            "dark_matter",
            () -> new DarkMatterLiquidBlock(
                    (net.minecraft.world.level.material.FlowingFluid) FluidRegistry.DARK_MATTER.get(),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .replaceable()
                            .noCollission()   // 1.20.1 拼写是 noCollission（双 l）
                            .strength(100.0F)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .liquid()
                            .sound(SoundType.EMPTY))
    );

    // ===== 虚空共振泵（末地悬空处把过剩物转化为暗物质粒子） =====
    public static final RegistryObject<VoidResonancePumpBlock> VOID_RESONANCE_PUMP = BLOCKS.register(
            "void_resonance_pump",
            () -> new VoidResonancePumpBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .requiresCorrectToolForDrops()
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL))
    );

    // 1.20.1 的 BlockEntityType 泛型在 RegistryObject 里要用通配（BlockEntity 基类的构造参数也是 BlockEntityType<?>）；
    // 构造器第三个参数是 datafixer Type，模组惯例传 null
    public static final RegistryObject<BlockEntityType<?>> VOID_RESONANCE_PUMP_BE =
            BLOCK_ENTITY_TYPES.register("void_resonance_pump",
                    () -> new BlockEntityType<>(VoidResonancePumpBlockEntity::new, Set.of(VOID_RESONANCE_PUMP.get()), null));

    // ===== 微型强子对撞机（红石充能，双粒子束对撞产出高能产物） =====
    public static final RegistryObject<MicroHadronColliderBlock> MICRO_HADRON_COLLIDER = BLOCKS.register(
            "micro_hadron_collider",
            () -> new MicroHadronColliderBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL))
    );

    public static final RegistryObject<BlockEntityType<?>> MICRO_HADRON_COLLIDER_BE =
            BLOCK_ENTITY_TYPES.register("micro_hadron_collider",
                    () -> new BlockEntityType<>(MicroHadronColliderBlockEntity::new,
                            Set.of(MICRO_HADRON_COLLIDER.get()), null));

    private BlockRegistry() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
    }
}
