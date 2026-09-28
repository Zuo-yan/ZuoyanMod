package org.gwfx.zuoyanmod.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.gwfx.zuoyanmod.Zuoyanmod;

import java.util.function.Consumer;

/**
 * 湮灭君王之刃：湮灭君主的必掉武器。
 *
 * <p>本体是一把高面板剑（攻击 35、攻速 2.4，属性在 {@code ItemRegistry} 里按
 * {@code swordAttributes/swordComponents} 范式挂），特色被动：命中
 * {@code zuoyanmod:bosses} 标签的实体时，追加一次相当于攻击力 50% 的魔法伤害——
 * 对肉度拉满的 Boss 战收益显著，对普通怪只是锦上添花。
 */
public class MonarchBladeItem extends Item {

    /** 对 Boss 目标的追加魔法伤害系数（基于攻击者最终攻击力） */
    private static final double BOSS_BONUS_FRACTION = 0.5D;

    /** 与数据包 tags/entity_type/bosses.json 对应的实体类型标签 */
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> BOSSES_TAG =
            TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "bosses"));

    public MonarchBladeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (target.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                && target.isAlive()
                && BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(target.getType()).is(BOSSES_TAG)) {
            float bonus = (float) (attacker.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                    * BOSS_BONUS_FRACTION);
            target.hurtServer(serverLevel, attacker.damageSources().magic(), bonus);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.zuoyanmod.monarch_blade.desc1"));
        tooltip.accept(Component.translatable("item.zuoyanmod.monarch_blade.desc2"));
        tooltip.accept(Component.translatable("item.zuoyanmod.monarch_blade.desc3"));
    }
}
