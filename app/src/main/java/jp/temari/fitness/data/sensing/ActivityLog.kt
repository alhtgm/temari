package jp.temari.fitness.data.sensing

import jp.temari.fitness.config.ActivityConfig
import jp.temari.fitness.config.SensingConfig
import jp.temari.fitness.data.DailyActivity
import jp.temari.fitness.data.TimeBlock
import jp.temari.fitness.data.TimeBlockActivity
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 1つの時間帯で測った生の値。ポイントへの換算は表示するときに行う（換算の式を後で変えても記録が使えるように）。
 *
 * @property steps 歩数。
 * @property lifeSeconds 「家のこと」として数えた秒数。
 */
data class BlockRecord(val steps: Int = 0, val lifeSeconds: Int = 0)

/**
 * 1日分の記録。
 *
 * @property blocks 時間帯ごとの値。記録が無い時間帯は入っていない。
 * @property exercises その日「たいそう」をした回数。
 */
data class DayRecord(
    val blocks: Map<TimeBlock, BlockRecord> = emptyMap(),
    val exercises: Int = 0,
) {
    /** 歩数の合計。 */
    val steps: Int get() = blocks.values.sumOf { it.steps }

    /** 計測の記録が1つでもあるか（体操だけの日は「記録が取れた日」に入れない）。 */
    val hasRecord: Boolean get() = blocks.isNotEmpty()

    /** 画面に出す形（ポイント）に直す。 */
    fun toDailyActivity(date: LocalDate, goal: Int): DailyActivity {
        val activities = TimeBlock.entries.map { block ->
            val record = blocks[block] ?: BlockRecord()
            TimeBlockActivity(
                block = block,
                walkPoints = record.steps / ActivityConfig.STEPS_PER_WALK_POINT,
                lifePoints = record.lifeSeconds / SensingConfig.LIFE_SECONDS_PER_POINT,
            )
        }
        return DailyActivity(
            date = date,
            walkPoints = activities.sumOf { it.walkPoints },
            lifePoints = activities.sumOf { it.lifePoints },
            steps = steps,
            goal = goal,
            blocks = activities,
        )
    }
}

/**
 * 端末に保存する記録の全体。
 *
 * @property days 日ごとの記録。
 * @property lastRecordedAt 最後にセンサーの値を記録した日時。記録が止まった時刻の表示に使う。
 */
data class ActivityLog(
    val days: Map<LocalDate, DayRecord> = emptyMap(),
    val lastRecordedAt: LocalDateTime? = null,
) {
    /**
     * 1区切りの結果を足した新しい記録を返す。
     *
     * 0〜5 時は集計しない（どの時間帯にも入らない）ので、最後に記録した時刻だけを進める。
     */
    fun plusEpoch(at: LocalDateTime, result: EpochResult): ActivityLog {
        val block = TimeBlock.of(at.hour) ?: return copy(lastRecordedAt = at)
        val date = at.toLocalDate()
        val day = days[date] ?: DayRecord()
        val old = day.blocks[block] ?: BlockRecord()
        val updated = old.copy(steps = old.steps + result.steps, lifeSeconds = old.lifeSeconds + result.lifeSeconds)
        return copy(
            days = days + (date to day.copy(blocks = day.blocks + (block to updated))),
            lastRecordedAt = at,
        )
    }

    /** 「たいそう」を1回足した新しい記録を返す。 */
    fun plusExercise(date: LocalDate): ActivityLog {
        val day = days[date] ?: DayRecord()
        return copy(days = days + (date to day.copy(exercises = day.exercises + 1)))
    }

    /** [today] から [keepDays] 日より古い日を消した記録を返す（ファイルが大きくなり続けないように）。 */
    fun pruned(today: LocalDate, keepDays: Long = SensingConfig.KEEP_DAYS): ActivityLog {
        val oldest = today.minusDays(keepDays)
        return copy(days = days.filterKeys { !it.isBefore(oldest) })
    }

    companion object {
        private const val HEADER = "temari-log-v1"
        private const val LAST = "last"
        private const val BLOCK = "block"
        private const val EXERCISE = "ex"

        /**
         * ファイルに書く文字列にする。1行1件のかんたんな形（カンマ区切り）。
         *
         * 例:
         * ```
         * temari-log-v1
         * last,2026-10-30T10:15:20
         * block,2026-10-30,Morning,1234,600
         * ex,2026-10-30,2
         * ```
         * JSON のライブラリを使わずに済み、単体テストでも確かめやすいので、この形にしている。
         */
        fun encode(log: ActivityLog): String = buildString {
            appendLine(HEADER)
            log.lastRecordedAt?.let { appendLine("$LAST,$it") }
            log.days.toSortedMap().forEach { (date, day) ->
                day.blocks.toSortedMap().forEach { (block, record) ->
                    appendLine("$BLOCK,$date,${block.name},${record.steps},${record.lifeSeconds}")
                }
                if (day.exercises > 0) appendLine("$EXERCISE,$date,${day.exercises}")
            }
        }

        /** [encode] で作った文字列を読み戻す。読めない行は飛ばす（壊れた行があっても、残りの記録は使えるように）。 */
        fun decode(text: String): ActivityLog {
            val days = mutableMapOf<LocalDate, DayRecord>()
            var last: LocalDateTime? = null
            text.lineSequence().forEach { line ->
                val parts = line.trim().split(',')
                runCatching {
                    when (parts.firstOrNull()) {
                        LAST -> last = LocalDateTime.parse(parts[1])
                        BLOCK -> {
                            val date = LocalDate.parse(parts[1])
                            val block = TimeBlock.valueOf(parts[2])
                            val day = days[date] ?: DayRecord()
                            val record = BlockRecord(parts[3].toInt(), parts[4].toInt())
                            days[date] = day.copy(blocks = day.blocks + (block to record))
                        }
                        EXERCISE -> {
                            val date = LocalDate.parse(parts[1])
                            val day = days[date] ?: DayRecord()
                            days[date] = day.copy(exercises = parts[2].toInt())
                        }
                    }
                }
            }
            return ActivityLog(days, last)
        }
    }
}
