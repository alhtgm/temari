package jp.temari.fitness.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.temari.fitness.R
import jp.temari.fitness.config.ActivityConfig
import jp.temari.fitness.config.TextSizeOption
import jp.temari.fitness.config.UiConfig
import jp.temari.fitness.data.DailyActivity
import jp.temari.fitness.data.RecordingStatus
import jp.temari.fitness.data.fake.MockScenario
import jp.temari.fitness.ui.common.PreviewData
import jp.temari.fitness.ui.common.labelRes
import jp.temari.fitness.ui.components.BigButton
import jp.temari.fitness.ui.components.Confetti
import jp.temari.fitness.ui.components.Mascot
import jp.temari.fitness.ui.components.MascotMood
import jp.temari.fitness.ui.components.MenuTile
import jp.temari.fitness.ui.components.ProgressRing
import jp.temari.fitness.ui.components.PulsingDot
import jp.temari.fitness.ui.components.ScreenFrame
import jp.temari.fitness.ui.components.TemariCard
import jp.temari.fitness.ui.components.rememberCountUp
import jp.temari.fitness.ui.theme.AttentionBackground
import jp.temari.fitness.ui.theme.AttentionOrange
import jp.temari.fitness.ui.theme.HeroGradientEnd
import jp.temari.fitness.ui.theme.HeroGradientStart
import jp.temari.fitness.ui.theme.TemariBlue
import jp.temari.fitness.ui.theme.TemariBlueLight
import jp.temari.fitness.ui.theme.TemariGreen
import jp.temari.fitness.ui.theme.TemariGreenLight
import jp.temari.fitness.ui.theme.TemariTheme
import jp.temari.fitness.ui.theme.TemariVermilion
import jp.temari.fitness.ui.theme.TemariYellow
import jp.temari.fitness.ui.theme.TextSizePreviewProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

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
 * @param state 表示する内容。null（読み込み中）のときはメニューだけを出す。
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
                emoji = stringResource(R.string.emoji_settings),
            )
        },
    ) {
        if (state != null) {
            GreetingHeader(state.nickname, state.greeting, state.today)
            HeroCard(state.today)
            MessageCard(state.message, state.today)
            Breakdown(state.today)
            RecordingStatusView(state.status, onResumeRecording)
        }
        MenuGrid(onOpenReview = onOpenReview, onOpenBingo = onOpenBingo)
    }
}

/** 日付と「◯◯さん、こんにちは」。呼び名が空なら「こんにちは」だけ。 */
@Composable
private fun GreetingHeader(nickname: String, greeting: Greeting, today: DailyActivity) {
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
    Column {
        Text(
            text = stringResource(
                R.string.home_date,
                today.date.monthValue,
                today.date.dayOfMonth,
                stringResource(today.date.dayOfWeek.labelRes),
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/**
 * 画面の中心: きょうの活動ポイント（いちばん大きな数字）と、目標までの進み具合の輪。
 *
 * 輪の中には手まりちゃんがいて、進み具合で表情が変わる。目標をたっせいしたら紙ふぶきが降る。
 */
@Composable
private fun HeroCard(today: DailyActivity) {
    val points by rememberCountUp(today.totalPoints)
    val percent = (today.progress * 100).toInt()
    val pointsDescription = stringResource(R.string.home_points_a11y, today.totalPoints)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        shadowElevation = UiConfig.CardElevation,
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier.background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd))),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(UiConfig.CardPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(UiConfig.SmallSpacing),
            ) {
                Text(
                    text = stringResource(R.string.home_points_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                )
                // 読み上げでは、増えていく途中の数字ではなく最後の数字を読む。
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = pointsDescription
                    },
                ) {
                    Text(
                        text = stringResource(R.string.home_points_value, points),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                    )
                    Text(
                        text = stringResource(R.string.home_points_unit),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        // 数字の下のラインに揃えて見えるよう、少しだけ下に余白をとる。
                        modifier = Modifier.padding(start = UiConfig.SmallSpacing / 2, bottom = UiConfig.UnitBaselineOffset),
                    )
                }
                ProgressRing(
                    progress = today.progress,
                    trackColor = Color.White.copy(alpha = 0.22f),
                    colors = listOf(TemariYellow, TemariVermilion, TemariYellow),
                ) {
                    Mascot(mood = moodOf(today), size = UiConfig.MascotInRing)
                }
                // 進み具合は色や輪だけでなく、必ず文字でも伝える。
                Text(
                    text = if (today.isGoalAchieved) {
                        stringResource(R.string.home_goal_achieved)
                    } else {
                        stringResource(R.string.home_goal_remaining, today.goal - today.totalPoints)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.home_goal_value_percent, today.goal, percent),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
            if (today.isGoalAchieved) {
                Confetti(key = today.date, modifier = Modifier.matchParentSize())
            }
        }
    }
}

/** 進み具合に合わせた手まりちゃんの表情。 */
private fun moodOf(today: DailyActivity): MascotMood = when {
    today.isGoalAchieved -> MascotMood.Excited
    today.progress >= ActivityConfig.NEAR_GOAL_RATIO / 2 -> MascotMood.Happy
    else -> MascotMood.Calm
}

/** ひとことメッセージ。手まりちゃんの吹き出しのように見せる。 */
@Composable
private fun MessageCard(message: HomeMessage, today: DailyActivity) {
    val text = when (message) {
        HomeMessage.ResumeRecording -> stringResource(R.string.message_resume)
        HomeMessage.GoalAchieved -> stringResource(R.string.message_goal_achieved)
        HomeMessage.NearGoal -> stringResource(R.string.message_near_goal)
        is HomeMessage.BestBlock ->
            stringResource(R.string.message_best_block, stringResource(message.block.labelRes))
        HomeMessage.KeepGoing -> stringResource(R.string.message_keep_going)
    }
    TemariCard(color = MaterialTheme.colorScheme.tertiaryContainer) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UiConfig.SmallSpacing),
        ) {
            Mascot(mood = moodOf(today), size = UiConfig.MascotSmall, bounce = false)
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 内訳: 歩いた／家のこと を2つのタイルで並べる。 */
@Composable
private fun Breakdown(today: DailyActivity) {
    // IntrinsicSize.Min: 2つのタイルの高さを、背の高い方にそろえる。
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(UiConfig.ButtonSpacing),
    ) {
        StatTile(
            emoji = stringResource(R.string.emoji_walk),
            label = stringResource(R.string.home_walk_label),
            value = stringResource(R.string.home_points_short, today.walkPoints),
            sub = stringResource(R.string.home_steps_about, today.steps),
            color = TemariBlueLight,
            accent = TemariBlue,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        StatTile(
            emoji = stringResource(R.string.emoji_life),
            label = stringResource(R.string.home_life_label),
            value = stringResource(R.string.home_points_short, today.lifePoints),
            sub = null,
            color = TemariGreenLight,
            accent = TemariGreen,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

/** 数字を1つ見せる小さなタイル。 */
@Composable
private fun StatTile(
    emoji: String,
    label: String,
    value: String,
    sub: String?,
    color: Color,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.large, color = color) {
        Column(
            modifier = Modifier.padding(UiConfig.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(UiConfig.SmallSpacing / 2),
        ) {
            Text(text = "$emoji $label", style = MaterialTheme.typography.bodyLarge)
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = accent)
            if (sub != null) {
                Text(text = sub, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** 記録の状態。記録中は小さく点滅する点、止まっているときは目立つ枠と「再開する」ボタン。 */
@Composable
private fun RecordingStatusView(status: RecordingStatus, onResumeRecording: () -> Unit) {
    when (status) {
        RecordingStatus.Recording -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UiConfig.SmallSpacing),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PulsingDot(color = TemariGreen, size = UiConfig.SmallSpacing * 2)
            Text(
                text = stringResource(R.string.home_recording_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        is RecordingStatus.Stopped -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AttentionBackground, MaterialTheme.shapes.large)
                .border(UiConfig.AttentionBorderWidth, AttentionOrange, MaterialTheme.shapes.large)
                .padding(UiConfig.CardPadding),
            verticalArrangement = Arrangement.spacedBy(UiConfig.ButtonSpacing),
        ) {
            Text(
                text = stringResource(R.string.home_stopped),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            BigButton(
                text = stringResource(R.string.home_button_resume),
                onClick = onResumeRecording,
                emoji = stringResource(R.string.emoji_resume),
            )
        }
    }
}

/** ほかの画面へ進む大きなタイル。 */
@Composable
private fun MenuGrid(onOpenReview: () -> Unit, onOpenBingo: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(UiConfig.ButtonSpacing),
    ) {
        MenuTile(
            emoji = stringResource(R.string.emoji_review),
            text = stringResource(R.string.home_button_review),
            color = TemariBlue,
            contentColor = Color.White,
            onClick = onOpenReview,
            modifier = Modifier.weight(1f),
        )
        MenuTile(
            emoji = stringResource(R.string.emoji_bingo),
            text = stringResource(R.string.home_button_bingo),
            color = TemariVermilion,
            contentColor = Color.White,
            onClick = onOpenBingo,
            modifier = Modifier.weight(1f),
        )
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

@Preview(showBackground = true, widthDp = 360, heightDp = 1200)
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

@Preview(showBackground = true, widthDp = 360, heightDp = 1200, name = "記録が止まった")
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
