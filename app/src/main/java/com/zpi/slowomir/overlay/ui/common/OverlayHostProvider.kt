package com.zpi.slowomir.overlay.ui.common

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Singleton

class OverlayHost(val context: Context, val isAccessibility: Boolean) {
    val windowManager: WindowManager = context.getSystemService(WindowManager::class.java)
    val windowType: Int =
        if (isAccessibility) WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        else WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
}

@Singleton
class OverlayHostProvider @Inject constructor(
    @ApplicationContext context: Context
) {
    private val appHost = OverlayHost(context, isAccessibility = false)
    private val _host = MutableStateFlow(appHost)
    val host: StateFlow<OverlayHost> = _host.asStateFlow()

    fun useAccessibility(service: AccessibilityService) {
        _host.value = OverlayHost(service, isAccessibility = true)
    }

    fun useApplication() {
        _host.value = appHost
    }
}
