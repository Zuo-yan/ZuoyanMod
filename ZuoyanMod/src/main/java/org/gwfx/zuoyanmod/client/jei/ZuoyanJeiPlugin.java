package org.gwfx.zuoyanmod.client.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.menu.KleinBottleMenu;
import org.gwfx.zuoyanmod.menu.MenuRegistry;
import org.gwfx.zuoyanmod.network.PacketHandler;

import java.util.Optional;

/**
 * JEI 联动：让「四维空间」终端的**内嵌 3×3 工作台**能从玩家的四维空间直接抓料。
 *
 * <p>这就是 RS2 的合成网格 / AE2 的合成终端那套关系：终端里本来就摆着 3×3 网格和产物槽，
 * 在 JEI 里点配方右侧的 +，材料直接从网络里填进网格，不用先手动把东西从终端搬进背包。
 *
 * <p>这个类只在装了 JEI 时才会被加载（JEI 通过注解扫描发现插件），
 * 所以 JEI 是 compileOnly 依赖也能安全留在这——没装 JEI 的客户端永远不会碰到它。
 *
 * <p>1.20.1 适配：Identifier→{@link ResourceLocation}；1.20.1 的配方没有
 * RecipeHolder 包装，JEI 直接以 {@link CraftingRecipe} 为类型，配方 ID 用 {@code recipe.getId()}。
 */
@JeiPlugin
public class ZuoyanJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(Zuoyanmod.MODID, "jei");
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
     * 所以只能发一个包，让服务端去 {@link KleinBottleMenu#transfer} 里执行。
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
