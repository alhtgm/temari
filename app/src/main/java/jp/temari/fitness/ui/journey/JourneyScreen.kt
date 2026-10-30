package jp.temari.fitness.ui.journey

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.temari.fitness.R
import jp.temari.fitness.config.JourneyConfig
import jp.temari.fitness.config.TextSizeOption
import jp.temari.fitness.config.UiConfig
import jp.temari.fitness.data.JourneyProgress
import jp.temari.fitness.ui.components.BigButton
import jp.temari.fitness.ui.components.Mascot
import jp.temari.fitness.ui.components.MascotMood
import jp.temari.fitness.ui.components.ScreenFrame
import jp.temari.fitness.ui.components.TemariCard
import jp.temari.fitness.ui.theme.TemariGreen
import jp.temari.fitness.ui.theme.TemariTheme
import jp.temari.fitness.ui.theme.TemariYellow
import jp.temari.fitness.ui.theme.TextSizePreviewProvider
import kotlin.math.roundToInt

/** 道の上の背景のグラデーション（若竹色 → 藍色）。白い文字が読める濃さ。 */
private val JourneyGradient = listOf(Color(0xFF1F6B4A), Color(0xFF2F3E9E))

/**
 * たび画面（ViewModel とつなぐ側）。
 */
@Composable
fun JourneyRoute(
    onBack: () -> Unit,
    viewModel: JourneyViewModel = viewModel(factory = JourneyViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    JourneyScreen(state = state, onBack = onBack)
}

/**
 * たび画面（表示だけ）。これまで歩いた距離で、東海道（日本橋 → 京都）を旅する。
 *
 * @param state 表示する内容。null（読み込み中）のときは「もどる」だけを出す。
 */
@Composable
fun JourneyScreen(state: JourneyUiState?, onBack: () -> Unit) {
    ScreenFrame(
        title = stringResource(R.string.journey_title),
        emoji = stringResource(R.string.emoji_journey),
        bottomBar = {
            BigButton(
                text = stringResource(R.string.button_back),
                onClick = onBack,
                emoji = stringResource(R.string.emoji_back),
            )
        },
    ) {
        if (state != null) {
            val stations = stringArrayResource(R.array.journey_stations).toList()
            DistanceCard(state, stations)
            NextStationCard(state.progress, stations)
            StationList(state.progress, stations)
        }
    }
}

/** これまでの距離と、道の上の手まりちゃん。 */
@Composable
private fun DistanceCard(state: JourneyUiState, stations: List<String>) {
    val progress = state.progress
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        shadowElevation = UiConfig.CardElevation,
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(JourneyGradient))
                .padding(UiConfig.CardPadding),
            verticalArrangement = Arrangement.spacedBy(UiConfig.SmallSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.journey_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            if (progress.lap > 1) {
                Text(
                    text = stringResource(R.string.journey_lap, progress.lap),
                    style = MaterialTheme.typography.titleLarge,
                    color = TemariYellow,
                )
            }
            Text(
                text = stringResource(R.string.journey_distance, formatKm(progress.distanceKm)),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
            )
            Text(
                text = stringResource(R.string.journey_total_steps, state.totalSteps),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
            )
            RoadView(progress.routeFraction)
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stations.first(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stations.last(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = stringResource(R.string.journey_route_percent, (progress.routeFraction * 100).roundToInt()),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
        }
    }
}

/** 日本橋から京都までの道。進んだところまで黄色くなり、手まりちゃんが今の位置にいる。 */
@Composable
private fun RoadView(fraction: Float) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiConfig.MascotSmall)
            .clearAndSetSemantics { },
    ) {
        val mascotSize = UiConfig.MascotSmall * 0.7f
        Canvas(modifier = Modifier.matchParentSize()) {
            val y = size.height * 0.75f
            val stroke = UiConfig.SmallSpacing.toPx()
            // まだの道は点線、進んだ道は黄色の線。
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 1.5f, stroke)),
            )
            drawLine(
                color = TemariYellow,
                start = Offset(0f, y),
                end = Offset(size.width * fraction, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        // 手まりちゃんの真ん中が今の位置に来るようにする（端ではみ出さないよう範囲内に収める）。
        val x = (maxWidth * fraction - mascotSize / 2).coerceIn(0.dp, maxWidth - mascotSize)
        Box(modifier = Modifier.offset(x = x)) {
            Mascot(mood = MascotMood.Happy, size = mascotSize)
        }
    }
}

/** つぎの宿場まであと何 km か。 */
@Composable
private fun NextStationCard(progress: JourneyProgress, stations: List<String>) {
    val stepsToNext = (progress.kmToNext * 1000 / JourneyConfig.STRIDE_METERS).roundToInt()
    TemariCard(color = MaterialTheme.colorScheme.tertiaryContainer) {
        Text(
            text = stringResource(R.string.journey_next, stations[progress.nextIndex]),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.journey_next_km, formatKm(progress.kmToNext), stepsToNext),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** 宿場の一覧。通った宿場は「✓」、今いるところに「いま ここ」、まだの宿場は「○」。 */
@Composable
private fun StationList(progress: JourneyProgress, stations: List<String>) {
    TemariCard(
        title = stringResource(R.string.journey_list_title),
        emoji = stringResource(R.string.emoji_station),
    ) {
        stations.forEachIndexed { index, name ->
            val passed = index <= progress.passedIndex
            val km = JourneyConfig.STATION_KM.getOrElse(index) { 0.0 }
            StationRow(
                mark = stringResource(if (passed) R.string.journey_passed_mark else R.string.journey_upcoming_mark),
                name = name,
                km = stringResource(R.string.journey_station_km, formatKm(km)),
                passed = passed,
            )
            if (index == progress.passedIndex && index != stations.lastIndex) {
                Text(
                    text = stringResource(R.string.journey_here),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(start = UiConfig.ScreenPadding),
                )
            }
        }
    }
}

@Composable
private fun StationRow(mark: String, name: String, km: String, passed: Boolean) {
    val color = if (passed) TemariGreen else MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "$mark $name $km" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UiConfig.SmallSpacing),
    ) {
        Text(text = mark, style = MaterialTheme.typography.titleLarge, color = color)
        Text(text = name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(text = km, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

/** km を小数1けたの文字にする（例: 31.2）。 */
private fun formatKm(km: Double): String = "%.1f".format(km)

@Preview(showBackground = true, widthDp = 360, heightDp = 1400)
@Composable
private fun JourneyScreenPreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) {
        JourneyScreen(state = JourneyUiState(56_000, JourneyProgress.of(56_000)), onBack = {})
    }
}
