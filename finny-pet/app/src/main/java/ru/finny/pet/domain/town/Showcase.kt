package ru.finny.pet.domain.town

import ru.finny.pet.domain.Category

/** Витрина лавки (TOWN-A1s, решение 39 а): товары на досках по 3, по 3 доски на странице. Чистый Kotlin. */
object Showcase {
    const val PER_ROW = 3
    const val ROWS = 3

    /**
     * Страницы → доски → слоты (null — пустой слот). Отделы по порядку [Category] (нужное → хочу → прочее), внутри
     * отдела — порядок [shelf]; каждый отдел с новой доски, неполная доска прижата вправо (пустые слоты слева);
     * по [ROWS] досок на странице. Пустая полка — пустой список.
     */
    fun pages(shelf: List<ShelfItem>): List<List<List<ShelfItem?>>> =
        Category.entries.flatMap { c ->
            shelf.filter { it.item.category == c }.chunked(PER_ROW) { row -> List(PER_ROW - row.size) { null } + row }
        }.chunked(ROWS)

    /** Товар события на витрине: из setup PRICE/OFFER, иначе первый исход BUY:<товар>; нет — null. */
    fun eventItem(e: EventDef): String? =
        e.setup.firstNotNullOfOrNull { (it as? EventEffect.Price)?.item ?: (it as? EventEffect.Offer)?.item }
            ?: e.outcomes.firstNotNullOfOrNull { ((it.fact as? Fact.Buy)?.target as? BuyTarget.Item)?.id }
}
