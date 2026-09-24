package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gwfx.zuoyanmod.entity.PrimordialBlackHoleEntity;

/**
 * 「原始黑洞」—— 一次性奇点装置：右键在准心落点展开一个 10 秒的黑洞。
 *
 * <p>效果本身全在 {@link PrimordialBlackHoleEntity} 里，本类只负责**使用规则**：
 * 冷却、一人一个、全局上限、落点计算、扣料。
 *
 * <h2>只覆写 {@code use}，故意不覆写 {@code useOn}</h2>
 * 两者的调用关系是"方块交互没消费掉 → 落到 useOn → 再落到 use"。
 * 两个都覆写会在"对着地面右键"时**双触发**（一次 useOn 一次 use），直接放出两个黑洞。
 * 只写 {@code use} 就同时覆盖了"对着空气"和"对着普通方块"两种情况。
 *
 * <h2>冷却期间收不到任何回调 —— 这不是 bug</h2>
 * {@code ServerPlayerGameMode#useItem} 的第一段就是：
 * <pre>{@code if (player.getCooldowns().isOnCooldown(itemStack)) return InteractionResult.PASS; }</pre>
 * 也就是说冷却期间本方法**根本不会被调用**，想在这里发"还在冷却"的提示是徒劳的。
 * 原版给的反馈是物品图标上的灰色冷却扇形（{@code ItemCooldowns} 会自动同步给客户端），
 * 这是原版标准 UX，我们不再另加消息。
 */
public class PrimordialBlackHoleItem extends DescribedItem {

    /** 冷却：60 秒。 */
    private static final int COOLDOWN_TICKS = 20 * 60;

    /** 射线最远打到 24 格：再远就当作"朝那个方向丢出去"，落点在射线上。 */
    private static final double RAY_RANGE = 24.0D;

    /**
     * 落点沿视线外移的距离。
     * <p>射线打到的是方块表面，黑洞中心正好落在表面上的话，黑盘有一半会嵌进方块里 ——
     * 视觉上像被切掉一块。往外推 0.6 格让它整个浮在表面外侧。
     */
    private static final double SURFACE_OFFSET = 0.6D;

    public PrimordialBlackHoleItem(Properties properties, String... descKeys) {
        super(properties, descKeys);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 防御性检查：正常情况下服务端在冷却中根本不会调到这里（见类注释），
        // 但客户端路径与其它模组清冷却的异常情况可能绕过原版那层拦截，多一道判断不亏。
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        // 客户端只做挥手的动作预测，真正的生成由服务端完成
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        MinecraftServer server = serverLevel.getServer();

        // 每人同时只能维持一个黑洞。
        // 注意：60 秒冷却远长于 10 秒寿命，正常途径下第二次右键根本进不到这里 ——
        // 这条是兜底（防其它模组清冷却、防 /clear 指令、防跨维度快速重放）。
        if (PrimordialBlackHoleEntity.hasActiveBlackHole(server, player.getUUID())) {
            player.sendSystemMessage(Component.literal("§7你还有一个黑洞尚未坍缩"));
            return InteractionResult.FAIL;
        }

        // 全局上限：每个黑洞每 2 tick 都要扫一遍 10 格内的实体，数量必须封顶
        if (PrimordialBlackHoleEntity.countActive(server) >= PrimordialBlackHoleEntity.MAX_ACTIVE_HOLES) {
            player.sendSystemMessage(Component.literal("§7空间已经承受不住更多奇点"));
            return InteractionResult.FAIL;
        }

        PrimordialBlackHoleEntity.spawn(serverLevel, player, resolveImpact(player));

        // ⚠️ 顺序不能反：先加冷却再扣数量。
        // 反过来（先 consume）的话，当这是最后一个黑洞时物品栏那一格会变成空气，
        // addCooldown 就会加在空物品上，冷却条直接不显示 —— 玩家会以为没进冷却。
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        // consume 自带创造模式豁免（内部判 hasInfiniteMaterials），不需要自己写 isCreative
        stack.consume(1, player);

        return InteractionResult.CONSUME;
    }

    /**
     * 算落点：从眼睛朝视线方向打一条 24 格的射线。
     *
     * <p>没打到任何东西（对天、对虚空）时用射线末端 —— 相当于"朝那个方向 24 格处丢一个黑洞"，
     * 而不是"脚下"或者"失败"。空中的黑洞对掉落物无效，但那是玩家的选择，不做特殊保护。
     */
    private static Vec3 resolveImpact(Player player) {
        Vec3 eye = player.getEyePosition();
        HitResult hit = player.pick(RAY_RANGE, 0.0F, false);
        Vec3 point = hit.getLocation();

        if (hit.getType() == HitResult.Type.MISS) {
            return point;
        }

        // 用"落点 - 眼睛"反推射线方向（比再调一次 getViewVector 更可靠，
        // 因为 pick 内部可能带有视线插值）
        Vec3 direction = point.subtract(eye);
        if (direction.lengthSqr() < 1.0E-6D) {
            return point;
        }
        return point.add(direction.normalize().scale(SURFACE_OFFSET));
    }
}
