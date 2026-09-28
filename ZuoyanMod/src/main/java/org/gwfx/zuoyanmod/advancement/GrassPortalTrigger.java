package org.gwfx.zuoyanmod.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.gwfx.zuoyanmod.Zuoyanmod;

/**
 * "多此一举"成就的触发器：成功点燃草原传送门（门框被填充成门）时 {@link #trigger} 一下。
 *
 * <p>用自定义 criterion 而不是程序化查表授予：成就 JSON 里只声明
 * {@code "trigger": "zuoyanmod:grass_portal_ignited"}，授予路径全部走原版
 * {@link SimpleCriterionTrigger#trigger} 管线，不触碰 26.3 成就管理器的内部查找 API。
 *
 * <p>结构与原版 {@code ChangeDimensionTrigger} 同款：单 record 实例 +
 * 可选 player 谓词字段（成就 JSON 不写 player 时就是恒真）。
 */
public final class GrassPortalTrigger extends SimpleCriterionTrigger<GrassPortalTrigger.Instance> {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, Zuoyanmod.MODID);

    public static final DeferredHolder<CriterionTrigger<?>, GrassPortalTrigger> GRASS_PORTAL_IGNITED =
            TRIGGERS.register("grass_portal_ignited", GrassPortalTrigger::new);

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    /** 点燃成功时调用；当前无附加条件，任何监听中的实例都判命中。 */
    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> true);
    }

    public record Instance(Optional<Holder<LootItemCondition>> player) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    LootItemCondition.CODEC.optionalFieldOf("player").forGetter(Instance::player)
                )
                .apply(i, Instance::new)
        );

        public static Criterion<Instance> ignited() {
            return GRASS_PORTAL_IGNITED.get().createCriterion(new Instance(Optional.empty()));
        }
    }
}
