"""Приёмочные проверки арта (TOWN-A1b и дальше): регрессия кадра дампом сцены, композиты «как увидит ребёнок»
с контрастом текста на фоне, альфа-bbox спрайтов, палитра жителей. Запуск из корня репозитория:

  python tools/art_check.py regress SRC_DIR OUT_DIR   дампы сцены room.py (room_port_day) и pet.py (bunny, cat, puppy ×
                                                      stage 0, 2) генераторами из SRC_DIR -> OUT_DIR/*.json
  python tools/art_check.py diff A_DIR B_DIR          сравнить дампы; exit 1, если есть разница или нет файла
  python tools/art_check.py bg BG.png OUT_DIR         композиты UI со снимков на фон места + контраст G.ink под текстом;
                                                      exit 1, если среднее < 4,5 : 1 или 10-й перцентиль < 3 : 1
  python tools/art_check.py bbox PNG [PNG …] [--ref x0,y0,x1,y1] [--tol 3] [--margin 2]
  python tools/art_check.py palette                   RESIDENT_COLORS из pet.py против town.residents и палитры питомца

Дамп пишет этот же файл, запущенный внутри Blender ПОСЛЕ генератора: --python tools/art_check.py с env DUMP_OUT.
Снимки для композитов — finny-pet/screenshots/emu_*.png (в .gitignore, лежат на машине команды).
Blender — env BLENDER, иначе путь установки Windows (HANDOFF, «Окружение»)."""
import json, math, os, subprocess, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FP = os.path.join(ROOT, "finny-pet")
INK = (0x1C, 0x1D, 0x22)  # G.ink
# Текст прямо на фоне (360 × 640 dp = 1080 × 1920 px): (снимок, подпись, x0, y0, x1, y1). Статус-бар — полоса 0–5 %.
TEXT = [("emu_demo05a_market.png", "HUD-2 «Не разложено»", 20, 318, 500, 372),
        ("emu_demo06b_order.png", "HUD-2 отделения", 25, 290, 525, 402),
        ("emu_s1d3_job13.png", "заголовок «Пекарня» (1,3)", 20, 452, 385, 530)]
SNAPS = ["emu_demo05a_market.png", "emu_demo06b_order.png", "emu_demo06c_order.png", "emu_s1d3_job13.png"]
S23 = (1.219, 118)  # S23 360 × 780 dp: фон ×1,219 (Crop по высоте), по бокам срезается по 118 px


def blender():
    return os.environ.get("BLENDER") or "C:/Program Files/Blender Foundation/Blender 5.2/blender.exe"


def dump():
    import bpy
    r = lambda v, n=4: [round(x, n) for x in v]

    def mat(m):
        if m is None: return None
        d = {"name": m.name}
        if m.use_nodes and "Principled BSDF" in m.node_tree.nodes:
            p = m.node_tree.nodes["Principled BSDF"].inputs
            for k in ("Base Color", "Roughness", "Emission Strength", "Alpha", "Metallic", "Transmission Weight", "Coat Weight", "Subsurface Weight"):
                if k in p:
                    v = p[k].default_value
                    d[k] = r(v) if hasattr(v, "__len__") else round(v, 4)
        return d

    dg = bpy.context.evaluated_depsgraph_get()
    out = {}
    for o in sorted(bpy.context.scene.objects, key=lambda o: o.name):
        e = {"type": o.type, "loc": r(o.matrix_world.translation), "rot": r(o.rotation_euler), "scale": r(o.scale),
             "dims": r(o.evaluated_get(dg).dimensions), "parent": o.parent.name if o.parent else None,
             "mats": [mat(s.material) for s in o.material_slots], "shadow": getattr(o, "visible_shadow", None)}
        if o.type == "LIGHT":
            L = o.data; e["light"] = {"kind": L.type, "energy": round(L.energy, 4), "color": r(L.color), "size": round(getattr(L, "size", 0), 4)}
        if o.type == "CAMERA":
            e["cam"] = {"lens": round(o.data.lens, 4), "kind": o.data.type, "ortho": round(o.data.ortho_scale, 4)}
        if o.type == "FONT":
            e["text"] = o.data.body
        out[o.name] = e
    s = bpy.context.scene
    if s.world and s.world.use_nodes:
        bg = s.world.node_tree.nodes["Background"].inputs
        out["__world__"] = {"color": r(bg[0].default_value), "strength": round(bg[1].default_value, 4)}
    out["__view__"] = {"film_transparent": s.render.film_transparent, "exposure": round(s.view_settings.exposure, 4),
                       "view": s.view_settings.view_transform, "color_mode": s.render.image_settings.color_mode,
                       "aspect": round(s.render.resolution_x / s.render.resolution_y, 4)}
    json.dump(out, open(os.environ["DUMP_OUT"], "w", encoding="utf-8"), indent=1, sort_keys=True, ensure_ascii=False)
    print("DUMPED", len(out), os.environ["DUMP_OUT"], flush=True)


def regress(src, out):
    os.makedirs(out, exist_ok=True)
    me = os.path.abspath(__file__)
    runs = [("room_port_day", "room.py", ["--only", "room_port_day", "--out", os.path.join(out, "room.png"), "--preview"])]
    runs += [(f"pet_{sp}_{st}", "pet.py", ["--species", sp, "--stage", str(st), "--size", "64", "--samples", "1",
                                           "--out", os.path.join(out, f"pet_{sp}_{st}.png")])
             for sp in ("bunny", "cat", "puppy") for st in (0, 2)]
    for name, script, args in runs:
        env = dict(os.environ, DUMP_OUT=os.path.join(out, name + ".json"))
        cmd = [blender(), "-b", "--factory-startup", "--python-exit-code", "1", "-P", os.path.join(src, script), "--python", me, "--"] + args
        p = subprocess.run(cmd, env=env, capture_output=True, text=True, encoding="utf-8", errors="replace")
        if p.returncode != 0 or "DUMPED" not in p.stdout:
            print(name, "FAIL exit", p.returncode, "\n", (p.stdout + p.stderr)[-2000:]); return 1
        print(name, "ok")
    return 0


def diff(a, b):
    import glob
    bad = 0
    files = sorted(glob.glob(os.path.join(a, "*.json")))
    if not files: print("нет дампов в", a); return 1
    for fa in files:
        fb = os.path.join(b, os.path.basename(fa))
        if not os.path.exists(fb): print("MISSING", fb); bad += 1; continue
        A, B = json.load(open(fa, encoding="utf-8")), json.load(open(fb, encoding="utf-8"))
        d = sorted(k for k in set(A) | set(B) if A.get(k) != B.get(k))
        print(os.path.basename(fa), len(A), "объектов, разница:", len(d), d[:12]); bad += bool(d)
        for k in d[:3]:
            print("   ", k, "\n     A:", json.dumps(A.get(k), ensure_ascii=False)[:300], "\n     B:", json.dumps(B.get(k), ensure_ascii=False)[:300])
    print("DUMP DIFF EMPTY" if not bad else f"DUMP DIFF: {bad} file(s)")
    return 1 if bad else 0


def lum(rgb):
    """WCAG relative luminance of an sRGB (0–255) array/tuple, per pixel."""
    import numpy as np
    c = np.asarray(rgb, dtype=float) / 255
    c = np.where(c <= 0.03928, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)
    return c[..., 0] * 0.2126 + c[..., 1] * 0.7152 + c[..., 2] * 0.0722


def bg(path, out):
    import numpy as np
    from PIL import Image
    from scipy import ndimage
    os.makedirs(out, exist_ok=True)
    B = np.asarray(Image.open(path).convert("RGB")).astype(int)
    if B.shape[:2] != (1920, 1080): print("фон не 1080 × 1920:", B.shape); return 1
    room = np.asarray(Image.open(os.path.join(FP, "app/src/game/res/drawable-nodpi/room_port_day.webp")).convert("RGB")).astype(int)
    for s in SNAPS:
        S = np.asarray(Image.open(os.path.join(FP, "screenshots", s)).convert("RGB")).astype(int)
        m = np.abs(S - room).max(axis=2) < 12
        # белое в карточке совпадает с белым окном комнаты: тонкие совпадения убрать, замкнутые UI дыры залить
        m = ~ndimage.binary_fill_holes(~ndimage.binary_opening(m, iterations=2))
        Image.fromarray(np.where(m[..., None], B, S).astype("uint8")).save(os.path.join(out, "comp_" + s))
    Lk = float(lum(INK))
    Lb = lum(B)
    fails = 0
    print(f"{'где':34} {'экран':8} {'среднее':>8} {'p10':>6}")
    rows = [("статус-бар 0–5 %", 0, 0, 1080, 96)] + [(cap, x0, y0, x1, y1) for _, cap, x0, y0, x1, y1 in TEXT]
    for cap, x0, y0, x1, y1 in rows:
        for scr, (k, dx) in (("360×640", (1.0, 0)), ("S23", S23)):
            X0, X1 = max(0, round((x0 + dx) / k)), min(1080, round((x1 + dx) / k))
            Y0, Y1 = round(y0 / k), round(y1 / k)
            L = Lb[Y0:Y1, X0:X1]
            mean = (L.mean() + 0.05) / (Lk + 0.05); p10 = (np.percentile(L, 10) + 0.05) / (Lk + 0.05)
            ok = mean >= 4.5 and p10 >= 3.0; fails += not ok
            print(f"{cap:34} {scr:8} {mean:8.2f} {p10:6.2f}  {'OK' if ok else 'FAIL'}")
    print("CONTRAST OK" if not fails else f"CONTRAST FAIL: {fails}")
    return 1 if fails else 0


def bbox(args):
    from PIL import Image
    ref, tol, margin, files = None, 3, 2, []
    it = iter(args)
    for a in it:
        if a == "--ref": ref = [int(v) for v in next(it).split(",")]
        elif a == "--tol": tol = int(next(it))
        elif a == "--margin": margin = int(next(it))
        else: files.append(a)
    bad = 0
    for f in files:
        im = Image.open(f)
        if im.mode != "RGBA": print(f, "mode", im.mode, "FAIL (нужен RGBA)"); bad += 1; continue
        a = im.split()[-1]; bb = a.getbbox(); w, h = im.size
        corners = [a.getpixel(p) for p in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1))]
        ok = bb is not None and min(bb[0], bb[1], w - bb[2], h - bb[3]) >= margin and max(corners) == 0
        if ref and bb: ok = ok and all(abs(bb[i] - ref[i]) <= tol for i in range(4))
        bad += not ok
        print(f"{os.path.basename(f)} {w}x{h} bbox {bb} углы α {corners} {'OK' if ok else 'FAIL'}")
    return 1 if bad else 0


def lab(h):
    c = [int(h.lstrip("#")[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    c = [x / 12.92 if x <= 0.04045 else ((x + 0.055) / 1.055) ** 2.4 for x in c]
    X = (0.4124 * c[0] + 0.3576 * c[1] + 0.1805 * c[2]) / 0.95047
    Y = 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]
    Z = (0.0193 * c[0] + 0.1192 * c[1] + 0.9505 * c[2]) / 1.08883
    f = lambda t: t ** (1 / 3) if t > 0.008856 else 7.787 * t + 16 / 116
    return (116 * f(Y) - 16, 500 * (f(X) - f(Y)), 200 * (f(Y) - f(Z)))


def palette():
    import ast
    tree = ast.parse(open(os.path.join(FP, "tools/art/pet.py"), encoding="utf-8").read())
    val = {t.id: ast.literal_eval(n.value) for n in tree.body if isinstance(n, ast.Assign)
           for t in n.targets if isinstance(t, ast.Name) and t.id in ("COLORS", "RESIDENT_COLORS")}
    if "RESIDENT_COLORS" not in val: print("RESIDENT_COLORS в pet.py нет (литерал на уровне модуля)"); return 1
    res = json.load(open(os.path.join(FP, "app/src/main/assets/content/content.json"), encoding="utf-8"))["town"]["residents"]
    R, P = val["RESIDENT_COLORS"], val["COLORS"]
    ids = [r["id"] for r in res]; bad = 0
    if sorted(R) != sorted(ids): print("id не совпадают с town.residents:", sorted(set(R) ^ set(ids))); bad += 1
    sp = {r["id"]: r["look"]["species"] for r in res}
    for i in ids:
        if i not in R: continue
        d = min(math.dist(lab(R[i]), lab(p)) for p in P.values())
        mates = [(j, math.dist(lab(R[i]), lab(R[j]))) for j in ids if j != i and j in R and sp[j] == sp[i]]
        dm = min((v for _, v in mates), default=99)
        ok = d >= 20 and dm >= 20; bad += not ok
        print(f"{i:7} {sp[i]:6} {R[i]}  ΔE до питомца {d:5.1f}  до жителя того же вида {dm:5.1f}  {'OK' if ok else 'FAIL'}")
    print("PALETTE OK" if not bad else "PALETTE FAIL")
    return 1 if bad else 0


if __name__ == "__main__":
    if os.environ.get("DUMP_OUT") and "bpy" in sys.modules:  # внутри Blender после генератора
        dump()
    else:
        cmd, rest = sys.argv[1], sys.argv[2:]
        sys.exit({"regress": lambda: regress(*rest), "diff": lambda: diff(*rest), "bg": lambda: bg(*rest),
                  "bbox": lambda: bbox(rest), "palette": palette}[cmd]())
