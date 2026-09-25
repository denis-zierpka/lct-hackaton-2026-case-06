package ru.finny.pet.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.pet.data.ContentRepository
import java.io.File

/** Oracle for docs/tasks/MVP-T01.md, CONTRACT A–E. Texts for the child are asserted verbatim: they are the contract. */
class MvpRulesTest {
    private val content = TestContent.content
    private val e = TestContent.economy
    private val rules = content.rules

    private fun economyWith(r: Rules) = Economy(content.copy(rules = r))
    private fun newPet(econ: Economy = e, demo: Boolean = true) =
        econ.createPet(econ.newGame(demo), "Финни", "cat", "orange").ok()
    private fun ready(econ: Economy = e, demo: Boolean = true, mandatory: Int = 50, optional: Int = 20, savings: Int = 30): GameState {
        val s = econ.setPlan(newPet(econ, demo), mandatory, optional, savings).ok()
        return econ.confirmPlan(s).ok()
    }
    private fun end(s: GameState, econ: Economy = e): Outcome.Ok =
        econ.endPeriod(s) as? Outcome.Ok ?: error("expected Ok from endPeriod")
    private fun pet(hunger: Int, clean: Int, mood: Int) = Pet("Финни", "cat", "orange", hunger = hunger, clean = clean, mood = mood)
    private fun rawContent(): JsonObject =
        Json.parseToJsonElement(File("src/main/assets/content/content.json").readText()).jsonObject
    private fun piggyHint(x: Int) = "На этой неделе копилка не выросла. Попробуй отложить хотя бы $x монет — так цель приблизится."

    private val lockNoPet = "Сначала создай питомца"
    private val lockNoPlan = "Сначала составь план на неделю — игра откроется после него"
    private val taskHint = "Выполни задание — за него дают монеты"
    private val savingsHint = "Можно взять из копилки, но цель отодвинется"
    private val waitHint = "Или подожди новую неделю — придут карманные деньги"
    private val limitError = "На этой неделе все бонусы уже начислены — новые будут со следующей недели."
    private val limitReached = "Лимит бонусов на эту неделю исчерпан."

    // ---------- A. Rules and content ----------

    @Test
    fun `новые числа правил в content json`() {
        assertEquals(10, rules.rewardCorrect)
        assertEquals(10, rules.parentBonusAmount)
        assertEquals(3, rules.parentBonusPerPeriod)
        assertEquals(1, rules.quizBombReward)
        assertEquals(70, rules.startStat)
        assertEquals(30, rules.faceSadBelow)
        assertEquals(60, rules.faceHappyAvg)
        assertEquals(40, rules.needLowBelow)
        assertEquals(10, rules.planStep)
        assertEquals(listOf(10, 20, 30, 50), rules.savingsAmounts)
    }

    @Test
    fun `значения Rules по умолчанию совпадают с content json`() = assertEquals(Rules(), rules)

    @Test
    fun `новые поля правил записаны в content json явно`() {
        val root = rawContent()
        val r = root.getValue("rules").jsonObject
        mapOf(
            "rewardCorrect" to 10, "parentBonusAmount" to 10, "parentBonusPerPeriod" to 3, "quizBombReward" to 1,
            "startStat" to 70, "faceSadBelow" to 30, "faceHappyAvg" to 60, "needLowBelow" to 40, "planStep" to 10,
        ).forEach { (key, value) -> assertEquals(key, value, r[key]?.jsonPrimitive?.int) }
        assertEquals(listOf(10, 20, 30, 50), r.getValue("savingsAmounts").jsonArray.map { it.jsonPrimitive.int })
        assertEquals(4, root.getValue("parentBonusReasons").jsonArray.size)
    }

    @Test
    fun `причин бонуса взрослого ровно четыре`() = assertEquals(
        listOf("Помог по дому", "Довёл дело до конца", "Сам навёл порядок", "Придумал, как сэкономить"),
        content.parentBonusReasons,
    )

    @Test
    fun `контент без причин бонуса читается с пустым списком`() {
        val stripped = JsonObject(rawContent() - "parentBonusReasons")
        assertEquals(emptyList<String>(), ContentRepository.parse(stripped.toString()).parentBonusReasons)
    }

    @Test
    fun `задание про внезапную трату называет цену бантика`() {
        val situation = content.task("shop_unexpected").situation
        assertEquals(15, content.item("fun_bow").price)
        assertTrue(situation, situation.contains("Лекарство стоит 15 монет"))
        assertFalse(situation, situation.contains("25 монет"))
    }

    // ---------- B. State defaults ----------

    @Test
    fun `новые поля состояния имеют значения по умолчанию`() {
        val g = GameState()
        assertFalse(g.music)
        assertTrue(g.sounds)
        assertEquals(0, g.parentBonusesThisPeriod)
        val p = PeriodSummary(
            period = 1, plan = BudgetPlan(), factMandatory = 0, factOptional = 0, factSavings = 0,
            mandatoryCovered = false, planKept = false, saved = false, score = 0,
            growthBefore = 0, growthAfter = 0, stageBefore = 0, stageAfter = 0, messages = emptyList(),
        )
        assertEquals(0, p.miniGameEarned)
        assertEquals(0, p.parentBonus)
    }

    @Test
    fun `старое сохранение без новых полей читается`() {
        val old = """
            {"demo":true,"animations":false,"sounds":false,"period":2,"balance":105,
             "pet":{"name":"Финни","speciesId":"cat","colorId":"orange"},
             "history":[{"period":1,"plan":{"mandatory":50,"optional":20,"savings":30,"confirmed":true},
               "factMandatory":50,"factOptional":15,"factSavings":30,"mandatoryCovered":true,"planKept":true,
               "saved":true,"score":3,"growthBefore":0,"growthAfter":3,"stageBefore":0,"stageAfter":0,"messages":[]}]}
        """.trimIndent()
        val s = Json { ignoreUnknownKeys = true }.decodeFromString(GameState.serializer(), old)
        assertTrue(s.demo)
        assertFalse(s.sounds)
        assertFalse(s.music)
        assertEquals(0, s.parentBonusesThisPeriod)
        assertEquals(0, s.history.single().miniGameEarned)
        assertEquals(0, s.history.single().parentBonus)
    }

    // ---------- C1–C3. Profile settings survive reset and delete ----------

    @Test
    fun `newGame принимает анимации звуки и музыку`() {
        assertEquals(
            GameState(demo = true, animations = false, sounds = false, music = true),
            e.newGame(demo = true, animations = false, sounds = false, music = true),
        )
        assertEquals(GameState(demo = false, animations = true, sounds = true, music = false), e.newGame(demo = false))
    }

    @Test
    fun `сброс профиля сохраняет демо анимации звуки и музыку`() {
        listOf(true, false).forEach { flag ->
            val s = e.createPet(e.newGame(demo = flag, animations = !flag, sounds = flag, music = !flag), "Финни", "cat", "orange").ok()
            val reset = e.resetProfile(s)
            assertNull(reset.pet)
            assertEquals(e.newGame(demo = flag, animations = !flag, sounds = flag, music = !flag), reset)
            assertEquals(!flag, reset.music)
            assertEquals(flag, reset.sounds)
        }
    }

    @Test
    fun `удаление профиля сохраняет только анимации звуки и музыку`() {
        listOf(true, false).forEach { flag ->
            val s = e.createPet(e.newGame(demo = true, animations = flag, sounds = !flag, music = flag), "Финни", "cat", "orange").ok()
            assertEquals(GameState(animations = flag, sounds = !flag, music = flag), e.deleteProfile(s))
        }
    }

    // ---------- C4–C5. Pet start and face ----------

    @Test
    fun `новый питомец получает стартовые показатели из rules`() {
        val p = newPet().pet!!
        assertEquals(listOf(rules.startStat, rules.startStat, rules.startStat), listOf(p.hunger, p.clean, p.mood))
        val custom = newPet(economyWith(rules.copy(startStat = 55))).pet!!
        assertEquals(listOf(55, 55, 55), listOf(custom.hunger, custom.clean, custom.mood))
    }

    @Test
    fun `питомец грустит если минимум ниже 30 и не грустит на 30`() {
        assertEquals(Face.SAD, e.face(pet(29, 100, 100)))
        assertEquals(Face.SAD, e.face(pet(100, 100, 29)))
        assertEquals(Face.NEUTRAL, e.face(pet(30, 100, 100)))
    }

    @Test
    fun `питомец доволен только без показателя ниже 40`() {
        assertEquals(Face.NEUTRAL, e.face(pet(39, 100, 100)))
        assertEquals(Face.HAPPY, e.face(pet(40, 100, 100)))
    }

    @Test
    fun `питомец доволен только при среднем от 60`() {
        assertEquals(Face.NEUTRAL, e.face(pet(40, 40, 97))) // avg 59
        assertEquals(Face.HAPPY, e.face(pet(40, 40, 100))) // avg 60
    }

    @Test
    fun `пороги настроения питомца берутся из rules`() {
        assertEquals(Face.HAPPY, e.face(pet(45, 100, 100)))
        assertEquals(Face.SAD, economyWith(rules.copy(faceSadBelow = 50)).face(pet(45, 100, 100)))
        assertEquals(Face.HAPPY, e.face(pet(67, 100, 100)))
        assertEquals(Face.NEUTRAL, economyWith(rules.copy(faceHappyAvg = 90)).face(pet(67, 100, 100))) // avg 89
        assertEquals(Face.HAPPY, economyWith(rules.copy(faceHappyAvg = 90)).face(pet(70, 100, 100))) // avg 90
        assertEquals(Face.HAPPY, e.face(pet(55, 100, 100)))
        assertEquals(Face.NEUTRAL, economyWith(rules.copy(needLowBelow = 60)).face(pet(55, 100, 100)))
    }

    @Test
    fun `причина настроения говорит хочет помыться а не грязный`() {
        val neutral = e.faceReason(pet(35, 39, 100))
        assertTrue(neutral, neutral.contains("хочет помыться"))
        assertTrue(neutral, neutral.contains("голодный"))
        assertFalse(neutral, neutral.contains("грязн"))
        val sad = e.faceReason(pet(100, 20, 100))
        assertTrue(sad, sad.contains("хочет помыться"))
        assertFalse(sad, sad.contains("грязн"))
        val cleanEnough = e.faceReason(pet(35, 40, 100))
        assertFalse(cleanEnough, cleanEnough.contains("помыться"))
    }

    @Test
    fun `низкий показатель в причине считается по needLowBelow из rules`() {
        assertFalse(e.faceReason(pet(100, 55, 100)).contains("помыться"))
        val custom = economyWith(rules.copy(needLowBelow = 60)).faceReason(pet(100, 55, 100))
        assertTrue(custom, custom.contains("хочет помыться"))
    }

    // ---------- C6. Plan kept: savings part is net and never negative ----------

    @Test
    fun `снятие из копилки при плане копилки 0 не проваливает план`() {
        var s = e.deposit(ready(), 30).ok()
        s = end(s).state
        s = e.confirmPlan(e.setPlan(s, 30, 0, 0).ok()).ok()
        s = e.withdraw(s, 20).ok()
        s = e.buy(s, "food_basic").ok()
        val r = end(s)
        val sum = r.state.history.last()
        assertEquals(-20, sum.factSavings)
        assertTrue(sum.planKept)
        assertTrue(r.messages.none { it.startsWith("В копилку") })
    }

    @Test
    fun `чистый взнос ровно по плану выполняет план`() {
        var s = e.deposit(ready(), 40).ok()
        s = e.withdraw(s, 10).ok()
        val sum = end(s).state.history.last()
        assertEquals(30, sum.factSavings)
        assertTrue(sum.planKept)
    }

    @Test
    fun `чистый взнос на монету меньше плана объясняет снятие`() {
        var s = e.deposit(ready(), 39).ok()
        s = e.withdraw(s, 10).ok()
        val r = end(s)
        assertFalse(r.state.history.last().planKept)
        assertTrue(r.messages.toString(), r.messages.contains("В копилку чистыми 29 (положил 39, взял 10), а планировал 30."))
        assertTrue(r.messages.none { it.startsWith("В копилку отложил") })
    }

    @Test
    fun `без снятий текст про копилку показывает отложенное`() {
        val r = end(e.deposit(ready(), 20).ok())
        assertFalse(r.state.history.last().planKept)
        assertTrue(r.messages.toString(), r.messages.contains("В копилку отложил 20, а планировал 30."))
        assertTrue(r.messages.none { it.contains("чистыми") })
    }

    @Test
    fun `чистый взнос в тексте не бывает отрицательным`() {
        var s = end(e.deposit(ready(), 30).ok()).state
        s = e.confirmPlan(e.setPlan(s, 50, 20, 30).ok()).ok()
        s = e.deposit(s, 10).ok()
        s = e.withdraw(s, 30).ok()
        val r = end(s)
        assertFalse(r.state.history.last().planKept)
        assertTrue(r.messages.toString(), r.messages.contains("В копилку чистыми 0 (положил 10, взял 30), а планировал 30."))
    }

    // ---------- C7. Week summary texts ----------

    @Test
    fun `без еды и ухода тексты без упрёка`() {
        val r = end(e.buy(ready(), "fun_bow").ok())
        val drop = rules.uncoveredExtraDrop
        assertTrue(r.messages.toString(), r.messages.contains("Еды на этой неделе не было — Финни проголодался сильнее: сытость ещё −$drop."))
        assertTrue(r.messages.toString(), r.messages.contains("Ухода на этой неделе не было — Финни запачкался сильнее: чистота ещё −$drop."))
        assertTrue(r.messages.none { it.contains("не купил") })
    }

    @Test
    fun `без еды но с уходом только текст про еду`() {
        val r = end(e.buy(ready(), "care_shampoo").ok())
        assertTrue(r.messages.any { it.startsWith("Еды на этой неделе не было") })
        assertTrue(r.messages.none { it.startsWith("Ухода на этой неделе не было") })
    }

    @Test
    fun `копилка не выросла подсказывает первую сумму из savingsAmounts`() {
        assertTrue(end(ready()).messages.contains(piggyHint(rules.savingsAmounts.first())))
        val custom = economyWith(rules.copy(savingsAmounts = listOf(25, 50)))
        assertTrue(end(ready(custom), custom).messages.contains(piggyHint(25)))
    }

    @Test
    fun `копилка не выросла при пустом savingsAmounts подсказывает planStep`() {
        val custom = economyWith(rules.copy(savingsAmounts = emptyList(), planStep = 15))
        val r = end(ready(custom), custom)
        assertTrue(r.messages.toString(), r.messages.contains(piggyHint(15)))
    }

    @Test
    fun `рост показывает счёт из трёх проверок`() {
        var good = e.buy(ready(), "food_basic").ok()
        good = e.buy(good, "care_shampoo").ok()
        good = e.deposit(good, 30).ok()
        assertTrue(end(good).messages.contains("Рост: +3 из 3 (всего 3)."))
        // nothing bought, plan savings 0: only the plan check passes
        assertTrue(end(ready(savings = 0)).messages.contains("Рост: +1 из 3 (всего 1)."))
    }

    @Test
    fun `без мини-игры и бонуса строк вне плана нет`() {
        val r = end(ready())
        assertTrue(r.messages.none { it.startsWith("Вне плана:") })
        val sum = r.state.history.last()
        assertEquals(0, sum.miniGameEarned)
        assertEquals(0, sum.parentBonus)
    }

    @Test
    fun `монеты мини-игры видны отдельной строкой и в итоге недели`() {
        val s = e.finishMiniGame(ready(), score = 7 * rules.miniGameScorePerCoin, bombsUsed = 0).ok()
        val r = end(s)
        assertTrue(r.messages.toString(), r.messages.contains("Вне плана: игра «Монетки в ряд» принесла 7 монет."))
        assertTrue(r.messages.none { it.startsWith("Вне плана: бонус") })
        assertEquals(7, r.state.history.last().miniGameEarned)
        assertEquals(0, r.state.history.last().parentBonus)
    }

    @Test
    fun `бонус взрослого виден отдельной строкой и в итоге недели`() {
        val r = end(e.parentBonus(ready(), 0).ok())
        assertTrue(r.messages.toString(), r.messages.contains("Вне плана: бонус от взрослого — ${rules.parentBonusAmount} монет."))
        assertTrue(r.messages.none { it.startsWith("Вне плана: игра") })
        assertEquals(rules.parentBonusAmount, r.state.history.last().parentBonus)
        assertEquals(0, r.state.history.last().miniGameEarned)
    }

    @Test
    fun `строки вне плана стоят перед новой неделей`() {
        var s = e.finishMiniGame(ready(), score = 10 * rules.miniGameScorePerCoin, bombsUsed = 0).ok()
        s = e.parentBonus(s, 0).ok()
        s = e.parentBonus(s, 1).ok()
        val r = end(s)
        val newWeek = r.messages.indexOfFirst { it.startsWith("Новая неделя:") }
        val game = r.messages.indexOf("Вне плана: игра «Монетки в ряд» принесла 10 монет.")
        val bonus = r.messages.indexOf("Вне плана: бонус от взрослого — ${2 * rules.parentBonusAmount} монет.")
        assertTrue(r.messages.toString(), newWeek >= 0 && game >= 0 && bonus >= 0)
        assertTrue(r.messages.toString(), game < newWeek && bonus < newWeek)
        val sum = r.state.history.last()
        assertEquals(10, sum.miniGameEarned)
        assertEquals(2 * rules.parentBonusAmount, sum.parentBonus)
        assertEquals(0, r.state.parentBonusesThisPeriod)
    }

    // ---------- C8. Plan with zero mandatory ----------

    @Test
    fun `план с нулём на обязательное предупреждает без упрёка`() {
        val r = e.confirmPlan(e.setPlan(newPet(), 0, 20, 30).ok()) as Outcome.Ok
        assertTrue(
            r.messages.toString(),
            r.messages.contains("На обязательное в плане 0 монет, а Финни всё равно понадобятся еда и уход — оставь на них монеты."),
        )
        assertTrue(r.messages.none { it.contains("Ты не заложил") })
        val withMandatory = e.confirmPlan(e.setPlan(newPet(), 10, 20, 30).ok()) as Outcome.Ok
        assertTrue(withMandatory.messages.none { it.startsWith("На обязательное в плане") })
    }

    // ---------- C9. Buy hints ----------

    @Test
    fun `подсказка про задание только если задания остались`() {
        val base = ready(demo = false).copy(balance = 10) // fun_tent 60
        assertTrue(e.buy(base, "fun_tent").err().hints.contains(taskHint))
        // week 1 in normal mode: every open task is done, later ones are still locked
        val allOpenDone = base.copy(taskResults = content.tasks.filter { it.unlockPeriod <= 1 }.map { TaskResult(it.id, true, rules.rewardCorrect, 1) })
        assertTrue(e.availableTasks(allOpenDone).isEmpty())
        assertTrue(allOpenDone.taskResults.size < content.tasks.size)
        val hints = e.buy(allOpenDone, "fun_tent").err().hints
        assertFalse(hints.toString(), hints.contains(taskHint))
        assertTrue(hints.toString(), hints.contains(waitHint))
    }

    @Test
    fun `подсказка про копилку только если в ней хватает`() {
        val base = ready().copy(balance = 40) // fun_tent 60, missing 20
        val enough = e.buy(base.copy(savings = 20), "fun_tent").err()
        assertTrue(enough.message, enough.message.contains("Не хватает 20"))
        assertTrue(enough.hints.toString(), enough.hints.contains(savingsHint))
        assertTrue(enough.hints.toString(), enough.hints.contains(waitHint))
        assertFalse(e.buy(base.copy(savings = 19), "fun_tent").err().hints.contains(savingsHint))
        assertFalse(e.buy(base.copy(savings = 0), "fun_tent").err().hints.contains(savingsHint))
    }

    // ---------- C10. Quiz ----------

    @Test
    fun `повторный ответ на верно отвеченный вопрос отклоняется`() {
        val s = ready()
        val q = e.availableQuiz(s).first()
        val wrongIdx = (q.correct + 1) % q.options.size
        val answered = e.answerQuiz(s, q.id, q.correct).ok()
        assertEquals("На этот вопрос ты уже ответил", e.answerQuiz(answered, q.id, q.correct).err().message)
        assertEquals("На этот вопрос ты уже ответил", e.answerQuiz(answered, q.id, wrongIdx).err().message)
        // a wrong answer can still be retried
        val wrong = e.answerQuiz(s, q.id, wrongIdx).ok()
        assertEquals(s.bombs + rules.quizBombReward, e.answerQuiz(wrong, q.id, q.correct).ok().bombs)
    }

    @Test
    fun `верный ответ даёт бомбочки из rules`() {
        val s = ready().copy(bombs = 2)
        val q = e.availableQuiz(s).first()
        assertEquals(2 + rules.quizBombReward, e.answerQuiz(s, q.id, q.correct).ok().bombs)
        assertEquals(5, economyWith(rules.copy(quizBombReward = 3)).answerQuiz(s, q.id, q.correct).ok().bombs)
        assertEquals(2, e.answerQuiz(s, q.id, (q.correct + 1) % q.options.size).ok().bombs)
    }

    // ---------- C11. Mini-game lock ----------

    @Test
    fun `мини-игра закрыта без питомца`() {
        val s = e.newGame(true)
        assertEquals(lockNoPet, e.miniGameLock(s))
        assertEquals(lockNoPet, e.finishMiniGame(s, score = 200, bombsUsed = 0).err().message)
    }

    @Test
    fun `мини-игра закрыта до подтверждения плана`() {
        val s = newPet()
        assertEquals(lockNoPlan, e.miniGameLock(s))
        assertEquals(lockNoPlan, e.finishMiniGame(s, score = 200, bombsUsed = 0).err().message)
        val planned = e.setPlan(s, 50, 20, 30).ok()
        assertEquals(lockNoPlan, e.miniGameLock(planned))
        assertEquals(lockNoPlan, e.finishMiniGame(planned, score = 200, bombsUsed = 0).err().message)
    }

    @Test
    fun `мини-игра открыта после плана и снова закрыта в новой неделе`() {
        val s = ready()
        assertNull(e.miniGameLock(s))
        val played = e.finishMiniGame(s, score = 200, bombsUsed = 0).ok()
        assertEquals(s.balance + 200 / rules.miniGameScorePerCoin, played.balance)
        assertEquals(lockNoPlan, e.miniGameLock(end(played).state))
    }

    // ---------- C12. Parent bonus ----------

    @Test
    fun `бонус взрослого без питомца отклоняется`() =
        assertEquals("Сначала создай питомца", e.parentBonus(e.newGame(true), 0).err().message)

    @Test
    fun `бонус взрослого требует причину из списка`() {
        val s = newPet()
        assertEquals("Выберите причину бонуса", e.parentBonus(s, -1).err().message)
        assertEquals("Выберите причину бонуса", e.parentBonus(s, content.parentBonusReasons.size).err().message)
        e.parentBonus(s, content.parentBonusReasons.lastIndex).ok()
    }

    @Test
    fun `бонус взрослого начисляет монеты и пишет в журнал`() {
        val s = newPet() // plan not confirmed yet: the bonus does not need it
        val reason = content.parentBonusReasons[0]
        val r = e.parentBonus(s, 0) as Outcome.Ok
        assertEquals(s.balance + rules.parentBonusAmount, r.state.balance)
        assertEquals(1, r.state.parentBonusesThisPeriod)
        assertEquals(LedgerEntry("Бонус от взрослого: $reason", rules.parentBonusAmount), r.state.ledger.last())
        assertEquals(r.state.balance, r.state.ledger.sumOf { it.amount })
        assertEquals(
            listOf(
                "Бонус от взрослого: +${rules.parentBonusAmount} монет — «$reason». На балансе ${r.state.balance}.",
                "На этой неделе можно начислить ещё ${rules.parentBonusPerPeriod - 1}.",
            ),
            r.messages,
        )
        assertEquals(s.plan, r.state.plan)
        assertEquals(rules.parentBonusPerPeriod - 1, e.parentBonusesLeft(r.state))
    }

    @Test
    fun `бонус взрослого не меняет подтверждённый план`() {
        val s = ready()
        val r = e.parentBonus(s, 2).ok()
        assertEquals(s.plan, r.plan)
        assertTrue(r.plan.confirmed)
        assertEquals("Бонус от взрослого: Сам навёл порядок", r.ledger.last().text)
    }

    @Test
    fun `лимит бонусов взрослого за неделю и сброс в новой неделе`() {
        var s = ready()
        val start = s.balance
        lateinit var last: Outcome.Ok
        for (i in 1..rules.parentBonusPerPeriod) {
            last = e.parentBonus(s, 1) as Outcome.Ok
            s = last.state
        }
        assertEquals(rules.parentBonusPerPeriod, s.parentBonusesThisPeriod)
        assertEquals(0, e.parentBonusesLeft(s))
        assertEquals(start + rules.parentBonusPerPeriod * rules.parentBonusAmount, s.balance)
        assertEquals(limitReached, last.messages.last())
        assertTrue(last.messages.none { it.startsWith("На этой неделе можно начислить") })
        assertEquals(limitError, e.parentBonus(s, 1).err().message)

        val next = end(s).state
        assertEquals(0, next.parentBonusesThisPeriod)
        assertEquals(rules.parentBonusPerPeriod, e.parentBonusesLeft(next))
        assertEquals(1, e.parentBonus(next, 1).ok().parentBonusesThisPeriod)
    }

    @Test
    fun `сумма и лимит бонуса взрослого берутся из rules`() {
        val custom = economyWith(rules.copy(parentBonusAmount = 7, parentBonusPerPeriod = 1))
        val s = newPet(custom)
        val r = custom.parentBonus(s, 3) as Outcome.Ok
        assertEquals(s.balance + 7, r.state.balance)
        assertEquals(
            listOf("Бонус от взрослого: +7 монет — «Придумал, как сэкономить». На балансе ${s.balance + 7}.", limitReached),
            r.messages,
        )
        assertEquals(limitError, custom.parentBonus(r.state, 0).err().message)
    }

    @Test
    fun `остаток бонусов взрослого не бывает отрицательным`() {
        val s = newPet().copy(parentBonusesThisPeriod = rules.parentBonusPerPeriod + 2)
        assertEquals(0, e.parentBonusesLeft(s))
        assertEquals(limitError, e.parentBonus(s, 0).err().message)
    }

    // ---------- D. Match3 seed ----------

    @Test
    fun `поле мини-игры определяется переданным seed`() {
        val a = Match3.newGame(7, 6, 15, 0, 42L)
        assertEquals(a, Match3.newGame(7, 6, 15, 0, 42L))
        assertNotEquals(a.tiles, Match3.newGame(7, 6, 15, 0, 43L).tiles)
    }
}
