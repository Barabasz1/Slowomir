package com.zpi.slowomir.overlay.executors

import android.content.Context
import android.content.Intent
import android.telecom.CallAudioState
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.util.Log
import androidx.core.net.toUri
import com.zpi.slowomir.overlay.services.PhoneCallService
import com.zpi.slowomir.utils.ContactUtils

/**
 * Handles phone call operations using Android Telecom APIs and [PhoneCallService].
 *
 * @param service The active [PhoneCallService] instance.
 */
class PhoneCallExecutor(private val service: PhoneCallService) {

    /**
     * Answers an incoming phone call.
     *
     * @return `true` if an active incoming call was answered, `false` otherwise.
     */
    fun pickUp(): Boolean {
        val call = service.activeCall
        if (call == null) {
            Log.w(TAG, "Cannot pick up: No active call found.")
            return false
        }

        return try {
            call.answer(VideoProfile.STATE_AUDIO_ONLY)
            Log.i(TAG, "Call answered successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to answer call", e)
            false
        }
    }

    /**
     * Declines an incoming phone call.
     *
     * @return `true` if an active incoming call was rejected, `false` otherwise.
     */
    fun decline(): Boolean {
        val call = service.activeCall
        if (call == null) {
            Log.w(TAG, "Cannot decline: No active call found.")
            return false
        }

        return try {
            call.reject(false, null)
            Log.i(TAG, "Call declined successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decline call", e)
            false
        }
    }

    /**
     * Ends an active phone call.
     *
     * @return `true` if an active call was disconnected, `false` otherwise.
     */
    fun end(): Boolean {
        val call = service.activeCall
        if (call == null) {
            Log.w(TAG, "Cannot end: No active call found.")
            return false
        }

        return try {
            call.disconnect()
            Log.i(TAG, "Call disconnected successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect call", e)
            false
        }
    }

    /**
     * Toggles or turns on the speakerphone.
     *
     * @return `true` if the audio route was changed to speakerphone, `false` otherwise.
     */
    @Suppress("DEPRECATION")
    fun speakerOn(): Boolean {
        return try {
            service.setAudioRoute(CallAudioState.ROUTE_SPEAKER)
            Log.i(TAG, "Audio route changed to speakerphone.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to change audio route to speaker", e)
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
            val resolvedNumber = ContactUtils.resolvePhoneNumber(service, phoneNumber)
            val telecomManager = service.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if ((telecomManager != null) && resolvedNumber.isNotBlank()) {
                val uri = "tel:$resolvedNumber".toUri()
                telecomManager.placeCall(uri, null)
                true
            } else {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = "tel:$resolvedNumber".toUri()
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
            val resolvedNumber = ContactUtils.resolvePhoneNumber(service, phoneNumber)
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "smsto:$resolvedNumber".toUri()
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
