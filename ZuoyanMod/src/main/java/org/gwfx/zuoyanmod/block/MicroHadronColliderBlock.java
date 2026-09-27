package org.gwfx.zuoyanmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
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

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;

/**
 * 微型强子对撞机：桌面级粒子对撞机，两束材料粒子对撞产出高能产物（奇点核心）。
 * 充能 = 红石信号（hasNeighborSignal），断电时进度冻结（不回退，回来接着撞）。
 * 结构沿用虚空共振泵：BaseEntityBlock + 手动 FACING（1.21.1 不能多继承）。
 */
public class MicroHadronColliderBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final MapCodec<MicroHadronColliderBlock> CODEC = simpleCodec(MicroHadronColliderBlock::new);

    public MicroHadronColliderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MicroHadronColliderBlockEntity collider) {
            player.openMenu(collider);
        }
        return InteractionResult.CONSUME;
    }

    
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MicroHadronColliderBlockEntity(pos, state);
    }

    
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level instanceof ServerLevel serverLevel
                ? createTickerHelper(type, BlockRegistry.MICRO_HADRON_COLLIDER_BE.get(),
                        (lvl, pos, blockState, be) -> MicroHadronColliderBlockEntity.tick(serverLevel, pos, blockState, be))
                : null;
    }

    /** 破坏时掉落内含物（1.21.1 用 onRemove；活塞推动 movedByPiston=true 不掉落） */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!movedByPiston && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MicroHadronColliderBlockEntity collider) {
            Containers.dropContents(level, pos, collider.getInventory());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
