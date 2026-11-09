package com.ngongole.app.ui.customers

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ngongole.app.reminders.ReminderNotifier

/**
 * On Android 13+ the app must ask before showing notifications.
 * We ask once when the list first opens. If the owner says no, the app keeps working
 * and shows a small card explaining reminders are off, with a link to Settings.
 */
@Composable
fun NotificationPermissionBanner(modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    var granted by rememberSaveable { mutableStateOf(ReminderNotifier.hasPermission(context)) }
    var alreadyAsked by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        granted = isGranted
    }

    LaunchedEffect(Unit) {
        if (!granted && !alreadyAsked) {
            alreadyAsked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // The owner may switch it on in Settings and come back: check again on resume.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = ReminderNotifier.hasPermission(context)
    }

    if (!granted && alreadyAsked) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 4.dp)) {
                Text(
                    "Daily reminders are off because notifications are not allowed.",
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    context.startActivity(intent)
                }) { Text("Turn on in Settings") }
            }
        }
    }
}
