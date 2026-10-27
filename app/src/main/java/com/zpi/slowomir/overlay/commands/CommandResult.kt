package com.zpi.slowomir.overlay.commands

sealed class CommandResult {
    data class Success(val message: String? = null) : CommandResult() {
        override fun toString(): String = if (message != null) "Success: $message" else "Success"
    }

    data class Failure(val reason: String) : CommandResult() {
        override fun toString(): String = "Failure: $reason"
    }

    data class InvalidContext(val reason: String) : CommandResult() {
        override fun toString(): String = "Invalid Context: $reason"
    }
}


