#!/usr/bin/env python3
"""Tiny adb/uiautomator driver for walking the demo scenario on an emulator.

Usage: python3 tools/ui.py <command> [args]
  tap <text> [index]      tap the first node whose text/desc contains <text>; "exact:<text>" matches the whole text
  tapnear <anchor> <text> tap the <text> node vertically closest to the <anchor> node
  tapxy <x> <y>           tap coordinates
  type <ascii>            type text into the focused field
  key <keyevent>          send a key event (e.g. 4 = back)
  scroll [up]             scroll the screen down (or up)
  shot <name>             save screenshot to screenshots/<name>.png
  find <text>             print matching nodes
  tree                    print all texts on screen
  wait <seconds>
Commands can be chained with ';' as separate argv groups: ui.py tap Дальше ; shot x
"""
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ADB = "/opt/homebrew/share/android-commandlinetools/platform-tools/adb"
SHOTS = Path(__file__).resolve().parent.parent / "screenshots"


def adb(*args, binary=False):
    r = subprocess.run([ADB, *args], capture_output=True)
    return r.stdout if binary else r.stdout.decode(errors="replace")


def dump():
    for _ in range(5):
        adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
        x = adb("shell", "cat", "/sdcard/ui.xml")
        if x.strip().startswith("<?xml"):
            try:
                return ET.fromstring(x)
            except ET.ParseError:
                pass
        time.sleep(0.5)
    raise SystemExit("ui dump failed")


def nodes(text):
    """Substring match on text/desc; an 'exact:' prefix requires the exact text (e.g. 'exact:30' for a chip)."""
    exact = text.startswith("exact:")
    want = text[len("exact:"):] if exact else text
    out = []
    for n in dump().iter("node"):
        t = n.get("text") or ""
        blob = t + "|" + (n.get("content-desc") or "")
        if (t == want) if exact else (want in blob):
            out.append(n)
    return out


def center(n):
    b = n.get("bounds")  # [x1,y1][x2,y2]
    x1, y1, x2, y2 = [int(v) for v in b.replace("][", ",").strip("[]").split(",")]
    return (x1 + x2) // 2, (y1 + y2) // 2


def scroll(up=False):
    if up:
        adb("shell", "input", "swipe", "540", "700", "540", "1900", "900")
    else:
        adb("shell", "input", "swipe", "540", "1900", "540", "700", "900")
    time.sleep(0.45)


def locate(text, idx=0):
    """Find a node, scrolling down and then back up when it is not on screen yet."""
    plan = [None, None, "down", "down", "up", "up", "up"]
    for step in plan:
        if step == "down": scroll()
        elif step == "up": scroll(up=True)
        else: time.sleep(0.3)
        ns = nodes(text)
        if (len(ns) > idx) if idx >= 0 else (len(ns) >= -idx):
            return ns
    raise SystemExit(f"not found: {text}")


def tap_text(text, idx=0):
    x, y = center(locate(text, idx)[idx])
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(0.45)


def shot(name):
    """Save a screenshot: a bare name goes to screenshots/<name>.png, a path is used as given."""
    target = Path(f"{name}.png") if "/" in name else SHOTS / f"{name}.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    png = adb("exec-out", "screencap", "-p", binary=True)
    target.write_bytes(png)
    print("shot", target, len(png))


def run(argv):
    cmd, args = argv[0], argv[1:]
    if cmd == "tap":
        tap_text(args[0], int(args[1]) if len(args) > 1 else 0)
    elif cmd == "tapnear":  # tapnear <anchor text> <button text>: button vertically closest to the anchor
        anchor = locate(args[0])
        ay = center(anchor[0])[1]
        below = [n for n in nodes(args[1]) if center(n)[1] >= ay]  # a card's button sits under its title
        if not below:
            raise SystemExit(f"not found below {args[0]}: {args[1]}")
        best = min(below, key=lambda n: center(n)[1] - ay)
        x, y = center(best)
        adb("shell", "input", "tap", str(x), str(y)); time.sleep(0.45)
    elif cmd == "gate":  # solve the parent-section arithmetic example on screen
        import re
        for n in nodes("= ?"):
            m = re.search(r"(\d+)\s*×\s*(\d+)", n.get("text") or "")
            if m:
                tap_text("= ?")  # focus is on the field below; tap the field itself
                fields = [f for f in dump().iter("node") if f.get("class") == "android.widget.EditText"]
                if fields:
                    x, y = center(fields[0]); adb("shell", "input", "tap", str(x), str(y)); time.sleep(0.5)
                adb("shell", "input", "text", str(int(m.group(1)) * int(m.group(2)))); time.sleep(0.3)
                adb("shell", "input", "keyevent", "4"); time.sleep(0.5)
                tap_text("Войти")
                break
        else:
            raise SystemExit("gate not found")
    elif cmd == "tapxy":
        adb("shell", "input", "tap", args[0], args[1]); time.sleep(0.45)
    elif cmd == "type":
        adb("shell", "input", "text", args[0]); time.sleep(0.4)
    elif cmd == "key":
        adb("shell", "input", "keyevent", args[0]); time.sleep(0.6)
    elif cmd == "scroll":
        scroll(up=bool(args and args[0] == "up"))
    elif cmd == "shot":
        shot(args[0])
    elif cmd == "find":
        for n in nodes(args[0]):
            print(center(n), n.get("text"), n.get("content-desc"))
    elif cmd == "tree":
        for n in dump().iter("node"):
            t = n.get("text") or n.get("content-desc")
            if t:
                print(center(n), t)
    elif cmd == "wait":
        time.sleep(float(args[0]))
    else:
        raise SystemExit(f"unknown command {cmd}")


if __name__ == "__main__":
    group = []
    for a in sys.argv[1:] + [";"]:
        if a == ";":
            if group:
                run(group)
            group = []
        else:
            group.append(a)
