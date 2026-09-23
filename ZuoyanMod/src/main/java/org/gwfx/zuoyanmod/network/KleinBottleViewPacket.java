package org.gwfx.zuoyanmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.gwfx.zuoyanmod.menu.KleinBottleMenu;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：终端的**连续型**视图状态（搜索词 + 滚动位置）。
 *
 * <p>为什么这两样要走包而不是原版的 {@code clickMenuButton}：
 * <ul>
 *   <li>搜索是逐字输入的，要能在打字时连续提交（客户端做了 150ms 去抖）；</li>
 *   <li>滚动条是拖拽的，一秒钟可能来二三十次，走按钮通道又慢又啰嗦。</li>
 * </ul>
 * 排序方式、排序方向、行数、整理这些**离散**动作仍然走 {@code clickMenuButton}。
 *
 * <p>服务端是权威：这里只提交意图，真正生效的值由 {@link KleinBottleSyncPacket} 回传。
 */
public class KleinBottleViewPacket {

    /**
     * 搜索框长度上限，和界面里 {@code EditBox#setMaxLength} 保持一致。
     */
    public static final int MAX_SEARCH_LENGTH = 64;

    public final String search;
    public final int scrollRow;

    public KleinBottleViewPacket(String search, int scrollRow) {
        this.search = search;
        this.scrollRow = scrollRow;
    }

    public static void encode(KleinBottleViewPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.search, MAX_SEARCH_LENGTH);
        buf.writeVarInt(msg.scrollRow);
    }

    public static KleinBottleViewPacket decode(FriendlyByteBuf buf) {
        return new KleinBottleViewPacket(buf.readUtf(MAX_SEARCH_LENGTH), buf.readVarInt());
    }

    public static void handle(KleinBottleViewPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender(); // 1.20.1 的 getSender() 已经直接返回 ServerPlayer
            if (player != null
                    && player.containerMenu instanceof KleinBottleMenu menu) {
                menu.applyClientView(msg.search, msg.scrollRow);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
