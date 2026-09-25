"""Procedural toy-style pets for Finny, rendered with Blender (Cycles, transparent background).

One frame:  Blender -b -P tools/art/pet.py -- --species cat --color '#F4A261' --face happy --stage 1 --out /abs/cat.png
Full set:   Blender -b -P tools/art/pet.py -- --all /abs/outdir [--only-species puppy] [--size 512 --samples 96]
            renders species x colour x stage x face (3 x 3 x 3 x 4 = 108 frames) as pet_<species>_<colour>_<stage>_<face>.png;
            existing PNGs are skipped. Then: python tools/art/import_sprites.py /abs/outdir

Colours and species ids mirror app/src/main/assets/content/content.json. The ground shadow is drawn by the
app (PetView), so no shadow catcher here. All geometry is primitives + subdivision: the art belongs to the team.
"""
import bpy, math, sys, argparse, os
from mathutils import Vector

SPECIES = ["cat", "bunny", "puppy"]
COLORS = {"orange": "#F4A261", "blue": "#6FB1E0", "green": "#7BC47F"}
FACES = ["happy", "neutral", "sad", "blink"]
STAGES = [0, 1, 2]

argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
ap = argparse.ArgumentParser()
ap.add_argument("--species", default="cat"); ap.add_argument("--color", default="#F4A261")
ap.add_argument("--face", default="happy"); ap.add_argument("--stage", type=int, default=0)
ap.add_argument("--out", default=None); ap.add_argument("--all", default=None)
ap.add_argument("--samples", type=int, default=96); ap.add_argument("--size", type=int, default=512)
ap.add_argument("--only-species", default=None, choices=SPECIES)
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


def build_pet(species, color_hex, stage, face):
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


def render(species, color_hex, stage, face, out):
    MATS.clear()
    scene = setup_scene(A.samples, A.size)
    build_pet(species, color_hex, stage, face)
    setup_lights_camera(scene)
    scene.render.filepath = out
    bpy.ops.render.render(write_still=True)
    print("rendered", out, flush=True)


if A.all:
    os.makedirs(A.all, exist_ok=True)
    for sp in [A.only_species] if A.only_species else SPECIES:
        for cid, chex in COLORS.items():
            for st in STAGES:
                for f in FACES:
                    out = os.path.join(A.all, f"pet_{sp}_{cid}_{st}_{f}.png")
                    if not os.path.exists(out): render(sp, chex, st, f, out)
else:
    render(A.species, A.color, A.stage, A.face, A.out or os.path.abspath("pet.png"))
