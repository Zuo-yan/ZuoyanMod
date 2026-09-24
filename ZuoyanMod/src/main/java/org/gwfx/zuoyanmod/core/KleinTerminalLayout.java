package org.gwfx.zuoyanmod.core;

/**
 * 克莱因瓶终端界面的**布局常量**。
 *
 * <p>属于 {@code core} 纯逻辑层：不依赖任何 Minecraft / NeoForge API，
 * 跨版本迁移时无需改动。终端网格的形状（{@link #COLUMNS} × {@link #ROWS}）
 * 也以这里为**单一事实来源**，数据模型侧的 {@code FourDimensionalSpace}
 * 通过别名引用这里的常量，保证两侧永不错开。
 *
 * <p>这里的每一个数字都必须和三处保持一致，改一处必改另两处：
 * <ul>
 *   <li>{@code KleinBottleMenu} 里的槽位坐标（原版靠槽位序号同步，坐标只在客户端用于绘制，
 *       但两侧的<b>槽位数量</b>必须完全一样，否则原版同步会错位）；</li>
 *   <li>{@code client/KleinBottleScreen} 与 {@code client/klein/*} 的运行时绘制；</li>
 *   <li>{@code tools/gen_klein_terminal_gui.py} 里的 GUI 底图绘制。</li>
 * </ul>
 *
 * <h2>版面（v6）</h2>
 * <pre>
 *   ┌─┐ ┌────────────────────────────────────────┬──────────────────┐
 *   │↑│ │ 四维空间              57 种 · 3,587 个 │  合成            │
 *   │序│ ┌ ⌕ 搜索物品 · @模组 · -排除 ┐          │ ┌──┬──┬──┐       │
 *   │↓│ └───────────────────────────┘          │ │  │  │  │ →  ┌┐ │
 *   │×│ ┌──────────────────────────────┐ ┌▮┐   │ └──┴──┴──┘       │
 *   │ │ │      9 × 6 存储视图窗口       │ │ │   │ ─────────        │
 *   │ │ └──────────────────────────────┘ └▯┘   │  熔炼             │
 *   │ │ ────────────────────────────           │ ┌┐      ┌┐       │
 *   │ │ 物品栏                        ┌∞┐      │ ││ 🔥   │ │       │
 *   │ │ [ 背包 3 行 + 快捷栏 ]        │ │      │ └┘ ──→  └┘       │
 *   └─┘                               └∞┘      │    （涡环）      │
 *   ↑ 四个功能按钮竖排在最左列                    └──────────────────┘
 * </pre>
 *
 * <h2>为什么按钮竖排在最左列（v6）</h2>
 * v4/v5 把四个按钮横排在搜索框下面，占掉一整行（约 18px 高）。竖排之后那一行整个
 * 还给了存储网格，整张图**矮了 18px**；而且按钮和"它们作用于谁"的关系更清楚——
 * 就贴在存储网格左边。这也是 RS2 的 {@code AbstractSideButtonWidget} 的排法
 * （只是 RS2 把它们摆在面板外侧，这里摆在面板内侧、不占用游戏画面）。
 *
 * <p>物品栏右侧那一竖条是 ∞（见 {@code KleinTheme#infinityFlow}）。⚠️ 那里早先放过
 * "维度刻度"装饰，因为紧贴物品栏、看起来像第二根滚动条被砍了——**别再往那一列
 * 放"轨道状"的东西**，按钮列也不行（所以按钮在最左、不与 ∞ 同列）。
 *
 * <p>行数固定 6 行（54 格）是刻意的：客户端和服务端各自构造 {@code AbstractContainerMenu}
 * 时槽位数量必须一致，而客户端的窗口尺寸在构造菜单时不可知，所以行数不能做成"自适应"。
 * AE2 的 ME 终端同样是固定的 9×6。
 */
public final class KleinTerminalLayout {

    /** 终端网格的列数（单一事实来源：{@code FourDimensionalSpace.COLUMNS} 引用这里） */
    public static final int COLUMNS = 9;
    /** 终端网格的行数（单一事实来源：{@code FourDimensionalSpace.DEFAULT_ROWS} 引用这里） */
    public static final int ROWS = 6;
    public static final int CELL = 18;
    public static final int WINDOW_SLOTS = COLUMNS * ROWS;

    // ===== 整体 =====
    public static final int IMAGE_W = 318;
    public static final int IMAGE_H;

    // ===== 侧边按钮列（最左，竖排）=====
    public static final int SIDE_BUTTON_X = 8;
    public static final int SIDE_BUTTON_Y = 34;
    public static final int SIDE_BUTTON_SIZE = 16;
    public static final int SIDE_BUTTON_GAP = 2;
    public static final int SIDE_BUTTON_COUNT = 4;

    /** 第 index 个侧边按钮的左上角 y（x 恒为 {@link #SIDE_BUTTON_X}） */
    public static int sideButtonY(int index) {
        return SIDE_BUTTON_Y + index * (SIDE_BUTTON_SIZE + SIDE_BUTTON_GAP);
    }

    // ===== 标题行（一行，左标题右统计）=====
    public static final int TITLE_X = 30;
    public static final int TITLE_Y = 4;
    /** 统计文字的右边界：停在左列的右边，不许伸进右栏 */
    public static final int STATS_RIGHT = 208;

    // ===== 搜索行 =====
    public static final int SEARCH_X = 30;
    public static final int SEARCH_Y = 17;
    public static final int SEARCH_W = 140;
    public static final int SEARCH_H = 13;
    /** EditBox 只覆盖文字带；外框与放大镜由底图 / {@code KleinTheme} 绘制 */
    public static final int SEARCH_TEXT_INSET_X = 20;
    public static final int SEARCH_TEXT_Y = SEARCH_Y + 2;
    public static final int SEARCH_TEXT_W = SEARCH_W - SEARCH_TEXT_INSET_X - 6;

    // ===== 存储网格 =====
    public static final int GRID_X = 30;
    public static final int GRID_Y = 34;
    public static final int GRID_W = COLUMNS * CELL;
    public static final int GRID_H = ROWS * CELL;
    public static final int GRID_BOTTOM = GRID_Y + GRID_H;

    // ===== 滚动条（界面右侧唯一一根能拖的条）=====
    /** 与网格带上沿对齐（网格槽框从 slotY-1 起，所以这里也 -1） */
    public static final int SCROLLBAR_X = 196;
    public static final int SCROLLBAR_Y = GRID_Y - 1;
    public static final int SCROLLBAR_W = 14;
    public static final int SCROLLBAR_H = GRID_H;

    /** 分隔线只横跨左列，不许跑到无限符号那列上 */
    public static final int SEPARATOR_X = GRID_X;
    public static final int SEPARATOR_W = GRID_W;

    // ===== 物品栏右侧的无限符号（纯动效，底图不画）=====
    public static final int INFINITY_X = 196;
    public static final int INFINITY_Y = GRID_BOTTOM + 4;
    public static final int INFINITY_W = 14;
    public static final int INFINITY_H;

    /**
     * 左列四个功能按钮**下方**那条空的竖位——也放一条 ∞。
     *
     * <p>按钮只占 y 34..104，下面到物品栏还有一大截空着；放上同款动效之后，
     * 左列就变成"按钮 + 流动的无限"一根完整的竖栏，和物品栏右侧那条
     * （{@link #INFINITY_X}）左右呼应。相位在屏幕那边故意错开，两条不会齐刷刷地一起流。</p>
     */
    public static final int LEFT_FLOW_X = 9;
    public static final int LEFT_FLOW_Y = 112;
    public static final int LEFT_FLOW_W = 14;
    public static final int LEFT_FLOW_H;

    // ===== 右栏 =====
    public static final int SIDEBAR_X = 208;
    public static final int SIDEBAR_W = 102;

    /**
     * 右栏三张"卡片"：合成 / 熔炼 / 铁砧。
     *
     * <p>v7 之前三块功能挤在一整块面板里，只有分隔线，看不出哪块是哪个。
     * 现在每个功能区一张**独立卡片**：各自带一点色调、边框和顶部亮边，
     * 标题压在卡片外面上头——层次感就是这么来的（三个 accent 色见
     * {@code KleinTheme} 的 CARD_* 常量）。
     */
    public static final int CARD_X = 210;
    public static final int CARD_W = 98;

    // ---- 上：合成 ----
    public static final int CRAFT_LABEL_Y = 4;
    public static final int CRAFT_CARD_Y = 13;
    public static final int CRAFT_CARD_H = 62;
    /** 3×3 合成网格的**槽位坐标**基准（槽框从 -1 起画） */
    public static final int CRAFT_GRID_X = 212;
    public static final int CRAFT_GRID_Y = 16;
    public static final int CRAFT_ARROW_X = 268;
    public static final int CRAFT_ARROW_Y = 39;
    public static final int CRAFT_ARROW_W = 14;
    public static final int CRAFT_ARROW_H = 9;
    public static final int CRAFT_RESULT_X = 286;
    public static final int CRAFT_RESULT_Y = 35;

    // ---- 中：熔炉 ----
    public static final int FURNACE_LABEL_Y = 79;
    public static final int FURNACE_CARD_Y = 88;
    public static final int FURNACE_CARD_H = 62;
    public static final int FURNACE_INPUT_X = 212;
    public static final int FURNACE_INPUT_Y = 92;
    /** 火焰：夹在输入格与燃料格之间，高度按剩余燃料从下往上烧 */
    public static final int FLAME_X = 213;
    public static final int FLAME_Y = 111;
    public static final int FLAME_W = 14;
    public static final int FLAME_H = 14;
    public static final int FURNACE_FUEL_X = 212;
    public static final int FURNACE_FUEL_Y = 130;
    /** 熔炼进度条：从燃料指向产物格 */
    public static final int FURNACE_ARROW_X = 236;
    public static final int FURNACE_ARROW_Y = 112;
    public static final int FURNACE_ARROW_W = 32;
    public static final int FURNACE_ARROW_H = 9;
    public static final int FURNACE_RESULT_X = 284;
    public static final int FURNACE_RESULT_Y = 108;

    // ---- 下：铁砧 ----
    public static final int ANVIL_LABEL_Y = 154;
    public static final int ANVIL_CARD_Y = 163;
    public static final int ANVIL_CARD_H = 66;
    public static final int ANVIL_BASE_X = 214;
    public static final int ANVIL_BASE_Y = 168;
    public static final int ANVIL_MATERIAL_X = 242;
    public static final int ANVIL_MATERIAL_Y = 168;
    public static final int ANVIL_RESULT_X = 284;
    public static final int ANVIL_RESULT_Y = 168;
    /** 重命名输入框（真 EditBox，底图只烤凹槽） */
    public static final int ANVIL_NAME_X = 214;
    public static final int ANVIL_NAME_Y = 192;
    public static final int ANVIL_NAME_W = 88;
    public static final int ANVIL_NAME_H = 12;
    /** 「需要 X 级经验」那行字 */
    public static final int ANVIL_COST_X = 214;
    public static final int ANVIL_COST_Y = 208;

    // ===== 下半段 =====
    public static final int SEPARATOR_Y = GRID_BOTTOM + 4;
    public static final int INV_LABEL_Y = SEPARATOR_Y + 5;
    public static final int INV_Y = INV_LABEL_Y + 11;
    public static final int HOTBAR_Y = INV_Y + 58;
    /** 底部留白 */
    public static final int BOTTOM_PAD = 6;

    static {
        IMAGE_H = HOTBAR_Y + CELL + BOTTOM_PAD;
        INFINITY_H = IMAGE_H - BOTTOM_PAD - INFINITY_Y;
        LEFT_FLOW_H = IMAGE_H - BOTTOM_PAD - LEFT_FLOW_Y;
    }

    private KleinTerminalLayout() {}

    public static int slotX(int column) {
        return GRID_X + column * CELL;
    }

    public static int slotY(int row) {
        return GRID_Y + row * CELL;
    }

    public static int craftSlotX(int column) {
        return CRAFT_GRID_X + column * CELL;
    }

    public static int craftSlotY(int row) {
        return CRAFT_GRID_Y + row * CELL;
    }

    // ===== 侧边按钮（下标即含义）=====
    /** 回到顶部 */
    public static final int BUTTON_HOME = 0;
    public static final int BUTTON_SORT_MODE = 1;
    public static final int BUTTON_SORT_DIR = 2;
    public static final int BUTTON_CLEAR = 3;
}
