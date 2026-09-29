```
TASK: TOWN-J1-0 — контракт «Поднос по заказу»: TRAY, Tray.kt, закрытые работы после лимита (№ 47 б)
EPIC: TOWN-J1 (docs/tasks/TOWN-J1.md — концепция, решения владельца № 40–48, журнал проверки)
BASE: f4360b3 (коммит оракула; для кодера и ревьювера); старт — a45496f (спека и контент)
BRANCH: feat/town

## КОНТЕКСТ
Срез 0 эпика TOWN-J1 (концепция § 3, § 5, § 8; GAME_CONCEPT §5.2, §17.3): домен мини-игры пекарни
«Поднос по заказу» вместо Match3 — без экрана. Раунд живёт во ViewModel (как `match`), в профиль не
пишется; оплата — прежняя схема смен (база по уровню + надбавка ≤ shiftBonusMax), звезда покупателя
= +1. Работа `job_bakery` в content.json остаётся MATCH3 до среза 1а (экран): данные подноса (меню,
ступени, demoSizes) лежат в её записи уже сейчас, а TRAY проверяется на ТЕСТОВОЙ КОПИИ контента, где
у пекарни game = TRAY. Решение № 47 б («ребёнок не должен залипать много»): после лимита смен работа
закрыта до нового конверта, игры «ради рекорда» нет. Для пекарни решение окончательное; для прочих
работ — принцип по умолчанию, подтвердить на воротах среза 1а (§18 № 47). Сейчас это заметно только
у пекарни: TAPS после лимита закрыты и раньше, касса в game недостижима (job_kassa_help в eventsOff);
если владелец откажет — игру после лимита вернуть можно только кассе (CHANGE), отдельной задачей.

## ТРЕБОВАНИЕ ТЗ (docs/sources/ТЗ_текст.txt)
2.5.4 «Для каждого начисления указываются источник и сумма» (итог «6 за смену + 3 за ★. ✉ +9»,
ShiftPay); 2.1 «за один игровой период нельзя купить все сразу» (лимит смен, № 47 б); 2.2
«Безопасная ошибка» (неверная отдача не штрафуется, поднос дособирается); 2.5.13 «обязательные этапы
игрового цикла воспроизводятся подряд без ожидания календарных сроков» (раскладка — только от seed
профиля и счётчиков, демо — demoSizes); 2.5.14 «Новое задание добавляется без переработки основной
логики приложения» (изделие и ступень — записи content.json); 3.5 «не должны запугивать, стыдить» и
8.1 «отсутствие давления, стыда и манипулятивных механик» (без таймера — стоп-лист GAME_CONCEPT §9.2
№ 3; без слов «ошибка», «неправильно» — № 10). Рамка: «как человек может повлиять на увеличение
оплаты своего труда» — уровни и звёзды.

## CONTRACT
Пакет ru.finny.pet.domain.town. R = town.rules, job — запись town.jobs, name — имя жителя job.resident.
Внешняя пара «» у строк — разделитель. Все строки ребёнку — дословно. NIGHT = «Сейчас ночь — сначала
проснёмся», CLOSED = «Эта работа пока закрыта», BAD_SHIFT = «Так закончить смену нельзя», LIMIT =
«Смены на неделе закончились — новые с новым конвертом» (строка Town.kt:610, одна для всех работ —
концепция § 2; дни недели ребёнку не называются, §3.3), PAY_LATER = «придёт с новым конвертом».
Правило домена — только добавление (GAME_CONCEPT §17.1): существующие сигнатуры не меняются, поля
добавляются в конец.

### 0. Контент (коммитит оркестратор ВМЕСТЕ СО СПЕКОЙ, до оракула; кодеру content.json закрыт)
town.rules += "riddleHint": 1 (пометка "_todo" на town.rules уже есть). job_bakery (game остаётся
MATCH3; board, moves, demoMoves, title не меняются) += поля ниже и "_todo": "Меню, ступени и
demoSizes — заглушки, подобрать плейтестом (TOWN-J1, концепция § 5 п. 4)":
  "menu": [ {"id":"croissant","title":"Круассан","emoji":"🥐"}, {"id":"bread","title":"Хлеб","emoji":"🍞"},
            {"id":"baguette","title":"Багет","emoji":"🥖"}, {"id":"pretzel","title":"Крендель","emoji":"🥨"},
            {"id":"donut","title":"Пончик","emoji":"🍩"}, {"id":"cupcake","title":"Кекс","emoji":"🧁"} ],
  "steps": [ {"fromShift":0,"kinds":3,"sizes":[1,2,2,2]},
             {"fromShift":1,"kinds":4,"sizes":[2,2,3,3],"intro":"Новинка — крендель!"},
             {"fromShift":3,"kinds":5,"sizes":[2,3,3,3],"intro":"Новинка — пончики!"},
             {"fromShift":5,"kinds":6,"sizes":[2,3,3,3],"intro":"Новинка — кексы!"},
             {"fromShift":7,"kinds":6,"sizes":[3,3,4,4],"intro":"Большие заказы!"} ],
  "demoSizes": [1,2,2,2]
(решение № 44 а: 3 изделия и указатель в первой смене, новинка на 2-й, 4-й, 6-й сменах, большие заказы
на 8-й; длина sizes = shiftBonusMax = 4 — звезда = монета, диапазон честный.) Название «Помочь Боре»,
game = TRAY и тексты № 42 в — срез 1а.

### 1. Модель контента (TownContent.kt)
enum class JobGame { MATCH3, TAPS, CHANGE, TRAY }          // TRAY — последним
@Serializable data class Pastry(val id: String, val title: String, val emoji: String)
/** Ступень меню: действует после стольких оплачиваемых смен на этой работе; kinds — сколько первых
 *  изделий меню на витрине; sizes — размер заказа каждого покупателя по порядку; intro — реплика
 *  жителя работы на первой смене ступени. */
@Serializable data class TrayStep(val fromShift: Int, val kinds: Int, val sizes: List<Int>, val intro: String? = null)
Job += (в конец) val menu: List<Pastry> = emptyList(), val steps: List<TrayStep> = emptyList(),
       val demoSizes: List<Int>? = null
TownRules += (в конец) val riddleHint: Int                 // без умолчания, как все поля TownRules

### 2. Раунд (новый файл domain/town/Tray.kt; чистый Kotlin, без android*/времени/случайности без seed)
/** Заказ покупателя: id жителя и id изделий в порядке job.menu (одинаковые — рядом). */
data class TrayOrder(val customer: String, val items: List<String>)
data class TrayRound(
    val jobId: String,
    val menu: List<String>,               // id изделий на витрине — первые step.kinds изделий job.menu
    val orders: List<TrayOrder>,          // по покупателю на каждый элемент sizes
    val tray: List<String> = emptyList(), // поднос текущего покупателя в порядке укладки
    val missed: Boolean = false,          // текущему покупателю уже отдавали неверный поднос
    val results: List<Boolean> = emptyList(), // по обслуженным покупателям: true — звезда
    val pointer: Boolean = false,         // указатель обучения
    val intro: String? = null,            // реплика жителя работы на первой смене ступени
    val riddle: Boolean = false,          // в этой смене у жителя работы будет значок «?»
) {
    val index: Int get() = results.size             // текущий покупатель = число обслуженных
    val stars: Int get() = results.count { it }
    val done: Boolean get() = index >= orders.size
}
data class Give(val round: TrayRound, val accepted: Boolean, val served: Boolean, val star: Boolean,
                val missing: List<String>, val extra: List<String>)
object Tray {
    fun put(r: TrayRound, pastry: String): TrayRound
    fun take(r: TrayRound, slot: Int): TrayRound
    fun give(r: TrayRound): Give
    fun hint(r: TrayRound, n: Int): TrayRound
}
Обозначения: o = r.orders[r.index] (текущий заказ), size = o.items.size. Сравнение подноса и заказа —
как мультимножеств: m = число совпадений (пересечение с кратностью); missing = элементы o.items, для
которых в подносе не нашлось пары (обход o.items по порядку, каждое изделие подноса парится один раз),
extra = элементы подноса без пары в o.items (обход подноса по порядку).
- put: r.done, или pastry ∉ r.menu, или tray.size ≥ size → r без изменений (равен входу); иначе
  изделие дописывается в конец подноса.
- take: r.done или slot ∉ tray.indices → r без изменений; иначе поднос без элемента slot (порядок
  остальных сохраняется).
- give: r.done → Give(r, false, false, false, [], []). tray.size < size (неполный поднос) → Give(r,
  false, false, false, [], []) — round РАВЕН входу, звезда не теряется (missed не ставится).
  Полный поднос, missing пуст → star = !missed; round = r.copy(tray = [], missed = false, results =
  results + star); Give(round, true, true, star, [], []). Полный поднос, missing не пуст → round =
  r.copy(tray = поднос без extra (совпавшие остаются в своём порядке), missed = true); Give(round,
  true, false, false, missing, extra). Штрафа нет: results и stars не меняются.
- hint: r.done, n ≤ 0 или m == size (поднос уже собран верно) → r без изменений. Иначе add = min(n,
  size − 1 − m); add == 0 → r без изменений. free = size − tray.size; если add > free — с подноса
  снимаются (add − free) изделий из extra, начиная с конца подноса (лишних всегда хватает: add − free ≤
  |extra| − 1). Затем первые add элементов missing дописываются в конец подноса. missed и results не
  меняются. Итог: при m < size после hint m' = min(m + n, size − 1) — подсказка никогда не собирает
  заказ целиком (звезду зарабатывает ребёнок, стоп-лист № 12). Снятие лишнего нужно, чтобы верный
  ответ на полном подносе не пропал (§5.2 обещает изделие на поднос); снимается не больше n лишних
  (add − free ≤ add ≤ n). Правило одно для любого подноса, в том числе дособранного после неверной
  отдачи. Показать на воротах среза 1а как уточнение № 48 б.

### 3. Town: раунд, житель недели, рекорд звёзд
fun Town.residentOfWeek(s: GameState): Resident?
- последний в town.residents, у кого arrivesWeek != null и arrivesWeek ≤ s.period (правило окна
  комнаты, RoomScreen.kt:130; демо не ослабляет); нет таких — null. На реальном контенте: неделя 1 —
  osya, 2 — tosha, 3 — stepan, 4 — kesha, 5 и дальше — asya.
fun Town.bestStars(s: GameState, jobId: String): Int?
- job есть и job.game == TRAY и records[jobId] ∈ 0..R.shiftBonusMax → records[jobId]; иначе null
  (число больше shiftBonusMax — наследие Match3 в профиле, не рекорд звёзд).
fun Town.trayRound(s: GameState, jobId: String): TrayRound?
- null, если работы нет, job.game != TRAY, !shiftQuote(s, jobId).canPlay, |P| < 2 (ниже) или
  витрина (min(step.kinds, menu.size) изделий) меньше min(k, 1 + k / 2) для какого-либо k из sizes
  этого раунда (в демо — из demoSizes). Последние два на валидном контенте не бывают (§ 5); они нужны,
  чтобы выбор покупателей и видов не зацикливался. null — это и есть «раунд только у оплачиваемой
  смены» концепции.
- n = s.jobShifts[jobId] ?: 0; step = последняя ступень job.steps с fromShift ≤ n.
- sizes = job.demoSizes, если s.demo и demoSizes != null; иначе step.sizes.
- menu = первые step.kinds id из job.menu (в порядке menu).
- pointer = (bestStars(s, jobId) ?: 0) == 0 (смена с 0★ указатель не снимает).
- intro = step.intro, если n == step.fromShift, иначе null.
- riddle = n ≥ 1 и nextQuestion(s) != null (загадка — не в первой смене, одна за смену; № 48 б).
- orders: по одному на элемент sizes (k = sizes[i]):
  - customer — из пула P: жители с arrivesWeek != null и arrivesWeek ≤ s.period, кроме job.resident;
    два соседних покупателя — разные жители; житель недели W = residentOfWeek(s) — хотя бы в одном
    заказе, если W != null и W.id != job.resident (иначе W не навязывается).
  - items: k изделий из menu, разных видов ровно min(k, 1 + k / 2), каждое выбранное — хотя бы раз,
    отсортированы по порядку job.menu.
- Случайность. Выборы (покупатели и изделия) — только из Random(x), где x — детерминированная смесь
  s.seed, s.period, s.shiftsThisPeriod и n; формула смеси и алгоритм выбора не закреплены. При равных
  этих четырёх величинах, s.demo и контенте orders равны. sizes зависят от n и s.demo, menu и intro —
  от n, pointer — от records[jobId], riddle — от riddles и riddleAsked, null — от canPlay (ночь,
  открытие работы, лимит). Кошелёк, банки, копилка, day, diary, purchases, stickers, envelope на раунд
  не влияют. Каждая из четырёх величин входит в смесь: при demo = false и прочих равных orders
  меняются хотя бы для одного seed из 1..20, когда меняется одна из них — n 8 ↔ 9 (одна ступень,
  без intro, riddle тот же), period 6 ↔ 7 (тот же пул и W = asya), shiftsThisPeriod 0 ↔ 1; для самого
  seed — orders при seed 1..20 не все равны.

### 4. Смены: котировка, конец смены, загадка (правки Town.kt, TownState.kt)
data class ShiftQuote(…прежние поля…, val top: Int)        // в конец; конструирует только Town
data class ShiftPay(val base: Int, val bonus: Int, val total: Int)   // в Town.kt рядом с ShiftQuote
TownOutcome += (в конец) val pay: ShiftPay? = null         // TownState.kt, одна строка
Town.shiftQuote(s, jobId):
- canPlay = open и !s.asleep и paid — у ВСЕХ работ (№ 47 б; игры без оплаты больше нет).
- top: TAPS → base; MATCH3, CHANGE, TRAY → base + R.shiftBonusMax (у TRAY валидатор держит длину
  sizes = shiftBonusMax, поэтому это и есть base + min(shiftBonusMax, sizes.size)); неизвестная
  работа → 0.
- levelBombs — как было (только MATCH3).
- line по порядку: !open CLOSED; asleep NIGHT; paid: TAPS «{base} за три поручения», иначе
  «{base}–{top} за смену» (тире U+2013 без пробелов: «6–10 за смену»); иначе LIMIT — до плана и
  после.
Town.finishShift(s, jobId, score, bombsUsed) — сигнатура прежняя:
- Refused по порядку (состояние не меняется): pet «Сначала создай питомца»; asleep NIGHT; !open
  CLOSED; !canPlay — shiftQuote.line (после лимита — LIMIT); BAD_SHIFT, если score < 0, или
  bombsUsed < 0, или bombsUsed > 0 не у MATCH3, или bombsUsed > levelBombs + s.bombs, или у TAPS
  score > job.tasks.size, или у TRAY score > длины sizes этой смены (правило § 3).
- Done — только оплачиваемая смена (ветка «Счёт N. Это игра ради рекорда.» удаляется):
  bonus: MATCH3 min(R.shiftBonusMax, score / R.shiftScorePerBonus); TAPS 0; CHANGE и TRAY
  min(R.shiftBonusMax, score) (у TRAY score — звёзды раунда); total = base + bonus; envelope,
  shiftsThisPeriod, jobShifts, дневник, наклейка заказа, bombs — как было.
  riddleAsked = false у MATCH3 и TRAY. records: MATCH3, CHANGE — max(records ?: 0, score); TRAY —
  max(bestStars(s, jobId) ?: 0, score) (наследие Match3 перезаписывается); TAPS не пишет.
  НОВЫЙ = score > (records[jobId] ?: 0) до смены — только MATCH3 и CHANGE.
  pay = ShiftPay(base, bonus, total).
- line: TAPS «{base} за три поручения. ✉ +{total} — PAY_LATER»; bonus > 0 «{base} за смену + {bonus}
  за {what}. ✉ +{total} — PAY_LATER», what: MATCH3 «булочки», CHANGE «сдачу», TRAY «★»; bonus == 0
  «{base} за смену. ✉ +{total} — PAY_LATER».
- why, по порядку, не больше 3: 1) уровень вырос этой сменой → «{name}: новый уровень {L}! Теперь
  {R'}», R' — строка котировки нового уровня: TAPS «{base'} за три поручения», иначе «{base'}–{top'}
  за смену» (пример «Боря: новый уровень 2! Теперь 7–11 за смену»; номер уровня — как в прогрессе
  «до уровня 2», Town.kt:678-680); иначе прогресс «{name}: {n} из {порог} смен до уровня {L+1}» или
  «{name}: высший уровень мастерства» — как было; 2) «Карманные приходят каждую неделю, зарплата —
  когда поработаешь»; 3) «Новый рекорд!», если НОВЫЙ (у TRAY не пишется).
Town.answerQuestion — отказы, riddles, riddleAsked, line — как было. M = первая работа town.jobs с
game ∈ {MATCH3, TRAY} (вместо `first { MATCH3 }`; M есть всегда — правило валидатора § 5). Верно:
M.game == MATCH3 → bombs += quizBombReward, why [«Бомбочка +{r} — для поля «{M.title}»»] (как было);
M.game == TRAY → bombs не меняются, why [«Подсказка Бори — на поднос»] (№ 48 б: подсказка — на
текущий поднос; кладёт её ViewModel через Tray.hint в срезе 1а). Неверно — как было.
Town.nextQuestion — без изменений.

### 5. Валидатор контента (ContentValidationTest, пишет test-author)
Правила — функцией, применяются к реальному контенту и к TRAY-копии из ORACLE. Для каждой работы с
непустыми steps (сейчас — job_bakery MATCH3; на копии — TRAY) и для каждой TRAY (у неё menu и steps
обязаны быть непустыми): id меню уникальны, title и emoji непустые; steps[0].fromShift == 0, fromShift
строго растут, kinds не убывают, 1 ≤ kinds ≤ menu.size; у каждой ступени и у demoSizes (если есть)
длина == R.shiftBonusMax, каждый размер k ∈ 1..4 и kinds ≥ min(k, 1 + k / 2) (для demoSizes — против
steps[0].kinds); R.riddleHint ≥ 1 и R.riddleHint ≤ min(L.drop(1)) − 1 для каждого L из sizes всех
ступеней и demoSizes (загадка приходит после первого покупателя — подсказка целиком ложится на
пустой поднос любого следующего; целиком заказ не собирает сам hint, § 2); жителей с arrivesWeek ≤ 1,
кроме job.resident, — не меньше 2 (демо и мечта открывают работу в периоде 1, Town.kt:618).
Правило «есть работа MATCH3» (ContentValidationTest.kt:617-626) → «есть работа MATCH3 или TRAY»
(загадке нужна работа M); проверка board/moves/demoMoves остаётся для каждой MATCH3. Сырой JSON
town.rules (ContentValidationTest.kt:151-156) — 11 полей, включая riddleHint. intro ступеней проходят
те же стоп-слова, что и прочие строки town (стоп-лист № 3, 10).

## SCOPE
variant: main
allow (task-scope.json, 4 пути):
  finny-pet/app/src/main/java/ru/finny/pet/domain/town/Tray.kt       (новый)
  finny-pet/app/src/main/java/ru/finny/pet/domain/town/Town.kt
  finny-pet/app/src/main/java/ru/finny/pet/domain/town/TownContent.kt
  finny-pet/app/src/main/java/ru/finny/pet/domain/town/TownState.kt   (только TownOutcome.pay)
protect (так он пойдёт в JSON; пересечение с allow — пусто, правило № 23):
  базовый (.claude/, CLAUDE.md, docs/, finny-pet/app/src/test/, finny-pet/app/build.gradle.kts,
  finny-pet/build.gradle.kts, finny-pet/settings.gradle.kts, finny-pet/gradle/, finny-pet/gradle.properties,
  finny-pet/gradlew, finny-pet/app/proguard-rules.pro, finny-pet/app/src/main/AndroidManifest.xml) +
  finny-pet/docs/, finny-pet/app/src/game/, finny-pet/app/src/classic/, finny-pet/app/src/main/assets/,
  finny-pet/app/src/main/res/, finny-pet/app/src/main/java/ru/finny/pet/PetSprites.kt,
  finny-pet/app/src/main/java/ru/finny/pet/data/, …/domain/{Content,Economy,GameState,Match3}.kt,
  …/domain/town/{Dictionary,Enums,EventDef,Migration,PetTalk,Prices,TownCodec,TownEvents}.kt,
  finny-pet/tools/art/, finny-pet/tools/{ui.py,demo_run.sh,office/}, корневые tools/{art_check.py,
  sheets.py,perf.sh,adbui.sh,emu.sh,ui_measure.py,mutation_probe.py,content_map_events.py,town_route.sh,
  rec.sh,mock_bakery.py} — поимённо, не каталогом (правило № 29)

## ANTI-SCOPE
UI (раунд, экран заказа, загадка значком, итог в сцене — срезы 1а/1б); GameViewModel и экраны game
(собираются как есть: исчерпывающих when по JobGame вне Town.kt нет); content.json (контент § 0
коммитит оркестратор); переключение job_bakery на TRAY вместе с названием «Помочь Боре» (журнал
«Смена: Помочь Боре», ТЗ 2.5.4) и тексты № 42 в («Помоги Боре в пекарне!») — срез 1а; Match3,
MiniGameScreen, jobLevelBombs, shiftScorePerBonus, quizBombReward — не трогать (чистка — срез 2);
GameState — без новых полей (bombs не трогать, MigrationTest.kt:53); Economy; доки (оркестратор);
публичный trayStep и счётчик replay (не нужны: ступень вне раунда ни одному экрану не нужна, игры
после лимита нет).

## БЮДЖЕТ
≤ 260 вставок в app/src/main (Tray.kt ≈ 120, Town.kt ≈ 90, TownContent.kt ≈ 15, TownState.kt ≈ 1);
новые файлы 1; зависимости 0

## ORACLE (test-author до кодера)
Новый app/src/test/java/ru/finny/pet/domain/town/TrayTest.kt и правки ShiftTest, TownContentTest,
ContentValidationTest, EventCatalogTest (пины, ставшие ложными по § 4 и № 47 б, обновляет
test-author, покрытие не ослабляется, тесты не удаляются без замены).
Станут красными на реальном контенте (16): ShiftTest — тесты, начинающиеся на строках 59, 113, 136,
158, 198, 283, 333, 353, 369, 382, 402, 442, 463, 549 (строки «База …» и «ради рекорда»; 463 и 549 —
заменить проверкой Refused(LIMIT) на тех же входах, не удалять); EventCatalogTest 820; TownContentTest
204. Зелёными останутся без правки: ShiftTest 238, 490, 733; BalanceSimTest, TownDayTest,
FiveDemoWeeksTownTest, WalletTest.
Копия контента с TRAY — хелпер в тестах: job_bakery.copy(game = TRAY, board = null, moves = null,
demoMoves = null), остальное из реального content.json (числа меню и ступеней в тестах не
дублировать — читать из копии). Профили S1aStand по умолчанию демо: у TRAY в демо sizes = demoSizes;
всё, что зависит от ступени (sizes, виды в заказе), проверять на demo = false.
TrayTest:
- детерминизм и влияние по § 3 «Случайность» дословно (значения n, period, shiftsThisPeriod, seed
  оттуда); кошелёк, day, diary, purchases не влияют;
- покупатели: из пула P, соседние разные, житель недели в раунде (недели 1–5, seed 1..50, demo =
  false); на копии, где у пекарни resident = "asya", при period 5 ни в одном заказе нет asya;
- ступень по jobShifts (0, 1, 2, 3, 5, 7, 20) на demo = false: menu = первые kinds, sizes = step.sizes,
  intro только при n == fromShift; демо — sizes = demoSizes;
- виды в заказе = min(k, 1 + k/2) на demo = false при jobShifts 1 (k = 3 → 2 вида) и 7 (k = 4 → 3),
  seed 1..20; изделия из menu, порядок job.menu;
- pointer по bestStars (нет записи, 0, 1, наследие 60); riddle (jobShifts 0 → false; ≥ 1 → true;
  riddleAsked или все 15 решены → false);
- put/take: не из витрины, полный поднос, чужой slot, done — без изменений; put в конец, take сохраняет
  порядок;
- give: неполный — accepted = false, раунд равен входу; верно с первого раза — звезда; верно после
  неверной — served без звезды; missing/extra с кратностью и порядком; после неверной отдачи на
  подносе только совпавшие; done — без изменений;
- hint: при m < size m' = min(m + n, size − 1) на всех подносах (перебор подносов из витрины для
  size 1..4); собранный верно поднос, n ≤ 0, done — без изменений; на полном подносе снимает лишнее с
  конца; добавленные — в конец; неверная отдача → put лишнего до полного подноса → hint снимает его
  с конца; оплата от hint не зависит (только от звёзд);
- «одинаковые подносы — одинаковая оплата при любых двух seed»: весь раунд верно с первого раза и весь
  раунд с одной неверной отдачей на покупателя → total finishShift совпадает для seed 1..20;
- trayRound = null для MATCH3 (реальный контент), TAPS, неизвестной работы, ночью, после лимита; на
  копии с kinds = 1 у ступени 0 — null и при demo = false, и при demo = true (витрина меньше видов);
- finishShift TRAY на копии: bonus = score, score > sizes.size и bombsUsed > 0 → BAD_SHIFT, riddleAsked
  сброшен, records через bestStars (наследие 60 → 2), без «Новый рекорд!», pay = ShiftPay, строки § 4
  дословно («6 за смену + 3 за ★. ✉ +9 — придёт с новым конвертом», «6 за смену. ✉ +6 — …»);
  потолок и top — на второй копии, где ещё и town.rules.copy(shiftBonusMax = 2) (sizes не менять,
  валидатор к ней не применяется): score 3 → bonus 2, top = base + 2, строка «6–8 за смену»;
- answerQuestion на копии: bombs не меняются, why [«Подсказка Бори — на поднос»];
- residentOfWeek: 1 osya, 2 tosha, 3 stepan, 4 kesha, 5 asya, 6 asya;
- bestStars: не TRAY, нет записи, 0, 3, 60 (→ null).
ShiftTest: строки котировки и итога § 4 («6–10 за смену», «6 за три поручения», «6 за смену + 2 за
булочки», «Боря: новый уровень 2! Теперь 7–11 за смену», top), № 47 б на всех работах до и после
плана (canPlay = false, строка LIMIT, finishShift — отказ LIMIT, наклейки нет), pay у MATCH3, TAPS,
CHANGE; стоп-слова и род — на всех новых строках (S1aChildText).
TownContentTest: JobGame = [MATCH3, TAPS, CHANGE, TRAY]; riddleHint в чтении rules и в списке
обязательных полей (11 полей); разбор job_bakery.menu/steps/demoSizes сверять с сырым JSON (как `town
rules читает числа из JSON`), без литералов § 0 — числа подбираются плейтестом; умолчания Job (menu,
steps пусты, demoSizes null) и TrayStep.intro = null. ContentValidationTest — § 5. EventCatalogTest 820
→ после лимита смена отказывает и наклейки заказа не даёт.
Шаг 4 (WORKFLOW, оркестратор): заглушка-контракт в одноразовой копии дерева — типы § 1–§ 2, Tray.*
возвращают вход, trayRound/residentOfWeek/bestStars = null, shiftQuote и finishShift прежние с top = 0
и pay = null; по XML TrayTest, ShiftTest и EventCatalogTest падают на ассертах, а не на компиляции.

## ACCEPTANCE (из finny-pet/, оркестратор)
1. ./gradlew testClassicDebugUnitTest --console=plain            -> exit 0, не UP-TO-DATE
2. tests = 581 (число @Test в app/src/test на BASE: grep -rho '@Test' app/src/test | wc -l), failed 0,
   skipped 0 — скрипт пресета по XML
3. ./gradlew testGameDebugUnitTest --console=plain               -> exit 0, то же число
4. ./gradlew assembleClassicDebug assembleGameDebug assembleGameRelease -> exit 0
5. ./gradlew lintClassicDebug lintGameDebug -> ошибок 0, предупреждений без сетевых ≤ 5 / 5 (замер
   2026-09-28 на cc90fed; команда пресета, п. 4)
6. git diff --name-only BASE -- app/src/test/ app/src/main/assets/ '*.gradle.kts' gradle/ gradle.properties -> пусто;
   git status --porcelain -uall -- app/src/test app/src/main/assets app/src/game app/src/classic -> пусто
7. git status --porcelain -uall -- app/src/main -> только 4 пути allow (Tray.kt — «??»);
   вставки: git diff --numstat BASE -- app/src/main/java + wc -l Tray.kt -> в сумме ≤ 260
8. grep -rnE '^import (android|androidx)|System\.(currentTimeMillis|nanoTime)|Random\(\)|\.shuffled\(\)|\.random\(\)|Random\.(Default|next)|LocalDate|Clock|TimeSource|Instant' app/src/main/java/ru/finny/pet/domain/ -> пусто
9. мутанты кода (python ../tools/mutation_probe.py <json> из finny-pet/; у каждого мутанта — поле expect
   с именем теста, который обязан упасть; одноразовые правки Town.kt/Tray.kt): give ставит звезду после
   неверной отдачи; hint без «− 1» (собирает заказ целиком); раунд без seed (Random(0)); без
   shiftsThisPeriod в смеси; без period в смеси; без n в смеси; неполная отдача ставит missed; canPlay
   по-старому (игра после лимита); bestStars без отсечения наследия; bonus TRAY без потолка (ловит копия
   с shiftBonusMax = 2); у TRAY нет проверки score > sizes; покупатель — житель работы; виды в заказе = k
   -> каждый красный
9б. зонды контента (тот же инструмент, file = app/src/main/assets/content/content.json): исходный
   зелёный; каждый — CAUGHT только если упал ожидаемый тест правила валидатора (expect — имя из
   ContentValidationTest, вписать при коммите оракула): steps[0].fromShift 1; fromShift не растёт;
   kinds убывает; kinds 7 > menu.size; sizes длины 3; размер 5; kinds 1 при k = 2; demoSizes длины 3;
   riddleHint 0; riddleHint 2; дубль id меню; пустой emoji; intro со словом «успей»; job_bakery.game
   "MATCH3" → "CHANGE" (нет MATCH3 и TRAY — у CHANGE своих правил валидатора нет). До приёмки
   оркестратор дорабатывает tools/mutation_probe.py: поле expect, полный список упавших, timeout
   прогона (зависание — вердикт HUNG, не CAUGHT)
10. живая проверка (эмулятор, game release, холодный старт): пекарня — «6–10 за смену»; рынок — «6 за
   три поручения»; итог смены у Марты — «6 за три поручения. ✉ +6 — придёт с новым конвертом»; после
   трёх смен — LIMIT и «Начать смену» неактивна; «Загадка Бори» под кнопкой остаётся (прежняя схема до
   среза 1а, как сейчас до плана и ночью — PlaceScreen.kt:366); снимки до/после

## ДОКИ (оркестратор, после приёмки, до ревью; греп по всему репозиторию)
Грепы: «ради рекорда», «Ради рекорда», «рекорд», «База », «за результат», «Счёт », «можно играть»,
«после раскладки», «бомб», «Булочки в ряд», «MATCH3|Match3», «JobGame», «riddleHint». Править то, что
стало ложью в срезе 0: finny-pet/docs/ECONOMY.md (смены: строки, № 47 б; TRAY, top, riddleHint — как
контракт домена, в game пекарня — Match3 до среза 1а), TEST_CASES TC-28, TC-32, TC-41,
BUILD_AND_DEMO (шаги 6, 9), DATA_MODEL (jobs, rules), ARCHITECTURE (Tray.kt — контракт, UI с 1а),
REQUIREMENTS_MATRIX (строка :122 — после трёх смен работа закрыта до нового конверта, № 47 б, имена
тестов по коммиту оракула; строка контракта TRAY — «в работе»), docs/BACKLOG.md:26; GAME_CONCEPT §5.2,
§9.2 № 4/12/13, §17 уже переписаны; что относится к срезам 1а/1б — не трогать.

## ПРИ БЛОКЕРЕ
STATUS: BLOCKED + один конкретный вопрос. Не изобретать.
```

## Журнал спеки
- 2026-09-28: черновик по концепции TOWN-J1 (§ 3, § 5, § 8, журнал) и решениям № 42–48.
- 2026-09-28, круг 1 критиков (4 измерения — концепция и решения, контракт против кода, алгоритм и
  арифметика, процесс и доки; каждый список — скептику): 48 находок, 25 подтверждены, 21 частично,
  2 опровергнуты. Приняты: мутант потолка надбавки эквивалентен → копия с shiftBonusMax = 2; свойство
  hint ложно на собранном подносе → m == size без изменений, m' = min(m + n, size − 1), «в конец»;
  фраза «остальные поля не влияют» противоречила § 3 → перечень зависимостей; влияние n и period
  проверялось ложнозелёно → значения 8 ↔ 9, 6 ↔ 7 и мутанты; виды в заказе и ступень — на demo =
  false; валидатор пула в демо → arrivesWeek ≤ 1, |P| < 2 → null; ветка M == null снята (валидатор);
  ShiftPay — в Town.kt; поимённый список красных тестов; git diff не видит новый Tray.kt → git status
  -uall и wc; protect поимённо (№ 29); зонды контента 9б; расширенный греп случайности; release в
  сборке; ТЗ — дословно; TownContentTest сверяет с сырым JSON, без литералов заглушек; ContentValidationTest
  :151-156 — 11 полей; план шага 4; № 47 б — оговорка «по умолчанию для прочих работ»; «Помочь Боре» —
  в ANTI-SCOPE как срез 1а; снятие лишнего в hint обосновано. Отличия от концепции § 3 (контракт —
  эта спека, GAME_CONCEPT §5.2): results вместо index/stars/served (порядок ★/● для ряда и итога),
  riddle в домене, trayRound nullable, без публичного trayStep, why загадки «на поднос» (№ 48 б),
  «Теперь 7–11 за смену» (формат котировки, у TAPS «Теперь 7 за три поручения»). Отклонены: пример «за
  булочки» в §3.1 (это итог Match3, не подноса); строка LIMIT (выбрана концепцией § 2 и вторым кругом).
  Правки GAME_CONCEPT по кругу: §0 (срезы), §5.2 (кнопка в срезе 0, цитата загадки, диапазон только у
  работ с надбавкой, оговорка № 47 б), §9.1, §9.2 № 4, §17.3 (1а — «Помочь Боре»), §17.4.
- 2026-09-28, круг 2 (2 критика — концепция и решения; контракт, оракул, приёмка; каждый — скептику):
  15 находок, 8 подтверждены, 5 частично, 2 опровергнуты. Приняты: null при витрине меньше нужного
  числа видов (иначе выбор видов циклом зависает на зонде «kinds 1»); зонды контента с ожидаемым
  именем теста, мутант «game = CHANGE» вместо TAPS (TAPS краснел чужим правилом), доработка
  mutation_probe.py (expect, полный список, timeout); обоснование снятия лишнего в hint было ложным
  («после неверной отдачи лишнего нет») — правило одно для любого подноса, случай в ORACLE; загадка
  на закрытой карточке пекарни — прежняя схема до 1а (п. 10). Доки: §0 GAME_CONCEPT — правило вместо
  перечня, даты решений, блок J1 в §17.3 — после таблицы среза 1; TOWN-J1.md — риск 3 (до 3 загадок в
  неделю), пометка № 48 б в «Выбранном направлении», пины среза 0 — ссылкой на эту спеку. Отклонены:
  оговорка № 47 б ещё в трёх местах (она в §18, §5.2 и КОНТЕКСТ); итог TAPS «за три поручения» (строка
  концепции, неточность старая — вопрос к срезу 1б); номера строк красных тестов (однозначны).

## Приёмка (оркестратор, 2026-09-28; BASE f4360b3, код кодера не закоммичен)
| П. | Команда | Итог |
|---|---|---|
| 1 | `./gradlew testClassicDebugUnitTest` (XML удалены перед прогоном) | exit 0, не UP-TO-DATE; tests = 581, failed 0, skipped 0 |
| 3 | `./gradlew testGameDebugUnitTest` | exit 0; 581 / 0 / 0 |
| 4 | `assembleClassicDebug assembleGameDebug assembleGameRelease` | exit 0 |
| 5 | `lintClassicDebug lintGameDebug` | exit 0; ошибок 0, предупреждений без сетевых 5 / 5 (база 5 / 5) |
| 6 | `git diff --name-only f4360b3 -- app/src/test/ app/src/main/assets/ '*.gradle.kts' gradle/ gradle.properties`; `git status --porcelain -uall -- app/src/test app/src/main/assets app/src/game app/src/classic` | пусто; пусто |
| 7 | `git status --porcelain -uall -- app/src/main`; `git diff --numstat` + `wc -l Tray.kt` | 4 пути allow (Tray.kt — `??`); вставки 121 + 19 + 1 + 90 = 231 ≤ 260 |
| 8 | греп случайности и времени по `domain/` | пусто (exit 1) |
| 9, 9б | `python tools/mutation_probe.py probes_all.json` — 20 мутантов кода (13 из п. 9 и 7 на ветки § 3–§ 4: загадка в первой смене, бомба у подноса, житель недели не навязан, рекорд с наследием, загадка не сброшена, intro в каждой смене, demoSizes не читаются) и 14 зондов контента, у каждого `expect` | SELF-CHECK 581 / 0; 34 из 34 CAUGHT ожидаемым тестом; RESTORE-CHECK 581 / 0; «ALL CAUGHT» |
| 10 | эмулятор `finni`, 360 dp (1080 × 1920 / 480), release (`pkgFlags` без DEBUGGABLE), холодный старт 2,6 с, демо-профиль с нуля, шрифт 1,0; маршрут — промахов 0 | пекарня «6–10 за смену»; итог «6 за смену. ✉ +6 — придёт с новым конвертом», «Боря: 1 из 6 смен до уровня 2»; Марта «6 за три поручения», итог «6 за три поручения. ✉ +6 — …»; после трёх смен — «Смены на неделе закончились — новые с новым конвертом», «●●●», «Начать смену» `enabled=false` (касание раунд не открывает), «Загадка Бори» под кнопкой — прежняя схема. Лист до/после — `finny-pet/screenshots/town/j10_live.jpg` |
Доки (греп по обоим `docs/`): ECONOMY (смены, № 47 б, TRAY, top, ShiftPay, riddleHint), TEST_CASES TC-28/32/41, BUILD_AND_DEMO шаги 6 и 9, DATA_MODEL (`records`, 11 полей rules, `jobs[]` подноса), ARCHITECTURE (`Tray`, `trayRound`), REQUIREMENTS_MATRIX (строка «Булочки в ряд» — имена переименованных тестов и № 47 б; строка «Поднос по заказу» — «в работе»), BACKLOG п. 1.
Ревью (reviewer, без контекста кодера): **PASS**, находок нет; перепроверил п. 1–8 сам (`--rerun-tasks`: 581 / 0 / 0
в обоих вариантах, сборка с release, lint 5 / 5, границы, греп), контракт — построчно, строки — побайтно, доки — по
коду. Вне вердикта: макет `app/src/game/…/mock/MockShopJob.kt:186, 216` (debug, вход из раздела взрослого) держит
строки «База 6 монет + до 4 за булочки» и «База 6 + 3 за булочки…» — править в срезе 1а вместе с экраном пекарни.
