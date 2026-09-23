#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
虚空共振泵美术资源生成（纯标准库 zlib/struct 手写 PNG，无需 Pillow）
  - 方块四面对贴图：front（观察窗）/ top（进气格栅）/ side（散热鳍片）/ bottom（向下喷口）
  - GUI 贴图 176x154：极简单进单出布局（顶栏标题+状态、输入槽、进度条、输出槽、共振储备条）
  - 暗物质粒子物品贴图（沿用原设计）
用法: python gen_void_pump_textures.py [assets/zuoyanmod 目录] [预览图输出目录]
"""
import math
import os
import struct
import sys
import zlib

# ===================== 通用调色板（暗紫金属 + 紫青共振光） =====================
CASE_DARK = (22, 16, 34)
CASE_MID = (44, 33, 64)
CASE_LIGHT = (66, 50, 92)
RIVET = (104, 78, 142)
INSET = (12, 9, 20)
GLOW_PURPLE = (139, 92, 246)
GLOW_CYAN = (63, 217, 200)
GLOW_WHITE = (214, 246, 255)
CORE = (8, 8, 14)

PARTICLE_CORE = (26, 18, 48)
PARTICLE_MID = (90, 74, 156)
PARTICLE_GLOW = (139, 92, 246)

BOLTS = {(1, 1), (14, 1), (1, 14), (14, 14)}

# ===================== GUI 调色板与布局常量（须与 Java 侧一致） =====================
GUI_W, GUI_H = 176, 146
DIVIDER_Y = 58
G_TEXTAREA = (36, 28, 54)
G_BG_MID = (31, 24, 47)
G_PLAYER = (28, 22, 42)
G_BG_BOT = (24, 18, 36)
G_BORDER = (16, 11, 26)
G_HILIGHT = (72, 57, 104)
G_SLOT_EDGE = (56, 43, 84)
G_SLOT_IN = (15, 11, 24)
G_TRACK_EDGE = (56, 43, 84)
G_TRACK_IN = (10, 7, 17)
G_TICK = (68, 53, 100)

# 以下坐标必须与 VoidResonancePumpMenu / VoidResonancePumpScreen 保持一致
SLOT_IN = (57, 24)
SLOT_OUT = (104, 24)
PROG_INNER = (77, 28, 22, 9)   # 两槽之间的产出进度条
RES_INNER = (56, 46, 65, 8)    # 两槽下方的共振储备条（带 4 条等级刻度）
PLAYER_ROWS = (70, 88, 106)
HOTBAR_Y = 128


# ===================== 基础工具 =====================
def clamp8(v):
    return max(0, min(255, int(v)))


def mix(c1, c2, t):
    return tuple(clamp8(a + (b - a) * t) for a, b in zip(c1, c2))


def new_img(w, h, color=(0, 0, 0, 0)):
    return [[color for _ in range(w)] for _ in range(h)]


def put(img, x, y, c):
    if 0 <= y < len(img) and 0 <= x < len(img[0]):
        img[y][x] = c


def rect(img, x, y, w, h, c):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            put(img, xx, yy, c)


def hline(img, x, y, w, c):
    rect(img, x, y, w, 1, c)


def vline(img, x, y, h, c):
    rect(img, x, y, 1, h, c)


def frame(img, x, y, w, h, c):
    hline(img, x, y, w, c)
    hline(img, x, y + h - 1, w, c)
    vline(img, x, y, h, c)
    vline(img, x + w - 1, y, h, c)


def write_png(path, pixels):
    """宽高从像素矩阵推断（勿硬编码）。"""
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


# ===================== 方块：共同的金属壳底 =====================
def dither(x, y):
    """确定性微噪声，避免大色块死平"""
    return (((x * 7 + y * 13 + (x * y) % 5) % 7) - 3) * 0.022


def plate(x, y):
    """金属壳：上/左受光，下/右背光，MC 方块惯例"""
    c = mix(CASE_DARK, CASE_MID, 0.58)
    t = dither(x, y)
    if y == 0:
        t += 0.42
    elif y == 1:
        t += 0.16
    elif y == 15:
        t -= 0.42
    elif y == 14:
        t -= 0.16
    if x == 0:
        t += 0.22
    elif x == 15:
        t -= 0.26
    return mix(c, CASE_LIGHT, t) if t >= 0 else mix(c, CASE_DARK, -t)


# ---- front：正面观察窗（内部共振核心发光竖条）----
def front_pixel(x, y):
    if (x, y) in BOLTS:
        return (*RIVET, 255)
    if 3 <= x <= 12 and 3 <= y <= 12:
        if x in (3, 12) or y in (3, 12):
            return (*mix(CASE_DARK, CORE, 0.5), 255)  # 凹槽边框
        if 6 <= x <= 9:
            t = 1.0 - abs(y - 7.5) / 4.5  # 中间最亮
            c = mix(mix(CORE, GLOW_PURPLE, 0.78), GLOW_WHITE, max(0.0, t - 0.5) * 1.5)
            if y in (7, 8) and x in (7, 8):
                c = mix(GLOW_WHITE, GLOW_CYAN, 0.35)  # 白热核心
            return (*c, 255)
        return (*mix(INSET, CORE, 0.35), 255)
    if y == 14 and x in (6, 8, 10):  # 状态指示灯
        return (*(GLOW_CYAN if x == 8 else GLOW_PURPLE), 255)
    return (*plate(x, y), 255)


# ---- top：顶部进气格栅（圆形凹腔 + 平行格栅条）----
def top_pixel(x, y):
    if (x, y) in BOLTS:
        return (*RIVET, 255)
    dx, dy = x - 7.5, y - 7.5
    r = math.hypot(dx, dy)
    if r <= 6.6:
        if r > 5.5:  # 进气口外圈金属环
            return (*mix(CASE_MID, CASE_LIGHT, 0.18), 255)
        k = (y - 3) % 3  # 每 3 行一组：两条亮筋夹一条暗缝
        if k == 0:
            return (*mix(CASE_LIGHT, CASE_MID, 0.12), 255)
        if k == 1:
            return (*mix(CASE_MID, CASE_LIGHT, 0.35), 255)
        return (*mix(INSET, CORE, 0.55), 255)  # 吸气缝隙
    return (*plate(x, y), 255)


# ---- side：侧面散热鳍片 ----
def side_pixel(x, y):
    if (x, y) in BOLTS:
        return (*RIVET, 255)
    if 2 <= x <= 13 and y in (2, 13):
        return (*mix(CASE_LIGHT, CASE_MID, 0.3 if y == 2 else 0.6), 255)
    if 3 <= x <= 12 and 4 <= y <= 11:
        c = mix(CASE_LIGHT, CASE_MID, 0.15) if x % 2 else mix(CASE_DARK, CASE_MID, 0.3)
        return (*c, 255)
    if 3 <= x <= 12 and y == 12:  # 鳍片下方的余温辉光
        return (*mix(CASE_MID, GLOW_PURPLE, 0.28), 255)
    return (*plate(x, y), 255)


# ---- bottom：底部向虚空的共振喷口 ----
def bottom_pixel(x, y):
    if (x, y) in BOLTS:
        return (*RIVET, 255)
    dx, dy = x - 7.5, y - 7.5
    r = math.hypot(dx, dy)
    if r <= 1.5:
        return (*CORE, 255)
    if r <= 2.6:
        return (*mix(CORE, GLOW_PURPLE, (r - 1.5) / 1.1 * 0.85), 255)
    if r <= 3.6:
        t = (math.sin(math.atan2(dy, dx) * 2.0) + 1.0) / 2.0
        return (*mix(GLOW_PURPLE, GLOW_CYAN, t), 255)
    if r <= 4.4:
        return (*mix(CASE_DARK, CORE, 0.35), 255)
    if r <= 5.6:
        return (*mix(CASE_MID, CASE_LIGHT, 0.25), 255)
    return (*plate(x, y), 255)


# ===================== 暗物质粒子（沿用原设计） =====================
def particle_pixel(x, y):
    dx, dy = x - 7.5, y - 7.5
    r = math.hypot(dx, dy)
    if r > 7.0:
        return (0, 0, 0, 0)
    if r < 2.0:
        return (*PARTICLE_CORE, 255)
    if r < 4.0:
        return (*mix(PARTICLE_CORE, PARTICLE_MID, (r - 2.0) / 2.0), 255)
    t = (r - 4.0) / 3.0
    return (*mix(PARTICLE_MID, PARTICLE_GLOW, t), clamp8(255 * (1.0 - t)))


# ===================== GUI 贴图 =====================
def draw_slot(img, x, y):
    """槽位底框：外 18x18 边框 + 内 16x16 凹槽（上/左压暗做出凹陷感）"""
    rect(img, x - 1, y - 1, 18, 18, (*G_SLOT_EDGE, 255))
    rect(img, x, y, 16, 16, (*G_SLOT_IN, 255))
    hline(img, x, y, 16, (*G_BORDER, 255))
    vline(img, x, y, 16, (*G_BORDER, 255))


def draw_track(img, box):
    """进度/储备条的槽：1px 外边框 + 深色内槽"""
    x, y, w, h = box
    rect(img, x - 1, y - 1, w + 2, h + 2, (*G_TRACK_EDGE, 255))
    rect(img, x, y, w, h, (*G_TRACK_IN, 255))


def player_slot_positions():
    out = []
    for py in PLAYER_ROWS:
        for c in range(9):
            out.append((8 + c * 18, py))
    for c in range(9):
        out.append((8 + c * 18, HOTBAR_Y))
    return out


def build_gui():
    img = new_img(GUI_W, GUI_H, (0, 0, 0, 0))

    # 上半功能区：略亮的深紫，向下渐暗
    for y in range(0, DIVIDER_Y):
        col = mix(G_TEXTAREA, G_BG_MID, (y / max(1, DIVIDER_Y - 1)) * 0.85)
        rect(img, 0, y, GUI_W, 1, (*col, 255))
    # 下半玩家背包区：更暗一层，底部再渐暗
    for y in range(DIVIDER_Y, GUI_H):
        col = mix(G_PLAYER, G_BG_BOT, (y - DIVIDER_Y) / max(1, GUI_H - DIVIDER_Y))
        rect(img, 0, y, GUI_W, 1, (*col, 255))

    # 外框：1px 深色描边 + 内圈顶部/左侧高光，做出浮雕感
    frame(img, 0, 0, GUI_W, GUI_H, (*G_BORDER, 255))
    hline(img, 1, 1, GUI_W - 2, (*G_HILIGHT, 255))
    vline(img, 1, 1, GUI_H - 2, (*G_HILIGHT, 255))
    # 功能区与背包区的分隔
    hline(img, 1, DIVIDER_Y, GUI_W - 2, (*G_BORDER, 255))
    hline(img, 1, DIVIDER_Y + 1, GUI_W - 2, (*mix(G_PLAYER, G_HILIGHT, 0.22), 255))

    # 槽位（含输入/输出）
    for (sx, sy) in [SLOT_IN, SLOT_OUT] + player_slot_positions():
        draw_slot(img, sx, sy)

    # 产出进度条（两槽之间）与共振储备条（两槽下方，带 4 条等级刻度）
    draw_track(img, PROG_INNER)
    draw_track(img, RES_INNER)
    rx, ry, rw, rh = RES_INNER
    for k in range(1, 5):
        vline(img, rx + rw * k // 5, ry, rh, (*G_TICK, 255))

    # 四角切掉 1px，弱化方正感
    for (cx, cy) in ((0, 0), (GUI_W - 1, 0), (0, GUI_H - 1), (GUI_W - 1, GUI_H - 1)):
        put(img, cx, cy, (0, 0, 0, 0))
    return img


# ===================== 入口 =====================
def render(fn, size=16):
    return [[fn(x, y) for x in range(size)] for y in range(size)]


if __name__ == "__main__":
    root = sys.argv[1] if len(sys.argv) > 1 else \
        r"A:\Code\Minecraft\ZuoyanMod\ZuoyanMod\src\main\resources\assets\zuoyanmod"
    preview_dir = sys.argv[2] if len(sys.argv) > 2 else r"A:\Code\Minecraft\ZuoyanMod"

    faces = [
        ("void_resonance_pump_front", front_pixel),
        ("void_resonance_pump_top", top_pixel),
        ("void_resonance_pump_side", side_pixel),
        ("void_resonance_pump_bottom", bottom_pixel),
    ]
    targets = [(os.path.join(root, "textures", "block", n + ".png"), fn, 16) for n, fn in faces]
    targets.append((os.path.join(root, "textures", "item", "dark_matter_particle.png"), particle_pixel, 16))
    targets.append((os.path.join(root, "textures", "gui", "void_resonance_pump.png"), None, 0))

    for path, fn, size in targets:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        pixels = build_gui() if fn is None else render(fn, size)
        write_png(path, pixels)
        print("written:", path)

    # 预览图（放 assets 外，避免被打进 jar）
    if preview_dir:
        os.makedirs(preview_dir, exist_ok=True)
        k = 7
        sheet = [[] for _ in range(16 * k)]
        for _, fn in faces:
            px = scale(render(fn), k)
            for i in range(16 * k):
                sheet[i] = sheet[i] + px[i] + [[0, 0, 0, 0] for _ in range(4)]
        write_png(os.path.join(preview_dir, "pump_faces_preview.png"),
                  [row[:-4] for row in sheet])
        print("written:", os.path.join(preview_dir, "pump_faces_preview.png"))
        write_png(os.path.join(preview_dir, "pump_gui_preview.png"), scale(build_gui(), 3))
        print("written:", os.path.join(preview_dir, "pump_gui_preview.png"))
