package org.gwfx.zuoyanmod.item;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.gwfx.zuoyanmod.core.KleinTerminalLayout;
import org.gwfx.zuoyanmod.menu.KleinBottleMenu;
import org.gwfx.zuoyanmod.platform.RegistryLookup;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * 「四维空间」：属于**每个玩家**的无限存储终端。
 *
 * <h2>数据模型：一格一种物品，总量存 long</h2>
 * 一条条目 = {@code (模板 ItemStack, 总量 long)}，**同一种东西永远只有一条**。
 * 这与"每条目就是一个普通 ItemStack（≤64）"的老模型是本质区别：
 * 老模型下 3300 个石头会摊成 52 格，玩家看到满屏同一种东西；
 * 现在它就是一格，右下角写 {@code 3.3k}——这才是 AE2 / RS2 的手感。
 *
 * <p>因此**取放必须自己接管**，不能走原版的槽位算术：原版 {@code Slot.remove(n)}
 * 只认那个夹到 64 的展示栈，取走一组就会把"这一格空了"当成事实，3300 会凭空消失。
 * 所有交互都在 {@code KleinBottleMenu#clicked} 里按"从总量里扣"来实现。
 *
 * <h2>视图模型</h2>
 * 存储本体 {@link #entries} 保持录入顺序；界面看到的顺序是
 * {@link #view} = "过滤 + 排序后的条目下标数组"，搜索/排序只重排它。
 *
 * <h2>1.20.1 与 26.3 的差别</h2>
 * 26.3 挂在 NeoForge 玩家 Attachment 上、用 ValueInput/ValueOutput + Codec 存档；
 * 1.20.1 挂在 Forge Capability 上（见 {@link FourDimensionalSpaceCapability}），
 * 存档走原生 CompoundTag NBT。
 */
public class FourDimensionalSpace {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 终端的列数（单一事实来源在 core 的 KleinTerminalLayout，与 GUI 布局常量绑定） */
    public static final int COLUMNS = KleinTerminalLayout.COLUMNS;
    public static final int DEFAULT_ROWS = KleinTerminalLayout.ROWS;

    /** 终端排序方式。语义与 AE2 的 SortOrder / RS2 的 GridSortingTypes 对应。 */
    public enum SortMode {
        /** 显示名（本地化后的名字，玩家看到的那个） */
        NAME,
        /** 总量 */
        COUNT,
        /** 注册名：同模组的东西天然聚在一起 */
        REGISTRY,
        /** 录入顺序：先放进去的排前面 */
        RECENT;

        public static final SortMode[] VALUES = values();

        public static SortMode byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : NAME;
        }
    }

    /**
     * 一条条目：模板（count 恒为 1）+ 总量。
     */
    public record Entry(ItemStack template, long total) {
        public boolean isEmpty() {
            return total <= 0 || template.isEmpty();
        }
    }

    /** 拿到该玩家的四维空间（Forge Capability，见 FourDimensionalSpaceCapability） */
    public static FourDimensionalSpace of(Player player) {
        return player.getCapability(FourDimensionalSpaceCapability.FOUR_DIMENSIONAL_SPACE)
                .orElseThrow(() -> new IllegalStateException("FourDimensionalSpace capability missing on player " + player));
    }

    // ===== 存储本体 =====

    private final List<Entry> entries = new ArrayList<>();
    /**
     * 终端右栏那台随身熔炉。放在这里而不是菜单里，是为了让它跟着玩家存档、
     * 关着界面也继续烧（推进见 {@code KleinBottleItem#inventoryTick}）。
     */
    private final KleinFurnace furnace = new KleinFurnace();
    private SortMode sortMode = SortMode.RECENT;
    private boolean descending;
    private String search = "";

    /** 有 total = 0 的空条目待清理（列表长度变了，视图下标会挪） */
    private boolean dirty;
    /** 视图的**内容或顺序**可能变了（增删条目不一定要清理，但视图一定要重建） */
    private boolean viewStale;
    /** 过滤 + 排序后的条目下标数组 */
    private int[] view = new int[0];

    /** 条目数 = 物品种类数 */
    public int size() {
        return entries.size();
    }

    /** 终端右栏那台随身熔炉（跟着玩家存档，关着界面也继续烧） */
    public KleinFurnace furnace() {
        return furnace;
    }

    public Entry entry(int index) {
        return index >= 0 && index < entries.size() ? entries.get(index) : null;
    }

    /** 全部条目的总个数（页脚统计用） */
    public long totalItems() {
        long n = 0;
        for (Entry e : entries) {
            n += e.total();
        }
        return n;
    }

    /**
     * 给界面/原版用的**展示栈**：模板不变，count 固定为 1。
     * 面板上那格显示的"这格有多少"靠的是界面自己画的叠字（{@link #total(int)}）。
     * <p>
     * 1.20.1 的 {@code AbstractContainerScreen#renderSlot} 是私有的，没法像 26.x 那样
     * 整个换掉存储槽的绘制；把展示栈的 count 固定为 1 可以让原版**不画堆叠数字**，
     * 界面再在 {@code super.render} 之后自绘真实总量，同样避免两个数字重叠。
     */
    public ItemStack display(int index) {
        Entry e = entry(index);
        if (e == null || e.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return e.template().copyWithCount(1);
    }

    public long total(int index) {
        Entry e = entry(index);
        return e == null ? 0L : e.total();
    }

    /** 一格的堆叠上限（取一组就是取它） */
    public int stackSize(int index) {
        Entry e = entry(index);
        return e == null ? 64 : Math.max(1, e.template().getMaxStackSize());
    }

    // ===== 视图状态 =====

    public String search() {
        return search;
    }

    public void setSearch(String value) {
        String next = value == null ? "" : value.trim();
        if (!next.equals(search)) {
            search = next;
            rebuildView();
        }
    }

    public SortMode sortMode() {
        return sortMode;
    }

    public void setSortMode(SortMode mode) {
        if (mode != null && mode != sortMode) {
            sortMode = mode;
            rebuildView();
        }
    }

    public boolean descending() {
        return descending;
    }

    public void setDescending(boolean value) {
        if (value != descending) {
            descending = value;
            rebuildView();
        }
    }

    public int viewSize() {
        return view.length;
    }

    /** 视图第 index 位的展示栈（count 已夹到堆叠上限） */
    public ItemStack viewItem(int index) {
        return display(viewStorageIndex(index));
    }

    /** 视图第 index 位对应的**条目下标**；越界返回 -1 */
    public int viewStorageIndex(int index) {
        return index >= 0 && index < view.length ? view[index] : -1;
    }

    /** 视图总行数（滚动条按行算） */
    public int viewRows() {
        return (viewSize() + COLUMNS - 1) / COLUMNS;
    }

    // ===== 过滤 / 排序 =====

    /**
     * 重建视图。查询语法（比 RS2 的查询语言简单，够用就行）：
     * <ul>
     *   <li>空格分隔的多个词：全部命中才算（AND）</li>
     *   <li>{@code @前缀}：按模组过滤——匹配命名空间**或模组显示名**，都是前缀匹配。</li>
     *   <li>{@code -词}：排除，例如 {@code -石头}</li>
     * </ul>
     * 匹配对象是**玩家看到的显示名**和**注册名 id**。
     */
    public void rebuildView() {
        List<String> required = new ArrayList<>();
        List<String> excluded = new ArrayList<>();
        List<String> modFilters = new ArrayList<>();
        for (String raw : search.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (raw.isEmpty()) {
                continue;
            }
            if (raw.startsWith("@")) {
                if (raw.length() > 1) {
                    modFilters.add(raw.substring(1));
                }
            } else if (raw.startsWith("-")) {
                if (raw.length() > 1) {
                    excluded.add(raw.substring(1));
                }
            } else {
                required.add(raw);
            }
        }

        List<Integer> indices = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (e.isEmpty()) {
                continue;
            }
            ResourceLocation id = RegistryLookup.itemId(e.template().getItem());
            String haystack = (e.template().getHoverName().getString() + " " + id).toLowerCase(Locale.ROOT);
            if (!modFilters.isEmpty() && !matchesMod(id, modFilters)) {
                continue;
            }
            if (!required.stream().allMatch(haystack::contains)) {
                continue;
            }
            if (excluded.stream().anyMatch(haystack::contains)) {
                continue;
            }
            indices.add(i);
        }

        Comparator<Integer> comparator = comparator();
        indices.sort(descending ? comparator.reversed() : comparator);
        view = new int[indices.size()];
        for (int i = 0; i < view.length; i++) {
            view[i] = indices.get(i);
        }
    }

    /** 模组过滤：命名空间或模组显示名，**前缀匹配**。 */
    private static boolean matchesMod(ResourceLocation id, List<String> filters) {
        String namespace = id.getNamespace();
        String displayName = null;
        for (String filter : filters) {
            if (namespace.equals(filter) || namespace.startsWith(filter)) {
                return true;
            }
            if (displayName == null) {
                displayName = modDisplayName(namespace);
            }
            if (displayName != null && displayName.toLowerCase(Locale.ROOT).contains(filter)) {
                return true;
            }
        }
        return false;
    }

    /** 模组显示名（匹配不到就返回 null；ModList 没起来也不炸） */
    private static String modDisplayName(String namespace) {
        try {
            return net.minecraftforge.fml.ModList.get()
                    .getModContainerById(namespace)
                    .map(container -> container.getModInfo().getDisplayName())
                    .orElse(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private Comparator<Integer> comparator() {
        return switch (sortMode) {
            case NAME -> Comparator.comparing(
                    i -> entries.get(i).template().getHoverName().getString(),
                    Comparator.naturalOrder());
            case COUNT -> Comparator.comparingLong(i -> entries.get(i).total());
            // 注册名排序用注册表的数字 id：同模组的东西天然连在一起
            case REGISTRY -> Comparator.comparingInt(i -> RegistryLookup.itemNumericId(entries.get(i).template().getItem()));
            case RECENT -> Comparator.comparingInt(i -> i);
        };
    }

    // ===== 写入 =====

    /**
     * 放入：同一种东西并进已有的那一条，没有就新建一条。
     * @return 实际放入数量
     */
    public int insert(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        int count = stack.getCount();
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            // 1.20.1 的同类判定是 isSameItemSameTags（26.x 改名 isSameItemSameComponents）
            if (!e.isEmpty() && ItemStack.isSameItemSameTags(e.template(), stack)) {
                entries.set(i, new Entry(e.template(), e.total() + count));
                // 注意：**不在这里 rebuildView**。一次界面交互会在同一 tick 里连续
                // insert / consume 好几次，中途重排视图会让后续写入打错条目。
                if (count > 0) {
                    viewStale = true;
                }
                return count;
            }
        }
        entries.add(new Entry(stack.copyWithCount(1), count));
        viewStale = true;
        return count;
    }

    /**
     * 从某一条里扣掉 amount 个。
     * <p>
     * 扣空时**不把条目从列表里删掉**，而是留一条 total = 0 的空条目——
     * 列表长度不变，{@link #view} 里的下标就还是稳的。
     *
     * @return 实际扣掉的数量（可能小于 amount，说明这一条不够了）
     */
    public long consume(int index, long amount) {
        Entry e = entry(index);
        if (e == null || e.isEmpty() || amount <= 0) {
            return 0L;
        }
        long moved = Math.min(amount, e.total());
        long left = e.total() - moved;
        if (left <= 0) {
            entries.set(index, new Entry(e.template(), 0L));
            dirty = true;
        } else {
            entries.set(index, new Entry(e.template(), left));
        }
        return moved;
    }

    /** 把光标上的东西整个收进空间（同种合并）。返回实际收下的数量 */
    public int absorb(ItemStack stack) {
        return insert(stack);
    }

    /**
     * 收尾：把这一 tick 里被取空的条目清掉并重建视图。
     *
     * @return 是否有变化（调用方据此决定要不要重新装填窗口）
     */
    public boolean settle() {
        if (!dirty && !viewStale) {
            return false;
        }
        dirty = false;
        viewStale = false;
        entries.removeIf(Entry::isEmpty);
        rebuildView();
        return true;
    }

    /** 数一数有多少个符合条件的东西（JEI 配料前先探一下够不够） */
    public int countMatching(Predicate<ItemStack> filter) {
        long n = 0;
        for (Entry e : entries) {
            if (!e.isEmpty() && filter.test(e.template())) {
                n += e.total();
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, n);
    }

    /**
     * 从第一条符合条件的条目里取走 count 个，取不到就返回 EMPTY。
     */
    public ItemStack take(Predicate<ItemStack> filter, int count) {
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (e.isEmpty() || !filter.test(e.template())) {
                continue;
            }
            long moved = consume(i, count);
            if (moved <= 0) {
                return ItemStack.EMPTY;
            }
            return e.template().copyWithCount((int) moved);
        }
        return ItemStack.EMPTY;
    }

    // ===== 打开界面 =====

    public void openTerminal(ServerPlayer player) {
        rebuildView();
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new KleinBottleMenu(id, inventory, p),
                Component.translatable("gui.zuoyanmod.klein_bottle.title")));
    }

    // ===== 持久化（1.20.1：原生 CompoundTag NBT） =====

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Entry e : entries) {
            if (e.isEmpty()) {
                continue;
            }
            CompoundTag entryTag = new CompoundTag();
            entryTag.put("item", e.template().save(new CompoundTag()));
            entryTag.putLong("count", e.total());
            list.add(entryTag);
        }
        tag.put("Entries", list);
        tag.putString("SortMode", sortMode.name());
        tag.putBoolean("SortDescending", descending);
        tag.putString("Search", search);
        tag.put("Furnace", furnace.serialize());
        return tag;
    }

    public void deserialize(CompoundTag tag) {
        entries.clear();
        ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            ItemStack template = ItemStack.of(entryTag.getCompound("item"));
            long total = entryTag.getLong("count");
            if (!template.isEmpty() && total > 0) {
                entries.add(new Entry(template, total));
            }
        }

        furnace.deserialize(tag.getCompound("Furnace"));

        descending = tag.getBoolean("SortDescending");
        search = tag.getString("Search");
        String mode = tag.getString("SortMode");
        sortMode = SortMode.NAME;
        for (SortMode candidate : SortMode.VALUES) {
            if (candidate.name().equals(mode)) {
                sortMode = candidate;
                break;
            }
        }
        rebuildView();
    }

    /**
     * 兼容旧存档：老版本把存储存成"每格一个普通 ItemStack"的 {@code Items} 列表，
     * 同一种东西可能散在几十条里。这里把它们按种类并起来，
     * 免得玩家升级模组之后一仓库东西全没了。
     */
    public void migrateLegacyItems(CompoundTag tag) {
        if (!tag.contains("Items")) {
            return;
        }
        ListTag legacy = tag.getList("Items", Tag.TAG_COMPOUND);
        boolean any = false;
        for (int i = 0; i < legacy.size(); i++) {
            ItemStack stack = ItemStack.of(legacy.getCompound(i));
            if (!stack.isEmpty()) {
                insert(stack);
                any = true;
            }
        }
        if (any) {
            LOGGER.info("[FourDimensionalSpace] 已把旧版逐格存档合并成 {} 种物品", entries.size());
        }
    }
}
