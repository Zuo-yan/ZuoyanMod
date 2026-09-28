package org.gwfx.zuoyanmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.upgrade.UpgradeManager;

/**
 * 客户端 → 服务端：右键属性行，退还该能力最后一级加点并返还当年的经验花费。
 * 服务端全权校验（下标范围 / 有没有点可退），退完重算属性并回同步包。
 */
public record RefundStatPacket(int statIndex) implements CustomPacketPayload {

    public static final Type<RefundStatPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "refund_stat"));

    public static final StreamCodec<ByteBuf, RefundStatPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RefundStatPacket::statIndex,
            RefundStatPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RefundStatPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                UpgradeManager.refundStat(player, packet.statIndex());
            }
        });
    }
}
