package jp.temari.fitness.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import jp.temari.fitness.config.UiConfig
import jp.temari.fitness.ui.theme.TemariPalette
import jp.temari.fitness.ui.theme.TextBlack
import kotlin.math.PI
import kotlin.math.sin

/** 手まりちゃんの表情。 */
enum class MascotMood {
    /** おだやか（ふだん）。 */
    Calm,

    /** にっこり（よく動けた）。 */
    Happy,

    /** 大よろこび（目標たっせい）。 */
    Excited,
}

/**
 * アプリのキャラクター「手まりちゃん」。日本の手まりを顔つきのまるい形にしたもの。
 *
 * 外部の画像を使わず Canvas で描く。ゆっくり上下に弾んで、画面に動きを出す。
 * 飾りなので、読み上げ機能では読まない。
 *
 * @param mood 表情。
 * @param size 直径。
 * @param bounce true なら上下に弾む。
 */
@Composable
fun Mascot(
    mood: MascotMood,
    modifier: Modifier = Modifier,
    size: Dp = UiConfig.MascotSmall,
    bounce: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "mascot")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(UiConfig.MASCOT_BOUNCE_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "bounce",
    )
    val bounceHeight = UiConfig.MascotBounce
    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                if (bounce) {
                    // sin の正の半分だけを使い、「地面から跳ねる」動きにする。
                    translationY = -bounceHeight.toPx() * sin(phase * PI).toFloat()
                }
            }
            .clearAndSetSemantics { },
    ) {
        drawTemariBall()
        drawFace(mood)
    }
}

/** 手まりの模様（色の帯と、ひし形のかがり模様）を描く。 */
private fun DrawScope.drawTemariBall() {
    val radius = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val ball = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center, radius)) }
    drawCircle(Color(0xFFFFF7EC), radius, center)
    clipPath(ball) {
        // 放射状の色の帯（手まりの「かがり」）。
        val bands = TemariPalette.size * 2
        for (i in 0 until bands) {
            rotate(degrees = i * 360f / bands, pivot = center) {
                drawLine(
                    color = TemariPalette[i % TemariPalette.size].copy(alpha = 0.55f),
                    start = center,
                    end = Offset(center.x, center.y - radius),
                    strokeWidth = radius * 0.22f,
                )
            }
        }
        // 真ん中の帯（赤い帯の上に白い線）。
        drawRect(
            color = TemariPalette[0],
            topLeft = Offset(0f, center.y - radius * 0.16f),
            size = Size(size.width, radius * 0.32f),
        )
        drawLine(
            color = Color.White,
            start = Offset(0f, center.y),
            end = Offset(size.width, center.y),
            strokeWidth = radius * 0.05f,
        )
    }
    // 顔を描く白い丸。
    drawCircle(Color(0xFFFFFBF5), radius * 0.62f, center)
    drawCircle(TextBlack.copy(alpha = 0.25f), radius, center, style = Stroke(width = radius * 0.04f))
}

/** 表情（目・口・ほっぺ）を描く。 */
private fun DrawScope.drawFace(mood: MascotMood) {
    val radius = size.minDimension / 2f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val eyeY = cy - radius * 0.12f
    val eyeDx = radius * 0.22f
    val stroke = radius * 0.07f
    val eyeColor = TextBlack

    if (mood == MascotMood.Excited) {
        // うれしくて目が「＾＾」になる。
        listOf(-1f, 1f).forEach { side ->
            val ex = cx + side * eyeDx
            val path = Path().apply {
                moveTo(ex - radius * 0.09f, eyeY + radius * 0.04f)
                quadraticTo(ex, eyeY - radius * 0.1f, ex + radius * 0.09f, eyeY + radius * 0.04f)
            }
            drawPath(path, eyeColor, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
    } else {
        drawCircle(eyeColor, radius * 0.065f, Offset(cx - eyeDx, eyeY))
        drawCircle(eyeColor, radius * 0.065f, Offset(cx + eyeDx, eyeY))
    }

    // ほっぺ。
    val cheek = Color(0xFFF7A1A1).copy(alpha = if (mood == MascotMood.Calm) 0.5f else 0.85f)
    drawCircle(cheek, radius * 0.09f, Offset(cx - radius * 0.36f, cy + radius * 0.1f))
    drawCircle(cheek, radius * 0.09f, Offset(cx + radius * 0.36f, cy + radius * 0.1f))

    // 口。表情によって笑い方を変える。
    val mouthY = cy + radius * 0.14f
    val mouthWidth = when (mood) {
        MascotMood.Calm -> radius * 0.14f
        MascotMood.Happy -> radius * 0.2f
        MascotMood.Excited -> radius * 0.22f
    }
    val mouthDepth = when (mood) {
        MascotMood.Calm -> radius * 0.08f
        MascotMood.Happy -> radius * 0.16f
        MascotMood.Excited -> radius * 0.26f
    }
    val mouth = Path().apply {
        moveTo(cx - mouthWidth, mouthY)
        quadraticTo(cx, mouthY + mouthDepth * 2f, cx + mouthWidth, mouthY)
    }
    if (mood == MascotMood.Excited) {
        // 大よろこびは口を開けて笑う。
        mouth.close()
        drawPath(mouth, Color(0xFFB3261E))
    } else {
        drawPath(mouth, eyeColor, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}
