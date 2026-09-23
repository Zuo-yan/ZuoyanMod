package org.gwfx.zuoyanmod.item;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.gwfx.zuoyanmod.Zuoyanmod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 「四维空间」的 Forge Capability 载体（1.20.1 分支）。
 *
 * <p>26.3 主线用的是 NeoForge 玩家 Attachment（{@code FourDimensionalSpace.ATTACHMENTS}
 * + copyOnDeath）；Forge 1.20.1 没有那套，对应物是 Capability：
 * <ul>
 *   <li>{@link RegisterCapabilitiesEvent}：注册能力类型；</li>
 *   <li>{@link AttachCapabilitiesEvent}：给每个玩家挂上 provider（可序列化，随玩家存档）；</li>
 *   <li>{@link PlayerEvent.Clone}：死亡重生 / 从末地返回时把数据复制到新实体
 *       （对应 26.3 的 copyOnDeath，这里死亡和跨末地都保）。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class FourDimensionalSpaceCapability {

    public static final Capability<FourDimensionalSpace> FOUR_DIMENSIONAL_SPACE =
            CapabilityManager.get(new CapabilityToken<>() {});

    private static final ResourceLocation ID =
            new ResourceLocation(Zuoyanmod.MODID, "four_dimensional_space");

    private FourDimensionalSpaceCapability() {}

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(FourDimensionalSpace.class);
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ID, new Provider());
        }
    }

    /** 死亡重生和从末地返回都会走 Clone：把四维空间原样搬过去（对应 26.3 的 copyOnDeath） */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(FOUR_DIMENSIONAL_SPACE).ifPresent(old ->
                event.getEntity().getCapability(FOUR_DIMENSIONAL_SPACE).ifPresent(space ->
                        space.deserialize(old.serialize())));
        event.getOriginal().invalidateCaps();
    }

    private static final class Provider implements ICapabilitySerializable<CompoundTag> {

        private final FourDimensionalSpace space = new FourDimensionalSpace();
        private final LazyOptional<FourDimensionalSpace> optional = LazyOptional.of(() -> space);

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            if (cap == FOUR_DIMENSIONAL_SPACE) {
                return optional.cast();
            }
            return LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return space.serialize();
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            space.deserialize(tag);
            // 兼容旧存档：老版本把存储存成"每格一个普通 ItemStack"的 Items 列表
            space.migrateLegacyItems(tag);
        }
    }
}
