package com.t4kash.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.t4kash.app.ui.screen.ApplicationSentScreen
import com.t4kash.app.ui.screen.ApplicationManagementScreen
import com.t4kash.app.ui.screen.AdminScreen
import com.t4kash.app.ui.screen.AssignedJobsScreen
import com.t4kash.app.ui.screen.JobDetailScreen
import com.t4kash.app.ui.screen.ChatScreen
import com.t4kash.app.ui.screen.ConversationScreen
import com.t4kash.app.ui.screen.LoginScreen
import com.t4kash.app.ui.screen.LoginVerificationScreen
import com.t4kash.app.ui.screen.MarketplaceScreen
import com.t4kash.app.ui.screen.MyPublicationsScreen
import com.t4kash.app.ui.screen.NetworkScreen
import com.t4kash.app.ui.screen.NotificationsScreen
import com.t4kash.app.ui.screen.OpportunityDetailScreen
import com.t4kash.app.ui.screen.OpportunityMapScreen
import com.t4kash.app.ui.screen.PostTaskScreen
import com.t4kash.app.ui.screen.ProfileScreen
import com.t4kash.app.ui.screen.RegisterScreen
import com.t4kash.app.ui.screen.ForgotPasswordScreen
import com.t4kash.app.ui.screen.ResetPasswordScreen
import com.t4kash.app.ui.screen.SplashScreen
import com.t4kash.app.ui.screen.WalletScreen
import com.t4kash.app.ui.screen.VerifyEmailScreen
import com.t4kash.app.ui.viewmodel.MarketplaceViewModel
import com.t4kash.app.ui.viewmodel.AuthViewModel
import com.t4kash.app.ui.viewmodel.CommunicationViewModel
import com.t4kash.app.ui.session.UserSession

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = viewModel()
    val marketplaceViewModel: MarketplaceViewModel = viewModel()
    val communicationViewModel: CommunicationViewModel = viewModel()
    val session by UserSession.session.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.validateStoredSession {
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }
    LaunchedEffect(session?.token) {
        if (session == null) {
            communicationViewModel.clearSession()
        } else {
            communicationViewModel.refreshOverview()
        }
    }
    val onBottomNavigate: (String) -> Unit = { route ->
        if (route == Routes.MARKETPLACE) {
            marketplaceViewModel.refresh(force = true)
        }
        if (route == Routes.CHAT) {
            communicationViewModel.refreshOverview()
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
                    val destination = if (session == null) {
                        Routes.LOGIN
                    } else {
                        Routes.MARKETPLACE
                    }
                    navController.navigate(destination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginVerification = { email ->
                    navController.navigate(Routes.loginVerification(email))
                },
                onRegister = { navController.navigate(Routes.REGISTER) },
                onVerifyEmail = {
                    navController.navigate(Routes.verifyEmail())
                },
                onForgotPassword = {
                    navController.navigate(Routes.FORGOT_PASSWORD)
                }
            )
        }
        composable(
            route = Routes.LOGIN_VERIFICATION,
            arguments = listOf(
                navArgument(Routes.LOGIN_VERIFICATION_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            LoginVerificationScreen(
                initialEmail = backStackEntry.arguments
                    ?.getString(Routes.LOGIN_VERIFICATION_ARG)
                    .orEmpty(),
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onVerified = {
                    navController.navigate(Routes.MARKETPLACE) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onCodeRequested = { email ->
                    navController.navigate(Routes.resetPassword(email))
                }
            )
        }
        composable(
            route = Routes.RESET_PASSWORD,
            arguments = listOf(
                navArgument(Routes.RESET_PASSWORD_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            ResetPasswordScreen(
                initialEmail = backStackEntry.arguments
                    ?.getString(Routes.RESET_PASSWORD_ARG)
                    .orEmpty(),
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.LOGIN) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onVerificationRequired = { email ->
                    navController.navigate(Routes.verifyEmail(email))
                }
            )
        }
        composable(
            route = Routes.VERIFY_EMAIL,
            arguments = listOf(
                navArgument(Routes.VERIFY_EMAIL_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            VerifyEmailScreen(
                initialEmail = backStackEntry.arguments
                    ?.getString(Routes.VERIFY_EMAIL_ARG)
                    .orEmpty(),
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onVerified = {
                    navController.navigate(Routes.MARKETPLACE) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.MARKETPLACE) {
            MarketplaceScreen(
                viewModel = marketplaceViewModel,
                currentRoute = Routes.MARKETPLACE,
                user = session?.user,
                onNavigate = onBottomNavigate,
                onTaskSelected = { task -> navController.navigate(Routes.taskDetails(task.idTarea)) },
                onOpenMap = { navController.navigate(Routes.OPPORTUNITY_MAP) },
                onOpenNotifications = {
                    navController.navigate(Routes.NOTIFICATIONS)
                },
                unreadNotifications =
                    communicationViewModel.uiState.unreadNotifications
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
            ChatScreen(
                viewModel = communicationViewModel,
                onNavigate = onBottomNavigate,
                onOpenConversation = { conversationId ->
                    navController.navigate(
                        Routes.conversation(conversationId)
                    )
                },
                onOpenNotifications = {
                    navController.navigate(Routes.NOTIFICATIONS)
                }
            )
        }
        composable(
            route = Routes.CONVERSATION,
            arguments = listOf(
                navArgument(Routes.CONVERSATION_ID_ARG) {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments
                ?.getInt(Routes.CONVERSATION_ID_ARG)
                ?: 0
            ConversationScreen(
                conversationId = conversationId,
                viewModel = communicationViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(
                viewModel = communicationViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.TASK_EDIT,
            arguments = listOf(
                navArgument(Routes.TASK_ID_ARG) { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getInt(Routes.TASK_ID_ARG) ?: 0
            PostTaskScreen(
                viewModel = marketplaceViewModel,
                onNavigate = onBottomNavigate,
                editTaskId = taskId,
                onBack = { navController.popBackStack() },
                onTaskPublished = {
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.PROFILE) {
            val currentUser = session?.user
            if (currentUser == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
                return@composable
            }
            ProfileScreen(
                viewModel = marketplaceViewModel,
                user = currentUser,
                onNavigate = onBottomNavigate,
                onOpenPublications = { filter ->
                    navController.navigate(Routes.myPublications(filter))
                },
                onOpenJobs = { navController.navigate(Routes.ASSIGNED_JOBS) },
                onOpenWallet = { navController.navigate(Routes.WALLET) },
                onOpenAdmin = { navController.navigate(Routes.ADMIN) },
                onLogout = {
                    authViewModel.logout {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Routes.ADMIN) {
            val currentUser = session?.user
            if (currentUser?.roles?.contains("ADMIN") != true) {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
                return@composable
            }
            AdminScreen(
                viewModel = marketplaceViewModel,
                onBack = { navController.popBackStack() }
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
                },
                onEditTask = { taskId ->
                    navController.navigate(Routes.editTask(taskId))
                }
            )
        }
        composable(Routes.WALLET) {
            WalletScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ASSIGNED_JOBS) {
            AssignedJobsScreen(
                viewModel = marketplaceViewModel,
                onBack = { navController.popBackStack() },
                onJobSelected = { jobId ->
                    navController.navigate(Routes.jobDetails(jobId))
                }
            )
        }
        composable(
            route = Routes.JOB_DETAILS,
            arguments = listOf(
                navArgument(Routes.JOB_ID_ARG) { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getInt(Routes.JOB_ID_ARG) ?: 0
            JobDetailScreen(
                jobId = jobId,
                viewModel = marketplaceViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.APPLICATION_SENT) {
            val application = marketplaceViewModel.uiState.sentApplication
            val task = marketplaceViewModel.uiState.tasks.firstOrNull {
                it.idTarea == application?.idTarea
            }
            ApplicationSentScreen(
                application = application,
                task = task,
                onViewOpportunity = {
                    marketplaceViewModel.clearApplicationFeedback()
                    navController.popBackStack()
                },
                onExplore = {
                    marketplaceViewModel.clearApplicationFeedback()
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
