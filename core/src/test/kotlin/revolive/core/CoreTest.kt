package revolive.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import revolive.core.LoadBalancer.WARN_CAPACITY
import revolive.core.LoadBalancer.WARN_EXISTS
import kotlin.test.assertTrue

class CoreTest {

    @Test
    fun testRegistration() {
        val firstPart = (1..10).map { "https://test$it.com" }
        val secondPart = (11..20).map { "https://test$it.com" }

        firstPart.forEach { LoadBalancer.register(it) }

        val random1 = firstPart.random()
        assertThrows<RuntimeException>(WARN_EXISTS) {
            LoadBalancer.register(random1)
        }

        val random2 = secondPart.random()
        assertThrows<RuntimeException>(WARN_CAPACITY) {
            LoadBalancer.register(random2)
        }
    }

    @Test
    fun testGetter() {
        val firstPart = (1..10).map { "https://test$it.com" }

        firstPart.forEach { LoadBalancer.register(it) }

        val random = LoadBalancer.getRandom()
        assertTrue {
            firstPart.contains(random)
        }
    }
}