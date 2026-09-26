package ru.finny.pet.domain.town

import kotlinx.serialization.Serializable
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.PeriodSummary

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

/** Действия ребёнка в городке, которые пишут факты событий (§7.3); реализация — срез 1. */
interface Town {
    /** shopId = null: оплата в сцене по цене товара; source = RESERVE при нехватке запаса добирает из «Хочу» (составная оплата С4) (§6.4, §7.3). */
    fun buyAt(s: GameState, itemId: String, shopId: String?, source: Source): TownResult
    /** Починка стартовой вещи дома у мастера из банки source; факт REPAIR (§5.3, С2). */
    fun repair(s: GameState, homeItemId: String, source: Source): TownResult
    /** Запас в копилку с предпросмотром срока мечты; факт DEPOSIT (§5.5). */
    fun reserveToSavings(s: GameState, amount: Int): TownResult
    /** Перенос между банками NEED, WANT и RESERVE у кассы (§6.4). */
    fun transfer(s: GameState, from: Source, to: Source, amount: Int): TownResult
    /** Подтверждение плана и «сначала заплати себе»: взнос в копилку; факт PLAN (§6.3, П1). */
    fun confirmPlan(s: GameState): TownResult
    /** Оплата смены в конверт следующей недели; закрывает JOB-событие с params.job == jobId без EventResult (§5.2). */
    fun finishShift(s: GameState, jobId: String, score: Int, bombsUsed: Int): TownResult
    /** Сон: без wake день не двигается, без плана открывает раскладку, третий сон — конец недели и факт WEEK_END_NO (§3.2, §3.3). */
    fun sleep(s: GameState): TownResult
    /** Кнопка «Проснуться»: следующий день или новый конверт (§3.2, §9.2 №19). */
    fun wake(s: GameState): TownResult
    /** Поправки «Что поменяем?» по итогу недели (§6.5). */
    fun planTweaks(s: GameState, summary: PeriodSummary): List<Recovery>
    /** Бонус взрослого — в конверт следующей недели, причина из town.parentBonusReasons (§5.2, решение 21). */
    fun parentBonus(s: GameState, reasonIndex: Int): TownResult
    /** Загадка Бори из town.quiz: верный ответ — бомба, неверный — объяснение без штрафа (§5.2, решение 10). */
    fun answerQuestion(s: GameState, questionId: String, optionIndex: Int): TownResult
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

/** Цена товара в лавке с учётом эффектов PRICE и OFFER (§5.4); реализация — срез 1. */
interface Prices {
    /** null — товар в этой лавке не продаётся. */
    fun price(s: GameState, itemId: String, shopId: String): Int?
}
/** Перенос сохранения 1.3.0 на схему «Городка» (§16.3); реализация — срез 1. */
interface Migration {
    /** Один раз при stateVersion < 1: банки из плана, owned, stateVersion = 1; монеты не теряются и не появляются. */
    fun migrate(s: GameState): GameState
}
