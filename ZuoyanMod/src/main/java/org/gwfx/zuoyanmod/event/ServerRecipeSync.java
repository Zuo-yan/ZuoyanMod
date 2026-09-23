package org.gwfx.zuoyanmod.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.recipe.RecipeRegistry;

/**
 * 服务端：请求把自定义配方类型同步给客户端。
 *
 * <p>26.3 的配方同步是<b>按需</b>的：服务端只把 {@link OnDatapackSyncEvent#sendRecipes}
 * 显式请求过的 {@code RecipeType} 发给客户端——见 {@code RecipeContentPayload.create} 的
 * 「空集合快路径」：没有任何 mod 请求时直接发空列表。不在这里请求，客户端
 * {@code RecipesReceivedEvent} 拿到的 {@code RecipeMap} 就是空的，JEI 的微型强子对撞
 * 类别自然永远没有配方可显示。
 *
 * <p>这是此前三版 JEI 方案（registerRecipes 推送 / runtime.addRecipes / ISimpleRecipeManagerPlugin）
 * 全部「静默失败」的真正根因：配方根本没同步到客户端，跟时序、类别可见性都无关。
 *
 * <p>本事件发在服务端 {@code NeoForge.EVENT_BUS}（玩家加入 / 数据包 reload 时，见
 * {@code PlayerList} 的 post 点），客户端不会收到，无需 {@code Dist} 限制。
 */
@EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class ServerRecipeSync {

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(RecipeRegistry.MICRO_COLLISION_TYPE.get());
    }

    private ServerRecipeSync() {}
}
