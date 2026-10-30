package jp.temari.fitness.data

import java.time.LocalDate

/**
 * 記録した日の並びから数字を出す計算。モックと本物の計測の両方で使う。
 */
object ActivityStats {
    /**
     * 今日まで記録が何日続いているか。
     *
     * 今日の記録がまだ無いとき（朝いちばんなど）は、昨日から数える。
     * 「まだ今日は記録が無いから 0 日」と出すと、続けてきた人をがっかりさせてしまうため。
     *
     * @param recordedDates 記録があった日。
     */
    fun currentStreak(today: LocalDate, recordedDates: Set<LocalDate>): Int {
        var date = if (today in recordedDates) today else today.minusDays(1)
        var count = 0
        while (date in recordedDates) {
            count++
            date = date.minusDays(1)
        }
        return count
    }
}
