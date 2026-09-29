package ru.finny.pet.domain.town

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.ok

/**
 * Демо-профиль проходит пять игровых недель подряд из дня 1, без привязки к реальному времени
 * (ТЗ 2.5.13, 2.6; TOWN-S1c ORACLE FiveDemoWeeksTownTest): период, рост и стадии,
 * первый запуск пустого профиля и события, которые приходят сами.
 */
class FiveDemoWeeksTownTest {

    private val town = S1aStand.town
    private val economy = S1aStand.economy

    // ---------- Первый запуск: пустой профиль, мечты ещё нет ----------

    @Test
    fun `первый запуск демо-профиля приносит «Список на неделю» до раскладки монет`() {
        val fresh = S1aStand.profile()
        assertTrue("профиль не демонстрационный", fresh.demo)
        assertEquals("на свежем профиле уже есть события", emptyList<EventState>(), fresh.events)
        assertNull("на свежем профиле уже есть мечта", fresh.goal)
        assertEquals("на свежем профиле пусто не в городке", Card("В городке спокойно — загляни на доску"), town.card(fresh))

        val first = town.tick(fresh)
        assertEquals("tick сам что-то сказал ребёнку", "", first.line)
        assertEquals("на первом запуске пришло не «Список на неделю»", listOf("p1_list"), first.arrived)
        assertEquals("доска событий", listOf("p1_list"), town.activeEvents(first.state).map { it.id })
        assertEquals("карточка «В городке»", Card("Список на неделю", "p1_list", "home"), town.card(first.state))
        assertS1aInvariants("после первого tick", first.state)

        val again = town.tick(first.state)
        assertEquals("второй tick привёл событие ещё раз", emptyList<String>(), again.arrived)
        assertEquals("событие записано дважды", 1, again.state.events.size)

        // заказы идут только после раскладки монет — демо-послаблений у них нет
        assertEquals("до раскладки в городке есть заказы", emptyList<String>(), town.orders(fresh).map { it.id })
        val planned = town.confirmPlan(economy.setPlan(first.state, 40, 20, 30).ok()).s1aState()
        assertEquals(
            "после раскладки заказ Бори не появился",
            listOf("job_bakery_help", "job_market_help"),
            town.orders(planned).map { it.id },
        )
    }

    // ---------- Пять недель подряд ----------

    @Test
    fun `демо-профиль проживает пять недель подряд из дня 1`() {
        var s = town.tick(S1aStand.profile()).state
        val texts = mutableListOf<String>()
        val periods = mutableListOf<Int>()
        val growth = mutableListOf<Int>()
        val stages = mutableListOf<Int>()
        val stageTitles = mutableListOf<String>()
        val scores = mutableListOf<Int>()

        repeat(5) { i ->
            val week = i + 1
            assertEquals("неделя перед раскладкой", week, s.period)
            assertEquals("день перед раскладкой", 1, s.day)

            s = economy.setPlan(s, 40, 20, 30).ok()
            val plan = town.confirmPlan(s)
            s = plan.s1aState()
            texts += plan.s1aTexts()

            val food = town.buyAt(s, "food_basic", "shop_market", Source.NEED)
            s = food.s1aState()
            texts += food.s1aTexts()
            val care = town.buyAt(s, "care_soap", "shop_foma", Source.NEED)
            s = care.s1aState()
            texts += care.s1aTexts()

            if (week == 1) {
                // П1 пришёл на пустой профиль и решается поступком — раскладкой монет
                assertEquals(
                    "строка недели 1",
                    "Посмотрели цены заранее — на корм и мыло хватит.",
                    plan.s1aOutcome().line,
                )
                assertEquals(
                    "«Почему?» недели 1",
                    listOf(
                        "План готов! В копилку +30, запас на всякий случай: 10",
                        "Сначала откладываем, потом тратим — так мечта ближе",
                        "Запас — на нужное и на всякий случай, не на «хочу»",
                    ),
                    plan.s1aOutcome().why,
                )
                assertEquals(
                    "итог события на раскладке",
                    listOf(EventResult("p1_list", Verdict.GOOD, "Посмотрели цены заранее — на корм и мыло хватит.", "st_list")),
                    plan.s1aOutcome().eventResults,
                )
                assertEquals(
                    "строка покупки корма",
                    "Одинаковый корм — выбрали, где дешевле: сэкономили 10.",
                    food.s1aOutcome().line,
                )
                assertEquals(
                    "«Почему?» покупки корма",
                    listOf("Финни с удовольствием хрустит кормом!", "Из «Нужного»: 40 → 20"),
                    food.s1aOutcome().why,
                )
            }
            if (week == 2) {
                assertEquals(
                    "строка недели 2 — событий на раскладке нет",
                    "План готов! В копилку +30, запас на всякий случай: 50",
                    plan.s1aOutcome().line,
                )
            }

            assertEquals("день перед итогом недели", 1, s.day)
            val end = town.endWeek(s) // демо-кнопка «Сразу к итогу недели»
            s = end.s1aState()
            texts += end.s1aTexts()
            assertS1aInvariants("неделя $week, итог", s)

            val sum = s.history.last()
            assertEquals("неделя итога", week, sum.period)
            assertTrue("неделя $week: еда и уход не покрыты", sum.mandatoryCovered)
            assertTrue("неделя $week: траты вышли из плана", sum.planKept)
            assertTrue("неделя $week: копилка не выросла", sum.saved)
            scores += sum.score
            growth += s.pet!!.growth
            stages += sum.stageAfter
            stageTitles += economy.stageTitle(s.pet!!.growth)
            periods += s.period

            val morning = town.wake(s)
            s = morning.s1aState()
            texts += morning.s1aTexts()
            if (week == 1) {
                assertEquals("строка нового конверта", "Новый конверт: карманные 100", morning.s1aOutcome().line)
            }
        }

        assertEquals("периоды подряд", listOf(2, 3, 4, 5, 6), periods)
        assertEquals("рост за каждую неделю", listOf(3, 3, 3, 3, 3), scores)
        assertEquals("рост питомца", listOf(3, 6, 9, 12, 15), growth)
        assertEquals("стадии питомца", listOf(0, 1, 2, 2, 2), stages)
        assertEquals(
            "названия стадий",
            listOf("Малыш", "Подросток", "Взрослый", "Взрослый", "Взрослый"),
            stageTitles,
        )
        assertEquals("итогов недели в истории", 5, s.history.size)
        assertEquals("день после пяти недель", 1, s.day)
        assertEquals("монет в кошельке", 300, s.balance)
        assertEquals("копилка после пяти недель", 150, s.savings)

        // события пришли сами, решены поступком и не висят на следующей неделе
        assertEquals(
            "события за пять недель",
            listOf("p1_list", "pk1_cheaper_food", "pk1_cheaper_soap"),
            s.events.map { it.id },
        )
        assertTrue("событие осталось незакрытым", s.events.all { it.status == EventStatus.DONE })
        assertEquals("вердикты событий", listOf(Verdict.GOOD, Verdict.GOOD, Verdict.GOOD), s.events.map { it.verdict })
        assertEquals("наклейки за пять недель", listOf("st_list", "st_cheaper"), s.stickers)

        S1aChildText.check("пять демо-недель", texts)
    }

    @Test
    fun `в обычной игре пять недель подряд из дня 1 пройти нельзя`() {
        val plain: GameState = S1aStand.planned(40, 20, 30, demo = false)
        assertEquals("день", 1, plain.day)
        assertEquals("итог недели из дня 1 без демо", "Неделя ещё идёт", town.endWeek(plain).s1aRefusal())
    }
}
