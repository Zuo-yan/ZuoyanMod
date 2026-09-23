package org.gwfx.zuoyanmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.gwfx.zuoyanmod.menu.KleinBottleMenu;

import java.util.function.Supplier;

/**
 * JEI 的 + 按钮 → 服务端：按配方 id 从四维空间抓材料填终端的**内嵌 3×3 网格**。
 *
 * <p>只发配方 id 而不是"要放哪 9 个东西"，是因为 JEI 的合成分类会把 2×2 配方紧凑显示，
 * 客户端拼出来的网格顺序不可靠；交给服务端按 {@code ShapedRecipe} 的宽高落位才稳。
 */
public class CraftingTransferPacket {

    public final ResourceLocation recipeId;
    public final boolean maxTransfer;

    public CraftingTransferPacket(ResourceLocation recipeId, boolean maxTransfer) {
        this.recipeId = recipeId;
        this.maxTransfer = maxTransfer;
    }

    public static void encode(CraftingTransferPacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.recipeId);
        buf.writeBoolean(msg.maxTransfer);
    }

    public static CraftingTransferPacket decode(FriendlyByteBuf buf) {
        return new CraftingTransferPacket(buf.readResourceLocation(), buf.readBoolean());
    }

    public static void handle(CraftingTransferPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender(); // 1.20.1 的 getSender() 已经直接返回 ServerPlayer
            if (player != null) {
                KleinBottleMenu.transfer(player, msg.recipeId, msg.maxTransfer);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
