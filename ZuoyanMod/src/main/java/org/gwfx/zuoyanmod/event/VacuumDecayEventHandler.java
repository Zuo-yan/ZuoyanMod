package org.gwfx.zuoyanmod.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.damage.ZuoyanDamageTypes;
import org.gwfx.zuoyanmod.effect.EffectRegistry;
import org.gwfx.zuoyanmod.item.ItemRegistry;

/**
 * 「真空衰变」的两条伤害侧逻辑。
 * <p>
 * <b>为什么是 {@link LivingDamageEvent}（26.x 的 LivingDamageEvent.Pre）而不是 {@code LivingHurtEvent}</b>：
 * 前者在护甲/抗性**结算之后**触发，所以"伤害降低 80%"拿到的是最终伤害的 80%；
 * 后者在结算之前，减 80% 之后还要再吃一遍护甲减免，实际收益会超过 80%，与描述不符。
 * <p>
 * 1.20.1 适配：getNewDamage/setNewDamage→getAmount/setAmount。
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class VacuumDecayEventHandler {

    /** 【负熵灌注】受到伤害的减免比例（手持时生效） */
    public static final float DAMAGE_REDUCTION = 0.80F;

    /** 【分子离解】由武器附加时的持续时间：60 秒 */
    public static final int DISSOCIATION_TICKS = 60 * 20;

    private VacuumDecayEventHandler() {}

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        applyEntropyInfusion(event, target);
        applyStrike(event, target);
    }

    // ===== 效果① 负熵灌注：手持者受到的伤害 ×0.2 =====

    private static void applyEntropyInfusion(LivingDamageEvent event, LivingEntity target) {
        if (!(target instanceof Player player) || !isHolding(player)) {
            return;
        }
        event.setAmount(event.getAmount() * (1.0F - DAMAGE_REDUCTION));
    }

    // ===== 效果②③ 分子离解 / 对称破缺 =====

    private static void applyStrike(LivingDamageEvent event, LivingEntity target) {
        DamageSource source = event.getSource();

        // 自己黑洞的爆炸伤害不再触发命中逻辑（否则会互相引爆成死循环）
        if (source.is(ZuoyanDamageTypes.VACUUM_DECAY)) {
            return;
        }
        // 只认真正的近战：攻击者必须就是直接伤害来源（排除箭、投射物、反伤）
        if (!(source.getEntity() instanceof Player wielder) || source.getDirectEntity() != wielder) {
            return;
        }
        if (target == wielder || !isHolding(wielder)) {
            return;
        }

        if (target.hasEffect(EffectRegistry.MOLECULAR_DISSOLUTION.get())) {
            // ③ 对称破缺：目标身上已有引信 → 拆掉引信，在目标处开一个黑洞
            target.removeEffect(EffectRegistry.MOLECULAR_DISSOLUTION.get());
            if (wielder.level() instanceof ServerLevel serverLevel) {
                VacuumDecayBlackHoleManager.spawn(serverLevel, target.getBoundingBox().getCenter(), wielder);
            }
            return;
        }

        // ② 分子离解：打上 60 秒引信（复用液态暗物质的同一个效果，名称/表现完全一致）
        target.addEffect(new MobEffectInstance(EffectRegistry.MOLECULAR_DISSOLUTION.get(),
                DISSOCIATION_TICKS, 0, false, false));
    }

    /** 「手持」= 主手或副手 */
    private static boolean isHolding(Player player) {
        return player.getMainHandItem().is(ItemRegistry.VACUUM_DECAY.get())
                || player.getOffhandItem().is(ItemRegistry.VACUUM_DECAY.get());
    }
}
