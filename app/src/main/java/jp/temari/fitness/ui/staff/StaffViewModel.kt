package jp.temari.fitness.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import jp.temari.fitness.TemariApplication
import jp.temari.fitness.data.fake.MockScenario
import jp.temari.fitness.data.fake.ScenarioSwitcher
import jp.temari.fitness.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * スタッフ・開発用画面に出すもの一式。
 *
 * @property scenario 今のモックのシナリオ。
 * @property userId 利用者ID。まだ発行していなければ null。
 */
data class StaffUiState(
    val scenario: MockScenario,
    val userId: String?,
)

/**
 * スタッフ・開発用画面の ViewModel。
 */
class StaffViewModel(
    private val scenarioSwitcher: ScenarioSwitcher,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /** 画面の状態。最初の値が届くまでは null。 */
    val uiState: StateFlow<StaffUiState?> =
        combine(scenarioSwitcher.scenario, settingsRepository.settings) { scenario, settings ->
            StaffUiState(scenario, settings.userId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** シナリオが選ばれたとき。すべての画面にすぐ反映される。 */
    fun onScenarioSelect(scenario: MockScenario) {
        scenarioSwitcher.setScenario(scenario)
    }

    companion object {
        /** AppContainer から部品を受け取って StaffViewModel を作る方法。 */
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TemariApplication).container
                StaffViewModel(container.scenarioSwitcher, container.settingsRepository)
            }
        }
    }
}
