package org.gwfx.zuoyanmod.event;

import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.world.DomainExpansionDimensions;

/**
 * 领域展开维度：禁止末影人自然生成（决斗场需要干净的场地）。
 * 1.20.1 适配：26.x 的 FinalizeSpawnEvent→Forge 的 {@code MobSpawnEvent.FinalizeSpawn}，
 * EntityTypeIds.ENDERMAN→{@code EntityType.ENDERMAN}。
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class DomainExpansionSpawnControl {

    private DomainExpansionSpawnControl() {}

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getEntity().level().dimension().equals(DomainExpansionDimensions.DOMAIN_KEY)
                && event.getEntity().getType() == EntityType.ENDERMAN) {
            event.setSpawnCancelled(true);
        }
    }
}
