package jp.temari.fitness.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import jp.temari.fitness.data.settings.DataSource
import jp.temari.fitness.data.settings.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * 記録するサービス（[RecordingService]）を始めたり止めたりする窓口。
 *
 * 「いつ始めてよいか」（許可・初回設定・データの出どころ）の判断をここにまとめる。
 */
class RecordingController(
    private val context: Context,
    private val settings: Flow<UserSettings>,
) {
    /** 「身体活動」の許可（歩数計を読むのに必要）があるか。 */
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    /** サービスを始める。許可が無ければ何もしない（許可なしで始めると Android が例外を出すため）。 */
    fun start() {
        if (!hasPermission()) return
        try {
            ContextCompat.startForegroundService(context, Intent(context, RecordingService::class.java))
        } catch (e: IllegalStateException) {
            // バックグラウンドから始めることを Android が許さなかったとき。次にアプリを開いたときに始め直す。
            Log.w(TAG, "記録を始められませんでした", e)
        }
    }

    /** サービスを止める。 */
    fun stop() {
        context.stopService(Intent(context, RecordingService::class.java))
    }

    /** 初回設定が済んでいて、データの出どころが「本物の計測」なら始める。モックなら止める。 */
    suspend fun startIfReady() {
        val current = settings.first()
        when {
            !current.onboardingCompleted -> Unit
            current.dataSource == DataSource.Sensor -> start()
            else -> stop()
        }
    }

    private companion object {
        const val TAG = "RecordingController"
    }
}
