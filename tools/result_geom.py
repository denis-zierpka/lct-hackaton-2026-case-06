"""Зонд геометрии итога смены (TOWN-J1-1b1-2 «ДО СПАВНА» п. 1; правила питомца — TOWN-J1-1b1-3) по сырому дампу uiautomator.

  python tools/result_geom.py DUMP.xml DENSITY [--big] [--counter] [--line]  -> GEOM OK | GEOM FAIL: <правило>[; …]; exit 0 | 1
  python tools/result_geom.py --selfcheck   -> каждое правило краснеет на своей синтетике, верные — OK; exit 0 | 1

DENSITY — px на dp: вызывающий скрипт берёт `tools/adbui.sh shell wm density` (последнее число / 160; эмулятор после
wm360 — 3). Узлы по resource-id (testTagsAsResourceId): job_result, result_pet, result_resident, result_tail,
result_counter, line, hud2; кнопка «Почему?»/«Готово» — самый маленький узел clickable="true", чьи bounds содержат центр
узла с таким text. bounds — непокрытая часть узла (режут узлы, важные для TalkBack и нарисованные позже; узел с одним
testTag не режет никого; перекрытый целиком — выпадает из дампа).
Правила — без --line: пол = верх кнопок − 3 dp, потолок облачка справа = пол − (низ hud2 + 8 dp) − 120 dp; питомец справа
от жителя — только если облачко ≤ потолка − 3 px, и тогда под облачком; перед жителем — только если облачко ≥ потолка +
3 px (сторона по месту, № 83 в; полоса ± 3 px — красный при любой стороне); верх питомца = пол − 82 dp ± 2 px (лапы на
полу, № 87 б), низ = верх кнопок ± 2 px (кадр уходит под ряд кнопок, режут только они); кадр жителя ≤ 232 dp, с --big
(шрифт 1,3) ≤ 216 dp; хвост 16 × 22 dp на левом краю облачка, центр — на уровне
морды жителя (K = 0,37 кадра, зажат в облачко на 31 dp; облачко < 62 dp — его центр), выше кнопок; узлы result_* без
текста, описания и действий; --counter — прилавок 56 dp у пола под жителем почти во всю ширину, без флага прилавка нет.
С --line (после «Почему?», только эти правила): line есть, обе кнопки в дампе и не подрезаны (≥ 61 dp), низ line ≤ верх
кнопок. Сообщения — ASCII (консоль Windows).
"""
import re
import sys

K = 0.37  # resident face level, fraction of the frame from its top — spec «ДО СПАВНА» п. 0 (k)
RESULT = ("result_pet", "result_resident", "result_tail", "result_counter")


def parse(xml):
    out = []
    for n in re.findall(r"<node [^>]*>", xml):
        g = lambda k: (re.search(" " + k + r'="([^"]*)"', n) or [None, ""])[1]
        b = list(map(int, re.findall(r"-?\d+", g("bounds"))))
        if len(b) == 4:
            out.append(dict(rid=g("resource-id").split("/")[-1], text=g("text"), desc=g("content-desc"),
                            click=g("clickable") == "true", focus=g("focusable") == "true", b=b))
    return out


def check(xml, d, big=False, counter=False, line=False):
    ns = parse(xml)
    tag = {n["rid"]: n for n in reversed(ns) if n["rid"]}  # first node wins
    area = lambda b: (b[2] - b[0]) * (b[3] - b[1])
    inside = lambda b, x, y: b[0] <= x < b[2] and b[1] <= y < b[3]

    def button(text):  # the clickable node around the text «Почему?»/«Готово»
        t = next((n["b"] for n in ns if n["text"] == text), None)
        if t is None:
            return None
        cx, cy = (t[0] + t[2]) / 2, (t[1] + t[3]) / 2
        cs = [n["b"] for n in ns if n["click"] and inside(n["b"], cx, cy)]
        return min(cs, key=area) if cs else None

    btns = [b for b in (button("Почему?"), button("Готово")) if b]
    fail = []
    if line:
        if "line" not in tag:
            return ["no line"]
        if button("Почему?") is None or button("Готово") is None:
            return ["buttons hidden by line"]
        if any(b[3] - b[1] < 61 * d - 2 for b in btns):
            fail.append("buttons cut by line")
        if tag["line"]["b"][3] > min(b[1] for b in btns):
            fail.append("line over buttons")
        return fail

    miss = [k for k in ("job_result", "result_pet", "result_resident", "result_tail", "hud2") if k not in tag]
    if button("Готово") is None:
        miss.append("button Gotovo")
    if miss:
        return ["no nodes: " + ", ".join(miss)]
    bub, pet, res, tail = (tag[k]["b"] for k in ("job_result", "result_pet", "result_resident", "result_tail"))
    btop = min(b[1] for b in btns)
    cx = lambda b: (b[0] + b[2]) / 2
    h = lambda b: b[3] - b[1]
    floor = btop - 3 * d  # the button row starts 3 dp below the floor (RoundScreen: floor = maxHeight − 64 dp, row 61 dp)
    cap = floor - (tag["hud2"]["b"][3] + 8 * d) - 120 * d  # side ceiling of the bubble: floor − 16 − 104 dp from the scene top
    if cx(pet) > cx(res):  # right, under the bubble — only when the bubble fits there (№ 83 в)
        if h(bub) > cap - 3:
            fail.append("pet right but bubble at side ceiling")
        if pet[1] < bub[3]:
            fail.append("pet over bubble")
    else:  # in front of the resident — only when the bubble does not fit on the right
        if h(bub) < cap + 3:
            fail.append("pet in front though bubble fits")
    if abs(pet[1] - (floor - 82 * d)) > 2:  # № 87 б: frame 14 dp lower — paws (0,86 of 96 dp) on the floor
        fail.append("pet not on floor")
    if abs(pet[3] - btop) > 2:  # the frame goes under the button row; only the buttons cut it
        fail.append("pet bottom not at buttons")
    if h(res) > (216 if big else 232) * d + 2:
        fail.append(f"resident frame > {216 if big else 232} dp")
    if abs(tail[2] - tail[0] - 16 * d) > 2 or abs(h(tail) - 22 * d) > 2:
        fail.append("tail size")
    if not bub[0] <= tail[2] <= bub[0] + 3 * d:
        fail.append("tail not on bubble left edge")
    if h(bub) < 62 * d:
        hy = (bub[1] + bub[3]) / 2
    else:
        hy = min(max(res[1] + K * h(res), bub[1] + 31 * d), bub[3] - 31 * d)
    if abs((tail[1] + tail[3]) / 2 - hy) > 3:
        fail.append("tail not at resident face")
    if tail[3] >= btop:
        fail.append("tail touches buttons")
    if any(n["text"] or n["desc"] or n["click"] or n["focus"] for k in RESULT if k in tag for n in [tag[k]]):
        fail.append("result nodes not empty")
    if counter:
        if "result_counter" not in tag:
            return fail + ["no counter"]
        c = tag["result_counter"]["b"]
        root = max((n["b"] for n in ns), key=area)
        if abs(c[3] - res[3]) > 3:
            fail.append("counter not at resident bottom")
        if abs(c[3] - (btop - 3 * d)) > 2:
            fail.append("counter not at floor")
        if abs(h(c) - 56 * d) > 2:
            fail.append("counter height")
        if c[1] <= res[1]:
            fail.append("counter top not below resident top")
        if c[2] - c[0] < 0.9 * (root[2] - root[0]):
            fail.append("counter width")
    elif "result_counter" in tag:
        fail.append("counter on non-tray job")
    return fail


# ---- self-check: a synthetic 360 × 640 dp scene at d = 3 (1080 × 1920 px) after the real 1b1 dump emu_s12b_levelup10:
# content 24…1056 px, floor 1632 (bubble bottom 1608 = floor − 8 dp), button row top 1641 = floor + 3 dp.
# 1,0: frame 232 dp — resident [0,936][535,1632] (off-screen part clipped), bubble left 361 (8 + 0,45 f), its bottom
# floor − 112 dp = 1296; pet 96 dp at maxWidth − 104 dp; hy = 936 + 0,37 · 696 = 1193,5 → tail [319,1160][367,1226].
# 1,3: frame 216 dp — resident [0,984][502,1632], bubble left 340, bottom 1608; pet at 8 dp; hy = 1223,8 → tail [298,1191][346,1257].
# TOWN-J1-1b1-3: pet frame top floor − 82 dp = 1386, its bottom (floor + 14 dp) cut by the buttons at 1641; hud2 [0,270][1080,414];
# zero at 1,3 fits on the right (273 dp ≤ 278): bubble [340,477][1056,1296], hy clamped to 1296 − 93 = 1203 → tail [298,1170][346,1236].
E = 'text="" content-desc="" clickable="false" focusable="false"'


def synth(big=False, counter=True, line=None, cut=0, buttons=True, pet_attrs=E, fits=False, **over):
    """over: resource-id=bounds (None — no node); cut — px the button row lost at its top; buttons=False — no buttons.
    big — levelup at 1,3 (bubble 336 dp > side ceiling 278 dp: pet in front); big + fits — zero at 1,3 (273 dp: pet right).
    hud2 bottom 414 + 8 dp → scene top 438; side ceiling 1632 − 438 − 360 = 834 px; pet frame floor − 82 dp … under buttons."""
    b = dict(root=(0, 0, 1080, 1920), hud2=(0, 270, 1080, 414))
    if big and fits:
        b.update(job_result=(340, 477, 1056, 1296), result_resident=(0, 984, 502, 1632),
                 result_pet=(744, 1386, 1032, 1641), result_tail=(298, 1170, 346, 1236))
    elif big:
        b.update(job_result=(340, 599, 1056, 1608), result_resident=(0, 984, 502, 1632),
                 result_pet=(48, 1386, 336, 1641), result_tail=(298, 1191, 346, 1257))
    else:
        b.update(job_result=(361, 500, 1056, 1296), result_resident=(0, 936, 535, 1632),
                 result_pet=(744, 1386, 1032, 1641), result_tail=(319, 1160, 367, 1226))
    if counter:
        b["result_counter"] = (24, 1464, 1056, 1632)  # floor − 56 dp … floor; the bubble at 1,3 leaves its bbox whole
    b["line"] = line
    b.update(over)
    attrs = dict(line='text="" content-desc="" clickable="true" focusable="true"', result_pet=pet_attrs)
    node = lambda a, rid, x: f'<node index="0" {a} resource-id="{rid}" bounds="[{x[0]},{x[1]}][{x[2]},{x[3]}]" />'
    s = [node(attrs.get(k, E), "" if k == "root" else k, x) for k, x in b.items() if x is not None]
    if buttons:  # a button is a pair «clickable + text», as in the real dump
        for t, x0, x1, tx1 in (("Почему?", 24, 528, 314), ("Готово", 552, 1056, 780)):
            y0 = 1641 + cut
            s.append(node('text="" content-desc="" clickable="true" focusable="true"', "", (x0, y0, x1, 1824)))
            s.append(node(f'text="{t}" content-desc="" clickable="false" focusable="false"', "", (x0 + 42, max(1694, y0), tx1, 1756)))
    return "<hierarchy>" + "".join(s) + "</hierarchy>"


def selfcheck():
    tail_at = lambda y: (319, y - 33, 367, y + 33)  # 1,0 tail centred at y
    L = dict(line=True)
    cases = [  # name, dump, flags, expected failures
        ("верный 1,0, пекарня", synth(), dict(counter=True), set()),
        ("верный 1,0, Марта (без прилавка)", synth(counter=False), {}, set()),
        ("верный 1,3, облачко выше потолка справа — перед жителем", synth(big=True), dict(big=True, counter=True), set()),
        ("верный 1,3, облачко помещается — справа", synth(big=True, fits=True), dict(big=True, counter=True), set()),
        ("облачко 50 dp, хвост по его центру", synth(job_result=(361, 1146, 1056, 1296), result_tail=tail_at(1221)), dict(counter=True), set()),
        ("строка над кнопками", synth(line=(24, 1210, 1056, 1614)), L, set()),
        ("облачко 50 dp, хвост у морды, не по центру", synth(job_result=(361, 1146, 1056, 1296)), dict(counter=True), {"tail not at resident face"}),
        ("1b1-2 при 1,3: перед жителем, хотя облачко помещается", synth(big=True, job_result=(340, 789, 1056, 1608)), dict(big=True, counter=True),
         {"pet in front though bubble fits"}),
        ("1,3: перед жителем, облачко = потолок + 2 px", synth(big=True, job_result=(340, 772, 1056, 1608)), dict(big=True, counter=True),
         {"pet in front though bubble fits"}),
        ("1,3: справа, облачко = потолок − 2 px", synth(big=True, fits=True, job_result=(340, 464, 1056, 1296)), dict(big=True, counter=True),
         {"pet right but bubble at side ceiling"}),
        ("всегда справа: облачко у потолка справа", synth(big=True, fits=True, job_result=(340, 462, 1056, 1296)), dict(big=True, counter=True),
         {"pet right but bubble at side ceiling"}),
        ("справа при облачке перед жителем (подрезан им)", synth(big=True, result_pet=(744, 1608, 1032, 1641)), dict(big=True, counter=True),
         {"pet right but bubble at side ceiling", "pet not on floor"}),
        ("перед жителем при 1,0, облачко помещается", synth(result_pet=(48, 1386, 336, 1641)), dict(counter=True), {"pet in front though bubble fits"}),
        ("питомец не опущен (1b1-2)", synth(result_pet=(744, 1344, 1032, 1632)), dict(counter=True), {"pet not on floor", "pet bottom not at buttons"}),
        ("питомец опущен на 28 dp", synth(result_pet=(744, 1428, 1032, 1641)), dict(counter=True), {"pet not on floor"}),
        ("кадр питомца кончается на полу", synth(result_pet=(744, 1386, 1032, 1632)), dict(counter=True), {"pet bottom not at buttons"}),
        ("питомец выше пола на 1 dp", synth(result_pet=(744, 1383, 1032, 1641)), dict(counter=True), {"pet not on floor"}),
        ("низ кадра на 1 dp выше кнопок", synth(result_pet=(744, 1386, 1032, 1638)), dict(counter=True), {"pet bottom not at buttons"}),
        ("питомец на облачке (подрезан)", synth(result_pet=(744, 1296, 1032, 1488)), dict(counter=True), {"pet not on floor", "pet bottom not at buttons"}),
        # облачко (позже, важно для TalkBack) режет питомца по ширине не целиком — bbox непокрытой части = весь кадр
        ("питомец на полу из-под угла облачка, облачко ниже его верха", synth(job_result=(361, 800, 1056, 1400), result_pet=(200, 1386, 488, 1641)),
         dict(counter=True), {"pet over bubble"}),
        ("кадр 240 dp при 1,0", synth(result_resident=(0, 912, 535, 1632), result_tail=tail_at(1178)), dict(counter=True), {"resident frame > 232 dp"}),
        ("кадр 232 при --big", synth(big=True, job_result=(361, 599, 1056, 1608), result_resident=(0, 936, 535, 1632), result_tail=(319, 1160, 367, 1226)),
         dict(big=True, counter=True), {"resident frame > 216 dp"}),
        ("нет шапки hud2", synth(hud2=None), dict(counter=True), {"no nodes: hud2"}),
        ("нет хвоста", synth(result_tail=None), dict(counter=True), {"no nodes: result_tail"}),
        ("хвост 86 px в высоту", synth(result_tail=(319, 1150, 367, 1236)), dict(counter=True), {"tail size"}),
        ("хвост справа", synth(result_tail=(1008, 1160, 1056, 1226)), dict(counter=True), {"tail not on bubble left edge"}),
        ("хвост у низа облачка", synth(result_tail=tail_at(1263)), dict(counter=True), {"tail not at resident face"}),
        ("хвост на кнопках (подрезан ими)", synth(result_tail=(319, 1620, 367, 1641)), dict(counter=True), {"tail size", "tail not at resident face", "tail touches buttons"}),
        ("питомец с действием", synth(pet_attrs='text="" content-desc="" clickable="true" focusable="true"'), dict(counter=True), {"result nodes not empty"}),
        ("прилавок на середине сцены под облачком", synth(result_counter=(24, 1300, 1056, 1468)), dict(counter=True),
         {"counter not at resident bottom", "counter not at floor"}),
        ("житель выше прилавка", synth(result_resident=(0, 900, 535, 1596), result_tail=tail_at(1157)), dict(counter=True), {"counter not at resident bottom"}),
        ("прилавок и житель выше пола", synth(result_resident=(0, 906, 535, 1602), result_counter=(24, 1434, 1056, 1602), result_tail=tail_at(1163)),
         dict(counter=True), {"counter not at floor"}),
        ("прилавок 50 dp", synth(result_counter=(24, 1482, 1056, 1632)), dict(counter=True), {"counter height"}),
        ("житель спрятан за прилавком", synth(result_resident=(0, 1480, 535, 1632), result_tail=tail_at(1203)), dict(counter=True), {"counter top not below resident top"}),
        ("прилавок узкий", synth(result_counter=(24, 1464, 900, 1632)), dict(counter=True), {"counter width"}),
        ("прилавок у Марты", synth(), {}, {"counter on non-tray job"}),
        ("нет прилавка у пекарни", synth(counter=False), dict(counter=True), {"no counter"}),
        ("--line без строки", synth(), L, {"no line"}),
        ("строка на кнопках (узлов кнопок нет)", synth(line=(24, 1500, 1056, 1904), buttons=False), L, {"buttons hidden by line"}),
        ("кнопка подрезана строкой на 18 dp (узел 43 dp)", synth(line=(24, 1440, 1056, 1695), cut=54), L, {"buttons cut by line"}),
        # ponytail: not a real frame (uiautomator would cut the button) — only proves the last --line rule's code path
        ("строка ниже верха кнопок, кнопки целые", synth(line=(24, 1500, 1056, 1700)), L, {"line over buttons"}),
    ]
    bad = 0
    for i, (name, xml, kw, want) in enumerate(cases, 1):
        got = set(check(xml, 3, **kw))
        bad += got != want
        show = lambda r: "; ".join(sorted(r)) or "OK"
        print(("ok  " if got == want else "BAD ") + f"{i:2} {name}: {show(got)}" + ("" if got == want else f"  (want: {show(want)})"))
    print("SELFCHECK " + ("OK" if not bad else f"FAIL {bad}"))
    return 1 if bad else 0


if __name__ == "__main__":
    a = sys.argv[1:]
    sys.stdout.reconfigure(errors="replace")  # Cyrillic case names on a non-UTF-8 console
    if a[:1] == ["--selfcheck"]:
        sys.exit(selfcheck())
    pos = [x for x in a if not x.startswith("--")]
    if len(pos) != 2 or any(x.startswith("--") and x not in ("--big", "--counter", "--line") for x in a):  # a typo must not relax the probe
        sys.exit(__doc__.splitlines()[2])
    r = check(open(pos[0], encoding="utf-8").read(), float(pos[1]), "--big" in a, "--counter" in a, "--line" in a)
    print("GEOM OK" if not r else "GEOM FAIL: " + "; ".join(r))
    sys.exit(1 if r else 0)
