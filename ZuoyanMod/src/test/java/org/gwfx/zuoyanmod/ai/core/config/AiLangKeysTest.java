package org.gwfx.zuoyanmod.ai.core.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 语言文件与字段表 / 界面用键的一致性。
 *
 * <p><b>为什么需要这个测试</b>：语言文件是纯资源，编译器不会替我们检查"表里加了字段、
 * 文案忘了写"。而那种错误的症状是界面上直接显示 {@code ai.zuoyanmod.gui.field.ai.foo} 这种键名 ——
 * 玩家看不懂，也不会来报告，只会觉得这个界面是半成品。
 *
 * <p>所以这里逐个字段核对"标签 + 说明"在两种语言里都存在，再核一遍界面直接写死的那些键。
 */
class AiLangKeysTest {

    private static final List<String> LANGUAGES = List.of("zh_cn.json", "en_us.json");

    /** 界面直接引用的键（不经字段表），改了界面就容易忘，所以在这里钉住。 */
    private static final List<String> SCREEN_KEYS = List.of(
            "ai.zuoyanmod.gui.title",
            "ai.zuoyanmod.gui.test_connection",
            "ai.zuoyanmod.gui.testing",
            "ai.zuoyanmod.gui.test_success",
            "ai.zuoyanmod.gui.test_failed",
            "ai.zuoyanmod.gui.loading",
            "ai.zuoyanmod.gui.on",
            "ai.zuoyanmod.gui.off",
            "ai.zuoyanmod.gui.apply",
            "ai.zuoyanmod.gui.done",
            "ai.zuoyanmod.gui.more",
            "ai.zuoyanmod.gui.back",
            "ai.zuoyanmod.gui.page",
            "ai.zuoyanmod.gui.range",
            "ai.zuoyanmod.gui.read_only",
            "ai.zuoyanmod.gui.applying",
            "ai.zuoyanmod.gui.api_key",
            "ai.zuoyanmod.gui.api_key_desc",
            "ai.zuoyanmod.gui.key_source",
            "ai.zuoyanmod.gui.key_source_file",
            "ai.zuoyanmod.gui.key_file",
            "ai.zuoyanmod.gui.effective_provider",
            "ai.zuoyanmod.gui.advanced.title",
            "ai.zuoyanmod.gui.advanced.header",
            "ai.zuoyanmod.gui.status.applied",
            "ai.zuoyanmod.gui.status.adjusted",
            "ai.zuoyanmod.gui.status.rejected",
            "ai.zuoyanmod.gui.status.failed",
            "ai.zuoyanmod.gui.status.nothing_to_apply",
            AiConfigEdits.ERROR_NO_PERMISSION,
            AiConfigEdits.ERROR_INVALID,
            AiConfigEdits.ERROR_NUMBER,
            AiConfigEdits.ERROR_UNKNOWN_FIELD,
            AiConfigEdits.ERROR_NO_FIELDS,
            AiConfigEdits.ERROR_PROVIDER,
            AiConfigEdits.ERROR_BASE_URL_EMPTY,
            AiConfigEdits.ERROR_BASE_URL_SCHEME,
            AiConfigEdits.ERROR_TEMPERATURE,
            AiConfigEdits.ERROR_MAX_TOKENS,
            AiConfigEdits.ERROR_PREFIX,
            // 空回复的两种可诊断形态（T001-6 排查建造时的空回复）
            "ai.zuoyanmod.error.empty_reply_reason",
            "ai.zuoyanmod.error.truncated_reply",
            // T002：身份自查 + 危险级指令的确认流程
            "ai.zuoyanmod.level.0",
            "ai.zuoyanmod.level.1",
            "ai.zuoyanmod.level.2",
            "ai.zuoyanmod.level.3",
            "ai.zuoyanmod.level.4",
            "ai.zuoyanmod.level.unknown",
            "ai.zuoyanmod.whoami.level",
            "ai.zuoyanmod.whoami.config",
            "ai.zuoyanmod.whoami.tools",
            "ai.zuoyanmod.whoami.allowed",
            "ai.zuoyanmod.whoami.denied",
            "ai.zuoyanmod.whoami.danger_on",
            "ai.zuoyanmod.whoami.danger_off",
            "ai.zuoyanmod.whoami.hint",
            "ai.zuoyanmod.command.proposed",
            "ai.zuoyanmod.command.none",
            "ai.zuoyanmod.command.disabled",
            "ai.zuoyanmod.command.low_level",
            "ai.zuoyanmod.command.executed",
            "ai.zuoyanmod.command.no_output",
            "ai.zuoyanmod.command.truncated",
            "ai.zuoyanmod.command.cancelled",
            "ai.zuoyanmod.status.dangerous",
            "ai.zuoyanmod.help.whoami",
            "ai.zuoyanmod.help.confirm",
            "ai.zuoyanmod.help.cancel",
            // T001-6：AI 建造
            "ai.zuoyanmod.help.undo",
            "ai.zuoyanmod.command.cancelled_all",
            "ai.zuoyanmod.whoami.build",
            "ai.zuoyanmod.whoami.build_on",
            "ai.zuoyanmod.whoami.build_off",
            "ai.zuoyanmod.status.build",
            "ai.zuoyanmod.build.proposed",
            "ai.zuoyanmod.build.materials",
            "ai.zuoyanmod.build.preview_too_big",
            "ai.zuoyanmod.build.confirm_hint",
            "ai.zuoyanmod.build.started",
            "ai.zuoyanmod.build.done",
            "ai.zuoyanmod.build.aborted",
            "ai.zuoyanmod.build.empty",
            "ai.zuoyanmod.build.unknown_block",
            "ai.zuoyanmod.build.closed",
            "ai.zuoyanmod.build.blocked",
            "ai.zuoyanmod.build.abort_unloaded",
            "ai.zuoyanmod.build.abort_rejected",
            "ai.zuoyanmod.build.busy",
            "ai.zuoyanmod.build.undo_none",
            "ai.zuoyanmod.build.undo_started",
            "ai.zuoyanmod.build.undo_done",
            "ai.zuoyanmod.build.undo_aborted");

    @Test
    void everyFieldHasLabelAndDescriptionInEveryLanguage() throws IOException {
        for (String language : LANGUAGES) {
            JsonObject lang = load(language);
            for (AiConfigFields.Field field : AiConfigFields.all()) {
                assertTrue(lang.has(field.labelKey()),
                        () -> language + " 缺少标签：" + field.labelKey());
                assertTrue(lang.has(field.descriptionKey()),
                        () -> language + " 缺少说明：" + field.descriptionKey());
            }
        }
    }

    @Test
    void everyGroupHasATitleInEveryLanguage() throws IOException {
        for (String language : LANGUAGES) {
            JsonObject lang = load(language);
            for (AiConfigFields.Group group : AiConfigFields.Group.values()) {
                assertTrue(lang.has(group.labelKey()),
                        () -> language + " 缺少分组标题：" + group.labelKey());
            }
        }
    }

    @Test
    void everyScreenKeyExistsInEveryLanguage() throws IOException {
        for (String language : LANGUAGES) {
            JsonObject lang = load(language);
            for (String key : SCREEN_KEYS) {
                assertTrue(lang.has(key), () -> language + " 缺少界面文案：" + key);
            }
        }
    }

    /**
     * 标签必须装得进标签列。
     *
     * <p>标签是和控件左右并排画的，标签列宽固定（{@code AiConfigFormScreen.LABEL_W}，78px）。
     * 名字太长就会直接压到输入框上 —— 这是已经踩过的坑，所以在这里按"全角 9px / 半角 6px"
     * 的保守估算卡一道；宽度取的都是上限，宁可估宽也不要漏。
     */
    @Test
    void labelsFitInTheLabelColumn() throws IOException {
        int labelColumnWidth = 78;
        for (String language : LANGUAGES) {
            JsonObject lang = load(language);
            for (AiConfigFields.Field field : AiConfigFields.all()) {
                String label = lang.get(field.labelKey()).getAsString();
                int width = estimateWidth(label);
                assertTrue(width <= labelColumnWidth,
                        () -> language + " 的标签太宽（约 " + width + "px > " + labelColumnWidth + "px）：" + label);
            }
            // 主界面固定行的标签也走同一条列宽，一并卡住
            String apiKeyLabel = lang.get("ai.zuoyanmod.gui.api_key").getAsString();
            assertTrue(estimateWidth(apiKeyLabel) <= labelColumnWidth,
                    () -> language + " 的「API Key」标签太宽：" + apiKeyLabel);
        }
    }

    /** 粗略宽度估算：全角按 9px、半角按 6px，只取上界比较。 */
    private static int estimateWidth(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += text.charAt(i) > 0x2E80 ? 9 : 6;
        }
        return width;
    }

    private static JsonObject load(String fileName) throws IOException {
        Path path = Path.of("src", "main", "resources", "assets", "zuoyanmod", "lang", fileName);
        assertTrue(Files.isRegularFile(path),
                "找不到语言文件（测试工作目录应为本项目根目录）：" + path.toAbsolutePath());
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
