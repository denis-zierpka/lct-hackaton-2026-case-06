"""Converts rendered PNGs (room/props/uiprops) to WebP with the same parameters as import_sprites.py.

Usage: python tools/art/to_webp.py SRC [SRC ...] --dst DIR [--size N | --size WxH] [--rgb]
SRC is a PNG file or a directory (its *.png files are used). --rgb converts to RGB (backgrounds);
without it, converts to RGBA (sprites). --size N resizes to NxN, --size WxH to that exact size.
"""
import argparse, os, sys
from pathlib import Path
from PIL import Image


def parse_size(s):
    if not s:
        return None
    if "x" in s.lower():
        w, h = s.lower().split("x")
        return int(w), int(h)
    n = int(s)
    return n, n


def collect(srcs):
    files = []
    for s in srcs:
        p = Path(s)
        if not p.exists():
            print(f"not found: {s}", file=sys.stderr)
            return None
        files += sorted(p.glob("*.png")) if p.is_dir() else [p]
    return files


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("src", nargs="+")
    ap.add_argument("--dst", required=True)
    ap.add_argument("--size")
    ap.add_argument("--rgb", action="store_true")
    A = ap.parse_args()

    files = collect(A.src)
    if files is None:
        return 1
    if not files:
        print("no PNG files found", file=sys.stderr)
        return 1

    size = parse_size(A.size)
    mode = "RGB" if A.rgb else "RGBA"
    dst = Path(A.dst)
    dst.mkdir(parents=True, exist_ok=True)

    total_bytes = 0
    for f in files:
        im = Image.open(f).convert(mode)
        if size:
            im = im.resize(size, Image.LANCZOS)
        out = dst / f"{f.stem}.webp"
        im.save(out, "WEBP", quality=88, alpha_quality=90, method=6)
        saved = Image.open(out)
        n = os.path.getsize(out)
        total_bytes += n
        print(f"{f.stem} {saved.width}x{saved.height} {saved.mode} {n} B")
    print(f"{len(files)} files, {total_bytes} B")
    return 0


if __name__ == "__main__":
    sys.exit(main())
