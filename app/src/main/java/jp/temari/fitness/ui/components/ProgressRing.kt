package jp.temari.fitness.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import jp.temari.fitness.config.UiConfig

/**
 * 目標までの進み具合を表す輪（ドーナツ型のグラフ）。真ん中に [content] を置ける。
 *
 * 画面を開いたとき、0 から今の値まで輪が伸びていく。目標を超えたら輪はいっぱいで止める。
 *
 * @param progress 進み具合（1.0 で目標）。
 * @param trackColor 輪の下地の色。
 * @param colors 輪の色（グラデーション）。
 */
@Composable
fun ProgressRing(
    progress: Float,
    trackColor: Color,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    diameter: Dp = UiConfig.RingSize,
    content: @Composable BoxScope.() -> Unit,
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animated.animateTo(
            progress.coerceIn(0f, 1f),
            tween(UiConfig.COUNT_UP_MILLIS, easing = FastOutSlowInEasing),
        )
    }
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = UiConfig.RingStroke.toPx()
            // 線の太さの半分だけ内側に描かないと、輪の外側が切れてしまう。
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            val sweep = 360f * animated.value
            if (sweep > 0f) {
                drawArc(
                    // 時計の 12 時の位置（-90 度）から右回りに伸ばす。
                    brush = Brush.sweepGradient(colors + colors.first()),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}
