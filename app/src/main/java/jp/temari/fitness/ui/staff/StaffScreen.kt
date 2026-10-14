package jp.temari.fitness.ui.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.temari.fitness.R
import jp.temari.fitness.config.TextSizeOption
import jp.temari.fitness.config.UiConfig
import jp.temari.fitness.data.fake.MockScenario
import jp.temari.fitness.ui.components.BigButton
import jp.temari.fitness.ui.components.ChoiceButtons
import jp.temari.fitness.ui.components.ScreenFrame
import jp.temari.fitness.ui.theme.TemariTheme
import jp.temari.fitness.ui.theme.TextSizePreviewProvider

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
        onScenarioSelect = viewModel::onScenarioSelect,
        onRestartOnboarding = onRestartOnboarding,
        onBack = onBack,
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
fun StaffScreen(
    state: StaffUiState?,
    onScenarioSelect: (MockScenario) -> Unit,
    onRestartOnboarding: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenFrame(
        title = stringResource(R.string.staff_title),
        bottomBar = {
            BigButton(text = stringResource(R.string.button_back), onClick = onBack)
        },
    ) {
        if (state != null) {
            SectionTitle(stringResource(R.string.staff_section_scenario))
            ChoiceButtons(
                options = MockScenario.entries,
                selected = state.scenario,
                label = { scenarioLabel(it) },
                onSelect = onScenarioSelect,
                vertical = true,
            )
            HorizontalDivider()
            SectionTitle(stringResource(R.string.staff_section_user))
            Text(
                text = state.userId?.let { stringResource(R.string.staff_user_id, it) }
                    ?: stringResource(R.string.staff_user_id_none),
                style = MaterialTheme.typography.labelSmall,
            )
            BigButton(
                text = stringResource(R.string.staff_button_restart_onboarding),
                onClick = onRestartOnboarding,
                primary = false,
            )
            HorizontalDivider()
            SectionTitle(stringResource(R.string.staff_section_later))
            Column(verticalArrangement = Arrangement.spacedBy(UiConfig.SmallSpacing)) {
                listOf(R.string.staff_recording_status, R.string.staff_sensor_info, R.string.staff_data_export)
                    .forEach { item ->
                        Text(
                            text = stringResource(R.string.staff_preparing, stringResource(item)),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() },
    )
}

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

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun StaffScreenPreview(
    @PreviewParameter(TextSizePreviewProvider::class) textSize: TextSizeOption,
) {
    TemariTheme(textSize) {
        StaffScreen(
            state = StaffUiState(MockScenario.Normal, userId = "123e4567-e89b-12d3-a456-426614174000"),
            onScenarioSelect = {},
            onRestartOnboarding = {},
            onBack = {},
        )
    }
}
