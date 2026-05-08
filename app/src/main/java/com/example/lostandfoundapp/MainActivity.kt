package com.example.lostandfoundapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lostandfoundapp.model.UserRole
import com.example.lostandfoundapp.ui.screens.*
import com.example.lostandfoundapp.ui.theme.LostAndFoundAppTheme
import com.example.lostandfoundapp.viewmodel.AuthViewModel
import com.example.lostandfoundapp.viewmodel.LostItemViewModel
import com.example.lostandfoundapp.viewmodel.RatingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val isDarkMode by authViewModel.isDarkMode.collectAsState()

            LostAndFoundAppTheme(darkTheme = isDarkMode) {
                LostAndFoundApp(authViewModel)
            }
        }
    }
}

@Composable
fun LostAndFoundApp(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val viewModel: LostItemViewModel = viewModel()
    val ratingViewModel: RatingViewModel = viewModel()

    val currentUser by authViewModel.currentUser.collectAsState()
    val isCheckingAuth by authViewModel.isCheckingAuth.collectAsState()

    if (isCheckingAuth) {
        // Show a loading indicator while the app checks for a persisted session
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // Start at login if no user is found, otherwise dashboard
        val startDest = if (currentUser == null) "login" else "dashboard"

        NavHost(navController = navController, startDestination = startDest) {
            composable("login") {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate("register") }
                )
            }
            composable("register") {
                RegisterScreen(
                    viewModel = authViewModel,
                    onRegisterSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo("register") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.navigate("login") }
                )
            }
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    navController = navController,
                    userEmail = currentUser?.email ?: "",
                    isAdmin = currentUser?.role == UserRole.ADMIN,
                    onAddItemClick = { navController.navigate("add_item") },
                    onItemClick = { item -> navController.navigate("item_detail/${item.id}") },
                    onUpdateClick = { item -> navController.navigate("update_item/${item.id}") },
                    onAdminPanelClick = { navController.navigate("admin_panel") },
                    onLogoutClick = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }
                )
            }
            composable("admin_panel") {
                AdminPanelScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    authViewModel = authViewModel,
                    onBackClick = { navController.popBackStack() },
                    onLogoutClick = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }
                )
            }
            composable("rating") {
                RatingScreen(
                    viewModel = ratingViewModel,
                    userEmail = currentUser?.email ?: "guest",
                    userName = currentUser?.fullName ?: "Guest",
                    onBackClick = { navController.popBackStack() },
                    onViewAllRatingsClick = { navController.navigate("all_ratings") }
                )
            }
            composable("all_ratings") {
                AllRatingsScreen(
                    viewModel = ratingViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("add_item") {
                AddItemScreen(
                    viewModel = viewModel,
                    userEmail = currentUser?.email,
                    userName = currentUser?.fullName,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = "update_item/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId")
                AddItemScreen(
                    viewModel = viewModel,
                    userEmail = currentUser?.email,
                    userName = currentUser?.fullName,
                    itemId = itemId,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = "item_detail/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId")
                itemId?.let { id ->
                    val item by viewModel.getItem(id).collectAsState(initial = null)
                    item?.let { lostItem ->
                        ItemDetailScreen(
                            item = lostItem,
                                           currentUserEmail = currentUser?.email,
                            onUpdateClick = { navController.navigate("update_item/${it.id}") },
                            onDeleteClick = { viewModel.deleteItem(it) },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
