#!/usr/bin/env bash
# Живая проверка «Городка» (TOWN-A1c, A1f, J1-1a): демо-профиль с нуля и маршрут по местам со снимками PREFIX_<экран>[10|13]
# при шрифте 1,0 и 1,3 в finny-pet/screenshots/ и зондом обрезки. Из корня; устройство — ANDROID_SERIAL (эмулятор после
# `tools/adbui.sh wm360` или S23). ВНИМАНИЕ: «Создать тестовый профиль» стирает прогресс — на S23 сначала бэкап профиля
# (HANDOFF, «Окружение»). Касания — по началу подписи (у S23 другие координаты), в раунде подноса — точно (tapx: кнопка
# витрины «Хлеб» — префикс слота «Хлеб на подносе»). Фон под снимками — tools/art_check.py which.
# Промах касания печатает «not found» и маршрут идёт дальше: лог проверять `grep -cE "not found|gate not passed"` → 0.
# Неделя 1 (3 смены): пекарня 1 (указатель, отдача с расхождением, «Спасибо!», итог) → Марта → пекарня 2 (новинка в
# облачке заказа, «?», загадка на полном подносе из лишних, подсказка) → после лимита — пекарня и рынок.
#   tools/town_route.sh PREFIX
set -u
P=${1:?PREFIX}
A="$(cd "$(dirname "$0")" && pwd)/adbui.sh"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
find_xy() { "$A" ui | awk -F'\t' -v p="$1" 'index($1,p)==1 || index($2,p)==1 {print $3, $4; exit}'; }
tp() {  # tap the first node whose text or description starts with $1; a slow device draws late — retry up to ~9 s
  local xy i
  for i in 1 2 3 4 5 6; do xy=$(find_xy "$1"); [ -n "$xy" ] && break; sleep 1.5; done
  [ -z "$xy" ] && { echo "  not found: $1"; return; }
  "$A" shell input tap $xy; sleep ${2:-1.5}
}
tapx() {  # tap the node whose text or description is exactly $1 (retry ~6 s)
  local xy i
  for i in 1 2 3 4 5 6; do xy=$("$A" ui | awk -F'\t' -v t="$1" '$1==t || $2==t {print $3, $4; exit}'); [ -n "$xy" ] && break; sleep 1.0; done
  [ -z "$xy" ] && { echo "  not found: $1"; return 1; }
  "$A" shell input tap $xy; sleep ${2:-0.6}
}
has() { "$A" ui | grep -qF "$1" || echo "  not found: node «$1» on $2"; }   # the screen we came to — its own node (WORKFLOW №32)
probe() { "$A" ui | grep -o 'Обрезано: [0-9]*' | head -1; }
shot1() { sleep 0.5; "$A" shot ${P}_$1 >/dev/null; echo "$1: $(probe)"; }
shot2() {
  "$A" font 1.0; sleep 1.5; "$A" shot ${P}_${1}10 >/dev/null; echo "${1}10: $(probe)"
  "$A" font 1.3; sleep 2.2; "$A" shot ${P}_${1}13 >/dev/null; echo "${1}13: $(probe)"
  "$A" font 1.0; sleep 1.5
}
swipe_up() { for i in $(seq ${1:-3}); do "$A" shell input swipe 540 1450 540 450 300; done; sleep 1; }  # from mid-screen: at y ≈ 1737 (360 × 640) sits the debug button «Макеты „Городка“»
scroll_to() {  # swipe up until a node starting with $1 is on screen: the parent screen grows with the profile's history
  local k; for k in 1 2 3 4 5 6 7 8 9 10; do [ -n "$(find_xy "$1")" ] && return; swipe_up 1; done
}
gate() {  # parent gate «A × B = ?»; on a slow device the field may get focus after the typing — check and retry
  local q a b i j
  question() { "$A" ui | grep -oE '[0-9]+ × [0-9]+ = \?' | head -1; }
  for j in $(seq 10); do q=$(question); [ -n "$q" ] && break; sleep 1.5; done   # wait for the gate to be drawn (cold start after install is slow)
  [ -z "$q" ] && echo "  gate not passed: no question"
  for i in 1 2 3; do
    [ -z "$q" ] && return
    a=${q%% ×*}; b=${q#*× }; b=${b%% =*}
    tp "Ответ" 1.5; "$A" text $((a * b)); sleep 1; tp "Войти" 2.5
    q=$(question)                                                               # still here — retry the new example
  done
  echo "  gate not passed"
}
plan() { tp "Банки"; for i in 1 2 3 4; do tp "Нужное: " 0.3; done; for i in 1 2; do tp "Хочу: " 0.3; done; tp "В копилку: " 0.3; tp "Готово" 1; tp "Да"; }

# ---- the tray round (TOWN-J1-1a § 4, § 6): the order is read from the customer's bubble «Имя. Заказ: хлеб, круассан»
order() { "$A" ui | awk -F'\t' 'index($2, ". Заказ: ") > 0 { s = $2; sub(/^[^.]*\. Заказ: /, "", s); sub(/\. Ещё нужно: .*$/, "", s); print s; exit }'; }
need() { "$A" ui | awk -F'\t' 'index($2, ". Ещё нужно: ") > 0 { s = $2; sub(/^.*\. Ещё нужно: /, "", s); print s; exit }'; }
TITLES="$(PYTHONIOENCODING=utf-8 python -c "import json; print(' '.join(p['title'] for j in json.load(open(r'$ROOT/finny-pet/app/src/main/assets/content/content.json', encoding='utf-8'))['town']['jobs'] if j.get('game') == 'TRAY' for p in j['menu']))")"
showcase() { local d t; d=$("$A" ui); for t in $TITLES; do printf '%s\n' "$d" | awk -F'\t' -v t="$t" '$2==t {f=1} END {exit !f}' && echo "$t"; done; }
cap() { PYTHONIOENCODING=utf-8 python -c "import sys; s=sys.argv[1].strip(); print(s[:1].upper() + s[1:])" "$1"; }
low() { PYTHONIOENCODING=utf-8 python -c "import sys; print(sys.argv[1].strip().lower())" "$1"; }
put_all() { local it; IFS=',' read -ra its <<< "$1"; for it in "${its[@]}"; do tapx "$(cap "$it")" 0.4 || return 1; done; }
serve() { local o; o=$(order); [ -z "$o" ] && { echo "  not found: order"; return 1; }; put_all "$o"; tapx "Отдать" "${1:-2.0}"; }
outside() {  # showcase items not in the order on screen, one per line
  local o m; o=",$(order | sed 's/, /,/g'),"
  showcase | while read -r m; do case "$o" in *",$(low "$m"),"*) ;; *) echo "$m" ;; esac; done
}
riddle_answer() {  # the right option of the question on screen, from content.json (town.quiz)
  "$A" ui > "$TMP_UI"
  PYTHONIOENCODING=utf-8 python - "$TMP_UI" "$ROOT" <<'PY'
import json, sys
ui = open(sys.argv[1], encoding="utf-8").read()
quiz = json.load(open(sys.argv[2] + "/finny-pet/app/src/main/assets/content/content.json", encoding="utf-8"))["town"]["quiz"]
for q in quiz:
    if q["question"] in ui:
        print(q["options"][q["correct"]]); break
PY
}
TMP_UI=$(mktemp)

"$A" font 1.0
"$A" launch >/dev/null; sleep 2.5; shot1 title
tp "Для взрослого"; gate; scroll_to "Создать тестовый профиль (демо)"; tp "Создать тестовый профиль (демо)"; tp "Да, продолжить" 2
for i in 1 2 3 4 5; do "$A" tap "Дальше" >/dev/null 2>&1 || break; sleep 1; done
tp "Создать питомца"; tp "Зайка" 0.5; tp "Рыжий" 0.5; tp "Финни" 0.5; swipe_up 1; tp "Начать!" 2.5
shot1 room
tp "Дверь: на улицу"; shot1 street
tp "Рынок у реки"; shot2 market
tp "У Фомы"; shot2 foma
tp "Домой"; tp "Дверь: на улицу"; "$A" shell input swipe 900 586 200 586 300; sleep 1; shot1 street2   # Боря за краем ряда
# ---- bakery, shift 1 (demo: demoSizes, 3 items, the pointer)
tp "Пекарня"; has "Заказ: " job; shot2 job                               # «Помоги Боре в пекарне!», «6–10», «Смены ○○○»
tapx "Начать смену" 2; has ". Заказ: " round; shot2 round              # customer, order, showcase of 3, pointer
serve 2.0                                                               # customer 1 (one item)
o=$(order); first=$(cap "${o%%,*}"); other=$(outside | head -1)         # customer 2: one right item and one outside the order
[ -z "$other" ] && echo "  not found: showcase item outside the order"
tapx "$first" 0.4; tapx "$other" 0.4; tapx "Отдать" 1.2; has "Ещё нужно: " wrong; shot2 wrong   # ✓ and dashed circles
put_all "$(need)"; tapx "Отдать" 0; "$A" shot ${P}_thanks >/dev/null; echo "thanks: shot"; sleep 1.5   # «Спасибо!» at once, 1.0
serve 2.0; serve 2.5; has "Спасибо за помощь" result; shot2 result      # customers 3, 4 → the result card
tapx "Готово" 1.5
tp "Домой"; plan; shot1 jars
tp "Домой"; tp "Лавки"; tp "Рынок у реки"; shot2 marketp
# the order card sits below the fold; a font change recreates the screen and loses the scroll — scroll after each
swipe_up 4; shot1 order10; "$A" font 1.3; sleep 2.2; swipe_up 4; shot1 order13; "$A" font 1.0; sleep 1.5; swipe_up 4
tp "Начать смену" 2; shot2 taps                                          # shift 2 of the week — Marta
tp "Разложить яблоки" 0.4; tp "Подмести у прилавка" 0.4; tp "Отнести ящик" 0.4; tp "Закончить" 2; shot2 tresult
tp "Готово"
# ---- bakery, shift 2 (3rd of the week): the new item in the order bubble, «?», the riddle on a full tray of extras
tp "Домой"; tp "Дверь: на улицу"; "$A" shell input swipe 900 586 200 586 300; sleep 1
tp "Пекарня"; has "Новинка" job2; shot2 job2                             # «Новинка — крендель!» in the bubble, ●●○
tapx "Начать смену" 2; has ". Заказ: " round2; shot2 round2             # 4 items on the showcase
serve 2.0; has "Загадка Бори" riddle_icon; shot1 riddle_icon             # after the first served — «?» at Borya
for m in $(outside | head -2); do tapx "$m" 0.4; done; shot1 full_wrong # customer 2: a full tray of items outside the order
tapx "Загадка Бори" 1.2; has "Не сейчас" riddle; shot2 riddle            # the bubble lives in the view model: survives the font change
ans=$(riddle_answer); [ -z "$ans" ] && echo "  not found: riddle question"
tapx "$ans" 1.2; has "Дальше" answer; shot1 answer                       # «Верно! …», «Подсказка Бори — на поднос»
tapx "Дальше" 1.0; shot1 hint                                            # an extra came off the end, a needed item is on the tray
tapx "Закончить" 2; shot1 result2; tapx "Готово" 1.5
has "Смены на неделе закончились" limit; shot2 limit                    # bakery after the limit: ●●●, no button
tp "Домой"; tp "Лавки"; tp "Рынок у реки"; swipe_up 4; shot1 marketlimit # market after the limit: no Marta's order card
tp "Домой"; shot1 home_from_place
tp "События"; shot1 board
tp "Домой"; tp "Копилка"; shot1 savings
tp "Домой"; tp "Кровать: сон" 2.5; shot1 night
tp "Сразу к итогу недели"; "$A" tap "Закончить неделю" >/dev/null; sleep 3.5; shot1 weekend   # exact: tp would hit the title «Закончить неделю?»
# weeks 2–5 (TOWN-A1f): the room window shows the resident who came last (Тоша, Степан, Кеша, Ася). «Как было» is below
# the fold; the new week's plan is not confirmed, and «Сразу к итогу недели» needs a confirmed plan — hence `plan`.
for w in 2 3 4 5; do
  swipe_up 3; tp "Как было"; tp "Играть дальше" 2.5; tp "Проснуться" 2.5
  "$A" ui | grep -q "Окно: улица" || echo "  not found: room of week $w"
  shot1 room_w$w
  [ $w -lt 5 ] && { plan; tp "Домой"; tp "Кровать: сон" 2.5; tp "Сразу к итогу недели"; "$A" tap "Закончить неделю" >/dev/null; sleep 3.5; }
done
rm -f "$TMP_UI"
echo "done $P"
