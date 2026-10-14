package jp.temari.fitness.ui.home

import jp.temari.fitness.data.DailyActivity
import jp.temari.fitness.data.RecordingStatus
import jp.temari.fitness.data.TimeBlock
import jp.temari.fitness.data.TimeBlockActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** ホームのひとことメッセージ（SPEC_UI.md 5章）とあいさつの選び方を確かめる。 */
class HomeMessageTest {

    private val date = LocalDate.of(2026, 10, 15)

    /** 時間帯ごとの活動ポイント（すべて「歩いた」）から1日分を作る。目標は 600。 */
    private fun day(morning: Int = 0, noon: Int = 0, afternoon: Int = 0, night: Int = 0): DailyActivity {
        val blocks = listOf(
            TimeBlockActivity(TimeBlock.Morning, morning, 0),
            TimeBlockActivity(TimeBlock.Noon, noon, 0),
            TimeBlockActivity(TimeBlock.Afternoon, afternoon, 0),
            TimeBlockActivity(TimeBlock.Night, night, 0),
        )
        val walk = blocks.sumOf { it.walkPoints }
        return DailyActivity(date, walk, 0, walk * 10, 600, blocks)
    }

    private val recording = RecordingStatus.Recording
    private val stopped = RecordingStatus.Stopped(LocalDateTime.of(date, LocalTime.of(10, 0)))
    private val threePm = LocalTime.of(15, 0)

    @Test
    fun stopped_comesFirstEvenIfGoalAchieved() {
        assertEquals(HomeMessage.ResumeRecording, HomeMessage.select(day(morning = 700), stopped, threePm))
    }

    @Test
    fun goalAchieved() {
        assertEquals(HomeMessage.GoalAchieved, HomeMessage.select(day(morning = 600), recording, threePm))
    }

    @Test
    fun nearGoal_from80Percent() {
        assertEquals(HomeMessage.NearGoal, HomeMessage.select(day(morning = 480), recording, threePm))
    }

    @Test
    fun bestBlock_isTheLargestUntilNow() {
        assertEquals(
            HomeMessage.BestBlock(TimeBlock.Afternoon),
            HomeMessage.select(day(morning = 100, afternoon = 200), recording, threePm),
        )
    }

    @Test
    fun bestBlock_ignoresBlocksNotStartedYet() {
        // 11時の時点では、午後はまだ始まっていない
        assertEquals(
            HomeMessage.BestBlock(TimeBlock.Morning),
            HomeMessage.select(day(morning = 100, afternoon = 200), recording, LocalTime.of(11, 0)),
        )
    }

    @Test
    fun keepGoing_whenNothingYet() {
        assertEquals(HomeMessage.KeepGoing, HomeMessage.select(day(), recording, threePm))
    }

    @Test
    fun greeting_byHour() {
        assertEquals(Greeting.Morning, Greeting.of(LocalTime.of(7, 0)))
        assertEquals(Greeting.Day, Greeting.of(LocalTime.of(10, 0)))
        assertEquals(Greeting.Evening, Greeting.of(LocalTime.of(18, 0)))
        assertEquals(Greeting.Evening, Greeting.of(LocalTime.of(2, 0)))
    }
}
