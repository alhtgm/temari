package jp.temari.fitness.data

import jp.temari.fitness.config.BingoConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** [BingoRules] と [BingoCard] の判定を確かめる。 */
class BingoRulesTest {

    private val monday = LocalDate.of(2026, 10, 12)

    /** テスト用に1日分を作る。 */
    private fun day(
        date: LocalDate,
        walk: Int = 100,
        life: Int = 100,
        goal: Int = 600,
        morning: Int = 0,
        night: Int = 0,
    ) = DailyActivity(
        date = date,
        walkPoints = walk,
        lifePoints = life,
        steps = walk * 10,
        goal = goal,
        blocks = listOf(
            TimeBlockActivity(TimeBlock.Morning, morning, 0),
            TimeBlockActivity(TimeBlock.Night, night, 0),
        ),
    )

    private fun BingoCard.isOpen(id: BingoCellId) = cells.first { it.id == id }.achieved

    @Test
    fun weekStart_isMonday() {
        assertEquals(monday, BingoRules.weekStartOf(LocalDate.of(2026, 10, 18))) // 日曜
        assertEquals(monday, BingoRules.weekStartOf(monday))
    }

    @Test
    fun emptyWeek_onlyFreeIsOpen() {
        val card = BingoRules.buildCard(monday, emptyList())
        assertEquals(9, card.cells.size)
        assertEquals(listOf(BingoCellId.Free), card.cells.filter { it.achieved }.map { it.id })
        assertFalse(card.hasBingo)
    }

    @Test
    fun thresholds_openEachCell() {
        val week = listOf(
            day(monday, morning = BingoConfig.MORNING_ACTIVE_POINTS),
            day(monday.plusDays(1), life = BingoConfig.LIFE_MASTER_POINTS),
            day(monday.plusDays(2), walk = BingoConfig.WALK_GOOD_POINTS, night = 1),
        )
        val card = BingoRules.buildCard(monday.plusDays(2), week)
        assertTrue(card.isOpen(BingoCellId.MorningActive))
        assertTrue(card.isOpen(BingoCellId.LifeMaster))
        assertTrue(card.isOpen(BingoCellId.WalkGood))
        assertTrue(card.isOpen(BingoCellId.NightLittle))
        assertTrue(card.isOpen(BingoCellId.RecordStreak))
        assertTrue(card.isOpen(BingoCellId.EveryDay))
        assertFalse(card.isOpen(BingoCellId.GoalOnce))
    }

    @Test
    fun goalDays_areCounted() {
        val achieved = { d: LocalDate -> day(d, walk = 400, life = 200) } // 600 で目標ちょうど
        val two = BingoRules.buildCard(monday.plusDays(1), listOf(achieved(monday), achieved(monday.plusDays(1))))
        assertTrue(two.isOpen(BingoCellId.GoalOnce))
        assertFalse(two.isOpen(BingoCellId.GoalThreeTimes))

        val three = BingoRules.buildCard(monday.plusDays(2), (0L..2L).map { achieved(monday.plusDays(it)) })
        assertTrue(three.isOpen(BingoCellId.GoalThreeTimes))
    }

    @Test
    fun streak_breaksOnMissingDay() {
        val week = listOf(day(monday), day(monday.plusDays(1)), day(monday.plusDays(3)))
        val card = BingoRules.buildCard(monday.plusDays(3), week)
        assertFalse(card.isOpen(BingoCellId.RecordStreak))
        assertFalse("水曜が抜けているので毎日ではない", card.isOpen(BingoCellId.EveryDay))
    }

    @Test
    fun lines_areDetected() {
        assertEquals(8, BingoCard.LINES.size)
        // 真ん中の横一列（夜もすこし・フリー・よく歩いた日）
        val week = listOf(day(monday, walk = BingoConfig.WALK_GOOD_POINTS, night = 1))
        val card = BingoRules.buildCard(monday, week)
        assertEquals(listOf(listOf(3, 4, 5)), card.completedLines)
        assertTrue(card.hasBingo)
    }

    @Test
    fun timeBlock_ofHour() {
        assertEquals(null, TimeBlock.of(4))
        assertEquals(TimeBlock.Morning, TimeBlock.of(5))
        assertEquals(TimeBlock.Noon, TimeBlock.of(10))
        assertEquals(TimeBlock.Afternoon, TimeBlock.of(17))
        assertEquals(TimeBlock.Night, TimeBlock.of(23))
    }
}
