#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把"烤进底图的静态件"和"运行时现画的动态件"叠在一起，出一张接近实机观感的预览图。

用途：不启动游戏也能评估终端 UI 的观感与排版。真机上的文字（中文字体）和物品图标
无法在这里复现，所以：
  · 文字一律画成**等宽占位条**（长度按大致字数估），只用来检查排版不越界、不重叠；
  · 物品图标画成色块 + 真实的数量叠字样式；
  · 按钮图标、搜索框、滚动条手柄、合成箭头、克莱因环都是**照着 Java 那边的公式重画**，
    这几个才是"设计"本身，尽量画准。

底图那部分布局常量直接从 gen_klein_terminal_gui.py 导入；
运行时专属的坐标在本文件顶部镜像 KleinTerminalLayout.java。
"""
import importlib.util
import io
import math
import os
import struct
import sys
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))

_spec = importlib.util.spec_from_file_location("gui", os.path.join(HERE, "gen_klein_terminal_gui.py"))
GUI = importlib.util.module_from_spec(_spec)
_saved = sys.stdout
sys.stdout = io.StringIO()
_spec.loader.exec_module(GUI)
GUI.build()  # 底图生成器的 build() 只在 __main__ 里被调用，这里要手动跑一次
sys.stdout = _saved

W, H = GUI.IMAGE_W, GUI.IMAGE_H
IMAGE = [row[:] for row in GUI.grid]  # 底图（已烤好的静态件）

# ===== 运行时专属坐标：镜像 KleinTerminalLayout.java =====
# （四个功能按钮 v6 起是真控件、竖排在最左列，见 side_button()；不再有横排工具行）
TITLE_X, TITLE_Y = 30, 4
STATS_RIGHT = 208
SEARCH_X, SEARCH_Y, SEARCH_W, SEARCH_H = 30, 17, 140, 13
SIDE_BUTTON_X, SIDE_BUTTON_Y, SIDE_BUTTON_SIZE, SIDE_BUTTON_GAP = 8, 34, 16, 2
SIDEBAR_X, SIDEBAR_W = GUI.SIDEBAR_X, GUI.SIDEBAR_W
CRAFT_LABEL_Y = GUI.CRAFT_LABEL_Y
CRAFT_ARROW_X, CRAFT_ARROW_Y, CRAFT_ARROW_W, CRAFT_ARROW_H = 268, 39, 14, 9
FURNACE_LABEL_Y = GUI.FURNACE_LABEL_Y
ANVIL_LABEL_Y = GUI.ANVIL_LABEL_Y
ANVIL_NAME_X, ANVIL_NAME_Y, ANVIL_NAME_W, ANVIL_NAME_H = (
    GUI.ANVIL_NAME_X, GUI.ANVIL_NAME_Y, GUI.ANVIL_NAME_W, GUI.ANVIL_NAME_H)
INFINITY_X, INFINITY_Y = GUI.INFINITY_X, GUI.INFINITY_Y
INFINITY_W, INFINITY_H = GUI.INFINITY_W, GUI.INFINITY_H
LEFT_FLOW_X, LEFT_FLOW_Y = GUI.LEFT_FLOW_X, GUI.LEFT_FLOW_Y
LEFT_FLOW_W, LEFT_FLOW_H = GUI.LEFT_FLOW_W, GUI.LEFT_FLOW_H

BUTTON_HOME, BUTTON_SORT_MODE, BUTTON_SORT_DIR, BUTTON_CLEAR = 0, 1, 2, 3
SIDE_BUTTON_COUNT = 4


def side_button_y(index):
    return SIDE_BUTTON_Y + index * (SIDE_BUTTON_SIZE + SIDE_BUTTON_GAP)


# ===== 与 KleinTheme 一致的配色 =====
TEXT = (237, 231, 246, 255)
TEXT_DIM = (156, 143, 190, 255)
TEXT_FAINT = (94, 83, 120, 255)
CYAN = (110, 227, 212, 255)
CYAN_DEEP = (47, 140, 134, 255)
MAGENTA = (240, 111, 216, 255)
GOLD = (255, 212, 121, 255)
BORDER = (109, 91, 168, 255)
BORDER_BRIGHT = (155, 124, 248, 255)
BTN_FILL = (42, 32, 70, 255)
BTN_FILL_HOVER = (60, 46, 103, 255)
BTN_EDGE = (75, 59, 120, 255)
BTN_EDGE_HOVER = (140, 114, 224, 255)
PANEL_DEEP = (18, 12, 34, 255)


def put(x, y, c):
    if 0 <= x < W and 0 <= y < H and c[3] != 0:
        IMAGE[y][x] = c


def rect(x, y, w, h, c):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            put(xx, yy, c)


def outline(x, y, w, h, c):
    rect(x, y, w, 1, c)
    rect(x, y + h - 1, w, 1, c)
    rect(x, y, 1, h, c)
    rect(x + w - 1, y, 1, h, c)


def mix(a, b, t):
    return (int(a[0] + (b[0] - a[0]) * t), int(a[1] + (b[1] - a[1]) * t),
            int(a[2] + (b[2] - a[2]) * t), 255)


def alpha(c, a):
    return (c[0], c[1], c[2], max(0, min(255, int(a))))


def text_bar(x, y, chars, color, height=8):
    """等宽占位条：一个"字"约 6px（中文约 9px，这里统一取 6 保守估）"""
    rect(x, y, max(2, chars * 6), height, color)


# ===== 运行时：控制区 =====

def search_frame(focused):
    x, y, w, h = SEARCH_X, SEARCH_Y, SEARCH_W, SEARCH_H
    rect(x, y, w, h, alpha(PANEL_DEEP, 235))
    outline(x, y, w, h, alpha(CYAN, 230) if focused else alpha(BORDER, 180))
    cx, cy = x + 7, y + h // 2 - 1
    outline(cx - 3, cy - 3, 6, 6, CYAN if focused else TEXT_DIM)
    rect(cx + 2, cy + 2, 2, 1, CYAN if focused else TEXT_DIM)
    rect(cx + 3, cy + 3, 2, 1, CYAN if focused else TEXT_DIM)
    text_bar(x + 20, y + 3, 13, alpha(TEXT_FAINT, 220), 7)
    rect(x + 20, y + 2, 1, 9, TEXT)


def side_button(index, hovered, descending):
    """浮空无边框的功能按钮（v8）：平时只有图标，hover 才给一层淡底 + 青色下划线"""
    size = SIDE_BUTTON_SIZE
    x, y = SIDE_BUTTON_X, side_button_y(index)
    if hovered:
        rect(x + 1, y + size - 2, size - 2, 1, alpha(CYAN, 217))
    color = TEXT if hovered else TEXT_DIM
    accent = CYAN if hovered else CYAN_DEEP
    s = size
    if index == BUTTON_HOME:                  # 顶杠 + 上箭头
        cx = x + s // 2
        rect(x + 3, y + 3, s - 6, 1, color)
        rect(cx - 1, y + 6, 1, s - 9, accent)
        for i in range(4):
            rect(cx - 4 + i, y + 6 + i, 8 - i * 2, 1, accent)
    elif index == BUTTON_SORT_MODE:           # 长度递减的三条
        right = x + s - 2
        rect(right - 11, y + 3, 11, 1, color)
        rect(right - 11, y + 7, 7, 1, color)
        rect(right - 11, y + 11, 3, 1, color)
        for yy in (3, 7, 11):
            rect(x + 2, y + yy, 1, 1, alpha(color, 153))
    elif index == BUTTON_SORT_DIR:            # 升降箭头
        cx, top, bottom = x + s // 2, y + 3, y + s - 4
        rect(cx - 1, top, 1, bottom - top, accent)
        for i in range(4):
            if descending:
                rect(cx - 4 + i, bottom - i - 1, 8 - i * 2, 1, accent)
            else:
                rect(cx - 4 + i, top + i + 1, 8 - i * 2, 1, accent)
    elif index == BUTTON_CLEAR:               # ×
        length = s - 6
        for i in range(length):
            rect(x + 3 + i, y + 3 + i, 1, 1, color)
            rect(x + 3 + length - 1 - i, y + 3 + i, 1, 1, color)


def craft_icon(x, y, size, color):
    mid = size // 2
    outline(x + 1, y + 1, size - 2, size - 2, color)
    rect(x + mid - 1, y + 2, 1, size - 4, color)
    rect(x + 2, y + mid - 1, size - 4, 1, color)


# ===== 运行时：滚动条 / 徽记 / 合成箭头 =====

def vgrad(x, y, w, h, top, bottom):
    for i in range(h):
        t = i / max(1, h - 1)
        rect(x, y + i, w, 1, mix(top, bottom, t))


def scrollbar(current_row, max_row, hovered=False):
    tx, ty, tw, th = GUI.SCROLLBAR_X + 1, GUI.SCROLLBAR_Y + 1, GUI.SCROLLBAR_W - 2, GUI.SCROLLBAR_H - 2
    if max_row <= 0:
        vgrad(tx, ty, tw, th, alpha(GUI.WELL_LIP, 140), alpha(GUI.WELL_LIP, 64))
        outline(tx, ty, tw, th, alpha(GUI.TICK_DIM, 128))
        return
    handle_h = max(14, th * GUI.ROWS // (max_row + GUI.ROWS))
    handle_y = ty + (th - handle_h) * current_row // max_row
    vgrad(tx, handle_y, tw, handle_h,
          CYAN if hovered else BORDER_BRIGHT, MAGENTA if hovered else CYAN_DEEP)
    outline(tx, handle_y, tw, handle_h, alpha(TEXT, 115))
    for i in range(3):
        gy = handle_y + handle_h // 2 - 3 + i * 3
        if handle_y + 2 < gy < handle_y + handle_h - 3:
            rect(tx + 3, gy, tw - 6, 1, alpha(PANEL_DEEP, 191))


def portal_rings(cx, cy, radius, time=0.0):
    """三圈同心、纵向压扁、带缺口、反向慢转的点阵环 —— 对应 KleinTheme.portalRings"""
    for ring in range(3):
        r = radius * (1.0 - ring * 0.28)
        phase = time * (0.5 + ring * 0.35) * (1.0 if ring % 2 == 0 else -1.0)
        steps = 48 - ring * 10
        color = mix(CYAN, MAGENTA, ring / 2.0)
        base = 0.85 - ring * 0.18
        for i in range(steps):
            th = phase + i * 2 * math.pi / steps
            gap = 0.5 + 0.5 * math.sin(th * 2.0 + phase)
            put(int(cx + math.cos(th) * r), int(cy + math.sin(th) * r * 0.70),
                alpha(color, 255 * base * (0.30 + 0.70 * gap)))
    put(int(cx), int(cy), alpha(TEXT, 180))
    put(int(cx) + 1, int(cy) + 1, alpha(TEXT, 120))


def craft_arrow():
    ax, ay, aw, ah = CRAFT_ARROW_X, CRAFT_ARROW_Y, CRAFT_ARROW_W, CRAFT_ARROW_H
    mid = ay + ah // 2
    for i in range(aw - 4):
        rect(ax + i, mid, 1, 1, alpha(CYAN, 217))
    for i in range(4):
        rect(ax + aw - 5 + i, mid - 3 + i, 1, 1, alpha(CYAN, 217))
        rect(ax + aw - 5 + i, mid + 2 - i, 1, 1, alpha(CYAN, 217))


# ===== 运行时：无限符号 / 炉火 / 进度条 =====

def infinity_flow(x, y, w, h, time=0.0):
    """竖放的 ∞ + 一个顺着曲线往下淌的光点（对应 KleinTheme#infinityFlow）"""
    cx, cy = x + w / 2.0, y + h / 2.0
    ax, ay = w - 2.0, h / 2.0 - 1.0
    steps = 280
    for i in range(steps):
        t = i * 2 * math.pi / steps
        px = cx + math.sin(t) * math.cos(t) * ax
        py = cy - math.cos(t) * ay
        flow = 0.5 + 0.5 * math.sin(t - time * 2.0)
        put(int(px), int(py), alpha(mix(CYAN_DEEP, CYAN, flow), 255 * (0.22 + 0.60 * flow)))
    head = ((time * 0.45) % 1.0) * 2 * math.pi
    for k in range(7):
        t = head - k * 0.13
        px = cx + math.sin(t) * math.cos(t) * ax
        py = cy - math.cos(t) * ay
        put(int(px), int(py), alpha(TEXT, 255 * (0.85 * (1 - k / 7.0) + 0.08)))


def flame(x, y, w, h, fill, time=0.0):
    """炉火：高度 = 剩余燃料，从下往上烧掉（对应 KleinTheme#flame）"""
    rect(x, y, w, h, alpha(GUI.WELL_DEEP, 224))
    outline(x, y, w, h, alpha(GUI.WELL_EDGE, 217))
    if fill <= 0:
        return
    lit = max(1, int(round(h * fill)))
    cx = x + w / 2.0
    for row in range(lit):
        yy = y + h - 1 - row
        t = row / max(1, lit - 1)
        flicker = 0.92 + 0.08 * math.sin(time * 6.5 + row * 0.9)
        half = (w / 2.0) * (0.95 - 0.60 * t) * flicker
        color = mix(GOLD, CYAN, t * 0.85)
        rect(int(round(cx - half)), yy, max(1, int(round(2 * half))), 1, alpha(color, 242))


def progress_bar(x, y, w, h, progress, time=0.0):
    rect(x, y, w, h, alpha(GUI.WELL_DEEP, 224))
    outline(x, y, w, h, alpha(GUI.WELL_EDGE, 217))
    filled = int(round(w * progress))
    if filled <= 0:
        return
    for i in range(filled):
        rect(x + i, y + 1, 1, h - 2, alpha(mix(CYAN_DEEP, CYAN, i / w), 242))
    pulse = 0.55 + 0.35 * math.sin(time * 4.0)
    rect(x + filled - 1, y, 2, h, alpha(TEXT, 255 * pulse))


def anvil_icon(x, y, size, color):
    base = y + size - 3
    rect(x + 1, base, size - 2, 2, color)
    rect(x + 3, base - 2, size - 6, 2, color)
    rect(x + 2, base - 4, size - 4, 2, color)
    rect(x + 1, base - 6, size - 2, 2, color)
    rect(x + size - 4, base - 5, 2, 1, color)


def furnace_icon(x, y, size, color, fire):
    outline(x + 1, y + 1, size - 2, size - 2, color)
    rect(x + 2, y + size - 3, size - 4, 1, color)
    cx, base = x + size // 2, y + size - 4
    for r in range(4):
        half = max(1, (4 - r) // 2 + 1)
        rect(cx - half, base - r, half * 2, 1, fire)


# ===== 假物品与假数量 =====

ITEM_COLORS = [
    (140, 100, 200, 255), (90, 150, 210, 255), (200, 170, 90, 255),
    (110, 190, 140, 255), (200, 110, 120, 255), (150, 140, 210, 255),
    (120, 200, 200, 255), (180, 130, 200, 255), (160, 160, 170, 255),
    (190, 150, 110, 255), (120, 130, 210, 255), (170, 200, 130, 255),
]
# 一格一种物品之后，格子应该长这样：每种东西一个格子，数量可以很大
AMOUNTS = ["3.3k", "1.2k", "256", "12.3k", "64", "999", "45.6k", "128", "1.8M",
           "320", "7", "2.4k", "18", "512", "96", "77.7k", "1", "4.5k"]


def fake_item(col, row, index):
    c = ITEM_COLORS[index % len(ITEM_COLORS)]
    amount = AMOUNTS[index % len(AMOUNTS)]
    sx = GUI.GRID_X + col * GUI.CELL
    sy = GUI.GRID_Y + row * GUI.CELL
    rect(sx + 2, sy + 2, 12, 12, c)
    rect(sx + 3, sy + 3, 5, 5, mix(c, (255, 255, 255, 255), 0.45))
    rect(sx + 9, sy + 9, 5, 5, mix(c, (0, 0, 0, 255), 0.35))
    # 数量叠字：只画"占位方块"，长度按字符数缩放
    scale = 1.0 if len(amount) <= 2 else (0.75 if len(amount) <= 3 else 0.6)
    bw = int(len(amount) * 6 * scale)
    bh = max(4, int(8 * scale))
    color = CYAN if amount.endswith(("k", "M")) else TEXT
    rect(sx + 16 - bw - 1, sy + 16 - bh - 1, bw, bh, color)


def build_mock():
    # 标题行：左边标题，右边统计，同一行，没有徽记动效
    text_bar(TITLE_X, TITLE_Y, 4, TEXT, 8)                        # 「四维空间」
    stats_w = 15 * 6
    rect(STATS_RIGHT - stats_w, TITLE_Y, stats_w, 8, GOLD)        # "18 种 · 123,456 个"

    # 搜索框 + 最左列竖排的功能按钮（第 3 个演示 hover 态）
    search_frame(focused=False)
    side_button(BUTTON_HOME, False, True)
    side_button(BUTTON_SORT_MODE, False, True)
    side_button(BUTTON_SORT_DIR, True, True)
    side_button(BUTTON_CLEAR, False, True)

    # 滚动条（一屏放得下 → 置灰态）
    scrollbar(current_row=0, max_row=0, hovered=False)

    # 右栏：合成
    craft_icon(SIDEBAR_X + 6, CRAFT_LABEL_Y - 1, 10, BORDER_BRIGHT)
    text_bar(SIDEBAR_X + 20, CRAFT_LABEL_Y, 2, TEXT, 8)           # 「合成」
    craft_arrow()
    for (col, row, idx) in ((0, 0, 2), (1, 0, 10), (2, 0, 5), (1, 1, 11), (0, 2, 3), (2, 2, 6)):
        sx = GUI.CRAFT_GRID_X + col * GUI.CELL
        sy = GUI.CRAFT_GRID_Y + row * GUI.CELL
        rect(sx + 3, sy + 3, 10, 10, ITEM_COLORS[idx % len(ITEM_COLORS)])
    rect(GUI.CRAFT_RESULT_X + 3, GUI.CRAFT_RESULT_Y + 3, 10, 10, (230, 200, 120, 255))

    # 中卡：熔炉（输入 / 火 / 燃料 + 进度条 + 产物）
    furnace_icon(SIDEBAR_X + 6, FURNACE_LABEL_Y - 1, 10, BORDER_BRIGHT, GOLD)
    text_bar(SIDEBAR_X + 20, FURNACE_LABEL_Y, 2, TEXT, 8)         # 「熔炼」
    rect(GUI.FURNACE_INPUT_X + 3, GUI.FURNACE_INPUT_Y + 3, 10, 10, (150, 130, 120, 255))
    rect(GUI.FURNACE_FUEL_X + 3, GUI.FURNACE_FUEL_Y + 3, 10, 10, (70, 60, 55, 255))
    flame(GUI.FLAME_X, GUI.FLAME_Y, GUI.FLAME_W, GUI.FLAME_H, fill=0.72, time=1.1)
    progress_bar(GUI.FURNACE_ARROW_X, GUI.FURNACE_ARROW_Y,
                 GUI.FURNACE_ARROW_W, GUI.FURNACE_ARROW_H, progress=0.45, time=1.1)
    rect(GUI.FURNACE_RESULT_X + 3, GUI.FURNACE_RESULT_Y + 3, 10, 10, (235, 205, 125, 255))

    # 下卡：铁砧（目标 + 材料 + 产物 + 改名框 + 代价）
    anvil_icon(SIDEBAR_X + 6, ANVIL_LABEL_Y - 1, 10, BORDER_BRIGHT)
    text_bar(SIDEBAR_X + 20, ANVIL_LABEL_Y, 2, TEXT, 8)           # 「铁砧」
    rect(GUI.ANVIL_BASE_X + 3, GUI.ANVIL_BASE_Y + 3, 10, 10, (140, 145, 165, 255))
    rect(GUI.ANVIL_MATERIAL_X + 3, GUI.ANVIL_MATERIAL_Y + 3, 10, 10, (110, 120, 100, 255))
    rect(GUI.ANVIL_RESULT_X + 3, GUI.ANVIL_RESULT_Y + 3, 10, 10, (225, 215, 150, 255))
    # 改名框（文字占位条）+ 代价条
    rect(ANVIL_NAME_X + 4, ANVIL_NAME_Y + 3, 30, 6, TEXT_DIM)
    text_bar(GUI.ANVIL_COST_X, GUI.ANVIL_COST_Y, 7, (150, 200, 150, 255), 7)

    # 两处会流动的 ∞：物品栏右侧 + 左列按钮下方（相位错开）
    infinity_flow(INFINITY_X, INFINITY_Y, INFINITY_W, INFINITY_H, time=1.1)
    infinity_flow(LEFT_FLOW_X, LEFT_FLOW_Y, LEFT_FLOW_W, LEFT_FLOW_H, time=1.1 + 2.1)

    # 存储内容：18 种物品各占一格（演示"一格一种"）
    n = 0
    for row in range(2):
        for col in range(9):
            fake_item(col, row, n)
            n += 1

    # 悬停高亮：给一格画青色括号
    hx = GUI.GRID_X + 4 * GUI.CELL
    hy = GUI.GRID_Y + 2 * GUI.CELL
    for (x, y, w, h) in ((hx - 1, hy - 1, 5, 1), (hx - 1, hy - 1, 1, 5),
                         (hx + 13, hy - 1, 5, 1), (hx + 17, hy - 1, 1, 5),
                         (hx - 1, hy + 16, 5, 1), (hx - 1, hy + 12, 1, 5),
                         (hx + 13, hy + 16, 5, 1), (hx + 17, hy + 12, 1, 5)):
        rect(x, y, w, h, CYAN)

    # 背包
    text_bar(GUI.GRID_X, GUI.INV_LABEL_Y, 3, TEXT_DIM, 8)
    for row in range(3):
        for col in range(9):
            if (row * 9 + col) % 4 == 0:
                sx = GUI.GRID_X + col * GUI.CELL
                sy = GUI.INV_Y + row * GUI.CELL
                rect(sx + 2, sy + 2, 12, 12, ITEM_COLORS[(row + col) % len(ITEM_COLORS)])

    outline(-1, -1, W + 2, H + 2, alpha(CYAN, 66))


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


def scale_up(pixels, scale=2):
    out = []
    for y in range(len(pixels) * scale):
        row = []
        for x in range(len(pixels[0]) * scale):
            px = pixels[y // scale][x // scale]
            if px[3] == 0:
                s = 90 if ((x // 12) + (y // 12)) % 2 == 0 else 60
                row.append((s, s, s, 255))
            else:
                row.append(px)
        out.append(row)
    return out


if __name__ == "__main__":
    build_mock()
    workspace = os.path.dirname(os.path.dirname(HERE))
    pv = os.path.join(workspace, "klein_terminal_mock.png")
    write_png(pv, scale_up(IMAGE, 2))
    print("wrote", pv, f"({W}x{H} @2x)")
