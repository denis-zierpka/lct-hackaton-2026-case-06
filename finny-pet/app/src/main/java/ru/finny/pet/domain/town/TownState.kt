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

/** Итог действия: состояние, одна строка ребёнку, «Почему?» до 3 строк, эффекты для анимаций, исходы событий, пришедшие события (§7.3, §8; TOWN-S1c §0). */
data class TownOutcome(val state: GameState, val line: String = "", val why: List<String> = emptyList(), val effects: List<EventEffect> = emptyList(), val eventResults: List<EventResult> = emptyList(), val arrived: List<String> = emptyList())

/** Итог одного сработавшего события внутри действия движка. */
data class EventResult(val eventId: String, val verdict: Verdict, val line: String, val sticker: String? = null, val recovery: List<Recovery> = emptyList())
/** Итог действия: выполнено или отказ кассы (§6.4). */
sealed interface TownResult {
    data class Done(val outcome: TownOutcome) : TownResult
    /** Отказ с объяснением; варианты кассы — срез 1. */
    data class Refused(val line: String) : TownResult
}

/** Карточка «В городке»: активное событие, заказ или «спокойно» (TOWN-S1c §6). */
data class Card(val title: String, val eventId: String? = null, val place: String? = null)
