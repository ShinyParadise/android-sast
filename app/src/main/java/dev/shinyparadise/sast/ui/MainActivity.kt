package dev.shinyparadise.sast.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import dev.shinyparadise.sast.BuildConfig
import dev.shinyparadise.sast.ui.navigation.MainNavGraph
import dev.shinyparadise.sast.ui.navigation.Routes
import dev.shinyparadise.sast.ui.screens.main.MainUiEffect
import dev.shinyparadise.sast.ui.screens.main.MainUiEvent
import dev.shinyparadise.sast.ui.screens.main.MainViewModel
import dev.shinyparadise.sast.ui.screens.settings.SettingsViewModel
import dev.shinyparadise.sast.ui.theme.SASTTheme
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.android.ext.android.inject

@Composable
private fun <T : NavKey> rememberNavBackStack(vararg elements: T): NavBackStack<T> {
    return rememberSerializable(
        serializer = NavBackStackSerializer(elementSerializer = NavKeySerializer())
    ) {
        NavBackStack(*elements)
    }
}

class MainActivity : ComponentActivity() {

    private val viewModel by inject<MainViewModel>()
    private val settingsViewModel by inject<SettingsViewModel>()

    private lateinit var backStack: NavBackStack<Routes>

    private val registerForOpenFileResult = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.onEvent(MainUiEvent.OnApkChosen(uri))
    }

    private val registerForStoragePermissionResult = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    private val storagePermissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.uiEffects.receiveAsFlow()
            .onEach(::handleEffects)
            .launchIn(lifecycleScope)

        requestStoragePermission()

        setContent {
            backStack = rememberNavBackStack(Routes.Main)

            SASTTheme {
                MainNavGraph(
                    viewModel = viewModel,
                    settingsViewModel = settingsViewModel,
                    backStack = backStack,
                )
            }
        }
    }

    private fun requestStoragePermission() {
        if (isStoragePermissionGranted()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uri = "package:${BuildConfig.APPLICATION_ID}".toUri()
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    uri
                )
            )
        } else {
            registerForStoragePermissionResult.launch(storagePermissions)
        }
    }

    private fun isStoragePermissionGranted() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            storagePermissions.all {
                ContextCompat.checkSelfPermission(
                    this,
                    it,
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

    private fun handleEffects(effect: MainUiEffect) {
        when (effect) {
            is MainUiEffect.OpenFilePicker -> registerForOpenFileResult.launch(arrayOf("*/*"))
            MainUiEffect.NavigateToDetails -> backStack.add(Routes.Details)
            MainUiEffect.NavigateBack -> backStack.removeLastOrNull()
            MainUiEffect.NavigateToSettings -> backStack.add(Routes.Settings)
        }
    }
}
