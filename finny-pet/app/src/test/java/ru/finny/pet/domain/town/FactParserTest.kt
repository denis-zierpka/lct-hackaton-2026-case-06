package ru.finny.pet.domain.town

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.pet.domain.Need
import ru.finny.pet.domain.Theme

/**
 * Оракул словаря событий «Городка» (TOWN-S0a §2–§4): каноническая строка <-> значение,
 * каждая ветка ошибки разбора, эталонная запись события.
 */
class FactParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    // --- помощники: разбор в значение и печать обратно ---

    private fun fact(canonical: String, value: Fact) {
        assertEquals("разбор «$canonical»", value, TownCodec.fact(canonical))
        assertEquals("печать значения $value", canonical, TownCodec.print(value))
    }

    private fun condition(canonical: String, value: Condition) {
        assertEquals("разбор «$canonical»", value, TownCodec.condition(canonical))
        assertEquals("печать значения $value", canonical, TownCodec.print(value))
    }

    private fun effect(canonical: String, value: EventEffect) {
        assertEquals("разбор «$canonical»", value, TownCodec.effect(canonical))
        assertEquals("печать значения $value", canonical, TownCodec.print(value))
    }

    private fun recovery(canonical: String, value: Recovery) {
        assertEquals("разбор «$canonical»", value, TownCodec.recovery(canonical))
        assertEquals("печать значения $value", canonical, TownCodec.print(value))
    }

    private fun trigger(canonical: String, value: Trigger) {
        assertEquals("разбор «$canonical»", value, TownCodec.trigger(canonical))
        assertEquals("печать значения $value", canonical, TownCodec.print(value))
    }

    /** Ошибка разбора: IllegalArgumentException, в message — вход после trim в ёлочках. */
    private fun bad(input: String, parse: (String) -> Any) {
        var parsed: Any? = null
        val thrown: Throwable? = try {
            parsed = parse(input)
            null
        } catch (t: Throwable) {
            t
        }
        assertNotNull("ожидалась ошибка на «$input», получено значение $parsed", thrown)
        val error: Throwable = thrown ?: return
        assertTrue(
            "на «$input» ожидался IllegalArgumentException, получен ${error.javaClass.name}",
            error is IllegalArgumentException,
        )
        val expected = "«" + input.trim() + "»"
        assertTrue(
            "сообщение об ошибке на «$input» должно содержать $expected, получено: ${error.message}",
            error.message?.contains(expected) == true,
        )
    }

    // ---------------------------------------------------------------- Fact

    @Test
    fun `факты без аргументов разбираются и печатаются`() {
        fact("SKIP", Fact.Skip)
        fact("DEPOSIT", Fact.Deposit)
        fact("WITHDRAW", Fact.Withdraw)
        fact("CHECK_CHANGE", Fact.CheckChange)
        fact("MAKE_CARD", Fact.MakeCard)
    }

    @Test
    fun `покупка товара потребности и тега дают три разные цели`() {
        val byItem = TownCodec.fact("BUY:food_basic")
        val byNeed = TownCodec.fact("BUY:FOOD")
        val byTag = TownCodec.fact("BUY:#gift")
        assertEquals("BUY по идентификатору товара", Fact.Buy(BuyTarget.Item("food_basic")), byItem)
        assertEquals("BUY по потребности", Fact.Buy(BuyTarget.ByNeed(Need.FOOD)), byNeed)
        assertEquals("BUY по тегу", Fact.Buy(BuyTarget.Tag("gift")), byTag)
        assertTrue("товар и потребность не должны совпадать", byItem != byNeed)
        assertTrue("потребность и тег не должны совпадать", byNeed != byTag)
        assertTrue("товар и тег не должны совпадать", byItem != byTag)
        assertEquals("BUY:food_basic", TownCodec.print(byItem))
        assertEquals("BUY:FOOD", TownCodec.print(byNeed))
        assertEquals("BUY:#gift", TownCodec.print(byTag))
    }

    @Test
    fun `покупка товара из каждой банки`() {
        for (s in Source.entries) {
            fact("BUY:food_basic:$s", Fact.Buy(BuyTarget.Item("food_basic"), s))
        }
    }

    @Test
    fun `покупка по каждой потребности из перечисления`() {
        for (n in Need.entries) {
            fact("BUY:$n", Fact.Buy(BuyTarget.ByNeed(n)))
            fact("BUY:$n:NEED", Fact.Buy(BuyTarget.ByNeed(n), Source.NEED))
        }
    }

    @Test
    fun `покупка по тегу с источником`() {
        fact("BUY:#gift:WANT", Fact.Buy(BuyTarget.Tag("gift"), Source.WANT))
        fact("BUY:#gift:TRANSFER_NEED", Fact.Buy(BuyTarget.Tag("gift"), Source.TRANSFER_NEED))
    }

    @Test
    fun `покупка по цене для каждого ранга`() {
        for (r in PriceRank.entries) {
            fact("BUY_AT:fun_robot:$r", Fact.BuyAt("fun_robot", r))
        }
        assertTrue(
            "дешёвая и дорогая покупка — разные факты",
            TownCodec.fact("BUY_AT:fun_robot:CHEAPEST") != TownCodec.fact("BUY_AT:fun_robot:DEARER"),
        )
    }

    @Test
    fun `план покрывает список или нет`() {
        fact("PLAN:NEED>=LIST", Fact.Plan(true))
        fact("PLAN:NEED<LIST", Fact.Plan(false))
        assertTrue(
            "две стороны сравнения — разные факты",
            TownCodec.fact("PLAN:NEED>=LIST") != TownCodec.fact("PLAN:NEED<LIST"),
        )
    }

    @Test
    fun `починка с банкой и без банки`() {
        fact("REPAIR:home_lamp", Fact.Repair("home_lamp", null))
        for (s in Source.entries) {
            fact("REPAIR:home_lamp:$s", Fact.Repair("home_lamp", s))
        }
        assertNull("без третьей части источник пуст", (TownCodec.fact("REPAIR:home_lamp") as Fact.Repair).source)
    }

    @Test
    fun `цель посещение и конец недели хранят идентификаторы`() {
        fact("MAKE_GOAL:goal_scooter", Fact.MakeGoal("goal_scooter"))
        fact("ATTEND:club_art", Fact.Attend("club_art"))
        fact("WEEK_END_NO:FOOD", Fact.WeekEndNo(Need.FOOD))
        fact("WEEK_END_NO:CARE", Fact.WeekEndNo(Need.CARE))
        assertTrue(
            "разные цели — разные факты",
            TownCodec.fact("MAKE_GOAL:goal_scooter") != TownCodec.fact("MAKE_GOAL:goal_bike"),
        )
    }

    // ----------------------------------------------------------- Condition

    @Test
    fun `условия без аргументов`() {
        condition("beforePlan", Condition.BeforePlan)
        condition("afterPlan", Condition.AfterPlan)
        condition("hasGoal", Condition.HasGoal)
    }

    @Test
    fun `числовые условия хранят своё число`() {
        condition("week>=2", Condition.WeekAtLeast(2))
        condition("day==3", Condition.DayIs(3))
        condition("savingsPct>=20", Condition.SavingsPctAtLeast(20))
        condition("want>=1", Condition.WantAtLeast(1))
        condition("reserve>=15", Condition.ReserveAtLeast(15))
        condition("week>=0", Condition.WeekAtLeast(0))
        assertTrue(
            "week>=2 и week>=3 — разные условия",
            TownCodec.condition("week>=2") != TownCodec.condition("week>=3"),
        )
        assertTrue(
            "want>=1 и reserve>=1 — разные условия",
            TownCodec.condition("want>=1") != TownCodec.condition("reserve>=1"),
        )
    }

    @Test
    fun `условия про товары жителей и потребности`() {
        condition("notBought:FOOD", Condition.NotBought(Need.FOOD))
        condition("notBought:CARE", Condition.NotBought(Need.CARE))
        condition("owns:fun_robot", Condition.Owns("fun_robot"))
        condition("notOwned:fun_robot", Condition.NotOwned("fun_robot"))
        condition("notBroken:home_lamp", Condition.NotBroken("home_lamp"))
        condition("resident:foma", Condition.ResidentArrived("foma"))
        assertTrue(
            "owns и notOwned — разные условия",
            TownCodec.condition("owns:fun_robot") != TownCodec.condition("notOwned:fun_robot"),
        )
    }

    @Test
    fun `любой из вариантов разбирается в список`() {
        condition(
            "want>=1 || reserve>=1",
            Condition.AnyOf(listOf(Condition.WantAtLeast(1), Condition.ReserveAtLeast(1))),
        )
        condition(
            "afterPlan || hasGoal || week>=2",
            Condition.AnyOf(listOf(Condition.AfterPlan, Condition.HasGoal, Condition.WeekAtLeast(2))),
        )
        val two = TownCodec.condition("want>=1 || reserve>=1") as Condition.AnyOf
        assertEquals("два варианта", 2, two.options.size)
        val three = TownCodec.condition("afterPlan || hasGoal || week>=2") as Condition.AnyOf
        assertEquals("три варианта", 3, three.options.size)
        assertTrue("порядок вариантов значим", two != Condition.AnyOf(two.options.reversed()))
    }

    // --------------------------------------------------------- EventEffect

    @Test
    fun `монеты приходят из каждого источника`() {
        for (s in CoinSource.entries) {
            effect("COINS(5, $s)", EventEffect.Coins(5, s))
        }
        effect("COINS(0, WINDFALL)", EventEffect.Coins(0, CoinSource.WINDFALL))
        assertTrue(
            "разные суммы — разные эффекты",
            TownCodec.effect("COINS(5, WINDFALL)") != TownCodec.effect("COINS(6, WINDFALL)"),
        )
        assertTrue(
            "разные источники — разные эффекты",
            TownCodec.effect("COINS(5, WINDFALL)") != TownCodec.effect("COINS(5, CHANGE_RETURN)"),
        )
    }

    @Test
    fun `недоданная сдача`() {
        effect("SHORT_CHANGE(3)", EventEffect.ShortChange(3))
        effect("SHORT_CHANGE(0)", EventEffect.ShortChange(0))
    }

    @Test
    fun `изменение каждой шкалы вверх и вниз`() {
        for (st in Stat.entries) {
            effect("STAT($st, -10)", EventEffect.StatChange(st, -10))
            effect("STAT($st, 10)", EventEffect.StatChange(st, 10))
            effect("STAT($st, 0)", EventEffect.StatChange(st, 0))
        }
        assertEquals(
            "плюс на входе допустим, в печати его нет",
            EventEffect.StatChange(Stat.MOOD, 5),
            TownCodec.effect("STAT(MOOD, +5)"),
        )
        assertEquals("STAT(MOOD, 5)", TownCodec.print(TownCodec.effect("STAT(MOOD, +5)")))
    }

    @Test
    fun `эффекты с вещами`() {
        effect("ITEM_GIVE(fun_robot)", EventEffect.ItemGive("fun_robot"))
        effect("ITEM_BREAK(home_lamp)", EventEffect.ItemBreak("home_lamp"))
        effect("ITEM_FIX(home_lamp)", EventEffect.ItemFix("home_lamp"))
        assertTrue(
            "подарить и сломать — разные эффекты",
            TownCodec.effect("ITEM_GIVE(fun_robot)") != TownCodec.effect("ITEM_BREAK(fun_robot)"),
        )
    }

    @Test
    fun `цена и распродажа держатся до своего срока`() {
        for (u in Until.entries) {
            effect(
                "PRICE(food_basic, shop_foma, 35, $u)",
                EventEffect.Price("food_basic", "shop_foma", 35, u),
            )
            effect(
                "OFFER(fun_robot, shop_foma, 40, 25, $u)",
                EventEffect.Offer("fun_robot", "shop_foma", 40, 25, u),
            )
        }
        val offer = TownCodec.effect("OFFER(fun_robot, shop_foma, 40, 25, WEEK_END)") as EventEffect.Offer
        assertEquals("было", 40, offer.was)
        assertEquals("стало", 25, offer.now)
        assertTrue(
            "цена в разных лавках — разные эффекты",
            TownCodec.effect("PRICE(food_basic, shop_foma, 35, DAY_END)") !=
                TownCodec.effect("PRICE(food_basic, shop_lada, 35, DAY_END)"),
        )
    }

    @Test
    fun `записка наклейка переход и демо-цель`() {
        effect("NOTE(Сдачу надо считать)", EventEffect.Note("Сдачу надо считать"))
        effect("STICKER(st_robot_sale)", EventEffect.Sticker("st_robot_sale"))
        effect("GOTO(shop_foma)", EventEffect.Goto("shop_foma"))
        effect("DEMO_GOAL(80)", EventEffect.DemoGoal(80))
        effect("NONE", EventEffect.None)
    }

    @Test
    fun `в записке сохраняются запятые и скобки`() {
        val text = "Сдача, ещё сдача (мелочь) — проверь"
        effect("NOTE($text)", EventEffect.Note(text))
        assertEquals(
            "крайние пробелы записки срезаются",
            EventEffect.Note("привет"),
            TownCodec.effect("NOTE( привет )"),
        )
        assertEquals("NOTE(привет)", TownCodec.print(TownCodec.effect("NOTE( привет )")))
    }

    // ------------------------------------------------------------ Recovery

    @Test
    fun `восстановления без аргументов`() {
        recovery("RESERVE_TO_SAVINGS", Recovery.ReserveToSavings)
        recovery("RESERVE_TO_NEED", Recovery.ReserveToNeed)
        recovery("WANT_TO_NEED", Recovery.WantToNeed)
        recovery("RETRY_NEXT_WEEK", Recovery.RetryNextWeek)
        recovery("RETURN_LATER", Recovery.ReturnLater)
    }

    @Test
    fun `правка плана по каждому направлению`() {
        for (d in TweakDir.entries) {
            recovery("PLAN_TWEAK($d, 10)", Recovery.PlanTweak(d, 10))
        }
        assertTrue(
            "разные направления правки — разные восстановления",
            TownCodec.recovery("PLAN_TWEAK(NEED, 10)") != TownCodec.recovery("PLAN_TWEAK(WANT, 10)"),
        )
        assertTrue(
            "разный размер правки — разные восстановления",
            TownCodec.recovery("PLAN_TWEAK(NEED, 10)") != TownCodec.recovery("PLAN_TWEAK(NEED, 5)"),
        )
    }

    // ------------------------------------------------------------- Trigger

    @Test
    fun `триггеры входа и подтверждения плана`() {
        trigger("ENTER:shop_foma", Trigger.Enter("shop_foma"))
        trigger("PLAN_CONFIRMED", Trigger.PlanConfirmed)
        assertTrue(
            "вход в разные места — разные триггеры",
            TownCodec.trigger("ENTER:shop_foma") != TownCodec.trigger("ENTER:shop_lada"),
        )
    }

    // ------------------------------------------- пробелы на разрешённых местах

    @Test
    fun `пробелы по краям факта и триггера не мешают`() {
        assertEquals("SKIP", TownCodec.print(TownCodec.fact(" SKIP ")))
        assertEquals("BUY:food_basic:WANT", TownCodec.print(TownCodec.fact(" BUY:food_basic:WANT ")))
        assertEquals("PLAN_CONFIRMED", TownCodec.print(TownCodec.trigger(" PLAN_CONFIRMED ")))
        assertEquals("ENTER:shop_foma", TownCodec.print(TownCodec.trigger(" ENTER:shop_foma ")))
    }

    @Test
    fun `пробелы вокруг разделителя вариантов не мешают`() {
        val expected = Condition.AnyOf(listOf(Condition.WantAtLeast(1), Condition.ReserveAtLeast(1)))
        for (input in listOf("want>=1||reserve>=1", " want>=1 || reserve>=1 ", "want>=1 ||reserve>=1")) {
            assertEquals("разбор «$input»", expected, TownCodec.condition(input))
            assertEquals("печать «$input»", "want>=1 || reserve>=1", TownCodec.print(TownCodec.condition(input)))
        }
        assertEquals("afterPlan", TownCodec.print(TownCodec.condition(" afterPlan ")))
        assertEquals("week>=2", TownCodec.print(TownCodec.condition(" week>=2 ")))
    }

    @Test
    fun `пробелы у скобок и запятых печатаются канонически`() {
        val cases = mapOf(
            "COINS( 5, WINDFALL )" to "COINS(5, WINDFALL)",
            "COINS(5,WINDFALL)" to "COINS(5, WINDFALL)",
            "COINS(5 , WINDFALL)" to "COINS(5, WINDFALL)",
            " STAT(MOOD, -5) " to "STAT(MOOD, -5)",
            "OFFER( fun_robot , shop_foma , 40 , 25 , WEEK_END )" to
                "OFFER(fun_robot, shop_foma, 40, 25, WEEK_END)",
            "PRICE(food_basic,shop_foma,35,DAY_END)" to "PRICE(food_basic, shop_foma, 35, DAY_END)",
            " NONE " to "NONE",
        )
        for ((input, canonical) in cases) {
            assertEquals("печать «$input»", canonical, TownCodec.print(TownCodec.effect(input)))
        }
        assertEquals(
            "печать «PLAN_TWEAK( NEED , 10 )»",
            "PLAN_TWEAK(NEED, 10)",
            TownCodec.print(TownCodec.recovery("PLAN_TWEAK( NEED , 10 )")),
        )
    }

    // ---------------------------------------------------------- ошибки §3

    @Test
    fun `пустая строка не разбирается ни одним видом`() {
        for (input in listOf("", "   ")) {
            bad(input, TownCodec::fact)
            bad(input, TownCodec::condition)
            bad(input, TownCodec::effect)
            bad(input, TownCodec::recovery)
            bad(input, TownCodec::trigger)
        }
    }

    @Test
    fun `неизвестное имя не разбирается`() {
        bad("FLY", TownCodec::fact)
        bad("BUY_LATER:food_basic", TownCodec::fact)
        bad("moonPhase", TownCodec::condition)
        bad("week<=2", TownCodec::condition)
        bad("BOOM(5)", TownCodec::effect)
        bad("NAP", TownCodec::recovery)
        bad("EXIT:shop_foma", TownCodec::trigger)
    }

    @Test
    fun `неизвестное значение перечисления не разбирается`() {
        bad("BUY:food_basic:POCKET", TownCodec::fact)
        bad("BUY_AT:fun_robot:PRICIEST", TownCodec::fact)
        bad("REPAIR:home_lamp:POCKET", TownCodec::fact)
        bad("BUY:MONEY", TownCodec::fact)
        bad("COINS(5, GIFT)", TownCodec::effect)
        bad("STAT(ENERGY, -5)", TownCodec::effect)
        bad("PRICE(food_basic, shop_foma, 35, MONTH_END)", TownCodec::effect)
        bad("OFFER(fun_robot, shop_foma, 40, 25, MONTH_END)", TownCodec::effect)
        bad("PLAN_TWEAK(SNACK, 10)", TownCodec::recovery)
    }

    @Test
    fun `конец недели и непокупка принимают только еду и уход`() {
        for (n in Need.entries) {
            if (n == Need.FOOD || n == Need.CARE) continue
            bad("WEEK_END_NO:$n", TownCodec::fact)
            bad("notBought:$n", TownCodec::condition)
        }
        bad("WEEK_END_NO:FUN", TownCodec::fact)
        bad("notBought:FUN", TownCodec::condition)
    }

    @Test
    fun `недостающая или лишняя часть не разбирается`() {
        bad("BUY_AT:fun_robot", TownCodec::fact)
        bad("BUY_AT:fun_robot:CHEAPEST:WANT", TownCodec::fact)
        bad("SKIP:fun_robot", TownCodec::fact)
        bad("BUY:FOOD:", TownCodec::fact)
        bad("BUY:#", TownCodec::fact)
        bad("BUY:", TownCodec::fact)
        bad("MAKE_GOAL", TownCodec::fact)
        bad("owns:", TownCodec::condition)
        bad("resident:", TownCodec::condition)
        bad("OFFER(fun_robot, shop_foma, 40)", TownCodec::effect)
        bad("PRICE(food_basic, shop_foma, 35)", TownCodec::effect)
        bad("COINS(5)", TownCodec::effect)
        bad("NOTE(Сдача)хвост", TownCodec::effect)
        bad("NONE(1)", TownCodec::effect)
        bad("PLAN_TWEAK(NEED)", TownCodec::recovery)
        bad("RETURN_LATER:foma", TownCodec::recovery)
        bad("ENTER", TownCodec::trigger)
        bad("PLAN_CONFIRMED:foma", TownCodec::trigger)
    }

    @Test
    fun `не-число и число вне диапазона не разбираются`() {
        bad("week>=two", TownCodec::condition)
        bad("week>=9999999999", TownCodec::condition)
        bad("day==2147483648", TownCodec::condition)
        bad("COINS(пять, WINDFALL)", TownCodec::effect)
        bad("STAT(MOOD, 2147483648)", TownCodec::effect)
        bad("STAT(MOOD, -2147483649)", TownCodec::effect)
        bad("SHORT_CHANGE(1.5)", TownCodec::effect)
        bad("PLAN_TWEAK(NEED, x)", TownCodec::recovery)
    }

    @Test
    fun `знак у неотрицательного числа не разбирается`() {
        bad("COINS(+5, WINDFALL)", TownCodec::effect)
        bad("COINS(-5, WINDFALL)", TownCodec::effect)
        bad("SHORT_CHANGE(+3)", TownCodec::effect)
        bad("DEMO_GOAL(+80)", TownCodec::effect)
        bad("week>=+2", TownCodec::condition)
        bad("PLAN_TWEAK(NEED, +10)", TownCodec::recovery)
    }

    @Test
    fun `идентификатор не по лексике не разбирается`() {
        bad("BUY:Food", TownCodec::fact)
        bad("BUY:food-basic", TownCodec::fact)
        bad("BUY:1food", TownCodec::fact)
        bad("BUY:#Gift", TownCodec::fact)
        bad("GOTO(1a)", TownCodec::effect)
        bad("STICKER(St_robot)", TownCodec::effect)
        bad("owns:Fun_robot", TownCodec::condition)
        bad("ENTER:Shop", TownCodec::trigger)
    }

    @Test
    fun `сравнение не из таблицы не разбирается`() {
        bad("week>2", TownCodec::condition)
        bad("want>0", TownCodec::condition)
        bad("day>=3", TownCodec::condition)
        bad("savingsPct>20", TownCodec::condition)
        bad("PLAN:NEED>LIST", TownCodec::fact)
        bad("PLAN:WANT>=LIST", TownCodec::fact)
        bad("PLAN:NEED<=LIST", TownCodec::fact)
    }

    @Test
    fun `пустая записка и пустая сторона разделителя не разбираются`() {
        bad("NOTE()", TownCodec::effect)
        bad("NOTE(   )", TownCodec::effect)
        bad("afterPlan ||", TownCodec::condition)
        bad("|| hasGoal", TownCodec::condition)
        bad("afterPlan || || hasGoal", TownCodec::condition)
    }

    @Test
    fun `пробел не на разрешённом месте не разбирается`() {
        bad("week >= 2", TownCodec::condition)
        bad("week>= 2", TownCodec::condition)
        bad("owns: fun_robot", TownCodec::condition)
        bad("BUY: FOOD", TownCodec::fact)
        bad("BUY :FOOD", TownCodec::fact)
        bad("SK IP", TownCodec::fact)
        bad("NOTE (Сдача)", TownCodec::effect)
        bad("COINS (5, WINDFALL)", TownCodec::effect)
        bad("PLAN_TWEAK (NEED, 10)", TownCodec::recovery)
        bad("ENTER: shop_foma", TownCodec::trigger)
    }

    // ------------------------------------------------- строковые сериализаторы

    @Test
    fun `факт читается и пишется как строка json`() {
        assertEquals(
            "разбор строки json",
            Fact.Buy(BuyTarget.Item("fun_robot"), Source.WANT),
            json.decodeFromString<Fact>("\"BUY:fun_robot:WANT\""),
        )
        assertEquals(
            "запись строки json",
            "\"BUY:fun_robot:WANT\"",
            json.encodeToString<Fact>(Fact.Buy(BuyTarget.Item("fun_robot"), Source.WANT)),
        )
        assertEquals("\"SKIP\"", json.encodeToString<Fact>(Fact.Skip))
    }

    @Test
    fun `условие эффект восстановление и триггер читаются как строки json`() {
        assertEquals(
            Condition.AnyOf(listOf(Condition.WantAtLeast(1), Condition.ReserveAtLeast(1))),
            json.decodeFromString<Condition>("\"want>=1 || reserve>=1\""),
        )
        assertEquals(
            "\"want>=1 || reserve>=1\"",
            json.encodeToString<Condition>(
                Condition.AnyOf(listOf(Condition.WantAtLeast(1), Condition.ReserveAtLeast(1))),
            ),
        )
        assertEquals(
            EventEffect.Coins(5, CoinSource.CHANGE_RETURN),
            json.decodeFromString<EventEffect>("\"COINS(5, CHANGE_RETURN)\""),
        )
        assertEquals(
            "\"COINS(5, CHANGE_RETURN)\"",
            json.encodeToString<EventEffect>(EventEffect.Coins(5, CoinSource.CHANGE_RETURN)),
        )
        assertEquals(
            Recovery.PlanTweak(TweakDir.NEED, 10),
            json.decodeFromString<Recovery>("\"PLAN_TWEAK(NEED, 10)\""),
        )
        assertEquals(
            "\"PLAN_TWEAK(NEED, 10)\"",
            json.encodeToString<Recovery>(Recovery.PlanTweak(TweakDir.NEED, 10)),
        )
        assertEquals(Trigger.Enter("shop_foma"), json.decodeFromString<Trigger>("\"ENTER:shop_foma\""))
        assertEquals("\"ENTER:shop_foma\"", json.encodeToString<Trigger>(Trigger.Enter("shop_foma")))
    }

    @Test
    fun `ошибка разбора изнутри json остаётся ошибкой аргумента`() {
        bad("FLY") { json.decodeFromString<Fact>("\"" + it + "\"") }
        bad("week>2") { json.decodeFromString<Condition>("\"" + it + "\"") }
        bad("COINS(+5, WINDFALL)") { json.decodeFromString<EventEffect>("\"" + it + "\"") }
    }

    // ------------------------------------------------- эталонная запись §4

    @Test
    fun `эталонная запись разбирается по полям`() {
        val e = json.decodeFromString<EventDef>(REFERENCE)
        assertEquals("id", "c1_robot_sale", e.id)
        assertEquals("kind", EventKind.OFFER, e.kind)
        assertEquals("title", "Распродажа робота", e.title)
        assertEquals("theme", Theme.SAVINGS, e.theme)
        assertEquals("place", "foma", e.place)
        assertEquals("resident", "foma", e.resident)
        assertEquals("arrives", Arrives(2, 2), e.arrives)
        assertEquals(
            "requires",
            listOf(Condition.AfterPlan, Condition.NotOwned("fun_robot")),
            e.requires,
        )
        assertEquals(
            "setup",
            listOf(EventEffect.Offer("fun_robot", "shop_foma", 40, 25, Until.WEEK_END)),
            e.setup,
        )
        assertEquals("intro", "Робот! Было 40 — стало 25. Такая распродажа бывает.", e.intro)
        assertEquals("сколько исходов", 4, e.outcomes.size)
        assertEquals("факт первого исхода", Fact.Skip, e.outcomes[0].fact)
        assertEquals("вердикт первого исхода", Verdict.GOOD, e.outcomes[0].verdict)
        assertEquals("реплика первого исхода", "Скидка — это дешевле, а не нужнее.", e.outcomes[0].line)
        assertEquals(
            "факт второго исхода",
            Fact.Buy(BuyTarget.Item("fun_robot"), Source.WANT),
            e.outcomes[1].fact,
        )
        assertEquals("вердикт второго исхода", Verdict.OK, e.outcomes[1].verdict)
        assertEquals("вердикт третьего исхода", Verdict.MISTAKE, e.outcomes[2].verdict)
        assertEquals(
            "восстановление третьего исхода",
            listOf(Recovery.ReserveToSavings),
            e.outcomes[2].recovery,
        )
        assertEquals(
            "восстановление четвёртого исхода",
            listOf(Recovery.ReserveToNeed, Recovery.PlanTweak(TweakDir.NEED, 10)),
            e.outcomes[3].recovery,
        )
        assertEquals(
            "факт четвёртого исхода",
            Fact.Buy(BuyTarget.Item("fun_robot"), Source.TRANSFER_NEED),
            e.outcomes[3].fact,
        )
        assertEquals("default", EventDefault(Until.WEEK_END, Fact.Skip), e.default)
        assertEquals("sticker", "st_robot_sale", e.sticker)
        assertEquals("repeat", Repeat(afterWeeks = 3, pool = true, weeks = emptyList()), e.repeat)
        assertEquals(
            "demo",
            Demo(
                startNow = true,
                keepOnShelf = true,
                setup = emptyList(),
                wrongPath = "Положить в «Хочу» меньше 25 и купить через «Взять из „Нужного“»",
            ),
            e.demo,
        )
    }

    @Test
    fun `у эталонной записи умолчания на месте пропущенных полей`() {
        val e = json.decodeFromString<EventDef>(REFERENCE)
        assertEquals("без триггеров", emptyList<Trigger>(), e.triggers)
        assertEquals("приоритет по умолчанию", 0, e.priority)
        assertEquals("параметры по умолчанию", EventParams(), e.params)
        assertNull("продолжения нет", e.followUp)
        for (o in e.outcomes) {
            assertNull("условие исхода без when пусто, исход «${o.line}»", o.condition)
            assertEquals("эффектов у исхода нет, исход «${o.line}»", emptyList<EventEffect>(), o.effects)
        }
        assertEquals("восстановления первого исхода нет", emptyList<Recovery>(), e.outcomes[0].recovery)
    }

    @Test
    fun `эталонная запись печатается обратно тем же json`() {
        val e = json.decodeFromString<EventDef>(REFERENCE)
        val printed = json.encodeToString(e)
        assertEquals(
            "печать записи должна совпасть с эталоном, получено: $printed",
            Json.parseToJsonElement(REFERENCE),
            Json.parseToJsonElement(printed),
        )
    }

    @Test
    fun `условие исхода читается из ключа when`() {
        val text = """
            {
              "id": "c9_ask", "kind": "ASK", "title": "Вопрос", "intro": "Спросили",
              "outcomes": [
                { "fact": "SKIP", "verdict": "OK", "line": "Ладно", "when": "week>=2",
                  "effects": ["STAT(MOOD, +5)"] }
              ]
            }
        """.trimIndent()
        val e = json.decodeFromString<EventDef>(text)
        assertEquals("условие из when", Condition.WeekAtLeast(2), e.outcomes[0].condition)
        assertEquals(
            "эффекты исхода",
            listOf(EventEffect.StatChange(Stat.MOOD, 5)),
            e.outcomes[0].effects,
        )
        assertNull("темы у записи нет", e.theme)
        val printed = Json.parseToJsonElement(json.encodeToString(e)).jsonObject
        val outcome = printed["outcomes"]!!.jsonArray[0].jsonObject
        assertTrue("в записи должен быть ключ when, получено ${outcome.keys}", outcome.containsKey("when"))
        assertTrue(
            "ключа condition в записи быть не должно, получено ${outcome.keys}",
            !outcome.containsKey("condition"),
        )
    }

    @Test
    fun `битая строка факта внутри записи события — ошибка аргумента`() {
        bad("SKIP:x") { s ->
            json.decodeFromString<EventDef>(
                """{"id":"e1","kind":"ASK","title":"Т","intro":"И",
                   "outcomes":[{"fact":"$s","verdict":"OK","line":"Л"}]}""",
            )
        }
    }

    private companion object {
        /** Эталонная запись §4 спеки TOWN-S0a, дословно. */
        const val REFERENCE = """
{
  "id": "c1_robot_sale", "kind": "OFFER", "title": "Распродажа робота", "theme": "SAVINGS",
  "place": "foma", "resident": "foma",
  "arrives": { "week": 2, "day": 2 },
  "requires": ["afterPlan", "notOwned:fun_robot"],
  "setup": ["OFFER(fun_robot, shop_foma, 40, 25, WEEK_END)"],
  "intro": "Робот! Было 40 — стало 25. Такая распродажа бывает.",
  "outcomes": [
    { "fact": "SKIP", "verdict": "GOOD", "line": "Скидка — это дешевле, а не нужнее." },
    { "fact": "BUY:fun_robot:WANT", "verdict": "OK", "line": "На «Хочу» хватило — это твой выбор." },
    { "fact": "BUY:fun_robot:SAVINGS", "verdict": "MISTAKE", "line": "Робот из копилки — мечта отодвинулась.",
      "recovery": ["RESERVE_TO_SAVINGS"] },
    { "fact": "BUY:fun_robot:TRANSFER_NEED", "verdict": "MISTAKE", "line": "Робот из «Нужного» — на корм может не хватить.",
      "recovery": ["RESERVE_TO_NEED", "PLAN_TWEAK(NEED, 10)"] }
  ],
  "default": { "at": "WEEK_END", "fact": "SKIP" },
  "sticker": "st_robot_sale",
  "repeat": { "afterWeeks": 3, "pool": true },
  "demo": { "keepOnShelf": true, "wrongPath": "Положить в «Хочу» меньше 25 и купить через «Взять из „Нужного“»" }
}
"""
    }
}
