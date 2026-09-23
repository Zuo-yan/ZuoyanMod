#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""「四维空间」终端底图 klein_terminal.png（318x244）。纯标准库手写 PNG。

布局公式与 util/KleinTerminalLayout.java **逐行对应**，改一处必改另一处：
  四个功能按钮竖排在最左列 (8,34) 起步 16x16、间隔 2 —— **底图不画**，是真控件
  （KleinSideButton，v6 之前是手画+手写命中判定，点了没反应）；
  标题只有一行（y=4，左标题右统计）；搜索框 (30,17) 140x13；
  存储网格 (30,34) 9x6；滚动条槽 (196,33) 14x108；
  右栏 (208,2)..(310,236)：合成 3x3 (212,16) / 产物槽 (286,35) / 分隔线 y=78
  / 熔炉 输入 (212,98) 燃料 (212,134) 产物 (284,116) / 最底下涡环 cy=190；
  分隔线 y=146（只横跨左列）；背包 (30,162) 3 行 + 快捷栏 (30,220)；
  物品栏右侧 (196,146) 14x92 是无限符号 ∞ 的位置——底图**什么都不画**。

只烤**静态**的东西：面板、54 个存储凹槽、9+1 个合成凹槽、3 个熔炉凹槽、
36 个背包凹槽、滚动条凹槽、右栏面板。按钮、搜索框、滚动条手柄、箭头、
炉火、进度条、无限符号都带状态或动效，一律运行时现画。

⚠️ 槽位凹槽必须从 (slot.x - 1, slot.y - 1) 起画 18x18：物品图标画在 slot.x/slot.y
且占 16x16，凹槽要往外扩 1px 才正好把图标框住（旧版漏了这一下，框和图标差一像素）。
"""
import os
import struct
import zlib

# ===== 与 KleinTerminalLayout 对应的布局常量 =====
IMAGE_W = 318
GRID_X, GRID_Y = 30, 34
CELL, COLUMNS, ROWS = 18, 9, 6
GRID_W, GRID_H = COLUMNS * CELL, ROWS * CELL
GRID_BOTTOM = GRID_Y + GRID_H
SCROLLBAR_X, SCROLLBAR_Y = 196, GRID_Y - 1
SCROLLBAR_W, SCROLLBAR_H = 14, GRID_H
SEPARATOR_Y = GRID_BOTTOM + 4
INV_LABEL_Y = SEPARATOR_Y + 5
INV_Y = INV_LABEL_Y + 11
HOTBAR_Y = INV_Y + 58
BOTTOM_PAD = 6
IMAGE_H = HOTBAR_Y + CELL + BOTTOM_PAD

# 物品栏右侧的 ∞：底图留空，运行时画（见 KleinTheme#infinityFlow）
INFINITY_X, INFINITY_Y = 196, GRID_BOTTOM + 4
INFINITY_W = 14
INFINITY_H = IMAGE_H - BOTTOM_PAD - INFINITY_Y

# 四个功能按钮：真控件（KleinSideButton），底图不画
SIDE_BUTTON_X, SIDE_BUTTON_Y, SIDE_BUTTON_SIZE, SIDE_BUTTON_GAP = 8, 34, 16, 2

SIDEBAR_X, SIDEBAR_W = 208, 102
CARD_X, CARD_W = 210, 98
SIDEBAR_TOP, SIDEBAR_BOTTOM = 2, IMAGE_H - 8
SIDEBAR_RIGHT = SIDEBAR_X + SIDEBAR_W
CRAFT_LABEL_Y = 4
CRAFT_CARD_Y, CRAFT_CARD_H = 13, 62
CRAFT_GRID_X, CRAFT_GRID_Y = 212, 16
CRAFT_RESULT_X, CRAFT_RESULT_Y = 286, 35
FURNACE_LABEL_Y = 79
FURNACE_CARD_Y, FURNACE_CARD_H = 88, 62
FURNACE_INPUT_X, FURNACE_INPUT_Y = 212, 92
FURNACE_FUEL_X, FURNACE_FUEL_Y = 212, 130
FURNACE_RESULT_X, FURNACE_RESULT_Y = 284, 108
FLAME_X, FLAME_Y, FLAME_W, FLAME_H = 213, 111, 14, 14
FURNACE_ARROW_X, FURNACE_ARROW_Y = 236, 112
FURNACE_ARROW_W, FURNACE_ARROW_H = 32, 9

ANVIL_LABEL_Y = 154
ANVIL_CARD_Y, ANVIL_CARD_H = 163, 66
ANVIL_BASE_X, ANVIL_BASE_Y = 214, 168
ANVIL_MATERIAL_X, ANVIL_MATERIAL_Y = 242, 168
ANVIL_RESULT_X, ANVIL_RESULT_Y = 284, 168
ANVIL_NAME_X, ANVIL_NAME_Y, ANVIL_NAME_W, ANVIL_NAME_H = 214, 192, 88, 12
ANVIL_COST_X, ANVIL_COST_Y = 214, 208

# 三张功能卡片的色调与 accent：合成 = 暖紫 / 熔炉 = 炉火橙 / 铁砧 = 钢灰蓝
CARD_TINT_CRAFT = (94, 66, 84, 255)
CARD_TINT_FURNACE = (104, 62, 40, 255)
CARD_TINT_ANVIL = (58, 70, 92, 255)
CARD_ACCENT_CRAFT = (150, 110, 128, 255)
CARD_ACCENT_FURNACE = (188, 110, 62, 255)
CARD_ACCENT_ANVIL = (104, 122, 150, 255)

# ===== 配色（与 KleinTheme 同一套紫金 + 青色强调）=====
PANEL_TOP = (38, 28, 62, 255)
PANEL_BOT = (18, 12, 34, 255)
PANEL_INNER = (26, 19, 44, 255)
SIDE_BG_TOP = (34, 25, 56, 255)
SIDE_BG_BOT = (21, 14, 38, 255)
HEADER_TOP = (47, 36, 76, 255)
HEADER_BOT = (31, 23, 52, 255)
BEVEL_HI = (96, 76, 144, 255)
BEVEL_LO = (40, 30, 64, 255)
LINE = (58, 45, 94, 255)
LINE_DIM = (38, 29, 62, 255)

WELL_EDGE = (46, 36, 74, 255)
WELL_DEEP = (13, 9, 24, 255)
WELL_SHADE = (7, 4, 14, 255)
WELL_LIP = (56, 44, 88, 255)
CRAFT_EDGE = (74, 52, 70, 255)
CRAFT_LIP = (104, 74, 92, 255)
TICK = (72, 132, 128, 255)
TICK_DIM = (46, 88, 86, 255)
GOLD_DIM = (128, 100, 52, 255)

CYAN = (110, 227, 212, 255)
MAGENTA = (240, 111, 216, 255)
GOLD = (255, 212, 121, 255)

grid = [[(0, 0, 0, 0) for _ in range(IMAGE_W)] for _ in range(IMAGE_H)]


def put(x, y, c):
    if 0 <= x < IMAGE_W and 0 <= y < IMAGE_H and c[3] != 0:
        grid[y][x] = c


def rect(x, y, w, h, c):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            put(xx, yy, c)


def hline(x0, x1, y, c):
    for xx in range(x0, x1 + 1):
        put(xx, y, c)


def outline(x, y, w, h, c):
    rect(x, y, w, 1, c)
    rect(x, y + h - 1, w, 1, c)
    rect(x, y, 1, h, c)
    rect(x + w - 1, y, 1, h, c)


def vline(x, y0, y1, c):
    for yy in range(y0, y1 + 1):
        put(x, yy, c)


def mix(a, b, t):
    return (int(a[0] + (b[0] - a[0]) * t),
            int(a[1] + (b[1] - a[1]) * t),
            int(a[2] + (b[2] - a[2]) * t),
            int(a[3] + (b[3] - a[3]) * t))


def with_alpha(c, a):
    return (c[0], c[1], c[2], max(0, min(255, int(a))))


def over(dst, src):
    sa = src[3] / 255.0
    if sa <= 0:
        return dst
    return (int(src[0] * sa + dst[0] * (1 - sa)),
            int(src[1] * sa + dst[1] * (1 - sa)),
            int(src[2] * sa + dst[2] * (1 - sa)),
            255)


# ===== 构件 =====

def panel():
    for y in range(IMAGE_H):
        t = y / (IMAGE_H - 1)
        row = mix(PANEL_TOP, PANEL_BOT, t)
        for x in range(IMAGE_W):
            put(x, y, row)
    hline(0, IMAGE_W - 1, 0, BEVEL_HI)
    vline(0, 0, IMAGE_H - 1, BEVEL_HI)
    hline(0, IMAGE_W - 1, IMAGE_H - 1, BEVEL_LO)
    vline(IMAGE_W - 1, 0, IMAGE_H - 1, BEVEL_LO)
    hline(1, IMAGE_W - 2, 1, mix(BEVEL_HI, PANEL_TOP, 0.55))
    vline(1, 1, IMAGE_H - 2, mix(BEVEL_HI, PANEL_TOP, 0.55))
    hline(1, IMAGE_W - 2, IMAGE_H - 2, mix(BEVEL_LO, PANEL_BOT, 0.5))
    vline(IMAGE_W - 2, 1, IMAGE_H - 2, mix(BEVEL_LO, PANEL_BOT, 0.5))
    # 第 2 层嵌套方框：呼应槽位的"方框套方框"
    hline(3, IMAGE_W - 4, 3, LINE_DIM)
    vline(3, 3, IMAGE_H - 4, LINE_DIM)
    hline(3, IMAGE_W - 4, IMAGE_H - 4, LINE_DIM)
    vline(IMAGE_W - 4, 3, IMAGE_H - 4, LINE_DIM)


def header():
    """标题带的底：一条稍亮的横带 + 下沿一根线 + 一条极淡的青→金渐变发丝。

    带子只到 y=13：标题文字画在 y=4（运行时），下面从 y=17 起就是搜索框，
    再往下（y=34）直接是存储网格 —— 工具按钮那行已经还给网格了。
    """
    top, bottom = 2, 14
    for y in range(top, bottom):
        t = (y - top) / max(1, bottom - top - 1)
        row = mix(HEADER_TOP, HEADER_BOT, t)
        for x in range(2, IMAGE_W - 2):
            put(x, y, row)
    hline(2, IMAGE_W - 3, bottom - 1, LINE)
    span = IMAGE_W - 12
    for i in range(span):
        t = i / (span - 1)
        c = mix(CYAN, MAGENTA, t * 2) if t < 0.5 else mix(MAGENTA, GOLD, (t - 0.5) * 2)
        x = 6 + i
        put(x, bottom - 2, over(grid[bottom - 2][x], with_alpha(c, 104)))


def well(slot_x, slot_y, tesseract, edge=WELL_EDGE, lip=WELL_LIP):
    """18x18 槽位凹槽，从 (slot_x-1, slot_y-1) 起画（物品图标占 slot_x..+15）"""
    x, y = slot_x - 1, slot_y - 1
    rect(x, y, CELL, CELL, edge)
    rect(x + 1, y + 1, CELL - 2, CELL - 2, WELL_DEEP)
    hline(x + 1, x + CELL - 2, y + 1, WELL_SHADE)
    vline(x + 1, y + 1, y + CELL - 2, WELL_SHADE)
    hline(x + 1, x + CELL - 2, y + CELL - 2, lip)
    vline(x + CELL - 2, y + 1, y + CELL - 2, lip)
    if not tesseract:
        return
    for (ax, ay, dx, dy) in ((x, y, 1, 1), (x + CELL - 1, y, -1, 1),
                             (x, y + CELL - 1, 1, -1), (x + CELL - 1, y + CELL - 1, -1, -1)):
        put(ax, ay, TICK)
        put(ax + dx, ay, TICK_DIM)
        put(ax, ay + dy, TICK_DIM)


def storage_grid():
    for row in range(ROWS):
        for col in range(COLUMNS):
            well(GRID_X + col * CELL, GRID_Y + row * CELL, True)


def scrollbar_groove():
    rect(SCROLLBAR_X, SCROLLBAR_Y, SCROLLBAR_W, SCROLLBAR_H, WELL_EDGE)
    rect(SCROLLBAR_X + 1, SCROLLBAR_Y + 1, SCROLLBAR_W - 2, SCROLLBAR_H - 2, (9, 6, 16, 255))
    hline(SCROLLBAR_X + 1, SCROLLBAR_X + SCROLLBAR_W - 2, SCROLLBAR_Y + 1, WELL_SHADE)
    vline(SCROLLBAR_X + 1, SCROLLBAR_Y + 1, SCROLLBAR_Y + SCROLLBAR_H - 2, WELL_SHADE)
    hline(SCROLLBAR_X + 1, SCROLLBAR_X + SCROLLBAR_W - 2, SCROLLBAR_Y + SCROLLBAR_H - 2, WELL_LIP)
    vline(SCROLLBAR_X + SCROLLBAR_W - 2, SCROLLBAR_Y + 1, SCROLLBAR_Y + SCROLLBAR_H - 2, WELL_LIP)


def sidebar():
    """右栏：合成区面板。用偏暖一点的暗紫，和存储区区分开"""
    for y in range(SIDEBAR_TOP, SIDEBAR_BOTTOM + 1):
        t = (y - SIDEBAR_TOP) / max(1, SIDEBAR_BOTTOM - SIDEBAR_TOP)
        row = mix(SIDE_BG_TOP, SIDE_BG_BOT, t)
        for x in range(SIDEBAR_X, SIDEBAR_RIGHT + 1):
            put(x, y, row)
    # 边框：上/左稍亮，下/右更暗，读作一块独立的板
    hline(SIDEBAR_X, SIDEBAR_RIGHT, SIDEBAR_TOP, LINE)
    vline(SIDEBAR_X, SIDEBAR_TOP, SIDEBAR_BOTTOM, LINE)
    hline(SIDEBAR_X, SIDEBAR_RIGHT, SIDEBAR_BOTTOM, LINE_DIM)
    vline(SIDEBAR_RIGHT, SIDEBAR_TOP, SIDEBAR_BOTTOM, LINE_DIM)
    # 四角小三角：和存储区的超立方角标同源
    for (ax, ay, dx, dy) in ((SIDEBAR_X + 1, SIDEBAR_TOP + 1, 1, 1),
                             (SIDEBAR_RIGHT - 1, SIDEBAR_TOP + 1, -1, 1),
                             (SIDEBAR_X + 1, SIDEBAR_BOTTOM - 1, 1, -1),
                             (SIDEBAR_RIGHT - 1, SIDEBAR_BOTTOM - 1, -1, -1)):
        for i in range(4):
            put(ax + dx * i, ay, with_alpha(CYAN, 140 - i * 32))
            put(ax, ay + dy * i, with_alpha(CYAN, 140 - i * 32))


def craft_grid():
    """3x3 合成网格 + 产物槽：凹槽用暖紫边，一眼区别于存储槽"""
    for row in range(3):
        for col in range(3):
            well(CRAFT_GRID_X + col * CELL, CRAFT_GRID_Y + row * CELL, False, CRAFT_EDGE, CRAFT_LIP)
    # 产物槽：金边 + 四角金点，强调"这是产出"
    rx, ry = CRAFT_RESULT_X, CRAFT_RESULT_Y
    well(rx, ry, False, GOLD_DIM, (150, 118, 60, 255))
    for (ax, ay) in ((rx - 1, ry - 1), (rx + CELL - 2, ry - 1),
                     (rx - 1, ry + CELL - 2), (rx + CELL - 2, ry + CELL - 2)):
        put(ax, ay, GOLD)


def furnace_grid():
    """熔炉三格：输入/燃料用暖紫边（和合成同源），产物用金边——表示"这是产出"。

    炉火与进度条的底板是运行时画的（KleinTheme#flame / #progressBar 自带背景），
    所以这里不给它们留凹槽，免得叠出两层框。
    """
    well(FURNACE_INPUT_X, FURNACE_INPUT_Y, False, CRAFT_EDGE, CRAFT_LIP)
    well(FURNACE_FUEL_X, FURNACE_FUEL_Y, False, CRAFT_EDGE, CRAFT_LIP)
    rx, ry = FURNACE_RESULT_X, FURNACE_RESULT_Y
    well(rx, ry, False, GOLD_DIM, (150, 118, 60, 255))
    for (ax, ay) in ((rx - 1, ry - 1), (rx + CELL - 2, ry - 1),
                     (rx - 1, ry + CELL - 2), (rx + CELL - 2, ry + CELL - 2)):
        put(ax, ay, GOLD)
    # 炉膛：把输入 → 火 → 燃料这一竖列框成一格"炉子"，读起来才像一台机器
    hx, hy = FURNACE_INPUT_X - 4, FURNACE_INPUT_Y - 4
    hw = FURNACE_ARROW_X + FURNACE_ARROW_W + 2 - hx
    hh = FURNACE_FUEL_Y + CELL + 2 - hy
    hline(hx, hx + hw, hy, LINE_DIM)
    hline(hx, hx + hw, hy + hh, LINE_DIM)
    vline(hx, hy, hy + hh, LINE_DIM)
    vline(hx + hw, hy, hy + hh, LINE_DIM)
    for (ax, ay, dx, dy) in ((hx, hy, 1, 1), (hx + hw, hy, -1, 1),
                             (hx, hy + hh, 1, -1), (hx + hw, hy + hh, -1, -1)):
        for i in range(3):
            put(ax + dx * i, ay, with_alpha(GOLD, 120 - i * 34))
            put(ax, ay + dy * i, with_alpha(GOLD, 120 - i * 34))


def section_card(y, h, tint, accent):
    """一张功能区卡片：比面板亮一档的底 + 该区的 accent 边框 + 顶部一条亮边。

    层次感就是这三件事叠出来的：底色分层、边框有色调、顶边像"标题条"。
    标题（图标 + 文字）画在卡片外面上头，运行时画。
    """
    for yy in range(y, y + h):
        t = (yy - y) / max(1, h - 1)
        row = mix(tint, SIDE_BG_BOT, 0.30 + 0.30 * t)
        for xx in range(CARD_X + 1, CARD_X + CARD_W - 1):
            put(xx, yy, over(grid[yy][xx], row))
    outline(CARD_X, y, CARD_W, h, mix(accent, (0, 0, 0, 255), 0.45))
    hline(CARD_X + 1, CARD_X + CARD_W - 2, y, accent)
    hline(CARD_X + 1, CARD_X + CARD_W - 2, y + 1, with_alpha(accent, 110))
    # 左上/右上两个小角标：和存储槽的"超立方角标"同源，标出"这是一张卡"
    for (ax, ay, dx) in ((CARD_X + 1, y + 2, 1), (CARD_X + CARD_W - 2, y + 2, -1)):
        put(ax, ay, accent)
        put(ax + dx, ay, with_alpha(accent, 150))


def sidebar_divider():
    section_card(CRAFT_CARD_Y, CRAFT_CARD_H, CARD_TINT_CRAFT, CARD_ACCENT_CRAFT)
    section_card(FURNACE_CARD_Y, FURNACE_CARD_H, CARD_TINT_FURNACE, CARD_ACCENT_FURNACE)
    section_card(ANVIL_CARD_Y, ANVIL_CARD_H, CARD_TINT_ANVIL, CARD_ACCENT_ANVIL)


def separator():
    hline(GRID_X, SCROLLBAR_X - 2, SEPARATOR_Y, LINE)
    for i in range(40):
        x = GRID_X + 20 + i
        put(x, SEPARATOR_Y, over(grid[SEPARATOR_Y][x], with_alpha(CYAN, 44)))


def player_inventory():
    for row in range(3):
        for col in range(9):
            well(GRID_X + col * CELL, INV_Y + row * CELL, False)
    for col in range(9):
        well(GRID_X + col * CELL, HOTBAR_Y, False)
    hline(GRID_X, GRID_X + GRID_W - 1, HOTBAR_Y - 4, LINE_DIM)


def anvil_grid():
    """铁砧三格：目标 / 材料（普通暖紫边），产物金边。改名框凹槽也在这里烤。"""
    well(ANVIL_BASE_X, ANVIL_BASE_Y, False, CRAFT_EDGE, CRAFT_LIP)
    well(ANVIL_MATERIAL_X, ANVIL_MATERIAL_Y, False, CRAFT_EDGE, CRAFT_LIP)
    rx, ry = ANVIL_RESULT_X, ANVIL_RESULT_Y
    well(rx, ry, False, GOLD_DIM, (150, 118, 60, 255))
    for (ax, ay) in ((rx - 1, ry - 1), (rx + CELL - 2, ry - 1),
                     (rx - 1, ry + CELL - 2), (rx + CELL - 2, ry + CELL - 2)):
        put(ax, ay, GOLD)
    # 改名框凹槽（真 EditBox 在运行时画文字）
    rect(ANVIL_NAME_X, ANVIL_NAME_Y, ANVIL_NAME_W, ANVIL_NAME_H, WELL_DEEP)
    outline(ANVIL_NAME_X, ANVIL_NAME_Y, ANVIL_NAME_W, ANVIL_NAME_H,
            mix(CARD_ACCENT_ANVIL, (0, 0, 0, 255), 0.35))
    hline(ANVIL_NAME_X + 1, ANVIL_NAME_X + ANVIL_NAME_W - 2, ANVIL_NAME_Y, with_alpha(CARD_ACCENT_ANVIL, 90))


def build():
    panel()
    header()
    storage_grid()
    scrollbar_groove()
    sidebar()
    sidebar_divider()
    craft_grid()
    furnace_grid()
    anvil_grid()
    separator()
    player_inventory()


# ===== PNG 输出 =====

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


def preview(pixels, scale=2, checker=True):
    out = []
    for y in range(len(pixels) * scale):
        row = []
        for x in range(len(pixels[0]) * scale):
            px = pixels[y // scale][x // scale]
            if px[3] == 0 and checker:
                s = 90 if ((x // 12) + (y // 12)) % 2 == 0 else 60
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
                          "textures", "gui", "klein_terminal.png")
    os.makedirs(os.path.dirname(target), exist_ok=True)
    write_png(target, grid)
    print("wrote", target, f"({IMAGE_W}x{IMAGE_H})")
    pv = os.path.join(workspace, "klein_terminal_preview.png")
    write_png(pv, preview(grid))
    print("wrote", pv)
