package org.gwfx.zuoyanmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 一击必杀：持有者下一次对任何目标造成的伤害会被换成 {@link Float#MAX_VALUE}，然后本效果被消耗掉。
 *
 * <p><b>为什么改名：</b>注册 id 仍然叫 {@code mamba_force_attack}（lang 里英文是 Mamba Force、
 * 中文是「你嘴唇有点发紫.」），但那个名字说的是黑曼巴这个梗，读不出任何行为。
 * 而这个类在代码里唯一的作用就是"提供一次必杀"，由
 * {@link org.gwfx.zuoyanmod.event.InstantKillEventHandler} 消费，所以名字按行为取 ——
 * 读代码的人不必为了搞懂它去翻 lang 文件。
 *
 * <p><b>注册 id 为什么不动：</b>它是存档与命令的对外契约 —— 旧存档里保存的效果、
 * {@code /effect} 命令、lang 键 {@code effect.zuoyanmod.mamba_force_attack} 全按 id 取，
 * 改名等于让旧存档里的效果失效。只改 Java 侧的类名，id 保持不动。
 *
 * <p><b>为什么这里没有逻辑：</b>要拿到原始伤害来源再转发给目标，只能在伤害事件里做，
 * {@code MobEffect} 的生命周期里没有"这一次攻击"的上下文。所以本类是空实现，替换发生在事件处理器。
 *
 * <p><b>施加途径：</b>目前只有巧乐兹（{@code item/ChocolateCrispItem}），吃下去给自己上 1200 tick。
 */
public class InstantKillEffect extends MobEffect {

    public InstantKillEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00BFFF);
    }
}
