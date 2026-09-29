package ru.finny.pet.domain.town

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.pet.domain.Category
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.Need
import ru.finny.pet.domain.ShopItem
import ru.finny.pet.domain.TestContent

/**
 * Оракул TOWN-A1s (раздел ORACLE спеки): чистая раскладка витрины `Showcase`.
 * Полка — реальный контент через `Prices(content).shelf(state, shopId)`, состояние строится как в PricesTest;
 * синтетика — `ShelfItem(ShopItem(...), price)` и `EventDef(...)` с нужными setup / outcomes.
 */
class ShowcaseTest {

    private val content = TestContent.content
    private val town = content.town ?: error("content.json: нет ключа town")
    private val prices = Prices(content)

    private fun state(period: Int = 1, demo: Boolean = false, owned: List<String> = emptyList()) =
        GameState(demo = demo, period = period, day = 1, owned = owned)

    private fun shelf(shopId: String, period: Int = 1, demo: Boolean = false, owned: List<String> = emptyList()) =
        prices.shelf(state(period, demo, owned), shopId)

    /** Страницы полки в виде id товаров: null — пустой слот. */
    private fun plan(shelf: List<ShelfItem>): List<List<List<String?>>> =
        Showcase.pages(shelf).map { page -> page.map { row -> row.map { it?.item?.id } } }

    private fun shelfItem(id: String, category: Category, price: Int = 10) = ShelfItem(
        ShopItem(
            id = id, title = id, emoji = "🧸", category = category, need = Need.UNPLANNED,
            price = price, description = "", reaction = "",
        ),
        price,
    )

    private fun event(
        setup: List<EventEffect> = emptyList(),
        outcomes: List<Fact> = emptyList(),
        kind: EventKind = EventKind.OFFER,
    ) = EventDef(
        id = "t_event", kind = kind, title = "Тестовое событие", intro = "Вступление",
        setup = setup, outcomes = outcomes.map { EventOutcome(it, Verdict.OK, "строка") },
    )

    private fun def(id: String) = town.events.firstOrNull { it.id == id } ?: error("в контенте нет события «$id»")

    private fun price(item: String) = EventEffect.Price(item, "shop_foma", 7, Until.WEEK_END)
    private fun offer(item: String) = EventEffect.Offer(item, "shop_foma", 9, 7, Until.WEEK_END)
    private fun buy(item: String) = Fact.Buy(BuyTarget.Item(item))

    // ---------- pages: реальный контент ----------

    @Test
    fun `витрина рынка на первой неделе — одна страница из трёх досок`() {
        assertEquals(
            listOf(
                listOf(
                    listOf("food_basic", "food_porridge", "care_soap"),
                    listOf(null, null, "care_shampoo_simple"),
                    listOf("fun_balloon", "fun_icecream", "fun_carousel"),
                ),
            ),
            plan(shelf("shop_market", period = 1)),
        )
    }

    @Test
    fun `витрина рынка со второй недели ставит супер-корм в отдел нужного`() {
        val expected = listOf(
            listOf(
                listOf("food_basic", "food_porridge", "food_super"),
                listOf(null, "care_soap", "care_shampoo_simple"),
                listOf("fun_balloon", "fun_icecream", "fun_carousel"),
            ),
        )
        assertEquals("вторая неделя", expected, plan(shelf("shop_market", period = 2)))
        assertEquals("демо с первой недели", expected, plan(shelf("shop_market", period = 1, demo = true)))
    }

    @Test
    fun `витрина лавки Фомы на первой неделе — две страницы`() {
        assertEquals(
            listOf(
                listOf(
                    listOf("food_basic", "food_lunch", "care_soap"),
                    listOf("care_shampoo", "care_brush", "care_vitamins"),
                    listOf("fun_balloon", "fun_rug", "fun_ball"),
                ),
                listOf(
                    listOf("fun_spinner", "fun_book", "fun_robot"),
                    listOf("fun_tent", "gift_card", "gift_toy"),
                ),
            ),
            plan(shelf("shop_foma", period = 1)),
        )
    }

    @Test
    fun `витрина лавки Фомы на пятой неделе — полные шесть досок`() {
        val expected = listOf(
            listOf(
                listOf("food_basic", "food_lunch", "care_soap"),
                listOf("care_shampoo", "care_brush", "care_vitamins"),
                listOf("fun_balloon", "fun_picture", "fun_rug"),
            ),
            listOf(
                listOf("fun_ball", "fun_spinner", "fun_kite"),
                listOf("fun_book", "fun_robot", "fun_starlamp"),
                listOf("fun_tent", "gift_card", "gift_toy"),
            ),
        )
        assertEquals("пятая неделя", expected, plan(shelf("shop_foma", period = 5)))
        assertEquals("демо с первой недели", expected, plan(shelf("shop_foma", period = 1, demo = true)))
    }

    @Test
    fun `купленные вещи уходят с витрины и последняя доска прижата вправо`() {
        val pages = plan(shelf("shop_foma", period = 5, owned = listOf("fun_robot", "fun_tent")))
        assertEquals("страниц", 2, pages.size)
        assertEquals(
            "вторая страница",
            listOf(
                listOf("fun_ball", "fun_spinner", "fun_kite"),
                listOf("fun_book", "fun_starlamp", "gift_card"),
                listOf(null, null, "gift_toy"),
            ),
            pages[1],
        )
    }

    // ---------- pages: границы и правила ----------

    @Test
    fun `пустая полка не даёт страниц а один товар даёт одну доску`() {
        assertEquals("пустая полка", emptyList<List<List<String?>>>(), plan(emptyList()))
        assertEquals(
            "один товар",
            listOf(listOf(listOf(null, null, "a"))),
            plan(listOf(shelfItem("a", Category.OPTIONAL))),
        )
    }

    @Test
    fun `девять товаров отдела заполняют страницу а десятый уходит на вторую`() {
        val nine = ('a'..'i').map { shelfItem(it.toString(), Category.MANDATORY) }
        assertEquals(
            "ровно девять",
            listOf(listOf(listOf("a", "b", "c"), listOf("d", "e", "f"), listOf("g", "h", "i"))),
            plan(nine),
        )
        assertEquals(
            "на один больше",
            listOf(
                listOf(listOf("a", "b", "c"), listOf("d", "e", "f"), listOf("g", "h", "i")),
                listOf(listOf(null, null, "j")),
            ),
            plan(nine + shelfItem("j", Category.MANDATORY)),
        )
    }

    @Test
    fun `каждый отдел начинается с новой доски`() {
        val shelf = listOf(
            shelfItem("a", Category.OPTIONAL),
            shelfItem("b", Category.MANDATORY),
            shelfItem("c", Category.OPTIONAL),
            shelfItem("d", Category.UNPLANNED),
        )
        assertEquals(
            listOf(
                listOf(
                    listOf(null, null, "b"),
                    listOf(null, "a", "c"),
                    listOf(null, null, "d"),
                ),
            ),
            plan(shelf),
        )
    }

    @Test
    fun `отделы идут по порядку Category а внутри отдела — по порядку полки`() {
        val shelf = listOf(
            shelfItem("m1", Category.MANDATORY),
            shelfItem("o1", Category.OPTIONAL),
            shelfItem("u1", Category.UNPLANNED),
            shelfItem("m2", Category.MANDATORY),
        )
        assertEquals(
            listOf(
                listOf(
                    listOf(null, "m1", "m2"),
                    listOf(null, null, "o1"),
                    listOf(null, null, "u1"),
                ),
            ),
            plan(shelf),
        )
    }

    @Test
    fun `в доске три слота один отдел и хотя бы один товар`() {
        assertEquals("товаров на доске", 3, Showcase.PER_ROW)
        assertEquals("досок на странице", 3, Showcase.ROWS)
        val stands = listOf(
            shelf("shop_market", period = 1),
            shelf("shop_market", period = 2),
            shelf("shop_foma", period = 1),
            shelf("shop_foma", period = 5),
            shelf("shop_foma", period = 5, owned = listOf("fun_robot", "fun_tent")),
            listOf(shelfItem("a", Category.UNPLANNED)),
            ('a'..'j').map { shelfItem(it.toString(), Category.OPTIONAL) },
        )
        for (shelf in stands) {
            val pages = Showcase.pages(shelf)
            val label = shelf.map { it.item.id }.toString()
            pages.forEachIndexed { i, page ->
                assertTrue("страница $i пуста, полка $label", page.isNotEmpty())
                assertTrue("на странице $i досок ${page.size}, полка $label", page.size in 1..Showcase.ROWS)
                if (i < pages.lastIndex) assertEquals("не последняя страница $i, полка $label", Showcase.ROWS, page.size)
                page.forEach { row ->
                    assertEquals("слотов в доске, полка $label", Showcase.PER_ROW, row.size)
                    val items = row.filterNotNull()
                    assertTrue("пустая доска, полка $label", items.isNotEmpty())
                    assertEquals("в доске один отдел, полка $label", 1, items.map { it.item.category }.distinct().size)
                }
            }
        }
    }

    @Test
    fun `витрина сохраняет все товары полки в порядке отделов`() {
        val stands = listOf(
            shelf("shop_market", period = 2),
            shelf("shop_foma", period = 5),
            shelf("shop_foma", period = 1),
            listOf(
                shelfItem("a", Category.UNPLANNED),
                shelfItem("b", Category.MANDATORY),
                shelfItem("c", Category.UNPLANNED),
                shelfItem("d", Category.OPTIONAL),
            ),
        )
        for (shelf in stands) {
            assertEquals(
                "товары витрины, полка ${shelf.map { it.item.id }}",
                shelf.sortedBy { it.item.category.ordinal },
                Showcase.pages(shelf).flatten().flatten().filterNotNull(),
            )
        }
    }

    // ---------- eventItem ----------

    @Test
    fun `товар события берётся из setup PRICE и OFFER`() {
        assertEquals("подорожание корма", "food_basic", Showcase.eventItem(def("p3_price_up")))
        assertEquals("распродажа робота", "fun_robot", Showcase.eventItem(def("c1_robot_sale")))
    }

    @Test
    fun `без setup товар берётся из первого исхода покупки товара`() {
        assertEquals("новинка-змей", "fun_kite", Showcase.eventItem(def("c3_almost")))
        assertEquals("супер-корм мимо BUY_AT", "food_super", Showcase.eventItem(def("pk3_super_food")))
    }

    @Test
    fun `у события без товара товара нет`() {
        assertNull("заказ работы", Showcase.eventItem(def("job_market_help")))
        assertNull("где дешевле корм", Showcase.eventItem(def("pk1_cheaper_food")))
        assertNull("где дешевле мыло", Showcase.eventItem(def("pk1_cheaper_soap")))
        assertNull("список на неделю", Showcase.eventItem(def("p1_list")))
        assertNull("пустое событие", Showcase.eventItem(event()))
    }

    @Test
    fun `setup важнее исходов`() {
        assertEquals("x", Showcase.eventItem(event(setup = listOf(offer("x")), outcomes = listOf(buy("y")))))
        assertEquals("x", Showcase.eventItem(event(setup = listOf(price("x")), outcomes = listOf(buy("y")))))
    }

    @Test
    fun `берётся первый эффект setup а PRICE и OFFER равноправны`() {
        assertEquals("две цены", "a", Showcase.eventItem(event(setup = listOf(price("a"), price("b")))))
        assertEquals("скидка перед ценой", "a", Showcase.eventItem(event(setup = listOf(offer("a"), price("b")))))
        assertEquals("цена перед скидкой", "a", Showcase.eventItem(event(setup = listOf(price("a"), offer("b")))))
        assertEquals(
            "эффекты без товара пропускаются",
            "a",
            Showcase.eventItem(event(setup = listOf(EventEffect.ShortChange(5), EventEffect.ItemGive("g"), offer("a")))),
        )
    }

    @Test
    fun `нужда тег и BUY_AT товаром не считаются`() {
        val skipped = listOf(
            Fact.Buy(BuyTarget.ByNeed(Need.FOOD)),
            Fact.Buy(BuyTarget.Tag("gift")),
            Fact.BuyAt("food_basic", PriceRank.CHEAPEST),
            Fact.Skip,
            Fact.MakeGoal("fun_kite"),
        )
        assertNull("ни одного товара", Showcase.eventItem(event(outcomes = skipped)))
        assertEquals("товар после них", "z", Showcase.eventItem(event(outcomes = skipped + buy("z"))))
    }

    // ---------- eventItem: правило по живому content.json ----------

    /** Места-лавки события: своё place и триггеры ENTER, у которых есть лавка. */
    private fun shopEvents(t: TownContent): List<Pair<String, EventDef>> =
        t.events.filter { it.id !in t.eventsOff && it.kind != EventKind.JOB }
            .flatMap { e ->
                (listOfNotNull(e.place) + e.triggers.filterIsInstance<Trigger.Enter>().map { it.place })
                    .distinct().filter { p -> t.shops.any { it.place == p } }.map { it to e }
            }

    /** Нарушения правила витрины: у события лавки есть товар, он на полке этой лавки, и он один на место. */
    private fun problems(t: TownContent): List<String> {
        val found = shopEvents(t)
        val bad = mutableListOf<String>()
        for ((place, e) in found) {
            val id = Showcase.eventItem(e)
            if (id == null) {
                bad += "${e.id}: событие лавки «$place» без товара"
                continue
            }
            val shop = t.shops.first { it.place == place }
            if (shop.sells.none { it.item == id }) bad += "${e.id}: товара «$id» нет в ассортименте лавки «$place»"
        }
        found.groupBy { (place, e) -> place to Showcase.eventItem(e) }
            .filterValues { it.size > 1 }
            .forEach { (key, group) -> bad += "место «${key.first}»: товар «${key.second}» у ${group.map { it.second.id }}" }
        return bad
    }

    @Test
    fun `у каждого живого события лавки есть свой товар на её полке`() {
        assertEquals("нарушения правила витрины", emptyList<String>(), problems(town))
        assertEquals(
            "события лавок и их товары",
            setOf(
                "market" to "food_basic",
                "foma" to "food_basic",
                "foma" to "fun_robot",
                "foma" to "fun_kite",
                "market" to "food_super",
            ),
            shopEvents(town).map { (place, e) -> place to Showcase.eventItem(e) }.toSet(),
        )
    }

    @Test
    fun `включённое событие лавки без товара роняет правило витрины`() {
        assertNotNull("pk2_change убран из eventsOff", town.eventsOff.firstOrNull { it == "pk2_change" })
        assertNull("у события про сдачу товара нет", Showcase.eventItem(def("pk2_change")))
        val broken = town.copy(eventsOff = town.eventsOff - "pk2_change")
        val bad = problems(broken)
        assertTrue("правило не заметило pk2_change: $bad", bad.any { it.startsWith("pk2_change") })
    }
}
