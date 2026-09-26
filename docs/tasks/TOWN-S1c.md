```
TASK: TOWN-S1c — события «Городка»: приход, окна, сопоставление фактов, демо-доска
EPIC: TOWN-S1 (docs/tasks/TOWN-S1.md)
BASE: <sha коммита оракула> (для кодера и ревьювера)
BRANCH: feat/town

## КОНТЕКСТ
Срез 1, блок `Events` (GAME_CONCEPT §7, §17.3): финансовые задания — события в мире; ребёнок решает
их поступком (купить, пройти мимо, разложить монеты), исход определяется по факту, а не по ответу.
Шесть событий среза (П1 p1_list, П3 p3_price_up, С1 c1_robot_sale, С3 c3_almost, Пк1 pk1_cheaper_food
и pk1_cheaper_soap, Пк3 pk3_super_food) и заказы-подработки JOB (job_bakery_help, job_market_help).
Продолжает Town (S1a, S1b); UI — S1d–S1e. Спека прошла 3 критиков (контракт, прогон семи событий по
реальному контенту, концепция и тексты): 37 находок учтены.

## ТРЕБОВАНИЕ ТЗ
2.5.8 «задания… ситуация с выбором и последствием… не ограничиваются выбором ответа», «объяснение при
любом исходе», «в демо… без привязки к реальному времени»; 2.5.9 «путь восстановления»; 2.5.14
«добавление новых заданий… без переработки основной логики»; 2.6 «не менее 6 заданий по 3 темам…
прохождение правильного и ошибочного варианта»; 2.5.3 карточка «В городке» (активное задание).

## CONTRACT
Пакет ru.finny.pet.domain.town. Все новые функции — методы класса Town (фасад; запись `Town.f` —
обозначение метода). `interface Events` из TownState.kt удаляется; логика событий — внутри Town или
internal-класса пакета. Обозначения — как в TOWN-S1a (b, jn, jw, r, M, KEEP_DEMO, NIGHT …).
Внешняя пара «» у строк — разделитель.

### 0. Контент и типы
- town.eventsOff: List<String> = emptyList() (TownContent) — id событий, которые в этой сборке не
  приходят и не начинаются (решение эпика 5). Заведено оркестратором в content.json до оракула: p2_party, c2_lamp, c4_paw,
  pk2_change, pk4_everyone_has, windfall_granny, job_kassa_help, job_courier_help.
  ЖИВЫЕ события = town.events минус eventsOff. Контент (оркестратор, тем же коммитом): у c1_robot_sale
  и c3_almost triggers = [ENTER:foma] — распродажа приходит ценником в лавке, а не утренним анонсом
  (§9.2 №17); строка OK исхода BUY:FOOD:SAVINGS у p3_price_up — «Копилка выручила — корм куплен».
- TownOutcome получает поле `arrived: List<String> = emptyList()` — id событий, пришедших этим действием.
  TownOutcome.effects = эффекты setup пришедших событий и effects разрешённых исходов в порядке
  применения (для анимаций и подсветки GOTO в UI); у действий без событий — пустой.
- data class Card(val title: String, val eventId: String? = null, val place: String? = null)

### 1. Условия (requires и when) на состоянии x
WeekAtLeast(n): period ≥ n; DayIs(d): day == d; BeforePlan: !plan.confirmed; AfterPlan: plan.confirmed;
NotBought(n): в purchases недели нет товара с need n; Owns(i): i ∈ owned; NotOwned(i): i ∉ owned;
HasGoal: goal != null; SavingsPctAtLeast(p): goal != null и savings × 100 ≥ p × goal.price;
ResidentArrived(id): у жителя arrivesWeek != null и period ≥ arrivesWeek; WantAtLeast(n): jw ≥ n;
ReserveAtLeast(n): r ≥ n; NotBroken(i): i ∉ broken; AnyOf: хотя бы одно.
ДЕМО-ОСЛАБЛЕНИЯ (только requires, при s.demo; §7.4): WeekAtLeast, DayIs, ResidentArrived, NotBought —
true; NotOwned(i) — true для i ∈ KEEP_DEMO. when исходов не ослабляется.
ЗАКРЫТОЕ МЕСТО: у события place с places[place].opensBy.goal, которой нет в achievedGoals, — событие
не приходит и в демо не начинается (и в демо тоже).

### 2. Приход
Живое событие E с kind != JOB ПРИХОДИТ в момент проверки, если одновременно:
 a) нет его EventState со статусом ACTIVE;
 b) у E ещё нет EventState, или его последний EventState — DONE, в сработавшем исходе
    (outcomes[outcome], outcome ≥ 0) есть RETRY_NEXT_WEEK и последний приход был в неделю < s.period
    (возврат из пула — срез 2);
 c) s.period ≥ arrives.week и (s.period > arrives.week или arrives.day == null или s.day ≥ arrives.day);
 d) requires выполнены (с демо-ослаблениями), место не закрыто;
 e) у E нет triggers и это ПЛАНОВАЯ проверка, или у E есть триггер, сработавший этим действием:
    PlanConfirmed — в confirmPlan, Enter(place) — в visit(place);
 f) у E без triggers — ещё не исчерпан лимит дня: EventState с (period, day) == (s.period, s.day), чьё
    событие без triggers и не JOB, меньше town.rules.eventsPerDay (продолжения по триггеру лимит не
    занимают и не проверяют);
 g) !s.asleep.
Проверяются ОДНИМ проходом по порядку (arrives.week, arrives.day ?: 0, порядок в content); триггер —
только условие e); пришедшее сразу учитывается в лимите f для следующих. ПРИХОД = EventState(E.id, ACTIVE, null, null, s.period, s.day) в конец
s.events + эффекты setup (§4) + id в TownOutcome.arrived. Событие без outcomes (WINDFALL) при приходе
сразу DONE (verdict null, outcome −1), его наклейка — в stickers тогда же.
Плановая проверка идёт: в wake (после смены дня), в confirmPlan (после наблюдения плана и триггеров
PlanConfirmed), в visit (после триггеров Enter) и в tick.
fun Town.tick(s: GameState): TownOutcome — только плановая проверка; line ""; зовёт VM после создания
профиля, загрузки и миграции (S1d).

### 3. Наблюдения и сопоставление («один факт — одно событие», §7.3)
Методы Town пишут наблюдения ПОСЛЕ своего основного действия, на состоянии после него:
 - buyAt → покупка (itemId, source, shopId, цена p);
 - confirmPlan → «план подтверждён»;
 - deposit → Deposit; withdraw → Withdraw; makeGoal → MakeGoal(itemId);
 - endWeek, ДО Economy.endPeriod → WeekEndNo(FOOD), потом WeekEndNo(CARE) — для нужд, которых нет в
   purchases недели, каждое отдельным наблюдением;
 - pass(eventId) → Skip — только для этого события.
Исход O события E ПОДХОДИТ к наблюдению, если:
 - O.fact = Buy(target, src) и наблюдение — покупка: Item(id) — id == itemId; ByNeed(n) — item.need == n;
   Tag(t) — t ∈ item.tags; и src == null или src == source;
 - O.fact = BuyAt(item, rank) и наблюдение — покупка этого item с shopId != null: rank = CHEAPEST, если
   p ≤ prices.cheapest(s, item), иначе DEARER;
 - O.fact = Plan(covers) и наблюдение — «план подтверждён»: covers == (M ≥ LIST(E)), LIST(E) = сумма по
   E.params.list значений prices.cheapest(s, id) ?: item.price;
 - Skip, Deposit, Withdraw, MakeGoal(i), WeekEndNo(n) — равенство с наблюдением;
 - Repair, CheckChange, MakeCard, Attend — в срезе 1 наблюдать нечем, не подходят никогда;
 - и O.condition (when) == null или выполнено на состоянии после действия (без демо-ослаблений).
Исход события — ПЕРВЫЙ подходящий сверху вниз. Кандидаты — живые события не-JOB с ACTIVE-состоянием,
у которых есть подходящий исход; выбирается ОДНО: наибольший priority, при равенстве — пришедшее
раньше (порядок в s.events). Остальные остаются ACTIVE. У наблюдения Skip из pass кандидат только его
событие.
РАЗРЕШЕНИЕ: его EventState → DONE, verdict = O.verdict, outcome = индекс O; эффекты O.effects (§4);
E.sticker — в s.stickers (без дублей: наклейка за любой исход); EventResult(E.id, O.verdict, O.line,
E.sticker, O.recovery) в TownOutcome.eventResults.
СТРОКА ДЕЙСТВИЯ: если действие разрешило событие, TownOutcome.line = O.line, а why = [прежняя line
действия, если не пустая] + прежний why — первые 3. Исключения: (1) прежняя line — приглашение доплаты
из запаса «Хорошо, что был запас! …» (решение 2) — остаётся line, а O.line идёт первой в why; (2) sleep
и endWeek свою line и why не меняют — результаты событий только в eventResults. Иначе line и why
действия не меняются.
Refused-ветки методов наблюдений не пишут.

### 4. Эффекты (setup — при приходе; effects — при разрешении)
PRICE, OFFER — только в setup, действуют через Prices (S1a: WEEK_END — до конца недели прихода,
DAY_END — только в день прихода), состояние не меняют; NOTE(t) → t в s.notes (без дублей); STICKER(id) →
в s.stickers (без дублей); ITEM_GIVE(i) → owned и место (как покупка keep, S1a §6); ITEM_BREAK(i) → i в
s.broken (без дублей); ITEM_FIX(i) → убрать из s.broken; GOTO, NONE — состояние не меняют (GOTO — в
effects для UI); DEMO_GOAL — только из demo.setup (§6).
COINS (любой источник), SHORT_CHANGE, STAT, а также PRICE и OFFER в effects исхода — срез 2: у живых
событий их нет; встретив при применении (setup — при приходе или startEvent, effects — при
разрешении; в конструкторе НЕ проверять), движок бросает IllegalStateException с id события.

### 5. Окна и умолчания
- default.fact допустим только Skip или WeekEndNo (иначе IllegalStateException с id события).
- sleep (не последний день), перед засыпанием: ACTIVE живые события с default.at == DAY_END
  разрешаются фактом default.fact — исход = первый исход с этим фактом и выполненным when; не
  нашлось → DONE, verdict = null, outcome = −1, без наклейки и БЕЗ EventResult.
- endWeek, до Economy.endPeriod: (1) наблюдения WeekEndNo (§3); (2) оставшиеся ACTIVE с default
  (WEEK_END или DAY_END) разрешаются так же; (3) все ещё ACTIVE живые события закрываются: DONE,
  verdict = null, outcome = −1, без наклейки и EventResult (событие без подходящего факта не висит
  на следующей неделе; С4 со сценой — срез 2). Результаты (1)–(2) — в eventResults итога недели.

### 6. Доска, карточка, «Пройти мимо», демо
fun Town.activeEvents(s: GameState): List<EventDef>   // ACTIVE живые не-JOB, в порядке s.events
fun Town.orders(s: GameState): List<EventDef>          // заказы JOB, не хранятся в s.events
fun Town.card(s: GameState): Card                      // «В городке» — никогда не пустая
fun Town.pass(s: GameState, eventId: String): TownResult
fun Town.demoBoard(s: GameState): List<EventDef>
fun Town.startEvent(s: GameState, eventId: String): TownResult
- orders: живые события kind == JOB в порядке content, у которых shiftQuote(s, params.job).paid, requires
  выполнены БЕЗ демо-ослаблений (в демо заказ тоже идёт после плана, §4.3, §11) и место не закрыто.
  finishShift ЛЮБОЙ оплачиваемой смены (S1b) кладёт sticker первого живого события kind == JOB с
  params.job == jobId в s.stickers (без дублей; requires и paid не проверяются).
- card: первое из activeEvents → Card(E.title, E.id, E.place); иначе первый из orders → Card(E.title,
  E.id, E.place); иначе Card(«В городке спокойно — загляни на доску»).
- pass. Refused (состояние не меняется): pet «Сначала создай питомца»; asleep NIGHT; нет ACTIVE-состояния живого события с
  этим id «Такого события сейчас нет»; у события нет исхода с фактом Skip «Здесь решают делом»;
  исход Skip не подходит по when «Здесь решают делом». Done: разрешение (§3), line = строка исхода.
- demoBoard: s.demo → все живые не-JOB события, чьё место не закрыто, в порядке content; иначе [].
- startEvent — только демо, повтор не ограничен. Refused: pet; asleep NIGHT; !demo «Событие придёт
  само»; нет живого не-JOB события с этим id «Такого события нет»; место закрыто «Место откроет
  мечта»; уже ACTIVE «Событие уже идёт»; requires (с демо-ослаблениями) не выполнены → применить
  demo.setup и проверить снова; всё ещё нет → отказ со строкой по ПЕРВОМУ невыполненному условию
  requires, вычисленному на состоянии ПОСЛЕ demo.setup (само состояние при отказе не меняется):
  BeforePlan «План уже готов — событие придёт с новым конвертом»; AfterPlan «Сейчас не начать: после
  раскладки монет»; HasGoal «Сейчас не начать: нужна мечта»; SavingsPctAtLeast(p) «Сейчас не начать:
  в копилке нужно {ceil(p × goal.price / 100)}» (goal — после setup; С3 при копилке 30: мечта 50 →
  «…нужно 35»); WantAtLeast(n) «Сейчас не начать: в «Хочу» нужно {n}»; ReserveAtLeast(n) «Сейчас не
  начать: в запасе нужно {n}»; Owns(i) «Сейчас не начать: сначала нужна вещь «{T}»»; NotOwned(i)
  «Сейчас не начать: {t} уже есть дома»; NotBroken(i) «Сейчас не начать: {title вещи дома с маленькой
  буквы} уже не работает»; AnyOf — строка первого варианта; прочие (в демо не встречаются) —
  «Сейчас не начать». Pk1 (корм и мыло) приходят сами в confirmPlan и в демо: startEvent после плана
  отвечает «Событие уже идёт».
  Done: ПРИХОД (§2, без проверок b, c, e, f) + line = E.intro, arrived = [id].
- DEMO_GOAL(pct): s.goal = Goal(«demo_goal», «Мечта для показа», «⭐», max(town.rules.customGoalFromItemMin,
  floor(savings × 100 / pct))). Цель, для которой requires уже выполнены, не заменяется (demo.setup
  применяется только при невыполненных requires).

### 7. Места и наклейки
visit — порядок шагов: visited → триггеры Enter этого места и плановая проверка (приход, setup) →
seenPrices по полке на состоянии ПОСЛЕ прихода (П3 при входе на рынок: холодильник запомнит 30) →
наклейка места (places[placeId].sticker) в s.stickers без дублей. Дом — тоже место: S1d вызывает
visit(«home») при входе в комнату, так даётся наклейка дома.

### 8. Стоп-лист на выдаче
Все строки, которые отдаёт ребёнку Town (line, why, EventResult.line, Card.title, отказы) на всех
путях оракула — без стоп-слов §9.2 №3, №7, №10 и слов с родом (списки ContentValidationTest).

## SCOPE
variant: main
allow:   finny-pet/app/src/main/java/ru/finny/pet/domain/town/   (Town.kt, TownState.kt, TownContent.kt, новые файлы ≤ 2)
protect: базовый + finny-pet/app/src/main/ кроме allow + finny-pet/docs/ + content.json

## ANTI-SCOPE
Пул после исчерпания и hash(seed) (срез 2); сцены П2, С2, С4, Пк2 и их факты; UI; Economy; content.json
(eventsOff заводит оркестратор); доки (оркестратор); новые зависимости.

## БЮДЖЕТ
≤ 600 вставок в app/src/main; новые файлы ≤ 2; зависимости 0

## ORACLE (test-author до кодера; app/src/test/java/ru/finny/pet/domain/town/)
- EventCatalogTest — для каждого из семи живых событий: приход (по расписанию, по триггеру, лимит
  дня, ночью — нет, eventsOff — не приходит), верный и ошибочный путь ПОСТУПКОМ (buyAt, confirmPlan,
  pass, endWeek) — вердикт, строка исхода как line и why с прежней строкой, наклейка, recovery,
  эффекты (цены П3 и С1 через Prices, NOTE Пк1 и Пк3), умолчание в конце недели, RETRY_NEXT_WEEK
  (Пк1 возвращается на следующей неделе, без него — нет), «один факт — одно событие» (корм у реки при
  активных П3, Пк1, Пк3 → П3; при Пк1 и Пк3 → Пк1 по priority; повтор Пк3 на неделе 3 уступает П3 —
  принято для среза 1), запуск со свежего демо-профиля: демо §11 (план 40/20/30) + взнос 10 из «Хочу»
  → С3 стартует (мечта «Мечта для показа» 57), копилка 30 → отказ «…нужно 35»; П3 после покупки
  корма стартует и закрывается в endWeek без факта (не висит на неделе 2); отказы startEvent со
  строками; карточка (событие → заказ → «спокойно»), orders (после плана и в демо тоже, исчезает без
  жетонов), наклейки места (порядок шагов visit, seenPrices после прихода П3), наклейка заказа за
  смену до плана, pass (отказы и строки), строка доплаты из запаса при П3, TownOutcome.effects,
  ITEM_BREAK/FIX на тестовом контенте, IllegalStateException на COINS, STAT, SHORT_CHANGE и PRICE в
  effects при применении; C1 и C3 приходят только при входе к Фоме.
- ContentValidationTest (дописать): каждый id из town.eventsOff есть в town.events.
- BalanceSimTest — 5 и 10 недель, три детерминированные стратегии, названные в тесте (мечта «Самокат»):
  «всё по плану» (план §15), «всё на хотелки» (нужное по минимуму, остальное в «Хочу», каждый день —
  самая дорогая хотелка по карману), «копилка максимум»; через реальные методы Town со сменами S1b:
  balance ≥ 0 и инварианты S1a §8 на каждом шаге; тупика нет (после плана у quote каши на рынке
  всегда есть вариант, а сон и конец недели доступны); на неделе 1 всё купить нельзя (сумма цен
  хотелок обеих полок > кошелька при плане); «Самокат» 150 копится к плану недели 4 по сценарию §15;
  наклейки всех живых событий достижимы в ОБЪЕДИНЕНИИ трёх стратегий (С3 не приходит при пустой
  копилке, С1 — если робот уже куплен: пула в срезе 1 нет).
- FiveDemoWeeksTownTest — демо-профиль, пять недель подряд через endWeek из дня 1: период, рост и стадии.
Стоп-лист §8 — на всех строках выдачи.

## ACCEPTANCE (из finny-pet/, оркестратор) — как в S1a, п. 1–7; п. 2 — tests = <после S1b> + <оракул>;
  п. 8 — ≤ 600 вставок;
  9. мутанты: priority игнорируется; исход берётся последний, а не первый; when не проверяется;
     eventsOff не действует; лимит дня не действует; RETRY_NEXT_WEEK не возвращает; default не
     применяется в endWeek; WeekEndNo наблюдается после endPeriod (покупки уже обнулены);
     seenPrices до прихода в visit; ACTIVE без факта не закрывается в endWeek; orders с демо-ослаблениями
```
