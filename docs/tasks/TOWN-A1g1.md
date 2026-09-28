# TOWN-A1g1 — генератор улицы: фон `bg_street_port` и приметы фасадов (`facade.py`, пилот № 61 б)

```
TASK: TOWN-A1g1 — статичный фон улицы и точечная доработка фасадов в finny-pet/tools/art/facade.py (только генератор;
      встройка в UI — A1g2)
EPIC: TOWN-A1 (docs/tasks/TOWN-A1.md: строка A1g :46, пилот № 61 б :56-60)
BASE: sha коммита этой спеки; в том же коммите — правка tools/art_check.py (регресс 7 фасадов), доли бюджета в
      TOWN-A1.md и строка HANDOFF.md:111 (раздел «До спавна»); worktree wt/a1g и wt/j1b уже построены от d35ac7c
      (коммит 6b2277d) и переносятся на BASE rebase-ом («До спавна» п. 6)
BRANCH: feat/town; кодер — git worktree wt/a1g/ (внутри корня, wt/ в .gitignore:19), ветка pilot/a1g, своя
        сессия Claude Code в каталоге worktree и свой wt/a1g/.claude/task-scope.json (вариант A; запасной B — ниже);
        в feat/town переносится только facade.py, ветка не мержится (раздел «Слияние») — после ворот арта
ЗАВИСИТ ОТ: № 30 (один статичный фон, GAME_CONCEPT.md:2047), № 38 (стиль 7 фасадов, :2055), № 61 б (:2078, dd72652);
        регресс art_check.py с place_bakery и props (d35ac7c); параллельно — 1b3 (wt/j1b/: props.py, place.py,
        to_webp.py) и 1b1–1b2 (основное дерево).
        № 39, 40 (GAME_CONCEPT.md:2056-2057) касаются экранов мест; улица по № 30 а — ряд карточек-фасадов, большого
        текстового окна на ней нет, подпись карточки — имя и название, как на листе № 38
```
Номера строк — дерево HEAD `30cdc49` (доки 1b1 закоммичены; код 1b1 в основном дереве не закоммичен, `tools/` и
`finny-pet/tools/` он не трогает).

## КОНТЕКСТ
- **Фасады уже есть, их стиль одобрен.**
  - 7 id в `FACADES`: `facade.py:194-195`, коммит b7675bc (A1b).
  - Одобрены на воротах № 38 по листу `finny-pet/screenshots/town/a1b_style_c_facade.jpg`.
  - Кадр 384 × 384 RGBA, ортокамера (`facade.py:19`, :198-204); зоны карточки описаны в докстринге :6-11.
- **Фона улицы нет нигде.**
  - Его нет ни в `facade.py`, ни в `place.PLACES`: там только market, foma, bakery (`place.py:24-51`).
  - В `game/res` нет файлов `bg_street*`.
  - Для улицы `placeBackground` возвращает `null` (`GameApp.kt:242-246`), поэтому улица лежит на комнате (`GameApp.kt:185`; BACKLOG.md:82, п. 12).
- **Пекарня разошлась с одобренным листом.**
  - На листе № 38 табличка кремовая: на 61a6602 в `place.py` стоит `board="#FFF3D6"`.
  - A1c поменял её на `#F4A261` (`place.py:46`) ради фона пекарни, причина — в комментарии :47-48.
  - Фасад берёт доску из `c["board"]`, а рамку доски и дверь — из `c["counter"]` (`facade.py:131`, :139). Это тот же `#F4A261`, так что доска слилась с рамкой.
  - TOWN-A1c.md:295-296 прямо просит «учесть в A1g».
- **Дом и пекарня — один силуэт.**
  - IoU альфа-масок `fac_home` и `fac_bakery` = **0,989**. Для сравнения: home–foma 0,924, foma–bakery 0,927, market–foma 0,858.
  - Замер по рендерам HEAD (`--all`, 160 сэмплов, 2026-09-28), судья повторил его на тех же файлах.
  - Пекарню отличают только труба, мини-навес и караваи в окне; в сером виде они почти одинаковы.
  - Место на улице ребёнок должен узнать без чтения: ТЗ 3.6, ТЗ_текст.txt:389 («Открыв любой раздел, ребенок понимает, что здесь можно сделать…»). Пункт о цвете (:395-396) говорит о категории расхода, здесь он — довод по аналогии.
- **Таблички фасадов без текста** (`facade.py:77`; TOWN-A1b.md:116).
  - Примета-рисунок есть только у зоопарка — лапа (`facade.py:187-191`).
  - Калитки парка, леса и зоопарка различаются силуэтом деревьев (IoU 0,75–0,82, fac160) и цветом забора (`facade.py:22-24`).
- **Небо рынка темнее мира.**
  - Панель неба фасада рынка `#6FB1E0` сделана без свечения (`facade.py:94`); в центре кадра она рендерится в (83,116,154).
  - Небо фона рынка ≈ (130,193,240) (`place.py:78`, emit 0,6).
  - Замеры: исследование и судья, рендер HEAD.
- **Имена файлов калиток не по правилу.**
  - Правило — `fac_<placeId>` (TOWN-A1.md:68).
  - Id мест: `park`, `forest`, `zoo` (content.json:780, :789, :798).
  - `--all` пишет `fac_gate_park` и т. д. (`facade.py:22-24`, :215).
- **Где будет виден фон.** Замер исследования по снимкам, 3 px/dp.
  - Ряд карточек:
    - 360 × 640 при 1,0 (`emu_j1a2e_street.png`): y 375–796 px, это 19,5–41,5 %;
    - при 1,3 (`emu_s1d_street13.png`, старый снимок, код карточки тот же с 2a20d9f): верх карточки на 386 px;
    - S23 (`emu_j1a2s_street.png`, в координатах фона): 15–33 %.
  - Калитки — до ≈ 75 % на 360 × 640 и до ≈ 52 % на S23; ниже на S23 открыто ≈ 48 % фона.
  - На S23 фон растянут ×1,219 и обрезан на 118 px с каждого бока (`art_check.py:47`).
  - Заголовок «Улица» цветом `G.ink` лежит прямо на фоне (`StreetScreen.kt:61`).
- **Пилот № 61 б** идёт параллельно с 1b3 (`wt/j1b/`, TOWN-J1-1b.md:374-480).
  - `lib.py` заморожен.
  - `facade.py` импортирует `place` (`facade.py:16`); 1b3 обязуется не менять `PLACES`, `M`, `put`, `cloud` (TOWN-J1-1b.md:429-430).
  - Остальной `place.py` без флага 1b3 оставляет «побайтно прежним» (:430), а его свет и мир ловят дампы `place_*` из приёмки 1b3 п. 1 (TOWN-J1-1b.md:443-445; дамп несёт свет и мир, `art_check.py:80-91`).
- **Время рендера** (замер исследования, HIP):
  - `--all` — 41,5 с при 160 сэмплах, один фасад — 3,5–8,4 с;
  - фон места 1080 × 1920 — 35,0 с полный и 14,8 с в `--preview`.
- **ТЗ.**
  - 3.5: тексты и визуальные реакции «не должны запугивать, стыдить» (ТЗ_текст.txt:375).
  - 3.6: мишени ≥ 48 dp, контраст, цвет — не единственный признак.
  - 3.3: право на изображения (строка LICENSES — в A1g2).

## ЧТО УВИДИТ РЕБЁНОК НА УЛИЦЕ (после встройки A1g2; A1g1 экран не меняет)
| Момент | Что на экране | Что понимает ребёнок |
|---|---|---|
| вышел в дверь | Небо, земля и ряд домиков, которые стоят на улице (№ 70). Сверху шапка и тёмное «Улица» на светлом небе | «Я на улице своего городка — это тот же мир, что рынок у реки» |
| смотрит на ряд | Свой дом со своим питомцем 64 dp внизу по центру, как житель у мест (`facade.py:10`; № 73), рынок-навес, синяя лавка с плоским верхом, пекарня с кренделем (№ 71) | Куда идти, видно по форме, а не по подписи |
| событие | «!» в углу карточки (`StreetScreen.kt:90-92`); на фоне нет красных и малиновых круглых вещей; на фасаде рынка — шарики и мяч (одобрены № 38, `facade.py:106-108`) | «!» заметен и на карточке рынка — кадр К3 |
| свайп | Ряд едет, небо и улица стоят (№ 30); за краем ряда — пекарня и Боря | Ряд продолжается |
| калитки | Закрытые калитки: за забором деревья своего места, примета на доске, картинка мечты и «0 / 150» (№ 72) | «Накоплю — открою». Не страшно и не дразнит: внутри никого, замок не нависает |
| S23 | Нижняя половина экрана — спокойная улица без мест, дверей и кнопок | Вне карточек нажимать нечего |

## CONTRACT (только `finny-pet/tools/art/facade.py`)
1. **Фон улицы.**
   - Функции `street()` и `render_street(out, samples, preview=False)`.
   - CLI: `--street OUT [--preview] [--samples N]` — PNG 1080 × 1920 RGB; `--preview` даёт 540 × 960 и 24 сэмпла.
   - Непрозрачный кадр: `film_transparent = False`, `color_mode = "RGB"`, как `place.py:167-168`.
   - `--all` фон НЕ рендерит.
   - Имя файла приёмки — `bg_street_port`. Места «улица» в `places` нет, поэтому имя закрепляется этой спекой; в доки оно пойдёт строкой TOWN-A1.md:68.
2. **Один мир с фонами мест.**
   - Камера — `lib.camera`; стартовая точка — камера `render_place` (`place.py:171`).
   - Свет — только `lib.studio`/`lib.light`. `place.day_light` не вызывать: у 1b3 он не заморожен (TOWN-J1-1b.md:429-430 замораживает `PLACES`, `M`, `put`, `cloud`), смену его сигнатуры дампы `place_*` не поймают.
   - Цвет и emit неба фона — одна константа `SKY`, чтобы вариант в) № 74 оркестратор рендерил правкой одной строки, как `ROW`.
   - Цвета берутся только из уже существующих значений:
     - `place.PLACES[…]["colors"]`: небо, холмы и вода рынка (`place.py:26-27`);
     - литералы `facade.py`: плита `#E8DCCB` (:82, :114, :128), газон `#9ADB8E` (:144), ствол `#B9743F` и кроны `#7BC47F` / `#5EAA66` (:145-146), небо `#6FB1E0` (:94).
     - Нужен новый hex — оставить `TODO` в коде и строку в отчёте; решает владелец по листу.
   - Облака — `place.cloud`.
   - Из `place` читать только `PLACES`, `M`, `put`, `cloud`. `day_light`, `riverside`, `build`, `bread_shelf`, `render_place` и `props.*` напрямую не вызывать.
   - В `street()` не вызывать `put`: товар на фоне — это запечённая вещь (урок № 27).
3. **Зоны фона** — доли высоты кадра 1080 × 1920; на S23 видна вся высота и центральные 844 px ширины.
   - **0–20 % (y 0–386)** — ровное светлое небо, без облаков.
     - Здесь лежат статус-бар, `Hud1` и «Улица» `G.ink` при 1,0 и 1,3; верх ряда — 375 и 386 px.
     - Облака у рынка выглядывали между плашками HUD (`place.py:76-77`).
     - Проверка — приёмка п. 5.
   - **Ряд, 15–42 %** — крупные спокойные массы. Фон виден в щелях 8 dp, над крышами и вокруг калиток и едет «под» карточками; мелкая деталь выдаст скольжение (№ 30).
   - **Земля — по № 70**, одна константа `ROW`:
     - `ROW = "ground"` (а): линия земли между 20 % и 33 %, то есть выше низа ряда на обоих экранах; плиты фасадов стоят на тротуаре `#E8DCCB`;
     - `ROW = "sky"` (б): как лист A1b — фасады на небе, земля ниже ряда.
     - Кодер ставит `ground`. Вариант б оркестратор рендерит правкой этой строки в копии.
   - **Низ, 42–100 %** — улица: тротуар, газон, кусты, фонарь или ограда на выбор кодера.
   - **Везде нельзя:**
     - мест, дверей, окон, вывесок, в которые «можно войти» (урок № 27, TOWN-A1.md:20-22);
     - текста (шрифт в LICENSES.md:36 покрывает только вывески `bg_*` мест);
     - жителей и животных, замков;
     - красных и малиновых круглых вещей: они читались значком HUD (`place.py:29-31`), а на улице так выглядит «!» (`StreetScreen.kt:90`);
     - плиток, похожих на Match3, и полос, похожих на панель UI (TOWN-A1c.md:291).
4. **Пекарня.**
   - Доска — кремовая, как на одобренном листе: `board(EAVE + 0.3, "#FFF3D6", soft(c["counter"]))` вместо `c["board"]` в :131, с комментарием «board of the approved sheet № 38 (place.py before A1c)».
   - Под `if MARKS:` — крупная примета без текста, крендель-вывеска из примитивов `lib.py` `torus`/`cylinder`/`sphere` (у них origin в центре); `curve` не использовать — у кривой origin в мировом нуле, и приёмка п. 7 её не меряет. Тон корки `#C9803F` (:134).
     - Форма — петля с двумя просветами, как `pastry_pretzel` в контракте 1b3 (TOWN-J1-1b.md:424); на общем листе оба кренделя в одном ряду К4.
     - Место — верхние ≈ 30 % кадра слева или кронштейн слева.
     - Критерий — IoU силуэтов (приёмка п. 6).
     - Судья замерил: чтобы IoU с домом стал ≤ 0,93, пекарне нужно ≈ 6 900 px силуэта вне маски дома (≈ 83 × 83 px).
     - Свободен левый верхний угол (x < −0,9, z > EAVE) или кронштейн слева. Правый верхний угол (x > 0,95, z > 2,9) в A1g2 закрыт значком «!» (`StreetScreen.kt:90`, событие пекарни `job_bakery_help` — с недели 1, content.json:2180); проверка — приёмка п. 7.
   - Караваи, труба и навес остаются. `place.PLACES` не менять — это контракт 1b3.
5. **Приметы калиток** (под `if MARKS:`, по образцу лапы зоопарка :187-191): парк — цветок, лес — гриб (сфера и цилиндр) на доске, ветвью по `g["tree"]` в `gate()`. Дерево, забор, калитку и цвета `GATES` (:22-24) не менять.
   - Гриб — боровик без точек: шляпка `#8D5A3B` (`facade.py:23`) или `#C9803F` (:129), ножка `#FFF3D6` (:22). Цветок — лепестки `#FFC94D` (:24), серединка `#C9803F`. Красных и розовых (`#FF6F91`, `#FFD6E4`) на приметах нет.
6. **Константа `MARKS`.** Одна строка `MARKS = True  # № 71/71а: True — приметы (а), False — как лист № 38 (б)`. При `False` фасады совпадают с BASE, кроме доски пекарни из п. 4.
7. **Имена новых объектов** фасадов — те, которых нет в BASE-фасаде: `pretzel*`, `emblem*`. Совпавшее имя сдвигает суффиксы `.001` у старых объектов, и дамп покажет их изменёнными.
8. **Имена файлов.**
   - `--all` пишет `fac_<placeId>`: `"fac_%s.png" % fid.removeprefix("gate_")`. Получается `fac_home`, `fac_market`, `fac_foma`, `fac_bakery`, `fac_park`, `fac_forest`, `fac_zoo`.
   - Ключи `GATES`/`FACADES` и `--place gate_*` не меняются: регресс BASE вызывает `--place gate_park`.
9. **Не меняются:**
   - функции `home()`, `market()`, `foma()`;
   - геометрия `gate()` вне п. 5;
   - общие функции `soft`, `white`, `ground`, `walls`, `window`, `door`, `gable`, `board`, `render_facade`;
   - константы `ORTHO/MID/ELEV/EAVE`, кадр 384 × 384 RGBA, свет `studio(scale=2.0)`;
   - флаги `--place/--out/--all/--samples`.
   - Всё это проверяется машинно, приёмка п. 2.
10. **Докстринг** (:1-13): строка запуска `--street`, зоны фона из п. 3, `ROW` и `MARKS`, правило имён `fac_<placeId>` и `bg_street_port`.

На выбор кодера (судят лист и ворота): облака ниже 20 %, кусты, фонари, форма тротуара и холмов, пропорции кренделя,
форма цветка и гриба.

## SCOPE
**Вариант A (основной).** Файл `wt/a1g/.claude/task-scope.json` пишет оркестратор. Файл в `.gitignore:4`, поэтому worktree его не получает. Пути — от корня worktree:
```json
{"task": "TOWN-A1g1 — фон улицы и приметы фасадов (facade.py), пилот № 61 б (docs/tasks/TOWN-A1g1.md)",
 "base": "<sha BASE>",
 "allow": ["finny-pet/tools/art/facade.py"],
 "protect": [".claude/", "CLAUDE.md", "README.md", ".gitignore", ".gitattributes", "docs/", "finny-pet/.gitignore",
  "finny-pet/docs/", "finny-pet/app/", "finny-pet/screenshots/", "finny-pet/assets/", "finny-pet/release/",
  "finny-pet/README.md", "finny-pet/CHANGELOG.md", "finny-pet/keystore.properties.example",
  "finny-pet/build.gradle.kts", "finny-pet/settings.gradle.kts", "finny-pet/gradle/", "finny-pet/gradle.properties",
  "finny-pet/gradlew", "finny-pet/gradlew.bat",
  "finny-pet/tools/art/lib.py", "finny-pet/tools/art/place.py", "finny-pet/tools/art/props.py",
  "finny-pet/tools/art/to_webp.py", "finny-pet/tools/art/room.py", "finny-pet/tools/art/pet.py",
  "finny-pet/tools/art/uiprops.py", "finny-pet/tools/art/import_sprites.py", "finny-pet/tools/art/sounds.py",
  "finny-pet/tools/art/smoke.py", "finny-pet/tools/art/montserrat_extrabold.ttf",
  "finny-pet/tools/ui.py", "finny-pet/tools/demo_run.sh", "finny-pet/tools/office/",
  "tools/art_check.py", "tools/sheets.py", "tools/perf.sh", "tools/adbui.sh", "tools/emu.sh", "tools/ui_measure.py",
  "tools/mutation_probe.py", "tools/content_map_events.py", "tools/town_route.sh", "tools/bakery_states.sh",
  "tools/rec.sh", "tools/mock_bakery.py"]}
```
- protect покрывает все отслеживаемые файлы `git ls-tree -r HEAD`, кроме `facade.py` (проверка — «До спавна» п. 6).
- Каталогов `tools/`, `finny-pet/tools/`, `finny-pet/tools/art/` в protect нет. Guard сверяет хвосты от двух сегментов подстрокой (`guard-paths.js:139-146`), а признак записи — любой `>` (:122-123), включая `2>&1` (WORKFLOW № 29).

**Вариант B (запасной — только если отдельную сессию в `wt/a1g` открыть нельзя).**
- Общий скоуп основного дерева по правилу хвостов 1b3 (TOWN-J1-1b.md:400-412): в allow добавляется `wt/a1g/finny-pet/tools/art/facade.py`.
- Изоляция проверяется только через `git -C wt/a1g status`: стоп-хук не видит `wt/`.
- Отступление от № 61 б сообщить владельцу.

**Кодеру, в обоих вариантах:**
- команды с путями из protect — без `>` и `2>&1`;
- вывод Blender — в `$T` через `tee` или без перенаправления; рендеры — только в `$T`;
- лист `a1b_style_c_facade.jpg` читать инструментом Read.

## ANTI-SCOPE
- Ассеты в `res/`, LICENSES, любой Kotlin — всё это A1g2.
- `lib.py` (заморожен, № 61 б), `place.py`, `props.py`, `to_webp.py`, `tools/`.
- `put("pastry_*")` до слияния 1b3.
- Изменение одобренных дома, рынка, «У Фомы», зоопарка. Небо и плита рынка меняются только по ответу № 74 б, отдельным кругом A1g1-2.
- Плитки фона: по № 30 — только если на S23 видно скольжение, это запись в A1g2.
- Замок, открытая калитка, маска фасада (срез 2, TOWN-A1.md:77-78).
- Текст и места на фоне.

## БЮДЖЕТ
- **`facade.py`: вставок ≤ 90, удалений ≤ 8** — по столбцам `git diff -w --numstat BASE`.
  - Не `grep -c '^-[^-]'`: он не видит пустых строк и строк на «-».
  - Разбивка вставок, итого ≈ 85:

    | Что | Строк |
    |---|---|
    | `street()` и `ROW` | ≈ 40: небо 1–2, холмы 3–4, земля, тротуар, газон, бордюр 4–6, деревья и кусты 4–8, фонарь или ограда 3–5, облака 2 |
    | `render_street` | ≈ 8 |
    | CLI | ≈ 4 |
    | `--all` | 1 |
    | доска пекарни | 1 |
    | крендель | ≈ 10 |
    | приметы парка и леса | ≈ 10 |
    | `MARKS` | 1 |
    | докстринг | ≈ 8 |

  - Удаления — только эти строки: доска :131, `--all` :215, `ap.add_argument` :210–211 и `ap.error` :219 (≤ 2), докстринг (≤ 3).
- **Инструменты до спавна** (коммит спеки, оркестратор):
  - `tools/art_check.py` +2/−2: регресс 7 фасадов, строка докстринга :5;
  - `docs/tasks/TOWN-A1.md` +3: доли;
  - `docs/HANDOFF.md:111` +1/−1;
  - `docs/tasks/TOWN-J1-1b.md`: :46 и :417 — ссылка на строку «≈ 70 КБ» в TOWN-J1.md (`TOWN-J1.md:303` → `:313`, `grep -n "≈ 70 КБ" docs/tasks/TOWN-J1.md` на момент коммита BASE); :17 — ссылка на ответ 7 концепции (`TOWN-J1.md:305` → `:315`).
- **Доля A1g в бюджете APK** (записывается в TOWN-A1 до спавна, TOWN-J1-1b.md:417):
  - Формула: Σ WebP 7 фасадов (замер на BASE) + 61 440 (потолок фона, TOWN-A1.md:64) + 6 000 (dex, arsc и заголовки zip, TOWN-A1f.md:139).
  - Замер на HEAD, q88/α90/m6, 384 px: home 11 262, market 14 446, foma 11 574, bakery 13 346, park 23 064, forest 23 122, zoo 27 998 — итого **124 812 Б**.
  - Доля = 124 812 + 61 440 + 6 000 = **192 252 Б**. Это 27,0 % от 710 847 Б: остаток эпика после A1f 780 847 (TOWN-A1.md:45) минус доля J1 ≤ 70 000 (TOWN-J1.md:313).
  - После J1 и A1g на A1s, A1d, A1e, A1h и код остаётся 518 595 Б.
  - Сумма 8 WebP ≤ 186 252 Б. Приметы № 71 утяжеляют три фасада — это покрывает запас фона: фоны мест весят 38 578–45 392 Б при потолке 61 440.
  - Превышение — вопрос оркестратору; quality 88 молча не снижать. `--size 360` невыгоден: калитки тяжелеют, сумма 132 438 Б.
  - Риск: release на диске уже +32 984 Б к A1f — код J1 тоже тратит бюджет (из какого дерева собран, не проверено). Вопрос владельцу о бюджете эпика — к A1e (HANDOFF.md:57-58), а A1e идёт раньше A1g2 (TOWN-A1.md:49-51). Нового вопроса здесь нет.
- **Время:** регресс из 20 дампов — оценка 3–5 мин на прогон, не мерили. Приёмка — 2 регресса (d1, «вариант б») плюс рендеры ≈ 3 мин.

## ДО СПАВНА (оркестратор)
Правки protect основного дерева — одним коммитом и только когда кодер 1b1 или 1b2 не работает (HANDOFF.md:149-150); до
этого готовить их в scratch.
1. **`tools/art_check.py`.**
   - :109 — фасады по одному на прогон: `for f in ("home", "market", "foma", "bakery", "gate_park", "gate_forest", "gate_zoo")` → `facade_<f>` с `--place <f> --samples 1`. В дамп попадает только последняя сцена прогона (:110-114).
   - Докстринг :5: «facade.py (market)» → «facade.py (7 фасадов по одному)».
   - Попутно это усиливает приёмку 1b3: правка `PLACES/M/put/cloud` в `wt/j1b` покраснеет на `facade_*` — после rebase `wt/j1b` на BASE (п. 6).
2. **Доки в том же коммите.**
   - TOWN-A1.md, абзац пилота :56-60: доли J1 (≤ 70 000 Б) и A1g (формула раздела «Бюджет»). Число — замер HEAD 124 812 (раздел «Бюджет»): facade.py, place.py, lib.py и to_webp.py в коммите BASE не меняются, после коммита проверка `git diff --name-only BASE~1 BASE -- finny-pet/tools/art` → пусто.
   - HANDOFF.md:111 — список дампов: плюс `place_bakery`, props, 7 фасадов.
   - TOWN-J1-1b.md:17, :46, :417 — номера строк TOWN-J1.md на момент коммита (раздел «Бюджет»).
   - Сама спека `docs/tasks/TOWN-A1g1.md`.
   - Коммит → sha = **BASE**.
3. **BASE-дампы:** `mkdir -p $T/a1g/base && git archive BASE finny-pet/tools/art | tar -x -C $T/a1g/base`; `ls $T/a1g/base/finny-pet/tools/art/facade.py` → файл есть; `python tools/art_check.py regress $T/a1g/base/finny-pet/tools/art $T/a1g/d0` → 20 строк `… ok`, exit 0.
4. **Самопроверка регресса** — он должен уметь покраснеть (WORKFLOW № 27).
   - Копия `$T/a1g/mut` = копия base с двумя мутантами:
     - в `facade.py:24` табличка зоопарка `"#FFC94D"` → `"#FFFFFF"`;
     - в `place.py:45` стена пекарни `wall="#FFEBD2"` → `"#FFFFFF"`.
   - `python tools/art_check.py regress $T/a1g/mut/finny-pet/tools/art $T/a1g/dm`, затем `python tools/art_check.py diff $T/a1g/d0 $T/a1g/dm | grep -E 'разница: [1-9]|DUMP DIFF'` → ровно три файла и `DUMP DIFF: 3 file(s)`:
     - `facade_bakery.json`;
     - `facade_gate_zoo.json … ['sign']`;
     - `place_bakery.json`.
   - Второй мутант показывает связь с 1b3: `PLACES` → фасад.
   - Иначе — чинить регресс, не спавнить.
5. **Перепроверка доли на BASE и `fac0` для листа:**
   - `"$BL" -b --factory-startup --python-exit-code 1 -P $T/a1g/base/finny-pet/tools/art/facade.py -- --all $T/a1g/fac0` → exit 0, 7 файлов (`fac_gate_*` у калиток).
   - `python finny-pet/tools/art/to_webp.py $T/a1g/fac0 --dst $T/a1g/w0` → последняя строка `7 files, 124812 B` (число из п. 2; иное — вопрос оркестратору до спавна).
   - `fac0` — ещё и «до» для листа и для самопроверки п. 6 приёмки.
6. **Worktree.** `wt/a1g` и `wt/j1b` уже есть: коммит 6b2277d (профиль coder: Opus, xhigh, maxTurns 150), ветки `pilot/a1g` и `pilot/j1b`, база d35ac7c.
   - В каждом: `git -C wt/<x> status --porcelain -uall` → пусто, затем `git -C wt/<x> rebase BASE`.
   - Проверки: `git -C wt/a1g log --oneline BASE..HEAD` → одна строка `pilot(№ 61 б)`; `grep -c gate_park wt/j1b/tools/art_check.py` → ≥ 1 (регресс 7 фасадов из п. 1).
   - BASE для 1b3 (TOWN-J1-1b.md, ACCEPTANCE п. 1 «BASE-копия» и п. 6 numstat) — тот же sha.
   - ПЕРЕзаписать `wt/a1g/.claude/task-scope.json` из SCOPE: сейчас там base d35ac7c, путь `docs/tasks/TOWN-A1g.md` и неполный protect. В `wt/j1b/.claude/task-scope.json` — base = BASE.
   - Пересечение allow с protect и хвосты protect в allow (логика `variants`, `guard-paths.js:139-146`), из корня основного дерева:
     `python -c "import json;d=json.load(open('wt/a1g/.claude/task-scope.json'));v=lambda e:[e]+['/'.join(e.rstrip('/').split('/')[i:])+('/'*e.endswith('/')) for i in range(1,len(e.rstrip('/').split('/'))-1)];a=d['allow'];print([x for x in a for p in d['protect'] if x.startswith(p)],[t for p in d['protect'] for t in v(p.lower()) for x in a if t in x.lower()])"` → `[] []`.
   - Покрытие: `git -C wt/a1g ls-files` против protect → не покрыт только `finny-pet/tools/art/facade.py`.
7. **Снимок основного дерева** для изоляции: `git status --porcelain -uall -- tools finny-pet/tools > $T/a1g/main0.txt`. Bash-запись кодера через `../../` guard не ловит, он ловит только Edit/Write вне корня.
8. **Сессия Claude Code в каталоге `wt/a1g`.**
   - Механизм (проверено 2026-09-28): `claude -p` из Bash (встроенный Claude Code 2.1.281, `%APPDATA%\Claude\claude-code`)
     в каталоге worktree не стартует — «Not logged in» (авторизацию сессиям даёт приложение; своего входа у CLI нет);
     инструмента создания сессии у оркестратора нет. Сессию открывает владелец в приложении (папка `wt\a1g`, режим
     auto); задания и самотест оркестратор шлёт ей сообщением (`send_message`), отчёт она шлёт обратно.
   - Не проверено, записать в журнал:
     - значение `$CLAUDE_PROJECT_DIR`;
     - грузит ли сессия родительский CLAUDE.md второй раз (worktree лежит внутри корня);
     - нужна ли копия `.claude/settings.local.json`: она в `.gitignore:6`, без неё будут запросы разрешений; копировать или нет — решает владелец.
9. **Самотест хука зонд-агентом `agentType: 'coder'` из сессии worktree.**
   - Хук ограничивает только coder (`guard-paths.js:75`).
   - Каждая строка — ожидание и факт в журнал:

   | № | Действие зонда | Ожидание |
   |---|---|---|
   | 1 | Edit `finny-pet/tools/art/facade.py`: строка-комментарий в конец | пропуск |
   | 2 | Write `finny-pet/tools/art/place.py` | блок «не входит в allow» |
   | 3 | Write `<абс. корень основного дерева>/wt/probe_outside.txt` (для корня хука `wt/a1g` это `../probe_outside.txt`; `wt/` игнорируется, .gitignore:19) | блок «находится вне репозитория» (`guard-paths.js:101-102`) |
   | 4 | Bash `echo probe > "<абс. путь scratch сессии worktree>/a1g_probe.txt"` — путь буквально, его подставляет оркестратор в промпт зонда (`$T` в сессии зонда не задана) | пропуск |
   | 5 | Bash `echo -P finny-pet/tools/art/facade.py 2>&1 \| tail -1` | пропуск (регресс правила № 29) |
   | 6 | Bash `cat finny-pet/tools/art/place.py > /dev/null` | блок «shell-запись в защищённый путь» |
   | 7 | Bash `git -C . checkout -- finny-pet/tools/art/facade.py` | блок «операции с git-историей…» (:116-118) |
   | 8 | Сессия worktree до конца зонда оставляет незакоммиченную правку `docs/HANDOFF.md` | SubagentStop: «ORACLE INTEGRITY FAILED» с этим файлом (`assert-oracle-intact.js:53-66`), зонд возвращает BLOCKED |

   - Самопроверка самотеста: пункты 2, 3, 6, 7 и 8 обязаны дать блок. Если у зонда всё «пропуск», хук не подключён (WORKFLOW № 14).
   - Восстановление:
     - `git -C wt/a1g checkout -- finny-pet/tools/art/facade.py docs/HANDOFF.md`;
     - `git -C wt/a1g status --porcelain -uall` → пусто;
     - из корня основного дерева `git status --porcelain -uall -- tools finny-pet/tools | diff - $T/a1g/main0.txt` → пусто; `rm -f wt/probe_outside.txt`;
     - удалить `<абс. путь scratch сессии worktree>/a1g_probe.txt` (тот же буквальный путь, что в строке 4).
   - Хоть одна строка не совпала — вариант B или A1g1 после 1b3; отступление сообщить владельцу.
9а. **Два Blender на одном HIP.** Одновременно запустить `"$BL" -b --factory-startup --python-exit-code 1 -P finny-pet/tools/art/place.py -- --place market --out $T/c1.png --preview > $T/c1.log 2>&1 & p1=$!` и `"$BL" … -P finny-pet/tools/art/facade.py -- --place market --out $T/c2.png --samples 16 > $T/c2.log 2>&1 & p2=$!`, затем `wait $p1; echo rc1=$?; wait $p2; echo rc2=$?`.
   - Ожидание: `rc1=0 rc2=0`, в обоих логах `cycles device: HIP`; время каждого — в журнал.
   - Красный — кодер A1g стартует после кодера 1b3.
10. **Спавн** из сессии worktree: coder, Opus, effort xhigh, Blender 5.2 `-b --factory-startup --python-exit-code 1`.
    - В промпт: CONTRACT, SCOPE, правило № 29.
    - Итерации — только `--street $T/… --preview` и `--place <id> --samples 16`, в `$T`.
    - Финальные рендеры приёмки и листа — оркестратор, очередью с 1b3; параллельные итерации двух кодеров — по итогу п. 9а.

## ACCEPTANCE (оркестратор, из корня `wt/a1g`, Git Bash)
Обозначения: `$BL` — `C:/Program Files/Blender Foundation/Blender 5.2/blender.exe`, `$T` — scratch основной сессии. Команды без
`bc` — его в Git Bash этой машины нет.
1. **Изоляция.**
   - `git status --porcelain -uall` → ровно ` M finny-pet/tools/art/facade.py`.
   - `git diff -w --numstat BASE -- finny-pet/tools/art/facade.py` → I ≤ 90, D ≤ 8.
   - `git diff --name-only BASE` → ровно `.claude/agents/coder.md` (коммит профиля 6b2277d после rebase) и `finny-pet/tools/art/facade.py`.
   - `node .claude/hooks/assert-oracle-intact.js` → exit 0.
   - Из корня основного дерева: `git status --porcelain -uall -- tools finny-pet/tools | diff - $T/a1g/main0.txt` → пусто.
   - `grep -c '^MARKS = True' finny-pet/tools/art/facade.py` → 1; `grep -c '^ROW = "ground"' finny-pet/tools/art/facade.py` → 1; `grep -c '^SKY = ' finny-pet/tools/art/facade.py` → 1.
2. **Регресс.**
   - `python tools/art_check.py regress finny-pet/tools/art $T/a1g/d1` → 20 × `ok`.
   - `python tools/art_check.py diff $T/a1g/d0 $T/a1g/d1 | grep -E 'разница: [1-9]|DUMP DIFF'` → ровно `facade_bakery.json …`, `facade_gate_park.json …`, `facade_gate_forest.json …` и `DUMP DIFF: 3 file(s)`, exit 1 ожидаем.
   - Остальные 17 дампов — «разница: 0»: room, 6 питомцев, `place_market/foma/bakery`, `facade_home/market/foma/gate_zoo`, `props_*`. Любой иной файл — FAIL по п. 9 CONTRACT.
   - **Вариант б (№ 71):**
     - `rm -rf $T/a1g/nb && cp -r finny-pet/tools/art $T/a1g/nb && sed -i 's/^MARKS = True/MARKS = False/' $T/a1g/nb/facade.py && grep -c '^MARKS = False' $T/a1g/nb/facade.py` → 1;
     - `python tools/art_check.py regress $T/a1g/nb $T/a1g/d2 && python tools/art_check.py diff $T/a1g/d0 $T/a1g/d2 | grep -E 'разница: [1-9]|DUMP DIFF'` → ровно `facade_bakery.json … разница: 1 ['sign']` и `DUMP DIFF: 1 file(s)`;
     - для К4 (очередью с 1b3): `"$BL" -b --factory-startup --python-exit-code 1 -P $T/a1g/nb/facade.py -- --all $T/a1g/fac_nb` → exit 0.
3. **Связь с `place.py` и пилотом.**
   - По AST, чтобы комментарии (`# place.py bread` :134, `# place.day_light (tried)` :201) не считались: `python -c "import ast;t=ast.parse(open('finny-pet/tools/art/facade.py',encoding='utf-8').read());print(sorted({n.attr for n in ast.walk(t) if isinstance(n,ast.Attribute) and isinstance(n.value,ast.Name) and n.value.id=='place'}))"` → подмножество `['M', 'PLACES', 'cloud', 'put']`.
   - Самопроверка: та же команда на `$T/a1g/base/finny-pet/tools/art/facade.py` → `['M', 'PLACES', 'cloud', 'put']` — без `py` и `day_light` из комментариев.
   - `grep -c "pastry_" finny-pet/tools/art/facade.py` → 0.
4. **Рендеры** — очередью с 1b3.
   - `"$BL" -b --factory-startup --python-exit-code 1 -P finny-pet/tools/art/facade.py -- --all $T/a1g/fac` → exit 0, в выводе `cycles device: HIP`.
   - `ls $T/a1g/fac` → ровно `fac_bakery.png fac_foma.png fac_forest.png fac_home.png fac_market.png fac_park.png fac_zoo.png`.
   - `python tools/art_check.py bbox $T/a1g/fac/*.png` → 7 × OK: 384x384, углы α 0, bbox ≥ 2 px от краёв.
   - `… -P finny-pet/tools/art/facade.py -- --street $T/a1g/bg_street_port.png` → exit 0.
   - Самопроверка, после рендера: `python tools/art_check.py bbox $T/a1g/bg_street_port.png | grep -c "FAIL (нужен RGBA)"` → 1.
   - С `--preview` в `$T/a1g/prev.png` → 540 × 960.
   - Вариант № 70 б — своя копия, не `nb`: `rm -rf $T/a1g/row_sky && cp -r finny-pet/tools/art $T/a1g/row_sky && sed -i 's/^ROW = "ground"/ROW = "sky"/' $T/a1g/row_sky/facade.py && grep -c '^ROW = "sky"' $T/a1g/row_sky/facade.py` → 1; `… -P $T/a1g/row_sky/facade.py -- --street $T/a1g/bg_street_sky.png` → exit 0; `cmp -s $T/a1g/bg_street_port.png $T/a1g/bg_street_sky.png` → exit 1 (файлы различаются).
5. **Фон: формат, контраст, пересвет, отличимость.**
   - `python finny-pet/tools/art/to_webp.py $T/a1g/bg_street_port.png $T/a1g/bg_street_sky.png --rgb --dst $T/a1g/w`.
   - Инлайн-проверка `$T/a1g/street_check.py`; `lum` и `INK` — из `tools/art_check.py:31`, :139-144:
   ```python
   import sys; sys.path.insert(0, "tools"); from art_check import lum, INK
   import numpy as np; from PIL import Image
   def check(B):  # B: 1920 × 1080 × 3 int
       if B.shape != (1920, 1080, 3): return ["size %s" % (B.shape,)]
       L, Lk, bad = lum(B[0:386]), float(lum(INK)), []   # 0 … верх ряда при 1,3 (386 px): статус-бар, Hud1, «Улица»
       mean, p10 = (L.mean() + .05) / (Lk + .05), (np.percentile(L, 10) + .05) / (Lk + .05)
       if mean < 4.5 or p10 < 3.0: bad.append("contrast %.2f/%.2f" % (mean, p10))   # пороги art_check.bg :214-215
       for a, b in ((0, 31), (80, 100)):                                           # пересвет, как art_check.bg :217-219
           band = B[1920 * a // 100:1920 * b // 100]
           if ((band[..., 0] >= 250) & (band[..., 1] >= 250)).mean() > 0.05: bad.append("clip %d-%d" % (a, b))
       return bad
   im = Image.open(sys.argv[1]); B = np.asarray(im.convert("RGB")).astype(int)
   r = check(B) + ([] if im.mode == "RGB" else ["mode " + im.mode])
   M = B.copy(); M[200:260] = INK                   # мутант: тёмная полоса 60 px на месте заголовка
   assert check(M), "SELFCHECK FAIL: мутант не покраснел"
   print("STREET BG OK" if not r else "STREET BG FAIL %s" % r); sys.exit(bool(r))
   ```
   - `python $T/a1g/street_check.py $T/a1g/w/bg_street_port.webp` и то же для `bg_street_sky.webp` → `STREET BG OK`, exit 0.
   - Автор черновика прогнал скрипт на `bg_market_port`, `bg_foma_port`, `room_port_day`: OK, мутант краснеет.
   - Отличимость для `which` в A1g2 (полоса статус-бара, `art_check.py:259-282`):
     - `python tools/art_check.py which $T/a1g/bg_street_port.png --expect bg_street_port --bgdir $T/a1g/w` → exit 0.
     - Самопроверка: `python -c "from PIL import Image; Image.open('finny-pet/app/src/game/res/drawable-nodpi/bg_market_port.webp').save('$T/a1g/mk.png')"`, затем `… which $T/a1g/mk.png --expect bg_street_port --bgdir $T/a1g/w` → `bg_market_port … FAIL, expected bg_street_port`, exit 1.
6. **Форма, а не цвет** (инлайн PIL; ТЗ 3.6):
   ```
   python - "$T/a1g/fac" <<'EOF'
   import sys, os, itertools, numpy as np; from PIL import Image
   d = sys.argv[1]; ids = ["home", "market", "foma", "bakery", "park", "forest", "zoo"]
   p = lambda i: f"{d}/fac_{i}.png" if os.path.exists(f"{d}/fac_{i}.png") else f"{d}/fac_gate_{i}.png"  # fac0 (BASE) пишет fac_gate_*
   A = {i: np.asarray(Image.open(p(i)).getchannel("A")) > 127 for i in ids}
   iou = [(a, b, round(float((A[a] & A[b]).sum() / (A[a] | A[b]).sum()), 3)) for a, b in itertools.combinations(ids, 2)]
   bad = [x for x in iou if x[2] > 0.93]
   print("SILHOUETTE OK" if not bad else f"SILHOUETTE FAIL {bad}", iou); sys.exit(bool(bad))
   EOF
   ```
   - Ожидание: `SILHOUETTE OK`, 21 число идёт на лист (К4).
   - Порог 0,93 — не выше самой похожей пары разных мест на HEAD: foma–bakery 0,927. Пекарня должна отличаться от дома хотя бы как лавка.
   - Самопроверка: тот же скрипт на `$T/a1g/fac0` → `SILHOUETTE FAIL [('home', 'bakery', 0.989)]`, exit 1. Прогон на fac160 (рендер HEAD): все 21 пары, кроме home–bakery, ≤ 0,927; калитки между собой 0,746–0,819.
7. **Зоны карточки** — по дампу, а не по пикселям: разница рендеров несёт отражённый свет от новых объектов.
   - Изменённые и новые объекты фасадов не заходят в зону жителя: |x| < 0,62 и z < EAVE.
   - Откуда числа: EAVE 2,8 — `facade.py:20`, верх стен, выше — верхние ≈ 30 %. Внутренний край рамки окна дома и пекарни: −1,15 + (0,9 + 0,16)/2 = −0,62 (:46, :84, :133).
   - Скрипт `$T/a1g/zones.py`:
   ```python
   import json, sys, itertools, numpy as np
   EAVE, HALF = 2.8, 0.62
   def rot(r):  # euler XYZ Blender: Rz·Ry·Rx
       (cx, sx), (cy, sy), (cz, sz) = [(np.cos(a), np.sin(a)) for a in r]
       return (np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]) @ np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
               @ np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]))
   def intrude(A, B, bang=True):   # bang: зона «!» — только у карточки места (у калиток значка нет, StreetScreen.kt:98-107)
       out = []
       for k, e in B.items():
           if k.startswith("__") or A.get(k) == e or e["type"] not in ("MESH", "CURVE"): continue
           if e["type"] == "CURVE": out.append(k + ":curve"); continue   # origin кривой в мировом нуле (lib.curve): коробку не строим, fail-closed
           c = np.array(list(itertools.product(*[(-d / 2, d / 2) for d in e["dims"]]))) @ rot(e["rot"]).T + np.array(e["loc"])
           if (c[:, 2].min() < EAVE and c[:, 0].min() < HALF and c[:, 0].max() > -HALF)               or (bang and c[:, 0].max() > 0.95 and c[:, 2].max() > 2.9): out.append(k)   # зона жителя или зона «!» (StreetScreen.kt:90)
       return out
   d0, d1, bad = sys.argv[1], sys.argv[2], []
   for f in ("bakery", "gate_park", "gate_forest"):
       A, B = (json.load(open(f"{d}/facade_{f}.json", encoding="utf-8")) for d in (d0, d1))
       bad += [f"{f}:{k}" for k in intrude(A, B, bang=f == "bakery")]
   A = json.load(open(f"{d0}/facade_bakery.json", encoding="utf-8"))   # мутанты — на дампе пекарни
   for m in ({"type": "MESH", "loc": [0, -0.2, 1.0], "rot": [0, 0, 0], "dims": [0.3, 0.3, 0.3]},    # низ по центру
             {"type": "MESH", "loc": [1.5, -0.2, 3.5], "rot": [0, 0, 0], "dims": [0.3, 0.3, 0.3]},    # угол «!»
             {"type": "CURVE", "loc": [0, 0, 0], "rot": [0, 0, 0], "dims": [0.1, 0.1, 0.1]}):
       assert intrude(A, dict(A, mutant=m)) in (["mutant"], ["mutant:curve"]), "SELFCHECK FAIL %s" % m
   print("ZONES OK" if not bad else f"ZONES FAIL {bad}"); sys.exit(bool(bad))
   ```
   - `python $T/a1g/zones.py $T/a1g/d0 $T/a1g/d1` → `ZONES OK`.
   - Самопроверка встроена: три мутанта на дампе пекарни — в центре низа, в углу «!» и кривая — каждый обязан попасть в список. Зона «!» проверяется только у пекарни: у строки калитки значка события нет (`StreetScreen.kt:98-107`), цветок на правой половине доски — не нарушение.
   - Доска пекарни (`sign`, z 3,1 ± 0,25) проходит: её низ 2,85 ≥ EAVE, а x ≤ 0,9 — вне зоны «!».
8. **Состав сцены фона** — нет мест, дверей, текста, товаров.
   - `DUMP_OUT=$T/a1g/d1/street.json "$BL" -b --factory-startup --python-exit-code 1 -P finny-pet/tools/art/facade.py --python tools/art_check.py -- --street $T/a1g/sp.png --preview` → `DUMPED`.
   - Затем `$T/a1g/street_dump.py`:
   ```python
   import json, sys
   d = json.load(open(sys.argv[1], encoding="utf-8")); v = d["__view__"]; bad = []
   if v["film_transparent"] or v["color_mode"] != "RGB" or v["aspect"] != 0.5625: bad.append("view %s" % v)
   for n, e in d.items():
       b = n.split(".")[0]
       if n.startswith("__"): continue
       if e["type"] == "FONT" or b in ("sign", "sign_rim", "sign_text", "door", "door_top", "knob", "step") \
          or b.startswith(("win_", "item_", "pastry_", "tile_", "goal_", "ui_")):
           bad.append(n)
   print("STREET SCENE OK" if not bad else "STREET SCENE FAIL %s" % bad[:8]); sys.exit(bool(bad))
   ```
   - `python $T/a1g/street_dump.py $T/a1g/d1/street.json` → `STREET SCENE OK`.
   - Самопроверка: `python $T/a1g/street_dump.py $T/a1g/d1/facade_home.json` → `STREET SCENE FAIL` (view, `door`, `win_*`), exit 1.
9. **WebP и доля.**
   - `python finny-pet/tools/art/to_webp.py $T/a1g/fac --dst $T/a1g/w` → 7 строк RGBA.
   - `stat -c '%s %n' $T/a1g/w/fac_*.webp | awk '$1 > 37888'` → пусто.
   - `stat -c %s $T/a1g/w/bg_street_port.webp` → ≤ 61 440.
   - `stat -c %s $T/a1g/w/fac_*.webp $T/a1g/w/bg_street_port.webp | awk '{s += $1} END {print NR, s}'` → `8 S`, S ≤ 186 252.
   - Превышение — вопрос оркестратору; quality 88 молча не снижать.
10. **Докстринг:**
    - `grep -c -- "--street" finny-pet/tools/art/facade.py` → ≥ 2 (запуск и CLI);
    - `grep -c "bg_street_port" finny-pet/tools/art/facade.py` → ≥ 1;
    - `grep -c "fac_<placeId>" finny-pet/tools/art/facade.py` → ≥ 1.
11. **Общий лист** — кадры в разделе вопросов; судьи — после листа, до ворот.
    - **«Ребёнок» вслепую** (WORKFLOW № 35).
      - Материал: 7 вырезок фасадов без текста с нейтральными метками вперемешку с BASE-пекарней и BASE-калитками парка и леса (`fac0`).
      - Вопросы: «что это за место? где купить хлеб? где твой дом? можно ли сюда сейчас войти? страшно ли?».
      - Самопроверка: силуэты дома и пекарни, залитые одним серым по α > 127 (`fac0`), — линза не должна различать их лучше случайного.
      - BASE-вырезки — материал, а не самопроверка: BASE / `MARKS=False` / `MARKS=True`.
    - **Доступность:**
      - серый и дейтеранопия по К4 — судья доступности, не машинно; машинно форма проверяется в п. 6 (IoU ≤ 0,93, 7 фасадов);
      - шов неба рынка — число ΔRGB на лист (К5);
      - всё полупрозрачное поверх нового фона — плашки α 0,92 / 0,85 (WORKFLOW № 30), на К2.

**Слияние** (после ответов № 70, 71, 71а, 74; при ответах б — после круга A1g1-2):
- Оркестратор коммитит в `wt/a1g` принятые значения `ROW`/`MARKS`/`SKY`.
- На feat/town: `git checkout pilot/a1g -- finny-pet/tools/art/facade.py` и коммит `A1g1: …`. Ветку не мержить, `coder.md` (6b2277d, «в feat/town не сливается») не переносить.
- Тем же коммитом на feat/town в `art_check.py` добавляется запись регресса `("street", "facade.py", ["--street", <out>, "--preview"])` — те же флаги, что в п. 8.
- `$T/a1g/d1` — дампы ПРИНЯТОЙ версии: после фиксации `ROW`/`MARKS`/`SKY` перегнать `regress` в d1 и повторить команду ACCEPTANCE п. 8 с `DUMP_OUT=<d1>/street.json` (`regress` до слияния записи street не содержит). Абсолютный путь — в HANDOFF; хранить до второго слияния. Перенос d1 в `wt/a1g_d1` (игнорируется, .gitignore:19) — по желанию, как защита от чистки Temp.
- После обоих слияний (1b3 и A1g): `python tools/art_check.py regress finny-pet/tools/art $T/m` и `python tools/art_check.py diff $T/a1g/d1 $T/m` → `DUMP DIFF EMPTY`.
  - Равны принятым дампам A1g все фасады, фон улицы, места, питомцы и props.
  - Это и есть «`facade_market` — только правки A1g» (TOWN-J1-1b.md:477), без дыры E9 (:694-695).

## ВОПРОСЫ НА ВОРОТА АРТА
Продолжают §18 с № 70. Общий лист арта пилота с 1b3: № 65–68 и № 70–74 (с № 71а), ответ одной пачкой. Кадры A1g на листе:
- **К1** — фон целиком, оба варианта `ROW`, 360 × 640 и кадр S23 (×1,219, срез 118 px). Рядом «сейчас»: `emu_j1a2e_street.png`, `emu_j1a2s_street.png`.
- **К2** — временная сборка в превью-worktree (HANDOFF.md:145), не коммитится.
  - `bg_street_port.webp` в res и ветка `is Screen.Street -> R.drawable.bg_street_port` в `placeBackground` (`GameApp.kt:242-246`, 1 строка).
  - Улица недели 1 при 1,0 и 1,3 на 360 × 640 и S23, плюс после свайпа (Боря). Шрифт 1,3 — `tools/adbui.sh font 1.3`: `town_route.sh` снимает улицу только `shot1` при 1,0 (:41, :116, :119).
  - Без звука; громкость и шрифт вернуть; S23 перед прогоном попросить разблокировать.
  - Машинно: `python tools/art_check.py which <снимки> --expect bg_street_port --bgdir $T/a1g/w`.
- **К3** — макет целевой улицы A1g2 (PIL-композит на кадрах К2, подпись «макет, не сборка»).
  - Фасады 1:1 вместо белых плашек, жители 64 dp внизу по центру, «!»; подписи — на подложке White α 0,85 под фасадом, как на листе № 38.
  - Карточка рынка с «!» (событие `job_market_help`, неделя 1, content.json:2198) — в цвете, в сером и при дейтеранопии; судье доступности вопрос «где на карточке значок события?».
  - Дом — оба варианта № 73; калитки — варианты № 72.
- **К4** — 7 фасадов 1:1 (360 px = 120 dp) и ×2 на новом фоне в полосе ряда, в сером и при дейтеранопии.
  - Пекарня и калитки парка и леса: BASE / `MARKS=False` / `MARKS=True`.
  - В одном ряду с пекарней — `pastry_pretzel` 1b3 в том же масштабе (оба кренделя рядом).
  - IoU из приёмки п. 6.
- **К5** — карточка рынка ×2 на новом небе, число ΔRGB панели и неба фона:
  - а — рендер `fac/fac_market.png` на `bg_street_port`;
  - б — PIL-макет (подпись «макет»): панель неба фасада перекрашена в средний пиксель неба фона — рендера б до ответа нет (ANTI-SCOPE: `facade_market` меняется только по № 74 б кругом A1g1-2);
  - в — своя копия: `rm -rf $T/a1g/sky74 && cp -r finny-pet/tools/art $T/a1g/sky74`, строку `^SKY = ` подменить значением ≈ G.sky (111,177,224) `sed`-ом с проверкой `grep -c` → 1, `"$BL" … -P $T/a1g/sky74/facade.py -- --street $T/a1g/bg_street_sky74.png` → exit 0, средний пиксель полосы 0–20 % — числом на К5; допуск «≈» — решает владелец по листу (TODO, не критерий приёмки).
- **К6** — ответы «ребёнка» (приёмка п. 11).

70. **Композиция улицы** (К1–К3):
    - а) ряд на земле — линия земли ≤ 33 %, плиты сливаются с тротуаром, домики «стоят» (`ROW = "ground"`);
    - б) ряд на небе, как одобренный лист `a1b_style_c` — земля ниже ряда, домики на плитах «висят» (`ROW = "sky"`);
    - в) ни один — ещё круг `street()` по замечаниям (A1g1-2, повторно — только № 70).

    Риск а: при 1,3 и на 360 × 640 низ ряда ниже, полоса земли над ним толще.
    **Рекомендую а**, если на К3 при 1,0, при 1,3 и на S23 низ фасада (верх карточки с К2 + 120 dp) лежит на тротуаре или ниже линии земли; иначе б.
71. **Крендель пекарни** (К4, К6):
    - а) крендель-вывеска пекарни (`MARKS = True`);
    - б) как одобрено № 38 — у пекарни только своя кремовая доска.

    Цена а: меняется одобренный фасад пекарни.
    **Рекомендую а**, если пекарню с кренделем «ребёнок» назвал, а BASE — нет; если BASE тоже назвал — б (примета не нужна).
71а. **Цветок и гриб на калитках** (К4, К6):
    - а) да — цветок на доске парка, гриб-боровик на доске леса;
    - б) как на № 38 — парк и лес без примет.

    Цена а: меняются два одобренных фасада калиток. `MARKS` — одна константа на обе приметы; при ответах 71 а и 71а б (или наоборот) — малый круг A1g1-2.
    **Рекомендую б**, если «ребёнок» называет парк и лес на BASE; иначе а.
72. **Калитки на улице** (К3; делается в A1g2):
    - а) строка: слева фасад калитки 64 dp вместо нынешнего 🔒 в тексте (`StreetScreen.kt:99`), картинка мечты и «n / N» (№ 25); в видимом тексте нет «🔒»; меняет подпись листа № 38 «замок — оверлей»;
    - б) то же плюс `ui_lock` оверлеем, как в строке эпика (TOWN-A1.md:46, «замок оверлеем», «🔒 → `ui_lock`»);
    - в) калитки карточками в ряду после мест.

    Риск б: `ui_lock` уже значит «Взрослым» (`TownUi.kt:202`, `StartScreens.kt:78`) — одна картинка в двух смыслах, и замок у мечты может читаться как «нельзя».
    Риск в: мечты уходят за край ряда при 360 dp, без свайпа их не видно.
    **Рекомендую а**: наименьшая правка, число мечты на первом экране, мишень ≥ 64 dp уже есть (`StreetScreen.kt:101`). Цена — меняются строка A1g в TOWN-A1.md:46, ARCHITECTURE.md:187 и докстринг `facade.py:11` (ДОКИ A1g2).
    TalkBack: «Калитка в парк закрыта: мечта „…“, 0 из 150».
73. **Карточка дома** (К3; A1g2):
    - а) только фасад, низ по центру пуст;
    - б) фасад и свой питомец 64 dp внизу по центру, как житель у мест (`facade.py:10`; `PetSprite`, как в комнате).

    **Рекомендую б**: «мой дом» ребёнок узнаёт по своему питомцу, а не по подписи.
74. **Рынок на новом фоне** (К5):
    - а) оставить, как одобрено № 38: панель неба `#6FB1E0` (`facade.py:94`) рендерится в (83,116,154) при небе фона ≈ (130,193,240), плита — цвет пола рынка (:93);
    - б) панель неба в тон неба фона и плита в тон тротуара (меняется дамп `facade_market`, малый круг A1g1-2);
    - в) небо фона подогнать так, чтобы ПИКСЕЛЬ рендера в полосе 0–20 % был ≈ G.sky (111,177,224), как фон листа № 38 (`GameTheme.kt:36`); фасад рынка и его дамп не меняются; ΔRGB панели и неба — числом на К5 (правка константы `SKY`).

    **Рекомендую б**, если на К5 видна «картина в рамке», которую A1b снимал (`facade.py:89-91`); иначе а.

Решения спеки без вопроса — сообщить владельцу вместе с листом:
- доска пекарни кремовая, как на одобренном листе № 38 — это возврат дрейфа от A1c, а не новое решение;
- имена `bg_street_port` и `fac_park/forest/zoo` — по правилу TOWN-A1.md:68;
- фон без текста, мест и новых цветов;
- фасады остаются 384 px;
- лист арта общий с 1b3: если A1g1 не принят к воротам арта, лист идёт без фасадов (TOWN-J1-1b.md:40, :460), отступление сообщить.

По неподвижному листу не решить: скольжение карточек по небу (№ 30) и плавность — это запись на S23 в A1g2 (WORKFLOW № 31).

## A1g2 — встройка (набросок)
После A1e, по очереди (TOWN-A1.md:49-51); контракт — по ответам № 70–74.
- **Контракт** (`game/`, ≤ 30 вставок; test-author не нужен, TOWN-A1.md:69):
  - `placeBackground`: `Screen.Street` → `R.drawable.bg_street_port` (`GameApp.kt:242-246`).
  - `facadeRes(placeId)` — `when` с явными `R.drawable.fac_*` (R8, TOWN-A1.md:66-67).
  - `PlaceCard` (`StreetScreen.kt:77-94`): фасад 120 dp вместо белой плашки α 0,92, житель 64 dp поверх низа по центру; подписи (имя, название) — на подложке White α 0,85 под фасадом, как на листе № 38, контраст G.ink ≥ 4,5 : 1 (TOWN-A1.md:72) — в приёмке A1g2 по снимкам; «!» и `contentDescription` (:83) как сейчас; дом — по № 73.
  - `Gate` — по № 72. Id места калитки: `places.first { it.opensBy.goal == g.id }.id`, потому что в `Gate` приходит `TownGoal`, а не `Place` (:55, :98).
- **До спавна:**
  - `town_route.sh` снимает улицу при 1,3 и на неделе 5;
  - зонд `art_check.py facades --selfcheck` по образцу `residents` (:20-22): id мест = ветки `facadeRes` = файлы `fac_*`.
- **Приёмка:**
  - aapt2 по release (TOWN-A1c.md:194-197);
  - `which --expect bg_street_port`;
  - листы 360 × 640 и S23 при 1,0 / 1,3, недели 1 и 5;
  - мишени ≥ 48 dp; `perf.sh` при прокрутке ряда;
  - запись S23 — видно ли скольжение (№ 30);
  - прирост APK ≤ доли A1g.
  - Оценка, не замер: карточка с фасадом 120 dp ≈ 197 dp при 1,0 и ≈ 213 dp при 1,3 против 170 dp на макете № 38 — мерить.
- **Доки:**
  - LICENSES: строка «Улица: фон и фасады | `bg_street_port`, 7 `fac_*` | `facade.py --street` / `--all` → `to_webp.py` (фон `--rgb`)» и итог «52 файла» (LICENSES.md:58, ARCHITECTURE.md:28) — от числа на момент коммита;
  - BACKLOG.md:82 (п. 12), LIMITATIONS_ROADMAP.md:28 (п. 16);
  - комментарий `StreetScreen.kt:60`;
  - TOWN-A1.md:46;
  - при № 72 а — ещё ARCHITECTURE.md:187, докстринг `facade.py:11` и grep `git grep -n "замок — оверлей\|замок оверлеем\|ui_lock is an overlay\|оверлей ui_lock" -- docs finny-pet/docs finny-pet/tools ':!docs/tasks/TOWN-A1g1.md'` → только исторический TOWN-A1b.md:56;
  - GAME_CONCEPT.md:1803 («без нового кода») расходится с TOWN-A1.md:66-67 (ветка `when`).

## ДОКИ (после приёмки A1g1; что станет ложью)
- `finny-pet/docs/ARCHITECTURE.md:187` — строка `facade.py`: добавить «и фон улицы `bg_street_port` 1080 × 1920 RGB (`--street`)», имена `fac_<placeId>`.
- `docs/tasks/TOWN-A1.md`:
  - :46 — статус A1g: «A1g1 сделано (генератор), A1g2 — встройка»;
  - :68 — `bg_street_port` (у улицы нет `placeId`), калитки — `fac_<placeId>`.
- `docs/tasks/TOWN-J1-1b.md:40`, :460 — «фасадов A1g к приёмке 1b3 нет — лист без них»: ложь, если A1g1 принят до ворот арта. Заменить на «лист общий: № 65–68 и № 70–74 (с 71а)».
- `docs/HANDOFF.md` — строка пилота и путь `$T/a1g/d1`.
- `docs/GAME_CONCEPT.md` §18 — № 70–74 с ответами после ворот.
- **Грепы** по обоим `docs/` и по `tools/`, `finny-pet/tools/`:
  - `grep -rn --exclude=TOWN-A1g1.md "fac_gate_" docs finny-pet/docs tools finny-pet/tools` → пусто. Историческая `docs/tasks/TOWN-A1b.md:55` называет ключи `--place`, там верно, её не трогать.
  - `grep -rn --exclude=TOWN-A1g1.md "facade.py (market)\|facade market" tools docs` → только исторический `TOWN-A1c.md:184`. HANDOFF.md:111 и `art_check.py:5` исправлены в коммите спеки.
  - `grep -rn --exclude=TOWN-A1g1.md "лист без них" docs` → пусто.
- BACKLOG.md:82 и LIMITATIONS_ROADMAP.md:28 («улица — на фоне комнаты») остаются верными до A1g2.

## ПРИ БЛОКЕРЕ
`STATUS: BLOCKED` с одним вопросом и остановка, если:
- нужен цвет, которого нет ни в `PLACES`, ни в `facade.py` (п. 2);
- нужен примитив, которого нет в `lib.py`: плоскость, текстура, градиент;
- нужны `place.day_light`, `place.riverside`, `props.fit_camera` или `put` в фоне;
- «Улица» не набирает контраст п. 5 ни при одном `ROW`;
- IoU дом–пекарня ≤ 0,93 не достигается без правки общих функций (п. 9) или без выхода из зон приёмки п. 7;
- регресс даёт разницу вне трёх заявленных дампов;
- сумма WebP > 186 252 Б.

Не изобретать и не трогать protect.

## Журнал спеки
- **2026-09-28, синтез судьи — круг 1.**
  - Два независимых черновика: «минимальный проверяемый срез» (M) и «ребёнок и улица как сцена» (Р).
  - Баллы рубрики 1–5, M / Р:

    | Критерий | M | Р |
    |---|---|---|
    | верность решениям владельца и пилоту № 61 б | 5 | 4 |
    | опыт ребёнка и узнаваемость | 3 | 5 |
    | проверяемость приёмки | 5 | 3 |
    | точность фактов | 4 | 4 |
    | реализуемость в worktree без выхода за `facade.py` | 5 | 4 |
    | объём и бюджет | 4 | 3 |
    | ясность вопросов | 4 | 4 |
    | **итого** | **30** | **27** |

  - **Основа — M.** Из него взяты:
    - регресс 7 фасадов с ожидаемым списком дампов;
    - контраст фона инлайн-скриптом с мутантом;
    - состав сцены по дампу;
    - сравнение после слияния с принятыми дампами A1g (закрывает E9);
    - изоляция основного дерева по снимку `tools`;
    - кремовая доска пекарни без вопроса;
    - скоуп и самотест с самопроверкой.
  - **Из Р взяты:**
    - таблица «что увидит ребёнок»;
    - находка «дом = пекарня» (IoU 0,989), проверка силуэтов с самопроверкой на BASE и примета-крендель;
    - приметы калиток;
    - замер неба рынка;
    - `place.day_light` в разрешённом наборе: его ловят дампы `place_*` приёмки 1b3;
    - правило «без красного и малинового круглого»;
    - два варианта композиции одной константой;
    - вопросы № 70 (композиция), № 71, № 73 и калитки без замка (№ 72 а);
    - поиск id места калитки в A1g2;
    - расхождение GAME_CONCEPT.md:1803.
  - **Правки судьи:**
    1. `paste | bc` в приёмке Р не исполнится: `bc` в Git Bash нет. Суммы — через `awk`.
    2. «24,6 % от 710 847» в M неверно: это 27,0 % (24,6 % — от 780 847).
    3. Сдвиги ссылок: `place.py` превью и RGB — :167-168 (в черновиках :166/:167); `placeBackground` — `GameApp.kt:242-246` (в M :241-245); докстринг `art_check.py` — :5 (в M :4).
    4. `art_check.py bg` на фоне улицы из Р проверил бы только статус-бар: в `TEXT` нет строк улицы, фильтр :206-208. Композиты строились бы из снимков мест (`SNAPS`). Заменено инлайн-проверкой полосы 0–386 px — верх ряда при 1,3 — и проверкой отличимости `which` с самопроверкой.
    5. Новая проверка зон карточки по дампу: мировой AABB изменённых объектов вне зоны жителя. Числа выведены из геометрии `facade.py`, самопроверка — мутантом.
    6. Константы `MARKS`/`ROW`: оба варианта ворот № 70 и № 71 из одного кода. Для варианта б — второй регресс с ожидаемым `facade_bakery ['sign']`.
    7. Самопроверка регресса — два мутанта сразу, ожидаются ровно 3 файла; второй мутант показывает связь с 1b3.
    8. protect дополнен отслеживаемыми файлами корня и `finny-pet/`, которых не было в обоих черновиках.
    9. Вопросы сведены к 5: небо и плита рынка — в № 74. Вопрос о бюджете не нужен: он уже стоит «к A1e», а A1e идёт раньше A1g2.
    10. Добавлены правки `docs/` в коммит спеки: HANDOFF.md:111 и «TOWN-J1.md:303» → «:305».
    11. Ссылки на §18 — по HEAD: рабочее дерево сдвинуто правками 1b1.
  - **Проверено судьёй** на HEAD `d35ac7c`:
    - 20+ ссылок file:line (`git show HEAD:`);
    - IoU 6 пар и пиксель неба рынка по рендерам `scratchpad/fac160`;
    - размеры 7 WebP (сумма 124 812);
    - отсутствие `bc`;
    - логика guard для строк самотеста 3–7 по коду `guard-paths.js:101-146`.
  - **Не запускались:**
    - `zones.py`, `street_dump.py`, скрипт IoU с Blender-дампами новой версии;
    - регресс 7 фасадов;
    - самотест хука.
  - **Открыто:**
    - механизм сессии в `wt/a1g`, `$CLAUDE_PROJECT_DIR`, `settings.local.json`;
    - два Blender на одном HIP одновременно — зонд «До спавна» п. 9а;
    - замер ряда при 1,3 на BASE (WORKFLOW № 28; взят старый снимок `emu_s1d_street13.png`);
    - достижимость IoU ≤ 0,93: замер fac160 — нужно 6 918 px силуэта вне маски дома; свободно в верхних 64 px кадра ≈ 7 616 слева и ≈ 6 405 справа (вне обеих масок). Одного угла впритык, а правый занят «!» (CONTRACT п. 4): крендель с просветами — левый угол плюс кронштейн слева;
    - из какого дерева собран APK 4 382 206 Б.
- **2026-09-28, круг критиков (2 измерения — продукт и арт; инженерия и пилот; каждый список — скептику).**
  - Счёт находок:

    | Критик | Находок | Подтверждено | Частично | Опровергнуто |
    |---|---|---|---|---|
    | продукт и арт | 15 | 6 | 9 | 0 |
    | инженерия и пилот | 17 | 11 | 6 | 0 |

  - **Принято целиком (confirmed):**
    - PA1 — довод КОНТЕКСТА о пекарне: ТЗ_текст.txt:389; пункт о цвете (:395-396) — про категорию расхода, по аналогии; № 51 а убран из КОНТЕКСТА и из заголовка приёмки п. 6;
    - PA2 — правый верхний угол фасада закрыт «!» (событие пекарни с недели 1): крендель — слева; в `zones.py` зона «!» и мутант в ней;
    - PA5 — самопроверка слепого судьи — серые силуэты дома и пекарни; BASE-вырезки — материал; рекомендация № 71 переписана;
    - PA13 — «серый и дейтеранопия» — судья, не машина; машинно — IoU п. 6;
    - PA14 — грепы ДОКИ с `--exclude=TOWN-A1g1.md`;
    - PA15 — питомец дома «внизу по центру», не «у двери» (`facade.py:10`, дверь :85 справа);
    - E1 — worktree уже есть (6b2277d, `pilot/a1g`, `pilot/j1b`): rebase на BASE вместо `worktree add`, перезапись обоих task-scope, проверка регресса 7 фасадов в `wt/j1b`;
    - E2 — `coder.md` пилота в feat/town не идёт: слияние через `git checkout pilot/a1g -- facade.py`; numstat только по facade.py; `--name-only` → coder.md и facade.py. То же правило нужно слиянию 1b3 (TOWN-J1-1b.md, «Слияние worktree в feat/town») — за рамками этой спеки;
    - E3 — `curve` запрещён для кренделя, `zones.py` fail-closed на CURVE, мутант-кривая;
    - E4 — связь с `place` по AST вместо grep (комментарии :134, :201 давали ложный красный на BASE); прогон на HEAD → `['M', 'PLACES', 'cloud', 'put']`;
    - E5 — `mkdir -p $T/a1g/base` перед `tar -C`;
    - E7 — самотест, строка 3 — Write в `wt/probe_outside.txt`, не в настоящий facade.py основного дерева; восстановление сверяет main0;
    - E10 — номер строки «≈ 70 КБ» в TOWN-J1.md — на момент коммита BASE (HEAD :305, рабочее дерево :313), в TOWN-J1-1b.md :17, :46, :414;
    - E11 — самопроверка `bbox` после рендера фона;
    - E12 — отдельные копии `nb` (MARKS) и `sky` (ROW) с `rm -rf` и проверкой подстановки; рендер `fac_nb` для К4; `cmp` фонов;
    - E15 — `finny-pet/.gitignore` в protect; проверка покрытия `ls-files`;
    - E16 — вариант (б): `place.day_light` убран из разрешённого набора (CONTRACT п. 2, приёмка п. 3), свет только `lib.studio`/`lib.light`; «Из Р взяты — `place.day_light`» выше этим отменено.
  - **Принято частично (partial) — только подтверждённая часть:**
    - PA3 — цвета примет калиток заданы (боровик без точек, цветок `#FFC94D`); довод «мухомор пугает» и grep по diff — нет;
    - PA4 — подложка White α 0,85 под подписями в эскизе A1g2 и на К3; вторая полоса в `street_check.py` — нет (геометрия карточки — оценка, S23 масштабирован);
    - PA6 — строка о калитках исправлена (IoU 0,75–0,82 и цвет забора); № 71 разделён на 71 и 71а; калитки в скрипте п. 6 (21 пара, на fac160 все, кроме home–bakery, ≤ 0,927); тезис «приметы не нужны» — нет, решает слепой судья;
    - PA7 — 72 а: убирается 🔒 из текста, меняется подпись листа № 38; ДОКИ A1g2: ARCHITECTURE.md:187, facade.py:11, grep с исключением спеки; UX_ACCESSIBILITY.md:47 и риск «только числа» — нет;
    - PA8 — вариант в) № 74 по пикселю рендера ≈ G.sky, константа `SKY`, К5 — три кадра; «шов как на листе» — нет (hex ≠ пиксель);
    - PA9 — условие № 70 — низ фасада на К3 при 1,0, 1,3 и S23; uiautomator-дамп — нет, верх карточки берётся с К2;
    - PA10 — форма кренделя — петля с двумя просветами, как `pastry_pretzel`; оба кренделя в одном ряду К4; отдельный кадр и строка «Слияние» — нет;
    - PA11 — строка «событие» и кадр рынка с «!» на К3 (ссылка на событие исправлена: `job_market_help` — content.json:2198, не :2042);
    - PA12 — строка о № 39, 40 в ЗАВИСИТ ОТ; нарушения нет;
    - E6 — зонд двух Blender «До спавна» п. 9а; очередь на п. 3–5 — нет (они до спавна);
    - E8 — перегон d1 и street.json после фиксации констант; перенос в `wt/a1g_d1` — по желанию (чистка scratch при смене сессии не доказана);
    - E9 — число доли — замер HEAD, так как арт-файлы в коммите BASE не меняются; п. 5 — перепроверка; отдельный п. 0 с рендером — нет;
    - E13 — буквальный путь в строке 4 самотеста и в восстановлении; ACCEPTANCE не менялся (`$T`/`$BL` — обозначения оркестратора);
    - E14 — исполнимая команда пересечения и хвостов (прогон на текущем task-scope → `[] []`);
    - E17 — CONTRACT п. 4 по этой находке не менялся (по пикселям свободны оба верхних угла; правый закрыт «!», PA2 — крендель слева), в «Открыто» — замер вместо оценки.
  - **Отклонено:** опровергнутых находок нет. Не применены опровергнутые части partial-находок (перечислены выше после «нет»).
  - Правки журнала судьи выше не переписаны: п. 8 «protect дополнен» был неполон (E15), `day_light` из Р отменён (E16).
- **2026-09-28, сессия 12 — исполнение.** Сессию `wt/a1g` открыл владелец; самотест хука — 7 строк совпали, стоп-хук в
  сессии worktree не сработал (два зонда), изоляция — guard и `git status`. Кодер (Opus xhigh) — 67 / 5 в facade.py, за один
  запуск. Решения кодера: экспозиция фона −1,0 (при −0,15 тротуар пересвечен), свет — `lib.studio`, облаков нет (при ROW =
  ground над холмами только полоса 0–21 %), крендель на столбе с кронштейном слева (без столба IoU ≈ 0,927 — впритык).
  Приёмка оркестратора — все 10 пунктов зелёные: регресс ровно 3 дампа, вариант б — только `sign`; AST —
  M/PLACES/cloud/put; bbox 7 × OK; STREET BG OK (оба ROW); which 1,00 (самопроверка — FAIL); силуэты ≤ 0,924
  (самопроверка BASE — 0,989 FAIL); ZONES OK; STREET SCENE OK (самопроверка — FAIL); WebP фасадов 129 580 Б (max
  27 986), фон 28 354 Б, сумма 8 — 157 934 ≤ 186 252. Небо улицы в полосе 0–20 % в среднем (129, 180, 218) ≈ G.sky
  (111, 177, 224) — вариант «в» № 74 фактически уже есть; панель неба фасада рынка (83, 116, 154) темнее на ≈ (46, 64,
  64). Коммит в ветке пилота — c9287a2 (`pilot/a1g`).
- 2026-09-28, сессия 13 — ворота арта: № 70 **а**, № 71 **а**, № 71а **а** (одна константа `MARKS`, круг на разделение не
  нужен), № 72 **а**, № 73 **а** «но с рисунком, который показывает, что за здание» (рисунок на табличке дома — варианты
  листом из круга A1g1-2), № 74 **б** (круг A1g1-2: панель неба в тон неба улицы, плита в тон тротуара; попутно —
  тёмная серединка цветка парка, находка Р6). Находки судьи доступности Р5 (границы строк калиток 1,50 : 1) — в контракт
  A1g2, Р7–Р8 (значок «!») — № 75 а, TOWN-J1-1b1-2.
- 2026-09-29, сессия 13 — A1g1-2 (кодер Opus xhigh, 29 / 5): панель неба рынка `MARKET_SKY_EMIT = 0.15` (Δ +5, −2, 0),
  плита рынка #E8DCCB, серединка цветка #8D5A3B, `HOME_MARK` (house / heart / key / None). Приёмка: регресс — ровно
  facade_home/market/gate_park; SKY OK; PLATE OK; силуэты ≤ 0,924; ZONES OK (zones2.py — сравнение по геометрии); WebP
  8 — 158 396 Б. Коммит a0915cf. Лист `pilot_art_8_street2`: «ребёнок» — домик = «мой дом», сердечко — «добрый», ключ —
  «закрыто»; доступность — сердечко при дейтеранопии 2,93 : 1 и цвет значка «!». Владелец № 82: вывески на табличках
  всех зданий — пекарня багет, магазины корзинка (дом — домик) → круг A1g1-3 (зонды sign_contrast.py, zones3.py).
