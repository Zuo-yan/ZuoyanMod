package org.gwfx.zuoyanmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.entity.SuperElectricGatlingPeaEntity;

/**
 * 客户端 → 服务端：玩家抱持超级电能机枪豌豆时的动作指令包（射击 / 放下）。
 */
public record CarriedPeaActionPacket(int action) implements CustomPacketPayload {

    public static final int ACTION_SHOOT = 0;
    public static final int ACTION_PUT_DOWN = 1;

    public static final Type<CarriedPeaActionPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "carried_pea_action"));

    public static final StreamCodec<ByteBuf, CarriedPeaActionPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    CarriedPeaActionPacket::action,
                    CarriedPeaActionPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CarriedPeaActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                AABB box = player.getBoundingBox().inflate(3.0D);
                for (SuperElectricGatlingPeaEntity pea : player.level().getEntitiesOfClass(SuperElectricGatlingPeaEntity.class, box)) {
                    if (pea.getCarrierId() == player.getId()) {
                        if (packet.action() == ACTION_SHOOT) {
                            pea.playerShoot(player);
                        } else if (packet.action() == ACTION_PUT_DOWN) {
                            pea.putDown(player);
                        }
                        break;
                    }
                }
            }
        });
    }
}
