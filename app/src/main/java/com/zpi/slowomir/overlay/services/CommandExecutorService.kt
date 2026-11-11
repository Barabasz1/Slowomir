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
import com.zpi.slowomir.overlay.ui.common.OverlayHostProvider
import com.zpi.slowomir.overlay.executors.TouchExecutor
import com.zpi.slowomir.overlay.ui.voice.VoiceOverlayManager
import com.zpi.slowomir.voice.mediator.CommandMediator
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

    @Inject
    lateinit var overlayHostProvider: OverlayHostProvider

    @Inject
    lateinit var voiceOverlayManager: VoiceOverlayManager

    @Inject
    lateinit var gridExecutor: GridExecutor

    @Inject
    lateinit var macroExecutor: MacroExecutor

    // Executors for different modules
    var touchExecutor: TouchExecutor? = null
        private set

    var mediaExecutor: MediaExecutor? = null
        private set

    var navigationExecutor: NavigationExecutor? = null
        private set

    var predictionExecutor: PredictionExecutor? = null
        private set

    var textEditExecutor: TextEditExecutor? = null
        private set

    var phoneCallExecutor: PhoneCallExecutor? = null
        private set

    companion object {
        var instance: CommandExecutorService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        overlayHostProvider.useAccessibility(this)

        // Ensure soft keyboard mode is restored to AUTO so system input is not globally suppressed
        softKeyboardController.setShowMode(SHOW_MODE_AUTO)
        val touch = TouchExecutor(this)
        touchExecutor = touch
        mediaExecutor = MediaExecutor(this)
        navigationExecutor = NavigationExecutor(this, touch)
        predictionExecutor = PredictionExecutor(this)
        textEditExecutor = TextEditExecutor(this, touch)
        phoneCallExecutor = PhoneCallExecutor(this, touch)

        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        release()
        return super.onUnbind(intent)
    }

    private fun release() {
        overlayHostProvider.useApplication()
        clearExecutors()
        instance = null
    }

    private fun clearExecutors() {
        touchExecutor = null
        mediaExecutor = null
        navigationExecutor = null
        predictionExecutor = null
        textEditExecutor = null
        phoneCallExecutor = null
    }
}
