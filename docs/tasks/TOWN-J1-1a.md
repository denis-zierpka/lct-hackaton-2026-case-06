```
TASK: TOWN-J1-1a — «Поднос по заказу» в игре: раунд и ViewModel, экран заказа в сцене, загадка значком «?», экран после лимита
EPIC: TOWN-J1 (docs/tasks/TOWN-J1.md — концепция § 2 «Экраны», § 8 строка 1а, решения владельца № 40–48)
BASE: e4bc44f (коммит оракула, 600 тестов; для кодера и ревьювера); старт — 8dd059c (спека, контент, маршрут)
BRANCH: feat/town
ЗАВИСИТ ОТ: TOWN-J1-0 (контракт Tray.kt, trayRound, № 47 б — сделан, ревью PASS)

## КОНТЕКСТ
Срез 1а эпика TOWN-J1: пекарня переходит с Match3 на «Поднос по заказу» — в content.json `job_bakery`
становится TRAY с названием «Помочь Боре», ребёнок играет раунд из среза 0 на экране: покупатели по одному
у прилавка, заказ картинками, витрина, поднос, «Отдать». Экран заказа — сцена (Боря за прилавком и
небольшое облачко «Помоги Боре в пекарне!», «6–10», жетоны, одна кнопка), загадка Бори — значком «?» внутри
раунда (№ 48 б), после лимита смен — строка вместо кнопки (№ 47 б). Итог смены — прежняя карточка
(`job_result`, итог в сцене — срез 1б), выпечка — emoji-заглушки через `Pic`, прилавок — полоса Compose
цветами макета (спрайт-оверлей — срез 1б).

## ТРЕБОВАНИЕ ТЗ (docs/sources/ТЗ_текст.txt)
2.1 «короткую и понятную обратную связь по итогам принятых решений» (реакция покупателя после каждой
отдачи; лимит смен — «за один игровой период нельзя купить все сразу»); 2.2 «Безопасная ошибка» (неверная
отдача не штрафуется: покупатель показывает, чего не хватает, поднос дособирается) и «никаких длинных
правил» (заказ картинками, первая смена — указатель); 2.5.4 «Для каждого начисления указываются источник и
сумма» (журнал «Смена: Помочь Боре», итог «6 за смену + 3 за ★. ✉ +9 — придёт с новым конвертом»); 2.5.8
(смена — заказ-задание, решение 20); 2.5.13 (без календарных сроков: раскладка от seed профиля, демо —
`demoSizes`); 3.4 «визуальный отклик не более чем за 1 секунду» (отклик на каждое касание); 3.5 «не должны
запугивать, стыдить» (без таймера, крестов, красного и слова «ошибка»); 3.6 (мишени ≥ 48 dp, текст ≥ 16 sp с
капом 1,3, форма вместо цвета, контраст, анимации и звук отключаются, путь TalkBack). Рамка — «как человек может
повлиять на увеличение оплаты своего труда» (звезда = монета, уровни). Стоп-лист GAME_CONCEPT §9.2 № 1, 3, 4, 10,
12, 13, 15, 17, 18.

## CONTRACT
Внешняя пара «» у строк — разделитель. Все строки ребёнку — дословно, других новых строк нет. Правило
домена — только добавление (GAME_CONCEPT §17.1). R = town.rules. Обозначения ниже — функции ViewModel § 2:
`residentName(id)` — имя жителя по id; `pastryTitle(id)` — `Pastry.title` изделия из меню работы раунда;
`trayList(ids)` = `ids.map { pastryTitle(it).lowercase() }.joinToString(", ")`; `dot(t)` = t, если t кончается
на «.», «!» или «?», иначе t + «.».

### 0. Контент (коммитит оркестратор ВМЕСТЕ СО СПЕКОЙ, до оракула; кодеру content.json закрыт)
- `job_bakery`: `"title": "Помочь Боре"`, `"game": "TRAY"`, поля `board`, `moves`, `demoMoves` удалены
  (меню, ступени, demoSizes и `_todo` — как в срезе 0).
- `events[job_bakery_help].intro`: «Помоги Боре в пекарне!» (№ 42 в; title события «Пекарне нужен помощник»
  остаётся — это карточка «В городке»).
- `residents[borya].lines[0]`: «Помоги Боре в пекарне!» — экран заказа показывает её, пока заказа на доске нет
  (до плана недели 1: `requires` заказа «afterPlan || week>=2», `PlaceScreen.kt:361`).
Замер на этом контенте (оракул среза 0 + код eec1239): 581 тест, красных 20 — ShiftTest 16, TrayTest 2,
TownContentTest 1, EventCatalogTest 1 (список — ORACLE); ContentValidationTest, BalanceSimTest,
FiveDemoWeeksTownTest, TownDayTest — зелёные.

### 1. Домен (группа 1 кодера; Tray.kt, Town.kt)
```kotlin
// Tray.kt
/** Ответ на загадку Бори в раунде подноса: исход Town и раунд после него (TOWN-J1-1a § 1). */
data class TrayAnswer(val result: TownResult, val round: TrayRound)

object Tray {
    // put, take, give, hint — без изменений
    /** Изделия текущего заказа без пары на подносе (мультимножество, порядок o.items); r.done → пусто. */
    fun missing(r: TrayRound): List<String>
}

// Town.kt (методы класса Town)
fun riddleInRound(s: GameState, r: TrayRound): QuizQuestion?
fun answerInRound(s: GameState, r: TrayRound, questionId: String, optionIndex: Int): TrayAnswer
```
- `missing(r)` = `missAndExtra(o.items, r.tray).first` (правило сравнения — TOWN-J1-0 § 2).
- `riddleInRound(s, r)` = `nextQuestion(s)`, если `r.riddle && r.index >= 1 && !r.done`; иначе null (№ 48 б:
  не в первой смене — `r.riddle`, после первого обслуженного, пока смена идёт; после ответа `riddleAsked` →
  null до конца смены).
- `answerInRound(s, r, questionId, optionIndex)`, по порядку:
  1. `riddleInRound(s, r)?.id != questionId` → `TrayAnswer(TownResult.Refused(NO_RIDDLE), r)`;
  2. `res = answerQuestion(s, questionId, optionIndex)`; `res` — Refused или ответ неверный (последняя запись
     `riddles` исхода с `correct = false`) → `TrayAnswer(res, r)`;
  3. верный: `h = Tray.hint(r, R.riddleHint)`; `h != r` → `TrayAnswer(res, h)`; `h == r` (класть нечего: заказ
     собран; или на подносе size − 1 верных и пустой слот; или size − 1 верных и одно лишнее на полном подносе —
     подсказка не собирает заказ целиком, стоп-лист № 12) → `TrayAnswer(Done(res.outcome.copy(why = listOf(HINT_IDLE))), r)`
     (line исхода — прежняя «Верно! {explanation}»).
- Строки (константы companion Town): NO_RIDDLE = «Загадки сейчас нет» (ребёнку не показывается — UI зовёт ответ
  только при видимом «?»); HINT_IDLE = «Этот поднос Боря оставил тебе» (верна во всех трёх случаях h == r).
- Остальное в исходе — как answerQuestion: riddles + запись, riddleAsked = true, bombs у TRAY не меняются,
  line «Верно! {explanation}» / «{explanation}», why [«Подсказка Бори — на поднос»] / [«Вопрос вернётся позже»].
- Прочие функции Town, Tray, TownState, TownContent — без изменений.

### 2. ViewModel (GameViewModel.kt, GameApp.kt; группа 2)
Раунд живёт во ViewModel, как `match`, в профиль не пишется (риск 9 концепции). Состояние облачка загадки —
тоже во ViewModel: смена шрифта пересоздаёт Activity (в `configChanges` нет fontScale), ViewModel переживает её.
```kotlin
/** Покупатель, которому только что отдали верный поднос: «Спасибо!» на THANKS_MS, потом следующий. */
data class TrayThanks(val customer: String, val star: Boolean)
/** Статичная подсказка подноса: SHORT — «Отдать» при неполном (пустые слоты), FULL — касание при полном. */
enum class TrayCue { NONE, SHORT, FULL }

var tray: TrayRound?        by mutableStateOf(null); private set   // раунд TRAY или null
var trayMiss: Give?         by mutableStateOf(null); private set   // отдача с расхождением у текущего покупателя
var trayThanks: TrayThanks? by mutableStateOf(null); private set   // пауза «Спасибо!»
var trayCue: TrayCue        by mutableStateOf(TrayCue.NONE); private set
var trayNote: String        by mutableStateOf(""); private set     // текст live region (§ 6)
var trayWobble: Int         by mutableIntStateOf(0); private set   // +1 — покачать поднос (только с анимациями)
var riddleOpen: QuizQuestion? by mutableStateOf(null); private set // облачко загадки открыто, его вопрос
var riddleLine: Line?       by mutableStateOf(null); private set   // ответ в облачке (после ответа)

fun trayPut(pastry: String)
fun trayTake(slot: Int)
fun trayGive()
fun trayThanksDone()
fun trayRiddle(): QuizQuestion?          // = tray?.let { town.riddleInRound(state, it) }
fun openRiddle()                         // riddleOpen = trayRiddle(); riddleLine = null
fun closeRiddle()                        // riddleOpen = null; riddleLine = null
fun trayAnswer(i: Int)
fun trayIntro(jobId: String): String?    // = town.trayRound(state, jobId)?.intro
fun trayText(r: TrayRound): String
fun trayList(ids: List<String>): String
fun residentName(id: String): String     // имя из tc.residents, иначе id
fun pastryTitle(id: String): String      // title из меню работы tray (tc.jobs), иначе id
```
- `startRound(jobId)`: для ВСЕХ работ до ветвления (рядом с `match = null`) — `tray = null`, `trayMiss = null`,
  `trayThanks = null`, `trayCue = NONE`, `trayNote = ""`, `riddleOpen = null`, `riddleLine = null`. Ветка
  `job.game == TRAY` — вставка `else if` без переотступа веток MATCH3/TAPS: `tray = town.trayRound(state, jobId)`;
  null → `sfx(FAIL)` и выход (на валидном контенте при `q.canPlay` не бывает); иначе `trayNote =
  «{residentName(o.customer)}. Заказ: {trayList(o.items)}»` (o — первый заказ) и `navigate(Screen.Round(jobId))`.
- `trayPut`, `trayTake`, `trayGive` — проверки строго по порядку: (1) `r = tray ?: return`; (2) пауза:
  `trayThanks != null` → `sfx(TAP)`, `trayThanksDone()`, return (своё действие не выполняется: заказа нового
  покупателя ребёнок ещё не видел; отклик ТЗ 3.4 — звук и приход покупателя); (3) `r.done` → `trayPut`/`trayTake`
  return, `trayGive` → `finishRound()`, return; (4) только теперь `o = r.orders[r.index]`.
- `trayPut(p)`: `n = Tray.put(r, p)`; `n == r` → `trayCue = FULL`, `trayWobble++`, `sfx(POP)`, `trayNote =
  «Поднос полный»`; иначе `tray = n`, `trayCue = NONE`, `sfx(TAP)`, `trayNote = trayText(n)`.
- `trayTake(slot)`: `n = Tray.take(r, slot)`; `n != r` → `tray = n`, `trayCue = NONE`, `sfx(TAP)`, `trayNote = trayText(n)`.
- `trayGive()`: `g = Tray.give(r)`.
  - `!g.accepted` → `trayCue = SHORT`, `sfx(POP)`, `trayNote = «Поднос ещё не полный: свободно {o.items.size − r.tray.size}»`.
  - `g.served` → `tray = g.round`, `trayMiss = null`, `trayCue = NONE`, `trayThanks = TrayThanks(o.customer, g.star)`,
    `sfx(SUCCESS)`, `emit(Effect.Hearts("customer"))`, `trayNote = «{residentName(o.customer)}: спасибо! Звезда»`
    при звезде, иначе `«{residentName(o.customer)}: спасибо!»`.
  - иначе → `tray = g.round`, `trayMiss = g`, `trayCue = NONE`, `sfx(POP)`, `trayNote = «Ещё нужно: {trayList(g.missing)}»`.
- `trayThanksDone()`: `trayThanks ?: return`; `trayThanks = null`; `tray?.done == true` → `finishRound()`; иначе
  `trayNote = «{residentName(o.customer)}. Заказ: {trayList(o.items)}»` (o — заказ нового покупателя).
- `trayAnswer(i)`: `r = tray ?: return`; `q = riddleOpen ?: return`; `a = town.answerInRound(state, r, q.id, i)`.
  Done → `commit(a.result.outcome.state)`; `a.round != r` → `tray = a.round`, `trayCue = NONE`, `trayNote =
  trayText(a.round)`; `sfx(SUCCESS)`, если последняя запись `state.riddles` верна, иначе `sfx(POP)`; `riddleLine =
  Line(outcome.line, outcome.why)`. Refused → `sfx(FAIL)`, `riddleLine = Line(refused.line)`. Без `act()`,
  `say()` и LINE.
- `finishRound()`: у TRAY `score = tray?.stars ?: 0`, `bombs = 0`; после исхода (обе ветки, рядом с `match = null`)
  — `trayMiss = null`, `trayThanks = null`, `trayCue = NONE`, `riddleOpen = null`, `riddleLine = null`; `tray`
  остаётся до `closeRound` (концепция § 3: звёзды раунда живут до `closeRound`).
- `closeRound()`: дополнительно `tray = null`.
- `trayText(r)`: `r.done` — «На подносе пусто»; пустой поднос — «На подносе пусто. Свободно {size}»;
  0 < tray.size < size — «На подносе: {trayList(r.tray)}. Свободно {size − tray.size}»; полный — «На подносе:
  {trayList(r.tray)}. Поднос полный».
- `Effect.Hearts` → `data class Hearts(val target: String = "pet")`; `petTapped()` шлёт `Effect.Hearts()`;
  GameApp.kt: `is Effect.Hearts -> particles.targetOf(e.target)?.let { particles.hearts(it) }`.
- Прочие методы ViewModel не меняются; старые `answerRiddle` и `Riddle` (карточка Match3) остаются
  недостижимым кодом до чистки Match3 (срез 2).

### 3. Экран заказа в сцене (PlaceScreen.kt; группа 2)
`JobPlace` для работы с `job.game == TRAY`: `BoxWithConstraints(Modifier.weight(1f).fillMaxWidth())` (h — его
высота), внутри `Column(Modifier.verticalScroll(…).padding(8.dp))`: заголовок места («Пекарня», headlineSmall,
G.ink) и `PlaceEvents` (их общая высота e — замер `onSizeChanged`), затем `TrayJobScene(vm, job, order,
Modifier.height(max(h − 16.dp − e, 360.dp)))`. Без событий сцена занимает остаток экрана; события места
(контент, ТЗ 2.5.14) сдвигают сцену вниз, а не сжимают её (сцена не ниже 360 dp, колонка прокручивается). У
прочих работ `JobPlace` (курьер в парке) — прежняя колонка и `OrderCard`, в котором «Начать смену» рисуется
только при `q.canPlay` (№ 47 б для всех работ; одна строка).
- q = `vm.shiftQuote(job.id)`; text = `order?.intro ?: resident.lines.firstOrNull() ?: job.title`;
  intro = `vm.trayIntro(job.id)` (реплика новинки ступени — «Новинка — крендель!» перед 2-й сменой; null, когда
  смену начать нельзя или ступень не новая).
- Сцена — `BoxWithConstraints` (H — её высота, ≈ 426 dp на 360 × 640 при 1,0, ≈ 422 при 1,3; W ≈ 344):
  | Слой (снизу вверх) | Где | Что |
  |---|---|---|
  | Боря | x = 8 − 0,3·F (фигура начинается у ≈ 13 dp); низ кадра = верх прилавка + 56 dp | `ResidentPic(borya, F)`, F = (H − 72 − 72 + 56 − 8).coerceIn(160, 264) dp |
  | Прилавок | во всю ширину, высота 72 dp; низ — над кнопкой (отступ 8) или у низа сцены без кнопки (отступ 8) | полоса § 4 «Прилавок», без изделий на ней |
  | Облачко | x от 8 + 0,45·F до W; y от 8; низ ≤ верх прилавка − 4 dp, иначе прокрутка внутри облачка | белое, радиус 20, тень 8, отступ 14; хвост к Боре |
  | «Начать смену» | низ, во всю ширину | `GameButton` PRIMARY, minHeight 56, только при `q.canPlay` |
- Облачко при `q.canPlay`: text (titleMedium, G.ink); intro, если не null (bodyLarge, G.purple); строка
  «{q.base}–{q.top}» (headlineMedium, G.purple, тире U+2013) + монета `ui_coin` 32 dp; строка «Смены» (bodyLarge)
  + `ShiftTokens(vm, label = false)`. При `!q.canPlay` (лимит, ночь, закрыто): `q.line` (bodyLarge; после
  лимита — «Смены на неделе закончились — новые с новым конвертом») и `ShiftTokens(vm, label = false)`; кнопки нет.
- Облачко — один узел TalkBack (§ 6); картинка Бори — «Боря» (как `ResidentPic`).

### 4. Раунд «Поднос» (новый TrayScreen.kt; RoundScreen.kt; группа 2)
`RoundScreen`: ветка `job?.game == JobGame.TRAY && vm.tray != null -> TrayScreen(vm, Modifier.weight(1f))` —
после `result != null` и до `vm.match != null`; остальные ветки без изменений. Итог — прежняя карточка `job_result`.
Раскладка — корневой `BoxWithConstraints` TrayScreen (H ≈ 478 dp на 360 × 640 — замер wm360: статус-бар 34,
навигация 24; ≈ 633 на S23 — мерить на сборке); в нём колонка сверху вниз:
| Зона | Размер | Что |
|---|---|---|
| Сцена | всё, что выше витрины (A ≈ 243 dp на 360 × 640) | Box, `isTraversalGroup = true`: «Закончить», облачко покупателя, покупатель, Боря, «?», прилавок с подносом |
| Витрина | ВСЕГДА 2 ряда по B, зазор 8, отступы 8 (при kinds = 3 второй ряд пуст — A и кадры не меняются от смены к смене); B = (H·0,145).coerceIn(64, 88) dp (≈ 69 на 360 × 640) | 3 колонки |
| Ряд смены | min 56 dp, отступы 8 | `testTag("job_row")`, фон G.paperTint, радиус 16: символы слева, «Отдать» справа |
Поверх колонки в корневом Box — указатель, live region и облачко загадки (§ 5).
Индекс показанного покупателя: `shown = if (vm.trayThanks != null) r.index − 1 else minOf(r.index, r.orders.lastIndex)`.
Кадр покупателя P = min((A·0,62).coerceIn(128, 220), верх прилавка + 48 − 84) dp (84 — низ облачка-заказа при
y = 8 и картинке 48 + отступ); ≈ 150 на 360 × 640, ≈ 220 на S23; кадр Бори = 0,8·P.
- «Закончить»: правый верхний угол сцены (отступ 8), `GameButton("Закончить", style = PAPER, minHeight = 48)` →
  `vm.finishRound()`; `semantics { traversalIndex = -1f }` — TalkBack читает его первым в сцене. Не в ряду с «Отдать».
- Прилавок: полоса во всю ширину высотой 56 dp у низа сцены, цвета макета `tools/mock_bakery.py:89-96`: перед
  #F4A261, нижняя кромка #D68046 (≈ 9 dp), верхняя доска #D9A36C с тенью #C48C58 (≈ 19 dp), вертикальные планки
  #E29254 через ≈ 80 dp. Константы цветов — в TrayScreen.kt (UI, не экономика). Без семантики.
- Покупатель: `AnimatedContent(targetState = shown, transitionSpec = { tr })` → `ResidentPic(resident(r.orders[i].customer), P)`
  слева (x 0), низ кадра = верх прилавка + 48 dp (ноги за прилавком: прилавок рисуется поверх); `tr` вычислен до
  AnimatedContent: `if (animate) fadeIn(tween(200)) togetherWith fadeOut(tween(120)) else EnterTransition.None
  togetherWith ExitTransition.None`. Без описания (`clearAndSetSemantics {}`) — имя читает облачко.
  `particleTarget(particles, "customer")`. При анимациях — на `trayThanks` прыжок (translationY 0 → −16 dp → 0,
  ≤ 400 мс). На время паузы P не пересчитывается.
- Боря: `ResidentPic(borya, 0,8·P)` справа (x = W − кадр), тот же низ; без описания.
- Облачко покупателя: слева, низ облачка = верх кадра покупателя + 0,05·P − 4 dp (у головы; 0,05 — прозрачный
  верх кадра `res_*`), но не выше y 8; белое, радиус 16, тень 4, отступ 8, хвост вниз к покупателю:
  - при `trayThanks == null && !r.done` — заказ `r.orders[shown]`: картинки (`Pic(null, emoji, s)`, s = 48 dp при ≤ 3
    изделиях, 40 dp при 4 — картинки не мишени; облачко из 4 — 4·40 + 3·4 + 16 = 188 dp, «Закончить» при 1,3 ≈
    150 dp: 8 + 188 < 360 − 8 − 150), зазор 4, одинаковые рядом, по картинке на штуку. После отдачи с
    расхождением (`trayMiss != null`) — по `trayMiss.missing` (итог той отдачи, до следующей отдачи не
    пересчитывается; живой `Tray.missing` здесь не берётся — № 43 а): у каждого вида первые (сколько в заказе −
    сколько в trayMiss.missing) картинок — значок ✓ (круг 20 dp G.greenDark, белая галочка — Canvas-путь, 2 dp,
    правый нижний угол картинки), остальные — пунктирный круг ВНУТРИ картинки (диаметр s, обводка 3 dp G.purple,
    радиус по оси штриха s/2 − 1,5 dp, штрих 6/4 dp, `drawBehind`; соседние круги не накладываются). Крестов,
    красного и «?» нет.
  - при `trayThanks != null` — в одну строку «Спасибо!» (titleMedium) и при звезде «★ +1» (★ — как в ряду смены,
    «+1» titleMedium G.purpleDeep).
- Значок «?»: при `vm.trayRiddle() != null` (и во время паузы) — круг 48 dp G.purple с «?» (titleLarge, белый);
  левый край = левый край кадра Бори, низ = min(верх кадра Бори + 48, верх плашки подноса − 4); статичный (без
  пульса); касание → `vm.openRiddle()` (§ 5).
- Поднос: по центру по горизонтали, центр — на верхнем крае прилавка; плашка #FAF4E8, обводка 2 dp #D2BEA0
  (при `trayCue == FULL` — 3 dp G.purple), радиус 32. Слотов — `r.orders[shown].items.size` (в паузе — пустые слоты
  отблагодарившего покупателя: его поднос уже отдан; при `r.done` без паузы поднос не рисуется); слот 48 dp, зазор 8,
  отступ 8. Заполненный слот i — `Pic(null, emoji, 40.dp)` в круге 48 dp, кликабельный → `vm.trayTake(i)` (§ 6). Пустой
  — пунктирный круг 40 dp (обводка 2 dp #8C7A66, штрих 6/4 dp); при `trayCue == SHORT` — обводка 4 dp G.purple. При
  анимациях: новое изделие в слоте — `scaleIn`; покачивание ±4° ≤ 250 мс — только на рост `trayWobble` после первой
  композиции экрана: `val seen = remember { mutableIntStateOf(vm.trayWobble) }`, `LaunchedEffect(vm.trayWobble) {
  if (vm.trayWobble != seen.intValue) { seen.intValue = vm.trayWobble; if (animate) … } }` (вход в раунд и
  пересоздание Activity не качают поднос); без анимаций — без покачивания.
- Витрина: кнопки по порядку `r.menu`, белые, радиус 16, тень 4, `Pic(null, emoji, 56.dp)` по центру, без
  подписи; `clickable(role = Role.Button, onClickLabel = «Положить на поднос»)` → `vm.trayPut(id)`; семантика § 6.
- Указатель «☝️» (U+261D U+FE0F; при `r.pointer && trayThanks == null && !r.done`): emoji 40 dp в оверлее корневого
  Box, левый верхний угол — в (правый − 44, низ − 44) dp границ цели (`onGloballyPositioned`), целиком внутри цели;
  без семантики, статичный; цель — «Отдать», если поднос полный, иначе кнопка витрины `Tray.missing(r).first()`.
- Ряд смены: слева символы по `r.orders.indices`: `results[i] == true` — ★ (Canvas-путь, пятиконечная звезда
  24 dp, заливка G.gold, обводка 1,5 dp G.purpleDeep) и под ней «+1» (bodyMedium, G.purpleDeep); `false` — ●
  (круг 16 dp G.purpleDeep); не обслужен — ○ (окружность 16 dp, обводка 2 dp G.purpleDeep). Один узел (§ 6).
  Справа — `GameButton("Отдать", style = PRIMARY, minHeight = 48)` шириной `weight(1f)` → `vm.trayGive()`, всегда
  активна (PRIMARY — белый на G.purple 12,6:1; зелёный макета — белый на G.green 2,57:1, вопрос ворот 6).
- Пауза «Спасибо!»: `val t = vm.trayThanks; LaunchedEffect(t) { if (t != null) { delay(THANKS_MS); vm.trayThanksDone() } }`,
  `THANKS_MS = 1200L` (константа UI с пометкой `ponytail:` — время показа реплики, не экономика). Касание
  витрины, подноса или «Отдать» во время паузы завершает её (§ 2). «Закончить», ⌂ и «Назад» — как всегда.
- Live region: последний ребёнок корневого Box TrayScreen, вне группы сцены, узел 1 dp в композиции всегда,
  `contentDescription = vm.trayNote`, `liveRegion = LiveRegionMode.Polite`, `traversalIndex = 1000f` (в линейном
  пути TalkBack — последний, после «Отдать»).

### 5. Облачко загадки (TrayScreen.kt; группа 2)
Оверлей в корневом Box TrayScreen при `vm.riddleOpen != null` — облачко Бори, а не карточка по центру: ⌂ и HUD
доступны (⌂ одним касанием завершает смену через `vm.home()`), Боря виден.
- `BackHandler { vm.closeRiddle() }`. Под облачком — прозрачный слой на всю площадь TrayScreen:
  `pointerInput(Unit) { detectTapGestures { vm.closeRiddle() } }`, без фона и без семантики (сцена не затемняется).
- Пока облачко открыто, контейнеры сцены, витрины и ряда смены получают `Modifier.clearAndSetSemantics {}` —
  TalkBack видит HUD и облачко (модальность для касаний и для TalkBack; закрытие — «Не сейчас» и «Назад»).
- Облачко: у низа TrayScreen (низ — отступ 8), ширина W − 16, верх не выше низа кадра Бори − 0,4·кадр (закрывает
  витрину, ряд и прилавок, голова и торс Бори видны), выше — прокрутка внутри облачка; белое, радиус 20, тень 8,
  отступ 14; хвост вверх к значку «?» у Бори; `semantics { paneTitle = «Загадка Бори» }`.
- До ответа (`riddleLine == null`): «Загадка Бори» (titleMedium, G.purple, `heading()`), `q.question`
  (bodyLarge), варианты `q.options` — плашки: `Box` на всю ширину, `heightIn(min = 48.dp)`,
  `padding(horizontal = 14.dp, vertical = 8.dp)`, `contentAlignment = CenterStart`, лицевая сторона как у
  `GameButton` PAPER (градиент белый → G.paperTint, радиус 24, кромка 5 dp #D8D3E2 под лицевой стороной —
  `drawBehind`), `clickable(role = Role.Button)` → `vm.trayAnswer(i)`, внутри `TText(option, style = bodyLarge,
  color = G.purpleDeep)` без ограничения строк (подпись `GameButton` — `Text` с `maxLines = 2`, обрез при 1,3 зонд
  не видит). Через зазор 16 dp — «Не сейчас»: плашка той же ширины и высоты, но другого вида — без заливки,
  обводка 2 dp G.purple, текст G.purple (не путается с вариантом ответа) → `vm.closeRiddle()` (riddleAsked не ставится).
- После ответа (`riddleLine != null`): `riddleLine.text` (bodyLarge), `riddleLine.why` (bodyMedium, G.inkSoft),
  «Дальше» — `GameButton(PRIMARY, minHeight = 48, fillMaxWidth)` → `vm.closeRiddle()`.

### 6. TalkBack (все цели — Compose-узлы; строки дословно)
| Узел | contentDescription / действие |
|---|---|
| облачко заказа, `q.canPlay` | «Заказ: {dot(text)}» + (intro ≠ null: « {dot(intro)}») + « Оплата от {q.base} до {q.top} монет. Смены: {n} из {max}» (n, max — как `ShiftTokens`); пример: «Заказ: Помоги Боре в пекарне! Оплата от 6 до 10 монет. Смены: 0 из 3» |
| облачко заказа, `!q.canPlay` | «{dot(q.line)} Смены: {n} из {max}» |
| облачко покупателя, заказ | «{residentName(o.customer)}. Заказ: {trayList(o.items)}»; при `trayMiss` + «. Ещё нужно: {trayList(trayMiss.missing)}» |
| облачко покупателя, «Спасибо!» | «{residentName(thanks.customer)}: спасибо!» и при звезде « Звезда» |
| «Закончить» | кнопка (текст), первая в сцене |
| «?» | «Загадка Бори», Role.Button |
| поднос (плашка) | `vm.trayText(r)` |
| слот с изделием | «{pastryTitle(p)} на подносе», Role.Button, onClickLabel «Убрать» |
| кнопка витрины | «{pastryTitle(p)}», Role.Button, onClickLabel «Положить на поднос», stateDescription «на подносе: {k}» при k > 0 |
| ряд символов | «Обслужено {r.results.size} из {r.orders.size}, звёзд {r.stars}» |
| облачко загадки | paneTitle «Загадка Бори»; варианты и «Не сейчас» — Role.Button с текстом; «Дальше» — кнопка |
| live region | `vm.trayNote` (§ 2), последний в пути |
Без семантики: пустые слоты, прилавок, указатель, ✓, пунктир, картинки внутри облачков, прозрачный слой под
облачком загадки (`clearAndSetSemantics {}` у группы или у картинки). Появление «?» не объявляется (№ 48 б — не
навязывается). Повтор того же `trayNote` подряд (второе касание при полном подносе, второе «Отдать» при неполном)
TalkBack не объявляет — принято, как правило LINE «одинаковая строка дважды не ставится»; отклик — звук и обводка.
В паузе «Спасибо!» кнопки витрины сохраняют метку «Положить на поднос», а касание завершает паузу (§ 2) — принято:
пауза 1,2 с, заказ нового покупателя ещё не показан.

### 7. Без анимаций, звук, доступность
- `LocalAnimate == false`: изделие сразу в слоте, покачивания и прыжка нет (FULL видно по обводке плашки),
  смена покупателя мгновенная, «Спасибо!» и ★ — сразу (пауза та же), сердечки не летят (`particles.enabled`),
  указатель статичный всегда.
- Звуки — только существующие `Sound` (§ 2); новых нет. Важное не только звуком: FULL и SHORT видны обводкой.
- Мишени ≥ 48 dp: кнопки витрины, слоты подноса, «Отдать», «Закончить», «?», плашки облачка загадки, «Дальше»,
  «Начать смену» (56). Текст ≥ 16 sp (`labelMedium`, `labelSmall` и `GameButton(tight = true)` не используются);
  кап 1,3 — `SceneFontScale` (Round и Place уже fixed()). Контраст: «Отдать» — PRIMARY (12,6:1); ★ — заливка с
  тёмной обводкой; ✓ — белое на G.greenDark (5,0:1); пунктир пустого слота #8C7A66 на #FAF4E8 (≥ 3:1); «Не сейчас» —
  G.purple на белом.
- Весь текст — `TText` (зонд «Обрезано»), кроме emoji внутри `Pic` и подписей `GameButton` в одну строку
  («Закончить», «Отдать», «Дальше», «Начать смену»).

### 8. Макет MockShopJob.kt (debug; группа 2)
Строка 186 «База 6 монет + до 4 за булочки» → «6–10 за смену»; строка 216 «База 6 + 3 за булочки. ✉ +9 —
придёт с новым конвертом» → «6 за смену + 3 за ★. ✉ +9 — придёт с новым конвертом»; блок загадки 189–194
(«Загадка Бори: ответишь — бомбочка», «Ответить», «Нет, спасибо») удалить. Поле Match3 макета (шаг 1) не трогать.

### Явно НЕ определено (кодер спрашивает, а не решает)
Любая новая строка ребёнку сверх § 1–§ 6; любое изменение домена сверх § 1; раскладка экранов других работ сверх
строки `OrderCard`; новые звуки, картинки, ресурсы; сохранение раунда в профиль; комментарии в коде — без
стоп-основ п. 10 (в том числе «последн», «точно», «плохо»).

## SCOPE
variant: main (домен) + game (UI)
Группа 1 (домен), allow: finny-pet/app/src/main/java/ru/finny/pet/domain/town/Tray.kt,
  finny-pet/app/src/main/java/ru/finny/pet/domain/town/Town.kt
Группа 2 (UI), allow: finny-pet/app/src/game/java/ru/finny/pet/game/GameViewModel.kt,
  finny-pet/app/src/game/java/ru/finny/pet/game/GameApp.kt,
  finny-pet/app/src/game/java/ru/finny/pet/game/screens/PlaceScreen.kt,
  finny-pet/app/src/game/java/ru/finny/pet/game/screens/RoundScreen.kt,
  finny-pet/app/src/game/java/ru/finny/pet/game/screens/TrayScreen.kt (новый),
  finny-pet/app/src/game/java/ru/finny/pet/game/mock/MockShopJob.kt
protect (обе группы; пересечение с allow — пусто, правило № 23): базовый (.claude/, CLAUDE.md, docs/,
  finny-pet/app/src/test/, gradle-файлы, AndroidManifest, proguard) + finny-pet/docs/, finny-pet/app/src/classic/,
  finny-pet/app/src/main/assets/, finny-pet/app/src/main/res/, …/PetSprites.kt, …/data/,
  …/domain/{Content,Economy,GameState,Match3}.kt, …/domain/town/{Dictionary,Enums,EventDef,Migration,PetTalk,
  Prices,TownCodec,TownContent,TownEvents,TownState}.kt, finny-pet/tools/art/, finny-pet/tools/{ui.py,demo_run.sh,
  office/}, корневые tools/* поимённо (№ 29); группе 1 — ещё finny-pet/app/src/game/; группе 2 — ещё файлы группы 1.
  Правки `tools/town_route.sh` (маршрут п. 11) оркестратор коммитит ДО спавна кодера группы 2 (иначе стоп-хук
  отнесёт их кодеру).

## ANTI-SCOPE
Итог смены в сцене с питомцем, «Прогресс» через `bestStars` (сейчас «Рекорды: Помочь Боре 3»; у старого
профиля — наследие Match3 числом), окно комнаты через `residentOfWeek` — срез 1б; спрайт-оверлей прилавка,
спрайты выпечки, полёт изделия с витрины и возврат лишнего полётом (в 1а — `scaleIn` и исчезновение) — 1б;
чистка Match3 (`Match3.kt`, `MiniGameScreen.kt`, тайлы, `answerRiddle`, `Riddle`, `jobLevelBombs`,
`shiftScorePerBonus`) — срез 2; карточка заказа Марты в лавке — A1s; иконка HUD «Подсказка» в раунде (вопрос
ворот 5); экран других работ сверх строки `OrderCard`; сохранение раунда; новые поля GameState; правки content.json
(§ 0 — оркестратор), тестов (test-author), доков и `tools/` (оркестратор); правка `Widgets.kt`, `TownUi.kt`,
`Common.kt` (плашки загадки — в TrayScreen.kt); новые зависимости, звуки, ресурсы; «Новый рекорд!» и любые
сообщения о рекорде у TRAY.

## БЮДЖЕТ
Группа 1: ≤ 30 вставок (Tray.kt ≈ 5, Town.kt ≈ 20). Группа 2: ≤ 600 вставок (TrayScreen.kt ≈ 390, PlaceScreen.kt
≈ 80, GameViewModel.kt ≈ 120, RoundScreen.kt ≈ 2, GameApp.kt ≈ 2, MockShopJob.kt ≈ 2). Счёт — `git diff -w
--numstat BASE` + `wc -l TrayScreen.kt`. Новые файлы 1; зависимости 0.

## ORACLE (test-author до кодера)
Файлы: TrayTest.kt (новые тесты § 1, правки), ShiftTest.kt, TownContentTest.kt, EventCatalogTest.kt; при нужде —
ContentValidationTest.kt. Пины, ставшие ложными по § 0, обновляет test-author: покрытие не ослабляется, ассерты не
удаляются без замены. Правило переноса: ассерт ветки Match3 (`finishShift` MATCH3 — счёт / shiftScorePerBonus,
«за булочки», бомбы уровня, бомба за загадку и строка «Бомбочка +1 — для поля «…»», «Новый рекорд!» у MATCH3)
переезжает на ТЕСТОВУЮ КОПИЮ контента `job_bakery.copy(game = MATCH3, title = "Булочки в ряд", board = Board(6, 6),
moves = 15, demoMoves = 5)` (значения до среза 1а — коммит d4a06c9; единственные литералы Match3 в оракуле);
ассерт, не зависящий от игры (ночь, лимит, журнал, конверт, кошелёк, уровни, наклейка), остаётся на реальном
контенте со счётом TRAY ≤ длины sizes (журнал «Смена: Помочь Боре»); смешанный тест делится на два. KDoc `J1Stand`
и шапку TrayTest.kt («в content.json пекарня остаётся MATCH3 до среза 1а», «копия контента среза 0») — привести к
§ 0. Разбор ненулевых `board`/`moves`/`demoMoves` из JSON (был в TownContentTest:278-280) сохранить: в сыром дереве
content.json (`raw`, TownContentTest.kt:33-35) заменить запись job_bakery фрагментом MATCH3 (board 6 × 6, moves 15,
demoMoves 5) и разобрать `ContentRepository.parse(modified.toString())` — тем же декодером, что читает игра.
Красные на контенте § 0 (20):
- ShiftTest: `ночью работа отказывает а загадка и бонус взрослого идут` (:188), `смену нельзя закончить с чужими
  числами` (:280), `смена в пекарне кладёт зарплату в конверт а кошелёк и банки не трогает` (:299), `зарплата
  приходит со следующим конвертом и штампу «По плану» не мешает` (:332), `база платится и за нулевой счёт` (:350),
  `надбавка в пекарне растёт через каждые тридцать булочек и упирается в потолок` (:372), `три работы за один и тот
  же счёт платят по-разному` (:403), `смена оплачивается по уровню до неё а новый уровень идёт в «Почему?»` (:423),
  `сначала тратятся бомбы уровня потом накопленные а надбавка держит потолок` (:472), `после лимита смена отказывает
  и рекорда не пишет` (:493), `новый рекорд в оплачиваемой смене виден в «Почему?»` (:514), `верная загадка даёт
  бомбочку и молчит до конца смены в пекарне` (:537), `после лимита смена загадку не открывает и бомбы не тратит`
  (:573), `конец недели загадку не открывает` (:602), `неверный ответ не даёт бомбочку и следующим не повторяется`
  (:618), `ответы викторины 1_3_0 загадок не снимают` (:665);
- TrayTest: `рекорд звёзд есть только у работы на подносах и отсекает наследие Match3` (:105), `раунд собирается
  только у оплачиваемой смены` (:126) — «работа Match3» → копия MATCH3;
- TownContentTest: `работа читает игру поле ходы и задания` (:275);
- EventCatalogTest: `наклейка заказа даётся за оплаченную смену ещё до раскладки монет` (:809).
Новое (реальный контент):
- TownContentTest: job_bakery.title «Помочь Боре», game TRAY, board/moves/demoMoves null;
  `events[job_bakery_help].intro` и `residents[borya].lines[0]` — «Помоги Боре в пекарне!» (дословно), проходят
  стоп-слова (S1aChildText); журнал смены — «Смена: Помочь Боре», дневник — «Заработали {total}: «Помочь Боре»».
- TrayTest, настоящий путь (маршрут п. 11): демо-профиль недели 1 (`S1aStand.profile(demo = true)`) —
  `trayRound(job_bakery)` не null, sizes = demoSizes, menu = первые steps[0].kinds, pointer = true, intro = null,
  riddle = false; после одной смены пекарни (`finishShift`) — menu = первые steps[1].kinds, intro = steps[1].intro,
  riddle = true; после третьей смены недели — null (лимит).
- TrayTest § 1:
  - `missing` — с кратностью, порядок o.items, пустой при собранном подносе, при `done` — пусто;
  - `riddleInRound` — каждый null-случай с ровно одним ложным условием (остальные истинны): index 0 (riddle, !done,
    вопрос есть), `riddle = false` при index ≥ 1, `done`, `riddleAsked`, все 15 решены; не null при index ≥ 1,
    riddle, !done и вопросе;
  - `answerInRound` верно: стенд, где подсказка различает n и n + 1 (предусловие в тесте `Tray.hint(r,
    R.riddleHint) != Tray.hint(r, R.riddleHint + 1)`, например не демо, n = 1, index 2 — заказ из 3, поднос пустой)
    → раунд = `Tray.hint(r, R.riddleHint)`, bombs не меняются, line «Верно! {q.explanation}», why [«Подсказка Бори —
    на поднос»], riddleAsked;
  - верно, но класть нечего — три стенда: size − 1 верных и пустой слот; size − 1 верных и одно лишнее на полном
    подносе; собранный поднос → раунд равен входу, line «Верно! {q.explanation}», why [«Этот поднос Боря оставил
    тебе»], riddles + запись, riddleAsked;
  - неверно — на стенде с `Tray.hint(r, R.riddleHint) != r` (тот же, что «верно») → раунд равен входу, line
    «{q.explanation}», why [«Вопрос вернётся позже»]; вариант вне диапазона — Refused «Выбери ответ», раунд и состояние
    равны входу; без загадки в раунде (index 0, riddle = false, done, чужой questionId, riddleAsked) — Refused
    «Загадки сейчас нет», раунд и состояние равны входу;
  - line и why всех исходов `answerInRound` (в том числе «Загадки сейчас нет», «Этот поднос Боря оставил тебе») —
    через S1aChildText (стоп-слова и род);
  - после верного ответа `riddleInRound` = null до конца смены; после `finishShift` у следующего раунда (n ≥ 1) —
    снова не null; оплата смены с подсказкой = оплата по звёздам (hint сам звезду не даёт).
- Имена ожидаемых тестов для мутантов п. 9 — в отчёте test-author.
Шаг 4 (WORKFLOW, оркестратор): заглушка-контракт в одноразовой копии дерева — `Tray.missing` = emptyList(),
`riddleInRound` = null, `answerInRound` = TrayAnswer(Refused(""), r); тесты § 1 падают на ассертах, а не на
компиляции (зелёные на этой заглушке — только null-случаи `riddleInRound` и пустые случаи `missing`; их
ловят мутанты п. 9); перенастроенные пины — зелёные уже на заглушке.

## ACCEPTANCE (команды 1–10 — из finny-pet/, 11–12а — из корня; оркестратор)
1. ./gradlew testClassicDebugUnitTest --console=plain -> exit 0, не UP-TO-DATE
2. tests = 600 (число @Test на BASE e4bc44f), failed 0, skipped 0 (скрипт пресета по XML)
3. ./gradlew testGameDebugUnitTest --console=plain -> exit 0, то же число
4. ./gradlew assembleClassicDebug assembleGameDebug assembleGameRelease -> exit 0
5. ./gradlew lintClassicDebug lintGameDebug -> ошибок 0, предупреждений без сетевых ≤ 5 / 5 (замер на BASE)
6. git diff --name-only BASE -- app/src/test/ app/src/main/assets/ app/src/main/res/ '*.gradle.kts' gradle/ gradle.properties -> пусто;
   git status --porcelain -uall -- app/src/test app/src/main/assets app/src/main/res app/src/classic -> пусто
7. git status --porcelain -uall -- app/src -> только пути allow (TrayScreen.kt — «??»); из корня
   `node .claude/hooks/assert-oracle-intact.js` -> exit 0; вставки по группам (`git diff -w --numstat BASE` +
   `wc -l` TrayScreen.kt) ≤ бюджета
8. grep -rnE '^import (android|androidx)|System\.(currentTimeMillis|nanoTime)|Random\(\)|\.shuffled\(\)|\.random\(\)|Random\.(Default|next)|LocalDate|Clock|TimeSource|Instant' app/src/main/java/ru/finny/pet/domain/ -> пусто;
   { git diff -w BASE -- app/src/game | grep '^+'; cat app/src/game/java/ru/finny/pet/game/screens/TrayScreen.kt; } | grep -cE 'System\.|Random|LocalDate|Clock' -> 0
9. мутанты (python ../tools/mutation_probe.py <json>, у каждого `expect` — имя теста от test-author): riddleInRound
   без `index ≥ 1`; без `r.riddle`; без `!r.done`; answerInRound кладёт подсказку и при неверном ответе; без проверки
   загадки в раунде (сразу answerQuestion); hint с `riddleHint + 1`; без ветки HINT_IDLE; ветка HINT_IDLE — новый
   `TownOutcome(st, why = …)` без line; `missing` без кратности (distinct); `missing` возвращает весь заказ -> каждый
   CAUGHT своим тестом
10. стоп-слова в строковых литералах (game и новые строки домена): { git diff -w BASE -- app/src/game app/src/main/java | grep '^+';
   cat app/src/game/java/ru/finny/pet/game/screens/TrayScreen.kt; } | grep -oE '"[^"]*"' | grep -ciE
   'сегодня|осталось|до конца недели|скорее|пока не|успей|последн|ошибк|неправильн|точно|потеряеш|рекорд|цена лени|ленив|зачем|транжир|жадин|зря|впуст|провал|плохо' -> 0
11. живая проверка (из корня; эмулятор `tools/adbui.sh wm360` и S23, debug `feat/town` с зондом «Обрезано», шрифт
   1,0 и 1,3; маршрут `tools/town_route.sh` — блок пекарни заменён оркестратором и закоммичен до кодера группы 2; лог —
   `grep -cE "not found|gate not passed"` → 0; на каждом новом экране — проверочный узел в дампе (`has`): «Заказ: »,
   «. Заказ: », «Ещё нужно: », «Не сейчас», «Дальше», «Новинка», «Смены на неделе закончились»). Заказ демо-профиля
   случаен (seed нового профиля — `System.nanoTime()`): маршрут читает его из описания облачка покупателя «{Имя}.
   Заказ: {список}» (строчные названия → с заглавной) и жмёт витрину ТОЧНЫМ совпадением (`tapx`; префикс «Хлеб»
   совпал бы со слотом «Хлеб на подносе»); «не из заказа» — вид витрины, которого нет в заказе; вариант загадки —
   по `town.quiz` из content.json. Порядок (3 смены недели 1 демо-профиля): пекарня 1 (экран заказа; раунд с
   указателем; отдача с расхождением у покупателя index 1 — одно верное и одно не из заказа — ✓ и пунктир; «Спасибо!» —
   снимок сразу после касания «Отдать» без паузы маршрута, при 1,0; итог «6 за смену + N за ★. ✉ +… — придёт с новым
   конвертом») → Марта (как сейчас) → пекарня 2 (реплика «Новинка — крендель!» в облачке заказа; 4 изделия; «?» после
   первого обслуженного; полный поднос из двух изделий не из заказа → «?» → облачко при 1,0 и 1,3 (состояние во
   ViewModel переживает смену шрифта) → верный ответ → лишнее снято с конца, нужное на подносе; «Закончить») → после
   лимита: пекарня («●●●», кнопки нет) и рынок (карточки заказа Марты нет). На каждом снимке «Обрезано: 0»;
   `tools/ui_measure.py` — кликабельных < 48 dp нет, узлов за краем нет.
   Вручную (оркестратор, эмулятор, debug): (а) большие заказы — `run-as` правка state.json демо-профиля
   (`jobShifts.job_bakery = 7`, `demo = false`, `shiftsThisPeriod = 0`, `riddleAsked = false`), холодный старт →
   пекарня: раунд с заказами из 3 и 4 при 1,0 и 1,3 (облачко из 4 по 40 dp, поднос из 4 слотов), отдача с расхождением
   на заказе из 4 (✓ и пунктир); там же после первого обслуженного — загадка, когда на подносе size − 1 верных, и
   верный ответ → облачко «Этот поднос Боря оставил тебе»; (б) режим без анимаций (неделя 2 демо-профиля, раздел
   взрослого): запись `tools/rec.sh` по одному действию — положить изделие, верная отдача (пауза и смена покупателя) —
   изделие и «Спасибо!» появляются в одном кадре (WORKFLOW № 31); (в) release на эмуляторе: `adb install -r
   "$(cygpath -w …/game/release/…apk)"`, `dumpsys package ru.finny.pet | grep pkgFlags` без DEBUGGABLE в логе
   (№ 33), новый демо-профиль, холодный старт и одна смена пекарни.
12. лист для ворот владельца (из корня): `python tools/sheets.py town/j1a_gate.jpg "…" emu_<кадр>.png "<подпись>" …`
   (OUT и кадры — относительно finny-pet/screenshots) — заказ 1,0 / 1,3 / S23; раунд 1,0 / 1,3 / S23; расхождение;
   «Спасибо!»; итог; облачко заказа с «Новинка — крендель!»; «?» у Бори; загадка 1,0 / 1,3; полный поднос до и после
   верного ответа; «Этот поднос Боря оставил тебе»; большие заказы (облачко из 4, поднос из 4, ✓ и пунктир); после
   лимита — пекарня и рынок; раунд с обоими «?» (HUD и Бори). Подписи без «✉» (sheets.py:4).
12а. судьи по листу до показа владельцу: «ребёнок 7–11» вслепую с самопроверкой (WORKFLOW № 35: понятен ли заказ
   картинками, что значат ✓, пунктир, «?», ★ ● ○; что из плашек загадки — ответ, а что — «Не сейчас»; какой из двух
   «?» — загадка) и доступность (форма вместо цвета, эмодзи витрины в сером и при дейтеранопии, наложения, контраст).
   Находки — в журнал спеки до ворот.

## ВОПРОСЫ НА ВОРОТА (владелец, по листу п. 12; ответ пачкой «1 а, 2 б…»)
1. Касса (CHANGE, срез 2) после лимита смен: а) закрыта до нового конверта, как пекарня (№ 47 б); б) игра «ради
   рекорда» без оплаты. Рекомендую а — один принцип для всех работ, «ребёнок не должен залипать много». (У «Помочь
   Марте» и курьера игры после лимита не было и до № 47 — кадр рынка после лимита это показывает.)
2. Подсказка Бори на полном подносе: а) снимает лишнее с конца и кладёт нужное (кадры до и после), а когда класть
   нечего — строка «Этот поднос Боря оставил тебе»; б) на полном подносе подсказка не срабатывает. Рекомендую а —
   верный ответ не пропадает, заказ целиком Боря не собирает (звезда — за ребёнком).
3. Строка после лимита: а) «Смены на неделе закончились — новые с новым конвертом» (срок в конвертах, как ✉ и
   «≈ 3 ✉», §3.3); б) «Смены на неделе закончились — приходи на новой неделе» (слова владельца). Рекомендую а — один
   ритм с конвертом.
4. Облачко загадки: а) вопрос, варианты и «Не сейчас» (как сейчас); б) под заголовком строка «Отгадаешь — Боря
   поможет с подносом». Рекомендую б — ребёнок видит, зачем отвечать (стоп-лист № 13: не викторина ради викторины);
   по ответу — отдельной правкой.
5. Два «?» на экране раунда (HUD «Подсказка» и загадка Бори; HUD уводит на подсказку и бросает смену): а) оставить;
   б) в раунде скрыть HUD «Подсказка». Рекомендую б — «?» только загадке (концепция § 2), без брошенных смен.
6. Цвет «Отдать»: а) фиолетовая PRIMARY (контраст 12,6:1, как сделано); б) зелёная, как на макете (2,57:1 — в
   список слабых мест доступности). Рекомендую а — ТЗ 3.6.
7. Отличия 1а от концепции, каждое — «оставить / как в концепции»: реплика новинки — в облачке экрана заказа, а не
   у Бори в раунде (рекомендую оставить: на 360 × 640 облачко в раунде ложилось на поднос); пауза «Спасибо!» 1,2 с,
   касание завершает её, но изделие не кладёт — надо нажать ещё раз (оставить); прилавок — полоса до спрайта 1б
   (оставить до 1б); покупатель ≈ 150 dp и Боря ≈ 120 dp на 360 × 640 (в концепции 176 и 144), на S23 — 220 и 176
   (оставить: выше — облачко заказа); поднос из 4 закрывает торс покупателя (оставить); 4 картинки заказа — по 40 dp
   (в концепции 48; оставить: иначе облачко заходит под «Закончить»); изделие появляется в слоте без полёта (полёт — 1б).

## ЖИВАЯ ПРОВЕРКА (человек: docs/BACKLOG.md п. 1 и 11, ручные TC в finny-pet/docs/TEST_CASES.md)
S23, game: пекарня → «Начать смену» → собрать поднос, отдать с расхождением, дособрать; вторая смена — «?»,
ответ. Смотреть: отклик на каждое касание, прыжок и сердечки покупателя, звуки TAP/POP/SUCCESS (без звука — всё
видно), пауза «Спасибо!» не раздражает; TalkBack — путь § 6, live region, модальность облачка загадки, объявляется
ли повторное касание полного подноса.

## ДОКИ (оркестратор, после приёмки, до ревью; греп по всему репозиторию, оба docs/)
Грепы: «Булочки в ряд», «три в ряд», «MATCH3|Match3», «бомб», «Загадка Бори», «до среза 1а», «срез 1а», «Пекарне
нужен помощник! Боря», «Булочки сами», «board», «demoMoves», «TalkBack-пути нет», «Марте ещё нужен». Править то, что
стало ложью в 1а: GAME_CONCEPT §0, §3.1, §3.2, §4.1, §5.2 (жетоны, бомбы уровня, загадка «до среза 1а», таблица game),
§9.2 № 12–13, §14 (Match3 на 360 dp, TalkBack), §15 (демо), §17.2, §17.3 (статус 1а), §18 № 40–41 («пока не
реализовано» → «реализовано срезом 1а TOWN-J1», текст решений не менять); TOWN-J1.md (статус среза; концепция § 2 —
«Ещё нужно: …» в TalkBack, реплика новинки в облачке заказа, облачко загадки, HINT_IDLE — по журналу спеки);
finny-pet/docs: ECONOMY, DATA_MODEL, BUILD_AND_DEMO (шаги 6 и 9 — поднос и загадка со второй смены), TEST_CASES
(TC-28, TC-32, TC-41), REQUIREMENTS_MATRIX (в том числе «пекарня — три в ряд»), UX_ACCESSIBILITY (путь TalkBack
подноса, контраст ★, пунктира и «Отдать», модальность облачка загадки), ARCHITECTURE (TrayScreen); docs/BACKLOG.md.
Что относится к 1б — не трогать.

## ПРИ БЛОКЕРЕ
STATUS: BLOCKED + один конкретный вопрос. Не изобретать.
```

## Журнал спеки
- 2026-09-28: черновик по концепции TOWN-J1 (§ 2, § 3, § 8), контракту среза 0 и замеру красных пинов на
  контенте § 0 (581 / 20).
- 2026-09-28, круг 1 критиков (4 измерения — концепция и решения; контракт против кода; раскладка, арифметика и
  доступность; оракул, приёмка и доки; каждый список — скептику): 61 находка, 48 подтверждены, 13 частично,
  0 опровергнуты. Приняты:
  - облачко загадки — оверлей в дереве вместо фокусируемого Popup (⌂ в одно касание — стоп-лист № 15; зонд
    «Обрезано» и кап шрифта работают; состояние — во ViewModel, переживает смену шрифта маршрутом);
  - у TRAY экран заказа не в прокрутке без меры (BoxWithConstraints в прокрутке получал бесконечную высоту), облачко
    заказа — от верха сцены, Боря сдвинут влево (облачко не закрывает плечо), строка «Смены» + точки вместо «Смены
    на неделе:» (короче, без повтора после лимита), выпечка на прилавке экрана заказа снята (облачко её закрывало);
  - реплика новинки — в облачке экрана заказа (`trayIntro`), а не облачком у Бори в раунде (на 360 × 640 оно
    ложилось на поднос);
  - касание во время паузы «Спасибо!» завершает её (отклик ТЗ 3.4), «?» в паузе не прячется, «Спасибо! ★ +1» —
    одной строкой, P в паузе не пересчитывается; при `r.done` экран не читает `orders[index]`;
  - верный ответ, когда класть нечего, — строка HINT_IDLE вместо обещания «на поднос» (домен, под оракулом);
  - `startRound` сбрасывает раунд подноса у всех работ (брошенный через HUD раунд не показывался на экране Марты);
    ветка RoundScreen — по `game == TRAY`; `tray` живёт до `closeRound` (концепция § 3);
  - ✓ — по замороженному `trayMiss.missing` (№ 43 а), галочка — Canvas на G.greenDark, пунктир — `drawBehind`
    без влияния на раскладку, ★ — звезда с тёмной обводкой, пунктир пустого слота #8C7A66 (контраст);
  - варианты загадки — плашки с `TText` без ограничения строк (подпись `GameButton` — `Text` с `maxLines = 2`,
    вариант q_shop_3 при 1,3 обрезался бы молча); `tight` не используется (14 sp нет в исключениях);
  - FULL — статичная обводка плашки (без анимаций и звука было не видно); live region — узел всегда в композиции,
    после паузы объявляет заказ нового покупателя; «Закончить» читается первым (`traversalIndex`);
  - `trayText`, `trayList`, `residentName`, `pastryTitle` — публичные функции ViewModel; «Заказ: {dot(text)}» без
    «!.»; наброски кода исправлены (`LaunchedEffect`, переходы); «Начать смену» в `OrderCard` — только при
    `canPlay` (№ 47 б для всех работ);
  - dp пересчитаны по замеру wm360 (H раунда ≈ 478, сцены заказа ≈ 426): покупатель ≈ 150 dp — в вопросы ворот;
  - оракул: правило переноса пинов на копию MATCH3 с title «Булочки в ряд», смешанные тесты делятся, разбор
    board/moves/demoMoves из JSON сохраняется, KDoc J1Stand; изоляция условий `riddleInRound`; стенд, где подсказка
    различает n и n + 1; случай HINT_IDLE; мутант «без HINT_IDLE»;
  - приёмка: стоп-слова и греп времени — по литералам и с новым TrayScreen.kt (git diff не видит неотслеживаемых),
    `git diff -w`; `assert-oracle-intact.js` без `--base`; маршрут расписан по трём сменам недели, снимок «Спасибо!»
    без паузы маршрута, облачко загадки при 1,3 — из ViewModel; проверочные узлы; снятие лишнего подсказкой и рынок
    после лимита — на лист; без анимаций — записью rec.sh; release — `cygpath -w` и pkgFlags; судьи по листу (12а);
    вопросы на ворота; пути tools из корня; LIVE_CHECKLIST.md не существует — ссылка на BACKLOG и TEST_CASES;
    доки — «три в ряд», §3.2, §15.
  Частично: «пауза нарушает ТЗ 3.4» — отклик у кнопок есть (нажатие видно), но касание теряло действие без звука —
  принято завершение паузы касанием; «нужно больше трёх смен» — нет, без анимаций — на неделе 2.
- 2026-09-28, круг 2 критиков (3 измерения по всей спеке — концепция, решения и тексты; контракт, реализуемость,
  оракул и приёмка; раскладка и доступность; каждый список — скептику; сеть оборвала 3 агента, прогон повторён с
  кэшем): 36 находок, 19 подтверждены, 17 частично, 0 опровергнуты. Приняты:
  - облачко загадки — облачко Бори у низа сцены с хвостом к «?», без затемнения (Боря виден; «сцена, а не
    карточки»), модально и для TalkBack (`clearAndSetSemantics` на сцене, витрине и ряду, прозрачный слой без
    семантики), «Не сейчас» другого вида через зазор 16 (не путается с ответом), плашки вариантов — с отступами и
    кромкой, как у GameButton;
  - HINT_IDLE — «Этот поднос Боря оставил тебе» (прежняя «Поднос почти готов — подсказка не понадобилась» лгала при
    собранном подносе и при одном лишнем на полном — ребёнок отдал бы поднос с лишним); три стенда в оракуле;
  - порядок проверок в `trayPut`/`trayTake`/`trayGive`: пауза → done → чтение заказа (после последней отдачи верны
    оба условия); индекс показанного покупателя `shown` — ключ AnimatedContent, облачко и слоты подноса в паузе —
    отблагодарившего покупателя; витрина всегда резервирует 2 ряда (A и кадры одинаковы во всех сменах); облачко
    покупателя — у его головы; указатель «☝️» с FE0F целиком внутри цели; пунктир — внутри картинки (соседние круги
    не накладываются); `trayWobble` качает только на рост после первой композиции;
  - «Отдать» — PRIMARY (белый на зелёном 2,57:1 — вопрос ворот 6); экран заказа — сцена не ниже 360 dp в
    прокрутке (события места сдвигают её, а не сжимают); live region — последний в пути; повтор той же строки не
    объявляется — принято и записано в § 6, проверка — человеку на S23;
  - оракул: line исходов `answerInRound` пинится (мутант «HINT_IDLE без line»); у «неверно» — стенд, где hint меняет
    раунд; S1aChildText на строках `answerInRound`; разбор фрагмента MATCH3 — `ContentRepository.parse` над
    подменённым `raw`;
  - приёмка: греп стоп-слов — полный список № 10 и строки домена; маршрут — чтение заказа из облачка и точные касания;
    большие заказы и HINT_IDLE — вручную правкой state.json (run-as); лист `sheets.py town/…` (путь от screenshots);
    вопросы на ворота переписаны вариантами с рекомендацией (касса вместо «прочих работ», строка лимита против слов
    владельца, строка пользы в облачке загадки, два «?» в раунде, цвет «Отдать», отличия от концепции с числами);
    доки — §18 № 40–41, «Марте ещё нужен».
  Частично / принято иначе: «касание витрины в паузе должно класть изделие» — нет (заказ нового покупателя ещё не
  виден), расхождение метки TalkBack в паузе принято и записано в § 6; «TalkBack „Ещё нужно“ — отклонение от
  концепции» — концепция сама задаёт «Ещё нужно» только в TalkBack (TOWN-J1.md:142), пример «Марте ещё нужен хлеб»
  правится в доках; «строка пользы в облачке загадки» — вопрос ворот 4 (состав облачка задала концепция после № 48 б);
  второй «?» (HUD) — вопрос ворот 5, скрытие HUD — вне скоупа до ответа.
