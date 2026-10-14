package jp.temari.fitness.ui.home

import jp.temari.fitness.config.ActivityConfig
import jp.temari.fitness.config.HomeConfig
import jp.temari.fitness.data.DailyActivity
import jp.temari.fitness.data.RecordingStatus
import jp.temari.fitness.data.TimeBlock
import java.time.LocalTime

/** 時間帯によるあいさつ。 */
enum class Greeting {
    /** おはようございます */
    Morning,

    /** こんにちは */
    Day,

    /** こんばんは */
    Evening,
    ;

    companion object {
        /** [time] に合うあいさつ。 */
        fun of(time: LocalTime): Greeting {
            val hour = time.hour
            return when {
                hour >= HomeConfig.GREETING_EVENING_START_HOUR -> Evening
                hour >= HomeConfig.GREETING_DAY_START_HOUR -> Day
                hour >= HomeConfig.GREETING_MORNING_START_HOUR -> Morning
                // 0〜4時は夜の続きとして「こんばんは」。
                else -> Evening
            }
        }
    }
}

/** ホームのひとことメッセージ（SPEC_UI.md 5章）。 */
sealed interface HomeMessage {
    /** 記録を再開しましょう */
    data object ResumeRecording : HomeMessage

    /** 目標たっせい！ すばらしいです */
    data object GoalAchieved : HomeMessage

    /** あと少しで目標です */
    data object NearGoal : HomeMessage

    /**
     * ◯◯によく動けましたね
     *
     * @property block いちばん動いた時間帯。
     */
    data class BestBlock(val block: TimeBlock) : HomeMessage

    /** すこしずつ動いていきましょう */
    data object KeepGoing : HomeMessage

    companion object {
        /**
         * 5章のルールを上から順に調べ、最初に当てはまったメッセージを返す。
         *
         * @param now 今の時刻。「今の時間帯まで」を決めるのに使う。
         */
        fun select(today: DailyActivity, status: RecordingStatus, now: LocalTime): HomeMessage = when {
            status is RecordingStatus.Stopped -> ResumeRecording
            today.isGoalAchieved -> GoalAchieved
            today.progress >= ActivityConfig.NEAR_GOAL_RATIO -> NearGoal
            else -> bestBlockUntil(today, now)?.let { BestBlock(it) } ?: KeepGoing
        }

        /** 今の時間帯までで、いちばん活動ポイントが多い時間帯。どこも 0 なら null。 */
        private fun bestBlockUntil(today: DailyActivity, now: LocalTime): TimeBlock? =
            today.blocks
                .filter { it.block.startHour <= now.hour && it.totalPoints > 0 }
                .maxByOrNull { it.totalPoints }
                ?.block
    }
}
