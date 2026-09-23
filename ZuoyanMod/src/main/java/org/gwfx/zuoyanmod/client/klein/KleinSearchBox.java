package org.gwfx.zuoyanmod.client.klein;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * 终端的搜索框。
 *
 * <p>在 {@link EditBox} 上做三件小事：
 * <ol>
 *   <li><b>去掉原版边框</b>（{@code setBordered(false)}）。原版边框是给浅色背景用的，
 *       在深紫底上非常突兀；终端的凹槽 + 放大镜由 {@link KleinTheme#searchFrame} 现画。</li>
 *   <li><b>把点击热区放大到整个凹槽</b>。因为文字带只有 9px 高，直接点凹槽的上下两像素会点不到，
 *       玩家会以为搜索框坏了。</li>
 *   <li><b>右键清空</b>（照抄 RS2 的 {@code SearchFieldWidget}），顺手。</li>
 * </ol>
 */
public final class KleinSearchBox extends EditBox {

    /** 文字带相对凹槽的内缩：左边留给放大镜，右边留一点呼吸 */
    public static final int TEXT_INSET_X = 20;
    public static final int TEXT_INSET_RIGHT = 6;

    private final int frameX;
    private final int frameY;
    private final int frameW;
    private final int frameH;

    public KleinSearchBox(Font font, int frameX, int frameY, int frameW, int frameH, Component hint) {
        super(font,
                frameX + TEXT_INSET_X,
                frameY + (frameH - font.lineHeight) / 2,
                Math.max(8, frameW - TEXT_INSET_X - TEXT_INSET_RIGHT),
                font.lineHeight,
                Component.translatable("gui.zuoyanmod.klein.search"));
        this.frameX = frameX;
        this.frameY = frameY;
        this.frameW = frameW;
        this.frameH = frameH;

        setBordered(false);
        setMaxLength(64);
        // 提示文字自己带颜色：EditBox#setHint 只在"样式为空"时才套默认的深灰（0xFF555555），
        // 那个颜色在深紫底上几乎看不见。给一个显式样式，它就原样保留。
        setHint(hint.copy().withStyle(ChatFormatting.GRAY));
        setTextColor(KleinTheme.TEXT);
        setTextColorUneditable(KleinTheme.TEXT_DIM);
        setTextShadow(false);
        // 允许失焦，否则 Esc 会被搜索框永久吃掉、界面关不掉
        setCanLoseFocus(true);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= frameX && mouseX < frameX + frameW
                && mouseY >= frameY && mouseY < frameY + frameH;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (isMouseOver(event.x(), event.y())) {
            if (event.button() == 1) {
                setValue("");
            }
            // super 会按"点到第几个字符"移动光标（它用的是文字带坐标，越界会夹到两端，无副作用）
            super.mouseClicked(event, doubleClick);
            setFocused(true);
            return true;
        }
        setFocused(false);
        return false;
    }
}
