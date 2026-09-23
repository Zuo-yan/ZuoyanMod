package org.gwfx.zuoyanmod.client.klein;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.IntConsumer;

/**
 * 终端的滚动条。
 *
 * <p>参照 AE2 的 {@code Scrollbar}：<b>以"行"为单位</b>而不是以像素为单位——
 * {@code min/max/currentRow} 全是行号，手柄位置由行号换算。
 * 好处是"滚动"和"显示哪几行"天然对齐，永远不会出现滚到半行、槽位对不上格子的情况。
 *
 * <p>交互照抄 RS2 的 {@code ScrollbarWidget}：
 * <ul>
 *   <li>按在轨道任意位置 → <b>直接跳到那里并进入拖动</b>（v6 改的：早先是 AE2 那套
 *       "必须点中手柄才能拖"，手柄只有十几像素，玩家按住轨道拖 = 没反应）；</li>
 *   <li>拖动中手柄跟着鼠标走（见 {@code KleinBottleScreen#mouseMoved}，那里把拖动
 *       同时挂在 {@code mouseDragged} 和 {@code mouseMoved} 两条路上，哪条通走哪条）；</li>
 *   <li>滚轮 → 滚一行（按住 Ctrl 滚一屏）。</li>
 * </ul>
 *
 * <p><b>刻意不继承 {@code AbstractWidget}</b>：原版的拖拽事件只转发给"当前获得焦点的子控件"
 * （见 {@code ContainerEventHandler#mouseDragged}），而终端里搜索框、按钮都在抢焦点，
 * 拖到一半焦点被抢走就会断掉。做成纯辅助类、由 Screen 直接转发鼠标事件，行为最稳。
 */
public final class KleinScrollbar {

    private static final int HANDLE_MIN_H = 14;

    private final int trackX;
    private final int trackY;
    private final int trackW;
    private final int trackH;

    private int visibleRows = 1;
    private int totalRows = 1;
    private int currentRow;
    private int pageRows = 1;

    private boolean dragging;

    private IntConsumer listener;

    public KleinScrollbar(int trackX, int trackY, int trackW, int trackH) {
        this.trackX = trackX;
        this.trackY = trackY;
        this.trackW = trackW;
        this.trackH = trackH;
    }

    public void setListener(IntConsumer listener) {
        this.listener = listener;
    }

    public int getCurrentRow() {
        return currentRow;
    }

    /** 服务端权威值回填。区别是"用户自己拖出来的"，不会反过来再发包 */
    public void setCurrentRow(int row) {
        this.currentRow = clampRow(row);
    }

    public void setRange(int visibleRows, int totalRows, int pageRows) {
        this.visibleRows = Math.max(1, visibleRows);
        this.totalRows = Math.max(1, totalRows);
        this.pageRows = Math.max(1, pageRows);
        this.currentRow = clampRow(this.currentRow);
    }

    /** 可滚动的最大行号；为 0 表示内容一屏就放得下，滚动条置灰 */
    public int maxRow() {
        return Math.max(0, totalRows - visibleRows);
    }

    public boolean isEnabled() {
        return maxRow() > 0;
    }

    private int clampRow(int row) {
        return Math.clamp(row, 0, maxRow());
    }

    private void moveTo(int row) {
        int next = clampRow(row);
        if (next == currentRow) {
            return;
        }
        currentRow = next;
        if (listener != null) {
            listener.accept(currentRow);
        }
    }

    // ===== 几何 =====

    private int handleHeight() {
        if (totalRows <= visibleRows) {
            return trackH;
        }
        long h = (long) trackH * visibleRows / totalRows;
        return (int) Math.max(HANDLE_MIN_H, h);
    }

    private int handleTop() {
        int range = maxRow();
        if (range <= 0) {
            return trackY;
        }
        return trackY + (trackH - handleHeight()) * currentRow / range;
    }

    /**
     * 命中区域比绘制区域大一圈（把凹槽的 1px 边框也算进来）。
     * 拖动这种交互差 1 像素就很挫败，宽一点不亏。
     */
    public boolean isOver(double mouseX, double mouseY) {
        return mouseX >= trackX - 1 && mouseX < trackX + trackW + 1
                && mouseY >= trackY - 1 && mouseY < trackY + trackH + 1;
    }

    // ===== 交互 =====

    /**
     * 按下去：**直接跳到那个位置并进入拖动**，不区分"点在手柄上"还是"点在轨道上"。
     *
     * <p>v6 之前是 AE2 的三件套（点手柄拖 / 点上下翻屏 / 滚轮一行），实测玩家点在轨道上
     * 拖不动——因为必须正好命中那 14px 的手柄，而手柄又小。RS2 的 {@code ScrollbarWidget}
     * 就是"点哪儿跳哪儿 + 按住即拖"，手感对，这里照搬。代价是失去"点手柄上下翻一屏"，
     * 翻屏留给滚轮 + Ctrl 和 PageUp/PageDown。
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isOver(mouseX, mouseY)) {
            return false;
        }
        if (!isEnabled()) {
            return true; // 内容放得下：吃掉点击，什么也不做
        }
        dragging = true;
        jumpTo(mouseY);
        return true;
    }

    /** 拖动中：手柄跟着鼠标走 */
    public boolean mouseDragged(double mouseY) {
        if (!dragging || !isEnabled()) {
            return false;
        }
        jumpTo(mouseY);
        return true;
    }

    /** 按"点击处的 y"换算手柄位置：以手柄中心对齐鼠标，免得点击瞬间手柄突跳半格 */
    private void jumpTo(double mouseY) {
        int usable = trackH - handleHeight();
        if (usable <= 0) {
            moveTo(0);
            return;
        }
        double upper = mouseY - trackY - handleHeight() / 2.0;
        double position = Math.clamp(upper / usable, 0.0, 1.0);
        moveTo((int) Math.round(position * maxRow()));
    }

    public boolean mouseReleased() {
        boolean was = dragging;
        dragging = false;
        return was;
    }

    public boolean isDragging() {
        return dragging;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!isOver(mouseX, mouseY) || !isEnabled()) {
            return false;
        }
        // 滚轮一律一行：和"鼠标停在网格上滚"保持一致的手感。
        // 一屏一页留给"点手柄上/下方"和 Ctrl + 滚轮。
        scroll(scrollY > 0 ? -1 : 1, 1);
        return true;
    }

    /** @param steps 正数向下、负数向上 */
    public void scroll(int steps, int rowsPerStep) {
        if (steps == 0) {
            return;
        }
        moveTo(currentRow + steps * Math.max(1, rowsPerStep));
    }

    // ===== 绘制 =====

    /** 手柄。轨道凹槽已经烤进底图了，这里只画会动的那截。 */
    public void render(GuiGraphicsExtractor g, float time, boolean hovered) {
        boolean enabled = isEnabled();
        int h = handleHeight();
        int y = handleTop();
        int x = trackX + 1;
        int w = trackW - 2;

        if (!enabled) {
            // 内容一屏放得下：画一条暗色实心条表示"没有可滚动的部分"
            KleinTheme.vGradient(g, x, trackY, w, trackH,
                    KleinTheme.withAlpha(KleinTheme.WELL_EDGE, 0.55F),
                    KleinTheme.withAlpha(KleinTheme.WELL_EDGE, 0.25F));
            KleinTheme.outline(g, x, trackY, w, trackH, KleinTheme.withAlpha(KleinTheme.WELL_TICK, 0.5F));
            return;
        }

        int top = dragging || hovered ? KleinTheme.CYAN : KleinTheme.BORDER_BRIGHT;
        int bottom = dragging || hovered ? KleinTheme.MAGENTA : KleinTheme.CYAN_DEEP;
        KleinTheme.vGradient(g, x, y, w, h, KleinTheme.withAlpha(top, 0.95F), KleinTheme.withAlpha(bottom, 0.95F));
        KleinTheme.outline(g, x, y, w, h, KleinTheme.withAlpha(KleinTheme.TEXT, dragging ? 0.85F : 0.45F));

        // 三条握纹：让手柄"看起来能抓"
        int gripY = y + h / 2 - 3;
        int gripColor = KleinTheme.withAlpha(KleinTheme.PANEL_DEEP, 0.75F);
        for (int i = 0; i < 3; i++) {
            int gy = gripY + i * 3;
            if (gy > y + 2 && gy < y + h - 3) {
                g.fill(x + 3, gy, x + w - 3, gy + 1, gripColor);
            }
        }

        // 拖动时手柄外侧的一圈呼吸光，强调"你现在正在移动视窗"
        if (dragging) {
            float pulse = 0.35F + 0.25F * (float) Math.sin(time * 6.0F);
            KleinTheme.outline(g, x - 1, y - 1, w + 2, h + 2, KleinTheme.withAlpha(KleinTheme.CYAN, pulse));
        }
    }
}
