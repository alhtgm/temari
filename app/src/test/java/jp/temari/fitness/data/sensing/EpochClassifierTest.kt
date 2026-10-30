package jp.temari.fitness.data.sensing

import jp.temari.fitness.config.SensingConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** [EpochClassifier] と [StepDetector] の判別を確かめる。 */
class EpochClassifierTest {

    private val g = SensingConfig.GRAVITY

    @Test
    fun stillPhoneIsNotActive() {
        val c = EpochClassifier(epochSeconds = 10, walkMinSteps = 4, lifeThresholdMg = 25f)
        repeat(200) { c.addAcceleration(0f, 0f, g) }
        val r = c.finish()
        assertFalse(r.walking)
        assertEquals(0, r.lifeSeconds)
        assertEquals(0f, r.meanEnmoMg, 0.01f)
    }

    @Test
    fun enoughStepsIsWalking() {
        val c = EpochClassifier(epochSeconds = 10, walkMinSteps = 4, lifeThresholdMg = 25f)
        c.addSteps(8)
        repeat(200) { c.addAcceleration(0f, 0f, g * 1.2f) }
        val r = c.finish()
        assertTrue(r.walking)
        assertEquals(8, r.steps)
        // 歩いた区切りは「家のこと」にしない（二重に数えない）。
        assertEquals(0, r.lifeSeconds)
    }

    @Test
    fun shakingWithoutStepsIsLife() {
        val c = EpochClassifier(epochSeconds = 10, walkMinSteps = 4, lifeThresholdMg = 25f)
        // 1.05g = 50mg の揺れ。
        repeat(200) { c.addAcceleration(0f, 0f, g * 1.05f) }
        val r = c.finish()
        assertEquals(10, r.lifeSeconds)
        assertEquals(50f, r.meanEnmoMg, 0.5f)
    }

    @Test
    fun finishResetsForNextEpoch() {
        val c = EpochClassifier()
        c.addSteps(10)
        c.finish()
        assertEquals(0, c.finish().steps)
    }

    @Test
    fun stepDetectorCountsPeaksWithMinimumInterval() {
        val d = StepDetector(threshold = 1.0f, minIntervalMs = 300, smoothing = 1f)
        var steps = 0
        // 500ms ごとに強い揺れ → 4 歩。
        for (i in 0 until 4) {
            if (d.add(0f, 0f, g + 3f, i * 500L)) steps++
            if (d.add(0f, 0f, g, i * 500L + 250)) steps++
        }
        assertEquals(4, steps)
        // 100ms 後のすぐの揺れは数えない。
        assertFalse(d.add(0f, 0f, g + 3f, 1_600L))
    }
}
