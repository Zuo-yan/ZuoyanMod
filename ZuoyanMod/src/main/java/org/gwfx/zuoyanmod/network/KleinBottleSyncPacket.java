package org.gwfx.zuoyanmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.client.ClientKleinBottleView;

import java.util.List;

/**
 * 服务端 → 客户端：终端的**视图快照**。
 *
 * <p>原版的槽位同步只带 {@code ItemStack}，而 {@code ItemStack.count} 上限就是堆叠上限，
 * 所以客户端根本不知道"这一格其实有 3300 个"。这就是 AE2 要单独同步
 * {@code GridInventoryEntry}、RS2 要单独同步 {@code ResourceAmount} 的原因；这里照做：
 * 把当前窗口每一格的**总量**单独打成一个包，客户端画在槽位右下角。
 *
 * <p>总量超过 {@link Integer#MAX_VALUE} 时按上限截断——真实场景到不了，
 * 但别让溢出把数字画成负数。
 *
 * @param scrollRow    当前滚动到第几行（服务端权威值，客户端据此校正滚动条）
 * @param visibleRows  当前一屏显示几行
 * @param viewSize     过滤之后的物**种类**数
 * @param totalItems   整顿空间里的总个数（页脚统计）
 * @param search       服务端记住的搜索词（重开界面时回填搜索框）
 * @param sortOrdinal  排序方式序号，对应 {@code FourDimensionalSpace.SortMode#VALUES}
 * @param descending   是否倒序
 * @param windowTotals 当前窗口每格的总量（长度 = 可见槽位数）
 */
public record KleinBottleSyncPacket(
        int scrollRow,
        int visibleRows,
        int viewSize,
        long totalItems,
        String search,
        int sortOrdinal,
        boolean descending,
        List<Integer> windowTotals) implements CustomPacketPayload {

    public static final Type<KleinBottleSyncPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "klein_bottle_sync"));

    public static final StreamCodec<ByteBuf, KleinBottleSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, KleinBottleSyncPacket::scrollRow,
            ByteBufCodecs.VAR_INT, KleinBottleSyncPacket::visibleRows,
            ByteBufCodecs.VAR_INT, KleinBottleSyncPacket::viewSize,
            ByteBufCodecs.VAR_LONG, KleinBottleSyncPacket::totalItems,
            ByteBufCodecs.stringUtf8(KleinBottleViewPacket.MAX_SEARCH_LENGTH), KleinBottleSyncPacket::search,
            ByteBufCodecs.VAR_INT, KleinBottleSyncPacket::sortOrdinal,
            ByteBufCodecs.BOOL, KleinBottleSyncPacket::descending,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(256)), KleinBottleSyncPacket::windowTotals,
            KleinBottleSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 当前窗口第 slot 格的总量。界面每帧都会问，做成 O(1)。 */
    public long windowTotal(int slot) {
        return slot >= 0 && slot < windowTotals.size() ? windowTotals.get(slot) : 0L;
    }

    public static void handle(KleinBottleSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientKleinBottleView.apply(packet));
    }

    /** 服务端发送入口 */
    public static void send(ServerPlayer player, KleinBottleSyncPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}
