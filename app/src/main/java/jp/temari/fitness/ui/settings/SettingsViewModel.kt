package jp.temari.fitness.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import jp.temari.fitness.TemariApplication
import jp.temari.fitness.config.TextSizeOption
import jp.temari.fitness.data.settings.SettingsRepository
import jp.temari.fitness.data.settings.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * せってい画面に出すもの一式。
 *
 * @property settings 保存されている設定。
 * @property nicknameSaved 呼び名を保存した直後なら true（「ほぞんしました」を出す）。
 */
data class SettingsUiState(
    val settings: UserSettings,
    val nicknameSaved: Boolean,
)

/**
 * せってい画面の ViewModel。変更はすぐに DataStore に保存する。
 */
class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val nicknameSaved = MutableStateFlow(false)

    /** 画面の状態。最初の値が届くまでは null。 */
    val uiState: StateFlow<SettingsUiState?> =
        combine(repository.settings, nicknameSaved) { settings, saved -> SettingsUiState(settings, saved) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 文字の大きさが選ばれたとき。 */
    fun onTextSizeSelect(textSize: TextSizeOption) {
        viewModelScope.launch { repository.setTextSize(textSize) }
    }

    /** 呼び名の入力欄が変わったとき。「ほぞんしました」を消す。 */
    fun onNicknameEdit() {
        nicknameSaved.value = false
    }

    /** 「よび名を ほぞんする」が押されたとき。 */
    fun onNicknameSave(nickname: String) {
        viewModelScope.launch {
            repository.setNickname(nickname)
            nicknameSaved.value = true
        }
    }

    /** 「−」「＋」で目標が変わったとき。 */
    fun onGoalChange(goal: Int) {
        viewModelScope.launch { repository.setDailyGoal(goal) }
    }

    companion object {
        /** AppContainer から部品を受け取って SettingsViewModel を作る方法。 */
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TemariApplication).container
                SettingsViewModel(container.settingsRepository)
            }
        }
    }
}
