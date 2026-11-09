package com.ngongole.app.ui.details

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

/**
 * Opens another app (SMS or WhatsApp) with the reminder already typed in.
 * Uses Android "intents", so Ngongole itself needs no SMS permission: the owner
 * still presses Send in the messaging app.
 *
 * Each function returns false if no app on the phone can handle it,
 * so the screen can show a message instead of crashing.
 */
object ReminderIntents {

    fun openSms(context: Context, phone: String, message: String): Boolean {
        val intent = Intent(Intent.ACTION_SENDTO, "smsto:$phone".toUri())
            .putExtra("sms_body", message)
        return start(context, intent)
    }

    fun openWhatsApp(context: Context, phone: String, message: String): Boolean {
        // wa.me links need the number without "+", e.g. 260971234567
        val digits = phone.removePrefix("+")
        val intent = Intent(Intent.ACTION_VIEW, "https://wa.me/$digits?text=${Uri.encode(message)}".toUri())
        return start(context, intent)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
