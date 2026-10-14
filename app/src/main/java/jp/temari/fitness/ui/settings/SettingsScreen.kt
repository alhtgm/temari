package jp.temari.fitness.ui.settings

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.temari.fitness.R
import jp.temari.fitness.config.ActivityConfig
import jp.temari.fitness.config.TextSizeOption
import jp.temari.fitness.config.UiConfig
import jp.temari.fitness.data.settings.UserSettings
import jp.temari.fitness.ui.components.BigButton
import jp.temari.fitness.ui.components.ChoiceButtons
import jp.temari.fitness.ui.components.GoalStepper
import jp.temari.fitness.ui.components.ScreenFrame
import jp.temari.fitness.ui.theme.TemariTheme
import jp.temari.fitness.ui.theme.TextSizePreviewProvider

/**
 * せってい画面（ViewModel とつなぐ側）。
 */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenStaff: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onTextSizeSelect = viewModel::onTextSizeSelect,
        onNicknameEdit = viewModel::onNicknameEdit,
        onNicknameSave = viewModel::onNicknameSave,
        onGoalChange = viewModel::onGoalChange,
        onBack = onBack,
        onOpenStaff = onOpenStaff,
    )
}

/**
 * せってい画面（利用者向け・表示だけ）。文字の大きさ・呼び名・1日の目標を変える。
 *
 * 画面の一番下の「スタッフ用」を 2 秒長押しすると、スタッフ・開発用画面へ進む。
 *
 * @param state 表示する内容。null（読み込み中）のときは「もどる」だけを出す。
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState?,
    onTextSizeSelect: (TextSizeOption) -> Unit,
    onNicknameEdit: () -> Unit,
    onNicknameSave: (String) -> Unit,
    onGoalChange: (Int) -> Unit,
    onBack: () -> Unit,
    onOpenStaff: () -> Unit,
) {
    ScreenFrame(
        title = stringResource(R.string.settings_title),
        bottomBar = {
            BigButton(text = stringResource(R.string.button_back), onClick = onBack)
            Text(
                text = stringResource(R.string.settings_staff_entry),
                style = MaterialTheme.typography.bodySmall,
                // 利用者の目に留まりにくいよう、灰色で目立たなくする。
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(UiConfig.ScreenPadding)
                    .longPressFor(UiConfig.STAFF_LONG_PRESS_MILLIS, onOpenStaff),
            )
        },
    ) {
        if (state != null) {
            Section(title = stringResource(R.string.settings_text_size_label)) {
                ChoiceButtons(
                    options = TextSizeOption.entries,
                    selected = state.settings.textSize,
                    label = { option ->
                        stringResource(
                            when (option) {
                                TextSizeOption.Large -> R.string.text_size_large
                                TextSizeOption.ExtraLarge -> R.string.text_size_extra_large
                            },
                        )
                    },
                    onSelect = onTextSizeSelect,
                )
            }
            HorizontalDivider()
            Section(title = stringResource(R.string.settings_nickname_label)) {
                NicknameEditor(
                    saved = state.settings.nickname,
                    showSavedMessage = state.nicknameSaved,
                    onEdit = onNicknameEdit,
                    onSave = onNicknameSave,
                )
            }
            HorizontalDivider()
            Section(title = stringResource(R.string.settings_goal_label)) {
                GoalStepper(goal = state.settings.dailyGoal, onGoalChange = onGoalChange)
            }
        }
    }
}

/** 見出しつきのまとまり。 */
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(UiConfig.SmallSpacing)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

/**
 * 呼び名の入力欄と保存ボタン。
 *
 * 入力中の文字は画面側で持ち、「ほぞんする」を押したときだけ保存する（1文字ごとに保存しない）。
 */
@Composable
private fun NicknameEditor(
    saved: String,
    showSavedMessage: Boolean,
    onEdit: () -> Unit,
    onSave: (String) -> Unit,
) {
    // saved が変わったら（読み込みが終わったときなど）入力欄もその値から始め直す。
    var draft by rememberSaveable(saved) { mutableStateOf(saved) }
    val focusManager = LocalFocusManager.current
    val save = {
        onSave(draft)
        // 保存したらキーボードを閉じる。
        focusManager.clearFocus()
    }
    Column(verticalArrangement = Arrangement.spacedBy(UiConfig.SmallSpacing)) {
        OutlinedTextField(
            value = draft,
            onValueChange = {
                draft = it.take(ActivityConfig.NICKNAME_MAX_LENGTH)
                onEdit()
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = {
                Text(stringResource(R.string.settings_nickname_placeholder), style = MaterialTheme.typography.bodyLarge)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
            modifier = Modifier.fillMaxWidth(),
        )
        BigButton(text = stringResource(R.string.settings_nickname_save), onClick = save, primary = false)
        if (showSavedMessage) {
            Text(
                text = stringResource(R.string.settings_nickname_saved),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

/**
 * 指を [millis] ミリ秒 離さずに押し続けたときだけ [onLongPress] を呼ぶ。
 *
 * Compose 標準の長押しは 0.5 秒ほどで反応してしまい、利用者がうっかりスタッフ用画面に入るおそれがあるため自作する。
 */
private fun Modifier.longPressFor(millis: Long, onLongPress: () -> Unit): Modifier =
    pointerInput(millis, onLongPress) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            // 時間内に指が離れた（またはずれた）ら released が true になる。時間切れなら false のまま。
            var released = false
            withTimeoutOrNull(millis) {
                waitForUpOrCancellation()
                released = true
            }
            if (!released) onLongPress()
        }
    }

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun SettingsScreenPreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) {
        SettingsScreen(
            state = SettingsUiState(UserSettings(textSize = textSize, nickname = "はなこ"), nicknameSaved = false),
            onTextSizeSelect = {},
            onNicknameEdit = {},
            onNicknameSave = {},
            onGoalChange = {},
            onBack = {},
            onOpenStaff = {},
        )
    }
}
