package ru.finny.pet.domain

import ru.finny.pet.data.ContentRepository
import java.io.File

/** Real production content, loaded from assets so tests validate what ships. */
object TestContent {
    val content: Content by lazy {
        val f = File("src/main/assets/content/content.json")
        ContentRepository.parse(f.readText())
    }
    val economy: Economy by lazy { Economy(content) }

    /** A fresh profile with the plan confirmed, ready to shop. */
    fun readyState(demo: Boolean = true): GameState {
        val e = economy
        var s = e.newGame(demo)
        s = (e.createPet(s, "Финни", "cat", "orange") as Outcome.Ok).state
        s = (e.setPlan(s, 50, 20, 30) as Outcome.Ok).state
        s = (e.confirmPlan(s) as Outcome.Ok).state
        return s
    }
}

fun Outcome.ok(): GameState = (this as? Outcome.Ok)?.state ?: error("expected Ok, got $this")
fun Outcome.err(): Outcome.Error = (this as? Outcome.Error) ?: error("expected Error, got $this")
