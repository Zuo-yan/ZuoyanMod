package org.gwfx.zuoyanmod.menu;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.SimpleContainerData;
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
import org.gwfx.zuoyanmod.item.KleinFurnace;
import org.gwfx.zuoyanmod.network.KleinBottleSyncPacket;
import org.gwfx.zuoyanmod.util.KleinTerminalLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 「四维空间」终端：无限存储 + **内嵌工作台**的一体化终端。
 *
 * <h2>版面与区间</h2>
 * <pre>
 * 槽位序号   区间        作用
 *   0..53    存储窗口     9×6 的视图窗口，一格 = 一种物品（总量存在 space 里）
 *  54..62    合成网格     3×3，内嵌在右栏，**不需要点按钮切换界面**
 *    63      合成产物     原版 ResultSlot
 *    64      熔炉输入
 *    65      熔炉燃料
 *    66      熔炉产物
 *  67..69    铁砧         左 = 目标 / 右 = 材料 / 产物，外加一个重命名输入框
 *  70..105   玩家背包     27 格 + 9 格快捷栏
 * </pre>
 *
 * <h2>为什么取放要自己接管（{@link #clicked}）</h2>
 * 存储的一格背后是 {@code (template, long total)}，而原版的 {@code Slot} 只认那个
 * 夹到堆叠上限的展示栈。玩家从"有 3300 个"的格子里拿走一组，原版会认为
 * "这一格被掏空了"——3300 个剩下的全没了。所以存储槽位
 * （{@link StorageSlot}）声明 {@code mayPlace/mayPickup = false}，
 * 所有取放都在 {@link #clicked} 里按"从总量里扣/往总量里加"实现：
 * <ul>
 *   <li>左键 → 取一组（该物品的堆叠上限）；</li>
 *   <li>右键 → <b>再拿一个</b>（可以连着点，光标一路涨到满一组；满了才改成"放一个回去"）；</li>
 *   <li>光标拿着别的东西右键 → 先整堆收进空间，再从这一格拿一个出来（＝交换）；</li>
 *   <li>光标拿着同类东西左键 → 直接并进这一格（不限量）；</li>
 *   <li>Shift + 左键 → 反复搬整组进背包，直到背包塞不下或这一格取空；</li>
 *   <li>Shift + 右键 → 从光标放回一个（"连着拿"的镜像，不然多拿的还只能整堆放回）；</li>
 *   <li>数字键 → 快捷栏那格与这一格互换（快捷栏里的先收进空间）；</li>
 *   <li>Q / Ctrl+Q → 丢一个 / 丢一组。</li>
 * </ul>
 *
 * <p>右键那条是刻意偏离原版的：原版右键是"取一半"，而这里一格可能有几千个，
 * 取一半没有意义；更难受的是原版"右键放回去"——拿了 1 个再右键就还回去了，
 * 永远攒不起来。所以改成"只要光标还没满，右键就继续拿"，和从箱子里一件一件
 * 往手上摞的感觉一致。
 *
 * <h2>JEI</h2>
 * JEI 的 + 按钮走 {@link #transfer}：从四维空间全量库存里抓材料填进内嵌的 3×3，
 * 不用先把东西从终端搬进背包，也不用管在第几行。这和 RS2 的合成网格是同一套关系。
 */
public class KleinBottleMenu extends AbstractContainerMenu {

    // ===== 按钮（走原版的 clickMenuButton 通道；搜索与滚动走独立包）=====
    /** 与 {@code KleinTerminalLayout.BUTTON_*} 一一对应 */
    public static final int BUTTON_HOME = 0;
    public static final int BUTTON_SORT_MODE = 1;
    public static final int BUTTON_SORT_DIR = 2;
    public static final int BUTTON_SCROLL_UP_PAGE = 3;
    public static final int BUTTON_SCROLL_DOWN_PAGE = 4;
    public static final int BUTTON_SCROLL_HOME = 5;
    public static final int BUTTON_SCROLL_END = 6;

    public static final int COLUMNS = FourDimensionalSpace.COLUMNS;
    public static final int VISIBLE_ROWS = FourDimensionalSpace.DEFAULT_ROWS;
    public static final int STORAGE_COUNT = COLUMNS * VISIBLE_ROWS;
    public static final int CRAFT_START = STORAGE_COUNT;
    public static final int CRAFT_COUNT = 9;
    public static final int RESULT_SLOT = CRAFT_START + CRAFT_COUNT;
    /** 熔炉三格。客户端也各建一个空容器——内容由原版槽位同步填过来 */
    public static final int FURNACE_INPUT = RESULT_SLOT + 1;
    public static final int FURNACE_FUEL = FURNACE_INPUT + 1;
    public static final int FURNACE_RESULT = FURNACE_FUEL + 1;
    public static final int ANVIL_BASE = FURNACE_RESULT + 1;
    public static final int ANVIL_MATERIAL = ANVIL_BASE + 1;
    public static final int ANVIL_RESULT = ANVIL_MATERIAL + 1;
    public static final int INV_START = ANVIL_RESULT + 1;
    public static final int INV_SLOTS = 36;
    public static final int INV_END = INV_START + INV_SLOTS;

    private final SimpleContainer window;
    private final TransientCraftingContainer craftSlots;
    private final ResultContainer resultSlots;
    private final Player player;
    /** 仅服务端非空：客户端没有存储实体 */
    private final FourDimensionalSpace space;
    /** 熔炉容器：服务端就是玩家附件上那一份，客户端只是个空壳（靠槽位同步填） */
    private final KleinFurnace furnace;
    /** 燃料 / 进度走原版的 ContainerData 通道，自动同步给客户端 */
    private final SimpleContainerData furnaceData = new SimpleContainerData(KleinFurnace.DATA_COUNT);
    /**
     * 铁砧的两个输入格（真菜单槽位，客户端由原版槽位同步填）。
     *
     * <p>⚠️ 这里必须覆写 {@code setChanged()} 去调 {@link #slotsChanged}——
     * 普通 {@link SimpleContainer} 的 {@code setChanged()} **不会**通知菜单
     * （原版只有 {@code TransientCraftingContainer}、{@code ItemCombinerMenu} 内部那个
     * 容器会）。v7 就是因为漏了这一步，铁砧的结果永远算不出来，卡片看起来"不能用"。
     */
    private final SimpleContainer anvilInput = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            KleinBottleMenu.this.slotsChanged(this);
        }
    };
    private final ResultContainer anvilResult = new ResultContainer();
    /**
     * 服务端的"隐形铁砧"：借一个**不发给客户端**的原版 {@link AnvilMenu} 来算结果。
     * 修理、合并、附魔书、重命名、经验代价、代价上限——全和真铁砧一致，
     * 因为那套几百行的算术就是它自己算的。客户端侧 {@code anvil == null}，
     * 只看到三个同步过来的槽位和代价数值。
     *
     * <p>构造时用 {@code ContainerLevelAccess.NULL}（{@code AnvilMenu(int, Inventory)} 就是
     * 这么造的），于是 {@code onTake} 里"消耗铁砧耐久"那段是空操作——我们没有方块可掉耐久。
     */
    private final GhostAnvil anvil;
    /** 铁砧的经验代价，走 ContainerData 同步给客户端画「需要 X 级」 */
    private final SimpleContainerData anvilData = new SimpleContainerData(1);
    private boolean anvilUpdating;

    private int scrollRow;
    private boolean windowDirty = true;
    private boolean syncPending = true;

    public KleinBottleMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, playerInventory.player);
    }

    public KleinBottleMenu(int containerId, Inventory playerInventory, Player player) {
        super(MenuRegistry.KLEIN_BOTTLE_MENU.get(), containerId);
        this.player = player;
        this.space = player.level().isClientSide() ? null : FourDimensionalSpace.of(player);
        this.furnace = space == null ? new KleinFurnace() : space.furnace();
        this.anvil = space == null ? null : new GhostAnvil(player.getInventory());
        this.window = new SimpleContainer(STORAGE_COUNT);
        this.craftSlots = new TransientCraftingContainer(this, 3, 3);
        this.resultSlots = new ResultContainer();

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                addSlot(new StorageSlot(col + row * COLUMNS,
                        KleinTerminalLayout.slotX(col), KleinTerminalLayout.slotY(row)));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(craftSlots, col + row * 3,
                        KleinTerminalLayout.craftSlotX(col), KleinTerminalLayout.craftSlotY(row)));
            }
        }
        addSlot(new ResultSlot(player, craftSlots, resultSlots, 0,
                KleinTerminalLayout.CRAFT_RESULT_X, KleinTerminalLayout.CRAFT_RESULT_Y));

        addSlot(new Slot(furnace, KleinFurnace.SLOT_INPUT,
                KleinTerminalLayout.FURNACE_INPUT_X, KleinTerminalLayout.FURNACE_INPUT_Y));
        addSlot(new FurnaceFuelSlot(furnace, KleinFurnace.SLOT_FUEL,
                KleinTerminalLayout.FURNACE_FUEL_X, KleinTerminalLayout.FURNACE_FUEL_Y));
        addSlot(new FurnaceOutputSlot(player, furnace, KleinFurnace.SLOT_RESULT,
                KleinTerminalLayout.FURNACE_RESULT_X, KleinTerminalLayout.FURNACE_RESULT_Y));
        addDataSlots(furnaceData);

        addSlot(new Slot(anvilInput, 0,
                KleinTerminalLayout.ANVIL_BASE_X, KleinTerminalLayout.ANVIL_BASE_Y));
        addSlot(new Slot(anvilInput, 1,
                KleinTerminalLayout.ANVIL_MATERIAL_X, KleinTerminalLayout.ANVIL_MATERIAL_Y));
        addSlot(new AnvilOutputSlot(anvilResult, 0,
                KleinTerminalLayout.ANVIL_RESULT_X, KleinTerminalLayout.ANVIL_RESULT_Y));
        addDataSlots(anvilData);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        KleinTerminalLayout.GRID_X + col * 18, KleinTerminalLayout.INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col,
                    KleinTerminalLayout.GRID_X + col * 18, KleinTerminalLayout.HOTBAR_Y));
        }

        refreshWindow();
        windowDirty = false;
    }

    // ===== 视图状态 =====

    public int getScrollRow() {
        return scrollRow;
    }

    public int maxScrollRow() {
        if (space == null) {
            return 0;
        }
        return Math.max(0, space.viewRows() - VISIBLE_ROWS);
    }

    /**
     * 客户端提交搜索词与滚动位置（{@code KleinBottleViewPacket}）。
     * 这里只做校验与落库，真正生效的值随下一个同步包回传——单一权威，
     * 界面和服务端永远对得上。
     */
    public void applyClientView(String search, int requestedRow) {
        if (space == null) {
            return;
        }
        String next = search == null ? "" : search.trim();
        boolean viewChanged = !next.equals(space.search());
        space.setSearch(next);
        int clamped = Math.clamp(requestedRow, 0, maxScrollRow());
        if (clamped != scrollRow) {
            scrollRow = clamped;
            windowDirty = true;
        }
        if (viewChanged) {
            windowDirty = true;
        }
        syncPending = true;
    }

    @Override
    public boolean clickMenuButton(Player clicker, int id) {
        if (!(clicker instanceof ServerPlayer) || space == null) {
            return false;
        }
        switch (id) {
            case BUTTON_HOME -> {
                space.settle();
                scrollRow = 0;
                windowDirty = true;
            }
            case BUTTON_SORT_MODE -> {
                space.setSortMode(nextSortMode(space.sortMode()));
                scrollRow = Math.min(scrollRow, maxScrollRow());
                windowDirty = true;
            }
            case BUTTON_SORT_DIR -> {
                space.setDescending(!space.descending());
                scrollRow = Math.min(scrollRow, maxScrollRow());
                windowDirty = true;
            }
            case BUTTON_SCROLL_UP_PAGE -> {
                scrollRow = Math.max(0, scrollRow - VISIBLE_ROWS);
                windowDirty = true;
            }
            case BUTTON_SCROLL_DOWN_PAGE -> {
                scrollRow = Math.min(maxScrollRow(), scrollRow + VISIBLE_ROWS);
                windowDirty = true;
            }
            case BUTTON_SCROLL_HOME -> {
                scrollRow = 0;
                windowDirty = true;
            }
            case BUTTON_SCROLL_END -> {
                scrollRow = maxScrollRow();
                windowDirty = true;
            }
            default -> {
                return false;
            }
        }
        syncPending = true;
        return true;
    }

    private static FourDimensionalSpace.SortMode nextSortMode(FourDimensionalSpace.SortMode current) {
        FourDimensionalSpace.SortMode[] values = FourDimensionalSpace.SortMode.VALUES;
        return values[(current.ordinal() + 1) % values.length];
    }

    // ===== 存储槽位的取放（全部自己接管）=====

    /**
     * 拦截落在**存储窗口**上的点击。
     *
     * <p>QUICK_CRAFT 故意放给 super：{@link StorageSlot#mayPlace} 是 false，
     * 所以拖拽涂色永远不会把存储槽选进 {@code quickCraftSlots}，
     * 也就不会出现"涂着涂着把东西复制进终端"这种事。
     */
    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player clicker) {
        if (space != null && slotIndex >= 0 && slotIndex < STORAGE_COUNT) {
            switch (input) {
                case PICKUP -> {
                    storagePickup(slotIndex, buttonNum);
                    return;
                }
                case QUICK_MOVE -> {
                    // Shift+左键 = 尽量搬进背包；Shift+右键 = 从光标放回一个。
                    // 后者是必需的：右键只有"光标满了才放回"，不补这一条，
                    // 玩家手上多拿的那个就没法一个一还回去了。
                    if (buttonNum == 1) {
                        storagePlaceOne(slotIndex);
                    } else {
                        moveStorageToPlayer(slotIndex);
                    }
                    return;
                }
                case SWAP -> {
                    storageSwap(slotIndex, buttonNum);
                    return;
                }
                case THROW -> {
                    storageThrow(slotIndex, buttonNum);
                    return;
                }
                case CLONE, PICKUP_ALL -> {
                    // 终端里没有"创造模式复制"和"双击收拢"的语义，直接吞掉
                    return;
                }
                case QUICK_CRAFT -> {
                    // 落到 super（见方法注释）
                }
            }
        }
        super.clicked(slotIndex, buttonNum, input, clicker);
    }

    /** 这一格对应的条目下标；视图末尾之后的空位返回 -1 */
    private int entryAt(int viewSlot) {
        return space == null ? -1 : space.viewStorageIndex(scrollRow * COLUMNS + viewSlot);
    }

    private void storagePickup(int viewSlot, int button) {
        ItemStack carried = getCarried();
        int entryIndex = entryAt(viewSlot);

        if (entryIndex < 0) {
            // 视图末尾之后的空位：光标上有东西就收下，没东西就什么都不做
            if (!carried.isEmpty()) {
                space.insert(carried);
                setCarried(ItemStack.EMPTY);
                markMutated();
            }
            return;
        }

        ItemStack template = space.entry(entryIndex).template().copyWithCount(1);
        boolean same = !carried.isEmpty() && ItemStack.isSameItemSameComponents(carried, template);

        if (button == 1) {
            storagePickupOne(entryIndex, template, carried, same);
            return;
        }

        if (carried.isEmpty()) {
            // 左键：取一整组
            long moved = space.consume(entryIndex, space.stackSize(entryIndex));
            if (moved > 0) {
                setCarried(template.copyWithCount((int) moved));
            }
        } else {
            // 拿着东西左键 → 直接并进这一格（同类合并，异类就是收进空间）
            space.insert(carried);
            setCarried(ItemStack.EMPTY);
        }
        markMutated();
    }

    /**
     * 右键：**再拿一个**。
     *
     * <p>连着点就能一个一个往光标上摞，直到光标满一组——这样"我要拿 17 个"
     * 就是连点 17 下，和在原版箱子里一件件往手上摞是一回事。
     * 光标满了才退化成原版的"放回一个"，否则玩家会卡在"拿不动也放不进"的状态里。
     */
    private void storagePickupOne(int entryIndex, ItemStack template, ItemStack carried, boolean same) {
        if (carried.isEmpty() || (same && carried.getCount() < carried.getMaxStackSize())) {
            long moved = space.consume(entryIndex, 1);
            if (moved <= 0) {
                return;
            }
            setCarried(template.copyWithCount(carried.getCount() + (int) moved));
            markMutated();
            return;
        }
        if (same) {
            // 光标已经满了：右键放回一个（原版语义）
            space.insert(carried.copyWithCount(1));
            carried.shrink(1);
            markMutated();
            return;
        }
        // 拿着别的东**右键**这一格 → 整堆收进空间，再从这一格拿一个出来（＝交换）
        space.insert(carried);
        long moved = space.consume(entryIndex, 1);
        setCarried(moved > 0 ? template.copyWithCount((int) moved) : ItemStack.EMPTY);
        markMutated();
    }

    /**
     * Shift + 右键：从光标上放回**一个**。
     *
     * <p>它是 {@link #storagePickupOne} 的镜像。没有它的时候，"右键连着拿"这条路
     * 有去无回——多拿的那个只能整堆放回去。
     */
    private void storagePlaceOne(int viewSlot) {
        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            return; // 视图末尾的空位也允许放回：空间本来就是一个大桶，不挑格子
        }
        space.insert(carried.copyWithCount(1));
        carried.shrink(1);
        markMutated();
    }

    /** 数字键：快捷栏那一格 ⇄ 这一格 */
    private void storageSwap(int viewSlot, int hotbarIndex) {
        Inventory inventory = player.getInventory();
        if (hotbarIndex < 0 || hotbarIndex >= inventory.getContainerSize()) {
            return;
        }
        ItemStack hotbarStack = inventory.getItem(hotbarIndex);
        if (!hotbarStack.isEmpty()) {
            space.insert(hotbarStack);
            inventory.setItem(hotbarIndex, ItemStack.EMPTY);
        }
        int entryIndex = entryAt(viewSlot);
        if (entryIndex >= 0) {
            ItemStack template = space.entry(entryIndex).template().copyWithCount(1);
            long moved = space.consume(entryIndex, space.stackSize(entryIndex));
            if (moved > 0) {
                inventory.setItem(hotbarIndex, template.copyWithCount((int) moved));
            }
        }
        markMutated();
    }

    private void storageThrow(int viewSlot, int button) {
        int entryIndex = entryAt(viewSlot);
        if (entryIndex < 0) {
            return;
        }
        ItemStack template = space.entry(entryIndex).template().copyWithCount(1);
        int want = button == 1 ? space.stackSize(entryIndex) : 1;
        long moved = space.consume(entryIndex, want);
        if (moved <= 0) {
            return;
        }
        // 26.3 的 Player#drop 多了第三个参数 Prediction（老版本只有两个参数）
        player.drop(template.copyWithCount((int) moved), false, net.minecraft.util.Prediction.SERVER_ONLY);
        markMutated();
    }

    /**
     * Shift + 左键：把这一格**尽可能多地**搬进背包。
     *
     * <p>一格可能有两三千个，而背包一格最多 64，所以不能只搬一次——
     * 循环地"造一个整组 → 用原版搬运塞进背包 → 看塞进去多少"，直到背包满或这一格取空。
     */
    private void moveStorageToPlayer(int viewSlot) {
        int entryIndex = entryAt(viewSlot);
        if (entryIndex < 0) {
            return;
        }
        ItemStack template = space.entry(entryIndex).template().copyWithCount(1);
        int perStack = Math.max(1, template.getMaxStackSize());
        long left = space.total(entryIndex);
        long movedTotal = 0L;
        while (left > 0) {
            int batch = (int) Math.min(perStack, left);
            ItemStack batchStack = template.copyWithCount(batch);
            boolean progressed = moveItemStackTo(batchStack, INV_START, INV_END, true);
            int done = batch - batchStack.getCount();
            if (!progressed || done <= 0) {
                break; // 背包塞不下了
            }
            movedTotal += done;
            left -= done;
        }
        if (movedTotal > 0) {
            space.consume(entryIndex, movedTotal);
            markMutated();
        }
    }

    private void markMutated() {
        windowDirty = true;
        syncPending = true;
    }

    // ===== 窗口装填 / 同步 =====

    /**
     * 把当前滚动位置的这一段视图装进窗口容器。
     *
     * <p>只能走 {@code getItems().set(...)}：走 {@code setItem()} 会回调
     * {@code setChanged()}，而那是"这一格被原版改过了"的信号，会绕开我们自己的取放逻辑。
     * 装进去的是**副本**，免得原版就地改槽位里那个 ItemStack。
     */
    private void refreshWindow() {
        if (space == null) {
            return; // 客户端：窗口内容由原版槽位同步负责，这里碰一下就清空了
        }
        int base = scrollRow * COLUMNS;
        for (int i = 0; i < STORAGE_COUNT; i++) {
            ItemStack stack = space.viewItem(base + i);
            window.getItems().set(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
    }

    @Override
    public void broadcastChanges() {
        if (space != null) {
            // 1) 先收尾：清掉被取空的条目、按当前搜索/排序重建视图
            if (space.settle()) {
                scrollRow = Math.clamp(scrollRow, 0, maxScrollRow());
                windowDirty = true;
                syncPending = true;
            }
            // 2) 再装填：必须在 super 之前，super 会拿 lastSlots 比对并把变更发下去
            if (windowDirty) {
                refreshWindow();
                windowDirty = false;
            }
            // 3) 熔炉的火与进度：走原版 ContainerData 通道（addDataSlots 会自动发变更）
            KleinFurnace active = space.furnace();
            furnaceData.set(KleinFurnace.DATA_BURN_TIME, active.burnTime());
            furnaceData.set(KleinFurnace.DATA_BURN_DURATION, active.burnDuration());
            furnaceData.set(KleinFurnace.DATA_COOK_TIME, active.cookTime());
            furnaceData.set(KleinFurnace.DATA_COOK_DURATION, active.cookDuration());
            // 4) 最后推视图快照（每格总量 + 统计），这个走独立包
            if (syncPending) {
                sendSync();
                syncPending = false;
            }
        }
        super.broadcastChanges();
    }

    // ===== 熔炉进度（客户端读它来画火焰与进度条）=====

    public boolean furnaceLit() {
        return furnaceData.get(KleinFurnace.DATA_BURN_TIME) > 0;
    }

    /** 剩余燃料比例：火焰从下往上烧掉 */
    public float burnProgress() {
        int duration = furnaceData.get(KleinFurnace.DATA_BURN_DURATION);
        return duration <= 0 ? 0.0F
                : Mth.clamp((float) furnaceData.get(KleinFurnace.DATA_BURN_TIME) / duration, 0.0F, 1.0F);
    }

    /** 当前这一格的熔炼进度 */
    public float cookProgress() {
        int duration = furnaceData.get(KleinFurnace.DATA_COOK_DURATION);
        return duration <= 0 ? 0.0F
                : Mth.clamp((float) furnaceData.get(KleinFurnace.DATA_COOK_TIME) / duration, 0.0F, 1.0F);
    }

    private void sendSync() {
        if (!(player instanceof ServerPlayer serverPlayer) || space == null) {
            return;
        }
        int base = scrollRow * COLUMNS;
        List<Integer> totals = new ArrayList<>(STORAGE_COUNT);
        for (int i = 0; i < STORAGE_COUNT; i++) {
            long total = space.total(space.viewStorageIndex(base + i));
            totals.add((int) Math.min(Integer.MAX_VALUE, total));
        }
        KleinBottleSyncPacket.send(serverPlayer, new KleinBottleSyncPacket(
                scrollRow,
                VISIBLE_ROWS,
                space.viewSize(),
                space.totalItems(),
                space.search(),
                space.sortMode().ordinal(),
                space.descending(),
                totals));
    }

    // ===== 原版搬运 =====

    @Override
    public ItemStack quickMoveStack(Player clicker, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack clicked = slot.getItem().copy();

        if (index < STORAGE_COUNT) {
            // 存储 → 背包：不能走原版搬运（源头的总量远超一组），自己循环搬
            moveStorageToPlayer(index);
            return clicked;
        }
        if (index == RESULT_SLOT) {
            return quickMoveResult(clicker, slot);
        }
        if (index == FURNACE_RESULT) {
            return quickMoveFurnaceResult(clicker, slot);
        }
        if (index == ANVIL_RESULT) {
            return quickMoveAnvilResult(clicker, slot);
        }
        if (index == FURNACE_INPUT || index == FURNACE_FUEL
                || index == ANVIL_BASE || index == ANVIL_MATERIAL) {
            // 熔炉/铁砧 → 背包：普通槽位，照原版的搬运写法（就地改 slot.getItem() 再 setChanged）
            ItemStack stack = slot.getItem();
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return clicked;
        }
        if (index < INV_START) {
            // 合成网格 → 先进四维空间（那才是仓库），再回落背包
            ItemStack stack = slot.getItem();
            if (space != null) {
                int moved = space.insert(stack);
                if (moved > 0) {
                    stack.shrink(moved);
                    markMutated();
                }
            }
            if (!stack.isEmpty()) {
                moveItemStackTo(stack, INV_START, INV_END, true);
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return clicked;
        }

        // 背包 → 存储：**直接调 insert**，不要用 moveItemStackTo。
        // 原版只能看见窗口这 54 格，格子都被别的东西占着就"搬不动"了——
        // 而这里背后是种类数不设上限的四维空间，没有搬不动这回事。
        if (space == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        int moved = space.insert(stack);
        if (moved <= 0) {
            return ItemStack.EMPTY;
        }
        stack.shrink(moved);
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        markMutated();
        return clicked;
    }

    /**
     * 产物槽的 Shift + 左键。
     *
     * <p>{@code slot.onTake(player, stack)} 是**必须**的：原版 {@link ResultSlot} 靠它
     * 消耗合成网格、返还桶/瓶之类的容器残留物、记统计。漏掉这句就是
     * "产物拿走了、网格一个都没少"＝无限复制。
     */
    private ItemStack quickMoveResult(Player clicker, Slot slot) {
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        // 产物优先进四维空间
        if (space != null) {
            int moved = space.insert(stack);
            if (moved > 0) {
                stack.shrink(moved);
                markMutated();
            }
        }
        if (!stack.isEmpty()) {
            moveItemStackTo(stack, INV_START, INV_END, true);
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == clicked.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(clicker, stack);
        return clicked;
    }

    /**
     * 熔炉产物的 Shift + 左键。
     *
     * <p>{@code slot.onTake(...)} 是**必须**的：熔炉的经验就挂在产物被取走这个时机上
     * （原版熔炉把经验存在方块实体里，玩家取出产物时才结算）。
     */
    private ItemStack quickMoveFurnaceResult(Player clicker, Slot slot) {
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack clicked = stack.copy();
        if (!moveItemStackTo(stack, INV_START, INV_END, true)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(clicker, clicked);
        return clicked;
    }

    /** 数据挂在玩家身上、不依附方块，所以永远有效 */
    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /**
     * 铁砧产物的 Shift + 左键。
     *
     * <p>{@code slot.onTake(...)} 是**必须**的：铁砧的"扣经验、消耗材料"全挂在产物
     * 被取走这个时机上（原版 {@code AnvilMenu#onTake}）。
     */
    private ItemStack quickMoveAnvilResult(Player clicker, Slot slot) {
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack clicked = stack.copy();
        if (!moveItemStackTo(stack, INV_START, INV_END, true)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(clicker, clicked);
        return clicked;
    }

    /** 双击拾取时不要把产物槽算进去（照抄原版 CraftingMenu） */
    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != this.resultSlots && super.canTakeItemForPickAll(carried, target);
    }

    // ===== 合成 =====

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == anvilInput) {
            updateAnvilResult();
            return;
        }
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

    // ===== 铁砧 =====

    /**
     * 把两个输入格喂给隐形铁砧，让它算结果与代价。
     *
     * <p>往 ghost 的槽位 {@code set(...)} 会走 {@code SimpleContainer#setChanged} →
     * {@code ItemCombinerMenu#slotsChanged} → {@code createResult()}，
     * 所以不用手动调 {@code createResult}。
     * {@code anvilUpdating} 是防重入闸：回写产物时会再触发一次 {@code slotsChanged}，
     * 不拦住就是无限递归。
     */
    private void updateAnvilResult() {
        if (anvil == null || anvilUpdating) {
            return;
        }
        anvilUpdating = true;
        try {
            anvil.getSlot(AnvilMenu.INPUT_SLOT).set(anvilInput.getItem(0).copy());
            anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(anvilInput.getItem(1).copy());
            anvilResult.setItem(0, anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem().copy());
            anvilData.set(0, anvil.getCost());
        } finally {
            anvilUpdating = false;
        }
    }

    /**
     * 客户端把重命名输入框的内容发过来（{@code KleinAnvilNamePacket}）。
     * {@code setItemName} 自己会判"没变就跳过"并重算结果。
     */
    public void applyAnvilName(String name) {
        if (anvil == null || anvilUpdating) {
            return;
        }
        anvilUpdating = true;
        try {
            anvil.setItemName(name);
        } finally {
            anvilUpdating = false;
        }
        anvilResult.setItem(0, anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem().copy());
        anvilData.set(0, anvil.getCost());
    }

    /** 当前这条铁砧操作要几级经验（客户端画「需要 X 级」用） */
    public int anvilCost() {
        return anvilData.get(0);
    }

    /** 铁砧两个输入格；关界面时把没用完的送回四维空间 */
    private void returnAnvilInputs() {
        for (int i = 0; i < anvilInput.getContainerSize(); i++) {
            ItemStack left = anvilInput.getItem(i);
            if (!left.isEmpty()) {
                space.insert(left);
            }
        }
        anvilInput.clearContent();
        if (anvil != null) {
            anvil.getSlot(AnvilMenu.INPUT_SLOT).set(ItemStack.EMPTY);
            anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(ItemStack.EMPTY);
            anvil.getSlot(AnvilMenu.RESULT_SLOT).set(ItemStack.EMPTY);
        }
        anvilResult.setItem(0, ItemStack.EMPTY);
        anvilData.set(0, 0);
    }

    /**
     * 铁砧的输出格：只出不进，能不能拿走由隐形铁砧说了算（经验够不够、代价是不是 0）。
     * 客户端没有隐形铁砧，就退化为"有东西就能拿"——真正的校验在服务端那一次点击里。
     */
    private final class AnvilOutputSlot extends Slot {

        AnvilOutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player taker) {
            if (anvil == null) {
                return hasItem();
            }
            return anvil.canTake(taker, hasItem());
        }

        @Override
        public void onTake(Player taker, ItemStack stack) {
            if (anvil != null) {
                anvil.consume(taker, stack);   // 扣经验、消耗材料、触发 AnvilCraft 事件
            }
            anvilUpdating = true;
            try {
                anvilInput.setItem(0, ItemStack.EMPTY);
                anvilInput.setItem(1, ItemStack.EMPTY);
                anvilResult.setItem(0, ItemStack.EMPTY);
            } finally {
                anvilUpdating = false;
            }
            anvilData.set(0, anvil == null ? 0 : anvil.getCost());
            super.onTake(taker, stack);
        }
    }

    /**
     * 服务端专用的隐形铁砧：只为把 {@code mayPickup}/{@code onTake} 这两个 protected
     * 方法暴露出来——它们在 {@link AnvilMenu} 里是 protected，别的包拿不到。
     */
    private static final class GhostAnvil extends AnvilMenu {

        GhostAnvil(Inventory inventory) {
            super(0, inventory);
        }

        boolean canTake(Player taker, boolean hasResult) {
            return mayPickup(taker, hasResult);
        }

        void consume(Player taker, ItemStack stack) {
            onTake(taker, stack);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (space == null) {
            return;
        }
        // 关界面时把网格里没用完的东西送回四维空间，而不是丢在地上
        for (int i = 0; i < CRAFT_COUNT; i++) {
            ItemStack left = craftSlots.getItem(i);
            if (!left.isEmpty()) {
                space.insert(left);
            }
        }
        craftSlots.clearContent();
        returnAnvilInputs();
    }

    // ===== JEI 配料传输（服务端） =====

    /**
     * 按配方 id 从四维空间里抓材料填内嵌的 3×3 网格。
     *
     * <p>配料清单与"哪个格子放第几项"来自 26.3 的 {@link PlacementInfo}。⚠️ 但它的
     * {@code slotsToIngredientIndex()} 用的是<b>配方自身网格</b>的坐标——2×2 配方长度是 4、
     * 1×3 配方长度是 3，都不是 9。所以有形配方必须再拿 {@code ShapedRecipe#getWidth()}
     * 换算一次 {@code cell = (s / w) * 3 + (s % w)} 才能落到 3×3 网格上。
     * 无形配方没有宽度，直接按顺序占 0..n-1（本来就不关心位置）。
     * 填完再用 {@code recipe.matches(...)} 自检，不匹配就整单撤销、原样退回四维空间。
     */
    public static void transfer(ServerPlayer player, Identifier recipeId, boolean maxTransfer) {
        if (!(player.containerMenu instanceof KleinBottleMenu menu) || menu.space == null) {
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
        for (int i = 0; i < CRAFT_COUNT; i++) {
            ItemStack old = menu.craftSlots.getItem(i);
            if (!old.isEmpty()) {
                space.insert(old);
            }
        }

        int recipeWidth = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 0;

        NonNullList<ItemStack> placed = NonNullList.withSize(CRAFT_COUNT, ItemStack.EMPTY);
        boolean ok = true;
        for (int s = 0; s < slotsToIngredient.size() && ok; s++) {
            int ingIndex = slotsToIngredient.getInt(s);
            if (ingIndex == PlacementInfo.EMPTY_SLOT || ingIndex < 0 || ingIndex >= ingredients.size()) {
                continue;
            }
            Ingredient ing = ingredients.get(ingIndex);
            // 配方自身网格 → 3×3 网格
            int cell = recipeWidth > 0 ? (s / recipeWidth) * 3 + (s % recipeWidth) : s;
            if (ing.isEmpty() || cell >= CRAFT_COUNT) {
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
            for (int i = 0; i < CRAFT_COUNT; i++) {
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
            for (int i = 0; i < CRAFT_COUNT; i++) {
                menu.craftSlots.setItem(i, ItemStack.EMPTY);
            }
        }
        menu.markMutated();
    }

    /**
     * 视图槽位。
     *
     * <p>刻意把 {@code mayPlace} / {@code mayPickup} 都关掉：存储的一格背后是
     * {@code (template, long total)}，原版那套"往槽位里塞一个 ItemStack"的算术
     * 只会动那个夹到 64 的展示栈，剩下的总量会凭空消失或复制。
     * 所有取放都走 {@link KleinBottleMenu#clicked} 自己实现；
     * 关掉这两个开关还能顺带保证拖拽涂色 / 原版搬运都不会误伤存储。
     */
    private final class StorageSlot extends Slot {

        StorageSlot(int index, int x, int y) {
            super(window, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    /**
     * 熔炉燃料格：只收真燃料。
     *
     * <p>26.3 判定燃料就是 {@code stack.has(DataComponents.COOKING_FUEL)}
     * （原版 {@code AbstractFurnaceMenu#isFuel} 就是这么写的，熔岩桶之所以能烧
     * 也是因为它带这个组件）。
     */
    private static final class FurnaceFuelSlot extends Slot {

        FurnaceFuelSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return KleinFurnace.isFuel(stack);
        }
    }

    /**
     * 熔炉产物格：只出不进。经验在 {@link #onTake} 里结算——
     * 和原版一样，是"玩家把产物拿走"的那一刻才给经验，不是烧好的那一刻。
     */
    private static final class FurnaceOutputSlot extends Slot {

        private final Player player;
        private final KleinFurnace furnace;

        FurnaceOutputSlot(Player player, KleinFurnace furnace, int index, int x, int y) {
            super(furnace, index, x, y);
            this.player = player;
            this.furnace = furnace;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player takenBy, ItemStack stack) {
            stack.onCraftedBy(player, stack.getCount());
            furnace.grantExperience(player);
            super.onTake(takenBy, stack);
        }
    }
}
