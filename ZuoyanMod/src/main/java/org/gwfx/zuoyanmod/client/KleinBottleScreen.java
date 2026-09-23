package org.gwfx.zuoyanmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.gwfx.zuoyanmod.core.AmountFormat;
import org.gwfx.zuoyanmod.core.KleinTerminalLayout;
import org.gwfx.zuoyanmod.client.klein.KleinAmountRenderer;
import org.gwfx.zuoyanmod.client.klein.KleinScrollbar;
import org.gwfx.zuoyanmod.client.klein.KleinSearchBox;
import org.gwfx.zuoyanmod.client.klein.KleinSideButton;
import org.gwfx.zuoyanmod.client.klein.KleinTheme;
import org.gwfx.zuoyanmod.item.FourDimensionalSpace;
import org.gwfx.zuoyanmod.menu.KleinBottleMenu;
import org.gwfx.zuoyanmod.network.KleinBottleSyncPacket;
import org.gwfx.zuoyanmod.network.PacketHandler;

import java.util.List;

/**
 * 「四维空间」终端界面：**存储 + 工作台一体**。
 *
 * <pre>
 * ┌──────────────────────────────────────────────┬──────────────────┐
 * │ 四维空间                     57 种 · 3,587 个 │  合成            │  左列 = 存储，右栏 = 合成 + 熔炉
 * │ ┌ ⌕ 搜索物品 · @模组 · -排除 ┐                │ ┌──┬──┬──┐       │
 * │ └───────────────────────────┘                │ │  │  │  │ →  ┌┐ │  3×3 网格 + 产物槽，
 * │ [↑][序][↓][×]   录入顺序 ↑ 6/7                │ ├──┼──┼──┤    └┘ │  **不用点按钮切界面**
 * │ ┌──────────────────────────────┐ ┌▮┐          │ └──┴──┴──┘       │
 * │ │      9 × 6 存储视图窗口       │ │ │          │ ─────────        │
 * │ └──────────────────────────────┘ └▯┘          │  熔炼             │  下半是一台真熔炉：
 * │ ────────────────────────────────              │ ┌┐      ┌┐       │  输入 / 燃料 / 产物三格
 * │ 物品栏                                ┌∞┐      │ ││ 🔥   │ │       │  **关着界面也继续烧**
 * │ [ 背包 3 行 + 快捷栏 ]                │ │      │ └┘ ──→  └┘       │
 * │                                       └∞┘      │    （涡环）      │
 * └──────────────────────────────────────────────┴──────────────────┘
 * </pre>
 *
 * <h2>操作</h2>
 * <table>
 *   <tr><td>左键条目</td><td>取一组（该物品的堆叠上限）到光标</td></tr>
 *   <tr><td>右键条目</td><td><b>再拿一个</b>——连着点就一个一个往光标上摞，直到满一组</td></tr>
 *   <tr><td>光标拿着同类东西左键条目</td><td>并进那一格（不限量）</td></tr>
 *   <tr><td>光标拿着别的东西右键条目</td><td>整堆收进空间，再从这一格拿一个出来（＝交换）</td></tr>
 *   <tr><td>Shift + 左键条目</td><td>反复搬整组进背包，直到背包塞不下或这一格取空</td></tr>
 *   <tr><td>Shift + 右键条目</td><td>从光标放回一个</td></tr>
 *   <tr><td>数字键</td><td>快捷栏那一格 ⇄ 这一格</td></tr>
 *   <tr><td>Q / Ctrl+Q</td><td>丢一个 / 丢一组</td></tr>
 *   <tr><td>滚轮 / 拖滚动条</td><td>视图滚动（一行；Ctrl 一屏）</td></tr>
 *   <tr><td>PageUp/PageDown/Home/End</td><td>翻屏 / 到顶 / 到底</td></tr>
 *   <tr><td>F</td><td>聚焦搜索框（Esc 退出搜索框，再按一次关界面）</td></tr>
 *   <tr><td>JEI 配方上的 +</td><td>材料从四维空间直接填进右栏的 3×3</td></tr>
 * </table>
 *
 * <h2>26.3 的两条界面约定（踩过坑）</h2>
 * <ul>
 *   <li>{@code extractBackground} 里是**屏幕绝对坐标**（要加 leftPos/topPos）；</li>
 *   <li>{@code extractLabels} / {@code extractSlot} / 原版高亮都在
 *       {@code translate(leftPos, topPos)} **之后**调用，用的是界面局部坐标。
 *       所以 {@code extractContents} 里 {@code super} 返回时矩阵已弹回，之后画的
 *       滚动条、扫光要按<b>绝对坐标</b>来。</li>
 * </ul>
 */
public class KleinBottleScreen extends AbstractContainerScreen<KleinBottleMenu> {

    private static final int IMAGE_W = KleinTerminalLayout.IMAGE_W;
    private static final int IMAGE_H = KleinTerminalLayout.IMAGE_H;
    private static final int STORAGE_COUNT = KleinBottleMenu.STORAGE_COUNT;
    private static final int VISIBLE_ROWS = KleinBottleMenu.VISIBLE_ROWS;

    /** 搜索去抖：4 tick ≈ 200ms。逐字发包没必要，打字时也省得服务端跟着重建视图 */
    private static final int SEARCH_DEBOUNCE_TICKS = 4;
    /** 发出滚动请求后，多少 tick 内忽略服务端回传的旧值，免得手柄被"弹回去" */
    private static final int SCROLL_ECHO_TICKS = 20;

    private KleinSearchBox searchBox;
    private KleinScrollbar scrollbar;
    /** 四个功能按钮：真控件（v6 之前是手画 + 手写命中判定，点了没反应） */
    private KleinSideButton[] sideButtons;
    /** 铁砧的重命名输入框（真 EditBox；内容随输入发到服务端那份隐形铁砧） */
    private EditBox anvilName;

    /** 换了一个菜单才清空视图快照；窗口缩放引起的 init() 不该把总量清掉 */
    private int boundContainerId = -1;
    private boolean searchInitialized;
    private int searchDebounce;
    private int pendingScrollRow = -1;
    private int scrollEchoTicks;
    private KleinBottleSyncPacket seenSnapshot;

    public KleinBottleScreen(KleinBottleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_W, IMAGE_H);
        this.titleLabelX = KleinTerminalLayout.TITLE_X;
        this.titleLabelY = KleinTerminalLayout.TITLE_Y;
        this.inventoryLabelX = KleinTerminalLayout.GRID_X;
        this.inventoryLabelY = KleinTerminalLayout.INV_LABEL_Y;
    }

    @Override
    protected void init() {
        // 记下旧搜索词：窗口缩放会重建控件，不该把玩家输入的文字弄丢
        String previousSearch = searchBox == null ? null : searchBox.getValue();
        super.init();

        if (boundContainerId != menu.containerId) {
            boundContainerId = menu.containerId;
            ClientKleinBottleView.clear();
            searchInitialized = false;
            seenSnapshot = null;
        }

        searchBox = new KleinSearchBox(font,
                leftPos + KleinTerminalLayout.SEARCH_X,
                topPos + KleinTerminalLayout.SEARCH_Y,
                KleinTerminalLayout.SEARCH_W,
                KleinTerminalLayout.SEARCH_H,
                Component.translatable("gui.zuoyanmod.klein.search_hint"));
        if (previousSearch != null) {
            searchBox.setValue(previousSearch);
        }
        searchBox.setResponder(this::onSearchChanged);
        addRenderableWidget(searchBox);

        // 铁砧改名框：不描边（凹槽烤在底图里）、最多 50 字（和原版铁砧一致）
        anvilName = new EditBox(font,
                leftPos + KleinTerminalLayout.ANVIL_NAME_X + 4,
                topPos + KleinTerminalLayout.ANVIL_NAME_Y + 2,
                KleinTerminalLayout.ANVIL_NAME_W - 8,
                KleinTerminalLayout.ANVIL_NAME_H - 4,
                Component.translatable("gui.zuoyanmod.klein.anvil_name"));
        anvilName.setBordered(false);
        anvilName.setMaxLength(net.minecraft.world.inventory.AnvilMenu.MAX_NAME_LENGTH);
        anvilName.setTextColor(KleinTheme.TEXT);
        anvilName.setTextColorUneditable(KleinTheme.TEXT_DIM);
        anvilName.setTextShadow(false);
        anvilName.setCanLoseFocus(true);
        anvilName.setHint(Component.translatable("gui.zuoyanmod.klein.anvil_name_hint")
                .copy().withStyle(ChatFormatting.GRAY));
        anvilName.setResponder(text -> PacketHandler.sendKleinAnvilName(text));
        addRenderableWidget(anvilName);

        scrollbar = new KleinScrollbar(
                leftPos + KleinTerminalLayout.SCROLLBAR_X + 1,
                topPos + KleinTerminalLayout.SCROLLBAR_Y + 1,
                KleinTerminalLayout.SCROLLBAR_W - 2,
                KleinTerminalLayout.SCROLLBAR_H - 2);
        scrollbar.setListener(this::onScrollbarMoved);

        addSideButtons();

        KleinBottleSyncPacket snapshot = ClientKleinBottleView.latest();
        scrollbar.setRange(VISIBLE_ROWS, snapshotRows(snapshot), VISIBLE_ROWS);
        scrollbar.setCurrentRow(snapshot == null ? 0 : snapshot.scrollRow());
    }

    /**
     * 四个功能按钮，竖排在存储网格左边那一列。
     *
     * <p>这里是 v6 的关键修复：v5 之前这四个按钮是 {@code Screen} 手画 + 手写命中判定，
     * **点了全没反应**。现在换成 {@link KleinSideButton}（{@code AbstractButton} 子类），
     * 命中/hover/按键触发/按下音效全部交给控件框架——RS2 的
     * {@code AbstractSideButtonWidget} 就是这个结构，这里照搬。
     *
     * <p>排序方式/方向那种"会随状态变"的图标，直接在图标回调里读最新快照来画，
     * 不用监听同步包。
     */
    private void addSideButtons() {
        KleinSideButton.IconRenderer toTop = (g, x, y, s, hovered, time) ->
                KleinTheme.iconToTop(g, x, y, s, hovered ? KleinTheme.TEXT : KleinTheme.TEXT_DIM,
                        hovered ? KleinTheme.CYAN : KleinTheme.CYAN_DEEP);
        KleinSideButton.IconRenderer sortMode = (g, x, y, s, hovered, time) ->
                KleinTheme.iconSortMode(g, x, y, s, hovered ? KleinTheme.TEXT : KleinTheme.TEXT_DIM);
        KleinSideButton.IconRenderer sortDir = (g, x, y, s, hovered, time) ->
                KleinTheme.iconSortDir(g, x, y, s, hovered ? KleinTheme.CYAN : KleinTheme.CYAN_DEEP,
                        sortDescending(ClientKleinBottleView.latest()));
        KleinSideButton.IconRenderer clear = (g, x, y, s, hovered, time) ->
                KleinTheme.iconClear(g, x, y, s, hovered ? KleinTheme.TEXT : KleinTheme.TEXT_DIM);

        sideButtons = new KleinSideButton[KleinTerminalLayout.SIDE_BUTTON_COUNT];
        for (int i = 0; i < sideButtons.length; i++) {
            final int index = i;
            KleinSideButton button = new KleinSideButton(
                    leftPos + KleinTerminalLayout.SIDE_BUTTON_X,
                    topPos + KleinTerminalLayout.sideButtonY(index),
                    KleinTerminalLayout.SIDE_BUTTON_SIZE,
                    buttonTooltip(index),
                    switch (index) {
                        case KleinTerminalLayout.BUTTON_HOME -> toTop;
                        case KleinTerminalLayout.BUTTON_SORT_MODE -> sortMode;
                        case KleinTerminalLayout.BUTTON_SORT_DIR -> sortDir;
                        default -> clear;
                    },
                    () -> clickToolbar(index));
            sideButtons[index] = button;
            addRenderableWidget(button);
        }
    }

    /** 把侧边按钮的按下翻译成服务端的 {@code clickMenuButton}（清空搜索在客户端本地做） */
    private void clickToolbar(int index) {
        switch (index) {
            case KleinTerminalLayout.BUTTON_HOME -> button(KleinBottleMenu.BUTTON_HOME);
            case KleinTerminalLayout.BUTTON_SORT_MODE -> button(KleinBottleMenu.BUTTON_SORT_MODE);
            case KleinTerminalLayout.BUTTON_SORT_DIR -> button(KleinBottleMenu.BUTTON_SORT_DIR);
            case KleinTerminalLayout.BUTTON_CLEAR -> {
                if (searchBox != null) {
                    searchBox.setValue("");
                }
            }
            default -> {
                // 不该发生
            }
        }
    }

    private void button(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            // 这条是**主动**要服务端改状态（回到顶部 / 翻页 / 排序），不是"我滚到了某处"等回声。
            // 不清掉等待窗口的话，紧接着回来的快照会被当成过期回声丢弃，
            // 「回到顶部」最多要等 1 秒才生效——看起来就像按钮没反应。
            pendingScrollRow = -1;
            scrollEchoTicks = 0;
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    // ===== 视图状态（客户端只读服务端那份快照）=====

    private static int snapshotRows(KleinBottleSyncPacket snapshot) {
        if (snapshot == null) {
            return VISIBLE_ROWS;
        }
        int rows = (snapshot.viewSize() + FourDimensionalSpace.COLUMNS - 1) / FourDimensionalSpace.COLUMNS;
        return Math.max(VISIBLE_ROWS, rows);
    }

    private static int sortOrdinal(KleinBottleSyncPacket snapshot) {
        return snapshot == null ? FourDimensionalSpace.SortMode.RECENT.ordinal() : snapshot.sortOrdinal();
    }

    private static boolean sortDescending(KleinBottleSyncPacket snapshot) {
        return snapshot != null && snapshot.descending();
    }

    private String sortModeName(int ordinal) {
        FourDimensionalSpace.SortMode mode = FourDimensionalSpace.SortMode.byOrdinal(ordinal);
        return Component.translatable("gui.zuoyanmod.klein.sort." + mode.name().toLowerCase()).getString();
    }

    // ===== 交互 =====

    private void onSearchChanged(String text) {
        searchDebounce = SEARCH_DEBOUNCE_TICKS;
        // 换了搜索词就回到顶部：不然过滤之后停在"已经不存在的第 7 行"很困惑
        scrollbar.setCurrentRow(0);
    }

    private void onScrollbarMoved(int row) {
        sendViewRequest(row);
    }

    private void sendViewRequest(int row) {
        pendingScrollRow = row;
        scrollEchoTicks = SCROLL_ECHO_TICKS;
        PacketHandler.sendKleinBottleView(searchBox == null ? "" : searchBox.getValue(), row);
    }

    private void flushSearch() {
        sendViewRequest(scrollbar.getCurrentRow());
    }

    private boolean isOverGrid(double mouseX, double mouseY) {
        return mouseX >= leftPos + KleinTerminalLayout.GRID_X
                && mouseX < leftPos + KleinTerminalLayout.GRID_X + KleinTerminalLayout.GRID_W
                && mouseY >= topPos + KleinTerminalLayout.GRID_Y
                && mouseY < topPos + KleinTerminalLayout.GRID_Y + KleinTerminalLayout.GRID_H;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // 滚动条不是控件（原因见 KleinScrollbar 类注释），由这里转发；
        // 功能按钮现在是真控件，走 super 的子控件分发，不再在这里手写命中判定。
        if (event.button() == 0 && scrollbar != null && scrollbar.mouseClicked(event.x(), event.y(), 0)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (scrollbar != null && scrollbar.mouseReleased()) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (scrollbar != null && scrollbar.isDragging()) {
            scrollbar.mouseDragged(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    /**
     * 拖动滚动条的第二条路。
     *
     * <p>26.3 里 {@code ContainerEventHandler#mouseDragged} 只转发**右键**拖动，
     * 左键拖动能不能到我们这层取决于 {@code MouseHandler} 的按压状态。所以拖滚动条
     * 同时挂在 {@code mouseDragged} 和 {@code mouseMoved} 两条路上——鼠标一动这里
     * 必到，哪条通走哪条。
     */
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (scrollbar != null && scrollbar.isDragging()) {
            scrollbar.mouseDragged(mouseY);
            return;
        }
        super.mouseMoved(mouseX, mouseY);
    }

    /**
     * 滚轮 = 滚动视图（一行；Ctrl 一屏）。
     *
     * <p>滚轮打在滚动条上和打在网格上要分开走：滚动条那个分支自己会调 {@code scroll}，
     * 两个都调就变成一次滚轮跳两行。
     * 停在搜索框上时不抢滚轮——不然输入法切候选词会顺手把整页翻走。
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (searchBox != null && searchBox.isMouseOver(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (scrollbar != null && scrollbar.isOver(mouseX, mouseY)) {
            scrollbar.mouseScrolled(mouseX, mouseY, scrollY);
            return true;
        }
        if (isOverGrid(mouseX, mouseY)) {
            int step = minecraft != null && minecraft.hasControlDown() ? VISIBLE_ROWS : 1;
            scrollbar.scroll(scrollY > 0 ? -1 : 1, step);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchBox != null && searchBox.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }
    @Override
    public boolean keyPressed(KeyEvent event) {
        // 铁砧改名框独占键盘：否则打字会顺手触发 F 聚焦搜索框、数字键换物品
        if (anvilName != null && anvilName.isFocused()) {
            if (event.isEscape()) {
                anvilName.setFocused(false);
                return true;
            }
            anvilName.keyPressed(event);
            return true;
        }
        // 搜索框拿着焦点时独占键盘：否则打字会顺手触发 E 关界面、数字键换物品
        if (searchBox != null && searchBox.isFocused()) {
            if (event.isEscape() || event.isConfirmation()) {
                searchBox.setFocused(false);
                return true;
            }
            searchBox.keyPressed(event);
            return true;
        }

        // 26.3 的 KeyEvent#key() 是 InputConstants 那套 SDL scancode（不是 GLFW key），
        // 所以这里统一用 InputConstants.KEY_* 比较；Esc/回车/方向键另有语义化判断可用。
        switch (event.key()) {
            case InputConstants.KEY_PAGEUP -> {
                button(KleinBottleMenu.BUTTON_SCROLL_UP_PAGE);
                return true;
            }
            case InputConstants.KEY_PAGEDOWN -> {
                button(KleinBottleMenu.BUTTON_SCROLL_DOWN_PAGE);
                return true;
            }
            case InputConstants.KEY_HOME -> {
                button(KleinBottleMenu.BUTTON_SCROLL_HOME);
                return true;
            }
            case InputConstants.KEY_END -> {
                button(KleinBottleMenu.BUTTON_SCROLL_END);
                return true;
            }
            case InputConstants.KEY_F -> {
                if (!event.hasShiftDown()) {
                    if (searchBox != null) {
                        searchBox.setFocused(true);
                    }
                    return true;
                }
            }
            default -> {
                // 交给原版
            }
        }
        return super.keyPressed(event);
    }

    // ===== 每 tick =====

    @Override
    protected void containerTick() {
        super.containerTick();

        // 拖动滚动条的**第三条路**：每 tick 直接读鼠标当前坐标推一次。
        // 前两条（mouseDragged / mouseMoved）走的是事件分发，哪条断了这条兜底；
        // 20Hz 的分辨率对"拖滚动条"完全够用，而且不依赖任何事件能不能到。
        if (scrollbar != null && scrollbar.isDragging() && minecraft != null) {
            Window window = minecraft.getWindow();
            if (window.getScreenWidth() > 0) {
                scrollbar.mouseDragged(minecraft.mouseHandler.getScaledYPos(window));
            }
        }

        if (searchDebounce > 0 && --searchDebounce == 0) {
            flushSearch();
        }
        if (scrollEchoTicks > 0) {
            scrollEchoTicks--;
        }

        KleinBottleSyncPacket snapshot = ClientKleinBottleView.latest();
        if (snapshot != null) {
            if (!searchInitialized) {
                searchInitialized = true;
                // 服务端记着上次的搜索词（AE2 也会记）；只在首次快照、且玩家还没输入时回填
                if (searchBox != null && searchBox.getValue().isEmpty() && !snapshot.search().isEmpty()) {
                    searchBox.setValue(snapshot.search());
                }
            }
            if (snapshot != seenSnapshot) {
                seenSnapshot = snapshot;
                boolean echo = pendingScrollRow >= 0 && snapshot.scrollRow() == pendingScrollRow;
                if (echo || scrollEchoTicks <= 0) {
                    // 服务端已经认定（或早就过了等待窗口）：以它为准
                    pendingScrollRow = -1;
                    scrollEchoTicks = 0;
                    scrollbar.setCurrentRow(snapshot.scrollRow());
                }
            }
            scrollbar.setRange(VISIBLE_ROWS, snapshotRows(snapshot), VISIBLE_ROWS);
        }
    }

    // ===== 绘制：背景 =====

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        float time = KleinTheme.now();

        graphics.blit(RenderPipelines.GUI_TEXTURED, KleinTheme.BACKGROUND, x, y, 0.0F, 0.0F,
                IMAGE_W, IMAGE_H, IMAGE_W, IMAGE_H);

        // 外框呼吸：一条极淡的青边，让整个终端看起来在"通电"
        float breathe = 0.16F + 0.10F * (float) Math.sin(time * 1.6F);
        KleinTheme.outline(graphics, x - 1, y - 1, IMAGE_W + 2, IMAGE_H + 2,
                KleinTheme.withAlpha(KleinTheme.CYAN, breathe));

        // 搜索框（凹槽 + 放大镜）——功能按钮已改成真控件自己画，这里只剩搜索框
        KleinTheme.searchFrame(graphics, x + KleinTerminalLayout.SEARCH_X, y + KleinTerminalLayout.SEARCH_Y,
                KleinTerminalLayout.SEARCH_W, KleinTerminalLayout.SEARCH_H,
                searchBox != null && searchBox.isFocused());
        drawSidebar(graphics, x, y, time);
        // 物品栏右侧那一列：一条会往下淌光点的 ∞。
        // 它占的正是早先"维度刻度"的位置——那条因为看起来像第二根滚动条被砍了，
        // ∞ 竖着放刚好填满这一列，也不会和右边那根真滚动条抢语义。
        KleinTheme.infinityFlow(graphics,
                x + KleinTerminalLayout.INFINITY_X, y + KleinTerminalLayout.INFINITY_Y,
                KleinTerminalLayout.INFINITY_W, KleinTerminalLayout.INFINITY_H, time);
    }

    /**
     * 右栏：合成箭头、熔炉的<b>炉火与进度条</b>、铁砧的<b>代价与改名框</b>。
     * 卡片底、槽位凹槽、改名框凹槽都烤在底图里；带状态的件（火、进度、代价颜色）运行时画。
     */
    private void drawSidebar(GuiGraphicsExtractor graphics, int x, int y, float time) {
        // 合成箭头
        int ax = x + KleinTerminalLayout.CRAFT_ARROW_X;
        int ay = y + KleinTerminalLayout.CRAFT_ARROW_Y;
        int aw = KleinTerminalLayout.CRAFT_ARROW_W;
        int ah = KleinTerminalLayout.CRAFT_ARROW_H;
        int midY = ay + ah / 2;
        for (int i = 0; i < aw - 4; i++) {
            graphics.fill(ax + i, midY, ax + i + 1, midY + 1, KleinTheme.withAlpha(KleinTheme.CYAN, 0.85F));
        }
        for (int i = 0; i < 4; i++) {
            graphics.fill(ax + aw - 5 + i, midY - 3 + i, ax + aw - 4 + i, midY - 2 + i,
                    KleinTheme.withAlpha(KleinTheme.CYAN, 0.85F));
            graphics.fill(ax + aw - 5 + i, midY + 2 - i, ax + aw - 4 + i, midY + 3 - i,
                    KleinTheme.withAlpha(KleinTheme.CYAN, 0.85F));
        }

        // 炉火：高度 = 剩余燃料（没火就是一空炉膛）
        KleinTheme.flame(graphics,
                x + KleinTerminalLayout.FLAME_X, y + KleinTerminalLayout.FLAME_Y,
                KleinTerminalLayout.FLAME_W, KleinTerminalLayout.FLAME_H,
                menu.furnaceLit() ? menu.burnProgress() : 0.0F, time);
        // 熔炼进度条
        KleinTheme.progressBar(graphics,
                x + KleinTerminalLayout.FURNACE_ARROW_X, y + KleinTerminalLayout.FURNACE_ARROW_Y,
                KleinTerminalLayout.FURNACE_ARROW_W, KleinTerminalLayout.FURNACE_ARROW_H,
                menu.cookProgress(), time);

        // 铁砧：改名框的框 + 「+」「→」连接符（代价文字在 extractLabels 里，随颜色变）
        KleinTheme.nameFrame(graphics,
                x + KleinTerminalLayout.ANVIL_NAME_X, y + KleinTerminalLayout.ANVIL_NAME_Y,
                KleinTerminalLayout.ANVIL_NAME_W, KleinTerminalLayout.ANVIL_NAME_H,
                anvilName != null && anvilName.isFocused());
        int px = x + (KleinTerminalLayout.ANVIL_BASE_X + KleinTerminalLayout.ANVIL_MATERIAL_X) / 2;
        int py = y + KleinTerminalLayout.ANVIL_BASE_Y + 6;
        graphics.fill(px - 3, py, px + 4, py + 1, KleinTheme.withAlpha(KleinTheme.TEXT_DIM, 0.9F));
        graphics.fill(px, py - 3, px + 1, py + 4, KleinTheme.withAlpha(KleinTheme.TEXT_DIM, 0.9F));
        int vx = x + (KleinTerminalLayout.ANVIL_MATERIAL_X + KleinTerminalLayout.ANVIL_RESULT_X) / 2;
        int vy = y + KleinTerminalLayout.ANVIL_BASE_Y + 8;
        for (int i = 0; i < 10; i++) {
            graphics.fill(vx - 4 + i, vy, vx - 3 + i, vy + 1, KleinTheme.withAlpha(KleinTheme.TEXT_DIM, 0.9F));
        }
        for (int i = 0; i < 3; i++) {
            graphics.fill(vx + 5 - i, vy - 3 + i, vx + 6 - i, vy - 2 + i, KleinTheme.withAlpha(KleinTheme.TEXT_DIM, 0.9F));
            graphics.fill(vx + 5 - i, vy + 2 - i, vx + 6 - i, vy + 3 - i, KleinTheme.withAlpha(KleinTheme.TEXT_DIM, 0.9F));
        }
    }

    /** 铁砧的代价：不够就红、够就暗金；没有产物就不显示 */
    private void drawAnvilCost(GuiGraphicsExtractor graphics) {
        int cost = menu.anvilCost();
        if (cost <= 0) {
            return;
        }
        boolean affordable = minecraft != null && minecraft.player != null
                && (minecraft.player.hasInfiniteMaterials() || minecraft.player.experienceLevel >= cost);
        String text = Component.translatable("gui.zuoyanmod.klein.anvil_cost", cost).getString();
        graphics.text(font, text, KleinTerminalLayout.ANVIL_COST_X, KleinTerminalLayout.ANVIL_COST_Y,
                affordable ? KleinTheme.TEXT_DIM : KleinTheme.TEXT_WARN, false);
    }

    // ===== 绘制：槽位与滚动条 =====

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        float time = KleinTheme.now();

        // 斜向扫光：铺在网格带上，alpha 极低，只为让整片槽位"活"起来
        KleinTheme.sweep(graphics,
                leftPos + KleinTerminalLayout.GRID_X, topPos + KleinTerminalLayout.GRID_Y,
                KleinTerminalLayout.GRID_W, KleinTerminalLayout.GRID_H,
                time, KleinTheme.CYAN);

        if (scrollbar != null) {
            scrollbar.render(graphics, time, scrollbar.isOver(mouseX, mouseY));
        }
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        if (slot.index < STORAGE_COUNT) {
            drawStorageSlot(graphics, slot);
            return;
        }
        super.extractSlot(graphics, slot, mouseX, mouseY);
    }

    /**
     * 存储槽自己画，**不调 super**。
     *
     * <p>原版 {@code renderSlotContents} 会调 {@code itemDecorations} 画槽位自带的堆叠数字
     * （那个数字是夹到 64 的展示栈，不是真实总量），两个数字会重叠，
     * 所以这里自己画图标、自己画总量——AE2 对 {@code RepoSlot} 也是这么处理的。
     */
    private void drawStorageSlot(GuiGraphicsExtractor graphics, Slot slot) {
        ItemStack stack = slot.getItem();
        if (!stack.isEmpty()) {
            int seed = slot.x + slot.y * imageWidth;
            graphics.item(stack, slot.x, slot.y, seed);

            long total = ClientKleinBottleView.windowTotal(slot.index);
            if (total <= 0) {
                total = stack.getCount(); // 快照还没到：退化成槽位自带数量，别显示成 0
            }
            String text = AmountFormat.compact(total);
            int color = KleinTheme.amountColor(total);

            // 把图标和叠字切成两个绘制批次，保证叠字一定压在图标之上
            graphics.nextStratum();
            if (total >= 1_000L) {
                KleinAmountRenderer.drawGlow(graphics, font, slot.x, slot.y, text, color, KleinTheme.now());
            }
            KleinAmountRenderer.draw(graphics, font, slot.x, slot.y, text, color);
        }

        // 悬停：在凹槽上补一圈青色括号，比原版高亮更贴合"超立方"的观感
        if (hoveredSlot == slot) {
            KleinTheme.cornerBrackets(graphics, slot.x, slot.y, KleinTheme.CYAN);
            if (menu.getCarried().isEmpty()) {
                graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16,
                        KleinTheme.withAlpha(KleinTheme.CYAN, 0.10F));
            }
        }
    }

    // ===== 绘制：文字 =====

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 不调 super：原版标签色 0xFF404040 在深紫底上根本看不见
        KleinBottleSyncPacket snapshot = ClientKleinBottleView.latest();

        graphics.text(font, title, KleinTerminalLayout.TITLE_X, KleinTerminalLayout.TITLE_Y, KleinTheme.TEXT, false);
        drawStats(graphics, snapshot);

        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, KleinTheme.TEXT_DIM, false);

        // 右栏三个小标题：合成 / 熔炼 / 铁砧
        int labelX = KleinTerminalLayout.SIDEBAR_X + 6;
        KleinTheme.iconCrafting(graphics, labelX, KleinTerminalLayout.CRAFT_LABEL_Y - 1, 10,
                KleinTheme.BORDER_BRIGHT);
        graphics.text(font, Component.translatable("gui.zuoyanmod.klein.craft"),
                labelX + 14, KleinTerminalLayout.CRAFT_LABEL_Y, KleinTheme.TEXT, false);

        KleinTheme.iconFurnace(graphics, labelX, KleinTerminalLayout.FURNACE_LABEL_Y - 1, 10,
                KleinTheme.BORDER_BRIGHT, KleinTheme.GOLD);
        graphics.text(font, Component.translatable("gui.zuoyanmod.klein.smelt"),
                labelX + 14, KleinTerminalLayout.FURNACE_LABEL_Y, KleinTheme.TEXT, false);

        KleinTheme.iconAnvil(graphics, labelX, KleinTerminalLayout.ANVIL_LABEL_Y - 1, 10,
                KleinTheme.BORDER_BRIGHT);
        graphics.text(font, Component.translatable("gui.zuoyanmod.klein.anvil"),
                labelX + 14, KleinTerminalLayout.ANVIL_LABEL_Y, KleinTheme.TEXT, false);
        drawAnvilCost(graphics);

        if (snapshot != null && snapshot.viewSize() == 0) {
            String hint = snapshot.search().isEmpty()
                    ? Component.translatable("gui.zuoyanmod.klein.empty").getString()
                    : Component.translatable("gui.zuoyanmod.klein.no_match").getString();
            int cx = KleinTerminalLayout.GRID_X + KleinTerminalLayout.GRID_W / 2;
            int cy = KleinTerminalLayout.GRID_Y + KleinTerminalLayout.GRID_H / 2 - 12;
            graphics.centeredText(font, hint, cx, cy, KleinTheme.TEXT_FAINT);
            if (!snapshot.search().isEmpty()) {
                String tip = Component.translatable("gui.zuoyanmod.klein.no_match_tip").getString();
                graphics.centeredText(font, tip, cx, cy + 12, KleinTheme.TEXT_FAINT);
            }
        }
    }

    private void drawStats(GuiGraphicsExtractor graphics, KleinBottleSyncPacket snapshot) {
        if (snapshot == null) {
            return;
        }
        String kinds = AmountFormat.grouped(snapshot.viewSize());
        String total = AmountFormat.grouped(snapshot.totalItems());
        String text = Component.translatable("gui.zuoyanmod.klein.stats", kinds, total).getString();
        int color = snapshot.viewSize() == 0 ? KleinTheme.TEXT_FAINT : KleinTheme.GOLD;
        graphics.text(font, text, KleinTerminalLayout.STATS_RIGHT - font.width(text),
                KleinTerminalLayout.TITLE_Y, color, false);
    }

    // ===== 提示 =====

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 侧边按钮的提示：排序方式/方向会随状态变，所以每帧重读快照
        if (sideButtons != null) {
            for (int i = 0; i < sideButtons.length; i++) {
                if (sideButtons[i] != null && sideButtons[i].isHovered()) {
                    graphics.setTooltipForNextFrame(font, buttonTooltip(i), mouseX, mouseY);
                    return;
                }
            }
        }
        if (scrollbar != null && scrollbar.isOver(mouseX, mouseY)) {
            int max = Math.max(0, snapshotRows(ClientKleinBottleView.latest()) - VISIBLE_ROWS);
            int row = ClientKleinBottleView.latest() == null ? 0 : ClientKleinBottleView.latest().scrollRow();
            graphics.setTooltipForNextFrame(font,
                    Component.translatable("gui.zuoyanmod.klein.scrollbar_hint", row, max), mouseX, mouseY);
            return;
        }
        if (hoveredSlot != null && hoveredSlot.index < STORAGE_COUNT && hoveredSlot.hasItem()) {
            List<Component> lines = getTooltipFromContainerItem(hoveredSlot.getItem());
            long total = ClientKleinBottleView.windowTotal(hoveredSlot.index);
            if (total <= 0) {
                total = hoveredSlot.getItem().getCount();
            }
            lines.add(Component.translatable("gui.zuoyanmod.klein.tooltip_total",
                    AmountFormat.grouped(total)).withColor(KleinTheme.GOLD & 0xFFFFFF));
            ItemStack stack = hoveredSlot.getItem();
            graphics.setTooltipForNextFrame(font, lines, stack.getTooltipImage(), stack, mouseX, mouseY,
                    stack.get(net.minecraft.core.component.DataComponents.TOOLTIP_STYLE), true);
            return;
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }

    private Component buttonTooltip(int index) {
        KleinBottleSyncPacket snapshot = ClientKleinBottleView.latest();
        return switch (index) {
            case KleinTerminalLayout.BUTTON_HOME ->
                    Component.translatable("gui.zuoyanmod.klein.button.home");
            case KleinTerminalLayout.BUTTON_SORT_MODE -> Component.translatable(
                    "gui.zuoyanmod.klein.button.sort_mode", sortModeName(sortOrdinal(snapshot)));
            case KleinTerminalLayout.BUTTON_SORT_DIR -> Component.translatable(
                    "gui.zuoyanmod.klein.button.sort_dir",
                    Component.translatable(sortDescending(snapshot)
                            ? "gui.zuoyanmod.klein.sort.desc_full"
                            : "gui.zuoyanmod.klein.sort.asc_full"));
            case KleinTerminalLayout.BUTTON_CLEAR ->
                    Component.translatable("gui.zuoyanmod.klein.button.clear");
            default -> Component.empty();
        };
    }
}
