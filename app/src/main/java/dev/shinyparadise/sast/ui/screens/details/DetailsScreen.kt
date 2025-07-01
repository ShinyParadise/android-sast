package dev.shinyparadise.sast.ui.screens.details

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.shinyparadise.sast.domain.Vulnerability
import dev.shinyparadise.sast.ui.screens.main.MainUiEvent
import dev.shinyparadise.sast.ui.screens.main.MainUiState
import dev.shinyparadise.sast.ui.screens.main.MainViewModel

@Composable
fun DetailsScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DetailsScreenContent(uiState, viewModel::onEvent)
}

@Composable
private fun DetailsScreenContent(uiState: MainUiState, onEvent: (MainUiEvent) -> Unit) {
    LazyColumn {
        uiState.report?.let { report ->
            item {
                Text(uiState.report.apkPath)
                Spacer(Modifier.height(5.dp))

                Text(uiState.report.summary)
                Spacer(Modifier.height(5.dp))

                Text(uiState.report.vulnerabilities.size.toString())
                Spacer(Modifier.height(5.dp))
            }

            items(report.vulnerabilities) {
                VulnerabilityItem(it, Modifier.padding(10.dp))
            }
        }
    }
}

@Composable
private fun VulnerabilityItem(item: Vulnerability, modifier: Modifier = Modifier) {
    Card(modifier = modifier.padding(5.dp)) {
        Text(item.file)
        Spacer(Modifier.height(5.dp))

        Text(item.line.toString())
        Spacer(Modifier.height(5.dp))

        item.type?.let { Text(it) }
        Spacer(Modifier.height(5.dp))

        Text(item.description)
    }
}
