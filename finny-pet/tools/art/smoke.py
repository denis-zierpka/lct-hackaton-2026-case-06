"""Smoke test for lib.py: a coin and a glass jar. Blender -b -P tools/art/smoke.py -- /abs/out.png"""
import sys, os; sys.path.insert(0, os.path.dirname(os.path.abspath(__file__))); from lib import *
out = sys.argv[sys.argv.index("--") + 1]
scene = reset_scene(64, 512)
gold = metal("gold", hexc("#FFC94D"), rough=0.3)
cylinder("coin", (-0.9, 0, 0.6), 0.6, 0.14, gold, rot=(math.radians(75), 0, math.radians(-20)), bevel=0.04)
torus("rim", (-0.9, 0, 0.6), 0.5, 0.05, metal("gold2", hexc("#E0A800")), rot=(math.radians(75), 0, math.radians(-20)))
jar = cylinder("jar", (0.8, 0, 0.7), 0.5, 1.4, glass("glass", (0.85, 0.95, 1.0, 1.0)), bevel=0.12)
cylinder("lid", (0.8, 0, 1.45), 0.56, 0.16, material("lid", hexc("#520978")), bevel=0.05)
for i in range(4):
    cylinder("c%d" % i, (0.8 + (i % 2) * 0.2 - 0.1, (i // 2) * 0.2 - 0.1, 0.1 + i * 0.16), 0.3, 0.12, gold, bevel=0.03, rot=(0, 0, i * 0.6))
studio(scene, target=(0, 0, 0.7)); shadow_ground(); prop_camera(scene, height=1.6, radius=1.4)
render(scene, out)
