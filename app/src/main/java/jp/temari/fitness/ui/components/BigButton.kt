package jp.temari.fitness.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import jp.temari.fitness.config.UiConfig

/**
 * 高さ 64dp 以上・横いっぱいの大きなボタン。
 *
 * 文字は必ず付ける（アイコンだけのボタンは作らない）ため、引数で文字を受け取る。
 *
 * @param text ボタンに表示する文字。
 * @param onClick 押されたときの処理。
 * @param primary true なら塗りつぶし（主な操作）、false なら枠線だけ（控えめな操作）。
 * @param emoji 文字の前に付ける絵文字（飾り）。null なら付けない。
 */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    emoji: String? = null,
) {
    // heightIn(min) にしておくと、特大の文字で2行になってもボタンが伸びて文字が切れない。
    val buttonModifier = modifier
        .fillMaxWidth()
        .heightIn(min = UiConfig.MinButtonHeight)
    val label: @Composable () -> Unit = {
        Text(
            text = if (emoji != null) "$emoji  $text" else text,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
    }
    if (primary) {
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            shape = MaterialTheme.shapes.medium,
            // 少しだけ影を付けて、押せるものだと分かりやすくする。
            elevation = ButtonDefaults.buttonElevation(defaultElevation = UiConfig.CardElevation),
        ) { label() }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(UiConfig.BorderWidth, MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
        ) { label() }
    }
}
