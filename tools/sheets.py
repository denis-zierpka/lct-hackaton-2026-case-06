"""Лист скриншотов с подписями (живая проверка, TOWN-S1d): 5 кадров в ряд шириной 360, высота — по пропорции первого кадра.
Запуск из корня: python tools/sheets.py OUT.jpg "Заголовок" emu_room.png "Подпись" emu_shop.png "Подпись" ...
Кадры — из finny-pet/screenshots/ (tools/adbui.sh shot NAME кладёт туда emu_NAME.png), OUT — туда же.
Шрифт — Arial (Windows) или DejaVu (Linux); символа ✉ в них нет — в подписях писать «конверт»."""
import os, sys
from PIL import Image, ImageDraw, ImageFont, ImageOps

W, COLS, CAP = 360, 5, 64
SHOTS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "finny-pet", "screenshots")


def font(size, bold=False):
    for f in (f"C:/Windows/Fonts/arial{'bd' if bold else ''}.ttf",
              f"/usr/share/fonts/truetype/dejavu/DejaVuSans{'-Bold' if bold else ''}.ttf"):
        if os.path.exists(f):
            return ImageFont.truetype(f, size)
    return ImageFont.load_default()


def flatten(im):
    """Transparent frame (RGBA/LA, or P with a transparency entry) goes onto white; RGB stays as is."""
    if im.mode in ("RGBA", "LA") or (im.mode == "P" and "transparency" in im.info):
        im = im.convert("RGBA")
        bg = Image.new("RGB", im.size, "white")
        bg.paste(im, mask=im.split()[-1])
        return bg
    return im.convert("RGB")


def fit(im, w, h):
    """Frame fitted into a w x h cell without stretching (ImageOps.contain), centred on white."""
    im = ImageOps.contain(im, (w, h), Image.LANCZOS)
    cell = Image.new("RGB", (w, h), "white")
    cell.paste(im, ((w - im.width) // 2, (h - im.height) // 2))
    return cell


out, title, pairs = sys.argv[1], sys.argv[2], sys.argv[3:]
shots = list(zip(pairs[::2], pairs[1::2]))
w0, h0 = Image.open(os.path.join(SHOTS, shots[0][0])).size
H = round(W * h0 / w0)
rows = (len(shots) + COLS - 1) // COLS
sheet = Image.new("RGB", (COLS * (W + 16) + 16, 60 + rows * (H + CAP + 16)), "white")
d = ImageDraw.Draw(sheet)
d.text((16, 16), title, fill="black", font=font(26, True))
for i, (name, cap) in enumerate(shots):
    x, y = 16 + (i % COLS) * (W + 16), 60 + (i // COLS) * (H + CAP + 16)
    sheet.paste(fit(flatten(Image.open(os.path.join(SHOTS, name))), W, H), (x, y))
    d.rectangle((x, y, x + W - 1, y + H - 1), outline="#999")
    d.text((x, y + H + 8), cap, fill="black", font=font(17))
path = os.path.join(SHOTS, out)
os.makedirs(os.path.dirname(path), exist_ok=True)
sheet.save(path, quality=85)
print(path, sheet.size)
