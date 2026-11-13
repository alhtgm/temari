package jp.temari.fitness.data.sync

import jp.temari.fitness.data.sensing.ActivityLog
import jp.temari.fitness.data.sensing.EpochResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** [SyncPayloads] が Supabase の表に合う JSON を作るかを確かめる。 */
class SyncPayloadsTest {

    private val payloads = SyncPayloads(ZoneId.of("Asia/Tokyo"))
    private val day: LocalDate = LocalDate.of(2026, 11, 13)

    @Test
    fun userRowHasTimezoneAndEscapedNickname() {
        val json = payloads.user(
            UserInfo("u1", "は\"な\\こ", 600, true, day.atTime(10, 0)),
            day.atTime(10, 5),
        )
        assertEquals(
            "{\"id\":\"u1\",\"nickname\":\"は\\\"な\\\\こ\",\"daily_goal\":600,\"recording\":true," +
                "\"last_recorded_at\":\"2026-11-13T10:00+09:00\",\"last_synced_at\":\"2026-11-13T10:05+09:00\"}",
            json,
        )
    }

    @Test
    fun userRowWithoutRecordIsNull() {
        val json = payloads.user(UserInfo("u1", "", 600, false, null), day.atTime(10, 5))
        assertTrue(json.contains("\"last_recorded_at\":null"))
    }

    @Test
    fun blocksOnlySinceGivenDay() {
        val log = ActivityLog()
            .plusEpoch(day.minusDays(3).atTime(8, 0), EpochResult(10, 0, 0f, true))
            .plusEpoch(day.atTime(15, 0), EpochResult(20, 30, 0f, true))
        assertEquals(
            "[{\"user_id\":\"u1\",\"date\":\"2026-11-13\",\"block\":\"Afternoon\",\"steps\":20,\"life_seconds\":30}]",
            payloads.blocks("u1", log, since = day.minusDays(1)),
        )
        // 一度も送っていなければ全部。
        assertEquals(2, Regex("\"block\"").findAll(payloads.blocks("u1", log, since = null)).count())
    }

    @Test
    fun extrasSkipDaysWithoutExercise() {
        val log = ActivityLog().plusEpoch(day.atTime(8, 0), EpochResult(1, 0, 0f, false)).plusExercise(day)
        assertEquals(
            "[{\"user_id\":\"u1\",\"date\":\"2026-11-13\",\"exercises\":1}]",
            payloads.extras("u1", log, since = null),
        )
        assertEquals("[]", payloads.extras("u1", ActivityLog(), since = null))
    }

    @Test
    fun epochsUseDotDecimal() {
        val json = payloads.epochs("u1", listOf(EpochRecord(day.atTime(9, 0, 10), 3, 10, 31.456f, false)))
        assertEquals(
            "[{\"user_id\":\"u1\",\"recorded_at\":\"2026-11-13T09:00:10+09:00\",\"steps\":3," +
                "\"life_seconds\":10,\"enmo_mg\":31.46,\"walking\":false}]",
            json,
        )
    }

    @Test
    fun controlCharactersAreEscaped() {
        assertEquals("\"a\\nb\\u0001\"", SyncPayloads.str("a\nb\u0001"))
    }
}
