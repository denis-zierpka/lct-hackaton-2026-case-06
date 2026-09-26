"""Place backgrounds of the town — the «interior kit» (Blender, Cycles): market, «У Фомы», bakery.

One:  Blender -b --python-exit-code 1 -P tools/art/place.py -- --place market --out /abs/bg_market_port.png [--preview] [--samples 160]
All:  Blender -b --python-exit-code 1 -P tools/art/place.py -- --all /abs/dir [--preview]   -> dir/bg_<id>_port.png

Portrait 1080 × 1920, opaque PNG RGB, the camera and day light of room.py (the style of room_port_day). The app lays
its HUD, tabs, cards and the Match3 field over the background, so the frame is built in zones (TOWN-A1b, CONTRACT 1):
  0–31 %    light and plain (sky or wall): G.ink text of HUD-2 and place titles sits right on it (≥ 4.5 : 1); the
            place is told by big colour masses — the striped awning at the top edge, posts and wall colour at the sides;
  SHOP      31–100 % calm large forms: counter, shelves;
  JOB       30–80 % plain wall and floor, nothing like a Match3 tile; 80–100 % the counter with bread.
Decor on the shelves comes only from props.py builders on the per-place white list (PLACES[id]["decor"]).
"""
import sys, os, math, argparse
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__))); from lib import *
import props

R = math.radians
K, WALL_Y = 0.68, 3.0  # the room's portrait squeeze and back wall, so the planks and walls match room_port_day
FORBIDDEN = ("item_care_vitamins", "item_fun_bow")  # and every tile_*: never decor in a place
PLACES = {  # decor: (props.py builder, location, scale, rotation deg)
    "market": dict(kind="SHOP", outdoors=True, sign="РЫНОК",
                   colors=dict(sky="#94D2FA", water="#5FB8E8", hills=("#9ADB8E", "#7CC47F"), floor=("#E9B983", "#D9A36C"),
                               awning=("#FF6F91", "#FFD6E4"), board="#FFC94D", letters="#520978", counter="#7FD6C2",
                               post="#8A83D1"),  # lavender posts: not read as bridges of the white cards, ≥ 3 : 1 under HUD-2
                   # decor under the card columns (bg x 2–31 %, 33–62 %, 64–93 %; on S23 the gaps shift to 34–36 %, 60–62 %,
                   # 85–91 %). No balloons: above 31 % they were the loudest spot under the tabs, on the counter they
                   # peeked red through the gap between the card rows (comp 05a/06b)
                   decor=[("item_food_basic", (-1.45, -1.45, 1.21), 0.45, (0, 0, 0)),
                          ("item_fun_ball", (1.1, -1.5, 1.21), 0.42, (0, 0, 0))]),
    "foma": dict(kind="SHOP", outdoors=False, sign="ЛАВКА",
                 colors=dict(wall="#D6ECFF", side="#C4E0FA", floor=("#D2B590", "#C7A67A"), awning=("#6F9FE6", "#FFFFFF"),
                             board="#FFFFFF", letters="#2F5BA8", counter="#FFD37A", cabinet="#FFF6E6"),
                 # light shampoo and one book lying flat (pages to the camera), all under the card columns (shelf x ±2.6
                 # spans bg 15–85 %; the gaps 30–37 % and 59–65 % are x −1.47…−0.96 and 0.70…1.10 at the shelf depth)
                 decor=[("item_care_shampoo", (-2.0, WALL_Y - 0.4, 1.56), 0.55, (0, 0, 20)),
                        ("item_fun_book", (-0.55, WALL_Y - 0.45, 1.74), 0.55, (-102, 0, -90)),
                        ("item_care_shampoo", (1.85, WALL_Y - 0.4, 1.56), 0.55, (0, 0, -20)),
                        ("item_care_shampoo", (-1.95, WALL_Y - 0.4, 2.96), 0.55, (0, 0, 10)),
                        ("item_care_shampoo", (-0.1, WALL_Y - 0.4, 2.96), 0.55, (0, 0, 0)),
                        ("item_care_shampoo", (1.8, WALL_Y - 0.4, 2.96), 0.55, (0, 0, -10))]),
    "bakery": dict(kind="JOB", outdoors=False, sign="ХЛЕБ",
                   colors=dict(wall="#FFEBD2", side="#F8DDBE", floor=("#E9B983", "#D9A36C"), awning=("#F4A261", "#FFF6E6"),
                               board="#FFF3D6", letters="#8D5A3B", counter="#F4A261"),
                   decor=[]),
}
assert not [n for p in PLACES.values() for n, *_ in p["decor"] if n in FORBIDDEN or n.startswith("tile_")]


def M(h, **kw):
    return material(h, hexc(h), **kw)


def put(name, loc, s, rot):
    """One props.py builder moved to `loc`, scaled by s, turned by `rot` (deg); nested parents are kept."""
    before = set(bpy.data.objects.keys())
    props.PROPS[name]()
    e = bpy.data.objects.new(name, None); bpy.context.collection.objects.link(e)
    e.location, e.scale, e.rotation_euler = loc, (s, s, s), tuple(R(a) for a in rot)
    for n in set(bpy.data.objects.keys()) - before - {e.name}:
        if bpy.data.objects[n].parent is None: bpy.data.objects[n].parent = e


def cloud(x, y, z, s):
    m = material("cloud", (1, 1, 1, 1), emit=0.75, sss=0, coat=0)
    for dx, dz, r in ((0, 0, 1.0), (-1.1, -0.15, 0.75), (1.1, -0.1, 0.8), (0.5, 0.45, 0.7)):
        sphere("cloud", (x + dx * s, y, z + dz * s), r * s, mat=m, levels=1).visible_shadow = False


def riverside(c):
    """Outdoors instead of the back wall: sky, clouds, the far green bank with round trees, the river."""
    box("sky", (0, 62, 15), (140, 0.1, 60), M(c["sky"], rough=1, sss=0, coat=0, emit=0.6), bevel=0).visible_shadow = False
    cloud(-9, 55, 12.5, 3.0)  # one cloud: the right one lay behind the white tab «У Фомы» (white on white)
    g1, g2 = (M(h, rough=0.6, sss=0.2, coat=0.0) for h in c["hills"])
    for x, y, s, m in ((-14, 34, 7, g2), (-5, 36, 6, g1), (5, 34, 7.5, g2), (15, 36, 6, g1)):  # staggered: no seams
        sphere("hill", (x, y, -1.6), s, (1.6, 0.6, 0.45), m)
    trunk = M("#B9743F", rough=0.6)
    for x, r in ((-6.2, 1.1), (6.5, 1.2), (-1.5, 0.8)):  # no shadows: they fell on the water apart from the trees
        cylinder("trunk", (x, 31.5, 0.9), 0.14, 1.2, trunk).visible_shadow = False
        sphere("crown", (x, 31.5, 1.5 + r), r, (1, 1, 1.1), g2 if r > 1 else g1).visible_shadow = False
    box("river", (0, 30, -0.35), (120, 60, 0.1), M(c["water"], rough=0.6, sss=0, coat=0.0), bevel=0)


def build(p, c):
    wood = [material("plank%d" % i, mix(hexc(c["floor"][0]), hexc(c["floor"][1]), i / 3), rough=0.55, sss=0.1, coat=0.25) for i in range(4)]
    white = M("#FFFBF6", rough=0.4, sss=0.1, coat=0.3)
    plank_floor(wood, K, n=6, pitch=1.2)  # wider than the room's: fewer lines, fewer WebP bytes
    stripes = [M(h, rough=0.6, sss=0.2, coat=0.0) for h in c["awning"]]  # no coat: the sun glared white on it
    if p["outdoors"]:  # a stall on the pier: posts hold the awning over the counter, the river behind
        riverside(c)
        box("pier_edge", (0, 5.0, -0.12), (14, 0.3, 0.3), wood[3], bevel=0.03)
        yb, zb = awning(stripes, -2.7, 2.7, 0.2, 7.3, n=7)
        for sx in (-1, 1):  # up to the bottom of the valance (its centre zb − drop / 2), not through the canopy
            cylinder("post", (1.8 * sx, -1.95, (zb - 0.2) / 2), 0.1, zb - 0.2, M(c["post"], rough=0.5, sss=0.2, coat=0.3), bevel=0.03)
    else:  # a room: the awning hangs on the back wall above the shelves
        shell_walls(M(c["wall"], rough=0.7, sss=0.0, coat=0.1), M(c["side"], rough=0.7, sss=0.0, coat=0.1), white, K, WALL_Y)
        yb, zb = awning(stripes, -3.55, 3.55, WALL_Y - 0.1, 7.9, depth=1.6, n=11)
    # the sign sits on the valance hidden under the HUD pills (bg 6–10 %, on 360 × 640 and on S23): half-covered
    # letters read as clutter; the place name is the UI's job, the sign is for the full frame and the facade
    sign((0, yb - 0.12, zb - 0.09), 1.6, 0.42, M(c["board"], rough=0.5, sss=0.1, coat=0.3), p["sign"],
         M(c["letters"], rough=0.4, sss=0.0, coat=0.4), white)
    front = M(c["counter"], rough=0.5, sss=0.2, coat=0.3)
    if p["kind"] == "SHOP":
        if not p["outdoors"]:  # a shelving unit on the back wall, top below the 31 % line
            box("cabinet", (0, WALL_Y - 0.05, 2.2), (5.2, 0.1, 4.0), M(c["cabinet"], rough=0.6, sss=0.1, coat=0.2), bevel=0.04)
            for x in (-2.6, 2.6):
                box("cabinet_side", (x, WALL_Y - 0.4, 2.2), (0.16, 0.8, 4.0), wood[2], bevel=0.04)
            for z in (0.2, 1.5, 2.9, 4.2):
                box("shelf", (0, WALL_Y - 0.4, z), (5.36, 0.8, 0.12), wood[1], bevel=0.03)
        box("counter", (0, -1.4, 0.575), (3.5, 0.8, 1.15), front, bevel=0.06)
        box("counter_top", (0, -1.4, 1.19), (3.7, 1.0, 0.12), wood[1], bevel=0.03)
    else:  # JOB: a low counter along the bottom edge, the loaves whole in 86–100 % (under the order card they were cut)
        box("counter", (0, -5.1, 0.15), (6, 1.0, 0.3), front, bevel=0.06)
        box("counter_top", (0, -5.1, 0.34), (6.2, 1.2, 0.08), wood[1], bevel=0.03)
        crust, cut = M("#C9803F", rough=0.5, sss=0.3, coat=0.3), M("#F6D9A8", rough=0.6, sss=0.2, coat=0.1)
        for x, s in ((-0.8, 1.0), (0.1, 1.15), (0.9, 0.9)):
            sphere("loaf", (x, -5.2, 0.42), 0.26 * s, (1.4, 0.85, 0.6), crust)
            for d in (-0.13, 0.0, 0.13):
                sphere("cut", (x + d * s, -5.22, 0.42 + 0.15 * s), 0.05 * s, (0.5, 2.0, 0.35), cut, rot=(0, 0, R(35)), levels=1)
    for name, loc, s, rot in p["decor"]:
        put(name, loc, s, rot)


def day_light(scene, outdoors=False):
    """room.py light_room(evening=False): lavender world, warm key, cool fill, sun from behind. Outdoors the key and
    fill are weaker: no walls hold the room's light back, it burnt the floor and the awning to white."""
    key, fill, amb = (2200, 900, 0.25) if outdoors else (5200, 2000, 0.45)
    world = bpy.data.worlds.new("w"); scene.world = world; world.use_nodes = True
    bg = world.node_tree.nodes["Background"]; bg.inputs[0].default_value = (0.82, 0.8, 0.95, 1); bg.inputs[1].default_value = amb
    light("key", "AREA", (-8, -12, 9), key, 8.0, (1.0, 0.96, 0.9), target=(0, 0, 1.5))
    light("fill", "AREA", (9, -9, 5), fill, 8.0, (0.88, 0.92, 1.0), target=(0, 0, 1.5))
    light("sun", "SUN", (1.5, 8, 7), 1.5, color=(1.0, 0.93, 0.8), target=(0, -2, 0)).data.angle = math.radians(3)


def render_place(pid, out, samples, preview=False):
    scene = reset_scene(24 if preview else samples, width=540 if preview else 1080, height=960 if preview else 1920)
    scene.render.film_transparent = False; scene.render.image_settings.color_mode = "RGB"
    build(PLACES[pid], PLACES[pid]["colors"])
    day_light(scene, PLACES[pid]["outdoors"])
    camera(scene, (0, -10.5, 3.1), (0, 2, 2.0), lens=36)
    render(scene, out)


if __name__ == "__main__":
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    ap = argparse.ArgumentParser()
    ap.add_argument("--place", choices=list(PLACES)); ap.add_argument("--out"); ap.add_argument("--all")
    ap.add_argument("--samples", type=int, default=160); ap.add_argument("--preview", action="store_true")
    A = ap.parse_args(argv)
    if A.all:
        os.makedirs(A.all, exist_ok=True)
        for pid in PLACES: render_place(pid, os.path.join(A.all, "bg_%s_port.png" % pid), A.samples, A.preview)
    elif A.place and A.out:
        render_place(A.place, A.out, A.samples, A.preview)
    else:
        ap.error("use --place ID --out FILE or --all DIR")
