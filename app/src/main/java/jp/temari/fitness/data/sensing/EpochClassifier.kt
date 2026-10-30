package jp.temari.fitness.data.sensing

import jp.temari.fitness.config.SensingConfig
import kotlin.math.max
import kotlin.math.sqrt

/**
 * 1区切り（10秒）の判別の結果。
 *
 * @property steps この区切りの歩数。
 * @property lifeSeconds 「家のこと」として数えた秒数（区切りの長さか 0）。
 * @property meanEnmoMg 揺れの平均（ENMO、mg）。スタッフ・開発用画面でしきい値を決めるのに使う。
 * @property walking 「歩いた」区切りか。
 */
data class EpochResult(
    val steps: Int,
    val lifeSeconds: Int,
    val meanEnmoMg: Float,
    val walking: Boolean,
)

/**
 * 加速度と歩数を集めて、1区切りごとに「歩いた／家のこと／動いていない」を決める。
 *
 * 判別のしかた:
 * 1. 区切りの中の歩数が [walkMinSteps] 以上なら「歩いた」。
 * 2. そうでなく、揺れの平均（ENMO）が [lifeThresholdMg] 以上なら「家のこと」（立ったり、手を動かしたりしている）。
 * 3. どちらでもなければ「動いていない」。
 *
 * ENMO（Euclidean Norm Minus One）は「加速度の大きさ − 重力（1g）」で、0 より小さいときは 0 にした値。
 * スマホの向きに関係なく、体の動きの強さだけを取り出せるので、研究でもよく使われている。
 *
 * Android に依存しない計算だけのクラスなので、単体テストで確かめられる。
 */
class EpochClassifier(
    private val epochSeconds: Int = SensingConfig.EPOCH_SECONDS,
    private val walkMinSteps: Int = SensingConfig.WALK_MIN_STEPS_PER_EPOCH,
    private val lifeThresholdMg: Float = SensingConfig.LIFE_ENMO_THRESHOLD_MG,
) {
    private var enmoSumMg = 0.0
    private var samples = 0
    private var steps = 0

    /** 加速度（m/s²、重力を含む）を1つ加える。 */
    fun addAcceleration(x: Float, y: Float, z: Float) {
        enmoSumMg += enmoMg(x, y, z)
        samples++
    }

    /** 歩数を加える。 */
    fun addSteps(count: Int) {
        if (count > 0) steps += count
    }

    /** 今の区切りを締めて結果を返し、次の区切りのために値を 0 に戻す。 */
    fun finish(): EpochResult {
        val mean = if (samples > 0) (enmoSumMg / samples).toFloat() else 0f
        val walking = steps >= walkMinSteps
        val life = !walking && mean >= lifeThresholdMg
        val result = EpochResult(
            steps = steps,
            lifeSeconds = if (life) epochSeconds else 0,
            meanEnmoMg = mean,
            walking = walking,
        )
        enmoSumMg = 0.0
        samples = 0
        steps = 0
        return result
    }

    companion object {
        /** 加速度から ENMO（mg）を計算する。 */
        fun enmoMg(x: Float, y: Float, z: Float): Float {
            val magnitude = sqrt(x * x + y * y + z * z)
            return max(magnitude - SensingConfig.GRAVITY, 0f) / SensingConfig.GRAVITY * 1000f
        }
    }
}

/**
 * 歩数計センサーが無い端末のために、加速度の揺れから歩数を数える簡単なしくみ。
 *
 * 加速度の大きさをなめらかにし、重力より [threshold] だけ大きくなった瞬間（下から上へ越えたとき）を1歩と数える。
 * 歩数計センサーほど正確ではないので、あくまで予備として使う。
 */
class StepDetector(
    private val threshold: Float = SensingConfig.STEP_DETECT_THRESHOLD_MS2,
    private val minIntervalMs: Long = SensingConfig.STEP_MIN_INTERVAL_MS,
    private val smoothing: Float = SensingConfig.STEP_SMOOTHING,
) {
    private var smoothed = SensingConfig.GRAVITY
    private var above = false
    private var lastStepAtMs = Long.MIN_VALUE / 2

    /**
     * 加速度を1つ加える。1歩と数えたら true を返す。
     *
     * @param timeMs その値を測った時刻（ミリ秒）。
     */
    fun add(x: Float, y: Float, z: Float, timeMs: Long): Boolean {
        val magnitude = sqrt(x * x + y * y + z * z)
        smoothed += (magnitude - smoothed) * smoothing
        val nowAbove = smoothed - SensingConfig.GRAVITY > threshold
        val isStep = nowAbove && !above && timeMs - lastStepAtMs >= minIntervalMs
        if (isStep) lastStepAtMs = timeMs
        above = nowAbove
        return isStep
    }
}
