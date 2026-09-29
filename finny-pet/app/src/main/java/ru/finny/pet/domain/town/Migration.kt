package ru.finny.pet.domain.town

import ru.finny.pet.domain.Content
import ru.finny.pet.domain.GameState

/** Перенос сохранения 1.3.0 на схему «Городка» (TOWN-S1a §7, §16.3): монеты не появляются и не исчезают. */
class Migration(private val content: Content) {
    private val town = content.town ?: error("content.json: нет ключа town")

    fun migrate(s: GameState): GameState {
        if (s.stateVersion >= 1) return s
        var st = s
        if (s.plan.confirmed) {
            val jn = (s.plan.mandatory - s.factMandatory).coerceIn(0, s.balance)
            val jw = (s.plan.optional - s.factOptional).coerceIn(0, s.balance - jn)
            st = st.copy(jarNeed = jn, jarWant = jw)
        }
        s.purchases.mapNotNull { content.itemOrNull(it.itemId) }.filter { it.keep }
            .forEach { st = town.putHome(st, it) }
        return st.copy(
            seed = if (s.seed == 0L) MIGRATION_SEED else s.seed,
            day = 1, asleep = false, stateVersion = 1,
        )
    }

    companion object {
        const val MIGRATION_SEED = 20260926L
    }
}
