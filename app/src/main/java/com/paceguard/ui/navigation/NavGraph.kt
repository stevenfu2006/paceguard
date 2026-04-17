package com.paceguard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.paceguard.ui.DetectionViewModel
import com.paceguard.ui.dashboard.DashboardScreen
import com.paceguard.ui.detail.DetailScreen

@Composable
fun NavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: DetectionViewModel = viewModel(factory = DetectionViewModel.factory(context))

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onFlagClick = { index -> navController.navigate("detail/$index") }
            )
        }
        composable(
            route = "detail/{index}",
            arguments = listOf(navArgument("index") { type = NavType.IntType })
        ) { backStack ->
            val index = backStack.arguments?.getInt("index") ?: return@composable
            DetailScreen(
                viewModel = viewModel,
                flagIndex = index,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
