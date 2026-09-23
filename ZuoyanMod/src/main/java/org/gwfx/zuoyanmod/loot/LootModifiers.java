package org.gwfx.zuoyanmod.loot;

import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.gwfx.zuoyanmod.Zuoyanmod;

/**
 * 全局战利品修改器（GLM）注册表。
 *
 * <p>Forge 1.20.1 的 GLM 是 Codec 注册制：注册到
 * {@link ForgeRegistries#GLOBAL_LOOT_MODIFIER_SERIALIZERS}（元素类型是
 * {@code Codec<? extends IGlobalLootModifier>}）。数据侧用
 * {@code data/forge/loot_modifiers/global_loot_modifiers.json} 列出启用的修饰器 ID。
 */
public final class LootModifiers {

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Zuoyanmod.MODID);

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final RegistryObject<Codec<AddTableLootModifier>> ADD_TABLE =
            (RegistryObject) LOOT_MODIFIERS.register("add_table", () -> AddTableLootModifier.CODEC);

    private LootModifiers() {}
}
