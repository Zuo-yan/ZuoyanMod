package org.gwfx.zuoyanmod.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.gwfx.zuoyanmod.Zuoyanmod;

/**
 * 自定义配方注册：RecipeType + RecipeSerializer。
 * 26.3 的 RecipeSerializer 是 record(MapCodec 数据层, StreamCodec 网络层)，直接构造；
 * RecipeType 用 RecipeType.simple(Identifier) 包一个实例。
 */
public final class RecipeRegistry {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Zuoyanmod.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Zuoyanmod.MODID);

    // ===== 微型强子对撞 =====

    public static final DeferredHolder<RecipeType<?>, RecipeType<MicroCollisionRecipe>> MICRO_COLLISION_TYPE =
            RECIPE_TYPES.register("micro_collision",
                    () -> RecipeType.simple(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "micro_collision")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MicroCollisionRecipe>> MICRO_COLLISION_SERIALIZER =
            RECIPE_SERIALIZERS.register("micro_collision",
                    () -> new RecipeSerializer<>(MicroCollisionRecipe.MAP_CODEC, MicroCollisionRecipe.STREAM_CODEC));

    private RecipeRegistry() {}

    public static void register(IEventBus modBus) {
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
    }
}
