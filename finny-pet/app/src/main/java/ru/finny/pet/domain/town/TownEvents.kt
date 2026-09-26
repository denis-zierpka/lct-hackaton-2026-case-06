package ru.finny.pet.domain.town

import ru.finny.pet.domain.Content
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.Goal
import ru.finny.pet.domain.ShopItem

/** Что сделал ребёнок — наблюдение, с которым сверяются исходы событий (TOWN-S1c §3). */
internal sealed interface Seen {
    data class Bought(val item: ShopItem, val source: Source, val shopId: String?, val price: Int) : Seen
    data object Planned : Seen
    /** Skip, Deposit, Withdraw, MakeGoal, WeekEndNo — сверяются равенством. */
    data class Plain(val fact: Fact) : Seen
}

/** События «Городка»: приход, сопоставление фактов, окна и демо-запуск (TOWN-S1c §1–§6). Фасад — Town. */
internal class TownEvents(private val content: Content, private val prices: Prices) {
    private val town = content.town ?: error("content.json: нет ключа town")

    /** Живые события: town.events минус eventsOff (§0). */
    val live: List<EventDef> = town.events.filter { it.id !in town.eventsOff }

    /** Порядок одного прохода прихода: неделя, день, порядок в content (§2). */
    private val schedule = live.withIndex()
        .sortedWith(compareBy({ it.value.arrives.week }, { it.value.arrives.day ?: 0 }, { it.index }))
        .map { it.value }

    fun liveEvent(id: String): EventDef? = live.firstOrNull { it.id == id && it.kind != EventKind.JOB }

    fun isActive(s: GameState, id: String): Boolean = s.events.any { it.id == id && it.status == EventStatus.ACTIVE }

    fun active(s: GameState): List<EventDef> =
        s.events.filter { it.status == EventStatus.ACTIVE }.mapNotNull { liveEvent(it.id) }

    /** Место события откроет мечта, которой ещё нет (§1). */
    fun closed(s: GameState, e: EventDef): Boolean {
        val goal = town.places.firstOrNull { it.id == e.place }?.opensBy?.goal ?: return false
        return s.achievedGoals.none { it.id == goal }
    }

    /** Условие на состоянии s; relaxed — демо-ослабления requires (§1). */
    fun holds(s: GameState, c: Condition, relaxed: Boolean): Boolean {
        val d = relaxed && s.demo
        return when (c) {
            is Condition.WeekAtLeast -> d || s.period >= c.n
            is Condition.DayIs -> d || s.day == c.day
            Condition.BeforePlan -> !s.plan.confirmed
            Condition.AfterPlan -> s.plan.confirmed
            is Condition.NotBought -> d || s.purchases.none { it.need == c.need }
            is Condition.Owns -> c.item in s.owned
            is Condition.NotOwned -> c.item !in s.owned || (d && c.item in prices.keepDemo)
            Condition.HasGoal -> s.goal != null
            is Condition.SavingsPctAtLeast -> s.goal.let { it != null && s.savings * 100 >= c.pct * it.price }
            is Condition.ResidentArrived ->
                d || town.residents.firstOrNull { it.id == c.id }?.arrivesWeek.let { it != null && s.period >= it }
            is Condition.WantAtLeast -> s.jarWant >= c.n
            is Condition.ReserveAtLeast -> s.reserve >= c.n
            is Condition.NotBroken -> c.item !in s.broken
            is Condition.AnyOf -> c.options.any { holds(s, it, relaxed) }
        }
    }

    fun unmet(s: GameState, e: EventDef): Condition? = e.requires.firstOrNull { !holds(s, it, true) }

    /** Строка отказа startEvent по первому невыполненному условию (§6). */
    fun notReady(s: GameState, c: Condition): String = when (c) {
        Condition.BeforePlan -> "План уже готов — событие придёт с новым конвертом"
        Condition.AfterPlan -> "$NOT_NOW: после раскладки монет"
        Condition.HasGoal -> "$NOT_NOW: нужна мечта"
        is Condition.SavingsPctAtLeast ->
            s.goal?.let { "$NOT_NOW: в копилке нужно ${(c.pct * it.price + 99) / 100}" } ?: "$NOT_NOW: нужна мечта"
        is Condition.WantAtLeast -> "$NOT_NOW: в «Хочу» нужно ${c.n}"
        is Condition.ReserveAtLeast -> "$NOT_NOW: в запасе нужно ${c.n}"
        is Condition.Owns -> "$NOT_NOW: сначала нужна вещь «${title(c.item)}»"
        is Condition.NotOwned -> "$NOT_NOW: ${lower(title(c.item))} уже есть дома"
        is Condition.NotBroken -> "$NOT_NOW: ${lower(title(c.item))} уже не работает"
        is Condition.AnyOf -> c.options.firstOrNull()?.let { notReady(s, it) } ?: NOT_NOW
        else -> NOT_NOW
    }

    private fun title(id: String): String =
        town.homeItems.firstOrNull { it.id == id }?.title ?: content.itemOrNull(id)?.title ?: id

    // ---------- §2. Приход ----------

    /** Плановая проверка и триггер trigger одним проходом (§2). */
    fun arrive(o: TownOutcome, trigger: Trigger?): TownOutcome {
        var r = o
        for (e in schedule) {
            val s = r.state
            if (s.asleep) return r
            if (e.kind == EventKind.JOB) continue
            val last = s.events.lastOrNull { it.id == e.id }
            if (last?.status == EventStatus.ACTIVE) continue
            if (last != null && !retry(e, last, s)) continue
            val a = e.arrives
            if (s.period < a.week || (s.period == a.week && a.day != null && s.day < a.day)) continue
            if (unmet(s, e) != null || closed(s, e)) continue
            if (e.triggers.isNotEmpty() && trigger !in e.triggers) continue
            if (e.triggers.isEmpty() && dayCount(s) >= town.rules.eventsPerDay) continue
            r = start(r, e)
        }
        return r
    }

    /** Возврат после исхода с RETRY_NEXT_WEEK на следующей неделе (§2 b). */
    private fun retry(e: EventDef, last: EventState, s: GameState): Boolean {
        val i = last.outcome ?: return false
        return i >= 0 && Recovery.RetryNextWeek in e.outcomes[i].recovery && last.period < s.period
    }

    /** Сколько событий без триггера и не JOB пришло сегодня (§2 f). */
    private fun dayCount(s: GameState): Int = s.events.count { es ->
        es.period == s.period && es.day == s.day &&
            town.events.firstOrNull { it.id == es.id }?.let { it.triggers.isEmpty() && it.kind != EventKind.JOB } == true
    }

    /** ПРИХОД: EventState, setup, arrived; событие без исходов сразу закрывается с наклейкой (§2). */
    fun start(o: TownOutcome, e: EventDef): TownOutcome {
        val s = o.state
        val done = e.outcomes.isEmpty()
        val es = EventState(e.id, if (done) EventStatus.DONE else EventStatus.ACTIVE, null, if (done) -1 else null, s.period, s.day)
        var st = apply(s.copy(events = s.events + es), e.setup, e.id, setup = true)
        if (done) st = st.copy(stickers = st.stickers.plusNew(e.sticker))
        return o.copy(state = st, arrived = o.arrived + e.id, effects = o.effects + e.setup)
    }

    /** demo.setup при невыполненных requires (§6). */
    fun demoSetup(s: GameState, e: EventDef): GameState = apply(s, e.demo.setup, e.id, setup = true, demo = true)

    // ---------- §4. Эффекты ----------

    private fun apply(s: GameState, fx: List<EventEffect>, id: String, setup: Boolean, demo: Boolean = false): GameState =
        fx.fold(s) { st, f ->
            when (f) {
                is EventEffect.Note -> st.copy(notes = st.notes.plusNew(f.text))
                is EventEffect.Sticker -> st.copy(stickers = st.stickers.plusNew(f.id))
                is EventEffect.ItemGive -> town.putHome(st, content.itemOrNull(f.item) ?: error("Событие $id: нет вещи ${f.item}"))
                is EventEffect.ItemBreak -> st.copy(broken = st.broken.plusNew(f.item))
                is EventEffect.ItemFix -> st.copy(broken = st.broken - f.item)
                is EventEffect.Price, is EventEffect.Offer -> if (setup) st else error("Событие $id: PRICE и OFFER — только в setup")
                is EventEffect.Coins, is EventEffect.ShortChange, is EventEffect.StatChange ->
                    error("Событие $id: эффект $f — срез 2")
                is EventEffect.DemoGoal -> if (!demo) st else st.copy(
                    goal = Goal("demo_goal", "Мечта для показа", "⭐", maxOf(town.rules.customGoalFromItemMin, st.savings * 100 / f.pct)),
                )
                is EventEffect.Goto, EventEffect.None -> st
            }
        }

    // ---------- §3. Сопоставление ----------

    /** Один факт — одно событие: наибольший priority, при равенстве — пришедшее раньше (§3). */
    fun observe(o: TownOutcome, seen: Seen, only: String? = null, rewrite: Boolean = true): TownOutcome {
        val s = o.state
        var best: Pair<EventDef, Int>? = null
        for (es in s.events) {
            if (es.status != EventStatus.ACTIVE || (only != null && es.id != only)) continue
            val e = liveEvent(es.id) ?: continue
            val i = e.outcomes.indexOfFirst { fits(s, e, it, seen) }
            if (i >= 0 && (best == null || e.priority > best.first.priority)) best = e to i
        }
        val (e, i) = best ?: return o
        return resolve(o, e, i, rewrite)
    }

    private fun fits(s: GameState, e: EventDef, o: EventOutcome, seen: Seen): Boolean {
        val f = o.fact
        val ok = when {
            f is Fact.Buy && seen is Seen.Bought -> (f.source == null || f.source == seen.source) &&
                when (val t = f.target) {
                    is BuyTarget.Item -> t.id == seen.item.id
                    is BuyTarget.ByNeed -> seen.item.need == t.need
                    is BuyTarget.Tag -> t.tag in seen.item.tags
                }
            f is Fact.BuyAt && seen is Seen.Bought -> seen.shopId != null && seen.item.id == f.item &&
                f.rank == if (seen.price <= (prices.cheapest(s, f.item) ?: seen.price)) PriceRank.CHEAPEST else PriceRank.DEARER
            f is Fact.Plan && seen is Seen.Planned -> f.needCoversList == (s.plan.mandatory >= listSum(s, e))
            seen is Seen.Plain -> f == seen.fact
            else -> false
        }
        return ok && (o.condition == null || holds(s, o.condition, false))
    }

    /** LIST(E): сумма самых низких цен товаров списка (§3). */
    private fun listSum(s: GameState, e: EventDef): Int =
        e.params.list.sumOf { id -> prices.cheapest(s, id) ?: content.itemOrNull(id)?.price ?: 0 }

    /** РАЗРЕШЕНИЕ исходом i и строка действия (§3). */
    private fun resolve(o: TownOutcome, e: EventDef, i: Int, rewrite: Boolean): TownOutcome {
        val out = e.outcomes[i]
        val s = o.state
        val idx = s.events.indexOfLast { it.id == e.id }
        val events = s.events.toMutableList().also { it[idx] = it[idx].copy(status = EventStatus.DONE, verdict = out.verdict, outcome = i) }
        var st = apply(s.copy(events = events), out.effects, e.id, setup = false)
        st = st.copy(stickers = st.stickers.plusNew(e.sticker))
        val r = o.copy(
            state = st, effects = o.effects + out.effects,
            eventResults = o.eventResults + EventResult(e.id, out.verdict, out.line, e.sticker, out.recovery),
        )
        return when {
            !rewrite -> r
            o.line.startsWith(RESERVE_INVITE) -> r.copy(why = (listOf(out.line) + o.why).take(3))
            else -> r.copy(line = out.line, why = (listOfNotNull(o.line.ifEmpty { null }) + o.why).take(3))
        }
    }

    // ---------- §5. Окна ----------

    /** Умолчания окон at у ACTIVE событий; без подходящего исхода — тихое закрытие (§5). */
    fun defaults(o: TownOutcome, at: Set<Until>): TownOutcome {
        var r = o
        for (es in o.state.events) {
            if (es.status != EventStatus.ACTIVE) continue
            val e = liveEvent(es.id) ?: continue
            val d = e.default?.takeIf { it.at in at } ?: continue
            if (d.fact != Fact.Skip && d.fact !is Fact.WeekEndNo) error("Событие ${e.id}: умолчание — только SKIP или WEEK_END_NO")
            val i = e.outcomes.indexOfFirst { it.fact == d.fact && (it.condition == null || holds(r.state, it.condition, false)) }
            r = if (i >= 0) resolve(r, e, i, rewrite = false) else close(r, e.id)
        }
        return r
    }

    /** Все ещё ACTIVE живые события закрываются без итога (§5 п. 3). */
    fun closeAll(o: TownOutcome): TownOutcome =
        o.state.events.filter { it.status == EventStatus.ACTIVE && liveEvent(it.id) != null }.fold(o) { r, es -> close(r, es.id) }

    private fun close(o: TownOutcome, id: String): TownOutcome {
        val events = o.state.events.map {
            if (it.id == id && it.status == EventStatus.ACTIVE) it.copy(status = EventStatus.DONE, verdict = null, outcome = -1) else it
        }
        return o.copy(state = o.state.copy(events = events))
    }

    private companion object {
        const val NOT_NOW = "Сейчас не начать"
        const val RESERVE_INVITE = "Хорошо, что был запас!"
    }
}

internal fun List<String>.plusNew(x: String?): List<String> = if (x == null || x in this) this else this + x
