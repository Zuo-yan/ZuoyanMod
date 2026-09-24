package org.gwfx.zuoyanmod.sound;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier; // 改为 Identifier
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

    /**
     * 领域展开的战斗 BGM：音源在 sounds.json 里指向 {@code zuoyanmod:music_disc/castle}，
     * 与 Castle 唱片共用同一个 ogg——不复制一份是为了不把 2.6MB 重复打进 jar。
     * 好处是换 BGM 只需改 sounds.json 的一行，不用动代码也不用重新编译。
     * <p>
     * 衰减距离在 sounds.json 里放大到 64：竞技场是 50x50，中心到对角约 35 格，
     * 而音效默认只有 16 格，照默认的话只有站在台子正中间才听得见。
     */
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

    // ===== 音乐唱片（三首约 3 分钟的完整曲子）=====
    // 命名约定：唱片物品 id 为 music_disc_<name>，音效 id 为 music_disc.<name>，
    // 点歌数据 id 与曲目名同名（data/zuoyanmod/jukebox_song/<name>.json）。三者一一对应。
    //
    // JukeboxSong 是数据包注册表，实体不在代码里注册——data/ 下的 json 就是注册表条目，
    // 这里只留 ResourceKey 供物品挂 jukeboxPlayable 组件时引用。
    // 时长必须与 ogg 实测一致（tools/ogg_info.py）：写短了唱片会提前停，写长了比较器多亮。
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

    /**
     * 唱片音效统一走 createVariableRangeEvent（区间 16 格）：
     * 唱片是点声源，音量按距离衰减交给音效系统，与普通音效同一套规则即可；
     * 真正的"长音频"问题由 sounds.json 里的 stream: true 解决（不流式会把整条 PCM 常驻内存）。
     */
    private static DeferredHolder<SoundEvent, SoundEvent> musicDisc(String name) {
        String path = "music_disc." + name;
        return SOUND_EVENTS.register(path,
                () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, path)));
    }
}