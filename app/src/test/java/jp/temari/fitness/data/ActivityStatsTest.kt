package jp.temari.fitness.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** [ActivityStats.currentStreak] の数え方を確かめる。 */
class ActivityStatsTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 15)

    private fun daysBack(vararg offsets: Long): Set<LocalDate> = offsets.map { today.minusDays(it) }.toSet()

    @Test
    fun countsBackFromToday() {
        assertEquals(3, ActivityStats.currentStreak(today, daysBack(0, 1, 2, 4)))
    }

    @Test
    fun startsFromYesterdayWhenTodayHasNoRecordYet() {
        assertEquals(2, ActivityStats.currentStreak(today, daysBack(1, 2)))
    }

    @Test
    fun zeroWhenNoRecentRecord() {
        assertEquals(0, ActivityStats.currentStreak(today, daysBack(3, 4)))
    }
}
