#!/usr/bin/env bash
# Живая проверка «Городка» (TOWN-A1c, A1f): демо-профиль с нуля и маршрут по местам со снимками PREFIX_<экран>[10|13] при
# шрифте 1,0 и 1,3 в finny-pet/screenshots/ и зондом обрезки. Из корня; устройство — ANDROID_SERIAL (эмулятор после
# `tools/adbui.sh wm360` или S23). ВНИМАНИЕ: «Создать тестовый профиль» стирает прогресс — на S23 сначала бэкап профиля
# (HANDOFF, «Окружение»). Касания — по началу подписи (у S23 другие координаты). Фон под снимками — tools/art_check.py which.
# Промах касания печатает «not found» и маршрут идёт дальше: лог проверять `grep -cE "not found|gate not passed"` → 0.
#   tools/town_route.sh PREFIX
set -u
P=${1:?PREFIX}
A="$(cd "$(dirname "$0")" && pwd)/adbui.sh"
find_xy() { "$A" ui | awk -F'\t' -v p="$1" 'index($1,p)==1 || index($2,p)==1 {print $3, $4; exit}'; }
tp() {  # tap the first node whose text or description starts with $1; a slow device draws late — retry up to ~9 s
  local xy i
  for i in 1 2 3 4 5 6; do xy=$(find_xy "$1"); [ -n "$xy" ] && break; sleep 1.5; done
  [ -z "$xy" ] && { echo "  not found: $1"; return; }
  "$A" shell input tap $xy; sleep ${2:-1.5}
}
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
tp "Пекарня"; shot2 job
tp "Начать смену" 2; shot2 round
tp "Закончить" 2; shot2 result
tp "Готово"; tp "Домой"; plan; shot1 jars
tp "Домой"; tp "Лавки"; tp "Рынок у реки"; shot2 marketp
# the order card sits below the fold; a font change recreates the screen and loses the scroll — scroll after each
swipe_up 4; shot1 order10; "$A" font 1.3; sleep 2.2; swipe_up 4; shot1 order13; "$A" font 1.0; sleep 1.5; swipe_up 4
tp "Начать смену" 2; shot2 taps
tp "Разложить яблоки" 0.4; tp "Подмести у прилавка" 0.4; tp "Отнести ящик" 0.4; tp "Закончить" 2; shot2 tresult
tp "Готово"; tp "Домой"; shot1 home_from_place
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
echo "done $P"
