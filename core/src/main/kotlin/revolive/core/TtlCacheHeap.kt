package revolive.core

import java.time.*
import java.util.*
import java.util.concurrent.*

class TtlCacheHeap(
    private val capacity: Int,
    private val defaultTtl: Duration,
    private val clock: Clock = Clock.systemUTC(),
    sweepPeriod: Duration = Duration.ofMillis(500)
) {
    private data class Entry(val value: String, val expiresAt: Instant)
    private data class Exp(val expiresAt: Instant, val key: String): Comparable<Exp> {
        override fun compareTo(other: Exp) = expiresAt.compareTo(other.expiresAt)
    }

    private val lru = Cache<String, Entry>(capacity)
    private val pq = PriorityQueue<Exp>() // min-heap по expiresAt

    private val scheduler: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "ttl-sweeper").apply { isDaemon = true }
        }

    init {
        scheduler.scheduleAtFixedRate({
            sweep()
        }, sweepPeriod.toMillis(), sweepPeriod.toMillis(), TimeUnit.MILLISECONDS)
    }

    @Synchronized
    fun put(key: String, value: String, ttl: Duration = defaultTtl) {
        require(!ttl.isZero && !ttl.isNegative)
        val exp = clock.instant().plus(ttl)
        lru[key] = Entry(value, exp)
        pq.add(Exp(exp, key))
    }

    @Synchronized
    fun get(key: String): String? {
        val e = lru[key] ?: return null
        if (e.expiresAt <= clock.instant()) {
            // локальная ленивная зачистка одного ключа
            lru.remove(key)
            return null
        }
        return e.value // LinkedHashMap обновит LRU порядок (accessOrder=true)
    }

    @Synchronized
    fun size(): Int = lru.size
    @Synchronized
    fun clear() = {
        lru.clear()
        pq.clear()
    }

    fun shutdown() { scheduler.shutdownNow() }

    @Synchronized
    private fun sweep() {
        val now = clock.instant()
        while (true) {
            val head = pq.peek() ?: break
            if (head.expiresAt > now) break
            pq.poll()
            // Может быть «старый» expiry для ключа, проверим актуальность
            val current = lru[head.key]
            if (current != null && current.expiresAt <= now) {
                lru.remove(head.key)
            }
        }
    }
}