#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""克莱因瓶物品图标（16x16）。纯标准库手写 PNG。

设计说明（16x16 别指望"画得像"，要的是剪影可读 + 含义可读）：
  · 剪影：瓶口在右上 → 瓶颈向左下弯 → 从瓶身左上方插进瓶身（这就是克莱因瓶的自交），
    瓶身是一枚蛋形。整体读作"一个正在把自己吞进去的瓶子"。
  · 内部暗腔 'a' 就是瓶颈插进去的入口，青色 'g'/'h' 描出自交处的边缘 ——
    这是"四维"这个设定在物品图标上唯一的落点，所以必须细、必须亮。
  · 明度分 5 阶（b/c/d/e/f），光从左上来；不做自动描边（1px 的斜臂四邻全透明会被吃掉）。
"""
import os
import struct
import zlib

W = H = 16

# ===== 5 阶明度 + 强调色 =====
LEGEND = {
    '.': (0, 0, 0, 0),
    'a': (18, 11, 30, 255),      # 内腔（最暗）
    'b': (54, 37, 92, 255),      # 暗面 / 外轮廓
    'c': (88, 62, 148, 255),     # 中间调
    'd': (130, 98, 208, 255),    # 亮面
    'e': (180, 150, 240, 255),   # 高光
    'f': (228, 216, 255, 255),   # 最亮（瓶口受光）
    'g': (112, 228, 214, 255),   # 青色强调：自交边缘 / 右缘反光
    'h': (56, 126, 122, 255),    # 暗青：强调色的收边
    'w': (250, 249, 255, 255),   # 腔内星点
}

# 瓶颈从右上下来、瓶口在瓶身内部张开（克莱因瓶的自交）；瓶身占面积大头，蛋形收口。
# 'a' 是"从口里看进去"的空腔，'g'/'h' 描出自交处的青色边缘。
ROWS = [
    "................",
    "........effe....",
    ".......deffed...",
    "........caad....",
    "........baad....",
    "........baad....",
    "........bedd....",
    ".......bcdedc...",
    "....bcdeaacb....",
    "...bcedgaacdb...",
    "..bcefchaaacdb..",
    "..bdefdgaaacdb..",
    "...bcdeghcccb...",
    "...bcdedccdcb...",
    "....bcdeedcb....",
    ".....bbccbb.....",
]


def build():
    for i, row in enumerate(ROWS):
        if len(row) != W:
            raise SystemExit(f"第 {i} 行长度是 {len(row)}，不是 {W}：{row!r}")
    return [[LEGEND[ch] for ch in row] for row in ROWS]


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


def preview(pixels, scale=16):
    out = []
    for y in range(len(pixels) * scale):
        row = []
        for x in range(len(pixels[0]) * scale):
            px = pixels[y // scale][x // scale]
            if px[3] == 0:
                s = 92 if ((x // 12) + (y // 12)) % 2 == 0 else 62
                row.append((s, s, s, 255))
            else:
                row.append(px)
        out.append(row)
    return out


if __name__ == "__main__":
    grid = build()
    here = os.path.dirname(os.path.abspath(__file__))
    mod_root = os.path.dirname(here)
    workspace = os.path.dirname(mod_root)
    target = os.path.join(mod_root, "src", "main", "resources", "assets", "zuoyanmod",
                          "textures", "item", "klein_bottle.png")
    os.makedirs(os.path.dirname(target), exist_ok=True)
    write_png(target, grid)
    print("wrote", target)
    pv = os.path.join(workspace, "klein_bottle_preview.png")
    write_png(pv, preview(grid))
    print("wrote", pv)
