package org.gwfx.zuoyanmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.menu.KleinBottleMenu;

/**
 * 客户端 → 服务端：终端的**连续型**视图状态（搜索词 + 滚动位置）。
 *
 * <p>为什么这两样要走包而不是原版的 {@code clickMenuButton}：
 * <ul>
 *   <li>搜索是逐字输入的，要能在打字时连续提交（客户端做了 150ms 去抖）；</li>
 *   <li>滚动条是拖拽的，一秒钟可能来二三十次，走按钮通道又慢又啰嗦。</li>
 * </ul>
 * 排序方式、排序方向、行数、整理这些**离散**动作仍然走 {@code clickMenuButton}，
 * 因为它们本来就是"点一下"的语义，用原版通道还能顺带带上完整的服务端校验。
 *
 * <p>服务端是权威：这里只提交意图，真正生效的值由 {@link KleinBottleSyncPacket} 回传。
 */
public record KleinBottleViewPacket(String search, int scrollRow) implements CustomPacketPayload {

    /**
     * 搜索框长度上限，和界面里 {@code EditBox#setMaxLength} 保持一致。
     *
     * <p>注意 {@code ByteBufCodecs.stringUtf8(n)} 的 {@code n} 是**字符数**上限，
     * 并且额外校验"编码后字节数 ≤ 3n"（{@code Utf8String.write}）。
     * 64 个字符最坏就是 64×3 = 192 字节，正好等于 3n，不会越界；
     * 但也别把 n 调小——改成 60 就会让 60 个中文字的搜索词直接抛 EncoderException。
     */
    public static final int MAX_SEARCH_LENGTH = 64;

    public static final Type<KleinBottleViewPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "klein_bottle_view"));

    public static final StreamCodec<ByteBuf, KleinBottleViewPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_SEARCH_LENGTH), KleinBottleViewPacket::search,
            ByteBufCodecs.VAR_INT, KleinBottleViewPacket::scrollRow,
            KleinBottleViewPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(KleinBottleViewPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.containerMenu instanceof KleinBottleMenu menu) {
                menu.applyClientView(packet.search(), packet.scrollRow());
            }
        });
    }
}
