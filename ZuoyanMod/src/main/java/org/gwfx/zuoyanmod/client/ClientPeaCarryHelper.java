package org.gwfx.zuoyanmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.gwfx.zuoyanmod.entity.SuperElectricGatlingPeaEntity;

/**
 * 客户端抱持豌豆状态辅助查询工具类。
 */
public final class ClientPeaCarryHelper {

    private ClientPeaCarryHelper() {}

    public static boolean isCarryingPea(int playerId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        Entity playerEntity = mc.level.getEntity(playerId);
        if (playerEntity == null) return false;

        AABB box = playerEntity.getBoundingBox().inflate(3.0D);
        for (SuperElectricGatlingPeaEntity pea : mc.level.getEntitiesOfClass(SuperElectricGatlingPeaEntity.class, box)) {
            if (pea.getCarrierId() == playerId) {
                return true;
            }
        }
        return false;
    }

    public static SuperElectricGatlingPeaEntity getLocalCarriedPea() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;
        AABB box = mc.player.getBoundingBox().inflate(3.0D);
        for (SuperElectricGatlingPeaEntity pea : mc.level.getEntitiesOfClass(SuperElectricGatlingPeaEntity.class, box)) {
            if (pea.getCarrierId() == mc.player.getId()) {
                return pea;
            }
        }
        return null;
    }
}
