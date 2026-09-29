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
    val eventsPerDay: Int)   // без умолчаний: числа только в JSON; срок возврата распродажи — repeat.afterWeeks записи
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
finny-pet/app/src/test/java/ru/finny/pet/domain/town/ContentValidationTest.kt — валидатор контента
(§16.1): читает TestContent.content (реальный content.json) и сырой JSON того же файла. Каждая
проверка — отдельный тест с сообщением, называющим запись и нарушение. «Все товары» = items ∪
town.items. «Тексты town» = все строковые значения поддерева town (включая town.quiz),
кроме id и строк словаря (fact/when/requires/setup/effects/recovery/triggers).

Проверки валидатора:
 1. Разбор: town != null; places, shops, items, homeItems, goals, jobs, residents, events,
    stickers, quiz, parentBonusReasons не пусты; в сыром JSON town.rules есть все 10 полей TownRules.
 2. Уникальность id внутри: все товары, places, shops, homeItems, goals, jobs, residents, events,
    stickers, town.quiz; товар не повторяется внутри одной лавки.
 3. Ссылки: shops.place → places; sells.item → все товары; places.resident → residents;
    places.sticker, events.sticker → stickers; opensBy.goal (места и работы) → town.goals;
    homeItems.replaceItem → все товары; goals.unlocks.place → places, unlocks.item → все товары;
    jobs.place → places, jobs.resident → residents; residents.place → places;
    residents.look.species → content.species, look.color → content.colors; events.place,
    followUp.place, Trigger.Enter, Fact.Attend, EventEffect.Goto → places; events.resident,
    Condition.ResidentArrived → residents; params.list, Fact.Buy(Item), Fact.BuyAt, Fact.MakeGoal,
    Condition.Owns/NotOwned, EventEffect.ItemGive → все товары; Fact.Buy(Tag) → тег хотя бы
    одного товара, который продаёт какая-то лавка; Fact.BuyAt → товар продают ≥ 2 лавки;
    Fact.Repair, Condition.NotBroken, ItemBreak, ItemFix → homeItems; EventEffect.Price/Offer →
    лавка есть и продаёт этот товар; EventEffect.Sticker → stickers; params.job → jobs.
 4. Минимум ТЗ 2.6: засчитываемых событий (theme != null) ≥ 6, их темы покрывают все Theme,
    у каждого есть исход GOOD и исход MISTAKE; в лавках town продаётся ≥ 8 разных товаров, среди
    них оба Category.MANDATORY и OPTIONAL; town.goals ≥ 3.
 5. Смешанные цены (§5.4): среди MANDATORY-товаров, которые продают и shop_market, и shop_foma,
    есть товар дешевле в shop_market и товар дешевле в shop_foma.
 6. №3 стоп-слова: ни один текст town не содержит (без учёта регистра) «сегодня», «осталось»,
    «до конца недели», «скорее», «пока не», «успей», «последн».
 7. №7 займы: ни один текст town не содержит «долг», «одолж», «взаймы», «занять».
 8. №10 стыд: ни один текст town не содержит «цена лени», «ленив», «зачем», «транжир», «жадин»,
    «зря», «впуст», «провал», «плохо», «неправильно», «ошибк»; строки исходов MISTAKE не содержат
    «знали», «надо было».
 9. Род: тексты town не содержат целых слов (Unicode-границы) «отложил», «планировал»,
    «потратил», «положил», «взял», «проголодался», «запачкался», «пришёл», «пришла», «захотел»,
    «захотела», «купил», «купила», «выбрал», «выбрала», «бегал», «подвернул», «достал», «видел»,
    «видела», «доволен», «довольна», «устроил», «чистенький» (список 1.3.0 плюс новые; ребёнок и
    питомец — без рода). Проверка идёт по текстам town И по description/reaction всех товаров,
    которые продают лавки town (там же тексты 1.3.0, которые game показывает у Фомы).
10. Длина: каждая outcomes[].line ≤ 80 символов; каждый intro ≤ 120 символов.
11. №11: ItemBreak только над homeItem с repairPrice != null и starter == true.
12. №12 и №4: COINS(WINDFALL) — только в setup событий kind WINDFALL; COINS(CHANGE_RETURN) — только
    в effects исходов события, в setup которого есть SHORT_CHANGE(m), и n ≤ m; у WINDFALL и
    MISHAP repeat.pool == false.
13. №13: у засчитываемого события ни один факт не встречается и в исходе GOOD, и в исходе MISTAKE.
14. №14: у каждого исхода MISTAKE непустой recovery; у события с SHORT_CHANGE(m) есть исход с
    COINS(m, CHANGE_RETURN), а каждый его исход MISTAKE содержит RETURN_LATER.
15. Пул: у каждого засчитываемого события, кроме MISHAP, repeat.pool == true.
16. №16 MISHAP: приходы = [arrives.week] + repeat.weeks — не больше 2, по возрастанию, разрыв
    ≥ 4; ни один приход не совпадает с arrives.week событий kind BREAK и событий с эффектом PRICE
    в setup; requires только из ResidentArrived/WeekAtLeast/DayIs/AfterPlan (не действия ребёнка); default == null; intro и
    строки исходов не содержат названий товаров с keep == true и названий мест с opensBy.goal.
17. Вещи: ITEM_BREAK-вещь с repairPrice имеет replaceItem; eventOnly-товар продаёт лавка, только если
    он replaceItem какой-то homeItem (новая лампа — на полке, пока идёт С2), остальные eventOnly
    (care_doctor, fun_lego) не продаёт ни одна лавка; бантик (fun_bow) не продаёт ни одна лавка town; keep == true у fun_ball, fun_book,
    fun_tent и у всех товаров town со slot != null.
18. «Ванна с пеной» (решение 16): care_vitamins — title «Ванна с пеной», emoji «🛁», price 25,
    hunger 0, clean 20, mood 5; description и reaction без «здоров», «бодр», «витамин».
19. Работы: у MATCH3 — board 6 × 6, moves и demoMoves заданы; у TAPS — ровно 3 tasks;
    baseByLevel.size == rules.jobLevelShifts.size у каждой работы.
20. town.quiz: 15 вопросов, correct в пределах options, explanation не пуст; q_shop_3 не
    предлагает займ; тексты проходят проверки 6–9.
21. parentBonusReasons — ровно 3 строки (решение 21, формулировки — в контенте).
22. Демо: у засчитываемого события непустой demo.wrongPath.

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
