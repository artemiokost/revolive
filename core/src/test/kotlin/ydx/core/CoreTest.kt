package ydx.core

import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock

class CoreTest {

    @Test
    fun test1() {
        val impl = CallbackSchedulerImpl()
        val calls = (1..10).map {
            val result = CallbackResult()
            val lock = ReentrantLock()
            result.isDone = true
        }
        calls.forEach {
            impl.schedule(it, Instant.now())
        }
    }
}