package jp.temari.fitness.ui.exercise

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.temari.fitness.R
import jp.temari.fitness.config.ExerciseConfig
import jp.temari.fitness.config.TextSizeOption
import jp.temari.fitness.config.UiConfig
import jp.temari.fitness.ui.components.BigButton
import jp.temari.fitness.ui.components.Confetti
import jp.temari.fitness.ui.components.Mascot
import jp.temari.fitness.ui.components.MascotMood
import jp.temari.fitness.ui.components.ProgressRing
import jp.temari.fitness.ui.components.ScreenFrame
import jp.temari.fitness.ui.components.TemariCard
import jp.temari.fitness.ui.theme.TemariGreen
import jp.temari.fitness.ui.theme.TemariGreenLight
import jp.temari.fitness.ui.theme.TemariTheme
import jp.temari.fitness.ui.theme.TemariYellow
import jp.temari.fitness.ui.theme.TextSizePreviewProvider

/**
 * たいそう画面（ViewModel とつなぐ側）。
 */
@Composable
fun ExerciseRoute(
    onBack: () -> Unit,
    viewModel: ExerciseViewModel = viewModel(factory = ExerciseViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // 体操の途中で Android の戻る操作をしたら、ホームではなく一覧に戻す。
    BackHandler(enabled = state?.phase !is ExercisePhase.Choose && state != null) {
        viewModel.onBackToList()
    }
    ExerciseScreen(
        state = state,
        onStart = viewModel::onStart,
        onBackToList = viewModel::onBackToList,
        onBack = onBack,
    )
}

/**
 * たいそう画面（表示だけ）。座ってできる体操を選び、大きなタイマーに合わせて体を動かす。
 *
 * @param state 表示する内容。null（読み込み中）のときは「もどる」だけを出す。
 */
@Composable
fun ExerciseScreen(
    state: ExerciseUiState?,
    onStart: (Exercise) -> Unit,
    onBackToList: () -> Unit,
    onBack: () -> Unit,
) {
    val phase = state?.phase ?: ExercisePhase.Choose
    val keepScreenOn = phase is ExercisePhase.Ready || phase is ExercisePhase.Doing
    // 体操の途中で画面が消えないようにする。
    val view = LocalView.current
    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        ScreenFrame(
            title = stringResource(R.string.exercise_title),
            emoji = stringResource(R.string.emoji_exercise),
            bottomBar = { BottomButtons(phase, onBackToList, onBack) },
        ) {
            if (state != null) {
                when (phase) {
                    ExercisePhase.Choose -> ChooseList(state.exercisesToday, onStart)
                    is ExercisePhase.Ready -> TimerView(phase.exercise, phase.secondsLeft, ready = true)
                    is ExercisePhase.Doing -> TimerView(phase.exercise, phase.secondsLeft, ready = false)
                    is ExercisePhase.Done -> DoneView(phase.exercise, state.exercisesToday)
                }
            }
        }
        if (phase is ExercisePhase.Done) {
            Confetti(key = phase, modifier = Modifier.matchParentSize())
        }
    }
}

/** 画面の下のボタン。段階によって変える。 */
@Composable
private fun BottomButtons(phase: ExercisePhase, onBackToList: () -> Unit, onBack: () -> Unit) {
    when (phase) {
        ExercisePhase.Choose -> BigButton(
            text = stringResource(R.string.button_back),
            onClick = onBack,
            emoji = stringResource(R.string.emoji_back),
        )
        is ExercisePhase.Ready, is ExercisePhase.Doing -> BigButton(
            text = stringResource(R.string.exercise_button_stop),
            onClick = onBackToList,
            primary = false,
        )
        is ExercisePhase.Done -> {
            BigButton(text = stringResource(R.string.exercise_button_more), onClick = onBackToList)
            BigButton(
                text = stringResource(R.string.button_back),
                onClick = onBack,
                primary = false,
                emoji = stringResource(R.string.emoji_back),
            )
        }
    }
}

/** 体操の一覧。押すと始まる。 */
@Composable
private fun ChooseList(exercisesToday: Int, onStart: (Exercise) -> Unit) {
    TemariCard(color = TemariGreenLight) {
        Text(
            text = stringResource(R.string.exercise_today_count, exercisesToday),
            style = MaterialTheme.typography.titleLarge,
            color = TemariGreen,
        )
        Text(text = stringResource(R.string.exercise_safety), style = MaterialTheme.typography.bodyLarge)
    }
    Exercise.entries.forEach { exercise ->
        TemariCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(UiConfig.SmallSpacing),
            ) {
                Text(text = stringResource(exercise.emoji), fontSize = UiConfig.TILE_EMOJI_SP.sp)
                Text(
                    text = stringResource(exercise.title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(text = stringResource(exercise.howTo), style = MaterialTheme.typography.bodyLarge)
            BigButton(
                text = stringResource(R.string.exercise_button_start, ExerciseConfig.DURATION_SECONDS),
                onClick = { onStart(exercise) },
                emoji = stringResource(R.string.emoji_resume),
            )
        }
    }
}

/** 数え下げのタイマー。輪がだんだん減っていき、真ん中に残りの秒数を大きく出す。 */
@Composable
private fun TimerView(exercise: Exercise, secondsLeft: Int, ready: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiConfig.ButtonSpacing),
    ) {
        Text(
            text = "${stringResource(exercise.emoji)} ${stringResource(exercise.title)}",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        val total = if (ready) ExerciseConfig.READY_SECONDS else ExerciseConfig.DURATION_SECONDS
        // 説明の文字は輪の外に出す。輪の中に入れると、特大の文字で輪からはみ出すため。
        Text(
            text = if (ready) stringResource(R.string.exercise_ready) else stringResource(R.string.exercise_go),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.tertiary,
        )
        ProgressRing(
            progress = secondsLeft.toFloat() / total,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            colors = listOf(TemariGreen, TemariYellow, TemariGreen),
        ) {
            Text(
                text = stringResource(R.string.exercise_seconds_left, secondsLeft),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                // 読み上げ機能が、変わった秒数を読む。
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        TemariCard {
            Text(text = stringResource(exercise.howTo), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** 1つ終わったときのお祝い。 */
@Composable
private fun DoneView(exercise: Exercise, exercisesToday: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiConfig.ButtonSpacing),
    ) {
        Mascot(mood = MascotMood.Excited, size = UiConfig.MascotLarge)
        Text(
            text = stringResource(R.string.exercise_done),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.exercise_done_detail, stringResource(exercise.title)),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.exercise_today_count, exercisesToday),
            style = MaterialTheme.typography.titleLarge,
            color = TemariGreen,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 1400, name = "一覧")
@Composable
private fun ExerciseChoosePreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) {
        ExerciseScreen(ExerciseUiState(ExercisePhase.Choose, 1), onStart = {}, onBackToList = {}, onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "タイマー")
@Composable
private fun ExerciseTimerPreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) {
        ExerciseScreen(
            ExerciseUiState(ExercisePhase.Doing(Exercise.HeelRaise, 18), 1),
            onStart = {},
            onBackToList = {},
            onBack = {},
        )
    }
}
