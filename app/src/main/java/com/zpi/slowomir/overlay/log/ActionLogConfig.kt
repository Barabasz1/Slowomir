package com.zpi.slowomir.overlay.log

object ActionLogConfig {
    val isEnabled: Boolean = true
    const val RETENTION_DAYS = 90L
    const val PRUNE_INTERVAL_HOURS = 24L
    val clearOnStart: Boolean = false
    val logRowsToConsole: Boolean = true
    const val CONSOLE_TAG = "ActionLog"
}