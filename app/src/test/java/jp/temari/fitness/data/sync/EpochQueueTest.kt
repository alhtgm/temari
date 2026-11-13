package jp.temari.fitness.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

/** [EpochQueue] のため方・取り出し方を確かめる。 */
class EpochQueueTest {

    private fun record(second: Int) = EpochRecord(LocalDateTime.of(2026, 11, 13, 9, 0, second), second, 0, 1.5f, true)

    @Test
    fun peekAndRemoveInOrder() {
        val queue = EpochQueue(null, maxSize = 10)
        (1..5).forEach { queue.add(record(it)) }
        assertEquals(listOf(1, 2), queue.peek(2).map { it.steps })
        queue.removeFirst(2)
        assertEquals(3, queue.size)
        assertEquals(3, queue.peek(1).single().steps)
    }

    @Test
    fun oldestAreDroppedOverLimit() {
        val queue = EpochQueue(null, maxSize = 3)
        (1..5).forEach { queue.add(record(it)) }
        assertEquals(listOf(3, 4, 5), queue.peek(10).map { it.steps })
    }

    @Test
    fun encodeDecodeRoundTrip() {
        val r = record(7)
        assertEquals(r, EpochQueue.decode(EpochQueue.encode(r)))
        assertNull(EpochQueue.decode("こわれた行"))
    }
}
