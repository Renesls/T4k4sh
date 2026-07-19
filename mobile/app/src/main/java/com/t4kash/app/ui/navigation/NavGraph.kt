package com.t4kash.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.t4kash.app.ui.screen.MarketplaceScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.MARKETPLACE
    ) {
        composable(Routes.MARKETPLACE) {
            MarketplaceScreen()
        }
    }
}
