package org.gwfx.zuoyanmod.menu;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.gwfx.zuoyanmod.item.FourDimensionalSpace;
import org.gwfx.zuoyanmod.network.PacketHandler;

import java.util.List;
import java.util.Optional;

/**
 * 「四维空间 · 工作台」：3×3 合成网格 + 产物槽 + 一条 27 格的存储快取条。
 *
 * <p>和普通工作台的区别是**配料来源**：JEI 的 + 按钮触发
 * {@link PacketHandler#sendCraftingTransfer(Identifier, boolean)}，服务端在
 * {@link #transfer} 里直接从玩家的 {@link FourDimensionalSpace} 全量库存里抓材料填网格——
 * 不用先手动把东西从终端搬进背包，也不用管这些东西在第几页。
 *
 * <p>合成产物本身复用原版 {@link ResultSlot}：它会正确处理"取出时消耗网格"、
 * "桶/瓶子之类容器的残留物返还"和统计/成就，这里一行都不用重写。
 */
public class KleinBottleCraftingMenu extends AbstractContainerMenu {

    public static final int BUTTON_PAGE_PREV = 0;
    public static final int BUTTON_PAGE_NEXT = 1;

    public static final int GRID_START = 0;
    public static final int GRID_COUNT = 9;
    public static final int RESULT_SLOT = 9;
    public static final int SPACE_START = 10;
    public static final int SPACE_COUNT = 27;
    public static final int INV_START = SPACE_START + SPACE_COUNT;
    public static final int INV_COUNT = 36;

    // 布局常量，必须与 client/KleinBottleCraftingScreen.java 一致
    public static final int GRID_X = 30;
    public static final int GRID_Y = 22;
    public static final int RESULT_X = 124;
    public static final int RESULT_Y = 44;
    public static final int SPACE_Y = 84;
    public static final int PLAYER_Y = 150;

    private final TransientCraftingContainer craftSlots;
    private final ResultContainer resultSlots;
    private final SimpleContainer window;
    private final ContainerData data;
    private final FourDimensionalSpace space;
    private final Player player;
    private int page;
    private int lastSize = -1;
    private int lastPage = -1;

    public KleinBottleCraftingMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, playerInventory.player);
    }

    public KleinBottleCraftingMenu(int containerId, Inventory playerInventory, Player player) {
        super(MenuRegistry.KLEIN_BOTTLE_CRAFTING_MENU.get(), containerId);
        this.player = player;
        boolean client = player.level().isClientSide();
        this.space = client ? null : FourDimensionalSpace.of(player);
        this.craftSlots = new TransientCraftingContainer(this, 3, 3);
        this.resultSlots = new ResultContainer();
        this.window = new SimpleContainer(SPACE_COUNT);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                if (space == null) {
                    return 0;
                }
                return switch (index) {
                    case 0 -> page;
                    case 1 -> pageCount();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 2;
            }
        };

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(craftSlots, col + row * 3, GRID_X + col * 18, GRID_Y + row * 18));
            }
        }
        addSlot(new ResultSlot(player, craftSlots, resultSlots, 0, RESULT_X, RESULT_Y));

        for (int row = 0; row < SPACE_COUNT / 9; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new SpaceSlot(col + row * 9, 8 + col * 18, SPACE_Y + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, PLAYER_Y + 58));
        }
        addDataSlots(data);
        refreshWindow();
    }

    public int getPage() {
        return data.get(0);
    }

    public int getPageCount() {
        return Math.max(1, data.get(1));
    }

    /** 快取条按"视图"分页：过滤之后的条目按每页 {@link #SPACE_COUNT} 条切 */
    private int pageCount() {
        if (space == null) {
            return 1;
        }
        return Math.max(1, (space.viewSize() + SPACE_COUNT - 1) / SPACE_COUNT);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (space == null) {
            return false;
        }
        switch (id) {
            case BUTTON_PAGE_PREV -> page = Math.max(0, page - 1);
            case BUTTON_PAGE_NEXT -> page = Math.min(pageCount() - 1, page + 1);
            default -> {
                return false;
            }
        }
        refreshWindow();
        return true;
    }

    /** 装填只能走 getItems().set(...)：走 setItem() 会回调写回钩子，形成装填↔写回的递归 */
    private void refreshWindow() {
        if (space == null) {
            return; // 客户端：窗口内容由原版槽位同步负责
        }
        int base = page * SPACE_COUNT;
        for (int i = 0; i < SPACE_COUNT; i++) {
            ItemStack stack = space.viewItem(base + i);
            window.getItems().set(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container != craftSlots || space == null || player.level().isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        CraftingInput input = craftSlots.asCraftInput();
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> recipe =
                level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
        if (recipe.isPresent()) {
            resultSlots.setRecipeUsed(recipe.get());
            result = recipe.get().value().assemble(input);
        }
        resultSlots.setItem(0, result);
    }

    @Override
    public void broadcastChanges() {
        if (space != null) {
            boolean changed = space.settle();
            if (changed || lastPage != page || lastSize != space.viewSize()) {
                page = Math.min(page, Math.max(0, pageCount() - 1));
                refreshWindow();
                lastPage = page;
                lastSize = space.viewSize();
            }
        }
        super.broadcastChanges();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (space == null) {
            return;
        }
        // 关界面时把网格里没用完的东西送回四维空间，而不是丢在地上
        for (int i = 0; i < GRID_COUNT; i++) {
            ItemStack left = craftSlots.getItem(i);
            if (!left.isEmpty()) {
                space.insert(left);
            }
        }
        craftSlots.clearContent();
    }

    /**
     * 结构照抄原版 {@code CraftingMenu.quickMoveStack}——尤其是最后那句
     * {@code slot.onTake(player, stack)}：<b>产物槽靠它才会消耗合成网格</b>
     * （{@code ResultSlot.onTake} 里做扣网格 + 返还容器残留物）。漏掉这句就会
     * "产物拿走了、网格一个都没少"，等于无限复制。
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        clicked = stack.copy();

        if (index == RESULT_SLOT) {
            // 产物优先回四维空间（那才是仓库），塞不下再回落背包
            if (!moveItemStackTo(stack, SPACE_START, SPACE_START + SPACE_COUNT, false)
                    && !moveItemStackTo(stack, INV_START, INV_START + INV_COUNT, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, clicked);
        } else if (index < GRID_COUNT) {
            // 合成网格 → 四维空间
            if (!moveItemStackTo(stack, SPACE_START, SPACE_START + SPACE_COUNT, false)
                    && !moveItemStackTo(stack, INV_START, INV_START + INV_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index < INV_START) {
            // 四维空间 → 背包（要手动摆配方就用鼠标拖，别让 shift-click 去抢网格）
            if (!moveItemStackTo(stack, INV_START, INV_START + INV_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, SPACE_START, SPACE_START + SPACE_COUNT, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == clicked.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return clicked;
    }

    /** 数据挂在玩家身上、不依附方块，所以永远有效 */
    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** 双击拾取时不要把产物槽算进去（照抄原版 CraftingMenu） */
    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != this.resultSlots && super.canTakeItemForPickAll(carried, target);
    }

    // ===== JEI 配料传输（服务端） =====

    /**
     * 按配方 id 从四维空间里抓材料填 3×3 网格。
     *
     * <p>配料清单与"哪个格子放第几项"来自 26.3 的 {@link PlacementInfo}。⚠️ 但它的
     * {@code slotsToIngredientIndex()} 用的是<b>配方自身网格</b>的坐标——2×2 配方长度是 4、
     * 1×3 配方长度是 3，都不是 9。所以有形配方必须再拿 {@code ShapedRecipe#getWidth()}
     * 换算一次 {@code cell = (s / w) * 3 + (s % w)} 才能落到 3×3 网格上。
     * 无形配方没有宽度，直接按顺序占 0..n-1（本来就不关心位置）。
     * 填完再用 {@code recipe.matches(...)} 自检，不匹配就整单撤销、原样退回四维空间。
     */
    public static void transfer(ServerPlayer player, Identifier recipeId, boolean maxTransfer) {
        if (!(player.containerMenu instanceof KleinBottleCraftingMenu menu) || menu.space == null) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        Optional<RecipeHolder<?>> found =
                level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, recipeId));
        if (found.isEmpty() || !(found.get().value() instanceof CraftingRecipe recipe)) {
            return;
        }

        PlacementInfo placement = recipe.placementInfo();
        if (placement.isImpossibleToPlace()) {
            return;
        }
        List<Ingredient> ingredients = placement.ingredients();
        IntList slotsToIngredient = placement.slotsToIngredientIndex();

        FourDimensionalSpace space = menu.space;

        // 先探一遍够不够，顺便算出"最多能同时做几份"
        int sets = maxTransfer ? Integer.MAX_VALUE : 1;
        for (Ingredient ing : ingredients) {
            if (ing.isEmpty()) {
                continue;
            }
            int available = space.countMatching(ing::test);
            if (available == 0) {
                return; // 一样都没有，静默失败，别把网格搞乱
            }
            if (maxTransfer) {
                sets = Math.min(sets, Math.min(available, 64));
            }
        }
        if (sets == Integer.MAX_VALUE) {
            sets = 1;
        }

        // 网格里原有的东西先还回空间
        for (int i = 0; i < GRID_COUNT; i++) {
            ItemStack old = menu.craftSlots.getItem(i);
            if (!old.isEmpty()) {
                space.insert(old);
            }
        }

        int recipeWidth = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 0;

        NonNullList<ItemStack> placed = NonNullList.withSize(GRID_COUNT, ItemStack.EMPTY);
        boolean ok = true;
        for (int s = 0; s < slotsToIngredient.size() && ok; s++) {
            int ingIndex = slotsToIngredient.getInt(s);
            if (ingIndex == PlacementInfo.EMPTY_SLOT || ingIndex < 0 || ingIndex >= ingredients.size()) {
                continue;
            }
            Ingredient ing = ingredients.get(ingIndex);
            // 配方自身网格 → 3×3 网格
            int cell = recipeWidth > 0 ? (s / recipeWidth) * 3 + (s % recipeWidth) : s;
            if (ing.isEmpty() || cell >= GRID_COUNT) {
                continue;
            }
            ItemStack got = space.take(ing::test, sets);
            if (got.getCount() < sets) {
                if (!got.isEmpty()) {
                    space.insert(got);
                }
                ok = false;
                break;
            }
            placed.set(cell, got);
        }

        if (ok) {
            for (int i = 0; i < GRID_COUNT; i++) {
                menu.craftSlots.setItem(i, placed.get(i));
            }
            if (!recipe.matches(menu.craftSlots.asCraftInput(), level)) {
                ok = false; // 落位不对，整单撤销
            }
        }

        if (!ok) {
            for (ItemStack s : placed) {
                if (!s.isEmpty()) {
                    space.insert(s);
                }
            }
            for (int i = 0; i < GRID_COUNT; i++) {
                menu.craftSlots.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /**
     * 快取条槽位。写回挂在 {@code setChanged()} 上而不是容器的 {@code setItem()} 上——
     * 原版 {@code moveItemStackTo} 合并同类物品时是就地改 {@code slot.getItem()} 的 count、
     * 然后只调 {@code setChanged()}，容器的 setItem 根本不会被回调。
     */
    private final class SpaceSlot extends Slot {

        SpaceSlot(int index, int x, int y) {
            super(window, index, x, y);
        }

        @Override
        public void setChanged() {
            writeBack(getContainerSlot());
        }

        @Override
        public ItemStack remove(int amount) {
            ItemStack out = super.remove(amount);
            writeBack(getContainerSlot());
            return out;
        }

        /** 视图下标查不到对应条目时（往空位里丢东西）返回 -1，setStable 会走 insert 收下 */
        private void writeBack(int viewSlot) {
            if (space == null) {
                return;
            }
            int viewIndex = page * SPACE_COUNT + viewSlot;
            space.setStable(space.viewStorageIndex(viewIndex), window.getItem(viewSlot));
        }
    }
}
