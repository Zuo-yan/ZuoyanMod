package org.gwfx.zuoyanmod.event;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.item.ItemRegistry;
import org.gwfx.zuoyanmod.util.AccessoryChecks;

@EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class MingDaoSiMingEventHandler {

    private MingDaoSiMingEventHandler() {}

    /**
     * 优先级 HIGH：与空间锚点（NORMAL）共存时先判定。
     * 规则：名刀司命是"原地保命"，代价小；空间锚点是"保命 + 强制位移"，代价大。
     * 代价小的先消耗，把代价大的留作兜底。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        // 若其它效果已经取消了这次伤害，不重复抵挡
        if (event.isCanceled()) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        if (FatalProtection.isProtectedThisTick(player)) {
            return;
        }

        ItemStack mingDaoStack = getMingDaoSiMingStack(player);
        if (mingDaoStack.isEmpty()) {
            return;
        }

        CustomData customData = mingDaoStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        long cooldownUntil = tag.getLong("zuoyan_mingdao_cooldown_until").orElse(0L);

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
        mingDaoStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        player.sendSystemMessage(Component.literal("§6名刀司命触发，抵挡了致命伤害！"));
    }

    private static ItemStack getMingDaoSiMingStack(Player player) {
        return AccessoryChecks.findEquippedStack(player, ItemRegistry.MING_DAO_SI_MING.get());
    }
}