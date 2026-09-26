"""Shared Blender helpers for Finny's toy-style renders (pets, props, room).

Style contract (keep every asset consistent):
  * primitives + subdivision, no hard edges: `smooth()` on meshes, `cone()` gets a bevel
  * `material()` = Principled BSDF with soft subsurface, mild coat gloss; colours via `hexc()` (sRGB → linear)
  * three area lights (warm key front-left, cool fill right, pink rim behind) + violet world, see `studio()`
  * transparent film, PNG RGBA, Standard view transform, exposure -0.15
  * front of an object faces -Y; camera looks from -Y; ground is z = 0

Import from a script run inside Blender:
    import sys, os; sys.path.insert(0, os.path.dirname(os.path.abspath(__file__))); from lib import *
"""
import bpy, math
from mathutils import Vector

MATS = {}


def hexc(h, a=1.0):
    h = h.lstrip("#"); r, g, b = (int(h[i:i + 2], 16) / 255 for i in (0, 2, 4))
    lin = lambda c: c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4
    return (lin(r), lin(g), lin(b), a)


def mix(c1, c2, t):
    return tuple(c1[i] * (1 - t) + c2[i] * t for i in range(3)) + (1.0,)


def reset_scene(samples=96, size=512, width=None, height=None):
    """Fresh scene: Cycles on GPU if available (CPU fallback), transparent background, PNG RGBA."""
    MATS.clear()
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
    scene.render.resolution_x = width or size; scene.render.resolution_y = height or size
    scene.render.image_settings.file_format = "PNG"; scene.render.image_settings.color_mode = "RGBA"
    scene.view_settings.view_transform = "Standard"; scene.view_settings.look = "None"; scene.view_settings.exposure = -0.15
    return scene


def material(name, rgba, rough=0.45, sss=0.25, coat=0.35, alpha=1.0, emit=0.0, metallic=0.0, transmission=0.0):
    key = (name, rgba, rough, sss, coat, alpha, emit, metallic, transmission)
    if key in MATS and MATS[key].name in bpy.data.materials: return MATS[key]
    m = bpy.data.materials.new(name); m.use_nodes = True
    p = m.node_tree.nodes["Principled BSDF"]
    def s(n, v):
        if n in p.inputs: p.inputs[n].default_value = v
    s("Base Color", rgba); s("Roughness", rough); s("Alpha", alpha); s("Metallic", metallic)
    s("Subsurface Weight", sss); s("Subsurface Radius", (0.8, 0.35, 0.2)); s("Subsurface Scale", 0.25)
    s("Coat Weight", coat); s("Coat Roughness", 0.15)
    s("Transmission Weight", transmission)
    if emit: s("Emission Color", rgba); s("Emission Strength", emit)
    if (alpha < 1 or transmission > 0) and hasattr(m, "surface_render_method"): m.surface_render_method = "BLENDED"
    MATS[key] = m; return m


def glass(name, rgba, rough=0.05):
    return material(name, rgba, rough=rough, sss=0.0, coat=0.0, transmission=0.95, alpha=1.0)


def metal(name, rgba, rough=0.25):
    return material(name, rgba, rough=rough, sss=0.0, coat=0.6, metallic=0.9)


def smooth(o, levels=2):
    o.modifiers.new("subd", "SUBSURF").levels = levels
    o.modifiers["subd"].render_levels = levels
    for pg in o.data.polygons: pg.use_smooth = True
    return o


def sphere(name, loc, r, scale=(1, 1, 1), mat=None, rot=(0, 0, 0), levels=2):
    bpy.ops.mesh.primitive_uv_sphere_add(radius=r, location=loc, segments=48, ring_count=24)
    o = bpy.context.object; o.name = name; o.scale = scale; o.rotation_euler = rot
    for pg in o.data.polygons: pg.use_smooth = True
    if mat: o.data.materials.append(mat)
    if levels: smooth(o, levels)
    return o


def cylinder(name, loc, r, depth, mat, scale=(1, 1, 1), rot=(0, 0, 0), bevel=0.0, vertices=48):
    bpy.ops.mesh.primitive_cylinder_add(radius=r, depth=depth, location=loc, vertices=vertices)
    o = bpy.context.object; o.name = name; o.scale = scale; o.rotation_euler = rot
    if bevel:
        b = o.modifiers.new("bev", "BEVEL"); b.width = bevel; b.segments = 6
    for pg in o.data.polygons: pg.use_smooth = True
    o.data.materials.append(mat); return o


def box(name, loc, size, mat, rot=(0, 0, 0), bevel=0.06):
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=loc)
    o = bpy.context.object; o.name = name; o.scale = size; o.rotation_euler = rot
    if bevel:
        b = o.modifiers.new("bev", "BEVEL"); b.width = bevel; b.segments = 6
    for pg in o.data.polygons: pg.use_smooth = True
    o.data.materials.append(mat); return o


def cone(name, loc, r, depth, rot, mat, scale=(1, 1, 1), r2=0.0):
    bpy.ops.mesh.primitive_cone_add(radius1=r, radius2=r2, depth=depth, location=loc, vertices=48)
    o = bpy.context.object; o.name = name; o.rotation_euler = rot; o.scale = scale
    o.modifiers.new("bev", "BEVEL").width = max(r, r2) * 0.35
    o.modifiers["bev"].segments = 6
    smooth(o, 2)
    o.data.materials.append(mat); return o


def torus(name, loc, major, minor, mat, scale=(1, 1, 1), rot=(0, 0, 0)):
    bpy.ops.mesh.primitive_torus_add(major_radius=major, minor_radius=minor, location=loc, major_segments=64, minor_segments=16)
    o = bpy.context.object; o.name = name; o.scale = scale; o.rotation_euler = rot
    for pg in o.data.polygons: pg.use_smooth = True
    o.data.materials.append(mat); return o


def curve(name, pts, bevel, mat, taper=False, cyclic=False):
    cd = bpy.data.curves.new(name, "CURVE"); cd.dimensions = "3D"; cd.bevel_depth = bevel; cd.bevel_resolution = 6
    cd.use_fill_caps = True
    sp = cd.splines.new("BEZIER"); sp.bezier_points.add(len(pts) - 1); sp.use_cyclic_u = cyclic
    for bp, p in zip(sp.bezier_points, pts):
        bp.co = Vector(p); bp.handle_left_type = bp.handle_right_type = "AUTO"
    if taper:
        for i, bp in enumerate(sp.bezier_points): bp.radius = 1.0 - 0.75 * i / max(1, len(pts) - 1)
    o = bpy.data.objects.new(name, cd); bpy.context.collection.objects.link(o)
    cd.materials.append(mat); return o


def text3d(name, txt, loc, size, mat, extrude=0.06, rot=(math.radians(90), 0, 0), align="CENTER"):
    """Extruded text facing -Y (readable from the camera)."""
    td = bpy.data.curves.new(name, "FONT"); td.body = txt; td.size = size; td.extrude = extrude
    td.align_x = align; td.bevel_depth = size * 0.02
    o = bpy.data.objects.new(name, td); bpy.context.collection.objects.link(o)
    o.location = loc; o.rotation_euler = rot; td.materials.append(mat); return o


# Interior shell (room.py, place.py): floor is z = 0, the back wall stands at y = wall_y, x is squeezed by k for
# portrait framing (k = 1: landscape). Materials come from the caller, so every place keeps its own colours.
def plank_floor(mats, k=1.0, n=9, pitch=0.8, y0=-11.0, y1=5.0):
    """Planks running towards the back wall (perspective lines), 2n + 1 of them, tints cycle through `mats`."""
    for i in range(-n, n + 1):
        box("plank", (i * pitch * k, (y0 + y1) / 2, -0.06), ((pitch - 0.02) * k, y1 - y0, 0.12), mats[abs(i) % len(mats)], bevel=0.015)


def shell_walls(m_wall, m_side, m_skirt, k=1.0, wall_y=3.0, half=5.2, height=14.0, depth=10.0, reach=12.0, window=None, back=True):
    """Back wall (solid, or around a `window` hole (half-width, bottom z, top z) — half-width already squeezed),
    side walls at x = ±half·k running `depth` towards the camera, white skirting. back=False: no back wall and
    no back skirting (outdoor places show sky/river there). `reach` = how far the back wall runs past the hole."""
    h, r = height, reach * k
    if back and window:
        ww, z0, z1 = window
        box("wall_l", (-r / 2 - ww, wall_y + 0.15, h / 2), (r, 0.3, h), m_wall, bevel=0)
        box("wall_r", (r / 2 + ww, wall_y + 0.15, h / 2), (r, 0.3, h), m_wall, bevel=0)
        box("wall_b", (0, wall_y + 0.15, z0 / 2), (2 * ww, 0.3, z0), m_wall, bevel=0)
        box("wall_t", (0, wall_y + 0.15, (z1 + h) / 2), (2 * ww, 0.3, h - z1), m_wall, bevel=0)
    elif back:
        box("wall_back", (0, wall_y + 0.15, h / 2), (2 * r, 0.3, h), m_wall, bevel=0)
    for sx in (-1, 1):
        box("side", (half * k * sx, wall_y - depth / 2, h / 2), (0.3, depth, h), m_side, bevel=0)
        box("skirt_s", ((half - 0.15) * k * sx, wall_y - depth / 2, 0.11), (0.06, depth, 0.24), m_skirt, bevel=0.01)
    if back:
        box("skirt", (0, wall_y - 0.03, 0.11), ((2 * half - 0.2) * k, 0.07, 0.24), m_skirt, bevel=0.01)


def _track(o, target):
    c = o.constraints.new("TRACK_TO"); t = bpy.data.objects.new(o.name + "_t", None); t.location = target
    bpy.context.collection.objects.link(t); c.target = t; c.track_axis = "TRACK_NEGATIVE_Z"; c.up_axis = "UP_Y"


def light(name, kind, loc, energy, size=3.0, color=(1, 1, 1), target=(0, 0, 1.0)):
    ld = bpy.data.lights.new(name, kind); ld.energy = energy; ld.color = color
    if kind == "AREA": ld.size = size
    o = bpy.data.objects.new(name, ld); bpy.context.collection.objects.link(o); o.location = loc
    _track(o, target); return o


def studio(scene, target=(0, 0, 1.0), scale=1.0, world_strength=0.22):
    """The house lighting rig; `scale` grows the rig for bigger subjects (room = ~4)."""
    s = scale
    light("key", "AREA", (-3.5 * s, -5 * s, 6 * s), 750 * s * s, 4.0 * s, (1.0, 0.96, 0.9), target)
    light("fill", "AREA", (5 * s, -4 * s, 2.5 * s), 180 * s * s, 5.0 * s, (0.85, 0.9, 1.0), target)
    light("rim", "AREA", (1.5 * s, 5 * s, 4.5 * s), 600 * s * s, 3.0 * s, (1.0, 0.85, 1.0), target)
    world = bpy.data.worlds.new("w"); scene.world = world; world.use_nodes = True
    bg = world.node_tree.nodes["Background"]; bg.inputs[0].default_value = (0.75, 0.7, 0.85, 1); bg.inputs[1].default_value = world_strength


def camera(scene, loc, target, lens=62, ortho_scale=None):
    cam_d = bpy.data.cameras.new("cam"); cam_d.lens = lens
    if ortho_scale:
        cam_d.type = "ORTHO"; cam_d.ortho_scale = ortho_scale
    cam = bpy.data.objects.new("cam", cam_d); bpy.context.collection.objects.link(cam)
    cam.location = loc; _track(cam, target); scene.camera = cam; return cam


def prop_camera(scene, height=1.0, radius=1.0):
    """Default three-quarter view for a prop standing at the origin with the given bounding size."""
    d = max(height, radius * 2) * 2.6
    return camera(scene, (0.9 * radius + 0.35 * d, -d, height * 0.55 + 0.45 * d), (0, 0, height * 0.5), lens=60)


def shadow_ground(size=80):
    bpy.ops.mesh.primitive_plane_add(size=size, location=(0, 0, 0)); g = bpy.context.object
    g.is_shadow_catcher = True; g.data.materials.append(material("ground", (1, 1, 1, 1), sss=0.0, coat=0.0)); return g


def render(scene, out):
    scene.render.filepath = out
    bpy.ops.render.render(write_still=True)
    print("rendered", out, flush=True)
