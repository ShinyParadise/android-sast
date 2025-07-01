package dev.shinyparadise.sast.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.shinyparadise.sast.ui.theme.SASTTheme

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
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        uiState.chosenApkName?.let {
            Text(it)
            Spacer(Modifier.padding(vertical = 5.dp))
        }

        Button(
            onClick = { onEvent(MainUiEvent.OnChooseClicked) },
            enabled = !uiState.isLoading,
        ) {
            Text("Choose apk")
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
