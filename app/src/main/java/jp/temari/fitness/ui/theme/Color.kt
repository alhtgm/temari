package jp.temari.fitness.ui.theme

import androidx.compose.ui.graphics.Color

// 背景と文字のコントラストを強くするため、文字はほぼ黒、背景はほぼ白にしている。
// 主な色（濃い青）も白い文字が十分読める濃さにしている。

/** ボタンなど主な色（濃い青）。 */
val TemariBlue = Color(0xFF0B4A86)

/** 主な色の上に置く文字の色。 */
val OnTemariBlue = Color(0xFFFFFFFF)

/** 2番目の色（濃い緑）。 */
val TemariGreen = Color(0xFF1E6B3A)

/** 画面の背景（少しあたたかみのある白）。 */
val BackgroundWhite = Color(0xFFFFFDF8)

/** 本文の文字の色。 */
val TextBlack = Color(0xFF1A1A1A)

/** カードなど背景と区別したい面の色。 */
val SurfaceGray = Color(0xFFEEF1F5)

/** 目立たせたくない文字や枠線の色（背景とのコントラストは確保する）。 */
val QuietGray = Color(0xFF555555)

/** 「記録が止まっています」など、気づいてほしいところの枠の色（赤は否定的に見えるので濃いオレンジ）。 */
val AttentionOrange = Color(0xFFA14A00)

/** 気づいてほしいところの背景（うすいオレンジ）。 */
val AttentionBackground = Color(0xFFFFF1E0)

/** グラフの「家のこと」の地の色（うすい緑）。この上に濃い緑の斜線を引く。 */
val LifeGreenLight = Color(0xFFCDE9D5)
