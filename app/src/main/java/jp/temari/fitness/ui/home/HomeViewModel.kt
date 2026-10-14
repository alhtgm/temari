package jp.temari.fitness.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import jp.temari.fitness.TemariApplication
import jp.temari.fitness.data.ActivityRepository
import jp.temari.fitness.data.DailyActivity
import jp.temari.fitness.data.RecordingStatus
import jp.temari.fitness.ui.common.nowTicker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock

/**
 * ホーム画面に出すもの一式。
 *
 * @property nickname 呼び名。空ならあいさつに名前を付けない。
 * @property today 今日の活動。
 * @property status 記録の状態。
 * @property message ひとことメッセージ。
 */
data class HomeUiState(
    val nickname: String,
    val greeting: Greeting,
    val today: DailyActivity,
    val status: RecordingStatus,
    val message: HomeMessage,
)

/**
 * ホーム画面の ViewModel（画面に出すデータを用意し、ボタンの操作を受け取るクラス）。
 */
class HomeViewModel(
    private val repository: ActivityRepository,
    nickname: Flow<String>,
    clock: Clock,
) : ViewModel() {

    /** 画面の状態。最初の値が届くまでは null。 */
    val uiState: StateFlow<HomeUiState?> = combine(
        repository.todayActivity(),
        repository.recordingStatus(),
        nickname,
        nowTicker(clock),
    ) { today, status, name, now ->
        HomeUiState(
            nickname = name,
            greeting = Greeting.of(now.toLocalTime()),
            today = today,
            status = status,
            message = HomeMessage.select(today, status, now.toLocalTime()),
        )
    }.stateIn(
        scope = viewModelScope,
        // 画面が見えなくなって 5 秒たったら計算を止める（画面を回転したときなどにすぐ再開できるよう少し待つ）。
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    /** 「再開する」が押されたとき。 */
    fun onResumeRecording() {
        viewModelScope.launch { repository.resumeRecording() }
    }

    companion object {
        /** AppContainer から部品を受け取って HomeViewModel を作る方法。 */
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TemariApplication).container
                HomeViewModel(container.activityRepository, container.nickname, container.clock)
            }
        }
    }
}
