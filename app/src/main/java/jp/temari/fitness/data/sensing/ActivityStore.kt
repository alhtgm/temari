package jp.temari.fitness.data.sensing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * センサーの様子（スタッフ・開発用画面に出す）。
 *
 * @property hasStepCounter 歩数計センサーがあるか。null はまだ調べていない（記録を始めていない）。
 * @property lastEnmoMg 最後の区切りの揺れの平均（mg）。
 * @property lastEpochSteps 最後の区切りの歩数。
 */
data class SensorDiagnostics(
    val hasStepCounter: Boolean? = null,
    val lastEnmoMg: Float = 0f,
    val lastEpochSteps: Int = 0,
)

/**
 * 本物の計測の記録を、メモリーと端末内のファイルに持つ入れ物。
 *
 * 記録するサービス（RecordingService）と画面（SensorActivityRepository）は同じアプリの中で動くので、
 * この1つの入れ物を両方から使う。サービスが値を足すと、画面にすぐ反映される。
 *
 * @param file 保存するファイル。null ならファイルに保存しない（テスト用）。
 */
class ActivityStore(private val file: File?) {

    // ファイルは小さい（1日 4 行 × 120 日ほど）ので、作るときにすぐ読み込む。
    private val _log = MutableStateFlow(load())

    /** 今の記録。 */
    val log: StateFlow<ActivityLog> = _log.asStateFlow()

    private val _running = MutableStateFlow(false)

    /** 記録するサービスが動いているか。アプリが終了させられたら、次に起動したとき false から始まる。 */
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _diagnostics = MutableStateFlow(SensorDiagnostics())

    /** センサーの様子。 */
    val diagnostics: StateFlow<SensorDiagnostics> = _diagnostics.asStateFlow()

    /** 1区切りの結果を記録する。 */
    fun addEpoch(at: LocalDateTime, result: EpochResult) {
        _log.update { it.plusEpoch(at, result) }
        _diagnostics.update { it.copy(lastEnmoMg = result.meanEnmoMg, lastEpochSteps = result.steps) }
    }

    /** 「たいそう」を1回記録し、すぐ保存する（回数は少ないので、そのつど保存しても負担にならない）。 */
    suspend fun addExercise(date: LocalDate) {
        _log.update { it.plusExercise(date) }
        save(date)
    }

    /** サービスが動いているかを知らせる。 */
    fun setRunning(running: Boolean) {
        _running.value = running
    }

    /** 歩数計センサーがあるかを知らせる。 */
    fun setHasStepCounter(has: Boolean) {
        _diagnostics.update { it.copy(hasStepCounter = has) }
    }

    /** 今の記録をファイルに書き出す。古い日は消してから書く。 */
    suspend fun save(today: LocalDate) {
        val target = file ?: return
        val text = ActivityLog.encode(_log.updateAndGet { it.pruned(today) })
        withContext(Dispatchers.IO) {
            // いったん別のファイルに書いてから名前を替える。書いている途中でアプリが止まっても、前の記録が壊れないように。
            val temp = File(target.parentFile, target.name + ".tmp")
            temp.writeText(text)
            if (!temp.renameTo(target)) {
                target.delete()
                temp.renameTo(target)
            }
        }
    }

    private fun load(): ActivityLog {
        val source = file ?: return ActivityLog()
        return if (source.exists()) ActivityLog.decode(source.readText()) else ActivityLog()
    }
}
