#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""克莱因瓶第三版：分页滚动的无限终端。
数据模型从"每格 100 万"改回"正常堆叠 + 无限条目 + 翻页"，熔炉改成"放入即烧"的开关。"""
import json
import os
import shutil

ROOT = r"A:/Code/Minecraft/ZuoyanMod/ZuoyanMod/src/main"
JAVA = os.path.join(ROOT, "java", "org", "gwfx", "zuoyanmod")
RES = os.path.join(ROOT, "resources", "assets", "zuoyanmod")
F = {}

# ---------------------------------------------------------------- 四维空间（数据模型）
F[r"item/FourDimensionalSpace.java"] = '''package org.gwfx.zuoyanmod.item;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.menu.KleinBottleMenu;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 「四维空间」：属于**每个玩家**的无限存储终端。
 *
 * <p>第三版的数据模型改回主流终端模组的做法——**每个条目就是一个正常堆叠的 ItemStack**
 * （最多 64/16/1），不再做"一格塞 100 万"（26.x 的 ItemStack 根本装不下那么大的数量，
 * 第一版为此引入了一整套数量分离逻辑，v2 又因为 SimpleContainer 的回调约定炸栈）。
 * "无限"来自**条目数量不设上限 + 界面翻页滚动**，而不是来自单格容量。
 *
 * <p>数据挂在玩家的 attachment 上（跟着人走，不跟物品走），死亡不掉。
 */
public class FourDimensionalSpace {

    public static final int PAGE_SLOTS = 54;

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Zuoyanmod.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<FourDimensionalSpace>> ATTACHMENT =
            ATTACHMENTS.register("four_dimensional_space", () -> AttachmentType
                    .builder(FourDimensionalSpace::new)
                    .serialize(new IAttachmentSerializer<FourDimensionalSpace>() {
                        @Override
                        public FourDimensionalSpace read(IAttachmentHolder holder, ValueInput input) {
                            FourDimensionalSpace space = new FourDimensionalSpace();
                            space.deserialize(input);
                            return space;
                        }

                        @Override
                        public boolean write(FourDimensionalSpace attachment, ValueOutput output) {
                            attachment.serialize(output);
                            return true;
                        }
                    })
                    .copyOnDeath()
                    .build());

    public static FourDimensionalSpace of(Player player) {
        return player.getData(ATTACHMENT);
    }

    // ===== 数据 =====

    /** 条目列表：每个元素都是一个正常堆叠的 ItemStack，条目数不设上限 */
    private final List<ItemStack> items = new ArrayList<>();
    /** 自动熔炼开关：开着的时候，放进来能烧的东西会直接变成产物 */
    private boolean autoSmelt;

    public int size() {
        return items.size();
    }

    public ItemStack get(int index) {
        return index >= 0 && index < items.size() ? items.get(index) : ItemStack.EMPTY;
    }

    public int pageCount() {
        return Math.max(1, (items.size() + PAGE_SLOTS - 1) / PAGE_SLOTS);
    }

    public int entryCount() {
        return items.size();
    }

    public long totalItems() {
        long n = 0;
        for (ItemStack s : items) {
            n += s.getCount();
        }
        return n;
    }

    /**
     * 放入：先并进已有的同类条目（到该物品堆叠上限为止），剩下的开新条目。
     * @return 实际放入数量
     */
    public int insert(ItemStack stack) {
        ItemStack in = stack.copy();
        int remaining = in.getCount();
        int max = Math.max(1, in.getMaxStackSize());
        for (ItemStack cur : items) {
            if (remaining <= 0) {
                break;
            }
            if (ItemStack.isSameItemSameComponents(cur, in) && cur.getCount() < max) {
                int moved = Math.min(max - cur.getCount(), remaining);
                cur.setCount(cur.getCount() + moved);
                remaining -= moved;
            }
        }
        while (remaining > 0) {
            int moved = Math.min(max, remaining);
            items.add(in.copyWithCount(moved));
            remaining -= moved;
        }
        return stack.getCount() - remaining;
    }

    /** 覆写某个条目（界面槽位写回用）；空栈 = 删除该条目 */
    public void set(int index, ItemStack stack) {
        if (index < 0) {
            return;
        }
        if (stack.isEmpty()) {
            if (index < items.size()) {
                items.remove(index);
            }
            return;
        }
        while (items.size() <= index) {
            items.add(ItemStack.EMPTY);
        }
        items.set(index, stack.copy());
    }

    /** 一键整理：同种合并、去掉空档 */
    public void compact() {
        List<ItemStack> pool = new ArrayList<>();
        for (ItemStack s : items) {
            if (!s.isEmpty()) {
                pool.add(s.copy());
            }
        }
        items.clear();
        for (ItemStack s : pool) {
            insert(s);
        }
    }

    // ===== 自动熔炼 =====

    public boolean isAutoSmelt() {
        return autoSmelt;
    }

    public void setAutoSmelt(boolean value) {
        autoSmelt = value;
        // 不在这里立刻转换：附件拿不到 ServerLevel，转换由克莱因瓶的 inventoryTick 触发（下一 tick 生效）
    }

    /**
     * 自动熔炼：把列表里所有可烧条目就地烧成产物。
     * 只能由持有 ServerLevel 的调用方触发（克莱因瓶的 inventoryTick / 熔炉按钮）——
     * 附件本身拿不到 Level，这也是为什么烧制不能写进 insert()。
     */
    public void processAutoSmelt(ServerLevel level) {
        if (!autoSmelt) {
            return;
        }
        for (int i = items.size() - 1; i >= 0; i--) {
            ItemStack input = items.get(i);
            if (input.isEmpty()) {
                continue;
            }
            ItemStack result = smeltResult(level, input);
            if (result.isEmpty() || ItemStack.isSameItemSameComponents(result, input)) {
                continue; // 没有配方，或产物就是原料本身（避免死循环）
            }
            int count = input.getCount();
            items.remove(i);
            ItemStack produced = result.copy();
            produced.setCount(Math.max(1, result.getCount() * count));
            insert(produced);
        }
    }

    // ===== 打开界面 =====

    public void openTerminal(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new KleinBottleMenu(id, inventory, p),
                Component.literal("四维空间")));
    }

    public static void openCrafting(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new CraftingMenu(id, inventory, ContainerLevelAccess.NULL),
                Component.literal("四维空间 · 工作台")));
    }

    // ===== 持久化 =====

    public void serialize(ValueOutput output) {
        output.store("Items", ItemStack.CODEC.listOf(), items);
        output.putBoolean("AutoSmelt", autoSmelt);
    }

    public void deserialize(ValueInput input) {
        items.clear();
        for (ItemStack s : input.read("Items", ItemStack.CODEC.listOf()).orElse(List.of())) {
            if (!s.isEmpty()) {
                items.add(s);
            }
        }
        autoSmelt = input.getBooleanOr("AutoSmelt", false);
    }
}
'''

for rel, content in F.items():
    path = os.path.join(JAVA, rel.replace("/", os.sep))
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    print("wrote", rel)
