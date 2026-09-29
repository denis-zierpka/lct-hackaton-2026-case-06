package ru.finny.pet.domain.town

import ru.finny.pet.domain.Economy
import ru.finny.pet.domain.Face
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.Need

/** Реплика питомца по тапу; shop — id лавки из подсказки (TOWN-S1d §A). */
data class PetLine(val text: String, val need: Need? = null, val shop: String? = null)

/** Что питомец говорит по тапу tap ≥ 0: нужда недели с подсказкой по увиденным ценам, грусть или спокойствие. */
fun Town.petLine(s: GameState, tap: Int): PetLine {
    val pet = s.pet ?: return PetLine("")
    val c = content.town!!
    fun pick(list: List<String>) = if (list.isEmpty()) "" else list[tap.mod(list.size)].replace("{pet}", pet.name)
    if (s.plan.confirmed) {
        val (need, lines) = when {
            s.purchases.none { it.need == Need.FOOD } -> Need.FOOD to c.chatter.needFood
            s.purchases.none { it.need == Need.CARE } -> Need.CARE to c.chatter.needCare
            else -> null to emptyList()
        }
        if (need != null) {
            val seen = (c.items + content.items).filter { it.need == need }.distinctBy { it.id }
                .mapNotNull { item -> s.seenPrices[item.id]?.takeIf { it.period == s.period }?.let { item to it } }
                .minByOrNull { it.second.price }
            val shop = seen?.let { (_, p) -> c.shops.firstOrNull { it.id == p.shop } }
            val hint = if (seen != null && shop != null) " ${seen.first.title} ${shop.at} — ${seen.second.price}" else ""
            return PetLine(pick(lines) + hint, need, shop?.id)
        }
    }
    return PetLine(pick(if (Economy(content).face(pet) == Face.SAD) c.chatter.sad else c.chatter.calm))
}
