"""Макеты экрана работы пекарни «в сцене» и направлений мини-игры (TOWN-A1c ворота № 40, TOWN-J1; лист
finny-pet/screenshots/town/bakery_mock_1.jpg). Кадр 1080 × 1920 = 360 × 640 dp, 3 px/dp. Фон — bg_bakery_port из res;
HUD — полоса живого снимка SNAP (тот же фон под плашками); выпечка и ингредиенты — эмодзи-заглушки Segoe UI Emoji (в игре —
спрайты Blender); жители — рендеры pet.py --residents в RES. Эскиз для решений владельца, не ассет.

  python tools/mock_bakery.py RES_DIR OUT_DIR [SNAP.png]            — направления (ворота № 40, 41)
  python tools/mock_bakery.py RES_DIR OUT_DIR SNAP.png decisions     — решения № 42–44 концепции TOWN-J1 (d42_*, d43_*, d44_*)
  (RES_DIR: Blender -b --factory-startup --python-exit-code 1 -P finny-pet/tools/art/pet.py -- --residents RES_DIR
   --only-resident borya | marta | kesha; SNAP по умолчанию — finny-pet/screenshots/emu_a_job10.png, снимок пекарни 360 × 640)"""
import os, sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES, HERE = sys.argv[1], sys.argv[2]
os.makedirs(HERE, exist_ok=True)
BG = os.path.join(ROOT, "finny-pet/app/src/game/res/drawable-nodpi/bg_bakery_port.webp")
DECISIONS = "decisions" in sys.argv[4:]
SNAP = sys.argv[3] if len(sys.argv) > 3 else os.path.join(ROOT, "finny-pet/screenshots/emu_a_job10.png")
COIN = os.path.join(ROOT, "finny-pet/app/src/game/res/drawable-nodpi/ui_coin.webp")
MONT = os.path.join(ROOT, "finny-pet/app/src/main/res/font/montserrat.ttf")
MONTB = os.path.join(ROOT, "finny-pet/tools/art/montserrat_extrabold.ttf")
EMO = "C:/Windows/Fonts/seguiemj.ttf"
PURPLE, PURPLE_D, INK, GREEN, GOLD, PAPER = (82, 9, 120), (49, 15, 83), (28, 29, 34), (46, 184, 106), (255, 201, 77), (255, 255, 255)


def font(size, bold=False):
    f = ImageFont.truetype(MONTB if bold else MONT, size)
    if not bold:
        try: f.set_variation_by_axes([600])
        except Exception: pass
    return f


def res(name, h):
    im = Image.open(os.path.join(RES, f"res_{name}.png")).convert("RGBA")
    im = im.crop(im.getbbox())
    return im.resize((round(im.width * h / im.height), h), Image.LANCZOS)


def base():
    im = Image.open(BG).convert("RGBA")
    hud = Image.open(SNAP).convert("RGBA").crop((0, 0, 1080, 412))
    im.alpha_composite(hud, (0, 0))
    return im


def shadow_box(im, box, r, fill, outline=None, blur=14, off=6, alpha=70):
    x0, y0, x1, y1 = box
    sh = Image.new("RGBA", im.size, (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle((x0, y0 + off, x1, y1 + off), r, fill=(40, 20, 60, alpha))
    im.alpha_composite(sh.filter(ImageFilter.GaussianBlur(blur)))
    ImageDraw.Draw(im).rounded_rectangle(box, r, fill=fill, outline=outline, width=4 if outline else 0)


def bubble(im, box, tail, r=44):
    """White speech bubble with a triangle tail to point (tx, ty) from the bubble's nearest side."""
    shadow_box(im, box, r, PAPER)
    x0, y0, x1, y1 = box
    tx, ty = tail
    d = ImageDraw.Draw(im)
    if tx < x0:
        cy = min(max(ty, y0 + 60), y1 - 60); d.polygon([(x0 + 2, cy - 34), (x0 + 2, cy + 34), (tx, ty)], fill=PAPER)
    else:
        cx = min(max(tx, x0 + 60), x1 - 60); d.polygon([(cx - 34, y1 - 2), (cx + 34, y1 - 2), (tx, ty)], fill=PAPER)


def button(im, box, text, color=PURPLE, fg=PAPER, size=54):
    x0, y0, x1, y1 = box
    ImageDraw.Draw(im).rounded_rectangle((x0, y0 + 10, x1, y1 + 10), (y1 - y0) // 2, fill=tuple(max(0, c - 40) for c in color))
    ImageDraw.Draw(im).rounded_rectangle(box, (y1 - y0) // 2, fill=color)
    f = font(size, True); w = ImageDraw.Draw(im).textlength(text, font=f)
    ImageDraw.Draw(im).text(((x0 + x1 - w) / 2, (y0 + y1) / 2), text, font=f, fill=fg, anchor="lm")


def emoji(im, ch, xy, size):
    f = ImageFont.truetype(EMO, 109)
    tmp = Image.new("RGBA", (140, 140), (0, 0, 0, 0))
    ImageDraw.Draw(tmp).text((4, 4), ch, font=f, embedded_color=True)
    tmp = tmp.crop(tmp.getbbox()); k = size / max(tmp.size)
    tmp = tmp.resize((round(tmp.width * k), round(tmp.height * k)), Image.LANCZOS)
    im.alpha_composite(tmp, (round(xy[0] - tmp.width / 2), round(xy[1] - tmp.height / 2)))


def coin(im, xy, size):
    c = Image.open(COIN).convert("RGBA").resize((size, size), Image.LANCZOS)
    im.alpha_composite(c, (round(xy[0]), round(xy[1])))


def counter(im, y0, y1, x0=30, x1=1050, goods=()):
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((x0, y0 + 40, x1, y1), 26, fill=(244, 162, 97))            # front #F4A261 (bakery counter)
    d.rectangle((x0, y1 - 26, x1, y1), fill=(214, 128, 70))
    for x in range(x0 + 120, x1, 240): d.line((x, y0 + 70, x, y1 - 40), fill=(226, 146, 84), width=5)
    d.rounded_rectangle((x0 - 20, y0, x1 + 20, y0 + 56), 20, fill=(217, 163, 108))  # top plank (floor tone)
    d.rectangle((x0 - 20, y0 + 40, x1 + 20, y0 + 56), fill=(196, 140, 88))
    for ch, x in goods: emoji(im, ch, (x, y0 - 38), 110)


def text(im, xy, s, size=50, bold=False, fill=INK, anchor="la"):
    ImageDraw.Draw(im).text(xy, s, font=font(size, bold), fill=fill, anchor=anchor)


def tokens(im, xy, n=0, of=3, size=34):
    d = ImageDraw.Draw(im); x, y = xy
    for i in range(of):
        box = (x + i * (size + 14), y, x + i * (size + 14) + size, y + size)
        d.ellipse(box, fill=PURPLE if i < n else None, outline=PURPLE, width=5)


def save(im, name):
    p = os.path.join(HERE, name); im.convert("RGB").save(p, quality=92); print(p)


def pointer(im, xy, size=110):
    emoji(im, "☝️", xy, size)


def tray_scene(order, marks, on_tray, tiles, stars=0, point=None, note=None, riddle_chip=False, customer="marta"):
    """Раунд «Поднос» по синтезу J1: облачко-заказ сверху, покупатель слева, Боря справа, поднос на прилавке, витрина,
    ряд смены с кнопками 50 dp. marks[i]: None / "ok" (✓) / "need" (рамка и «?») у картинки заказа i."""
    im = base()
    b = res("borya", 430); im.alpha_composite(b, (760, 1010 - b.height + 30))
    counter(im, 930, 1110)
    slots = len(order); w = 150; x0 = 540 - slots * w // 2
    d = ImageDraw.Draw(im)
    if slots: d.rounded_rectangle((x0 - 30, 850, x0 + slots * w + 30, 950), 40, fill=(250, 244, 232), outline=(210, 190, 160), width=5)
    for k in range(slots):
        cx = x0 + k * w + w // 2
        if k < len(on_tray): emoji(im, on_tray[k], (cx, 890), 100)
        else: d.ellipse((cx - 45, 845, cx + 45, 935), outline=(190, 170, 150), width=4)
    if customer:
        m = res(customer, 560); im.alpha_composite(m, (20, 1110 - m.height + 120))
    bw = 170 * len(order) + 80
    if order: bubble(im, (40, 440, 40 + max(bw, 300), 660), (200, 760))
    for k, ch in enumerate(order):
        x = 120 + k * 170
        emoji(im, ch, (x, 550), 120)
        if marks[k] == "ok":
            d = ImageDraw.Draw(im); d.ellipse((x + 20, 580, x + 74, 634), fill=GREEN)
            d.line([(x + 33, 608), (x + 44, 620), (x + 62, 594)], fill=PAPER, width=7, joint="curve")  # ✓ is missing in Montserrat
        elif marks[k] == "need":
            d = ImageDraw.Draw(im); d.rounded_rectangle((x - 72, 478, x + 72, 622), 24, outline=PURPLE, width=6)
            d.ellipse((x + 22, 580, x + 76, 634), fill=PURPLE); text(im, (x + 49, 607), "?", 38, True, PAPER, "mm")
    if note: text(im, (60, 690), note, 44, True, PURPLE)
    n = len(tiles); cols = 3
    for k, ch in enumerate(tiles):
        tx = 50 + (k % cols) * 332; ty = 1150 + (k // cols) * 250
        shadow_box(im, (tx, ty, tx + 300, ty + 220), 36, PAPER); emoji(im, ch, (tx + 150, ty + 110), 150)
        if point == k: pointer(im, (tx + 250, ty + 190), 110)
    ImageDraw.Draw(im).rounded_rectangle((20, 1672, 1060, 1868), 48, fill=(244, 242, 248))
    for k in range(4):
        emoji(im, "⭐" if k < stars else "⚪", (80 + k * 92, 1770), 56)
        if k < stars: text(im, (80 + k * 92, 1832), "+1", 34, True, PURPLE, "mm")
    button(im, (440, 1695, 1044, 1845), "Отдать", GREEN, size=56)
    button(im, (800, 420, 1050, 540), "Закончить", (244, 242, 248), PURPLE, 38)   # corner of the scene, not next to «Отдать»
    if riddle_chip:
        d = ImageDraw.Draw(im); d.ellipse((960, 560, 1060, 660), fill=PURPLE); text(im, (1010, 612), "?", 64, True, PAPER, "mm")
    return im


def order_scene(line1, line2):
    im = base()
    b = res("borya", 700); im.alpha_composite(b, (70, 1370 - b.height + 40))
    counter(im, 1250, 1480, goods=[("🥐", 700), ("🍞", 850), ("🥖", 980)])
    bubble(im, (470, 560, 1040, 1010), (330, 820))
    text(im, (520, 610), line1, 52, True); text(im, (520, 676), line2, 52, True)
    text(im, (520, 780), "6–10", 84, True, PURPLE); coin(im, (745, 782), 78)
    text(im, (520, 900), "Смены:", 44); tokens(im, (700, 905), 0)
    button(im, (60, 1560, 1020, 1704), "Начать смену")
    return im


if DECISIONS:
    # № 42 — текст облачка заказа
    for tag, a, b in (("a", "Помоги испечь", "булочки!"), ("b", "Помоги продать", "булочки!"), ("c", "Помоги Боре", "в пекарне!")):
        save(order_scene(a, b), f"d42_{tag}.jpg")
    # № 43 — галочки по ходу сборки: а) нет (✓ и «?» только после «Отдать»), б) да, как на макете; и отдача с расхождением
    save(tray_scene(["🥐", "🥐", "🍞"], [None, None, None], ["🥐"], ["🥐", "🍞", "🥖", "🥨", "🍩", "🧁"]), "d43_a_building.jpg")
    save(tray_scene(["🥐", "🥐", "🍞"], ["ok", None, None], ["🥐"], ["🥐", "🍞", "🥖", "🥨", "🍩", "🧁"]), "d43_b_building.jpg")
    save(tray_scene(["🥐", "🥐", "🍞"], ["ok", "need", "ok"], ["🥐", "🍞"], ["🥐", "🍞", "🥖", "🥨", "🍩", "🧁"]),
         "d43_after_give.jpg")
    # № 44 — первая смена: а) обучение — 3 изделия, заказ из одного, указатель; б) сразу 6 изделий и заказ 2–4
    save(tray_scene(["🥐"], [None], [], ["🥐", "🍞", "🥖"], point=0), "d44_a_first.jpg")
    save(tray_scene(["🥐", "🥐", "🍞"], [None, None, None], [], ["🥐", "🍞", "🥖", "🥨", "🍩", "🧁"]), "d44_b_first.jpg")
    # № 48 — загадка: а) облачко Бори поверх витрины, у прилавка никого; б) значок «?» у Бори, витрина работает
    im = tray_scene([], [], [], ["🥐", "🍞", "🥖", "🥨", "🍩", "🧁"], stars=1, customer=None)
    bubble(im, (80, 1120, 1000, 1640), (900, 1000))
    text(im, (130, 1160), "Загадка!", 54, True, PURPLE); text(im, (130, 1240), "Отгадаешь — Боря положит", 44)
    text(im, (130, 1296), "одну булочку на поднос", 44)
    button(im, (130, 1400, 530, 1550), "Отгадать", GREEN, size=48); button(im, (560, 1400, 960, 1550), "Не сейчас", (244, 242, 248), PURPLE, 44)
    save(im, "d48_a_bubble.jpg")
    save(tray_scene(["🥐", "🍞"], [None, None], [], ["🥐", "🍞", "🥖", "🥨", "🍩", "🧁"], stars=1, riddle_chip=True, customer="osya"), "d48_b_chip.jpg")
    sys.exit(0)

# 1. Экран заказа в сцене: Боря на полу за прилавком, облачко, «6–10», жетоны, «Начать смену»
im = base()
b = res("borya", 700); im.alpha_composite(b, (70, 1370 - b.height + 40))
counter(im, 1250, 1480, goods=[("🥐", 700), ("🍞", 850), ("🥖", 980)])
bubble(im, (470, 560, 1040, 1010), (330, 820))
text(im, (520, 610), "Помоги испечь", 52, True); text(im, (520, 676), "булочки!", 52, True)
text(im, (520, 780), "6–10", 84, True, PURPLE); coin(im, (745, 782), 78)
text(im, (520, 900), "Смены:", 44); tokens(im, (700, 905), 0)
button(im, (60, 1560, 1020, 1704), "Начать смену")
save(im, "m1_order_scene.jpg")

# 2. Направление А — «Поднос по заказу»: покупатель у прилавка показывает облачко-заказ, ребёнок собирает поднос с витрины
im = base()
b = res("borya", 470); im.alpha_composite(b, (700, 1010 - b.height + 30))
counter(im, 930, 1110)
tray = (360, 868, 740, 940); ImageDraw.Draw(im).ellipse(tray, fill=(250, 244, 232), outline=(210, 190, 160), width=5)
emoji(im, "🥐", (470, 880), 96)
m = res("marta", 560); im.alpha_composite(m, (20, 1110 - m.height + 120))
bubble(im, (40, 470, 560, 690), (200, 780))
for ch, x, done in (("🥐", 140, True), ("🥐", 300, False), ("🍞", 460, False)):
    emoji(im, ch, (x, 580), 120)
    if done: ImageDraw.Draw(im).ellipse((x + 20, 610, x + 70, 660), fill=GREEN); text(im, (x + 45, 636), "✓", 36, True, PAPER, "mm")
tiles = ["🥐", "🥖", "🍞", "🧁", "🥨", "🥯"]
for i, ch in enumerate(tiles):
    x0 = 50 + (i % 3) * 332; y0 = 1150 + (i // 3) * 250
    shadow_box(im, (x0, y0, x0 + 300, y0 + 220), 36, PAPER); emoji(im, ch, (x0 + 150, y0 + 110), 150)
ImageDraw.Draw(im).rounded_rectangle((20, 1680, 1060, 1860), 48, fill=(244, 242, 248))
text(im, (60, 1712), "Покупатели", 40);
for i in range(4):
    emoji(im, "⭐" if i < 1 else "⚪", (80 + i * 70, 1805), 52)
button(im, (400, 1718, 690, 1832), "Отдать", GREEN, size=48); button(im, (720, 1718, 1040, 1832), "Закончить", size=46)
save(im, "m2_A_tray.jpg")

# 3. Направление Б — «Печём по рецепту»: Боря показывает рецепт, ребёнок кладёт ингредиенты в миску и ставит в печь
im = base()
oven = (720, 640, 1030, 1000); d = ImageDraw.Draw(im)
d.rounded_rectangle(oven, 40, fill=(176, 92, 52)); d.rounded_rectangle((760, 740, 990, 930), 26, fill=(70, 34, 24))
d.rounded_rectangle((780, 760, 970, 910), 20, fill=(255, 160, 60)); d.rectangle((720, 620, 1030, 660), fill=(150, 76, 44))
b = res("borya", 600); im.alpha_composite(b, (40, 1080 - b.height + 40))
counter(im, 980, 1160)
bowl = (400, 860, 700, 1000); d = ImageDraw.Draw(im); d.pieslice((400, 780, 700, 1040), 0, 180, fill=(120, 170, 220)); d.rectangle((400, 900, 700, 912), fill=(100, 150, 200))
emoji(im, "🌾", (520, 880), 80); emoji(im, "🥚", (600, 885), 70)
bubble(im, (380, 440, 1040, 600), (300, 700))
for ch, n, x in (("🌾", "×2", 470), ("🥚", "×1", 680), ("🧈", "×1", 890)):
    emoji(im, ch, (x, 520), 100); text(im, (x + 58, 520), n, 50, True, INK, "lm")
for i, ch in enumerate(["🌾", "🥚", "🧈", "🥛", "🍫", "🍓"]):
    x0 = 50 + (i % 3) * 332; y0 = 1200 + (i // 3) * 230
    shadow_box(im, (x0, y0, x0 + 300, y0 + 200), 36, PAPER); emoji(im, ch, (x0 + 150, y0 + 100), 140)
ImageDraw.Draw(im).rounded_rectangle((20, 1680, 1060, 1860), 48, fill=(244, 242, 248))
text(im, (60, 1712), "Партия 1 из 3", 40)
for j, (ch, t) in enumerate((("🌾", "1/2"), ("🥚", "1/1"), ("🧈", "0/1"))): emoji(im, ch, (85 + j * 160, 1805), 50); text(im, (118 + j * 160, 1805), t, 38, False, INK, "lm")
button(im, (560, 1718, 780, 1832), "В печь", GREEN, size=46); button(im, (800, 1718, 1040, 1832), "Закончить", size=40)
save(im, "m3_B_recipe.jpg")

# 4. Загадка внутри игры (А): между покупателями Боря спрашивает; верно — «подсказка Бори» на следующий заказ
im = base()
b = res("borya", 700); im.alpha_composite(b, (60, 1370 - b.height + 40))
counter(im, 1250, 1480, goods=[("🥐", 760), ("🍞", 930)])
bubble(im, (440, 520, 1050, 1150), (330, 800))
text(im, (490, 565), "Загадка!", 54, True, PURPLE)
text(im, (490, 650), "Отгадаешь —", 46); text(im, (490, 710), "подсказка на", 46); text(im, (490, 770), "следующий заказ", 46)
button(im, (490, 870, 1000, 990), "Отгадать", GREEN, size=48); button(im, (490, 1010, 1000, 1110), "Не сейчас", (244, 242, 248), PURPLE, 44)
ImageDraw.Draw(im).rounded_rectangle((20, 1680, 1060, 1860), 48, fill=(244, 242, 248))
text(im, (60, 1712), "Покупатели", 40)
for i in range(4): emoji(im, "⭐" if i < 2 else "⚪", (80 + i * 70, 1805), 52)
button(im, (720, 1718, 1040, 1832), "Закончить", size=46)
save(im, "m4_riddle_in_game.jpg")

# 5. Итог смены в сцене: Боря благодарит, звёзды покупателей, «✉ +9», «Готово»
im = base()
b = res("borya", 700); im.alpha_composite(b, (70, 1370 - b.height + 40))
counter(im, 1250, 1480, goods=[("🥐", 700), ("🍞", 850), ("🥖", 980)])
bubble(im, (470, 560, 1040, 1010), (330, 820))
text(im, (520, 606), "Спасибо!", 56, True)
for i in range(4): emoji(im, "⭐" if i < 3 else "⚪", (550 + i * 90, 730), 70)
emoji(im, "✉️", (565, 845), 80); text(im, (625, 845), "+9", 80, True, PURPLE, "lm"); text(im, (520, 920), "придёт с конвертом", 40)
button(im, (60, 1560, 1020, 1704), "Готово")
save(im, "m5_result_scene.jpg")
