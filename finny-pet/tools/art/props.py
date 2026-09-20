"""Toy-style 3D props for the Finny game flavour: shop items, goals, match-3 tiles, UI coin.

All:  Blender -b -P tools/art/props.py -- --all /abs/outdir [--samples 96]
One:  Blender -b -P tools/art/props.py -- --only tile_apple [--out /abs/file.png]

Every builder puts its object at the origin, standing on z = 0, front facing -Y, and returns nothing.
The camera is fitted automatically to the bounding box of what was built (see `fit_camera`), so a
prop only has to be modelled; per-prop framing lives in `VIEWS`. Same studio rig as pet.py.
"""
import sys, os, math, argparse, random
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__))); from lib import *
import bpy
from bpy_extras.object_utils import world_to_camera_view
from mathutils import Vector

R = math.radians
WHITE = (1, 1, 1, 1)
C = {  # brand accents + pastel toy palette
    "purple": "#520978", "accent": "#FF0053", "pink": "#FFD6E4", "lav": "#8A83D1",
    "red": "#E63946", "orange": "#F4A261", "yellow": "#FFC94D", "gold": "#F5B400", "gold_dk": "#D99000",
    "green": "#7BC47F", "blue": "#6FB1E0", "sky": "#BFE3FF", "mint": "#9EE2C8", "cream": "#FFF3D6",
    "brown": "#8D5A3B", "tan": "#D8A66F", "ink": "#2B2B2B", "grey": "#B8BCC8", "navy": "#3A3F55",
}


def M(c, **kw):
    """Toy plastic material by palette key or hex."""
    return material(c, hexc(C.get(c, c)), **kw)


def m_metal(c="gold", rough=0.28):
    return metal("m_" + c, hexc(C.get(c, c)), rough=rough)


def m_glint():
    return material("glint", WHITE, emit=1.5)


def star(name, loc, r_out, r_in, thick, mat, rot=(0, 0, 0), points=5):
    """Puffy star: triangle fan + solidify + bevel + subsurf. Lies in the XY plane, one point along +Y."""
    verts = [(0.0, 0.0, 0.0)]
    for i in range(points * 2):
        a = math.pi / 2 + i * math.pi / points
        r = r_out if i % 2 == 0 else r_in
        verts.append((r * math.cos(a), r * math.sin(a), 0.0))
    n = points * 2
    faces = [[0, 1 + i, 1 + (i + 1) % n] for i in range(n)]
    mesh = bpy.data.meshes.new(name); mesh.from_pydata(verts, [], faces); mesh.update()
    o = bpy.data.objects.new(name, mesh); bpy.context.collection.objects.link(o)
    o.location = loc; o.rotation_euler = rot
    sol = o.modifiers.new("sol", "SOLIDIFY"); sol.thickness = thick; sol.offset = 0
    bev = o.modifiers.new("bev", "BEVEL"); bev.width = thick * 0.35; bev.segments = 4
    smooth(o, 2); o.data.materials.append(mat); return o


def capsule(name, loc, r, length, mat, rot=(0, 0, 0)):
    return sphere(name, loc, r, (1, 1, length / (2 * r)), mat, rot)


def eyes(y, z, dx=0.17, r=0.09):
    """Two cartoon eyes on a surface at depth y (front = -Y)."""
    for sx in (-1, 1):
        sphere("sclera", (dx * sx, y, z), r, (1, 0.6, 1.1), M("#FFFFFF", rough=0.3, coat=0.5, sss=0.0))
        sphere("pupil", (dx * sx, y - r * 0.55, z), r * 0.6, (1, 0.6, 1.1), M("ink", rough=0.2, coat=0.8, sss=0.0))
        sphere("glint", (dx * sx - r * 0.25 * sx, y - r * 1.05, z + r * 0.3), r * 0.2, (1, 1, 1), m_glint())


# ---------------------------------------------------------------- shop items

def item_food_basic():
    bowl = M("blue"); rim = M("sky")
    cone("bowl", (0, 0, 0.32), 0.7, 0.64, (0, 0, 0), bowl, r2=1.0)
    torus("rim", (0, 0, 0.62), 0.93, 0.09, rim)
    cylinder("food", (0, 0, 0.55), 0.84, 0.14, M("tan"))
    rnd = random.Random(3); kib = M("#B9743F", rough=0.6)
    for ring, n, z in ((0.0, 1, 0.86), (0.3, 6, 0.78), (0.58, 11, 0.68)):
        for i in range(n):
            a = i * 2 * math.pi / n + ring
            cylinder("kibble", (ring * math.cos(a), ring * math.sin(a), z), 0.12, 0.09, kib, bevel=0.03,
                     rot=(rnd.uniform(-0.5, 0.5), rnd.uniform(-0.5, 0.5), rnd.uniform(0, 3)))
    sphere("paw", (0, -0.86, 0.32), 0.1, (1.0, 0.35, 0.85), rim, (R(-25), 0, 0))
    for i in (-1.2, -0.4, 0.4, 1.2):
        sphere("toe", (0.1 * i, -0.88 - 0.008 * abs(i), 0.46 - 0.02 * abs(i)), 0.035, (1, 0.35, 1.1), rim, (R(-25), 0, 0))


def item_food_lunch():
    pot = M("accent"); lid = M("pink")
    cylinder("pot", (0, 0, 0.45), 0.8, 0.9, pot, bevel=0.14)
    torus("rim", (0, 0, 0.88), 0.78, 0.07, lid)
    for sx in (-1, 1):
        torus("handle", (0.88 * sx, 0, 0.72), 0.2, 0.06, lid, rot=(R(90), 0, 0))
    cylinder("soup", (0, 0, 0.86), 0.72, 0.08, M("#FFD166", rough=0.3, coat=0.8, sss=0.4))
    rnd = random.Random(7)
    for i, col in enumerate(("green", "orange", "cream", "green", "#FF8C42", "cream", "green")):
        a = i * 0.9; r = 0.15 + 0.35 * ((i * 7) % 5) / 5
        sphere("veg", (r * math.cos(a), r * math.sin(a), 0.92), 0.1, (1, 1, 0.6), M(col), (0, 0, rnd.uniform(0, 3)))
    steam = material("steam", WHITE, alpha=0.55, sss=0.0, coat=0.0, rough=0.9)
    for (x, y, z, r) in ((-0.28, 0.1, 1.28, 0.2), (0.0, -0.05, 1.55, 0.22), (0.28, 0.12, 1.85, 0.19), (0.1, 0.0, 2.1, 0.14), (-0.22, 0.05, 1.62, 0.13), (0.34, -0.1, 1.45, 0.12)):
        sphere("steam", (x, y, z), r, (1, 1, 1), steam)


def item_care_shampoo():
    body = M("mint")
    cylinder("body", (0, 0, 0.68), 0.44, 1.36, body, scale=(1, 0.72, 1), bevel=0.18)
    cylinder("label", (0, 0, 0.62), 0.455, 0.56, M("#FFFFFF", sss=0.1), scale=(1, 0.72, 1), bevel=0.02)
    sphere("drop", (0, -0.33, 0.56), 0.11, (1, 0.35, 1.0), M("blue"))
    cone("drop_tip", (0, -0.33, 0.7), 0.1, 0.22, (0, 0, 0), M("blue"), scale=(1, 0.35, 1))
    cylinder("neck", (0, 0, 1.42), 0.15, 0.2, M("#FFFFFF", sss=0.0), bevel=0.03)
    cylinder("pump", (0, 0, 1.68), 0.09, 0.4, M("lav"), bevel=0.02)
    box("head", (0, -0.13, 1.92), (0.2, 0.46, 0.15), M("lav"), bevel=0.06)
    cylinder("spout", (0, -0.35, 1.86), 0.045, 0.14, M("lav"), rot=(R(90), 0, 0))


def item_care_brush():
    pad = M("lav"); pin = M("#FFFFFF", sss=0.0, rough=0.3)
    sphere("pad", (0, 0, 0.14), 0.56, (1.0, 0.78, 0.26), pad)
    sphere("cushion", (0, 0, 0.24), 0.5, (1.0, 0.72, 0.16), M("pink"))
    for i in range(5):
        for j in range(4):
            x = -0.36 + 0.18 * i; y = -0.24 + 0.16 * j
            if x * x / 0.2 + y * y / 0.12 > 1.0: continue
            cylinder("pin", (x, y, 0.42), 0.02, 0.32, pin)
            sphere("tip", (x, y, 0.59), 0.035, (1, 1, 1), M("accent"))
    cylinder("handle", (0.95, 0, 0.16), 0.11, 0.9, pad, rot=(0, R(90), 0), bevel=0.06)
    sphere("handle_end", (1.38, 0, 0.16), 0.13, (1, 1, 1), M("accent"))


def item_care_vitamins():
    jar = material("jar", hexc("#D6F0FF"), alpha=0.32, sss=0.0, coat=1.0, rough=0.08)
    rnd = random.Random(11)
    cols = ["accent", "yellow", "green", "blue", "orange", "lav", "#FF8C42", "mint"]
    k = 0
    for layer in range(4):
        for i in range(5):
            a = i * 1.26 + layer * 0.5; r = 0.28 if layer % 2 else 0.18
            k += 1
            capsule("pill", (r * math.cos(a), r * math.sin(a), 0.14 + 0.2 * layer), 0.08, 0.28, M(cols[k % len(cols)]),
                    (rnd.uniform(0, 1.2), rnd.uniform(0, 1.2), rnd.uniform(0, 3)))
    cylinder("jar", (0, 0, 0.56), 0.55, 1.12, jar, bevel=0.1)
    cylinder("lid", (0, 0, 1.22), 0.58, 0.24, M("accent"), bevel=0.06)
    torus("band", (0, 0, 1.1), 0.5, 0.06, M("accent"))
    star("label", (0, -0.56, 0.5), 0.2, 0.09, 0.03, M("accent"), rot=(R(90), 0, 0))


def item_fun_ball():
    rot = (R(22), R(-18), 0)
    sphere("ball", (0, 0, 0.75), 0.75, (1, 1, 1), M("accent", rough=0.35, coat=0.6))
    torus("stripe", (0, 0, 0.75), 0.74, 0.07, M("#FFFFFF", sss=0.0), scale=(1, 1, 1.4), rot=rot)
    torus("stripe2", (0, 0, 0.75), 0.74, 0.03, M("yellow"), scale=(1, 1, 1), rot=(R(22 + 24), R(-18), 0))
    torus("stripe3", (0, 0, 0.75), 0.74, 0.03, M("yellow"), scale=(1, 1, 1), rot=(R(22 - 24), R(-18), 0))


def item_fun_bow():
    m = M("accent", rough=0.4, coat=0.5); m2 = M("#FF6FA0")
    for sx in (-1, 1):
        sphere("loop", (0.5 * sx, 0, 0.86), 0.5, (1, 0.42, 0.62), m, (0, R(-10 * sx), 0))
    sphere("knot", (0, -0.06, 0.8), 0.24, (0.8, 0.7, 0.9), m2)
    for sx in (-1, 1):
        box("tail", (0.28 * sx, 0.02, 0.3), (0.24, 0.08, 0.6), m, rot=(0, R(-20 * sx), 0), bevel=0.03)


def item_fun_balloon():
    sphere("balloon", (0, 0, 1.95), 0.6, (1, 1, 1.15), M("accent", rough=0.25, coat=0.8))
    cone("knot", (0, 0, 1.22), 0.1, 0.16, (0, 0, 0), M("accent"))
    sphere("glint", (-0.2, -0.42, 2.3), 0.07, (1, 0.6, 1.6), m_glint())
    curve("string", [(0, 0, 1.16), (0.12, 0, 0.8), (-0.12, 0, 0.45), (0.1, 0, 0.15), (0.25, 0.05, 0.02)], 0.014, M("grey"))


def item_fun_book():
    piv = (0, 0, 0.74); tilt = (R(12), 0, 0)
    bpy.ops.object.empty_add(location=piv); p = bpy.context.object; p.rotation_euler = tilt
    cov = box("cover", (0, 0, 0), (1.1, 0.34, 1.4), M("purple"), bevel=0.05); cov.parent = p
    pages = box("pages", (0.04, -0.02, 0), (1.04, 0.26, 1.32), M("cream", sss=0.1), bevel=0.02); pages.parent = p
    spine = box("spine", (-0.57, 0, 0), (0.1, 0.36, 1.42), M("accent"), bevel=0.04); spine.parent = p
    st = star("star", (0.04, -0.19, 0.12), 0.34, 0.15, 0.05, M("yellow"), rot=(R(90), 0, 0)); st.parent = p
    moon = sphere("moon", (0.3, -0.19, -0.42), 0.13, (1, 0.3, 1), M("yellow")); moon.parent = p
    for i, x in enumerate((-0.32, -0.02, 0.28)):
        b = box("band", (x, -0.19, -0.5 + 0.02 * i), (0.16, 0.03, 0.06), M("pink"), bevel=0.01); b.parent = p


def item_fun_tent():
    tent = M("yellow"); trim = M("accent")
    t = cone("tent", (0, 0, 0.82), 1.05, 1.64, (0, 0, 0), tent); t.modifiers["bev"].width = 0.12
    for z, r in ((0.35, 0.83), (0.85, 0.5)):
        torus("stripe", (0, 0, z), r, 0.045, trim)
    a = R(32)  # door turned toward the default camera azimuth so it reads as an entrance
    sphere("door", (-0.7 * math.sin(a), -0.7 * math.cos(a), 0.46), 0.46, (1, 0.2, 1.5), M("purple"), (R(32), 0, -a))
    for a in (0.3, 2.4, 4.4):
        cylinder("pole", (0.16 * math.cos(a), 0.16 * math.sin(a), 1.85), 0.045, 0.9, M("tan"),
                 rot=(R(-16) * math.sin(a), R(16) * math.cos(a), 0))
    cone("flag", (0.13, -0.05, 2.12), 0.14, 0.24, (R(90), 0, 0), trim, scale=(1, 0.2, 1))


# ---------------------------------------------------------------- goals

def goal_scooter():
    deck = M("accent"); dark = M("navy"); chrome = m_metal("grey", 0.3)
    box("deck", (0.02, 0, 0.32), (1.4, 0.3, 0.1), deck, bevel=0.04)
    for x in (-0.68, 0.78):
        torus("tire", (x, 0, 0.23), 0.155, 0.075, dark, rot=(R(90), 0, 0))
        cylinder("hub", (x, 0, 0.23), 0.1, 0.22, M("grey"), rot=(R(90), 0, 0))
    box("fender", (-0.68, 0, 0.4), (0.36, 0.2, 0.08), M("purple"), bevel=0.03)
    cylinder("fork", (0.78, 0, 0.42), 0.05, 0.4, chrome, rot=(0, R(-12), 0))
    cylinder("stem", (0.63, 0, 1.05), 0.055, 1.3, chrome, rot=(0, R(-12), 0))
    cylinder("bar", (0.5, 0, 1.68), 0.045, 0.8, chrome, rot=(R(90), 0, 0))
    for sy in (-1, 1):
        cylinder("grip", (0.5, 0.36 * sy, 1.68), 0.07, 0.22, deck, rot=(R(90), 0, 0), bevel=0.02)


def goal_paints():
    tray = M("#FFFFFF", sss=0.1); lid = M("lav")
    box("tray", (0, 0, 0.09), (1.7, 1.1, 0.18), tray, bevel=0.05)
    box("lid", (0, 0.72, 0.56), (1.7, 0.1, 1.05), lid, rot=(R(-18), 0, 0), bevel=0.04)
    cols = ("accent", "#FF8C42", "yellow", "green", "blue", "purple", "#FF6FA0", "brown")
    for i, c in enumerate(cols):
        x = -0.6 + 0.4 * (i % 4); y = 0.25 - 0.5 * (i // 4)
        cylinder("pan", (x, y, 0.2), 0.16, 0.08, M(c, rough=0.25, coat=0.7), bevel=0.02)
    rot = (0, R(90), R(-22))
    cylinder("brush", (0.15, -0.12, 0.3), 0.045, 1.5, M("tan"), rot=rot)
    cylinder("ferrule", (-0.55, 0.16, 0.3), 0.05, 0.22, m_metal("grey"), rot=rot)
    cone("bristles", (-0.78, 0.25, 0.3), 0.05, 0.3, (0, R(-90), R(-22)), M("blue"))


def goal_lego():
    def brick(loc, size, mat, studs):
        box("brick", loc, size, mat, bevel=0.03)
        for i in range(studs[0]):
            for j in range(studs[1]):
                x = loc[0] + (i - (studs[0] - 1) / 2) * 0.32; y = loc[1] + (j - (studs[1] - 1) / 2) * 0.32
                cylinder("stud", (x, y, loc[2] + size[2] / 2 + 0.05), 0.1, 0.1, mat, bevel=0.02)
    brick((0, 0, 0.19), (1.28, 0.64, 0.38), M("accent"), (4, 2))
    brick((-0.32, 0, 0.57), (0.64, 0.64, 0.38), M("blue"), (2, 2))
    brick((0.16, 0, 0.95), (1.28, 0.64, 0.38), M("yellow"), (4, 2))
    brick((0.64, -0.7, 0.19), (0.64, 0.64, 0.38), M("green"), (2, 2))


def goal_zoo():
    piv = (0, 0, 0.44); bpy.ops.object.empty_add(location=piv); p = bpy.context.object; p.rotation_euler = (R(14), 0, 0)
    t = box("ticket", (0, 0, 0), (1.7, 0.06, 0.84), M("yellow", sss=0.1), bevel=0.03); t.parent = p
    for k in range(6):
        d = cylinder("perf", (0.42, -0.02, -0.34 + 0.14 * k), 0.035, 0.05, M("purple"), rot=(R(90), 0, 0)); d.parent = p
    for k in range(3):
        b = box("stub", (0.72, -0.04, -0.2 + 0.2 * k), (0.34, 0.02, 0.08), M("purple"), bevel=0.01); b.parent = p
    mane = torus("mane", (-0.28, -0.08, 0.04), 0.24, 0.12, M("#FF8C42"), rot=(R(90), 0, 0)); mane.parent = p
    face = sphere("face", (-0.28, -0.1, 0.04), 0.23, (1, 0.5, 1), M("orange")); face.parent = p
    for sx in (-1, 1):
        e = sphere("ear", (-0.28 + 0.17 * sx, -0.08, 0.22), 0.07, (1, 0.5, 1), M("orange")); e.parent = p
        ey = sphere("eye", (-0.28 + 0.08 * sx, -0.21, 0.09), 0.035, (1, 0.6, 1), M("ink", sss=0.0)); ey.parent = p
    nose = sphere("nose", (-0.28, -0.22, -0.02), 0.05, (1.3, 0.6, 0.8), M("brown")); nose.parent = p
    m = curve("mouth", [(-0.36, -0.24, -0.08), (-0.28, -0.25, -0.12), (-0.2, -0.24, -0.08)], 0.015, M("brown")); m.parent = p


def goal_custom():
    star("star", (0, 0, 0.9 * math.sin(R(54)) + 0.06), 0.9, 0.42, 0.34, M("gold", rough=0.3, coat=0.7), rot=(R(90), 0, 0))
    sphere("glint", (-0.22, -0.2, 1.05), 0.06, (1, 0.5, 1.4), m_glint())


# ---------------------------------------------------------------- tiles & ui

def coin(rot, loc=(0, 0, 0.5)):
    g = M("gold", rough=0.25, coat=0.8, sss=0.0); d = M("gold_dk", rough=0.3, coat=0.6, sss=0.0)
    bpy.ops.object.empty_add(location=loc); p = bpy.context.object; p.rotation_euler = rot
    c = cylinder("coin", (0, 0, 0), 0.5, 0.11, g, bevel=0.03); c.parent = p
    r = torus("rim", (0, 0, 0.05), 0.42, 0.035, d); r.parent = p
    r2 = torus("rim_b", (0, 0, -0.05), 0.42, 0.035, d); r2.parent = p
    s = star("emboss", (0, 0, 0.06), 0.3, 0.14, 0.04, d); s.parent = p
    s2 = star("emboss_b", (0, 0, -0.06), 0.3, 0.14, 0.04, d); s2.parent = p


def tile_coin():
    coin((R(62), 0, R(-18)), loc=(0, 0, 0.48))


def ui_coin():
    coin((R(90), 0, 0))


def tile_apple():
    sphere("apple", (0, 0, 0.53), 0.56, (1, 1, 0.93), M("red", rough=0.3, coat=0.7))
    sphere("dimple", (0, 0, 1.04), 0.13, (1, 1, 0.5), M("#B8202C"))
    cylinder("stem", (0.03, 0, 1.12), 0.035, 0.28, M("brown"), rot=(0, R(10), 0), bevel=0.01)
    sphere("leaf", (0.2, -0.06, 1.14), 0.17, (1, 0.45, 0.18), M("green"), (0, R(-30), R(25)))
    sphere("glint", (-0.2, -0.42, 0.78), 0.05, (1, 0.5, 1.6), m_glint())


def tile_gift():
    bx = M("blue"); rb = M("yellow", rough=0.35, coat=0.5)
    box("box", (0, 0, 0.4), (0.9, 0.9, 0.8), bx, bevel=0.05)
    box("lid", (0, 0, 0.84), (1.0, 1.0, 0.22), M("sky"), bevel=0.05)
    box("ribbon_x", (0, 0, 0.48), (1.04, 0.2, 0.96), rb, bevel=0.02)
    box("ribbon_y", (0, 0, 0.48), (0.2, 1.04, 0.96), rb, bevel=0.02)
    for sx in (-1, 1):
        torus("loop", (0.2 * sx, 0, 1.06), 0.16, 0.07, rb, scale=(1, 0.6, 0.8), rot=(R(90), 0, R(10 * sx)))
    sphere("knot", (0, 0, 1.04), 0.1, (1, 1, 1), rb)


def tile_piggy():
    pk = M("#FF9EC4"); dk = M("#F77FAF")
    sphere("body", (0, 0.05, 0.6), 0.52, (1.15, 1.0, 0.92), pk)
    sphere("head", (0, -0.5, 0.66), 0.38, (1, 1, 1), pk)
    cylinder("snout", (0, -0.86, 0.6), 0.17, 0.14, dk, rot=(R(90), 0, 0), bevel=0.04)
    for sx in (-1, 1):
        sphere("nostril", (0.06 * sx, -0.93, 0.6), 0.03, (1, 1, 1.3), M("ink", sss=0.0))
        cone("ear", (0.2 * sx, -0.55, 1.02), 0.12, 0.26, (R(-20), R(20 * sx), 0), pk)
        sphere("eye", (0.16 * sx, -0.82, 0.78), 0.05, (1, 0.6, 1), M("ink", sss=0.0))
        for sy in (-1, 1):
            cylinder("leg", (0.3 * sx, 0.05 + 0.24 * sy, 0.14), 0.11, 0.28, pk, bevel=0.03)
    box("slot", (0, 0.05, 1.05), (0.34, 0.07, 0.06), M("ink", sss=0.0), bevel=0.01)
    c = cylinder("coin", (0, 0.05, 1.2), 0.2, 0.05, M("gold", sss=0.0, coat=0.8), rot=(R(90), 0, 0), bevel=0.01)
    curve("tail", [(0, 0.6, 0.72), (0.15, 0.75, 0.82), (0, 0.85, 0.7), (0.14, 0.9, 0.62)], 0.03, dk)


def tile_star():
    star("star", (0, 0, 0.9 * math.sin(R(54)) + 0.06), 0.9, 0.42, 0.34, M("yellow", rough=0.3, coat=0.7), rot=(R(90), 0, 0))
    sphere("glint", (-0.22, -0.2, 1.05), 0.06, (1, 0.5, 1.4), m_glint())


def tile_bomb():
    sphere("bomb", (0, 0, 0.56), 0.56, (1, 1, 1), M("navy", rough=0.3, coat=0.8))
    cylinder("cap", (0, 0, 1.1), 0.15, 0.14, M("grey"), bevel=0.04)
    curve("fuse", [(0, 0, 1.15), (0.06, -0.04, 1.32), (0.24, -0.08, 1.4), (0.36, -0.12, 1.36)], 0.03, M("tan"))
    spark = material("spark", hexc("#FFE45C"), emit=3.0, sss=0.0, coat=0.0)
    star("spark", (0.42, -0.14, 1.36), 0.17, 0.07, 0.05, spark, rot=(R(80), 0, R(-20)), points=6)
    sphere("spark_core", (0.42, -0.14, 1.36), 0.07, (1, 1, 1), material("spark_w", WHITE, emit=4.0))
    eyes(-0.48, 0.66)
    curve("smile", [(-0.12, -0.53, 0.48), (0, -0.56, 0.43), (0.12, -0.53, 0.48)], 0.02, M("#FFFFFF", sss=0.0))


PROPS = {
    "item_food_basic": item_food_basic, "item_food_lunch": item_food_lunch, "item_care_shampoo": item_care_shampoo,
    "item_care_brush": item_care_brush, "item_care_vitamins": item_care_vitamins, "item_fun_ball": item_fun_ball,
    "item_fun_bow": item_fun_bow, "item_fun_balloon": item_fun_balloon, "item_fun_book": item_fun_book, "item_fun_tent": item_fun_tent,
    "goal_scooter": goal_scooter, "goal_paints": goal_paints, "goal_lego": goal_lego, "goal_zoo": goal_zoo,
    "goal_custom": goal_custom,
    "tile_coin": tile_coin, "tile_apple": tile_apple, "tile_gift": tile_gift, "tile_piggy": tile_piggy, "tile_star": tile_star,
    "tile_bomb": tile_bomb, "ui_coin": ui_coin,
}
# per-prop framing: (size px, fill fraction, azimuth deg (0 = straight from -Y, + = camera to the left), elevation deg)
VIEWS = {name: (512, 0.9, 32, 24) for name in PROPS}
VIEWS.update({n: (256, 0.96, 22, 38) for n in PROPS if n.startswith("tile_")})
VIEWS.update({"ui_coin": (256, 0.92, 0, 0), "item_care_brush": (512, 0.9, 32, 40), "goal_paints": (512, 0.9, 28, 42),
              "goal_lego": (512, 0.9, 34, 32), "item_food_basic": (512, 0.9, 30, 36), "tile_coin": (256, 0.96, 10, 30)})


def fit_camera(scene, fill, azim, elev, lens=60):
    """Aim a camera from (azim, elev) at the built geometry and move it until the bbox fills `fill` of the frame."""
    dg = bpy.context.evaluated_depsgraph_get()
    pts = []
    for o in scene.objects:
        if o.type in ("MESH", "CURVE") and o.name != "ground":
            ev = o.evaluated_get(dg)
            if o.type == "CURVE":  # curve bound_box is bogus (bevel profile), use the tessellated mesh
                me = ev.to_mesh(); pts += [ev.matrix_world @ v.co for v in me.vertices]; ev.to_mesh_clear()
            else:
                pts += [ev.matrix_world @ Vector(c) for c in ev.bound_box]
    lo = Vector([min(p[i] for p in pts) for i in range(3)]); hi = Vector([max(p[i] for p in pts) for i in range(3)])
    target = (lo + hi) / 2; size = (hi - lo).length
    d = Vector((-math.sin(R(azim)) * math.cos(R(elev)), -math.cos(R(azim)) * math.cos(R(elev)), math.sin(R(elev))))
    dist = size * 2.2
    cam = camera(scene, target + d * dist, target, lens=lens)
    tgt = bpy.data.objects["cam_t"]
    for _ in range(5):
        bpy.context.view_layer.update()
        uv = [world_to_camera_view(scene, cam, p) for p in pts]
        xs = [u.x for u in uv]; ys = [u.y for u in uv]
        ext = max(max(xs) - min(xs), max(ys) - min(ys))
        cx = (max(xs) + min(xs)) / 2 - 0.5; cy = (max(ys) + min(ys)) / 2 - 0.5
        view = dist * 36.0 / lens  # frame width at the target distance (36 mm sensor)
        m = cam.matrix_world.to_3x3()
        target = target + m @ Vector((cx * view, cy * view, 0))
        dist *= ext / fill
        tgt.location = target; cam.location = target + d * dist
    bpy.context.view_layer.update()
    return cam


def render_prop(name, out, samples):
    size, fill, azim, elev = VIEWS[name]
    scene = reset_scene(samples, size)
    PROPS[name]()
    studio(scene, target=(0, 0, 0.6))
    fit_camera(scene, fill, azim, elev)
    # no shadow catcher: props float in UI panels; the app draws contact shadows where needed
    render(scene, out)


if __name__ == "__main__":
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    ap = argparse.ArgumentParser()
    ap.add_argument("--all", default=None); ap.add_argument("--only", default=None)
    ap.add_argument("--out", default=None); ap.add_argument("--samples", type=int, default=96)
    A = ap.parse_args(argv)
    if A.all:
        os.makedirs(A.all, exist_ok=True)
        for n in PROPS: render_prop(n, os.path.join(A.all, n + ".png"), A.samples)
    elif A.only:
        for n in A.only.split(","): render_prop(n, A.out or os.path.abspath(n + ".png"), A.samples)
    else:
        ap.error("use --all DIR or --only NAME")
