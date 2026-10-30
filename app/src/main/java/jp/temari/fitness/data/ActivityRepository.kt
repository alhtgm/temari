package jp.temari.fitness.data

import kotlinx.coroutines.flow.Flow

/**
 * 活動データの入口。画面はデータをこのインターフェースからだけ受け取る。
 *
 * 今はモック（[jp.temari.fitness.data.fake.FakeActivityRepository]）が実装している。
 * 後で本物の計測の実装に差し替えても、画面側のコードは変えなくてよい。
 *
 * どの関数も Flow（値が変わるたびに新しい値を流す仕組み）で返すので、画面は常に最新の値を表示できる。
 */
interface ActivityRepository {
    /** 今日の活動。 */
    fun todayActivity(): Flow<DailyActivity>

    /** 今週（月曜〜今日）の、記録が取れた日の活動。日付の古い順。今日も含む。 */
    fun weekActivities(): Flow<List<DailyActivity>>

    /** 記録の状態。 */
    fun recordingStatus(): Flow<RecordingStatus>

    /** 今週のビンゴカード。 */
    fun bingoCard(): Flow<BingoCard>

    /** 日をまたいだまとめ（これまでの歩数・つづけて記録した日数・今日の体操の回数）。 */
    fun summary(): Flow<ActivitySummary>

    /** 止まっている記録を再開する。 */
    suspend fun resumeRecording()

    /** 「たいそう」を1回終えたことを記録する。 */
    suspend fun recordExercise()
}
