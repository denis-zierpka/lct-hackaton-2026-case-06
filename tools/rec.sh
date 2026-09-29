#!/usr/bin/env bash
# Запись перехода экрана и фон под каждым кадром (TOWN-A1c, ACCEPTANCE п. 7 б). Из корня репозитория; устройство —
# как в adbui.sh (ANDROID_SERIAL); нужны ffmpeg и tools/art_check.py which.
#   tools/rec.sh OUT_DIR SECONDS "tap:Текст" "xy:X Y" "sleep:1.5" "back" ...
# xy:X Y — прямой `input tap` по заранее снятым координатам (без дампа uiautomator — касание не опаздывает, WORKFLOW № 39).
# Пишет OUT_DIR/r.mp4, кадры f_NNN.png, which.txt и печатает последовательность вердиктов: «room_port_day×141 none×3
# bg_market_port×62» (none — кадр посреди crossfade или fade). Самопроверка перед прогоном — запись неподвижного экрана:
# все кадры — один фон. Касания через uiautomator медленные (≈ 1,5 с каждое): действия после конца записи в неё не попадают.
set -uo pipefail
A="$(cd "$(dirname "$0")" && pwd)/adbui.sh"; OUT=$1; SEC=$2; shift 2
command -v cygpath >/dev/null 2>&1 && OUT="$(cygpath -m "$OUT")"
mkdir -p "$OUT"; rm -f "$OUT"/f_*.png "$OUT"/which.txt
"$A" shell screenrecord --size 1080x1920 --time-limit "$SEC" /sdcard/rec_tmp.mp4 &
REC=$!; sleep 1.2
for a in "$@"; do
  case "$a" in
    tap:*) "$A" tap "${a#tap:}" >/dev/null 2>&1 || echo "  not found: ${a#tap:}" ;;
    xy:*) xy=${a#xy:}; "$A" tapxy ${xy% *} ${xy#* } ;;
    sleep:*) sleep "${a#sleep:}" ;;
    back) "$A" back ;;
  esac
done
wait $REC; sleep 1
"$A" pull /sdcard/rec_tmp.mp4 "$OUT/r.mp4" >/dev/null
ffmpeg -loglevel error -i "$OUT/r.mp4" "$OUT/f_%03d.png"
ls "$OUT"/f_*.png | xargs -n 80 python tools/art_check.py which > "$OUT/which.txt"
awk '{print $3}' "$OUT/which.txt" | uniq -c | awk '{printf "%s×%s  ", $2, $1} END {print ""}'
