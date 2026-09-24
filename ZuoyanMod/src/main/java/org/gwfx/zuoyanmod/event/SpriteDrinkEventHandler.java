package org.gwfx.zuoyanmod.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;

@EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class SpriteDrinkEventHandler {

    private static final Identifier SPEED_MODIFIER = Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "sprite_speed");
    private static final Identifier ATTACK_MODIFIER = Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "sprite_attack");

    private SpriteDrinkEventHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean("zuoyan_sprite_active").orElse(false)) return;

        long endTick = data.getLong("zuoyan_sprite_end_tick").orElse(0L);
        if (endTick > 0L && player.level().getGameTime() >= endTick) {
            clear(player);
            player.sendSystemMessage(Component.translatable("message.zuoyanmod.sprite_drink.ended"));
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
        double healthLoss = data.getFloat("zuoyan_sprite_bonus").orElse(0.0F);
        double speedBonus = 0.5D;
        double attackBonus = healthLoss;

        var speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_MODIFIER);
            speedAttr.addTransientModifier(new AttributeModifier(SPEED_MODIFIER, speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }

        var attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.removeModifier(ATTACK_MODIFIER);
            attackAttr.addTransientModifier(new AttributeModifier(ATTACK_MODIFIER, attackBonus, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void trackMovement(Player player, CompoundTag data) {
        double lastX = data.getDouble("zuoyan_sprite_last_x").orElse(player.getX());
        double lastY = data.getDouble("zuoyan_sprite_last_y").orElse(player.getY());
        double lastZ = data.getDouble("zuoyan_sprite_last_z").orElse(player.getZ());

        double dx = player.getX() - lastX;
        double dy = player.getY() - lastY;
        double dz = player.getZ() - lastZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        data.putDouble("zuoyan_sprite_last_x", player.getX());
        data.putDouble("zuoyan_sprite_last_y", player.getY());
        data.putDouble("zuoyan_sprite_last_z", player.getZ());

        float accumulated = data.getFloat("zuoyan_sprite_walk_acc").orElse(0.0F);
        accumulated += (float) distance;

        if (accumulated >= 1.0F) {
            int steps = (int) accumulated;
            accumulated -= steps;

            float damagePerStep = 1.0F;
            float currentBonus = data.getFloat("zuoyan_sprite_bonus").orElse(0.0F);

            if (player.level() instanceof ServerLevel serverLevel) {
                for (int i = 0; i < steps && player.isAlive(); i++) {
                    player.hurtServer(serverLevel, player.damageSources().generic(), damagePerStep);
                    currentBonus += damagePerStep;
                }
            }

            data.putFloat("zuoyan_sprite_bonus", currentBonus);
        }

        data.putFloat("zuoyan_sprite_walk_acc", accumulated);
    }

    private static void clear(Player player) {
        CompoundTag data = player.getPersistentData();

        var speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_MODIFIER);
        }

        var attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
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