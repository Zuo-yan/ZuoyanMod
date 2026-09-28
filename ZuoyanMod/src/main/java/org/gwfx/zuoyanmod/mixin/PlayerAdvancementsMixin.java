package org.gwfx.zuoyanmod.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.logging.LogUtils;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 修复 26.3 原版"全新世界里进度界面全空"的疏漏。
 *
 * <h2>现象</h2>
 * 全新世界（玩家进度存档里没有任何记录）第一次打开进度界面，
 * 窗口里只有「这里好像什么都没有……」，**连原版进度也不显示**。
 * 服务端日志一切正常：{@code Loaded 1902 advancements}、0 ERROR —— 数据没问题，是同步没发生。
 *
 * <h2>根因（读源码 + 离线模拟定位）</h2>
 * 首次全量同步依赖 {@code PlayerAdvancements#flushDirty}，而它发出去的节点只来自
 * {@code rootsToUpdate}，后者**只由 {@code markForVisibilityUpdate} 填充**，
 * 而该方法只有三个调用点：{@code applyFrom}（读玩家进度存档）、{@code award}、{@code revoke}。
 *
 * <p>全新玩家进入时：存档零记录 → {@code applyFrom} 不填；一个成就没拿 → 没有 {@code award}。
 * ⇒ {@code rootsToUpdate} 与 {@code progressChanged} 双空 ⇒ {@code added} 空 ⇒
 * <b>发包点那个 {@code if (!progress.isEmpty() || !added.isEmpty() || !removed.isEmpty())} 判断为假，
 * 包根本不构造</b>；可 {@code isFirstPacket} 依然被置 false（赋值在 if 外面），
 * <b>首次全量同步的机会被永久消耗</b>。
 *
 * <h2>为什么不能只"把所有根塞进 rootsToUpdate"</h2>
 * 这是本修复的第一个版本，**被离线模拟器当场否掉**，值得记下来：
 * {@code updateTreeVisibility} 会调 {@code AdvancementVisibilityEvaluator}，
 * 而它有一条 {@code VISIBILITY_DEPTH = 2} 的规则 —— 未完成的节点只有在
 * **祖先（3 层内）已有已完成节点（SHOW）**时才可见；全新玩家什么都没完成，
 * 于是连**根节点自己都返回 false**，{@code added} 依然是空的。
 *
 * <p>所以修法必须是**绕开可见性评估**：首包直接发送树里的全部节点。
 *
 * <h2>修法</h2>
 * 注入 {@code flushDirty} 的 HEAD，拦截 {@code isFirstPacket} 那一轮，自己构造并发送
 * "包含全部节点"的全量包，然后 {@code cancel} 掉原方法（避免它再发一个空的/重复的）。
 * 之后的 {@code flushDirty} 调用完全走原版逻辑 —— 增量同步、可见性评估都不受影响。
 *
 * <p><b>为什么取消而不是"补发"</b>：原方法在 {@code isFirstPacket} 且无内容时会
 * 走完整个流程（清空集合、把 isFirstPacket 置 false）什么都不发。
 * 我们已在 HEAD 里发出了正确的全量包，<b>此时必须把 {@code isFirstPacket} 也置为 false</b>，
 * 状态才与原版收敛一致；直接用 {@code ci.cancel()} 会跳过那句赋值，
 * 所以下面手动补上。
 *
 * <p><b>为什么用 {@code tree.nodes()} 而不是遍历每个根</b>：`nodes()` 是树里全部节点的
 * 平坦视图，正是客户端建树需要的全集；按根递归反而要自己处理重复与顺序。
 * 注意客户端 {@code AdvancementTree.addAll} 内部会用"父必须先于子"的多轮插入消化这个列表，
 * **不要求发送顺序**，所以直接把 {@code nodes()} 丢过去即可。
 */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 原版私有字段：服务端成就树。 */
    @Shadow
    private AdvancementTree tree;

    /** 原版私有字段：是否还没发过第一个（全量）包。 */
    @Shadow
    private boolean isFirstPacket;

    /** 原版私有字段：本 tick 需要重新评估可见性的根集合（这里只用于清空，避免重复发）。 */
    @Shadow
    @Final
    private Set<AdvancementNode> rootsToUpdate;

    /**
     * 首个包之前，直接下发整棵成就树。
     *
     * <p>注入点选 HEAD 的原因：原方法会在中途 {@code clear()} 掉若干集合，
     * 且在末尾把 {@code isFirstPacket} 置 false，只有 HEAD 能拿到"尚未被消费"的初始状态。
     *
     * <p>{@code injectors.defaultRequire = 1}（见 zuoyanmod.mixins.json）保证签名一旦对不上
     * 就在启动期直接失败 —— 静默失效正是这个 bug 当初能潜伏下来的原因。
     */
    @Inject(method = "flushDirty", at = @At("HEAD"), cancellable = true)
    private void zuoyanmod$sendFullTreeOnFirstPacket(ServerPlayer player, boolean showAdvancements, CallbackInfo ci) {
        if (!this.isFirstPacket || this.tree == null) {
            return;
        }

        List<ClientboundUpdateAdvancementsPacket.PositionedAdvancement> all = new ArrayList<>();
        for (AdvancementNode node : this.tree.nodes()) {
            all.add(ClientboundUpdateAdvancementsPacket.PositionedAdvancement.fromNode(node));
        }

        // shouldReset = true：让客户端先清空自己的树，避免与残留状态拼出脏结构。
        // progress / removed 传空：玩家本来就没有任何进度，无需同步。
        player.connection.send(new ClientboundUpdateAdvancementsPacket(
                true,
                all,
                Set.of(),
                Map.of(),
                showAdvancements
        ));

        // 与原版收敛：这些集合在本轮已被我们"消费"，且首包已发出。
        this.rootsToUpdate.clear();
        this.isFirstPacket = false;

        // 取消原方法：它此刻只会走一个"什么都不发"的空流程，还会重复清空集合。
        ci.cancel();

        LOGGER.debug("[advancements] sent full tree on first packet: {} nodes", all.size());
    }
}
