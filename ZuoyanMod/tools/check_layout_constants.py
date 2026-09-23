#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""校验 kleinterminal 的布局常量在三处是否一致。

布局数字散在三个地方，改一处漏一处就会错位，而且错了只有进游戏才看得出来：

  1. src/main/java/.../util/KleinTerminalLayout.java   —— 服务端建槽 + 客户端绘制
  2. tools/gen_klein_terminal_gui.py                   —— 烤进底图的静态凹槽
  3. （客户端用的 2 号文件的常量，故 2 与 1 对齐即可）

2 号文件里的常量是从 Kotlin/Java 那边**手抄**过来的，所以这里反过来用 Java 当基准，
把 Java 的 `public static final int NAME = <表达式>;` 全解析出来（支持引用前面的常量），
再跟 Python 那边的模块属性逐个比对。

用法：  python tools/check_layout_constants.py
退出码：0 = 全部一致；1 = 有差异（会逐条打印）
"""
import importlib.util
import io
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
MOD_ROOT = os.path.dirname(HERE)
LAYOUT_JAVA = os.path.join(MOD_ROOT, "src", "main", "java", "org", "gwfx", "zuoyanmod",
                           "util", "KleinTerminalLayout.java")
GUI_PY = os.path.join(HERE, "gen_klein_terminal_gui.py")

# Java 里引用了别的类（FourDimensionalSpace.COLUMNS / CELL）时的已知取值
SEEDS = {"COLUMNS": 9, "ROWS": 6}

CHECK = [
    "IMAGE_W", "IMAGE_H", "CELL", "COLUMNS", "ROWS",
    "GRID_X", "GRID_Y", "GRID_W", "GRID_H", "GRID_BOTTOM",
    "SCROLLBAR_X", "SCROLLBAR_Y", "SCROLLBAR_W", "SCROLLBAR_H",
    "SEPARATOR_Y", "INV_LABEL_Y", "INV_Y", "HOTBAR_Y", "BOTTOM_PAD",
    "INFINITY_X", "INFINITY_Y", "INFINITY_W", "INFINITY_H",
    "SIDEBAR_X", "SIDEBAR_W", "CRAFT_LABEL_Y",
    "CRAFT_GRID_X", "CRAFT_GRID_Y", "CRAFT_RESULT_X", "CRAFT_RESULT_Y",
    "FURNACE_LABEL_Y",
    "FURNACE_INPUT_X", "FURNACE_INPUT_Y", "FURNACE_FUEL_X", "FURNACE_FUEL_Y",
    "FURNACE_RESULT_X", "FURNACE_RESULT_Y",
    "FLAME_X", "FLAME_Y", "FLAME_W", "FLAME_H",
    "FURNACE_ARROW_X", "FURNACE_ARROW_Y", "FURNACE_ARROW_W", "FURNACE_ARROW_H",
    "CRAFT_CARD_Y", "CRAFT_CARD_H",
    "FURNACE_CARD_Y", "FURNACE_CARD_H",
    "ANVIL_LABEL_Y", "ANVIL_CARD_Y", "ANVIL_CARD_H",
    "ANVIL_BASE_X", "ANVIL_BASE_Y", "ANVIL_MATERIAL_X", "ANVIL_MATERIAL_Y",
    "ANVIL_RESULT_X", "ANVIL_RESULT_Y",
    "ANVIL_NAME_X", "ANVIL_NAME_Y", "ANVIL_NAME_W", "ANVIL_NAME_H",
    "ANVIL_COST_X", "ANVIL_COST_Y",
]


def load_python_constants():
    spec = importlib.util.spec_from_file_location("gui", GUI_PY)
    module = importlib.util.module_from_spec(spec)
    saved = sys.stdout
    sys.stdout = io.StringIO()          # 生成器 import 时不打印
    try:
        spec.loader.exec_module(module)
    finally:
        sys.stdout = saved
    return module


def load_java_constants():
    source = open(LAYOUT_JAVA, encoding="utf-8").read()
    pairs = re.findall(r"public static final int (\w+) = ([^;]+);", source)
    values = dict(SEEDS)
    # CELL 是纯字面量，先取出来给后面的表达式用
    for name, expr in pairs:
        try:
            values.setdefault(name, int(expr.strip()))
        except ValueError:
            pass
    # 迭代解析：后面的常量可能引用前面的
    for _ in range(8):
        for name, expr in pairs:
            if name in values:
                continue
            try:
                values[name] = int(eval(expr.strip(), {"__builtins__": {}}, dict(values)))
            except Exception:
                pass
    # IMAGE_H / INFINITY_H 是在 static 块里算的，源码里没有 `= 值`
    try:
        values["IMAGE_H"] = values["HOTBAR_Y"] + values["CELL"] + values["BOTTOM_PAD"]
        values["INFINITY_H"] = values["IMAGE_H"] - values["BOTTOM_PAD"] - values["INFINITY_Y"]
    except KeyError:
        pass
    return values


def main():
    py = load_python_constants()
    java = load_java_constants()
    bad = []
    for name in CHECK:
        pv = getattr(py, name, "<Python 缺>")
        jv = java.get(name, "<Java 缺>")
        if pv != jv:
            bad.append((name, pv, jv))
    for name, pv, jv in bad:
        print(f"MISMATCH {name}: python={pv}  java={jv}")
    if bad:
        print(f"\n{bad.__len__()} 处不一致 —— 改 KleinTerminalLayout.java 的同时必须改 gen_klein_terminal_gui.py")
        return 1
    print(f"OK · {len(CHECK)} 个布局常量三处一致（IMAGE_H={java.get('IMAGE_H')}）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
