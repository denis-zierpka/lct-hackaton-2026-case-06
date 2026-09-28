```
TASK: TOWN-J1-1b — срез 1б: итог смены в сцене для всех работ (с питомцем), «Дневник» через bestStars, окно комнаты
      через residentOfWeek, арт пекарни (прилавок-оверлей, 6 спрайтов выпечки), вопросы на ворота 1б (№ 62–69)
EPIC: TOWN-J1 (docs/tasks/TOWN-J1.md — § 2 «Итог в сцене», § 5 п. 6–7, § 8 строка «1б»; решения владельца № 39–61)
BASE: у каждой задачи свой (в разделе задачи); первая — коммит оракула test(TOWN-J1-1b1)
BRANCH: feat/town; арт 1b3 — git worktree `wt/j1b/` внутри корня от коммита спеки, своя сессия Claude Code и свой
        `task-scope.json` (решение № 61 б, вариант A; запасной B — общий скоуп основного дерева; `wt/` — в .gitignore)
ЗАВИСИТ ОТ: TOWN-J1-1a2 — сделан (7475838, ревью PASS, лист town/j1a2_gate.jpg); решение № 61 б — dd72652
```
Номера строк — дерево dd72652. Высоты облачка — расчёт по метрикам montserrat (PIL по `res/font/montserrat.ttf`, скрипт сессии в репозиторий не вошёл), не замер:
замер — п. 11 приёмки 1b1.

## КОНТЕКСТ
Срез 1а поставил Борю за прилавок и сделал «Поднос по заказу», но конец смены остался карточкой: `Panel` «Спасибо за
помощь!» с портретом 72 dp и всеми строками `why` сразу, без питомца и без звёзд смены (`RoundScreen.kt:43-51`) —
большой текстовый Panel на экране места, регрессия по № 39/40. Срез 1б заменяет его сценой, общей для всех работ
(ответ 7 концепции, TOWN-J1.md:315), переводит «Дневник» и окно комнаты на геттеры среза 0 (`bestStars`,
`residentOfWeek`) и доводит арт пекарни (прилавок-оверлей и 6 спрайтов вместо emoji, № 58).

## ЧТО ВИДИТ РЕБЁНОК 7–11 В КОНЦЕ СМЕНЫ
| Когда | Что видит и слышит | Что понимает |
|---|---|---|
| сразу | Боря стоит на полу, рядом питомец подпрыгивает (у пекарни после ворот — пробует круассан, № 65); облачко Бори: «Спасибо!» | смена кончилась, меня благодарят |
| сразу | ряд ★ «+1» / ● / ○ — тот же, что был в раунде | мои звёзды — это монеты |
| 0–1 с | крупно «✉ +9» и рядом мелко «за смену», из числа летят монеты в чип «✉ +N» шапки; звук успеха | заработал 9 за эту смену, они ушли в конверт |
| сразу | «6 за смену + 3 за ★ — придёт с новым конвертом» | откуда 9 и когда придут (ТЗ 2.5.4) |
| сразу | «Боря: 1 из 6 смен до уровня 2» / «Боря: новый уровень 2! Теперь 7–11 за смену» | работаю больше — платят больше (рамка, Область 2) |
| по желанию | «Почему?» → питомец строкой внизу: «Карманные приходят каждую неделю, зарплата — когда поработаешь» | чем зарплата отличается от карманных (ТЗ 2.2) |
| выход | «Готово» → экран места; после третьей смены недели: у пекарни — «…приходи на новой неделе» и «Домой», у Марты — рынок без её заказа | естественная точка остановки (№ 47 б, 59 б) |
Без анимаций: питомец — кадр, монет нет, число «✉ +9» и чип шапки на месте. Нет большой карточки, списка строк сразу,
слов «база», «рекорд», «ошибка».

## НАРЕЗКА
| Задача | Что | Оракул | Кто | Порядок |
|---|---|---|---|---|
| **1b1** | итог смены в сцене (все работы, питомец, крупное «✉ +N»); строка итога домена без «. ✉ +N» | test-author: 18 литералов (ShiftTest 15, TrayTest 3), тестов 600 | coder, Gradle | 1 |
| **1b2** | «Дневник» через `bestStars`, окно через `residentOfWeek` + имя жителя в TalkBack | не нужен: геттеры закреплены (`TrayTest.kt:93-127, 731-761`), домен не меняется | coder, Gradle | 2 (после коммита 1b1) |
| **1b3** | арт: 6 `pastry_*` (`props.py`), `place.py --counter`, `to_webp.py --trim` | не нужен (A1: инструменты); зонды `art_check.py` | coder Opus xhigh, Blender, **worktree `wt/j1b/`, своя сессия Claude Code и свой `task-scope.json`** (№ 61 б, вариант A; запасной B — общий скоуп основного дерева, 1b3 «До спавна») | параллельно 1b1–1b2 (B — после коммита оракула 1b1; самотест хука красный — в основном дереве после 1b2) |
| — | **ворота 1б**: лист `town/j1b_gate.jpg` (1b1, 1b2) — вопросы № 62–64 и 69 | — | оркестратор | после 1b2 |
| — | **ворота арта**: общий лист арта пилота (выпечка, прилавок, фасады A1g — № 61 б) — вопросы № 65–68; фасадов A1g к приёмке 1b3 нет — лист без них, отступление от «один общий лист» сообщить владельцу | — | оркестратор | после 1b3 и 1b1 (№ 65 — питомец на итоге) |
| **1b1-2** | по нерекомендованным ответам № 62–64: своя спека после ворот 1б, как 1a2; RoundScreen/TrayScreen/PlaceScreen (+ `Town.kt` `orders` при подварианте Марты «остаётся»), ≤ 15 вставок | только при подварианте Марты № 62 б | coder, Gradle | после ворот 1б, до 1b4 |
| **1b3-2** | по № 66 б: ещё круг `props.py`, лист до/после, повторно — только № 66 | не нужен | coder Opus xhigh, тот же worktree `wt/j1b/` | после ответа № 66 б; ≤ 40 вставок в `props.py`; слияние worktree — после ответа |
| **1b4** | ассеты в UI по ответам: `pastryRes`, `Counter` → спрайт (№ 67), питомец ест круассан (№ 65) | не нужен; зонд `art_check.py pastries`, цикл `aapt2` | coder | после коммита ассетов |
| **1b5** | полёт изделия с витрины на поднос (№ 58) | не нужен | coder | последней; обязательна по № 58 — не успевает к сдаче: вопрос владельцу, а не молчаливый срез |
| — | закрытие: шаги 1–12 Приложения А, TC-26…TC-42, судьи, лист `town/j1b_final.jpg` | — | оркестратор | конец |
Оценка: 1b1 ≤ 100 вставок, 1b2 ≤ 15, 1b3 ≤ 190 (инструменты), 1b4 ≤ 30, 1b5 ≤ 50; арт ≤ 70 000 Б (TOWN-J1.md:313).
Критический путь: max(1b1 → 1b2 → ворота 1б; max(1b1, 1b3) → ворота арта) → [1b1-2; 1b3-2 и повторный № 66 при № 66 б] → коммит ассетов → 1b4 → 1b5 →
закрытие; если самотест хука красный и 1b3 идёт в основном дереве (не вариант B) — 1b1 → 1b2 → 1b3 → ворота арта → …; передача
сессии — после ответов на ворота 1б.

## ТРЕБОВАНИЕ ТЗ (docs/sources/ТЗ_текст.txt)
2.1 «короткую и понятную обратную связь по итогам принятых решений» (итог — сцена с короткими числами); 2.2
«изменение баланса … отвечает на вопрос „что изменилось и почему“» (крупное «✉ +9», строка «откуда», «Почему?»);
2.5.4 «Для каждого начисления указываются источник и сумма; баланс не меняется без объяснения» (сумма крупно из
`ShiftPay`, источник строкой домена, куда ушли — полёт монет в чип конверта); 2.5.11 «Пользователь видит
завершенные задания, прогресс по текущей цели» (мастерство на итоге, лучшая смена в «Дневнике»); 3.5 «не должны
запугивать, стыдить» (0★ — без оценочных слов; в «Дневнике» нет пустых звёзд); 3.6 — 48 × 48 dp, 16 sp «при
системном увеличении шрифта», «Цвет не является единственным способом…» (★ ● ○ — форма), «Звуки и анимации можно
отключить». Приложение А, шаг 6 — «Выполнение задания и получение игровой валюты — Да, с объяснением результата».
Рамка, Область 2 (`Единая_рамка_текст.txt:177-180`): «Знать, от чего зависит размер оплаты труда людей. Понимать, как
человек может повлиять на увеличение оплаты своего труда» — звезда = монета, смены → уровень → выше оплата
(`grep -c оплат docs/competencies.md` сейчас 0). Стоп-лист GAME_CONCEPT §9.2 № 1, 3, 5, 8, 10, 12, 13, 18.

---

# TOWN-J1-1b1 — итог смены в сцене (полностью)

```
TASK: TOWN-J1-1b1 — итог смены в сцене: житель на полу, питомец, облачко «Спасибо!» · ★★★● · «✉ +9 за смену» · откуда ·
      мастерство; «Почему?» и «Готово» под сценой; строка итога домена без «. ✉ +N»
BASE: коммит оракула test(TOWN-J1-1b1) (18 литералов, 600 тестов); старт — коммит спеки и инструментов 1b1
```

## CONTRACT
Внешняя пара «» у строк — разделитель. Строки ребёнку — дословно. Новые видимые строки две — «Спасибо!» (титул
облачка; слова Бори из TOWN-J1 § 1–2) и «за смену» (подпись к крупному «✉ +N», концепция «✉ +9 за смену»,
TOWN-J1.md:171); новые строки TalkBack — «Заработали N:» (повторяет запись дневника `Town.kt:755`), «за звезду» / «за звёзды» вместо
«за ★», `paneTitle` «Имя: спасибо! Заработали N»; «Имя: спасибо!»
в TalkBack — повтор строки раунда (`TrayScreen.kt:369`).

### 1. Домен (Town.kt, `finishShift`, :758-765) — только строка
Из трёх веток уходит «. ✉ +$total»; сумма живёт в `TownOutcome.pay` (`ShiftPay.total`, `TownState.kt:19`):
- TAPS: «${quote.base} за три поручения — придёт с новым конвертом»
- `bonus > 0`: «${quote.base} за смену + $bonus за $what — придёт с новым конвертом» (`what` — как сейчас)
- иначе: «${quote.base} за смену — придёт с новым конвертом»
Прочее не меняется: `why` (:767-783), `pay` (:784), журнал «Смена: …», дневник «Заработали …», отказы. Слова только
убираются — стоп-лист № 3/№ 10 и S1aChildText не задеты.

### 2. ViewModel (GameViewModel.kt)
- рядом с `roundResult` (:131-132): `/** Числа итога смены для сцены (TOWN-J1-1b1); null — в раунде и после отказа. */
  var roundPay: ShiftPay? by mutableStateOf(null); private set` (импорт `ru.finny.pet.domain.town.ShiftPay`);
- `startRound` (:439) и `closeRound` (:505): рядом с `roundResult = null` — `roundPay = null`;
- `finishRound`, ветка `Done` (:487-495): после `roundResult = Line(o.line, o.why)` — `roundPay = o.pay`; после
  `sfx(Sound.SUCCESS)` — `bounce++` (питомец на итоге подпрыгивает; образец — `achieveGoal`, :301; `seen` общий —
  комната прыжок не повторит, `PetSprite.kt:78-85`). Ветка `Refused` — без изменений. `Effect.PetAction` не шлётся
  (EAT рисует корм `item_food_basic`, `PetSprite.kt:135`; «пробует круассан» — 1b4 по № 65).

### 3. Ряд звёзд — общий (TrayScreen.kt)
Ряд `TrayScreen.kt:217-230` выносится без изменения вида и TalkBack раунда:
```kotlin
/** TalkBack ряда звёзд подноса (раунд и итог, TOWN-J1-1b1). */
internal fun starsText(r: TrayRound) = "Обслужено ${r.results.size} из ${r.orders.size}, звёзд ${r.stars}"
/** ★ «+1» — с первого раза, ● — обслужен, ○ — ещё придёт (раунд) / не пришёл (итог); один узел TalkBack [starsText]. */
@Composable internal fun StarRow(r: TrayRound)   // тело — Row :217-230 как есть, семантика — clearAndSetSemantics { starsText(r) }
```
В раунде — `StarRow(r)` на прежнем месте (`job_row`). `Star`, `Bubble`, `customerFrame` остаются `private`. «+1» — при
любом шрифте, как в раунде.

### 4. Сцена итога (RoundScreen.kt; вместо ветки `result != null -> Column { Panel … }`, :43-51)
`result != null -> ShiftResult(vm, job, result, Modifier.weight(1f))`,
`@Composable private fun ShiftResult(vm: GameViewModel, job: Job?, result: Line, modifier: Modifier)` — одна раскладка
для TRAY, TAPS, CHANGE, MATCH3; без `Panel`, без прокрутки. HUD прежний (`Hud1(vm, inPlace = true, inRound = true)`,
`Hud2 { StatsCollapsed; MailChip }`, :40-41); фон — фон места (`GameApp.kt:241-253`).

Признаки: `val pay = vm.roundPay` (null — отказ); `val r = vm.tray?.takeIf { pay != null && job?.game ==
JobGame.TRAY }` (поднос раунда живёт до `closeRound`, `finishRound` его не обнуляет, :498-500); `val act =
LocalPetAction.current`.

`BoxWithConstraints(modifier.fillMaxWidth().padding(8.dp))`:
- `floor = maxHeight − 64.dp` (ряд кнопок 61 dp — 56 + кромка 5, `Widgets.kt:114`; зазор 3; кончик хвоста облачка
  касается «Почему?»); `f = (floor − 8.dp).coerceIn(160.dp, 232.dp)` — кадр жителя на итоге ≤ 232 dp (на экране заказа
  264): облачку нужна ширина под крупное «✉ +N» и «+1» при 1,3 (расчёт ниже); `bx = 8.dp + f * 0.45f`; `head = floor −
  f + f * 0.1f`.
- **Житель работы** (`vm.tc.residents.firstOrNull { it.id == job?.resident }`, нет — пропустить): `ResidentPic(it, f,
  Modifier.offset(x = 8.dp − f * 0.3f, y = floor − f).clearAndSetSemantics {})` — низ кадра на `floor`, стоит на полу.
  **Прилавка на итоге нет ни у одной работы** (`Counter` не вызывается; вариант «за прилавком» — вопрос № 63 б).
- **Питомец** (`vm.state.pet`, null — пропустить): `PetSprite(speciesId = pet.speciesId, colorId = pet.colorId, stage =
  vm.economy.stageIndex(pet.growth), face = Face.HAPPY, animate = LocalAnimate.current, modifier = Modifier.offset(x = 8.dp,
  y = floor − 96.dp).clearAndSetSemantics {}, size = 96.dp, bounceKey = vm.bounce, action = act.action, actionKey =
  act.key, seen = act)` — по центру перед жителем, закрывает ноги и низ фартука; низ на `floor`; `particleTarget("pet")`
  не ставить (цель сердечек — питомец комнаты). Облачко начинается с x = `bx` ≈ 112 dp — правее питомца (8–104 dp).
  Лицо на итоге — всегда `Face.HAPPY` (как на экранах старта, `StartScreens.kt:86`): питомец радуется смене; сытость и
  настроение видны в HUD и в комнате (правка оркестратора по судьям 2026-09-28: с `vm.face` голодный питомец грустил
  после 4★, и «ребёнок» читал это как «расстроился из-за меня» — ТЗ 3.5).
- **Облачко** `SpeechBubble` (Widgets.kt:280) — расстановка как в `TrayJobScene` (`PlaceScreen.kt:358-363`):
  `Modifier.layout { m, c -> val pl = m.measure(c); val y = minOf(head.roundToPx(), (floor − 8.dp).roundToPx() −
  pl.height).coerceAtLeast(8.dp.roundToPx()); layout(pl.width, pl.height) { pl.place(bx.roundToPx(), y) } }
  .width(maxWidth − bx).heightIn(max = floor − 16.dp).clearAndSetSemantics { contentDescription = desc; paneTitle =
  if (pay != null) "${vm.residentName(job?.resident ?: "")}: спасибо! Заработали ${pay.total}" else result.text;
  testTag = "job_result" }` (`testTag` и `paneTitle` — внутри блока семантики, импорты
  `androidx.compose.ui.semantics.testTag` и `…semantics.paneTitle`, как `TrayScreen.kt:77`; образцы `paneTitle` —
  `Common.kt:98`, `TrayScreen.kt:260`). `paneTitle` — объявление итога без касания; не `liveRegion` (Compose для нового
  узла с живой областью события не шлёт; живая область раунда `TrayScreen.kt:296` уходит вместе с раундом). У итога с
  оплатой низ облачка = `floor − 8` (кончик хвоста — `floor + 4`, над «Почему?»), ветка `head` — только у отказа (одна
  строка). Содержимое — прямо в колонке `SpeechBubble` (у неё уже `spacedBy(8.dp)`), **без `verticalScroll`**:
  невлезшая строка тогда обрезается и видна зонду «Обрезано» (TText, `TownUi.kt:72-79`) — это машинная проверка
  «влезает» (п. 11); под `clearAndSetSemantics` признак `scrollable="true"` в дампе не появился бы (семантика потомков
  стёрта), поэтому проверка прокрутки здесь слепа. Сверху вниз:
  1. `if (pay != null) TText("Спасибо!", style = titleMedium, color = G.ink, maxLines = 1)`;
  2. `r?.let { StarRow(it) }`;
  3. `pay?.let { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp))
     { TText("✉ +${it.total}", Modifier.particleTarget(LocalParticles.current, "job_result"), style = headlineMedium,
     color = G.purple, maxLines = 1); TText("за смену", style = bodyMedium, color = G.inkSoft, maxLines = 1) } }` — одна
     строка `Row`, как «6–10» с монетой на заказе (`PlaceScreen.kt:369-372`; по ширине текста 204 dp влезает, высота не
     растёт); монеты `CoinsFrom("job_result", "mail")` (`GameViewModel.kt:494`, без изменений) летят из крупного числа
     в чип «✉ +N» шапки (`PlaceScreen.kt:393`);
  4. `TText(result.text, style = bodyMedium, color = G.ink)` — строка домена (§ 1) или строка отказа;
  5. `result.why.firstOrNull()?.let { TText(it, style = bodyMedium, color = G.inkSoft) }` — мастерство сразу.
- **Ряд кнопок** `Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalArrangement = spacedBy(8.dp))`:
  `if (result.why.size > 1) GameButton("Почему?", Modifier.weight(1f), ButtonStyle.PAPER, minHeight = 56.dp) {
  result.why.drop(1).forEach { vm.say(it) } }` — строки встают в очередь LINE (`say` не ставит дубль, :193) и их
  говорит питомец (`LineHost`, `TownUi.kt:288`), касание строки её закрывает; `GameButton("Готово", Modifier.weight(1f),
  ButtonStyle.PRIMARY, minHeight = 56.dp) { vm.closeRound() }`. Кнопки вне облачка — прокрутка их не спрячет
  (WORKFLOW № 38). У отказа (`why` пуст) — одна «Готово» во всю ширину, в облачке только `result.text`.

**TalkBack** — облачко одним узлом, `dot` — из PlaceScreen.kt (:336, `private` → `internal`):
```kotlin
val desc = listOfNotNull(
    if (pay != null) "${vm.residentName(job?.resident ?: "")}: спасибо!" else null,   // повтор строки раунда, TrayScreen.kt:369
    r?.let(::starsText),
    (pay?.let { "Заработали ${it.total}: " } ?: "") +
        result.text.replace("за ★", if (pay?.bonus == 1) "за звезду" else "за звёзды"),
    result.why.firstOrNull(),
    *result.why.drop(1).toTypedArray(),
).joinToString(" ") { dot(it) }
```
Пример: «Боря: спасибо! Обслужено 4 из 4, звёзд 3. Заработали 9: 6 за смену + 3 за звёзды — придёт с новым конвертом.
Боря: 1 из 6 смен до уровня 2. Карманные приходят каждую неделю, зарплата — когда поработаешь.» (при 1★ — «1 за
звезду»). При появлении объявляется «Боря: спасибо! Заработали 9» (`paneTitle`), полный текст — в описании облачка.
Обход: HUD → облачко → «Почему?» → «Готово»; житель и питомец скрыты (как жители раунда).

**Расчёт высоты облачка** (360 × 640; H сцены = 478 dp — замер 1а, `TOWN-J1-1a.md:178`, от шрифта не зависит; шрифт на
сцене ≤ 1,3 — `SceneFontScale`, `GameApp.kt:116`). При f = 232: ширина облачка 344 − 112 = 232 dp, текст 204 dp;
потолок `floor − 16` = 382 dp при обоих шрифтах.
| Случай (с «+1» и крупным «✉ +N» headlineMedium) | 1,0 | 1,3 |
|---|---|---|
| пекарня 4★ + новый уровень (худший) | ≈ 272 | ≈ 357 |
| пекарня 3★ | ≈ 272 | ≈ 328 |
| Марта (без ряда звёзд) | ≈ 218 | ≈ 296 |
| для сравнения: худший при f = 264 (текст 189 dp) | ≈ 294 | ≈ 414 — не влезает; без «+1» ≈ 386 (впритык) |

### Явно НЕ определено (кодер спрашивает, а не решает)
Любая строка сверх § 1–§ 4; другое место или размер жителя и питомца; жест питомца, кроме прыжка; прилавок на итоге;
изменение раунда (кроме выноса `StarRow`), экрана заказа (`TrayJobScene`) и `OrderCard`; поведение ⌂ и «Назад» на
итоге (`GameViewModel.kt:178, 181` — как есть); LINE, пришедшая на итог (`arrived`, :222-226) поверх ряда кнопок, — как
на прочих экранах (касание закрывает).

## SCOPE
variant: main (одна функция домена, только литералы) + game (UI)
allow: finny-pet/app/src/main/java/ru/finny/pet/domain/town/Town.kt,
  finny-pet/app/src/game/java/ru/finny/pet/game/GameViewModel.kt,
  finny-pet/app/src/game/java/ru/finny/pet/game/screens/RoundScreen.kt,
  finny-pet/app/src/game/java/ru/finny/pet/game/screens/TrayScreen.kt,
  finny-pet/app/src/game/java/ru/finny/pet/game/screens/PlaceScreen.kt (только `private fun dot` → `internal`)
protect: базовый (.claude/, CLAUDE.md, docs/, finny-pet/app/src/test/, gradle-файлы, AndroidManifest, proguard) +
  finny-pet/docs/, finny-pet/app/src/classic/, finny-pet/app/src/main/assets/, finny-pet/app/src/main/res/,
  finny-pet/app/src/game/res/, …/PetSprites.kt, …/data/, …/domain/{Content,Economy,GameState,Match3}.kt,
  …/domain/town/{Dictionary,Enums,EventDef,Migration,PetTalk,Prices,TownCodec,TownContent,TownEvents,TownState,Tray}.kt,
  …/game/GameApp.kt, …/game/ui/ (TownUi, Widgets, PetSprite, GameTheme…), …/game/mock/; инструменты поимённо
  (WORKFLOW № 29); при варианте B пилота скоуп общий с 1b3 и A1g — правило protect там (1b3, «До спавна»).
  Пересечение с allow — пусто (№ 23).

## ANTI-SCOPE
Прилавок на итоге (№ 63 б); жест «пробует круассан» и параметр предмета в `PetSprite` (№ 65 → 1b4); жетоны или полоска
мастерства (№ 64); «Дневник», окно (1b2); спрайты, `Counter()`, полёт (1b3–1b5); «Домой» у `OrderCard` и курьера
(№ 62); сцена кассы и курьера, фон парка (курьер недостижим: `job_courier_help` в `eventsOff`, `content.json:2256`);
чистка Match3 и строки «Загадка Бори: отгадаешь — бомбочка» (`PlaceScreen.kt:447`, срез 2); `MockShopJob.kt:208` (debug,
старая строка); общий скелет с `TrayJobScene` (экран заказа не трогаем); `content.json`, тесты, доки, `tools/`; новые
зависимости, звуки, ресурсы.

## БЮДЖЕТ
≤ 100 вставок: Town.kt 3, GameViewModel.kt ≈ 7, RoundScreen.kt ≈ 60, TrayScreen.kt ≈ 16 (−13), PlaceScreen.kt 1. Новых
файлов 0, зависимостей 0. Счёт — `git diff -w --numstat BASE -- app/src`. Правки по живой проверке и судьям — отдельным
кругом кодера по решению оркестратора (≤ 30, прецедент 1а).

## ORACLE (test-author до кодера)
Правило: в литерале строки итога `". ✉ +N — $payLater"` → `" — $payLater"` (`payLater` — ShiftTest.kt:77,
TrayTest.kt:69). Покрытие не ослаблять, тестов остаётся 600.
- ShiftTest.kt: 212, 367, 387, 421, 427, 438, 453, 467, 468, 483, 484, 485, 486, 517, 565 (15; ветки MATCH3 — 387, 438,
  453, 565 — тоже: ветка в домене живёт до среза 2);
- TrayTest.kt: 685, 689, 776 (3).
Красные на BASE — все тесты с этими строками, имена — в отчёте test-author (WORKFLOW № 37). `pay` уже закреплён
(ShiftTest.kt:213, 368). UI (§ 2–§ 4) JVM-тестом не покрывается — п. 11.

## ИНСТРУМЕНТЫ ДО СПАВНА (оркестратор; строки 1b1 — коммитом со спекой, строки 1b2 — отдельным коммитом инструментов до спавна 1b2: на сборке 1b1 они дают «not found»)
- `tools/town_route.sh` (1b1):
  - `shot2` (:39-43) — `DUMP=1` сохраняет сырой дамп рядом со снимком, по образцу `bakery_states.sh:47-48`;
  - :124 → `serve 2.0; serve 2.5; has "Заработали " result; has "Почему?" result; no_hud_hint "Готово" result; shot2
    result`; :125 → `tapx "Почему?" 1.2; [ -n "$(text_xy "Карманные приходят каждую неделю, зарплата — когда
    поработаешь")" ] || echo "  not found: LINE on why"; shot2 why; tp "Карманные" 0.8; tapx "Готово" 1.5` (строка
    питомца — через `text_xy`, текстовый узел: `has` нашёл бы её и в описании облачка);
  - :131 (Марта) → `has "Заработали 6: 6 за три поручения — придёт с новым конвертом" tresult; has "Почему?" tresult`
    перед `shot2 tresult`;
  - :144 → `has "Заработали " result2; has "Почему?" result2; no_hud_hint "Готово" result2; shot2 result2` вместо
    `shot1 result2`;
  - шапка — строки 1б.
- `tools/bakery_states.sh` (1b1): `state` += 5-й необязательный аргумент — `records.job_bakery`; состояние `levelup` —
  `state 5 0 keep`, «Начать смену», 4 покупателя верно (`put_list "$(order)"; tapx "Отдать" 2.2` × 4) → `has
  "Заработали 10: 6 за смену + 4 за звёзды" levelup; has "новый уровень 2" levelup; shot2 levelup` (худший случай
  высоты: 4★ и строка нового уровня; `jobLevelShifts` [0, 6, 15], `baseByLevel` [6, 7, 8]); DUMP=1 даёт дамп для
  `ui_measure.py`; `tapx "Готово" 1`. Состояние `zero` — `state 1 0 keep` (1 смена пекарни), «Начать смену», 4
  покупателя: каждому сначала полный поднос с изделием вне заказа (верные без одного + одно из `outside`, как `big`,
  `bakery_states.sh:113-114`; заказ из 1 — одно из `outside`) → «Отдать» → `has "Ещё нужно: "` → недостающее →
  «Отдать» (неполный поднос домен не принимает и звезды не снимает: `Tray.kt:50, 53`); итог 0★ — строка «6 за смену — придёт с новым
  конвертом», ряд ●●●● → `has "Заработали 6: 6 за смену — придёт с новым конвертом" zero; shot2 zero`; `tapx "Готово" 1`.
- `tools/rec.sh` (1b1): действие `xy:X Y` — одна строка через `adbui.sh tapxy` (:39).
- 1b2 (отдельный коммит): `tools/town_route.sh` — после `shot1 room` недели 1 — `has "Окно: улица, машет Ося" room`; в
  цикле недель 2–5 — `has "Окно: улица, машет $name" room_w$w` (Тоша, Степан, Кеша, Ася — `arrivesWeek` в
  `content.json`); после `shot1 home_from_place` — `tp "Дневник"; has "Помочь Боре: лучшая смена, звёзд " diary; shot1
  diary; tp "Назад"`. `tools/bakery_states.sh` — состояние `diary`: `open_diary` (launch → «Продолжить» → «Дневник»),
  якорь `has "Очки роста" diary`; строка «лучшая смена» ищется листанием с проверкой после каждого свайпа, одинаково во
  всех подслучаях: `state 0 0 keep "" 60` → узла «лучшая смена» нет (наследие Match3 отсечено); `state 0 0 keep "" 0` →
  нет (0★ не показываем); `state 3 0 keep "" 3` → узел «Помочь Боре: лучшая смена, звёзд 3» есть (он же самопроверка
  навигации), `shot2 diary`.
- До листа ворот 1б (не коммит): `tools/rec.sh` — запись смены пекарни с `demoSizes`, длительность — для № 69; для № 69
  допустима запись `rec.sh sleep:25` с касаниями человека.

## ACCEPTANCE (команды 1–10 — из finny-pet/, 11–12 — из корня; гоняет оркестратор)
1. ./gradlew testClassicDebugUnitTest --rerun --console=plain -> exit 0
2. tests = 600, failed 0, skipped 0 (подсчёт по XML, пресет)
3. ./gradlew testGameDebugUnitTest --rerun --console=plain -> exit 0, 600 / 0 / 0
4. ./gradlew assembleClassicDebug assembleGameDebug assembleGameRelease -> exit 0
5. ./gradlew lintClassicDebug lintGameDebug -> ошибок 0, предупреждений без сетевых ≤ 5 / 5
6. git diff --name-only BASE -- app/src/test/ app/src/main/assets/ app/src/main/res/ app/src/classic/ app/src/game/res/ '*.gradle.kts' gradle/ gradle.properties -> пусто
7. git status --porcelain -uall -- app/src -> только пути allow (вариант B пилота — `git status --porcelain -uall` всего
   дерева и `git -C wt/… status`, 1b3 «До спавна»); из корня `node .claude/hooks/assert-oracle-intact.js` -> exit 0;
   git diff -w --numstat BASE -- app/src -> вставок ≤ 100;
   git diff -w BASE -- app/src/game/java/ru/finny/pet/game/screens/PlaceScreen.kt | grep -c '^[-+][^-+]' -> 2;
   git diff -w BASE -- app/src/main/java/ru/finny/pet/domain/town/Town.kt | grep -c '^[-+][^-+]' -> 6
8. grep -rnE '^import (android|androidx)|System\.(currentTimeMillis|nanoTime)|Random\(\)|LocalDate|Clock|Instant' app/src/main/java/ru/finny/pet/domain/ -> пусто
9. S=app/src/game/java/ru/finny/pet/game/screens:
   grep -c '✉ +' app/src/main/java/ru/finny/pet/domain/town/Town.kt -> 0;
   grep -cE '\bPanel\(' $S/RoundScreen.kt -> 0; grep -c 'Спасибо за помощь' $S/RoundScreen.kt -> 0;
   grep -c 'verticalScroll(' $S/RoundScreen.kt -> 1 (только кнопочные работы, :54); grep -c 'Counter(' $S/RoundScreen.kt -> 0;
   grep -c '"job_result"' $S/RoundScreen.kt -> 2 (testTag, particleTarget); grep -c 'PetSprite(' $S/RoundScreen.kt -> 1;
   grep -c 'StarRow(' $S/TrayScreen.kt -> 2; grep -c 'StarRow(' $S/RoundScreen.kt -> 1;
   grep -c 'roundPay' app/src/game/java/ru/finny/pet/game/GameViewModel.kt -> 4;
   grep -c 'bounce++' app/src/game/java/ru/finny/pet/game/GameViewModel.kt -> 2;
   grep -rn '"Обслужено ' app/src/game --include=*.kt -> одна строка (TrayScreen.kt, starsText)
10. стоп-слова в литералах диффа: git diff -w BASE -- app/src | grep '^+' | grep -oE '"[^"]*"' | grep -ciE
   'сегодня|осталось|до конца недели|скорее|пока не|успей|последн|ошибк|неправильн|точно|потеряеш|рекорд|цена лени|ленив|зачем|транжир|жадин|зря|впуст|провал|плохо|скучал|где ты был|рейтинг|лидер|лучше, чем' -> 0
11. живая проверка (эмулятор `tools/adbui.sh wm360` и S23, debug, шрифт 1,0 и 1,3; на S23 — громкость, шрифт, бэкап
   профиля до и после — HANDOFF «Окружение»):
   а) `DUMP=1 tools/town_route.sh PREFIX`: лог `grep -cE "not found|gate not passed"` → 0; на каждом снимке «Обрезано:
      0»; итоги `result`, `tresult`, `result2` — узел «Заработали …», «Почему?», нет HUD «Подсказка»; «Почему?» → LINE
      «Карманные приходят…» (текстовый узел, `text_xy`) → касание закрывает → «Готово» → экран места;
   б) `DUMP=1 tools/bakery_states.sh PREFIX levelup` → «levelup10: Обрезано: 0», «levelup13: Обрезано: 0»; самопроверка
      зонда (WORKFLOW № 14): `assembleGameDebug` в превью-worktree (HANDOFF «Окружение») поверх кода кодера с
      `heightIn(max = 200.dp)` у облачка — `levelup` при 1,3 даёт «Обрезано: ≥ 1»; в ветку не идёт; после —
      переустановить сборку задачи. При «Обрезано ≥ 1» на `levelup13` у сборки задачи — `f.coerceIn(160.dp, 216.dp)`
      без нового вопроса;
   в) `python tools/ui_measure.py <дамп levelup13> levelup13 3` и на `result13` — кликабельных < 48 dp нет, за краем нет;
   г) без анимаций (раздел взрослого): итог — питомец неподвижен, «✉ +9» и чип шапки числом, строки на месте;
      `tools/rec.sh` — координаты «Отдать» заранее, `input tap`, ≥ 8 с, `fps=10`, разница по области питомца = 0;
      самопроверка — та же запись с анимациями даёт разницу > 0 (WORKFLOW № 31, № 39);
   д) release на эмуляторе (`adb install -r "$(cygpath -w …/app-game-release.apk)"`, pkgFlags без DEBUGGABLE,
      WORKFLOW № 33): смена пекарни → итог в сцене, питомец и житель видны.
12. судьи по снимкам (эмулятор 1,0 / 1,3, S23; пекарня 0★ (`zero`) и 3★, `levelup`, Марта, `result2` при 1,0 / 1,3, кадр
   «Почему?» при 1,0 / 1,3): «ребёнок 7–11» вслепую с самопроверкой (WORKFLOW № 35: кто благодарит, сколько заработал и
   когда придут монеты, что значат ★ ● ○ и «+1», что значит «до уровня 2» и чей это уровень — твой или Бори, что делает
   питомец, что нажать дальше; на `result2` — сколько заработал за эту смену и что значит ○); доступность (контраст
   облачка, форма вместо цвета, наложения питомца, жителя и облачка, строка питомца при 1,3 закрывает питомца, отличим
   ли питомец от жителя (Боря, Марта) в цвете и в сером, мыльность Марты 512 в кадре 232, серый и дейтеранопия);
   TalkBack-дамп (порядок узлов; ещё один — с открытой строкой «Почему?»; судье — понятно ли, что делает «Почему?»).
   Находки — в журнал до ворот.

## ЖИВАЯ ПРОВЕРКА (человек: docs/BACKLOG.md п. 1 и 11)
S23: смены пекарни и Марты — сцена итога, монеты летят в «✉» шапки, питомец подпрыгивает, звук успеха; «Почему?» —
строка питомца; TalkBack: итог объявлен без касания (дублируется ли чтение — при появлении и при фокусе на облачке —
записать), облачко одним узлом, затем «Почему?», «Готово»; появление LINE после «Почему?» TalkBack не объявляет — записать.

## ДОКИ 1b1 (оркестратор, после приёмки, до ревью; готовить в scratch, применять, когда кодер основного дерева не работает; при варианте B пилота и работающем арт-кодере — одной командой вместе с коммитом, 1b3 «До спавна»; греп по обоим docs/ и tools/)
Грепы: `Спасибо за помощь`, `прежняя карточка|итоговом экране`, `\. ✉ \+[0-9]* —`, `✉ \+[0-9]+( —|…)`,
`одн(а|ой) кнопк`, `итог 72 dp`, `job_result`, `пробует булочку`, `из 6 смен до уровня`. Править то, что стало ложью:
GAME_CONCEPT §0 (:20 «до него итог — прежняя карточка»), §3.1 (:135), §5.2 (:351-352, :410), §5.5 (:519, :529-531; «и
рекорд» убрать), §11 шаг 6 (:1433-1435), §14 (путь TalkBack итога), §17.2; :313 (строка HUD «✉ +18 — придёт…») не
трогать; TOWN-J1.md — статус, § 2 («Почему?» под сценой и строкой питомца, житель на полу, кадр 232, строка домена без
«✉ +N»; ссылки `GameViewModel.kt:468` → :494, `Town.kt:678` → :776); finny-pet/docs — ECONOMY (строка итога), TEST_CASES
TC-28, TC-32, TC-41, BUILD_AND_DEMO шаги 6 и 9, UX_ACCESSIBILITY (итог: узел, порядок, объявление без касания,
«Почему?» — LINE, без анимаций; при TalkBack «Почему?» ничего не озвучивает; ответ уже в описании облачка (обход);
BACKLOG п. 11), ARCHITECTURE (`roundPay`, `StarRow`, `job_result` — облачко итога),
LIMITATIONS_ROADMAP:28 («итог 72 dp» → кадр итога ≤ 232 dp), REQUIREMENTS_MATRIX (2.5.4; строки 2.1 нет — добавить);
docs/competencies.md — Область 2 «оплата труда» (`grep -c оплат` ≥ 1). Спеки прошлых задач не трогать.

---

# TOWN-J1-1b2 — «Дневник» через `bestStars`, окно через `residentOfWeek` (контракт и приёмка)
**BASE**: коммит 1b1 (ревью PASS). **Оракул не нужен**: `residentOfWeek` и `bestStars` закреплены (`TrayTest.kt:93-106,
110-127, 731-761`), домен не меняется. **До спавна** — коммит инструментов 1b2 (раздел «Инструменты»).

**CONTRACT** (новые строки: «лучшая смена», «машет»)
1. RoomScreen.kt:130 → `val resident = vm.town.residentOfWeek(s)` (то же правило, `Town.kt:617-618`).
2. RoomScreen.kt:140 — подпись `Target`: `"Окно: улица" + (resident?.let { ", машет ${it.name}" } ?: "") + (if
   (streetEvent) ", есть событие" else "")` — «Окно: улица, машет Ося». «Машет» — слово концепции (§10.1 «в окне машет
   житель недели»), рода не требует (у `Resident` его нет, `TownContent.kt:125-128`). Закрывает долг TalkBack A1d
   (`UX_ACCESSIBILITY.md:332`); 64 dp и подоконник — A1d.
3. ProgressScreens.kt:55-58 — вместо «Рекорды: …» из сырых `s.records`:
   ```kotlin
   val best = vm.tc.jobs.mapNotNull { j -> vm.town.bestStars(s, j.id)?.takeIf { it > 0 }?.let { j.title to it } }
   if (best.isNotEmpty()) Row(/* как сейчас, с Image(ui_gamepad) 32 dp */) { Column { best.forEach { (t, n) ->
       TText("$t: лучшая смена " + "★".repeat(n), Modifier.clearAndSetSemantics { contentDescription = "$t: лучшая смена, звёзд $n" }, style = bodyMedium) } } }
   ```
   Видно «Помочь Боре: лучшая смена ★★★». Не показываются: 0★ и пустые ☆ (личный раздел не говорит «не вышло», ТЗ 3.5,
   стоп-лист № 10), наследие Match3 (60) и не-TRAY работы (`bestStars` → null). `ui_gamepad` остаётся (иначе новое
   предупреждение lint `UnusedResources`). Слова «рекорд» на экране нет.
**SCOPE** allow: …/screens/RoomScreen.kt, …/screens/ProgressScreens.kt; protect — как у 1b1 + `…/domain/town/Town.kt`,
`…/game/GameViewModel.kt`, `…/screens/{RoundScreen,TrayScreen,PlaceScreen}.kt`; пересечение с allow — пусто.
**БЮДЖЕТ** ≤ 15. **ANTI-SCOPE**: житель 64 dp на подоконнике (A1d), мастерство работ в «Дневнике» (BACKLOG), картинка
строки, домен, `content.json`, тесты.
**ACCEPTANCE**: п. 1–6, 8 и 10 как у 1b1 (600 тестов, `git diff --name-only BASE -- app/src/main/` → пусто); п. 7 —
status только allow, `assert-oracle-intact` exit 0, вставок ≤ 15 (подпункты PlaceScreen и Town.kt — только 1b1); из
finny-pet/: `grep -rn 'arrivesWeek' app/src/game/java` → пусто; `grep -rnE '\bs\.records|state\.records'
app/src/game/java` → пусто; `grep -rn 'Рекорды' app/src/game/java --include=*.kt` → пусто; `grep -c 'residentOfWeek'
…/RoomScreen.kt` → 1; `grep -c 'bestStars' …/ProgressScreens.kt` → 1. Живая: маршрут (окна недель 1–5 «машет
<имя>», «Дневник») — лог 0 промахов; `tools/bakery_states.sh PREFIX diary` — все три подслучая.
**ДОКИ**: ARCHITECTURE:86 (`Room` ← `Town.residentOfWeek`), :94 (`Progress` ← `Town.bestStars`); ECONOMY:267, :277 и
DATA_MODEL:222 (`records` показывает «Дневник» через `bestStars`); REQUIREMENTS_MATRIX:76 (2.5.11 «лучшая смена»);
UX_ACCESSIBILITY:306, :332 (подпись окна, долг снять); TOWN-A1.md:42 (A1d: подпись сделана, остаются 64 dp и
подоконник); GAME_CONCEPT §4.1 («личный рекорд» → «лучшая смена» в «Дневнике»), §10.1 (окно, «Дневник»);
LIMITATIONS_ROADMAP:26; TEST_CASES — новый TC «Дневник: лучшая смена» (с наследием 60).

---

# TOWN-J1-1b3 — арт: выпечка и прилавок-оверлей (worktree `wt/j1b/`, № 61 б; контракт и приёмка)
**BASE**: коммит спеки (от него строится worktree).
**Кодер** — Opus, effort xhigh (Workflow `agent({agentType:'coder', model:'opus', effort:'xhigh'})`), Blender 5.2,
`-b --factory-startup --python-exit-code 1`, рендеры — только в `$T` вне репозитория, Gradle не нужен. Идёт в git
worktree `wt/j1b/` (внутри корня) от коммита спеки параллельно 1b1–1b2 и генератору фасадов A1g (`wt/a1g/`, своя спека;
пилот № 61 б); спавнится из своей сессии Claude Code в `wt/j1b/` со своим `task-scope.json` (вариант A, «До спавна»).
Итерации кодера — `--samples 16` (выпечка) и `--preview` (прилавок); финальные рендеры приёмки и листа — оркестратор, по
очереди с A1g. Кодеру (в обоих вариантах): «команды с путями из protect — без `>` (WORKFLOW № 29); вывод Blender — в `$T`
через `tee` или без перенаправления».

**До спавна (оркестратор)** — пилот № 61 б, **вариант A (основной)**: у каждого арт-кодера свой корень проекта.
- `wt/` — в `.gitignore`; git worktree `wt/j1b/` (1b3) и `wt/a1g/` (A1g, своя спека) от коммита спеки;
- в каждом worktree — отдельная сессия Claude Code, открытая в его каталоге: её `$CLAUDE_PROJECT_DIR` = корень worktree,
  поэтому `guard-paths.js` и `assert-oracle-intact.js` (корень — от расположения хука, `guard-paths.js:24-25`,
  `assert-oracle-intact.js:20-21`) читают `.claude/task-scope.json` ЭТОГО worktree; файл в `.gitignore`, worktree его не
  получает — оркестратор пишет его туда до спавна: allow — три файла 1b3 `finny-pet/tools/art/{props,place,to_webp}.py`
  (у A1g — `finny-pet/tools/art/facade.py`), protect — базовый, инструменты поимённо (WORKFLOW № 29). Кодер спавнится из
  этой сессии (агенты `.claude/agents` — в дереве). Так выполняется формулировка № 61 б дословно («со своим
  `task-scope.json`»), и все три слоя защиты (tools агента, PreToolUse guard, SubagentStop) работают в своём дереве;
- механизм открытия сессии (ручное открытие владельцем в приложении или инструменты сессий) — проверить до спавна,
  результат — в журнал;
- до спавна — живой самотест зонд-агентом `agentType: 'coder'` из сессии worktree (хук ограничивает только coder,
  `guard-paths.js:75`; сама сессия его не проверит): Write в allow → пропуск, Write в protect → блок, Bash с `>` в
  `$T` → пропуск; после самотеста — `git checkout -- <файл>` в сессии worktree (из корня основного дерева —
  `git -C wt/j1b checkout -- <файл>`). Самотест красный — 1b3 идёт в основном дереве после
  1b2, отступление от № 61 б сообщить владельцу;
- **вариант B (запасной — только если отдельную сессию открыть нельзя)**: один общий `.claude/task-scope.json` основного
  дерева (круг 2: P1, E1–E3): allow = allow текущей задачи основного дерева ∪
  `wt/j1b/finny-pet/tools/art/{props,place,to_webp}.py` ∪ `wt/a1g/finny-pet/tools/art/facade.py` (при смене задачи
  1b1 → 1b2 allow пилота остаётся в объединении); в protect нет каталогов `tools/`, `finny-pet/tools/`,
  `finny-pet/tools/art/` и нет записи, хвост которой из ≥ 2 сегментов входит в allow любого кодера; самопроверка скоупа —
  скрипт пересечения хвостов (как `variants()`, `guard-paths.js:139-146`) → пусто; живой самотест — как в A (зонд-агент `coder`), в основной
  сессии, с командой A1g, после него — `git -C wt/j1b checkout -- <файл>` и `git -C wt/a1g checkout --
  finny-pet/tools/art/facade.py`. Изоляция кодеров — только `git status`
  (стоп-хук не видит `wt/`): п. 7 основного дерева — `git status --porcelain -uall` всего дерева → только allow; в
  каждой приёмке — `git -C wt/j1b status --porcelain -uall` и `git -C wt/a1g status --porcelain -uall` → совпадает с
  прошлым снимком. Арт-кодеры спавнятся только после коммита оракула 1b1 и пока в основном дереве нет незакоммиченных
  protect-правок; доки и инструменты основного дерева при работающем арт-кодере — готовить в scratch и применять одной
  командой вместе с коммитом (`cp … && git add … && git commit`);
- `tools/art_check.py:105` — `place_bakery` в `regress` (сейчас market и foma) и дампы props отдельными прогонами по
  одному предмету — `tile_apple`, `goal_custom`, `ui_coin`, каждый своей записью `regress` (`render_prop` пересоздаёт
  сцену на каждый предмет: `props.py:378`, `lib.py:34`); правка `art_check.py` — в коммите спеки, от которого строится
  worktree; самопроверка — мутант цвета `counter_top` и мутант одного из трёх построителей дают `diff` ≥ 1; BASE-дампы;
- в TOWN-A1 записаны доли бюджета APK J1 (≤ 70 000 Б, TOWN-J1.md:313) и A1g.

**CONTRACT**
1. `finny-pet/tools/art/props.py`: 6 построителей в `PROPS` (:330-338) — `pastry_croissant`, `pastry_bread`,
   `pastry_baguette`, `pastry_pretzel`, `pastry_donut`, `pastry_cupcake` (id = id меню `job_bakery`); только примитивы
   `lib.py` (заморожен, № 61 б), объект в начале координат; `VIEWS` — `(256, 0.9, 22, 38)` (сверху, как `tile_*`: видны
   дырки кренделя и пончика). Изделия различаются **силуэтом**: полумесяц; каравай-кирпичик со срезами; длинный багет
   по диагонали с косыми надрезами; крендель-петля с просветами; кольцо с глазурью; бумажная формочка с шапкой крема.
   Четыре бурые корки расходятся тоном; хлеб и багет не копируют полку фона (`place.py:90-107`).
2. `finny-pet/tools/art/place.py`: флаг `--counter OUT [--loaves]` — только с `--place bakery`: та же сцена, свет и
   камера `render_place` (:166-172); видимы только объекты с `name.split(".")[0]` ∈ {`counter`, `counter_top`} (+
   {`loaf`, `cut`} при `--loaves`), прочим — `visible_camera = False`; `film_transparent = True`, RGBA 1080 × 1920.
   `facade.py` второго генератора пилота импортирует `place.py` (`facade.py:16`) и читает только `PLACES`, `M`, `put`,
   `cloud` — их не менять; путь без флага — побайтно прежний.
3. `finny-pet/tools/art/to_webp.py`: флаг `--trim` — до записи `im = im.crop(im.getchannel("A").getbbox())`.
**SCOPE** (вариант A — `wt/j1b/.claude/task-scope.json`, пути от корня worktree; «До спавна») allow:
`finny-pet/tools/art/{props,place,to_webp}.py`. protect — базовый, инструменты поимённо:
`finny-pet/tools/art/{lib,facade,pet,room,uiprops,import_sprites,smoke,sounds}.py`, корневые `tools/*.py|sh`, finny-pet/app/,
docs/, finny-pet/docs/; каталогов `tools/`, `finny-pet/tools/`, `finny-pet/tools/art/` нет (иначе shell-запись `> $T/log` блокируется,
WORKFLOW № 29). Вариант B — allow `wt/j1b/finny-pet/tools/art/{props,place,to_webp}.py` в общем скоупе основного
дерева, protect — по правилу хвостов («До спавна»). **БЮДЖЕТ** ≤ 190 (props ≈ 150, place ≈ 30, to_webp ≈ 3).
**ANTI-SCOPE**: ассеты в `res/`, LICENSES, код приложения, фон пекарни (урок решения 27), другие построители.
**Правило пилота для спеки A1g**: allow — только `facade.py`; `props.py`, `place.py`, `to_webp.py` не трогать, проверка —
`git -C wt/a1g status --porcelain -uall` → только `facade.py`; `put("pastry_*")` до слияния не использовать.

**ACCEPTANCE** (оркестратор, из корня worktree; `$BL` — blender.exe, `$T` — scratch; рендер — очередью с A1g):
1. `python tools/art_check.py regress <BASE-копия tools/art> $T/a` и `… regress finny-pet/tools/art $T/b`, `python
   tools/art_check.py diff $T/a $T/b` → DUMP DIFF EMPTY (room, питомцы, place market/foma/**bakery**, facade_market,
   props `tile_apple`, `goal_custom`, `ui_coin`).
2. `"$BL" -b --factory-startup --python-exit-code 1 -P finny-pet/tools/art/props.py -- --only
   pastry_croissant,pastry_bread,pastry_baguette,pastry_pretzel,pastry_donut,pastry_cupcake --out $T/j1b` → exit 0;
   `… -P finny-pet/tools/art/place.py -- --place bakery --counter $T/j1b/counter_bakery.png` и с `--loaves` в
   `counter_bakery_loaves.png` → exit 0.
3. `python tools/art_check.py bbox $T/j1b/pastry_*.png` → 6 × OK.
4. прилавок (инлайн PIL), оба файла: 1080 × 1920 RGBA; α = 0 во всех строках y < 0,80·1920; нижняя строка α = 255 по
   всей ширине; самопроверка — мутант без `visible_camera = False` → FAIL.
5. `python finny-pet/tools/art/to_webp.py $T/j1b/pastry_*.png --dst $T/j1b/webp --size 256` и `… counter_bakery.png
   --dst $T/j1b/webp --trim` → каждый ≤ 37 888 Б, сумма 7 ≤ 70 000 Б; прилавок 1080 × 200–340 (≈ 285 px по проекции
   верха `counter_top`, уточнить рендером на BASE). Сумма > 70 000 Б — вопрос оркестратору: исключение из quality 88
   для прилавка или `--size 192` для выпечки (≥ 168 px под 56 dp); правило quality 88 (TOWN-A1.md:64) молча не
   нарушать.
6. из `wt/j1b/` `git status --porcelain -uall` → только allow; `git diff -w --numstat BASE` → ≤ 190; `git diff -U0 BASE
   -- finny-pet/tools/art/props.py | grep -c '^-[^-]'` → ≤ 2.
7. Лист (общий с фасадами A1g, № 61 б; фасадов A1g к приёмке 1b3 нет — лист без них, отступление от «один общий лист»
   сообщить владельцу): кадры — временная сборка в превью-worktree (HANDOFF «Окружение»): WebP в res, `pastryRes`,
   `Counter` → `Image`, `bakery_states.sh` при 1,0 / 1,3 и S23, на настоящем `bg_bakery_port` — витрина 2 × 3, поднос,
   облачко-заказ (emoji против спрайтов); прилавок-спрайт с караваями и без против полосы; питомец на итоге с
   круассаном (вопрос № 65): во временную сборку — п. 4 контракта 1b4 (EAT + `eatProp`), снимок — `tapx "Отдать" 0;
   sleep 1.5; "$A" shot …` (окно 1,2–2,7 с), подтвердить глазами, что круассан на кадре; серый и дейтеранопия — numpy
   по снимкам. Судьи: «ребёнок» вслепую с самопроверкой (WORKFLOW № 35: вырезки emoji и спрайтов вперемешку, на emoji
   линза не должна «угадывать» лучше) называет изделия (≥ 5 из 6), на кадре с круассаном — «поел ли питомец? нужно ли
   ещё покупать ему еду?»; доступность — 6 силуэтов различимы в сером, шов прилавка с полом фона (верх цвета пола,
   `place.py:141`).
**После ворот (№ 66–68) — коммит оркестратора до 1b4**: N WebP в `finny-pet/app/src/game/res/drawable-nodpi/`, N =
6·[№ 66 а] + 1·[№ 67 а]; строки LICENSES по N («Выпечка пекарни | 6 WebP `pastry_*` 256 × 256 | `props.py` →
`to_webp.py --size 256`» при № 66 а, «Прилавок пекарни (оверлей) | `counter_bakery` | `place.py --place bakery
--counter` → `to_webp.py --trim`» при № 67 а); TOWN-A1.md:68 — имена `pastry_<menuId>`, `counter_<placeId>`; доля J1 в
бюджете — факт; грепы доков `52 файла|52 WebP` (LICENSES:58, ARCHITECTURE:28) → новое число 52 + N. Слияние worktree в
`feat/town` — после ворот (при № 66 б — после ответа по 1b3-2); после второго слияния (1b3 и A1g) — `art_check.py
regress` на `feat/town` и `diff` с BASE-дампами: DUMP DIFF EMPTY для room, питомцев, place market/foma/bakery, props
`tile_apple`, `goal_custom`, `ui_coin`; `facade_market` — только правки A1g.
**В итог пилота** (№ 61 б): выпечка — в `props.py` (общие `PROPS` и `VIEWS` с товарами A1e); в волне № 61 а два
генератора в одном файле не пускать.

---

# ВОПРОСЫ НА ВОРОТА 1б (продолжают §18; № 62–64 и 69 — лист `town/j1b_gate.jpg` после 1b2, № 65–68 — общий лист арта пилота после 1b3 и 1b1; ответ пачкой по каждому листу)
62. **«Домой» после лимита у прочих работ** (`OrderCard`, курьер; § 8; кадры — `limit10` и `marketlimit`). У Марты тупика нет: карточка пропадает после
    лимита (`Town.orders` требует `paid`, `Town.kt:547-548`, TC-41); курьер в сборке недостижим (улица — только места
    без `opensBy`, `StreetScreen.kt:49`; `job_courier_help` в `eventsOff`) — показать нечего. `OrderCard` — это Panel.
    а) кнопка в `OrderCard` сейчас (≈ 3 строки, но закрепляет Panel — регрессия № 39/40); б) принцип «на месте „Начать
    смену“ — „Домой“ у всех работ» записать в §5.2 сейчас, код — со сценами Марты (A1s) и курьера (срез 3), подвариант Марты «остаётся» — в 1b1-2 с оракулом; для Марты —
    выбрать: заказ Марты после лимита остаётся со строкой лимита и «Домой» (меняются `Town.orders` и TC-41, нужен
    оракул) или заказ пропадает, выход — ⌂ шапки (по Марте рекомендую: заказ пропадает, выход — ⌂ шапки, без кода и
    оракула); в) без кнопки. **Рекомендую б.**
63. **Сцена итога** (кадры: пекарня 3★ при 1,0 / 1,3 / S23, пекарня 0★ `zero`, `levelup` 1,3, Марта, «Почему?» →
    строка питомца при 1,0 / 1,3). В составе: Боря «Спасибо!» (так в TOWN-J1 § 2), кадр 232 dp (на заказе 264 — место под крупное «✉ +N» и «+1»
    при 1,3), облачко ≈ 272 dp при 1,0, ≈ 328 (3★) и ≈ 357 (`levelup`) при 1,3 — до 75 % сцены (облачко заказа — замер
    `ui_measure` на `job13`), питомец перед ним, облачко справа, хвост облачка у итога — над «Почему?» (низ облачка на
    `floor − 8`, кончик хвоста на `floor + 4`, касается кнопки), «Почему?» и «Готово» под сценой, ответ на «Почему?»
    говорит питомец строкой внизу (в концепции — чип в облачке; при 1,3 он и ответ ушли бы в прокрутку, WORKFLOW № 38;
    строка закрывает кнопки до касания). а) житель на полу без прилавка у всех работ (сделано); у пекарни № 40 («Боря …
    за прилавком») на итоге не выполнено — на заказе прилавок есть, на итоге нет; а2) как а, но без «+1» под ★ на итоге
    (≈ −28 dp при 1,3; параметр `StarRow(r, plus = false)`, ≈ 2 строки; откатывает принятое F9/F12 концепции; кадр `levelup13`
    временной сборки со `StarRow(r, plus = false)`); б) у
    пекарни — за полосой прилавка 56 dp, как на экране заказа (кадр — временная сборка оркестратора; ≈ 3 строки; вызов `Counter` на итоге; вид — по
    ответу № 67 через 1b4 п. 3); при
    1,3 потолок ≈ 330 dp — `levelup` (≈ 357) не влезает: нужен а2 или облачко поверх прилавка; в) с замечаниями.
    **Рекомендую а**, если судьи не нашли наложений; иначе в — правки по находкам судей.
    Судьи 1b1 (2026-09-28, журнал) наложения нашли — рекомендация **в**, замечания по пунктам (каждое — кадром до/после на
    листе, ответ «да / нет» по пункту): в1) строка питомца после «Почему?» закрывает «Почему?», «Готово», питомца и строку
    мастерства (`why13`) — на итоге строке тот же отступ снизу, что в комнате (72 dp, `GameApp.kt:225`), ≈ 1 строка;
    в2) питомец перед жителем читается частью жителя, у Марты (тоже зайка) — «детёныш на руках» (`tresult`) — питомец
    сбоку от жителя, не перед ним (раскладка — временной сборкой на листе); в3) облачко на `levelup13` ≈ 82 % высоты сцены
    и пустая стена над ним — при 1,3 кадр жителя 216 dp или а2; в4) «за смену» дважды с разными числами («✉ +9 за смену»
    и «6 за смену + 3 за ★») — подпись крупного числа «всего» вместо «за смену» (UI, оракул не нужен) или без подписи;
    в5) хвост облачка упирается в «Почему?» — хвост к жителю, а не вниз.
64. **Прогресс мастерства в облачке** (кадр б — временная сборка в превью-worktree, ≈ 6 строк): а) текстом «Боря: 1 из 6 смен
    до уровня 2» (сделано); б) полоской «Боря ▰▱▱▱▱▱ до уровня 2», число — в TalkBack (не ●○: ● уже значит «обслужен» в
    ряду и «смена недели» на экране заказа; ≈ 6 строк UI; новый и высший уровень — всё равно текстом); в) без имени —
    UI снимает префикс «Имя: » (≈ 1 строка, домен и оракул не трогаем). **Рекомендую а.**
65. **Жест питомца на итоге**: а) прыжок у всех работ (сделано); б) у пекарни — пробует круассан: спрайт у лап 1,2 с и
    хруст MUNCH (отключаемый), показатели не меняются, у прочих — прыжок (как в концепции § 1–2; 1b4, ≈ 6 строк). Риск б:
    ребёнок может ждать роста сытости; в комнате питомец может сказать «На этой неделе еды ещё нет.»
    (`chatter.needFood`), если корм не куплен. **Рекомендую б**, если круассан читается на кадре питомца 96 dp; иначе а.
66. **Выпечка** (витрина, поднос, облачко; цвет, серый, дейтеранопия; ответы «ребёнка»): а) спрайты; б) ещё круг
    `props.py` по замечаниям; в) остаться на emoji (пересматривает § 8 (строка 1б); остаток — в
    BACKLOG). **Рекомендую а**, если «ребёнок» назвал ≥ 5 из 6 и силуэты различимы в сером; иначе б (1b3-2).
67. **Прилавок**: а) спрайт-оверлей той же камерой (планка сверху, низ срезан под 56 / 72 dp); б) Compose-полоса, как
    сейчас (пересматривает № 58; остаток — в BACKLOG). Риск а: верх цвета пола фона, перспектива снята для низа кадра,
    а стоит на середине экрана; шов по яркости (+5…+25 luma по замеру фона) и масштаб досок ×1,219 на S23. **Рекомендую
    а, если на листе нет шва; иначе б.**
68. **Караваи на прилавке-спрайте**: а) нет; б) да. **Рекомендую а** — центральный уходит под поднос.
69. **Пекарня в демо-видео** (Приложение А шаг 6; бюджет 161 из 180 с, GAME_CONCEPT §11): а) шаг 6 — Марта, пекарня по
    желанию на шаге 9 (как сейчас); б) шаг 6 — смена пекарни с `demoSizes`, Марта — вне видео; оценка ≈ 15–20 с
    (TOWN-J1 риск 10), замер — `tools/rec.sh` смены с `demoSizes` до листа ворот (раздел «Инструменты»); цена б —
    GAME_CONCEPT §11 шаги 6 и 10, :1433-1435, бюджет, :591, :1628 (зарплата 6 → 6 + ★); BUILD_AND_DEMO шаги 6 и 10;
    TC-28, TC-32, TC-34; REQUIREMENTS_MATRIX шаг 6. **Рекомендую б, если смена ≤ 20 с и сумма ≤ 180 с** (поднос —
    лучшая витрина игры для жюри); иначе а.

Решения спеки без вопроса (сообщить владельцу вместе с листом): заголовок итога «Спасибо!» вместо «Спасибо за помощь!»
(совпадает с TOWN-J1 § 2 и экономит строку при 1,3); крупное «✉ +N» на итоге — с мелким «за смену» в той же строке
(концепция «✉ +9 за смену», TOWN-J1.md:171; чип «✉ +N» шапки — сумма конверта недели, `PlaceScreen.kt:396`); в
«Дневнике» — только смены с ★, без пустых ☆ и без наследия Match3; окно для TalkBack — «Окно: улица, машет {имя}»;
возврат лишнего с подноса — без полёта; мастерство работ в «Дневнике» — не сейчас (кандидат в BACKLOG к эпику
«Дневник»); итерации Blender двух арт-кодеров идут одновременно (`--samples 16` / `--preview`), очередью — только
финальные рендеры приёмки и листа; если вариант A недоступен — вариант B: один общий скоуп основного дерева, изоляция
арт-кодеров — только `git status` (стоп-хук не видит `wt/`).

---

# TOWN-J1-1b4 — ассеты и жест питомца в UI (после ворот; контракт по ответам № 65–67)
**BASE**: коммит ассетов. **До спавна (оркестратор)**: зонд `python tools/art_check.py pastries [--selfcheck]` по
образцу `residents` (:337): id меню TRAY-работ = ветки `pastryRes` в TownUi.kt = файлы `pastry_*.webp`; мутанты «нет
ветки», «чужой ресурс», «ветка в комментарии», «else не null» → MISMATCH.
**CONTRACT**
1. TownUi.kt, рядом с `itemRes` (:91): `fun pastryRes(id: String): Int? = when (id) { "croissant" ->
   R.drawable.pastry_croissant … "cupcake" -> R.drawable.pastry_cupcake; else -> null }` — явный `R.drawable` (R8,
   TOWN-A1 «Общие правила»).
2. TrayScreen.kt: `Pic(null, emoji(x), …)` → `Pic(pastryRes(x), emoji(x), …)` в :206, :353, :397; emoji — запасной
   (поле обязательно, `ContentValidationTest.kt:709`).
3. № 67 а: тело `Counter` (TrayScreen.kt:115-123) → `Image(painterResource(R.drawable.counter_bakery), null,
   modifier.clipToBounds(), contentScale = ContentScale.FillWidth, alignment = Alignment.TopCenter)` — одна правка для
   раунда (56 dp) и экрана заказа (72 dp); цвета `Counter*` (:100-104) удалить. № 67 б — пункта нет. При № 68 б —
   отдельный вопрос о смещении окна прилавка (56 / 72 dp) к караваям.
4. № 65 б: `PetSprite(…, eatProp: Int = R.drawable.item_food_basic)` — параметр в конец, `PetAct.EAT -> eatProp`
   (:135); GameViewModel `finishRound`, `Done`: `if (job.game == JobGame.TRAY) emit(Effect.PetAction(PetAct.EAT)) else
   bounce++` (MUNCH — `GameApp.kt:167`; показатели не меняются); RoundScreen: `eatProp = job?.menu?.firstOrNull()?.let {
   pastryRes(it.id) } ?: R.drawable.item_food_basic`. Без анимаций — предмет стоит 1,2 с (`PetSprite.kt:96`).
**SCOPE** allow: …/ui/TownUi.kt, …/screens/TrayScreen.kt, …/ui/PetSprite.kt, …/GameViewModel.kt, …/screens/RoundScreen.kt
(пункты 3–4 — по ответам); protect — как у 1b1, но вместо каталога `…/game/ui/` —
`…/ui/{GameTextField,GameTheme,Particles,Widgets}.kt`; + `…/domain/town/Town.kt`, `…/screens/PlaceScreen.kt`;
пересечение с allow — пусто. **БЮДЖЕТ** ≤ 30.
**ACCEPTANCE**: п. 1–6, 8 и 10 как у 1b1 (600 тестов); п. 7 — status только allow, `assert-oracle-intact` exit 0,
вставок ≤ 30 (подпункты PlaceScreen и Town.kt — только 1b1); `grep -c 'Pic(null, emoji' …/TrayScreen.kt` → 0; по № 65 б —
`grep -c 'PetAction(PetAct.EAT)' …/GameViewModel.kt` → 1 (сейчас 0), `grep -c 'eatProp' …/ui/PetSprite.kt` → ≥ 2,
`tools/rec.sh` итога пекарни с анимациями и без (без анимаций предмет виден ≈ 1,2 с, питомец неподвижен), на S23 со
звуком «выкл.» MUNCH не слышен; `python tools/art_check.py pastries` → MATCH 6, `--selfcheck` → SELFCHECK OK; цикл `aapt2 dump resources` по release-APK
(`TOWN-A1f.md:187-200`) на N имён → N × OK, самопроверка на release коммита ассетов (ссылок ещё нет) → N × FAIL; прирост
APK к release BASE ≤ сумма N WebP + 6 000 Б, остаток бюджета эпика — в отчёт; живые снимки `town_route.sh` и
`bakery_states.sh first riddle limit big levelup` при 1,0 / 1,3 / S23 — «Обрезано: 0»; судьи — серый и дейтеранопия
по снимкам раунда.
**ДОКИ**: грепы `выпечка — emoji`, `прилавок — полоса`, `emoji-заглушк`, `пробует булочку` → GAME_CONCEPT §0 (:20),
§17 (:1835), TOWN-J1.md § 8 (:340), LIMITATIONS_ROADMAP:28, UX_ACCESSIBILITY (жест питомца, MUNCH отключаем).

# TOWN-J1-1b5 — полёт изделия (№ 58; последней, обязательна; не успевает к сдаче — вопрос владельцу, а не молчаливый срез)
**BASE**: коммит 1b4.
**CONTRACT**: TrayScreen.kt — при `vm.trayPut` картинка изделия летит от центра кнопки витрины (`bounds["p:$id"]`, уже
есть, :187, :202) к своему слоту ≤ 250 мс; ввод не блокируется (без `busy`/`delay` Match3, `MiniGameScreen.kt:111,
123`); без анимаций — как сейчас (`PopIn`, :167-168); VM не меняется; возврат лишнего — мгновенный. **SCOPE** allow:
TrayScreen.kt; protect — как у 1b1 + `…/domain/town/Town.kt`, `…/game/GameViewModel.kt`,
`…/screens/{RoundScreen,PlaceScreen}.kt`. **ANTI-SCOPE**: VM, возврат лишнего полётом, звук полёта, новые ресурсы.
**БЮДЖЕТ** ≤ 50. **ACCEPTANCE**: п. 1–6, 8 и 10; п. 7 — status только allow, `assert-oracle-intact`
exit 0, вставок ≤ 50 (подпункты PlaceScreen и Town.kt — только 1b1); запись `tools/rec.sh` (положить два изделия
подряд, второе — во время полёта первого) — оба на подносе; без анимаций — изделие в слоте в кадре касания (WORKFLOW
№ 31, № 39: координаты заранее, `input tap`, `fps=10`). **ДОКИ**: `REQUIREMENTS_MATRIX.md:164` (+ «полёт изделия»),
TOWN-J1 § 8 (строка 1б: полёт сделан).

---

# ЗАКРЫТИЕ СРЕЗА 1б (оркестратор; критерий «дальше», TOWN-J1.md:338)
- повторный прогон шагов 1–12 Приложения А (ТЗ :598-622) по BUILD_AND_DEMO (`game`) на эмуляторе 360 × 640 при 1,0 и
  1,3 и ключевых шагов на S23; `town_route.sh` целиком — лог 0 промахов, «Обрезано: 0» на всех снимках;
- TC-26…TC-42; TC-28 с новыми строками (итог Марты — «Спасибо!», «✉ +6 за смену», «6 за три поручения — придёт с
  новым конвертом», «Марта: 1 из 6 смен до уровня 2», «Почему?» → строка питомца, «Готово» → рынок);
- судьи по снимкам (ребёнок вслепую, доступность, TalkBack-дамп); лист `town/j1b_final.jpg` — ворота «посмотреть»;
- бюджет демо (GAME_CONCEPT §11, 161 с) — пересчитать по ответу № 69;
- грепы среза по обоим `docs/`: «бомб», «База », `quizBombReward`, `MATCH3|Match3`, «Булочки в ряд», «Монетки в ряд»,
  «ради рекорда», «рекорд» (ожидаемые попадания — тексты `classic`, спеки `docs/tasks/*`, «в `game` не используется»).

# ДОКИ СРЕЗА (сводно; по задачам — выше; правки `docs/` — только пока кодер основного дерева не работает, готовить в scratch; при варианте B пилота и работающем арт-кодере — одной командой вместе с коммитом)
- греп по всему репозиторию — `git grep` или ripgrep (учитывает .gitignore, не заходит в `wt/`); не `grep -r … .`;
- GAME_CONCEPT: §0, §3.1, §4.1, §5.2 (+ принцип по № 62), §5.5, §5.9, §6.1, §9.1 («Мастерство» — уровни и ступени меню,
  лучшая смена — «Дневник»), §9.2 № 1/4/12/13, §10.1, §11 шаг 6 и бюджет, §14, §15, §16.1, §17.2, §17.3–17.4 (метрики
  J1: медиана звёзд у 7-летних < 2, доля смен с открытым «?», «открыл ли игру на следующий день сам» — и в
  `finny-pet/docs/USER_TESTING.md`), §18 (№ 62–69 с ответами);
- TOWN-J1.md (статус, § 2, § 8 — строка 1б += полёт изделия по № 58, «Дальше»); TOWN-A1.md (A1d, доля J1, имена);
- finny-pet/docs: ECONOMY, DATA_MODEL, REQUIREMENTS_MATRIX, ARCHITECTURE, RUSTORE_CARD (:66-72 «Что внутри» — под
  `game`), TEST_CASES, BUILD_AND_DEMO (+ :176 «бомбочек 0»), UX_ACCESSIBILITY, LICENSES, LIMITATIONS_ROADMAP, USER_TESTING;
- docs/competencies.md (Область 2); docs/BACKLOG.md (J1 — статус; риск 11 — касса `CHANGE` без экрана в `game`; у
  CHANGE в `why` «Новый рекорд!» (`Town.kt:783`) — снять до включения кассы; «Домой» курьера по № 62; мастерство в
  «Дневнике»; п. 11 — чтение «✉» во всплывающей панели кошелька (`TownUi.kt:195`), не 1б, и появление LINE в TalkBack; полёт — только если владелец снимет 1b5);
  docs/HANDOFF.md — «Дальше».

## ПРИ БЛОКЕРЕ
STATUS: BLOCKED + один конкретный вопрос. Не изобретать.

## Журнал спеки
- 2026-09-28: черновик — синтез двух черновиков (углы: минимальный срез; ребёнок и сцена) и судьи. Основа — «ребёнок и
  сцена» по содержанию: по сумме рубрики он на балл ниже (26 из 35 против 27 у «минимального» — проседают проверяемость
  и точность расчёта), но выигрывает там, где решает владелец, — верность концепции и опыт ребёнка; проверяемость и
  доки пересажены из «минимального». Взято из основы: нарезка (итог / «Дневник» и окно / арт / ассеты), итог по концепции § 2 —
  крупное «✉ +N» из `ShiftPay` и строка домена без «. ✉ +N» (оракул 18 литералов, сверены грепом), «Почему?» —
  кнопкой под сценой, ответ строкой питомца (LINE), житель на полу без прилавка у всех работ, питомец 96 dp, TalkBack
  «Заработали N:» и ★ → «звёзды», монеты из крупного числа, таблица «что видит ребёнок», `pastryRes` в TownUi.kt рядом с
  `itemRes`, `to_webp.py --trim` (кроп в Python с PIL, а не в Blender), в вопросе прогресса ▰▱ вместо ●○.
  Из «минимального среза»: облачко без прокрутки и зонд «Обрезано» с самопроверкой на временной сборке; `StarRow`/
  `starsText`; `dot` → `internal`; «Дневник» без ☆ и наследия; точные `VIEWS` выпечки; release-проверка итога; полёт —
  отдельной задачей, режется первым; состояния `levelup` и наследия 60; точные грепы диффа (PlaceScreen → 2); полный
  список доков с номерами строк; ответ «Почему?» не внутри облачка.
  Правки судьи: (1) зависимость от 1a2 в обоих черновиках устарела — 1a2 закоммичен (7475838, ревью PASS). (2) № 61
  занят (dd72652, пилот параллельных генераторов) — вопросы ворот 62–69, а не 61–70. (3) Арт по № 61 б — свой worktree
  параллельно 1b1–1b2 и фасадам A1g, `lib.py` заморожен, общий лист; `facade.py` второго генератора импортирует
  `place.py` — `PLACES`/`M`/`put`/`cloud` не трогать, регресс `facade_market` и `place_bakery`. (4) Проверка
  «нет `scrollable="true"`» черновика «ребёнок» слепа: `clearAndSetSemantics` на облачке стирает семантику прокрутки
  потомков — заменена облачком без прокрутки и зондом «Обрезано». (5) Высота: «ребёнок» занижал худший случай (прогресс
  нового уровня при 1,3 — 4 строки, не 3: ≈ 385 из 390, впритык, и ценой «+1»); «минимальный» отказывался от крупного
  числа (≈ 409 > 390). Кадр жителя на итоге ≤ 232 dp: худший случай с «+1» и «✉ +10» ≈ 357 из 382 (расчёт по метрикам шрифта). «+1» не
  прячется: утверждение «ребёнка», что раунд прячет «+1» при > 1,15 (TOWN-J1.md:136), неверно — `TrayScreen.kt:223-224`
  рисует его всегда. (6) Ответ «Почему?» в облачке («минимальный») при 1,3 всегда уходил под прокрутку (≈ 328 + 122 >
  382) — ответ говорит питомец. (7) Сняты вопросы: Марта 512 на итоге (кадр 232 мягче; судьи смотрят мыльность) и
  мастерство в «Дневнике» (§ 8 не требует — BACKLOG); жест питомца оставлен вопросом с рекомендацией концепции (б).
  Не проверено: ширина глифов ✉ и ★ (запасной шрифт), чтение «✉» TalkBack, длительность демо-смены. Guard-хук: зонд
  скептика R1 на копии хука: вне корня — отказ; внутри корня с префиксом `wt/j1b/` — пропуск; с `finny-pet/tools/` в
  protect — shell-запись блокируется; живой самотест — до спавна.
- 2026-09-28, круг критиков 1 (4 измерения — решения и тексты; контракт, оракул, приёмка, инструменты; доступность и
  раскладка; арт, бюджет, реализуемость; каждый список — скептику): 55 находок (13 + 16 + 10 + 16), из них подтверждены
  35 (7 + 14 + 6 + 8), частично 20 (6 + 2 + 4 + 8), опровергнуты 0 (внутри частичных сняты отдельные доводы: «разрыв
  заказ ↔ раунд» и «ломает TC-28», «слева» и «нижние 43 %» питомца, «на 1 dp заходит», прилавок ≈ 384 px, караваи выше
  планки, 3 из 22 построителей под регрессом — на деле 5). Приняты в редакции скептиков: worktree `wt/j1b/` и `wt/a1g/`
  внутри корня, один общий скоуп пилота без каталогов `tools/`, живой самотест хука, запасной путь — 1b3 после 1b2;
  ворота двумя листами (№ 62–64, 69 — после 1b2; № 65–68 — лист арта) и правило пилота для спеки A1g; кадры листа арта
  — временная сборка вместо правки `mock_bakery.py`; N ассетов по ответам; строка 1b3-2; регресс `props.py` (дамп трёх
  построителей и счёт удалений), проверка после второго слияния; прилавок — видимость по `name.split(".")`, α по
  0,80·1920, ≈ 285 px, импорт `facade.py` → `place.py`, TOWN-A1.md:68, `pastryRes` у `itemRes`; доли бюджета J1 и A1g
  и вопрос при > 70 000 Б; итерации Blender кодера — превью, финал — оркестратор, итог пилота про два генератора в
  одном файле; критический путь; грепы доков 1b1 и коммита ассетов; «за смену» в строке крупного числа; 1b5
  обязательна; № 62–65 и 69 дополнены (а2, № 40, потолок б, вариант в, Марта после лимита, риск `needFood`, оценка и
  цена демо); TalkBack — имя жителя, «за звезду», все строки `why`, `paneTitle`; раскладка — питомец по центру, H = 478,
  ряд кнопок 61 + 3, низ облачка `floor − 8`, запасной кадр 216; приёмка и инструменты — строки 1b2 отдельным коммитом,
  KDoc и грепы в кавычках, п. 7 у 1b2/1b4/1b5, `open_diary` с тремя подслучаями, DUMP в `shot2` маршрута, `text_xy`
  для строки питомца, `rec.sh` без анимаций, самопроверка «Обрезано» в п. 11б, бюджет 1b1 ≤ 100, `bounce++` → 2,
  protect поимённо; KDoc ○, стоп-слова § 9.2 № 5 и № 8, риск 11 в BACKLOG. Отклонено: вариант 1 скептика R1 (1b3 в
  основном дереве сразу) — только запасной путь; quality 80 для прилавка — против правила эпика quality 88; «Спасибо!» с
  именем в видимом тексте — видимый титул «Спасибо!», имя — только в TalkBack. R11 (шов по яркости
  и масштаб ×1,219 в риске № 67) — принято (круг 2, P13).
- 2026-09-28, круг критиков 2 (2 критика по всему документу — продукт: решения, тексты, доступность, ворота; инженерия:
  контракт, скоупы, приёмка, инструменты, арт-пилот; каждый список — скептику): 31 находка (17 + 14), подтверждено 16
  (7 + 9), частично 15 (10 + 5), опровергнуто 0 (внутри частичных сняты доводы: коммит инструментов 1b2 «вне правила» —
  его покрывает HANDOFF:135-136; «прилавок на итоге без контракта» — `Counter` общий, вид даёт 1b4 п. 3; греп
  `PetAct.EAT` → 1 — он уже 1, `GameViewModel.kt:413`; `no_hud_hint` на `tresult` — уже есть; «иначе а2 или кадр 216» у
  № 63 — они лечат высоту, не наложения; «R11 уходит к кодеру»; «ограничение „Почему?“ не учтено»; «TalkBack нигде не
  читает „✉“» — читает в панели кошелька; двойное чтение `paneTitle` — никем не проверено; «доки станут ложью» у 1b5 —
  только REQUIREMENTS_MATRIX:164; `sed` в чужой `facade.py` как довод E2; «координаты неоткуда взять», «≈ 1,5 с на
  касание» у 1b5 и неисполнимость № 69; снимок «≤ 0,5 с» — застанет раунд; порядок `.gitignore`; грепы спеки в `wt/` —
  они ограничены путями). Приняты 31 (части P2, E4–E6, E9 — нет, ниже) в редакции скептиков и решений оркестратора: строка 1b1-2 и критический путь
  через ворота арта (P3, P10); кадры № 62 (`limit10`, `marketlimit`, рекомендация по Марте), № 63 а2 и № 64 б —
  временная сборка, № 65 — EAT + `eatProp` во временной сборке, окно 1,2–2,7 с (P4, P5, E7); № 66 в / № 67 б
  пересматривают § 8 / № 58, «иначе» у № 63, 65, 66, R11 — в риск № 67 (P11–P13); приёмка № 65 б в 1b4 (P6);
  состояние `zero` (P7); «Почему?» и `no_hud_hint` на `tresult`/`result2` (P8, E13); строка «выход» (P9); `paneTitle`
  коротким, двойное чтение — в живую проверку, «✉» — панель кошелька в BACKLOG, «Почему?» без озвучки — в
  UX_ACCESSIBILITY и вопрос судье (P14–P16); protect, ANTI-SCOPE и ДОКИ 1b5 (P17, E10); общий скоуп без перекрёстных
  хвостов и каталога `tools/art/`, самопроверка хвостов, изоляция `git status` всех деревьев, спавн после коммита
  оракула 1b1, доки и инструменты — `cp && commit` — вариантом B (P1, P2, E1–E3); правило № 29 кодеру 1b3 (E11); дампы
  props по одному предмету, правка `art_check.py` в коммите спеки, props — в проверку после второго слияния (E4, E5,
  E9); `xy:` в `rec.sh` и `sleep:25` для № 69 (E6); самопроверка «ребёнка» на листе арта (E8); восстановление файла
  после самотеста (E12); `git grep`/ripgrep (E14). Не взяты: правка HANDOFF:135-136 (P2); FPS и numpy-разница в
  `rec.sh`, два `input tap` одной командой у 1b5 (E6); мутант `tile_apple` и счёт 14 дампов (E4, E5); сверка
  `facade_market` с дампами A1g (E9). Решение оркестратора по пилоту: вариант A (основной) — у каждого арт-кодера свой
  корень проекта: worktree `wt/j1b/` и `wt/a1g/`, в каждом своя сессия Claude Code со своим `task-scope.json` (№ 61 б
  дословно, три слоя защиты — в своём дереве), живой самотест в сессии worktree с восстановлением файла, механизм
  открытия сессии — проверить до спавна; вариант B (запасной — только если сессию открыть нельзя) — общий скоуп
  основного дерева по P1/E1–E3; одновременные итерации Blender и вариант B — владельцу в «Решениях спеки без вопроса».
  Попутно: очередь доков 1b1 — только за кодером основного дерева (при варианте A арт-кодеры правок основного дерева не
  видят), строка `art_check.py:106` → :105 (строка place market/foma).
- 2026-09-28, проверка применения круга 2 (аналитик: каждое решение по тексту r3, внутренние противоречия, 12 ссылок
  file:line — все сходятся): исправлены оркестратором — самотест хука зонд-агентом `coder` (хук ограничивает только
  coder, `guard-paths.js:75`); состояние `zero` — полный поднос с изделием вне заказа (неполный домен не принимает,
  `Tray.kt:50`); восстановление файлов после самотеста в обоих деревьях; «запасной путь» в критическом пути — это
  красный самотест, а не вариант B; 1b1-2 и подвариант Марты — `Town.kt` и оракул; BASE у 1b3 и 1b5; 1b3-2 в
  критическом пути; счёт новых строк TalkBack; protect 1b3 — `finny-pet/app/`, `finny-pet/docs/`; счёт принятого в
  журнале круга 2.
- 2026-09-28, сессия 12 — приёмка 1b1. Инструменты 1b1 — 3afdd6c; оракул — 5ababb9 (test-author: 18 литералов; прогон
  оркестратора на 3afdd6c: tests 600, failed 11, все `ComparisonFailure` строки итога, имена — в коммите); кодер (Opus,
  high) — 99 вставок, п. 1–10 зелёные (600 / 0 / 0 в обоих вариантах, три сборки exit 0, lint 0 ошибок, 5 / 5; предупреждение
  компилятора `PlaceScreen.kt:183` — из BASE). Живая проверка, эмулятор 360 × 640: `town_route.sh` — промахов 0, 51 снимок
  «Обрезано: 0» (result, why, tresult, result2 при 1,0 / 1,3); `bakery_states.sh levelup zero` — «Обрезано: 0»; самопроверка
  зонда — сборка с `heightIn(max = 200.dp)` даёт «Обрезано: 2» на levelup10 и levelup13 (пойман); `ui_measure` levelup13
  и result13 — мишеней < 48 dp и узлов за краем нет, облачко levelup13 232 × 336 dp (расчёт спеки ≈ 357); без анимаций —
  разница по области питомца 0 за 7 с после появления итога, с анимациями 123 931 px (самопроверка), «✉ +10» и чип шапки
  числом сразу; release (pkgFlags без DEBUGGABLE) — итог в сцене, житель и питомец видны. Один ANR на улице при первом
  прогоне `zero`: главный поток ждал `HardwareRenderer.syncAndDrawFrame` (GPU хоста занят рендером Blender агента
  исследования), повтор чистый. S23 (после круга правок): `town_route.sh` — промахов 0, 55 снимков «Обрезано: 0», `levelup`/`zero` при 1,0 / 1,3 — «Обрезано: 0», `ui_measure` levelup13 и result13 — мишеней < 48 dp и узлов за краем нет; профиль команды возвращён (`cmp`), громкость 5 и шрифт 1,0 возвращены; на профиле команды питомец — щенок перед щенком Борей (к № 63 в2). Судьи по снимкам эмулятора (ребёнок
  вслепую — самопроверка на кадрах старой карточки пройдена; доступность — серый и дейтеранопия numpy, контраст облачка
  проходит; TalkBack-дамп; сцена и решения владельца; сводка со скептиком): FIX в пределах контракта — 0; в ворота № 63 в —
  в1–в5 (строка «Почему?» закрывает кнопки и питомца; питомец перед жителем, у Марты «детёныш»; облачко levelup13 ≈ 82 %
  сцены; «за смену» дважды; хвост в «Почему?»); № 64 — «Теперь 7–11 за смену» ребёнок читает как свою плату; «Почему?»
  при TalkBack лишь повторяет описание облачка (по контракту) — к № 63 в; BACKLOG — ○ на досрочном итоге ребёнку не
  объяснён, ● «я плохо сделал?» (USER_TESTING), чип «✉ +15» шапки рядом с «+6 за смену», Марта 512 мягче Бори 768, у
  картинки строки питомца нет имени (`TownUi.kt:288`). Ребёнок понял: кто благодарит, «+N за смену», ★ = монета, «Готово»;
  не понял: когда придёт конверт, ○, что делает «Почему?». Правка оркестратора без вопроса — лицо питомца на итоге
  `Face.HAPPY` (§ 4): с `vm.face` голодный питомец грустил после 4★ (ТЗ 3.5); круг кодера ≤ 30.
- 2026-09-28, сессия 12 — ревью 1b1: круг 1 — FAIL только за доки (статусы «живая проверка, судьи и ревью впереди» в
  TOWN-J1.md:14, :348 и «UI не проверен» в REQUIREMENTS_MATRIX.md:41 отстали от журнала; исправлено 4e63a02), круг 2 —
  PASS; код — 563c5dd. Пилот № 61 б, «До спавна» 1b3 и A1g1: BASE обоих — f0a6585 (спека A1g1, регресс 7 фасадов, доли
  бюджета); worktree `wt/j1b` и `wt/a1g` (ветки `pilot/j1b`, `pilot/a1g`, коммит профиля арт-кодера — Opus, xhigh,
  maxTurns 150, в feat/town не сливается) перенесены на BASE; `task-scope.json` каждого — allow своих файлов, protect
  поимённо (каталогов `tools/` нет), пересечений и хвостов нет. Механизм варианта A: `claude -p` из Bash (встроенный
  Claude Code 2.1.281) — «Not logged in», инструмента создания сессий нет; сессии открыл владелец в приложении, задания
  — сообщениями. Самотест хука зонд-агентом coder в каждой сессии: Edit allow — пропуск; Write чужого файла пилота и файла
  вне корня — блок; Bash `echo > scratch` и `echo … 2>&1 | tail` — пропуск; `cat <protect> > /dev/null` — блок
  (правило № 29); `git checkout` — блок; в `$CLAUDE_PROJECT_DIR` Bash сессии пусто, хук видит корень worktree (Edit
  `to_webp.py`/`facade.py` прошёл — в основном дереве они под защитой). Стоп-хук: скрипт worktree вручную — exit 2
  «ORACLE INTEGRITY FAILED»; сообщения в обёртку не пришло — повтор зондом «OK» в шаге 1б. Регресс (самопроверка):
  BASE-дампы 20 × ok; мутанты (табличка зоопарка, стена пекарни в `PLACES`) — ровно `facade_bakery`, `facade_gate_zoo
  ['sign']`, `place_bakery`; WebP 7 фасадов BASE — 124 912 Б (спека — 124 812 по HEAD: +100 Б у рынка, шум рендера;
  доля не меняется); два Blender на одном HIP одновременно — rc 0 / 0, по 9 с, `cycles device: HIP`.
