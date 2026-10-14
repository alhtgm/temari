package jp.temari.fitness.config

/**
 * 活動ポイントと目標に関する定数（SPEC_UI.md 4章）。
 */
object ActivityConfig {
    // ---- 時間帯の区切り（時）。end は「その時刻の手前まで」を表す。0〜5時は集計しない。 ----
    const val MORNING_START_HOUR = 5
    const val NOON_START_HOUR = 10
    const val AFTERNOON_START_HOUR = 14
    const val NIGHT_START_HOUR = 18
    const val NIGHT_END_HOUR = 24

    /**
     * 歩数をいくつで「歩いたポイント 1」とするか。
     * 仮の値（計算式は未定。モックでは「歩数 ÷ 10 = 歩いたポイント」）。
     */
    const val STEPS_PER_WALK_POINT = 10

    /** 1日の目標の初期値（ポイント）。仮の値。 */
    const val DEFAULT_DAILY_GOAL = 600

    /** 「−」「＋」で目標を変えるときの1回分の幅。 */
    const val GOAL_STEP = 50

    /** 目標の下限。 */
    const val MIN_DAILY_GOAL = 100

    /** 目標の上限。 */
    const val MAX_DAILY_GOAL = 2_000

    /** 「あと少しで目標です」と言う割合（目標の 80% 以上）。 */
    const val NEAR_GOAL_RATIO = 0.8f
}
