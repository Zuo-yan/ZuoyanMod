package org.gwfx.zuoyanmod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 空间锚点：潜行右键记录坐标，再次潜行右键传送回锚点；
 * 手持时受到致命伤害会抵挡并回溯到锚点（冷却 120 秒）。
 * <p>
 * 致命抵挡部分与名刀司命共用同一套事件，由 {@link org.gwfx.zuoyanmod.event.SpaceAnchorEventHandler}
 * 通过 {@code event.isCanceled()} 判断名刀是否已经介入，避免重复触发。
 * <p>
 * 1.20.1：物品数据直接走 ItemStack NBT（26.x 的 CustomData 组件在 1.20.1 不存在）。
 */
public class SpaceAnchorItem extends Item {

    private static final String KEY_SET = "zuoyan_anchor_set";
    private static final String KEY_DIM = "zuoyan_anchor_dim";
    private static final String KEY_X = "zuoyan_anchor_x";
    private static final String KEY_Y = "zuoyan_anchor_y";
    private static final String KEY_Z = "zuoyan_anchor_z";
    private static final String KEY_Y_ROT = "zuoyan_anchor_yrot";
    private static final String KEY_X_ROT = "zuoyan_anchor_xrot";
    private static final String KEY_COOLDOWN_UNTIL = "zuoyan_anchor_cd_until";

    /** 冷却时长（tick），与名刀司命一致为 120 秒 */
    public static final int COOLDOWN_TICKS = 120 * 20;

    /** 物品可"使用"的最大时长，用于区分短按/长按 */
    private static final int USE_DURATION = 72000;
    /** 长按超过该 tick 数视为"强制覆盖保存" */
    private static final int OVERRIDE_TICKS = 20;

    public SpaceAnchorItem(Properties properties) {
        super(properties);
    }

    // ===== 交互：潜行短按 = 保存/传送，潜行长按 = 覆盖保存 =====

    // 1.20.1 的 Item#use 返回 InteractionResultHolder<ItemStack>
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        if (USE_DURATION - remainingUseDuration < OVERRIDE_TICKS) {
            return;
        }
        ItemStack anchor = findHeldAnchor(player);
        if (!anchor.isEmpty()) {
            saveAnchor(player, anchor);
            player.sendSystemMessage(Component.literal("§b空间锚点 §7- 锚点已重新校准"));
        }
        player.releaseUsingItem();
    }

    // 1.20.1 的 releaseUsing 返回 void（26.x 是 boolean）
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        // 长按已经由 onUseTick 处理，这里只处理短按
        if (USE_DURATION - timeLeft >= OVERRIDE_TICKS) {
            return;
        }
        ItemStack anchor = findHeldAnchor(player);
        if (anchor.isEmpty()) {
            return;
        }
        if (!hasAnchor(anchor)) {
            saveAnchor(player, anchor);
            player.sendSystemMessage(Component.literal("§b空间锚点 §7- 已记录当前坐标"));
            return;
        }
        if (teleportToAnchor(player, anchor)) {
            player.sendSystemMessage(Component.literal("§b空间锚点 §7- 已回溯至锚点"));
        }
    }

    // ===== 锚点数据读写（ItemStack NBT） =====

    public static boolean hasAnchor(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(KEY_SET);
    }

    public static void saveAnchor(Player player, ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(KEY_SET, true);
        tag.putString(KEY_DIM, player.level().dimension().location().toString());
        tag.putDouble(KEY_X, player.getX());
        tag.putDouble(KEY_Y, player.getY());
        tag.putDouble(KEY_Z, player.getZ());
        tag.putFloat(KEY_Y_ROT, player.getYRot());
        tag.putFloat(KEY_X_ROT, player.getXRot());
    }

    /** 已记录的锚点坐标；未设置返回 null */
    public record AnchorPos(String dimension, double x, double y, double z, float yRot, float xRot) {}

    public static AnchorPos readAnchorPos(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getBoolean(KEY_SET)) {
            return null;
        }
        return new AnchorPos(
                tag.getString(KEY_DIM),
                tag.getDouble(KEY_X),
                tag.getDouble(KEY_Y),
                tag.getDouble(KEY_Z),
                tag.getFloat(KEY_Y_ROT),
                tag.getFloat(KEY_X_ROT)
        );
    }

    /** 立即传送到锚点，成功返回 true；失败时玩家会收到原因提示 */
    public static boolean teleportToAnchor(Player player, ItemStack stack) {
        AnchorPos pos = readAnchorPos(stack);
        if (pos == null) {
            player.sendSystemMessage(Component.literal("§7空间锚点尚未记录坐标"));
            return false;
        }
        return teleportNow(player, pos);
    }

    /** 真正执行传送；供主动使用与致命回溯共用 */
    public static boolean teleportNow(Player player, AnchorPos pos) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel currentLevel)) {
            return false;
        }
        MinecraftServer server = currentLevel.getServer();
        if (server == null) {
            return false;
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(pos.dimension());
        if (dimensionId == null) {
            player.sendSystemMessage(Component.literal("§7空间锚点所在的维度不可用"));
            return false;
        }
        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
        ServerLevel target = server.getLevel(dimensionKey);
        if (target == null) {
            player.sendSystemMessage(Component.literal("§7空间锚点所在的维度不可用"));
            return false;
        }

        // 传送签名随版本变化，统一走 platform 适配层
        org.gwfx.zuoyanmod.platform.Teleports.crossDimension(
                serverPlayer, target, pos.x(), pos.y(), pos.z(), pos.yRot(), pos.xRot());
        serverPlayer.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0F, 1.0F);
        return true;
    }

    // ===== 冷却 =====

    public static boolean isOnCooldown(Player player, ItemStack stack) {
        CompoundTag tag = stack.getTag();
        long until = tag == null ? 0L : tag.getLong(KEY_COOLDOWN_UNTIL);
        return until > player.level().getGameTime();
    }

    public static void markCooldown(Player player, ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong(KEY_COOLDOWN_UNTIL, player.level().getGameTime() + COOLDOWN_TICKS);
    }

    /** 仅主手/副手持有才生效 */
    public static ItemStack findHeldAnchor(Player player) {
        if (player.getMainHandItem().getItem() instanceof SpaceAnchorItem) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() instanceof SpaceAnchorItem) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§b空间锚点"));
        tooltip.add(Component.literal("§7潜行右键: 记录当前坐标"));
        tooltip.add(Component.literal("§7再次潜行右键: 传送至锚点"));
        tooltip.add(Component.literal("§7潜行长按右键 1 秒: 覆盖锚点"));
        tooltip.add(Component.literal("§7受到致命伤害时抵挡并回溯至锚点"));
        tooltip.add(Component.literal("§2冷却:120s"));

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.getBoolean(KEY_SET)) {
            String dimensionId = tag.getString(KEY_DIM);
            tooltip.add(Component.literal("§8锚点: " + dimensionId + " "
                    + String.format("%.0f, %.0f, %.0f",
                    tag.getDouble(KEY_X),
                    tag.getDouble(KEY_Y),
                    tag.getDouble(KEY_Z))));
        } else {
            tooltip.add(Component.literal("§8锚点: 未设置"));
        }

        tooltip.add(Component.literal("§e§o需手持（主手/副手）生效"));
    }
}

