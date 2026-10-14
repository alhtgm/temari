package jp.temari.fitness.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import jp.temari.fitness.R
import jp.temari.fitness.config.ActivityConfig
import jp.temari.fitness.config.UiConfig

/**
 * 1日の目標を「−」「＋」の大きなボタンで変える部品（キーボード入力はさせない）。
 *
 * せってい と 初回設定 の両方で使う。
 *
 * @param goal 今の目標。
 * @param onGoalChange 新しい目標。下限・上限の範囲に収めてから渡す。
 */
@Composable
fun GoalStepper(goal: Int, onGoalChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiConfig.ButtonSpacing),
    ) {
        Text(
            text = stringResource(R.string.goal_value, goal),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            // 値が変わったら読み上げ機能が新しい値を読む。
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(UiConfig.ButtonSpacing)) {
            BigButton(
                text = stringResource(R.string.goal_decrease),
                onClick = { onGoalChange(clampGoal(goal - ActivityConfig.GOAL_STEP)) },
                primary = false,
                modifier = Modifier.weight(1f),
            )
            BigButton(
                text = stringResource(R.string.goal_increase),
                onClick = { onGoalChange(clampGoal(goal + ActivityConfig.GOAL_STEP)) },
                primary = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 目標を下限〜上限の範囲に収める。 */
private fun clampGoal(goal: Int): Int = goal.coerceIn(ActivityConfig.MIN_DAILY_GOAL, ActivityConfig.MAX_DAILY_GOAL)
