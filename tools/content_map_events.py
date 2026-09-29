"""Раздел «События «Городка»» в finny-pet/docs/CONTENT_MAP.md — из content.json → town.events (ТЗ 5 п. 7).
Пишет между маркерами <!-- events:begin --> и <!-- events:end -->. Запуск из корня: python tools/content_map_events.py
Проверка без записи: python tools/content_map_events.py --check (exit 1, если раздел устарел)."""
import json, sys, pathlib

root = pathlib.Path(__file__).resolve().parent.parent
content = json.loads((root / "finny-pet/app/src/main/assets/content/content.json").read_text(encoding="utf-8"))
town = content["town"]
off = set(town.get("eventsOff", []))
places = {p["id"]: p["title"] for p in town["places"]}
themes = {"BUDGET": "Планирование бюджета", "SAVINGS": "Сбережения", "SHOPPING": "Платежи и покупки"}
verdict = {"GOOD": "верный", "OK": "нормально", "MISTAKE": "ошибочный"}

def arrives(e):
    a = e.get("arrives", {})
    s = f"неделя {a.get('week', 1)}" + (f", день {a['day']}" if a.get("day") else "")
    if e.get("triggers"):
        enter = [places.get(t[6:], t[6:]) for t in e["triggers"] if t.startswith("ENTER:")]
        s += "; приходит " + ("при входе: " + ", ".join(enter) if enter else "при подтверждении плана")
    if e.get("requires"):
        s += "; условия: " + ", ".join(f"`{r}`" for r in e["requires"])
    return s

lines = ["| Событие | Тема | Приход | Поступок → исход: строка для ребёнка | Восстановление |", "|---|---|---|---|---|"]
live = [e for e in town["events"] if e["id"] not in off and e["kind"] != "JOB"]
for e in live:
    outs = "<br>".join(f"`{o['fact']}`" + (f" при `{o['when']}`" if o.get("when") else "") + f" → {verdict[o['verdict']]}: «{o['line']}»" for o in e.get("outcomes", []))
    rec = "<br>".join(sorted({r for o in e.get("outcomes", []) for r in o.get("recovery", [])})) or "—"
    lines.append(f"| {e['title']} (`{e['id']}`) | {themes.get(e.get('theme'), '—')} | {arrives(e)} | {outs} | {rec} |")
jobs = [e for e in town["events"] if e["id"] not in off and e["kind"] == "JOB"]
text = ("\n".join([
    f"Живых событий в сборке — {len(live)} (засчитываемых в ТЗ 2.6), заказов-подработок — {len(jobs)}: "
    + ", ".join(f"«{j['title']}»" for j in jobs) + ". Выключены до среза 2 (`town.eventsOff`): "
    + ", ".join(f"`{i}`" for i in sorted(off)) + ".", "", *lines]))
p = root / "finny-pet/docs/CONTENT_MAP.md"
doc = p.read_text(encoding="utf-8")
b, e_ = "<!-- events:begin -->", "<!-- events:end -->"
new = doc[: doc.index(b) + len(b)] + "\n" + text + "\n" + doc[doc.index(e_):]
if "--check" in sys.argv:
    sys.exit(0 if new == doc else 1)
p.write_text(new, encoding="utf-8")
print(f"events: {len(live)}, jobs: {len(jobs)}")
