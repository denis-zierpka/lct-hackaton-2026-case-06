"""Procedural toy-style pets for Finny, rendered with Blender (Cycles, transparent background).

One frame:  Blender -b -P tools/art/pet.py -- --species cat --color '#F4A261' --face happy --stage 1 --out /abs/cat.png
Full set:   Blender -b -P tools/art/pet.py -- --all /abs/outdir [--only-species puppy] [--size 512 --samples 96]
            renders species x colour x stage x face (3 x 3 x 3 x 4 = 108 frames) as pet_<species>_<colour>_<stage>_<face>.png;
            existing PNGs are skipped. Then: python tools/art/import_sprites.py /abs/outdir

Town residents: Blender -b -P tools/art/pet.py -- --residents /abs/outdir [--only-resident marta] [--size N --samples N]
            renders town.residents of content.json (species, accessory, stage 0, happy face, colour from
            RESIDENT_COLORS) as res_<id>.png. A single frame takes --accessory apron|cap|glasses as well.
            Role props (ROLE_PROPS, decisions 35, 46 b): Tosha's ball, Stepan's wrench, Kesha's coin badge instead of the
            apron pocket, Liza's palette with the beret; Foma's cap is gold with a dark visor (CAP_COLORS).

Colours and species ids mirror app/src/main/assets/content/content.json. The ground shadow is drawn by the
app (PetSprite in game, PetView in classic), so no shadow catcher here. All geometry is primitives + subdivision: the art belongs to the team.
"""
import bpy, bmesh, math, sys, argparse, os, json
from mathutils import Vector

SPECIES = ["cat", "bunny", "puppy"]
COLORS = {"orange": "#F4A261", "blue": "#6FB1E0", "green": "#7BC47F"}
# Residents get their own body colours so they never look like the child's pet (owner's decision 33 a):
# a hue from the look.color family, dE76 >= 20 to every pet colour and between residents of one species.
RESIDENT_COLORS = {"marta": "#EE8266", "foma": "#5A7FC4", "borya": "#B7C46A", "osya": "#A9A6CC",
                   "tosha": "#8C5A34", "stepan": "#56B4B8", "kesha": "#7FCBB0", "liza": "#F7C996",
                   "asya": "#B7AEEA"}
# What a resident's job looks like, on top of the content accessory (owner's decisions 35, 46 b); content.json stays as it is.
ROLE_PROPS = {"osya": ("bag",), "asya": ("doctor",), "borya": ("toque",), "liza": ("beret", "palette"), "tosha": ("ball",),
              "stepan": ("wrench",), "kesha": ("coin",)}
# Cap (dome, visor) other than the usual #520978 / #FF0053, so that Foma is not Osya's or Stepan's twin (decision 46 b)
CAP_COLORS = {"foma": ("#FFC94D", "#520978")}
ACCESSORIES = ["apron", "cap", "glasses"]
# Hats sit on an empty on the crown (z, y, radius, forward tilt); the bunny's is on the forehead, like the crown.
HATS = {"bunny": (2.74, -0.42, 0.44, -22), "cat": (2.78, -0.32, 0.42, 14), "puppy": (2.82, -0.12, 0.5, 10)}
FACES = ["happy", "neutral", "sad", "blink"]
STAGES = [0, 1, 2]
CONTENT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../app/src/main/assets/content/content.json"))

argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
ap = argparse.ArgumentParser()
ap.add_argument("--species", default="cat"); ap.add_argument("--color", default="#F4A261")
ap.add_argument("--face", default="happy"); ap.add_argument("--stage", type=int, default=0)
ap.add_argument("--out", default=None); ap.add_argument("--all", default=None)
ap.add_argument("--samples", type=int, default=96); ap.add_argument("--size", type=int, default=512)
ap.add_argument("--only-species", default=None, choices=SPECIES)
ap.add_argument("--accessory", default=None, choices=ACCESSORIES)
ap.add_argument("--residents", default=None); ap.add_argument("--only-resident", default=None)
A = ap.parse_args(argv)


def hexc(h, a=1.0):
    h = h.lstrip("#"); r, g, b = (int(h[i:i + 2], 16) / 255 for i in (0, 2, 4))
    lin = lambda c: c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4  # sRGB -> linear
    return (lin(r), lin(g), lin(b), a)


def mix(c1, c2, t):
    return tuple(c1[i] * (1 - t) + c2[i] * t for i in range(3)) + (1.0,)


def setup_scene(samples, size):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    scene = bpy.context.scene
    scene.render.engine = "CYCLES"
    scene.cycles.device = "CPU"
    prefs = bpy.context.preferences.addons["cycles"].preferences
    for kind in ("OPTIX", "CUDA", "HIP", "ONEAPI", "METAL"):  # first GPU backend with a device wins, else CPU
        try:
            prefs.compute_device_type = kind
            gpus = [d for d in prefs.get_devices_for_type(kind) if d.type == kind]
        except (TypeError, ValueError):  # backend not built for this OS
            continue
        if gpus:
            for d in prefs.devices: d.use = d.type == kind
            scene.cycles.device = "GPU"
            break
    print("cycles device:", prefs.compute_device_type if scene.cycles.device == "GPU" else "CPU", flush=True)
    scene.cycles.samples = samples
    scene.cycles.use_denoising = True
    scene.render.film_transparent = True
    scene.render.resolution_x = scene.render.resolution_y = size
    scene.render.image_settings.file_format = "PNG"; scene.render.image_settings.color_mode = "RGBA"
    scene.view_settings.view_transform = "Standard"; scene.view_settings.look = "None"; scene.view_settings.exposure = -0.15
    return scene


MATS = {}


def material(name, rgba, rough=0.45, sss=0.25, coat=0.35, alpha=1.0, emit=0.0):
    key = (name, rgba, rough, sss, coat, alpha, emit)
    if key in MATS and MATS[key].name in bpy.data.materials: return MATS[key]
    m = bpy.data.materials.new(name); m.use_nodes = True
    p = m.node_tree.nodes["Principled BSDF"]
    def s(n, v):
        if n in p.inputs: p.inputs[n].default_value = v
    s("Base Color", rgba); s("Roughness", rough); s("Alpha", alpha)
    s("Subsurface Weight", sss); s("Subsurface Radius", (0.8, 0.35, 0.2)); s("Subsurface Scale", 0.25)
    s("Coat Weight", coat); s("Coat Roughness", 0.15)
    if emit: s("Emission Color", rgba); s("Emission Strength", emit)
    if alpha < 1 and hasattr(m, "surface_render_method"): m.surface_render_method = "BLENDED"
    MATS[key] = m; return m


def smooth(o, levels=2):
    o.modifiers.new("subd", "SUBSURF").levels = levels
    o.modifiers["subd"].render_levels = levels
    for pg in o.data.polygons: pg.use_smooth = True
    return o


def sphere(name, loc, r, scale=(1, 1, 1), mat=None, rot=(0, 0, 0)):
    bpy.ops.mesh.primitive_uv_sphere_add(radius=r, location=loc, segments=48, ring_count=24)
    o = bpy.context.object; o.name = name; o.scale = scale; o.rotation_euler = rot
    for pg in o.data.polygons: pg.use_smooth = True
    if mat: o.data.materials.append(mat)
    return o


def cone(name, loc, r, depth, rot, mat, scale=(1, 1, 1)):
    bpy.ops.mesh.primitive_cone_add(radius1=r, radius2=0.0, depth=depth, location=loc, vertices=48)
    o = bpy.context.object; o.name = name; o.rotation_euler = rot; o.scale = scale
    o.modifiers.new("bev", "BEVEL").width = r * 0.35
    o.modifiers["bev"].segments = 6
    smooth(o, 2)
    o.data.materials.append(mat); return o


def curve(name, pts, bevel, mat, taper=False):
    cd = bpy.data.curves.new(name, "CURVE"); cd.dimensions = "3D"; cd.bevel_depth = bevel; cd.bevel_resolution = 6
    cd.use_fill_caps = True
    sp = cd.splines.new("BEZIER"); sp.bezier_points.add(len(pts) - 1)
    for bp, p in zip(sp.bezier_points, pts):
        bp.co = Vector(p); bp.handle_left_type = bp.handle_right_type = "AUTO"
    if taper:
        for i, bp in enumerate(sp.bezier_points): bp.radius = 1.0 - 0.75 * i / max(1, len(pts) - 1)
    o = bpy.data.objects.new(name, cd); bpy.context.collection.objects.link(o)
    cd.materials.append(mat); return o


def box(name, loc, dims, mat, bev):
    bpy.ops.mesh.primitive_cube_add(size=1, location=loc); o = bpy.context.object; o.name = name
    for v in o.data.vertices: v.co = Vector([c * d for c, d in zip(v.co, dims)])
    o.modifiers.new("bev", "BEVEL").width = bev; o.modifiers["bev"].segments = 3
    o.data.materials.append(mat); return o


def drum(name, loc, r, depth, mat):
    bpy.ops.mesh.primitive_cylinder_add(radius=r, depth=depth, location=loc, vertices=48)
    o = bpy.context.object; o.name = name
    o.modifiers.new("bev", "BEVEL").width = min(r, depth) * 0.3; o.modifiers["bev"].segments = 4
    smooth(o, 2); o.data.materials.append(mat); return o


def ring(name, loc, R, r, rot, mat, scale=(1, 1, 1)):
    bpy.ops.mesh.primitive_torus_add(major_radius=R, minor_radius=r, location=loc, rotation=rot, major_segments=48, minor_segments=12)
    o = bpy.context.object; o.name = name; o.scale = scale; o.data.materials.append(mat)
    for pg in o.data.polygons: pg.use_smooth = True
    return o


def arc_panel(name, R, w, z0, z1, mat):
    """Cloth bent around the body: the front arc |x| <= w of an open cylinder of radius R from z0 to z1.
    Returns the real half-width (the arc ends on a cylinder vertex)."""
    bpy.ops.mesh.primitive_cylinder_add(vertices=128, radius=R, depth=z1 - z0, location=(0, 0, (z0 + z1) / 2), end_fill_type="NOTHING")
    o = bpy.context.object; o.name = name
    bm = bmesh.new(); bm.from_mesh(o.data)
    bmesh.ops.delete(bm, geom=[v for v in bm.verts if v.co.y > 0 or abs(v.co.x) > w], context="VERTS")
    w = max(abs(v.co.x) for v in bm.verts); bm.to_mesh(o.data); bm.free()
    o.modifiers.new("solid", "SOLIDIFY").thickness = 0.035
    for pg in o.data.polygons: pg.use_smooth = True
    o.data.materials.append(mat); return w


def hat(species):
    """An empty on the crown of the head (HATS), tilted so that a hat faces the camera; returns it and the hat radius."""
    hz, hy, rad, tilt = HATS[species]
    bpy.ops.object.empty_add(location=(0, hy, hz)); pivot = bpy.context.object; pivot.rotation_euler = (math.radians(tilt), 0, 0)
    return pivot, rad


def add_accessory(kind, species, resident=None):
    """A resident's accessory, the same colours for everyone (a cap: CAP_COLORS); it never covers the eyes or the mouth."""
    if kind == "apron":  # a flat matte panel bent around the belly, wider than it, with a straight top edge; piping on the
        # top and the sides, a belt, a pocket in the middle of the bib and straps to its corners are dark (decision 36)
        cloth = material("apron", hexc("#FFF3D6"), rough=0.9, sss=0.1, coat=0.0)
        trim = material("apron_trim", hexc("#520978"), rough=0.9, sss=0.0, coat=0.0)
        R, z0, zb, z1 = 1.02, 0.25, 0.95, 1.5  # belly in build_pet: front y -0.96, half-width 0.65, z 0.14..1.56
        w = arc_panel("apron", R, 0.69, z0, z1, cloth)  # with the piping 1.12 x the belly's width
        yw = -math.sqrt(R * R - w * w)
        arc = lambda z, r, x: [(x * k / 3, -math.sqrt(r * r - (x * k / 3) ** 2), z) for k in range(-3, 4)]
        curve("apron_trim", arc(z1, R, w), 0.044, trim)
        curve("apron_belt", arc(zb, R + 0.03, w + 0.02), 0.055, trim)
        for sx in (-1, 1):
            curve("apron_trim", [(sx * w, yw, z0), (sx * w, yw, z1)], 0.044, trim)
            curve("apron_strap", [(sx * w, yw, z1), (sx * (w - 0.08), yw + 0.25, z1 + 0.2), (sx * 0.45, -0.15, 1.85)], 0.044, trim)
        box("apron_pocket", (0, -R - 0.03, (zb + z1) / 2), (0.34, 0.04, 0.24), trim, 0.03)
    elif kind == "cap":  # a dome on the crown of the head, visor tipped down to face the camera; bunny: like the crown
        dome, visor = CAP_COLORS.get(resident, ("#520978", "#FF0053"))
        felt = material("cap", hexc(dome), rough=0.6, sss=0.1, coat=0.2)
        peak = material("cap_visor", hexc(visor), rough=0.45, sss=0.1, coat=0.3)
        pivot, rad = hat(species)
        smooth(sphere("cap", (0, 0, 0), rad, (1.0, 1.0, 0.85), felt)).parent = pivot
        smooth(sphere("cap_visor", (0, -rad * 0.9, -0.03), rad * 0.85, (1.05, 0.9, 0.2), peak, (math.radians(20), 0, 0))).parent = pivot
        smooth(sphere("cap_button", (0, 0, rad * 0.85), 0.07, (1, 1, 0.6), peak), 1).parent = pivot
    elif kind == "glasses":  # round rims around the eyes (eye coordinates in build_pet) and a bridge
        rim = material("glasses", hexc("#2B2B2B"), rough=0.25, coat=0.8, sss=0.0)
        for sx in (-1, 1):
            bpy.ops.mesh.primitive_torus_add(major_radius=0.25, minor_radius=0.055, location=(0.3 * sx, -1.0, 2.28),
                                             rotation=(math.radians(90), 0, math.radians(15 * sx)), major_segments=48, minor_segments=12)
            o = bpy.context.object; o.name = "glasses_rim"; o.scale = (1.0, 1.1, 1.0); o.data.materials.append(rim)
            for pg in o.data.polygons: pg.use_smooth = True
        curve("glasses_bridge", [(-0.05, -1.03, 2.34), (0, -1.05, 2.37), (0.05, -1.03, 2.34)], 0.04, rim)


def add_role_prop(kind, species, m_body):
    """What a resident's job looks like (ROLE_PROPS), over the accessory; hats sit above the eyes, the rest below the mouth.
    ADULT_FRAME props are placed in the adult's own coordinates (grow_up leaves them be), held ones at the left hip."""
    white = material("role_white", hexc("#F7F7F7"), rough=0.8, sss=0.1, coat=0.0)
    accent = material("acc_accent", hexc("#FF0053"), rough=0.45, sss=0.1, coat=0.3)
    dark = material("apron_trim", hexc("#520978"), rough=0.9, sss=0.0, coat=0.0)
    gold = material("gold", hexc("#FFC94D"), rough=0.2, coat=0.9, sss=0.0)
    hand = lambda loc: smooth(sphere("hand", loc, 0.19, (1, 0.9, 0.9), m_body), 1)
    parts = []
    if kind == "doctor":  # a white cap with a heart (not the red cross: a protected emblem) and a stethoscope on the neck
        pivot, rad = hat(species); h, y, z = rad * 0.27, -rad * 1.05 - 0.02, rad * 0.1
        parts += [drum("doctor_cap", (0, 0, z), rad * 1.05, rad * 0.95, white),
                  smooth(sphere("heart", (-h * 0.8, y, z + h * 0.55), h, (1, 0.5, 1), accent), 1),
                  smooth(sphere("heart", (h * 0.8, y, z + h * 0.55), h, (1, 0.5, 1), accent), 1),
                  cone("heart", (0, y, z - h * 0.55), h * 1.7, h * 2.2, (math.pi, 0, 0), accent, (1, 0.5, 1))]
        tube = material("steth", hexc("#2B2B2B"), rough=0.35, sss=0.0, coat=0.5)
        metal = material("steth_metal", hexc("#D5DCE6"), rough=0.2, sss=0.0, coat=0.8)
        curve("steth_tube", [(-0.5, -0.3, 1.75), (-0.48, -0.8, 1.38), (-0.3, -1.0, 1.12)], 0.04, tube)
        smooth(sphere("steth_tip", (-0.3, -1.01, 1.1), 0.07, (1, 1, 1), tube), 1)
        curve("steth_tube", [(0.5, -0.3, 1.75), (0.48, -0.8, 1.38), (0.36, -1.0, 1.05), (0.3, -1.03, 0.82)], 0.04, tube)
        smooth(sphere("steth_rim", (0.3, -1.03, 0.68), 0.17, (1, 0.35, 1), tube), 1)
        smooth(sphere("steth_disc", (0.3, -1.07, 0.68), 0.12, (1, 0.3, 1), metal), 1)
    elif kind == "toque":  # a baker's white hat: a band and a puffy top wider than it
        pivot, rad = hat(species)
        parts += [drum("toque", (0, 0, rad * 0.2), rad * 0.85, rad * 0.9, white)]
        parts += [smooth(sphere("toque_puff", (x * rad, 0, (1.05 if x else 1.25) * rad), rad * 0.6, (1, 1, 0.9), white))
                  for x in (-0.45, 0.45, 0)]
    elif kind == "beret":  # an artist's beret tipped to one side, with a stalk
        pivot, rad = hat(species)
        felt = material("beret", hexc("#FF0053"), rough=0.9, sss=0.1, coat=0.0)
        parts += [smooth(sphere("beret", (rad * 0.12, 0, rad * 0.1), rad * 1.25, (1, 0.95, 0.3), felt, (0, math.radians(-10), 0))),
                  smooth(sphere("beret_stalk", (rad * 0.18, 0, rad * 0.5), rad * 0.13, (1, 1, 1.3), felt), 1)]
    elif kind == "bag":  # a postman's bag across the chest, its flap an envelope's triangle
        paper = material("bag_flap", hexc("#FFF3D6"), rough=0.9, sss=0.1, coat=0.0)
        curve("bag_strap", [(0.62, -0.5, 1.72), (0.3, -0.98, 1.3), (-0.2, -1.03, 0.95), (-0.58, -0.86, 0.66)], 0.05, dark)
        box("bag", (-0.62, -0.7, 0.5), (0.62, 0.2, 0.52), dark, 0.05)
        bpy.ops.mesh.primitive_cone_add(vertices=3, radius1=0.28, radius2=0, depth=0.04, location=(-0.62, -0.83, 0.61),
                                        rotation=(math.radians(-90), 0, 0))
        bpy.context.object.name = "bag_flap"; bpy.context.object.data.materials.append(paper)
    elif kind == "ball":  # a ball tucked at the hip under the paw, stripes in accents (not the body's colour)
        smooth(sphere("ball", (-1.02, -0.3, 0.95), 0.34, (1, 1, 1), accent), 1)
        for a in (-35, 35): ring("ball_stripe", (-1.02, -0.3, 0.95), 0.335, 0.045, (0, math.radians(90), math.radians(a)), gold)
        hand((-1.25, -0.45, 1.0))
    elif kind == "wrench":  # an open-end wrench in the paw: steel over a dark copy behind it, so it reads on a white card
        metal = material("steth_metal", hexc("#D5DCE6"), rough=0.2, sss=0.0, coat=0.8)
        bpy.ops.object.empty_add(location=(-0.88, -0.5, 0.9), rotation=(0, math.radians(-20), 0)); pivot = bpy.context.object; pivot.name = "wrench"
        jaw = [(0.2 * math.cos(math.radians(a)), 1.05 + 0.2 * math.sin(math.radians(a))) for a in (125, 200, 270, 340, 415)]
        for m, d in ((dark, 0.03), (metal, 0)):
            parts += [box("wrench", (0, d, 0.43), (0.16 + 2 * d, 0.06, 1.0 + 2 * d), m, 0.03), curve("wrench", [(x, d, z) for x, z in jaw], 0.07 + d, m)]
        parts.append(hand((0, -0.12, 0.12)))
    elif kind == "coin":  # a cashier's gold badge on the bib instead of the pocket: a disc, a dark edge, a raised rim
        bpy.data.objects.remove(bpy.data.objects["apron_pocket"]); up = (math.radians(90), 0, 0)
        drum("coin", (0, -0.95, 1.56), 0.2, 0.05, gold).rotation_euler = up
        ring("coin_edge", (0, -0.95, 1.56), 0.2, 0.03, up, dark); ring("coin_rim", (0, -0.98, 1.56), 0.12, 0.02, up, gold)
    elif kind == "palette":  # an artist's palette in the paw: a flat oval with a dark edge, a thumb hole and four dabs of paint
        bpy.ops.object.empty_add(location=(-1.1, -0.55, 1.05), rotation=(math.radians(75), math.radians(-15), 0)); pivot = bpy.context.object; pivot.name = "palette"
        parts += [drum("palette", (0, 0, 0), 0.36, 0.05, white), ring("palette_edge", (0, 0, 0), 0.36, 0.022, (0, 0, 0), dark, (1.3, 1, 1)),
                  ring("palette_hole", (0.28, 0.05, 0.03), 0.065, 0.022, (0, 0, 0), dark), hand((0.44, -0.2, 0.04))]
        parts[0].scale = (1.3, 1, 1)
        parts += [smooth(sphere("palette_paint", (x, y, 0.03), 0.085, (1, 1, 0.45), material("paint", hexc(h), rough=0.3, sss=0.0, coat=0.6)), 1)
                  for x, y, h in ((-0.35, 0.02, "#FF0053"), (-0.2, 0.2, "#FFC94D"), (0.02, 0.24, "#6FB1E0"), (-0.18, -0.18, "#7BC47F"))]
    for o in parts: o.parent = pivot


BODY_PARTS = ("body", "belly", "tail", "apron", "bag", "steth")
ADULT_FRAME = ("ball", "wrench", "coin", "palette", "hand")  # regrouped, a ball or a coin would be squashed by the body's scale


def grow_up(m_body):
    """Adult silhouette of a resident (owner's decision 34) in the pet's own frame: the parts are regrouped under empties,
    the head (with hats and glasses) smaller and lifted, the body (with apron, bag) taller and slimmer, feet on long legs."""
    s, kx, kz, lift, drop = 0.82, 0.9, 1.15, 0.15, 0.5
    top = lift + 1.9 * kz  # the body sphere spans z 0..1.9 in build_pet
    roots = [o for o in bpy.context.scene.objects if o.parent is None and not o.name.startswith(ADULT_FRAME)]
    group = {}
    for name, loc, scale in (("head", (0, -0.15 * (1 - s), top - 1.9 * s), (s, s, s)), ("body", (0, 0, lift), (kx, kx, kz)),
                             ("legs", (0, 0, -drop), (1, 1, 1))):
        bpy.ops.object.empty_add(location=loc); group[name] = bpy.context.object; group[name].scale = scale
    for o in roots:
        o.parent = group["legs" if o.name.startswith("paw") else "body" if o.name.startswith(BODY_PARTS) else "head"]
    for sx in (-1, 1):
        smooth(sphere("leg", (0.44 * sx, -0.4, 0.12), 0.22, (1, 1, 1.8), m_body))


def build_pet(species, color_hex, stage, face, accessory=None, resident=None):
    BODY = hexc(color_hex)
    DARK = mix(BODY, (0, 0, 0, 1), 0.35)
    LIGHT = mix(BODY, (1, 1, 1, 1), 0.38)
    PINK = hexc("#FFB3C1"); NOSE = hexc("#F08AA0"); INK = hexc("#2B2B2B"); WHITE = (1, 1, 1, 1)
    m_body = material("body", BODY); m_belly = material("belly", LIGHT, sss=0.35)
    m_dark = material("dark", DARK); m_pink = material("pink", PINK, sss=0.4)
    m_nose = material("nose", NOSE, rough=0.25, coat=0.6); m_ink = material("ink", INK, rough=0.2, coat=0.8, sss=0.0)
    m_white = material("white", WHITE, rough=0.3, coat=0.5, sss=0.0)
    m_cheek = material("cheek", hexc("#FF6B7A"), alpha=0.55, sss=0.0, coat=0.0)
    m_gold = material("gold", hexc("#FFC94D"), rough=0.2, coat=0.9, sss=0.0)
    m_red = material("red", hexc("#E63946"), rough=0.5, sss=0.2, coat=0.2)
    m_glint = material("glint", WHITE, emit=1.5)

    # body & head (front = -Y)
    smooth(sphere("body", (0, 0, 0.95), 0.95, (1.0, 0.92, 1.0), m_body))
    smooth(sphere("belly", (0, -0.62, 0.85), 0.62, (1.05, 0.55, 1.15), m_belly))
    smooth(sphere("head", (0, -0.15, 2.15), 0.82, (1.0, 0.95, 0.95), m_body))
    for sx in (-1, 1):
        smooth(sphere("paw", (0.48 * sx, -0.62, 0.3), 0.3, (1.05, 1.0, 0.75), m_body))
        smooth(sphere("cheek", (0.52 * sx, -0.7, 1.98), 0.15, (1.0, 0.45, 0.8), m_cheek))
        ex, ey, ez = 0.3 * sx, -0.86, 2.28
        if face == "blink":
            curve("lid", [(ex - 0.13, ey - 0.02, ez + 0.02), (ex, ey - 0.06, ez - 0.05), (ex + 0.13, ey - 0.02, ez + 0.02)], 0.025, m_ink)
        else:
            smooth(sphere("sclera", (ex, ey, ez), 0.17, (1.0, 0.7, 1.15), m_white), 1)
            smooth(sphere("pupil", (ex + 0.02 * sx, ey - 0.1, ez - 0.01), 0.11, (1.0, 0.6, 1.2), m_ink), 1)
            smooth(sphere("glint", (ex - 0.04 * sx, ey - 0.19, ez + 0.06), 0.035, (1, 1, 1), m_glint), 1)
        if face == "sad":  # soft worried brows
            curve("brow", [(ex - 0.15 * sx, ey + 0.0, ez + 0.31), (ex + 0.13 * sx, ey - 0.03, ez + 0.23)], 0.022, m_ink)
    my = -0.05 if species == "puppy" else 0.0  # the puppy's muzzle pushes the mouth forward
    mouth = lambda pts, r: curve("mouth", [(x, y + my, z) for x, y, z in pts], r, m_ink)
    if face in ("happy", "blink"):
        mouth([(-0.2, -0.93, 1.9), (0, -0.98, 1.8), (0.2, -0.93, 1.9)], 0.028)
    elif face == "sad":
        mouth([(-0.18, -0.93, 1.78), (0, -0.98, 1.88), (0.18, -0.93, 1.78)], 0.028)
    else:
        mouth([(-0.14, -0.94, 1.84), (0, -0.97, 1.815), (0.14, -0.94, 1.84)], 0.026)

    if species == "cat":
        for sx in (-1, 1):
            cone("ear", (0.5 * sx, -0.05, 2.9), 0.3, 0.62, (math.radians(-12), math.radians(28 * sx), 0), m_body)
            cone("ear_in", (0.5 * sx, -0.11, 2.86), 0.17, 0.42, (math.radians(-12), math.radians(28 * sx), 0), m_pink)
            for k in range(3):
                z = 1.98 - 0.06 * k
                curve("whisker", [(0.45 * sx, -0.82, z + 0.02), (0.95 * sx, -0.78, z + (k - 1) * 0.1 - 0.03)], 0.009, m_dark)
        smooth(sphere("nose", (0, -0.97, 2.03), 0.07, (1.5, 0.7, 0.9), m_nose), 1)
        for i in (-1, 0, 1):
            smooth(sphere("stripe", (0.21 * i, -0.5, 2.8 + (0.04 if i == 0 else 0)), 0.05, (0.8, 0.45, 2.2), m_dark, (math.radians(-38), 0, 0)), 1)
        curve("tail", [(0.75, 0.55, 0.5), (1.45, 0.45, 0.9), (1.5, 0.2, 1.7)], 0.14, m_body, taper=True)
    elif species == "bunny":
        for sx in (-1, 1):
            rot = (math.radians(-6), math.radians(12 * sx), 0)
            smooth(sphere("ear", (0.38 * sx, 0.08, 3.35), 0.24, (0.85, 0.65, 2.4), m_body, rot))
            smooth(sphere("ear_in", (0.38 * sx, -0.05, 3.35), 0.13, (0.85, 0.5, 1.9), m_pink, rot))
        smooth(sphere("nose", (0, -0.97, 2.02), 0.08, (1.3, 0.7, 0.8), m_nose), 1)
        if face != "sad": smooth(sphere("tooth", (0, -0.9, 1.74), 0.07, (1.6, 0.4, 0.9), m_white), 1)
        smooth(sphere("tail", (0, 0.95, 0.75), 0.26, (1, 1, 1), m_belly))
    else:  # puppy: floppy ears, light muzzle with a button nose, a short wagging tail
        for sx in (-1, 1):
            smooth(sphere("ear", (0.84 * sx, -0.1, 2.25), 0.3, (0.64, 0.42, 1.7), m_dark, (math.radians(6), math.radians(-24 * sx), 0)))
        smooth(sphere("muzzle", (0, -0.76, 1.92), 0.3, (1.2, 0.67, 0.73), m_belly))
        smooth(sphere("nose", (0, -1.0, 2.04), 0.09, (1.5, 0.8, 0.95), m_ink), 1)
        if face in ("happy", "blink"): smooth(sphere("tongue", (0, -0.97, 1.72), 0.08, (1.1, 0.5, 1.1), m_nose), 1)
        curve("tail", [(0.6, 0.6, 0.65), (1.05, 0.65, 0.95), (1.22, 0.5, 1.45)], 0.13, m_body, taper=True)

    if stage >= 1:  # bandana: a ring hugging the neck, a knot and a hanging tip in front of the belly
        bpy.ops.mesh.primitive_torus_add(major_radius=0.7, minor_radius=0.15, location=(0, -0.05, 1.45), major_segments=64, minor_segments=16)
        ring = bpy.context.object; ring.name = "bandana"; ring.scale = (1.0, 0.95, 0.75); ring.rotation_euler = (math.radians(8), 0, 0)
        for pg in ring.data.polygons: pg.use_smooth = True
        ring.data.materials.append(m_red)
        smooth(sphere("knot", (0.34, -0.8, 1.38), 0.14, (1.1, 0.8, 0.9), m_red), 1)
        cone("bandana_tip", (0.4, -0.98, 1.08), 0.15, 0.5, (math.radians(95), 0, math.radians(-25)), m_red)
    if stage >= 2:
        # the bunny wears the crown tilted on the forehead, in front of the ears
        bunny = species == "bunny"
        hz, hy, rad, tilt = (2.78, -0.42, 0.36, math.radians(-22)) if bunny else (2.95, -0.1, 0.42, 0.0)
        bpy.ops.object.empty_add(location=(0, hy, hz)); pivot = bpy.context.object; pivot.rotation_euler = (tilt, 0, 0)
        bpy.ops.mesh.primitive_cylinder_add(vertices=6, radius=rad, depth=0.28, location=(0, 0, 0))
        crown = bpy.context.object; crown.name = "crown"; crown.data.materials.append(m_gold); crown.parent = pivot
        for k in range(6):
            a = k * math.pi / 3
            pt = cone("crown_pt", (rad * math.cos(a), rad * math.sin(a), 0.3), 0.1, 0.3, (0, 0, 0), m_gold); pt.parent = pivot
        gem = smooth(sphere("gem", (0, -rad, 0.02), 0.09, (1, 0.6, 1.2), material("gem", hexc("#5CC8FF"), rough=0.1, coat=1.0, sss=0.0)), 1)
        gem.parent = pivot
    if accessory: add_accessory(accessory, species, resident)
    for kind in ROLE_PROPS.get(resident, ()): add_role_prop(kind, species, m_body)
    if resident: grow_up(m_body)


def light(name, kind, loc, energy, size=3.0, color=(1, 1, 1), target=(0, 0, 1.6)):
    ld = bpy.data.lights.new(name, kind); ld.energy = energy; ld.color = color
    if kind == "AREA": ld.size = size
    o = bpy.data.objects.new(name, ld); bpy.context.collection.objects.link(o); o.location = loc
    c = o.constraints.new("TRACK_TO"); tgt = bpy.data.objects.new(name + "_t", None); tgt.location = target
    bpy.context.collection.objects.link(tgt); c.target = tgt; c.track_axis = "TRACK_NEGATIVE_Z"; c.up_axis = "UP_Y"
    return o


def setup_lights_camera(scene):
    light("key", "AREA", (-3.5, -5, 6), 750, 4.0, (1.0, 0.96, 0.9))
    light("fill", "AREA", (5, -4, 2.5), 180, 5.0, (0.85, 0.9, 1.0))
    light("rim", "AREA", (1.5, 5, 4.5), 600, 3.0, (1.0, 0.85, 1.0))
    world = bpy.data.worlds.new("w"); scene.world = world; world.use_nodes = True
    bg = world.node_tree.nodes["Background"]; bg.inputs[0].default_value = (0.75, 0.7, 0.85, 1); bg.inputs[1].default_value = 0.22
    # One framing for every species and stage so sprites never jump when the pet changes: fits bunny ears + crown.
    cam_d = bpy.data.cameras.new("cam"); cam_d.lens = 62
    cam = bpy.data.objects.new("cam", cam_d); bpy.context.collection.objects.link(cam)
    cam.location = (0.3, -8.8, 2.9)
    c = cam.constraints.new("TRACK_TO"); t = bpy.data.objects.new("cam_t", None); t.location = (0, 0, 1.75)
    bpy.context.collection.objects.link(t); c.target = t; c.track_axis = "TRACK_NEGATIVE_Z"; c.up_axis = "UP_Y"
    scene.camera = cam


def render(species, color_hex, stage, face, out, accessory=None, resident=None):
    MATS.clear()
    scene = setup_scene(A.samples, A.size)
    build_pet(species, color_hex, stage, face, accessory, resident)
    setup_lights_camera(scene)
    scene.render.filepath = out
    bpy.ops.render.render(write_still=True)
    print("rendered", out, flush=True)


if A.residents:
    residents = json.load(open(CONTENT, encoding="utf-8"))["town"]["residents"]
    # everything is checked before the first frame: a new resident is a row in RESIDENT_COLORS, there is no fallback
    for r in residents:
        if r["id"] not in RESIDENT_COLORS: sys.exit(f"resident {r['id']!r} has no colour in RESIDENT_COLORS (pet.py)")
        if r["look"].get("accessory") not in (None, *ACCESSORIES): sys.exit(f"resident {r['id']!r}: unknown accessory {r['look']['accessory']!r}")
    for i in [*ROLE_PROPS, *CAP_COLORS, *([A.only_resident] if A.only_resident else [])]:
        if i not in [r["id"] for r in residents]: sys.exit(f"unknown resident {i!r}: not in town.residents of {CONTENT}")
    os.makedirs(A.residents, exist_ok=True)
    for r in residents:
        if A.only_resident in (None, r["id"]):
            render(r["look"]["species"], RESIDENT_COLORS[r["id"]], 0, "happy", os.path.join(A.residents, f"res_{r['id']}.png"),
                   r["look"].get("accessory"), r["id"])
elif A.all:
    os.makedirs(A.all, exist_ok=True)
    for sp in [A.only_species] if A.only_species else SPECIES:
        for cid, chex in COLORS.items():
            for st in STAGES:
                for f in FACES:
                    out = os.path.join(A.all, f"pet_{sp}_{cid}_{st}_{f}.png")
                    if not os.path.exists(out): render(sp, chex, st, f, out)
else:
    render(A.species, A.color, A.stage, A.face, A.out or os.path.abspath("pet.png"), A.accessory)
