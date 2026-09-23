package org.gwfx.zuoyanmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.gwfx.zuoyanmod.item.KleinBottleItem;

import java.util.function.Supplier;

/** 客户端按 K → 请服务端打开背包里的克莱因瓶 */
public class OpenKleinBottlePacket {

    public static void encode(OpenKleinBottlePacket msg, FriendlyByteBuf buf) {
    }

    public static OpenKleinBottlePacket decode(FriendlyByteBuf buf) {
        return new OpenKleinBottlePacket();
    }

    public static void handle(OpenKleinBottlePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender(); // 1.20.1 的 getSender() 已经直接返回 ServerPlayer
            if (player != null) {
                KleinBottleItem.openFor(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
