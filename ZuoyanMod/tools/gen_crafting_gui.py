#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""「四维空间 · 工作台」界面底图（176×234）。纯标准库手写 PNG。

布局常量必须与 menu/KleinBottleCraftingMenu.java 与 client/KleinBottleCraftingScreen.java 一致：
  合成网格 (30,22) 3×3 · 产物槽 (124,44) · 翻页按钮 (124/150, 22) 24×14
  存储快取条 (8,84) 9×3 · 玩家背包 (8,150) 3行 + 快捷栏 (8,208)
槽框画在 (slotX-1, slotY-1) 起 18×18 —— 26.3 的 extractSlots 只画物品图标，不画底框。
"""
import os
import struct
import zlib

W, H = 176, 234

BG = (28, 22, 40, 255)
PANEL = (44, 34, 62, 255)
PANEL_HI = (66, 52, 92, 255)
BORDER = (108, 88, 152, 255)
SLOT_BG = (18, 14, 28, 255)
SLOT_EDGE = (74, 60, 104, 255)
CRAFT_BG = (52, 40, 74, 255)
ARROW = (150, 126, 196, 255)
ARROW_DIM = (92, 74, 128, 255)

grid = [[(0, 0, 0, 0) for _ in range(W)] for _ in range(H)]


def put(x, y, c):
    if 0 <= x < W and 0 <= y < H:
        grid[y][x] = c


def rect(x, y, w, h, c):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            put(xx, yy, c)


def panel(x, y, w, h, fill, hi=True):
    rect(x, y, w, h, fill)
    for xx in range(x, x + w):
        put(xx, y, BORDER)
        put(xx, y + h - 1, BORDER)
    for yy in range(y, y + h):
        put(x, yy, BORDER)
        put(x + w - 1, yy, BORDER)
    if hi:
        for xx in range(x + 1, x + w - 1):
            put(xx, y + 1, PANEL_HI)


def slot(x, y):
    """18x18 槽位框"""
    rect(x, y, 18, 18, SLOT_EDGE)
    rect(x + 1, y + 1, 16, 16, SLOT_BG)


def arrow(x, y, c):
    """合成箭头：指向右的实心三角 + 一横，16×10"""
    rows = [
        "....#...........",
        "....##..........",
        "....###.........",
        "....####........",
        "#########.......",
        "#########.......",
        "....####........",
        "....###.........",
        "....##..........",
        "....#...........",
    ]
    bar = [
        "....#####......." for _ in range(2)
    ]
    for r, line in enumerate(rows + bar):
        for cidx, ch in enumerate(line):
            if ch == '#':
                put(x + cidx, y + r, c)


def build():
    rect(0, 0, W, H, BG)
    for xx in range(W):
        put(xx, 0, BORDER)
        put(xx, H - 1, BORDER)
    for yy in range(H):
        put(0, yy, BORDER)
        put(W - 1, yy, BORDER)

    # 顶部标题条
    rect(1, 1, W - 2, 17, PANEL)
    for xx in range(2, W - 2):
        put(xx, 17, BORDER)

    # 合成区底板：网格 (30,22)~(84,76)，产物 (124,44)
    panel(22, 20, 68, 58, CRAFT_BG, hi=False)
    panel(116, 42, 30, 22, CRAFT_BG, hi=False)
    arrow(96, 44, ARROW)

    # 翻页按钮（原版按钮控件自己会画，这里只铺底板做对齐参考）
    panel(124, 22, 24, 14, PANEL)
    panel(150, 22, 24, 14, PANEL)

    # 合成网格 3×3
    for row in range(3):
        for col in range(3):
            slot(30 + col * 18 - 1, 22 + row * 18 - 1)
    # 产物槽
    slot(124 - 1, 44 - 1)

    # 存储快取条 9×3
    for row in range(3):
        for col in range(9):
            slot(8 + col * 18 - 1, 84 + row * 18 - 1)

    # 分隔线
    for xx in range(2, W - 2):
        put(xx, 144, BORDER)
        put(xx, 145, BG)

    # 玩家背包 3 行 + 快捷栏
    for row in range(3):
        for col in range(9):
            slot(8 + col * 18 - 1, 150 + row * 18 - 1)
    for col in range(9):
        slot(8 + col * 18 - 1, 208 - 1)


def write_png(path):
    raw = bytearray()
    for y in range(H):
        raw.append(0)
        for x in range(W):
            raw.extend(grid[y][x])

    def chunk(tag, data):
        c = struct.pack('>I', len(data)) + tag + data
        return c + struct.pack('>I', zlib.crc32(tag + data) & 0xFFFFFFFF)

    ihdr = struct.pack('>IIBBBBB', W, H, 8, 6, 0, 0, 0)
    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) \
        + chunk(b'IDAT', zlib.compress(bytes(raw), 9)) + chunk(b'IEND', b'')
    with open(path, 'wb') as f:
        f.write(png)


if __name__ == '__main__':
    build()
    out = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                       'src', 'main', 'resources', 'assets', 'zuoyanmod', 'textures', 'gui',
                       'four_dimensional_crafting.png')
    write_png(out)
    print('written', out, W, 'x', H)
