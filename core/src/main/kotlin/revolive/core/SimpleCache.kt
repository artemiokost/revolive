package revolive.core

class Cache<K, V>(val capacity: Int) : LinkedHashMap<K, V>(capacity, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
        return size > capacity
    }
}

/**
 * Простой LRU-кэш (String -> String).
 * - Ограниченная емкость CAPACITY
 * - При переполнении удаляется LRU (least recently used)
 * - Потокобезопасность — за счёт @Synchronized на публичных методах
 */
object SimpleCache {

    const val CAPACITY: Int = 5

    // LinkedHashMap с accessOrder=true -> поддерживает порядок по обращению (LRU)
    private val map = Cache<String, String>(CAPACITY)

    @Synchronized
    fun put(key: String, value: String) {
        map[key] = value
    }

    @Synchronized
    fun get(key: String): String? = map[key]

    @Synchronized
    fun size(): Int = map.size

    @Synchronized
    fun keysInLruOrder(): List<String> = map.keys.toList()

    @Synchronized
    fun clear() = map.clear()
}