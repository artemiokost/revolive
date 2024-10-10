package revolive.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

object LoadBalancer {

    val WARN_EXISTS = "Such instance already exists!"
    val WARN_CAPACITY = "No more space!"
    val STORAGE_CAPACITY = 10

    private val INSTANCE_STORAGE = ConcurrentHashMap<String, Int>(STORAGE_CAPACITY)

    fun register(instance: String) {
        if (INSTANCE_STORAGE.size == STORAGE_CAPACITY) {
            throw RuntimeException(WARN_CAPACITY)
        }
        INSTANCE_STORAGE[instance]?.let {
            throw RuntimeException(WARN_EXISTS)
        }
        INSTANCE_STORAGE[instance] = INSTANCE_STORAGE.size + 1
    }

    fun getAllRegistered(): Enumeration<String>? {
        return INSTANCE_STORAGE.keys()
    }

    fun getRandom(): String? {
        val keys = INSTANCE_STORAGE.keys().toList()
        val rand = Random.nextInt(keys.size)
        return keys[rand]
    }
}

fun amountToReachLimit(l: BigDecimal, r: BigDecimal): BigDecimal {
    return BigDecimal.ONE.subtract(l).max(BigDecimal.ZERO).min(r).setScale(2, RoundingMode.HALF_UP)
}

fun BigDecimal.amountToReachLimit(other: BigDecimal, limit: BigDecimal = BigDecimal.ONE): BigDecimal {
    val result = limit.subtract(this).max(BigDecimal.ZERO).min(other)
    return result.setScale(2, RoundingMode.HALF_UP)
}

fun main(args: Array<String>) {
    println("Biba boba")
}