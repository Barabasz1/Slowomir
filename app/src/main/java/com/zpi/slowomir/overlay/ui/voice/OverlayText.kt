package com.zpi.slowomir.overlay.ui.voice

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zpi.slowomir.R
import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.voice.OverlayError
import com.zpi.slowomir.voice.OverlayState
import com.zpi.slowomir.voice.VoiceIntent
import com.zpi.slowomir.voice.selection.SelectionFailure

@Composable
internal fun OverlayState.displayText(): String = when (this) {
    is OverlayState.Idle, is OverlayState.ListeningForKeyword -> ""
    is OverlayState.ListeningForCommand -> stringResource(R.string.overlay_listening)
    is OverlayState.ListeningToSpeech -> partialText.ifBlank { stringResource(R.string.overlay_listening) }
    is OverlayState.Dictating -> text
    is OverlayState.Editing -> text
    is OverlayState.ProcessingCommand -> text
    is OverlayState.CommandExecuted ->
        stringResource(R.string.overlay_command_executed, intent.describe() ?: commandName, commandName)
    is OverlayState.Error -> error.displayText()
}

@Composable
internal fun OverlayError.displayText(): String = when (this) {
    is OverlayError.InvalidCommand -> stringResource(R.string.overlay_error_invalid_command, text)
    is OverlayError.CommandFailed -> stringResource(R.string.overlay_error_command_failed, commandName, reason)
    is OverlayError.InvalidContext -> stringResource(R.string.overlay_error_invalid_context, reason)
    is OverlayError.SelectionFailed -> failure.displayText()
    is OverlayError.TextInsertFailed -> stringResource(R.string.overlay_error_text_insert)
}

@Composable
internal fun SelectionFailure.displayText(): String = when (this) {
    is SelectionFailure.Unsupported -> stringResource(R.string.overlay_error_selection_unsupported, identifier.toString())
    is SelectionFailure.ContactNotFound -> stringResource(R.string.overlay_error_contact_not_found, query)
    is SelectionFailure.ContactsPermissionDenied -> stringResource(R.string.overlay_error_contacts_permission_denied)
    is SelectionFailure.NoContactGiven -> stringResource(R.string.overlay_error_no_contact)
    is SelectionFailure.ApplicationNotFound -> stringResource(R.string.overlay_error_application_not_found, query)
    is SelectionFailure.NoApplicationGiven -> stringResource(R.string.overlay_error_no_application)
    is SelectionFailure.NoMacros -> stringResource(R.string.overlay_error_no_macros)
}

internal fun VoiceIntent?.describe(): String? = this?.let {
    val argument = when (val arg = it.argument) {
        is CommandArgument.Number -> " [${arg.value}]"
        is CommandArgument.TwoNumbers -> " [${arg.from} -> ${arg.to}]"
        is CommandArgument.Text -> " ['${arg.value}']"
        null -> ""
    }
    "${it.identifier}$argument"
}
