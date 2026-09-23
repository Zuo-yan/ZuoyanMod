package org.gwfx.zuoyanmod.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.world.DomainExpansionDimensions;
import org.gwfx.zuoyanmod.world.DomainExpansionDuelManager;

@EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class DomainExpansionDuelEventHandler {

    private DomainExpansionDuelEventHandler() {}

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().getServer() == null) {
            return;
        }
        if (DomainExpansionDuelManager.isParticipant(entity.getUUID())) {
            DomainExpansionDuelManager.onParticipantDeath(entity.level().getServer(), entity.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.level().getServer() == null) {
            return;
        }
        if (!DomainExpansionDimensions.DOMAIN_KEY.equals(event.getTo())) {
            if (DomainExpansionDuelManager.isParticipant(player.getUUID())) {
                DomainExpansionDuelManager.onParticipantLeftDimension(player.level().getServer(), player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity().level().isClientSide() || event.getEntity().level().getServer() == null) {
            return;
        }
        if (DomainExpansionDuelManager.isParticipant(event.getOriginal().getUUID())) {
            DomainExpansionDuelManager.onParticipantDeath(event.getEntity().level().getServer(), event.getOriginal().getUUID());
        }
    }
}
