package org.gwfx.zuoyanmod.ai.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.gwfx.zuoyanmod.Zuoyanmod;

/**
 * 服务端 → 客户端：AI 连通性测试结果。
 */
public record AiTestConnectionResultPacket(boolean success, int latencyMs, String message) implements CustomPacketPayload {

    public static final Type<AiTestConnectionResultPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "ai_test_connection_result"));

    public static final StreamCodec<ByteBuf, AiTestConnectionResultPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, AiTestConnectionResultPacket::success,
            ByteBufCodecs.VAR_INT, AiTestConnectionResultPacket::latencyMs,
            ByteBufCodecs.stringUtf8(1024), AiTestConnectionResultPacket::message,
            AiTestConnectionResultPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AiTestConnectionResultPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> org.gwfx.zuoyanmod.client.ai.AiConfigClientData.onTestResult(packet));
    }
}