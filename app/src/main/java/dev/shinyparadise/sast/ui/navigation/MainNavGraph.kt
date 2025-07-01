package dev.shinyparadise.sast.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.shinyparadise.sast.ui.screens.details.DetailsScreen
import dev.shinyparadise.sast.ui.screens.main.MainScreen
import dev.shinyparadise.sast.ui.screens.main.MainViewModel

@Composable
fun MainNavGraph(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    Box(modifier = modifier) {
        NavHost(
            navController = navController,
            startDestination = Routes.Main,
        ) {
            composable<Routes.Main> {
                MainScreen(viewModel)
            }

            composable<Routes.Details> {
                DetailsScreen(viewModel)
            }
        }
    }
}
