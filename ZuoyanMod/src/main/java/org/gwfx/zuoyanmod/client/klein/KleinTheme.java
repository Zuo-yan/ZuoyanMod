package org.gwfx.zuoyanmod.client.klein;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gwfx.zuoyanmod.Zuoyanmod;

/**
 * 终端的视觉语言：配色 + 全部**运行时绘制**的装饰与图标。
 *
 * <h2>为什么按钮/搜索框/滚动条手柄都是画出来的，而不是贴图</h2>
 * 只有"静态且面积大"的东西（面板、54 个槽位凹槽、滚动条凹槽、玩家背包凹槽）烤进底图
 * {@link #BACKGROUND}；凡是需要在 hover / 开关 / 拖拽时换样子的东西全部用
 * {@code fill} 现场画。这样不用为每个状态各出一张贴图，也不会因为贴图对不上而错位。
 *
 * <h2>"无限 / 四维"的视觉语言（第五版）</h2>
 * <ul>
 *   <li><b>无限符号</b>（{@link #infinityFlow}）：物品栏右侧那一竖条。
 *       竖放的双纽线 + 一个顺曲线往下淌的光点。它占的是早先"维度刻度"的位置——
 *       那条因为紧贴物品栏、看起来像第二根滚动条被砍了。</li>
 *   <li><b>越界辉光</b>（{@link #amountColor} + {@code KleinAmountRenderer#drawGlow}）：
 *       数量叠字在总量破千时转成青色并发光，用颜色语言说明"这一格已经不是普通堆叠了"。</li>
 *   <li><b>超立方角标</b>（底图里烤的青色 L）：只点存储槽不点背包槽，
 *       一眼分出"四维空间"和"背包"。</li>
 *   <li><b>三张功能卡片</b>（底图里烤的）：合成 = 暖紫 / 熔炉 = 炉火橙 / 铁砧 = 钢灰蓝，
 *       各自带色调、边框和顶部亮边——层次感靠这个，不靠分隔线。</li>
 * </ul>
 *
 * <p>⚠️ 早先用过的两种纯装饰——标题旁的克莱因环徽记（v5 删，顶部太挤）、
 * 右栏底部的维度涡环（v7 删，位置让给了铁砧）——都别加回来。
 * 需要"自交曲面"那种意象时请用小尺寸，放大到 19px 会像一只眼睛。
 *
 * <p>配色沿用项目既有的紫金体系（与虚空共振泵、旧版四维空间界面一致），
 * 青色作为"第四维"的强调色；熔炉用金 → 青的炉火，和合成区的暖紫边区分开。
 */
public final class KleinTheme {

    // ===== 底图 =====
    public static final ResourceLocation BACKGROUND =
            new ResourceLocation(Zuoyanmod.MODID, "textures/gui/klein_terminal.png");

    // ===== 面板 =====
    public static final int PANEL = 0xFF1B1430;
    public static final int PANEL_DEEP = 0xFF120C22;
    public static final int BORDER_OUTER = 0xFF3A2C5E;
    public static final int BORDER = 0xFF6D5BA8;
    public static final int BORDER_BRIGHT = 0xFF9B7CF8;

    // ===== 槽位 =====
    public static final int WELL = 0xFF0D0918;
    public static final int WELL_EDGE = 0xFF352A50;
    public static final int WELL_TICK = 0xFF4E3F78;

    // ===== 文字 =====
    public static final int TEXT = 0xFFEDE7F6;
    public static final int TEXT_DIM = 0xFF9C8FBE;
    public static final int TEXT_FAINT = 0xFF5E5378;
    public static final int TEXT_WARN = 0xFFFF8A8A;

    // ===== 强调色 =====
    public static final int CYAN = 0xFF6FE3D4;
    public static final int CYAN_DEEP = 0xFF2F8C86;
    public static final int MAGENTA = 0xFFF06FD8;
    public static final int GOLD = 0xFFFFD479;

    // ===== 按钮 =====
    public static final int BUTTON_FILL = 0xFF2A2046;
    public static final int BUTTON_FILL_HOVER = 0xFF3C2E67;
    public static final int BUTTON_ON = 0xFF29506B;
    public static final int BUTTON_EDGE = 0xFF4B3B78;
    public static final int BUTTON_EDGE_HOVER = 0xFF8C72E0;

    /** 一帧里"现在几秒"——动画统一用它，避免各处各算一套 */
    public static float now() {
        return (System.currentTimeMillis() % 1_000_000L) / 1000.0F;
    }

    private KleinTheme() {}

    // ===== 通用绘制 =====

    public static int withAlpha(int argb, float alpha) {
        int a = (int) (((argb >>> 24) & 0xFF) * Mth.clamp(alpha, 0.0F, 1.0F));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    public static int scaleColor(int argb, float factor) {
        int r = Mth.clamp((int) (((argb >> 16) & 0xFF) * factor), 0, 255);
        int g = Mth.clamp((int) (((argb >> 8) & 0xFF) * factor), 0, 255);
        int b = Mth.clamp((int) ((argb & 0xFF) * factor), 0, 255);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /** 上下渐变的横条（用若干条 1px 线近似），用于"发光"物件 */
    public static void vGradient(GuiGraphics g, int x, int y, int w, int h, int top, int bottom) {
        for (int i = 0; i < h; i++) {
            float t = h <= 1 ? 0F : (float) i / (h - 1);
            int r = (int) (((top >> 16) & 0xFF) * (1 - t) + ((bottom >> 16) & 0xFF) * t);
            int gg = (int) (((top >> 8) & 0xFF) * (1 - t) + ((bottom >> 8) & 0xFF) * t);
            int b = (int) ((top & 0xFF) * (1 - t) + (bottom & 0xFF) * t);
            int a = (int) (((top >>> 24) & 0xFF) * (1 - t) + ((bottom >>> 24) & 0xFF) * t);
            g.fill(x, y + i, x + w, y + i + 1, (a << 24) | (r << 16) | (gg << 8) | b);
        }
    }

    /** 1px 描边矩形 */
    public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** 工具按钮：外框随 hover / 开关状态换色 */
    public static void buttonFrame(GuiGraphics g, int x, int y, int size, boolean hovered, boolean on) {
        int fill = on ? BUTTON_ON : (hovered ? BUTTON_FILL_HOVER : BUTTON_FILL);
        g.fill(x, y, x + size, y + size, fill);
        vGradient(g, x + 1, y + 1, size - 2, (size - 2) / 2,
                withAlpha(0xFFFFFFFF, 0.06F), withAlpha(0xFFFFFFFF, 0.0F));
        int edge = hovered ? BUTTON_EDGE_HOVER : BUTTON_EDGE;
        outline(g, x, y, size, size, edge);
        if (on || hovered) {
            // 左上角的高光角标：和槽位四角的"超立方刻度"呼应
            g.fill(x + 1, y + 1, x + 3, y + 2, on ? CYAN : withAlpha(CYAN, 0.65F));
            g.fill(x + 1, y + 1, x + 2, y + 3, on ? CYAN : withAlpha(CYAN, 0.65F));
        }
    }

    /** 搜索框：凹槽 + 左侧放大镜 + 聚焦时的青色呼吸边 */
    public static void searchFrame(GuiGraphics g, int x, int y, int w, int h, boolean focused) {
        g.fill(x, y, x + w, y + h, withAlpha(PANEL_DEEP, 0.92F));
        outline(g, x, y, w, h, focused ? withAlpha(CYAN, 0.9F) : withAlpha(BORDER, 0.7F));
        // 放大镜
        int cx = x + 7;
        int cy = y + h / 2 - 1;
        outline(g, cx - 3, cy - 3, 6, 6, focused ? CYAN : TEXT_DIM);
        g.fill(cx + 2, cy + 2, cx + 4, cy + 3, focused ? CYAN : TEXT_DIM);
        g.fill(cx + 3, cy + 3, cx + 5, cy + 4, focused ? CYAN : TEXT_DIM);
    }

    /** 存储槽：在底图的凹槽上再叠一层，用于 hover 高亮与"越界"提示 */
    public static void slotOverlay(GuiGraphics g, int slotX, int slotY, int color) {
        g.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, color);
    }

    /** 悬浮在槽位四角的青色括号，比原版高亮更贴合"超立方"的观感 */
    public static void cornerBrackets(GuiGraphics g, int x, int y, int color) {
        int x1 = x + 16;
        int y1 = y + 16;
        // 左上
        g.fill(x - 1, y - 1, x + 4, y, color);
        g.fill(x - 1, y - 1, x, y + 4, color);
        // 右上
        g.fill(x1 - 3, y - 1, x1 + 1, y, color);
        g.fill(x1, y - 1, x1 + 1, y + 4, color);
        // 左下
        g.fill(x - 1, y1, x + 4, y1 + 1, color);
        g.fill(x - 1, y1 - 3, x, y1 + 1, color);
        // 右下
        g.fill(x1 - 3, y1, x1 + 1, y1 + 1, color);
        g.fill(x1, y1 - 3, x1 + 1, y1 + 1, color);
    }

    public static int blend(int a, int b, float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        int aa = (int) (((a >>> 24) & 0xFF) * (1 - t) + ((b >>> 24) & 0xFF) * t);
        int ar = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int ag = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int ab = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (aa << 24) | (ar << 16) | (ag << 8) | ab;
    }

    // ===== 无限符号（物品栏右侧那一条）=====

    /**
     * 竖放的 ∞，有一个光点顺着曲线往下淌。x 的幅度取 {@code w-2}：
     * {@code sin·cos} 的峰值只有 0.5，取 {@code w/2} 会瘦成一根线。
     */
    public static void infinityFlow(GuiGraphics g, int x, int y, int w, int h, float time) {
        float cx = x + w / 2.0F;
        float cy = y + h / 2.0F;
        float ax = w - 2.0F;
        float ay = h / 2.0F - 1.0F;
        final int steps = 140;
        for (int i = 0; i < steps; i++) {
            float t = (float) (i * 2 * Math.PI / steps);
            float px = cx + (float) (Math.sin(t) * Math.cos(t)) * ax;
            float py = cy - (float) Math.cos(t) * ay;
            float flow = 0.5F + 0.5F * (float) Math.sin(t - time * 2.0F);
            int color = blend(CYAN_DEEP, CYAN, flow);
            g.fill((int) px, (int) py, (int) px + 1, (int) py + 1, withAlpha(color, 0.22F + 0.60F * flow));
        }
        // 顺着曲线往下淌的光点：拖一条短尾巴，读起来才像"在流"而不是"在闪"
        float head = (float) ((time * 0.45F) % 1.0F) * (float) (2 * Math.PI);
        for (int k = 0; k < 7; k++) {
            float t = head - k * 0.13F;
            float px = cx + (float) (Math.sin(t) * Math.cos(t)) * ax;
            float py = cy - (float) Math.cos(t) * ay;
            g.fill((int) px, (int) py, (int) px + 1, (int) py + 1,
                    withAlpha(TEXT, 0.85F * (1.0F - k / 7.0F) + 0.08F));
        }
    }

    // ===== 熔炉：炉火与进度条 =====

    /** 炉火。火焰高度 = 剩余燃料，<b>从下往上烧掉</b>。底部金、顶部转青。 */
    public static void flame(GuiGraphics g, int x, int y, int w, int h, float fill, float time) {
        g.fill(x, y, x + w, y + h, withAlpha(WELL, 0.88F));
        outline(g, x, y, w, h, withAlpha(WELL_EDGE, 0.85F));
        if (fill <= 0.0F) {
            return;
        }
        int lit = Math.max(1, Math.round(h * Mth.clamp(fill, 0.0F, 1.0F)));
        float cx = x + w / 2.0F;
        for (int row = 0; row < lit; row++) {
            int yy = y + h - 1 - row;
            float t = lit <= 1 ? 0.0F : (float) row / (lit - 1);
            float flicker = 0.92F + 0.08F * (float) Math.sin(time * 6.5F + row * 0.9F);
            float half = (w / 2.0F) * (0.95F - 0.60F * t) * flicker;
            int color = blend(GOLD, CYAN, t * 0.85F);
            g.fill(Math.round(cx - half), yy, Math.round(cx + half), yy + 1, withAlpha(color, 0.95F));
        }
    }

    /** 横向进度条：填充随进度推进，头部有一道会呼吸的亮边 */
    public static void progressBar(GuiGraphics g, int x, int y, int w, int h, float progress, float time) {
        g.fill(x, y, x + w, y + h, withAlpha(WELL, 0.88F));
        outline(g, x, y, w, h, withAlpha(WELL_EDGE, 0.85F));
        float p = Mth.clamp(progress, 0.0F, 1.0F);
        int filled = Math.round(w * p);
        if (filled <= 0) {
            return;
        }
        for (int i = 0; i < filled; i++) {
            int color = blend(CYAN_DEEP, CYAN, (float) i / w);
            g.fill(x + i, y + 1, x + i + 1, y + h - 1, withAlpha(color, 0.95F));
        }
        int head = x + filled;
        float pulse = 0.55F + 0.35F * (float) Math.sin(time * 4.0F);
        g.fill(head - 1, y, head + 1, y + h, withAlpha(TEXT, pulse));
    }

    /** 单行输入框的凹槽（铁砧改名用）。搜索框带放大镜，这里不带，其余同款。 */
    public static void nameFrame(GuiGraphics g, int x, int y, int w, int h, boolean focused) {
        g.fill(x, y, x + w, y + h, withAlpha(PANEL_DEEP, 0.92F));
        outline(g, x, y, w, h, focused ? withAlpha(CYAN, 0.9F) : withAlpha(BORDER, 0.7F));
    }

    /**
     * 斜向扫光。只画在网格带上、alpha 很低，用来让整片槽位"活"起来，
     * 又不会盖住物品图标（最大值 ≈ 0x12）。
     */
    public static void sweep(GuiGraphics g, int x, int y, int w, int h, float time, int color) {
        float period = 7.5F;
        float t = (time % period) / period;
        int bandW = 26;
        int head = x - bandW + (int) ((w + bandW * 2) * t);
        g.enableScissor(x, y, x + w, y + h);
        for (int i = 0; i < bandW; i++) {
            float fade = 1.0F - Math.abs(i - bandW / 2.0F) / (bandW / 2.0F);
            int col = withAlpha(color, 0.075F * fade);
            int cx = head + i;
            if (cx >= x && cx < x + w) {
                g.fill(cx, y, cx + 1, y + h, col);
            }
        }
        g.disableScissor();
    }

    // ===== 图标 =====

    /** 工作台：2×2 网格。右栏「合成」标题旁边那枚小图标。 */
    public static void iconCrafting(GuiGraphics g, int x, int y, int size, int color) {
        int mid = size / 2;
        outline(g, x + 1, y + 1, size - 2, size - 2, color);
        g.fill(x + mid - 1, y + 2, x + mid, y + size - 2, color);
        g.fill(x + 2, y + mid - 1, x + size - 2, y + mid, color);
    }

    /** 回到顶部：顶部一条横杠 + 一个向上的箭头 */
    public static void iconToTop(GuiGraphics g, int x, int y, int size, int color, int accent) {
        int cx = x + size / 2;
        g.fill(x + 3, y + 3, x + size - 3, y + 4, color);
        g.fill(cx - 1, y + 6, cx, y + size - 3, accent);
        int steps = 4;
        for (int i = 0; i < steps; i++) {
            g.fill(cx - steps + i, y + 6 + i, cx + steps - i, y + 7 + i, accent);
        }
    }

    /** 排序方式：长度递减的三条横条（右对齐） */
    public static void iconSortMode(GuiGraphics g, int x, int y, int size, int color) {
        int right = x + size - 2;
        g.fill(right - 12, y + 3, right, y + 4, color);
        g.fill(right - 12, y + 7, right - 4, y + 8, color);
        g.fill(right - 12, y + 11, right - 8, y + 12, color);
        g.fill(x + 2, y + 3, x + 3, y + 4, withAlpha(color, 0.6F));
        g.fill(x + 2, y + 7, x + 3, y + 8, withAlpha(color, 0.6F));
        g.fill(x + 2, y + 11, x + 3, y + 12, withAlpha(color, 0.6F));
    }

    /** 排序方向：升/降箭头 */
    public static void iconSortDir(GuiGraphics g, int x, int y, int size, int color, boolean down) {
        int cx = x + size / 2;
        int top = y + 3;
        int bottom = y + size - 4;
        g.fill(cx - 1, top, cx, bottom, color);
        int steps = 4;
        for (int i = 0; i < steps; i++) {
            if (down) {
                g.fill(cx - steps + i, bottom - i - 1, cx + steps - i, bottom - i, color);
            } else {
                g.fill(cx - steps + i, top + i + 1, cx + steps - i, top + i + 2, color);
            }
        }
    }

    /** 清空搜索：一个 × */
    public static void iconClear(GuiGraphics g, int x, int y, int size, int color) {
        int a = x + 3;
        int b = y + 3;
        int len = size - 6;
        for (int i = 0; i < len; i++) {
            g.fill(a + i, b + i, a + i + 2, b + i + 2, color);
            g.fill(a + len - 1 - i, b + i, a + len + 1 - i, b + i + 2, color);
        }
    }

    /** 熔炉图标：一个小炉膛 + 里面的火苗。「熔炼」标题旁边那枚。 */
    public static void iconFurnace(GuiGraphics g, int x, int y, int size, int color, int fireColor) {
        outline(g, x + 1, y + 1, size - 2, size - 2, color);
        g.fill(x + 2, y + size - 3, x + size - 2, y + size - 2, color);
        int cx = x + size / 2;
        int base = y + size - 4;
        for (int r = 0; r < 4; r++) {
            int half = Math.max(1, (4 - r) / 2 + 1);
            g.fill(cx - half, base - r, cx + half, base - r + 1, fireColor);
        }
    }

    /** 铁砧图标：锤头朝下的铁砧侧影。「铁砧」标题旁边那枚。 */
    public static void iconAnvil(GuiGraphics g, int x, int y, int size, int color) {
        int base = y + size - 3;
        g.fill(x + 1, base, x + size - 1, base + 2, color);              // 底座
        g.fill(x + 3, base - 2, x + size - 3, base, color);              // 颈
        g.fill(x + 2, base - 4, x + size - 2, base - 2, color);          // 身
        g.fill(x + 1, base - 6, x + size - 1, base - 4, color);          // 砧角
        g.fill(x + size - 4, base - 5, x + size - 2, base - 4, color);   // 尖角
    }

    // ===== 数量叠字的配色 =====

    /**
     * 数量叠字颜色。普通堆叠用白色；一旦总量破千就转青并发光——
     * 这是"这一格是四维空间的条目、不是普通背包格子"的颜色语言。
     */
    public static int amountColor(long total) {
        if (total >= 1_000_000L) {
            return CYAN;
        }
        if (total >= 1_000L) {
            return blend(TEXT, CYAN, 0.55F);
        }
        return TEXT;
    }
}

