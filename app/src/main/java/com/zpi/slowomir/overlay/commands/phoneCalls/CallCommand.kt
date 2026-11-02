package com.zpi.slowomir.overlay.commands.phoneCalls

import android.content.Context
import android.content.Intent
import android.telecom.TelecomManager
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

class CallCommand @AssistedInject constructor(
    private val phoneCallExecutorProvider: Provider<PhoneCallExecutor?>,
    @ApplicationContext private val appContext: Context,
    @Assisted("recipient") private val recipient: String? = null,
) : Command {

    override fun execute(): CommandResult {
        val query = recipient.orEmpty()

        val number = when (val result = ContactUtils.lookup(appContext, query)) {
            is ContactLookupResult.Found -> result.number
            is ContactLookupResult.Ambiguous ->
                return CommandResult.NeedsSelection(result.options)
            ContactLookupResult.NotFound ->
                return CommandResult.Failure("Nie znaleziono kontaktu: $query")
            ContactLookupResult.PermissionDenied ->
                return CommandResult.Failure("Brak uprawnienia do odczytu kontaktów")
            ContactLookupResult.EmptyQuery ->
                return CommandResult.Failure("Nie podano, do kogo zadzwonić")
        }

        val executor = phoneCallExecutorProvider.get()
        /*
        val success = if (executor != null) {
            executor.call(number)
        } else {
            placeCall(appContext, number)
        }*/
        Log.i("CallCommand", "TEST: would call $number")   // <-- TEST
        val success = true

        return if (success) CommandResult.Success() else CommandResult.Failure("Phone call failed")
    }

    override fun canExecute(context: OverlayContext): Boolean = true

    private fun placeCall(context: Context, number: String): Boolean {
        return try {
            val uri = "tel:$number".toUri()
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (telecomManager != null) {
                telecomManager.placeCall(uri, null)
            } else {
                context.startActivity(
                    Intent(Intent.ACTION_CALL).apply {
                        data = uri
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }
            true
        } catch (e: SecurityException) {
            Log.e("CallCommand", "SecurityException initiating call: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e("CallCommand", "Failed to initiate phone call", e)
            false
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("recipient") recipient: String? = null,
        ): CallCommand
    }
}