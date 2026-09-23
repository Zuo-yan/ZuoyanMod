package org.gwfx.zuoyanmod.network;

import com.mojang.logging.LogUtils;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.slf4j.Logger;

@EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class PacketHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private PacketHandler() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Zuoyanmod.MODID).versioned("1");
        registrar.playToServer(
                ToggleRealmPacket.TYPE,
                ToggleRealmPacket.STREAM_CODEC,
                ToggleRealmPacket::handle
        );
        registrar.playToServer(
                DeathNotePacket.TYPE,
                DeathNotePacket.STREAM_CODEC,
                DeathNotePacket::handle
        );
        registrar.playToServer(
                OpenKleinBottlePacket.TYPE,
                OpenKleinBottlePacket.STREAM_CODEC,
                OpenKleinBottlePacket::handle
        );
        registrar.playToServer(
                CraftingTransferPacket.TYPE,
                CraftingTransferPacket.STREAM_CODEC,
                CraftingTransferPacket::handle
        );
        // 终端视图：客户端提交搜索词/滚动位置，服务端回传视图快照（含每格总量）
        registrar.playToServer(
                KleinBottleViewPacket.TYPE,
                KleinBottleViewPacket.STREAM_CODEC,
                KleinBottleViewPacket::handle
        );
        // 终端铁砧：重命名输入框内容（改名也算进铁砧代价里，必须到服务端那份隐形铁砧那儿算）
        registrar.playToServer(
                KleinAnvilNamePacket.TYPE,
                KleinAnvilNamePacket.STREAM_CODEC,
                KleinAnvilNamePacket::handle
        );
        registrar.playToClient(
                KleinBottleSyncPacket.TYPE,
                KleinBottleSyncPacket.STREAM_CODEC,
                KleinBottleSyncPacket::handle
        );
        LOGGER.info("[Network] Successfully registered payload handlers");
    }

    public static void sendToggleRealm() {
        LOGGER.info("[Realm] Sending toggle packet to server");
        sendToServer(new ToggleRealmPacket());
    }

    public static void sendDeathNote(String targetName, int durationSeconds) {
        sendToServer(new DeathNotePacket(targetName, durationSeconds));
    }

    public static void sendOpenKleinBottle() {
        sendToServer(new OpenKleinBottlePacket());
    }

    /** JEI 的 + 按钮：请服务端从四维空间抓这份配方的材料 */
    public static void sendCraftingTransfer(net.minecraft.resources.Identifier recipeId, boolean maxTransfer) {
        sendToServer(new CraftingTransferPacket(recipeId, maxTransfer));
    }

    /** 终端：提交搜索词与滚动位置（搜索词已按长度截断，服务端还会再校验一次） */
    public static void sendKleinBottleView(String search, int scrollRow) {
        String text = search == null ? "" : search;
        if (text.length() > KleinBottleViewPacket.MAX_SEARCH_LENGTH) {
            text = text.substring(0, KleinBottleViewPacket.MAX_SEARCH_LENGTH);
        }
        sendToServer(new KleinBottleViewPacket(text, Math.max(0, scrollRow)));
    }

    /** 终端铁砧：提交重命名输入框内容（已按 50 字符截断） */
    public static void sendKleinAnvilName(String name) {
        String text = name == null ? "" : name;
        if (text.length() > KleinAnvilNamePacket.MAX_NAME_LENGTH) {
            text = text.substring(0, KleinAnvilNamePacket.MAX_NAME_LENGTH);
        }
        sendToServer(new KleinAnvilNamePacket(text));
    }

    private static void sendToServer(CustomPacketPayload payload) {
        // 实现放在 client 包：本类是双端类，绝不能直接引用 net.minecraft.client.*，
        // 否则专用服务端加载模组时会因为要解析 Minecraft 而连带加载客户端界面类直接崩。
        org.gwfx.zuoyanmod.client.ClientPacketSender.sendToServer(payload);
    }
}