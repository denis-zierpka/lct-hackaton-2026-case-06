#!/usr/bin/env bash
# Живая проверка экранов через adb на Windows (Git Bash) и Linux: эмулятор или телефон.
# Устройство: ANDROID_SERIAL=emulator-5554 | RZCX923ZP4L (Samsung S23 команды) — иначе adb возьмёт единственное.
#
#   tools/adbui.sh ui                 — видимые тексты и описания с координатами (uiautomator)
#   tools/adbui.sh tap "Текст"        — нажать элемент по тексту или contentDescription
#   tools/adbui.sh tapxy X Y | back | text STR
#   tools/adbui.sh shot NAME [WIDTH]  — снимок в finny-pet/screenshots/emu_NAME.png + уменьшенный _s.jpg (в .gitignore)
#   tools/adbui.sh launch             — холодный запуск ru.finny.pet, печатает TotalTime
#   tools/adbui.sh wm360 | wmreset    — эмуляция ширины 360 dp и возврат
#   tools/adbui.sh font 1.3           — системный масштаб шрифта
#   tools/adbui.sh mute | unmute [N]  — громкость медиа 0 / N (тесты — без звука, после — вернуть)
#   tools/adbui.sh <любые аргументы adb>
# Samsung с «Защищённой папкой»: pm/am требуют --user 0 (launch это делает).
set -uo pipefail
export MSYS_NO_PATHCONV=1
SDK="${ANDROID_HOME:-${LOCALAPPDATA:-$HOME}/Android/Sdk}"
[ -d "$SDK" ] || SDK="$HOME/android-sdk"
ADB="$SDK/platform-tools/adb"; [ -x "$ADB.exe" ] && ADB="$ADB.exe"
OUT="$(cd "$(dirname "$0")/.." && pwd)/finny-pet/screenshots"
command -v cygpath >/dev/null 2>&1 && OUT="$(cygpath -m "$OUT")"  # Windows Python не понимает /c/…
PY=python; command -v python >/dev/null 2>&1 || PY=python3

nodes() {  # печатает: text \t desc \t cx \t cy \t selected
  "$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null && "$ADB" exec-out cat /sdcard/ui.xml | "$PY" -c '
import sys,re
for n in re.findall(r"<node [^>]*>", sys.stdin.read()):
    g=lambda k: (re.search(k+r"=\"([^\"]*)\"", n) or [None,""])[1]
    b=list(map(int,re.findall(r"\d+", g("bounds"))))
    if (g("text") or g("content-desc")) and len(b)==4:
        print(g("text"), g("content-desc"), (b[0]+b[2])//2, (b[1]+b[3])//2, g("selected"), sep="\t")'
}

case "${1:-}" in
  ui) nodes ;;
  tap) xy=$(nodes | awk -F'\t' -v t="$2" '$1==t || $2==t {print $3, $4; exit}')
       [ -z "$xy" ] && { echo "not found: $2" >&2; exit 1; }
       "$ADB" shell input tap $xy; echo "tap $2 @ $xy" ;;
  tapxy) "$ADB" shell input tap "$2" "$3" ;;
  back) "$ADB" shell input keyevent 4 ;;
  text) "$ADB" shell input text "$2" ;;
  shot) mkdir -p "$OUT"; "$ADB" exec-out screencap -p > "$OUT/emu_$2.png"
        "$PY" -c "from PIL import Image; im=Image.open(r'$OUT/emu_$2.png').convert('RGB'); im.thumbnail((${3:-420},2400)); im.save(r'$OUT/emu_$2_s.jpg',quality=72)"
        echo "$OUT/emu_$2_s.jpg" ;;
  launch) "$ADB" shell am force-stop --user 0 ru.finny.pet
          "$ADB" shell am start -W --user 0 -n ru.finny.pet/ru.finny.pet.MainActivity | grep -E "LaunchState|TotalTime" ;;
  wm360) "$ADB" shell wm size 1080x1920; "$ADB" shell wm density 480 ;;
  wmreset) "$ADB" shell wm size reset; "$ADB" shell wm density reset ;;
  font) "$ADB" shell settings put system font_scale "$2" ;;
  mute) "$ADB" shell cmd media_session volume --stream 3 --get | tail -1; "$ADB" shell cmd media_session volume --stream 3 --set 0 >/dev/null ;;
  unmute) "$ADB" shell cmd media_session volume --stream 3 --set "${2:-11}" >/dev/null ;;
  "") sed -n '2,15p' "$0" ;;
  *) "$ADB" "$@" ;;
esac
