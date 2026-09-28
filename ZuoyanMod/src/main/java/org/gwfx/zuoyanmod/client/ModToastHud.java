package org.gwfx.zuoyanmod.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.client.klein.KleinTheme;

/**
 * 模组效果通知的屏幕渲染：右上角堆叠 Toast，替代聊天栏刷屏。
 *
 * <p>挂在 {@code RenderGuiEvent.Post}（与升级 HUD 同层思路）：F1 隐藏 HUD 时
 * 原版根本不会走到这一层，不用自己处理。面板用 KleinTheme 深色底 + 描边，
 * 文字保留服务端下发的 § 配色；生命末期面板透明度线性降 0（淡出只做面板，
 * § 多色文本没法整条统一调 alpha，视觉上面板先隐、文字随后整体消失，观感一致）。
 */
@EventBusSubscriber(modid = Zuoyanmod.MODID, value = Dist.CLIENT)
public final class ModToastHud {

    private static final int PAD_X = 5;
    private static final int PAD_Y = 3;
    private static final int GAP = 2;
    private static final int MARGIN = 5;

    private ModToastHud() {}

    @SubscribeEvent
    public static void onRenderHud(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.hud.isHidden()) {
            return;
        }
        List<ClientModToasts.Toast> toasts = ClientModToasts.active();
        if (toasts.isEmpty()) {
            return;
        }

        GuiGraphicsExtractor g = event.getGuiGraphics();
        Font font = minecraft.font;
        int right = minecraft.getWindow().getGuiScaledWidth() - MARGIN;
        long now = System.currentTimeMillis();

        int y = MARGIN;
        for (ClientModToasts.Toast toast : toasts) {
            float alpha = toast.alphaAt(now);
            int width = font.width(toast.text()) + PAD_X * 2;
            int height = font.lineHeight + PAD_Y * 2;
            int x = right - width;

            g.fill(x, y, x + width, y + height, KleinTheme.withAlpha(KleinTheme.PANEL_DEEP, 0.85F * alpha));
            g.outline(x, y, width, height, KleinTheme.withAlpha(KleinTheme.BORDER, alpha));
            g.text(font, toast.text(), x + PAD_X, y + PAD_Y, KleinTheme.withAlpha(KleinTheme.TEXT, alpha), true);

            y += height + GAP;
        }
    }
}
