package com.zpi.slowomir.prediction

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.zpi.slowomir.overlay.commands.navigation.OpenAppCommand
import com.zpi.slowomir.overlay.commands.predictions.ConfirmCommand
import com.zpi.slowomir.overlay.commands.predictions.DismissCommand
import com.zpi.slowomir.overlay.log.ActionDescriptor
import com.zpi.slowomir.overlay.log.ActionHasher
import com.zpi.slowomir.overlay.log.ActionType
import com.zpi.slowomir.overlay.log.db.ActionDefEntity
import com.zpi.slowomir.overlay.log.db.PredictionDao
import com.zpi.slowomir.overlay.macro.MacroPlaybackState
import com.zpi.slowomir.overlay.services.CommandExecutorService
import com.zpi.slowomir.overlay.ui.prediction.PredictionOverlayManager
import com.zpi.slowomir.voice.stt.VoiceState
import com.zpi.slowomir.voice.stt.VoiceStateSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

data class ContextKeys(
    val pkg: String,
    val screenFp: String,
    val key0: String,
    val key1: String,
    val key2: String?,
)

data class ActiveSuggestion(
    val targetDef: ActionDefEntity,
    val candidate: Candidate,
    val bounds: Rect,
    val screenFp: String,
    val shownAt: Long,
)

data class ScreenSnapshot(
    val screenFp: String,
)

internal interface PredictionEnvironment {
    fun activeRoot(): AccessibilityNodeInfo?
    fun findRootForPackage(packageName: String?): AccessibilityNodeInfo? = activeRoot()
    fun screenWidth(): Int
    fun screenHeight(): Int
    fun keyboardTop(): Int?
    fun isVoiceServiceRunning(): Boolean
    fun isTextSessionActive(): Boolean
    fun isMacroPlaying(): Boolean
}

@Singleton
class DefaultPredictionEnvironment @Inject constructor(
    private val macroPlaybackState: MacroPlaybackState,
    private val voiceStateSource: VoiceStateSource,
) : PredictionEnvironment {

    override fun activeRoot(): AccessibilityNodeInfo? {
        val service = CommandExecutorService.instance ?: return null
        service.rootInActiveWindow?.let { return it }
        return runCatching {
            service.windows
                .sortedByDescending { it.layer }
                .firstOrNull { window ->
                    window.type == AccessibilityWindowInfo.TYPE_APPLICATION &&
                        window.root?.packageName?.toString()?.let { it != service.packageName } == true
                }?.root
        }.getOrNull()
    }

    override fun findRootForPackage(packageName: String?): AccessibilityNodeInfo? {
        val active = activeRoot()
        if (packageName == null || active?.packageName?.toString() == packageName) {
            return active
        }
        val service = CommandExecutorService.instance ?: return null
        return runCatching {
            service.windows
                .sortedByDescending { it.layer }
                .firstNotNullOfOrNull { window ->
                    window.root?.takeIf { it.packageName?.toString() == packageName }
                }
        }.getOrNull() ?: active
    }

    override fun screenWidth(): Int {
        val service = CommandExecutorService.instance ?: return 1080
        val metricWidth = service.getSystemService(WindowManager::class.java)
            ?.currentWindowMetrics?.bounds?.width()
            ?.takeIf { it > 0 }
        return metricWidth ?: service.resources.displayMetrics.widthPixels
    }

    override fun screenHeight(): Int {
        val service = CommandExecutorService.instance ?: return 2400
        val metricHeight = service.getSystemService(WindowManager::class.java)
            ?.currentWindowMetrics?.bounds?.height()
            ?.takeIf { it > 0 }
        return metricHeight ?: service.resources.displayMetrics.heightPixels
    }

    override fun keyboardTop(): Int? {
        val service = CommandExecutorService.instance ?: return null
        return runCatching {
            service.windows
                .firstOrNull { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
                ?.let { window ->
                    val bounds = Rect()
                    window.getBoundsInScreen(bounds)
                    bounds.top.takeIf { bounds.right > bounds.left && bounds.bottom > bounds.top }
                }
        }.getOrNull()
    }

    override fun isVoiceServiceRunning(): Boolean {
        val state = voiceStateSource.state.value
        return state !is VoiceState.Stopped && state !is VoiceState.Failed
    }

    override fun isTextSessionActive(): Boolean {
        val state = voiceStateSource.state.value
        return state is VoiceState.Dictating ||
            state is VoiceState.Editing ||
            state is VoiceState.TextProcessing
    }

    override fun isMacroPlaying(): Boolean = macroPlaybackState.isPlaying
}

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class PredictionManager internal constructor(
    private val ownPackageName: String,
    private val dao: PredictionDao,
    private val engine: VariableOrderMarkovEngine,
    private val overlayManager: PredictionOverlayManager,
    private val env: PredictionEnvironment,
    private val voiceStateFlow: Flow<VoiceState>? = null,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        dao: PredictionDao,
        engine: VariableOrderMarkovEngine,
        overlayManager: PredictionOverlayManager,
        env: DefaultPredictionEnvironment,
        voiceStateSource: VoiceStateSource,
    ) : this(context.packageName, dao, engine, overlayManager, env, voiceStateSource.state)

    private sealed interface OverlayAction {
        data object None : OverlayAction
        data class Show(val bounds: Rect, val generation: Long) : OverlayAction
        data class Clear(val generation: Long) : OverlayAction
    }

    private val workerDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Unhandled exception in prediction worker", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + workerDispatcher + exceptionHandler)
    private val stateMutex = Mutex()
    private val debounceLock = Any()

    // In-memory prediction state (guarded by stateMutex unless @Volatile)
    private val buffer = mutableListOf<String>()
    private var sessionPackage: String? = null
    private var lastCommandTime: Long = 0L
    private var currentKeys: ContextKeys? = null
    private var lastSettledScreenFp: String? = null
    private var recordedCount: Int = 0

    @Volatile
    private var activeSuggestion: ActiveSuggestion? = null
    private var lastDisplayedSuggestion: ActiveSuggestion? = null
    private var lastShownPair: Pair<String, Long>? = null

    @Volatile
    private var overlayGeneration: Long = 0L

    private var debounceJob: Job? = null
    private var debounceFirstEventTime: Long = 0L
    private var settleRunning: Boolean = false
    private var settlePending: Boolean = false
    private var suggestionTimeoutJob: Job? = null

    init {
        if (voiceStateFlow != null) {
            scope.launch {
                var wasRunning: Boolean? = null
                voiceStateFlow
                    .map { it !is VoiceState.Stopped && it !is VoiceState.Failed }
                    .distinctUntilChanged()
                    .collect { running ->
                        if (!running) {
                            onVoiceServiceStopped()
                        } else if (wasRunning == false) {
                            scheduleScreenSettled()
                        }
                        wasRunning = running
                    }
            }
        }
    }

    fun captureScreenSnapshot(action: ActionDescriptor): ScreenSnapshot? {
        if (!env.isVoiceServiceRunning() || env.isMacroPlaying() || action.packageName == ownPackageName) {
            return null
        }
        val root = env.findRootForPackage(action.packageName) ?: return null
        val rootPkg = root.packageName?.toString()
        if (rootPkg == ownPackageName) return null
        if (action.packageName != null && rootPkg != action.packageName) {
            Log.d(TAG, "captureScreenSnapshot skipped: pkg mismatch root=$rootPkg action=${action.packageName}")
            return null
        }

        val screenFp = ScreenFingerprint.compute(root, env.screenWidth(), env.screenHeight())
        if (screenFp == ScreenFingerprint.UNKNOWN_SCREEN) {
            Log.d(TAG, "captureScreenSnapshot skipped: UNKNOWN_SCREEN for pkg=$rootPkg")
            return null
        }

        return ScreenSnapshot(screenFp = screenFp)
    }

    fun onActionRecorded(
        action: ActionDescriptor,
        actionHash: String,
        actionDefId: Long,
        screenSnapshot: ScreenSnapshot? = null,
    ) {
        if (action.commandType == ConfirmCommand::class.java.name ||
            action.commandType == DismissCommand::class.java.name ||
            action.packageName == ownPackageName ||
            !env.isVoiceServiceRunning() ||
            env.isMacroPlaying()
        ) {
            return
        }

        scope.launch {
            if (!env.isVoiceServiceRunning() || env.isMacroPlaying()) return@launch

            val overlayAction: OverlayAction = stateMutex.withLock {
                val now = System.currentTimeMillis()
                val prevCommandTime = lastCommandTime
                lastCommandTime = now
                if (prevCommandTime > 0L && now - prevCommandTime > PredictionConfig.SESSION_IDLE_MS) {
                    buffer.clear()
                }
                val clearAction = clearSuggestionStateLocked()

                val keys = currentKeys
                val currentScreenFp = screenSnapshot?.screenFp
                    ?: keys?.takeIf { it.pkg == action.packageName }?.screenFp

                val pkg = action.packageName
                    ?: sessionPackage
                    ?: "unknown"

                if (action.type == ActionType.UI_CLICK && currentScreenFp != null) {
                    try {
                        val ignored = lastDisplayedSuggestion
                        lastDisplayedSuggestion = null
                        if (ignored != null &&
                            currentScreenFp == ignored.screenFp &&
                            actionDefId != ignored.targetDef.idActionDef
                        ) {
                            runCatching {
                                dao.recordImplicitDismiss(
                                    contextHash = ignored.candidate.key,
                                    targetActionDefId = ignored.targetDef.idActionDef,
                                    now = now,
                                    halfLifeMs = PredictionConfig.HALF_LIFE_MS,
                                    weight = PredictionConfig.IMPLICIT_DISMISS_WEIGHT,
                                    maxDecayedDismisses = PredictionConfig.MAX_DECAYED_DISMISSES,
                                )
                            }
                        }

                        val ctxKeys = buildContextKeys(pkg, currentScreenFp)
                        dao.recordHit(
                            contextHash = ctxKeys.key0,
                            contextOrder = 0,
                            packageName = pkg,
                            targetActionDefId = actionDefId,
                            now = now,
                            halfLifeMs = PredictionConfig.HALF_LIFE_MS,
                            maxDecayedHits = PredictionConfig.MAX_DECAYED_HITS,
                            hitDismissDecayFactor = PredictionConfig.HIT_DISMISS_DECAY_FACTOR,
                        )
                        dao.recordHit(
                            contextHash = ctxKeys.key1,
                            contextOrder = 1,
                            packageName = pkg,
                            targetActionDefId = actionDefId,
                            now = now,
                            halfLifeMs = PredictionConfig.HALF_LIFE_MS,
                            maxDecayedHits = PredictionConfig.MAX_DECAYED_HITS,
                            hitDismissDecayFactor = PredictionConfig.HIT_DISMISS_DECAY_FACTOR,
                        )
                        if (ctxKeys.key2 != null) {
                            dao.recordHit(
                                contextHash = ctxKeys.key2,
                                contextOrder = 2,
                                packageName = pkg,
                                targetActionDefId = actionDefId,
                                now = now,
                                halfLifeMs = PredictionConfig.HALF_LIFE_MS,
                                maxDecayedHits = PredictionConfig.MAX_DECAYED_HITS,
                                hitDismissDecayFactor = PredictionConfig.HIT_DISMISS_DECAY_FACTOR,
                            )
                        }

                        lastSettledScreenFp = currentScreenFp
                        lastShownPair = Pair(currentScreenFp, actionDefId)
                        Log.d(
                            TAG,
                            "Recorded UI_CLICK for screen $currentScreenFp: actionDefId=$actionDefId ($actionHash)"
                        )

                        if (++recordedCount % PRUNE_EVERY_N_RECORDS == 0) {
                            dao.pruneStale(now - PredictionConfig.STALE_RETENTION_MS)
                            dao.pruneGlobal(PredictionConfig.GLOBAL_MAX_ROWS)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error recording observation", e)
                    }
                }

                sessionPackage = pkg
                if (isContextAction(action)) {
                    buffer.add(actionHash)
                    while (buffer.size > 2) {
                        buffer.removeAt(0)
                    }
                }

                clearAction
            }

            applyOverlayAction(overlayAction)
            val settleDelay = if (isAsyncGestureCommand(action)) {
                PredictionConfig.GESTURE_SETTLE_DELAY_MS
            } else {
                PredictionConfig.DEBOUNCE_MS
            }
            scheduleScreenSettled(settleDelay)
        }
    }

    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!env.isVoiceServiceRunning()) return
        val type = event.eventType
        if (type == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            scheduleScreenSettled()
            return
        }

        val pkg = event.packageName?.toString() ?: return
        if (pkg == ownPackageName) return

        if (type == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            if (activeSuggestion != null) {
                scope.launch { clearSuggestionForTransientChange() }
            }
            scheduleScreenSettled()
            return
        }

        val isTransientScreenChange = type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            type == AccessibilityEvent.TYPE_VIEW_CLICKED

        if (isTransientScreenChange || type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (isTransientScreenChange && activeSuggestion != null) {
                scope.launch { clearSuggestionForTransientChange() }
            }
            scheduleScreenSettled()
        }
    }

    private fun scheduleScreenSettled(delayMs: Long = PredictionConfig.DEBOUNCE_MS) {
        synchronized(debounceLock) {
            val now = System.currentTimeMillis()
            if (debounceFirstEventTime == 0L) {
                debounceFirstEventTime = now
            }

            val elapsed = now - debounceFirstEventTime
            if (elapsed >= PredictionConfig.DEBOUNCE_MAX_WAIT_MS) {
                debounceFirstEventTime = 0L
                debounceJob?.cancel()
                debounceJob = null
                triggerOrQueueSettleLocked()
                return
            }

            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(delayMs.milliseconds)
                synchronized(debounceLock) {
                    debounceFirstEventTime = 0L
                    triggerOrQueueSettleLocked()
                }
            }
        }
    }

    private fun triggerOrQueueSettleLocked() {
        if (settleRunning) {
            settlePending = true
        } else {
            settleRunning = true
            launchSettleLoop()
        }
    }

    private fun launchSettleLoop() {
        scope.launch {
            var keepGoing = true
            while (keepGoing) {
                try {
                    onScreenSettled()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error during onScreenSettled", e)
                } finally {
                    keepGoing = synchronized(debounceLock) {
                        if (settlePending) {
                            settlePending = false
                            true
                        } else {
                            settleRunning = false
                            false
                        }
                    }
                }
            }
        }
    }

    suspend fun onScreenSettled() {
        val overlayAction: OverlayAction = stateMutex.withLock {
            if (!env.isVoiceServiceRunning() || env.isTextSessionActive() || env.isMacroPlaying()) {
                Log.d(
                    TAG,
                    "Skipping settle: voiceRunning=${env.isVoiceServiceRunning()}, textSession=${env.isTextSessionActive()}, macroPlaying=${env.isMacroPlaying()}"
                )
                currentKeys = null
                lastSettledScreenFp = null
                lastShownPair = null
                lastDisplayedSuggestion = null
                return@withLock clearSuggestionStateLocked()
            }

            val root = env.activeRoot() ?: return@withLock OverlayAction.None
            val pkg = root.packageName?.toString() ?: return@withLock OverlayAction.None
            if (pkg == ownPackageName) {
                currentKeys = null
                lastSettledScreenFp = null
                lastShownPair = null
                lastDisplayedSuggestion = null
                return@withLock clearSuggestionStateLocked()
            }

            val now = System.currentTimeMillis()
            if (now - lastCommandTime > PredictionConfig.SESSION_IDLE_MS && lastCommandTime > 0) {
                buffer.clear()
            }
            sessionPackage = pkg

            val screenFp = ScreenFingerprint.compute(root, env.screenWidth(), env.screenHeight())
            if (screenFp == ScreenFingerprint.UNKNOWN_SCREEN) {
                Log.d(TAG, "onScreenSettled: UNKNOWN_SCREEN for pkg=$pkg")
                currentKeys = null
                lastSettledScreenFp = null
                lastShownPair = null
                lastDisplayedSuggestion = null
                return@withLock clearSuggestionStateLocked()
            }

            var pendingClear: OverlayAction = OverlayAction.None
            if (screenFp != lastSettledScreenFp) {
                lastShownPair = null
                lastDisplayedSuggestion = null
                lastSettledScreenFp = screenFp
                pendingClear = clearSuggestionStateLocked()
            }

            val keys = buildContextKeys(pkg, screenFp)
            currentKeys = keys

            val candidate = engine.predict(keys.key0, keys.key1, keys.key2, now)
                ?: return@withLock clearOrKeep(pendingClear)

            if (lastShownPair == Pair(screenFp, candidate.targetId)) {
                val current = activeSuggestion ?: return@withLock pendingClear
                val refreshedMatches = PredictionNodeMatcher.findMatchingNodes(root, current.targetDef)
                if (refreshedMatches.size != 1) {
                    Log.d(
                        TAG,
                        "Active suggestion target gone or ambiguous (count=${refreshedMatches.size})"
                    )
                    return@withLock clearOrKeep(pendingClear)
                }

                val freshBounds = Rect()
                refreshedMatches[0].getBoundsInScreen(freshBounds)
                if (freshBounds.right <= freshBounds.left ||
                    freshBounds.bottom <= freshBounds.top ||
                    !isBoundsOnScreen(freshBounds, root) ||
                    isOversizedBackdrop(freshBounds, root) ||
                    isOccludedByKeyboard(freshBounds)
                ) {
                    return@withLock clearOrKeep(pendingClear)
                }

                if (freshBounds.left != current.bounds.left ||
                    freshBounds.top != current.bounds.top ||
                    freshBounds.right != current.bounds.right ||
                    freshBounds.bottom != current.bounds.bottom
                ) {
                    activeSuggestion = current.copy(bounds = freshBounds)
                    val gen = ++overlayGeneration
                    return@withLock OverlayAction.Show(freshBounds, gen)
                }

                return@withLock pendingClear
            }

            val targetDef = dao.getActionDef(candidate.targetId)
                ?: return@withLock clearOrKeep(pendingClear)
            if (targetDef.actionType != ActionType.UI_CLICK) {
                return@withLock clearOrKeep(pendingClear)
            }

            val matchingNodes = PredictionNodeMatcher.findMatchingNodes(root, targetDef)
            if (matchingNodes.size != 1) {
                Log.d(
                    TAG,
                    "Prediction matching count=${matchingNodes.size} for ${targetDef.resourceId ?: targetDef.actionHash} (requires exactly 1)"
                )
                return@withLock clearOrKeep(pendingClear)
            }

            val targetNode = matchingNodes[0]
            val bounds = Rect()
            targetNode.getBoundsInScreen(bounds)
            if (bounds.right <= bounds.left || bounds.bottom <= bounds.top || !isBoundsOnScreen(bounds, root)) {
                return@withLock clearOrKeep(pendingClear)
            }
            if (isOversizedBackdrop(bounds, root)) {
                Log.d(
                    TAG,
                    "Ignoring oversized backdrop target $bounds for ${targetDef.resourceId ?: targetDef.actionHash}"
                )
                return@withLock clearOrKeep(pendingClear)
            }
            if (isOccludedByKeyboard(bounds)) {
                Log.d(TAG, "Ignoring target occluded by keyboard: $bounds")
                return@withLock clearOrKeep(pendingClear)
            }

            val suggestion = ActiveSuggestion(
                targetDef = targetDef,
                candidate = candidate,
                bounds = bounds,
                screenFp = screenFp,
                shownAt = now
            )

            activeSuggestion = suggestion
            lastDisplayedSuggestion = suggestion
            lastShownPair = Pair(screenFp, candidate.targetId)

            val gen = ++overlayGeneration
            suggestionTimeoutJob?.cancel()
            suggestionTimeoutJob = scope.launch {
                delay(PredictionConfig.SUGGESTION_TIMEOUT_MS.milliseconds)
                val timeoutAction = stateMutex.withLock {
                    val current = activeSuggestion
                    if (current != null &&
                        current.shownAt == suggestion.shownAt &&
                        current.candidate == suggestion.candidate
                    ) {
                        lastDisplayedSuggestion = null
                        runCatching {
                            dao.recordImplicitDismiss(
                                contextHash = current.candidate.key,
                                targetActionDefId = current.targetDef.idActionDef,
                                now = System.currentTimeMillis(),
                                halfLifeMs = PredictionConfig.HALF_LIFE_MS,
                                weight = PredictionConfig.IMPLICIT_DISMISS_WEIGHT,
                                maxDecayedDismisses = PredictionConfig.MAX_DECAYED_DISMISSES,
                            )
                        }
                        clearSuggestionStateLocked()
                    } else {
                        OverlayAction.None
                    }
                }
                applyOverlayAction(timeoutAction)
            }

            Log.i(
                TAG,
                "Showing suggestion: ${targetDef.resourceId ?: targetDef.actionHash} on screen $screenFp at $bounds"
            )
            OverlayAction.Show(bounds, gen)
        }

        applyOverlayAction(overlayAction)
    }

    fun getActiveSuggestion(): ActiveSuggestion? = activeSuggestion

    suspend fun onPredictionConfirmed(): Rect? {
        val (bounds, overlayAction) = stateMutex.withLock {
            val suggestion = activeSuggestion ?: return@withLock Pair(null, OverlayAction.None)
            lastDisplayedSuggestion = null

            if (!env.isVoiceServiceRunning() || env.isTextSessionActive() || env.isMacroPlaying()) {
                return@withLock Pair(null, clearSuggestionStateLocked())
            }

            val root = env.activeRoot() ?: return@withLock Pair(null, clearSuggestionStateLocked())
            val currentScreen = ScreenFingerprint.compute(root, env.screenWidth(), env.screenHeight())
            if (currentScreen != suggestion.screenFp) {
                Log.w(TAG, "Double-check failed: screen changed from ${suggestion.screenFp} to $currentScreen")
                return@withLock Pair(null, clearSuggestionStateLocked())
            }

            val matchingNodes = PredictionNodeMatcher.findMatchingNodes(root, suggestion.targetDef)
            if (matchingNodes.size != 1) {
                Log.w(TAG, "Confirm double-check failed: matching count=${matchingNodes.size}")
                return@withLock Pair(null, clearSuggestionStateLocked())
            }

            val freshBounds = Rect()
            matchingNodes[0].getBoundsInScreen(freshBounds)
            if (freshBounds.right <= freshBounds.left ||
                freshBounds.bottom <= freshBounds.top ||
                !isBoundsOnScreen(freshBounds, root) ||
                isOversizedBackdrop(freshBounds, root) ||
                isOccludedByKeyboard(freshBounds)
            ) {
                Log.w(TAG, "Confirm double-check failed: invalid or occluded bounds $freshBounds")
                return@withLock Pair(null, clearSuggestionStateLocked())
            }

            dao.clearSuppression(suggestion.candidate.key, suggestion.targetDef.idActionDef)

            val clearAction = clearSuggestionStateLocked()
            Pair(freshBounds, clearAction)
        }

        applyOverlayAction(overlayAction)
        return bounds
    }

    suspend fun onPredictionDismissed(): Boolean {
        val (dismissed, overlayAction) = stateMutex.withLock {
            val suggestion = activeSuggestion ?: return@withLock Pair(false, OverlayAction.None)
            lastDisplayedSuggestion = null
            val now = System.currentTimeMillis()

            val count = dao.recordDismiss(
                contextHash = suggestion.candidate.key,
                targetActionDefId = suggestion.targetDef.idActionDef,
                now = now,
                halfLifeMs = PredictionConfig.HALF_LIFE_MS,
                dismissWeight = PredictionConfig.DISMISS_WEIGHT,
                cooldownDurationsMs = PredictionConfig.DISMISS_COOLDOWNS_MS,
                maxDecayedDismisses = PredictionConfig.MAX_DECAYED_DISMISSES,
            )
            if (suggestion.candidate.order > 0) {
                dao.recordImplicitDismiss(
                    contextHash = "screen:${suggestion.screenFp}",
                    targetActionDefId = suggestion.targetDef.idActionDef,
                    now = now,
                    halfLifeMs = PredictionConfig.HALF_LIFE_MS,
                    weight = PredictionConfig.DISMISS_WEIGHT * 0.5,
                    maxDecayedDismisses = PredictionConfig.MAX_DECAYED_DISMISSES,
                )
            }

            Log.i(TAG, "Dismissed suggestion: count=$count, order=${suggestion.candidate.order}")
            Pair(true, clearSuggestionStateLocked())
        }

        applyOverlayAction(overlayAction)
        return dismissed
    }

    suspend fun clearSuggestion() {
        val overlayAction = stateMutex.withLock {
            clearSuggestionStateLocked()
        }
        applyOverlayAction(overlayAction)
    }

    fun onServiceDisconnected() {
        synchronized(debounceLock) {
            debounceJob?.cancel()
            debounceJob = null
            debounceFirstEventTime = 0L
            settleRunning = false
            settlePending = false
        }
        scope.launch(NonCancellable) {
            val clearAction = stateMutex.withLock {
                currentKeys = null
                lastSettledScreenFp = null
                lastShownPair = null
                lastDisplayedSuggestion = null
                clearSuggestionStateLocked()
            }
            applyOverlayAction(clearAction)
        }
    }

    suspend fun onHistoryCleared() {
        val clearAction = stateMutex.withLock {
            buffer.clear()
            sessionPackage = null
            lastCommandTime = 0L
            currentKeys = null
            lastSettledScreenFp = null
            lastShownPair = null
            lastDisplayedSuggestion = null
            clearSuggestionStateLocked()
        }
        applyOverlayAction(clearAction)
    }

    private suspend fun onVoiceServiceStopped() {
        synchronized(debounceLock) {
            debounceJob?.cancel()
            debounceJob = null
            debounceFirstEventTime = 0L
            settlePending = false
        }
        val clearAction = stateMutex.withLock {
            currentKeys = null
            lastSettledScreenFp = null
            lastShownPair = null
            lastDisplayedSuggestion = null
            clearSuggestionStateLocked()
        }
        applyOverlayAction(clearAction)
    }

    private suspend fun clearSuggestionForTransientChange() {
        val overlayAction = stateMutex.withLock {
            lastShownPair = null
            clearSuggestionStateLocked()
        }
        applyOverlayAction(overlayAction)
    }

    private fun isAsyncGestureCommand(action: ActionDescriptor): Boolean {
        if (action.type == ActionType.UI_CLICK) return true
        val cmd = action.commandType ?: return false
        return cmd.contains("Swipe") || cmd.contains("Pinch") || cmd.contains("LongTap") || cmd.contains("Tap")
    }

    private fun isContextAction(action: ActionDescriptor): Boolean =
        action.type == ActionType.UI_CLICK ||
            action.type == ActionType.BACK ||
            action.type == ActionType.HOME ||
            action.type == ActionType.LAST_APPS ||
            action.commandType == OpenAppCommand::class.java.name

    private fun buildContextKeys(pkg: String, screenFp: String): ContextKeys {
        val key0 = "screen:$screenFp"
        val t1 = buffer.lastOrNull() ?: "START:$pkg"
        val key1 = ActionHasher.sha256Hex("1|$pkg|$screenFp|$t1")
        val t2 = if (buffer.size >= 2) buffer[buffer.size - 2] else null
        val key2 = t2?.let { ActionHasher.sha256Hex("2|$pkg|$screenFp|$it|$t1") }
        return ContextKeys(pkg = pkg, screenFp = screenFp, key0 = key0, key1 = key1, key2 = key2)
    }

    private fun clearSuggestionStateLocked(): OverlayAction {
        suggestionTimeoutJob?.cancel()
        suggestionTimeoutJob = null
        if (activeSuggestion == null) return OverlayAction.None
        activeSuggestion = null
        val gen = ++overlayGeneration
        return OverlayAction.Clear(gen)
    }

    private fun clearOrKeep(pendingClear: OverlayAction): OverlayAction {
        val cleared = clearSuggestionStateLocked()
        return if (cleared !is OverlayAction.None) cleared else pendingClear
    }

    private fun isOversizedBackdrop(bounds: Rect, root: AccessibilityNodeInfo): Boolean {
        val winBounds = Rect()
        root.getBoundsInScreen(winBounds)
        val winW = (winBounds.right - winBounds.left).coerceAtLeast(0).toLong()
        val winH = (winBounds.bottom - winBounds.top).coerceAtLeast(0).toLong()
        val winArea = winW * winH
        if (winArea <= 0L) return false

        val nodeW = (bounds.right - bounds.left).coerceAtLeast(0).toLong()
        val nodeH = (bounds.bottom - bounds.top).coerceAtLeast(0).toLong()
        val nodeArea = nodeW * nodeH
        return nodeArea * 100L >= winArea * 80L
    }

    private fun isOccludedByKeyboard(bounds: Rect): Boolean {
        val keyboardTop = env.keyboardTop() ?: return false
        return keyboardTop > 0 && bounds.bottom > keyboardTop
    }

    private fun isBoundsOnScreen(bounds: Rect, root: AccessibilityNodeInfo): Boolean {
        val winBounds = Rect()
        root.getBoundsInScreen(winBounds)
        val maxX = maxOf(env.screenWidth(), winBounds.right)
        val maxY = maxOf(env.screenHeight(), winBounds.bottom)
        val centerX = (bounds.left + bounds.right) / 2
        val centerY = (bounds.top + bounds.bottom) / 2
        return centerX in 0 until maxX && centerY in 0 until maxY
    }

    private suspend fun applyOverlayAction(action: OverlayAction) {
        when (action) {
            OverlayAction.None -> Unit
            is OverlayAction.Show -> withContext(NonCancellable + Dispatchers.Main.immediate) {
                if (action.generation == overlayGeneration) {
                    overlayManager.highlight(action.bounds)
                }
            }
            is OverlayAction.Clear -> withContext(NonCancellable + Dispatchers.Main.immediate) {
                if (action.generation == overlayGeneration) {
                    overlayManager.clearHighlight()
                }
            }
        }
    }

    companion object {
        private const val TAG = "PredictionManager"
        private const val PRUNE_EVERY_N_RECORDS = 50
    }
}
