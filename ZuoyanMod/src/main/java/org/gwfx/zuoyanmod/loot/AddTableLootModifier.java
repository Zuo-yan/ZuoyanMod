package org.gwfx.zuoyanmod.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/**
 * 「往目标战利品表的结果里追加另一张表」的全局战利品修改器（GLM）。
 *
 * <p>26.x 用的是 NeoForge 内置的 {@code neoforge:add_table}；Forge 1.20.1 没有
 * {@code GlobalLootModifierSerializer} 类——它的 GLM 是 **Codec 注册制**：
 * 修饰器实现 {@link IGlobalLootModifier#codec()}，把 Codec 注册进
 * {@code ForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS}（见 {@link LootModifiers}），
 * JSON 里 {@code "type": "zuoyanmod:add_table"}。
 *
 * <p>行为与 NeoForge 版一致：conditions 命中（通常是 {@code forge:loot_table_id}，
 * 即"这次开的宝箱是某张表"）时，把 {@code table} 指向的表 roll 一遍并追加进掉落列表。
 * 启用哪些修饰器写在 {@code data/forge/loot_modifiers/global_loot_modifiers.json}。
 */
public class AddTableLootModifier extends LootModifier {

    public static final Codec<AddTableLootModifier> CODEC = RecordCodecBuilder.create(inst ->
            codecStart(inst)
                    .and(ResourceLocation.CODEC.fieldOf("table").forGetter(m -> m.table))
                    .apply(inst, AddTableLootModifier::new));

    private final ResourceLocation table;

    public AddTableLootModifier(LootItemCondition[] conditions, ResourceLocation table) {
        super(conditions);
        this.table = table;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        // conditions 的判定由父类 LootModifier#apply 完成，走到这里说明已命中。
        // 1.20.1 的 LootContext 没有 getLootTable，要从服务器的 LootData 里取。
        context.getLevel().getServer().getLootData().getLootTable(table)
                .getRandomItems(context, generatedLoot::add);
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
