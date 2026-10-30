package com.zpi.slowomir.overlay.services

import android.os.Build
import android.os.OutcomeReceiver
import android.telecom.Call
import android.telecom.CallEndpoint
import android.telecom.CallEndpointException
import android.telecom.InCallService
import android.util.Log
import androidx.annotation.RequiresApi
import com.zpi.slowomir.overlay.executors.PhoneCallExecutor
import dagger.hilt.android.AndroidEntryPoint

/**
 * Dedicated InCallService responsible for handling incoming and active phone calls
 * via the Android Telecom framework.
 */
@AndroidEntryPoint
class PhoneCallService : InCallService() {
    private var availableEndpoints = emptyList<CallEndpoint>()
    private var previousEndpoint: CallEndpoint? = null

    var activeCall: Call? = null
        private set

    var phoneCallExecutor: PhoneCallExecutor? = null
        private set

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.i(TAG, "New call added: $call")
        activeCall = call
        phoneCallExecutor = PhoneCallExecutor(this)
        instance = this

        call.registerCallback(
            object : Call.Callback() {
                override fun onStateChanged(call: Call, state: Int) {
                    super.onStateChanged(call, state)
                    Log.d(TAG, "Call state changed: $state")
                    if (state == Call.STATE_DISCONNECTED) {
                        if (activeCall == call) {
                            activeCall = null
                        }
                    }
                }
            },
        )
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.i(TAG, "Call removed: $call")
        if (activeCall == call) {
            activeCall = null
        }
        if (activeCall == null) {
            phoneCallExecutor = null
            instance = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activeCall = null
        phoneCallExecutor = null
        instance = null
    }

    override fun onAvailableCallEndpointsChanged(endpoints: List<CallEndpoint>) {
        super.onAvailableCallEndpointsChanged(endpoints)
        availableEndpoints = endpoints
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) //TOOD: consider upgrading to API 34 as the minimal version
    override fun onCallEndpointChanged(endpoint: CallEndpoint) {
        super.onCallEndpointChanged(endpoint)

        if (endpoint.endpointType != CallEndpoint.TYPE_SPEAKER) {
            previousEndpoint = endpoint
        }
    }

    fun toggleSpeaker(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return false
        }

        val currentEndpoint = currentCallEndpoint
        val target = if (currentEndpoint.endpointType == CallEndpoint.TYPE_SPEAKER) {
            availableEndpoints.firstOrNull {
                it == previousEndpoint
            } ?: availableEndpoints.firstOrNull {
                it.endpointType == CallEndpoint.TYPE_EARPIECE
            }
        } else {
            availableEndpoints.firstOrNull {
                it.endpointType == CallEndpoint.TYPE_SPEAKER
            }
        } ?: return false

        requestCallEndpointChange(
            target,
            mainExecutor,
            object : OutcomeReceiver<Void?, CallEndpointException> {
                override fun onResult(result: Void?) {
                    Log.d(TAG, "Audio output changed")
                }

                override fun onError(error: CallEndpointException) {
                    Log.d(TAG, "Could not change audio output")
                }
            }
        )

        return true
    }

    companion object {
        private const val TAG = "PhoneCallService"

        var instance: PhoneCallService? = null
            private set
    }
}
