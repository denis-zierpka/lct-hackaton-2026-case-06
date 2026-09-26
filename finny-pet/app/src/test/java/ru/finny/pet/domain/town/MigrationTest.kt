package ru.finny.pet.domain.town

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.pet.domain.BudgetPlan
import ru.finny.pet.domain.Category
import ru.finny.pet.domain.GameState
import ru.finny.pet.domain.LedgerEntry
import ru.finny.pet.domain.Need
import ru.finny.pet.domain.Purchase
import java.io.File

/**
 * Перенос сохранения 1.3.0 на схему «Городка» (TOWN-S1a §7, GAME_CONCEPT §16.3).
 * Фикстура — реальный state.json варианта game: неделя 2, план 50 / 30 / 30, потрачено 45 на нужное.
 */
class MigrationTest {

    /** Ровно конфигурация StateStore, как в GameStateCompatTest. */
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val fixtureText = File("src/test/resources/town/state_1_3_0.json").readText()
    private val fixture: GameState get() = json.decodeFromString(GameState.serializer(), fixtureText)
    private val migration = Migration(S1aStand.content)
    private val seed = 20260926L

    @Test
    fun `старое сохранение получает банки из плана и факта`() {
        val old = fixture
        assertEquals("версия схемы фикстуры", 0, old.stateVersion)
        val s = migration.migrate(old)
        assertEquals("«Нужное» — остаток плана по нужному", 5, s.jarNeed)
        assertEquals("«Хочу» — весь план по хотелкам", 30, s.jarWant)
        assertEquals("запас — остальное, в нём и невнесённые 30 в копилку", 80, s.reserve)
        assertEquals("версия схемы", 1, s.stateVersion)
        assertEquals("день", 1, s.day)
        assertFalse("ночь", s.asleep)
        assertEquals("зерно", seed, s.seed)
    }

    @Test
    fun `миграция не создаёт и не теряет монет`() {
        val old = fixture
        val s = migration.migrate(old)
        assertEquals("кошелёк", old.balance, s.balance)
        assertEquals("копилка", old.savings, s.savings)
        assertEquals("копилку никто не пополнял", old.depositedThisPeriod, s.depositedThisPeriod)
        assertEquals("журнал", old.ledger, s.ledger)
        assertEquals("сумма журнала не равна балансу", s.balance, s.ledger.sumOf { it.amount })
        assertEquals("итоги недель", old.history, s.history)
        assertEquals("бомбы", old.bombs, s.bombs)
        assertEquals("полученные мечты", old.achievedGoals, s.achievedGoals)
        assertEquals("покупки", old.purchases, s.purchases)
        assertEquals("мечта", old.goal, s.goal)
        assertEquals("план", old.plan, s.plan)
        assertS1aInvariants("после миграции", s)
    }

    @Test
    fun `повторная миграция ничего не меняет`() {
        val once = migration.migrate(fixture)
        assertEquals("вторая миграция изменила состояние", once, migration.migrate(once))
    }

    @Test
    fun `сыгранный движком профиль не мигрирует повторно`() {
        val played = S1aStand.town.deposit(S1aStand.planned(40, 20, 30), Source.WANT, 10).s1aState()
        assertEquals("confirmPlan не поставил версию схемы", 1, played.stateVersion)
        assertEquals("миграция тронула сыгранный профиль", played, migration.migrate(played))
    }

    @Test
    fun `свежий профиль получает только зерно версию и день`() {
        val fresh = S1aStand.economy.newGame(false)
        assertEquals(
            "миграция свежего профиля",
            fresh.copy(seed = seed, day = 1, asleep = false, stateVersion = 1),
            migration.migrate(fresh),
        )
    }

    @Test
    fun `своё зерно сохранения не переписывается`() {
        assertEquals("зерно", 7L, migration.migrate(fixture.copy(seed = 7L)).seed)
    }

    @Test
    fun `вещи купленные на этой неделе оказываются дома`() {
        // копия фикстуры: к покупкам недели добавлен мячик за 25 — кошелёк и журнал сходятся
        val withBall = fixture.let {
            it.copy(
                purchases = it.purchases + Purchase("fun_ball", "Мячик", Category.OPTIONAL, Need.FUN, 25),
                balance = it.balance - 25,
                ledger = it.ledger + LedgerEntry("Покупка: Мячик", -25),
            )
        }
        val s = migration.migrate(withBall)
        assertEquals("сундук", listOf("fun_ball"), s.owned)
        assertEquals("мячик не встал на первое свободное место пола", "fun_ball", s.placed["spot_5"])
        assertEquals("«Нужное»", 5, s.jarNeed)
        assertEquals("«Хочу» — план минус потраченное", 5, s.jarWant)
        assertEquals("сумма журнала не равна балансу", s.balance, s.ledger.sumOf { it.amount })
        assertS1aInvariants("после миграции с вещью", s)
    }

    @Test
    fun `расходные покупки и неизвестные товары в сундук не попадают`() {
        // корм и расчёска фикстуры не остаются дома
        assertEquals("сундук", emptyList<String>(), migration.migrate(fixture).owned)
        assertEquals("места", emptyMap<String, String>(), migration.migrate(fixture).placed)

        val unknown = fixture.let {
            it.copy(purchases = it.purchases + Purchase("нет_такого", "Неизвестно", Category.OPTIONAL, Need.FUN, 0))
        }
        assertEquals("сундук", emptyList<String>(), migration.migrate(unknown).owned)
    }

    @Test
    fun `банки не уходят в минус когда потрачено больше плана`() {
        // граничный план: на нужное потрачено 45 при плане 20
        val over = fixture.copy(plan = BudgetPlan(20, 30, 30, confirmed = true))
        val s = migration.migrate(over)
        assertEquals("«Нужное»", 0, s.jarNeed)
        assertEquals("«Хочу»", 30, s.jarWant)
        assertEquals("запас", 85, s.reserve)
        assertS1aInvariants("после миграции перерасхода", s)
    }

    @Test
    fun `банки не больше кошелька`() {
        // граничный кошелёк: монет меньше, чем остаток плана
        val poor = fixture.copy(balance = 3, ledger = listOf(LedgerEntry("Остаток с прошлой недели", 3)))
        val s = migration.migrate(poor)
        assertEquals("«Нужное»", 3, s.jarNeed)
        assertEquals("«Хочу»", 0, s.jarWant)
        assertEquals("запас", 0, s.reserve)
        assertS1aInvariants("после миграции пустого кошелька", s)
    }

    @Test
    fun `неподтверждённый план банок не создаёт`() {
        val notConfirmed = fixture.copy(plan = fixture.plan.copy(confirmed = false))
        val s = migration.migrate(notConfirmed)
        assertEquals("«Нужное»", 0, s.jarNeed)
        assertEquals("«Хочу»", 0, s.jarWant)
        assertEquals("запас — весь кошелёк", s.balance, s.reserve)
        assertEquals("версия схемы", 1, s.stateVersion)
    }

    @Test
    fun `ночь старого сохранения кончается утром а мечта остаётся`() {
        // граничный день: сохранение 1.3.0 не знало ни дней, ни ночи
        val night = fixture.copy(day = 3, asleep = true)
        val s = migration.migrate(night)
        assertEquals("день", 1, s.day)
        assertFalse("ночь", s.asleep)
        assertEquals("мечта", "goal_scooter", s.goal?.id)
        assertTrue("решённые задания 1.3.0 пропали", s.taskResults == night.taskResults)
    }
}
