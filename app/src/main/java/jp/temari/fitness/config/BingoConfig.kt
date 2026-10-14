package jp.temari.fitness.config

/**
 * ビンゴのマスの条件に使うしきい値（SPEC_UI.md 3.3）。
 *
 * どれも **仮の値**。実際の計測が始まって、利用者の普段の値が分かってから決め直す。
 */
object BingoConfig {
    /** 「朝からげんき」: 朝の時間帯の活動ポイントがこれ以上の日があれば達成。（仮） */
    const val MORNING_ACTIVE_POINTS = 200

    /** 「家のことマスター」: 家のことポイントがこれ以上の日があれば達成。（仮） */
    const val LIFE_MASTER_POINTS = 250

    /** 「夜もすこし」: 夜の時間帯の活動ポイントがこれ以上の日があれば達成（少しでも動けばよい）。（仮） */
    const val NIGHT_ANY_POINTS = 1

    /** 「よく歩いた日」: 歩いたポイントがこれ以上の日があれば達成。（仮） */
    const val WALK_GOOD_POINTS = 450

    /** 「目標たっせい×3」: 目標を達成した日がこの日数以上なら達成。 */
    const val GOAL_DAYS_MANY = 3

    /** 「3日つづけて記録」: 記録が取れた日がこの日数続けば達成。 */
    const val RECORD_STREAK_DAYS = 3
}
