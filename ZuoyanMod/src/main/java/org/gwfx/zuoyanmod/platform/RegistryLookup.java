package org.gwfx.zuoyanmod.platform;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

/**
 * 版本适配层：内置注册表查询。
 *
 * <p>「按注册 ID 查一个东西」在各版本间的返回类型变过多次：
 * 1.20.1 直接返回可空实体（null 表示不存在），26.3 返回的是
 * {@code Optional<Holder.Reference>}。这里集中封装，调用方只面对
 * {@code Optional<Item>}，跨版本迁移时只改本文件。
 */
public final class RegistryLookup {

    private RegistryLookup() {}

    /** 1.20.1：注册表按 ID 直接返回实体，查不到为 null */
    public static Optional<Item> item(ResourceLocation id) {
        return Optional.ofNullable(ForgeRegistries.ITEMS.getValue(id));
    }

    /** 注册表里是否存在该 ID 的物品（id 为 null 时返回 false） */
    public static boolean hasItem(ResourceLocation id) {
        return id != null && ForgeRegistries.ITEMS.containsKey(id);
    }

    /** 物品的注册 ID（查询/显示用；查不到返回 null，1.20.1 与 26.x 语义一致） */
    public static ResourceLocation itemId(Item item) {
        return ForgeRegistries.ITEMS.getKey(item);
    }

    /** 注册表的数字 id（注册名排序用，同模组的东西天然连在一起）。1.20.1 走 Item.getId 静态方法 */
    public static int itemNumericId(Item item) {
        return Item.getId(item);
    }
}
