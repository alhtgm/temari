package jp.temari.fitness.data.settings

import jp.temari.fitness.config.ActivityConfig
import jp.temari.fitness.config.TextSizeOption
import kotlinx.coroutines.flow.Flow

/**
 * 利用者ごとの設定。
 *
 * @property textSize アプリ内の文字の大きさ（大／特大）。
 * @property nickname 呼び名。まだ決めていなければ空。
 * @property dailyGoal 1日の目標ポイント。
 * @property userId 利用者ID（UUID）。初回設定で発行する。まだなら null。
 */
data class UserSettings(
    val textSize: TextSizeOption = TextSizeOption.Large,
    val nickname: String = "",
    val dailyGoal: Int = ActivityConfig.DEFAULT_DAILY_GOAL,
    val userId: String? = null,
)

/**
 * 設定の保存と読み出しの窓口。
 *
 * 画面は保存のしかた（DataStore）を知らなくてよいように、インターフェースにしている。
 */
interface SettingsRepository {
    /** 今の設定。保存されるたびに新しい値が流れる。 */
    val settings: Flow<UserSettings>

    /** 文字の大きさを保存する。 */
    suspend fun setTextSize(textSize: TextSizeOption)

    /** 呼び名を保存する。前後の空白は取り除き、長すぎる分は切る。 */
    suspend fun setNickname(nickname: String)

    /** 1日の目標を保存する。下限・上限の外なら範囲内に収める。 */
    suspend fun setDailyGoal(goal: Int)
}
