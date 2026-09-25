package org.gwfx.zuoyanmod.network;

import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.network.NetworkEvent;
import org.gwfx.zuoyanmod.effect.EffectRegistry;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class DeathNotePacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    public final String targetName;
    public final int durationSeconds;

    public DeathNotePacket(String targetName, int durationSeconds) {
        this.targetName = targetName;
        this.durationSeconds = durationSeconds;
    }

    public static void encode(DeathNotePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.targetName, 64);
        buf.writeVarInt(msg.durationSeconds);
    }

    public static DeathNotePacket decode(FriendlyByteBuf buf) {
        return new DeathNotePacket(buf.readUtf(64), buf.readVarInt());
    }

    public static void handle(DeathNotePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) {
                return;
            }
            if (!(sender.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            MinecraftServer server = serverLevel.getServer();
            if (server == null) {
                sender.sendSystemMessage(Component.translatable("message.zuoyanmod.death_note.server_unavailable"));
                return;
            }
            ServerPlayer target = server.getPlayerList().getPlayerByName(msg.targetName);
            if (target == null) {
                sender.sendSystemMessage(Component.translatable("message.zuoyanmod.death_note.target_not_found", msg.targetName));
                return;
            }
            int durationTicks = Math.max(1, msg.durationSeconds) * 20;
            target.addEffect(new MobEffectInstance(
                    EffectRegistry.HEART_PARALYSIS.get(),
                    durationTicks,
                    0,
                    false,
                    true,
                    true
            ));
            sender.sendSystemMessage(Component.translatable("message.zuoyanmod.death_note.written",
                    target.getName().getString(), msg.durationSeconds));
            target.sendSystemMessage(Component.translatable("message.zuoyanmod.death_note.victim_notice"));
            LOGGER.info("[DeathNote] {} wrote {} for {} seconds", sender.getName().getString(), target.getName().getString(), msg.durationSeconds);
        });
        ctx.get().setPacketHandled(true);
    }
}

