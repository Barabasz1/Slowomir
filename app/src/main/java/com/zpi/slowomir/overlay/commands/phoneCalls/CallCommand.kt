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
import com.zpi.slowomir.utils.ContactUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Provider

/**
 * A command that initiates an outgoing phone call to a given recipient.
 *
 * @param phoneCallExecutorProvider A provider for the [PhoneCallExecutor].
 * @param appContext The application context used as fallback when no active call is present.
 * @param recipient The optional recipient contact name or phone number.
 */
class CallCommand @AssistedInject constructor(
    private val phoneCallExecutorProvider: Provider<PhoneCallExecutor?>,
    @ApplicationContext private val appContext: Context,
    @Assisted("recipient") private val recipient: String? = null,
) : Command {

    override fun execute(): CommandResult {
        val targetRecipient = recipient.orEmpty()
        val executor = phoneCallExecutorProvider.get()

        val success = if (executor != null) {
            executor.call(targetRecipient)
        } else {
            fallbackCall(appContext, targetRecipient)
        }

        return if (success) CommandResult.Success() else CommandResult.Failure("Phone call failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    private fun fallbackCall(context: Context, recipient: String): Boolean {
        return try {
            val resolvedNumber = ContactUtils.resolvePhoneNumber(context, recipient)
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if ((telecomManager != null) && resolvedNumber.isNotBlank()) {
                val uri = "tel:$resolvedNumber".toUri()
                telecomManager.placeCall(uri, null)
                true
            } else {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = "tel:$resolvedNumber".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            }
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