package com.plcoding.spendlesssample.transactions.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable

/** The transactions feature graph. */
@Serializable
data object TransactionsGraphRoute

/** All Transactions list. The create sheet is not a destination; it is hosted by the list. */
@Serializable
data object TransactionListRoute

@Suppress("UNUSED_PARAMETER") // Wired when the list screen lands (US2).
fun NavGraphBuilder.transactionsGraph(
    navController: NavController,
    onNavigateBack: () -> Unit,
) {
    navigation<TransactionsGraphRoute>(startDestination = TransactionListRoute) {
        composable<TransactionListRoute> {
            // TransactionListRoot is wired in User Story 2 (T066).
        }
    }
}
