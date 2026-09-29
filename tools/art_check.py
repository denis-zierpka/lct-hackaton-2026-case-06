"""Приёмочные проверки арта (TOWN-A1b и дальше): регрессия кадра дампом сцены, композиты «как увидит ребёнок»
с контрастом текста на фоне, альфа-bbox спрайтов, палитра жителей. Запуск из корня репозитория:

  python tools/art_check.py regress SRC_DIR OUT_DIR   дампы сцены room.py (room_port_day, room_port_evening, 7 спрайтов мебели
                                                      furn_<id> по одному -> room_furn_<id>), pet.py (bunny, cat, puppy ×
                                                      stage 0, 2), place.py (market, foma, bakery), facade.py (7 фасадов по одному
                                                      и фон улицы --street -> street)
                                                      и props.py (tile_apple, goal_custom, ui_coin — по одному на
                                                      прогон) генераторами из SRC_DIR -> OUT_DIR/*.json
  python tools/art_check.py diff A_DIR B_DIR          сравнить дампы; exit 1, если есть разница или нет файла
  python tools/art_check.py bg BG OUT_DIR [--veil A]  композиты UI со снимков на фон места + контраст G.ink под текстом
                                                      этого места (bg_<place>_port; иной файл — все тексты); exit 1,
                                                      если среднее < 4,5 : 1, 10-й перцентиль < 3 : 1 или пересвет
                                                      (R и G ≥ 250) в полосах 0–31 % / 80–100 % > 5 %
  python tools/art_check.py bg - - --selfcheck        самопроверка маски: белые карточки над белым окном комнаты не
                                                      уходят фону (≤ 50 px), а сырая маска их отдаёт (≥ 1000 px)
  python tools/art_check.py bbox PNG [PNG …] [--ref x0,y0,x1,y1] [--tol 3] [--margin 2]
  python tools/art_check.py palette                   RESIDENT_COLORS из pet.py против town.residents и палитры питомца
  python tools/art_check.py which SHOT … [--expect N] [--bgdir D]  какой фон под снимком (360 × 640 или S23): по полосе
                                                      статус-бара; D — фоны bg_*_port.webp, которых ещё нет в res
  python tools/art_check.py tiles BG [--veil A]       ΔE плиток Match3 до фона места под подложкой поля (p5 ≥ 15)
  python tools/art_check.py residents [--selfcheck]   id town.residents = ветки residentRes в TownUi.kt = res_*.webp в
                                                      game/res, else -> null (TOWN-A1f); --selfcheck — 4 мутанта дают
                                                      MISMATCH
  python tools/art_check.py pastries [--selfcheck]    id меню работ TRAY (town.jobs) = ветки pastryRes в TownUi.kt =
                                                      pastry_*.webp в game/res, else -> null (TOWN-J1-1b4); --selfcheck —
                                                      эталон из content.json и 10 мутантов, каждый со своей причиной
                                                      (TownUi.kt не читает)

Дамп пишет этот же файл, запущенный внутри Blender ПОСЛЕ генератора: --python tools/art_check.py с env DUMP_OUT.
Снимки для композитов — finny-pet/screenshots/emu_*.png (в .gitignore, лежат на машине команды).
Blender — env BLENDER, иначе путь установки Windows (HANDOFF, «Окружение»)."""
import json, math, os, subprocess, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FP = os.path.join(ROOT, "finny-pet")
INK = (0x1C, 0x1D, 0x22)  # G.ink
# Текст прямо на фоне (360 × 640 dp = 1080 × 1920 px), замер по снимкам сборки BASE TOWN-A1c (emu_b_*, 2026-09-27;
# тёмные пиксели ± 8 px): (снимок, подпись, x0, y0, x1, y1, места с этим текстом). Статус-бар — полоса 0–5 %, у всех.
SHOPS = ("market", "foma")
TEXT = [("emu_b_market10.png", "HUD-2 «Не разложено» (1,0)", 20, 317, 500, 375, SHOPS),
        ("emu_b_market13.png", "HUD-2 «Не разложено» (1,3)", 22, 313, 621, 383, SHOPS),
        ("emu_b_marketp10.png", "HUD-2 отделения (1,0)", 26, 289, 525, 405, SHOPS),
        ("emu_b_marketp13.png", "HUD-2 отделения (1,3)", 84, 312, 512, 374, SHOPS),
        ("emu_b_job10.png", "заголовок «Пекарня» (1,0)", 21, 448, 340, 522, ("bakery",)),
        ("emu_b_job13.png", "заголовок «Пекарня» (1,3)", 22, 451, 384, 532, ("bakery",)),
        ("emu_b_taps10.png", "заголовок «Помочь Марте» (1,0)", 21, 448, 553, 521, ("market",)),
        ("emu_b_taps13.png", "заголовок «Помочь Марте» (1,3)", 22, 451, 627, 532, ("market",))]
SNAPS = ["emu_b_market10.png", "emu_b_market13.png", "emu_b_marketp10.png", "emu_b_foma10.png", "emu_b_event10.png",
         "emu_b_order10.png", "emu_b_taps10.png", "emu_b_taps13.png", "emu_b_tresult10.png", "emu_b_job10.png",
         "emu_b_job13.png", "emu_b_round10.png", "emu_b_round13.png", "emu_b_result10.png"]
VEIL = 0.18  # подложка поля Match3: White α 0,18 (MiniGameScreen.kt), фон под ней = фон·0,82 + белый·0,18
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
             "mats": [mat(s.material) for s in o.material_slots], "shadow": getattr(o, "visible_shadow", None),
             # bevel and subdivision do not move the bbox: without them a changed bevel dumped «no change» (TOWN-A1c)
             "mods": [{k: (round(v, 4) if isinstance(v, float) else v) for k in ("type", "width", "segments", "levels", "render_levels", "voxel_size")
                       for v in [getattr(m, k, None)] if v is not None} for m in o.modifiers]}
        if o.type == "CURVE":
            e["curve"] = {"bevel": round(o.data.bevel_depth, 4), "res": o.data.bevel_resolution}
        if o.type == "LIGHT":
            L = o.data; e["light"] = {"kind": L.type, "energy": round(L.energy, 4), "color": r(L.color), "size": round(getattr(L, "size", 0), 4),
                                      "angle": round(getattr(L, "angle", 0), 4)}
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
    runs += [("room_port_evening", "room.py", ["--only", "room_port_evening", "--out", os.path.join(out, "room_e.png"), "--preview"])]  # A1d1: the evening shell
    # all 7 furniture sprites, one per run (a run dumps its last scene only): the room camera and light for A1e stay under regress
    runs += [(f"room_furn_{n}", "room.py", ["--only", f"furn_{n}", "--samples", "1", "--out", os.path.join(out, f"room_furn_{n}.png")])
             for n in ("window", "shelf", "fridge", "door", "bed", "mailbox", "chest")]
    runs += [(f"pet_{sp}_{st}", "pet.py", ["--species", sp, "--stage", str(st), "--size", "64", "--samples", "1",
                                           "--out", os.path.join(out, f"pet_{sp}_{st}.png")])
             for sp in ("bunny", "cat", "puppy") for st in (0, 2)]
    # approved at gate № 38: the market and «У Фомы» backgrounds and the market facade (facade.py reads place.PLACES)
    # + the bakery (gate № 40): the 1b3 counter overlay is cut from this scene (TOWN-J1-1b3, pilot № 61 б)
    runs += [(f"place_{p}", "place.py", ["--place", p, "--out", os.path.join(out, f"place_{p}.png"), "--preview"]) for p in ("market", "foma", "bakery")]
    # all 7 facades, one per run (a run dumps its last scene only): the street generator A1g1 changes some (pilot № 61 б)
    runs += [(f"facade_{f}", "facade.py", ["--place", f, "--samples", "1", "--out", os.path.join(out, f"facade_{f}.png")])
             for f in ("home", "market", "foma", "bakery", "gate_park", "gate_forest", "gate_zoo")]
    # the street background bg_street_port (A1g1): the same flags as its acceptance (TOWN-A1g1 п. 8)
    runs += [("street", "facade.py", ["--street", os.path.join(out, "street.png"), "--preview"])]
    # pastries go into props.py (1b3): three builders, one per run — render_prop resets the scene per prop, a joint run dumps the last only
    runs += [(f"props_{n}", "props.py", ["--only", n, "--samples", "1", "--out", os.path.join(out, f"props_{n}.png")]) for n in ("tile_apple", "goal_custom", "ui_coin")]
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


def ui_mask(S, room, clean=True):
    """Where a snapshot shows the room (plain, m1, or under the Match3 underlay VEIL). A white card over the white window
    of the room matches it too (the A1b leak): thin matches are opened away, small enclosed holes of the UI are filled
    (large ones are background in a frame, e.g. the whole Match3 field). Returns (m1, mask); clean=False — the raw mask."""
    from scipy import ndimage
    import numpy as np
    m1 = np.abs(S - room).max(axis=2) < 12
    m2 = np.abs(S - (room * (1 - VEIL) + 255 * VEIL)).max(axis=2) < 12
    if not clean:
        return m1, m1 | m2
    m = ndimage.binary_opening(m1 | m2, iterations=2)
    holes, n = ndimage.label(ndimage.binary_fill_holes(~m) & m)
    return m1, m & ~np.isin(holes, 1 + np.flatnonzero(ndimage.sum(np.ones_like(holes), holes, range(1, n + 1)) < 60000))


# Snapshots whose white window of the room lies wholly under cards: every near-white pixel the mask gives away is a leak
LEAK_SNAPS = ["emu_b_market10.png", "emu_b_marketp10.png", "emu_b_foma10.png", "emu_b_order10.png", "emu_b_job10.png",
              "emu_b_event10.png"]


def selfcheck(room):
    """The mask on a magenta background: near-white snapshot pixels (min ≥ 245) given to the background must be ≤ 50 per
    snapshot with the working mask and ≥ 1000 with the raw one (the check can go red: that is the A1b leak)."""
    import numpy as np
    from PIL import Image
    bad = 0
    for s in LEAK_SNAPS:
        S = np.asarray(Image.open(os.path.join(FP, "screenshots", s)).convert("RGB")).astype(int)
        white = S.min(axis=2) >= 245
        n = int((white & ui_mask(S, room)[1]).sum()); raw = int((white & ui_mask(S, room, clean=False)[1]).sum())
        ok = n <= 50 and raw >= 1000; bad += not ok
        print(f"{s}: белых отдано фону {n} (≤ 50), сырой маской {raw} (≥ 1000)  {'OK' if ok else 'FAIL'}")
    print("SELFCHECK OK" if not bad else "SELFCHECK FAIL")
    return 1 if bad else 0


def bg(path, out, *opt):
    """opt: --veil A — alpha of the Match3 field underlay drawn on the composite (the snapshots carry VEIL);
    --selfcheck — only the leak self-check of the mask (path and out are ignored)."""
    import numpy as np
    from PIL import Image
    veil = float(opt[opt.index("--veil") + 1]) if "--veil" in opt else VEIL
    room = np.asarray(Image.open(os.path.join(FP, "app/src/game/res/drawable-nodpi/room_port_day.webp")).convert("RGB")).astype(int)
    if "--selfcheck" in opt:
        return selfcheck(room)
    os.makedirs(out, exist_ok=True)
    B = np.asarray(Image.open(path).convert("RGB")).astype(int)
    if B.shape[:2] != (1920, 1080): print("фон не 1080 × 1920:", B.shape); return 1
    for s in SNAPS:
        S = np.asarray(Image.open(os.path.join(FP, "screenshots", s)).convert("RGB")).astype(int)
        m1, m = ui_mask(S, room)
        C = np.where((m & ~m1)[..., None], B * (1 - veil) + 255 * veil, np.where(m[..., None], B, S))
        Image.fromarray(C.astype("uint8")).save(os.path.join(out, "comp_" + s))
    Lk = float(lum(INK))
    Lb = lum(B)
    fails = 0
    print(f"{'где':34} {'экран':8} {'среднее':>8} {'p10':>6}")
    # a place background carries only its own texts (bg_<place>_port); any other file (room_port_day) — all of them
    import re
    pm = re.search(r"bg_(\w+?)_port", os.path.basename(path))
    rows = [("статус-бар 0–5 %", 0, 0, 1080, 96)] + [(cap, x0, y0, x1, y1) for _, cap, x0, y0, x1, y1, places in TEXT
                                                     if pm is None or pm.group(1) in places]
    for cap, x0, y0, x1, y1 in rows:
        for scr, (k, dx) in (("360×640", (1.0, 0)), ("S23", S23)):
            X0, X1 = max(0, round((x0 + dx) / k)), min(1080, round((x1 + dx) / k))
            Y0, Y1 = round(y0 / k), round(y1 / k)
            L = Lb[Y0:Y1, X0:X1]
            mean = (L.mean() + 0.05) / (Lk + 0.05); p10 = (np.percentile(L, 10) + 0.05) / (Lk + 0.05)
            ok = mean >= 4.5 and p10 >= 3.0; fails += not ok
            print(f"{cap:34} {scr:8} {mean:8.2f} {p10:6.2f}  {'OK' if ok else 'FAIL'}")
    for a, b in ((0, 31), (80, 100)):  # пересвет: доля пикселей с R и G ≥ 250 (у room_port_day 0,00)
        band = B[1920 * a // 100:1920 * b // 100]
        clip = float(((band[..., 0] >= 250) & (band[..., 1] >= 250)).mean()); ok = clip <= 0.05; fails += not ok
        print(f"пересвет R,G ≥ 250 на {a}–{b} %: {clip:.3f} (≤ 0,05)  {'OK' if ok else 'FAIL'}")
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


def backdrops(extra=None):
    """Full-screen backgrounds of the app: the room (day, evening) and the places, as (name, 1920 × 1080 × 3 int array).
    extra — a directory with bg_*_port.webp that are not in res yet (they replace the ones of res with the same name)."""
    import glob
    import numpy as np
    from PIL import Image
    res = os.path.join(FP, "app/src/game/res/drawable-nodpi")
    places = {os.path.basename(f): f for d in (res, extra) if d for f in sorted(glob.glob(os.path.join(d, "bg_*_port.webp")))}
    files = [os.path.join(res, f"room_port_{t}.webp") for t in ("day", "evening")] + sorted(places.values())
    return [(os.path.basename(f)[:-5], np.asarray(Image.open(f).convert("RGB")).astype(int)) for f in files]


def which(args):
    """Which background is under a screenshot: the status-bar band (0–4 % of the height — the only band where the
    background shows on every screen) against every backdrop, fitted like ContentScale.Crop (360 × 640: 1:1; S23
    1080 × 2340: ×1,219, 118 px cut at each side). Share of band pixels with max|Δ| < 12: the best ≥ 0,6 and the
    second < 0,3, else «none» (a crossfade or a fade in progress). --expect NAME: exit 1 unless every shot is NAME."""
    import numpy as np
    from PIL import Image
    expect = args[args.index("--expect") + 1] if "--expect" in args else None
    shots = [a for a in args if a.lower().endswith((".png", ".jpg"))]
    cands, bad = backdrops(args[args.index("--bgdir") + 1] if "--bgdir" in args else None), 0
    for f in shots:
        S = np.asarray(Image.open(f).convert("RGB")).astype(int); H, W = S.shape[:2]
        k = max(W / 1080, H / 1920); band = round(H * 0.04)
        dx, dy = round((1080 * k - W) / 2), round((1920 * k - H) / 2)
        scores = []
        for name, B in cands:
            Bk = B if k == 1 else np.asarray(Image.fromarray(B.astype("uint8")).resize((round(1080 * k), round(1920 * k)), Image.BILINEAR)).astype(int)
            scores.append((float((np.abs(S[:band] - Bk[dy:dy + band, dx:dx + W]).max(axis=2) < 12).mean()), name))
        scores.sort(reverse=True)
        (s1, n1), (s2, n2) = scores[0], scores[1]
        got = n1 if s1 >= 0.6 and s2 < 0.3 else "none"
        ok = expect is None or got == expect; bad += not ok
        print(f"{os.path.basename(f)} {W}x{H}: {got}  ({n1} {s1:.2f}, {n2} {s2:.2f}){'' if ok else '  FAIL, expected ' + expect}")
    return 1 if bad else 0


def tiles(path, *opt):
    """How far each Match3 tile is from the place background under the field underlay: ΔE76 between the tile's mean
    colour (α > 200) and every background pixel of the field zone (x 3–97 %, y 25–80 %), 5th percentile ≥ 15: below the
    palette's 20 (residents) — a tile is set apart from the background by its shading, rim and the underlay too; on the
    BASE bakery 19,1–40,9, and the A1b judges read the tiles there."""
    import glob
    import numpy as np
    from PIL import Image
    veil = float(opt[opt.index("--veil") + 1]) if "--veil" in opt else VEIL
    B = np.asarray(Image.open(path).convert("RGB")).astype(float)
    zone = (B[round(1920 * .25):round(1920 * .80), round(1080 * .03):round(1080 * .97)] * (1 - veil) + 255 * veil).reshape(-1, 3)
    labs = np.array([lab("#%02x%02x%02x" % tuple(int(v) for v in p)) for p in zone[::97]])  # a sample is enough for a percentile
    bad = 0
    for f in sorted(glob.glob(os.path.join(FP, "app/src/game/res/drawable-nodpi/tile_*.webp"))):
        if "bomb" in f: continue  # the bomb is a button, not a board tile (Match3 has five kinds)
        T = np.asarray(Image.open(f).convert("RGBA")).astype(float)
        mean = T[T[..., 3] > 200][:, :3].mean(axis=0)
        d = np.percentile(np.linalg.norm(labs - np.array(lab("#%02x%02x%02x" % tuple(int(v) for v in mean))), axis=1), 5)
        ok = d >= 15; bad += not ok
        print(f"{os.path.basename(f):16} ΔE p5 {d:5.1f}  {'OK' if ok else 'FAIL'}")
    print("TILES OK" if not bad else "TILES FAIL")
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


def when_check(fn, prefix, ids, files, text):
    """id = ветки (id слева во всех парах `"<id>" -> R.drawable.<prefix><x>`, x ≠ id — «чужой ресурс») в
    `fun <fn>(id: String): Int? = when (id) {…\n}` = файлы, последняя ветка else -> null (общий разбор residents и pastries). Печатает строку с причинами, возвращает (ok, строка)."""
    import re
    body = re.search(r"fun %s\(id: String\): Int\? = when \(id\) \{(.*?)\n\}" % re.escape(fn), text, re.S)
    # commented-out branches are not branches; an unknown id must fall back to the default picture (else -> null)
    code = re.sub(r"//[^\n]*|/\*.*?\*/", "", body.group(1), flags=re.S) if body else ""
    pairs = re.findall(r'"(\w+)"\s*->\s*R\.drawable\.%s(\w+)' % re.escape(prefix), code)
    br = {a for a, b in pairs}; bad = [(a, b) for a, b in pairs if a != b]  # br — все ветки: иначе not bad следует из ids == br и len
    dup = sorted({a for a in br if pairs.count((a, a)) > 1})  # len(pairs) == len(ids) catches it; the reason is printed here
    tail = bool(re.search(r"\n\s*else\s*->\s*null\s*$", code))
    ok = bool(ids) and ids == br == files and not bad and len(pairs) == len(ids) and tail
    line = (f"id {len(ids)}, ветки {len(br)}, файлы {len(files)}, чужой ресурс {bad}, else -> null {tail}; нет ветки "
            f"{sorted(ids - br)}, нет файла {sorted(ids - files)}, лишние {sorted((br | files) - ids)}, дубли {dup} -> "
            f"{'MATCH %d' % len(ids) if ok else 'MISMATCH'}")
    print(line)
    return ok, line


def residents(args):
    """id town.residents = ветки `"<id>" -> R.drawable.res_<id>` в residentRes (TownUi.kt) = файлы res_*.webp (TOWN-A1f)."""
    import re
    src = open(os.path.join(FP, "app/src/game/java/ru/finny/pet/game/ui/TownUi.kt"), encoding="utf-8").read()
    ids = {r["id"] for r in json.load(open(os.path.join(FP, "app/src/main/assets/content/content.json"), encoding="utf-8"))["town"]["residents"]}
    files = {f[4:-5] for f in os.listdir(os.path.join(FP, "app/src/game/res/drawable-nodpi")) if f.startswith("res_") and f.endswith(".webp")}
    check = lambda text: when_check("residentRes", "res_", ids, files, text)[0]
    if "--selfcheck" in args:  # мутанты обязаны дать MISMATCH: без ветки Марты, Марта с ресурсом Фомы, ветка в /* */,
        # неизвестный житель рисуется Мартой
        m = [re.sub(r'\n\s*"marta"\s*->\s*R\.drawable\.res_marta', "", src, count=1),
             src.replace('"marta" -> R.drawable.res_marta', '"marta" -> R.drawable.res_foma', 1),
             src.replace('"marta" -> R.drawable.res_marta', '/* "marta" -> R.drawable.res_marta */', 1),
             re.sub(r"(fun residentRes.*?)else\s*->\s*null", r"\1else -> R.drawable.res_marta", src, count=1, flags=re.S)]
        r = [x != src and not check(x) for x in m]
        print("SELFCHECK OK" if all(r) else f"SELFCHECK FAIL {r}"); return 0 if all(r) else 1
    return 0 if check(src) else 1


def pastries(args):
    """id меню работ TRAY (town.jobs) = ветки `"<id>" -> R.drawable.pastry_<id>` в pastryRes (TownUi.kt) = файлы
    pastry_*.webp (TOWN-J1-1b4). --selfcheck не читает TownUi.kt: эталон § 1 спеки из id content.json -> MATCH,
    10 мутантов -> MISMATCH, каждый со своей причиной в строке вывода."""
    jobs = json.load(open(os.path.join(FP, "app/src/main/assets/content/content.json"), encoding="utf-8"))["town"]["jobs"]
    menu = list(dict.fromkeys(p["id"] for j in jobs if j.get("game") == "TRAY" for p in j.get("menu", [])))
    ids = set(menu)
    check = lambda i, f, text: when_check("pastryRes", "pastry_", i, f, text)
    if "--selfcheck" not in args:
        src = open(os.path.join(FP, "app/src/game/java/ru/finny/pet/game/ui/TownUi.kt"), encoding="utf-8").read()
        files = {f[7:-5] for f in os.listdir(os.path.join(FP, "app/src/game/res/drawable-nodpi")) if f.startswith("pastry_") and f.endswith(".webp")}
        return 0 if check(ids, files, src)[0] else 1
    ref = lambda m: ("fun pastryRes(id: String): Int? = when (id) {\n" + "".join(f'    "{i}" -> R.drawable.pastry_{i}\n' for i in m)
                     + "    else -> null\n}\n")
    R0, C = ref(menu), '"croissant" -> R.drawable.pastry_croissant'
    L = f"    {C}\n"
    cases = [(ids, ids, R0, "-> MATCH %d" % len(ids)),                                                   # 0 эталон
             (ids, ids, R0.replace(L, ""), "нет ветки ['croissant']"),                                   # 1
             (ids, ids, R0.replace(C, '"croissant" -> R.drawable.pastry_bread'), "чужой ресурс [('croissant', 'bread')]"),
             (ids, ids, R0.replace(C, f"/* {C} */"), "нет ветки ['croissant']"),                         # 3
             (ids, ids, R0.replace(C, f"// {C}"), "нет ветки ['croissant']"),                            # 4
             (ids, ids, R0.replace("else -> null", "else -> R.drawable.pastry_bread"), "else -> null False"),
             (ids, ids - {"cupcake"}, R0, "нет файла ['cupcake']"),                                      # 6
             (ids | {"eclair"}, ids, R0, "нет ветки ['eclair']"),                                        # 7
             (ids, ids, R0.replace(L, L + L), "дубли ['croissant']"),                                    # 8
             (set(), set(), ref([]), "id 0,"),                                                           # 9: конъюнкт bool(ids)
             (ids, ids, R0.replace('"bread" -> R.drawable.pastry_bread', C), "нет ветки ['bread']")]     # 10: ids == br
    fail = []
    for n, (i, f, text, why) in enumerate(cases):
        ok, line = check(i, f, text)
        if ok != (n == 0) or why not in line: fail.append(n)
    print("SELFCHECK OK" if not fail else f"SELFCHECK FAIL {fail}"); return 1 if fail else 0


if __name__ == "__main__":
    if os.environ.get("DUMP_OUT") and "bpy" in sys.modules:  # внутри Blender после генератора
        dump()
    else:
        cmd, rest = sys.argv[1], sys.argv[2:]
        sys.exit({"regress": lambda: regress(*rest), "diff": lambda: diff(*rest), "bg": lambda: bg(*rest),
                  "bbox": lambda: bbox(rest), "palette": palette, "which": lambda: which(rest),
                  "tiles": lambda: tiles(*rest), "residents": lambda: residents(rest),
                  "pastries": lambda: pastries(rest)}[cmd]())
