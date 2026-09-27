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
 * 客户端 → 服务端：选定终极天赋（下标对齐 {@code UltimateTalent#VALUES}）。
 * 前提：5 条基础能力全部满级且尚未选定过——选定即永久，服务端会拒绝第二次。
 */
public record ChooseTalentPacket(int talentIndex) implements CustomPacketPayload {

    public static final Type<ChooseTalentPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Zuoyanmod.MODID, "choose_talent"));

    public static final StreamCodec<ByteBuf, ChooseTalentPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ChooseTalentPacket::talentIndex,
            ChooseTalentPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChooseTalentPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                UpgradeManager.chooseTalent(player, packet.talentIndex());
            }
        });
    }
}
