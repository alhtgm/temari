package jp.temari.fitness.data

import jp.temari.fitness.config.BingoConfig
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * 1週間の活動からビンゴの各マスが開いたかを決める（SPEC_UI.md 3.3 の表）。
 *
 * 画面にもモックにも依存しない計算だけのオブジェクトなので、本物の計測に替わってもそのまま使える。
 */
object BingoRules {

    /** [date] を含む週の月曜日。 */
    fun weekStartOf(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /**
     * ビンゴカードを作る。
     *
     * @param today 今日の日付（「毎日コツコツ」で今日までを数えるため）。
     * @param week 今週の、記録が取れた日の活動。記録が無い日は入れない。
     */
    fun buildCard(today: LocalDate, week: List<DailyActivity>): BingoCard {
        val cells = BingoCellId.entries.map { id -> BingoCell(id, isAchieved(id, today, week)) }
        return BingoCard(weekStartOf(today), cells)
    }

    private fun isAchieved(id: BingoCellId, today: LocalDate, week: List<DailyActivity>): Boolean =
        when (id) {
            BingoCellId.MorningActive ->
                week.any { it.block(TimeBlock.Morning).totalPoints >= BingoConfig.MORNING_ACTIVE_POINTS }
            BingoCellId.GoalOnce -> week.count { it.isGoalAchieved } >= 1
            BingoCellId.GoalThreeTimes -> week.count { it.isGoalAchieved } >= BingoConfig.GOAL_DAYS_MANY
            BingoCellId.LifeMaster -> week.any { it.lifePoints >= BingoConfig.LIFE_MASTER_POINTS }
            BingoCellId.NightLittle ->
                week.any { it.block(TimeBlock.Night).totalPoints >= BingoConfig.NIGHT_ANY_POINTS }
            BingoCellId.RecordStreak -> longestStreak(week.map { it.date }) >= BingoConfig.RECORD_STREAK_DAYS
            BingoCellId.WalkGood -> week.any { it.walkPoints >= BingoConfig.WALK_GOOD_POINTS }
            BingoCellId.EveryDay -> everyDayActive(today, week)
            BingoCellId.Free -> true
        }

    /** 日付の並びの中で、1日ずつ続いている最長の日数。 */
    private fun longestStreak(dates: List<LocalDate>): Int {
        var longest = 0
        var current = 0
        var previous: LocalDate? = null
        for (date in dates.distinct().sorted()) {
            current = if (previous != null && previous.plusDays(1) == date) current + 1 else 1
            longest = maxOf(longest, current)
            previous = date
        }
        return longest
    }

    /** 月曜から今日まで、すべての日に少しでも活動があるか。 */
    private fun everyDayActive(today: LocalDate, week: List<DailyActivity>): Boolean {
        val activeDates = week.filter { it.totalPoints > 0 }.map { it.date }.toSet()
        var date = weekStartOf(today)
        while (!date.isAfter(today)) {
            if (date !in activeDates) return false
            date = date.plusDays(1)
        }
        return true
    }
}
