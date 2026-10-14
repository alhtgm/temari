package jp.temari.fitness.data.fake

import jp.temari.fitness.config.ActivityConfig
import jp.temari.fitness.data.ActivityRepository
import jp.temari.fitness.data.BingoCard
import jp.temari.fitness.data.BingoRules
import jp.temari.fitness.data.DailyActivity
import jp.temari.fitness.data.RecordingStatus
import jp.temari.fitness.data.TimeBlock
import jp.temari.fitness.data.TimeBlockActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

/**
 * モックの [ActivityRepository]。計測はせず、シナリオに合わせて作った値を返す。
 *
 * @param clock 「今」を決める時計。テストでは固定した時計を渡して、結果を毎回同じにする。
 * @param goal 1日の目標。せってい で変えた値がここに流れてくる。
 */
class FakeActivityRepository(
    private val clock: Clock = Clock.systemDefaultZone(),
    goal: Flow<Int> = flowOf(ActivityConfig.DEFAULT_DAILY_GOAL),
) : ActivityRepository, ScenarioSwitcher {

    /** ある時点でのモックの値一式。 */
    private data class Snapshot(
        val today: DailyActivity,
        val week: List<DailyActivity>,
        val status: RecordingStatus,
    )

    private val currentScenario = MutableStateFlow(MockScenario.Normal)

    /** 「再開する」を押したかどうか。 */
    private val resumed = MutableStateFlow(false)

    override val scenario: StateFlow<MockScenario> = currentScenario.asStateFlow()

    // シナリオ・再開・目標のどれかが変わると、全部の値を作り直して流す。
    private val snapshot: Flow<Snapshot> =
        combine(currentScenario, resumed, goal) { scenario, isResumed, g -> buildSnapshot(scenario, isResumed, g) }

    override fun todayActivity(): Flow<DailyActivity> = snapshot.map { it.today }

    override fun weekActivities(): Flow<List<DailyActivity>> = snapshot.map { it.week }

    override fun recordingStatus(): Flow<RecordingStatus> = snapshot.map { it.status }

    override fun bingoCard(): Flow<BingoCard> =
        snapshot.map { BingoRules.buildCard(it.today.date, it.week) }

    override suspend fun resumeRecording() {
        resumed.value = true
    }

    override fun setScenario(scenario: MockScenario) {
        // シナリオを替えたら「再開した」状態も元に戻す。
        resumed.value = false
        currentScenario.value = scenario
    }

    private fun buildSnapshot(scenario: MockScenario, isResumed: Boolean, goal: Int): Snapshot {
        val now = LocalDateTime.now(clock)
        val today = now.toLocalDate()
        val profile = MockProfiles.of(scenario)

        val stopHour = profile.stopHour
        // まだ始まっていない時間帯にはデータを入れない（今が 8 時なら 朝 だけ）。
        // 0〜5 時はどの時間帯にも入らないので、モックでは 1 日分すべてを見せる（画面の確認をしやすくするため）。
        val startedBlocks = if (now.hour < ActivityConfig.MORNING_START_HOUR) {
            TimeBlock.entries
        } else {
            TimeBlock.entries.filter { it.startHour <= now.hour }
        }
        val todayActivity = if (stopHour != null) {
            // 記録が止まったシナリオ: 止まった時刻より前の時間帯だけ。割合はそのまま（止まった分だけ少なくなる）。
            makeDay(
                today, goal, profile.todayRatio, profile.todayShape, profile.walkShare,
                included = startedBlocks.filter { it.startHour < stopHour },
                renormalize = false,
            )
        } else {
            // それ以外: 始まった時間帯に割り振り直して、シナリオどおりの合計（例: 目標の 70%）になるようにする。
            makeDay(
                today, goal, profile.todayRatio, profile.todayShape, profile.walkShare,
                included = startedBlocks,
                renormalize = true,
            )
        }

        val weekStart = BingoRules.weekStartOf(today)
        val pastDays = profile.pastRatios
            .mapIndexedNotNull { index, ratio ->
                val date = weekStart.plusDays(index.toLong())
                // 今日より前で、記録がある（ratio が null でない）日だけ。
                if (ratio == null || !date.isBefore(today)) {
                    null
                } else {
                    makeDay(
                        date, goal, ratio, profile.pastShape, profile.walkShare,
                        included = TimeBlock.entries,
                        renormalize = false,
                    )
                }
            }

        val status = if (stopHour != null && !isResumed) {
            val stoppedAt = today.atTime(stopHour, 0)
            // 今がまだ止まる時刻より前なら、今止まったことにする（未来の時刻にしないため）。
            RecordingStatus.Stopped(if (stoppedAt.isAfter(now)) now else stoppedAt)
        } else {
            RecordingStatus.Recording
        }
        return Snapshot(todayActivity, pastDays + todayActivity, status)
    }

    /**
     * 1日分の活動を作る。
     *
     * @param ratio 目標に対する1日の合計の割合（例: 0.7 なら目標の 70%）。
     * @param shape 時間帯ごとの割合。
     * @param walkShare 各時間帯のうち「歩いた」の割合。残りが「家のこと」。
     * @param included データを入れる時間帯。それ以外は 0。
     * @param renormalize true なら、入れる時間帯だけで合計が ratio になるように割合を調整する。
     */
    private fun makeDay(
        date: LocalDate,
        goal: Int,
        ratio: Float,
        shape: DayShape,
        walkShare: Float,
        included: List<TimeBlock>,
        renormalize: Boolean,
    ): DailyActivity {
        val total = goal * ratio
        val weightSum = included.sumOf { shape.weight(it).toDouble() }.toFloat()
        val blocks = TimeBlock.entries.map { block ->
            val weight = when {
                block !in included -> 0f
                renormalize && weightSum > 0f -> shape.weight(block) / weightSum
                else -> shape.weight(block)
            }
            val blockTotal = (total * weight).roundToInt()
            val walk = (blockTotal * walkShare).roundToInt()
            TimeBlockActivity(block, walkPoints = walk, lifePoints = blockTotal - walk)
        }
        val walkPoints = blocks.sumOf { it.walkPoints }
        return DailyActivity(
            date = date,
            walkPoints = walkPoints,
            lifePoints = blocks.sumOf { it.lifePoints },
            steps = walkPoints * ActivityConfig.STEPS_PER_WALK_POINT,
            goal = goal,
            blocks = blocks,
        )
    }
}
