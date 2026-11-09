package com.ngongole.app

import android.content.Context
import com.ngongole.app.data.CustomerRepository
import com.ngongole.app.data.local.NgongoleDatabase

/**
 * Creates the objects the app shares (database, repository) once, so every
 * screen and the background worker use the same instances.
 */
class AppContainer(context: Context) {
    private val database = NgongoleDatabase.getInstance(context)
    val customerRepository = CustomerRepository(database.customerDao())
}
