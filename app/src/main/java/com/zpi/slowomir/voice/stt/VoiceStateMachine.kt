package com.zpi.slowomir.voice.stt

import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.commands.CommandResult

/** Commands that end a text input session. */
private val TEXT_SESSION_END = setOf(
    CommandIdentifier.TEXT_CONFIRM,
    CommandIdentifier.TEXT_CANCEL,
)

private fun TextMode.resume(): VoiceState = when (this) {
    TextMode.DICTATE -> VoiceState.Dictating()
    TextMode.EDIT -> VoiceState.Editing()
}

internal object VoiceStateMachine {

    fun reduce(state: VoiceState, event: SessionEvent): VoiceState {
        if (event is VoiceInput.ServiceStopped) {
            return event.error?.let { VoiceState.Failed(it) } ?: VoiceState.Stopped
        }

        return when (state) {
            VoiceState.Stopped, is VoiceState.Failed -> {
                if (event is VoiceInput.ServiceStarted) VoiceState.KeywordSpotting
                // TODO: Added only for testing the command input in the current version of HomeScreen (03.11.2026), remove afterwards
                else if (event is VoiceInput.SpeechFinal) {
                    if (event.text.isNotBlank()) VoiceState.IntentParsing(rawText = event.text)
                    else state
                }
                else state
            }

            VoiceState.KeywordSpotting -> when (event) {
                VoiceInput.KeywordDetected, VoiceInput.EnableSpeechRecognition -> VoiceState.Listening()
                else -> state
            }

            is VoiceState.Listening -> when (event) {
                is VoiceInput.SpeechPartial -> VoiceState.Listening(partialText = event.text)
                is VoiceInput.SpeechFinal -> {
                    if (event.text.isNotBlank()) VoiceState.IntentParsing(rawText = event.text)
                    else VoiceState.Listening()
                }
                VoiceInput.EnableKwsMode -> VoiceState.KeywordSpotting
                is InternalEvent.IntentParsed -> VoiceState.CommandExecuting(intent = event.intent, command = event.command)
                else -> state
            }

            is VoiceState.Dictating -> when (event) {
                is VoiceInput.SpeechPartial -> VoiceState.Dictating(partialText = event.text)
                is VoiceInput.SpeechFinal ->
                    if (event.text.isNotBlank()) VoiceState.TextProcessing(event.text, TextMode.DICTATE) else VoiceState.Dictating()
                VoiceInput.EnableKwsMode -> VoiceState.KeywordSpotting
                else -> state
            }

            is VoiceState.Editing -> when (event) {
                is VoiceInput.SpeechPartial -> VoiceState.Editing(partialText = event.text)
                is VoiceInput.SpeechFinal ->
                    if (event.text.isNotBlank()) VoiceState.TextProcessing(event.text, TextMode.EDIT) else VoiceState.Editing()
                VoiceInput.EnableKwsMode -> VoiceState.KeywordSpotting
                else -> state
            }

            is VoiceState.TextProcessing -> when (event) {
                InternalEvent.TextHandled -> state.mode.resume()
                is InternalEvent.IntentParsed ->
                    VoiceState.CommandExecuting(intent = event.intent, command = event.command, textMode = state.mode)
                VoiceInput.EnableKwsMode -> VoiceState.KeywordSpotting
                else -> state
            }

            is VoiceState.IntentParsing -> when (event) {
                is InternalEvent.IntentParsed -> VoiceState.CommandExecuting(intent = event.intent, command = event.command)
                is InternalEvent.IntentFailed -> VoiceState.Listening()
                VoiceInput.EnableKwsMode -> VoiceState.KeywordSpotting
                else -> state
            }

            is VoiceState.CommandExecuting -> when (event) {
                is InternalEvent.CommandFinished -> {
                    val id = state.intent.identifier
                    val succeeded = event.result is CommandResult.Success
                    when {
                        event.returnToKws -> VoiceState.KeywordSpotting
                        id in TEXT_SESSION_END -> VoiceState.Listening()
                        id == CommandIdentifier.TEXT_ENTER && succeeded -> VoiceState.Dictating()
                        id == CommandIdentifier.TEXT_EDIT && succeeded -> VoiceState.Editing()
                        // any other command spoken inside a session (cursor, delete, a failed switch): stay in it
                        else -> state.textMode?.resume() ?: VoiceState.Listening()
                    }
                }
                VoiceInput.EnableKwsMode -> VoiceState.KeywordSpotting
                else -> state
            }
        }
    }
}
