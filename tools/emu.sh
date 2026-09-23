#!/usr/bin/env bash
# Управление эмулятором Android для проверки экранов «Питомца Финни».
#
# Сервер общий с боевыми ботами (sol-auto, bybit-copy), поэтому эмулятор
# запускается ТОЛЬКО этим скриптом и только с ограничениями:
#   - 2 ядра, 2 ГБ памяти, без окна, без звука, программная отрисовка;
#   - пониженный приоритет процессора (nice 15): при конкуренции ядра получают боты;
#   - включается на время проверки и выключается сразу после.
# Сборку Gradle и эмулятор одновременно не запускать — скрипт это проверяет.
#
#   tools/emu.sh start            поднять и дождаться полной загрузки
#   tools/emu.sh install <apk>    поставить APK (с заменой)
#   tools/emu.sh launch <pkg>     запустить приложение с холодного старта
#   tools/emu.sh shot <файл.png>  скриншот экрана
#   tools/emu.sh tap <x> <y>      нажатие по координатам экрана
#   tools/emu.sh stop             выключить
#   tools/emu.sh status           запущен ли и сколько ест
set -euo pipefail

export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-$HOME/.android/avd}"
ADB="$ANDROID_HOME/platform-tools/adb"
EMU="$ANDROID_HOME/emulator/emulator"
AVD="finni"
LOG="/tmp/finni-emu.log"

need_kvm() {
  # Группа kvm выдаётся пользователю один раз; в старой сессии её может не быть,
  # тогда запускаем через sg — иначе эмулятор молча уходит в медленный режим.
  if [ -r /dev/kvm ] && [ -w /dev/kvm ]; then echo ""; else echo "sg kvm -c"; fi
}

is_running() { "$ADB" devices 2>/dev/null | grep -q '^emulator-'; }

case "${1:-}" in
  start)
    if is_running; then echo "эмулятор уже запущен"; exit 0; fi
    if pgrep -f 'GradleDaemon|gradlew' >/dev/null; then
      echo "ОТКАЗ: идёт сборка Gradle. Сначала дождись её или останови демон (./gradlew --stop)." >&2
      exit 1
    fi
    CMD="nice -n 15 $EMU -avd $AVD -no-window -no-audio -no-boot-anim -no-snapshot-save \
-gpu swiftshader_indirect -cores 2 -memory 2048 -netdelay none -netspeed full"
    PFX="$(need_kvm)"
    if [ -n "$PFX" ]; then
      setsid $PFX "$CMD" >"$LOG" 2>&1 < /dev/null &
    else
      setsid $CMD >"$LOG" 2>&1 < /dev/null &
    fi
    echo "жду загрузку (лог: $LOG)…"
    "$ADB" wait-for-device
    for i in $(seq 1 180); do
      [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && break
      sleep 2
    done
    [ "$("$ADB" shell getprop sys.boot_completed | tr -d '\r')" = "1" ] || { echo "не загрузился за 6 минут" >&2; exit 1; }
    # Системная оболочка без видеокарты первые десятки секунд после загрузки
    # не успевает отвечать, и Android вешает поверх всего диалог
    # «System UI isn't responding» — он попадает в каждый скриншот (2026-09-23).
    # Прячем диалоги ошибок системы и даём оболочке отдышаться.
    "$ADB" shell settings put global hide_error_dialogs 1
    sleep 30
    "$ADB" shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
    # Анимации системы выключаем: скриншоты не должны ловить середину перехода.
    "$ADB" shell settings put global window_animation_scale 0
    "$ADB" shell settings put global transition_animation_scale 0
    "$ADB" shell settings put global animator_duration_scale 0
    # Лог эмулятора про KVM молчит; надёжный признак — открытые дескрипторы /dev/kvm.
    # pgrep находит и обёртку `sg kvm -c …` с тем же текстом — проверяем все процессы.
    KVM_OK=""
    for QP in $(pgrep -f "qemu-system.*$AVD"); do
      # Без grep -q: при pipefail ранний выход grep даёт ls SIGPIPE, и конвейер
      # «падает» даже при найденном /dev/kvm — ложное «без KVM» (2026-09-23).
      [ -n "$(ls -l "/proc/$QP/fd" 2>/dev/null | grep kvm)" ] && KVM_OK=1
    done
    if [ -n "$KVM_OK" ]; then
      echo "ускорение: KVM"
    else
      echo "ВНИМАНИЕ: эмулятор работает без KVM — будет очень медленно" >&2
    fi
    echo "готов: $("$ADB" shell getprop ro.build.version.release | tr -d '\r'), экран $("$ADB" shell wm size | tr -d '\r' | awk '{print $3}')"
    ;;
  install)  "$ADB" install -r -g "${2:?путь к apk}" ;;
  launch)
    # am start -W ждёт первого кадра и печатает время холодного старта (ТЗ 3.4: ≤ 5 с).
    # Фиксированный sleep тут врал: вариант game за 6 с не успевал отрисоваться
    # и давал чёрный скриншот (2026-09-23).
    "$ADB" shell am force-stop "${2:?имя пакета}"
    COMP="$("$ADB" shell cmd package resolve-activity --brief "$2" | tr -d '\r' | tail -1)"
    "$ADB" shell am start -W -n "$COMP" | tr -d '\r' | grep -E "TotalTime" || true
    sleep 3   # Compose дорисовывает содержимое после первого кадра
    ;;
  shot)
    # Страховка: если системный диалог всё же всплыл, закрываем его до снимка.
    if "$ADB" shell dumpsys window 2>/dev/null | grep -q "mCurrentFocus=.*Not Responding"; then
      "$ADB" shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
      "$ADB" shell input keyevent KEYCODE_ESCAPE; sleep 1
    fi
    "$ADB" exec-out screencap -p > "${2:?файл png}"; echo "$2"
    ;;
  tap)      "$ADB" shell input tap "${2:?x}" "${3:?y}" ;;
  stop)
    if is_running; then "$ADB" emu kill >/dev/null 2>&1 || true; fi
    for i in $(seq 1 20); do pgrep -f "qemu-system.*$AVD" >/dev/null || break; sleep 1; done
    pkill -f "qemu-system.*-avd $AVD" 2>/dev/null || true
    echo "выключен"
    ;;
  status)
    if is_running; then
      ps -eo rss,pcpu,ni,args | grep "[q]emu-system.*$AVD" | awk '{printf "запущен: %.0f МБ памяти, %s%% процессора, приоритет nice %s\n",$1/1024,$2,$3}'
    else echo "не запущен"; fi
    ;;
  *) sed -n '2,20p' "$0"; exit 1 ;;
esac
