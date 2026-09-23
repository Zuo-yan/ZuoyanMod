package org.gwfx.zuoyanmod.client;

import net.minecraft.world.item.ItemStack;

/**
 * 死亡笔记的<b>纯客户端</b>入口。
 *
 * <p>为什么要单独一个类，而不是把这几行直接写在 {@code DeathNoteItem} 里：
 * <p>只要一个类的字节码里出现 {@code new SomeScreen(...)}，JVM 在<b>校验</b>该方法时
 * 就必须加载该 Screen 及其父类（要判断能不能赋给 {@code setScreen(Screen)} 的参数类型）。
 * 于是 {@code DeathNoteItem} 一旦在服务端被链接（物品注册时就会），
 * 就会抛 {@code NoClassDefFoundError: net/minecraft/client/gui/screens/Screen}，
 * 整个模组加载失败。
 *
 * <p>把客户端代码挪到 client 包、并且<b>方法签名只用通用类型</b>（这里是 {@link ItemStack}），
 * 服务端的校验就只会去解析 {@code ItemStack}，永远碰不到 Screen。
 * 这个方法本身也只在客户端被调用。
 */
public final class DeathNoteClient {

    private DeathNoteClient() {
    }

    /** 打开死亡笔记界面。只在客户端调用。 */
    public static void openScreen(ItemStack stack) {
        net.minecraft.client.Minecraft.getInstance().gui.setScreen(new DeathNoteScreen(stack));
    }
}
