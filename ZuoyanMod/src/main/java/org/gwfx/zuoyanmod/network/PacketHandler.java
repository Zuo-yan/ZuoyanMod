package org.gwfx.zuoyanmod.network;

import com.mojang.logging.LogUtils;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.slf4j.Logger;

@EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class PacketHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private PacketHandler() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Zuoyanmod.MODID).versioned("1");
        registrar.playToServer(
                ToggleRealmPacket.TYPE,
                ToggleRealmPacket.STREAM_CODEC,
                ToggleRealmPacket::handle
        );
        registrar.playToServer(
                DeathNotePacket.TYPE,
                DeathNotePacket.STREAM_CODEC,
                DeathNotePacket::handle
        );
        registrar.playToServer(
                OpenKleinBottlePacket.TYPE,
                OpenKleinBottlePacket.STREAM_CODEC,
                OpenKleinBottlePacket::handle
        );
        registrar.playToServer(
                CraftingTransferPacket.TYPE,
                CraftingTransferPacket.STREAM_CODEC,
                CraftingTransferPacket::handle
        );
        // 终端视图：客户端提交搜索词/滚动位置，服务端回传视图快照（含每格总量）
        registrar.playToServer(
                KleinBottleViewPacket.TYPE,
                KleinBottleViewPacket.STREAM_CODEC,
                KleinBottleViewPacket::handle
        );
        // 终端铁砧：重命名输入框内容（改名也算进铁砧代价里，必须到服务端那份隐形铁砧那儿算）
        registrar.playToServer(
                KleinAnvilNamePacket.TYPE,
                KleinAnvilNamePacket.STREAM_CODEC,
                KleinAnvilNamePacket::handle
        );
        registrar.playToClient(
                KleinBottleSyncPacket.TYPE,
                KleinBottleSyncPacket.STREAM_CODEC,
                KleinBottleSyncPacket::handle
        );
        // 模组效果通知（Toast）：右上角堆叠显示，替代聊天栏刷屏
        registrar.playToClient(
                ModToastPacket.TYPE,
                ModToastPacket.STREAM_CODEC,
                ModToastPacket::handle
        );
        // 经验升级系统：升级/选天赋/触发技能三个动作 + 一对同步握手
        registrar.playToServer(
                UpgradeStatPacket.TYPE,
                UpgradeStatPacket.STREAM_CODEC,
                UpgradeStatPacket::handle
        );
        // 右键退还加点
        registrar.playToServer(
                RefundStatPacket.TYPE,
                RefundStatPacket.STREAM_CODEC,
                RefundStatPacket::handle
        );
        registrar.playToServer(
                ChooseTalentPacket.TYPE,
                ChooseTalentPacket.STREAM_CODEC,
                ChooseTalentPacket::handle
        );
        registrar.playToServer(
                TriggerUltimatePacket.TYPE,
                TriggerUltimatePacket.STREAM_CODEC,
                TriggerUltimatePacket::handle
        );
        registrar.playToServer(
                RequestUpgradeSyncPacket.TYPE,
                RequestUpgradeSyncPacket.STREAM_CODEC,
                RequestUpgradeSyncPacket::handle
        );
        registrar.playToClient(
                UpgradeSyncPacket.TYPE,
                UpgradeSyncPacket.STREAM_CODEC,
                UpgradeSyncPacket::handle
        );
        // AI 聊天：服务端把回复分页下发（只读展示，不需要客户端回传）
        registrar.playToClient(
                org.gwfx.zuoyanmod.ai.net.AiChatReplyPacket.TYPE,
                org.gwfx.zuoyanmod.ai.net.AiChatReplyPacket.STREAM_CODEC,
                org.gwfx.zuoyanmod.ai.net.AiChatReplyPacket::handle
        );
        // AI 图形化配置：打开界面请求快照 → 提交修改 → 服务端回权威快照
        registrar.playToServer(
                org.gwfx.zuoyanmod.ai.net.RequestAiConfigPacket.TYPE,
                org.gwfx.zuoyanmod.ai.net.RequestAiConfigPacket.STREAM_CODEC,
                org.gwfx.zuoyanmod.ai.net.RequestAiConfigPacket::handle
        );
        registrar.playToServer(
                org.gwfx.zuoyanmod.ai.net.AiConfigUpdatePacket.TYPE,
                org.gwfx.zuoyanmod.ai.net.AiConfigUpdatePacket.STREAM_CODEC,
                org.gwfx.zuoyanmod.ai.net.AiConfigUpdatePacket::handle
        );
        registrar.playToClient(
                org.gwfx.zuoyanmod.ai.net.AiConfigSyncPacket.TYPE,
                org.gwfx.zuoyanmod.ai.net.AiConfigSyncPacket.STREAM_CODEC,
                org.gwfx.zuoyanmod.ai.net.AiConfigSyncPacket::handle
        );
        // AI 连通性测试
        registrar.playToServer(
                org.gwfx.zuoyanmod.ai.net.AiTestConnectionPacket.TYPE,
                org.gwfx.zuoyanmod.ai.net.AiTestConnectionPacket.STREAM_CODEC,
                org.gwfx.zuoyanmod.ai.net.AiTestConnectionPacket::handle
        );
        registrar.playToClient(
                org.gwfx.zuoyanmod.ai.net.AiTestConnectionResultPacket.TYPE,
                org.gwfx.zuoyanmod.ai.net.AiTestConnectionResultPacket.STREAM_CODEC,
                org.gwfx.zuoyanmod.ai.net.AiTestConnectionResultPacket::handle
        );
        LOGGER.info("[Network] Successfully registered payload handlers");
    }

    public static void sendToggleRealm() {
        LOGGER.info("[Realm] Sending toggle packet to server");
        sendToServer(new ToggleRealmPacket());
    }

    public static void sendDeathNote(String targetName, int durationSeconds) {
        sendToServer(new DeathNotePacket(targetName, durationSeconds));
    }

    public static void sendOpenKleinBottle() {
        sendToServer(new OpenKleinBottlePacket());
    }

    /** JEI 的 + 按钮：请服务端从四维空间抓这份配方的材料 */
    public static void sendCraftingTransfer(net.minecraft.resources.Identifier recipeId, boolean maxTransfer) {
        sendToServer(new CraftingTransferPacket(recipeId, maxTransfer));
    }

    /** 终端：提交搜索词与滚动位置（搜索词已按长度截断，服务端还会再校验一次） */
    public static void sendKleinBottleView(String search, int scrollRow) {
        String text = search == null ? "" : search;
        if (text.length() > KleinBottleViewPacket.MAX_SEARCH_LENGTH) {
            text = text.substring(0, KleinBottleViewPacket.MAX_SEARCH_LENGTH);
        }
        sendToServer(new KleinBottleViewPacket(text, Math.max(0, scrollRow)));
    }

    /** 终端铁砧：提交重命名输入框内容（已按 50 字符截断） */
    public static void sendKleinAnvilName(String name) {
        String text = name == null ? "" : name;
        if (text.length() > KleinAnvilNamePacket.MAX_NAME_LENGTH) {
            text = text.substring(0, KleinAnvilNamePacket.MAX_NAME_LENGTH);
        }
        sendToServer(new KleinAnvilNamePacket(text));
    }

    /** 升级界面：升级某条基础能力（下标对齐 UpgradeType#VALUES） */
    public static void sendUpgradeStat(int statIndex) {
        sendToServer(new UpgradeStatPacket(statIndex));
    }

    /** 升级界面：右键退还某条基础能力的最后一级（下标对齐 UpgradeType#VALUES） */
    public static void sendRefundStat(int statIndex) {
        sendToServer(new RefundStatPacket(statIndex));
    }

    /** 升级界面：选定终极天赋（下标对齐 UltimateTalent#VALUES） */
    public static void sendChooseTalent(int talentIndex) {
        sendToServer(new ChooseTalentPacket(talentIndex));
    }

    /** Y 键：触发终极天赋 */
    public static void sendTriggerUltimate() {
        sendToServer(new TriggerUltimatePacket());
    }

    /** 打开升级界面时：请求一份档案快照 */
    public static void sendRequestUpgradeSync() {
        sendToServer(new RequestUpgradeSyncPacket());
    }

    /** 打开 AI 配置界面时：请求一份配置快照（服务端立刻回，否则界面第一次打开是空白） */
    public static void sendRequestAiConfig() {
        sendToServer(new org.gwfx.zuoyanmod.ai.net.RequestAiConfigPacket());
    }

    /**
     * AI 配置界面：提交一次修改。
     *
     * <p>内容是一个 JSON 字符串（字段集定义在 {@code ai.core.config.AiConfigEdits}）。
     * 这里只做长度上限保护，<b>真正的校验在服务端</b> —— 客户端说了不算。
     */
    /** AI 配置界面：测试连接 */
    public static void sendTestAiConnection(String json) {
        String payload = json == null ? "{}" : json;
        if (payload.length() > 4096) {
            payload = "{}";
        }
        sendToServer(new org.gwfx.zuoyanmod.ai.net.AiTestConnectionPacket(payload));
    }

    public static void sendUpdateAiConfig(String json) {
        String payload = json == null ? "{}" : json;
        if (payload.length() > org.gwfx.zuoyanmod.ai.net.AiConfigSyncPacket.MAX_JSON_CHARS) {
            payload = "{}";
        }
        sendToServer(new org.gwfx.zuoyanmod.ai.net.AiConfigUpdatePacket(payload));
    }

    private static void sendToServer(CustomPacketPayload payload) {
        // 实现放在 client 包：本类是双端类，绝不能直接引用 net.minecraft.client.*，
        // 否则专用服务端加载模组时会因为要解析 Minecraft 而连带加载客户端界面类直接崩。
        org.gwfx.zuoyanmod.client.ClientPacketSender.sendToServer(payload);
    }
}