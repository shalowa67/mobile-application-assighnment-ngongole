package com.ngongole.app.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngongole.app.data.CustomerRepository
import com.ngongole.app.data.local.Customer
import com.ngongole.app.data.local.EntryType
import com.ngongole.app.data.local.LedgerEntry
import com.ngongole.app.domain.Money
import com.ngongole.app.domain.Validators
import com.ngongole.app.ui.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the details screen is showing. */
sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data object NotFound : DetailsUiState
    data class Loaded(
        val customer: Customer,
        val entries: List<LedgerEntry>, // newest first
        val balanceNgwee: Long
    ) : DetailsUiState
}

/**
 * State for one customer's page, plus the "record payment / add credit" dialog.
 * The dialog's open state and typed text live in SavedStateHandle, so rotating
 * the phone while the dialog is open keeps it open with the same text.
 */
class CustomerDetailsViewModel(
    private val savedState: SavedStateHandle,
    private val repository: CustomerRepository
) : ViewModel() {

    // Navigation puts the {customerId} from the route into SavedStateHandle for us.
    private val customerId: Long = checkNotNull(savedState[Routes.ARG_CUSTOMER_ID])

    val uiState: StateFlow<DetailsUiState> = repository.observeCustomer(customerId)
        .map { data ->
            if (data == null) DetailsUiState.NotFound
            else DetailsUiState.Loaded(
                customer = data.customer,
                entries = data.entries.sortedByDescending { it.createdAt },
                balanceNgwee = data.balanceNgwee
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailsUiState.Loading)

    // ----- Payment / credit dialog -----

    /** Which dialog is open: CREDIT, PAYMENT, or null when closed. Stored as text. */
    val dialogType: StateFlow<String?> = savedState.getStateFlow(KEY_DIALOG, null)
    val dialogAmount: StateFlow<String> = savedState.getStateFlow(KEY_AMOUNT, "")
    val dialogNote: StateFlow<String> = savedState.getStateFlow(KEY_NOTE, "")

    private val _dialogError = MutableStateFlow<String?>(null)
    val dialogError: StateFlow<String?> = _dialogError.asStateFlow()

    val showDeleteConfirm: StateFlow<Boolean> = savedState.getStateFlow(KEY_DELETE, false)

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    fun openDialog(type: EntryType) {
        savedState[KEY_AMOUNT] = ""
        savedState[KEY_NOTE] = ""
        _dialogError.value = null
        savedState[KEY_DIALOG] = type.name
    }

    fun closeDialog() {
        savedState[KEY_DIALOG] = null
    }

    fun onAmountChange(value: String) {
        savedState[KEY_AMOUNT] = value
        _dialogError.value = null
    }

    fun onNoteChange(value: String) {
        savedState[KEY_NOTE] = value
    }

    fun saveEntry() {
        val type = dialogType.value?.let { EntryType.valueOf(it) } ?: return
        val loaded = uiState.value as? DetailsUiState.Loaded ?: return

        // A customer cannot pay back more than they owe.
        val error = if (type == EntryType.PAYMENT) {
            if (loaded.balanceNgwee <= 0) "This customer does not owe anything"
            else Validators.validateAmount(
                dialogAmount.value,
                maxNgwee = loaded.balanceNgwee,
                maxMessage = "Payment cannot be more than the balance of ${Money.format(loaded.balanceNgwee)}"
            )
        } else {
            Validators.validateAmount(dialogAmount.value)
        } ?: Validators.validateNote(dialogNote.value)

        if (error != null) {
            _dialogError.value = error
            return
        }

        val amountNgwee = Money.parseToNgwee(dialogAmount.value)!!
        val note = dialogNote.value
        closeDialog()
        viewModelScope.launch {
            repository.addEntry(customerId, type, amountNgwee, note)
        }
    }

    // ----- Delete customer -----

    fun askDelete() { savedState[KEY_DELETE] = true }
    fun cancelDelete() { savedState[KEY_DELETE] = false }

    fun confirmDelete() {
        savedState[KEY_DELETE] = false
        viewModelScope.launch {
            repository.deleteCustomer(customerId)
            _deleted.value = true
        }
    }

    private companion object {
        const val KEY_DIALOG = "dialog_type"
        const val KEY_AMOUNT = "dialog_amount"
        const val KEY_NOTE = "dialog_note"
        const val KEY_DELETE = "delete_confirm"
    }
}
