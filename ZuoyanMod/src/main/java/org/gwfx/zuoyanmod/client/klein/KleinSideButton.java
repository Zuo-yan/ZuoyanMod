package org.gwfx.zuoyanmod.client.klein;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
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
 */
public final class KleinSideButton extends AbstractButton {

    /** 图标绘制回调：在按钮左上角坐标系里画 {@code size × size} 的图标 */
    public interface IconRenderer {
        void draw(GuiGraphicsExtractor graphics, int x, int y, int size, boolean hovered, float time);
    }

    private final IconRenderer icon;
    private final Runnable action;

    public KleinSideButton(int x, int y, int size, Component name, IconRenderer icon, Runnable action) {
        super(x, y, size, size, name);
        this.icon = icon;
        this.action = action;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHovered();
        // **无边框、无底板**（v8，玩家点名要 RS2 那种"四个浮空控件"）：
        // 平时只有图标本身；hover 时也**只画一条青色下划线**，连淡底都不给——
        // 之前留过一层 0.55 alpha 的紫底，玩家看到的还是"一个紫色方块按钮"。
        if (hovered) {
            graphics.fill(getX() + 1, getY() + getHeight() - 2,
                    getX() + getWidth() - 1, getY() + getHeight() - 1,
                    KleinTheme.withAlpha(KleinTheme.CYAN, 0.85F));
        }
        icon.draw(graphics, getX(), getY(), getWidth(), hovered, KleinTheme.now());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
