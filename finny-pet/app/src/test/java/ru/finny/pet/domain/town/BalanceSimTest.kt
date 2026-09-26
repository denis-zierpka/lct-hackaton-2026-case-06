package ru.finny.pet.domain.town

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.pet.domain.Category
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.Goal
import ru.finny.pet.domain.ok

/**
 * Стенд симуляции «Городка» (docs/tasks/TOWN-S1c.md, ORACLE BalanceSimTest).
 * Каждая неделя проживается настоящими методами Town: tick → setPlan → confirmPlan →
 * visit → buyAt → deposit → finishShift → pass → sleep → wake. Руками не правится ничего:
 * события приходят сами, цены берутся из Prices, зарплата — из смен S1b.
 */
internal object S1cSimStand {
    val content = S1aStand.content
    val town = S1aStand.town
    val economy = S1aStand.economy
    val prices = S1aStand.prices
    val days: Int get() = S1aStand.days

    /** Живые события среза 1: town.events минус town.eventsOff (§0). */
    val liveEvents: List<EventDef>
        get() = content.town!!.events.filter { it.id !in content.town!!.eventsOff }

    fun scooter(): Goal = S1aStand.townGoal("goal_scooter")
}

/**
 * Одна прогонка стратегии. После КАЖДОГО шага проверяются инварианты S1a §8, кошелёк ≥ 0
 * и отсутствие тупика (у каши на рынке есть вариант оплаты, спать можно).
 */
internal class S1cSimRun(val name: String) {
    private val town = S1cSimStand.town

    var state: GameState = S1aStand.profile(demo = false)
        private set

    /** Всё, что движок сказал ребёнку за прогонку, — для стоп-листа §8. */
    val texts = mutableListOf<String>()
    val walletAtWeekStart = mutableListOf<Int>()
    val savingsAfterPlan = mutableListOf<Int>()
    val weekScores = mutableListOf<Int>()
    var deadlockChecks = 0
        private set
    var steps = 0
        private set

    init {
        assertStep("$name: новый профиль")
        // VM зовёт tick после создания профиля (§2): так на пустом профиле приходит П1
        record("$name: первый запуск", town.tick(state))
    }

    /** Ход, который мог бы отказать: отказ роняет прогонку строкой самого движка. */
    fun act(where: String, r: TownResult) {
        when (r) {
            is TownResult.Refused -> error("$name, $where: движок отказал — «${r.line}»")
            is TownResult.Done -> record("$name: $where", r.outcome)
        }
    }

    fun record(where: String, o: TownOutcome) {
        texts += o.line
        texts += o.why
        texts += o.eventResults.map { it.line }
        state = o.state
        steps++
        assertStep(where)
    }

    fun enter(placeId: String) = record("$name: вход в место $placeId", town.visit(state, placeId))

    /** Мечту выбирает Economy — Town её не оборачивает (S1a §6). */
    fun choose(goal: Goal) {
        state = S1cSimStand.economy.chooseGoal(state, goal).ok()
        assertStep("$name: мечта ${goal.title}")
    }

    /** Неделя целиком: раскладка, три дня со сном и утром, итог недели третьим сном. */
    fun week(plan: (GameState) -> Triple<Int, Int, Int>, day: (S1cSimRun, Int) -> Unit) {
        val w = state.period
        walletAtWeekStart += state.balance
        val (m, o, sv) = plan(state)
        state = S1cSimStand.economy.setPlan(state, m, o, sv).ok()
        act("неделя $w, «Готово» $m — $o — $sv", town.confirmPlan(state))
        savingsAfterPlan += state.savings
        for (d in 1..S1cSimStand.days) {
            day(this, d)
            act("неделя $w, сон дня $d", town.sleep(state))
            act("неделя $w, утро после дня $d", town.wake(state))
        }
        weekScores += state.history.last().score
    }

    private fun assertStep(where: String) {
        val s = state
        assertS1aInvariants(where, s)
        assertTrue("$where: кошелёк ${s.balance} ушёл в минус", s.balance >= 0)
        if (s.plan.confirmed && !s.asleep) {
            deadlockChecks++
            assertTrue(
                "$where: у каши на рынке не осталось ни одного варианта — тупик",
                town.quote(s, "food_porridge", "shop_market").options.isNotEmpty(),
            )
            assertTrue("$where: спать нельзя — тупик", town.sleep(s) is TownResult.Done)
        }
    }
}

/** «Всё по плану» (§15): корм у реки, мыло у Фомы, одна хотелка, взнос из «Хочу», три смены. */
private fun s1cByPlanDay(r: S1cSimRun, d: Int) {
    val town = S1cSimStand.town
    if (d == 1) {
        r.enter("market")
        r.act("корм у реки", town.buyAt(r.state, "food_basic", "shop_market", Source.NEED))
        r.enter("foma")
        r.act("мыло у Фомы", town.buyAt(r.state, "care_soap", "shop_foma", Source.NEED))
        if (r.state.period == 1) {
            r.choose(S1cSimStand.scooter())
            r.act("взнос 10 из «Хочу»", town.deposit(r.state, Source.WANT, 10))
            r.act("шарик у реки", town.buyAt(r.state, "fun_balloon", "shop_market", Source.WANT))
        } else {
            r.act("мороженое у реки", town.buyAt(r.state, "fun_icecream", "shop_market", Source.WANT))
        }
        val goal = r.state.goal
        if (goal != null && r.state.savings >= goal.price) r.act("мечта сбылась", town.achieveGoal(r.state))
    }
    if (d == 2) {
        // событие решается поступком: мимо витрин проходят кнопкой «Пройти мимо»
        town.activeEvents(r.state).filter { def -> def.outcomes.any { it.fact == Fact.Skip } }
            .forEach { def -> r.act("«Пройти мимо» ${def.id}", town.pass(r.state, def.id)) }
    }
    r.act("смена дня $d", town.finishShift(r.state, if (d == 2) "job_market" else "job_bakery", 0, 0))
}

/** «Всё на хотелки»: нужное по минимуму, остальное в «Хочу», каждый день — самая дорогая хотелка. */
private fun s1cByWantsDay(r: S1cSimRun, d: Int) {
    val town = S1cSimStand.town
    if (d == 1) {
        r.enter("market")
        r.act("каша у реки", town.buyAt(r.state, "food_porridge", "shop_market", Source.NEED))
        r.enter("foma")
        r.act("мыло у Фомы", town.buyAt(r.state, "care_soap", "shop_foma", Source.NEED))
    }
    s1cDearestWant(r.state)?.let { (shop, item) ->
        r.act("самая дорогая хотелка по карману — $item", town.buyAt(r.state, item, shop, Source.WANT))
    }
    r.act("смена дня $d", town.finishShift(r.state, if (d == 2) "job_market" else "job_bakery", 0, 0))
}

/** «Копилка максимум»: нужное по минимуму, «Хочу» пустая, остальное в копилку. */
private fun s1cBySavingsDay(r: S1cSimRun, d: Int) {
    val town = S1cSimStand.town
    if (d == 1) {
        r.enter("market")
        r.act("каша у реки", town.buyAt(r.state, "food_porridge", "shop_market", Source.NEED))
        r.enter("foma")
        r.act("мыло у Фомы", town.buyAt(r.state, "care_soap", "shop_foma", Source.NEED))
        if (r.state.period == 1) r.choose(S1cSimStand.scooter())
        val goal = r.state.goal
        if (goal != null && r.state.savings >= goal.price) r.act("мечта сбылась", town.achieveGoal(r.state))
    }
    r.act("смена дня $d", town.finishShift(r.state, if (d == 2) "job_market" else "job_bakery", 0, 0))
}

/** Самая дорогая хотелка обеих полок, на которую хватает банки «Хочу»; при равной цене — первая по полкам. */
private fun s1cDearestWant(s: GameState): Pair<String, String>? =
    listOf("shop_market", "shop_foma")
        .flatMap { shop -> S1cSimStand.prices.shelf(s, shop).map { shop to it } }
        .filter { it.second.item.category == Category.OPTIONAL && it.second.price <= s.jarWant }
        .maxByOrNull { it.second.price }
        ?.let { it.first to it.second.item.id }

private fun s1cByPlanRun(weeks: Int): S1cSimRun {
    val r = S1cSimRun("всё по плану")
    repeat(weeks) { r.week({ s -> if (s.period == 1) Triple(40, 20, 30) else Triple(40, 30, 40) }, ::s1cByPlanDay) }
    return r
}

private fun s1cByWantsRun(weeks: Int): S1cSimRun {
    val r = S1cSimRun("всё на хотелки")
    repeat(weeks) { r.week({ s -> Triple(30, s.balance - 30, 0) }, ::s1cByWantsDay) }
    return r
}

private fun s1cBySavingsRun(weeks: Int): S1cSimRun {
    val r = S1cSimRun("копилка максимум")
    repeat(weeks) { r.week({ s -> Triple(30, 0, s.balance - 30) }, ::s1cBySavingsDay) }
    return r
}

/**
 * Три детерминированные стратегии на 5 и 10 недель настоящими методами Town
 * (TOWN-S1c ORACLE BalanceSimTest): деньги, тупик, сценарий §15 и наклейки живых событий.
 */
class BalanceSimTest {

    private val town = S1cSimStand.town
    private val prices = S1cSimStand.prices

    // ---------- Сценарий §15: «Самокат» к неделе 4 ----------

    @Test
    fun `стратегия «всё по плану» за пять недель копит на «Самокат» и получает его на неделе 4`() {
        val r = s1cByPlanRun(5)

        // кошелёк недели 3 меньше на 10, чем недели 2 плюс конверт: на неделе 3 корм подорожал до 30 (П3)
        assertEquals("кошелёк в начале каждой недели", listOf(100, 140, 178, 206, 246), r.walletAtWeekStart)
        assertEquals("копилка сразу после «Готово»", listOf(30, 80, 120, 160, 50), r.savingsAfterPlan)
        assertTrue("к плану недели 4 на «Самокат» 150 не накопилось", r.savingsAfterPlan[3] >= 150)
        assertEquals("рост за каждую неделю", listOf(3, 3, 3, 3, 3), r.weekScores)

        assertEquals("сбывшиеся мечты", listOf("goal_scooter"), r.state.achievedGoals.map { it.id })
        assertEquals(
            "«Самокат» сбылся не на неделе 4",
            4,
            r.state.diary.first { it.text.startsWith("Мечта сбылась") }.period,
        )
        assertEquals("копилка в конце пятой недели", 50, r.state.savings)
        assertEquals("неделя после пяти недель", 6, r.state.period)
        assertEquals("рост питомца", 15, r.state.pet!!.growth)
        assertEquals("хотелки-вещи в сундуке", emptyList<String>(), r.state.owned)

        // «Пройти мимо» на неделе 3: распродажа робота и «Мечта почти твоя» решены поступком
        assertTrue("исход «Пройти мимо» у распродажи", "Скидка — это дешевле, а не нужнее." in r.texts)
        assertTrue("исход «Пройти мимо» у витрины мечты", "До мечты чуть-чуть — копилка цела." in r.texts)
        assertTrue("наклейка распродажи", "st_robot_sale" in r.state.stickers)
        assertTrue("наклейка «Мечта почти твоя»", "st_almost" in r.state.stickers)
    }

    // ---------- Десять недель тремя стратегиями ----------

    @Test
    fun `три стратегии проходят десять недель без минуса и без тупика`() {
        listOf(s1cByPlanRun(10), s1cByWantsRun(10), s1cBySavingsRun(10)).forEach { r ->
            assertEquals("${r.name}: неделя после десяти недель", 11, r.state.period)
            assertEquals("${r.name}: итогов недели в истории", 10, r.state.history.size)
            assertEquals("${r.name}: недель с оценкой", 10, r.weekScores.size)
            assertTrue("${r.name}: шагов слишком мало — прогонка не состоялась", r.steps > 100)
            assertTrue(
                "${r.name}: проверка тупика не выполнялась ни разу (${r.deadlockChecks})",
                r.deadlockChecks > 50,
            )
            assertTrue("${r.name}: кошелёк в минусе", r.state.balance >= 0)
            assertTrue("${r.name}: еда и уход не куплены ни разу", r.state.history.all { it.mandatoryCovered })
            assertTrue("${r.name}: событие осталось висеть после конца недели", r.state.events.all { it.status == EventStatus.DONE })
        }
    }

    @Test
    fun `три стратегии за пять недель приходят к разным итогам`() {
        val byPlan = s1cByPlanRun(5)
        val byWants = s1cByWantsRun(5)
        val bySavings = s1cBySavingsRun(5)

        assertEquals("рост «всё по плану»", 15, byPlan.state.pet!!.growth)
        assertEquals("рост «всё на хотелки»", 10, byWants.state.pet!!.growth)
        assertEquals("рост «копилка максимум»", 15, bySavings.state.pet!!.growth)
        assertEquals("оценки недель «всё на хотелки»", listOf(2, 2, 2, 2, 2), byWants.weekScores)

        assertEquals("копилка «всё на хотелки»", 0, byWants.state.savings)
        assertTrue(
            "копилка «копилка максимум» ${bySavings.state.savings} не обогнала «всё по плану» ${byPlan.state.savings}",
            bySavings.state.savings > byPlan.state.savings,
        )
        assertEquals("мечты «всё на хотелки»", emptyList<String>(), byWants.state.achievedGoals.map { it.id })
        assertEquals(
            "«копилка максимум» получила «Самокат» не на неделе 2",
            2,
            bySavings.state.diary.first { it.text.startsWith("Мечта сбылась") }.period,
        )

        assertTrue("палатка не куплена", "fun_tent" in byWants.state.owned)
        assertTrue("робот не куплен", "fun_robot" in byWants.state.owned)
        assertEquals("сундук «копилка максимум»", emptyList<String>(), bySavings.state.owned)

        assertNotEquals(
            "«всё по плану» и «всё на хотелки» пришли к одному итогу",
            byPlan.state.savings to byPlan.state.owned,
            byWants.state.savings to byWants.state.owned,
        )
        assertNotEquals(
            "«копилка максимум» и «всё по плану» пришли к одной копилке",
            byPlan.state.savings,
            bySavings.state.savings,
        )
    }

    // ---------- Неделя 1: всё сразу купить нельзя (ТЗ 2.1) ----------

    @Test
    fun `на неделе 1 все хотелки обеих полок купить нельзя`() {
        val s = S1aStand.planned(0, 100, 0, demo = false)
        val shelf = listOf("shop_market", "shop_foma")
            .flatMap { shop -> prices.shelf(s, shop).map { shop to it } }
            .filter { it.second.item.category == Category.OPTIONAL }

        assertEquals("хотелок на полках недели 1", 12, shelf.size)
        assertEquals("цена всех хотелок недели 1", 288, shelf.sumOf { it.second.price })
        assertTrue("кошелёк ${s.balance} покрывает все хотелки сразу", shelf.sumOf { it.second.price } > s.balance)
        assertEquals("весь конверт в «Хочу»", 100, s.jarWant)

        var st = s
        var bought = 0
        val refusals = mutableListOf<String>()
        shelf.sortedBy { it.second.price }.forEach { (shop, si) ->
            when (val r = town.buyAt(st, si.item.id, shop, Source.WANT)) {
                is TownResult.Done -> { st = r.outcome.state; bought++ }
                is TownResult.Refused -> refusals += r.line
            }
        }
        assertEquals("куплено хотелок с самых дешёвых", 6, bought)
        assertEquals("хотелок осталось на полках", 6, refusals.size)
        assertEquals("«Хочу» после скупки", 22, st.jarWant)
        assertTrue("кошелёк ушёл в минус", st.balance >= 0)
        assertS1aInvariants("после скупки хотелок", st)
        S1aChildText.check("отказы кассы на скупке хотелок", refusals)
    }

    // ---------- Наклейки живых событий ----------

    @Test
    fun `наклейки всех живых событий собираются в объединении трёх стратегий`() {
        assertEquals(
            "живые события среза 1",
            listOf(
                "p1_list", "p3_price_up", "c1_robot_sale", "c3_almost",
                "pk1_cheaper_food", "pk1_cheaper_soap", "pk3_super_food",
                "job_bakery_help", "job_market_help",
            ),
            S1cSimStand.liveEvents.map { it.id },
        )

        val byPlan = s1cByPlanRun(5)
        val byWants = s1cByWantsRun(5)
        val bySavings = s1cBySavingsRun(5)
        val union = (byPlan.state.stickers + byWants.state.stickers + bySavings.state.stickers).toSet()
        val wanted = S1cSimStand.liveEvents.mapNotNull { it.sticker }.toSet()

        assertEquals("наклеек живых событий", 8, wanted.size)
        assertEquals("наклейки живых событий, которых не собрала ни одна стратегия", emptySet<String>(), wanted - union)
        assertTrue("наклейка заказа пекарни не за смену", "st_job_bakery" in byPlan.state.stickers)
        assertTrue("наклейка заказа Марты не за смену", "st_job_market" in byPlan.state.stickers)

        // С3 приходит только при полной копилке, С1 — только пока робота нет дома
        assertTrue("«Мечта почти твоя» не пришла при копилке 120 из 150", "st_almost" in byPlan.state.stickers)
        assertTrue("распродажа робота не пришла", "st_robot_sale" in byPlan.state.stickers)
        assertFalse("распродажа робота пришла, хотя робот уже дома", "st_robot_sale" in byWants.state.stickers)
        assertFalse("«Мечта почти твоя» пришла при пустой копилке", "st_almost" in byWants.state.stickers)
    }

    @Test
    fun `выключенные события не приходят ни в одной стратегии`() {
        val off = S1cSimStand.content.town!!.eventsOff
        assertEquals("выключено событий", 8, off.size)

        val seen = listOf(s1cByPlanRun(5), s1cByWantsRun(5), s1cBySavingsRun(5))
            .flatMap { r -> r.state.events.map { it.id } }
            .toSet()
        assertTrue("ни одно событие вообще не пришло", seen.isNotEmpty())
        assertEquals("выключенные события пришли", emptySet<String>(), seen intersect off.toSet())
    }

    // ---------- Тексты ----------

    @Test
    fun `тексты трёх стратегий без стоп-слов и без рода`() {
        listOf(s1cByPlanRun(5), s1cByWantsRun(5), s1cBySavingsRun(5)).forEach { r ->
            S1aChildText.check("стратегия «${r.name}»", r.texts)
        }
    }
}
