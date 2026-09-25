package org.gwfx.zuoyanmod.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

/**
 * 超平坦世界（{@code zuoyanmod:realm}）用的区块生成器。
 *
 * <p>地形部分完全复用原版 {@link FlatLevelSource}（JSON 的 {@code settings} 字段格式一模一样），
 * 区别只在装饰阶段：跑完原版逻辑后，再铺一层「分层矿物带」——见 {@link RealmOreBandGenerator}。
 *
 * <p>为什么要自定义生成器而不是纯数据包：
 * 原版的矿物挂在群系的 features 里，而 {@code minecraft:flat} 的 {@code features:true}
 * 会把这个群系的<b>全部</b>地物（树、草、花、洞穴装饰…）一起生成。既要「要矿物」又要
 * 「地表完全干净」，就只能自己接管这一步。顺带也解决了「后续模组新增的矿物自动进矿层」：
 * 面板是运行时扫描注册表得来的，不需要手写 JSON。
 *
 * <p>用法：维度 JSON 里把 {@code "type"} 写成 {@code "zuoyanmod:realm_flat"}，其余照抄
 * {@code minecraft:flat}。
 *
 * <h2>1.20.1 适配</h2>
 * <ul>
 *   <li>26.3 的生成器 codec 是 {@code MapCodec}，1.20.1 用的是 {@link Codec}
 *       （{@code RecordCodecBuilder.create}，不是 {@code mapCodec}）；
 *       {@code apply} 的写法也相应从 {@code i.apply(i, i.stable(...))} 变成 {@code .apply(instance, ...)}。</li>
 *   <li><b>{@code refreshFeaturesPerStep} 在 1.20.1 不存在</b>：这是 26.3 用于「数据包重载后
 *       重建 features-per-step」的回调。1.20.1 的流程是每次 {@code /reload} / 重进世界
 *       由 codec 重新构造整个 ChunkGenerator（也就是重新构造本对象），
 *       {@link RealmOreBandGenerator} 作为本对象的成员跟着重建，缓存天然失效，
 *       所以这里不再需要任何重载回调。</li>
 *   <li>{@code applyBiomeDecoration} 与 {@code settings()} 的签名与 26.3 一致，原样保留。</li>
 * </ul>
 */
public class RealmFlatChunkGenerator extends FlatLevelSource {

    public static final Codec<RealmFlatChunkGenerator> CODEC =
            RecordCodecBuilder.<RealmFlatChunkGenerator>create(
                    instance -> instance.group(
                            FlatLevelGeneratorSettings.CODEC.fieldOf("settings")
                                    .forGetter(RealmFlatChunkGenerator::settings)
                    ).apply(instance, instance.stable(RealmFlatChunkGenerator::new))
            );

    private final RealmOreBandGenerator oreBands = new RealmOreBandGenerator();

    public RealmFlatChunkGenerator(FlatLevelGeneratorSettings settings) {
        super(settings);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        // 原版逻辑照跑（当前 realm 的群系 features 全空、lakes 关闭，所以这一步是空操作，
        // 但留着它以后想用数据包往 realm 群系挂地物时不用再改生成器）。
        super.applyBiomeDecoration(level, chunk, structureManager);
        this.oreBands.decorate(this, level, chunk);
    }
}
