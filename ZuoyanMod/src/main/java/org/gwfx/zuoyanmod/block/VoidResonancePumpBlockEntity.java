package org.gwfx.zuoyanmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.gwfx.zuoyanmod.item.ItemRegistry;
import org.gwfx.zuoyanmod.menu.VoidResonancePumpMenu;

/**
 * 虚空共振泵：把末地过剩物（末影珍珠 / 龙息 / 紫颂果）转化为暗物质粒子。
 * 运行条件：末地维度 + 正下方为空气，否则休眠（共振值冻结，不浪费燃料）。
 * 设计文档见 docs/void_resonance_pump.md。
 * <p>
 * 1.20.1 的方块实体存档是 {@code saveAdditional(CompoundTag)} / {@code load(CompoundTag)}
 * （26.x 换成了 ValueInput/ValueOutput）。
 */
public class VoidResonancePumpBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {

    /** 共振槽上限（10 颗末影珍珠） */
    public static final int MAX_RESONANCE = 1200;
    /** 单次投料冷却（tick），防止瞬间塞满跳过照料环节 */
    public static final int FEED_COOLDOWN = 20;
    /** 进度满值，产出 1 个暗物质粒子 */
    private static final float PROGRESS_TARGET = 100.0F;

    /** 各等级的产出速率（progress/tick）：L1 6 秒/个 → L5 1 秒/个 */
    private static final float[] RATES = {0.0F, 0.833F, 1.25F, 2.0F, 3.33F, 5.0F};
    /** 等级区间宽度 */
    private static final int LEVEL_STEP = 240;

    private int resonance;
    private float progress;
    private int feedCooldown;

    private final SimpleContainer inventory = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            VoidResonancePumpBlockEntity.this.setChanged();
        }
    };

    /** 1.20.1 惯例：给方块实体挂一个物品能力包装，供管道类模组抽取产物 */
    private final LazyOptional<IItemHandler> items = LazyOptional.of(() -> new InvWrapper(inventory));

    public VoidResonancePumpBlockEntity(BlockPos pos, BlockState state) {
        super(BlockRegistry.VOID_RESONANCE_PUMP_BE.get(), pos, state);
    }

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
            return items.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        items.invalidate();
    }

    // ===== 运行条件 =====

    public static boolean canRun(Level level, BlockPos pos) {
        return level.dimension() == Level.END && level.getBlockState(pos.below()).isAir();
    }

    // ===== 每 tick =====

    public static void tick(Level level, BlockPos pos, BlockState state, VoidResonancePumpBlockEntity be) {
        if (level.isClientSide) {
            return;
        }
        if (!canRun(level, pos)) {
            return; // 休眠：共振值冻结
        }

        be.feedFromInput();

        if (be.resonance <= 0) {
            return;
        }
        int lvl = be.resonanceLevel();
        be.progress += RATES[lvl];
        be.resonance = Math.max(0, be.resonance - 1);

        if (be.progress >= PROGRESS_TARGET) {
            be.progress -= PROGRESS_TARGET;
            be.outputParticle();
        }
        be.setChanged();

        if (level instanceof ServerLevel serverLevel && serverLevel.getRandom().nextInt(6) == 0) {
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    pos.getX() + 0.5D, pos.getY() + 0.9D, pos.getZ() + 0.5D,
                    2, 0.3D, 0.3D, 0.3D, 0.01D);
        }
    }

    private void feedFromInput() {
        if (feedCooldown > 0) {
            feedCooldown--;
            return;
        }
        ItemStack input = inventory.getItem(0);
        if (input.isEmpty()) {
            return;
        }
        int value = fuelValue(input);
        if (value <= 0 || resonance + value > MAX_RESONANCE) {
            return;
        }
        input.shrink(1);
        resonance += value;
        feedCooldown = FEED_COOLDOWN;
        setChanged();
    }

    /** 燃料价值：末影珍珠 120 / 龙息 80 / 紫颂果 40 */
    public static int fuelValue(ItemStack stack) {
        if (stack.is(Items.ENDER_PEARL)) {
            return 120;
        }
        if (stack.is(Items.DRAGON_BREATH)) {
            return 80;
        }
        if (stack.is(Items.CHORUS_FRUIT)) {
            return 40;
        }
        return 0;
    }

    private void outputParticle() {
        ItemStack existing = inventory.getItem(1);
        ItemStack particle = new ItemStack(ItemRegistry.DARK_MATTER_PARTICLE.get());
        if (existing.isEmpty()) {
            inventory.setItem(1, particle);
        } else if (existing.is(particle.getItem()) && existing.getCount() < existing.getMaxStackSize()) {
            existing.grow(1);
        } else {
            // 输出槽满：卡在满进度，燃料继续消耗（倒逼玩家来取）
            progress = PROGRESS_TARGET;
        }
        setChanged();
    }

    // ===== 状态查询 =====

    public int resonanceLevel() {
        if (resonance <= 0) {
            return 0;
        }
        return Math.min(5, (resonance - 1) / LEVEL_STEP + 1);
    }

    public int getResonance() {
        return resonance;
    }

    public float getProgress() {
        return progress;
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    // ===== GUI 数据同步 =====

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> resonanceLevel();
                case 1 -> (int) progress;
                case 2 -> resonance;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 1 -> progress = value;
                case 2 -> resonance = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public ContainerData getData() {
        return data;
    }

    // ===== 存档 =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Resonance", resonance);
        tag.putFloat("Progress", progress);
        tag.putInt("FeedCooldown", feedCooldown);
        // 1.20.1 的 SimpleContainer 没有 getItems()，手工把内容铺进 NonNullList 再整体保存
        net.minecraft.core.NonNullList<ItemStack> list = net.minecraft.core.NonNullList.create();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            list.add(inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        resonance = tag.getInt("Resonance");
        progress = tag.getFloat("Progress");
        feedCooldown = tag.getInt("FeedCooldown");
        inventory.clearContent();
        net.minecraft.core.NonNullList<ItemStack> list =
                net.minecraft.core.NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, list);
        for (int i = 0; i < list.size() && i < inventory.getContainerSize(); i++) {
            inventory.setItem(i, list.get(i));
        }
    }

    // ===== MenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.zuoyanmod.void_resonance_pump");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new VoidResonancePumpMenu(containerId, playerInventory, inventory, data);
    }
}
