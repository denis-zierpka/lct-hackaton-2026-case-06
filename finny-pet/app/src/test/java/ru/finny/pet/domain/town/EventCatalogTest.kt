package ru.finny.pet.domain.town

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.pet.domain.Content
import ru.finny.pet.domain.Economy
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.Goal
import ru.finny.pet.domain.Need
import ru.finny.pet.domain.TestContent
import ru.finny.pet.domain.ok

/**
 * Стенд событий «Городка» (docs/tasks/TOWN-S1c.md).
 * Состояния строятся настоящим путём игрока: newGame → createPet → setPlan → confirmPlan →
 * visit / buyAt / pass / finishShift → sleep → wake → endWeek. Руками ничего не дописывается.
 */
internal object S1cStand {
    val content: Content = TestContent.content
    val town = Town(content)
    val economy = Economy(content)
    val prices = Prices(content)
    val days: Int get() = content.town!!.rules.daysPerWeek

    fun profile(demo: Boolean = false): GameState =
        economy.createPet(economy.newGame(demo), "Финни", "cat", "orange").ok()

    /** Свежий профиль, которому первая проверка уже принесла «Список на неделю». */
    fun started(demo: Boolean = false): GameState = town.tick(profile(demo)).state

    fun planned(s: GameState, m: Int, o: Int, sv: Int): GameState =
        town.confirmPlan(economy.setPlan(s, m, o, sv).ok()).s1aState()

    /** Ночь и следующее утро: день недели +1. */
    fun nextDay(s: GameState): GameState = town.wake(town.sleep(s).s1aState()).s1aState()

    /** Неделя целиком: дни, итог недели (третий сон) и утро новой недели. */
    fun finishWeek(s: GameState): GameState {
        var st = s
        repeat(days - 1) { st = nextDay(st) }
        st = town.sleep(st).s1aState()
        return town.wake(st).s1aState()
    }

    /** Дожить до недели n, каждую неделю раскладывая монеты одинаково и ничего не покупая. */
    fun atWeek(n: Int, m: Int = 10, o: Int = 0, sv: Int = 0): GameState {
        var s = started()
        while (s.period < n) s = finishWeek(planned(s, m, o, sv))
        return s
    }

    fun activeIds(s: GameState): List<String> = town.activeEvents(s).map { it.id }

    fun eventOf(s: GameState, id: String): EventState = s.events.last { it.id == id }

    fun texts(o: TownOutcome): List<String> = listOf(o.line) + o.why + o.eventResults.map { it.line }
}

/** Тестовый контент: реальный «Городок» с подменённым списком событий (эффекты и ошибки контента). */
internal object S1cFixture {
    private val base: Content = TestContent.content
    private val baseTown: TownContent = base.town ?: error("content.json: нет ключа town")

    fun content(vararg events: EventDef): Content =
        base.copy(town = baseTown.copy(events = events.toList(), eventsOff = emptyList()))

    fun town(vararg events: EventDef): Town = Town(content(*events))

    /** Событие-заготовка; по умолчанию приходит на неделе 9, то есть само в тестах не приходит. */
    fun event(
        id: String,
        arrives: Arrives = Arrives(week = 9),
        place: String? = null,
        triggers: List<Trigger> = emptyList(),
        requires: List<Condition> = emptyList(),
        setup: List<EventEffect> = emptyList(),
        outcomes: List<EventOutcome> = emptyList(),
        default: EventDefault? = null,
        sticker: String? = null,
        demo: Demo = Demo(),
    ): EventDef = EventDef(
        id = id, kind = EventKind.ASK, title = "Проверка $id", place = place, arrives = arrives,
        triggers = triggers, requires = requires, setup = setup, intro = "Проверка $id",
        outcomes = outcomes, default = default, sticker = sticker, demo = demo,
    )
}

/**
 * Каталог событий «Городка» (TOWN-S1c §1–§8): приход, окна, сопоставление фактов,
 * эффекты, доска и демо-запуск. Семь живых событий проходят верным и ошибочным путём.
 */
class EventCatalogTest {

    private val town = S1cStand.town
    private val economy = S1cStand.economy
    private val night = "Сейчас ночь — сначала проснёмся"
    private val noPet = "Сначала создай питомца"

    // ---------- состояния-заготовки (только настоящие действия) ----------

    /** Неделя 2, день 2, монеты разложены, распродажа ещё не пришла. */
    private fun c1Ready(): GameState {
        var s = S1cStand.started()
        s = S1cStand.planned(s, 10, 0, 50)
        s = S1cStand.finishWeek(s)
        s = S1cStand.planned(s, 20, 40, 0)
        return S1cStand.nextDay(s)
    }

    /** То же, но с мечтой «Домик-палатка» за 60 и копилкой 50 — для «Мечта почти твоя». */
    private fun c3Ready(): GameState {
        var s = S1cStand.started()
        s = S1cStand.planned(s, 10, 0, 50)
        s = S1cStand.finishWeek(s)
        s = S1cStand.planned(s, 20, 40, 0)
        s = town.makeGoal(s, "fun_tent").s1aState()
        return S1cStand.nextDay(s)
    }

    /** Неделя 2, плакат «Супер-корм» уже на рынке. */
    private fun pk3Ready(): GameState {
        var s = S1cStand.started()
        s = S1cStand.planned(s, 30, 0, 0)
        s = S1cStand.finishWeek(s)
        s = S1cStand.planned(s, 40, 20, 30)
        return town.visit(s, "market").state
    }

    private fun startRefusal(requires: List<Condition>, s: GameState): String =
        S1cFixture.town(S1cFixture.event("fx_case", requires = requires)).startEvent(s, "fx_case").s1aRefusal()

    // ---------- §2. Приход ----------

    @Test
    fun `первая проверка приносит список на неделю`() {
        val s0 = S1cStand.profile()
        val o = town.tick(s0)
        assertEquals("пришедшие события", listOf("p1_list"), o.arrived)
        assertEquals("у проверки нет своей строки", "", o.line)
        assertTrue("проверка ничего не засчитывает", o.eventResults.isEmpty())
        assertTrue("у списка нет эффектов", o.effects.isEmpty())
        assertEquals(
            "ход события",
            EventState("p1_list", EventStatus.ACTIVE, null, null, 1, 1),
            S1cStand.eventOf(o.state, "p1_list"),
        )
        assertEquals("доска активных", listOf("p1_list"), S1cStand.activeIds(o.state))
    }

    @Test
    fun `ночью событие не приходит а утром приходит`() {
        var s = S1cStand.profile()
        s = S1cStand.planned(s, 30, 0, 0)
        repeat(S1cStand.days - 1) { s = S1cStand.nextDay(s) }
        val nightState = town.sleep(s).s1aState() // третий сон — итог недели, наступает ночь
        assertTrue("после итога недели ночь", nightState.asleep)
        assertEquals("новая неделя", 2, nightState.period)
        val asleep = town.tick(nightState)
        assertTrue("ночью событие не приходит", asleep.arrived.isEmpty())
        assertTrue("ночью ход события не заводится", asleep.state.events.none { it.id == "p1_list" })
        val morning = town.wake(nightState).s1aOutcome()
        assertEquals("утром список приходит", listOf("p1_list"), morning.arrived)
    }

    @Test
    fun `выключенное событие не приходит и его нигде нет`() {
        var s = S1cStand.started()
        s = S1cStand.planned(s, 30, 0, 0)
        s = S1cStand.finishWeek(s)
        assertEquals("неделя праздника у Тоши", 2, s.period)
        assertEquals("день", 1, s.day)
        val o = town.tick(s)
        assertTrue("выключенное событие не приходит: ${o.arrived}", o.arrived.isEmpty())
        assertTrue("ход выключенного события не заводится", o.state.events.none { it.id == "p2_party" })
        assertEquals("Такого события сейчас нет", town.pass(s, "p2_party").s1aRefusal())
        val demo = S1cStand.planned(S1cStand.profile(demo = true), 40, 20, 30)
        assertEquals("Такого события нет", town.startEvent(demo, "p2_party").s1aRefusal())
        assertTrue("выключенного нет на демо-доске", town.demoBoard(demo).none { it.id == "p2_party" })
    }

    @Test
    fun `демо-доска показывает все живые события и только в демо`() {
        val demo = S1cStand.profile(demo = true)
        assertEquals(
            listOf(
                "p1_list", "p3_price_up", "c1_robot_sale", "c3_almost",
                "pk1_cheaper_food", "pk1_cheaper_soap", "pk3_super_food",
            ),
            town.demoBoard(demo).map { it.id },
        )
        assertTrue("в обычной игре доски нет", town.demoBoard(S1cStand.profile()).isEmpty())
    }

    @Test
    fun `распродажа и мечта приходят только в лавку Фомы`() {
        val s = c3Ready()
        assertTrue("плановая проверка события по триггеру не приносит", town.tick(s).arrived.isEmpty())
        assertEquals("на рынке своё событие", listOf("pk3_super_food"), town.visit(s, "market").arrived)
        assertEquals("у Фомы своё", listOf("c3_almost", "c1_robot_sale"), town.visit(s, "foma").arrived)
    }

    @Test
    fun `распродажа ждёт своего дня недели`() {
        var s = S1cStand.started()
        s = S1cStand.planned(s, 10, 0, 50)
        s = S1cStand.finishWeek(s)
        s = S1cStand.planned(s, 20, 40, 0)
        s = town.makeGoal(s, "fun_tent").s1aState()
        assertEquals("первый день недели", 1, s.day)
        assertEquals("в первый день распродажи ещё нет", listOf("c3_almost"), town.visit(s, "foma").arrived)
        val day2 = S1cStand.nextDay(s)
        assertEquals("во второй день приходит", listOf("c3_almost", "c1_robot_sale"), town.visit(day2, "foma").arrived)
    }

    @Test
    fun `подорожание корма приходит только на третьей неделе`() {
        val w2 = S1cStand.planned(S1cStand.atWeek(2), 20, 10, 0)
        assertTrue("на второй неделе подорожания нет", town.visit(w2, "market").arrived.none { it == "p3_price_up" })
        val w3 = S1cStand.planned(S1cStand.atWeek(3), 20, 10, 0)
        assertEquals("порядок одного прохода", listOf("pk3_super_food", "p3_price_up"), town.visit(w3, "market").arrived)
    }

    @Test
    fun `за день приходит только одно событие без триггера`() {
        val skip = listOf(EventOutcome(Fact.Skip, Verdict.OK, "Пока пройдём мимо."))
        val fx = S1cFixture.town(
            S1cFixture.event("fx_first", arrives = Arrives(1, 1), outcomes = skip),
            S1cFixture.event("fx_second", arrives = Arrives(1, 1), outcomes = skip),
        )
        val day1 = fx.tick(S1cStand.profile())
        assertEquals("первое заняло лимит дня", listOf("fx_first"), day1.arrived)
        val planned = fx.confirmPlan(economy.setPlan(day1.state, 30, 0, 0).ok()).s1aState()
        assertEquals("лимит держится весь день", listOf("fx_first"), fx.activeEvents(planned).map { it.id })
        val day2 = fx.wake(fx.sleep(planned).s1aState()).s1aOutcome()
        assertEquals("утром приходит второе", listOf("fx_second"), day2.arrived)
    }

    @Test
    fun `событие по триггеру приходит сверх дневного лимита`() {
        val s = S1cStand.started()
        assertEquals("лимит дня занят списком", 1, s.events.size)
        val o = town.confirmPlan(economy.setPlan(s, 30, 0, 0).ok()).s1aOutcome()
        assertEquals("сравнение цен приходит по триггеру", listOf("pk1_cheaper_food"), o.arrived)
    }

    @Test
    fun `событие без исходов сразу закрывается и дарит наклейку`() {
        val fx = S1cFixture.town(
            S1cFixture.event(
                "fx_windfall", arrives = Arrives(1, 1),
                setup = listOf(EventEffect.Note("подарок")), sticker = "st_granny",
            ),
        )
        val o = fx.tick(S1cStand.profile())
        assertEquals(listOf("fx_windfall"), o.arrived)
        assertEquals(
            EventState("fx_windfall", EventStatus.DONE, null, -1, 1, 1),
            o.state.events.last { it.id == "fx_windfall" },
        )
        assertTrue("наклейка сразу", "st_granny" in o.state.stickers)
        assertTrue("на доске его нет", fx.activeEvents(o.state).isEmpty())
        assertEquals("заметка из setup", listOf("подарок"), o.state.notes)
    }

    @Test
    fun `событие в закрытом месте не приходит и не начинается`() {
        val fx = S1cFixture.town(
            S1cFixture.event(
                "fx_park", arrives = Arrives(1, 1), place = "park",
                outcomes = listOf(EventOutcome(Fact.Skip, Verdict.OK, "Пока пройдём мимо.")),
            ),
        )
        val demo = S1cStand.profile(demo = true)
        assertTrue("закрытое место события не приносит", fx.tick(demo).arrived.isEmpty())
        assertTrue("на демо-доске его нет", fx.demoBoard(demo).isEmpty())
        assertEquals("Место откроет мечта", fx.startEvent(demo, "fx_park").s1aRefusal())
    }

    // ---------- §3. Наблюдения и сопоставление: П1 ----------

    @Test
    fun `план ровно на список — верный путь`() {
        val s = S1cStand.started()
        val o = town.confirmPlan(economy.setPlan(s, 30, 0, 0).ok()).s1aOutcome()
        val r = o.eventResults.single()
        assertEquals("p1_list", r.eventId)
        assertEquals(Verdict.GOOD, r.verdict)
        assertEquals("Посмотрели цены заранее — на корм и мыло хватит.", r.line)
        assertEquals("st_list", r.sticker)
        assertTrue("у верного пути нет восстановления", r.recovery.isEmpty())
        assertEquals("строка исхода становится строкой действия", r.line, o.line)
        assertEquals(
            listOf(
                "План готов! Запас на всякий случай: 70",
                "Сначала откладываем, потом тратим — так мечта ближе",
                "Запас — на нужное и на всякий случай, не на «хочу»",
            ),
            o.why,
        )
        assertTrue("наклейка в дневнике", "st_list" in o.state.stickers)
        assertEquals(
            EventState("p1_list", EventStatus.DONE, Verdict.GOOD, 0, 1, 1),
            S1cStand.eventOf(o.state, "p1_list"),
        )
        S1aChildText.check("верный путь списка", S1cStand.texts(o))
    }

    @Test
    fun `в плане на монету меньше списка — ошибочный путь`() {
        val s = S1cStand.started()
        val o = town.confirmPlan(economy.setPlan(s, 29, 0, 0).ok()).s1aOutcome()
        val r = o.eventResults.single()
        assertEquals(Verdict.MISTAKE, r.verdict)
        assertEquals("Корм и мыло — 30, а в «Нужном» меньше. Можно добавить из «Хочу».", r.line)
        assertEquals(
            listOf(Recovery.WantToNeed, Recovery.ReserveToNeed, Recovery.PlanTweak(TweakDir.NEED, 10)),
            r.recovery,
        )
        assertEquals("наклейка за любой исход", "st_list", r.sticker)
        assertEquals(
            EventState("p1_list", EventStatus.DONE, Verdict.MISTAKE, 1, 1, 1),
            S1cStand.eventOf(o.state, "p1_list"),
        )
        S1aChildText.check("ошибочный путь списка", S1cStand.texts(o))
    }

    // ---------- §3. Пк1: где дешевле ----------

    @Test
    fun `корм куплен там где дешевле`() {
        val s = S1cStand.planned(S1cStand.started(), 40, 20, 30)
        val o = town.buyAt(s, "food_basic", "shop_market", Source.NEED).s1aOutcome()
        val r = o.eventResults.single()
        assertEquals("pk1_cheaper_food", r.eventId)
        assertEquals(Verdict.GOOD, r.verdict)
        assertEquals("Одинаковый корм — выбрали, где дешевле: сэкономили 10.", r.line)
        assertEquals("st_cheaper", r.sticker)
        assertTrue("верный путь заметок не оставляет", o.state.notes.isEmpty())
        assertEquals(r.line, o.line)
        assertEquals(listOf("Финни с удовольствием хрустит кормом!", "Из «Нужного»: 40 → 20"), o.why)
        S1aChildText.check("корм у реки", S1cStand.texts(o))
    }

    @Test
    fun `ошибка с ценой корма возвращает событие на следующей неделе`() {
        var s = S1cStand.planned(S1cStand.started(), 40, 20, 30)
        val wrong = town.buyAt(s, "food_basic", "shop_foma", Source.NEED).s1aOutcome()
        val r = wrong.eventResults.single()
        assertEquals("pk1_cheaper_food", r.eventId)
        assertEquals(Verdict.MISTAKE, r.verdict)
        assertEquals("У реки такой же корм дешевле.", r.line)
        assertEquals(listOf(Recovery.RetryNextWeek), r.recovery)
        assertEquals(listOf(EventEffect.Note("корм у реки дешевле")), wrong.effects)
        assertEquals(listOf("корм у реки дешевле"), wrong.state.notes)

        s = S1cStand.finishWeek(wrong.state)
        val back = town.confirmPlan(economy.setPlan(s, 40, 20, 30).ok()).s1aOutcome()
        assertTrue("сравнение цен вернулось: ${back.arrived}", "pk1_cheaper_food" in back.arrived)
        val again = town.buyAt(back.state, "food_basic", "shop_foma", Source.NEED).s1aOutcome()
        assertEquals("заметка не задваивается", listOf("корм у реки дешевле"), again.state.notes)
        S1aChildText.check("корм у Фомы", S1cStand.texts(wrong))
    }

    @Test
    fun `после умолчания событие не возвращается`() {
        var s = S1cStand.planned(S1cStand.started(), 40, 20, 30)
        assertTrue("pk1_cheaper_food" in S1cStand.activeIds(s))
        s = S1cStand.finishWeek(s)
        assertEquals(
            EventState("pk1_cheaper_food", EventStatus.DONE, Verdict.OK, 2, 1, 1),
            S1cStand.eventOf(s, "pk1_cheaper_food"),
        )
        val next = town.confirmPlan(economy.setPlan(s, 40, 20, 30).ok()).s1aOutcome()
        assertEquals("вернулось только мыло", listOf("pk1_cheaper_soap"), next.arrived)
    }

    @Test
    fun `мыло у Фомы дешевле чем на рынке`() {
        var s = S1cStand.started()
        s = S1cStand.planned(s, 30, 0, 0)
        s = S1cStand.finishWeek(s)
        s = S1cStand.planned(s, 40, 20, 30)
        assertEquals("на второй неделе идёт только мыло", listOf("pk1_cheaper_soap"), S1cStand.activeIds(s))
        val right = town.buyAt(s, "care_soap", "shop_foma", Source.NEED).s1aOutcome()
        assertEquals(Verdict.GOOD, right.eventResults.single().verdict)
        assertEquals("Одинаковое мыло — выбрали, где дешевле.", right.eventResults.single().line)
        val wrong = town.buyAt(s, "care_soap", "shop_market", Source.NEED).s1aOutcome()
        assertEquals(Verdict.MISTAKE, wrong.eventResults.single().verdict)
        assertEquals("У Фомы такое же мыло дешевле.", wrong.eventResults.single().line)
        assertEquals(listOf("мыло у Фомы дешевле"), wrong.state.notes)
        S1aChildText.check("мыло", S1cStand.texts(right) + S1cStand.texts(wrong))
    }

    // ---------- §3. Пк3: супер-корм ----------

    @Test
    fun `супер-корм — ошибка, обычный корм — верный путь`() {
        val base = pk3Ready()
        assertTrue("pk3_super_food" in S1cStand.activeIds(base))
        val right = town.buyAt(base, "food_basic", "shop_market", Source.NEED).s1aOutcome()
        assertEquals("Реклама хвалит, а сытость та же — обычный корм дешевле.", right.eventResults.single().line)
        assertEquals(Verdict.GOOD, right.eventResults.single().verdict)
        assertEquals("st_poster", right.eventResults.single().sticker)

        val wrong = town.buyAt(base, "food_super", "shop_market", Source.NEED).s1aOutcome()
        assertEquals("Сытость одинаковая — выгоднее тот, что дешевле.", wrong.eventResults.single().line)
        assertEquals(Verdict.MISTAKE, wrong.eventResults.single().verdict)
        assertEquals(listOf(Recovery.RetryNextWeek), wrong.eventResults.single().recovery)
        assertEquals(listOf(EventEffect.Note("сытость +40 у обоих")), wrong.effects)
        assertEquals(listOf("сытость +40 у обоих"), wrong.state.notes)

        val dearer = town.buyAt(base, "food_basic", "shop_foma", Source.NEED).s1aOutcome()
        assertEquals("Сытость как у супер-корма, а у реки корм ещё дешевле.", dearer.eventResults.single().line)
        assertEquals(Verdict.OK, dearer.eventResults.single().verdict)
        S1aChildText.check("супер-корм", S1cStand.texts(right) + S1cStand.texts(wrong) + S1cStand.texts(dearer))
    }

    // ---------- §3. С1 и С3 у Фомы ----------

    @Test
    fun `распродажа меняет цену на полке и прежнюю цену`() {
        val before = c1Ready()
        assertEquals("до события робот стоит своё", 40, S1cStand.prices.price(before, "fun_robot", "shop_foma"))
        val o = town.visit(before, "foma")
        assertEquals(listOf("c1_robot_sale"), o.arrived)
        assertEquals(
            listOf(EventEffect.Offer("fun_robot", "shop_foma", 40, 25, Until.WEEK_END)),
            o.effects,
        )
        val s = o.state
        assertEquals("цена по распродаже", 25, S1cStand.prices.price(s, "fun_robot", "shop_foma"))
        assertEquals("прежняя цена", 40, S1cStand.prices.was(s, "fun_robot", "shop_foma"))
        val r = town.buyAt(s, "fun_robot", "shop_foma", Source.WANT).s1aOutcome()
        assertEquals(Verdict.OK, r.eventResults.single().verdict)
        assertEquals("На «Хочу» хватило — это твой выбор.", r.eventResults.single().line)
        assertEquals("строка исхода становится строкой действия", r.eventResults.single().line, r.line)
        assertEquals(listOf("Финни играет с роботом!", "Из «Хочу»: 40 → 15"), r.why)
    }

    @Test
    fun `робот из копилки — ошибка, а мимо — верный путь`() {
        val s = town.visit(c1Ready(), "foma").state
        val wrong = town.buyAt(s, "fun_robot", "shop_foma", Source.SAVINGS).s1aOutcome()
        assertEquals(Verdict.MISTAKE, wrong.eventResults.single().verdict)
        assertEquals("Робот из копилки — мечта отодвинулась.", wrong.eventResults.single().line)
        assertEquals(listOf(Recovery.ReserveToSavings), wrong.eventResults.single().recovery)

        val right = town.pass(s, "c1_robot_sale").s1aOutcome()
        assertEquals("Скидка — это дешевле, а не нужнее.", right.line)
        assertTrue("у «мимо» нет своей прежней строки", right.why.isEmpty())
        assertEquals(Verdict.GOOD, right.eventResults.single().verdict)
        assertTrue("st_robot_sale" in right.state.stickers)
        S1aChildText.check("робот", S1cStand.texts(wrong) + S1cStand.texts(right))
    }

    @Test
    fun `мечта почти твоя — мимо верно, из копилки ошибка`() {
        val s = town.visit(c3Ready(), "foma").state
        assertEquals(listOf("pk1_cheaper_soap", "c3_almost", "c1_robot_sale"), S1cStand.activeIds(s))
        val right = town.pass(s, "c3_almost").s1aOutcome()
        assertEquals("До мечты чуть-чуть — копилка цела.", right.line)
        assertEquals("одно «мимо» — одно событие", listOf("c3_almost"), right.eventResults.map { it.eventId })
        assertEquals(
            "остальные остаются на доске",
            listOf("pk1_cheaper_soap", "c1_robot_sale"),
            S1cStand.activeIds(right.state),
        )
        assertTrue("st_almost" in right.state.stickers)

        val wrong = town.buyAt(s, "fun_kite", "shop_foma", Source.SAVINGS).s1aOutcome()
        assertEquals(listOf("c3_almost"), wrong.eventResults.map { it.eventId })
        assertEquals("Копилка −30 — мечта снова дальше.", wrong.eventResults.single().line)
        assertEquals(
            listOf(Recovery.ReserveToSavings, Recovery.PlanTweak(TweakDir.SAVINGS, 10)),
            wrong.eventResults.single().recovery,
        )
        S1aChildText.check("мечта почти твоя", S1cStand.texts(right) + S1cStand.texts(wrong))
    }

    @Test
    fun `отказ кассы событию не засчитывается`() {
        val s = town.visit(c3Ready(), "foma").state
        val r = town.buyAt(s, "fun_robot", "shop_foma", Source.RESERVE)
        assertEquals(
            "Запас — на нужное и на всякий случай. Хотелки — из банки «Хочу»",
            r.s1aRefusal(),
        )
        assertEquals(
            "после отказа все события идут дальше",
            listOf("pk1_cheaper_soap", "c3_almost", "c1_robot_sale"),
            S1cStand.activeIds(s),
        )
    }

    // ---------- §3. П3: корм подорожал ----------

    @Test
    fun `корм подорожал — монеты нашлись, верный путь`() {
        val s = town.visit(S1cStand.planned(S1cStand.atWeek(3), 40, 20, 30), "market").state
        val o = town.buyAt(s, "food_basic", "shop_market", Source.NEED).s1aOutcome()
        assertEquals("один факт — одно событие", listOf("p3_price_up"), o.eventResults.map { it.eventId })
        val r = o.eventResults.single()
        assertEquals(Verdict.GOOD, r.verdict)
        assertEquals("Нужное подорожало — монеты нашлись, копилка цела.", r.line)
        assertEquals("st_price_up", r.sticker)
        assertEquals("плакат остался на доске", listOf("pk3_super_food"), S1cStand.activeIds(o.state))
        S1aChildText.check("подорожание", S1cStand.texts(o))
    }

    @Test
    fun `копилка на корм при пустых банках — не ошибка, а при полных — ошибка`() {
        val full = town.visit(S1cStand.planned(S1cStand.atWeek(3), 10, 20, 100), "market").state
        assertEquals("«Хочу» не пусто", 20, full.jarWant)
        val a = town.buyAt(full, "food_basic", "shop_market", Source.SAVINGS).s1aOutcome()
        val ra = a.eventResults.single { it.eventId == "p3_price_up" }
        assertEquals(Verdict.MISTAKE, ra.verdict)
        assertEquals("Когда нужное дорожает, первым урезают «Хочу», а не копилку.", ra.line)
        assertEquals(
            listOf(Recovery.ReserveToSavings, Recovery.PlanTweak(TweakDir.NEED, 10)),
            ra.recovery,
        )

        val empty = town.visit(S1cStand.planned(S1cStand.atWeek(3), 10, 0, 290), "market").state
        assertEquals("«Хочу» пусто", 0, empty.jarWant)
        assertEquals("запас пуст", 0, empty.reserve)
        val b = town.buyAt(empty, "food_basic", "shop_market", Source.SAVINGS).s1aOutcome()
        val rb = b.eventResults.single { it.eventId == "p3_price_up" }
        assertEquals(Verdict.OK, rb.verdict)
        assertEquals("Копилка выручила — корм куплен.", rb.line)
        S1aChildText.check("копилка на корм", S1cStand.texts(a) + S1cStand.texts(b))
    }

    @Test
    fun `приглашение доплатить из запаса остаётся строкой действия`() {
        val s = town.visit(S1cStand.planned(S1cStand.atWeek(3), 20, 10, 0), "market").state
        assertEquals("запас до покупки", 270, s.reserve)
        val o = town.buyAt(s, "food_basic", "shop_market", Source.RESERVE).s1aOutcome()
        assertEquals("Хорошо, что был запас! В новом плане дадим «Нужному» побольше?", o.line)
        assertEquals(
            listOf(
                "Нужное подорожало — монеты нашлись, копилка цела.",
                "Из «Нужного»: 20 → 0",
                "Из запаса: 270 → 260",
            ),
            o.why,
        )
        S1aChildText.check("доплата из запаса", S1cStand.texts(o))
    }

    @Test
    fun `плакат уступает сравнению цен по приоритету`() {
        // неделя 1: ошибка с ценой корма — событие вернётся
        var s = S1cStand.planned(S1cStand.started(), 40, 20, 30)
        s = town.buyAt(s, "food_basic", "shop_foma", Source.NEED).s1aState()
        s = S1cStand.finishWeek(s)
        // неделя 2: сравнение цен вернулось, на рынке пришёл плакат — приоритет 2 против 1
        s = S1cStand.planned(s, 40, 20, 30)
        val market = town.visit(s, "market")
        assertTrue("pk3_super_food" in market.arrived)
        val week2 = town.buyAt(market.state, "food_basic", "shop_market", Source.NEED).s1aOutcome()
        assertEquals("больший приоритет", listOf("pk1_cheaper_food"), week2.eventResults.map { it.eventId })
        assertEquals(Verdict.GOOD, week2.eventResults.single().verdict)
        assertTrue("плакат остался активным", "pk3_super_food" in S1cStand.activeIds(week2.state))
    }

    @Test
    fun `корм у реки при трёх событиях засчитывается подорожанию`() {
        var s = S1cStand.planned(S1cStand.started(), 40, 20, 30)
        s = town.buyAt(s, "food_basic", "shop_foma", Source.NEED).s1aState() // ошибка недели 1
        s = S1cStand.finishWeek(s)
        s = S1cStand.planned(s, 40, 20, 30)
        s = town.buyAt(s, "food_basic", "shop_foma", Source.NEED).s1aState() // ошибка недели 2
        s = S1cStand.finishWeek(s)
        s = S1cStand.planned(s, 40, 20, 30)
        assertTrue("сравнение цен снова идёт", "pk1_cheaper_food" in S1cStand.activeIds(s))
        s = town.visit(s, "market").state
        assertEquals(
            listOf("pk1_cheaper_food", "pk3_super_food", "p3_price_up"),
            S1cStand.activeIds(s),
        )
        val o = town.buyAt(s, "food_basic", "shop_market", Source.NEED).s1aOutcome()
        assertEquals("наибольший приоритет", listOf("p3_price_up"), o.eventResults.map { it.eventId })
        assertEquals(
            "остальные остаются на доске",
            listOf("pk1_cheaper_food", "pk3_super_food"),
            S1cStand.activeIds(o.state),
        )
    }

    // ---------- §5. Окна и закрытие ----------

    @Test
    fun `недельное умолчание ждёт конца недели а не сна`() {
        var s = town.visit(c1Ready(), "foma").state
        assertTrue("c1_robot_sale" in S1cStand.activeIds(s))
        s = S1cStand.nextDay(s)
        assertTrue("после сна событие ещё идёт", "c1_robot_sale" in S1cStand.activeIds(s))
        val end = town.endWeek(s).s1aOutcome()
        val r = end.eventResults.single { it.eventId == "c1_robot_sale" }
        assertEquals(Verdict.GOOD, r.verdict)
        assertEquals("Скидка — это дешевле, а не нужнее.", r.line)
        assertEquals("st_robot_sale", r.sticker)
        assertTrue("st_robot_sale" in end.state.stickers)
        assertEquals(
            EventState("c1_robot_sale", EventStatus.DONE, Verdict.GOOD, 0, 2, 2),
            S1cStand.eventOf(end.state, "c1_robot_sale"),
        )
        assertEquals("итог недели оставляет свою строку", "Траты — по плану! В новом конверте начнём с корма и мыла", end.line)
        S1aChildText.check("итог недели с умолчанием", S1cStand.texts(end))
    }

    @Test
    fun `непокрытая еда в конце недели засчитывается подорожанию`() {
        var s = S1cStand.planned(S1cStand.atWeek(3), 20, 10, 0)
        s = town.visit(s, "market").state
        s = S1cStand.nextDay(S1cStand.nextDay(s))
        assertEquals("последний день недели", 3, s.day)
        val end = town.endWeek(s).s1aOutcome()
        val r = end.eventResults.single { it.eventId == "p3_price_up" }
        assertEquals(Verdict.MISTAKE, r.verdict)
        assertEquals("Когда нужное дорожает, первым урезают «Хочу», а не еду.", r.line)
        assertEquals(listOf(Recovery.PlanTweak(TweakDir.NEED, 10)), r.recovery)
        assertEquals("плакат закрылся умолчанием", "Плакат висит — выбрать можно в другой раз.", end.eventResults.single { it.eventId == "pk3_super_food" }.line)
        assertEquals("итог недели оставляет свою строку", "Траты — по плану! В новом конверте начнём с корма и мыла", end.line)
        assertEquals(
            listOf(
                "«Нужное»: план 20, потрачено 0",
                "«Хочу»: план 10, потрачено 0",
                "Копилка: план 0, отложено 0",
            ),
            end.why,
        )
    }

    @Test
    fun `вечернее умолчание срабатывает во сне`() {
        val fx = S1cFixture.town(
            S1cFixture.event(
                "fx_evening", arrives = Arrives(1, 1),
                outcomes = listOf(EventOutcome(Fact.Skip, Verdict.OK, "Вечер прошёл спокойно.")),
                default = EventDefault(Until.DAY_END, Fact.Skip), sticker = "st_list",
            ),
        )
        var s = fx.tick(S1cStand.profile()).state
        s = fx.confirmPlan(economy.setPlan(s, 30, 0, 0).ok()).s1aState()
        val nightOutcome = fx.sleep(s).s1aOutcome()
        assertEquals("сон оставляет свою строку", "Спокойный день дома", nightOutcome.line)
        val r = nightOutcome.eventResults.single()
        assertEquals(Verdict.OK, r.verdict)
        assertEquals("Вечер прошёл спокойно.", r.line)
        assertTrue("st_list" in nightOutcome.state.stickers)
        assertEquals(
            EventState("fx_evening", EventStatus.DONE, Verdict.OK, 0, 1, 1),
            nightOutcome.state.events.last { it.id == "fx_evening" },
        )
    }

    @Test
    fun `вечернее умолчание без подходящего исхода закрывает событие тихо`() {
        val fx = S1cFixture.town(
            S1cFixture.event(
                "fx_evening_no", arrives = Arrives(1, 1),
                outcomes = listOf(
                    EventOutcome(Fact.Skip, Verdict.OK, "Так не бывает.", condition = Condition.SavingsPctAtLeast(70)),
                ),
                default = EventDefault(Until.DAY_END, Fact.Skip), sticker = "st_list",
            ),
        )
        var s = fx.tick(S1cStand.profile()).state
        s = fx.confirmPlan(economy.setPlan(s, 30, 0, 0).ok()).s1aState()
        assertEquals("Здесь решают делом", fx.pass(s, "fx_evening_no").s1aRefusal())
        val nightOutcome = fx.sleep(s).s1aOutcome()
        assertTrue("тихое закрытие без итога", nightOutcome.eventResults.isEmpty())
        assertFalse("наклейки нет", "st_list" in nightOutcome.state.stickers)
        assertEquals(
            EventState("fx_evening_no", EventStatus.DONE, null, -1, 1, 1),
            nightOutcome.state.events.last { it.id == "fx_evening_no" },
        )
    }

    // ---------- §4. Эффекты ----------

    @Test
    fun `на рынке холодильник запоминает цену уже с подорожанием`() {
        val s = S1cStand.planned(S1cStand.atWeek(3), 40, 20, 30)
        assertEquals("до входа цена обычная", 20, S1cStand.prices.price(s, "food_basic", "shop_market"))
        val o = town.visit(s, "market")
        assertEquals(listOf("pk3_super_food", "p3_price_up"), o.arrived)
        assertEquals(
            listOf(
                EventEffect.Price("food_basic", "shop_market", 30, Until.WEEK_END),
                EventEffect.Price("food_basic", "shop_foma", 40, Until.WEEK_END),
            ),
            o.effects,
        )
        assertEquals(SeenPrice("shop_market", 30, 3), o.state.seenPrices["food_basic"])
        assertEquals("цена у Фомы тоже поднялась", 40, S1cStand.prices.price(o.state, "food_basic", "shop_foma"))
        assertTrue("наклейка места", "st_place_market" in o.state.stickers)
    }

    @Test
    fun `вещь ломается при приходе и чинится исходом`() {
        val fx = S1cFixture.town(
            S1cFixture.event(
                "fx_break", arrives = Arrives(1, 1),
                setup = listOf(EventEffect.ItemBreak("home_lamp")),
                outcomes = listOf(
                    EventOutcome(
                        Fact.Skip, Verdict.GOOD, "Лампа снова горит.",
                        effects = listOf(EventEffect.ItemFix("home_lamp")),
                    ),
                ),
                sticker = "st_lamp",
            ),
        )
        val o = fx.tick(S1cStand.profile())
        assertEquals(listOf("fx_break"), o.arrived)
        assertEquals(listOf("home_lamp"), o.state.broken)
        assertEquals(listOf(EventEffect.ItemBreak("home_lamp")), o.effects)
        val fixed = fx.pass(o.state, "fx_break").s1aOutcome()
        assertTrue("лампа починена", fixed.state.broken.isEmpty())
        assertEquals(listOf(EventEffect.ItemFix("home_lamp")), fixed.effects)
        assertEquals("Лампа снова горит.", fixed.line)
        assertTrue("st_lamp" in fixed.state.stickers)
    }

    @Test
    fun `монеты и сдача в приходе — ошибка контента`() {
        listOf(
            S1cFixture.event("fx_coins", arrives = Arrives(1, 1), setup = listOf(EventEffect.Coins(20, CoinSource.WINDFALL))),
            S1cFixture.event("fx_change", arrives = Arrives(1, 1), setup = listOf(EventEffect.ShortChange(5))),
        ).forEach { ev ->
            val e = assertThrows(IllegalStateException::class.java) {
                S1cFixture.town(ev).tick(S1cStand.profile())
            }
            assertTrue("в сообщении нет id события «${ev.id}»: ${e.message}", e.message.orEmpty().contains(ev.id))
        }
    }

    @Test
    fun `показатели и цены в исходе — ошибка контента`() {
        listOf<EventEffect>(
            EventEffect.StatChange(Stat.MOOD, 5),
            EventEffect.Price("food_basic", "shop_market", 5, Until.WEEK_END),
        ).forEachIndexed { i, effect ->
            val ev = S1cFixture.event(
                "fx_effect_$i", arrives = Arrives(1, 1),
                outcomes = listOf(EventOutcome(Fact.Skip, Verdict.OK, "Так бывает.", effects = listOf(effect))),
            )
            val fx = S1cFixture.town(ev)
            val started = fx.tick(S1cStand.profile()).state
            assertTrue("событие не пришло — проверять нечего", started.events.any { it.id == ev.id })
            val e = assertThrows(IllegalStateException::class.java) { fx.pass(started, ev.id) }
            assertTrue("в сообщении нет id события «${ev.id}»: ${e.message}", e.message.orEmpty().contains(ev.id))
        }
    }

    @Test
    fun `умолчание не «мимо» и не «конец недели» — ошибка контента`() {
        assertThrows(IllegalStateException::class.java) {
            val fx = S1cFixture.town(
                S1cFixture.event(
                    "fx_bad_default", arrives = Arrives(1, 1),
                    outcomes = listOf(EventOutcome(Fact.Skip, Verdict.OK, "Так бывает.")),
                    default = EventDefault(Until.WEEK_END, Fact.Buy(BuyTarget.Item("food_basic"))),
                ),
            )
            var s = fx.tick(S1cStand.profile()).state
            s = fx.confirmPlan(economy.setPlan(s, 30, 0, 0).ok()).s1aState()
            repeat(S1cStand.days - 1) { s = fx.wake(fx.sleep(s).s1aState()).s1aState() }
            fx.sleep(s)
        }
    }

    // ---------- §6. Доска, карточка, «мимо», заказы ----------

    @Test
    fun `карточка городка — событие, потом заказ, потом тишина`() {
        val s0 = S1cStand.profile()
        assertEquals(Card("В городке спокойно — загляни на доску"), town.card(s0))
        val s1 = town.tick(s0).state
        assertEquals(Card("Список на неделю", "p1_list", "home"), town.card(s1))
        val s2 = S1cStand.planned(s1, 30, 0, 0)
        assertEquals(Card("Где дешевле: корм", "pk1_cheaper_food", null), town.card(s2))
        val s3 = town.buyAt(s2, "food_basic", "shop_market", Source.NEED).s1aState()
        assertEquals(Card("Пекарне нужен помощник", "job_bakery_help", "bakery"), town.card(s3))
        S1aChildText.check("карточка", listOf(town.card(s0).title, town.card(s1).title, town.card(s3).title))
    }

    @Test
    fun `заказы появляются после раскладки монет — и в демо тоже`() {
        val plain = S1cStand.profile()
        assertTrue("до раскладки заказов нет", town.orders(plain).isEmpty())
        assertTrue("в демо заказ тоже ждёт раскладки", town.orders(S1cStand.profile(demo = true)).isEmpty())
        var s = S1cStand.planned(plain, 40, 20, 30)
        assertEquals(listOf("job_bakery_help", "job_market_help"), town.orders(s).map { it.id })
        assertTrue("заказы в сохранении не живут", s.events.none { it.id.startsWith("job_") })
        assertTrue("заказов нет на доске событий", S1cStand.activeIds(s).none { it.startsWith("job_") })
        repeat(3) { s = town.finishShift(s, "job_market", 3, 0).s1aState() }
        assertEquals("жетоны смен кончились", 3, s.shiftsThisPeriod)
        assertTrue("без жетонов заказов нет", town.orders(s).isEmpty())
    }

    @Test
    fun `наклейка заказа даётся за оплаченную смену ещё до раскладки монет`() {
        val s = S1cStand.profile()
        assertFalse("монеты ещё не разложены", s.plan.confirmed)
        assertTrue("заказа на доске пока нет", town.orders(s).isEmpty())
        val after = town.finishShift(s, "job_bakery", 60, 0).s1aState()
        assertEquals(listOf("st_job_bakery"), after.stickers)
        val again = town.finishShift(after, "job_bakery", 60, 0).s1aState()
        assertEquals("наклейка не задваивается", listOf("st_job_bakery"), again.stickers)
    }

    @Test
    fun `смена ради рекорда наклейку заказа не даёт`() {
        var s = S1cStand.planned(S1cStand.profile(), 40, 20, 30)
        repeat(3) { s = town.finishShift(s, "job_market", 3, 0).s1aState() }
        assertEquals(listOf("st_job_market"), s.stickers.filter { it.startsWith("st_job_") })
        val record = town.finishShift(s, "job_bakery", 60, 0).s1aState()
        assertFalse("за игру ради рекорда наклейки нет", "st_job_bakery" in record.stickers)
    }

    @Test
    fun `наклейка места даётся за вход и не задваивается`() {
        val s = S1cStand.planned(S1cStand.started(), 40, 20, 30)
        val home = town.visit(s, "home").state
        assertEquals(listOf("st_place_home"), home.stickers.filter { it.startsWith("st_place_") })
        val market = town.visit(home, "market").state
        val twice = town.visit(market, "market").state
        assertEquals(
            listOf("st_place_home", "st_place_market"),
            twice.stickers.filter { it.startsWith("st_place_") },
        )
    }

    @Test
    fun `пройти мимо можно не всюду`() {
        val s = S1cStand.started()
        assertEquals("Здесь решают делом", town.pass(s, "p1_list").s1aRefusal())
        assertEquals("Такого события сейчас нет", town.pass(s, "c1_robot_sale").s1aRefusal())
        assertEquals("Такого события сейчас нет", town.pass(s, "нет_такого").s1aRefusal())
        assertEquals(noPet, town.pass(economy.newGame(false), "p1_list").s1aRefusal())
        val asleep = town.sleep(S1cStand.planned(s, 30, 0, 0)).s1aState()
        assertEquals(night, town.pass(asleep, "pk1_cheaper_food").s1aRefusal())
    }

    // ---------- §6. Демо: startEvent ----------

    @Test
    fun `startEvent работает только в демо и только для живых событий`() {
        val plain = S1cStand.planned(S1cStand.profile(), 40, 20, 30)
        assertEquals("Событие придёт само", town.startEvent(plain, "c1_robot_sale").s1aRefusal())
        val demo = S1cStand.planned(S1cStand.profile(demo = true), 40, 20, 30)
        assertEquals("Такого события нет", town.startEvent(demo, "job_bakery_help").s1aRefusal())
        assertEquals("Такого события нет", town.startEvent(demo, "нет_такого").s1aRefusal())
        assertEquals("Событие уже идёт", town.startEvent(demo, "pk1_cheaper_food").s1aRefusal())
        assertEquals("План уже готов — событие придёт с новым конвертом", town.startEvent(demo, "p1_list").s1aRefusal())
        assertEquals(
            "Сейчас не начать: после раскладки монет",
            town.startEvent(S1cStand.profile(demo = true), "c1_robot_sale").s1aRefusal(),
        )
        assertEquals(noPet, town.startEvent(economy.newGame(true), "c1_robot_sale").s1aRefusal())
        assertEquals(night, town.startEvent(town.sleep(demo).s1aState(), "c1_robot_sale").s1aRefusal())
    }

    @Test
    fun `startEvent объясняет что мешает по плану и мечте`() {
        val fresh = S1cStand.profile(demo = true)
        val planned = S1cStand.planned(fresh, 30, 20, 50)
        assertEquals("Сейчас не начать: после раскладки монет", startRefusal(listOf(Condition.AfterPlan), fresh))
        assertEquals(
            "План уже готов — событие придёт с новым конвертом",
            startRefusal(listOf(Condition.BeforePlan), planned),
        )
        assertEquals("Сейчас не начать: нужна мечта", startRefusal(listOf(Condition.HasGoal), planned))
        assertEquals(
            "у «хотя бы одного» — строка первого варианта",
            "Сейчас не начать: нужна мечта",
            startRefusal(listOf(Condition.AnyOf(listOf(Condition.HasGoal, Condition.ReserveAtLeast(99)))), planned),
        )
        val poor = town.makeGoal(S1cStand.planned(S1cStand.profile(demo = true), 60, 20, 10), "fun_tent").s1aState()
        assertEquals("копилка", 10, poor.savings)
        assertEquals(
            "Сейчас не начать: в копилке нужно 42",
            startRefusal(listOf(Condition.SavingsPctAtLeast(70)), poor),
        )
    }

    @Test
    fun `startEvent объясняет чего не хватает в банках`() {
        val s = S1cStand.planned(S1cStand.profile(demo = true), 30, 20, 30)
        assertEquals("«Хочу»", 20, s.jarWant)
        assertEquals("запас", 20, s.reserve)
        assertEquals("Сейчас не начать: в «Хочу» нужно 50", startRefusal(listOf(Condition.WantAtLeast(50)), s))
        assertEquals("Сейчас не начать: в запасе нужно 50", startRefusal(listOf(Condition.ReserveAtLeast(50)), s))
    }

    @Test
    fun `startEvent объясняет чего не хватает среди вещей`() {
        var s = S1cStand.planned(S1cStand.profile(demo = true), 0, 30, 0)
        assertEquals(
            "Сейчас не начать: сначала нужна вещь «Робот»",
            startRefusal(listOf(Condition.Owns("fun_robot")), s),
        )
        s = town.buyAt(s, "fun_rug", "shop_foma", Source.WANT).s1aState()
        assertTrue("коврик дома", "fun_rug" in s.owned)
        assertEquals(
            "Сейчас не начать: коврик уже есть дома",
            startRefusal(listOf(Condition.NotOwned("fun_rug")), s),
        )
        val fx = S1cFixture.town(
            S1cFixture.event("fx_break_lamp", arrives = Arrives(1, 1), setup = listOf(EventEffect.ItemBreak("home_lamp"))),
            S1cFixture.event("fx_needs_lamp", requires = listOf(Condition.NotBroken("home_lamp"))),
        )
        val broken = fx.tick(S1cStand.profile(demo = true)).state
        assertEquals(listOf("home_lamp"), broken.broken)
        assertEquals(
            "Сейчас не начать: лампа над кроватью уже не работает",
            fx.startEvent(broken, "fx_needs_lamp").s1aRefusal(),
        )
    }

    @Test
    fun `в демо мечта для показа появляется только когда монет хватает`() {
        val planned = S1cStand.planned(S1cStand.profile(demo = true), 40, 20, 30)
        assertEquals("копилка после раскладки", 30, planned.savings)
        assertEquals("Сейчас не начать: в копилке нужно 35", town.startEvent(planned, "c3_almost").s1aRefusal())
        assertNull("отказ мечту не заводит", planned.goal)

        val richer = town.deposit(planned, Source.WANT, 10).s1aState()
        assertEquals("копилка после взноса", 40, richer.savings)
        val o = town.startEvent(richer, "c3_almost").s1aOutcome()
        assertEquals(listOf("c3_almost"), o.arrived)
        assertEquals("В витрине новинка — воздушный змей за 30. А до мечты совсем чуть-чуть!", o.line)
        assertEquals(Goal("demo_goal", "Мечта для показа", "⭐", 57), o.state.goal)
        assertEquals(listOf("pk1_cheaper_food", "c3_almost"), S1cStand.activeIds(o.state))
        S1aChildText.check("демо-запуск мечты", S1cStand.texts(o))
    }

    @Test
    fun `своя мечта в демо не заменяется мечтой для показа`() {
        var s = S1cStand.planned(S1cStand.profile(demo = true), 30, 20, 50)
        s = town.makeGoal(s, "fun_tent").s1aState()
        val o = town.startEvent(s, "c3_almost").s1aOutcome()
        assertEquals("item:fun_tent", o.state.goal?.id)
        assertEquals(60, o.state.goal?.price)
        assertTrue("c3_almost" in S1cStand.activeIds(o.state))
    }

    @Test
    fun `в демо подорожание начинается даже когда корм уже куплен`() {
        var s = S1cStand.planned(S1cStand.profile(demo = true), 40, 20, 30)
        s = town.buyAt(s, "food_basic", "shop_market", Source.NEED).s1aState()
        assertTrue("корм в покупках недели", s.purchases.any { it.need == Need.FOOD })
        val o = town.startEvent(s, "p3_price_up").s1aOutcome()
        assertEquals(listOf("p3_price_up"), o.arrived)
        assertEquals("Ценник: «Привоз задержался — корм подорожал»", o.line)
        assertEquals("цена поднялась", 30, S1cStand.prices.price(o.state, "food_basic", "shop_market"))

        val end = town.endWeek(o.state).s1aOutcome()
        assertTrue("без факта итога нет", end.eventResults.none { it.eventId == "p3_price_up" })
        assertEquals(
            EventState("p3_price_up", EventStatus.DONE, null, -1, 1, 1),
            S1cStand.eventOf(end.state, "p3_price_up"),
        )
        assertFalse("наклейки за это нет", "st_price_up" in end.state.stickers)
        val week2 = town.wake(end.state).s1aState()
        assertEquals("новая неделя", 2, week2.period)
        assertFalse("на новой неделе событие не висит", "p3_price_up" in S1cStand.activeIds(week2))
    }

    // ---------- §8. Стоп-лист на выдаче ----------

    @Test
    fun `городок говорит с ребёнком без стоп-слов на путях событий`() {
        val texts = mutableListOf<String>()
        fun add(o: TownOutcome) {
            texts += o.line
            texts += o.why
            texts += o.eventResults.map { it.line }
        }

        var s = S1cStand.profile()
        texts += town.card(s).title
        val first = town.tick(s)
        add(first)
        s = first.state
        texts += town.card(s).title
        texts += town.pass(s, "p1_list").s1aRefusal()
        texts += town.pass(s, "c1_robot_sale").s1aRefusal()

        val plan = town.confirmPlan(economy.setPlan(s, 40, 20, 30).ok()).s1aOutcome()
        add(plan)
        s = plan.state
        texts += town.card(s).title
        add(town.buyAt(s, "food_basic", "shop_foma", Source.NEED).s1aOutcome())
        val right = town.buyAt(s, "food_basic", "shop_market", Source.NEED).s1aOutcome()
        add(right)
        s = right.state
        add(town.visit(s, "market"))
        add(town.endWeek(S1cStand.nextDay(S1cStand.nextDay(s))).s1aOutcome())

        add(town.pass(town.visit(c3Ready(), "foma").state, "c3_almost").s1aOutcome())

        val demo = S1cStand.planned(S1cStand.profile(demo = true), 40, 20, 30)
        texts += town.startEvent(demo, "c3_almost").s1aRefusal()
        texts += town.startEvent(demo, "p1_list").s1aRefusal()
        texts += town.startEvent(demo, "job_bakery_help").s1aRefusal()
        texts += town.startEvent(demo, "pk1_cheaper_food").s1aRefusal()
        texts += town.startEvent(S1cStand.profile(), "c1_robot_sale").s1aRefusal()
        add(town.startEvent(town.deposit(demo, Source.WANT, 10).s1aState(), "c3_almost").s1aOutcome())
        texts += town.card(demo).title

        S1aChildText.check("строки событий «Городка»", texts)
    }
}
