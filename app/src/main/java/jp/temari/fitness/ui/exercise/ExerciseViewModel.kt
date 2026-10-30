package jp.temari.fitness.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import jp.temari.fitness.TemariApplication
import jp.temari.fitness.config.ExerciseConfig
import jp.temari.fitness.data.ActivityRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** たいそう画面の今の段階。 */
sealed interface ExercisePhase {
    /** 体操を選ぶ一覧。 */
    data object Choose : ExercisePhase

    /**
     * はじまる前の「よーい」。
     *
     * @property secondsLeft はじまるまでの秒数。
     */
    data class Ready(val exercise: Exercise, val secondsLeft: Int) : ExercisePhase

    /**
     * 体操をしている。
     *
     * @property secondsLeft のこりの秒数。
     */
    data class Doing(val exercise: Exercise, val secondsLeft: Int) : ExercisePhase

    /** 1つ終わった。 */
    data class Done(val exercise: Exercise) : ExercisePhase
}

/**
 * たいそう画面に出すもの一式。
 *
 * @property exercisesToday 今日した回数。
 */
data class ExerciseUiState(
    val phase: ExercisePhase,
    val exercisesToday: Int,
)

/**
 * たいそう画面の ViewModel。数え下げのタイマーを動かし、終わったら回数を記録する。
 */
class ExerciseViewModel(private val repository: ActivityRepository) : ViewModel() {

    private val phase = MutableStateFlow<ExercisePhase>(ExercisePhase.Choose)
    private var timer: Job? = null

    /** 画面の状態。最初の値が届くまでは null。 */
    val uiState: StateFlow<ExerciseUiState?> = combine(phase, repository.summary()) { p, summary ->
        ExerciseUiState(p, summary.exercisesToday)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 体操が選ばれたとき。「よーい」から始めて、数え下げる。 */
    fun onStart(exercise: Exercise) {
        timer?.cancel()
        timer = viewModelScope.launch {
            for (left in ExerciseConfig.READY_SECONDS downTo 1) {
                phase.value = ExercisePhase.Ready(exercise, left)
                delay(ONE_SECOND)
            }
            for (left in ExerciseConfig.DURATION_SECONDS downTo 1) {
                phase.value = ExercisePhase.Doing(exercise, left)
                delay(ONE_SECOND)
            }
            repository.recordExercise()
            phase.value = ExercisePhase.Done(exercise)
        }
    }

    /** 途中で「おわる」、または終わった後に「一覧へ」が押されたとき。 */
    fun onBackToList() {
        timer?.cancel()
        phase.value = ExercisePhase.Choose
    }

    companion object {
        private const val ONE_SECOND = 1_000L

        /** AppContainer から部品を受け取って ExerciseViewModel を作る方法。 */
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TemariApplication).container
                ExerciseViewModel(container.activityRepository)
            }
        }
    }
}
