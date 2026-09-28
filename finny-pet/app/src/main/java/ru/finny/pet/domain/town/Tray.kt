package ru.finny.pet.domain.town

/** Заказ покупателя: id жителя и id изделий в порядке job.menu (одинаковые — рядом), TOWN-J1-0 § 2. */
data class TrayOrder(val customer: String, val items: List<String>)

/** Раунд мини-игры «Поднос по заказу» (TOWN-J1-0 § 2). */
data class TrayRound(
    val jobId: String,
    val menu: List<String>,
    val orders: List<TrayOrder>,
    val tray: List<String> = emptyList(),
    val missed: Boolean = false,
    val results: List<Boolean> = emptyList(),
    val pointer: Boolean = false,
    val intro: String? = null,
    val riddle: Boolean = false,
) {
    val index: Int get() = results.size
    val stars: Int get() = results.count { it }
    val done: Boolean get() = index >= orders.size
}

/** Итог отдачи подноса (TOWN-J1-0 § 2). */
data class Give(
    val round: TrayRound, val accepted: Boolean, val served: Boolean, val star: Boolean,
    val missing: List<String>, val extra: List<String>,
)

/** Ответ на загадку Бори в раунде подноса: исход Town и раунд после него (TOWN-J1-1a § 1). */
data class TrayAnswer(val result: TownResult, val round: TrayRound)

/** Механика подноса: укладка, снятие, отдача и подсказка Бори (TOWN-J1-0 § 2). Чистый Kotlin. */
object Tray {

    fun put(r: TrayRound, pastry: String): TrayRound {
        if (r.done || pastry !in r.menu) return r
        val size = r.orders[r.index].items.size
        if (r.tray.size >= size) return r
        return r.copy(tray = r.tray + pastry)
    }

    fun take(r: TrayRound, slot: Int): TrayRound {
        if (r.done || slot !in r.tray.indices) return r
        return r.copy(tray = r.tray.filterIndexed { i, _ -> i != slot })
    }

    fun give(r: TrayRound): Give {
        if (r.done) return Give(r, false, false, false, emptyList(), emptyList())
        val order = r.orders[r.index].items
        if (r.tray.size < order.size) return Give(r, false, false, false, emptyList(), emptyList())
        val (missing, extra) = missAndExtra(order, r.tray)
        if (missing.isEmpty()) {
            val star = !r.missed
            val round = r.copy(tray = emptyList(), missed = false, results = r.results + star)
            return Give(round, true, true, star, emptyList(), emptyList())
        }
        val kept = r.tray.toMutableList()
        extra.forEach { kept.remove(it) }
        val round = r.copy(tray = kept, missed = true)
        return Give(round, true, false, false, missing, extra)
    }

    /** Изделия текущего заказа без пары на подносе (мультимножество, порядок o.items); r.done → пусто (TOWN-J1-1a § 1). */
    fun missing(r: TrayRound): List<String> {
        if (r.done) return emptyList()
        val order = r.orders[r.index].items
        return missAndExtra(order, r.tray).first
    }

    fun hint(r: TrayRound, n: Int): TrayRound {
        if (r.done || n <= 0) return r
        val order = r.orders[r.index].items
        val size = order.size
        val (missing, extra) = missAndExtra(order, r.tray)
        val m = size - missing.size
        if (m == size) return r
        val add = minOf(n, size - 1 - m)
        if (add == 0) return r
        var tray = r.tray
        val free = size - tray.size
        if (add > free) {
            val toDrop = add - free
            val extraLeft = extra.toMutableList()
            repeat(toDrop) {
                val victim = extraLeft.removeAt(extraLeft.size - 1)
                val idx = tray.indexOfLast { it == victim }
                tray = tray.filterIndexed { i, _ -> i != idx }
            }
        }
        tray = tray + missing.take(add)
        return r.copy(tray = tray)
    }

    /** Мультимножественное сравнение: missing — из order без пары в tray, extra — из tray без пары в order. */
    private fun missAndExtra(order: List<String>, tray: List<String>): Pair<List<String>, List<String>> {
        val left = tray.toMutableList()
        val missing = order.filter { !left.remove(it) }
        return missing to left
    }
}
