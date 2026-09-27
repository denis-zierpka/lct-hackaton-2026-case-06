package ru.finny.pet.domain.town

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.LedgerEntry
import ru.finny.pet.domain.TaskResult
import ru.finny.pet.domain.ok

/**
 * Стенд смен, «Загадки Бори» и бонуса взрослого (docs/tasks/TOWN-S1b.md).
 * Состояния строятся настоящим путём игрока: newGame → createPet → setPlan → Town.confirmPlan → …
 * Руками (copy) задаются только граничные счётчики — jobShifts, shiftsThisPeriod, bombs, riddles,
 * quizResults; каждый раз с комментарием, почему настоящий путь сюда не годится.
 */
internal object S1bStand {
    val content = S1aStand.content
    val town = S1aStand.town
    val economy = S1aStand.economy
    val quizIds: List<String> get() = content.town!!.quiz.map { it.id }

    fun explanation(id: String): String = content.town!!.quiz.first { it.id == id }.explanation

    /** Ответ на загадку в сохранении (граничная очередь вопросов). */
    fun riddle(id: String, correct: Boolean, period: Int = 1) = TaskResult(id, correct, 0, period)

    /** Неделя обычной игры целиком: три дня, итог недели и та же раскладка на новой неделе. */
    fun nextWeek(s: GameState): GameState {
        var st = s
        repeat(S1aStand.days - 1) {
            st = town.sleep(st).s1aState()
            st = town.wake(st).s1aState()
        }
        st = town.sleep(st).s1aState() // сон в последний день недели = итог недели
        st = town.wake(st).s1aState()
        return town.confirmPlan(st).s1aState()
    }
}

class ShiftTest {

    private val town = S1bStand.town
    private val economy = S1bStand.economy
    private val payLater = "придёт с новым конвертом"
    private val wage = "Карманные приходят каждую неделю, зарплата — когда поработаешь"
    private val closed = "Эта работа пока закрыта"
    private val night = "Сейчас ночь — сначала проснёмся"
    private val badShift = "Так закончить смену нельзя"

    /** Одна строка после лимита смен для всех работ (TOWN-J1-0 § 4, решение № 47 б). */
    private val limitLine = "Смены на неделе закончились — новые с новым конвертом"

    /** Граничный счётчик мастерства: 15 настоящих смен — это пять недель игры. */
    private fun withShifts(s: GameState, jobId: String, n: Int): GameState =
        s.copy(jobShifts = s.jobShifts + (jobId to n))

    // ---------- §1. shiftQuote ----------

    @Test
    fun `открытая работа платит базу и обещает надбавку ещё до раскладки монет`() {
        val s = S1aStand.profile()
        assertFalse("монеты уже разложены — ветка «до плана» не проверяется", s.plan.confirmed)

        val bakery = town.shiftQuote(s, "job_bakery")
        assertEquals("работа", "job_bakery", bakery.jobId)
        assertTrue("пекарня закрыта", bakery.open)
        assertTrue("смена до плана не оплачивается", bakery.paid)
        assertTrue("играть нельзя", bakery.canPlay)
        assertEquals("уровень", 0, bakery.level)
        assertEquals("база", 6, bakery.base)
        assertEquals("бомбы уровня", 0, bakery.levelBombs)
        assertEquals("смен на неделе", 3, bakery.shiftsLeft)
        assertEquals("строка поля", "6–10 за смену", bakery.line)
        assertEquals("верх диапазона поля", 10, bakery.top)

        val market = town.shiftQuote(s, "job_market")
        assertEquals("строка кнопочной работы", "6 за три поручения", market.line)
        assertEquals("у кнопочной работы бомб нет", 0, market.levelBombs)
        assertEquals("у кнопочной работы верх диапазона равен базе", 6, market.top)

        // касса открыта по неделе 4, но демо-режим недели не проверяет
        val kassa = town.shiftQuote(s, "job_kassa")
        assertTrue("в демо касса закрыта", kassa.open)
        assertEquals("строка кассы", "6–10 за смену", kassa.line)
        assertEquals("у кассы бомб нет", 0, kassa.levelBombs)
        assertEquals("верх диапазона кассы", 10, kassa.top)

        // неизвестной работе платить нечем
        assertEquals("верх диапазона неизвестной работы", 0, town.shiftQuote(s, "job_которой_нет").top)
    }

    @Test
    fun `закрытая и неизвестная работа не платят и не пускают играть`() {
        val plain = S1aStand.profile(demo = false)
        val kassa = town.shiftQuote(plain, "job_kassa")
        assertFalse("касса открыта на первой неделе обычной игры", kassa.open)
        assertFalse("закрытая работа платит", kassa.paid)
        assertFalse("в закрытую работу пускают играть", kassa.canPlay)
        assertEquals("строка закрытой работы", closed, kassa.line)
        assertEquals("база закрытой работы берётся из её записи", 6, kassa.base)
        assertEquals("смены недели видны и у закрытой работы", 3, kassa.shiftsLeft)

        val courier = town.shiftQuote(S1aStand.profile(), "job_courier")
        assertFalse("курьер открыт без сбывшейся мечты — в демо тоже нужна мечта", courier.open)
        assertEquals("строка курьера", closed, courier.line)

        val unknown = town.shiftQuote(S1aStand.profile(), "job_которой_нет")
        assertEquals("работа", "job_которой_нет", unknown.jobId)
        assertFalse("неизвестная работа открыта", unknown.open)
        assertFalse("неизвестная работа платит", unknown.paid)
        assertFalse("в неизвестную работу пускают играть", unknown.canPlay)
        assertEquals("уровень неизвестной работы", 0, unknown.level)
        assertEquals("база неизвестной работы", 0, unknown.base)
        assertEquals("бомбы неизвестной работы", 0, unknown.levelBombs)
        assertEquals("смены недели", 3, unknown.shiftsLeft)
        assertEquals("строка", closed, unknown.line)
    }

    @Test
    fun `работа закрыта пока закрыто её место`() {
        // в реальном контенте закрытое место есть только у курьера и совпадает с ключом работы,
        // поэтому место проверяется на копии контента: у пекарни своего opensBy нет, а место ждёт неделю 2
        val closedPlace = townWithBakeryFromWeek(2)

        val plain = S1aStand.profile(demo = false)
        val q = closedPlace.shiftQuote(plain, "job_bakery")
        assertFalse("работа открыта, хотя её место ещё закрыто", q.open)
        assertFalse("закрытое место платит", q.paid)
        assertFalse("в закрытое место пускают играть", q.canPlay)
        assertEquals("строка", closed, q.line)

        val demo = closedPlace.shiftQuote(S1aStand.profile(), "job_bakery")
        assertTrue("демо не пускает в место, которое ждёт недели", demo.open)
        assertEquals("строка в демо", "6–10 за смену", demo.line)

        val weekTwo = S1bStand.nextWeek(S1aStand.planned(10, 0, 0, demo = false))
        assertEquals("неделя", 2, weekTwo.period)
        assertTrue("место не открылось на своей неделе", closedPlace.shiftQuote(weekTwo, "job_bakery").open)
        assertTrue("та же работа с открытым местом закрыта", town.shiftQuote(plain, "job_bakery").open)
    }

    @Test
    fun `в обычной игре касса открывается ровно на своей неделе`() {
        var s = S1aStand.planned(10, 0, 0, demo = false)
        assertEquals("неделя", 1, s.period)
        assertFalse("касса открыта на неделе 1", town.shiftQuote(s, "job_kassa").open)

        s = S1bStand.nextWeek(s)
        assertEquals("неделя", 2, s.period)
        assertFalse("касса открыта на неделе 2", town.shiftQuote(s, "job_kassa").open)

        s = S1bStand.nextWeek(s)
        assertEquals("неделя", 3, s.period)
        assertFalse("касса открыта на неделе 3 — за неделю до срока", town.shiftQuote(s, "job_kassa").open)

        s = S1bStand.nextWeek(s)
        assertEquals("неделя", 4, s.period)
        val kassa = town.shiftQuote(s, "job_kassa")
        assertTrue("касса не открылась на неделе 4", kassa.open)
        assertTrue("касса открылась, но не платит", kassa.paid)
        assertEquals("строка кассы", "6–10 за смену", kassa.line)
    }

    @Test
    fun `сбывшаяся мечта открывает работу курьера с её базой`() {
        val s = courierOpen()
        assertEquals("мечта не в списке сбывшихся", listOf("goal_scooter"), s.achievedGoals.map { it.id })

        val courier = town.shiftQuote(s, "job_courier")
        assertTrue("курьер закрыт после сбывшейся мечты", courier.open)
        assertTrue("курьер не платит", courier.paid)
        assertEquals("база курьера", 8, courier.base)
        assertEquals("строка курьера", "8 за три поручения", courier.line)
        assertEquals("верх диапазона курьера равен базе", 8, courier.top)

        val r = town.finishShift(s, "job_courier", 3, 0)
        val after = r.s1aState()
        assertEquals("зарплата курьера", listOf(LedgerEntry("Смена: Курьер", 8)), after.envelope)
        assertEquals("строка", "8 за три поручения. ✉ +8 — $payLater", r.s1aOutcome().line)
        assertEquals("числа итога курьера", ShiftPay(8, 0, 8), r.s1aOutcome().pay)
        assertEquals("первая строка «Почему?»", "Ося: 1 из 6 смен до уровня 2", r.s1aOutcome().why.first())
    }

    @Test
    fun `ночью работа отказывает а загадка и бонус взрослого идут`() {
        val sleeping = town.endWeek(S1aStand.planned(40, 20, 30)).s1aState()
        assertTrue("после итога недели не ночь", sleeping.asleep)

        val q = town.shiftQuote(sleeping, "job_bakery")
        assertTrue("работа закрылась ночью", q.open)
        assertTrue("смены на новой неделе не начислились", q.paid)
        assertFalse("ночью пускают работать", q.canPlay)
        assertEquals("строка ночи", night, q.line)
        assertEquals("ночная смена", night, town.finishShift(sleeping, "job_bakery", 60, 0).s1aRefusal())

        // в строке shiftQuote закрытая работа идёт раньше ночи, в отказе finishShift — наоборот
        assertEquals("строка закрытой работы ночью", closed, town.shiftQuote(sleeping, "job_courier").line)
        assertEquals("ночная смена на закрытой работе", night, town.finishShift(sleeping, "job_courier", 3, 0).s1aRefusal())

        assertEquals("ночная загадка", "q_budget_1", town.nextQuestion(sleeping)?.id)
        val riddle = town.answerQuestion(sleeping, "q_budget_1", 0)
        assertEquals("бомбочка ночью", 1, riddle.s1aState().bombs)
        assertEquals("бонус взрослого ночью", 1, town.parentBonus(sleeping, 0).s1aState().parentBonusesThisPeriod)
    }

    @Test
    fun `после трёх смен все работы закрыты до нового конверта`() {
        var s = S1aStand.planned(40, 20, 30)
        s = town.finishShift(s, "job_bakery", 0, 0).s1aState()
        s = town.finishShift(s, "job_market", 3, 0).s1aState()
        s = town.finishShift(s, "job_kassa", 2, 0).s1aState()
        assertEquals("смен за неделю", 3, s.shiftsThisPeriod)

        listOf("job_bakery", "job_market", "job_kassa").forEach { id ->
            val q = town.shiftQuote(s, id)
            assertEquals("смен осталось у «$id»", 0, q.shiftsLeft)
            assertFalse("четвёртая смена у «$id» платит", q.paid)
            assertFalse("после лимита у «$id» пускают играть", q.canPlay)
            assertEquals("строка после лимита у «$id»", limitLine, q.line)
            assertEquals("четвёртая смена у «$id»", limitLine, town.finishShift(s, id, 1, 0).s1aRefusal())
        }

        // граничный счётчик: три смены до раскладки монет — строка после лимита одна и та же
        val beforePlan = S1aStand.profile().copy(shiftsThisPeriod = 3)
        val q = town.shiftQuote(beforePlan, "job_bakery")
        assertFalse("до плана после лимита пускают играть", q.canPlay)
        assertEquals("строка до плана", limitLine, q.line)
        assertEquals(
            "смена до плана после лимита",
            limitLine,
            town.finishShift(beforePlan, "job_bakery", 200, 0).s1aRefusal(),
        )
    }

    @Test
    fun `конец недели возвращает все три оплачиваемые смены`() {
        var s = S1aStand.planned(40, 20, 30)
        repeat(3) { s = town.finishShift(s, "job_bakery", 0, 0).s1aState() }
        assertEquals("смен за неделю", 0, town.shiftQuote(s, "job_bakery").shiftsLeft)

        val morning = town.wake(town.endWeek(s).s1aState()).s1aState()
        val q = town.shiftQuote(morning, "job_bakery")
        assertEquals("смены новой недели", 3, q.shiftsLeft)
        assertTrue("новая неделя не платит за смену", q.paid)
        assertEquals("мастерство сбросилось концом недели", 3, morning.jobShifts["job_bakery"])
    }

    // ---------- §2. Отказы finishShift ----------

    @Test
    fun `отказы смены идут по порядку и не меняют состояние`() {
        assertEquals(
            "нет питомца",
            "Сначала создай питомца",
            town.finishShift(economy.newGame(true), "job_bakery", 10, 0).s1aRefusal(),
        )

        val s = S1aStand.planned(40, 20, 30)
        val sleeping = town.endWeek(s).s1aState()
        assertEquals("ночь", night, town.finishShift(sleeping, "job_bakery", 10, 0).s1aRefusal())
        assertEquals("ночь раньше закрытой работы", night, town.finishShift(sleeping, "job_которой_нет", 10, 0).s1aRefusal())
        assertEquals("неизвестная работа", closed, town.finishShift(s, "job_которой_нет", 10, 0).s1aRefusal())
        assertEquals("закрытая работа", closed, town.finishShift(s, "job_courier", 3, 0).s1aRefusal())

        // три смены позади: после лимита работа закрыта — отказ строкой из shiftQuote
        var spent = s
        repeat(3) { spent = town.finishShift(spent, "job_bakery", 0, 0).s1aState() }
        assertEquals(
            "четвёртая смена у Марты",
            "Смены на неделе закончились — новые с новым конвертом",
            town.finishShift(spent, "job_market", 3, 0).s1aRefusal(),
        )
        assertEquals("состояние после отказов", S1aStand.planned(40, 20, 30), s)
    }

    @Test
    fun `смену нельзя закончить с чужими числами`() {
        val s = S1aStand.planned(40, 20, 30)
        assertEquals("счёт меньше нуля", badShift, town.finishShift(s, "job_bakery", -1, 0).s1aRefusal())
        assertEquals("бомбы меньше нуля", badShift, town.finishShift(s, "job_bakery", 10, -1).s1aRefusal())
        assertEquals("бомба у кнопочной работы", badShift, town.finishShift(s, "job_market", 3, 1).s1aRefusal())
        assertEquals("бомба у кассы", badShift, town.finishShift(s, "job_kassa", 3, 1).s1aRefusal())
        assertEquals("бомб больше, чем есть", badShift, town.finishShift(s, "job_bakery", 10, 1).s1aRefusal())
        assertEquals("поручений всего три", badShift, town.finishShift(s, "job_market", 4, 0).s1aRefusal())
        assertEquals("все три поручения", 6, town.finishShift(s, "job_market", 3, 0).s1aState().envelope.first().amount)

        // граничный счётчик бомбочек: две в кладовке, у уровня 0 своих нет
        val withBombs = s.copy(bombs = 2)
        assertEquals("ровно все бомбы", 0, town.finishShift(withBombs, "job_bakery", 10, 2).s1aState().bombs)
        assertEquals("на одну бомбу больше", badShift, town.finishShift(withBombs, "job_bakery", 10, 3).s1aRefusal())
    }

    // ---------- §2. Оплачиваемая смена ----------

    @Test
    fun `смена в пекарне кладёт зарплату в конверт а кошелёк и банки не трогает`() {
        val s = S1aStand.planned(50, 20, 30)
        val r = town.finishShift(s, "job_bakery", 60, 0)
        val after = r.s1aOutcome().state

        assertEquals("зарплата в конверте", listOf(LedgerEntry("Смена: Булочки в ряд", 8)), after.envelope)
        assertEquals("кошелёк изменился", s.balance, after.balance)
        assertEquals("копилка изменилась", s.savings, after.savings)
        assertEquals("банка «Нужное» изменилась", s.jarNeed, after.jarNeed)
        assertEquals("банка «Хочу» изменилась", s.jarWant, after.jarWant)
        assertEquals("журнал изменился", s.ledger, after.ledger)
        assertEquals("питомец изменился", s.pet, after.pet)
        assertEquals("смен за неделю", 1, after.shiftsThisPeriod)
        assertEquals("мастерство", 1, after.jobShifts["job_bakery"])
        assertEquals("рекорд поля", 60, after.records["job_bakery"])
        assertFalse("загадка не открылась концом смены", after.riddleAsked)

        val diary = after.diary.last()
        assertEquals("строка дневника", "Заработали 8: «Булочки в ряд»", diary.text)
        assertEquals("неделя строки дневника", s.period, diary.period)
        assertEquals("день строки дневника", s.day, diary.day)

        assertEquals("строка", "6 за смену + 2 за булочки. ✉ +8 — $payLater", r.s1aOutcome().line)
        assertEquals("числа итога", ShiftPay(6, 2, 8), r.s1aOutcome().pay)
        assertEquals(
            "«Почему?»",
            listOf("Боря: 1 из 6 смен до уровня 2", wage, "Новый рекорд!"),
            r.s1aOutcome().why,
        )
        assertS1aInvariants("после оплачиваемой смены", after)
    }

    @Test
    fun `зарплата приходит со следующим конвертом и штампу «По плану» не мешает`() {
        val s = town.finishShift(S1aStand.planned(50, 20, 30), "job_bakery", 60, 0).s1aState()
        assertEquals("кошелёк до конца недели", 70, s.balance)

        val ended = town.endWeek(s).s1aState()
        assertEquals("кошелёк после конверта", 70 + S1aStand.allowance + 8, ended.balance)
        assertEquals("конверт не опустел", emptyList<LedgerEntry>(), ended.envelope)
        assertEquals("зарплата в журнале", LedgerEntry("Смена: Булочки в ряд", 8), ended.ledger.last())
        val summary = ended.history.last()
        assertEquals("зарплата в итоге недели", 8, summary.shiftEarned)
        assertTrue("зарплата сняла штамп «По плану»", summary.planKept)
        assertEquals("смены новой недели", 0, ended.shiftsThisPeriod)
        assertS1aInvariants("после конверта с зарплатой", ended)

        assertEquals("строка утра", "Новый конверт: карманные 100 + зарплата 8", town.wake(ended).s1aOutcome().line)
    }

    @Test
    fun `база платится и за нулевой счёт`() {
        val s = S1aStand.planned(40, 20, 30)

        val taps = town.finishShift(s, "job_market", 0, 0)
        assertEquals("зарплата за поручения", listOf(LedgerEntry("Смена: Помочь Марте", 6)), taps.s1aState().envelope)
        assertEquals("строка", "6 за три поручения. ✉ +6 — $payLater", taps.s1aOutcome().line)
        assertEquals("числа итога кнопочной работы", ShiftPay(6, 0, 6), taps.s1aOutcome().pay)
        assertNull("кнопочная работа пишет рекорд", taps.s1aState().records["job_market"])

        val m3 = town.finishShift(s, "job_bakery", 0, 0)
        assertEquals("зарплата за смену", listOf(LedgerEntry("Смена: Булочки в ряд", 6)), m3.s1aState().envelope)
        assertEquals("строка", "6 за смену. ✉ +6 — $payLater", m3.s1aOutcome().line)
        assertEquals("числа итога без надбавки", ShiftPay(6, 0, 6), m3.s1aOutcome().pay)
        assertEquals("рекорд поля", 0, m3.s1aState().records["job_bakery"])
        assertEquals(
            "«Почему?» без рекорда",
            listOf("Боря: 1 из 6 смен до уровня 2", wage),
            m3.s1aOutcome().why,
        )
    }

    @Test
    fun `надбавка в пекарне растёт через каждые тридцать булочек и упирается в потолок`() {
        val s = S1aStand.planned(40, 20, 30)
        fun total(score: Int) = town.finishShift(s, "job_bakery", score, 0).s1aState().envelope.first().amount

        assertEquals("счёт 29 — надбавки ещё нет", 6, total(29))
        assertEquals("счёт 30 — первая надбавка", 7, total(30))
        assertEquals("счёт 120 — потолок надбавки", 10, total(120))
        assertEquals("счёт 1000 — надбавка выше потолка", 10, total(1000))
        assertEquals(
            "строка с надбавкой",
            "6 за смену + 1 за булочки. ✉ +7 — $payLater",
            town.finishShift(s, "job_bakery", 30, 0).s1aOutcome().line,
        )
    }

    @Test
    fun `касса платит за каждую верную сдачу и тоже не выше потолка`() {
        val s = S1aStand.planned(40, 20, 30)
        fun r(score: Int) = town.finishShift(s, "job_kassa", score, 0)

        assertEquals("ни одной верной сдачи", 6, r(0).s1aState().envelope.first().amount)
        assertEquals("три верные сдачи", 9, r(3).s1aState().envelope.first().amount)
        assertEquals("пять верных сдач — потолок 4", 10, r(5).s1aState().envelope.first().amount)
        assertEquals("строка кассы", "6 за смену + 3 за сдачу. ✉ +9 — $payLater", r(3).s1aOutcome().line)
        assertEquals("строка кассы без надбавки", "6 за смену. ✉ +6 — $payLater", r(0).s1aOutcome().line)
        assertEquals("числа итога кассы", ShiftPay(6, 3, 9), r(3).s1aOutcome().pay)
        assertEquals("числа итога кассы на потолке", ShiftPay(6, 4, 10), r(5).s1aOutcome().pay)
        assertEquals("рекорд кассы", 5, r(5).s1aState().records["job_kassa"])
    }

    @Test
    fun `три работы за один и тот же счёт платят по-разному`() {
        val s = S1aStand.planned(40, 20, 30)
        val bakery = town.finishShift(s, "job_bakery", 3, 0)
        val market = town.finishShift(s, "job_market", 3, 0)
        val kassa = town.finishShift(s, "job_kassa", 3, 0)

        assertEquals("пекарня", "6 за смену. ✉ +6 — $payLater", bakery.s1aOutcome().line)
        assertEquals("рынок", "6 за три поручения. ✉ +6 — $payLater", market.s1aOutcome().line)
        assertEquals("касса", "6 за смену + 3 за сдачу. ✉ +9 — $payLater", kassa.s1aOutcome().line)
        assertEquals("житель пекарни", "Боря: 1 из 6 смен до уровня 2", bakery.s1aOutcome().why.first())
        assertEquals("житель рынка", "Марта: 1 из 6 смен до уровня 2", market.s1aOutcome().why.first())
        assertEquals("житель кассы", "Кеша: 1 из 6 смен до уровня 2", kassa.s1aOutcome().why.first())
        assertEquals("конверт пекарни", "Смена: Булочки в ряд", bakery.s1aState().envelope.first().text)
        assertEquals("конверт рынка", "Смена: Помочь Марте", market.s1aState().envelope.first().text)
        assertEquals("конверт кассы", "Смена: Касса", kassa.s1aState().envelope.first().text)
    }

    // ---------- §2. Уровни и бомбы ----------

    @Test
    fun `смена оплачивается по уровню до неё а новый уровень идёт в «Почему?»`() {
        // граничный счётчик мастерства: пять смен позади, шестая поднимает уровень
        val s = withShifts(S1aStand.planned(40, 20, 30), "job_bakery", 5)
        assertEquals("база до смены", 6, town.shiftQuote(s, "job_bakery").base)
        assertEquals("уровень до смены", 0, town.shiftQuote(s, "job_bakery").level)

        val r = town.finishShift(s, "job_bakery", 0, 0)
        val after = r.s1aState()
        assertEquals("платить надо по уровню до смены", 6, after.envelope.first().amount)
        assertEquals("мастерство", 6, after.jobShifts["job_bakery"])
        assertEquals("уровень после смены", 1, town.shiftQuote(after, "job_bakery").level)
        assertEquals("база после смены", 7, town.shiftQuote(after, "job_bakery").base)
        assertEquals("бомбы уровня после смены", 1, town.shiftQuote(after, "job_bakery").levelBombs)
        assertEquals("верх диапазона после смены", 11, town.shiftQuote(after, "job_bakery").top)
        assertEquals("строка", "6 за смену. ✉ +6 — $payLater", r.s1aOutcome().line)
        assertEquals("первая строка «Почему?»", "Боря: новый уровень 2! Теперь 7–11 за смену", r.s1aOutcome().why.first())

        // пятнадцатая смена — третий уровень
        val toTop = withShifts(S1aStand.planned(40, 20, 30), "job_bakery", 14)
        val top = town.finishShift(toTop, "job_bakery", 0, 0)
        assertEquals("оплата четырнадцатой смены", 7, top.s1aState().envelope.first().amount)
        assertEquals("новый высший уровень", "Боря: новый уровень 3! Теперь 8–12 за смену", top.s1aOutcome().why.first())

        // у кнопочной работы новый уровень называет одно число — её котировкой
        val taps = town.finishShift(withShifts(S1aStand.planned(40, 20, 30), "job_market", 5), "job_market", 3, 0)
        assertEquals(
            "новый уровень кнопочной работы",
            "Марта: новый уровень 2! Теперь 7 за три поручения",
            taps.s1aOutcome().why.first(),
        )
    }

    @Test
    fun `«Почему?» показывает путь до следующего уровня и высшее мастерство`() {
        // граничные счётчики: три смены до строки прогресса, пятнадцать — до высшего уровня
        val progress = town.finishShift(withShifts(S1aStand.planned(40, 20, 30), "job_bakery", 3), "job_bakery", 0, 0)
        assertEquals("строка прогресса", "Боря: 4 из 6 смен до уровня 2", progress.s1aOutcome().why.first())

        val second = town.finishShift(withShifts(S1aStand.planned(40, 20, 30), "job_bakery", 6), "job_bakery", 0, 0)
        assertEquals("оплата второго уровня", 7, second.s1aState().envelope.first().amount)
        assertEquals("строка прогресса второго уровня", "Боря: 7 из 15 смен до уровня 3", second.s1aOutcome().why.first())

        val top = town.finishShift(withShifts(S1aStand.planned(40, 20, 30), "job_bakery", 15), "job_bakery", 0, 0)
        assertEquals("оплата высшего уровня", 8, top.s1aState().envelope.first().amount)
        assertEquals("строка высшего уровня", "Боря: высший уровень мастерства", top.s1aOutcome().why.first())
        assertEquals("«Почему?» на высшем уровне", listOf("Боря: высший уровень мастерства", wage), top.s1aOutcome().why)
    }

    @Test
    fun `сначала тратятся бомбы уровня потом накопленные а надбавка держит потолок`() {
        // граничные счётчики: второй уровень даёт свою бомбу, три лежат в кладовке
        val s = withShifts(S1aStand.planned(40, 20, 30), "job_bakery", 6).copy(bombs = 3)
        assertEquals("бомбы уровня", 1, town.shiftQuote(s, "job_bakery").levelBombs)

        val one = town.finishShift(s, "job_bakery", 1000, 1).s1aState()
        assertEquals("первой тратится бомба уровня", 3, one.bombs)

        val r = town.finishShift(s, "job_bakery", 1000, 3)
        val after = r.s1aState()
        assertEquals("накопленные бомбы", 1, after.bombs)
        assertEquals("надбавка выше потолка", 11, after.envelope.first().amount)
        assertEquals("строка", "7 за смену + 4 за булочки. ✉ +11 — $payLater", r.s1aOutcome().line)

        assertEquals("все бомбы сразу", 0, town.finishShift(s, "job_bakery", 1000, 4).s1aState().bombs)
        assertEquals("на одну бомбу больше", badShift, town.finishShift(s, "job_bakery", 1000, 5).s1aRefusal())
    }

    // ---------- §2. После лимита смен (решение № 47 б) ----------

    @Test
    fun `после лимита смена отказывает и рекорда не пишет`() {
        // граничный счётчик: три смены недели уже сыграны
        val beforePlan = S1aStand.profile().copy(shiftsThisPeriod = 3)
        assertEquals("до раскладки монет", limitLine, town.finishShift(beforePlan, "job_bakery", 200, 0).s1aRefusal())

        val s = S1aStand.planned(40, 20, 30).copy(shiftsThisPeriod = 3)
        assertEquals("четвёртая смена в пекарне", limitLine, town.finishShift(s, "job_bakery", 200, 0).s1aRefusal())
        assertEquals("четвёртая смена у кассы", limitLine, town.finishShift(s, "job_kassa", 2, 0).s1aRefusal())
        assertEquals("четвёртая смена у Марты", limitLine, town.finishShift(s, "job_market", 3, 0).s1aRefusal())
        assertNull("рекорд записан без оплачиваемой смены", s.records["job_bakery"])
        assertEquals("состояние после отказов", S1aStand.planned(40, 20, 30).copy(shiftsThisPeriod = 3), s)

        // новый конверт возвращает смены и оплату
        val fresh = S1bStand.nextWeek(s)
        assertTrue("новая неделя не платит за смену", town.shiftQuote(fresh, "job_bakery").paid)
        val worked = town.finishShift(fresh, "job_bakery", 200, 0)
        assertEquals("зарплата первой смены новой недели", 10, worked.s1aState().envelope.first().amount)
        assertEquals("рекорд первой смены новой недели", 200, worked.s1aState().records["job_bakery"])
    }

    @Test
    fun `новый рекорд в оплачиваемой смене виден в «Почему?»`() {
        var s = S1aStand.planned(40, 20, 30)
        s = town.finishShift(s, "job_bakery", 90, 0).s1aState()
        val weaker = town.finishShift(s, "job_bakery", 60, 0)
        assertEquals("рекорд просел", 90, weaker.s1aState().records["job_bakery"])
        assertEquals(
            "«Почему?» с чужим рекордом",
            listOf("Боря: 2 из 6 смен до уровня 2", wage),
            weaker.s1aOutcome().why,
        )

        val better = town.finishShift(s, "job_bakery", 91, 0)
        assertEquals("рекорд", 91, better.s1aState().records["job_bakery"])
        assertEquals(
            "«Почему?» с рекордом",
            listOf("Боря: 2 из 6 смен до уровня 2", wage, "Новый рекорд!"),
            better.s1aOutcome().why,
        )
    }

    // ---------- §3. «Загадка Бори» ----------

    @Test
    fun `верная загадка даёт бомбочку и молчит до конца смены в пекарне`() {
        val s = S1aStand.profile()
        assertEquals("первая загадка", "q_budget_1", town.nextQuestion(s)?.id)

        val r = town.answerQuestion(s, "q_budget_1", 0)
        val after = r.s1aState()
        assertEquals("бомбочка за верный ответ", 1, after.bombs)
        assertEquals("ответ записан", listOf(TaskResult("q_budget_1", true, 0, 1)), after.riddles)
        assertTrue("загадка не отмечена как заданная", after.riddleAsked)
        assertEquals("викторина 1.3.0 тронута", emptyList<TaskResult>(), after.quizResults)
        assertEquals("питомец изменился", s.pet, after.pet)
        assertEquals("монеты изменились", s.balance, after.balance)
        assertEquals("строка", "Верно! ${S1bStand.explanation("q_budget_1")}", r.s1aOutcome().line)
        assertEquals("«Почему?»", listOf("Бомбочка +1 — для поля «Булочки в ряд»"), r.s1aOutcome().why)

        assertNull("вторая загадка за один заказ", town.nextQuestion(after))
        assertEquals(
            "второй ответ за один заказ",
            "Следующая загадка — после смены",
            town.answerQuestion(after, "q_budget_2", 0).s1aRefusal(),
        )

        val worked = town.finishShift(after, "job_bakery", 30, 0).s1aState()
        assertFalse("конец смены в пекарне не открыл загадку", worked.riddleAsked)
        assertEquals("следующая загадка", "q_budget_2", town.nextQuestion(worked)?.id)
    }

    @Test
    fun `смена у Марты загадку не открывает`() {
        val asked = town.answerQuestion(S1aStand.profile(), "q_budget_1", 0).s1aState()
        val worked = town.finishShift(asked, "job_market", 3, 0).s1aState()
        assertTrue("кнопочная смена открыла загадку", worked.riddleAsked)
        assertNull("загадка появилась после кнопочной смены", town.nextQuestion(worked))
    }

    @Test
    fun `после лимита смена загадку не открывает и бомбы не тратит`() {
        var s = S1aStand.planned(40, 20, 30)
        s = town.answerQuestion(s, "q_budget_1", 0).s1aState()
        assertTrue("загадка не задана", s.riddleAsked)
        assertEquals("бомбочка за верный ответ", 1, s.bombs)
        assertNull("загадка ещё доступна", town.nextQuestion(s))

        // граничный счётчик: три смены недели уже сыграны
        val spent = s.copy(shiftsThisPeriod = 3)
        assertFalse("смена ещё оплачивается", town.shiftQuote(spent, "job_bakery").paid)
        assertEquals("своих бомб у первого уровня нет", 0, town.shiftQuote(spent, "job_bakery").levelBombs)

        assertEquals("смена после лимита", limitLine, town.finishShift(spent, "job_bakery", 60, 1).s1aRefusal())
        assertEquals(
            "отказ после лимита идёт раньше проверки бомб",
            limitLine,
            town.finishShift(spent, "job_bakery", 60, 2).s1aRefusal(),
        )
        assertNull("загадка открылась без смены", town.nextQuestion(spent))

        // загадку открывает только оплачиваемая смена — она приходит с новым конвертом
        val fresh = S1bStand.nextWeek(spent)
        val after = town.finishShift(fresh, "job_bakery", 60, 1).s1aState()
        assertFalse("оплачиваемая смена не сбросила загадку", after.riddleAsked)
        assertEquals("накопленная бомбочка не списана", 0, after.bombs)
        assertEquals("следующая загадка", "q_budget_2", town.nextQuestion(after)?.id)
    }

    @Test
    fun `конец недели загадку не открывает`() {
        val s = town.answerQuestion(S1aStand.planned(40, 20, 30), "q_budget_1", 0).s1aState()
        val ended = town.endWeek(s).s1aState()
        assertTrue("конец недели открыл загадку", ended.riddleAsked)
        assertNull("загадка нашлась без смены", town.nextQuestion(ended))
        assertEquals("ответы на загадки пропали за неделю", 1, ended.riddles.size)

        val morning = town.wake(ended).s1aState()
        assertTrue("утро новой недели открыло загадку", morning.riddleAsked)
        assertNull("загадка нашлась утром новой недели", town.nextQuestion(morning))

        val worked = town.finishShift(morning, "job_bakery", 30, 0).s1aState()
        assertEquals("загадку открывает только смена в пекарне", "q_budget_2", town.nextQuestion(worked)?.id)
    }

    @Test
    fun `неверный ответ не даёт бомбочку и следующим не повторяется`() {
        val s = S1aStand.profile()
        val r = town.answerQuestion(s, "q_budget_1", 1)
        val after = r.s1aState()
        assertEquals("бомбочка за неверный ответ", 0, after.bombs)
        assertEquals("ответ записан", listOf(TaskResult("q_budget_1", false, 0, 1)), after.riddles)
        assertEquals("строка", S1bStand.explanation("q_budget_1"), r.s1aOutcome().line)
        assertEquals("«Почему?»", listOf("Вопрос вернётся позже"), r.s1aOutcome().why)

        val worked = town.finishShift(after, "job_bakery", 30, 0).s1aState()
        assertEquals("неверный вопрос задан снова", "q_budget_2", town.nextQuestion(worked)?.id)

        // у второго вопроса верный ответ — 1, значит 0 снова неверный
        val again = town.finishShift(town.answerQuestion(worked, "q_budget_2", 0).s1aState(), "job_bakery", 30, 0).s1aState()
        assertEquals("третья загадка", "q_budget_3", town.nextQuestion(again)?.id)
    }

    @Test
    fun `загадки без верного ответа возвращаются по очереди от самой давней`() {
        val ids = S1bStand.quizIds
        assertEquals("вопросов в городке", 15, ids.size)

        // граничная очередь: все пятнадцать разобраны неверно, самый давний — последний по списку
        val all = S1aStand.profile().copy(riddles = ids.reversed().map { S1bStand.riddle(it, false) })
        assertEquals("очередь начинается не с самой давней попытки", ids.last(), town.nextQuestion(all)?.id)

        val asked = all.copy(riddles = all.riddles + S1bStand.riddle(ids.last(), false))
        assertEquals("после повтора очередь не сдвинулась", ids[13], town.nextQuestion(asked)?.id)

        val solved = all.copy(riddles = all.riddles + S1bStand.riddle(ids.last(), true))
        assertEquals("разобранный вопрос остался в очереди", ids[13], town.nextQuestion(solved)?.id)
    }

    @Test
    fun `после пятнадцати верных ответов загадок больше нет`() {
        val ids = S1bStand.quizIds
        // граничная очередь: все пятнадцать уже разобраны верно
        val done = S1aStand.profile().copy(riddles = ids.map { S1bStand.riddle(it, true) })
        assertNull("загадка нашлась после пятнадцати верных", town.nextQuestion(done))
        assertEquals(
            "вопрос задан второй раз",
            "Этот вопрос уже разобран",
            town.answerQuestion(done, ids.first(), 0).s1aRefusal(),
        )
    }

    @Test
    fun `ответы викторины 1_3_0 загадок не снимают`() {
        val ids = S1bStand.quizIds
        // старый профиль 1.3.0: вся викторина пройдена верно, загадок «Городка» ещё не было
        val old = S1aStand.profile().copy(quizResults = ids.map { S1bStand.riddle(it, true) })
        assertEquals("загадка пропала из-за старой викторины", ids.first(), town.nextQuestion(old)?.id)

        val r = town.answerQuestion(old, ids.first(), 0)
        assertEquals("ответ на загадку записан", 1, r.s1aState().riddles.size)
        assertEquals("викторина 1.3.0 изменилась", old.quizResults, r.s1aState().quizResults)
        assertEquals("бомбочка за загадку", 1, r.s1aState().bombs)
    }

    @Test
    fun `отказы загадки идут по порядку и не меняют состояние`() {
        assertEquals(
            "нет питомца",
            "Сначала создай питомца",
            town.answerQuestion(economy.newGame(true), "q_budget_1", 0).s1aRefusal(),
        )

        val s = S1aStand.profile()
        assertEquals("нет такого вопроса", "Этот вопрос уже разобран", town.answerQuestion(s, "q_нет", 0).s1aRefusal())
        assertEquals("ответ меньше нуля", "Выбери ответ", town.answerQuestion(s, "q_budget_1", -1).s1aRefusal())
        assertEquals("ответ за списком", "Выбери ответ", town.answerQuestion(s, "q_budget_1", 3).s1aRefusal())
        assertEquals("состояние после отказов", S1aStand.profile(), s)

        val asked = town.answerQuestion(s, "q_budget_1", 0).s1aState()
        assertEquals(
            "заданная загадка идёт раньше неизвестного вопроса",
            "Следующая загадка — после смены",
            town.answerQuestion(asked, "q_нет", 0).s1aRefusal(),
        )
        assertEquals(
            "заданная загадка идёт раньше проверки ответа",
            "Следующая загадка — после смены",
            town.answerQuestion(asked, "q_budget_1", 9).s1aRefusal(),
        )
    }

    // ---------- §4. Бонус взрослого ----------

    @Test
    fun `бонус взрослого идёт в конверт и считает остаток недели`() {
        val s = S1aStand.planned(40, 20, 30)
        val first = town.parentBonus(s, 0)
        val afterFirst = first.s1aState()
        assertEquals(
            "строка конверта",
            listOf(LedgerEntry("Бонус от взрослого: Идея, как сэкономить", 10)),
            afterFirst.envelope,
        )
        assertEquals("кошелёк изменился", s.balance, afterFirst.balance)
        assertEquals("журнал изменился", s.ledger, afterFirst.ledger)
        assertEquals("бонусов за неделю", 1, afterFirst.parentBonusesThisPeriod)
        assertEquals("строка", "Придёт в новом конверте ребёнка", first.s1aOutcome().line)
        assertEquals("«Почему?»", listOf("На этой неделе можно начислить ещё 2"), first.s1aOutcome().why)

        val second = town.parentBonus(afterFirst, 1)
        assertEquals(
            "причина берётся из списка городка",
            "Бонус от взрослого: Рассказ о своей мечте",
            second.s1aState().envelope.last().text,
        )
        assertEquals("«Почему?»", listOf("На этой неделе можно начислить ещё 1"), second.s1aOutcome().why)

        val third = town.parentBonus(second.s1aState(), 2)
        assertEquals("«Почему?» на последнем бонусе", listOf("Лимит бонусов на эту неделю исчерпан"), third.s1aOutcome().why)
        assertEquals("бонусов за неделю", 3, third.s1aState().parentBonusesThisPeriod)
        assertEquals(
            "четвёртый бонус",
            "На этой неделе все бонусы уже начислены — новые будут со следующей недели",
            town.parentBonus(third.s1aState(), 0).s1aRefusal(),
        )

        val ended = town.endWeek(third.s1aState()).s1aState()
        assertEquals("бонусы пришли в кошелёк", 70 + S1aStand.allowance + 30, ended.balance)
        assertEquals("бонус в итоге недели", 30, ended.history.last().parentBonus)
        assertS1aInvariants("после конверта с бонусами", ended)
        assertEquals("лимит новой недели", 0, ended.parentBonusesThisPeriod)
        assertEquals("бонус ночью после итога", 1, town.parentBonus(ended, 0).s1aState().parentBonusesThisPeriod)
    }

    @Test
    fun `бонус взрослого отказывает без питомца и без причины`() {
        assertEquals(
            "нет питомца",
            "Сначала создай питомца",
            town.parentBonus(economy.newGame(true), 0).s1aRefusal(),
        )
        val s = S1aStand.planned(40, 20, 30)
        assertEquals("причина меньше нуля", "Выберите причину бонуса", town.parentBonus(s, -1).s1aRefusal())
        assertEquals("причина за списком", "Выберите причину бонуса", town.parentBonus(s, 3).s1aRefusal())
        assertEquals("последняя причина списка", 1, town.parentBonus(s, 2).s1aState().parentBonusesThisPeriod)
    }

    // ---------- Тексты ----------

    @Test
    fun `тексты смен загадок и бонуса без стоп-слов и без рода`() {
        val s = S1aStand.planned(40, 20, 30)
        val spent = s.copy(shiftsThisPeriod = 3)
        val level = withShifts(s, "job_bakery", 5).copy(bombs = 2)
        val sleeping = town.endWeek(s).s1aState()
        val texts = buildList {
            listOf("job_bakery", "job_market", "job_kassa", "job_courier", "job_которой_нет").forEach {
                add(town.shiftQuote(s, it).line)
                add(town.shiftQuote(spent, it).line)
                add(town.shiftQuote(sleeping, it).line)
                add(town.shiftQuote(S1aStand.profile().copy(shiftsThisPeriod = 3), it).line)
            }
            addAll(town.finishShift(s, "job_bakery", 60, 0).s1aTexts())
            addAll(town.finishShift(s, "job_bakery", 0, 0).s1aTexts())
            addAll(town.finishShift(level, "job_bakery", 1000, 2).s1aTexts())
            addAll(town.finishShift(withShifts(s, "job_bakery", 15), "job_bakery", 0, 0).s1aTexts())
            addAll(town.finishShift(s, "job_market", 3, 0).s1aTexts())
            addAll(town.finishShift(s, "job_kassa", 3, 0).s1aTexts())
            addAll(town.finishShift(spent, "job_bakery", 200, 0).s1aTexts())
            addAll(town.finishShift(s, "job_market", 9, 0).s1aTexts())
            addAll(town.finishShift(sleeping, "job_bakery", 9, 0).s1aTexts())
            addAll(town.finishShift(s, "job_courier", 3, 0).s1aTexts())
            addAll(town.answerQuestion(s, "q_budget_1", 0).s1aTexts())
            addAll(town.answerQuestion(s, "q_budget_1", 1).s1aTexts())
            addAll(town.answerQuestion(s, "q_budget_1", 9).s1aTexts())
            S1bStand.quizIds.forEach { addAll(town.answerQuestion(s, it, 0).s1aTexts()) }
            addAll(town.parentBonus(s, 0).s1aTexts())
            addAll(town.parentBonus(s, 7).s1aTexts())
            var bonuses = s
            repeat(4) { bonuses = (town.parentBonus(bonuses, 0) as? TownResult.Done)?.outcome?.state ?: bonuses }
            addAll(town.parentBonus(bonuses, 0).s1aTexts())
        }
        S1aChildText.check("смены, загадки и бонус", texts)
    }

    // ---------- Сцены ----------

    /** Копия контента: место пекарни ждёт неделю n, сама работа остаётся открытой. */
    private fun townWithBakeryFromWeek(n: Int): Town {
        val base = S1bStand.content
        val t = base.town!!
        val places = t.places.map { if (it.id == "bakery") it.copy(opensBy = OpensBy(week = n)) else it }
        val job = t.jobs.first { it.id == "job_bakery" }
        assertEquals("у работы в пекарне появился свой ключ открытия", OpensBy(), job.opensBy)
        return Town(base.copy(town = t.copy(places = places)))
    }

    /** Мечта-ключ: самокат 150 — два конверта по 100 в копилку, потом «Мечта сбылась». */
    private fun courierOpen(): GameState {
        var s = economy.chooseGoal(S1aStand.planned(0, 0, 100), S1aStand.townGoal("goal_scooter")).ok()
        s = town.endWeek(s).s1aState() // демо-кнопка «Сразу к итогу недели»
        s = town.wake(s).s1aState()
        s = town.confirmPlan(s).s1aState() // заготовка та же: 0 / 0 / 100
        return town.achieveGoal(s).s1aState()
    }
}
