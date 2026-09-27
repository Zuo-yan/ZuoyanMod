package org.gwfx.zuoyanmod.platform;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Optional;

/**
 * 版本适配层：内置注册表查询。
 *
 * <p>「按注册 ID 查一个东西」在各版本间的返回类型变过多次：
 * 1.21.1 的 {@code Registry#get} 直接返回可空实体（查不到返回 null），
 * 26.x 改成了 {@code Optional<Holder.Reference>}。这里集中封装，
 * 调用方只面对 {@code Optional<Item>}，跨版本迁移时只改本文件。
 */
public final class RegistryLookup {

    private RegistryLookup() {}

    /** 适配 1.21.1：Registry#get 按 ID 直接返回可空 Item，包一层 Optional */
    public static Optional<Item> item(ResourceLocation id) {
        return Optional.ofNullable(BuiltInRegistries.ITEM.get(id));
    }

    /** 注册表里是否存在该 ID 的物品（id 为 null 时返回 false） */
    public static boolean hasItem(ResourceLocation id) {
        return id != null && BuiltInRegistries.ITEM.containsKey(id);
    }
}
