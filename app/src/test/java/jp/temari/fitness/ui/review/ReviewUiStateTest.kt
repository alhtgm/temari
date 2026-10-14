package jp.temari.fitness.ui.review

import jp.temari.fitness.data.DailyActivity
import jp.temari.fitness.data.TimeBlock
import jp.temari.fitness.data.TimeBlockActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/** ふりかえりの「いちばん動いた」と、今週7日分の並べ方を確かめる。 */
class ReviewUiStateTest {

    private val thursday = LocalDate.of(2026, 10, 15)

    private fun day(date: LocalDate, morning: Int = 0, afternoon: Int = 0): DailyActivity {
        val blocks = listOf(
            TimeBlockActivity(TimeBlock.Morning, morning, 0),
            TimeBlockActivity(TimeBlock.Noon, 0, 0),
            TimeBlockActivity(TimeBlock.Afternoon, 0, afternoon),
            TimeBlockActivity(TimeBlock.Night, 0, 0),
        )
        return DailyActivity(date, morning, afternoon, morning * 10, 600, blocks)
    }

    @Test
    fun weekDays_areMondayToSundayWithGaps() {
        val week = listOf(day(thursday.minusDays(3), morning = 10), day(thursday, morning = 20))
        val days = ReviewUiState.weekDaysOf(thursday, week)

        assertEquals(7, days.size)
        assertEquals(DayOfWeek.MONDAY, days.first().date.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, days.last().date.dayOfWeek)
        assertEquals(10, days[0].activity?.totalPoints)
        assertNull("火曜は記録なし", days[1].activity)
        assertNull("金曜はまだ来ていない", days[4].activity)
    }

    @Test
    fun best_blockAndDay() {
        val today = day(thursday, morning = 50, afternoon = 80)
        val week = listOf(day(thursday.minusDays(2), morning = 300), today)
        val state = ReviewUiState(ReviewMode.Today, today, ReviewUiState.weekDaysOf(thursday, week))

        assertEquals(TimeBlock.Afternoon, state.bestBlock)
        assertEquals(DayOfWeek.TUESDAY, state.bestDay)
    }

    @Test
    fun best_isNullWhenNothing() {
        val today = day(thursday)
        val state = ReviewUiState(ReviewMode.Week, today, ReviewUiState.weekDaysOf(thursday, listOf(today)))
        assertNull(state.bestBlock)
        assertNull(state.bestDay)
    }
}
