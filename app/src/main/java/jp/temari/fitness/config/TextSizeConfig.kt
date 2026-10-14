package jp.temari.fitness.config

/**
 * 文字の大きさに関する定数。単位はすべて sp。
 *
 * ここに書くのは「大」のときの大きさ。「特大」は [TextSizeOption.ExtraLarge] の倍率を掛けて作る。
 * sp は端末の文字サイズ設定でも拡大されるので、最終的な大きさは「アプリ内設定 × 端末設定」になる。
 */
object TextSizeConfig {
    /** ホームの活動ポイントなど、いちばん大きな数字（ルール: 48sp 以上）。 */
    const val DISPLAY_SP = 56f

    /** 画面のタイトル。 */
    const val HEADLINE_SP = 30f

    /** 見出しやボタンの文字。 */
    const val TITLE_SP = 24f

    /** 本文（ルール: 20sp 以上）。 */
    const val BODY_SP = 22f

    /** 補足など小さめの文字。これでも本文の下限 20sp は守る。 */
    const val SMALL_SP = 20f

    /** 行の高さ = 文字の大きさ × この値。漢字とかなが詰まって見えないよう少し広めにする。 */
    const val LINE_HEIGHT_RATIO = 1.4f
}

/**
 * アプリ内の「文字の大きさ」設定（大／特大）。
 *
 * @property scale [TextSizeConfig] の大きさに掛ける倍率。
 */
enum class TextSizeOption(val scale: Float) {
    /** 大（初期値）。 */
    Large(1.0f),

    /** 特大。 */
    ExtraLarge(1.25f),
}
