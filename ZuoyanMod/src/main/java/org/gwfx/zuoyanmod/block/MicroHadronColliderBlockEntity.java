package org.gwfx.zuoyanmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.gwfx.zuoyanmod.menu.MicroHadronColliderMenu;
import org.gwfx.zuoyanmod.recipe.MicroCollisionRecipe;
import org.gwfx.zuoyanmod.recipe.RecipeRegistry;

import java.util.Optional;

/**
 * 微型强子对撞机 BlockEntity。
 * 工作流：红石充能 → 两格材料匹配 micro_collision 配方 → 进度推进（duration 由配方 JSON 决定）→ 产出。
 * - 断电：进度冻结（不回退，回来接着撞），配方不匹配时进度清零。
 * - 声光：起撞 PORTAL_TRIGGER，工作中 PORTAL 粒子，出产物 ANVIL_USE + REVERSE_PORTAL 粒子爆发。
 * - 同步：进度/时长/充能状态走 ContainerData（原版自动发 ClientboundContainerSetDataPacket）。
 *
 * <p>1.20.1 适配：方块实体存档是 {@code saveAdditional(CompoundTag)} / {@code load(CompoundTag)}；
 * ticker 的 level 形参是 {@link Level}（在方法体里再判 ServerLevel）；
 * 配方查询走 {@code level.getRecipeManager().getRecipeFor(...)}（返回裸配方，没有 RecipeHolder 包装）。</p>
 */
public class MicroHadronColliderBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {

    /** 槽位：0 = 粒子束 A，1 = 粒子束 B，2 = 产物 */
    public static final int SLOT_BEAM_A = 0;
    public static final int SLOT_BEAM_B = 1;
    public static final int SLOT_OUTPUT = 2;

    private int progress;
    private int duration;
    private boolean powered;
    private boolean working;

    private final SimpleContainer inventory = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            MicroHadronColliderBlockEntity.this.setChanged();
        }
    };

    public MicroHadronColliderBlockEntity(BlockPos pos, BlockState state) {
        super(BlockRegistry.MICRO_HADRON_COLLIDER_BE.get(), pos, state);
    }

    // ===== 每 tick（仅服务端，见 Block#getTicker） =====

    public static void tick(Level level, BlockPos pos, BlockState state, MicroHadronColliderBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean signal = level.hasNeighborSignal(pos);
        if (signal != be.powered) {
            be.powered = signal;
            be.setChanged();
        }

        // 断电：冻结（保留进度，来电继续）
        if (!signal) {
            if (be.working) {
                be.working = false;
                be.setChanged();
            }
            return;
        }

        Optional<MicroCollisionRecipe> recipe = be.findRecipe(serverLevel);
        if (recipe.isEmpty() || !be.canOutput(recipe.get(), serverLevel)) {
            // 没配方 / 出不去：进度清零重来（材料不消耗）
            if (be.progress > 0 || be.working) {
                be.progress = 0;
                be.working = false;
                be.setChanged();
            }
            return;
        }

        if (!be.working) {
            // 起撞音效
            level.playSound(null, pos, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 0.8F, 1.4F);
            be.working = true;
            be.duration = recipe.get().duration();
            if (be.progress >= be.duration) {
                be.progress = 0;
            }
            be.setChanged();
        }

        be.progress++;
        be.setChanged();

        // 工作中：对撞环上方冒传送门粒子（随机节流，避免每 tick 粒子风暴）
        // 注意 sendParticles 是 ServerLevel 上的方法
        if (level.getRandom().nextInt(4) == 0) {
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D,
                    3, 0.25D, 0.1D, 0.25D, 0.5D);
        }
        if (level.getRandom().nextInt(8) == 0) {
            level.playSound(null, pos, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.3F, 1.8F);
        }

        // 撞完：各扣 1 个，出产物
        if (be.progress >= be.duration) {
            be.finishCollision(serverLevel, pos, recipe.get());
        }
    }

    private Optional<MicroCollisionRecipe> findRecipe(ServerLevel level) {
        ItemStack a = inventory.getItem(SLOT_BEAM_A);
        ItemStack b = inventory.getItem(SLOT_BEAM_B);
        if (a.isEmpty() || b.isEmpty()) {
            return Optional.empty();
        }
        MicroCollisionRecipe.CollisionInput input = new MicroCollisionRecipe.CollisionInput(a, b);
        return level.getRecipeManager().getRecipeFor(RecipeRegistry.MICRO_COLLISION_TYPE.get(), input, level);
    }

    private boolean canOutput(MicroCollisionRecipe recipe, ServerLevel level) {
        ItemStack result = recipe.getResultItem(level.registryAccess());
        ItemStack existing = inventory.getItem(SLOT_OUTPUT);
        if (existing.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameTags(existing, result)
                && existing.getCount() + result.getCount() <= existing.getMaxStackSize();
    }

    private void finishCollision(ServerLevel level, BlockPos pos, MicroCollisionRecipe recipe) {
        ItemStack result = recipe.getResultItem(level.registryAccess());
        ItemStack existing = inventory.getItem(SLOT_OUTPUT);
        if (existing.isEmpty()) {
            inventory.setItem(SLOT_OUTPUT, result);
        } else {
            existing.grow(result.getCount());
        }
        inventory.getItem(SLOT_BEAM_A).shrink(1);
        inventory.getItem(SLOT_BEAM_B).shrink(1);
        progress = 0;
        working = false;
        setChanged();

        // 出产物：铁砧音效 + 粒子内爆
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.6F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D,
                24, 0.3D, 0.25D, 0.3D, 0.08D);
    }

    // ===== 状态查询 =====

    public boolean isPowered() {
        return powered;
    }

    public int getProgress() {
        return progress;
    }

    public int getDuration() {
        return duration > 0 ? duration : 100;
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    // ===== GUI 数据同步（原版 ContainerData 自动发包，参考 AbstractFurnaceMenu） =====

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> getDuration();
                case 2 -> powered ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> duration = value;
                case 2 -> powered = value != 0;
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

    // ===== 存档（1.20.1：CompoundTag） =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Progress", progress);
        tag.putInt("Duration", duration);
        tag.putBoolean("Powered", powered);
        tag.putBoolean("Working", working);
        // 1.20.1 的 SimpleContainer 没有 getItems()，手工铺进 NonNullList
        ContainerHelper.saveAllItems(tag, copyToNonNullList());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("Progress");
        duration = tag.getInt("Duration");
        powered = tag.getBoolean("Powered");
        working = tag.getBoolean("Working");
        NonNullList<ItemStack> list = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, list);
        for (int i = 0; i < list.size() && i < inventory.getContainerSize(); i++) {
            inventory.setItem(i, list.get(i));
        }
    }

    private NonNullList<ItemStack> copyToNonNullList() {
        NonNullList<ItemStack> list = NonNullList.create();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            list.add(inventory.getItem(i));
        }
        return list;
    }

    // ===== MenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.zuoyanmod.micro_hadron_collider");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MicroHadronColliderMenu(containerId, playerInventory, inventory, data);
    }
}
