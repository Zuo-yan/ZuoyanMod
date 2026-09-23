package org.gwfx.zuoyanmod.event;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.world.DomainExpansionDimensions;
import org.gwfx.zuoyanmod.world.DomainExpansionWorldState;
import org.gwfx.zuoyanmod.world.DuelReturnData;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class DomainExpansionDuelTickHandler {

    private static final int RETURN_DELAY_TICKS = 20 * 5;
    private static final Map<UUID, DelayedReturn> DELAYED_RETURNS = new ConcurrentHashMap<>();

    private DomainExpansionDuelTickHandler() {}

    public static void scheduleReturn(MinecraftServer server, UUID participant, DuelReturnData data) {
        DELAYED_RETURNS.put(participant, new DelayedReturn(participant, data, RETURN_DELAY_TICKS));
        sendCountdown(server, participant, 5);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();

        Iterator<Map.Entry<UUID, DelayedReturn>> iterator = DELAYED_RETURNS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, DelayedReturn> entry = iterator.next();
            DelayedReturn delayed = entry.getValue();
            delayed.tickCountdown(server);
            if (delayed.shouldReturn()) {
                returnParticipant(server, delayed.defeatedParticipant, delayed.data);
                iterator.remove();
            }
        }
    }

    private static void sendCountdown(MinecraftServer server, UUID participant, int seconds) {
        var player = server.getPlayerList().getPlayer(participant);
        if (player != null) {
            player.sendOverlayMessage(Component.literal("§e" + seconds + "秒后回到原世界").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        }
    }

    private static void returnParticipant(MinecraftServer server, UUID participant, DuelReturnData data) {
        ServerLevel originLevel = server.getLevel(data.originDimension());
        ServerLevel duelLevel = server.getLevel(DomainExpansionDimensions.DOMAIN_KEY);
        if (originLevel == null || duelLevel == null) {
            return;
        }
        Entity entity = duelLevel.getEntity(participant);
        if (entity != null) {
            DomainExpansionWorldState.teleportEntity(originLevel, entity,
                    data.originPos().x, data.originPos().y, data.originPos().z, data.yRot(), data.xRot());
        }
    }

    private static final class DelayedReturn {
        private final UUID defeatedParticipant;
        private final DuelReturnData data;
        private int remainingTicks;

        private DelayedReturn(UUID defeatedParticipant, DuelReturnData data, int remainingTicks) {
            this.defeatedParticipant = defeatedParticipant;
            this.data = data;
            this.remainingTicks = remainingTicks;
        }

        private void tickCountdown(MinecraftServer server) {
            if (remainingTicks > 0) {
                if (remainingTicks % 20 == 0) {
                    sendCountdown(server, defeatedParticipant, remainingTicks / 20);
                }
                remainingTicks--;
            }
        }

        private boolean shouldReturn() {
            return remainingTicks <= 0;
        }
    }
}
