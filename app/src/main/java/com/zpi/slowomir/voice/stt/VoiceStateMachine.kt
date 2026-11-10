package com.zpi.slowomir.voice.stt

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

            is VoiceState.IntentParsing -> when (event) {
                is InternalEvent.IntentParsed -> VoiceState.CommandExecuting(intent = event.intent, command = event.command)
                is InternalEvent.IntentFailed -> VoiceState.Listening()
                VoiceInput.EnableKwsMode -> VoiceState.KeywordSpotting
                else -> state
            }

            is VoiceState.CommandExecuting -> when (event) {
                is InternalEvent.CommandFinished -> {
                    if (event.returnToKws) VoiceState.KeywordSpotting
                    else VoiceState.Listening()
                }
                VoiceInput.EnableKwsMode -> VoiceState.KeywordSpotting
                else -> state
            }
        }
    }
}
