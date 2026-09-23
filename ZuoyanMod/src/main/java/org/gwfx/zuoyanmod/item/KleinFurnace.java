package org.gwfx.zuoyanmod.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

import java.util.List;
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
 * <h2>26.3 的燃料 API（和老版本不一样）</h2>
 * 燃料时长不再是 {@code FurnaceFuel#getBurnTime} 那套黑魔法，而是物品上的
 * {@code DataComponents.COOKING_FUEL}，里面的 {@code burnTime} 是一个
 * {@link ResolvableInt}：常量直接读值，注册表引用则要用 {@link LootContext} 解析。
 * 判定"是不是燃料"就是 {@code stack.has(DataComponents.COOKING_FUEL)}。
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

    /** 原版 {@code AbstractFurnaceMenu#isFuel}：有没有 COOKING_FUEL 组件 */
    public static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.COOKING_FUEL);
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
                && outputAccepts(getItem(SLOT_RESULT), recipe.value().assemble(new SingleRecipeInput(input)));

        if (burnTime > 0) {
            burnTime--;
        }

        if (canSmelt) {
            cookDuration = recipe.value().cookingTime();
            if (burnTime <= 0) {
                // 火灭了：看燃料格能不能点着一根新的
                int duration = burnDurationOf(level, getItem(SLOT_FUEL));
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
                    smelt(recipe);
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

    private void smelt(RecipeHolder<? extends AbstractCookingRecipe> recipe) {
        ItemStack input = getItem(SLOT_INPUT).copy();
        ItemStack produced = recipe.value().assemble(new SingleRecipeInput(input));
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
        pendingExperience += recipe.value().experience();
    }

    private void consumeFuel() {
        ItemStack fuel = getItem(SLOT_FUEL);
        // 26.3 没有 hasCraftingRemainingItem 了：容器残留物统一走 DataComponents.USE_REMAINDER。
        // 熔岩桶烧完要退回一个空桶，靠的就是它。
        UseRemainder remainder = fuel.get(DataComponents.USE_REMAINDER);
        if (remainder != null) {
            ItemStack converted = remainder.convertInto().create();
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

    private static RecipeHolder<? extends AbstractCookingRecipe> recipeFor(ServerLevel level, ItemStack input) {
        if (input.isEmpty()) {
            return null;
        }
        Optional<RecipeHolder<SmeltingRecipe>> found =
                level.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level);
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
     * <p>常量（绝大多数原版燃料）直接读值；注册表引用要走 {@link LootContext} 解析，
     * 那需要一个带 registry lookup 的上下文——用空的 {@link ContextKeySet} 建一个就够。
     */
    private static int burnDurationOf(ServerLevel level, ItemStack fuel) {
        if (!isFuel(fuel)) {
            return 0;
        }
        CookingFuel component = fuel.get(DataComponents.COOKING_FUEL);
        if (component == null) {
            return 0;
        }
        if (component.burnTime() instanceof ResolvableInt.Constant constant) {
            return Math.max(0, constant.value());
        }
        LootParams params = new LootParams.Builder(level).create(ContextKeySet.EMPTY);
        LootContext context = new LootContext.Builder(params).create(Optional.empty());
        return Math.max(0, ResolvableInt.getFromItem(
                fuel, DataComponents.COOKING_FUEL, CookingFuel::burnTime, context, 0));
    }

    // ===== 存档 =====

    public void serialize(ValueOutput output) {
        output.store("Items", ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(getItems()));
        output.putInt("BurnTime", burnTime);
        output.putInt("BurnDuration", burnDuration);
        output.putInt("CookTime", cookTime);
        output.putInt("CookDuration", cookDuration);
        output.putFloat("PendingXp", pendingExperience);
    }

    public void deserialize(ValueInput input) {
        List<ItemStack> items = input.read("Items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < SLOT_COUNT; i++) {
            setItem(i, i < items.size() ? items.get(i) : ItemStack.EMPTY);
        }
        burnTime = input.getIntOr("BurnTime", 0);
        burnDuration = input.getIntOr("BurnDuration", 0);
        cookTime = input.getIntOr("CookTime", 0);
        cookDuration = input.getIntOr("CookDuration", 0);
        pendingExperience = input.getFloatOr("PendingXp", 0.0F);
    }
}
