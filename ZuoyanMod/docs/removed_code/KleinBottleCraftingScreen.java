package org.gwfx.zuoyanmod.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.menu.KleinBottleCraftingMenu;

/** 「四维空间 · 工作台」界面：3×3 网格 + 产物 + 27 格存储快取条（可翻页/滚轮）。 */
public class KleinBottleCraftingScreen extends AbstractContainerScreen<KleinBottleCraftingMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "textures/gui/four_dimensional_crafting.png");

    public static final int IMAGE_W = 176;
    public static final int IMAGE_H = 234;
    public static final int PLAYER_Y = KleinBottleCraftingMenu.PLAYER_Y;

    public KleinBottleCraftingScreen(KleinBottleCraftingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_W, IMAGE_H);
        this.inventoryLabelY = PLAYER_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("<"), b -> click(KleinBottleCraftingMenu.BUTTON_PAGE_PREV))
                .bounds(leftPos + 124, topPos + 22, 24, 14).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> click(KleinBottleCraftingMenu.BUTTON_PAGE_NEXT))
                .bounds(leftPos + 150, topPos + 22, 24, 14).build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    /** 滚轮 = 翻页 */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && minecraft != null && minecraft.player != null) {
            click(scrollY > 0 ? KleinBottleCraftingMenu.BUTTON_PAGE_NEXT : KleinBottleCraftingMenu.BUTTON_PAGE_PREV);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE,
                leftPos, topPos, 0.0F, 0.0F, IMAGE_W, IMAGE_H, IMAGE_W, IMAGE_H);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 不调 super：默认标签色在深色底上看不清
        String page = "页 " + (menu.getPage() + 1) + " / " + menu.getPageCount();
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFFEDE7F6, false);
        graphics.text(font, Component.literal(page), IMAGE_W - 8 - font.width(page), 8, 0xFFFFE082, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF9C8FBE, false);
    }
}
