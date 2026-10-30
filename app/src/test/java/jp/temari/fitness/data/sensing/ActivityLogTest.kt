package jp.temari.fitness.data.sensing

import jp.temari.fitness.data.TimeBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/** [ActivityLog] の足し算・保存形式を確かめる。 */
class ActivityLogTest {

    private val day: LocalDate = LocalDate.of(2026, 10, 30)
    private fun epoch(steps: Int, life: Int) = EpochResult(steps, life, 0f, steps > 0)

    @Test
    fun epochsGoIntoTimeBlocks() {
        val log = ActivityLog()
            .plusEpoch(day.atTime(8, 0), epoch(20, 0))
            .plusEpoch(day.atTime(9, 59), epoch(5, 10))
            .plusEpoch(day.atTime(15, 0), epoch(0, 10))
        val record = log.days.getValue(day)
        assertEquals(BlockRecord(25, 10), record.blocks[TimeBlock.Morning])
        assertEquals(BlockRecord(0, 10), record.blocks[TimeBlock.Afternoon])
        assertEquals(day.atTime(15, 0), log.lastRecordedAt)
    }

    @Test
    fun earlyMorningIsNotCountedButUpdatesLastTime() {
        val at = day.atTime(3, 0)
        val log = ActivityLog().plusEpoch(at, epoch(100, 10))
        assertTrue(log.days.isEmpty())
        assertEquals(at, log.lastRecordedAt)
    }

    @Test
    fun encodeDecodeRoundTrip() {
        val log = ActivityLog()
            .plusEpoch(day.atTime(8, 0), epoch(1234, 600))
            .plusEpoch(day.minusDays(1).atTime(19, 0), epoch(10, 20))
            .plusExercise(day)
            .plusExercise(day)
        assertEquals(log, ActivityLog.decode(ActivityLog.encode(log)))
    }

    @Test
    fun brokenLinesAreSkipped() {
        val text = "temari-log-v1\nblock,2026-10-30,Morning,10,20\nblock,こわれた行\nex,2026-10-30,x\n"
        val log = ActivityLog.decode(text)
        assertEquals(BlockRecord(10, 20), log.days.getValue(day).blocks[TimeBlock.Morning])
    }

    @Test
    fun pointsConversion() {
        val log = ActivityLog().plusEpoch(day.atTime(8, 0), epoch(1000, 600))
        val activity = log.days.getValue(day).toDailyActivity(day, goal = 600)
        // 1000 歩 ÷ 10 = 100、600 秒 ÷ 20 = 30。
        assertEquals(100, activity.walkPoints)
        assertEquals(30, activity.lifePoints)
        assertEquals(1000, activity.steps)
    }

    @Test
    fun prunedDropsOldDays() {
        val log = ActivityLog()
            .plusEpoch(day.minusDays(200).atTime(8, 0), epoch(1, 0))
            .plusEpoch(LocalDateTime.of(day, java.time.LocalTime.of(8, 0)), epoch(1, 0))
        assertEquals(setOf(day), log.pruned(day, keepDays = 120).days.keys)
    }
}
