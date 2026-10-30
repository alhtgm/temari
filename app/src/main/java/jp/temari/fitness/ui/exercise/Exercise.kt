package jp.temari.fitness.ui.exercise

import androidx.annotation.StringRes
import jp.temari.fitness.R

/**
 * 座ってできる体操の種類。内容は仮のもので、施設の専門職に確認してもらう。
 *
 * @property emoji 飾りの絵文字。
 * @property title 名前。
 * @property howTo やり方の説明。
 */
enum class Exercise(
    @StringRes val emoji: Int,
    @StringRes val title: Int,
    @StringRes val howTo: Int,
) {
    March(R.string.exercise_march_emoji, R.string.exercise_march_title, R.string.exercise_march_howto),
    HeelRaise(R.string.exercise_heel_emoji, R.string.exercise_heel_title, R.string.exercise_heel_howto),
    Shoulder(R.string.exercise_shoulder_emoji, R.string.exercise_shoulder_title, R.string.exercise_shoulder_howto),
    ArmsUp(R.string.exercise_arms_emoji, R.string.exercise_arms_title, R.string.exercise_arms_howto),
    Breath(R.string.exercise_breath_emoji, R.string.exercise_breath_title, R.string.exercise_breath_howto),
}
