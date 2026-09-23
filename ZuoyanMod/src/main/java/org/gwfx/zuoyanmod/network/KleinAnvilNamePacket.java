package org.gwfx.zuoyanmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraftforge.network.NetworkEvent;
import org.gwfx.zuoyanmod.menu.KleinBottleMenu;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：终端右栏铁砧的重命名输入框内容变了。
 *
 * <p>铁砧的"改名"是算进代价里的（改名 = 1 级），所以名字必须到服务端那份
 * 隐形铁砧那儿去——客户端只负责打字。长度在客户端已按
 * {@link AnvilMenu#MAX_NAME_LENGTH} 截断，服务端 {@code setItemName} 还会再校验一次。
 */
public class KleinAnvilNamePacket {

    public static final int MAX_NAME_LENGTH = 50;

    public final String name;

    public KleinAnvilNamePacket(String name) {
        this.name = name;
    }

    public static void encode(KleinAnvilNamePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.name, MAX_NAME_LENGTH);
    }

    public static KleinAnvilNamePacket decode(FriendlyByteBuf buf) {
        return new KleinAnvilNamePacket(buf.readUtf(MAX_NAME_LENGTH));
    }

    public static void handle(KleinAnvilNamePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender(); // 1.20.1 的 getSender() 已经直接返回 ServerPlayer
            if (player != null
                    && player.containerMenu instanceof KleinBottleMenu menu) {
                menu.applyAnvilName(msg.name);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
