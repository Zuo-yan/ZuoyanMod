package org.gwfx.zuoyanmod.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.gwfx.zuoyanmod.network.PacketHandler;

public class DeathNoteScreen extends Screen {

    private final ItemStack stack;
    private EditBox targetBox;
    private EditBox durationBox;

    public DeathNoteScreen(ItemStack stack) {
        super(Component.literal("死亡笔记"));
        this.stack = stack;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addRenderableWidget(new StringWidget(centerX - 100, centerY - 60, 200, 16, Component.literal("§c死亡笔记 - 写入目标"), this.font));
        this.addRenderableWidget(new StringWidget(centerX - 100, centerY - 44, 200, 14, Component.literal("目标玩家 ID:"), this.font));

        this.targetBox = new EditBox(this.font, centerX - 100, centerY - 28, 200, 20, Component.literal("玩家ID"));
        this.targetBox.setMaxLength(64);
        this.targetBox.setValue("");
        this.addRenderableWidget(this.targetBox);

        this.addRenderableWidget(new StringWidget(centerX - 100, centerY - 2, 200, 14, Component.literal("持续秒数 (默认 41 秒):"), this.font));

        this.durationBox = new EditBox(this.font, centerX - 100, centerY + 14, 200, 20, Component.literal("时长秒数"));
        this.durationBox.setMaxLength(6);
        this.durationBox.setValue("41");
        this.addRenderableWidget(this.durationBox);

        this.addRenderableWidget(Button.builder(Component.literal("写入死亡笔记"), button -> submit())
                .bounds(centerX - 100, centerY + 42, 200, 20)
                .build());
    }

    private void submit() {
        String target = this.targetBox.getValue().trim();
        String durationText = this.durationBox.getValue().trim();
        int seconds;
        try {
            seconds = Integer.parseInt(durationText);
        } catch (NumberFormatException e) {
            return;
        }
        if (target.isEmpty() || seconds <= 0) {
            return;
        }
        PacketHandler.sendDeathNote(target, seconds);
        if (this.minecraft != null && this.minecraft.gui != null) {
            this.minecraft.gui.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}