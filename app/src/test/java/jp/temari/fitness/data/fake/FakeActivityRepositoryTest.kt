package jp.temari.fitness.data.fake

import jp.temari.fitness.config.ActivityConfig
import jp.temari.fitness.data.RecordingStatus
import jp.temari.fitness.data.TimeBlock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * [FakeActivityRepository] がシナリオごとに SPEC_UI.md 6章どおりの値を返すかを確かめる。
 *
 * 「今」は 2026年10月15日（木）15:00 に固定する（月・火・水・木の4日分が今週に入る）。
 */
class FakeActivityRepositoryTest {

    private val zone = ZoneId.of("Asia/Tokyo")
    private val thursday: LocalDate = LocalDate.of(2026, 10, 15)

    private fun clockAt(hour: Int, date: LocalDate = thursday): Clock =
        Clock.fixed(date.atTime(hour, 0).atZone(zone).toInstant(), zone)

    private fun repository(
        scenario: MockScenario,
        hour: Int = 15,
        goal: Int = ActivityConfig.DEFAULT_DAILY_GOAL,
    ) = FakeActivityRepository(clockAt(hour), flowOf(goal)).apply { setScenario(scenario) }

    @Test
    fun normal_isAbout70PercentWithoutBingo() = runTest {
        val repo = repository(MockScenario.Normal)
        val today = repo.todayActivity().first()

        assertEquals(thursday, today.date)
        assertTrue("70% 前後のはず: ${today.progress}", today.progress in 0.65f..0.75f)
        assertFalse(today.isGoalAchieved)
        // 朝と午後に山がある
        val morning = today.block(TimeBlock.Morning).totalPoints
        val noon = today.block(TimeBlock.Noon).totalPoints
        val afternoon = today.block(TimeBlock.Afternoon).totalPoints
        assertTrue(morning > noon && afternoon > noon)
        // 15時なので、夜の時間帯はまだ 0
        assertEquals(0, today.block(TimeBlock.Night).totalPoints)
        assertEquals(RecordingStatus.Recording, repo.recordingStatus().first())
        assertFalse(repo.bingoCard().first().hasBingo)
    }

    @Test
    fun normal_weekSkipsDayWithoutRecord() = runTest {
        val week = repository(MockScenario.Normal).weekActivities().first()
        // 水曜は記録なし。月・火・木（今日）の3日。
        assertEquals(
            listOf(thursday.minusDays(3), thursday.minusDays(2), thursday),
            week.map { it.date },
        )
    }

    @Test
    fun active_exceedsGoalAndHasBingo() = runTest {
        val repo = repository(MockScenario.Active)
        val today = repo.todayActivity().first()
        val week = repo.weekActivities().first()

        assertTrue(today.isGoalAchieved)
        assertTrue("今週の達成日が多いはず", week.count { it.isGoalAchieved } >= 3)
        assertTrue(repo.bingoCard().first().hasBingo)
    }

    @Test
    fun low_isAbout20Percent() = runTest {
        val today = repository(MockScenario.Low).todayActivity().first()
        assertTrue("20% 前後のはず: ${today.progress}", today.progress in 0.15f..0.25f)
    }

    @Test
    fun stopped_hasNoDataAfterNoonAndCanResume() = runTest {
        val repo = repository(MockScenario.Stopped)
        val today = repo.todayActivity().first()

        assertTrue(today.block(TimeBlock.Morning).totalPoints > 0)
        assertEquals(0, today.block(TimeBlock.Noon).totalPoints)
        assertEquals(0, today.block(TimeBlock.Afternoon).totalPoints)
        assertEquals(
            RecordingStatus.Stopped(LocalDateTime.of(thursday, java.time.LocalTime.of(10, 0))),
            repo.recordingStatus().first(),
        )

        repo.resumeRecording()
        assertEquals(RecordingStatus.Recording, repo.recordingStatus().first())

        // シナリオを選び直すと、また止まった状態から始まる
        repo.setScenario(MockScenario.Stopped)
        assertTrue(repo.recordingStatus().first() is RecordingStatus.Stopped)
    }

    @Test
    fun firstDay_hasLittleDataAndNoPastDays() = runTest {
        val repo = repository(MockScenario.FirstDay)
        val today = repo.todayActivity().first()
        val week = repo.weekActivities().first()

        assertTrue(today.totalPoints > 0)
        assertTrue(today.progress < 0.1f)
        assertEquals(listOf(thursday), week.map { it.date })
    }

    @Test
    fun earlyMorning_keepsScenarioTotalInMorningOnly() = runTest {
        val today = repository(MockScenario.Normal, hour = 8).todayActivity().first()
        // 8時なので朝だけにデータがあり、それでも合計は目標の 70% 前後
        assertTrue(today.block(TimeBlock.Morning).totalPoints > 0)
        assertEquals(0, today.block(TimeBlock.Noon).totalPoints)
        assertTrue(today.progress in 0.65f..0.75f)
    }

    @Test
    fun goal_isTakenFromSettings() = runTest {
        val today = repository(MockScenario.Normal, goal = 1_000).todayActivity().first()
        assertEquals(1_000, today.goal)
        assertTrue(today.totalPoints in 650..750)
    }

    @Test
    fun steps_areWalkPointsTimesTen() = runTest {
        val today = repository(MockScenario.Normal).todayActivity().first()
        assertEquals(today.walkPoints * ActivityConfig.STEPS_PER_WALK_POINT, today.steps)
        assertEquals(today.walkPoints + today.lifePoints, today.totalPoints)
        assertEquals(today.blocks.sumOf { it.totalPoints }, today.totalPoints)
    }

    @Test
    fun scenario_switchIsReflectedImmediately() = runTest {
        val repo = repository(MockScenario.Normal)
        assertFalse(repo.todayActivity().first().isGoalAchieved)
        repo.setScenario(MockScenario.Active)
        assertEquals(MockScenario.Active, repo.scenario.value)
        assertTrue(repo.todayActivity().first().isGoalAchieved)
    }
}
