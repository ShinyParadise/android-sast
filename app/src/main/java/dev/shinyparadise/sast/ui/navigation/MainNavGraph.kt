package dev.shinyparadise.sast.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import dev.shinyparadise.sast.ui.screens.details.DetailsScreen
import dev.shinyparadise.sast.ui.screens.main.MainScreen
import dev.shinyparadise.sast.ui.screens.main.MainViewModel
import dev.shinyparadise.sast.ui.screens.settings.SettingsScreen
import dev.shinyparadise.sast.ui.screens.settings.SettingsViewModel

fun NavBackStack<Routes>.navigateBack(): Boolean {
    if (size <= 1) return false
    removeLastOrNull()
    return true
}

@Composable
fun MainNavGraph(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    backStack: NavBackStack<Routes>,
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.navigateBack() },
        entryProvider = entryProvider {
            entry<Routes.Main> {
                MainScreen(viewModel)
            }

            entry<Routes.Details> {
                DetailsScreen(viewModel)
            }

            entry<Routes.Settings> {
                SettingsScreen(settingsViewModel) {
                    backStack.navigateBack()
                }
            }
        }
    )
}
