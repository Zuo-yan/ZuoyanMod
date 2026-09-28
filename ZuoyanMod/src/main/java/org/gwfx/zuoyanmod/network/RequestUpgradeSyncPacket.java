package org.gwfx.zuoyanmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.upgrade.UpgradeManager;

/**
 * 客户端 → 服务端：请求一份升级档案快照（打开升级界面时发）。
 * 服务端立刻回 {@link UpgradeSyncPacket}——没有它界面第一次打开会是空白。
 */
public record RequestUpgradeSyncPacket() implements CustomPacketPayload {

    public static final Type<RequestUpgradeSyncPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "request_upgrade_sync"));

    public static final StreamCodec<ByteBuf, RequestUpgradeSyncPacket> STREAM_CODEC =
            StreamCodec.unit(new RequestUpgradeSyncPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestUpgradeSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                UpgradeManager.sync(player);
            }
        });
    }
}
