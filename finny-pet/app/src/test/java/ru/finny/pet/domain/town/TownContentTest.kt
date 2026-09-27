package ru.finny.pet.domain.town

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import ru.finny.pet.data.ContentRepository
import ru.finny.pet.domain.Category
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.Need
import ru.finny.pet.domain.Purchase
import ru.finny.pet.domain.TestContent
import java.io.File

/**
 * Оракул классов разбора «Городка» (docs/tasks/TOWN-S0b.md, CONTRACT):
 * Content.town, Content.item, новые поля ShopItem, Need/Category.UNPLANNED, TownRules без умолчаний.
 */
class TownContentTest {

    private val content = TestContent.content
    private val town: TownContent get() = content.town ?: error("content.json: нет ключа town")

    private val raw: JsonObject by lazy {
        Json.parseToJsonElement(File("src/main/assets/content/content.json").readText()).jsonObject
    }
    private val rawTown: JsonObject by lazy { raw.getValue("town").jsonObject }

    private fun rawSize(key: String) = rawTown.getValue(key).jsonArray.size

    // ---------- Content.town ----------

    @Test
    fun `town разобран и списки содержат все записи файла`() {
        assertNotNull("content.town == null", content.town)
        val sizes = mapOf(
            "places" to town.places.size, "shops" to town.shops.size, "items" to town.items.size,
            "homeItems" to town.homeItems.size, "goals" to town.goals.size, "jobs" to town.jobs.size,
            "residents" to town.residents.size, "events" to town.events.size,
            "stickers" to town.stickers.size, "quiz" to town.quiz.size,
            "parentBonusReasons" to town.parentBonusReasons.size,
        )
        sizes.forEach { (key, size) ->
            assertEquals("town.$key разобран не полностью", rawSize(key), size)
            assertTrue("town.$key пуст", size > 0)
        }
    }

    @Test
    fun `town rules читает числа из JSON`() {
        val r = town.rules
        val j = rawTown.getValue("rules").jsonObject
        fun num(key: String) = j.getValue(key).jsonPrimitive.int
        fun list(key: String) = j.getValue(key).jsonArray.map { it.jsonPrimitive.int }
        assertEquals("daysPerWeek", num("daysPerWeek"), r.daysPerWeek)
        assertEquals("shiftsPerWeek", num("shiftsPerWeek"), r.shiftsPerWeek)
        assertEquals("shiftBonusMax", num("shiftBonusMax"), r.shiftBonusMax)
        assertEquals("shiftScorePerBonus", num("shiftScorePerBonus"), r.shiftScorePerBonus)
        assertEquals("changeCoins", list("changeCoins"), r.changeCoins)
        assertEquals("jobLevelShifts", list("jobLevelShifts"), r.jobLevelShifts)
        assertEquals("jobLevelBombs", list("jobLevelBombs"), r.jobLevelBombs)
        assertEquals("customGoalFromItemMin", num("customGoalFromItemMin"), r.customGoalFromItemMin)
        assertEquals("freeFunMood", num("freeFunMood"), r.freeFunMood)
        assertEquals("eventsPerDay", num("eventsPerDay"), r.eventsPerDay)
        assertEquals("riddleHint", num("riddleHint"), r.riddleHint)
    }

    @Test
    fun `контент без ключа town читается с town null`() {
        val stripped = JsonObject(raw - "town")
        assertNull("town не null без ключа town", ContentRepository.parse(stripped.toString()).town)
    }

    @Test
    fun `town rules без любого поля не читается`() {
        val fields = listOf(
            "daysPerWeek", "shiftsPerWeek", "shiftBonusMax", "shiftScorePerBonus", "changeCoins",
            "jobLevelShifts", "jobLevelBombs", "customGoalFromItemMin", "freeFunMood", "eventsPerDay",
            "riddleHint",
        )
        fields.forEach { field ->
            val rules = JsonObject(rawTown.getValue("rules").jsonObject - field)
            val text = JsonObject(raw + ("town" to JsonObject(rawTown + ("rules" to rules)))).toString()
            try {
                val parsed = ContentRepository.parse(text)
                fail("town.rules без «$field» разобран: ${parsed.town?.rules}")
            } catch (expected: IllegalArgumentException) {
                assertTrue("сообщение об ошибке без поля «$field»", expected.message.orEmpty().isNotEmpty())
            }
        }
    }

    // ---------- Content.item ----------

    @Test
    fun `item находит товар верхнего уровня`() {
        val i = content.item("fun_ball")
        assertEquals("fun_ball.title", "Мячик", i.title)
        assertEquals("fun_ball.price", content.items.first { it.id == "fun_ball" }.price, i.price)
    }

    @Test
    fun `item находит товар из town items`() {
        val expected = town.items.first { it.id == "food_porridge" }
        assertTrue("food_porridge попал в items верхнего уровня", content.items.none { it.id == "food_porridge" })
        assertEquals("item(food_porridge)", expected, content.item("food_porridge"))
        val doctor = content.item("care_doctor")
        assertEquals("care_doctor.category", Category.UNPLANNED, doctor.category)
        assertEquals("care_doctor.need", Need.UNPLANNED, doctor.need)
    }

    @Test
    fun `item на неизвестном id бросает NoSuchElementException`() {
        try {
            val i = content.item("no_such_item")
            fail("item(no_such_item) вернул $i")
        } catch (expected: NoSuchElementException) {
            // ожидаемо
        }
    }

    // ---------- Новые поля ShopItem ----------

    @Test
    fun `у товара без новых полей действуют умолчания`() {
        val i = content.item("care_brush")
        assertFalse("care_brush.keep", i.keep)
        assertNull("care_brush.slot", i.slot)
        assertEquals("care_brush.unlockPeriod", 1, i.unlockPeriod)
        assertEquals("care_brush.tags", emptyList<String>(), i.tags)
        assertFalse("care_brush.eventOnly", i.eventOnly)
    }

    @Test
    fun `новые поля товара читаются из JSON`() {
        val ball = content.item("fun_ball")
        assertTrue("fun_ball.keep", ball.keep)
        assertEquals("fun_ball.slot", "floor", ball.slot)

        val lamp = content.item("fun_starlamp")
        assertTrue("fun_starlamp.keep", lamp.keep)
        assertEquals("fun_starlamp.slot", "table", lamp.slot)
        assertEquals("fun_starlamp.unlockPeriod", 3, lamp.unlockPeriod)

        val card = content.item("gift_card")
        assertEquals("gift_card.tags", listOf("gift"), card.tags)
        assertFalse("gift_card.eventOnly", card.eventOnly)

        val lego = content.item("fun_lego")
        assertTrue("fun_lego.eventOnly", lego.eventOnly)
    }

    // ---------- Need и Category UNPLANNED ----------

    @Test
    fun `Need и Category содержат UNPLANNED`() {
        assertTrue("Need.entries без UNPLANNED", Need.UNPLANNED in Need.entries)
        assertTrue("Category.entries без UNPLANNED", Category.UNPLANNED in Category.entries)
    }

    private fun purchase(id: String, category: Category, need: Need, price: Int) =
        Purchase(itemId = id, title = id, category = category, need = need, price = price)

    @Test
    fun `покупка UNPLANNED не входит ни в обязательное ни в желаемое`() {
        val s = GameState(
            purchases = listOf(
                purchase("food_basic", Category.MANDATORY, Need.FOOD, 30),
                purchase("fun_ball", Category.OPTIONAL, Need.FUN, 25),
                purchase("care_doctor", Category.UNPLANNED, Need.UNPLANNED, 10),
            ),
        )
        assertEquals("factMandatory", 30, s.factMandatory)
        assertEquals("factOptional", 25, s.factOptional)

        val onlyUnplanned = GameState(purchases = listOf(purchase("care_doctor", Category.UNPLANNED, Need.UNPLANNED, 10)))
        assertEquals("factMandatory без плановых покупок", 0, onlyUnplanned.factMandatory)
        assertEquals("factOptional без плановых покупок", 0, onlyUnplanned.factOptional)

        val other = GameState(
            purchases = listOf(
                purchase("food_lunch", Category.MANDATORY, Need.FOOD, 45),
                purchase("care_doctor", Category.UNPLANNED, Need.UNPLANNED, 10),
            ),
        )
        assertEquals("factMandatory другого набора", 45, other.factMandatory)
        assertEquals("factOptional другого набора", 0, other.factOptional)
    }

    // ---------- Перечисления и умолчания классов town ----------

    @Test
    fun `Template содержит ровно значения контракта`() =
        assertEquals(listOf(Template.HOME, Template.SHOP, Template.JOB, Template.SCENE), Template.entries.toList())

    @Test
    fun `JobGame содержит ровно значения контракта`() =
        assertEquals(listOf(JobGame.MATCH3, JobGame.TAPS, JobGame.CHANGE, JobGame.TRAY), JobGame.entries.toList())

    @Test
    fun `OpensBy без полей — оба null`() {
        val o = OpensBy()
        assertNull("OpensBy().week", o.week)
        assertNull("OpensBy().goal", o.goal)
    }

    @Test
    fun `место без opensBy открыто сразу а место за мечту помнит цель`() {
        val home = town.places.first { it.id == "home" }
        assertEquals("home.template", Template.HOME, home.template)
        assertEquals("home.opensBy", OpensBy(), home.opensBy)
        assertNull("home.resident", home.resident)

        val park = town.places.first { it.id == "park" }
        assertEquals("park.template", Template.SCENE, park.template)
        assertEquals("park.opensBy.goal", "goal_scooter", park.opensBy.goal)
        assertNull("park.opensBy.week", park.opensBy.week)

        val workshop = town.places.first { it.id == "workshop" }
        assertEquals("workshop.opensBy.week", 3, workshop.opensBy.week)
        assertNull("workshop.opensBy.goal", workshop.opensBy.goal)
    }

    @Test
    fun `лавка читает предложения товаров`() {
        val workshop = town.shops.first { it.id == "shop_workshop" }
        assertEquals("shop_workshop.place", "workshop", workshop.place)
        assertEquals("shop_workshop.sells", listOf(ShopOffer("lamp_new", 25)), workshop.sells)
        val market = town.shops.first { it.id == "shop_market" }
        assertEquals("shop_market продаёт food_basic", 20, market.sells.first { it.item == "food_basic" }.price)
        val foma = town.shops.first { it.id == "shop_foma" }
        assertEquals("shop_foma продаёт food_basic", 30, foma.sells.first { it.item == "food_basic" }.price)
    }

    @Test
    fun `вещь дома по умолчанию стартовая и без цены починки`() {
        val flower = town.homeItems.first { it.id == "home_flower" }
        assertTrue("home_flower.starter", flower.starter)
        assertNull("home_flower.repairPrice", flower.repairPrice)
        assertNull("home_flower.replaceItem", flower.replaceItem)
        assertEquals("home_flower.spot", "spot_1", flower.spot)

        val lamp = town.homeItems.first { it.id == "home_lamp" }
        assertEquals("home_lamp.repairPrice", 8, lamp.repairPrice)
        assertEquals("home_lamp.replaceItem", "lamp_new", lamp.replaceItem)
        assertNull("home_lamp.spot", lamp.spot)
    }

    @Test
    fun `цель без unlocks не открывает ничего`() {
        val paints = town.goals.first { it.id == "goal_paints" }
        assertEquals("goal_paints.price", 80, paints.price)
        assertEquals("goal_paints.unlocks", Unlocks(houseColors = true, cards = true), paints.unlocks)
        assertNull("goal_paints.unlocks.place", paints.unlocks.place)
        assertNull("goal_paints.unlocks.item", paints.unlocks.item)

        val zoo = town.goals.first { it.id == "goal_zoo" }
        assertEquals("goal_zoo.unlocks", Unlocks(place = "zoo"), zoo.unlocks)
        val lego = town.goals.first { it.id == "goal_lego" }
        assertEquals("goal_lego.unlocks", Unlocks(item = "fun_lego"), lego.unlocks)
        assertFalse("goal_lego.unlocks.houseColors", lego.unlocks.houseColors)
        assertFalse("goal_lego.unlocks.cards", lego.unlocks.cards)
    }

    @Test
    fun `работа читает игру поле ходы и задания`() {
        val bakery = town.jobs.first { it.id == "job_bakery" }
        assertEquals("job_bakery.game", JobGame.MATCH3, bakery.game)
        assertEquals("job_bakery.board", Board(6, 6), bakery.board)
        assertEquals("job_bakery.moves", 15, bakery.moves)
        assertEquals("job_bakery.demoMoves", 5, bakery.demoMoves)
        assertEquals("job_bakery.opensBy", OpensBy(), bakery.opensBy)
        assertEquals("job_bakery.tasks", emptyList<String>(), bakery.tasks)

        val market = town.jobs.first { it.id == "job_market" }
        assertEquals("job_market.game", JobGame.TAPS, market.game)
        assertNull("job_market.board", market.board)
        assertNull("job_market.moves", market.moves)
        assertEquals("job_market.tasks", 3, market.tasks.size)

        val courier = town.jobs.first { it.id == "job_courier" }
        assertEquals("job_courier.opensBy.goal", "goal_scooter", courier.opensBy.goal)
    }

    /** Запись работы в сыром JSON. */
    private fun rawJob(id: String): JsonObject = rawTown.getValue("jobs").jsonArray
        .map { it.jsonObject }.first { it.getValue("id").jsonPrimitive.content == id }

    @Test
    fun `запись пекарни читает меню ступени и размеры демо из JSON`() {
        val bakery = town.jobs.first { it.id == "job_bakery" }
        val j = rawJob("job_bakery")

        val menu = j.getValue("menu").jsonArray.map { it.jsonObject }
        assertTrue("в сыром JSON пекарни пустое меню", menu.isNotEmpty())
        assertEquals("изделий в меню", menu.size, bakery.menu.size)
        menu.forEachIndexed { i, raw ->
            val p = bakery.menu[i]
            assertEquals("menu $i — id", raw.getValue("id").jsonPrimitive.content, p.id)
            assertEquals("menu $i — title", raw.getValue("title").jsonPrimitive.content, p.title)
            assertEquals("menu $i — emoji", raw.getValue("emoji").jsonPrimitive.content, p.emoji)
        }

        val steps = j.getValue("steps").jsonArray.map { it.jsonObject }
        assertTrue("в сыром JSON пекарни нет ступеней", steps.isNotEmpty())
        assertEquals("ступеней", steps.size, bakery.steps.size)
        steps.forEachIndexed { i, raw ->
            val st = bakery.steps[i]
            assertEquals("steps $i — fromShift", raw.getValue("fromShift").jsonPrimitive.int, st.fromShift)
            assertEquals("steps $i — kinds", raw.getValue("kinds").jsonPrimitive.int, st.kinds)
            assertEquals("steps $i — sizes", raw.getValue("sizes").jsonArray.map { s -> s.jsonPrimitive.int }, st.sizes)
            assertEquals("steps $i — intro", raw["intro"]?.jsonPrimitive?.content, st.intro)
        }

        assertEquals(
            "demoSizes",
            j.getValue("demoSizes").jsonArray.map { it.jsonPrimitive.int },
            bakery.demoSizes,
        )
    }

    @Test
    fun `у работы без подноса меню и ступени пусты а ступень без реплики молчит`() {
        val market = town.jobs.first { it.id == "job_market" }
        assertTrue("в сыром JSON у рынка есть меню", "menu" !in rawJob("job_market"))
        assertEquals("job_market.menu", emptyList<Pastry>(), market.menu)
        assertEquals("job_market.steps", emptyList<TrayStep>(), market.steps)
        assertNull("job_market.demoSizes", market.demoSizes)
        assertNull("ступень без реплики жителя", TrayStep(fromShift = 0, kinds = 3, sizes = listOf(1, 2)).intro)
    }

    @Test
    fun `житель читает вид цвет и реплики`() {
        val tosha = town.residents.first { it.id == "tosha" }
        assertEquals("tosha.name", "Тоша", tosha.name)
        assertEquals("tosha.look", Look(species = "puppy", color = "orange"), tosha.look)
        assertNull("tosha.look.accessory", tosha.look.accessory)
        assertEquals("tosha.arrivesWeek", 2, tosha.arrivesWeek)
        assertEquals("tosha.place", "toshas_house", tosha.place)

        val osya = town.residents.first { it.id == "osya" }
        assertNull("osya.place", osya.place)
        assertEquals("osya.look.accessory", "cap", osya.look.accessory)
        assertTrue("osya.lines пусты", osya.lines.isNotEmpty())
    }

    @Test
    fun `наклейка читает название и подсказку`() {
        val s = town.stickers.first { it.id == "st_place_home" }
        assertEquals("st_place_home", StickerDef("st_place_home", "Мой дом", "Есть с первого дня"), s)
    }

    @Test
    fun `события town разобраны словарём S0a`() {
        val e = town.events.first { it.id == "c1_robot_sale" }
        assertEquals("c1_robot_sale.kind", EventKind.OFFER, e.kind)
        assertEquals("c1_robot_sale.arrives", Arrives(week = 2, day = 2), e.arrives)
        assertEquals(
            "c1_robot_sale.setup",
            listOf(EventEffect.Offer("fun_robot", "shop_foma", 40, 25, Until.WEEK_END)),
            e.setup,
        )
        assertEquals("c1_robot_sale.requires", listOf(Condition.AfterPlan, Condition.NotOwned("fun_robot")), e.requires)
        val skip = e.outcomes.first()
        assertEquals("первый исход c1_robot_sale", Fact.Skip, skip.fact)
        assertEquals("первый исход c1_robot_sale", Verdict.GOOD, skip.verdict)
        assertEquals("c1_robot_sale.default", EventDefault(Until.WEEK_END, Fact.Skip), e.default)
    }
}
