package jp.temari.fitness.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import jp.temari.fitness.config.UiConfig

/**
 * 白い面に少し影を付けた、角の丸いカード。画面の中のまとまりはこれで囲む。
 *
 * @param title カードの見出し。null なら出さない。
 * @param emoji 見出しの前に付ける絵文字（飾り）。
 * @param color カードの面の色。
 */
@Composable
fun TemariCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    emoji: String? = null,
    color: Color = MaterialTheme.colorScheme.surface,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = color,
        shadowElevation = UiConfig.CardElevation,
    ) {
        Column(
            modifier = Modifier.padding(UiConfig.CardPadding),
            verticalArrangement = Arrangement.spacedBy(UiConfig.SmallSpacing),
            horizontalAlignment = horizontalAlignment,
        ) {
            if (title != null) {
                Text(
                    text = if (emoji != null) "$emoji $title" else title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
            }
            content()
        }
    }
}
