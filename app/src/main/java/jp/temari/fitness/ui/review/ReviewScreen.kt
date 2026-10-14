package jp.temari.fitness.ui.review

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.temari.fitness.R
import jp.temari.fitness.config.TextSizeOption
import jp.temari.fitness.ui.common.PreviewData
import jp.temari.fitness.ui.common.labelRes
import jp.temari.fitness.ui.components.BigButton
import jp.temari.fitness.ui.components.ChartBar
import jp.temari.fitness.ui.components.ChartLegend
import jp.temari.fitness.ui.components.ChoiceButtons
import jp.temari.fitness.ui.components.ScreenFrame
import jp.temari.fitness.ui.components.StackedBarChart
import jp.temari.fitness.ui.theme.TemariTheme
import jp.temari.fitness.ui.theme.TextSizePreviewProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * ふりかえり画面（ViewModel とつなぐ側）。
 */
@Composable
fun ReviewRoute(
    onBack: () -> Unit,
    viewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReviewScreen(state = state, onSelectMode = viewModel::onSelectMode, onBack = onBack)
}

/**
 * ふりかえり画面（表示だけ）。きょう／今週 の活動を積み上げ棒グラフで見る。
 *
 * @param state 表示する内容。null（読み込み中）のときは切り替えボタンだけを出す。
 */
@Composable
fun ReviewScreen(
    state: ReviewUiState?,
    onSelectMode: (ReviewMode) -> Unit,
    onBack: () -> Unit,
) {
    ScreenFrame(
        title = stringResource(R.string.review_title),
        bottomBar = {
            BigButton(text = stringResource(R.string.button_back), onClick = onBack)
        },
    ) {
        ModeSwitch(selected = state?.mode ?: ReviewMode.Today, onSelectMode = onSelectMode)
        if (state != null) {
            when (state.mode) {
                ReviewMode.Today -> StackedBarChart(bars = todayBars(state))
                ReviewMode.Week -> StackedBarChart(bars = weekBars(state))
            }
            ChartLegend()
            SummaryText(state)
        }
    }
}

/** 上部の「きょう」「今週」の切り替え。 */
@Composable
private fun ModeSwitch(selected: ReviewMode, onSelectMode: (ReviewMode) -> Unit) {
    ChoiceButtons(
        options = ReviewMode.entries,
        selected = selected,
        label = { mode ->
            stringResource(
                when (mode) {
                    ReviewMode.Today -> R.string.review_tab_today
                    ReviewMode.Week -> R.string.review_tab_week
                },
            )
        },
        onSelect = onSelectMode,
    )
}

/** きょうの4つの時間帯の棒。 */
@Composable
private fun todayBars(state: ReviewUiState): List<ChartBar> = state.today.blocks.map { block ->
    val label = stringResource(block.block.labelRes)
    ChartBar(
        label = label,
        walkPoints = block.walkPoints,
        lifePoints = block.lifePoints,
        description = stringResource(
            R.string.review_chart_description, label, block.walkPoints, block.lifePoints,
        ),
    )
}

/** 今週の月〜日の7本の棒。記録が無い日・まだ来ていない日は 0。 */
@Composable
private fun weekBars(state: ReviewUiState): List<ChartBar> = state.weekDays.map { day ->
    val label = stringResource(day.date.dayOfWeek.labelRes)
    val walk = day.activity?.walkPoints ?: 0
    val life = day.activity?.lifePoints ?: 0
    val achieved = day.activity?.isGoalAchieved == true
    val description = stringResource(
        if (achieved) R.string.review_chart_description_achieved else R.string.review_chart_description,
        label, walk, life,
    )
    ChartBar(
        label = label,
        walkPoints = walk,
        lifePoints = life,
        achieved = achieved,
        description = description,
    )
}

/** グラフの下の1行: 「いちばん動いたのは ◯◯ でした」。 */
@Composable
private fun SummaryText(state: ReviewUiState) {
    val text = when (state.mode) {
        ReviewMode.Today -> state.bestBlock?.let {
            stringResource(R.string.review_best_block, stringResource(it.labelRes))
        }
        ReviewMode.Week -> state.bestDay?.let {
            stringResource(R.string.review_best_day, stringResource(it.labelRes))
        }
    } ?: stringResource(R.string.review_no_data)
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** プレビュー用の状態。 */
private fun previewState(mode: ReviewMode): ReviewUiState = runBlocking {
    val repository = PreviewData.repository()
    val today = repository.todayActivity().first()
    val week = repository.weekActivities().first()
    ReviewUiState(mode, today, ReviewUiState.weekDaysOf(today.date, week))
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "きょう")
@Composable
private fun ReviewTodayPreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) {
        ReviewScreen(state = previewState(ReviewMode.Today), onSelectMode = {}, onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "今週")
@Composable
private fun ReviewWeekPreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) {
        ReviewScreen(state = previewState(ReviewMode.Week), onSelectMode = {}, onBack = {})
    }
}
