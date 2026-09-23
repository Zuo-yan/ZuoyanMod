#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
「真空衰变」锤贴图生成器（纯标准库手写 PNG，无 Pillow）。

为什么用**手写像素 ROWS** 而不是数学循环：见 docs/absolute_zero.md §贴图踩坑——
循环画出来的东西在 16x16 上经常退化成"胖团"或"虚线"，而逐行写死能精确控制剪影，
且改一个字符就能微调，不需要在脑子里模拟光栅化。

剪影：右上一块 7x5 的方锤头（暗紫罗兰金属 + 受光/背光边），头心 2x2 真空核心并向外裂开，
左下一条 2 像素阶梯斜柄。不做自动描边（细结构会被描边吃掉）。

输出：
  src/main/resources/assets/zuoyanmod/textures/item/vacuum_decay.png   (16x16)
  A:/Code/Minecraft/ZuoyanMod/vacuum_decay_preview.png                 (放大预览，放项目根)
"""
import os
import struct
import zlib

# ===== 调色板：5 阶金属 + 3 阶辉光 =====
PALETTE = {
    "1": (176, 166, 196, 255),    # 金属高光
    "2": (128, 118, 154, 255),    # 金属亮面
    "3": (86, 78, 110, 255),      # 金属中间调
    "4": (58, 52, 78, 255),       # 金属暗面
    "5": (34, 28, 48, 255),       # 金属最暗 / 硬边
    "g": (96, 58, 180, 255),      # 辉光外圈
    "G": (150, 102, 255, 255),    # 辉光主体
    "W": (235, 228, 255, 255),    # 辉光白热核心
}

# 每行 16 字符；"." = 透明
# 构图：右上一块 9x6 的方锤头，**以金属为主体**，只在头心开一道"十"字裂隙
#       （竖缝 1px 贯穿 + 横缝 1px 展开，交点白热）——上一版把辉光铺满整个头，
#       结果头看着像一滩紫水而不是锤子，这里刻意把辉光压回细线。
#       左下一条 2 像素阶梯斜柄，柄尾一个小外扩配重。光照统一来自左上。
ROWS = [
    "................",  # 0
    "......111111111.",  # 1  锤头顶面受光
    "......2333g3334.",  # 2  竖缝上段
    "......2gGWWg334.",  # 3  横缝（左长右短，故意不对称）+ 2px 白热交点
    "......2333G3334.",  # 4  竖缝下段
    "......2333g3334.",  # 5
    "......555555555.",  # 6  锤头底面硬边
    "..........23....",  # 7  握柄起手
    ".........23.....",  # 8
    "........23......",  # 9
    ".......23.......",  # 10
    "......23........",  # 11
    ".....23.........",  # 12
    "....23..........",  # 13
    "...23...........",  # 14
    "..444...........",  # 15 柄尾配重
]


def build():
    grid = []
    for line in ROWS:
        assert len(line) == 16, f"row length must be 16, got {len(line)}: {line!r}"
        grid.append([PALETTE.get(ch, (0, 0, 0, 0)) for ch in line])
    return grid


def write_png(path, pixels):
    """宽高一律从像素矩阵推断，绝不硬编码（硬编码会让预览图损坏）"""
    height = len(pixels)
    width = len(pixels[0])
    raw = b"".join(
        b"\x00" + b"".join(struct.pack("4B", *px) for px in row)
        for row in pixels
    )

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(raw, 9))
           + chunk(b"IEND", b""))
    with open(path, "wb") as f:
        f.write(png)


def preview(pixels, scale=14):
    height = len(pixels)
    width = len(pixels[0])
    out = []
    for y in range(height * scale):
        row = []
        for x in range(width * scale):
            px = pixels[y // scale][x // scale]
            if px[3] == 0:
                shade = 90 if ((x // 7) + (y // 7)) % 2 == 0 else 60   # 棋盘底，看清边缘
                row.append((shade, shade, shade, 255))
            else:
                row.append(px)
        out.append(row)
    return out


if __name__ == "__main__":
    grid = build()

    here = os.path.dirname(os.path.abspath(__file__))
    mod_root = os.path.dirname(here)                       # .../ZuoyanMod/ZuoyanMod
    workspace = os.path.dirname(mod_root)                  # .../ZuoyanMod

    target = os.path.join(mod_root, "src", "main", "resources", "assets",
                          "zuoyanmod", "textures", "item", "vacuum_decay.png")
    os.makedirs(os.path.dirname(target), exist_ok=True)
    write_png(target, grid)
    print("wrote", target)

    preview_path = os.path.join(workspace, "vacuum_decay_preview.png")
    write_png(preview_path, preview(grid))
    print("wrote", preview_path)
