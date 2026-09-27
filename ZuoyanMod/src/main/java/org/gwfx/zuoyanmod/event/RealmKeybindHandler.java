package org.gwfx.zuoyanmod.event;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;

@EventBusSubscriber(modid = Zuoyanmod.MODID, value = Dist.CLIENT)
public final class RealmKeybindHandler {

    /**
     * 按键分类。1.21.1 的分类就是一个普通的字符串（本身就是 lang 键，
     * 这里取 {@code key.categories.zuoyanmod.zuoyan}）；构造 {@link KeyMapping}
     * 时会自动把新分类登记进原版的分类集合，不需要单独注册。
     */
    public static final String CATEGORY = "key.categories.zuoyanmod.zuoyan";

    // 1.21.1 使用 GLFW keycode：InputConstants.KEY_HOME
    public static final KeyMapping TOGGLE_REALM =
            new KeyMapping("key.zuoyanmod.toggle_realm", InputConstants.KEY_HOME, CATEGORY);

    private RealmKeybindHandler() {}

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_REALM);
    }
}
