package org.gwfx.zuoyanmod.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.gwfx.zuoyanmod.menu.MicroHadronColliderMenu;

/**
 * 微型强子对撞机界面：自绘布局（176x146，与虚空共振泵同尺寸体系）。
 * 布局：标题行（含充能状态）→ 粒子束 A | B — 对撞进度束 — 产物槽 → 玩家背包。
 * 坐标常量与 tools/gen_collider_textures.py 的 GUI 部分严格对应，改一处必改另一处。
 *
 * extractBackground 是屏幕绝对坐标；extractLabels 是 GUI 局部坐标且默认深灰字（深底看不清，不调 super）。
 */
public class MicroHadronColliderScreen extends AbstractContainerScreen<MicroHadronColliderMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("zuoyanmod", "textures/gui/micro_hadron_collider.png");

    private static final int TEX_W = 176;
    private static final int TEX_H = 146;

    /** 对撞进度束内槽（B 束与产物槽之间，从左向右充能） */
    private static final int PROG_X = 84;
    private static final int PROG_Y = 27;
    private static final int PROG_W = 30;
    private static final int PROG_H = 10;

    public MicroHadronColliderScreen(MicroHadronColliderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEX_W, TEX_H);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 60;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, TEX_W, TEX_H, TEX_W, TEX_H);

        // 对撞进度：从左向右填充，充能后呈高能青白色
        int progress = menu.getProgress();
        int duration = menu.getDuration();
        if (progress > 0 && duration > 0) {
            int filled = Math.min(PROG_W, progress * PROG_W / duration);
            if (filled > 0) {
                graphics.fill(x + PROG_X, y + PROG_Y, x + PROG_X + filled, y + PROG_Y + PROG_H, 0xFF3FD9C8);
                graphics.fill(x + PROG_X, y + PROG_Y, x + PROG_X + filled, y + PROG_Y + 2, 0xFFBFFDF4);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 深底上用浅色文字（原版默认 0xFF404040 会看不清），故不调用 super
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFFE8F4F6, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF8FA3BE, false);

        String status;
        int color;
        if (!menu.isPowered()) {
            status = "§7未充能 · 接红石信号";
            color = 0xFF7A6E96;
        } else if (menu.getProgress() > 0) {
            status = "对撞中 " + menu.getProgress() * 100 / menu.getDuration() + "%";
            color = 0xFF3FD9C8;
        } else {
            status = "已充能 · 待投料";
            color = 0xFF6FE3D4;
        }
        graphics.text(font, status, TEX_W - 9 - font.width(status), titleLabelY, color, false);
    }
}
