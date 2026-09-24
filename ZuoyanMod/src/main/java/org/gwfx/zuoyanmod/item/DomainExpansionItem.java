package org.gwfx.zuoyanmod.item;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gwfx.zuoyanmod.world.DomainExpansionDimensionBootstrap;
import org.gwfx.zuoyanmod.world.DomainExpansionDuelManager;
import org.slf4j.Logger;

import java.util.function.Consumer;

public class DomainExpansionItem extends Item {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int COOLDOWN_TICKS = 20 * 5;

    public DomainExpansionItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            return InteractionResult.CONSUME;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ServerLevel domainLevel = DomainExpansionDimensionBootstrap.getOrCreateDomain(serverPlayer.level().getServer());
            LOGGER.info("Domain expansion item used on server. domainLevel={}, player={}", domainLevel, serverPlayer.getGameProfile().name());
            if (domainLevel == null) {
                LOGGER.warn("Domain dimension is not loaded. Check datapack registration.");
                return InteractionResult.FAIL;
            }

            LivingEntity target = findLookTarget(player, 32.0D);
            if (target == null) {
                player.sendSystemMessage(Component.translatable("message.zuoyanmod.domain_expansion.must_target"));
                return InteractionResult.FAIL;
            }
            LOGGER.info("Domain expansion target acquired: {}", target);
            if (!DomainExpansionDuelManager.startDuel(serverPlayer.level().getServer(), serverPlayer, target, domainLevel)) {
                return InteractionResult.FAIL;
            }
            player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
            stack.shrink(1);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.FAIL;
    }

    private LivingEntity findLookTarget(Player player, double range) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 end = eyePos.add(lookVec.scale(range));
        AABB box = player.getBoundingBox().inflate(range).expandTowards(lookVec.scale(range));
        return player.level().getEntitiesOfClass(LivingEntity.class, box, entity -> entity != player && entity.isAlive()).stream()
                .filter(entity -> entity.getBoundingBox().inflate(0.3D).clip(eyePos, end).isPresent())
                .findFirst()
                .orElse(null);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.zuoyanmod.domain_expansion.desc1"));
        tooltip.accept(Component.translatable("item.zuoyanmod.domain_expansion.desc2"));
        tooltip.accept(Component.translatable("item.zuoyanmod.domain_expansion.desc3"));
        tooltip.accept(Component.translatable("item.zuoyanmod.domain_expansion.desc4"));
    }
}
