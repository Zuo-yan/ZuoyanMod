package org.gwfx.zuoyanmod.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.item.SpaceAnchorItem;

/**
 * 空间锚点的被动：手持时受到致命伤害会抵挡并回溯到锚点。
 * <p>
 * 与名刀司命的共存规则见 {@link FatalProtection}：名刀司命是 HIGH 优先级先行判定，
 * 空间锚点是 NORMAL 优先级兜底；同一次伤害只允许一个效果生效。
 * 传送不在伤害事件内执行，排队到玩家 tick 末尾再落地。
 * <p>
 * 1.20.1 适配：LivingIncomingDamageEvent→{@code LivingHurtEvent}，
 * PlayerTickEvent.Post→{@code TickEvent.PlayerTickEvent}(END)。
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class SpaceAnchorEventHandler {

    private SpaceAnchorEventHandler() {}

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingHurt(LivingHurtEvent event) {
        // 名刀司命（HIGH）已经处理过这次伤害
        if (event.isCanceled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide || player.isCreative() || player.isSpectator()) {
            return;
        }
        if (FatalProtection.isProtectedThisTick(player) || player.isDeadOrDying()) {
            return;
        }

        // 仅主手/副手持有生效
        ItemStack anchor = SpaceAnchorItem.findHeldAnchor(player);
        if (anchor.isEmpty()) {
            return;
        }
        SpaceAnchorItem.AnchorPos pos = SpaceAnchorItem.readAnchorPos(anchor);
        if (pos == null) {
            return;
        }
        if (SpaceAnchorItem.isOnCooldown(player, anchor)) {
            return;
        }
        if (event.getAmount() < player.getHealth()) {
            return;
        }

        event.setCanceled(true);
        player.setHealth(1.0F);
        FatalProtection.markProtected(player);
        SpaceAnchorItem.markCooldown(player, anchor);

        // 传送排队到本 tick 末尾，避免在伤害事件内做维度切换
        FatalProtection.queueTeleport(player, pos.dimension(), pos.x(), pos.y(), pos.z(), pos.yRot(), pos.xRot());
        player.sendSystemMessage(Component.literal("§b空间锚点 §7- 抵挡致命伤害，正在回溯"));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide) {
            return;
        }
        if (FatalProtection.flushTeleport(player)) {
            player.sendSystemMessage(Component.literal("§b空间锚点 §7- 已回溯至锚点"));
        } else if (FatalProtection.consumeFailedTeleport(player)) {
            player.sendSystemMessage(Component.literal("§7空间锚点回溯失败，锚点维度不可用"));
        }
    }
}
