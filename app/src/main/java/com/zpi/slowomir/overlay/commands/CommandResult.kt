package com.zpi.slowomir.overlay.commands

/**
 * Represents the outcome of executing a [Command].
 */
sealed class CommandResult {
    /**
     * Indicates successful command execution.
     *
     * @param message Optional message describing the result.
     */
    data class Success(val message: String? = null) : CommandResult() {
        override fun toString(): String = if (message != null) "Success: $message" else "Success"
    }

    /**
     * Indicates that command execution failed due to an error or unexpected condition.
     *
     * @param reason Description of why the execution failed.
     */
    data class Failure(val reason: String) : CommandResult() {
        override fun toString(): String = "Failure: $reason"
    }

    /**
     * Indicates that the command cannot be executed because the current [com.zpi.slowomir.overlay.OverlayContext] is invalid or unsuitable.
     *
     * @param reason Explanation of why the context is invalid for execution.
     */
    data class InvalidContext(val reason: String) : CommandResult() {
        override fun toString(): String = "Invalid Context: $reason"
    }
}