package jp.temari.fitness.data

import jp.temari.fitness.data.settings.DataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest

/**
 * 設定に合わせて、本物の計測とモックのどちらかに処理を任せる [ActivityRepository]。
 *
 * 画面はこのクラスを普通の ActivityRepository として使うだけでよく、切り替えを知らなくてよい。
 * スタッフ・開発用画面で切り替えると、すべての画面にすぐ反映される。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SwitchingActivityRepository(
    private val dataSource: Flow<DataSource>,
    private val sensor: ActivityRepository,
    private val mock: ActivityRepository,
) : ActivityRepository {

    private fun pick(source: DataSource): ActivityRepository = when (source) {
        DataSource.Sensor -> sensor
        DataSource.Mock -> mock
    }

    // flatMapLatest: 設定が変わったら、前の出どころを見るのをやめて新しい方を見る。
    private fun <T> follow(select: ActivityRepository.() -> Flow<T>): Flow<T> =
        dataSource.flatMapLatest { pick(it).select() }

    override fun todayActivity(): Flow<DailyActivity> = follow { todayActivity() }

    override fun weekActivities(): Flow<List<DailyActivity>> = follow { weekActivities() }

    override fun recordingStatus(): Flow<RecordingStatus> = follow { recordingStatus() }

    override fun bingoCard(): Flow<BingoCard> = follow { bingoCard() }

    override fun summary(): Flow<ActivitySummary> = follow { summary() }

    override suspend fun resumeRecording() {
        pick(dataSource.first()).resumeRecording()
    }

    override suspend fun recordExercise() {
        pick(dataSource.first()).recordExercise()
    }
}
