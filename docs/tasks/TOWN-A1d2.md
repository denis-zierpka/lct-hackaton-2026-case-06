```
TASK: TOWN-A1d2 — встройка комнаты A1d1 в UI: пустые оболочки room_port_* и 7 спрайтов furn_* в мишенях RoomScreen без
      плашек; холодильник и дверь низом на линию пола (№ 76 а); окно-спрайт — житель 64 dp на подоконнике, «!» в кружке
      (№ 75 а / 85 б) и достигнутая мечта у забора (№ 77 б, решение 25); банки на доске; светлый ореол подписей на мебели;
      ночью furn_bed под спящим питомцем (№ 91 б); вуаль титула α 0,6; строка «Почтальон принёс конверт» — с портретом Оси
EPIC: TOWN-A1 (docs/tasks/TOWN-A1.md: строка A1d :42, доля A1d :63-64, «ОБЩИЕ ПРАВИЛА ЭПИКА» :68-80); генератор —
      TOWN-A1d1 («что увидит ребёнок» :91-108, «Слияние» :496-512, «A1d2 — встройка (набросок)» :863-883)
BASE: sha коммита ассетов «TOWN-A1d2: ассеты комнаты …» (ДО СПАВНА п. 2; код app/src = 8235e47). Кодер и ревьювер — от
      последнего коммита оркестратора (спека и её правки по замерам не трогают app/src: `git diff --stat BASE HEAD --
      finny-pet/app/src` перед спавном пуст). Номера строк — рабочее дерево bf68648 (код app/src = 8235e47); у номеров в
      `tools/` и `docs/` — якорь-содержимое (номер сдвигается, якорь — нет)
BRANCH: feat/town
ЗАВИСИТ ОТ: выполнено — перенос room.py в feat/town 7571ef7 (с кругом A1d1-2: флажок ящика `m_gold`, `WARM = (0.0,
      -1.0)`), журнал круга — TOWN-A1d1 в b98298b (ДО СПАВНА п. 0 на bf68648: 5, :140 `m_gold`, дифф app/src пуст);
      решения владельца 2026-09-29 «всё по рекомендации» (§18, 0538492): № 76 а, 77 б, 78 б, 79 б, 80 б, 81 а, 91 б, 92 б
      (после A1d2); № 75 а и 85 б (форма «!»), № 25 (в окне — житель, «!» и достигнутые мечты), № 27 (мебель — спрайты в
      мишенях `Target`)
```
Сведено из черновиков «от кода» и «от приёмки» (2026-09-29, r1); r2 — правки по критике (3 критика, 30 находок, по 2
скептика на каждую). Расхождения и решения — журнал спеки в конце.

## КОНТЕКСТ
A1d1 (`pilot/a1d` b536480, PASS 44/0) дал генератор `room.py`: пустую оболочку (стены, пол, гирлянда, часы) днём и вечером
и 7 спрайтов мебели одной камерой, 3 px на dp, размер = мишень. Владелец принял всё по рекомендациям (GATE_QUEUE раздел 3,
листы `finny-pet/screenshots/town/a1d_1…a1d_7.jpg`) и дальше смотрит по снимкам. A1d2 кладёт ассеты в APK и переделывает
экран: мебель — спрайт в своей мишени, лавандовые плашки комнаты и белые плашки 7 мишеней уходят.

### Что решил владелец и что из этого делает A1d2
| № | Ответ | В A1d2 |
|---|---|---|
| 76 | а — холодильник и дверь низом на линию пола, остальное — где сейчас | § 1, § 3: холодильник — отступ сверху (колонка SpaceBetween пересчитывается, сундук уходит под холодильник — и на S23), дверь — `offset` |
| 77 | б — небо с облаком, газон и забор, без переплёта | генератор уже так; житель 64 dp на подоконнике, мечта у забора (§ 4) |
| 78 | б — тёплое пятно у центра пола | только ассет (вечерняя оболочка после круга A1d1-2) |
| 79 | б — без тени | только ассет |
| 80 | б — флажок ящика золотой | только ассет (круг A1d1-2) |
| 81 | а — копилка и словарик на белых плашках | их `Target` не трогать (умолчание White α 0,85) |
| 91 | б — готовый `furn_bed` под спящим питомцем; размер проверить макетом (кровать 128 dp, питомец 180 dp) | § 7; размер — макетом ДО СПАВНА п. 4, живьём — клоном п. 7 |
| 92 | б — `ui_jar` светлее, малый круг `uiprops.py` ПОСЛЕ A1d2 | не здесь; § 5 опирается на поле 5 dp под банкой — строка в спеку круга (ДОКИ) |
| 25 | в окне — житель недели, «!» события и достигнутые мечты | § 4: последняя достигнутая мечта у забора |
| 85 | б — значок «!» 32 dp, заливка 28 dp | окно сразу в этой форме (§ 4); `RoomScreen.kt:115` («В городке») и `StreetScreen.kt:91` — TOWN-J1-1b1-3, не здесь |

Попутно (GATE_QUEUE раздел 3 «для сведения», :142-144): банки на доску (§ 5); «Списку» светлый ореол (§ 2); «Разложи» и
«Список» при 1,3 (зонды `label`, `pix`); на S23 сундук под холодильник (§ 3); вуаль титула α 0,6 и подзаголовок в машинную
проверку (§ 8, `a1d2_title.py`); живой снимок 360 × 640 при 1,3; кроссфейд день → вечер — запись экрана (ACCEPTANCE п. 12).
Строка эпика A1d (:42) — ещё портрет Оси в строке «Почтальон принёс конверт» (судьи A1f S4, S5) — § 9.

### Слои (GameApp.kt, не меняется)
`RoomBackground(…)` (:185, тело :255-263) — `room_port_day` на всё окно `ContentScale.Crop` (под системными полосами), поверх
`AnimatedVisibility` вечера `room_port_evening` (fadeIn/fadeOut 900 мс, `orNone()`) на `Night` и `WeekEnd`; слой места
`Crossfade` (:187) — только `Place`/`Round`; экраны — `AnimatedContent` в `systemBarsPadding()` (:190: вход fadeIn +
scaleIn 0,96 за 320 мс, выход fadeOut + scaleOut 1,02 за 160 мс); `LineHost` (:226). `room_port_day` — фон 12 экранов без
слоя места: замена файла меняет их все (титул — § 8). Портрет закреплён (`game/AndroidManifest.xml:9`), `room_land_*` не
используются — остаются до A1h.

### Карта кода (рабочее дерево, проверено)
`RoomScreen`: `Column(Hud1 56, Hud2 48, TownCard 56, Room weight 1, BottomRow 72)` (:86-93). `Room` (:124-192):
| Строки | Сейчас | После A1d2 |
|---|---|---|
| :128 | `val label = MaterialTheme.typography.labelSmall` — только «Список» :154, «Сон» :175, «Улица» :183 | с ореолом (§ 2) |
| :134-137 | две плашки `G.lavenderLight` / `G.lavender` на 0,6 / 0,4 высоты закрывают фон целиком | удалить (§ 2) |
| :140-143 | окно `Target(…, color = G.sky)`, `ResidentPic` 40 dp `CenterStart`, «!» — `TText` `G.magenta` | § 4 |
| :144, :196-217 | полка — плашка, ряд банок по центру мишени (при 1,0 / 1,3 низ стекла на 38,9 / 35,9 dp — «висят») | § 5 |
| :145-147, :158 | копилка, словарик — плашка | без изменений (№ 81 а) |
| :149 | средняя зона `Box(weight 1, padding(top 4, bottom 4))` | § 3 |
| :151-159 | левая колонка `SpaceBetween`: холодильник (линия :153, «Список» :154), `spot_3` :156, сундук 🧳 :157, словарик :158 | § 3, § 6 |
| :174-176 | кровать: 🛏 + `Spacer` + «Сон» | § 6 |
| :180-185 | правая колонка `SpaceBetween`: дверь `G.goldDark`, точка-ручка :182, «Улица» :183; ящик :185 | § 3, § 6 |
| :232-245 | `Mailbox`: `Target` 48 dp :235, Canvas-конверт с пульсом, «+N» :242 | § 6 (спрайт под конвертом) |
| :115 | «!» плашки «В городке» — `G.magenta` 28 dp | **не здесь** (№ 85 б — 1b1-3) |

`Target` (TownUi.kt:174-179): `Box(modifier.size(w, h).background(color, RoundedCornerShape(10.dp)).clickable(Role.Button)
.clearAndSetSemantics { contentDescription = desc })` — параметр `color` уже есть; умолчание White α 0,85 не меняется
(копилка, словарик, `mock/MockHome.kt`), 7 мишеней мебели передают `color = Color.Transparent`. Картинки внутри мишени узлов
не дают (`clearAndSetSemantics`) — TalkBack прежний. `SpotThing` (:221-228) — пустое место не рисуется; `spot_3` (слот
`table`) пуст, пока ребёнок не поставит вещь.

**Связка ресурсов — без `furnRes`.** `pastryRes`/`residentRes` (TownUi.kt:109-117, :143-154) переводят id из контента в `R.drawable`;
мебель в контенте не живёт — 7 мишеней написаны руками. Прямая ссылка `painterResource(R.drawable.furn_<id>)` в месте
вызова — тот же явный `R.drawable`, который держит R8 (так уже подключены `ui_piggy`, `ui_book`, `ui_jar` в этом файле);
`when` без ключа из данных — лишние строки ради проверки, которую делают lint `UnusedResources` 0 и цикл `aapt2` (ACCEPTANCE
п. 4, 9). Правило эпика TOWN-A1.md:72-73 («… через `when` в `game/`») уточняется в ДОКАХ.

Ночь: `NightScreen` (NightScreens.kt:48-74) — вуаль `purpleDeep` α 0,35 фоном `Box` (:54, содержимое она не темнит),
`Column(verticalScroll, spacedBy(12, CenterVertically), padding 16)`: луна 72 (:59), питомец `…blink` 180 dp «<имя> спит»
(:60), `Panel` с «Проснуться» (:61-67). Титул: вуаль α 0,45 (StartScreens.kt:69), «Питомец» `G.pink` (:72), «Финни» (:73),
подзаголовок `G.pink` bodyLarge (:74), сноска `Color.White.copy(alpha = 0.8f)` (:79); Intro (:113), CreatePet (:172) — α 0,45,
«Как тебя зовут?» `G.pink` headlineSmall (:176). Строка почтальона — `createPet` (GameViewModel.kt:261-262); портрет в
`LineHost` — всегда питомец (TownUi.kt:309); у `Line` (GameViewModel.kt:68) нет поля «кто говорит»; `vm.tc.residents`
содержит `osya` (Ося, «Вам письмо!»); беззвучный портрет жителя — образец TrayScreen.kt:310.

### Замеры (сессия 14; рендеры приёмки A1d1 `$TEMP/s12/a1d/acc_w`; повторить на закоммиченных WebP — ДО СПАВНА п. 2)
- **Линия пола** `room_port_day`: колонки x = 100, 540, 980 — переход стены `[207,179,181]` в пол `[187,144,125]` в строках
  1228–1229 (1228 — сглаженная; плинтус 1194..1227) → **1228 px = 409,3 dp**. На экране (Crop `k = max(W/1080, H/1920)`):
  `F = H/2 + (1228 − 960)·k` px; 360 × 640 — 409,3 dp; S23 и эмулятор 1080 × 2340 / 480 — k = 1,21875, **498,9 dp**.
- **Средняя зона** (статус-бар эмулятора 102 px = 34 dp): 360 × 640 — 286..540 dp (254); S23 — 279..689 (410); эмулятор
  1080 × 2340 — 286..680 (394).
- **Левая колонка при № 76 а.** Холодильник низом на F ⇒ отступ сверху `F − верх зоны − 96`: 360 × 640 — 27,3 dp, S23 —
  123,9. Колонка = отступ + 96 + `spot_3` 36 + сундук 48 + словарик 48 = отступ + 228: на 360 × 640 при занятом `spot_3` —
  255,3 > 254, а `Target` — `Modifier.size` (не `requiredSize`): последний ребёнок `Column` получает остаток и сжимается
  (словарик 48 → 46,7 dp, ТЗ 3.6; ловят `geom` — размер, `ui_measure` п. 10 д — кликабельное < 48 dp, и греп потолка п. 7).
  Отсюда потолок отступа «высота зоны − 228» (§ 3): на 360 × 640 отступ 26 — холодильник на
  1,3 dp выше линии (цена № 76 а, глазом не видно). SpaceBetween с отступом = макет листа `a1d_2a/2b` (`a1d_sheets.py`
  `targets(…, "a")`): на S23 холодильник 402,9..498,9 dp, сундук ниже него, словарик внизу — наложений нет.
- **Дверь** низом на F: `offset(y = F − верх зоны − 136)`: 360 × 640 — −12,7 dp (дверь 273..409, копилка кончается на 262 —
  зазор 11 dp), S23 — +83,9. Отрицательный сдвиг — только `offset`; ящик внизу колонки не сдвигается.
- **Окно** `furn_window` 336 × 240: строки 196..212 — внутренняя полоса под стеклом (ширина 278 px), 213..230 — доска
  подоконника (ширина 316 px). Верх доски — **71 dp**, как `SILL = 71` листа `a1d_3_window` (`a1d_sheets.py:31`), который
  видел владелец. Жители 512: низ bbox 490–492 (Боря 736 из 768) → в кадре 64 dp ступни на 61,5 dp ⇒ верх кадра на
  **10 dp**; видимая фигура по x — до 49,4 dp кадра (Лиза) → с отступом 8 — до 57,4 dp окна. Строки окна (профиль колонок):
  стекло (небо) 26,7..85,7 dp по x; забор 50..62 dp, газон 62,7..65, низ стекла 65,3, светлая полоса 65,3..70,7, доска с
  71 dp. **Мечта у забора** (№ 77 б, решение 25 «вещь появляется в мире», GAME_CONCEPT :593) — `goal_*` 24 dp, кадр `start
  60 / top 42`: bbox scooter 63,7..80,3 dp, lego/zoo/paints 61,3..82,7 dp по x, верх ≥ 43,3, низ 61,0..64,3 dp — на газоне
  за стеклом (макет критика `scratchpad/sk1/fix_b.png`); правее жителя (≤ 57,4), левее края стекла 85,7, ниже «!» (76..108 ×
  4..36) без наложения. 32 dp в правую треть не влезает: с низом на газоне lego/zoo заходят на штору или на жителя; кадр
  r1 `58 / 43` 32 dp ставил мечту на доску подоконника (низ 68–73 dp) рядом с жителем — место варианта а № 77 (TOWN-A1d1.md:814-816).
- **Полка** `furn_shelf` 456 × 216: доска — строки 126..142 = **42,0..47,3 dp**. `ui_jar` 512: α > 127 — строки 100..429 →
  под стеклом в кадре 32 dp пусто 5,1 dp. Ряд банок `offset(y = 12.dp)` → низ стекла 12 + 36 − 5,1 = **42,9 dp** (на доске);
  числа / «Разложи» 48..68 dp (1,0), 48..74 (1,3) — под доской, на стене; при 1,3 строка выходит за мишень на 2 dp без
  обрезки (`offset` не отнимает места у `Column`; с `padding(top = 12.dp)` 36 + 26 dp не влезли бы в 60).
- **Кровать** `furn_bed` 384 × 192: верх одеяла в колонках x = 60..330 px — строка 51–52 = **17 dp**; ножки до строки 186.
  Питомец `…_blink` 180 dp (27 кадров): линия лап 439 из 512 = 154,3 dp (под лапами пусто 26 dp), видимая ширина 65 (зайка)
  … 91–93 (кот) dp < кровати 118 dp видимых. Лапы на одеяле ⇒ низ рамки питомца на 64 − 17 − 26 = **21 dp** выше низа
  кровати. Ночь BASE 360 × 640 (`emu_b4r_night`): лапы ≈ 294 dp, панель 336..590; с кроватью столбец выше на 21 dp.
  Кровать ночью (§ 7, вариант а) — над панелью: 116..244 × ≈ 270..335 dp при линии пола F 409,3, то есть на ≈ 75 dp выше
  пола (без демо-кнопки панель короче — ≈ 37 dp), и перед настенными часами вечерней оболочки (золотой обод x 111,7..148,7 ×
  y 309,3..346,7 dp) — ≈ 38 % рамки часов; вариант б висит так же. «Кровать на F» на 360 × 640 в демо не помещается (409 + 12 +
  254 > 616 dp) — это не критерий приёмки, а факт для ворот (№ 98, ВОПРОСЫ НА ВОРОТА). Пятно № 78 б ночью — под панелью.
- **Холодильник** в зоне «Списка»: левая кромка — тёмный кант (яркость 0,164), середина 0,30–0,38; «Список» при 1,3 шире
  64 dp (`wrapContentWidth(unbounded = true)`, :154) и ложится краями на кант — находка «доступности» A1d1.
- **Титул** (модель `оболочка·(1 − α) + #310F53·α`, рамки по `emu_b4r_title.png`; два независимых замера): подзаголовок —
  старая оболочка α 0,45: 4,90 (BASE); новая α 0,45: 3,57–3,75 (FAIL); новая α 0,6: 5,06–5,24. «Питомец» (крупный, 3 : 1):
  BASE 2,43 (FAIL уже сейчас — за ним было окно), новая α 0,6 — 4,94. Сноска (16 sp, белый α 0,8): 2,8–3,0 уже на BASE,
  α 0,6 и белый без α — ≥ 5,2 → § 8 делает её белой (давний дефект ТЗ 3.6, та же проверка).
- **«!» при 1,3:** строка titleMedium 18 sp × 1,3 — 31,2 dp в круге 28 dp: `TText` отметил бы «Обрезано: !» (TownUi.kt:80)
  — отсюда `wrapContentSize(unbounded = true)` (§ 4). Тот же «!» 28 dp в «В городке» (:115) при 1,3 может резаться уже на
  BASE — давнее, 1b1-3.
- **Облачко питомца** (`PetBubble`, RoomScreen.kt:249-271, не меняется; появляется по касанию питомца, закрывается тапом и
  при навигации — GameViewModel.kt:176, :187, :327): тело — непрозрачный белый ряд 8..352 dp по x, низ = низ зоны − 176 −
  12: 360 × 780 — ≈ 428–454..492 dp, S23 — ..501. После № 76 а оно на 360 × 780 и S23 закрывает низ холодильника и двери
  вместе со «Список» и «Улица» (на BASE там холодильник был выше облачка); на 360 × 640 подписи открыты (тело ≈ 288–314..352,
  холодильник 312..408), закрыто меньше, чем на BASE. Временное перекрытие облачком — допуск эпика (TOWN-A1d1.md:120-122);
  в приёмку не входит, факт — на ворота «для сведения».
- **Первый кадр входа** (§ 3): `fridgeTop`/`doorY` пишутся в `onGloballyPositioned` и попадают в композицию кадром позже;
  без анимаций вход — `EnterTransition.None` (GameApp.kt:194). Скачок, если кадр виден отдельно: 360 × 640 — 26 / 12,7 dp,
  S23 — 123,9 / 83,9 dp (холодильник / дверь). См. § 3 «Первый кадр».
- **Ассеты** (acc_w, до круга A1d1-2): 7 `furn_*` = 34 156 Б; оболочки 33 916 + 37 818 = 71 734 против нынешних 80 316
  (−8 582). Release 8235e47 — 4 416 824 Б; остаток бюджета эпика 713 245 Б; доля A1d ≤ 153 638 Б (TOWN-A1.md:63-64).

## ЧТО УВИДИТ РЕБЁНОК (TOWN-A1d1.md:91-108 с ответами № 76–91)
| Момент | Что на экране | Что понимает ребёнок |
|---|---|---|
| открыл игру днём | стены, пол, гирлянда, часы; плашек нет — мебель стоит на своих местах; холодильник и дверь **стоят на полу**; копилка и словарик — на белых плашках (№ 81 а) | «это дом питомца» |
| ищет еду и список | высокий светлый холодильник на полу, «Список» внизу со светлым ореолом — читается и при крупном шрифте | узнаёт по форме |
| смотрит на план | доска на кронштейнах, банки с крышками **стоят на доске**, числа (или «Разложи») под ней | «деньги по банкам» |
| смотрит в окно | небо, облако, газон с забором; житель недели 64 dp стоит на подоконнике; «!» — красный кружок с белой обводкой; достигнутая мечта — у забора справа | «на улице меня ждут» |
| хочет выйти | светлая дверь в полный рост на полу, «Улица» внизу | «дверь — на улицу» |
| пришёл конверт | ящик на столбике (флажок золотой), конверт «+N» пульсирует поверх (только с анимацией) | «пришли карманные» |
| сундук | деревянный сундук ниже холодильника (и на S23) | «тут мои вещи» |
| пора спать | кровать боком, «Сон» на одеяле; ночью питомец **спит на той же кровати** над панелью ночи | «лёг спать в кровать» |
| вечер / ночь | мебель уходит вместе с экраном (160 мс), комната темнеет (900 мс); тёплое пятно у центра пола, без тени мебели | спокойно, не страшно |
| титул | та же пустая комната под более плотной вуалью; подзаголовок и сноска читаются | — |
| первый день | строка «Почтальон принёс конверт…» с портретом Оси, а не питомца | «конверт принёс Ося» |

Цвет — не единственный признак: 7 предметов различаются силуэтом (A1d1 К4, «ребёнок» — 7 из 7 и в сером); «!» — форма
(кружок со знаком) + «есть событие» в TalkBack.

## ТРЕБОВАНИЕ ТЗ (docs/sources/ТЗ_текст.txt)
3.6 (:386-400): «рекомендуемый минимум — 48 × 48 dp» — размеры 9 мишеней не меняются, сжатие словарика исключено потолком
§ 3; «читаемость сохраняется при системном увеличении шрифта» — «Список», числа банок, «Разложи», «!» при 1,3 (§ 2, § 4,
§ 5); «Цвет не является единственным способом…» — силуэты мебели, форма «!»; контраст текста — ореол подписей (§ 2), вуаль и
сноска титула (§ 8); «Звуки и анимации можно отключить» — пульс конверта (`LocalAnimate`) и кроссфейд (`orNone()`) как были.
3.5 (:375): ночью питомец спит в кровати (№ 91 б), тёплое пятно — не страшно. 3.3 (:346) и раздел 5 п. 12 — право на
изображения: строка LICENSES в коммите ассетов. 2.5.3 — всё на главном экране видно одновременно (мебель не уезжает за край
и не наезжает — зонд `geom`).

## CONTRACT
Новых строк ребёнку — 0; новых описаний TalkBack — одно (хвост мечты у окна, § 4); новых литералов в диффе — ровно два
(ACCEPTANCE п. 8). Домен, `main/`, `content.json`, тесты, ресурсы (они в BASE), `GameApp.kt`, `Target`, `Pic`,
`ResidentPic`, `GoalPic`, размеры 9 мишеней, `SpotThing`, `PetBubble`, `BottomRow`, `TownCard` (:103-121), `mock/*` — не
меняются. Ниже — дословно, с комментариями; пробелы — на выбор кодера (`git diff -w`); строки § 1–9 — каждая одной
строкой, как в спеке, перенос запрещён (п. 6 считает строки, п. 7 грепает построчно); числа, имена и порядок модификаторов —
как написано.

### § 1. Линия пола — RoomScreen.kt, перед KDoc `Room` (:123)
```kotlin
/** Floor line of room_port_day (room.py, 1080 × 1920 px): the back wall meets the floor at y = 1228 (TOWN-A1d2, № 76 а). */
private const val FLOOR_PX = 1228f
```
Число — из замера ДО СПАВНА п. 2 на закоммиченной оболочке; другое — оно же в зонде `a1d2_geom.py` (`FLOOR`).

### § 2. Плашки уходят, подписи на мебели — с ореолом (RoomScreen.kt)
- :134-137 (`Column` с двумя плашками `G.lavenderLight` / `G.lavender`) — удалить целиком; фон — `room_port_day` из
  `GameApp.RoomBackground`, взамен ничего не рисовать.
- :128 →
  ```kotlin
      // a light halo: labels lie right on the sprites («Список» on the fridge's dark rim, A1d1 judges)
      val label = MaterialTheme.typography.labelSmall.copy(shadow = Shadow(Color.White, blurRadius = 8f))
  ```
  Касается «Список», «Сон», «Улица» (других пользователей `label` нет). `blurRadius` — число ДО СПАВНА п. 7 (клон): 8, если
  `a1d2_label.py` на клоне зелёный; иначе оркестратор до спавна заменяет ореол обводкой (второй `TText` «Список» белым
  `drawStyle = Stroke(3.dp)` под основным) и пишет в журнал — кодер получает итоговый §.

### § 3. Линия пола (№ 76 а) — Room: состояния и средняя зона
После `val mail = …` (:132):
```kotlin
    val dream = s.achievedGoals.lastOrNull()
    val density = LocalDensity.current
    var fridgeTop by remember { mutableStateOf(0.dp) } // № 76 а: set from the floor line below; 0 — the first frame
    var doorY by remember { mutableStateOf(0.dp) }
```
Строка :149 `Box(Modifier.fillMaxWidth().weight(1f).padding(top = 4.dp, bottom = 4.dp)) {` →
```kotlin
            Box(Modifier.fillMaxWidth().weight(1f).padding(top = 4.dp, bottom = 4.dp).onGloballyPositioned { c ->
                // № 76 а: the fridge and the door stand on the floor line of room_port_day, drawn Crop over the whole window (GameApp.RoomBackground)
                val root = c.findRootCoordinates().size
                val k = maxOf(root.width / 1080f, root.height / 1920f)
                val floor = with(density) { (root.height / 2f + (FLOOR_PX - 960f) * k - c.positionInRoot().y).toDp() }
                // not below «zone − 228» (fridge 96 + spot_3 36 + chest 48 + book 48): else the book shrinks under 48 dp
                fridgeTop = (floor - 96.dp).coerceAtMost(with(density) { c.size.height.toDp() } - 228.dp).coerceAtLeast(0.dp)
                doorY = floor - 136.dp
            }) {
```
- Холодильник — `Modifier.padding(top = fridgeTop)` (§ 6): отступ раздвигает колонку, `spot_3`, сундук и словарик по
  `SpaceBetween` уходят ниже — «сундук под холодильник» на S23 без отдельной правки. Дверь — `Modifier.offset { IntOffset(0,
  doorY.roundToPx()) }` (§ 6; лямбда: `doorY` — State, `offset(y = …)` с ним даёт lint `UseOfNonLambdaOffsetOverload` —
  6-е предупреждение game при пороге 5, проверено на копии), может быть отрицательным; ящик не сдвигается. Средняя
  колонка, питомец, места — без изменений.
- `coerceAtMost`/`coerceAtLeast`/`maxOf` — из `kotlin.*` (`Dp` — `Comparable`, как `coerceAtLeast` на :161); импорты — § 10.
  Не `coerceIn`: при зоне ниже 228 dp границы перевёрнуты — `IllegalArgumentException`.
- **Первый кадр входа** — прежние места (состояние пишется при раскладке кадра N, в композицию попадает в N + 1). С
  анимациями — под fadeIn входа. Без анимаций (родительский тумблер или системное «убрать анимации», GameApp.kt:134, :194)
  кадр N — тот, в котором `AnimatedContent` с None-переходами и так рисует оба экрана (TOWN-A1c F2, «вне задачи»,
  TOWN-A1c.md:302, :309-310); по разбору фаз Compose обе поправки приходят в N + 1 вместе с уходом старого экрана — лишнего
  кадра A1d2 не добавляет (живьём не проверено). Цена, если кадр всё же виден отдельно, — один кадр скачка (Замеры «Первый
  кадр входа»). Принято; формулой вместо `onGloballyPositioned` не заменять (WORKFLOW № 38); на ворота — № 99.
- `positionInRoot` видит `scaleIn` 0,96 входа `AnimatedContent`; конечное значение обязано совпасть с покоем — это меряет
  ACCEPTANCE п. 10 а (кадры после ≥ 1,5 с, анимации вкл. и выкл.). Если на клоне (ДО СПАВНА п. 7) с анимациями отличие
  > 2 dp, а без них нет — оркестратор до спавна меняет § 3 на координаты относительно корня `RoomScreen` (`localPositionOf`)
  + `WindowInsets.systemBars.getTop(density)`; кодер сам — не меняет.

### § 4. Окно (№ 77 б, № 75 а / 85 б, решение 25) — RoomScreen.kt:140-143
```kotlin
                Target("Окно: улица" + (resident?.let { ", машет ${it.name}" } ?: "") + (if (streetEvent) ", есть событие" else "") + (dream?.let { ", у забора мечта «${it.title}»" } ?: ""), 112.dp, 80.dp, color = Color.Transparent, onClick = { vm.navigate(Screen.Street) }) {
                    Image(painterResource(R.drawable.furn_window), null, Modifier.fillMaxSize())
                    // feet on the sill (71 dp of furn_window): 492 of the 512 frame = 61,5 dp down the 64 dp resident (TOWN-A1f)
                    if (resident != null) ResidentPic(resident, 64.dp, Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 10.dp))
                    // decision 25: the last reached dream on the grass by the fence behind the glass (fence 50..62, grass 62,7..65 dp), right of the resident
                    dream?.let { GoalPic(it, 24.dp, Modifier.align(Alignment.TopStart).padding(start = 60.dp, top = 42.dp)) }
                    // № 75 а, № 85 б: #E0004A 28 dp in a white 2 dp ring, 32 dp; the «!» line (31 dp at 1.3) is not clipped by the circle
                    if (streetEvent) Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp).background(Color.White, CircleShape).padding(2.dp).background(Color(0xFFE0004A), CircleShape), contentAlignment = Alignment.Center) {
                        TText("!", style = MaterialTheme.typography.titleMedium, modifier = Modifier.wrapContentSize(unbounded = true), color = Color.White, maxLines = 1)
                    }
                }
```
Белый «!» на `#E0004A` — 4,92 : 1, кольцо к небу ≈ 2,1 (3 : 1 держится на кольце, GATE_QUEUE раздел 3). Мечта — одна,
последняя достигнутая (`GameState.achievedGoals`, GameState.kt:24; их бывает несколько — сужение решения 25, строка
BACKLOG и ворот, ДОКИ); 24 dp `60 / 42` — низ на газоне за стеклом, у забора (Замеры «Окно»); картинка — `GoalPic` (свой
спрайт, вещь для `item:x`, `goal_custom`, иначе emoji). Хвост описания «, у забора мечта «…»» — единственная новая фраза
TalkBack задачи.

### § 5. Полка — банки на доске — RoomScreen.kt:201-202
```kotlin
    Target(desc, 152.dp, 72.dp, color = Color.Transparent, onClick = { vm.navigate(Screen.Jars) }) {
        Image(painterResource(R.drawable.furn_shelf), null, Modifier.fillMaxSize())
        // jars stand on the board (42 dp of furn_shelf; ui_jar's glass ends 5 dp above its 36 dp box); offset, not padding: 36 + 26 dp at 1.3 would not fit
        Column(Modifier.align(Alignment.TopCenter).offset(y = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
```
Остальное тело `Shelf` (крышки, числа, «Разложи», описание) — без изменений.

### § 6. Холодильник, сундук, кровать, дверь, ящик — RoomScreen.kt
- :152-153 (холодильник; линия `G.lavender` :153 — удалить, «Список» :154 — без изменений):
  ```kotlin
                        Target("Холодильник: список нужного и цены", 64.dp, 96.dp, Modifier.padding(top = fridgeTop), color = Color.Transparent, onClick = { onPanel("fridge") }) {
                            Image(painterResource(R.drawable.furn_fridge), null, Modifier.fillMaxSize())
  ```
- :157 (сундук): `Target("Сундук: обустроить комнату", 64.dp, 48.dp, color = Color.Transparent, onClick = { vm.navigate(Screen.Arrange) }) { Image(painterResource(R.drawable.furn_chest), null, Modifier.fillMaxSize()) }`
- :145-147 (копилка), :158 (словарик) — **не трогать** (№ 81 а).
- :174-175 (кровать; «Сон» — по центру мишени, на светлом одеяле):
  ```kotlin
                            Target("Кровать: сон", 128.dp, 64.dp, Modifier.align(Alignment.Center), color = Color.Transparent, onClick = onBed) {
                                Image(painterResource(R.drawable.furn_bed), null, Modifier.fillMaxSize())
                                TText("Сон", style = label, maxLines = 1)
  ```
- :181-182 (дверь; точку-ручку :182 — удалить, ручка есть на спрайте; «Улица» :183 — без изменений):
  ```kotlin
                        // lambda offset: doorY is State (lint UseOfNonLambdaOffsetOverload)
                        Target("Дверь: на улицу", 64.dp, 136.dp, Modifier.offset { IntOffset(0, doorY.roundToPx()) }, color = Color.Transparent, onClick = { vm.navigate(Screen.Street) }) {
                            Image(painterResource(R.drawable.furn_door), null, Modifier.fillMaxSize())
  ```
- :235 (ящик): `Target("Почтовый ящик: плюс $mail придёт с новым конвертом", 48.dp, 48.dp, modifier, color = Color.Transparent, onClick = onClick) {`
  и первой строкой тела `Image(painterResource(R.drawable.furn_mailbox), null, Modifier.fillMaxSize())`; `Column` с
  конвертом-Canvas, пульсом и «+N» (:236-243) — без изменений.

### § 7. Ночь: кровать под спящим питомцем (№ 91 б) — NightScreens.kt:60
Строку :60 заменить на
```kotlin
            // № 91 б: the pet sleeps on furn_bed, paws on the blanket: 64 − 17 (blanket top) − 26 (empty under the paws of the 180 dp frame) = 21 dp
            Box(contentAlignment = Alignment.BottomCenter) {
                Image(painterResource(R.drawable.furn_bed), null, Modifier.size(128.dp, 64.dp))
                Image(painterResource(PetSprites.id(pet.speciesId, pet.colorId, vm.economy.stageIndex(pet.growth), "blink")), "${pet.name} спит", Modifier.padding(bottom = 21.dp).size(180.dp))
            }
```
- Кровать без голоса, питомец — прежний узел «<имя> спит». Вуаль экрана — фон `Box` (:54), содержимое она не темнит:
  кровать, как и питомец, — без тона. Тёплое пятно № 78 б ночью — под панелью ночи (GATE_QUEUE:100-101).
- Кровать стоит над панелью ночи, не на линии пола, и заходит на часы (Замеры «Кровать») — № 91 б буквально («под спящим
  питомцем, только Kotlin»); на ворота — № 98 (ВОПРОСЫ НА ВОРОТА), не блокер.
- 128 × 64 — 1:1 спрайту (3 px/dp). Столбец ночи растёт на 21 dp (центрирован — всё сдвигается на ≈ 10,5 dp).
- Размер, посадка и тон — **по макету** (ДО СПАВНА п. 4, «размер проверить макетом» № 91 б) и клону (п. 7); в CONTRACT до
  спавна — одно число каждого: а) 128 × 64, `padding(bottom = 21.dp)` (по умолчанию); б) 160 × 80, `padding(bottom = 33.dp)`
  (80 − 21,25 − 26); если «Проснуться» при 1,3 уходит за экран (а на BASE — нет) — `spacedBy(12.dp …)` :57 → `8.dp`; если
  судья «сцена» скажет «кровать светится на тёмной комнате» — `colorFilter = ColorFilter.tint(G.purpleDeep.copy(alpha =
  0.35f), BlendMode.SrcAtop)` у кровати (+2 импорта — БЮДЖЕТ и п. 6 ≤ 11) и та же модель в `a1d2_pix.py`. Кодер не выбирает.

### § 8. Титул — StartScreens.kt (только `TitleScreen`)
- :69 `G.purpleDeep.copy(alpha = 0.45f)` → `G.purpleDeep.copy(alpha = 0.6f)`, строкой выше
  `// A1d2: the empty day shell is lighter — the pink subtitle 3,6 → 5,1 : 1 under 0.6 (GATE_QUEUE, A1d1 judges)`;
- :79 `color = Color.White.copy(alpha = 0.8f)` → `color = Color.White` (сноска 2,8–3,0 → ≥ 5,2 : 1; давний дефект).
- Intro (:113) и CreatePet (:172) — α 0,45; если ДО СПАВНА п. 6 покажет «Как тебя зовут?» (:176) < 3 : 1 — :172 тоже 0,6
  (оркестратор до спавна, в журнал).

### § 9. Строка почтальона с портретом Оси (эпик :42, судьи A1f S4, S5)
- GameViewModel.kt:68 → `class Line(val text: String, val why: List<String> = emptyList(), val speaker: String? = null)`;
  :262 → `if (lines.none { it.text == envelopeLine }) lines.add(0, Line(envelopeLine, speaker = "osya"))`.
- TownUi.kt `LineHost`: после `val pet = vm.state.pet` (:304) — `val who = line.speaker?.let { id -> vm.tc.residents.firstOrNull { it.id == id } }`;
  :309 → `if (who != null) ResidentPic(who, 48.dp, Modifier.clearAndSetSemantics {}) else if (pet != null) Image(…)` (ветка
  питомца — прежняя). Портрет без голоса, как у питомца (образец TrayScreen.kt:310). Прочие строки — прежний портрет.

### § 10. Импорты
Точечно, без `*` (WORKFLOW № 45). RoomScreen.kt — 9: `androidx.compose.foundation.layout.offset` (и полка § 5, и
лямбда-перегрузка двери § 6), `androidx.compose.foundation.layout.wrapContentSize`, `androidx.compose.ui.graphics.Shadow`,
`androidx.compose.ui.layout.findRootCoordinates`, `androidx.compose.ui.layout.onGloballyPositioned`,
`androidx.compose.ui.layout.positionInRoot`, `androidx.compose.ui.platform.LocalDensity`, `androidx.compose.ui.unit.IntOffset`,
`ru.finny.pet.game.ui.GoalPic` (`roundToPx` — из получателя `Density` лямбды, без импорта).
NightScreens, StartScreens, TownUi, GameViewModel — 0. Удалённых — 0 (`G`, `Spacer`, `width` остаются в деле).

### § 11. Явно НЕ определено (кодер спрашивает, а не решает)
Числа § 1, § 3 (96, 136, 228, 1080, 1920, 960), § 4 (64, 8, 10, 24, 60, 42, 4, 32, 2), § 5 (12), § 2 (`blurRadius`), § 7
(128 × 64, 21, 12), § 8 (0,6); форма и цвет «!»; какая мечта и сколько; `furnRes`/`when`; правка `Target`, `Pic`,
`ResidentPic`, `GoalPic`, `TownCard`, `GameApp`; подложки, тени, обводки спрайтов; контуры пустых мест (стоп-лист № 17);
мебель ночью, кроме кровати; порядок слоёв; вуали Intro/CreatePet; портреты других строк; новые строки и описания сверх § 4;
`testTag`. Нужное вне § 1–10 — `STATUS: BLOCKED` с одним вопросом.

## SCOPE
variant: game (только UI)
allow: finny-pet/app/src/game/java/ru/finny/pet/game/screens/RoomScreen.kt,
  finny-pet/app/src/game/java/ru/finny/pet/game/screens/NightScreens.kt (только § 7),
  finny-pet/app/src/game/java/ru/finny/pet/game/screens/StartScreens.kt (только § 8),
  finny-pet/app/src/game/java/ru/finny/pet/game/ui/TownUi.kt (только `LineHost`, § 9),
  finny-pet/app/src/game/java/ru/finny/pet/game/GameViewModel.kt (только :68 и :262, § 9)
protect: `.claude/task-scope.json` 1b4 (base 3a0d9ec, 82 пути), но: RoomScreen.kt, NightScreens.kt, StartScreens.kt — из
  protect в allow; TrayScreen.kt — из allow в protect. В protect поимённо остаются все прочие файлы `game/` (`java/`,
  `AndroidManifest.xml`, `res/`, `mock/`, `audio/`), `main/`, `classic/`, `test/`, gradle, `gradlew.bat`, `finny-pet/tools/…`,
  корневые `tools/*` поимённо (сверить с `ls tools/` — 14 файлов), `docs/`, `finny-pet/docs/`, `wt/`. task-scope пишется
  после последнего коммита оркестратора (ДО СПАВНА п. 8); пересечение allow × protect — пусто (WORKFLOW № 23).

## ANTI-SCOPE
- Ассеты и генератор: `room.py`, WebP, LICENSES, `to_webp.py` — оркестратор (ДО СПАВНА); `room_land_*`, альбом — A1h.
- `GameApp.kt` (`RoomBackground`, кроссфейд 900 мс — только запись), `Target`, `ResidentPic`, `GoalPic`, `Pic`.
- № 85 б — «!» плашки «В городке» (:115) и карточки улицы (`StreetScreen.kt:91`) — TOWN-J1-1b1-3 (тем же файлом, после A1d2).
- № 92 б — банка светлее (`uiprops.py`) — малый круг после A1d2; он сохраняет поле ≈ 5 dp под стеклом `ui_jar` или
  пересчитывает 12 dp § 5 и прогоняет `a1d2_pix.py` (полка).
- Тени мебели (№ 79 б), вечерние спрайты, мебель и окно ночью (кроме кровати), стол для `spot_3`, 2–3 спрайта на титул
  (№ 80, «если нужен уют») — нет. Вещи мест и стартовые вещи камерой комнаты — A1e; эмодзи 🪴 / 🛋️ на местах остаются.
- Портрет говорящего в других строках (интро событий «Почтальон Ося: …», строка домена «Новый конверт…» `Town.wake`) —
  строка BACKLOG. Несколько мечт в окне — только последняя (сужение решения 25: строка BACKLOG и ворот, ДОКИ).
- Зонды задачи — в scratch оркестратора (тексты — «Зонды приёмки»); правки `tools/` — только `art_check.py bg` (ДО СПАВНА
  п. 1); новых скриптов в `tools/`, зависимостей, правок `content.json` и домена нет.

## БЮДЖЕТ
Без строк import: вставок ≤ 65, удалений ≤ 32 (ожидаю ≈ 53 / ≈ 25: RoomScreen ≈ 41 / 19 — константа 2, ореол 2 / 1,
состояния 4, зона 8 / 1, окно 10 / 3, полка 4 / 2, холодильник 2 / 2, сундук 1 / 1, кровать 3 / 2, дверь 3 / 2, ящик 2 / 1,
плашки 0 / 4; NightScreens 5 / 1; StartScreens 3 / 2; TownUi 2 / 1; GameViewModel 2 / 2); импортов +9 (с тоном кровати § 7 —
+11), удалённых 0; новых файлов 0; зависимостей 0. Счёт — из finny-pet/, как ACCEPTANCE 1–8: `git diff -w BASE -- app/src |
grep '^+[^+]' | grep -vc '^+import '` и то же с `'^-[^-]'` / `'^-import '` (путь `finny-pet/app/src` из finny-pet/ даёт
пустой дифф и 0 при любом объёме — контроль непустоты в ДО СПАВНА п. 3). APK: прирост release к `$T/base_release.apk` ≤
Σ 7 `furn_*` + 6 000 Б; к release 8235e47 (4 416 824 Б) — ≤ доли A1d 153 638 Б; в отчёт и TOWN-A1.md — прирост, факт доли, остаток бюджета эпика (4 081 493 +
1 048 576 − размер APK).

## ORACLE
Не нужен: всё — `game/` (Compose, `R.drawable`, `Line` ViewModel варианта); `src/test` общий для обоих вариантов и
`ru.finny.pet.game` / `R.drawable` не видит (`grep -rln 'ru.finny.pet.game\|R\.drawable' finny-pet/app/src/test` → пусто,
наборов `src/testGame` нет); правило эпика TOWN-A1.md:75 («test-author не нужен»). Эффект держат: lint `UnusedResources`
0 и цикл `aapt2` (7 `furn_*` в release), грепы формы (п. 7), зонды раскладки, пикселей (все 7 спрайтов в своих мишенях,
банки, житель, «!», мечта, ночная кровать, портрет Оси), контраста и узлов TalkBack с отрицательным контролем на
BASE и положительным на клоне с выключателями (ДО СПАВНА п. 6–7), запись кроссфейда, судьи. Тестов — 600, как на BASE.

## ДО СПАВНА (оркестратор)
`T="$(cygpath -m "<scratchpad сессии>")/a1d2"; mkdir -p "$T"` — в начале КАЖДОГО вызова с `$T`; `$BL` —
`C:/Program Files/Blender Foundation/Blender 5.2/blender.exe`. Правки основного дерева — когда в нём нет кодера.
0. **Условия** — выполнены (2026-09-29, сессия 14, bf68648; повторить перед п. 1, в журнал): `room.py` на feat/town
   коммитом 7571ef7 «A1d1: генератор комнаты room.py из pilot/a1d (…круг A1d1-2 флажок золотой + WARM…)» («Слияние»
   TOWN-A1d1.md:496-512: 7 прогонов `room_furn_*` в `art_check.py regress`), журнал круга A1d1-2 — b98298b;
   `grep -cE '^(PX = 3|SHADOW = False|MULLION = False|WINDOW_VIEW = "fence"|WARM = \(0\.0, -1\.0\))' finny-pet/tools/art/room.py`
   → 5; `grep -n 'mail_flag' finny-pet/tools/art/room.py` → :140 с `p["m_gold"]`, без `m_mag`; `git diff --stat 8235e47
   HEAD -- finny-pet/app/src` и `git status --porcelain -uall -- finny-pet/app` → пусто.
1. **Спека и эталон маски `bg`** (коммит «docs+tools(TOWN-A1d2): …» ДО замены оболочки): маска UI `art_check.py bg`
   (tools/art_check.py:203 `room = np.asarray(…room_port_day.webp…)`, в `def bg` :197) сравнивает снимки `emu_b_*` (сняты на
   старой комнате) с `res/room_port_day.webp` — после замены `bg - - --selfcheck` покраснеет. Эталон: `git show
   HEAD:finny-pet/app/src/game/res/drawable-nodpi/room_port_day.webp > finny-pet/screenshots/emu_b_room_port_day.webp`
   (гитигнор `emu_*`, рядом со снимками `emu_b_*`); в `bg` маска — по нему (нет файла — exit 1 со строкой, что делать;
   докстринг — :11-16, строки `bg BG OUT_DIR` и `bg - - --selfcheck`). `which` и `backdrops()` читают res — так и надо.
   Проверка: `python tools/art_check.py bg - - --selfcheck` → `SELFCHECK OK` до и после п. 2. Отрицательный контроль (в
   журнал) — после п. 2 эталон временно = новая оболочка: `cp finny-pet/app/src/game/res/drawable-nodpi/room_port_day.webp
   finny-pet/screenshots/emu_b_room_port_day.webp` → `bg - - --selfcheck` → `SELFCHECK FAIL` и 6 строк «сырой маской 0»;
   вернуть `git show <коммит п. 1>:finny-pet/app/src/game/res/drawable-nodpi/room_port_day.webp >
   finny-pet/screenshots/emu_b_room_port_day.webp` → `SELFCHECK OK`. Ветка «нет файла» — отдельно: `mv` эталона → exit 1 и
   строка-подсказка; файл вернуть.
2. **Коммит ассетов = BASE** («TOWN-A1d2: ассеты комнаты — оболочки портрета (пустые, № 78 б) и 7 furn_*»; файлы поимённо):
   - рендер финальными параметрами (WORKFLOW № 47; очередь HIP — ≤ 1 `blender.exe`): `"$BL" -b --factory-startup
     --python-exit-code 1 -P finny-pet/tools/art/room.py -- --only room_port_day --out $T/a/room_port_day.png`, то же
     `room_port_evening`, затем `-- --sprites $T/a/spr` (свой каталог); зонды A1d1 (из корня):
     дампы — regress-дампы room.py (сборная база `$TEMP/s12/refbase`, = regress п. 0) под именами, которые ищет зонд
     (`DUMP_DIR/furn_<id>.json`, regress пишет `room_furn_<id>.json`): `mkdir -p $T/a/sd; for n in window shelf fridge door
     bed mailbox chest; do cp $TEMP/s12/refbase/room_furn_$n.json $T/a/sd/furn_$n.json; done; python
     $TEMP/s12/a1d/sprite_check.py $T/a/spr $T/a/sd $TEMP/s12/refbase/room_port_day.json` → `SPRITES OK` (запасной путь —
     дампы рендером с `DUMP_OUT`, как A1d1 `$TEMP/s12/a1d/acc_r2.sh:100-101`); `python $TEMP/s12/a1d/zones.py . $T/a/spr
     $T/a/room_port_day.png` → `LABEL ZONES OK`; `python tools/art_check.py bbox $T/a/spr/*.png` → 7 × OK;
   - `python finny-pet/tools/art/to_webp.py $T/a/room_port_day.png $T/a/room_port_evening.png --rgb --dst
     finny-pet/app/src/game/res/drawable-nodpi` → 2 × RGB; `… to_webp.py $T/a/spr --dst …` → 7 × RGBA (каталог без оболочек,
     иначе они перегонятся RGBA поверх RGB); `room_land_*` не трогать;
   - потолки: `stat -c '%s %n' …/drawable-nodpi/{room_port_*,furn_*}.webp` — оболочка ≤ 61 440, спрайт ≤ 37 888, Σ 9 ≤
     227 954 (TOWN-A1d1.md:268); `ls …/drawable-nodpi | wc -l` → 65;
   - LICENSES.md:51 → «Комната: портрет день/вечер — пустая оболочка (TOWN-A1d1–A1d2), альбом — прежний рендер (A1h);
     мебель — 7 спрайтов | 4 WebP `room_*`, 7 WebP `furn_*` в `app/src/game/res/drawable-nodpi/` | `tools/art/room.py`
     (`--only room_port_*`, `--sprites`) → `tools/art/to_webp.py` (оболочки `--rgb`)», :59 «58 файлов» → «65 файлов»;
     ARCHITECTURE.md:28 «(58 WebP)» → «(65 WebP)», «комната» → «комната и мебель»; :185 и BUILD_AND_DEMO.md:305 — `--sprites`,
     `furn_*` (если коммит слияния A1d1 этого не сделал); TOWN-A1.md:63-64 — факт Σ 9 WebP; `git grep -nE "65 (файл|WebP)" --
     finny-pet/docs` → 2 строки;
   - **замеры на закоммиченных WebP → `$T/measure.txt`** (числа CONTRACT и зондов): линия пола — строка перехода в колонках
     x = 100, 540, 980 `room_port_day.webp` (ожидаю 1228); верх доски подоконника — первая строка ≥ 200 px `furn_window`, где
     ширина α > 127 ≥ 300 px (213 → 71 dp); доска полки — первая строка α > 127 в колонке 228 `furn_shelf` (126); одеяло —
     первая строка α > 127 в колонке 192 `furn_bed` (51). Расхождение — правка числа в CONTRACT и зондах до спавна;
   - `git diff --name-only HEAD~1 HEAD -- finny-pet/app/src/game/java` → пусто.
3. **Замеры BASE** (коммита нет):
   - release BASE → `$T/base_release.apk`, размер в журнал; цикл `aapt2` п. 9 на нём → `furn_*` **7 × FAIL** (ссылок нет —
     R8 убрал), `room_port_*` 2 × OK (без этого красного п. 9 не засчитывается);
   - lint BASE: game без сетевых = 5 + 7 `UnusedResources` `furn_*` (ожидание; `grep -c 'UnusedResources'
     app/build/reports/lint-results-gameDebug.xml` → 7), classic — замерить; тестов 600;
   - счёт БЮДЖЕТА не пустой (из finny-pet/, на уже принятом диффе 1b4): `git diff -w 47f5001 8235e47 -- app/src | grep
     '^+[^+]' | grep -vc '^+import '` → 14 (факт 2026-09-29); 0 — стоп, путь неверен;
   - `python tools/art_check.py bg finny-pet/app/src/game/res/drawable-nodpi/room_port_day.webp $T/bgd` → `CONTRAST OK`
     (новая оболочка под текстами экранов на комнате; самопроверка — тёмный верх, A1d1 п. 7 → `CONTRAST FAIL`);
   - `which` различает новые оболочки: PNG-копии `room_port_day`/`room_port_evening` → `python tools/art_check.py which …`
     → `room_port_day`, `room_port_evening` (лучшая ≥ 0,6, вторая < 0,3).
4. **Макет ночи** (№ 91 б «размер проверить макетом»; PIL, `$T/night_mock.py`, образец — генератор листа
   `scratchpad/a1d/a1d_sheets.py`): вечерняя оболочка res + вуаль `#310F53` α 0,35 + кровать + питомец «blink» 180 dp поверх
   (зайка, кот, щенок; стадии 0 и 2) по раскладке ночи 360 × 640 (`emu_b4r_night`, столбец +21 dp); варианты а) 128 × 64 и
   `padding(bottom = 21.dp)`; б) 160 × 80 и 33 dp; вперемешку с ночью BASE («в воздухе»). Судья «ребёнок» вслепую (№ 35):
   «что делает зверёк? где он?»; «сцена»: кровать не «игрушечная», не висит, не светится. Машинные факты по а и б — в журнал:
   «низ кровати − F, dp» (F 409,3) и «пикселей кровати в рамке часов x 111,7..148,7 × y 309,3..346,7 dp» (часы — в вечерней
   оболочке res, макет их уже содержит). Выбор: а, если «спит в кровати / на кровати» ≥ 5 из 6 и «сцена» без high; иначе б —
   § 7 и `a1d2_pix.py` (`LIFT` 33, кровать ×1,25) правятся до спавна; если «сцена» говорит «висит» и про а, и про б — берётся а
   (№ 91 б буквально, только Kotlin; «спит в кровати» у судьи «ребёнок» сохраняется), находка — № 98 на ворота с листом
   `a1d2_2_window_night.jpg` (ВОПРОСЫ НА ВОРОТА). Критерия «низ кровати ≥ F − 3 dp» в зондах нет: на 360 × 640 в демо он
   недостижим. Итог — в журнал.
5. **Зонды** — тексты в «Зонды приёмки» (извлечение там же); самопроверки `geom`, `label`, `title` встроены (`geom` —
   8 мутантов, по одному на ветку, `title` — кадр создания и кадр титула); `pix` — прогнан на синтетике (журнал); после правок r2 перепрогнать
   синтетику `pix` целиком (эталон § 4–6 с мечтой 24 dp `60 / 42`, ветки мебели, кольца «!» и мечты) — в журнал. **`$T/a1d2_room.sh`** — пишется до спавна workflow «автор → критик по коду → правка»
   (WORKFLOW № 44, 46) по таблице «Состояния» ниже.
6. **«До» на эмуляторе** (AVD `finni`, обычный запуск; debug BASE `adb install -r "$(cygpath -w …)"`, `pkgFlags` —
   `DEBUGGABLE` в лог, № 33; `mute`; висящих прогонов нет (№ 40) — ОТДЕЛЬНЫМ вызовом оболочки, в котором нет имён
   скриптов маршрута и их запуска: `ps -ef | grep -E "[t]own_route|[b]akery_states|[a]1d2_room"` → пусто, exit 1 (без
   скобок или рядом с именем скрипта в том же вызове совпадает строка обёртки `bash -c … eval '…'`, TOWN-J1-1b1-3 F5); отпечаток профиля
   `tools/adbui.sh exec-out run-as ru.finny.pet cat files/state.json | md5sum` — в журнал): `$T/a1d2_room.sh a2b` на
   `tools/adbui.sh wm360` и `a2bL` на 1080 × 2340 / 480 (`tools/adbui.sh shell wm size 1080x2340`, плотность 480 — раскладка
   S23 360 × 780 dp; после — `wm360`). Отрицательные контроли — каждый обязан покраснеть (№ 14, № 27): `a1d2_geom.py` на
   `a2b_plan1{10,13}.xml` и `a2bL_*` → FAIL «fridge: низ …» и «door: низ …»; `a1d2_pix.py` на `a2b_plan110` → FAIL «банка …»,
   «житель …» и по 5 мишеням мебели «холодильник …», «сундук …», «кровать в комнате …», «дверь …», «ящик …» (плашки вместо
   спрайтов), на `a2b_night10` → «кровать …», на `a2b_mailline10` → «портрет Оси …» (на `a2b_dream10` хвоста мечты в описании
   нет — ветку «мечта» держат синтетика и выключатель д п. 7);
   `A1D2_FOOT=0.8 a1d2_title.py … 0.45` на `a2b_title10` (новая оболочка, код BASE) → FAIL «Копи, планир…» (≈ 3,6) и сноска
   (≈ 3,0), `… 0.6` → FAIL «модель»; «Как тебя зовут?» на `a2b_create10` при 0,45 — решение по § 8 для CreatePet — по строке
   «Как тебя зов» med/p10 вывода (med или p10 < 3,0 → § 8 на CreatePet), не по итоговому FAIL скрипта; `a1d2_label.py`
   на BASE (подписи на плашках) → `LABELS OK` — положительный контроль меры, отрицательный — встроенный мутант (кромка
   `#520978`). «Обрезано» на кадрах BASE — список в журнал (давние — 1b1-3). Статус-бар ночи (новая вечерняя оболочка уже в
   BASE) — информационно ≥ 3,0 : 1 (A1d1: 3,3–4,0); меньше — вопрос оркестратору по ассету, не кодеру.
7. **Положительный контроль на клоне** (№ 14, № 44; без него п. 10 не засчитывается): `git worktree add --detach "$T/wt"
   HEAD`, копия `finny-pet/local.properties`; в клоне — § 1–10 (оркестратор, по тексту), `./gradlew assembleGameDebug
   lintGameDebug` → exit 0; lint game без сетевых по пресету (ACCEPTANCE_PRESETS.md:50-52) → 5 (на BASE 5 + 7
   `UnusedResources`), `grep -c 'UnusedResources\|UseOfNonLambdaOffsetOverload' app/build/reports/lint-results-gameDebug.xml`
   → 0 — дефекты lint в тексте CONTRACT ловятся до спавна, а не на приёмке; установить (`pkgFlags`), отпечаток профиля = п. 6;
   `$T/a1d2_room.sh a2p` (оба профиля) → все зонды п. 10 зелёные; `geom` на `anim`-кадрах (анимации выкл.) и обычных —
   разница низа холодильника и двери ≤ 2 dp (иначе § 3, запасной ход). Затем выключатели ровно X (№ 44), по одной
   пересборке, `a1d2_room.sh a2x plan1 dream night` на 360 × 640:
   а) ряд банок без `offset(y = 12.dp)` → `pix` FAIL «банка …», остальное OK;
   б) житель 40 dp `CenterStart` (как BASE) → FAIL «житель …»;
   в) без ореола (§ 2 `label` без `copy`) → `label` на `plan113`: FAIL «Список …» ожидаем (находка A1d1); OK — в журнал
      «ореол не обязателен по числу», § 2 остаётся (решение GATE_QUEUE); FAIL и с ореолом — обводка по § 2, повтор;
   г) без кровати ночью → FAIL «кровать …»;
   д) мечта не рисуется (хвост описания остаётся) → на `a2x_dream10` FAIL «мечта …»;
   е) «!» без `.background(Color.White, CircleShape).padding(2.dp)` (кружок без кольца) → на `a2x_plan110` `pix` FAIL ««!» …»;
   ж) `R.drawable.furn_door` в мишени холодильника → `pix` FAIL «холодильник …»;
   з) сундук `Image(painterResource(R.drawable.furn_chest), null, Modifier.size(40.dp))` → `pix` FAIL «сундук …».
   Здесь же живьём подтверждается выбор п. 4 (кадры `a2p_night{,_cat,_puppy}{10,13}`: «Проснуться» при 1,3 на экране — § 7;
   «низ кровати − F» и пиксели кровати в рамке часов — в журнал, это факт для № 98, не условие). Доли и контрасты — в
   журнал; пороги 0,60 / 0,90 (кровать, мебель, кольцо «!») / 30 / 24 — по фактам клона; красный на верной
   реализации — чинится зонд или спека (оркестратор), не кодер. Затем debug BASE обратно, `git worktree remove --force
   "$T/wt"`.
8. task-scope (base = последний коммит оркестратора; allow/protect — SCOPE); пересечение — пусто; `git diff --stat BASE HEAD
   -- finny-pet/app/src` → пусто; спавн coder (агент `coder`; промпт — CONTRACT, SCOPE, ANTI-SCOPE, ПРИ БЛОКЕРЕ, № 29).

**Состояния `$T/a1d2_room.sh PREFIX [состояния…]`** (scratch; образец — `tools/bakery_states.sh`: бэкап профиля с `[ -s ]`
и `json.load`, подмена `files/state.json` на каждое состояние, `force-stop`, снимок и сырой дамп uiautomator ВСЕГДА рядом
(`emu_PREFIX_<кадр>.png/.xml`), при 1,0 и 1,3, зонд обрезки в лог, `trap` → возврат и `cmp` → «profile restored»; касания —
точным описанием; промах — «not found»). Поля — `GameState` (main/…/domain/GameState.kt:12-60):
| Состояние | Правка `state.json` (от бэкапа демо-профиля) | Путь | Кадры и узел-якорь |
|---|---|---|---|
| plan0 | `plan.confirmed = false`, `jarNeed = jarWant = 0`, `envelope = []`, `events` без активных уличных | launch → «Продолжить» | `plan0{10,13}`: «Банки: разложи монеты» |
| plan1 | `plan.confirmed = true`, `jarNeed`/`jarWant` двузначные в пределах `balance`, `envelope = [{"text":"Карманные","amount":20}]`, `events` + одно активное событие с `place` не `home`, `animations = true` (в демо-бэкапе сейчас `false` — кроссфейд был бы `snap()`, GameApp.kt:134, :187), `day = 1` (< `rules.daysPerWeek` = 3: «Кровать: сон» → ночь, а не `BedAsk`/`endWeek`, RoomScreen.kt:80-83, GameViewModel.kt:333) | то же | `plan1{10,13}`: «Банки: Нужное …», «, есть событие» |
| spot3 | plan1 + `placed.spot_3` = вещь слота `table` из `owned` | то же | `spot3{10,13}` (левая колонка полная — потолок § 3) |
| dream | plan1 + `achievedGoals = [goal_scooter из town.goals: id, title, emoji, price]` | то же | `dream{10,13}`: «у забора мечта «Самокат»» (на BASE узла нет — не промах) |
| w5 | plan1 + `period = 5` | то же | `w5{10,13}`: «машет <житель недели 5>» |
| night, night_cat, night_puppy | plan1 (`day = 1`); для cat / puppy — `pet.speciesId` | «Кровать: сон» | `night*{10,13}`: узел «… спит» |
| anim | plan1 + `animations = false` | то же | `anim{10,13}` (§ 3 без входа) |
| title | — | launch | `title{10,13}` |
| create | `pet = null` | launch → «Играть» → «Дальше» петлёй (`for i in 1 2 3 4 5; do tap "Дальше" \|\| break; done`, как tools/town_route.sh:133 — не зависит от числа страниц StartScreens.kt:100-104) → «Создать питомца» → `has "Как тебя зовут?"` → снимок → «Зайка», «Рыжий», «Финни» → `scroll_to "Начать!"` (town_route.sh:69: листать до узла — при 1,0 кнопка ниже сгиба, при 1,3 форма длиннее) → «Начать!» → `has "Почтальон принёс конверт"` → снимок | `create{10,13}` (якорь «Как тебя зовут?», StartScreens.kt:176), `mailline{10,13}` (узел «Почтальон принёс конверт…») |
| screens | plan1 | «Сундук…», «Словарик», нижний ряд «Банки», «Копилка», «События», «Дверь: на улицу» — снимок и назад | `arrange10`, `glossary10`, `jars10`, `savings10`, `board10`, `street10` |
| rec | plan1; только 1,0 и только `wm360`; ПОСЛЕ `night` этого прогона | launch → «Продолжить» → дамп на месте (центр «Кровать: сон») → самопроверка `tools/rec.sh $T/rec_still 3` без касаний (неподвижная комната — один фон) → `tools/rec.sh $T/rec_on 8 "xy:<кровать>" "sleep:4" "xy:<проснуться>"` (центр «Проснуться» — из `emu_PREFIX_night10.xml` этого прогона) | `$T/rec_still`, `$T/rec_on` (`which.txt`, `f_*.png`, `r.mp4`) |
| recanim | plan1 + `animations = false`; как `rec` | то же, запись в `$T/rec_anim` | `$T/rec_anim` |

Записи — внутри `a1d2_room.sh`, под тем же `trap`: каждое состояние заново подменяет `state.json` от бэкапа (сдвиг дня
после сна в `rec` в `recanim` не попадает), возврат и `cmp` — один на весь прогон, «BACKUP» в логе не появляется; записи
делаются в прогоне п. 10 (`a1d2_room.sh a2` — все состояния таблицы, `rec`/`recanim` последними), до п. 11, который
пересоздаёт профиль маршрутом.

## ACCEPTANCE (команды 1–8 — из finny-pet/, 9–15 — из корня; гоняет оркестратор; `$T` — шапка ДО СПАВНА)
1. `./gradlew testClassicDebugUnitTest --rerun --console=plain` → exit 0; tests = 600, failed 0, skipped 0 (по XML, пресет)
2. `./gradlew testGameDebugUnitTest --rerun --console=plain` → exit 0; 600 / 0 / 0
3. `./gradlew assembleClassicDebug assembleGameDebug assembleGameRelease` → exit 0
4. `./gradlew lintClassicDebug lintGameDebug` → ошибок 0; без сетевых (ACCEPTANCE_PRESETS.md:50-53): game ≤ 5 (BASE ≈ 12 =
   5 + 7 `UnusedResources` `furn_*`), classic ≤ BASE; `grep -c 'UnusedResources' app/build/reports/lint-results-gameDebug.xml` → 0
5. `git diff --name-only BASE -- app/src/main/ app/src/classic/ app/src/test/ app/src/game/res/ app/src/game/AndroidManifest.xml app/src/game/java/ru/finny/pet/game/GameApp.kt app/src/game/java/ru/finny/pet/game/mock/ '*.gradle.kts' gradle/ gradle.properties gradlew gradlew.bat`
   → пусто
6. `git status --porcelain -uall -- app/src` → только 5 путей allow; из корня `node .claude/hooks/assert-oracle-intact.js` →
   exit 0; вставок / удалений без import — формула БЮДЖЕТА (путь `app/src`); `git diff -w BASE -- app/src | grep -c
   '^+import '` ≤ 9 (≤ 11 — если журнал ДО СПАВНА включил тон кровати § 7), `grep -c
   '^-import '` → 0; `git diff -w BASE -- app/src/game/java/ru/finny/pet/game/GameViewModel.kt | grep -c '^[-+][^-+]'` → 4;
   то же для `ui/TownUi.kt` → 3
7. грепы по коду без строк import (№ 45); `RS=app/src/game/java/ru/finny/pet/game/screens/RoomScreen.kt`, `NS=…/screens/NightScreens.kt`,
   `SS=…/screens/StartScreens.kt`, `TU=app/src/game/java/ru/finny/pet/game/ui/TownUi.kt`, `GV=…/game/GameViewModel.kt`,
   `C() { grep -v '^import' "$1"; }`:
   - `C $RS | grep -oE 'R\.drawable\.furn_[a-z]+' | sort | uniq -c` → 7 строк по 1 (bed, chest, door, fridge, mailbox, shelf,
     window); `C $NS | grep -c 'R.drawable.furn_bed'` → 1 (на BASE все 0); `grep -rn 'getIdentifier\|fun furnRes' app/src` → пусто;
   - `C $RS | grep -c 'color = Color.Transparent'` → 7; `C $RS | grep -E 'Target\("(Копилка|Словарик)' | grep -c 'color ='` → 0;
     `C $RS | grep -cE '🧳|🛏|G\.lavender|G\.sky|G\.goldDark'` → 0 (на BASE 7: :135, :136, :140, :153, :157, :175, :181);
   - описания 9 мишеней (10 строк: у полки две) побайтно, одинарные кавычки обязательны (в двойных `$mail` раскроется в
     пустоту — 0 = 0, `${s.savings}` — ошибка bash), команда одной строкой:
     ```
     for d in 'Окно: улица' 'Банки: разложи монеты' 'Банки: Нужное ' 'Копилка ${s.savings}' 'Холодильник: список нужного и цены' 'Сундук: обустроить комнату' 'Target("Словарик"' 'Кровать: сон' 'Дверь: на улицу' 'Почтовый ящик: плюс $mail придёт с новым конвертом'; do echo "$d=$(C $RS | grep -cF -e "$d")"; done | grep -vc '=1$'
     ```
     → 0 (без хвоста `| grep -vc` — 10 строк `…=1`; на 8235e47 — 0, прогнано 2026-09-29);
   - `C $RS | grep -cF 'FLOOR_PX = 1228f'` → 1 (или число замера п. 2); `C $RS | grep -c 'onGloballyPositioned'` → 1;
     `C $RS | grep -c 'coerceIn'` → 0; `C $RS | grep -cF -e '- 228.dp'` → 1 (без `-e` шаблон с «-» grep читает как опции —
     exit 2); `C $RS | grep -cF 'padding(top = fridgeTop)'` → 1;
     `C $RS | grep -cF 'offset { IntOffset(0, doorY.roundToPx()) }'` → 1; `C $RS | grep -cF 'offset(y = doorY)'` → 0;
   - `C $RS | grep -cF 'ResidentPic(resident, 64.dp, Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 10.dp))'` → 1;
     `C $RS | grep -cF 'GoalPic(it, 24.dp, Modifier.align(Alignment.TopStart).padding(start = 60.dp, top = 42.dp))'` → 1;
     `C $RS | grep -cF 'size(32.dp).background(Color.White, CircleShape).padding(2.dp).background(Color(0xFFE0004A), CircleShape)'` → 1;
     `C $RS | grep -cF 'wrapContentSize(unbounded = true)'` → 1; `C $RS | grep -cF ', у забора мечта «${it.title}»'` → 1;
     `C $RS | grep -c 'G.magenta'` → 1 (только «В городке» :115);
   - `C $RS | grep -cF 'align(Alignment.TopCenter).offset(y = 12.dp)'` → 1;
     `C $RS | grep -cF 'labelSmall.copy(shadow = Shadow(Color.White, blurRadius = 8f))'` → 1 (или форма журнала п. 7 в);
   - `C $NS | grep -cF 'Modifier.size(128.dp, 64.dp)'` → 1; `C $NS | grep -cF 'padding(bottom = 21.dp).size(180.dp)'` → 1 (или
     числа п. 4); `C $NS | grep -cF '"${pet.name} спит"'` → 1;
   - `C $SS | grep -cF 'G.purpleDeep.copy(alpha = 0.6f)'` → 1; `C $SS | grep -cF 'G.purpleDeep.copy(alpha = 0.45f)'` → 2 (1 —
     если п. 6 ДО СПАВНА распространил § 8 на CreatePet); `C $SS | grep -cF 'Color.White.copy(alpha = 0.8f)'` → 0;
   - `C $GV | grep -cF 'val speaker: String? = null'` → 1; `C $GV | grep -cF 'Line(envelopeLine, speaker = "osya")'` → 1;
     `C $TU | grep -cF 'ResidentPic(who, 48.dp, Modifier.clearAndSetSemantics {})'` → 1;
   - `grep -c '^import .*\*$' $RS $NS $SS $TU $GV` → 0 у всех
8. новые литералы — разность множеств (как TOWN-J1-1b4 п. 8):
   `comm -23 <(git diff -w BASE -- app/src | grep '^+[^+]' | grep -oE '"[^"]*"' | sort -u) <(git diff -w BASE -- app/src | grep '^-[^-]' | grep -oE '"[^"]*"' | sort -u)`
   → ровно `", у забора мечта «${it.title}»"` и `"osya"`; стоп-слова в литералах диффа (TOWN-J1-1a2.md:116-117) → 0;
   `git diff -w BASE -- app/src | grep '^+[^+]' | grep -cE 'contentDescription|stateDescription'` → 0
9. release `game` — R8 оставил 7 спрайтов, оболочки — новые (образец TOWN-J1-1b4.md :345-354):
   ```
   APK=finny-pet/app/build/outputs/apk/game/release/app-game-release.apk
   D=$("$LOCALAPPDATA/Android/Sdk/build-tools/36.0.0/aapt2.exe" dump resources $APK)
   for n in furn_window furn_shelf furn_fridge furn_door furn_bed furn_mailbox furn_chest room_port_day room_port_evening; do
     p=$(echo "$D" | grep -A1 "drawable/$n\$" | grep -o 'res/[^ ]*')
     s=$([ -n "$p" ] && unzip -l $APK "$p" | awk 'NR==4{print $1}'); w=$(stat -c%s finny-pet/app/src/game/res/drawable-nodpi/$n.webp)
     [ -n "$p" ] && [ "$s" = "$w" ] && echo "$n OK $p $s" || echo "$n FAIL path=$p size=$s src=$w"
   done
   ```
   → 9 × OK (на release BASE — `furn_*` 7 × FAIL, ДО СПАВНА п. 3). Прирост и остаток — БЮДЖЕТ.
10. живая проверка — эмулятор (AVD `finni`, обычный запуск), debug п. 3 (`cygpath -w`, `pkgFlags` — `DEBUGGABLE`, № 33),
    `mute`; отпечаток профиля = ДО СПАВНА п. 6 (иначе пара «до/после» переснимается подряд); висящих прогонов нет (№ 40) —
    отдельным вызовом оболочки без имён скриптов: `ps -ef | grep -E "[t]own_route|[b]akery_states|[a]1d2_room"` → пусто,
    exit 1. `$T/a1d2_room.sh a2` на `wm360` (все состояния таблицы, `rec`/`recanim` последними) и `a2L` на 1080 × 2340 / 480
    (без `rec`/`recanim`; затем `wm360`); лог: `grep -cE "not found|BACKUP"` → 0, «profile restored», «Обрезано: [1-9]» —
    только те же тексты, что в `a2b`:
    а) `a1d2_geom.py` на `a2{,L}_{plan0,plan1,spot3,dream,w5,anim}{10,13}.xml` (24 дампа) → `GEOM OK` каждый; низ холодильника
       и двери на `anim` и `plan1` — разница ≤ 2 dp;
    б) `a1d2_pix.py` на `a2_{plan0,plan1,spot3,dream,w5}{10,13}` (полка, житель, «!», мечта, 5 спрайтов мебели в мишенях), `a2_night{,_cat,_puppy}{10,13}`
       (кровать), `a2_mailline{10,13}` (портрет Оси) → `PIX OK` каждый;
    в) `a1d2_label.py` на комнатных кадрах `a2` и `a2L` при 1,0 и 1,3 → `LABELS OK` (числа в журнал: «Разложи» — plan0,
       числа — plan1, «Список» при 1,3 — все);
    г) `a1d2_title.py a2_title{10,13} … 0.6` → `TITLE OK`; `a2_create{10,13}` с α по § 8 → `TITLE OK`;
    д) `python $T/a1d2_tb.py finny-pet/screenshots a2b a2` и `a2bL a2L` → `N frames, 0 FAIL` (разница — только хвост мечты на
       `dream*`); `python tools/ui_measure.py <дамп> <кадр> 3` по всем дампам `a2`, `a2L` → кликабельных < 48 dp нет, узлов
       за краем нет; «Проснуться» на `a2_night13` целиком на экране (если на `a2b_night13` тоже);
    е) повтор при FAIL — пара «BASE → A1d2» подряд с отпечатком профиля; повторился — FAIL с напечатанными узлами.
11. регресс маршрута: `DUMP=1 tools/town_route.sh a2r 2>&1 | tee "$T/a2r_route.log"` → `grep -cE "not found|gate not
    passed|GEOM FAIL|CLOSE FAIL"` → 0, `grep -c "GEOM OK"` → 8, `grep -c "CLOSE OK"` → 2; `python tools/art_check.py which
    finny-pet/screenshots/emu_a2r_{room,room_w2,room_w3,room_w4,room_w5,jars}.png --expect room_port_day` → exit 0, `…
    emu_a2r_night.png --expect room_port_evening` → exit 0. Окна недель 2–4 — снимки маршрута (житель по неделям — судьи п. 13).
12. день → вечер → день (WORKFLOW № 31): записи сделаны в прогоне п. 10 — состояния `rec` и `recanim` таблицы «Состояния»
    (профиль, координаты и самопроверка `$T/rec_still` — там же); здесь — разбор `which.txt`:
    а) `$T/rec_on` → подряд `room_port_day×≥1 none×k room_port_evening×≥1 none×m room_port_day×≥1`, k и m ≥ 3, без возврата к
       дню внутри перехода; `$T/rec_still` — один фон;
    б) `$T/rec_anim` (анимации выкл.) → `none` ≤ 1 кадра на переход;
    в) кадры начала, середины и конца каждого перехода — на лист п. 13; `r.mp4` — владельцу (GATE_QUEUE).
13. листы (`tools/sheets.py`, ряд «до» — `a2b*`, «после» — `a2*` на тех же кадрах; заголовок — «S23 — доснять»):
    `town/a1d2_1_room.jpg` (plan0, plan1 при 1,0 и 1,3, spot3 при 1,3 — 360 × 640 и 360 × 780), `town/a1d2_2_window_night.jpg`
    (plan1, dream, w5, `a2r_room_w2…w4`; night × 3 вида, ночь BASE, кадры кроссфейда), `town/a1d2_3_screens.jpg` (title
    1,0 / 1,3 до/после, create, mailline, arrange, glossary, jars, savings, board, street). Судьи — одним workflow, вырезки 1:1
    с живых снимков, без подписей, нейтральные метки, ключ у оркестратора; каждая находка — перепроверка по картинке:
    - **«ребёнок» вслепую** (№ 35): по `plan1` и `a2L_plan1` «после» и «до» вперемешку — «на что здесь можно нажать? где
      поспать? где выйти на улицу? где еда и список? где твои вещи? куда приходят деньги? где банки? куда смотреть на
      улицу?»; по окну (`plan1`, `dream`, `w5`) — «кто в окне? где он стоит? что у забора?»; по ночи (3 вида) — «что делает
      зверёк? страшно?»; по `mailline` — «кто принёс конверт?»; по титулу 1,3 — прочитать подзаголовок. Критерий: 7 из 7
      предметов, «стоит на подоконнике», «спит в/на кровати» у 3 из 3, «Ося / почтальон», никто не назван страшным (ТЗ 3.5);
      самопроверка линзы — на кадрах «до» она не говорит «спит в кровати» и не находит мечту у забора;
    - **«доступность»**: серый и дейтеранопия `plan1` 1,3, `a2L_plan1` 1,3, `dream` — подписи читаются (числа п. 10 в),
      ореол не превращается в плашку, «!» — форма в сером; 48 dp и TalkBack — п. 10 д;
    - **«сцена»**: холодильник и дверь стоят на полу, дверь не наезжает на копилку, сундук ниже холодильника, банки на
      доске, житель на подоконнике не «висит», мечта у забора не закрывает жителя и «!», кровать ночью не «игрушечная» и
      не светится, портрет Оси; полупрозрачное поверх новой оболочки на экранах `a1d2_3` (WORKFLOW № 30). Первый кадр
      входа судье не поручается (по статичным листам не проверяется; § 3 «Первый кадр входа», ворота № 99); «кровать висит
      над полом» — не находка приёмки, а № 98 (§ 7).
    Зелёный — 0 подтверждённых high / medium. Кодер вид сам не меняет (§ 11); правка по находке — круг кодера (≤ 2) или вопрос.
14. **S23 — доснять при владельце** (GATE_QUEUE раздел 6, одним блоком с 1b4): `export ANDROID_SERIAL=RZCX923ZP4L` на весь
    блок, разблокирует владелец; громкость и `font_scale` до/после; бэкап профиля до маршрута и `cmp` (TOWN-A1f.md:222-233);
    `a1d2_room.sh a2s plan1 spot3 night` + `town_route.sh a2rs` → `a1d2_geom.py`, `a1d2_label.py` на дампах S23 → OK.
    Отступление — в журнал.
15. `python tools/art_check.py bg - - --selfcheck` → `SELFCHECK OK`; `bg …/room_port_day.webp $T/bgd` → `CONTRAST OK`;
    плавность (информационно, протокол TOWN-A1f.md п. 7: release BASE и A1d2, 4 прогона, первый отброшен, top1 `perf.sh
    max` ≤ 0,7 с после касания, № 26): титул «Продолжить» → комната, «Кровать: сон» → ночь; красное — медиана A1d2 − BASE
    > 50 мс и диапазоны не пересекаются → вопрос владельцу, задача не блокируется.

Порядок (WORKFLOW № 46): п. 1–9 → живая п. 10–12 → листы и судьи п. 13 → доки → ревьювер (без контекста кодера: спека,
BASE, коммиты оркестратора с путями, выводы ДО СПАВНА п. 6–7 и п. 10–13; сам гоняет п. 1–9).

## ЖИВАЯ ПРОВЕРКА (человек: docs/BACKLOG.md п. 1 и 11)
S23 при владельце — п. 14. TalkBack в комнате: 9 мишеней — те же фразы в том же порядке (окно с мечтой — «…, у забора мечта
«Самокат»»), картинки мебели отдельно не озвучиваются; ночь — «<имя> спит», кровать молчит; строка почтальона — текст без
имени портрета. Плейтест 1: узнают ли дети мебель без подписей, «Список» и мечту в окне.

## ВОПРОСЫ НА ВОРОТА
Заранее — нет: владелец ответил «по рекомендации, дальше посмотрю по скринам». После приёмки — раздел «A1d2» в
`docs/tasks/GATE_QUEUE.md`: листы п. 13, `r.mp4`, выжимка судей, числа (контраст подписей и титула, доля APK), «S23 —
доснять»; вопросы — по находкам судей и три известных заранее (посчитаны при критике, не блокируют спавн), с рекомендацией
и ценой; без других находок — строка «A1d2: листы, судьи без находок, рекомендация — принять» плюс эти три:
- **№ 98 — кровать ночью над панелью** (лист `a1d2_2_window_night.jpg`; числа — журнал ДО СПАВНА п. 4 и 7): кровать на ≈ 37–75
  dp выше линии пола и перед часами (Замеры «Кровать»). а) принять — № 91 б буквально, 0 правок; б) кровать с питомцем на
  линии пола, панель ночи ниже — на 360 × 640 в демо не помещается (409 + 12 + 254 > 616 dp): компактнее панель или демо-кнопка
  в другом месте, правка NightScreens и живая проверка при 1,3; в) ночью без кровати, как BASE (отказ от № 91 б).
  Рекомендация — а.
- **№ 99 — первый кадр входа в комнату без анимаций** (§ 3; запись `$T/rec_anim`, кадр входа Ночь → Комната): прежние места
  холодильника и двери — в кадре, где `AnimatedContent` и так рисует оба экрана (TOWN-A1c F2). Рекомендация — принять; иначе
  — отдельная задача с `graphicsLayer { alpha = if (placed) 1f else 0f }` у холодильника и двери (появление на кадр позже,
  проверка на клоне).
- **Для сведения — облачко питомца на 360 × 780 и S23** закрывает низ холодильника и двери с «Список» и «Улица» (Замеры
  «Облачко питомца»); временно, закрывается тапом. Рекомендация — принять (допуск A1d1); иначе — задача на `PetBubble`
  (ширина или якорь над питомцем).
- Строкой в разделе «A1d2»: решение 25 сужено — в окне одна, последняя достигнутая мечта (правая треть окна); несколько —
  строка BACKLOG.

## ДОКИ (оркестратор, после живой, до ревью; черновик в scratch; `git grep` по обоим `docs/` и `tools/`)
Грепы: `плашк` рядом с `комнат|мебел|окн|полк`, `🧳|🛏`, `лавандов`, `G\.sky`, `40 dp` рядом с `окн`, `точк[аи]-ручк`,
`спит над|в воздухе`, `ночью мебели|окна нет`, `торшер`, `запечен|запечён` рядом с `коврик|мячик`, `портрет питомца` рядом с
`Почтальон|LineHost`, `room_\*|4 WebP|58 (файл|WebP)`, `furn_`, `только через .when.`, `на фоне комнаты не измерялись`,
`достигнут\w* мечт`, `мечт\w* .*окн|окн\w* .*мечт`; каждое совпадение правдиво после задачи (история в `docs/tasks/*` не
правится):
- GAME_CONCEPT :1810 (`HOME`: «сейчас коврик, торшер и мячик запечены в фон» → оболочка пустая, мебель — спрайты `furn_*`
  в мишенях, TOWN-A1d2), §10.1 (окно: житель на подоконнике, «!»; холодильник и дверь на полу; :1341 «Видны достигнутые
  мечты (самокат у забора)» → «У забора — последняя достигнутая мечта (одна: ей остаётся правая треть окна, GATE_QUEUE
  раздел 3, № 77 б), TalkBack: «…, у забора мечта «Самокат»»»); :1257 «видны достигнутые мечты» → «видна последняя
  достигнутая мечта»; §18 № 25 (:2056) — «— реализовано TOWN-A1d2: в окне одна, последняя достигнутая мечта»; № 76, 77, 78,
  79, 81, 91 (:2109-2114, :2124) — «реализовано TOWN-A1d2», № 91 — выбранный размер кровати; §17 — статус A1d.
- TOWN-A1.md :42 — «A1d1 генератор, A1d2 встройка — сделано», факт доли и прирост APK; :63-64 — факт доли; :72-73 (правило
  «через `when` в `game/`») — «ссылки — только явные `R.drawable` (для id из контента — `when` в `game/`)».
- finny-pet/docs: ARCHITECTURE :28, :86 (комната: мебель — спрайты `furn_*` в мишенях на пустой оболочке, холодильник и дверь
  по линии пола фона), :117 (`LineHost`: у строки почтальона портрет Оси — `Line.speaker`), :185 (`room.py` — `--sprites`,
  `furn_*`); BUILD_AND_DEMO :305; UX_ACCESSIBILITY :43 (без плашек у 7 предметов, копилка и словарик — на плашках), :102
  (подписи на мебели — светлый ореол, контраст по месту), таблица контраста (+ «!» окна 32 dp, 4,92 : 1), :328 (описание окна
  + «, у забора мечта «…»»), :359-360 («окно крупнее — в A1d» → 64 dp); LIMITATIONS_ROADMAP :28 — если говорит о комнате;
  REQUIREMENTS_MATRIX 3.6 (мишени комнаты — Compose поверх спрайтов, TOWN-A1d2, живая проверка); LICENSES — в коммите ассетов.
- docs/BACKLOG.md — портрет говорящего в интро событий и строке «Новый конверт…» (цена: `Line.speaker` из домена);
  «несколько достигнутых мечт в окне (сейчас только последняя, TOWN-A1d2 § 4) — срок не назначен; цена: раскладка правой
  трети окна и хвост TalkBack»; спека круга `uiprops.py` № 92 б — «сохранить поле ≈ 5 dp под стеклом `ui_jar` или поправить
  `offset(y = 12.dp)` полки (TOWN-A1d2 § 5)».
- docs/WORKFLOW.md № 40 (:257, якорь `ps -ef | grep -E "town_route|bakery_states"`) и docs/HANDOFF.md:82 (`ps -ef | grep` и
  `kill -9`): если слияние TOWN-J1-1b1-3 ещё не внесло свою правку (TOWN-J1-1b1-3.md:572) — внести её одной правкой с общим
  правилом «имена фоновых скриптов в скобках первой буквы (`[t]own_route|[b]akery_states|[a]1d2_room`), отдельным вызовом
  оболочки без имён скриптов»; если внесло — только добавить `[a]1d2_room`. Две конфликтующие правки одной строки не делать;
  закрытые спеки (TOWN-J1-1b4.md:232-233, :359-360) не трогать.
- docs/tasks/GATE_QUEUE.md раздел 3 — «сделано A1d2» + раздел «A1d2»; docs/HANDOFF.md «Дальше» (№ 92 б, 1b1-3, A1e1);
  TOWN-A1d1.md — только строка журнала «встройка — TOWN-A1d2»; `tools/art_check.py` — докстринг `bg` (ДО СПАВНА п. 1).

## ПРИ БЛОКЕРЕ
`STATUS: BLOCKED` + один конкретный вопрос. Не изобретать: нет `R.drawable.furn_*` при сборке (ассеты не в BASE); `Target`
без параметра `color`; форма § 3–9 не компилируется или мишень выходит < 48 dp; § требует менять `Target`, `GameApp.kt` или
`main/`; хочется `furnRes`, подложку, тень, подвинуть мишень, поменять размер спрайта, жителя, мечты, значка или вернуть
плашку; приёмка п. 7 требует формы, которой нет в § 1–10.

## Зонды приёмки (тексты)
Первая строка блока — имя файла. Извлечь в `$T` (Git Bash, из корня):
```bash
T="$(cygpath -m "<scratchpad сессии>")/a1d2"; mkdir -p "$T" && python -c "import re,sys;t=open(sys.argv[1],encoding='utf-8').read();[open(sys.argv[2]+'/'+m.group(2),'w',encoding='utf-8').write(m.group(1)) for m in re.finditer(r'^\`\`\`python\n(# (\w+\.py) — .*?)^\`\`\`',t,re.S|re.M)]" docs/tasks/TOWN-A1d2.md "$T" && ls "$T"/a1d2_*.py
```
→ `a1d2_geom.py a1d2_label.py a1d2_pix.py a1d2_tb.py a1d2_title.py`. Запуск — из корня (читают `tools/art_check.py` и res;
`A1D2_RES` — другой каталог WebP). Прогнано при сведении (2026-09-29, синтетика в scratch `a1d2/r1/t/`, ассеты приёмки A1d1
через `A1D2_RES`): `geom` — самопроверка (формула пола 409,3 / 498,9 dp, эталон § 3, мутанты «висящий холодильник» и
«сундук над холодильником») OK; `pix` — эталон § 4–5 (банки на 12 dp, житель 64 dp на 8 / 10, мечта 58 / 43, «!») OK;
раскладка BASE (банки по центру, житель 40 dp, плашка неба) FAIL «банка … низ 116 px» × 3 и «житель: 0..239 px»; без мечты
при описании с мечтой FAIL «мечта»; житель на 4 / 6 dp (черновик «приёмка») FAIL «житель: 54..201 px»; ночь — кровать под
котом на 21 dp OK, без кровати — доля 0,00 FAIL, кровать на 33 dp — 0,35 FAIL; `title` — `emu_b4r_title.png` (старая
оболочка, α 0,45, `A1D2_FOOT=0.8`) подзаголовок 4,90 OK, «Питомец» 2,43 FAIL (давнее), модель α 0,6 → FAIL «модель»;
`label` — встроенная самопроверка OK. Черновиком «приёмка» (`a1d2/t/`): `label` на синтетике «Список» — без ореола
6,12 / 5,17 (Montserrat тоньше приложения — находку A1d1 не воспроизводит), мутант кольца `#520978` — 1,34 FAIL; `tb` —
лишний узел FAIL, хвост мечты на чужом кадре FAIL, на `dream*` OK. Не прогнано: живые дампы (до сборки их нет), пороги на
Android-отрисовке — ДО СПАВНА п. 6–7.
r2 (2026-09-29, после критики; извлечение командой выше в scratch `a1d2/r2t/`, ассеты приёмки A1d1 через `A1D2_RES`):
самопроверки `geom` (эталон + 8 мутантов), `label`, `title` (кадр создания с бумажной «Финни» — OK, титул с ней — FAIL
«модель «Финни»») — все assert проходят; `pix` на синтетике 360 × 640 (`r2t/pixsyn.py`: окно + Лиза 64 dp + «!» 32 dp +
мечта 24 dp `60 / 42`, 5 спрайтов мебели в мишенях § 3) — эталон со scooter / lego / zoo / paints → `[]`; мутанты: кружок
без кольца → «белого в кольце 0.00», мечта r1 `58 / 43` 32 dp → «низ 216 px, правый край 256 px», без мечты → FAIL,
`furn_door` в мишени холодильника → «доля 0.19»; `r2t/fitsyn.py` — сундук 40 dp внутри мишени → доля 0,48, маска ящика
после выреза конверта — 2 054 px. Порог кольца 0,9 и доли мебели 0,90 — перемерить на клоне (ДО СПАВНА п. 7).

```python
# a1d2_geom.py — TOWN-A1d2: раскладка комнаты по дампу uiautomator (px на dp = 3: эмулятор wm360, 1080 × 2340 / 480, S23).
# python a1d2_geom.py DUMP.xml → GEOM OK / GEOM FAIL [...], exit 0/1. Линия пола фона (Crop room_port_day, стык стены и пола
# FLOOR px из 1920, CONTRACT § 1): y = H/2 + (FLOOR − 960)·max(W/1080, H/1920) px, W × H — корень дампа. 9 мишеней — по
# описанию; размеры ± 1 px; попарно без наложений (и с питомцем); холодильник и дверь низом на полу ± 3 dp (№ 76 а);
# левая колонка по порядку: сундук не выше низа холодильника, словарик не выше низа сундука; сундук, словарик, кровать, ящик —
# не выше пола − 3 dp; все — между «room» и «bottom_row».
import re, sys
PX, FLOOR = 3.0, 1228
def nodes(xml):
    out = []
    for n in re.findall(r"<node [^>]*>", xml):
        g = lambda k: (re.search(k + r'="([^"]*)"', n) or [None, ""])[1]
        b = list(map(int, re.findall(r"-?\d+", g("bounds"))))
        if len(b) == 4: out.append((g("content-desc") or g("text"), g("resource-id").split("/")[-1], b))
    return out
T = {"window": ("Окно: улица", 112, 80), "shelf": ("Банки: ", 152, 72), "piggy": (r"Копилка \d+", 64, 64),
     "fridge": ("Холодильник: ", 64, 96), "chest": ("Сундук: ", 64, 48), "book": ("Словарик", 48, 48),
     "bed": ("Кровать: сон", 128, 64), "door": ("Дверь: на улицу", 64, 136), "mail": ("Почтовый ящик: ", 48, 48)}
def floor(W, H): return H / 2 + (FLOOR - 960) * max(W / 1080, H / 1920)
def check(N):
    W = max(b[2] for _, _, b in N); H = max(b[3] for _, _, b in N); F = floor(W, H); bad = []; R = {}
    for k, (p, w, h) in T.items():
        m = [b for d, _, b in N if (re.fullmatch(p, d) if k == "piggy" else d == p if k == "book" else d.startswith(p))]
        if len(m) != 1: bad.append(f"{k}: узлов {len(m)}"); continue
        b = R[k] = m[0]
        if abs(b[2] - b[0] - w * PX) > 1 or abs(b[3] - b[1] - h * PX) > 1:
            bad.append(f"{k}: {(b[2] - b[0]) / PX:.1f} × {(b[3] - b[1]) / PX:.1f} dp, нужно {w} × {h}")
    pet = [b for d, _, b in N if d.endswith("Нажми — что на уме")]
    ks = list(R) + (["pet"] if pet else []); R2 = dict(R, **({"pet": pet[0]} if pet else {}))
    for i, a in enumerate(ks):
        for c in ks[i + 1:]:
            A, B = R2[a], R2[c]
            if min(A[2], B[2]) > max(A[0], B[0]) and min(A[3], B[3]) > max(A[1], B[1]): bad.append(f"наложение {a} × {c}")
    for k in ("fridge", "door"):
        if k in R and abs(R[k][3] - F) > 3 * PX: bad.append(f"{k}: низ {R[k][3] / PX:.1f} dp, пол {F / PX:.1f} dp")
    for up, down in (("fridge", "chest"), ("chest", "book")):
        if up in R and down in R and R[down][1] < R[up][3] - 1: bad.append(f"{down} выше низа {up}: {R[down][1] / PX:.1f} < {R[up][3] / PX:.1f} dp")
    for k in ("chest", "book", "bed", "mail"):
        if k in R and R[k][1] < F - 3 * PX: bad.append(f"{k}: верх {R[k][1] / PX:.1f} dp выше пола {F / PX:.1f}")
    room = [b for _, r, b in N if r == "room"]; row = [b for _, r, b in N if r == "bottom_row"]
    if not (room and row): bad.append("нет узлов room / bottom_row")
    else:
        bad += [f"{k}: вне комнаты" for k, b in R.items() if b[1] < room[0][1] - 1 or b[3] > row[0][1] + 1]
    return bad, F
# самопроверка: формула пола (360 × 640 → 409,3 dp, S23 1080 × 2340 → 498,9 dp), эталон раскладки § 3, мутант на каждую ветку (№ 44)
assert abs(floor(1080, 1920) / PX - 409.33) < 0.01 and abs(floor(1080, 2340) / PX - 498.87) < 0.01, "SELFCHECK FAIL: формула пола"
d = lambda x0, y0, w, h: [round(x0 * PX), round(y0 * PX), round((x0 + w) * PX), round((y0 + h) * PX)]
def syn(fy, cy):  # синтетика 360 × 640: средняя зона 286..540 dp; fy — верх холодильника, cy — верх сундука
    return [("", "", [0, 0, 1080, 1920]), ("", "room", d(0, 194, 360, 350)), ("", "bottom_row", d(0, 544, 360, 72)),
            ("Окно: улица", "", d(8, 198, 112, 80)), ("Банки: разложи монеты", "", d(128, 198, 152, 72)), ("Копилка 0", "", d(288, 198, 64, 64)),
            ("Холодильник: список нужного и цены", "", d(8, fy, 64, 96)), ("Сундук: обустроить комнату", "", d(8, cy, 64, 48)),
            ("Словарик", "", d(8, 492, 48, 48)), ("Кровать: сон", "", d(116, 476, 128, 64)),
            ("Дверь: на улицу", "", d(288, 273.3, 64, 136)), ("Почтовый ящик: плюс 0 придёт с новым конвертом", "", d(304, 492, 48, 48))]
assert not check(syn(312, 426))[0], "SELFCHECK FAIL: эталон %s" % check(syn(312, 426))[0]   # § 3: отступ 26 = 254 − 228
assert any(s.startswith("fridge: низ") for s in check(syn(286, 426))[0]), "SELFCHECK FAIL: висящий холодильник прошёл"
assert any(s.startswith("chest выше") for s in check(syn(312, 380))[0]), "SELFCHECK FAIL: сундук над холодильником прошёл"
rep = lambda N, pre, b: [(t, r, b if t.startswith(pre) else bb) for t, r, bb in N]
_B = syn(312, 426)
for _N, _want in ((rep(_B, "Словарик", d(8, 493.3, 48, 46.7)), "book: 48.0 × 46.7"),     # сжатый словарик (без потолка § 3)
                  (rep(_B, "Дверь", d(288, 250, 64, 136)), "наложение piggy × door"),       # 262 — стык, не наложение
                  (rep(_B, "Окно", d(8, 190, 112, 80)), "window: вне комнаты"),
                  (rep(_B, "Кровать", d(116, 400, 128, 64)), "bed: верх"),
                  (_B + [("Словарик", "", d(8, 492, 48, 48))], "book: узлов 2"),
                  ([n for n in _B if n[1] != "room"], "нет узлов room")):
    assert any(s.startswith(_want) for s in check(_N)[0]), "SELFCHECK FAIL: мутант «%s» прошёл: %s" % (_want, check(_N)[0])
if __name__ == "__main__":
    bad, F = check(nodes(open(sys.argv[1], encoding="utf-8").read()))
    print(("GEOM OK" if not bad else "GEOM FAIL %s" % bad), "пол %.1f dp" % (F / PX)); sys.exit(bool(bad))
```

```python
# a1d2_pix.py — TOWN-A1d2: что стоит на мебели, по пикселям снимка 1080 × 1920 (фон комнаты и спрайт мишени — 1:1).
# python a1d2_pix.py SHOT.png DUMP.xml → PIX OK / FAIL, exit 0/1; из корня (WebP — game/res; A1D2_RES — другой каталог).
# Модель мишени = room_port_day + furn_<id> 1:1 по bounds узла; «нарисовано поверх» — max|Δ| > 30 со снимком.
# Полка: у каждой из 3 банок (колонки ±10 dp от 29 / 76 / 123 dp) первое пятно сверху (крышка + банка) кончается на доске:
#   низ ∈ [верх доски; верх доски + 8 px] (верх доски — первая строка α > 127 в колонке w/2 furn_shelf, 126 px).
# Окно: житель (колонки 12..48 dp) — низ ∈ [207; 220] px (ступни на 71,5 dp: верх подоконника furn_window — 71 dp), высота
#   ≥ 40 dp; «есть событие» — кружок #E0004A ≥ 3000 px в углу 40 × 40 dp и доля белого (min ≥ 245) в кольце r 44..46 px
#   вокруг центроида красного ≥ 0,9 (сам furn_window даёт в углу > 700 белых px — счёт белого по углу ничего не проверял);
#   «мечта» — колонки 60..84 dp (правее жителя: его фигура кончается на 57,4 dp), низ ∈ [180; 196] px (газон у забора, 60..65,3
#   dp), правый край ниже «!» (строки 40..72 dp) ≤ 252 px (стекло до 85,7 dp). Мебель — холодильник, сундук, кровать в
#   комнате, дверь, ящик: доля пикселей маски спрайта (α > 200) с max|Δ| ≤ 24 к модели «фон + спрайт» 1:1 по bounds ≥ 0,90,
#   без зон подписей (холодильник, дверь — нижние 36 dp; кровать — 40..88 × 12..52 dp; ящик — колонка 10..38 dp: конверт с
#   пульсом до 1,15 и «+N»); маска < 300 px или bounds не размера спрайта — доля 0 (FAIL, не nan). Полку и окно держит over()
#   выше. Ночь (узел «… спит», рамка 180 dp): furn_bed 1:1
#   под питомцем — низ кровати на LIFT dp ниже низа рамки (CONTRACT § 7), по центру рамки; доля пикселей маски (α > 200)
#   с max|Δ| ≤ 24 ≥ 0,90, лучший сдвиг ± 2 dp; не считаются верхние 20 dp кровати в центре ± 48 dp — там стоит питомец.
# Строка «Почтальон принёс конверт…» (узел line, TownUi.kt:306): портрет 48 dp — res_osya (LANCZOS до 144 px, маска α > 200,
#   max|Δ| ≤ 40): лучшая доля ≥ 0,60 в полосе x0 + 10 dp ± 2 dp, y от y0 + 10 dp вниз на 40 dp (Row по центру строки).
import os, re, sys, numpy as np
LIFT = 21  # dp: CONTRACT § 7
from PIL import Image
RES = os.environ.get("A1D2_RES", "finny-pet/app/src/game/res/drawable-nodpi")
def rgba(n): return np.asarray(Image.open(f"{RES}/{n}.webp").convert("RGBA")).astype(float)
def nodes(xml): return [((re.search(r'content-desc="([^"]*)"', n) or [0, ""])[1], list(map(int, re.findall(r"-?\d+", re.search(r'bounds="([^"]*)"', n)[1]))))
                        for n in re.findall(r"<node [^>]*>", xml) if 'bounds="' in n]
def over(S, BG, b, sprite):  # где UI рисует поверх модели «фон + спрайт»
    sp = rgba(sprite); a = sp[..., 3:] / 255; M = BG[b[1]:b[3], b[0]:b[2]] * (1 - a) + sp[..., :3] * a
    return np.abs(S[b[1]:b[3], b[0]:b[2]] - M).max(2) > 30
FURN = [("Холодильник: ", "furn_fridge", "холодильник", [(0, 60, 64, 96)]), ("Сундук: ", "furn_chest", "сундук", []),
        ("Кровать: сон", "furn_bed", "кровать в комнате", [(40, 12, 88, 52)]), ("Дверь: на улицу", "furn_door", "дверь", [(0, 100, 64, 136)]),
        ("Почтовый ящик: ", "furn_mailbox", "ящик", [(10, 0, 38, 48)])]   # вырезы — зоны подписей и конверта, dp (x0, y0, x1, y1)
def fit(S, BG, b, sprite, cut):  # доля маски спрайта (α > 200), где снимок = модель «фон + спрайт» (max|Δ| ≤ 24); 0 — пустая маска / не тот размер
    sp = rgba(sprite); h, w = sp.shape[:2]; m = sp[..., 3] > 200
    if (b[3] - b[1], b[2] - b[0]) != (h, w): return 0.0, 0
    for x0, y0, x1, y1 in cut: m[y0 * 3:y1 * 3, x0 * 3:x1 * 3] = False
    if m.sum() < 300: return 0.0, int(m.sum())
    a = sp[..., 3:] / 255; M = BG[b[1]:b[3], b[0]:b[2]] * (1 - a) + sp[..., :3] * a
    return float((np.abs(S[b[1]:b[3], b[0]:b[2]] - M).max(2)[m] <= 24).mean()), int(m.sum())
def blob_bottom(col):  # низ первого пятна сверху: до первого просвета ≥ 3 строк
    ys = np.where(col)[0]
    if not len(ys): return None
    for i in range(1, len(ys)):
        if ys[i] - ys[i - 1] > 3: return int(ys[i - 1])
    return int(ys[-1])
def check(S, N, BG, L=()):   # L — (text или resource-id, bounds) для строки LINE
    bad, box = [], lambda pre: next((b for d, b in N if d.startswith(pre)), None)
    sh = box("Банки: ")
    if sh:
        D = over(S, BG, sh, "furn_shelf"); A = rgba("furn_shelf")[..., 3]; top = int(np.where(A[:, A.shape[1] // 2] > 127)[0][0])
        for c in (29, 76, 123):
            y = blob_bottom(D[:, (c - 10) * 3:(c + 10) * 3].any(1))
            if y is None or not top <= y <= top + 8: bad.append(f"банка {c} dp: низ {y} px, доска {top}..{top + 8}")
    wi = next(((d, b) for d, b in N if d.startswith("Окно: улица")), None)
    if wi:
        d, b = wi; D = over(S, BG, b, "furn_window")
        if "машет" in d:
            ys = np.where(D[:, 36:144].any(1))[0]
            if not len(ys) or not 207 <= ys.max() <= 220 or ys.max() - ys.min() < 120: bad.append(f"житель: {ys.min() if len(ys) else None}..{ys.max() if len(ys) else None} px")
        if "есть событие" in d:
            q = S[b[1]:b[1] + 120, b[2] - 120:b[2]]
            rm = np.abs(q - (224, 0, 74)).max(2) <= 24; red = int(rm.sum()); ys, xs = np.where(rm)
            cy, cx = (ys.mean(), xs.mean()) if red else (60, 60)
            yy, xx = np.mgrid[:120, :120]
            band = np.abs(np.hypot(yy - cy, xx - cx) - 45) <= 1   # кольцо 42..48 px (32 dp снаружи, 28 dp заливка), ±1 px на сглаживание
            share = float((q.min(2)[band] >= 245).mean())
            if red < 3000 or share < 0.9: bad.append(f"«!»: кружок {red} px, белого в кольце {share:.2f}")
        if "мечта" in d:
            ys = np.where(D[:, 180:252].any(1))[0]; xs = np.where(D[120:216, 180:].any(0))[0]
            if not len(ys) or not 180 <= ys.max() <= 196 or not len(xs) or 180 + xs.max() > 252:
                bad.append(f"мечта: низ {ys.max() if len(ys) else None} px, правый край {180 + xs.max() if len(xs) else None} px")
    pet = next((b for d, b in N if d.endswith(" спит")), None)
    if pet:
        bed = rgba("furn_bed"); m = bed[..., 3] > 200; h, w = m.shape
        m[:60, w // 2 - 144:w // 2 + 144] = False
        x0, y0 = (pet[0] + pet[2]) // 2 - w // 2, pet[3] + LIFT * 3 - h
        best = max((float((np.abs(S[y0 + dy:y0 + dy + h, x0 + dx:x0 + dx + w] - bed[..., :3]).max(2)[m] <= 24).mean()), dx, dy)
                   for dy in range(-6, 7) for dx in range(-6, 7))
        if best[0] < 0.90: bad.append(f"кровать: доля {best[0]:.2f} (сдвиг {best[1]}, {best[2]} px)")
    for pre, sp, name, cut in FURN:
        b = box(pre)
        if b:
            f, n = fit(S, BG, b, sp, cut)
            if f < 0.90: bad.append(f"{name}: доля {f:.2f} (маска {n} px)")
    ln = next((b for d, b in L if d == "line"), None)
    if ln and any(t.startswith("Почтальон принёс конверт") for t, _ in L):
        o = np.asarray(Image.open(f"{RES}/res_osya.webp").convert("RGBA").resize((144, 144), Image.LANCZOS)).astype(float); m = o[..., 3] > 200
        best = max((float((np.abs(S[y:y + 144, x:x + 144] - o[..., :3]).max(2)[m] <= 40).mean()), x - ln[0], y - ln[1])
                   for x in range(ln[0] + 24, ln[0] + 37, 2) for y in range(ln[1] + 24, min(ln[1] + 151, ln[3] - 143), 2))
        if best[0] < 0.60: bad.append(f"портрет Оси: доля {best[0]:.2f} ({best[1]}, {best[2]} px)")
    return bad
if __name__ == "__main__":
    S = np.asarray(Image.open(sys.argv[1]).convert("RGB")).astype(float)
    if S.shape[:2] != (1920, 1080): print("PIX FAIL: снимок не 1080 × 1920 (фон 1:1 только на 360 × 640)"); sys.exit(1)
    N = nodes(open(sys.argv[2], encoding="utf-8").read())
    BG = rgba("room_port_day")[..., :3]
    seen = [k for k, p in (("полка", "Банки: "), ("окно", "Окно: улица"), ("мебель", "Холодильник: "), ("ночь", None)) if any(d.startswith(p) if p else d.endswith(" спит") for d, _ in N)]
    X = open(sys.argv[2], encoding="utf-8").read()
    L = [(t or r.split("/")[-1], list(map(int, re.findall(r"-?\d+", b)))) for t, r, b in re.findall(r'<node [^>]*?text="([^"]*)" resource-id="([^"]*)"[^>]*?bounds="([^"]*)"', X)]
    seen += ["строка"] if any(t.startswith("Почтальон принёс конверт") for t, _ in L) else []
    bad = check(S, N, BG, L) if seen else ["нет ни полки, ни окна, ни ночи, ни строки почтальона"]
    print(("PIX OK" if not bad else "PIX FAIL %s" % bad), seen); sys.exit(bool(bad))
```

```python
# a1d2_label.py — TOWN-A1d2: контраст подписей на мебели по месту, на живом снимке (любой экран, 3 px на dp).
# python a1d2_label.py SHOT.png DUMP.xml → LABELS OK / FAIL, exit 0/1; из корня. Зона подписи — по bounds мишени из дампа:
# «Список», «Улица» — нижние 36 dp, «Сон» — середина 40..88 × 12..52 dp, «+N» — ящик 0..48 × 22..48 dp, числа банок и
# «Разложи» — полка 0..152 × 44..76 dp. Текст — пиксели G.ink (max|Δ| ≤ 30: G.ink и G.purpleDeep расходятся на 49); фон —
# кольцо 3..5 px вокруг текста (сглаживание букв и 1–2 px ореола не считаются); пороги art_check.bg: среднее ≥ 4,5 : 1 и
# p10 ≥ 3,0 : 1. Конверт ящика (0..48 × 0..22 dp) — графика G.purpleDeep: среднее ≥ 3,0 : 1. Текста в зоне < 40 px — FAIL «нет подписи» (подпись пропала или не того цвета).
import re, sys, numpy as np
from PIL import Image
from scipy import ndimage
sys.path.insert(0, "tools"); from art_check import lum, INK
PD, P = (0x31, 0x0F, 0x53), 3
Z = [("Холодильник: ", "Список", lambda w, h: (0, h - 36, w, h + 4), INK, 4.5, 3.0),
     ("Дверь: на улицу", "Улица", lambda w, h: (0, h - 36, w, h + 4), INK, 4.5, 3.0),
     ("Кровать: сон", "Сон", lambda w, h: (40, 12, 88, 52), INK, 4.5, 3.0),
     ("Почтовый ящик: ", "+N", lambda w, h: (0, 22, 48, 48), INK, 4.5, 3.0),
     ("Почтовый ящик: ", "конверт", lambda w, h: (0, 0, 48, 22), PD, 3.0, 0.0),
     ("Банки: ", "числа / Разложи", lambda w, h: (0, 44, 152, 76), INK, 4.5, 3.0)]
def nodes(xml): return [((re.search(r'content-desc="([^"]*)"', n) or [0, ""])[1], list(map(int, re.findall(r"-?\d+", re.search(r'bounds="([^"]*)"', n)[1]))))
                        for n in re.findall(r"<node [^>]*>", xml) if 'bounds="' in n]
def ratio(S, b, zone, color):
    x0, y0, x1, y1 = zone((b[2] - b[0]) / P, (b[3] - b[1]) / P)
    Q = S[b[1] + round(y0 * P):b[1] + round(y1 * P), b[0] + round(x0 * P):b[0] + round(x1 * P)]
    text = np.abs(Q - color).max(2) <= 30
    if text.sum() < 40: return None
    ring = ndimage.binary_dilation(text, iterations=5) & ~ndimage.binary_dilation(text, iterations=2)
    L, Lc = lum(Q[ring]), float(lum(color))
    return (L.mean() + .05) / (Lc + .05), (np.percentile(L, 10) + .05) / (Lc + .05)
def check(S, N):
    bad, out = [], []
    for pre, cap, zone, color, m, p in Z:
        b = next((b for d, b in N if d.startswith(pre)), None)
        if b is None: continue
        r = ratio(S, b, zone, color)
        out.append(f"{cap} {'—' if r is None else '%.2f/%.2f' % r}")
        if r is None or r[0] < m or r[1] < p: bad.append(f"{cap} {'нет подписи' if r is None else '%.2f/%.2f' % r}")
    return bad, out
# самопроверка: тёмная подпись на белом проходит; та же на тёмной кромке #520978 — нет; пустая зона — «нет подписи»
_S = np.full((300, 300, 3), 255.0); _S[200:230, 60:240] = INK; _N = [("Холодильник: список", [0, 0, 192, 288])]
assert not check(_S, _N)[0], "SELFCHECK FAIL: белый фон %s" % check(_S, _N)[0]
_D = _S.copy(); _D[190:240, 50:250] = (0x52, 0x09, 0x78); _D[200:230, 60:240] = INK
assert check(_D, _N)[0], "SELFCHECK FAIL: тёмная кромка прошла"
assert "нет подписи" in check(np.full((300, 300, 3), 255.0), _N)[0][0], "SELFCHECK FAIL: пустая зона"
if __name__ == "__main__":
    S = np.asarray(Image.open(sys.argv[1]).convert("RGB")).astype(float)
    bad, out = check(S, nodes(open(sys.argv[2], encoding="utf-8").read()))
    print(("LABELS OK" if not bad and out else "LABELS FAIL %s" % (bad or ["нет мишеней"])), "; ".join(out)); sys.exit(bool(bad) or not out)
```

```python
# a1d2_title.py — TOWN-A1d2: тексты титула (StartScreens.kt:69-79) и «Как тебя зовут?» создания питомца (:176) на оболочке под
# вуалью G.purpleDeep α A; снимок 1080 × 1920. Текст, которого нет в дампе или видно < 10 px (прокрутка), пропускается; ни одного — FAIL.
# python a1d2_title.py SHOT.png DUMP.xml A → TITLE OK / FAIL, exit 0/1; из корня (фон — game/res room_port_day; A1D2_RES — иначе).
# Модель фона = room_port_day · (1 − A) + #310F53 · A. Проверка модели: в рамке каждого текста ≥ 30 % пикселей снимка совпадают
# с моделью (max|Δ| ≤ 12), иначе FAIL «модель» (вуаль в коде не A или фон не тот). Контраст текста к модели попиксельно:
# медиана ≥ порога (4,5; крупный текст «Питомец» 28 sp и «Финни» 44 sp — 3,0) и p10 ≥ 3,0. Цвета — StartScreens.kt:72-79.
# Кадр создания (есть узел «Как тебя зовут?») — только этот текст: там «Финни» — бумажная кнопка имени (StartScreens.kt:217,
# GameButton PAPER), модели вуали она не равна; на титуле «Как тебя зовут?» нет, «Финни» питомца — content-desc, не text.
import os, re, sys, numpy as np
from PIL import Image
sys.path.insert(0, "tools"); from art_check import lum
RES = os.environ.get("A1D2_RES", "finny-pet/app/src/game/res/drawable-nodpi")
T = {"Питомец": ((255, 214, 228), 3.0), "Финни": ((255, 255, 255), 3.0), "Копи, планируй, заботься": ((255, 214, 228), 4.5),
     "Без регистрации. Данные остаются на устройстве.": (None, 4.5),   # None — белый α W поверх фона (StartScreens.kt:79)
     "Как тебя зовут?": ((255, 214, 228), 3.0)}                         # headlineSmall — крупный (StartScreens.kt:176)
W = float(os.environ.get("A1D2_FOOT", "1.0"))                           # альфа белого сноски: 1,0 по § 8; на BASE — 0,8
def model(a): return np.asarray(Image.open(f"{RES}/room_port_day.webp").convert("RGB")).astype(float) * (1 - a) + np.array([49.0, 15.0, 83.0]) * a
def check(S, N, M):
    bad, out = [], []
    keys = ["Как тебя зовут?"] if any(d == "Как тебя зовут?" for d, _ in N) else [k for k in T if k != "Как тебя зовут?"]
    for t in keys:
        c, thr = T[t]; b = next((b for d, b in N if d == t), None)
        if b is None or min(b[3], 1920) - max(b[1], 0) < 10: continue
        b = [b[0], max(b[1], 0), b[2], min(b[3], 1920)]; m, s = M[b[1]:b[3], b[0]:b[2]], S[b[1]:b[3], b[0]:b[2]]
        share = float((np.abs(s - m).max(2) <= 12).mean())
        col = m * (1 - W) + 255 * W if c is None else np.array(c, dtype=float)
        r = (lum(col) + .05) / (lum(m) + .05); med, p10 = float(np.median(r)), float(np.percentile(r, 10))
        out.append(f"«{t[:12]}» {med:.2f}/{p10:.2f} модель {share:.2f}")
        if share < 0.3: bad.append(f"модель «{t[:12]}» {share:.2f}")
        elif med < thr or p10 < 3.0: bad.append(f"«{t[:12]}» {med:.2f}/{p10:.2f} < {thr}")
    return bad + ([] if out else ["ни одного текста титула"]), out
# самопроверка: модель — тёмная ровная вуаль; розовая строка поверх модели; бумажная кнопка «Финни» (#F4F2F8, текст #310F53)
_M = np.full((400, 600, 3), 60.0); _S = _M.copy(); _S[40:48, 30:560] = (255, 214, 228)
_S[100:160, 20:200] = 244.0; _S[125:135, 40:180] = (49, 15, 83)
_Q, _F = ("Как тебя зовут?", [20, 20, 580, 70]), ("Финни", [20, 100, 200, 160])
assert not check(_S, [_Q, _F], _M)[0], "SELFCHECK FAIL: кадр создания — бумажная «Финни» проверена: %s" % check(_S, [_Q, _F], _M)[0]
assert any(s.startswith("модель «Финни»") for s in check(_S, [_F], _M)[0]), "SELFCHECK FAIL: титул — «Финни» на бумаге прошла"
if __name__ == "__main__":
    S = np.asarray(Image.open(sys.argv[1]).convert("RGB")).astype(float)
    if S.shape[:2] != (1920, 1080): print("TITLE FAIL: снимок не 1080 × 1920"); sys.exit(1)
    N = [((re.search(r'text="([^"]*)"', n) or [0, ""])[1], list(map(int, re.findall(r"-?\d+", re.search(r'bounds="([^"]*)"', n)[1]))))
         for n in re.findall(r"<node [^>]*>", open(sys.argv[2], encoding="utf-8").read()) if 'bounds="' in n]
    bad, out = check(S, N, model(float(sys.argv[3])))
    print("TITLE OK" if not bad else "TITLE FAIL %s" % bad, "; ".join(out)); sys.exit(bool(bad))
```

```python
# a1d2_tb.py — TOWN-A1d2: значимые узлы TalkBack (текст, описание, clickable, focusable — без bounds: мебель сдвинута по № 76 а)
# на тех же кадрах те же, что на BASE. python a1d2_tb.py DIR PREFIX_BASE PREFIX_NEW → N frames, K FAIL; exit 0/1.
# Единственная ожидаемая разница — кадры dream*: описание окна + «, у забора мечта «…»» (CONTRACT § 4).
import re, sys, glob, os
from collections import Counter
d, a, b = sys.argv[1:4]
R = re.compile(r'<node [^>]*?text="([^"]*)"[^>]*?content-desc="([^"]*)"[^>]*?clickable="(\w+)"[^>]*?focusable="(\w+)"')
N = lambda p: Counter(n for n in R.findall(open(p, encoding="utf-8").read()) if n[0] or n[1] or "true" in n[2:4]) if os.path.exists(p) else Counter()
fr = sorted(os.path.basename(p)[len(f"emu_{a}_"):-4] for p in glob.glob(f"{d}/emu_{a}_*.xml"))
bad = 0
for f in fr:
    x, y = N(f"{d}/emu_{a}_{f}.xml"), N(f"{d}/emu_{b}_{f}.xml")
    lost, got = list((x - y).elements()), list((y - x).elements())
    dream = f.startswith("dream") and len(lost) == len(got) == 1 and got[0][1] == lost[0][1] + got[0][1][len(lost[0][1]):] \
        and re.fullmatch(r", у забора мечта «[^»]+»", got[0][1][len(lost[0][1]):] or "") is not None
    ok = sum(x.values()) > 0 and (not lost and not got or dream); bad += not ok
    print(f, f"OK sig {sum(x.values())}" + (" (мечта)" if dream else "") if ok else f"FAIL -{lost[:2]} +{got[:2]}")
print(f"{len(fr)} frames, {bad} FAIL")
sys.exit(1 if bad or not fr else 0)
```

## Журнал спеки
- 2026-09-29 (сессия 14), r1 — сведение черновиков «от кода» и «от приёмки» (подагент). Сверено с рабочим деревом
  0538492: RoomScreen.kt (:115, :123-192, :196-245), TownUi.kt (:75-80, :94-157, :174-179, :302-310), NightScreens.kt:48-74,
  StartScreens.kt:66-79, :113, :172, :176, GameApp.kt:185-190, :226, :255-263, GameViewModel.kt:68, :255-265, GameState.kt:12-60,
  :103, TrayScreen.kt:310, StreetScreen.kt:91, GameTheme.kt:25-73, `game/AndroidManifest.xml:9`, content.json (`osya`,
  `homeItems`, `spots`, `goals`), `tools/art_check.py` (`bg` :197-238, `which`/`backdrops` :261-298 — номера перенесены на
  bf68648 в r2), `town_route.sh` (`tp`, `has`), `rec.sh` (`xy:`), TOWN-A1.md:36-80 (bf68648), TOWN-A1d1.md:91-108, :496-512, :863-883, GATE_QUEUE раздел 3, GAME_CONCEPT
  §18 № 25, 27, 75-92, WORKFLOW № 44-47, `scratchpad/a1d/a1d_sheets.py` (`SILL`, `GEO`, `targets`). Перемерено на
  `$TEMP/s12/a1d/acc_w`: линия пола, окно, доска, одеяло, поле `ui_jar`; bbox 9 жителей и 5 `goal_*` в res.
- Расхождения черновиков и выбор:
  1. Линия пола 1228 («код») против 1229 («приёмка»): переход сглажен, 1228 — смешанная строка; взято 1228, разница 0,3 dp
     внутри допуска ± 3 dp; число всё равно перемеряется на закоммиченном WebP (ДО СПАВНА п. 2).
  2. Левая колонка: `padding(top = fridgeTop)` в прежнем `SpaceBetween` («код») вместо `Spacer` + `weight` («приёмка»): меньше
     диффа и совпадает с макетом листа, который видел владелец (`a1d_sheets.py targets(…, "a")`); зонд `geom` проверяет
     порядок (сундук ниже холодильника), а не зазор 0..40 dp — на S23 зазор ≈ 47 dp, и это «сундук под холодильник».
  3. Окно: житель `start 8 / top 10` (ступни на 71 dp = `SILL` листа a1d_3, принятого владельцем) вместо `4 / 6` (ступни на
     67,5 — на полосе под стеклом); «!» — форма «приёмки» (белый круг, `padding(2)`, заливка; без `border`) +
     `wrapContentSize(unbounded)` «кода» (строка 31,2 dp в круге 28 — иначе «Обрезано: !»).
  4. Мечта у забора — в задаче (решение владельца № 25: «в окне — житель недели, «!» события и достигнутые мечты»; «код»
     уводил в BACKLOG); место `58 / 43` — по bbox жителей (до 57,4 dp) и `goal_*` (низ 25–30 из 32), у «приёмки» `52 / 36`
     низ мечты на 63,5 dp — над подоконником. [r2: пересмотрено по S3 — `58 / 43` 32 dp ставил мечту на доску подоконника
     рядом с жителем (место варианта а № 77), а 63,5 dp — это газон, то есть «у забора»; взято 24 dp `60 / 42`.]
  5. Портрет Оси — в задаче (строка эпика A1d :42, набросок A1d1 :875; «код» уводил в BACKLOG); цена — 2 файла allow
     (`TownUi.kt` `LineHost`, `GameViewModel.kt` :68, :262), +4 строки, литерал `"osya"` рядом с `residentRes` той же папки.
  6. Полка: одна правка `offset(y = 12.dp)` у `Column` («приёмка») вместо `padding(top = 9)` + `offset(4)` у банки.
  7. Ореол: одна строка `label` (все три подписи мебели) вместо ореола только у «Списка» или у всех подписей с числами —
     числа и «+N» лежат на стене и ящике, прошли у судьи A1d1 (p5 ≥ 5,15); проверяет `label`.
  8. Ночь: кровать ПОД питомцем, лапы на одеяле («код»; № 91 б «под спящего питомца»), а не поверх лап; без тона, как сам
     питомец (тон — запасной ход по судье); «размер проверить макетом» — PIL-макет до спавна («приёмка») + живой клон.
  9. Сноска титула белой — сразу (оба замера на BASE 2,8–3,0 : 1), а не по условию; CreatePet — по замеру ДО СПАВНА п. 6.
  10. Эталон маски `bg` — `git show HEAD:…` и exit 1 без файла («приёмка»), без тихого отката к res («код»).
  11. Живая проверка — состояния подменой профиля (`a1d2_room.sh`, «приёмка») + регресс маршрута + `rec.sh xy:` (№ 39);
      высокий профиль 1080 × 2340 / 480 — оба черновика.
- Не проверено: сборка и живой запуск (кода нет); повторный вызов `onGloballyPositioned` после `scaleIn` входа (§ 3 —
  проверка и запасной ход на клоне); кнопки ночи при 1,3 с кроватью; финальные WebP после круга A1d1-2.
- 2026-09-29 (сессия 14), r2 — критика r1 (workflow: 3 критика по измерениям — «контракт», «приёмка», «ребёнок»; каждую
  находку опровергали 2 скептика). Находок 30: «контракт» F1–F6, «приёмка» A1–A17, «ребёнок» S1–S7. Подтверждено обоими — 19
  (приняты целиком), частично — 4 (принята подтверждённая часть), опровергнуто — 7 (не приняты). Номера строк перенесены на
  bf68648 (сверено: TOWN-A1.md правила :68-80, `when` :72-73, test-author :75; art_check.py `def bg` :197, `room =` :203,
  докстринг :11-16, `backdrops` :261, `which` :273; GAME_CONCEPT, GATE_QUEUE, WORKFLOW, finny-pet/docs с 0538492 не менялись).
  Принято (подтверждено):
  - F1 / A1 — комментарий § 3 без «coerceIn» (объяснение — в прозу § 3), греп `coerceIn` → 0 остаётся; вариант A1 `'coerceIn('`
    не взят — одно из двух.
  - F2 — дверь `offset { IntOffset(0, doorY.roundToPx()) }` + комментарий (скептики: дословный `offset(y = doorY)` — lint 6 при
    пороге 5, лямбда — 5); импортов 9, п. 6 ≤ 9, § 7 с тоном ≤ 11, грепы п. 7, lint на клоне ДО СПАВНА п. 7.
  - F3 — перенос строк § 1–9 запрещён (пробелы — на выбор); вариант «склеить без пробелов» отклонён — в предложенном виде не
    работает (отступ остаётся внутри фрагмента), «≤» вместо точных чисел п. 6 не ловит лишних правок.
  - F5 — номера строк по bf68648 (не по 4a75eee, как у критика: там art_check.py на 3 строки выше), якоря; ЗАВИСИТ ОТ и п. 0 —
    выполнено (7571ef7, b98298b; 5, :140 `m_gold`, дифф пуст — перепроверено).
  - F6 — сужение № 25 до одной мечты: грепы ДОКОВ `достигнут\w* мечт`, `мечт\w* .*окн`, GAME_CONCEPT :1341, :1257, :2056,
    строка BACKLOG (срок «после A1e» критика снят — придуман, A1e с окном не связана), строка ворот.
  - A2 — `grep -cF -e '- 228.dp'` (без `-e` — exit 2 при любом коде).
  - A3 — `title`: кадр создания определяется узлом «Как тебя зовут?», бумажная кнопка «Финни» не проверяется; самопроверка;
    решение по § 8 для CreatePet — по строке «Как тебя зов».
  - A4 — формула БЮДЖЕТА с `app/src` из finny-pet/ (было 0 при любом диффе); контроль непустоты на 47f5001..8235e47 → 14.
  - A5 — кольцо «!» мерится долей белого в полосе r 44..46 px вокруг центроида красного (счёт белого по углу закрывал сам
    спрайт окна: 711–753 px); выключатель е п. 7.
  - A6 — `pix` проверяет спрайты холодильника, сундука, кровати, двери, ящика в своих мишенях (без зон подписей; ящик — вырез
    колонки 10..38 dp, пустая маска — FAIL); окно и полка не добавлены (их держит over(); доля окна с жителем 0,87–0,89 < 0,9
    покраснела бы на верной работе); выключатели ж, з; отрицательный контроль на `a2b_plan110`.
  - A9 — записи `rec`/`recanim` внутри `a1d2_room.sh` под `trap`; plan1 — `animations = true` (в демо-бэкапе `false` — был бы
    `snap()`) и `day = 1` (иначе «Кровать: сон» → «Банки»/`BedAsk`); п. 12 — только разбор `which.txt`.
  - A10 — «ничего не мигает при входе» убрано из судьи «сцена»; цена записана в Замеры и § 3; ворота № 99 (вариант Б критика —
    `graphicsLayer` alpha — только как «иначе» на воротах, не в CONTRACT).
  - A13 — `ps -ef | grep -E "[t]own_route|[b]akery_states|[a]1d2_room"` отдельным вызовом, exit 1 (п. 6, п. 10, ДОКИ № 40 —
    согласовать с 1b1-3).
  - A14 — описания 10 строк — готовой командой в одинарных кавычках с фиксированным ожиданием (на 8235e47 → 0, прогнано).
  - A15 — путь create: «Дальше» петлёй → «Создать питомца» → `scroll_to "Начать!"`.
  - A17 — отрицательный контроль `bg` подменой эталона новой оболочкой; ветка «нет файла» отдельно; `sprite_check.py` с
    дампами `furn_<id>.json` из `$TEMP/s12/refbase` (правка критика «каталог regress» не работала: зонд ищет `furn_<id>.json`,
    regress пишет `room_furn_<id>.json`); `zones.py` полной командой.
  - S1 — кровать ночью висит над панелью и заходит на часы: факты в Замеры и журнал п. 4 / 7, ветка «висит у а и б → а», № 98
    на воротах; :271 «пятно под кроватью» → «под панелью». Отклонено из правки: критерий «низ кровати ≥ F − 3 dp» в зонде (на
    360 × 640 в демо недостижим) и блокирующий вопрос до спавна (владелец: «по рекомендации, дальше посмотрю по скринам»).
  - S3 — мечта у забора: 24 dp `60 / 42` (низ на газоне, 61–64 dp) вместо 32 dp `58 / 43` (доска подоконника, заходила на
    штору); зонд `pix` — низ ∈ [180; 196] px, правый край ≤ 252 px (у критика [183; 196] — провалил бы zoo, 182 px); греп п. 7.
    Вариант «переписать подпись на «на подоконнике»» отклонён — противоречит № 77 б и решению 25.
  Принято частично:
  - F4 / S5 — первый кадр входа без анимаций: факт принят (§ 3, Замеры, ворота № 99, строка судьи убрана); отклонено —
    тяжесть medium и «мигание на каждом входе» (кадр совпадает со смешанным кадром TOWN-A1c F2 — по разбору фаз, живьём не
    проверено), машинная проверка «≤ 1 кадра» (проходит и на BASE), формула вместо `onGloballyPositioned` (WORKFLOW № 38).
  - A11 — облачко питомца закрывает «Список» / «Улица» на 360 × 780 и S23: факт — в Замеры и ворота «для сведения»;
    отклонено — новое состояние `bubble`, исключения в зондах, кадр на лист (правка `PetBubble` вне задачи, на 360 × 640
    перекрытие меньше BASE, допуск A1d1).
  - A16 — мутанты на ветки `geom` (8, прогнаны); отклонено — «сжатие словарика ловит только geom» (ловят ещё `ui_measure` п. 10 д
    и греп п. 7), мутант двери с верхом 262 (стык, не наложение — взят 250), выключатель «без `coerceAtMost`» на клоне
    (дублирует п. 7 и п. 10 д).
  Отклонено (опровергнуто скептиками): A7 (реестр «Обрезано» по тексту «!»), A8 (события при запуске), A12 (`bg` не меряет
  тексты комнаты), S2 (пропорция кровати день / ночь), S4 (титул темнее ночи), S6 (кнопка «Сразу к итогу недели» при 1,3),
  S7 (кровать светится без числового порога).
- Не проверено после r2: сборка и живой запуск; `pix` на Android-отрисовке (пороги 0,9 кольца и 0,90 мебели), совпадение
  кадра входа с кадром F2 (запись `rec_anim`), запись `rec`/`recanim` внутри `a1d2_room.sh` (скрипт пишется до спавна, п. 5).
- **2026-09-29, сессия 15 — быстрый путь к сдаче** (владелец: «максимально быстро дойти до рабочей версии и залить в
  main»). П. 0 — 5 совпадений, `mail_flag` с `m_gold`, `app/` чисто. П. 1 — `art_check.py bg`: маска по
  `screenshots/emu_b_room_port_day.webp` (старая комната, `git show 83667c6:…`), `SELFCHECK OK` до и после замены; нет
  файла — exit 1 с подсказкой. П. 2 — BASE 7c6ebab: рендер room.py feat/town финальными параметрами (HIP), `SPRITES OK`,
  `LABEL ZONES OK`, bbox 7 × OK; оболочки 33 912 + 34 100 Б, `furn_*` 34 244 Б, Σ 9 = 102 256 Б; 73 WebP; замеры — пол
  1228, подоконник 213 (71 dp), доска 126, одеяло 51 — как в CONTRACT. Отложено до после сдачи: отрицательный контроль
  маски, п. 3 замеры BASE, зонды на BASE и клоне. Приёмка — ACCEPTANCE машинные пункты, живые снимки комнаты (день, вечер,
  ночь, титул) при 1,0 и 1,3, `ui_measure`, глазами, ревьювер. Task-scope — объединённый с TOWN-A1g2.
