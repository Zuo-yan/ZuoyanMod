package org.gwfx.zuoyanmod.event;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.item.JackTheRipperScalpelItem;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Zuoyanmod.MODID)
public class JackTheRipperEventHandler {

    private static final Identifier DAMAGE_MODIFIER = Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "jack_damage");
    private static final Identifier ATTACK_SPEED_MODIFIER = Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "jack_attack_speed");

    private static final Map<UUID, Integer> STACK_COUNTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> INVISIBILITY_END_TIME = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> COOLDOWN_END_TIME = new ConcurrentHashMap<>();

    private static final int INVISIBILITY_DURATION_TICKS = 1200;
    private static final long INVISIBILITY_DURATION_MS = 60000L;
    private static final long COOLDOWN_DURATION_MS = 60000L;

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer)) return;
        if (killer.level().isClientSide()) return;

        ItemStack heldItem = killer.getMainHandItem();
        if (!(heldItem.getItem() instanceof JackTheRipperScalpelItem)) return;

        LivingEntity target = event.getEntity();
        if (!(target instanceof Player || target instanceof net.minecraft.world.entity.Mob)) return;

        UUID playerUUID = killer.getUUID();
        long now = System.currentTimeMillis();
        long cooldownEnd = COOLDOWN_END_TIME.getOrDefault(playerUUID, 0L);

        if (killer.hasEffect(MobEffects.INVISIBILITY)) {
            int currentStack = STACK_COUNTS.getOrDefault(playerUUID, 1) + 1;
            STACK_COUNTS.put(playerUUID, currentStack);
            updateAttributes(killer, currentStack);
            return;
        }

        if (now < cooldownEnd) {
            return;
        }

        killer.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INVISIBILITY_DURATION_TICKS, 0, false, false, true));
        STACK_COUNTS.put(playerUUID, 1);
        INVISIBILITY_END_TIME.put(playerUUID, now + INVISIBILITY_DURATION_MS);
        COOLDOWN_END_TIME.put(playerUUID, now + INVISIBILITY_DURATION_MS + COOLDOWN_DURATION_MS);
        updateAttributes(killer, 1);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        UUID playerUUID = player.getUUID();
        int stackCount = STACK_COUNTS.getOrDefault(playerUUID, 0);
        if (stackCount <= 0) return;

        boolean holdingScalpel = player.getMainHandItem().getItem() instanceof JackTheRipperScalpelItem;
        boolean hasInvisibility = player.hasEffect(MobEffects.INVISIBILITY);
        long now = System.currentTimeMillis();
        long invisibilityEnd = INVISIBILITY_END_TIME.getOrDefault(playerUUID, 0L);
        long cooldownEnd = COOLDOWN_END_TIME.getOrDefault(playerUUID, 0L);

        if (!holdingScalpel || !hasInvisibility || now >= invisibilityEnd) {
            resetAttributes(player);
        } else {
            updateAttributes(player, stackCount);
        }

        if (!hasInvisibility && now >= cooldownEnd) {
            STACK_COUNTS.remove(playerUUID);
            INVISIBILITY_END_TIME.remove(playerUUID);
            COOLDOWN_END_TIME.remove(playerUUID);
        }
    }

    private static void updateAttributes(Player player, int stackCount) {
        float multiplier = (float) Math.pow(2, stackCount) - 1;

        if (player.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            player.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(DAMAGE_MODIFIER);
            player.getAttribute(Attributes.ATTACK_DAMAGE).addTransientModifier(
                    new AttributeModifier(DAMAGE_MODIFIER, multiplier, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            );
        }
        if (player.getAttribute(Attributes.ATTACK_SPEED) != null) {
            player.getAttribute(Attributes.ATTACK_SPEED).removeModifier(ATTACK_SPEED_MODIFIER);
            player.getAttribute(Attributes.ATTACK_SPEED).addTransientModifier(
                    new AttributeModifier(ATTACK_SPEED_MODIFIER, multiplier, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            );
        }
    }

    private static void resetAttributes(Player player) {
        if (player.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            player.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(DAMAGE_MODIFIER);
        }
        if (player.getAttribute(Attributes.ATTACK_SPEED) != null) {
            player.getAttribute(Attributes.ATTACK_SPEED).removeModifier(ATTACK_SPEED_MODIFIER);
        }
    }
}