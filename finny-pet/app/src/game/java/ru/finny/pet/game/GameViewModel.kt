package ru.finny.pet.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import ru.finny.pet.data.ContentRepository
import ru.finny.pet.data.StateStore
import ru.finny.pet.domain.Content
import ru.finny.pet.domain.Economy
import ru.finny.pet.domain.Face
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.Goal
import ru.finny.pet.domain.Match3
import ru.finny.pet.domain.Match3State
import ru.finny.pet.domain.Need
import ru.finny.pet.domain.Outcome
import ru.finny.pet.domain.PeriodSummary
import ru.finny.pet.domain.QuizQuestion
import ru.finny.pet.domain.Turn
import ru.finny.pet.domain.Case
import ru.finny.pet.game.audio.Sound

sealed interface Screen {
    data object Title : Screen
    data object Intro : Screen
    data object CreatePet : Screen
    data object Room : Screen
    data object Plan : Screen
    data object Shop : Screen
    data object Savings : Screen
    data object Tasks : Screen
    data class Task(val id: String) : Screen
    data object MiniGame : Screen
    data object WeekEnd : Screen
    data object Progress : Screen
    data object Parent : Screen
    data object Glossary : Screen
}

enum class Mood { GOOD, INFO, OOPS }

class Feedback(val title: String, val messages: List<String>, val mood: Mood, val next: Screen? = null)

/** What the pet says right now; [options] turns it into a question. */
class Bubble(val text: String, val options: List<String> = emptyList(), val questionId: String? = null, val ttlMs: Long = 9000)

/** One-shot visual/audio effects the UI plays: the view model never touches Android views. */
sealed interface Effect {
    data class Sfx(val sound: Sound) : Effect
    data class Coins(val toTarget: String, val count: Int) : Effect   // fly coins from the pet to a HUD target
    data class CoinsFrom(val fromTarget: String, val toTarget: String, val count: Int) : Effect
    data object Confetti : Effect
    data object Hearts : Effect
    data object Sparkles : Effect
    data object Bubbles : Effect
    data class PetAction(val action: PetAct) : Effect
}

enum class PetAct { EAT, WASH, PLAY, HOP, SLEEP }

/** Thin glue: UI events → Economy → persisted state + effects. All rules live in Economy. */
class GameViewModel(app: Application) : AndroidViewModel(app) {
    val content: Content = ContentRepository.load(app)
    val economy = Economy(content)
    private val store = StateStore(app)

    // an old save may carry a speciesId/colorId no longer in content.json (e.g. dropped "dragon"): normalize to a known one (MVP-T12)
    var state: GameState by mutableStateOf(store.load().let { s -> s.pet?.let { p -> s.copy(pet = p.copy(speciesId = content.species(p.speciesId).id, colorId = content.color(p.colorId).id)) } ?: s })
        private set
    var screen: Screen by mutableStateOf(Screen.Title)
        private set
    var feedback: Feedback? by mutableStateOf(null)
        private set
    var bubble: Bubble? by mutableStateOf(null)
        private set
    var match: Match3State? by mutableStateOf(null)
        private set
    var matchBombsUsed: Int by mutableIntStateOf(0)
        private set
    /** Lives with the view model (not rememberSaveable in the screen), so a mid-game process death does not leak
     * the previous round's "over" panel into the next fresh game in the same Activity (MVP-T12). */
    var matchOver: Boolean by mutableStateOf(false)
    val effects = MutableSharedFlow<Effect>(extraBufferCapacity = 32)
    /** Lives with the view model, so the room replays only actions it has not shown yet (also after rotation). */
    val petAction = PetActionState()

    /** Increments when something good happened to the pet: the sprite hops. */
    var bounce: Int by mutableIntStateOf(0)
        private set

    val lastSummary: PeriodSummary? get() = state.history.lastOrNull()
    val face: Face get() = state.pet?.let { economy.face(it) } ?: Face.HAPPY

    private fun emit(e: Effect) { effects.tryEmit(e) }
    fun sfx(s: Sound) = emit(Effect.Sfx(s))

    fun navigate(to: Screen) {
        if (to != screen) { screen = to; sfx(Sound.WHOOSH); bubble = null }
    }

    /** Back always lands on a known parent; on Room the system back exits the app. The mini-game handles back itself. */
    fun back() {
        navigate(
            when (screen) {
                is Screen.Task -> Screen.Tasks
                Screen.Glossary -> Screen.Progress
                Screen.CreatePet, Screen.Title -> Screen.Title
                Screen.MiniGame -> return
                else -> if (state.hasProfile) Screen.Room else Screen.Title
            },
        )
    }

    fun start() { navigate(if (state.hasProfile) Screen.Room else Screen.Intro) }

    fun dismissFeedback() {
        val next = feedback?.next
        feedback = null
        if (next != null) navigate(next)
    }

    private fun apply(title: String, outcome: Outcome, next: Screen? = null, hop: Boolean = false, mood: Mood = Mood.GOOD): Boolean = when (outcome) {
        is Outcome.Ok -> {
            commit(outcome.state)
            if (hop) { bounce++; emit(Effect.PetAction(PetAct.HOP)) }
            if (outcome.messages.isNotEmpty()) feedback = Feedback(title, outcome.messages, mood, next)
            else if (next != null) navigate(next)
            true
        }
        is Outcome.Error -> {
            sfx(Sound.FAIL)
            feedback = Feedback("Пока не получится", listOf(outcome.message) + outcome.hints.map { "Вариант: $it" }, Mood.OOPS)
            false
        }
    }

    private fun commit(s: GameState) {
        state = s
        if (!store.save(s)) feedback = Feedback("Не удалось сохранить", listOf("Проверь, есть ли свободное место на устройстве"), Mood.OOPS)
    }

    // ---- profile
    fun createPet(name: String, speciesId: String, colorId: String) {
        if (apply("Знакомься!", economy.createPet(state, name, speciesId, colorId), next = Screen.Room)) { sfx(Sound.FANFARE); emit(Effect.Confetti) }
    }

    // ---- plan
    fun setPlan(mandatory: Int, optional: Int, savings: Int) = apply("План", economy.setPlan(state, mandatory, optional, savings))
    fun confirmPlan() { if (apply("План принят", economy.confirmPlan(state), next = Screen.Room)) sfx(Sound.SUCCESS) }

    // ---- shop
    fun buy(itemId: String) {
        val item = content.item(itemId)
        if (apply("Покупка", economy.buy(state, itemId), hop = false)) {
            sfx(Sound.COIN)
            emit(Effect.PetAction(when (item.need) { Need.FOOD -> PetAct.EAT; Need.CARE -> PetAct.WASH; Need.FUN, Need.UNPLANNED -> PetAct.PLAY }))
            emit(Effect.CoinsFrom("coins", "pet", (item.price / 10).coerceIn(3, 8)))
        }
    }

    // ---- savings
    fun deposit(amount: Int) { if (apply("Копилка", economy.deposit(state, amount))) { sfx(Sound.COIN); emit(Effect.CoinsFrom("coins", "piggy", (amount / 10).coerceIn(2, 8))) } }
    fun withdraw(amount: Int) { if (apply("Копилка", economy.withdraw(state, amount))) { sfx(Sound.COIN); emit(Effect.CoinsFrom("piggy", "coins", (amount / 10).coerceIn(2, 8))) } }
    fun chooseGoal(goal: Goal) { if (apply("Цель выбрана", economy.chooseGoal(state, goal))) { sfx(Sound.SUCCESS); emit(Effect.Sparkles) } }
    fun achieveGoal() { if (apply("Цель достигнута!", economy.achieveGoal(state), hop = true)) { sfx(Sound.FANFARE); emit(Effect.Confetti) } }

    // ---- tasks
    fun answerChoice(taskId: String, index: Int) = answered(economy.answerChoice(state, taskId, index))
    fun answerNumber(taskId: String, value: Int?) = answered(economy.answerNumber(state, taskId, value))

    private fun answered(outcome: Outcome) {
        val ok = apply("Ответ", outcome, next = Screen.Tasks, mood = Mood.INFO)
        if (ok) {
            val correct = state.taskResults.lastOrNull()?.correct == true
            if (correct) { bounce++; sfx(Sound.SUCCESS); emit(Effect.Sparkles); emit(Effect.Coins("coins", 6)) } else { sfx(Sound.POP); emit(Effect.Coins("coins", 2)) }
        }
    }

    // ---- pet chat & quiz
    fun petTapped() {
        val pet = state.pet ?: return
        sfx(Sound.POP); emit(Effect.Hearts)
        // a sad or so-so pet first says why (2.5.10); the next tap chats as usual
        val reason = economy.faceReason(pet)
        if (economy.face(pet) != Face.HAPPY && bubble?.text != reason) { bubble = Bubble(reason, ttlMs = 9000); return }
        val q = economy.availableQuiz(state)
        val ask = q.isNotEmpty() && (bubble?.questionId == null) && (state.quizResults.size + bounce) % 2 == 0
        bubble = if (ask) {
            val question = q.random()
            sfx(Sound.BUBBLE)
            Bubble(question.question, question.options, question.id, ttlMs = 60_000)
        } else {
            Bubble(nextLine(pet.name), ttlMs = 7000)
        }
    }

    /** Contextual idle line: needs first, then the next step, then chatter and facts. */
    fun nextLine(petName: String): String {
        val c = content.chatter
        val pet = state.pet
        val low = content.rules.needLowBelow
        val pool: List<String> = when {
            pet != null && pet.hunger < low && c.hungry.isNotEmpty() -> c.hungry
            pet != null && pet.clean < low && c.dirty.isNotEmpty() -> c.dirty
            pet != null && pet.mood < low && c.bored.isNotEmpty() -> c.bored
            else -> listOf(nextStep().first) + c.idle + c.facts
        }
        return pool.random().replace("{pet}", petName)
    }

    fun say(text: String, ttlMs: Long = 8000) { bubble = Bubble(text, ttlMs = ttlMs); sfx(Sound.BUBBLE) }
    fun clearBubble() { bubble = null }

    fun askQuestion() {
        val q = economy.availableQuiz(state).randomOrNull() ?: run { say("Все мои вопросы уже разобраны. Здорово!"); return }
        sfx(Sound.BUBBLE)
        bubble = Bubble(q.question, q.options, q.id, ttlMs = 60_000)
    }

    fun answerBubble(index: Int) {
        val id = bubble?.questionId ?: return
        when (val o = economy.answerQuiz(state, id, index)) {
            is Outcome.Ok -> {
                commit(o.state)
                val correct = o.state.quizResults.last().correct
                if (correct) { sfx(Sound.SUCCESS); emit(Effect.Sparkles); bounce++; emit(Effect.PetAction(PetAct.HOP)); match = match?.let { it.copy(bombs = it.bombs + content.rules.quizBombReward) } } else sfx(Sound.FAIL)
                bubble = Bubble(o.messages.joinToString(" "), ttlMs = 12_000)
            }
            is Outcome.Error -> bubble = Bubble(o.message)
        }
    }

    fun quizFor(id: String): QuizQuestion = content.quiz(id)

    /** What to do next: (pet line, button text, screen). Guides the child through the cycle. */
    fun nextStep(): Triple<String, String, Screen> {
        val s = state
        val name = s.pet?.name ?: "Питомец"
        return when {
            !s.plan.confirmed -> Triple("Давай разделим ${Economy.coins(s.balance, Case.ACC)}: обязательное, желаемое и копилка!", "Составить план", Screen.Plan)
            s.purchases.none { it.need == Need.FOOD } -> Triple("Я проголодался. В магазине есть корм!", "В магазин", Screen.Shop)
            s.purchases.none { it.need == Need.CARE } -> Triple("Мне бы шампунь или расчёску.", "В магазин", Screen.Shop)
            s.goal == null -> Triple("На что будем копить? Выбери цель!", "Выбрать цель", Screen.Savings)
            s.factSavings <= 0 && s.balance >= (content.rules.savingsAmounts.minOrNull() ?: content.rules.planStep) -> Triple("Отложи немного в копилку, и цель станет ближе.", "В копилку", Screen.Savings)
            economy.availableTasks(s).isNotEmpty() -> Triple("Есть новое задание. Решим вместе?", "К заданиям", Screen.Tasks)
            else -> Triple("Всё сделано! Заверши неделю, и $name подрастёт.", "Завершить неделю", Screen.WeekEnd)
        }
    }

    // ---- mini-game
    /** Closed until the week's plan is confirmed (2.8): the child gets the reason instead of the board. */
    fun startMiniGame() {
        economy.miniGameLock(state)?.let { reason ->
            sfx(Sound.FAIL)
            feedback = Feedback("Пока рано", listOf(reason, "Вариант: Открой «План» в комнате"), Mood.OOPS)
            return
        }
        match = Match3.newGame(moves = content.rules.miniGameMoves, bombs = state.bombs, seed = System.nanoTime())
        matchBombsUsed = 0
        matchOver = false
        navigate(Screen.MiniGame)
    }

    fun matchSwap(a: ru.finny.pet.domain.Cell, b: ru.finny.pet.domain.Cell): Turn? {
        val m = match ?: return null
        val t = Match3.swap(m, a, b) ?: run { sfx(Sound.POP); return null }
        match = t.state; sfx(Sound.MATCH); return t
    }

    fun matchBomb(at: ru.finny.pet.domain.Cell): Turn? {
        val m = match ?: return null
        val t = Match3.bomb(m, at) ?: return null
        match = t.state; matchBombsUsed++; sfx(Sound.BOMB); return t
    }

    /** The only way out of a round: used bombs are spent, coins (if any) are credited. */
    fun finishMiniGame() {
        val m = match ?: return
        val before = state.balance
        val o = economy.finishMiniGame(state, m.score, matchBombsUsed)
        match = null
        if (apply("Игра окончена", o, next = Screen.Room, mood = Mood.INFO)) {
            val earned = state.balance - before
            if (earned > 0) { sfx(Sound.SUCCESS); emit(Effect.Coins("coins", (earned / 5).coerceIn(3, 10))) }
        }
    }

    // ---- period
    fun endPeriod() {
        when (val o = economy.endPeriod(state)) {
            is Outcome.Ok -> { commit(o.state); sfx(Sound.SLEEP); navigate(Screen.WeekEnd) }
            is Outcome.Error -> feedback = Feedback("Пока не получится", listOf(o.message) + o.hints, Mood.OOPS)
        }
    }

    // ---- parent section
    fun setDemo(on: Boolean) = commit(state.copy(demo = on))
    fun setAnimations(on: Boolean) = commit(state.copy(animations = on))
    fun setSounds(on: Boolean) = commit(state.copy(sounds = on))
    fun setMusic(on: Boolean) = commit(state.copy(music = on))
    fun parentBonus(reasonIndex: Int) { if (apply("Бонус от взрослого", economy.parentBonus(state, reasonIndex))) sfx(Sound.COIN) }
    fun createTestProfile() { commit(economy.newGame(demo = true, state.animations, state.sounds, state.music)); navigate(Screen.Intro) }
    fun resetProfile() { commit(economy.resetProfile(state)); navigate(Screen.Title) }
    fun deleteProfile() { commit(economy.deleteProfile(state)); navigate(Screen.Title) }
}
