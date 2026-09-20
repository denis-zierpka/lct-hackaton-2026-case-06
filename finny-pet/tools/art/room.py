"""Cozy child's room: the game background the pet lives in (Blender, Cycles).

All four:   Blender -b -P tools/art/room.py -- --all /abs/outdir [--samples 160] [--preview]
One:        Blender -b -P tools/art/room.py -- --only room_land_evening --out /abs/room.png

Layout: floor is z = 0, the back wall stands at y = WALL_Y, the camera looks from -Y straight on and a little
from above so the floor fills the lower ~35 % of the frame. The centre-bottom third (the rug) is kept empty:
the app composites the pet sprite there and overlays trophies on the shelf. Portrait variants are the same
room re-framed: every x coordinate is squeezed by K so the composition fits a vertical camera.
"""
import sys, os, math, argparse, random
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__))); from lib import *

VARIANTS = {  # name: (width, height, evening)
    "room_land_day": (1920, 1080, False), "room_land_evening": (1920, 1080, True),
    "room_port_day": (1080, 1920, False), "room_port_evening": (1080, 1920, True),
}
WALL_Y = 3.0

argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
ap = argparse.ArgumentParser()
ap.add_argument("--all"); ap.add_argument("--only"); ap.add_argument("--out")
ap.add_argument("--samples", type=int, default=160); ap.add_argument("--preview", action="store_true")
A = ap.parse_args(argv)


def build_room(evening, portrait):
    k = 0.68 if portrait else 1.0  # horizontal squeeze for the vertical framing
    X = lambda x: x * k
    wood = [material("wood%d" % i, mix(hexc("#E9B983"), hexc("#D9A36C"), i / 3), rough=0.55, sss=0.1, coat=0.25) for i in range(4)]
    m_wall = material("wall", hexc("#EAE2F6"), rough=0.7, sss=0.0, coat=0.1)
    m_side = material("side", hexc("#DCD2EE"), rough=0.7, sss=0.0, coat=0.1)
    m_white = material("white", hexc("#FFFBF6"), rough=0.4, sss=0.1, coat=0.3)
    m_pink = material("pink", hexc("#FFD6E4"), rough=0.6, sss=0.3, coat=0.2)
    m_mag = material("magenta", hexc("#FF0053"), rough=0.5, sss=0.2, coat=0.3)
    m_purple = material("purple", hexc("#520978"), rough=0.45, sss=0.15, coat=0.4)
    m_lav = material("lavender", hexc("#8A83D1"), rough=0.5, sss=0.2, coat=0.3)
    m_gold = material("gold", hexc("#FFC94D"), rough=0.25, sss=0.0, coat=0.8)
    m_green = material("green", hexc("#7BC47F"), rough=0.5, sss=0.3, coat=0.3)
    m_green2 = material("green2", hexc("#5EAA66"), rough=0.5, sss=0.3, coat=0.3)
    m_blue = material("blue", hexc("#6FB1E0"), rough=0.5, sss=0.2, coat=0.3)

    # floor: planks running towards the wall (perspective lines), slightly different tints
    for i in range(-9, 10):
        box("plank", (X(i * 0.8), -3, -0.06), (X(0.78), 16, 0.12), wood[abs(i) % 4], bevel=0.015)

    # back wall with a window opening, side walls, skirting boards
    ww, wz0, wz1 = X(1.5), 1.9, 4.1  # window half-width, bottom, top
    box("wall_l", (X(-6) - ww, WALL_Y + 0.15, 7), (X(12), 0.3, 14), m_wall, bevel=0)
    box("wall_r", (X(6) + ww, WALL_Y + 0.15, 7), (X(12), 0.3, 14), m_wall, bevel=0)
    box("wall_b", (0, WALL_Y + 0.15, wz0 / 2), (2 * ww, 0.3, wz0), m_wall, bevel=0)
    box("wall_t", (0, WALL_Y + 0.15, (wz1 + 14) / 2), (2 * ww, 0.3, 14 - wz1), m_wall, bevel=0)
    for sx in (-1, 1):
        box("side", (X(5.2) * sx, -2, 7), (0.3, 10, 14), m_side, bevel=0)
        box("skirt_s", (X(5.05) * sx, -2, 0.11), (0.06, 10, 0.24), m_white, bevel=0.01)
    box("skirt", (0, WALL_Y - 0.03, 0.11), (X(10.2), 0.07, 0.24), m_white, bevel=0.01)

    # window: frame, cross bars, sill, curtains, rod
    t = 0.12
    for x, z, sxz in ((-ww - t / 2, (wz0 + wz1) / 2, (t, wz1 - wz0 + 2 * t)), (ww + t / 2, (wz0 + wz1) / 2, (t, wz1 - wz0 + 2 * t)),
                      (0, wz0 - t / 2, (2 * ww, t)), (0, wz1 + t / 2, (2 * ww, t)),
                      (0, (wz0 + wz1) / 2, (0.07, wz1 - wz0)), (0, (wz0 + wz1) / 2, (2 * ww, 0.07))):
        box("frame", (x, WALL_Y - 0.02, z), (sxz[0], 0.2, sxz[1]), m_white, bevel=0.02)
    box("sill", (0, WALL_Y - 0.12, wz0 - 0.2), (2 * ww + 0.5, 0.36, 0.12), m_white, bevel=0.03)
    rod = cylinder("rod", (0, WALL_Y - 0.28, wz1 + 0.42), 0.035, 2 * ww + 1.4, m_gold, rot=(0, math.radians(90), 0))
    for sx in (-1, 1):
        sphere("finial", ((ww + 0.7) * sx, WALL_Y - 0.28, wz1 + 0.42), 0.09, mat=m_gold, levels=1)
        for j in range(3):
            cylinder("fold", ((ww + 0.2 + j * 0.2) * sx, WALL_Y - 0.28, (wz1 + 0.35 + wz0 - 0.55) / 2), 0.13,
                     wz1 - wz0 + 0.9, m_pink, scale=(1, 0.7, 1), bevel=0.05)

    # sky behind the window
    if evening:
        sky = material("sky", hexc("#1B2160"), rough=1, sss=0, coat=0, emit=0.9)
        box("sky", (0, WALL_Y + 3.5, 3.5), (12, 0.1, 8), sky, bevel=0).visible_shadow = False
        star = material("star", (1, 0.98, 0.85, 1), emit=6.0, sss=0, coat=0)
        random.seed(7)
        for _ in range(26):
            sphere("star", (random.uniform(-ww * 0.95, ww * 0.95), WALL_Y + 3.2, random.uniform(wz0 + 0.25, wz1 - 0.15)),
                   random.uniform(0.025, 0.05), mat=star, levels=0)
        moon = material("moon", hexc("#FFF1B8"), emit=2.2, sss=0, coat=0)
        sphere("moon", (ww * 0.45, WALL_Y + 3.0, wz1 - 0.55), 0.32, mat=moon, levels=1)
        sphere("moon_cut", (ww * 0.45 + 0.17, WALL_Y + 2.85, wz1 - 0.45), 0.29, mat=sky, levels=1)
    else:
        sky = material("sky", hexc("#8ED4FF"), rough=1, sss=0, coat=0, emit=1.0)
        box("sky", (0, WALL_Y + 3.5, 3.5), (12, 0.1, 8), sky, bevel=0).visible_shadow = False
        cloud = material("cloud", (1, 1, 1, 1), emit=0.85, sss=0, coat=0)
        for cx, cz, s in ((-ww * 0.55, wz1 - 0.6, 0.28), (ww * 0.5, wz0 + 0.9, 0.24)):
            for dx, dz, r in ((0, 0, 1.0), (-1.1, -0.15, 0.75), (1.1, -0.1, 0.8), (0.5, 0.45, 0.7)):
                sphere("cloud", (cx + dx * s, WALL_Y + 3.0, cz + dz * s), r * s, mat=cloud, levels=1).visible_shadow = False

    # shelf on the left wall (empty: trophies are overlaid by the app)
    sh_x, sh_w = X(-3.35), X(1.9)
    box("shelf", (sh_x, WALL_Y - 0.25, 3.3), (sh_w, 0.5, 0.11), wood[1], bevel=0.02)
    for dx in (-0.32, 0.32):
        box("bracket", (sh_x + dx * sh_w, WALL_Y - 0.12, 3.12), (0.09, 0.24, 0.26), m_white, bevel=0.015)

    # pennant garland high on the wall (seen in the portrait framing) and a wall clock above the toy box
    n = 9
    for i in range(n):
        u = i / (n - 1); gx = X(-4.6 + 9.2 * u); gz = 6.5 - 0.7 * math.sin(math.pi * u)
        cone("flag", (gx, WALL_Y - 0.08, gz - 0.2), 0.17, 0.36, (math.radians(180), 0, 0), (m_mag, m_gold, m_lav, m_pink)[i % 4], scale=(1, 0.2, 1))
    curve("string", [(X(-4.8), WALL_Y - 0.06, 6.55), (0, WALL_Y - 0.06, 5.8), (X(4.8), WALL_Y - 0.06, 6.55)], 0.015, m_white)
    cx, cz = X(3.4), 3.75
    cylinder("clock_rim", (cx, WALL_Y - 0.08, cz), 0.4, 0.12, m_gold, rot=(math.radians(90), 0, 0), bevel=0.03)
    cylinder("clock_face", (cx, WALL_Y - 0.15, cz), 0.33, 0.04, m_white, rot=(math.radians(90), 0, 0))
    box("hand_h", (cx + 0.06, WALL_Y - 0.19, cz + 0.06), (0.2, 0.02, 0.035), m_purple, rot=(0, math.radians(-40), 0), bevel=0.005)
    box("hand_m", (cx, WALL_Y - 0.2, cz + 0.12), (0.035, 0.02, 0.26), m_purple, bevel=0.005)
    sphere("clock_pin", (cx, WALL_Y - 0.2, cz), 0.03, mat=m_mag, levels=1)

    # rug in the centre front: the pet stands here
    cylinder("rug_border", (0, -1.0, 0.03), 2.4, 0.06, m_mag, scale=(k, 0.62, 1), bevel=0.02)
    cylinder("rug", (0, -1.0, 0.055), 2.2, 0.07, m_pink, scale=(k, 0.62, 1), bevel=0.02)
    cylinder("rug_dot", (0, -1.0, 0.075), 0.9, 0.07, material("pink2", hexc("#FFF3F7"), rough=0.6, sss=0.3, coat=0.2), scale=(k, 0.62, 1), bevel=0.02)

    # plant on the left
    px, py = X(-3.7), 1.6
    cone("pot", (px, py, 0.42), 0.36, 0.84, (math.radians(180), 0, 0), m_lav, r2=0.44)
    torus("pot_rim", (px, py, 0.84), 0.44, 0.05, m_gold)
    for a, h, r in ((0, 1.35, 0.42), (2.1, 1.25, 0.38), (4.2, 1.3, 0.4), (1.0, 1.7, 0.34), (3.3, 1.65, 0.32), (5.4, 1.6, 0.35)):
        sphere("leaf", (px + 0.26 * math.cos(a), py + 0.2 * math.sin(a), h), r, (0.55, 0.45, 1.25), m_green if int(a) % 2 else m_green2,
               rot=(math.radians(-25 * math.sin(a)), math.radians(25 * math.cos(a)), 0))

    # toy box on the right against the wall, a ball beside it
    bx, by = X(2.7), 1.9
    box("toybox", (bx, by, 0.42), (1.35, 0.9, 0.84), m_purple, bevel=0.05)
    box("toybox_lid", (bx, by, 0.9), (1.45, 1.0, 0.14), m_gold, bevel=0.04)
    sphere("knob", (bx, by - 0.5, 0.9), 0.07, mat=m_gold, levels=1)
    sphere("ball", (bx - 1.05, by - 0.7, 0.28), 0.28, mat=m_blue)
    torus("ball_stripe", (bx - 1.05, by - 0.7, 0.28), 0.27, 0.035, m_mag, rot=(math.radians(20), 0, 0))

    # floor lamp on the right
    lx, ly = X(4.0), 1.2
    cylinder("lamp_base", (lx, ly, 0.05), 0.42, 0.1, m_gold, bevel=0.03)
    cylinder("lamp_pole", (lx, ly, 1.35), 0.045, 2.6, m_purple)
    shade = material("shade", hexc("#FFD6E4"), rough=0.6, sss=0.4, coat=0.1, emit=0.55 if evening else 0.0)
    cone("shade", (lx, ly, 2.95), 0.6, 0.7, (0, 0, 0), shade, r2=0.35)
    sphere("lamp_top", (lx, ly, 3.35), 0.07, mat=m_gold, levels=1)
    return (lx, ly)


def light_room(scene, evening, lamp_xy):
    world = bpy.data.worlds.new("w"); scene.world = world; world.use_nodes = True
    bg = world.node_tree.nodes["Background"]
    lx, ly = lamp_xy
    if evening:
        bg.inputs[0].default_value = (0.2, 0.25, 0.6, 1); bg.inputs[1].default_value = 0.07
        light("key", "AREA", (-8, -12, 9), 520, 8.0, (0.5, 0.62, 1.0), target=(0, 0, 1.5))
        light("fill", "AREA", (9, -9, 5), 160, 8.0, (0.7, 0.6, 1.0), target=(0, 0, 1.5))
        light("lamp", "POINT", (lx, ly, 2.75), 90, color=(1.0, 0.78, 0.5))
        light("lamp_spill", "AREA", (lx - 1.5, ly - 2.0, 3.6), 170, 3.0, (1.0, 0.8, 0.55), target=(lx * 0.5, 0, 0.5))
        light("moon", "SUN", (1.5, 8, 7), 0.5, color=(0.7, 0.8, 1.0), target=(0, -2, 0)).data.angle = math.radians(3)
    else:
        bg.inputs[0].default_value = (0.82, 0.8, 0.95, 1); bg.inputs[1].default_value = 0.45
        light("key", "AREA", (-8, -12, 9), 5200, 8.0, (1.0, 0.96, 0.9), target=(0, 0, 1.5))
        light("fill", "AREA", (9, -9, 5), 2000, 8.0, (0.88, 0.92, 1.0), target=(0, 0, 1.5))
        light("sun", "SUN", (1.5, 8, 7), 1.5, color=(1.0, 0.93, 0.8), target=(0, -2, 0)).data.angle = math.radians(3)


def render_variant(name, out, samples, preview=False):
    w, h, evening = VARIANTS[name]
    portrait = h > w
    scene = reset_scene(24 if preview else samples, width=w // 2 if preview else w, height=h // 2 if preview else h)
    scene.render.film_transparent = False
    lamp = build_room(evening, portrait)
    light_room(scene, evening, lamp)
    if portrait:
        camera(scene, (0, -10.5, 3.1), (0, 2, 2.0), lens=36)
    else:
        camera(scene, (0, -10.5, 3.6), (0, 2, 1.45), lens=37)
    render(scene, out)


if A.all:
    os.makedirs(A.all, exist_ok=True)
    for n in VARIANTS: render_variant(n, os.path.join(A.all, n + ".png"), A.samples, A.preview)
else:
    n = A.only or "room_land_day"
    render_variant(n, A.out or os.path.abspath(n + ".png"), A.samples, A.preview)
