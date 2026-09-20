package ru.finny.pet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the minimum demo content volume from section 2.6 of the ТЗ. */
class ContentTest {
    private val c = TestContent.content

    @Test
    fun `at least 9 pet combinations`() = assertTrue(c.species.size * c.colors.size >= 9)

    @Test
    fun `at least 6 tasks covering 3 themes`() {
        assertTrue(c.tasks.size >= 6)
        assertEquals(Theme.entries.toSet(), c.tasks.map { it.theme }.toSet())
        assertTrue(c.tasks.any { it.type == TaskType.NUMBER }) // not only multiple choice
    }

    @Test
    fun `at least 8 shop items of both types`() {
        assertTrue(c.items.size >= 8)
        assertTrue(c.items.count { it.category == Category.MANDATORY } >= 2)
        assertTrue(c.items.count { it.category == Category.OPTIONAL } >= 2)
        assertTrue(c.items.any { it.need == Need.FOOD })
        assertTrue(c.items.any { it.need == Need.CARE })
    }

    @Test
    fun `at least 3 goals and 3 stages`() {
        assertTrue(c.goals.size >= 3)
        assertTrue(c.rules.stageThresholds.size >= 3)
        assertEquals(c.rules.stageThresholds.size, c.rules.stageTitles.size)
    }

    @Test
    fun `every task is well formed`() {
        c.tasks.forEach { t ->
            assertTrue(t.id, t.situation.isNotBlank())
            when (t.type) {
                TaskType.CHOICE -> {
                    assertTrue(t.id, t.options.size >= 2)
                    assertEquals(t.id, 1, t.options.count { it.correct })
                    assertTrue(t.id, t.options.all { it.explanation.isNotBlank() })
                }
                TaskType.NUMBER -> {
                    assertTrue(t.id, t.answer != null)
                    assertTrue(t.id, t.explanationCorrect.isNotBlank() && t.explanationWrong.isNotBlank())
                }
            }
        }
        assertEquals(c.tasks.size, c.tasks.map { it.id }.toSet().size)
        assertEquals(c.items.size, c.items.map { it.id }.toSet().size)
    }

    @Test
    fun `all tasks are reachable within 5 demo periods in normal mode`() =
        assertTrue(c.tasks.all { it.unlockPeriod in 1..5 })
}
