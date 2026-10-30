package jp.temari.fitness.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import jp.temari.fitness.TemariApplication
import jp.temari.fitness.data.fake.MockScenario
import jp.temari.fitness.data.fake.ScenarioSwitcher
import jp.temari.fitness.data.sensing.ActivityStore
import jp.temari.fitness.data.sensing.SensorDiagnostics
import jp.temari.fitness.data.settings.DataSource
import jp.temari.fitness.data.settings.SettingsRepository
import jp.temari.fitness.service.RecordingController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 本物の計測の様子（スタッフ・開発用画面に出す）。
 *
 * @property running 記録するサービスが動いているか。
 * @property hasPermission 「身体活動」が許可されているか。
 * @property lastRecordedAt 最後に記録した日時。
 * @property todaySteps 今日の歩数（本物の計測）。
 */
data class SensorStatus(
    val running: Boolean,
    val hasPermission: Boolean,
    val diagnostics: SensorDiagnostics,
    val lastRecordedAt: LocalDateTime?,
    val todaySteps: Int,
)

/**
 * スタッフ・開発用画面に出すもの一式。
 *
 * @property scenario 今のモックのシナリオ。
 * @property dataSource 今のデータの出どころ。
 * @property userId 利用者ID。まだ発行していなければ null。
 * @property sensor 本物の計測の様子。
 * @property showRestartDialog 「初回設定をやり直しますか？」の確認を出しているか。
 */
data class StaffUiState(
    val scenario: MockScenario,
    val dataSource: DataSource,
    val userId: String?,
    val sensor: SensorStatus,
    val showRestartDialog: Boolean = false,
)

/**
 * スタッフ・開発用画面の ViewModel。
 */
class StaffViewModel(
    private val scenarioSwitcher: ScenarioSwitcher,
    private val settingsRepository: SettingsRepository,
    store: ActivityStore,
    private val recordingController: RecordingController,
    private val clock: Clock,
) : ViewModel() {

    private val showRestartDialog = MutableStateFlow(false)

    private val sensorStatus = combine(store.running, store.diagnostics, store.log) { running, diagnostics, log ->
        val today = LocalDate.now(clock)
        SensorStatus(
            running = running,
            hasPermission = recordingController.hasPermission(),
            diagnostics = diagnostics,
            lastRecordedAt = log.lastRecordedAt,
            todaySteps = log.days[today]?.steps ?: 0,
        )
    }

    /** 画面の状態。最初の値が届くまでは null。 */
    val uiState: StateFlow<StaffUiState?> = combine(
        scenarioSwitcher.scenario,
        settingsRepository.settings,
        sensorStatus,
        showRestartDialog,
    ) { scenario, settings, sensor, dialog ->
        StaffUiState(scenario, settings.dataSource, settings.userId, sensor, dialog)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** シナリオが選ばれたとき。モックを使っているときは、すべての画面にすぐ反映される。 */
    fun onScenarioSelect(scenario: MockScenario) {
        scenarioSwitcher.setScenario(scenario)
    }

    /** データの出どころが選ばれたとき。本物の計測なら記録を始め、モックなら止める。 */
    fun onDataSourceSelect(dataSource: DataSource) {
        viewModelScope.launch {
            settingsRepository.setDataSource(dataSource)
            recordingController.startIfReady()
        }
    }

    /** 「記録を始める」が押されたとき。 */
    fun onStartRecording() {
        recordingController.start()
    }

    /** 「初回設定のやり直し」が押されたとき。まず確認を出す。 */
    fun onRestartClick() {
        showRestartDialog.value = true
    }

    /** 確認で「やめる」が押されたとき。 */
    fun onRestartCancel() {
        showRestartDialog.value = false
    }

    /** 確認で「やり直す」が押されたとき。設定をすべて消し、記録も止めてから [onDone] を呼ぶ。 */
    fun onRestartConfirm(onDone: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.clearAll()
            recordingController.stop()
            showRestartDialog.value = false
            onDone()
        }
    }

    companion object {
        /** AppContainer から部品を受け取って StaffViewModel を作る方法。 */
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TemariApplication).container
                StaffViewModel(
                    container.scenarioSwitcher,
                    container.settingsRepository,
                    container.activityStore,
                    container.recordingController,
                    container.clock,
                )
            }
        }
    }
}
