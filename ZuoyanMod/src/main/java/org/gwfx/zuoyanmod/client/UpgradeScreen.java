package org.gwfx.zuoyanmod.client;

import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import org.gwfx.zuoyanmod.client.klein.KleinTheme;
import org.gwfx.zuoyanmod.network.PacketHandler;
import org.gwfx.zuoyanmod.upgrade.UltimateTalent;
import org.gwfx.zuoyanmod.upgrade.UpgradeType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 升级界面（纯 Screen，无 Menu——数据全部来自 {@link ClientUpgradeData} 的同步快照）。
 *
 * <h2>布局</h2>
 * 左列 5 条基础能力：每行两行字——名称/等级 + 10 段进度格与花费；点击整行升级。
 * 数值细节（每级加多少、当前→下级）放 hover tooltip，行内只留得下花费。
 * 右列 4 张终极天赋卡：未满级时灰锁；满级后点一下选中（金色描边 + 底部确认提示）、
 * 再点同一张确认选定——两段确认防手滑，选定永久不可改。
 *
 * <h2>可点区域必须是真控件（KleinSideButton v6 的教训）</h2>
 * 第一版这里也是"Screen 手画方块 + 手写命中判定"，和终端 v5 一样落得"看着对但点不动"。
 * 现在每行/每卡都是一个 {@link UpgradeRowButton}（{@link AbstractButton} 子类），
 * 命中 / hover / 点击音效 / tooltip 全部交给控件框架，Screen 只负责摆位置和画面板。
 *
 * <h2>界面内行内反馈</h2>
 * 服务端的成败提示走 actionbar，而本界面开着时 actionbar 画不出来——玩家点了没反应
 * 的体感就是这么来的。所以校验结果直接画在页脚：不可负担/满级在客户端就地拦截，
 * 升级成功靠同步快照的等级差闪烁确认（服务端仍然是权威，actionbar 在界面关闭后照发）。
 *
 * <p>全部运行时绘制（{@code GuiGraphicsExtractor.fill}），配色沿用 klein 主题，
 * 没有任何贴图依赖，任意 GUI 缩放都是 1:1 像素。
 */
public class UpgradeScreen extends Screen {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int PANEL_W = 280;
    private static final int PANEL_H = 200;

    // 左列（基础能力）
    private static final int STATS_X = 10;
    private static final int STATS_W = 150;
    private static final int STATS_Y = 30;
    private static final int ROW_H = 27;

    // 右列（终极天赋）
    private static final int TALENT_X = 170;
    private static final int TALENT_W = 100;
    private static final int CARD_H = 26;
    private static final int CARD_GAP = 2;

    /** 底部「额外技能」槽位（技能本体未设计，先摆 UI） */
    private static final int EXTRA_SKILL_Y = PANEL_H - 22;
    private static final int EXTRA_SKILL_H = 14;
    /** 行内状态/确认提示的行位置（额外技能槽上方） */
    private static final int STATUS_Y = PANEL_H - 34;

    /** 两段确认的"待选定"卡（第一击存这里，第二击发包） */
    private @Nullable UltimateTalent pendingTalent;

    // ===== 界面内行内反馈 =====

    private @Nullable Component statusText;
    private int statusColor;
    private long statusUntilMs;
    private int @org.jspecify.annotations.Nullable [] lastLevels;

    public UpgradeScreen() {
        super(Component.translatable("gui.zuoyanmod.upgrade.title"));
    }

    /** 页脚行内提示，2.5 秒后自动消失。 */
    private void showStatus(Component text, int color) {
        this.statusText = text;
        this.statusColor = color;
        this.statusUntilMs = Util.getMillis() + 2500L;
    }

    // ===== 控件搭建 =====

    @Override
    protected void init() {
        int px = panelX();
        int py = panelY();

        for (int i = 0; i < UpgradeType.VALUES.length; i++) {
            final int index = i;
            addRenderableWidget(new UpgradeRowButton(
                    px + STATS_X, py + STATS_Y + i * ROW_H, STATS_W, ROW_H - 2,
                    Component.translatable(UpgradeType.VALUES[i].nameKey()),
                    (g, x, y, w, h, hovered) -> drawStatRow(g, index, x, y, w, h, hovered),
                    () -> statTooltip(index),
                    () -> handleStatClick(index),
                    () -> handleStatRefund(index)));
        }
        for (int i = 0; i < UltimateTalent.VALUES.length; i++) {
            final int index = i;
            addRenderableWidget(new UpgradeRowButton(
                    px + TALENT_X, py + 40 + i * (CARD_H + CARD_GAP), TALENT_W, CARD_H,
                    Component.translatable(UltimateTalent.VALUES[i].nameKey()),
                    (g, x, y, w, h, hovered) -> drawTalentCard(g, index, x, y, w, h, hovered),
                    () -> talentTooltip(index),
                    () -> handleTalentClick(UltimateTalent.VALUES[index]),
                    null));
        }
        // 「额外技能」按钮位：技能本体还没设计，先把槽位 UI 摆出来，
        // 点击只回一句"敬请期待"；将来接技能时把 action 换成施法逻辑即可
        addRenderableWidget(new UpgradeRowButton(
                px + 10, py + EXTRA_SKILL_Y, PANEL_W - 20, EXTRA_SKILL_H,
                Component.translatable("gui.zuoyanmod.upgrade.extra_skill"),
                this::drawExtraSkillSlot,
                null,
                () -> showStatus(Component.translatable("gui.zuoyanmod.upgrade.extra_skill_locked"),
                        KleinTheme.TEXT_WARN),
                null));
    }

    /**
     * 自绘可点行：外观回调 + tooltip 回调 + 点击回调。命中 / hover / 按下音效 /
     * 叙述全部由控件框架负责——手写命中判定在 26.3 里和控件框架并行必然出鬼
     * （终端 v5 的"四个按钮全点不动"就是前车之鉴）。
     */
    private final class UpgradeRowButton extends AbstractButton {

        private interface RowPainter {
            void draw(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hovered);
        }

        private final RowPainter painter;
        private final @Nullable Supplier<List<Component>> tooltip;
        private final Runnable action;
        /** Shift+左键动作（属性行 = 退还最后一级；天赋卡没有，传 null） */
        private final @Nullable Runnable shiftAction;

        UpgradeRowButton(int x, int y, int w, int h, Component name,
                         RowPainter painter, @Nullable Supplier<List<Component>> tooltip,
                         Runnable action, @Nullable Runnable shiftAction) {
            super(x, y, w, h, name);
            this.painter = painter;
            this.tooltip = tooltip;
            this.action = action;
            this.shiftAction = shiftAction;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            // Shift+左键 = 退还，普通左键 = 升级。
            // ⚠️ 别在这里用裸数字判断鼠标左右键：26.3 的键值换了体系
            // （InputConstants.MOUSE_BUTTON_LEFT=1 / RIGHT=3，不是 GLFW 的 0/1），
            // 上一版就是拿 1 当"右键"结果把左键拦成了退还。
            if (this.shiftAction != null && input.hasShiftDown()) {
                this.shiftAction.run();
            } else {
                this.action.run();
            }
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            this.painter.draw(g, getX(), getY(), getWidth(), getHeight(), isHovered());
            if (isHovered() && this.tooltip != null) {
                List<Component> lines = this.tooltip.get();
                if (lines != null && !lines.isEmpty()) {
                    g.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
                }
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    // ===== 绘制 =====

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // 同步快照等级上涨 → 界面内闪一条"升级成功"（哪条升的、升到几级都点名）
        int[] levels = new int[UpgradeType.VALUES.length];
        for (int i = 0; i < levels.length; i++) {
            levels[i] = ClientUpgradeData.level(i);
        }
        if (this.lastLevels != null) {
            for (int i = 0; i < levels.length; i++) {
                if (levels[i] > this.lastLevels[i]) {
                    showStatus(Component.translatable("message.zuoyanmod.upgrade.level_up",
                            Component.translatable(UpgradeType.VALUES[i].nameKey()), levels[i]),
                            KleinTheme.GOLD);
                }
            }
        }
        this.lastLevels = levels;

        int px = panelX();
        int py = panelY();

        drawPanel(g, px, py);
        // 行与卡是真控件，走 Screen 的标准控件渲染（面板之上、状态行之下）
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        drawHeader(g, px, py);
        drawStatusLine(g, px, py);
    }

    /** 面板底 + 双层描边 + 顶部青色亮线（klein 面板的三件套） */
    private void drawPanel(GuiGraphicsExtractor g, int px, int py) {
        g.fill(px, py, px + PANEL_W, py + PANEL_H, withAlpha(KleinTheme.PANEL, 0.94F));
        g.outline(px - 1, py - 1, PANEL_W + 2, PANEL_H + 2, KleinTheme.BORDER_OUTER);
        g.outline(px, py, PANEL_W, PANEL_H, KleinTheme.BORDER);
        g.fill(px + 1, py + 1, px + PANEL_W - 1, py + 2, withAlpha(KleinTheme.CYAN, 0.55F));
    }

    private void drawHeader(GuiGraphicsExtractor g, int px, int py) {
        g.text(this.font, this.title, px + 10, py + 8, KleinTheme.GOLD, true);

        Player player = minecraftPlayer();
        if (player != null) {
            Component xp = Component.translatable("gui.zuoyanmod.upgrade.xp", player.experienceLevel);
            g.text(this.font, xp, px + PANEL_W - 10 - this.font.width(xp), py + 8, KleinTheme.CYAN, true);
        }

        int total = ClientUpgradeData.totalLevels();
        int max = UpgradeType.VALUES.length * UpgradeType.MAX_LEVEL;
        // 总进度条：细条 + 右侧数字
        int barX = px + STATS_X;
        int barY = py + 21;
        int barW = STATS_W;
        g.fill(barX, barY, barX + barW, barY + 3, withAlpha(KleinTheme.WELL, 0.9F));
        int filled = Math.round(barW * total / (float) max);
        if (filled > 0) {
            g.fill(barX, barY, barX + filled, barY + 3, KleinTheme.CYAN_DEEP);
            g.fill(barX + filled - 1, barY, barX + filled, barY + 3, KleinTheme.CYAN);
        }
        Component progress = Component.translatable("gui.zuoyanmod.upgrade.progress", total, max);
        g.text(this.font, progress, px + TALENT_X + TALENT_W - this.font.width(progress), py + 19,
                total >= max ? KleinTheme.GOLD : KleinTheme.TEXT_DIM, false);
    }

    private void drawStatRow(GuiGraphicsExtractor g, int index, int x, int y, int w, int h, boolean hovered) {
        UpgradeType type = UpgradeType.VALUES[index];
        int level = ClientUpgradeData.level(index);
        boolean maxed = level >= UpgradeType.MAX_LEVEL;
        int cost = UpgradeType.xpCost(level);
        Player player = minecraftPlayer();
        boolean affordable = !maxed && player != null && player.experienceLevel >= cost;

        if (hovered && !maxed) {
            g.fill(x, y, x + w, y + h, withAlpha(KleinTheme.BORDER, 0.30F));
        }

        // 行 1：名称 + Lv
        g.text(this.font, Component.translatable(type.nameKey()), x + 2, y + 2,
                maxed ? KleinTheme.GOLD : KleinTheme.TEXT, false);
        Component lv = Component.translatable("gui.zuoyanmod.upgrade.level", level);
        g.text(this.font, lv, x + w - 2 - this.font.width(lv), y + 2, KleinTheme.TEXT_DIM, false);

        // 行 2：10 段进度格 + 花费/状态
        int pipY = y + 13;
        for (int p = 0; p < UpgradeType.MAX_LEVEL; p++) {
            int pipX = x + 2 + p * 8;
            if (p < level) {
                g.fill(pipX, pipY, pipX + 6, pipY + 4,
                        p == level - 1 ? KleinTheme.CYAN : KleinTheme.CYAN_DEEP);
            } else {
                g.fill(pipX, pipY, pipX + 6, pipY + 4, withAlpha(KleinTheme.WELL, 0.95F));
            }
        }

        Component status;
        int statusColor;
        if (maxed) {
            status = Component.translatable("gui.zuoyanmod.upgrade.maxed");
            statusColor = KleinTheme.GOLD;
        } else if (affordable) {
            status = Component.translatable("gui.zuoyanmod.upgrade.cost", cost);
            statusColor = KleinTheme.TEXT_DIM;
        } else {
            status = Component.translatable("gui.zuoyanmod.upgrade.cost", cost);
            statusColor = KleinTheme.TEXT_WARN;
        }
        g.text(this.font, status, x + w - 2 - this.font.width(status), pipY - 1, statusColor, false);
    }

    private void drawTalentCard(GuiGraphicsExtractor g, int index, int x, int y, int w, int h, boolean hovered) {
        UltimateTalent talent = UltimateTalent.VALUES[index];
        boolean unlocked = ClientUpgradeData.allMaxed();
        UltimateTalent chosen = ClientUpgradeData.talent();
        boolean isChosen = chosen == talent;
        boolean pending = pendingTalent == talent;

        int border;
        int nameColor;
        if (isChosen || pending) {
            border = KleinTheme.GOLD;
            nameColor = KleinTheme.GOLD;
        } else if (unlocked && chosen == null && hovered) {
            border = KleinTheme.BORDER_BRIGHT;
            nameColor = KleinTheme.TEXT;
        } else if (!unlocked || chosen != null) {
            border = withAlpha(KleinTheme.BORDER_OUTER, 0.9F);
            nameColor = KleinTheme.TEXT_FAINT;
        } else {
            border = KleinTheme.BORDER;
            nameColor = KleinTheme.TEXT;
        }
        g.fill(x, y, x + w, y + h, withAlpha(KleinTheme.PANEL_DEEP, !unlocked ? 0.55F : 0.92F));
        g.outline(x, y, w, h, border);

        drawTalentGlyph(g, talent, x + 5, y + h / 2 - 4,
                nameColor == KleinTheme.TEXT_FAINT ? KleinTheme.TEXT_FAINT : KleinTheme.CYAN);
        Component name = Component.translatable(talent.nameKey());
        g.text(this.font, name, x + 20, y + h / 2 - 4, nameColor, false);
    }

    /**
     * 行内状态行（额外技能槽上方）：升级成败反馈 / 天赋两段确认提示。
     * 原先这里的"冷却剩余/Y 已就绪"常驻提示已删——就绪走 toast，查询按 Y。
     */
    private void drawStatusLine(GuiGraphicsExtractor g, int px, int py) {
        int y = py + STATUS_Y;
        if (this.statusText != null && Util.getMillis() < this.statusUntilMs) {
            g.text(this.font, this.statusText, px + PANEL_W / 2 - this.font.width(this.statusText) / 2, y,
                    this.statusColor, false);
            return;
        }
        this.statusText = null;

        if (pendingTalent != null) {
            Component confirm = Component.translatable("gui.zuoyanmod.upgrade.confirm_hint",
                    Component.translatable(pendingTalent.nameKey()));
            g.text(this.font, confirm, px + PANEL_W / 2 - this.font.width(confirm) / 2, y,
                    KleinTheme.GOLD, false);
        }
    }

    /**
     * 「额外技能」槽位的背景 UI（技能本体未设计）。做成技能栏格子的观感：
     * 深色井 + 描边 + 顶部亮线，左侧一枚四芒星图标，右侧"敬请期待"。
     */
    private void drawExtraSkillSlot(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hovered) {
        g.fill(x, y, x + w, y + h, withAlpha(KleinTheme.PANEL_DEEP, 0.92F));
        g.outline(x, y, w, h, hovered ? KleinTheme.BORDER_BRIGHT : KleinTheme.BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + 2,
                withAlpha(KleinTheme.CYAN, hovered ? 0.45F : 0.18F));

        // 左侧图标格：小井 + 四芒星
        g.fill(x + 3, y + 2, x + 13, y + h - 2, withAlpha(KleinTheme.WELL, 0.9F));
        g.outline(x + 3, y + 2, 10, h - 4, withAlpha(KleinTheme.WELL_EDGE, 0.9F));
        int cx = x + 8;
        int cy = y + h / 2;
        g.fill(cx, y + 3, cx + 1, y + h - 3, hovered ? KleinTheme.GOLD : KleinTheme.CYAN_DEEP);
        g.fill(x + 4, cy, x + 12, cy + 1, hovered ? KleinTheme.GOLD : KleinTheme.CYAN_DEEP);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, hovered ? KleinTheme.GOLD : KleinTheme.CYAN);

        Component name = Component.translatable("gui.zuoyanmod.upgrade.extra_skill");
        g.text(this.font, name, x + 18, y + (h - 8) / 2,
                hovered ? KleinTheme.TEXT : KleinTheme.TEXT_DIM, false);
        Component hint = Component.translatable("gui.zuoyanmod.upgrade.extra_skill_hint");
        g.text(this.font, hint, x + w - 3 - this.font.width(hint), y + (h - 8) / 2,
                KleinTheme.TEXT_FAINT, false);
    }

    /** 四枚 8×8 几何小图标，全 fill 画，不依赖字形覆盖（❄/⚛ 这类字符在中文字体下没有保证）。 */
    private void drawTalentGlyph(GuiGraphicsExtractor g, UltimateTalent talent, int x, int y, int color) {
        switch (talent) {
            case GRAPPLE_ABSOLUTE_ZERO -> {
                // 向下箭头 + 底部小方（"拽到身边"）：箭杆 + 两撇 + 落点
                g.fill(x + 3, y, x + 5, y + 5, color);
                g.fill(x + 1, y + 3, x + 3, y + 5, color);
                g.fill(x + 5, y + 3, x + 7, y + 5, color);
                g.fill(x + 2, y + 7, x + 6, y + 8, color);
            }
            case SKY_LAUNCH -> {
                // 向上箭头：箭杆 + 顶部两撇
                g.fill(x + 3, y + 3, x + 5, y + 8, color);
                g.fill(x + 1, y + 2, x + 3, y + 4, color);
                g.fill(x + 5, y + 2, x + 7, y + 4, color);
                g.fill(x + 3, y, x + 5, y + 2, KleinTheme.GOLD);
            }
            case MOLECULAR_DISSOCIATION -> {
                // 原子：中心点 + 斜十字环
                g.fill(x + 3, y + 3, x + 5, y + 5, KleinTheme.MAGENTA);
                g.fill(x + 1, y + 3, x + 3, y + 5, color);
                g.fill(x + 5, y + 3, x + 7, y + 5, color);
                g.fill(x + 3, y + 1, x + 5, y + 3, color);
                g.fill(x + 3, y + 5, x + 5, y + 7, color);
            }
            case PRIMORDIAL_BLACK_HOLE -> {
                // 黑洞：暗核 + 亮吸积环
                g.fill(x + 2, y + 2, x + 6, y + 6, KleinTheme.PANEL_DEEP);
                g.fill(x + 3, y + 3, x + 5, y + 5, 0xFF000000);
                g.outline(x + 2, y + 2, 4, 4, color);
            }
        }
    }

    // ===== tooltip 内容 =====

    private @Nullable List<Component> statTooltip(int index) {
        UpgradeType type = UpgradeType.VALUES[index];
        int level = ClientUpgradeData.level(index);
        List<Component> lines = new java.util.ArrayList<>(List.of(
                Component.translatable(type.nameKey()).withStyle(net.minecraft.ChatFormatting.GOLD),
                Component.translatable(type.valueKey(), formatPerLevel(type)),
                Component.translatable("gui.zuoyanmod.upgrade.value_now", formatTotal(type, level)),
                Component.translatable("gui.zuoyanmod.upgrade.value_next",
                        formatTotal(type, Math.min(level + 1, UpgradeType.MAX_LEVEL)))));
        if (level > 0) {
            lines.add(Component.translatable("gui.zuoyanmod.upgrade.refund_hint")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        return lines;
    }

    private @Nullable List<Component> talentTooltip(int index) {
        UltimateTalent talent = UltimateTalent.VALUES[index];
        boolean unlocked = ClientUpgradeData.allMaxed();
        UltimateTalent chosen = ClientUpgradeData.talent();
        List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable(talent.nameKey()).withStyle(net.minecraft.ChatFormatting.GOLD));
        lines.add(Component.translatable(talent.descKey()));
        // 基础冷却写进描述（动态计算，跟随配置倍率）；选定后下面另有剩余时间的状态行
        lines.add(Component.translatable("gui.zuoyanmod.upgrade.talent_cooldown",
                (talent.cooldownTicks() + 19) / 20));
        if (chosen == talent) {
            long seconds = ClientUpgradeData.cooldownRemainingSeconds(talent);
            lines.add(seconds > 0
                    ? Component.translatable("gui.zuoyanmod.upgrade.cooldown", seconds)
                            .withStyle(net.minecraft.ChatFormatting.RED)
                    : Component.translatable("gui.zuoyanmod.upgrade.ready")
                            .withStyle(net.minecraft.ChatFormatting.AQUA));
        } else if (!unlocked) {
            lines.add(Component.translatable("gui.zuoyanmod.upgrade.locked")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        } else if (chosen != null) {
            lines.add(Component.translatable("gui.zuoyanmod.upgrade.taken_other")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        } else if (pendingTalent == talent) {
            lines.add(Component.translatable("gui.zuoyanmod.upgrade.confirm")
                    .withStyle(net.minecraft.ChatFormatting.YELLOW));
        }
        return lines;
    }

    // ===== 交互 =====

    /**
     * 点属性行：先在客户端就地校验（经验/满级），不过关直接在页脚给原因，
     * 不发注定失败的包——服务端会拒绝，但它的提示在界面打开时看不见。
     */
    private void handleStatClick(int index) {
        UpgradeType type = UpgradeType.VALUES[index];
        int level = ClientUpgradeData.level(index);
        Player player = minecraftPlayer();
        if (player == null) {
            return;
        }
        if (level >= UpgradeType.MAX_LEVEL) {
            showStatus(Component.translatable("gui.zuoyanmod.upgrade.maxed"), KleinTheme.GOLD);
            return;
        }
        int cost = UpgradeType.xpCost(level);
        if (player.experienceLevel < cost) {
            showStatus(Component.translatable("message.zuoyanmod.upgrade.no_xp", cost),
                    KleinTheme.TEXT_WARN);
            return;
        }
        LOGGER.info("[Upgrade] 客户端发送升级请求 statIndex={} (当前 Lv.{}, 经验 {} 级)",
                index, level, player.experienceLevel);
        PacketHandler.sendUpgradeStat(index);
    }

    /** Shift+左键属性行：退还最后一级。客户端只拦"没点可退"，能不能退以服务端为准。 */
    private void handleStatRefund(int index) {
        UpgradeType type = UpgradeType.VALUES[index];
        if (ClientUpgradeData.level(index) <= 0) {
            showStatus(Component.translatable("message.zuoyanmod.upgrade.refund_empty",
                    Component.translatable(type.nameKey())), KleinTheme.TEXT_WARN);
            return;
        }
        LOGGER.info("[Upgrade] 客户端发送退还请求 statIndex={} (当前 Lv.{})", index, ClientUpgradeData.level(index));
        PacketHandler.sendRefundStat(index);
    }

    /** 两段确认：第一击标记待选卡，第二击（同一张）真正发选定包；点了选不了的卡给原因。 */
    private void handleTalentClick(UltimateTalent talent) {
        boolean unlocked = ClientUpgradeData.allMaxed();
        UltimateTalent chosen = ClientUpgradeData.talent();
        if (!unlocked) {
            showStatus(Component.translatable("message.zuoyanmod.upgrade.not_maxed"), KleinTheme.TEXT_WARN);
            return;
        }
        if (chosen != null) {
            if (chosen != talent) {
                showStatus(Component.translatable("message.zuoyanmod.upgrade.talent_taken"),
                        KleinTheme.TEXT_WARN);
            }
            return;
        }
        if (pendingTalent == talent) {
            PacketHandler.sendChooseTalent(talent.ordinal());
            pendingTalent = null;
        } else {
            pendingTalent = talent;
        }
    }

    // ===== 工具 =====

    private int panelX() {
        return (this.width - PANEL_W) / 2;
    }

    private int panelY() {
        return (this.height - PANEL_H) / 2;
    }

    private static int withAlpha(int argb, float alpha) {
        return KleinTheme.withAlpha(argb, alpha);
    }

    private @Nullable Player minecraftPlayer() {
        return net.minecraft.client.Minecraft.getInstance().player;
    }

    /** 每级加成的显示值（比例类转成百分数）。 */
    private static String formatPerLevel(UpgradeType type) {
        return formatBonus(type, type.perLevel());
    }

    /** 升到 level 级时的累计加成显示值。 */
    private static String formatTotal(UpgradeType type, int level) {
        return formatBonus(type, type.totalBonus(level));
    }

    private static String formatBonus(UpgradeType type, double value) {
        if (type == UpgradeType.MOVEMENT_SPEED || type == UpgradeType.DAMAGE_REDUCTION) {
            return String.format("%+.0f%%", value * 100.0D);
        }
        if (value == Math.floor(value)) {
            return String.format("%+.0f", value);
        }
        return String.format("%+.1f", value);
    }
}
