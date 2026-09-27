# Передача между сессиями

Правило (решение 2026-09-25): раз в 1–2 эпика сессия останавливается, обновляет этот файл,
готовит спеку следующей задачи в `docs/tasks/`, коммитит — работа продолжается в новой
сессии. Новая сессия читает этот файл первым. Пуш на GitHub — только по явной просьбе (CLAUDE.md).

## Где мы (2026-09-27, сессия 7 — TOWN-A1c сделана, решения владельца 40–41, эпик пекарни TOWN-J1 заведён)

- **`main`** — пакет 7.1: 1.3.0 (versionCode 4), тег `v1.3.0`; игра на `main` по правилам 1.3.0.
- **Ветка `feat/town`** (в `main` не влита; новые коммиты сессий 6–7 не запушены — `git status -sb` покажет «ahead»):
  срез 0, эпик TOWN-S1 «Одна неделя» ([`TOWN-S1.md`](tasks/TOWN-S1.md)), тесты 523/523 в обоих вариантах.
  Сверка: `git log --oneline -20`, `git status -sb`.
- **Эпик TOWN-A1 «Арт среза 1»** — [`docs/tasks/TOWN-A1.md`](tasks/TOWN-A1.md), решения владельца 27–41 (GAME_CONCEPT §18):
  | Задача | Статус |
  |---|---|
  | A1a инструменты, A1b превью стиля (ворота № 38) | **сделано** |
  | **A1c** фоны мест в UI | **сделано** — [`TOWN-A1c.md`](tasks/TOWN-A1c.md): 3 фона в APK (+127 720 Б, остаток бюджета эпика 920 856 Б), слой места над комнатой (`placeBackground` в `GameApp.kt`, crossfade 320 мс, без анимаций — сразу), пекарня — полка с хлебом в открытой зоне, плашки HUD непрозрачные; ревью PASS с 1-го круга; эмулятор 30 снимков и S23 17 снимков — верный фон (`which`); ворота № 40 — **б** |
  | **A1f** жители в UI (9 кадров `pet.py --residents`, `ResidentPic` по `res_<id>`) | **следующая** — спеку писать (строка в TOWN-A1.md) |
  | A1s лавка-витрина → A1d комната → A1e вещи и товары → A1g улица → A1h | порядок — в TOWN-A1.md |
- **Решения владельца сессии 7** (GAME_CONCEPT §18): **№ 40 — б**: фоны приняты; экран работы пекарни — без большого
  текстового окна, Боря на полу за прилавком с небольшим облачком, оплата диапазоном «6–10», загадка Бори — внутри игры,
  для пекарни — другая мини-игра. **№ 41 — а «Поднос по заказу»** (макеты `finny-pet/screenshots/town/bakery_mock_1.jpg`).
  По воротам A1c ещё: загрузчик фонов не делаем (вклад фонов в первый вход ≈ 15–55 мс, пики — смена экранов и до A1c;
  BACKLOG п. 14), «У Фомы» узнаётся слабо → витрина A1s, мелочи судей оставить.
- **Эпик TOWN-J1 «Пекарня в сцене и „Поднос по заказу“»** — [`docs/tasks/TOWN-J1.md`](tasks/TOWN-J1.md): рамка, бриф
  4 исследований (код и тесты, которые правит test-author, требования, аналоги, риски), 7 открытых вопросов, порядок.
  Проектирование (шаги 3–8 `dev-contour:concept-design`) не сделано; стартует после A1f (нужен спрайт Бори).

## Дальше

1. **A1f** — спека по контуру (критики → кодер Opus xhigh для Blender → приёмка → судьи по снимкам → ревью), листы.
2. **TOWN-J1** — концепция «Поднос по заказу» по брифу из `TOWN-J1.md`: 3–4 черновика с разных углов → 2 судьи → синтез →
   критики → решения владельцу с макетами (`tools/mock_bakery.py`) → раздел GAME_CONCEPT (решения с № 42) → срез 0 (контракт,
   оракул test-author) → срез 1 (сцена заказа и итога, мини-игра, загадка в игре, спрайты выпечки, фон пекарни).
3. Дальше по TOWN-A1: A1s (витрина), A1d, A1e, A1g, A1h.
4. **Людям, без сессии:** TalkBack вручную и звук/плавность на слух на S23 (BACKLOG п. 1, 11); плейтест 1 (§17.3,
   согласия законных представителей, ТЗ 8.4). **Пуш `feat/town`** и слияние в `main` — по просьбе.

Параллельно ждут людей: ключ подписи (у Дениса), доступ экспертов, отправка QUESTIONS.md заказчику.

**Как работать с владельцем**: решения даёт пачкой, пронумерованными ответами на пронумерованный список с
рекомендациями; критерий любой механики — «захочет ли ребёнок играть сам». Визуальное решает только по картинке —
варианты всегда листом/макетом на настоящем фоне (`tools/sheets.py`, `tools/mock_bakery.py`). **Экраны мест — сцена, а не
карточки** (решения 39, 40): житель на полу, небольшое облачко, короткие числа («6–10»); большой текстовый Panel на экране
места — регрессия. Одобрение стиля — только ответ после показа листа.

**Уроки сессии 7** — в WORKFLOW правила № 26–31: окно `perf.sh max` (читать ≤ 0,7 с после касания, A/B с BASE);
самопроверка зонда должна уметь покраснеть; замеры по снимкам — на сборке BASE; guard ловит `tools/` подстрокой;
полупрозрачный UI над новым фоном; переходы — записью экрана. Ещё: критики спеки (2 круга, 67 находок) и две панели судей
(этап а по композитам до коммита ассетов, этап б по живым снимкам) окупились — 3 круга правок пекарни и регрессия плашек
HUD найдены до ревью; ревьювер нашёл 0.

## Окружение (Windows 10, эта машина)

- JDK 17 Adoptium (`JAVA_HOME` задан), Android SDK — `%LOCALAPPDATA%\Android\Sdk` (platform 36, build-tools 36.0.0,
  emulator, образ android-36.1 google_apis x86_64), `finny-pet/local.properties` с `sdk.dir=C\:/Users/Singularity/AppData/Local/Android/Sdk`.
  `adb` и `aapt2` не в PATH Git Bash: adb — через `tools/adbui.sh <аргументы adb>`, aapt2 —
  `$LOCALAPPDATA/Android/Sdk/build-tools/36.0.0/aapt2.exe`.
- Gradle: из `finny-pet/` — `./gradlew` в Git Bash или `.\gradlew.bat` в PowerShell. Из Python вызывать `gradlew.bat`
  по абсолютному пути: `bash` из Python попадает в WSL. Кодеров с Gradle — по одному.
- **Blender 5.2 LTS** — `C:/Program Files/Blender Foundation/Blender 5.2/blender.exe` (не в PATH). Cycles на HIP (AMD RX 9060 XT):
  фон 1080 × 1920 × 160 сэмплов ≈ 35–40 с, `place.py -- --all` (3 места) ≈ 1 мин 40 с, житель ≈ 10 с. Всегда
  `-b --factory-startup --python-exit-code 1`; рендеры кодера — только вне репозитория. Кодер для Blender — Opus xhigh
  (Workflow `agent({agentType:'coder', model:'opus', effort:'xhigh'})`, с продолжением новым кодером при лимите ходов).
  Приёмка арта — `python tools/art_check.py`: `regress`/`diff` (дамп сцены: room, 6 питомцев, place market/foma, facade market;
  с модификаторами и углом солнца), `bg` (композиты UI со снимков BASE `emu_b_*` на фон места, контраст по месту;
  `--selfcheck` — самопроверка маски; `--veil A`), `which` (какой фон под снимком 360 × 640 или S23), `tiles` (ΔE плиток
  Match3 до фона), `bbox`, `palette`. Снимки `emu_b_*` (BASE A1c) лежат только на этой машине (`.gitignore`).
  MCP Blender подключён (N-панель → BlenderMCP → Connect); генераторы окно Blender не используют.
- Эмулятор: AVD `finni`, запуск `"$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe" -avd finni -no-snapshot-save -no-boot-anim -no-audio &`;
  урезанный для замера плавности — плюс `-cores 2 -memory 3072 -gpu swiftshader_indirect` (сейчас запущен урезанный, в нём
  debug `feat/town` с демо-профилем). Портрет: `tools/adbui.sh shell cmd window user-rotation lock 0`. 360 dp —
  `tools/adbui.sh wm360`, сброс — `wmreset`. Громкость медиа эмулятора — 5 (`mute` перед проверкой, `unmute 5` после).
- Телефон команды: Samsung Galaxy S23 (SM-S911B), Android 14, serial `RZCX923ZP4L`, 360 × 780 dp, 120 Гц.
  **Засыпает через 10 минут, после сна — защищённая блокировка** (разблокирует только человек, PIN не вводить) — перед
  блоком на телефоне просить владельца разблокировать. Тесты — без звука: `tools/adbui.sh mute` печатает громкость
  (сейчас 5) — вернуть `unmute 5`. «Защищённая папка» → `pm`/`am` с `--user 0`. На телефоне — debug `feat/town`
  (код bf09078), профиль команды восстановлен. **Бэкап профиля** (только у debug-сборки: run-as):
  `tools/adbui.sh exec-out run-as ru.finny.pet cat files/state.json > $T/s23_state.json`; возврат —
  `tools/adbui.sh exec-in run-as ru.finny.pet sh -c 'cat > files/state.json' < $T/s23_state.json`, проверка —
  `tools/adbui.sh exec-out run-as ru.finny.pet cat files/state.json | cmp - $T/s23_state.json`, затем force-stop.
  Release поверх debug ставится `install -r` (та же debug-подпись), профиль сохраняется, но run-as у release нет.
- Живая проверка: [`tools/adbui.sh`](../tools/adbui.sh) (`ui`, `tap "Текст"`, `tapxy`, `shot`, `launch`, `wm360`, `font 1.3`,
  `mute`/`unmute`); маршрут демо-профиля со снимками при 1,0 и 1,3 — [`tools/town_route.sh`](../tools/town_route.sh) PREFIX
  (стирает прогресс: на S23 — сначала бэкап); переходы — [`tools/rec.sh`](../tools/rec.sh) (запись + `which` по кадрам);
  замер экрана — `tools/ui_measure.py`; листы — `python tools/sheets.py OUT.jpg "Заголовок" emu_X.png "Подпись" …`;
  плавность — `tools/perf.sh` (`reset`, `frames`, `max` — **читать не позже 0,7 с после касания**, WORKFLOW № 26);
  зонды мутантов — `tools/mutation_probe.py`; макеты пекарни — `python tools/mock_bakery.py RES_DIR OUT_DIR`.
  Превью-сборка без правки ветки: `git worktree add --detach <scratch>/pv <sha>`, скопировать `local.properties`,
  `./gradlew assembleGameRelease`; удалить — PowerShell `Remove-Item -LiteralPath "\\?\<путь>" -Recurse -Force` и `git worktree prune`.
- Роли контура `test-author`, `coder`, `reviewer` — типы агентов (Agent/Workflow с `subagent_type`/`agentType`), хук guard их
  видит. Whitelist — `.claude/task-scope.json` (сейчас — скоуп всей закрытой TOWN-A1c), переписать перед спавном кодера;
  корневые `tools/*` в protect — поимённо (WORKFLOW № 29). Правки `docs/` и других protect-путей коммитить ДО спавна.
  Кодер упирается в 72 хода — продолжать новым агентом по дереву (workflow) или через SendMessage.
- Workflow-скрипты: `${…}` внутри шаблонных строк промпта — подстановка JS; результат с большим выводом — из
  `journal.jsonl` (строки `type: result`). `scriptPath` принимает только пути, которые вернул сам Workflow (scratch — нет):
  скрипт передавать inline или переиспользовать сохранённый.

## Стартовый промпт для новой сессии

> Продолжаем проект «Питомец Финни». Прочитай `docs/HANDOFF.md` и `CLAUDE.md`, затем `docs/tasks/TOWN-A1.md`
> (решения 27–41, порядок A1f → A1s → …), `docs/tasks/TOWN-A1c.md` (что сделано, итог и решения по воротам № 40) и
> `docs/tasks/TOWN-J1.md` (эпик пекарни «Поднос по заказу»: бриф и открытые вопросы). Ветка — `feat/town`.
> Задача — TOWN-A1f «Жители в UI»: напиши спеку (через критиков), затем по контуру (coder на Opus → приёмка твоя → судьи
> по снимкам → reviewer), живая проверка на эмуляторе и S23 без звука; телефон засыпает через 10 минут и после сна
> заблокирован — проси разблокировать перед прогоном. После A1f — концепция TOWN-J1 (шаги 3–8 concept-design).
