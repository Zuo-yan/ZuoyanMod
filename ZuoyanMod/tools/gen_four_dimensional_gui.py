#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""「四维空间」终端界面底图（176×250）。纯标准库手写 PNG。
布局常量必须与 client/KleinBottleScreen.java 保持一致：
  网格 (8,44) 9×6 · 玩家背包 (8,168) 3行 + 快捷栏 (8,226) · 按钮行 y=24 h=16 · 分隔线 y=155
"""
import os
import struct
import zlib

W, H = 176, 250

BG = (28, 22, 40, 255)
PANEL = (44, 34, 62, 255)
PANEL_HI = (66, 52, 92, 255)
BORDER = (108, 88, 152, 255)
SLOT_BG = (18, 14, 28, 255)
SLOT_EDGE = (74, 60, 104, 255)
BTN = (58, 44, 84, 255)
BTN_HI = (96, 76, 138, 255)
BTN_LO = (34, 26, 48, 255)
SEP = (78, 62, 112, 255)

grid = [[(0, 0, 0, 0) for _ in range(W)] for _ in range(H)]


def put(x, y, c):
    if 0 <= x < W and 0 <= y < H:
        grid[y][x] = c


def rect(x, y, w, h, c):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            put(xx, yy, c)


def panel(x, y, w, h, fill, hi_top=True):
    """带一像素描边 + 顶部高光的凸起面板"""
    rect(x, y, w, h, fill)
    for xx in range(x, x + w):
        put(xx, y, BORDER)
        put(xx, y + h - 1, BORDER)
    for yy in range(y, y + h):
        put(x, yy, BORDER)
        put(x + w - 1, yy, BORDER)
    if hi_top:
        for xx in range(x + 1, x + w - 1):
            put(xx, y + 1, hi_top and PANEL_HI)


def slot(x, y):
    """18x18 的槽位框：原版不画槽位底框，必须烤进贴图"""
    rect(x, y, 18, 18, SLOT_EDGE)
    rect(x + 1, y + 1, 16, 16, SLOT_BG)


def build():
    # 主面板
    rect(0, 0, W, H, BG)
    for xx in range(W):
        put(xx, 0, BORDER)
        put(xx, H - 1, BORDER)
    for yy in range(H):
        put(0, yy, BORDER)
        put(W - 1, yy, BORDER)

    # 顶部标题条
    rect(1, 1, W - 2, 21, PANEL)
    for xx in range(2, W - 2):
        put(xx, 21, BORDER)

    # 按钮行（三个 52x16，位置与 Screen 的 BUTTON_X 对齐）
    for bx in (8, 62, 116):
        panel(bx, 24, 52, 16, BTN)

    # 存储网格 9×6
    for row in range(6):
        for col in range(9):
            slot(8 + col * 18, 44 + row * 18)

    # 分隔线
    for xx in range(8, W - 8):
        put(xx, 155, SEP)

    # 玩家背包 3 行 + 快捷栏
    for row in range(3):
        for col in range(9):
            slot(8 + col * 18, 168 + row * 18)
    for col in range(9):
        slot(8 + col * 18, 226)


def write_png(path, pixels):
    height, width = len(pixels), len(pixels[0])
    raw = b"".join(b"\x00" + b"".join(struct.pack("4B", *px) for px in row) for row in pixels)

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n"
                + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw, 9))
                + chunk(b"IEND", b""))


def preview(pixels, scale=2):
    out = []
    for y in range(len(pixels) * scale):
        row = []
        for x in range(len(pixels[0]) * scale):
            px = pixels[y // scale][x // scale]
            if px[3] == 0:
                s = 90 if ((x // 7) + (y // 7)) % 2 == 0 else 60
                row.append((s, s, s, 255))
            else:
                row.append(px)
        out.append(row)
    return out


if __name__ == "__main__":
    build()
    here = os.path.dirname(os.path.abspath(__file__))
    mod_root = os.path.dirname(here)
    workspace = os.path.dirname(mod_root)
    target = os.path.join(mod_root, "src", "main", "resources", "assets", "zuoyanmod",
                          "textures", "gui", "four_dimensional_space.png")
    os.makedirs(os.path.dirname(target), exist_ok=True)
    write_png(target, grid)
    print("wrote", target)
    pv = os.path.join(workspace, "four_dimensional_space_preview.png")
    write_png(pv, preview(grid))
    print("wrote", pv)
