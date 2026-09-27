package org.gwfx.zuoyanmod.event;

import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.world.DomainExpansionDimensions;

@EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class DomainExpansionSpawnControl {

    private DomainExpansionSpawnControl() {}

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getEntity().level().dimension().equals(DomainExpansionDimensions.DOMAIN_KEY)
                && event.getEntity().getType() == EntityType.ENDERMAN) {
            event.setSpawnCancelled(true);
        }
    }
}
