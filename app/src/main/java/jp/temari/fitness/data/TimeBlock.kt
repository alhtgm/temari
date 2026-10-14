package jp.temari.fitness.data

import jp.temari.fitness.config.ActivityConfig

/**
 * 1日を4つに分けた時間帯（SPEC_UI.md 4章）。
 *
 * @property startHour この時間帯が始まる時（この時を含む）。
 * @property endHour この時間帯が終わる時（この時は含まない）。
 */
enum class TimeBlock(val startHour: Int, val endHour: Int) {
    /** 朝 5:00〜10:00 */
    Morning(ActivityConfig.MORNING_START_HOUR, ActivityConfig.NOON_START_HOUR),

    /** 昼 10:00〜14:00 */
    Noon(ActivityConfig.NOON_START_HOUR, ActivityConfig.AFTERNOON_START_HOUR),

    /** 午後 14:00〜18:00 */
    Afternoon(ActivityConfig.AFTERNOON_START_HOUR, ActivityConfig.NIGHT_START_HOUR),

    /** 夜 18:00〜24:00 */
    Night(ActivityConfig.NIGHT_START_HOUR, ActivityConfig.NIGHT_END_HOUR),
    ;

    companion object {
        /** [hour] 時が含まれる時間帯を返す。0〜5時のように、どこにも入らないときは null。 */
        fun of(hour: Int): TimeBlock? = entries.firstOrNull { hour >= it.startHour && hour < it.endHour }
    }
}
