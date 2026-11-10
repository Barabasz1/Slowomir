package com.zpi.slowomir.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("slowomir_settings", Context.MODE_PRIVATE)

    private val _isOverlayEnabled = MutableStateFlow(prefs.getBoolean(KEY_OVERLAY_ENABLED, true))
    val isOverlayEnabled: StateFlow<Boolean> = _isOverlayEnabled.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _showIntroHighlight = MutableStateFlow(prefs.getBoolean(KEY_SHOW_INTRO_HIGHLIGHT, false))
    val showIntroHighlight: StateFlow<Boolean> = _showIntroHighlight.asStateFlow()

    fun setOverlayEnabled(enabled: Boolean) {
        _isOverlayEnabled.value = enabled
        prefs.edit().putBoolean(KEY_OVERLAY_ENABLED, enabled).apply()
    }

    fun setOnboardingCompleted(completed: Boolean) {
        _isOnboardingCompleted.value = completed
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
        if (completed) {
            setIntroHighlightShown(true)
        }
    }

    fun setIntroHighlightShown(show: Boolean) {
        _showIntroHighlight.value = show
        prefs.edit().putBoolean(KEY_SHOW_INTRO_HIGHLIGHT, show).apply()
    }

    companion object {
        private const val KEY_OVERLAY_ENABLED = "is_overlay_enabled"
        private const val KEY_ONBOARDING_COMPLETED = "is_onboarding_completed"
        private const val KEY_SHOW_INTRO_HIGHLIGHT = "show_intro_highlight"
    }
}
