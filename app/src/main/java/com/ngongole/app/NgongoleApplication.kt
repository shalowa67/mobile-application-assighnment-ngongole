package com.ngongole.app

import android.app.Application
import com.ngongole.app.reminders.ReminderNotifier
import com.ngongole.app.reminders.ReminderScheduler

/** Runs once when the app process starts, before any screen. */
class NgongoleApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderNotifier.createChannel(this)
        ReminderScheduler.scheduleDaily(this)
    }
}
