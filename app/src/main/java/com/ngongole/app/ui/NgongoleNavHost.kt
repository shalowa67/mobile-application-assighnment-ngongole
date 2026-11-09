package com.ngongole.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ngongole.app.ui.addcustomer.AddCustomerScreen
import com.ngongole.app.ui.customers.CustomerListScreen
import com.ngongole.app.ui.details.CustomerDetailsScreen

/** The routes (addresses) of our three screens. */
object Routes {
    const val CUSTOMERS = "customers"
    const val ADD_CUSTOMER = "customers/add"
    const val ARG_CUSTOMER_ID = "customerId"
    const val DETAILS = "customers/{$ARG_CUSTOMER_ID}"
    fun details(customerId: Long) = "customers/$customerId"
}

/**
 * Connects the screens. The NavController keeps a back stack, so the system
 * Back button returns to the previous screen and exits the app from the list.
 */
@Composable
fun NgongoleNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.CUSTOMERS) {
        composable(Routes.CUSTOMERS) {
            CustomerListScreen(
                onAddCustomer = { navController.navigate(Routes.ADD_CUSTOMER) },
                onOpenCustomer = { id -> navController.navigate(Routes.details(id)) }
            )
        }
        composable(Routes.ADD_CUSTOMER) { entry ->
            AddCustomerScreen(onDone = { navController.popIfCurrent(entry) })
        }
        composable(
            route = Routes.DETAILS,
            arguments = listOf(navArgument(Routes.ARG_CUSTOMER_ID) { type = NavType.LongType })
        ) { entry ->
            CustomerDetailsScreen(onBack = { navController.popIfCurrent(entry) })
        }
    }
}

/**
 * Only go back if [entry] is still the screen on top. Stops a fast double-tap on the
 * back arrow from popping two screens and leaving an empty window.
 */
private fun NavHostController.popIfCurrent(entry: NavBackStackEntry) {
    if (currentBackStackEntry?.id == entry.id) popBackStack()
}
