package org.gwfx.zuoyanmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.upgrade.UpgradeManager;

/**
 * 客户端 → 服务端：请求升级某条基础能力（下标对齐 {@code UpgradeType#VALUES}）。
 * 服务端全权校验（下标范围 / 未满级 / 经验够不够），成功后扣经验、重算属性并回同步包。
 */
public record UpgradeStatPacket(int statIndex) implements CustomPacketPayload {

    public static final Type<UpgradeStatPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Zuoyanmod.MODID, "upgrade_stat"));

    public static final StreamCodec<ByteBuf, UpgradeStatPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UpgradeStatPacket::statIndex,
            UpgradeStatPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpgradeStatPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                UpgradeManager.upgradeStat(player, packet.statIndex());
            }
        });
    }
}
