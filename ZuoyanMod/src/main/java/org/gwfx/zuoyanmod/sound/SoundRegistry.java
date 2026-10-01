package org.gwfx.zuoyanmod.sound;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.JukeboxSong;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.gwfx.zuoyanmod.Zuoyanmod;

public class SoundRegistry {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, Zuoyanmod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> CHILLED_DRINK = SOUND_EVENTS.register(
            "drink.chilled",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "drink.chilled"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> DOMAIN_EXPANSION_ACTIVATE = SOUND_EVENTS.register(
            "domain_expansion.activate",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "domain_expansion.activate"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> DOMAIN_EXPANSION_MUSIC = SOUND_EVENTS.register(
            "domain_expansion.music",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "domain_expansion.music"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> CAUSALITY_PISTOL_SHOOT = SOUND_EVENTS.register(
            "causality_pistol.shoot",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "causality_pistol.shoot"))
    );

    // ===== 超级电能机枪豌豆音效 =====
    public static final DeferredHolder<SoundEvent, SoundEvent> SUPER_GATLING_PEA_SHOOT = SOUND_EVENTS.register(
            "entity.super_electric_gatling_pea.shoot",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "entity.super_electric_gatling_pea.shoot"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> SUPER_GATLING_PEA_ULT = SOUND_EVENTS.register(
            "entity.super_electric_gatling_pea.ult",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "entity.super_electric_gatling_pea.ult"))
    );

    // ===== 音乐唱片 =====
    public static final ResourceKey<JukeboxSong> SONG_SHOTS = songKey("shots");
    public static final ResourceKey<JukeboxSong> SONG_NIGHT_DANCER = songKey("night_dancer");
    public static final ResourceKey<JukeboxSong> SONG_CASTLE = songKey("castle");

    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DISC_SHOTS = musicDisc("shots");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DISC_NIGHT_DANCER = musicDisc("night_dancer");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DISC_CASTLE = musicDisc("castle");

    private static ResourceKey<JukeboxSong> songKey(String name) {
        return ResourceKey.create(Registries.JUKEBOX_SONG,
                Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, name));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> musicDisc(String name) {
        String path = "music_disc." + name;
        return SOUND_EVENTS.register(path,
                () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, path)));
    }
}

