package org.gwfx.zuoyanmod.client.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.block.VoidResonancePumpBlockEntity;
import org.gwfx.zuoyanmod.event.ClientRecipeCache;
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
 * 在 JEI 里点配方右侧的 +，材料直接从网络里填进网格，不用先手动把东西从终端搬进背包。
 *
 * <p>这个类只在装了 JEI 时才会被加载（JEI 通过注解扫描发现插件），
 * 所以 JEI 是 compileOnly 依赖也能安全留在这——没装 JEI 的客户端永远不会碰到它。
 */
@JeiPlugin
public class ZuoyanJeiPlugin implements IModPlugin {

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "jei");
    }

    /**
     * 隐藏 JEI 的「方块标签」信息类别（{@code minecraft:tag_recipes/block}）。
     *
     * <p>成因：机器方块带 {@code mineable/pickaxe} + {@code needs_iron_tool}，原版自动把它
     * 归入 {@code incorrect_for_wooden_tool} 等一堆标签，JEI 就为每个标签生成一页"方块标签"
     * 信息页，查机器时排在配方前面、且毫无信息量。
     *
     * <p>JEI 的标签类别是全局的，没有按物品隐藏的 API，只能整类隐藏——查任何方块
     * 都不再出现"方块标签"页（「物品标签」「流体标签」两个类别不受影响）。
     */
    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
        runtime.getRecipeManager()
                .getRecipeType(Identifier.fromNamespaceAndPath("minecraft", "tag_recipes/block"))
                .ifPresent(type -> runtime.getRecipeManager().hideRecipeCategory(type));
    }

    @Override
    public void registerCategories(mezz.jei.api.registration.IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new MicroCollisionCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new VoidPumpCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(mezz.jei.api.registration.IRecipeRegistration registration) {
        // 虚空共振泵：硬编码逻辑（非数据驱动配方），注册期直接喂展示条目
        var pump = new org.gwfx.zuoyanmod.client.jei.VoidPumpCategory.VoidPumpDisplay[] {
                new org.gwfx.zuoyanmod.client.jei.VoidPumpCategory.VoidPumpDisplay(
                        new ItemStack(Items.ENDER_PEARL),
                        new ItemStack(ItemRegistry.DARK_MATTER_PARTICLE.get()),
                        VoidResonancePumpBlockEntity.fuelValue(new ItemStack(Items.ENDER_PEARL))),
                new org.gwfx.zuoyanmod.client.jei.VoidPumpCategory.VoidPumpDisplay(
                        new ItemStack(Items.DRAGON_BREATH),
                        new ItemStack(ItemRegistry.DARK_MATTER_PARTICLE.get()),
                        VoidResonancePumpBlockEntity.fuelValue(new ItemStack(Items.DRAGON_BREATH))),
                new org.gwfx.zuoyanmod.client.jei.VoidPumpCategory.VoidPumpDisplay(
                        new ItemStack(Items.CHORUS_FRUIT),
                        new ItemStack(ItemRegistry.DARK_MATTER_PARTICLE.get()),
                        VoidResonancePumpBlockEntity.fuelValue(new ItemStack(Items.CHORUS_FRUIT)))
        };
        registration.addRecipes(VoidPumpCategory.TYPE, List.of(pump));

        // 微型强子对撞：数据包配方，与虚空泵同一套「注册期 addRecipes」机制。
        // 之所以能在这里（而不是等 runtime）直接读到配方：服务端已通过 ServerRecipeSync
        // 的 OnDatapackSyncEvent#sendRecipes 请求同步 micro_collision 类型；而 JEI 的启动
        // 观察者用 LOWEST 优先级监听 RecipesReceivedEvent，晚于我们的 HIGHEST 缓存，
        // 故本方法执行时 ClientRecipeCache 里已有配方。
        List<RecipeHolder<MicroCollisionRecipe>> collisionRecipes =
                List.copyOf(ClientRecipeCache.getRecipes().byType(RecipeRegistry.MICRO_COLLISION_TYPE.get()));
        if (!collisionRecipes.isEmpty()) {
            registration.addRecipes(MicroCollisionCategory.TYPE, collisionRecipes);
        }
    }

    @Override
    public void registerRecipeCatalysts(mezz.jei.api.registration.IRecipeCatalystRegistration registration) {
        // 对撞机方块 = 微型强子对撞配方的催化剂（JEI 里点方块看它能做什么）
        registration.addCraftingStation(MicroCollisionCategory.TYPE,
                new ItemStack(ItemRegistry.MICRO_HADRON_COLLIDER_ITEM.get()));
        // 虚空共振泵 = 共振转化的催化剂（点泵方块看它能换什么）
        registration.addCraftingStation(VoidPumpCategory.TYPE,
                new ItemStack(ItemRegistry.VOID_RESONANCE_PUMP_ITEM.get()));
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
     * 四维空间是空的（数据挂在玩家 attachment 上、只存在于服务端）。客户端改了也没用，
     * 所以只能发一个包，让服务端去 {@link KleinBottleMenu#transfer} 里执行。
     */
    private static final class SpaceTransferHandler
            implements IRecipeTransferHandler<KleinBottleMenu, RecipeHolder<CraftingRecipe>> {

        @Override
        public Class<? extends KleinBottleMenu> getContainerClass() {
            return KleinBottleMenu.class;
        }

        @Override
        public Optional<MenuType<KleinBottleMenu>> getMenuType() {
            return Optional.of(MenuRegistry.KLEIN_BOTTLE_MENU.get());
        }

        @Override
        public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
            return RecipeTypes.CRAFTING;
        }

        // 六参版是接口里唯一能覆盖的抽象方法（context 版是 default，会转调到这里）
        @SuppressWarnings("removal")
        @Override
        public IRecipeTransferError transferRecipe(KleinBottleMenu container,
                                                  RecipeHolder<CraftingRecipe> recipe,
                                                  IRecipeSlotsView recipeSlots,
                                                  Player player,
                                                  boolean maxTransfer,
                                                  boolean doTransfer) {
            if (!doTransfer) {
                // 试算：客户端拿不到四维空间的内容，无法判断够不够，一律放行让按钮显示
                return null;
            }
            PacketHandler.sendCraftingTransfer(recipe.id().identifier(), maxTransfer);
            return null;
        }
    }
}
