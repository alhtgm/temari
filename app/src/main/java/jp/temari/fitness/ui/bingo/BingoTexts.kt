package jp.temari.fitness.ui.bingo

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jp.temari.fitness.R
import jp.temari.fitness.config.ActivityConfig
import jp.temari.fitness.config.BingoConfig
import jp.temari.fitness.data.BingoCellId
import jp.temari.fitness.data.TimeBlock

/** マスの短い名前。 */
@Composable
fun bingoCellTitle(id: BingoCellId): String = stringResource(
    when (id) {
        BingoCellId.MorningActive -> R.string.bingo_morning_active_title
        BingoCellId.GoalOnce -> R.string.bingo_goal_once_title
        BingoCellId.LifeMaster -> R.string.bingo_life_master_title
        BingoCellId.NightLittle -> R.string.bingo_night_little_title
        BingoCellId.Free -> R.string.bingo_free_title
        BingoCellId.WalkGood -> R.string.bingo_walk_good_title
        BingoCellId.RecordStreak -> R.string.bingo_record_streak_title
        BingoCellId.GoalThreeTimes -> R.string.bingo_goal_three_title
        BingoCellId.EveryDay -> R.string.bingo_every_day_title
    },
)

/**
 * マスの条件の説明。
 *
 * しきい値や時刻は config の定数から入れる（定数を変えたら説明も自動で変わるようにするため）。
 */
@Composable
fun bingoCellDescription(id: BingoCellId): String = when (id) {
    BingoCellId.MorningActive -> stringResource(
        R.string.bingo_morning_active_description,
        TimeBlock.Morning.startHour, TimeBlock.Morning.endHour, BingoConfig.MORNING_ACTIVE_POINTS,
    )
    BingoCellId.GoalOnce -> stringResource(R.string.bingo_goal_once_description)
    BingoCellId.LifeMaster ->
        stringResource(R.string.bingo_life_master_description, BingoConfig.LIFE_MASTER_POINTS)
    BingoCellId.NightLittle -> stringResource(
        R.string.bingo_night_little_description, TimeBlock.Night.startHour, TimeBlock.Night.endHour,
    )
    BingoCellId.Free -> stringResource(R.string.bingo_free_description)
    BingoCellId.WalkGood -> stringResource(
        R.string.bingo_walk_good_description,
        BingoConfig.WALK_GOOD_POINTS, BingoConfig.WALK_GOOD_POINTS * ActivityConfig.STEPS_PER_WALK_POINT,
    )
    BingoCellId.RecordStreak ->
        stringResource(R.string.bingo_record_streak_description, BingoConfig.RECORD_STREAK_DAYS)
    BingoCellId.GoalThreeTimes ->
        stringResource(R.string.bingo_goal_three_description, BingoConfig.GOAL_DAYS_MANY)
    BingoCellId.EveryDay -> stringResource(R.string.bingo_every_day_description)
}
