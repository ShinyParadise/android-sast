package dev.shinyparadise.sast.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import dev.shinyparadise.sast.ui.screens.details.DetailsScreen
import dev.shinyparadise.sast.ui.screens.main.MainScreen
import dev.shinyparadise.sast.ui.screens.main.MainViewModel

@Composable
fun MainNavGraph(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    backStack: NavBackStack<Routes>,
) {
    Box(modifier = modifier) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<Routes.Main> {
                    MainScreen(viewModel)
                }

                entry<Routes.Details> {
                    DetailsScreen(viewModel)
                }
            }
        )
    }
}