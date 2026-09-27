package org.gwfx.zuoyanmod.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;

import java.util.Optional;

/**
 * 终端右栏下半那台**随身熔炉**：输入 / 燃料 / 产物三格，行为和原版熔炉一致。
 *
 * <h2>为什么它是"真熔炉"而不是"一键烧"</h2>
 * 上一版做过"放进去就自动烧"的开关，那种做法没有燃料、没有进度、也没有产物格，
 * 玩家既看不到在烧什么，也没法只烧一部分。现在按原版熔炉的节奏来：
 * 要放燃料 → 火会烧完 → 输入一格一格地变成产物 → 产物满了就停。
 *
 * <h2>为什么状态挂在玩家附件上（而不是菜单里）</h2>
 * 菜单关掉就消失的东西不能叫熔炉。这份状态跟着 {@code FourDimensionalSpace} 一起存档，
 * 由 {@code KleinBottleItem#inventoryTick} 每 tick 推进——所以<b>关着界面也在烧</b>，
 * 跟把熔炉带在身上是一回事。
 *
 * <h2>1.21.1 的燃料 API</h2>
 * 燃料判定与时长都走 NeoForge 的 {@code ItemStack#getBurnTime(RecipeType)}：
 * 它汇总原版燃料表（{@code AbstractFurnaceBlockEntity} 的缓存 map）与 NeoForge 的
 * {@code FurnaceFuel} 数据映射，一个调用拿全。返回 &gt; 0 就是燃料，
 * 具体数值就是燃烧秒数 ×20。
 */
public class KleinFurnace extends SimpleContainer {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_RESULT = 2;
    public static final int SLOT_COUNT = 3;

    /** 与 {@code AbstractFurnaceMenu} 的 ContainerData 约定一致，界面照原版的方式读进度 */
    public static final int DATA_BURN_TIME = 0;
    public static final int DATA_BURN_DURATION = 1;
    public static final int DATA_COOK_TIME = 2;
    public static final int DATA_COOK_DURATION = 3;
    public static final int DATA_COUNT = 4;

    /** 没火时进度的回落速度（照抄原版：不再烧就往回退） */
    private static final int COOL_DOWN = 2;

    private int burnTime;
    private int burnDuration;
    private int cookTime;
    private int cookDuration;
    /** 攒着的经验：原版熔炉把经验存在方块里，玩家取出产物时才结算 */
    private float pendingExperience;

    public KleinFurnace() {
        super(SLOT_COUNT);
    }

    public int burnTime() {
        return burnTime;
    }

    public int burnDuration() {
        return burnDuration;
    }

    public int cookTime() {
        return cookTime;
    }

    public int cookDuration() {
        return cookDuration;
    }

    public boolean isLit() {
        return burnTime > 0;
    }

    /** 原版 {@code AbstractFurnaceBlockEntity#isFuel}：烧时 &gt; 0 就是燃料 */
    public static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && stack.getBurnTime(RecipeType.SMELTING) > 0;
    }

    // ===== 每 tick =====

    /**
     * 推进一个 tick。只能服务端调（{@code KleinBottleItem#inventoryTick}）。
     *
     * <p>完全没有东西在忙就直接返回——大部分玩家大部分时间根本不带熔炉工作，
     * 这条早退能把"每 tick 都查一次配方"的开销压到几乎为零。
     */
    public void tick(ServerLevel level) {
        boolean idle = burnTime <= 0
                && cookTime <= 0
                && getItem(SLOT_INPUT).isEmpty()
                && getItem(SLOT_FUEL).isEmpty()
                && getItem(SLOT_RESULT).isEmpty();
        if (idle) {
            burnDuration = 0;
            cookDuration = 0;
            return;
        }

        ItemStack input = getItem(SLOT_INPUT);
        RecipeHolder<? extends AbstractCookingRecipe> recipe = recipeFor(level, input);
        boolean canSmelt = recipe != null
                && outputAccepts(getItem(SLOT_RESULT),
                        recipe.value().assemble(new SingleRecipeInput(input), level.registryAccess()));

        if (burnTime > 0) {
            burnTime--;
        }

        if (canSmelt) {
            cookDuration = recipe.value().getCookingTime();
            if (burnTime <= 0) {
                // 火灭了：看燃料格能不能点着一根新的
                int duration = burnDurationOf(getItem(SLOT_FUEL));
                if (duration > 0) {
                    burnDuration = duration;
                    burnTime = duration;
                    consumeFuel();
                }
            }
            if (burnTime > 0) {
                cookTime++;
                if (cookTime >= cookDuration) {
                    cookTime = 0;
                    smelt(recipe, level);
                }
            } else {
                cookTime = Math.max(0, cookTime - COOL_DOWN);
            }
        } else {
            cookTime = Math.max(0, cookTime - COOL_DOWN);
            cookDuration = 0;
        }

        if (burnTime <= 0) {
            burnDuration = 0;
        }
    }

    private void smelt(RecipeHolder<? extends AbstractCookingRecipe> recipe, ServerLevel level) {
        ItemStack input = getItem(SLOT_INPUT).copy();
        ItemStack produced = recipe.value().assemble(new SingleRecipeInput(input), level.registryAccess());
        if (produced.isEmpty()) {
            return;
        }
        ItemStack result = getItem(SLOT_RESULT);
        if (result.isEmpty()) {
            setItem(SLOT_RESULT, produced.copy());
        } else {
            ItemStack merged = result.copy();
            merged.grow(produced.getCount());
            setItem(SLOT_RESULT, merged);
        }
        input.shrink(1);
        setItem(SLOT_INPUT, input.isEmpty() ? ItemStack.EMPTY : input);
        pendingExperience += recipe.value().getExperience();
    }

    private void consumeFuel() {
        ItemStack fuel = getItem(SLOT_FUEL);
        // 容器残留走物品的 craftingRemainingItem（熔岩桶烧完退回空桶，同原版熔炉）
        if (fuel.hasCraftingRemainingItem()) {
            setItem(SLOT_FUEL, fuel.getCraftingRemainingItem());
            return;
        }
        ItemStack rest = fuel.copy();
        rest.shrink(1);
        setItem(SLOT_FUEL, rest.isEmpty() ? ItemStack.EMPTY : rest);
    }

    /** 取出产物时结算经验（原版熔炉也是这个时机） */
    public void grantExperience(Player player) {
        if (pendingExperience < 1.0F) {
            return;
        }
        int points = (int) pendingExperience;
        pendingExperience -= points;
        player.giveExperiencePoints(points);
    }

    private static RecipeHolder<? extends AbstractCookingRecipe> recipeFor(ServerLevel level, ItemStack input) {
        if (input.isEmpty()) {
            return null;
        }
        Optional<RecipeHolder<SmeltingRecipe>> found =
                level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level);
        return found.orElse(null);
    }

    private static boolean outputAccepts(ItemStack result, ItemStack produced) {
        if (produced.isEmpty()) {
            return false;
        }
        if (result.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(result, produced)
                && result.getCount() + produced.getCount() <= result.getMaxStackSize();
    }

    /**
     * 燃料能烧多久。
     *
     * <p>NeoForge 的 {@code ItemStack#getBurnTime(RecipeType)} 一个调用拿全：
     * 原版燃料表与数据映射（{@code FurnaceFuel}）都汇总在里面，返回值就是燃烧的 tick 数。
     */
    private static int burnDurationOf(ItemStack fuel) {
        if (!isFuel(fuel)) {
            return 0;
        }
        return Math.max(0, fuel.getBurnTime(RecipeType.SMELTING));
    }

    // ===== 存档 =====

    /** 布局与物品同原版 {@code ContainerHelper#saveAllItems}，外层字段由 {@code FourDimensionalSpace} 落盘 */
    public void serialize(CompoundTag tag, HolderLookup.Provider registries) {
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items, true, registries);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
        tag.putInt("CookTime", cookTime);
        tag.putInt("CookDuration", cookDuration);
        tag.putFloat("PendingXp", pendingExperience);
    }

    public void deserialize(CompoundTag tag, HolderLookup.Provider registries) {
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        for (int i = 0; i < SLOT_COUNT; i++) {
            setItem(i, items.get(i));
        }
        burnTime = tag.getInt("BurnTime");
        burnDuration = tag.getInt("BurnDuration");
        cookTime = tag.getInt("CookTime");
        cookDuration = tag.getInt("CookDuration");
        pendingExperience = tag.getFloat("PendingXp");
    }
}
