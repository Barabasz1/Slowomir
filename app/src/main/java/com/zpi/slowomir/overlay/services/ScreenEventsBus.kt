package com.zpi.slowomir.overlay.services

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ScreenEventsBus @Inject constructor() {
    private val _types = MutableSharedFlow<Int>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val types: SharedFlow<Int> = _types.asSharedFlow()

    fun publish(eventType: Int) {
        _types.tryEmit(eventType)
    }
}
