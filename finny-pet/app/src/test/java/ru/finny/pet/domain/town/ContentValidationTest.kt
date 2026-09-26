package ru.finny.pet.domain.town

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.pet.domain.Category
import ru.finny.pet.domain.ShopItem
import ru.finny.pet.domain.TestContent
import ru.finny.pet.domain.Theme
import java.io.File

/**
 * Валидатор контента «Городка» (docs/tasks/TOWN-S0b.md, ORACLE, проверки 1–22).
 * Читает реальный content.json двумя путями: разобранным (TestContent.content) и сырым JSON.
 * Правила берутся из спеки, а не из текущего содержимого файла.
 */
class ContentValidationTest {

    private val content = TestContent.content
    private val town: TownContent get() = content.town ?: error("content.json: нет ключа town")

    private val raw: JsonObject by lazy {
        Json.parseToJsonElement(File("src/main/assets/content/content.json").readText()).jsonObject
    }
    private val rawTown: JsonObject by lazy { raw.getValue("town").jsonObject }

    // ---------- справочники ----------

    /** «Все товары» = items верхнего уровня ∪ town.items. */
    private val allItems: List<ShopItem> by lazy { content.items + town.items }
    private val itemIds: Set<String> by lazy { allItems.map { it.id }.toSet() }
    private val placeIds: Set<String> by lazy { town.places.map { it.id }.toSet() }
    private val shopIds: Set<String> by lazy { town.shops.map { it.id }.toSet() }
    private val homeIds: Set<String> by lazy { town.homeItems.map { it.id }.toSet() }
    private val goalIds: Set<String> by lazy { town.goals.map { it.id }.toSet() }
    private val jobIds: Set<String> by lazy { town.jobs.map { it.id }.toSet() }
    private val residentIds: Set<String> by lazy { town.residents.map { it.id }.toSet() }
    private val stickerIds: Set<String> by lazy { town.stickers.map { it.id }.toSet() }

    /** id товара -> лавки, которые его продают. */
    private val shopsSelling: Map<String, List<Shop>> by lazy {
        town.shops.flatMap { s -> s.sells.map { it.item to s } }.groupBy({ it.first }, { it.second })
    }
    private val soldIds: Set<String> by lazy { shopsSelling.keys }
    private val soldTags: Set<String> by lazy { allItems.filter { it.id in soldIds }.flatMap { it.tags }.toSet() }

    private fun itemOrNull(id: String): ShopItem? = allItems.firstOrNull { it.id == id }
    private fun priceIn(shopId: String, itemId: String): Int? =
        town.shops.firstOrNull { it.id == shopId }?.sells?.firstOrNull { it.item == itemId }?.price

    private val countable: List<EventDef> get() = town.events.filter { it.theme != null }

    private fun flatten(c: Condition): List<Condition> =
        if (c is Condition.AnyOf) c.options.flatMap { flatten(it) } else listOf(c)

    private fun conditionsOf(e: EventDef): List<Condition> =
        (e.requires + e.outcomes.mapNotNull { it.condition }).flatMap { flatten(it) }

    private fun factsOf(e: EventDef): List<Fact> = e.outcomes.map { it.fact } + listOfNotNull(e.default?.fact)

    /** Все эффекты события вместе с местом, где они записаны. */
    private fun effectsOf(e: EventDef): List<Pair<String, EventEffect>> =
        e.setup.map { "setup" to it } +
            e.outcomes.flatMap { o -> o.effects.map { "исход «${o.line}»" to it } } +
            e.demo.setup.map { "demo.setup" to it }

    private fun ref(what: String, id: String?, pool: Set<String>) {
        if (id == null) return
        assertTrue("$what ссылается на «$id», такой записи нет", id in pool)
    }

    private fun dup(what: String, ids: List<String>) {
        val repeated = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertTrue("$what — повторяющиеся id $repeated", repeated.isEmpty())
    }

    // ---------- тексты town (проверки 6–9) ----------

    /** Ключи со строками словаря событий: их значения — код, а не текст ребёнку. */
    private val dictKeys = setOf("fact", "when", "requires", "setup", "effects", "recovery", "triggers")

    /** Ссылочные и служебные ключи: значения — id и имена перечислений. */
    private val refKeys = setOf(
        "id", "place", "resident", "sticker", "item", "shop", "job", "goal", "replaceItem", "spot",
        "slot", "tags", "species", "color", "accessory", "template", "kind", "theme", "verdict",
        "game", "category", "need", "emoji", "_todo",
    )

    private fun collect(el: JsonElement, path: String, out: MutableList<Pair<String, String>>) {
        when (el) {
            is JsonObject -> el.forEach { (k, v) -> if (k !in dictKeys && k !in refKeys) collect(v, "$path.$k", out) }
            is JsonArray -> el.forEachIndexed { i, v -> collect(v, path + "#" + i, out) }
            is JsonPrimitive -> if (el.isString) out.add(path to el.content)
        }
    }

    /** Все строковые значения поддерева town, кроме id, строк словаря и ссылочных полей. */
    private val townTexts: List<Pair<String, String>> by lazy {
        val out = mutableListOf<Pair<String, String>>()
        collect(rawTown, "town", out)
        out
    }

    /** Тексты товаров верхнего уровня, которые продают лавки town (их показывает game у Фомы). */
    private val soldTopItemTexts: List<Pair<String, String>> by lazy {
        content.items.filter { it.id in soldIds }
            .flatMap { listOf("${it.id}.description" to it.description, "${it.id}.reaction" to it.reaction) }
    }

    private fun assertNoSubstring(texts: List<Pair<String, String>>, words: List<String>, rule: String) {
        texts.forEach { (path, text) ->
            words.forEach { w ->
                assertTrue("$rule — «$w» в $path «$text»", !text.lowercase().contains(w))
            }
        }
    }

    private fun wholeWord(word: String) = Regex("(?<![\\p{L}\\p{N}_])" + Regex.escape(word) + "(?![\\p{L}\\p{N}_])")

    private fun assertNoWholeWord(texts: List<Pair<String, String>>, words: List<String>, rule: String) {
        texts.forEach { (path, text) ->
            words.forEach { w ->
                assertTrue("$rule — целое слово «$w» в $path «$text»", !wholeWord(w).containsMatchIn(text.lowercase()))
            }
        }
    }

    // ---------- 1. Разбор ----------

    @Test
    fun `town разобран и его списки не пусты`() {
        assertNotNull("content.json разобран без town", content.town)
        val lists = mapOf(
            "places" to town.places, "shops" to town.shops, "items" to town.items,
            "homeItems" to town.homeItems, "goals" to town.goals, "jobs" to town.jobs,
            "residents" to town.residents, "events" to town.events, "stickers" to town.stickers,
            "quiz" to town.quiz, "parentBonusReasons" to town.parentBonusReasons,
        )
        lists.forEach { (name, list) -> assertTrue("town.$name пуст", list.isNotEmpty()) }
    }

    @Test
    fun `в сыром JSON town rules есть все десять полей`() {
        val r = rawTown.getValue("rules").jsonObject
        listOf(
            "daysPerWeek", "shiftsPerWeek", "shiftBonusMax", "shiftScorePerBonus", "changeCoins",
            "jobLevelShifts", "jobLevelBombs", "customGoalFromItemMin", "freeFunMood", "eventsPerDay",
        ).forEach { assertTrue("town.rules: нет поля «$it»", r.containsKey(it)) }
    }

    // ---------- 2. Уникальность id ----------

    @Test
    fun `id товаров уникальны среди всех товаров`() = dup("товары (items и town.items)", allItems.map { it.id })

    @Test
    fun `id записей town уникальны внутри своего списка`() {
        dup("town.places", town.places.map { it.id })
        dup("town.shops", town.shops.map { it.id })
        dup("town.homeItems", town.homeItems.map { it.id })
        dup("town.goals", town.goals.map { it.id })
        dup("town.jobs", town.jobs.map { it.id })
        dup("town.residents", town.residents.map { it.id })
        dup("town.events", town.events.map { it.id })
        dup("town.stickers", town.stickers.map { it.id })
        dup("town.quiz", town.quiz.map { it.id })
    }

    @Test
    fun `товар не повторяется внутри одной лавки`() =
        town.shops.forEach { dup("лавка «${it.id}»", it.sells.map { o -> o.item }) }

    // ---------- 3. Ссылки ----------

    @Test
    fun `ссылки лавок на места и товары`() = town.shops.forEach { s ->
        ref("лавка «${s.id}».place", s.place, placeIds)
        s.sells.forEach { ref("лавка «${s.id}».sells.item", it.item, itemIds) }
    }

    @Test
    fun `ссылки мест на жителей наклейки и цели`() = town.places.forEach { p ->
        ref("место «${p.id}».resident", p.resident, residentIds)
        ref("место «${p.id}».sticker", p.sticker, stickerIds)
        ref("место «${p.id}».opensBy.goal", p.opensBy.goal, goalIds)
    }

    @Test
    fun `ссылки вещей дома целей и работ`() {
        town.homeItems.forEach { ref("вещь «${it.id}».replaceItem", it.replaceItem, itemIds) }
        town.goals.forEach {
            ref("цель «${it.id}».unlocks.place", it.unlocks.place, placeIds)
            ref("цель «${it.id}».unlocks.item", it.unlocks.item, itemIds)
        }
        town.jobs.forEach {
            ref("работа «${it.id}».place", it.place, placeIds)
            ref("работа «${it.id}».resident", it.resident, residentIds)
            ref("работа «${it.id}».opensBy.goal", it.opensBy.goal, goalIds)
        }
    }

    @Test
    fun `ссылки жителей на места вид и цвет`() = town.residents.forEach { r ->
        ref("житель «${r.id}».place", r.place, placeIds)
        ref("житель «${r.id}».look.species", r.look.species, content.species.map { it.id }.toSet())
        ref("житель «${r.id}».look.color", r.look.color, content.colors.map { it.id }.toSet())
    }

    @Test
    fun `ссылки событий на места жителей наклейки и работы`() = town.events.forEach { e ->
        ref("событие «${e.id}».place", e.place, placeIds)
        ref("событие «${e.id}».followUp.place", e.followUp?.place, placeIds)
        ref("событие «${e.id}».resident", e.resident, residentIds)
        ref("событие «${e.id}».sticker", e.sticker, stickerIds)
        ref("событие «${e.id}».params.job", e.params.job, jobIds)
        e.params.list.forEach { ref("событие «${e.id}».params.list", it, itemIds) }
        e.triggers.filterIsInstance<Trigger.Enter>().forEach { ref("событие «${e.id}» ENTER", it.place, placeIds) }
    }

    @Test
    fun `ссылки фактов событий на товары места и вещи дома`() = town.events.forEach { e ->
        factsOf(e).forEach { f ->
            val where = "событие «${e.id}», факт $f"
            when (f) {
                is Fact.Buy -> when (val t = f.target) {
                    is BuyTarget.Item -> ref("$where BUY", t.id, itemIds)
                    is BuyTarget.Tag -> assertTrue(
                        "$where: тега «${t.tag}» нет ни у одного товара, который продаёт лавка",
                        t.tag in soldTags,
                    )
                    is BuyTarget.ByNeed -> Unit
                }
                is Fact.BuyAt -> {
                    ref("$where BUY_AT", f.item, itemIds)
                    val shops = shopsSelling[f.item].orEmpty()
                    assertTrue("$where: товар «${f.item}» продаёт ${shops.size} лавка(и), нужно ≥ 2", shops.size >= 2)
                }
                is Fact.MakeGoal -> ref("$where MAKE_GOAL", f.item, itemIds)
                is Fact.Attend -> ref("$where ATTEND", f.place, placeIds)
                is Fact.Repair -> ref("$where REPAIR", f.item, homeIds)
                else -> Unit
            }
        }
    }

    @Test
    fun `ссылки условий событий на товары жителей и вещи дома`() = town.events.forEach { e ->
        conditionsOf(e).forEach { c ->
            val where = "событие «${e.id}», условие $c"
            when (c) {
                is Condition.Owns -> ref("$where owns", c.item, itemIds)
                is Condition.NotOwned -> ref("$where notOwned", c.item, itemIds)
                is Condition.NotBroken -> ref("$where notBroken", c.item, homeIds)
                is Condition.ResidentArrived -> ref("$where resident", c.id, residentIds)
                else -> Unit
            }
        }
    }

    @Test
    fun `ссылки эффектов событий на товары лавки места и наклейки`() = town.events.forEach { e ->
        effectsOf(e).forEach { (at, eff) ->
            val where = "событие «${e.id}», $at, эффект $eff"
            when (eff) {
                is EventEffect.ItemGive -> ref("$where ITEM_GIVE", eff.item, itemIds)
                is EventEffect.ItemBreak -> ref("$where ITEM_BREAK", eff.item, homeIds)
                is EventEffect.ItemFix -> ref("$where ITEM_FIX", eff.item, homeIds)
                is EventEffect.Sticker -> ref("$where STICKER", eff.id, stickerIds)
                is EventEffect.Goto -> ref("$where GOTO", eff.place, placeIds)
                is EventEffect.Price -> {
                    ref("$where PRICE лавка", eff.shop, shopIds)
                    assertNotNull("$where: лавка «${eff.shop}» не продаёт «${eff.item}»", priceIn(eff.shop, eff.item))
                }
                is EventEffect.Offer -> {
                    ref("$where OFFER лавка", eff.shop, shopIds)
                    assertNotNull("$where: лавка «${eff.shop}» не продаёт «${eff.item}»", priceIn(eff.shop, eff.item))
                }
                else -> Unit
            }
        }
    }

    // ---------- 4. Минимум ТЗ 2.6 ----------

    @Test
    fun `засчитываемых событий не меньше шести и они покрывают все темы`() {
        assertTrue("засчитываемых событий (theme != null) ${countable.size}, нужно ≥ 6", countable.size >= 6)
        assertEquals("темы засчитываемых событий", Theme.entries.toSet(), countable.mapNotNull { it.theme }.toSet())
    }

    @Test
    fun `у каждого засчитываемого события есть верный и ошибочный исход`() = countable.forEach { e ->
        val verdicts = e.outcomes.map { it.verdict }.toSet()
        assertTrue("событие «${e.id}»: нет исхода GOOD", Verdict.GOOD in verdicts)
        assertTrue("событие «${e.id}»: нет исхода MISTAKE", Verdict.MISTAKE in verdicts)
    }

    @Test
    fun `лавки продают не меньше восьми товаров обеих категорий`() {
        assertTrue("лавки town продают ${soldIds.size} разных товаров, нужно ≥ 8", soldIds.size >= 8)
        val categories = allItems.filter { it.id in soldIds }.map { it.category }.toSet()
        assertTrue("среди продаваемых нет MANDATORY", Category.MANDATORY in categories)
        assertTrue("среди продаваемых нет OPTIONAL", Category.OPTIONAL in categories)
    }

    @Test
    fun `целей городка не меньше трёх`() =
        assertTrue("town.goals ${town.goals.size}, нужно ≥ 3", town.goals.size >= 3)

    // ---------- 5. Смешанные цены ----------

    @Test
    fun `среди обязательных товаров есть дешевле на рынке и дешевле у Фомы`() {
        val both = allItems.filter { it.category == Category.MANDATORY }
            .filter { priceIn("shop_market", it.id) != null && priceIn("shop_foma", it.id) != null }
        assertTrue("нет обязательного товара, который продают обе лавки", both.isNotEmpty())
        val cheaperMarket = both.filter { priceIn("shop_market", it.id)!! < priceIn("shop_foma", it.id)!! }
        val cheaperFoma = both.filter { priceIn("shop_foma", it.id)!! < priceIn("shop_market", it.id)!! }
        val prices = both.map { "${it.id}: market ${priceIn("shop_market", it.id)}, foma ${priceIn("shop_foma", it.id)}" }
        assertTrue("нет обязательного товара дешевле на рынке — $prices", cheaperMarket.isNotEmpty())
        assertTrue("нет обязательного товара дешевле у Фомы — $prices", cheaperFoma.isNotEmpty())
    }

    // ---------- 6–9. Тексты ----------

    @Test
    fun `сбор текстов town охватывает вступления исходы подсказки и вопросы`() {
        val values = townTexts.map { it.second }.toSet()
        listOf(
            town.events.first().intro,
            town.events.first { it.outcomes.isNotEmpty() }.outcomes.first().line,
            town.stickers.first().hint,
            town.residents.first { it.lines.isNotEmpty() }.lines.first(),
            town.quiz.first().question,
            town.quiz.first().explanation,
            town.quiz.first().options.first(),
            town.parentBonusReasons.first(),
            town.events.first { it.demo.wrongPath.isNotEmpty() }.demo.wrongPath,
            town.jobs.first { it.tasks.isNotEmpty() }.tasks.first(),
        ).forEach { assertTrue("текст «$it» не попал в выборку текстов town", it in values) }
        assertTrue("в выборку текстов town попал id", town.events.first().id !in values)
        assertTrue("в выборку текстов town попала строка словаря", "SKIP" !in values)
    }

    @Test
    fun `тексты town без спешки и срочности`() = assertNoSubstring(
        townTexts,
        listOf("сегодня", "осталось", "до конца недели", "скорее", "пока не", "успей", "последн"),
        "правило 3 (без срочности)",
    )

    @Test
    fun `тексты town без займов`() =
        assertNoSubstring(townTexts, listOf("долг", "одолж", "взаймы", "занять"), "правило 7 (без займов)")

    @Test
    fun `тексты town не стыдят`() = assertNoSubstring(
        townTexts,
        listOf("цена лени", "ленив", "зачем", "транжир", "жадин", "зря", "впуст", "провал", "плохо", "неправильно", "ошибк"),
        "правило 10 (не стыдим)",
    )

    @Test
    fun `строки ошибочных исходов не упрекают прошлым`() {
        val lines = town.events.flatMap { e ->
            e.outcomes.filter { it.verdict == Verdict.MISTAKE }.map { "событие «${e.id}» исход" to it.line }
        }
        assertNoSubstring(lines, listOf("знали", "надо было"), "правило 10 (MISTAKE без упрёка)")
    }

    private val genderedWords = listOf(
        "отложил", "планировал", "потратил", "положил", "взял", "проголодался", "запачкался",
        "пришёл", "пришла", "захотел", "захотела", "купил", "купила", "выбрал", "выбрала",
        "бегал", "подвернул", "достал", "видел", "видела", "доволен", "довольна", "устроил", "чистенький",
    )

    @Test
    fun `тексты town без рода ребёнка и питомца`() =
        assertNoWholeWord(townTexts, genderedWords, "правило 9 (без рода)")

    @Test
    fun `описания товаров лавок town без рода`() {
        assertTrue("лавки town не продают ни одного товара верхнего уровня", soldTopItemTexts.isNotEmpty())
        assertNoWholeWord(soldTopItemTexts, genderedWords, "правило 9 (без рода, товары лавок town)")
    }

    // ---------- 10. Длина ----------

    @Test
    fun `строка исхода не длиннее 80 символов`() = town.events.forEach { e ->
        e.outcomes.forEach {
            assertTrue("событие «${e.id}»: строка ${it.line.length} символов «${it.line}»", it.line.length <= 80)
        }
    }

    @Test
    fun `вступление не длиннее 120 символов`() = town.events.forEach { e ->
        assertTrue("событие «${e.id}»: intro ${e.intro.length} символов «${e.intro}»", e.intro.length <= 120)
    }

    // ---------- 11. Ломается только стартовая вещь с ценой починки ----------

    @Test
    fun `ITEM_BREAK только над стартовой вещью с ценой починки`() = town.events.forEach { e ->
        effectsOf(e).map { it.second }.filterIsInstance<EventEffect.ItemBreak>().forEach { br ->
            val h = town.homeItems.firstOrNull { it.id == br.item }
            assertNotNull("событие «${e.id}»: ITEM_BREAK(${br.item}) — такой вещи дома нет", h)
            assertNotNull("событие «${e.id}»: у вещи «${br.item}» нет repairPrice", h!!.repairPrice)
            assertTrue("событие «${e.id}»: вещь «${br.item}» не стартовая (starter == false)", h.starter)
        }
    }

    // ---------- 12. Монеты извне ----------

    @Test
    fun `COINS WINDFALL только в setup события WINDFALL`() = town.events.forEach { e ->
        effectsOf(e).forEach { (at, eff) ->
            if (eff is EventEffect.Coins && eff.source == CoinSource.WINDFALL) {
                assertEquals("событие «${e.id}»: COINS(WINDFALL) записан в $at", "setup", at)
                assertEquals("событие «${e.id}»: COINS(WINDFALL) не в событии kind WINDFALL", EventKind.WINDFALL, e.kind)
            }
        }
    }

    @Test
    fun `COINS CHANGE_RETURN только в исходе события со сдачей и не больше недостачи`() = town.events.forEach { e ->
        val shortChange = e.setup.filterIsInstance<EventEffect.ShortChange>().map { it.n }
        effectsOf(e).forEach { (at, eff) ->
            if (eff is EventEffect.Coins && eff.source == CoinSource.CHANGE_RETURN) {
                assertTrue("событие «${e.id}»: COINS(CHANGE_RETURN) записан в $at, а не в effects исхода", at.startsWith("исход"))
                assertTrue("событие «${e.id}»: COINS(CHANGE_RETURN) без SHORT_CHANGE в setup", shortChange.isNotEmpty())
                assertTrue(
                    "событие «${e.id}»: возврат ${eff.n} больше недостачи $shortChange",
                    shortChange.any { eff.n <= it },
                )
            }
        }
    }

    @Test
    fun `WINDFALL и MISHAP не возвращаются в пул`() =
        town.events.filter { it.kind == EventKind.WINDFALL || it.kind == EventKind.MISHAP }.forEach {
            assertTrue("событие «${it.id}» (${it.kind}): repeat.pool == true", !it.repeat.pool)
        }

    // ---------- 13. Факт не бывает и верным, и ошибочным ----------

    @Test
    fun `факт не встречается и в верном и в ошибочном исходе одного события`() = countable.forEach { e ->
        val good = e.outcomes.filter { it.verdict == Verdict.GOOD }.map { it.fact }.toSet()
        val mistake = e.outcomes.filter { it.verdict == Verdict.MISTAKE }.map { it.fact }.toSet()
        val both = good intersect mistake
        assertTrue("событие «${e.id}»: факты $both стоят и в GOOD, и в MISTAKE", both.isEmpty())
    }

    // ---------- 14. Восстановление ----------

    @Test
    fun `у каждого ошибочного исхода есть путь восстановления`() = town.events.forEach { e ->
        e.outcomes.filter { it.verdict == Verdict.MISTAKE }.forEach {
            assertTrue("событие «${e.id}», исход «${it.line}»: пустой recovery", it.recovery.isNotEmpty())
        }
    }

    @Test
    fun `событие со сдачей возвращает недостачу и предлагает вернуться`() {
        val withShort = town.events.filter { e -> e.setup.any { it is EventEffect.ShortChange } }
        assertTrue("нет ни одного события с SHORT_CHANGE в setup", withShort.isNotEmpty())
        withShort.forEach { e ->
            val m = e.setup.filterIsInstance<EventEffect.ShortChange>().first().n
            val returned = e.outcomes.any { o -> o.effects.contains(EventEffect.Coins(m, CoinSource.CHANGE_RETURN)) }
            assertTrue("событие «${e.id}»: нет исхода с COINS($m, CHANGE_RETURN)", returned)
            e.outcomes.filter { it.verdict == Verdict.MISTAKE }.forEach {
                assertTrue(
                    "событие «${e.id}», исход «${it.line}»: в recovery нет RETURN_LATER",
                    Recovery.ReturnLater in it.recovery,
                )
            }
        }
    }

    // ---------- 15. Пул ----------

    @Test
    fun `засчитываемые события кроме MISHAP возвращаются в пул`() =
        countable.filter { it.kind != EventKind.MISHAP }.forEach {
            assertTrue("событие «${it.id}»: repeat.pool == false", it.repeat.pool)
        }

    // ---------- 16. MISHAP ----------

    private val mishaps: List<EventDef> get() = town.events.filter { it.kind == EventKind.MISHAP }
    private fun arrivals(e: EventDef) = listOf(e.arrives.week) + e.repeat.weeks

    @Test
    fun `приходов MISHAP не больше двух по возрастанию с разрывом не меньше четырёх`() {
        assertTrue("в контенте нет ни одного события kind MISHAP", mishaps.isNotEmpty())
        mishaps.forEach { e ->
            val w = arrivals(e)
            assertTrue("событие «${e.id}»: приходов ${w.size} ($w), можно не больше 2", w.size <= 2)
            assertEquals("событие «${e.id}»: приходы $w не по возрастанию", w.sorted(), w)
            w.zipWithNext().forEach { (a, b) ->
                assertTrue("событие «${e.id}»: разрыв между приходами $a и $b меньше 4", b - a >= 4)
            }
        }
    }

    @Test
    fun `приходы MISHAP не совпадают с поломкой и подорожанием`() {
        val busy = town.events.filter { e ->
            e.kind == EventKind.BREAK || e.setup.any { it is EventEffect.Price }
        }.associate { it.id to it.arrives.week }
        mishaps.forEach { e ->
            arrivals(e).forEach { w ->
                val clash = busy.filterValues { it == w }.keys
                assertTrue("событие «${e.id}»: приход на неделе $w совпадает с $clash", clash.isEmpty())
            }
        }
    }

    @Test
    fun `условия MISHAP не зависят от действий ребёнка и умолчания нет`() = mishaps.forEach { e ->
        e.requires.flatMap { flatten(it) }.forEach { c ->
            val allowed = c is Condition.ResidentArrived || c is Condition.WeekAtLeast ||
                c is Condition.DayIs || c is Condition.AfterPlan
            assertTrue("событие «${e.id}»: условие $c зависит от действий ребёнка", allowed)
        }
        assertNull("событие «${e.id}»: у MISHAP есть default", e.default)
    }

    @Test
    fun `тексты MISHAP не называют вещи которые остаются и места за мечту`() {
        val keepTitles = allItems.filter { it.keep }.map { it.title }
        val goalPlaceTitles = town.places.filter { it.opensBy.goal != null }.map { it.title }
        assertTrue("нет ни одного товара с keep == true", keepTitles.isNotEmpty())
        assertTrue("нет ни одного места с opensBy.goal", goalPlaceTitles.isNotEmpty())
        mishaps.forEach { e ->
            (listOf("intro" to e.intro) + e.outcomes.map { "исход" to it.line }).forEach { (what, text) ->
                (keepTitles + goalPlaceTitles).forEach { title ->
                    assertTrue(
                        "событие «${e.id}», $what «$text» называет «$title»",
                        !text.lowercase().contains(title.lowercase()),
                    )
                }
            }
        }
    }

    // ---------- 17. Вещи ----------

    @Test
    fun `ломающаяся вещь с ценой починки имеет замену`() {
        val broken = town.events.flatMap { e -> effectsOf(e).map { it.second } }
            .filterIsInstance<EventEffect.ItemBreak>().map { it.item }.toSet()
        assertTrue("ни одно событие не ломает вещь дома", broken.isNotEmpty())
        broken.mapNotNull { id -> town.homeItems.firstOrNull { it.id == id } }
            .filter { it.repairPrice != null }
            .forEach { assertNotNull("вещь «${it.id}»: есть repairPrice, но нет replaceItem", it.replaceItem) }
    }

    @Test
    fun `товар только из событий продаётся лишь как замена вещи дома`() {
        val replacements = town.homeItems.mapNotNull { it.replaceItem }.toSet()
        val eventOnly = allItems.filter { it.eventOnly }
        assertTrue("нет ни одного товара eventOnly", eventOnly.isNotEmpty())
        eventOnly.filter { it.id in soldIds }.forEach {
            assertTrue(
                "товар «${it.id}» eventOnly, его продаёт ${shopsSelling[it.id]?.map { s -> s.id }}, но он не replaceItem вещи дома",
                it.id in replacements,
            )
        }
    }

    @Test
    fun `бантик не продаётся ни в одной лавке town`() =
        assertTrue("fun_bow продают лавки ${shopsSelling["fun_bow"]?.map { it.id }}", "fun_bow" !in soldIds)

    @Test
    fun `вещи которые остаются в доме помечены keep`() {
        listOf("fun_ball", "fun_book", "fun_tent").forEach { id ->
            val i = itemOrNull(id)
            assertNotNull("нет товара «$id»", i)
            assertTrue("товар «$id»: keep == false", i!!.keep)
        }
        town.items.filter { it.slot != null }.forEach {
            assertTrue("товар «${it.id}»: slot «${it.slot}», но keep == false", it.keep)
        }
    }

    // ---------- 18. Ванна с пеной ----------

    @Test
    fun `care_vitamins это ванна с пеной без здоровья`() {
        val i = content.item("care_vitamins")
        assertEquals("care_vitamins.title", "Ванна с пеной", i.title)
        assertEquals("care_vitamins.emoji", "🛁", i.emoji)
        assertEquals("care_vitamins.price", 25, i.price)
        assertEquals("care_vitamins.hunger", 0, i.hunger)
        assertEquals("care_vitamins.clean", 20, i.clean)
        assertEquals("care_vitamins.mood", 5, i.mood)
        listOf("здоров", "бодр", "витамин").forEach { w ->
            assertTrue("care_vitamins.description содержит «$w»", !i.description.lowercase().contains(w))
            assertTrue("care_vitamins.reaction содержит «$w»", !i.reaction.lowercase().contains(w))
        }
    }

    // ---------- 19. Работы ----------

    @Test
    fun `работа MATCH3 имеет поле шесть на шесть и ходы`() {
        val m3 = town.jobs.filter { it.game == JobGame.MATCH3 }
        assertTrue("нет ни одной работы MATCH3", m3.isNotEmpty())
        m3.forEach {
            assertEquals("работа «${it.id}»: board", Board(6, 6), it.board)
            assertNotNull("работа «${it.id}»: нет moves", it.moves)
            assertNotNull("работа «${it.id}»: нет demoMoves", it.demoMoves)
        }
    }

    @Test
    fun `работа TAPS имеет ровно три задания`() {
        val taps = town.jobs.filter { it.game == JobGame.TAPS }
        assertTrue("нет ни одной работы TAPS", taps.isNotEmpty())
        taps.forEach { assertEquals("работа «${it.id}»: заданий", 3, it.tasks.size) }
    }

    @Test
    fun `уровни работы совпадают с jobLevelShifts`() = town.jobs.forEach {
        assertEquals(
            "работа «${it.id}»: baseByLevel ${it.baseByLevel} против jobLevelShifts ${town.rules.jobLevelShifts}",
            town.rules.jobLevelShifts.size, it.baseByLevel.size,
        )
    }

    // ---------- 20. Вопросы ----------

    @Test
    fun `вопросов town пятнадцать и у каждого верный ответ и объяснение`() {
        assertEquals("town.quiz", 15, town.quiz.size)
        town.quiz.forEach {
            assertTrue("вопрос «${it.id}»: correct ${it.correct} вне options ${it.options.size}", it.correct in it.options.indices)
            assertTrue("вопрос «${it.id}»: пустое explanation", it.explanation.isNotBlank())
        }
    }

    @Test
    fun `вопрос про нехватку монет не предлагает займ`() {
        val q = town.quiz.firstOrNull { it.id == "q_shop_3" }
        assertNotNull("нет вопроса q_shop_3", q)
        val texts = (listOf(q!!.question, q.explanation) + q.options).map { "q_shop_3" to it }
        assertNoSubstring(texts, listOf("долг", "одолж", "взаймы", "занять"), "правило 7 (q_shop_3)")
    }

    // ---------- 21. Причины бонуса ----------

    @Test
    fun `причин бонуса взрослого в town ровно три`() =
        assertEquals("town.parentBonusReasons ${town.parentBonusReasons}", 3, town.parentBonusReasons.size)

    // ---------- 22. Демо ----------

    @Test
    fun `у засчитываемого события есть неверный путь для демо`() = countable.forEach {
        assertTrue("событие «${it.id}»: пустой demo.wrongPath", it.demo.wrongPath.isNotBlank())
    }

    // ---------- 23. Места для вещей и «где» лавок (TOWN-S1a §0, ORACLE) ----------

    @Test
    fun `мест для вещей шесть и их id уникальны`() {
        assertEquals("town.spots ${town.spots.map { it.id }}", 6, town.spots.size)
        dup("town.spots", town.spots.map { it.id })
    }

    @Test
    fun `у каждой вещи которая остаётся есть место такого типа`() {
        val slots = town.spots.map { it.slot }.toSet()
        val keep = allItems.filter { it.keep }
        assertTrue("нет ни одного товара keep", keep.isNotEmpty())
        keep.forEach { item ->
            assertNotNull("товар «${item.id}»: keep, но slot не задан", item.slot)
            assertTrue(
                "товар «${item.id}»: slot «${item.slot}», а места есть только типов $slots",
                slots.any { it == item.slot },
            )
        }
    }

    @Test
    fun `стартовая вещь на месте ссылается на существующее место`() {
        val spotIds = town.spots.map { it.id }.toSet()
        val placed = town.homeItems.filter { it.spot != null }
        assertTrue("ни одна стартовая вещь не стоит на месте", placed.isNotEmpty())
        placed.forEach { ref("вещь дома «${it.id}».spot", it.spot, spotIds) }
    }

    @Test
    fun `у каждой лавки сказано где она находится`() = town.shops.forEach {
        assertTrue("лавка «${it.id}»: пустое поле at", it.at.isNotBlank())
    }
}
