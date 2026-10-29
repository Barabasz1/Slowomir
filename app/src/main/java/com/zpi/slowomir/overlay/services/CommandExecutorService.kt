package com.zpi.slowomir.overlay.services

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.zpi.slowomir.overlay.executors.GridExecutor
import com.zpi.slowomir.overlay.executors.MacroExecutor
import com.zpi.slowomir.overlay.executors.MediaExecutor
import com.zpi.slowomir.overlay.executors.NavigationExecutor
import com.zpi.slowomir.overlay.executors.PhoneCallExecutor
import com.zpi.slowomir.overlay.executors.PredictionExecutor
import com.zpi.slowomir.overlay.executors.TextEditExecutor
import com.zpi.slowomir.overlay.executors.TouchExecutor
import com.zpi.slowomir.voice.mediator.CommandMediator
import com.zpi.slowomir.overlay.ui.grid.GridService
import com.zpi.slowomir.voice.VoiceSessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Core Accessibility Service responsible for bridging application commands with Android APIs.
 *
 * This service operates as a singleton context provider that delegates specialized duties
 * (touch emulation, text injection, navigation, media control, and window overlays) to dedicated executor modules.
 *
 * @see TouchExecutor
 * @see GridExecutor
 * @see MacroExecutor
 * @see MediaExecutor
 * @see NavigationExecutor
 * @see PhoneCallExecutor
 * @see PredictionExecutor
 * @see TextEditExecutor
 */
@SuppressLint("AccessibilityPolicy")
@AndroidEntryPoint
class CommandExecutorService : AccessibilityService() {

    @Inject
    lateinit var mediator: CommandMediator

    @Inject
    lateinit var voiceSessionManager: VoiceSessionManager

    // Executors for different modules
    var touchExecutor: TouchExecutor? = null
        private set

    var gridExecutor: GridExecutor? = null
        private set

    var macroExecutor: MacroExecutor? = null
        private set

    var mediaExecutor: MediaExecutor? = null
        private set

    var navigationExecutor: NavigationExecutor? = null
        private set

    var phoneCallExecutor: PhoneCallExecutor? = null
        private set

    var predictionExecutor: PredictionExecutor? = null
        private set

    var textEditExecutor: TextEditExecutor? = null
        private set

    companion object {
        var instance: CommandExecutorService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        gridExecutor = GridExecutor(GridService(this))
        gridExecutor?.toggle()

        touchExecutor = TouchExecutor(this)
        macroExecutor = MacroExecutor(this)
        mediaExecutor = MediaExecutor(this)
        navigationExecutor = NavigationExecutor(this)
        phoneCallExecutor = PhoneCallExecutor(this)
        predictionExecutor = PredictionExecutor(this)
        textEditExecutor = TextEditExecutor(this)
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        clearExecutors()
        instance = null
    }

    override fun onUnbind(intent: Intent?): Boolean {
        clearExecutors()
        instance = null
        return super.onUnbind(intent)
    }

    private fun clearExecutors() {
        touchExecutor = null
        gridExecutor = null
        macroExecutor = null
        mediaExecutor = null
        navigationExecutor = null
        phoneCallExecutor = null
        predictionExecutor = null
        textEditExecutor = null
    }
}
