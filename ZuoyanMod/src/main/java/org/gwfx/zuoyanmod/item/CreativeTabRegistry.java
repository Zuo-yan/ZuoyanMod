package org.gwfx.zuoyanmod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.gwfx.zuoyanmod.Zuoyanmod;

public final class CreativeTabRegistry {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Zuoyanmod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ZUOYAN_TAB =
            CREATIVE_MODE_TABS.register("zuoyan", () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> new ItemStack(ItemRegistry.KLEIN_BOTTLE.get()))
                    .title(Component.literal("过往浮现"))
                    .displayItems((parameters, output) -> {
                        // 武器与法宝
                        output.accept(ItemRegistry.BEIMING_BLADE.get());
                        output.accept(ItemRegistry.HERCULES_BOW.get());
                        output.accept(ItemRegistry.JACK_THE_RIPPER_SCALPEL.get());
                        output.accept(ItemRegistry.DEATH_NOTE.get());
                        output.accept(ItemRegistry.MING_DAO_SI_MING.get());
                        output.accept(ItemRegistry.DOMAIN_EXPANSION.get());
                        output.accept(ItemRegistry.SPACE_ANCHOR.get());

                        // 紫金神装四件套
                        output.accept(ItemRegistry.SHADOW_HELMET.get());
                        output.accept(ItemRegistry.SHENG_TIAN_CHESTPLATE.get());
                        output.accept(ItemRegistry.WIND_LEGGINGS.get());
                        output.accept(ItemRegistry.WALKER_BOOTS.get());

                        // 饰品（放入背包即生效）
                        output.accept(ItemRegistry.RING_OF_KILLS.get());
                        output.accept(ItemRegistry.WAN_HUI_RING.get());
                        output.accept(ItemRegistry.COUNTER_BELT.get());
                        output.accept(ItemRegistry.VOODOO_NECKLACE.get());
                        output.accept(ItemRegistry.YEMENGADE_VENOM_FANG.get());

                        // 补给饮品
                        output.accept(ItemRegistry.ICE_TEA.get());
                        output.accept(ItemRegistry.SPRITE_DRINK.get());

                        // 紫金材料与方块
                        output.accept(ItemRegistry.RAW_VIOLET_GOLD.get());
                        output.accept(ItemRegistry.VIOLET_GOLD_INGOT.get());
                        output.accept(ItemRegistry.VIOLET_GOLD_ORE_ITEM.get());
                        output.accept(ItemRegistry.DEEPSLATE_VIOLET_GOLD_ORE_ITEM.get());
                        output.accept(ItemRegistry.VIOLET_GOLD_BLOCK_ITEM.get());
                        output.accept(ItemRegistry.RELIC_CORE.get());
                        output.accept(ItemRegistry.HALLOWED_UPGRADE_SMITHING_TEMPLATE.get());

                        // 虚空共振泵与暗物质链
                        output.accept(ItemRegistry.VOID_RESONANCE_PUMP_ITEM.get());
                        output.accept(ItemRegistry.DARK_MATTER_PARTICLE.get());

                        // 超流体暗物质
                        output.accept(ItemRegistry.DARK_MATTER.get());
                        output.accept(ItemRegistry.DARK_MATTER_BUCKET.get());

                        // 绝对零度（把暗物质压成超流体的唯一入口）
                        output.accept(ItemRegistry.ABSOLUTE_ZERO.get());

                        // 微型强子对撞机与奇点核心（克莱因瓶的获取链）
                        output.accept(ItemRegistry.MICRO_HADRON_COLLIDER_ITEM.get());
                        output.accept(ItemRegistry.SINGULARITY_CORE.get());
                        output.accept(ItemRegistry.ANTIMATTER_PARTICLE.get());

                        // 真空衰变（万能挖掘锤：负熵灌注 / 分子离解 / 对称破缺）
                        output.accept(ItemRegistry.VACUUM_DECAY.get());

                        // 克莱因瓶（随身存储终端）
                        output.accept(ItemRegistry.KLEIN_BOTTLE.get());
                    }).build());

    private CreativeTabRegistry() {}

    public static void register(IEventBus modBus) {
        CREATIVE_MODE_TABS.register(modBus);
    }
}