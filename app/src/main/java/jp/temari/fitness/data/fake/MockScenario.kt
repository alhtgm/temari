package jp.temari.fitness.data.fake

import kotlinx.coroutines.flow.StateFlow

/** モックデータのシナリオ（SPEC_UI.md 6章）。 */
enum class MockScenario {
    /** ふつうの日（初期値）。目標の 70% 程度。朝と午後に山。 */
    Normal,

    /** よく動いた日。目標を超えている。今週の達成日が多く、ビンゴがそろう。 */
    Active,

    /** あまり動けなかった日。目標の 20% 程度。 */
    Low,

    /** 記録が止まった。昼から記録停止中。 */
    Stopped,

    /** はじめての日。今日のデータがわずかで、今週の過去データなし。 */
    FirstDay,
}

/**
 * シナリオを切り替える窓口。スタッフ・開発用画面だけが使う。
 *
 * 本物の計測にはシナリオが無いので、[jp.temari.fitness.data.ActivityRepository] とは分けている。
 */
interface ScenarioSwitcher {
    /** 今のシナリオ。 */
    val scenario: StateFlow<MockScenario>

    /** シナリオを切り替える。すべての画面にすぐ反映される。 */
    fun setScenario(scenario: MockScenario)
}
