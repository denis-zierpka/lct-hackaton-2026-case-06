package ru.finny.pet.domain.town

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.finny.pet.domain.Theme

/** Запись одного события «Городка» из закрытого словаря (§7.2, ТЗ 2.5.14). */
@Serializable
data class EventDef(
    val id: String, val kind: EventKind, val title: String, val theme: Theme? = null,
    val place: String? = null, val resident: String? = null, val arrives: Arrives = Arrives(),
    val triggers: List<Trigger> = emptyList(), val requires: List<Condition> = emptyList(),
    val setup: List<EventEffect> = emptyList(), val intro: String,
    val outcomes: List<EventOutcome> = emptyList(), val default: EventDefault? = null,
    val sticker: String? = null, val repeat: Repeat = Repeat(), val demo: Demo = Demo(),
    val priority: Int = 0, val params: EventParams = EventParams(), val followUp: FollowUp? = null,
)

/** Первый приход события: игровая неделя и день (§7.4). */
@Serializable
data class Arrives(val week: Int = 1, val day: Int? = null)

/** Проверяются сверху вниз; срабатывает первый исход с подходящим fact и выполненным when. */
@Serializable
data class EventOutcome(
    val fact: Fact, val verdict: Verdict, val line: String,
    @SerialName("when") val condition: Condition? = null,
    val recovery: List<Recovery> = emptyList(), val effects: List<EventEffect> = emptyList(),
)

/** Что засчитывается по окончании окна, если ребёнок ничего не сделал (§7.4). */
@Serializable
data class EventDefault(val at: Until, val fact: Fact)

/** afterWeeks/pool — возврат в пул не раньше afterWeeks недель; weeks — приходы по расписанию (MISHAP, WINDFALL). */
@Serializable
data class Repeat(val afterWeeks: Int = 0, val pool: Boolean = false, val weeks: List<Int> = emptyList())
/** Демо-режим: запуск сразу, вещь остаётся на полке, предусловие без монет (§7.4). */
@Serializable
data class Demo(val startNow: Boolean = true, val keepOnShelf: Boolean = false, val setup: List<EventEffect> = emptyList(), val wrongPath: String = "")
/** list — товары списка П1; job — работа заказа JOB. */
@Serializable
data class EventParams(val list: List<String> = emptyList(), val job: String? = null)
/** Сцена-продолжение события: день и место (П2). */
@Serializable
data class FollowUp(val day: Int, val place: String)
