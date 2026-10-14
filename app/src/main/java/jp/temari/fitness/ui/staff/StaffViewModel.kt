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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * スタッフ・開発用画面に出すもの一式。
 *
 * @property scenario 今のモックのシナリオ。
 * @property userId 利用者ID。まだ発行していなければ null。
 * @property showRestartDialog 「初回設定をやり直しますか？」の確認を出しているか。
 */
data class StaffUiState(
    val scenario: MockScenario,
    val userId: String?,
    val showRestartDialog: Boolean = false,
)

/**
 * スタッフ・開発用画面の ViewModel。
 */
class StaffViewModel(
    private val scenarioSwitcher: ScenarioSwitcher,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val showRestartDialog = MutableStateFlow(false)

    /** 画面の状態。最初の値が届くまでは null。 */
    val uiState: StateFlow<StaffUiState?> =
        combine(scenarioSwitcher.scenario, settingsRepository.settings, showRestartDialog) { scenario, settings, dialog ->
            StaffUiState(scenario, settings.userId, dialog)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** シナリオが選ばれたとき。すべての画面にすぐ反映される。 */
    fun onScenarioSelect(scenario: MockScenario) {
        scenarioSwitcher.setScenario(scenario)
    }

    /** 「初回設定のやり直し」が押されたとき。まず確認を出す。 */
    fun onRestartClick() {
        showRestartDialog.value = true
    }

    /** 確認で「やめる」が押されたとき。 */
    fun onRestartCancel() {
        showRestartDialog.value = false
    }

    /** 確認で「やり直す」が押されたとき。設定をすべて消してから [onDone] を呼ぶ。 */
    fun onRestartConfirm(onDone: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.clearAll()
            showRestartDialog.value = false
            onDone()
        }
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
