package org.gwfx.zuoyanmod.client.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;

import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.block.VoidResonancePumpBlockEntity;
import org.gwfx.zuoyanmod.item.ItemRegistry;
import org.gwfx.zuoyanmod.menu.KleinBottleMenu;
import org.gwfx.zuoyanmod.menu.MenuRegistry;
import org.gwfx.zuoyanmod.network.PacketHandler;
import org.gwfx.zuoyanmod.recipe.MicroCollisionRecipe;
import org.gwfx.zuoyanmod.recipe.RecipeRegistry;

import java.util.List;
import java.util.Optional;

/**
 * JEI 联动：让「四维空间」终端的**内嵌 3×3 工作台**能从玩家的四维空间直接抓料。
 *
 * <p>这就是 RS2 的合成网格 / AE2 的合成终端那套关系：终端里本来就摆着 3×3 网格和产物槽，
 * 在 JEI 里点配方右侧的 +，材料直接从网络里填进网格，不用先手动把东西从终端搬进背包。</p>
 *
 * <p>这个类只在装了 JEI 时才会被加载（JEI 通过注解扫描发现插件），
 * 所以 JEI 是 compileOnly 依赖也能安全留在这——没装 JEI 的客户端永远不会碰到它。</p>
 *
 * <p>1.20.1 适配（JEI 15.x）：
 * <ul>
 *   <li>Identifier→{@link ResourceLocation}；类别类型是 {@code mezz.jei.api.recipe.RecipeType}；</li>
 *   <li>1.20.1 的配方没有 RecipeHolder 包装，JEI 直接以 {@link CraftingRecipe} 为类型，
 *       配方 ID 用 {@code recipe.getId()}；</li>
 *   <li>Forge 1.20.1 会把**整张配方表**同步给客户端（不像 26.x 需要按类型点播），
 *       所以微型强子对撞的配方直接从客户端 {@link RecipeManager} 里取，无需缓存事件。</li>
 * </ul>
 * </p>
 */
@JeiPlugin
public class ZuoyanJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(Zuoyanmod.MODID, "jei");
    }

    @Override
    public void registerCategories(mezz.jei.api.registration.IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new MicroCollisionCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new VoidPumpCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(mezz.jei.api.registration.IRecipeRegistration registration) {
        // 虚空共振泵：硬编码逻辑（非数据驱动配方），注册期直接喂展示条目
        var pump = new VoidPumpCategory.VoidPumpDisplay[] {
                new VoidPumpCategory.VoidPumpDisplay(
                        new ItemStack(Items.ENDER_PEARL),
                        new ItemStack(ItemRegistry.DARK_MATTER_PARTICLE.get()),
                        VoidResonancePumpBlockEntity.fuelValue(new ItemStack(Items.ENDER_PEARL))),
                new VoidPumpCategory.VoidPumpDisplay(
                        new ItemStack(Items.DRAGON_BREATH),
                        new ItemStack(ItemRegistry.DARK_MATTER_PARTICLE.get()),
                        VoidResonancePumpBlockEntity.fuelValue(new ItemStack(Items.DRAGON_BREATH))),
                new VoidPumpCategory.VoidPumpDisplay(
                        new ItemStack(Items.CHORUS_FRUIT),
                        new ItemStack(ItemRegistry.DARK_MATTER_PARTICLE.get()),
                        VoidResonancePumpBlockEntity.fuelValue(new ItemStack(Items.CHORUS_FRUIT)))
        };
        registration.addRecipes(VoidPumpCategory.TYPE, List.of(pump));

        // 微型强子对撞：数据包配方。1.20.1 的客户端手持完整配方表，直接读即可。
        List<MicroCollisionRecipe> collisionRecipes = clientCollisionRecipes();
        if (!collisionRecipes.isEmpty()) {
            registration.addRecipes(MicroCollisionCategory.TYPE, collisionRecipes);
        }
    }

    /** 从客户端配方表里取全部微型强子对撞配方；客户端尚未连入时返回空表。 */
    private static List<MicroCollisionRecipe> clientCollisionRecipes() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return List.of();
        }
        RecipeManager manager = connection.getRecipeManager();
        if (manager == null) {
            return List.of();
        }
        return manager.getAllRecipesFor(RecipeRegistry.MICRO_COLLISION_TYPE.get());
    }

    @Override
    public void registerRecipeCatalysts(mezz.jei.api.registration.IRecipeCatalystRegistration registration) {
        // 对撞机方块 = 微型强子对撞配方的催化剂（JEI 里点方块看它能做什么）
        registration.addRecipeCatalyst(new ItemStack(ItemRegistry.MICRO_HADRON_COLLIDER_ITEM.get()),
                MicroCollisionCategory.TYPE);
        // 虚空共振泵 = 共振转化的催化剂
        registration.addRecipeCatalyst(new ItemStack(ItemRegistry.VOID_RESONANCE_PUMP_ITEM.get()),
                VoidPumpCategory.TYPE);
    }

    @Override
    public void registerGuiHandlers(mezz.jei.api.registration.IGuiHandlerRegistration registration) {
        // 对撞机界面里点进度束区域 = 打开 JEI 的微型强子对撞配方列表
        registration.addRecipeClickArea(org.gwfx.zuoyanmod.client.MicroHadronColliderScreen.class,
                84, 27, 30, 10, MicroCollisionCategory.TYPE);
        // 虚空泵界面里点产出进度条区域 = 打开 JEI 的共振转化列表
        registration.addRecipeClickArea(org.gwfx.zuoyanmod.client.VoidResonancePumpScreen.class,
                77, 28, 22, 9, VoidPumpCategory.TYPE);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new SpaceTransferHandler(), RecipeTypes.CRAFTING);
    }

    /**
     * 只负责"把意图发到服务端"，真正搬东西在服务端做。
     *
     * <p>为什么不在这里直接改容器：JEI 是在**客户端**调用 transferRecipe 的，客户端那份
     * 四维空间是空的（数据挂在玩家 capability 上、只存在于服务端）。客户端改了也没用，
     * 所以只能发一个包，让服务端去 {@link KleinBottleMenu#transfer} 里执行。</p>
     */
    private static final class SpaceTransferHandler
            implements IRecipeTransferHandler<KleinBottleMenu, CraftingRecipe> {

        @Override
        public Class<? extends KleinBottleMenu> getContainerClass() {
            return KleinBottleMenu.class;
        }

        @Override
        public Optional<MenuType<KleinBottleMenu>> getMenuType() {
            return Optional.of(MenuRegistry.KLEIN_BOTTLE_MENU.get());
        }

        @Override
        public RecipeType<CraftingRecipe> getRecipeType() {
            return RecipeTypes.CRAFTING;
        }

        // 六参版是接口里唯一能覆盖的抽象方法（context 版是 default，会转调到这里）
        @Override
        public IRecipeTransferError transferRecipe(KleinBottleMenu container,
                                                  CraftingRecipe recipe,
                                                  IRecipeSlotsView recipeSlots,
                                                  Player player,
                                                  boolean maxTransfer,
                                                  boolean doTransfer) {
            if (!doTransfer) {
                // 试算：客户端拿不到四维空间的内容，无法判断够不够，一律放行让按钮显示
                return null;
            }
            PacketHandler.sendCraftingTransfer(recipe.getId(), maxTransfer);
            return null;
        }
    }
}
