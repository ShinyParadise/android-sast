package dev.shinyparadise.sast.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import dev.shinyparadise.sast.ui.screens.details.DetailsScreen
import dev.shinyparadise.sast.ui.screens.main.MainScreen
import dev.shinyparadise.sast.ui.screens.main.MainViewModel

@Composable
fun MainNavGraph(
    viewModel: MainViewModel,
    backStack: NavBackStack<Routes>,
) {
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
