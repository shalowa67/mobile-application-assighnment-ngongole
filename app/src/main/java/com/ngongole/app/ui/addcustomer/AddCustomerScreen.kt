package com.ngongole.app.ui.addcustomer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ngongole.app.ui.AppViewModelProvider

/** Form to add a new customer together with the amount they took on credit. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(
    onDone: () -> Unit,
    viewModel: AddCustomerViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val phone by viewModel.phone.collectAsStateWithLifecycle()
    val amount by viewModel.amount.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    // When the ViewModel reports the customer was saved, go back to the list.
    LaunchedEffect(saved) { if (saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add customer") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FormField(
                value = name, onChange = viewModel::onNameChange,
                label = "Customer name *", error = errors.name,
                keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
            )
            FormField(
                value = phone, onChange = viewModel::onPhoneChange,
                label = "Phone number *", error = errors.phone,
                supporting = "e.g. 0977 123456",
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
            )
            FormField(
                value = amount, onChange = viewModel::onAmountChange,
                label = "Amount owed (K) *", error = errors.amount,
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
            )
            FormField(
                value = note, onChange = viewModel::onNoteChange,
                label = "Note (what they took)", error = errors.note,
                supporting = "Optional, e.g. 2 bags of mealie meal",
                keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                onDone = viewModel::save
            )
            Button(
                onClick = viewModel::save,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (isSaving) "Saving…" else "Save customer") }
        }
    }
}

/** A text field that shows its error message in red underneath when there is one. */
@Composable
private fun FormField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    error: String?,
    keyboard: KeyboardOptions,
    supporting: String? = null,
    onDone: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = keyboard,
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        modifier = Modifier.fillMaxWidth()
    )
}
