package com.zpi.slowomir.overlay.commands.phoneCalls

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.PhoneCallExecutor
import com.zpi.slowomir.utils.ContactLookupResult
import com.zpi.slowomir.utils.ContactUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Provider

/**
 * A command that opens or prepares an SMS message draft for a given recipient.
 *
 * @param phoneCallExecutorProvider A provider for the [PhoneCallExecutor].
 * @param appContext The application context used as fallback when no active call is present.
 * @param recipient The optional recipient contact name or phone number.
 */
class WriteSMSCommand @AssistedInject constructor(
    private val phoneCallExecutorProvider: Provider<PhoneCallExecutor?>,
    private val contactUtils: ContactUtils,
    @ApplicationContext private val appContext: Context,
    @Assisted("recipient") private val recipient: String? = null,
) : Command {

    override fun execute(): CommandResult {
        val query = recipient.orEmpty()

        val number = when (val result = contactUtils.lookup(appContext, query)) {
            is ContactLookupResult.Found -> result.number
            is ContactLookupResult.Ambiguous ->
                return CommandResult.NeedsSelection(result.options)
            ContactLookupResult.NotFound ->
                return CommandResult.Failure("Could not find a contact: $query")
            ContactLookupResult.PermissionDenied ->
                return CommandResult.Failure("No access to reading contacts")
            ContactLookupResult.EmptyQuery ->
                return CommandResult.Failure("No contact was given.")
        }

        val executor = phoneCallExecutorProvider.get()

        val success = executor?.writeSMS(number) ?: fallbackWriteSMS(appContext, number)

        return if (success) CommandResult.Success() else CommandResult.Failure("Write SMS failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    /**
     * Fallback implementation to open the SMS composer.
     *
     * @param context The application context.
     * @param resolvedNumber The resolved phone number.
     * @return `true` if the SMS composer was successfully opened, `false` otherwise.
     */
    private fun fallbackWriteSMS(context: Context, resolvedNumber: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "smsto:$resolvedNumber".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("WriteSMSCommand", "Failed to open SMS composer", e)
            false
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("recipient") recipient: String? = null,
        ): WriteSMSCommand
    }
}