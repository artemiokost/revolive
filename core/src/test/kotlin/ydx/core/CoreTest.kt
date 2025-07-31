package ydx.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue

class CoreTest {

    @Test
    fun test1() {
        assertThrows<RuntimeException>("Exception") {
            throw RuntimeException("Exception")
        }
    }

    @Test
    fun test2() {
        assertTrue {
            true
        }
    }
}