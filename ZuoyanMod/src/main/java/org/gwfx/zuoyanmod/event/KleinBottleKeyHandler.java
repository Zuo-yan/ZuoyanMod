package org.gwfx.zuoyanmod.event;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.network.PacketHandler;

/**
 * 克莱因瓶的 K 键：把背包里的随身终端召出来。
 * <p>
 * 1.20.1 适配：注册移到 {@link RealmKeybindHandler}（MOD 总线）；这里只保留
 * FORGE 总线上的客户端 tick 消费。KeyMapping 构造器在 1.20.1 接受 String 类别。
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID, value = Dist.CLIENT)
public final class KleinBottleKeyHandler {

    public static final KeyMapping OPEN_KLEIN_BOTTLE =
            new KeyMapping("key.zuoyanmod.open_klein_bottle", InputConstants.KEY_K, RealmKeybindHandler.CATEGORY);

    private KleinBottleKeyHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // 26.x 的 ClientTickEvent.Post 对应这里的 END phase
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        if (OPEN_KLEIN_BOTTLE.consumeClick()) {
            PacketHandler.sendOpenKleinBottle();
        }
    }
}
