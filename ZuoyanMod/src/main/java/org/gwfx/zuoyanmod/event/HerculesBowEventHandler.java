package org.gwfx.zuoyanmod.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.item.HerculesBowItem;

/**
 * 赫拉克勒斯之弓的祝福效果（箭矢命中时结算）。
 * 1.20.1 适配：EntityTypes→{@code EntityType}（create 不带 SpawnReason）；
 * igniteForSeconds→setSecondsOnFire；LivingDamageEvent.Pre→{@code LivingDamageEvent}。
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID)
public class HerculesBowEventHandler {

    /** 刻耳柏洛斯猎犬存活时长（tick）：20 秒 */
    public static final long CERBERUS_HOUND_LIFETIME_TICKS = 20L * 20L;
    private static final String HOUND_DESPAWN_AT_TAG = "CerberusHoundDespawnAt";

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide) return;

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
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide) return;

        ItemStack heldItem = attacker.getMainHandItem();
        if (!(heldItem.getItem() instanceof HerculesBowItem)) return;

        HerculesBowItem.BlessingType blessing = HerculesBowItem.getBlessingType(heldItem);
        if (blessing == null) return;

        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) return;
        if (arrow.getOwner() != attacker) return;

        LivingEntity target = event.getEntity();

        switch (blessing) {
            case ARTEMIS:
                event.setAmount(event.getAmount() * 1.5f);
                break;
            case HELIOS:
                target.setSecondsOnFire(5);
                float armor = target.getArmorValue();
                float armorToughness = (float) target.getAttributeValue(
                        net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
                float damageReduction = armor / (armor + 20 + armorToughness);
                event.setAmount(event.getAmount() * (1.0f + damageReduction));
                break;
            case CERBERUS:
                if (attacker.level() instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 3; i++) {
                        Wolf hound = EntityType.WOLF.create(serverLevel);
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
                event.setAmount(event.getAmount() + additionalDamage);
                break;
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof Wolf wolf)) return;

        CompoundTag data = wolf.getPersistentData();
        if (!data.contains(HOUND_DESPAWN_AT_TAG)) return;

        if (wolf.level().getGameTime() >= data.getLong(HOUND_DESPAWN_AT_TAG)) {
            if (wolf.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF,
                        wolf.getX(), wolf.getY(0.5), wolf.getZ(),
                        8, 0.3, 0.3, 0.3, 0.02);
            }
            wolf.discard();
        }
    }
}
