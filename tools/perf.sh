#!/usr/bin/env bash
# Замер плавности ru.finny.pet через gfxinfo (TOWN-S1f). Устройство — как в adbui.sh (ANDROID_SERIAL).
#   tools/perf.sh reset          — сбросить счётчики кадров перед сценарием
#   tools/perf.sh frames LABEL   — сводка: кадров, janky, p50/p90/p95/p99, медленных кадров UI-потока
#   tools/perf.sh max LABEL      — 5 самых долгих из последних ~120 кадров (IntendedVsync → FrameCompleted), > 100 и > 250 мс
# Урезанный эмулятор: emulator -avd finni -cores 2 -memory 3072 -gpu swiftshader_indirect.
# На swiftshader p50 ≈ 32 мс даже в покое — доля janky там не отражает телефон; порог S1f — кадров > 250 мс нет, p99 ≤ 150 мс.
set -uo pipefail
ADBUI="$(cd "$(dirname "$0")" && pwd)/adbui.sh"
PY=python; command -v python >/dev/null 2>&1 || PY=python3
case "${1:-}" in
  reset) "$ADBUI" shell dumpsys gfxinfo ru.finny.pet reset >/dev/null ;;
  frames) "$ADBUI" shell dumpsys gfxinfo ru.finny.pet | tr -d '\r' | awk -v L="${2:-}" '
    /Total frames rendered/ {t=$4} /Janky frames:/ && !j {j=$3" "$4} /50th percentile/ && !a {a=$3} /90th percentile/ && !b {b=$3}
    /95th percentile/ && !c {c=$3} /99th percentile/ && !d {d=$3} /Number Slow UI thread/ {u=$5}
    END {printf "%-12s frames %s janky %s p50 %s p90 %s p95 %s p99 %s slowUI %s\n", L, t, j, a, b, c, d, u}' ;;
  max) "$ADBUI" shell dumpsys gfxinfo ru.finny.pet framestats | tr -d '\r' | "$PY" -c "
import sys
d, hdr = [], None
for l in sys.stdin.read().split('\n'):
    if l.startswith('Flags,'): hdr = l.rstrip(',').split(','); continue
    if hdr and l[:2] in ('0,', '1,'):
        r = l.rstrip(',').split(','); a, b = int(r[hdr.index('IntendedVsync')]), int(r[hdr.index('FrameCompleted')])
        if b > a > 0: d.append((b - a) / 1e6)
d.sort(reverse=True)
print('${2:-}', 'frames', len(d), 'top5 ms', [round(x) for x in d[:5]], '>100ms', sum(x > 100 for x in d), '>250ms', sum(x > 250 for x in d))" ;;
  *) sed -n '2,8p' "$0"; exit 1 ;;
esac
