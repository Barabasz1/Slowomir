package com.zpi.slowomir.overlay.ui.common

import android.content.Context
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

abstract class OverlayWindowManager(
    private val hostProvider: OverlayHostProvider
) {
    protected abstract val tag: String

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var hostJob: Job? = null

    private var host: OverlayHost = hostProvider.host.value
    private var view: View? = null
    private var viewHost: OverlayHost? = null
    private var wanted = false

    val isShowing: Boolean get() = view != null

    protected abstract fun onCreateView(context: Context): View

    protected abstract fun onCreateLayoutParams(windowType: Int): WindowManager.LayoutParams

    protected open fun onViewAttached(view: View) {}

    protected open fun onViewDetached(view: View) {}

    protected open fun onAttachDenied(host: OverlayHost) {}

    fun show(): Boolean {
        wanted = true
        observeHost()
        if (view != null) {
            return true
        }
        return attach(host)
    }

    fun hide() {
        wanted = false
        detach()
    }

    open fun toggle(): Boolean {
        if (isShowing) {
            hide()
        } else {
            show()
        }
        return isShowing
    }

    private fun observeHost() {
        if (hostJob != null) return
        hostJob = scope.launch {
            hostProvider.host.collect { rehost(it) }
        }
    }

    private fun rehost(newHost: OverlayHost) {
        if (newHost === host) return
        host = newHost
        if (wanted && view != null) {
            detach()
            attach(newHost)
        }
    }

    private fun attach(target: OverlayHost): Boolean {
        if (!target.isAccessibility && !Settings.canDrawOverlays(target.context)) {
            Log.w(tag, "Overlay permission not granted")
            onAttachDenied(target)
            return false
        }

        val newView = onCreateView(target.context)

        return try {
            target.windowManager.addView(newView, onCreateLayoutParams(target.windowType))
            view = newView
            viewHost = target
            onViewAttached(newView)
            true
        } catch (e: Exception) {
            Log.e(tag, "addView failed", e)
            onViewDetached(newView)
            false
        }
    }

    private fun detach() {
        val old = view ?: return

        try {
            viewHost?.windowManager?.removeView(old)
        } catch (e: Exception) {
            Log.w(tag, "removeView failed", e)
        }

        view = null
        viewHost = null
        onViewDetached(old)
    }
}
