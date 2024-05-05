package fr.outadoc.pictochat.data

import android.util.Log
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

suspend fun <T> retry(
    label: String,
    times: Int = 5,
    initialDelay: Duration = 500.milliseconds,
    maxDelay: Duration = 1.seconds,
    factor: Double = 2.0,
    block: suspend () -> T,
): T {
    var currentDelay = initialDelay
    repeat(times - 1) { attemptNb ->
        try {
            return block()
        } catch (e: Exception) {
            Log.e("RetryExt", "Attempt #$attemptNb failed for $label: ${e.message}", e)
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).coerceAtMost(maxDelay)
    }

    // last attempt
    return block()
}
