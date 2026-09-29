package ru.finny.pet.domain.town

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.pet.domain.Face
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.Need
import ru.finny.pet.domain.ok

/**
 * Реплики питомца по тапу (docs/tasks/TOWN-S1d.md §A, ORACLE).
 * Состояния собираются настоящим путём игрока: newGame → createPet → setPlan → Town.confirmPlan →
 * Town.visit → Town.buyAt → Town.endWeek → Town.wake. Руками (copy pet) задаются только граничные
 * показатели питомца у порога грусти — каждый раз с комментарием.
 * Строки сравниваются со списками town.chatter из настоящего content.json; литералами записана
 * только склейка подсказки (« {T} {shop.at} — {цена}»).
 */
class PetTalkTest {

    private val town = S1aStand.town
    private val economy = S1aStand.economy
    private val content = S1aStand.content
    private val chatter = content.town!!.chatter
    private val sadBelow = content.rules.faceSadBelow

    // ---------- сцены ----------

    /** Питомец есть, монеты ещё не разложены. */
    private fun fresh(): GameState = S1aStand.profile()

    /** План 40 / 20 / 30 подтверждён, в лавках ещё не были. */
    private fun planned(): GameState = S1aStand.planned(40, 20, 30)

    /** План подтверждён, цены обеих лавок увидены на этой неделе. */
    private fun seenBoth(): GameState = town.visit(town.visit(planned(), "market").state, "foma").state

    /** Еда недели куплена (каша на рынке), уход — ещё нет. */
    private fun foodBought(): GameState =
        town.buyAt(seenBoth(), "food_porridge", "shop_market", Source.NEED).s1aState()

    /** Еда и уход недели куплены. */
    private fun bothBought(): GameState =
        town.buyAt(foodBought(), "care_soap", "shop_foma", Source.NEED).s1aState()

    /** Утро недели 2 после недели без еды и ухода: питомец грустит, монеты ещё не разложены. */
    private fun sadMorning(): GameState {
        val night = town.endWeek(planned()).s1aState()
        val morning = town.wake(night).s1aState()
        assertEquals("стенд: не наступила неделя 2", 2, morning.period)
        assertTrue("стенд: утром план уже подтверждён", !morning.plan.confirmed)
        assertEquals("стенд: питомец не загрустил", Face.SAD, economy.face(morning.pet!!))
        return morning
    }

    private fun texts(s: GameState, taps: Int = 4): List<String> = (0 until taps).map { town.petLine(s, it).text }

    // ---------- контент ----------

    @Test
    fun `реплики питомца разобраны из контента`() {
        listOf(
            "needFood" to chatter.needFood, "needCare" to chatter.needCare,
            "sad" to chatter.sad, "calm" to chatter.calm,
        ).forEach { (name, list) ->
            assertTrue("town.chatter.$name разобран как $list, нужно ≥ 2 реплик", list.size >= 2)
        }
    }

    // ---------- пустой профиль ----------

    @Test
    fun `без питомца реплики нет`() {
        val empty = economy.newGame(true)
        val line = town.petLine(empty, 0)
        assertEquals("строка пустого профиля", "", line.text)
        assertNull("нужда пустого профиля", line.need)
        assertNull("лавка пустого профиля", line.shop)
    }

    // ---------- чистота и счётчик тапов ----------

    @Test
    fun `одно состояние и один тап дают одну и ту же реплику`() {
        val s = seenBoth()
        val first = town.petLine(s, 0)
        town.petLine(s, 1)
        town.petLine(s, 7)
        assertEquals("повторный тап 0 дал другое", first, town.petLine(s, 0))
        assertEquals("тап 1 не повторился", town.petLine(s, 1), town.petLine(s, 1))
    }

    @Test
    fun `реплики нужды идут по кругу по счётчику тапов`() {
        val s = planned()
        val food = chatter.needFood
        assertEquals(
            "цикл реплик еды",
            List(food.size + 2) { food[it % food.size] },
            texts(s, food.size + 2),
        )
        assertEquals("разные тапы дали одинаковые строки", food.size, texts(s, food.size).toSet().size)
    }

    @Test
    fun `разные состояния дают разные реплики`() {
        val lines = listOf(fresh(), planned(), foodBought()).map { town.petLine(it, 0) }
        assertEquals("разные состояния дали одну и ту же реплику $lines", 3, lines.toSet().size)
    }

    // ---------- до плана ----------

    @Test
    fun `до плана питомец не называет нужды`() {
        val s = fresh()
        assertTrue("стенд: план подтверждён", !s.plan.confirmed)
        assertTrue("стенд: еда уже куплена", s.purchases.none { it.need == Need.FOOD })
        texts(s).forEachIndexed { tap, text ->
            assertTrue("тап $tap до плана дал «$text», а не реплику calm", text in chatter.calm)
        }
        val line = town.petLine(s, 0)
        assertNull("до плана названа нужда", line.need)
        assertNull("до плана названа лавка", line.shop)
    }

    @Test
    fun `цены увиденные до плана не превращаются в подсказку`() {
        val s = town.visit(fresh(), "market").state
        assertTrue("стенд: цены рынка не запомнились", s.seenPrices.isNotEmpty())
        val line = town.petLine(s, 0)
        assertTrue("до плана реплика «${line.text}» не из calm", line.text in chatter.calm)
        assertNull("до плана названа нужда", line.need)
        assertNull("до плана названа лавка", line.shop)
    }

    // ---------- порядок нужд ----------

    @Test
    fun `сначала еда потом уход потом спокойная реплика`() {
        val start = planned()
        val afterFood = town.buyAt(start, "food_porridge", "shop_market", Source.NEED).s1aState()
        val afterBoth = town.buyAt(afterFood, "care_soap", "shop_foma", Source.NEED).s1aState()

        val first = town.petLine(start, 0)
        assertEquals("до покупок реплика не про еду", chatter.needFood[0], first.text)
        assertEquals("до покупок нужда", Need.FOOD, first.need)

        val second = town.petLine(afterFood, 0)
        assertEquals("после еды реплика не про уход", chatter.needCare[0], second.text)
        assertEquals("после еды нужда", Need.CARE, second.need)

        val third = town.petLine(afterBoth, 0)
        assertNull("после обеих покупок нужда осталась", third.need)
        assertNull("после обеих покупок осталась лавка", third.shop)
        assertTrue(
            "после обеих покупок реплика «${third.text}» не из calm и не из sad",
            third.text in chatter.calm || third.text in chatter.sad,
        )
        assertEquals("питомец недели 1 сыт и чист, ждали спокойную реплику", chatter.calm[0], third.text)
    }

    // ---------- подсказка по увиденным ценам ----------

    @Test
    fun `до входа в лавку подсказки и лавки в реплике нет`() {
        val s = planned()
        assertTrue("стенд: цены уже увидены", s.seenPrices.isEmpty())
        val line = town.petLine(s, 0)
        assertEquals("без увиденных цен реплика с подсказкой", chatter.needFood[0], line.text)
        assertEquals("нужда", Need.FOOD, line.need)
        assertNull("лавка без увиденных цен", line.shop)
    }

    @Test
    fun `место без полки подсказку не даёт`() {
        // в мастерской продаётся только «Новая лампа» — товар события, на полке его нет
        val s = town.visit(planned(), "workshop").state
        assertTrue("стенд: мастерская что-то положила в увиденные цены", s.seenPrices.isEmpty())
        val line = town.petLine(s, 0)
        assertEquals("реплика после пустой полки", chatter.needFood[0], line.text)
        assertNull("лавка после пустой полки", line.shop)
    }

    @Test
    fun `подсказка называет самый дешёвый виденный товар нужды и его лавку`() {
        val s = seenBoth()
        val food = town.petLine(s, 0)
        // на рынке каша 15 — дешевле корма 20 и супер-корма 35, у Фомы еда дороже
        assertEquals("подсказка про еду", chatter.needFood[0] + " Каша у реки — 15", food.text)
        assertEquals("нужда", Need.FOOD, food.need)
        assertEquals("лавка подсказки про еду", "shop_market", food.shop)

        val care = town.petLine(foodBought(), 0)
        // мыло у Фомы 10 дешевле мыла на рынке 12
        assertEquals("подсказка про уход", chatter.needCare[0] + " Мыло у Фомы — 10", care.text)
        assertEquals("нужда", Need.CARE, care.need)
        assertEquals("лавка подсказки про уход", "shop_foma", care.shop)

        listOf(food, care).forEach { line ->
            val at = content.town!!.shops.first { it.id == line.shop }.at
            assertTrue("в строке «${line.text}» нет «$at» лавки ${line.shop}", line.text.contains(at))
        }
    }

    @Test
    fun `подсказка держится за тапами по кругу`() {
        val s = seenBoth()
        val hint = " Каша у реки — 15"
        assertEquals(
            "подсказка отвалилась на других тапах",
            chatter.needFood.map { it + hint } + listOf(chatter.needFood[0] + hint),
            texts(s, chatter.needFood.size + 1),
        )
        assertEquals("лавка на втором тапе", "shop_market", town.petLine(s, 1).shop)
    }

    @Test
    fun `цены прошлой недели не подсказывают`() {
        val week1 = town.visit(planned(), "market").state
        assertTrue("стенд: цены недели 1 не запомнились", week1.seenPrices.isNotEmpty())
        val morning = town.wake(town.endWeek(week1).s1aState()).s1aState()
        val week2 = town.confirmPlan(economy.setPlan(morning, 40, 20, 30).ok()).s1aState()

        assertEquals("стенд: не наступила неделя 2", 2, week2.period)
        assertTrue("стенд: цены прошлой недели стёрлись", week2.seenPrices.isNotEmpty())
        assertTrue(
            "стенд: среди увиденных цен есть цена этой недели ${week2.seenPrices}",
            week2.seenPrices.values.all { it.period < week2.period },
        )
        val line = town.petLine(week2, 0)
        assertEquals("нужда недели 2", Need.FOOD, line.need)
        assertEquals("реплика с подсказкой прошлой недели", chatter.needFood[0], line.text)
        assertNull("лавка по цене прошлой недели", line.shop)
    }

    // ---------- грусть ----------

    @Test
    fun `грустный питомец до плана говорит о грусти без нужды и лавки`() {
        val s = sadMorning()
        texts(s).forEachIndexed { tap, text ->
            assertTrue("тап $tap у грустного питомца дал «$text», а не реплику sad", text in chatter.sad)
        }
        val line = town.petLine(s, 0)
        assertNull("у грустного питомца до плана названа нужда", line.need)
        assertNull("у грустного питомца до плана названа лавка", line.shop)
    }

    @Test
    fun `грусть не отменяет напоминание о еде после плана`() {
        val s = town.confirmPlan(economy.setPlan(sadMorning(), 40, 20, 30).ok()).s1aState()
        assertEquals("стенд: питомец перестал грустить", Face.SAD, economy.face(s.pet!!))
        val line = town.petLine(s, 0)
        assertEquals("после плана грусть заслонила еду", chatter.needFood[0], line.text)
        assertEquals("нужда", Need.FOOD, line.need)
    }

    @Test
    fun `после покупок нужд грусть отличается от спокойствия по порогу`() {
        val s = bothBought()
        val pet = s.pet!!
        // граница faceSadBelow: сытость на монету ниже порога — грусть, ровно порог — спокойствие
        val sad = s.copy(pet = pet.copy(hunger = sadBelow - 1))
        val calm = s.copy(pet = pet.copy(hunger = sadBelow))
        assertEquals("стенд: питомец ниже порога не грустит", Face.SAD, economy.face(sad.pet!!))
        assertNotEquals("стенд: питомец ровно на пороге грустит", Face.SAD, economy.face(calm.pet!!))

        val sadLine = town.petLine(sad, 0)
        assertTrue("ниже порога реплика «${sadLine.text}» не из sad", sadLine.text in chatter.sad)
        assertNull("у грустного питомца названа нужда", sadLine.need)
        assertNull("у грустного питомца названа лавка", sadLine.shop)

        val calmLine = town.petLine(calm, 0)
        assertTrue("на пороге реплика «${calmLine.text}» не из calm", calmLine.text in chatter.calm)
        assertNull("в спокойной реплике названа нужда", calmLine.need)
    }

    @Test
    fun `грустные реплики идут по кругу`() {
        val s = sadMorning()
        assertEquals(
            "цикл реплик грусти",
            List(chatter.sad.size + 1) { chatter.sad[it % chatter.sad.size] },
            texts(s, chatter.sad.size + 1),
        )
    }

    // ---------- тексты ----------

    @Test
    fun `реплики питомца без стоп-слов без рода и без неподставленных скобок`() {
        val scenes = listOf(fresh(), planned(), seenBoth(), foodBought(), bothBought(), sadMorning())
        val spoken = scenes.flatMap { texts(it, 4) }
        spoken.forEachIndexed { i, text -> assertTrue("реплика $i пустая — питомцу нечего сказать", text.isNotBlank()) }
        assertTrue("все реплики одинаковые: ${spoken.toSet()}", spoken.toSet().size >= chatter.calm.size + chatter.sad.size)
        val all = spoken + texts(economy.newGame(true), 1)
        S1aChildText.check("реплики питомца", all)
        PetTalk.check("реплики питомца", all)
        all.forEach { text ->
            assertTrue("неподставленная скобка в строке «$text»", !text.contains("{") && !text.contains("}"))
        }
    }
}

/** Слова о теле и настроении питомца, которых в репликах нет (TOWN-S1d, ORACLE). */
internal object PetTalk {
    private val body = listOf("голодный", "голоден", "сыт", "рад", "грустный", "чистый", "грязный")
    private val phrases = listOf("скучал", "где ты был", "хочу-хочу")

    private fun wholeWord(word: String) =
        Regex("(?<![\\p{L}\\p{N}_])" + Regex.escape(word) + "(?![\\p{L}\\p{N}_])")

    fun check(where: String, texts: List<String>) {
        assertTrue("$where: нечего проверять — движок не вернул ни одной строки", texts.isNotEmpty())
        texts.filter { it.isNotBlank() }.forEach { text ->
            val low = text.lowercase()
            body.forEach { w ->
                assertTrue("$where: слово о теле «$w» в строке «$text»", !wholeWord(w).containsMatchIn(low))
            }
            phrases.forEach { w ->
                assertTrue("$where: запрещённая фраза «$w» в строке «$text»", !low.contains(w))
            }
        }
    }
}
