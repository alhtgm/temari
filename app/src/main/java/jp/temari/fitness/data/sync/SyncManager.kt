package jp.temari.fitness.data.sync

import jp.temari.fitness.config.SyncConfig
import jp.temari.fitness.data.sensing.ActivityStore
import jp.temari.fitness.data.settings.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDateTime

/** 1回の送信の結果。 */
enum class SyncOutcome {
    /** 送り終えた。 */
    Success,

    /** 接続先（URL・鍵）が設定されていないので送らなかった。 */
    NotConfigured,

    /** 初回設定が済んでおらず、利用者IDが無いので送らなかった。 */
    NoUser,

    /** 通信に失敗した（ネットにつながらない・断られた など）。次の機会にもう一度送る。 */
    Failed,
}

/**
 * 送信の様子（スタッフ・開発用画面に出す）。
 *
 * @property configured 接続先が設定されているか。
 * @property syncing 今送っている途中か。
 * @property lastSyncedAt 最後に送り終えた日時。
 * @property pendingEpochs まだ送っていない 10 秒ごとの記録の件数。
 * @property lastError 最後の送信が失敗したときの理由。成功したら null。
 */
data class SyncState(
    val configured: Boolean,
    val syncing: Boolean = false,
    val lastSyncedAt: LocalDateTime? = null,
    val pendingEpochs: Int = 0,
    val lastError: String? = null,
)

/**
 * 端末の記録を Supabase に送る係。
 *
 * 1回の送信で行うこと:
 * 1. ログインしていなければ匿名ログインする（入場券の期限が近ければ新しくする）。
 * 2. 利用者の情報（よび名・目標・記録中か）を送る。
 * 3. 前回送った日の前日から今日までの、時間帯ごとの集計と体操の回数を送る（送ったあとに増えた分も上書きされる）。
 * 4. ためている 10 秒ごとの記録を、古い順に少しずつ送り、送れた分を待ち行列から消す。
 *
 * どこかで失敗したら、そこで止めて次の機会にやり直す。同じ行を何度送っても上書きになるだけなので、やり直しても重ならない。
 *
 * @param api Supabase との通信。null なら接続先が設定されていない（何も送らない）。
 */
class SyncManager(
    private val api: SupabaseApi?,
    private val stateStore: SyncStateStore,
    private val store: ActivityStore,
    private val queue: EpochQueue,
    private val settings: Flow<UserSettings>,
    private val clock: Clock,
) {
    private val payloads = SyncPayloads(clock.zone)

    // 自動の送信と「今すぐ送る」が同時に走らないようにする。
    private val mutex = Mutex()

    private val _state = MutableStateFlow(
        SyncState(configured = api != null, lastSyncedAt = stateStore.loadLastSyncedAt(), pendingEpochs = queue.size),
    )

    /** 送信の様子。 */
    val state: StateFlow<SyncState> = _state.asStateFlow()

    /** 送信の様子のうち、待ち行列の件数だけを新しくする（記録が増えたとき用）。 */
    fun refreshPendingCount() {
        _state.update { it.copy(pendingEpochs = queue.size) }
    }

    /** 今すぐ送る。 */
    suspend fun syncNow(): SyncOutcome = mutex.withLock {
        val client = api ?: return SyncOutcome.NotConfigured
        val current = settings.first()
        val userId = current.userId ?: return SyncOutcome.NoUser
        _state.update { it.copy(syncing = true) }
        try {
            val now = LocalDateTime.now(clock)
            val log = store.log.value
            withSession(client) { session ->
                client.upsert(
                    session,
                    "app_users",
                    "id",
                    payloads.user(
                        UserInfo(userId, current.nickname, current.dailyGoal, store.running.value, log.lastRecordedAt),
                        now,
                    ),
                )
            }
            // 前回送った日の前日からを送り直す（前回のあとに、その日の記録が増えているかもしれないため）。
            val since = stateStore.loadLastSyncedAt()?.toLocalDate()?.minusDays(SyncConfig.RESEND_RECENT_DAYS)
            val blocks = payloads.blocks(userId, log, since)
            if (blocks != EMPTY) withSession(client) { client.upsert(it, "block_records", "user_id,date,block", blocks) }
            val extras = payloads.extras(userId, log, since)
            if (extras != EMPTY) withSession(client) { client.upsert(it, "daily_extras", "user_id,date", extras) }
            sendEpochs(client, userId)
            stateStore.saveLastSyncedAt(now)
            _state.update { it.copy(syncing = false, lastSyncedAt = now, pendingEpochs = queue.size, lastError = null) }
            SyncOutcome.Success
        } catch (e: Exception) {
            _state.update { it.copy(syncing = false, pendingEpochs = queue.size, lastError = e.message ?: e.javaClass.simpleName) }
            SyncOutcome.Failed
        } finally {
            // 送れた分を待ち行列から消したので、ファイルにも反映しておく。
            queue.save()
        }
    }

    /** 待ち行列の 10 秒ごとの記録を、少しずつ全部送る。 */
    private suspend fun sendEpochs(client: SupabaseApi, userId: String) {
        while (true) {
            val batch = queue.peek(SyncConfig.EPOCHS_PER_REQUEST)
            if (batch.isEmpty()) return
            withSession(client) { client.upsert(it, "epochs", "user_id,recorded_at", payloads.epochs(userId, batch)) }
            queue.removeFirst(batch.size)
            _state.update { it.copy(pendingEpochs = queue.size) }
        }
    }

    /**
     * 使えるセッションを用意して [block] を行う。
     *
     * 入場券の期限が切れていた（401）ときは、1回だけ新しくしてからやり直す。
     */
    private suspend fun withSession(client: SupabaseApi, block: suspend (Session) -> Unit) {
        val session = validSession(client)
        try {
            block(session)
        } catch (e: SupabaseException) {
            if (e.status != UNAUTHORIZED) throw e
            block(renew(client, session))
        }
    }

    private suspend fun validSession(client: SupabaseApi): Session {
        val saved = stateStore.loadSession() ?: return newSession(client)
        val nowSeconds = clock.instant().epochSecond
        // 期限まで 1 分を切っていたら、先に新しくしておく。
        return if (saved.expiresAtEpochSeconds - nowSeconds < RENEW_MARGIN_SECONDS) renew(client, saved) else saved
    }

    private suspend fun renew(client: SupabaseApi, old: Session): Session = try {
        client.refresh(old.refreshToken).also { stateStore.saveSession(it) }
    } catch (e: SupabaseException) {
        // 更新用の券も使えなくなっていたら、ログインし直す（新しい端末として扱われる）。
        if (e.status in 400..499) newSession(client) else throw e
    }

    private suspend fun newSession(client: SupabaseApi): Session =
        client.signInAnonymously().also { stateStore.saveSession(it) }

    private companion object {
        const val EMPTY = "[]"
        const val UNAUTHORIZED = 401
        const val RENEW_MARGIN_SECONDS = 60L
    }
}
