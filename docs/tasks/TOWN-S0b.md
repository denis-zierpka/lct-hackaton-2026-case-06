```
TASK: TOWN-S0b — «Городок» данными: town в content.json + ContentValidationTest
EPIC: TOWN-S0 (docs/tasks/TOWN-S0.md)
BASE: <sha коммита контента>
BRANCH: feat/town

## КОНТЕКСТ
Все сущности «Городка» (§16.1) записываются в content.json под ключом town ДО кода движка:
несовместимость словаря всплывает сейчас, а не в срезе 2 (§17.3). Контент (числа и тексты)
пишет оркестратор — это отдельный коммит до кодера; кодер пишет только классы разбора;
валидатор контента — JVM-тест test-author (§16.1).

## ТРЕБОВАНИЕ ТЗ
2.5.14 — новое задание, товар, цель записью данных; 2.6 — ≥ 6 заданий по 3 темам с верным
и ошибочным вариантом, ≥ 8 покупок двух типов, ≥ 3 целей; 3.5 — тексты не стыдят и не пугают;
2.5.6 — влияние товара видно до покупки.

## CONTRACT

### Content.kt (добавлением)
- enum Need += UNPLANNED; enum Category += UNPLANNED (в factMandatory/factOptional не входят).
- ShopItem += keep: Boolean = false, slot: String? = null, unlockPeriod: Int = 1,
  tags: List<String> = emptyList(), eventOnly: Boolean = false.
- Content += val town: TownContent? = null.
- Content.item(id): ищет в items, затем в town.items; нет нигде — NoSuchElementException, как сейчас.

### domain/town/TownContent.kt (новый файл) — все @Serializable, имена JSON = имена свойств
data class TownContent(
    val rules: TownRules,
    val places: List<Place>, val shops: List<Shop>, val items: List<ShopItem>,
    val homeItems: List<HomeItem>, val goals: List<TownGoal>, val jobs: List<Job>,
    val residents: List<Resident>, val events: List<EventDef>, val stickers: List<StickerDef>,
    val quiz: List<QuizQuestion>, val parentBonusReasons: List<String>,
)
data class TownRules(val daysPerWeek: Int, val shiftsPerWeek: Int, val shiftBonusMax: Int,
    val shiftScorePerBonus: Int, val changeCoins: List<Int>, val jobLevelShifts: List<Int>,
    val jobLevelBombs: List<Int>, val customGoalFromItemMin: Int, val freeFunMood: Int,
    val eventsPerDay: Int, val offerReturnWeeks: Int)          // без умолчаний: числа только в JSON
enum class Template { HOME, SHOP, JOB, SCENE }
data class OpensBy(val week: Int? = null, val goal: String? = null)   // оба null — сразу
data class Place(val id: String, val title: String, val template: Template,
    val opensBy: OpensBy = OpensBy(), val resident: String? = null, val sticker: String? = null)
data class ShopOffer(val item: String, val price: Int)
data class Shop(val id: String, val place: String, val title: String, val sells: List<ShopOffer>)
data class HomeItem(val id: String, val title: String, val emoji: String, val starter: Boolean = true,
    val repairPrice: Int? = null, val replaceItem: String? = null, val spot: String? = null)
data class Unlocks(val place: String? = null, val houseColors: Boolean = false,
    val cards: Boolean = false, val item: String? = null)
data class TownGoal(val id: String, val title: String, val emoji: String, val price: Int,
    val unlocks: Unlocks = Unlocks())
enum class JobGame { MATCH3, TAPS, CHANGE }
data class Board(val w: Int, val h: Int)
data class Job(val id: String, val place: String, val resident: String, val title: String,
    val game: JobGame, val baseByLevel: List<Int>, val board: Board? = null, val moves: Int? = null,
    val demoMoves: Int? = null, val opensBy: OpensBy = OpensBy(), val tasks: List<String> = emptyList())
data class Look(val species: String, val color: String, val accessory: String? = null)
data class Resident(val id: String, val name: String, val look: Look, val arrivesWeek: Int? = null,
    val place: String? = null, val lines: List<String> = emptyList())
data class StickerDef(val id: String, val title: String, val hint: String)

### GameViewModel.kt (game)
Одна правка: исчерпывающий when (item.need) на строке 160 получает ветку Need.UNPLANNED
(PetAct.PLAY — нет еды и мытья). Больше ничего.

### Контент
Пишет оркестратор отдельным коммитом ДО кодера: ключ town целиком, «Витаминки» → «Ванна с
пеной» в общем items[] (решение 16: id прежний, 🛁, описание и реакция без здоровья, сытость
+10 убрана), keep у существующих мячика, книжки и домика. Кодер content.json не меняет.

## SCOPE
variant: main
allow:   finny-pet/app/src/main/java/ru/finny/pet/domain/Content.kt
         finny-pet/app/src/main/java/ru/finny/pet/domain/town/TownContent.kt   (новый)
         finny-pet/app/src/game/java/ru/finny/pet/game/GameViewModel.kt   (одна ветка when)
protect: базовый + content.json + остальное finny-pet/app/src/

## ANTI-SCOPE
Движок, загрузка town в UI, правка Economy, classic, новые зависимости, ассеты.

## БЮДЖЕТ
дифф кода: ≤ 150 строк (JSON не считается); новые файлы кода: 1; зависимости: 0

## ORACLE (test-author до кодера)
finny-pet/app/src/test/java/ru/finny/pet/domain/town/ContentValidationTest.kt — см. раздел
«Проверки валидатора» (дописывается при старте).

## ACCEPTANCE (из finny-pet/, оркестратор)
  1. тесты обоих вариантов -> exit 0; tests = было + оракул, skipped 0
  2. assembleClassicDebug assembleGameDebug assembleGameRelease -> exit 0; lint ≤ 5 / 6
  3. git diff --name-only BASE -> только allow
  4. мутационные зонды контента (оркестратор): каждая проверка валидатора краснеет на своём
     сломанном контенте и зеленеет на исходном
  5. доки: ECONOMY.md:75, CONTENT_MAP.md:106, REQUIREMENTS_MATRIX.md:80 — «Витаминки» →
     «Ванна с пеной» (правило «доки не врут»); остальное ECONOMY/CONTENT_MAP не трогать до движка

## ПРИ БЛОКЕРЕ
STATUS: BLOCKED + один вопрос.
```
