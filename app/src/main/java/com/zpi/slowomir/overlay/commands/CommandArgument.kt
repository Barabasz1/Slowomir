package com.zpi.slowomir.overlay.commands

sealed class CommandArgument {
    data class Number(val value: Int) : CommandArgument()
    data class TwoNumbers(val from: Int, val to: Int) : CommandArgument()
    data class Text(val value: String) : CommandArgument()
}
