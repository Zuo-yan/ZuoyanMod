package org.gwfx.zuoyanmod.item;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraftforge.common.ForgeHooks;

/**
 * 终端右栏下半那台**随身熔炉**：输入 / 燃料 / 产物三格，行为和原版熔炉一致。
 *
 * <h2>为什么它是"真熔炉"而不是"一键烧"</h2>
 * 上一版做过"放进去就自动烧"的开关，那种做法没有燃料、没有进度、也没有产物格，
 * 玩家既看不到在烧什么，也没法只烧一部分。现在按原版熔炉的节奏来：
 * 要放燃料 → 火会烧完 → 输入一格一格地变成产物 → 产物满了就停。
 *
 * <h2>为什么状态挂在玩家能力上（而不是菜单里）</h2>
 * 菜单关掉就消失的东西不能叫熔炉。这份状态跟着 {@code FourDimensionalSpace} 一起存档，
 * 由 {@code KleinBottleItem#inventoryTick} 每 tick 推进——所以<b>关着界面也在烧</b>，
 * 跟把熔炉带在身上是一回事。
 *
 * <h2>1.20.1 的燃料 API（和 26.x 不一样）</h2>
 * 1.20.1 的燃料时长走 {@code ForgeHooks.getBurnTime(stack, RecipeType.SMELTING)}
 * （26.x 换成了 {@code DataComponents.COOKING_FUEL} 组件）；
 * 容器残留走 {@code ItemStack#hasCraftingRemainingItem / getCraftingRemainingItem}
 * （26.x 换成了 {@code DataComponents.USE_REMAINDER}）。
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

    /** 原版 {@code AbstractFurnaceMenu#isFuel} 的等价物：ForgeHooks 里有没有燃烧时间 */
    public static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && burnDurationOf(stack) > 0;
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
        AbstractCookingRecipe recipe = recipeFor(level, input);
        boolean canSmelt = recipe != null
                && outputAccepts(getItem(SLOT_RESULT), recipe.assemble(new SimpleContainer(input), level.registryAccess()));

        if (burnTime > 0) {
            burnTime--;
        }

        if (canSmelt) {
            cookDuration = recipe.getCookingTime();
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
                    smelt(recipe, level.registryAccess());
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

    private void smelt(AbstractCookingRecipe recipe, net.minecraft.core.RegistryAccess registryAccess) {
        ItemStack input = getItem(SLOT_INPUT).copy();
        ItemStack produced = recipe.assemble(new SimpleContainer(input), registryAccess);
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
        pendingExperience += recipe.getExperience();
    }

    private void consumeFuel() {
        ItemStack fuel = getItem(SLOT_FUEL);
        // 1.20.1 有 hasCraftingRemainingItem（26.3 已移除，换成 USE_REMAINDER 组件）。
        // 熔岩桶烧完要退回一个空桶，靠的就是它。
        if (fuel.hasCraftingRemainingItem()) {
            ItemStack converted = fuel.getCraftingRemainingItem();
            if (!converted.isEmpty()) {
                setItem(SLOT_FUEL, converted);
                return;
            }
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

    private static AbstractCookingRecipe recipeFor(ServerLevel level, ItemStack input) {
        if (input.isEmpty()) {
            return null;
        }
        // 1.20.1 的配方输入是 Container（26.x 是 SingleRecipeInput），直接包一个单格容器
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SimpleContainer(input), level)
                .orElse(null);
    }

    private static boolean outputAccepts(ItemStack result, ItemStack produced) {
        if (produced.isEmpty()) {
            return false;
        }
        if (result.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameTags(result, produced)
                && result.getCount() + produced.getCount() <= result.getMaxStackSize();
    }

    /** 燃料能烧多久（1.20.1：ForgeHooks 的燃烧时间表） */
    private static int burnDurationOf(ItemStack fuel) {
        return Math.max(0, ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING));
    }

    // ===== 存档（1.20.1：原生 CompoundTag NBT） =====

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        NonNullList<ItemStack> items = NonNullList.create();
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.add(getItem(i));
        }
        net.minecraft.world.ContainerHelper.saveAllItems(tag, items);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
        tag.putInt("CookTime", cookTime);
        tag.putInt("CookDuration", cookDuration);
        tag.putFloat("PendingXp", pendingExperience);
        return tag;
    }

    public void deserialize(CompoundTag tag) {
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        net.minecraft.world.ContainerHelper.loadAllItems(tag, items);
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
