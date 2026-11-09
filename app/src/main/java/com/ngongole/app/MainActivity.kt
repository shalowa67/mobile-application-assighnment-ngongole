package com.ngongole.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ngongole.app.ui.NgongoleNavHost
import com.ngongole.app.ui.theme.NgongoleTheme

/** The only Activity. Each screen is a composable shown by the navigation host. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NgongoleTheme {
                NgongoleNavHost()
            }
        }
    }
}
