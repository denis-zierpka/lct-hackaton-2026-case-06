package ru.finny.pet.domain

import kotlinx.serialization.Serializable

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
) {
    val hasProfile: Boolean get() = pet != null
    val factMandatory: Int get() = purchases.filter { it.category == Category.MANDATORY }.sumOf { it.price }
    val factOptional: Int get() = purchases.filter { it.category == Category.OPTIONAL }.sumOf { it.price }
    val factSavings: Int get() = depositedThisPeriod - withdrawnThisPeriod
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
data class Purchase(val itemId: String, val title: String, val category: Category, val need: Need, val price: Int)

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
)

enum class Face { HAPPY, NEUTRAL, SAD }

sealed interface Outcome {
    data class Ok(val state: GameState, val messages: List<String> = emptyList()) : Outcome
    data class Error(val message: String, val hints: List<String> = emptyList()) : Outcome
}
