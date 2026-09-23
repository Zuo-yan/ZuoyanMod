package org.gwfx.zuoyanmod.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.gwfx.zuoyanmod.Zuoyanmod;

import java.util.Optional;

/**
 * 网络通道（1.20.1 Forge：SimpleChannel）。
 *
 * <p>26.3 主线用的是 NeoForge 的 {@code PayloadRegistrar} + StreamCodec
 * （自动编解码的 record payload）；Forge 1.20.1 对应的是 {@link SimpleChannel}
 * + 手写 encode/decode + {@code Supplier<NetworkEvent.Context>}。
 * 包的语义与主线一一对应。
 */
public final class PacketHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Zuoyanmod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private PacketHandler() {}

    /** 在主类构造期调用（SimpleChannel 需要在 channel lock 阶段注册消息） */
    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, ToggleRealmPacket.class,
                ToggleRealmPacket::encode, ToggleRealmPacket::decode, ToggleRealmPacket::handle);
        CHANNEL.registerMessage(id++, DeathNotePacket.class,
                DeathNotePacket::encode, DeathNotePacket::decode, DeathNotePacket::handle);
        CHANNEL.registerMessage(id++, OpenKleinBottlePacket.class,
                OpenKleinBottlePacket::encode, OpenKleinBottlePacket::decode, OpenKleinBottlePacket::handle);
        CHANNEL.registerMessage(id++, CraftingTransferPacket.class,
                CraftingTransferPacket::encode, CraftingTransferPacket::decode, CraftingTransferPacket::handle);
        CHANNEL.registerMessage(id++, KleinBottleViewPacket.class,
                KleinBottleViewPacket::encode, KleinBottleViewPacket::decode, KleinBottleViewPacket::handle);
        CHANNEL.registerMessage(id++, KleinAnvilNamePacket.class,
                KleinAnvilNamePacket::encode, KleinAnvilNamePacket::decode, KleinAnvilNamePacket::handle);
        CHANNEL.registerMessage(id++, KleinBottleSyncPacket.class,
                KleinBottleSyncPacket::encode, KleinBottleSyncPacket::decode, KleinBottleSyncPacket::handle);
    }

    public static void sendToggleRealm() {
        CHANNEL.sendToServer(new ToggleRealmPacket());
    }

    public static void sendDeathNote(String targetName, int durationSeconds) {
        CHANNEL.sendToServer(new DeathNotePacket(targetName, durationSeconds));
    }

    public static void sendOpenKleinBottle() {
        CHANNEL.sendToServer(new OpenKleinBottlePacket());
    }

    /** JEI 的 + 按钮：请服务端从四维空间抓这份配方的材料 */
    public static void sendCraftingTransfer(net.minecraft.resources.ResourceLocation recipeId, boolean maxTransfer) {
        CHANNEL.sendToServer(new CraftingTransferPacket(recipeId, maxTransfer));
    }

    /** 终端：提交搜索词与滚动位置（搜索词已按长度截断，服务端还会再校验一次） */
    public static void sendKleinBottleView(String search, int scrollRow) {
        String text = search == null ? "" : search;
        if (text.length() > KleinBottleViewPacket.MAX_SEARCH_LENGTH) {
            text = text.substring(0, KleinBottleViewPacket.MAX_SEARCH_LENGTH);
        }
        CHANNEL.sendToServer(new KleinBottleViewPacket(text, Math.max(0, scrollRow)));
    }

    /** 终端铁砧：提交重命名输入框内容（已按 50 字符截断） */
    public static void sendKleinAnvilName(String name) {
        String text = name == null ? "" : name;
        if (text.length() > KleinAnvilNamePacket.MAX_NAME_LENGTH) {
            text = text.substring(0, KleinAnvilNamePacket.MAX_NAME_LENGTH);
        }
        CHANNEL.sendToServer(new KleinAnvilNamePacket(text));
    }

    /** 服务端 → 指定玩家的视图快照 */
    public static void sendToPlayer(ServerPlayer player, KleinBottleSyncPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
