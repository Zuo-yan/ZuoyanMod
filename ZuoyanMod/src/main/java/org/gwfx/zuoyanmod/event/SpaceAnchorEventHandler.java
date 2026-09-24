package org.gwfx.zuoyanmod.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.item.SpaceAnchorItem;

/**
 * 空间锚点的被动：手持时受到致命伤害会抵挡并回溯到锚点。
 * <p>
 * 与名刀司命的共存规则见 {@link FatalProtection}：名刀司命是 HIGH 优先级先行判定，
 * 空间锚点是 NORMAL 优先级兜底；同一次伤害只允许一个效果生效。
 * 传送不在伤害事件内执行，排队到 {@link PlayerTickEvent.Post} 再落地。
 */
@EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class SpaceAnchorEventHandler {

    private SpaceAnchorEventHandler() {}

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        // 名刀司命（HIGH）已经处理过这次伤害
        if (event.isCanceled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide() || player.isCreative() || player.isSpectator()) {
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
        player.sendSystemMessage(Component.translatable("message.zuoyanmod.space_anchor.saving_fatal"));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (FatalProtection.flushTeleport(player)) {
            player.sendSystemMessage(Component.translatable("message.zuoyanmod.space_anchor.recalled"));
        } else if (FatalProtection.consumeFailedTeleport(player)) {
            player.sendSystemMessage(Component.translatable("message.zuoyanmod.space_anchor.recall_failed"));
        }
    }
}
