package org.gwfx.zuoyanmod.recipe;

import com.google.gson.JsonObject;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * 微型强子对撞配方（type: zuoyanmod:micro_collision）。
 * 数据驱动：输入 A / 输入 B / 产物 / 对撞时长（ticks）全部来自 DataPack JSON，
 * 逻辑层（对撞机 BlockEntity）只问「这两格材料能撞出什么、要撞多久」，不写死任何配方。
 *
 * <p>对撞是「双束流」语义：A、B 两束粒子地位对称，因此匹配时 A/B 顺序无关。</p>
 *
 * <pre>
 * {
 *   "type": "zuoyanmod:micro_collision",
 *   "ingredient_a": "minecraft:echo_shard",
 *   "ingredient_b": "minecraft:netherite_ingot",
 *   "result": { "item": "zuoyanmod:singularity_core", "count": 1 },
 *   "duration": 200
 * }
 * </pre>
 *
 * <p>1.20.1 适配：{@code Recipe<C extends Container>} 的接口与 26.x 完全不同
 * （matches/assemble/getResultItem/canCraftInDimensions + 显式 id），
 * 输入视图 {@link CollisionInput} 因此实现 {@link Container} 而不是 26.x 的 RecipeInput。
 * JSON 的 result 用 1.20.1 的 {@code {"item": ...}} 形式。</p>
 */
public class MicroCollisionRecipe implements Recipe<MicroCollisionRecipe.CollisionInput> {

    /** 数据包 JSON 里的配方 id（1.20.1 的 Recipe 接口要求自带 id）。 */
    private final ResourceLocation id;
    private final Ingredient inputA;
    private final Ingredient inputB;
    private final ItemStack result;
    private final int duration;

    public MicroCollisionRecipe(ResourceLocation id, Ingredient inputA, Ingredient inputB, ItemStack result, int duration) {
        this.id = id;
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

    public Ingredient inputA() {
        return this.inputA;
    }

    public Ingredient inputB() {
        return this.inputB;
    }

    /** 仅展示用（JEI 类别等）：不关心输入，直接产出展示栈。 */
    public ItemStack resultDisplay() {
        return this.result.copy();
    }

    /**
     * 双束流匹配：A/B 槽位顺序无关（对撞机里两束粒子交换方向物理结果相同）。
     * 输入槽是「一束粒子」，整组材料都参与对撞（消耗时每束各扣 1 个）。
     */
    @Override
    public boolean matches(CollisionInput input, Level level) {
        return (this.inputA.test(input.getItem(0)) && this.inputB.test(input.getItem(1)))
                || (this.inputA.test(input.getItem(1)) && this.inputB.test(input.getItem(0)));
    }

    @Override
    public ItemStack assemble(CollisionInput input, RegistryAccess registryAccess) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return this.result.copy();
    }

    /** 让 JEI / 配方书能列出这份配方的两个材料。 */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(this.inputA);
        list.add(this.inputB);
        return list;
    }

    /** 机器配方不进配方书，标记为特殊配方。 */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.MICRO_COLLISION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeRegistry.MICRO_COLLISION_TYPE.get();
    }

    // ===== 输入视图：对撞机的两格材料 =====

    /**
     * 两束粒子的只读视图，实现 1.20.1 的 {@link Container} 以复用原版配方查询管线。
     * 只读：所有写方法都是空实现，匹配阶段不会被调用。
     */
    public static final class CollisionInput implements Container {

        private final ItemStack beamA;
        private final ItemStack beamB;

        public CollisionInput(ItemStack beamA, ItemStack beamB) {
            this.beamA = beamA;
            this.beamB = beamB;
        }

        @Override
        public int getContainerSize() {
            return 2;
        }

        @Override
        public boolean isEmpty() {
            return this.beamA.isEmpty() && this.beamB.isEmpty();
        }

        @Override
        public ItemStack getItem(int index) {
            return switch (index) {
                case 0 -> this.beamA;
                case 1 -> this.beamB;
                default -> throw new IndexOutOfBoundsException("CollisionInput has 2 beams, got index " + index);
            };
        }

        @Override
        public ItemStack removeItem(int index, int amount) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            // 只读视图
        }

        @Override
        public void setChanged() {
            // 只读视图
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            // 只读视图
        }
    }

    // ===== 序列化 =====

    /** 数据层：DataPack JSON → 配方对象。duration 默认 100 ticks（5 秒）。 */
    public static class Serializer implements RecipeSerializer<MicroCollisionRecipe> {

        public static final String INGREDIENT_A = "ingredient_a";
        public static final String INGREDIENT_B = "ingredient_b";
        public static final String RESULT = "result";
        public static final String DURATION = "duration";

        @Override
        public MicroCollisionRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient a = Ingredient.fromJson(json.get(INGREDIENT_A));
            Ingredient b = Ingredient.fromJson(json.get(INGREDIENT_B));
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, RESULT));
            int duration = GsonHelper.getAsInt(json, DURATION, 100);
            return new MicroCollisionRecipe(id, a, b, result, duration);
        }

        @Override
        public MicroCollisionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient a = Ingredient.fromNetwork(buf);
            Ingredient b = Ingredient.fromNetwork(buf);
            ItemStack result = buf.readItem();
            int duration = buf.readVarInt();
            return new MicroCollisionRecipe(id, a, b, result, duration);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, MicroCollisionRecipe recipe) {
            recipe.inputA().toNetwork(buf);
            recipe.inputB().toNetwork(buf);
            buf.writeItem(recipe.getResultItem(null));
            buf.writeVarInt(recipe.duration());
        }
    }
}
