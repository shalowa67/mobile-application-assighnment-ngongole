package com.ngongole.app.ui.addcustomer

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngongole.app.data.CustomerRepository
import com.ngongole.app.domain.Money
import com.ngongole.app.domain.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Error message for each field; null means the field is OK. */
data class AddCustomerErrors(
    val name: String? = null,
    val phone: String? = null,
    val amount: String? = null,
    val note: String? = null
) {
    val hasErrors get() = listOf(name, phone, amount, note).any { it != null }
}

/**
 * Holds what the user typed in the Add Customer form.
 *
 * Each field is stored in SavedStateHandle, so the typed text survives:
 *  - rotation (the ViewModel itself survives), and
 *  - Android killing the app while it is in the background (SavedStateHandle is restored).
 */
class AddCustomerViewModel(
    private val savedState: SavedStateHandle,
    private val repository: CustomerRepository
) : ViewModel() {

    val name: StateFlow<String> = savedState.getStateFlow(KEY_NAME, "")
    val phone: StateFlow<String> = savedState.getStateFlow(KEY_PHONE, "")
    val amount: StateFlow<String> = savedState.getStateFlow(KEY_AMOUNT, "")
    val note: StateFlow<String> = savedState.getStateFlow(KEY_NOTE, "")

    private val _errors = MutableStateFlow(AddCustomerErrors())
    val errors: StateFlow<AddCustomerErrors> = _errors.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** Becomes true once the customer is saved; the screen then closes. */
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    // Typing in a field clears that field's error message.
    fun onNameChange(value: String) { savedState[KEY_NAME] = value; _errors.update { it.copy(name = null) } }
    fun onPhoneChange(value: String) { savedState[KEY_PHONE] = value; _errors.update { it.copy(phone = null) } }
    fun onAmountChange(value: String) { savedState[KEY_AMOUNT] = value; _errors.update { it.copy(amount = null) } }
    fun onNoteChange(value: String) { savedState[KEY_NOTE] = value; _errors.update { it.copy(note = null) } }

    fun save() {
        if (_isSaving.value) return // ignore double taps

        val errors = AddCustomerErrors(
            name = Validators.validateName(name.value),
            phone = Validators.validatePhone(phone.value),
            amount = Validators.validateAmount(amount.value),
            note = Validators.validateNote(note.value)
        )
        _errors.value = errors
        if (errors.hasErrors) return

        // Safe to use !! because validation above already passed
        val normalisedPhone = Validators.normalisePhone(phone.value)!!
        val amountNgwee = Money.parseToNgwee(amount.value)!!

        viewModelScope.launch {
            _isSaving.value = true
            try {
                if (repository.phoneExists(normalisedPhone)) {
                    _errors.update { it.copy(phone = "A customer with this phone number already exists") }
                    return@launch
                }
                repository.addCustomer(name.value, normalisedPhone, amountNgwee, note.value)
                _saved.value = true
            } catch (e: SQLiteConstraintException) {
                // Backup check: the unique index on phone rejected a duplicate
                _errors.update { it.copy(phone = "A customer with this phone number already exists") }
            } finally {
                _isSaving.value = false
            }
        }
    }

    private companion object {
        const val KEY_NAME = "name"
        const val KEY_PHONE = "phone"
        const val KEY_AMOUNT = "amount"
        const val KEY_NOTE = "note"
    }
}
