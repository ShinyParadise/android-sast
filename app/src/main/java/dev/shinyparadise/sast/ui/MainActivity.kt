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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.shinyparadise.sast.BuildConfig
import dev.shinyparadise.sast.ui.navigation.MainNavGraph
import dev.shinyparadise.sast.ui.navigation.Routes
import dev.shinyparadise.sast.ui.screens.main.MainUiEffect
import dev.shinyparadise.sast.ui.screens.main.MainUiEvent
import dev.shinyparadise.sast.ui.screens.main.MainViewModel
import dev.shinyparadise.sast.ui.theme.SASTTheme
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    lateinit var navController: NavHostController

    private val viewModel by inject<MainViewModel>()

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
            navController = rememberNavController()

            SASTTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainNavGraph(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel,
                        navController = navController,
                    )
                }
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
            MainUiEffect.NavigateToDetails -> navController.navigate(Routes.Details)
        }
    }
}
