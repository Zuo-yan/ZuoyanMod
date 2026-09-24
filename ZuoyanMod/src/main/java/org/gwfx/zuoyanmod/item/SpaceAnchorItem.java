package org.gwfx.zuoyanmod.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.Set;
import java.util.function.Consumer;

/**
 * 空间锚点：潜行右键记录坐标，再次潜行右键传送回锚点；
 * 手持时受到致命伤害会抵挡并回溯到锚点（冷却 120 秒）。
 * <p>
 * 致命抵挡部分与名刀司命共用同一套事件，由 {@link org.gwfx.zuoyanmod.event.SpaceAnchorEventHandler}
 * 通过 {@code event.isCanceled()} 判断名刀是否已经介入，避免重复触发。
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

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }
        if (USE_DURATION - remainingUseDuration < OVERRIDE_TICKS) {
            return;
        }
        ItemStack anchor = findHeldAnchor(player);
        if (!anchor.isEmpty()) {
            saveAnchor(player, anchor);
            player.sendSystemMessage(Component.translatable("message.zuoyanmod.space_anchor.recalibrated"));
        }
        player.releaseUsingItem();
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return false;
        }
        // 长按已经由 onUseTick 处理，这里只处理短按
        if (USE_DURATION - timeLeft >= OVERRIDE_TICKS) {
            return false;
        }
        ItemStack anchor = findHeldAnchor(player);
        if (anchor.isEmpty()) {
            return false;
        }
        if (!hasAnchor(anchor)) {
            saveAnchor(player, anchor);
            player.sendSystemMessage(Component.translatable("message.zuoyanmod.space_anchor.saved"));
            return true;
        }
        if (teleportToAnchor(player, anchor)) {
            player.sendSystemMessage(Component.translatable("message.zuoyanmod.space_anchor.recalled"));
        }
        return true;
    }

    // ===== 锚点数据读写（CUSTOM_DATA） =====

    public static CompoundTag readTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static void writeTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean hasAnchor(ItemStack stack) {
        return readTag(stack).getBoolean(KEY_SET).orElse(false);
    }

    public static void saveAnchor(Player player, ItemStack stack) {
        CompoundTag tag = readTag(stack);
        tag.putBoolean(KEY_SET, true);
        tag.putString(KEY_DIM, player.level().dimension().identifier().toString());
        tag.putDouble(KEY_X, player.getX());
        tag.putDouble(KEY_Y, player.getY());
        tag.putDouble(KEY_Z, player.getZ());
        tag.putFloat(KEY_Y_ROT, player.getYRot());
        tag.putFloat(KEY_X_ROT, player.getXRot());
        writeTag(stack, tag);
    }

    /** 已记录的锚点坐标；未设置返回 null */
    public record AnchorPos(String dimension, double x, double y, double z, float yRot, float xRot) {}

    public static AnchorPos readAnchorPos(ItemStack stack) {
        CompoundTag tag = readTag(stack);
        if (!tag.getBoolean(KEY_SET).orElse(false)) {
            return null;
        }
        return new AnchorPos(
                tag.getString(KEY_DIM).orElse(""),
                tag.getDouble(KEY_X).orElse(0D),
                tag.getDouble(KEY_Y).orElse(0D),
                tag.getDouble(KEY_Z).orElse(0D),
                tag.getFloat(KEY_Y_ROT).orElse(0F),
                tag.getFloat(KEY_X_ROT).orElse(0F)
        );
    }

    /** 立即传送到锚点，成功返回 true；失败时玩家会收到原因提示 */
    public static boolean teleportToAnchor(Player player, ItemStack stack) {
        AnchorPos pos = readAnchorPos(stack);
        if (pos == null) {
            player.sendSystemMessage(Component.translatable("message.zuoyanmod.space_anchor.not_set"));
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
        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(pos.dimension()));
        ServerLevel target = server.getLevel(dimensionKey);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("message.zuoyanmod.space_anchor.dimension_unavailable"));
            return false;
        }

        // 与 RealmTransitionManager 一致的跨维度传送写法。
        // 注意：跨维度分支内部是"创建新实体 + 移除旧实体"，可能返回 false，必须检查返回值
        boolean success = serverPlayer.teleportTo(target, pos.x(), pos.y(), pos.z(), Set.<Relative>of(), pos.yRot(), pos.xRot(), false);
        if (!success) {
            return false;
        }
        serverPlayer.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0F, 1.0F);
        return true;
    }

    // ===== 冷却 =====

    public static boolean isOnCooldown(Player player, ItemStack stack) {
        long until = readTag(stack).getLong(KEY_COOLDOWN_UNTIL).orElse(0L);
        return until > player.level().getGameTime();
    }

    public static void markCooldown(Player player, ItemStack stack) {
        CompoundTag tag = readTag(stack);
        tag.putLong(KEY_COOLDOWN_UNTIL, player.level().getGameTime() + COOLDOWN_TICKS);
        writeTag(stack, tag);
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
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.zuoyanmod.space_anchor.desc1"));
        tooltip.accept(Component.translatable("item.zuoyanmod.space_anchor.desc2"));
        tooltip.accept(Component.translatable("item.zuoyanmod.space_anchor.desc3"));
        tooltip.accept(Component.translatable("item.zuoyanmod.space_anchor.desc4"));
        tooltip.accept(Component.translatable("item.zuoyanmod.space_anchor.desc5"));
        tooltip.accept(Component.translatable("item.zuoyanmod.space_anchor.desc6"));

        CompoundTag tag = readTag(stack);
        if (tag.getBoolean(KEY_SET).orElse(false)) {
            String dimensionId = tag.getString(KEY_DIM).orElse("");
            tooltip.accept(Component.translatable("item.zuoyanmod.space_anchor.desc9", dimensionId,
                    String.format("%.0f, %.0f, %.0f",
                            tag.getDouble(KEY_X).orElse(0D),
                            tag.getDouble(KEY_Y).orElse(0D),
                            tag.getDouble(KEY_Z).orElse(0D))));
        } else {
            tooltip.accept(Component.translatable("item.zuoyanmod.space_anchor.desc7"));
        }

        tooltip.accept(Component.translatable("item.zuoyanmod.space_anchor.desc8"));
    }
}
