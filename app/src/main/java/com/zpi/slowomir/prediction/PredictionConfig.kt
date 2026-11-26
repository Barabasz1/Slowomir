package com.zpi.slowomir.prediction

object PredictionConfig {
    const val HALF_LIFE_MS = 14L * 24 * 3_600_000L
    const val STALE_RETENTION_MS = 90L * 24 * 3_600_000L
    const val MAX_DECAYED_HITS = 15.0
    const val MAX_DECAYED_DISMISSES = 6.0
    const val HIT_DISMISS_DECAY_FACTOR = 0.7
    const val DISMISS_WEIGHT = 1.5
    const val IMPLICIT_DISMISS_WEIGHT = 0.25
    const val ACTIVE_WEIGHT_EPSILON = 0.05
    const val WILSON_Z = 1.0

    const val MIN_SUPPORT_ORDER_2 = 1.8
    const val MIN_WILSON_ORDER_2 = 0.38
    const val MIN_SUPPORT_ORDER_1 = 2.5
    const val MIN_WILSON_ORDER_1 = 0.44
    const val MIN_SUPPORT_ORDER_0 = 3.5
    const val MIN_WILSON_ORDER_0 = 0.50
    const val CONFLICT_WEIGHT = 1.5

    const val GLOBAL_MAX_ROWS = 20_000
    const val DEBOUNCE_MS = 350L
    const val GESTURE_SETTLE_DELAY_MS = 600L
    const val DEBOUNCE_MAX_WAIT_MS = 2_000L
    const val SUGGESTION_TIMEOUT_MS = 12_000L
    const val SESSION_IDLE_MS = 5 * 60_000L
    const val MIN_SCREEN_IDS = 1
    val DISMISS_COOLDOWNS_MS = longArrayOf(15 * 60_000L, 2 * 3_600_000L, 24 * 3_600_000L)
}
