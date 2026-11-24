package com.zpi.slowomir.prediction

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.zpi.slowomir.overlay.commands.predictions.ConfirmCommand
import com.zpi.slowomir.overlay.commands.predictions.DismissCommand
import com.zpi.slowomir.overlay.log.ActionDescriptor
import com.zpi.slowomir.overlay.log.ActionHasher
import com.zpi.slowomir.overlay.log.ActionType
import com.zpi.slowomir.overlay.log.db.ActionDefEntity
import com.zpi.slowomir.overlay.log.db.PredictionDao
import com.zpi.slowomir.overlay.log.db.PredictionObservationEntity
import com.zpi.slowomir.overlay.log.db.PredictionSuppressionEntity
import com.zpi.slowomir.overlay.macro.MacroPlayer
import com.zpi.slowomir.overlay.services.CommandExecutorService
import com.zpi.slowomir.overlay.ui.prediction.PredictionOverlayManager
import com.zpi.slowomir.voice.stt.VoiceState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

data class ContextKeys(
    val pkg: String,
    val screenFp: String,
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
    val isGuardedTarget: Boolean,
)

internal interface PredictionEnvironment {
    fun activeRoot(): AccessibilityNodeInfo?
    fun screenWidth(): Int
    fun screenHeight(): Int
    fun keyboardTop(): Int?
    fun isTextSessionActive(): Boolean
    fun isMacroPlaying(): Boolean
}

@Singleton
class DefaultPredictionEnvironment @Inject constructor(
    private val macroPlayer: MacroPlayer,
) : PredictionEnvironment {

    override fun activeRoot(): AccessibilityNodeInfo? =
        CommandExecutorService.instance?.rootInActiveWindow

    override fun screenWidth(): Int =
        CommandExecutorService.instance?.resources?.displayMetrics?.widthPixels ?: 1080

    override fun screenHeight(): Int =
        CommandExecutorService.instance?.resources?.displayMetrics?.heightPixels ?: 2400

    override fun keyboardTop(): Int? =
        CommandExecutorService.instance?.focusLayout()?.keyboardTop

    override fun isTextSessionActive(): Boolean {
        val state = CommandExecutorService.instance?.voiceSessionManager?.state?.value
        return state is VoiceState.Dictating ||
            state is VoiceState.Editing ||
            state is VoiceState.TextProcessing
    }

    override fun isMacroPlaying(): Boolean = macroPlayer.isPlaying
}

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class PredictionManager internal constructor(
    private val ownPackageName: String,
    private val dao: PredictionDao,
    @Suppress("unused") private val engine: VariableOrderMarkovEngine,
    private val overlayManager: PredictionOverlayManager,
    private val env: PredictionEnvironment,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        dao: PredictionDao,
        engine: VariableOrderMarkovEngine,
        overlayManager: PredictionOverlayManager,
        env: DefaultPredictionEnvironment,
    ) : this(context.packageName, dao, engine, overlayManager, env)

    private sealed interface OverlayAction {
        data object None : OverlayAction
        data class Show(val bounds: Rect, val generation: Long) : OverlayAction
        data class Clear(val generation: Long) : OverlayAction
    }

    private val workerDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + workerDispatcher)
    private val stateMutex = Mutex()
    private val debounceLock = Any()

    // In-memory prediction state (guarded by stateMutex unless @Volatile)
    private val buffer = mutableListOf<String>()
    private var sessionPackage: String? = null
    private var lastCommandTime: Long = 0L
    private var currentKeys: ContextKeys? = null
    private var lastSettledScreenFp: String? = null
    private val lastActionPerScreen = mutableMapOf<String, Long>()
    private var recordedCount: Int = 0

    @Volatile
    private var activeSuggestion: ActiveSuggestion? = null
    private var lastShownPair: Pair<String, Long>? = null

    @Volatile
    private var overlayGeneration: Long = 0L

    private var debounceJob: Job? = null
    private var debounceFirstEventTime: Long = 0L
    private var settleRunning: Boolean = false
    private var suggestionTimeoutJob: Job? = null

    fun captureScreenSnapshot(action: ActionDescriptor): ScreenSnapshot? {
        if (env.isMacroPlaying()) return null
        val root = env.activeRoot() ?: return null
        val rootPkg = root.packageName?.toString()
        if (action.packageName != null && rootPkg != action.packageName) {
            Log.d(TAG, "captureScreenSnapshot skipped: pkg mismatch root=$rootPkg action=${action.packageName}")
            return null
        }

        val screenFp = ScreenFingerprint.compute(root, env.screenWidth(), env.screenHeight())
        if (screenFp == ScreenFingerprint.UNKNOWN_SCREEN) {
            Log.d(TAG, "captureScreenSnapshot skipped: UNKNOWN_SCREEN for pkg=$rootPkg")
            return null
        }

        val isGuardedPkg = PredictionGuard.isGuardedPackage(action.packageName ?: rootPkg)
        val isUnsafeClick = if (action.type == ActionType.UI_CLICK) {
            val tempDef = ActionDefEntity(
                actionHash = ActionHasher.hash(action),
                actionType = action.type,
                packageName = action.packageName,
                resourceId = action.resourceId,
                hierarchyPath = action.hierarchyPath,
                labelHash = action.labelHash,
            )
            PredictionNodeMatcher.findMatchingNodes(root, tempDef).isEmpty().also { empty ->
                if (empty) {
                    Log.d(
                        TAG,
                        "captureScreenSnapshot: click target not re-matched in root (guarded): res=${action.resourceId} path=${action.hierarchyPath}"
                    )
                }
            }
        } else {
            false
        }

        return ScreenSnapshot(
            screenFp = screenFp,
            isGuardedTarget = isGuardedPkg || isUnsafeClick,
        )
    }

    fun onActionRecorded(
        action: ActionDescriptor,
        actionHash: String,
        actionDefId: Long,
        screenSnapshot: ScreenSnapshot? = null,
    ) {
        if (action.commandType == ConfirmCommand::class.java.name ||
            action.commandType == DismissCommand::class.java.name ||
            env.isMacroPlaying()
        ) {
            return
        }

        scope.launch {
            if (env.isMacroPlaying()) return@launch

            val overlayAction: OverlayAction = stateMutex.withLock {
                val now = System.currentTimeMillis()
                lastCommandTime = now
                val clearAction = clearSuggestionStateLocked()

                val keys = currentKeys
                val isGuarded = PredictionGuard.isGuardedPackage(action.packageName) ||
                    screenSnapshot?.isGuardedTarget == true

                val currentScreenFp = screenSnapshot?.screenFp
                    ?: keys?.takeIf { it.pkg == action.packageName }?.screenFp

                val pkg = keys?.takeIf { it.pkg == action.packageName }?.pkg
                    ?: action.packageName
                    ?: sessionPackage
                    ?: "unknown"

                if (!isGuarded) {
                    try {
                        if (action.type == ActionType.UI_CLICK && currentScreenFp != null) {
                            lastActionPerScreen[currentScreenFp] = actionDefId
                            lastSettledScreenFp = currentScreenFp
                            lastShownPair = Pair("screen:$currentScreenFp", actionDefId)
                            Log.d(
                                TAG,
                                "Recorded last UI_CLICK for screen $currentScreenFp: actionDefId=$actionDefId ($actionHash)"
                            )

                            dao.record(
                                PredictionObservationEntity(
                                    contextHash = "screen:$currentScreenFp",
                                    contextOrder = 0,
                                    packageName = pkg,
                                    targetActionDefId = actionDefId
                                ),
                                PredictionConfig.WINDOW_M
                            )

                            dao.upsertSuppression(
                                PredictionSuppressionEntity(
                                    contextHash = "screen:$currentScreenFp",
                                    targetActionDefId = actionDefId,
                                    consecutiveDismissCount = 0,
                                    cooldownUntilTimestamp = 0L
                                )
                            )
                        }

                        if (keys != null) {
                            dao.record(
                                PredictionObservationEntity(
                                    contextHash = keys.key1,
                                    contextOrder = 1,
                                    packageName = keys.pkg,
                                    targetActionDefId = actionDefId
                                ),
                                PredictionConfig.WINDOW_M
                            )

                            if (keys.key2 != null) {
                                dao.record(
                                    PredictionObservationEntity(
                                        contextHash = keys.key2,
                                        contextOrder = 2,
                                        packageName = keys.pkg,
                                        targetActionDefId = actionDefId
                                    ),
                                    PredictionConfig.WINDOW_M
                                )
                            }
                        }

                        if (++recordedCount % PRUNE_EVERY_N_RECORDS == 0) {
                            dao.pruneGlobal(PredictionConfig.GLOBAL_MAX_ROWS)
                            dao.pruneExpiredSuppressions(now)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error recording observation", e)
                    }
                }

                sessionPackage = pkg
                buffer.add(actionHash)
                while (buffer.size > 2) {
                    buffer.removeAt(0)
                }

                clearAction
            }

            applyOverlayAction(overlayAction)
            scheduleScreenSettled()
        }
    }

    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val type = event.eventType
        if (type == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            scheduleScreenSettled()
            return
        }

        val pkg = event.packageName?.toString() ?: return
        if (pkg == ownPackageName) return

        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            scheduleScreenSettled()
        }
    }

    private fun scheduleScreenSettled() {
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
                if (!settleRunning) {
                    settleRunning = true
                    scope.launch {
                        try {
                            onScreenSettled()
                        } finally {
                            synchronized(debounceLock) {
                                settleRunning = false
                            }
                        }
                    }
                }
                return
            }

            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(PredictionConfig.DEBOUNCE_MS.milliseconds)
                val shouldRun = synchronized(debounceLock) {
                    debounceFirstEventTime = 0L
                    if (settleRunning) {
                        false
                    } else {
                        settleRunning = true
                        true
                    }
                }
                if (shouldRun) {
                    scope.launch {
                        try {
                            onScreenSettled()
                        } finally {
                            synchronized(debounceLock) {
                                settleRunning = false
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun onScreenSettled() {
        val overlayAction: OverlayAction = stateMutex.withLock {
            if (env.isTextSessionActive() || env.isMacroPlaying()) {
                Log.d(
                    TAG,
                    "Skipping settle: textSession=${env.isTextSessionActive()}, macroPlaying=${env.isMacroPlaying()}"
                )
                lastSettledScreenFp = null
                lastShownPair = null
                return@withLock clearSuggestionStateLocked()
            }

            val root = env.activeRoot() ?: return@withLock OverlayAction.None
            val pkg = root.packageName?.toString() ?: return@withLock OverlayAction.None
            if (pkg == ownPackageName) {
                currentKeys = null
                lastSettledScreenFp = null
                lastShownPair = null
                return@withLock clearSuggestionStateLocked()
            }
            if (PredictionGuard.isGuardedPackage(pkg)) {
                currentKeys = null
                lastSettledScreenFp = null
                lastShownPair = null
                return@withLock clearSuggestionStateLocked()
            }

            val now = System.currentTimeMillis()
            if (pkg != sessionPackage || (now - lastCommandTime > PredictionConfig.SESSION_IDLE_MS && lastCommandTime > 0)) {
                buffer.clear()
                buffer.add("START:$pkg")
                sessionPackage = pkg
            }

            val screenFp = ScreenFingerprint.compute(root, env.screenWidth(), env.screenHeight())
            if (screenFp == ScreenFingerprint.UNKNOWN_SCREEN) {
                Log.d(TAG, "onScreenSettled: UNKNOWN_SCREEN for pkg=$pkg")
                currentKeys = null
                lastSettledScreenFp = null
                lastShownPair = null
                return@withLock clearSuggestionStateLocked()
            }

            var pendingClear: OverlayAction = OverlayAction.None
            if (screenFp != lastSettledScreenFp) {
                lastShownPair = null
                lastSettledScreenFp = screenFp
                pendingClear = clearSuggestionStateLocked()
            }

            val t1 = buffer.lastOrNull() ?: "START:$pkg"
            val key1 = ActionHasher.sha256Hex("1|$pkg|$screenFp|$t1")
            val t2 = if (buffer.size >= 2) buffer[buffer.size - 2] else null
            val key2 = if (t2 != null) ActionHasher.sha256Hex("2|$pkg|$screenFp|$t2|$t1") else null

            currentKeys = ContextKeys(pkg, screenFp, key1, key2)

            val targetId = lastActionPerScreen[screenFp]
                ?: dao.lastUiClickTarget("screen:$screenFp")
                ?: return@withLock clearOrKeep(pendingClear)

            val screenKey = "screen:$screenFp"
            val candidate = Candidate(targetId = targetId, key = screenKey, k = 1, n = 1)

            if (lastShownPair == Pair(candidate.key, candidate.targetId)) {
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

            val suppression = dao.getSuppression(candidate.key, candidate.targetId)
            if (suppression != null && now < suppression.cooldownUntilTimestamp) {
                Log.d(
                    TAG,
                    "Suggestion suppressed by cooldown until ${suppression.cooldownUntilTimestamp} for target=${candidate.targetId}"
                )
                return@withLock clearOrKeep(pendingClear)
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
            if (bounds.right <= bounds.left || bounds.bottom <= bounds.top) {
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
            lastShownPair = Pair(candidate.key, candidate.targetId)

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

            if (env.isTextSessionActive() || env.isMacroPlaying()) {
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
                isOversizedBackdrop(freshBounds, root) ||
                isOccludedByKeyboard(freshBounds)
            ) {
                Log.w(TAG, "Confirm double-check failed: invalid or occluded bounds $freshBounds")
                return@withLock Pair(null, clearSuggestionStateLocked())
            }

            dao.upsertSuppression(
                PredictionSuppressionEntity(
                    contextHash = suggestion.candidate.key,
                    targetActionDefId = suggestion.targetDef.idActionDef,
                    consecutiveDismissCount = 0,
                    cooldownUntilTimestamp = 0L
                )
            )

            val clearAction = clearSuggestionStateLocked()
            Pair(freshBounds, clearAction)
        }

        applyOverlayAction(overlayAction)
        return bounds
    }

    suspend fun onPredictionDismissed(): Boolean {
        val (dismissed, overlayAction) = stateMutex.withLock {
            val suggestion = activeSuggestion ?: return@withLock Pair(false, OverlayAction.None)
            val now = System.currentTimeMillis()

            val existing = dao.getSuppression(suggestion.candidate.key, suggestion.targetDef.idActionDef)
            val count = (existing?.consecutiveDismissCount ?: 0) + 1
            val cooldownIndex = min(count - 1, PredictionConfig.DISMISS_COOLDOWNS_MS.size - 1)
            val cooldownDuration = PredictionConfig.DISMISS_COOLDOWNS_MS[cooldownIndex]

            dao.upsertSuppression(
                PredictionSuppressionEntity(
                    contextHash = suggestion.candidate.key,
                    targetActionDefId = suggestion.targetDef.idActionDef,
                    consecutiveDismissCount = count,
                    cooldownUntilTimestamp = now + cooldownDuration
                )
            )

            Log.i(TAG, "Dismissed suggestion: count=$count, cooldown=${cooldownDuration / 1000}s")
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
        }
        scope.launch(NonCancellable) {
            val clearAction = stateMutex.withLock {
                currentKeys = null
                lastSettledScreenFp = null
                lastShownPair = null
                clearSuggestionStateLocked()
            }
            applyOverlayAction(clearAction)
        }
    }

    suspend fun onHistoryCleared() {
        val clearAction = stateMutex.withLock {
            lastActionPerScreen.clear()
            buffer.clear()
            sessionPackage = null
            lastCommandTime = 0L
            currentKeys = null
            lastSettledScreenFp = null
            lastShownPair = null
            clearSuggestionStateLocked()
        }
        applyOverlayAction(clearAction)
    }

    private fun clearSuggestionStateLocked(): OverlayAction {
        suggestionTimeoutJob?.cancel()
        suggestionTimeoutJob = null
        val hadSuggestion = activeSuggestion != null
        activeSuggestion = null
        val gen = ++overlayGeneration
        return if (hadSuggestion) OverlayAction.Clear(gen) else OverlayAction.None
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
