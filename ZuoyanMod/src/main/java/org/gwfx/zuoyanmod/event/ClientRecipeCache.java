package org.gwfx.zuoyanmod.event;

import java.util.Collection;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * 客户端配方入口（1.21.1）。
 *
 * <p>1.21.1 的服务端在玩家登录与 datapack 重载时自动把<b>全部</b>配方同步到客户端
 * （ClientboundUpdateRecipesPacket），客户端 {@code level.getRecipeManager()} 里就是
 * 完整配方表——26.x 那套 RecipesReceivedEvent + RecipeMap 自建缓存通道已不需要
 * （原 {@code ServerRecipeSync} 一并停用）。这里收口成一个静态入口，JEI 插件从这拿配方。
 */
public final class ClientRecipeCache {

    /** 客户端已同步的某类型全部配方；未进世界时返回空列表。 */
    public static <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> byType(RecipeType<T> type) {
        RecipeManager manager = recipeManager();
        return manager == null ? List.of() : manager.getAllRecipesFor(type);
    }

    private static RecipeManager recipeManager() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? null : minecraft.level.getRecipeManager();
    }

    private ClientRecipeCache() {}
}
