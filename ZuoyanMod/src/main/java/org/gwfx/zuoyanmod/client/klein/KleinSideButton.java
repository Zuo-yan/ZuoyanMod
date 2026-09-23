package org.gwfx.zuoyanmod.client.klein;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 终端的侧边功能按钮：**真控件**，不是"自己在 Screen 里画个方块再手写命中判定"。
 *
 * <h2>为什么要换成真控件（v6）</h2>
 * v5 及之前这四个按钮是 {@code KleinBottleScreen} 手画的：自己算 hover、自己算命中、
 * 自己发包。结果**四个按钮点了全没反应**——手写命中那一套和控件框架并行，
 * 事件到底被谁吃掉、坐标差一像素，只有进游戏才看得出来。
 * 换成 {@link AbstractButton} 之后，命中/hover/按键触发/按下音效全部由控件框架负责，
 * {@code Screen} 只负责摆位置；这类"看起来对但点不动"的问题就不再可能出现。
 * RS2 的 {@code AbstractSideButtonWidget} 就是这么做的（继承 {@code Button}、
 * 屏幕里 {@code addSideButton} 逐个往下排），这里照搬了那套结构。
 *
 * <h2>外观</h2>
 * **无边框、浮空**（v8，玩家点名要 RS2 那种）：平时就是一枚图标；hover 时
 * 一层极淡的底 + 底部一条青色下划线。图标由调用方给一个绘制回调——
 * 排序方向那种会随状态翻转的图标就是这么换的。
 *
 * <p>1.20.1 适配：GuiGraphicsExtractor→{@link GuiGraphics}；
 * onPress 不带参数；extractContents→renderWidget。
 */
public final class KleinSideButton extends AbstractButton {

    /** 图标绘制回调：在按钮左上角坐标系里画 {@code size × size} 的图标 */
    public interface IconRenderer {
        void draw(GuiGraphics graphics, int x, int y, int size, boolean hovered, float time);
    }

    private final IconRenderer icon;
    private final Runnable action;

    public KleinSideButton(int x, int y, int size, Component name, IconRenderer icon, Runnable action) {
        super(x, y, size, size, name);
        this.icon = icon;
        this.action = action;
    }

    @Override
    public void onPress() {
        action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHovered();
        // **不画边框**（v8 改的，玩家点名要 RS2 那种"四个浮空控件"）：
        // 平时只有图标本身；hover 时才给一层极淡的底 + 底部一条青色下划线，
        // 告诉你"这里能点"，但不至于变成一个方块按钮。
        if (hovered) {
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                    KleinTheme.withAlpha(KleinTheme.BUTTON_FILL_HOVER, 0.55F));
            graphics.fill(getX() + 2, getY() + getHeight() - 2,
                    getX() + getWidth() - 2, getY() + getHeight() - 1,
                    KleinTheme.withAlpha(KleinTheme.CYAN, 0.8F));
        }
        icon.draw(graphics, getX(), getY(), getWidth(), hovered, KleinTheme.now());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
