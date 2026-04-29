package dev.shinyparadise.sast.ui.screens.main

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.shinyparadise.sast.ui.theme.SASTTheme
import kotlinx.coroutines.flow.collect

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MainScreenContent(uiState, viewModel::onEvent)
}

@Composable
private fun MainScreenContent(
    uiState: MainUiState,
    onEvent: (MainUiEvent) -> Unit,
) {
    PredictiveBackHandler(enabled = uiState.isLoading) { progress ->
        progress.collect()
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.loadingProgress > 0) {
                    LinearProgressIndicator(
                        progress = { uiState.loadingProgress },
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                } else {
                    CircularProgressIndicator()
                }

                Text(
                    text = uiState.loadingStage,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                if (uiState.loadingProgress > 0) {
                    Text(
                        text = "${(uiState.loadingProgress * 100).toInt()}%",
                        fontSize = 14.sp
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                uiState.chosenApkName?.let {
                    Text(it)
                    Spacer(Modifier.padding(vertical = 8.dp))
                }

                Button(
                    onClick = { onEvent(MainUiEvent.OnChooseClicked) },
                    enabled = !uiState.isLoading,
                ) {
                    Text("Choose apk")
                }
            }

            IconButton(
                onClick = { onEvent(MainUiEvent.NavigateToSettings) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(horizontal = 16.dp)
                    .systemBarsPadding()
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Preview(showSystemUi = false, showBackground = true)
@Composable
private fun MainScreenPreview() {
    SASTTheme {
        MainScreenContent(MainUiState(), {})
    }
}
