package jp.temari.fitness.data

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 1つの時間帯の活動量。
 *
 * @property walkPoints 歩いたポイント。
 * @property lifePoints 家のこと（家事など、歩く以外の動き）のポイント。
 */
data class TimeBlockActivity(
    val block: TimeBlock,
    val walkPoints: Int,
    val lifePoints: Int,
) {
    /** この時間帯の活動ポイント（歩いた + 家のこと）。 */
    val totalPoints: Int get() = walkPoints + lifePoints
}

/**
 * 1日分の活動量。
 *
 * @property date 日付。
 * @property walkPoints その日の歩いたポイントの合計。
 * @property lifePoints その日の家のことポイントの合計。
 * @property steps その日の歩数（約）。
 * @property goal その日の目標ポイント。
 * @property blocks 時間帯ごとの内訳。4つの時間帯すべてが朝から順に入る。
 */
data class DailyActivity(
    val date: LocalDate,
    val walkPoints: Int,
    val lifePoints: Int,
    val steps: Int,
    val goal: Int,
    val blocks: List<TimeBlockActivity>,
) {
    /** 活動ポイント = 歩いたポイント + 家のことポイント。 */
    val totalPoints: Int get() = walkPoints + lifePoints

    /** 目標を達成したか。 */
    val isGoalAchieved: Boolean get() = totalPoints >= goal

    /** 目標に対する割合（0.0〜。目標を超えると 1.0 より大きくなる）。目標が 0 以下なら 1.0 とみなす。 */
    val progress: Float get() = if (goal > 0) totalPoints.toFloat() / goal else 1f

    /** 指定した時間帯の内訳。 */
    fun block(block: TimeBlock): TimeBlockActivity =
        blocks.firstOrNull { it.block == block } ?: TimeBlockActivity(block, 0, 0)
}

/** 記録（計測）の状態。 */
sealed interface RecordingStatus {
    /** 記録中。 */
    data object Recording : RecordingStatus

    /**
     * 記録が止まっている。
     *
     * @property since 止まった日時。
     */
    data class Stopped(val since: LocalDateTime) : RecordingStatus
}
