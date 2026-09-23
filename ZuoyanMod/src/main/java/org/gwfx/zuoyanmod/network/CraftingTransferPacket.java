package org.gwfx.zuoyanmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.menu.KleinBottleMenu;

/**
 * JEI 的 + 按钮 → 服务端：按配方 id 从四维空间抓材料填终端的**内嵌 3×3 网格**。
 *
 * <p>只发配方 id 而不是"要放哪 9 个东西"，是因为 JEI 的合成分类会把 2×2 配方紧凑显示，
 * 客户端拼出来的网格顺序不可靠；交给服务端按 {@code ShapedRecipe} 的宽高落位才稳。
 */
public record CraftingTransferPacket(Identifier recipeId, boolean maxTransfer) implements CustomPacketPayload {

    public static final Type<CraftingTransferPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "crafting_transfer"));

    public static final StreamCodec<ByteBuf, CraftingTransferPacket> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, CraftingTransferPacket::recipeId,
            ByteBufCodecs.BOOL, CraftingTransferPacket::maxTransfer,
            CraftingTransferPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CraftingTransferPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                KleinBottleMenu.transfer(player, packet.recipeId(), packet.maxTransfer());
            }
        });
    }
}
