package jp.temari.fitness.data.fake

import jp.temari.fitness.config.ActivityConfig
import jp.temari.fitness.data.TimeBlock

/**
 * 1日の中で、どの時間帯にどれだけ動いたかの割合（4つ足すと 1.0 になるようにする）。
 */
internal data class DayShape(
    val morning: Float,
    val noon: Float,
    val afternoon: Float,
    val night: Float,
) {
    /** 時間帯ごとの割合。 */
    fun weight(block: TimeBlock): Float = when (block) {
        TimeBlock.Morning -> morning
        TimeBlock.Noon -> noon
        TimeBlock.Afternoon -> afternoon
        TimeBlock.Night -> night
    }
}

/**
 * 1つのシナリオで作るデータの形。
 *
 * @property todayRatio 今日の合計の、目標に対する割合。
 * @property todayShape 今日の時間帯ごとの割合。
 * @property walkShare 「歩いた」の割合（残りが「家のこと」）。
 * @property pastRatios 月曜から順に、その日の合計の目標に対する割合。null は「記録なし」。今日以降の分は使わない。
 * @property pastShape 過去の日の時間帯ごとの割合。
 * @property stopHour 記録が止まった時刻（時）。null なら記録中。
 */
internal data class ScenarioProfile(
    val todayRatio: Float,
    val todayShape: DayShape,
    val walkShare: Float,
    val pastRatios: List<Float?>,
    val pastShape: DayShape,
    val stopHour: Int? = null,
)

/**
 * 各シナリオの値（SPEC_UI.md 6章）。数字はモック用の仮のもの。
 *
 * ビンゴのしきい値（BingoConfig）と合わせて、
 * 「ふつうの日」ではビンゴがそろわず、「よく動いた日」ではそろうように選んでいる。
 */
internal object MockProfiles {
    /** 朝と午後に山がある1日。 */
    private val MorningAndAfternoon = DayShape(morning = 0.45f, noon = 0.10f, afternoon = 0.45f, night = 0f)

    /** 時間帯ごとの差が小さい1日。 */
    private val Even = DayShape(morning = 0.25f, noon = 0.25f, afternoon = 0.35f, night = 0.15f)

    /** 朝から夜までよく動いた1日。 */
    private val Busy = DayShape(morning = 0.40f, noon = 0.15f, afternoon = 0.30f, night = 0.15f)

    /** 朝と昼だけ少し動いた1日（はじめての日）。 */
    private val FirstMorning = DayShape(morning = 0.5f, noon = 0.5f, afternoon = 0f, night = 0f)

    private val Normal = ScenarioProfile(
        todayRatio = 0.70f,
        todayShape = MorningAndAfternoon,
        walkShare = 0.65f,
        pastRatios = listOf(0.9f, 1.05f, null, 0.8f, 0.6f, 0.9f),
        pastShape = Even,
    )

    private val Active = ScenarioProfile(
        todayRatio = 1.2f,
        todayShape = Busy,
        walkShare = 0.55f,
        pastRatios = listOf(1.1f, 1.3f, 1.0f, 1.2f, 0.9f, 1.15f),
        pastShape = Busy,
    )

    private val Low = ScenarioProfile(
        todayRatio = 0.2f,
        todayShape = Even,
        walkShare = 0.6f,
        pastRatios = listOf(0.3f, null, 0.5f, 0.2f, 0.4f, 0.3f),
        pastShape = Even,
    )

    // 記録が止まった日: ふつうの日と同じ形で、昼（10時）から止まっている。
    private val Stopped = Normal.copy(stopHour = ActivityConfig.NOON_START_HOUR)

    private val FirstDay = ScenarioProfile(
        todayRatio = 0.05f,
        todayShape = FirstMorning,
        walkShare = 0.6f,
        pastRatios = emptyList(),
        pastShape = Even,
    )

    /** シナリオに対応する値。 */
    fun of(scenario: MockScenario): ScenarioProfile = when (scenario) {
        MockScenario.Normal -> Normal
        MockScenario.Active -> Active
        MockScenario.Low -> Low
        MockScenario.Stopped -> Stopped
        MockScenario.FirstDay -> FirstDay
    }
}
