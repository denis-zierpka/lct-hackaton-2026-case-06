package ru.finny.pet.domain.town

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.pet.domain.Content
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.LedgerEntry
import ru.finny.pet.domain.TaskResult

/**
 * Оракул мини-игры «Поднос по заказу» (docs/tasks/TOWN-J1-0.md § 1–§ 4, TOWN-J1-1a.md § 1).
 * Срезом 1а пекарня в content.json стала «Помочь Боре» на подносах (game = TRAY, полей Match3 нет),
 * поэтому раунд, оплата и загадка в раунде проверяются на настоящем контенте (J1Stand.content).
 * Ветка поля «три в ряд» живёт на копии S1bStand.content3. Числа меню, ступеней и demoSizes в тестах
 * не дублируются: они читаются из контента.
 * Состояния строятся настоящим путём игрока (S1aStand.profile / planned, Town.finishShift, Tray.*);
 * граничные счётчики (seed, period, shiftsThisPeriod, jobShifts, records, riddles) задаются copy
 * с комментарием, почему настоящий путь сюда не годится.
 */
internal object J1Stand {
    const val BAKERY = "job_bakery"

    private fun withBakery(base: Content, f: (Job) -> Job): Content {
        val t = base.town ?: error("content.json: нет ключа town")
        return base.copy(town = t.copy(jobs = t.jobs.map { if (it.id == BAKERY) f(it) else it }))
    }

    /** Настоящий контент среза 1а: пекарня на подносах. */
    val content: Content = S1aStand.content

    /** Копия: потолок надбавки 2 при тех же sizes — валидатор § 5 к ней не применяется. */
    val capped: Content = content.town!!.let { t -> content.copy(town = t.copy(rules = t.rules.copy(shiftBonusMax = 2))) }

    /** Вторая копия: пекарь — Ася, она же житель недели 5 (жителя работы не зовут в покупатели). */
    val bakerAsya: Content = withBakery(content) { it.copy(resident = "asya") }

    val town = Town(content)
    val cappedTown = Town(capped)
    val asyaTown = Town(bakerAsya)

    val job: Job get() = content.town!!.jobs.first { it.id == BAKERY }
    val rules: TownRules get() = content.town!!.rules
    val residents: List<Resident> get() = content.town!!.residents

    /** Ступень меню после n оплаченных смен — правило § 3, числа из content.json. */
    fun step(n: Int): TrayStep = job.steps.last { it.fromShift <= n }

    fun showcase(n: Int): List<String> = job.menu.take(step(n).kinds).map { it.id }

    fun sizes(n: Int, demo: Boolean): List<Int> =
        if (demo) job.demoSizes ?: error("в записи пекарни нет demoSizes") else step(n).sizes

    /** Пул покупателей недели w: пришедшие жители, кроме жителя работы. */
    fun pool(w: Int, jobResident: String = job.resident): List<String> = residents.filter {
        val week = it.arrivesWeek
        week != null && week <= w && it.id != jobResident
    }.map { it.id }
}

class TrayTest {

    private val bakery = J1Stand.BAKERY
    private val town = J1Stand.town
    private val payLater = "придёт с новым конвертом"
    private val wage = "Карманные приходят каждую неделю, зарплата — когда поработаешь"
    private val badShift = "Так закончить смену нельзя"
    private val limit = "Смены на неделе закончились — новые с новым конвертом"

    /** Строки загадки в раунде (TOWN-J1-1a § 1). */
    private val noRiddle = "Загадки сейчас нет"
    private val hintIdle = "Этот поднос Боря оставил тебе"
    private val hintWhy = "Подсказка Бори — на поднос"
    private val laterWhy = "Вопрос вернётся позже"

    /**
     * Граничные счётчики: настоящий путь до 8 смен в пекарне на неделе 6 — это шесть недель игры
     * и 18 смен подряд, а проверяется одна раскладка.
     */
    private fun stand(seed: Long, period: Int = 6, shifts: Int = 0, n: Int = 8, demo: Boolean = false): GameState =
        S1aStand.profile(demo = demo)
            .copy(seed = seed, period = period, shiftsThisPeriod = shifts, jobShifts = mapOf(bakery to n))

    private fun ordersOf(s: GameState): List<TrayOrder> =
        town.trayRound(s, bakery)?.orders ?: error("раунд не собрался при seed ${s.seed}, неделе ${s.period}")

    // ---------- § 3. Житель недели ----------

    @Test
    fun `житель недели — последний из приехавших`() {
        val expected = mapOf(1 to "osya", 2 to "tosha", 3 to "stepan", 4 to "kesha", 5 to "asya", 6 to "asya")
        expected.forEach { (week, id) ->
            assertEquals("житель недели $week", id, town.residentOfWeek(stand(1, period = week))?.id)
        }
        // граничная неделя: до первой недели никто не приехал
        assertNull("житель недели нашёлся до первой недели", town.residentOfWeek(stand(1, period = 0)))

        // настоящий путь: вторая неделя игры
        val weekTwo = S1bStand.nextWeek(S1aStand.planned(10, 0, 0, demo = false))
        assertEquals("неделя", 2, weekTwo.period)
        assertEquals("житель второй недели игры", "tosha", town.residentOfWeek(weekTwo)?.id)
    }

    // ---------- § 3. Рекорд звёзд ----------

    @Test
    fun `рекорд звёзд есть только у работы на подносах и отсекает наследие Match3`() {
        val s = S1aStand.profile()
        val max = J1Stand.rules.shiftBonusMax
        // граничные записи profile.records: настоящий путь к записи 60 — это смены Match3 прошлых версий
        fun stars(v: Int?, engine: Town = town, jobId: String = bakery): Int? =
            engine.bestStars(if (v == null) s else s.copy(records = mapOf(jobId to v)), jobId)

        assertNull("рекорд звёзд у работы Match3", stars(2, engine = S1bStand.town3))
        assertNull("рекорд без записи в профиле", stars(null))
        assertNull("рекорд неизвестной работы", stars(2, jobId = "job_которой_нет"))
        assertNull("рекорд кнопочной работы", stars(2, jobId = "job_market"))
        assertEquals("ноль звёзд — это рекорд", 0, stars(0))
        assertEquals("три звезды", 3, stars(3))
        assertEquals("все звёзды смены", max, stars(max))
        assertNull("на одну звезду больше потолка", stars(max + 1))
        assertNull("наследие Match3 — 60 булочек", stars(60))
    }

    // ---------- § 3. Когда раунд есть ----------

    @Test
    fun `раунд собирается только у оплачиваемой смены`() {
        val s = S1aStand.planned(40, 20, 30)
        assertNotNull("у оплачиваемой смены нет раунда", town.trayRound(s, bakery))
        assertNull("раунд у работы Match3", S1bStand.town3.trayRound(s, bakery))
        assertNull("раунд у кнопочной работы", town.trayRound(s, "job_market"))
        assertNull("раунд у неизвестной работы", town.trayRound(s, "job_которой_нет"))
        assertNull("раунд у закрытой работы", town.trayRound(s, "job_courier"))

        val sleeping = town.endWeek(s).s1aState()
        assertTrue("после итога недели не ночь", sleeping.asleep)
        assertNull("раунд ночью", town.trayRound(sleeping, bakery))

        var spent = s
        repeat(3) { spent = town.finishShift(spent, bakery, 0, 0).s1aState() }
        assertEquals("смены недели ещё остались", 0, town.shiftQuote(spent, bakery).shiftsLeft)
        assertNull("раунд после лимита смен", town.trayRound(spent, bakery))
    }

    /** Копия копии: у первой ступени одно изделие на витрине, заказы по-прежнему по 2 изделия. */
    private fun narrowShowcase(): Town {
        val c = J1Stand.content
        val t = c.town!!
        val jobs = t.jobs.map { j ->
            if (j.id == bakery) j.copy(steps = j.steps.mapIndexed { i, st -> if (i == 0) st.copy(kinds = 1) else st }) else j
        }
        return Town(c.copy(town = t.copy(jobs = jobs)))
    }

    @Test
    fun `раунда нет когда на витрине меньше видов чем нужно заказу`() {
        val narrow = narrowShowcase()
        listOf(true, false).forEach { demo ->
            assertTrue(
                "в этой ступени нет заказа из двух видов, витрина из одного изделия ничему не мешает",
                J1Stand.sizes(0, demo).any { it >= 2 },
            )
            assertNull(
                "раунд собрался на витрине из одного изделия, демо $demo",
                narrow.trayRound(stand(1, n = 0, demo = demo), bakery),
            )
        }
    }

    // ---------- § 3. Ступень меню ----------

    @Test
    fun `ступень меню растёт по числу оплаченных смен`() {
        listOf(0, 1, 2, 3, 5, 7, 20).forEach { n ->
            val step = J1Stand.step(n)
            val r = town.trayRound(stand(1, n = n), bakery) ?: error("нет раунда при $n сменах")
            assertEquals("витрина при $n сменах", J1Stand.showcase(n), r.menu)
            assertEquals("размеры заказов при $n сменах", step.sizes, r.orders.map { it.items.size })
            assertEquals("покупателей при $n сменах", step.sizes.size, r.orders.size)
            assertEquals("реплика жителя при $n сменах", if (n == step.fromShift) step.intro else null, r.intro)
        }
        assertNotNull("ни одна ступень не начинается репликой — проверка intro пустая", J1Stand.step(1).intro)
        assertNull("первая ступень начинается репликой", J1Stand.step(0).intro)
    }

    @Test
    fun `в демо размеры заказов берутся из demoSizes а витрина — из ступени`() {
        val n = 1
        val step = J1Stand.step(n)
        val demoSizes = J1Stand.sizes(n, demo = true)
        assertNotEquals("demoSizes совпали с sizes ступени — ветка демо не видна", step.sizes, demoSizes)

        val r = town.trayRound(stand(1, n = n, demo = true), bakery) ?: error("нет раунда в демо")
        assertEquals("размеры заказов в демо", demoSizes, r.orders.map { it.items.size })
        assertEquals("витрина в демо", J1Stand.showcase(n), r.menu)
        assertEquals("реплика ступени в демо", step.intro, r.intro)
    }

    @Test
    fun `новый демо-профиль начинает с первой ступени и доходит до лимита смен`() {
        // настоящий первый запуск: питомец создан, монеты ещё не разложены, смен в пекарне нет
        val fresh = S1aStand.profile(demo = true)
        assertNull("у нового профиля уже есть смены в пекарне", fresh.jobShifts[bakery])
        assertFalse("монеты у нового профиля уже разложены", fresh.plan.confirmed)

        val first = town.trayRound(fresh, bakery) ?: error("у нового демо-профиля нет раунда в пекарне")
        assertEquals("витрина первой смены", J1Stand.showcase(0), first.menu)
        assertEquals("размеры заказов первой смены", J1Stand.sizes(0, demo = true), first.orders.map { it.items.size })
        assertTrue("указателя нет в самой первой смене", first.pointer)
        assertNull("реплика новинки в первой смене", first.intro)
        assertFalse("значок «?» в самой первой смене", first.riddle)

        // одна смена позади — вторая ступень: новая витрина, реплика новинки и значок «?»
        val after = town.finishShift(fresh, bakery, 0, 0).s1aState()
        assertEquals("смен в пекарне", 1, after.jobShifts[bakery])
        val second = town.trayRound(after, bakery) ?: error("нет раунда во второй смене")
        assertNotEquals("вторая ступень не меняет витрину — проверка пустая", J1Stand.showcase(0), J1Stand.showcase(1))
        assertEquals("витрина второй смены", J1Stand.showcase(1), second.menu)
        assertNotNull("у второй ступени нет реплики — проверка пустая", J1Stand.step(1).intro)
        assertEquals("реплика новинки", J1Stand.step(1).intro, second.intro)
        assertTrue("во второй смене нет значка «?»", second.riddle)

        // три смены недели сыграны — работа закрыта до нового конверта (№ 47 б)
        var spent = after
        repeat(2) { spent = town.finishShift(spent, bakery, 0, 0).s1aState() }
        assertEquals("смен за неделю", 3, spent.shiftsThisPeriod)
        assertNull("раунд после лимита смен", town.trayRound(spent, bakery))
    }

    @Test
    fun `в заказе ровно столько видов сколько велит правило`() {
        listOf(1, 7).forEach { n ->
            (1L..20L).forEach { seed ->
                val r = town.trayRound(stand(seed, n = n), bakery) ?: error("нет раунда при $n сменах")
                r.orders.forEach { o ->
                    val k = o.items.size
                    assertEquals("видов в заказе $o при $n сменах", minOf(k, 1 + k / 2), o.items.distinct().size)
                    assertTrue("изделие не с витрины ${r.menu} в заказе $o", o.items.all { it in r.menu })
                    assertEquals(
                        "изделия заказа $o не по порядку меню ${r.menu}",
                        o.items.sortedBy { r.menu.indexOf(it) }, o.items,
                    )
                }
            }
        }
        assertTrue("в этих ступенях нет заказа из 4 изделий", J1Stand.step(7).sizes.any { it == 4 })
    }

    // ---------- § 3. Покупатели ----------

    @Test
    fun `покупатели — пришедшие жители кроме жителя работы и без повтора подряд`() {
        (1..5).forEach { week ->
            val pool = J1Stand.pool(week)
            assertTrue("на неделе $week покупателей меньше двух", pool.size >= 2)
            (1L..50L).forEach { seed ->
                val s = stand(seed, period = week, n = 1)
                val r = town.trayRound(s, bakery) ?: error("нет раунда на неделе $week")
                val customers = r.orders.map { it.customer }
                assertTrue("покупатели $customers вне пула $pool недели $week", customers.all { it in pool })
                assertFalse("житель работы стоит в очереди $customers", J1Stand.job.resident in customers)
                customers.zipWithNext().forEach { (a, b) ->
                    assertNotEquals("два заказа подряд у одного жителя на неделе $week", a, b)
                }
                val w = town.residentOfWeek(s) ?: error("нет жителя недели $week")
                assertTrue("жителя недели ${w.id} нет ни в одном заказе $customers", customers.any { it == w.id })
            }
        }
    }

    @Test
    fun `житель недели не приходит покупателем если он сам за прилавком`() {
        val s = stand(1, period = 5, n = 1)
        assertEquals("житель недели 5", "asya", J1Stand.asyaTown.residentOfWeek(s)?.id)
        assertEquals("в копии за прилавком не Ася", "asya", J1Stand.bakerAsya.town!!.jobs.first { it.id == bakery }.resident)
        (1L..50L).forEach { seed ->
            val r = J1Stand.asyaTown.trayRound(stand(seed, period = 5, n = 1), bakery)
                ?: error("нет раунда при seed $seed")
            assertTrue("Ася печёт и сама же покупает — ${r.orders.map { it.customer }}", r.orders.none { it.customer == "asya" })
        }
    }

    // ---------- § 3. Указатель и загадка ----------

    @Test
    fun `указатель горит пока у работы нет ни одной звезды`() {
        val s = S1aStand.planned(40, 20, 30)
        assertTrue("в первой смене указателя нет", town.trayRound(s, bakery)!!.pointer)

        // настоящий путь: смена без звёзд пишет рекорд 0 и указатель не снимает
        val zero = town.finishShift(s, bakery, 0, 0).s1aState()
        assertEquals("рекорд звёзд после смены без звёзд", 0, town.bestStars(zero, bakery))
        assertTrue("смена с нулём звёзд сняла указатель", town.trayRound(zero, bakery)!!.pointer)

        val one = town.finishShift(s, bakery, 1, 0).s1aState()
        assertFalse("указатель остался после первой звезды", town.trayRound(one, bakery)!!.pointer)

        // граничная запись профиля: 60 булочек Match3 — не рекорд звёзд
        assertTrue("наследие Match3 сняло указатель", town.trayRound(s.copy(records = mapOf(bakery to 60)), bakery)!!.pointer)
    }

    @Test
    fun `значок загадки приходит не в первой смене и один раз за смену`() {
        val s = S1aStand.planned(40, 20, 30)
        assertFalse("загадка в самой первой смене", town.trayRound(s, bakery)!!.riddle)

        val second = town.finishShift(s, bakery, 0, 0).s1aState()
        assertEquals("смен в пекарне", 1, second.jobShifts[bakery])
        assertTrue("во второй смене загадки нет", town.trayRound(second, bakery)!!.riddle)

        val asked = town.answerQuestion(second, "q_budget_1", 0).s1aState()
        assertTrue("загадка не отмечена как заданная", asked.riddleAsked)
        assertFalse("вторая загадка за одну смену", town.trayRound(asked, bakery)!!.riddle)

        // граничная очередь: все пятнадцать загадок разобраны верно
        val solved = second.copy(riddles = S1bStand.quizIds.map { S1bStand.riddle(it, true) })
        assertFalse("загадка после всех пятнадцати верных ответов", town.trayRound(solved, bakery)!!.riddle)
    }

    // ---------- § 3. Случайность ----------

    @Test
    fun `при равных счётчиках заказы совпадают`() {
        assertEquals("два одинаковых профиля дали разные заказы", ordersOf(stand(7)), ordersOf(stand(7)))
        val one = stand(7)
        assertEquals("повторный вызов на том же профиле дал другие заказы", ordersOf(one), ordersOf(one))
    }

    @Test
    fun `кошелёк день дневник покупки и наклейки на заказы не влияют`() {
        val base = stand(7)
        val rich = base.copy(
            balance = base.balance + 50, savings = 99, jarNeed = 1, jarWant = 2, day = 3,
            diary = base.diary + DiaryLine(1, 1, "Купили на рынке корм"),
            stickers = listOf("st_place_home"),
            envelope = listOf(LedgerEntry("Бонус от взрослого: Идея, как сэкономить", 10)),
        )
        assertEquals("заказы изменились от кошелька, дня и дневника", ordersOf(base), ordersOf(rich))

        // настоящий путь: раскладка монет и покупка корма на рынке
        val bought = town.buyAt(
            S1aStand.planned(40, 20, 30, demo = false).copy(seed = 7, period = 6, jobShifts = mapOf(bakery to 8)),
            "food_basic", "shop_market", Source.NEED,
        ).s1aState()
        assertTrue("покупка не записалась", bought.purchases.isNotEmpty())
        assertEquals("заказы изменились от покупки", ordersOf(base), ordersOf(bought))
    }

    @Test
    fun `seed профиля входит в раскладку заказов`() {
        val all = (1L..20L).map { ordersOf(stand(it)) }
        assertTrue("заказы одинаковы при всех seed от 1 до 20", all.distinct().size > 1)
    }

    @Test
    fun `число смен на работе входит в раскладку заказов`() {
        assertEquals("8 и 9 смен — разные ступени, различие было бы не от смеси", J1Stand.step(8), J1Stand.step(9))
        assertTrue(
            "заказы при 8 и 9 сменах совпали на всех seed от 1 до 20",
            (1L..20L).any { ordersOf(stand(it, n = 8)) != ordersOf(stand(it, n = 9)) },
        )
    }

    @Test
    fun `неделя входит в раскладку заказов`() {
        assertEquals("пул покупателей на неделях 6 и 7 разный", J1Stand.pool(6), J1Stand.pool(7))
        assertEquals(
            "житель недели 6 и 7 разный",
            town.residentOfWeek(stand(1, period = 6))?.id, town.residentOfWeek(stand(1, period = 7))?.id,
        )
        assertTrue(
            "заказы на неделях 6 и 7 совпали на всех seed от 1 до 20",
            (1L..20L).any { ordersOf(stand(it, period = 6)) != ordersOf(stand(it, period = 7)) },
        )
    }

    @Test
    fun `сыгранные за неделю смены входят в раскладку заказов`() {
        assertTrue("смена за неделю не оплачивается", town.shiftQuote(stand(1, shifts = 1), bakery).paid)
        assertTrue(
            "заказы при 0 и 1 сыгранной смене совпали на всех seed от 1 до 20",
            (1L..20L).any { ordersOf(stand(it, shifts = 0)) != ordersOf(stand(it, shifts = 1)) },
        )
    }

    // ---------- § 2. Поднос: put и take ----------

    /** Синтетическая витрина: Tray читает только раунд, содержимое контента ему не нужно. */
    private val showcase = listOf("a", "b", "c", "x")

    private fun round(
        order: List<String>,
        tray: List<String> = emptyList(),
        missed: Boolean = false,
        results: List<Boolean> = emptyList(),
    ) = TrayRound(
        jobId = bakery, menu = showcase, orders = listOf(TrayOrder("marta", order)),
        tray = tray, missed = missed, results = results,
    )

    @Test
    fun `изделие ложится в конец подноса а лишнее нажатие ничего не меняет`() {
        val r = round(listOf("a", "a", "b"))
        val one = Tray.put(r, "b")
        assertEquals("поднос после первого нажатия", listOf("b"), one.tray)
        val full = Tray.put(Tray.put(one, "x"), "a")
        assertEquals("поднос в порядке укладки", listOf("b", "x", "a"), full.tray)
        assertEquals("на полный поднос легло четвёртое изделие", full, Tray.put(full, "a"))
        assertEquals("на поднос легло изделие не с витрины", r, Tray.put(r, "z"))
        assertEquals("укладка тронула звёзды", emptyList<Boolean>(), full.results)

        val done = round(listOf("a", "a", "b"), results = listOf(true))
        assertTrue("раунд с обслуженным покупателем не закончен", done.done)
        assertEquals("укладка после последнего покупателя", done, Tray.put(done, "a"))
    }

    @Test
    fun `снятое изделие уходит с подноса а порядок остальных держится`() {
        val r = round(listOf("a", "a", "b"), tray = listOf("a", "x", "b"))
        assertEquals("снятие из середины", listOf("a", "b"), Tray.take(r, 1).tray)
        assertEquals("снятие с конца", listOf("a", "x"), Tray.take(r, 2).tray)
        assertEquals("снятие первого", listOf("x", "b"), Tray.take(r, 0).tray)
        assertEquals("слот за подносом", r, Tray.take(r, 3))
        assertEquals("слот меньше нуля", r, Tray.take(r, -1))

        val done = round(listOf("a"), tray = listOf("a"), results = listOf(true))
        assertEquals("снятие после последнего покупателя", done, Tray.take(done, 0))
    }

    // ---------- § 2. Отдача ----------

    @Test
    fun `неполный поднос не отдаётся и звезду не отнимает`() {
        val r = round(listOf("a", "a", "b"), tray = listOf("a", "a"))
        val g = Tray.give(r)
        assertFalse("неполный поднос приняли", g.accepted)
        assertFalse("неполный поднос обслужил покупателя", g.served)
        assertFalse("за неполный поднос дали звезду", g.star)
        assertEquals("неполный поднос назвал нехватку", emptyList<String>(), g.missing)
        assertEquals("неполный поднос назвал лишнее", emptyList<String>(), g.extra)
        assertEquals("неполная отдача изменила раунд", r, g.round)
        assertFalse("неполная отдача записана как неверная", g.round.missed)
    }

    @Test
    fun `верный поднос с первого раза даёт звезду`() {
        val g = Tray.give(round(listOf("a", "b"), tray = listOf("b", "a")))
        assertTrue("верный поднос не приняли", g.accepted)
        assertTrue("верный поднос не обслужил покупателя", g.served)
        assertTrue("за верный поднос с первого раза нет звезды", g.star)
        assertEquals("нехватка у верного подноса", emptyList<String>(), g.missing)
        assertEquals("лишнее у верного подноса", emptyList<String>(), g.extra)
        assertEquals("поднос не опустел к новому покупателю", emptyList<String>(), g.round.tray)
        assertFalse("метка неверной отдачи встала на верном подносе", g.round.missed)
        assertEquals("звёзды покупателей", listOf(true), g.round.results)
        assertEquals("звёзд за раунд", 1, g.round.stars)
        assertEquals("обслужено покупателей", 1, g.round.index)
        assertTrue("раунд одного покупателя не закончился", g.round.done)
    }

    @Test
    fun `неверный поднос называет нехватку и лишнее и оставляет совпавшие`() {
        val g = Tray.give(round(listOf("a", "a", "b"), tray = listOf("a", "x", "x")))
        assertTrue("неверный поднос не приняли", g.accepted)
        assertFalse("неверный поднос обслужил покупателя", g.served)
        assertFalse("за неверный поднос дали звезду", g.star)
        assertEquals("чего не хватает с кратностью", listOf("a", "b"), g.missing)
        assertEquals("что лишнее с кратностью", listOf("x", "x"), g.extra)
        assertEquals("на подносе остались не только совпавшие", listOf("a"), g.round.tray)
        assertTrue("неверная отдача не записана", g.round.missed)
        assertEquals("неверная отдача отняла звезду", emptyList<Boolean>(), g.round.results)
        assertEquals("неверная отдача сдвинула очередь", 0, g.round.index)

        // пара на каждое изделие заказа ищется по порядку, каждое изделие подноса парится один раз
        val mixed = Tray.give(round(listOf("a", "a", "b"), tray = listOf("b", "x", "a")))
        assertEquals("чего не хватает", listOf("a"), mixed.missing)
        assertEquals("что лишнее", listOf("x"), mixed.extra)
        assertEquals("совпавшие остались в своём порядке", listOf("b", "a"), mixed.round.tray)
    }

    @Test
    fun `верный поднос после неверного обслуживает покупателя без звезды`() {
        val first = Tray.give(round(listOf("a", "a", "b"), tray = listOf("a", "x", "x")))
        var r = first.round
        first.missing.forEach { r = Tray.put(r, it) }
        assertEquals("дособранный поднос", listOf("a", "a", "b"), r.tray)

        val g = Tray.give(r)
        assertTrue("дособранный поднос не приняли", g.accepted)
        assertTrue("дособранный поднос не обслужил покупателя", g.served)
        assertFalse("звезда за вторую попытку", g.star)
        assertEquals("звёзды покупателей", listOf(false), g.round.results)
        assertEquals("звёзд за раунд", 0, g.round.stars)
        assertFalse("метка неверной отдачи не снялась к новому покупателю", g.round.missed)
        assertEquals("поднос не опустел", emptyList<String>(), g.round.tray)
    }

    @Test
    fun `после последнего покупателя отдавать нечего`() {
        val done = round(listOf("a"), tray = listOf("a"), results = listOf(true))
        val g = Tray.give(done)
        assertEquals("отдача после последнего покупателя изменила раунд", done, g.round)
        assertFalse("отдачу после последнего покупателя приняли", g.accepted)
        assertFalse("отдача после последнего покупателя обслужила покупателя", g.served)
        assertFalse("отдача после последнего покупателя дала звезду", g.star)
        assertEquals("нехватка после последнего покупателя", emptyList<String>(), g.missing)
        assertEquals("лишнее после последнего покупателя", emptyList<String>(), g.extra)
    }

    // ---------- § 2. Подсказка Бори ----------

    /** m из § 2: пересечение подноса и заказа с кратностью. */
    private fun matched(order: List<String>, tray: List<String>): Int {
        val left = tray.toMutableList()
        var m = 0
        order.forEach { if (left.remove(it)) m++ }
        return m
    }

    /** Все заказы из size изделий трёх видов (одинаковые рядом, по порядку витрины). */
    private fun multisets(kinds: List<String>, size: Int): List<List<String>> =
        if (size == 0) {
            listOf(emptyList())
        } else {
            kinds.flatMapIndexed { i, k -> multisets(kinds.drop(i), size - 1).map { listOf(k) + it } }
        }

    /** Все подносы длины 0..max из изделий витрины. */
    private fun trays(max: Int): List<List<String>> {
        var level = listOf(emptyList<String>())
        val out = mutableListOf<List<String>>()
        repeat(max + 1) {
            out += level
            level = level.flatMap { t -> showcase.map { t + it } }
        }
        return out
    }

    @Test
    fun `подсказка добавляет нужное но заказ целиком не собирает`() {
        val kinds = showcase.dropLast(1)
        var checked = 0
        for (size in 1..4) {
            for (order in multisets(kinds, size)) {
                for (tray in trays(size)) {
                    val r = round(order, tray = tray)
                    val m = matched(order, tray)
                    for (n in 1..4) {
                        val out = Tray.hint(r, n)
                        checked++
                        val where = "заказ $order, поднос $tray, подсказок $n"
                        if (m == size) {
                            assertEquals("собранный поднос изменён подсказкой — $where", r, out)
                            continue
                        }
                        assertEquals("верных изделий после подсказки — $where", minOf(m + n, size - 1), matched(order, out.tray))
                        assertTrue("поднос переполнен после подсказки — $where, вышло ${out.tray}", out.tray.size <= size)
                        assertFalse("подсказка записала неверную отдачу — $where", out.missed)
                        assertEquals("подсказка тронула звёзды — $where", r.results, out.results)
                        assertEquals("подсказка тронула заказы — $where", r.orders, out.orders)
                    }
                }
            }
        }
        assertTrue("перебор подносов оказался пустым", checked > 1000)
    }

    @Test
    fun `подсказка молчит на собранном подносе при нуле и после последнего покупателя`() {
        val ready = round(listOf("a", "b"), tray = listOf("b", "a"))
        assertEquals("подсказка на собранном подносе", ready, Tray.hint(ready, 1))

        val half = round(listOf("a", "a", "b"), tray = listOf("x"))
        assertEquals("подсказок ноль", half, Tray.hint(half, 0))
        assertEquals("подсказок меньше нуля", half, Tray.hint(half, -3))

        val done = round(listOf("a", "a", "b"), tray = listOf("x"), results = listOf(true))
        assertEquals("подсказка после последнего покупателя", done, Tray.hint(done, 1))
    }

    @Test
    fun `подсказка дописывает изделия в конец подноса`() {
        val r = round(listOf("a", "a", "b"), tray = listOf("x"))
        val out = Tray.hint(r, 2)
        assertEquals("нужные изделия легли не в конец", listOf("x", "a", "a"), out.tray)
        assertEquals("подсказка собрала не столько, сколько просили", 2, matched(r.orders[0].items, out.tray))
    }

    @Test
    fun `подсказка снимает лишнее с конца полного подноса`() {
        val r = round(listOf("a", "a", "b"), tray = listOf("a", "x", "x"))
        val out = Tray.hint(r, 1)
        assertEquals("лишнее снято не с конца", listOf("a", "x", "a"), out.tray)
        assertEquals("после подсказки в подносе не два верных изделия", 2, matched(r.orders[0].items, out.tray))
    }

    @Test
    fun `подсказка работает и на подносе дособранном после неверной отдачи`() {
        var r = Tray.give(round(listOf("a", "a", "b"), tray = listOf("a", "x", "x"))).round
        assertTrue("неверная отдача не записана", r.missed)
        assertEquals("на подносе остались не только совпавшие", listOf("a"), r.tray)
        r = Tray.put(Tray.put(r, "x"), "x")
        assertEquals("поднос не заполнен лишним", listOf("a", "x", "x"), r.tray)

        val out = Tray.hint(r, 1)
        assertEquals("лишнее снято не с конца дособранного подноса", listOf("a", "x", "a"), out.tray)
        assertTrue("подсказка сняла метку неверной отдачи", out.missed)
        assertEquals("подсказка тронула звёзды", emptyList<Boolean>(), out.results)
    }

    // ---------- § 2 и § 4. Одинаковые подносы — одинаковая оплата ----------

    /** Раунд целиком верно с первого раза. */
    private fun playPerfect(r0: TrayRound): TrayRound {
        var r = r0
        while (!r.done) {
            r.orders[r.index].items.forEach { r = Tray.put(r, it) }
            r = Tray.give(r).also { assertTrue("верный поднос не приняли", it.star) }.round
        }
        return r
    }

    /** Раунд с одной неверной отдачей на каждого покупателя. */
    private fun playWithOneMiss(r0: TrayRound): TrayRound {
        var r = r0
        while (!r.done) {
            val o = r.orders[r.index]
            val wrong = r.menu.first { it !in o.items }
            (o.items.dropLast(1) + wrong).forEach { r = Tray.put(r, it) }
            val g = Tray.give(r)
            assertFalse("за неверный поднос дали звезду", g.star)
            r = g.round
            g.missing.forEach { r = Tray.put(r, it) }
            r = Tray.give(r).also { assertTrue("дособранный поднос не обслужил покупателя", it.served) }.round
        }
        return r
    }

    /** Раунд верно с первого раза, но с подсказкой Бори на каждом подносе. */
    private fun playWithHint(r0: TrayRound, n: Int): TrayRound {
        var r = r0
        while (!r.done) {
            r = Tray.hint(r, n)
            val left = r.orders[r.index].items.toMutableList()
            r.tray.forEach { left.remove(it) }
            left.forEach { r = Tray.put(r, it) }
            r = Tray.give(r).also { assertTrue("поднос с подсказкой не обслужил покупателя", it.served) }.round
        }
        return r
    }

    private fun totalFor(s: GameState, stars: Int): Int =
        town.finishShift(s, bakery, stars, 0).s1aOutcome().pay?.total ?: error("смена не назвала числа итога")

    @Test
    fun `одинаковые подносы дают одинаковую оплату при любом seed`() {
        val perfect = mutableSetOf<Int>()
        val missed = mutableSetOf<Int>()
        val hinted = mutableSetOf<Int>()
        (1L..20L).forEach { seed ->
            val s = stand(seed, n = 1)
            val r0 = town.trayRound(s, bakery) ?: error("нет раунда при seed $seed")
            perfect += totalFor(s, playPerfect(r0).stars)
            missed += totalFor(s, playWithOneMiss(r0).stars)
            hinted += totalFor(s, playWithHint(r0, J1Stand.rules.riddleHint).stars)
        }
        assertEquals("верный раунд платит по-разному при разных seed — $perfect", 1, perfect.size)
        assertEquals("раунд с одной неверной отдачей платит по-разному — $missed", 1, missed.size)
        assertEquals("подсказка Бори изменила оплату", perfect, hinted)
        assertNotEquals("раунд без промахов и раунд с промахами платят одинаково", perfect, missed)
    }

    // ---------- § 4. Конец смены на подносах ----------

    @Test
    fun `смена на подносах платит по монете за звезду`() {
        val s = S1aStand.planned(40, 20, 30)
        val zero = town.finishShift(s, bakery, 0, 0)
        assertEquals("зарплата без звёзд", listOf(LedgerEntry("Смена: ${J1Stand.job.title}", 6)), zero.s1aState().envelope)
        assertEquals("строка без звёзд", "6 за смену. ✉ +6 — $payLater", zero.s1aOutcome().line)
        assertEquals("числа итога без звёзд", ShiftPay(6, 0, 6), zero.s1aOutcome().pay)

        val three = town.finishShift(s, bakery, 3, 0)
        assertEquals("строка с тремя звёздами", "6 за смену + 3 за ★. ✉ +9 — $payLater", three.s1aOutcome().line)
        assertEquals("числа итога с тремя звёздами", ShiftPay(6, 3, 9), three.s1aOutcome().pay)
        assertEquals("зарплата с тремя звёздами", 9, three.s1aState().envelope.first().amount)

        val max = J1Stand.rules.shiftBonusMax
        assertEquals("все звёзды смены", ShiftPay(6, max, 6 + max), town.finishShift(s, bakery, max, 0).s1aOutcome().pay)

        val q = town.shiftQuote(s, bakery)
        assertEquals("верх диапазона котировки", 6 + max, q.top)
        assertEquals("строка котировки", "6–10 за смену", q.line)
        assertTrue("смена на подносах не оплачивается", q.paid)
    }

    @Test
    fun `смену на подносах нельзя закончить чужим счётом и бомбой`() {
        val s = S1aStand.planned(40, 20, 30)
        val customers = J1Stand.sizes(0, demo = true).size
        assertEquals("покупателей в смене не по потолку надбавки", J1Stand.rules.shiftBonusMax, customers)
        assertEquals(
            "звёзд ровно по числу покупателей",
            ShiftPay(6, customers, 6 + customers),
            town.finishShift(s, bakery, customers, 0).s1aOutcome().pay,
        )
        assertEquals("на одну звезду больше, чем покупателей", badShift, town.finishShift(s, bakery, customers + 1, 0).s1aRefusal())
        assertEquals("бомбочка на подносе", badShift, town.finishShift(s, bakery, 1, 1).s1aRefusal())
        assertEquals("счёт меньше нуля", badShift, town.finishShift(s, bakery, -1, 0).s1aRefusal())
        assertEquals("бомбы меньше нуля", badShift, town.finishShift(s, bakery, 1, -1).s1aRefusal())
        assertEquals("состояние после отказов", S1aStand.planned(40, 20, 30), s)
    }

    @Test
    fun `после лимита смен поднос закрыт`() {
        var s = S1aStand.planned(40, 20, 30)
        repeat(3) { s = town.finishShift(s, bakery, 1, 0).s1aState() }
        val q = town.shiftQuote(s, bakery)
        assertFalse("четвёртая смена оплачивается", q.paid)
        assertFalse("после лимита на подносы пускают", q.canPlay)
        assertEquals("строка после лимита", limit, q.line)
        assertEquals("отказ после лимита", limit, town.finishShift(s, bakery, 1, 0).s1aRefusal())
        assertEquals("рекорд звёзд изменился после отказа", 1, s.records[bakery])
    }

    @Test
    fun `смена на подносах сбрасывает загадку и не хвалит рекордом`() {
        val s = town.answerQuestion(S1aStand.planned(40, 20, 30), "q_budget_1", 0).s1aState()
        assertTrue("загадка не отмечена как заданная", s.riddleAsked)

        val r = town.finishShift(s, bakery, 2, 0)
        val after = r.s1aState()
        assertFalse("смена на подносах не сбросила загадку", after.riddleAsked)
        assertEquals("следующая загадка", "q_budget_2", town.nextQuestion(after)?.id)
        assertEquals("рекорд звёзд", 2, after.records[bakery])
        assertEquals("рекорд звёзд через bestStars", 2, town.bestStars(after, bakery))
        assertEquals("«Почему?»", listOf("Боря: 1 из 6 смен до уровня 2", wage), r.s1aOutcome().why)
        assertFalse("«Новый рекорд!» у подноса", "Новый рекорд!" in r.s1aOutcome().why)
        assertEquals("мастерство", 1, after.jobShifts[bakery])
        assertEquals("смен за неделю", 1, after.shiftsThisPeriod)
        assertEquals("строка дневника", "Заработали 8: «${J1Stand.job.title}»", after.diary.last().text)

        val weaker = town.finishShift(after, bakery, 1, 0)
        assertEquals("рекорд звёзд просел", 2, weaker.s1aState().records[bakery])
        assertFalse("«Новый рекорд!» у подноса", "Новый рекорд!" in weaker.s1aOutcome().why)
    }

    @Test
    fun `рекорд звёзд перезаписывает наследие Match3`() {
        // граничная запись профиля: 60 булочек Match3 в records пекарни
        val s = S1aStand.planned(40, 20, 30).copy(records = mapOf(bakery to 60))
        assertNull("наследие прочиталось как рекорд звёзд", town.bestStars(s, bakery))
        val after = town.finishShift(s, bakery, 2, 0).s1aState()
        assertEquals("наследие Match3 не перезаписано", 2, after.records[bakery])
        assertEquals("рекорд звёзд после смены", 2, town.bestStars(after, bakery))
    }

    @Test
    fun `потолок надбавки и верх диапазона берутся из правил`() {
        val s = S1aStand.planned(40, 20, 30)
        val engine = J1Stand.cappedTown
        assertEquals("потолок надбавки копии", 2, J1Stand.capped.town!!.rules.shiftBonusMax)
        assertEquals("sizes копии не тронуты", J1Stand.job.steps.first().sizes, J1Stand.capped.town!!.jobs.first { it.id == bakery }.steps.first().sizes)

        val q = engine.shiftQuote(s, bakery)
        assertEquals("верх диапазона", 8, q.top)
        assertEquals("строка котировки", "6–8 за смену", q.line)

        val r = engine.finishShift(s, bakery, 3, 0)
        assertEquals("надбавка выше потолка", ShiftPay(6, 2, 8), r.s1aOutcome().pay)
        assertEquals("строка", "6 за смену + 2 за ★. ✉ +8 — $payLater", r.s1aOutcome().line)
        assertEquals("зарплата", 8, r.s1aState().envelope.first().amount)
    }

    @Test
    fun `верная загадка у пекарни на подносах кладёт изделие а не бомбочку`() {
        val s = S1aStand.planned(40, 20, 30)
        val r = town.answerQuestion(s, "q_budget_1", 0)
        assertEquals("бомбочка вместо подсказки", s.bombs, r.s1aState().bombs)
        assertEquals("«Почему?»", listOf("Подсказка Бори — на поднос"), r.s1aOutcome().why)
        assertEquals("строка", "Верно! ${S1bStand.explanation("q_budget_1")}", r.s1aOutcome().line)
        assertTrue("загадка не отмечена как заданная", r.s1aState().riddleAsked)

        val wrong = town.answerQuestion(s, "q_budget_1", 1)
        assertEquals("«Почему?» неверного ответа", listOf("Вопрос вернётся позже"), wrong.s1aOutcome().why)
        assertEquals("бомбочка за неверный ответ", s.bombs, wrong.s1aState().bombs)
    }

    // ---------- § 1 (срез 1а). Нехватка подноса ----------

    @Test
    fun `нехватка подноса считается с кратностью и по порядку заказа`() {
        assertEquals(
            "нехватка на пустом подносе",
            listOf("b", "a", "a"), Tray.missing(round(listOf("b", "a", "a"))),
        )
        assertEquals(
            "нехватка идёт не в порядке заказа",
            listOf("b", "a"), Tray.missing(round(listOf("b", "a", "a"), tray = listOf("a"))),
        )
        assertEquals(
            "две одинаковых нехватки слиплись в одну",
            listOf("a", "a"), Tray.missing(round(listOf("a", "a", "b"), tray = listOf("b"))),
        )
        assertEquals(
            "лишнее на подносе спутало нехватку",
            listOf("a"), Tray.missing(round(listOf("a", "a", "b"), tray = listOf("x", "a", "b"))),
        )
        assertEquals(
            "нехватка собранного подноса",
            emptyList<String>(), Tray.missing(round(listOf("a", "b"), tray = listOf("b", "a"))),
        )

        val done = round(listOf("a"), tray = listOf("a"), results = listOf(true))
        assertTrue("раунд с обслуженным покупателем не закончен", done.done)
        assertEquals("нехватка после последнего покупателя", emptyList<String>(), Tray.missing(done))

        // то же правило, что называет отдача полного подноса (TOWN-J1-0 § 2)
        val full = round(listOf("a", "a", "b"), tray = listOf("a", "x", "x"))
        assertEquals("нехватка расходится с отдачей", Tray.give(full).missing, Tray.missing(full))
    }

    // ---------- § 1 (срез 1а). Значок «?» в раунде ----------

    /** Обслужить текущего покупателя верно: доложить недостающее и отдать. */
    private fun serve(r: TrayRound): TrayRound {
        var x = r
        val left = x.orders[x.index].items.toMutableList()
        x.tray.forEach { left.remove(it) }
        left.forEach { x = Tray.put(x, it) }
        return Tray.give(x).also { assertTrue("верный поднос не обслужил покупателя", it.served) }.round
    }

    /**
     * Стенд значка «?»: вторая смена пекарни настоящим путём (одна смена позади), двое покупателей
     * обслужены, поднос третьего пуст — на таком подносе подсказка на n и на n + 1 разная.
     */
    private fun riddleStand(): Pair<GameState, TrayRound> {
        val s = town.finishShift(S1aStand.planned(40, 20, 30, demo = false), bakery, 0, 0).s1aState()
        var r = town.trayRound(s, bakery) ?: error("нет раунда во второй смене")
        assertTrue("во второй смене раунд не обещает значок «?»", r.riddle)
        repeat(2) { r = serve(r) }
        assertEquals("обслужено покупателей", 2, r.index)
        assertFalse("смена кончилась раньше времени", r.done)
        assertTrue("поднос третьего покупателя не пуст", r.tray.isEmpty())
        return s to r
    }

    @Test
    fun `значок «?» есть после первого покупателя не в первой смене`() {
        val (s, r) = riddleStand()
        val q = town.riddleInRound(s, r)
        assertNotNull("значка «?» нет на стенде второй смены", q)
        assertEquals("значок «?» показал не ту загадку", town.nextQuestion(s)?.id, q?.id)
    }

    @Test
    fun `значка «?» нет до первого обслуженного покупателя`() {
        val (s, _) = riddleStand()
        val r = town.trayRound(s, bakery) ?: error("нет раунда во второй смене")
        assertEquals("покупателей обслужено", 0, r.index)
        assertTrue("остальные условия значка не выполнены", r.riddle && !r.done)
        assertNotNull("очередь загадок пуста", town.nextQuestion(s))
        assertNull("значок «?» у первого покупателя смены", town.riddleInRound(s, r))
    }

    @Test
    fun `значка «?» нет в самой первой смене работы`() {
        val s = S1aStand.planned(40, 20, 30, demo = false)
        val r = serve(town.trayRound(s, bakery) ?: error("нет раунда в первой смене"))
        assertFalse("первая смена обещает значок «?»", r.riddle)
        assertTrue("остальные условия значка не выполнены", r.index >= 1 && !r.done)
        assertNotNull("очередь загадок пуста", town.nextQuestion(s))
        assertNull("значок «?» в первой смене работы", town.riddleInRound(s, r))
    }

    @Test
    fun `значка «?» нет после последнего покупателя смены`() {
        val (s, r0) = riddleStand()
        var r = r0
        while (!r.done) r = serve(r)
        assertTrue("смена не закончилась", r.done)
        assertTrue("остальные условия значка не выполнены", r.riddle && r.index >= 1)
        assertNotNull("очередь загадок пуста", town.nextQuestion(s))
        assertNull("значок «?» после последнего покупателя", town.riddleInRound(s, r))
    }

    @Test
    fun `значка «?» нет когда загадка в этой смене уже была`() {
        val (s, r) = riddleStand()
        val q = town.nextQuestion(s) ?: error("нет загадки в очереди")
        val asked = town.answerQuestion(s, q.id, q.correct).s1aState()
        assertTrue("загадка не отмечена как заданная", asked.riddleAsked)
        assertTrue("остальные условия значка не выполнены", r.riddle && r.index >= 1 && !r.done)
        assertNull("второй значок «?» за одну смену", town.riddleInRound(asked, r))
    }

    @Test
    fun `значка «?» нет когда все загадки разобраны`() {
        val (s, r) = riddleStand()
        // граничная очередь: все пятнадцать загадок разобраны верно
        val solved = s.copy(riddles = S1bStand.quizIds.map { S1bStand.riddle(it, true) })
        assertNull("очередь загадок не опустела", town.nextQuestion(solved))
        assertTrue("остальные условия значка не выполнены", r.riddle && r.index >= 1 && !r.done)
        assertNull("значок «?» без единой загадки", town.riddleInRound(solved, r))
    }

    // ---------- § 1 (срез 1а). Ответ на загадку в раунде ----------

    @Test
    fun `верный ответ в раунде кладёт подсказку Бори на поднос`() {
        val (s, r) = riddleStand()
        val n = J1Stand.rules.riddleHint
        assertNotEquals("на стенде подсказка на $n и на ${n + 1} одинакова", Tray.hint(r, n), Tray.hint(r, n + 1))
        val q = town.riddleInRound(s, r) ?: error("на стенде нет значка «?»")

        val a: TrayAnswer = town.answerInRound(s, r, q.id, q.correct)
        val after = a.result.s1aState()
        assertEquals("подсказка легла не по правилу riddleHint", Tray.hint(r, n), a.round)
        assertNotEquals("поднос после верного ответа не изменился", r, a.round)
        assertEquals("звёзды раунда тронуты подсказкой", r.results, a.round.results)
        assertEquals("бомбочка вместо подсказки", s.bombs, after.bombs)
        assertEquals("строка", "Верно! ${q.explanation}", a.result.s1aOutcome().line)
        assertEquals("«Почему?»", listOf(hintWhy), a.result.s1aOutcome().why)
        assertTrue("загадка не отмечена как заданная", after.riddleAsked)
        assertEquals("ответ не записан", s.riddles + TaskResult(q.id, true, 0, s.period), after.riddles)
    }

    @Test
    fun `верный ответ когда класть нечего оставляет поднос как есть`() {
        val (s, base) = riddleStand()
        val order = base.orders[base.index].items
        val alien = base.menu.first { it !in order }
        val stands = listOf(
            "не хватает одного изделия, слот пуст" to order.dropLast(1),
            "не хватает одного изделия, поднос полон лишним" to (order.dropLast(1) + alien),
            "поднос собран верно" to order,
        )

        stands.forEach { (where, tray) ->
            var r = base
            tray.forEach { r = Tray.put(r, it) }
            assertEquals("$where: поднос собран не так", tray, r.tray)
            assertEquals("$where: подсказке есть что класть", r, Tray.hint(r, J1Stand.rules.riddleHint))
            val q = town.riddleInRound(s, r) ?: error("$where: на стенде нет значка «?»")

            val a = town.answerInRound(s, r, q.id, q.correct)
            val after = a.result.s1aState()
            assertEquals("$where: поднос изменился", r, a.round)
            assertEquals("$where: строка", "Верно! ${q.explanation}", a.result.s1aOutcome().line)
            assertEquals("$where: «Почему?»", listOf(hintIdle), a.result.s1aOutcome().why)
            assertTrue("$where: загадка не отмечена как заданная", after.riddleAsked)
            assertEquals("$where: ответ не записан", s.riddles + TaskResult(q.id, true, 0, s.period), after.riddles)
            assertEquals("$where: бомбочка вместо подсказки", s.bombs, after.bombs)
        }
    }

    @Test
    fun `неверный ответ в раунде поднос не трогает`() {
        val (s, r) = riddleStand()
        val q = town.riddleInRound(s, r) ?: error("на стенде нет значка «?»")
        assertNotEquals("на стенде подсказка ничего не кладёт", r, Tray.hint(r, J1Stand.rules.riddleHint))

        val a = town.answerInRound(s, r, q.id, (q.correct + 1) % q.options.size)
        val after = a.result.s1aState()
        assertEquals("поднос изменился после неверного ответа", r, a.round)
        assertEquals("строка", q.explanation, a.result.s1aOutcome().line)
        assertEquals("«Почему?»", listOf(laterWhy), a.result.s1aOutcome().why)
        assertEquals("ответ не записан", s.riddles + TaskResult(q.id, false, 0, s.period), after.riddles)
        assertTrue("загадка не отмечена как заданная", after.riddleAsked)
        assertEquals("бомбочка за неверный ответ", s.bombs, after.bombs)
    }

    @Test
    fun `вариант ответа за списком отказывает и раунд не трогает`() {
        val (s, r) = riddleStand()
        val q = town.riddleInRound(s, r) ?: error("на стенде нет значка «?»")
        listOf(-1, q.options.size).forEach { i ->
            val a = town.answerInRound(s, r, q.id, i)
            assertEquals("вариант $i", "Выбери ответ", a.result.s1aRefusal())
            assertEquals("вариант $i изменил раунд", r, a.round)
        }
        assertNotNull("отказ погасил значок «?»", town.riddleInRound(s, r))
    }

    @Test
    fun `ответ без значка «?» отказывает и раунд не трогает`() {
        val (s, r) = riddleStand()
        val q = town.riddleInRound(s, r) ?: error("на стенде нет значка «?»")

        fun refusal(where: String, state: GameState, round: TrayRound, id: String, option: Int) {
            val a = town.answerInRound(state, round, id, option)
            assertEquals(where, noRiddle, a.result.s1aRefusal())
            assertEquals("$where: раунд изменился", round, a.round)
        }

        val start = town.trayRound(s, bakery) ?: error("нет раунда во второй смене")
        refusal("до первого обслуженного покупателя", s, start, q.id, q.correct)

        val firstShift = S1aStand.planned(40, 20, 30, demo = false)
        val early = serve(town.trayRound(firstShift, bakery) ?: error("нет раунда в первой смене"))
        val q1 = town.nextQuestion(firstShift) ?: error("нет загадки в очереди")
        refusal("в первой смене работы", firstShift, early, q1.id, q1.correct)

        var done = r
        while (!done.done) done = serve(done)
        refusal("после последнего покупателя", s, done, q.id, q.correct)

        val other = S1bStand.quizIds.first { it != q.id }
        refusal("чужой вопрос", s, r, other, 0)

        val asked = town.answerQuestion(s, q.id, q.correct).s1aState()
        assertTrue("загадка не отмечена как заданная", asked.riddleAsked)
        refusal("вторая загадка за смену", asked, r, q.id, q.correct)
    }

    @Test
    fun `после верного ответа значок «?» гаснет до конца смены а в следующей приходит снова`() {
        val (s, r) = riddleStand()
        val q = town.riddleInRound(s, r) ?: error("на стенде нет значка «?»")
        val a = town.answerInRound(s, r, q.id, q.correct)
        val after = a.result.s1aState()
        assertNull("значок «?» остался сразу после ответа", town.riddleInRound(after, a.round))

        var played = a.round
        while (!played.done) played = serve(played)
        assertNull("значок «?» вернулся в той же смене", town.riddleInRound(after, played))

        // оплата смены — по звёздам покупателей: своей звезды подсказка не даёт
        val paid = town.finishShift(after, bakery, played.stars, 0)
        val next = paid.s1aState()
        assertEquals("надбавка не по звёздам", played.stars, paid.s1aOutcome().pay?.bonus)
        assertFalse("конец смены не сбросил загадку", next.riddleAsked)

        val third = serve(town.trayRound(next, bakery) ?: error("нет раунда в третьей смене"))
        assertNotNull("значок «?» не вернулся в новой смене", town.riddleInRound(next, third))
    }

    // ---------- Тексты ----------

    @Test
    fun `тексты подноса без стоп-слов и без рода`() {
        val s = S1aStand.planned(40, 20, 30)
        val texts = buildList {
            J1Stand.job.steps.forEach { st -> st.intro?.let { add(it) } }
            J1Stand.job.menu.forEach { add(it.title) }
            listOf(0, 1, 3, 5, 7).forEach { n ->
                town.trayRound(stand(1, n = n), bakery)?.intro?.let { add(it) }
            }
            add(town.shiftQuote(s, bakery).line)
            add(J1Stand.cappedTown.shiftQuote(s, bakery).line)
            listOf(0, 1, J1Stand.rules.shiftBonusMax).forEach { addAll(town.finishShift(s, bakery, it, 0).s1aTexts()) }
            addAll(J1Stand.cappedTown.finishShift(s, bakery, 3, 0).s1aTexts())
            addAll(town.answerQuestion(s, "q_budget_1", 0).s1aTexts())
            var spent = s
            repeat(3) { spent = town.finishShift(spent, bakery, 0, 0).s1aState() }
            add(town.shiftQuote(spent, bakery).line)
            addAll(town.finishShift(spent, bakery, 0, 0).s1aTexts())

            // строки ответа на загадку в раунде (§ 1): верно, «класть нечего», неверно и оба отказа
            val (rs, rr) = riddleStand()
            val q = town.riddleInRound(rs, rr) ?: error("на стенде нет значка «?»")
            addAll(town.answerInRound(rs, rr, q.id, q.correct).result.s1aTexts())
            addAll(town.answerInRound(rs, rr, q.id, (q.correct + 1) % q.options.size).result.s1aTexts())
            addAll(town.answerInRound(rs, rr, q.id, -1).result.s1aTexts())
            addAll(town.answerInRound(rs, rr, S1bStand.quizIds.first { it != q.id }, 0).result.s1aTexts())
            var ready = rr
            rr.orders[rr.index].items.forEach { ready = Tray.put(ready, it) }
            addAll(town.answerInRound(rs, ready, q.id, q.correct).result.s1aTexts())
        }
        assertTrue("строки загадки в раунде не попали в проверку", hintIdle in texts && noRiddle in texts)
        S1aChildText.check("поднос по заказу", texts)
    }
}
