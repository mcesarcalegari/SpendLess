package com.plcoding.spendlesssample.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.plcoding.spendlesssample.transactions.presentation.navigation.TransactionsGraphRoute
import com.plcoding.spendlesssample.transactions.presentation.navigation.transactionsGraph

/**
 * The app's navigation host. It emits no UI of its own; each destination's Screen carries the
 * previews (Constitution VIII), and the Roots it hosts need Koin, which previews must not use.
 */
@Composable
fun SpendLessNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val activity = LocalActivity.current
    NavHost(
        navController = navController,
        startDestination = TransactionsGraphRoute,
        modifier = modifier,
    ) {
        transactionsGraph(
            navController = navController,
            onNavigateBack = {
                // popBackStack() on the start destination would pop it and leave an empty host,
                // so finish when there is no previous destination.
                if (navController.previousBackStackEntry != null) {
                    navController.popBackStack()
                } else {
                    activity?.finish()
                }
            },
        )
    }
}
