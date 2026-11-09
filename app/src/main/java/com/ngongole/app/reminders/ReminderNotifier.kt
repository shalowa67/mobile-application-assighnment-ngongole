package com.ngongole.app.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ngongole.app.MainActivity
import com.ngongole.app.R
import com.ngongole.app.data.local.CustomerBalance
import com.ngongole.app.domain.Money

/** Builds and shows the "customers owe you" notification. */
object ReminderNotifier {

    private const val CHANNEL_ID = "payment_reminders"
    private const val NOTIFICATION_ID = 1001

    /** Android 8+ needs a channel before any notification can be shown. Safe to call many times. */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** True if we may post notifications (always true below Android 13). */
    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun showOverdueNotification(context: Context, overdue: List<CustomerBalance>) {
        // If the owner refused the permission we quietly do nothing instead of crashing.
        if (!hasPermission(context)) return

        val total = overdue.sumOf { it.balanceNgwee }
        val title = if (overdue.size == 1) "1 customer has owed you for a week"
        else "${overdue.size} customers have owed you for a week"
        val summary = "${Money.format(total)} unpaid for 7 days or more"

        val inbox = NotificationCompat.InboxStyle().setSummaryText(summary)
        overdue.take(5).forEach { inbox.addLine("${it.customer.name}: ${Money.format(it.balanceNgwee)}") }
        if (overdue.size > 5) inbox.addLine("and ${overdue.size - 5} more…")

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(inbox)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission was revoked between the check and now; nothing to do.
        }
    }
}
