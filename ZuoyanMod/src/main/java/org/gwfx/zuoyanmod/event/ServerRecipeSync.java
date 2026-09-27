package org.gwfx.zuoyanmod.event;

/**
 * 服务端：自定义配方的客户端同步。
 *
 * <p>1.21.1 不再需要 26.3 那种「按需请求同步」：原版 {@code PlayerList} 在玩家加入与
 * 数据包 reload 时会自动把 {@code RecipeManager} 里<b>全部</b>配方
 * （含 {@code zuoyanmod:micro_collision} 等自定义类型）通过
 * {@code ClientboundUpdateRecipesPacket} 发给客户端。配方注册表本身没有任何
 * 「哪类要同步」的开关，因此这里没有需要订阅的事件——保留本类作为该结论的落点，
 * 客户端（{@code ClientRecipeCache} / JEI 插件）直接读同步到的配方即可。
 */
public final class ServerRecipeSync {

    private ServerRecipeSync() {}
}
