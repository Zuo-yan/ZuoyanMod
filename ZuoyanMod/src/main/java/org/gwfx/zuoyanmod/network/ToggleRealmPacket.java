package org.gwfx.zuoyanmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.gwfx.zuoyanmod.world.RealmTransitionManager;

import java.util.function.Supplier;

/** 客户端按 Home → 请服务端切换随身维度（26.x 是 record + StreamCodec，1.20.1 是手写编解码） */
public class ToggleRealmPacket {

    public static void encode(ToggleRealmPacket msg, FriendlyByteBuf buf) {
    }

    public static ToggleRealmPacket decode(FriendlyByteBuf buf) {
        return new ToggleRealmPacket();
    }

    public static void handle(ToggleRealmPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender(); // 1.20.1 的 getSender() 已经直接返回 ServerPlayer
            if (player != null) {
                RealmTransitionManager.toggle(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
