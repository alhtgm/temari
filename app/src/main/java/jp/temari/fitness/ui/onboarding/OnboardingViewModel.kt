package jp.temari.fitness.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import jp.temari.fitness.TemariApplication
import jp.temari.fitness.data.settings.SettingsRepository
import jp.temari.fitness.data.settings.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 初回設定の ViewModel。入力された値はその場で保存する（途中でアプリを閉じても入れた分は残る）。
 */
class OnboardingViewModel(private val repository: SettingsRepository) : ViewModel() {

    /** 保存されている設定。最初の値が届くまでは null。 */
    val settings: StateFlow<UserSettings?> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * 呼び名の画面で「つぎへ」が押されたとき。呼び名を保存し、利用者IDを発行してから [onDone] を呼ぶ。
     */
    fun onNameSubmit(nickname: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.setNickname(nickname)
            repository.ensureUserId()
            onDone()
        }
    }

    /** 目標が変わったとき。 */
    fun onGoalChange(goal: Int) {
        viewModelScope.launch { repository.setDailyGoal(goal) }
    }

    /** 最後の「はじめましょう」が押されたとき。初回設定を終えたことを保存してから [onDone] を呼ぶ。 */
    fun onComplete(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.setOnboardingCompleted(true)
            onDone()
        }
    }

    companion object {
        /** AppContainer から部品を受け取って OnboardingViewModel を作る方法。 */
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TemariApplication).container
                OnboardingViewModel(container.settingsRepository)
            }
        }
    }
}
