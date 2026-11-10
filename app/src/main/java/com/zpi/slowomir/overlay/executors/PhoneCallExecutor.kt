package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.telecom.TelecomManager
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.net.toUri
import com.zpi.slowomir.utils.AccessibilityUtils

/**
 * Executes phone call related operations using system services and Accessibility UI interaction.
 *
 * @param service The active [AccessibilityService] instance.
 * @param touchExecutor The [TouchExecutor] used to perform tap and click gestures.
 */
class PhoneCallExecutor(
    private val service: AccessibilityService,
    private val touchExecutor: TouchExecutor = TouchExecutor(service),
) {

    /**
     * Answers an incoming phone call using the system headset hook action.
     *
     * @return `true` if the headset hook action was dispatched, `false` otherwise.
     */
    fun pickUp(): Boolean {
        val success = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_KEYCODE_HEADSETHOOK)
        if (success) {
            Log.i(TAG, "Call answered via HEADSET_HOOK.")
        } else {
            Log.w(TAG, "Failed to perform HEADSET_HOOK action to answer call.")
        }
        return success
    }

    /**
     * Declines an incoming phone call using the TelecomManager system API.
     *
     * @return `true` if an active incoming call was rejected, `false` otherwise.
     */
    @Suppress("DEPRECATION")
    fun decline(): Boolean {
        return try {
            val telecomManager = service.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val success = telecomManager?.endCall() == true
            if (success) {
                Log.i(TAG, "Call declined via TelecomManager.endCall().")
            } else {
                Log.w(TAG, "Cannot decline: TelecomManager.endCall() returned false.")
            }
            success
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException declining call: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decline call", e)
            false
        }
    }

    /**
     * Ends an active phone call using the system headset hook action.
     *
     * @return `true` if the headset hook action was dispatched, `false` otherwise.
     */
    fun end(): Boolean {
        val success = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_KEYCODE_HEADSETHOOK)
        if (success) {
            Log.i(TAG, "Call ended via HEADSET_HOOK.")
        } else {
            Log.w(TAG, "Failed to perform HEADSET_HOOK action to end call.")
        }
        return success
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
                "wyłącz głośnik", "słuchawka telefonu", "słuchawka", "speaker", "speakerphone", "earpiece",
            ),
            idKeywords = listOf("incall_audio_button", "audio_route", "speaker", "btn_speaker"),
            excludeKeywords = listOf("emoji", "emotik", "efekt", "effect"),
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
     * content description, or view ID, and delegates the click or tap to [touchExecutor].
     *
     * @param textKeywords Keywords to match in the node's text.
     * @param idKeywords Keywords to match in the node's view ID.
     * @param excludeKeywords Keywords to exclude from the search.
     * @return `true` if the node was found and interacted with, `false` otherwise.
     */
    private fun findAndInteractWithNode(
        textKeywords: List<String>,
        idKeywords: List<String>,
        excludeKeywords: List<String> = emptyList(),
    ): Boolean {
        val rootNodes = collectRootNodes()

        // 1. Fast path: Native indexed text search across root windows
        for (root in rootNodes) {
            for (keyword in textKeywords) {
                val matches = try {
                    root.findAccessibilityNodeInfosByText(keyword).orEmpty()
                } catch (_: Exception) {
                    emptyList()
                }
                for (match in matches) {
                    val pkg = match.packageName?.toString().orEmpty().lowercase()
                    if (isRelevantPackage(pkg)) {
                        val text = match.text?.toString().orEmpty().lowercase()
                        val desc = match.contentDescription?.toString().orEmpty().lowercase()
                        val resId = match.viewIdResourceName.orEmpty().lowercase()
                        val isExcluded = excludeKeywords.any {
                            text.contains(it) || desc.contains(it) || resId.contains(it)
                        }
                        if (!isExcluded && touchExecutor.tapNode(match)) {
                            Log.i(TAG, "Fast-path interaction successful on node matching '$keyword'.")
                            return true
                        }
                    }
                }
            }
        }

        // 2. Comprehensive path: Full hierarchy search
        for (root in rootNodes) {
            val target = AccessibilityUtils.findNode(root) { node ->
                val pkg = node.packageName?.toString().orEmpty().lowercase()
                if (!isRelevantPackage(pkg)) return@findNode false

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

            if ((target != null) && touchExecutor.tapNode(target)) {
                Log.i(TAG, "Interacted with target node via TouchExecutor.click.")
                return true
            }
        }
        return false
    }

    private fun isRelevantPackage(pkg: String): Boolean {
        return pkg.contains("dialer") ||
            pkg.contains("telecom") ||
            pkg.contains("phone") ||
            pkg.contains("incall") ||
            pkg.contains("systemui")
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

    companion object {
        private const val TAG = "PhoneCallExecutor"
    }
}
