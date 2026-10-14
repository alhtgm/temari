package jp.temari.fitness.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import jp.temari.fitness.R
import jp.temari.fitness.config.UiConfig

/**
 * いくつかの中から1つを選ぶ大きなボタンの並び（「きょう／今週」「大／特大」など）。
 *
 * 選ばれているボタンは塗りつぶしにし、文字の前に「✓」を付ける（色だけで伝えないため）。
 *
 * @param options 選べるもの。
 * @param selected 今選ばれているもの。
 * @param label 各ボタンに出す文字。
 * @param onSelect ボタンが押されたとき。
 * @param vertical true なら縦に並べる（選択肢が多いとき）。false なら横に並べる。
 */
@Composable
fun <T> ChoiceButtons(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
) {
    val button: @Composable (T, Modifier) -> Unit = { option, buttonModifier ->
        val isSelected = option == selected
        val text = label(option)
        BigButton(
            text = if (isSelected) stringResource(R.string.choice_selected_mark, text) else text,
            onClick = { onSelect(option) },
            primary = isSelected,
            modifier = buttonModifier.semantics { this.selected = isSelected },
        )
    }
    if (vertical) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(UiConfig.ButtonSpacing),
        ) {
            options.forEach { button(it, Modifier) }
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(UiConfig.ButtonSpacing),
        ) {
            options.forEach { button(it, Modifier.weight(1f)) }
        }
    }
}
