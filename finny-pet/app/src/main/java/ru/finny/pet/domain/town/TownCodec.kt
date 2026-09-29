package ru.finny.pet.domain.town

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ru.finny.pet.domain.Need

private val ID_RE = Regex("^[a-z][a-z0-9_]*$")
private val UINT_RE = Regex("^[0-9]+$")
private val SIGNED_RE = Regex("^[+-]?[0-9]+$")
private val ANY_OF_SPLIT_RE = Regex("\\s*\\|\\|\\s*")

/** Разбор и каноническая печать строкового словаря событий «Городка» (GAME_CONCEPT §7.3; TOWN-S0a §2–§3). */
object TownCodec {
    fun fact(s: String): Fact = parse("факт", s) { parseFactBody(it) }
    fun condition(s: String): Condition = parse("условие", s) { parseConditionBody(it) }
    fun effect(s: String): EventEffect = parse("эффект", s) { parseEffectBody(it) }
    fun recovery(s: String): Recovery = parse("восстановление", s) { parseRecoveryBody(it) }
    fun trigger(s: String): Trigger = parse("триггер", s) { parseTriggerBody(it) }

    fun print(fact: Fact): String = when (fact) {
        is Fact.Buy -> "BUY:" + printTarget(fact.target) + printSource(fact.source)
        is Fact.BuyAt -> "BUY_AT:${fact.item}:${fact.rank}"
        Fact.Skip -> "SKIP"
        Fact.Deposit -> "DEPOSIT"
        Fact.Withdraw -> "WITHDRAW"
        is Fact.Plan -> if (fact.needCoversList) "PLAN:NEED>=LIST" else "PLAN:NEED<LIST"
        is Fact.Repair -> "REPAIR:${fact.item}" + printSource(fact.source)
        Fact.CheckChange -> "CHECK_CHANGE"
        Fact.MakeCard -> "MAKE_CARD"
        is Fact.MakeGoal -> "MAKE_GOAL:${fact.item}"
        is Fact.Attend -> "ATTEND:${fact.place}"
        is Fact.WeekEndNo -> "WEEK_END_NO:${fact.need}"
    }
    fun print(condition: Condition): String = when (condition) {
        is Condition.WeekAtLeast -> "week>=${condition.n}"
        is Condition.DayIs -> "day==${condition.day}"
        Condition.BeforePlan -> "beforePlan"
        Condition.AfterPlan -> "afterPlan"
        is Condition.NotBought -> "notBought:${condition.need}"
        is Condition.Owns -> "owns:${condition.item}"
        is Condition.NotOwned -> "notOwned:${condition.item}"
        Condition.HasGoal -> "hasGoal"
        is Condition.SavingsPctAtLeast -> "savingsPct>=${condition.pct}"
        is Condition.ResidentArrived -> "resident:${condition.id}"
        is Condition.WantAtLeast -> "want>=${condition.n}"
        is Condition.ReserveAtLeast -> "reserve>=${condition.n}"
        is Condition.NotBroken -> "notBroken:${condition.item}"
        is Condition.AnyOf -> condition.options.joinToString(" || ") { print(it) }
    }
    fun print(effect: EventEffect): String = when (effect) {
        is EventEffect.Coins -> "COINS(${effect.n}, ${effect.source})"
        is EventEffect.ShortChange -> "SHORT_CHANGE(${effect.n})"
        is EventEffect.StatChange -> "STAT(${effect.stat}, ${effect.n})"
        is EventEffect.ItemGive -> "ITEM_GIVE(${effect.item})"
        is EventEffect.ItemBreak -> "ITEM_BREAK(${effect.item})"
        is EventEffect.ItemFix -> "ITEM_FIX(${effect.item})"
        is EventEffect.Price -> "PRICE(${effect.item}, ${effect.shop}, ${effect.price}, ${effect.until})"
        is EventEffect.Offer -> "OFFER(${effect.item}, ${effect.shop}, ${effect.was}, ${effect.now}, ${effect.until})"
        is EventEffect.Note -> "NOTE(${effect.text})"
        is EventEffect.Sticker -> "STICKER(${effect.id})"
        is EventEffect.Goto -> "GOTO(${effect.place})"
        is EventEffect.DemoGoal -> "DEMO_GOAL(${effect.pct})"
        EventEffect.None -> "NONE"
    }
    fun print(recovery: Recovery): String = when (recovery) {
        Recovery.ReserveToSavings -> "RESERVE_TO_SAVINGS"
        Recovery.ReserveToNeed -> "RESERVE_TO_NEED"
        Recovery.WantToNeed -> "WANT_TO_NEED"
        is Recovery.PlanTweak -> "PLAN_TWEAK(${recovery.dir}, ${recovery.n})"
        Recovery.RetryNextWeek -> "RETRY_NEXT_WEEK"
        Recovery.ReturnLater -> "RETURN_LATER"
    }
    fun print(trigger: Trigger): String = when (trigger) {
        is Trigger.Enter -> "ENTER:${trigger.place}"
        Trigger.PlanConfirmed -> "PLAN_CONFIRMED"
    }
    private fun printTarget(t: BuyTarget): String = when (t) {
        is BuyTarget.Item -> t.id
        is BuyTarget.ByNeed -> t.need.name
        is BuyTarget.Tag -> "#${t.tag}"
    }
    private fun printSource(s: Source?): String = if (s == null) "" else ":$s"
    private fun <T> parse(kind: String, raw: String, block: (String) -> T?): T {
        val t = raw.trim()
        val value = if (t.isEmpty()) null else block(t)
        return value ?: throw IllegalArgumentException("не удалось разобрать $kind «$t»")
    }
    private fun parseId(s: String): String? = if (ID_RE.matches(s)) s else null
    private fun parseUInt(s: String): Int? = if (UINT_RE.matches(s)) s.toIntOrNull() else null
    private fun parseSignedInt(s: String): Int? = if (SIGNED_RE.matches(s)) s.toIntOrNull() else null
    private fun parseNeed2(s: String): Need? = if (s == "FOOD" || s == "CARE") Need.valueOf(s) else null
    private inline fun <reified E : Enum<E>> parseEnumValue(s: String): E? =
        enumValues<E>().firstOrNull { it.name == s }
    private fun parseFactBody(t: String): Fact? = when {
        t == "SKIP" -> Fact.Skip
        t == "DEPOSIT" -> Fact.Deposit
        t == "WITHDRAW" -> Fact.Withdraw
        t == "CHECK_CHANGE" -> Fact.CheckChange
        t == "MAKE_CARD" -> Fact.MakeCard
        t.startsWith("BUY_AT:") -> parseBuyAt(t.removePrefix("BUY_AT:"))
        t.startsWith("BUY:") -> parseBuy(t.removePrefix("BUY:"))
        t == "PLAN:NEED>=LIST" -> Fact.Plan(true)
        t == "PLAN:NEED<LIST" -> Fact.Plan(false)
        t.startsWith("REPAIR:") -> parseRepair(t.removePrefix("REPAIR:"))
        t.startsWith("MAKE_GOAL:") -> parseId(t.removePrefix("MAKE_GOAL:"))?.let { Fact.MakeGoal(it) }
        t.startsWith("ATTEND:") -> parseId(t.removePrefix("ATTEND:"))?.let { Fact.Attend(it) }
        t.startsWith("WEEK_END_NO:") -> parseNeed2(t.removePrefix("WEEK_END_NO:"))?.let { Fact.WeekEndNo(it) }
        else -> null
    }
    private fun parseBuy(suffix: String): Fact.Buy? {
        if (suffix.isEmpty()) return null
        val parts = suffix.split(":"); if (parts.size !in 1..2) return null
        val first = parts[0]
        val target = when {
            first.startsWith("#") -> parseId(first.substring(1))?.let { BuyTarget.Tag(it) }
            first in Need.entries.map { it.name } -> BuyTarget.ByNeed(Need.valueOf(first))
            else -> parseId(first)?.let { BuyTarget.Item(it) }
        } ?: return null
        if (parts.size == 1) return Fact.Buy(target)
        return Fact.Buy(target, parseSourcePart(parts[1]) ?: return null)
    }
    private fun parseSourcePart(part: String): Source? = if (part.isEmpty()) null else parseEnumValue<Source>(part)
    private fun parseBuyAt(suffix: String): Fact.BuyAt? {
        val parts = suffix.split(":"); if (parts.size != 2) return null
        val id = parseId(parts[0]) ?: return null
        return Fact.BuyAt(id, parseEnumValue<PriceRank>(parts[1]) ?: return null)
    }
    private fun parseRepair(suffix: String): Fact.Repair? {
        if (suffix.isEmpty()) return null
        val parts = suffix.split(":"); if (parts.size !in 1..2) return null
        val id = parseId(parts[0]) ?: return null
        if (parts.size == 1) return Fact.Repair(id)
        return Fact.Repair(id, parseSourcePart(parts[1]) ?: return null)
    }
    private fun parseConditionBody(t: String): Condition? {
        if (t.contains("||")) {
            val parts = t.split(ANY_OF_SPLIT_RE)
            if (parts.size < 2 || parts.any { it.isEmpty() }) return null
            val options = parts.map { parseAtomicCondition(it) ?: return null }
            return Condition.AnyOf(options)
        }
        return parseAtomicCondition(t)
    }
    private fun parseAtomicCondition(t: String): Condition? = when {
        t == "beforePlan" -> Condition.BeforePlan
        t == "afterPlan" -> Condition.AfterPlan
        t == "hasGoal" -> Condition.HasGoal
        t.startsWith("week>=") -> parseUInt(t.removePrefix("week>="))?.let { Condition.WeekAtLeast(it) }
        t.startsWith("day==") -> parseUInt(t.removePrefix("day=="))?.let { Condition.DayIs(it) }
        t.startsWith("savingsPct>=") -> parseUInt(t.removePrefix("savingsPct>="))?.let { Condition.SavingsPctAtLeast(it) }
        t.startsWith("want>=") -> parseUInt(t.removePrefix("want>="))?.let { Condition.WantAtLeast(it) }
        t.startsWith("reserve>=") -> parseUInt(t.removePrefix("reserve>="))?.let { Condition.ReserveAtLeast(it) }
        t.startsWith("notBought:") -> parseNeed2(t.removePrefix("notBought:"))?.let { Condition.NotBought(it) }
        t.startsWith("owns:") -> parseId(t.removePrefix("owns:"))?.let { Condition.Owns(it) }
        t.startsWith("notOwned:") -> parseId(t.removePrefix("notOwned:"))?.let { Condition.NotOwned(it) }
        t.startsWith("notBroken:") -> parseId(t.removePrefix("notBroken:"))?.let { Condition.NotBroken(it) }
        t.startsWith("resident:") -> parseId(t.removePrefix("resident:"))?.let { Condition.ResidentArrived(it) }
        else -> null
    }
    private fun parseEffectBody(t: String): EventEffect? {
        if (t == "NONE") return EventEffect.None
        val open = t.indexOf('(')
        if (open == -1 || !t.endsWith(")")) return null
        val name = t.substring(0, open)
        val inner = t.substring(open + 1, t.length - 1)
        return when (name) {
            "COINS" -> parseCoins(inner)
            "SHORT_CHANGE" -> parseUInt(inner.trim())?.let { EventEffect.ShortChange(it) }
            "STAT" -> parseStat(inner)
            "ITEM_GIVE" -> parseId(inner.trim())?.let { EventEffect.ItemGive(it) }
            "ITEM_BREAK" -> parseId(inner.trim())?.let { EventEffect.ItemBreak(it) }
            "ITEM_FIX" -> parseId(inner.trim())?.let { EventEffect.ItemFix(it) }
            "PRICE" -> parsePrice(inner)
            "OFFER" -> parseOffer(inner)
            "NOTE" -> inner.trim().takeIf { it.isNotEmpty() }?.let { EventEffect.Note(it) }
            "STICKER" -> parseId(inner.trim())?.let { EventEffect.Sticker(it) }
            "GOTO" -> parseId(inner.trim())?.let { EventEffect.Goto(it) }
            "DEMO_GOAL" -> parseUInt(inner.trim())?.let { EventEffect.DemoGoal(it) }
            else -> null
        }
    }
    private fun args(inner: String): List<String> = inner.split(",").map { it.trim() }
    private fun parseCoins(inner: String): EventEffect.Coins? {
        val a = args(inner); if (a.size != 2) return null
        val n = parseUInt(a[0]) ?: return null
        return EventEffect.Coins(n, parseEnumValue<CoinSource>(a[1]) ?: return null)
    }
    private fun parseStat(inner: String): EventEffect.StatChange? {
        val a = args(inner); if (a.size != 2) return null
        val stat = parseEnumValue<Stat>(a[0]) ?: return null
        return EventEffect.StatChange(stat, parseSignedInt(a[1]) ?: return null)
    }
    private fun parsePrice(inner: String): EventEffect.Price? {
        val a = args(inner); if (a.size != 4) return null
        val item = parseId(a[0]) ?: return null; val shop = parseId(a[1]) ?: return null
        val price = parseUInt(a[2]) ?: return null
        return EventEffect.Price(item, shop, price, parseEnumValue<Until>(a[3]) ?: return null)
    }
    private fun parseOffer(inner: String): EventEffect.Offer? {
        val a = args(inner); if (a.size != 5) return null
        val item = parseId(a[0]) ?: return null; val shop = parseId(a[1]) ?: return null
        val was = parseUInt(a[2]) ?: return null; val now = parseUInt(a[3]) ?: return null
        return EventEffect.Offer(item, shop, was, now, parseEnumValue<Until>(a[4]) ?: return null)
    }
    private fun parseRecoveryBody(t: String): Recovery? = when {
        t == "RESERVE_TO_SAVINGS" -> Recovery.ReserveToSavings
        t == "RESERVE_TO_NEED" -> Recovery.ReserveToNeed
        t == "WANT_TO_NEED" -> Recovery.WantToNeed
        t == "RETRY_NEXT_WEEK" -> Recovery.RetryNextWeek
        t == "RETURN_LATER" -> Recovery.ReturnLater
        t.startsWith("PLAN_TWEAK(") && t.endsWith(")") -> parsePlanTweak(t.substring(11, t.length - 1))
        else -> null
    }
    private fun parsePlanTweak(inner: String): Recovery.PlanTweak? {
        val a = args(inner); if (a.size != 2) return null
        val dir = parseEnumValue<TweakDir>(a[0]) ?: return null
        return Recovery.PlanTweak(dir, parseUInt(a[1]) ?: return null)
    }
    private fun parseTriggerBody(t: String): Trigger? = when {
        t == "PLAN_CONFIRMED" -> Trigger.PlanConfirmed
        t.startsWith("ENTER:") -> parseId(t.removePrefix("ENTER:"))?.let { Trigger.Enter(it) }
        else -> null
    }
}

/** Строковый JSON-формат словарного значения — печать/разбор через TownCodec. */
private class StringForm<T : Any>(name: String, val decode: (String) -> T, val show: (T) -> String) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(name, PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(show(value))
    override fun deserialize(decoder: Decoder): T = decode(decoder.decodeString())
}

object FactSerializer : KSerializer<Fact> by StringForm("Fact", TownCodec::fact, TownCodec::print)
object ConditionSerializer : KSerializer<Condition> by StringForm("Condition", TownCodec::condition, TownCodec::print)
object EventEffectSerializer : KSerializer<EventEffect> by StringForm("EventEffect", TownCodec::effect, TownCodec::print)
object RecoverySerializer : KSerializer<Recovery> by StringForm("Recovery", TownCodec::recovery, TownCodec::print)
object TriggerSerializer : KSerializer<Trigger> by StringForm("Trigger", TownCodec::trigger, TownCodec::print)
