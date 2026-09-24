package org.gwfx.zuoyanmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * 微型强子对撞机：桌面级粒子对撞机，两束材料粒子对撞产出高能产物（奇点核心）。
 * 充能 = 红石信号（hasNeighborSignal），断电时进度冻结（不回退，回来接着撞）。
 * 结构沿用虚空共振泵：BaseEntityBlock + 手动 FACING。
 *
 * <p>1.20.1 适配：交互入口是 {@code use(...)}（26.x 改名 useWithoutItem）；
 * 破坏掉落钩子是 {@code onRemove}。</p>
 */
public class MicroHadronColliderBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public MicroHadronColliderBlock(Properties properties) {
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
        if (level.getBlockEntity(pos) instanceof MicroHadronColliderBlockEntity collider) {
            player.openMenu(collider);
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MicroHadronColliderBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level instanceof ServerLevel
                ? createTickerHelper(type,
                        (BlockEntityType<MicroHadronColliderBlockEntity>) BlockRegistry.MICRO_HADRON_COLLIDER_BE.get(),
                        MicroHadronColliderBlockEntity::tick)
                : null;
    }

    /** 破坏时掉落内含物（1.20.1 是 onRemove） */
    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!movedByPiston && state.getBlock() != newState.getBlock()
                && level.getBlockEntity(pos) instanceof MicroHadronColliderBlockEntity collider) {
            Containers.dropContents(level, pos, collider.getInventory());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
