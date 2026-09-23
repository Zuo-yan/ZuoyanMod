package org.gwfx.zuoyanmod.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.block.BlockRegistry;
import org.gwfx.zuoyanmod.item.ItemRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 玻色-爱因斯坦凝聚场（绝对零度的技能本体）。服务端权威，释放瞬间一次性完成两件事：
 * <ol>
 *   <li><b>时停</b>：以施术者为中心 {@link #RADIUS} 格内的所有实体静止 {@link #FREEZE_TICKS} tick。
 *       生物额外关闭 AI（并在场结束后按原值还原），掉落物速度归零——它们悬停在空中。</li>
 *   <li><b>相变</b>：范围内的暗物质掉落物就地变成超流体暗物质（1 个 = 1 格流体源）。</li>
 * </ol>
 * 除此之外没有第三个效果（凝聚合成链已按用户要求移除）。
 * <p>
 * <b>施术者是唯一的例外，不参与冻结</b>——他是这个场的参照系。这既是设定（冻结的是别人的时间），
 * 也是必要的：相变会在原地造出致命液池（见 {@code DarkMatterEventHandler}），施术者必须有时间离开，
 * 否则技能变成"用一次死一次"。见 docs/absolute_zero.md §5.2。
 * <p>
 * 液化的产出**完全沿用**原有液态暗物质（Phase 侵蚀全套惩罚 + 紫金神装免疫），不做任何削弱。
 */
@EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class TimeFreezeManager {

    /**
     * 场的半径，单位：格。以中心方块为 0，向四周各扩 6 格 → **13×13×13**。
     * <p>
     * 这个值是唯一的范围旋钮：改它一处，冻结范围与暗物质转化范围同时生效。
     */
    public static final double RADIUS = 6.0D;

    /** 冻结时长：15 秒 */
    public static final int FREEZE_TICKS = 15 * 20;

    /**
     * 单次释放最多液化的暗物质数量。一整箱暗物质一次性铺开会让一个 tick 内产生
     * 数百次方块更新 + 流体扩散调度，是明确的卡顿源；超出的部分**原样留在地上**（不吞东西）。
     */
    public static final int MAX_CONVERSIONS_PER_CAST = 128;

    /** 一个已展开的凝聚场 */
    private record Field(ResourceKey<Level> dimension, BlockPos center, UUID caster, long expiresAt) {}

    private static final List<Field> FIELDS = new ArrayList<>();

    /** 被我们关掉 AI 的生物：原始 NoAi 值 / 实体引用，场结束后还原（引用只保留到下一个 tick） */
    private static final Map<UUID, Boolean> NO_AI_ORIGINAL = new HashMap<>();
    private static final Map<UUID, Mob> NO_AI_ENTITIES = new HashMap<>();

    /** 减速效果每 N tick 才刷一次，避免每 tick 都给场内每个实体发包 */
    private static final long EFFECT_REFRESH_INTERVAL = 5L;

    /**
     * 液体的落点搜索顺序：以物品所在格为中心，由近及远。
     * 同一水平层优先，然后往下一层，最后往上——流体该往低处淌，不该堆到半空。
     */
    private static final List<BlockPos> SPREAD_OFFSETS = buildSpreadOffsets();

    private TimeFreezeManager() {}

    private static List<BlockPos> buildSpreadOffsets() {
        int radius = (int) RADIUS;
        int[] layerOrder = {0, -1, 1, -2, 2};
        List<BlockPos> offsets = new ArrayList<>();
        for (int dy : layerOrder) {
            for (int ring = 0; ring <= radius; ring++) {
                for (int dx = -ring; dx <= ring; dx++) {
                    for (int dz = -ring; dz <= ring; dz++) {
                        // 只取方环的边框，保证按"距离"由近及远推进
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                            continue;
                        }
                        offsets.add(new BlockPos(dx, dy, dz));
                    }
                }
            }
        }
        return List.copyOf(offsets);
    }

    // ===== 对外：是否还有未消散的场（防止连点浪费耐久） =====

    public static boolean hasActiveField(Player caster) {
        long now = caster.level().getGameTime();
        for (Field field : FIELDS) {
            if (field.caster().equals(caster.getUUID()) && field.expiresAt() > now) {
                return true;
            }
        }
        return false;
    }

    // ===== 释放 =====

    public static void cast(ServerLevel level, Player caster) {
        BlockPos center = caster.blockPosition();
        AABB zone = new AABB(center).inflate(RADIUS);

        int liquefied = liquefy(level, zone);

        FIELDS.add(new Field(level.dimension(), center.immutable(), caster.getUUID(),
                level.getGameTime() + FREEZE_TICKS));

        // 演出：冰裂 + 霜环
        level.playSound(null, center, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.45F);
        level.playSound(null, center, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0F, 0.6F);
        spawnFrostRing(level, center);

        caster.sendSystemMessage(Component.literal("§b绝对零度 §7- 玻色-爱因斯坦凝聚展开 §8(15s)"));
        if (liquefied > 0) {
            caster.sendSystemMessage(Component.literal("§7相变：§d" + liquefied + " §7个暗物质坍缩为超流体"));
        }
    }

    // ===== 相变：暗物质掉落物 → 超流体暗物质 =====

    private static int liquefy(ServerLevel level, AABB zone) {
        BlockState fluid = BlockRegistry.DARK_MATTER_BLOCK.get().defaultBlockState();
        Set<BlockPos> used = new HashSet<>();
        int convertedTotal = 0;

        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, zone)) {
            if (convertedTotal >= MAX_CONVERSIONS_PER_CAST) {
                break;
            }
            ItemStack stack = itemEntity.getItem();
            if (!stack.is(ItemRegistry.DARK_MATTER.get())) {
                continue;
            }
            BlockPos origin = itemEntity.blockPosition();
            int converted = 0;
            for (int i = 0; i < stack.getCount() && convertedTotal < MAX_CONVERSIONS_PER_CAST; i++) {
                BlockPos target = findFluidSpot(level, origin, used);
                if (target == null) {
                    break;
                }
                if (level.setBlockAndUpdate(target, fluid)) {
                    used.add(target);
                    converted++;
                    convertedTotal++;
                }
            }
            if (converted <= 0) {
                continue; // 没有落点就原样留下，不吃玩家的东西
            }
            stack.shrink(converted);
            if (stack.isEmpty()) {
                itemEntity.remove(Entity.RemovalReason.DISCARDED);
            } else {
                itemEntity.setItem(stack);
            }
        }
        return convertedTotal;
    }

    /** 从物品所在格起找一个"还没被我们放过流体、且当前为空"的落点 */
    private static BlockPos findFluidSpot(ServerLevel level, BlockPos origin, Set<BlockPos> used) {
        for (BlockPos offset : SPREAD_OFFSETS) {
            BlockPos pos = origin.offset(offset);
            if (used.contains(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            // 流体方块本身是 replaceable 的：不排掉它，第二叠暗物质就会"放进已有的流体里"
            // —— 视觉无变化但物品被消耗，等于吞玩家东西。
            if (state.is(BlockRegistry.DARK_MATTER_BLOCK.get())) {
                continue;
            }
            if (state.canBeReplaced()) {
                return pos;
            }
        }
        return null;
    }

    // ===== 时停：每 tick 维持 =====

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (FIELDS.isEmpty() && NO_AI_ENTITIES.isEmpty()) {
            return;
        }
        long now = server.overworld().getGameTime();
        boolean refreshEffects = now % EFFECT_REFRESH_INTERVAL == 0L;
        Set<UUID> frozenThisTick = new HashSet<>();

        FIELDS.removeIf(field -> field.expiresAt() <= now || server.getLevel(field.dimension()) == null);

        for (Field field : FIELDS) {
            ServerLevel level = server.getLevel(field.dimension());
            if (level == null) {
                continue;
            }
            AABB zone = new AABB(field.center()).inflate(RADIUS);
            int remaining = (int) Math.max(1L, field.expiresAt() - now);
            boolean frostTick = now % 4L == 0L;

            for (Entity entity : level.getEntitiesOfClass(Entity.class, zone)) {
                // 施术者不冻结：他是这个场的参照系，也是唯一的撤离窗口
                if (entity.getUUID().equals(field.caster())) {
                    continue;
                }
                if (entity instanceof ItemEntity itemEntity) {
                    // 掉落物悬停
                    itemEntity.setDeltaMovement(0.0D, 0.0D, 0.0D);
                    frozenThisTick.add(itemEntity.getUUID());
                } else if (entity instanceof LivingEntity living) {
                    living.setDeltaMovement(0.0D, 0.0D, 0.0D);
                    // 6 级缓慢 ≈ 移动速度归零；按间隔续期，随场一起过期
                    if (refreshEffects) {
                        living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
                                remaining + (int) EFFECT_REFRESH_INTERVAL + 1, 6, false, false, false));
                    }
                    if (living instanceof Mob mob) {
                        NO_AI_ORIGINAL.putIfAbsent(mob.getUUID(), mob.isNoAi());
                        NO_AI_ENTITIES.put(mob.getUUID(), mob);
                        mob.setNoAi(true);
                        mob.setTarget(null);
                    }
                    frozenThisTick.add(living.getUUID());
                    if (frostTick) {
                        level.sendParticles(ParticleTypes.SNOWFLAKE,
                                living.getX(), living.getY() + living.getBbHeight() * 0.6D, living.getZ(),
                                3, 0.3D, 0.4D, 0.3D, 0.0D);
                    }
                }
            }

            if (frostTick) {
                level.sendParticles(ParticleTypes.SNOWFLAKE,
                        field.center().getX() + 0.5D, field.center().getY() + 0.5D, field.center().getZ() + 0.5D,
                        6, 1.2D, 1.2D, 1.2D, 0.0D);
            }
        }

        // 还原已经脱离冻结的生物 AI（场消散 / 实体被移除）
        Iterator<Map.Entry<UUID, Mob>> iterator = NO_AI_ENTITIES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Mob> entry = iterator.next();
            if (frozenThisTick.contains(entry.getKey())) {
                continue;
            }
            Mob mob = entry.getValue();
            if (!mob.isRemoved()) {
                mob.setNoAi(Boolean.TRUE.equals(NO_AI_ORIGINAL.get(entry.getKey())));
            }
            NO_AI_ORIGINAL.remove(entry.getKey());
            iterator.remove();
        }
    }

    /** 起手霜环：一圈向外扩散的雪片，让"场已展开"一眼可见 */
    private static void spawnFrostRing(ServerLevel level, BlockPos center) {
        level.sendParticles(ParticleTypes.SNOWFLAKE,
                center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D,
                40, RADIUS * 0.22D, RADIUS * 0.22D, RADIUS * 0.22D, 0.05D);
        level.sendParticles(ParticleTypes.WHITE_ASH,
                center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D,
                30, RADIUS * 0.20D, RADIUS * 0.20D, RADIUS * 0.20D, 0.02D);
    }
}
