# TOWN-A1d1 — генератор комнаты: пустая оболочка день/вечер и 7 спрайтов мебели камерой комнаты (`room.py`, третий генератор № 61)

```
TASK: TOWN-A1d1 — в finny-pet/tools/art/room.py: оболочка комнаты (пол, стены, гирлянда, часы) день/вечер, камера
      комнаты для спрайтов (room_camera + sprite_frame) и 7 спрайтов мебели (только генератор; встройка в UI,
      res/, LICENSES — A1d2)
EPIC: TOWN-A1 (docs/tasks/TOWN-A1.md: строка A1d :42, решения 27 :20-22 и 28 :23, пилот № 61 б :56-62, правила :64-76)
BASE: sha коммита этой спеки; в том же коммите — +1 прогон room_port_evening в tools/art_check.py, строка A1d и доля
      бюджета в TOWN-A1.md, правило имён furn_<id>, строка HANDOFF («До спавна» п. 1); finny-pet/tools/ в нём не меняется
BRANCH: feat/town; кодер — git worktree wt/a1d/ (внутри корня, wt/ в .gitignore:19), ветка pilot/a1d от BASE +
        cherry-pick 8dbfd2d (профиль coder: Opus, xhigh, maxTurns 150 — в feat/town не сливается); своя сессия Claude
        Code в каталоге wt\a1d (открывает владелец), свой wt/a1d/.claude/task-scope.json; в feat/town переносится
        только room.py (`git checkout pilot/a1d -- …`) после ворот
ЗАВИСИТ ОТ: № 17 (обустроенная комната, окно — «живой кусочек улицы», GAME_CONCEPT.md:2034), № 27 (оболочка без вещей,
        мебель, окно и полка — спрайты в мишенях, :2045), № 28 (одна камера комнаты для мест и A1e, :2046), стоп-лист
        № 17 (пустые места контуром не рисуются, :1348-1349), № 38 (стиль, lib.py:4-9), № 61 б и дополнение владельца
        2026-09-28 (третий генератор — комната, :2079), № 25 (в окне — житель, «!» и достигнутые мечты, :2042),
        № 75 а (значок «!» — `#E0004A`, белая обводка 2 dp, :2094). Круги 1b3-2 (props.py, № 66 б; коммит 46d5e60
        в pilot/j1b) и A1g1-2 (facade.py, № 71а, 73 — `HOME_MARK`, 74 б; a0915cf в pilot/a1g) — кодеры закончили, ждут
        приёмки и слияния (wt/j1b, wt/a1g); lib.py заморожен (№ 61 б).
```
Номера строк — HEAD `fd50e64` (feat/town). После `1380e99` добавлены только `docs/tasks/TOWN-J1-1b1-2.md`,
`tools/line_close.py`, `tools/result_geom.py` и правлены `tools/bakery_states.sh`, `tools/town_route.sh` — прочие ссылки
совпадают с 1380e99 (после `97642b4` код не менялся; ответы ворот арта № 65–75 — в 1380e99, строки §18 ниже :2009 +1).
В основном дереве не закоммичена работа кодера J1-1b1-2 (`GameApp.kt`, `TownUi.kt`, `StreetScreen.kt`, `RoundScreen.kt`,
`Widgets.kt`): после её коммита в `TownUi.kt` `Target` :160 → :163, центрирование :163 → :166 (+3 импорта); строки
`GameApp.kt` :177, :185, :224-226, :258-260 не сдвигаются. Перед коммитом BASE — `grep -n '^fun Target' finny-pet/app/src/game/java/ru/finny/pet/game/ui/TownUi.kt`.
Вопросы владельцу № 62–69 — ворота 1б, № 70–74 и 71а — A1g1, № 75 — значок «!» события (ответ а); у A1d1 — с № 76.

## КОНТЕКСТ
- **Почему оболочка пустая** (№ 27, GAME_CONCEPT.md:2045): «при Crop и шрифте 1,3 запечённая мебель разъезжается с
  мишенями»; там же — «окно и полка с банками — тоже спрайты в своих мишенях» (как читаем полку — «Решения без вопроса»).
- **room.py сейчас** (`finny-pet/tools/art/room.py`):
  - 4 варианта `VARIANTS` (:15-18): `room_land_day/evening` 1920 × 1080, `room_port_day/evening` 1080 × 1920;
    CLI (:159-170): `--all DIR`, `--only NAME --out FILE`, `--samples` (160), `--preview` (½ размера, 24 сэмпла, :148).
  - Кадр непрозрачный (`film_transparent = False`, :149), PNG RGBA (`lib.py:54`).
  - Камера портрета (0, −10,5, 3,1) → (0, 2, 2,0), lens 36 (:153); x сжат в 0,68 (:23).
  - В фон запечено то, чего в оболочке быть не должно (№ 27): окно с проёмом, небом и шторами (:40-75), пустая
    полка (:77-81), коврик (:96-99), цветок (:101-107), ящик и мяч (:109-115), торшер (:117-123). Коврик, торшер и
    мячик — покупаемые вещи (GAME_CONCEPT.md:1797). Вечерний тёплый свет `lamp`, `lamp_spill` привязан к торшеру
    (:135-136, `return (lx, ly)` :124).
  - Годится как есть: пол `plank_floor` (:39), гирлянда (:83-88), часы (:89-94); `shell_walls(window=None)` даёт
    цельную `wall_back` (`lib.py:172-173`).
  - Докстринг :7-9 («the rug is kept empty», «The shelf stays empty») станет ложью.
- **Запечённое расходится с мишенями** (проекция камерой дампа, 360 × 640): рама окна фона — под мишенью полки,
  полка фона — под мишенью окна; часы — 272..311 × 213..252 dp, под мишенями полки и копилки, на S23 ещё и под
  дверью (видны на 0 %). Свободная стена на обоих экранах — только полоса между верхним рядом и питомцем, слева и
  справа от места `spot_2` (зонд `clock_probe.py`: точки (±1,05; 2,92; 1,75) → 111..149 / 211..249 × 309..347 dp).
- **Линия пола** (стык пола и задней стены при x = 0): 408,8 dp из 640 по проекции (замер исследования — 1229 px из
  1920). Сейчас холодильник и дверь стоят низом на 382 / 422 dp (360 × 640), на S23 — на 124 / 84 dp выше пола
  (Crop ×1,219, `art_check.py:47`). Одной оболочкой обе раскладки на пол не поставить — вопрос № 76 (решает A1d2).
- **Мишени 7 спрайтов** (`game/screens/RoomScreen.kt`; `Target` — `game/ui/TownUi.kt:160`, фон по умолчанию White
  α 0,85):

  | Мишень, dp | Строки | Сейчас | Смысл для игры |
  |---|---|---|---|
  | окно 112 × 80 | :140-142 | плашка `G.sky`, житель 40 dp, малиновый «!» | улица: житель недели, «!» события, достигнутые мечты у забора (решение 25, GAME_CONCEPT.md:586, :1333, :2042) |
  | полка 152 × 72 | :201-215 | белая плашка, `ui_jar` 32 dp + крышки 22 dp, числа или «Разложи» | план недели: Нужное / Хочу / Запас |
  | холодильник 64 × 96 | :152-155 | белая плашка, линия, «Список» | список нужного и цены |
  | сундук 64 × 48 | :157 | эмодзи 🧳 | вещи, вход в «Обустроить» |
  | кровать 128 × 64 | :174-176 | эмодзи 🛏 и «Сон» | сон, конец дня |
  | дверь 64 × 136 | :181-184 | плашка `goldDark`, точка-ручка, «Улица» | выход на улицу |
  | почтовый ящик 48 × 48 | :235-243 | Canvas-конверт `purpleDeep` с пульсом, «+N» | конверт карманных |

  Подписи «Список», «Улица», «Сон», «+N», числа банок и «!» — живой текст UI (масштаб шрифта, TalkBack).
  Копилка `ui_piggy` (:145-146) и словарик `ui_book` (:158) — уже спрайты, не трогаем.
- **Спрайты только дневные.** `RoomScreen` всегда на дневной оболочке; вечерняя — под `Night` и `WeekEnd`
  (`GameApp.kt:185`), где мишеней нет: вуаль `purpleDeep` α 0,35, `ui_moon` 72 dp, питомец 180 dp, `Panel` (`NightScreens.kt:54-67`).
  Окна-спрайта ночью нет; следствие — ночью в комнате нет ни мебели, ни окна (сейчас ночью видно окно со звёздами и
  торшер, room.py:59-67, :117-123). У итога недели `Panel` на весь экран с отступом 8 dp (`NightScreens.kt:86-87`).
- **`room_port_day` — фон 12 экранов без слоя места** (TOWN-A1c.md:47: Title, Intro, CreatePet, Room, Jars, Savings,
  Arrange, Street, Board, Progress, Glossary, Parent; `GameApp.kt:185`, :258), не только `RoomScreen` — кадр К7.
- **Один мир с фонами мест.** `place.py` копирует `K, WALL_Y` (`place.py:22`), дневной свет (`day_light`, :155, копия
  `room.py:139-142`) и камеру (:171 ← `room.py:153`); фасад дома — цвета стен (`facade.py:21`, «# room.py»). Регресс
  мест правку room.py не видит (у мест свои копии) — поэтому камеру, дневной свет, `K`, `WALL_Y` и цвета стен в
  room.py не менять.
- **Правило волны № 61:** два генератора в одном файле не пускать (TOWN-J1-1b.md:478-479). A1d1 пишет только room.py;
  камера спрайтов — своя функция в room.py на примитивах lib.py, не `props.py --room` (строка эпика :42 допускает оба).
  room.py не импортирует `place`, `props`, `facade` (у них идут правки пилотов) и `uiprops`.
- **Регресс** (`tools/art_check.py:102-113`): 20 дампов, из room.py — только `room_port_day` (`--only … --preview`);
  прогон дампит последнюю сцену (:109). Вечер регресс не видит — BASE добавляет `room_port_evening` (21 дамп).
- **Импорт room.py без побочных эффектов** (:159; TOWN-A1a.md:28, зонд :69) — функции камеры нужны A1e (TOWN-A1.md:43).
- **Уроки пилота** (WORKFLOW № 41, TOWN-J1-1b.md:739): guard-хук в сессии worktree работает и читает скоуп worktree;
  стоп-хук (SubagentStop) там не срабатывает — изоляцию держат guard и `git status` приёмки; два Blender на одном HIP
  одновременно — rc 0/0 (TOWN-J1-1b.md:743).
- **Время на HIP** (замер исследования, с запуском Blender): превью с дампом 11 с; оболочка 1080 × 1920 при 160
  сэмплах ≈ 32 с; спрайт рендером части кадра (64 сэмпла) 7–8 с.
- **ТЗ:** 3.5 — не пугать и не стыдить (вечер без торшера темнее — № 78); 3.6 — мишени ≥ 48 dp остаются
  Compose-элементами, контраст, цвет не единственный признак; 3.3 — строка LICENSES (A1d2).

## ЧТО УВИДИТ РЕБЁНОК В КОМНАТЕ (после встройки A1d2; A1d1 экран не меняет)
| Момент | Что на экране | Что понимает ребёнок |
|---|---|---|
| открыл игру днём | Лавандовые стены, деревянный пол, гирлянда, часы на свободной стене; плашек у мебели нет — мебель стоит там, куда нажимать; у копилки и словарика плашки остаются (вопрос № 81) | «Это дом моего питомца, тут уютно» |
| ищет еду и список | Высокий светлый холодильник: две дверцы, ручка, листок на магните; «Список» на светлом низу | узнаёт по форме, не по цвету |
| смотрит на план | Деревянная полка на кронштейнах, на ней прежние банки с крышками и числами | «Мои деньги разложены по банкам» |
| смотрит в окно | Рама с подоконником, небо с облаком, внизу полоска газона и забора (№ 77); на подоконнике житель недели 64 dp, «!» в кружке `#E0004A` с белой обводкой (№ 75 а) | «На улице меня ждут» |
| хочет выйти | Светлая деревянная дверь в полный рост с ручкой; «Улица» на светлом низу | «Дверь — на улицу» |
| пришёл конверт | Почтовый ящик на столбике под дверью; конверт с «+N» пульсирует поверх (пульс — только при включённой анимации) | «Пришли карманные» |
| сундук | Деревянный сундук с выпуклой крышкой и полосами — не чемодан | «Тут мои вещи» |
| пора спать | Кровать боком: изголовье, подушка, одеяло; питомец над ней; «Сон» на одеяле | «Нажму — питомец ляжет спать» |
| вечер / ночь | Ночь: комната без мебели и окна (стены, пол, гирлянда, часы), темнее; тёплый свет — по № 78 (при а его нет); луна `ui_moon` над спящим питомцем, панель ночи | спокойно, не страшно |
| пустое место | ничего: контуров и подставок нет (стоп-лист № 17) | не приманка «купи» |

Цвет — не единственный признак: 7 предметов различаются силуэтом и пропорцией (высокий узкий, самый высокий,
широкий низкий, плоский на кронштейнах, маленький квадрат, низкий ящик, рама с подоконником) — проверка в сером (К4)
и слепым судьёй (К6).

## CONTRACT (только `finny-pet/tools/art/room.py`)
1. **Оболочка — пустая.** `build_room(evening, portrait)` строит только `plank_floor` (как :39),
   `shell_walls(…, window=None)` (цельная `wall_back`, `side`×2, `skirt_s`×2, `skirt`), гирлянду (:83-88 без
   изменений) и часы. Окно, раму, шторы, карниз, небо, облака, звёзды, луну, полку, коврик, цветок, ящик, мяч, торшер
   в оболочке не строить; ничего другого (мебели, контуров мест, подставок, текста) не добавлять. Имена `flag`×9,
   `string`, `clock_rim`, `clock_face`, `hand_h`, `hand_m`, `clock_pin` (:87-94) не менять — их сверяет `room_keys.py`.
   Материалы, цвета, `WALL_Y`, `K` = 0,68, дневной свет `light_room` и обе камеры — без изменений.
2. **Часы** — геометрию и материалы (:90-94) не менять, двигается только центр `cx, cz` (:89). Критерий — `CLOCK OK`
   зонда `clock_probe.py` (приёмка п. 4): проекция не заходит на мишени, места `spot_1`, `spot_2`, `spot_6`, питомца и
   шапку на 360 × 640 и на S23. Точку выбирает кодер; подсказка: зонд проходит при центре x = −1,05 после сжатия
   (в записи :89 — `X(-1.54)`), z = 1,75 — слева от `spot_2`, над питомцем (111..149 × 309..347 dp). Облачко питомца
   `PetBubble` (≈ 8..352 × 288..364 dp, x × y: `padding(bottom = 176.dp)`, RoomScreen.kt:252; закрывается тапом и при
   навигации — GameViewModel.kt:176, :187, :327) и длинная строка `LineHost` (до 0,4 высоты экрана, GameApp.kt:177, над
   нижним рядом :225 — верх до ≈ 312 dp) могут временно закрыть часы — допустимо, в BAN их нет (расчёт по раскладке).
3. **Вечер без торшера.** Константа `WARM = (4.0, 1.2)  # № 78: (x до сжатия K, y) тёплого света вечера; None — без него`.
   При значении BASE свет `lamp`/`lamp_spill` побайтно прежний (цвет, сила, размер, позиция `X(4.0), 1.2`, цель
   `lx * 0.5, 0, 0.5`); `lamp` — без target, как BASE (room.py:135; цель по умолчанию (0, 0, 1), `lib.py:222`, пустышка
   `lamp_t` от `WARM` не зависит); при `None` не создаётся. При любом `WARM` у `lamp`, `lamp_spill`, `lamp_spill_t`
   меняется только позиция (`room_keys.py`). `key`, `fill`, `moon` (:133-134, :137) и мир вечера (:132) не меняются;
   эмиссия абажура (:121) уходит с торшером.
4. **Камера комнаты** — функция `room_camera(scene, portrait=True)`: ставит ту же камеру, что `render_variant`
   (:152-155), и возвращает её; `render_variant` вызывает её же (одна точка правды для A1e, № 28).
5. **Кадр спрайта** — функция `sprite_frame(scene, objs, w, h, floor=False, top=None)`: включает рендер части кадра (`use_border` +
   `use_crop_to_border`) ровно `w × h` px по проекции `objs` камерой сцены (`bpy_extras.object_utils.world_to_camera_view`
   по углам bbox). Перед проекцией — `bpy.context.view_layer.update()` (TRACK_TO у камеры, `lib.py:217-219`, :243-244).
   По x кадр центрируется по проекции; по y: при `floor=True` низ кадра = низ проекции + 3 px (≤ 9 px, п. 6); при `top`
   (px) верх кадра = верх проекции − `top` (полка: при центре по y верх проекции не ниже 108 px из 216 — требование п. 7
   «верх доски 123..132 px» невыполнимо); иначе — центр по y. Границы кадра — целые px кадра 1080 × 1920. A1e вызывает
   её для своих вещей.
6. **Спрайт** — `SPRITES = {name: (w_dp, h_dp, builder)}` и `render_sprite(name, out, samples, preview=False)`:
   - сцена — портретная комната дня: `plank_floor`, `shell_walls(window=None)` (без гирлянды и часов), `light_room`
     дня, `room_camera(portrait=True)`, разрешение 1080 × 1920 (`PX = 3` px/dp — константа), `film_transparent = True`,
     `color_mode = "RGBA"`;
   - предмет строится в мире комнаты, x — со стороны своей мишени (окно, холодильник, сундук — x < 0; дверь, ящик —
     x > 0; кровать, полка — у центра). Окно и полка висят на задней стене. Холодильник и дверь стоят на полу у стены
     (z = 0). Мишени кровати, сундука и почтового ящика на обеих раскладках ниже линии пола — эти предметы строятся на
     полу перед стеной (z = 0);
   - оболочка в кадре не видна: при `SHADOW = False  # № 79` (основной рендер) у всех её объектов `visible_camera = False`.
     При `True` (только вариант для листа К4, приёмка п. 6) её объекты — ловцы тени (`is_shadow_catcher = True`): у
     кровати, сундука и ящика — только `plank_floor` (у `wall_back` `visible_camera = False`), у окна и полки — только
     `wall_back`, у холодильника и двери — пол и стена;
   - кадр — `sprite_frame(…, w_dp·PX, h_dp·PX, floor=…, top=…)` (`floor=True` у холодильника, двери, кровати, сундука,
     ящика; `top=126` у полки — середина 123..132 px, п. 7; окно — центр по y).
     Край — по α > 0, как `art_check.py bbox` (:240-242): углы α 0, bbox ≥ 2 px от краёв. Заполнение и низ — по
     α > 127: по большей доле оси ≥ 0,88 (fill 0,9 из `props.py:340` минус край); у напольных низ bbox ≤ 9 px (3 dp) от
     низа кадра. У окна дыр в альфе (α < 8, не связанных с краем) нет — щель рама/небо; у прочих замкнутый просвет
     (кронштейн, ручка, изголовье) показывает стену и допустим. Критерии — для основного рендера; вариант `SHADOW = True`
     им не подчиняется: тень срезается краем кадра мишени (зонд `feas.py`, «До спавна» п. 7а);
   - `--preview` у спрайта — 24 сэмпла, размер тот же (кадр точный).
7. **7 спрайтов** (имена `furn_<id>`, размер = мишень × 3 px/dp):

   | name | dp | px | силуэт (форма, не цвет) | обязательно |
   |---|---|---|---|---|
   | `furn_window` | 112 × 80 | 336 × 240 | рама, подоконник на всю ширину, шторы по бокам; за стеклом небо с облаком, при `WINDOW_VIEW = "fence"` внизу стекла полоска газона и забора | верх подоконника — не выше 184 px от верха кадра (ступни жителя 64 dp — 0,96 кадра, TOWN-A1f.md:35-36); под ним подоконник видимой толщины, его низ — по правилу bbox ≥ 2 px (п. 6); линия верха подоконника — на К3; панель неба закрывает проём целиком, без щелей; `MULLION = False  # № 77` (True — крест, как :47); `WINDOW_VIEW = "fence"  # № 77` ("sky" — только небо) |
   | `furn_shelf` | 152 × 72 | 456 × 216 | доска на двух кронштейнах | без банок: банки, крышки и числа — прежние `ui_jar`/`ui_lid_*` (RoomScreen.kt:204-210); верх доски — 123..132 px от верха кадра (41..44 dp: на него встают банки `ui_jar` по центру мишени при шрифте 1,3 и 1,0 — RoomScreen.kt:201-215, центрирование `Target` TownUi.kt:163 (после коммита J1-1b1-2 — :166), потолок шрифта 1,3 — GameApp.kt:116-118, lineHeight 20 sp GameTheme.kt:67; `sprite_check.py` меряет первую строку α > 127 в колонке x = w/2); кадр — `top=126` (п. 5), поэтому проекция доски с кронштейнами — не выше 88 px (216 − 126 − 2, правило bbox ≥ 2 px); числа ложатся ниже — на доску, кронштейны и стену; полоса 36..72 dp — светлая (п. 8 приёмки) |
   | `furn_fridge` | 64 × 96 | 192 × 288 | высокий, скруглённый, две дверцы, вертикальная ручка, листок на магните | напольный; низ 66..90 dp светлый под «Список» |
   | `furn_door` | 64 × 136 | 192 × 408 | дверь в полный рост, ручка справа | напольная; светлое дерево, не тёмное; низ 106..130 dp под «Улица» |
   | `furn_bed` | 128 × 64 | 384 × 192 | кровать боком: изголовье, подушка, одеяло | напольная; центр 40..88 × 20..44 dp светлый под «Сон» |
   | `furn_mailbox` | 48 × 48 | 144 × 144 | почтовый ящик со щелью и флажком на столбике или подставке | напольный (стоит на полу, в `FLOOR` зонда `sprite_check.py`); без конверта (конверт-Canvas и «+N» — UI, :236-242); не красный и не малиновый круглый (так выглядит «!»); середина светлая |
   | `furn_chest` | 64 × 48 | 192 × 144 | сундук с выпуклой крышкой, полосами и застёжкой | напольный; не похож на ящик для игрушек :111-113 |

   Цвета — только литералы room.py (:25-36, небо `#8ED4FF` и облако `(1, 1, 1, 1)` emit 0,85 — :70-72; все цвета `HOME`
   в facade.py:21 среди них; для забора и газона — `m_white`/`wood[*]` и `m_green`/`m_green2`). Нужен новый hex — `TODO`
   в коде и строка в отчёте (не блокер; судит лист). Текста (`text3d`, `sign`) на спрайтах нет. По контуру светлых
   предметов допустим тёмный кант `purple` #520978 (:31): белый #FFFBF6 на стене #EAE2F6 — 1,22 : 1 по hex (после
   рендера — светотень; судят К4 и «ребёнок»).
8. **CLI.** `--all DIR` — 4 оболочки, как прежде; `--only NAME --out FILE` принимает имя варианта или спрайта; новый
   `--sprites DIR` — 7 файлов `furn_*.png`, по одной сцене на спрайт (`reset_scene` перед каждым, как
   `render_variant` :148: иначе свет копится, а дамп `--only` этого не видит). `--samples`, `--preview` работают в обоих режимах. Вызов регресса
   `--only room_port_day --out F --preview` работает как прежде; спрайты в прогоне оболочки не рендерятся.
9. **Импорты** — только `sys, os, math, argparse, random`, `lib` и модули Blender (`bpy`, `bpy_extras`, `mathutils`).
   Импорт room.py не рендерит и не создаёт объектов и материалов: разбор argv и запуск — под `if __name__ == "__main__"`
   (:159; TOWN-A1a.md:28), приёмка п. 2.
10. **Докстринг** (:1-11): запуск `--sprites` и `--only furn_<id>`, состав оболочки, правило `furn_<id>`, `room_camera`,
    `sprite_frame`, константы `PX`, `SHADOW`, `MULLION`, `WINDOW_VIEW`, `WARM`; без «kept empty» и «stays empty».

На выбор кодера (судят лист и ворота): точка часов в свободной полосе, форма ручек, облако, узор одеяла, пропорции
сундука и ящика, изголовье с одной или двух сторон.

## SCOPE
**Вариант A.** `wt/a1d/.claude/task-scope.json` пишет оркестратор (файл в `.gitignore:4`, worktree его не получает).
Пути — от корня worktree; это protect A1g1 с заменой: `facade.py` → protect, `room.py` → allow:
```json
{"task": "TOWN-A1d1 — оболочка комнаты и 7 спрайтов мебели (room.py), третий генератор № 61 (docs/tasks/TOWN-A1d1.md)",
 "base": "<sha BASE>",
 "allow": ["finny-pet/tools/art/room.py"],
 "protect": [".claude/", "CLAUDE.md", "README.md", ".gitignore", ".gitattributes", "docs/", "finny-pet/.gitignore",
  "finny-pet/docs/", "finny-pet/app/", "finny-pet/screenshots/", "finny-pet/assets/", "finny-pet/release/",
  "finny-pet/README.md", "finny-pet/CHANGELOG.md", "finny-pet/keystore.properties.example",
  "finny-pet/build.gradle.kts", "finny-pet/settings.gradle.kts", "finny-pet/gradle/", "finny-pet/gradle.properties",
  "finny-pet/gradlew", "finny-pet/gradlew.bat",
  "finny-pet/tools/art/lib.py", "finny-pet/tools/art/place.py", "finny-pet/tools/art/props.py",
  "finny-pet/tools/art/to_webp.py", "finny-pet/tools/art/facade.py", "finny-pet/tools/art/pet.py",
  "finny-pet/tools/art/uiprops.py", "finny-pet/tools/art/import_sprites.py", "finny-pet/tools/art/sounds.py",
  "finny-pet/tools/art/smoke.py", "finny-pet/tools/art/montserrat_extrabold.ttf",
  "finny-pet/tools/ui.py", "finny-pet/tools/demo_run.sh", "finny-pet/tools/office/",
  "tools/art_check.py", "tools/sheets.py", "tools/perf.sh", "tools/adbui.sh", "tools/emu.sh", "tools/ui_measure.py",
  "tools/mutation_probe.py", "tools/content_map_events.py", "tools/town_route.sh", "tools/bakery_states.sh",
  "tools/rec.sh", "tools/mock_bakery.py", "tools/line_close.py", "tools/result_geom.py"]}
```
- Каталогов `tools/`, `finny-pet/tools/`, `finny-pet/tools/art/` в protect нет: guard сверяет хвосты от двух сегментов
  подстрокой (`guard-paths.js:139-146`), каталог `finny-pet/tools/art/` хвостом `tools/art/` задел бы `room.py`.
- Проверено на HEAD fd50e64 (2026-09-29, сессия 13): пересечения и хвосты `[] []`; непокрытым отслеживаемым файлом
  остаётся только `room.py` (при `git -c core.quotepath=off ls-files`; без этого флага 4 пути `docs/sources/…` кириллицей
  идут в кавычках и ложно «не покрыты»). Без двух путей из fd50e64 (`tools/line_close.py`, `tools/result_geom.py`)
  покрытие давало `['finny-pet/tools/art/room.py', 'tools/line_close.py', 'tools/result_geom.py']`. Новый отслеживаемый
  файл (например, после коммита J1-1b1-2) — дописать в protect поимённо (HANDOFF.md:175-176, WORKFLOW № 29).
- Файл — литерал JSON выше. `$TEMP/s12_scope.py` (на него ссылается HANDOFF.md:66) a1d не знает (`KeyError`: только
  `j1b`, `a1g`), и его protect уже (нет `README.md`, `.gitignore`, `finny-pet/screenshots/`, `finny-pet/assets/` и др.) —
  не использовать.
- **Вариант B** (если сессию в `wt\a1d` открыть нельзя): общий скоуп основного дерева, allow —
  `wt/a1d/finny-pet/tools/art/room.py`; только когда в основном дереве нет кодера (1b1-2 принят или на паузе: guard читает
  единственный `.claude/task-scope.json`, guard-paths.js:24-25, :102-104); скоуп — только A1d1; после приёмки вернуть скоуп
  1b1-2 из его спеки; изоляция — только `git -C wt/a1d status`; отступление сообщить владельцу.
- **Кодеру:** команды с путями protect — без `>` и `2>&1` (признак записи — любой `>`, `guard-paths.js:122-123`,
  WORKFLOW № 29); `lib.py` читать инструментом Read, не `cat`; рендеры и логи — только в свой scratch; итерации —
  `--only <имя> --out <scratch>/… --preview`; финальные рендеры — оркестратор.
- **Самопроверка кодера** (зонды и `d0` — в его scratch, «До спавна» п. 11а; все команды без `>` и `2>&1`; `<sc>` —
  scratch сессии worktree, `<art>` — каталог для png; `BL` — `C:/Program Files/Blender Foundation/Blender 5.2/blender.exe`):
  - сначала `mkdir -p <sc>/d <sc>/sd <art>`: `dump()` открывает `DUMP_OUT` без создания каталога (art_check.py:95);
  - часы: `DUMP_OUT=<sc>/d/room_port_day.json "$BL" -b --factory-startup --python-exit-code 1 -P finny-pet/tools/art/room.py --python tools/art_check.py -- --only room_port_day --out <sc>/r.png --preview`,
    затем `python <sc>/clock_probe.py <sc>/d/room_port_day.json` → `CLOCK OK`;
  - состав оболочки: `DUMP_OUT=<sc>/d/room_port_evening.json "$BL" … --python tools/art_check.py -- --only room_port_evening --out <sc>/re.png --preview`,
    затем `python <sc>/room_keys.py <sc>/d0 <sc>/d` → `ROOM KEYS OK` (режим kept — `WARM` BASE);
  - спрайты: для каждого `n` — `DUMP_OUT=<sc>/sd/furn_<n>.json "$BL" … --python tools/art_check.py -- --only furn_<n> --out <art>/furn_<n>.png --samples 16`,
    затем `python <sc>/sprite_check.py <art> <sc>/sd <sc>/d/room_port_day.json` → `SPRITES OK`;
  - подписи: оболочка в полный размер `"$BL" … -P finny-pet/tools/art/room.py -- --only room_port_day --out <sc>/room_day.png`
    (без `--preview`: зонд режет фон по 3 px/dp), затем `python <sc>/zones.py . <art> <sc>/room_day.png` → `LABEL ZONES OK`.
  - Зонды — критерий CONTRACT п. 1–3, 6 и 7; FAIL зонда при сдаче — не `DONE`.

## ANTI-SCOPE
- `res/`, WebP в APK, LICENSES, любой Kotlin, снятие плашек, житель 64 dp, «!» в кружке, портрет Оси — A1d2.
- `lib.py` (заморожен, № 61 б), `place.py`, `props.py`, `facade.py`, `uiprops.py`, `to_webp.py`, `tools/`.
- Камера, дневной свет, `K`, `WALL_Y`, цвета стен и пола (их копии в place.py и facade.py).
- Вещи мест и стартовые вещи (лампа-оверлей, цветок, кресло, коврик, мячик) — A1e; стол для `spot_3` (слот `table`,
  `content.json` `town.spots`) — не в этой задаче, строка в BACKLOG от оркестратора.
- Вечерние спрайты, ночное окно, подсветка кровати вечером (GAME_CONCEPT.md:1387 — это UI).
- Судьба `room_land_*` — A1h (портрет закреплён, `game/AndroidManifest.xml:9`); `--all` их рендерит общим кодом, в
  приёмку и APK они не идут.
- Контуры, подставки, тени пустых мест (стоп-лист № 17).

## БЮДЖЕТ
- **`room.py`: вставок ≤ 170, удалений ≤ 90** — `git diff -w --numstat BASE -- finny-pet/tools/art/room.py`.
  - Удаления (оценка ≈ 75): коврик–торшер :96-124 (29), окно, небо и полка, если переносятся в builder-ы, а не
    оборачиваются на месте (до 38; `-w` не считает смену отступа), вечерний свет :135-136 и сигнатуры (≤ 4),
    докстринг (≤ 4).
  - Вставки (оценка ≈ 150): builder-ы 5 новых предметов (≈ 55), окно и полка на новых размерах (≈ 45 при переносе),
    `room_camera` (≈ 4), `sprite_frame` (≈ 15), `render_sprite` и `SPRITES` (≈ 15), константы (5), CLI (≈ 5),
    докстринг (≈ 10).
  - Превышение — вопрос оркестратору до приёмки.
- **Доля A1d в бюджете APK** (пишется в TOWN-A1.md в коммите BASE; остаток на A1s, A1d, A1e, A1h и код — 518 595 Б,
  TOWN-A1.md:62):
  - оболочки портрета заменяют нынешние: `room_port_day` 40 338 + `room_port_evening` 39 978 = 80 316 Б (`stat`); худший
    прирост 2 × 61 440 − 80 316 = **42 564** Б (ожидаю ≈ 0: фоны мест весят 38 578–45 392 Б, зонд исследования
    пустой оболочки — 33 280 + 38 678 Б, −8 358 к паре res, a1d_research.md:131; оговорка — боковые стены в зонде задвоены);
  - 7 спрайтов при 3 px/dp = 434 880 px кадра; плотность 45 RGBA-спрайтов `game/res` (замер судьи: байт на пиксель с
    α > 127) — медиана 0,222384, p75 0,268463 (пересчёт r3 на HEAD) → при заполнении кадра 60–90 % от 58 026 до **105 074** Б (оценка, не рендер);
  - dex, arsc, заголовки — **6 000** Б (TOWN-A1f.md:139, как у A1g1);
  - **доля A1d ≤ 153 638 Б** (29,6 % от 518 595); на A1s, A1e, A1h и код останется 364 957 Б;
  - **критерий приёмки:** Σ 9 WebP (2 оболочки портрета + 7 спрайтов) ≤ 80 316 + 147 638 = **227 954 Б**; оболочка
    ≤ 61 440, спрайт ≤ 37 888 (TOWN-A1.md:66-67). Превышение — вопрос оркестратору; quality 88 и 3 px/dp молча не менять.
  - Риски: полка по верхней оценке исследования ≈ 47 КБ (> 37 888) — вопрос оркестратору, не решение кодера;
    замена `room_land_*` в A1d2 — до +42 806 Б сверх доли (не нужна: решает A1h).
- **Время:** регресс 21 дамп ≈ 4–5 мин; 7 дампов спрайтов при 16 сэмплах ≈ 1 мин; рендеры приёмки ≈ 4 мин
  (оболочки 4 × 32 с, спрайты 7 × ≈ 8 с, 5 вариантов констант).

## ДО СПАВНА (оркестратор)
Обозначения: `$BL` — `C:/Program Files/Blender Foundation/Blender 5.2/blender.exe`; `T=$(cygpath -m <scratch сессии
оркестратора>)`, `W=$(cygpath -m "$(pwd)/wt/a1d")` (из корня основного дерева) — обе в форме `C:/…`: путь вида `/c/…` во
вшитых строках Python и Blender не открывается (TOWN-A1a.md:58); все команды спеки рассчитаны на эту форму. Самопроверка:
`python -c "import os;print(os.path.isdir(r'$T'), os.path.isdir(r'$W'))"` → `True True` (после п. 2). Правки основного
дерева — когда в нём не работает кодер (HANDOFF.md:176-177).
1. **Коммит BASE** (файлы поимённо; чужие неотслеживаемые, например `finny-pet/screenshots/town/pilot_art_6_street_mock.jpg`,
   не трогать):
   - `docs/tasks/TOWN-A1d1.md` (на неё уже ссылается GAME_CONCEPT.md:2079);
   - `tools/art_check.py` — после прогона `room_port_day` (:102) строка
     `runs += [("room_port_evening", "room.py", ["--only", "room_port_evening", "--out", os.path.join(out, "room_e.png"), "--preview"])]`,
     докстринг :4 — «room.py (room_port_day, room_port_evening)»;
   - `docs/tasks/TOWN-A1.md`: :42 «`props.py --room` или своя функция на частях lib» → «своя функция `room.room_camera` +
     `room.sprite_frame` (правило волны № 61), [TOWN-A1d1](TOWN-A1d1.md)»; абзац пилота :56-62 — доля A1d ≤ 153 638 Б и
     остаток 364 957 Б; :61 «TOWN-J1.md:313» → «:314» (`grep -n "≈ 70 КБ" docs/tasks/TOWN-J1.md` → 314); :70 — «мебель
     комнаты — `furn_<id>`»;
   - `docs/HANDOFF.md` — строка третьего генератора (`wt/a1d`, `pilot/a1d`, регресс 21 дамп); п. 3 «A1d» (:64-68:
     «генератор скоупа — `$TEMP/s12_scope.py`») — заменить ссылкой на SCOPE этой спеки.
   - Пилотам лишний дамп не мешает: `diff` перебирает файлы первого аргумента (art_check.py:127); у A1g1 первым идёт
     старый d1 (TOWN-A1g1.md:443) — вечер не виден; у 1b3 порядок не задан (TOWN-J1-1b.md:475-477) — первым ставить
     старый каталог, иначе `MISSING …/room_port_evening.json`. «20 × ok» пилотов — в их worktree со своим art_check.py.
   - Проверки: `git diff --name-only BASE~1 BASE -- finny-pet/tools` → пусто; `git diff --name-only BASE~1 BASE -- tools`
     → `tools/art_check.py`.
2. **Worktree:** `git worktree add -b pilot/a1d wt/a1d BASE && git -C wt/a1d cherry-pick 8dbfd2d`.
   - `git -C wt/a1d log --oneline BASE..HEAD` → одна строка `… pilot(№ 61 б): профиль арт-кодера …`;
     `git -C wt/a1d diff --name-only BASE` → ровно `.claude/agents/coder.md`. Судья проверил: `.claude/` на HEAD равен
     родителю 8dbfd2d (`git diff --quiet 8dbfd2d~1 HEAD -- .claude/` → 0) — конфликта не ждём; конфликт — стоп, вопрос владельцу.
3. **Скоуп** из SCOPE в `wt/a1d/.claude/task-scope.json`, `base` = BASE. Из корня основного дерева:
   - пересечения и хвосты (логика `variants`, guard-paths.js:139-146):
     `python -c "import json,sys;d=json.load(open(sys.argv[1]));v=lambda e:[e]+['/'.join(e.rstrip('/').split('/')[i:])+('/'*e.endswith('/')) for i in range(1,len(e.rstrip('/').split('/'))-1)];a=d['allow'];print([x for x in a for p in d['protect'] if x.startswith(p)],[t for p in d['protect'] for t in v(p.lower()) for x in a if t in x.lower()])" wt/a1d/.claude/task-scope.json`
     → `[] []`; самопроверка — та же команда на копии с `"finny-pet/tools/art/"` в protect → второй список не пуст;
   - покрытие: `git -C wt/a1d -c core.quotepath=off ls-files | python -c "import sys,json;p=json.load(open(sys.argv[1]))['protect'];print([f for f in sys.stdin.read().split(chr(10)) if f and not any(f.startswith(x) for x in p)])" wt/a1d/.claude/task-scope.json`
     → `['finny-pet/tools/art/room.py']`.
4. **Зонды** — тексты в разделе «Зонды приёмки» этой спеки (копии круга r2, прогнанные редактором:
   `…/6fe7a2a6-…/scratchpad/r2p/`; Temp может быть почищен — источник правды спека). Извлечь (Git Bash, из корня основного дерева):
   ```bash
   mkdir -p $T/a1d && python -c "import re,sys;t=open('docs/tasks/TOWN-A1d1.md',encoding='utf-8').read();[open(sys.argv[1]+'/'+m.group(2),'w',encoding='utf-8').write(m.group(1)) for m in re.finditer(r'^\`\`\`python\n(# (\w+\.py) — .*?)^\`\`\`',t,re.S|re.M)]" $T/a1d && ls $T/a1d/*.py
   ```
   → `clock_probe.py feas.py room_keys.py sprite_check.py zones.py`.
5. **BASE-дампы:** `mkdir -p $T/a1d/base && git archive BASE finny-pet/tools/art | tar -x -C $T/a1d/base`;
   `python tools/art_check.py regress $T/a1d/base/finny-pet/tools/art $T/a1d/d0` → 21 строка `… ok`, exit 0; в
   `room_port_day.json` 101 запись, в `room_port_evening.json` 125 (свет луны в BASE — `moon.001`, тёзка сферы `moon`;
   без сферы станет `moon` — `room_keys.py` сравнивает имена без суффиксов).
6. **Самопроверка регресса** (WORKFLOW № 27): `cp -r $T/a1d/base $T/a1d/mut && sed -i 's/lens=36)/lens=35)/' $T/a1d/mut/finny-pet/tools/art/room.py && grep -c "lens=35)" $T/a1d/mut/finny-pet/tools/art/room.py`
   → 1; `python tools/art_check.py regress $T/a1d/mut/finny-pet/tools/art $T/a1d/dm; python tools/art_check.py diff $T/a1d/d0 $T/a1d/dm | grep -E 'разница: [1-9]|DUMP DIFF'`
   → ровно `room_port_day.json 101 объектов, разница: 1 ['cam']`, `room_port_evening.json 125 объектов, разница: 1 ['cam']`,
   `DUMP DIFF: 2 file(s)` (place.py держит свою копию камеры). Иначе — чинить регресс, не спавнить.
7. **Самопроверки зондов на d0** (каждый обязан покраснеть на BASE):
   - `python $T/a1d/room_keys.py $T/a1d/d0 $T/a1d/d0` → `ROOM KEYS FAIL …`, exit 1, без `SELFCHECK FAIL`;
   - `python $T/a1d/clock_probe.py $T/a1d/d0/room_port_day.json` → `CLOCK FAIL dp 272..311 x 213..252 ['360x640 (8, 198, 352, 278)', 'S23 (8, 191, 352, 279)', 'S23 (288, 279, 352, 689)']`, exit 1;
   - `mkdir -p $T/a1d/e && python $T/a1d/sprite_check.py $T/a1d/e $T/a1d/e $T/a1d/d0/room_port_day.json` → `SPRITES FAIL ['furn_window: no png', …]`, exit 1;
   - `zones.py`: синтетика — `mkdir -p $T/a1d/zt && python -c "from PIL import Image;[Image.new('RGBA',s,(0xE9,0xB9,0x83,255)).save(r'$T/a1d/zt/furn_%s.png'%n) for n,s in {'fridge':(192,288),'door':(192,408),'bed':(384,192),'mailbox':(144,144),'shelf':(456,216)}.items()]"`,
     `python $T/a1d/zones.py . $T/a1d/zt finny-pet/app/src/game/res/drawable-nodpi/room_port_day.webp` → `LABEL ZONES OK`;
     затем `furn_door.png` залить `(0x52,0x09,0x78,255)` → `LABEL ZONES FAIL ['door 1.34/1.34']`, exit 1;
     мутант координат (зона на своём месте): `rm -rf $T/a1d/zm && cp -r $T/a1d/zt $T/a1d/zm && python -c "from PIL import Image,ImageDraw;im=Image.open(r'$T/a1d/zm/furn_fridge.png');ImageDraw.Draw(im).rectangle((0,198,191,269),fill=(28,29,34,255));im.save(r'$T/a1d/zm/furn_fridge.png')"`
     (полоса INK на y 66..90 dp), `python $T/a1d/zones.py . $T/a1d/zm finny-pet/app/src/game/res/drawable-nodpi/room_port_day.webp`
     → `LABEL ZONES FAIL ['fridge 1.00/1.00']`, exit 1;
     та же полоса на `(0,0,191,71)` (y 0..24 dp, на свежей копии `zt`) → `LABEL ZONES OK` (прогнано 2026-09-29).
   Судья прогнал все четыре (2026-09-28, d0 на 97642b4): вывод совпал; редактор r2 — `room_keys.py` и `sprite_check.py`
   после правок (журнал).
7а. **Зонд осуществимости тени** (оркестратор, до спавна; результат — в журнал):
   `for m in shadow noshadow; do "$BL" -b --factory-startup --python-exit-code 1 --python $T/a1d/feas.py -- $T/a1d/base/finny-pet/tools/art $T/a1d/feas_$m.png $m; done`
   → оба exit 0; `python $T/a1d/feas.py --check $T/a1d/feas_shadow.png $T/a1d/feas_noshadow.png` → `FEAS OK`, exit 0.
   Ожидание (прогон редактора r2 на HIP, 1380e99): `feas_shadow.png size (206, 310) corners [0, 28, 45, 3] edge2px t/b/l/r
   [28, 65, 121, 53]`, `feas_noshadow.png … corners [0, 0, 0, 0] edge2px [0, 0, 0, 0] margin a>0 5`. Самопроверка встроена:
   тень без α на краях → `SELFCHECK FAIL` (ловец тени не сработал). `FEAS FAIL` — кадр без тени не прозрачен по краю: стоп,
   вопрос оркестратору до спавна.
8. **Снимок основного дерева:** `git status --porcelain -uall -- tools finny-pet/tools > $T/a1d/main0.txt`. Слияния пилотов и
   правки оркестратора в `tools/`, `finny-pet/tools/` между снимком и приёмкой — коммитом до приёмки; иначе снимок
   переснять, причину — в журнал.
9. **Сессия в `wt\a1d`** открывает владелец в приложении (режим auto; `claude -p` не авторизован, инструмента создания
   сессий у оркестратора нет): вставить строку «Прочитай файл …\probe_msg_a1d.txt и выполни инструкцию из него»
   (образец — `$TEMP/s12/probe_msg_a1g.txt`, id сессии оркестратора — новый, `get_session self`). Если образца нет —
   сообщение собрать из: роль (обёртка оркестратора, код не правит и не коммитит), id сессии для ответа, шаг (а)
   `echo "$CLAUDE_PROJECT_DIR"; git rev-parse --show-toplevel; git status --porcelain -uall`, строки 1–9 таблицы п. 10
   дословно промптом подагенту coder, отчёт — по строке «№ / ожидание / факт». Задания и отчёты —
   сообщениями; в каждом — id сессии для ответа. `settings.local.json` в worktree не копировать (как A1g1, 1b3).
10. **Самотест хука** зонд-агентом `agentType: 'coder'` из сессии worktree (хук ограничивает только coder,
    `guard-paths.js:75`); ожидание и факт каждой строки — в журнал:

    | № | Действие | Ожидание |
    |---|---|---|
    | 1 | Edit `finny-pet/tools/art/room.py`: комментарий в конец | пропуск |
    | 2 | Write `finny-pet/tools/art/facade.py` | блок «не входит в allow» |
    | 3 | Write `<абс. корень основного дерева>/wt/probe_outside.txt` | блок «находится вне репозитория» (:102) |
    | 4 | Bash `echo probe > "<абс. scratch сессии worktree>/a1d_probe.txt"` (путь буквально в промпте) | пропуск |
    | 5 | Bash `echo -P finny-pet/tools/art/room.py 2>&1 \| tail -1` | пропуск |
    | 6 | Bash `cat finny-pet/tools/art/lib.py > /dev/null` | блок «shell-запись в защищённый путь» (:149) |
    | 7 | Bash `git -C . checkout -- finny-pet/tools/art/room.py` | блок «операции с git-историей…» (:118) |
    | 8 | Bash `python tools/art_check.py bbox x.png` | пропуск (файла нет — exit 1 скрипта, не хука) |
    | 9 | та же команда с `2>&1 \| tail -1` | блок (WORKFLOW № 29) |
    | 10 | оркестратор, не зонд: незакоммиченная правка `docs/HANDOFF.md` в worktree, `node .claude/hooks/assert-oracle-intact.js` из корня worktree | exit 2, «ORACLE INTEGRITY FAILED» (`assert-oracle-intact.js:24`); стоп-хук в сессии worktree не срабатывает (WORKFLOW № 41) |

    - Самопроверка: строки 2, 3, 6, 7, 9, 10 обязаны дать блок; всё «пропуск» — хук не подключён (WORKFLOW № 14).
    - Восстановление: `git -C wt/a1d checkout -- finny-pet/tools/art/room.py docs/HANDOFF.md`; `git -C wt/a1d status --porcelain -uall`
      → пусто; `git status --porcelain -uall -- tools finny-pet/tools | diff - $T/a1d/main0.txt` → пусто;
      `rm -f wt/probe_outside.txt` и файл строки 4.
    - Хоть одна строка не совпала — вариант B; отступление сообщить владельцу.
11. **Параллельность.** Кодеры кругов 1b3-2 и A1g1-2 закончили (46d5e60, a0915cf); их приёмочные рендеры — в ту же
    очередь. Одновременно на HIP — не больше двух Blender (два проверены, TOWN-J1-1b.md:743; три — не проверены). Очередь
    ведёт оркестратор: сессия, которая рендерит, пишет ему «рендерю» и «свободно»; перед рендером
    `tasklist | grep -ci '^blender\.exe'` → ≤ 1 (именно `blender.exe`: `grep -ci blender` считает и `blender-mcp.exe` —
    2026-09-29 их было 6).
11а. **Зонды кодеру:** оркестратор копирует `$T/a1d/clock_probe.py`, `$T/a1d/sprite_check.py`, `$T/a1d/zones.py`,
    `$T/a1d/room_keys.py` в scratch сессии worktree, а `$T/a1d/d0/room_port_day.json` и `room_port_evening.json` — в его
    `d0/` (`cp`; кодеру Write в scratch guard запрещает — вне репозитория, guard-paths.js:102, а извлекатель п. 4
    с `docs/` — protect, :127, :150-151).
12. **Спавн** из сессии worktree: coder, Opus, effort xhigh; Blender 5.2 `-b --factory-startup --python-exit-code 1`.
    В промпт: CONTRACT, SCOPE (с «Самопроверкой кодера»), ANTI-SCOPE, ПРИ БЛОКЕРЕ, правило № 29, абсолютный путь scratch
    сессии, путь `BL` и абсолютные пути четырёх зондов и `d0/` п. 11а; «зонды — критерий CONTRACT п. 1–3, 6 и 7; FAIL
    зонда при сдаче — не DONE».

## ACCEPTANCE (оркестратор, из корня `wt/a1d`, кроме п. 7 (`bg`) и строки основного дерева в п. 1; Git Bash; без `bc`)
1. **Изоляция.**
   - `git status --porcelain -uall` → ровно ` M finny-pet/tools/art/room.py`.
   - `git diff --name-only BASE` → ровно `.claude/agents/coder.md`, `finny-pet/tools/art/room.py`.
   - `git diff -w --numstat BASE -- finny-pet/tools/art/room.py` → вставок ≤ 170, удалений ≤ 90.
   - `node .claude/hooks/assert-oracle-intact.js` → exit 0.
   - Из корня основного дерева: `git status --porcelain -uall -- tools finny-pet/tools | diff - $T/a1d/main0.txt` → пусто.
   - Константы: `grep -cE '^(PX = 3|SHADOW = False|MULLION = False|WINDOW_VIEW = "fence"|WARM = \(4\.0, 1\.2\))' finny-pet/tools/art/room.py` → 5.
2. **Импорты и импорт без побочных эффектов.**
   - `python -c "import ast,sys;t=ast.parse(open(sys.argv[1],encoding='utf-8').read());m={a.name.split('.')[0] for n in ast.walk(t) if isinstance(n,ast.Import) for a in n.names}|{n.module.split('.')[0] for n in ast.walk(t) if isinstance(n,ast.ImportFrom) and n.module};print(sorted(m-{'sys','os','math','argparse','random','lib','bpy','bpy_extras','mathutils'}))" finny-pet/tools/art/room.py`
     → `[]`. Самопроверка: та же команда на `finny-pet/tools/art/facade.py` → `['place']` (проверено судьёй).
   - `mkdir -p $T/a1d/imp && cd $T/a1d/imp && "$BL" -b --factory-startup --python-exit-code 1 --python-expr "import sys,bpy; sys.path.insert(0, r'$W/finny-pet/tools/art'); n=(len(bpy.data.objects),len(bpy.data.materials)); import lib, room; assert callable(room.room_camera) and callable(room.sprite_frame) and len(room.SPRITES)==7; m=(len(bpy.data.objects),len(bpy.data.materials)); assert m==n, m; print('IMPORT OK', n)" -- --only room_port_day --out $T/a1d/leak.png; echo rc=$?; cd -`
     → `IMPORT OK`, `rc=0`; `ls -A $T/a1d/imp` → пусто; `$T/a1d/leak.png` нет (образец TOWN-A1a.md:69).
   - Самопроверка: `mkdir -p $T/a1d/impm && cp finny-pet/tools/art/lib.py finny-pet/tools/art/room.py $T/a1d/impm/ && echo 'build_room(False, True)' >> $T/a1d/impm/room.py`,
     та же команда с `$T/a1d/impm` вместо `$W/finny-pet/tools/art` → `rc=1`, в выводе `AssertionError`, не
     `ImportError`/`ModuleNotFoundError`. Прогнано 2026-09-29 (Git Bash, Blender 5.2) на копии BASE room.py с заглушками
     `room_camera`, `sprite_frame`, `SPRITES`: `IMPORT OK (3, 2)`, `rc=0`, `imp` пуст, `leak.png` нет; мутант —
     `AssertionError: (94, 21)`, `rc=1`.
3. **Регресс.**
   - `rm -rf $T/a1d/d1 && python tools/art_check.py regress finny-pet/tools/art $T/a1d/d1` → 21 × `ok` (при Слиянии d1
     перегоняется — без старых файлов).
   - `python tools/art_check.py diff $T/a1d/d0 $T/a1d/d1 | grep -E 'разница: [1-9]|DUMP DIFF'` → ровно
     `room_port_day.json 101 объектов, разница: N …`, `room_port_evening.json 125 объектов, разница: M …`,
     `DUMP DIFF: 2 file(s)` (exit 1 ожидаем); остальные 19 — «разница: 0».
   - Число записей d1: `python -c "import json,sys;print([len(json.load(open(f,encoding='utf-8'))) for f in sys.argv[1:]])" $T/a1d/d1/room_port_day.json $T/a1d/d1/room_port_evening.json`
     → `[50, 54]` при `WARM` BASE и при moved (101 − 52 + 1 и 125 − 72 + 1); `[50, 50]` при `WARM = None` (вечером
     уходят ещё `lamp`, `lamp_t`, `lamp_spill`, `lamp_spill_t`) — для Слияния по ответу № 78.
4. **Состав оболочки и часы** (`diff` печатает ≤ 12 ключей, `art_check.py:134`, поэтому — зонды).
   - `python $T/a1d/room_keys.py $T/a1d/d0 $T/a1d/d1` → `ROOM KEYS OK`. Ожидается ровно: ушли 52 (день) / 72 (вечер)
     объекта, пришёл `wall_back`, сдвинулись 5 объектов часов — одним вектором, у них меняется только `loc`; пол, боковые
     стены, плинтусы, гирлянда, камера, свет (`key/fill/sun`; вечером `key/fill/moon` и `lamp/lamp_spill` при `WARM`
     BASE), мир и вид — побайтно прежние. Самопроверка встроена (эталон из дампа BASE проходит; мутанты камеры, мира,
     флажка, света, цвета доски +0,03, оставленный коврик, размер и цвет `clock_rim`, отдельный сдвиг `hand_h`, в режиме
     `moved` — сила `lamp` краснеют) плюс внешняя (До спавна п. 7).
   - `python $T/a1d/clock_probe.py $T/a1d/d1/room_port_day.json` → `CLOCK OK dp …`. Прямоугольники S23 для колонок,
     питомца и мест — расчёт по раскладке (SpaceBetween, `padding(bottom = 64.dp)`), не замер: при расхождении со
     снимком S23 в A1d2 правится зонд, не код.
5. **Рендеры** (очередью; в выводе `cycles device: HIP`).
   - `"$BL" -b --factory-startup --python-exit-code 1 -P finny-pet/tools/art/room.py -- --all $T/a1d/shell` → exit 0,
     4 PNG: `room_port_*` 1080 × 1920, `room_land_*` 1920 × 1080.
   - `… -- --sprites $T/a1d/spr` → exit 0; `ls $T/a1d/spr` → ровно `furn_bed.png furn_chest.png furn_door.png furn_fridge.png furn_mailbox.png furn_shelf.png furn_window.png`.
   - Дампы спрайтов: `mkdir -p $T/a1d/sd && for n in window shelf fridge door bed mailbox chest; do DUMP_OUT=$T/a1d/sd/furn_$n.json "$BL" -b --factory-startup --python-exit-code 1 -P finny-pet/tools/art/room.py --python tools/art_check.py -- --only furn_$n --out $T/a1d/sd/furn_$n.png --samples 16 | grep -c DUMPED; done`
     → семь `1`.
   - `python $T/a1d/sprite_check.py $T/a1d/spr $T/a1d/sd $T/a1d/d1/room_port_day.json` → `SPRITES OK` (размер = мишень × 3,
     RGBA, край по α > 0: углы α 0, bbox ≥ 2 px; вписан по α > 127 ≥ 0,88; напольные и ящик — низ ≤ 9 px; полка — верх
     доски 123..132 px; у окна без дыр; камера дампа = камера `room_port_day`, свет `key/fill/sun` с целями и мир — как у
     `room_port_day`, кадр прозрачный). Самопроверка встроена: мутанты PNG (край, мягкая тень α 20, мелкий, «висит», RGB)
     краснеют; дыра в окне краснеет, просвет у холодильника — нет; доска полки на 126 px проходит, на 60 px краснеет;
     эталон дампа проходит, мутанты lens и силы солнца краснеют.
   - `python tools/art_check.py bbox $T/a1d/spr/*.png` → 7 строк `… OK`; самопроверка —
     `python tools/art_check.py bbox $T/a1d/shell/room_port_day.png` → `FAIL` (непрозрачный фон).
   - Подоконник окна и вид из окна — не машинно: кадр К3, судья «ребёнок», владелец (№ 77).
6. **Варианты констант для листа** — свои копии, каждая с проверкой замены:
   `v() { d=$T/a1d/v_$1; rm -rf $d && cp -r finny-pet/tools/art $d && sed -i "s/^$2/$3/" $d/room.py && grep -c "^$3" $d/room.py; }`
   затем `v cross 'MULLION = False' 'MULLION = True'`, `v sky 'WINDOW_VIEW = "fence"' 'WINDOW_VIEW = "sky"'`,
   `v shadow 'SHADOW = False' 'SHADOW = True'`, `v nowarm 'WARM = (4.0, 1.2)' 'WARM = None'`,
   `v center 'WARM = (4.0, 1.2)' 'WARM = (0.0, -1.0)'` → пять строк `1` (несработавшая замена → `0`; (0,0; −1,0) — центр
   бывшего коврика, «здесь стоит питомец», room.py:96-97).
   - Рендеры: `v_cross`, `v_sky` — `--only furn_window --out $T/a1d/var/window_<v>.png`; `v_shadow` — `--sprites $T/a1d/var/shadow`
     (только для листа К4; критериям края `sprite_check.py` и `art_check.py bbox` он не подчиняется — тень срезана кадром,
     п. 7а; `bbox` ниже — лишь признак, что константа подействовала);
     `v_nowarm`, `v_center` — `--only room_port_evening --out $T/a1d/var/evening_<v>.png`. Все exit 0.
   - Действие констант — дампами, не `cmp` PNG: два рендера одной сцены на HIP различаются (зонд сессии 12
     `…/22cbd09a-…/scratchpad/e3/det_1.png`, `det_2.png`: `cmp -s` → 1, max |Δ| 2 у 0,009 % пикселей), сравнение PNG
     покраснеть не может.
     - `cross`, `sky`: `for v in cross sky; do mkdir -p $T/a1d/v_$v/d && DUMP_OUT=$T/a1d/v_$v/d/furn_window.json "$BL" -b --factory-startup --python-exit-code 1 -P $T/a1d/v_$v/room.py --python tools/art_check.py -- --only furn_window --out $T/a1d/v_$v/d/w.png --samples 16 | grep -c DUMPED; python tools/art_check.py diff $T/a1d/v_$v/d $T/a1d/sd | grep -c 'разница: [1-9]'; done`
       → четыре `1` (окно варианта отличается от `sd/furn_window.json` основного; сэмплы в дамп не входят,
       art_check.py:92-94). Самопроверка: `python tools/art_check.py diff $T/a1d/sd $T/a1d/sd | tail -1` → `DUMP DIFF EMPTY`.
     - `shadow`: флаги ловца тени дамп не пишет (art_check.py:72-74 — только `visible_shadow`), поэтому по PNG:
       `python tools/art_check.py bbox $T/a1d/var/shadow/furn_{fridge,door,bed,chest,mailbox}.png | grep -c FAIL` → ≥ 1
       (тень у края кадра, как в зонде `feas.py`, п. 7а); `0` — `SHADOW` не подействовал, вопрос кодеру.
   - Свет вечера по вариантам: для `nowarm` и `center` — каталог с `room_port_day.json` из d1 и дампом вечера копии
     (`DUMP_OUT=… -P $T/a1d/v_<v>/room.py --python tools/art_check.py -- --only room_port_evening --out … --preview`);
     `room_keys.py $T/a1d/d0 <каталог> none` и `… moved` → `ROOM KEYS OK`.
7. **Оболочка: WebP, контраст UI, яркость вечера.**
   - `python finny-pet/tools/art/to_webp.py $T/a1d/shell/room_port_day.png $T/a1d/shell/room_port_evening.png --rgb --dst $T/a1d/w`
     → 2 строки `RGB`.
   - `bg` и его самопроверка — из корня основного дерева: снимки `finny-pet/screenshots/emu_b_*` в `.gitignore:13`,
     worktree их не получает (art_check.py:29-30, :197-198). Сначала `git diff --quiet BASE -- tools/art_check.py finny-pet/app/src/game/res/drawable-nodpi/room_port_day.webp; echo $?`
     → `0`. Затем `python tools/art_check.py bg $T/a1d/w/room_port_day.webp $T/a1d/bgd` → `CONTRAST OK` (статус-бар, HUD-2, заголовки
     при 1,0 и 1,3, 360 × 640 и S23; пересвет 0–31 и 80–100 % ≤ 0,05). На res `room_port_day` сейчас — `CONTRAST OK`
     (судья, 2026-09-28). Самопроверка: `python -c "from PIL import Image;im=Image.open(r'$T/a1d/w/room_port_day.webp').convert('RGB');im.paste((28,29,34),(0,0,1080,600));im.save(r'$T/a1d/dark.png')"`,
     `python tools/art_check.py bg $T/a1d/dark.png $T/a1d/bgm` → `CONTRAST FAIL: …`, exit 1.
   - Яркость вечера (число, не FAIL — на К5 и в журнал):
     `python -c "import sys;sys.path.insert(0,'tools');from art_check import lum;from PIL import Image;import numpy as np;[print(f, round(float(lum(np.asarray(Image.open(f).convert('RGB'))).mean()),4)) for f in sys.argv[1:]]" finny-pet/app/src/game/res/drawable-nodpi/room_port_evening.webp $T/a1d/shell/room_port_evening.png $T/a1d/var/evening_nowarm.png $T/a1d/var/evening_center.png`
     → 4 строки со средней яркостью.
8. **Зоны подписей** («Список», «Улица», «Сон», «+N», конверт, числа банок; спрайт на новой оболочке в нынешних
   координатах мишени, пороги `art_check.bg`): `python $T/a1d/zones.py . $T/a1d/spr $T/a1d/shell/room_port_day.png`
   → `LABEL ZONES OK`. Самопроверка встроена (тёмная дверь краснеет, белая проходит) плюс внешняя (До спавна п. 7).
9. **WebP спрайтов и доля.**
   - `python finny-pet/tools/art/to_webp.py $T/a1d/spr --dst $T/a1d/w` → 7 строк `RGBA`.
   - `stat -c '%s %n' $T/a1d/w/*.webp | awk '{s+=$1; if ($2 ~ /furn_/ && $1 > 37888) b=b" "$2; if ($2 ~ /room_/ && $1 > 61440) b=b" "$2} END {print NR, s, (NR == 9 && s <= 227954 && b == "" ? "BUDGET OK" : "BUDGET FAIL" b)}'`
     → `9 <S> BUDGET OK`. Самопроверка: та же `awk` с `227954` → `1000` → `BUDGET FAIL`.
10. **Докстринг** (именно докстринг, не весь файл):
    `python -c "import ast,sys;d=ast.get_docstring(ast.parse(open(sys.argv[1],encoding='utf-8').read()));need=['--sprites','furn_','room_camera','sprite_frame','PX','SHADOW','MULLION','WINDOW_VIEW','WARM'];print([s for s in need if s not in d], 'kept empty' in d or 'stays empty' in d)" finny-pet/tools/art/room.py`
    → `[] False`. Самопроверка: та же команда на room.py BASE (`$T/a1d/base/finny-pet/tools/art/room.py`) → непустой
    список и `True`.
11. **Лист и судьи** (кадры — в разделе вопросов; листы — `tools/sheets.py`, «до/после» на настоящем фоне).
    - «Ребёнок» вслепую (WORKFLOW № 35), К6: вырезки 7 спрайтов 1:1 в цвете и в сером вперемешку с нынешними мишенями
      (плашки, 🧳, 🛏 со снимка), без подписей, нейтральные метки. Вопросы: «где поспать? где выйти на улицу? где еда и
      список? где твои вещи? куда приходят деньги? куда смотреть на улицу? страшно ли вечером (К5)?»; «где банки?» — только
      по композиту К2 (спрайт полки без банок). Первый вопрос по композиту К2 — «на что здесь можно нажать?» (ТЗ 3.6,
      ТЗ_текст.txt:389-390); в К2 — и стартовый декор без касания 🪴 (`spot_1`) и 🛋️ (`spot_4`) (`content.json`
      `homeItems`, RoomScreen.kt:219-227, GAME_CONCEPT.md:1346): после снятия плашек (A1d2) декор кнопкой не назван. Самопроверка: серые прямоугольники-пустышки размеров мишеней судья не
      узнаёт лучше случайного (иначе вопрос подсказывает).
    - Доступность: К4 в сером и при дейтеранопии; машинно — п. 7 и п. 8.
    - Переход день → вечер (кроссфейд 900 мс, `GameApp.kt:259-260`) по листу не решить — запись экрана в A1d2 (WORKFLOW № 31).
    - `git status` основного дерева — снова п. 1.

**Слияние** (после ответов № 76–81; при «круге» — после A1d1-2):
- Оркестратор коммитит в `wt/a1d` принятые значения `MULLION`, `WINDOW_VIEW`, `SHADOW`, `WARM`; перегоняет d1
  (`regress`, 7 дампов спрайтов) и повторяет п. 3–5 (`room_keys.py` в режиме ответа № 78: а — `none`, б — `moved`,
  в — `kept`).
- На feat/town: `git checkout pilot/a1d -- finny-pet/tools/art/room.py`, коммит `A1d1: …`; ветку не мержить, `coder.md`
  не переносить.
- Тем же коммитом в `tools/art_check.py` — 7 прогонов `room_furn_<id>` по одному (`--only furn_<id> --out … --samples 1`),
  как фасады (:110-111, комментарий :109): камера комнаты для A1e под регрессом; докстринг :4.
- **Сборная эталонная база.** TOWN-A1g1.md:443 и TOWN-J1-1b.md:475-477 после слияний ждут `DUMP DIFF EMPTY` против
  своих d1, где `room_port_day.json` — BASE; после слияния A1d1 обе проверки покраснеют на `room_*`. Эталон после всех
  слияний: `room_*` — из принятого `$T/a1d/d1`; `room_furn_<id>.json` — копия `$T/a1d/sd/furn_<id>.json` принятой версии
  (в d1 их нет: регресс worktree прогонов `room_furn_*` не содержит, имя дампа — `name + ".json"`, art_check.py:115);
  `facade_*` и street — из d1 принятого A1g1-2; `props_*` — из d1 принятого 1b3-2; `place_*` — из BASE (`place.py`
  пилота не сливается, № 67 б). После коммита слияния — `regress` на feat/town и `diff` эталона с ним → разница 0 у
  `room_furn_*` (сэмплы в дамп не входят, art_check.py:92-94). Путь `$T/a1d/d1` и это правило — в HANDOFF до первого
  слияния.

## Зонды приёмки (тексты)
Первая строка каждого блока — имя файла; извлечение — «До спавна» п. 4. Судья прогнал четыре на d0 (97642b4) и
синтетике. Сессия 13: `sprite_check.py` — свет и мир дня в дампе, дыры только у окна (прогон в журнале). Редактор r2 прогнал новые тексты: `room_keys.py` — три режима × реалистичные d1 (часы одним сдвигом, `lamp_t`
на месте): диагональ OK, перекрёстные FAIL, `d0 d0` FAIL, d1 с `clock_rim` ×2 и красным FAIL; `sprite_check.py` —
самопроверка проходит, пустой каталог → `SPRITES FAIL`; `feas.py` — рендер на HIP, вывод в п. 7а.

```python
# room_keys.py — TOWN-A1d1: состав дампов оболочки комнаты против BASE. python room_keys.py D0 D1 [kept|none|moved] → ROOM KEYS OK / FAIL, exit 0/1
# kept (по умолчанию) — тёплый свет вечера на месте торшера (WARM BASE, № 78 в); none — убран (№ 78 а); moved — в центр пола (№ 78 б)
import json, re, sys, copy
from collections import Counter
N = lambda s: re.sub(r"\.\d{3}(?=[\"_]|$)", "", s)  # только суффиксы имён .001 (ключи, "name", "parent"), не цифры чисел
COMMON = Counter({"wall_l": 1, "wall_r": 1, "wall_b": 1, "wall_t": 1, "frame": 6, "sill": 1, "rod": 1, "finial": 2, "fold": 6,
                  "sky": 1, "shelf": 1, "bracket": 2, "rug": 1, "rug_border": 1, "rug_dot": 1, "pot": 1, "pot_rim": 1, "leaf": 6,
                  "toybox": 1, "toybox_lid": 1, "knob": 1, "ball": 1, "ball_stripe": 1, "lamp_base": 1, "lamp_pole": 1, "shade": 1, "lamp_top": 1})
GONE = {"room_port_day": COMMON + Counter({"cloud": 8}),                                  # 52
        "room_port_evening": COMMON + Counter({"star": 26, "moon": 1, "moon_cut": 1})}   # 72: mesh moon, the light moon.001 → moon stays
NEW = Counter({"wall_back": 1})
MOVED0 = MOVED = Counter({"clock_rim": 1, "clock_face": 1, "hand_h": 1, "hand_m": 1, "clock_pin": 1})  # CONTRACT п. 2: часы переносятся
bag = lambda d: Counter((N(k), N(json.dumps(v, sort_keys=True))) for k, v in d.items())
keys = lambda c: Counter(k for k, _ in c.elements())
one = lambda D, n: next((v for k, v in D.items() if N(k) == n), None)
strip = lambda v: N(json.dumps({k: x for k, x in v.items() if k != "loc"}, sort_keys=True))

def check(name, A, B):
    MOVED = MOVED_E if name.endswith("evening") else MOVED0
    a, b = bag(A), bag(B)
    lost, got = keys(a - b), keys(b - a)
    bad = []
    if lost != GONE[name] + MOVED: bad.append("lost %s" % sorted((lost - GONE[name] - MOVED) + ((GONE[name] + MOVED) - lost)))
    if got != NEW + MOVED: bad.append("got %s" % sorted((got - NEW - MOVED) + ((NEW + MOVED) - got)))
    sh = []  # CONTRACT п. 2–3: у сдвинутых меняется только loc; 5 объектов часов — одним вектором
    for n in MOVED:
        u, v = one(A, n), one(B, n)
        if u is None or v is None: continue  # пропажу уже поймали lost/got
        if strip(u) != strip(v): bad.append("changed besides loc: %s" % n)
        if n in MOVED0: sh.append([q - p for p, q in zip(u["loc"], v["loc"])])
    if sh and max(abs(s[i] - sh[0][i]) for s in sh for i in range(3)) > 1e-3: bad.append("clock shift differs %s" % sh)
    return bad

d0, d1, bad = sys.argv[1], sys.argv[2], []
WARM = Counter({"lamp": 1, "lamp_t": 1, "lamp_spill": 1, "lamp_spill_t": 1})
WARM_MV = Counter({"lamp": 1, "lamp_spill": 1, "lamp_spill_t": 1})  # lamp_t — цель по умолчанию (0, 0, 1), lib.py:222, от WARM не зависит
mode = sys.argv[3] if len(sys.argv) > 3 else "kept"
if mode == "none": GONE["room_port_evening"] += WARM
MOVED_E = MOVED + (WARM_MV if mode == "moved" else Counter())
for name in GONE:
    A, B = (json.load(open(f"{d}/{name}.json", encoding="utf-8")) for d in (d0, d1))
    # самопроверка на дампе BASE: синтетическая «правильная» оболочка проходит, её мутанты краснеют
    S = {k: v for k, v in A.items() if N(k) not in GONE[name] or A[k]["type"] == "LIGHT"}
    S["wall_back"] = dict(A["wall_l"], loc=[0, 3.15, 7.0])
    mv = MOVED_E if name.endswith("evening") else MOVED0
    S = {k: v for k, v in S.items() if not (name.endswith("evening") and mode == "none" and N(k) in WARM)}
    rim = A["clock_rim"]["loc"]; dx, dz = -1.05 - rim[0], 1.75 - rim[2]
    for k in S:
        if N(k) in MOVED0: l = A[k]["loc"]; S[k] = dict(A[k], loc=[l[0] + dx, l[1], l[2] + dz])
        elif N(k) in mv: l = A[k]["loc"]; S[k] = dict(A[k], loc=[l[0] - 1, l[1] - 1, l[2]])
    assert not check(name, A, S), "SELFCHECK FAIL (эталон): %s" % check(name, A, S)
    for mut in ("cam", "__world__", "flag.003", "sun" if name.endswith("day") else "key"):
        M = copy.deepcopy(S); M[mut] = dict(M[mut], loc=[9, 9, 9]) if "loc" in M[mut] else dict(M[mut], strength=9)
        assert check(name, A, M), "SELFCHECK FAIL: мутант %s не покраснел" % mut
    M = dict(S, rug=A["rug"]); assert check(name, A, M), "SELFCHECK FAIL: коврик остался"
    M = copy.deepcopy(S); p = next(k for k in M if N(k) == "plank"); c = M[p]["mats"][0]["Base Color"]
    c[0] = round(c[0] + 0.03, 4); assert check(name, A, M), "SELFCHECK FAIL: мутант цвета доски (+0,03) не покраснел"
    for f in ("dims", "color", "shift") + (("lamp",) if name.endswith("evening") and mode == "moved" else ()):
        M = copy.deepcopy(S)
        if f == "dims": M["clock_rim"]["dims"] = [x * 2 for x in M["clock_rim"]["dims"]]
        if f == "color": M["clock_rim"]["mats"][0]["Base Color"][0] = 0.5
        if f == "shift": M["hand_h"]["loc"][0] += 0.1
        if f == "lamp": M["lamp"]["light"]["energy"] *= 2
        assert check(name, A, M), "SELFCHECK FAIL: мутант часов/света %s не покраснел" % f
    bad += ["%s: %s" % (name, x) for x in check(name, A, B)]
print("ROOM KEYS OK" if not bad else "ROOM KEYS FAIL %s" % bad); sys.exit(bool(bad))
```

```python
# clock_probe.py — TOWN-A1d1: часы оболочки видны на 360 × 640 и на S23 — не под мишенями, местом spot_2, питомцем, spot_1/spot_6 и шапкой.
# python clock_probe.py DUMP.json → CLOCK OK / FAIL, exit 0/1. Прямоугольники — dp экрана (RoomScreen.kt:139-186, замеры
# исследования по emu_s12f_room.png и emu_j1a_s23_room_w5.png); фон: 360 × 640 — 1:1, S23 — ×1,219 и срез 39 dp с боков.
import json, sys, numpy as np
BAN = {"360x640": (1.0, 0, [(0, 0, 360, 194), (8, 198, 352, 278), (162, 286, 198, 322), (8, 286, 72, 540), (288, 286, 352, 540),
                            (120, 356, 240, 476), (116, 476, 244, 540),
                            (80, 436, 116, 472), (244, 436, 280, 472)]),                    # + spot_1 (стартовый цветок), spot_6
       "S23":     (1.219, 39, [(0, 0, 360, 187), (8, 191, 352, 279), (162, 283, 198, 319), (8, 279, 72, 689), (288, 279, 352, 689),
                            (120, 505, 240, 625), (116, 625, 244, 689),
                            (80, 585, 116, 621), (244, 585, 280, 621)])}
def dp_box(d, loc, r):  # проекция камерой дампа (lens, sensor 36 по высоте кадра 1080 × 1920) → dp фона 360 × 640
    c, t, lens = np.array(d["cam"]["loc"]), np.array(d["cam_t"]["loc"]), d["cam"]["cam"]["lens"]
    f = (t - c) / np.linalg.norm(t - c); rt = np.cross(f, [0, 0, 1]); rt /= np.linalg.norm(rt); up = np.cross(rt, f)
    pts = []
    for dx in (-r, r):
        for dz in (-r, r):
            v = np.array(loc) + [dx, 0, dz] - c; s = lens / 18 / (v @ f)
            pts.append((180 + (v @ rt) * s * 320, 320 - (v @ up) * s * 320))
    p = np.array(pts); return p[:, 0].min(), p[:, 1].min(), p[:, 0].max(), p[:, 1].max()
def hits(box):
    out = []
    for scr, (k, dx, rects) in BAN.items():
        x0, y0, x1, y1 = box[0] * k - dx, box[1] * k, box[2] * k - dx, box[3] * k
        out += ["%s %s" % (scr, r) for r in rects if x0 < r[2] and x1 > r[0] and y0 < r[3] and y1 > r[1]]
    return out
d = json.load(open(sys.argv[1], encoding="utf-8")); rim = d["clock_rim"]; r = max(rim["dims"]) / 2
box = dp_box(d, rim["loc"], r)
base = dp_box(d, [2.312, 2.92, 3.75], 0.4)                    # часы BASE (room.py:89-90, X(3.4)) — под полкой и копилкой
assert hits(base), "SELFCHECK FAIL: часы BASE не покраснели %s" % (base,)
free = dp_box(d, [-1.05, 2.92, 1.75], 0.4)                    # точка свободной стены слева от spot_2 — обязана пройти
assert not hits(free), "SELFCHECK FAIL: свободная стена краснеет %s %s" % (free, hits(free))
bad = hits(box)
print("CLOCK OK" if not bad else "CLOCK FAIL", "dp %.0f..%.0f x %.0f..%.0f" % (box[0], box[2], box[1], box[3]), bad); sys.exit(bool(bad))
```

```python
# sprite_check.py — TOWN-A1d1: 7 спрайтов мебели. python sprite_check.py PNG_DIR DUMP_DIR ROOM_DAY_DUMP → SPRITES OK / FAIL, exit 0/1
# PNG: размер = dp мишени × 3 (RoomScreen.kt:140,201,152,181,174,235,157), RGBA; край по α > 0 (как art_check.py bbox): углы α 0,
# bbox ≥ 2 px от краёв; объект по α > 127 вписан (max доля bbox по осям ≥ 0,88 — fill 0,9 из props.py:340 минус край), без дыр
# в альфе у окна (щель рама/небо; у прочих замкнутый просвет — кронштейн, ручка — показывает стену, не дефект); у напольных
# (и ящика на столбике) низ bbox ≤ 9 px от низа кадра; у полки верх доски в колонке w/2 — 123..132 px.
# Дамп: камера та же, что у room_port_day (loc, rot, lens, цель), свет и мир дня те же, кадр прозрачный RGBA, аспект 0,5625.
import json, os, sys, numpy as np
from PIL import Image, ImageDraw
DP = {"furn_window": (112, 80), "furn_shelf": (152, 72), "furn_fridge": (64, 96), "furn_door": (64, 136),
      "furn_bed": (128, 64), "furn_mailbox": (48, 48), "furn_chest": (64, 48)}
FLOOR = ("furn_fridge", "furn_door", "furn_bed", "furn_chest", "furn_mailbox")
SAME = ("key", "key_t", "fill", "fill_t", "sun", "sun_t", "__world__")        # light_room дня (room.py:139-142) и мир
def png_bad(name, im):
    w, h = DP[name][0] * 3, DP[name][1] * 3
    if im.mode != "RGBA": return ["mode " + im.mode]
    if im.size != (w, h): return ["size %s != %s" % (im.size, (w, h))]
    A = np.asarray(im.getchannel("A")); m = A > 127; bad = []
    if not m.any(): return ["empty"]
    ey, ex = np.where(A > 0)
    if max(A[0, 0], A[0, -1], A[-1, 0], A[-1, -1]) or min(ex.min(), ey.min(), w - 1 - ex.max(), h - 1 - ey.max()) < 2:
        bad.append("edge bbox a>0 %s" % ((ex.min(), ey.min(), ex.max(), ey.max()),))
    ys, xs = np.where(m); x0, x1, y0, y1 = xs.min(), xs.max(), ys.min(), ys.max()
    if max((x1 - x0 + 1) / w, (y1 - y0 + 1) / h) < 0.88: bad.append("fill %.2f" % max((x1 - x0 + 1) / w, (y1 - y0 + 1) / h))
    if name in FLOOR and h - 1 - y1 > 9: bad.append("floor gap %d px" % (h - 1 - y1))
    if name == "furn_shelf":
        col = np.where(m[:, w // 2])[0]
        if not len(col) or not 123 <= col[0] <= 132: bad.append("shelf top %s" % (col[0] if len(col) else None))
    if name == "furn_window":
        L = Image.fromarray(np.where(A < 8, 0, 255).astype(np.uint8)).copy(); ImageDraw.floodfill(L, (0, 0), 128)
        holes = int((np.asarray(L) == 0).sum())
        if holes: bad.append("holes %d px" % holes)
    return bad
def dump_bad(D, R):
    same = lambda k, f: [round(v, 3) for v in D[k][f]] == [round(v, 3) for v in R[k][f]]
    v = D["__view__"]; bad = []
    if not (same("cam", "loc") and same("cam", "rot") and same("cam_t", "loc") and D["cam"]["cam"] == R["cam"]["cam"]): bad.append("camera")
    if not v["film_transparent"] or v["color_mode"] != "RGBA" or v["aspect"] != 0.5625: bad.append("view %s" % v)
    bad += ["light/world %s" % k for k in SAME if D.get(k) != R.get(k)]
    return bad
def selfcheck():
    ok = Image.new("RGBA", (192, 288)); ImageDraw.Draw(ok).rectangle((10, 12, 181, 281), fill=(200, 200, 200, 255))
    assert not png_bad("furn_fridge", ok), png_bad("furn_fridge", ok)
    m = ok.copy(); ImageDraw.Draw(m).rectangle((90, 140, 99, 149), fill=(0, 0, 0, 0))
    assert not png_bad("furn_fridge", m), "SELFCHECK FAIL: просвет у холодильника краснеет %s" % png_bad("furn_fridge", m)
    w = Image.new("RGBA", (336, 240)); ImageDraw.Draw(w).rectangle((10, 12, 325, 235), fill=(200, 200, 200, 255))
    assert not png_bad("furn_window", w), png_bad("furn_window", w)
    ImageDraw.Draw(w).rectangle((150, 100, 159, 109), fill=(0, 0, 0, 0))
    assert png_bad("furn_window", w), "SELFCHECK FAIL: дыра в окне не покраснела"
    for mut in ("edge", "soft", "small", "float", "rgb"):
        m = ok.copy(); d = ImageDraw.Draw(m)
        if mut == "edge": d.rectangle((0, 100, 5, 110), fill=(0, 0, 0, 255))
        if mut == "soft": d.rectangle((0, 283, 191, 287), fill=(0, 0, 0, 20))       # мягкая тень до края кадра (α 20)
        if mut == "small": m = Image.new("RGBA", (192, 288)); ImageDraw.Draw(m).rectangle((60, 100, 130, 200), fill=(9, 9, 9, 255))
        if mut == "float": m = Image.new("RGBA", (192, 288)); ImageDraw.Draw(m).rectangle((10, 12, 181, 260), fill=(9, 9, 9, 255))
        if mut == "rgb": m = ok.convert("RGB")
        assert png_bad("furn_fridge", m), "SELFCHECK FAIL: мутант %s не покраснел" % mut
    for top, want in ((126, False), (60, True)):                                       # доска полки на 42 dp проходит, на 20 dp — нет
        sh = Image.new("RGBA", (456, 216)); ImageDraw.Draw(sh).rectangle((10, top, 445, 205), fill=(200, 200, 200, 255))
        assert bool(png_bad("furn_shelf", sh)) == want, "SELFCHECK FAIL: полка с доской на %d px %s" % (top, png_bad("furn_shelf", sh))
selfcheck()
pd, dd, R = sys.argv[1], sys.argv[2], json.load(open(sys.argv[3], encoding="utf-8"))
S = dict(R, __view__=dict(R["__view__"], film_transparent=True))                 # эталон дампа спрайта: камера, свет и мир дня
assert not dump_bad(S, R), "SELFCHECK FAIL (эталон дампа): %s" % dump_bad(S, R)
M = dict(S, cam=dict(R["cam"], cam=dict(R["cam"]["cam"], lens=37.0)))
assert dump_bad(M, R), "SELFCHECK FAIL: мутант lens не покраснел"
M = dict(S, sun=dict(R["sun"], light=dict(R["sun"]["light"], energy=R["sun"]["light"]["energy"] + 1)))
assert dump_bad(M, R), "SELFCHECK FAIL: мутант силы солнца не покраснел"
bad = []
for n in DP:
    p, j = os.path.join(pd, n + ".png"), os.path.join(dd, n + ".json")
    bad += ["%s: %s" % (n, b) for b in (png_bad(n, Image.open(p)) if os.path.exists(p) else ["no png"])]
    bad += ["%s: %s" % (n, b) for b in (dump_bad(json.load(open(j, encoding="utf-8")), R) if os.path.exists(j) else ["no dump"])]
print("SPRITES OK" if not bad else "SPRITES FAIL %s" % bad); sys.exit(bool(bad))
```

```python
# zones.py — TOWN-A1d1: зоны подписей UI на спрайтах мебели. python zones.py REPO SPR_DIR ROOM_DAY.png|webp → LABEL ZONES OK / FAIL, exit 0/1
# Спрайт кладётся на новую оболочку в нынешние координаты мишени 360 × 640 (RoomScreen.kt, 3 px/dp), в зоне подписи —
# контраст G.ink (пороги art_check.bg: среднее ≥ 4,5, p10 ≥ 3,0); у конверта-Canvas — G.purpleDeep #310F53 (GameTheme.kt:25).
import sys; sys.path.insert(0, sys.argv[1] + "/tools"); from art_check import lum, INK
import numpy as np; from PIL import Image
D, BG = sys.argv[2], Image.open(sys.argv[3]).convert("RGBA")
PD = (0x31, 0x0F, 0x53)
# (спрайт, x, y мишени в dp; зона в dp внутри мишени (x0, y0, x1, y1); цвет подписи; порог среднего, p10)
Z = [("fridge", 8, 286, (0, 66, 64, 90), INK, 4.5, 3.0),     # «Список» низ, padding 6 dp, 14 sp при 1,3 ≈ 23,4 dp
     ("door", 288, 286, (0, 106, 64, 130), INK, 4.5, 3.0),   # «Улица»
     ("bed", 116, 476, (40, 20, 88, 44), INK, 4.5, 3.0),     # «Сон» по центру (эмодзи 🛏 уходит в A1d2)
     ("mailbox", 304, 492, (0, 20, 48, 46), INK, 4.5, 3.0),  # «+N»
     ("mailbox", 304, 492, (13, 3, 35, 19), PD, 3.0, 3.0),   # конверт-Canvas (контур 2 dp — графика, порог 3 : 1)
     ("shelf", 128, 198, (0, 36, 152, 72), INK, 4.5, 3.0)]   # числа под банками ui_jar
def ratio(A, c):
    L, Lc = lum(A), float(lum(c)); return (L.mean() + .05) / (Lc + .05), (np.percentile(L, 10) + .05) / (Lc + .05)
def zone(im, x, y, z):
    c = BG.crop((x * 3, y * 3, x * 3 + im.width, y * 3 + im.height)); c.alpha_composite(im)
    return np.asarray(c.convert("RGB")).astype(int)[z[1] * 3:z[3] * 3, z[0] * 3:z[2] * 3]
dark = Image.new("RGBA", (192, 408), (0x6B, 0x3A, 0x1E, 255))                       # мутант: тёмная дверь
assert ratio(zone(dark, 288, 286, Z[1][3]), INK)[0] < 4.5, "SELFCHECK FAIL: тёмная дверь прошла"
light = Image.new("RGBA", (192, 408), (0xFF, 0xFB, 0xF6, 255))                      # эталон: белая дверь #FFFBF6
assert ratio(zone(light, 288, 286, Z[1][3]), INK)[0] >= 4.5, "SELFCHECK FAIL: белая дверь не прошла"
bad = []
for n, x, y, z, c, m, p in Z:
    r = ratio(zone(Image.open(f"{D}/furn_{n}.png").convert("RGBA"), x, y, z), c)
    if r[0] < m or r[1] < p: bad.append("%s %.2f/%.2f" % (n, *r))
print("LABEL ZONES OK" if not bad else "LABEL ZONES FAIL %s" % bad); sys.exit(bool(bad))
```

```python
# feas.py — TOWN-A1d1, зонд осуществимости до спавна: коробка-«холодильник» у задней стены BASE-комнаты, камера комнаты, рендер части кадра.
# Blender: "$BL" -b --factory-startup --python-exit-code 1 --python feas.py -- ART_DIR OUT.png shadow|noshadow   (ART_DIR — tools/art BASE)
# Проверка: python feas.py --check SH.png NS.png → FEAS OK / FAIL, exit 0/1. Кадр по проекции (заполнение 0,9 по высоте), не мишень × 3.
import sys
if sys.argv[1:2] == ["--check"]:
    import numpy as np; from PIL import Image
    def edge(f):
        A = np.asarray(Image.open(f).getchannel("A")); h, w = A.shape; ey, ex = np.where(A > 0)
        c = [int(A[0, 0]), int(A[0, -1]), int(A[-1, 0]), int(A[-1, -1])]; e = [int(A[:2].max()), int(A[-2:].max()), int(A[:, :2].max()), int(A[:, -2:].max())]
        print(f, "size", (w, h), "corners", c, "edge2px t/b/l/r", e, "margin a>0", int(min(ex.min(), ey.min(), w - 1 - ex.max(), h - 1 - ey.max())))
        return max(c) == 0 and max(e) == 0
    sh, ns = edge(sys.argv[2]), edge(sys.argv[3])
    assert not sh, "SELFCHECK FAIL: у тени нет α на краях — ловец тени не сработал, зонд ничего не мерит"
    print("FEAS OK" if ns else "FEAS FAIL: без тени край кадра не прозрачный"); sys.exit(not ns)
import os, bpy
art, out, mode = sys.argv[sys.argv.index("--") + 1:][:3]
sys.path.insert(0, art); import room
from lib import *
from bpy_extras.object_utils import world_to_camera_view
from mathutils import Vector
scene = reset_scene(32, width=1080, height=1920); k = 0.68
wood = [material("wood%d" % i, mix(hexc("#E9B983"), hexc("#D9A36C"), i / 3)) for i in range(4)]
m_wall, m_side, m_white = material("wall", hexc("#EAE2F6")), material("side", hexc("#DCD2EE")), material("white", hexc("#FFFBF6"))
plank_floor(wood, k); shell_walls(m_wall, m_side, m_white, k, room.WALL_Y, window=None)
for o in list(scene.objects):
    if mode == "shadow": o.is_shadow_catcher = True
    else: o.visible_camera = False
fr = box("fridge", (-2.3, 2.55, 0.9), (0.9, 0.8, 1.8), m_white, bevel=0.08)
room.light_room(scene, False, (0, 0))
camera(scene, (0, -10.5, 3.1), (0, 2, 2.0), lens=36)
bpy.context.view_layer.update()                                  # TRACK_TO камеры (lib.py:217-219, :243-244)
pts = [world_to_camera_view(scene, scene.camera, fr.matrix_world @ Vector(c)) for c in fr.bound_box]
xs = [p.x * 1080 for p in pts]; ys = [(1 - p.y) * 1920 for p in pts]
x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
H = (y1 - y0) / 0.9; W = H * 192 / 288; cx = (x0 + x1) / 2; by1 = y1 + 3; by0 = by1 - H
r = scene.render; r.use_border = True; r.use_crop_to_border = True
r.border_min_x, r.border_max_x = (cx - W / 2) / 1080, (cx + W / 2) / 1080
r.border_min_y, r.border_max_y = 1 - by1 / 1920, 1 - by0 / 1920
render(scene, out)
```

## ВОПРОСЫ НА ВОРОТА (продолжают §18 с № 76; лист A1d — отдельный, ответ одной пачкой)
«Один общий лист на ворота» № 61 б (GAME_CONCEPT.md:2079) — про пилот; его ворота закрыты в 1380e99, поэтому лист A1d
свой — сказать владельцу вместе с листом.
Кадры листа:
- **К1** — оболочка день и вечер целиком, 360 × 640 и кадр S23 (× 1,219, срез 118 px), рядом нынешние `room_port_*` из
  res; часы обведены.
- **К2** — PIL-макет «Дом» на К1 (подпись «макет, не сборка»): плашки сняты, 7 спрайтов 1:1 в мишенях по снимкам
  `emu_s12f_room.png` (неделя 1) и `emu_j1a_s23_room_w5.png` (неделя 5), житель 64 dp на подоконнике, «!» в кружке
  `#E0004A` с белой обводкой (№ 75 а), банки `ui_jar` на полке; шрифт 1,0 и 1,3; варианты № 76 а и б (для а — проверить,
  что в левой колонке и под копилкой нет наложений при 1,0 и 1,3); при 1,0 и 1,3 — банки стоят на доске; копилка и
  словарик по № 81 а и б; на S23 — сетка 2 × 2: № 76 а / б × `SHADOW` True / False (при б тень висела бы в воздухе,
  № 79). Это лист «Дом» до/после из строки эпика (:42); живая сборка — A1d2.
- **К3** — окно 1:1 и × 2 с Осей (неделя 1) и Асей (неделя 5) на подоконнике, «!» в кружке; три варианта № 77; линия
  верха подоконника.
- **К4** — 7 спрайтов 1:1 и × 2 на новой оболочке в координатах их мишеней на 360 × 640 и S23, в цвете, в сером и при
  дейтеранопии; зоны подписей с текстом «Список», «Улица», «Сон», «+3», «12» при 1,3; `SHADOW` False (основной) / True
  (вариант, тень срезана кадром).
- **К5** — ночь на вечерней оболочке под вуалью `purpleDeep` α 0,35 (WORKFLOW № 30), три варианта № 78 с панелью
  ночи, рядом ночь из нынешнего res (окно со звёздами и торшер уходят) и пара «комната днём → ночь» на 360 × 640
  (мебель и окно ночью исчезают); числа яркости (приёмка п. 7). Итог недели не показываем: `Panel` на весь экран с
  отступом 8 dp (`NightScreens.kt:86-87`), фона не видно. «До» — из res: это рендер HEAD room.py (средняя |Δ| 1,06,
  a1d_research.md:43).
- **К6** — ответы слепого судьи (приёмка п. 11).
- **К7** — титул, банки, улица (до A1g2) на новой дневной оболочке — PIL-подмена фона на снимках 360 × 640 и S23 при 1,3,
  рядом нынешние (`room_port_day` — фон 12 экранов, TOWN-A1c.md:47).

76. **Мебель на полу** (К2; раскладку меняет A1d2):
    - а) холодильник и дверь ставятся низом на линию пола фона через Crop (≈ 409 dp на 360 × 640, ≈ 498 dp на S23);
      сундук, словарик, кровать и почтовый ящик остаются на полу ближе к камере, где они сейчас; размеры мишеней те же
      (сундук под холодильником в той же колонке, RoomScreen.kt:151-158: «низом на линию» для обоих дал бы наложение);
    - б) раскладка как сейчас — на 360 × 640 холодильник висит на 27 dp над полом, дверь уходит на 13 dp ниже стыка
      стены и пола; на S23 холодильник и дверь на 124 / 84 dp выше пола;
    - в) отдельная задача раскладки комнаты после A1d2.
    **Рекомендую а**: стоящая на полу мебель делает комнату «настоящей», на S23 при б холодильник висит на стене.
    Цена а — `RoomScreen` считает линию пола (`BoxWithConstraints`), проверка наездов при 1,3 на К2; левая колонка на
    360 × 640 — 286..540 dp (BAN зонда часов): под холодильником с низом на 409 dp остаётся 131 dp, а `spot_3` 36 +
    сундук 48 + словарик 48 = 132 dp — при занятом `spot_3` не влезает на 1 dp (расчёт, RoomScreen.kt:151-158), решает A1d2.
77. **Окно** (К3; житель во всех вариантах стоит на подоконнике перед стеклом):
    - а) небо с облаком, без переплёта (`WINDOW_VIEW = "sky"`);
    - б) как а, плюс внизу полоска газона и забора — «кусочек улицы» (`"fence"`);
    - в) как б (небо, газон и забор), плюс крест переплёта, как в нынешней комнате (`MULLION = True`).
    **Рекомендую б**: № 17 называет окно «живым кусочком улицы» — забор читается как улица, а не картина; у забора
    встанет достигнутая мечта (решение 25, GAME_CONCEPT.md:586, :2042) — при а ей остаётся подоконник рядом с жителем
    (48 dp из 112); крест за жителем 64 dp дробит оставшееся стекло окна 80 dp на мелкие куски — проверить на К3. Если «ребёнок» не узнаёт улицу и на б — а
    (проще).
78. **Вечер без торшера** (К5; окна ночью нет — луна `ui_moon` на экране ночи):
    - а) без тёплого света (`WARM = None`) — ровный синий вечер, темнее нынешнего;
    - б) тёплое пятно у центра пола, где стоит питомец (`WARM = (0.0, -1.0)`), — «ночник»;
    - в) как сейчас — тёплый свет справа, где стоял торшер (`WARM` BASE): блик на стене без лампы.
    **Рекомендую б**: вечер остаётся уютным без новой вещи (ТЗ 3.5); при а — если «ребёнок» назовёт К5 «темно/страшно»
    или яркость ниже нынешней (п. 7), а отпадает. Риск б: свет без источника — судья «ребёнок» на К5.
    Ещё риск б: центр пятна (0; −1,0; 0) камерой комнаты — 180 × 468 dp на 360 × 640, а панель ночи идёт под питомцем
    180 dp (луна 72 + питомец 180 + `Panel`, по центру, NightScreens.kt:55-67) — по расчёту раскладки ≈ с 340–390 dp до
    530–580 dp: пятно, вероятно, под панелью, проверить на К5. В неделю 3 (С2) лампа над кроватью сломана
    (content.json:1158, :1701) — пятно б горит и тогда.
79. **Тень под мебелью** (К4):
    - а) мягкая тень на полу и стене запечена в спрайт (`SHADOW = True`, ловцы тени оболочки); тень срезается краем кадра
      мишени (зонд `feas.py`: α на краях до 121) — принять а значит в круге A1d1-2 менять кадр или критерий края, правило
      края для тени задаётся тогда, по замеру;
    - б) без тени (`False`, основной рендер).
    Рекомендация — по К4 (обе картинки на листе); при № 76 б тень висела бы в воздухе.
80. **Оболочка и 7 спрайтов целиком** (К1, К4, К6, К7): пустая стена, гирлянда, часы на новом месте, вид мебели, те же
    стены под титулом, банками и улицей.
    - а) принять;
    - б) правки по списку владельца — круг A1d1-2 (только названные предметы).
    **Рекомендую а**, если слепой судья назвал все 7 предметов в сером.
81. **Копилка и словарик** (К2; их мишени — `Target` с белой плашкой α 0,85 по умолчанию, RoomScreen.kt:145-146, :158,
    TownUi.kt:160; в A1d2 фон снимается только у 7 мишеней мебели):
    - а) белые плашки, как сейчас;
    - б) без плашки: свинка висит на стене рядом с полкой, книга лежит на полу (`ui_piggy` и `ui_book` уже с прозрачным
      фоном).
    Рекомендация — по К2.

Решения спеки без вопроса — сообщить владельцу вместе с листом (любое можно развернуть — круг A1d1-2):
- 3 px/dp — родная плотность эмулятора и S23 (TOWN-A1g1.md:52); спрайты только дневные (`GameApp.kt:185`); следствие —
  ночью мебели и окна в комнате нет (сейчас ночью видно окно со звёздами и торшер, room.py:59-67, :117-123); видно на К5;
  вернуть — кругом A1d1-2 (дешевле всего — A1d2 рисует дневные `furn_*` на Night под вуалью: Kotlin, нового арта нет;
  мишеней ночью нет, NightScreens.kt:54-67);
- имена `furn_<id>` — правило эпика (TOWN-A1.md:70) для мебели имени не даёт, `room_*` занят оболочками (LICENSES.md:51),
  `home_*` — id стартовых вещей в `content.json`;
- полка — доска на кронштейнах, банки с крышками и числами остаются прежними `ui_jar`/`ui_lid_*` поверх; буква № 27
  («окно и полка с банками — тоже спрайты», GAME_CONCEPT.md:2045) читаем как мишень «Полка с банками» (§10.1, :1334) —
  это толкование, назвать владельцу явно (одна банка на весь проект — та же на экране «Банки»; вариант «банки
  запечены» без кода не показать — только кругом);
- почтовый ящик без конверта (конверт с пульсом — Canvas UI, пульс отключаем);
- «!» в окне — как № 75 а: кружок `#E0004A`, белый «!», белая обводка 2 dp (белый на `#E0004A` 4,92 : 1, кружок на небе
  `#8ED4FF` 3,05 : 1; нынешний «!» на `G.sky` — 1,68 : 1, ТЗ 3.6 не проходит); делает A1d2; подписи мебели — живой текст на
  светлых зонах спрайта (A1d2);
- камера, дневной свет и цвета стен не меняются (их копии в place.py и facade.py).

## A1d2 — встройка (набросок)
После ворот № 76–81, по очереди с A1e/A1g2 (все трогают `game/`); test-author не нужен (TOWN-A1.md:71).
- WebP в `game/res/drawable-nodpi`: 2 оболочки портрета (замена, `--rgb`) и 7 `furn_*`; ссылки — `when` с явными
  `R.drawable` (R8, TOWN-A1.md:68-69); LICENSES — строка «Комната: оболочка и мебель | `room_port_*`, 7 `furn_*` |
  `room.py --only` / `--sprites` → `to_webp.py`», итог «52 файла» (LICENSES.md:58, ARCHITECTURE.md:28) → новое число.
- `RoomScreen`: снять плашки (:135-136) и фон `Target` у 7 мишеней (параметр `color`, `TownUi.kt:160` — умолчание для
  других экранов не менять); убрать эмодзи :157, :175, линию :153, точку-ручку :182; конверт-Canvas и «+N» остаются;
  окно — спрайт + `ResidentPic` 40 → 64 dp ногами на подоконнике (:141) + «!» в кружке (№ 75 а); раскладка по № 76;
  копилка и словарик по № 81; `contentDescription` 7 мишеней не меняются (RoomScreen.kt:140, :152, :157, :174, :181,
  :200, :235).
- Мечты в окне — спрайт мечты у забора справа от жителя, размер — TODO по К3 A1d2; или строка в BACKLOG с явным отказом
  (решение 25, GAME_CONCEPT.md:2042).
- Строка «Почтальон принёс конверт…» (`GameViewModel.kt:261`) — портрет Оси `res_osya` вместо питомца (`TownUi.kt:298`);
  у `Line` нет поля «кто говорит» (`GameViewModel.kt:68`) — минимальная правка в `game/`.
- Инструменты: `art_check.py bg`/`which` читают `room_port_day.webp` из res (:191, :257) и сверяют со снимками
  `emu_b_*` — после замены фона пересчитать маску (`bg - - --selfcheck`).
- Приёмка: лист «Дом» до/после живьём (недели 1 и 5, 360 × 640 и S23, 1,0 и 1,3), мишени ≥ 48 dp с `contentDescription`,
  `perf.sh`, прирост APK ≤ доли A1d, запись кроссфейда день → вечер.
- Подписи мишеней 14 sp — исключение UX_ACCESSIBILITY.md:91-98; после снятия плашек оно держится, пока подписи стоят
  на светлых зонах спрайта (приёмка п. 8). Строку в BACKLOG не заводить.

## ДОКИ (после приёмки A1d1; что станет ложью)
- `docs/tasks/TOWN-A1.md:42` — статус «A1d1 сделано (генератор), A1d2 — встройка»; «(`props.py --room` или своя функция на
  частях lib)» → «(`room.room_camera` + `room.sprite_frame`)», если коммит BASE этого ещё не сделал.
- `docs/GAME_CONCEPT.md:1968` «камерой комнаты (`--room`)» → «камерой комнаты (`room.room_camera` + `room.sprite_frame`)».
- `docs/GAME_CONCEPT.md:1831` — «камерой комнаты (`props.py --room` …)» → `room.room_camera` + `room.sprite_frame`;
  стартовые вещи «пересборкой room.py» → A1e (TOWN-A1.md:43); :1797 «сейчас коврик, торшер и мячик запечены» —
  после слияния верно только для res до A1d2: «в res до A1d2».
- `finny-pet/docs/ARCHITECTURE.md:185`, `BUILD_AND_DEMO.md:303` — добавить `--sprites`, `furn_*` (RGBA); `LICENSES.md:51` — в A1d2.
- `docs/GAME_CONCEPT.md:1333` — по ответу № 77: при а «самокат у забора» снять (мечты — на подоконнике).
- `docs/tasks/TOWN-A1g1.md:443`, `TOWN-J1-1b.md:475-477` — если A1d1 сливается раньше 1b3 или A1g: сослаться на сборный
  эталон HANDOFF (их `DUMP DIFF EMPTY` иначе покраснеет на `room_*`).
- `docs/HANDOFF.md` — статус третьего генератора, путь `$T/a1d/d1`, сборный эталон регресса; `docs/GAME_CONCEPT.md` §18 —
  № 76–81 с ответами.
- Грепы по обоим `docs/`, `tools/`, `finny-pet/tools/`:
  `git grep -n -e "--room" -- docs finny-pet/docs tools finny-pet/tools ':!docs/tasks/TOWN-A1d1.md'` → пусто (фиксированная
  строка через `-e`: сейчас находит GAME_CONCEPT.md:1831, :1968 и TOWN-A1.md:42);
  `git grep -n "kept empty\|stays empty" -- finny-pet/tools` → пусто;
  `git grep -n "room.py (room_port_day)" -- tools docs` → пусто.

## ПРИ БЛОКЕРЕ
`STATUS: BLOCKED` с одним вопросом и остановка, если:
- кадр спрайта нельзя получить ровно `w·PX × h·PX` px рендером части кадра (округление `use_border`);
- предмет при `SHADOW = False` не влезает в кадр мишени при камере комнаты (край, заполнение, низ — CONTRACT п. 6);
- нужен примитив вне lib.py (плоскость с текстурой, градиент) или импорт place/props/facade/uiprops (новый цвет — не
  блокер: `TODO` и строка в отчёте, CONTRACT п. 7);
- часы не проходят `CLOCK OK` ни в одной точке без изменения их размера;
- зона подписи (`zones.py`) не проходит ни при каком светлом материале предмета из литералов room.py — подложку в
  спрайте не изобретать;
- регресс даёт разницу вне `room_port_day.json`/`room_port_evening.json`, или `ROOM KEYS` требует менять камеру,
  дневной свет, пол или гирлянду;
- `--only room_port_day --out F --preview` перестаёт работать как прежде.

Не изобретать, protect не трогать.

## Журнал спеки
- **2026-09-28, судья-синтезатор (r1).** Входы: исследование `a1d_research.md`, черновики «минимальный проверяемый срез»
  (`a1d_draft_min.md`) и «ребёнок и комната как дом» (`a1d_draft_child.md`), зонды min в `…/22cbd09a-…/scratchpad/a1d_min/`.
  - Баллы 1–5 (верность решениям и волне / ребёнок и узнаваемость / проверяемость / точность фактов / реализуемость в
    worktree / объём и бюджет / ясность вопросов): **min** 5 / 3 / 4 / 4 / 4 / 4 / 4 = 28; **child** 3 / 5 / 3 / 4 / 4 / 4 / 4 = 27.
    Child: ортокамера «того же направления» — не камера комнаты (перспектива спрайта разошлась бы с фоном, № 28),
    `SHELF_JARS` требует банок в room.py ради отвергнутого варианта, `--preview` половиной размера ломает точный кадр,
    `keys.py` краснел на переименовании света луны (оставлен TODO), прямоугольники часов — TODO. Основа — **min**.
  - Из min: шапка, CONTRACT (пустая оболочка, часы по зонду, `WARM`, `room_camera`, перспективный кадр рендером части
    кадра, `SHADOW` ловцами тени, CLI `--only`/`--sprites`, импорты), SCOPE, ДО СПАВНА, зонды `room_keys.py`,
    `clock_probe.py`, `sprite_check.py`, варианты констант, слияние, сборная эталонная база, ПРИ БЛОКЕРЕ.
  - Из child: таблица «что увидит ребёнок», силуэты и «цвет — не единственный признак», светлые зоны подписей и зонд
    зон, ящик не малиновый круглый, светлая дверь, окно с забором (`WINDOW_VIEW`, № 17), `room_port_evening` в регрессе
    коммитом BASE, пара строк самотеста 8–9 (пропуск / блок по № 29), яркость вечера числом, 14 sp → BACKLOG.
  - Отклонено: ортокамера, `FLOOR_LINE` печатью (число в спеке — 408,8 dp), `CLOCK_ON` (часы проверяет проекция),
    `--sprite` отдельным флагом, вопрос о банках (вариант «запечены» без кода не показать — решение спеки без вопроса).
  - Правки судьи (проверено на HEAD d209762):
    - `room_keys.py`: регэксп `\.\d{3}` стирал цифры чисел в JSON (0,7745 и 0,8045 → «05») — сдвиг цвета или координаты
      в 2–4-м знаке проходил молча. Исправлено на `\.\d{3}(?=["_]|$)` + мутант «цвет доски +0,03»: старый текст —
      `SELFCHECK FAIL`, новый — `ROOM KEYS OK` на синтетике kept/none/moved, `d0 d0` → FAIL, kept на none → FAIL.
    - `clock_probe.py`: + места `spot_1` (стартовый цветок, `content.json` `home_flower`) и `spot_6`; BASE → прежний FAIL.
    - `zones.py` (у child не прогонялся): самопроверка тёмная/белая дверь; прогон — светлое дерево OK, фиолетовая
      дверь FAIL 1,34, прозрачная FAIL 3,36.
    - `sprite_check.py` — без изменений (совпал с файлом зонда min).
    - Факты: 45 RGBA-спрайтов (исследование писало 51; медиана 0,2224, p75 0,2685 — замер судьи); `place.py:22`, а не :21;
      fill 0,9 — `props.py:340`; кроссфейд `GameApp.kt:259-260`; два Blender — TOWN-J1-1b.md:743; стоп-хук — :739,
      WORKFLOW № 41; почтовый ящик :235-243; HEAD d209762 (после 864dd2c — только HANDOFF).
    - Регресс: +`room_port_evening` в BASE → 21 дамп; самопроверка lens → ровно 2 файла `['cam']`; приёмка ждёт 2 файла.
    - Вопросы перенумерованы в № 76–80 (№ 75 занят); вариант «б» вечера — точка (0,0; −1,0) из room.py:96-97, а не
      «над кроватью» (на ночи мишеней и кровати нет).
    - Бюджет room.py 170/90: перенос кода окна и полки в builder-ы считается и в удалениях, и во вставках.
    - Зонды — блоками верхнего уровня с именем файла в первой строке и извлекателем (Temp может быть почищен).
  - Проверено судьёй: скоуп на HEAD — `[] []`, непокрыт только `room.py`; `.claude/` = родитель 8dbfd2d; AST-зонд на
    facade.py → `['place']`, на room.py → `[]`; `art_check.py bg` на res `room_port_day` → `CONTRAST OK`; размеры 4 WebP res.
  - Не проверено: рендер спрайта точного размера через `use_border`, реальные WebP спрайтов, самотест хука в `wt/a1d`,
    прямоугольники S23 зонда часов (расчёт по раскладке), три Blender одновременно.
- **2026-09-28, круг критиков (r2), редактор.** Находки — в редакции скептиков: confirmed — целиком, partial — только
  подтверждённая часть, refuted — не применялись. Правки — в самих утверждениях, сверка по коду на HEAD 1380e99.
  - Продукт и арт: 14 находок, подтверждено 7, частично 5, опровергнуто 2. Приняты: P2, P4, P5, P8, P11, P12, P13
    (целиком); P1, P3, P7, P10, P14 (подтверждённая часть). Отклонены: P6 (порог 3 : 1 для края графики выдуман, в ТЗ 3.6
    — контраст текста), P9 (яркость вариантов меряет п. 7, лампа С2 ночью не рисуется).
    - P1: строка «вечер / ночь» — пустая ночная комната; следствие дописано к «спрайты только дневные»; К5 — рядом ночь
      из res; отдельного вопроса нет (сказано в КОНТЕКСТ и ANTI-SCOPE). P3: ящик — на столбике, на полу, в `FLOOR`;
      ловцы тени по месту предмета. P5 → № 81 (вариант «полка шире мишени» отклонён: ломает «спрайт = мишень × 3»).
      P7: «в» = забор + крест; довод «режет жителя» заменён. P10: «где банки?» — только по К2, самопроверка — пустышки.
      P14: подоконник «не выше 184 px», числа критика (216, 12 px) не взяты.
  - Инженерия и волна: 11 находок, подтверждено 10, частично 1, опровергнуто 0. Приняты: E1–E6, E8–E11 (целиком), E7
    (подтверждённая часть: `view_layer.update()` и правило выравнивания `floor`).
    - E1: `WARM_MV` без `lamp_t` — moved прогнан на реалистичном d1 (`lamp_t` на месте, часы одним сдвигом) → OK, а не
      только на синтетике. E5: у сдвинутых — только `loc`, часы одним вектором; синтетика двигает часы одним сдвигом
      (прежняя ставила все 5 частей в одну точку — новый зонд её справедливо краснит). E2 (вместе с F2): `SHADOW = False`
      по умолчанию, `v shadow` — только К4; зонд `feas.py` в «До спавна» п. 7а. E11: HEAD 1380e99, §18 +1, `:109`.
  - Полнота и исполнимость для арт-кодера: 10 находок, подтверждено 4, частично 5, опровергнуто 1. Приняты: F2, F3, F5,
    F6 (целиком); F1, F4, F7, F8, F9 (подтверждённая часть). Отклонена: F10 (результат `WARM` задан однозначно и ловится
    `room_keys.py kept`).
    - F1: зонды копирует кодеру оркестратор (п. 11а), команды — в SCOPE «Самопроверка кодера». F2: край по α > 0,
      заполнение по α > 127. F4: `floor=True` — низ + 3 px; шкала 0,021 не внесена. F5 = P4: верх доски 123..132 px,
      проверка в `sprite_check.py`. F6: К7. F7: облако — в перечне; «нужен цвет» убран из ПРИ БЛОКЕРЕ (TODO + отчёт).
      F8 = E11. F9: состав сообщения, если образца нет.
  - Прогнано редактором: `room_keys.py` — 3 × 3 реалистичных d1 (диагональ OK, перекрёстные FAIL), `d0 d0` FAIL, d1 с
    `clock_rim` ×2 и красным FAIL; `sprite_check.py` — самопроверка (6 мутантов, 2 полки, lens) проходит, пустой каталог →
    FAIL; `feas.py` на HIP (Blender 5.2, 32 сэмпла): тень — углы α [0, 28, 45, 3], края [28, 65, 121, 53]; без тени — 0,
    отступ 5 px, `FEAS OK`; извлекатель п. 4 на r2 → 5 файлов, тексты совпали с прогнанными.
  - Не проверено: рендер спрайта точного размера `w × h` через `use_border` (зонд `feas.py` кадрирует по проекции, размер
    206 × 310); `zones.py`, `clock_probe.py` не менялись; самотест хука в `wt/a1d`; три Blender одновременно.
- **2026-09-28, проверка r2 → r3, редактор.** 10 несоответствий проверяющего сверены по коду HEAD 1380e99; приняты все
  10, отклонённых нет.
  - 1 (high): при центре по y верх проекции полки ≤ 108 px, а п. 7 требует 123..132 — `sprite_frame` получил `top=None`,
    у полки `top=126` (CONTRACT п. 5, 6, строка `furn_shelf`: проекция ≤ 88 px); `sprite_check.py` и `FLOOR` не менялись.
  - 2: «№ 76–80» → «№ 76–81» в Слиянии, A1d2, ДОКИ (строка журнала r1 — история, не правилась).
  - 3: `dump()` без `makedirs` (art_check.py:95) — в самопроверку кодера `mkdir -p <sc>/d <sc>/sd <art>`; путь `BL` — в
    SCOPE и п. 12. 4: кодеру копируются `room_keys.py` и `d0/` (2 дампа), в SCOPE — вечерний дамп и `ROOM KEYS OK`;
    критерий зондов в SCOPE и п. 12 — «п. 1–3, 6 и 7» (закрывает и 10).
  - 5: d1 — `[50, 54]` при kept/moved, `[50, 50]` при none. 6: art_check.py — регресс :102-113, фасады :110-111 (:109
    комментарий). 7: UX_ACCESSIBILITY.md:91-98 (строка «Предметы комнаты» — :98). 8: плотности пересчитаны редактором на
    45 RGBA-WebP res — медиана 0,222384, p75 0,268463; 58 026, 105 074, 153 638, 364 957, 227 954 без изменений.
    9: тёплый свет ночью — по № 78.
  - Не проверено: рендер полки с `top=126` (осуществимость ≤ 88 px — судит кодер, иначе ПРИ БЛОКЕРЕ п. 2).
- **2026-09-29, сессия 13: сведение с версией сессии 12** (`…/22cbd09a-…/scratchpad/a1d_spec_r2.md`, её проверка —
  `$TEMP/s12/a1d_check.txt`, 11 несоответствий). Основа — r3; из r2 взято только то, что верно по коду HEAD fd50e64.
  Решения владельца № 65–75 (1380e99) учтены: № 75 а — «!» `#E0004A` с обводкой (делает J1-1b1-2), № 73 и 74 б — в
  a0915cf (pilot/a1g), № 67 б — прилавок Compose-полосой (place.py пилота не сливается); вопросы A1d — с № 76.
  - Пересажено из r2 (с проверкой):
    1. Причина № 27 и буква «полка с банками — тоже спрайты» (GAME_CONCEPT.md:2045; в r2 цитата приписана TOWN-A1.md:20-22
       — там её нет) → КОНТЕКСТ и «Решения без вопроса» (толкование полки назвать владельцу).
    2. Имена объектов гирлянды и часов в CONTRACT п. 1 (room.py:87-94; их сверяет `room_keys.py`).
    3. Подсказка точки часов `X(-1.54)` и допуск временного перекрытия `PetBubble` (RoomScreen.kt:252, GameViewModel.kt:176,
       :187, :327) — CONTRACT п. 2; оси облачка — по пункту 11 проверки r2 (x × y). Поправлено: r2 писала «LineHost часы
       не задевает», а строка до 0,4 высоты (GameApp.kt:177) над нижним рядом (:225) доходит до ≈ 312 dp — тоже допуск.
    4. Кант `purple` и 1,22 : 1 белого на стене (пересчитано по hex) — CONTRACT п. 7.
    5. «По одной сцене на спрайт» в `--sprites` (`reset_scene`, lib.py:31-56) — CONTRACT п. 8.
    6. Предупреждение о `$TEMP/s12_scope.py`: проверено — ветки только `j1b`/`a1g`, protect уже; HANDOFF.md:66 на него
       ссылается → в BASE заменить.
    7. Довод бюджета «прирост оболочек ≈ 0» — фоны мест 38 578–45 392 Б (`git ls-tree -l`), зонд пустой оболочки
       33 280 + 38 678 Б (a1d_research.md:131).
    8. `rm -rf $T/a1d/d1` перед регрессом (E1 r2) — приёмка п. 3.
    9. Мутант координат `zones.py` (E12 r2) — «До спавна» п. 7; прогнан: полоса INK на 66..90 dp → FAIL 1,00, на 0..24 → OK.
    10. Свет и мир дня в дампе спрайта (`SAME` r2) — в `sprite_check.py` r3 их не было, а CONTRACT п. 6 требует
        `light_room` дня; ключи `key, key_t, fill, fill_t, sun, sun_t, __world__` сверены с d0 `room_port_day.json`.
    11. Дыры в альфе — только у окна (правка судьи r2): у прочих замкнутый просвет (кронштейн полки — таблица п. 7) при
        невидимой оболочке даёт α 0, и floodfill из угла считает его дырой — ложный FAIL.
    12. Варианты констант — дампами, не `cmp -s` (E11 r2): проверено по `e3/det_1.png`, `det_2.png` сессии 12 — `cmp` → 1,
        max |Δ| 2. Для `cross`/`sky` — `art_check.py diff` дампов окна; для `shadow` дамп флаги ловца не пишет
        (art_check.py:72-74) — `bbox` → ≥ 1 FAIL (своё, не из r2: у r2 проверка `Plane` от `shadow_ground`, а в r3 ловцы —
        объекты оболочки).
    13. Слепой судья: «на что здесь можно нажать?» и декор 🪴/🛋️ (content.json `homeItems`, RoomScreen.kt:219-227,
        GAME_CONCEPT.md:1346 — не :1345, ТЗ_текст.txt:389-390 — не :388-389).
    14. К2 — сетка 2 × 2 на S23 (№ 76 × `SHADOW`); К5 — пара «день → ночь», без итога недели (NightScreens.kt:86-87).
    15. Цена № 76 а — левая колонка 131 dp против 132 dp (расчёт по раскладке RoomScreen.kt:151-158 и BAN зонда часов).
    16. № 77: «при а ей негде стоять» → «подоконник рядом с жителем» (r2 № 76 а).
    17. Риски № 78 б: пятно под панелью ночи (проекция (0; −1,0; 0) → 180 × 468 dp пересчитана камерой комнаты; панель —
        расчёт по NightScreens.kt:55-67) и С2 (content.json:1158, :1701).
    18. Дешёвый возврат ночной мебели (r2 № 80 б: дневные `furn_*` на Night, Kotlin) — в «Решения без вопроса»; отдельный
        вопрос не заводим (решение r3, P1).
    19. `tasklist` перед рендером (E8 r2), исправлено: `grep -ci blender` ловит `blender-mcp.exe` (их 6) — нужен `'^blender\.exe'`.
    20. Отступление от «одного общего листа» № 61 б (GAME_CONCEPT.md:2079) — строкой в шапке вопросов.
    21. ДОКИ: GAME_CONCEPT.md:1333 по № 77; условная ссылка на сборный эталон в TOWN-A1g1.md:443 и TOWN-J1-1b.md:475-477.
    22. ПРИ БЛОКЕРЕ: зона подписи не проходит ни при каком светлом материале (без «подложки не вводить (№ 39, 40)» —
        TOWN-A1.md:74 подложку допускает).
    23. `contentDescription` 7 мишеней в A1d2 (RoomScreen.kt:140, :152, :157, :174, :181, :200, :235).
    24. HANDOFF.md:176-177 — ссылка на правило «правки protect до спавна» (пункт 7 проверки r2).
  - Отклонено из r2:
    - ORTHO `room_view` + `CAM_PORT`, вписывание «оба зазора ≤ 6 px», тень `shadow_ground` — r3 выбрал перспективу камерой
      комнаты (№ 28) и ловцы тени оболочки, зонд `feas.py` это подтверждает.
    - Константа `CLOCK` и `grep '^CLOCK'` — r3 двигает центр :89, критерий — `clock_probe.py`.
    - Вечер не в BASE (r2): довод «сломает DUMP DIFF EMPTY пилотов» опровергнут — `diff` перебирает первый аргумент
      (art_check.py:127), у A1g1 первым идёт старый d1 (TOWN-A1g1.md:443); нужная оговорка про порядок у 1b3 — в BASE.
    - `edge.py` — замер без порога, `POS` с `None` падает (пункт 5 проверки r2); порог 3 : 1 для края r3 уже отклонил
      (P6); видимость силуэта судят К4 и «ребёнок».
    - Окно «в» за стеклом (второй спрайт), «рама без переплёта» как решение — у r3 `MULLION` и три варианта № 77.
    - Полка «40..68 dp», подоконник «184..240 px», зона ящика 22..44 — у r3 свои, согласованные с `zones.py`/`sprite_check.py`.
    - Фон `Target` у 9 мишеней — у r3 вопрос № 81; рекомендация № 78 а (С2) — у r3 б, С2 добавлен риском.
    - «Цвет вне room.py — блокер» — у r3 `TODO` и строка в отчёте.
    - Грепы ДОКИ r2 («props.py --room» → «только исторические спеки»): `git grep -e "--room"` на HEAD находит ровно
      GAME_CONCEPT.md:1831, :1968 и TOWN-A1.md:42 — грепы r3 верны.
    - «Рендер HEAD совпадает с res» оставлен ссылкой на a1d_research.md:43, «ARCHITECTURE.md:192 остаётся верным» — не
      правка, не переносилось.
  - Ошибки r3, найденные при сведении и исправленные: скоуп устарел на fd50e64 (покрытие давало ещё `tools/line_close.py`,
    `tools/result_geom.py` — дописаны в protect, проверка `[] []` и покрытие перепрогнаны); `TownUi.kt:162` → :163
    (центрирование; :162 — строка модификатора); `NightScreens.kt:54-66` → :54-67 (Panel до :67); `cmp -s` вариантов
    не мог покраснеть; в `sprite_check.py` не сверялся свет; требование «импорт без побочных эффектов» было только в
    КОНТЕКСТ и приёмке — промпт кодера (п. 12) КОНТЕКСТ не включает, добавлено в CONTRACT п. 9; «итог недели» на К5 не
    виден под `Panel`; «круги идут параллельно» — кодеры кругов уже закончили (46d5e60, a0915cf).
  - Прогнано в сессии 13: извлекатель п. 4 на тексте спеки → 5 файлов; `sprite_check.py` r4 — пустой каталог → FAIL rc 1,
    синтетика (7 PNG, полка с просветом, 7 дампов = d0 с прозрачным кадром) → OK, мутант силы `key` → FAIL; `zones.py` —
    синтетика OK, мутант координат FAIL / OK; `clock_probe.py` и `room_keys.py d0 d0` на d0 → прежние FAIL; скоуп
    `[] []` и покрытие `['finny-pet/tools/art/room.py']`; команды приёмки в Git Bash — п. 2 (импорт и мутант, Blender 5.2),
    п. 6 (`v()` → пять `1`, неверный шаблон → `0`; `diff … | grep -c 'разница: [1-9]'` → 1, кириллица в конвейере
    ловится), п. 9 (`awk` → `BUDGET OK` / с порогом 1000 → `BUDGET FAIL`).
  - Не проверено: рендер спрайтов и их дампы (кода нет), положение панели ночи (расчёт, не замер), `bbox` варианта тени
    на настоящих спрайтах.
- 2026-09-29 (сессия 14): кодер в сессии `wt/a1d`, два круга. Круг 1 — DONE, 165/75; приёмка оркестратора
  (`$TEMP/s12/a1d/acc.sh` по ACCEPTANCE — автор, критик по коду, правка до отчёта кодера) — PASS 42 / FAIL 2: п. 5.4 и
  5.5 — `furn_mailbox` на финальном рендере (сэмплы по умолчанию) касался низа кадра (α > 0 до строки 142 из 144, поле
  < 2 px); кодер проверял на `--samples 16`. Круг 2 — плита-основание ящика выше и с фаской 0,03 (одна строка), 165/75;
  приёмка — **PASS 44 / FAIL 0**; `pilot/a1d` b536480. П. 7.3 (яркость вечера): приложение 0,199, оболочка 0,325, без
  тёплого 0,032, центр 0,314. WebP 7 спрайтов — 34 156 Б. Листы `town/a1d_1…a1d_7` (К6 — ответы «ребёнка» текстом в
  GATE_QUEUE), судьи: «сцена» — лучше нынешней, мешает малиновый флажок ящика; «ребёнок» вслепую — 7 из 7 вещей в цвете
  и в сером (пустышки 0 из 7); «доступность» — подписи ≥ 5,15 : 1 (p5), кроме краёв «Списка». Вопросы — GATE_QUEUE
  раздел 3 (№ 76–81, 91–92).
- 2026-09-29 (сессия 14): ответ владельца на очередь ворот (GATE_QUEUE раздел 3) — «по рекомендациям»: № 76 **а**,
  77 **б**, 78 **б**, 79 **б**, 80 **б** (только флажок почтового ящика — золотой), 81 **а**, 91 **б**, 92 **б**; § 18 —
  строки № 76–81, 91–92. Следует:
  - круг A1d1-2 (сообщение обёртке `wt/a1d`): флажок `mail_flag` золотой — `p["m_gold"]` вместо `p["m_mag"]`
    (room.py:140 на b536480; № 80 б) и `WARM = (0.0, -1.0)` (№ 78 б: на `pilot/a1d` стоит вариант в, `(4.0, 1.2)`);
    `WINDOW_VIEW = "fence"`, `MULLION = False` (№ 77 б) и `SHADOW = False` (№ 79 б) там уже стоят;
  - затем перенос `room.py` из `pilot/a1d` в `feat/town` (регресс `art_check.py regress` и `diff` с BASE-дампами, как
    184843d);
  - A1d2 (встройка): раскладка по № 76 а — холодильник и дверь низом на линию пола (1 dp левой колонки при 1,3 и занятом
    `spot_3` — живым снимком); окно — № 77 б, вечер — № 78 б, мебель без тени — № 79 б; плашки копилки и словарика
    остаются (№ 81 а: фон снимается только у 7 мишеней мебели); ночью `furn_bed` под спящим питомцем (№ 91 б; размер —
    макетом: кровать 128 dp, питомец 180 dp); попутные правки — GATE_QUEUE раздел 3 «для сведения»;
  - № 92 б — малый круг `uiprops.py` после A1d2: `ui_jar` светлее, с бликом, чтобы читалась банка, а не ведро.
  ДОКИ: правка GAME_CONCEPT.md:1333 по № 77 не нужна (она — только при а).
