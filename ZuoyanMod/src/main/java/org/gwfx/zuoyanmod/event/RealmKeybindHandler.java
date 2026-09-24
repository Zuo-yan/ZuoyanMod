package org.gwfx.zuoyanmod.event;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;

@EventBusSubscriber(modid = Zuoyanmod.MODID, value = Dist.CLIENT)
public final class RealmKeybindHandler {

    /**
     * 按键分类。这里的 {@link Identifier} 路径 {@code zuoyan} 同时决定了两件事：
     * 分类在按键设置里的排序位置，以及它去 lang 文件里取的名字
     * （{@code key.category.zuoyanmod.zuoyan}）。
     *
     * <p>为什么不在这里调 {@code KeyMapping.Category.register(...)}：那个方法在 NeoForge 里
     * 已标 {@code @Deprecated}，它会把分类塞进一个全局静态表，注册时机不受控。
     * 正解是在 {@link RegisterKeyMappingsEvent#registerCategory} 里登记，
     * 由加载器决定何时收集——见下方 {@link #onRegisterKeys}。
     */
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "zuoyan"));

    // 26.x 使用 SDL scancode：InputConstants.KEY_HOME
    public static final KeyMapping TOGGLE_REALM =
            new KeyMapping("key.zuoyanmod.toggle_realm", InputConstants.KEY_HOME, CATEGORY);

    private RealmKeybindHandler() {}

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(TOGGLE_REALM);
    }
}
