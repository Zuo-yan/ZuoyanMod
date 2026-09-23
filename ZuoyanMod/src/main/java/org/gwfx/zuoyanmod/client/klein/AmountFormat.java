package org.gwfx.zuoyanmod.client.klein;

import java.util.Locale;

/**
 * 数量的显示格式。
 *
 * <p>槽位右下角放的是 {@link #compact(long)}（{@code 1.2k} / {@code 3.4M}），
 * tooltip 里放的是 {@link #grouped(long)}（{@code 1,234,567}，带千分位）。
 * 前者是 AE2 的 {@code AmountFormat.SLOT} 观感，后者是 RS2 的 "detailed tooltip"。
 * 两个都要有：槽位要**一眼能比大小**，tooltip 要**能读到确切数字**。
 */
public final class AmountFormat {

    private AmountFormat() {}

    /** 槽位用：最多 4 个字符，保证在任何缩放档位下都放得进 16px 的格子 */
    public static String compact(long amount) {
        if (amount < 0) {
            return "0";
        }
        // 每个档位的上界取「四舍五入后仍不到 1000」的值，免得 999,999 被显示成 1000k
        if (amount < 1_000L) {
            return Long.toString(amount);
        }
        if (amount < 999_950L) {
            return trim(amount / 1_000.0) + "k";
        }
        if (amount < 999_950_000L) {
            return trim(amount / 1_000_000.0) + "M";
        }
        if (amount < 999_950_000_000L) {
            return trim(amount / 1_000_000_000.0) + "G";
        }
        return trim(amount / 1_000_000_000_000.0) + "T";
    }

    /** tooltip 用：带千分位的完整数字 */
    public static String grouped(long amount) {
        return String.format(Locale.ROOT, "%,d", amount);
    }

    /** 保留一位小数，整数则不留小数点（1.0k → 1k，1.25k → 1.3k） */
    private static String trim(double value) {
        double rounded = Math.floor(value * 10.0 + 0.5) / 10.0;
        if (rounded == Math.floor(rounded)) {
            return Long.toString((long) rounded);
        }
        return String.format(Locale.ROOT, "%.1f", rounded);
    }
}
