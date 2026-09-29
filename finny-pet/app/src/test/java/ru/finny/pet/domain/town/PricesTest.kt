package ru.finny.pet.domain.town

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.TestContent

/**
 * Оракул §2 спеки TOWN-S1a: цены лавок, переопределения событий и полка.
 * Контент — реальный content.json. Состояния строятся руками: Prices зависит только от
 * недели, дня, списка событий, сундука и демо — плана и питомца ему не нужно.
 */
class PricesTest {

    private val content = TestContent.content
    private val townContent = content.town ?: error("content.json: нет ключа town")
    private val prices = Prices(content)

    private fun state(
        period: Int = 1,
        day: Int = 1,
        demo: Boolean = false,
        events: List<EventState> = emptyList(),
        owned: List<String> = emptyList(),
    ) = GameState(demo = demo, period = period, day = day, events = events, owned = owned)

    private fun active(id: String, period: Int, day: Int = 1) =
        EventState(id = id, status = EventStatus.ACTIVE, period = period, day = day)

    /** Цены на контенте, к которому дописано тестовое событие (в реальном контенте DAY_END нет). */
    private fun pricesWith(vararg defs: EventDef) =
        Prices(content.copy(town = townContent.copy(events = townContent.events + defs)))

    private val dayOffer = EventDef(
        id = "t_day_offer",
        kind = EventKind.OFFER,
        title = "Мячик со скидкой на один день",
        intro = "Скидка на один день",
        setup = listOf(EventEffect.Offer("fun_ball", "shop_foma", 25, 9, Until.DAY_END)),
    )

    private val dayPrice = EventDef(
        id = "t_day_price",
        kind = EventKind.PRICE,
        title = "Мыло по новой цене на один день",
        intro = "Цена на один день",
        setup = listOf(EventEffect.Price("care_soap", "shop_foma", 18, Until.DAY_END)),
    )

    private val foodOffer = EventDef(
        id = "t_food_offer",
        kind = EventKind.OFFER,
        title = "Корм по скидке на неделю",
        intro = "Корм по скидке",
        setup = listOf(EventEffect.Offer("food_basic", "shop_market", 20, 14, Until.WEEK_END)),
    )

    private fun ids(shelf: List<ShelfItem>) = shelf.map { it.item.id }

    // ---------- базовые цены ----------

    @Test
    fun `цена товара берётся из списка своей лавки`() {
        val s = state()
        assertEquals("корм у реки", 20, prices.price(s, "food_basic", "shop_market"))
        assertEquals("корм у Фомы", 30, prices.price(s, "food_basic", "shop_foma"))
        assertEquals("мыло у реки", 12, prices.price(s, "care_soap", "shop_market"))
        assertEquals("мыло у Фомы", 10, prices.price(s, "care_soap", "shop_foma"))
    }

    @Test
    fun `цены нет у незнакомой лавки и у товара которого лавка не продаёт`() {
        val s = state()
        assertNull("незнакомая лавка", prices.price(s, "food_basic", "shop_nowhere"))
        assertNull("рынок не продаёт обед", prices.price(s, "food_lunch", "shop_market"))
        assertNull("Фома не продаёт кашу", prices.price(s, "food_porridge", "shop_foma"))
        assertNull("такого товара нет", prices.price(s, "no_such_item", "shop_foma"))
    }

    @Test
    fun `без событий надбавки нет прежней цены`() {
        val s = state()
        assertNull("корм у реки", prices.was(s, "food_basic", "shop_market"))
        assertNull("робот у Фомы", prices.was(s, "fun_robot", "shop_foma"))
    }

    @Test(expected = IllegalStateException::class)
    fun `без контента городка цены не строятся`() {
        Prices(content.copy(town = null))
    }

    // ---------- переопределения событий ----------

    @Test
    fun `подорожание этой недели действует в обеих лавках`() {
        val s = state(period = 3, events = listOf(active("p3_price_up", 3)))
        assertEquals("корм у реки", 30, prices.price(s, "food_basic", "shop_market"))
        assertEquals("корм у Фомы", 40, prices.price(s, "food_basic", "shop_foma"))
        assertNull("подорожание — не скидка", prices.was(s, "food_basic", "shop_market"))
        assertEquals("мыло не дорожало", 10, prices.price(s, "care_soap", "shop_foma"))
    }

    @Test
    fun `событие прошлой недели на цену не влияет`() {
        val s = state(period = 3, events = listOf(active("p3_price_up", 2)))
        assertEquals("корм у реки", 20, prices.price(s, "food_basic", "shop_market"))
        assertEquals("корм у Фомы", 30, prices.price(s, "food_basic", "shop_foma"))
    }

    @Test
    fun `скидка показывает прежнюю цену и новую`() {
        val s = state(period = 2, day = 2, events = listOf(active("c1_robot_sale", 2, 2)))
        assertEquals("робот по скидке", 25, prices.price(s, "fun_robot", "shop_foma"))
        assertEquals("прежняя цена робота", 40, prices.was(s, "fun_robot", "shop_foma"))
        assertEquals("скидка только на робота", 40, prices.price(s, "fun_starlamp", "shop_foma"))
        assertNull("у лампы прежней цены нет", prices.was(s, "fun_starlamp", "shop_foma"))
    }

    @Test
    fun `закрытое событие меняет цену так же как активное`() {
        val done = EventState("c1_robot_sale", EventStatus.DONE, Verdict.GOOD, 1, period = 2, day = 2)
        val s = state(period = 2, day = 3, events = listOf(done))
        assertEquals("скидка до конца недели держится", 25, prices.price(s, "fun_robot", "shop_foma"))
        assertEquals("прежняя цена", 40, prices.was(s, "fun_robot", "shop_foma"))
    }

    @Test
    fun `скидка на один день держится только в день прихода события`() {
        val p = pricesWith(dayOffer)
        val s = state(period = 2, day = 2, events = listOf(active("t_day_offer", 2, 2)))
        assertEquals("в день прихода", 9, p.price(s, "fun_ball", "shop_foma"))
        assertEquals("прежняя цена в день прихода", 25, p.was(s, "fun_ball", "shop_foma"))
        val next = s.copy(day = 3)
        assertEquals("на следующий день", 25, p.price(next, "fun_ball", "shop_foma"))
        assertNull("прежней цены больше нет", p.was(next, "fun_ball", "shop_foma"))
    }

    @Test
    fun `цена на один день держится только в день прихода события`() {
        val p = pricesWith(dayPrice)
        val s = state(period = 2, day = 2, events = listOf(active("t_day_price", 2, 2)))
        assertEquals("в день прихода", 18, p.price(s, "care_soap", "shop_foma"))
        assertEquals("на следующий день", 10, p.price(s.copy(day = 3), "care_soap", "shop_foma"))
    }

    @Test
    fun `побеждает последнее подходящее переопределение`() {
        val p = pricesWith(foodOffer)
        val order = listOf(active("p3_price_up", 3), active("t_food_offer", 3))
        val s = state(period = 3, events = order)
        assertEquals("скидка записана после подорожания", 14, p.price(s, "food_basic", "shop_market"))
        assertEquals("прежняя цена из скидки", 20, p.was(s, "food_basic", "shop_market"))
        val other = state(period = 3, events = order.reversed())
        assertEquals("подорожание записано после скидки", 30, p.price(other, "food_basic", "shop_market"))
        assertNull("подорожание прежнюю цену не показывает", p.was(other, "food_basic", "shop_market"))
    }

    // ---------- cheapest и basePrice ----------

    @Test
    fun `самая низкая цена ищется по всем лавкам`() {
        val s = state()
        assertEquals("корм", 20, prices.cheapest(s, "food_basic"))
        assertEquals("мыло", 10, prices.cheapest(s, "care_soap"))
        assertEquals("шампунь только у Фомы", 20, prices.cheapest(s, "care_shampoo"))
        assertNull("бантик не продаётся", prices.cheapest(s, "fun_bow"))
        assertNull("такого товара нет", prices.cheapest(s, "no_such_item"))
    }

    @Test
    fun `самая низкая цена учитывает события а базовая цена нет`() {
        val s = state(period = 3, events = listOf(active("p3_price_up", 3)))
        assertEquals("корм подорожал в обеих лавках", 30, prices.cheapest(s, "food_basic"))
        assertEquals("базовая цена корма не меняется", 20, prices.basePrice("food_basic"))
    }

    @Test
    fun `базовая цена берётся по всем лавкам без событий`() {
        assertEquals("мыло", 10, prices.basePrice("care_soap"))
        assertEquals("домик-палатка", 60, prices.basePrice("fun_tent"))
        assertEquals("мячик", 25, prices.basePrice("fun_ball"))
        assertNull("бантик не продаётся", prices.basePrice("fun_bow"))
        assertNull("конструктор не продаётся", prices.basePrice("fun_lego"))
    }

    // ---------- полка ----------

    @Test
    fun `полка рынка идёт в порядке списка лавки с её ценами`() {
        val shelf = prices.shelf(state(demo = true), "shop_market")
        assertEquals(
            "порядок товаров на полке",
            listOf(
                "food_basic", "food_porridge", "food_super", "care_soap",
                "care_shampoo_simple", "fun_balloon", "fun_icecream", "fun_carousel",
            ),
            ids(shelf),
        )
        assertEquals("цены полки", listOf(20, 15, 35, 12, 15, 8, 10, 20), shelf.map { it.price })
        assertTrue("без скидок прежних цен нет", shelf.all { it.was == null })
    }

    @Test
    fun `незнакомая лавка даёт пустую полку`() =
        assertEquals("полка", emptyList<ShelfItem>(), prices.shelf(state(demo = true), "shop_nowhere"))

    @Test
    fun `товар только из событий на полку не попадает`() {
        assertEquals("полка мастерской", emptyList<ShelfItem>(), prices.shelf(state(demo = true), "shop_workshop"))
        assertEquals("лампу мастерская продаёт", 25, prices.price(state(), "lamp_new", "shop_workshop"))
    }

    @Test
    fun `товар будущей недели на полке появляется только со своей недели`() {
        assertTrue("супер-корм на первой неделе", "food_super" !in ids(prices.shelf(state(period = 1), "shop_market")))
        assertTrue("супер-корм на второй неделе", "food_super" in ids(prices.shelf(state(period = 2), "shop_market")))
        val foma1 = ids(prices.shelf(state(period = 1), "shop_foma"))
        listOf("fun_kite", "fun_starlamp", "fun_picture").forEach {
            assertTrue("«$it» на первой неделе, полка $foma1", it !in foma1)
        }
        val foma5 = ids(prices.shelf(state(period = 5), "shop_foma"))
        listOf("fun_kite", "fun_starlamp", "fun_picture").forEach {
            assertTrue("«$it» на пятой неделе, полка $foma5", it in foma5)
        }
    }

    @Test
    fun `в демо неделя товара не проверяется`() {
        val demo = ids(prices.shelf(state(period = 1, demo = true), "shop_foma"))
        val usual = ids(prices.shelf(state(period = 1), "shop_foma"))
        assertEquals("в демо полка Фомы полная", 18, demo.size)
        assertEquals("обычная полка Фомы на первой неделе", 15, usual.size)
        assertTrue("змей в демо", "fun_kite" in demo)
    }

    @Test
    fun `купленная вещь уходит с полки`() {
        val s = state(owned = listOf("fun_ball"))
        assertTrue("мячик", "fun_ball" !in ids(prices.shelf(s, "shop_foma")))
        assertTrue("коврик остался", "fun_rug" in ids(prices.shelf(s, "shop_foma")))
        assertTrue("расходное мороженое не уходит", "fun_icecream" in ids(prices.shelf(state(owned = listOf("fun_icecream")), "shop_market")))
    }

    @Test
    fun `в демо вещь показа остаётся на полке а остальные уходят`() {
        listOf("fun_robot", "fun_kite", "fun_spinner").forEach { id ->
            val s = state(demo = true, owned = listOf(id))
            assertTrue("«$id» в демо остаётся на полке", id in ids(prices.shelf(s, "shop_foma")))
        }
        listOf("fun_ball", "fun_book", "fun_rug").forEach { id ->
            val s = state(demo = true, owned = listOf(id))
            assertTrue("«$id» в демо тоже уходит с полки", id !in ids(prices.shelf(s, "shop_foma")))
        }
    }

    @Test
    fun `на полке стоит цена и прежняя цена скидки`() {
        val s = state(period = 2, day = 2, demo = true, events = listOf(active("c1_robot_sale", 2, 2)))
        val robot = prices.shelf(s, "shop_foma").first { it.item.id == "fun_robot" }
        assertEquals("цена робота на полке", 25, robot.price)
        assertEquals("прежняя цена робота на полке", 40, robot.was)
        val ball = prices.shelf(s, "shop_foma").first { it.item.id == "fun_ball" }
        assertEquals("цена мячика на полке", 25, ball.price)
        assertNull("у мячика прежней цены нет", ball.was)
    }
}
