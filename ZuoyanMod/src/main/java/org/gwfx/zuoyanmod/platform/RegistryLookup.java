package org.gwfx.zuoyanmod.platform;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.Optional;

/**
 * 版本适配层：内置注册表查询。
 *
 * <p>「按注册 ID 查一个东西」在各版本间的返回类型变过多次：
 * 老版本直接返回实体或 {@code Holder}，26.3 返回的是
 * {@code Optional<Holder.Reference>}——要用两次解包才能拿到真正的对象。
 * 这里集中封装，调用方只面对 {@code Optional<Item>}，跨版本迁移时只改本文件。
 */
public final class RegistryLookup {

    private RegistryLookup() {}

    /** 适配 26.3：BuiltInRegistries 按 ID 返回 Optional&lt;Holder.Reference&gt;，解包后才是具体 Item */
    public static Optional<Item> item(Identifier id) {
        return BuiltInRegistries.ITEM.get(id).map(Holder.Reference::value);
    }

    /** 注册表里是否存在该 ID 的物品（id 为 null 时返回 false） */
    public static boolean hasItem(Identifier id) {
        return id != null && BuiltInRegistries.ITEM.containsKey(id);
    }
}
