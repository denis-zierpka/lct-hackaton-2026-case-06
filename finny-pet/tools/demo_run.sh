#!/usr/bin/env bash
# Full demo scenario (Appendix A of the ТЗ) on a running emulator/device:
# fresh install of the release APK, screen recording (docs/demo.mp4, ≤ 3 min), store screenshots.
# Usage: tools/demo_run.sh [apk]   (default: app/build/outputs/apk/release/app-release.apk)
set -euo pipefail
cd "$(dirname "$0")/.."
export PATH="$PATH:/opt/homebrew/share/android-commandlinetools/platform-tools"
APK="${1:-app/build/outputs/apk/classic/release/app-classic-release.apk}"
UI="python3 tools/ui.py"
OUT=screenshots/store
QA=docs/qa
rm -rf "$OUT"; mkdir -p "$OUT" "$QA"

# screenrecord is capped at 180 s per file, so the run is recorded in parts and joined with ffmpeg.
PARTS=()
REC=
rec_start() { adb shell rm -f "/sdcard/demo_$1.mp4"; adb shell screenrecord --time-limit 180 --bit-rate 3000000 "/sdcard/demo_$1.mp4" >/dev/null 2>&1 & REC=$!; PARTS+=("$1"); sleep 1; }
rec_stop() { adb shell pkill -l2 screenrecord >/dev/null 2>&1 || true; wait "$REC" 2>/dev/null || true; sleep 1; }

adb uninstall ru.finny.pet >/dev/null 2>&1 || true
adb install "$APK" | tail -1
rec_start 1
adb shell am start -n ru.finny.pet/.MainActivity >/dev/null
sleep 2

# 1–2. first launch, guest profile; enable the demo test profile from the adult section
$UI tap "Для взрослого" \; wait 1 \; gate \; wait 1 \; scroll \; tap "Создать тестовый профиль" \; tap "Да, продолжить" \; wait 0.7
$UI shot "$OUT/00_onboarding" \; tap Дальше \; tap Дальше \; tap "Создать питомца" \; wait 0.7
# 3. pet look and name
$UI tap Котик \; tap Рыжий \; tap Финни \; shot "$OUT/01_create_pet" \; scroll \; tap "Начать игру" \; wait 1 \; tap Понятно \; wait 0.7
# 4. starting budget on home
$UI shot "$OUT/02_home"
# 5. plan 50 / 20 / 30
$UI tap exact:План -1 \; wait 1 \; tap "Больше на 10" 0 \; tap "Больше на 10" 0 \; tap "Больше на 10" 0 \; tap "Больше на 10" 0 \; tap "Больше на 10" 0 \; tap "Больше на 10" 1 \; tap "Больше на 10" 1 \; tap "Больше на 10" 2 \; tap "Больше на 10" 2 \; tap "Больше на 10" 2 \; shot "$OUT/03_plan" \; scroll \; tap "Подтвердить план" \; wait 1 \; tap Понятно \; wait 0.7
# 6. task with a choice (+20 → 120)
$UI tap exact:Задания -1 \; wait 1 \; shot "$OUT/04_tasks" \; tapnear "С чего начать" Решить \; wait 1 \; tap "Купить корм и шампунь" \; wait 1 \; tap Понятно \; wait 0.7 \; tap exact:Питомец -1 \; wait 0.7
# 7. purchases: mandatory (30 + 20), optional (15) → 55 left; the 60-coin tent is out of reach
$UI tap exact:Магазин -1 \; wait 1 \; shot "$OUT/05_shop" \; tapnear Корм Купить \; tap Купить 1 \; wait 1 \; tap Понятно \; scroll \; tapnear Шампунь Купить \; tap Купить 1 \; wait 1 \; tap Понятно
$UI tap Желаемое \; wait 0.7 \; tapnear Бантик Купить \; tap Купить 1 \; wait 1 \; tap Понятно \; scroll \; scroll \; tapnear "Домик-палатка" "Не хватает" \; wait 1 \; shot "$OUT/06_not_enough" \; tap Понятно \; tap exact:Питомец -1 \; wait 0.7
# 6b. task with a number answer (+20 → 75)
$UI tap exact:Задания -1 \; wait 1 \; tapnear "Сколько в копилку" Решить \; wait 1 \; tap "Ответ (число)" \; type 30 \; key 4 \; tap Ответить \; wait 1 \; tap Понятно \; wait 0.7 \; tap exact:Питомец -1 \; wait 0.7
# 8. goal and savings
$UI tap exact:Копилка -1 \; wait 1 \; tapnear Самокат exact:Копить \; wait 1 \; tap Понятно \; tap exact:20 \; tap "Отложить 20" \; wait 1 \; tap Понятно \; shot "$OUT/07_savings" \; tap "Забрать 20" \; wait 1 \; shot "$OUT/08_withdraw_preview" \; tap Отмена \; tap exact:Питомец -1 \; wait 0.7
# 9–10. feedback and next week
$UI tapxy 735 1935 \; wait 1 \; tap Завершить 1 \; wait 1.5 \; shot "$OUT/09_summary" \; scroll \; tap "Дальше, к неделе" \; wait 1 \; shot "$OUT/10_home_week2"
rec_stop; rec_start 2
# weeks 2 and 3 quickly: plan, food + care, deposit, end → stage grows
for w in 2 3; do
  $UI tap exact:План -1 \; wait 1 \; tap "Больше на 10" 0 \; tap "Больше на 10" 0 \; tap "Больше на 10" 0 \; tap "Больше на 10" 0 \; tap "Больше на 10" 0 \; tap "Больше на 10" 2 \; tap "Больше на 10" 2 \; tap "Больше на 10" 2 \; scroll \; tap "Подтвердить план" \; wait 1 \; tap Понятно \; wait 0.7
  $UI tap exact:Магазин -1 \; wait 1 \; tapnear Корм Купить \; tap Купить 1 \; wait 1 \; tap Понятно \; scroll \; tapnear Шампунь Купить \; tap Купить 1 \; wait 1 \; tap Понятно \; tap exact:Питомец -1 \; wait 0.7
  $UI tap exact:Копилка -1 \; wait 1 \; tap exact:30 \; tap "Отложить 30" \; wait 1 \; tap Понятно \; tap exact:Питомец -1 \; wait 0.7
  $UI tapxy 735 1935 \; wait 1 \; tap Завершить 1 \; wait 1.5 \; scroll \; tap "Дальше, к неделе" \; wait 0.7
done
$UI shot "$OUT/11_pet_grown" \; tap "Прогресс" \; wait 1 \; shot "$OUT/12_progress" \; key 4 \; wait 0.7
rec_stop; rec_start 3
# 11. restart and check persistence
adb shell am force-stop ru.finny.pet; sleep 1; adb shell am start -n ru.finny.pet/.MainActivity >/dev/null; sleep 3
$UI find "Неделя" \; find "Копилка"
# 12. adult section: progress, reset test profile
$UI tap "Для взрослого" \; wait 1 \; gate \; wait 1 \; shot "$OUT/13_parent" \; scroll \; tap "Сбросить профиль" \; tap "Да, продолжить" \; wait 1 \; find "Знакомство"
sleep 2
rec_stop

# join the parts; if longer than 175 s, speed the video up to fit the 3-minute limit
: > "$QA/concat.txt"
for p in "${PARTS[@]}"; do adb pull "/sdcard/demo_$p.mp4" "$QA/demo_$p.mp4" >/dev/null; echo "file 'demo_$p.mp4'" >> "$QA/concat.txt"; done
ffmpeg -y -loglevel error -f concat -safe 0 -i "$QA/concat.txt" -c copy "$QA/demo_full.mp4"
DUR=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$QA/demo_full.mp4" | cut -d. -f1)
if [ "$DUR" -gt 175 ]; then
  F=$(python3 -c "print(round($DUR/170, 3))")
  ffmpeg -y -loglevel error -i "$QA/demo_full.mp4" -filter:v "setpts=PTS/$F" -an -r 30 docs/demo.mp4
else
  cp "$QA/demo_full.mp4" docs/demo.mp4
fi
echo "video: ${DUR}s raw -> $(ffprobe -v error -show_entries format=duration -of csv=p=0 docs/demo.mp4 | cut -d. -f1)s docs/demo.mp4"
ls "$OUT"
