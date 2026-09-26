package ru.finny.pet.domain.town

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.pet.domain.Category
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.Goal
import ru.finny.pet.domain.Need
import ru.finny.pet.domain.Purchase
import ru.finny.pet.domain.TestContent
import ru.finny.pet.domain.ok

/**
 * Оракул §3 спеки TOWN-S1a: касса «Городка» — предпросмотр (quote) и оплата (buyAt).
 * Состояния строятся путём игрока: newGame → createPet → setPlan → Town.confirmPlan → покупки.
 * Руками (copy) задаются только граничные банки и поля, которых движок среза 1 ещё не меняет
 * (asleep, owned) — каждый такой случай отмечен в тесте.
 */
class CheckoutTest {

    private val content = TestContent.content
    private val economy = TestContent.economy
    private val town = Town(content)
    private val prices = Prices(content)
    private val townContent = content.town ?: error("content.json: нет ключа town")

    private val stamp = "Штамп «По плану» на этой неделе не получится"
    private val reserveWhy = "Еда и уход нужны каждую неделю. Запас — для сюрпризов"

    // ---------- вспомогательное ----------

    /** Неделя 1, план разложен и подтверждён кассой; карманные — 100. */
    private fun ready(m: Int, o: Int, sv: Int, demo: Boolean = true): GameState {
        var s = economy.newGame(demo)
        s = economy.createPet(s, "Финни", "cat", "orange").ok()
        s = economy.setPlan(s, m, o, sv).ok()
        return town.confirmPlan(s).done().state
    }

    private fun TownResult.done(): TownOutcome =
        (this as? TownResult.Done)?.outcome ?: error("ожидался Done, получен $this")

    private fun TownResult.refusal(): String =
        (this as? TownResult.Refused)?.line ?: error("ожидался Refused, получен $this")

    private fun GameState.buy(itemId: String, shopId: String?, source: Source): GameState =
        town.buyAt(this, itemId, shopId, source).done().state

    private fun Quote.pay(source: Source): PayOption? =
        options.firstOrNull { it.kind == PayKind.PAY && it.source == source }

    private fun Quote.kinds(): List<Pair<PayKind, Source?>> = options.map { it.kind to it.source }

    private fun assertPay(
        where: String,
        option: PayOption?,
        label: String,
        preview: List<String>,
        more: Boolean,
    ) {
        assertNotNull("$where: такого варианта оплаты нет", option)
        assertEquals("$where: подпись", label, option!!.label)
        assertEquals("$where: предпросмотр", preview, option.preview)
        assertEquals("$where: под «Ещё»", more, option.more)
    }

    /** Правило Economy.endPeriod, посчитанное оракулом на любом состоянии (PLANKEPT спеки). */
    private fun planKept(s: GameState): Boolean =
        s.plan.confirmed && s.factMandatory <= s.plan.mandatory &&
            s.factOptional <= s.plan.optional && maxOf(s.factSavings, 0) >= s.plan.savings

    // ---------- отказы 1–6 ----------

    @Test
    fun `без питомца касса не работает`() {
        val s = economy.newGame(true)
        assertEquals(
            "отказ оплаты",
            "Сначала создай питомца",
            town.buyAt(s, "food_basic", "shop_market", Source.NEED).refusal(),
        )
        val q = town.quote(s, "food_basic", "shop_market")
        assertEquals("строка предпросмотра", "Сначала создай питомца", q.line)
        assertEquals("вариантов нет", emptyList<PayOption>(), q.options)
        assertEquals("цена всё равно видна", 20, q.price)
    }

    @Test
    fun `ночью покупок нет`() {
        // asleep ставит Town.sleep (§5); здесь он задан руками — сон не предмет этого файла.
        val s = ready(40, 20, 30).copy(asleep = true)
        assertEquals(
            "отказ оплаты",
            "Сейчас ночь — сначала проснёмся",
            town.buyAt(s, "food_basic", "shop_market", Source.NEED).refusal(),
        )
        assertEquals("строка предпросмотра", "Сейчас ночь — сначала проснёмся", town.quote(s, "food_basic", "shop_market").line)
    }

    @Test
    fun `до раскладки монет покупок нет`() {
        var s = economy.newGame(true)
        s = economy.createPet(s, "Финни", "cat", "orange").ok()
        val r = town.buyAt(s, "food_basic", "shop_market", Source.NEED)
        assertEquals("отказ оплаты", "Покупки — после того как разложишь монеты", r.refusal())
        val q = town.quote(s, "food_basic", "shop_market")
        assertEquals("строка предпросмотра", "Покупки — после того как разложишь монеты", q.line)
        assertEquals("вариантов нет", emptyList<PayOption>(), q.options)
    }

    @Test
    fun `неизвестный товар и товар не из этой лавки не продаются`() {
        val s = ready(40, 20, 30)
        assertEquals("нет в контенте", "Этого товара здесь нет", town.buyAt(s, "no_such_item", "shop_market", Source.NEED).refusal())
        assertEquals("цена неизвестного товара", 0, town.quote(s, "no_such_item", "shop_market").price)
        assertEquals("рынок не продаёт обед", "Этого товара здесь нет", town.buyAt(s, "food_lunch", "shop_market", Source.NEED).refusal())
        assertEquals("цена чужого товара", 0, town.quote(s, "food_lunch", "shop_market").price)
    }

    @Test
    fun `товар только из событий на полке не продаётся а в сцене продаётся`() {
        val s = ready(40, 20, 30)
        assertEquals("лампа на полке", "Этого товара здесь нет", town.buyAt(s, "lamp_new", "shop_workshop", Source.RESERVE).refusal())
        assertEquals("цена лампы известна", 25, town.quote(s, "lamp_new", "shop_workshop").price)
        assertEquals("корм в сцене", "Этого товара здесь нет", town.buyAt(s, "food_basic", null, Source.NEED).refusal())
        assertTrue("доктор в сцене оплачивается", town.buyAt(s, "care_doctor", null, Source.RESERVE) is TownResult.Done)
    }

    @Test
    fun `вещь которая уже дома второй раз не продаётся`() {
        // owned наполняют покупки; здесь сундук задан руками, чтобы проверить только отказ 5.
        val s = ready(40, 40, 20, demo = false).copy(owned = listOf("fun_ball"))
        val r = town.buyAt(s, "fun_ball", "shop_foma", Source.WANT)
        assertEquals("отказ", "Эта вещь уже есть дома", r.refusal())
        assertEquals("строка предпросмотра", "Эта вещь уже есть дома", town.quote(s, "fun_ball", "shop_foma").line)
    }

    @Test
    fun `в демо вещь показа продаётся снова и не двоится в сундуке`() {
        val demo = ready(10, 60, 30).copy(owned = listOf("fun_robot"))
        val after = demo.buy("fun_robot", "shop_foma", Source.WANT)
        assertEquals("сундук без дублей", listOf("fun_robot"), after.owned)
        assertEquals("«Хочу» после покупки", 20, after.jarWant)
        val usual = ready(10, 60, 30, demo = false).copy(owned = listOf("fun_robot"))
        assertEquals("вне демо — отказ", "Эта вещь уже есть дома", town.buyAt(usual, "fun_robot", "shop_foma", Source.WANT).refusal())
        val ball = ready(10, 60, 30).copy(owned = listOf("fun_ball"))
        assertEquals("мячик и в демо уже дома", "Эта вещь уже есть дома", town.buyAt(ball, "fun_ball", "shop_foma", Source.WANT).refusal())
    }

    @Test
    fun `товар который ещё не появился в лавке купить нельзя а в демо можно`() {
        val usual = ready(40, 40, 20, demo = false)
        assertEquals("змей на первой неделе", "Этого товара здесь нет", town.buyAt(usual, "fun_kite", "shop_foma", Source.WANT).refusal())
        val demo = ready(40, 40, 20)
        val after = demo.buy("fun_kite", "shop_foma", Source.WANT)
        assertEquals("в демо змей куплен", 10, after.jarWant)
    }

    // ---------- отказы 7 и 8 ----------

    @Test
    fun `хотелку из запаса оплатить нельзя`() {
        val s = ready(40, 20, 30)
        val r = town.buyAt(s, "fun_balloon", "shop_market", Source.RESERVE)
        assertEquals(
            "отказ",
            "Запас — на нужное и на всякий случай. Хотелки — из банки «Хочу»",
            r.refusal(),
        )
    }

    @Test
    fun `прочие пары банки и товара не разрешены`() {
        val s = ready(40, 20, 30)
        val pairs = listOf(
            Triple("food_basic", "shop_market", Source.WANT),
            Triple("food_basic", "shop_market", Source.TRANSFER_NEED),
            Triple("fun_balloon", "shop_market", Source.NEED),
            Triple("fun_balloon", "shop_market", Source.TRANSFER_WANT),
            Triple("care_doctor", null, Source.NEED),
            Triple("care_doctor", null, Source.TRANSFER_NEED),
            Triple("care_doctor", null, Source.TRANSFER_WANT),
        )
        pairs.forEach { (item, shop, source) ->
            assertEquals("«$item» из $source", "Так оплатить нельзя", town.buyAt(s, item, shop, source).refusal())
        }
    }

    @Test
    fun `невыполнимая оплата из родной банки объясняет чего не хватает`() {
        val need = ready(10, 40, 30)
        assertEquals(
            "обязательное из «Нужного»",
            "В «Нужном» 10, корм стоит 20. Не хватает 10",
            town.buyAt(need, "food_basic", "shop_market", Source.NEED).refusal(),
        )
        val want = ready(40, 5, 30)
        assertEquals(
            "хотелка из «Хочу»",
            "В «Хочу» 5, мячик стоит 25. Не хватает 20",
            town.buyAt(want, "fun_ball", "shop_foma", Source.WANT).refusal(),
        )
        val unplanned = ready(88, 3, 5)
        assertEquals(
            "непредвиденное из запаса",
            "Запас 4, приём доктора стоит 10. Не хватает 6",
            town.buyAt(unplanned, "care_doctor", null, Source.RESERVE).refusal(),
        )
    }

    @Test
    fun `невыполнимая оплата из копилки называет её остаток`() {
        val s = ready(40, 20, 30)
        assertEquals("обязательное", "В копилке только 30", town.buyAt(s, "food_lunch", "shop_foma", Source.SAVINGS).refusal())
        assertEquals("хотелка", "В копилке только 30", town.buyAt(s, "fun_tent", "shop_foma", Source.SAVINGS).refusal())
        val poor = ready(88, 3, 5)
        assertEquals("непредвиденное", "В копилке только 5", town.buyAt(poor, "care_doctor", null, Source.SAVINGS).refusal())
    }

    @Test
    fun `невыполнимая оплата из чужой банки Хочу называет её остаток`() {
        val s = ready(88, 3, 5)
        assertEquals("непредвиденное из «Хочу»", "В «Хочу» только 3", town.buyAt(s, "care_doctor", null, Source.WANT).refusal())
    }

    @Test
    fun `невыполнимый перенос и доплата отказывают общей строкой`() {
        val noReserve = ready(10, 20, 70)
        assertEquals("доплата без запаса", "Так оплатить нельзя", town.buyAt(noReserve, "food_basic", "shop_market", Source.RESERVE).refusal())
        val noWant = ready(10, 5, 30)
        assertEquals("перенос из пустого «Хочу»", "Так оплатить нельзя", town.buyAt(noWant, "food_basic", "shop_market", Source.TRANSFER_WANT).refusal())
        val noNeed = ready(5, 5, 30)
        assertEquals("перенос из пустого «Нужного»", "Так оплатить нельзя", town.buyAt(noNeed, "fun_ball", "shop_foma", Source.TRANSFER_NEED).refusal())
    }

    // ---------- оплата: банки, журнал, дневник ----------

    @Test
    fun `обязательное из Нужного списывает цену лавки`() {
        val s = ready(40, 20, 30)
        val o = town.buyAt(s, "food_basic", "shop_market", Source.NEED).done()
        val a = o.state
        assertEquals("кошелёк", 50, a.balance)
        assertEquals("«Нужное»", 20, a.jarNeed)
        assertEquals("«Хочу»", 20, a.jarWant)
        assertEquals("запас", 10, a.reserve)
        assertEquals("копилка", 30, a.savings)
        assertEquals(
            "покупка",
            Purchase("food_basic", "Корм", Category.MANDATORY, Need.FOOD, 20, "shop_market", Source.NEED),
            a.purchases.last(),
        )
        assertEquals("строка", "Финни с удовольствием хрустит кормом!", o.line)
        assertEquals("почему", listOf("Из «Нужного»: 40 → 20"), o.why)
        assertEquals("дневник", listOf(DiaryLine(1, 1, "Купили у реки: корм")), a.diary)
        assertEquals("сундук пуст", emptyList<String>(), a.owned)
        assertEquals("эффектов нет", emptyList<EventEffect>(), o.effects)
        assertEquals("исходов событий нет", emptyList<EventResult>(), o.eventResults)
        assertEquals("журнал сходится с кошельком", a.balance, a.ledger.sumOf { it.amount })
    }

    @Test
    fun `один и тот же корм в двух лавках стоит по-разному`() {
        val s = ready(40, 20, 30)
        val market = s.buy("food_basic", "shop_market", Source.NEED)
        val foma = s.buy("food_basic", "shop_foma", Source.NEED)
        assertEquals("кошелёк после рынка", 50, market.balance)
        assertEquals("кошелёк после Фомы", 40, foma.balance)
        assertEquals("«Нужное» после рынка", 20, market.jarNeed)
        assertEquals("«Нужное» после Фомы", 10, foma.jarNeed)
        assertEquals("цена покупки на рынке", 20, market.purchases.last().price)
        assertEquals("цена покупки у Фомы", 30, foma.purchases.last().price)
        assertEquals("дневник рынка", "Купили у реки: корм", market.diary.last().text)
        assertEquals("дневник Фомы", "Купили у Фомы: корм", foma.diary.last().text)
    }

    @Test
    fun `дневник называет товар с маленькой буквы`() {
        val s = ready(40, 20, 30)
        assertEquals("каша", "Купили у реки: каша", s.buy("food_porridge", "shop_market", Source.NEED).diary.last().text)
        assertEquals("мыло", "Купили у Фомы: мыло", s.buy("care_soap", "shop_foma", Source.NEED).diary.last().text)
        assertEquals("ванна с пеной", "Купили у Фомы: ванна с пеной", s.buy("care_vitamins", "shop_foma", Source.NEED).diary.last().text)
    }

    @Test
    fun `хотелка из Хочу запас не трогает`() {
        val s = ready(40, 20, 30)
        val o = town.buyAt(s, "fun_balloon", "shop_market", Source.WANT).done()
        val a = o.state
        assertEquals("кошелёк", 62, a.balance)
        assertEquals("«Хочу»", 12, a.jarWant)
        assertEquals("«Нужное»", 40, a.jarNeed)
        assertEquals("запас", 10, a.reserve)
        assertEquals("почему", listOf("Из «Хочу»: 20 → 12"), o.why)
        assertEquals("строка", "Финни подпрыгивает за шариком.", o.line)
        assertEquals("дневник", "Купили у реки: шарик", a.diary.last().text)
    }

    @Test
    fun `первая доплата из запаса обнуляет Нужное и хвалит запас`() {
        val s = ready(10, 40, 30)
        val o = town.buyAt(s, "food_basic", "shop_market", Source.RESERVE).done()
        val a = o.state
        assertEquals("кошелёк", 50, a.balance)
        assertEquals("«Нужное»", 0, a.jarNeed)
        assertEquals("«Хочу»", 40, a.jarWant)
        assertEquals("запас", 10, a.reserve)
        assertEquals("строка", "Хорошо, что был запас! В новом плане дадим «Нужному» побольше?", o.line)
        assertEquals(
            "почему",
            listOf("Из «Нужного»: 10 → 0", "Из запаса: 20 → 10", reserveWhy),
            o.why,
        )
    }

    @Test
    fun `вторая доплата из запаса за неделю говорит короче и опускает строки без движения`() {
        val first = ready(10, 40, 30).buy("food_basic", "shop_market", Source.RESERVE)
        val o = town.buyAt(first, "care_soap", "shop_foma", Source.RESERVE).done()
        val a = o.state
        assertEquals("кошелёк", 40, a.balance)
        assertEquals("«Нужное»", 0, a.jarNeed)
        assertEquals("«Хочу»", 40, a.jarWant)
        assertEquals("запас", 0, a.reserve)
        assertEquals("строка", "Запас выручил", o.line)
        assertEquals("почему без строк «0 → 0»", listOf("Из запаса: 10 → 0", reserveWhy), o.why)
    }

    @Test
    fun `составная доплата берёт остаток из Хочу и оставляет три строки почему`() {
        val s = ready(10, 60, 20)
        val o = town.buyAt(s, "food_lunch", "shop_foma", Source.RESERVE).done()
        val a = o.state
        assertEquals("кошелёк", 35, a.balance)
        assertEquals("«Нужное»", 0, a.jarNeed)
        assertEquals("«Хочу»", 35, a.jarWant)
        assertEquals("запас", 0, a.reserve)
        assertEquals("строка", "Хорошо, что был запас! В новом плане дадим «Нужному» побольше?", o.line)
        assertEquals(
            "почему — три строки движения монет",
            listOf("Из «Нужного»: 10 → 0", "Из запаса: 10 → 0", "Из «Хочу»: 60 → 35"),
            o.why,
        )
    }

    @Test
    fun `запас выручил когда траты остались в плане`() {
        val s = ready(50, 5, 30)
        val first = town.buyAt(s, "fun_ball", "shop_foma", Source.TRANSFER_NEED).done()
        val mid = first.state
        assertEquals("«Нужное» после переноса", 30, mid.jarNeed)
        assertEquals("«Хочу» после переноса", 0, mid.jarWant)
        assertEquals("кошелёк после переноса", 45, mid.balance)
        assertEquals("запас после переноса", 15, mid.reserve)
        assertEquals("почему переноса", listOf("Из «Нужного»: 50 → 30", "Из «Хочу»: 5 → 0"), first.why)

        val o = town.buyAt(mid, "food_lunch", "shop_foma", Source.RESERVE).done()
        val a = o.state
        assertEquals("кошелёк потрачен ровно весь", 0, a.balance)
        assertEquals("«Нужное»", 0, a.jarNeed)
        assertEquals("запас", 0, a.reserve)
        assertEquals("строка без просьбы увеличить «Нужное»", "Запас выручил", o.line)
        assertEquals(
            "почему",
            listOf("Из «Нужного»: 30 → 0", "Из запаса: 15 → 0", reserveWhy),
            o.why,
        )
    }

    @Test
    fun `перенос из Хочу закрывает Нужное`() {
        val s = ready(10, 40, 30)
        val o = town.buyAt(s, "food_basic", "shop_market", Source.TRANSFER_WANT).done()
        val a = o.state
        assertEquals("кошелёк", 50, a.balance)
        assertEquals("«Нужное»", 0, a.jarNeed)
        assertEquals("«Хочу»", 30, a.jarWant)
        assertEquals("запас не тронут", 20, a.reserve)
        assertEquals("почему", listOf("Из «Хочу»: 40 → 30", "Из «Нужного»: 10 → 0"), o.why)
    }

    @Test
    fun `оплата из копилки не трогает банки и пишет в журнал две строки`() {
        val s = ready(10, 40, 30)
        val o = town.buyAt(s, "food_basic", "shop_market", Source.SAVINGS).done()
        val a = o.state
        assertEquals("кошелёк не меняется", 70, a.balance)
        assertEquals("«Нужное»", 10, a.jarNeed)
        assertEquals("«Хочу»", 40, a.jarWant)
        assertEquals("запас", 20, a.reserve)
        assertEquals("копилка", 10, a.savings)
        assertEquals("снято за неделю", 20, a.withdrawnThisPeriod)
        assertEquals("почему", listOf("Из копилки: 30 → 10"), o.why)
        assertEquals("в журнал добавлены две строки", s.ledger.size + 2, a.ledger.size)
        assertEquals("суммы новых строк", listOf(20, -20), a.ledger.takeLast(2).map { it.amount })
        assertEquals("журнал сходится с кошельком", a.balance, a.ledger.sumOf { it.amount })
        assertEquals("источник покупки", Source.SAVINGS, a.purchases.last().source)
        assertEquals("лавка покупки", "shop_market", a.purchases.last().shop)
        assertEquals("дневник", "Купили у реки: корм", a.diary.last().text)
    }

    @Test
    fun `хотелка из копилки не трогает банки`() {
        val s = ready(40, 20, 30)
        val o = town.buyAt(s, "fun_ball", "shop_foma", Source.SAVINGS).done()
        val a = o.state
        assertEquals("кошелёк", 70, a.balance)
        assertEquals("«Хочу»", 20, a.jarWant)
        assertEquals("копилка", 5, a.savings)
        assertEquals("почему", listOf("Из копилки: 30 → 5"), o.why)
        assertEquals("сундук", listOf("fun_ball"), a.owned)
    }

    @Test
    fun `непредвиденное из запаса не входит в факт плана`() {
        val s = ready(40, 20, 30)
        val o = town.buyAt(s, "care_doctor", null, Source.RESERVE).done()
        val a = o.state
        assertEquals("кошелёк", 60, a.balance)
        assertEquals("«Нужное»", 40, a.jarNeed)
        assertEquals("«Хочу»", 20, a.jarWant)
        assertEquals("запас", 0, a.reserve)
        assertEquals("почему", listOf("Из запаса: 10 → 0"), o.why)
        assertEquals("строка", "Финни в повязке со звёздочками.", o.line)
        assertNull("лавки нет", a.purchases.last().shop)
        assertEquals("источник", Source.RESERVE, a.purchases.last().source)
        assertEquals("категория", Category.UNPLANNED, a.purchases.last().category)
        assertEquals("факт обязательного", 0, a.factMandatory)
        assertEquals("факт желаемого", 0, a.factOptional)
        assertEquals("в дневник сцена не пишет", emptyList<DiaryLine>(), a.diary)
    }

    @Test
    fun `непредвиденное берёт остаток из Хочу когда запаса мало`() {
        val s = ready(58, 8, 30)
        assertEquals("запас до оплаты", 4, s.reserve)
        val o = town.buyAt(s, "care_doctor", null, Source.RESERVE).done()
        val a = o.state
        assertEquals("кошелёк", 60, a.balance)
        assertEquals("«Нужное» не тронуто", 58, a.jarNeed)
        assertEquals("«Хочу»", 2, a.jarWant)
        assertEquals("запас", 0, a.reserve)
        assertEquals("почему", listOf("Из запаса: 4 → 0", "Из «Хочу»: 8 → 2"), o.why)
    }

    @Test
    fun `непредвиденное из Хочу и из копилки`() {
        val s = ready(40, 20, 30)
        val want = town.buyAt(s, "care_doctor", null, Source.WANT).done()
        assertEquals("«Хочу»", 10, want.state.jarWant)
        assertEquals("запас не тронут", 10, want.state.reserve)
        assertEquals("почему", listOf("Из «Хочу»: 20 → 10"), want.why)
        val savings = town.buyAt(s, "care_doctor", null, Source.SAVINGS).done()
        assertEquals("кошелёк", 70, savings.state.balance)
        assertEquals("копилка", 20, savings.state.savings)
        assertEquals("«Хочу» не тронуто", 20, savings.state.jarWant)
        assertEquals("почему", listOf("Из копилки: 30 → 20"), savings.why)
    }

    // ---------- вещи, которые остаются ----------

    @Test
    fun `вещь ложится в сундук и на первое свободное место своего типа`() {
        var s = ready(0, 100, 0)
        s = s.buy("fun_ball", "shop_foma", Source.WANT)
        assertEquals("сундук", listOf("fun_ball"), s.owned)
        assertEquals("мячик на полу", mapOf("spot_5" to "fun_ball"), s.placed)
        s = s.buy("fun_picture", "shop_foma", Source.WANT)
        assertEquals("картина на стене", "fun_picture", s.placed["spot_2"])
        s = s.buy("fun_spinner", "shop_foma", Source.WANT)
        assertEquals("вертушка на столе", "fun_spinner", s.placed["spot_3"])
        assertEquals("занято три места", 3, s.placed.size)
        assertEquals("сундук", listOf("fun_ball", "fun_picture", "fun_spinner"), s.owned)
    }

    @Test
    fun `когда свободных мест типа нет вещь остаётся в сундуке`() {
        var s = ready(0, 100, 0)
        s = s.buy("fun_ball", "shop_foma", Source.WANT)
        s = s.buy("fun_rug", "shop_foma", Source.WANT)
        assertEquals("два свободных места пола заняты", mapOf("spot_5" to "fun_ball", "spot_6" to "fun_rug"), s.placed)
        s = s.buy("fun_robot", "shop_foma", Source.WANT)
        assertEquals("сундук", listOf("fun_ball", "fun_rug", "fun_robot"), s.owned)
        assertEquals("мест больше не заняли", mapOf("spot_5" to "fun_ball", "spot_6" to "fun_rug"), s.placed)
    }

    // ---------- предпросмотр: обязательное ----------

    @Test
    fun `когда в Нужном хватает касса даёт один вариант`() {
        val s = ready(40, 20, 30)
        val q = town.quote(s, "food_basic", "shop_market")
        assertEquals("товар", "food_basic", q.itemId)
        assertEquals("лавка", "shop_market", q.shopId)
        assertEquals("цена", 20, q.price)
        assertEquals("строка пуста", "", q.line)
        assertEquals("заметки нет", "", q.note)
        assertEquals("вариант один", 1, q.options.size)
        assertPay("покупка", q.pay(Source.NEED), "Купить за 20", listOf("Из «Нужного»: 40 → 20"), false)
    }

    @Test
    fun `когда в Нужном не хватает касса называет обе суммы и четыре варианта`() {
        val s = ready(10, 40, 30)
        val q = town.quote(s, "food_basic", "shop_market")
        assertEquals("строка", "В «Нужном» 10, корм стоит 20. Не хватает 10", q.line)
        assertEquals("заметки нет", "", q.note)
        assertEquals(
            "порядок вариантов",
            listOf(
                PayKind.CHEAPER to null,
                PayKind.PAY to Source.RESERVE,
                PayKind.PAY to Source.TRANSFER_WANT,
                PayKind.PAY to Source.SAVINGS,
            ),
            q.kinds(),
        )
        val cheaper = q.options[0]
        assertEquals("подпись «дешевле»", "Дешевле: каша 15", cheaper.label)
        assertEquals("товар «дешевле»", "food_porridge", cheaper.itemId)
        assertEquals("предпросмотр «дешевле» пуст", emptyList<String>(), cheaper.preview)
        assertEquals("«дешевле» — основной вариант", false, cheaper.more)
        assertPay("доплата", q.pay(Source.RESERVE), "Добавить 10 из запаса", listOf("Корм будет!", stamp), false)
        assertPay("перенос", q.pay(Source.TRANSFER_WANT), "Взять 10 из «Хочу»", listOf("В «Хочу» будет 30", stamp), false)
        assertPay("копилка", q.pay(Source.SAVINGS), "Из копилки 20", listOf("Копилка: 30 → 10", stamp), true)
    }

    @Test
    fun `доплата обещает штамп Нужное куплено когда закрывает вторую нужду`() {
        val s = ready(10, 40, 30).buy("care_soap", "shop_foma", Source.NEED)
        assertEquals("«Нужное» пусто", 0, s.jarNeed)
        val q = town.quote(s, "food_basic", "shop_market")
        assertEquals("строка", "В «Нужном» 0, корм стоит 20. Не хватает 20", q.line)
        assertPay(
            "доплата",
            q.pay(Source.RESERVE),
            "Из запаса 20",
            listOf("Корм будет!", "Штамп «Нужное куплено» — да", stamp),
            false,
        )
    }

    @Test
    fun `предпросмотр из копилки показывает срок мечты`() {
        var s = ready(10, 20, 60)
        s = economy.chooseGoal(s, Goal("goal_scooter", "Самокат", "🛴", 150)).ok()
        val q = town.quote(s, "food_basic", "shop_foma")
        assertEquals("строка", "В «Нужном» 10, корм стоит 30. Не хватает 20", q.line)
        assertEquals(
            "порядок вариантов",
            listOf(
                PayKind.PAY to Source.RESERVE,
                PayKind.PAY to Source.TRANSFER_WANT,
                PayKind.PAY to Source.SAVINGS,
            ),
            q.kinds(),
        )
        assertPay("доплата", q.pay(Source.RESERVE), "Запас 10 + «Хочу» 10", listOf("Корм будет!", stamp), false)
        assertPay("перенос", q.pay(Source.TRANSFER_WANT), "Взять 20 из «Хочу»", listOf("В «Хочу» будет 0", stamp), false)
        assertPay(
            "копилка",
            q.pay(Source.SAVINGS),
            "Из копилки 30",
            listOf("Копилка: 60 → 30", "Самокат: ≈ 2 ✉ → ≈ 4 ✉", stamp),
            true,
        )
    }

    @Test
    fun `когда остаётся только копилка ожидание стоит первым`() {
        val s = ready(10, 0, 85)
        val q = town.quote(s, "food_lunch", "shop_foma")
        assertEquals("строка", "Не хватает 30: в кошельке 15, вкусный обед 45", q.line)
        assertEquals("порядок вариантов", listOf(PayKind.WAIT to null, PayKind.PAY to Source.SAVINGS), q.kinds())
        assertEquals("подпись ожидания", "Подождать нового конверта", q.options[0].label)
        assertEquals("предпросмотр ожидания пуст", emptyList<String>(), q.options[0].preview)
        assertEquals("ожидание — основной вариант", false, q.options[0].more)
        assertPay("копилка", q.pay(Source.SAVINGS), "Из копилки 45", listOf("Копилка: 85 → 40", stamp), true)
    }

    // ---------- предпросмотр: хотелки ----------

    @Test
    fun `когда в Хочу не хватает касса напоминает про запас и прячет перенос под Ещё`() {
        val s = ready(40, 5, 30)
        val q = town.quote(s, "fun_ball", "shop_foma")
        assertEquals("строка", "В «Хочу» 5, мячик стоит 25. Не хватает 20", q.line)
        assertEquals("заметка", "Запас 25 — на нужное и на всякий случай", q.note)
        assertEquals(
            "порядок вариантов",
            listOf(
                PayKind.WAIT to null,
                PayKind.PAY to Source.TRANSFER_NEED,
                PayKind.PAY to Source.SAVINGS,
            ),
            q.kinds(),
        )
        assertPay("перенос", q.pay(Source.TRANSFER_NEED), "Взять 20 из «Нужного»", listOf("В «Нужном» будет 20", stamp), true)
        assertPay("копилка", q.pay(Source.SAVINGS), "Из копилки 25", listOf("Копилка: 30 → 5", stamp), true)
    }

    @Test
    fun `когда в Хочу хватает касса даёт один вариант`() {
        val s = ready(40, 20, 30)
        val q = town.quote(s, "fun_balloon", "shop_market")
        assertEquals("строка пуста", "", q.line)
        assertEquals("вариант один", 1, q.options.size)
        assertPay("покупка", q.pay(Source.WANT), "Купить за 8", listOf("Из «Хочу»: 20 → 12"), false)
    }

    @Test
    fun `демо-путь шаг 7 когда не хватает всего кошелька`() {
        var s = ready(40, 20, 30)
        assertEquals("кошелёк после раскладки", 70, s.balance)
        s = s.buy("food_basic", "shop_market", Source.NEED)
        assertEquals("«Нужное» после корма", 20, s.jarNeed)
        assertEquals("кошелёк после корма", 50, s.balance)
        s = s.buy("fun_balloon", "shop_market", Source.WANT)
        assertEquals("«Хочу» после шарика", 12, s.jarWant)
        assertEquals("кошелёк после шарика", 42, s.balance)
        s = s.buy("care_soap", "shop_foma", Source.NEED)
        assertEquals("«Нужное» после мыла", 10, s.jarNeed)
        assertEquals("кошелёк после мыла", 32, s.balance)

        val q = town.quote(s, "fun_tent", "shop_foma")
        assertEquals("цена", 60, q.price)
        assertEquals("строка", "Не хватает 28: в кошельке 32, домик-палатка 60", q.line)
        assertEquals(
            "порядок вариантов",
            listOf(PayKind.MAKE_GOAL to null, PayKind.WAIT to null, PayKind.CHEAPER to null),
            q.kinds(),
        )
        assertEquals("подпись мечты", "Сделать мечтой", q.options[0].label)
        assertEquals("товар мечты", "fun_tent", q.options[0].itemId)
        assertEquals("предпросмотр мечты", listOf("Мечта: домик-палатка — 60"), q.options[0].preview)
        assertEquals("подпись ожидания", "Подождать нового конверта", q.options[1].label)
        assertEquals("подпись «дешевле»", "Дешевле: шарик 10", q.options[2].label)
        assertEquals("товар «дешевле»", "fun_balloon", q.options[2].itemId)
        assertTrue("все три варианта — основные", q.options.none { it.more })
    }

    // ---------- предпросмотр: непредвиденное ----------

    @Test
    fun `непредвиденное даёт запас Хочу и копилку`() {
        val s = ready(40, 20, 30)
        val q = town.quote(s, "care_doctor", null)
        assertNull("лавки нет", q.shopId)
        assertEquals("цена", 10, q.price)
        assertEquals("строка пуста", "", q.line)
        assertEquals(
            "порядок вариантов",
            listOf(
                PayKind.PAY to Source.RESERVE,
                PayKind.PAY to Source.WANT,
                PayKind.PAY to Source.SAVINGS,
            ),
            q.kinds(),
        )
        assertPay("запас", q.pay(Source.RESERVE), "Из запаса 10", listOf("Из запаса: 10 → 0"), false)
        assertPay("«Хочу»", q.pay(Source.WANT), "Из «Хочу» 10", listOf("Из «Хочу»: 20 → 10"), false)
        assertPay("копилка", q.pay(Source.SAVINGS), "Из копилки 10", listOf("Копилка: 30 → 20", stamp), true)
    }

    @Test
    fun `непредвиденное предлагает запас и Хочу вместе`() {
        val s = ready(58, 8, 30)
        val q = town.quote(s, "care_doctor", null)
        assertEquals("строка пуста", "", q.line)
        assertEquals(
            "порядок вариантов",
            listOf(PayKind.PAY to Source.RESERVE, PayKind.PAY to Source.SAVINGS),
            q.kinds(),
        )
        assertPay(
            "составная оплата",
            q.pay(Source.RESERVE),
            "Запас 4 + «Хочу» 6",
            listOf("Из запаса: 4 → 0", "Из «Хочу»: 8 → 2"),
            false,
        )
    }

    @Test
    fun `когда есть только копилка она стоит основным вариантом`() {
        val s = ready(60, 5, 35)
        assertEquals("запаса нет", 0, s.reserve)
        val q = town.quote(s, "care_doctor", null)
        assertEquals("строка", "Запас 0, приём доктора стоит 10. Не хватает 10", q.line)
        assertEquals("вариант один", listOf(PayKind.PAY to Source.SAVINGS), q.kinds())
        assertPay("копилка", q.pay(Source.SAVINGS), "Из копилки 10", listOf("Копилка: 35 → 25", stamp), false)
    }

    @Test
    fun `когда платить нечем вариантов нет`() {
        val s = ready(88, 3, 5)
        val q = town.quote(s, "care_doctor", null)
        assertEquals("строка", "Запас 4, приём доктора стоит 10. Не хватает 6", q.line)
        assertEquals("вариантов нет", emptyList<PayOption>(), q.options)
    }

    @Test
    fun `когда не хватает всего кошелька на непредвиденное касса называет кошелёк`() {
        val s = ready(3, 2, 95)
        assertEquals("кошелёк", 5, s.balance)
        val q = town.quote(s, "care_doctor", null)
        assertEquals("строка", "Не хватает 5: в кошельке 5, приём доктора 10", q.line)
        assertEquals("вариант один", listOf(PayKind.PAY to Source.SAVINGS), q.kinds())
        assertEquals("копилка стала основным вариантом", false, q.options[0].more)
    }

    // ---------- границы банок ----------

    @Test
    fun `обязательное покупается ровно на свою цену и отказывает на монету меньше`() {
        val base = ready(100, 0, 0)
        shelfItems().filter { it.second.item.category == Category.MANDATORY }.forEach { (shopId, shelf) ->
            val id = shelf.item.id
            val p = shelf.price
            val t = lower(shelf.item.title)
            val exact = base.copy(balance = p, jarNeed = p, jarWant = 0)
            val q = town.quote(exact, id, shopId)
            assertEquals("«$id» в «$shopId»: ровно хватает — строки нет", "", q.line)
            assertPay("«$id» ровно хватает", q.pay(Source.NEED), "Купить за $p", listOf("Из «Нужного»: $p → 0"), false)
            val after = town.buyAt(exact, id, shopId, Source.NEED).done().state
            assertEquals("«$id»: кошелёк потрачен весь", 0, after.balance)
            assertEquals("«$id»: «Нужное» пусто", 0, after.jarNeed)

            val short = base.copy(balance = p - 1, jarNeed = p - 1, jarWant = 0)
            assertEquals(
                "«$id» в «$shopId»: на монету меньше",
                "Не хватает 1: в кошельке ${p - 1}, $t $p",
                town.buyAt(short, id, shopId, Source.NEED).refusal(),
            )
        }
    }

    @Test
    fun `хотелка покупается ровно на свою цену и отказывает на монету меньше`() {
        val base = ready(0, 100, 0)
        shelfItems().filter { it.second.item.category == Category.OPTIONAL }.forEach { (shopId, shelf) ->
            val id = shelf.item.id
            val p = shelf.price
            val t = lower(shelf.item.title)
            val exact = base.copy(balance = p, jarNeed = 0, jarWant = p)
            val q = town.quote(exact, id, shopId)
            assertEquals("«$id» в «$shopId»: ровно хватает — строки нет", "", q.line)
            assertPay("«$id» ровно хватает", q.pay(Source.WANT), "Купить за $p", listOf("Из «Хочу»: $p → 0"), false)
            val after = town.buyAt(exact, id, shopId, Source.WANT).done().state
            assertEquals("«$id»: кошелёк потрачен весь", 0, after.balance)
            assertEquals("«$id»: «Хочу» пусто", 0, after.jarWant)

            val short = base.copy(balance = p - 1, jarNeed = 0, jarWant = p - 1)
            assertEquals(
                "«$id» в «$shopId»: на монету меньше",
                "Не хватает 1: в кошельке ${p - 1}, $t $p",
                town.buyAt(short, id, shopId, Source.WANT).refusal(),
            )
        }
    }

    @Test
    fun `непредвиденное оплачивается ровно из запаса и отказывает на монету меньше`() {
        val base = ready(0, 100, 0)
        val exact = base.copy(balance = 10, jarNeed = 0, jarWant = 0)
        assertEquals("запас ровно на приём", 10, exact.reserve)
        val after = town.buyAt(exact, "care_doctor", null, Source.RESERVE).done().state
        assertEquals("кошелёк потрачен весь", 0, after.balance)
        val short = base.copy(balance = 9, jarNeed = 0, jarWant = 0)
        assertEquals(
            "на монету меньше",
            "Не хватает 1: в кошельке 9, приём доктора 10",
            town.buyAt(short, "care_doctor", null, Source.RESERVE).refusal(),
        )
    }

    @Test
    fun `с пустыми банками ни один товар лавок не оплачивается из банок`() {
        val base = ready(50, 30, 20).copy(balance = 0, jarNeed = 0, jarWant = 0, savings = 0)
        shelfItems().forEach { (shopId, shelf) ->
            val q = town.quote(base, shelf.item.id, shopId)
            assertTrue("«${shelf.item.id}» в «$shopId»: есть вариант оплаты ${q.options}", q.options.none { it.kind == PayKind.PAY })
            assertTrue("«${shelf.item.id}» в «$shopId»: пустая строка отказа", q.line.isNotEmpty())
        }
    }

    // ---------- свойства §3 перебором ----------

    /** Все товары всех полок демо-состояния: (лавка, строка полки). */
    private fun shelfItems(): List<Pair<String, ShelfItem>> {
        val probe = ready(50, 30, 20)
        return townContent.shops.flatMap { shop -> prices.shelf(probe, shop.id).map { shop.id to it } }
    }

    private fun lower(title: String) = title.replaceFirstChar { it.lowercase() }

    /** Разные банки на одной и той же неделе; собраны руками — это и есть перебор границ. */
    private fun jarStates(): List<Pair<String, GameState>> {
        val base = ready(50, 30, 20)
        fun st(name: String, b: Int, jn: Int, jw: Int, sv: Int) =
            name to base.copy(balance = b, jarNeed = jn, jarWant = jw, savings = sv)
        return listOf(
            st("пустые банки", 0, 0, 0, 0),
            st("только копилка", 0, 0, 0, 100),
            st("только запас", 60, 0, 0, 0),
            st("на составную оплату", 12, 0, 8, 0),
            st("полные банки", 100, 45, 45, 50),
            st("немного всего", 40, 20, 5, 15),
        )
    }

    /** (описание, состояние, товар, лавка, предпросмотр) по всем полкам и всем банкам. */
    private fun allQuotes(): List<Triple<String, GameState, Quote>> =
        jarStates().flatMap { (name, s) ->
            shelfItems().map { (shopId, shelf) ->
                Triple("$name, «${shelf.item.id}» в «$shopId»", s, town.quote(s, shelf.item.id, shopId))
            } + Triple("$name, приём доктора", s, town.quote(s, "care_doctor", null))
        }

    @Test
    fun `любой предложенный способ оплаты проходит через кассу`() {
        allQuotes().forEach { (where, s, q) ->
            q.options.filter { it.kind == PayKind.PAY }.forEach { o ->
                val r = town.buyAt(s, q.itemId, q.shopId, o.source!!)
                assertTrue("$where: «${o.label}» — $r", r is TownResult.Done)
            }
        }
    }

    @Test
    fun `основных вариантов не больше трёх`() {
        allQuotes().forEach { (where, _, q) ->
            val main = q.options.filter { !it.more }
            assertTrue("$where: основных ${main.size} — ${main.map { it.label }}", main.size <= 3)
        }
    }

    @Test
    fun `вариант дешевле ведёт к товару который покупается сразу`() {
        var seen = 0
        allQuotes().forEach { (where, s, q) ->
            q.options.filter { it.kind == PayKind.CHEAPER }.forEach { o ->
                seen++
                val id = o.itemId ?: error("$where: у варианта «${o.label}» нет товара")
                val other = town.quote(s, id, q.shopId)
                assertTrue(
                    "$where: «${o.label}» ведёт на «$id», а там ${other.options.map { it.label }}",
                    other.options.any { it.kind == PayKind.PAY && !it.more },
                )
            }
        }
        assertTrue("вариант «Дешевле» ни разу не встретился", seen > 0)
    }

    @Test
    fun `вариант сделать мечтой всегда принимается`() {
        var seen = 0
        allQuotes().forEach { (where, s, q) ->
            q.options.filter { it.kind == PayKind.MAKE_GOAL }.forEach { o ->
                seen++
                val id = o.itemId ?: error("$where: у варианта «${o.label}» нет товара")
                assertTrue("$where: мечта «$id» не принята", town.makeGoal(s, id) is TownResult.Done)
            }
        }
        assertTrue("вариант «Сделать мечтой» ни разу не встретился", seen > 0)
    }

    @Test
    fun `предупреждение о штампе стоит ровно там где план перестаёт держаться`() {
        var withStamp = 0
        allQuotes().forEach { (where, s, q) ->
            q.options.filter { it.kind == PayKind.PAY }.forEach { o ->
                val after = town.buyAt(s, q.itemId, q.shopId, o.source!!).done().state
                val expected = planKept(s) && !planKept(after)
                if (expected) withStamp++
                assertEquals(
                    "$where: «${o.label}», предпросмотр ${o.preview}",
                    expected,
                    stamp in o.preview,
                )
            }
        }
        assertTrue("предупреждение о штампе ни разу не понадобилось", withStamp > 0)
    }

    // ---------- тексты для ребёнка ----------

    private val hurryWords = listOf("сегодня", "осталось", "до конца недели", "скорее", "пока не", "успей", "последн")
    private val debtWords = listOf("долг", "одолж", "взаймы", "занять")
    private val shameWords = listOf(
        "цена лени", "ленив", "зачем", "транжир", "жадин", "зря", "впуст", "провал", "плохо", "неправильно", "ошибк",
    )
    private val genderedWords = listOf(
        "отложил", "планировал", "потратил", "положил", "взял", "проголодался", "запачкался",
        "пришёл", "пришла", "захотел", "захотела", "купил", "купила", "выбрал", "выбрала",
        "бегал", "подвернул", "достал", "видел", "видела", "доволен", "довольна", "устроил", "чистенький",
    )

    private fun wholeWord(word: String) =
        Regex("(?<![\\p{L}\\p{N}_])" + Regex.escape(word) + "(?![\\p{L}\\p{N}_])")

    /** Все строки кассы: предпросмотры, подписи, отказы, итоги покупок и дневник. */
    private fun checkoutTexts(): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        allQuotes().forEach { (where, s, q) ->
            if (q.line.isNotEmpty()) out += "$where line" to q.line
            if (q.note.isNotEmpty()) out += "$where note" to q.note
            q.options.forEach { o ->
                out += "$where label" to o.label
                o.preview.forEachIndexed { i, p -> out += "$where preview$i" to p }
            }
            q.options.filter { it.kind == PayKind.PAY }.forEach { o ->
                val done = town.buyAt(s, q.itemId, q.shopId, o.source!!).done()
                out += "$where итог" to done.line
                done.why.forEachIndexed { i, w -> out += "$where почему$i" to w }
                done.state.diary.forEach { d -> out += "$where дневник" to d.text }
            }
            Source.entries.forEach { src ->
                val r = town.buyAt(s, q.itemId, q.shopId, src)
                if (r is TownResult.Refused) out += "$where отказ $src" to r.line
            }
        }
        return out
    }

    @Test
    fun `тексты кассы не торопят не занимают и не стыдят`() {
        val texts = checkoutTexts()
        assertTrue("строк кассы не собрано", texts.size > 50)
        texts.forEach { (where, text) ->
            (hurryWords + debtWords + shameWords).forEach { w ->
                assertTrue("стоп-слово «$w» в $where «$text»", !text.lowercase().contains(w))
            }
        }
    }

    @Test
    fun `тексты кассы не называют род ребёнка и питомца`() {
        checkoutTexts().forEach { (where, text) ->
            genderedWords.forEach { w ->
                assertTrue("слово с родом «$w» в $where «$text»", !wholeWord(w).containsMatchIn(text.lowercase()))
            }
        }
    }
}
