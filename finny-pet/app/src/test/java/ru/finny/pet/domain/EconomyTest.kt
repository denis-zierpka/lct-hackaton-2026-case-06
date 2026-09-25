package ru.finny.pet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EconomyTest {
    private val e = TestContent.economy
    private val rules = TestContent.content.rules

    @Test
    fun `create pet gives allowance with explained source`() {
        val s = e.createPet(e.newGame(true), "Финни", "cat", "orange").ok()
        assertEquals(rules.allowance, s.balance)
        assertEquals(1, s.period)
        assertEquals(rules.allowance, s.ledger.single().amount)
    }

    @Test
    fun `create pet validates name`() {
        val s = e.newGame(true)
        e.createPet(s, "   ", "cat", "orange").err()
        e.createPet(s, "ОченьДлинноеИмяПитомца", "cat", "orange").err()
        e.createPet(s, "Финни", "unicorn", "orange").err()
    }

    @Test
    fun `plan cannot exceed balance and shows remainder`() {
        val s = e.createPet(e.newGame(true), "Финни", "cat", "orange").ok()
        val err = e.setPlan(s, 60, 30, 20).err()
        assertTrue(err.message.contains("110"))
        e.setPlan(s, -1, 0, 0).err()
        val planned = e.setPlan(s, 50, 20, 20).ok()
        assertFalse(planned.plan.confirmed)
        val confirmed = e.confirmPlan(planned) as Outcome.Ok
        assertTrue(confirmed.state.plan.confirmed)
        assertTrue(confirmed.messages.any { it.contains("осталось 10") })
        e.setPlan(confirmed.state, 1, 1, 1).err() // locked after confirmation
    }

    @Test
    fun `empty plan cannot be confirmed`() {
        val s = e.createPet(e.newGame(true), "Финни", "cat", "orange").ok()
        e.confirmPlan(s).err()
    }

    @Test
    fun `shopping requires a confirmed plan`() {
        val s = e.createPet(e.newGame(true), "Финни", "cat", "orange").ok()
        e.buy(s, "food_basic").err()
        e.deposit(s, 10).err()
    }

    @Test
    fun `purchase reduces balance and changes pet state`() {
        val s = TestContent.readyState()
        val r = e.buy(s, "food_basic") as Outcome.Ok
        assertEquals(70, r.state.balance)
        assertEquals(100, r.state.pet!!.hunger) // 70 + 40 clamped
        assertEquals(1, r.state.purchases.size)
        assertEquals(-30, r.state.ledger.last().amount)
        assertTrue(r.messages.any { it.contains("Сытость") })
    }

    @Test
    fun `purchase over balance is refused with explanation and options`() {
        var s = TestContent.readyState()
        s = e.buy(s, "fun_tent").ok() // 100 -> 40
        val err = e.buy(s, "food_lunch").err() // 45 > 40
        assertTrue(err.message.contains("Не хватает 5"))
        assertTrue(err.hints.any { it.contains("задание") })
        assertTrue(err.hints.any { it.contains("Корм") }) // cheaper alternative of same need
        assertEquals(40, s.balance) // unchanged
    }

    @Test
    fun `overspending a category warns but is allowed`() {
        val s = TestContent.readyState() // optional plan = 20
        val r = e.buy(s, "fun_ball") as Outcome.Ok // 25 > 20
        assertTrue(
            r.messages.toString(),
            r.messages.contains("На желаемое потрачено 25 из 20 по плану — больше плана. Не страшно: учти это в следующем плане."),
        )
        assertTrue(r.messages.none { it.contains("планировал") })
    }

    @Test
    fun `deposit and withdraw move coins between balance and savings`() {
        var s = TestContent.readyState()
        s = e.deposit(s, 30).ok()
        assertEquals(70, s.balance)
        assertEquals(30, s.savings)
        assertEquals(30, s.depositedThisPeriod)
        e.deposit(s, 0).err()
        e.deposit(s, 71).err()
        e.withdraw(s, 31).err()
        s = e.withdraw(s, 10).ok()
        assertEquals(80, s.balance)
        assertEquals(20, s.savings)
        assertEquals(20, s.factSavings)
    }

    @Test
    fun `goal eta uses average deposit per period`() {
        var s = TestContent.readyState()
        s = e.chooseGoal(s, Goal("g", "Самокат", "🛴", 150)).ok()
        assertNull(e.goalEta(s)) // no deposits yet
        s = e.deposit(s, 30).ok()
        assertEquals(4, e.goalEta(s)) // (150-30)/30 = 4
        s = e.endPeriod(s).ok()
        assertEquals(listOf(30), s.depositHistory)
        // period 2, nothing deposited yet: the running week is not counted, avg stays 30, left 120 -> 4
        assertEquals(4, e.goalEta(s))
        // an unknown task id in saved results must not crash history views
        assertEquals(0, e.completedTasks(s.copy(taskResults = listOf(TaskResult("removed", true, 20, 1)))).size)
        val preview = e.withdrawPreview(s, 20)
        assertTrue(preview.any { it.contains("останется 10") })
        // a week with a net withdrawal is recorded as 0, not negative: the forecast keeps working
        s = e.setPlan(s, 30, 0, 0).ok()
        s = e.confirmPlan(s).ok()
        s = e.withdraw(s, 30).ok()
        s = e.endPeriod(s).ok()
        assertEquals(listOf(30, 0), s.depositHistory)
        assertEquals(10, e.goalEta(s)) // 150 left / avg 15
    }

    @Test
    fun `goal is achieved only with enough savings and boosts mood`() {
        var s = TestContent.readyState()
        s = e.chooseGoal(s, Goal("g", "Краски", "🎨", 80)).ok()
        e.achieveGoal(s).err()
        s = e.deposit(s, 80).ok()
        val moodBefore = s.pet!!.mood
        val r = e.achieveGoal(s) as Outcome.Ok
        assertEquals(0, r.state.savings)
        assertNull(r.state.goal)
        assertEquals(1, r.state.achievedGoals.size)
        assertEquals((moodBefore + rules.goalMoodBonus).coerceAtMost(100), r.state.pet!!.mood)
    }

    @Test
    fun `task answers reward coins and explain either way`() {
        var s = TestContent.readyState()
        val task = TestContent.content.tasks.first { it.type == TaskType.CHOICE }
        val wrongIdx = task.options.indexOfFirst { !it.correct }
        val r1 = e.answerChoice(s, task.id, wrongIdx) as Outcome.Ok
        assertEquals(s.balance + rules.rewardWrong, r1.state.balance)
        assertTrue(r1.messages.first().isNotBlank())
        e.answerChoice(r1.state, task.id, 0).err() // already done

        val num = TestContent.content.tasks.first { it.type == TaskType.NUMBER }
        val r2 = e.answerNumber(s, num.id, num.answer) as Outcome.Ok
        assertEquals(s.balance + rules.rewardCorrect, r2.state.balance)
        assertEquals(rules.rewardCorrect, r2.state.ledger.last().amount)
        e.answerNumber(s, num.id, null).err()
    }

    @Test
    fun `tasks unlock by period in normal mode and all at once in demo`() {
        val demo = TestContent.readyState(demo = true)
        assertEquals(TestContent.content.tasks.size, e.availableTasks(demo).size)
        val normal = TestContent.readyState(demo = false)
        assertTrue(e.availableTasks(normal).size < TestContent.content.tasks.size)
        assertTrue(e.availableTasks(normal).all { it.unlockPeriod <= 1 })
    }

    @Test
    fun `period end requires plan and scores three rules`() {
        val fresh = e.createPet(e.newGame(true), "Финни", "cat", "orange").ok()
        e.endPeriod(fresh).err()

        var s = TestContent.readyState() // plan 50/20/30
        s = e.buy(s, "food_basic").ok()   // 30 mandatory
        s = e.buy(s, "care_shampoo").ok() // 20 mandatory => 50
        s = e.buy(s, "fun_bow").ok()      // 15 optional
        s = e.deposit(s, 30).ok()
        val r = e.endPeriod(s) as Outcome.Ok
        val sum = r.state.history.single()
        assertTrue(sum.mandatoryCovered)
        assertTrue(sum.planKept)
        assertTrue(sum.saved)
        assertEquals(3, sum.score)
        assertEquals(3, r.state.pet!!.growth)
        assertEquals(2, r.state.period)
        assertEquals(5 + rules.allowance, r.state.balance) // 100-30-20-15-30 = 5, +100
        assertTrue(r.state.purchases.isEmpty())
        assertFalse(r.state.plan.confirmed)
        // every coin on the new balance has a source: leftover + allowance (2.5.4)
        assertEquals(r.state.balance, r.state.ledger.sumOf { it.amount })
        assertEquals(listOf(5, rules.allowance), r.state.ledger.map { it.amount })
    }

    @Test
    fun `uncovered needs lower pet state but never below floor and never death`() {
        var s = TestContent.readyState()
        s = e.buy(s, "fun_tent").ok()
        val r = e.endPeriod(s) as Outcome.Ok
        val sum = r.state.history.single()
        assertFalse(sum.mandatoryCovered)
        assertEquals(0, sum.score) // optional overspent (60 > 20), no savings
        val pet = r.state.pet!!
        assertTrue(pet.hunger >= rules.statFloor)
        assertTrue(pet.hunger < 70)
        assertTrue(sum.messages.any { it.startsWith("Еды на этой неделе не было") }) // no reproach (3.5)
        assertTrue(sum.messages.any { it.contains("начни с обязательного") }) // recovery path
        assertEquals(Face.SAD, e.face(pet))
    }

    @Test
    fun `stage grows with good periods and never regresses`() {
        var s = TestContent.readyState()
        assertEquals(0, e.stageIndex(s.pet!!.growth))
        repeat(2) {
            s = e.buy(s, "food_basic").ok()
            s = e.buy(s, "care_shampoo").ok()
            s = e.deposit(s, 30).ok() // meets plan.savings = 30
            s = e.endPeriod(s).ok()
            s = e.setPlan(s, 50, 20, 30).ok()
            s = e.confirmPlan(s).ok()
        }
        assertEquals(6, checkNotNull(s.pet).growth)
        assertEquals(1, e.stageIndex(6))
        assertEquals("Подросток", e.stageTitle(6))
        // a bad period does not reduce growth
        s = e.endPeriod(s).ok()
        assertEquals(6, s.pet!!.growth)
        assertEquals(2, e.stageIndex(9))
        assertEquals(3, e.nextStageLeft(6))
        assertNull(e.nextStageLeft(9))
    }

    @Test
    fun `five demo periods run back to back`() {
        var s = TestContent.readyState()
        repeat(5) {
            s = e.buy(s, "food_basic").ok()
            s = e.endPeriod(s).ok()
            s = e.setPlan(s, 30, 0, 0).ok()
            s = e.confirmPlan(s).ok()
        }
        assertEquals(6, s.period)
        assertEquals(5, s.history.size)
    }

    @Test
    fun `reset keeps demo flag and delete clears everything`() {
        val s = TestContent.readyState(demo = true)
        val reset = e.resetProfile(s)
        assertTrue(reset.demo)
        assertNull(reset.pet)
        val deleted = e.deleteProfile(s)
        assertFalse(deleted.demo)
        assertNull(deleted.pet)
    }

    @Test
    fun `russian plural for weeks`() {
        assertEquals("1 неделя", Economy.weeks(1))
        assertEquals("2 недели", Economy.weeks(2))
        assertEquals("5 недель", Economy.weeks(5))
        assertEquals("11 недель", Economy.weeks(11))
        assertEquals("21 неделя", Economy.weeks(21))
    }

    // ---------- MVP-T01a CONTRACT 1: coins(n, case) ----------

    private fun assertCoins(case: Case, expected: Map<Int, String>) =
        expected.forEach { (n, text) -> assertEquals("coins($n, $case)", text, Economy.coins(n, case)) }

    @Test
    fun `падежи монет перечислены в порядке NOM ACC GEN`() =
        assertEquals(listOf("NOM", "ACC", "GEN"), Case.entries.map { it.name })

    @Test
    fun `монеты в именительном падеже`() = assertCoins(
        Case.NOM,
        mapOf(
            0 to "0 монет", 1 to "1 монета", 2 to "2 монеты", 3 to "3 монеты", 4 to "4 монеты", 5 to "5 монет",
            10 to "10 монет", 11 to "11 монет", 12 to "12 монет", 13 to "13 монет", 14 to "14 монет", 15 to "15 монет",
            21 to "21 монета", 22 to "22 монеты", 24 to "24 монеты", 25 to "25 монет", 100 to "100 монет",
            101 to "101 монета", 111 to "111 монет", 112 to "112 монет", 114 to "114 монет", 122 to "122 монеты",
        ),
    )

    @Test
    fun `монеты в винительном падеже`() = assertCoins(
        Case.ACC,
        mapOf(
            0 to "0 монет", 1 to "1 монету", 2 to "2 монеты", 4 to "4 монеты", 5 to "5 монет",
            11 to "11 монет", 12 to "12 монет", 13 to "13 монет", 14 to "14 монет",
            21 to "21 монету", 22 to "22 монеты", 101 to "101 монету", 111 to "111 монет", 112 to "112 монет",
        ),
    )

    @Test
    fun `монеты в родительном падеже`() = assertCoins(
        Case.GEN,
        mapOf(
            0 to "0 монет", 1 to "1 монеты", 2 to "2 монет", 4 to "4 монет", 5 to "5 монет",
            11 to "11 монет", 12 to "12 монет", 13 to "13 монет", 14 to "14 монет",
            21 to "21 монеты", 22 to "22 монет", 101 to "101 монеты", 111 to "111 монет", 112 to "112 монет",
        ),
    )

    @Test
    fun `монеты по умолчанию в именительном падеже`() {
        assertEquals("1 монета", Economy.coins(1))
        assertEquals("2 монеты", Economy.coins(2))
        assertEquals("5 монет", Economy.coins(5))
        assertEquals("11 монет", Economy.coins(11))
        assertEquals("21 монета", Economy.coins(21))
        assertEquals("22 монеты", Economy.coins(22))
        assertEquals("0 монет", Economy.coins(0))
        assertEquals("112 монет", Economy.coins(112))
    }

    @Test
    fun `форма монет берётся по модулю числа`() {
        assertEquals("-1 монета", Economy.coins(-1))
        assertEquals("-3 монеты", Economy.coins(-3))
        assertEquals("-12 монет", Economy.coins(-12))
        assertEquals("-21 монету", Economy.coins(-21, Case.ACC))
        assertEquals("-2 монет", Economy.coins(-2, Case.GEN))
        assertEquals("-31 монеты", Economy.coins(-31, Case.GEN))
    }
}

class GameEditionTest {
    private val e = TestContent.economy
    private val rules = TestContent.content.rules

    @Test
    fun `mini game coins are capped per period and explained`() {
        val s = TestContent.readyState()
        val first = e.finishMiniGame(s, score = 400, bombsUsed = 0) as Outcome.Ok
        assertEquals(20, first.state.balance - s.balance)
        assertTrue(first.state.ledger.last().text.contains("Монетки"))
        val second = e.finishMiniGame(first.state, score = 1000, bombsUsed = 0) as Outcome.Ok
        assertEquals(rules.miniGameCap, second.state.miniGameEarned)
        assertEquals(rules.miniGameCap - 20, second.state.balance - first.state.balance)
        val third = e.finishMiniGame(second.state, score = 500, bombsUsed = 0) as Outcome.Ok
        assertEquals(second.state.balance, third.state.balance)
        assertTrue(third.messages.any { it.contains("лимит") || it.contains("уже принесла") })
        val next = e.endPeriod(third.state).ok()
        assertEquals(0, next.miniGameEarned)
    }

    @Test
    fun `quiz answers arm bombs without paying coins`() {
        val s = TestContent.readyState()
        val q = e.availableQuiz(s).first()
        val ok = e.answerQuiz(s, q.id, q.correct) as Outcome.Ok
        assertEquals(rules.quizBombReward, ok.state.bombs)
        assertEquals(s.balance, ok.state.balance)
        assertFalse(e.availableQuiz(ok.state).any { it.id == q.id })
        val wrong = e.answerQuiz(s, q.id, (q.correct + 1) % q.options.size) as Outcome.Ok
        assertEquals(0, wrong.state.bombs)
        assertTrue(e.availableQuiz(wrong.state).any { it.id == q.id })
        val spent = e.finishMiniGame(ok.state, 0, bombsUsed = 1).ok()
        assertEquals(0, spent.bombs)
    }

    @Test
    fun `quiz content covers three themes and is well formed`() {
        val quiz = TestContent.content.quiz
        assertTrue(quiz.size >= 12)
        assertEquals(Theme.entries.toSet(), quiz.map { it.theme }.toSet())
        quiz.forEach { q ->
            assertTrue(q.options.size in 2..4)
            assertTrue(q.correct in q.options.indices)
            assertTrue(q.explanation.isNotBlank())
        }
        assertTrue(TestContent.content.chatter.idle.isNotEmpty())
    }
}
