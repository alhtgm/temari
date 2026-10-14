package jp.temari.fitness.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.temari.fitness.R
import jp.temari.fitness.config.TextSizeOption
import jp.temari.fitness.config.UiConfig
import jp.temari.fitness.data.DailyActivity
import jp.temari.fitness.data.RecordingStatus
import jp.temari.fitness.data.fake.MockScenario
import jp.temari.fitness.ui.common.PreviewData
import jp.temari.fitness.ui.common.labelRes
import jp.temari.fitness.ui.components.BigButton
import jp.temari.fitness.ui.components.ScreenFrame
import jp.temari.fitness.ui.theme.AttentionBackground
import jp.temari.fitness.ui.theme.AttentionOrange
import jp.temari.fitness.ui.theme.TemariTheme
import jp.temari.fitness.ui.theme.TextSizePreviewProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

/** カードの形（角を丸くする）。 */
private val CardShape = RoundedCornerShape(UiConfig.CornerRadius)

/**
 * ホーム画面（ViewModel とつなぐ側）。
 *
 * ViewModel から状態を受け取り、表示だけを担当する [HomeScreen] に渡す。
 */
@Composable
fun HomeRoute(
    onOpenReview: () -> Unit,
    onOpenBingo: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onResumeRecording = viewModel::onResumeRecording,
        onOpenReview = onOpenReview,
        onOpenBingo = onOpenBingo,
        onOpenSettings = onOpenSettings,
    )
}

/**
 * ホーム画面（表示だけ）。アプリの中心で、ここから各画面へ1段だけ進む。
 *
 * @param state 表示する内容。null（読み込み中）のときはボタンだけを出す。
 */
@Composable
fun HomeScreen(
    state: HomeUiState?,
    onResumeRecording: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenBingo: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    ScreenFrame(
        title = null,
        bottomBar = {
            // せってい は利用者がよく使うものではないので、控えめな見た目で一番下に置く。
            BigButton(
                text = stringResource(R.string.home_button_settings),
                onClick = onOpenSettings,
                primary = false,
            )
        },
    ) {
        if (state != null) {
            GreetingText(state.nickname, state.greeting)
            PointsCard(state.today)
            Breakdown(state.today)
            MessageText(state.message)
            RecordingStatusView(state.status, onResumeRecording)
        }
        BigButton(text = stringResource(R.string.home_button_review), onClick = onOpenReview)
        BigButton(text = stringResource(R.string.home_button_bingo), onClick = onOpenBingo)
    }
}

/** 「◯◯さん、こんにちは」。呼び名が空なら「こんにちは」だけ。 */
@Composable
private fun GreetingText(nickname: String, greeting: Greeting) {
    val greetingText = stringResource(
        when (greeting) {
            Greeting.Morning -> R.string.greeting_morning
            Greeting.Day -> R.string.greeting_day
            Greeting.Evening -> R.string.greeting_evening
        },
    )
    val text = if (nickname.isBlank()) {
        greetingText
    } else {
        stringResource(R.string.home_greeting_with_name, nickname, greetingText)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.semantics { heading() },
    )
}

/** 画面の中心: きょうの活動ポイント（いちばん大きな数字）と、目標までの進み具合。 */
@Composable
private fun PointsCard(today: DailyActivity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, CardShape)
            .padding(UiConfig.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiConfig.SmallSpacing),
    ) {
        Text(
            text = stringResource(R.string.home_points_label),
            style = MaterialTheme.typography.bodyLarge,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = stringResource(R.string.home_points_value, today.totalPoints),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.home_points_unit),
                style = MaterialTheme.typography.bodyLarge,
                // 数字の下のラインに揃えて見えるよう、少しだけ下に余白をとる。
                modifier = Modifier.padding(start = UiConfig.SmallSpacing / 2, bottom = UiConfig.UnitBaselineOffset),
            )
        }
        ProgressBar(progress = today.progress)
        // 進み具合は色だけでなく、必ず文字でも伝える。
        Text(
            text = if (today.isGoalAchieved) {
                stringResource(R.string.home_goal_achieved)
            } else {
                stringResource(R.string.home_goal_remaining, today.goal - today.totalPoints)
            },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.home_goal_value, today.goal),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** 目標までの進み具合を表す横長のバー。目標を超えても、いっぱいで止める。 */
@Composable
private fun ProgressBar(progress: Float) {
    val shape = RoundedCornerShape(UiConfig.ProgressBarHeight / 2)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiConfig.ProgressBarHeight)
            .background(MaterialTheme.colorScheme.background, shape)
            .border(UiConfig.BorderWidth, MaterialTheme.colorScheme.outline, shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(MaterialTheme.colorScheme.primary, shape),
        )
    }
}

/** 内訳2行: 歩いた／家のこと。 */
@Composable
private fun Breakdown(today: DailyActivity) {
    Column(verticalArrangement = Arrangement.spacedBy(UiConfig.SmallSpacing / 2)) {
        Text(
            text = stringResource(R.string.home_walk_points, today.walkPoints, today.steps),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.home_life_points, today.lifePoints),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** ひとことメッセージ。 */
@Composable
private fun MessageText(message: HomeMessage) {
    val text = when (message) {
        HomeMessage.ResumeRecording -> stringResource(R.string.message_resume)
        HomeMessage.GoalAchieved -> stringResource(R.string.message_goal_achieved)
        HomeMessage.NearGoal -> stringResource(R.string.message_near_goal)
        is HomeMessage.BestBlock ->
            stringResource(R.string.message_best_block, stringResource(message.block.labelRes))
        HomeMessage.KeepGoing -> stringResource(R.string.message_keep_going)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.secondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** 記録の状態。記録中は小さく、止まっているときは目立つ枠と「再開する」ボタン。 */
@Composable
private fun RecordingStatusView(status: RecordingStatus, onResumeRecording: () -> Unit) {
    when (status) {
        RecordingStatus.Recording -> Text(
            text = stringResource(R.string.home_recording),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        is RecordingStatus.Stopped -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AttentionBackground, CardShape)
                .border(UiConfig.AttentionBorderWidth, AttentionOrange, CardShape)
                .padding(UiConfig.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(UiConfig.ButtonSpacing),
        ) {
            Text(
                text = stringResource(R.string.home_stopped),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            BigButton(text = stringResource(R.string.home_button_resume), onClick = onResumeRecording)
        }
    }
}

/** プレビュー用に、モックのシナリオからホームの状態を作る。 */
private fun previewState(scenario: MockScenario): HomeUiState = runBlocking {
    val repository = PreviewData.repository(scenario)
    val today = repository.todayActivity().first()
    val status = repository.recordingStatus().first()
    val now = LocalDateTime.now(PreviewData.clock).toLocalTime()
    HomeUiState(
        nickname = "はなこ",
        greeting = Greeting.of(now),
        today = today,
        status = status,
        message = HomeMessage.select(today, status, now),
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun HomeScreenPreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) {
        HomeScreen(
            state = previewState(MockScenario.Normal),
            onResumeRecording = {},
            onOpenReview = {},
            onOpenBingo = {},
            onOpenSettings = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "記録が止まった")
@Composable
private fun HomeScreenStoppedPreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) {
        HomeScreen(
            state = previewState(MockScenario.Stopped),
            onResumeRecording = {},
            onOpenReview = {},
            onOpenBingo = {},
            onOpenSettings = {},
        )
    }
}
