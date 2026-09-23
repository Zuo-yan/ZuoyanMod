package org.gwfx.zuoyanmod.event;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/**
 * 灵境切换键（Home）的 KeyMapping 常量。
 *
 * <p>1.20.1 Forge 里按键注册由主类 {@code Zuoyanmod.ClientModEvents} 在
 * MOD 总线的 {@code RegisterKeyMappingsEvent} 里完成；按键消费在
 * {@link RealmKeyInputHandler}（FORGE 总线）。本类只持有常量。
 *
 * <p>1.20.1 的 KeyMapping 类别是字符串（26.x 是 Category 对象），语言键为
 * {@code key.categories.zuoyan}。
 */
public final class RealmKeybindHandler {

    /** 按键类别：语言键 key.categories.zuoyan */
    public static final String CATEGORY = "zuoyan";

    public static final KeyMapping TOGGLE_REALM =
            new KeyMapping("key.zuoyanmod.toggle_realm", InputConstants.KEY_HOME, CATEGORY);

    private RealmKeybindHandler() {}
}
