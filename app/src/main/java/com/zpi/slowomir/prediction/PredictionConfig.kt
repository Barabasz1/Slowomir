package com.zpi.slowomir.prediction

object PredictionConfig {
    const val WINDOW_M = 25
    const val MIN_SUPPORT = 2
    const val MIN_CONFIDENCE = 0.3
    const val MIN_WILSON = 0.60
    const val WILSON_Z = 1.96
    const val CONFLICT_MIN = 2
    const val GLOBAL_MAX_ROWS = 20_000
    const val DEBOUNCE_MS = 350L
    const val DEBOUNCE_MAX_WAIT_MS = 2_000L
    const val SUGGESTION_TIMEOUT_MS = 12_000L
    const val SESSION_IDLE_MS = 5 * 60_000L
    const val GUARD_FLAG_MAX_MS = 60_000L
    const val MIN_SCREEN_IDS = 1
    val DISMISS_COOLDOWNS_MS = longArrayOf(15 * 60_000L, 2 * 3_600_000L, 24 * 3_600_000L)
}
