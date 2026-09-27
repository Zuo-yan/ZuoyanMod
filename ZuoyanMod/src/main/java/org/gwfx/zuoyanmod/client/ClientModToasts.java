package org.gwfx.zuoyanmod.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import net.minecraft.network.chat.Component;

/**
 * 客户端 Toast 队列：模组效果通知的瞬时状态。
 *
 * <p>服务端 {@code ModToastPacket} 到达后进队；{@code ModToastHud} 每帧取
 * {@link #active()} 渲染并顺带清理过期项。用系统毫秒计时而不是 gameTime：
 * 通知是纯 UI 反馈，暂停/掉刻时不该被游戏时间牵着走。
 */
public final class ClientModToasts {

    /** 单条存续时长（毫秒）：3.2 秒完整显示 + 0.8 秒淡出 */
    public static final long LIFETIME_MS = 4000L;
    /** 淡出占生命周期的比例（最后 20% 透明度线性降到 0） */
    public static final float FADE_FRACTION = 0.2F;
    /** 同屏最多同时显示几条，超出时最旧的先消失 */
    public static final int MAX_VISIBLE = 6;

    private static final long FADE_MS = (long) (LIFETIME_MS * FADE_FRACTION);

    /** 一条通知：文本 + 入队时刻（系统毫秒） */
    public record Toast(Component text, long bornAt) {
        /** 剩余透明度 0~1：前 80% 恒 1，最后 20% 线性降 0 */
        public float alphaAt(long now) {
            long age = now - bornAt;
            if (age >= LIFETIME_MS) return 0F;
            long fadeStart = LIFETIME_MS - FADE_MS;
            return age <= fadeStart ? 1F : (LIFETIME_MS - age) / (float) FADE_MS;
        }
    }

    private static final Deque<Toast> TOASTS = new ArrayDeque<>();

    private ClientModToasts() {}

    /** 新通知置顶（最新在最上），超出上限丢最旧的 */
    public static void push(Component text) {
        TOASTS.addFirst(new Toast(text, System.currentTimeMillis()));
        while (TOASTS.size() > MAX_VISIBLE * 2) {
            TOASTS.removeLast();
        }
    }

    /** 当前应显示的通知（新的在前），顺带清理过期项 */
    public static List<Toast> active() {
        long now = System.currentTimeMillis();
        List<Toast> visible = new ArrayList<>(MAX_VISIBLE);
        Iterator<Toast> it = TOASTS.iterator();
        while (it.hasNext() && visible.size() < MAX_VISIBLE) {
            Toast t = it.next();
            if (now - t.bornAt() >= LIFETIME_MS) {
                it.remove();
            } else {
                visible.add(t);
            }
        }
        return visible;
    }
}
