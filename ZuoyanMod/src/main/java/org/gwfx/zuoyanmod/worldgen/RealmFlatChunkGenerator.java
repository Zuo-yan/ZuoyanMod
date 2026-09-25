package org.gwfx.zuoyanmod.worldgen;

import com.mojang.serialization.MapCodec;
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
 */
public class RealmFlatChunkGenerator extends FlatLevelSource {

    public static final MapCodec<RealmFlatChunkGenerator> CODEC =
            RecordCodecBuilder.<RealmFlatChunkGenerator>mapCodec(
                    i -> i.group(
                            FlatLevelGeneratorSettings.CODEC.fieldOf("settings")
                                    .forGetter(RealmFlatChunkGenerator::settings)
                    ).apply(i, i.stable(RealmFlatChunkGenerator::new))
            );

    private final RealmOreBandGenerator oreBands = new RealmOreBandGenerator();

    public RealmFlatChunkGenerator(FlatLevelGeneratorSettings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        // 原版逻辑照跑（当前 realm 的群系 features 全空、lakes 关闭，所以这一步是空操作，
        // 但留着它以后想用数据包往 realm 群系挂地物时不用再改生成器）。
        super.applyBiomeDecoration(level, chunk, structureManager);
        this.oreBands.decorate(this, level, chunk);
    }

    @Override
    public void refreshFeaturesPerStep() {
        super.refreshFeaturesPerStep();
        // 数据包重载：层配置和群系注册表都可能变了，矿物带缓存必须一起失效
        this.oreBands.invalidate();
    }
}
