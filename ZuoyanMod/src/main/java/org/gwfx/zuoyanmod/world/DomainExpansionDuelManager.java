package org.gwfx.zuoyanmod.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.gwfx.zuoyanmod.effect.EffectRegistry;
import org.gwfx.zuoyanmod.event.DomainExpansionDuelTickHandler;
import org.gwfx.zuoyanmod.sound.SoundRegistry;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DomainExpansionDuelManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int FIGHT_AGAIN_DURATION_TICKS = 20 * 60;

    private static final Map<UUID, DuelMatch> MATCHES = new ConcurrentHashMap<>();

    private DomainExpansionDuelManager() {}

    public static boolean isParticipant(UUID uuid) {
        return MATCHES.containsKey(uuid);
    }

    public static DuelMatch getMatch(UUID participant) {
        return MATCHES.get(participant);
    }

    public static boolean startDuel(MinecraftServer server, ServerPlayer caster, LivingEntity target, ServerLevel domainLevel) {
        if (isParticipant(caster.getUUID()) || isParticipant(target.getUUID())) {
            return false;
        }

        DuelParticipant casterParticipant = DuelParticipant.capture(caster);
        DuelParticipant targetParticipant = DuelParticipant.capture(target);

        DuelMatch match = new DuelMatch(caster.getUUID(), target.getUUID(), casterParticipant, targetParticipant, domainLevel.dimension());
        MATCHES.put(caster.getUUID(), match);
        MATCHES.put(target.getUUID(), match);

        DomainExpansionWorldGen.generateArena(domainLevel);
        DomainExpansionWorldState.teleportPair(domainLevel, caster, target);

        caster.addEffect(new MobEffectInstance(EffectRegistry.FIGHT_AGAIN.get(), FIGHT_AGAIN_DURATION_TICKS, 0, false, false, true));
        playDuelAudio(domainLevel);
        return true;
    }

    public static void endDuel(MinecraftServer server, UUID participant, EndReason reason) {
        DuelMatch match = MATCHES.get(participant);
        if (match == null) {
            return;
        }

        match.end(server, participant, reason);
    }

    public static void onParticipantDeath(MinecraftServer server, UUID participant) {
        endDuel(server, participant, EndReason.DEATH);
    }

    public static void onParticipantLeftDimension(MinecraftServer server, UUID participant) {
        endDuel(server, participant, EndReason.LEFT_DIMENSION);
    }

    private static void playDuelAudio(ServerLevel domainLevel) {
        domainLevel.playSound(null, DomainExpansionDimensions.PLATFORM_CENTER, SoundRegistry.DOMAIN_EXPANSION_ACTIVATE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        domainLevel.playSound(null, DomainExpansionDimensions.PLATFORM_CENTER, SoundRegistry.DOMAIN_EXPANSION_MUSIC.get(), SoundSource.MASTER, 1.0F, 1.0F);
    }

    public enum EndReason {
        DEATH,
        LEFT_DIMENSION
    }

    public record DuelParticipant(ResourceKey<Level> originDimension, Vec3 originPos, float yRot, float xRot) {
        static DuelParticipant capture(LivingEntity entity) {
            return new DuelParticipant(entity.level().dimension(), entity.position(), entity.getYRot(), entity.getXRot());
        }

        DuelReturnData toReturnData() {
            return new DuelReturnData(originDimension, originPos, yRot, xRot);
        }
    }

    public static final class DuelMatch {
        private final UUID caster;
        private final UUID target;
        private final DuelParticipant casterState;
        private final DuelParticipant targetState;
        private final ResourceKey<Level> duelDimension;
        private boolean ended;

        private DuelMatch(UUID caster, UUID target, DuelParticipant casterState, DuelParticipant targetState, ResourceKey<Level> duelDimension) {
            this.caster = caster;
            this.target = target;
            this.casterState = casterState;
            this.targetState = targetState;
            this.duelDimension = duelDimension;
        }

        public boolean contains(UUID uuid) {
            return caster.equals(uuid) || target.equals(uuid);
        }

        public void end(MinecraftServer server, UUID defeatedParticipant, EndReason reason) {
            if (ended) {
                return;
            }
            ended = true;
            remove(caster);
            remove(target);

            if (!caster.equals(defeatedParticipant)) {
                DomainExpansionDuelTickHandler.scheduleReturn(server, caster, casterState.toReturnData());
            }
            if (!target.equals(defeatedParticipant)) {
                DomainExpansionDuelTickHandler.scheduleReturn(server, target, targetState.toReturnData());
            }
            LOGGER.info("Domain expansion duel ended because {}", reason);
        }

        private void remove(UUID uuid) {
            MATCHES.remove(uuid);
        }

    }
}

