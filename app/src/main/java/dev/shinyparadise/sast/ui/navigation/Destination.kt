package dev.shinyparadise.sast.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Routes {
    @Serializable data object Main : Routes
    @Serializable data object Details : Routes
}
