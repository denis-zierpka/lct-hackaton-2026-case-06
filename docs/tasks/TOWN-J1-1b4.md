```
TASK: TOWN-J1-1b4 — спрайты выпечки в раунде пекарни: витрина, поднос и облачко покупателя рисуют `pastry_<id>` вместо emoji
      (`pastryRes`, № 66 а); попутно — системное «Назад» на итоге смены чистит строку питомца (§ 5, находка D1 доков 1b1-2)
EPIC: TOWN-J1 (docs/tasks/TOWN-J1.md; срез 1б — docs/tasks/TOWN-J1-1b.md: таблица :41-47, строка 1b4 :43, коммит ассетов
      :470-473, раздел «# TOWN-J1-1b4» :548, ответы ворот арта :772-776)
BASE: 47f5001 — коммит ассетов выпечки (сделан 2026-09-29 во время сведения: 6 `pastry_*.webp`, LICENSES, ARCHITECTURE,
      TOWN-A1.md; код `app/src` = 70928df). Кодер и ревьювер — от коммита спеки и зонда поверх него
      (`git diff --stat 47f5001 HEAD -- finny-pet/app/src` перед спавном пуст). Номера строк — дерево 2026-09-29 на
      HEAD 47f5001 (код `app/src` = 70928df; ассеты кода не трогают)
BRANCH: feat/town
ЗАВИСИТ ОТ: TOWN-J1-1b1-2 (70928df); ассеты 47f5001; решения владельца № 65 а, № 66 а, № 67 б
```

## КОНТЕКСТ
Ворота арта пилота (TOWN-J1-1b.md:772-776, GAME_CONCEPT §18 :2096-2099) сузили 1b4 до одной вещи:
- **№ 65 а** — прыжок питомца у всех работ (сделано 1b1); круассан в лапах — BACKLOG п. 16, не здесь;
- **№ 66 а** (повторно, 2026-09-29) — спрайты выпечки круга 1b3-2 приняты: лист `pilot_art_7_pastry2.jpg`, «ребёнок»
  вслепую 6 из 6, кромка багета 3,61 : 1, кекса 3,60 : 1 на белом; генератор `finny-pet/tools/art/props.py`
  (построители `pastry_*`, слит 184843d; круг 1b3-2 — 46d5e60 в `pilot/j1b`);
- **№ 67 б** — прилавок остаётся Compose-полосой `Counter` (TrayScreen.kt:113-123), спрайт — BACKLOG п. 17; № 68 снят.

Пункты 3 (`Counter` → `Image`) и 4 (`eatProp`, `PetAct.EAT`) контракта раздела 1b4 среза (TOWN-J1-1b.md:548-570) этими
ответами отменены. Ассетов N = 6 (TOWN-J1-1b.md:470: N = 6·[№ 66 а] + 1·[№ 67 а]). Итог: 1b4 — `pastryRes` и три места,
где выпечка рисуется emoji. Что видит ребёнок: на кнопках витрины, в кружках подноса и в облачке покупателя — те же
нарисованные круассан, хлеб, багет, крендель, пончик, кекс, что на листе ворот; ✓, пунктир, указатель ☝️, тексты и
TalkBack — как были.

### Карта кода (рабочее дерево, проверено)
Откуда id выпечки:
- контент — `content.json:1245-1252`, `town.jobs[job_bakery].menu` (`game: "TRAY"`, :1238): `croissant 🥐`, `bread 🍞`,
  `baguette 🥖`, `pretzel 🥨`, `donut 🍩`, `cupcake 🧁` (порядок = порядок ступеней меню, :1253-1259; с 7 смен — 6 видов,
  заказы `[3, 3, 4, 4]`, :1258); модель `Pastry(id, title, emoji)` (`TownContent.kt:98`); `emoji` обязателен и не пуст
  (`ContentValidationTest.kt:709`, `TownContentTest.kt:314`);
- домен знает только id; UI-лямбда `emoji` — TrayScreen.kt:178 (id → `job.menu…emoji`, `.orEmpty()`), передаётся в
  `TrayScene` (:192, :289); название для TalkBack — `vm.pastryTitle(id)` (`GameViewModel.kt:521`), к картинке не относится;
- число видов в заказе задаёт код, а не сид: `distinct = minOf(k, 1 + k / 2)` (`Town.kt:646`) — заказ из 3 — 2 вида, из
  4 — 3 вида; размеры идут покупателям по порядку (`customers.zip(sizes)`, `Town.kt:660-667`); сид профиля
  (`traySeed`, `Town.kt:636-641`) выбирает только сами виды.

Где выпечка рисуется emoji — ровно три вызова `Pic(null, emoji(…), …)`, все в TrayScreen.kt:
| # | Место | Строка | Размер | Контейнер и его семантика (не меняются) |
|---|---|---|---|---|
| 1 | витрина, кнопка изделия | :206 `Pic(null, emoji(id), 56.dp)` | 56 dp | белая карточка `weight(1f)` × `b` = 64–88 dp (:189, :202), `contentAlignment = Center`; `clickable(Role.Button, "Положить на поднос")`, `contentDescription = title`, `stateDescription = "на подносе: k"` (:203-204) |
| 2 | поднос, занятый слот | :340 `PopIn(i, item, animate) { Pic(null, emoji(item), 40.dp) }` | 40 dp | белый круг 48 dp, `clickable(Role.Button, "Убрать")`, `"$title на подносе"` (:336-337); пустой слот — пунктир `Canvas` 40 dp (:345) |
| 3 | облачко покупателя | :384 `Pic(null, emoji(id), s)` | `s` = 48 dp, в заказе из 4 — 40 dp (:373) | `Box(Modifier.size(s).drawBehind { if (gap) dashedCircle(G.purple, 3.dp) })` (:383), ✓ 20 dp поверх в углу (:385-390); облачко — один узел `clearAndSetSemantics { contentDescription = desc }` (:355-363) |

Где emoji выпечки НЕТ: итог смены (RoundScreen — только `ResidentPic`), экран заказа (PlaceScreen — «Новинка — крендель!»
текстом из `steps[].intro`), «Дневник», комната. Указатель `Pic(null, "☝️", 40.dp, …)` (:228) — не выпечка, остаётся.
Макеты `game/mock/*` и `tools/mock_bakery.py` — не приложение.

`Pic` (TownUi.kt:119-126): при `res != null` — `Image(painterResource(res), null, modifier.size(size))`; иначе emoji в
`Box(… .clearAndSetSemantics {})`. **Обе ветки дают в дампе uiautomator пустой узел.** Emoji — очищенный `Box`
(на `emu_s12r_wrong10.xml`: 168 × 168 px под каждой кнопкой витрины, 120 × 120 px в занятом слоте; в облачке детей нет —
оно очищено целиком). `Image` с описанием `null` даёт СВОЙ пустой узел: внутри `clipToBounds` → `graphicsLayer`, а это
`SemanticsModifierNode` в ui 1.11.4 (BOM 2026.06.01, `app/build.gradle.kts:83`; javap `SimpleGraphicsLayerModifier`);
узел картинки меньше 48 dp растянут до 48 dp (кошелёк 24 dp → 144 px в `emu_s12r_market10.xml`; у `IconBox`
«Подсказка» с `Image(icon, null, 34.dp)` — тот же пустой узел). Поэтому **дамп эффект не различает**: TalkBack держит
сравнение значимых узлов (п. 9 а), эффект на экране — пиксельный зонд (п. 9 б). Образец связки id → ресурс —
`residentRes` (TownUi.kt:131-143) и зонд `python tools/art_check.py residents` (art_check.py:342-371).

### Ассеты (замер 2026-09-29, `$TEMP/s12/j1b2/webp/`, круг 1b3-2; в BASE 47f5001 — те же байты, `cmp` 6 × same)
6 WebP 256 × 256 RGBA: baguette 4 596, bread 4 572, croissant 5 762, cupcake 5 412, donut 5 520, pretzel 7 732 — сумма
33 594 Б (≤ 70 000 доли J1, TOWN-A1.md:60-61; каждый ≤ 37 КБ, :69); `art_check.py bbox` — 6 × OK (поля ≥ 12 px, углы α 0).
- Пунктир «не хватает» (кольцо `s/2 − 3 dp … s/2`, :125-126, рисуется `drawBehind` под картинкой) спрайт закрывает на
  0–4 % площади кольца (α > 128; 48 dp: багет 3 %, крендель 2 %, круассан 1 %, прочие 0; 40 dp: багет и крендель 4 %,
  круассан 3 %); на остальных ≥ 96 % кольцо граничит с белым (#520978 на белом ≈ 12,6 : 1) — пунктир читается,
  `drawBehind` не меняется (вид — судьям п. 13, вопрос № 90 — только по находке).
- ✓ 20 dp (поверх, в правом нижнем углу) закрывает на 40 dp до 22 % изделия: кекс 0,22, крендель 0,20, хлеб 0,17,
  пончик 0,12, круассан 0,07, багет 0; на 48 dp — ≤ 0,07 (α > 128, BILINEAR до 120 / 144 px).
- Спрайт шире прежнего emoji: рамка непрозрачного 0,72–0,90 коробки против 0,66–0,67, наибольший радиус 0,77–1,03
  полуразмера против 0,70–0,91; площадь та же ± 8 % в обе стороны (спрайт / emoji: круассан 0,247 / 0,270, хлеб
  0,341 / 0,326, багет 0,154 / 0,167, крендель 0,265 / 0,259). Emoji мерено на `emu_s12r_round210` (кадр без указателя:
  на `wrong10` узел ☝️ [572,1265][692,1385] перекрывает коробку хлеба); кекс и пончик в emoji-кадрах не встречались.

## ТРЕБОВАНИЕ ТЗ (docs/sources/ТЗ_текст.txt)
2.5.14 «Новое задание добавляется без переработки основной логики» — изделие без ветки `pastryRes` рисуется своим emoji
из `content.json` (`else -> null`, как `itemRes`). 3.3 и раздел 5 п. 12 — право на изображения и перечень лицензий:
строка LICENSES — в BASE 47f5001 (LICENSES.md:55). 3.6 — мишени ≥ 48 × 48 dp и тексты 16 sp не меняются (картинки не
мишени); «Цвет не является единственным способом…» — изделия различаются силуэтом (кекс и круассан одного тона в сером —
различает форма, судьи 1b3-2), «не хватает» — формой (пунктир), «уже на подносе» — ✓; путь TalkBack без дублей и без
потери смысла — не меняется. 3.5 — новых строк нет. Решение № 58 (GAME_CONCEPT.md:2089): выпечка — спрайты, срез 1б.

## CONTRACT
Новых строк ребёнку и описаний TalkBack — 0. Домен, ViewModel (кроме одной строки § 5), `content.json`, модель `Pastry`, ресурсы (уже в BASE
47f5001), `Pic`, `Counter`, пунктир облачка, раскладка и размеры в dp — не меняются.

### 1. `pastryRes` — TownUi.kt, сразу после `itemRes` (после его `}` на :106, перед KDoc `goalRes` :108)
```kotlin
/** Drawable of a bakery pastry (pastry_<menuId>, props.py), or null — then the pastry's emoji is drawn. */
fun pastryRes(id: String): Int? = when (id) {
    "croissant" -> R.drawable.pastry_croissant
    "bread" -> R.drawable.pastry_bread
    "baguette" -> R.drawable.pastry_baguette
    "pretzel" -> R.drawable.pastry_pretzel
    "donut" -> R.drawable.pastry_donut
    "cupcake" -> R.drawable.pastry_cupcake
    else -> null
}
```
Сигнатура — дословно (по ней разбирает зонд `art_check.py pastries`); ветки — в порядке `menu`; последняя — `else -> null`.
Только явный `R.drawable` в `when` (TOWN-A1.md:70-71: R8 `isShrinkResources` молча выкидывает ресурс без такой ссылки);
`getIdentifier` и имена ресурсов строкой — нельзя. Отдельно от `itemRes`: у товаров свои id, а `goalRes` берёт `itemRes`
для мечт `item:x` (:114) — изделие пекарни мечтой не станет. Новых импортов в TownUi.kt не нужно (`R` импортирован).

### 2. Три вызова — TrayScreen.kt: только первый аргумент `null` → `pastryRes(…)`, размеры и обёртки те же
- :206 → `Pic(pastryRes(id), emoji(id), 56.dp)` (витрина);
- :340 → `PopIn(i, item, animate) { Pic(pastryRes(item), emoji(item), 40.dp) }` (поднос; анимация появления та же);
- :384 → `Pic(pastryRes(id), emoji(id), s)` (облачко; `s` :373, `drawBehind` :383 и ✓ :385-390 — без изменений).

Импорт — точечный `import ru.finny.pet.game.ui.pastryRes` (по алфавиту — после `particleTarget`, :97; wildcard нельзя,
WORKFLOW № 45). Лямбда `emoji` (:178) и сигнатура `TrayScene` (:289) остаются — emoji запасной путь. Описание картинке не
передавать (`Pic` передаёт `null` сам); `contentDescription`/`semantics` в дифф не добавлять.

### 3. TalkBack не меняется
Кнопка витрины — «{Изделие}», состояние «на подносе: k», действие «Положить на поднос»; слот — «{Изделие} на подносе»,
«Убрать»; облачко — «{Имя}. Заказ: …[. Ещё нужно: …]» одним узлом. Значимые узлы дампа (текст, описание, clickable,
focusable, bounds) на тех же состояниях — те же, что на BASE (п. 9 а).

### 5. «Назад» на итоге смены чистит строку питомца (GameViewModel.kt:184; находка D1 доков 1b1-2)
Сейчас `back()` на `Screen.Round` с итогом зовёт `closeRound()` и строки LINE не чистит (GameViewModel.kt:184), тогда
как все прочие ветки `back()` чистят (`lines.clear()`, :188), а кнопка «Готово» итога чистит с 1b1-2
(RoundScreen.kt:155 `{ vm.lines.clear(); vm.closeRound() }`; обоснование — TOWN-J1-1b1-2.md § 4: строка итога к экрану
места не относится и легла бы на «Начать смену»). Правка — ровно одна строка:
`if (screen is Screen.Round) { if (roundResult == null) finishRound() else { lines.clear(); closeRound() }; return }`.
Ветка `roundResult == null` (досрочное завершение раунда) — без изменений; `closeRound()` не меняется (его зовёт и
«Готово»). Новых строк ребёнку нет.

### 4. Явно НЕ определено (кодер спрашивает, а не решает)
Правка `Pic`, `itemRes`, `goalRes`, `residentRes`; размеры, `contentScale`, отступы, обводка или тень спрайта, фон кнопок
и слотов; пунктир облачка (`drawBehind` → `drawWithContent` и иное); указатель ☝️; `Counter`; `PetSprite`, жест питомца,
звуки; анимации (полёт — 1b5); любые строки; картинки выпечки вне раунда.

## SCOPE
variant: game (только UI)
allow: finny-pet/app/src/game/java/ru/finny/pet/game/ui/TownUi.kt,
  finny-pet/app/src/game/java/ru/finny/pet/game/screens/TrayScreen.kt,
  finny-pet/app/src/game/java/ru/finny/pet/game/GameViewModel.kt (только § 5, одна строка :184)
protect: как в `.claude/task-scope.json` 1b1-2 (базовый + `finny-pet/docs/`, `…/src/main/…` поимённо, `…/classic/`,
  `…/game/res/`, `…/game/mock/`, `…/game/audio/`, `finny-pet/tools/…`, корневые `tools/` поимённо — сверено с `ls tools/`:
  adbui.sh, art_check.py, bakery_states.sh, content_map_events.py, emu.sh, line_close.py, mock_bakery.py,
  mutation_probe.py, perf.sh, rec.sh, result_geom.py, sheets.py, town_route.sh, ui_measure.py; `wt/`), но:
  TrayScreen.kt и GameViewModel.kt — из protect в allow; allow 1b1-2 кроме TownUi.kt (GameApp.kt, ui/Widgets.kt, screens/RoundScreen.kt,
  screens/StreetScreen.kt) — в protect; + `finny-pet/app/src/game/java/ru/finny/pet/MainActivity.kt`,
  + `finny-pet/app/src/game/AndroidManifest.xml` (манифест варианта; в protect 1b1-2 только `main/AndroidManifest.xml`,
  shell-запись в манифест `game` guard пропускал — зонд хука exit 0), + `finny-pet/gradlew.bat` (guard ловит его и так —
  подстрокой `finny-pet/gradlew`; запись нужна, чтобы `git status` в assert-oracle-intact.js видел правку обёртки).
  Итог: в protect поимённо все файлы `game/` (`java/` и `AndroidManifest.xml`), кроме двух из allow. task-scope пишется
  после коммита спеки и зонда (ДО СПАВНА п. 6); пересечение с allow — пусто (одна строка из WORKFLOW № 23).

## ANTI-SCOPE
- № 65 а — круассан в лапах, `eatProp`, `PetAct.EAT` на итоге, MUNCH (BACKLOG п. 16);
- № 67 б — спрайт прилавка `counter_bakery`, правка `Counter` (:113-123), цвета `Counter*` (:100-104), `place.py
  --counter`, `to_webp.py --trim` (BACKLOG п. 17; в feat/town не сливаются); № 68 (снят);
- 1b5 — полёт изделия с витрины на поднос;
- картинки выпечки вне раунда (итог смены, экран заказа «Новинка — …», «Дневник») — вопрос № 89;
- пунктир поверх картинки (`drawWithContent`) — не делается: спрайт закрывает ≤ 4 % кольца (замер выше); по находке
  судей — вопрос № 90;
- правки `bakery_states.sh` / `town_route.sh` и новый зонд в `tools/` — не делаются: эффект держат конструкция (греп
  вызовов п. 7 + `art_check.py pastries` + цикл `aapt2` п. 11, WORKFLOW № 17) и зонды сессии п. 9 (`$T/b4_tb.py`,
  `$T/b4_pic.py` — scratch оркестратора, по снимкам и дампам `DUMP=1`, в репозиторий не входят);
- `content.json`, поле `emoji` (обязательно, запасной рисунок); домен, ViewModel, тесты (оракул не нужен — § ORACLE);
- ресурсы, LICENSES, `tools/`, доки — оркестратор; `perf.sh` (6 спрайтов по 256 px — как товары лавки, не фон);
  новые зависимости.

## БЮДЖЕТ
≤ 30 вставок без строк import (TOWN-J1-1b.md:43, :46); ожидается 14: TownUi 10 (KDoc, сигнатура, 6 веток, `else`, `}`;
пустая строка не считается), TrayScreen 3, GameViewModel 1 (§ 5). Удалений без import — ровно 4 (три старых вызова и
строка `back()`), в TownUi.kt — 0. Импортов
+1 точечный. Новых файлов 0; зависимостей 0. Счёт — `git diff -w BASE -- app/src | grep '^+[^+]' | grep -vc '^+import '`
и то же с `'^-[^-]'` / `'^-import '`.

## ORACLE
Не нужен. (1) `pastryRes` живёт в `game/` и возвращает `R.drawable`, а `app/src/test/` общий для обоих вариантов
(`testClassicDebugUnitTest` компилирует те же файлы): в тестах нет ни одной ссылки на `ru.finny.pet.game` или `R.drawable`
(`grep -rln 'ru.finny.pet.game\|R\.drawable' finny-pet/app/src/test` → пусто), набора `src/testGame/` нет (`ls app/src` —
classic, game, main, test); тест потребовал бы нового набора исходников в gradle (protect) ради `when` из 7 строк.
(2) Правило эпика: «Соглашение живёт в `game/`: `main/`, схема и `content.json` не меняются, test-author не нужен»
(TOWN-A1.md:73) — как у `itemRes`, `residentRes`. (3) Связку «id меню = ветки = файлы» держит зонд `art_check.py pastries`
с самопроверкой (ДО СПАВНА п. 2), попадание в APK — цикл `aapt2` (п. 11) и lint `UnusedResources` 0 (п. 4), TalkBack —
значимые узлы дампа (п. 9 а), эффект на экране — пиксельный зонд с положительным контролем на настоящей сборке
(п. 9 б, ДО СПАВНА п. 5), снимки и судьи (п. 12–13). Домен не меняется — тестов 600, как на BASE. `pastries` — проверка
набора арта, а не закон контента: новое изделие без спрайта законно рисуется emoji, зонд тогда напоминает (MISMATCH) о
строке и WebP.

## ДО СПАВНА (оркестратор)
`$T` — каталог scratch сессии: `T="$(cygpath -m "<scratchpad сессии>")/1b4"; mkdir -p "$T"`. Состояние оболочки между
вызовами не сохраняется — эту строку ставить в начало КАЖДОГО вызова, где есть `$T` (здесь и в ACCEPTANCE). В п. 7
ACCEPTANCE файлы кода — `TS`, `TU`, не `T`.
0. **Сделано** (проверено 2026-09-29): 1b1-2 — 70928df; ассеты — 47f5001 = BASE: `cmp` 6 × same с `$TEMP/s12/j1b2/webp/`
   (`bbox` 6 × OK — § Ассеты, те же байты), в каталоге 58 файлов, LICENSES.md:55 (строка выпечки, формулировка коммита)
   и :59 («58 файлов»), ARCHITECTURE.md:28 («выпечка (58 WebP)»), TOWN-A1.md:61 (факт доли J1 — 33 594 Б) и :72 (имена
   `pastry_<menuId>`). Проверка перед спавном: `git status --porcelain -uall -- finny-pet/app/src` → пусто;
   `git diff --stat 47f5001 HEAD -- finny-pet/app/src` → пусто; `ls finny-pet/app/src/game/res/drawable-nodpi | wc -l`
   → 58; `git grep -nE "5[28] (файл|WebP)" -- finny-pet/docs` → ровно 2 строки, обе с 58: ARCHITECTURE.md:28 и
   LICENSES.md:59.
1. **Замеры BASE** (коммита нет):
   - release BASE: `./gradlew assembleGameRelease` → копия `$T/b4_base_release.apk`, размер — в журнал; цикл п. 11 на нём →
     **6 × FAIL** (ссылок нет — R8 убрал; без этого красного п. 11 не засчитывается);
   - lint BASE: game — без сетевых 11 (5 + 6 `UnusedResources` `R.drawable.pastry_*`, замер критика на копии 47f5001),
     `grep -c 'UnusedResources' app/build/reports/lint-results-gameDebug.xml` → 6; classic — замерить (ожидается 5);
     оба числа — в журнал.
2. **Зонд `python tools/art_check.py pastries [--selfcheck]`** (правка оркестратора, коммит «tools(TOWN-J1-1b4): …»
   вместе со спекой):
   - разбор — ОБЩИЙ с `residents`: тело `check(text)` (art_check.py:349-362) выносится в функцию с параметрами (имя
     функции, префикс ресурса, множество id, множество файлов), без копии кода; `residents` вызывает её как раньше; единственная правка тела — `br` = id слева во всех
     ветках (было — только верные пары; тогда `not bad` следовал из прочих конъюнктов, мета-проверка № 44 давала 4/5);
     в вывод добавляется причина задвоения: `dup = sorted({a for a in br if pairs.count((a, a)) > 1})`, поле «дубли {dup}»
     (условие `len(pairs) == len(ids)` ловит только задвоенную ветку, а причины в выводе не было — № 37);
   - `pastries`: id — `{p.id | job ∈ town.jobs, job.game == "TRAY", p ∈ job.menu}` (сейчас 6); ветки — id слева во всех парах `"<id>" ->
     R.drawable.pastry_<x>` (пара с x ≠ id — «чужой ресурс») в теле `fun pastryRes(id: String): Int? = when (id) {…\n}` TownUi.kt без `//` и `/* */`;
     файлы — `pastry_*.webp` в `game/res/drawable-nodpi`; вывод `id 6, ветки 6, файлы 6, … дубли [] -> MATCH 6` ⇔ id =
     ветки = файлы, чужих ресурсов нет, пар столько же, сколько id, последняя ветка `else -> null`; иначе `MISMATCH` с
     перечнем; exit 0/1;
   - `--selfcheck` не читает TownUi.kt (работает до кодера, № 14): эталонный текст `pastryRes` строится из id
     `content.json` в виде § 1 → `MATCH`; 10 мутантов → `MISMATCH`, каждый **со своей причиной в выводе** (№ 37): (1) без
     ветки croissant → «нет ветки ['croissant']»; (2) croissant → `R.drawable.pastry_bread` → «чужой ресурс
     [('croissant', 'bread')]»; (3) ветка croissant в `/* */` и (4) в `//` → «нет ветки ['croissant']»; (5) `else ->
     R.drawable.pastry_bread` → «else -> null False»; (6) файлы без cupcake → «нет файла ['cupcake']»; (7) меню +
     «eclair» → «нет ветки ['eclair']»; (8) ветка croissant дважды → «дубли ['croissant']»; (9) меню TRAY пусто и файлов
     нет → «id 0 … MISMATCH» (конъюнкт `bool(ids)`); (10) строка bread заменена строкой croissant → «нет ветки ['bread']» (часть `ids == br`,
     находка критика зонда). `SELFCHECK OK` — только если все 11 (эталон + 10) дали ожидаемое,
     иначе `SELFCHECK FAIL` с номерами;
   - проверка самой самопроверки (№ 44, критик по коду зонда до коммита): в scratch-копии удалить по одному каждый из
     5 конъюнктов `ok` (`bool(ids)`, `ids == br == files`, `not bad`, `len(pairs) == len(ids)`, `tail`) — каждое удаление
     обязано уронить `pastries --selfcheck` в `SELFCHECK FAIL`; итог — в журнал;
   - на BASE: `pastries` → `MISMATCH` (функции нет, exit 1), `pastries --selfcheck` → `SELFCHECK OK`; регресс общего
     разбора: `residents` → `MATCH 9`, `residents --selfcheck` → `SELFCHECK OK` (как на HEAD; в выводе добавится
     «дубли []» — сверка с HEAD по вердикту, а не по строке целиком); строка в docstring art_check.py.
3. **Скрипты п. 9** — `$T/b4_tb.py` (TalkBack) и `$T/b4_pic.py` (пиксели) лежат в scratch с 2026-09-29 и проверены
   (журнал); перед п. 4 сверить с блоками п. 9.
4. **«До» на эмуляторе** (AVD `finni`, обычный запуск; debug BASE через `adb install -r "$(cygpath -w …)"`, `dumpsys
   package ru.finny.pet | grep pkgFlags` — `DEBUGGABLE` в лог, № 33; `tools/adbui.sh wm360`, `mute`; `ps -ef | grep -E
   "town_route|bakery_states"` пусто, № 40):
   - отпечаток профиля в журнал: `tools/adbui.sh exec-out run-as ru.finny.pet cat files/state.json | md5sum`;
   - `DUMP=1 tools/bakery_states.sh b4b riddle big 2>&1 | tee "$T/b4b_states.log"` → 11 кадров со снимками и дампами
     `finny-pet/screenshots/emu_b4b_*` (riddle_job10/13, riddle_round10/13, riddle10/13, big_almost, big_riddle_idle,
     big_hint_idle, big_wrong10/13); `grep -cE "not found|BACKUP" "$T/b4b_states.log"` → 0; `profile restored` в логе;
   - отрицательные контроли (№ 14, № 27): `python "$T/b4_pic.py" finny-pet/screenshots b4b 3` → `5 frames, 5 FAIL`,
     exit 1 (emoji ниже порога — зонд краснеет, когда эффекта нет); копия `emu_b4m_*.xml` из `b4b`, где
     `content-desc="Круассан"` → `"Круассан, Круассан"` (так читался бы спрайт с описанием внутри кнопки), →
     `python "$T/b4_tb.py" finny-pet/screenshots b4b b4m` → FAIL с этим узлом, exit 1. Итоги — в журнал.
5. **Положительный контроль на настоящей сборке** (№ 14, № 44; без него п. 9 не засчитывается):
   `git worktree add --detach "$T/b4p_wt" HEAD`, копия `finny-pet/local.properties`; в клоне — § 1–2 дословно;
   `./gradlew assembleGameDebug` (из `$T/b4p_wt/finny-pet`) → установить (`pkgFlags` — `DEBUGGABLE`); отпечаток профиля =
   п. 4; из основного дерева `DUMP=1 tools/bakery_states.sh b4p riddle big 2>&1 | tee "$T/b4p_states.log"` (лог — как
   п. 4); `python "$T/b4_tb.py" finny-pet/screenshots b4b b4p` → `11 frames, 0 FAIL`; `python "$T/b4_pic.py"
   finny-pet/screenshots b4p 3` → `5 frames, 0 FAIL`; затем вернуть debug BASE, `git worktree remove --force
   "$T/b4p_wt"`. Итоги и доли по коробкам — в журнал. Красный на верной реализации — чинится зонд или спека
   (оркестратор), а не кодер.
6. task-scope (base = коммит спеки и зонда; allow/protect — SCOPE); пересечение — пусто.

## ACCEPTANCE (команды 1–8 и 10 — из finny-pet/, 9 и 11–13 — из корня; гоняет оркестратор; `$T` — шапка ДО СПАВНА)
1. ./gradlew testClassicDebugUnitTest --rerun --console=plain -> exit 0; tests = 600, failed 0, skipped 0 (по XML, пресет)
2. ./gradlew testGameDebugUnitTest --rerun --console=plain -> exit 0; 600 / 0 / 0
3. ./gradlew assembleClassicDebug assembleGameDebug assembleGameRelease -> exit 0
4. ./gradlew lintClassicDebug lintGameDebug -> ошибок 0; без сетевых по правилу пресета (ACCEPTANCE_PRESETS.md:51-53):
   game ≤ 5 (на BASE 11 = 5 + 6 `UnusedResources` `pastry_*`), classic ≤ BASE classic (ДО СПАВНА п. 1);
   `grep -c 'UnusedResources' app/build/reports/lint-results-gameDebug.xml` -> 0 (на BASE 6)
5. git diff --name-only BASE -- app/src/main/ app/src/classic/ app/src/test/ app/src/game/res/ '*.gradle.kts' gradle/ gradle.properties gradlew gradlew.bat -> пусто
6. git status --porcelain -uall -- app/src -> только пути allow; из корня `node .claude/hooks/assert-oracle-intact.js` -> exit 0;
   `git diff -w BASE -- app/src | grep '^+[^+]' | grep -vc '^+import '` -> ≤ 30;
   `git diff -w BASE -- app/src | grep '^-[^-]' | grep -vc '^-import '` -> 4 (три вызова TrayScreen и строка `back()` § 5);
   `git diff -w --numstat BASE -- app/src/game/java/ru/finny/pet/game/ui/TownUi.kt` -> удалений 0
7. греп по коду без строк import (№ 45); `TS=app/src/game/java/ru/finny/pet/game/screens/TrayScreen.kt;
   TU=app/src/game/java/ru/finny/pet/game/ui/TownUi.kt`:
   `grep -v '^import' $TS | grep -c 'Pic(null, emoji('` -> 0 (на BASE 3);
   `grep -v '^import' $TS | grep -cF 'Pic(pastryRes(id), emoji(id), 56.dp)'` -> 1;
   `grep -v '^import' $TS | grep -cF 'Pic(pastryRes(item), emoji(item), 40.dp)'` -> 1;
   `grep -v '^import' $TS | grep -cF 'Pic(pastryRes(id), emoji(id), s)'` -> 1 (на BASE все три 0);
   `grep -v '^import' $TS | grep -cF 'Pic(null, "☝️", 40.dp'` -> 1 (указатель не тронут);
   `grep -v '^import' $TS | grep -cF 'drawBehind { if (gap) dashedCircle(G.purple, 3.dp) }'` -> 1 (пунктир не тронут);
   `grep -cx 'import ru.finny.pet.game.ui.pastryRes' $TS` -> 1; `grep -c '^import ru\.finny\.pet\.game\.ui\.\*' $TS` -> 0;
   `grep -rn 'fun pastryRes' app/src` -> одна строка, в $TU: `fun pastryRes(id: String): Int? = when (id) {`;
   `grep -rn 'getIdentifier' app/src` -> пусто;
   из корня: `python tools/art_check.py pastries` -> `MATCH 6`, exit 0; `… pastries --selfcheck` -> `SELFCHECK OK`;
   `… residents` -> `MATCH 9`; `… residents --selfcheck` -> `SELFCHECK OK`
8. новые литералы как разность множеств:
   `comm -23 <(git diff -w BASE -- app/src | grep '^+[^+]' | grep -oE '"[^"]*"' | sort -u) <(git diff -w BASE -- app/src | grep '^-[^-]' | grep -oE '"[^"]*"' | sort -u)`
   -> ровно 6 строк: `"baguette" "bread" "croissant" "cupcake" "donut" "pretzel"` (ключи `when`; строк ребёнку нет);
   `git diff -w BASE -- app/src | grep '^+[^+]' | grep -cE 'contentDescription|semantics|stateDescription'` -> 0
9. TalkBack тот же и эффект есть (машинно, после п. 12 а; скрипты — `$T`, ДО СПАВНА п. 3, положительный контроль —
   ДО СПАВНА п. 5). Отпечаток профиля перед прогоном `b4` (п. 12 а) равен отпечатку перед `b4b` (ДО СПАВНА п. 4) — иначе
   а) на этой паре не засчитывается и снимается пара заново (см. ниже).
   а) `python "$T/b4_tb.py" finny-pet/screenshots b4b b4` -> `11 frames, 0 FAIL`, exit 0 (скрипт — ровно этот блок):
   ```python
   # TOWN-J1-1b4 п. 9: значимые узлы TalkBack (текст, описание, clickable, focusable, bounds) на тех же кадрах — те же, что на BASE
   import re, sys, glob, os
   from collections import Counter
   d, a, b = sys.argv[1], sys.argv[2], sys.argv[3]  # каталог дампов, префикс BASE, префикс 1b4
   R = re.compile(r'<node [^>]*?text="([^"]*)"[^>]*?content-desc="([^"]*)"[^>]*?clickable="(\w+)"[^>]*?focusable="(\w+)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
   N = lambda p: Counter(n for n in R.findall(open(p, encoding="utf-8").read()) if n[0] or n[1] or "true" in n[2:4]) if os.path.exists(p) else Counter()
   fr = sorted(os.path.basename(p)[len(f"emu_{a}_"):-4] for p in glob.glob(f"{d}/emu_{a}_*.xml"))
   bad = 0
   for f in fr:
       x, y = N(f"{d}/emu_{a}_{f}.xml"), N(f"{d}/emu_{b}_{f}.xml")
       ok = x == y and sum(x.values()) > 0; bad += not ok
       print(f, f"OK sig {sum(x.values())}" if ok else f"FAIL sig {sum(x.values())}/{sum(y.values())} -{list((x - y).elements())[:2]} +{list((y - x).elements())[:2]}")
   print(f"{len(fr)} frames, {bad} FAIL")
   sys.exit(1 if bad or not fr else 0)
   ```
   Пустые узлы не сравниваются: у `Image` без описания свой пустой узел (КОНТЕКСТ) — дамп эффект не различает.
   б) `python "$T/b4_pic.py" finny-pet/screenshots b4 3` (3 — px на dp у `wm360`) -> `5 frames, 0 FAIL, boxes N`, N > 0,
   exit 0 (скрипт — ровно этот блок; запуск из корня — пути к `content.json` и WebP относительные):
   ```python
   # TOWN-J1-1b4 п. 9 б: на месте картинки изделия (витрина 56, слот 40, облачко 48/40 dp) — спрайт этого изделия, а не emoji
   import re, sys, json, numpy as np
   from PIL import Image
   d, a, px = sys.argv[1], sys.argv[2], float(sys.argv[3])  # каталог снимков и дампов, префикс, px на dp
   F = {"riddle_round10": "show bubble", "riddle_round13": "show bubble", "big_almost": "show tray bubble", "big_wrong10": "show tray bubble", "big_wrong13": "show tray bubble"}
   menu = [p for j in json.load(open("finny-pet/app/src/main/assets/content/content.json", encoding="utf-8"))["town"]["jobs"] if j.get("game") == "TRAY" for p in j["menu"]]
   T2ID, L2ID = {p["title"]: p["id"] for p in menu}, {p["title"].lower(): p["id"] for p in menu}
   SPR = {i: Image.open(f"finny-pet/app/src/game/res/drawable-nodpi/pastry_{i}.webp").convert("RGBA") for i in T2ID.values()}
   def boxes(xml):  # (вид, id, x, y, сторона dp) — по значимым узлам: кнопка витрины, «… на подносе», облачко «Имя. Заказ: …»
       for n in re.findall(r"<node [^>]*>", xml):
           desc = re.search(r'content-desc="([^"]*)"', n)[1]; b = list(map(int, re.findall(r"\d+", re.search(r'bounds="([^"]*)"', n)[1])))
           cx, cy = (b[0] + b[2]) / 2, (b[1] + b[3]) / 2
           if desc in T2ID: yield "show", T2ID[desc], round(cx - 28 * px), round(cy - 28 * px), 56
           elif desc.endswith(" на подносе") and desc[:-11] in T2ID: yield "tray", T2ID[desc[:-11]], round(cx - 20 * px), round(cy - 20 * px), 40
           elif (m := re.match(r"^[^.]+\. Заказ: (.+?)(\. Ещё нужно: .*)?$", desc)) and b[2] - b[0] >= 48 * px:
               its = m[1].split(", "); s = 48 if len(its) <= 3 else 40
               for k, t in enumerate(its): yield "bubble", L2ID[t], round(b[0] + (8 + k * (s + 4)) * px), round(b[1] + 8 * px), s
   def frac(img, x, y, s, pid, check):  # доля пикселей маски спрайта (α > 200), совпавших с кадром (max |RGB| ≤ 40), лучший сдвиг ±4 px
       q = round(s * px); t = np.asarray(SPR[pid].resize((q, q), Image.BILINEAR), dtype=float); m = t[..., 3] > 200
       if check: m[int(q - 20 * px):, int(q - 20 * px):] = False  # угол ✓ облачка
       return max((np.abs(np.asarray(img.crop((x + dx, y + dy, x + dx + q, y + dy + q)), dtype=float) - t[..., :3]).max(-1)[m] <= 40).mean() for dy in range(-4, 5, 2) for dx in range(-4, 5, 2))
   bad = tot = 0
   for f, need in F.items():
       img = Image.open(f"{d}/emu_{a}_{f}.png").convert("RGB"); bx = list(boxes(open(f"{d}/emu_{a}_{f}.xml", encoding="utf-8").read()))
       miss = [k for k in need.split() if k not in {b[0] for b in bx}]
       low = [f"{k}:{i}={v:.2f}" for k, i, x, y, s in bx if (v := frac(img, x, y, s, i, k == "bubble")) < 0.60]
       bad += bool(miss or low); tot += len(bx)
       print(f, f"FAIL no {miss} low {low[:4]}" if miss or low else "OK " + " ".join(f"{k} {sum(b[0] == k for b in bx)}" for k in need.split()))
   print(f"{len(F)} frames, {bad} FAIL, boxes {tot}")
   sys.exit(1 if bad or not tot else 0)
   ```
   Смысл: на месте каждой картинки изделия (коробки — по значимым узлам и раскладке BASE: квадрат 56 dp по центру кнопки
   витрины, 40 dp по центру слота, в облачке `8 + k·(s + 4)` dp от края, TrayScreen.kt:202-206, :336-340, :373-384) —
   спрайт ЭТОГО изделия: доля пикселей его маски, совпавших с кадром, ≥ 0,60 (журнал: emoji BASE ≤ 0,47, чужой спрайт
   ≤ 0,43, спрайт под указателем ☝️ ≥ 0,79, с ✓ ≥ 0,95); вида без коробок на кадре — FAIL `no` (№ 14). Размер зонд не
   меряет — греп п. 7.
   FAIL в а) или б) — повтор ПАРЫ подряд: debug BASE → `DUMP=1 tools/bakery_states.sh b4b riddle big`, debug 1b4 →
   `… b4 riddle big`, отпечаток профиля перед каждым прогоном, всё — в журнал (профиль у пары одинаков по построению:
   скрипт возвращает его с `cmp`); повторился — FAIL с напечатанными узлами и коробками.
10. стоп-слова в литералах диффа (команда TOWN-J1-1a2.md:116-117) -> 0
11. release `game` — R8 оставил 6 спрайтов (из корня; образец — TOWN-A1f.md:187-195):
    ```
    APK=finny-pet/app/build/outputs/apk/game/release/app-game-release.apk
    D=$("$LOCALAPPDATA/Android/Sdk/build-tools/36.0.0/aapt2.exe" dump resources $APK)
    for n in croissant bread baguette pretzel donut cupcake; do
      p=$(echo "$D" | grep -A1 "drawable/pastry_$n\$" | grep -o 'res/[^ ]*')
      s=$([ -n "$p" ] && unzip -l $APK "$p" | awk 'NR==4{print $1}'); w=$(stat -c%s finny-pet/app/src/game/res/drawable-nodpi/pastry_$n.webp)
      [ -n "$p" ] && [ "$s" = "$w" ] && echo "pastry_$n OK $p $s" || echo "pastry_$n FAIL path=$p size=$s src=$w"
    done
    ```
    -> 6 × OK (на release BASE — 6 × FAIL, ДО СПАВНА п. 1). Прирост APK к `$T/b4_base_release.apk` ≤ 33 594 + 6 000 =
    39 594 Б; в отчёт — прирост, доля J1 (33 594 из ≤ 70 000, TOWN-A1.md:60-61) и остаток бюджета эпика
    = 4 081 493 + 1 048 576 − размер APK.
12. живая проверка — эмулятор (AVD `finni`, обычный запуск), debug п. 3 (`cygpath -w`, `pkgFlags` с `DEBUGGABLE` в лог —
    № 33), `tools/adbui.sh wm360`, `mute`; шрифт 1,0 и 1,3 скрипты ставят сами; перед прогоном `ps -ef | grep -E
    "town_route|bakery_states"` пусто (№ 40):
    а) отпечаток профиля (ДО СПАВНА п. 4) — в журнал; `DUMP=1 tools/bakery_states.sh b4 riddle big 2>&1 | tee
       "$T/b4_states.log"`: `grep -cE "not found|BACKUP" "$T/b4_states.log"` -> 0; `grep -cE "Обрезано: [1-9]|^[A-Za-z0-9_]+:
       *$" "$T/b4_states.log"` -> 0 (пустой ответ зонда обрезки — тоже красный); `profile restored` в логе;
       `python tools/ui_measure.py finny-pet/screenshots/emu_b4_<кадр>.xml <кадр> 3` по 11 дампам — кликабельных < 48 dp
       нет, узлов за краем нет;
    б) `tools/town_route.sh b4r 2>&1 | tee "$T/b4r_route.log"` (регресс 1b1-2 и указатель ☝️ первой смены поверх спрайта;
       профиль скрипт не возвращает — после п. 9 или перед его повтором не гонять):
       `grep -cE "not found|gate not passed|GEOM FAIL|CLOSE FAIL" "$T/b4r_route.log"` -> 0 (№ 32);
       `grep -c "GEOM OK" "$T/b4r_route.log"` -> 8; `grep -c "CLOSE OK" "$T/b4r_route.log"` -> 2; «Обрезано» — как в а);
    в) глазами: `emu_b4_riddle_round10/13` (4 вида, облачко 48 dp), `big_almost` (6 видов на витрине, 56 dp; поднос 2 из 3 и
       пустой пунктирный слот 40 dp; облачко 48 dp — заказ из 3, 2 вида), `big_wrong10/13` (облачко 40 dp — заказ из 4,
       3 вида, ✓ и пунктир; на подносе одно верное изделие, слоты 40 dp), `emu_b4r_round10` (указатель): на витрине, на
       подносе и в облачке — спрайты, emoji выпечки нет; спрайт не упирается в края кнопки и круга подноса; пунктир
       пропусков читается вокруг спрайта, ✓ на месте; указатель ☝️ поверх спрайта (рисуется позже, :222-229);
    г) S23 спит и заблокирован — **«S23 доснять при владельце»** (GATE_QUEUE, раздел «1b4»): а) и б) на S23
       (`ANDROID_SERIAL`, плотность — из `wm density`; разблокирует владелец; громкость и шрифт до и после; профиль —
       бэкап и `cmp`, `town_route.sh` стирает прогресс — HANDOFF «Окружение»). Отступление — в журнал.
13. лист `finny-pet/screenshots/town/j1b4_gate.jpg` (`tools/sheets.py`; заголовок листа — argv[2] — с пометкой «S23 —
    доснять»): ряд «до» — `emu_b4b_*` (BASE, emoji), ряд «после» — `emu_b4_*` на тех же кадрах: `riddle_round10`,
    `riddle_round13`, `big_almost`, `big_wrong10`, `big_wrong13`; третий ряд — `emu_b4r_round10` (указатель) с подписью
    «… S23 — доснять» (пустой ячейки в sheets.py нет — :48 открывает каждое имя). Судьи по кадрам «после» (одним
    workflow):
    - **«ребёнок» вслепую** (№ 35): вырезки с живых снимков в настоящем размере — 56 dp: витрина `big_almost` (6 видов);
      48 dp: облачко `riddle_round10` (в демо — 1 изделие) и облачко `big_almost` (заказ из 3, 2 вида); 40 dp: облачко и
      слот подноса `big_wrong10` (заказ из 4, 3 вида; вид слота — из облачка) и слоты подноса `big_almost` (виды — из его
      облачка). Без подписей, с нейтральными метками, вперемешку с вырезками «до» (emoji) и двумя приманками того же
      размера (`item_fun_ball`, `item_care_shampoo`). Ключ — у оркестратора, собирается машинно из дампов
      `emu_b4_{riddle_round10,big_almost,big_wrong10}.xml` (облачко — `content-desc` «{Имя}. Заказ: …[. Ещё нужно: …]»,
      слоты — «X на подносе», кнопки — названия): у каждой вырезки размер и вид; до прогона в ключ записаны допустимые
      названия (круассан / рогалик; багет / батон; хлеб / буханка; кекс / капкейк / пирожное; пончик; крендель /
      брецель; «булочка» и «хлеб» для нехлебных изделий не засчитываются). Вопросы: «Что это за еда?»; по `big_almost`
      без подписей — «Какие кнопки внизу нажать, чтобы собрать то, что просит покупатель?»; по `big_wrong10` — «Что ещё
      нужно покупателю? Что он уже получил?». Критерий:
      - 56 dp — ≥ 5 из 6 видов названы верно;
      - 48 dp и 40 dp — каждый вид, который есть на живых вырезках, назван верно, ошибок 0 (перечень и число видов — из
        ключа; «из 6» не фиксируется: число видов в кадре задают код и сид профиля, КОНТЕКСТ);
      - `big_almost` — для обоих видов облачка названа верная кнопка витрины и названо недостающее изделие;
      - `big_wrong10` — названы все виды из «Ещё нужно» (сверка с `content-desc` облачка), изделие с ✓ названо полученным,
        пунктир не принят за «уже есть»; на вырезке «до» ответ тот же;
      - `riddle_round10` в соответствия «облачко → кнопка» не входит;
      - самопроверка линзы: любую приманку назвал едой или напитком (а не «не еда / мяч / флакон») — ответы линзы не
        засчитываются, линза — заново;
    - **«доступность»**: серый (luma) и дейтеранопия (numpy по `big_almost`, `big_wrong10`): 6 силуэтов витрины различимы
      (кекс и круассан одного тона — различает форма); ✓ и пунктир облачка читаются без цвета и поверх/вокруг спрайта
      (замер: спрайт закрывает ≤ 4 % кольца); кромка спрайта на белом при 40 dp (остаточный риск 1b3-2: кольцо 1 px у
      кекса 2,84 : 1 на 48 dp); TalkBack — п. 9 а. Виды в пунктире и под ✓ на `big_wrong10` (из `content-desc` облачка) и
      долю ✓ для вида под ним (§ Ассеты) оркестратор пишет в журнал: худшие случаи (крендель в пунктире, кекс на 40 dp)
      в кадр могут не попасть — принято по замеру и № 66 а, синтетику не собираем;
    - **«сцена и владелец»**: спрайты в одном мире с фоном пекарни и жителями; спрайт шире прежнего emoji (рамка 0,72–0,90
      коробки против 0,66–0,67, радиус 0,77–1,03 полуразмера против 0,70–0,91, площадь та же ± 8 % — § Ассеты) — не
      упирается ли изделие в край кнопки 64–88 dp и белого круга подноса 48 dp (крендель 0,90 × 40 dp = 36 dp);
      указатель ☝️ читается поверх спрайта.
    Ворота — «посмотреть» (№ 66 а решён). Вопросы владельцу — только по находкам судей и № 89, пачкой в
    `docs/tasks/GATE_QUEUE.md` (раздел «1b4»: лист, выжимка судей, рекомендации, «S23 доснять»). Без находок — строка
    «1b4: лист `j1b4_gate.jpg`, судьи без находок, рекомендация — принять». Кодер вид сам не меняет (§ 4).

14. **§ 5 «Назад» на итоге** (эмулятор, debug, шрифт 1,0; из корня): `grep -c 'else { lines.clear(); closeRound() }'
   finny-pet/app/src/game/java/ru/finny/pet/game/GameViewModel.kt` -> 1 (на BASE 0); `git diff -w BASE --
   finny-pet/app/src/game/java/ru/finny/pet/game/GameViewModel.kt | grep -c '^[-+][^-+]'` -> 2. Живо: пекарня →
   «Начать смену» → смена до итога → «Почему?» (в дампе есть узел resource-id `line`) → `tools/adbui.sh shell input
   keyevent 4` → свежий дамп: узла `line` нет, есть «Начать смену» или «Заказ: » экрана пекарни. Самопроверка — на
   сборке BASE (п. 4 ДО СПАВНА, профиль тот же): после «Назад» узел `line` ЕСТЬ (красный) — в журнал. Профиль — бэкап и
   `cmp`, как в `bakery_states.sh`.

## ЖИВАЯ ПРОВЕРКА (человек: docs/BACKLOG.md п. 1 и 11)
S23 при владельце: снимки п. 12 г. TalkBack в раунде пекарни: кнопка витрины — «Круассан, кнопка, Положить на поднос»
одной остановкой, после касания — «на подносе: 1»; слот — «Круассан на подносе, Убрать»; облачко покупателя — одна фраза
«{Имя}. Заказ: …»; картинка отдельно не озвучивается — как до 1b4. Узнаёт ли ребёнок изделия на телефоне — плейтест 1
(GAME_CONCEPT §17.4).

## ВОПРОСЫ НА ВОРОТА 1b4 (в GATE_QUEUE пачкой с листом `j1b4_gate.jpg`; ответ — «№ 89 а, …»)
- **№ 89. Выпечка картинкой вне раунда.** Сейчас изделие видно спрайтом только в раунде; на экране заказа новинка — строкой
  «Новинка — крендель!» (`steps[].intro`), на итоге смены и в «Дневнике» изделий нет. а) только раунд (как решено ответами
  № 65–67), строка BACKLOG «новинка картинкой на экране заказа» — к плейтесту; б) + спрайт новинки рядом с репликой на
  экране заказа — отдельная задача после 1b5 (≈ 5–8 вставок PlaceScreen + строка `pastryRes` уже есть); в) другое.
  **Рекомендую а**: новинка и так появляется спрайтом на витрине в первом же раунде; 1b5 (полёт, № 58) важнее и под
  риском по сроку; решать по плейтесту, читает ли ребёнок реплику.
- **№ 90 (только если судья «доступность» или «ребёнок» найдёт, что пунктир «не хватает» теряется на спрайте).** а) кольцо
  поверх изделия (`drawBehind` → `drawWithContent { drawContent(); if (gap) … }`, 1 строка TrayScreen.kt:383);
  б) недостающее изделие бледнее (α 0,5) — второй признак помимо формы; в) оставить как есть. **Рекомендация — а**
  (одна строка, форма не меняется, цвет не единственный признак сохраняется); без находки вопрос не задаётся (замер —
  спрайт закрывает ≤ 4 % кольца).
- Не вопрос, а пункт «доснять»: S23 при владельце — п. 12 г (разблокировка, громкость, шрифт, профиль).

## ДОКИ (оркестратор, после приёмки, до ревью; готовить в scratch; `git grep` по обоим `docs/` и `tools/`)
Грепы: `выпечка — emoji`, `emoji-заглушк|эмодзи-заглушк`, `спрайты выпечки — дальше`, `прилавок-оверлей|6 спрайтов
выпечки`, `emoji|эмодзи` рядом с `выпеч|изделия|пекарн|поднос|витрин|облачк`, `itemRes`, `pastryRes`, `pastry_`, `1b4`;
`52 файла|52 WebP` — по `-- finny-pet/docs docs ':!docs/tasks'`: до обновления HANDOFF — только docs/HANDOFF.md:64, после —
0 (в finny-pet/docs 0 с 47f5001; в docs/tasks/* 5 строк — история прошлых спек, не правятся). Править то, что стало ложью
(строки — рабочее дерево):
- GAME_CONCEPT: §0 :20-21 («спрайты выпечки — дальше в срезе 1б … до них выпечка — emoji» → спрайты выпечки —
  TOWN-J1-1b4; прилавок — Compose-полоса, № 67 б); §17 строка 1б (:1853) — спрайты выпечки сделаны; запасной план
  :1856-1857 («сдаём их: выпечка — emoji») — устарел; §18 № 66 (:2097) — «реализовано TOWN-J1-1b4»; № 89–90 — после ответа.
- TOWN-J1.md :359 (строка 1б: 1b4 сделан), :361 (запасной «выпечка — emoji-заглушка»); :212, :324 — только если это
  описание сборки, а не план.
- TOWN-J1-1b.md — только строка журнала о 1b4 с отсылкой «раздел 1b4 (:548-581) заменён спекой TOWN-J1-1b4.md; п. 3–4 его
  контракта отменены ответами № 65 а, № 67 б». Тело среза не переписывать: раздел 1b4 и абзац коммита ассетов
  (:470-476, со ссылкой «TOWN-A1.md:68» — выполнен в 47f5001, номер строки — снимок того дня).
- TOWN-A1.md:61 — факт по файлам (33 594 Б) внесён в 47f5001; дописать только прирост APK по п. 11.
- finny-pet/docs: ARCHITECTURE :76 (`TownUi.kt` — картинки `itemRes`/`goalRes`/`pastryRes`), :174 (+ «изделие пекарни без
  ветки `pastryRes` рисуется своим emoji; своя картинка — WebP `pastry_<id>` и строка в `pastryRes`»), :188 (`props.py` —
  + выпечка `pastry_*`); BUILD_AND_DEMO :308 (`props.py` — + выпечка пекарни, `--only pastry_…`, `to_webp.py --size 256`);
  CONTENT_MAP :268-271 (картинку изделия `town.jobs[].menu` выбирает `pastryRes`; без строки — emoji записи);
  DATA_MODEL :245-246 (`emoji` изделия — запасной рисунок `game`); UX_ACCESSIBILITY :58-59 и :66-68 (витрина 56 dp,
  слот 40 dp, заказ 48 / 40 dp — спрайты `pastry_*`; своего описания TalkBack у картинки нет, название — у кнопки, слота,
  облачка), :126 (пунктир «не хватает» — вокруг спрайта); REQUIREMENTS_MATRIX :128 (+ спрайты выпечки, TOWN-J1-1b4,
  живая проверка); TEST_CASES TC-32 (:115) — только если шаги называют emoji (сейчас — «заказ картинками», верно).
- docs/BACKLOG.md — строка по № 89, если ответ а; docs/HANDOFF.md «Дальше»; docs/tasks/GATE_QUEUE.md — раздел «1b4».
Спеки прошлых задач и тело TOWN-J1-1b.md не трогать (в TOWN-J1-1b.md — только строка журнала выше).

## ПРИ БЛОКЕРЕ
STATUS: BLOCKED + один конкретный вопрос. Не изобретать: нет `R.drawable.pastry_*` при сборке (ассеты не на BASE), форма
вызова в § 2 не компилируется, приёмка п. 7 требует формы, которой нет в § 1–2, хочется поправить пунктир, `Pic` или
размер — вопрос, а не обход.

## Журнал спеки
- 2026-09-29 (сессия 14): два независимых черновика (угол «от кода» и угол «приёмка и живая проверка») сведены в одну
  спеку; расхождения решены по рабочему дереву, противоречия — в пользу ответов № 65 а, № 66 а, № 67 б. HEAD — 47f5001:
  во время сведения закоммичены 1b1-2 (70928df), GATE_QUEUE (b0960e8) и ассеты (47f5001); код `app/src` = 70928df.
  Строки черновиков по TrayScreen.kt (:178, :206, :340, :373, :383-390, :228, :113-123) и TownUi.kt (:94-106, :108,
  :119-126, :131-143) — подтверждены; GAME_CONCEPT и TOWN-J1.md в рабочем дереве сдвинуты (1b1-2 доки): §18 № 66 —
  :2097 (не :2094), §17 1б — :1853, запасной — :1856-1857; TOWN-J1.md 1б — :359, запасной — :361.
- Пунктир «не хватает»: черновик «от кода» предлагал `drawWithContent` (кольцо поверх), опираясь на радиус непрозрачного
  0,77–1,03 полуразмера. Замер доли площади кольца под спрайтом (α > 128, BILINEAR до 144 / 120 / 168 px при d = 3):
  48 dp — багет 0,03, крендель 0,02, круассан 0,01, хлеб, кекс, пончик 0,00; 40 dp — багет 0,04, крендель 0,04,
  круассан 0,03, прочие 0,00. Радиус достаётся только по диагоналям — кольцо читаемо на ≥ 96 %. Решение: `drawBehind` не
  трогать; вид — судьям, правка — вопрос № 90 только по находке.
- Эффект на экране: черновик «приёмка» предлагал пиксельный зонд `pastry_pic.py` в `tools/` с правкой двух скриптов
  маршрута (13 случаев самопроверки; порог 0,60 по замеру `frac.py`: вклеенный спрайт 1,00, с указателем ≥ 0,79, с ✓
  ≥ 0,95, emoji ≤ 0,42, чужой спрайт ≤ 0,43). Черновик r1 выбрал вместо него признак «пустой узел emoji исчезает, у
  `Image` без описания узла нет» и проверил его только на синтетическом «после» (из дампов `emu_s12r_*` удалены квадраты
  168 / 120 px: `7 frames, 0 FAIL, pic 22`). Посылка ложна (круг критиков, accept/F1): `Image(null)` даёт свой пустой узел
  (`clipToBounds` → `graphicsLayer` — `SemanticsModifierNode`, ui 1.11.4; дампы `IconBox` и кошелька в
  `emu_s12r_market10.xml`), модель на настоящих дампах — `7 frames, 6 FAIL`, то есть п. 9 r1 не позеленел бы на верной
  работе. Итог — п. 9: а) только значимые узлы (`b4_tb.py`), б) пиксельный зонд по `frac.py` в scratch (`b4_pic.py`,
  коробки — по значимым узлам, без правок `tools/`), положительный контроль — настоящая сборка клона (ДО СПАВНА п. 5).
- Зонд `art_check.py pastries`: разбор общий с `residents` (черновик «от кода»), самопроверка на эталонном тексте без
  TownUi.kt и с ожидаемой причиной у каждого мутанта (черновик «приёмка», № 14, № 37) — работает до кодера.
- Оракул не нужен (оба черновика): `R.drawable` только в `game`, тесты общие, `src/testGame` нет, TOWN-A1.md:73.
- Вопросы: № 89 (картинка вне раунда, рекомендация а), № 90 (условный, пунктир поверх, рекомендация а). S23 — «доснять».
  Номера: задание сведения давало «с № 85», но GATE_QUEUE (b0960e8, во время сведения) занял № 83–88 вопросами 1b1-2 —
  взяты следующие свободные.
- 2026-09-29 (сессия 14), круг критиков по трём измерениям (контракт и контур; приёмка и живая проверка; ребёнок и
  судьи), каждую находку опровергали два скептика по коду: 20 находок — подтверждено 16, частично 3, опровергнуто 1.
  Контракт (7): подтверждено 6 (C1–C6), опровергнуто 1 (C7 — адреса шапки :470-473 и :772-776, не меняются). Приёмка (6):
  подтверждено 6 (F1–F6). Ребёнок (7): подтверждено 4 (K1, K3, K5, K7), частично 3 (K2, K4, K6). Принято:
  - C1, F2, K7 — коммит ассетов 47f5001 сделан во время сведения (r1 записан на минуту позже, считал его будущим): BASE —
    по sha; ДО СПАВНА п. 0 — «сделано» с фактом; п. 1 — только release BASE и lint BASE. Строка LICENSES:55 — в
    формулировке коммита (другой порядок изделий, без `--only`), а не r1; итог «58 файлов» — LICENSES.md:59, не :58.
  - C2, F2 — греп «`52 файла|52 WebP` → пусто» по обоим `docs/` недостижим и до, и после ассетов (6 строк истории:
    HANDOFF.md:64, TOWN-A1d1.md:867, TOWN-A1f.md:91 и :267, TOWN-A1g1.md:537, TOWN-J1-1b.md:474); проверка п. 0 —
    `5[28] (файл|WebP)` по finny-pet/docs → 2 строки с 58 (положительная, а не «пусто»); в ДОКИ — диапазон без docs/tasks.
  - C3 — lint BASE game 11 = 5 + 6 `UnusedResources` `pastry_*` (замер критика на копии 47f5001; конфига lint и baseline
    нет): п. 4 — game ≤ 5 и `UnusedResources` 0. Счёт `grep -c 'pastry_'` критика отклонён: на BASE 12 (сообщение и
    location), и он ловит `errorLine1` любой будущей строки с `R.drawable.pastry_`.
  - C4 — `game/AndroidManifest.xml` в protect (shell-запись проходила guard: зонд хука exit 0; п. 6 её всё равно ловит,
    поэтому в п. 5 манифест не добавлен); `gradlew.bat` в protect — ради `git status` в assert-oracle-intact.js (guard
    ловит его и так); в п. 5 — `gradlew gradlew.bat` (вне `app/src` их не проверял ни один пункт). Замечено скептиками, вне
    1b4: из `finny-pet/` двухсегментные `gradlew` / `gradlew.bat` guard не ловит (хвостов нет, guard-paths.js:139-145) —
    для 1b4 закрыто п. 5.
  - C5 — условие `len(pairs) == len(ids)` не покрыто ни одним мутантом (и в `residents` на HEAD: без него самопроверка
    OK), а задвоение давало MISMATCH без причины: поле «дубли» (причина, а не счётчик), мутант (8) задвоенной ветки,
    мутант (9) пустого меню под `bool(ids)`, удаление каждого конъюнкта роняет самопроверку.
  - C6 — противоречие ДОКИ («:473 → :72» против «спеки прошлых задач не трогать») снято удалением правки :473: абзац
    :470-476 выполнен 47f5001, его номер строки — снимок, как :661, TOWN-A1g1.md:49 и :511, TOWN-A1d1.md:866, которые не
    правятся; образец 70928df тронул в TOWN-J1-1b.md только журнал. Довод второго скептика (f0a6585 и 30cdc49 правили
    номера в теле; № 19) рассмотрен: № 19 — о ложном действующем критерии, а выполненный абзац уже не инструкция; приписку
    о сдвиге в журнал тоже не добавляем.
  - F1 — см. запись «Эффект на экране» выше; КОНТЕКСТ, CONTRACT § 3, ANTI-SCOPE, ORACLE, ДО СПАВНА п. 3–5 исправлены.
    Проверки сессии (scratch `1b4/`): `b4_tb.py` — BASE против себя, против синтетики r1 и против модели варианта (а)
    (`sk2m`, узел `Image` на месте emoji) — `7 frames, 0 FAIL`; «Круассан» → «Круассан, Круассан» — `6 FAIL` по узлу,
    exit 1; кадров нет — exit 1. `b4_pic.py` на emoji BASE `emu_s12r_{round10,round13,round210,wrong10,wrong13}` —
    `5 frames, 5 FAIL, boxes 25`, наибольшая доля 0,47 (круассан в слоте при 1,3), ≥ 0,60 — 0; спрайты, вклеенные в те же
    коробки (`pic_t/`), — `0 FAIL` (позиции тут тавтологичны — их проверяет п. 5 на настоящей сборке).
  - F3 — `$T` (scratch) определён в шапке ДО СПАВНА с оговоркой про новый вызов оболочки; в п. 7 файлы — `TS`, `TU`;
    в грепах п. 4 и 12 — явный файл лога (без него `grep -c` молчит с exit 2).
  - F4, K1 — у `big_almost` облачко 48 dp, заказ из 3 (content.json:1258, TrayScreen.kt:373, `bakery_states.sh:140`);
    40 dp — только облачко `big_wrong10` и слоты подноса.
  - F5, K1 — «≥ 5 из 6 на 40 dp» и «≥ 5 из 6 соответствий» по назначенным кадрам недостижимы при любом сиде
    (`Town.kt:646`): критерии — от фактического состава кадров, ключ — машинно из дампов. Отклонён дополнительный ряд
    6 WebP на 40 dp (F5, по желанию): вид спрайтов принят № 66 а, нужна узнаваемость в живом кадре.
  - F6 — отпечаток профиля md5 перед каждым прогоном; запасной ход — повтор ПАРЫ (скрипт возвращает профиль с `cmp`);
    town_route.sh — не между п. 9 и его повтором. Отклонено упоминание A1d: её приёмка эмулятор не трогает.
  - K2 (частично) — вопрос «Что это за еда?» оставлен (оба скептика: № 35 — о метках, а не о вопросе; вопрос повторяет
    контекст экрана пекарни; прецедент 1b3-2 — 6 из 6); подтверждённая часть — дыра самопроверки: приманка, названная
    любой едой или напитком (мяч → «яблоко», шампунь → «йогурт»), теперь роняет линзу; допустимые названия — в ключе до
    прогона.
  - K3 — у вопроса по `big_wrong10` появился критерий; «✓ не закрывает изделие целиком» выполнялся всегда (круг 20 dp
    ≤ 19,6 % коробки) — заменён фактом замера в § Ассеты (до 22 % на 40 dp), у «сцены» критерия ✓ нет: узнаваемость под
    ✓ — в критерии «ребёнка» («с ✓ назван полученным»); ✓ в кадре только на первом изделии заказа
    (`bakery_states.sh:146-147`), вид — в журнал.
  - K4 (частично) — посылка подтверждена (крендель в пунктире ≈ 53 % профилей и кекс на 40 dp ≈ 42 % в кадр не
    попадают — заказ от сида), вывод «синтетический лист кольца» отклонён: кольцо рисуется под картинкой и на ≥ 96 %
    граничит с белым (контраст кренделя с пунктиром 1,57 : 1 к читаемости не относится), кромка кекса — остаточный риск,
    принятый № 66 а. Осталось: виды пунктира и ✓ — в журнал.
  - K5 — пустой ячейки в sheets.py нет (зонд: `FileNotFoundError`): «S23 — доснять» — в заголовке листа и подписи.
    Часть (б) опровергнута: лист 1 px на dp физически не мельче телефона (40 px на мониторе ≈ 9–10 мм, 40 dp ≈ 6,4 мм),
    ряд в натуральную величину не добавлен.
  - K6 (частично) — «≈ 0,6» было кеглем emoji (TownUi.kt:124), а не протяжённостью: заменено замером (рамка и радиус
    спрайта больше, площадь та же ± 8 %); тезис критика «рамка почти одинакова» и «площадь меньше» опровергнут; вопрос
    «мельче / бледнее» отклонён — дубль «ребёнка» (называет изделия на 40 и 56 dp) и «доступности»; хлеб на
    `emu_s12r_wrong10` мерить нельзя — его коробку перекрывает указатель.
- 2026-09-29 (сессия 14), оркестратор после редакции: добавлен § 5 — системное «Назад» на итоге смены чистит строку
  питомца (находка D1 проверки доков 1b1-2, подтверждена по коду: GameViewModel.kt:184 зовёт `closeRound()` без
  `lines.clear()`, прочие ветки `back()` чистят, :188); GameViewModel.kt в allow (одна строка), бюджет — ожидается 14
  вставок и 4 удаления, ACCEPTANCE п. 6 (удалений 4) и п. 14 (греп и живая проверка «Назад» с красным на BASE).
  Нумерация вопросов 1b4 — № 89–90 (№ 83–88 заняты воротами j1b12).
- 2026-09-29 (сессия 14), ДО СПАВНА (workflow: автор зонда → критик по коду → правка; замеры; эмулятор; клон):
  п. 2 — `art_check.py pastries` (+59 −14 и правки критика): на BASE `MISMATCH` exit 1, `--selfcheck` OK (эталон + 10
  мутантов, у каждого своя причина), `residents` MATCH 9 и `--selfcheck` OK — вердикты как на HEAD; мета-проверка № 44 —
  сначала 4/5 (при `br` только из верных пар `not bad` лишний) → `br` = все ветки → 5/5; критик добавил мутант 10
  (задвоенная строка вместо bread; ослабление `ids == files` краснеет, остаток `br == files` — только при одновременном
  новом изделии и задвоении); на копии TownUi.kt с § 1 дословно — MATCH 6. П. 1 — release BASE 4 382 302 Б, цикл п. 11 —
  6 × FAIL (162 drawable в дампе, красный настоящий); lint BASE: game без сетевых 11 (5 + 6 `UnusedResources`
  `pastry_*`), classic 5. П. 4 — debug BASE, профиль md5 cba6afd3…, `DUMP=1 bakery_states.sh b4b riddle big` — 11 кадров,
  промахов 0, «Обрезано: 0»; `b4_pic.py b4b` → 5 frames, 5 FAIL (доли 0,01–0,39); `b4_tb.py b4b b4m` (подмена
  «Круассан, Круассан») → 5 FAIL. П. 5 — клон 47f5001 с § 1, 2, 5 дословно (+14 −4): `b4_tb.py b4b b4p` → 11 frames,
  0 FAIL; `b4_pic.py b4p` → 5 frames, 0 FAIL (витрина 0,9999–1,0, поднос 1,0, облачко 0,9996–1,0). П. 14 (§ 5):
  на BASE после «Назад» узел `line` ЕСТЬ, строка лежит на «Начать смену» (`emu_b4x_why/back`); на клоне — нет
  (`emu_b4y_*`); повтор — `$T/b4_back.sh PREFIX whyback` (копия `bakery_states.sh` с состоянием `whyback`, scratch).
- 2026-09-29 (сессия 14), кодер → приёмка → судьи → доки: дифф § 1, 2, 5 дословно (+14 −4 без import, +1 import; TownUi
  удалений 0). Машинная ACCEPTANCE — PASS 38/0: тесты 600/0/0 × 2; lint без сетевых game 5, classic 5, `UnusedResources`
  0 (на BASE 6); п. 7 — `art_check.py pastries` MATCH 6, `--selfcheck` OK, `residents` MATCH 9; п. 8 — 6 литералов-ключей;
  п. 11 — 6 × OK, APK 4 416 824 Б (+34 522 к BASE 4 382 302; доля J1 33 594 из ≤ 70 000; остаток бюджета эпика 713 245 Б).
  Живая (POST, эмулятор 360 × 640) — PASS 12/0, всего с повтором машинной — 50/0: профиль перед `b4` = перед `b4b` (md5 cba6afd3); п. 9 а — 11 frames,
  0 FAIL; п. 9 б — 5 frames, 0 FAIL, boxes 43; п. 12 а — промахов 0, «Обрезано: 0», `ui_measure` 11/11, profile restored;
  п. 12 б — GEOM OK 8, CLOSE OK 2; п. 14 — после «Назад» узла `line` нет (`b4z`; на BASE — есть, `b4x`). S23 не снимался
  (спит) — «доснять при владельце», GATE_QUEUE раздел 4. Лист `town/j1b4_gate.jpg`; судьи: «сцена» — PASS (крендель
  закрывает 2,5 % кольца, ✓ и пунктир читаются); «ребёнок» вслепую — 6 из 6 видов, хлеб сначала похож на кекс →
  вопрос № 97 (№ 93–96 заняты A1e); «доступность» — кромка пончика на витрине 2,98 : 1, различает форма. № 90 не задаётся
  (пунктир не теряется). Доки — § ДОКИ (GAME_CONCEPT, TOWN-J1, TOWN-A1, GATE_QUEUE, HANDOFF, журнал TOWN-J1-1b; ARCHITECTURE,
  BUILD_AND_DEMO, CONTENT_MAP, DATA_MODEL, UX_ACCESSIBILITY, REQUIREMENTS_MATRIX, TEST_CASES), в том числе § 5: снята
  оговорка «кроме системного „Назад“ на итоге» (ARCHITECTURE, UX_ACCESSIBILITY).
  Ревью: круг 1 — FAIL только за доки (ревьювер запущен параллельно с живой проверкой, до правки доков — WORKFLOW № 46),
  по коду нарушений 0; после доков (редактор → линзы → скептики, 5 правок) круг 2 — **PASS**.
- 2026-09-29 (сессия 14): ответ владельца на вопросы ворот 1b4 (GATE_QUEUE раздел 4) — «по рекомендациям»: № 89 **а**,
  № 97 **а**; § 18 — строки № 89, 90 (не задавался), 97. Кода не требуют: выпечка картинкой — только в раунде, вне
  раунда решит плейтест 1 (BACKLOG п. 19); хлеб остаётся — № 97 по смыслу закрыт № 66 повторно «а» (выпечка круга 1b3-2
  принята), малый круг `props.py` — только если дети на плейтесте 1 путают его с кексом (BACKLOG п. 20). S23 —
  по-прежнему «доснять» (GATE_QUEUE раздел 6).
