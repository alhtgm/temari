package jp.temari.fitness.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import jp.temari.fitness.config.TextSizeOption

// 端末の壁紙から色を作る機能（ダイナミックカラー）やダークモードは使わない。
// 色が端末ごとに変わるとコントラストが保証できず、スタッフの説明とも見た目が食い違うため。
private val TemariColors = lightColorScheme(
    primary = TemariBlue,
    onPrimary = OnTemariBlue,
    secondary = TemariGreen,
    onSecondary = OnTemariBlue,
    background = BackgroundWhite,
    onBackground = TextBlack,
    surface = BackgroundWhite,
    onSurface = TextBlack,
    surfaceVariant = SurfaceGray,
    onSurfaceVariant = TextBlack,
    outline = QuietGray,
)

/**
 * アプリ全体のテーマ（色と文字の大きさ）。
 *
 * @param textSize アプリ内設定の文字の大きさ。今は既定値の「大」。せってい画面（step06）で切り替えられるようにする。
 */
@Composable
fun TemariTheme(
    textSize: TextSizeOption = TextSizeOption.Large,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = TemariColors,
        typography = temariTypography(textSize),
        content = content,
    )
}
