package org.gwfx.zuoyanmod.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.effect.EffectRegistry;
import org.slf4j.Logger;

public record DeathNotePacket(String targetName, int durationSeconds) implements CustomPacketPayload {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Type<DeathNotePacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "death_note"));

    public static final StreamCodec<ByteBuf, DeathNotePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DeathNotePacket::targetName,
            ByteBufCodecs.VAR_INT, DeathNotePacket::durationSeconds,
            DeathNotePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeathNotePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) return;
            if (!(sender.level() instanceof ServerLevel serverLevel)) return;
            MinecraftServer server = serverLevel.getServer();
            if (server == null) {
                sender.sendSystemMessage(Component.translatable("message.zuoyanmod.death_note.server_unavailable"));
                return;
            }
            ServerPlayer target = server.getPlayerList().getPlayerByName(packet.targetName());
            if (target == null) {
                sender.sendSystemMessage(Component.translatable("message.zuoyanmod.death_note.target_not_found", packet.targetName()));
                return;
            }
            int durationTicks = Math.max(1, packet.durationSeconds()) * 20;
            target.addEffect(new MobEffectInstance(
                    EffectRegistry.MAMBA_FORCE_DEFENSE,
                    durationTicks,
                    0,
                    false,
                    true,
                    true
            ));
            sender.sendSystemMessage(Component.translatable("message.zuoyanmod.death_note.written",
                    target.getName().getString(), packet.durationSeconds()));
            target.sendSystemMessage(Component.translatable("message.zuoyanmod.death_note.victim_notice"));
            LOGGER.info("[DeathNote] {} wrote {} for {} seconds", sender.getName().getString(), target.getName().getString(), packet.durationSeconds());
        });
    }
}