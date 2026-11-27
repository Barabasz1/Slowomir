package com.zpi.slowomir.overlay.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.InputMethod
import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.zpi.slowomir.overlay.commands.touch.TapCommand
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import com.zpi.slowomir.overlay.executors.GridExecutor
import com.zpi.slowomir.overlay.commands.touch.LongTapCommand
import com.zpi.slowomir.overlay.commands.touch.TapCommand
import com.zpi.slowomir.overlay.executors.LabelsExecutor
import com.zpi.slowomir.overlay.executors.MacroExecutor
import com.zpi.slowomir.overlay.log.UiNodeDescriber
import com.zpi.slowomir.overlay.ui.common.AccessibilityUiTreeSource
import com.zpi.slowomir.overlay.executors.NavigationExecutor
import com.zpi.slowomir.overlay.executors.PhoneCallExecutor
import com.zpi.slowomir.overlay.executors.PredictionExecutor
import com.zpi.slowomir.overlay.executors.TextEditExecutor
import com.zpi.slowomir.overlay.ui.common.OverlayHostProvider
import com.zpi.slowomir.overlay.executors.TouchExecutor
import com.zpi.slowomir.utils.AccessibilityUtils
import com.zpi.slowomir.utils.PreferencesManager
import com.zpi.slowomir.overlay.ui.voice.VoiceOverlayManager
import com.zpi.slowomir.voice.dictation.FieldProfileHolder
import com.zpi.slowomir.prediction.PredictionManager
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
    lateinit var fieldProfileHolder: FieldProfileHolder

    @Inject
    lateinit var voiceSessionManager: VoiceSessionManager

    @Inject
    lateinit var overlayHostProvider: OverlayHostProvider

    @Inject
    lateinit var voiceOverlayManager: VoiceOverlayManager

    @Inject
    lateinit var screenEventsBus: ScreenEventsBus

    @Inject
    lateinit var gridExecutor: GridExecutor

    @Inject
    lateinit var macroExecutor: MacroExecutor

    @Inject
    lateinit var predictionManager: PredictionManager

    @Inject
    lateinit var tapCommandFactory: TapCommand.Factory

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var uiTree: AccessibilityUiTreeSource

    @Inject
    lateinit var tapCommandFactory: TapCommand.Factory

    @Inject
    lateinit var longTapCommandFactory: LongTapCommand.Factory

    @Inject
    lateinit var uiNodeDescriber: UiNodeDescriber

    // Executors for different modules
    var labelsExecutor: LabelsExecutor? = null
        private set

    var touchExecutor: TouchExecutor? = null
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
        val touch = TouchExecutor(this, preferencesManager)
        touchExecutor = touch
        navigationExecutor = NavigationExecutor(this, touch)
        predictionExecutor = PredictionExecutor(predictionManager, tapCommandFactory, mediator)
        textEditExecutor = TextEditExecutor(
            service = this,
            touchExecutor = touch,
            fieldCapsModes = { fieldProfileHolder.profile.capsModes },
            editorInfo = { textInputMethod?.currentInputEditorInfo },
            inputConnection = { textInputMethod?.currentInputConnection },
        )
        phoneCallExecutor = PhoneCallExecutor(this, touch)
        labelsExecutor = LabelsExecutor(
            hostProvider = overlayHostProvider,
            uiTree = uiTree,
            preferencesManager = preferencesManager,
            mediator = mediator,
            tapCommandFactory = tapCommandFactory,
            longTapCommandFactory = longTapCommandFactory,
            uiNodeDescriber = uiNodeDescriber,
        )

        fieldProfileHolder.focusProbe = {
            findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { AccessibilityUtils.isInputCandidate(it) } == true
        }

        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        predictionManager.onAccessibilityEvent(event)
        event?.let { screenEventsBus.publish(it.eventType) }
    }

    @Volatile
    private var textInputMethod: InputMethod? = null

    override fun onCreateInputMethod(): InputMethod {
        val service = this
        return object : InputMethod(service) {
            override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
                super.onStartInput(attribute, restarting)
                fieldProfileHolder.update(attribute)
                Log.d(
                    "FieldProfile",
                    "field started: pkg=${attribute.packageName} inputType=0x${attribute.inputType.toString(16)} " +
                        "imeOptions=0x${attribute.imeOptions.toString(16)} hint='${attribute.hintText}' " +
                        "label='${attribute.label}' -> ${fieldProfileHolder.profile}",
                )
            }

            override fun onFinishInput() {
                super.onFinishInput()
                fieldProfileHolder.reset()
            }
        }.also { textInputMethod = it }
    }


    override fun onInterrupt() {}

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        release()
        return super.onUnbind(intent)
    }

    /**
     * Releases all resources associated with the service.
     */
    private fun release() {
        fieldProfileHolder.focusProbe = null
        predictionManager.onServiceDisconnected()
        overlayHostProvider.useApplication()
        clearExecutors()
        instance = null
    }

    /**
     * Clears all executors associated with the service by setting them to `null`.
     */
    private fun clearExecutors() {
        labelsExecutor?.release()
        labelsExecutor = null
        touchExecutor = null
        navigationExecutor = null
        predictionExecutor = null
        textEditExecutor = null
        phoneCallExecutor = null
        textInputMethod = null
    }
}
