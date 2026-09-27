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
 * 客户端 → 服务端：按 Y 触发终极天赋。服务端校验已选天赋与冷却后执行；
 * 没选天赋 / 冷却中会在 actionbar 给出理由，不消耗冷却。
 */
public record TriggerUltimatePacket() implements CustomPacketPayload {

    public static final Type<TriggerUltimatePacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "trigger_ultimate"));

    public static final StreamCodec<ByteBuf, TriggerUltimatePacket> STREAM_CODEC =
            StreamCodec.unit(new TriggerUltimatePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TriggerUltimatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                UpgradeManager.triggerUltimate(player);
            }
        });
    }
}
