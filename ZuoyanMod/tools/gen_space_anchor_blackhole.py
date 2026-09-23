#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
空间锚点物品贴图生成器 —— 黑洞 + 暗物质主题（16x16，透明背景）
纯标准库实现（手写 PNG 编码），无需 Pillow。改配色/构图只动顶部常量与 pixel() 函数。
用法：python gen_space_anchor_blackhole.py [输出png路径]
同时输出 256x256 最近邻放大预览，便于人工检查。
"""
import math
import random
import struct
import sys
import zlib

SIZE = 16
SEED = 20260922
OUT = sys.argv[1] if len(sys.argv) > 1 else \
    r"A:\Code\Minecraft\ZuoyanMod\ZuoyanMod\src\main\resources\assets\zuoyanmod\textures\item\space_anchor.png"
PREVIEW = OUT + ".preview.png"

# ===== 配色（与项目紫金系 + 暗物质呼应）=====
EVENT_HORIZON = (10, 10, 18)      # 事件视界：近纯黑带蓝
PHOTON_RING = (255, 243, 214)     # 光子环：白炽
DISK_INNER = (255, 217, 138)      # 吸积盘内缘：紫金亮金
DISK_MID = (232, 163, 61)         # 吸积盘中段：炽橙
DISK_OUTER = (139, 92, 246)       # 吸积盘外缘：紫
DISK_FADE = (76, 42, 133)         # 吸积盘外衰减：暗紫
BG_INNER = (26, 18, 48)           # 暗物质雾内侧
BG_OUTER = (13, 10, 26)           # 暗物质雾外侧
GRAIN_A = (61, 46, 110)           # 暗物质颗粒（亮）
GRAIN_B = (90, 74, 156)           # 暗物质颗粒（更亮，少量）

# ===== 构图常量 =====
TILT = math.radians(-24)      # 吸积盘倾角（左高右低）
R_HORIZON = 2.6               # 事件视界半径
E_DISC_IN, E_DISC_OUT = 0.82, 1.12   # 吸积盘椭圆半径范围（归一化）
DISC_RX, DISC_RY = 6.0, 2.5   # 吸积盘椭圆半轴
R_BODY = 7.4                  # 主体圆形外径（超出部分透明）
GRAIN_RATE = 0.07             # 暗物质颗粒密度
DOPPLER = 0.35                # 多普勒增亮幅度


def lerp(a, b, t):
    return a + (b - a) * t


def mix(c1, c2, t):
    return tuple(lerp(a, b, t) for a, b in zip(c1, c2))


def clamp8(v):
    return max(0, min(255, int(v)))


def disc_color(e, u):
    """吸积盘：内缘金 -> 炽橙 -> 外缘紫；左侧多普勒增亮（旋转方向朝左）。"""
    t = (e - E_DISC_IN) / (E_DISC_OUT - E_DISC_IN)
    if t < 0.3:
        c = mix(DISK_INNER, DISK_MID, t / 0.3)
    elif t < 0.7:
        c = mix(DISK_MID, DISK_OUTER, (t - 0.3) / 0.4)
    else:
        c = mix(DISK_OUTER, DISK_FADE, (t - 0.7) / 0.3)
    doppler = 1.0 + DOPPLER * max(-1.0, min(1.0, -u / DISC_RX))
    return tuple(clamp8(ch * doppler) for ch in c)


def pixel(x, y, rng):
    dx, dy = x - 7.5, y - 7.5
    u = dx * math.cos(TILT) - dy * math.sin(TILT)
    v = dx * math.sin(TILT) + dy * math.cos(TILT)
    r = math.hypot(dx, dy)
    e = math.hypot(u / DISC_RX, v / DISC_RY)

    if r > R_BODY:
        return (0, 0, 0, 0)                      # 四角透明
    in_disc = E_DISC_IN <= e <= E_DISC_OUT
    if in_disc:
        if v <= 0 and r < R_HORIZON * 0.9:
            return (*EVENT_HORIZON, 255)         # 盘的远端被视界吞掉
        return (*disc_color(e, u), 255)          # 近端从视界前方穿过
    if r < R_HORIZON:
        return (*EVENT_HORIZON, 255)             # 事件视界
    if R_HORIZON <= r < 3.2 and e < E_DISC_IN:
        return (*PHOTON_RING, 255)               # 光子环（盘内缺口处可见）

    # 背景：暗物质雾 + 颗粒流动感
    t = min(1.0, r / R_BODY)
    c = mix(BG_INNER, BG_OUTER, t)
    if r > 3.4 and rng.random() < GRAIN_RATE:
        c = GRAIN_A if rng.random() < 0.65 else GRAIN_B
    # 外缘 alpha 渐变，避免硬边
    a = 255 if r < R_BODY - 1.2 else 255 * (R_BODY - r) / 1.2
    return (*[clamp8(ch) for ch in c], clamp8(a))


def render():
    rng = random.Random(SEED)
    return [[pixel(x, y, rng) for x in range(SIZE)] for y in range(SIZE)]


def write_png(path, pixels):
    h = len(pixels)
    w = len(pixels[0])
    raw = b"".join(
        b"\x00" + b"".join(struct.pack("4B", *p) for p in row)
        for row in pixels
    )

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(chunk(b"IHDR", ihdr))
        f.write(chunk(b"IDAT", zlib.compress(raw, 9)))
        f.write(chunk(b"IEND", b""))


def write_preview(path, pixels, scale=16):
    """最近邻放大，供人工检查。"""
    big = []
    for row in pixels:
        scaled_row = []
        for p in row:
            scaled_row.extend([p] * scale)
        big.extend([scaled_row] * scale)
    write_png(path, big)


if __name__ == "__main__":
    px = render()
    write_png(OUT, px)
    write_preview(PREVIEW, px)
    print("written:", OUT)
    print("preview:", PREVIEW)
