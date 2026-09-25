package org.gwfx.zuoyanmod.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.registries.RegistryObject;
import org.gwfx.zuoyanmod.Zuoyanmod;

/**
 * 旧注册 id 的迁移表。
 *
 * <p>Forge 的 {@link MissingMappingsEvent} 是「存档里有一个 id，但当前注册表里没有」时的
 * 补救入口。我们用它把<b>已经改名</b>的注册项接到新 id 上，让旧存档里的物品不会凭空消失。
 *
 * <h2>为什么 1.20.1 分支需要、26.3 主线不需要</h2>
 * 主线与本分支都会随主线同步改掉一部分注册 id（例如这次的 {@code ice_tea → chocolate_crisp}，
 * 因为显示名一直是「巧乐兹」，id 才是对不上的那个）。区别在于：
 * <ul>
 *   <li>26.3 主线是<b>开发中</b>的版本，用户存档本来就是随版本一起重建的；</li>
 *   <li>1.20.1 分支是可以长期停在某个整合包里的版本，玩家的箱子、潜影盒、物品展示框里
 *       可能真的有旧 id 的物品。id 一改，这些物品在加载时会被<b>静默丢弃</b>
 *       （原版行为是发一条 "Missing mapping" 警告然后移除），玩家只会发现东西不见了。</li>
 * </ul>
 * 所以这里补一张显式的迁移表 —— 代价只有几行，收益是「同步 id 改名不再有数据损失」。
 * 以后再有注册项改名，往 {@link #register} 里加一行即可。
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LegacyIdRemapper {

    private LegacyIdRemapper() {}

    /**
     * 旧 id → 新注册项。
     *
     * <p>用 {@code RegistryObject} 而不是直接拿物品实例：注册表在 mod 构造期才填好，
     * 而本方法只在「真的有人在找旧 id」时才被调用，那时 {@code .get()} 一定已经可用。
     */
    private static void register() {
        remapItem("ice_tea", ItemRegistry.CHOCOLATE_CRISP);
    }

    private static void remapItem(String legacyPath, RegistryObject<? extends Item> target) {
        ResourceLocation legacy = new ResourceLocation(Zuoyanmod.MODID, legacyPath);
        MissingMappingsEvent.Mapping<Item> mapping = MAPPINGS.get(legacy);
        if (mapping != null) {
            mapping.remap(target.get());
        }
    }

    /** 本轮事件里所有「找不到的 item id」按 id 索引，避免每次线性查找。 */
    private static final java.util.Map<ResourceLocation, MissingMappingsEvent.Mapping<Item>> MAPPINGS =
            new java.util.HashMap<>();

    @SubscribeEvent
    public static void onMissingMappings(MissingMappingsEvent event) {
        MAPPINGS.clear();
        for (MissingMappingsEvent.Mapping<Item> mapping : event.getMappings(
                net.minecraft.core.registries.Registries.ITEM, Zuoyanmod.MODID)) {
            MAPPINGS.put(mapping.getKey(), mapping);
        }
        if (!MAPPINGS.isEmpty()) {
            register();
        }
        MAPPINGS.clear();
    }
}
