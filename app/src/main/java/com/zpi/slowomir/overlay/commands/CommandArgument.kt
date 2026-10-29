package com.zpi.slowomir.overlay.commands

/**
 * Represents the argument of a command. There are three types of arguments:
 * - [CommandArgument.Number]: Represents a single integer value.
 * - [CommandArgument.TwoNumbers]: Represents a pair of integer values.
 * - [CommandArgument.Text]: Represents a string value.
 */
sealed class CommandArgument {
    data class Number(val value: Int) : CommandArgument()
    data class TwoNumbers(val from: Int, val to: Int) : CommandArgument()
    data class Text(val value: String) : CommandArgument()
}
