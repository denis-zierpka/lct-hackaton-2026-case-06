"""Street facades — the cards of the street row (Blender, Cycles): home, market, «У Фомы», bakery and the dream gates.

One:  Blender -b --python-exit-code 1 -P tools/art/facade.py -- --place market --out /abs/fac_market.png [--samples 160]
All:  Blender -b --python-exit-code 1 -P tools/art/facade.py -- --all /abs/dir [--samples 160]   -> dir/fac_<id>.png

384 × 384 RGBA, transparent film, orthographic camera from the street a little above (lib.camera(ortho_scale)), the
house studio light of the pets and props that stand on the card. The street is one static background, the cards ride
over it in a LazyRow (TOWN-A1, decision 30); the 120 dp card relies on these zones (TOWN-A1b, CONTRACT 2):
  top ≈ 30 %     roof and a blank sign board (the place title is drawn by the UI);
  bottom centre  free: the resident (64 dp) stands there, so the door, windows and goods keep to the sides;
  gates          fence + wicket, no resident; the lock is NOT baked (ui_lock is an overlay in the UI).
Market, «У Фомы» and bakery take their colours from place.PLACES: a facade is the same world as its background.
"""
import sys, os, math, argparse
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__))); from lib import *
import place

R, M, put = math.radians, place.M, place.put
ORTHO, MID, ELEV = 4.4, 1.96, 10  # frame 4.4 × 4.4 around z = MID (x ±2.2, z −0.24…4.16); camera elevation, deg
EAVE = 2.8  # top of the walls; roof and board above it fill the top ≈ 30 % of the frame
HOME = dict(wall="#EAE2F6", side="#DCD2EE", roof="#8A83D1", door="#520978", curtain="#FFD6E4", board="#FFFBF6")  # room.py
GATES = {"gate_park": dict(fence="#FFFBF6", post="#7BC47F", board="#FFF3D6", tree="round"),
         "gate_forest": dict(fence="#D8A66F", post="#8D5A3B", board="#FFF3D6", tree="fir"),
         "gate_zoo": dict(fence="#3A3F55", post="#F4A261", board="#FFC94D", tree="palm")}


def soft(h):
    return M(h, rough=0.55, sss=0.15, coat=0.2)


def white():
    return M("#FFFBF6", rough=0.4, sss=0.1, coat=0.3)


def ground(h):
    box("ground", (0, 0.0, -0.06), (4.2, 0.9, 0.12), soft(h), bevel=0.04)


def walls(h):
    box("wall", (0, 0.2, EAVE / 2), (3.7, 0.4, EAVE), M(h, rough=0.7, sss=0.0, coat=0.1), bevel=0.05)


def window(x, z, w, h, curtain=None):
    """Framed window with a cross bar and a sill; glass is the sky of room.py, a bit dimmer."""
    f = white()
    box("win_frame", (x, -0.02, z), (w + 0.16, 0.1, h + 0.16), f, bevel=0.05)
    box("win_glass", (x, -0.06, z), (w, 0.04, h), M("#8ED4FF", rough=1, sss=0, coat=0, emit=0.5), bevel=0.02)
    if curtain:
        for s in (-1, 1):
            box("curtain", (x + s * w * 0.36, -0.1, z + 0.05), (w * 0.28, 0.06, h * 0.95), soft(curtain), bevel=0.03)
    box("win_bar", (x, -0.1, z), (0.07, 0.04, h), f, bevel=0.01)
    box("win_bar", (x, -0.1, z), (w, 0.04, 0.07), f, bevel=0.01)
    box("win_sill", (x, -0.14, z - h / 2 - 0.08), (w + 0.3, 0.3, 0.08), f, bevel=0.03)
    return z - h / 2 - 0.04  # top of the sill


def door(x, w, h, col):
    """Arched door (box + half-hidden cylinder) in a white frame, golden knob, a step."""
    for d, m, y in ((0.14, white(), -0.02), (0.0, soft(col), -0.07)):
        box("door", (x, y, (h - w / 2) / 2), (w + d, 0.1, h - w / 2), m, bevel=0.03)
        cylinder("door_top", (x, y + 0.01, h - w / 2), (w + d) / 2, 0.1, m, rot=(R(90), 0, 0), bevel=0.03)
    sphere("knob", (x + w * 0.3, -0.15, h * 0.45), 0.06, mat=M("#FFC94D", rough=0.25, sss=0.0, coat=0.8))
    box("step", (x, -0.2, 0.04), (w + 0.3, 0.4, 0.08), white(), bevel=0.03)


def gable(roof, wall, w=3.9, rise=1.0, y=0.2):
    """Triangle gable (a 3-sided cylinder: its first vertex is local +Y, turned up) on the walls under two roof slabs."""
    cylinder("gable", (0, y, EAVE + rise / 3), 1.0, 0.4, M(wall, rough=0.7, sss=0.0, coat=0.1), scale=(w / 1.732, rise / 1.5, 1),
             rot=(R(90), 0, 0), vertices=3)
    a, L, t = math.atan2(rise, w / 2), math.hypot(rise, w / 2) + 0.1, 0.2
    for s in (-1, 1):
        box("roof", (s * (w / 4 + t / 2 * math.sin(a)), y - 0.15, EAVE + rise / 2 + t / 2 * math.cos(a)), (L, 0.9, t), soft(roof),
            rot=(0, s * a, 0), bevel=0.06)


def board(z, col, rim=None, w=1.8, h=0.5, y=-0.05):
    sign((0, y, z), w, h, soft(col), m_rim=rim or white())  # no text: the UI writes the place title


def home():
    c = HOME
    ground("#E8DCCB"); walls(c["wall"]); gable(c["roof"], c["side"])
    board(EAVE + 0.3, c["board"], soft(c["roof"]))
    window(-1.15, 1.45, 0.9, 1.0, c["curtain"])
    door(1.2, 0.8, 1.9, c["door"])


def market():
    """The stall of bg_market_port seen from the street: striped awning with scallops, yellow sign, lavender posts,
    mint counter with the bowl and the ball, balloons; through the stall — sky, the far bank and the river."""
    c = place.PLACES["market"]["colors"]
    ground(c["floor"][0])
    box("sky", (0, 0.9, 2.2), (3.7, 0.1, 2.4), M(c["sky"], rough=1, sss=0, coat=0, emit=0.4), bevel=0)
    place.cloud(1.0, 0.8, 2.45, 0.28)
    for x, s, h in ((-1.15, 0.55, c["hills"][1]), (0.1, 0.65, c["hills"][0]), (1.2, 0.55, c["hills"][1])):
        sphere("hill", (x, 0.85, 1.35), s, (1.2, 0.2, 0.55), M(h, rough=0.6, sss=0.2, coat=0.1))
    box("river", (0, 0.7, 1.3), (3.7, 0.1, 0.3), M(c["water"], rough=0.6, sss=0, coat=0.0), bevel=0)
    for sx in (-1, 1):
        cylinder("post", (1.85 * sx, -0.2, 1.65), 0.09, 3.3, M(c["post"], rough=0.5, sss=0.2, coat=0.3), bevel=0.03)
    yf, zv = awning([M(h, rough=0.6, sss=0.2, coat=0.15) for h in c["awning"]], -2.05, 2.05, 0.5, 3.95, depth=1.0, slope=25, n=7, drop=0.3)
    board(zv, c["board"], y=yf - 0.12, w=2.4, h=0.55)
    box("counter", (0, -0.35, 0.55), (3.4, 0.6, 1.1), M(c["counter"], rough=0.5, sss=0.2, coat=0.3), bevel=0.06)
    box("counter_top", (0, -0.35, 1.15), (3.6, 0.8, 0.1), soft(c["floor"][1]), bevel=0.03)
    put("item_food_basic", (-1.1, -0.4, 1.2), 0.34, (0, 0, 0))
    put("item_fun_ball", (1.15, -0.4, 1.2), 0.3, (0, 0, 0))
    for x, s, r in ((-1.55, 0.5, -10), (-1.4, 0.42, 14)):
        put("item_fun_balloon", (x, -0.5, 1.2), s, (0, r, 0))


def foma():
    """Blue shop: raised front with the board, blue-white awning over the shop window (shampoo, book) and the door."""
    c = place.PLACES["foma"]["colors"]
    ground("#E8DCCB"); walls(c["wall"])
    box("parapet", (0, 0.2, EAVE + 0.5), (2.9, 0.4, 1.0), M(c["side"], rough=0.7, sss=0.0, coat=0.1), bevel=0.05)
    box("cornice", (0, 0.05, EAVE), (3.9, 0.6, 0.18), soft(c["awning"][0]), bevel=0.05)
    board(EAVE + 0.55, c["board"], soft(c["awning"][0]), w=2.3, h=0.55)
    awning([M(h, rough=0.6, sss=0.2, coat=0.15) for h in c["awning"]], -1.95, 1.95, 0.0, EAVE - 0.12, depth=0.7, slope=30, n=9, drop=0.22)
    z = window(-1.15, 1.2, 1.0, 1.0)
    put("item_care_shampoo", (-1.4, -0.2, z), 0.26, (0, 0, 20))
    put("item_fun_book", (-0.9, -0.2, z), 0.3, (0, 0, -10))
    door(1.2, 0.8, 1.8, c["counter"])


def bakery():
    """Cream house under a brown roof with a chimney, loaves in the window under a small striped awning, orange door."""
    c = place.PLACES["bakery"]["colors"]
    ground("#E8DCCB"); walls(c["wall"])
    box("chimney", (1.05, 0.3, EAVE + 0.8), (0.34, 0.34, 0.75), soft("#C9803F"), bevel=0.04)
    gable(c["letters"], c["side"])
    board(EAVE + 0.3, c["board"], soft(c["counter"]))
    awning([M(h, rough=0.6, sss=0.2, coat=0.15) for h in c["awning"]], -1.8, -0.5, 0.0, 2.3, depth=0.45, slope=35, n=4, drop=0.16)
    z = window(-1.15, 1.35, 0.9, 0.8)
    crust, cut = M("#C9803F", rough=0.5, sss=0.3, coat=0.3), M("#F6D9A8", rough=0.6, sss=0.2, coat=0.1)  # place.py bread
    for x, s in ((-1.38, 1.0), (-0.92, 0.85)):
        sphere("loaf", (x, -0.2, z + 0.12 * s), 0.19 * s, (1.4, 0.85, 0.7), crust)
        for d in (-0.1, 0.0, 0.1):
            sphere("cut", (x + d * s, -0.2 - 0.15 * s, z + 0.14 * s), 0.045 * s, (0.45, 0.4, 1.6), cut, rot=(0, R(30), 0), levels=1)
    door(1.2, 0.8, 1.9, c["counter"])


def gate(g):
    """Fence + wicket in the middle under a beam with the board; trees of the place behind the fence."""
    ground("#9ADB8E")
    post, fence, green = soft(g["post"]), soft(g["fence"]), M("#7BC47F", rough=0.5, sss=0.3, coat=0.3)
    trunk, dark = soft("#B9743F"), M("#5EAA66", rough=0.5, sss=0.3, coat=0.3)
    for sx in (-1, 1):
        x = 1.35 * sx
        if g["tree"] == "round":
            cylinder("trunk", (x, 0.8, 0.9), 0.1, 1.8, trunk); sphere("crown", (x, 0.8, 2.15), 0.6, (1, 1, 1.05), green)
        elif g["tree"] == "fir":
            cylinder("trunk", (x, 0.8, 0.5), 0.1, 1.0, trunk)
            for i, (z, r) in enumerate(((1.2, 0.62), (1.75, 0.5), (2.25, 0.36))):
                cone("fir", (x, 0.8, z), r, 0.75, (0, 0, 0), dark if i % 2 == 0 else green)
        else:
            cylinder("trunk", (x, 0.8, 1.1), 0.09, 2.2, trunk, rot=(0, R(-8 * sx), 0))
            for a in range(0, 360, 72):
                sphere("leaf", (x - 0.15 * sx + 0.35 * math.cos(R(a)), 0.8, 2.25 + 0.12 * math.sin(R(a))), 0.4, (1, 0.16, 0.3), green,
                       rot=(0, R(-a + 20 * math.cos(R(a))), 0))
    xs = [s * (0.95 + 0.27 * i) for s in (-1, 1) for i in range(4)] + [s * (0.12 + 0.2 * i) for s in (-1, 1) for i in range(3)]
    for x in xs:  # pickets (bars at the zoo): the fence at the sides, the wicket leaves between the posts
        leaf = abs(x) < 0.6
        m, h = (post if leaf else fence), (1.25 if leaf else 1.35)
        if g["tree"] == "palm":
            cylinder("bar", (x, 0.05, h / 2 + 0.05), 0.045, h, m); sphere("bar_tip", (x, 0.05, h + 0.08), 0.07, mat=m)
        else:
            box("picket", (x, 0.05, h / 2 + 0.05), (0.15, 0.07, h), m, bevel=0.05)
            sphere("picket_top", (x, 0.05, h + 0.05), 0.075, (1, 0.47, 1), m)
    for z in (0.45, 1.05):
        for sx in (-1, 1):
            box("rail", (sx * 1.45, 0.12, z), (1.3, 0.05, 0.1), fence, bevel=0.02)
            box("rail", (sx * 0.28, 0.0, z), (0.5, 0.05, 0.1), post, bevel=0.02)
    for sx in (-1, 1):
        box("pillar", (0.68 * sx, 0.05, 1.4), (0.26, 0.3, 2.8), post, bevel=0.06)
        sphere("finial", (0.68 * sx, 0.05, 2.95), 0.17, mat=post)
    box("beam", (0, 0.05, 2.55), (1.6, 0.24, 0.18), post, bevel=0.05)
    board(3.35, g["board"], post, w=2.0, h=0.6, y=-0.02)


FACADES = {"home": home, "market": market, "foma": foma, "bakery": bakery}
FACADES.update({gid: (lambda g=g: gate(g)) for gid, g in GATES.items()})


def render_facade(fid, out, samples):
    scene = reset_scene(samples, 384)
    FACADES[fid]()
    studio(scene, target=(0, 0, MID), scale=2.0)  # place.day_light (tried) burns the pale walls to white
    d = 30
    camera(scene, (0, -d * math.cos(R(ELEV)), MID + d * math.sin(R(ELEV))), (0, 0, MID), ortho_scale=ORTHO)
    render(scene, out)


if __name__ == "__main__":
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    ap = argparse.ArgumentParser()
    ap.add_argument("--place", choices=list(FACADES)); ap.add_argument("--out"); ap.add_argument("--all")
    ap.add_argument("--samples", type=int, default=160)
    A = ap.parse_args(argv)
    if A.all:
        os.makedirs(A.all, exist_ok=True)
        for fid in FACADES: render_facade(fid, os.path.join(A.all, "fac_%s.png" % fid), A.samples)
    elif A.place and A.out:
        render_facade(A.place, A.out, A.samples)
    else:
        ap.error("use --place ID --out FILE or --all DIR")
