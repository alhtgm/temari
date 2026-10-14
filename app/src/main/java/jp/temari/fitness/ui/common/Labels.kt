package jp.temari.fitness.ui.common

import androidx.annotation.StringRes
import jp.temari.fitness.R
import jp.temari.fitness.data.TimeBlock
import java.time.DayOfWeek

/** 時間帯の名前（朝・昼・午後・夜）の文字列。 */
@get:StringRes
val TimeBlock.labelRes: Int
    get() = when (this) {
        TimeBlock.Morning -> R.string.block_morning
        TimeBlock.Noon -> R.string.block_noon
        TimeBlock.Afternoon -> R.string.block_afternoon
        TimeBlock.Night -> R.string.block_night
    }

/** 曜日の名前（月〜日）の文字列。 */
@get:StringRes
val DayOfWeek.labelRes: Int
    get() = when (this) {
        DayOfWeek.MONDAY -> R.string.day_mon
        DayOfWeek.TUESDAY -> R.string.day_tue
        DayOfWeek.WEDNESDAY -> R.string.day_wed
        DayOfWeek.THURSDAY -> R.string.day_thu
        DayOfWeek.FRIDAY -> R.string.day_fri
        DayOfWeek.SATURDAY -> R.string.day_sat
        DayOfWeek.SUNDAY -> R.string.day_sun
    }
