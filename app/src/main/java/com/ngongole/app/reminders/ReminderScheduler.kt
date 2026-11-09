package com.ngongole.app.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Tells WorkManager when to run [OverdueReminderWorker]. */
object ReminderScheduler {

    private const val DAILY_WORK_NAME = "daily_overdue_reminder"
    private const val REMINDER_HOUR = 8 // 08:00, before the shop gets busy

    /**
     * Schedules the check to run about once a day. "Unique" work with KEEP means
     * calling this every time the app starts does not create duplicate jobs.
     * WorkManager remembers the job even after the phone restarts.
     */
    fun scheduleDaily(context: Context) {
        val request = PeriodicWorkRequestBuilder<OverdueReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilNext(REMINDER_HOUR), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** Runs the check once, right now. Used by the "check reminders" button (handy for demos). */
    fun runNow(context: Context) {
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<OverdueReminderWorker>().build()
        )
    }

    private fun millisUntilNext(hour: Int): Long {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis - now.timeInMillis
    }
}
