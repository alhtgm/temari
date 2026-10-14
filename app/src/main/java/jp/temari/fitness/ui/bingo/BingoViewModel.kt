package jp.temari.fitness.ui.bingo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import jp.temari.fitness.TemariApplication
import jp.temari.fitness.data.ActivityRepository
import jp.temari.fitness.data.BingoCard
import jp.temari.fitness.data.BingoCellId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * ビンゴ画面に出すもの一式。
 *
 * @property card 今週のビンゴカード。
 * @property selectedCell 説明のダイアログを出しているマス。出していなければ null。
 */
data class BingoUiState(
    val card: BingoCard,
    val selectedCell: BingoCellId?,
)

/**
 * ビンゴ画面の ViewModel。どのマスの説明を開いているかも覚えておく。
 */
class BingoViewModel(repository: ActivityRepository) : ViewModel() {

    private val selectedCell = MutableStateFlow<BingoCellId?>(null)

    /** 画面の状態。最初の値が届くまでは null。 */
    val uiState: StateFlow<BingoUiState?> = combine(repository.bingoCard(), selectedCell) { card, selected ->
        BingoUiState(card, selected)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** マスが押されたとき。説明のダイアログを開く。 */
    fun onCellClick(id: BingoCellId) {
        selectedCell.value = id
    }

    /** ダイアログの「とじる」が押されたとき。 */
    fun onDialogDismiss() {
        selectedCell.value = null
    }

    companion object {
        /** AppContainer から部品を受け取って BingoViewModel を作る方法。 */
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TemariApplication).container
                BingoViewModel(container.activityRepository)
            }
        }
    }
}
