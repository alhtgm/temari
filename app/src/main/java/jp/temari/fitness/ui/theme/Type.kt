package jp.temari.fitness.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import jp.temari.fitness.config.TextSizeConfig
import jp.temari.fitness.config.TextSizeOption

/**
 * 文字の大きさの設定に合わせた [Typography]（文字スタイルの一式）を作る。
 *
 * 画面側は MaterialTheme.typography.bodyLarge などを使うだけで、大／特大が自動で反映される。
 */
fun temariTypography(option: TextSizeOption): Typography {
    val display = style(TextSizeConfig.DISPLAY_SP, option, FontWeight.Bold)
    val headline = style(TextSizeConfig.HEADLINE_SP, option, FontWeight.Bold)
    val title = style(TextSizeConfig.TITLE_SP, option, FontWeight.Bold)
    val body = style(TextSizeConfig.BODY_SP, option, FontWeight.Normal)
    val small = style(TextSizeConfig.SMALL_SP, option, FontWeight.Normal)
    // Material の文字スタイルは種類が多いので、どれを使っても規定の大きさを下回らないよう全部埋める。
    return Typography(
        displayLarge = display,
        displayMedium = display,
        displaySmall = display,
        headlineLarge = headline,
        headlineMedium = headline,
        headlineSmall = headline,
        titleLarge = title,
        titleMedium = title,
        titleSmall = title,
        bodyLarge = body,
        bodyMedium = body,
        bodySmall = small,
        labelLarge = title,
        labelMedium = small,
        labelSmall = small,
    )
}

/** 基準の大きさに倍率を掛けた文字スタイルを作る。 */
private fun style(baseSp: Float, option: TextSizeOption, weight: FontWeight): TextStyle {
    val size = baseSp * option.scale
    return TextStyle(
        fontSize = size.sp,
        lineHeight = (size * TextSizeConfig.LINE_HEIGHT_RATIO).sp,
        fontWeight = weight,
    )
}
