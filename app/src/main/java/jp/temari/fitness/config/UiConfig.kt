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

    /** ボタンや小さなカードの角の丸み。 */
    val CornerRadius = 20.dp

    /** 大きなカードの角の丸み。 */
    val CardCornerRadius = 28.dp

    /** カードの影の高さ。浮いて見える程度に小さくする。 */
    val CardElevation = 3.dp

    /** カードの内側の余白。 */
    val CardPadding = 20.dp

    /** ホームのメニューのタイル（ふりかえり・ビンゴなど）の最低の高さ。 */
    val MenuTileMinHeight = 112.dp

    /** タイルなどに出す絵文字の大きさ（sp）。絵文字は飾りなので、文字の大きさ設定では大きくしない。 */
    const val TILE_EMOJI_SP = 36f

    /** ホームの進み具合の輪の太さ。 */
    val RingStroke = 18.dp

    /** ホームの進み具合の輪の大きさ（直径）。数字は輪の外に出すので、特大の文字でも輪の大きさは変えない。 */
    val RingSize = 184.dp

    /** 輪の中に入れる手まりちゃんの大きさ。 */
    val MascotInRing = 124.dp

    /** 手まりちゃん（キャラクター）の大きさ（ホームの上）。 */
    val MascotSmall = 72.dp

    /** 手まりちゃんの大きさ（たいそう・初回設定など大きく出すとき）。 */
    val MascotLarge = 140.dp

    /** 手まりちゃんがぴょんぴょん弾む高さ。 */
    val MascotBounce = 6.dp

    /** 手まりちゃんが1回弾むのにかかる時間（ミリ秒）。ゆっくりにして、落ち着いた動きにする。 */
    const val MASCOT_BOUNCE_MILLIS = 1_400

    /** 数字が 0 から増えていくアニメーションの時間（ミリ秒）。 */
    const val COUNT_UP_MILLIS = 1_200

    /** 紙ふぶきの粒の数。 */
    const val CONFETTI_COUNT = 70

    /** 紙ふぶきが落ちきるまでの時間（ミリ秒）。 */
    const val CONFETTI_MILLIS = 3_500

    /** 「● 記録中」の点がゆっくり明るくなったり暗くなったりする時間（ミリ秒）。 */
    const val PULSE_MILLIS = 1_200

    /** グラフの棒が伸びるアニメーションの時間（ミリ秒）。 */
    const val CHART_GROW_MILLIS = 700

    /** 枠線の太さ。 */
    val BorderWidth = 2.dp

    /** 目立たせたい枠（「記録が止まっています」など）の線の太さ。 */
    val AttentionBorderWidth = 4.dp

    /** ホームの「目標までの進み具合」バーの高さ。 */
    val ProgressBarHeight = 28.dp

    /** 「ポイント」の文字を、大きな数字の下のラインに揃えて見せるための下の余白。 */
    val UnitBaselineOffset = 12.dp

    /** グラフの棒の部分の高さ（いちばん高い棒がこの高さになる）。 */
    val ChartBarAreaHeight = 220.dp

    /** 棒の太さ（1本分の幅に対する割合）。 */
    const val CHART_BAR_WIDTH_FRACTION = 0.7f

    /** 「家のこと」の斜線の間隔。 */
    val HatchSpacing = 10.dp

    /** 「家のこと」の斜線の太さ。 */
    val HatchStroke = 3.dp

    /** 凡例（色と模様の見本）の四角の大きさ。 */
    val LegendSwatchSize = 28.dp

    /** ビンゴのマスの最低の高さ（文字が多いときは伸びる）。 */
    val BingoCellMinHeight = 110.dp

    /** ビンゴのマスの絵文字の大きさ（sp）。 */
    const val BINGO_EMOJI_SP = 28f

    /** ビンゴのそろった列に引く線の太さ。 */
    val BingoLineWidth = 12.dp

    /** ビンゴの線の不透明度。線の下のマスの文字が少し透けて読めるようにする。 */
    const val BINGO_LINE_ALPHA = 0.75f

    /** ビンゴの線が伸びていくアニメーションの時間（ミリ秒）。 */
    const val BINGO_LINE_ANIMATION_MILLIS = 900

    /** 「ビンゴ！」の文字が大きくなるアニメーションの始まりの大きさ（倍率）。 */
    const val BINGO_TEXT_START_SCALE = 0.3f

    /** 「スタッフ用」を長押しと判定するまでの時間（ミリ秒）。利用者がうっかり入らないよう 2 秒にしている。 */
    const val STAFF_LONG_PRESS_MILLIS = 2_000L
}
