package ru.finny.pet.domain.town

import kotlinx.serialization.Serializable
import ru.finny.pet.domain.Need

/** Цель покупки: конкретный товар, любой товар нужды или товар с тегом. */
sealed interface BuyTarget {
    data class Item(val id: String) : BuyTarget
    data class ByNeed(val need: Need) : BuyTarget
    data class Tag(val tag: String) : BuyTarget
}

/** Действие ребёнка, с которым сверяются исходы события (§7.3). */
@Serializable(with = FactSerializer::class)
sealed interface Fact {
    data class Buy(val target: BuyTarget, val source: Source? = null) : Fact
    data class BuyAt(val item: String, val rank: PriceRank) : Fact
    data object Skip : Fact; data object Deposit : Fact; data object Withdraw : Fact
    data class Plan(val needCoversList: Boolean) : Fact
    data class Repair(val item: String, val source: Source? = null) : Fact
    data object CheckChange : Fact; data object MakeCard : Fact
    data class MakeGoal(val item: String) : Fact
    data class Attend(val place: String) : Fact
    data class WeekEndNo(val need: Need) : Fact
}

/** Условие прихода события (requires) или исхода (when) (§7.3). */
@Serializable(with = ConditionSerializer::class)
sealed interface Condition {
    data class WeekAtLeast(val n: Int) : Condition
    data class DayIs(val day: Int) : Condition
    data object BeforePlan : Condition; data object AfterPlan : Condition
    data class NotBought(val need: Need) : Condition
    data class Owns(val item: String) : Condition
    data class NotOwned(val item: String) : Condition
    data object HasGoal : Condition
    data class SavingsPctAtLeast(val pct: Int) : Condition
    data class ResidentArrived(val id: String) : Condition
    data class WantAtLeast(val n: Int) : Condition
    data class ReserveAtLeast(val n: Int) : Condition
    data class NotBroken(val item: String) : Condition
    data class AnyOf(val options: List<Condition>) : Condition
}

/** Последствие события в мире (§7.3). */
@Serializable(with = EventEffectSerializer::class)
sealed interface EventEffect {
    data class Coins(val n: Int, val source: CoinSource) : EventEffect
    data class ShortChange(val n: Int) : EventEffect
    data class StatChange(val stat: Stat, val n: Int) : EventEffect
    data class ItemGive(val item: String) : EventEffect; data class ItemBreak(val item: String) : EventEffect
    data class ItemFix(val item: String) : EventEffect
    data class Price(val item: String, val shop: String, val price: Int, val until: Until) : EventEffect
    data class Offer(val item: String, val shop: String, val was: Int, val now: Int, val until: Until) : EventEffect
    data class Note(val text: String) : EventEffect
    data class Sticker(val id: String) : EventEffect; data class Goto(val place: String) : EventEffect
    data class DemoGoal(val pct: Int) : EventEffect
    data object None : EventEffect
}

/** Путь восстановления после исхода MISTAKE (§7.3, ТЗ 2.5.9). */
@Serializable(with = RecoverySerializer::class)
sealed interface Recovery {
    data object ReserveToSavings : Recovery; data object ReserveToNeed : Recovery; data object WantToNeed : Recovery
    data class PlanTweak(val dir: TweakDir, val n: Int) : Recovery
    data object RetryNextWeek : Recovery; data object ReturnLater : Recovery
}

/** Что запускает приход события: вход в место или подтверждение плана (§7.3). */
@Serializable(with = TriggerSerializer::class)
sealed interface Trigger {
    data class Enter(val place: String) : Trigger
    data object PlanConfirmed : Trigger
}
