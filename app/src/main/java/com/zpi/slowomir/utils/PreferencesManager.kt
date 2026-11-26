package com.zpi.slowomir.utils

import android.content.Context
import com.zpi.slowomir.overlay.ui.grid.GridType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("slowomir_settings", Context.MODE_PRIVATE)

    private val _isOverlayEnabled = MutableStateFlow(prefs.getBoolean(KEY_OVERLAY_ENABLED, true))
    val isOverlayEnabled: StateFlow<Boolean> = _isOverlayEnabled.asStateFlow()

    private val _isOnboardingCompleted =
        MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _showIntroHighlight =
        MutableStateFlow(prefs.getBoolean(KEY_SHOW_INTRO_HIGHLIGHT, false))
    val showIntroHighlight: StateFlow<Boolean> = _showIntroHighlight.asStateFlow()

    private val _gridRows = MutableStateFlow(
        prefs.getInt(KEY_GRID_ROWS, DEFAULT_GRID_ROWS).coerceIn(GRID_MIN, GRID_MAX)
    )
    val gridRows: StateFlow<Int> = _gridRows.asStateFlow()

    private val _gridCols = MutableStateFlow(
        prefs.getInt(KEY_GRID_COLS, DEFAULT_GRID_COLS).coerceIn(GRID_MIN, GRID_MAX)
    )
    val gridCols: StateFlow<Int> = _gridCols.asStateFlow()

    private val _gridType = MutableStateFlow(GridType.fromId(prefs.getString(KEY_GRID_TYPE, null)))
    val gridType: StateFlow<GridType> = _gridType.asStateFlow()

    private val _invertedControls = MutableStateFlow(
        prefs.getBoolean(KEY_INVERTED_CONTROLS, INVERTED_CONTROLS)
    )
    val invertedControls: StateFlow<Boolean> = _invertedControls.asStateFlow()

    fun setGridRows(rows: Int) {
        val value = rows.coerceIn(GRID_MIN, GRID_MAX)
        _gridRows.value = value
        prefs.edit { putInt(KEY_GRID_ROWS, value) }
    }

    fun setGridCols(cols: Int) {
        val value = cols.coerceIn(GRID_MIN, GRID_MAX)
        _gridCols.value = value
        prefs.edit { putInt(KEY_GRID_COLS, value) }
    }

    fun setGridType(type: GridType) {
        _gridType.value = type
        prefs.edit { putString(KEY_GRID_TYPE, type.id) }
    }

    fun setInvertedControls(inverted: Boolean) {
        _invertedControls.value = inverted
        prefs.edit { putBoolean(KEY_INVERTED_CONTROLS, inverted) }
    }

    fun setOverlayEnabled(enabled: Boolean) {
        _isOverlayEnabled.value = enabled
        prefs.edit { putBoolean(KEY_OVERLAY_ENABLED, enabled) }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        _isOnboardingCompleted.value = completed
        prefs.edit { putBoolean(KEY_ONBOARDING_COMPLETED, completed) }
        if (completed) {
            setIntroHighlightShown(true)
        }
    }

    fun setIntroHighlightShown(show: Boolean) {
        _showIntroHighlight.value = show
        prefs.edit { putBoolean(KEY_SHOW_INTRO_HIGHLIGHT, show) }
    }

    companion object {
        const val INVERTED_CONTROLS = false;
        const val GRID_MIN = 1
        const val GRID_MAX = 20
        const val DEFAULT_GRID_ROWS = 8
        const val DEFAULT_GRID_COLS = 4

        private const val KEY_INVERTED_CONTROLS = "inverted_controls"
        private const val KEY_GRID_ROWS = "grid_rows"
        private const val KEY_GRID_COLS = "grid_cols"
        private const val KEY_GRID_TYPE = "grid_type"

        private const val KEY_OVERLAY_ENABLED = "is_overlay_enabled"
        private const val KEY_ONBOARDING_COMPLETED = "is_onboarding_completed"
        private const val KEY_SHOW_INTRO_HIGHLIGHT = "show_intro_highlight"
    }
}
