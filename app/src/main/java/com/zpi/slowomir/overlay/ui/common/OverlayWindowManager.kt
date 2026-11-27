package com.zpi.slowomir.overlay.ui.common

import android.content.Context
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

abstract class OverlayWindowManager(
    private val hostProvider: OverlayHostProvider
) {
    protected abstract val tag: String

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var hostJob: Job? = null

    private var host: OverlayHost = hostProvider.host.value
    private var view: View? = null
    private var viewHost: OverlayHost? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var wanted = false

    val isShowing: Boolean get() = view != null

    protected val attachedView: View? get() = view

    protected fun updateLayout(change: WindowManager.LayoutParams.() -> Unit) {
        val current = view ?: return
        val params = layoutParams ?: return
        params.change()
        try {
            viewHost?.windowManager?.updateViewLayout(current, params)
        } catch (e: Exception) {
            Log.w(tag, "updateViewLayout failed", e)
        }
    }

    protected abstract fun onCreateView(context: Context): View

    protected abstract fun onCreateLayoutParams(windowType: Int): WindowManager.LayoutParams

    protected open fun onViewAttached(view: View) {}

    protected open fun onViewDetached(view: View) {}

    protected open fun onAttachDenied(host: OverlayHost) {}

    open fun show(): Boolean {
        wanted = true
        observeHost()
        return view != null || attach(host)
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

    private fun isMainThread(): Boolean {
        val mainLooper = Looper.getMainLooper()
        return mainLooper == null || Looper.myLooper() == mainLooper
    }

    private fun attach(target: OverlayHost): Boolean {
        // IDK
        if (!isMainThread()) {
            return runCatching {
                runBlocking(Dispatchers.Main.immediate) {
                    attach(target)
                }
            }.getOrDefault(false)
        }

        if (!target.isAccessibility && !Settings.canDrawOverlays(target.context)) {
            Log.w(tag, "Overlay permission not granted")
            onAttachDenied(target)
            return false
        }

        val newView = onCreateView(target.context)

        return try {
            val params = onCreateLayoutParams(target.windowType)
            target.windowManager.addView(newView, params)
            layoutParams = params
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
        // IDK
        if (!isMainThread()) {
            runCatching {
                runBlocking(Dispatchers.Main.immediate) {
                    detach()
                }
            }
            return
        }

        val old = view ?: return

        try {
            viewHost?.windowManager?.removeView(old)
        } catch (e: Exception) {
            Log.w(tag, "removeView failed", e)
        }

        view = null
        viewHost = null
        layoutParams = null
        onViewDetached(old)
    }

}
