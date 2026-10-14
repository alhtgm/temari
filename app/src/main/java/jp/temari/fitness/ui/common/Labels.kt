package jp.temari.fitness.ui.common

import androidx.annotation.StringRes
import jp.temari.fitness.R
import jp.temari.fitness.data.TimeBlock

/** 時間帯の名前（朝・昼・午後・夜）の文字列。 */
@get:StringRes
val TimeBlock.labelRes: Int
    get() = when (this) {
        TimeBlock.Morning -> R.string.block_morning
        TimeBlock.Noon -> R.string.block_noon
        TimeBlock.Afternoon -> R.string.block_afternoon
        TimeBlock.Night -> R.string.block_night
    }
