package ru.finny.pet.ui

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import ru.finny.pet.data.ContentRepository
import ru.finny.pet.data.StateStore
import ru.finny.pet.domain.Content
import ru.finny.pet.domain.Economy
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.Goal
import ru.finny.pet.domain.Outcome
import ru.finny.pet.domain.PeriodSummary

sealed interface Screen {
    data object Onboarding : Screen
    data object CreatePet : Screen
    data object Home : Screen
    data object Plan : Screen
    data object Shop : Screen
    data object Savings : Screen
    data object Tasks : Screen
    data class Task(val id: String) : Screen
    data object Progress : Screen
    data object Glossary : Screen
    data object Parent : Screen
    data object Summary : Screen
}

class Feedback(val title: String, val messages: List<String>, val icon: ImageVector, val next: Screen? = null)

/** Thin glue: UI events → Economy → persisted state. All rules live in Economy. */
class GameViewModel(app: Application) : AndroidViewModel(app) {
    val content: Content = ContentRepository.load(app)
    val economy = Economy(content)
    private val store = StateStore(app)

    var state: GameState by mutableStateOf(store.load())
        private set
    var screen: Screen by mutableStateOf(if (state.hasProfile) Screen.Home else Screen.Onboarding)
        private set
    var feedback: Feedback? by mutableStateOf(null)
        private set
    var error: Outcome.Error? by mutableStateOf(null)
        private set

    /** Increments when something good happened to the pet — PetView hops on change. */
    var bounce: Int by mutableIntStateOf(0)
        private set

    val lastSummary: PeriodSummary? get() = state.history.lastOrNull()

    fun navigate(to: Screen) { screen = to }

    /** Back always lands on a known parent; the system back on Home exits the app. */
    fun back() {
        screen = when (screen) {
            is Screen.Task -> Screen.Tasks
            Screen.Glossary -> Screen.Progress
            Screen.CreatePet -> Screen.Onboarding
            else -> if (state.hasProfile) Screen.Home else Screen.Onboarding
        }
    }

    fun dismissFeedback() {
        val next = feedback?.next
        feedback = null
        if (next != null) screen = next
    }

    fun dismissError() { error = null }

    private fun apply(title: String, icon: ImageVector, outcome: Outcome, next: Screen? = null, hop: Boolean = false): Boolean = when (outcome) {
        is Outcome.Ok -> {
            commit(outcome.state)
            if (hop) bounce++
            if (outcome.messages.isNotEmpty()) feedback = Feedback(title, outcome.messages, icon, next)
            else if (next != null) screen = next
            true
        }
        is Outcome.Error -> { error = outcome; false }
    }

    private fun commit(s: GameState) {
        state = s
        if (!store.save(s)) {
            error = Outcome.Error("Не удалось сохранить прогресс", listOf("Проверь, есть ли свободное место на устройстве"))
        }
    }

    // ---- profile
    fun startCreatePet() { screen = Screen.CreatePet }
    fun createPet(name: String, speciesId: String, colorId: String) =
        apply("Знакомься!", Icons.Outlined.Pets, economy.createPet(state, name, speciesId, colorId), next = Screen.Home)

    // ---- plan
    fun setPlan(mandatory: Int, optional: Int, savings: Int) = apply("План", Icons.AutoMirrored.Outlined.EventNote, economy.setPlan(state, mandatory, optional, savings))
    fun confirmPlan() = apply("План принят", Icons.AutoMirrored.Outlined.EventNote, economy.confirmPlan(state), next = Screen.Home)

    // ---- shop
    fun buy(itemId: String) = apply("Покупка", Icons.Outlined.ShoppingCart, economy.buy(state, itemId), hop = true)

    // ---- savings
    fun deposit(amount: Int) = apply("Копилка", Icons.Outlined.Savings, economy.deposit(state, amount))
    fun withdraw(amount: Int) = apply("Копилка", Icons.Outlined.Savings, economy.withdraw(state, amount))
    fun chooseGoal(goal: Goal) = apply("Цель выбрана", Icons.Outlined.Savings, economy.chooseGoal(state, goal))
    fun achieveGoal() = apply("Цель достигнута!", Icons.Outlined.Celebration, economy.achieveGoal(state), hop = true)

    // ---- tasks
    fun answerChoice(taskId: String, index: Int) = answered(economy.answerChoice(state, taskId, index))
    fun answerNumber(taskId: String, value: Int?) = answered(economy.answerNumber(state, taskId, value))

    /** The pet hops only for a correct answer; a wrong one still pays 5 coins but is not celebrated. */
    private fun answered(outcome: Outcome): Boolean {
        val ok = apply("Ответ", Icons.Outlined.Lightbulb, outcome, next = Screen.Tasks)
        if (ok && state.taskResults.lastOrNull()?.correct == true) bounce++
        return ok
    }

    // ---- period
    fun endPeriod() {
        when (val o = economy.endPeriod(state)) {
            is Outcome.Ok -> { commit(o.state); screen = Screen.Summary }
            is Outcome.Error -> error = o
        }
    }

    // ---- parent section
    fun setDemo(on: Boolean) = commit(state.copy(demo = on))
    fun setAnimations(on: Boolean) = commit(state.copy(animations = on))
    fun createTestProfile() { commit(economy.newGame(demo = true, animations = state.animations)); screen = Screen.Onboarding }
    fun resetProfile() { commit(economy.resetProfile(state)); screen = Screen.Onboarding }
    fun deleteProfile() { commit(economy.deleteProfile(state)); screen = Screen.Onboarding }
}
