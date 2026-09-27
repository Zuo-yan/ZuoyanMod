package org.gwfx.zuoyanmod.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 微型强子对撞配方（type: zuoyanmod:micro_collision）。
 * 数据驱动：输入 A / 输入 B / 产物 / 对撞时长（ticks）全部来自 DataPack JSON，
 * 逻辑层（对撞机 BlockEntity）只问「这两格材料能撞出什么、要撞多久」，不写死任何配方。
 *
 * 对撞是「双束流」语义：A、B 两束粒子地位对称，因此匹配时 A/B 顺序无关。
 * JSON 示例：
 * <pre>
 * {
 *   "type": "zuoyanmod:micro_collision",
 *   "ingredient_a": "minecraft:echo_shard",
 *   "ingredient_b": "minecraft:netherite_ingot",
 *   "result": { "id": "zuoyanmod:singularity_core", "count": 1 },
 *   "duration": 200
 * }
 * </pre>
 */
public class MicroCollisionRecipe implements Recipe<MicroCollisionRecipe.CollisionInput> {

    /** 数据层：DataPack JSON → 配方对象。duration 默认 100 ticks（5 秒）。 */
    public static final MapCodec<MicroCollisionRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(MicroCollisionRecipe::getGroup),
                    Ingredient.CODEC.fieldOf("ingredient_a").forGetter(MicroCollisionRecipe::inputA),
                    Ingredient.CODEC.fieldOf("ingredient_b").forGetter(MicroCollisionRecipe::inputB),
                    ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
                    Codec.INT.optionalFieldOf("duration", 100).forGetter(MicroCollisionRecipe::duration)
            ).apply(i, MicroCollisionRecipe::new)
    );

    /** 网络层：服务端 → 客户端同步（配方书/JEI 等客户端消费方）。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, MicroCollisionRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, MicroCollisionRecipe::getGroup,
            Ingredient.CONTENTS_STREAM_CODEC, MicroCollisionRecipe::inputA,
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.inputB,
            ItemStack.STREAM_CODEC, r -> r.result,
            ByteBufCodecs.VAR_INT, MicroCollisionRecipe::duration,
            MicroCollisionRecipe::new
    );

    private final String group;
    private final Ingredient inputA;
    private final Ingredient inputB;
    private final ItemStack result;
    private final int duration;

    public MicroCollisionRecipe(String group, Ingredient inputA, Ingredient inputB, ItemStack result, int duration) {
        this.group = group;
        this.inputA = inputA;
        this.inputB = inputB;
        this.result = result;
        this.duration = duration;
    }

    // ===== 配方语义 =====

    /** 对撞时长（ticks）。设计区间 100~200（5~10 秒），由 JSON 自由配置。 */
    public int duration() {
        return this.duration;
    }

    /**
     * 双束流匹配：A/B 槽位顺序无关（对撞机里两束粒子交换方向物理结果相同）。
     * 输入槽是「一束粒子」，整组材料都参与对撞（消耗时每束各扣 1 个）。
     */
    @Override
    public boolean matches(CollisionInput input, Level level) {
        return (this.inputA.test(input.beamA()) && this.inputB.test(input.beamB()))
                || (this.inputA.test(input.beamB()) && this.inputB.test(input.beamA()));
    }

    @Override
    public ItemStack assemble(CollisionInput input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    /** 仅展示用（JEI 类别等）：不关心输入，直接产出展示栈。 */
    public ItemStack resultDisplay() {
        return this.result.copy();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, this.inputA, this.inputB);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    public Ingredient inputA() {
        return this.inputA;
    }

    public Ingredient inputB() {
        return this.inputB;
    }

    // ===== Recipe 接口样板 =====

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<? extends Recipe<CollisionInput>> getSerializer() {
        return RecipeRegistry.MICRO_COLLISION_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<CollisionInput>> getType() {
        return RecipeRegistry.MICRO_COLLISION_TYPE.get();
    }

    // ===== 输入视图：对撞机的两格材料 =====

    /** 两束粒子的只读视图，实现原版 RecipeInput 以复用配方查询管线。 */
    public record CollisionInput(ItemStack beamA, ItemStack beamB) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return switch (index) {
                case 0 -> this.beamA;
                case 1 -> this.beamB;
                default -> throw new IndexOutOfBoundsException("CollisionInput has 2 beams, got index " + index);
            };
        }

        @Override
        public int size() {
            return 2;
        }
    }

    /** 1.21.1 的 RecipeSerializer 是「codec() + streamCodec()」接口，需要自己实现（对照 ShapelessRecipe.Serializer）。 */
    public static class Serializer implements RecipeSerializer<MicroCollisionRecipe> {
        @Override
        public MapCodec<MicroCollisionRecipe> codec() {
            return MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MicroCollisionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
