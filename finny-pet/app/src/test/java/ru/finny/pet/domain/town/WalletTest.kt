package ru.finny.pet.domain.town

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import ru.finny.pet.domain.BudgetPlan
import ru.finny.pet.domain.Economy
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.Goal
import ru.finny.pet.domain.LedgerEntry
import ru.finny.pet.domain.TestContent
import ru.finny.pet.domain.ok
import org.junit.Test

/**
 * Общий стенд оракула «Городка» (docs/tasks/TOWN-S1a.md).
 * Состояния строятся настоящим путём игрока: newGame → createPet → setPlan → Town.confirmPlan → …
 * Руками (copy) собираются только граничные банки и события — каждый раз с комментарием.
 */
internal object S1aStand {
    val content = TestContent.content
    val economy = Economy(content)
    val town = Town(content)
    val prices = Prices(content)
    val allowance: Int get() = content.rules.allowance
    val step: Int get() = content.rules.planStep
    val days: Int get() = content.town!!.rules.daysPerWeek
    val goalFromItemMin: Int get() = content.town!!.rules.customGoalFromItemMin

    fun profile(demo: Boolean = true): GameState =
        economy.createPet(economy.newGame(demo), "Финни", "cat", "orange").ok()

    /** Путь игрока до кассы: раскладка монет и «Готово». */
    fun planned(m: Int, o: Int, sv: Int, demo: Boolean = true): GameState =
        town.confirmPlan(economy.setPlan(profile(demo), m, o, sv).ok()).s1aState()

    fun townGoal(id: String): Goal = content.town!!.goals.first { it.id == id }
        .let { Goal(it.id, it.title, it.emoji, it.price) }
}

internal fun TownResult.s1aOutcome(): TownOutcome =
    (this as? TownResult.Done)?.outcome ?: error("ожидался Done, а пришло $this")

internal fun TownResult.s1aState(): GameState = s1aOutcome().state

internal fun TownResult.s1aRefusal(): String =
    (this as? TownResult.Refused)?.line ?: error("ожидался Refused, а пришло $this")

/** Тексты действия: строка ребёнку и «Почему?». */
internal fun TownResult.s1aTexts(): List<String> = when (this) {
    is TownResult.Done -> listOf(outcome.line) + outcome.why
    is TownResult.Refused -> listOf(line)
}

/** Инварианты §8, проверяются после каждой функции Town. */
internal fun assertS1aInvariants(where: String, s: GameState) {
    assertTrue("$where: «Нужное» ${s.jarNeed} меньше нуля", s.jarNeed >= 0)
    assertTrue("$where: «Хочу» ${s.jarWant} меньше нуля", s.jarWant >= 0)
    assertTrue("$where: запас ${s.reserve} меньше нуля", s.reserve >= 0)
    assertTrue("$where: копилка ${s.savings} меньше нуля", s.savings >= 0)
    assertEquals("$where: сумма журнала не равна балансу", s.balance, s.ledger.sumOf { it.amount })
    if (!s.plan.confirmed) {
        assertEquals("$where: до подтверждения плана «Нужное» не пусто", 0, s.jarNeed)
        assertEquals("$where: до подтверждения плана «Хочу» не пусто", 0, s.jarWant)
    }
}

/** Стоп-слова §9.2 №3, №7, №10 и слова с родом — списки ContentValidationTest. */
internal object S1aChildText {
    private val rush = listOf("сегодня", "осталось", "до конца недели", "скорее", "пока не", "успей", "последн")
    private val debt = listOf("долг", "одолж", "взаймы", "занять")
    private val shame = listOf(
        "цена лени", "ленив", "зачем", "транжир", "жадин", "зря", "впуст", "провал",
        "плохо", "неправильно", "ошибк",
    )
    private val gendered = listOf(
        "отложил", "планировал", "потратил", "положил", "взял", "проголодался", "запачкался",
        "пришёл", "пришла", "захотел", "захотела", "купил", "купила", "выбрал", "выбрала",
        "бегал", "подвернул", "достал", "видел", "видела", "доволен", "довольна", "устроил", "чистенький",
    )

    private fun wholeWord(word: String) =
        Regex("(?<![\\p{L}\\p{N}_])" + Regex.escape(word) + "(?![\\p{L}\\p{N}_])")

    fun check(where: String, texts: List<String>) {
        assertTrue("$where: нечего проверять — движок не вернул ни одной строки", texts.isNotEmpty())
        texts.filter { it.isNotBlank() }.forEach { text ->
            val low = text.lowercase()
            (rush + debt + shame).forEach { w ->
                assertTrue("$where: стоп-слово «$w» в строке «$text»", !low.contains(w))
            }
            gendered.forEach { w ->
                assertTrue("$where: слово с родом «$w» в строке «$text»", !wholeWord(w).containsMatchIn(low))
            }
        }
    }
}

/**
 * Кошелёк «Городка» (TOWN-S1a §4): подтверждение плана со взносом, взнос после плана,
 * снятие до плана, перенос между банками, мечта из вещи (§6) и инварианты §8 на цепочке недели.
 */
class WalletTest {

    private val town = S1aStand.town
    private val economy = S1aStand.economy
    private val stamp = "Штамп «По плану» на этой неделе не получится"

    // ---------- §4. confirmPlan ----------

    @Test
    fun `подтверждение плана сразу ссыпает «В копилку» в свинку и раскладывает банки`() {
        val before = economy.setPlan(S1aStand.profile(), 40, 20, 30).ok()
        val r = town.confirmPlan(before)
        val s = r.s1aState()
        assertEquals("копилка после взноса", 30, s.savings)
        assertEquals("взнос недели", 30, s.depositedThisPeriod)
        assertEquals("кошелёк", 70, s.balance)
        assertEquals("«Нужное»", 40, s.jarNeed)
        assertEquals("«Хочу»", 20, s.jarWant)
        assertEquals("запас", 10, s.reserve)
        assertTrue("план не подтверждён", s.plan.confirmed)
        assertEquals("версия схемы после игры движком", 1, s.stateVersion)
        assertEquals("строка", "План готов! В копилку +30, запас на всякий случай: 10", r.s1aOutcome().line)
        assertEquals(
            "«Почему?»",
            listOf(
                "Сначала откладываем, потом тратим — так мечта ближе",
                "Запас — на нужное и на всякий случай, не на «хочу»",
            ),
            r.s1aOutcome().why,
        )
        assertS1aInvariants("после confirmPlan", s)
    }

    @Test
    fun `план без копилки говорит только про запас`() {
        val r = town.confirmPlan(economy.setPlan(S1aStand.profile(), 40, 20, 0).ok())
        val s = r.s1aState()
        assertEquals("копилка", 0, s.savings)
        assertEquals("кошелёк", 100, s.balance)
        assertEquals("запас", 40, s.reserve)
        assertEquals("строка", "План готов! Запас на всякий случай: 40", r.s1aOutcome().line)
    }

    @Test
    fun `план ровно на весь кошелёк оставляет запас ноль`() {
        val r = town.confirmPlan(economy.setPlan(S1aStand.profile(), 50, 20, 30).ok())
        val s = r.s1aState()
        assertEquals("«Нужное»", 50, s.jarNeed)
        assertEquals("«Хочу»", 20, s.jarWant)
        assertEquals("запас", 0, s.reserve)
        assertEquals("строка", "План готов! В копилку +30, запас на всякий случай: 0", r.s1aOutcome().line)
        assertS1aInvariants("после плана на весь кошелёк", s)
    }

    @Test
    fun `отказы подтверждения плана идут по порядку и не меняют состояние`() {
        val noPet = economy.newGame(true)
        assertEquals("нет питомца", "Сначала создай питомца", town.confirmPlan(noPet).s1aRefusal())

        val fresh = S1aStand.profile()
        assertEquals("ничего не разложено", "Разложи хотя бы часть монет", town.confirmPlan(fresh).s1aRefusal())

        // граничная раскладка: setPlan сам не пустит план больше кошелька, поэтому собираем руками
        val tooBig = fresh.copy(plan = BudgetPlan(60, 50, 0))
        assertEquals(
            "план больше кошелька",
            "В плане больше монет, чем в кошельке",
            town.confirmPlan(tooBig).s1aRefusal(),
        )

        val ready = S1aStand.planned(40, 20, 30)
        assertEquals(
            "план уже подтверждён",
            "План уже готов — до нового конверта он не меняется",
            town.confirmPlan(ready).s1aRefusal(),
        )

        // ночь: настоящий путь — конец недели оставляет ребёнка спящим с новой заготовкой плана
        val night = town.endWeek(ready).s1aState()
        assertTrue("после конца недели не ночь", night.asleep)
        assertEquals("ночью план не подтверждают", "Сейчас ночь — сначала проснёмся", town.confirmPlan(night).s1aRefusal())
        assertTrue("после ночного отказа план подтвердился", !night.plan.confirmed)
    }

    // ---------- §4. deposit ----------

    @Test
    fun `взнос из запаса уменьшает запас и растит копилку`() {
        val s0 = S1aStand.planned(40, 20, 30)
        val jarNeedBefore = s0.jarNeed
        val r = town.deposit(s0, Source.RESERVE, 10)
        val s = r.s1aState()
        assertEquals("копилка", 40, s.savings)
        assertEquals("кошелёк", 60, s.balance)
        assertEquals("запас", 0, s.reserve)
        assertEquals("«Хочу» не трогаем", 20, s.jarWant)
        assertEquals("взнос не трогает «Нужное»", jarNeedBefore, s.jarNeed)
        assertEquals("строка", "Копилка +10", r.s1aOutcome().line)
        assertEquals(
            "«Почему?» без мечты",
            listOf("Запас: 10 → 0", "Копилка: 30 → 40"),
            r.s1aOutcome().why,
        )
        assertEquals("предпросмотр", listOf("Запас: 10 → 0"), town.depositPreview(s0, Source.RESERVE, 10))
        assertS1aInvariants("после взноса из запаса", s)
    }

    @Test
    fun `взнос из «Хочу» на демо-пути двигает срок мечты с четырёх конвертов на три`() {
        val s = demoStep8()
        assertEquals("кошелёк после трёх покупок", 32, s.balance)
        assertEquals("копилка до взноса", 30, s.savings)
        assertEquals("«Хочу» до взноса", 12, s.jarWant)

        assertEquals(
            "предпросмотр взноса",
            listOf("«Хочу»: 12 → 2", "Самокат: ≈ 4 ✉ → ≈ 3 ✉"),
            town.depositPreview(s, Source.WANT, 10),
        )
        val r = town.deposit(s, Source.WANT, 10)
        val after = r.s1aState()
        assertEquals("копилка", 40, after.savings)
        assertEquals("«Хочу»", 2, after.jarWant)
        assertEquals("«Нужное» взнос не трогает", 10, after.jarNeed)
        assertEquals("кошелёк", 22, after.balance)
        assertEquals("запас остаётся на всякий случай", 10, after.reserve)
        assertEquals("строка", "Копилка +10", r.s1aOutcome().line)
        assertEquals(
            "«Почему?» со сроком мечты",
            listOf("«Хочу»: 12 → 2", "Копилка: 30 → 40", "Самокат: ≈ 4 ✉ → ≈ 3 ✉"),
            r.s1aOutcome().why,
        )
        assertS1aInvariants("после взноса из «Хочу»", after)
    }

    @Test
    fun `предпросмотр взноса говорит «хватит на мечту» когда копилка добирает цену`() {
        val s0 = S1aStand.planned(10, 0, 60)
        val s = economy.chooseGoal(s0, S1aStand.townGoal("goal_paints")).ok()
        assertEquals("копилка", 60, s.savings)
        assertEquals("запас", 30, s.reserve)
        assertEquals(
            "предпросмотр",
            listOf("Запас: 30 → 10", "Краски: хватит на мечту!"),
            town.depositPreview(s, Source.RESERVE, 20),
        )
        val why = town.deposit(s, Source.RESERVE, 20).s1aOutcome().why
        assertEquals("«Почему?»", listOf("Запас: 30 → 10", "Копилка: 60 → 80", "Краски: хватит на мечту!"), why)
    }

    @Test
    fun `отказы взноса идут по порядку и не меняют состояние`() {
        assertEquals("нет питомца", "Сначала создай питомца", town.deposit(economy.newGame(true), Source.RESERVE, 10).s1aRefusal())

        val beforePlan = S1aStand.profile()
        assertEquals(
            "до раскладки",
            "Отложить можно после раскладки",
            town.deposit(beforePlan, Source.RESERVE, 10).s1aRefusal(),
        )
        assertEquals("предпросмотр до раскладки пуст", emptyList<String>(), town.depositPreview(beforePlan, Source.RESERVE, 10))

        val s = S1aStand.planned(40, 20, 30)
        assertEquals("ноль монет", "Выбери сумму больше нуля", town.deposit(s, Source.RESERVE, 0).s1aRefusal())
        assertEquals("минус", "Выбери сумму больше нуля", town.deposit(s, Source.RESERVE, -5).s1aRefusal())
        assertEquals("из «Нужного» откладывать нельзя", "Так отложить нельзя", town.deposit(s, Source.NEED, 10).s1aRefusal())
        assertEquals("из копилки в копилку нельзя", "Так отложить нельзя", town.deposit(s, Source.SAVINGS, 10).s1aRefusal())
        assertEquals("больше запаса", "В запасе только 10", town.deposit(s, Source.RESERVE, 11).s1aRefusal())
        assertEquals("больше «Хочу»", "В «Хочу» только 20", town.deposit(s, Source.WANT, 21).s1aRefusal())
        assertEquals("предпросмотр недопустимого ввода пуст", emptyList<String>(), town.depositPreview(s, Source.RESERVE, 11))
        assertEquals("состояние после отказов", S1aStand.planned(40, 20, 30), s)

        val night = town.endWeek(s).s1aState()
        assertEquals("ночью не откладывают", "Сейчас ночь — сначала проснёмся", town.deposit(night, Source.RESERVE, 10).s1aRefusal())
    }

    @Test
    fun `весь запас можно отложить а на монету больше уже нельзя`() {
        val s = S1aStand.planned(40, 20, 30)
        assertEquals("весь запас", 40, town.deposit(s, Source.RESERVE, 10).s1aState().savings)
        assertEquals("на монету больше", "В запасе только 10", town.deposit(s, Source.RESERVE, 11).s1aRefusal())
    }

    // ---------- §4. withdraw ----------

    @Test
    fun `снятие до плана уходит в «Не разложено» и отодвигает мечту`() {
        val s = weekTwoMorning()
        assertEquals("копилка после первой недели", 40, s.savings)
        val balanceBefore = s.balance
        val r = town.withdraw(s, 20)
        val after = r.s1aState()
        assertEquals("копилка", 20, after.savings)
        assertEquals("монеты пришли в кошелёк", balanceBefore + 20, after.balance)
        assertEquals("снято за период", 20, after.withdrawnThisPeriod)
        assertEquals("строка", "Из копилки 20 — в «Не разложено»", r.s1aOutcome().line)
        assertEquals(
            "«Почему?»",
            listOf("Копилка: 40 → 20", "Самокат: ≈ 3 ✉ → ≈ 4 ✉"),
            r.s1aOutcome().why,
        )
        assertS1aInvariants("после снятия до плана", after)
    }

    @Test
    fun `предпросмотр снятия предупреждает про штамп пока в плане есть «В копилку»`() {
        val s = weekTwoMorning()
        assertEquals("в заготовке есть «В копилку»", 30, s.plan.savings)
        assertEquals(
            "предпросмотр со штампом",
            listOf("Копилка: 40 → 20", "Самокат: ≈ 3 ✉ → ≈ 4 ✉", stamp),
            town.withdrawPreview(s, 20),
        )
        val withoutSavings = economy.setPlan(s, 40, 20, 0).ok()
        assertEquals(
            "без «В копилку» штампу ничего не грозит",
            listOf("Копилка: 40 → 20", "Самокат: ≈ 3 ✉ → ≈ 4 ✉"),
            town.withdrawPreview(withoutSavings, 20),
        )
        assertEquals("недопустимый ввод", emptyList<String>(), town.withdrawPreview(s, 41))
    }

    @Test
    fun `отказы снятия идут по порядку и не меняют состояние`() {
        assertEquals("нет питомца", "Сначала создай питомца", town.withdraw(economy.newGame(true), 10).s1aRefusal())

        val ready = S1aStand.planned(40, 20, 30)
        assertEquals(
            "после раскладки",
            "После раскладки из копилки платят только у кассы",
            town.withdraw(ready, 10).s1aRefusal(),
        )
        val night = town.endWeek(ready).s1aState()
        assertEquals("ночью", "Сейчас ночь — сначала проснёмся", town.withdraw(night, 10).s1aRefusal())

        val morning = town.wake(night).s1aState()
        assertEquals("ноль", "Выбери сумму больше нуля", town.withdraw(morning, 0).s1aRefusal())
        assertEquals("вся копилка", 0, town.withdraw(morning, 30).s1aState().savings)
        assertEquals("на монету больше копилки", "В копилке только 30", town.withdraw(morning, 31).s1aRefusal())
        assertEquals("копилка после отказов", 30, morning.savings)
    }

    // ---------- §4. transfer ----------

    @Test
    fun `перенос из запаса в «Нужное» предупреждает что штампа не будет`() {
        val s = S1aStand.planned(40, 20, 30)
        assertEquals(
            "предпросмотр переноса",
            listOf("Запас: 10 → 0", "«Нужное»: 40 → 50", "Потратишь больше плана — штампа «По плану» не будет"),
            town.transferPreview(s, Source.RESERVE, Source.NEED, 10),
        )
        val r = town.transfer(s, Source.RESERVE, Source.NEED, 10)
        val after = r.s1aState()
        assertEquals("«Нужное»", 50, after.jarNeed)
        assertEquals("запас", 0, after.reserve)
        assertEquals("«Хочу»", 20, after.jarWant)
        assertEquals("кошелёк не меняется", s.balance, after.balance)
        assertEquals("строка", "«Нужное» +10", r.s1aOutcome().line)
        assertEquals("«Почему?»", listOf("Запас: 10 → 0", "«Нужное»: 40 → 50"), r.s1aOutcome().why)
        assertS1aInvariants("после переноса из запаса", after)
    }

    @Test
    fun `перенос из «Хочу» в «Нужное» опустошает «Хочу»`() {
        val s = S1aStand.planned(40, 20, 30)
        val r = town.transfer(s, Source.WANT, Source.NEED, 20)
        val after = r.s1aState()
        assertEquals("«Нужное»", 60, after.jarNeed)
        assertEquals("«Хочу»", 0, after.jarWant)
        assertEquals("запас", 10, after.reserve)
        assertEquals("строка", "«Нужное» +20", r.s1aOutcome().line)
        assertEquals("«Почему?»", listOf("«Хочу»: 20 → 0", "«Нужное»: 40 → 60"), r.s1aOutcome().why)
    }

    @Test
    fun `после переноса из «Нужного» за хотелку предупреждения про штамп нет`() {
        // «Хочу» 20 меньше 35 — книжка оплачена переносом из «Нужного»: jn 40 → 25, план по нужному ещё цел
        val s = town.buyAt(S1aStand.planned(40, 20, 30), "fun_book", "shop_foma", Source.TRANSFER_NEED).s1aState()
        assertEquals("«Нужное» после переноса", 25, s.jarNeed)
        assertEquals("«Хочу» после переноса", 0, s.jarWant)
        assertEquals(
            "предпросмотр без строки про штамп",
            listOf("Запас: 10 → 0", "«Нужное»: 25 → 35"),
            town.transferPreview(s, Source.RESERVE, Source.NEED, 10),
        )
        assertEquals("из пустого «Хочу» переносить нечего", "В «Хочу» только 0", town.transfer(s, Source.WANT, Source.NEED, 10).s1aRefusal())
    }

    @Test
    fun `отказы переноса идут по порядку и не меняют состояние`() {
        assertEquals("нет питомца", "Сначала создай питомца", town.transfer(economy.newGame(true), Source.RESERVE, Source.NEED, 10).s1aRefusal())

        val beforePlan = S1aStand.profile()
        assertEquals("до раскладки", "Перенос — после раскладки", town.transfer(beforePlan, Source.RESERVE, Source.NEED, 10).s1aRefusal())
        assertEquals("предпросмотр до раскладки пуст", emptyList<String>(), town.transferPreview(beforePlan, Source.RESERVE, Source.NEED, 10))

        val s = S1aStand.planned(40, 20, 30)
        assertEquals("ноль", "Выбери сумму больше нуля", town.transfer(s, Source.RESERVE, Source.NEED, 0).s1aRefusal())
        assertEquals("«Нужное» в «Хочу»", "Так перенести нельзя", town.transfer(s, Source.NEED, Source.WANT, 10).s1aRefusal())
        assertEquals("запас в копилку не переносом", "Так перенести нельзя", town.transfer(s, Source.RESERVE, Source.SAVINGS, 10).s1aRefusal())
        assertEquals("больше запаса", "В запасе только 10", town.transfer(s, Source.RESERVE, Source.NEED, 11).s1aRefusal())
        assertEquals("больше «Хочу»", "В «Хочу» только 20", town.transfer(s, Source.WANT, Source.NEED, 21).s1aRefusal())
        assertEquals("предпросмотр недопустимого ввода пуст", emptyList<String>(), town.transferPreview(s, Source.NEED, Source.WANT, 10))
        assertEquals("состояние после отказов", S1aStand.planned(40, 20, 30), s)

        val night = town.endWeek(s).s1aState()
        assertEquals("ночью", "Сейчас ночь — сначала проснёмся", town.transfer(night, Source.RESERVE, Source.NEED, 10).s1aRefusal())
    }

    // ---------- §6. Мечта из вещи и получение мечты ----------

    @Test
    fun `«Сделать мечтой» берёт базовую цену лавки`() {
        val s = S1aStand.planned(40, 20, 30)
        val r = town.makeGoal(s, "fun_tent")
        val after = r.s1aState()
        assertEquals("мечта", "item:fun_tent", after.goal?.id)
        assertEquals("название мечты", "Домик-палатка", after.goal?.title)
        assertEquals("значок мечты", "⛺", after.goal?.emoji)
        assertEquals("цена мечты", 60, after.goal?.price)
        assertEquals("строка", "Мечта: домик-палатка — 60", r.s1aOutcome().line)
        assertEquals("монеты не двигаются", s.balance, after.balance)
        assertS1aInvariants("после выбора мечты из вещи", after)
    }

    @Test
    fun `мечтой нельзя сделать дешёвую вещь и расходную радость`() {
        val s = S1aStand.planned(40, 20, 30)
        assertEquals("нет такого товара", "Этого товара здесь нет", town.makeGoal(s, "нет_такого").s1aRefusal())
        assertEquals("мячик дешевле порога", "Мечтой можно сделать вещь от 50", town.makeGoal(s, "fun_ball").s1aRefusal())
        assertEquals("мороженое не остаётся дома", "Мечтой можно сделать вещь от 50", town.makeGoal(s, "fun_icecream").s1aRefusal())
        assertEquals("порог из контента", 50, S1aStand.goalFromItemMin)

        // копилка 60 = цена палатки: покупка из копилки кладёт вещь в сундук
        val owned = town.buyAt(S1aStand.planned(0, 0, 60), "fun_tent", "shop_foma", Source.SAVINGS).s1aState()
        assertTrue("палатка не попала в сундук", "fun_tent" in owned.owned)
        assertEquals("палатка уже дома", "Эта вещь уже есть дома", town.makeGoal(owned, "fun_tent").s1aRefusal())
    }

    @Test
    fun `полученная мечта из вещи встаёт на свободное место дома`() {
        val s0 = town.makeGoal(S1aStand.planned(0, 0, 60), "fun_tent").s1aState()
        assertEquals("копилка", 60, s0.savings)
        val r = town.achieveGoal(s0)
        val s = r.s1aState()
        assertEquals("копилка после мечты", 0, s.savings)
        assertNull("мечта осталась выбранной", s.goal)
        assertEquals("мечта в списке достигнутых", 1, s.achievedGoals.size)
        assertTrue("палатки нет в сундуке", "fun_tent" in s.owned)
        assertEquals("палатка не встала на первое свободное место пола", "fun_tent", s.placed["spot_5"])
        assertEquals("строка", "Мечта сбылась: Домик-палатка!", r.s1aOutcome().line)
        val diary = s.diary.last()
        assertEquals("строка дневника", "Мечта сбылась: домик-палатка", diary.text)
        assertEquals("неделя строки дневника", s.period, diary.period)
        assertEquals("день строки дневника", s.day, diary.day)
        assertS1aInvariants("после получения мечты", s)
    }

    @Test
    fun `мечта городка не кладёт вещь в сундук`() {
        val s0 = economy.chooseGoal(S1aStand.planned(0, 0, 80), S1aStand.townGoal("goal_paints")).ok()
        val r = town.achieveGoal(s0)
        val s = r.s1aState()
        assertEquals("сундук пополнился", emptyList<String>(), s.owned)
        assertEquals("места заняты", emptyMap<String, String>(), s.placed)
        assertEquals("копилка", 0, s.savings)
        assertEquals("строка", "Мечта сбылась: Краски!", r.s1aOutcome().line)
        assertEquals("строка дневника", "Мечта сбылась: краски", s.diary.last().text)
    }

    @Test
    fun `отказы получения мечты идут по порядку`() {
        assertEquals("нет питомца", "Сначала создай питомца", town.achieveGoal(economy.newGame(true)).s1aRefusal())
        val s = S1aStand.planned(40, 20, 30)
        assertEquals("нет мечты", "Сначала выбери мечту", town.achieveGoal(s).s1aRefusal())
        val withGoal = economy.chooseGoal(s, S1aStand.townGoal("goal_scooter")).ok()
        assertEquals("до мечты далеко", "До мечты ещё 120", town.achieveGoal(withGoal).s1aRefusal())
        val night = town.endWeek(withGoal).s1aState()
        assertEquals("ночью", "Сейчас ночь — сначала проснёмся", town.achieveGoal(night).s1aRefusal())
    }

    // ---------- §8. Инварианты на цепочке недели ----------

    @Test
    fun `инварианты держатся после каждого действия недели`() {
        var s = S1aStand.profile()
        assertS1aInvariants("новый профиль", s)

        s = economy.setPlan(s, 30, 20, 20).ok()
        assertS1aInvariants("раскладка до «Готово»", s)

        s = town.confirmPlan(s).s1aState()
        assertS1aInvariants("после «Готово»", s)
        assertEquals("банки после плана", listOf(30, 20, 30), listOf(s.jarNeed, s.jarWant, s.reserve))

        // хотелка дороже «Хочу»: перенос из «Нужного»
        s = town.buyAt(s, "fun_book", "shop_foma", Source.TRANSFER_NEED).s1aState()
        assertS1aInvariants("после покупки переносом из «Нужного»", s)
        assertEquals("«Нужное» после переноса", 15, s.jarNeed)
        assertEquals("«Хочу» после переноса", 0, s.jarWant)

        // доплата из запаса за нужное
        var jarNeedBefore = s.jarNeed
        s = town.buyAt(s, "food_basic", "shop_market", Source.RESERVE).s1aState()
        assertS1aInvariants("после доплаты из запаса", s)
        assertEquals("доплата из запаса не обнулила «Нужное»", 0, s.jarNeed)
        assertTrue("«Нужное» должно было уйти в ноль, было $jarNeedBefore", jarNeedBefore > 0)
        assertEquals("запас после доплаты", 25, s.reserve)

        // непредвиденное из запаса «Нужного» не касается
        jarNeedBefore = s.jarNeed
        s = town.buyAt(s, "care_doctor", null, Source.RESERVE).s1aState()
        assertS1aInvariants("после приёма доктора", s)
        assertEquals("непредвиденное тронуло «Нужное»", jarNeedBefore, s.jarNeed)
        assertEquals("запас после приёма", 15, s.reserve)

        s = town.transfer(s, Source.RESERVE, Source.NEED, 10).s1aState()
        assertS1aInvariants("после переноса запаса в «Нужное»", s)
        assertEquals("«Нужное» после переноса", 10, s.jarNeed)

        s = town.buyAt(s, "care_soap", "shop_foma", Source.NEED).s1aState()
        assertS1aInvariants("после покупки мыла", s)
        assertEquals("«Нужное» после мыла", 0, s.jarNeed)

        jarNeedBefore = s.jarNeed
        s = town.deposit(s, Source.RESERVE, 5).s1aState()
        assertS1aInvariants("после взноса из запаса", s)
        assertEquals("взнос тронул «Нужное»", jarNeedBefore, s.jarNeed)
        assertEquals("копилка", 25, s.savings)

        s = town.sleep(s).s1aState()
        assertS1aInvariants("после сна", s)
        s = town.wake(s).s1aState()
        assertS1aInvariants("после пробуждения", s)
        assertEquals("день", 2, s.day)

        val ownedBefore = s.owned
        s = town.endWeek(s).s1aState()
        assertS1aInvariants("после конца недели", s)
        assertEquals("сундук уменьшился", ownedBefore, s.owned)
        assertEquals("банки к новой неделе", listOf(0, 0), listOf(s.jarNeed, s.jarWant))

        s = town.wake(s).s1aState()
        assertS1aInvariants("после утра новой недели", s)

        s = town.withdraw(s, 10).s1aState()
        assertS1aInvariants("после снятия до плана", s)
        assertEquals("копилка", 15, s.savings)
    }

    @Test
    fun `конверт в кошелёк не входит и приходит в журнал отдельными строками`() {
        // строки конверта пишет S1b, здесь они заданы руками
        val s = S1aStand.planned(40, 20, 30).copy(
            envelope = listOf(
                LedgerEntry("Смена: Помочь Марте", 6),
                LedgerEntry("Бонус от взрослого: за помощь по дому", 10),
            ),
        )
        assertEquals("конверт попал в кошелёк", 70, s.balance)
        assertEquals("сумма журнала не равна балансу", s.balance, s.ledger.sumOf { it.amount })

        val after = town.endWeek(s).s1aState()
        assertEquals("конверт пришёл в кошелёк", 70 + S1aStand.allowance + 16, after.balance)
        assertEquals("конверт не опустел", emptyList<LedgerEntry>(), after.envelope)
        assertEquals(
            "строки конверта в конце журнала",
            listOf(LedgerEntry("Смена: Помочь Марте", 6), LedgerEntry("Бонус от взрослого: за помощь по дому", 10)),
            after.ledger.takeLast(2),
        )
        val summary = after.history.last()
        assertEquals("зарплата в итоге недели", 6, summary.shiftEarned)
        assertEquals("бонус взрослого в итоге недели", 10, summary.parentBonus)
        assertS1aInvariants("после прихода конверта", after)
    }

    // ---------- Тексты ----------

    @Test
    fun `тексты кошелька без стоп-слов и без рода`() {
        val s = S1aStand.planned(40, 20, 30)
        val withGoal = economy.chooseGoal(s, S1aStand.townGoal("goal_scooter")).ok()
        val texts = buildList {
            addAll(town.confirmPlan(economy.setPlan(S1aStand.profile(), 40, 20, 30).ok()).s1aTexts())
            addAll(town.confirmPlan(s).s1aTexts())
            addAll(town.deposit(withGoal, Source.RESERVE, 10).s1aTexts())
            addAll(town.deposit(withGoal, Source.WANT, 20).s1aTexts())
            addAll(town.deposit(withGoal, Source.NEED, 10).s1aTexts())
            addAll(town.deposit(withGoal, Source.RESERVE, 99).s1aTexts())
            addAll(town.depositPreview(withGoal, Source.WANT, 10))
            addAll(town.transfer(s, Source.RESERVE, Source.NEED, 10).s1aTexts())
            addAll(town.transfer(s, Source.WANT, Source.NEED, 20).s1aTexts())
            addAll(town.transfer(s, Source.NEED, Source.WANT, 10).s1aTexts())
            addAll(town.transferPreview(s, Source.RESERVE, Source.NEED, 10))
            addAll(town.makeGoal(s, "fun_tent").s1aTexts())
            addAll(town.makeGoal(s, "fun_ball").s1aTexts())
            addAll(town.achieveGoal(s).s1aTexts())
            val morning = weekTwoMorning()
            addAll(town.withdraw(morning, 20).s1aTexts())
            addAll(town.withdrawPreview(morning, 20))
            addAll(town.withdraw(morning, 99).s1aTexts())
            addAll(town.withdraw(s, 10).s1aTexts())
        }
        S1aChildText.check("кошелёк", texts)
    }

    // ---------- Сцены ----------

    /** Демо-путь §11 шаги 5–7 и выбор мечты шага 8: план 40 / 20 / 30, корм 20, шарик 8, мыло 10. */
    private fun demoStep8(): GameState {
        var s = S1aStand.planned(40, 20, 30)
        s = town.buyAt(s, "food_basic", "shop_market", Source.NEED).s1aState()
        s = town.buyAt(s, "fun_balloon", "shop_market", Source.WANT).s1aState()
        s = town.buyAt(s, "care_soap", "shop_foma", Source.NEED).s1aState()
        return economy.chooseGoal(s, S1aStand.townGoal("goal_scooter")).ok()
    }

    /** Утро недели 2 после недели с мечтой и взносом 30: копилка 40, план ещё не разложен. */
    private fun weekTwoMorning(): GameState {
        val week1 = economy.chooseGoal(S1aStand.planned(40, 20, 30), S1aStand.townGoal("goal_scooter")).ok()
        val deposited = town.deposit(week1, Source.WANT, 10).s1aState()
        val night = town.endWeek(deposited).s1aState()
        return town.wake(night).s1aState()
    }
}
