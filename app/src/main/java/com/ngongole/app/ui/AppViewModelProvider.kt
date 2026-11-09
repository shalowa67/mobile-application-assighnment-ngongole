package com.ngongole.app.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ngongole.app.NgongoleApplication
import com.ngongole.app.ui.addcustomer.AddCustomerViewModel
import com.ngongole.app.ui.customers.CustomerListViewModel
import com.ngongole.app.ui.details.CustomerDetailsViewModel

/**
 * Tells Android how to build each ViewModel, handing it the repository and a
 * SavedStateHandle (which keeps small values even if Android kills the app in the background).
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { CustomerListViewModel(createSavedStateHandle(), repository()) }
        initializer { AddCustomerViewModel(createSavedStateHandle(), repository()) }
        initializer { CustomerDetailsViewModel(createSavedStateHandle(), repository()) }
    }

    private fun CreationExtras.repository() =
        (this[APPLICATION_KEY] as NgongoleApplication).container.customerRepository
}
