package com.ngongole.app.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ngongole.app.NgongoleApplication

/**
 * Background job run by WorkManager, even when the app is closed.
 * It finds customers who have owed money for 7+ days and shows one notification.
 */
class OverdueReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as NgongoleApplication).container.customerRepository
        return try {
            val overdue = repository.getOverdueCustomers()
            if (overdue.isNotEmpty()) {
                ReminderNotifier.showOverdueNotification(applicationContext, overdue)
            }
            Result.success()
        } catch (e: Exception) {
            // e.g. database busy: let WorkManager try again later instead of crashing
            Result.retry()
        }
    }
}
