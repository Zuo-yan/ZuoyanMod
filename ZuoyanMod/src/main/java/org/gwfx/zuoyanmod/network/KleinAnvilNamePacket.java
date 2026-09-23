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
 * 客户端 → 服务端：终端右栏铁砧的重命名输入框内容变了。
 *
 * <p>铁砧的"改名"是算进代价里的（改名 = 1 级），所以名字必须到服务端那份
 * 隐形铁砧那儿去——客户端只负责打字。长度在客户端已按
 * {@link AnvilMenu#MAX_NAME_LENGTH} 截断，服务端 {@code setItemName} 还会再校验一次。
 */
public record KleinAnvilNamePacket(String name) implements CustomPacketPayload {

    public static final Type<KleinAnvilNamePacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "klein_anvil_name"));

    public static final int MAX_NAME_LENGTH = 50;

    public static final StreamCodec<ByteBuf, KleinAnvilNamePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH), KleinAnvilNamePacket::name,
            KleinAnvilNamePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(KleinAnvilNamePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.containerMenu instanceof KleinBottleMenu menu) {
                menu.applyAnvilName(packet.name());
            }
        });
    }
}
