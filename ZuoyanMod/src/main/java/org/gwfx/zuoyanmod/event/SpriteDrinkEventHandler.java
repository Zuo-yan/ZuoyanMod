package org.gwfx.zuoyanmod.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.gwfx.zuoyanmod.Zuoyanmod;

import java.util.UUID;

/**
 * 雪碧（元气弹饮料）：持续期间移速 +50%，攻击 +已损失生命，代价是每走 1 格扣 1 HP。
 * 1.20.1 适配：AttributeModifier ID 由 Identifier 换回 UUID；
 * CompoundTag 的 getter 返回原生类型（26.x 的 Optional 风格不适用）。
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class SpriteDrinkEventHandler {

    /** 1.20.1 的属性修改器用 UUID 标识（26.x 是 ResourceLocation） */
    private static final UUID SPEED_MODIFIER = UUID.fromString("a1f2e3d4-6b7c-4d8e-9f9a-4d5e6f7a8b93");
    private static final UUID ATTACK_MODIFIER = UUID.fromString("b2e3f4a5-7c8d-4e9f-8a0a-5e6f7a8b9ca4");

    private SpriteDrinkEventHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide) return;

        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean("zuoyan_sprite_active")) return;

        long endTick = data.getLong("zuoyan_sprite_end_tick");
        if (endTick > 0L && player.level().getGameTime() >= endTick) {
            clear(player);
            player.sendSystemMessage(Component.literal("§7雪碧效果结束"));
            return;
        }

        if (player.getHealth() <= 0.0F) {
            clear(player);
            return;
        }

        applyModifiers(player, data);
        trackMovement(player, data);
    }

    private static void applyModifiers(Player player, CompoundTag data) {
        double healthLoss = data.getFloat("zuoyan_sprite_bonus");
        double speedBonus = 0.5D;
        double attackBonus = healthLoss;

        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_MODIFIER);
            speedAttr.addTransientModifier(new AttributeModifier(SPEED_MODIFIER, "sprite_speed", speedBonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        AttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.removeModifier(ATTACK_MODIFIER);
            attackAttr.addTransientModifier(new AttributeModifier(ATTACK_MODIFIER, "sprite_attack", attackBonus, AttributeModifier.Operation.ADDITION));
        }
    }

    private static void trackMovement(Player player, CompoundTag data) {
        double lastX = data.contains("zuoyan_sprite_last_x") ? data.getDouble("zuoyan_sprite_last_x") : player.getX();
        double lastY = data.contains("zuoyan_sprite_last_y") ? data.getDouble("zuoyan_sprite_last_y") : player.getY();
        double lastZ = data.contains("zuoyan_sprite_last_z") ? data.getDouble("zuoyan_sprite_last_z") : player.getZ();

        double dx = player.getX() - lastX;
        double dy = player.getY() - lastY;
        double dz = player.getZ() - lastZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        data.putDouble("zuoyan_sprite_last_x", player.getX());
        data.putDouble("zuoyan_sprite_last_y", player.getY());
        data.putDouble("zuoyan_sprite_last_z", player.getZ());

        float accumulated = data.getFloat("zuoyan_sprite_walk_acc");
        accumulated += (float) distance;

        if (accumulated >= 1.0F) {
            int steps = (int) accumulated;
            accumulated -= steps;

            float damagePerStep = 1.0F;
            float currentBonus = data.getFloat("zuoyan_sprite_bonus");

            if (player.level() instanceof net.minecraft.server.level.ServerLevel) {
                for (int i = 0; i < steps && player.isAlive(); i++) {
                    player.hurt(player.damageSources().generic(), damagePerStep);
                    currentBonus += damagePerStep;
                }
            }

            data.putFloat("zuoyan_sprite_bonus", currentBonus);
        }

        data.putFloat("zuoyan_sprite_walk_acc", accumulated);
    }

    private static void clear(Player player) {
        CompoundTag data = player.getPersistentData();

        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_MODIFIER);
        }

        AttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.removeModifier(ATTACK_MODIFIER);
        }

        data.remove("zuoyan_sprite_active");
        data.remove("zuoyan_sprite_bonus");
        data.remove("zuoyan_sprite_last_x");
        data.remove("zuoyan_sprite_last_y");
        data.remove("zuoyan_sprite_last_z");
        data.remove("zuoyan_sprite_walk_acc");
        data.remove("zuoyan_sprite_end_tick");
    }
}
