package org.gwfx.zuoyanmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.gwfx.zuoyanmod.client.ClientKleinBottleView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

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
 * <p>26.x 就是 record payload；Forge 1.20.1 没有自动编解码，所以保留 record
 * 数据体，encode/decode/handle 改为静态方法（SimpleChannel 注册用）。
 */
public record KleinBottleSyncPacket(int scrollRow, int visibleRows, int viewSize, long totalItems,
                                    String search, int sortOrdinal, boolean descending,
                                    List<Integer> windowTotals) {

    public static void encode(KleinBottleSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.scrollRow);
        buf.writeVarInt(msg.visibleRows);
        buf.writeVarInt(msg.viewSize);
        buf.writeVarLong(msg.totalItems);
        buf.writeUtf(msg.search, KleinBottleViewPacket.MAX_SEARCH_LENGTH);
        buf.writeVarInt(msg.sortOrdinal);
        buf.writeBoolean(msg.descending);
        buf.writeVarInt(msg.windowTotals.size());
        for (int total : msg.windowTotals) {
            buf.writeVarInt(total);
        }
    }

    public static KleinBottleSyncPacket decode(FriendlyByteBuf buf) {
        int scrollRow = buf.readVarInt();
        int visibleRows = buf.readVarInt();
        int viewSize = buf.readVarInt();
        long totalItems = buf.readVarLong();
        String search = buf.readUtf(KleinBottleViewPacket.MAX_SEARCH_LENGTH);
        int sortOrdinal = buf.readVarInt();
        boolean descending = buf.readBoolean();
        int size = buf.readVarInt();
        List<Integer> totals = new ArrayList<>(Math.min(256, size));
        for (int i = 0; i < size; i++) {
            totals.add(buf.readVarInt());
        }
        return new KleinBottleSyncPacket(scrollRow, visibleRows, viewSize, totalItems,
                search, sortOrdinal, descending, totals);
    }

    /** 当前窗口第 slot 格的总量。界面每帧都会问，做成 O(1)。 */
    public long windowTotal(int slot) {
        return slot >= 0 && slot < windowTotals.size() ? windowTotals.get(slot) : 0L;
    }

    public static void handle(KleinBottleSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientKleinBottleView.apply(msg));
        ctx.get().setPacketHandled(true);
    }
}
