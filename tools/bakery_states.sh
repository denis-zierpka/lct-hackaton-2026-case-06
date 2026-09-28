#!/usr/bin/env bash
# Экраны пекарни на подменённых состояниях профиля (TOWN-J1-1a, 1a2, 1b1): быстрые снимки без полного маршрута town_route.sh.
# Из корня; устройство — ANDROID_SERIAL (эмулятор после `tools/adbui.sh wm360` или S23); сборка — ТОЛЬКО debug (run-as).
# Профиль: бэкап (проверка `[ -s ]` и json.load; прежний бэкап с тем же PREFIX — отказ) → на каждое состояние подмена
# state.json → снимки при шрифте 1,0 и 1,3 → возврат бэкапа и `cmp` (и при обрыве — trap). На S23 громкость и шрифт
# сохранить и вернуть самому (HANDOFF, «Окружение»).
#   tools/bakery_states.sh PREFIX [first|riddle[:QID]|limit|level2|big|levelup|zero|diary ...]   (по умолчанию — first riddle limit level2 big)
#   first      — 0 смен пекарни: экран заказа первой смены;
#   riddle:QID — 1 смена, загадка QID первая в очереди (вопросы до неё в town.quiz — отмечены решёнными): экран заказа с
#                новинкой, раунд, первый покупатель, облачко загадки — строка пользы, «Не сейчас», ПОСЛЕДНИЙ вариант и
#                нет прокрутки (`scrollable="true"`) при 1,0 и при 1,3 (WORKFLOW № 38); самые высокие при 1,3 — q_save_4,
#                q_budget_4 (TOWN-J1-1a2 § 3). Самопроверка признака прокрутки (№ 14): раздел взрослого (прокручиваемая
#                колонка) даёт в дампе 1 узел scrollable="true", комната — 0 (2026-09-28);
#   limit      — 3 смены недели: строка лимита целиком, кнопка «Домой» (текст) → комната («Окно: улица»);
#   level2     — 7 смен: «Большие заказы!», 7–11;
#   big        — 7 смен, демо выкл.: раунд с заказами из 3–4, облачко загадки при size − 1 верных (без строки пользы),
#                ответ — «Этот поднос Боря оставил тебе», расхождение на заказе из 4 (1,0 и 1,3).
#   levelup    — 5 смен: смена из 4 верных отдач → итог в сцене, худший случай высоты облачка (4★ и «новый уровень 2»:
#                «Заработали 10: 6 за смену + 4 за звёзды»), TOWN-J1-1b1 п. 11 б;
#   zero       — 1 смена: каждому покупателю сначала полный поднос с изделием вне заказа, затем недостающее → итог 0★
#                («Заработали 6: 6 за смену — придёт с новым конвертом», ряд ●●●●).
#   diary      — «Дневник» (TOWN-J1-1b2, bestStars): records.job_bakery 60 (наследие Match3) и 0 — строки «лучшая смена»
#                нет; 3 при 3 сменах — «Помочь Боре: лучшая смена, звёзд 3» (самопроверка листания), снимки при 1,0 и 1,3.
# Касания — точным совпадением (кнопка витрины «Хлеб» — префикс слота «Хлеб на подносе»). Промах печатает «not found»:
# лог проверять `grep -cE "not found|BACKUP"` → 0. DUMP=1 — рядом со снимком сырой дамп emu_PREFIX_<экран>.xml для
# `python tools/ui_measure.py <дамп> <метка> 3` (мишени < 48 dp, узлы за краем).
set -u
A="$(cd "$(dirname "$0")" && pwd)/adbui.sh"; P=${1:?PREFIX}; shift
STATES=${*:-first riddle limit level2 big}
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; T="${TEMP:-/tmp}"
command -v cygpath >/dev/null 2>&1 && { ROOT="$(cygpath -m "$ROOT")"; T="$(cygpath -m "$T")"; }
PY=python; command -v python >/dev/null 2>&1 || PY=python3
B="$T/${P}_backup.json"
[ -e "$B" ] && { echo "BACKUP exists: $B — верни его на устройство (или удали, если профиль цел) и повтори"; exit 1; }
"$A" exec-out run-as ru.finny.pet cat files/state.json > "$B"
[ -s "$B" ] && $PY -c "import json,sys; json.load(open(sys.argv[1],encoding='utf-8'))" "$B" || { echo "BACKUP FAILED"; rm -f "$B"; exit 1; }
restore() {
  trap - EXIT INT TERM
  "$A" font 1.0
  "$A" shell am force-stop --user 0 ru.finny.pet
  "$A" exec-in run-as ru.finny.pet sh -c 'cat > files/state.json' < "$B"; sleep 1
  if "$A" exec-out run-as ru.finny.pet cat files/state.json | cmp -s - "$B"; then echo "profile restored"; rm -f "$B"
  else echo "  not found: profile NOT restored — backup $B"; fi
  rm -f "$T/${P}_test.json" "$T/${P}_ui.txt" "$T/${P}_raw.xml"
}
trap restore EXIT INT TERM

tapx() { local xy i; for i in 1 2 3 4 5 6; do xy=$("$A" ui | awk -F'\t' -v t="$1" '$1==t || $2==t {print $3, $4; exit}'); [ -n "$xy" ] && break; sleep 1; done
  [ -z "$xy" ] && { echo "  not found: $1"; return 1; }; "$A" shell input tap $xy; sleep ${2:-0.8}; }
text_xy() { local xy i; for i in 1 2 3 4 5 6; do xy=$("$A" ui | awk -F'\t' -v t="$1" '$1==t {print $3, $4; exit}'); [ -n "$xy" ] && break; sleep 1; done; echo "$xy"; }  # TEXT node, not description
has() { "$A" ui | grep -qF "$1" || echo "  not found: node «$1» on $2"; }
probe() { "$A" ui | grep -o 'Обрезано: [0-9]*' | head -1; }
shot() { sleep 0.5; "$A" shot ${P}_$1 >/dev/null; echo "$1: $(probe)"
  [ -n "${DUMP:-}" ] && { "$A" shell uiautomator dump /sdcard/ui.xml >/dev/null; "$A" exec-out cat /sdcard/ui.xml > "$ROOT/finny-pet/screenshots/emu_${P}_$1.xml"; }; }  # DUMP=1: raw dump for tools/ui_measure.py
shot2() { "$A" font 1.0; sleep 1.5; shot ${1}10; "$A" font 1.3; sleep 2.5; shot ${1}13; "$A" font 1.0; sleep 1.5; }
order() { "$A" ui | awk -F'\t' 'index($2, ". Заказ: ") > 0 { s = $2; sub(/^[^.]*\. Заказ: /, "", s); sub(/\. Ещё нужно: .*$/, "", s); print s; exit }'; }
cap() { PYTHONIOENCODING=utf-8 $PY -c "import sys; s=sys.argv[1].strip(); print(s[:1].upper() + s[1:])" "$1"; }
low() { PYTHONIOENCODING=utf-8 $PY -c "import sys; print(sys.argv[1].strip().lower())" "$1"; }
need() { "$A" ui | awk -F'\t' 'index($2, ". Ещё нужно: ") > 0 { s = $2; sub(/^.*\. Ещё нужно: /, "", s); print s; exit }'; }
put_list() { local it; IFS=',' read -ra its <<< "$1"; for it in "${its[@]}"; do tapx "$(cap "$it")" 0.4; done; }
TITLES="$(PYTHONIOENCODING=utf-8 $PY -c "import json; print(' '.join(p['title'] for j in json.load(open(r'$ROOT/finny-pet/app/src/main/assets/content/content.json', encoding='utf-8'))['town']['jobs'] if j.get('game') == 'TRAY' for p in j['menu']))")"
outside() { local o m d; o=",$(order | sed 's/, /,/g'),"; d=$("$A" ui); for m in $TITLES; do printf '%s\n' "$d" | awk -F'\t' -v t="$m" '$2==t {f=1} END {exit !f}' || continue; case "$o" in *",$(low "$m"),"*) ;; *) echo "$m" ;; esac; done; }
quiz_on_screen() {  # FIELD (correct|last) of the town.quiz question on screen
  "$A" ui > "$T/${P}_ui.txt"; PYTHONIOENCODING=utf-8 $PY - "$T/${P}_ui.txt" "$ROOT" "$1" <<'PY'
import json, sys
ui = open(sys.argv[1], encoding="utf-8").read()
quiz = json.load(open(sys.argv[2] + "/finny-pet/app/src/main/assets/content/content.json", encoding="utf-8"))["town"]["quiz"]
for q in quiz:
    if q["question"] in ui:
        print(q["options"][q["correct"]] if sys.argv[3] == "correct" else q["options"][-1]); break
PY
}
riddle_fits() {  # $1 — screen label: the last option is on screen and the bubble does not scroll (WORKFLOW № 38)
  local last; last=$(quiz_on_screen last)
  [ -z "$last" ] && { echo "  not found: riddle question on $1"; return; }
  has "$last" "$1"
  "$A" shell uiautomator dump /sdcard/ui.xml >/dev/null; "$A" exec-out cat /sdcard/ui.xml > "$T/${P}_raw.xml"
  grep -q 'scrollable="true"' "$T/${P}_raw.xml" && echo "  not found: riddle bubble without scrolling on $1"
}
state() {  # jobShifts shiftsThisPeriod demo(keep|false) [target quiz id] [records.job_bakery]
  PYTHONIOENCODING=utf-8 $PY - "$B" "$T/${P}_test.json" "$1" "$2" "$3" "${4:-}" "$ROOT" "${5:-}" <<'PY'
import json, sys
s = json.load(open(sys.argv[1], encoding="utf-8"))
s.setdefault("jobShifts", {})["job_bakery"] = int(sys.argv[3]); s["shiftsThisPeriod"] = int(sys.argv[4])
s["riddleAsked"] = False; s["asleep"] = False
if sys.argv[5] == "false": s["demo"] = False
if sys.argv[6]:
    ids = [q["id"] for q in json.load(open(sys.argv[7] + "/finny-pet/app/src/main/assets/content/content.json", encoding="utf-8"))["town"]["quiz"]]
    s["riddles"] = [{"taskId": q, "correct": True, "reward": 0, "period": s.get("period", 1)} for q in ids[:ids.index(sys.argv[6])]]
if sys.argv[8]: s.setdefault("records", {})["job_bakery"] = int(sys.argv[8])
json.dump(s, open(sys.argv[2], "w", encoding="utf-8"), ensure_ascii=False)
PY
  "$A" shell am force-stop --user 0 ru.finny.pet
  "$A" exec-in run-as ru.finny.pet sh -c 'cat > files/state.json' < "$T/${P}_test.json"; sleep 1
}
open_diary() { "$A" font 1.0; "$A" launch >/dev/null; sleep 3; tapx "Продолжить" 2.5; tapx "Дневник" 2; }
best_row() {  # the «…: лучшая смена, звёзд N» node, scrolling down with a check after each swipe (≤ 6 swipes)
  local k n; for k in 0 1 2 3 4 5 6; do n=$("$A" ui | awk -F'\t' 'index($2, ": лучшая смена, звёзд ") > 0 {print $2; exit}'); [ -n "$n" ] && { echo "$n"; return; }
    "$A" shell input swipe 540 1450 540 650 300; sleep 0.8; done; }
open_bakery() { "$A" font 1.0; "$A" launch >/dev/null; sleep 3; tapx "Продолжить" 2.5; tapx "Дверь: на улицу" 1.5
  "$A" shell input swipe 900 586 200 586 300; sleep 1; tapx "Пекарня, Боря" 2; }

for st in $STATES; do case $st in
  first)  state 0 0 keep; open_bakery; has "Заказ: " first; shot2 first ;;
  riddle*) q=${st#riddle}; q=${q#:}; tag=riddle${q:+_$q}
          state 1 0 keep "$q"; open_bakery; has "Новинка" ${tag}_job; shot2 ${tag}_job
          tapx "Начать смену" 2.5; has ". Заказ: " ${tag}_round; shot2 ${tag}_round
          put_list "$(order)"; tapx "Отдать" 2.2; has "Загадка Бори" ${tag}_icon
          tapx "Загадка Бори" 1.5; has "Не сейчас" $tag; has "Отгадаешь — Боря поможет с подносом" $tag
          riddle_fits ${tag}10; shot ${tag}10
          "$A" font 1.3; sleep 2.5; riddle_fits ${tag}13; shot ${tag}13; "$A" font 1.0; sleep 1.5
          tapx "Не сейчас" 1; tapx "Закончить" 2; tapx "Готово" 1 ;;
  limit)  state 1 3 keep; open_bakery; has "Смены на неделе закончились — приходи на новой неделе" limit; shot2 limit
          xy=$(text_xy "Домой"); [ -z "$xy" ] && echo "  not found: button «Домой» on limit" || { "$A" shell input tap $xy; sleep 2
          "$A" ui | grep -q "Окно: улица" || echo "  not found: room after «Домой»"; } ;;
  level2) state 7 0 keep; open_bakery; has "Большие заказы" level2; shot2 level2 ;;
  big)    state 7 0 false; open_bakery; tapx "Начать смену" 2.5
          o=$(order); put_list "$o"; tapx "Отдать" 2.2                                   # customer 1: right tray
          o=$(order); put_list "${o%, *}"; shot big_almost                               # customer 2: all but the last
          tapx "Загадка Бори" 1.5; shot big_riddle_idle                                  # nothing to put: no helper line
          "$A" ui | grep -qF "Отгадаешь — Боря поможет с подносом" && echo "  not found: no helper line when the hint is idle"
          a=$(quiz_on_screen correct); [ -z "$a" ] && echo "  not found: riddle question"
          tapx "$a" 1.2; has "Этот поднос Боря оставил тебе" big_hint_idle; shot big_hint_idle
          tapx "Дальше" 0.8; put_list "${o##*, }"; tapx "Отдать" 2.2
          o=$(order); n=$(echo "$o" | awk -F', ' '{print NF}'); tapx "$(cap "${o%%,*}")" 0.4  # customer 3: one right + outside
          for m in $(outside | head -$((n - 1))); do tapx "$m" 0.4; done; tapx "Отдать" 1.2; has "Ещё нужно: " big_wrong; shot2 big_wrong
          tapx "Закончить" 2; tapx "Готово" 1 ;;
  levelup) state 5 0 keep; open_bakery; tapx "Начать смену" 2.5
          for c in 1 2 3 4; do put_list "$(order)"; tapx "Отдать" 2.2; done               # 4 customers, all right the first time
          has "Заработали 10: 6 за смену + 4 за звёзды" levelup; has "новый уровень 2" levelup; shot2 levelup; tapx "Готово" 1 ;;
  zero)   state 1 0 keep; open_bakery; tapx "Начать смену" 2.5
          for c in 1 2 3 4; do                                                           # a full tray with one item outside the order
            o=$(order); x=$(outside | head -1); [ -z "$o" ] || [ -z "$x" ] && echo "  not found: order or outside item on zero"
            case "$o" in *", "*) put_list "${o%, *}" ;; esac; tapx "$x" 0.4
            tapx "Отдать" 1.2; has "Ещё нужно: " zero_wrong$c; put_list "$(need)"; tapx "Отдать" 2.2
          done
          has "Заработали 6: 6 за смену — придёт с новым конвертом" zero; shot2 zero; tapx "Готово" 1 ;;
  diary)  state 0 0 keep "" 60; open_diary; has "Очки роста" diary; n=$(best_row); [ -n "$n" ] && echo "  not found: no «лучшая смена» for the Match3 legacy 60 (got «$n»)"
          state 0 0 keep "" 0; open_diary; has "Очки роста" diary; n=$(best_row); [ -n "$n" ] && echo "  not found: no «лучшая смена» for 0 stars (got «$n»)"
          state 3 0 keep "" 3; open_diary; has "Очки роста" diary; n=$(best_row); [ "$n" = "Помочь Боре: лучшая смена, звёзд 3" ] || echo "  not found: «Помочь Боре: лучшая смена, звёзд 3» (got «$n»)"
          shot2 diary ;;
  *) echo "  not found: unknown state $st" ;;
esac; done
echo "done $P"
