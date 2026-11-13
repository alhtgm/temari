package jp.temari.fitness.data.sync

import jp.temari.fitness.data.sensing.ActivityStore
import jp.temari.fitness.data.sensing.EpochResult
import jp.temari.fitness.data.settings.UserSettings
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** [SyncManager] の送り方（ログイン・送る順番・失敗したときのやり直し）を、偽物の通信で確かめる。 */
class SyncManagerTest {

    private val zone = ZoneId.of("Asia/Tokyo")
    private val now: LocalDateTime = LocalDate.of(2026, 11, 13).atTime(12, 0)
    private val clock = Clock.fixed(now.atZone(zone).toInstant(), zone)

    /** 送られたものを覚えておく偽物の通信。 */
    private class FakeApi : SupabaseApi {
        val calls = mutableListOf<String>()
        var signIns = 0
        var refreshes = 0
        var failTable: String? = null
        var unauthorizedOnce = false

        override suspend fun signInAnonymously(): Session {
            signIns++
            return Session("access$signIns", "refresh$signIns", Long.MAX_VALUE)
        }

        override suspend fun refresh(refreshToken: String): Session {
            refreshes++
            return Session("renewed$refreshes", "refresh-r$refreshes", Long.MAX_VALUE)
        }

        override suspend fun upsert(session: Session, table: String, onConflict: String, json: String) {
            if (unauthorizedOnce) {
                unauthorizedOnce = false
                throw SupabaseException(401, "expired")
            }
            if (table == failTable) throw SupabaseException(503, "down")
            // 「表:使った入場券:行数」を覚える。行数は { の数で数える。
            calls += "$table:${session.accessToken}:${json.count { it == '{' }}"
        }
    }

    private class MemoryStateStore : SyncStateStore {
        var session: Session? = null
        var last: LocalDateTime? = null
        override fun loadSession() = session
        override fun saveSession(session: Session) {
            this.session = session
        }
        override fun loadLastSyncedAt() = last
        override fun saveLastSyncedAt(at: LocalDateTime) {
            last = at
        }
    }

    private data class Setup(val manager: SyncManager, val queue: EpochQueue, val stateStore: MemoryStateStore)

    private fun setup(api: SupabaseApi?, userId: String? = "u1"): Setup {
        val queue = EpochQueue(null)
        val store = ActivityStore(null, queue)
        store.addEpoch(now.minusMinutes(1), EpochResult(5, 0, 2f, true))
        store.addEpoch(now, EpochResult(0, 10, 40f, false))
        val stateStore = MemoryStateStore()
        val manager = SyncManager(api, stateStore, store, queue, flowOf(UserSettings(userId = userId)), clock)
        return Setup(manager, queue, stateStore)
    }

    @Test
    fun firstSyncSignsInAndSendsEverything() = runTest {
        val api = FakeApi()
        val s = setup(api)
        assertEquals(SyncOutcome.Success, s.manager.syncNow())
        assertEquals(1, api.signIns)
        // 11:59 と 12:00 はどちらも「昼」なので、時間帯の集計は1行。10 秒ごとの記録は2行。
        assertEquals(listOf("app_users:access1:1", "block_records:access1:1", "epochs:access1:2"), api.calls)
        assertEquals(0, s.queue.size)
        assertEquals(now, s.stateStore.last)
        assertEquals(0, s.manager.state.value.pendingEpochs)
    }

    @Test
    fun failureKeepsQueueForNextTime() = runTest {
        val api = FakeApi().apply { failTable = "epochs" }
        val s = setup(api)
        assertEquals(SyncOutcome.Failed, s.manager.syncNow())
        assertEquals(2, s.queue.size)
        assertEquals(null, s.stateStore.last)
        assertEquals("down", s.manager.state.value.lastError)

        api.failTable = null
        assertEquals(SyncOutcome.Success, s.manager.syncNow())
        assertEquals(0, s.queue.size)
        // 2回目は保存したセッションを使い、ログインし直さない。
        assertEquals(1, api.signIns)
    }

    @Test
    fun expiredTokenIsRenewedOnce() = runTest {
        val api = FakeApi().apply { unauthorizedOnce = true }
        val s = setup(api)
        s.stateStore.session = Session("old", "oldRefresh", Long.MAX_VALUE)
        assertEquals(SyncOutcome.Success, s.manager.syncNow())
        assertEquals(1, api.refreshes)
        assertEquals("renewed1", s.stateStore.session!!.accessToken)
        assertEquals(0, api.signIns)
    }

    @Test
    fun notConfiguredOrNoUserDoesNothing() = runTest {
        assertEquals(SyncOutcome.NotConfigured, setup(null).manager.syncNow())
        val api = FakeApi()
        assertEquals(SyncOutcome.NoUser, setup(api, userId = null).manager.syncNow())
        assertTrue(api.calls.isEmpty())
    }
}
