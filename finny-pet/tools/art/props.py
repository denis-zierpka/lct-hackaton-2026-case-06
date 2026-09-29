"""Toy-style 3D props for the Finny game flavour: shop items, room things, goals, match-3 tiles, UI coin, bakery pastries.

All:  Blender -b -P tools/art/props.py -- --all /abs/outdir [--samples 96]
One:  Blender -b -P tools/art/props.py -- --only tile_apple [--out /abs/file.png]
Many: Blender -b -P tools/art/props.py -- --only a,b --out DIR
Room: Blender -b -P tools/art/props.py -- --only item_fun_rug --out F   (a name of ROOM goes through render_thing)

Every builder puts its object at the origin, standing on z = 0, front facing -Y, and returns nothing.
The camera is fitted automatically to the bounding box of what was built (see `fit_camera`), so a
prop only has to be modelled; per-prop framing lives in `VIEWS`. Same studio rig as pet.py.
Room things (№ 28 а, ROOM = {name: slot}; "wall": back face in y = 0, centred on x = 0, z = 0; no curve()): `render_thing`
builds them in the furniture world of room.py (`room_camera`, `sprite_frame`) on an empty at ANCHOR, filling ROOM_FILL of
ITEM_PX. Names: item_<id>, starters item_home_<id>, goal_<id>, event posters poster_<itemId>.
"""
import sys, os, math, argparse, random
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__))); from lib import *
import bpy
from bpy_extras.object_utils import world_to_camera_view
from mathutils import Vector
import room

R = math.radians
WHITE = (1, 1, 1, 1)
ITEM_PX = 216  # 72 dp × 3 px/dp: the largest box of an item or a dream (SavingsScreen)
ROOM_FILL = 0.9
THING_PX = {"item_home_armchair": 324}  # № 120: the armchair by the door is 108 dp (3 × 36), the rest ITEM_PX
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


def rod(name, p, q, r, mat):  # a stick or a string without curve(): a curve's bound_box is bogus (see fit_camera)
    p, q = Vector(p), Vector(q)
    return cylinder(name, (p + q) / 2, r, (q - p).length, mat, rot=(q - p).to_track_quat("Z", "Y").to_euler())


def bubbles(pts):  # soap bubbles (x, y, z, r) with a glint: see-through may be only glints and bubbles (№ 66 б)
    for x, y, z, r in pts:
        sphere("bubble", (x, y, z), r, mat=M("blue", alpha=0.75, rough=0.05, sss=0.0, coat=1.0))
        sphere("glint", (x - 0.35 * r, y - 0.85 * r, z + 0.35 * r), 0.2 * r, (1, 0.5, 1.4), m_glint())


def super_can(loc, s):  # the bright «Супер-корм» can of the shelf and the Пк3 poster: a burst and a paw, no letters (ТЗ 3.5)
    P = lambda x, y, z: (loc[0] + x * s, loc[1] + y * s, loc[2] + z * s)
    cylinder("can", P(0, 0, 0.62), 0.46 * s, 1.24 * s, M("accent"), bevel=0.05 * s)
    cylinder("can_label", P(0, 0, 0.62), 0.47 * s, 0.62 * s, M("purple"), bevel=0.02 * s)
    for z in (0.05, 1.19): torus("can_rim", P(0, 0, z), 0.46 * s, 0.05 * s, m_metal("grey"))
    star("burst", P(0, -0.47, 0.62), 0.34 * s, 0.22 * s, 0.03 * s, M("yellow"), rot=(R(90), 0, 0), points=10)
    sphere("paw", P(0, -0.5, 0.57), 0.09 * s, (1, 0.4, 0.85), M("brown"))
    for i in (-1.5, -0.5, 0.5, 1.5): sphere("toe", P(0.07 * i, -0.5, 0.69 - 0.02 * abs(i)), 0.035 * s, (1, 0.4, 1.1), M("brown"))
    sphere("glint", P(-0.3, -0.37, 0.95), 0.05 * s, (1, 0.5, 2.5), m_glint())


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


def item_care_vitamins():  # «Ванна с пеной» (№ 16, the id stays): a tub on feet full of foam, bubbles; no pills, no crosses
    cylinder("tub", (0, 0, 0.55), 0.5, 0.7, M("#3D78B0"), scale=(1.7, 1, 1), bevel=0.2)
    for sx, sy in ((-1, -1), (-1, 1), (1, -1), (1, 1)): sphere("foot", (0.62 * sx, 0.26 * sy, 0.1), 0.1, mat=M("gold_dk"))
    for x, y, r in ((-0.5, 0, 0.26), (-0.12, 0.05, 0.3), (0.3, -0.02, 0.28), (0.62, 0.02, 0.2)):
        sphere("foam", (x, y, 0.9), r, (1, 1, 0.75), M("#FFFFFF", rough=0.8, sss=0.2))
    bubbles(((-0.3, -0.1, 1.3, 0.1), (0.35, 0.0, 1.25, 0.12)))


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
    tent = M("#E08A00"); trim = M("accent")  # № 66 б: amber, the yellow edge was 2.2 : 1 on white
    t = cone("tent", (0, 0, 0.82), 1.05, 1.64, (0, 0, 0), tent); t.modifiers["bev"].width = 0.12
    for z, r in ((0.35, 0.83), (0.85, 0.5)):
        torus("stripe", (0, 0, z), r, 0.045, trim)
    torus("kant", (0, 0, 0.05), 1.0, 0.05, M("purple"))  # № 66 б: a dark hem along the bottom edge
    a = R(0)  # door faces -Y, the room camera (render_thing), so it reads as an entrance
    sphere("door", (-0.7 * math.sin(a), -0.7 * math.cos(a), 0.46), 0.46, (1, 0.2, 1.5), M("purple"), (R(32), 0, -a))
    for a in (0.3, 2.4, 4.4):
        cylinder("pole", (0.16 * math.cos(a), 0.16 * math.sin(a), 1.85), 0.045, 0.9, M("tan"),
                 rot=(R(-16) * math.sin(a), R(16) * math.cos(a), 0))
    cone("flag", (0.13, -0.05, 2.12), 0.14, 0.24, (R(90), 0, 0), trim, scale=(1, 0.2, 1))


def item_food_porridge():  # a deep bowl, a mound with butter, a wooden spoon across above the rim (not the kibble bowl)
    cone("bowl", (0, 0, 0.42), 0.46, 0.84, (0, 0, 0), M("red"), r2=0.8)
    torus("rim", (0, 0, 0.84), 0.8, 0.07, M("red"))
    sphere("porridge", (0, 0, 0.84), 0.76, (1, 1, 0.36), M("cream", rough=0.8))
    box("butter", (0.14, -0.1, 1.1), (0.24, 0.2, 0.1), M("yellow"), rot=(0, 0, R(25)), bevel=0.03)
    sphere("spoon", (-0.28, 0.02, 1.0), 0.17, (1, 0.7, 0.3), M("brown"))
    rod("spoon", (-0.28, 0.02, 1.0), (0.95, 0.3, 1.5), 0.045, M("brown"))


def item_food_super():  # «Супер-корм» (Пк3): bright, but a food; the portion not bigger than the bowl
    super_can((0, 0, 0), 1.0)


def item_care_soap():  # a rounded bar on a dish, bubbles with a glint (not the bath)
    cylinder("dish", (0, 0, 0.07), 0.6, 0.14, M("#3D78B0"), scale=(1.3, 1, 1), bevel=0.06)
    box("soap", (0, 0, 0.32), (0.92, 0.56, 0.34), M("#E0457B"), bevel=0.15)
    bubbles(((0.2, -0.1, 0.62, 0.14), (-0.18, 0.05, 0.7, 0.1), (0.42, 0.05, 0.86, 0.08)))


def item_care_shampoo_simple():  # a tall lavender bottle with shoulders, lower than item_care_shampoo: a narrow flip-top cap (lid open, nozzle) instead of the pump, a yellow label with the blue drop, foam bubbles
    body = M("lav")
    cylinder("body", (0, 0, 0.53), 0.4, 1.06, body, scale=(1, 0.72, 1), bevel=0.12)
    sphere("shoulder", (0, 0, 1.04), 0.4, (1, 0.72, 0.55), body)
    cylinder("label", (0, 0, 0.5), 0.412, 0.5, M("yellow"), scale=(1, 0.72, 1), bevel=0.02)
    sphere("drop", (0, -0.3, 0.46), 0.1, (1, 0.35, 1.0), M("blue"))
    cone("drop_tip", (0, -0.3, 0.59), 0.09, 0.2, (0, 0, 0), M("blue"), scale=(1, 0.35, 1))
    cylinder("cap", (0, 0, 1.3), 0.13, 0.14, M("purple"), bevel=0.03)
    cylinder("cap_lid", (0, 0.175, 1.49), 0.13, 0.04, M("purple"), rot=(R(70), 0, 0), bevel=0.015)  # hinged at the back, open
    cylinder("nozzle", (0, -0.03, 1.4), 0.035, 0.08, M("lav"))
    bubbles(((0.33, -0.1, 1.36, 0.09), (0.46, -0.05, 1.55, 0.07), (-0.3, -0.12, 1.3, 0.06)))


def item_fun_icecream():  # a waffle cone point down with a grid, two scoops, no cherry (not the cupcake)
    cone("cone", (0, 0, 0.6), 0.03, 1.2, (0, 0, 0), M("#C68642"), r2=0.36)
    for z in (0.4, 0.7, 1.0): torus("grid", (0, 0, z), 0.03 + 0.3 * z / 1.2, 0.018, M("brown"))
    for a in range(0, 360, 45): rod("grid", (0, 0, 0.08), (0.37 * math.cos(R(a)), 0.37 * math.sin(R(a)), 1.2), 0.018, M("brown"))
    sphere("scoop", (0, 0, 1.3), 0.4, (1, 1, 0.85), M("#E0457B"))
    sphere("scoop", (0.03, 0, 1.7), 0.32, (1, 1, 0.9), M("brown"))


def item_fun_carousel():  # a round platform, a centre pole, a striped dome with a flag, three horses on poles
    gold = M("gold")
    cylinder("platform", (0, 0, 0.1), 1.0, 0.2, M("#3D78B0"), bevel=0.05)
    cylinder("carousel_pole", (0, 0, 0.95), 0.08, 1.5, gold)
    cone("dome", (0, 0, 1.95), 1.08, 0.6, (0, 0, 0), M("accent"))
    torus("valance", (0, 0, 1.66), 1.06, 0.07, M("purple"))
    for a in range(0, 360, 45): rod("dome_stripe", (0, 0, 2.2), (1.02 * math.cos(R(a)), 1.02 * math.sin(R(a)), 1.68), 0.05, M("yellow"))
    rod("mast", (0, 0, 2.2), (0, 0, 2.62), 0.02, gold)
    cone("flag", (0.14, 0, 2.5), 0.12, 0.26, (0, R(90), 0), M("accent"), scale=(1, 0.25, 1))
    for t in (R(0), R(120), R(240)):  # t — the ride direction at a horse
        f = lambda d, h: (0.66 * math.sin(t) + d * math.cos(t), -0.66 * math.cos(t) + d * math.sin(t), h)  # d along the ride
        rod("horse_pole", f(0, 0.2), f(0, 1.64), 0.025, gold)
        sphere("horse", f(0, 0.8), 0.22, (1.5, 0.6, 0.75), M("#FFFFFF"), (0, 0, t))
        sphere("horse_head", f(0.32, 1.02), 0.12, (1.4, 0.7, 0.8), M("#FFFFFF"), (0, 0, t))
        sphere("mane", f(0.22, 1.06), 0.08, (1, 0.6, 1.2), M("accent"))
        for d in (-0.22, 0.22): rod("horse_leg", f(d, 0.75), f(1.3 * d, 0.45), 0.035, M("#FFFFFF"))


def item_gift_card():  # folded, standing like a little house, a heart on the front (not an envelope: pocket money)
    box("card", (0, 0, 0.55), (1.0, 0.03, 1.1), M("#7468CC"), bevel=0.01)
    box("card_back", (0, 0.3, 0.55), (1.0, 0.03, 1.25), M("#7468CC"), rot=(R(28.6), 0, 0), bevel=0.01)
    box("heart", (0, -0.03, 0.52), (0.3, 0.03, 0.3), M("accent"), rot=(0, R(45), 0), bevel=0.02)
    for sx in (-1, 1): sphere("heart", (0.106 * sx, -0.03, 0.626), 0.15, (1, 0.2, 1), M("accent"))


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


def goal_camp():  # dream «Походная палатка»: a gable tent (a prism, not the toy cone), the entrance open, pegs, lines, a fir
    tri = (R(90), 0, 0)  # a 3-sided cylinder: a corner up, the ridge along Y
    cylinder("camp", (0, 0, 0.45), 0.9, 1.5, M("#2A9D8F"), scale=(1.15, 1, 1), rot=tri, vertices=3, bevel=0.03)
    cylinder("camp_door", (0, -0.76, 0.3), 0.6, 0.02, M("navy"), scale=(1.15, 1, 1), rot=tri, vertices=3)
    for s in (-1, 1):
        rod("camp_flap", (0.03 * s, -0.8, 0.9), (0.57 * s, -0.8, 0.03), 0.05, M("mint"))
        rod("camp_line", (0, 0.75 * s, 1.3), (0, 1.3 * s, 0.02), 0.012, M("brown"))
        cylinder("camp_peg", (0, 1.3 * s, 0.05), 0.03, 0.12, M("brown"))
    cylinder("fir_trunk", (1.35, 0.3, 0.12), 0.07, 0.24, M("brown"))
    for k in range(3): cone("fir", (1.35, 0.3, 0.45 + 0.32 * k), 0.42 - 0.1 * k, 0.55, (0, 0, 0), M("#3E8E4A"))


def poster_food_super():  # Пк3 poster, front view: a deep burst-edged sheet, long rays, the super can big, a bowl of kibble in front, a shining heart over the can, small sparks, no letters
    rot = (R(90), 0, 0)
    star("poster", (0, 0.08, 1.05), 1.1, 0.84, 0.05, M("purple"), rot=rot, points=12)
    star("poster_in", (0, 0.04, 1.05), 1.0, 0.76, 0.05, M("yellow"), rot=rot, points=12)
    for a in range(0, 360, 30): box("ray", (0.7 * math.cos(R(a)), 0, 1.05 + 0.7 * math.sin(R(a))), (0.07, 0.02, 0.56), M("#FF8C42"), rot=(0, R(90 - a), 0), bevel=0.01)
    super_can((0, -0.28, 0.66), 0.62)
    for x, z, r in ((-0.6, 1.45, 0.14), (-0.62, 0.7, 0.1), (0.6, 1.5, 0.12)): star("spark", (x, -0.04, z), r, 0.45 * r, 0.04, M("accent"), rot=rot)
    cone("bowl", (0, -0.75, 0.52), 0.21, 0.19, (0, 0, 0), M("blue"), r2=0.3)
    torus("bowl_rim", (0, -0.75, 0.61), 0.28, 0.03, M("sky"))
    for i, (x, z) in enumerate(((-0.18, 0), (-0.06, 0), (0.06, 0), (0.18, 0), (-0.12, 0.1), (0, 0.1), (0.12, 0.1))): sphere("kibble", (x, -0.78 - 0.03 * (i % 2), 0.66 + z), 0.065, mat=M("#B9743F", rough=0.6))
    box("heart", (0, -0.3, 1.66), (0.2, 0.06, 0.2), M("accent", emit=0.6), rot=(0, R(45), 0), bevel=0.03)
    for sx in (-1, 1): sphere("heart", (0.0707 * sx, -0.3, 1.7307), 0.1, (1, 0.45, 1), M("accent", emit=0.6))
    sphere("glint", (-0.08, -0.36, 1.77), 0.035, (1, 0.5, 1.4), m_glint())


def item_fun_rug():  # room thing (render_thing, ANCHOR "rug"): a rounded rectangle 1.6 : 1 with a dark border, stripes across, dense fringe on the short edges
    for sx, sy, z, c in ((2.4, 1.5, 0.03, "purple"), (2.16, 1.26, 0.045, "accent")): box("rug", (0, 0, z), (sx, sy, 0.06), M(c), bevel=0.12)  # bevel of the unit cube: round corners
    for x, c in ((-0.66, "yellow"), (-0.22, "blue"), (0.22, "yellow"), (0.66, "blue")): box("rug_stripe", (x, 0, 0.06), (0.2, 1.2, 0.06), M(c), bevel=0.02)
    for sx in (-1, 1):
        for k in range(-6, 7): cylinder("fringe", (sx * 1.34, 0.09 * k, 0.024), 0.024, 0.34, M(("cream", "yellow")[k % 2]), rot=(0, R(90), 0), vertices=12)


def item_fun_starlamp():  # a night lamp: foot, stem, a glowing star in a dark rim (not the gold star of goal_custom)
    cylinder("lamp_foot", (0, 0, 0.08), 0.4, 0.16, M("lav"), bevel=0.06)
    cylinder("lamp_stem", (0, 0, 0.68), 0.06, 1.04, M("purple"))
    star("lamp_rim", (0, 0.03, 1.4), 0.62, 0.3, 0.12, M("purple"), rot=(R(90), 0, 0))
    star("lamp_star", (0, -0.03, 1.4), 0.55, 0.25, 0.16, M("#FFE45C", emit=2.0), rot=(R(90), 0, 0))


def item_fun_picture():  # on the wall: a thick frame darker than the wall; sun, hill and a little house inside (not the window)
    box("canvas", (0, -0.02, 0), (1.3, 0.04, 0.9), M("sky"), bevel=0)
    for x, z, w, h in ((0, 0.52, 1.58, 0.14), (0, -0.52, 1.58, 0.14), (-0.72, 0, 0.14, 1.18), (0.72, 0, 0.14, 1.18)): box("frame", (x, -0.07, z), (w, 0.14, h), M("brown"), bevel=0.03)
    cone("hill", (0.25, -0.06, -0.2), 0.5, 0.5, (0, 0, 0), M("#3E8E4A"), scale=(1, 0.08, 1))
    sphere("sun", (-0.38, -0.06, 0.22), 0.13, (1, 0.3, 1), M("yellow", emit=0.5))
    box("house", (-0.2, -0.1, -0.3), (0.3, 0.04, 0.26), M("accent"), bevel=0.02)
    cone("roof", (-0.2, -0.1, -0.1), 0.24, 0.18, (0, 0, 0), M("purple"), scale=(1, 0.17, 1))


def item_fun_robot():  # friendly: box body and head, an antenna, round glowing eyes and a smile of lights, arms, wheels
    b, d, glow = M("#3D78B0"), M("navy"), M("mint", emit=2.0)
    box("robot", (0, 0, 0.75), (0.8, 0.56, 0.7), b, bevel=0.1)
    box("robot_head", (0, 0, 1.36), (0.66, 0.5, 0.46), b, bevel=0.12)
    box("robot_panel", (0, -0.28, 0.78), (0.42, 0.02, 0.28), M("yellow"), bevel=0.03)
    cylinder("antenna", (0, 0, 1.7), 0.025, 0.26, d)
    sphere("antenna_tip", (0, 0, 1.86), 0.08, mat=M("accent"))
    for sx in (-1, 1):
        cylinder("wheel", (0.3 * sx, -0.1, 0.2), 0.2, 0.14, d, rot=(R(90), 0, 0), bevel=0.04)
        rod("arm", (0.4 * sx, 0, 0.92), (0.62 * sx, -0.06, 0.55), 0.07, b)
        sphere("hand", (0.64 * sx, -0.07, 0.5), 0.1, mat=M("accent"))
        sphere("eye", (0.15 * sx, -0.25, 1.4), 0.09, (1, 0.5, 1), glow)
    for i in range(-2, 3): sphere("smile", (0.06 * i, -0.26, 1.24 + 0.012 * i * i), 0.025, mat=glow)


def item_fun_kite():  # on the wall: a two-colour diamond on a cross of sticks, a zigzag tail with bows (over a third of the height)
    rot = (R(90), 0, 0)
    cylinder("kite", (0, -0.03, 0.5), 0.6, 0.04, M("accent"), scale=(0.75, 1, 1), rot=rot, vertices=4, bevel=0.02)
    cylinder("kite_in", (0, -0.06, 0.5), 0.34, 0.04, M("yellow"), scale=(0.75, 1, 1), rot=rot, vertices=4, bevel=0.02)
    rod("kite_stick", (0, -0.09, -0.08), (0, -0.09, 1.08), 0.02, M("brown"))
    rod("kite_stick", (-0.44, -0.09, 0.5), (0.44, -0.09, 0.5), 0.02, M("brown"))
    pts = [(0, -0.1), (0.12, -0.4), (-0.1, -0.7), (0.1, -1.0)]
    for (x0, z0), (x1, z1) in zip(pts, pts[1:]): rod("kite_tail", (x0, -0.05, z0), (x1, -0.05, z1), 0.015, M("purple"))
    for x, z in pts[1:]:
        for sx in (-1, 1): cone("bow", (x + 0.07 * sx, -0.06, z), 0.06, 0.14, (0, R(-90 * sx), 0), M("accent"), scale=(1, 0.4, 1))


def item_fun_spinner():  # a pinwheel: four curled glowing blades on a stick in a round stand (not the flower in a pot)
    cylinder("stand", (0, 0, 0.1), 0.3, 0.2, M("purple"), bevel=0.06)
    cylinder("stick", (0, 0, 0.8), 0.04, 1.3, M("brown"))
    for a, c in zip((20, 110, 200, 290), ("accent", "#3D78B0", "gold_dk", "#3E8E4A")):
        sphere("blade", (0.27 * math.cos(R(a)), -0.08, 1.45 + 0.27 * math.sin(R(a))), 0.3, (1, 0.1, 0.42), M(c, emit=0.4), (0, -R(a + 35), 0))
    sphere("hub", (0, -0.14, 1.45), 0.07, mat=M("yellow"))


def item_home_flower():  # starter thing of spot_1: a dark plum pot with a rim, dark green leaves and stems, three bright flowers in a thin purple kant (edge to the floor ≥ 3 : 1)
    pot, green = M("#2A0C3E", rough=0.8, sss=0.0, coat=0.0), M("#0B2410", rough=0.8, sss=0.0, coat=0.0)
    cone("pot", (0, 0, 0.3), 0.32, 0.6, (0, 0, 0), pot, r2=0.44)
    torus("pot_rim", (0, 0, 0.6), 0.45, 0.07, pot)
    cylinder("soil", (0, 0, 0.6), 0.42, 0.05, M("brown"))
    for i, a in enumerate((-55, -20, 20, 55)):
        sphere("leaf", (0.3 * math.sin(R(a)), 0.1 * (-1) ** i, 0.62 + 0.3 * math.cos(R(a))), 0.3, (0.38, 0.14, 1), green, (0, R(a), 0))
    for x, z in ((-0.3, 1.3), (0.05, 1.55), (0.33, 1.22)):
        rod("stem", (x * 0.3, 0, 0.6), (x, -0.04, z), 0.025, green)
        for b in range(90, 450, 72): sphere("petal", (x + 0.1 * math.cos(R(b)), -0.08, z + 0.1 * math.sin(R(b))), 0.08, (1, 0.4, 1), M("accent"))
        for b in range(90, 450, 72): sphere("petal_kant", (x + 0.1 * math.cos(R(b)), -0.06, z + 0.1 * math.sin(R(b))), 0.105, (1, 0.4, 1), M("purple", rough=0.8, sss=0.0, coat=0.0))
        sphere("flower", (x, -0.12, z), 0.06, (1, 0.5, 1), M("yellow"))


def item_home_armchair():  # starter thing of spot_4: soft — seat, cushion, back, two armrests; teal, not the lavender wall
    c = M("#2A9D8F", rough=0.7)  # A1d3 (owner): no legs — the soft body stands on the floor itself (was the same body 0.25 higher)
    box("chair", (0, 0, 0.17), (1.0, 0.86, 0.34), c, bevel=0.12)
    box("chair_back", (0, 0.32, 0.59), (1.0, 0.24, 1.18), c, bevel=0.12)
    box("cushion", (0, -0.06, 0.41), (0.96, 0.74, 0.18), M("mint", rough=0.7), bevel=0.08)
    for sx in (-1, 1):
        box("armrest", (0.6 * sx, 0, 0.36), (0.24, 0.9, 0.72), c, bevel=0.12)


def item_home_lamp():  # бра (home_lamp and lamp_new) on the wall: a plate, a bent arm, a bell shade, the bulb lit
    arm = M("purple")
    cylinder("lamp_plate", (0, -0.03, 0.3), 0.13, 0.06, arm, rot=(R(90), 0, 0), bevel=0.02)
    rod("lamp_arm", (0, -0.03, 0.3), (0, -0.5, 0.42), 0.035, arm)
    rod("lamp_arm", (0, -0.5, 0.42), (0, -0.5, 0.26), 0.035, arm)
    cone("lamp_shade", (0, -0.5, 0.06), 0.34, 0.42, (0, 0, 0), M("accent"), r2=0.13)
    sphere("lamp_bulb", (0, -0.5, -0.17), 0.12, mat=M("#FFE45C", emit=3.0))


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


# ---------------------------------------------------------------- bakery pastries (TOWN-J1-1b3, ids of the job_bakery menu)
# Told apart by silhouette first; the four crusts step in tone (baguette light, croissant amber, bread brown, pretzel
# dark). Bread and baguette are not the background's shelf (place.py bread_shelf: round loaf, baton, upright baguettes).

def pastry_croissant():
    """Crescent of rolled ridges, horns towards the camera (like 🥐): a tapered core, fat in the middle, and bands."""
    core, band = M("#D3822A", rough=0.45, coat=0.4), M("#EDA744", rough=0.45, coat=0.4)
    rc = lambda s: 0.28 * (1 - 0.75 * s)  # core radius at s = 0 (middle) … 1 (tip), as curve(taper=True) draws it
    at = lambda a, s: (0.62 * math.cos(R(a)), 0.62 * math.sin(R(a)) - 0.2, rc(s))
    for side in (1, -1):
        curve("croissant", [at(90 + side * 23 * k, k / 5) for k in range(6)], 0.28, core, taper=True)
        sphere("tip", at(90 + side * 115, 1), rc(1), mat=core)  # rounds the flat cap of the curve
    for s in (-0.68, -0.34, 0.0, 0.34, 0.68):
        a = 90 + 115 * s
        sphere("ridge", at(a, abs(s)), rc(abs(s)), (1.2, 0.85, 1.1), band, (0, 0, R(a)))


def pastry_bread():
    """Tin loaf «кирпичик»: lighter brick sides, a darker domed top, three slashes across it."""
    side, top, cut = M("#B9793C", rough=0.6, coat=0.1), M("#A05E28", rough=0.6, coat=0.1), M("#D9A560", rough=0.7, coat=0.0)
    box("loaf", (0, 0, 0.3), (1.5, 0.82, 0.6), side, bevel=0.1)
    sphere("dome", (0, 0, 0.55), 0.5, (1.52, 0.84, 0.56), top)
    for x in (-0.44, 0.0, 0.44):
        sphere("cut", (x, 0, 0.8 - 0.25 * x * x), 0.06, (1.3, 4.6, 0.5), cut, rot=(0, 0, R(40)), levels=1)


def pastry_baguette():
    """Long thin loaf lying diagonally, oblique slashes along its top (the shelf's stand upright)."""
    crust, cut = M("#E2B56E", rough=0.5, coat=0.2), M("#FFF0CF", rough=0.6, coat=0.0)
    under = M("#865024", rough=0.7, coat=0.0)  # № 66 б: the darker hearth-baked crust shows along the rim and the tips
    a = R(35)
    sphere("baguette", (0, 0, 0.2), 0.2, (6.5, 1.0, 0.95), under, (0, 0, a))
    # the light top crust: a smaller loaf lifted 0.045 up-front (40° off vertical), only its cap breaks through
    sphere("crust", (0.0166, -0.0237, 0.2345), 0.2, (6.3, 0.9, 0.855), crust, (0, 0, a))
    for t in (-0.8, -0.4, 0.0, 0.4, 0.8):
        z = 0.2 + 0.19 * math.sqrt(1 - (t / 1.3) ** 2)
        sphere("cut", (t * math.cos(a), t * math.sin(a), z), 0.055, (3.6, 0.8, 0.5), cut, (0, 0, a + R(40)), levels=1)


def pastry_pretzel():
    """Pretzel loop: a belly at the front, arms twisted in the middle, three see-through holes; coarse salt."""
    dough = M("#6B3417", rough=0.3, coat=0.7)
    pts = [(-0.44, -0.47, 0.12), (0.0, 0.08, 0.2), (0.3, 0.38, 0.14), (0.62, 0.46, 0.14), (0.88, 0.1, 0.14),
           (0.66, -0.36, 0.14), (0.0, -0.56, 0.14), (-0.66, -0.36, 0.14), (-0.88, 0.1, 0.14), (-0.62, 0.46, 0.14),
           (-0.3, 0.38, 0.14), (0.0, 0.02, 0.1), (0.44, -0.47, 0.12)]  # the ends sink into the belly
    curve("pretzel", pts, 0.13, dough)
    salt = M("#FFFFFF", rough=0.4, sss=0.0, coat=0.2); rnd = random.Random(5)
    for x, y, z in pts[2:11:2] + [(0.52, 0.32, 0.14), (-0.52, 0.32, 0.14), (-0.25, -0.5, 0.14), (0.25, -0.5, 0.14)]:
        box("salt", (x, y, z + 0.125), (0.05, 0.05, 0.04), salt, rot=(0, 0, rnd.uniform(0, 3)), bevel=0.01)


def pastry_donut():
    """Ring with pink glaze and sprinkles, the hole seen from above."""
    torus("donut", (0, 0, 0.24), 0.5, 0.24, M("#E6A866", rough=0.5, coat=0.2))
    torus("glaze", (0, 0, 0.32), 0.5, 0.245, M("#FF7FAF", rough=0.25, coat=0.8), scale=(1, 1, 0.72))
    rnd = random.Random(9); cols = ("yellow", "mint", "blue", "#FFFFFF", "lav")
    for i in range(16):
        a = i * 2 * math.pi / 16 + rnd.uniform(-0.15, 0.15); r = 0.5 + rnd.uniform(-0.12, 0.12)
        capsule("sprinkle", (r * math.cos(a), r * math.sin(a), 0.49), 0.025, 0.12, M(cols[i % 5], sss=0.0),
                (R(90), 0, rnd.uniform(0, 3)))


def pastry_cupcake():
    """Pleated paper cup under a swirl of cream with a cherry: the only tall one."""
    stripes = M("#3D78B0", sss=0.0), M("#6FA0CC", sss=0.0)  # № 66 б: darker than the palette blue / sky
    cylinder("cup_base", (0, 0, 0.02), 0.38, 0.04, stripes[0])
    for i in range(18):
        a = i * 2 * math.pi / 18
        box("pleat", (0.45 * math.cos(a), 0.45 * math.sin(a), 0.3), (0.035, 0.17, 0.6), stripes[i % 2],
            rot=(0, R(13), a), bevel=0.012)
    cream, roll = M("#F9B8D3", rough=0.35, coat=0.4), M("#E77FA9", rough=0.35, coat=0.4)  # № 66 б: deeper pink rolls
    for z, r, rr in ((0.66, 0.4, 0.17), (0.88, 0.28, 0.15), (1.06, 0.15, 0.12)):
        torus("cream", (0, 0, z), r, rr, roll)
        sphere("cream_core", (0, 0, z), r, (1, 1, rr / r), cream)
    sphere("cream_tip", (0, 0, 1.17), 0.1, (1, 1, 1.2), cream)
    sphere("cherry", (0, 0, 1.36), 0.12, (1, 1, 1), M("red", rough=0.2, coat=0.9))
    curve("cherry_stem", [(0, 0, 1.46), (0.05, 0, 1.56), (0.13, 0.02, 1.62)], 0.016, M("green"))


PROPS = {
    "item_food_basic": item_food_basic, "item_food_lunch": item_food_lunch, "item_care_shampoo": item_care_shampoo,
    "item_care_brush": item_care_brush, "item_care_vitamins": item_care_vitamins, "item_fun_ball": item_fun_ball,
    "item_fun_bow": item_fun_bow, "item_fun_balloon": item_fun_balloon, "item_fun_book": item_fun_book, "item_fun_tent": item_fun_tent,
    "goal_scooter": goal_scooter, "goal_paints": goal_paints, "goal_lego": goal_lego, "goal_zoo": goal_zoo,
    "goal_custom": goal_custom,
    "tile_coin": tile_coin, "tile_apple": tile_apple, "tile_gift": tile_gift, "tile_piggy": tile_piggy, "tile_star": tile_star,
    "tile_bomb": tile_bomb, "ui_coin": ui_coin,
    "pastry_croissant": pastry_croissant, "pastry_bread": pastry_bread, "pastry_baguette": pastry_baguette,
    "pastry_pretzel": pastry_pretzel, "pastry_donut": pastry_donut, "pastry_cupcake": pastry_cupcake,
    "item_food_porridge": item_food_porridge, "item_food_super": item_food_super, "item_care_soap": item_care_soap,
    "item_care_shampoo_simple": item_care_shampoo_simple, "item_fun_icecream": item_fun_icecream, "goal_camp": goal_camp,
    "poster_food_super": poster_food_super, "item_fun_carousel": item_fun_carousel, "item_gift_card": item_gift_card,
    "item_fun_rug": item_fun_rug, "item_fun_starlamp": item_fun_starlamp, "item_fun_picture": item_fun_picture,
    "item_fun_robot": item_fun_robot, "item_fun_kite": item_fun_kite, "item_fun_spinner": item_fun_spinner,
    "item_home_flower": item_home_flower, "item_home_armchair": item_home_armchair, "item_home_lamp": item_home_lamp,
}
ROOM = {"item_fun_rug": "rug", "item_fun_starlamp": "table", "item_fun_picture": "wall", "item_fun_robot": "floor",
        "item_fun_kite": "wall", "item_fun_spinner": "table", "item_fun_ball": "floor", "item_fun_book": "table",
        "item_fun_tent": "floor", "item_home_flower": "floor", "item_home_armchair": "floor", "item_home_lamp": "wall"}
# floor — the bottom of the spot row, 18° from above; "table" — the floor too (there is no table); wall — the centre of
# spot_2; rug — the floor at 29°, so the flat rounded rectangle reads as a rug (at -1.0 the same rectangle is ≈ 1 : 4, a line)
ANCHOR = {"floor": (0.0, -1.0, 0.0), "table": (0.0, -1.0, 0.0), "wall": (0.0, room.WALL_Y - 0.01, 2.25), "rug": (0.0, -5.0, 0.0)}
# per-prop framing: (size px, fill fraction, azimuth deg (0 = straight from -Y, + = camera to the left), elevation deg)
VIEWS = {name: (512, 0.9, 32, 24) for name in PROPS}
VIEWS.update({n: (256, 0.96, 22, 38) for n in PROPS if n.startswith("tile_")})
VIEWS.update({"ui_coin": (256, 0.92, 0, 0), "item_care_brush": (512, 0.9, 32, 40), "goal_paints": (512, 0.9, 28, 42),
              "goal_lego": (512, 0.9, 34, 32), "item_food_basic": (512, 0.9, 30, 36), "tile_coin": (256, 0.96, 10, 30)})
VIEWS.update({n: (256, 0.9, 22, 38) for n in PROPS if n.startswith("pastry_")})  # from above: the holes show
VIEWS.update({n: (ITEM_PX, 0.9, 32, 24) for n in ("item_food_super", "item_care_soap", "item_care_shampoo_simple",
                                                  "item_fun_icecream", "item_care_vitamins", "goal_camp", "item_fun_carousel", "item_gift_card")})
VIEWS.update({"item_food_porridge": (ITEM_PX, 0.9, 30, 36), "poster_food_super": (ITEM_PX, 0.9, 0, 0)})


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


def render_thing(name, out, samples):
    """A room thing (№ 28 а) in the world of room.render_sprite on an empty named after it at ANCHOR, scaled to ROOM_FILL."""
    scene = reset_scene(samples, width=1080, height=1920)
    p = room.palette()
    plank_floor(p["wood"], room.K); shell_walls(p["m_wall"], p["m_side"], p["m_white"], room.K, room.WALL_Y, window=None)
    shell = list(scene.objects)
    room.light_room(scene, False, None); cam = room.room_camera(scene)  # before the builder: key, fill, sun, cam keep their names
    old = set(scene.objects)
    PROPS[name]()
    new = [o for o in scene.objects if o not in old]
    e = bpy.data.objects.new(name, None); bpy.context.collection.objects.link(e); e.location = ANCHOR[ROOM[name]]
    for o in [o for o in new if o.parent is None]: o.parent = e
    wall = ROOM[name] == "wall"; floor = not wall
    for o in shell:  # the loop of room.render_sprite: the one № 79 decision lives in room.py
        o.is_shadow_catcher = room.SHADOW
        o.visible_camera = room.SHADOW and (floor and o.name.startswith("plank") or wall and o.name == "wall_back")
    objs, W, H, px = [o for o in new if o.type == "MESH"], scene.render.resolution_x, scene.render.resolution_y, THING_PX.get(name, ITEM_PX)
    for _ in range(6):  # perspective: measure again after each scaling (the bbox corners, as sprite_frame)
        bpy.context.view_layer.update()
        uv = [world_to_camera_view(scene, cam, o.matrix_world @ Vector(c)) for o in objs for c in o.bound_box]
        e.scale = e.scale * ROOM_FILL * px / max((max(q.x for q in uv) - min(q.x for q in uv)) * W, (max(q.y for q in uv) - min(q.y for q in uv)) * H)
    room.sprite_frame(scene, objs, px, px, floor=ROOM[name] != "wall")
    render(scene, out)


if __name__ == "__main__":
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    ap = argparse.ArgumentParser()
    ap.add_argument("--all", default=None); ap.add_argument("--only", default=None)
    ap.add_argument("--out", default=None); ap.add_argument("--samples", type=int, default=96)
    A = ap.parse_args(argv)
    if A.all:
        os.makedirs(A.all, exist_ok=True)
        for n in PROPS: (render_thing if n in ROOM else render_prop)(n, os.path.join(A.all, n + ".png"), A.samples)
    elif A.only:
        names = A.only.split(",")
        if A.out and (os.path.isdir(A.out) or len(names) > 1):
            os.makedirs(A.out, exist_ok=True)
            for n in names: (render_thing if n in ROOM else render_prop)(n, os.path.join(A.out, n + ".png"), A.samples)
        else:
            for n in names: (render_thing if n in ROOM else render_prop)(n, A.out or os.path.abspath(n + ".png"), A.samples)
    else:
        ap.error("use --all DIR or --only NAME")
