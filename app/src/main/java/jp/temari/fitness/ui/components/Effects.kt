package jp.temari.fitness.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import jp.temari.fitness.config.UiConfig
import jp.temari.fitness.ui.theme.TemariPalette
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * 数字が 0 から [target] まで増えていくアニメーションの、今の値。
 *
 * 画面を開いたときに数字が動くと、見ていて楽しく、数字にも目が行きやすい。
 */
@Composable
fun rememberCountUp(target: Int): State<Int> {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(target) {
        animated.animateTo(target.toFloat(), tween(UiConfig.COUNT_UP_MILLIS, easing = FastOutSlowInEasing))
    }
    return remember { derivedStateOf { animated.value.roundToInt() } }
}

/** 紙ふぶきの1粒。位置は 0〜1 の割合で持ち、描くときに画面の大きさを掛ける。 */
private data class ConfettiPiece(
    val x: Float,
    val delay: Float,
    val speed: Float,
    val sway: Float,
    val spin: Float,
    val color: Color,
    val widthFactor: Float,
)

/**
 * 目標たっせい・ビンゴのときに降らせる紙ふぶき。上から下へ1回だけ降る。
 *
 * 下のボタンを押す邪魔をしないよう、押す操作は受け取らない（Canvas は操作を横取りしない）。
 *
 * @param key この値が変わるたびに、もう一度降らせる。
 */
@Composable
fun Confetti(key: Any, modifier: Modifier = Modifier) {
    // 毎回同じ並びにならないよう、key が変わるたびに粒を作り直す。
    val pieces = remember(key) {
        val random = Random(key.hashCode())
        List(UiConfig.CONFETTI_COUNT) {
            ConfettiPiece(
                x = random.nextFloat(),
                delay = random.nextFloat() * 0.35f,
                speed = 0.8f + random.nextFloat() * 0.6f,
                sway = (random.nextFloat() - 0.5f) * 0.12f,
                spin = random.nextFloat() * 720f,
                color = TemariPalette[random.nextInt(TemariPalette.size)],
                widthFactor = 0.6f + random.nextFloat() * 0.8f,
            )
        }
    }
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        progress.animateTo(1f, tween(UiConfig.CONFETTI_MILLIS, easing = LinearEasing))
    }
    if (progress.value >= 1f) return
    Canvas(modifier = modifier.clearAndSetSemantics { }) {
        val pieceSize = size.minDimension * 0.025f
        pieces.forEach { piece ->
            // 出てくるのを少しずつずらし、それぞれの速さで落とす。
            val t = ((progress.value - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f) * piece.speed
            if (t <= 0f || t >= 1.1f) return@forEach
            val y = -pieceSize + t * (size.height + pieceSize * 2)
            val x = (piece.x + piece.sway * kotlin.math.sin(t * 12f)) * size.width
            rotate(piece.spin * t, pivot = Offset(x, y)) {
                drawRect(
                    color = piece.color,
                    topLeft = Offset(x - pieceSize / 2, y - pieceSize / 4),
                    size = Size(pieceSize * piece.widthFactor, pieceSize / 2),
                )
            }
        }
    }
}

/**
 * ゆっくり明るくなったり暗くなったりする点（「● 記録中」の点）。動いていることが一目で分かる。
 */
@Composable
fun PulsingDot(color: Color, size: Dp, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(UiConfig.PULSE_MILLIS), RepeatMode.Reverse),
        label = "alpha",
    )
    Canvas(modifier = modifier.size(size).clearAndSetSemantics { }) {
        drawCircle(color.copy(alpha = alpha))
    }
}
