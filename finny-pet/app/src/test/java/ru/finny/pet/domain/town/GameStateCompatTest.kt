package ru.finny.pet.domain.town

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.pet.domain.BudgetPlan
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.LedgerEntry
import java.io.File

/**
 * Оракул совместимости сохранения (TOWN-S0a §5): реальный state.json версии 1.3.0 варианта game
 * читается новой схемой, новые поля берут умолчания, старые ключи переживают чтение и запись.
 */
class GameStateCompatTest {

    /** Ровно конфигурация StateStore. */
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val fixtureText = File("src/test/resources/town/state_1_3_0.json").readText()
    private val fixture: JsonObject get() = Json.parseToJsonElement(fixtureText).jsonObject
    private val state: GameState get() = json.decodeFromString(GameState.serializer(), fixtureText)

    @Test
    fun `старое сохранение читается новой схемой`() {
        val s = state
        assertEquals("неделя", 2, s.period)
        assertEquals("баланс", 115, s.balance)
        assertEquals("копилка", 10, s.savings)
        assertEquals("питомец", "Финни", s.pet?.name)
        assertEquals("план подтверждён", true, s.plan.confirmed)
        assertEquals("план — обязательное", 50, s.plan.mandatory)
        assertEquals("две покупки", 2, s.purchases.size)
        assertEquals("один итог недели", 1, s.history.size)
        assertEquals("цель", "goal_scooter", s.goal?.id)
    }

    @Test
    fun `новые поля состояния равны умолчаниям`() {
        val s = state
        assertEquals("stateVersion", 0, s.stateVersion)
        assertEquals("day", 1, s.day)
        assertEquals("asleep", false, s.asleep)
        assertEquals("seed", 0L, s.seed)
        assertEquals("jarNeed", 0, s.jarNeed)
        assertEquals("jarWant", 0, s.jarWant)
        assertEquals("envelope", emptyList<LedgerEntry>(), s.envelope)
        assertEquals("owned", emptyList<String>(), s.owned)
        assertEquals("placed", emptyMap<String, String>(), s.placed)
        assertEquals("broken", emptyList<String>(), s.broken)
        assertNull("houseColor", s.houseColor)
        assertEquals("shiftsThisPeriod", 0, s.shiftsThisPeriod)
        assertEquals("jobShifts", emptyMap<String, Int>(), s.jobShifts)
        assertEquals("records", emptyMap<String, Int>(), s.records)
        assertEquals("events", emptyList<EventState>(), s.events)
        assertEquals("stickers", emptyList<String>(), s.stickers)
        assertEquals("visited", emptyList<String>(), s.visited)
        assertEquals("seenPrices", emptyMap<String, SeenPrice>(), s.seenPrices)
        assertEquals("notes", emptyList<String>(), s.notes)
        assertNull("planDraft", s.planDraft)
        assertEquals("freeFunDay", 0, s.freeFunDay)
        assertEquals("diary", emptyList<DiaryLine>(), s.diary)
    }

    @Test
    fun `у старых покупок нет лавки и источника`() {
        val purchases = state.purchases
        assertEquals("покупок в фикстуре", 2, purchases.size)
        for (p in purchases) {
            assertNull("лавка покупки ${p.itemId}", p.shop)
            assertNull("источник покупки ${p.itemId}", p.source)
        }
    }

    @Test
    fun `в старых итогах недели смена не приносила монет`() {
        val history = state.history
        assertEquals("итогов в фикстуре", 1, history.size)
        for (h in history) {
            assertEquals("заработок смен за неделю ${h.period}", 0, h.shiftEarned)
        }
    }

    @Test
    fun `старые ключи переживают чтение и запись`() {
        val written = Json.parseToJsonElement(json.encodeToString(GameState.serializer(), state)).jsonObject
        val old = fixture
        assertTrue("фикстура не пуста", old.keys.isNotEmpty())
        for ((key, value) in old) {
            assertKept(key, value, written[key])
        }
    }

    @Test
    fun `новые ключи попадают в запись`() {
        val written = Json.parseToJsonElement(json.encodeToString(GameState.serializer(), state)).jsonObject
        val newKeys = listOf(
            "stateVersion", "day", "asleep", "seed", "jarNeed", "jarWant", "envelope", "owned",
            "placed", "broken", "houseColor", "shiftsThisPeriod", "jobShifts", "records", "events",
            "stickers", "visited", "seenPrices", "notes", "planDraft", "freeFunDay", "diary",
        )
        for (key in newKeys) {
            assertTrue("в записанном состоянии нет ключа $key, есть ${written.keys}", written.containsKey(key))
        }
        val purchase = written["purchases"]!!.let { (it as JsonArray)[0] }.jsonObject
        assertTrue("у покупки нет ключа shop, есть ${purchase.keys}", purchase.containsKey("shop"))
        assertTrue("у покупки нет ключа source, есть ${purchase.keys}", purchase.containsKey("source"))
        val summary = written["history"]!!.let { (it as JsonArray)[0] }.jsonObject
        assertTrue("у итога недели нет ключа shiftEarned, есть ${summary.keys}", summary.containsKey("shiftEarned"))
    }

    @Test
    fun `состояние со всеми новыми полями переживает запись и чтение`() {
        val filled = filled()
        val back = json.decodeFromString(GameState.serializer(), json.encodeToString(GameState.serializer(), filled))
        assertEquals("состояние после записи и чтения", filled, back)
    }

    @Test
    fun `заполненное состояние отличается от старого по каждому новому полю`() {
        val filled = filled()
        val old = state
        assertTrue("stateVersion", filled.stateVersion != old.stateVersion)
        assertTrue("day", filled.day != old.day)
        assertTrue("asleep", filled.asleep != old.asleep)
        assertTrue("seed", filled.seed != old.seed)
        assertTrue("jarNeed", filled.jarNeed != old.jarNeed)
        assertTrue("jarWant", filled.jarWant != old.jarWant)
        assertTrue("envelope", filled.envelope != old.envelope)
        assertTrue("owned", filled.owned != old.owned)
        assertTrue("placed", filled.placed != old.placed)
        assertTrue("broken", filled.broken != old.broken)
        assertTrue("houseColor", filled.houseColor != old.houseColor)
        assertTrue("shiftsThisPeriod", filled.shiftsThisPeriod != old.shiftsThisPeriod)
        assertTrue("jobShifts", filled.jobShifts != old.jobShifts)
        assertTrue("records", filled.records != old.records)
        assertTrue("events", filled.events != old.events)
        assertTrue("stickers", filled.stickers != old.stickers)
        assertTrue("visited", filled.visited != old.visited)
        assertTrue("seenPrices", filled.seenPrices != old.seenPrices)
        assertTrue("notes", filled.notes != old.notes)
        assertTrue("planDraft", filled.planDraft != old.planDraft)
        assertTrue("freeFunDay", filled.freeFunDay != old.freeFunDay)
        assertTrue("diary", filled.diary != old.diary)
        assertTrue("покупки с лавкой", filled.purchases != old.purchases)
        assertTrue("итоги со сменами", filled.history != old.history)
    }

    @Test
    fun `состояние события хранит исход по умолчанию отдельно от индекса`() {
        val byIndex = EventState(id = "c1_robot_sale", status = EventStatus.DONE, verdict = Verdict.OK, outcome = 2, period = 2, day = 3)
        val byDefault = byIndex.copy(outcome = -1)
        val active = EventState(id = "c1_robot_sale", status = EventStatus.ACTIVE, period = 2, day = 3)
        assertTrue("индекс исхода и default — разные состояния", byIndex != byDefault)
        assertNull("у активного события вердикта нет", active.verdict)
        assertNull("у активного события исхода нет", active.outcome)
        val text = json.encodeToString(EventState.serializer(), byDefault)
        assertEquals("состояние события после записи и чтения", byDefault, json.decodeFromString(EventState.serializer(), text))
    }

    private fun filled(): GameState {
        val base = state
        return base.copy(
            purchases = base.purchases.map { it.copy(shop = "shop_foma", source = Source.NEED) },
            history = base.history.map { it.copy(shiftEarned = 12) },
            stateVersion = 2,
            day = 4,
            asleep = true,
            seed = 987654321L,
            jarNeed = 40,
            jarWant = 25,
            envelope = listOf(LedgerEntry("Сдача", 5)),
            owned = listOf("fun_robot"),
            placed = mapOf("fun_robot" to "shelf"),
            broken = listOf("home_lamp"),
            houseColor = "green",
            shiftsThisPeriod = 2,
            jobShifts = mapOf("job_bakery" to 3),
            records = mapOf("job_bakery" to 120),
            events = listOf(
                EventState(
                    id = "c1_robot_sale",
                    status = EventStatus.DONE,
                    verdict = Verdict.MISTAKE,
                    outcome = 3,
                    period = 2,
                    day = 2,
                ),
            ),
            stickers = listOf("st_robot_sale"),
            visited = listOf("foma"),
            seenPrices = mapOf("fun_robot" to SeenPrice(shop = "shop_foma", price = 25, period = 2)),
            notes = listOf("Сдачу надо считать"),
            planDraft = BudgetPlan(mandatory = 40, optional = 30, savings = 30, confirmed = false),
            freeFunDay = 5,
            diary = listOf(DiaryLine(period = 2, day = 3, text = "Купил корм в лавке Фомы")),
        )
    }

    /** Каждый ключ и элемент из старого сохранения обязан остаться на месте с тем же значением. */
    private fun assertKept(path: String, old: JsonElement, now: JsonElement?) {
        assertNotNull("после записи пропало поле $path", now)
        when (old) {
            is JsonObject -> {
                assertTrue("поле $path перестало быть объектом", now is JsonObject)
                val cur = now as JsonObject
                for ((key, value) in old) assertKept("$path -> $key", value, cur[key])
            }
            is JsonArray -> {
                assertTrue("поле $path перестало быть списком", now is JsonArray)
                val cur = now as JsonArray
                assertEquals("длина списка $path", old.size, cur.size)
                old.forEachIndexed { i, value -> assertKept("$path элемент $i", value, cur[i]) }
            }
            else -> assertEquals("значение поля $path", old, now)
        }
    }
}
