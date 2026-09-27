package org.gwfx.zuoyanmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.client.ClientModToasts;

/**
 * 服务端 → 客户端：模组效果通知（Toast）。
 *
 * <p>替代 {@code sendSystemMessage} 聊天栏提示 —— 战斗/效果类反馈（北冥狂刃的
 * 狂热结算、反伤、时停提示这类）一次战斗能刷十几条，聊天栏既挡视野又留不住重点。
 * 改走本包后由客户端 {@link ClientModToasts} 队列承接，{@code ModToastHud}
 * 在屏幕右上角堆叠展示、自动淡出，聊天栏只留给玩家自己打的字。
 *
 * <p>文本是完整 Component：翻译键、§ 样式、参数都在服务端解析好再下发，
 * 客户端只负责画。
 */
public record ModToastPacket(Component text) implements CustomPacketPayload {

    public static final Type<ModToastPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Zuoyanmod.MODID, "mod_toast"));

    public static final StreamCodec<ByteBuf, ModToastPacket> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC, ModToastPacket::text,
            ModToastPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ModToastPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientModToasts.push(packet.text()));
    }

    /**
     * 服务端发送入口：把一条效果通知推到玩家屏幕右上角。
     *
     * <p>参数放宽到 {@link Player}：事件处理器里拿到的静态类型常是 Player
     * （运行时在服务端必为 ServerPlayer），调用侧就不必强转了；
     * 万一真的传进来非 ServerPlayer（不应发生），退回聊天栏，消息不丢。
     */
    public static void send(Player player, Component text) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new ModToastPacket(text));
        } else {
            player.sendSystemMessage(text);
        }
    }
}
