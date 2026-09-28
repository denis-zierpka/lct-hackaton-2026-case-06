package ru.finny.pet.domain.town

import ru.finny.pet.domain.BudgetPlan
import ru.finny.pet.domain.Category
import ru.finny.pet.domain.Content
import ru.finny.pet.domain.Economy
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.Goal
import ru.finny.pet.domain.LedgerEntry
import ru.finny.pet.domain.Need
import ru.finny.pet.domain.Outcome
import ru.finny.pet.domain.PeriodSummary
import ru.finny.pet.domain.QuizQuestion
import ru.finny.pet.domain.ShopItem
import ru.finny.pet.domain.TaskResult

enum class PayKind { PAY, CHEAPER, WAIT, MAKE_GOAL }

/** Вариант кассы; itemId — только у CHEAPER (другой товар) и MAKE_GOAL (этот товар) (TOWN-S1a §3). */
data class PayOption(
    val kind: PayKind, val label: String, val source: Source? = null,
    val itemId: String? = null, val preview: List<String> = emptyList(), val more: Boolean = false,
)

/** Предпросмотр кассы: цена, строка нехватки, заметка и варианты (§3). */
data class Quote(
    val itemId: String, val shopId: String?, val price: Int, val line: String,
    val note: String = "", val options: List<PayOption> = emptyList(),
)

/** Открыта ли работа и сколько платит до самой смены (TOWN-S1b §1). */
data class ShiftQuote(
    val jobId: String, val open: Boolean, val paid: Boolean, val canPlay: Boolean,
    val level: Int, val base: Int, val levelBombs: Int, val shiftsLeft: Int, val line: String,
    val top: Int,
)

/** Числа итога смены: «источник и сумма» (ТЗ 2.5.4, TOWN-J1-0 § 4). */
data class ShiftPay(val base: Int, val bonus: Int, val total: Int)

/** Кошелёк, касса, неделя и дом «Городка» (TOWN-S1a §3–§6). Чистый Kotlin. */
class Town(internal val content: Content) {
    private val town = content.town ?: error("content.json: нет ключа town")
    private val economy = Economy(content)
    private val prices = Prices(content)
    private val events = TownEvents(content, prices)

    // ---------- §3. Касса ----------

    fun quote(s: GameState, itemId: String, shopId: String?): Quote {
        val item = content.itemOrNull(itemId)
        checkoutRefusal(s, itemId, shopId)?.let { return Quote(itemId, shopId, item?.let { i -> priceOf(s, i, shopId) } ?: 0, it) }
        item!!
        val p = priceOf(s, item, shopId)
        val (b, jn, jw, r) = listOf(s.balance, s.jarNeed, s.jarWant, s.reserve)
        val t = lower(item.title)
        val wallet = "Не хватает ${p - b}: в кошельке $b, $t $p"
        val wait = PayOption(PayKind.WAIT, "Подождать нового конверта")

        fun opt(src: Source, label: String, more: Boolean = false, preview: (GameState) -> List<String>): PayOption? {
            val jars = jarsAfter(s, item.category, src, p) ?: return null
            val after = pay(s, item, shopId, p, src, jars)
            val stamp = if (planKept(s) && !planKept(after)) listOf(STAMP) else emptyList()
            return PayOption(PayKind.PAY, label, src, preview = preview(after) + stamp, more = more)
        }
        fun savings(more: Boolean) = opt(Source.SAVINGS, "Из копилки $p", more) { save(s, p) }
        fun cheaper(): PayOption? {
            if (shopId == null) return null
            val best = prices.shelf(s, shopId).filter {
                it.item.category == item.category && it.item.need == item.need && it.item.id != item.id &&
                    it.price < p && hasMainPay(s, it.item.category, it.price)
            }.maxByOrNull { it.price } ?: return null
            return PayOption(PayKind.CHEAPER, "Дешевле: ${lower(best.item.title)} ${best.price}", itemId = best.item.id)
        }
        fun goal(): PayOption? {
            val base = prices.basePrice(item.id) ?: return null
            if (!item.keep || base < town.rules.customGoalFromItemMin || s.goal?.id == "item:${item.id}") return null
            return PayOption(PayKind.MAKE_GOAL, "Сделать мечтой", itemId = item.id, preview = listOf("Мечта: $t — $base"))
        }

        return when (item.category) {
            Category.MANDATORY -> {
                if (jn >= p) {
                    return Quote(itemId, shopId, p, "", options = listOfNotNull(opt(Source.NEED, "Купить за $p") { listOf("Из «Нужного»: $jn → ${jn - p}") }))
                }
                val miss = p - jn
                val y = maxOf(0, miss - r)
                val reserveLabel = when {
                    y > 0 -> "Запас ${miss - y} + «Хочу» $y"
                    jn > 0 -> "Добавить $miss из запаса"
                    else -> "Из запаса $p"
                }
                val main = listOfNotNull(
                    cheaper(),
                    opt(Source.RESERVE, reserveLabel) { a ->
                        listOfNotNull(
                            "${item.title} будет!",
                            "Штамп «Нужное куплено» — да".takeIf { a.purchases.any { it.need == Need.FOOD } && a.purchases.any { it.need == Need.CARE } },
                        )
                    },
                    opt(Source.TRANSFER_WANT, "Взять $miss из «Хочу»") { listOf("В «Хочу» будет ${jw - miss}") },
                )
                val line = if (b < p) wallet else "В «Нужном» $jn, $t стоит $p. Не хватает $miss"
                Quote(itemId, shopId, p, line, options = main.ifEmpty { listOf(wait) } + listOfNotNull(savings(true)))
            }
            Category.OPTIONAL -> {
                if (jw >= p) {
                    return Quote(itemId, shopId, p, "", options = listOfNotNull(opt(Source.WANT, "Купить за $p") { listOf("Из «Хочу»: $jw → ${jw - p}") }))
                }
                val miss = p - jw
                val main = if (b < p) listOfNotNull(goal(), wait, cheaper()) else listOfNotNull(cheaper(), wait, goal())
                val more = listOfNotNull(
                    opt(Source.TRANSFER_NEED, "Взять $miss из «Нужного»", true) { listOf("В «Нужном» будет ${jn - miss}") },
                    savings(true),
                )
                val line = if (b < p) wallet else "В «Хочу» $jw, $t стоит $p. Не хватает $miss"
                val note = if (b >= p && r > 0) "Запас $r — на нужное и на всякий случай" else ""
                Quote(itemId, shopId, p, line, note, main + more)
            }
            Category.UNPLANNED -> {
                // ponytail: отложить непредвиденное нельзя (решение 16) — WAIT и CHEAPER здесь нет никогда
                val main = listOfNotNull(
                    if (r >= p) opt(Source.RESERVE, "Из запаса $p") { listOf("Из запаса: $r → ${r - p}") } else null,
                    if (jw >= p) opt(Source.WANT, "Из «Хочу» $p") { listOf("Из «Хочу»: $jw → ${jw - p}") } else null,
                    if (r in 1 until p && jw < p && r + jw >= p) {
                        opt(Source.RESERVE, "Запас $r + «Хочу» ${p - r}") { listOf("Из запаса: $r → 0", "Из «Хочу»: $jw → ${jw - (p - r)}") }
                    } else null,
                )
                val line = when {
                    main.isNotEmpty() -> ""
                    b < p -> wallet
                    else -> "Запас $r, $t стоит $p. Не хватает ${p - r}"
                }
                Quote(itemId, shopId, p, line, options = main + listOfNotNull(savings(main.isNotEmpty())))
            }
        }
    }

    fun buyAt(s: GameState, itemId: String, shopId: String?, source: Source): TownResult {
        checkoutRefusal(s, itemId, shopId)?.let { return TownResult.Refused(it) }
        val item = content.itemOrNull(itemId)!!
        val c = item.category
        val p = priceOf(s, item, shopId)
        if (!allowed(c, source)) {
            return TownResult.Refused(
                if (c == Category.OPTIONAL && source == Source.RESERVE) "Запас — на нужное и на всякий случай. Хотелки — из банки «Хочу»" else CANT_PAY,
            )
        }
        val jars = jarsAfter(s, c, source, p) ?: run {
            val native = when (c) {
                Category.MANDATORY -> Source.NEED
                Category.OPTIONAL -> Source.WANT
                Category.UNPLANNED -> Source.RESERVE
            }
            val q = if (source == native) quote(s, itemId, shopId).line else ""
            return TownResult.Refused(
                when {
                    q.isNotEmpty() -> q
                    source == Source.SAVINGS -> "В копилке только ${s.savings}"
                    source == Source.WANT -> "В «Хочу» только ${s.jarWant}"
                    else -> CANT_PAY
                },
            )
        }
        val a = pay(s, item, shopId, p, source, jars)
        val line = if (c == Category.MANDATORY && source == Source.RESERVE) {
            val first = s.purchases.none { it.category == Category.MANDATORY && it.source == Source.RESERVE }
            if (first && a.factMandatory > a.plan.mandatory) "Хорошо, что был запас! В новом плане дадим «Нужному» побольше?" else "Запас выручил"
        } else {
            item.reaction.replace("{pet}", s.pet!!.name)
        }
        val need = move("Из «Нужного»", s.jarNeed, a.jarNeed)
        val want = move("Из «Хочу»", s.jarWant, a.jarWant)
        val reserve = move("Из запаса", s.reserve, a.reserve)
        val why = when {
            source == Source.SAVINGS -> listOf(move("Из копилки", s.savings, a.savings))
            source == Source.NEED -> listOf(need)
            source == Source.WANT -> listOf(want)
            source == Source.TRANSFER_WANT -> listOf(want, need)
            source == Source.TRANSFER_NEED -> listOf(need, want)
            c == Category.MANDATORY -> listOf(need, reserve, want, "Еда и уход нужны каждую неделю. Запас — для сюрпризов")
            else -> listOf(reserve, want)
        }.filterNotNull().take(3)
        return TownResult.Done(events.observe(TownOutcome(a, line, why), Seen.Bought(item, source, shopId, p)))
    }

    private fun checkoutRefusal(s: GameState, itemId: String, shopId: String?): String? {
        val item = content.itemOrNull(itemId)
        return when {
            s.pet == null -> NO_PET
            s.asleep -> NIGHT
            !s.plan.confirmed -> "Покупки — после того как разложишь монеты"
            item == null -> NOT_HERE
            shopId != null && (prices.price(s, itemId, shopId) == null || item.eventOnly) -> NOT_HERE
            shopId == null && !item.eventOnly -> NOT_HERE
            item.keep && itemId in s.owned && !(s.demo && itemId in prices.keepDemo) -> "Эта вещь уже есть дома"
            shopId != null && prices.shelf(s, shopId).none { it.item.id == itemId } -> NOT_HERE
            else -> null
        }
    }

    private fun priceOf(s: GameState, item: ShopItem, shopId: String?): Int =
        if (shopId == null) item.price else prices.price(s, item.id, shopId) ?: 0

    private fun allowed(c: Category, src: Source): Boolean = src in when (c) {
        Category.MANDATORY -> setOf(Source.NEED, Source.RESERVE, Source.TRANSFER_WANT, Source.SAVINGS)
        Category.OPTIONAL -> setOf(Source.WANT, Source.TRANSFER_NEED, Source.SAVINGS)
        Category.UNPLANNED -> setOf(Source.RESERVE, Source.WANT, Source.SAVINGS)
    }

    /** Банки (jn, jw) после оплаты p из src или null, если оплата не выполнима (таблица §3). */
    private fun jarsAfter(s: GameState, c: Category, src: Source, p: Int): Pair<Int, Int>? {
        val jn = s.jarNeed
        val jw = s.jarWant
        val r = s.reserve
        return when (src) {
            Source.NEED -> if (jn >= p) jn - p to jw else null
            Source.WANT -> if (jw >= p) jn to jw - p else null
            Source.SAVINGS -> if (s.savings >= p) jn to jw else null
            Source.TRANSFER_WANT -> if (jn < p && jw >= p - jn) 0 to jw - (p - jn) else null
            Source.TRANSFER_NEED -> if (jw < p && jn >= p - jw) jn - (p - jw) to 0 else null
            Source.RESERVE -> {
                val mandatory = c == Category.MANDATORY
                val rest = if (mandatory) p - jn else p
                if ((!mandatory || jn < p) && r > 0 && r + jw >= rest) {
                    (if (mandatory) 0 else jn) to jw - (rest - minOf(r, rest))
                } else {
                    null
                }
            }
        }
    }

    private fun hasMainPay(s: GameState, c: Category, p: Int): Boolean =
        if (c == Category.MANDATORY) {
            listOf(Source.NEED, Source.RESERVE, Source.TRANSFER_WANT).any { jarsAfter(s, c, it, p) != null }
        } else {
            s.jarWant >= p
        }

    private fun pay(s: GameState, item: ShopItem, shopId: String?, p: Int, src: Source, jars: Pair<Int, Int>): GameState {
        var st = s
        if (src == Source.SAVINGS) st = economy.withdraw(st, p).state()
        st = economy.buy(st, item.id, p).state()
        val last = st.purchases.last().copy(shop = shopId, source = src)
        st = st.copy(purchases = st.purchases.dropLast(1) + last, jarNeed = jars.first, jarWant = jars.second)
        if (item.keep) st = town.putHome(st, item)
        if (shopId != null) {
            val at = town.shops.first { it.id == shopId }.at
            st = st.copy(diary = st.diary + DiaryLine(s.period, s.day, "Купили $at: ${lower(item.title)}"))
        }
        return st
    }

    // ---------- §4. План недели и копилка ----------

    fun confirmPlan(s: GameState): TownResult {
        val p = s.plan
        refusal(
            (s.pet == null) to NO_PET,
            s.asleep to NIGHT,
            p.confirmed to "План уже готов — до нового конверта он не меняется",
            (p.total == 0) to "Разложи хотя бы часть монет",
            (p.total > s.balance) to "В плане больше монет, чем в кошельке",
        )?.let { return it }
        var st = economy.confirmPlan(s).state()
        if (p.savings > 0) st = economy.deposit(st, p.savings).state()
        st = st.copy(jarNeed = p.mandatory, jarWant = p.optional, stateVersion = 1)
        val line = if (p.savings > 0) {
            "План готов! В копилку +${p.savings}, запас на всякий случай: ${st.reserve}"
        } else {
            "План готов! Запас на всякий случай: ${st.reserve}"
        }
        val o = TownOutcome(st, line, listOf("Сначала откладываем, потом тратим — так мечта ближе", "Запас — на нужное и на всякий случай, не на «хочу»"))
        return TownResult.Done(events.arrive(events.observe(o, Seen.Planned), Trigger.PlanConfirmed))
    }

    fun deposit(s: GameState, from: Source, amount: Int): TownResult {
        depositRefusal(s, from, amount)?.let { return it }
        var st = economy.deposit(s, amount).state()
        if (from == Source.WANT) st = st.copy(jarWant = st.jarWant - amount)
        val why = listOf(from(s, from, amount), "Копилка: ${s.savings} → ${st.savings}") + eta(s, st)
        return done(st, "Копилка +$amount", why).seen(Fact.Deposit)
    }

    fun depositPreview(s: GameState, from: Source, amount: Int): List<String> {
        if (depositRefusal(s, from, amount) != null) return emptyList()
        val after = s.copy(savings = s.savings + amount, depositedThisPeriod = s.depositedThisPeriod + amount)
        return listOf(from(s, from, amount)) + eta(s, after)
    }

    fun withdraw(s: GameState, amount: Int): TownResult {
        withdrawRefusal(s, amount)?.let { return it }
        return done(economy.withdraw(s, amount).state(), "Из копилки $amount — в «Не разложено»", save(s, amount)).seen(Fact.Withdraw)
    }

    fun withdrawPreview(s: GameState, amount: Int): List<String> {
        if (withdrawRefusal(s, amount) != null) return emptyList()
        return save(s, amount) + (if (s.plan.savings > 0) listOf(STAMP) else emptyList())
    }

    fun transfer(s: GameState, from: Source, to: Source, amount: Int): TownResult {
        transferRefusal(s, from, to, amount)?.let { return it }
        val jw = if (from == Source.WANT) s.jarWant - amount else s.jarWant
        val st = s.copy(jarNeed = s.jarNeed + amount, jarWant = jw)
        return done(st, "«Нужное» +$amount", listOf(from(s, from, amount), "«Нужное»: ${s.jarNeed} → ${st.jarNeed}"))
    }

    fun transferPreview(s: GameState, from: Source, to: Source, amount: Int): List<String> {
        if (transferRefusal(s, from, to, amount) != null) return emptyList()
        val over = s.jarNeed + amount > s.plan.mandatory - s.factMandatory
        return listOf(from(s, from, amount), "«Нужное»: ${s.jarNeed} → ${s.jarNeed + amount}") +
            (if (over) listOf("Потратишь больше плана — штампа «По плану» не будет") else emptyList())
    }

    private fun depositRefusal(s: GameState, from: Source, a: Int) = refusal(
        (s.pet == null) to NO_PET,
        s.asleep to NIGHT,
        !s.plan.confirmed to "Отложить можно после раскладки",
        (a <= 0) to ABOVE_ZERO,
        (from != Source.RESERVE && from != Source.WANT) to "Так отложить нельзя",
        (from == Source.RESERVE && a > s.reserve) to "В запасе только ${s.reserve}",
        (from == Source.WANT && a > s.jarWant) to "В «Хочу» только ${s.jarWant}",
    )

    private fun withdrawRefusal(s: GameState, a: Int) = refusal(
        (s.pet == null) to NO_PET,
        s.asleep to NIGHT,
        s.plan.confirmed to "После раскладки из копилки платят только у кассы",
        (a <= 0) to ABOVE_ZERO,
        (a > s.savings) to "В копилке только ${s.savings}",
    )

    private fun transferRefusal(s: GameState, from: Source, to: Source, a: Int) = refusal(
        (s.pet == null) to NO_PET,
        s.asleep to NIGHT,
        !s.plan.confirmed to "Перенос — после раскладки",
        (a <= 0) to ABOVE_ZERO,
        (to != Source.NEED || (from != Source.RESERVE && from != Source.WANT)) to "Так перенести нельзя",
        (from == Source.RESERVE && a > s.reserve) to "В запасе только ${s.reserve}",
        (from == Source.WANT && a > s.jarWant) to "В «Хочу» только ${s.jarWant}",
    )

    /** FROM спеки: откуда уходят монеты взноса или переноса. */
    private fun from(s: GameState, from: Source, a: Int): String =
        if (from == Source.RESERVE) "Запас: ${s.reserve} → ${s.reserve - a}" else "«Хочу»: ${s.jarWant} → ${s.jarWant - a}"

    // ---------- §5. Дни, сон, конец недели ----------

    fun sleep(s: GameState): TownResult {
        refusal((s.pet == null) to NO_PET, !s.plan.confirmed to NO_PLAN_SLEEP, s.asleep to "Уже ночь")?.let { return it }
        if (s.day == town.rules.daysPerWeek) return endWeek(s)
        val ev = events.defaults(TownOutcome(s), setOf(Until.DAY_END))
        val lines = s.diary.filter { it.period == s.period && it.day == s.day }.map { it.text }
        val line = when (lines.size) {
            0 -> "Спокойный день дома"
            1 -> lines[0]
            else -> "${lines[0]}. ${lines[1]}"
        }
        return TownResult.Done(ev.copy(state = ev.state.copy(asleep = true), line = line))
    }

    fun wake(s: GameState): TownResult {
        refusal((s.pet == null) to NO_PET, !s.asleep to "Уже утро")?.let { return it }
        val st = s.copy(asleep = false, day = if (s.plan.confirmed) s.day + 1 else s.day)
        val last = s.history.lastOrNull()
        val line = if (!s.plan.confirmed && s.period > 1 && s.day == 1) {
            "Новый конверт: карманные ${content.rules.allowance}" +
                (last?.shiftEarned?.takeIf { it > 0 }?.let { " + зарплата $it" } ?: "") +
                (last?.parentBonus?.takeIf { it > 0 }?.let { " + бонус $it" } ?: "")
        } else {
            "Доброе утро!"
        }
        return TownResult.Done(events.arrive(TownOutcome(st, line), null))
    }

    fun endWeek(s0: GameState): TownResult {
        refusal(
            (s0.pet == null) to NO_PET,
            !s0.plan.confirmed to NO_PLAN_SLEEP,
            (!s0.demo && s0.day < town.rules.daysPerWeek) to "Неделя ещё идёт",
        )?.let { return it }
        // TOWN-S1c §5: WeekEndNo до Economy.endPeriod, потом умолчания, потом тихое закрытие
        var ev = TownOutcome(s0)
        for (n in listOf(Need.FOOD, Need.CARE)) {
            if (s0.purchases.none { it.need == n }) ev = events.observe(ev, Seen.Plain(Fact.WeekEndNo(n)), rewrite = false)
        }
        ev = events.closeAll(events.defaults(ev, setOf(Until.DAY_END, Until.WEEK_END)))
        val s = ev.state
        val ended = economy.endPeriod(s).state()
        fun sum(prefix: String) = s.envelope.filter { it.text.startsWith(prefix) }.sumOf { it.amount }
        val summary = ended.history.last().copy(shiftEarned = sum("Смена: "), parentBonus = sum("Бонус от взрослого: "))
        val balance = ended.balance + s.envelope.sumOf { it.amount }
        // заготовка: избыток снимается с «Хочу», потом с «В копилку», потом с «Нужного»
        var (m, o, sv) = Triple(s.plan.mandatory, s.plan.optional, s.plan.savings)
        var excess = maxOf(0, m + o + sv - balance)
        minOf(o, excess).let { o -= it; excess -= it }
        minOf(sv, excess).let { sv -= it; excess -= it }
        m = maxOf(0, m - excess)
        val st = ended.copy(
            history = ended.history.dropLast(1) + summary,
            balance = balance,
            ledger = ended.ledger + s.envelope,
            envelope = emptyList(),
            jarNeed = 0, jarWant = 0, day = 1, asleep = true, shiftsThisPeriod = 0,
            plan = BudgetPlan(m, o, sv),
        )
        val fs = summary.factSavings
        val got = listOf(summary.mandatoryCovered, summary.planKept, summary.saved)
        val gotText = listOf("Еда и уход были всю неделю!", "Траты — по плану!", "Копилка +$fs!")
        val stepText = listOf(
            "В новом конверте начнём с корма и мыла",
            "В новом конверте попробуем по плану",
            "В новом конверте попробуем отложить",
        )
        val line = when (got.count { it }) {
            3 -> "Всё по плану, копилка +$fs!"
            0 -> "Неделя позади. Начнём с корма и мыла?"
            else -> "${gotText[got.indexOf(true)]} ${stepText[got.indexOf(false)]}"
        }
        val plan = summary.plan
        val why = listOf(
            "«Нужное»: план ${plan.mandatory}, потрачено ${summary.factMandatory}",
            "«Хочу»: план ${plan.optional}, потрачено ${summary.factOptional}",
            "Копилка: план ${plan.savings}, отложено ${maxOf(fs, 0)}",
        )
        return TownResult.Done(ev.copy(state = st, line = line, why = why))
    }

    fun weekEndPreview(s: GameState): List<String> {
        val pet = s.pet?.name ?: return emptyList()
        return listOfNotNull(
            "Еда на этой неделе не куплена — $pet проголодается".takeIf { s.purchases.none { it.need == Need.FOOD } },
            "Уход на этой неделе не куплен — $pet испачкается".takeIf { s.purchases.none { it.need == Need.CARE } },
        )
    }

    fun planTweaks(s: GameState, summary: PeriodSummary): List<Recovery.PlanTweak> {
        val step = content.rules.planStep
        val candidates = mutableListOf<Recovery.PlanTweak>()
        if (!summary.mandatoryCovered || summary.factMandatory > summary.plan.mandatory) {
            candidates += Recovery.PlanTweak(TweakDir.NEED, step)
        }
        s.events.filter { it.period == summary.period && it.verdict == Verdict.MISTAKE }.forEach { e ->
            val outcome = e.outcome ?: return@forEach
            val def = town.events.firstOrNull { it.id == e.id } ?: return@forEach
            candidates += def.outcomes.getOrNull(outcome)?.recovery.orEmpty().filterIsInstance<Recovery.PlanTweak>()
        }
        if (s.goal != null) candidates += Recovery.PlanTweak(TweakDir.SAVINGS, step)
        return candidates.filter { tweakFits(s, it) }.distinctBy { it.dir }.take(3)
    }

    fun chooseTweak(s: GameState, tweak: Recovery.PlanTweak?): TownResult {
        refusal((s.pet == null) to NO_PET, s.plan.confirmed to "План уже готов")?.let { return it }
        if (tweak == null) return done(s)
        if (!tweakFits(s, tweak)) {
            return TownResult.Refused(
                if (tweak.dir == TweakDir.RESERVE) "В «Хочу» меньше ${tweak.n}" else "В плане больше монет, чем в кошельке",
            )
        }
        val p = s.plan
        val n = tweak.n
        val plan = when (tweak.dir) {
            TweakDir.WANT -> p.copy(optional = p.optional + n)
            TweakDir.RESERVE -> p.copy(optional = p.optional - n)
            TweakDir.NEED, TweakDir.SAVINGS -> {
                val grown = if (tweak.dir == TweakDir.NEED) p.copy(mandatory = p.mandatory + n) else p.copy(savings = p.savings + n)
                grown.copy(optional = grown.optional - maxOf(0, grown.total - s.balance))
            }
        }
        return done(s.copy(plan = plan))
    }

    /** Выполнимость поправки на неподтверждённом плане (§5). */
    private fun tweakFits(s: GameState, t: Recovery.PlanTweak): Boolean {
        val total = s.plan.total + t.n
        return when (t.dir) {
            TweakDir.WANT -> total <= s.balance
            TweakDir.NEED, TweakDir.SAVINGS -> total <= s.balance || total - s.balance <= s.plan.optional
            TweakDir.RESERVE -> s.plan.optional >= t.n
        }
    }

    // ---------- §6. Дом, мечта, места, посещение ----------

    fun place(s: GameState, spotId: String, itemId: String?): TownResult {
        val spot = town.spots.firstOrNull { it.id == spotId } ?: return TownResult.Refused("Такого места нет")
        town.homeItems.firstOrNull { it.spot == spotId }?.let { return TownResult.Refused("Здесь стоит ${lower(it.title)}") }
        if (itemId == null) return done(s.copy(placed = s.placed - spotId))
        if (itemId !in s.owned) return TownResult.Refused("Этой вещи нет в сундуке")
        if (content.itemOrNull(itemId)?.slot != spot.slot) return TownResult.Refused("Эта вещь сюда не встанет")
        return done(s.copy(placed = s.placed.filterValues { it != itemId } + (spotId to itemId)))
    }

    fun makeGoal(s: GameState, itemId: String): TownResult {
        s.pet ?: return TownResult.Refused(NO_PET)
        val item = content.itemOrNull(itemId) ?: return TownResult.Refused(NOT_HERE)
        if (item.keep && itemId in s.owned) return TownResult.Refused("Эта вещь уже есть дома")
        val min = town.rules.customGoalFromItemMin
        val base = prices.basePrice(itemId)
        if (!item.keep || base == null || base < min) return TownResult.Refused("Мечтой можно сделать вещь от $min")
        val st = economy.chooseGoal(s, Goal("item:$itemId", item.title, item.emoji, base)).state()
        return done(st, "Мечта: ${lower(item.title)} — $base").seen(Fact.MakeGoal(itemId))
    }

    fun achieveGoal(s: GameState): TownResult {
        val goal = s.goal
        refusal(
            (s.pet == null) to NO_PET,
            s.asleep to NIGHT,
            (goal == null) to "Сначала выбери мечту",
            (goal != null && s.savings < goal.price) to "До мечты ещё ${(goal?.price ?: 0) - s.savings}",
        )?.let { return it }
        goal!!
        var st = economy.achieveGoal(s).state()
        val item = goal.id.removePrefix("item:").takeIf { goal.id.startsWith("item:") }?.let { content.itemOrNull(it) }
        if (item != null) st = town.putHome(st, item)
        st = st.copy(diary = st.diary + DiaryLine(s.period, s.day, "Мечта сбылась: ${lower(goal.title)}"))
        return done(st, "Мечта сбылась: ${goal.title}!")
    }

    /** Вход в место: visited → приход (Enter и плановая проверка) → цены полки → наклейка места (TOWN-S1c §7). */
    fun visit(s0: GameState, placeId: String): TownOutcome {
        val place = town.places.firstOrNull { it.id == placeId } ?: return TownOutcome(s0)
        val o = events.arrive(TownOutcome(s0.copy(visited = s0.visited.plusNew(placeId))), Trigger.Enter(placeId))
        val s = o.state
        val seen = s.seenPrices.toMutableMap()
        town.shops.firstOrNull { it.place == placeId }?.let { shop ->
            prices.shelf(s, shop.id).forEach { si ->
                val old = seen[si.item.id]
                if (old == null || old.period < s.period || si.price < old.price || old.shop == shop.id) {
                    seen[si.item.id] = SeenPrice(shop.id, si.price, s.period)
                }
            }
        }
        return o.copy(state = s.copy(seenPrices = seen, stickers = s.stickers.plusNew(place.sticker)))
    }

    // ---------- TOWN-S1c. События: приход, доска, карточка, «Пройти мимо», демо ----------

    /** Плановая проверка прихода; VM зовёт после создания профиля, загрузки и миграции (§2). */
    fun tick(s: GameState): TownOutcome = events.arrive(TownOutcome(s), null)

    /** ACTIVE живые события не-JOB в порядке s.events (§6). */
    fun activeEvents(s: GameState): List<EventDef> = events.active(s)

    /** Заказы JOB: смена оплачивается, requires без демо-ослаблений, место открыто (§6). */
    fun orders(s: GameState): List<EventDef> = events.live.filter { e ->
        e.kind == EventKind.JOB && e.params.job?.let { shiftQuote(s, it).paid } == true &&
            e.requires.all { events.holds(s, it, false) } && !events.closed(s, e)
    }

    /** Карточка «В городке» — никогда не пустая (§6). */
    fun card(s: GameState): Card =
        (activeEvents(s) + orders(s)).firstOrNull()?.let { Card(it.title, it.id, it.place) }
            ?: Card("В городке спокойно — загляни на доску")

    /** «Пройти мимо»: факт Skip только для этого события (§6). */
    fun pass(s: GameState, eventId: String): TownResult {
        refusal(
            (s.pet == null) to NO_PET,
            s.asleep to NIGHT,
            (events.liveEvent(eventId) == null || !events.isActive(s, eventId)) to "Такого события сейчас нет",
        )?.let { return it }
        val o = events.observe(TownOutcome(s), Seen.Plain(Fact.Skip), only = eventId)
        return if (o.eventResults.isEmpty()) TownResult.Refused("Здесь решают делом") else TownResult.Done(o)
    }

    /** Демо-доска: все живые не-JOB события в открытых местах (§6). */
    fun demoBoard(s: GameState): List<EventDef> =
        if (!s.demo) emptyList() else events.live.filter { it.kind != EventKind.JOB && !events.closed(s, it) }

    /** Демо-запуск события; при невыполненных requires — demo.setup и повторная проверка (§6). */
    fun startEvent(s: GameState, eventId: String): TownResult {
        val e = events.liveEvent(eventId)
        refusal(
            (s.pet == null) to NO_PET,
            s.asleep to NIGHT,
            !s.demo to "Событие придёт само",
            (e == null) to "Такого события нет",
            (e != null && events.closed(s, e)) to "Место откроет мечта",
            events.isActive(s, eventId) to "Событие уже идёт",
        )?.let { return it }
        e!!
        val st = if (events.unmet(s, e) == null) s else events.demoSetup(s, e)
        events.unmet(st, e)?.let { return TownResult.Refused(events.notReady(st, it)) }
        return TownResult.Done(events.start(TownOutcome(st), e).copy(line = e.intro))
    }

    // ---------- §1–§4. Смены, «Загадка Бори», бонус взрослого (TOWN-S1b) ----------

    /** Открыта ли работа jobId и что она обещает до самой смены (§1). */
    fun shiftQuote(s: GameState, jobId: String): ShiftQuote {
        val shiftsLeft = maxOf(0, town.rules.shiftsPerWeek - s.shiftsThisPeriod)
        val job = town.jobs.firstOrNull { it.id == jobId }
            ?: return ShiftQuote(jobId, false, false, false, 0, 0, 0, shiftsLeft, CLOSED, 0)
        val place = town.places.firstOrNull { it.id == job.place }
        val open = opensByOk(job.opensBy, s) && (place == null || opensByOk(place.opensBy, s))
        val paid = open && shiftsLeft > 0
        // № 47 б: игра после лимита смен больше не идёт ни у одной работы
        val canPlay = open && !s.asleep && paid
        val shifts = s.jobShifts[jobId] ?: 0
        val level = town.rules.jobLevelShifts.indexOfLast { it <= shifts }
        val base = job.baseByLevel.getOrElse(level) { 0 }
        val levelBombs = if (job.game == JobGame.MATCH3) town.rules.jobLevelBombs.getOrElse(level) { 0 } else 0
        val top = if (job.game == JobGame.TAPS) base else base + town.rules.shiftBonusMax
        val line = when {
            !open -> CLOSED
            s.asleep -> NIGHT
            paid -> if (job.game == JobGame.TAPS) "$base за три поручения" else "$base–$top за смену"
            else -> LIMIT
        }
        return ShiftQuote(jobId, open, paid, canPlay, level, base, levelBombs, shiftsLeft, line, top)
    }

    /** Житель недели — последний из приехавших к этой неделе (RoomScreen.kt:130; TOWN-J1-0 § 3). */
    fun residentOfWeek(s: GameState): Resident? =
        town.residents.lastOrNull { it.arrivesWeek != null && it.arrivesWeek <= s.period }

    /** Рекорд звёзд подноса; отсекает наследие Match3 и значения не-TRAY работ (TOWN-J1-0 § 3). */
    fun bestStars(s: GameState, jobId: String): Int? {
        val job = town.jobs.firstOrNull { it.id == jobId } ?: return null
        if (job.game != JobGame.TRAY) return null
        val v = s.records[jobId] ?: return null
        return v.takeIf { it in 0..town.rules.shiftBonusMax }
    }

    /** Ступень меню подноса по числу оплаченных смен на этой работе (TOWN-J1-0 § 1). */
    private fun trayStep(job: Job, n: Int): TrayStep = job.steps.last { it.fromShift <= n }

    /** Размеры заказов этой смены: demoSizes в демо, иначе — ступени (TOWN-J1-0 § 1, § 3). */
    private fun traySizes(s: GameState, job: Job, n: Int): List<Int> =
        if (s.demo && job.demoSizes != null) job.demoSizes else trayStep(job, n).sizes

    /** Смесь seed, недели, сыгранных смен недели и мастерства — раскладка заказов (TOWN-J1-0 § 3). */
    private fun traySeed(s: GameState, n: Int): Long {
        var h = s.seed
        h = h * 6364136223846793005L + s.period
        h = h * 6364136223846793005L + s.shiftsThisPeriod
        h = h * 6364136223846793005L + n
        return h
    }

    /** k изделий из витрины: distinct = min(k, 1 + k/2) видов, каждый — хотя бы раз, по порядку меню. */
    private fun trayItems(rnd: kotlin.random.Random, menuIds: List<String>, k: Int): List<String> {
        val distinct = minOf(k, 1 + k / 2)
        val chosenIdx = menuIds.indices.shuffled(rnd).take(distinct).sorted()
        val counts = IntArray(distinct) { 1 }
        var remaining = k - distinct
        while (remaining > 0) {
            counts[rnd.nextInt(distinct)]++
            remaining--
        }
        val items = mutableListOf<String>()
        chosenIdx.forEachIndexed { i, idx -> repeat(counts[i]) { items += menuIds[idx] } }
        return items
    }

    /** Покупатели: из пула, без повтора подряд, житель недели — хотя бы в одном заказе. */
    private fun trayOrders(rnd: kotlin.random.Random, pool: List<String>, sizes: List<Int>, menuIds: List<String>, wid: String?): List<TrayOrder> {
        val customers = mutableListOf<String>()
        sizes.forEach {
            val cand = pool.filter { c -> c != customers.lastOrNull() }
            customers += cand[rnd.nextInt(cand.size)]
        }
        if (wid != null && wid !in customers) customers[rnd.nextInt(customers.size)] = wid
        return customers.zip(sizes).map { (c, k) -> TrayOrder(c, trayItems(rnd, menuIds, k)) }
    }

    /** Раунд подноса — только у оплачиваемой смены на TRAY-работе с валидной витриной (TOWN-J1-0 § 3). */
    fun trayRound(s: GameState, jobId: String): TrayRound? {
        val job = town.jobs.firstOrNull { it.id == jobId } ?: return null
        if (job.game != JobGame.TRAY) return null
        if (!shiftQuote(s, jobId).canPlay) return null
        val pool = town.residents.filter { it.arrivesWeek != null && it.arrivesWeek <= s.period && it.id != job.resident }.map { it.id }
        if (pool.size < 2) return null
        val n = s.jobShifts[jobId] ?: 0
        val step = trayStep(job, n)
        val sizes = traySizes(s, job, n)
        val kinds = minOf(step.kinds, job.menu.size)
        if (sizes.any { k -> kinds < minOf(k, 1 + k / 2) }) return null
        val menuIds = job.menu.take(step.kinds).map { it.id }
        val w = residentOfWeek(s)
        val wid = if (w != null && w.id != job.resident) w.id else null
        val rnd = kotlin.random.Random(traySeed(s, n))
        val orders = trayOrders(rnd, pool, sizes, menuIds, wid)
        return TrayRound(
            jobId = jobId, menu = menuIds, orders = orders,
            pointer = (bestStars(s, jobId) ?: 0) == 0,
            intro = if (n == step.fromShift) step.intro else null,
            riddle = n >= 1 && nextQuestion(s) != null,
        )
    }

    /** Загадка Бори в раунде подноса — значок «?» (не в первой смене, после первого покупателя, TOWN-J1-1a § 1). */
    fun riddleInRound(s: GameState, r: TrayRound): QuizQuestion? =
        if (r.riddle && r.index >= 1 && !r.done) nextQuestion(s) else null

    /** Ответ на загадку в раунде: верный кладёт подсказку на поднос, а не бомбочку (TOWN-J1-1a § 1). */
    fun answerInRound(s: GameState, r: TrayRound, questionId: String, optionIndex: Int): TrayAnswer {
        if (riddleInRound(s, r)?.id != questionId) return TrayAnswer(TownResult.Refused(NO_RIDDLE), r)
        val res = answerQuestion(s, questionId, optionIndex)
        val correct = (res as? TownResult.Done)?.outcome?.state?.riddles?.lastOrNull()?.correct == true
        if (res !is TownResult.Done || !correct) return TrayAnswer(res, r)
        val h = Tray.hint(r, town.rules.riddleHint)
        if (h != r) return TrayAnswer(res, h)
        return TrayAnswer(TownResult.Done(res.outcome.copy(why = listOf(HINT_IDLE))), r)
    }

    /** Когда открывается место или работа (§1): неделя (демо — сразу) или сбывшаяся мечта. */
    private fun opensByOk(o: OpensBy, s: GameState): Boolean = when {
        o.week != null -> s.demo || s.period >= o.week
        o.goal != null -> s.achievedGoals.any { it.id == o.goal }
        else -> true
    }

    /** Конец смены — только оплачиваемой, после лимита работа отказывает (§2, № 47 б). */
    fun finishShift(s: GameState, jobId: String, score: Int, bombsUsed: Int): TownResult {
        val quote = shiftQuote(s, jobId)
        val job = town.jobs.firstOrNull { it.id == jobId }
        val n = s.jobShifts[jobId] ?: 0
        refusal(
            (s.pet == null) to NO_PET,
            s.asleep to NIGHT,
            !quote.open to CLOSED,
            !quote.canPlay to quote.line,
            (score < 0 || bombsUsed < 0) to BAD_SHIFT,
            (job?.game != JobGame.MATCH3 && bombsUsed > 0) to BAD_SHIFT,
            (bombsUsed > quote.levelBombs + s.bombs) to BAD_SHIFT,
            (job?.game == JobGame.TAPS && score > job.tasks.size) to BAD_SHIFT,
            (job?.game == JobGame.TRAY && score > traySizes(s, job, n).size) to BAD_SHIFT,
        )?.let { return it }
        job!!
        val hasRecord = job.game != JobGame.TAPS
        val bonus = when (job.game) {
            JobGame.MATCH3 -> minOf(town.rules.shiftBonusMax, score / town.rules.shiftScorePerBonus)
            JobGame.TAPS -> 0
            JobGame.CHANGE, JobGame.TRAY -> minOf(town.rules.shiftBonusMax, score)
        }
        val prevRecord = s.records[jobId] ?: 0
        val isNew = (job.game == JobGame.MATCH3 || job.game == JobGame.CHANGE) && score > prevRecord
        val bombSpent = maxOf(0, bombsUsed - quote.levelBombs)
        var st = s.copy(bombs = s.bombs - bombSpent)
        if (job.game == JobGame.MATCH3 || job.game == JobGame.TRAY) st = st.copy(riddleAsked = false)
        if (hasRecord) {
            val newRecord = if (job.game == JobGame.TRAY) maxOf(bestStars(s, jobId) ?: 0, score) else maxOf(prevRecord, score)
            st = st.copy(records = st.records + (jobId to newRecord))
        }

        val total = quote.base + bonus
        st = st.copy(
            envelope = st.envelope + LedgerEntry("Смена: ${job.title}", total),
            shiftsThisPeriod = st.shiftsThisPeriod + 1,
            jobShifts = st.jobShifts + (jobId to ((st.jobShifts[jobId] ?: 0) + 1)),
            diary = st.diary + DiaryLine(s.period, s.day, "Заработали $total: «${job.title}»"),
            stickers = st.stickers.plusNew(events.live.firstOrNull { it.kind == EventKind.JOB && it.params.job == jobId }?.sticker),
        )
        val line = when {
            job.game == JobGame.TAPS -> "${quote.base} за три поручения. ✉ +$total — придёт с новым конвертом"
            bonus > 0 -> {
                val what = when (job.game) { JobGame.MATCH3 -> "булочки"; JobGame.TRAY -> "★"; else -> "сдачу" }
                "${quote.base} за смену + $bonus за $what. ✉ +$total — придёт с новым конвертом"
            }
            else -> "${quote.base} за смену. ✉ +$total — придёт с новым конвертом"
        }
        val newShifts = st.jobShifts[jobId] ?: 0
        val levelBefore = quote.level
        val levelAfter = town.rules.jobLevelShifts.indexOfLast { it <= newShifts }
        val name = town.residents.first { it.id == job.resident }.name
        val why = mutableListOf<String>()
        why += when {
            levelAfter > levelBefore -> {
                val base2 = job.baseByLevel.getOrElse(levelAfter) { 0 }
                val top2 = if (job.game == JobGame.TAPS) base2 else base2 + town.rules.shiftBonusMax
                val quote2 = if (job.game == JobGame.TAPS) "$base2 за три поручения" else "$base2–$top2 за смену"
                "$name: новый уровень ${levelAfter + 1}! Теперь $quote2"
            }
            levelAfter + 1 < town.rules.jobLevelShifts.size ->
                "$name: $newShifts из ${town.rules.jobLevelShifts[levelAfter + 1]} смен до уровня ${levelAfter + 2}"
            else -> "$name: высший уровень мастерства"
        }
        why += "Карманные приходят каждую неделю, зарплата — когда поработаешь"
        if (isNew) why += "Новый рекорд!"
        return TownResult.Done(TownOutcome(st, line, why, pay = ShiftPay(quote.base, bonus, total)))
    }

    /** Доступные загадки, отсортированные по спеке (§3). */
    private fun availableQuestions(s: GameState): List<QuizQuestion> {
        val solved = s.riddles.filter { it.correct }.map { it.taskId }.toSet()
        val available = town.quiz.filter { it.id !in solved }
        val (never, asked) = available.partition { q -> s.riddles.none { it.taskId == q.id } }
        val ordered = asked.sortedBy { q -> s.riddles.indexOfLast { it.taskId == q.id } }
        return never + ordered
    }

    /** Следующая «Загадка Бори» или null, если её сейчас нет (§3). */
    fun nextQuestion(s: GameState): QuizQuestion? = if (s.riddleAsked) null else availableQuestions(s).firstOrNull()

    /** Ответ на «Загадку Бори»: бомбочка за верный, без монет и показателей (§3). */
    fun answerQuestion(s: GameState, questionId: String, optionIndex: Int): TownResult {
        val q = town.quiz.firstOrNull { it.id == questionId }
        val alreadyCorrect = s.riddles.any { it.taskId == questionId && it.correct }
        refusal(
            (s.pet == null) to NO_PET,
            s.riddleAsked to "Следующая загадка — после смены",
            (q == null || alreadyCorrect) to "Этот вопрос уже разобран",
            (optionIndex !in q?.options.orEmpty().indices) to "Выбери ответ",
        )?.let { return it }
        q!!
        val correct = optionIndex == q.correct
        val m = town.jobs.first { it.game == JobGame.MATCH3 || it.game == JobGame.TRAY }
        val st = s.copy(
            riddles = s.riddles + TaskResult(questionId, correct, 0, s.period),
            riddleAsked = true,
            bombs = if (correct && m.game == JobGame.MATCH3) s.bombs + content.rules.quizBombReward else s.bombs,
        )
        val line = if (correct) "Верно! ${q.explanation}" else q.explanation
        val why = when {
            !correct -> listOf("Вопрос вернётся позже")
            m.game == JobGame.MATCH3 -> listOf("Бомбочка +${content.rules.quizBombReward} — для поля «${m.title}»")
            else -> listOf("Подсказка Бори — на поднос")
        }
        return done(st, line, why)
    }

    /** Бонус взрослого в конверт следующей недели (§4). Ночью разрешён. */
    fun parentBonus(s: GameState, reasonIndex: Int): TownResult {
        refusal(
            (s.pet == null) to NO_PET,
            (reasonIndex !in town.parentBonusReasons.indices) to "Выберите причину бонуса",
            (s.parentBonusesThisPeriod >= content.rules.parentBonusPerPeriod) to
                "На этой неделе все бонусы уже начислены — новые будут со следующей недели",
        )?.let { return it }
        val reason = town.parentBonusReasons[reasonIndex]
        val st = s.copy(
            envelope = s.envelope + LedgerEntry("Бонус от взрослого: $reason", content.rules.parentBonusAmount),
            parentBonusesThisPeriod = s.parentBonusesThisPeriod + 1,
        )
        val left = content.rules.parentBonusPerPeriod - st.parentBonusesThisPeriod
        val why = if (left > 0) listOf("На этой неделе можно начислить ещё $left") else listOf("Лимит бонусов на эту неделю исчерпан")
        return done(st, "Придёт в новом конверте ребёнка", why)
    }

    // ---------- общее ----------

    private fun planKept(x: GameState): Boolean =
        x.plan.confirmed && x.factMandatory <= x.plan.mandatory && x.factOptional <= x.plan.optional &&
            maxOf(x.factSavings, 0) >= x.plan.savings

    /** SAVE(n): копилка и срок мечты после снятия n. */
    private fun save(s: GameState, n: Int): List<String> =
        listOf("Копилка: ${s.savings} → ${s.savings - n}") +
            eta(s, s.copy(savings = s.savings - n, withdrawnThisPeriod = s.withdrawnThisPeriod + n))

    /** ETA: строка срока мечты до и после действия (§1). */
    private fun eta(before: GameState, after: GameState): List<String> {
        val g = before.goal ?: return emptyList()
        val a = economy.goalEta(before)
        val b = economy.goalEta(after)
        return listOfNotNull(
            when {
                b == 0 && a != 0 -> "${g.title}: хватит на мечту!"
                a != null && b != null && a != b -> "${g.title}: ≈ $a ✉ → ≈ $b ✉"
                a == null && b != null -> "${g.title}: ≈ $b ✉"
                else -> null
            },
        )
    }

    private fun move(label: String, a: Int, b: Int): String? = if (a == b) null else "$label: $a → $b"

    private fun refusal(vararg checks: Pair<Boolean, String>): TownResult.Refused? =
        checks.firstOrNull { it.first }?.let { TownResult.Refused(it.second) }

    private fun done(s: GameState, line: String = "", why: List<String> = emptyList()): TownResult =
        TownResult.Done(TownOutcome(s, line, why))

    /** Наблюдение простого факта после действия (TOWN-S1c §3). */
    private fun TownResult.seen(f: Fact): TownResult =
        if (this is TownResult.Done) TownResult.Done(events.observe(outcome, Seen.Plain(f))) else this

    private fun Outcome.state(): GameState = (this as? Outcome.Ok)?.state ?: error("Economy отказала: $this")

    private companion object {
        const val STAMP = "Штамп «По плану» на этой неделе не получится"
        const val NO_PET = "Сначала создай питомца"
        const val NIGHT = "Сейчас ночь — сначала проснёмся"
        const val NOT_HERE = "Этого товара здесь нет"
        const val CLOSED = "Эта работа пока закрыта"
        const val BAD_SHIFT = "Так закончить смену нельзя"
        const val LIMIT = "Смены на неделе закончились — новые с новым конвертом"
        const val CANT_PAY = "Так оплатить нельзя"
        const val ABOVE_ZERO = "Выбери сумму больше нуля"
        const val NO_PLAN_SLEEP = "Сначала разложим монеты — потом спать"
        const val NO_RIDDLE = "Загадки сейчас нет"
        const val HINT_IDLE = "Этот поднос Боря оставил тебе"
    }
}
