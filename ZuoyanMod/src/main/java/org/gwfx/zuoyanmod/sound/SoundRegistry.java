package org.gwfx.zuoyanmod.sound;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import org.gwfx.zuoyanmod.Zuoyanmod;

public class SoundRegistry {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, Zuoyanmod.MODID);

    /** 冷饮：26.3 里随饮品改名（ice_tea → chilled），避免日后加别的冰饮时名字指向偏甜的具体产品。 */
    public static final RegistryObject<SoundEvent> CHILLED_DRINK = SOUND_EVENTS.register(
            "drink.chilled",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Zuoyanmod.MODID, "drink.chilled"))
    );

    public static final RegistryObject<SoundEvent> DOMAIN_EXPANSION_ACTIVATE = SOUND_EVENTS.register(
            "domain_expansion.activate",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Zuoyanmod.MODID, "domain_expansion.activate"))
    );

    /**
     * 领域展开的战斗 BGM：音源在 sounds.json 里指向 {@code zuoyanmod:music_disc/castle}，
     * 与 Castle 唱片共用同一个 ogg —— 不复制一份是为了不把 2.6MB 重复打进 jar。
     * 好处是换 BGM 只需改 sounds.json 的一行，不用动代码也不用重新编译。
     *
     * <p>衰减距离在 sounds.json 里放大到 64：竞技场是 50x50，中心到对角约 35 格，
     * 而音效默认只有 16 格，照默认的话只有站在台子正中间才听得见。
     */
    public static final RegistryObject<SoundEvent> DOMAIN_EXPANSION_MUSIC = SOUND_EVENTS.register(
            "domain_expansion.music",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Zuoyanmod.MODID, "domain_expansion.music"))
    );

    /**
     * 因果律手枪射击音：TACZ（永恒枪械工坊，GPL-3.0）的沙漠之鹰第一人称射击声。
     * 音源位于 assets/zuoyanmod/sounds/causality_pistol/shoot.ogg。
     */
    public static final RegistryObject<SoundEvent> CAUSALITY_PISTOL_SHOOT = SOUND_EVENTS.register(
            "causality_pistol.shoot",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Zuoyanmod.MODID, "causality_pistol.shoot"))
    );

    // ===== 音乐唱片（三首完整曲子）=====
    // 命名约定：唱片物品 id 为 music_disc_<name>，音效 id 为 music_disc.<name>，两者一一对应。
    //
    // 1.20.1 适配：26.x 的曲目元数据走数据包注册表 data/<ns>/jukebox_song/<name>.json
    // （时长 / 比较器输出 / 描述都在那里，物品挂 JUKEBOX_PLAYABLE 组件引用），
    // 1.20.1 没有这套机制 —— 唱片是 {@code RecordItem}，曲目时长就是 ogg 本身的长度，
    // 比较器输出是构造参数。所以这里只注册音效，物品侧用 RecordItem 接上即可。
    public static final RegistryObject<SoundEvent> MUSIC_DISC_SHOTS = musicDisc("shots");

    public static final RegistryObject<SoundEvent> MUSIC_DISC_NIGHT_DANCER = musicDisc("night_dancer");

    public static final RegistryObject<SoundEvent> MUSIC_DISC_CASTLE = musicDisc("castle");

    private static RegistryObject<SoundEvent> musicDisc(String name) {
        String path = "music_disc." + name;
        return SOUND_EVENTS.register(path,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Zuoyanmod.MODID, path)));
    }
}
