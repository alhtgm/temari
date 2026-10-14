package jp.temari.fitness.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import jp.temari.fitness.R
import jp.temari.fitness.config.UiConfig
import jp.temari.fitness.ui.theme.AttentionOrange
import jp.temari.fitness.ui.theme.LifeGreenLight
import jp.temari.fitness.ui.theme.TemariBlue
import jp.temari.fitness.ui.theme.TemariGreen

/**
 * 積み上げ棒グラフの1本分。
 *
 * @property label 棒の下に出す名前（「朝」「月」など）。
 * @property walkPoints 歩いたポイント（棒の下側、青の塗りつぶし）。
 * @property lifePoints 家のことポイント（棒の上側、緑の斜線）。
 * @property achieved true なら棒の上に「★ たっせい」を出す。
 * @property description 読み上げ機能（TalkBack）で読む説明。
 */
data class ChartBar(
    val label: String,
    val walkPoints: Int,
    val lifePoints: Int,
    val achieved: Boolean = false,
    val description: String,
)

/**
 * 歩いた／家のこと を積み上げた棒グラフ。外部のライブラリを使わず Canvas で描く。
 *
 * 色だけで区別しないよう、家のこと側には斜線の模様を付ける。
 * 文字（名前・たっせい）は Canvas ではなく Text で出す。文字の大きさの設定がそのまま効くようにするため。
 */
@Composable
fun StackedBarChart(bars: List<ChartBar>, modifier: Modifier = Modifier) {
    // いちばん高い棒を基準に高さを決める。全部 0 のときに 0 で割らないよう 1 以上にする。
    val maxTotal = bars.maxOfOrNull { it.walkPoints + it.lifePoints }?.coerceAtLeast(1) ?: 1
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            // 棒の下端をそろえる（上の「たっせい」の行数が違っても棒の根元が並ぶ）。
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEach { bar ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics { contentDescription = bar.description },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (bar.achieved) {
                        Text(
                            text = stringResource(R.string.review_achieved_star),
                            style = MaterialTheme.typography.titleLarge,
                            color = AttentionOrange,
                        )
                        Text(
                            text = stringResource(R.string.review_achieved_text),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                    val fraction = (bar.walkPoints + bar.lifePoints).toFloat() / maxTotal
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth(UiConfig.CHART_BAR_WIDTH_FRACTION)
                            .height(UiConfig.ChartBarAreaHeight * fraction),
                    ) {
                        drawStackedBar(bar.walkPoints, bar.lifePoints)
                    }
                }
            }
        }
        // 棒の根元の線。
        HorizontalDivider(thickness = UiConfig.BorderWidth, color = MaterialTheme.colorScheme.outline)
        Row(modifier = Modifier.fillMaxWidth()) {
            bars.forEach { bar ->
                Text(
                    text = bar.label,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics { },
                )
            }
        }
    }
}

/** 凡例: 「歩いた」と「家のこと」の色・模様の見本。 */
@Composable
fun ChartLegend(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(UiConfig.SmallSpacing)) {
        LegendItem(text = stringResource(R.string.review_legend_walk)) {
            drawRect(TemariBlue)
        }
        LegendItem(text = stringResource(R.string.review_legend_life)) {
            drawHatchedRect(Offset.Zero, size)
        }
    }
}

@Composable
private fun LegendItem(text: String, drawSwatch: DrawScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UiConfig.SmallSpacing),
    ) {
        Canvas(modifier = Modifier.size(UiConfig.LegendSwatchSize), onDraw = drawSwatch)
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** 1本の棒を描く。下が歩いた（青）、上が家のこと（緑の斜線）。 */
private fun DrawScope.drawStackedBar(walkPoints: Int, lifePoints: Int) {
    val total = walkPoints + lifePoints
    if (total <= 0) return
    val walkHeight = size.height * walkPoints / total
    drawRect(
        color = TemariBlue,
        topLeft = Offset(0f, size.height - walkHeight),
        size = Size(size.width, walkHeight),
    )
    drawHatchedRect(Offset.Zero, Size(size.width, size.height - walkHeight))
}

/** うすい緑の地に、濃い緑の斜線を引いた四角を描く（「家のこと」の模様）。 */
private fun DrawScope.drawHatchedRect(topLeft: Offset, rectSize: Size) {
    if (rectSize.height <= 0f || rectSize.width <= 0f) return
    drawRect(LifeGreenLight, topLeft, rectSize)
    val spacing = UiConfig.HatchSpacing.toPx()
    val stroke = UiConfig.HatchStroke.toPx()
    // 四角の外にはみ出した線は切り取る。
    clipRect(topLeft.x, topLeft.y, topLeft.x + rectSize.width, topLeft.y + rectSize.height) {
        // 左下から右上へ 45 度の線を、左の外側から右の外側まで等間隔に引く。
        var x = topLeft.x - rectSize.height
        while (x < topLeft.x + rectSize.width) {
            drawLine(
                color = TemariGreen,
                start = Offset(x, topLeft.y + rectSize.height),
                end = Offset(x + rectSize.height, topLeft.y),
                strokeWidth = stroke,
            )
            x += spacing
        }
    }
    drawRect(TemariGreen, topLeft, rectSize, style = Stroke(width = stroke / 2))
}
