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
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Provider

/**
 * A command that initiates a phone call to a given recipient.
 *
 * @param phoneCallExecutorProvider A provider for the [PhoneCallExecutor].
 * @param appContext The application context used as fallback when no active call is present.
 * @param recipient The phone number to call.
 */
class CallCommand @AssistedInject constructor(
    private val phoneCallExecutorProvider: Provider<PhoneCallExecutor?>,
    @ApplicationContext private val appContext: Context,
    @Assisted("recipient") private val recipient: String? = null,
) : Command {

    override fun execute(): CommandResult {
        val number = recipient?.takeIf { it.isNotBlank() }
            ?: return CommandResult.Failure("No contact was given.")

        val executor = phoneCallExecutorProvider.get()
        val success = executor?.call(number) ?: placeCall(appContext, number)

        return if (success) CommandResult.Success() else CommandResult.Failure("Phone call failed")
    }

    override fun canExecute(context: OverlayContext): Boolean = true

    /**
     * Fallback implementation to initiate a phone call.
     *
     * @param context The application context.
     * @param number The resolved phone number.
     * @return `true` if the phone call was successfully initiated, `false` otherwise.
     */
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