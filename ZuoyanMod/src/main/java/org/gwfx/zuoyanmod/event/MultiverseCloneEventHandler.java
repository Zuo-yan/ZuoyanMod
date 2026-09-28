package org.gwfx.zuoyanmod.event;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
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
 */
@EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class MultiverseCloneEventHandler {

    private MultiverseCloneEventHandler() {}

    /** 防刷物①：带克隆标记的实体死亡时清空全部掉落 */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().entityTags().contains(MultiverseCloneService.CLONE_TAG)) {
            event.getDrops().clear();
        }
    }

    /** 防刷物②：带克隆标记的实体死亡时不产出任何经验 */
    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (event.getEntity().entityTags().contains(MultiverseCloneService.CLONE_TAG)) {
            event.setDroppedExperience(0);
        }
    }

    /** 生命周期驱动：每服务端 tick 末尾维持互殴目标并消散到期克隆体 */
    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        MultiverseCloneService.tickClones(server);
    }
}
