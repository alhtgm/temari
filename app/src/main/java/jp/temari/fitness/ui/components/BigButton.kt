package jp.temari.fitness.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
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
 */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
) {
    // heightIn(min) にしておくと、特大の文字で2行になってもボタンが伸びて文字が切れない。
    val buttonModifier = modifier
        .fillMaxWidth()
        .heightIn(min = UiConfig.MinButtonHeight)
    val label: @Composable () -> Unit = {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
    }
    if (primary) {
        Button(onClick = onClick, modifier = buttonModifier) { label() }
    } else {
        OutlinedButton(onClick = onClick, modifier = buttonModifier) { label() }
    }
}
