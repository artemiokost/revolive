package ydx.core
//
//import java.time.Instant
//import java.util.concurrent.Callable
//import java.util.concurrent.ConcurrentHashMap
//import java.util.concurrent.atomic.AtomicBoolean
//
//interface CallbackScheduler: AutoCloseable {
//    fun schedule(callback: Callable<CallbackResult>, after: Instant)
//}
//
//class CallbackSchedulerImpl: CallbackScheduler {
//
//    val state = AtomicBoolean(true)
//    val taskMap = ConcurrentHashMap<Instant, Callable<CallbackResult>>()
//
//    init {
//        Thread {
//            while (state.get()) {
//                Thread.sleep(1000)
//                val timestamps = taskMap.keys.toSortedSet { a, b -> a.compareTo(b) }
//                val target = timestamps.first()
//                if (target > Instant.now()) {
//                    taskMap[target]?.call()
//                }
//            }
//        }
//    }
//
//    override fun schedule(callback: Callable<CallbackResult>, whenn: Instant) {
//        taskMap[whenn] = callback
//    }
//
//    override fun close() {
//        state.set(false)
//    }
//}

fun lengthOfLastWord(s: String): Int {
    val split = s.trim()
    return s.trim().split(" ").last().length
}

fun main(args: Array<String>) {
    println(lengthOfLastWord("   fly me   to   the moon  "))
}