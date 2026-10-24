package com.zpi.slowomir.overlay.voice

import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier

data class VoiceIntent(
    val identifier: CommandIdentifier,
    val argument: CommandArgument? = null,
    val rawText: String = ""
)
