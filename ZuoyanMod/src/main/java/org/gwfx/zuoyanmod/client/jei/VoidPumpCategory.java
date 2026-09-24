package org.gwfx.zuoyanmod.client.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.block.BlockRegistry;

/**
 * JEI 配方类别：虚空共振泵（zuoyanmod:void_pump，仅 JEI 展示用）。
 *
 * <p>泵的转化逻辑硬编码在 {@link org.gwfx.zuoyanmod.block.VoidResonancePumpBlockEntity} 里
 * （燃料价值表），不是数据驱动的原版 Recipe，所以 JEI 侧用纯展示对象
 * {@link VoidPumpDisplay} + {@code RecipeType.create(...)} 注册——
 * 不进配方同步管线，注册期直接喂给 JEI 即可。</p>
 *
 * <p>布局：输入槽 — 共振线+箭头 — 产物（下标注燃料共振值）。
 * 类别图标 = 虚空共振泵方块物品；催化剂 = 虚空共振泵（见 ZuoyanJeiPlugin）。</p>
 *
 * <p>1.20.1 适配（JEI 15.x）：类别类型是 {@code mezz.jei.api.recipe.RecipeType}，
 * 绘制回调收 {@link GuiGraphics}，文字走 {@code drawString}。</p>
 */
public class VoidPumpCategory implements IRecipeCategory<VoidPumpCategory.VoidPumpDisplay> {

    /**
     * JEI-only 配方类型：id 用独立路径 "void_pump"，与原版 RecipeType 体系解耦
     * （{@code RecipeType.create(ns, path, class)} 不触碰注册表，静态初始化安全）。
     */
    public static final RecipeType<VoidPumpDisplay> TYPE =
            RecipeType.create(Zuoyanmod.MODID, "void_pump", VoidPumpDisplay.class);

    private static final int WIDTH = 124;
    private static final int HEIGHT = 40;

    /** 槽位布局常量（与 setRecipe/draw 严格对应） */
    private static final int SLOT_IN_X = 4;
    private static final int SLOT_Y = 12;
    private static final int ARROW_X = 58;
    private static final int OUT_X = 94;

    private final IDrawable icon;

    public VoidPumpCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(BlockRegistry.VOID_RESONANCE_PUMP.get());
    }

    @Override
    public RecipeType<VoidPumpDisplay> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.zuoyanmod.jei.void_pump");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, VoidPumpDisplay recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, SLOT_IN_X, SLOT_Y)
                .addItemStack(recipe.input())
                .setStandardSlotBackground();
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X, SLOT_Y)
                .addItemStack(recipe.output())
                .setOutputSlotBackground();
    }

    @Override
    public void draw(VoidPumpDisplay recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;

        // 共振线：输入 → 产物方向的一条充能线
        int lineY = SLOT_Y + 8;
        guiGraphics.fill(SLOT_IN_X + 20, lineY, ARROW_X - 2, lineY + 1, 0xFF8B5CF6);
        guiGraphics.fill(ARROW_X + 22, lineY, OUT_X - 3, lineY + 1, 0xFF3FD9C8);

        // 燃料共振值——玩家据此算"多少材料换多少粒子"
        Component label = Component.translatable("gui.zuoyanmod.jei.resonance", recipe.fuelValue());
        guiGraphics.drawString(font, label, (WIDTH - font.width(label)) / 2, HEIGHT - 10, 0xFF9FB8C8, false);
    }

    /**
     * 展示用"配方"：一份投入 → 产出暗物质粒子。
     *
     * @param input     可放入输入槽的燃料物品（展示栈）
     * @param output    产物（暗物质粒子）
     * @param fuelValue 燃料共振值（与 BlockEntity.fuelValue 一致）
     */
    public record VoidPumpDisplay(ItemStack input, ItemStack output, int fuelValue) {
    }
}
