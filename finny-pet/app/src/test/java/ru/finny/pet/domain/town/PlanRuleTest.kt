package ru.finny.pet.domain.town

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.LedgerEntry

/**
 * Правило «план соблюдён» (GAME_CONCEPT §6.6, TOWN-S1a §1 PLANKEPT и §3 STAMP):
 * что снимает штамп «По плану», а что нет. Штамп читается из итога недели, а не пересчитывается тестом.
 */
class PlanRuleTest {

    private val town = S1aStand.town
    private val stamp = "Штамп «По плану» на этой неделе не получится"

    /** Настоящий штамп недели: тот, что движок положил в итог. */
    private fun planKept(s: GameState): Boolean = town.endWeek(s).s1aState().history.last().planKept

    @Test
    fun `покупка нужного из «Нужного» в пределах плана штамп сохраняет`() {
        val s = town.buyAt(S1aStand.planned(20, 20, 30), "food_basic", "shop_market", Source.NEED).s1aState()
        assertEquals("потрачено на нужное", 20, s.factMandatory)
        assertTrue("штамп «По плану» пропал", planKept(s))
    }

    @Test
    fun `доплата за нужное из запаса снимает штамп`() {
        val s = town.buyAt(S1aStand.planned(20, 20, 30), "food_lunch", "shop_foma", Source.RESERVE).s1aState()
        assertEquals("«Нужное» после доплаты", 0, s.jarNeed)
        assertEquals("потрачено на нужное", 45, s.factMandatory)
        assertFalse("штамп «По плану» остался", planKept(s))
    }

    @Test
    fun `перенос из «Хочу» за нужное снимает штамп`() {
        val s = town.buyAt(S1aStand.planned(20, 30, 30), "food_lunch", "shop_foma", Source.TRANSFER_WANT).s1aState()
        assertEquals("«Нужное»", 0, s.jarNeed)
        assertEquals("«Хочу» после переноса", 5, s.jarWant)
        assertFalse("штамп «По плану» остался", planKept(s))
    }

    @Test
    fun `перенос запаса в «Нужное» и покупка из него снимают штамп а касса об этом предупреждает`() {
        val s = town.transfer(S1aStand.planned(20, 20, 30), Source.RESERVE, Source.NEED, 30).s1aState()
        assertEquals("«Нужное» после переноса", 50, s.jarNeed)

        val pay = town.quote(s, "food_lunch", "shop_foma").options.first { it.kind == PayKind.PAY && it.source == Source.NEED }
        assertEquals("предпросмотр оплаты", listOf("Из «Нужного»: 50 → 5", stamp), pay.preview)

        val after = town.buyAt(s, "food_lunch", "shop_foma", Source.NEED).s1aState()
        assertEquals("потрачено на нужное", 45, after.factMandatory)
        assertFalse("штамп «По плану» остался", planKept(after))
    }

    @Test
    fun `когда покупка укладывается в план предупреждения о штампе нет`() {
        val s = S1aStand.planned(50, 20, 30)
        val pay = town.quote(s, "food_lunch", "shop_foma").options.first { it.kind == PayKind.PAY && it.source == Source.NEED }
        assertEquals("предпросмотр оплаты", listOf("Из «Нужного»: 50 → 5"), pay.preview)
        assertTrue("штамп «По плану» пропал", planKept(town.buyAt(s, "food_lunch", "shop_foma", Source.NEED).s1aState()))
    }

    @Test
    fun `непредвиденное из запаса штамп не снимает`() {
        var s = town.buyAt(S1aStand.planned(20, 20, 30), "food_basic", "shop_market", Source.NEED).s1aState()
        val reserveBefore = s.reserve
        s = town.buyAt(s, "care_doctor", null, Source.RESERVE).s1aState()
        assertEquals("приём оплачен не из запаса", reserveBefore - 10, s.reserve)
        assertEquals("приём попал в нужное", 20, s.factMandatory)
        assertEquals("приём попал в хотелки", 0, s.factOptional)
        assertTrue("штамп «По плану» пропал", planKept(s))
    }

    @Test
    fun `непредвиденное из «Хочу» штамп не снимает`() {
        var s = town.buyAt(S1aStand.planned(20, 20, 30), "food_basic", "shop_market", Source.NEED).s1aState()
        s = town.buyAt(s, "care_doctor", null, Source.WANT).s1aState()
        assertEquals("«Хочу» после приёма", 10, s.jarWant)
        assertEquals("приём попал в хотелки", 0, s.factOptional)
        assertTrue("штамп «По плану» пропал", planKept(s))
    }

    @Test
    fun `взнос в копилку из «Хочу» штамп не снимает`() {
        var s = town.buyAt(S1aStand.planned(20, 20, 30), "food_basic", "shop_market", Source.NEED).s1aState()
        s = town.deposit(s, Source.WANT, 10).s1aState()
        assertEquals("отложено за неделю", 40, s.factSavings)
        assertTrue("штамп «По плану» пропал", planKept(s))
    }

    @Test
    fun `конверт на проверку плана не влияет`() {
        // строки конверта пишет S1b, здесь заданы руками
        val envelope = listOf(LedgerEntry("Смена: Помочь Марте", 6), LedgerEntry("Бонус от взрослого: за помощь по дому", 10))
        val kept = town.buyAt(S1aStand.planned(20, 20, 30), "food_basic", "shop_market", Source.NEED).s1aState()
        assertTrue("без конверта штамп есть", planKept(kept))
        assertTrue("конверт снял штамп", planKept(kept.copy(envelope = envelope)))

        val broken = town.buyAt(S1aStand.planned(20, 20, 30), "food_lunch", "shop_foma", Source.RESERVE).s1aState()
        assertFalse("без конверта штампа нет", planKept(broken))
        assertFalse("конверт вернул штамп", planKept(broken.copy(envelope = envelope)))
    }
}
