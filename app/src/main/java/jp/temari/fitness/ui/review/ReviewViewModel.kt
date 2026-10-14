package jp.temari.fitness.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import jp.temari.fitness.TemariApplication
import jp.temari.fitness.data.ActivityRepository
import jp.temari.fitness.data.BingoRules
import jp.temari.fitness.data.DailyActivity
import jp.temari.fitness.data.TimeBlock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate

/** ふりかえりで見ているもの。 */
enum class ReviewMode {
    /** きょう（4つの時間帯） */
    Today,

    /** 今週（月〜日） */
    Week,
}

/**
 * 今週の1日分。
 *
 * @property activity その日の活動。記録が無い日・まだ来ていない日は null。
 */
data class WeekDay(val date: LocalDate, val activity: DailyActivity?)

/**
 * ふりかえり画面に出すもの一式。
 *
 * @property weekDays 月曜から日曜までの7日分。
 */
data class ReviewUiState(
    val mode: ReviewMode,
    val today: DailyActivity,
    val weekDays: List<WeekDay>,
) {
    /** きょう、いちばん動いた時間帯。どこも 0 なら null。 */
    val bestBlock: TimeBlock?
        get() = today.blocks.filter { it.totalPoints > 0 }.maxByOrNull { it.totalPoints }?.block

    /** 今週、いちばん動いた曜日。どの日も 0 なら null。同じ値なら早い曜日。 */
    val bestDay: DayOfWeek?
        get() = weekDays
            .filter { (it.activity?.totalPoints ?: 0) > 0 }
            .maxByOrNull { it.activity?.totalPoints ?: 0 }
            ?.date?.dayOfWeek

    companion object {
        /** 記録のある日だけのリストから、月〜日の7日分を作る。 */
        fun weekDaysOf(today: LocalDate, week: List<DailyActivity>): List<WeekDay> {
            val start = BingoRules.weekStartOf(today)
            val byDate = week.associateBy { it.date }
            return (0L until 7L).map { offset ->
                val date = start.plusDays(offset)
                WeekDay(date, byDate[date])
            }
        }
    }
}

/**
 * ふりかえり画面の ViewModel。きょう／今週 の切り替えも覚えておく。
 */
class ReviewViewModel(repository: ActivityRepository) : ViewModel() {

    private val mode = MutableStateFlow(ReviewMode.Today)

    /** 画面の状態。最初の値が届くまでは null。 */
    val uiState: StateFlow<ReviewUiState?> = combine(
        mode,
        repository.todayActivity(),
        repository.weekActivities(),
    ) { selected, today, week ->
        ReviewUiState(selected, today, ReviewUiState.weekDaysOf(today.date, week))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 「きょう」「今週」が押されたとき。 */
    fun onSelectMode(selected: ReviewMode) {
        mode.value = selected
    }

    companion object {
        /** AppContainer から部品を受け取って ReviewViewModel を作る方法。 */
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TemariApplication).container
                ReviewViewModel(container.activityRepository)
            }
        }
    }
}
