```
TASK: TOWN-S0a — контракт движка «Городка» (чистый Kotlin)
EPIC: TOWN-S0 (docs/tasks/TOWN-S0.md)
BASE: <sha коммита оракула> (для кодера и ревьювера)
BRANCH: feat/town

## КОНТЕКСТ
Срез 0 концепции «Городок» (docs/GAME_CONCEPT.md §17.3). Без типов словаря событий и сигнатур
движка test-author не может написать оракул среза 1 до кодера. Реализаций Town/Events/Prices/
Migration здесь нет — только типы, парсер строкового формата и новые поля состояния.

## ТРЕБОВАНИЕ ТЗ
2.5.14: «добавление новых заданий, товаров, целей… без переработки основной логики» — событие
становится записью данных из закрытого словаря (§7.3). 2.5.13: «сохранение прогресса» —
сохранение 1.3.0 читается новой схемой.

## CONTRACT
Пакет ru.finny.pet.domain.town. Чистый Kotlin: без android.*/androidx.*, без времени и случайности.

### 1. Перечисления (все @Serializable, верхний уровень пакета)
enum class Source      { NEED, WANT, RESERVE, SAVINGS, TRANSFER_NEED, TRANSFER_WANT }
enum class PriceRank   { CHEAPEST, DEARER }
enum class CoinSource  { CHANGE_RETURN, WINDFALL }
enum class Stat        { HUNGER, CLEAN, MOOD }
enum class Until       { DAY_END, WEEK_END }
enum class TweakDir    { NEED, WANT, SAVINGS, RESERVE }
enum class Verdict     { GOOD, OK, MISTAKE }
enum class EventKind   { ASK, PRICE, OFFER, BREAK, CHANGE, WINDFALL, MISHAP, JOB }
enum class EventStatus { ACTIVE, DONE }
Need.UNPLANNED, Category.UNPLANNED и Template — НЕ здесь, это S0b (Content.kt).

### 2. Словарь — sealed-типы и КАНОНИЧЕСКАЯ строка каждого значения
Имена: подтипы ВЛОЖЕНЫ в свой интерфейс — Fact.Buy, Fact.Skip, Condition.AnyOf,
EventEffect.Price, EventEffect.None, Recovery.PlanTweak, Trigger.Enter и т. д.; BuyTarget —
отдельный sealed interface верхнего уровня с вложенными Item/ByNeed/Tag. Значения без полей —
data object, с полями — data class; имена и типы полей — ровно как в колонке «значение»
(все <id> и тексты — String, все числа — Int, перечисления — свои типы).

Лексика:
- <id> — `[a-z][a-z0-9_]*`;
- <n> — `[0-9]+`, неотрицательный Int; <signed> — `[+-]?[0-9]+`, Int (только в STAT), печать без «+»;
  число вне диапазона Int — ошибка разбора;
- <Need> — имя любого значения ru.finny.pet.domain.Need (набор берётся из Need.entries);
  <Need2> — только FOOD или CARE;
- регистр значим;
- пробел допустим ТОЛЬКО по краям всей строки, после «(», перед «)», вокруг «,» и вокруг «||».
  Любой другой пробел — ошибка (`week >= 2`, `BUY: FOOD`, `NOTE (x)`).
Печать — только каноническая форма: аргументы через ", ", варианты AnyOf через " || ",
без пробелов по краям.

sealed interface Fact   (@Serializable(with = FactSerializer::class))
| строка                                    | значение                                        |
|-------------------------------------------|-------------------------------------------------|
| BUY:<id>  / BUY:<id>:<Source>             | Buy(target = BuyTarget.Item(id), source)        |
| BUY:<Need> / BUY:<Need>:<Source>          | Buy(target = BuyTarget.ByNeed(need), source)    |
| BUY:#<id> / BUY:#<id>:<Source>            | Buy(target = BuyTarget.Tag(tag = id), source)   |
| BUY_AT:<id>:CHEAPEST / BUY_AT:<id>:DEARER | BuyAt(item: String, rank: PriceRank)            |
| SKIP / DEPOSIT / WITHDRAW                 | Skip / Deposit / Withdraw                       |
| PLAN:NEED>=LIST / PLAN:NEED<LIST          | Plan(needCoversList: Boolean) — true / false    |
| REPAIR:<id> / REPAIR:<id>:<Source>        | Repair(item: String, source: Source?)           |
| CHECK_CHANGE / MAKE_CARD                  | CheckChange / MakeCard                          |
| MAKE_GOAL:<id>                            | MakeGoal(item: String)                          |
| ATTEND:<id>                               | Attend(place: String)                           |
| WEEK_END_NO:<Need2>                       | WeekEndNo(need: Need)                           |
Buy(target: BuyTarget, source: Source? = null); Repair(item: String, source: Source? = null);
source = null — «из любой банки», в строке без третьей части.
BuyTarget.Item(id: String), BuyTarget.ByNeed(need: Need), BuyTarget.Tag(tag: String).

sealed interface Condition   (@Serializable(with = ConditionSerializer::class))
| week>=<n>          | WeekAtLeast(n: Int)          | hasGoal            | HasGoal                    |
| day==<n>           | DayIs(day: Int)              | savingsPct>=<n>    | SavingsPctAtLeast(pct: Int)|
| beforePlan         | BeforePlan                   | resident:<id>      | ResidentArrived(id: String)|
| afterPlan          | AfterPlan                    | want>=<n>          | WantAtLeast(n: Int)        |
| notBought:<Need2>  | NotBought(need: Need)        | reserve>=<n>       | ReserveAtLeast(n: Int)     |
| owns:<id>          | Owns(item: String)           | notBroken:<id>     | NotBroken(item: String)    |
| notOwned:<id>      | NotOwned(item: String)       |                    |                            |
| <c> || <c> [|| <c>…] | AnyOf(options: List<Condition>) — ≥ 2 варианта, ни один не AnyOf  |

sealed interface EventEffect   (@Serializable(with = EventEffectSerializer::class))
| COINS(<n>, <CoinSource>)                      | Coins(n: Int, source: CoinSource)             |
| SHORT_CHANGE(<n>)                             | ShortChange(n: Int)                           |
| STAT(<Stat>, <signed>)                        | StatChange(stat: Stat, n: Int)                |
| ITEM_GIVE(<id>) / ITEM_BREAK(<id>) / ITEM_FIX(<id>) | ItemGive / ItemBreak / ItemFix(item: String) |
| PRICE(<id>, <id>, <n>, <Until>)               | Price(item: String, shop: String, price: Int, until: Until) — price: новая АБСОЛЮТНАЯ цена в этой лавке |
| OFFER(<id>, <id>, <n>, <n>, <Until>)          | Offer(item: String, shop: String, was: Int, now: Int, until: Until) |
| NOTE(<текст>)                                 | Note(text: String) — всё между первой «(» и последней «)», trim, не пусто; запятые и скобки внутри — часть текста; любой символ после последней «)» — ошибка |
| STICKER(<id>) / GOTO(<id>)                    | Sticker(id: String) / Goto(place: String)     |
| DEMO_GOAL(<n>)                                | DemoGoal(pct: Int)                            |
| NONE                                          | None                                          |

sealed interface Recovery   (@Serializable(with = RecoverySerializer::class))
| RESERVE_TO_SAVINGS / RESERVE_TO_NEED / WANT_TO_NEED | ReserveToSavings / ReserveToNeed / WantToNeed |
| PLAN_TWEAK(<TweakDir>, <n>)                          | PlanTweak(dir: TweakDir, n: Int)             |
| RETRY_NEXT_WEEK / RETURN_LATER                       | RetryNextWeek / ReturnLater                  |

sealed interface Trigger   (@Serializable(with = TriggerSerializer::class))
| ENTER:<id>     | Enter(place: String) |
| PLAN_CONFIRMED | PlanConfirmed        |

### 3. Парсер и печать
object TownCodec {
    fun fact(s: String): Fact
    fun condition(s: String): Condition
    fun effect(s: String): EventEffect
    fun recovery(s: String): Recovery
    fun trigger(s: String): Trigger
    fun print(fact: Fact): String
    fun print(condition: Condition): String
    fun print(effect: EventEffect): String
    fun print(recovery: Recovery): String
    fun print(trigger: Trigger): String
}
Свойства: print(x(s)) == каноническая форма s; x(print(v)) == v для любого v, ПОЛУЧЕННОГО
РАЗБОРОМ. Конструкторы инвариантов не проверяют — валидация только в парсере.
Ошибка разбора — IllegalArgumentException (или наследник); message содержит «<вход после trim>»
в «ёлочках». Оракул проверяет тип и это вхождение; указание на ошибочный фрагмент желательно,
но не проверяется. Ошибка обязана быть на: пустой или пробельной строке; неизвестном имени
факта/условия/эффекта/восстановления/триггера; неизвестном значении перечисления (Source,
PriceRank, Need, CoinSource, Stat, Until, TweakDir); Need кроме FOOD/CARE в WEEK_END_NO и
notBought; недостающей или лишней части (`BUY_AT:x`, `SKIP:x`, `BUY:FOOD:`, `BUY:#`, `owns:`,
`OFFER(a, b, 1)`, `NOTE(a)b`); не-числе или числе вне Int на месте <n>/<signed>; знаке у <n>
(`COINS(+5, WINDFALL)`); id не по лексике (`BUY:Food`, `GOTO(1a)`); PLAN не NEED>=LIST/NEED<LIST;
пустом NOTE(); пустой стороне `||`; сравнении не из таблицы (`week>2`, `want>0`); пробеле не
на разрешённом месте.

Строковые сериализаторы: object FactSerializer, ConditionSerializer, EventEffectSerializer,
RecoverySerializer, TriggerSerializer : KSerializer<…> — PrimitiveKind.STRING, decode через
TownCodec.<вид>(decodeString()), encode через TownCodec.print. Ошибка разбора пробрасывается.

### 4. Запись события (§7.2) — @Serializable, имена JSON = имена свойств (кроме when)
data class EventDef(
    val id: String,
    val kind: EventKind,
    val title: String,                        // имя для карточки «В городке», доски и «Дневника»
    val theme: Theme? = null,                 // ru.finny.pet.domain.Theme; null у JOB и WINDFALL
    val place: String? = null,
    val resident: String? = null,
    val arrives: Arrives = Arrives(),
    val triggers: List<Trigger> = emptyList(),  // любой из них; пусто — без триггера
    val requires: List<Condition> = emptyList(),
    val setup: List<EventEffect> = emptyList(),
    val intro: String,
    val outcomes: List<EventOutcome> = emptyList(),
    val default: EventDefault? = null,
    val sticker: String? = null,
    val repeat: Repeat = Repeat(),
    val demo: Demo = Demo(),
    val priority: Int = 0,
    val params: EventParams = EventParams(),
    val followUp: FollowUp? = null,
)
data class Arrives(val week: Int = 1, val day: Int? = null)
data class EventOutcome(val fact: Fact, val verdict: Verdict, val line: String,
    @SerialName("when") val condition: Condition? = null,
    val recovery: List<Recovery> = emptyList(), val effects: List<EventEffect> = emptyList())
data class EventDefault(val at: Until, val fact: Fact)
data class Repeat(val afterWeeks: Int = 0, val pool: Boolean = false, val weeks: List<Int> = emptyList())
data class Demo(val startNow: Boolean = true, val keepOnShelf: Boolean = false,
    val setup: List<EventEffect> = emptyList(), val wrongPath: String = "")
data class EventParams(val list: List<String> = emptyList(), val job: String? = null)
data class FollowUp(val day: Int, val place: String)
Смысл (KDoc, одна строка на класс): outcomes проверяются сверху вниз, срабатывает первый
исход с подходящим fact и выполненным when; repeat.pool — возврат в пул не раньше afterWeeks
недель; repeat.weeks — повторные приходы по расписанию (MISHAP, WINDFALL); params.list —
товары списка П1; params.job — работа заказа JOB.

Эталонная запись. Разбор — Json { ignoreUnknownKeys = true } (как ContentRepository). Печать
обратно — encodeToString той же конфигурацией (encodeDefaults = false); сравнение —
parseToJsonElement(печать) == parseToJsonElement(эталон).
{
  "id": "c1_robot_sale", "kind": "OFFER", "title": "Распродажа робота", "theme": "SAVINGS",
  "place": "foma", "resident": "foma",
  "arrives": { "week": 2, "day": 2 },
  "requires": ["afterPlan", "notOwned:fun_robot"],
  "setup": ["OFFER(fun_robot, shop_foma, 40, 25, WEEK_END)"],
  "intro": "Робот! Было 40 — стало 25. Такая распродажа бывает.",
  "outcomes": [
    { "fact": "SKIP", "verdict": "GOOD", "line": "Скидка — это дешевле, а не нужнее." },
    { "fact": "BUY:fun_robot:WANT", "verdict": "OK", "line": "На «Хочу» хватило — это твой выбор." },
    { "fact": "BUY:fun_robot:SAVINGS", "verdict": "MISTAKE", "line": "Робот из копилки — мечта отодвинулась.",
      "recovery": ["RESERVE_TO_SAVINGS"] },
    { "fact": "BUY:fun_robot:TRANSFER_NEED", "verdict": "MISTAKE", "line": "Робот из «Нужного» — на корм может не хватить.",
      "recovery": ["RESERVE_TO_NEED", "PLAN_TWEAK(NEED, 10)"] }
  ],
  "default": { "at": "WEEK_END", "fact": "SKIP" },
  "sticker": "st_robot_sale",
  "repeat": { "afterWeeks": 3, "pool": true },
  "demo": { "keepOnShelf": true, "wrongPath": "Положить в «Хочу» меньше 25 и купить через «Взять из „Нужного“»" }
}

### 5. Состояние (§16.2)
Новый файл в domain/town, все @Serializable:
data class EventState(val id: String, val status: EventStatus, val verdict: Verdict? = null,
    val outcome: Int? = null, val period: Int, val day: Int)   // outcome — индекс в outcomes; -1 — default
data class SeenPrice(val shop: String, val price: Int, val period: Int)
data class DiaryLine(val period: Int, val day: Int, val text: String)

GameState.kt — ТОЛЬКО дописать в конец (порядок, имена, типы, умолчания дословно):
GameState(…,
    val stateVersion: Int = 0, val day: Int = 1, val asleep: Boolean = false, val seed: Long = 0,
    val jarNeed: Int = 0, val jarWant: Int = 0, val envelope: List<LedgerEntry> = emptyList(),
    val owned: List<String> = emptyList(), val placed: Map<String, String> = emptyMap(),
    val broken: List<String> = emptyList(), val houseColor: String? = null,
    val shiftsThisPeriod: Int = 0, val jobShifts: Map<String, Int> = emptyMap(),
    val records: Map<String, Int> = emptyMap(), val events: List<EventState> = emptyList(),
    val stickers: List<String> = emptyList(), val visited: List<String> = emptyList(),
    val seenPrices: Map<String, SeenPrice> = emptyMap(), val notes: List<String> = emptyList(),
    val planDraft: BudgetPlan? = null, val freeFunDay: Int = 0, val diary: List<DiaryLine> = emptyList(),
)
Purchase(…, val shop: String? = null, val source: Source? = null)
PeriodSummary(…, val shiftEarned: Int = 0)
Существующие поля, их порядок и вычисляемые свойства не меняются.

### 6. Сигнатуры движка (реализаций нет — срез 1; спека среза 1 вправе уточнить)
Реализации получат Content через конструктор; в сигнатурах контента нет.
data class TownOutcome(val state: GameState, val line: String = "", val why: List<String> = emptyList(),
    val effects: List<EventEffect> = emptyList(), val eventResults: List<EventResult> = emptyList())
data class EventResult(val eventId: String, val verdict: Verdict, val line: String,
    val sticker: String? = null, val recovery: List<Recovery> = emptyList())
sealed interface TownResult {
    data class Done(val outcome: TownOutcome) : TownResult
    data class Refused(val line: String) : TownResult   // варианты кассы (§6.4) — срез 1
}
interface Town {
    fun buyAt(s: GameState, itemId: String, shopId: String?, source: Source): TownResult
    fun repair(s: GameState, homeItemId: String, source: Source): TownResult
    fun reserveToSavings(s: GameState, amount: Int): TownResult
    fun transfer(s: GameState, from: Source, to: Source, amount: Int): TownResult
    fun confirmPlan(s: GameState): TownResult
    fun finishShift(s: GameState, jobId: String, score: Int, bombsUsed: Int): TownResult
    fun sleep(s: GameState): TownResult
    fun wake(s: GameState): TownResult
    fun planTweaks(s: GameState, summary: PeriodSummary): List<Recovery>
    fun parentBonus(s: GameState, reasonIndex: Int): TownResult
    fun answerQuestion(s: GameState, questionId: String, optionIndex: Int): TownResult
}
interface Events {
    fun available(s: GameState): List<EventDef>
    fun start(s: GameState, id: String): TownResult
    fun enter(s: GameState, placeId: String): TownOutcome
    fun observe(s: GameState, fact: Fact, place: String? = null): TownOutcome
}
interface Prices { fun price(s: GameState, itemId: String, shopId: String): Int? }
interface Migration { fun migrate(s: GameState): GameState }
KDoc у каждой функции — одна строка «что делает и какой пункт концепции»; обязательно:
buyAt — «shopId = null: оплата в сцене по цене товара; source = RESERVE при нехватке запаса
добирает из «Хочу» (составная оплата С4)»; finishShift — «закрывает JOB-событие с
params.job == jobId без EventResult»; enter — «ENTER-триггеры, RETURN_LATER, visited»;
observe — «place — где совершено действие; SKIP, CHECK_CHANGE, MAKE_GOAL, MAKE_CARD, ATTEND,
DEPOSIT, WITHDRAW приходят сюда без обёртки Town».

### Отступления от концепции (в том же коммите правится GAME_CONCEPT §5.4, §5.9, §7.2–§7.5, §9.2, §16.1–§16.3)
только `>=` (`want>=1 || reserve>=1` вместо `want>0 || reserve>0`); тег через «#» (`BUY:#gift:WANT`);
setup и эффекты — строки, а не объекты; recovery — список; расписание MISHAP/WINDFALL —
`arrives` + `repeat.weeks`; `triggers` — список (П3: ENTER обеих лавок); PRICE — абсолютная цена
в одной лавке (П3 — два эффекта); params.items убран (Пк1 — две записи: корм, мыло); добавлены
title, params.job, notBroken, EventState.outcome; Migration.migrate(s).

### Не определено здесь (не решать, не додумывать)
Семантика сопоставления факта с исходом, применение эффектов, варианты кассы, реализации
интерфейсов, reserve как вычисляемое свойство — срез 1.

## SCOPE
variant: main (общий код)
allow:   finny-pet/app/src/main/java/ru/finny/pet/domain/town   (новые файлы, ≤ 5)
         finny-pet/app/src/main/java/ru/finny/pet/domain/GameState.kt   (только §5 выше)
protect: базовый (docs/WORKFLOW.md) + finny-pet/app/src/main/ кроме allow + finny-pet/docs/

## ANTI-SCOPE
Реализации Town/Events/Prices/Migration; правка Economy, Content, Match3, data/; вычисляемые
свойства GameState; валидация ссылок на контент (это S0b); новые зависимости; UI.

## БЮДЖЕТ
≤ 500 вставок по git diff --shortstat -- app/src/main (включая KDoc и пустые строки);
новые файлы ≤ 5; зависимости 0

## ORACLE (test-author до кодера)
finny-pet/app/src/test/java/ru/finny/pet/domain/town/FactParserTest.kt — каждая строка таблиц §2
(разбор в значение и печать обратно), некаканонический ввод (пробелы на разрешённых местах) →
каноническая печать, три разных BuyTarget из BUY:FOOD / BUY:food_basic / BUY:#gift, перебор
Need.entries, Source, Until и прочих перечислений внутри словаря, каждая ветка ошибки §3,
эталонная запись §4 (поля и печать обратно), строковые сериализаторы через Json.
finny-pet/app/src/test/java/ru/finny/pet/domain/town/GameStateCompatTest.kt — фикстура
finny-pet/app/src/test/resources/town/state_1_3_0.json (реальный state.json 1.3.0 варианта game,
снят с эмулятора оркестратором: план 50/30/30 подтверждён, две покупки, один итог недели);
читать File("src/test/resources/town/state_1_3_0.json"), как TestContent. Разбор конфигурацией
StateStore (ignoreUnknownKeys, encodeDefaults); каждое новое поле = умолчанию; Purchase.shop/
source = null, PeriodSummary.shiftEarned = 0; старые поля целы — каждый ключ исходного
JsonObject равен тому же ключу после encode; состояние со всеми новыми полями не по умолчанию
переживает encode → decode.

## ACCEPTANCE (из finny-pet/, оркестратор)
  1. testClassicDebugUnitTest testGameDebugUnitTest -> exit 0
  2. tests = 117 + <оракул> в каждом варианте, failed 0, skipped 0 (подсчёт по XML)
  3. assembleClassicDebug assembleGameDebug assembleGameRelease -> exit 0
  4. lintClassicDebug lintGameDebug -> 0 ошибок, предупреждений без сетевых ≤ 5 / 6
  5. git diff --name-only BASE -- app/src/test/ '*.gradle.kts' gradle/ gradle.properties -> пусто
  6. git diff --name-only BASE -> только allow
  7. grep -rnE '^import (android|androidx)|System\.currentTimeMillis|LocalDate|Clock|Random\(' app/src/main/java/ru/finny/pet/domain/town/ -> пусто
  8. git diff --shortstat BASE -- app/src/main -> ≤ 500 вставок
  9. мутационные зонды оркестратора: ломается одна ветка парсера (неизвестный Source принимается;
     WEEK_END_NO:FUN принимается; печать без пробела после запятой; пробел внутри `week>= 2`
     принимается) -> оракул краснеет

## ПРИ БЛОКЕРЕ
STATUS: BLOCKED + один конкретный вопрос.
```
