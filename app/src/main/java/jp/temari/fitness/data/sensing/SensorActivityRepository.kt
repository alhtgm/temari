package jp.temari.fitness.data.sensing

import jp.temari.fitness.data.ActivityRepository
import jp.temari.fitness.data.ActivityStats
import jp.temari.fitness.data.ActivitySummary
import jp.temari.fitness.data.BingoCard
import jp.temari.fitness.data.BingoRules
import jp.temari.fitness.data.DailyActivity
import jp.temari.fitness.data.RecordingStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/** 日付が変わったかを調べ直す間隔（ミリ秒）。 */
private const val DATE_CHECK_MILLIS = 60_000L

/**
 * 本物の計測の [ActivityRepository]。[ActivityStore] の記録を、画面に出す形に直して返す。
 *
 * @param goal 1日の目標。せってい で変えた値が流れてくる。
 * @param startRecording 記録するサービスを始める処理（「再開する」で呼ぶ）。
 */
class SensorActivityRepository(
    private val store: ActivityStore,
    private val goal: Flow<Int>,
    private val clock: Clock,
    private val startRecording: () -> Unit,
) : ActivityRepository {

    /** 今日の日付。日付が変わったら新しい値を流す（アプリを開いたまま夜中の 0 時をまたいでも、今日の表示が変わるように）。 */
    private val today: Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now(clock))
            delay(DATE_CHECK_MILLIS)
        }
    }.distinctUntilChanged()

    override fun todayActivity(): Flow<DailyActivity> =
        combine(store.log, goal, today) { log, g, date ->
            (log.days[date] ?: DayRecord()).toDailyActivity(date, g)
        }

    override fun weekActivities(): Flow<List<DailyActivity>> =
        combine(store.log, goal, today) { log, g, date -> weekOf(log, g, date) }

    override fun recordingStatus(): Flow<RecordingStatus> =
        combine(store.running, store.log.map { it.lastRecordedAt }.distinctUntilChanged()) { running, last ->
            if (running) {
                RecordingStatus.Recording
            } else {
                // 一度も記録していなければ、今止まっていることにする。
                RecordingStatus.Stopped(last ?: LocalDateTime.now(clock))
            }
        }

    override fun bingoCard(): Flow<BingoCard> =
        combine(store.log, goal, today) { log, g, date -> BingoRules.buildCard(date, weekOf(log, g, date)) }

    override fun summary(): Flow<ActivitySummary> =
        combine(store.log, today) { log, date ->
            val activeDates = log.days
                .filterValues { day -> day.blocks.values.any { it.steps > 0 || it.lifeSeconds > 0 } }
                .keys
            ActivitySummary(
                totalSteps = log.days.values.sumOf { it.steps },
                streakDays = ActivityStats.currentStreak(date, activeDates),
                exercisesToday = log.days[date]?.exercises ?: 0,
            )
        }

    override suspend fun resumeRecording() {
        startRecording()
    }

    override suspend fun recordExercise() {
        store.addExercise(LocalDate.now(clock))
    }

    /** 今週（月曜〜今日）の、記録がある日。今日は記録が無くても入れる（インターフェースの約束）。 */
    private fun weekOf(log: ActivityLog, goal: Int, today: LocalDate): List<DailyActivity> {
        val start = BingoRules.weekStartOf(today)
        return generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(today) }
            .mapNotNull { date ->
                val day = log.days[date]
                when {
                    date == today -> (day ?: DayRecord()).toDailyActivity(date, goal)
                    day != null && day.hasRecord -> day.toDailyActivity(date, goal)
                    else -> null
                }
            }
            .toList()
    }
}
