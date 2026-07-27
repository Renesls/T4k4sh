package com.t4kash.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.t4kash.app.ui.screen.ApplicationSentScreen
import com.t4kash.app.ui.screen.ApplicationManagementScreen
import com.t4kash.app.ui.screen.ChatScreen
import com.t4kash.app.ui.screen.LoginScreen
import com.t4kash.app.ui.screen.MarketplaceScreen
import com.t4kash.app.ui.screen.MyPublicationsScreen
import com.t4kash.app.ui.screen.NetworkScreen
import com.t4kash.app.ui.screen.OpportunityDetailScreen
import com.t4kash.app.ui.screen.OpportunityMapScreen
import com.t4kash.app.ui.screen.PostTaskScreen
import com.t4kash.app.ui.screen.ProfileScreen
import com.t4kash.app.ui.screen.SplashScreen
import com.t4kash.app.ui.screen.WalletScreen
import com.t4kash.app.ui.viewmodel.MarketplaceViewModel

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    val marketplaceViewModel: MarketplaceViewModel = viewModel()
    val onBottomNavigate: (String) -> Unit = { route ->
        if (route == Routes.MARKETPLACE) {
            marketplaceViewModel.refresh()
        }
        navController.navigateBottom(route)
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MARKETPLACE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.MARKETPLACE) {
            MarketplaceScreen(
                viewModel = marketplaceViewModel,
                currentRoute = Routes.MARKETPLACE,
                onNavigate = onBottomNavigate,
                onTaskSelected = { task -> navController.navigate(Routes.taskDetails(task.idTarea)) },
                onCreateTask = { navController.navigateBottom(Routes.POST) },
                onOpenMap = { navController.navigate(Routes.OPPORTUNITY_MAP) }
            )
        }
        composable(Routes.OPPORTUNITY_MAP) {
            OpportunityMapScreen(
                viewModel = marketplaceViewModel,
                onBack = { navController.popBackStack() },
                onTaskSelected = { taskId ->
                    navController.navigate(Routes.taskDetails(taskId))
                }
            )
        }
        composable(
            route = Routes.OPPORTUNITY_MAP_TASK,
            arguments = listOf(
                navArgument(Routes.TASK_ID_ARG) { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getInt(Routes.TASK_ID_ARG)
            OpportunityMapScreen(
                viewModel = marketplaceViewModel,
                onBack = { navController.popBackStack() },
                onTaskSelected = { selectedTaskId ->
                    navController.navigate(Routes.taskDetails(selectedTaskId))
                },
                focusedTaskId = taskId
            )
        }
        composable(
            route = Routes.TASK_DETAILS,
            arguments = listOf(
                navArgument(Routes.TASK_ID_ARG) { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getInt(Routes.TASK_ID_ARG) ?: 0
            OpportunityDetailScreen(
                taskId = taskId,
                viewModel = marketplaceViewModel,
                onBack = { navController.popBackStack() },
                onApply = { navController.navigate(Routes.APPLICATION_SENT) },
                onOpenMap = { navController.navigate(Routes.opportunityMap(taskId)) },
                onManageApplications = {
                    navController.navigate(Routes.taskApplications(taskId))
                }
            )
        }
        composable(
            route = Routes.TASK_APPLICATIONS,
            arguments = listOf(
                navArgument(Routes.TASK_ID_ARG) { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getInt(Routes.TASK_ID_ARG) ?: 0
            ApplicationManagementScreen(
                taskId = taskId,
                viewModel = marketplaceViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.NETWORK) {
            NetworkScreen(onNavigate = onBottomNavigate)
        }
        composable(Routes.POST) {
            PostTaskScreen(
                viewModel = marketplaceViewModel,
                onNavigate = onBottomNavigate,
                onTaskPublished = {
                    navController.navigate(Routes.OPPORTUNITY_MAP) {
                        popUpTo(Routes.POST) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.CHAT) {
            ChatScreen(onNavigate = onBottomNavigate)
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                viewModel = marketplaceViewModel,
                onNavigate = onBottomNavigate,
                onOpenPublications = { filter ->
                    navController.navigate(Routes.myPublications(filter))
                },
                onOpenWallet = { navController.navigate(Routes.WALLET) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MARKETPLACE) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Routes.MY_PUBLICATIONS,
            arguments = listOf(
                navArgument(Routes.PUBLICATION_FILTER_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val filter = backStackEntry.arguments
                ?.getString(Routes.PUBLICATION_FILTER_ARG)
                ?: "ALL"
            MyPublicationsScreen(
                initialFilter = filter,
                viewModel = marketplaceViewModel,
                onBack = { navController.popBackStack() },
                onTaskSelected = { taskId ->
                    navController.navigate(Routes.taskDetails(taskId))
                }
            )
        }
        composable(Routes.WALLET) {
            WalletScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.APPLICATION_SENT) {
            ApplicationSentScreen(
                onBackHome = {
                    navController.navigate(Routes.MARKETPLACE) {
                        popUpTo(Routes.MARKETPLACE) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

private fun NavHostController.navigateBottom(route: String) {
    navigate(route) {
        popUpTo(Routes.MARKETPLACE) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
