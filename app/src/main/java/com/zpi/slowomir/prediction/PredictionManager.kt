package com.zpi.slowomir.prediction

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.zpi.slowomir.overlay.log.ActionDescriptor
import com.zpi.slowomir.overlay.log.ActionHasher
import com.zpi.slowomir.overlay.log.ActionType
import com.zpi.slowomir.overlay.log.HierarchyPath
import com.zpi.slowomir.overlay.log.UiNodeDescriber
import com.zpi.slowomir.overlay.log.db.ActionDefEntity
import com.zpi.slowomir.overlay.log.db.PredictionDao
import com.zpi.slowomir.overlay.log.db.PredictionObservationEntity
import com.zpi.slowomir.overlay.log.db.PredictionSuppressionEntity
import com.zpi.slowomir.overlay.services.CommandExecutorService
import com.zpi.slowomir.overlay.ui.prediction.PredictionOverlayManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

@Singleton
class PredictionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: PredictionDao,
    private val engine: VariableOrderMarkovEngine,
    private val overlayManager: PredictionOverlayManager,
    private val uiNodeDescriber: UiNodeDescriber,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val stateMutex = Mutex()

    // In-memory prediction state
    private val buffer = mutableListOf<String>()
    private var sessionPackage: String? = null
    private var lastCommandTime: Long = 0L
    private var currentKeys: ContextKeys? = null

    private var activeSuggestion: ActiveSuggestion? = null
    private var lastShownPair: Pair<String, Long>? = null

    private var suppressUntilNextCommand: Boolean = false
    private var guardSuppressionTime: Long = 0L

    private var debounceJob: Job? = null
    private var debounceFirstEventTime: Long = 0L
    private var suggestionTimeoutJob: Job? = null

    fun onActionRecorded(action: ActionDescriptor, actionHash: String, actionDefId: Long) {
        scope.launch {
            stateMutex.withLock {
                lastCommandTime = System.currentTimeMillis()
                clearSuggestionInternal()
                lastShownPair = null

                // Reset suppression flag on new user command
                if (suppressUntilNextCommand && System.currentTimeMillis() - guardSuppressionTime >= PredictionConfig.GUARD_FLAG_MAX_MS) {
                    suppressUntilNextCommand = false
                }

                val keys = currentKeys
                val isGuarded = PredictionGuard.isGuardedPackage(action.packageName)

                if (keys != null && !isGuarded && !suppressUntilNextCommand) {
                    try {
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
                    } catch (e: Exception) {
                        Log.e(TAG, "Error recording observation", e)
                    }
                }

                // Update buffer
                val pkg = action.packageName ?: keys?.pkg ?: sessionPackage ?: "unknown"
                sessionPackage = pkg
                buffer.add(actionHash)
                while (buffer.size > 2) {
                    buffer.removeAt(0)
                }

                scheduleScreenSettled()
            }
        }
    }

    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == context.packageName) return // Ignore own overlay events

        val type = event.eventType
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            scheduleScreenSettled()
        }
    }

    private fun scheduleScreenSettled() {
        val now = System.currentTimeMillis()
        if (debounceFirstEventTime == 0L) {
            debounceFirstEventTime = now
        }

        debounceJob?.cancel()
        debounceJob = scope.launch {
            val elapsed = now - debounceFirstEventTime
            val wait = if (elapsed >= PredictionConfig.DEBOUNCE_MAX_WAIT_MS) {
                0L
            } else {
                PredictionConfig.DEBOUNCE_MS
            }

            if (wait > 0) delay(wait.milliseconds)
            debounceFirstEventTime = 0L
            onScreenSettled()
        }
    }

    suspend fun onScreenSettled() {
        stateMutex.withLock {
            val service = CommandExecutorService.instance ?: return
            val root = service.rootInActiveWindow ?: return

            val pkg = root.packageName?.toString() ?: return
            if (pkg == context.packageName) return
            if (PredictionGuard.isGuardedPackage(pkg)) {
                currentKeys = null
                clearSuggestionInternal()
                return
            }

            val now = System.currentTimeMillis()
            // Session boundary check (app switch or idle timeout)
            if (pkg != sessionPackage || (now - lastCommandTime > PredictionConfig.SESSION_IDLE_MS && lastCommandTime > 0)) {
                buffer.clear()
                buffer.add("START:$pkg")
                sessionPackage = pkg
            }

            val displayMetrics = service.resources.displayMetrics
            val screenFp = ScreenFingerprint.compute(
                root,
                displayMetrics.widthPixels,
                displayMetrics.heightPixels
            )

            if (screenFp == ScreenFingerprint.UNKNOWN_SCREEN) {
                currentKeys = null
                clearSuggestionInternal()
                return
            }

            val t1 = buffer.lastOrNull() ?: "START:$pkg"
            val key1 = ActionHasher.sha256Hex("1|$pkg|$screenFp|$t1")
            val t2 = if (buffer.size >= 2) buffer[buffer.size - 2] else null
            val key2 = if (t2 != null) ActionHasher.sha256Hex("2|$pkg|$screenFp|$t2|$t1") else null

            currentKeys = ContextKeys(pkg, screenFp, key1, key2)

            if (suppressUntilNextCommand) return

            // Query predictor
            val candidate = engine.predict(key1, key2) ?: run {
                clearSuggestionInternal()
                return
            }

            // Check if already shown since last command
            if (lastShownPair == Pair(candidate.key, candidate.targetId)) {
                return
            }

            // Check cooldown suppression
            val suppression = dao.getSuppression(candidate.key, candidate.targetId)
            if (suppression != null && now < suppression.cooldownUntilTimestamp) {
                return
            }

            val targetDef = dao.getActionDef(candidate.targetId) ?: return
            if (targetDef.actionType != ActionType.UI_CLICK) return

            // Unambiguous target matcher in active window
            val matchingNodes = findMatchingNodes(root, targetDef)
            if (matchingNodes.size != 1) {
                Log.d(TAG, "Prediction matching count=${matchingNodes.size} (requires exactly 1)")
                clearSuggestionInternal()
                return
            }

            val targetNode = matchingNodes[0]
            val bounds = Rect()
            targetNode.getBoundsInScreen(bounds)

            val suggestion = ActiveSuggestion(
                targetDef = targetDef,
                candidate = candidate,
                bounds = bounds,
                screenFp = screenFp,
                shownAt = now
            )

            activeSuggestion = suggestion
            lastShownPair = Pair(candidate.key, candidate.targetId)

            Log.i(TAG, "Showing suggestion: ${targetDef.resourceId ?: targetDef.actionHash} (k=${candidate.k}, n=${candidate.n})")
            overlayManager.highlight(bounds)

            suggestionTimeoutJob?.cancel()
            suggestionTimeoutJob = scope.launch {
                delay(PredictionConfig.SUGGESTION_TIMEOUT_MS.milliseconds)
                stateMutex.withLock {
                    clearSuggestionInternal()
                }
            }
        }
    }

    private fun findMatchingNodes(root: AccessibilityNodeInfo, targetDef: ActionDefEntity): List<AccessibilityNodeInfo> {
        val matches = mutableListOf<AccessibilityNodeInfo>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        val targetHash = targetDef.actionHash
        val chain = ArrayList<Pair<AccessibilityNodeInfo, Int>>()

        while (queue.isNotEmpty() && matches.size < 2) {
            val node = queue.removeFirst()

            if (node.isVisibleToUser && !node.isPassword && !node.isEditable) {
                if (PredictionGuard.isGuardedNode(node)) {
                    // Skip guarded nodes
                } else {
                    val descriptor = uiNodeDescriber.describeTapAt(0f, 0f) // or custom node matching
                    val nodeHash = computeNodeHash(node)
                    if (nodeHash == targetHash) {
                        matches.add(node)
                    }
                }
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                if (child.isVisibleToUser) {
                    queue.add(child)
                }
            }
        }

        return matches
    }

    private fun computeNodeHash(node: AccessibilityNodeInfo): String {
        val isClick = true
        val pkg = node.packageName?.toString() ?: ""
        val resId = node.viewIdResourceName?.takeIf { it.isNotBlank() }
        val label = (node.contentDescription ?: node.text)?.toString()?.trim()
        val labelHash = label?.let { ActionHasher.sha256Hex(it.lowercase()) }

        // Build hierarchy segment
        val segments = mutableListOf<String>()
        var curr: AccessibilityNodeInfo? = node
        var depth = 0
        while (curr != null && depth < 8) {
            val parent = curr.parent
            val index = if (parent != null) {
                (0 until parent.childCount).firstOrNull { parent.getChild(it) == curr } ?: 0
            } else 0
            segments.add(0, HierarchyPath.segment(curr.className, index))
            curr = parent
            depth++
        }
        val path = HierarchyPath.format(segments)

        val descriptor = ActionDescriptor(
            type = ActionType.UI_CLICK,
            packageName = pkg,
            resourceId = resId,
            hierarchyPath = path,
            labelHash = labelHash
        )
        return ActionHasher.hash(descriptor)
    }

    fun getActiveSuggestion(): ActiveSuggestion? = activeSuggestion

    suspend fun onPredictionConfirmed(): Boolean {
        return stateMutex.withLock {
            val suggestion = activeSuggestion ?: return@withLock false
            val now = System.currentTimeMillis()

            // Double check: screen must be the same
            val service = CommandExecutorService.instance ?: return@withLock false
            val root = service.rootInActiveWindow ?: return@withLock false
            val currentScreen = ScreenFingerprint.compute(root)
            if (currentScreen != suggestion.screenFp) {
                Log.w(TAG, "Double-check failed: screen changed from ${suggestion.screenFp} to $currentScreen")
                clearSuggestionInternal()
                return@withLock false
            }

            // Reset dismiss count on success
            dao.upsertSuppression(
                PredictionSuppressionEntity(
                    contextHash = suggestion.candidate.key,
                    targetActionDefId = suggestion.targetDef.idActionDef,
                    consecutiveDismissCount = 0,
                    cooldownUntilTimestamp = 0L
                )
            )

            clearSuggestionInternal()
            true
        }
    }

    suspend fun onPredictionDismissed() {
        stateMutex.withLock {
            val suggestion = activeSuggestion ?: return@withLock
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
            clearSuggestionInternal()
        }
    }

    suspend fun clearSuggestion() {
        stateMutex.withLock {
            clearSuggestionInternal()
        }
    }

    private suspend fun clearSuggestionInternal() {
        suggestionTimeoutJob?.cancel()
        activeSuggestion = null
        overlayManager.clearHighlight()
    }

    companion object {
        private const val TAG = "PredictionManager"
    }
}
