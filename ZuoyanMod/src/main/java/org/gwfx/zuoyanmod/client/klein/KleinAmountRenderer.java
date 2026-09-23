package org.gwfx.zuoyanmod.client.klein;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 槽位右下角的数量叠字。
 *
 * <p>做法直接照搬 AE2 的 {@code StackSizeRenderer}：先把坐标系平移到槽位左上角，再整体缩放，
 * 这样字号虽小、位置却是按 16×16 的格子算的，任何缩放档位下都对齐在右下角。
 *
 * <p>和 AE2 的差别只有两点：
 * <ul>
 *   <li>缩放分三档（{@code 1.0 / 0.75 / 0.6 / 0.5}）而不是两档——数量达到 {@code 12.3M}
 *       这种五六个字符时，只有 0.5 才放得下且不至于糊成一片；</li>
 *   <li>颜色跟着量级走（见 {@link KleinTheme#amountColor(long)}）：破千转青、破百万纯青，
 *       让"这一格背后是四维空间"在视觉上先于文字被读到。</li>
 * </ul>
 *
 * <p>1.20.1 适配：GuiGraphicsExtractor→{@link GuiGraphics}，
 * pushMatrix/popMatrix→pushPose/popPose，graphics.text→graphics.drawString。
 */
public final class KleinAmountRenderer {

    /** 字体行高固定 9，写死避免依赖 Font 实例 */
    private static final int LINE_HEIGHT = 9;

    private KleinAmountRenderer() {}

    /**
     * 画数量叠字。
     *
     * <p>⚠️ <b>本方法不管绘制批次</b>：调用方要先自己调一次 {@code graphics.nextStratum()}（1.20.1 里叫
     * {@code graphics.flush()} 或在 item 层之后绘制），否则叠字可能被物品图标的批次盖住
     * （AE2 的 {@code StackSizeRenderer} 也是在调用点切批次的）。
     */
    public static void draw(GuiGraphics graphics, Font font, int slotX, int slotY, String text, int color) {
        int width = font.width(text);
        float scale = width <= 16 ? 1.0F
                : width <= 21 ? 0.75F
                : width <= 26 ? 0.6F
                : 0.5F;

        // 右对齐、贴槽位底边，留 1px 内缩
        float drawX = (16 - width * scale - 1.0F) / scale;
        float drawY = (16 - LINE_HEIGHT * scale) / scale;

        graphics.pose().pushPose();
        graphics.pose().translate(slotX, slotY, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, Math.round(drawX), Math.round(drawY), color, true);
        graphics.pose().popPose();
    }

    /**
     * 大数量额外补一圈很淡的外发光，读起来像"数字在往外溢"。
     * 只在 {@code >= 1000} 时调用，避免普通堆叠也被糊上一层。
     */
    public static void drawGlow(GuiGraphics graphics, Font font, int slotX, int slotY, String text,
                                int color, float time) {
        float pulse = 0.30F + 0.22F * (float) Math.sin(time * 2.4F);
        int glow = KleinTheme.withAlpha(color, pulse);
        int width = font.width(text);
        float scale = width <= 16 ? 1.0F : width <= 21 ? 0.75F : width <= 26 ? 0.6F : 0.5F;
        float drawX = (16 - width * scale - 1.0F) / scale;
        float drawY = (16 - LINE_HEIGHT * scale) / scale;

        graphics.pose().pushPose();
        graphics.pose().translate(slotX, slotY, 0);
        graphics.pose().scale(scale, scale, 1);
        for (int dx = -1; dx <= 1; dx += 2) {
            graphics.drawString(font, text, Math.round(drawX) + dx, Math.round(drawY), glow, false);
        }
        graphics.pose().popPose();
    }
}
