package ru.finny.pet.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import ru.finny.pet.data.ContentRepository
import ru.finny.pet.data.StateStore
import ru.finny.pet.domain.Cell
import ru.finny.pet.domain.Content
import ru.finny.pet.domain.Economy
import ru.finny.pet.domain.Face
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.Goal
import ru.finny.pet.domain.Match3
import ru.finny.pet.domain.Match3State
import ru.finny.pet.domain.Need
import ru.finny.pet.domain.Outcome
import ru.finny.pet.domain.QuizQuestion
import ru.finny.pet.domain.ShopItem
import ru.finny.pet.domain.Turn
import ru.finny.pet.domain.town.EventDef
import ru.finny.pet.domain.town.EventResult
import ru.finny.pet.domain.town.JobGame
import ru.finny.pet.domain.town.Migration
import ru.finny.pet.domain.town.PetLine
import ru.finny.pet.domain.town.Quote
import ru.finny.pet.domain.town.Recovery
import ru.finny.pet.domain.town.ShiftQuote
import ru.finny.pet.domain.town.Source
import ru.finny.pet.domain.town.Template
import ru.finny.pet.domain.town.Town
import ru.finny.pet.domain.town.TownContent
import ru.finny.pet.domain.town.TownOutcome
import ru.finny.pet.domain.town.TownResult
import ru.finny.pet.domain.town.Trigger
import ru.finny.pet.domain.town.petLine
import ru.finny.pet.game.audio.Sound

sealed interface Screen {
    data object Title : Screen
    data object Intro : Screen
    data object CreatePet : Screen
    data object Room : Screen
    data object Jars : Screen
    data object Savings : Screen
    data object Arrange : Screen
    data object Night : Screen
    data object WeekEnd : Screen
    data object Street : Screen
    data class Place(val placeId: String) : Screen
    data class Round(val jobId: String) : Screen
    data object Board : Screen
    data object Progress : Screen
    data object Glossary : Screen
    data object Parent : Screen
}

/** One engine line for the LINE bubble (§B.3): the line and «Почему?». */
class Line(val text: String, val why: List<String> = emptyList())

/** What the night or the week end brought: the engine's line, why and event results. */
class Report(val line: Line, val results: List<EventResult>)

/** One-shot visual/audio effects the UI plays: the view model never touches Android views. */
sealed interface Effect {
    data class Sfx(val sound: Sound) : Effect
    /** Coins fly from one named target to another; played only when both are on the screen (§F). */
    data class CoinsFrom(val fromTarget: String, val toTarget: String, val count: Int) : Effect
    data object Confetti : Effect
    data object Hearts : Effect
    data class PetAction(val action: PetAct) : Effect
}

enum class PetAct { EAT, WASH, PLAY, HOP, SLEEP }

/** Thin glue: UI events → Town → persisted state + effects. All rules live in domain/town (TOWN-S1d §B). */
class GameViewModel(app: Application) : AndroidViewModel(app) {
    val content: Content = ContentRepository.load(app)
    val economy = Economy(content)
    val town = Town(content)
    val tc: TownContent = content.town ?: error("content.json: нет ключа town")
    private val migration = Migration(content)
    private val store = StateStore(app)

    var state: GameState by mutableStateOf(GameState())
        private set

    // ---- navigation: a stack, Room (or Night while asleep) is the bottom (§B.6)
    private val stack = mutableStateListOf<Screen>(Screen.Title)
    val screen: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1 || screen is Screen.Round

    /** LINE queue: the first one is on screen, a tap shows the next. */
    val lines = mutableStateListOf<Line>()
    /** Ids of events that came while playing: «новое» on the Board. */
    val fresh = mutableStateListOf<String>()
    /** The last shop the child opened; «Лавки» goes there (not saved). */
    var lastShop by mutableStateOf("market")
    var petLine: PetLine? by mutableStateOf(null)
        private set
    private var petTaps = 0
    var night: Report? by mutableStateOf(null)
        private set
    var weekEnd: Report? by mutableStateOf(null)
        private set

    // ---- round
    var match: Match3State? by mutableStateOf(null)
        private set
    var matchBombsUsed: Int by mutableIntStateOf(0)
        private set
    /** Lives with the view model, so a rotation keeps the round-over panel. */
    var matchOver: Boolean by mutableStateOf(false)
    val taps = mutableStateListOf<Boolean>()
    /** The shift result on the Round screen (job_result); null while playing. */
    var roundResult: Line? by mutableStateOf(null)
        private set

    val effects = MutableSharedFlow<Effect>(extraBufferCapacity = 32)
    /** Lives with the view model, so the room replays only actions it has not shown yet (also after rotation). */
    val petAction = PetActionState()
    /** Increments when something good happened to the pet: the sprite hops. */
    var bounce: Int by mutableIntStateOf(0)
        private set

    val face: Face get() = state.pet?.let { economy.face(it) } ?: Face.HAPPY

    init {
        // an old save may carry a speciesId/colorId no longer in content.json: normalize to a known one (MVP-T12)
        val loaded = store.load().let { s -> s.pet?.let { p -> s.copy(pet = p.copy(speciesId = content.species(p.speciesId).id, colorId = content.color(p.colorId).id)) } ?: s }
        settle(migration.migrate(loaded))
    }

    private fun emit(e: Effect) { effects.tryEmit(e) }
    fun sfx(s: Sound) = emit(Effect.Sfx(s))

    // ---------- navigation ----------

    fun navigate(to: Screen) {
        val t = if (to == Screen.Room && state.asleep) Screen.Night else to
        when {
            t == Screen.Room || t == Screen.Night || t == Screen.Title -> { stack.clear(); stack += t }
            t == screen -> return
            // switching shops replaces the shop: «Назад» from a shop is the screen before the shops
            t is Screen.Place && screen is Screen.Place -> stack[stack.lastIndex] = t
            else -> stack += t
        }
        sfx(Sound.WHOOSH)
        petLine = null
    }

    /** ⌂: the stack clears down to Room (Night while asleep); in a round it is «Закончить». */
    fun home() { if (screen is Screen.Round && roundResult == null) finishRound() else navigate(Screen.Room) }

    fun back() {
        if (screen is Screen.Round) { if (roundResult == null) finishRound() else closeRound(); return }
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
        if (screen == Screen.Room && state.asleep) { stack.clear(); stack += Screen.Night }
        petLine = null
    }

    fun start() { navigate(if (state.hasProfile) Screen.Room else Screen.Intro) }

    // ---------- LINE and outcomes (§B.3) ----------

    fun say(text: String, why: List<String> = emptyList()) { if (text.isNotBlank()) lines += Line(text, why) }
    fun closeLine() { if (lines.isNotEmpty()) lines.removeAt(0) }

    private fun commit(s: GameState) {
        state = s
        if (!store.save(s)) say("Не удалось сохранить", listOf("Проверь, есть ли свободное место на устройстве"))
    }

    /** Commit, the line and why, results of events the action settled, then the intro of the first event that came. */
    private fun show(o: TownOutcome, results: Boolean = true) {
        commit(o.state)
        say(o.line, o.why)
        if (results) o.eventResults.forEach { say(it.line) }
        arrived(o.arrived)
    }

    private fun arrived(ids: List<String>) {
        val first = ids.firstOrNull() ?: return
        tc.events.firstOrNull { it.id == first }?.let { say(it.intro) }
        if (first !in fresh) fresh += first
    }

    /** Done → commit and LINE, then [done]; Refused → LINE and the FAIL sound. */
    private inline fun act(r: TownResult, done: (TownOutcome) -> Unit = {}): Boolean = when (r) {
        is TownResult.Done -> { show(r.outcome); done(r.outcome); true }
        is TownResult.Refused -> { say(r.line); sfx(Sound.FAIL); false }
    }

    /** Economy outcome for the allowed calls (setPlan, chooseGoal, createPet). */
    private fun ok(o: Outcome): GameState? = when (o) {
        is Outcome.Ok -> { commit(o.state); o.state }
        is Outcome.Error -> { say(o.message); sfx(Sound.FAIL); null }
    }

    /** After load and a new profile: planned arrival check (TOWN-S1c §2). */
    private fun settle(s: GameState) {
        if (s.pet == null) { state = s; return }
        show(town.tick(s))
    }

    private fun seeded(s: GameState): GameState = migration.migrate(if (s.seed == 0L) s.copy(seed = System.nanoTime()) else s)

    // ---------- profile ----------

    fun createPet(name: String, speciesId: String, colorId: String) {
        val o = economy.createPet(state, name, speciesId, colorId)
        if (o is Outcome.Ok) {
            lines.clear()
            say(o.messages.firstOrNull().orEmpty())
            settle(seeded(o.state).also(::commit))
            navigate(Screen.Room)
            sfx(Sound.FANFARE); emit(Effect.Confetti)
        } else ok(o)
    }

    // ---------- home: plan, jars, piggy bank ----------

    fun setPlan(mandatory: Int, optional: Int, savings: Int) { ok(economy.setPlan(state, mandatory, optional, savings)) }

    fun confirmPlan() {
        val sv = state.plan.savings
        act(town.confirmPlan(state)) {
            sfx(Sound.SUCCESS)
            if (sv > 0) emit(Effect.CoinsFrom("jar_save", "piggy", coinsFor(sv)))
        }
    }

    fun transfer(from: Source, amount: Int) { act(town.transfer(state, from, Source.NEED, amount)) { sfx(Sound.COIN) } }
    fun transferPreview(from: Source, amount: Int) = town.transferPreview(state, from, Source.NEED, amount)

    fun deposit(from: Source, amount: Int) {
        act(town.deposit(state, from, amount)) { sfx(Sound.COIN); emit(Effect.CoinsFrom("coins", "piggy", coinsFor(amount))) }
    }
    fun depositPreview(from: Source, amount: Int) = town.depositPreview(state, from, amount)

    fun withdraw(amount: Int) {
        act(town.withdraw(state, amount)) { sfx(Sound.COIN); emit(Effect.CoinsFrom("piggy", "coins", coinsFor(amount))) }
    }
    fun withdrawPreview(amount: Int) = town.withdrawPreview(state, amount)

    /** A dream from the town showcase (goalId) — or any ready Goal (own dream). */
    fun chooseGoal(goalId: String) {
        val g = tc.goals.firstOrNull { it.id == goalId } ?: return
        chooseGoal(Goal(g.id, g.title, g.emoji, g.price))
    }

    fun chooseGoal(goal: Goal) {
        val s = ok(economy.chooseGoal(state, goal)) ?: return
        say("Копим на «${goal.title}»: ${s.savings} / ${goal.price}")
        sfx(Sound.SUCCESS)
    }

    fun achieveGoal() { act(town.achieveGoal(state)) { sfx(Sound.FANFARE); emit(Effect.Confetti); bounce++; emit(Effect.PetAction(PetAct.HOP)) } }

    fun place(spotId: String, itemId: String?) { act(town.place(state, spotId, itemId)) }

    fun item(id: String): ShopItem? = content.items.firstOrNull { it.id == id } ?: tc.items.firstOrNull { it.id == id }

    /** Items of the first live event with a list (П1): the week's shopping list for the jars and the fridge. */
    val needList: List<String> get() = tc.events.firstOrNull { it.id !in tc.eventsOff && it.params.list.isNotEmpty() }?.params?.list.orEmpty()

    /** «{T} — {цена} {shop.at}» by the last seen price, or «{T} — ?». */
    fun seenLine(id: String): String {
        val title = item(id)?.title ?: id
        val seen = state.seenPrices[id] ?: return "$title — ?"
        val at = tc.shops.firstOrNull { it.id == seen.shop }?.at.orEmpty()
        return "$title — ${seen.price} $at".trim()
    }

    // ---------- the pet (§A) ----------

    fun petTapped() {
        petLine = town.petLine(state, petTaps++).takeIf { it.text.isNotBlank() }
        sfx(Sound.POP); emit(Effect.Hearts)
    }
    fun closePetLine() { petLine = null }

    // ---------- day, night, week (§D.6–§D.8) ----------

    /** Bed: the last day ends the week, other days end the day. */
    fun sleep() {
        if (state.plan.confirmed && state.day >= tc.rules.daysPerWeek) { endWeek(); return }
        when (val r = town.sleep(state)) {
            is TownResult.Done -> {
                val o = r.outcome
                commit(o.state)
                night = Report(Line(o.line, o.why), o.eventResults)
                arrived(o.arrived)
                sfx(Sound.SLEEP)
                navigate(Screen.Night)
            }
            is TownResult.Refused -> { say(r.line); sfx(Sound.FAIL) }
        }
    }

    fun endWeek() {
        when (val r = town.endWeek(state)) {
            is TownResult.Done -> {
                val o = r.outcome
                commit(o.state)
                weekEnd = Report(Line(o.line, o.why), o.eventResults)
                night = null
                arrived(o.arrived)
                sfx(Sound.SLEEP)
                stack.clear(); stack += Screen.Night; stack += Screen.WeekEnd
                petLine = null
            }
            is TownResult.Refused -> { say(r.line); sfx(Sound.FAIL) }
        }
    }

    fun wake() {
        val s = state
        val envelope = !s.plan.confirmed && s.period > 1 && s.day == 1
        if (act(town.wake(s))) {
            night = null
            navigate(Screen.Room)
            if (envelope) { sfx(Sound.COIN); emit(Effect.CoinsFrom("mail", "coins", coinsFor(state.balance))) }
        }
    }

    fun planTweaks(): List<Recovery.PlanTweak> = state.history.lastOrNull()?.let { town.planTweaks(state, it) }.orEmpty()
    fun chooseTweak(t: Recovery.PlanTweak?) = act(town.chooseTweak(state, t))

    fun weekEndPreview(): List<String> = town.weekEndPreview(state)

    // ---------- town (API for the town screens, §B.4) ----------

    /** Every way into a place: visit (prices, ENTER triggers), LINE for arrivals, then the place's screen. */
    fun openPlace(placeId: String) {
        val place = tc.places.firstOrNull { it.id == placeId } ?: return
        show(town.visit(state, placeId))
        when (place.template) {
            Template.HOME -> navigate(Screen.Room)
            Template.SHOP -> { lastShop = placeId; navigate(Screen.Place(placeId)) }
            Template.JOB, Template.SCENE -> navigate(Screen.Place(placeId))
        }
    }

    /** Where an event lives (§B.7): home → Room with the intro, no place → Board, else the place. */
    fun goEvent(eventId: String, sayIntro: Boolean = true) {
        val e = tc.events.firstOrNull { it.id == eventId } ?: return
        when (val p = e.place) {
            null -> navigate(Screen.Board)
            "home" -> { navigate(Screen.Room); if (sayIntro) say(e.intro) }
            else -> openPlace(p)
        }
    }

    fun quote(itemId: String, shopId: String): Quote = town.quote(state, itemId, shopId)

    fun buy(itemId: String, shopId: String, source: Source) {
        val price = town.quote(state, itemId, shopId).price
        val need = item(itemId)?.need
        act(town.buyAt(state, itemId, shopId, source)) {
            sfx(Sound.COIN)
            emit(Effect.PetAction(when (need) { Need.FOOD -> PetAct.EAT; Need.CARE -> PetAct.WASH; else -> PetAct.PLAY }))
            emit(Effect.CoinsFrom("coins", "pet", coinsFor(price)))
        }
    }

    fun makeGoal(itemId: String) { act(town.makeGoal(state, itemId)) { sfx(Sound.SUCCESS) } }
    fun pass(eventId: String) { act(town.pass(state, eventId)) }
    fun startEvent(eventId: String) { if (act(town.startEvent(state, eventId))) goEvent(eventId, sayIntro = false) }

    fun eventsAt(placeId: String): List<EventDef> = town.activeEvents(state).filter { e ->
        e.place == placeId || e.triggers.any { it is Trigger.Enter && it.place == placeId }
    }

    fun ordersAt(placeId: String): List<EventDef> = town.orders(state).filter { e ->
        e.place == placeId || tc.jobs.firstOrNull { it.id == e.params.job }?.place == placeId
    }

    fun shiftQuote(jobId: String): ShiftQuote = town.shiftQuote(state, jobId)

    fun startRound(jobId: String) {
        val q = town.shiftQuote(state, jobId)
        val job = tc.jobs.firstOrNull { it.id == jobId }
        if (job == null || !q.canPlay) { say(q.line); sfx(Sound.FAIL); return }
        roundResult = null
        taps.clear()
        match = null
        if (job.game == JobGame.MATCH3) {
            val b = job.board ?: return
            val moves = (if (state.demo) job.demoMoves ?: job.moves else job.moves) ?: return
            match = Match3.newGame(b.w, b.h, moves, state.bombs + q.levelBombs, seed = System.nanoTime())
            matchBombsUsed = 0
            matchOver = false
        } else {
            taps.addAll(List(job.tasks.size) { false })
        }
        navigate(Screen.Round(jobId))
    }

    fun matchSwap(a: Cell, b: Cell): Turn? {
        val m = match ?: return null
        val t = Match3.swap(m, a, b) ?: run { sfx(Sound.POP); return null }
        match = t.state; sfx(Sound.MATCH); return t
    }

    fun matchBomb(at: Cell): Turn? {
        val m = match ?: return null
        val t = Match3.bomb(m, at) ?: return null
        match = t.state; matchBombsUsed++; sfx(Sound.BOMB); return t
    }

    fun tapTask(i: Int) { if (i in taps.indices) { taps[i] = !taps[i]; sfx(Sound.TAP) } }

    /** «Закончить»: Town.finishShift, the result stays on the Round screen until [closeRound]. */
    fun finishRound() {
        val jobId = (screen as? Screen.Round)?.jobId ?: return
        if (roundResult != null) return
        val job = tc.jobs.firstOrNull { it.id == jobId } ?: return
        val score = if (job.game == JobGame.MATCH3) match?.score ?: 0 else taps.count { it }
        val bombs = if (job.game == JobGame.MATCH3) matchBombsUsed else 0
        val before = state.envelope.sumOf { it.amount }
        when (val r = town.finishShift(state, jobId, score, bombs)) {
            is TownResult.Done -> {
                val o = r.outcome
                commit(o.state)
                roundResult = Line(o.line, o.why)
                arrived(o.arrived)
                sfx(Sound.SUCCESS)
                val earned = state.envelope.sumOf { it.amount } - before
                if (earned > 0) emit(Effect.CoinsFrom("job_result", "mail", coinsFor(earned)))
            }
            is TownResult.Refused -> { roundResult = Line(r.line); sfx(Sound.FAIL) }
        }
        match = null
        matchOver = false
    }

    /** [Готово] after the shift: back to the job's place. */
    fun closeRound() {
        roundResult = null
        if (screen is Screen.Round) stack.removeAt(stack.lastIndex)
        if (stack.isEmpty()) stack += Screen.Room
    }

    fun nextQuestion(): QuizQuestion? = town.nextQuestion(state)

    fun answerRiddle(questionId: String, i: Int) {
        val bombs = state.bombs
        act(town.answerQuestion(state, questionId, i)) { sfx(if (state.bombs > bombs) Sound.SUCCESS else Sound.POP) }
    }

    // ---------- parent section ----------

    fun setDemo(on: Boolean) = commit(state.copy(demo = on))
    fun setAnimations(on: Boolean) = commit(state.copy(animations = on))
    fun setSounds(on: Boolean) = commit(state.copy(sounds = on))
    fun setMusic(on: Boolean) = commit(state.copy(music = on))
    fun parentBonus(reasonIndex: Int) { act(town.parentBonus(state, reasonIndex)) { sfx(Sound.COIN) } }
    fun createTestProfile() { reset(economy.newGame(demo = true, state.animations, state.sounds, state.music)); stack += Screen.Intro }
    fun resetProfile() = reset(economy.resetProfile(state))
    fun deleteProfile() = reset(economy.deleteProfile(state))

    private fun reset(s: GameState) {
        lines.clear(); fresh.clear(); night = null; weekEnd = null
        settle(seeded(s).also(::commit))
        navigate(Screen.Title)
    }

    /** How many coins fly for an amount: 2–8 (animation only, not an economy number). */
    private fun coinsFor(amount: Int) = (amount / 10).coerceIn(2, 8)
}
