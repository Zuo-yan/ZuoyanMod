package org.gwfx.zuoyanmod.event;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.entity.MultiverseCloneService;

/**
 * 「平行宇宙替身克隆」的三条守护规则（因果律手枪 Phase 3 事件层）：
 *
 * <ul>
 *   <li>防刷物①：克隆体死亡不掉任何掉落物（{@link LivingDropsEvent} 清空）；</li>
 *   <li>防刷物②：克隆体死亡不掉经验（{@link LivingExperienceDropEvent} 置零）；</li>
 *   <li>时限消散：每服务端 tick 驱动 {@link MultiverseCloneService#tickClones}，
 *       维持克隆互殴目标并令到期克隆体 discard 爆散流光粒子。</li>
 * </ul>
 *
 * <p>防套娃（克隆体不可再被克隆）的检查不在事件层，
 * 由 {@link MultiverseCloneService#tryResolve} 在子弹命中前置判定。
 *
 * <p><b>1.20.1 适配</b>：
 * <ul>
 *   <li>事件订阅：NeoForge 的 {@code @EventBusSubscriber} → Forge 的
 *       {@code @Mod.EventBusSubscriber(modid = Zuoyanmod.MODID)}（默认挂在
 *       {@code MinecraftForge.EVENT_BUS} 上，这三个事件都在该总线派发）；
 *       {@code SubscribeEvent} 的包名从 {@code net.neoforged.bus.api} 换成
 *       {@code net.minecraftforge.eventbus.api}；</li>
 *   <li>Tick：NeoForge 的 {@code ServerTickEvent.Post} → Forge 的
 *       {@code TickEvent.ServerTickEvent} + {@code phase == TickEvent.Phase.END}，
 *       server 用 {@code event.getServer()}（写法同 {@link DomainExpansionDuelTickHandler}）；</li>
 *   <li>实体标签：26.3 的 {@code entityTags()} → 1.20.1 的 {@code getTags()}；</li>
 *   <li>Forge 的 {@code LivingDropsEvent}/{@code LivingExperienceDropEvent} 字段名与 NeoForge 一致
 *       （{@code getEntity()} / {@code getDrops()} / {@code setDroppedExperience(int)}），无需改名。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class MultiverseCloneEventHandler {

    private MultiverseCloneEventHandler() {}

    /** 防刷物①：带克隆标记的实体死亡时清空全部掉落 */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().getTags().contains(MultiverseCloneService.CLONE_TAG)) {
            event.getDrops().clear();
        }
    }

    /** 防刷物②：带克隆标记的实体死亡时不产出任何经验 */
    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (event.getEntity().getTags().contains(MultiverseCloneService.CLONE_TAG)) {
            event.setDroppedExperience(0);
        }
    }

    /** 生命周期驱动：每服务端 tick 末尾维持互殴目标并消散到期克隆体 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        MultiverseCloneService.tickClones(server);
    }
}
