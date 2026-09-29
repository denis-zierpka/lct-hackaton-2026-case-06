"""Street facades — the cards of the street row (Blender, Cycles): home, market, «У Фомы», bakery and the dream gates;
and the street background under the row.

One:    Blender -b --python-exit-code 1 -P tools/art/facade.py -- --place market --out /abs/fac_market.png [--samples 160]
All:    Blender -b --python-exit-code 1 -P tools/art/facade.py -- --all /abs/dir [--samples 160]   -> dir/fac_<placeId>.png
        (the place ids of content.json: gate_park -> fac_park; the keys and --place stay gate_*)
Street: Blender -b --python-exit-code 1 -P tools/art/facade.py -- --street /abs/bg_street_port.png [--preview] [--samples 160]
        (the street has no placeId: the name bg_street_port is set by TOWN-A1g1; --all does not render it)

384 × 384 RGBA, transparent film, orthographic camera from the street a little above (lib.camera(ortho_scale)), the
house studio light of the pets and props that stand on the card. The street is one static background, the cards ride
over it in a LazyRow (TOWN-A1, decision 30); the 120 dp card relies on these zones (TOWN-A1b, CONTRACT 2):
  top ≈ 30 %     roof and the sign board with its picture (the place title is drawn by the UI);
  bottom centre  free: the resident (64 dp) stands there, so the door, windows and goods keep to the sides;
  gates          fence + wicket, no resident; the lock is NOT baked (ui_lock is an overlay in the UI).
Market, «У Фомы» and bakery take their colours from place.PLACES: a facade is the same world as its background.
MARKS: a pretzel sign of the bakery (top left, on a post), a flower and a boletus on the park and forest boards.
SIGNS: pictures on the boards — a house (home), a baguette (bakery), a basket (market, «У Фомы»); False — blank boards.
Street: 1080 × 1920 opaque RGB (--preview 540 × 960, 24 samples), the camera of place.py, the lib studio rig, colours
of the market's world (SKY — colour and emit of its sky). Zones of the frame height: 0–20 % plain light sky (status bar,
Hud1 and «Улица» in G.ink); the row 15–42 % big calm masses; ROW "ground" — the ground line at 20–33 %, the plates of
the facades stand on the pavement, "sky" — the row on the sky, the ground below it; 42–100 % the street: pavement,
lawn, bushes, lamps. No places, doors, text, residents, goods, red or crimson round things.
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
         "gate_zoo": dict(fence="#8A83D1", post="#F4A261", board="#FFC94D", tree="palm")}  # pastel bars: dark read as a cage
MARKS = True  # № 71/71а: True — приметы (а), False — как лист № 38 (б)
SIGNS = True  # № 82: рисунки-вывески на табличках (домик, багет, корзинка)
ROW = "ground"  # № 70: "ground" — the row stands on the pavement (а), "sky" — the row on the sky, the ground below it (б)
SKY = (place.PLACES["market"]["colors"]["sky"], 0.6)  # colour and emit of the street sky: the sky of bg_market_port
MARKET_SKY_EMIT = 0.15  # № 74 б: the market's sky panel in SKY[0] under the studio light, Δ ≤ 5 to the street sky
# (market_sky.py, 16 samples: 0.1 → (+1, −8, −7), 0.15 → (+5, −2, 0), 0.2 → (+9, +3, +6); SKY[1] 0.6 glowed)


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


def board(z, col, rim=None, w=1.8, h=0.5, y=-0.05, mark=None):
    sign((0, y, z), w, h, soft(col), m_rim=rim or white())  # no text: the UI writes the place title
    if SIGNS and mark: sign_mark(mark, y - 0.12, z, h / 0.5)


def sign_mark(kind, y, z, k):
    """№ 73/82: a relief picture on a board centred at z (0.5·k high), in front of it at y: 0.7–0.9 of the board height."""
    m = soft("#6B4226")  # № 82 б: the dark brown of the signs (only here), ≥ 4 : 1 on the cream, yellow and white boards
    if kind == "house":  # ⌂ of the game header (G.purpleDeep there), in the door colour here: 0.41 wide, 0.44 high
        m = soft(HOME["door"])
        box("sign_mark_wall", (0, y, z - 0.102 * k), (0.41 * k, 0.08, 0.24 * k), m, bevel=0.02)
        cylinder("sign_mark_roof", (0, y, z + 0.086 * k), 0.277 * k, 0.08, m, scale=(1, 0.49, 1), rot=(R(90), 0, 0), bevel=0.02, vertices=3)
    elif kind == "baguette":  # a thick loaf aslant (12°: 0.95 × 0.28 in the board), flat and matte (a glossy one read pale), three light cuts
        a = R(12)
        sphere("sign_mark_bread", (0, y, z), 0.14 * k, (3.4, 0.32, 1), M("#6B4226", rough=0.8, sss=0.0, coat=0.0), rot=(0, -a, 0))
        for t in (-0.24, 0.0, 0.24):
            sphere("sign_mark_cut", (t * k * math.cos(a), y - 0.045, z + t * k * math.sin(a)), 0.035 * k, (1.8, 0.5, 0.6),
                   M("#F6D9A8", rough=0.6, sss=0.2, coat=0.1), rot=(0, -a - R(60), 0), levels=1)  # the cut of the window loaves
    elif kind == "basket":  # three woven rows widening up, a rim, a handle arc behind them: one model for both shops
        k, z = 0.85 * k, z + 0.043 * k  # № 82 б: smaller, raised (all on the flat face) and shallow (a deep one cast a pale halo)
        for i in range(3):
            box("sign_mark_row", (0, y + 0.025, z + (0.085 * i - 0.19) * k), ((0.44 + 0.07 * i) * k, 0.04, 0.085 * k), m, bevel=0.025)
        box("sign_mark_rim", (0, y + 0.015, z + 0.045 * k), (0.66 * k, 0.06, 0.06 * k), m, bevel=0.025)
        torus("sign_mark_handle", (0, y + 0.03, z + 0.03 * k), 0.17 * k, 0.03 * k, m, scale=(1, 1, 0.6), rot=(R(90), 0, 0))


def home():
    c = HOME
    ground("#E8DCCB"); walls(c["wall"]); gable(c["roof"], c["side"])
    board(EAVE + 0.3, c["board"], soft(c["roof"]), mark="house")
    window(-1.15, 1.45, 0.9, 1.0, c["curtain"])
    door(1.2, 0.8, 1.9, c["door"])


def market():
    """The stall of bg_market_port seen from the street: striped awning with scallops, yellow sign, lavender posts,
    mint counter with the bowl and the ball, balloons; through the stall — sky, the far bank and the river. The sky
    panel matches the street sky (SKY): no «picture in a frame» on the card."""
    c = place.PLACES["market"]["colors"]
    ground("#E8DCCB")
    box("sky", (0, 0.9, 2.2), (3.7, 0.1, 2.4), M(SKY[0], rough=1, sss=0, coat=0, emit=MARKET_SKY_EMIT), bevel=0)
    place.cloud(1.0, 0.8, 2.45, 0.28)
    for x, s, h in ((-1.15, 0.55, c["hills"][1]), (0.1, 0.65, c["hills"][0]), (1.2, 0.55, c["hills"][1])):
        sphere("hill", (x, 0.85, 1.35), s, (1.2, 0.2, 0.55), M(h, rough=0.6, sss=0.2, coat=0.1))
    box("river", (0, 0.7, 1.3), (3.7, 0.1, 0.3), M(c["water"], rough=0.6, sss=0, coat=0.0), bevel=0)
    for sx in (-1, 1):
        cylinder("post", (1.85 * sx, -0.2, 1.65), 0.09, 3.3, M(c["post"], rough=0.5, sss=0.2, coat=0.3), bevel=0.03)
    yf, zv = awning([M(h, rough=0.6, sss=0.2, coat=0.15) for h in c["awning"]], -2.05, 2.05, 0.5, 3.95, depth=1.0, slope=25, n=7, drop=0.3)
    board(zv, c["board"], y=yf - 0.12, w=2.4, h=0.55, mark="basket")
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
    board(EAVE + 0.55, c["board"], soft(c["awning"][0]), w=2.3, h=0.55, mark="basket")
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
    board(EAVE + 0.3, "#FFF3D6", soft(c["counter"]), mark="baguette")  # board of the approved sheet № 38 (place.py before A1c)
    awning([M(h, rough=0.6, sss=0.2, coat=0.15) for h in c["awning"]], -1.8, -0.5, 0.0, 2.3, depth=0.45, slope=35, n=4, drop=0.16)
    z = window(-1.15, 1.35, 0.9, 0.8)
    crust, cut = M("#C9803F", rough=0.5, sss=0.3, coat=0.3), M("#F6D9A8", rough=0.6, sss=0.2, coat=0.1)  # place.py bread
    for x, s in ((-1.38, 1.0), (-0.92, 0.85)):
        sphere("loaf", (x, -0.2, z + 0.12 * s), 0.19 * s, (1.4, 0.85, 0.7), crust)
        for d in (-0.1, 0.0, 0.1):
            sphere("cut", (x + d * s, -0.2 - 0.15 * s, z + 0.14 * s), 0.045 * s, (0.45, 0.4, 1.6), cut, rot=(0, R(30), 0), levels=1)
    door(1.2, 0.8, 1.9, c["counter"])
    if MARKS:  # № 71: a pretzel sign on a post with a bracket, top left (the top right is the card's «!»)
        wood, x, z = soft(c["letters"]), -1.35, 3.7
        cylinder("pretzel_post", (-2.05, -0.6, 2.1), 0.07, 4.2, wood, bevel=0.02)
        cylinder("pretzel_bracket", ((x - 2.05) / 2, -0.6, 4.15), 0.05, x + 2.05, wood, rot=(0, R(90), 0), bevel=0.02)
        for s in (-1, 1):  # two tilted lobes (the two gaps) and the crossed ends of the dough under them
            torus("pretzel", (x + 0.25 * s, -0.6, z), 0.28, 0.14, crust, scale=(1, 1.1, 1), rot=(R(90), R(20 * s), 0))
            sphere("pretzel_knot", (x, -0.65, z - 0.38), 0.1, (3, 1, 1), crust, rot=(0, R(35 * s), 0))


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
        else:  # palm: a thick trunk leaning in, ringed into segments, a round crown of drooping leaves, coconuts
            t, ring = R(-8 * sx), soft("#8D5A3B")
            ax = (math.sin(t), 0, math.cos(t))
            cylinder("trunk", (x + ax[0], 0.8, ax[2]), 0.13, 2.0, trunk, rot=(0, t, 0), bevel=0.04)
            for i in range(7):
                d = 0.2 + 0.27 * i
                torus("trunk_ring", (x + ax[0] * d, 0.8, ax[2] * d), 0.13, 0.035, ring, rot=(0, t, 0))
            cx = x + ax[0] * 2.0
            sphere("crown", (cx, 0.8, 2.08), 0.22, mat=dark)
            for i, a in enumerate(range(0, 360, 60)):
                sphere("leaf", (cx + 0.38 * math.cos(R(a)), 0.8 + 0.38 * math.sin(R(a)), 1.95), 0.42, (1, 0.42, 0.2),
                       dark if i % 2 else green, rot=(0, R(25), R(a)))
            for d in (-0.1, 0.1):
                sphere("coconut", (cx + d, 0.62, 1.9), 0.09, mat=soft("#8D5A3B"))
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
    if g["tree"] == "palm":  # a paw print on the zoo board: a pad and four toes (a picture, not text)
        paw = soft("#8D5A3B")
        sphere("paw_pad", (0, -0.1, 3.27), 0.13, (1.2, 0.3, 0.95), paw)
        for dx, dz in ((-0.19, 3.43), (-0.07, 3.5), (0.07, 3.5), (0.19, 3.43)):
            sphere("paw_toe", (dx, -0.1, dz), 0.06, (1, 0.3, 1.15), paw)
    elif MARKS and g["tree"] == "round":  # № 71а: a flower on the park board, a boletus (no dots) on the forest one
        for a in range(0, 360, 60):
            sphere("emblem_petal", (0.17 * math.cos(R(a)), -0.1, 3.35 + 0.17 * math.sin(R(a))), 0.1, (1, 0.3, 1), soft("#FFC94D"))
        sphere("emblem_core", (0, -0.13, 3.35), 0.1, (1, 0.4, 1), soft("#8D5A3B"))
    elif MARKS:
        cylinder("emblem_stem", (0, -0.08, 3.21), 0.1, 0.28, soft("#FFF3D6"), bevel=0.04)
        sphere("emblem_cap", (0, -0.13, 3.43), 0.25, (1.3, 0.4, 0.62), soft("#8D5A3B"))


FACADES = {"home": home, "market": market, "foma": foma, "bakery": bakery}
FACADES.update({gid: (lambda g=g: gate(g)) for gid, g in GATES.items()})


def render_facade(fid, out, samples):
    scene = reset_scene(samples, 384)
    FACADES[fid]()
    studio(scene, target=(0, 0, MID), scale=2.0)  # place.day_light (tried) burns the pale walls to white
    d = 30
    camera(scene, (0, -d * math.cos(R(ELEV)), MID + d * math.sin(R(ELEV))), (0, 0, MID), ortho_scale=ORTHO)
    render(scene, out)


def street():
    """bg_street_port (decision 30): sky, a bank of the market's hills, the pavement of the row, a lawn with bushes and
    lamps; big calm masses — the cards ride over it."""
    c, crowns = place.PLACES["market"]["colors"], [M(h, rough=0.5, sss=0.3, coat=0.3) for h in ("#7BC47F", "#5EAA66")]
    pave, wood = M("#E8DCCB", rough=0.9, sss=0.0, coat=0.0), soft("#B9743F")  # matte: a glossy one glared at the grazing view
    box("sky", (0, 62, 15), (160, 0.1, 60), M(SKY[0], rough=1, sss=0, coat=0, emit=SKY[1]), bevel=0).visible_shadow = False
    for i, x in enumerate(range(-30, 31, 10)):  # staggered like place.riverside: no seams; tops below 20 %
        sphere("hill", (x, 22 + 3 * (i % 2), -1.2), 6.5 - (i % 2), (1.6, 0.6, 0.4), M(c["hills"][i % 2], rough=0.6, sss=0.2, coat=0.0))
    box("pavement", (0, 10, -0.05), (80, 20, 0.1), pave, bevel=0)  # the plates of the facades stand on it (ROW "ground")
    box("curb", (0, 0.6, 0.02), (80, 0.35, 0.14), soft("#E8DCCB"), bevel=0.05)
    box("lawn", (0, -9.6, -0.02), (80, 20, 0.1), M("#9ADB8E", rough=0.6, sss=0.2, coat=0.0), bevel=0)
    box("path", (0, -4.5, 0.04), (0.9, 10, 0.06), pave, bevel=0.02)  # above the lawn top (0.03)
    # low things only: at 1080 × 1920 their tops stay under the row (42 %), nothing tall shows between the cards
    for x, y, r in ((-2.0, -3.0, 0.6), (1.8, -3.6, 0.55), (-1.05, -7.0, 0.42), (1.1, -7.3, 0.38)):
        for dx, dz, k in ((-0.7, 0.0, 0.75), (0.0, 0.2, 1.0), (0.7, 0.0, 0.7)):  # a bush of three puffs
            sphere("bush", (x + dx * r, y, r * (0.5 + dz)), r * k, (1.1, 1, 0.9), crowns[x > 0])
    for x, y in ((-1.0, -5.2), (1.05, -5.6)):  # garden lamps: a wooden post, a cream globe under a wooden cap
        cylinder("lamp_post", (x, y, 0.65), 0.07, 1.3, wood, bevel=0.02)
        sphere("lamp", (x, y, 1.45), 0.18, mat=M("#E8DCCB", rough=0.3, sss=0.3, coat=0.4, emit=0.3))
        cone("lamp_cap", (x, y, 1.67), 0.2, 0.14, (0, 0, 0), wood)


def render_street(out, samples, preview=False):
    scene = reset_scene(24 if preview else samples, width=540 if preview else 1080, height=960 if preview else 1920)
    scene.render.film_transparent = False; scene.render.image_settings.color_mode = "RGB"  # opaque, as place.py
    street()
    studio(scene, target=(0, 6, 0), scale=8.0)  # the house rig from afar: even light over the wide ground
    scene.view_settings.exposure = -1.0  # the rig lights the open ground brighter than a facade
    camera(scene, (0, -10.5, 3.1), (0, 2, {"ground": -1.2, "sky": 1.5}[ROW]), lens=36)  # place.render_place, tilted by ROW
    render(scene, out)


if __name__ == "__main__":
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    ap = argparse.ArgumentParser()
    ap.add_argument("--place", choices=list(FACADES)); ap.add_argument("--out"); ap.add_argument("--all")
    ap.add_argument("--samples", type=int, default=160)
    ap.add_argument("--street"); ap.add_argument("--preview", action="store_true")
    A = ap.parse_args(argv)
    if A.all:
        os.makedirs(A.all, exist_ok=True)
        for fid in FACADES: render_facade(fid, os.path.join(A.all, "fac_%s.png" % fid.removeprefix("gate_")), A.samples)
    elif A.street:
        render_street(A.street, A.samples, A.preview)
    elif A.place and A.out:
        render_facade(A.place, A.out, A.samples)
    else:
        ap.error("use --place ID --out FILE, --all DIR or --street FILE")
