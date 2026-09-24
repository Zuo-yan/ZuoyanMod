package org.gwfx.zuoyanmod.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import org.gwfx.zuoyanmod.Zuoyanmod;

/**
 * 自定义配方注册：RecipeType + RecipeSerializer。
 *
 * <p>1.20.1 适配：{@code RecipeType} 用 {@code RecipeType.simple(ResourceLocation)} 造匿名实例
 * （不落注册表，与原版 CraftingRecipe 的做法一致）；{@code RecipeSerializer} 是接口，
 * 需要自己写 {@link MicroCollisionRecipe.Serializer}（26.x 的 serializer 是 record，
 * 可以直接由 MapCodec + StreamCodec 构造）。</p>
 */
public final class RecipeRegistry {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Zuoyanmod.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Zuoyanmod.MODID);

    // ===== 微型强子对撞 =====

    public static final RegistryObject<RecipeType<MicroCollisionRecipe>> MICRO_COLLISION_TYPE =
            RECIPE_TYPES.register("micro_collision",
                    () -> RecipeType.simple(new ResourceLocation(Zuoyanmod.MODID, "micro_collision")));

    public static final RegistryObject<RecipeSerializer<MicroCollisionRecipe>> MICRO_COLLISION_SERIALIZER =
            RECIPE_SERIALIZERS.register("micro_collision", MicroCollisionRecipe.Serializer::new);

    private RecipeRegistry() {}

    public static void register(IEventBus modBus) {
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
    }
}
