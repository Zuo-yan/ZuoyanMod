package org.gwfx.zuoyanmod.event;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypeIds;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.world.RealmDimensions;

@EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class RealmSpawnControlEventHandler {

    private RealmSpawnControlEventHandler() {}

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Level level = event.getLevel().getLevel();
        if (!level.dimension().equals(RealmDimensions.REALM_KEY)) {
            return;
        }

        EntitySpawnReason spawnType = event.getSpawnType();
        EntityType<?> entityType = event.getEntity().getType();
        boolean naturalSpawn = spawnType == EntitySpawnReason.NATURAL || spawnType == EntitySpawnReason.CHUNK_GENERATION || spawnType == EntitySpawnReason.PATROL;
        boolean hostile = entityType.getCategory() == MobCategory.MONSTER;
        boolean slime = entityType == BuiltInRegistries.ENTITY_TYPE.getValue(EntityTypeIds.SLIME)
                || entityType == BuiltInRegistries.ENTITY_TYPE.getValue(EntityTypeIds.MAGMA_CUBE);
        if (naturalSpawn && (hostile || slime)) {
            event.setSpawnCancelled(true);
        }
    }
}
