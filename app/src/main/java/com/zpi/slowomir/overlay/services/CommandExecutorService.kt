package com.zpi.slowomir.overlay.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.zpi.slowomir.overlay.executors.TouchExecutor


/**
 * Core Accessibility Service responsible for bridging application commands with Android APIs.
 *
 * This service operates as a singleton context provider that delegates specialized duties
 * (touch emulation, text injection, and window overlays) to dedicated executor modules.
 *
 * @see TouchExecutor
 */
class CommandExecutorService : AccessibilityService() {

    // Executors for different modules
    var touchExecutor: TouchExecutor? = null
        private set

    companion object {
        var instance: CommandExecutorService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        touchExecutor = TouchExecutor(this)
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onUnbind(intent: Intent?): Boolean {
        touchExecutor = null
        instance = null
        return super.onUnbind(intent)
    }
}