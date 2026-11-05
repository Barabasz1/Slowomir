package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.telecom.TelecomManager
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.net.toUri

/**
 * Designed to act as an accessibility overlay on top of the native system dialer.
 *
 * @param service The active [AccessibilityService] instance.
 * @param touchExecutor The [TouchExecutor] used to emulate tap gestures.
 */
class PhoneCallExecutor(
    private val service: AccessibilityService,
    private val touchExecutor: TouchExecutor = TouchExecutor(service)
) {

    /**
     * Answers an incoming phone call.
     *
     * @return `true` if an active incoming call was answered, `false` otherwise.
     */
    fun pickUp(): Boolean {
        val success = findAndInteractWithNode(
            textKeywords = listOf("odbierz", "odpowiedz", "answer"),
            idKeywords = listOf("answer", "accept"),
            isAnswer = true
        )
        if (success) {
            Log.i(TAG, "Call answered via Accessibility UI.")
        }
        else {
            Log.w(TAG, "Cannot pick up: No active call / answer button found.")
        }

        return success
    }

    /**
     * Declines an incoming phone call.
     *
     * First attempts to end/reject the call via TelecomManager, then falls back
     * to searching for and clicking/swiping a "Decline" UI element.
     *
     * @return `true` if an active incoming call was rejected, `false` otherwise.
     */
    fun decline(): Boolean {
        val success = findAndInteractWithNode(
            textKeywords = listOf("odrzuć", "odrzuć połączenie", "decline", "reject"),
            idKeywords = listOf("decline", "reject", "hangup", "end_call"),
            isDecline = true
        )
        if (success) {
            Log.i(TAG, "Call declined via Accessibility UI.")
        }
        else {
            Log.w(TAG, "Cannot decline: No active call / decline button found.")
        }

        return success
    }

    /**
     * Ends an active phone call.
     *
     * First attempts to end the call via TelecomManager, then falls back
     * to searching for and clicking an "End Call" UI element.
     *
     * @return `true` if an active call was disconnected, `false` otherwise.
     */
    fun end(): Boolean {
        val success = findAndInteractWithNode(
            textKeywords = listOf("rozłącz", "zakończ", "zakończ połączenie", "odłóż słuchawkę", "end call", "hang up", "disconnect"),
            idKeywords = listOf("end_call", "disconnect", "hangup")
        )
        if (success) {
            Log.i(TAG, "Call ended via Accessibility UI.")
            return true
        }

        Log.w(TAG, "Cannot end: No active call / end button found.")
        return false
    }

    /**
     * Toggles the speakerphone.
     *
     * Searches the active window and interactive windows for a speakerphone button
     * (matching Polish and English labels or resource IDs) and clicks or taps it.
     *
     * @return `true` if the speakerphone toggle button was found and clicked, `false` otherwise.
     */
    fun speakerToggle(): Boolean {
        val clicked = findAndInteractWithNode(
            textKeywords = listOf(
                "głośnik", "głośnomówiący", "tryb głośnomówiący", "przełącz na głośnik",
                "wyłącz głośnik", "słuchawka telefonu", "słuchawka", "speaker", "speakerphone", "earpiece"
            ),
            idKeywords = listOf("incall_audio_button", "audio_route", "speaker", "btn_speaker"),
            excludeKeywords = listOf("emoji", "emotik", "efekt", "effect")
        )
        if (clicked) {
            Log.i(TAG, "Speaker toggled via Accessibility UI.")
            return true
        }

        Log.w(TAG, "Failed to find speaker button on screen.")
        return false
    }

    /**
     * Initiates an outgoing phone call.
     *
     * @param phoneNumber Optional phone number or contact name to dial.
     * @return `true` if the call action was dispatched, `false` otherwise.
     */
    fun call(phoneNumber: String = ""): Boolean {
        return try {
            val telecomManager = service.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if ((telecomManager != null) && phoneNumber.isNotBlank()) {
                val uri = "tel:$phoneNumber".toUri()
                telecomManager.placeCall(uri, null)
                true
            } else {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = "tel:$phoneNumber".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                service.startActivity(intent)
                true
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException initiating call: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate phone call", e)
            false
        }
    }

    /**
     * Opens an SMS composition screen for a contact or number.
     *
     * @param phoneNumber Optional recipient phone number or contact name.
     * @return `true` if the SMS action was dispatched, `false` otherwise.
     */
    fun writeSMS(phoneNumber: String = ""): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "smsto:$phoneNumber".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            service.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open SMS composer", e)
            false
        }
    }

    /**
     * Searches accessible windows for a node matching the given keywords in its text,
     * content description, or view ID, and performs a click, tap, or swipe gesture.
     *
     * @param textKeywords Keywords to match in the node's text.
     * @param idKeywords Keywords to match in the node's view ID.
     * @param excludeKeywords Keywords to exclude from the search.
     * @param isAnswer Whether the interaction is for answering an incoming call.
     * @param isDecline Whether the interaction is for declining an incoming call.
     * @return `true` if the node was found and interacted with, `false` otherwise.
     */
    private fun findAndInteractWithNode(
        textKeywords: List<String>,
        idKeywords: List<String>,
        excludeKeywords: List<String> = emptyList(),
        isAnswer: Boolean = false,
        isDecline: Boolean = false
    ): Boolean {
        val rootNodes = collectRootNodes()
        for (root in rootNodes) {
            val target = findNode(root) { node ->
                val pkg = node.packageName?.toString().orEmpty().lowercase()
                val isRelevantPackage = pkg.contains("dialer") ||
                    pkg.contains("telecom") ||
                    pkg.contains("phone") ||
                    pkg.contains("incall") ||
                    pkg.contains("systemui")

                if (!isRelevantPackage) return@findNode false

                val text = node.text?.toString().orEmpty().lowercase()
                val desc = node.contentDescription?.toString().orEmpty().lowercase()
                val resId = node.viewIdResourceName.orEmpty().lowercase()

                val isExcluded = excludeKeywords.any {
                    text.contains(it) || desc.contains(it) || resId.contains(it)
                }
                if (isExcluded) return@findNode false

                textKeywords.any { text.contains(it) || desc.contains(it) } ||
                    idKeywords.any { resId.contains(it) }
            }

            if (target != null) {
                val pkg = target.packageName?.toString().orEmpty().lowercase()
                val bounds = Rect()
                target.getBoundsInScreen(bounds)

                // For Google Dialer incoming call UI: swipe gestures are required on the drag handle
                if (pkg.contains("dialer") && (isAnswer || isDecline) && !bounds.isEmpty) {
                    val cx = bounds.centerX().toFloat()
                    val cy = bounds.centerY().toFloat()
                    return if (isDecline) {
                        // Swipe down to decline on Google Dialer
                        touchExecutor.swipe(cx, cy, cx, cy + 400f)
                    } else {
                        // Swipe up to answer on Google Dialer
                        touchExecutor.swipe(cx, cy, cx, cy - 400f)
                    }
                }

                val success = performClickOrTap(target)
                if (success) return true
            }
        }
        return false
    }

    /**
     * Collects all interactive windows from the service.
     *
     * @return List of [AccessibilityNodeInfo] representing the interactive windows.
     */
    private fun collectRootNodes(): List<AccessibilityNodeInfo> {
        val roots = mutableListOf<AccessibilityNodeInfo>()
        service.rootInActiveWindow?.let { roots.add(it) }
        try {
            service.windows?.forEach { window ->
                window.root?.let { node ->
                    if (roots.none { it == node }) {
                        roots.add(node)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not retrieve interactive windows", e)
        }
        return roots
    }

    /**
     * Finds a node in the given root that matches the given predicate.
     *
     * @param root The root to search in.
     * @param predicate A function that takes an [AccessibilityNodeInfo] and returns a boolean.
     * @return The first node that matches the predicate, or null if no match is found.
     */
    private fun findNode(
        root: AccessibilityNodeInfo?,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (root == null) return null
        if (predicate(root)) return root

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val match = findNode(child, predicate)
            if (match != null) return match
        }
        return null
    }

    /**
     * Performs a click or tap gesture on the given node.
     *
     * @param node The node to interact with.
     * @return `true` if the interaction was successful, `false` otherwise.
     */
    private fun performClickOrTap(node: AccessibilityNodeInfo): Boolean {
        // 1. Try performAction(ACTION_CLICK) on node or its clickable parents
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            current = current.parent
        }

        // 2. Fallback to tap gesture at bounds
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        return !bounds.isEmpty && bounds.centerX() > 0 && bounds.centerY() > 0
                && touchExecutor.tap(bounds.centerX().toFloat(), bounds.centerY().toFloat())
    }

    companion object {
        private const val TAG = "PhoneCallExecutor"
    }
}
