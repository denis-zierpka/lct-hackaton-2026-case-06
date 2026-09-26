package ru.finny.pet.domain.town

import kotlinx.serialization.Serializable
import ru.finny.pet.domain.GameState

/** Ход одного события в сохранении; outcome — индекс в outcomes, -1 — сработал default (§16.2). */
@Serializable
data class EventState(val id: String, val status: EventStatus, val verdict: Verdict? = null, val outcome: Int? = null, val period: Int, val day: Int)

/** Последняя увиденная цена товара — для холодильника (§5.4, §6.1). */
@Serializable
data class SeenPrice(val shop: String, val price: Int, val period: Int)

/** Строка дневника питомца за игровой день (§8). */
@Serializable
data class DiaryLine(val period: Int, val day: Int, val text: String)

/** Итог действия: состояние, одна строка ребёнку, «Почему?» до 3 строк, эффекты для анимаций, исходы событий (§7.3, §8). */
data class TownOutcome(val state: GameState, val line: String = "", val why: List<String> = emptyList(), val effects: List<EventEffect> = emptyList(), val eventResults: List<EventResult> = emptyList())

/** Итог одного сработавшего события внутри действия движка. */
data class EventResult(val eventId: String, val verdict: Verdict, val line: String, val sticker: String? = null, val recovery: List<Recovery> = emptyList())
/** Итог действия: выполнено или отказ кассы (§6.4). */
sealed interface TownResult {
    data class Done(val outcome: TownOutcome) : TownResult
    /** Отказ с объяснением; варианты кассы — срез 1. */
    data class Refused(val line: String) : TownResult
}

/** События «Городка»: приход, запуск и засчитывание фактов (§7.3, §7.4); реализация — срез 1. */
interface Events {
    /** События, которые можно начать сейчас; в демо — все (§7.4). */
    fun available(s: GameState): List<EventDef>
    /** Запуск события: применяет setup, в демо — demo.setup (§7.4). */
    fun start(s: GameState, id: String): TownResult
    /** Вход в место: ENTER-триггеры, RETURN_LATER, visited (§7.3, §7.5 Пк2). */
    fun enter(s: GameState, placeId: String): TownOutcome
    /** place — где совершено действие; SKIP, CHECK_CHANGE, MAKE_GOAL, MAKE_CARD, ATTEND, DEPOSIT, WITHDRAW приходят сюда без обёртки Town (§7.3). */
    fun observe(s: GameState, fact: Fact, place: String? = null): TownOutcome
}
