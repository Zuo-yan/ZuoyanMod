package org.gwfx.zuoyanmod.block;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.gwfx.zuoyanmod.world.GrassPortalShape;
import org.gwfx.zuoyanmod.world.RealmTransitionManager;

/**
 * 草原传送门方块（对应原版下界传送门的位置）。
 *
 * <h2>为什么不用原版 {@code Portal} 接口</h2>
 * 原版接口的 {@code getPortalDestination} 返回固定目标坐标，而本模组进入超平坦世界
 * 走 {@link RealmTransitionManager} 的双向位置记忆（Home 键同款管线）：
 * 进门记录主世界坐标、出门回到上次记录点。所以传送交给 {@code toggle}，
 * 本方块只负责"站够时间就触发"。
 *
 * <h2>站立计时</h2>
 * {@link #entityInside} 每 tick 累计玩家在门内的连续时长，阈值读原版游戏规则
 * （生存默认 80 tick / 创造 0，和下界门同款规则键）。连续性靠"上一 tick 判定"：
 * 中途离开再进来会从零重计。传送后进入 {@link #COOLDOWN_TICKS} 冷却，
 * 防止从超平坦世界回来时还站在门里被立刻送回去。
 */
public class GrassPortalBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    /** 门体形状：居中的 4×16×4 细柱（1.21.1 用 Block.box 直接描述，两轴同款居中形状）。 */
    private static final VoxelShape SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);

    /** 传送成功后的冷却（游戏刻）：比默认站立阈值长，足够玩家走出回程门。 */
    private static final long COOLDOWN_TICKS = 300;

    private static final Map<UUID, Long> COOLDOWN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, PortalProgress> STAND_PROGRESS = new ConcurrentHashMap<>();

    private record PortalProgress(long lastTick, int ticks) {}

    public GrassPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * 门框被拆时整扇门塌掉：除了"更新方向就在传送门平面内"或"邻居也是传送门"，
     * 其余情况按当前轴向重测形状，测不出完整的一扇就变空气。
     */
    @Override
    protected BlockState updateShape(
        BlockState state,
        Direction directionToNeighbour,
        BlockState neighbourState,
        LevelAccessor level,
        BlockPos pos,
        BlockPos neighbourPos
    ) {
        Direction.Axis updateAxis = directionToNeighbour.getAxis();
        Direction.Axis axis = state.getValue(AXIS);
        boolean wrongAxis = axis != updateAxis && updateAxis.isHorizontal();
        return !wrongAxis && !neighbourState.is(this) && !GrassPortalShape.findAnyShape(level, pos, axis).isComplete()
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(state, directionToNeighbour, neighbourState, level, pos, neighbourPos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        long now = serverLevel.getGameTime();
        Long cooldownUntil = COOLDOWN_UNTIL.get(player.getUUID());
        if (cooldownUntil != null && now < cooldownUntil) {
            return;
        }

        int threshold = portalTransitionTime(serverLevel, player);
        PortalProgress progress = STAND_PROGRESS.get(player.getUUID());
        int ticks = progress != null && progress.lastTick() + 1 == now ? progress.ticks() + 1 : 1;
        if (ticks < threshold) {
            STAND_PROGRESS.put(player.getUUID(), new PortalProgress(now, ticks));
            return;
        }

        STAND_PROGRESS.remove(player.getUUID());
        COOLDOWN_UNTIL.put(player.getUUID(), now + COOLDOWN_TICKS);
        RealmTransitionManager.toggle(player);
    }

    /** 站立阈值与原版下界门同款游戏规则：创造/生存分别读对应键，默认 0 / 80 tick。 */
    private static int portalTransitionTime(ServerLevel level, ServerPlayer player) {
        return Math.max(0, level.getGameRules().getInt(
            player.getAbilities().invulnerable ? GameRules.RULE_PLAYERS_NETHER_PORTAL_CREATIVE_DELAY : GameRules.RULE_PLAYERS_NETHER_PORTAL_DEFAULT_DELAY));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                SoundEvents.PORTAL_AMBIENT,
                SoundSource.BLOCKS,
                0.5F,
                random.nextFloat() * 0.4F + 0.8F,
                false
            );
        }

        for (int i = 0; i < 4; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            double xa = (random.nextFloat() - 0.5) * 0.5;
            double ya = (random.nextFloat() - 0.5) * 0.5;
            double za = (random.nextFloat() - 0.5) * 0.5;
            int flip = random.nextInt(2) * 2 - 1;
            if (!level.getBlockState(pos.west()).is(this) && !level.getBlockState(pos.east()).is(this)) {
                x = pos.getX() + 0.5 + 0.25 * flip;
                xa = random.nextFloat() * 2.0F * flip;
            } else {
                z = pos.getZ() + 0.5 + 0.25 * flip;
                za = random.nextFloat() * 2.0F * flip;
            }

            // 绿色荧光粒子：和"草原"主题呼应（下界门用的是紫色 PORTAL 粒子）
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, xa, ya, za);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> state.setValue(AXIS,
                state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
            default -> state;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }
}
