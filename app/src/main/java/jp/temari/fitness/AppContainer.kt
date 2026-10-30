package jp.temari.fitness

import android.content.Context
import jp.temari.fitness.data.ActivityRepository
import jp.temari.fitness.data.SwitchingActivityRepository
import jp.temari.fitness.data.fake.FakeActivityRepository
import jp.temari.fitness.data.fake.ScenarioSwitcher
import jp.temari.fitness.data.sensing.ActivityStore
import jp.temari.fitness.data.sensing.SensorActivityRepository
import jp.temari.fitness.data.settings.DataSource
import jp.temari.fitness.data.settings.DataStoreSettingsRepository
import jp.temari.fitness.data.settings.SettingsRepository
import jp.temari.fitness.service.RecordingController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.Clock

/**
 * アプリ全体で1つだけ使う部品（リポジトリなど）をまとめて持つ入れ物。
 *
 * 各画面の ViewModel はここから部品を受け取る。画面は本物の計測かモックかを知らなくてよい。
 */
class AppContainer(context: Context) {
    /** 「今」を決める時計。 */
    val clock: Clock = Clock.systemDefaultZone()

    /** 設定（文字の大きさ・呼び名・目標・データの出どころ）の保存先。 */
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(context)

    private val goal: Flow<Int> = settingsRepository.settings.map { it.dailyGoal }.distinctUntilChanged()

    /** 本物の計測の記録（端末内のファイル）。記録するサービスと画面の両方から使う。 */
    val activityStore = ActivityStore(File(context.filesDir, "activity_log.csv"))

    /** 記録するサービスを始めたり止めたりする窓口。 */
    val recordingController = RecordingController(context.applicationContext, settingsRepository.settings)

    // せってい で目標を変えると、モックの「今日の目標」もすぐ変わるようにつなぐ。
    private val fakeRepository = FakeActivityRepository(clock = clock, goal = goal)

    private val sensorRepository = SensorActivityRepository(
        store = activityStore,
        goal = goal,
        clock = clock,
        startRecording = recordingController::start,
    )

    /** 今のデータの出どころ（本物の計測／モック）。 */
    val dataSource: Flow<DataSource> = settingsRepository.settings.map { it.dataSource }.distinctUntilChanged()

    /** 活動データの入口。設定に合わせて本物の計測かモックを使う。 */
    val activityRepository: ActivityRepository = SwitchingActivityRepository(dataSource, sensorRepository, fakeRepository)

    /** モックのシナリオ切り替え（スタッフ・開発用）。 */
    val scenarioSwitcher: ScenarioSwitcher = fakeRepository

    /** 利用者の呼び名。 */
    val nickname: Flow<String> = settingsRepository.settings.map { it.nickname }.distinctUntilChanged()
}
