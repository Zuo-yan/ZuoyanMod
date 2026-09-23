package org.gwfx.zuoyanmod.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.item.HerculesBowItem;

@EventBusSubscriber(modid = Zuoyanmod.MODID)
public class HerculesBowEventHandler {

    /** 刻耳柏洛斯猎犬存活时长（tick）：20 秒 */
    public static final long CERBERUS_HOUND_LIFETIME_TICKS = 20L * 20L;
    private static final String HOUND_DESPAWN_AT_TAG = "CerberusHoundDespawnAt";

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof HerculesBowItem) {
                if (HerculesBowItem.getBlessingType(stack) == null) {
                    HerculesBowItem.assignBlessingIfMissing(stack);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide()) return;

        ItemStack heldItem = attacker.getMainHandItem();
        if (!(heldItem.getItem() instanceof HerculesBowItem)) return;

        HerculesBowItem.BlessingType blessing = HerculesBowItem.getBlessingType(heldItem);
        if (blessing == null) return;

        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) return;
        if (arrow.getOwner() != attacker) return;

        LivingEntity target = event.getEntity();

        switch (blessing) {
            case ARTEMIS:
                event.setNewDamage(event.getNewDamage() * 1.5f);
                break;
            case HELIOS:
                target.igniteForSeconds(5);
                float armor = target.getArmorValue();
                float armorToughness = (float) target.getAttributeValue(
                        net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
                float damageReduction = armor / (armor + 20 + armorToughness);
                event.setNewDamage(event.getNewDamage() * (1.0f + damageReduction));
                break;
            case CERBERUS:
                if (attacker.level() instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 3; i++) {
                        Wolf hound = EntityTypes.WOLF.create(serverLevel, EntitySpawnReason.TRIGGERED);
                        if (hound != null) {
                            hound.setPos(target.getX() + (serverLevel.getRandom().nextDouble() - 0.5) * 2,
                                    target.getY(),
                                    target.getZ() + (serverLevel.getRandom().nextDouble() - 0.5) * 2);
                            hound.tame(attacker);
                            hound.setTarget(target);
                            CompoundTag data = hound.getPersistentData();
                            data.putLong(HOUND_DESPAWN_AT_TAG, serverLevel.getGameTime() + CERBERUS_HOUND_LIFETIME_TICKS);
                            serverLevel.addFreshEntity(hound);
                        }
                    }
                }
                break;
            case HIPPOLYTA:
                float lostHealth = target.getMaxHealth() - target.getHealth();
                float additionalDamage = lostHealth * 0.25f;
                event.setNewDamage(event.getNewDamage() + additionalDamage);
                break;
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Wolf wolf)) return;

        CompoundTag data = wolf.getPersistentData();
        if (!data.contains(HOUND_DESPAWN_AT_TAG)) return;

        if (data.getLong(HOUND_DESPAWN_AT_TAG).map(t -> wolf.level().getGameTime() >= t).orElse(false)) {
            if (wolf.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF,
                        wolf.getX(), wolf.getY(0.5), wolf.getZ(),
                        8, 0.3, 0.3, 0.3, 0.02);
            }
            wolf.discard();
        }
    }
}