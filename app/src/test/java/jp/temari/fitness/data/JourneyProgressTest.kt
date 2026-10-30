package jp.temari.fitness.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** [JourneyProgress.of] が歩数から正しい位置を出すかを確かめる。歩幅 1m・3 つの宿場の簡単な道で調べる。 */
class JourneyProgressTest {

    private val stations = listOf(0.0, 10.0, 30.0)

    private fun progress(steps: Int) = JourneyProgress.of(steps, strideMeters = 1.0, stations = stations)

    @Test
    fun startIsAtFirstStation() {
        val p = progress(0)
        assertEquals(0, p.passedIndex)
        assertEquals(1, p.nextIndex)
        assertEquals(10.0, p.kmToNext, 1e-9)
        assertEquals(1, p.lap)
    }

    @Test
    fun betweenStations() {
        val p = progress(12_000) // 12km
        assertEquals(1, p.passedIndex)
        assertEquals(2, p.nextIndex)
        assertEquals(18.0, p.kmToNext, 1e-9)
        assertEquals(0.4f, p.routeFraction, 1e-6f)
    }

    @Test
    fun afterGoalStartsSecondLap() {
        val p = progress(35_000) // 35km = 1 周（30km）+ 5km
        assertEquals(2, p.lap)
        assertEquals(5.0, p.distanceKm, 1e-9)
        assertEquals(0, p.passedIndex)
    }
}
