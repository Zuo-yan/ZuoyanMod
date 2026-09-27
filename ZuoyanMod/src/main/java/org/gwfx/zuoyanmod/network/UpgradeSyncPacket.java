package org.gwfx.zuoyanmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.client.ClientUpgradeData;

import java.util.List;

/**
 * 服务端 → 客户端：升级档案快照。界面与 HUD 都只读它，不发请求不查询。
 * 冷却存的是截止 gameTime（原版每 tick 都向客户端同步世界时间，客户端用它减出剩余秒数）。
 *
 * @param levels               5 条基础能力等级（下标对齐 {@code UpgradeType#VALUES}）
 * @param talent               已选终极天赋 ordinal（-1 = 未选）
 * @param cooldownEnds         每个天赋的冷却截止 gameTime（下标对齐 {@code UltimateTalent#VALUES}）
 * @param dissociationExpire   「分子离解·灌能」到期 gameTime（0 = 未灌能）
 */
public record UpgradeSyncPacket(
        List<Integer> levels,
        int talent,
        List<Long> cooldownEnds,
        long dissociationExpire) implements CustomPacketPayload {

    public static final Type<UpgradeSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Zuoyanmod.MODID, "upgrade_sync"));

    public static final StreamCodec<ByteBuf, UpgradeSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(8)), UpgradeSyncPacket::levels,
            ByteBufCodecs.VAR_INT, UpgradeSyncPacket::talent,
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list(8)), UpgradeSyncPacket::cooldownEnds,
            ByteBufCodecs.VAR_LONG, UpgradeSyncPacket::dissociationExpire,
            UpgradeSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpgradeSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientUpgradeData.apply(packet));
    }

    /** 服务端发送入口 */
    public static void send(ServerPlayer player, UpgradeSyncPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}
