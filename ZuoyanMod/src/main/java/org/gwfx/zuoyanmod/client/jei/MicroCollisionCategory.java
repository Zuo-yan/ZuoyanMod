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

import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.block.BlockRegistry;
import org.gwfx.zuoyanmod.recipe.MicroCollisionRecipe;

/**
 * JEI 配方类别：微型强子对撞（zuoyanmod:micro_collision）。
 * 布局：粒子束 A | 粒子束 B — 对撞束线+箭头区 — 产物（下标注对撞时长）。
 * 类别图标 = 对撞机方块物品；催化剂 = 对撞机（见 ZuoyanJeiPlugin）。
 *
 * <p>1.20.1 适配（JEI 15.x）：1.20.1 的配方没有 RecipeHolder 包装，
 * 类别直接以 {@link MicroCollisionRecipe} 为配方类型。</p>
 */
public class MicroCollisionCategory implements IRecipeCategory<MicroCollisionRecipe> {

    /** JEI 配方类型：与 mod 自己的 RecipeType 同名不同体系，仅作类别标识。 */
    public static final RecipeType<MicroCollisionRecipe> TYPE =
            RecipeType.create(Zuoyanmod.MODID, "micro_collision", MicroCollisionRecipe.class);

    private static final int WIDTH = 124;
    private static final int HEIGHT = 40;

    /** 槽位布局常量（与 setRecipe/draw 严格对应） */
    private static final int SLOT_A_X = 4;
    private static final int SLOT_B_X = 22;
    private static final int SLOT_Y = 12;
    private static final int ARROW_X = 58;
    private static final int OUT_X = 94;

    private final IDrawable icon;

    public MicroCollisionCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(BlockRegistry.MICRO_HADRON_COLLIDER.get());
    }

    @Override
    public RecipeType<MicroCollisionRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.zuoyanmod.jei.micro_collision");
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
    public void setRecipe(IRecipeLayoutBuilder builder, MicroCollisionRecipe recipe, IFocusGroup focuses) {
        // 双束流：A/B 顺序无关，两格都作为 INPUT 参与查询匹配
        builder.addSlot(RecipeIngredientRole.INPUT, SLOT_A_X, SLOT_Y)
                .addIngredients(recipe.inputA())
                .setStandardSlotBackground();
        builder.addSlot(RecipeIngredientRole.INPUT, SLOT_B_X, SLOT_Y)
                .addIngredients(recipe.inputB())
                .setStandardSlotBackground();
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X, SLOT_Y)
                .addItemStack(recipe.resultDisplay())
                .setOutputSlotBackground();
    }

    @Override
    public void draw(MicroCollisionRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;

        // 对撞束线：A|B → 产物方向的一条充能线
        int lineY = SLOT_Y + 8;
        guiGraphics.fill(SLOT_B_X + 20, lineY, ARROW_X - 2, lineY + 1, 0xFF3FD9C8);
        guiGraphics.fill(ARROW_X + 22, lineY, OUT_X - 3, lineY + 1, 0xFF3FD9C8);

        // 对撞时长（秒）——只在这里给数字：玩家要据此决定红石脉冲宽度
        int seconds = Math.max(1, Math.round(recipe.duration() / 20.0F));
        Component label = Component.translatable("gui.zuoyanmod.jei.duration", seconds);
        guiGraphics.drawString(font, label, (WIDTH - font.width(label)) / 2, HEIGHT - 10, 0xFF9FB8C8, false);
    }
}
