package org.gwfx.zuoyanmod.upgrade;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.gwfx.zuoyanmod.Config;
import org.gwfx.zuoyanmod.Zuoyanmod;

/**
 * 经验升级系统的事件挂点（全部服务端侧）：
 * <ul>
 *   <li>登录/重生：重算属性 modifier + 推一份档案快照给客户端（HUD/界面只读快照）</li>
 *   <li>伤害结算（{@code Pre}，护甲已算完）：叠加"伤害减免"等级的乘法减免；
 *       若攻击者是灌能中的玩家，消费灌能给目标挂分子离解</li>
 *   <li>服务端 tick：推进抓取拽拉与绝对零度冰冻两段登记表</li>
 * </ul>
 */
@EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class UpgradeServerEvents {

    private UpgradeServerEvents() {}

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UpgradeManager.applyAttributes(player);
            UpgradeManager.sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Attachment 的 copyOnDeath 已把等级带过来，这里只需把 transient modifier 补回去
            UpgradeManager.applyAttributes(player);
            UpgradeManager.sync(player);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!Config.upgradeEnabled) {
            return;
        }
        DamageSource source = event.getSource();

        // 攻击者侧：灌能中的近战命中 → 分子离解
        if (source.getEntity() instanceof ServerPlayer attacker
                && source.getDirectEntity() == source.getEntity()
                && event.getEntity() != attacker) {
            UpgradeManager.consumeDissociation(attacker, event.getEntity());
        }

        // 受害者侧：伤害减免（护甲与抗性结算之后的乘法减免）
        if (event.getEntity() instanceof Player victim && !victim.level().isClientSide()) {
            int level = UpgradeData.of(victim).level(UpgradeType.DAMAGE_REDUCTION.ordinal());
            if (level > 0) {
                float reduction = (float) (UpgradeType.DAMAGE_REDUCTION.totalBonus(level));
                event.setNewDamage(event.getNewDamage() * (1.0F - reduction));
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!Config.upgradeEnabled) {
            return;
        }
        UpgradeManager.tick(event.getServer());
        // 天赋冷却结束的玩家推一条模组 toast（HUD 不常驻冷却显示）
        UpgradeManager.pollReadyToasts(event.getServer());
    }
}
