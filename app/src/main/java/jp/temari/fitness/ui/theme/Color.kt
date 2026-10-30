package jp.temari.fitness.ui.theme

import androidx.compose.ui.graphics.Color

// 色は「手まり」の和の配色（藍・朱・若竹・山吹）をもとに、今風のやわらかい色合いにしている。
// 文字を置く色は、白い文字・黒い文字とのコントラストが十分になる濃さにしている（WCAG の AA 以上）。

/** ボタンなど主な色（藍色）。白い文字を十分読める濃さ。 */
val TemariBlue = Color(0xFF2F3E9E)

/** 主な色の上に置く文字の色。 */
val OnTemariBlue = Color(0xFFFFFFFF)

/** 主な色のうすい版。カードの地や、選んでいないボタンの地に使う。 */
val TemariBlueLight = Color(0xFFE6E9FB)

/** 2番目の色（若竹色）。「家のこと」や「できた」を表す。 */
val TemariGreen = Color(0xFF1F7A4D)

/** 2番目の色のうすい版。 */
val TemariGreenLight = Color(0xFFDDF3E6)

/** 3番目の色（朱色）。お祝いやアクセントに使う。白い文字も読める濃さ。 */
val TemariVermilion = Color(0xFFC2410C)

/** 朱色のうすい版。 */
val TemariVermilionLight = Color(0xFFFFE8DA)

/** 4番目の色（山吹色）。★ や紙ふぶきに使う。文字の色には使わない（白地では薄いため）。 */
val TemariYellow = Color(0xFFF2A900)

/** 山吹色のうすい版。 */
val TemariYellowLight = Color(0xFFFFF4D1)

/** 画面の背景（あたたかみのある白）。 */
val BackgroundWhite = Color(0xFFFFF9F2)

/** カードの面（白）。背景と少しだけ違う色にして、影と合わせて浮いて見せる。 */
val CardWhite = Color(0xFFFFFFFF)

/** 本文の文字の色。 */
val TextBlack = Color(0xFF1C1B1F)

/** カードなど背景と区別したい面の色。 */
val SurfaceGray = Color(0xFFF1EEF6)

/** 目立たせたくない文字や枠線の色（背景とのコントラストは確保する）。 */
val QuietGray = Color(0xFF5A5766)

/** 「記録が止まっています」など、気づいてほしいところの枠の色（赤は否定的に見えるので濃いオレンジ）。 */
val AttentionOrange = Color(0xFFA14A00)

/** 気づいてほしいところの背景（うすいオレンジ）。 */
val AttentionBackground = Color(0xFFFFF1E0)

/** グラフの「家のこと」の地の色（うすい緑）。この上に濃い緑の斜線を引く。 */
val LifeGreenLight = Color(0xFFCDEBD9)

/** ホームの大きなカードのグラデーション（上側）。白い文字が読める濃さ。 */
val HeroGradientStart = Color(0xFF2F3E9E)

/** ホームの大きなカードのグラデーション（下側）。 */
val HeroGradientEnd = Color(0xFF5B3A9E)

/** 手まりちゃん（キャラクター）や紙ふぶきに使う色の並び。 */
val TemariPalette = listOf(TemariVermilion, TemariYellow, TemariGreen, TemariBlue, Color(0xFFE05297))
