package com.zpi.slowomir.overlay.ui.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/**
 * Passes on the latest value once the flow has been quiet for [quietMs]. When values keep coming, the latest
 * one is passed on [maxWaitMs] after the first of the burst, so that a flow that never stops does not block
 * its consumer forever.
 */
fun <T> Flow<T>.settled(quietMs: Duration, maxWaitMs: Duration): Flow<T> = channelFlow {
    val upstream = this@settled.produceIn(this)
    var closed = false
    while (!closed) {
        val first = upstream.receiveCatching()
        if (first.isClosed) break
        var latest = first.getOrThrow()

        withTimeoutOrNull(maxWaitMs) {
            while (true) {
                val next = withTimeoutOrNull(quietMs) { upstream.receiveCatching() } ?: return@withTimeoutOrNull
                if (next.isClosed) {
                    closed = true
                    return@withTimeoutOrNull
                }
                latest = next.getOrThrow()
            }
        }
        send(latest)
    }
}
