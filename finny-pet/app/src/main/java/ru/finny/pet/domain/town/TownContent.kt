package ru.finny.pet.domain.town

import kotlinx.serialization.Serializable
import ru.finny.pet.domain.QuizQuestion
import ru.finny.pet.domain.ShopItem

/** Весь контент «Городка» (GAME_CONCEPT §16.1), лежит под ключом town в content.json. */
@Serializable
data class TownContent(
    val rules: TownRules,
    val places: List<Place>, val shops: List<Shop>, val items: List<ShopItem>,
    val homeItems: List<HomeItem>, val goals: List<TownGoal>, val jobs: List<Job>,
    val residents: List<Resident>, val events: List<EventDef>, val stickers: List<StickerDef>,
    val quiz: List<QuizQuestion>, val parentBonusReasons: List<String>,
    val spots: List<Spot>,
    /** id событий, которые в этой сборке не приходят и не начинаются (TOWN-S1c §0). */
    val eventsOff: List<String> = emptyList(),
    /** Реплики питомца по тапу (TOWN-S1d §A). */
    val chatter: TownChatter = TownChatter(),
)

/** Реплики питомца: нужда недели, грусть, спокойствие (TOWN-S1d §A). */
@Serializable
data class TownChatter(
    val needFood: List<String> = emptyList(), val needCare: List<String> = emptyList(),
    val sad: List<String> = emptyList(), val calm: List<String> = emptyList(),
)

/** Место для вещи в доме и его тип: floor, wall, table (§5.3). */
@Serializable
data class Spot(val id: String, val slot: String)

/** Числа «Городка» (§5.9): без умолчаний, чтобы число жило только в content.json. */
@Serializable
data class TownRules(
    val daysPerWeek: Int, val shiftsPerWeek: Int, val shiftBonusMax: Int,
    val shiftScorePerBonus: Int, val changeCoins: List<Int>, val jobLevelShifts: List<Int>,
    val jobLevelBombs: List<Int>, val customGoalFromItemMin: Int, val freeFunMood: Int,
    val eventsPerDay: Int,
    /** Сколько изделий подсказка Бори кладёт за раз на поднос (TOWN-J1-0 § 1, § 2). */
    val riddleHint: Int,
)

/** Шаблон экрана места (§17.2). */
@Serializable
enum class Template { HOME, SHOP, JOB, SCENE }

/** Когда открывается место или работа: неделя, мечта или сразу, если оба null; в демо неделя не проверяется (§4.1, §7.4). */
@Serializable
data class OpensBy(val week: Int? = null, val goal: String? = null)

/** Место на улице городка или дом (§4.1, §16.1). */
@Serializable
data class Place(
    val id: String, val title: String, val template: Template,
    val opensBy: OpensBy = OpensBy(), val resident: String? = null, val sticker: String? = null,
)

/** Строка ассортимента лавки: товар и цена в этой лавке (§5.4). */
@Serializable
data class ShopOffer(val item: String, val price: Int)

/** Лавка на месте, что она продаёт (§5.4). */
@Serializable
data class Shop(val id: String, val place: String, val title: String, val at: String, val sells: List<ShopOffer>)

/** Стартовая вещь дома: декор на своём месте или вещь, которая ломается и чинится (§16.1, С2). */
@Serializable
data class HomeItem(
    val id: String, val title: String, val emoji: String, val starter: Boolean = true,
    val repairPrice: Int? = null, val replaceItem: String? = null, val spot: String? = null,
)

/** Что открывает полученная мечта (§5.6). */
@Serializable
data class Unlocks(
    val place: String? = null, val houseColors: Boolean = false,
    val cards: Boolean = false, val item: String? = null,
)

/** Мечта: цена и что она открывает (§5.6). */
@Serializable
data class TownGoal(
    val id: String, val title: String, val emoji: String, val price: Int,
    val unlocks: Unlocks = Unlocks(),
)

/** Вид работы: поле Match3, кнопки-поручения, касса со сдачей или поднос по заказу (§5.2, TOWN-J1-0 § 1). */
@Serializable
enum class JobGame { MATCH3, TAPS, CHANGE, TRAY }

/** Размер поля мини-игры (§16.1). */
@Serializable
data class Board(val w: Int, val h: Int)

/** Изделие пекарни (TOWN-J1-0 § 1). */
@Serializable
data class Pastry(val id: String, val title: String, val emoji: String)

/**
 * Ступень меню подноса: действует после стольких оплачиваемых смен на этой работе; kinds — сколько
 * первых изделий меню на витрине; sizes — размер заказа каждого покупателя по порядку; intro —
 * реплика жителя работы на первой смене ступени (TOWN-J1-0 § 1).
 */
@Serializable
data class TrayStep(val fromShift: Int, val kinds: Int, val sizes: List<Int>, val intro: String? = null)

/** Подработка у жителя: вид работы, база по уровню, поле и ходы, когда открывается (§5.2, §16.1). */
@Serializable
data class Job(
    val id: String, val place: String, val resident: String, val title: String,
    val game: JobGame, val baseByLevel: List<Int>, val board: Board? = null, val moves: Int? = null,
    val demoMoves: Int? = null, val opensBy: OpensBy = OpensBy(), val tasks: List<String> = emptyList(),
    /** Витрина, ступени и размеры демо-заказов подноса (TOWN-J1-0 § 1). */
    val menu: List<Pastry> = emptyList(), val steps: List<TrayStep> = emptyList(),
    val demoSizes: List<Int>? = null,
)

/** Внешность персонажа: вид, цвет, необязательный аксессуар (§16.1). */
@Serializable
data class Look(val species: String, val color: String, val accessory: String? = null)

/** Житель городка: облик, неделя приезда, место и реплики (§4.2). */
@Serializable
data class Resident(
    val id: String, val name: String, val look: Look, val arrivesWeek: Int? = null,
    val place: String? = null, val lines: List<String> = emptyList(),
)

/** Наклейка «Дневника»: название и подсказка, откуда она берётся (§9.1, §16.1). */
@Serializable
data class StickerDef(val id: String, val title: String, val hint: String)
