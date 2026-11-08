package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import javax.inject.Inject


class AppLaunchExecutor @Inject constructor(
    private val service: AccessibilityService,
) {

    private fun launchAppPackage(packageName: String): Boolean {
        val launchIntent =
            service.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            service.startActivity(launchIntent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package: $packageName", e)
            false
        }
    }

    companion object {
        const val TAG = "AppLaunchExecutor"
    }
}

