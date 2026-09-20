package ru.finny.pet.domain

import kotlinx.serialization.Serializable

/** Educational content. Lives in assets/content/content.json, never in code. */
@Serializable
data class Content(
    val rules: Rules = Rules(),
    val species: List<PetSpecies>,
    val colors: List<PetColor>,
    val items: List<ShopItem>,
    val goals: List<GoalTemplate>,
    val customGoal: CustomGoalOptions,
    val tasks: List<Task>,
    val glossary: List<GlossaryEntry>,
    /** Short questions the pet asks in chat and that arm bombs in the mini-game (game edition). */
    val quiz: List<QuizQuestion> = emptyList(),
    /** Idle lines the pet says in the room (game edition); {pet} is replaced with the name. */
    val chatter: Chatter = Chatter(),
) {
    fun quiz(id: String): QuizQuestion = quiz.first { it.id == id }
    fun item(id: String): ShopItem = items.first { it.id == id }
    fun task(id: String): Task = tasks.first { it.id == id }
    fun taskOrNull(id: String): Task? = tasks.firstOrNull { it.id == id }

    /** Saved profiles may reference ids removed from content; fall back to the first option instead of crashing. */
    fun color(id: String): PetColor = colors.firstOrNull { it.id == id } ?: colors.first()
    fun species(id: String): PetSpecies = species.firstOrNull { it.id == id } ?: species.first()
}

/** Numbers that drive the game economy. Documented in docs/ECONOMY.md. */
@Serializable
data class Rules(
    val allowance: Int = 100,
    val rewardCorrect: Int = 20,
    val rewardWrong: Int = 5,
    val decayHunger: Int = 30,
    val decayClean: Int = 25,
    val decayMood: Int = 10,
    val uncoveredExtraDrop: Int = 20,
    val uncoveredMoodDrop: Int = 20,
    val planKeptMoodBonus: Int = 10,
    val savedMoodBonus: Int = 10,
    val goalMoodBonus: Int = 30,
    val correctMoodBonus: Int = 5,
    val statFloor: Int = 10,
    val stageThresholds: List<Int> = listOf(0, 4, 9),
    val stageTitles: List<String> = listOf("Малыш", "Подросток", "Взрослый"),
    val maxNameLength: Int = 12,
    /** Mini-game (game edition): coins per period cap, score needed per coin, moves per round, bombs per correct quiz answer. */
    val miniGameCap: Int = 30,
    val miniGameScorePerCoin: Int = 20,
    val miniGameMoves: Int = 15,
    val quizMoodBonus: Int = 3,
)

@Serializable
data class PetSpecies(val id: String, val title: String)

@Serializable
data class PetColor(val id: String, val title: String, val hex: String)

@Serializable
enum class Category { MANDATORY, OPTIONAL }

/** What a purchase satisfies. FOOD and CARE are the mandatory needs checked at period end. */
@Serializable
enum class Need { FOOD, CARE, FUN }

@Serializable
data class ShopItem(
    val id: String,
    val title: String,
    val emoji: String,
    val category: Category,
    val need: Need,
    val price: Int,
    val hunger: Int = 0,
    val clean: Int = 0,
    val mood: Int = 0,
    val description: String,
    val reaction: String,
)

@Serializable
data class GoalTemplate(val id: String, val title: String, val emoji: String, val price: Int)

@Serializable
data class CustomGoalOptions(val titles: List<String>, val prices: List<Int>)

@Serializable
enum class Theme(val title: String) {
    BUDGET("Планирование бюджета"),
    SAVINGS("Сбережения"),
    SHOPPING("Платежи и покупки"),
}

@Serializable
enum class TaskType { CHOICE, NUMBER }

@Serializable
data class TaskOption(val text: String, val correct: Boolean, val explanation: String)

@Serializable
data class Task(
    val id: String,
    val theme: Theme,
    val title: String,
    val situation: String,
    val type: TaskType,
    val options: List<TaskOption> = emptyList(),
    val answer: Int? = null,
    val explanationCorrect: String = "",
    val explanationWrong: String = "",
    val unlockPeriod: Int = 1,
)

@Serializable
data class GlossaryEntry(val term: String, val definition: String)

@Serializable
data class QuizQuestion(val id: String, val theme: Theme, val question: String, val options: List<String>, val correct: Int, val explanation: String)

@Serializable
data class Chatter(
    val idle: List<String> = emptyList(),
    val hungry: List<String> = emptyList(),
    val dirty: List<String> = emptyList(),
    val bored: List<String> = emptyList(),
    val proud: List<String> = emptyList(),
    val facts: List<String> = emptyList(),
)
