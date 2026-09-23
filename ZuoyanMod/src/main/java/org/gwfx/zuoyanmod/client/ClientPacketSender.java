package org.gwfx.zuoyanmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 客户端 → 服务端的发包入口。
 *
 * <p>等价于 NeoForge 的 {@code ClientPacketDistributor.sendToServer}，但多一个空连接保护：
 * 在标题界面等尚未连入服务器的时刻调用时静默忽略，而不是抛异常
 * （{@code ClientPacketDistributor} 内部用的是 {@code requireNonNull}）。
 *
 * <p>之所以要独立成类：{@code PacketHandler} 是<b>双端</b>类（{@code @EventBusSubscriber}
 * 不带 Dist），一旦它的字节码里直接出现 {@code Minecraft}，服务端加载时会因为类型解析
 * 被迫加载客户端类而崩溃。这里的方法签名只用通用类型 {@link CustomPacketPayload}，
 * 所以服务端的校验不会深入到这个类。
 */
public final class ClientPacketSender {

    private ClientPacketSender() {
    }

    /** 把负载发给服务器；未连接时什么都不做。只在客户端调用。 */
    public static void sendToServer(CustomPacketPayload payload) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(payload));
        }
    }
}
