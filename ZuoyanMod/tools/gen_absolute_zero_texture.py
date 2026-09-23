#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
绝对零度 物品贴图（16x16 RGBA，纯标准库手写 PNG）
设计：八臂冰晶（垂直/水平各 2px 主干 + 四条 2px 阶梯斜臂）+ 白热核心，冷蓝白配色。
两条踩过的坑，别再犯：
  1. 1px 斜臂在 16x16 上呈"虚线感"，不成形 → 斜臂必须用 2 像素阶梯（像素画里画粗斜线的标准手法）。
  2. 六臂雪花放不下描边：1px 斜臂四邻全是透明，描边会把整条臂吃掉 → 靠明暗对比立形状，不用描边。
用法: python gen_absolute_zero_texture.py [assets/zuoyanmod 目录] [预览图目录]
"""
import math
import os
import struct
import sys
import zlib

SIZE = 16
CX, CY = 7.5, 7.5

DEEP = (30, 58, 112)
MID = (74, 140, 214)
ICE = (168, 222, 255)
WHITE = (240, 252, 255)

# 手写像素画：A = 冰晶实体，. = 透明
ROWS = [
    "................",
    ".......AA.......",
    ".AA....AA....AA.",
    "..AA...AA...AA..",
    "...AA..AA..AA...",
    "....AA.AA.AA....",
    ".....AAAAAA.....",
    ".AAAAAAAAAAAAAA.",
    ".AAAAAAAAAAAAAA.",
    ".....AAAAAA.....",
    "....AA.AA.AA....",
    "...AA..AA..AA...",
    "..AA...AA...AA..",
    ".AA....AA....AA.",
    ".......AA.......",
    "................",
]

# 核心 2x2，用来画白热中心
CORE = {(7, 7), (8, 7), (7, 8), (8, 8)}


def clamp8(v):
    return max(0, min(255, int(v)))


def mix(c1, c2, t):
    t = max(0.0, min(1.0, t))
    return tuple(clamp8(a + (b - a) * t) for a, b in zip(c1, c2))


def render():
    grid = [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]
    for y, row in enumerate(ROWS):
        for x, ch in enumerate(row):
            if ch != "A":
                continue
            if (x, y) in CORE:
                color = WHITE
            else:
                # 由内向外：冰亮 → 中蓝 → 深蓝，中心像在发光
                r = math.hypot(x - CX, y - CY)
                if r <= 2.0:
                    color = ICE
                elif r <= 3.5:
                    color = mix(ICE, MID, 0.28)
                elif r <= 5.0:
                    color = mix(ICE, MID, 0.55)
                else:
                    color = mix(MID, DEEP, 0.4)
            grid[y][x] = (*color, 255)
    return grid


def write_png(path, pixels):
    h = len(pixels)
    w = len(pixels[0])
    raw = b"".join(b"\x00" + b"".join(struct.pack("4B", *p) for p in row) for row in pixels)

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(chunk(b"IHDR", ihdr))
        f.write(chunk(b"IDAT", zlib.compress(raw, 9)))
        f.write(chunk(b"IEND", b""))


def scale(pixels, k):
    out = []
    for row in pixels:
        big = [p for p in row for _ in range(k)]
        for _ in range(k):
            out.append(list(big))
    return out


if __name__ == "__main__":
    root = sys.argv[1] if len(sys.argv) > 1 else \
        r"A:\Code\Minecraft\ZuoyanMod\ZuoyanMod\src\main\resources\assets\zuoyanmod"
    preview_dir = sys.argv[2] if len(sys.argv) > 2 else r"A:\Code\Minecraft\ZuoyanMod"

    target = os.path.join(root, "textures", "item", "absolute_zero.png")
    os.makedirs(os.path.dirname(target), exist_ok=True)
    write_png(target, render())
    print("written:", target)

    if preview_dir:
        os.makedirs(preview_dir, exist_ok=True)
        out = os.path.join(preview_dir, "absolute_zero_preview.png")
        write_png(out, scale(render(), 8))
        print("written:", out)
