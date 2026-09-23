package org.gwfx.zuoyanmod.event;

import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;

/**
 * 客户端配方缓存。
 * 26.3 里客户端 {@code level.recipeAccess()} 返回的是 ClientRecipeContainer——
 * 只含属性集与切石机配方，**没有完整配方表**（强转 RecipeManager 会 ClassCastException）。
 * 配方同步的正路是 NeoForge 的 RecipesReceivedEvent：服务器发配方包时携带完整 RecipeMap。
 *
 * <p>注意：该事件携带的 RecipeMap 只包含<b>服务端通过
 * {@link net.neoforged.neoforge.event.OnDatapackSyncEvent#sendRecipes} 请求过的类型</b>——
 * 本 mod 已在 {@link ServerRecipeSync} 请求了 micro_collision，这里才能缓存到它。
 *
 * <p>HIGHEST 优先级：JEI 的启动观察者（{@code StartEventObserver}）用 LOWEST 监听同一事件，
 * 因此本缓存在 JEI 调用 {@code registerRecipes} 之前就已就位。
 */
@EventBusSubscriber(modid = Zuoyanmod.MODID, value = Dist.CLIENT)
public final class ClientRecipeCache {

    private static volatile RecipeMap recipes = RecipeMap.EMPTY;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRecipesReceived(RecipesReceivedEvent event) {
        recipes = event.getRecipeMap();
    }

    public static RecipeMap getRecipes() {
        return recipes;
    }

    private ClientRecipeCache() {}
}
