package jp.temari.fitness

import jp.temari.fitness.data.ActivityRepository
import jp.temari.fitness.data.fake.FakeActivityRepository
import jp.temari.fitness.data.fake.MockScenario
import jp.temari.fitness.data.fake.ScenarioSwitcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Clock

/**
 * アプリ全体で1つだけ使う部品（リポジトリなど）をまとめて持つ入れ物。
 *
 * 各画面の ViewModel はここから部品を受け取る。本物の計測に替えるときは、ここの1行を替えるだけでよい。
 */
class AppContainer {
    /** 「今」を決める時計。 */
    val clock: Clock = Clock.systemDefaultZone()

    private val fakeRepository = FakeActivityRepository(clock).apply {
        setScenario(INITIAL_SCENARIO)
    }

    /** 活動データの入口。今はモック。 */
    val activityRepository: ActivityRepository = fakeRepository

    /** モックのシナリオ切り替え（スタッフ・開発用）。 */
    val scenarioSwitcher: ScenarioSwitcher = fakeRepository

    /** 利用者の呼び名。保存のしくみは step06 で作るので、今は空（名前なしのあいさつになる）。 */
    val nickname: Flow<String> = flowOf("")

    private companion object {
        /** 起動時のシナリオ。ホームの見た目を確かめるときは、ここを一時的に書き換える。 */
        val INITIAL_SCENARIO = MockScenario.Normal
    }
}
