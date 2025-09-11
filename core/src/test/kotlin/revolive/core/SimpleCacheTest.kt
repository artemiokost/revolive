package revolive.core

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SimpleCacheTest {

    @AfterEach
    fun tearDown() {
        SimpleCache.clear()
    }

    @Test
    fun `put and get basic`() {
        SimpleCache.put("a", "1")
        SimpleCache.put("b", "2")

        assertEquals(2, SimpleCache.size())
        assertEquals("1", SimpleCache.get("a"))
        assertEquals("2", SimpleCache.get("b"))
        assertNull(SimpleCache.get("c"))
    }

    @Test
    fun `override value keeps size and updates value`() {
        SimpleCache.put("k", "old")
        SimpleCache.put("k", "new")

        assertEquals(1, SimpleCache.size())
        assertEquals("new", SimpleCache.get("k"))
    }

    @Test
    fun `evicts least recently used on overflow`() {
        // capacity = 5
        (1..5).forEach { SimpleCache.put("k$it", "$it") }
        // обращаемся к k2 и k3, чтобы они не были LRU
        assertEquals("2", SimpleCache.get("k2"))
        assertEquals("3", SimpleCache.get("k3"))

        // вставляем новый элемент -> должен вытесниться LRU = k1
        SimpleCache.put("k6", "6")

        assertEquals(SimpleCache.CAPACITY, SimpleCache.size())
        assertNull(SimpleCache.get("k1"), "k1 must be evicted as LRU")
        // остальные должны быть на месте
        assertEquals("2", SimpleCache.get("k2"))
        assertEquals("3", SimpleCache.get("k3"))
        assertEquals("4", SimpleCache.get("k4"))
        assertEquals("5", SimpleCache.get("k5"))
        assertEquals("6", SimpleCache.get("k6"))
    }

    @Test
    fun `get promotes key to most-recent`() {
        (1..5).forEach { SimpleCache.put("k$it", "$it") } // k1..k5

        // делаем k1 свежим (MRU)
        assertEquals("1", SimpleCache.get("k1"))

        // добавим k6 -> вытеснится LRU = k2 (а не k1)
        SimpleCache.put("k6", "6")

        assertNull(SimpleCache.get("k2"), "k2 should be LRU and evicted")
        assertEquals("1", SimpleCache.get("k1")) // всё ещё в кэше
        assertEquals("6", SimpleCache.get("k6"))
    }

    @Test
    fun `lru order can be inspected`() {
        SimpleCache.put("a", "1")
        SimpleCache.put("b", "2")
        SimpleCache.put("c", "3")

        // доступимся к 'a' -> теперь порядок LRU будет: b, c, a
        SimpleCache.get("a")

        val order = SimpleCache.keysInLruOrder()
        assertEquals(listOf("b", "c", "a"), order)
    }
}