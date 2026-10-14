package jp.temari.fitness.config

import androidx.compose.ui.unit.dp

/**
 * 画面の寸法に関する定数。
 *
 * 高齢の利用者でも押しやすく、見やすい大きさを CLAUDE.md の「UI のルール」に合わせて決めている。
 */
object UiConfig {
    /** 内容の最大幅。タブレットでもスマホと同じ見た目で確認できるように、これより広げない。 */
    val MaxContentWidth = 480.dp

    /** ボタンの最低の高さ（ルール: 64dp 以上）。 */
    val MinButtonHeight = 64.dp

    /** ボタン同士の間隔（ルール: 16dp 以上）。 */
    val ButtonSpacing = 16.dp

    /** 画面の端と内容のあいだの余白。 */
    val ScreenPadding = 16.dp

    /** 文字と文字のあいだなど、小さめの余白。 */
    val SmallSpacing = 8.dp

    /** カードの角の丸み。 */
    val CornerRadius = 16.dp

    /** 枠線の太さ。 */
    val BorderWidth = 2.dp

    /** 目立たせたい枠（「記録が止まっています」など）の線の太さ。 */
    val AttentionBorderWidth = 4.dp

    /** ホームの「目標までの進み具合」バーの高さ。 */
    val ProgressBarHeight = 28.dp

    /** 「ポイント」の文字を、大きな数字の下のラインに揃えて見せるための下の余白。 */
    val UnitBaselineOffset = 12.dp

    /** 「スタッフ用」を長押しと判定するまでの時間（ミリ秒）。利用者がうっかり入らないよう 2 秒にしている。 */
    const val STAFF_LONG_PRESS_MILLIS = 2_000L
}
