package jp.temari.fitness.ui.journey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import jp.temari.fitness.TemariApplication
import jp.temari.fitness.data.ActivityRepository
import jp.temari.fitness.data.JourneyProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * たび画面に出すもの一式。
 *
 * @property totalSteps これまでの歩数。
 * @property progress 今の位置。
 */
data class JourneyUiState(
    val totalSteps: Int,
    val progress: JourneyProgress,
)

/**
 * たび画面の ViewModel。これまでの歩数を、東海道の上の位置に直す。
 */
class JourneyViewModel(repository: ActivityRepository) : ViewModel() {

    /** 画面の状態。最初の値が届くまでは null。 */
    val uiState: StateFlow<JourneyUiState?> = repository.summary()
        .map { JourneyUiState(it.totalSteps, JourneyProgress.of(it.totalSteps)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        /** AppContainer から部品を受け取って JourneyViewModel を作る方法。 */
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TemariApplication).container
                JourneyViewModel(container.activityRepository)
            }
        }
    }
}
