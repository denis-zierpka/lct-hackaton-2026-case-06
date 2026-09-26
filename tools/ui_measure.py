"""Замер экрана по uiautomator-дампу (TOWN-S0c): python tools/ui_measure.py <dump.xml> <label> <density>
Дамп: tools/adbui.sh shell uiautomator dump /sdcard/ui.xml; tools/adbui.sh exec-out cat /sdcard/ui.xml > dump.xml
density — px на dp (эмулятор wm360 и S23 — 3). Нужен Modifier.semantics { testTagsAsResourceId = true } на корне.
Печатает JSON: экран в dp, bounds тегов (dp), кликабельные < 48 dp, клетки cell_*, узел overflow («Обрезано: N»),
узлы за краем экрана. Зонд сначала проверяют на заведомо плохом (узкий экран) — правило WORKFLOW №14."""
import re, sys, json

path, label, dens = sys.argv[1], sys.argv[2], float(sys.argv[3])
xml = open(path, encoding="utf-8").read()
nodes = []
for n in re.findall(r"<node [^>]*>", xml):
    g = lambda k: (re.search(k + r'="([^"]*)"', n) or [None, ""])[1]
    b = list(map(int, re.findall(r"-?\d+", g("bounds"))))
    if len(b) != 4:
        continue
    nodes.append(dict(rid=g("resource-id"), text=g("text"), desc=g("content-desc"), click=g("clickable") == "true",
                      x0=b[0], y0=b[1], x1=b[2], y1=b[3]))
if not nodes:
    sys.exit("NO NODES — dump empty")
root = max(nodes, key=lambda n: (n["x1"] - n["x0"]) * (n["y1"] - n["y0"]))
W = root["x1"] - root["x0"]; H = root["y1"] - root["y0"]
dp = lambda px: round(px / dens, 1)
out = {"label": label, "screen_dp": [dp(W), dp(H)], "rows": {}, "small_targets": [], "cells": [], "overflow": None, "offscreen": []}
for n in nodes:
    rid = n["rid"].split("/")[-1]
    w, h = n["x1"] - n["x0"], n["y1"] - n["y0"]
    if rid == "overflow":
        out["overflow"] = n["desc"]
    elif rid.startswith("cell_"):
        out["cells"].append((rid, dp(w), dp(h)))
    elif rid:
        out["rows"][rid] = dict(x=dp(n["x0"]), y=dp(n["y0"]), w=dp(w), h=dp(h))
    if n["click"] and (w < 48 * dens - 1 or h < 48 * dens - 1):
        out["small_targets"].append((n["text"] or n["desc"] or rid, dp(w), dp(h)))
    if n["x0"] < root["x0"] - 1 or n["x1"] > root["x1"] + 1:
        out["offscreen"].append((n["text"] or n["desc"] or rid, dp(n["x0"]), dp(n["x1"])))
if out["cells"]:
    ws = [c[1] for c in out["cells"]]; hs = [c[2] for c in out["cells"]]
    out["cells"] = dict(count=len(ws), min_w=min(ws), min_h=min(hs), max_w=max(ws))
print(json.dumps(out, ensure_ascii=False, indent=1))
