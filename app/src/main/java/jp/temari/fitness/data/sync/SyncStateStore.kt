package jp.temari.fitness.data.sync

import android.content.Context
import androidx.core.content.edit
import java.time.LocalDateTime

/**
 * 送信のために覚えておくこと（ログインの状態と、最後に送った日時）の保存先。
 *
 * テストでは端末に保存しない偽物に差し替える。
 */
interface SyncStateStore {
    /** 保存しているセッション。まだログインしていなければ null。 */
    fun loadSession(): Session?

    /** セッションを保存する。 */
    fun saveSession(session: Session)

    /** 最後に送り終えた日時。まだ送っていなければ null。 */
    fun loadLastSyncedAt(): LocalDateTime?

    /** 最後に送り終えた日時を保存する。 */
    fun saveLastSyncedAt(at: LocalDateTime)
}

/**
 * Android の SharedPreferences（小さな値を保存するしくみ）を使う [SyncStateStore]。
 *
 * 設定（DataStore）とは別のファイルにして、「初回設定のやり直し」でログインの状態まで消えないようにしている。
 * 同じ端末は同じ匿名ログインのまま使い続け、利用者IDだけが新しくなる。
 */
class PrefsSyncStateStore(context: Context) : SyncStateStore {
    private val prefs = context.applicationContext.getSharedPreferences("supabase_sync", Context.MODE_PRIVATE)

    override fun loadSession(): Session? {
        val access = prefs.getString(KEY_ACCESS, null) ?: return null
        val refresh = prefs.getString(KEY_REFRESH, null) ?: return null
        return Session(access, refresh, prefs.getLong(KEY_EXPIRES, 0L))
    }

    override fun saveSession(session: Session) {
        prefs.edit {
            putString(KEY_ACCESS, session.accessToken)
            putString(KEY_REFRESH, session.refreshToken)
            putLong(KEY_EXPIRES, session.expiresAtEpochSeconds)
        }
    }

    override fun loadLastSyncedAt(): LocalDateTime? =
        prefs.getString(KEY_LAST_SYNCED, null)?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

    override fun saveLastSyncedAt(at: LocalDateTime) {
        prefs.edit { putString(KEY_LAST_SYNCED, at.toString()) }
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_EXPIRES = "expires_at"
        const val KEY_LAST_SYNCED = "last_synced_at"
    }
}
