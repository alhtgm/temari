package jp.temari.fitness

import android.content.Context
import jp.temari.fitness.data.ActivityRepository
import jp.temari.fitness.data.fake.FakeActivityRepository
import jp.temari.fitness.data.fake.ScenarioSwitcher
import jp.temari.fitness.data.settings.DataStoreSettingsRepository
import jp.temari.fitness.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock

/**
 * アプリ全体で1つだけ使う部品（リポジトリなど）をまとめて持つ入れ物。
 *
 * 各画面の ViewModel はここから部品を受け取る。本物の計測に替えるときは、ここの1行を替えるだけでよい。
 */
class AppContainer(context: Context) {
    /** 「今」を決める時計。 */
    val clock: Clock = Clock.systemDefaultZone()

    /** 設定（文字の大きさ・呼び名・目標）の保存先。 */
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(context)

    // せってい で目標を変えると、モックの「今日の目標」もすぐ変わるようにつなぐ。
    private val fakeRepository = FakeActivityRepository(
        clock = clock,
        goal = settingsRepository.settings.map { it.dailyGoal }.distinctUntilChanged(),
    )

    /** 活動データの入口。今はモック。 */
    val activityRepository: ActivityRepository = fakeRepository

    /** モックのシナリオ切り替え（スタッフ・開発用）。 */
    val scenarioSwitcher: ScenarioSwitcher = fakeRepository

    /** 利用者の呼び名。 */
    val nickname: Flow<String> = settingsRepository.settings.map { it.nickname }.distinctUntilChanged()
}
