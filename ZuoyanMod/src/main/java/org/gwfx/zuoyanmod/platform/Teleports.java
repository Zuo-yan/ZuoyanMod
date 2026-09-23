package org.gwfx.zuoyanmod.platform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * 版本适配层：玩家跨维度传送。
 *
 * <p>「把玩家传到另一个维度的指定坐标」在 Minecraft 各版本间的**方法签名**变过多次，
 * 且调用点分散在多个维度管理类里——这里集中成一个入口，跨版本迁移时只改本文件。
 *
 * <p>1.20.1 的签名是 {@code teleportTo(ServerLevel, double, double, double, float, float)}
 * （26.x 相比它多了 {@code Set<Relative>} 和一个 boolean）。本 mod 的所有传送都是
 * "绝对坐标、不甩回末地传送门"，因此直接写死这些实参。
 */
public final class Teleports {

    private Teleports() {}

    /**
     * 把玩家传送到目标维度的绝对坐标。
     *
     * @param player 要传送的玩家（服务端实例）
     * @param level  目标维度（服务端 Level）
     * @param x     目标 X 坐标
     * @param y     目标 Y 坐标
     * @param z     目标 Z 坐标
     * @param yRot   目标视角水平角
     * @param xRot   目标视角俯仰角
     */
    public static void crossDimension(ServerPlayer player, ServerLevel level,
                                      double x, double y, double z,
                                      float yRot, float xRot) {
        // 匹配 1.20.1 签名：teleportTo(ServerLevel, double, double, double, float, float)
        player.teleportTo(level, x, y, z, yRot, xRot);
    }
}
