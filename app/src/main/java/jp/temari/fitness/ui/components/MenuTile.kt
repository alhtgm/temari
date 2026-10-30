package jp.temari.fitness.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import jp.temari.fitness.config.UiConfig

/**
 * ホームのメニューの大きなタイル（絵文字＋文字）。押すとその画面へ進む。
 *
 * ボタンのルール（高さ 64dp 以上・文字を必ず付ける）を守ったうえで、絵文字を大きく出して何の画面か分かりやすくする。
 *
 * @param emoji 上に大きく出す絵文字（飾り）。
 * @param text タイルの名前。読み上げ機能はこれを読む。
 * @param color タイルの地の色。
 * @param contentColor 文字の色。地の色とのコントラストを十分にとる。
 */
@Composable
fun MenuTile(
    emoji: String,
    text: String,
    color: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .clearAndSetSemantics {
                contentDescription = text
                role = Role.Button
            },
        shape = MaterialTheme.shapes.large,
        color = color,
        contentColor = contentColor,
        shadowElevation = UiConfig.CardElevation,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // 高さの最低を中身の側に付けると、絵文字と文字がタイルの上下の真ん中に来る。
                .heightIn(min = UiConfig.MenuTileMinHeight)
                .padding(UiConfig.SmallSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UiConfig.SmallSpacing / 2, Alignment.CenterVertically),
        ) {
            Text(text = emoji, fontSize = UiConfig.TILE_EMOJI_SP.sp)
            Text(text = text, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }
    }
}
