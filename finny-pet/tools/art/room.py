"""Cozy child's room: the game background the pet lives in and the furniture sprites of its targets (Blender, Cycles).

All four:   Blender -b -P tools/art/room.py -- --all /abs/outdir [--samples 160] [--preview]
One:        Blender -b -P tools/art/room.py -- --only room_land_evening --out /abs/room.png
Furniture:  Blender -b -P tools/art/room.py -- --sprites /abs/outdir [--samples 160] [--preview]   (9 × furn_<id>.png)
One sprite: Blender -b -P tools/art/room.py -- --only furn_door --out /abs/furn_door.png

Layout: floor is z = 0, the back wall stands at y = WALL_Y, the camera looks from -Y straight on and a little
from above so the floor fills the lower ~35 % of the frame. The shell is empty (№ 27): floor, solid walls, garland.
Window, shelf, clock and furniture are sprites furn_<id> (<id> — the RoomScreen target; the clock — a decoration under the shelf, № 123)
built in the portrait day room on the side of their targets: room_camera() (the one room camera, also for A1e) and
sprite_frame() (just the target-sized part of the frame, PX px per dp), the shell hidden; SHADOW = True — it catches the
shadow (№ 79). MULLION, WINDOW_VIEW — window cross and view (№ 77); WARM — the warm evening spot, None — none (№ 78).
Portrait variants are the same room re-framed: every x coordinate is squeezed by K so the composition fits a vertical camera.
"""
import sys, os, math, argparse, random
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__))); from lib import *
from bpy_extras.object_utils import world_to_camera_view
from mathutils import Matrix

VARIANTS = {  # name: (width, height, evening)
    "room_land_day": (1920, 1080, False), "room_land_evening": (1920, 1080, True),
    "room_port_day": (1080, 1920, False), "room_port_evening": (1080, 1920, True),
}
WALL_Y = 3.0
K = 0.68  # horizontal squeeze for the vertical framing
PX = 3  # px of a sprite per dp of its target: the native density of the emulator and the S23
SHADOW = False  # № 79: True — the shell catches the shadow of the furniture (sheet K4 only)
MULLION = False  # № 77: True — a cross in the window, as in the old room
WINDOW_VIEW = "fence"  # № 77: "sky" — only the sky with clouds behind the glass
WARM = (0.0, -1.0)  # № 78 б: (x до сжатия K, y) тёплого света вечера — центр пола, где стоит питомец; None — без него


def palette():
    """The room colours (place.py and facade.py copy them): local name -> material, wood — the 4 plank tints."""
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
    return locals()  # ponytail: every local here is a material


def furn_window(p):
    """Window on the back wall: frame, full-width sill, curtains on a rod; the sky with clouds (and the street) behind."""
    m_white, m_pink, m_gold, x0 = p["m_white"], p["m_pink"], p["m_gold"], -2.2
    ww, wz0, wz1 = 0.62, 2.95, 4.1  # glass half-width, bottom, top
    t = 0.12
    for x, z, sxz in ((-ww - t / 2, (wz0 + wz1) / 2, (t, wz1 - wz0 + 2 * t)), (ww + t / 2, (wz0 + wz1) / 2, (t, wz1 - wz0 + 2 * t)),
                      (0, wz0 - t / 2, (2 * ww, t)), (0, wz1 + t / 2, (2 * ww, t)),
                      (0, (wz0 + wz1) / 2, (0.07, wz1 - wz0)), (0, (wz0 + wz1) / 2, (2 * ww, 0.07)))[:6 if MULLION else 4]:
        box("frame", (x0 + x, WALL_Y - 0.02, z), (sxz[0], 0.2 - 0.02 * (sxz[1] == 0.07), sxz[1]), m_white, bevel=0.02)  # cross: no shared face
    box("sill", (x0, WALL_Y - 0.12, wz0 - t - 0.06), (2 * ww + 0.9, 0.36, 0.12), m_white, bevel=0.03)
    cylinder("rod", (x0, WALL_Y - 0.3, wz1 + t / 2), 0.035, 2 * ww + 0.9, m_gold, rot=(0, math.radians(90), 0))
    for sx in (-1, 1):
        for j in range(2):
            cylinder("fold", (x0 + (ww + 0.08 + j * 0.16) * sx, WALL_Y - 0.2, (wz0 + wz1) / 2), 0.1,
                     wz1 - wz0 + 2 * t, m_pink, scale=(1, 0.7, 1), bevel=0.05)
    sky = material("sky", hexc("#8ED4FF"), rough=1, sss=0, coat=0, emit=1.0)
    box("sky", (x0, WALL_Y - 0.03, (wz0 + wz1) / 2), (2 * ww + t, 0.04, wz1 - wz0 + t), sky, bevel=0)  # the whole opening
    cloud = material("cloud", (1, 1, 1, 1), emit=0.85, sss=0, coat=0)
    for cx, cz, s in ((x0 - ww * 0.45, wz1 - 0.3, 0.13), (x0 + ww * 0.5, wz1 - 0.55, 0.11)):
        for dx, dz, r in ((0, 0, 1.0), (-1.1, -0.15, 0.75), (1.1, -0.1, 0.8), (0.5, 0.45, 0.7)):
            sphere("cloud", (cx + dx * s, WALL_Y - 0.06, cz + dz * s), r * s, (1, 0.3, 1), mat=cloud, levels=1)
    if WINDOW_VIEW == "fence":  # a strip of the street at the bottom of the glass: lawn and a fence
        box("lawn", (x0, WALL_Y - 0.06, wz0 + 0.1), (2 * ww, 0.04, 0.2), p["m_green"], bevel=0)
        box("rail", (x0, WALL_Y - 0.09, wz0 + 0.22), (2 * ww, 0.02, 0.04), m_white, bevel=0)
        for i in range(8):
            box("picket", (x0 - ww + (i + 0.5) * ww / 4, WALL_Y - 0.1, wz0 + 0.18), (0.07, 0.02, 0.26), m_white, bevel=0.01)


def furn_shelf(p):
    """Wooden board on two brackets on the back wall; the jars with lids and numbers stay UI on top of it."""
    wood, m_white = p["wood"], p["m_white"]
    sh_x, sh_w = 0.4, 3.0
    box("shelf", (sh_x, WALL_Y - 0.25, 3.3), (sh_w, 0.5, 0.11), wood[1], bevel=0.02)
    for dx in (-0.32, 0.32):
        box("bracket", (sh_x + dx * sh_w, WALL_Y - 0.12, 3.12), (0.09, 0.24, 0.26), m_white, bevel=0.015)


def furn_fridge(p):
    """Tall rounded fridge by the wall: freezer and main door, vertical handles, a note on a magnet."""
    x, y, w = -2.6, WALL_Y - 0.42, 1.0
    box("fridge", (x, y, 0.95), (w, 0.8, 1.7), p["m_white"], bevel=0.1)
    box("plinth", (x, y + 0.03, 0.05), (w - 0.1, 0.72, 0.1), p["m_lav"], bevel=0.01)
    box("seam", (x, y - 0.4, 1.2), (w - 0.1, 0.02, 0.025), p["m_lav"], bevel=0)
    for z in (0.95, 1.45):
        box("handle", (x + w / 2 - 0.13, y - 0.44, z), (0.05, 0.06, 0.3), p["m_lav"], bevel=0.02)
    box("note", (x - 0.12, y - 0.41, 1.45), (0.26, 0.01, 0.32), p["m_pink"], rot=(0, 0.12, 0), bevel=0)
    sphere("magnet", (x - 0.14, y - 0.42, 1.58), 0.04, mat=p["m_mag"], levels=1)


def furn_door(p):
    """Full-height light wooden door in a white frame, the knob on the right."""
    x, h = 2.6, 2.6
    box("door_frame", (x, WALL_Y - 0.05, (h + 0.12) / 2), (1.2, 0.1, h + 0.12), p["m_white"], bevel=0.02)
    box("door", (x, WALL_Y - 0.12, h / 2), (1.0, 0.08, h), p["wood"][0], bevel=0.02)
    for z, dz in ((0.7, 0.9), (1.8, 0.8)):
        box("door_panel", (x, WALL_Y - 0.165, z), (0.7, 0.03, dz), p["wood"][2], bevel=0.03)
    sphere("door_knob", (x + 0.36, WALL_Y - 0.21, 1.2), 0.065, mat=p["m_gold"], levels=1)


def furn_bed(p):
    """Bed seen from the side: headboard on the left, pillow, blanket; the boards stand on the floor."""
    y, L, d, wood = -2.0, 1.55, 0.66, p["wood"]
    box("bed_base", (0, y, 0.19), (L, d, 0.18), wood[1], bevel=0.04)
    box("mattress", (0.03, y, 0.33), (L - 0.14, d - 0.04, 0.12), p["m_white"], bevel=0.05)
    box("blanket", (0.2, y - 0.01, 0.37), (L - 0.46, d, 0.09), p["m_pink"], bevel=0.04)
    box("bed_head", (-L / 2 + 0.05, y, 0.28), (0.1, d + 0.04, 0.56), wood[0], bevel=0.04)
    box("bed_foot", (L / 2 - 0.05, y, 0.22), (0.1, d + 0.04, 0.44), wood[0], bevel=0.04)
    sphere("pillow", (-L / 2 + 0.3, y, 0.43), 0.18, (1, 1.4, 0.45), mat=p["m_white"])


def furn_bed_v(p):
    """№ 121: the same bed turned 90° by the left edge — the headboard at the back wall, the foot to the camera; A1d3 (owner):
    ×2.1, bigger than the 120 dp pet; x −1.2, not −1.8 — else its 150 dp frame runs off the 1080 px render.
    furn_bed stays side-on: the night screen lays the pet on its blanket (№ 91 б)."""
    old = set(bpy.context.scene.objects); furn_bed(p)
    next(o for o in set(bpy.context.scene.objects) - old if o.name == "bed_base").scale.x -= 0.04  # its ends were flush with the boards: end-on, Cycles shades that face black
    _move(old, Matrix.Translation((-1.2, -2.0, 0)) @ Matrix.Scale(2.1, 4) @ Matrix.Rotation(math.radians(-90), 4, "Z") @ Matrix.Translation((0, 2.0, 0)))


def _move(old, m):
    """Moves every object built since the snapshot `old` by the world matrix m (the same model turned or scaled)."""
    bpy.context.view_layer.update()  # else the last object's matrix_world misses its fresh scale and rotation
    for o in set(bpy.context.scene.objects) - old: o.matrix_world = m @ o.matrix_world


def furn_chest(p):
    """Wooden chest with a domed lid, gold bands and a clasp (not the old purple toy box)."""
    x, y, w, d, h, gold = -2.0, -1.4, 0.73, 0.5, 0.3, p["m_gold"]
    box("chest", (x, y, h / 2), (w, d, h), p["wood"][2], bevel=0.03)
    cylinder("chest_lid", (x, y, h), d / 2, w + 0.03, p["wood"][1], scale=(0.55, 1, 1), rot=(0, math.radians(90), 0), bevel=0.02)
    for dx in (-0.3, 0.3):
        box("chest_band", (x + dx * w, y, h / 2), (0.06, d + 0.02, h + 0.01), gold, bevel=0.01)
        cylinder("chest_band", (x + dx * w, y, h), d / 2 + 0.01, 0.06, gold, scale=(0.55, 1, 1), rot=(0, math.radians(90), 0))
    box("chest_clasp", (x, y - d / 2 - 0.01, h - 0.02), (0.1, 0.03, 0.14), gold, bevel=0.01)


def furn_clock(p):
    """№ 123: the wall clock just under the jar shelf, right of the window. A sprite, not the shell: the shelf is UI in dp,
    the background is drawn Crop (S23 ×1,22), so a clock in the shell drifted off the shelf. Where the shell had it (x squeezed by K)."""
    cx, cz, m_purple = -1.25 * K, 2.5, p["m_purple"]
    cylinder("clock_rim", (cx, WALL_Y - 0.08, cz), 0.4, 0.12, p["m_gold"], rot=(math.radians(90), 0, 0), bevel=0.03)
    cylinder("clock_face", (cx, WALL_Y - 0.15, cz), 0.33, 0.04, p["m_white"], rot=(math.radians(90), 0, 0))
    box("hand_h", (cx + 0.06, WALL_Y - 0.19, cz + 0.06), (0.2, 0.02, 0.035), m_purple, rot=(0, math.radians(-40), 0), bevel=0.005)
    box("hand_m", (cx, WALL_Y - 0.2, cz + 0.12), (0.035, 0.02, 0.26), m_purple, bevel=0.005)
    sphere("clock_pin", (cx, WALL_Y - 0.2, cz), 0.03, mat=p["m_mag"], levels=1)


def furn_mailbox(p):
    """Mailbox on a post: rounded roof, a slot in front, a raised flag on the side (the envelope stays UI)."""
    x, y, blue = 1.7, -2.15, p["m_blue"]
    old = set(bpy.context.scene.objects)
    box("mail_foot", (x, y, 0.035), (0.26, 0.26, 0.07), p["m_white"], bevel=0.03)  # round edge: α stays ≥ 2 px off the frame
    box("mail_post", (x, y, 0.13), (0.08, 0.08, 0.22), p["m_white"], bevel=0.01)
    box("mailbox", (x, y, 0.31), (0.42, 0.3, 0.18), blue, bevel=0.03)
    cylinder("mail_roof", (x, y, 0.4), 0.15, 0.44, blue, scale=(0.6, 1, 1), rot=(0, math.radians(90), 0), bevel=0.02)
    box("mail_slot", (x, y - 0.155, 0.33), (0.22, 0.02, 0.03), p["m_purple"], bevel=0.005)
    box("mail_pole", (x + 0.225, y, 0.42), (0.025, 0.025, 0.26), p["m_white"], bevel=0.005)
    box("mail_flag", (x + 0.285, y, 0.51), (0.12, 0.02, 0.08), p["m_gold"], bevel=0.005)
    # № 124: twice as big, from the foot, 0.2 to the left — else the 288 px frame runs off the 1080 px render
    _move(old, Matrix.Translation((x - 0.2, y, 0)) @ Matrix.Scale(2, 4) @ Matrix.Translation((-x, -y, 0)))


def build_room(evening, portrait):
    """The empty shell (№ 27): floor, walls, garland. Returns where the warm evening light stands (WARM) or None."""
    k = K if portrait else 1.0
    X = lambda x: x * k
    p = palette(); wood, m_wall, m_side, m_white = p["wood"], p["m_wall"], p["m_side"], p["m_white"]
    m_pink, m_mag, m_lav, m_gold = (p[n] for n in ("m_pink", "m_mag", "m_lav", "m_gold"))

    # floor, solid back wall, side walls, skirting boards (lib: interior shell)
    plank_floor(wood, k)
    shell_walls(m_wall, m_side, m_white, k, WALL_Y, window=None)

    # pennant garland high on the wall (seen in the portrait framing)
    n = 9
    for i in range(n):
        u = i / (n - 1); gx = X(-4.6 + 9.2 * u); gz = 6.5 - 0.7 * math.sin(math.pi * u)
        cone("flag", (gx, WALL_Y - 0.08, gz - 0.2), 0.17, 0.36, (math.radians(180), 0, 0), (m_mag, m_gold, m_lav, m_pink)[i % 4], scale=(1, 0.2, 1))
    curve("string", [(X(-4.8), WALL_Y - 0.06, 6.55), (0, WALL_Y - 0.06, 5.8), (X(4.8), WALL_Y - 0.06, 6.55)], 0.015, m_white)
    return WARM and (X(WARM[0]), WARM[1])


def light_room(scene, evening, lamp_xy):
    world = bpy.data.worlds.new("w"); scene.world = world; world.use_nodes = True
    bg = world.node_tree.nodes["Background"]
    if evening:
        bg.inputs[0].default_value = (0.2, 0.25, 0.6, 1); bg.inputs[1].default_value = 0.07
        light("key", "AREA", (-8, -12, 9), 520, 8.0, (0.5, 0.62, 1.0), target=(0, 0, 1.5))
        light("fill", "AREA", (9, -9, 5), 160, 8.0, (0.7, 0.6, 1.0), target=(0, 0, 1.5))
        if lamp_xy:  # the warm spot (WARM), no lamp any more
            lx, ly = lamp_xy
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
    room_camera(scene, portrait)
    render(scene, out)


def room_camera(scene, portrait=True):
    """The one room camera (№ 28): the shell, the furn_* sprites and the things of A1e. Returns the camera object."""
    if portrait:
        camera(scene, (0, -10.5, 3.1), (0, 2, 2.0), lens=36)
    else:
        camera(scene, (0, -10.5, 3.6), (0, 2, 1.45), lens=37)
    return scene.camera


def sprite_frame(scene, objs, w, h, floor=False, top=None):
    """Render only the w × h px part (whole px) of the frame around the projection of the bbox corners of `objs`: centred
    on x; on y the bottom 3 px below the projection (floor), the top `top` px above it, else centred."""
    bpy.context.view_layer.update()  # solves the TRACK_TO of the camera (lib._track)
    W, H = scene.render.resolution_x, scene.render.resolution_y
    pts = [world_to_camera_view(scene, scene.camera, o.matrix_world @ Vector(c)) for o in objs for c in o.bound_box]
    xs, ys = [q.x * W for q in pts], [(1 - q.y) * H for q in pts]
    x0 = round((min(xs) + max(xs) - w) / 2)
    y0 = round(max(ys) + 3 - h if floor else min(ys) - top if top is not None else (min(ys) + max(ys) - h) / 2)
    r = scene.render; r.use_border = r.use_crop_to_border = True
    r.border_min_x, r.border_max_x = (x0 + 0.5) / W, (x0 + w + 0.5) / W  # + 0.5 px: Blender truncates border × size
    r.border_min_y, r.border_max_y = (H - y0 - h + 0.5) / H, (H - y0 + 0.5) / H


SPRITES = {"furn_window": (112, 80, furn_window), "furn_shelf": (152, 72, furn_shelf), "furn_fridge": (64, 96, furn_fridge),
           "furn_door": (64, 136, furn_door), "furn_bed": (128, 64, furn_bed), "furn_mailbox": (96, 96, furn_mailbox),
           "furn_chest": (64, 48, furn_chest), "furn_bed_v": (150, 162, furn_bed_v),
           "furn_clock": (42, 42, furn_clock)}  # name: (target width, height in dp — RoomScreen.kt, builder)


def render_sprite(name, out, samples, preview=False):
    """One furniture sprite: the thing in the portrait day room, the shell hidden from the camera, the target-sized frame."""
    w, h, build = SPRITES[name]
    scene = reset_scene(24 if preview else samples, width=1080, height=1920)
    p = palette()
    plank_floor(p["wood"], K); shell_walls(p["m_wall"], p["m_side"], p["m_white"], K, WALL_Y, window=None)
    shell = list(scene.objects)
    build(p)
    objs = [o for o in scene.objects if o not in shell]
    light_room(scene, False, None)
    room_camera(scene)
    wall, floor = name in ("furn_window", "furn_shelf", "furn_fridge", "furn_door", "furn_clock"), name not in ("furn_window", "furn_shelf", "furn_clock")
    for o in shell:  # SHADOW: the whole shell catches (№ 79), the camera sees only the floor and/or the back wall under the thing
        o.is_shadow_catcher = SHADOW
        o.visible_camera = SHADOW and (floor and o.name.startswith("plank") or wall and o.name == "wall_back")
    sprite_frame(scene, objs, w * PX, h * PX, floor=floor, top=126 if name == "furn_shelf" else None)
    render(scene, out)


if __name__ == "__main__":
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    ap = argparse.ArgumentParser()
    ap.add_argument("--all"); ap.add_argument("--only"); ap.add_argument("--out"); ap.add_argument("--sprites")
    ap.add_argument("--samples", type=int, default=160); ap.add_argument("--preview", action="store_true")
    A = ap.parse_args(argv)
    if A.all:
        os.makedirs(A.all, exist_ok=True)
        for n in VARIANTS: render_variant(n, os.path.join(A.all, n + ".png"), A.samples, A.preview)
    elif A.sprites:
        os.makedirs(A.sprites, exist_ok=True)
        for n in SPRITES: render_sprite(n, os.path.join(A.sprites, n + ".png"), A.samples, A.preview)
    else:
        n = A.only or "room_land_day"
        (render_sprite if n in SPRITES else render_variant)(n, A.out or os.path.abspath(n + ".png"), A.samples, A.preview)
