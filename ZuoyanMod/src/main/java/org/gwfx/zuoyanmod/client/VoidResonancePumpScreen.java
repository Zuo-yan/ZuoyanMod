package org.gwfx.zuoyanmod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.gwfx.zuoyanmod.block.VoidResonancePumpBlockEntity;
import org.gwfx.zuoyanmod.menu.VoidResonancePumpMenu;

/**
 * 虚空共振泵界面：自绘的极简布局（176x146），不再套用熔炉纹理。
 * 布局：顶部标题 + 状态；中部「输入槽 — 产出进度条 — 输出槽」一行；下方一条共振储备条（5 段等级刻度）；
 * 下半是玩家背包。所有坐标常量与 tools/gen_void_pump_textures.py 的 GUI 部分严格对应，改一处必改另一处。
 *
 * 1.20.1 适配：GuiGraphicsExtractor→{@link GuiGraphics}，
 * extractBackground/extractLabels→renderBg/renderLabels，blit 不再带 RenderPipelines。
 *   - renderBg 里的坐标是屏幕绝对坐标（需加 leftPos/topPos）
 *   - renderLabels 由父类在 translate(leftPos, topPos) 之后调用，坐标为 GUI 局部坐标
 */
public class VoidResonancePumpScreen extends AbstractContainerScreen<VoidResonancePumpMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("zuoyanmod", "textures/gui/void_resonance_pump.png");

    private static final int TEX_W = 176;
    private static final int TEX_H = 146;

    /** 产出进度条内槽（两槽之间） */
    private static final int PROG_X = 77;
    private static final int PROG_Y = 28;
    private static final int PROG_W = 22;
    private static final int PROG_H = 9;

    /** 共振储备条内槽（两槽下方） */
    private static final int RES_X = 56;
    private static final int RES_Y = 46;
    private static final int RES_W = 65;
    private static final int RES_H = 8;

    private static final int RES_MAX = VoidResonancePumpBlockEntity.MAX_RESONANCE;

    public VoidResonancePumpScreen(VoidResonancePumpMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = TEX_W;
        this.imageHeight = TEX_H;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 60;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.blit(TEXTURE, x, y, 0.0F, 0.0F, TEX_W, TEX_H, TEX_W, TEX_H);

        int level = menu.getResonanceLevel();

        // 产出进度：从左向右填充，等级越高越偏青（视觉上"充能"）
        int progress = menu.getProgress();
        if (progress > 0) {
            int filled = progress * PROG_W / 100;
            if (filled > 0) {
                graphics.fill(x + PROG_X, y + PROG_Y, x + PROG_X + filled, y + PROG_Y + PROG_H, flowColor(level));
            }
        }

        // 共振储备：0–1200 线性映射到整条（贴图里已带 4 条等级刻度）
        int resonance = menu.getResonance();
        if (resonance > 0) {
            int filled = resonance * RES_W / RES_MAX;
            if (filled > 0) {
                graphics.fill(x + RES_X, y + RES_Y, x + RES_X + filled, y + RES_Y + RES_H, levelColor(level));
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 深紫底上用浅色文字（原版默认 0xFF404040 会看不清），故不调用 super
        graphics.drawString(font, this.title, titleLabelX, titleLabelY, 0xFFEDE7F6, false);
        graphics.drawString(font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF9C8FBE, false);

        int level = menu.getResonanceLevel();
        String status = level > 0
                ? "共振 L" + level + " · " + menu.getResonance() + "/" + RES_MAX
                : "休眠 · 待投料";
        int color = level > 0 ? levelColor(level) : 0xFF7A6E96;
        graphics.drawString(font, status, TEX_W - 9 - font.width(status), titleLabelY, color, false);
    }

    /** 进度条填充色：等级越高越偏青 */
    private static int flowColor(int level) {
        return switch (level) {
            case 0 -> 0xFF4A3B6B;
            case 1, 2 -> 0xFF8B5CF6;
            case 3, 4 -> 0xFF6FE3D4;
            default -> 0xFF3FD9C8;
        };
    }

    /** 储备条填充色：由紫（低）渐变到青（高），作为"共振强度"的颜色语言 */
    private static int levelColor(int level) {
        return switch (level) {
            case 0 -> 0xFF3A2E55;
            case 1 -> 0xFF6D5BA8;
            case 2 -> 0xFF8B5CF6;
            case 3 -> 0xFF9B7CF8;
            case 4 -> 0xFF7ED7F0;
            default -> 0xFF3FD9C8;
        };
    }
}
