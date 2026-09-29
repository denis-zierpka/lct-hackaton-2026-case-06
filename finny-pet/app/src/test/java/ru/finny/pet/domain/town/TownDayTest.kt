package ru.finny.pet.domain.town

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.pet.domain.BudgetPlan
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.LedgerEntry
import ru.finny.pet.domain.ok

/**
 * День, сон и конец недели «Городка» (TOWN-S1a §5) плюс дом, места и посещение (§6).
 * Дни — счётчик: их двигает «Проснуться», а не календарь.
 */
class TownDayTest {

    private val town = S1aStand.town
    private val economy = S1aStand.economy

    // ---------- §5. Сон и день ----------

    @Test
    fun `в неделе три дня по контенту`() = assertEquals("town.rules.daysPerWeek", 3, S1aStand.days)

    @Test
    fun `без плана спать не пускают и день стоит`() {
        val s = S1aStand.profile()
        assertEquals("отказ", "Сначала разложим монеты — потом спать", town.sleep(s).s1aRefusal())
        assertEquals("день сдвинулся", 1, s.day)
        assertFalse("наступила ночь", s.asleep)
    }

    @Test
    fun `сон без пробуждения день не двигает а пробуждение двигает`() {
        val s = S1aStand.planned(40, 20, 30)
        val night = town.sleep(s).s1aState()
        assertTrue("не ночь", night.asleep)
        assertEquals("сон сдвинул день", 1, night.day)
        assertEquals("второй сон подряд", "Уже ночь", town.sleep(night).s1aRefusal())

        val morning = town.wake(night).s1aState()
        assertFalse("не утро", morning.asleep)
        assertEquals("день не сдвинулся", 2, morning.day)
        assertEquals("строка утра", "Доброе утро!", town.wake(night).s1aOutcome().line)
        assertEquals("второе пробуждение подряд", "Уже утро", town.wake(morning).s1aRefusal())
        assertEquals("без питомца", "Сначала создай питомца", town.wake(economy.newGame(true)).s1aRefusal())
    }

    @Test
    fun `строка дня берётся из дневника`() {
        val s = S1aStand.planned(40, 20, 30)
        assertEquals("день без покупок", "Спокойный день дома", town.sleep(s).s1aOutcome().line)

        val one = town.buyAt(s, "food_basic", "shop_market", Source.NEED).s1aState()
        assertEquals("одна запись дня", "Купили у реки: корм", town.sleep(one).s1aOutcome().line)

        val two = town.buyAt(one, "care_soap", "shop_foma", Source.NEED).s1aState()
        assertEquals(
            "две записи дня",
            "Купили у реки: корм. Купили у Фомы: мыло",
            town.sleep(two).s1aOutcome().line,
        )
    }

    @Test
    fun `строка дня берёт записи только этого дня`() {
        val day1 = town.buyAt(S1aStand.planned(40, 20, 30), "food_basic", "shop_market", Source.NEED).s1aState()
        val day2 = town.wake(town.sleep(day1).s1aState()).s1aState()
        assertEquals("день", 2, day2.day)
        assertEquals("вчерашняя запись попала в сегодня", "Спокойный день дома", town.sleep(day2).s1aOutcome().line)
    }

    @Test
    fun `третий сон это конец недели`() {
        var s = S1aStand.planned(40, 20, 30, demo = false)
        s = town.wake(town.sleep(s).s1aState()).s1aState()
        s = town.wake(town.sleep(s).s1aState()).s1aState()
        assertEquals("день перед третьим сном", 3, s.day)
        assertEquals("третий сон не равен концу недели", town.endWeek(s), town.sleep(s))
        assertEquals("неделя не кончилась", 2, town.sleep(s).s1aState().period)
    }

    @Test
    fun `без демо неделю раньше срока не закончить а в демо можно с любого дня`() {
        val real = S1aStand.planned(40, 20, 30, demo = false)
        assertEquals("день", 1, real.day)
        assertEquals("отказ", "Неделя ещё идёт", town.endWeek(real).s1aRefusal())

        val demo = S1aStand.planned(40, 20, 30, demo = true)
        assertEquals("демо-пропуск не сработал", 2, town.endWeek(demo).s1aState().period)
    }

    @Test
    fun `конец недели требует питомца и подтверждённого плана`() {
        assertEquals("нет питомца", "Сначала создай питомца", town.endWeek(economy.newGame(true)).s1aRefusal())
        assertEquals(
            "план не подтверждён",
            "Сначала разложим монеты — потом спать",
            town.endWeek(S1aStand.profile()).s1aRefusal(),
        )
    }

    // ---------- §5. Конец недели ----------

    @Test
    fun `конец недели обнуляет банки ставит ночь и день первый`() {
        val s = town.endWeek(S1aStand.planned(40, 20, 30)).s1aState()
        assertEquals("«Нужное»", 0, s.jarNeed)
        assertEquals("«Хочу»", 0, s.jarWant)
        assertEquals("день", 1, s.day)
        assertTrue("итог недели идёт ночью", s.asleep)
        assertEquals("жетоны смен", 0, s.shiftsThisPeriod)
        assertEquals("неделя", 2, s.period)
        assertFalse("заготовка плана подтверждена", s.plan.confirmed)
        assertS1aInvariants("после конца недели", s)
    }

    @Test
    fun `заготовка следующего плана это прошлый план`() {
        val s = town.endWeek(S1aStand.planned(40, 20, 30)).s1aState()
        assertEquals("заготовка", BudgetPlan(40, 20, 30, confirmed = false), s.plan)
    }

    @Test
    fun `заготовка больше кошелька режется сначала по «Хочу»`() {
        // неделя 1: копилка съедает 80, неделя 2: план 60 / 30 / 30 потрачен до нуля
        var s = S1aStand.planned(10, 10, 80)
        s = town.wake(town.endWeek(s).s1aState()).s1aState()
        assertEquals("кошелёк недели 2", 120, s.balance)
        s = town.confirmPlan(economy.setPlan(s, 60, 30, 30).ok()).s1aState()
        s = town.buyAt(s, "food_basic", "shop_foma", Source.NEED).s1aState()
        s = town.buyAt(s, "care_shampoo", "shop_foma", Source.NEED).s1aState()
        s = town.buyAt(s, "care_soap", "shop_foma", Source.NEED).s1aState()
        s = town.buyAt(s, "fun_carousel", "shop_market", Source.WANT).s1aState()
        s = town.buyAt(s, "fun_icecream", "shop_market", Source.WANT).s1aState()
        assertEquals("кошелёк перед итогом", 0, s.balance)

        val after = town.endWeek(s).s1aState()
        assertEquals("кошелёк новой недели", 100, after.balance)
        assertEquals("«Хочу» урезано на избыток", BudgetPlan(60, 10, 30, confirmed = false), after.plan)
        assertS1aInvariants("после урезания заготовки", after)
    }

    @Test
    fun `на избыток больше «Хочу» режется «В копилку» а потом «Нужное»`() {
        // граничные заготовки: пустой кошелёк перед итогом собран руками (журнал пуст, сумма 0)
        val base = S1aStand.planned(40, 20, 30).copy(balance = 0, jarNeed = 0, jarWant = 0, ledger = emptyList())

        val deepO = town.endWeek(base.copy(plan = BudgetPlan(60, 10, 50, confirmed = true))).s1aState()
        assertEquals("после «Хочу» режется «В копилку»", BudgetPlan(60, 0, 40, confirmed = false), deepO.plan)
        assertEquals("кошелёк", 100, deepO.balance)

        val deepM = town.endWeek(base.copy(plan = BudgetPlan(110, 5, 5, confirmed = true))).s1aState()
        assertEquals("последним режется «Нужное»", BudgetPlan(100, 0, 0, confirmed = false), deepM.plan)
    }

    @Test
    fun `строка итога с тремя штампами`() {
        val s = town.endWeek(demoWeekOne()).s1aState()
        val summary = s.history.last()
        assertTrue("штамп «Нужное куплено»", summary.mandatoryCovered)
        assertTrue("штамп «По плану»", summary.planKept)
        assertTrue("штамп «Отложено»", summary.saved)
        assertEquals("прибавка роста", 3, summary.score)
        assertEquals("строка итога", "Всё по плану, копилка +40!", town.endWeek(demoWeekOne()).s1aOutcome().line)
        assertEquals(
            "«Почему?» итога",
            listOf(
                "«Нужное»: план 40, потрачено 30",
                "«Хочу»: план 20, потрачено 8",
                "Копилка: план 30, отложено 40",
            ),
            town.endWeek(demoWeekOne()).s1aOutcome().why,
        )
    }

    @Test
    fun `строка итога без единого штампа`() {
        // план 0 / 30 / 0: корм оплачен из запаса, ухода нет, копилка не росла
        val s = town.buyAt(S1aStand.planned(0, 30, 0), "food_basic", "shop_market", Source.RESERVE).s1aState()
        val r = town.endWeek(s)
        val summary = r.s1aState().history.last()
        assertFalse("штамп «Нужное куплено»", summary.mandatoryCovered)
        assertFalse("штамп «По плану»", summary.planKept)
        assertFalse("штамп «Отложено»", summary.saved)
        assertEquals("строка итога", "Неделя позади. Начнём с корма и мыла?", r.s1aOutcome().line)
    }

    @Test
    fun `строка итога с одним штампом называет первый полученный и шаг первого неполученного`() {
        var s = S1aStand.planned(0, 30, 0)
        s = town.buyAt(s, "food_basic", "shop_market", Source.RESERVE).s1aState()
        s = town.buyAt(s, "care_soap", "shop_foma", Source.RESERVE).s1aState()
        val r = town.endWeek(s)
        val summary = r.s1aState().history.last()
        assertTrue("штамп «Нужное куплено»", summary.mandatoryCovered)
        assertFalse("штамп «По плану»", summary.planKept)
        assertEquals(
            "строка итога",
            "Еда и уход были всю неделю! В новом конверте попробуем по плану",
            r.s1aOutcome().line,
        )
    }

    @Test
    fun `строка итога с двумя штампами берёт шаг по порядку проверок`() {
        var withoutSavings = S1aStand.planned(40, 0, 0)
        withoutSavings = town.buyAt(withoutSavings, "food_basic", "shop_market", Source.NEED).s1aState()
        withoutSavings = town.buyAt(withoutSavings, "care_soap", "shop_foma", Source.NEED).s1aState()
        assertEquals(
            "нужное и план есть, копилки нет",
            "Еда и уход были всю неделю! В новом конверте попробуем отложить",
            town.endWeek(withoutSavings).s1aOutcome().line,
        )

        val withoutFood = S1aStand.planned(0, 20, 10)
        assertEquals(
            "план и копилка есть, нужного нет",
            "Траты — по плану! В новом конверте начнём с корма и мыла",
            town.endWeek(withoutFood).s1aOutcome().line,
        )
    }

    @Test
    fun `окно конца недели называет непокрытые нужды`() {
        val s = S1aStand.planned(40, 20, 30)
        assertEquals(
            "обе нужды открыты",
            listOf(
                "Еда на этой неделе не куплена — Финни проголодается",
                "Уход на этой неделе не куплен — Финни испачкается",
            ),
            town.weekEndPreview(s),
        )
        val fed = town.buyAt(s, "food_basic", "shop_market", Source.NEED).s1aState()
        assertEquals(
            "остался уход",
            listOf("Уход на этой неделе не куплен — Финни испачкается"),
            town.weekEndPreview(fed),
        )
        val cared = town.buyAt(fed, "care_soap", "shop_foma", Source.NEED).s1aState()
        assertEquals("всё куплено", emptyList<String>(), town.weekEndPreview(cared))
    }

    // ---------- §5 + §11 шаги 5–10 и §15 неделя 1 → 2 ----------

    @Test
    fun `демо-путь недели один заканчивается тремя штампами и новым конвертом`() {
        // строку зарплаты в конверт пишет S1b, здесь она задана руками
        val s = demoWeekOne().copy(envelope = listOf(LedgerEntry("Смена: Помочь Марте", 6)))
        assertEquals("кошелёк после шага 8", 22, s.balance)
        assertEquals("копилка после шага 8", 40, s.savings)
        assertEquals("окно конца недели короткое", emptyList<String>(), town.weekEndPreview(s))

        val night = town.endWeek(s).s1aState()
        assertEquals("кошелёк новой недели", 128, night.balance)
        assertEquals("сумма журнала", 128, night.ledger.sumOf { it.amount })
        assertEquals("зарплата в итоге", 6, night.history.last().shiftEarned)
        assertEquals("прибавка роста", 3, night.history.last().score)
        assertEquals("конверт опустел", emptyList<LedgerEntry>(), night.envelope)
        assertS1aInvariants("после итога недели 1", night)

        assertEquals("ночью план не разложить", "Сейчас ночь — сначала проснёмся", town.confirmPlan(night).s1aRefusal())

        val morning = town.wake(night)
        val s2 = morning.s1aState()
        assertEquals("день новой недели", 1, s2.day)
        assertFalse("утро не наступило", s2.asleep)
        assertEquals("строка конверта", "Новый конверт: карманные 100 + зарплата 6", morning.s1aOutcome().line)
        assertS1aInvariants("после утра недели 2", s2)
    }

    @Test
    fun `утро нового конверта называет только пришедшие источники`() {
        val plain = town.wake(town.endWeek(S1aStand.planned(40, 20, 30)).s1aState())
        assertEquals("только карманные", "Новый конверт: карманные 100", plain.s1aOutcome().line)

        // конверт с зарплатой и бонусом взрослого — строки S1b
        val rich = S1aStand.planned(40, 20, 30).copy(
            envelope = listOf(
                LedgerEntry("Смена: Булочки в ряд", 12),
                LedgerEntry("Бонус от взрослого: за помощь по дому", 10),
            ),
        )
        val richMorning = town.wake(town.endWeek(rich).s1aState())
        assertEquals(
            "карманные, зарплата и бонус",
            "Новый конверт: карманные 100 + зарплата 12 + бонус 10",
            richMorning.s1aOutcome().line,
        )
    }

    // ---------- §5. Что поменяем ----------

    @Test
    fun `после хорошей недели с мечтой предлагается только «В копилку»`() {
        val s = town.endWeek(economy.chooseGoal(demoWeekOne(), S1aStand.townGoal("goal_scooter")).ok()).s1aState()
        assertEquals(
            "поправки",
            listOf(Recovery.PlanTweak(TweakDir.SAVINGS, S1aStand.step)),
            town.planTweaks(s, s.history.last()),
        )
    }

    @Test
    fun `без мечты и без промаха по нужному поправок нет`() {
        val s = town.endWeek(demoWeekOne()).s1aState()
        assertEquals("поправки", emptyList<Recovery.PlanTweak>(), town.planTweaks(s, s.history.last()))
    }

    @Test
    fun `непокрытое нужное и перерасход дают поправку «Нужное»`() {
        val uncovered = town.endWeek(S1aStand.planned(40, 20, 30)).s1aState()
        assertEquals(
            "нужное не куплено",
            listOf(Recovery.PlanTweak(TweakDir.NEED, S1aStand.step)),
            town.planTweaks(uncovered, uncovered.history.last()),
        )

        var over = S1aStand.planned(20, 20, 30)
        over = town.buyAt(over, "food_lunch", "shop_foma", Source.RESERVE).s1aState()
        over = town.buyAt(over, "care_soap", "shop_foma", Source.RESERVE).s1aState()
        val overEnd = town.endWeek(over).s1aState()
        assertTrue("нужное покрыто", overEnd.history.last().mandatoryCovered)
        assertTrue("перерасхода по нужному нет", overEnd.history.last().factMandatory > overEnd.history.last().plan.mandatory)
        assertEquals(
            "доплата из запаса",
            listOf(Recovery.PlanTweak(TweakDir.NEED, S1aStand.step)),
            town.planTweaks(overEnd, overEnd.history.last()),
        )
    }

    @Test
    fun `поправки событий идут после «Нужного» без дублей и не больше трёх`() {
        val done = town.endWeek(demoWeekOne()).s1aState()
        val summary = done.history.last()
        // события пишет S1c — ходы событий недели 1 заданы руками
        val s = done.copy(
            events = listOf(
                EventState("p1_list", EventStatus.DONE, Verdict.MISTAKE, 1, summary.period, 1),
                EventState("c2_lamp", EventStatus.DONE, Verdict.MISTAKE, 2, summary.period, 2),
                EventState("c3_almost", EventStatus.DONE, Verdict.MISTAKE, 2, summary.period, 3),
                EventState("p3_price_up", EventStatus.DONE, Verdict.MISTAKE, 3, summary.period, 3),
                EventState("pk4_everyone_has", EventStatus.DONE, Verdict.MISTAKE, 2, summary.period, 3),
            ),
        )
        assertEquals(
            "поправки",
            listOf(
                Recovery.PlanTweak(TweakDir.NEED, 10),
                Recovery.PlanTweak(TweakDir.RESERVE, 10),
                Recovery.PlanTweak(TweakDir.SAVINGS, 10),
            ),
            town.planTweaks(s, summary),
        )
    }

    @Test
    fun `одна и та же поправка из правила и из события предлагается один раз`() {
        val start = economy.chooseGoal(S1aStand.planned(40, 20, 30), S1aStand.townGoal("goal_scooter")).ok()
        val done = town.endWeek(start).s1aState()
        val summary = done.history.last()
        assertFalse("нужное покрыто — правило не даст поправку «Нужное»", summary.mandatoryCovered)
        assertTrue("мечты нет — не будет поправки «В копилку»", done.goal != null)
        // ход события пишет S1c; p1_list, исход 1 (MISTAKE) — recovery с тем же PLAN_TWEAK(NEED, 10)
        val s = done.copy(events = listOf(EventState("p1_list", EventStatus.DONE, Verdict.MISTAKE, 1, summary.period, 1)))
        assertEquals(
            "поправки без дубля «Нужного»",
            listOf(
                Recovery.PlanTweak(TweakDir.NEED, S1aStand.step),
                Recovery.PlanTweak(TweakDir.SAVINGS, S1aStand.step),
            ),
            town.planTweaks(s, summary),
        )
    }

    @Test
    fun `ходы событий без исхода и с неизвестным id пропускаются`() {
        val done = town.endWeek(demoWeekOne()).s1aState()
        val summary = done.history.last()
        val s = done.copy(
            events = listOf(
                EventState("p1_list", EventStatus.ACTIVE, null, null, summary.period, 1),
                EventState("c2_lamp", EventStatus.DONE, Verdict.MISTAKE, -1, summary.period, 1),
                EventState("нет_такого", EventStatus.DONE, Verdict.MISTAKE, 1, summary.period, 1),
                EventState("c3_almost", EventStatus.DONE, Verdict.GOOD, 0, summary.period, 2),
                EventState("pk4_everyone_has", EventStatus.DONE, Verdict.MISTAKE, 2, summary.period - 1, 3),
            ),
        )
        assertEquals("поправки", emptyList<Recovery.PlanTweak>(), town.planTweaks(s, summary))
    }

    @Test
    fun `невыполнимая поправка не предлагается`() {
        val done = town.endWeek(demoWeekOne()).s1aState()
        val summary = done.history.last()
        // «Запас +10» — это «Хочу» −10, в заготовке «Хочу» должно хватить
        val withEvent = done.copy(events = listOf(EventState("c2_lamp", EventStatus.DONE, Verdict.MISTAKE, 2, summary.period, 2)))
        assertEquals(
            "при «Хочу» 20 поправка запаса выполнима",
            listOf(Recovery.PlanTweak(TweakDir.RESERVE, 10)),
            town.planTweaks(withEvent, summary),
        )
        val emptyWant = withEvent.copy(plan = withEvent.plan.copy(optional = 0))
        assertEquals(
            "при пустом «Хочу» поправки запаса нет",
            emptyList<Recovery.PlanTweak>(),
            town.planTweaks(emptyWant, summary),
        )
    }

    @Test
    fun `поправка правит заготовку плана`() {
        val s = town.endWeek(demoWeekOne()).s1aState()
        assertEquals("заготовка", BudgetPlan(40, 20, 30, confirmed = false), s.plan)

        val need = town.chooseTweak(s, Recovery.PlanTweak(TweakDir.NEED, 10)).s1aState()
        assertEquals("«Нужное» +10", BudgetPlan(50, 20, 30, confirmed = false), need.plan)

        val savings = town.chooseTweak(s, Recovery.PlanTweak(TweakDir.SAVINGS, 10)).s1aState()
        assertEquals("«В копилку» +10", BudgetPlan(40, 20, 40, confirmed = false), savings.plan)

        val want = town.chooseTweak(s, Recovery.PlanTweak(TweakDir.WANT, 10)).s1aState()
        assertEquals("«Хочу» +10", BudgetPlan(40, 30, 30, confirmed = false), want.plan)

        val reserve = town.chooseTweak(s, Recovery.PlanTweak(TweakDir.RESERVE, 10)).s1aState()
        assertEquals("«Запас +10» это «Хочу» −10", BudgetPlan(40, 10, 30, confirmed = false), reserve.plan)

        assertEquals("«Как было»", s, town.chooseTweak(s, null).s1aState())
        assertEquals("строка поправки", "", town.chooseTweak(s, null).s1aOutcome().line)
    }

    @Test
    fun `отказы поправки`() {
        val s = town.endWeek(demoWeekOne()).s1aState()
        assertEquals("нет питомца", "Сначала создай питомца", town.chooseTweak(economy.newGame(true), null).s1aRefusal())
        assertEquals(
            "план уже готов",
            "План уже готов",
            town.chooseTweak(S1aStand.planned(40, 20, 30), Recovery.PlanTweak(TweakDir.NEED, 10)).s1aRefusal(),
        )
        val noWant = s.copy(plan = s.plan.copy(optional = 0))
        assertEquals(
            "«Хочу» пусто",
            "В «Хочу» меньше 10",
            town.chooseTweak(noWant, Recovery.PlanTweak(TweakDir.RESERVE, 10)).s1aRefusal(),
        )
        // граничный кошелёк: в заготовке уже весь кошелёк
        val tight = s.copy(balance = 90)
        assertEquals(
            "в плане больше монет",
            "В плане больше монет, чем в кошельке",
            town.chooseTweak(tight, Recovery.PlanTweak(TweakDir.WANT, 10)).s1aRefusal(),
        )
    }

    // ---------- §6. Места и посещение ----------

    @Test
    fun `вещи пола встают на свободные места а лишняя ложится в сундук`() {
        // цветок и кресло стоят на spot_1 и spot_4 всегда, свободного пола остаётся два места
        var s = S1aStand.planned(0, 100, 0)
        s = town.buyAt(s, "fun_ball", "shop_foma", Source.WANT).s1aState()
        assertEquals("мячик на полу", "fun_ball", s.placed["spot_5"])
        s = town.buyAt(s, "fun_rug", "shop_foma", Source.WANT).s1aState()
        assertEquals("коврик на следующем месте пола", "fun_rug", s.placed["spot_6"])
        s = town.buyAt(s, "fun_robot", "shop_foma", Source.WANT).s1aState()
        assertTrue("робот не в сундуке", "fun_robot" in s.owned)
        assertFalse("свободных мест пола не осталось, а робот встал на место", "fun_robot" in s.placed.values)
        assertEquals("занятых мест", 2, s.placed.size)
    }

    @Test
    fun `вещь стола и вещь стены встают на места своего типа`() {
        var s = S1aStand.planned(0, 100, 0)
        s = town.buyAt(s, "fun_book", "shop_foma", Source.WANT).s1aState()
        assertEquals("книжка на столе", "fun_book", s.placed["spot_3"])
        s = town.buyAt(s, "fun_kite", "shop_foma", Source.WANT).s1aState()
        assertEquals("змей на стене", "fun_kite", s.placed["spot_2"])
        s = town.buyAt(s, "fun_spinner", "shop_foma", Source.WANT).s1aState()
        assertTrue("вертушка не в сундуке", "fun_spinner" in s.owned)
        assertFalse("стол один, вертушке места нет", "fun_spinner" in s.placed.values)
    }

    @Test
    fun `вещь переставляется с места на место а прежняя уходит в сундук`() {
        var s = S1aStand.planned(0, 100, 0)
        s = town.buyAt(s, "fun_ball", "shop_foma", Source.WANT).s1aState()
        s = town.buyAt(s, "fun_rug", "shop_foma", Source.WANT).s1aState()

        val moved = town.place(s, "spot_6", "fun_ball").s1aState()
        assertEquals("мячик не переехал", "fun_ball", moved.placed["spot_6"])
        assertNull("прежнее место мячика не освободилось", moved.placed["spot_5"])
        assertFalse("коврик остался на месте", "fun_rug" in moved.placed.values)
        assertTrue("коврик пропал из сундука", "fun_rug" in moved.owned)
        assertEquals("строка", "", town.place(s, "spot_6", "fun_ball").s1aOutcome().line)

        val freed = town.place(moved, "spot_6", null).s1aState()
        assertNull("место не освободилось", freed.placed["spot_6"])
        assertTrue("мячик пропал из сундука", "fun_ball" in freed.owned)
    }

    @Test
    fun `отказы расстановки вещей`() {
        val s = town.buyAt(S1aStand.planned(0, 100, 0), "fun_ball", "shop_foma", Source.WANT).s1aState()
        assertEquals("нет места", "Такого места нет", town.place(s, "spot_нет", "fun_ball").s1aRefusal())
        assertEquals("занято цветком", "Здесь стоит цветок", town.place(s, "spot_1", "fun_ball").s1aRefusal())
        assertEquals("занято креслом", "Здесь стоит кресло", town.place(s, "spot_4", "fun_ball").s1aRefusal())
        assertEquals("вещи нет в сундуке", "Этой вещи нет в сундуке", town.place(s, "spot_2", "fun_kite").s1aRefusal())
        assertEquals("мячик не вешают на стену", "Эта вещь сюда не встанет", town.place(s, "spot_2", "fun_ball").s1aRefusal())
        assertEquals("места не поменялись", mapOf("spot_5" to "fun_ball"), s.placed)
    }

    @Test
    fun `вход в место записывает посещение и самую низкую цену недели`() {
        val s0 = S1aStand.planned(40, 20, 30, demo = false)
        val market = town.visit(s0, "market")
        val s1 = market.state
        assertEquals("строка", "", market.line)
        assertEquals("посещения", listOf("market"), s1.visited)
        assertEquals("корм у реки", SeenPrice("shop_market", 20, 1), s1.seenPrices["food_basic"])
        assertEquals("мыло у реки", SeenPrice("shop_market", 12, 1), s1.seenPrices["care_soap"])
        assertNull("супер-корм открывается со второй недели", s1.seenPrices["food_super"])

        val s2 = town.visit(s1, "foma").state
        assertEquals("посещения", listOf("market", "foma"), s2.visited)
        assertEquals("корм у Фомы дороже — холодильник помнит реку", SeenPrice("shop_market", 20, 1), s2.seenPrices["food_basic"])
        assertEquals("мыло у Фомы дешевле", SeenPrice("shop_foma", 10, 1), s2.seenPrices["care_soap"])

        val again = town.visit(s2, "foma").state
        assertEquals("место записано дважды", listOf("market", "foma"), again.visited)
    }

    @Test
    fun `неизвестное место и место без лавки`() {
        val s0 = S1aStand.planned(40, 20, 30)
        assertEquals("неизвестное место изменило состояние", s0, town.visit(s0, "нет_такого").state)

        val home = town.visit(s0, "home").state
        assertEquals("посещения", listOf("home"), home.visited)
        assertEquals("цены дома", emptyMap<String, SeenPrice>(), home.seenPrices)
    }

    // ---------- Тексты ----------

    @Test
    fun `тексты дня и итога недели без стоп-слов и без рода`() {
        val ready = S1aStand.planned(40, 20, 30)
        val fed = town.buyAt(ready, "food_basic", "shop_market", Source.NEED).s1aState()
        val night = town.endWeek(demoWeekOne()).s1aState()
        val texts = buildList {
            addAll(town.sleep(ready).s1aTexts())
            addAll(town.sleep(fed).s1aTexts())
            addAll(town.sleep(S1aStand.profile()).s1aTexts())
            addAll(town.wake(ready).s1aTexts())
            addAll(town.wake(night).s1aTexts())
            addAll(town.endWeek(demoWeekOne()).s1aTexts())
            addAll(town.endWeek(ready).s1aTexts())
            addAll(town.endWeek(S1aStand.planned(40, 20, 30, demo = false)).s1aTexts())
            addAll(town.weekEndPreview(ready))
            addAll(town.chooseTweak(night, Recovery.PlanTweak(TweakDir.NEED, 10)).s1aTexts())
            addAll(town.chooseTweak(ready, Recovery.PlanTweak(TweakDir.NEED, 10)).s1aTexts())
            addAll(town.place(fed, "spot_1", "fun_ball").s1aTexts())
            addAll(town.place(fed, "spot_нет", null).s1aTexts())
        }
        S1aChildText.check("день и итог недели", texts)
    }

    // ---------- Сцены ----------

    /** Демо-путь §11 шаги 5–8: план 40 / 20 / 30, корм 20, шарик 8, мыло 10, мечта и взнос 10 из «Хочу». */
    private fun demoWeekOne(): GameState {
        var s = S1aStand.planned(40, 20, 30)
        s = town.buyAt(s, "food_basic", "shop_market", Source.NEED).s1aState()
        s = town.buyAt(s, "fun_balloon", "shop_market", Source.WANT).s1aState()
        s = town.buyAt(s, "care_soap", "shop_foma", Source.NEED).s1aState()
        return town.deposit(s, Source.WANT, 10).s1aState()
    }
}
