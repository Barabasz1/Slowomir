package com.zpi.slowomir.overlay.ui.choice

import android.content.Context
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.zpi.slowomir.overlay.ui.VoiceOverlayView
import com.zpi.slowomir.overlay.ui.common.OverlayHostProvider
import com.zpi.slowomir.overlay.ui.common.OverlayLifecycleOwner
import com.zpi.slowomir.overlay.ui.common.OverlayWindowManager
import com.zpi.slowomir.ui.theme.SłowomirTheme
import javax.inject.Inject


class AppPickerOverlayManager @Inject constructor(
    hostProvider: OverlayHostProvider
) : OverlayWindowManager(hostProvider) {
    override val tag = "AppPickerOverlayManager"
    private val lifecycleOwner: LifecycleOwner? = null

    override fun onCreateView(context: Context): View {
        return ComposeView(context).apply {
            val lifecycle = OverlayLifecycleOwner().apply {
                onCreate()
                onStart()
            }
            lifecycleOwner = lifecycle

            setViewTreeLifecycleOwner(lifecycle)
            setViewTreeViewModelStoreOwner(lifecycle)
            setViewTreeSavedStateRegistryOwner(lifecycle)

            setContent {
                SłowomirTheme {
                }
            }
        }
    }

    override fun onCreateLayoutParams(windowType: Int): WindowManager.LayoutParams {
        TODO("Not yet implemented")
    }
}