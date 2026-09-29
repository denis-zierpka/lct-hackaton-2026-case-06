# TOWN-A1e — вещи и товары: A1e1 — генератор 22 спрайтов 216 × 216 в `props.py` (12 камерой комнаты, 10 товарным ракурсом); A1e2 — встройка (набросок)

```
TASK: TOWN-A1e1 — в finny-pet/tools/art/props.py: режим камеры комнаты (render_thing на room.palette, room.light_room,
      room.room_camera, room.sprite_frame из A1d1) для вещей мест и стартовых вещей, новые построители товаров, мечты
      «Походная палатка», плаката «Супер-корм» и «Ванна с пеной» вместо баночки витаминок; 22 спрайта 216 × 216
      (только генератор; WebP в res/, itemRes/goalRes, стартовые в комнате, плакат в UI, LICENSES — A1e2)
EPIC: TOWN-A1 (docs/tasks/TOWN-A1.md: строка A1e :43, порядок :49-54, пилот № 61 и доли :56-64, правила :66-78, ANTI-SCOPE :80-83)
BASE: коммит на feat/town после слияния A1d1 (room.py + 7 прогонов room_furn_* в art_check.py) и ответов № 93–95; в нём —
      эта спека с ответами, строка A1e и доля в TOWN-A1.md («До спавна» п. 1); finny-pet/tools/ и tools/ в нём не меняются.
      При № 95 б первый заход (товары) — от feat/town до слияния A1d1 («До спавна» п. 0), вещи — от BASE после слияния
BRANCH: feat/town. Вариант A — основное дерево, если в нём в это время нет кодера; вариант B — git worktree wt/a1e (ветка
        pilot/a1e от BASE + cherry-pick 8dbfd2d — профиль арт-кодера, в feat/town не сливается), своя сессия Claude Code
        (открывает владелец), свой wt/a1e/.claude/task-scope.json; в feat/town переносится только props.py.
        Взято: вариант B в существующем wt/a1d (ветка pilot/a1e) и его сессии-обёртке — журнал 2026-09-29 «BASE»
ЗАВИСИТ ОТ: № 16 («Ванна с пеной», id прежний, GAME_CONCEPT.md:2046), № 17 (стоп-лист: пустые места контуром не рисуются, :1356),
        № 28 а (один спрайт камерой комнаты и для места, и для полки лавки, :2059), № 31 а (змей, палатка-мечта, плакат
        «Супер-корм»; вертушка, карусель, открытка, игрушка-подарок — срез 2, :2062), № 39 а (витрина без названий,
        плакатик над товаром, :2070), № 61 б и дополнение (генераторы по файлам, общий только lib.py, заморожен, рендер
        вне репозитория, :2092), № 66 б (кромка на белом ≥ 3 : 1, :2097); № 79 б (тени нет, `SHADOW = False`,
        TOWN-A1d1.md:828-833; вещи повторяют `room.SHADOW`, CONTRACT п. 4.5); TOWN-A1d1 CONTRACT п. 4–6, 9 (TOWN-A1d1.md:129-157,
        :179-181) — код A1d1 слит в feat/town 7571ef7 (pilot/a1d 51f7fc1: A1d1 b536480 + круг A1d1-2 + `WARM` по № 78 б)
```
Номера строк — HEAD `47f5001` (feat/town: 1b1-2 — 70928df, очередь ворот — b0960e8, 6 WebP выпечки — 47f5001; 2629c3e меняет только
доки A1d1 и GATE_QUEUE); room.py — `wt/a1d` b536480 (не слит; имена и подписи сверяет «До спавна» п. 0). Перед коммитом BASE строки
сверить (`grep -n '^fun itemRes' finny-pet/app/src/game/java/ru/finny/pet/game/ui/TownUi.kt`, `grep -n '^| 28 |' docs/GAME_CONCEPT.md`).
Сверено на BASE (журнал): room.py (7571ef7) — те же номера, что b536480; props.py не менялся; TownUi.kt — +11 строк после :107
(1b4): `goalRes` :120-128, `Pic` :132-138, шапка :237; GAME_CONCEPT.md — +1 (№ 28 — :2060, № 31 — :2063).
Вопросы владельцу — с № 93: № 76–81 — A1d1, № 82 — вывески, № 83–88 — итог смены 1b1-2, № 89–90 — 1b4 (TOWN-J1-1b4.md:565),
№ 91–92 — комната (GATE_QUEUE.md:4, 2629c3e); проверка перед записью в GATE_QUEUE — раздел вопросов.

## ПОЧЕМУ ДВЕ ЗАДАЧИ (A1e1 + A1e2), А НЕ ОДНА И НЕ ТРИ
1. **Вид решает владелец только по листу на настоящем фоне** (HANDOFF.md:91-94): генератор → лист → ворота → встройка, как
   A1d1/A1d2, A1g1/A1g2, 1b3/1b4. Встройка ответов не угадывает.
2. **Разные скоупы и исполнители.** A1e1 — Python в `tools/art` (арт-кодер, Opus xhigh, Blender); A1e2 — Kotlin в `game/`
   по очереди с A1d2 (`RoomScreen.kt`) и 1b4 (`TownUi.kt`, рядом с `itemRes`) — «обе трогают `TownUi.kt` — по очереди»
   (TOWN-A1.md:51-52). test-author не нужен ни там, ни там (имена в `game/`, `content.json` не меняется, TOWN-A1.md:72-74).
3. **Правило волны № 61:** два генератора в одном файле не пускать (TOWN-J1-1b.md:478-479). `props.py` свободен после
   слияния 1b3 (184843d); `room.py` A1e1 только импортирует.
4. **Товары отдельно от вещей не дробим по умолчанию**: лист и ворота одни (полка «У Фомы» — товары и вещи вместе), две
   приёмки одного файла — лишний круг. Если на воротах № 76–81 A1d1 уйдёт в круг, товарная половина может идти раньше —
   вопрос № 95 (CONTRACT п. 5–6 и зонд `--goods-only` к этому готовы).

## КОНТЕКСТ
- **Эмодзи сейчас.** Две лавки продают 23 разных товара (`town.shops`: рынок 8, «У Фомы» 18, общие `food_basic`,
  `care_soap`, `fun_balloon`). Картинку дают 9 веток `itemRes` (TownUi.kt:94-106: `food_basic`, `food_lunch`, `care_shampoo`,
  `care_brush`, `care_vitamins`, `fun_ball`, `fun_balloon`, `fun_book`, `fun_tent`; ещё `fun_bow` — в лавках его нет), эмодзи
  (`Pic(null…)`/`else -> null`, TownUi.kt:121-127) — у 14: `food_porridge` 🍚 (content.json:943), `food_super` 🥫 (:954),
  `care_soap` 🧼 (:966), `care_shampoo_simple` 🫧 (:977), `fun_icecream` 🍦 (:988), `fun_carousel` 🎠 (:999), `fun_picture` 🖼️
  (:1010), `fun_rug` 🧶 (:1024), `fun_spinner` 🌀 (:1037), `fun_kite` 🪁 (:1050), `fun_robot` 🤖 (:1064), `fun_starlamp` ⭐
  (:1077), `gift_card` 💌 (:1105), `gift_toy` 🎁 (:1119). Мечта `goal_camp` 🏕️ (:1214) — эмодзи (`goalRes` :109-117 её не знает).
  Стартовые `home_flower` 🪴 (`spot_1`, :1164) и `home_armchair` 🛋️ (`spot_4`, :1170) — эмодзи всегда: `Pic(null, starter.emoji…)`
  (RoomScreen.kt:225, JarsScreen.kt:204). `home_lamp` 💡 (:1157) нигде в `game` не рисуется.
- **«Ванна с пеной» показывает баночку витаминок.** Название сменили (№ 16), картинку — нет: `item_care_vitamins`
  (props.py:129-143) — прозрачная банка с таблетками (BACKLOG.md:107-111); файл 38 478 Б — выше потолка спрайта 37 888
  (TOWN-A1.md:68-69): полупрозрачное стекло `alpha=0.32`.
- **Где картинка видна** (одна картинка на все места; `Pic` рисует квадрат `size`):

  | Экран | Код | Размер |
  |---|---|---|
  | карточка лавки (белая, до A1s) | PlaceScreen.kt:229-239 | 40 dp |
  | витрина A1s (без названий, № 39) | макет `a1s_mock_1_market.jpg` | ≈ 60–64 dp |
  | место в комнате | RoomScreen.kt:219-227 (`SpotThing`) | 36 dp |
  | «Обустроить» (`paperTint` #F4F2F8) | JarsScreen.kt:204-205 | 40 dp |
  | копилка: мечта / список / выбор | SavingsScreen.kt:55, :88, :100 | 72 / 48 / 56 dp |
  | калитка мечты на улице | StreetScreen.kt:106 | 48 dp |
  | шапка | TownUi.kt:226 | 28 dp |

  Вещь `keep` становится мечтой `item:<id>` (Town.kt:502) и рисуется `itemRes` в копилке на 72 dp. 3 px/dp — плотность
  эмулятора 360 × 640 и S23 (TOWN-A1d1.md:847) → **216 × 216 px** закрывают самый большой показ 1:1; нынешние товары —
  512 px (в 2,4 раза больше нужного), выпечка — 256.
- **`props.py` сейчас.** `PROPS` :409-419 — 28 построителей (10 `item_*`, 5 `goal_*`, 6 `tile_*`, `ui_coin`, 6 `pastry_*`);
  `VIEWS` :421-425; `fit_camera` :428-457 (двигает камеру по углам bbox; у кривых — по вершинам: «curve bound_box is bogus»,
  :435); `render_prop` :460-467 (студийный свет, без ловца тени); CLI :470-487.
- **Чужие пользователи построителей.** `place.py` импортирует `props` (:19) и ставит декор построителями по имени (`put`
  :59-66): `item_food_basic` (:32), `item_care_shampoo`, `item_fun_book` (:39-43); `facade.py` через `place.put` —
  `item_food_basic`, `item_fun_ball`, `item_fun_balloon` (:105-108), `item_care_shampoo`, `item_fun_book` (:120-121);
  `FORBIDDEN` (place.py:23) — `item_care_vitamins`, `item_fun_bow`. Домик `item_fun_tent` и ванна больше нигде не
  используются (`git grep -n "item_fun_tent\|item_care_vitamins" -- finny-pet/tools tools` — только props.py и `FORBIDDEN`).
  Следствие: тела и ключи `PROPS` прочих построителей не менять; тело ванны и дверь/цвет домика — можно.
- **Камера комнаты A1d1** (wt/a1d, не принят): `palette()` room.py:33 (материалы оболочки), `build_room(evening, portrait)`
  :143, `light_room(scene, evening, lamp_xy)` :169, `room_camera(scene, portrait=True)` :199 — (0; −10,5; 3,1) → (0; 2; 2,0),
  lens 36; `sprite_frame(scene, objs, w, h, floor=False, top=None)` :208 — кадр ровно `w × h` px по проекции углов
  `o.bound_box` (по x — центр, `floor` — низ проекции + 3 px); `render_sprite` :227 — сцена спрайта мебели: `palette()`,
  `plank_floor`, `shell_walls(window=None)` невидимы камере (`SHADOW = False`), дневной `light_room`, `room_camera`, 1080 × 1920.
  После слияния регресс дампит 7 `room_furn_<id>` (TOWN-A1d1.md:502-503) — эталон мира для вещей A1e.
- **Где стоят вещи** (проекция камерой комнаты на 360 × 640, сенсор 36 по высоте; прогнано при сведении):

  | Точка мира | Проекция | Что там | 1 ед. мира | Взгляд сверху |
  |---|---|---|---|---|
  | пол (0; −1; 0) | 180 × 468 dp | низ строки мест `spot_1`/`spot_6` (436..472 dp) | ≈ 66 dp | 18,1° |
  | пол (0; −5; 0) | 180 × 610 dp | передний пол | — | 29,4° |
  | стена (0; 3; 2,25) | 180 × 304 dp | центр `spot_2` (286..322 dp) | ≈ 47 dp | 3,6° |
  | стык пола и стены | 180 × 408,8 dp | линия пола (как TOWN-A1d1.md:49) | — | 12,9° |

  `spot_3` (слот `table`) — левая колонка между холодильником и сундуком (RoomScreen.kt:151-158); стола нет (ANTI-SCOPE
  A1d1, TOWN-A1d1.md:243-244) — вещи «стола» (книжка, лампа-звёздочка, вертушка) стоят на полу. Овальный коврик прототипа
  (1,4 : 1, с толщиной) под 18° (якорь −1) виден эллипсом ≈ 1 : 4, под 29° (−5) — ≈ 1 : 2,5 (замер спрайта 2,49; расчёт
  проекцией — 4,0 и 2,46); для круга было бы 1 : 3,1 и 1 : 1,85 (журнал).
- **Что в срезе 1 не видно.** Мастерская (`lamp_new`, :1144) закрыта: улица показывает места без `opensBy`
  (StreetScreen.kt:50). С2 «Погасла лампа» (`c2_lamp`) — в `eventsOff` (content.json:2256): лампа не ломается; «лампа до и
  после починки» — арт среза 2 (GAME_CONCEPT.md:1885). `fun_lego` (`eventOnly`, :1091) приходит мечтой `goal_lego` и без
  ветки `itemRes` встанет в комнату и «Обустроить» эмодзи 🧱.
- **Расхождение № 31 и строки A1e.** № 31 отложил вертушку, карусель, открытку и игрушку-подарок на срез 2 (и ANTI-SCOPE
  эпика, TOWN-A1.md:81-82); строка A1e (:43, записана после № 39) требует «спрайты всех 23 товаров двух лавок» — на витрине
  A1s названий нет. Все четыре на полках с недели 1: `unlockPeriod` нет, `Prices.shelf` прячет только `eventOnly`, ещё не
  открытые и купленные `keep` (Prices.kt:37-45). Решает владелец — № 93.
- **Пк3 «Супер-корм»** (`pk3_super_food`, content.json:2042; GAME_CONCEPT.md:1114-1116): рынок, неделя 2; урок — «реклама
  хвалит, а сытость та же» (у обоих кормов «Сытость +40»). Сейчас — `Panel` «! Супер-корм» с текстом и «Пройти мимо»
  (PlaceScreen.kt:408-418). Банка обязана выглядеть ярко, но читаться кормом, как миска.
- **Пилот № 61:** `lib.py` заморожен (слияние `facade.py` ждёт № 82); на видеокарте ≤ 2 Blender (TOWN-A1d1.md:373-377),
  счёт — `tasklist | grep -ci '^blender\.exe'` (не `blender`: ловит `blender-mcp.exe`).
- **ТЗ** (docs/sources/ТЗ_текст.txt): 3.3 — право на изображения (строка LICENSES — A1e2); 3.5 — настоящей рекламы и брендов нет
  (:372, :124: плакат — игровой урок Пк3, без марок и логотипов), картинки не пугают (:375; робот добрый, ванна без медицины); 3.6 — цвет не
  единственный признак (пары похожих различаются силуэтом), текст растёт со шрифтом (на спрайтах букв нет), контраст кромки.

## ЧТО УВИДИТ РЕБЁНОК (после A1e2; A1e1 экран не меняет)
| Момент | Что на экране | Что понимает ребёнок |
|---|---|---|
| рынок, неделя 1 | миска корма, глубокая миска каши с ложкой, брусок мыла с пузырями, простой флакон, шарик, рожок мороженого, карусель — без кружков-эмодзи | «Тут еда, мыло и радость — узнаю без подписи» |
| рынок, неделя 2 (Пк3) | яркая банка со звездой-взрывом и лапкой; плакат — та же банка в лучах и звёздах, без букв; слоган — живым текстом рядом | «Банку хвалят — это реклама; а корм тот же» |
| «У Фомы» | ванночка с горой пены (не таблетки), робот, змей, лампа-звёздочка, картина, коврик, книжка, домик-палатка, мячик, открытка, подарок | «Вещи для дома и для игры» |
| купил и поставил | вещь на месте — та же картинка, что на полке, в свете и перспективе мебели A1d; коврик на полу, картина и змей на стене | «Моя вещь — в моём доме» |
| дом с первого запуска | цветок в горшке и мягкое кресло — картинки, как мебель; над кроватью горит бра (по К2) | «Дом обжитой» |
| «Обустроить» | те же картинки 40 dp | «Это мои вещи» |
| копилка и улица | мечта — двускатная палатка в поход, не жёлтый домик-игрушка | «Коплю на поход» |
| пустое место | ничего (стоп-лист № 17) | не приманка «купи» |

Цвет — не единственный признак: пары похожих различаются силуэтом. Список пар один — столбец «не путать» CONTRACT п. 7
(судьи — в сером и при дейтеранопии, К5).

## CONTRACT (только `finny-pet/tools/art/props.py`)
1. **Константы и имена.** `ITEM_PX = 216  # 72 dp × 3 px/dp: the largest box of an item or a dream (SavingsScreen)`,
   `ROOM_FILL = 0.9`. Имена: товары и вещи — `item_<id>`, стартовые — `item_<homeItemId>` (`item_home_flower`,
   `item_home_armchair`, `item_home_lamp`), мечта — `goal_<id>`, плакат события — `poster_<itemId>` (`poster_food_super`).
   Все построители (и вещей, и товаров) — в `PROPS` (одна точка: `place.put` и `--all` видят всё).
2. **Режим камеры комнаты** — вещи мест и стартовые (№ 28 а):
   - `ROOM = {name: slot}` — ровно 12 имён таблицы п. 7 с режимом «комната» (при № 93 б — 11, без `item_fun_spinner`);
     `slot` ∈ `"floor"`, `"table"`, `"wall"`, `"rug"`;
   - `ANCHOR = {"floor": (0.0, -1.0, 0.0), "table": (0.0, -1.0, 0.0), "wall": (0.0, room.WALL_Y - 0.01, 2.25), "rug": (0.0, -5.0, 0.0)}`
     с комментарием: пол — низ строки мест, 18°; «стол» — пол (стола нет); стена — центр `spot_2`; коврик — 29°, чтобы
     плоский овал читался (при −1,0 тот же овал ≈ 1 : 4).
3. **Соглашение построителей вещей:** `floor`/`table`/`rug` — как все в props.py (:7: у начала координат, стоит на z = 0,
   лицом к −Y); `wall` — задняя грань в плоскости y = 0, висящая часть по центру x = 0, z = 0, лицом к −Y. В построителях
   `ROOM` нет `curve()` (`sprite_frame` берёт `o.bound_box`, у кривых он ложный, props.py:435; нить змея — цилиндры и бантики).
4. **`render_thing(name, out, samples)`** — по шагам, в этом порядке (прототип прогнан, журнал):
   1. `scene = reset_scene(samples, width=1080, height=1920)`;
   2. оболочка как у `room.render_sprite`: `p = room.palette()`, `plank_floor(p["wood"], room.K)`,
      `shell_walls(p["m_wall"], p["m_side"], p["m_white"], room.K, room.WALL_Y, window=None)`; запомнить её объекты;
   3. `room.light_room(scene, False, None)` и `room.room_camera(scene)` — **до** построителя (у света и камеры остаются
      имена `key`, `fill`, `sun`, `cam`; «солнце» картины получит `sun.001`, а не заберёт имя у света);
   4. `PROPS[name]()`; новые объекты — появившиеся после шага 3; пустышка с **именем вещи** (как `place.put`, place.py:59-66)
      в `ANCHOR[ROOM[name]]`, поворот 0; новые объекты без родителя — её дети;
   5. у объектов оболочки — тот же цикл, что в `room.render_sprite` (wt/a1d room.py:239-241): `o.is_shadow_catcher =
      room.SHADOW; o.visible_camera = room.SHADOW and (floor and o.name.startswith("plank") or wall and o.name == "wall_back")`,
      где `wall = ROOM[name] == "wall"`, `floor = not wall` (решение № 79 одно — в room.py; при № 79 б в кадре оболочки нет);
   6. подгонка — до 6 итераций: `bpy.context.view_layer.update()`, проекция углов `o.bound_box` новых мешей
      (`world_to_camera_view`, те же точки, что у `sprite_frame`), большая сторона в px кадра → масштаб пустышки
      `*= ROOM_FILL · ITEM_PX / сторона` (единый);
   7. `room.sprite_frame(scene, objs, ITEM_PX, ITEM_PX, floor=ROOM[name] != "wall")` (`objs` — новые меши), `render(scene, out)`.
   Свет, камера, материалы оболочки — только из `room` (без копий чисел).
5. **Товарный ракурс** — прежние `render_prop`, `fit_camera`, `studio` без изменений; новые имена — в `PROPS`, их `VIEWS` —
   `(ITEM_PX, 0.9, 32, 24)` (азимут и высоту можно менять, `fill` ≤ 0,96, как у плиток, props.py:422: `fit_camera`
   вписывает углы bbox, круглое выходит мельче; при 0,98 и 96 сэмплах край α > 0 доходит до 1 px — FAIL зонда, журнал); `poster_food_super` — `(ITEM_PX, 0.9, 0, 0)` (анфас, как `ui_coin`); `item_care_vitamins` — `ITEM_PX`.
   Помощник `super_can(loc, s)` — одна банка для `item_food_super` и плаката.
6. **Прежнее не меняется** (зонд `kept_check.py`): каждое верхнеуровневое утверждение BASE — импорты, палитра `C`, помощники,
   построители, строки `VIEWS`, `fit_camera`, `render_prop` — побайтно по AST, кроме докстринга, словаря `PROPS` (ключи BASE
   с прежними построителями — на месте), блока `__main__` и двух построителей: `item_care_vitamins` (новое тело — ванна) и
   `item_fun_tent` (только дверь к −Y — она повёрнута под товарный азимут 32°, props.py:187 — и цвет/кант под кромку; силуэт
   тот же). Как править домик, чтобы зонд не дал ложный FAIL: цвет — только значения `tent = M(...)` и `trim = M(...)`; кант —
   отдельным утверждением, прежний цикл полос не трогать; дверь — только `a = R(...)` и утверждение с `door`. Зонд проверяет,
   что ядро BASE вошло в NEW; добавленное (лишний меш, масштаб, `hide_render`) он не видит — «силуэт тот же» судит лист К1
   (старый 512-px домик рядом с новым). Новые цвета — hex-литералами в построителях (как выпечка 1b3), `C` не расширять; список новых hex — в отчёт.
7. **22 построителя** (силуэт — то, что ребёнок узнаёт без подписи; «не путать» — пара судей К5):

   | name | id — где | режим | силуэт (форма, не цвет) | не путать с |
   |---|---|---|---|---|
   | `item_fun_rug` | `fun_rug` — Фома 20 | комната, rug | овальный коврик с каймой и узором, бахрома на коротких краях | миски корма и каши |
   | `item_fun_starlamp` | `fun_starlamp` — Фома 40 | комната, table | ночник: основание, ножка, светящаяся звезда (эмиссия) | звезда своей мечты `goal_custom`, `tile_star` |
   | `item_fun_picture` | `fun_picture` — Фома 15 | комната, wall | рама видимой толщины (темнее стены #EAE2F6), внутри солнце, холм, домик | окно `furn_window`, плакат |
   | `item_fun_robot` | `fun_robot` — Фома 40 | комната, floor | корпус-ящик, голова с антенной, круглые светящиеся глаза и улыбка (не страшный), руки, колёса | жители и питомец |
   | `item_fun_kite` | `fun_kite` — Фома 30 | комната, wall | ромб на крестовине двух цветов, хвост с бантиками (≥ 30 % высоты кадра) | — |
   | `item_fun_spinner` | `fun_spinner` — Фома 25 (№ 93) | комната, table | вертушка: 4 загнутые лопасти на палочке в подставке, лопасти светятся | цветок |
   | `item_fun_ball` | `fun_ball` — Фома 25 | комната, floor | прежний построитель :146-151 — меняется только камера | — |
   | `item_fun_book` | `fun_book` — Фома 35 | комната, table | прежний :170-179 (он же декор «У Фомы» и фасада) | открытка |
   | `item_fun_tent` | `fun_tent` — Фома 60 | комната, floor | прежний :182-194: конус с полосами и флажком; дверь к −Y | палатка-мечта |
   | `item_home_flower` | `home_flower` — старт, `spot_1` | комната, floor | горшок с ободком, листья, 1–3 цветка | вертушка |
   | `item_home_armchair` | `home_armchair` — старт, `spot_4` | комната, floor | мягкое кресло: сиденье, спинка, два подлокотника, ножки; не лавандовое (стена) и не диван | кровать `furn_bed` |
   | `item_home_lamp` | `home_lamp` и `lamp_new` | комната, wall | бра: кронштейн, абажур-колокол, лампочка горит (эмиссия) | лампа-звёздочка |
   | `item_food_porridge` | `food_porridge` — рынок 15 | товар | глубокая миска, горка каши с кусочком масла, ложка наискосок выше края | миска корма, кастрюля обеда |
   | `item_food_super` | `food_super` — рынок 35 | товар | `super_can`: высокая яркая банка с ободками, на этикетке звезда-взрыв и лапка, блик; порция на вид не больше миски | флакон шампуня, миска корма |
   | `item_care_soap` | `care_soap` — рынок 12, Фома 10 | товар | скруглённый брусок на мыльнице, 2–4 пузыря с бликом `m_glint` | ванна |
   | `item_care_shampoo_simple` | `care_shampoo_simple` — рынок 15 | товар | флакон ниже `item_care_shampoo`, откидная крышка вместо дозатора, другой основной цвет | `item_care_shampoo` |
   | `item_fun_icecream` | `fun_icecream` — рынок 10 | товар | вафельный рожок в сеточку остриём вниз, 1–2 шарика, без вишни | кекс `pastry_cupcake` |
   | `item_care_vitamins` | `care_vitamins` «Ванна с пеной» — Фома 25 | товар | ванночка на ножках, шапка пены с пузырями, можно уточку; ни таблеток, ни крестов (№ 16) | мыло |
   | `goal_camp` | мечта «Походная палатка» | товар | двускатная палатка (призма) с открытым входом, колышки и растяжки, рядом ёлочка или костерок | `item_fun_tent` |
   | `poster_food_super` | плакат события Пк3 | товар, анфас | лист с краем-взрывом, в центре `super_can` крупно, лучи, 2–3 звезды; «!» — фигурами, не буквами | картина |
   | `item_fun_carousel` | `fun_carousel` — рынок 20 (№ 93) | товар | круглая платформа, шест, полосатый купол с флажком, 2–3 лошадки на шестах | — |
   | `item_gift_card` | `gift_card` — Фома 10 (№ 93) | товар | сложенная открытка стоит «домиком», сердце на лицевой стороне; не конверт (конверт в комнате — карманные, RoomScreen.kt:235-243) | книжка |

   Без нового рендера (ветки A1e2): `gift_toy` → готовый `tile_gift` (коробка с бантом, props.py:287-295), `lamp_new` →
   `item_home_lamp`, `fun_lego` → `goal_lego`.
8. **Цвета и текст.** Кромка на белом (медиана контраста кольца 6 px к #FFFFFF) **≥ 3,0 : 1** у каждого нового спрайта и у
   домика (№ 66 б; белая карточка лавки, «Обустроить», облачко); светлое (мыло, пена, флакон, крем) — на цветном теле или с
   тёмным кантом `purple` #520978, без плашек-подложек. Полупрозрачные тела (`alpha` < 1, `transmission`) — только блики и
   пузыри (стекло банки витаминок дало 38 478 Б). Букв (`text3d`, `lib.sign` с текстом) нет: они не растут со шрифтом и не
   читаются TalkBack (ТЗ 3.6). Марок и логотипов нет (ТЗ 3.5).
9. **CLI** (:470-487): все три цикла — `(render_thing if n in ROOM else render_prop)(…)`; `--only NAME --out FILE`,
   `--only a,b --out DIR`, `--all DIR`, `--samples` — как прежде; вызов регресса `--only tile_apple --samples 1 --out F` —
   как прежде.
10. **Импорты:** прежние (`sys, os, math, argparse, random`, `lib`, `bpy`, `bpy_extras`, `mathutils`) плюс `import room` на
    верхнем уровне (без побочных эффектов — CONTRACT п. 9 A1d1; прототип: дампы `place_*`, `facade_*`, `props_*` не
    меняются). `place`, `facade`, `pet`, `uiprops` не импортировать (`place` импортирует `props` — цикл). Импорт `props`
    не рендерит и не создаёт объектов и материалов.
11. **Докстринг** (:1-10): режим камеры комнаты — `ROOM`, `ANCHOR`, `ITEM_PX`, `render_thing`, `room_camera`, `sprite_frame`;
    имена `item_home_<id>`, `poster_<itemId>`; пример `--only item_fun_rug --out F`.

На выбор кодера (судят лист и ворота): оттенки, узор коврика, сюжет картины, число лошадок и пузырей, форма абажуров,
ёлочка или костерок у палатки, сила свечения ламп и вертушки, азимут и высота `VIEWS` новых товаров (кроме анфаса плаката).

## SCOPE
**Вариант A** (основное дерево, когда в нём нет кодера): `.claude/task-scope.json` пишет оркестратор — protect A1d1
(TOWN-A1d1.md:191-207) с заменой `props.py` → allow, `room.py` → protect:
```json
{"task": "TOWN-A1e1 — вещи и товары (props.py), генератор (docs/tasks/TOWN-A1e.md)",
 "base": "<sha BASE>",
 "allow": ["finny-pet/tools/art/props.py"],
 "protect": [".claude/", "CLAUDE.md", "README.md", ".gitignore", ".gitattributes", "docs/", "finny-pet/.gitignore",
  "finny-pet/docs/", "finny-pet/app/", "finny-pet/screenshots/", "finny-pet/assets/", "finny-pet/release/",
  "finny-pet/README.md", "finny-pet/CHANGELOG.md", "finny-pet/keystore.properties.example",
  "finny-pet/build.gradle.kts", "finny-pet/settings.gradle.kts", "finny-pet/gradle/", "finny-pet/gradle.properties",
  "finny-pet/gradlew", "finny-pet/gradlew.bat",
  "finny-pet/tools/art/lib.py", "finny-pet/tools/art/place.py", "finny-pet/tools/art/room.py",
  "finny-pet/tools/art/to_webp.py", "finny-pet/tools/art/facade.py", "finny-pet/tools/art/pet.py",
  "finny-pet/tools/art/uiprops.py", "finny-pet/tools/art/import_sprites.py", "finny-pet/tools/art/sounds.py",
  "finny-pet/tools/art/smoke.py", "finny-pet/tools/art/montserrat_extrabold.ttf",
  "finny-pet/tools/ui.py", "finny-pet/tools/demo_run.sh", "finny-pet/tools/office/",
  "tools/art_check.py", "tools/sheets.py", "tools/perf.sh", "tools/adbui.sh", "tools/emu.sh", "tools/ui_measure.py",
  "tools/mutation_probe.py", "tools/content_map_events.py", "tools/town_route.sh", "tools/bakery_states.sh",
  "tools/rec.sh", "tools/mock_bakery.py", "tools/line_close.py", "tools/result_geom.py"]}
```
- Проверено на HEAD 47f5001 (сведение): пересечения и хвосты (команды TOWN-A1d1.md:304-308) → `[] []`; покрытие
  (`git -c core.quotepath=off ls-files`) → `['finny-pet/tools/art/props.py']`. Новый отслеживаемый файл
  на BASE (коммиты A1d1, 1b4) — дописать в protect поимённо и перепроверить (WORKFLOW № 29).
- Каталогов `tools/`, `finny-pet/tools/`, `finny-pet/tools/art/` в protect нет: хвост `tools/art/` задел бы `props.py`
  (guard-paths.js:139-146); хвост `art/uiprops.py` в `…/art/props.py` не входит — проверяет та же команда.
- **Вариант B** (в основном дереве идёт кодер — A1d2, 1b4, A1g2): тот же JSON в `wt/a1e/.claude/task-scope.json`, пути от
  корня worktree; сессию в `wt\a1e` открывает владелец; стоп-хук там не срабатывает (WORKFLOW № 41) — изоляцию держат guard
  и `git status` приёмки.
- **Кодеру:** команды с путями protect — без `>` и `2>&1` (WORKFLOW № 29); `lib.py`, `room.py`, `place.py` читать инструментом
  Read; рендеры и логи — только в свой scratch; итерации — `--only <имя> --out <sc>/<имя>.png --samples 16`; финальные
  рендеры — оркестратор.
- **Самопроверка кодера** (копии зондов, `d0/` и `props_base.py` — «До спавна» п. 7; `BL` — `C:/Program Files/Blender
  Foundation/Blender 5.2/blender.exe`; `<sc>` — scratch; `NAMES`, `ROOMN`, флаги и `N` — строка варианта в таблице ACCEPTANCE):
  - `mkdir -p <sc>/spr <sc>/sd`; `"$BL" -b --factory-startup --python-exit-code 1 -P finny-pet/tools/art/props.py -- --only "$NAMES" --out <sc>/spr --samples 16`;
  - для каждого `n` из `ROOMN`: `DUMP_OUT=<sc>/sd/$n.json "$BL" -b --factory-startup --python-exit-code 1 -P finny-pet/tools/art/props.py --python tools/art_check.py -- --only $n --out <sc>/sd/$n.png --samples 1`;
  - `python <sc>/a1e_check.py <sc>/spr <sc>/sd <sc>/d0/room_furn_chest.json <флаги>` → `A1E OK N`;
  - `python <sc>/kept_check.py <sc>/props_base.py finny-pet/tools/art/props.py` → `KEPT OK`;
  - контракт по AST — команда ACCEPTANCE п. 2а → `[] [] ["ROOM[name] != 'wall'"]`;
  - зонды — критерий CONTRACT п. 2–8; FAIL зонда при сдаче — не `DONE`.

## ANTI-SCOPE
- `res/`, WebP в APK, LICENSES, любой Kotlin, `itemRes`/`goalRes`, стартовые в `RoomScreen` и «Обустроить», бра в комнате,
  плакат в карточке события или витрине — A1e2.
- `content.json` (id, названия, эмодзи — прежние; эмодзи остаётся запасным путём `Pic`).
- `room.py` (A1d: камера, свет, оболочка — одна точка правды), `lib.py` (заморожен, № 61 б), `place.py`, `facade.py`,
  `pet.py`, `uiprops.py`, `to_webp.py`, `tools/`.
- Перерендер прочих нынешних спрайтов в 216 px (`food_basic`, `food_lunch`, `care_shampoo`, `care_brush`, `fun_balloon`,
  `fun_bow`, 5 мечт) — рычаг бюджета для A1h, не здесь.
- Сломанная лампа и «до / после починки» (С2 в `eventsOff`, срез 2 — GAME_CONCEPT.md:1885), `care_doctor` (С4, срез 2),
  `fun_bow` (в лавках нет), выпечка (1b4), `tile_bomb` (A1h), стол для `spot_3` (BACKLOG от A1d1), своя тень вещей
  помимо ловцов оболочки `room.SHADOW` (№ 79), вечерние и ночные версии, анимация (вертушка не крутится), контуры пустых мест,
  доски и ценники витрины (`shopprops.py`, A1s), новая картинка `gift_toy`.

## БЮДЖЕТ
- **`props.py`: вставок ≤ 260, удалений ≤ 30** — `git diff -w --numstat BASE -- finny-pet/tools/art/props.py`. Оценка ≈ 210 / ≈ 22:
  9 новых построителей вещей × ≈ 8 = 72; 9 новых товарных с `super_can` ≈ 76; ванна ≈ 10 (тело витаминок −15); домик ≈ 2 (−2);
  `render_thing` ≈ 22; `ROOM`, `ANCHOR`, константы ≈ 10; `PROPS`, `VIEWS` ≈ 8; `import room` 1; CLI 3 (−3); докстринг ≈ 8 (−2).
  Превышение — вопрос оркестратору до приёмки.
- **Доля A1e в бюджете APK** (записывается в TOWN-A1.md коммитом BASE по ответам № 93, № 94; на A1s, A1e, A1h и код остаётся
  364 957 Б, TOWN-A1.md:63-64):
  - худший спрайт 216 × 216: 46 656 px × 0,9 × 0,268463 Б/px (p75 плотности 45 RGBA-спрайтов res, TOWN-A1d1.md:264-265) =
    **11 273 Б**; замер прототипа (журнал): вещи камерой комнаты 2 784–5 166 Б; нынешние `item_*`/`goal_*`, пережатые 512 → 216
    теми же параметрами, — 6 032–8 554 Б;
  - заменяются 4 файла: `item_fun_ball` 11 414 + `item_fun_book` 13 224 + `item_fun_tent` 14 458 + `item_care_vitamins` 38 478 =
    **77 574 Б**; dex, arsc, заголовки A1e2 — **6 000 Б** (≈ 20 веток `when`, как A1d, A1g — TOWN-A1f.md:139);
  - **доля A1e ≤ 176 432 Б** (22 × 11 273 − 77 574 + 6 000; 48,3 % от 364 957); на A1s, A1h и код останется **188 525 Б**. При
    № 93 б (19 файлов) — ≤ 142 613 Б, останется 222 344 Б. Ожидаемо ≈ +52 КБ (12 × ≈ 4,5 + 10 × ≈ 7 − 77,6 + 6); доля —
    потолок, не резерв: после A1e2 записывается факт, разница возвращается в остаток;
  - **критерий приёмки:** Σ N WebP ≤ N × 11 273 — **248 006 Б** для 22 (№ 93 б — 19 ≤ 214 187; заход товаров № 95 б — 10 ≤ 112 730,
    с № 93 б — 8 ≤ 90 184; таблица ACCEPTANCE), каждый ≤ 37 888 (TOWN-A1.md:68-69).
    Превышение — вопрос оркестратору; quality 88 и 216 px молча не менять.
- **Время** (HIP, прототип): вещь — рендер части кадра с запуском Blender ≈ 3 с при 32 сэмплах; 22 спрайта при 96 ≈ 2–3 мин;
  12 дампов при 1 сэмпле ≈ 1 мин; регресс (21 прогон + 7 `room_furn_*`, после A1g1 — ещё `street`) ≈ 6 мин.

## ДО СПАВНА (оркестратор)
Обозначения, как TOWN-A1d1.md:276-280: `$BL`; обе в форме `C:/…` (путь `/c/…` во вшитых строках Python и Blender не открывается):
`T=$(cygpath -m <scratch сессии, которая спавнит кодера>)`, `W` — корень дерева кодера: вариант A — `W=$(cygpath -m "$(pwd)")`, вариант B
— `W=$(cygpath -m "$(pwd)/wt/a1e")` (из корня основного дерева); после п. 2 `python -c "import os;print(os.path.isdir(r'$T'), os.path.isdir(r'$W'))"`
→ `True True`.
0. **Условия.**
   - Ответы № 93–95 есть (они меняют объём и порядок, листа не требуют). Без них A1e1 не спавнится.
   - `ls -A $T/a1e 2>/dev/null | wc -l` → 0 (в scratch сессии сведения `a1e/` занят черновиками); иначе одной заменой взять свежее
     имя каталога (например, `a1e_run`) во всех путях `$T/a1e` разделов «До спавна» и ACCEPTANCE.
   - A1d1 принят и слит (TOWN-A1d1.md:496-511): `git log --oneline -1 -- finny-pet/tools/art/room.py` → `A1d1: …`; API:
     `python -c "import ast,sys;t=ast.parse(open(sys.argv[1],encoding='utf-8').read());f={n.name:[a.arg for a in n.args.args] for n in t.body if isinstance(n,ast.FunctionDef)};v={x.id for n in t.body if isinstance(n,ast.Assign) for x in n.targets if isinstance(x,ast.Name)};print(f.get('palette'),f.get('light_room'),f.get('room_camera'),f.get('sprite_frame'),sorted({'K','WALL_Y','SHADOW'}-v))" finny-pet/tools/art/room.py`
     → `[] ['scene', 'evening', 'lamp_xy'] ['scene', 'portrait'] ['scene', 'objs', 'w', 'h', 'floor', 'top'] []` (так в wt/a1d
     b536480; на HEAD — `None ['scene', 'evening', 'lamp_xy'] None None ['K', 'SHADOW']` — самопроверка). Иначе оркестратор
     правит CONTRACT п. 4 коммитом в спеку до спавна, не кодер. В `tools/art_check.py` — 7 прогонов `room_furn_<id>`.
   - Тень (№ 79): `grep -c '^SHADOW = False' finny-pet/tools/art/room.py` → 1 (зонд флаги тени не видит: дамп art_check.py пишет
     только `visible_shadow`). 0 (№ 79 а, тень запечена в мебель) — оркестратор до спавна отдельным коммитом правит спеку:
     критерий края в `png_bad` и мутант `soft` — по правилу края для тени из круга A1d1-2 (TOWN-A1d1.md:829-831); CONTRACT п. 4.5
     не трогать — вещи повторяют `room.SHADOW`. Вид вещей при «а» автоматически не решается (спрайт общий с полкой лавки,
     белыми карточками и копилкой — № 28 а): в К2 и К3 — вещь с тенью и без (без тени — копия props.py с `room.SHADOW` →
     `False` sed-ом, только для листа), подвопрос к № 96, рекомендация — по листу.
   - При № 95 б и круге A1d1-2 — заход товаров от feat/town до слияния A1d1: CONTRACT п. 1, 5–11 (`ROOM = {}`, без
     `render_thing` и `import room`); строка «заход товаров» таблицы ACCEPTANCE; в п. 2 — `len(props.ROOM)==0` без
     `render_thing`; в `d0` нет `room_furn_chest.json` (регресс — 21 прогон, exit 0); самопроверка `a1e_check` в п. 6 —
     `python $T/a1e/a1e_check.py $T/a1e/e $T/a1e/e x --goods-only` → `A1E FAIL […]` из 10 `no png` (с `--no-slice2` — 8; FURN_DUMP
     при `--goods-only` не открывается); вещи — вторым заходом от нового BASE после слияния A1d1 (этот раздел заново).
   - `props.py` не правит ни один worktree: `git worktree list`; у `wt/j1b` `git -C wt/j1b status --porcelain` → пусто.
1. **Коммит BASE** (файлы поимённо): `docs/tasks/TOWN-A1e.md` (ответы № 93–95 — в раздел вопросов);
   `docs/tasks/TOWN-A1.md` — :43 «A1e1 — генератор ([TOWN-A1e](TOWN-A1e.md)), A1e2 — встройка», абзац пилота :63-64 — «доля A1e
   ≤ 176 432 Б (или по № 93 б / № 94); на A1s, A1h и код остаётся 188 525 Б», :72 — «стартовые вещи — `item_<homeItemId>`,
   плакаты событий — `poster_<itemId>`», :81-82 — при № 93 а четыре товара из ANTI-SCOPE убрать; `docs/HANDOFF.md` — строка
   A1e (дерево кодера, строка таблицы ACCEPTANCE). Проверка: `git diff --name-only BASE~1 BASE -- finny-pet/tools tools` → пусто.
2. **Дерево и скоуп** — SCOPE (вариант B — сначала `git worktree add -b pilot/a1e wt/a1e BASE && git -C wt/a1e cherry-pick 8dbfd2d`,
   `git -C wt/a1e diff --name-only BASE` → ровно `.claude/agents/coder.md`); пересечения и покрытие → `[] []` и
   `['finny-pet/tools/art/props.py']`.
3. **Зонды** — тексты в «Зонды приёмки»; извлечь (Git Bash, из корня основного дерева):
   ```bash
   mkdir -p $T/a1e && python -c "import re,sys;t=open('docs/tasks/TOWN-A1e.md',encoding='utf-8').read();[open(sys.argv[1]+'/'+m.group(2),'w',encoding='utf-8').write(m.group(1)) for m in re.finditer(r'^\`\`\`python\n(# (\w+\.py) — .*?)^\`\`\`',t,re.S|re.M)]" $T/a1e && ls $T/a1e/*.py
   ```
   → `a1e_check.py kept_check.py`.
4. **BASE-копия и дампы:** `mkdir -p $T/a1e/base && git archive BASE finny-pet/tools/art | tar -x -C $T/a1e/base`;
   `python tools/art_check.py regress $T/a1e/base/finny-pet/tools/art $T/a1e/d0` → exit 0 (число прогонов не сверяется: regress
   выходит с 1 на первом сбое, art_check.py:123-124; после A1g1 добавится `street`, TOWN-A1g1.md:441); в `d0` есть
   `room_furn_chest.json` (кроме захода товаров № 95 б).
5. **Самопроверка регресса** (WORKFLOW № 27): `cp -r $T/a1e/base $T/a1e/mut && sed -i 's/"red": "#E63946"/"red": "#F63946"/' $T/a1e/mut/finny-pet/tools/art/props.py && grep -c '"red": "#F63946"' $T/a1e/mut/finny-pet/tools/art/props.py`
   → 1; `python tools/art_check.py regress $T/a1e/mut/finny-pet/tools/art $T/a1e/dm; python tools/art_check.py diff $T/a1e/d0 $T/a1e/dm | grep -E 'разница: [1-9]|DUMP DIFF'`
   → ровно `props_tile_apple.json … разница: 1 …` и `DUMP DIFF: 1 file(s)` (красный — только яблоко и вишня кекса,
   props.py:280, :405; кекс вне регресса). Иначе — чинить регресс, не спавнить.
6. **Самопроверки зондов** (обязаны уметь покраснеть):
   - `mkdir -p $T/a1e/e && python $T/a1e/a1e_check.py $T/a1e/e $T/a1e/e $T/a1e/d0/room_furn_chest.json` → `A1E FAIL ['item_fun_rug: no png', …]`,
     exit 1, без `SELFCHECK FAIL` (встроенные мутанты PNG — край, мягкая тень α 20, мелкий, белый, RGB, 256 px, пустой; дампа —
     lens, солнце, мир, доска пола, нет `wall_back`, непрозрачный кадр, аспект, якорь, неравный масштаб, пустышка без детей,
     кривая, не пустышка (MESH), пустышка не по имени вещи, поворот, цветовой режим RGB, второй свет `key.001`, лишний свет
     `rim`, свет-ребёнок вещи, экспозиция, AgX, повторная оболочка `plank.999`);
   - ветки основного цикла «no dump» и «лишние»: `mkdir -p $T/a1e/x && python -c "from PIL import Image;[Image.new('RGBA',(216,216)).save('$T/a1e/x/'+n+'.png') for n in ('item_fun_rug','zzz')]" && python $T/a1e/a1e_check.py $T/a1e/x $T/a1e/x $T/a1e/d0/room_furn_chest.json | grep -o "item_fun_rug: no dump\|лишние \['zzz'\]" | sort -u`
     → ровно две строки;
   - тот же вызов, что первый, с `$T/a1e/d0/room_port_day.json` → `SELFCHECK FAIL: FURN_DUMP — не дамп спрайта мебели A1d1`;
   - `python $T/a1e/kept_check.py $T/a1e/base/finny-pet/tools/art/props.py $T/a1e/base/finny-pet/tools/art/props.py` → `KEPT OK`
     (встроенные мутанты — цвет выпечки, значение и новый ключ `C`, пропавший построитель, строка `VIEWS`, `fit_camera`, книжка
     вне `PROPS`, домик ящиком, радиус домика 1,4, домик без полос — краснеют; ванна `alpha=1.0`, дверь домика, hex и кант
     домика, `import room`, CLI п. 9 в блоке `__main__`, новые пары `PROPS` и докстринг — нет).
   Прогнано при сведении (журнал) — тексты ниже дают тот же вывод.
7. **Копии кодеру** в его scratch (`cp`; Write вне репозитория кодеру запрещён, guard-paths.js:102): `a1e_check.py`,
   `kept_check.py`, `$T/a1e/d0/room_furn_chest.json` → `<sc>/d0/`, `$T/a1e/base/finny-pet/tools/art/props.py` → `<sc>/props_base.py`.
8. **Снимок дерева** (оба варианта, из корня основного дерева): `git status --porcelain -uall -- tools finny-pet/tools > $T/a1e/main0.txt`
   (без путей листы п. 10 и доки оркестратора дали бы ложный красный; записи вне `tools/` ловят п. 1 и хук). Правки оркестратора
   в `tools/` и `finny-pet/tools/` между снимком и приёмкой — коммитом до приёмки, иначе снимок переснять с записью в журнал.
9. **Самотест хука** зонд-агентом `agentType: 'coder'` (вариант B — из сессии worktree) — таблица TOWN-A1d1.md:352-372 с заменой:
   строка 1 — Edit `finny-pet/tools/art/props.py` (пропуск), 2 — Write `finny-pet/tools/art/room.py` (блок «не входит в allow»),
   5 и 7 — `props.py` вместо `room.py`; строки 2, 3, 6, 7, 9, 10 обязаны дать блок. Восстановление —
   `git checkout -- finny-pet/tools/art/props.py docs/HANDOFF.md`, снимок п. 8 совпадает.
10. **Очередь видеокарты:** перед рендером `tasklist | grep -ci '^blender\.exe'` → ≤ 1.
11. **Спавн:** coder, Opus, effort xhigh (`agentType: 'coder'`); Blender 5.2 `-b --factory-startup --python-exit-code 1`. В промпт:
    CONTRACT, SCOPE с «Самопроверкой кодера», ANTI-SCOPE, ПРИ БЛОКЕРЕ, правило № 29, абсолютные пути scratch, `BL`, копий п. 7;
    ответы № 93–95; «зонды — критерий CONTRACT п. 2–8; FAIL зонда при сдаче — не DONE».

## ACCEPTANCE (оркестратор, из корня дерева кодера; Git Bash; без `bc`)
`NAMES=item_fun_rug,item_fun_starlamp,item_fun_picture,item_fun_robot,item_fun_kite,item_fun_spinner,item_fun_ball,item_fun_book,item_fun_tent,item_home_flower,item_home_armchair,item_home_lamp,item_food_porridge,item_food_super,item_care_soap,item_care_shampoo_simple,item_fun_icecream,item_care_vitamins,goal_camp,poster_food_super,item_fun_carousel,item_gift_card`
`ROOMN="item_fun_rug item_fun_starlamp item_fun_picture item_fun_robot item_fun_kite item_fun_spinner item_fun_ball item_fun_book item_fun_tent item_home_flower item_home_armchair item_home_lamp"`
(первые 12 имён `NAMES` — `ROOM`; последние 10 — товары, `NAMES_G`). Вариант по ответам — одна строка таблицы во всех пунктах ниже
и в «Самопроверке кодера»:

| Заход | `NAMES` | `ROOMN` (дампы п. 5) | флаги `a1e_check` | `N` | `cap` п. 8 (N × 11 273) |
|---|---|---|---|---|---|
| № 93 а, № 95 а | 22 имени | 12 | — | 22 | 248 006 |
| № 93 б | без `item_fun_spinner`, `item_fun_carousel`, `item_gift_card` | 11, без `item_fun_spinner` | `--no-slice2` | 19 | 214 187 |
| № 95 б, заход товаров | `NAMES_G` | — (дампов нет) | `--goods-only` | 10 | 112 730 |
| № 93 б + № 95 б | последние 8 из 19 (без карусели и открытки) | — | `--goods-only --no-slice2` | 8 | 90 184 |

`N` — файлов в `spr`, `A1E OK N`, `bbox` N × `OK`, строк `RGBA` в п. 8. При `--goods-only` FURN_DUMP не открывается — третьим
аргументом `x`.
1. **Изоляция.**
   - `git status --porcelain -uall -- tools finny-pet/tools | diff - $T/a1e/main0.txt` (из корня основного дерева, оба варианта) →
     вариант A — ровно `< M finny-pet/tools/art/props.py` (плюс заголовок `diff` вида `NaM`); вариант B — пусто, а
     `git -C wt/a1e status --porcelain -uall` → ровно ` M finny-pet/tools/art/props.py`.
   - Вариант A: `git diff --name-only BASE -- finny-pet tools` → ровно `finny-pet/tools/art/props.py`. Вариант B:
     `git -C wt/a1e diff --name-only BASE` (без путей: видит и закоммиченное на pilot/a1e вне `finny-pet`/`tools`) → ровно
     `.claude/agents/coder.md`, `finny-pet/tools/art/props.py`.
   - `git diff -w --numstat BASE -- finny-pet/tools/art/props.py` → вставок ≤ 260, удалений ≤ 30.
   - `node .claude/hooks/assert-oracle-intact.js` → exit 0.
   - Константы: `grep -cE '^(ITEM_PX = 216|ROOM_FILL = 0\.9)\b' finny-pet/tools/art/props.py` → 2.
2. **Импорты и импорт без побочных эффектов.**
   - `python -c "import ast,sys;t=ast.parse(open(sys.argv[1],encoding='utf-8').read());m={a.name.split('.')[0] for n in ast.walk(t) if isinstance(n,ast.Import) for a in n.names}|{n.module.split('.')[0] for n in ast.walk(t) if isinstance(n,ast.ImportFrom) and n.module};print(sorted(m-{'sys','os','math','argparse','random','lib','room','bpy','bpy_extras','mathutils'}))" finny-pet/tools/art/props.py`
     → `[]`; самопроверка — та же на `finny-pet/tools/art/place.py` → `['props']` (прогнано).
   - `mkdir -p $T/a1e/imp && cd $T/a1e/imp && "$BL" -b --factory-startup --python-exit-code 1 --python-expr "import sys,bpy; sys.path.insert(0, r'$W/finny-pet/tools/art'); n=(len(bpy.data.objects),len(bpy.data.materials)); import lib, props; assert callable(props.render_thing) and props.ITEM_PX==216 and set(props.ROOM)<=set(props.PROPS) and len(props.ROOM)==12; m=(len(bpy.data.objects),len(bpy.data.materials)); assert m==n, m; print('IMPORT OK', n)" -- --only tile_apple --out $T/a1e/leak.png; echo rc=$?; cd -`
     → `IMPORT OK`, `rc=0`; `ls -A $T/a1e/imp` → пусто; `$T/a1e/leak.png` нет (12 — число имён `ROOMN`: при № 93 б 11; заход
     товаров — `len(props.ROOM)==0` без `render_thing`). Прогнано на прототипе: `IMPORT OK (3, 2)`, `rc=0`.
   - Самопроверка: `mkdir -p $T/a1e/impm && cp finny-pet/tools/art/lib.py finny-pet/tools/art/room.py finny-pet/tools/art/props.py $T/a1e/impm/ && echo 'item_fun_ball()' >> $T/a1e/impm/props.py`,
     та же команда с `$T/a1e/impm` → `rc=1`, `AssertionError`, не `ImportError`.
2а. **Контракт по AST** (буквы, `super_can`, флаг `floor` — CONTRACT п. 4.7, 5, 8; зонды PNG и дампа их не видят):
   `python -c "import ast,sys;u=ast.unparse;t=ast.parse(open(sys.argv[1],encoding='utf-8').read());f={n.name:n for n in t.body if isinstance(n,ast.FunctionDef)};C=lambda fn,nm:[c for c in ast.walk(f[fn]) if isinstance(c,ast.Call) and u(c.func)==nm] if fn in f else [];print([u(c)[:50] for c in ast.walk(t) if isinstance(c,ast.Call) and (u(c.func) in ('text3d','bpy.ops.object.text_add') or u(c.func)=='sign' and (len(c.args)>4 or any(k.arg=='text' for k in c.keywords)))]+['FONT' for c in ast.walk(t) if isinstance(c,ast.Constant) and c.value=='FONT'],[k for k in ('item_food_super','poster_food_super') if not C(k,'super_can')]+([] if 'super_can' in f else ['no def']),[u(k.value) for c in C('render_thing','room.sprite_frame') for k in c.keywords if k.arg=='floor'])" finny-pet/tools/art/props.py`
   → `[] [] ["ROOM[name] != 'wall'"]` (заход товаров — `[] [] []`); доска `sign` без текста законна (как facade.py:77).
   Самопроверка (прогнано): на `$T/a1e/base/…/props.py` → `[] ['item_food_super', 'poster_food_super', 'no def'] []`; на
   мутанте с `sign(…, "Супер")`, `text3d(…)` и плакатом без `super_can` → оба вызова в первом списке, `['poster_food_super']`.
3. **Прежнее цело.** `python $T/a1e/kept_check.py $T/a1e/base/finny-pet/tools/art/props.py finny-pet/tools/art/props.py` → `KEPT OK`.
4. **Регресс.** `rm -rf $T/a1e/d1 && python tools/art_check.py regress finny-pet/tools/art $T/a1e/d1` → exit 0;
   `python tools/art_check.py diff $T/a1e/d0 $T/a1e/d1 | tail -1` → `DUMP DIFF EMPTY` (`room_*`, `room_furn_*`, `place_*`,
   `facade_*`, питомцы, три `props_*` — `import room` и новый код их не меняют; прототип — журнал).
5. **Рендеры и дампы** (очередью; в выводе `cycles device: HIP`):
   - `"$BL" -b --factory-startup --python-exit-code 1 -P finny-pet/tools/art/props.py -- --only "$NAMES" --out $T/a1e/spr` → exit 0;
     `ls $T/a1e/spr | wc -l` → `N`;
   - `mkdir -p $T/a1e/sd && for n in $ROOMN; do DUMP_OUT=$T/a1e/sd/$n.json "$BL" -b --factory-startup --python-exit-code 1 -P finny-pet/tools/art/props.py --python tools/art_check.py -- --only $n --out $T/a1e/sd/$n.png --samples 1 | grep -c DUMPED; done`
     → по `1` на имя `ROOMN` (12 или 11; заход товаров — пункт пропускается).
6. **Зонд спрайтов:** `python $T/a1e/a1e_check.py $T/a1e/spr $T/a1e/sd $T/a1e/d0/room_furn_chest.json <флаги>`
   → `A1E OK N`: 216 × 216 RGBA, край по α, доля ≥ 0,60, кромка на белом ≥ 3,0 (мяч и книжка печатаются), дамп вещи = мир
   мебели (свет, камеры, вид целиком, вне вещи — только оболочка), пустышка на якоре, масштаб единый, кривых нет, лишних
   файлов нет. Строки «fill / кромка / зазор снизу» — в журнал (вход A1e2 и A1s). Край ещё раз:
   `python tools/art_check.py bbox $T/a1e/spr/*.png` → `N` × `OK`; самопроверка —
   `bbox $T/a1e/d0/room.png` → `FAIL`.
7. **Новые цвета:** `comm -23 <(grep -oiE '#[0-9a-f]{6}' finny-pet/tools/art/props.py | tr a-f A-F | sort -u) <(git show BASE:finny-pet/tools/art/props.py | grep -oiE '#[0-9a-f]{6}' | tr a-f A-F | sort -u)`
   — список подписью К1 (самопроверка на прототипе с `#ABCDEF` → ровно `#ABCDEF`).
8. **WebP и бюджет.** `python finny-pet/tools/art/to_webp.py $T/a1e/spr --dst $T/a1e/w` → `N` строк `RGBA`;
   `stat -c '%s %n' $T/a1e/w/*.webp | awk -v cap=<cap> -v n=<N> '{s+=$1; if ($1 > 37888) b=b" "$2} END {print NR, s, (NR == n && s <= cap && b == "" ? "BUDGET OK" : "BUDGET FAIL" b)}'`
   (`cap`, `N` — строка таблицы) → `N <S> BUDGET OK`; самопроверка — `cap=1000` → `BUDGET FAIL`. Прирост `S − 77 574` (заход
   товаров — `S − 38 478`, заменяется только ванна) — в журнал и к № 94.
9. **Докстринг:** `python -c "import ast,sys;d=ast.get_docstring(ast.parse(open(sys.argv[1],encoding='utf-8').read()));print([s for s in ['ROOM','ANCHOR','ITEM_PX','render_thing','room_camera','sprite_frame','item_home_','poster_','--only item_fun_rug'] if s not in d])" finny-pet/tools/art/props.py`
   → `[]`; самопроверка на `$T/a1e/base/…/props.py` → непустой список.
10. **Лист и судьи** — кадры и вопрос № 96 (раздел вопросов). Листы — `tools/sheets.py`, файлы `finny-pet/screenshots/town/a1e_<n>_*.jpg`.
11. Снова п. 1 (со снимком п. 8 по путям `tools finny-pet/tools` листы п. 10 его не трогают).

**Слияние / коммит** (после № 96; при «круге» — после A1e1-2). Вариант A — код уже в feat/town: коммит `A1e1: …`; вариант B —
`git checkout pilot/a1e -- finny-pet/tools/art/props.py`, коммит `A1e1: …`, ветку не мержить, `coder.md` не переносить. Тем же
коммитом в `tools/art_check.py` — 2 прогона по одному (`--only … --samples 1 --out …`): `props_item_fun_robot` (камера комнаты
со стороны props под регрессом) и `props_item_food_super` (новый товар), докстринг :4-7; при № 95 б в первом коммите — только
`props_item_food_super`, `props_item_fun_robot` — коммитом второго захода (вещи). Эталон после коммита — `$T/a1e/d1`
+ `$T/a1e/sd/item_fun_robot.json` под именем `props_item_fun_robot.json` + `props_item_food_super.json` (снять тем же
`DUMP_OUT=… --only item_food_super --samples 1`; сэмплы в дамп не входят, art_check.py:92-94); `regress` на feat/town и `diff`
с эталоном → разница 0; путь — в HANDOFF.

## Зонды приёмки (тексты)
Первая строка блока — имя файла; извлечение — «До спавна» п. 3. Прогнано при сведении и после круга критиков (журнал): `a1e_check.py`
— на 5 вещах, отрендеренных прототипом `render_thing` с room.py из wt/a1d, и синтетике до 22 → `A1E OK 22`; мутанты краснеют.

```python
# a1e_check.py — TOWN-A1e1: спрайты вещей и товаров. python a1e_check.py PNG_DIR DUMP_DIR FURN_DUMP [--goods-only] [--no-slice2] → A1E OK N / A1E FAIL […], exit 0/1
# PNG: RGBA 216 × 216; край по α > 0 (как art_check.py bbox): углы α 0, bbox ≥ 2 px от краёв; не мелкий — большая доля оси по α > 127
# ≥ 0,60 (кадр — по углам bbox: шар 0,75, нынешние item_*/goal_* 0,64–0,89); кромка на белом — медиана контраста WCAG кольца 6 px
# (евклидово, WORKFLOW № 44) маски α > 127 к #FFFFFF ≥ 3,0 (№ 66 б); мяч и книжка (построители декора place.py/facade.py) — печатаются.
# Дамп вещи (ROOM): оболочка, камера, свет и мир — как у спрайта мебели A1d1 (FURN_DUMP = room_furn_chest.json): тот же набор света и
# камер, вид (__view__) целиком, вне поддерева пустышки — только оболочка; кадр прозрачный RGBA, аспект 0,5625; пустышка с именем вещи (как place.put) — в ANCHOR слота, поворот 0, масштаб единый, у неё есть дети; кривых вне
# оболочки нет (sprite_frame кадрирует по bound_box). Пары похожих машиной не судятся (круг и квадрат дают IoU 0,785) — только судьи.
import copy, json, os, re, sys, numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt as edt
PX, FILL, EDGE = 216, 0.60, 3.0
ROOM = {"item_fun_rug": "rug", "item_fun_starlamp": "table", "item_fun_picture": "wall", "item_fun_robot": "floor",
        "item_fun_kite": "wall", "item_fun_spinner": "table", "item_fun_ball": "floor", "item_fun_book": "table",
        "item_fun_tent": "floor", "item_home_flower": "floor", "item_home_armchair": "floor", "item_home_lamp": "wall"}
GOODS = ["item_food_porridge", "item_food_super", "item_care_soap", "item_care_shampoo_simple", "item_fun_icecream",
         "item_care_vitamins", "goal_camp", "poster_food_super", "item_fun_carousel", "item_gift_card"]
SLICE2 = {"item_fun_spinner", "item_fun_carousel", "item_gift_card"}                          # № 93 б — без них
ANCHOR = {"floor": (0.0, -1.0, 0.0), "table": (0.0, -1.0, 0.0), "wall": (0.0, 2.99, 2.25), "rug": (0.0, -5.0, 0.0)}
REUSED = {"item_fun_ball", "item_fun_book"}                                                  # place.py:40, :42; facade.py:106, :121
SHELL = {"plank", "wall_back", "side", "skirt_s", "skirt", "cam", "cam_t", "key", "key_t", "fill", "fill_t", "sun", "sun_t", "__world__"}
N = lambda s: re.sub(r"\.\d{3}$", "", s)


def alpha(im): return np.asarray(im.getchannel("A"))
def lin(c): c = c / 255.0; return np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)


def edge(im):
    a = np.asarray(im).astype(float); m = a[..., 3] > 127; ring = m & (edt(m) <= 6)
    L = 0.2126 * lin(a[..., 0]) + 0.7152 * lin(a[..., 1]) + 0.0722 * lin(a[..., 2])
    return float(np.median(1.05 / (L[ring] + 0.05))) if ring.any() else 0.0


def png_bad(n, im, say=False):
    if im.mode != "RGBA": return ["mode " + im.mode]
    if im.size != (PX, PX): return ["size %s" % (im.size,)]
    A = alpha(im); m = A > 127
    if not m.any(): return ["empty"]
    bad, (ey, ex) = [], np.where(A > 0)
    if max(A[0, 0], A[0, -1], A[-1, 0], A[-1, -1]) or min(ex.min(), ey.min(), PX - 1 - ex.max(), PX - 1 - ey.max()) < 2:
        bad.append("edge bbox a>0 %s" % ((int(ex.min()), int(ey.min()), int(ex.max()), int(ey.max())),))
    ys, xs = np.where(m); f = max(xs.max() - xs.min() + 1, ys.max() - ys.min() + 1) / PX
    if f < FILL: bad.append("fill %.2f" % f)
    e = edge(im)
    if e < EDGE and n not in REUSED: bad.append("white edge %.2f" % e)
    if say: print("   %-26s fill %.2f  кромка на белом %.2f : 1  зазор снизу %3d px%s" % (n, f, e, PX - 1 - ys.max(), "  (декор place/facade — не валит)" if n in REUSED else ""))
    return bad


def room_bad(n, D, R):
    bad = ["shell/cam/light %s" % k for k in sorted(R) if N(k) in SHELL and D.get(k) != R[k]][:4]
    lc = lambda M: {k for k, o in M.items() if isinstance(o, dict) and o.get("type") in ("LIGHT", "CAMERA")}
    if lc(D) != lc(R): bad.append("lights/cams %s" % sorted(lc(D) ^ lc(R)))    # второй light_room, свет в лампочке — мимо мира мебели
    if D.get("__view__") != R["__view__"]: bad.append("view %s" % D.get("__view__"))  # прозрачный RGBA 0,5625 — проверен у FURN_DUMP
    e = D.get(n)
    if not e or e.get("type") != "EMPTY": return bad + ["no empty %s" % n]
    if max(abs(a - b) for a, b in zip(e["loc"], ANCHOR[ROOM[n]])) > 1e-3: bad.append("anchor %s" % e["loc"])
    if any(abs(x) > 1e-4 for x in e["rot"]) or max(e["scale"]) - min(e["scale"]) > 1e-4: bad.append("rot/scale %s %s" % (e["rot"], e["scale"]))
    if not any(isinstance(o, dict) and o.get("parent") == n for o in D.values()): bad.append("empty without children")
    sub, grow = {n}, True                                                      # всё вне поддерева вещи — ровно оболочка FURN_DUMP
    while grow:
        grow = {k for k, o in D.items() if isinstance(o, dict) and o.get("parent") in sub} - sub; sub |= grow
    out = set(D) - sub - {k for k in R if N(k) in SHELL} - {"__view__"}
    if out: bad.append("outside item %s" % sorted(out)[:3])
    cur = [k for k, o in D.items() if isinstance(o, dict) and o.get("type") == "CURVE" and N(k) not in SHELL]
    return bad + (["curves %s" % cur[:3]] if cur else [])


def selfcheck(R):
    ok = Image.new("RGBA", (PX, PX)); ImageDraw.Draw(ok).ellipse((20, 20, 195, 195), fill=(58, 63, 85, 255))
    assert not png_bad("x", ok), "SELFCHECK FAIL (эталон PNG)"
    muts = {"edge": lambda m: ImageDraw.Draw(m).rectangle((0, 100, 5, 110), fill=(58, 63, 85, 255)),
            "soft": lambda m: ImageDraw.Draw(m).rectangle((0, 212, 215, 215), fill=(0, 0, 0, 20)),        # мягкая тень до края, α 20
            "small": lambda m: m.paste((0, 0, 0, 0), (0, 0, PX, PX)) or ImageDraw.Draw(m).ellipse((80, 80, 190, 190), fill=(58, 63, 85, 255)),
            "white": lambda m: ImageDraw.Draw(m).ellipse((20, 20, 195, 195), fill=(250, 248, 240, 255))}
    for k, f in muts.items():
        m = ok.copy(); f(m); assert png_bad("x", m), "SELFCHECK FAIL: мутант PNG %s не покраснел" % k
    for k, m in (("rgb", ok.convert("RGB")), ("size", ok.resize((256, 256))), ("empty", Image.new("RGBA", (PX, PX)))):
        assert png_bad("x", m), "SELFCHECK FAIL: мутант PNG %s не покраснел" % k
    w = ok.copy(); muts["white"](w); assert not png_bad("item_fun_ball", w), "SELFCHECK FAIL: белый мяч (декор place/facade) валит"
    if R is None: return
    v = R.get("__view__", {})
    assert "wall_back" in R and v.get("film_transparent") and v.get("color_mode") == "RGBA" and v.get("aspect") == 0.5625,         "SELFCHECK FAIL: FURN_DUMP — не дамп спрайта мебели A1d1"
    S = {k: copy.deepcopy(v) for k, v in R.items() if N(k) in SHELL}; S["__view__"] = dict(R["__view__"])
    S["item_fun_robot"] = {"type": "EMPTY", "loc": list(ANCHOR["floor"]), "rot": [0.0, 0.0, 0.0], "scale": [0.6] * 3, "parent": None}
    S["robot_body"] = {"type": "MESH", "parent": "item_fun_robot"}
    assert not room_bad("item_fun_robot", S, R), "SELFCHECK FAIL (эталон дампа): %s" % room_bad("item_fun_robot", S, R)
    dm = {"lens": lambda M: M["cam"]["cam"].update(lens=37.0), "sun": lambda M: M["sun"]["light"].update(energy=9.0), "world": lambda M: M["__world__"].update(strength=0.22),
          "plank": lambda M: M["plank"]["mats"][0].update(Roughness=0.9), "wall": lambda M: M.pop("wall_back"),
          "opaque": lambda M: M["__view__"].update(film_transparent=False), "aspect": lambda M: M["__view__"].update(aspect=1.0),
          "anchor": lambda M: M["item_fun_robot"]["loc"].__setitem__(1, 3.0), "scale": lambda M: M["item_fun_robot"].update(scale=[0.6, 0.6, 1.0]),
          "orphan": lambda M: M.pop("robot_body"), "curve": lambda M: M.update(string={"type": "CURVE", "parent": "item_fun_robot"}),
          "type": lambda M: M["item_fun_robot"].update(type="MESH"), "rename": lambda M: M.__setitem__("robot", M.pop("item_fun_robot")),
          "rot": lambda M: M["item_fun_robot"].update(rot=[0.0, 0.0, 0.1]), "cmode": lambda M: M["__view__"].update(color_mode="RGB"),
          "key2": lambda M: M.update({"key.001": copy.deepcopy(M["key"])}), "rim": lambda M: M.update(rim={"type": "LIGHT", "parent": None}),
          "bulb": lambda M: M.update(bulb={"type": "LIGHT", "parent": "item_fun_robot"}), "exposure": lambda M: M["__view__"].update(exposure=0.35),
          "agx": lambda M: M["__view__"].update(view="AgX"), "plank2": lambda M: M.update({"plank.999": copy.deepcopy(M["plank"])})}
    for k, f in dm.items():
        M = copy.deepcopy(S); f(M); assert room_bad("item_fun_robot", M, R), "SELFCHECK FAIL: мутант дампа %s не покраснел" % k


args = [a for a in sys.argv[1:] if not a.startswith("--")]
pd, dd, furn = args
goods_only = "--goods-only" in sys.argv                                        # № 95 б: заход товаров до слияния A1d1
skip = SLICE2 if "--no-slice2" in sys.argv else set()
R = None if goods_only else json.load(open(furn, encoding="utf-8"))
selfcheck(R)
names = [n for n in ([] if goods_only else list(ROOM)) + GOODS if n not in skip]
bad = []
for n in names:
    p = os.path.join(pd, n + ".png")
    if not os.path.exists(p): bad.append("%s: no png" % n); continue
    bad += ["%s: %s" % (n, b) for b in png_bad(n, Image.open(p), True)]
    if n in ROOM:
        j = os.path.join(dd, n + ".json")
        bad += ["%s: %s" % (n, b) for b in (room_bad(n, json.load(open(j, encoding="utf-8")), R) if os.path.exists(j) else ["no dump"])]
extra = sorted(set(f[:-4] for f in os.listdir(pd) if f.endswith(".png")) - set(names))
if extra: bad.append("лишние %s" % extra)
print("A1E OK %d" % len(names) if not bad else "A1E FAIL %s" % bad); sys.exit(bool(bad))
```

```python
# kept_check.py — TOWN-A1e1: прежний props.py цел. python kept_check.py BASE_PROPS NEW_PROPS → KEPT OK / KEPT FAIL […], exit 0/1
# Каждое верхнеуровневое утверждение BASE (ast.dump: импорты, палитра C, помощники, построители, VIEWS, fit_camera, render_prop) есть
# в NEW без изменений, кроме докстринга, словаря PROPS, блока __main__ и построителей ALLOWED (ванна — новое тело, домик — дверь и цвет);
# пары PROPS BASE (имя → построитель) — все в NEW (place.py:62 и facade.py берут props.PROPS[name] декором мест и фасадов).
# Домик: каждое утверждение тела BASE, кроме двери (a = R(32) и утверждение с 'door'), со строками-масками (имена и hex цветов) — в NEW.
# Предел: добавленное в тело (лишний меш, масштаб, hide_render) зонд не видит — «силуэт тот же» судит лист К1.
import ast, sys
ALLOWED = {"item_care_vitamins", "item_fun_tent"}
is_props = lambda n: isinstance(n, ast.Assign) and getattr(n.targets[0], "id", "") == "PROPS"


class Mask(ast.NodeTransformer):
    def visit_Constant(self, n): return ast.Constant("S") if isinstance(n.value, str) else n


def core(text):
    f = [n for n in ast.parse(text).body if isinstance(n, ast.FunctionDef) and n.name == "item_fun_tent"]
    return {ast.dump(Mask().visit(ast.parse(u))) for s in (f[0].body if f else []) for u in [ast.unparse(s)] if "'door'" not in u and u != "a = R(32)"}


def parts(text):
    body = ast.parse(text).body
    skip = lambda n: (isinstance(n, ast.Expr) and isinstance(n.value, ast.Constant) and isinstance(n.value.value, str)) or is_props(n) \
        or (isinstance(n, ast.FunctionDef) and n.name in ALLOWED) or (isinstance(n, ast.If) and "__main__" in ast.dump(n.test))
    stm = {ast.dump(n): getattr(n, "name", None) or ast.get_source_segment(text, n).splitlines()[0][:60] for n in body if not skip(n)}
    props = {k.value: v.id for n in body if is_props(n) for k, v in zip(n.value.keys, n.value.values)}
    for n in body:  # PROPS.update({...}) после словаря — тоже пары
        if isinstance(n, ast.Expr) and isinstance(n.value, ast.Call) and ast.unparse(n.value.func) == "PROPS.update" and isinstance(n.value.args[0], ast.Dict):
            props.update({k.value: v.id for k, v in zip(n.value.args[0].keys, n.value.args[0].values)})
    return stm, props


def bad(base, new):
    (sb, pb), (sn, pn) = parts(base), parts(new)
    return ["changed %s" % v for k, v in sb.items() if k not in sn] + ["PROPS %s" % k for k in pb if pn.get(k) != pb[k]] \
        + ([] if core(base) <= core(new) else ["tent silhouette"])


base, new = (open(p, encoding="utf-8").read() for p in sys.argv[1:3])
assert not bad(base, base), "SELFCHECK FAIL (эталон): %s" % bad(base, base)
for name, m in (("pastry", base.replace('M("#D3822A"', 'M("#D3822B"', 1)),                                 # правка выпечки 1b3
                ("C value", base.replace('"gold": "#F5B400"', '"gold": "#F5B401"', 1)),                    # цвет палитры
                ("C key", base.replace('"navy": "#3A3F55",', '"navy": "#3A3F55", "new": "#123456",', 1)),  # новые цвета — hex в построителе
                ("tile", base.replace("def tile_apple():", "def tile_apple_():", 1)),                      # построитель пропал
                ("views", base.replace('"item_food_basic": (512, 0.9, 30, 36)', '"item_food_basic": (256, 0.9, 30, 36)', 1)),
                ("fit", base.replace("dist = size * 2.2", "dist = size * 2.3", 1)),
                ("props", base.replace(' "item_fun_book": item_fun_book,', "", 1)),                         # книжка ушла из PROPS — place.py упадёт
                ("tent box", base.replace('t = cone("tent", (0, 0, 0.82), 1.05, 1.64, (0, 0, 0), tent)', 't = box("tent", (0, 0, 0.5), (1, 1, 1), tent)', 1)),
                ("tent r", base.replace("1.05, 1.64", "1.4, 1.64", 1)),                                   # силуэт домика
                ("tent stripes", base.replace('    for z, r in ((0.35, 0.83), (0.85, 0.5)):\n        torus("stripe", (0, 0, z), r, 0.045, trim)\n', "", 1))):
    assert m != base and bad(base, m), "SELFCHECK FAIL: мутант %s не покраснел" % name
ok = base.replace("alpha=0.32", "alpha=1.0", 1).replace("a = R(32)  # door", "a = R(0)  # door", 1).replace('tent = M("yellow")', 'tent = M("#E09A00")', 1) \
    .replace("    a = R(0)  # door", '    torus("kant", (0, 0, 0.05), 1.0, 0.05, M("#8A4A00"))\n    a = R(0)  # door', 1) \
    .replace("for n in PROPS: render_prop(n,", "for n in PROPS: (render_thing if n in ROOM else render_prop)(n,", 1) \
    .replace("from mathutils import Vector\n", "from mathutils import Vector\nimport room\n", 1).replace('"""Toy-style', '"""Toy-style props and room things', 1) \
    .replace('    "pastry_pretzel": pastry_pretzel,', '    "item_x": tile_gift, "pastry_pretzel": pastry_pretzel,', 1) + '\nPROPS.update({"item_y": tile_gift})\n'
assert all(s in ok for s in ("alpha=1.0", "render_thing if n in ROOM", '"#E09A00"', '"kant"', "a = R(0)")) and ok.count("import room") == 1 \
    and '"item_x"' in ok and not bad(base, ok), "SELFCHECK FAIL: разрешённая правка краснеет: %s" % bad(base, ok)
r = bad(base, new)
print("KEPT OK" if not r else "KEPT FAIL %s" % r); sys.exit(bool(r))
```

## ВОПРОСЫ НА ВОРОТА (продолжают §18 с № 93; в GATE_QUEUE: № 93–95 — сейчас, без листа; № 96 — по листу A1e)
Заняты: № 89–90 — 1b4, № 91–92 — комната (GATE_QUEUE.md:4). Перед записью в GATE_QUEUE: `grep -n '№ 9[3-6]' docs/tasks/*.md
docs/GAME_CONCEPT.md | grep -v 'TOWN-A1e.md'` → пусто; номер, который другая спека сняла, не переиспользуется.

**До спавна (объём, бюджет, порядок; листа не требуют):**

93. **Четыре товара среза 2 на полках среза 1** (вертушка, карусель, открытка, игрушка-подарок; № 31 а отложил их арт, а
    `Prices.shelf` показывает их с недели 1, Prices.kt:37-45; на витрине A1s названий нет — № 39):
    - а) рисуем сейчас: вертушка — камерой комнаты (она `keep`, ставится «на стол»), карусель и открытка — товарным ракурсом,
      игрушка-подарок — готовая коробка с бантом `tile_gift` (без нового файла); строка ANTI-SCOPE эпика (TOWN-A1.md:81-82) — без них;
    - б) эмодзи до среза 2 (№ 31 как есть; критерий строки A1e «ни одного эмодзи» — без этих четырёх id).
    **Рекомендую а**: на витрине A1s названий нет (№ 39), а строка эпика (TOWN-A1.md:43, записана вместе с № 39) требует
    спрайты всех товаров, потому что «эмодзи ребёнок не узнает». Это не замерено: мерит слепой «ребёнок» на листе № 96 (на
    карточке 40 dp до A1s название под картинкой есть, PlaceScreen.kt:243-246). Эмодзи среди спрайтов вы видели на макетах
    `a1s_mock_1_market.jpg` (🎠) и `a1s_mock_3_foma_p2.jpg` (🌀 💌 🎁) на воротах № 39. Цена — 3 спрайта (≈ 18,5 КБ: 4,5 + 2 × 7,
    как в БЮДЖЕТЕ; диапазон прототипа 15–22 КБ, худшее 33 819 Б) и ≈ 25 строк генератора.
    **Ответ владельца 2026-09-29: а** (по рекомендации; § 18 GAME_CONCEPT) — 22 спрайта, строка таблицы ACCEPTANCE «№ 93 а, № 95 а».
94. **Бюджет эпика +1 МБ** (отложенный вопрос TOWN-A1g1.md:217 «к A1e»). Факт A1c +127 720, A1f +140 009; доли-потолки J1
    70 000 (факт картинок 33 594), A1g ≤ 192 252, A1d ≤ 153 638, A1e ≤ 176 432 (худшее) — на A1s, A1h и весь код эпика
    остаётся 188 525 Б. Код J1 в доле J1 не учтён (она — только арт, TOWN-J1.md:324): release на чистом `app/` после 47f5001
    (`app-game-release.apk` 02:27) — 4 382 302 Б, +33 080 к A1f (4 349 222, TOWN-A1f.md:347), выпечки в нём нет — это код.
    Остаток на A1s, A1h и прочий код — ≈ 155 445 Б. Перед записью в GATE_QUEUE — пересборка `gradlew.bat :app:assembleGameRelease`
    на чистом `app/` (после коммита 1b4) и `stat -c %s` минус 4 349 222. Ожидаемо A1e ≈ +52 КБ при потолке 176 432 — разница
    ≈ 124 КБ вернётся в остаток.
    - а) держать +1 МБ: доли — потолки, после каждой встройки — факт, разница возвращается в остаток; перед A1e2 — замер
      release APK;
    - б) поднять потолок эпика (например, до +1,5 МБ; ТЗ размер APK не ограничивает, потолок — наш).
    **Рекомендую а**: по замерам прототипа запас есть и с кодом J1, факт виден после каждой задачи. Вариант «192 px вместо
    216» визуальный — он на листе К1 и в № 96 б, а не здесь.
    **Ответ владельца 2026-09-29: а** (по рекомендации) — +1 МБ держим; доля A1e ≤ 176 432 Б, остаток 188 525 Б (TOWN-A1.md).
95. **Товары раньше вещей** (только если на воротах № 76–81 A1d1 уходит в круг A1d1-2): «четвёртый генератор» параллельно —
    решение о волне за вами (№ 61: «по итогам пилота — решение о волне»).
    - а) ждать слияния A1d1 — один заход, один лист;
    - б) товары (10 или 8) сразу, параллельно кругу A1d1-2 (≤ 2 Blender на видеокарте), вещи — вторым заходом после слияния
      A1d1; листа два.
    **Рекомендую а**, если A1d1 принят без круга (тогда вопрос снимается); **б**, если круг: товарам `room.py` не нужен, а
    лист товаров придёт раньше.
    **Ответ владельца 2026-09-29: а** (по рекомендации) — один заход после слияния A1d1 (7571ef7); захода товаров нет.

**По листу A1e** (кадры ниже; ответ одной пачкой):

96. **Вещи и товары целиком** (К1–К5):
    - а) принять;
    - б) круг A1e1-2 — только предметы и правки, которые вы назовёте (в том числе ракурс коврика, буквы на плакате, бра,
      192 px вместо 216 — ряд К1); а также входы A1e2 (Kotlin, кругом A1e1-2 не разворачиваются): место плаката до A1s (К3),
      место бра (К2).
    **Рекомендую а**, если слепой «ребёнок» в пачке 64 dp назвал предмет или его отдел у ≥ 18 из 22 (≥ 16 из 19); ни одну вещь
    не назвал именем напарника из столбца «не путать» CONTRACT п. 7 (коврик — «тарелка», картина — «окно», робот — «житель»),
    а у пар из 22 обе вырезки названы верно и по-разному; контрольные пары К5 судья «Доступность» назвал одинаковыми; о плакате
    на «чего от тебя хотят?» ответил «купить» или «хвалят»; в ответе о ванне нет лечения (таблетки, витамины, здоровье), а её
    «до» (нынешняя баночка) названо лекарством или витаминами; стартовые (цветок, кресло, бра) узнаны в пачке 36 dp. Иначе б
    со списком промахов (id и размер). id, где «до» узнан, а спрайт нет, — отдельным списком (кандидаты круга и при а).

**Кадры листа A1e** (фоны настоящие; «после» для лавок, комнаты и копилки — временная сборка в превью-worktree по наброску A1e2,
как 1b3 (TOWN-J1-1b.md:460-469): WebP в res, ветки `itemRes`/`goalRes`, стартовые через `itemRes`, плакат в карточке события;
«до» — та же сборка без правок; эмулятор 360 × 640 при 1,0 и 1,3; S23 заблокирован — кадры S23 «доснять» в GATE_QUEUE):
- **К1 — лист спрайтов.** 22 спрайта 1:1 на шашечке с именами и id; ряд «на фонах» — при 36, 40, 64 и 72 dp на белом, на
  `paperTint` #F4F2F8, на стене и полу оболочки A1d, на вырезках `bg_market_port` и `bg_foma_port`; рядом — нынешние эмодзи и
  старые 512-px `item_fun_ball`/`book`/`tent`/`care_vitamins`; числа доли, кромки и зазора из п. 6; новые цвета (п. 7);
  коврик при якоре −5 и −1 (вариант — копия с `sed` одной цифры, только для листа); `goal_camp` и одна вещь 216 → 72 dp
  рядом с 192 → 72 dp (растяжение ×1,125) — к № 96 б.
- **К2 — комната** (превью-сборка после A1d2 или PIL на оболочке и мебели A1d1 с подписью «макет, не сборка»): неделя 1 — цветок,
  кресло, бра над изголовьем (есть ли наложение с питомцем при 1,0 и 1,3); неделя 5 — три профиля `placed` (А: картина,
  лампа-звёздочка, робот, коврик; Б: змей, книжка, домик, мячик; В: вертушка); 360 × 640 и S23.
- **К3 — лавки:** «до / после» — рынок неделя 1 и неделя 2 с Пк3 (плакат в карточке события; под кадром — на сколько dp
  опустилась полка: верх узла `resource-id="shelf"` недели 2 минус недели 1, px / 3), «У Фомы» неделя 5 обе страницы; и витрина A1s макетом (`a1s_mock_1_market.jpg`, `_2_foma_p1`, `_3_foma_p2`: эмодзи заменены спрайтами 64 dp,
  плакат над банкой; подпись «макет A1s, не сборка»). На полке вещи — анфас светом комнаты, товары — в три четверти
  студийным: следствие № 28 а, видно здесь.
- **К4 — «Обустроить» и мечты:** сундук со всеми купленными; копилка с `goal_camp` 72 dp и мечтой-вещью `item:fun_robot`,
  калитка на улице 48 dp, шапка 28 dp; `goal_camp` рядом с жёлтым домиком-палаткой.
- **К5 — доступность:** К2–К4 в сером и при дейтеранопии; пары столбца «не путать» CONTRACT п. 7 рядом при 36 и 40 dp в сером
  и при дейтеранопии — напарники вне 22 готовыми спрайтами (`furn_window`, `furn_bed`, `tile_star`, питомец, житель,
  `food_basic`, `care_shampoo`); 2–3 контрольные пары (один спрайт дважды; спрайт и его серая копия).

**Судьи** (кадры — им, ключ меток — у оркестратора; WORKFLOW № 35, 42):
- **«Ребёнок» вслепую** — пачки отдельными прогонами, от мелкой к крупной (крупная вырезка не подсказывает мелкую), в каждой
  4 серые фигуры-пустышки того же размера, метки нейтральные:
  (1) 36 dp — вещи комнаты и стартовые на вырезке фона комнаты у своего места (там подписи нет нигде);
  (2) 64 dp — 22 спрайта на настоящем фоне (витрина A1s, № 39);
  (3) материал, а не самопроверка (как TOWN-A1g1.md:432), отдельным экземпляром судьи — «до» тех же id: эмодзи со снимков,
  у четырёх заменяемых — старые 512-px, у бра и плаката сравнения нет.
  Вопросы: «что это? для чего?»; «чтобы питомец поел, помылся, поиграл — или это вещь для дома, подарок, мечта?»; по плакату —
  «чего от тебя хотят?»; по К3 с Пк3 — «какой корм купишь и почему?» (урок не решается картинкой: обе еды — явно корм); пары
  «корм — супер-корм» и «шампунь — шампунь простой» рядом — «это один и тот же предмет?» и «для одного дела?» (урок — «нет»
  и «да»). Ванну сам вопрос не называет: промах — если в ответе о ней есть лечение; контроль — «до» того же id названо
  лекарством, иначе критерий нечувствителен и не довод (WORKFLOW № 35). Самопроверка — только пустышки (≥ 3 из 4 — «не знаю»,
  TOWN-A1d1.md:490-491), иначе вопрос подсказывает — прогон с новой формулировкой. Отчёт — таблица по id: спрайт / «до» / отдел;
  шапка 28 dp — только в отчёт (рядом число).
- **«Доступность».** К5: пары различимы в сером и при дейтеранопии при 36–40 dp («одно и то же или разное?»; если контрольную
  пару назвал «разное» — вердикт по парам не довод); вещь видна на своём фоне (коврик на полу,
  картина, змей и бра на лавандовой стене, кресло не сливается со стеной); свечение ламп в сером не пропадает.
- **«Сцена».** К2, К3: вещи в одном мире с мебелью A1d (свет, ракурс, стоят, а не висят; размер рядом с мебелью и питомцем —
  какая вещь не того размера, по трём профилям К2 при 1,0 и 1,3); полка — лавка, а не карточки (№ 39); не выросло ли текстовое
  окно события над полкой (К3, сдвиг полки); ничего не мигает и не зазывает (стоп-лист № 17).
- **«Урок».** К3: банка супер-корма ярче миски, но обе — корм; плакат — реклама; простой шампунь — тот же вид, что обычный, и
  выглядит проще; мыло, ванна и шампуни — «помыться».

**Решения спеки без вопроса** (сообщить владельцу с листом; генератор разворачивается кругом A1e1-2, места в UI — входы A1e2):
- 216 × 216 px (72 dp × 3) для всех 22; прежние 512-px спрайты, кроме четырёх заменяемых, не трогаем.
- Вещи — камерой комнаты в мире мебели A1d1 (та же оболочка, свет, камера — № 28 а); точки: пол (0; −1; 0), стена — центр
  `spot_2`, x = 0 — анфас, один спрайт для левых и правых мест; каждая вещь увеличена своим масштабом (равным по осям), чтобы
  заполнить 0,9 кадра: взаимные размеры вещей мир не передаёт, на экране размер задаёт место (36 dp).
- Вещи пола и «стола» стоят на низе кадра (`sprite_frame(floor=True)`, как мебель), вещи стены и товары — по центру;
  зазор снизу — в журнал для A1e2 и A1s. Вещи «стола» стоят на полу (стола нет — BACKLOG от A1d1).
- Коврик — с точки пола (0; −5; 0), 29°: плоский овал ≈ 1 : 2,5 читается ковриком; тот же овал при 18° ≈ 1 : 4 — черта
  (К1 показывает обе версии).
- Плакат без букв: «!» — фигурами, слоган «Супер-корм! Все питомцы в восторге!» — живой текст (растёт со шрифтом, читает
  TalkBack): до A1s — в карточке события (вход A1e2; сдвиг полки — на К3; ответ «только в витрине A1s» в № 96 б переносит
  WebP и `posterRes` в коммит A1s: без вызова R8 выкинет файл, TOWN-A1.md:70-71), в A1s — над товаром.
- Бра над кроватью — одна картинка, горит (С2 в срезе 1 выключено; «до / после починки» — срез 2); `lamp_new` — та же
  картинка; место в комнате — A1e2 по К2 (декор без касания).
- Игрушка-подарок — готовая `tile_gift`; `fun_lego` на месте — картинка мечты `goal_lego` (товарный ракурс, № 28 здесь не
  соблюдается ради нуля новых файлов).
- «Ванна с пеной» — новое тело `item_care_vitamins` (№ 16); файл заодно входит в потолок 37 888 Б.
- Домик-палатка: дверь повёрнута к зрителю (была под товарный азимут 32°), цвет или кант — под кромку ≥ 3 : 1 (замер
  прототипа 2,26); силуэт тот же. Мяч и книжка — прежние построители (декор фонов и фасадов); их кромка печатается, не
  валит; нынешние 512-px ниже порога (корм, обед, шампунь) — строка BACKLOG к A1h.
- Открытка — сложенная, с сердцем, не конверт: конверт в комнате означает карманные деньги.

## A1e2 — встройка (набросок)
После ворот № 96 и слияния A1e1, по очереди после A1d2 (`RoomScreen.kt`) и 1b4 (`TownUi.kt`); test-author не нужен
(TOWN-A1.md:72-74); кодер обычный.
- **Коммит ассетов (оркестратор, BASE A1e2):** 22 (19) WebP `to_webp.py` без `--size` в `game/res/drawable-nodpi/` (18 новых +
  4 замены; при № 93 б — 15 + 4); LICENSES.md:54 — «Товары, цели, плитки…» с новыми числами (`item_*` 10 → 26, при № 93 б — 23; `goal_*` 5 → 6; + `poster_*` 1) и
  «вещи камерой комнаты — `props.py` (`render_thing` на камере `room.py`) → `to_webp.py`»; итог LICENSES.md:59 и
  ARCHITECTURE.md:28 («58 файлов») — по факту (`ls | wc -l`) после слияний A1d, A1g. Зонд `python tools/art_check.py items
  [--selfcheck]` по образцу `residents()` в tools/art_check.py (номера строк не ставим — сдвинутся слиянием A1d1): id списка
  (товары `shop_market` и `shop_foma`, `lamp_new`, `town.homeItems`, `town.goals`, `fun_lego`; при № 93 б — без четырёх) =
  ветки `itemRes`/`goalRes` = файлы. Разбор свой: (а) `itemRes` — форма `when (id) {`, псевдонимы с полным именем ресурса
  (`lamp_new` → `item_home_lamp`, `fun_lego` → `goal_lego`, `gift_toy` → `tile_gift`, префикс `item_` к ним не применять);
  (б) `goalRes` — форма `when {` с ветками `id == "<id>" -> R.drawable.<id>` (имя ресурса = id целиком), ветку
  `demo_goal`/`custom_` → `goal_custom` не считать; (в) исключения вне перечня и вне «лишних» — ветка и файл `fun_bow`
  (в лавках нет, ANTI-SCOPE) и файл `goal_custom`; (г) самопроверка — мутант на каждый путь: нет ветки `itemRes`, псевдоним
  на чужой ресурс, нет ветки `goalRes`, ветка `goalRes` в комментарии, `else -> null` заменён — каждый MISMATCH со своей
  причиной (как `pastries --selfcheck`). Ветку `fun_bow` A1e не трогает (её удаление меняет APK, shrinkResources).
- **`TownUi.kt`:** `itemRes` + 19 веток (14 товаров, 3 стартовые, 2 псевдонима; при № 93 б — 15); `goalRes` + `goal_camp`;
  `posterRes(eventId)` — `"pk3_super_food" -> R.drawable.poster_food_super`, иначе `null` (явные `R.drawable` — R8,
  TOWN-A1.md:70-71). Живой зонд эмодзи — по шаблону отладочного зонда «overflow» (WORKFLOW № 17): `testTag` внутри родителя
  с `clearAndSetSemantics` uiautomator не видит (карточка лавки, «Обустроить», шапка, калитка, список мечт — дерево
  доступности не содержит потомков очищенного узла), поэтому узел — на корне: рядом с `LocalClipped` (TownUi.kt:67-68)
  `LocalEmoji = staticCompositionLocalOf<SnapshotStateList<String>?> { null }`; в запасной ветке `Pic` —
  `DisposableEffect(emoji) { list?.add(emoji); onDispose { list?.remove(emoji) } }`; в `GameApp.kt` рядом с «overflow»
  (:229-232, только `BuildConfig.DEBUG`) — `Box(Modifier.align(Alignment.TopStart).size(1.dp).testTag("emoji").semantics
  { contentDescription = "Эмодзи: ${emoji.size} …" })`; в релизе узла нет.
- **Стартовые:** `RoomScreen.kt:225`, `JarsScreen.kt:204` — `Pic(itemRes(starter.id), starter.emoji, …)`.
- **Бра:** `Image(item_home_lamp)` над изголовьем по К2 и раскладке A1d2, декор без касания и без узла TalkBack
  (`clearAndSetSemantics {}`); мишенью «К Степану» станет в срезе 2 (С2).
- **Плакат:** до A1s — картинка 64 dp в карточке `PlaceEvents` (PlaceScreen.kt:408-418) без описания (текст карточки уже
  читается), раскладка карточки (колонка или строка) — по ответу № 96 и сдвигу полки на К3; в A1s — над `food_super` в витрине.
- **Приёмка:** сборка обоих вариантов; `art_check.py items` → MATCH; `grep -rn 'Pic(null,' finny-pet/app/src/game/java/ru/finny/pet/game/screens`
  → только `TrayScreen.kt` (выпечка, 1b4) и `StreetScreen.kt:87` (🏠, не товар); release-APK `unzip -l` — все файлы набора на
  месте (R8 не выкинул); самопроверка живого зонда на BASE A1e2 до встройки — рынок недели 1: content-desc узла
  `resource-id="emoji"` — «Эмодзи: N», N ≥ 1 (🍚 `food_porridge`, 🧼 `care_soap`, 🍦 `fun_icecream`); живьём (эмулятор 360 × 640
  при 1,0 и 1,3; S23 без звука или «доснять»): рынок недели 1 и 2 с Пк3, «У Фомы» неделя 5 обе страницы, комната недель 1 и 5,
  «Обустроить», копилка — content-desc узла `emoji` начинается с «Эмодзи: 0»; улица — только 🏠; статический `items` остаётся; `perf.sh` на лавке; прирост APK ≤ доли, факт — в TOWN-A1.md;
  картинкам `contentDescription` не добавлять (двойное чтение); BACKLOG.md:107-111 («ещё витаминки») закрыть; ревьювер.

## ДОКИ (что станет ложью)
- После A1e1: `docs/tasks/TOWN-A1.md:43` — статус «A1e1 сделано (генератор), A1e2 — встройка»; `docs/GAME_CONCEPT.md:1844`
  «камерой комнаты (`props.py --room`, а не товарным ракурсом `fit_camera`)» и :1981 «(`--room`)» → «(`props.py` `ROOM`:
  `room.room_camera` + `room.sprite_frame`)» (если A1d1 не заменил раньше, TOWN-A1d1.md:887-888); :1980 «лампа до и после
  починки» → «лампа (горит; «до / после починки» — срез 2)»; §18 — № 93–96 с ответами, при № 93 а — примечание к № 31 (:2062).
- `finny-pet/docs/ARCHITECTURE.md:188` и `BUILD_AND_DEMO.md:308`, :318 — `props.py`: вещи камерой комнаты (`ROOM`,
  `render_thing`, `import room`), 216 px, имена `item_home_<id>`, `poster_<itemId>`, `--only item_fun_rug`.
- `tools/art_check.py:4-7` — докстринг регресса (+2 прогона), коммитом слияния.
- `docs/BACKLOG.md` — строка «кромка на белом < 3 : 1 у прежних 512-px (корм, обед, шампунь; мяч, книжка — декор) — A1h».
- После A1e2: `LICENSES.md:54`, :59, `ARCHITECTURE.md:28` — числа; `BACKLOG.md:107-111` — закрыть; `docs/tasks/GATE_QUEUE.md` —
  № 93–96 «решено»; `docs/HANDOFF.md` — статус A1e.
- Грепы по обоим `docs/`: `git grep -n -e "--room" -- docs finny-pet/docs tools finny-pet/tools ':!docs/tasks'` → пусто;
  после A1e2 `git grep -n "ещё витаминки" -- docs finny-pet/docs` → пусто.

## ПРИ БЛОКЕРЕ
`STATUS: BLOCKED` с одним вопросом и остановка, если:
- в feat/town нет `room.palette`, `room.light_room`, `room.room_camera`, `room.sprite_frame`, `room.K`, `room.WALL_Y` или `room.SHADOW` с
  подписями «До спавна» п. 0, или импорт `room` создаёт объекты;
- дамп вещи не совпадает с `room_furn_chest` по оболочке, камере, свету или миру (нужна правка `room.py` или порядок вне п. 4);
- вписанная вещь выходит за край кадра или доля < 0,60 при `ROOM_FILL` 0,9 — `ROOM_FILL` и `ITEM_PX` молча не менять;
- кромка на белом ≥ 3 : 1 недостижима без плашки-подложки или смены самой вещи (белая пена) — назвать спрайт и замер;
- спрайт > 37 888 Б или сумма выше потолка без полупрозрачности — назвать спрайт и вес;
- регресс даёт разницу хоть в одном дампе, или новому спрайту нужно менять построитель декора place/facade, `fit_camera`,
  `render_prop`, палитру `C`;
- нужен примитив вне `lib.py` и помощников props.py (текстура, булева операция, свой шейдер-обводка) или импорт
  `place`/`facade`/`pet`/`uiprops`.
Новый hex — не блокер (список в отчёте). Не изобретать, protect не трогать.

## Журнал спеки
- **2026-09-29, сессия 14 — сведение трёх черновиков** (углы «контент и код», «генератор и арт», «ребёнок, лист и
  приёмка»; репозиторий не менялся). Взято: из «контента» — таблица id, свет и камера до построителя, `kept_check`, разбивка
  на две задачи; из «арта» — пользователи построителей в place/facade, кромка на белом ≥ 3 : 1 (№ 66 б), якорь коврика −5,
  подгонка по углам bbox (те же точки, что `sprite_frame`), `super_can`, вопрос о порядке; из «ребёнка» — 216 px, мир мебели
  через дамп `room_furn_chest`, пустышка на якоре, вопросы о срезе 2 и бюджете эпика, судьи и кадры, крючок `pic_emoji`.
  Отброшено: сцена × 2 и `build_room` с гирляндой и часами (другой мир, чем у мебели), бра «погасла» (С2 выключено, срез 2),
  тумбочка `furn_table` (стол — BACKLOG A1d1), `RUG_ROLL`, вариант плаката с буквами (ТЗ 3.6), рендер `item_gift_toy`
  (есть `tile_gift`), `palette_new.py` (новые цвета — список грепом), зонд пар IoU (см. ниже), порог «низ ≤ 9 px» (см. ниже),
  три задачи и `item_home_lamp_off`.
- **Прогнано при сведении** (scratch `a1e/syn_r/`; HEAD 47f5001, room.py — копия рабочего дерева wt/a1d, не принят):
  - прототип `render_thing` (CONTRACT п. 4) в копии props.py: 5 вещей (мяч, книжка, домик — прежние построители; картина,
    коврик — пробные) на HIP за 18 с вместе с дампом `furn_chest`: все 216 × 216 RGBA, углы α 0; доля 0,75 (мяч) –0,90;
    зазор снизу — книжка 4, домик 8, коврик 3, мяч 25 px (углы bbox шара ниже силуэта — отсюда нет порога «≤ 9 px»);
    дампы вещей = `furn_chest` по оболочке, свету, камере и миру; «солнце» картины стало `sun.001`, свет сохранил имя;
  - кромка на белом: мяч 3,13, книжка 5,59, домик **2,26** (< 3 — отсюда исключение для домика в п. 6);
  - WebP вещей 216 px: 2 784–5 166 Б; нынешние товары, пережатые 512 → 216: 6 032–8 554 Б;
  - регресс копии (props.py с `import room` и `render_thing`, room.py из wt/a1d) против d0 BASE 1217ffe: `place_*`, `facade_*`,
    `props_*`, питомцы — «разница: 0», отличаются только `room_*` (код A1d1); импорт props в Blender — `IMPORT OK (3, 2)`;
  - `a1e_check.py`: пустой каталог → `A1E FAIL`; `room_port_day` вместо дампа мебели → `SELFCHECK FAIL`; 5 рендеров +
    синтетика до 22 → `A1E OK 22`; с `--no-slice2` и лишними файлами → FAIL, без них → `A1E OK 19`; `--goods-only` → `A1E OK 10`;
    мутанты дампа (якорь стены у вещи пола, неравный масштаб, кривая, lens 37) → FAIL; настоящий домик → `white edge 2.26`;
  - `kept_check.py`: BASE сам с собой и BASE против прототипа → `KEPT OK`; домик убран из `PROPS` → `KEPT FAIL ['PROPS item_fun_tent']`;
    правка книжки → `KEPT FAIL ['changed item_fun_book']`; `VIEWS` выпечки → FAIL;
  - зонд пар IoU черновика «ребёнок» (порог 0,78) отброшен: книжка против мяча — 0,80, круг против квадрата — 0,785 по
    геометрии; разные предметы компактной формы он валит — пары судят только судьи в сером (К5);
  - скоуп на HEAD 47f5001 → `[] []`, покрытие `['finny-pet/tools/art/props.py']`; проекции точек пола и стены — таблица КОНТЕКСТА.
  - Не проверено: рендер на слитом A1d1 (код может измениться на воротах № 76–81 — «До спавна» п. 0: API и `SHADOW`), реальные
    построители новых вещей и товаров, их WebP, живой зонд `emoji` (A1e2).
- **2026-09-29 (сессия 14): круг критиков по r1**, три линзы, находки в редакции двух скептиков на каждую; опровергнутые в
  спеку не вносились. Всего 35: подтверждено 24, частично 7, опровергнуто 4.
  «Контракт против кода»: 10 находок, подтверждено 9, частично 1, опровергнуто 0. Приняты C1, C2, C3, C4, C5, C6, C8, C9,
  C10 целиком: C1 — коллизия номеров с 1b4 (№ 89–90); C2 — при № 93 б товаров 8, а не 7; C3 — варианты № 93 б и № 95 б
  доведены до всех пунктов приёмки (таблица ACCEPTANCE, `cap = N × 11 273`, «Слияние» при № 95 б); C4 — вариант B сверяется
  `git -C wt/a1e diff --name-only BASE` без путей; C5 — шаг 4.5 повторяет цикл `room.render_sprite` по `room.SHADOW`, п. 0
  сверяет `SHADOW`; C6 — живой зонд эмодзи узлом на корне по шаблону «overflow» вместо слепого `pic_emoji`; C8 — ≈ +52 КБ и
  ≈ 18,5 КБ по своим формулам; C9 — эллипс коврика на одной основе (овал: ≈ 1 : 4 и ≈ 1 : 2,5); C10 — мутанты «не пустышка»,
  «не по имени», поворот, RGB. C7 — подтверждённая часть: свой разбор `items` (форма `goalRes` `when {`, псевдонимы, `fun_bow`
  и `goal_custom` вне «лишних», мутант на каждый путь), ссылка на `residents()` без номеров строк. Отклонено: из C7 —
  «строки :344-372 неверны» (на HEAD 47f5001 верны; :367-383 — незакоммиченное рабочее дерево 1b4); из C3 — потолок 80 252
  (это доля APK, а не сумма WebP) и цикл дампов из `props.ROOM` (props.py импортирует `bpy`); из C1 — нумерация с № 91: после
  2629c3e № 91–92 заняты комнатой (GATE_QUEUE.md:4), вопросы A1e — № 93–96 (в r1 — № 90–93).
  «Приёмка и зонды»: 12 находок, подтверждено 10, частично 1, опровергнуто 1. Приняты ACC-1 … ACC-10 целиком: ACC-1 — тот же
  набор света и камер, вид целиком, вне поддерева вещи только оболочка (правка критика не ловила повторную оболочку —
  добавлена проверка поддерева) и шесть мутантов; ACC-2 — вместе с C2 «ребёнка»; ACC-3 — снимок и сверка по путям
  `tools finny-pet/tools` в обоих вариантах; ACC-4 — ядро тела домика BASE ⊆ NEW со строками-масками, три мутанта силуэта,
  правила правки домика в CONTRACT п. 6 и честный предел (добавленное не видно — судит К1); ACC-5 — счётчики прогонов убраны
  («exit 0»: после A1g1 добавится `street`); ACC-6 — `fill` ≤ 0,96; ACC-7 — только AST-проверка п. 2а; ACC-8 — разрешённая
  правка ванны `alpha=1.0` и CLI п. 9 в самопроверке `kept_check`; ACC-9 — мутанты типа, поворота, цветового режима и
  команда веток «no dump» / «лишние»; ACC-10 — как C4. ACC-12 — подтверждённая часть: форма `C:/…` для `W` явно,
  `isdir` → `True True`, пустой `$T/a1e`. Отклонено: ACC-11 (К1 не собрать `sheets.py`) — опровергнута скептиками; из ACC-5
  — необязательный FURN_DUMP (команды передают путь, при `--goods-only` файл не открывается — `x`); из ACC-7 — правило зазора
  «низ < верх» (центрированный коврик его проходит, 71/53 px) и грепы `text3d|sign`, `super_can(` ≥ 3 (строже контракта,
  считают строки); из ACC-12 — `rm -rf $T/a1e` (стёр бы черновики сведения).
  «Ребёнок, лист и судьи»: 13 находок, подтверждено 5, частично 5, опровергнуто 3. Приняты C2, C3, C6, C9, C13 целиком: C2 и
  ACC-2 — самопроверка только пустышками, «до» (эмодзи, старые 512-px) — материал отдельной пачкой (в одной пачке судья
  сопоставил бы пару), сравнение по id — отдельным списком, не условием порога (скептик ACC-2: условие по каждому id отменило
  бы допуск 18 из 22); C3 — без «ванна — это лекарство?» (промах по свободному ответу, контроль — «до»), пары по ответам на
  «что это?», контрольные пары К5, условия № 96 переписаны; правку судьи «Урок» не брали (оба скептика: кадры К3 подписаны, на
  № 96 не влияет); C6 — как C5 контракта, при № 79 а вещь с тенью и без на листе (спрайт общий с полкой лавки — № 28 а);
  C9 — как C1; C13 — замер кода J1 +33 080 вместо непроверенных 32 984, «192 px» — в К1 и № 96 б. Частично: C1 — слепая
  пачка 36 dp для вещей комнаты и стартовых (подписи нет нигде), порог 64 dp на мелкие размеры не переносили (на карточке
  40 dp, калитке и в «Обустроить» подпись есть), шапка 28 dp — в отчёт; C4 — один список пар (столбец п. 7), напарники вне 22
  в К5, промах «назван именем напарника»; пару «игрушка-подарок — плитка подарка» не брали (одна картинка, один смысл), кадр
  «картина с окном» тоже (окно — постоянная мишень К2); C8 — место плаката до A1s — вход A1e2 (кругом A1e1-2 не
  разворачивается), сдвиг полки на К3 и вопрос «Сцене»; подвопрос а/б/в не брали (итоговое место — № 39 а, `Panel` события
  принят, TOWN-J1.md:126; рост на 64 dp не доказан); C10 — обоснование № 93 со ссылкой на строку эпика и пометкой «не
  замерено», «40–64 dp» исправлено, макеты № 39 названы; прогон судьи до ворот не брали; C12 — вопрос о размерах судье «Сцена»,
  формулировка масштаба в «Решениях спеки»; классы 28/36 dp не брали (построители не в масштабе мира, больше 36 dp в средней
  колонке не помещается). Отклонено целиком: C5 (контраст кромки к фону места), C7 (конструктор товарным ракурсом), C11
  (коврик в `spot_5`/`spot_6`, бра над изголовьем) — опровергнуты скептиками.
- **Перепрогнано после правок** (scratch `a1e/fin/`, зонды извлечены командой «До спавна» п. 3 из этой спеки):
  - `kept_check.py`: BASE (47f5001) сам с собой и против прототипа `syn_r` → `KEPT OK`; зонд без ванны в ALLOWED, без пропуска
    `__main__`, без проверки ядра домика, без домика в ALLOWED → `SELFCHECK FAIL` каждый;
  - `a1e_check.py`: самопроверка на `furn_chest` прототипа — OK; 5 дампов прототипа (мяч, книжка, картина с `sun.001`, коврик,
    домик) → `room_bad` пуст; зонд без проверки света и камер, вида, поддерева, типа пустышки, поворота, якоря, оболочки,
    кривых, детей пустышки → `SELFCHECK FAIL` на своём мутанте; FURN_DUMP с RGB → `SELFCHECK FAIL: FURN_DUMP …`; `x/` с пустыми
    `item_fun_rug`, `zzz` → ровно «item_fun_rug: no dump» и «лишние ['zzz']»; `--goods-only --no-slice2` → 8 `no png`;
  - AST п. 2а: props.py HEAD → `[] ['item_food_super', 'poster_food_super', 'no def'] []`; мутант (`sign(…, "Супер")`,
    `text3d`, плакат без банки) → оба вызова и `['poster_food_super']`; образец по контракту → `[] [] ["ROOM[name] != 'wall'"]`;
  - API room.py с `SHADOW`: HEAD → `… ['K', 'SHADOW']`, wt/a1d b536480 → `[]`; `grep -c '^SHADOW = False'` → 0 и 1;
  - из прогонов скептиков: коврик (проекция камерой комнаты) — овал 1,4 : 1 с толщиной при −1 ≈ 1 : 4,0, при −5 ≈ 1 : 2,46
    (замер спрайта 2,49), круг — 1 : 3,14 и 1 : 1,85; `fill` 0,98 в штатном ракурсе при 16 и 96 сэмплах — `goal_zoo`
    (1, …) FAIL, шампунь (65, 1, …) FAIL, 0,96 анфас при 96 — `goal_zoo` (4, 59, 213, 157) OK; release на чистом `app/`
    после 47f5001 — 4 382 302 Б (+33 080 к A1f); к 02:50 `app/` изменён (1b4 в работе), APK 4 416 824 Б — не замер.
  - HEAD при правке — 2629c3e (приёмка A1d1 PASS 44/0, вопросы № 91–92 комнаты; меняет только доки), номера строк — 47f5001.
- **2026-09-29, сессия 14 — ответ владельца на № 93–95** (GATE_QUEUE раздел 5): «по рекомендациям» — № 93 **а**, № 94
  **а**, № 95 **а**; § 18 — строки № 93–96. Следует: A1e1 — 22 спрайта (вертушка, карусель, открытка — новые, подарок —
  готовая `tile_gift`; ветки № 93 б и `--no-slice2` не нужны); бюджет эпика +1 МБ держим — после каждой встройки факт,
  перед A1e2 замер release APK (№ 94 а); A1e1 одним заходом и одним листом после круга A1d1-2 и переноса `room.py` в
  `feat/town` (№ 95 а; заход товаров до слияния A1d1 не нужен). Тень вещей — по № 79 б: нет, подвопрос к № 96 отпадает.
  № 96 — по листу A1e1, отдельной пачкой.
- **2026-09-29, сессия 14 — BASE** (коммит «docs(TOWN-A1e1): BASE …», от 7571ef7 — room.py A1d1 слит, в art_check.py 7 прогонов
  `room_furn_<id>`). Ответы № 93 а, № 94 а, № 95 а — в раздел вопросов; № 79 б — `SHADOW = False` (п. 0 → 1); строка таблицы
  ACCEPTANCE — «№ 93 а, № 95 а» (22 имени, 12 дампов, без флагов, `cap` 248 006). **Дерево кодера — wt/a1d на ветке pilot/a1e**
  (вариант B без нового каталога wt/a1e: сессию в новом дереве открыть некому, владелец спит): `git -C wt/a1d checkout -b
  pilot/a1e BASE && git -C wt/a1d cherry-pick 8dbfd2d`, `wt/a1d/.claude/task-scope.json` — SCOPE этой спеки; сессия —
  прежняя обёртка `wt/a1d`. Во всех командах «До спавна» и ACCEPTANCE `W` — `…/wt/a1d`, `wt/a1e` → `wt/a1d`; `$T/a1e` занят
  черновиками сведения — по п. 0 взят `$T/a1e_run`. Scratch кодера — `$TEMP/s12/a1e_coder/`.
- **2026-09-29, сессия 15 — круг A1e1-2 принят, слияние.** Журнал A1e1: PASS 39 / 0 (ca585fa) — п. 1.1 и 11.1 проверены
  вручную из-за дефекта `acc.sh:46` (комментарий проглотил `echo; is 1.1`, ИТОГ 37 вместо 39 — WORKFLOW № 49); круг
  A1e1-2 по № 96 б (коврик прямоугольный с бахромой, простой шампунь с откидной крышкой и каплей, плакат — миска и сердце без
  «!», цветок — тёмные горшок и листья) — PASS 50 / 0 (f1437e5). Перепроверка (workflow `wf_b60b3e26-7b7`, лист
  `town/a1e_6_circle2.jpg` — подписи ДО/ПОСЛЕ исправлены в сессии 15): «ребёнок» вслепую узнал 4 из 4, пустышки 0 из 2;
  кромка цветка к полу в сером 3,14 (64 dp) / 3,37 (36 dp) — находки № 96 закрыты. Новое — GATE_QUEUE № 101 (шампунь с
  помпой «антисептик»), № 102 (коврик на полу в сером < 3 : 1). **Слияние** — вариант B: `git checkout pilot/a1e --
  finny-pet/tools/art/props.py`, в `tools/art_check.py` прогоны `props_item_fun_robot`, `props_item_food_super` —
  bf1f161; регресс feat/town 31 × ok, эталон `$TEMP/s12/refbase_a1e` (29 дампов `refbase` + `props_item_fun_robot` из
  `acc2_sd` + `props_item_food_super` из `wt/a1d` f1437e5; `props_*` прежних трёх — побайтно те же, что в d1 приёмки) →
  `DUMP DIFF EMPTY`. Эталон приёмки A1e1-2 (`acc2_d1`) снят до слияния `facade.py` — для сверки не годится (6 `facade_*`,
  нет `street`). Тело спеки (:140-142, :178, :197, :737, :739) описывает спрайты до круга; действующий вид — круг A1e1-2 выше.
