"""UI props for the Finny game, 512x512 transparent renders (Blender, Cycles).

All:   Blender -b -P tools/art/uiprops.py -- --all /abs/outdir [--samples 160] [--size 512]
One:   Blender -b -P tools/art/uiprops.py -- --only ui_piggy [--out /abs/ui_piggy.png]

Every builder in PROPS stands its object at the origin on the ground (z = 0) with the front facing -Y and
returns (height, radius, view): view "3q" = three-quarter from front-left slightly above (prop_camera),
"top" = steeper from above-front (lids, so the app can animate them closing onto the jar).
"""
import sys, os, math, argparse
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__))); from lib import *

PURPLE, MAGENTA, PINK, LAV, GOLD = "#520978", "#FF0053", "#FFD6E4", "#8A83D1", "#FFC94D"
INK = hexc("#2B2B2B"); WHITE = (1, 1, 1, 1)


def ink(): return material("ink", INK, rough=0.2, coat=0.8, sss=0.0)
def gold(): return material("gold", hexc(GOLD), rough=0.2, coat=0.9, sss=0.0, metallic=0.35)
def white(): return material("white", WHITE, rough=0.3, coat=0.5, sss=0.0)


def flat_shape(name, pts, loc, size, thick, mat, rot=(0, 0, 0), bevel=0.04):
    """Closed 2-D outline (x, y in [-1, 1]) extruded in the XZ plane, facing -Y."""
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata([(x * size, 0, y * size) for x, y in pts], [], [list(range(len(pts)))]); mesh.update()
    o = bpy.data.objects.new(name, mesh); bpy.context.collection.objects.link(o)
    o.location = loc; o.rotation_euler = rot
    sol = o.modifiers.new("sol", "SOLIDIFY"); sol.thickness = thick; sol.offset = 0
    b = o.modifiers.new("bev", "BEVEL"); b.width = bevel; b.segments = 4
    for pg in o.data.polygons: pg.use_smooth = True
    o.data.materials.append(mat); return o


def star_pts(n=5, inner=0.45):
    return [((inner if i % 2 else 1.0) * math.sin(i * math.pi / n), (inner if i % 2 else 1.0) * math.cos(i * math.pi / n)) for i in range(2 * n)]


def heart_pts(n=40):
    pts = []
    for i in range(n):
        t = 2 * math.pi * i / n
        pts.append((math.sin(t) ** 3, (13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)) / 16))
    return pts


def face(cx, cy, cz, r, sleepy=False, ink_m=None):
    """Happy toy face on a surface whose front point is (cx, cy, cz); r scales the features."""
    ink_m = ink_m or ink()
    for sx in (-1, 1):
        ex, ez = cx + 0.32 * r * sx, cz + 0.12 * r
        if sleepy:
            curve("lid", [(ex - 0.13 * r, cy - 0.02, ez + 0.02 * r), (ex, cy - 0.06, ez - 0.06 * r), (ex + 0.13 * r, cy - 0.02, ez + 0.02 * r)], 0.028 * r, ink_m)
        else:
            sphere("eye", (ex, cy, ez), 0.12 * r, (1, 0.6, 1.1), ink_m, levels=1)
            sphere("glint", (ex - 0.04 * r * sx, cy - 0.1 * r, ez + 0.04 * r), 0.035 * r, mat=material("glint", WHITE, emit=1.5), levels=1)
        sphere("cheek", (cx + 0.5 * r * sx, cy + 0.02, cz - 0.12 * r), 0.11 * r, (1, 0.4, 0.8), material("cheek", hexc("#FF6B7A"), alpha=0.55, sss=0.0, coat=0.0), levels=1)
    curve("mouth", [(cx - 0.18 * r, cy - 0.02, cz - 0.22 * r), (cx, cy - 0.05, cz - 0.3 * r), (cx + 0.18 * r, cy - 0.02, cz - 0.22 * r)], 0.028 * r, ink_m)


# ---------------------------------------------------------------- jar and lids

def jar():
    g = glass("glass", (0.7, 0.9, 1.0, 1.0), rough=0.1)
    bpy.ops.mesh.primitive_cylinder_add(radius=0.72, depth=1.5, location=(0, 0, 0.8), vertices=64, end_fill_type="NOTHING")
    o = bpy.context.object; o.name = "jar"
    sol = o.modifiers.new("sol", "SOLIDIFY"); sol.thickness = 0.06; sol.offset = -1
    b = o.modifiers.new("bev", "BEVEL"); b.width = 0.02; b.segments = 3
    for pg in o.data.polygons: pg.use_smooth = True
    o.data.materials.append(g)
    cylinder("bottom", (0, 0, 0.06), 0.72, 0.12, g, bevel=0.05)
    torus("rim", (0, 0, 1.56), 0.72, 0.045, g)
    cylinder("neck", (0, 0, 1.68), 0.74, 0.12, g, bevel=0.03)
    return 1.75, 0.8, "3q"


def lid_base(col):
    m = material("lid", hexc(col), rough=0.4, sss=0.2, coat=0.4)
    cylinder("lid", (0, 0, 0.13), 0.82, 0.26, m, bevel=0.06)
    torus("lid_ring", (0, 0, 0.26), 0.62, 0.035, material("lid_lt", mix(hexc(col), WHITE, 0.25), rough=0.4, sss=0.2, coat=0.4))
    return material("emboss", mix(hexc(col), WHITE, 0.45), rough=0.4, sss=0.25, coat=0.4)


def lid_mandatory():
    m = lid_base("#E63946")
    sphere("apple", (0, 0, 0.62), 0.44, (1.0, 1.0, 0.85), m)
    sphere("apple_dent", (0, 0, 0.95), 0.13, mat=material("lid", hexc("#E63946"), rough=0.4, sss=0.2, coat=0.4), levels=1)
    curve("stem", [(0, 0, 0.88), (0.04, 0.03, 1.15)], 0.04, material("stem", hexc("#8B5A2B"), sss=0.1))
    sphere("leaf", (0.2, 0.06, 1.08), 0.18, (1.2, 0.5, 0.35), material("leaf", hexc("#7BC47F"), sss=0.3), rot=(0, math.radians(-30), math.radians(20)))
    return 0.9, 0.85, "top"


def lid_optional():
    m = lid_base(MAGENTA)
    g = gold()
    box("gift", (0, 0, 0.6), (0.72, 0.72, 0.6), m, bevel=0.07)
    box("ribbon_x", (0, 0, 0.61), (0.78, 0.16, 0.63), g, bevel=0.02)
    box("ribbon_y", (0, 0, 0.61), (0.16, 0.78, 0.63), g, bevel=0.02)
    for a in (0, math.pi / 2):
        torus("bow", (0.15 * math.cos(a), 0.15 * math.sin(a), 0.98), 0.13, 0.045, g, rot=(math.radians(90), 0, a))
    return 0.9, 0.85, "top"


def lid_savings():
    m = lid_base("#2E9E5B")
    sphere("pig", (0, 0, 0.68), 0.4, (1.2, 1.0, 0.85), m)
    sphere("snout", (0, -0.45, 0.62), 0.16, (1.0, 0.6, 0.85), m, levels=1)
    for sx in (-1, 1):
        cone("ear", (0.25 * sx, -0.12, 1.05), 0.11, 0.24, (math.radians(-10), 0, 0), m)
        sphere("nostril", (0.06 * sx, -0.56, 0.62), 0.03, mat=material("lid", hexc("#2E9E5B"), rough=0.4, sss=0.2, coat=0.4), levels=1)
        sphere("eye", (0.18 * sx, -0.36, 0.78), 0.05, mat=ink(), levels=1)
    for sx, sy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        cylinder("leg", (0.25 * sx, 0.15 * sy, 0.35), 0.09, 0.2, m)
    return 0.9, 0.85, "top"


# ---------------------------------------------------------------- other props

def purse():
    """Kiss-lock coin purse, open at the top with coins and a card peeking out."""
    m = material("purse", hexc(PURPLE), rough=0.45, sss=0.15, coat=0.5)
    m2 = material("purse_in", hexc("#3A0A55"), rough=0.6, sss=0.1, coat=0.2)
    g = gold()
    sphere("body", (0, 0, 0.78), 0.85, (1.35, 0.62, 0.92), m)
    cylinder("cut", (0, 0, 1.52), 0.75, 0.3, m2, scale=(1.35, 0.55, 1))  # dark opening
    torus("frame", (0, 0, 1.5), 0.78, 0.07, g, scale=(1.35, 0.55, 1))
    for sx in (-1, 1):
        sphere("clasp", (0.14 * sx, -0.42, 1.62), 0.11, mat=g, levels=1)
        sphere("stud", (0.55 * sx, -0.45, 0.85), 0.05, mat=g, levels=1)
    for x, r in ((-0.38, -0.25), (0.02, 0.1), (0.4, 0.3)):
        cylinder("coin", (x, 0.05, 1.62), 0.27, 0.06, g, rot=(math.radians(90), 0, r), bevel=0.015)
    box("card", (-0.15, 0.15, 1.7), (0.5, 0.03, 0.7), material("card", hexc(MAGENTA), rough=0.4, coat=0.5), rot=(0, math.radians(-10), 0), bevel=0.02)
    flat_shape("heart", heart_pts(), (0, -0.5, 0.78), 0.2, 0.06, material("heart_g", hexc(GOLD), rough=0.3, coat=0.7), bevel=0.01)
    return 1.95, 1.0, "3q"


def piggy():
    m = material("pig", hexc("#F9A8C2"), rough=0.45, sss=0.35, coat=0.35)
    md = material("pig_d", hexc("#F27DA5"), rough=0.45, sss=0.3, coat=0.35)
    sphere("body", (0, 0, 0.95), 0.85, (1.15, 1.0, 0.85), m)
    sphere("snout", (0, -0.95, 0.85), 0.28, (1.0, 0.55, 0.8), md, levels=1)
    for sx in (-1, 1):
        sphere("nostril", (0.1 * sx, -1.1, 0.86), 0.05, (1, 0.6, 1.2), material("nostril", hexc("#C9527E"), sss=0.1), levels=1)
        cone("ear", (0.5 * sx, -0.2, 1.65), 0.2, 0.42, (math.radians(-15), math.radians(25 * sx), 0), m)
        for sy in (-0.45, 0.4):
            cylinder("leg", (0.5 * sx, sy, 0.18), 0.17, 0.36, md, bevel=0.05)
    face(0, -0.78, 1.15, 0.9)
    box("slot", (0, 0.05, 1.68), (0.42, 0.1, 0.06), ink(), bevel=0.02)
    curve("tail", [(0, 0.95, 1.0), (0.15, 1.15, 1.15), (0.0, 1.25, 1.3), (-0.12, 1.1, 1.38)], 0.04, md, taper=True)
    return 1.85, 1.1, "3q"


def trophy():
    g = gold(); g2 = material("gold_d", hexc("#E0A800"), rough=0.25, coat=0.9, sss=0.0, metallic=0.35)
    m = material("plaque", hexc(PURPLE), rough=0.4, coat=0.5)
    cylinder("base", (0, 0, 0.12), 0.62, 0.24, m, bevel=0.06)
    cylinder("base2", (0, 0, 0.3), 0.45, 0.14, g, bevel=0.05)
    cylinder("stem", (0, 0, 0.55), 0.13, 0.4, g2)
    sphere("knob", (0, 0, 0.7), 0.2, (1, 1, 0.6), g, levels=1)
    bpy.ops.mesh.primitive_cylinder_add(radius=0.62, depth=1.05, location=(0, 0, 1.3), vertices=64)
    cup = bpy.context.object; cup.name = "cup"; cup.scale = (1, 1, 1)
    b = cup.modifiers.new("bev", "BEVEL"); b.width = 0.28; b.segments = 8
    for pg in cup.data.polygons: pg.use_smooth = True
    cup.data.materials.append(g)
    cone("cup_top", (0, 0, 1.75), 0.55, 0.3, (0, 0, 0), g, r2=0.66)
    torus("rim", (0, 0, 1.9), 0.64, 0.045, g2)
    for sx in (-1, 1):
        torus("handle", (0.62 * sx, 0, 1.38), 0.32, 0.07, g2, rot=(math.radians(90), 0, 0))
    flat_shape("star", star_pts(), (0, -0.62, 1.3), 0.3, 0.08, material("star", hexc(MAGENTA), rough=0.35, coat=0.6), bevel=0.02)
    return 2.0, 1.0, "3q"


def book():
    m = material("cover", hexc(PURPLE), rough=0.45, sss=0.1, coat=0.5)
    pages = material("pages", hexc("#FFF6E8"), rough=0.6, sss=0.1, coat=0.1)
    rot = (math.radians(-12), 0, math.radians(-8))
    bpy.ops.object.empty_add(location=(0, 0.1, 0)); piv = bpy.context.object; piv.rotation_euler = rot
    for o in (box("pages", (0.04, 0.02, 0.78), (1.3, 0.34, 1.55), pages, bevel=0.03),
              box("front", (0, -0.2, 0.8), (1.4, 0.07, 1.62), m, bevel=0.03),
              box("back", (0, 0.24, 0.8), (1.4, 0.07, 1.62), m, bevel=0.03),
              box("spine", (-0.7, 0.02, 0.8), (0.1, 0.48, 1.62), m, bevel=0.04),
              box("band", (0, -0.24, 0.52), (1.42, 0.02, 0.12), gold(), bevel=0.005),
              box("band2", (0, -0.24, 1.08), (1.42, 0.02, 0.12), gold(), bevel=0.005),
              flat_shape("star", star_pts(), (0.02, -0.25, 0.82), 0.36, 0.05, gold(), bevel=0.015)):
        o.parent = piv
    return 1.75, 0.9, "3q"


def lock():
    g = gold(); g2 = material("gold_d", hexc("#E0A800"), rough=0.25, coat=0.9, sss=0.0, metallic=0.35)
    box("body", (0, 0, 0.55), (1.3, 0.6, 1.1), g, bevel=0.2)
    torus("shackle", (0, 0, 1.15), 0.42, 0.11, g2, rot=(math.radians(90), 0, 0))
    cylinder("keyhole", (0, -0.32, 0.62), 0.12, 0.1, ink(), rot=(math.radians(90), 0, 0))
    box("keyslot", (0, -0.32, 0.45), (0.1, 0.1, 0.3), ink(), bevel=0.02)
    return 1.72, 0.75, "3q"


def sun():
    m = material("sun", hexc(GOLD), rough=0.4, sss=0.3, coat=0.4)
    r = material("ray", hexc("#FFA630"), rough=0.4, sss=0.3, coat=0.4)
    sphere("disc", (0, 0, 1.05), 0.62, (1, 0.8, 1), m)
    for i in range(10):
        a = i * math.pi / 5
        cone("ray", (0.85 * math.cos(a), 0, 1.05 + 0.85 * math.sin(a)), 0.16, 0.42, (0, math.pi / 2 - a, 0), r, scale=(1, 0.6, 1))
    face(0, -0.5, 1.12, 1.0)
    return 1.5, 0.8, "3q"


def moon():
    m = material("moon", hexc("#FFE08A"), rough=0.4, sss=0.3, coat=0.4)
    body = sphere("moon", (0, 0, 1.05), 0.8, (1, 0.75, 1), m)
    cutter = sphere("cutter", (0.45, -0.1, 1.15), 0.68, (1, 1.2, 1), None)
    cutter.hide_render = True; cutter.hide_viewport = True
    bo = body.modifiers.new("cut", "BOOLEAN"); bo.object = cutter; bo.operation = "DIFFERENCE"
    # sleepy face on the outer bulge (left side)
    ink_m = ink()
    for z in (1.3, 0.85):
        curve("lid", [(-0.5, -0.56, z + 0.02), (-0.4, -0.62, z - 0.04), (-0.3, -0.6, z + 0.02)], 0.025, ink_m)
    curve("mouth", [(-0.5, -0.58, 0.62), (-0.42, -0.62, 0.55), (-0.34, -0.6, 0.6)], 0.025, ink_m)
    sphere("cheek", (-0.62, -0.42, 0.68), 0.09, (1, 0.4, 0.8), material("cheek", hexc("#FF6B7A"), alpha=0.55, sss=0.0, coat=0.0), levels=1)
    flat_shape("star", star_pts(), (0.75, 0.0, 1.75), 0.2, 0.08, gold(), rot=(0, math.radians(15), 0), bevel=0.02)
    flat_shape("star2", star_pts(), (0.9, 0.1, 0.55), 0.12, 0.06, gold(), rot=(0, math.radians(-10), 0), bevel=0.015)
    return 1.6, 0.8, "3q"


def gamepad():
    m = material("pad", hexc(PURPLE), rough=0.45, sss=0.1, coat=0.5)
    m2 = material("pad_lt", hexc(LAV), rough=0.45, sss=0.15, coat=0.4)
    bpy.ops.object.empty_add(location=(0, 0, 0.62)); piv = bpy.context.object; piv.rotation_euler = (math.radians(50), 0, 0)
    parts = [sphere("body", (0, 0, 0), 0.7, (1.35, 0.85, 0.5), m)]
    for sx in (-1, 1):
        parts.append(sphere("grip", (0.72 * sx, -0.3, -0.02), 0.42, (0.85, 1.3, 0.75), m, rot=(0, 0, math.radians(-25 * sx))))
    parts.append(box("dpad_h", (-0.5, 0.08, 0.3), (0.42, 0.14, 0.12), m2, bevel=0.04))
    parts.append(box("dpad_v", (-0.5, 0.08, 0.3), (0.14, 0.42, 0.12), m2, bevel=0.04))
    for (dx, dy), c in zip(((0, 0.18), (-0.18, 0), (0.18, 0), (0, -0.18)), (MAGENTA, GOLD, "#7BC47F", "#6FB1E0")):
        parts.append(cylinder("btn", (0.5 + dx, 0.08 + dy, 0.32), 0.08, 0.12, material("btn" + c, hexc(c), rough=0.35, coat=0.6), bevel=0.02))
    parts.append(cylinder("start", (0.0, 0.05, 0.3), 0.055, 0.08, material("start", hexc(MAGENTA), rough=0.35, coat=0.6), scale=(1.8, 1, 1), bevel=0.01))
    for p in parts: p.parent = piv
    return 1.1, 0.9, "3q"


def bag():
    m = material("bag", hexc(LAV), rough=0.5, sss=0.2, coat=0.35)
    box("bag", (0, 0, 0.75), (1.45, 0.75, 1.5), m, bevel=0.08)
    box("bag_in", (0, 0, 1.45), (1.3, 0.6, 0.2), material("bag_in", hexc("#6E67B8"), rough=0.6, sss=0.1, coat=0.2), bevel=0.04)
    for sy in (-0.2, 0.2):
        torus("handle", (0, sy, 1.5), 0.42, 0.06, material("handle", hexc(MAGENTA), rough=0.4, coat=0.5), rot=(math.radians(90), 0, 0))
    flat_shape("heart", heart_pts(), (0, -0.38, 0.8), 0.36, 0.08, material("heart", hexc(MAGENTA), rough=0.35, coat=0.6), bevel=0.02)
    return 1.95, 0.85, "3q"


def bubble_q():
    m = material("bubble", hexc(PURPLE), rough=0.4, sss=0.1, coat=0.5)
    sphere("bubble", (0, 0, 1.2), 0.85, (1.25, 0.55, 1.0), m)
    cone("tail", (-0.55, -0.05, 0.42), 0.28, 0.6, (math.radians(180), 0, math.radians(-20)), m, scale=(1, 0.5, 1))
    text3d("q", "?", (0.0, -0.42, 0.72), 1.35, gold(), extrude=0.12)
    return 1.7, 0.9, "3q"


PROPS = {
    "ui_jar": jar, "ui_lid_mandatory": lid_mandatory, "ui_lid_optional": lid_optional, "ui_lid_savings": lid_savings,
    "ui_purse": purse, "ui_piggy": piggy, "ui_trophy": trophy, "ui_book": book, "ui_lock": lock, "ui_sun": sun,
    "ui_moon": moon, "ui_gamepad": gamepad, "ui_bag": bag, "ui_bubble_q": bubble_q,
}


def render_prop(name, out, samples=160, size=512):
    scene = reset_scene(samples, size)
    h, r, view = PROPS[name]()
    studio(scene, target=(0, 0, h * 0.5))  # no shadow catcher: UI props sit on panels
    if view == "top":
        d = max(h, 2 * r) * 1.6
        camera(scene, (0.35 * d, -0.8 * d, 1.1 * d), (0, 0, h * 0.4), lens=60)
    else:
        prop_camera(scene, h, r)
    render(scene, out)


if __name__ == "__main__":
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    ap = argparse.ArgumentParser()
    ap.add_argument("--all"); ap.add_argument("--only"); ap.add_argument("--out")
    ap.add_argument("--samples", type=int, default=160); ap.add_argument("--size", type=int, default=512)
    A = ap.parse_args(argv)
    if A.all:
        os.makedirs(A.all, exist_ok=True)
        for n in PROPS: render_prop(n, os.path.join(A.all, n + ".png"), A.samples, A.size)
    else:
        n = A.only or "ui_jar"
        render_prop(n, A.out or os.path.abspath(n + ".png"), A.samples, A.size)
