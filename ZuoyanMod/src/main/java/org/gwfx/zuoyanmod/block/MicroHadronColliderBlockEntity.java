package org.gwfx.zuoyanmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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

    public static void tick(ServerLevel level, BlockPos pos, BlockState state, MicroHadronColliderBlockEntity be) {
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

        Optional<MicroCollisionRecipe> recipe = be.findRecipe(level);
        if (recipe.isEmpty() || !be.canOutput(recipe.get())) {
            // 没配方 / 出不去：进度清零重来（材料不消耗）
            if (be.progress > 0 || be.working) {
                be.progress = 0;
                be.working = false;
                be.setChanged();
            }
            return;
        }

        if (!be.working) {
            // 起撞音效（PORTAL_TRIGGER 是裸 SoundEvent）
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
        if (level.getRandom().nextInt(4) == 0) {
            level.sendParticles(ParticleTypes.PORTAL,
                    pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D,
                    3, 0.25D, 0.1D, 0.25D, 0.5D);
        }
        if (level.getRandom().nextInt(8) == 0) {
            level.playSound(null, pos, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.3F, 1.8F);
        }

        // 撞完：各扣 1 个，出产物
        if (be.progress >= be.duration) {
            be.finishCollision(level, pos, recipe.get());
        }
    }

    private Optional<MicroCollisionRecipe> findRecipe(ServerLevel level) {
        ItemStack a = inventory.getItem(SLOT_BEAM_A);
        ItemStack b = inventory.getItem(SLOT_BEAM_B);
        if (a.isEmpty() || b.isEmpty()) {
            return Optional.empty();
        }
        MicroCollisionRecipe.CollisionInput input = new MicroCollisionRecipe.CollisionInput(a, b);
        return level.recipeAccess()
                .getRecipeFor(RecipeRegistry.MICRO_COLLISION_TYPE.get(), input, level)
                .map(holder -> holder.value());
    }

    private boolean canOutput(MicroCollisionRecipe recipe) {
        ItemStack result = recipe.assemble(null);
        ItemStack existing = inventory.getItem(SLOT_OUTPUT);
        if (existing.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(existing, result)
                && existing.getCount() + result.getCount() <= existing.getMaxStackSize();
    }

    private void finishCollision(ServerLevel level, BlockPos pos, MicroCollisionRecipe recipe) {
        ItemStack result = recipe.assemble(null);
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

        // 出产物：铁砧音效（裸 SoundEvent）+ 粒子内爆
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

    // ===== 存档 =====

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Progress", progress);
        output.putInt("Duration", duration);
        output.putBoolean("Powered", powered);
        output.putBoolean("Working", working);
        ContainerHelper.saveAllItems(output, inventory.getItems());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getInt("Progress").orElse(0);
        duration = input.getInt("Duration").orElse(0);
        powered = input.getBooleanOr("Powered", false);
        working = input.getBooleanOr("Working", false);
        inventory.clearContent();
        ContainerHelper.loadAllItems(input, inventory.getItems());
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
