package com.ngongole.app.ui.customers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngongole.app.data.CustomerRepository
import com.ngongole.app.data.local.CustomerBalance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/** Everything the Customer List screen needs to draw itself. */
data class CustomerListUiState(
    val isLoading: Boolean = true,
    val customers: List<CustomerBalance> = emptyList(),
    val totalOwedNgwee: Long = 0,
    val search: String = ""
)

/**
 * Holds the list screen's state. A ViewModel is kept alive when the phone rotates,
 * so the list is not reloaded and the search text is not lost. The search text is
 * also stored in SavedStateHandle so it survives Android killing the app in the background.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CustomerListViewModel(
    private val savedState: SavedStateHandle,
    repository: CustomerRepository
) : ViewModel() {

    /** The search box text. The screen reads this directly so typing never lags behind the database. */
    val search: StateFlow<String> = savedState.getStateFlow(KEY_SEARCH, "")

    val uiState: StateFlow<CustomerListUiState> = combine(
        search,
        search.flatMapLatest { repository.observeCustomers(it) },
        repository.observeTotalOwed()
    ) { query, customers, total ->
        CustomerListUiState(isLoading = false, customers = customers, totalOwedNgwee = total, search = query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CustomerListUiState(search = search.value)
    )

    fun onSearchChange(text: String) {
        savedState[KEY_SEARCH] = text
    }

    private companion object {
        const val KEY_SEARCH = "search"
    }
}
