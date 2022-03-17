package ly.img.awesomebrushapplication.view.data

import kotlin.collections.ArrayDeque

class FifoQueue<T>(private val capacity: Int = 1) {
    private val queue = ArrayDeque<T>(capacity)

    fun add(item: T): Boolean {
        if (capacity == 0) {
            return true
        }
        if (queue.size == capacity) {
            queue.removeFirst()
        }
        queue.add(item)
        return true
    }

    fun clear() = queue.clear()

    fun getSize() = queue.size

    fun asList(): List<T> = queue.toList()
}