"""Зонд видимого ✕ у строки питомца LINE (TOWN-J1-1b1-2, спека «ДО СПАВНА» п. 2): снимок + сырой дамп того же кадра.

  python tools/line_close.py SHOT.png DUMP.xml DENSITY           -> CLOSE OK <n> | CLOSE FAIL <n>; exit 0 | 1
  python tools/line_close.py SHOT.png DUMP.xml DENSITY --nodes   -> NODES OK | NODES FAIL <правило>[; …] (приёмка п. 9, только дамп)
  python tools/line_close.py --selfcheck                          -> офлайн, по кадру emu_s12t_why13 (сборка без ✕); exit 0 | 1

DENSITY — px на dp (`tools/adbui.sh shell wm density`, последнее число / 160; эмулятор после wm360 — 3).
CLOSE: в квадрате 48 × 48 dp у правого верхнего угла узла resource-id «line» пикселей цвета ✕ (G.purpleDeep 310F53:
max(|R−49|, |G−15|, |B−83|) ≤ 30 и B − G ≥ 40 — тёмный текст G.ink сюда не попадает) ≥ 200.
NODES: у line clickable="true"; узлов внутри bounds line (включая край) с clickable, focusable или непустым content-desc
нет — у ✕ своей семантики нет (§ 2); узлов с «✕» в text/content-desc нет. «Одно действие „Закрыть“» дамп не показывает.
Сообщения — ASCII (консоль Windows).
"""
import os
import re
import sys
import tempfile

import numpy as np
from PIL import Image, ImageDraw

MIN_PX = 200


def parse(xml):
    out = []
    for n in re.findall(r"<node [^>]*>", xml):
        g = lambda k: (re.search(" " + k + r'="([^"]*)"', n) or [None, ""])[1]
        b = list(map(int, re.findall(r"-?\d+", g("bounds"))))
        if len(b) == 4:
            out.append(dict(rid=g("resource-id").split("/")[-1], text=g("text"), desc=g("content-desc"),
                            click=g("clickable") == "true", focus=g("focusable") == "true", b=b))
    return out


def line_of(ns):
    return next((n for n in ns if n["rid"] == "line"), None)


def close_px(shot, xml, d):
    """Pixels of the ✕ colour in the 48 × 48 dp top-right square of `line`; None — no `line` node."""
    ln = line_of(parse(xml))
    if ln is None:
        return None
    b, s = ln["b"], round(48 * d)
    a = np.asarray(Image.open(shot).convert("RGB").crop((b[2] - s, b[1], b[2], b[1] + s))).astype(int)
    near = (np.abs(a - (49, 15, 83)).max(axis=2) <= 30) & (a[..., 2] - a[..., 1] >= 40)
    return int(near.sum())


def nodes_fail(xml):
    ns = parse(xml)
    ln = line_of(ns)
    if ln is None:
        return ["no line"]
    L, fail = ln["b"], []
    if not ln["click"]:
        fail.append("line not clickable")
    inner = [n for n in ns if n is not ln and L[0] <= n["b"][0] and L[1] <= n["b"][1] and n["b"][2] <= L[2] and n["b"][3] <= L[3]
             and (n["click"] or n["focus"] or n["desc"])]
    if inner:
        fail.append(f"{len(inner)} actionable node(s) inside line: " + " ".join("[{},{}][{},{}]".format(*n["b"]) for n in inner))
    if any("✕" in n["text"] + n["desc"] for n in ns):
        fail.append("node with the cross glyph")
    return fail


def selfcheck():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    shot, dump = (os.path.join(root, "finny-pet", "screenshots", "emu_s12t_why13" + e) for e in (".png", ".xml"))
    if not (os.path.exists(shot) and os.path.exists(dump)):  # emu_* are git-ignored: the frame lives on the orchestrator's machine
        print(f"SELFCHECK FAIL: no {shot} or .xml")
        return 1
    d, xml = 3.0, open(dump, encoding="utf-8").read()
    b = line_of(parse(xml))["b"]
    # the mutant: § 2 geometry — circle F4F2F8 Ø 48 dp, cross ≈ 16 dp with 3 dp strokes of 310F53, centre 24 dp from the top-right
    im = Image.open(shot).convert("RGB")
    cx, cy, r, c = b[2] - 24 * d, b[1] + 24 * d, 24 * d, 8 * d
    dr = ImageDraw.Draw(im)
    dr.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(0xF4, 0xF2, 0xF8))
    for sx in (1, -1):
        dr.line((cx - c, cy - sx * c, cx + c, cy + sx * c), fill=(0x31, 0x0F, 0x53), width=round(3 * d))
    fd, mutant = tempfile.mkstemp(suffix=".png")  # %TEMP% on Windows
    os.close(fd)
    try:
        im.save(mutant)
        n0, n1 = close_px(shot, xml, d), close_px(mutant, xml, d)
    finally:
        os.remove(mutant)
    s = round(48 * d)  # a clickable 144 × 144 px node in the top-right corner of `line` — what a clickable ✕ would add
    extra = f'<node index="9" text="" resource-id="" content-desc="" clickable="true" focusable="true" bounds="[{b[2] - s},{b[1]}][{b[2]},{b[1] + s}]" />'
    bad_xml = xml.replace("</hierarchy>", extra + "</hierarchy>")
    cases = [
        ("кадр без ✕ (emu_s12t_why13)", f"CLOSE {'OK' if n0 >= MIN_PX else 'FAIL'} {n0}", "CLOSE FAIL 0"),
        ("тот же кадр с ✕ по § 2", f"CLOSE {'OK' if n1 >= MIN_PX else 'FAIL'}", "CLOSE OK"),
        ("дамп без ✕-узла", "NODES " + ("FAIL" if nodes_fail(xml) else "OK"), "NODES OK"),
        ("clickable 144 × 144 px в углу line", "NODES " + ("FAIL" if nodes_fail(bad_xml) else "OK"), "NODES FAIL"),
    ]
    bad = 0
    for name, got, want in cases:
        bad += got != want
        print(("ok  " if got == want else "BAD ") + f"{name}: {got}" + ("" if got == want else f"  (want: {want})"))
    print(f"(✕ pixels on the mutant: {n1}, threshold {MIN_PX})")
    print("SELFCHECK " + ("OK" if not bad else f"FAIL {bad}"))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.stdout.reconfigure(errors="replace")  # Cyrillic case names on a non-UTF-8 console
    a = sys.argv[1:]
    if a[:1] == ["--selfcheck"]:
        sys.exit(selfcheck())
    pos = [x for x in a if not x.startswith("--")]
    if len(pos) != 3 or any(x.startswith("--") and x not in ("--nodes",) for x in a):  # a typo must not relax the probe
        sys.exit(__doc__.splitlines()[2])
    shot, dump, d = pos[0], pos[1], float(pos[2])
    xml = open(dump, encoding="utf-8").read()
    if "--nodes" in a:
        f = nodes_fail(xml)
        print("NODES OK" if not f else "NODES FAIL " + "; ".join(f))
        sys.exit(1 if f else 0)
    n = close_px(shot, xml, d)
    if n is None:
        print("CLOSE FAIL: no line")
        sys.exit(1)
    print(("CLOSE OK " if n >= MIN_PX else "CLOSE FAIL ") + str(n))
    sys.exit(0 if n >= MIN_PX else 1)
