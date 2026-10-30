package jp.temari.fitness.ui.staff

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.temari.fitness.R
import jp.temari.fitness.config.SensingConfig
import jp.temari.fitness.config.TextSizeOption
import jp.temari.fitness.data.fake.MockScenario
import jp.temari.fitness.data.sensing.SensorDiagnostics
import jp.temari.fitness.data.settings.DataSource
import jp.temari.fitness.ui.components.BigButton
import jp.temari.fitness.ui.components.ChoiceButtons
import jp.temari.fitness.ui.components.ScreenFrame
import jp.temari.fitness.ui.components.TemariCard
import jp.temari.fitness.ui.theme.TemariTheme
import jp.temari.fitness.ui.theme.TextSizePreviewProvider
import java.time.format.DateTimeFormatter

/** 「最後に記録」の日時の書き方。 */
private val RecordedAtFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d HH:mm:ss")

/**
 * スタッフ・開発用画面の操作をまとめたもの（引数が多くなりすぎないように）。
 */
data class StaffActions(
    val onScenarioSelect: (MockScenario) -> Unit,
    val onDataSourceSelect: (DataSource) -> Unit,
    val onStartRecording: () -> Unit,
    val onRestartClick: () -> Unit,
    val onRestartCancel: () -> Unit,
    val onRestartConfirm: () -> Unit,
    val onBack: () -> Unit,
)

/**
 * スタッフ・開発用画面（ViewModel とつなぐ側）。
 */
@Composable
fun StaffRoute(
    onBack: () -> Unit,
    onRestartOnboarding: () -> Unit,
    viewModel: StaffViewModel = viewModel(factory = StaffViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StaffScreen(
        state = state,
        actions = StaffActions(
            onScenarioSelect = viewModel::onScenarioSelect,
            onDataSourceSelect = viewModel::onDataSourceSelect,
            onStartRecording = viewModel::onStartRecording,
            onRestartClick = viewModel::onRestartClick,
            onRestartCancel = viewModel::onRestartCancel,
            onRestartConfirm = { viewModel.onRestartConfirm(onRestartOnboarding) },
            onBack = onBack,
        ),
    )
}

/**
 * スタッフ・開発用画面（表示だけ）。せってい画面の「スタッフ用」を長押しすると開く。
 *
 * この画面だけは文字サイズの規定（20sp 以上）を守らなくてよいので、説明は小さめの文字にしている。
 *
 * @param state 表示する内容。null（読み込み中）のときは「もどる」だけを出す。
 */
@Composable
fun StaffScreen(state: StaffUiState?, actions: StaffActions) {
    if (state?.showRestartDialog == true) {
        RestartDialog(onConfirm = actions.onRestartConfirm, onCancel = actions.onRestartCancel)
    }
    ScreenFrame(
        title = stringResource(R.string.staff_title),
        bottomBar = {
            BigButton(
                text = stringResource(R.string.button_back),
                onClick = actions.onBack,
                emoji = stringResource(R.string.emoji_back),
            )
        },
    ) {
        if (state != null) {
            TemariCard(title = stringResource(R.string.staff_section_data_source)) {
                ChoiceButtons(
                    options = DataSource.entries,
                    selected = state.dataSource,
                    label = { dataSourceLabel(it) },
                    onSelect = actions.onDataSourceSelect,
                )
            }
            TemariCard(title = stringResource(R.string.staff_section_sensor)) {
                SensorInfo(state.sensor)
                if (!state.sensor.running && state.dataSource == DataSource.Sensor) {
                    BigButton(
                        text = stringResource(R.string.staff_button_start_recording),
                        onClick = actions.onStartRecording,
                    )
                }
            }
            TemariCard(title = stringResource(R.string.staff_section_scenario)) {
                SmallText(stringResource(R.string.staff_scenario_note))
                ChoiceButtons(
                    options = MockScenario.entries,
                    selected = state.scenario,
                    label = { scenarioLabel(it) },
                    onSelect = actions.onScenarioSelect,
                    vertical = true,
                )
            }
            TemariCard(title = stringResource(R.string.staff_section_user)) {
                SmallText(
                    state.userId?.let { stringResource(R.string.staff_user_id, it) }
                        ?: stringResource(R.string.staff_user_id_none),
                )
                BigButton(
                    text = stringResource(R.string.staff_button_restart_onboarding),
                    onClick = actions.onRestartClick,
                    primary = false,
                )
            }
            TemariCard(title = stringResource(R.string.staff_section_later)) {
                SmallText(stringResource(R.string.staff_preparing, stringResource(R.string.staff_data_export)))
            }
        }
    }
}

/** 本物の計測の様子を1行ずつ出す。 */
@Composable
private fun SensorInfo(sensor: SensorStatus) {
    val yes = stringResource(R.string.staff_yes)
    val no = stringResource(R.string.staff_no)
    SmallText(
        stringResource(
            R.string.staff_recording_status_value,
            stringResource(if (sensor.running) R.string.staff_running else R.string.staff_not_running),
        ),
    )
    SmallText(stringResource(R.string.staff_permission_value, if (sensor.hasPermission) yes else no))
    SmallText(
        stringResource(
            R.string.staff_step_counter_value,
            when (sensor.diagnostics.hasStepCounter) {
                true -> yes
                false -> stringResource(R.string.staff_step_counter_fallback)
                null -> stringResource(R.string.staff_unknown)
            },
        ),
    )
    SmallText(
        stringResource(
            R.string.staff_last_recorded_value,
            sensor.lastRecordedAt?.format(RecordedAtFormat) ?: stringResource(R.string.staff_unknown),
        ),
    )
    SmallText(stringResource(R.string.staff_today_steps_value, sensor.todaySteps))
    SmallText(
        stringResource(
            R.string.staff_last_epoch_value,
            SensingConfig.EPOCH_SECONDS,
            sensor.diagnostics.lastEpochSteps,
            sensor.diagnostics.lastEnmoMg,
            SensingConfig.LIFE_ENMO_THRESHOLD_MG,
        ),
    )
}

/** 「初回設定をやり直しますか？」の確認。消える内容を書いておき、うっかり押しても戻れるようにする。 */
@Composable
private fun RestartDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(stringResource(R.string.staff_restart_dialog_title), style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(stringResource(R.string.staff_restart_dialog_text), style = MaterialTheme.typography.bodyLarge)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.staff_restart_dialog_confirm), style = MaterialTheme.typography.titleLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.staff_restart_dialog_cancel), style = MaterialTheme.typography.titleLarge)
            }
        },
    )
}

@Composable
private fun SmallText(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelSmall)
}

/** データの出どころの名前。 */
@Composable
private fun dataSourceLabel(dataSource: DataSource): String = stringResource(
    when (dataSource) {
        DataSource.Sensor -> R.string.data_source_sensor
        DataSource.Mock -> R.string.data_source_mock
    },
)

/** シナリオの名前。 */
@Composable
private fun scenarioLabel(scenario: MockScenario): String = stringResource(
    when (scenario) {
        MockScenario.Normal -> R.string.scenario_normal
        MockScenario.Active -> R.string.scenario_active
        MockScenario.Low -> R.string.scenario_low
        MockScenario.Stopped -> R.string.scenario_stopped
        MockScenario.FirstDay -> R.string.scenario_first_day
    },
)

@Preview(showBackground = true, widthDp = 360, heightDp = 1600)
@Composable
private fun StaffScreenPreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) {
        StaffScreen(
            state = StaffUiState(
                scenario = MockScenario.Normal,
                dataSource = DataSource.Sensor,
                userId = "123e4567-e89b-12d3-a456-426614174000",
                sensor = SensorStatus(
                    running = true,
                    hasPermission = true,
                    diagnostics = SensorDiagnostics(hasStepCounter = true, lastEnmoMg = 31.5f, lastEpochSteps = 6),
                    lastRecordedAt = null,
                    todaySteps = 2_345,
                ),
            ),
            actions = StaffActions({}, {}, {}, {}, {}, {}, {}),
        )
    }
}
