package org.gwfx.zuoyanmod.world;

import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.apache.commons.lang3.mutable.MutableInt;
import org.gwfx.zuoyanmod.block.BlockRegistry;
import org.jspecify.annotations.Nullable;

/**
 * 草原传送门的门框形状检测，算法照抄 26.3 原版 {@code PortalShape}，
 * 只替换两个谓词：框体 = 草原门框，内部可穿透 = 空气 / 火 / 草原传送门。
 *
 * <p>不直接复用原版类的原因：原版 FRAME 谓词硬编码下界合金门框标签，
 * 内部 EMPTY 谓词硬编码下界传送门，三处都无法从外部注入。
 *
 * <p>用法与原版一致：点燃位置（打火石将生成火的那个方块）跑
 * {@link #findEmptyPortalShape}，合法则 {@link #createPortalBlocks} 填充；
 * 门框被拆时传送门方块在 updateShape 里用 {@link #findAnyShape} 重校验。
 */
public final class GrassPortalShape {
    private static final int MIN_WIDTH = 2;
    public static final int MAX_WIDTH = 21;
    private static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 21;

    /** 框体方块：草原门框。 */
    private static final Predicate<BlockState> FRAME = state -> state.is(BlockRegistry.GRASS_PORTAL_FRAME.get());

    private final Direction.Axis axis;
    private final Direction rightDir;
    private final int numPortalBlocks;
    private final BlockPos bottomLeft;
    private final int height;
    private final int width;

    private GrassPortalShape(Direction.Axis axis, int portalBlockCount, Direction rightDir, BlockPos bottomLeft, int width, int height) {
        this.axis = axis;
        this.numPortalBlocks = portalBlockCount;
        this.rightDir = rightDir;
        this.bottomLeft = bottomLeft;
        this.width = width;
        this.height = height;
    }

    /**
     * 从点火位置找完整的空门框（内部还没有传送门方块）。
     * 与原版不同：这里不需要调用方指定轴，X/Z 两个方向都试一遍。
     */
    public static Optional<GrassPortalShape> findEmptyPortalShape(LevelAccessor level, BlockPos pos) {
        Optional<GrassPortalShape> alongX = tryAxis(level, pos, Direction.Axis.X);
        if (alongX.isPresent()) {
            return alongX;
        }
        return tryAxis(level, pos, Direction.Axis.Z);
    }

    private static Optional<GrassPortalShape> tryAxis(LevelAccessor level, BlockPos pos, Direction.Axis axis) {
        return Optional.of(findAnyShape(level, pos, axis)).filter(shape -> shape.isValid() && shape.numPortalBlocks == 0);
    }

    /** 从任意位置测量所在（或所在空洞的）门框形状；不合法时宽高为 0。供 updateShape 重校验复用。 */
    public static GrassPortalShape findAnyShape(BlockGetter level, BlockPos pos, Direction.Axis axis) {
        Direction rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        BlockPos bottomLeft = calculateBottomLeft(level, rightDir, pos);
        if (bottomLeft == null) {
            return new GrassPortalShape(axis, 0, rightDir, pos, 0, 0);
        }

        int width = calculateWidth(level, bottomLeft, rightDir);
        if (width == 0) {
            return new GrassPortalShape(axis, 0, rightDir, bottomLeft, 0, 0);
        }

        MutableInt portalBlockCount = new MutableInt();
        int height = calculateHeight(level, bottomLeft, rightDir, width, portalBlockCount);
        return new GrassPortalShape(axis, portalBlockCount.intValue(), rightDir, bottomLeft, width, height);
    }

    private static @Nullable BlockPos calculateBottomLeft(BlockGetter level, Direction rightDir, BlockPos pos) {
        int minY = Math.max(level.getMinY(), pos.getY() - MAX_HEIGHT);

        while (pos.getY() > minY && isEmpty(level.getBlockState(pos.below()))) {
            pos = pos.below();
        }

        Direction leftDir = rightDir.getOpposite();
        int edge = getDistanceUntilEdgeAboveFrame(level, pos, leftDir) - 1;
        return edge < 0 ? null : pos.relative(leftDir, edge);
    }

    private static int calculateWidth(BlockGetter level, BlockPos bottomLeft, Direction rightDir) {
        int width = getDistanceUntilEdgeAboveFrame(level, bottomLeft, rightDir);
        return width >= MIN_WIDTH && width <= MAX_WIDTH ? width : 0;
    }

    private static int getDistanceUntilEdgeAboveFrame(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int width = 0; width <= MAX_WIDTH; width++) {
            cursor.set(pos).move(direction, width);
            BlockState state = level.getBlockState(cursor);
            if (!isEmpty(state)) {
                if (FRAME.test(state)) {
                    return width;
                }
                break;
            }

            BlockState belowState = level.getBlockState(cursor.move(Direction.DOWN));
            if (!FRAME.test(belowState)) {
                break;
            }
        }

        return 0;
    }

    private static int calculateHeight(BlockGetter level, BlockPos bottomLeft, Direction rightDir, int width, MutableInt portalBlockCount) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int height = getDistanceUntilTop(level, bottomLeft, rightDir, cursor, width, portalBlockCount);
        return height >= MIN_HEIGHT && height <= MAX_HEIGHT && hasTopFrame(level, bottomLeft, rightDir, cursor, width, height) ? height : 0;
    }

    private static boolean hasTopFrame(BlockGetter level, BlockPos bottomLeft, Direction rightDir, BlockPos.MutableBlockPos cursor, int width, int height) {
        for (int i = 0; i < width; i++) {
            BlockPos.MutableBlockPos framePos = cursor.set(bottomLeft).move(Direction.UP, height).move(rightDir, i);
            if (!FRAME.test(level.getBlockState(framePos))) {
                return false;
            }
        }

        return true;
    }

    private static int getDistanceUntilTop(
        BlockGetter level, BlockPos bottomLeft, Direction rightDir, BlockPos.MutableBlockPos cursor, int width, MutableInt portalBlockCount
    ) {
        for (int height = 0; height < MAX_HEIGHT; height++) {
            cursor.set(bottomLeft).move(Direction.UP, height).move(rightDir, -1);
            if (!FRAME.test(level.getBlockState(cursor))) {
                return height;
            }

            cursor.set(bottomLeft).move(Direction.UP, height).move(rightDir, width);
            if (!FRAME.test(level.getBlockState(cursor))) {
                return height;
            }

            for (int i = 0; i < width; i++) {
                cursor.set(bottomLeft).move(Direction.UP, height).move(rightDir, i);
                BlockState state = level.getBlockState(cursor);
                if (!isEmpty(state)) {
                    return height;
                }

                if (state.is(BlockRegistry.GRASS_PORTAL.get())) {
                    portalBlockCount.increment();
                }
            }
        }

        return MAX_HEIGHT;
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir() || state.is(BlockTags.FIRE) || state.is(BlockRegistry.GRASS_PORTAL.get());
    }

    public boolean isValid() {
        return this.width >= MIN_WIDTH && this.width <= MAX_WIDTH && this.height >= MIN_HEIGHT && this.height <= MAX_HEIGHT;
    }

    /** 按测得的形状把内部填满草原传送门方块（axis 随门框朝向）。 */
    public void createPortalBlocks(LevelAccessor level) {
        BlockState portalState = BlockRegistry.GRASS_PORTAL.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_AXIS, this.axis);
        BlockPos.betweenClosed(this.bottomLeft, this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.rightDir, this.width - 1))
            .forEach(pos -> level.setBlock(pos, portalState, 18));
    }

    /** 内部是否已被传送门方块填满（updateShape 重校验用：少一块就塌）。 */
    public boolean isComplete() {
        return this.isValid() && this.numPortalBlocks == this.width * this.height;
    }
}
