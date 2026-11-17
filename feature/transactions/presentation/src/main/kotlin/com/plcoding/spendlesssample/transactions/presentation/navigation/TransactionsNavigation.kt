package com.plcoding.spendlesssample.transactions.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.plcoding.spendlesssample.transactions.presentation.list.TransactionListRoot
import kotlinx.serialization.Serializable

/** The transactions feature graph. */
@Serializable
data object TransactionsGraphRoute

/** All Transactions list. The create sheet is not a destination; it is hosted by the list. */
@Serializable
data object TransactionListRoute

@Suppress("UNUSED_PARAMETER") // navController is kept for the graph contract; no in-graph navigation yet.
fun NavGraphBuilder.transactionsGraph(
    navController: NavController,
    onNavigateBack: () -> Unit,
) {
    navigation<TransactionsGraphRoute>(startDestination = TransactionListRoute) {
        composable<TransactionListRoute> {
            TransactionListRoot(onNavigateBack = onNavigateBack)
        }
    }
}
