package org.gwfx.zuoyanmod.sound;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier; // 改为 Identifier
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.gwfx.zuoyanmod.Zuoyanmod;

public class SoundRegistry {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, Zuoyanmod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ICE_TEA_DRINK = SOUND_EVENTS.register(
            "drink.ice_tea",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "drink.ice_tea"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> DOMAIN_EXPANSION_ACTIVATE = SOUND_EVENTS.register(
            "domain_expansion.activate",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "domain_expansion.activate"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> DOMAIN_EXPANSION_MUSIC = SOUND_EVENTS.register(
            "domain_expansion.music",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "domain_expansion.music"))
    );

    /**
     * 因果律手枪射击音：TACZ（永恒枪械工坊，GPL-3.0）的沙漠之鹰第一人称射击声。
     * 音源位于 assets/zuoyanmod/sounds/causality_pistol/shoot.ogg。
     */
    public static final DeferredHolder<SoundEvent, SoundEvent> CAUSALITY_PISTOL_SHOOT = SOUND_EVENTS.register(
            "causality_pistol.shoot",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "causality_pistol.shoot"))
    );
}