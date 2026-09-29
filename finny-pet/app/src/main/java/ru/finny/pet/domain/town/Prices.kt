package ru.finny.pet.domain.town

import ru.finny.pet.domain.Content
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.ShopItem

/** Строка полки лавки: товар, цена в этой лавке и прежняя цена скидки (§5.4). */
data class ShelfItem(val item: ShopItem, val price: Int, val was: Int? = null)

/** Цены лавок с переопределениями PRICE и OFFER событий этой недели (TOWN-S1a §2). */
class Prices(private val content: Content) {
    private val town = content.town ?: error("content.json: нет ключа town")

    /** KEEP_DEMO: вещи событий с demo.keepOnShelf — в демо не уходят с полки (§1, §7.4). */
    val keepDemo: Set<String> = town.events.filter { it.demo.keepOnShelf }
        .flatMap { e -> e.outcomes.mapNotNull { ((it.fact as? Fact.Buy)?.target as? BuyTarget.Item)?.id } }
        .toSet()

    fun price(s: GameState, itemId: String, shopId: String): Int? {
        val base = town.shops.firstOrNull { it.id == shopId }?.sells?.firstOrNull { it.item == itemId }?.price
            ?: return null
        return when (val o = override(s, itemId, shopId)) {
            is EventEffect.Price -> o.price
            is EventEffect.Offer -> o.now
            else -> base
        }
    }

    fun was(s: GameState, itemId: String, shopId: String): Int? =
        if (price(s, itemId, shopId) == null) null else (override(s, itemId, shopId) as? EventEffect.Offer)?.was

    fun cheapest(s: GameState, itemId: String): Int? = town.shops.mapNotNull { price(s, itemId, it.id) }.minOrNull()

    fun basePrice(itemId: String): Int? =
        town.shops.flatMap { it.sells }.filter { it.item == itemId }.minOfOrNull { it.price }

    fun shelf(s: GameState, shopId: String): List<ShelfItem> {
        val shop = town.shops.firstOrNull { it.id == shopId } ?: return emptyList()
        return shop.sells.mapNotNull { offer ->
            val item = content.itemOrNull(offer.item) ?: return@mapNotNull null
            val hidden = item.eventOnly ||
                (!s.demo && item.unlockPeriod > s.period) ||
                (item.keep && item.id in s.owned && !(s.demo && item.id in keepDemo))
            if (hidden) null else ShelfItem(item, price(s, item.id, shopId)!!, was(s, item.id, shopId))
        }
    }

    /** Последнее подходящее переопределение событий, пришедших на этой неделе. */
    private fun override(s: GameState, itemId: String, shopId: String): EventEffect? {
        var win: EventEffect? = null
        for (e in s.events) {
            if (e.period != s.period) continue
            val def = town.events.firstOrNull { it.id == e.id } ?: continue
            for (fx in def.setup) {
                val (item, shop, until) = when (fx) {
                    is EventEffect.Price -> Triple(fx.item, fx.shop, fx.until)
                    is EventEffect.Offer -> Triple(fx.item, fx.shop, fx.until)
                    else -> continue
                }
                if (item == itemId && shop == shopId && (until == Until.WEEK_END || e.day == s.day)) win = fx
            }
        }
        return win
    }
}

internal fun Content.itemOrNull(id: String): ShopItem? =
    items.firstOrNull { it.id == id } ?: town?.items?.firstOrNull { it.id == id }

internal fun lower(title: String): String = title.replaceFirstChar { it.lowercase() }

/** Вещь в сундук (без дублей) и на первое свободное место своего типа, если она ещё не стоит (§6). */
internal fun TownContent.putHome(s: GameState, item: ShopItem): GameState {
    val owned = if (item.id in s.owned) s.owned else s.owned + item.id
    if (item.id in s.placed.values) return s.copy(owned = owned)
    val starters = homeItems.mapNotNull { it.spot }.toSet()
    val spot = spots.firstOrNull { it.slot == item.slot && it.id !in starters && it.id !in s.placed }
        ?: return s.copy(owned = owned)
    return s.copy(owned = owned, placed = s.placed + (spot.id to item.id))
}
