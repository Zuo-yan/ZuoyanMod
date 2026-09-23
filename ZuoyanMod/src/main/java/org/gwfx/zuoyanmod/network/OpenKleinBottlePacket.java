package org.gwfx.zuoyanmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.item.KleinBottleItem;

/** 客户端按 K → 请服务端打开背包里的克莱因瓶 */
public record OpenKleinBottlePacket() implements CustomPacketPayload {

    public static final Type<OpenKleinBottlePacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "open_klein_bottle"));

    public static final StreamCodec<ByteBuf, OpenKleinBottlePacket> STREAM_CODEC =
            StreamCodec.unit(new OpenKleinBottlePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenKleinBottlePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                org.gwfx.zuoyanmod.item.KleinBottleItem.openFor(player);
            }
        });
    }
}
