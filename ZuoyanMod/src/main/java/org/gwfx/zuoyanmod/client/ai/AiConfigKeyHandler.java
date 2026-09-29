package org.gwfx.zuoyanmod.client.ai;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.event.RealmKeybindHandler;
import org.gwfx.zuoyanmod.network.PacketHandler;

/**
 * AI 配置界面的按键（默认 <b>O</b>）。
 *
 * <p>与 {@code client.UpgradeKeyHandler} 的写法一致：本类只在客户端加载，
 * 也是全项目里唯一 new {@link AiConfigScreen} 的地方。
 */
@EventBusSubscriber(modid = Zuoyanmod.MODID, value = Dist.CLIENT)
public final class AiConfigKeyHandler {

    public static final KeyMapping OPEN_AI_CONFIG =
            new KeyMapping("key.zuoyanmod.open_ai_config", InputConstants.KEY_O, RealmKeybindHandler.CATEGORY);

    private AiConfigKeyHandler() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        // 分类由 RealmKeybindHandler 登记，这里只登记按键，避免同一个分类被 registerCategory 两次
        event.register(OPEN_AI_CONFIG);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null) {
            return;
        }
        if (OPEN_AI_CONFIG.consumeClick()) {
            // 先清掉上一份快照再请求：否则界面会先闪出上一次（可能来自别的服务器）的配置
            AiConfigClientData.clear();
            PacketHandler.sendRequestAiConfig();
            minecraft.gui.setScreen(new AiConfigScreen());
        }
    }
}
