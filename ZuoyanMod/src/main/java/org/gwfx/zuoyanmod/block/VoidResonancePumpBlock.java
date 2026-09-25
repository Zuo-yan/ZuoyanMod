package org.gwfx.zuoyanmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * 虚空共振泵：末地悬空处把过剩物（末影珍珠 / 龙息 / 紫颂果）转化为暗物质粒子。
 * 带水平朝向：放置时正面（观察窗）朝向玩家，四面贴图各不相同。
 * <p>
 * 1.20.1 的方块交互入口是 {@code use(BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)}
 * （26.x 改名为 useWithoutItem 且变成 protected）；破坏掉落钩子是 {@code onRemove}。
 */
public class VoidResonancePumpBlock extends BaseEntityBlock {

    /** 水平朝向，与熔炉同源（BaseEntityBlock 不能再继承 HorizontalDirectionalBlock，故手动取属性） */
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public VoidResonancePumpBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // 1.20.1 里父类这两个方法是 public，覆盖时不能收窄可见性
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof VoidResonancePumpBlockEntity pump)) {
            return InteractionResult.PASS;
        }
        if (!VoidResonancePumpBlockEntity.canRun(level, pos)) {
            player.sendSystemMessage(level.dimension() != Level.END
                    ? Component.translatable("gui.zuoyanmod.void_pump.error.end_only")
                    : Component.translatable("gui.zuoyanmod.void_pump.error.must_be_void"));
            return InteractionResult.SUCCESS;
        }
        player.openMenu(pump);
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VoidResonancePumpBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level instanceof ServerLevel serverLevel
                ? createTickerHelper(type,
                        (net.minecraft.world.level.block.entity.BlockEntityType<VoidResonancePumpBlockEntity>)
                                BlockRegistry.VOID_RESONANCE_PUMP_BE.get(),
                        (lvl, pos, blockState, be) ->
                                VoidResonancePumpBlockEntity.tick(serverLevel, pos, blockState, be))
                : null;
    }

    /** 破坏时掉落内含物（1.20.1 是 onRemove；26.x 改名 affectNeighborsAfterRemoval） */
    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!movedByPiston && state.getBlock() != newState.getBlock()
                && level.getBlockEntity(pos) instanceof VoidResonancePumpBlockEntity pump) {
            Containers.dropContents(level, pos, pump.getInventory());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
