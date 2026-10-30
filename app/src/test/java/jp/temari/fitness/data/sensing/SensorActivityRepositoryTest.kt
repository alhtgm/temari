package jp.temari.fitness.data.sensing

import jp.temari.fitness.data.RecordingStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** [SensorActivityRepository] が記録を画面の形に直すかを確かめる。今は 2026-10-29（木）15:00。 */
class SensorActivityRepositoryTest {

    private val zone = ZoneId.of("Asia/Tokyo")
    private val today: LocalDate = LocalDate.of(2026, 10, 29)
    private val clock = Clock.fixed(today.atTime(15, 0).atZone(zone).toInstant(), zone)

    private fun repo(store: ActivityStore, onStart: () -> Unit = {}) =
        SensorActivityRepository(store, flowOf(600), clock, onStart)

    @Test
    fun todayAndWeekComeFromStore() = runTest {
        val store = ActivityStore(null)
        store.addEpoch(today.atTime(9, 0), EpochResult(3000, 0, 0f, true))
        store.addEpoch(today.minusDays(1).atTime(9, 0), EpochResult(7000, 0, 0f, true))
        val r = repo(store)
        assertEquals(300, r.todayActivity().first().walkPoints)
        // 月(26)〜木(29) のうち、記録があるのは 水・木。
        assertEquals(listOf(today.minusDays(1), today), r.weekActivities().first().map { it.date })
        val summary = r.summary().first()
        assertEquals(10_000, summary.totalSteps)
        assertEquals(2, summary.streakDays)
    }

    @Test
    fun weekAlwaysContainsTodayEvenWithoutRecord() = runTest {
        val week = repo(ActivityStore(null)).weekActivities().first()
        assertEquals(listOf(today), week.map { it.date })
    }

    @Test
    fun statusFollowsService() = runTest {
        val store = ActivityStore(null)
        val r = repo(store)
        assertTrue(r.recordingStatus().first() is RecordingStatus.Stopped)
        store.setRunning(true)
        assertEquals(RecordingStatus.Recording, r.recordingStatus().first())
    }

    @Test
    fun resumeStartsServiceAndExerciseIsRecorded() = runTest {
        var started = false
        val store = ActivityStore(null)
        val r = repo(store) { started = true }
        r.resumeRecording()
        r.recordExercise()
        assertTrue(started)
        assertEquals(1, r.summary().first().exercisesToday)
    }
}
