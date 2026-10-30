package com.zpi.slowomir.overlay.services

import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.zpi.slowomir.overlay.executors.PhoneCallExecutor
import dagger.hilt.android.AndroidEntryPoint

/**
 * Dedicated InCallService responsible for handling incoming and active phone calls
 * via the Android Telecom framework.
 */
@AndroidEntryPoint
class PhoneCallService : InCallService() {

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

    companion object {
        private const val TAG = "PhoneCallService"

        var instance: PhoneCallService? = null
            private set
    }
}
