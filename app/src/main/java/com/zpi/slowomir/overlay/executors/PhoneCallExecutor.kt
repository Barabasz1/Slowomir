package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.net.toUri

/**
 * Executes phone call related operations using system services and global actions.
 *
 * @param service The active [AccessibilityService] instance.
 * @param touchExecutor An optional [TouchExecutor] retained for caller compatibility.
 */
class PhoneCallExecutor(
    private val service: AccessibilityService,
    @Suppress("UNUSED_PARAMETER") touchExecutor: TouchExecutor = TouchExecutor(service)
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
     * Toggles the speakerphone using system audio APIs.
     *
     * @param enable Optional explicit state to set. If null, the current state is inverted.
     * @return `true` if the speakerphone state was updated, `false` otherwise.
     */
    @Suppress("DEPRECATION")
    fun speakerToggle(enable: Boolean? = null): Boolean {
        return try {
            val audioManager = service.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false

            val targetState = enable ?: !audioManager.isSpeakerphoneOn

            if (targetState) {
                val speakerDevice = audioManager.availableCommunicationDevices
                    .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                if (speakerDevice != null) {
                    audioManager.setCommunicationDevice(speakerDevice)
                }
            } else {
                audioManager.clearCommunicationDevice()
            }

            audioManager.isSpeakerphoneOn = targetState
            Log.i(TAG, "Speaker updated to: $targetState")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle speakerphone", e)
            false
        }
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

    companion object {
        private const val TAG = "PhoneCallExecutor"
    }
}
