package com.ngongole.app.ui.customers

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ngongole.app.data.local.CustomerBalance
import com.ngongole.app.domain.Money
import com.ngongole.app.reminders.ReminderScheduler
import com.ngongole.app.ui.AppViewModelProvider

/** Home screen: total owed, search box and the scrollable list of customers. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    onAddCustomer: () -> Unit,
    onOpenCustomer: (Long) -> Unit,
    viewModel: CustomerListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ngongole") },
                actions = {
                    IconButton(onClick = {
                        ReminderScheduler.runNow(context)
                        Toast.makeText(context, "Checking for customers who owe 7+ days…", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Check overdue customers now")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCustomer,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add customer") }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            NotificationPermissionBanner(Modifier.padding(bottom = 8.dp))
            TotalOwedCard(state.totalOwedNgwee, state.customers.size, state.search.isBlank())
            Spacer(Modifier.height(12.dp))
            SearchField(search, viewModel::onSearchChange)
            Spacer(Modifier.height(8.dp))

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.customers.isEmpty() -> EmptyState(isSearching = state.search.isNotBlank())
                else -> LazyColumn(
                    // Extra bottom space so the last row is not hidden behind the button
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(state.customers, key = { it.customer.id }) { item ->
                        CustomerRow(item, onClick = { onOpenCustomer(item.customer.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalOwedCard(totalNgwee: Long, customerCount: Int, showCount: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Total owed to the shop", style = MaterialTheme.typography.labelLarge)
            Text(
                Money.format(totalNgwee),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (showCount) {
                Text(
                    if (customerCount == 1) "1 customer" else "$customerCount customers",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("Search by name or phone") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                }
            }
        }
    )
}

@Composable
private fun CustomerRow(item: CustomerBalance, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.customer.name, style = MaterialTheme.typography.titleMedium)
            Text(item.customer.phone, style = MaterialTheme.typography.bodySmall)
        }
        val settled = item.balanceNgwee <= 0
        Text(
            text = if (settled) "Paid up" else Money.format(item.balanceNgwee),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (settled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun EmptyState(isSearching: Boolean) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = if (isSearching) "No customer matches your search."
            else "No customers yet.\nTap \"Add customer\" to record your first credit sale.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
