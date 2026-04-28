package dev.shinyparadise.sast.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Routes : NavKey {
    @Serializable
    data object Main : Routes
    @Serializable
    data object Details : Routes
    @Serializable
    data object Settings : Routes
}