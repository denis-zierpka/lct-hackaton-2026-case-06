package ru.finny.pet.domain

import kotlinx.serialization.Serializable
import ru.finny.pet.domain.town.DiaryLine
import ru.finny.pet.domain.town.EventState
import ru.finny.pet.domain.town.SeenPrice
import ru.finny.pet.domain.town.Source

/** The whole local game profile. Persisted as one JSON file. */
@Serializable
data class GameState(
    val demo: Boolean = false,
    val animations: Boolean = true,
    val pet: Pet? = null,
    val period: Int = 1,
    val balance: Int = 0,
    val savings: Int = 0,
    val plan: BudgetPlan = BudgetPlan(),
    val purchases: List<Purchase> = emptyList(),
    val depositedThisPeriod: Int = 0,
    val withdrawnThisPeriod: Int = 0,
    val depositHistory: List<Int> = emptyList(),
    val goal: Goal? = null,
    val achievedGoals: List<Goal> = emptyList(),
    val taskResults: List<TaskResult> = emptyList(),
    val history: List<PeriodSummary> = emptyList(),
    val ledger: List<LedgerEntry> = emptyList(),
    // game edition
    val sounds: Boolean = true,
    val music: Boolean = false,
    val bombs: Int = 0,
    val miniGameEarned: Int = 0,
    val quizResults: List<TaskResult> = emptyList(),
    val parentBonusesThisPeriod: Int = 0,
    // town (TOWN-S0a §5)
    val stateVersion: Int = 0,
    val day: Int = 1,
    val asleep: Boolean = false,
    val seed: Long = 0,
    val jarNeed: Int = 0,
    val jarWant: Int = 0,
    val envelope: List<LedgerEntry> = emptyList(),
    val owned: List<String> = emptyList(),
    val placed: Map<String, String> = emptyMap(),
    val broken: List<String> = emptyList(),
    val houseColor: String? = null,
    val shiftsThisPeriod: Int = 0,
    val jobShifts: Map<String, Int> = emptyMap(),
    val records: Map<String, Int> = emptyMap(),
    val events: List<EventState> = emptyList(),
    val stickers: List<String> = emptyList(),
    val visited: List<String> = emptyList(),
    val seenPrices: Map<String, SeenPrice> = emptyMap(),
    val notes: List<String> = emptyList(),
    val planDraft: BudgetPlan? = null,
    val freeFunDay: Int = 0,
    val diary: List<DiaryLine> = emptyList(),
    // town shifts (TOWN-S1b §0)
    val riddles: List<TaskResult> = emptyList(),
    val riddleAsked: Boolean = false,
) {
    val hasProfile: Boolean get() = pet != null
    val factMandatory: Int get() = purchases.filter { it.category == Category.MANDATORY }.sumOf { it.price }
    val factOptional: Int get() = purchases.filter { it.category == Category.OPTIONAL }.sumOf { it.price }
    val factSavings: Int get() = depositedThisPeriod - withdrawnThisPeriod
    /** «Запас» (до плана — «Не разложено»): кошелёк минус банки; не сериализуется (TOWN-S1a §0). */
    val reserve: Int get() = balance - jarNeed - jarWant
}

@Serializable
data class Pet(
    val name: String,
    val speciesId: String,
    val colorId: String,
    val hunger: Int = 70,
    val clean: Int = 70,
    val mood: Int = 70,
    val growth: Int = 0,
)

@Serializable
data class BudgetPlan(
    val mandatory: Int = 0,
    val optional: Int = 0,
    val savings: Int = 0,
    val confirmed: Boolean = false,
) {
    val total: Int get() = mandatory + optional + savings
}

@Serializable
data class Purchase(
    val itemId: String,
    val title: String,
    val category: Category,
    val need: Need,
    val price: Int,
    val shop: String? = null,
    val source: Source? = null,
)

@Serializable
data class Goal(val id: String, val title: String, val emoji: String, val price: Int)

/** Every balance change has a source and an amount (requirement 2.5.4). */
@Serializable
data class LedgerEntry(val text: String, val amount: Int)

@Serializable
data class TaskResult(val taskId: String, val correct: Boolean, val reward: Int, val period: Int)

@Serializable
data class PeriodSummary(
    val period: Int,
    val plan: BudgetPlan,
    val factMandatory: Int,
    val factOptional: Int,
    val factSavings: Int,
    val mandatoryCovered: Boolean,
    val planKept: Boolean,
    val saved: Boolean,
    val score: Int,
    val growthBefore: Int,
    val growthAfter: Int,
    val stageBefore: Int,
    val stageAfter: Int,
    val messages: List<String>,
    /** Off-plan coins of the period: mini-game and adult bonus (2.8). */
    val miniGameEarned: Int = 0,
    val parentBonus: Int = 0,
    val shiftEarned: Int = 0,
)

enum class Face { HAPPY, NEUTRAL, SAD }

sealed interface Outcome {
    data class Ok(val state: GameState, val messages: List<String> = emptyList()) : Outcome
    data class Error(val message: String, val hints: List<String> = emptyList()) : Outcome
}
