package org.gwfx.zuoyanmod.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.world.DomainExpansionDimensions;
import org.gwfx.zuoyanmod.world.DomainExpansionDuelManager;

/**
 * 领域展开决斗的参与者生命周期：死亡 / 跨维度离开 / 克隆（死后重生）都视为退出决斗。
 * 1.20.1 适配：PlayerEvent 换到 Forge 的事件总线包。
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID)
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
        if (player.level().isClientSide || player.level().getServer() == null) {
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
        if (event.getEntity().level().isClientSide || event.getEntity().level().getServer() == null) {
            return;
        }
        if (DomainExpansionDuelManager.isParticipant(event.getOriginal().getUUID())) {
            DomainExpansionDuelManager.onParticipantDeath(event.getEntity().level().getServer(), event.getOriginal().getUUID());
        }
    }
}
