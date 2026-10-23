package com.zpi.slowomir.overlay.commands

sealed class CommandResult {
    data object Success : CommandResult()
    data class Failure(val reason: String) : CommandResult()
    data class InvalidContext(val reason: String) : CommandResult()
}