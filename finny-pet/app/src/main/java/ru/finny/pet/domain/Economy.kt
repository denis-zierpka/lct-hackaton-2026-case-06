package ru.finny.pet.domain

import kotlin.math.abs
import kotlin.math.ceil

/** Case of «монета» after a number: what the sentence needs (MVP-T01a). */
enum class Case { NOM, ACC, GEN }

/**
 * Pure game rules. No Android, no IO. Every public function returns a new state and
 * human-readable messages explaining what changed and why (requirement 2.5.9).
 * Formulas are documented in docs/ECONOMY.md and covered by EconomyTest.
 */
class Economy(val content: Content) {
    val rules: Rules get() = content.rules

    fun newGame(demo: Boolean, animations: Boolean = true, sounds: Boolean = true, music: Boolean = false) =
        GameState(demo = demo, animations = animations, sounds = sounds, music = music)

    // ---------- profile ----------

    fun createPet(s: GameState, name: String, speciesId: String, colorId: String): Outcome {
        val n = name.trim()
        if (n.isEmpty()) return Outcome.Error("Придумай имя питомцу")
        if (n.length > rules.maxNameLength) return Outcome.Error("Имя слишком длинное — до ${rules.maxNameLength} букв")
        if (content.species.none { it.id == speciesId } || content.colors.none { it.id == colorId }) {
            return Outcome.Error("Выбери вид и цвет питомца")
        }
        val state = s.copy(
            pet = Pet(name = n, speciesId = speciesId, colorId = colorId, hunger = rules.startStat, clean = rules.startStat, mood = rules.startStat),
            period = 1,
            balance = rules.allowance,
            savings = 0,
            plan = BudgetPlan(),
            purchases = emptyList(),
            ledger = listOf(LedgerEntry("Карманные деньги на неделю", rules.allowance)),
        )
        return Outcome.Ok(state, listOf("$n получает первые ${coins(rules.allowance, Case.ACC)} на неделю!"))
    }

    fun resetProfile(s: GameState): GameState = newGame(s.demo, s.animations, s.sounds, s.music)

    fun deleteProfile(s: GameState): GameState = GameState(animations = s.animations, sounds = s.sounds, music = s.music)

    // ---------- plan ----------

    fun setPlan(s: GameState, mandatory: Int, optional: Int, savings: Int): Outcome {
        if (s.plan.confirmed) return Outcome.Error("План на эту неделю уже подтверждён")
        if (mandatory < 0 || optional < 0 || savings < 0) return Outcome.Error("Сумма не может быть меньше нуля")
        val total = mandatory + optional + savings
        if (total > s.balance) {
            return Outcome.Error(
                "План больше, чем есть монет: $total из ${s.balance}",
                listOf("Уменьши одну из частей на ${total - s.balance}"),
            )
        }
        return Outcome.Ok(s.copy(plan = BudgetPlan(mandatory, optional, savings)))
    }

    fun confirmPlan(s: GameState): Outcome {
        val p = s.plan
        if (p.confirmed) return Outcome.Error("План уже подтверждён")
        if (p.total > s.balance) return Outcome.Error("План больше, чем есть монет: ${p.total} из ${s.balance}")
        if (p.total == 0) return Outcome.Error("Распредели хотя бы часть монет", listOf("Начни с обязательного: еда и уход"))
        val rest = s.balance - p.total
        val msgs = mutableListOf("План на неделю принят: обязательное ${p.mandatory}, желаемое ${p.optional}, копилка ${p.savings}.")
        msgs += if (rest > 0) "Вне плана осталось ${coins(rest)} — это запас." else "Все монеты распределены."
        if (p.mandatory == 0) msgs += "На обязательное в плане 0 монет, а еда и уход всё равно понадобятся — оставь на них монеты."
        return Outcome.Ok(s.copy(plan = p.copy(confirmed = true)), msgs)
    }

    // ---------- shop ----------

    fun buy(s: GameState, itemId: String, price: Int = content.item(itemId).price): Outcome {
        val item = content.item(itemId)
        val pet = s.pet ?: return Outcome.Error("Сначала создай питомца")
        if (!s.plan.confirmed) return Outcome.Error("Сначала составь план на неделю", listOf("Открой «План» на главном экране"))
        if (price > s.balance) {
            val missing = price - s.balance
            val hints = mutableListOf<String>()
            if (availableTasks(s).isNotEmpty()) hints += "Выполни задание — за него дают монеты"
            content.items
                .filter { it.need == item.need && it.price <= s.balance && it.id != item.id }
                .maxByOrNull { it.price }
                ?.let { hints += "Есть дешевле: ${it.title} за ${coins(it.price, Case.ACC)}" }
            if (s.savings >= missing) hints += "Можно взять из копилки, но цель отодвинется"
            hints += "Или подожди новую неделю — придут карманные деньги"
            return Outcome.Error("Не хватает ${coins(missing, Case.GEN)}: цена ${price}, у тебя ${s.balance}", hints)
        }
        val newBalance = s.balance - price
        val newPet = pet.copy(
            hunger = clamp(pet.hunger + item.hunger),
            clean = clamp(pet.clean + item.clean),
            mood = clamp(pet.mood + item.mood),
        )
        val purchases = s.purchases + Purchase(item.id, item.title, item.category, item.need, price)
        val msgs = mutableListOf("Баланс: −${coins(price)}, осталось $newBalance.")
        if (item.hunger != 0) msgs += "Сытость ${signed(item.hunger)} (теперь ${newPet.hunger})."
        if (item.clean != 0) msgs += "Чистота ${signed(item.clean)} (теперь ${newPet.clean})."
        if (item.mood != 0) msgs += "Настроение ${signed(item.mood)} (теперь ${newPet.mood})."
        msgs += item.reaction.replace("{pet}", pet.name)
        val spent = purchases.filter { it.category == item.category }.sumOf { it.price }
        val planned = if (item.category == Category.MANDATORY) s.plan.mandatory else s.plan.optional
        val catTitle = if (item.category == Category.MANDATORY) "обязательное" else "желаемое"
        msgs += if (spent > planned) {
            "На $catTitle потрачено $spent из $planned по плану — больше плана. Не страшно: учти это в следующем плане."
        } else {
            "На $catTitle потрачено $spent из $planned по плану."
        }
        val state = s.copy(
            balance = newBalance,
            pet = newPet,
            purchases = purchases,
            ledger = s.ledger + LedgerEntry("Покупка: ${item.title}", -price),
        )
        return Outcome.Ok(state, msgs)
    }

    // ---------- savings ----------

    fun deposit(s: GameState, amount: Int): Outcome {
        if (!s.plan.confirmed) return Outcome.Error("Сначала составь план на неделю", listOf("Открой «План» на главном экране"))
        if (amount <= 0) return Outcome.Error("Введи сумму больше нуля")
        if (amount > s.balance) {
            return Outcome.Error("Не хватает ${coins(amount - s.balance, Case.GEN)}: у тебя ${s.balance}", listOf("Отложи меньше или выполни задание"))
        }
        val state = s.copy(
            balance = s.balance - amount,
            savings = s.savings + amount,
            depositedThisPeriod = s.depositedThisPeriod + amount,
            ledger = s.ledger + LedgerEntry("В копилку", -amount),
        )
        val msgs = mutableListOf("В копилку: +${coins(amount)}, теперь там ${state.savings}. На балансе ${state.balance}.")
        msgs += goalProgressMessage(state)
        return Outcome.Ok(state, msgs)
    }

    /** What happens if [amount] is taken from savings — shown before the user confirms (2.5.7). */
    fun withdrawPreview(s: GameState, amount: Int): List<String> {
        val after = s.copy(savings = s.savings - amount, withdrawnThisPeriod = s.withdrawnThisPeriod + amount)
        val msgs = mutableListOf("В копилке останется ${coins(after.savings)} вместо ${s.savings}.")
        val goal = s.goal
        if (goal != null) {
            msgs += "До цели «${goal.title}» будет не хватать ${coins(remaining(after), Case.GEN)} вместо ${remaining(s)}."
            val etaBefore = goalEta(s)
            val etaAfter = goalEta(after)
            if (etaBefore != null && etaAfter != null && etaAfter != etaBefore) {
                msgs += "Срок достижения цели: ${weeks(etaBefore)} → ${weeks(etaAfter)}."
            }
        }
        return msgs
    }

    /** Unlike deposit, withdrawing needs no plan: taking coins out is how you fund this week's plan. */
    fun withdraw(s: GameState, amount: Int): Outcome {
        if (amount <= 0) return Outcome.Error("Введи сумму больше нуля")
        if (amount > s.savings) return Outcome.Error("В копилке только ${coins(s.savings)}")
        val state = s.copy(
            balance = s.balance + amount,
            savings = s.savings - amount,
            withdrawnThisPeriod = s.withdrawnThisPeriod + amount,
            ledger = s.ledger + LedgerEntry("Из копилки", amount),
        )
        val msgs = mutableListOf("Из копилки: −${coins(amount)}, осталось ${state.savings}. На балансе ${state.balance}.")
        msgs += goalProgressMessage(state)
        return Outcome.Ok(state, msgs)
    }

    fun chooseGoal(s: GameState, goal: Goal): Outcome {
        val state = s.copy(goal = goal)
        return Outcome.Ok(state, listOf("Цель: «${goal.title}» за ${coins(goal.price, Case.ACC)}.", goalProgressMessage(state)))
    }

    fun customGoal(title: String, price: Int): Goal = Goal("custom_${title.hashCode()}_$price", title, "⭐", price)

    fun achieveGoal(s: GameState): Outcome {
        val goal = s.goal ?: return Outcome.Error("Сначала выбери цель")
        val pet = s.pet ?: return Outcome.Error("Сначала создай питомца")
        if (s.savings < goal.price) return Outcome.Error("Пока не хватает ${coins(goal.price - s.savings, Case.GEN)}")
        val newPet = pet.copy(mood = clamp(pet.mood + rules.goalMoodBonus))
        val state = s.copy(
            savings = s.savings - goal.price,
            goal = null,
            achievedGoals = s.achievedGoals + goal,
            pet = newPet,
            ledger = s.ledger + LedgerEntry("Цель «${goal.title}» достигнута", 0),
        )
        return Outcome.Ok(
            state,
            listOf(
                "Ура! «${goal.title}» — твоя! Копилка: −${goal.price}, осталось ${state.savings}.",
                "${pet.name} прыгает от радости: настроение +${rules.goalMoodBonus} (теперь ${newPet.mood}).",
                "Выбери новую цель, чтобы копить дальше.",
            ),
        )
    }

    fun remaining(s: GameState): Int = (s.goal?.price ?: 0) - s.savings

    /**
     * Average net deposit per period (2.5.7: the estimate is based on the average top-up).
     * The running period counts only once something was deposited, so the forecast does not
     * halve every time a new week starts.
     */
    fun averageDeposit(s: GameState): Double {
        val current = s.factSavings.coerceAtLeast(0)
        val periods = s.depositHistory.size + if (current > 0) 1 else 0
        if (periods == 0) return 0.0
        return (s.depositHistory.sum() + current).toDouble() / periods
    }

    /** Periods left to reach the goal, or null when there is no data to estimate. */
    fun goalEta(s: GameState): Int? {
        val goal = s.goal ?: return null
        val left = goal.price - s.savings
        if (left <= 0) return 0
        val avg = averageDeposit(s)
        if (avg <= 0) return null
        return ceil(left / avg).toInt()
    }

    fun goalProgressMessage(s: GameState): String {
        val goal = s.goal ?: return "Цель пока не выбрана — выбери её, чтобы знать, ради чего копишь."
        val left = remaining(s)
        if (left <= 0) return "Накоплено на «${goal.title}»! Забери цель на экране копилки."
        val eta = goalEta(s)
        val etaText = if (eta == null) "Откладывай регулярно — и я посчитаю срок." else "При таком темпе — ещё ${weeks(eta)}."
        return "До цели «${goal.title}» осталось ${coins(left)}. $etaText"
    }

    // ---------- tasks ----------

    fun availableTasks(s: GameState): List<Task> =
        content.tasks.filter { t -> s.taskResults.none { it.taskId == t.id } && (s.demo || t.unlockPeriod <= s.period) }

    /** Results whose task was removed from content are skipped, not fatal. */
    fun completedTasks(s: GameState): List<Pair<Task, TaskResult>> =
        s.taskResults.mapNotNull { r -> content.taskOrNull(r.taskId)?.let { it to r } }

    fun answerChoice(s: GameState, taskId: String, optionIndex: Int): Outcome {
        val task = content.task(taskId)
        if (task.type != TaskType.CHOICE) return Outcome.Error("Это задание с числом")
        val opt = task.options.getOrNull(optionIndex) ?: return Outcome.Error("Выбери вариант")
        return applyAnswer(s, task, opt.correct, opt.explanation)
    }

    fun answerNumber(s: GameState, taskId: String, value: Int?): Outcome {
        val task = content.task(taskId)
        if (task.type != TaskType.NUMBER) return Outcome.Error("Это задание с выбором")
        if (value == null) return Outcome.Error("Введи число")
        val correct = value == task.answer
        return applyAnswer(s, task, correct, if (correct) task.explanationCorrect else task.explanationWrong)
    }

    private fun applyAnswer(s: GameState, task: Task, correct: Boolean, explanation: String): Outcome {
        val pet = s.pet ?: return Outcome.Error("Сначала создай питомца")
        if (availableTasks(s).none { it.id == task.id }) return Outcome.Error("Это задание уже выполнено или ещё закрыто")
        val reward = if (correct) rules.rewardCorrect else rules.rewardWrong
        val newPet = if (correct) pet.copy(mood = clamp(pet.mood + rules.correctMoodBonus)) else pet
        val state = s.copy(
            balance = s.balance + reward,
            pet = newPet,
            taskResults = s.taskResults + TaskResult(task.id, correct, reward, s.period),
            ledger = s.ledger + LedgerEntry("Задание «${task.title}»", reward),
        )
        val msgs = mutableListOf(explanation, "Награда: +${coins(reward)}, на балансе ${state.balance}.")
        if (correct) msgs += "${pet.name} гордится тобой: настроение +${rules.correctMoodBonus} (теперь ${newPet.mood})."
        else msgs += "Ничего страшного — теперь ты знаешь, как лучше. Монеты за старание тоже даются."
        return Outcome.Ok(state, msgs)
    }

    // ---------- game edition: quiz and mini-game ----------

    /** Questions not yet answered correctly, oldest first; wrong answers can be retried. */
    fun availableQuiz(s: GameState): List<QuizQuestion> =
        content.quiz.filter { q -> s.quizResults.none { it.taskId == q.id && it.correct } }

    /** Each question is answered correctly once. A correct answer arms quizBombReward bombs for the mini-game and cheers the pet; a wrong one just explains. No coins: chat is not a farm. */
    fun answerQuiz(s: GameState, questionId: String, index: Int): Outcome {
        val q = content.quiz(questionId)
        val pet = s.pet ?: return Outcome.Error("Сначала создай питомца")
        if (availableQuiz(s).none { it.id == q.id }) return Outcome.Error("Этот вопрос уже решён")
        val correct = index == q.correct
        val newPet = if (correct) pet.copy(mood = clamp(pet.mood + rules.quizMoodBonus)) else pet
        val state = s.copy(
            pet = newPet,
            bombs = if (correct) s.bombs + rules.quizBombReward else s.bombs,
            quizResults = s.quizResults + TaskResult(q.id, correct, 0, s.period),
        )
        val msgs = mutableListOf(q.explanation)
        msgs += if (correct) "Верно! ${pet.name} даёт тебе бомбочку для игры «Монетки в ряд»." else "Не страшно: теперь ты знаешь. Спроси ещё раз позже."
        return Outcome.Ok(state, msgs)
    }

    fun miniGameCoinsLeft(s: GameState): Int = (rules.miniGameCap - s.miniGameEarned).coerceAtLeast(0)

    /** Why the mini-game is closed, or null when it can be played: extras open only after the plan (2.8). */
    fun miniGameLock(s: GameState): String? = when {
        s.pet == null -> "Сначала создай питомца"
        !s.plan.confirmed -> "Сначала составь план на неделю — игра откроется после него"
        else -> null
    }

    /** Converts a finished round into coins (capped per period) and consumes used bombs. Source and amount go to the ledger (2.5.4). */
    fun finishMiniGame(s: GameState, score: Int, bombsUsed: Int): Outcome {
        miniGameLock(s)?.let { return Outcome.Error(it) }
        val earned = (score / rules.miniGameScorePerCoin).coerceIn(0, miniGameCoinsLeft(s))
        val state = s.copy(
            balance = s.balance + earned,
            miniGameEarned = s.miniGameEarned + earned,
            bombs = (s.bombs - bombsUsed).coerceAtLeast(0),
            ledger = if (earned > 0) s.ledger + LedgerEntry("Игра «Монетки в ряд»", earned) else s.ledger,
        )
        val msgs = mutableListOf("Очки: $score. Монеты: +$earned (на балансе ${state.balance}).")
        if (earned == 0 && miniGameCoinsLeft(s) == 0) msgs += "На этой неделе игра уже принесла ${coins(rules.miniGameCap, Case.ACC)} — больше только на следующей."
        else if (miniGameCoinsLeft(state) > 0) msgs += "За игру на этой неделе можно получить ещё ${coins(miniGameCoinsLeft(state), Case.ACC)}."
        else msgs += "Недельный лимит игры (${coins(rules.miniGameCap)}) набран. Остальное — из плана и заданий."
        return Outcome.Ok(state, msgs)
    }

    // ---------- parent bonus (2.5.12) ----------

    fun parentBonusesLeft(s: GameState): Int = (rules.parentBonusPerPeriod - s.parentBonusesThisPeriod).coerceAtLeast(0)

    /** An adult grants coins for a listed reason, limited per period. Like task rewards, the bonus does not touch the plan. */
    fun parentBonus(s: GameState, reasonIndex: Int): Outcome {
        if (s.pet == null) return Outcome.Error("Сначала создай питомца")
        val reason = content.parentBonusReasons.getOrNull(reasonIndex) ?: return Outcome.Error("Выберите причину бонуса")
        if (parentBonusesLeft(s) == 0) return Outcome.Error("На этой неделе все бонусы уже начислены — новые будут со следующей недели.")
        val amount = rules.parentBonusAmount
        val state = s.copy(
            balance = s.balance + amount,
            parentBonusesThisPeriod = s.parentBonusesThisPeriod + 1,
            ledger = s.ledger + LedgerEntry("Бонус от взрослого: $reason", amount),
        )
        val left = parentBonusesLeft(state)
        return Outcome.Ok(
            state,
            listOf(
                "Бонус от взрослого: +${coins(amount)} — «$reason». На балансе ${state.balance}.",
                if (left > 0) "На этой неделе можно начислить ещё $left." else "Лимит бонусов на эту неделю исчерпан.",
            ),
        )
    }

    // ---------- period ----------

    fun canEndPeriod(s: GameState): Boolean = s.plan.confirmed

    fun endPeriod(s: GameState): Outcome {
        val pet = s.pet ?: return Outcome.Error("Сначала создай питомца")
        if (!s.plan.confirmed) return Outcome.Error("Сначала составь и подтверди план", listOf("Открой «План» на главном экране"))
        val plan = s.plan
        val hasFood = s.purchases.any { it.need == Need.FOOD }
        val hasCare = s.purchases.any { it.need == Need.CARE }
        val mandatoryCovered = hasFood && hasCare
        // a net withdrawal counts as 0 saved: taking coins out funds the plan, it does not break it
        val netSavings = s.factSavings.coerceAtLeast(0)
        val planKept = s.factMandatory <= plan.mandatory && s.factOptional <= plan.optional && netSavings >= plan.savings
        val saved = s.factSavings > 0
        val checks = listOf(mandatoryCovered, planKept, saved)
        val score = checks.count { it }

        val msgs = mutableListOf<String>()
        var hunger = pet.hunger - rules.decayHunger
        var clean = pet.clean - rules.decayClean
        var mood = pet.mood - rules.decayMood
        msgs += "Прошла неделя: сытость −${rules.decayHunger}, чистота −${rules.decayClean}, настроение −${rules.decayMood}."
        if (mandatoryCovered) {
            msgs += "Еда и уход куплены — ${pet.name} сыт и ухожен. Так держать!"
        } else {
            mood -= rules.uncoveredMoodDrop
            if (!hasFood) { hunger -= rules.uncoveredExtraDrop; msgs += "Еды на этой неделе не было — сытость ещё −${rules.uncoveredExtraDrop}." }
            if (!hasCare) { clean -= rules.uncoveredExtraDrop; msgs += "Ухода на этой неделе не было — чистота ещё −${rules.uncoveredExtraDrop}." }
            msgs += "Настроение −${rules.uncoveredMoodDrop}. На следующей неделе начни с обязательного: еда и уход."
        }
        if (planKept) {
            mood += rules.planKeptMoodBonus
            msgs += "Траты совпали с планом — настроение +${rules.planKeptMoodBonus}."
        } else {
            if (s.factMandatory > plan.mandatory) msgs += "На обязательное потрачено ${s.factMandatory}, а в плане было ${plan.mandatory}."
            if (s.factOptional > plan.optional) msgs += "На желаемое потрачено ${s.factOptional}, а в плане было ${plan.optional}."
            if (netSavings < plan.savings) {
                val d = s.depositedThisPeriod
                val w = s.withdrawnThisPeriod
                msgs += if (w == 0) "В копилку отложено $d, а в плане было ${plan.savings}."
                else "В копилку чистыми $netSavings (положено $d, взято $w), а в плане было ${plan.savings}."
            }
            msgs += "План — это обещание себе. В следующий раз поставь суммы, которые сможешь соблюсти, или откажись от лишней покупки."
        }
        if (saved) {
            mood += rules.savedMoodBonus
            msgs += "Копилка выросла на ${coins(s.factSavings, Case.ACC)} — цель ближе, настроение +${rules.savedMoodBonus}."
        } else {
            msgs += "На этой неделе копилка не выросла. Попробуй отложить хотя бы ${coins(rules.savingsAmounts.firstOrNull() ?: rules.planStep, Case.ACC)} — так цель приблизится."
        }

        val growthBefore = pet.growth
        val growthAfter = growthBefore + score
        val stageBefore = stageIndex(growthBefore)
        val stageAfter = stageIndex(growthAfter)
        msgs += "Рост: +$score из ${checks.size} (всего $growthAfter)."
        val left = nextStageLeft(growthAfter)
        msgs += when {
            stageAfter > stageBefore -> "${pet.name} вырос! Теперь ${stageTitle(growthAfter).lowercase()}."
            left != null -> "До следующей стадии: ${points(left)} роста."
            else -> "${pet.name} достиг высшей стадии — так держать!"
        }
        // off-plan coins are shown apart from the plan checks (2.8)
        val parentBonus = s.parentBonusesThisPeriod * rules.parentBonusAmount
        if (s.miniGameEarned > 0) msgs += "Вне плана: игра «Монетки в ряд» принесла ${coins(s.miniGameEarned, Case.ACC)}."
        if (parentBonus > 0) msgs += "Вне плана: бонус от взрослого — ${coins(parentBonus)}."
        msgs += "Новая неделя: +${coins(rules.allowance)} карманных денег."

        val summary = PeriodSummary(
            period = s.period, plan = plan,
            factMandatory = s.factMandatory, factOptional = s.factOptional, factSavings = s.factSavings,
            mandatoryCovered = mandatoryCovered, planKept = planKept, saved = saved, score = score,
            growthBefore = growthBefore, growthAfter = growthAfter, stageBefore = stageBefore, stageAfter = stageAfter,
            messages = msgs, miniGameEarned = s.miniGameEarned, parentBonus = parentBonus,
        )
        val state = s.copy(
            pet = pet.copy(hunger = clampFloor(hunger), clean = clampFloor(clean), mood = clampFloor(mood), growth = growthAfter),
            period = s.period + 1,
            balance = s.balance + rules.allowance,
            plan = BudgetPlan(),
            purchases = emptyList(),
            depositedThisPeriod = 0,
            withdrawnThisPeriod = 0,
            miniGameEarned = 0,
            parentBonusesThisPeriod = 0,
            depositHistory = s.depositHistory + s.factSavings.coerceAtLeast(0), // withdrawals don't count as negative saving in the forecast

            history = s.history + summary,
            ledger = listOfNotNull(
                LedgerEntry("Остаток с прошлой недели", s.balance).takeIf { s.balance > 0 },
                LedgerEntry("Карманные деньги на неделю", rules.allowance),
            ),
        )
        return Outcome.Ok(state, msgs)
    }

    // ---------- pet ----------

    fun stageIndex(growth: Int): Int = rules.stageThresholds.indexOfLast { growth >= it }.coerceAtLeast(0)

    fun stageTitle(growth: Int): String = rules.stageTitles.getOrNull(stageIndex(growth)) ?: "Стадия ${stageIndex(growth) + 1}"

    fun nextStageLeft(growth: Int): Int? = rules.stageThresholds.firstOrNull { it > growth }?.let { it - growth }

    /** A single neglected need makes the pet sad even if a toy lifted its mood: needs come first. */
    fun face(pet: Pet): Face {
        val min = minOf(pet.hunger, pet.clean, pet.mood)
        val avg = (pet.hunger + pet.clean + pet.mood) / 3
        return when {
            min < rules.faceSadBelow -> Face.SAD
            avg >= rules.faceHappyAvg && min >= rules.needLowBelow -> Face.HAPPY
            else -> Face.NEUTRAL
        }
    }

    fun faceReason(pet: Pet): String {
        val low = listOfNotNull(
            "голодный".takeIf { pet.hunger < rules.needLowBelow },
            "хочет помыться".takeIf { pet.clean < rules.needLowBelow },
            "скучает".takeIf { pet.mood < rules.needLowBelow },
        )
        return when (face(pet)) {
            Face.HAPPY -> "${pet.name} доволен: сыт, чист и в хорошем настроении."
            Face.NEUTRAL -> if (low.isEmpty()) "${pet.name} в порядке, но можно лучше." else "${pet.name} ${low.joinToString(" и ")} — помоги ему."
            Face.SAD -> "${pet.name} грустит: ${low.joinToString(", ").ifEmpty { "ему нужна забота" }}. Купи еду или уход, и всё наладится."
        }
    }

    private fun clamp(v: Int) = v.coerceIn(0, 100)
    private fun clampFloor(v: Int) = v.coerceIn(rules.statFloor, 100)
    private fun signed(v: Int) = if (v > 0) "+$v" else "$v"

    companion object {
        fun weeks(n: Int): String {
            val r10 = n % 10
            val r100 = n % 100
            val word = when {
                r100 in 11..14 -> "недель"
                r10 == 1 -> "неделя"
                r10 in 2..4 -> "недели"
                else -> "недель"
            }
            return "$n $word"
        }

        /** "$n монета/монеты/монет" agreeing with |n| in [case], so texts stay right for any number from content.json. */
        fun coins(n: Int, case: Case = Case.NOM): String {
            val words = when (case) {
                Case.NOM -> listOf("монета", "монеты", "монет")
                Case.ACC -> listOf("монету", "монеты", "монет")
                Case.GEN -> listOf("монеты", "монет", "монет")
            }
            return "$n ${words[form(n)]}"
        }

        /** "$n очко/очка/очков" agreeing with |n|, same rule as [coins] (MVP-T11). */
        fun points(n: Int): String = "$n ${listOf("очко", "очка", "очков")[form(n)]}"

        private fun form(n: Int): Int {
            val r = abs(n % 100)
            return when {
                r % 10 == 1 && r != 11 -> 0
                r % 10 in 2..4 && r !in 12..14 -> 1
                else -> 2
            }
        }
    }
}
