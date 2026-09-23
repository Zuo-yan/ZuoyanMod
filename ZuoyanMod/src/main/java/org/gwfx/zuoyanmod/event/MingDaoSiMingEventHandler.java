package org.gwfx.zuoyanmod.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.item.ItemRegistry;

/**
 * 名刀司命：抵挡一次致命伤害，原地留 1 HP，冷却 120 秒。
 * <p>
 * 优先级 HIGH：与空间锚点（NORMAL）共存时先判定。
 * 规则：名刀司命是"原地保命"，代价小；空间锚点是"保命 + 强制位移"，代价大。
 * 代价小的先消耗，把代价大的留作兜底。
 * <p>
 * 1.20.1 适配：LivingIncomingDamageEvent→{@code LivingHurtEvent}；
 * DataComponents.CUSTOM_DATA→ItemStack 的 NBT tag。
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class MingDaoSiMingEventHandler {

    private MingDaoSiMingEventHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        // 若其它效果已经取消了这次伤害，不重复抵挡
        if (event.isCanceled()) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }
        if (FatalProtection.isProtectedThisTick(player)) {
            return;
        }

        ItemStack mingDaoStack = getMingDaoSiMingStack(player);
        if (mingDaoStack.isEmpty()) {
            return;
        }

        CompoundTag tag = mingDaoStack.getOrCreateTag();
        long cooldownUntil = tag.getLong("zuoyan_mingdao_cooldown_until");

        if (cooldownUntil > player.level().getGameTime()) {
            return;
        }

        float damage = event.getAmount();
        if (damage < player.getHealth()) {
            return;
        }

        // 成功抵挡致命伤害
        event.setCanceled(true);
        player.setHealth(1.0F);
        // 占用本 tick 的保护名额，空间锚点不再重复消耗
        FatalProtection.markProtected(player);

        tag.putLong("zuoyan_mingdao_cooldown_until", player.level().getGameTime() + 120L * 20L);
        mingDaoStack.setTag(tag);

        player.sendSystemMessage(Component.literal("§6名刀司命触发，抵挡了致命伤害！"));
    }

    private static ItemStack getMingDaoSiMingStack(Player player) {
        if (player.getMainHandItem().is(ItemRegistry.MING_DAO_SI_MING.get())) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().is(ItemRegistry.MING_DAO_SI_MING.get())) {
            return player.getOffhandItem();
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item.is(ItemRegistry.MING_DAO_SI_MING.get())) {
                return item;
            }
        }
        return ItemStack.EMPTY;
    }
}
