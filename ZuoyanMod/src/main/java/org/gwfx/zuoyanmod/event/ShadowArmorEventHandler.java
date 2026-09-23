package org.gwfx.zuoyanmod.event;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.item.BeimingBlade;
import org.gwfx.zuoyanmod.item.ItemRegistry;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Zuoyanmod.MODID)
public class ShadowArmorEventHandler {

    private static final Identifier WIND_HEALTH_ID = Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "wind_health");
    private static final Identifier WIND_SPEED_ID = Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "wind_speed");

    private static final Map<UUID, Long> SHADOW_BLADE_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ABSORPTION_REFRESH_TIMES = new ConcurrentHashMap<>();
    private static final Set<UUID> FULL_SET_FLYING_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> BATTLE_FRENZY_LAST_ATTACK = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> FRENZY_MAX_HEALTH_BONUS = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();
    private static int tickCounter = 0;

    // 常量配置
    private static final int SHADOW_BLADE_COOLDOWN_TICKS = 200;
    private static final float SHADOW_BLADE_DAMAGE_MULTIPLIER = 0.5f;
    private static final float SHADOW_BLADE_TRIGGER_CHANCE = 0.3f;
    private static final float LOW_HEALTH_THRESHOLD = 0.5f;
    private static final int BLINDNESS_ON_ATTACK_DURATION = 100;
    private static final float DAMAGE_NEGATE_CHANCE = 0.5f;

    private static final int ABSORPTION_REFRESH_TICKS = 200;
    private static final int EFFECT_REFRESH_INTERVAL = 40;
    private static final int EFFECT_DURATION = 100;
    private static final int NIGHT_VISION_DURATION = 400;
    private static final float DAMAGE_REDUCTION = 0.3f;
    private static final float DAMAGE_BOOST = 0.3f;

    private static final float WIND_LOW_HEALTH_THRESHOLD = 0.1f;
    private static final float RESISTANCE_REDUCTION = 0.2f;

    private static final float TELEPORT_DAMAGE_THRESHOLD = 0.45f;
    private static final int TELEPORT_RANGE = 20;

    private static final float SET_BONUS_DAMAGE_PERCENT = 0.1f;

    private static final int FRENZY_EXIT_TICKS = 200;
    private static final float FRENZY_SELF_DAMAGE_PERCENT = 0.1f;
    private static final float FRENZY_BONUS_DAMAGE_RATIO = 0.6f;
    private static final float FRENZY_MAX_HEALTH_PER_HIT = 2.0f;

    private static void teleportRandomly(LivingEntity entity, ServerLevel level) {
        double x = entity.getX() + (RANDOM.nextDouble() * 2 - 1) * TELEPORT_RANGE;
        double y = entity.getY() + (RANDOM.nextDouble() * 2 - 1) * TELEPORT_RANGE / 2;
        double z = entity.getZ() + (RANDOM.nextDouble() * 2 - 1) * TELEPORT_RANGE;
        y = Math.max(level.getMinY() + 1, Math.min(y, level.getMaxY() - 1));
        entity.setPos(x, y, z);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        tickCounter++;

        if (player.isDeadOrDying()) {
            FRENZY_MAX_HEALTH_BONUS.remove(player.getUUID());
            BATTLE_FRENZY_LAST_ATTACK.remove(player.getUUID());
            if (FULL_SET_FLYING_PLAYERS.contains(player.getUUID())) {
                FULL_SET_FLYING_PLAYERS.remove(player.getUUID());
            }
            return;
        }

        // ===== 战之狂热超时检查 =====
        if (BATTLE_FRENZY_LAST_ATTACK.containsKey(player.getUUID())) {
            long currentTick = player.level().getGameTime();
            long lastAttack = BATTLE_FRENZY_LAST_ATTACK.get(player.getUUID());
            if (currentTick - lastAttack >= FRENZY_EXIT_TICKS) {
                float bonus = FRENZY_MAX_HEALTH_BONUS.getOrDefault(player.getUUID(), 0.0f);
                if (bonus > 0 && player.getAttribute(Attributes.MAX_HEALTH) != null) {
                    float newMax = player.getMaxHealth() - bonus;
                    player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(
                            player.getAttributeBaseValue(Attributes.MAX_HEALTH) - bonus);
                    if (player.getHealth() > newMax) {
                        player.setHealth(newMax);
                    }
                }
                FRENZY_MAX_HEALTH_BONUS.remove(player.getUUID());
                BATTLE_FRENZY_LAST_ATTACK.remove(player.getUUID());
                player.sendSystemMessage(Component.literal("§4§l战之狂热 §7- 狂热状态消退..."));
            }
        }

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

        // （北冥狂刃的攻击力/攻速已改为物品自带剑类属性，见 ItemRegistry，不再由事件附加）

        // ===== 疾风护腿生命与移速加成 =====
        boolean hasLeggings = !leggings.isEmpty() && leggings.is(ItemRegistry.WIND_LEGGINGS.get());
        var healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        var moveAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (hasLeggings) {
            if (healthAttr != null && !healthAttr.hasModifier(WIND_HEALTH_ID)) {
                healthAttr.addTransientModifier(new AttributeModifier(WIND_HEALTH_ID, 20.0, AttributeModifier.Operation.ADD_VALUE));
            }
            if (moveAttr != null && !moveAttr.hasModifier(WIND_SPEED_ID)) {
                moveAttr.addTransientModifier(new AttributeModifier(WIND_SPEED_ID, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        } else {
            if (healthAttr != null) healthAttr.removeModifier(WIND_HEALTH_ID);
            if (moveAttr != null) moveAttr.removeModifier(WIND_SPEED_ID);
        }

        // ===== 检测全套装备 =====
        boolean hasFullSet = !helmet.isEmpty() && helmet.is(ItemRegistry.SHADOW_HELMET.get())
                && !chestplate.isEmpty() && chestplate.is(ItemRegistry.SHENG_TIAN_CHESTPLATE.get())
                && hasLeggings
                && !boots.isEmpty() && boots.is(ItemRegistry.WALKER_BOOTS.get());

        // ===== 全套效果：创造飞行 =====
        boolean wasFlying = FULL_SET_FLYING_PLAYERS.contains(player.getUUID());
        if (hasFullSet) {
            player.getAbilities().mayfly = true;
            FULL_SET_FLYING_PLAYERS.add(player.getUUID());
        } else if (wasFlying) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            FULL_SET_FLYING_PLAYERS.remove(player.getUUID());
        }
        if (hasFullSet || wasFlying) {
            player.onUpdateAbilities();
        }

        // ===== 暗影头盔被动效果 =====
        if (!helmet.isEmpty() && helmet.is(ItemRegistry.SHADOW_HELMET.get())) {
            if (tickCounter % EFFECT_REFRESH_INTERVAL == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, EFFECT_DURATION, 0, false, false));
            }

            float healthPercent = player.getHealth() / player.getMaxHealth();
            if (healthPercent < LOW_HEALTH_THRESHOLD) {
                if (tickCounter % EFFECT_REFRESH_INTERVAL == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, EFFECT_DURATION, 0, false, true));
                }
            } else {
                if (player.hasEffect(MobEffects.BLINDNESS)) {
                    player.removeEffect(MobEffects.BLINDNESS);
                }
            }
        }

        // ===== 胜天战甲被动效果 =====
        if (!chestplate.isEmpty() && chestplate.is(ItemRegistry.SHENG_TIAN_CHESTPLATE.get())) {
            if (tickCounter % EFFECT_REFRESH_INTERVAL == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_DURATION, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, EFFECT_DURATION, 4, false, false));
            }

            long currentTick = player.level().getGameTime();
            long lastAbsorption = ABSORPTION_REFRESH_TIMES.getOrDefault(player.getUUID(), -ABSORPTION_REFRESH_TICKS - 1L);
            if (currentTick - lastAbsorption >= ABSORPTION_REFRESH_TICKS) {
                player.removeEffect(MobEffects.ABSORPTION);
                float maxHealth = player.getMaxHealth();
                int amplifier = Math.max(0, (int)(maxHealth / 4.0f) - 1);
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ABSORPTION_REFRESH_TICKS + 20, amplifier, false, true));
                ABSORPTION_REFRESH_TIMES.put(player.getUUID(), currentTick);
            }
        }

        // ===== 疾风护腿被动效果 =====
        if (hasLeggings) {
            float healthPercent = player.getHealth() / player.getMaxHealth();
            if (healthPercent < WIND_LOW_HEALTH_THRESHOLD) {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, EFFECT_DURATION, 4, false, true));
            }
        }

        // ===== 行者之靴被动效果 =====
        if (!boots.isEmpty() && boots.is(ItemRegistry.WALKER_BOOTS.get())) {
            if (tickCounter % EFFECT_REFRESH_INTERVAL == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, EFFECT_DURATION, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, EFFECT_DURATION, 0, false, true));
            }
        }
    }

    // ===== 疾风掠影：免疫掉落伤害 =====
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (player.level().isClientSide()) return;

            ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
            if (!leggings.isEmpty() && leggings.is(ItemRegistry.WIND_LEGGINGS.get())) {
                float healthPercent = player.getHealth() / player.getMaxHealth();
                if (healthPercent < WIND_LOW_HEALTH_THRESHOLD) {
                    event.setCanceled(true);
                }
            }
        }
    }

    // ===== 受到伤害免伤（暗影庇护） =====
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.isEmpty() && helmet.is(ItemRegistry.SHADOW_HELMET.get())) {
            if (player.hasEffect(MobEffects.BLINDNESS) && RANDOM.nextFloat() < DAMAGE_NEGATE_CHANCE) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("§8§l暗影庇护 §7- 完全抵挡了一次攻击!"));
            }
        }
    }

    // ===== 伤害结算计算（攻击加成与防御减免） =====
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        DamageSource source = event.getSource();

        // 玩家作为攻击者
        if (source.getEntity() instanceof Player player) {
            if (player.level().isClientSide() || player.isDeadOrDying()) return;

            ItemStack mainHand = player.getMainHandItem();

            // ===== 北冥狂刃：战之狂热 =====
            if (!mainHand.isEmpty() && mainHand.getItem() instanceof BeimingBlade) {
                long currentTick = player.level().getGameTime();
                boolean wasInFrenzy = BATTLE_FRENZY_LAST_ATTACK.containsKey(player.getUUID());
                BATTLE_FRENZY_LAST_ATTACK.put(player.getUUID(), currentTick);

                if (!wasInFrenzy) {
                    player.sendSystemMessage(Component.literal("§4§l战之狂热 §7- 进入狂热状态！"));
                }

                float currentBonus = FRENZY_MAX_HEALTH_BONUS.getOrDefault(player.getUUID(), 0.0f);
                float oldMaxHealth = player.getMaxHealth();
                var maxHpAttr = player.getAttribute(Attributes.MAX_HEALTH);
                if (maxHpAttr != null) {
                    maxHpAttr.setBaseValue(player.getAttributeBaseValue(Attributes.MAX_HEALTH) + FRENZY_MAX_HEALTH_PER_HIT);
                }
                float actualGain = player.getMaxHealth() - oldMaxHealth;
                player.setHealth(player.getHealth() + actualGain);
                FRENZY_MAX_HEALTH_BONUS.put(player.getUUID(), currentBonus + actualGain);

                float selfDamage = player.getMaxHealth() * FRENZY_SELF_DAMAGE_PERCENT;
                player.hurt(player.damageSources().magic(), selfDamage);

                float missingHealth = player.getMaxHealth() - player.getHealth();
                float bonusDamage = missingHealth * FRENZY_BONUS_DAMAGE_RATIO;
                if (bonusDamage > 0) {
                    event.setNewDamage(event.getNewDamage() + bonusDamage);
                }

                player.sendSystemMessage(Component.literal("§4§l战之狂热 §7- 扣除 " + String.format("%.1f", selfDamage) + " HP，造成 " + String.format("%.1f", bonusDamage) + " 额外伤害!"));
            }

            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
            ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

            boolean hasFullSet = !helmet.isEmpty() && helmet.is(ItemRegistry.SHADOW_HELMET.get())
                    && !chestplate.isEmpty() && chestplate.is(ItemRegistry.SHENG_TIAN_CHESTPLATE.get())
                    && !leggings.isEmpty() && leggings.is(ItemRegistry.WIND_LEGGINGS.get())
                    && !boots.isEmpty() && boots.is(ItemRegistry.WALKER_BOOTS.get());

            // 纵横三千界：10%目标最大生命值伤害
            if (hasFullSet) {
                LivingEntity target = event.getEntity();
                float bonusDamage = target.getMaxHealth() * SET_BONUS_DAMAGE_PERCENT;
                event.setNewDamage(event.getNewDamage() + bonusDamage);
                player.sendSystemMessage(Component.literal("§e§l纵横三千界 §7- 造成 " + String.format("%.1f", bonusDamage) + " 额外伤害!"));
            }

            // 胜天之怒：生命>50%时30%增伤
            if (!chestplate.isEmpty() && chestplate.is(ItemRegistry.SHENG_TIAN_CHESTPLATE.get())) {
                float healthPercent = player.getHealth() / player.getMaxHealth();
                if (healthPercent > LOW_HEALTH_THRESHOLD) {
                    event.setNewDamage(event.getNewDamage() * (1.0f + DAMAGE_BOOST));
                }
            }

            // 暗影之刃与暗影侵蚀
            if (!helmet.isEmpty() && helmet.is(ItemRegistry.SHADOW_HELMET.get()) && player.hasEffect(MobEffects.BLINDNESS)) {
                long currentTick = player.level().getGameTime();
                long lastUsed = SHADOW_BLADE_COOLDOWNS.getOrDefault(player.getUUID(), -SHADOW_BLADE_COOLDOWN_TICKS - 1L);

                if (currentTick - lastUsed >= SHADOW_BLADE_COOLDOWN_TICKS && RANDOM.nextFloat() < SHADOW_BLADE_TRIGGER_CHANCE) {
                    float bonusDamage = event.getNewDamage() * SHADOW_BLADE_DAMAGE_MULTIPLIER;
                    event.setNewDamage(event.getNewDamage() + bonusDamage);
                    SHADOW_BLADE_COOLDOWNS.put(player.getUUID(), currentTick);
                    event.getEntity().addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_ON_ATTACK_DURATION, 0));
                    player.sendSystemMessage(Component.literal("§8§l暗影之刃 §7- 造成 " + String.format("%.1f", bonusDamage) + " 额外伤害!"));
                }
            }
        }

        // 玩家作为受击者
        if (event.getEntity() instanceof Player player) {
            if (player.level().isClientSide()) return;

            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
            ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

            boolean hasFullSet = !helmet.isEmpty() && helmet.is(ItemRegistry.SHADOW_HELMET.get())
                    && !chestplate.isEmpty() && chestplate.is(ItemRegistry.SHENG_TIAN_CHESTPLATE.get())
                    && !leggings.isEmpty() && leggings.is(ItemRegistry.WIND_LEGGINGS.get())
                    && !boots.isEmpty() && boots.is(ItemRegistry.WALKER_BOOTS.get());

            // 行至空元：满血受到超过45%伤害时随机传送
            if (hasFullSet && player.getHealth() >= player.getMaxHealth()) {
                float damageThreshold = player.getMaxHealth() * TELEPORT_DAMAGE_THRESHOLD;
                if (event.getNewDamage() >= damageThreshold && player.level() instanceof ServerLevel serverLevel) {
                    teleportRandomly(player, serverLevel);
                    Entity attackerEntity = event.getSource().getEntity();
                    if (attackerEntity instanceof LivingEntity livingAttacker) {
                        teleportRandomly(livingAttacker, serverLevel);
                    }
                    player.sendSystemMessage(Component.literal("§f§l行至空元 §7- 受到致命威胁，空间扭曲！"));
                }
            }

            // 抗性提升 II：20%减伤（疾风护腿）
            if (!leggings.isEmpty() && leggings.is(ItemRegistry.WIND_LEGGINGS.get())) {
                event.setNewDamage(event.getNewDamage() * (1.0f - RESISTANCE_REDUCTION));
            }

            // 不屈：生命<50%时30%减伤
            if (!chestplate.isEmpty() && chestplate.is(ItemRegistry.SHENG_TIAN_CHESTPLATE.get())) {
                float healthPercent = player.getHealth() / player.getMaxHealth();
                if (healthPercent < LOW_HEALTH_THRESHOLD) {
                    event.setNewDamage(event.getNewDamage() * (1.0f - DAMAGE_REDUCTION));
                }
            }
        }
    }
}