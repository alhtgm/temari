package jp.temari.fitness.ui.bingo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.temari.fitness.R
import jp.temari.fitness.config.TextSizeOption
import jp.temari.fitness.config.UiConfig
import jp.temari.fitness.data.BingoCard
import jp.temari.fitness.data.BingoCell
import jp.temari.fitness.data.BingoCellId
import jp.temari.fitness.data.fake.MockScenario
import jp.temari.fitness.ui.common.PreviewData
import jp.temari.fitness.ui.components.BigButton
import jp.temari.fitness.ui.components.ScreenFrame
import jp.temari.fitness.ui.theme.AttentionOrange
import jp.temari.fitness.ui.theme.TemariTheme
import jp.temari.fitness.ui.theme.TextSizePreviewProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** マスの形（角を丸くする）。 */
private val CellShape = RoundedCornerShape(UiConfig.CornerRadius)

/**
 * ビンゴ画面（ViewModel とつなぐ側）。
 */
@Composable
fun BingoRoute(
    onBack: () -> Unit,
    viewModel: BingoViewModel = viewModel(factory = BingoViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BingoScreen(
        state = state,
        onCellClick = viewModel::onCellClick,
        onDialogDismiss = viewModel::onDialogDismiss,
        onBack = onBack,
    )
}

/**
 * ビンゴ画面（表示だけ）。1週間で1枚の 3×3 のビンゴ。
 *
 * @param state 表示する内容。null（読み込み中）のときは「もどる」だけを出す。
 */
@Composable
fun BingoScreen(
    state: BingoUiState?,
    onCellClick: (BingoCellId) -> Unit,
    onDialogDismiss: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenFrame(
        title = state?.card?.let { weekTitle(it) } ?: stringResource(R.string.bingo_title),
        bottomBar = {
            BigButton(text = stringResource(R.string.button_back), onClick = onBack)
        },
    ) {
        if (state != null) {
            if (state.card.hasBingo) {
                // 列の数が変わったら（シナリオ切り替えなど）演出をやり直す。
                BingoCelebration(key = state.card.completedLines.size)
            }
            BingoGrid(card = state.card, onCellClick = onCellClick)
        }
    }
    val selected = state?.selectedCell
    if (state != null && selected != null) {
        CellDialog(
            id = selected,
            achieved = state.card.cells.firstOrNull { it.id == selected }?.achieved == true,
            onDismiss = onDialogDismiss,
        )
    }
}

/** 「今週のビンゴ（◯月◯日〜◯月◯日）」。 */
@Composable
private fun weekTitle(card: BingoCard): String {
    val end = card.weekStart.plusDays(6)
    return stringResource(
        R.string.bingo_week_range,
        card.weekStart.monthValue, card.weekStart.dayOfMonth, end.monthValue, end.dayOfMonth,
    )
}

/**
 * 3×3 のマスと、そろった列の線。
 *
 * 線を引くために、各マスの中心の位置を覚えておく（文字の大きさでマスの高さが変わるため、計算ではなく実際の位置を使う）。
 */
@Composable
private fun BingoGrid(card: BingoCard, onCellClick: (BingoCellId) -> Unit) {
    val cellCenters = remember { mutableStateMapOf<Int, Offset>() }
    var gridOrigin by remember { mutableStateOf(Offset.Zero) }
    // 線が端から端へ伸びていくアニメーションの進み具合（0〜1）。
    val lineProgress = remember { Animatable(0f) }
    val lines = card.completedLines
    LaunchedEffect(lines) {
        lineProgress.snapTo(0f)
        lineProgress.animateTo(1f, tween(UiConfig.BINGO_LINE_ANIMATION_MILLIS))
    }

    Box(modifier = Modifier.onGloballyPositioned { gridOrigin = it.positionInRoot() }) {
        Column(verticalArrangement = Arrangement.spacedBy(UiConfig.SmallSpacing)) {
            for (row in 0 until BingoCard.SIZE) {
                // IntrinsicSize.Min: 同じ行のマスの高さを、いちばん背の高いマスにそろえる。
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(UiConfig.SmallSpacing),
                ) {
                    for (col in 0 until BingoCard.SIZE) {
                        val index = row * BingoCard.SIZE + col
                        val cell = card.cells.getOrNull(index) ?: continue
                        BingoCellView(
                            cell = cell,
                            onClick = { onCellClick(cell.id) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .onGloballyPositioned { cellCenters[index] = it.boundsInRoot().center },
                        )
                    }
                }
            }
        }
        // そろった列の線。マスの上に重ねて描く（Canvas は押した操作を横取りしないので、下のマスは押せる）。
        Canvas(modifier = Modifier.matchParentSize()) {
            lines.forEach { line ->
                val start = cellCenters[line.first()] ?: return@forEach
                val end = cellCenters[line.last()] ?: return@forEach
                val from = start - gridOrigin
                val to = end - gridOrigin
                drawLine(
                    color = AttentionOrange.copy(alpha = UiConfig.BINGO_LINE_ALPHA),
                    start = from,
                    end = from + (to - from) * lineProgress.value,
                    strokeWidth = UiConfig.BingoLineWidth.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

/** 1つのマス。開いたマスは緑の塗りつぶしと「✓」、まだのマスは白地に枠。 */
@Composable
private fun BingoCellView(cell: BingoCell, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val title = bingoCellTitle(cell.id)
    val description = stringResource(
        if (cell.achieved) R.string.bingo_cell_open_description else R.string.bingo_cell_closed_description,
        title,
    )
    val background = if (cell.achieved) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.background
    val textColor = if (cell.achieved) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onBackground
    Column(
        modifier = modifier
            .heightIn(min = UiConfig.BingoCellMinHeight)
            .background(background, CellShape)
            .border(UiConfig.BorderWidth, MaterialTheme.colorScheme.outline, CellShape)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            }
            .padding(UiConfig.SmallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (cell.achieved) {
            Text(
                text = stringResource(R.string.bingo_check_mark),
                style = MaterialTheme.typography.titleLarge,
                color = textColor,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}

/** 1列そろったときのお祝い: 大きな「ビンゴ！」が弾むように大きくなる。 */
@Composable
private fun BingoCelebration(key: Int) {
    val scale = remember { Animatable(UiConfig.BINGO_TEXT_START_SCALE) }
    LaunchedEffect(key) {
        scale.snapTo(UiConfig.BINGO_TEXT_START_SCALE)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }
    Text(
        text = stringResource(R.string.bingo_celebration),
        style = MaterialTheme.typography.displayLarge,
        color = AttentionOrange,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
    )
}

/** マスの説明を大きな文字で出すダイアログ。 */
@Composable
private fun CellDialog(id: BingoCellId, achieved: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = bingoCellTitle(id), style = MaterialTheme.typography.headlineMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(UiConfig.SmallSpacing)) {
                if (achieved) {
                    Text(
                        text = stringResource(R.string.bingo_dialog_open),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Text(text = bingoCellDescription(id), style = MaterialTheme.typography.bodyLarge)
            }
        },
        confirmButton = {
            BigButton(text = stringResource(R.string.bingo_button_close), onClick = onDismiss)
        },
    )
}

/** プレビュー用の状態。 */
private fun previewState(scenario: MockScenario, selected: BingoCellId? = null): BingoUiState = runBlocking {
    BingoUiState(PreviewData.repository(scenario).bingoCard().first(), selected)
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "ビンゴ")
@Composable
private fun BingoScreenPreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) {
        BingoScreen(
            state = previewState(MockScenario.Active),
            onCellClick = {},
            onDialogDismiss = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "説明のダイアログ")
@Composable
private fun BingoDialogPreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) {
        BingoScreen(
            state = previewState(MockScenario.Normal, BingoCellId.MorningActive),
            onCellClick = {},
            onDialogDismiss = {},
            onBack = {},
        )
    }
}
