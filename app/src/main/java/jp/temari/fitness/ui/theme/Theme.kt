package jp.temari.fitness.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import jp.temari.fitness.config.TextSizeOption
import jp.temari.fitness.config.UiConfig

// 端末の壁紙から色を作る機能（ダイナミックカラー）やダークモードは使わない。
// 色が端末ごとに変わるとコントラストが保証できず、スタッフの説明とも見た目が食い違うため。
private val TemariColors = lightColorScheme(
    primary = TemariBlue,
    onPrimary = OnTemariBlue,
    primaryContainer = TemariBlueLight,
    onPrimaryContainer = TemariBlue,
    secondary = TemariGreen,
    onSecondary = OnTemariBlue,
    secondaryContainer = TemariGreenLight,
    onSecondaryContainer = TextBlack,
    tertiary = TemariVermilion,
    onTertiary = OnTemariBlue,
    tertiaryContainer = TemariVermilionLight,
    onTertiaryContainer = TextBlack,
    background = BackgroundWhite,
    onBackground = TextBlack,
    surface = CardWhite,
    onSurface = TextBlack,
    surfaceVariant = SurfaceGray,
    onSurfaceVariant = TextBlack,
    outline = QuietGray,
)

// 今風の見た目にするため、角を大きく丸める。
private val TemariShapes = Shapes(
    small = RoundedCornerShape(UiConfig.CornerRadius),
    medium = RoundedCornerShape(UiConfig.CornerRadius),
    large = RoundedCornerShape(UiConfig.CardCornerRadius),
    extraLarge = RoundedCornerShape(UiConfig.CardCornerRadius),
)

/**
 * アプリ全体のテーマ（色・形・文字の大きさ）。
 *
 * @param textSize アプリ内設定の文字の大きさ。
 */
@Composable
fun TemariTheme(
    textSize: TextSizeOption = TextSizeOption.Large,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = TemariColors,
        typography = temariTypography(textSize),
        shapes = TemariShapes,
        content = content,
    )
}
