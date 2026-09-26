```
TASK: TOWN-S1a — движок «Городка»: кошелёк, касса, неделя, цены лавок, миграция 1.3.0
EPIC: TOWN-S1 (docs/tasks/TOWN-S1.md)
BASE: <sha коммита оракула> (для кодера и ревьювера)
BRANCH: feat/town

## КОНТЕКСТ
Срез 1 концепции «Городок» (docs/GAME_CONCEPT.md §17.3, блоки `Town` и `Prices`, миграция).
Чистый Kotlin в domain/town: деньги и время недели. Смены (S1b), события (S1c) и UI (S1d–S1e)
приходят следующими задачами; интерфейс game после S1a ещё работает по правилам 1.3.0.
Спека прошла 4 критиков (контракт, концепция, тексты, арифметика демо-пути): 58 находок учтены.

## ТРЕБОВАНИЕ ТЗ
2.5.5 «распределение… до начала периода», «не больше доступного бюджета», «остаток»,
«изменить до подтверждения», «план / факт»; 2.5.6 «подтверждение, списание… без ухода в минус…
объяснение и варианты»; 2.5.7 «регулярный перевод», «снятие с подтверждением и предпросмотром»;
2.5.4 «для каждого начисления указываются источник и сумма»; 2.5.9 «путь восстановления»;
2.5.13 сохранение (миграция); 3.4 «тупика нет».

## CONTRACT
Пакет ru.finny.pet.domain.town, чистый Kotlin. Все функции §2–§6 — ЧЛЕНЫ классов (запись
`Town.f` — только обозначение метода). Обозначения: b = balance, sv = savings, jn = jarNeed,
jw = jarWant, r = reserve, M/O/S = plan.mandatory/optional/savings, fM/fO/fs =
factMandatory/factOptional/factSavings, p = цена товара в этой лавке, T = item.title,
t = T с первой буквой в нижнем регистре, step = content.rules.planStep,
days = town.rules.daysPerWeek. Числа в строках — без слова «монет». Строка вида «X: a → b»
в why и preview ОПУСКАЕТСЯ, если a == b.

### 0. Вне пакета (единственные правки)
- GameState: вычисляемое свойство в теле класса `val reserve: Int get() = balance - jarNeed - jarWant`
  (не сериализуется; до плана это «Не разложено»).
- Economy.buy(s: GameState, itemId: String, price: Int = content.item(itemId).price): Outcome —
  price заменяет item.price во ВСЕХ местах тела (проверка нехватки, баланс, Purchase.price, ledger).
  Остальное тело, тексты и правила не меняются (MvpRulesTest зелёный).
- TownContent: новое обязательное поле `spots: List<Spot>`; `@Serializable data class Spot(val id: String, val slot: String)`;
  Shop получает обязательное `at: String` (где лавка: «у реки»). Контент уже заведён оркестратором
  (town.spots — 6 мест, shops[].at).

### 1. Интерфейсы S0a → классы
Интерфейсы Town, Prices, Migration в TownState.kt удаляются и становятся классами с теми же
именами (одна реализация — интерфейс лишний; решение эпика 1): `class Prices(content: Content)`,
`class Town(content: Content)`, `class Migration(content: Content)`; внутри допустимы private
economy/prices. `interface Events` остаётся до S1c. Метод `repair` удаляется до среза 2.
TownOutcome, TownResult, EventResult, EventState, SeenPrice, DiaryLine — без изменений. Из Economy
НЕ вызываются answerQuiz, parentBonus, finishMiniGame. `content.town == null` → error(...) в
конструкторе (ошибка контента, не ребёнка).

Общие определения:
- PLANKEPT(x) = x.plan.confirmed && x.fM ≤ x.M && x.fO ≤ x.O && max(x.fs, 0) ≥ x.S — правило
  Economy.endPeriod (решение 3), вычисленное на любом состоянии.
- STAMP = «Штамп «По плану» на этой неделе не получится»; STAMP? — добавляется в preview, если
  PLANKEPT(s) && !PLANKEPT(состояние после этого действия).
- ETA(g, до, после) — строка срока мечты g (Economy.goalEta до и после): после == 0 и до != 0 →
  «{g.title}: хватит на мечту!»; оба известны и различаются → «{g.title}: ≈ {до} ✉ → ≈ {после} ✉»;
  до неизвестен, после известен → «{g.title}: ≈ {после} ✉»; иначе строки нет; нет цели — нет строки.
- SAVE(n) = [«Копилка: {sv} → {sv − n}»] + ETA на состоянии со savings − n и withdrawnThisPeriod + n.
- KEEP_DEMO = товары, на которые ссылается Fact.Buy(BuyTarget.Item(id), …) в исходах событий
  с demo.keepOnShelf (сейчас робот, змей, вертушка): в s.demo они не уходят с полки и отказ
  «уже есть дома» на них не действует (§7.4); owned — без дублей.

### 2. Prices
data class ShelfItem(val item: ShopItem, val price: Int, val was: Int? = null)
class Prices(content: Content) {
  fun price(s: GameState, itemId: String, shopId: String): Int?
  fun was(s: GameState, itemId: String, shopId: String): Int?
  fun cheapest(s: GameState, itemId: String): Int?
  fun basePrice(itemId: String): Int?
  fun shelf(s: GameState, shopId: String): List<ShelfItem>
}
- price: null, если лавки нет или товара нет в её `sells`. Иначе базовая цена из `sells`,
  поверх — переопределения событий: для каждого EventState e из s.events (по порядку списка)
  с e.period == s.period и событием с таким id в town.events, для каждого эффекта его `setup`:
  `PRICE(item, shop, price, until)` → цена `price`, `OFFER(item, shop, was, now, until)` → цена
  `now`, если item и shop совпали и (until == WEEK_END или (until == DAY_END и e.day == s.day)).
  Статус события не важен (EventState.period/day — неделя и день ПРИХОДА события). Побеждает
  последнее подходящее переопределение.
- was: `was` победившего переопределения, если это OFFER; иначе null.
- cheapest: минимум price по всем лавкам, где товар продаётся; null — нигде.
- basePrice: минимум базовых цен `sells` по всем лавкам, без событий; null — нигде.
- shelf: товары `sells` в их порядке как ShelfItem(item, price(s, id, shop)!!, was(s, id, shop)),
  кроме: eventOnly; unlockPeriod > s.period (кроме s.demo); keep-товар из s.owned (кроме KEEP_DEMO
  в s.demo). Неизвестная лавка → пустой список.

### 3. Касса: предпросмотр и оплата
enum class PayKind { PAY, CHEAPER, WAIT, MAKE_GOAL }
data class PayOption(val kind: PayKind, val label: String, val source: Source? = null,
    val itemId: String? = null, val preview: List<String> = emptyList(), val more: Boolean = false)
data class Quote(val itemId: String, val shopId: String?, val price: Int, val line: String,
    val note: String = "", val options: List<PayOption> = emptyList())
fun Town.quote(s: GameState, itemId: String, shopId: String?): Quote
fun Town.buyAt(s: GameState, itemId: String, shopId: String?, source: Source): TownResult

Цена p: shopId != null → prices.price(s, itemId, shopId); shopId == null → item.price (оплата в
сцене, только eventOnly-товары). Родная банка: MANDATORY → NEED, OPTIONAL → WANT, UNPLANNED → RESERVE.

Разрешённые источники, выполнимость, откуда монеты (всё — только после плана):
| категория | источник | выполним, если | откуда монеты |
|---|---|---|---|
| MANDATORY | NEED | jn ≥ p | jn −= p |
| MANDATORY | RESERVE | jn < p, r > 0, r + jw ≥ p − jn | jn → 0; из запаса min(r, p − jn); остаток из jw |
| MANDATORY | TRANSFER_WANT | jn < p, jw ≥ p − jn | jw −= (p − jn), jn → 0 |
| MANDATORY | SAVINGS | sv ≥ p | копилка −p, банки не трогаются |
| OPTIONAL | WANT | jw ≥ p | jw −= p |
| OPTIONAL | TRANSFER_NEED | jw < p, jn ≥ p − jw | jn −= (p − jw), jw → 0 |
| OPTIONAL | SAVINGS | sv ≥ p | копилка −p |
| UNPLANNED | RESERVE | r > 0, r + jw ≥ p | из запаса min(r, p), остаток из jw (составная оплата §7.5 С4) |
| UNPLANNED | WANT | jw ≥ p | jw −= p |
| UNPLANNED | SAVINGS | sv ≥ p | копилка −p |
Прочие пары не разрешены. SAVINGS = Economy.withdraw(p) и сразу Economy.buy(…, p): баланс и
банки не меняются, withdrawnThisPeriod += p, в ledger две строки. Остальные — Economy.buy(…, p) и
правка банок по таблице (запас вычисляется сам).

Проверки buyAt по порядку (quote — проверки 1–6 так же); первая сработавшая → Refused(line),
состояние не меняется:
| # | условие | line |
|---|---|---|
| 1 | s.pet == null | «Сначала создай питомца» |
| 2 | s.asleep | «Сейчас ночь — сначала проснёмся» |
| 3 | !plan.confirmed | «Покупки — после того как разложишь монеты» |
| 4 | товара нет в контенте; или shopId != null и (price == null или item.eventOnly); или shopId == null и !item.eventOnly | «Этого товара здесь нет» |
| 5 | item.keep и itemId ∈ s.owned (кроме KEEP_DEMO в s.demo) | «Эта вещь уже есть дома» |
| 6 | shopId != null и товара нет в shelf(s, shopId) (не открылся по неделе) | «Этого товара здесь нет» |
| 7 | пара не разрешена: OPTIONAL + RESERVE | «Запас — на нужное и на всякий случай. Хотелки — из банки «Хочу»» |
| 7' | прочие неразрешённые пары | «Так оплатить нельзя» |
| 8 | пара разрешена, но не выполнима: источник родной и quote.line не пуст → quote.line; SAVINGS → «В копилке только {sv}»; WANT (не родной) → «В «Хочу» только {jw}»; иначе | «Так оплатить нельзя» |
Done: покупка; последняя Purchase получает shop = shopId и source = source; keep-товар — в owned
(без дублей) и на первое свободное место своего типа (§6); shopId != null →
DiaryLine(period, day, «Купили {shop.at}: {t}»).
TownOutcome.line: MANDATORY + RESERVE → «Хорошо, что был запас! В новом плане дадим «Нужному»
побольше?», если ДО этой покупки в неделе не было покупки MANDATORY с source RESERVE и после
покупки fM > M; иначе «Запас выручил». Все прочие — item.reaction с {pet} → pet.name.
TownOutcome.why — движение монет «было → стало», не больше 3 строк (правило a == b выше):
  NEED [«Из «Нужного»: {jn} → {jn'}»]; WANT [«Из «Хочу»: {jw} → {jw'}»];
  MANDATORY + RESERVE [«Из «Нужного»: {jn} → 0», «Из запаса: {r} → {r'}», «Из «Хочу»: {jw} → {jw'}»,
    «Еда и уход нужны каждую неделю. Запас — для сюрпризов»] — первые 3 после опускания;
  UNPLANNED + RESERVE [«Из запаса: {r} → {r'}», «Из «Хочу»: {jw} → {jw'}»];
  TRANSFER_WANT [«Из «Хочу»: {jw} → {jw'}», «Из «Нужного»: {jn} → 0»];
  TRANSFER_NEED [«Из «Нужного»: {jn} → {jn'}», «Из «Хочу»: {jw} → 0»];
  SAVINGS [«Из копилки: {sv} → {sv'}»].
TownOutcome.effects и eventResults в S1a пусты (события — S1c).

quote. Отказы 1–6 → Quote(line = строка отказа, options = []); price = 0, если товара нет в
контенте или лавка его не продаёт, иначе p. Дальше по категории; WALLET = «Не хватает {p − b}:
в кошельке {b}, {t} {p}»; miss = p − (родная банка).
- MANDATORY.
  jn ≥ p → line "", options [PAY NEED «Купить за {p}» preview [«Из «Нужного»: {jn} → {jn − p}»] + STAMP?].
  Иначе line = b < p ? WALLET : «В «Нужном» {jn}, {t} стоит {p}. Не хватает {miss}». Основные:
   CHEAPER?; PAY RESERVE? (выполним) label: y = max(0, miss − r); y == 0 → (jn > 0 ? «Добавить {miss}
   из запаса» : «Из запаса {p}»), y > 0 → «Запас {miss − y} + «Хочу» {y}»; preview [«{T} будет!»,
   «Штамп «Нужное куплено» — да» (если после покупки за неделю есть и FOOD, и CARE), STAMP?];
   PAY TRANSFER_WANT? «Взять {miss} из «Хочу»» preview [«В «Хочу» будет {jw − miss}», STAMP?].
   Под «Ещё» (more = true): PAY SAVINGS? «Из копилки {p}» preview SAVE(p) + STAMP?.
   Нет ни одного основного → первым WAIT (more = false), «Ещё» сохраняется.
- OPTIONAL.
  jw ≥ p → line "", options [PAY WANT «Купить за {p}» preview [«Из «Хочу»: {jw} → {jw − p}»] + STAMP?].
  Иначе b < p → line WALLET, основные по порядку MAKE_GOAL?, WAIT, CHEAPER? (§6.4, §11 шаг 7);
  b ≥ p → line «В «Хочу» {jw}, {t} стоит {p}. Не хватает {miss}», note «Запас {r} — на нужное и на
  всякий случай» (если r > 0), основные CHEAPER?, WAIT, MAKE_GOAL?. Под «Ещё»: PAY TRANSFER_NEED?
  «Взять {miss} из «Нужного»» preview [«В «Нужном» будет {jn − miss}», STAMP?]; PAY SAVINGS? как выше.
- UNPLANNED (оплата непредвиденного; отложить нельзя — решение 16, WAIT и CHEAPER нет никогда).
  Основные: PAY RESERVE «Из запаса {p}» (r ≥ p) preview [«Из запаса: {r} → {r − p}»]; PAY WANT
  «Из «Хочу» {p}» (jw ≥ p) preview [«Из «Хочу»: {jw} → {jw − p}»]; PAY RESERVE «Запас {r} + «Хочу» {p − r}»
  (0 < r < p, jw < p, r + jw ≥ p) preview [«Из запаса: {r} → 0», «Из «Хочу»: {jw} → {jw − (p − r)}»].
  PAY SAVINGS «Из копилки {p}» (sv ≥ p) preview SAVE(p) + STAMP? — more = true, если основные есть,
  иначе more = false. line: "" при наличии основных; иначе b < p ? WALLET : «Запас {r}, {t} стоит
  {p}. Не хватает {p − r}». Вариантов может не быть вовсе: «бесплатно» решает сцена события (срез 2).
- CHEAPER: товар из shelf(s, shopId), той же категории и need, не этот, цена p2 < p, и для него есть
  выполнимый основной PAY (MANDATORY: jn ≥ p2, или RESERVE, или TRANSFER_WANT по таблице;
  OPTIONAL: jw ≥ p2); из них — самый дорогой, при равенстве — первый по полке; label «Дешевле: {t2} {p2}»,
  itemId = его id, preview []. При shopId == null CHEAPER нет.
- MAKE_GOAL: item.keep, prices.basePrice(itemId) ≥ town.rules.customGoalFromItemMin и s.goal?.id !=
  «item:{itemId}»; label «Сделать мечтой», itemId = этот товар, preview [«Мечта: {t} — {basePrice}»].
- WAIT: label «Подождать нового конверта», preview [].
- Свойства (оракул): каждый PAY → buyAt с его source → Done; CHEAPER → quote его товара содержит PAY
  с more = false; MAKE_GOAL → makeGoal Done; STAMP в preview PAY ⇔ PLANKEPT до && !PLANKEPT после
  buyAt; основных (more = false) ≤ 3.

### 4. План недели и копилка
fun Town.confirmPlan(s: GameState): TownResult
Refused по порядку: pet «Сначала создай питомца»; asleep «Сейчас ночь — сначала проснёмся»;
plan.confirmed «План уже готов — до нового конверта он не меняется»; plan.total == 0 «Разложи хотя
бы часть монет»; plan.total > b «В плане больше монет, чем в кошельке».
Done: Economy.confirmPlan; S > 0 → Economy.deposit(S) («сначала заплати себе», §6.3); jarNeed = M,
jarWant = O; stateVersion = 1 (профиль, сыгранный движком, не мигрирует повторно). line: S > 0 →
«План готов! В копилку +{S}, запас на всякий случай: {r}», S == 0 → «План готов! Запас на всякий
случай: {r}» (r — после взноса). why: [«Сначала откладываем, потом тратим — так мечта ближе»,
«Запас — на нужное и на всякий случай, не на «хочу»»]. Правка плана до подтверждения —
Economy.setPlan напрямую (Town её не оборачивает).

fun Town.deposit(s: GameState, from: Source, amount: Int): TownResult
fun Town.depositPreview(s: GameState, from: Source, amount: Int): List<String>
fun Town.withdraw(s: GameState, amount: Int): TownResult
fun Town.withdrawPreview(s: GameState, amount: Int): List<String>
fun Town.transfer(s: GameState, from: Source, to: Source, amount: Int): TownResult
fun Town.transferPreview(s: GameState, from: Source, to: Source, amount: Int): List<String>
FROM = «Запас: {r} → {r − a}» (RESERVE) или ««Хочу»: {jw} → {jw − a}» (WANT), a = amount.
- deposit (взнос после плана, §5.5, §11 шаг 8). Refused по порядку: pet; asleep; !confirmed
  «Отложить можно после раскладки»; amount ≤ 0 «Выбери сумму больше нуля»; from ∉ {RESERVE, WANT}
  «Так отложить нельзя»; RESERVE и a > r «В запасе только {r}»; WANT и a > jw «В «Хочу» только {jw}».
  Done: Economy.deposit(a); WANT → jw −= a. line «Копилка +{a}»; why [FROM, «Копилка: {sv} → {sv + a}»]
  + ETA (после — на состоянии после взноса).
- depositPreview: [FROM] + ETA; недопустимый ввод (любой отказ deposit) — пустой список.
- withdraw — только ДО плана: снятое идёт в «Не разложено» (§5.1). Refused: pet; asleep;
  plan.confirmed «После раскладки из копилки платят только у кассы»; a ≤ 0 «Выбери сумму больше
  нуля»; a > sv «В копилке только {sv}». Done: Economy.withdraw(a); line «Из копилки {a} — в «Не
  разложено»»; why = SAVE(a).
- withdrawPreview = SAVE(a) + [STAMP], если в неподтверждённом плане S > 0 (взнос S при
  подтверждении даст fs = S − a < S); недопустимый ввод — пустой список.
- transfer — восстановление у кассы, только RESERVE → NEED и WANT → NEED. Refused: pet; asleep;
  !confirmed «Перенос — после раскладки»; a ≤ 0 «Выбери сумму больше нуля»; другая пара «Так
  перенести нельзя»; a больше источника «В запасе только {r}» / «В «Хочу» только {jw}». Done:
  jn += a, WANT → jw −= a. line ««Нужное» +{a}», why [FROM, ««Нужное»: {jn} → {jn + a}»].
- transferPreview = [FROM, ««Нужное»: {jn} → {jn + a}»] + [«Потратишь больше плана — штампа «По
  плану» не будет»], если jn + a > M − fM; недопустимый ввод — пустой список.

### 5. Дни, сон, конец недели (§3.2, §3.3)
fun Town.sleep(s: GameState): TownResult
fun Town.wake(s: GameState): TownResult
fun Town.endWeek(s: GameState): TownResult
fun Town.weekEndPreview(s: GameState): List<String>
fun Town.planTweaks(s: GameState, summary: PeriodSummary): List<Recovery.PlanTweak>
fun Town.chooseTweak(s: GameState, tweak: Recovery.PlanTweak?): TownResult
- sleep. Refused: pet; !plan.confirmed «Сначала разложим монеты — потом спать» (UI открывает
  раскладку); asleep «Уже ночь». day == days → результат endWeek(s). Иначе Done: asleep = true,
  day НЕ меняется. line — строка дня из DiaryLine этого (period, day) по порядку: нет → «Спокойный
  день дома»; одна → её текст; две и больше → «{первая}. {вторая}».
- wake. Refused: pet; !asleep «Уже утро». Done: asleep = false; plan.confirmed → day += 1 (ночь
  обычного дня), иначе day не меняется (ночь после итога — утро дня 1 новой недели; план ночью
  не подтверждается, отказ confirmPlan №2). line: !plan.confirmed и period > 1 и day == 1 →
  «Новый конверт: карманные {allowance}» + (« + зарплата {W}», если W > 0) + (« + бонус {B}», если
  B > 0), W и B — shiftEarned и parentBonus последнего итога в history; иначе «Доброе утро!».
- endWeek — третий сон (sleep в день days) или демо-кнопка «Сразу к итогу недели». Refused: pet;
  !plan.confirmed «Сначала разложим монеты — потом спать»; !demo и day < days «Неделя ещё идёт».
  Done по шагам:
  1) Economy.endPeriod(s) (правило плана 1.3.0 без изменений, §6.6; убывание, рост, карманные);
  2) последний PeriodSummary в history заменяется копией с shiftEarned = сумма строк envelope с
     text, начинающимся на «Смена: », и parentBonus = сумма строк на «Бонус от взрослого: »
     (строки пишет S1b);
  3) конверт приходит: balance += сумма envelope; его строки дописываются в ledger после строк
     endPeriod в том же порядке; envelope = [] (сумма ledger == balance сохраняется);
  4) jarNeed = jarWant = 0, day = 1, asleep = true, shiftsThisPeriod = 0;
  5) заготовка: plan = BudgetPlan(M, O, S прошлого плана, confirmed = false); если total > balance —
     избыток снимается сначала с O, потом с S, потом с M (каждое не ниже 0). Первым урезают «Хочу»:
     так учит П3, копилка — «сначала заплати себе».
  line по штампам (mandatoryCovered, planKept, saved; fs = summary.factSavings): 3 → «Всё по плану,
  копилка +{fs}!»; 0 → «Неделя позади. Начнём с корма и мыла?»; 1–2 → «{первый полученный} {шаг
  первого неполученного}»; полученные по порядку: mandatoryCovered «Еда и уход были всю неделю!»,
  planKept «Траты — по плану!», saved «Копилка +{fs}!»; шаг для неполученного по тому же порядку:
  «В новом конверте начнём с корма и мыла», «В новом конверте попробуем по плану», «В новом конверте
  попробуем отложить».
  why: [««Нужное»: план {M}, потрачено {fM}», ««Хочу»: план {O}, потрачено {fO}», «Копилка: план {S},
  отложено {max(fs, 0)}»] (числа итога).
- weekEndPreview — непокрытые нужды для окна «Закончить неделю?»: нет покупки need FOOD → «Еда на
  этой неделе не куплена — {pet} проголодается»; нет CARE → «Уход на этой неделе не куплен — {pet}
  испачкается». Всё куплено — пустой список.
- planTweaks — «Что поменяем в понедельник?» (§6.5). s — состояние ПОСЛЕ endWeek, summary — его
  последний итог. Шаги: кандидаты по порядку → фильтр выполнимости → дубли по dir (остаётся
  первый) → первые три.
  Кандидаты: (1) PlanTweak(NEED, step), если !summary.mandatoryCovered или summary.factMandatory >
  summary.plan.mandatory; (2) для каждого EventState e из s.events по порядку с e.period ==
  summary.period, e.verdict == MISTAKE, событие e.id есть в town.events и e.outcome в
  0..outcomes.lastIndex — все PlanTweak из outcomes[e.outcome].recovery по порядку (outcome null
  или −1 и неизвестный id пропускаются); (3) PlanTweak(SAVINGS, step), если s.goal != null.
  Выполнимость (на неподтверждённом plan): WANT — total + n ≤ b; NEED и SAVINGS — total + n ≤ b или
  избыток (total + n − b) ≤ O; RESERVE — O ≥ n.
- chooseTweak. Refused: pet; plan.confirmed «План уже готов»; невыполнимо — «В плане больше монет,
  чем в кошельке» (NEED, WANT, SAVINGS) или «В «Хочу» меньше {n}» (RESERVE). null → Done без
  изменений. Иначе: NEED → M += n, SAVINGS → S += n (и если total > b — O −= избыток), WANT →
  O += n, RESERVE → O −= n. line "". Разрешена и ночью (итог недели идёт ночью).

### 6. Дом, мечта, места, посещение
fun Town.place(s: GameState, spotId: String, itemId: String?): TownResult
fun Town.makeGoal(s: GameState, itemId: String): TownResult
fun Town.achieveGoal(s: GameState): TownResult
fun Town.visit(s: GameState, placeId: String): TownOutcome
- Места — town.spots по порядку; место занято стартовой вещью, если у homeItem spot == его id
  (стартовые вещи стоят на своих местах всегда). Свободное место для вещи — первое по порядку с
  slot == item.slot, не занятое стартовой вещью и не ключ в s.placed. Нет свободного — вещь только
  в owned (сундук). Автоматически вещь с места не убирается никогда.
- place. Refused: места нет «Такого места нет»; занято стартовой вещью «Здесь стоит {t стартовой}»;
  itemId != null и его нет в owned «Этой вещи нет в сундуке»; item.slot != spot.slot «Эта вещь сюда
  не встанет». itemId == null → место освобождается (вещь остаётся в owned). Иначе вещь снимается с
  прежнего места (если стояла) и ставится сюда; стоявшая здесь — в сундук. line "".
- makeGoal («Сделать мечтой», §5.6). Refused: pet; нет товара «Этого товара здесь нет»; item.keep и
  itemId ∈ owned «Эта вещь уже есть дома»; !item.keep или basePrice < customGoalFromItemMin
  «Мечтой можно сделать вещь от {min}». Done: Economy.chooseGoal(Goal(«item:{itemId}», T, item.emoji,
  basePrice)); line «Мечта: {t} — {basePrice}».
- achieveGoal. Refused: pet; asleep; нет цели «Сначала выбери мечту»; sv < цены «До мечты ещё
  {цена − sv}». Done: Economy.achieveGoal; цель вида «item:{x}» → x в owned и на свободное место;
  DiaryLine «Мечта сбылась: {title цели с маленькой буквы}»; line «Мечта сбылась: {title}!».
  (Что открывают мечты town.goals — срез 2.)
- visit (вход в место, §6.1). Неизвестное место → состояние без изменений. visited += placeId (без
  дублей). Если у места есть лавка: для каждого ShelfItem её полки запись seenPrices[itemId] =
  SeenPrice(shop.id, price, s.period), если записи нет, или её period < s.period, или price <
  записанной, или записанная — той же лавки (холодильник помнит самую низкую цену недели и лавку).
  line "". (ENTER-триггеры — S1c.)

### 7. Migration (§16.3)
class Migration(content: Content) { fun migrate(s: GameState): GameState }
- stateVersion ≥ 1 → s без изменений (равно s).
- Иначе: plan.confirmed → jarNeed = clamp(M − fM, 0, b), jarWant = clamp(O − fO, 0, b − jarNeed);
  seed == 0 → MIGRATION_SEED = 20260926L (константа в Migration); keep-товары покупок текущей недели
  (неизвестные id пропускаются) → owned без дублей и на свободные места; day = 1; asleep = false;
  stateVersion = 1. Монеты не появляются и не исчезают: balance, savings, ledger, history, bombs,
  achievedGoals — без изменений.
- Взнос S в 1.3.0 делался вручную: недовнесённое S − depositedThisPeriod остаётся в запасе, ребёнок
  довносит сам (deposit из запаса); на фикстуре: банки 5 / 30, запас 80, из них 30 — невнесённое S.
- Вызывается и на свежем состоянии (новый профиль, сброс): план не подтверждён — меняются только
  seed, day, asleep, stateVersion. Кто зовёт migrate после newGame/reset — S1d (VM).

### 8. Инварианты (после любой функции Town, из любого достижимого состояния)
jn ≥ 0, jw ≥ 0, r ≥ 0, sv ≥ 0; сумма ledger == balance; envelope в balance не входит; owned не
уменьшается; до подтверждения плана jn == jw == 0; jn уменьшают только покупки MANDATORY с
источником NEED, RESERVE, TRANSFER_WANT и покупки OPTIONAL с источником TRANSFER_NEED — покупки
UNPLANNED, SAVINGS и взносы jn не трогают; Refused не меняет состояние.

### Не определено здесь (не решать, не додумывать)
Приход и сопоставление событий, наклейки, заметки NOTE, факты — S1c. Смены, «Загадка Бори»,
бонус взрослого — S1b. Реплики питомца по тапу, вызов migrate из VM — S1d. «Бесплатно» у С4, полка
мастерской с lamp_new, что открывают мечты — срез 2.

## SCOPE
variant: main
allow:   finny-pet/app/src/main/java/ru/finny/pet/domain/town/   (новые файлы ≤ 4 + TownState.kt, TownContent.kt)
         finny-pet/app/src/main/java/ru/finny/pet/domain/GameState.kt   (только reserve)
         finny-pet/app/src/main/java/ru/finny/pet/domain/Economy.kt     (только параметр price в buy)
protect: базовый (docs/WORKFLOW.md) + finny-pet/app/src/main/ кроме allow + finny-pet/docs/ + content.json

## ANTI-SCOPE
Events и факты; смены; UI и GameViewModel; правка текстов, правил и других функций Economy;
classic; content.json (заведён оркестратором); доки (пишет оркестратор); новые зависимости.

## БЮДЖЕТ
≤ 1000 вставок по git diff --shortstat -- app/src/main; новые файлы ≤ 4; зависимости 0

## ORACLE (test-author до кодера; файлы — app/src/test/java/ru/finny/pet/domain/town/)
- PricesTest — базовые цены; PRICE/OFFER из s.events текущей недели, DAY_END только в день прихода,
  прошлой недели — нет; последний побеждает; was; cheapest; basePrice; shelf (eventOnly,
  unlockPeriod с демо и без, owned keep, KEEP_DEMO в демо).
- CheckoutTest — quote и buyAt: таблица источников (каждая разрешённая пара — выполнимый и
  невыполнимый случай, каждая неразрешённая — отказ), отказы 1–8 со строками, строки line / note /
  why / label / preview дословно (в т. ч. на товаре женского рода: «Купили у реки: каша»), свойства §3
  перебором по всем товарам лавок и нескольким состояниям банок, демо-путь §11 шаг 7 (b 32, палатка:
  «Не хватает 28: в кошельке 32, домик-палатка 60», [Сделать мечтой][Подождать][Дешевле: шарик 10]),
  только SAVINGS → WAIT первым, UNPLANNED (care_doctor, shopId = null): все три основных, составная,
  копилка на основном при единственном выполнимом, пустой список; первая и вторая доплата из
  запаса; опускание «0 → 0»; owned, место keep-вещи, KEEP_DEMO; дневник.
- WalletTest — confirmPlan (взнос S сразу, банки, stateVersion, строки, ночью — отказ), deposit из
  запаса и «Хочу», preview с ETA (в т. ч. «хватит на мечту»), withdraw только до плана и
  withdrawPreview со STAMP, transfer и transferPreview, инварианты §8 после каждой функции на
  цепочке действий (с TRANSFER_NEED), envelope не в balance и приходит в ledger отдельными строками.
- TownDayTest — сон без плана не двигает день, sleep без wake не двигает день, wake двигает, третий
  сон = endWeek, демо-пропуск с любого дня, «Неделя ещё идёт» без демо, цепочка endWeek →
  confirmPlan ночью (отказ) → wake → day == 1 и строка конверта, строка дня (0 / 1 / 2+ записи),
  утро нового конверта с зарплатой и бонусом и без, заготовка с урезанием O → S → M, строки итога
  для 0 / 1 / 2 / 3 штампов, weekEndPreview, planTweaks (каждое условие, кандидаты из событий с
  outcome null / −1 / неизвестным id, выполнимость за счёт O, дубли, ≤ 3) и chooseTweak.
- PlanRuleTest — доплата нужного из запаса и перенос из «Хочу» снимают planKept; перенос RESERVE →
  NEED и покупка из «Нужного» — тоже, и STAMP был в preview; непредвиденное (care_doctor, shopId =
  null) из запаса и из «Хочу» — не снимает; взнос из «Хочу» не снимает; envelope не влияет.
- MigrationTest — фикстура src/test/resources/town/state_1_3_0.json: банки 5 / 30, запас 80, монеты
  и журнал целы, stateVersion 1, повторный migrate — без изменений; копия фикстуры с fun_ball в
  покупках → owned и место; свежее состояние; newGame → createPet → Town.confirmPlan →
  Town.deposit(WANT) → migrate == то же состояние.
- ContentValidationTest (дописать): spots — 6, id уникальны, slot каждого keep-товара есть среди
  мест, spot стартовых вещей ссылается на место; у каждой лавки непустое at.
Тексты для ребёнка из всех функций S1a (line, note, why, label, preview, Refused) проверяются на
стоп-слова §9.2 №3, №7, №10 и род (списки ContentValidationTest) на всех путях оракула.

## ACCEPTANCE (из finny-pet/, оркестратор)
  1. testClassicDebugUnitTest testGameDebugUnitTest -> exit 0
  2. tests = 244 + <оракул> в каждом варианте, failed 0, skipped 0 (подсчёт по XML)
  3. assembleClassicDebug assembleGameDebug assembleGameRelease -> exit 0
  4. lintClassicDebug lintGameDebug -> 0 ошибок, предупреждений без сетевых ≤ 5 / 6
  5. git diff --name-only BASE -- app/src/test/ '*.gradle.kts' gradle/ gradle.properties -> пусто
  6. git diff --name-only BASE -> только allow
  7. grep -rnE '^import (android|androidx)|System\.currentTimeMillis|nanoTime|LocalDate|Clock|Random\(' app/src/main/java/ru/finny/pet/domain/town/ -> пусто
  8. git diff --shortstat BASE -- app/src/main -> ≤ 1000 вставок
  9. мутационные зонды оркестратора (каждый → оракул краснеет): доплата из запаса не обнуляет jn;
     sleep двигает day; конверт не приходит в endWeek; quote кладёт TRANSFER_NEED на основной
     экран; STAMP без проверки PLANKEPT; миграция теряет монету запаса; OFFER прошлой недели действует;
     confirmPlan не ставит stateVersion

## ПРИ БЛОКЕРЕ
STATUS: BLOCKED + один конкретный вопрос.
```
