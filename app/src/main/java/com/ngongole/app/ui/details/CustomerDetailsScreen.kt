package com.ngongole.app.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ngongole.app.data.local.EntryType
import com.ngongole.app.data.local.LedgerEntry
import com.ngongole.app.domain.Money
import com.ngongole.app.domain.ReminderMessage
import com.ngongole.app.ui.AppViewModelProvider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One customer's balance, history and actions (record payment, add credit, remind). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(
    onBack: () -> Unit,
    viewModel: CustomerDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dialogType by viewModel.dialogType.collectAsStateWithLifecycle()
    val dialogAmount by viewModel.dialogAmount.collectAsStateWithLifecycle()
    val dialogNote by viewModel.dialogNote.collectAsStateWithLifecycle()
    val dialogError by viewModel.dialogError.collectAsStateWithLifecycle()
    val showDelete by viewModel.showDeleteConfirm.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(deleted) { if (deleted) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text((state as? DetailsUiState.Loaded)?.customer?.name ?: "Customer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state is DetailsUiState.Loaded) {
                        IconButton(onClick = viewModel::askDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete customer")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        when (val s = state) {
            DetailsUiState.Loading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            DetailsUiState.NotFound -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("This customer no longer exists.")
            }
            is DetailsUiState.Loaded -> {
                val message = ReminderMessage.build(s.customer.name, s.balanceNgwee)
                fun showError(text: String) { scope.launch { snackbar.showSnackbar(text) } }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        BalanceCard(s)
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { viewModel.openDialog(EntryType.PAYMENT) },
                                enabled = s.balanceNgwee > 0,
                                modifier = Modifier.weight(1f)
                            ) { Text("Record payment") }
                            OutlinedButton(
                                onClick = { viewModel.openDialog(EntryType.CREDIT) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Add credit") }
                        }
                    }
                    if (s.balanceNgwee > 0) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = {
                                        if (!ReminderIntents.openSms(context, s.customer.phone, message)) {
                                            showError("No SMS app found on this phone")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Remind by SMS") }
                                OutlinedButton(
                                    onClick = {
                                        if (!ReminderIntents.openWhatsApp(context, s.customer.phone, message)) {
                                            showError("WhatsApp or a web browser is needed for this")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("WhatsApp") }
                            }
                        }
                    }
                    item {
                        Text(
                            "History",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    if (s.entries.isEmpty()) {
                        item { Text("No entries yet.") }
                    }
                    items(s.entries, key = { it.id }) { entry ->
                        EntryRow(entry)
                        HorizontalDivider()
                    }
                }

                dialogType?.let { typeName ->
                    EntryDialog(
                        type = EntryType.valueOf(typeName),
                        amount = dialogAmount,
                        note = dialogNote,
                        error = dialogError,
                        onAmountChange = viewModel::onAmountChange,
                        onNoteChange = viewModel::onNoteChange,
                        onSave = viewModel::saveEntry,
                        onDismiss = viewModel::closeDialog
                    )
                }
                if (showDelete) {
                    DeleteCustomerDialog(
                        name = s.customer.name,
                        onConfirm = viewModel::confirmDelete,
                        onDismiss = viewModel::cancelDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(s: DetailsUiState.Loaded) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Balance owed", style = MaterialTheme.typography.labelLarge)
            Text(
                if (s.balanceNgwee <= 0) "Paid up" else Money.format(s.balanceNgwee),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(s.customer.phone, style = MaterialTheme.typography.bodyMedium)
            if (s.customer.note.isNotBlank()) {
                Text(s.customer.note, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EntryRow(entry: LedgerEntry) {
    val isPayment = entry.type == EntryType.PAYMENT
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(if (isPayment) "Payment" else "Credit", style = MaterialTheme.typography.titleSmall)
            Text(dateFormat.format(Date(entry.createdAt)), style = MaterialTheme.typography.bodySmall)
            if (entry.note.isNotBlank()) Text(entry.note, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = (if (isPayment) "- " else "+ ") + Money.format(entry.amountNgwee),
            color = if (isPayment) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold
        )
    }
}
