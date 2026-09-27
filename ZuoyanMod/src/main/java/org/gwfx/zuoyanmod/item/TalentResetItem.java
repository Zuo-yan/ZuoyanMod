package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.gwfx.zuoyanmod.upgrade.UpgradeManager;

/**
 * 「天赋重置卷轴」——右键清空已选定的终极天赋，让玩家可以重新选一个。
 *
 * <p>只动终极天赋：基础能力的加点原样保留（那些走界面右键退还）。
 * 重置成功才扣卷轴；本来就没选天赋时提示后原样奉还。
 * 冷却与「分子离解·灌能」状态随天赋一起归零——旧天赋的尾巴不带到新天赋上。
 */
public class TalentResetItem extends DescribedItem {

    public TalentResetItem(Properties properties, String... descKeys) {
        super(properties, descKeys);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // 客户端只做挥手预测，真正的重置由服务端完成
        if (level.isClientSide()) {
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }
        if (!(level instanceof ServerLevel)) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        boolean reset = UpgradeManager.resetTalent((net.minecraft.server.level.ServerPlayer) player);
        if (!reset) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        // consume 自带创造模式豁免（内部判 hasInfiniteMaterials）
        player.getItemInHand(hand).consume(1, player);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
}
