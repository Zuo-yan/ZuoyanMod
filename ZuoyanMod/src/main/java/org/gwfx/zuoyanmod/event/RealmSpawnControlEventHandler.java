package org.gwfx.zuoyanmod.event;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.world.RealmDimensions;

/**
 * 灵境维度：禁止敌对生物与史莱姆类自然生成。
 * 1.20.1 适配：26.x 的 FinalizeSpawnEvent→Forge 的 {@code MobSpawnEvent.FinalizeSpawn}，
 * EntitySpawnReason→{@code SpawnReason}，EntityTypeIds→{@code EntityType} 常量。
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class RealmSpawnControlEventHandler {

    private RealmSpawnControlEventHandler() {}

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        Level level = event.getLevel().getLevel();
        if (!level.dimension().equals(RealmDimensions.REALM_KEY)) {
            return;
        }

        MobSpawnType spawnType = event.getSpawnType();
        EntityType<?> entityType = event.getEntity().getType();
        boolean naturalSpawn = spawnType == MobSpawnType.NATURAL
                || spawnType == MobSpawnType.CHUNK_GENERATION
                || spawnType == MobSpawnType.PATROL;
        boolean hostile = entityType.getCategory() == MobCategory.MONSTER;
        boolean slime = entityType == EntityType.SLIME
                || entityType == EntityType.MAGMA_CUBE;
        if (naturalSpawn && (hostile || slime)) {
            // FinalizeSpawn 用 setSpawnCancelled 取消生成（比 setCanceled 语义更准）
            event.setSpawnCancelled(true);
        }
    }
}
