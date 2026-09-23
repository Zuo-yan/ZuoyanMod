package org.gwfx.zuoyanmod.event;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.item.JackTheRipperScalpelItem;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 开膛手杰克手术刀：击杀触发隐身与倍率叠加。
 * 1.20.1 适配：AttributeModifier ID 由 Identifier 换回 UUID（ADD_MULTIPLIED_TOTAL→MULTIPLY_TOTAL）。
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID)
public class JackTheRipperEventHandler {

    /** 1.20.1 的属性修改器用 UUID 标识（26.x 是 ResourceLocation） */
    private static final UUID DAMAGE_MODIFIER = UUID.fromString("c3d4e5f6-8a9b-4f0a-9b1b-6f7a8b9cadb5");
    private static final UUID ATTACK_SPEED_MODIFIER = UUID.fromString("d4e5f6a7-9b0c-4a1b-8c2c-7a8b9cadbec6");

    private static final Map<UUID, Integer> STACK_COUNTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> INVISIBILITY_END_TIME = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> COOLDOWN_END_TIME = new ConcurrentHashMap<>();

    private static final int INVISIBILITY_DURATION_TICKS = 1200;
    private static final long INVISIBILITY_DURATION_MS = 60000L;
    private static final long COOLDOWN_DURATION_MS = 60000L;

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer)) return;
        if (killer.level().isClientSide) return;

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
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide) return;

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

        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.removeModifier(DAMAGE_MODIFIER);
            attackDamage.addTransientModifier(
                    new AttributeModifier(DAMAGE_MODIFIER, "jack_damage", multiplier, AttributeModifier.Operation.MULTIPLY_TOTAL)
            );
        }
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(ATTACK_SPEED_MODIFIER);
            attackSpeed.addTransientModifier(
                    new AttributeModifier(ATTACK_SPEED_MODIFIER, "jack_attack_speed", multiplier, AttributeModifier.Operation.MULTIPLY_TOTAL)
            );
        }
    }

    private static void resetAttributes(Player player) {
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.removeModifier(DAMAGE_MODIFIER);
        }
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(ATTACK_SPEED_MODIFIER);
        }
    }
}
